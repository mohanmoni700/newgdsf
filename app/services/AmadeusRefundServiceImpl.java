package services;

import com.amadeus.xml._2010._06.ticketgtp_v3.AMATicketIgnoreRefundRS;
import com.amadeus.xml._2010._06.ticketgtp_v3.AMATicketInitRefundRS;
import com.amadeus.xml._2010._06.ticketgtp_v3.AMATicketProcessRefundRS;
import com.amadeus.xml._2010._06.tickettypes_v2.DocumentAndCouponInformationType;
import com.amadeus.xml._2010._06.tickettypes_v2.MonetaryInformationType;
import com.amadeus.xml._2010._06.tickettypes_v2.RefundDetailsLightType;
import com.amadeus.xml._2010._06.tickettypes_v2.RefundDetailsType;
import com.amadeus.xml._2010._06.types_v2.ErrorsType;
import com.amadeus.xml.fatceq_13_1_1a.TicketCheckEligibility;
import com.amadeus.xml.pnracc_11_3_1a.PNRReply;
import com.amadeus.xml.tatreq_20_1_1a.TicketProcessEDoc;
import com.amadeus.xml.tatres_20_1_1a.CouponInformationDetailsTypeI;
import com.amadeus.xml.tatres_20_1_1a.TicketProcessEDocReply;
import com.compassites.GDSWrapper.amadeus.RefundServiceHandler;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.model.ErrorMessage;
import com.compassites.model.PNRResponse;
import com.compassites.model.TicketCheckEligibilityRes;
import com.compassites.model.TicketProcessRefundRes;
import models.AmadeusSessionWrapper;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import play.Play;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AmadeusRefundServiceImpl implements RefundService{

    static org.slf4j.Logger amadeusLogger = LoggerFactory.getLogger("amadeus");

    static org.slf4j.Logger logger = LoggerFactory.getLogger("gds");

    @Autowired
    private AmadeusSourceOfficeService amadeusSourceOfficeService;

    @Autowired
    private ServiceHandler serviceHandler;

    @Autowired
    private RefundServiceHandler refundServiceHandler;
    @Override
    public TicketCheckEligibilityRes checkTicketEligibility(String gdsPnr,String searchOfficeId) {
        AmadeusSessionWrapper amadeusSessionWrapper = null;
        AMATicketInitRefundRS amaTicketInitRefundRS = null;
        TicketCheckEligibilityRes ticketCheckEligibilityRes = new TicketCheckEligibilityRes();
        TicketProcessEDocReply ticketProcessEDocReply = null;
        try {
            //get Delhi officeId
            String officeId = amadeusSourceOfficeService.getDelhiSourceOffice().getOfficeId();
            //securitySignin
            amadeusSessionWrapper = serviceHandler.logIn(officeId);
            PNRReply pnrReply = null;
            BigDecimal totalRefundable = new BigDecimal(0);
            //retrievePnr
            pnrReply = serviceHandler.retrivePNR(gdsPnr, amadeusSessionWrapper);
            if (pnrReply != null && pnrReply.getGeneralErrorInfo().size() == 0 && isPNRActive(pnrReply.getOriginDestinationDetails())) {
                List<String> ticketList = getTicketList(pnrReply.getDataElementsMaster().getDataElementsIndiv());
                if (ticketList != null && ticketList.size() > 0) {
                    ticketProcessEDocReply = serviceHandler.ticketProcessEDoc(ticketList, amadeusSessionWrapper);
                    //4 iterationsl
                    Boolean notUsed = ticketProcessEDocReply.getDocGroup().stream().flatMap(docGroup ->
                        docGroup.getDocDetailsGroup().stream()).flatMap(docDetailsGroup -> docDetailsGroup.getCouponGroup().stream()).
                            flatMap(couponGroup -> couponGroup.getCouponInfo().getCouponDetails().stream()).
                            anyMatch(couponInformationDetailsTypeI -> couponInformationDetailsTypeI.getCpnStatus().equalsIgnoreCase("I"));
                    Boolean errorStatus = Boolean.FALSE;
                    String erromsg = null;
                    if (notUsed) {
                        //process initRefund.
                        amaTicketInitRefundRS = refundServiceHandler.ticketInitRefund(ticketList, amadeusSessionWrapper,searchOfficeId);
                        if (amaTicketInitRefundRS != null && amaTicketInitRefundRS.getGeneralReply().getErrors() == null) {
                            List<AMATicketInitRefundRS.FunctionalData.ContractBundle> contractBundles = amaTicketInitRefundRS.getFunctionalData().getContractBundle();
                            for (AMATicketInitRefundRS.FunctionalData.ContractBundle contractBundle : contractBundles) {
                                if (contractBundle.getErrors() == null) {
                                    List<RefundDetailsType.Contracts.Contract> contracts = contractBundle.getRefundDetails().getContracts().getContract();
                                    for (RefundDetailsType.Contracts.Contract contract : contracts) {
                                        List<MonetaryInformationType> monetaryInformations = contract.getMonetaryInformations().getMonetaryInformation();
                                        for (MonetaryInformationType monetaryInformationType : monetaryInformations) {
                                            if (monetaryInformationType.getQualifier().toString().equalsIgnoreCase("RFT")) {
                                                ticketCheckEligibilityRes.setCurrency(monetaryInformationType.getCurrencyCode());
                                                totalRefundable = totalRefundable.add(monetaryInformationType.getAmount());
                                            }
                                        }
                                    }
                                } else {
                                    errorStatus = Boolean.TRUE;
                                    ErrorsType value = (ErrorsType) contractBundle.getErrors().getContent().get(0).getValue();
                                    erromsg =  value.getError().get(0).getValue();;
                                }
                            }
                            if (errorStatus) {
                                ErrorMessage errorMessage = new ErrorMessage();
                                errorMessage.setGdsPNR(gdsPnr);
                                errorMessage.setProvider("Amadeus");
                                errorMessage.setMessage(erromsg);
                                ticketCheckEligibilityRes.setStatus(Boolean.FALSE);
                                ticketCheckEligibilityRes.setRefundableAmount(new BigDecimal(0));
                                ticketCheckEligibilityRes.setMessage(errorMessage);
                            } else {
                                ticketCheckEligibilityRes.setStatus(Boolean.TRUE);
                                ticketCheckEligibilityRes.setRefundableAmount(calculateTotalRefund(totalRefundable));
                            }
                            AMATicketIgnoreRefundRS amaTicketIgnoreRefundRS = refundServiceHandler.ticketIgnoreRefundRQ(amadeusSessionWrapper);
                            pnrReply = serviceHandler.ignorePNRAddMultiElement(amadeusSessionWrapper);
                        } else {
                            AMATicketIgnoreRefundRS amaTicketIgnoreRefundRS = refundServiceHandler.ticketIgnoreRefundRQ(amadeusSessionWrapper);
                            pnrReply = serviceHandler.ignorePNRAddMultiElement(amadeusSessionWrapper);
                        }
                    }else{
                        Boolean airportControl = ticketProcessEDocReply.getDocGroup().stream().flatMap(docGroup ->
                                        docGroup.getDocDetailsGroup().stream()).flatMap(docDetailsGroup -> docDetailsGroup.getCouponGroup().stream()).
                                flatMap(couponGroup -> couponGroup.getCouponInfo().getCouponDetails().stream()).
                                allMatch(couponInformationDetailsTypeI -> couponInformationDetailsTypeI.getCpnStatus().equalsIgnoreCase("AL"));
                        Boolean used = ticketProcessEDocReply.getDocGroup().stream().flatMap(docGroup ->
                                        docGroup.getDocDetailsGroup().stream()).flatMap(docDetailsGroup -> docDetailsGroup.getCouponGroup().stream()).
                                flatMap(couponGroup -> couponGroup.getCouponInfo().getCouponDetails().stream()).
                                allMatch(couponInformationDetailsTypeI -> couponInformationDetailsTypeI.getCpnStatus().equalsIgnoreCase("B"));
                        ErrorMessage errorMessage = new ErrorMessage();
                        errorMessage.setGdsPNR(gdsPnr);
                        errorMessage.setProvider("Amadeus");
                        if(airportControl)
                            errorMessage.setMessage("Check with Airport Control. Refund is not possible");
                        if(used)
                            errorMessage.setMessage("Ticket is Used. Refund is not possible");

                        Boolean anyAirportControl = ticketProcessEDocReply.getDocGroup().stream().flatMap(docGroup ->
                                        docGroup.getDocDetailsGroup().stream()).flatMap(docDetailsGroup -> docDetailsGroup.getCouponGroup().stream()).
                                flatMap(couponGroup -> couponGroup.getCouponInfo().getCouponDetails().stream()).
                                anyMatch(couponInformationDetailsTypeI -> couponInformationDetailsTypeI.getCpnStatus().equalsIgnoreCase("AL"));
                        if(anyAirportControl){
                          String ticketsWithAL = getTicketListwithAL(ticketProcessEDocReply);
                            errorMessage.setMessage("Tickets: "+ticketsWithAL + "are not refundable. Contact Airport control.Proceed with partial refund for other tickets" );
                        }

                        Boolean anyused = ticketProcessEDocReply.getDocGroup().stream().flatMap(docGroup ->
                                        docGroup.getDocDetailsGroup().stream()).flatMap(docDetailsGroup -> docDetailsGroup.getCouponGroup().stream()).
                                flatMap(couponGroup -> couponGroup.getCouponInfo().getCouponDetails().stream()).
                                anyMatch(couponInformationDetailsTypeI -> couponInformationDetailsTypeI.getCpnStatus().equalsIgnoreCase("B"));
                        if(anyused){
                            String usedTickets = getUsedTickets(ticketProcessEDocReply);
                            errorMessage.setMessage("Tickets: "+usedTickets + "are used.Proceed with partial refund for other tickets" );
                        }
                        ticketCheckEligibilityRes.setMessage(errorMessage);
                        ticketCheckEligibilityRes.setStatus(false);
                        ticketCheckEligibilityRes.setRefundableAmount(new BigDecimal(0));
                    }

                }
            } else if (pnrReply != null && pnrReply.getGeneralErrorInfo().size() > 0) {
                pnrReply = serviceHandler.ignorePNRAddMultiElement(amadeusSessionWrapper);
            }else if (pnrReply != null && pnrReply.getOriginDestinationDetails().size() ==0) {
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setProvider("Amadeus");
                errorMessage.setGdsPNR(gdsPnr);
                errorMessage.setMessage("PNR not Active");
                ticketCheckEligibilityRes.setMessage(errorMessage);
                ticketCheckEligibilityRes.setStatus(Boolean.FALSE);
                pnrReply = serviceHandler.ignorePNRAddMultiElement(amadeusSessionWrapper);
            }
        } catch (Exception e) {
            logger.debug("An exception occured during CheckEligibility of TicketRefund"+ e.getMessage() );
            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setProvider("Amadeus");
            errorMessage.setGdsPNR(gdsPnr);
            errorMessage.setMessage(e.getMessage());
            ticketCheckEligibilityRes.setMessage(errorMessage);
            ticketCheckEligibilityRes.setStatus(Boolean.FALSE);
            e.printStackTrace();
        }finally {
            serviceHandler.logOut(amadeusSessionWrapper);
        }
        return ticketCheckEligibilityRes;
    }

    @Override
    public TicketProcessRefundRes processFullRefund(String gdsPnr,String searchOfficeId) {
        AmadeusSessionWrapper amadeusSessionWrapper = null;
        AMATicketInitRefundRS amaTicketInitRefundRS = null;
        AMATicketProcessRefundRS amaTicketProcessRefundRS = null;
        TicketProcessRefundRes ticketProcessRefundRes = new TicketProcessRefundRes();
        List<String> refundedTickets = new ArrayList<>();
        try {
            //get Delhi officeId
            String officeId = amadeusSourceOfficeService.getDelhiSourceOffice().getOfficeId();
            //securitySignin
            amadeusSessionWrapper = serviceHandler.logIn(officeId);
            PNRReply pnrReply = null;
            BigDecimal totalRefundable = new BigDecimal(0);
            //retrievePnr
            pnrReply = serviceHandler.retrivePNR(gdsPnr, amadeusSessionWrapper);
            if (pnrReply != null && pnrReply.getGeneralErrorInfo().size() == 0 && isPNRActive(pnrReply.getOriginDestinationDetails())) {
                List<String> ticketList = getTicketList(pnrReply.getDataElementsMaster().getDataElementsIndiv());
                if (ticketList != null && ticketList.size() > 0) {
                    TicketProcessEDocReply ticketProcessEDocReply = serviceHandler.ticketProcessEDoc(ticketList, amadeusSessionWrapper);
                    //4 iterations
                    Boolean notUsed = ticketProcessEDocReply.getDocGroup().stream().flatMap(docGroup ->
                                    docGroup.getDocDetailsGroup().stream()).flatMap(docDetailsGroup -> docDetailsGroup.getCouponGroup().stream()).
                            flatMap(couponGroup -> couponGroup.getCouponInfo().getCouponDetails().stream()).
                            anyMatch(couponInformationDetailsTypeI -> couponInformationDetailsTypeI.getCpnStatus().equalsIgnoreCase("I"));
                    if (notUsed) {
                        //process initRefund.
                        amaTicketInitRefundRS = refundServiceHandler.ticketInitRefund(ticketList, amadeusSessionWrapper,searchOfficeId);
                        if (amaTicketInitRefundRS != null && amaTicketInitRefundRS.getGeneralReply().getErrors() == null) {
                            //Process Refund
                            amaTicketProcessRefundRS = refundServiceHandler.ticketProcessRefund(amadeusSessionWrapper);
                            if(amaTicketProcessRefundRS != null && amaTicketProcessRefundRS.getGeneralReply().getErrors() == null){
                                List<AMATicketProcessRefundRS.FunctionalData.ContractBundle> contractBundles = amaTicketProcessRefundRS.getFunctionalData().getContractBundle();
                                for(AMATicketProcessRefundRS.FunctionalData.ContractBundle contractBundle:contractBundles){
                                    List<RefundDetailsLightType.Contracts.Contract> contracts = contractBundle.getRefundDetails().getContracts().getContract();
                                    for(RefundDetailsLightType.Contracts.Contract contract:contracts){
                                         ticketProcessRefundRes.setRefundableAmount(contract.getRefundable().getAmount().toString());
                                         ticketProcessRefundRes.setCurrency(contract.getRefundable().getCurrencyCode());
                                        List<DocumentAndCouponInformationType> documentAndCouponInformations = contract.getDocumentAndCouponInformation();
                                        for(DocumentAndCouponInformationType documentAndCouponInformation : documentAndCouponInformations ){
                                            refundedTickets.add(documentAndCouponInformation.getDocumentNumber().getNumber().toString());
                                        }
                                    }
                                }
                                PNRReply cancelFullPNR = serviceHandler.cancelFullPNR(gdsPnr,pnrReply,amadeusSessionWrapper,Boolean.TRUE);
                                if(cancelFullPNR.getGeneralErrorInfo().size() == 0){
                                    logger.debug("PNR Cancelled for PNR:",gdsPnr);
                                }
                                ticketProcessRefundRes.setStatus(Boolean.TRUE);
                                ticketProcessRefundRes.setRefTicketsList(refundedTickets);
                            }
                        } else {
                            AMATicketIgnoreRefundRS amaTicketIgnoreRefundRS = refundServiceHandler.ticketIgnoreRefundRQ(amadeusSessionWrapper);
                            pnrReply = serviceHandler.ignorePNRAddMultiElement(amadeusSessionWrapper);
                            ticketProcessRefundRes.setStatus(Boolean.FALSE);
                        }
                    }

                }
            } else if (pnrReply != null && pnrReply.getGeneralErrorInfo().size() > 0) {
                pnrReply = serviceHandler.ignorePNRAddMultiElement(amadeusSessionWrapper);
                ticketProcessRefundRes.setStatus(Boolean.FALSE);
            }else if (pnrReply != null && pnrReply.getOriginDestinationDetails().size() ==0) {
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setProvider("Amadeus");
                errorMessage.setGdsPNR(gdsPnr);
                errorMessage.setMessage("PNR not Active");
                ticketProcessRefundRes.setMessage(errorMessage);
                ticketProcessRefundRes.setStatus(Boolean.FALSE);
                pnrReply = serviceHandler.ignorePNRAddMultiElement(amadeusSessionWrapper);
            }
        } catch (Exception e) {
            logger.debug("An exception occured during CheckEligibility of TicketRefund"+ e.getMessage() );
            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setProvider("Amadeus");
            errorMessage.setGdsPNR(gdsPnr);
            errorMessage.setMessage(e.getMessage());
            ticketProcessRefundRes.setMessage(errorMessage);
            ticketProcessRefundRes.setStatus(Boolean.FALSE);
            e.printStackTrace();

        }finally {
            serviceHandler.logOut(amadeusSessionWrapper);
        }
        return ticketProcessRefundRes;
    }

    @Override
    public TicketCheckEligibilityRes checkPartRefundTicketEligibility(List<String> refundticketList, String gdsPnr,String searchOfficeId) {
        AmadeusSessionWrapper amadeusSessionWrapper = null;
        AMATicketInitRefundRS amaTicketInitRefundRS = null;
        TicketCheckEligibilityRes ticketCheckEligibilityRes = new TicketCheckEligibilityRes();
        TicketProcessEDocReply ticketProcessEDocReply = null;
        try {
            //get Delhi officeId
            String officeId = amadeusSourceOfficeService.getDelhiSourceOffice().getOfficeId();
            //securitySignin
            amadeusSessionWrapper = serviceHandler.logIn(officeId);
            PNRReply pnrReply = null;
            BigDecimal totalRefundable = new BigDecimal(0);
            //retrievePnr
            pnrReply = serviceHandler.retrivePNR(gdsPnr, amadeusSessionWrapper);
            if (pnrReply != null && pnrReply.getGeneralErrorInfo().size() == 0 && isPNRActive(pnrReply.getOriginDestinationDetails())) {
                List<String> pnrTicketList = getTicketList(pnrReply.getDataElementsMaster().getDataElementsIndiv());
                boolean isTicketMatched = refundticketList.stream().allMatch(pnrTicketList::contains);
                if (isTicketMatched) {
                    logger.debug("All Tickets are avaible in the PNR: ",gdsPnr);
                } else {
                    List<String> ticketsNotinPnr = refundticketList.stream().filter(ticket->!pnrTicketList.contains(ticket)).collect(Collectors.toList());
                    List<String> ticketsinPnr = refundticketList.stream().filter(ticket->pnrTicketList.contains(ticket)).collect(Collectors.toList());
                    refundticketList = ticketsinPnr;
                }

                if (refundticketList != null && refundticketList.size() > 0) {
                    ticketProcessEDocReply = serviceHandler.ticketProcessEDoc(refundticketList, amadeusSessionWrapper);
                    //4 iterationsl
                    Boolean notUsed = ticketProcessEDocReply.getDocGroup().stream().flatMap(docGroup ->
                                    docGroup.getDocDetailsGroup().stream()).flatMap(docDetailsGroup -> docDetailsGroup.getCouponGroup().stream()).
                            flatMap(couponGroup -> couponGroup.getCouponInfo().getCouponDetails().stream()).
                           anyMatch(couponInformationDetailsTypeI -> couponInformationDetailsTypeI.getCpnStatus().equalsIgnoreCase("I"));
                    if (notUsed) {
                        //process initRefund.
                        amaTicketInitRefundRS = refundServiceHandler.ticketInitRefund(refundticketList, amadeusSessionWrapper,searchOfficeId);
                        if (amaTicketInitRefundRS != null && amaTicketInitRefundRS.getGeneralReply().getErrors() == null) {
                            List<AMATicketInitRefundRS.FunctionalData.ContractBundle> contractBundles = amaTicketInitRefundRS.getFunctionalData().getContractBundle();
                            for(AMATicketInitRefundRS.FunctionalData.ContractBundle contractBundle : contractBundles){
                                List<RefundDetailsType.Contracts.Contract> contracts = contractBundle.getRefundDetails().getContracts().getContract();
                                for(RefundDetailsType.Contracts.Contract contract : contracts){
                                    List<MonetaryInformationType> monetaryInformations = contract.getMonetaryInformations().getMonetaryInformation();
                                    for(MonetaryInformationType monetaryInformationType :monetaryInformations){
                                        if(monetaryInformationType.getQualifier().toString().equalsIgnoreCase("RFT")){
                                            ticketCheckEligibilityRes.setCurrency(monetaryInformationType.getCurrencyCode());
                                            totalRefundable = totalRefundable.add(monetaryInformationType.getAmount());
                                        }
                                    }
                                }
                            }
                            ticketCheckEligibilityRes.setStatus(Boolean.TRUE);
                            ticketCheckEligibilityRes.setRefundableAmount(calculateTotalRefund(totalRefundable));
                            AMATicketIgnoreRefundRS amaTicketIgnoreRefundRS = refundServiceHandler.ticketIgnoreRefundRQ(amadeusSessionWrapper);
                            pnrReply = serviceHandler.ignorePNRAddMultiElement(amadeusSessionWrapper);
                        } else {
                            AMATicketIgnoreRefundRS amaTicketIgnoreRefundRS = refundServiceHandler.ticketIgnoreRefundRQ(amadeusSessionWrapper);
                            pnrReply = serviceHandler.ignorePNRAddMultiElement(amadeusSessionWrapper);
                        }
                    }else{
                        Boolean airportControl = ticketProcessEDocReply.getDocGroup().stream().flatMap(docGroup ->
                                        docGroup.getDocDetailsGroup().stream()).flatMap(docDetailsGroup -> docDetailsGroup.getCouponGroup().stream()).
                                flatMap(couponGroup -> couponGroup.getCouponInfo().getCouponDetails().stream()).
                                allMatch(couponInformationDetailsTypeI -> couponInformationDetailsTypeI.getCpnStatus().equalsIgnoreCase("AL"));
                        Boolean used = ticketProcessEDocReply.getDocGroup().stream().flatMap(docGroup ->
                                        docGroup.getDocDetailsGroup().stream()).flatMap(docDetailsGroup -> docDetailsGroup.getCouponGroup().stream()).
                                flatMap(couponGroup -> couponGroup.getCouponInfo().getCouponDetails().stream()).
                                allMatch(couponInformationDetailsTypeI -> couponInformationDetailsTypeI.getCpnStatus().equalsIgnoreCase("B"));
                        Boolean refunded = ticketProcessEDocReply.getDocGroup().stream().flatMap(docGroup ->
                                        docGroup.getDocDetailsGroup().stream()).flatMap(docDetailsGroup -> docDetailsGroup.getCouponGroup().stream()).
                                flatMap(couponGroup -> couponGroup.getCouponInfo().getCouponDetails().stream()).
                                allMatch(couponInformationDetailsTypeI -> couponInformationDetailsTypeI.getCpnStatus().equalsIgnoreCase("RF"));
                        ErrorMessage errorMessage = new ErrorMessage();
                        errorMessage.setGdsPNR(gdsPnr);
                        if(airportControl)
                            errorMessage.setMessage("Check with Airport Control. Refund is not possible");
                        if(used)
                            errorMessage.setMessage("Ticket is Used. Refund is not possible");
                        if(refunded)
                            errorMessage.setMessage("Ticket is Refunded");
                        ticketCheckEligibilityRes.setMessage(errorMessage);
                        ticketCheckEligibilityRes.setStatus(Boolean.FALSE);

                    }

                }
            } else if (pnrReply != null && pnrReply.getGeneralErrorInfo().size() > 0) {
                pnrReply = serviceHandler.ignorePNRAddMultiElement(amadeusSessionWrapper);

            }
        } catch (Exception e) {
            logger.debug("An exception occured during CheckEligibility of TicketRefund"+ e.getMessage() );
            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setProvider("Amadeus");
            errorMessage.setGdsPNR(gdsPnr);
            errorMessage.setMessage(e.getMessage());
            ticketCheckEligibilityRes.setMessage(errorMessage);
            ticketCheckEligibilityRes.setStatus(Boolean.FALSE);
            e.printStackTrace();
        }finally {
            serviceHandler.logOut(amadeusSessionWrapper);
        }
        return ticketCheckEligibilityRes;
    }

    @Override
    public TicketProcessRefundRes processPartialRefund(List<String> refundticketList, String gdsPnr,String searchOfficeId) {
        AmadeusSessionWrapper amadeusSessionWrapper = null;
        AMATicketInitRefundRS amaTicketInitRefundRS = null;
        AMATicketProcessRefundRS amaTicketProcessRefundRS = null;
        TicketProcessRefundRes ticketProcessRefundRes = new TicketProcessRefundRes();
        List<String> refundedTickets = new ArrayList<>();
        try {
            //get Delhi officeId
            String officeId = amadeusSourceOfficeService.getDelhiSourceOffice().getOfficeId();
            //securitySignin
            amadeusSessionWrapper = serviceHandler.logIn(officeId);
            PNRReply pnrReply = null;
            BigDecimal totalRefundable = new BigDecimal(0);
            //retrievePnr
            pnrReply = serviceHandler.retrivePNR(gdsPnr, amadeusSessionWrapper);
            if (pnrReply != null && pnrReply.getGeneralErrorInfo().size() == 0 && isPNRActive(pnrReply.getOriginDestinationDetails())) {
                List<String> pnrTicketList = getTicketList(pnrReply.getDataElementsMaster().getDataElementsIndiv());
                boolean isTicketMatched = refundticketList.stream().allMatch(pnrTicketList::contains);
                if (isTicketMatched) {
                    logger.debug("All Tickets are avaible in the PNR: ",gdsPnr);
                } else {
                    List<String> ticketsNotinPnr = refundticketList.stream().filter(ticket->!pnrTicketList.contains(ticket)).collect(Collectors.toList());
                    List<String> ticketsinPnr = refundticketList.stream().filter(ticket->pnrTicketList.contains(ticket)).collect(Collectors.toList());
                    refundticketList = ticketsinPnr;
                }

                if (refundticketList != null && refundticketList.size() > 0) {
                    TicketProcessEDocReply ticketProcessEDocReply = serviceHandler.ticketProcessEDoc(refundticketList, amadeusSessionWrapper);
                    //4 iterations
                    Boolean notUsed = ticketProcessEDocReply.getDocGroup().stream().flatMap(docGroup ->
                                    docGroup.getDocDetailsGroup().stream()).flatMap(docDetailsGroup -> docDetailsGroup.getCouponGroup().stream()).
                            flatMap(couponGroup -> couponGroup.getCouponInfo().getCouponDetails().stream()).
                            anyMatch(couponInformationDetailsTypeI -> couponInformationDetailsTypeI.getCpnStatus().equalsIgnoreCase("I"));
                    if (notUsed) {
                        //process initRefund.
                        amaTicketInitRefundRS = refundServiceHandler.ticketInitRefund(refundticketList, amadeusSessionWrapper,searchOfficeId);
                        if (amaTicketInitRefundRS != null && amaTicketInitRefundRS.getGeneralReply().getErrors() == null) {
                            //Process Refund
                            amaTicketProcessRefundRS = refundServiceHandler.ticketProcessRefund(amadeusSessionWrapper);
                            if(amaTicketProcessRefundRS != null && amaTicketProcessRefundRS.getGeneralReply().getErrors() == null){
                                List<AMATicketProcessRefundRS.FunctionalData.ContractBundle> contractBundles = amaTicketProcessRefundRS.getFunctionalData().getContractBundle();
                                for(AMATicketProcessRefundRS.FunctionalData.ContractBundle contractBundle:contractBundles){
                                    List<RefundDetailsLightType.Contracts.Contract> contracts = contractBundle.getRefundDetails().getContracts().getContract();
                                    for(RefundDetailsLightType.Contracts.Contract contract:contracts){
                                        totalRefundable = totalRefundable.add(contract.getRefundable().getAmount());
                                        ticketProcessRefundRes.setCurrency(contract.getRefundable().getCurrencyCode());
                                        List<DocumentAndCouponInformationType> documentAndCouponInformations = contract.getDocumentAndCouponInformation();
                                        for(DocumentAndCouponInformationType documentAndCouponInformation : documentAndCouponInformations ){
                                            refundedTickets.add(documentAndCouponInformation.getDocumentNumber().getNumber().toString());
                                        }
                                    }
                                }

                                ticketProcessRefundRes.setRefundableAmount(calculateTotalRefund(totalRefundable).toString());
                                ticketProcessRefundRes.setStatus(Boolean.TRUE);
                                ticketProcessRefundRes.setRefTicketsList(refundedTickets);
                            }
                            //split the ticket

                        } else {
                            AMATicketIgnoreRefundRS amaTicketIgnoreRefundRS = refundServiceHandler.ticketIgnoreRefundRQ(amadeusSessionWrapper);
                            pnrReply = serviceHandler.ignorePNRAddMultiElement(amadeusSessionWrapper);
                            ticketProcessRefundRes.setStatus(Boolean.FALSE);
                        }
                    }else{
                        Boolean airportControl = ticketProcessEDocReply.getDocGroup().stream().flatMap(docGroup ->
                                        docGroup.getDocDetailsGroup().stream()).flatMap(docDetailsGroup -> docDetailsGroup.getCouponGroup().stream()).
                                flatMap(couponGroup -> couponGroup.getCouponInfo().getCouponDetails().stream()).
                                allMatch(couponInformationDetailsTypeI -> couponInformationDetailsTypeI.getCpnStatus().equalsIgnoreCase("AL"));
                        Boolean used = ticketProcessEDocReply.getDocGroup().stream().flatMap(docGroup ->
                                        docGroup.getDocDetailsGroup().stream()).flatMap(docDetailsGroup -> docDetailsGroup.getCouponGroup().stream()).
                                flatMap(couponGroup -> couponGroup.getCouponInfo().getCouponDetails().stream()).
                                allMatch(couponInformationDetailsTypeI -> couponInformationDetailsTypeI.getCpnStatus().equalsIgnoreCase("B"));
                        Boolean refunded = ticketProcessEDocReply.getDocGroup().stream().flatMap(docGroup ->
                                        docGroup.getDocDetailsGroup().stream()).flatMap(docDetailsGroup -> docDetailsGroup.getCouponGroup().stream()).
                                flatMap(couponGroup -> couponGroup.getCouponInfo().getCouponDetails().stream()).
                                allMatch(couponInformationDetailsTypeI -> couponInformationDetailsTypeI.getCpnStatus().equalsIgnoreCase("RF"));
                        ErrorMessage errorMessage = new ErrorMessage();
                        errorMessage.setGdsPNR(gdsPnr);
                        if(airportControl)
                            errorMessage.setMessage("Check with Airport Control. Refund is not possible");
                        if(used)
                            errorMessage.setMessage("Ticket is Used. Refund is not possible");
                        if(refunded)
                            errorMessage.setMessage("Ticket is Refunded");
                        ticketProcessRefundRes.setMessage(errorMessage);
                        ticketProcessRefundRes.setStatus(Boolean.FALSE);

                    }

                }
            } else if (pnrReply != null && pnrReply.getGeneralErrorInfo().size() > 0) {
                pnrReply = serviceHandler.ignorePNRAddMultiElement(amadeusSessionWrapper);
                ticketProcessRefundRes.setStatus(Boolean.FALSE);
            }
        } catch (Exception e) {
            logger.debug("An exception occured during CheckEligibility of TicketRefund"+ e.getMessage() );
            ErrorMessage errorMessage = new ErrorMessage();
            errorMessage.setProvider("Amadeus");
            errorMessage.setGdsPNR(gdsPnr);
            errorMessage.setMessage(e.getMessage());
            ticketProcessRefundRes.setMessage(errorMessage);
            ticketProcessRefundRes.setStatus(Boolean.FALSE);
            e.printStackTrace();

        }finally {
            serviceHandler.logOut(amadeusSessionWrapper);
        }
        return ticketProcessRefundRes;
    }


    public Boolean isPNRActive(List<PNRReply.OriginDestinationDetails> originDestinationDetailsList){
        Boolean active = originDestinationDetailsList.stream().flatMap(
                        originDestinationDetails ->originDestinationDetails.getItineraryInfo().stream()).
                anyMatch(iternaryInfo ->iternaryInfo.getRelatedProduct().getStatus().get(0).toString().equalsIgnoreCase("HK"));

        return active;
    }

    public List<String> getTicketList(List<PNRReply.DataElementsMaster.DataElementsIndiv> dataElementsIndivList){
        List<String> ticketsList =  dataElementsIndivList.stream().flatMap(dataElementsIndiv -> dataElementsIndiv.getOtherDataFreetext().stream()).
                filter(longFreeTextType -> longFreeTextType.getFreetextDetail().getType().equalsIgnoreCase("P06"))
                .map(longFreeTextType -> longFreeTextType.getLongFreetext().toString()).collect(Collectors.toList());

        List<String> finalticketsList = new ArrayList<>();
        for(String ticket : ticketsList){
            String[] arraayStr = ticket.split("/");
            String[] data = arraayStr[0].split(" ");
            String ticketnumber = data[1].replace("-","");
            finalticketsList.add(ticketnumber);
        }

        return finalticketsList;
    }

    public String getTicketListwithAL(TicketProcessEDocReply ticketProcessEDocReply){
        String tickets = " ";
        List<TicketProcessEDocReply.DocGroup.DocDetailsGroup> docDetailsGroupList = ticketProcessEDocReply.getDocGroup().stream().flatMap(docGroup ->
                docGroup.getDocDetailsGroup().stream()).collect(Collectors.toList());
        for(TicketProcessEDocReply.DocGroup.DocDetailsGroup docDetailsGroup:docDetailsGroupList){
            for(TicketProcessEDocReply.DocGroup.DocDetailsGroup.CouponGroup coupongroup : docDetailsGroup.getCouponGroup()){
                List<CouponInformationDetailsTypeI> couponDetails = coupongroup.getCouponInfo().getCouponDetails();
                for(CouponInformationDetailsTypeI couponInformationDetailsTypeI :couponDetails){
                    if(couponInformationDetailsTypeI.getCpnStatus().equalsIgnoreCase("AL")){
                        tickets = tickets + docDetailsGroup.getDocInfo().getDocumentDetails().getNumber() +" ";
                    }
                }
            }
        }
     return tickets;
    }

    public String getUsedTickets(TicketProcessEDocReply ticketProcessEDocReply){
        String tickets = " ";
        List<TicketProcessEDocReply.DocGroup.DocDetailsGroup> docDetailsGroupList = ticketProcessEDocReply.getDocGroup().stream().flatMap(docGroup ->
                docGroup.getDocDetailsGroup().stream()).collect(Collectors.toList());
        for(TicketProcessEDocReply.DocGroup.DocDetailsGroup docDetailsGroup:docDetailsGroupList){
            for(TicketProcessEDocReply.DocGroup.DocDetailsGroup.CouponGroup coupongroup : docDetailsGroup.getCouponGroup()){
                List<CouponInformationDetailsTypeI> couponDetails = coupongroup.getCouponInfo().getCouponDetails();
                for(CouponInformationDetailsTypeI couponInformationDetailsTypeI :couponDetails){
                    if(couponInformationDetailsTypeI.getCpnStatus().equalsIgnoreCase("B")){
                        tickets = tickets + docDetailsGroup.getDocInfo().getDocumentDetails().getNumber() +" ";
                    }
                }
            }
        }
        return tickets;
    }
    /*
    1) In case of Non Seaman wherein there are cancellations charges, the calculations will be as below
 Refund Amt Rs 9000 + RAF Rs 300 = INR 9300 + Tax 18% = Rs 10974/-

2) In case of Seaman wherein there are no cancellation charges, the calculations will be as below
Refund Amt NIL + RAF Rs 300 = INR 300 + Tax 18% = Rs 354/-

     */

    public BigDecimal calculateTotalRefund(BigDecimal totalRefund){
        BigDecimal rafAmount = new BigDecimal( Play.application().configuration().getInt("amadeus.refundRAF"));
        BigDecimal taxRate = new BigDecimal( Play.application().configuration().getDouble("amadeus.refundTaxRate"));
        BigDecimal totalBeforeTax = totalRefund.add(rafAmount);
        BigDecimal taxAmount = totalBeforeTax.multiply(taxRate);
        totalBeforeTax = totalBeforeTax.add(taxAmount);
      return totalBeforeTax;
    }
}
