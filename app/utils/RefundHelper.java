package utils;

import com.amadeus.xml._2010._06.ticketgtp_v3.AMATicketInitRefundRS;
import com.amadeus.xml._2010._06.tickettypes_v2.*;
import dto.refund.*;
import org.opentravel.ota._2003._05.ota2010b.CommissionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class RefundHelper {

    private static final Logger logger = LoggerFactory.getLogger("gds");

    public List<PerPaxRefundPricingInformation> getRefundedPerPaxPricingInformation(List<AMATicketInitRefundRS.FunctionalData.ContractBundle> contractBundleList) {

        List<PerPaxRefundPricingInformation> perPaxRefundPricingInformationList = new ArrayList<>();

        try {

            for (AMATicketInitRefundRS.FunctionalData.ContractBundle contractBundle : contractBundleList) {
                PerPaxRefundPricingInformation perPaxRefundPricingInformation = new PerPaxRefundPricingInformation();

                RefundDetailsType.Contracts contracts = contractBundle.getRefundDetails().getContracts();
                List<RefundDetailsType.Contracts.Contract> contractList = contracts.getContract();

                for (RefundDetailsType.Contracts.Contract contract : contractList) {

                    RefundDetailsType.Contracts.Contract.Segments segments = contract.getSegments();
                    RefundDetailsType.Contracts.Contract.Passengers passengers = contract.getPassengers();
                    RefundDetailsType.Contracts.Contract.Taxes taxes = contract.getTaxes();
                    RefundDetailsType.Contracts.Contract.Penalties penalties = contract.getPenalties();
                    RefundDetailsType.Contracts.Contract.Commissions commissions = contract.getCommissions();
                    RefundDetailsType.Contracts.Contract.MonetaryInformations monetaryInformations = contract.getMonetaryInformations();
                    RefundDetailsType.Contracts.Contract.RefundedRoute refundedRoute = contract.getRefundedRoute();
                    RefundDetailsType.Contracts.Contract.Refundable refundable = contract.getRefundable();
                    List<DocumentAndCouponInformationType> documentAndCouponInformation = contract.getDocumentAndCouponInformation();

                    //Setting segment tattoos for refunded segments here
                    List<BigInteger> refundedSegmentTattoos = new ArrayList<>();
                    if(segments!=null && !segments.getSegment().isEmpty()) {
                        List<RefundedItineraryType> refundedItineraryTypeList = segments.getSegment();
                        for (RefundedItineraryType refundedSegmentTattoo : refundedItineraryTypeList) {
                            refundedSegmentTattoos.add(BigInteger.valueOf(refundedSegmentTattoo.getTattoo()));
                        }
                    }
                    perPaxRefundPricingInformation.setRefundedSegmentTattoos(refundedSegmentTattoos);

                    //Setting Pax Info here
                    PassengerType passengerType = passengers.getPassenger().get(0);

                    String paxFullName = passengerType.getFullName();
                    BigInteger paxTattoo = BigInteger.valueOf(passengerType.getTattoo());
                    perPaxRefundPricingInformation.setFullName(paxFullName);
                    perPaxRefundPricingInformation.setPaxTattoo(paxTattoo);

                    //Setting Detailed Tax Details here
                    List<DetailedTaxInformation> detailedTaxInformationList = new ArrayList<>();
                    if(taxes!=null && !taxes.getTax().isEmpty()) {

                        List<TaxType> taxTypeList = taxes.getTax();
                        for (TaxType taxType : taxTypeList) {
                            DetailedTaxInformation detailedTaxInformation = new DetailedTaxInformation();

                            BigDecimal amount = taxType.getAmount();
                            String currency = taxType.getCurrencyCode();
                            BigInteger decimalPlaces = BigInteger.valueOf(taxType.getDecimalPlaces().longValue());
                            String category = taxType.getCategory();
                            String isoCode = taxType.getISOCode();


                            detailedTaxInformation.setAmount(amount);
                            detailedTaxInformation.setCurrency(currency);
                            detailedTaxInformation.setDecimalPlaces(decimalPlaces);
                            detailedTaxInformation.setCategory(category);
                            detailedTaxInformation.setIsoCode(isoCode);

                            detailedTaxInformationList.add(detailedTaxInformation);

                        }
                    }
                    perPaxRefundPricingInformation.setDetailedTaxInformationList(detailedTaxInformationList);

                    //Setting Detailed penalties here
                    List<DetailedPenaltyInformation> detailedPenaltyInformationList = new ArrayList<>();
                    if(penalties!=null && !penalties.getPenalty().isEmpty()) {

                        List<PenaltyType> penaltyList = penalties.getPenalty();
                        for (PenaltyType penalty : penaltyList) {
                            DetailedPenaltyInformation detailedPenaltyInformation = new DetailedPenaltyInformation();

                            BigDecimal percent = penalty.getPercent();
                            BigDecimal amount = penalty.getAmount();
                            String currency = penalty.getCurrencyCode();
                            BigInteger decimalPlaces = BigInteger.valueOf(penalty.getDecimalPlaces().longValue());
                            String penaltyType = penalty.getPenaltyType();

                            detailedPenaltyInformation.setPercent(percent);
                            detailedPenaltyInformation.setAmount(amount);
                            detailedPenaltyInformation.setCurrency(currency);
                            detailedPenaltyInformation.setDecimalPlaces(decimalPlaces);
                            detailedPenaltyInformation.setPenaltyType(penaltyType);

                            detailedPenaltyInformationList.add(detailedPenaltyInformation);
                        }
                    }
                    perPaxRefundPricingInformation.setDetailedPenaltyInformationList(detailedPenaltyInformationList);

                    //Setting Commission Details here
                    List<DetailedCommissionInformation> detailedCommissionInformationList = new ArrayList<>();
                    if(commissions!=null && !commissions.getCommission().isEmpty()) {

                        List<CommissionType> commissionList = commissions.getCommission();
                        for (CommissionType commission : commissionList) {
                            DetailedCommissionInformation detailedCommissionInformation = new DetailedCommissionInformation();
                            BigDecimal percent = commission.getPercent();
                            String comment = commission.getComment().getName();

                            CommissionType.CommissionPayableAmount commissionPayableAmount = commission.getCommissionPayableAmount();
                            BigDecimal amount = commissionPayableAmount.getAmount();
                            String currency = commissionPayableAmount.getCurrencyCode();
                            BigInteger decimalPlaces = BigInteger.valueOf(commissionPayableAmount.getDecimalPlaces().longValue());

                            detailedCommissionInformation.setPercent(percent);
                            detailedCommissionInformation.setAmount(amount);
                            detailedCommissionInformation.setCurrency(currency);
                            detailedCommissionInformation.setDecimalPlaces(decimalPlaces);
                            detailedCommissionInformation.setComment(comment);

                            detailedCommissionInformationList.add(detailedCommissionInformation);
                        }
                    }
                    perPaxRefundPricingInformation.setDetailedCommissionInformationList(detailedCommissionInformationList);

                    //Setting Monetary Information Details here
                    List<DetailedMonetaryInformation> detailedMonetaryInformationList = new ArrayList<>();
                    if(monetaryInformations!= null && !monetaryInformations.getMonetaryInformation().isEmpty()) {

                        List<MonetaryInformationType> monetaryInformationList = monetaryInformations.getMonetaryInformation();
                        for (MonetaryInformationType monetaryInformation : monetaryInformationList) {
                            DetailedMonetaryInformation detailedMonetaryInformation = new DetailedMonetaryInformation();

                            String qualifier = monetaryInformation.getQualifier();
                            BigDecimal amount = monetaryInformation.getAmount();
                            String currency = monetaryInformation.getCurrencyCode();
                            //Weirdest error ever!!
                            BigInteger decimalPlaces = BigInteger.valueOf(monetaryInformation.getDecimalPlaces().longValue());

                            detailedMonetaryInformation.setQualifier(qualifier);
                            detailedMonetaryInformation.setAmount(amount);
                            detailedMonetaryInformation.setCurrency(currency);
                            detailedMonetaryInformation.setDecimalPlaces(decimalPlaces);

                            detailedMonetaryInformationList.add(detailedMonetaryInformation);
                        }
                    }
                    perPaxRefundPricingInformation.setDetailedMonetaryInformationList(detailedMonetaryInformationList);

                    //Segment String here
                    StringBuilder refundedSegmentString = new StringBuilder();
                    if(refundedRoute!=null && !refundedRoute.getStation().isEmpty()) {
                        List<String> stationList = refundedRoute.getStation();
                        int count = 0;
                        for (String station : stationList) {
                            refundedSegmentString.append(station);
                            if (stationList.get(count + 1) != null) {
                                refundedSegmentString.append("-");
                            }
                        }
                    }
                    perPaxRefundPricingInformation.setRefundedSegmentString(refundedSegmentString.toString());

                    //Total Refundable details here
                    BigDecimal refundableAmount = refundable.getAmount();
                    String refundableCurrency = refundable.getCurrencyCode();

                    perPaxRefundPricingInformation.setRefundedAmount(refundableAmount);
                    perPaxRefundPricingInformation.setRefundedAmountCurrency(refundableCurrency);


                    //Setting ticket number here
                    DocumentAndCouponInformationType documentAndCouponInformationType = documentAndCouponInformation.get(0);
                    String ticketNumber = insertHyphenAfterThreeChars(documentAndCouponInformationType.getDocumentNumber().getNumber());
                    perPaxRefundPricingInformation.setTicketNumber(ticketNumber);

                }

                perPaxRefundPricingInformationList.add(perPaxRefundPricingInformation);
            }
        } catch (Exception e) {
            logger.debug("Error while extracting reissue pricing information : {} ", e.getMessage(), e);
            e.printStackTrace();
        }

        return perPaxRefundPricingInformationList;
    }



    public static String insertHyphenAfterThreeChars(String input) {
        return input.substring(0, 3) + "-" + input.substring(3);
    }

    private static List<String> getFirstName0Salutation1(String firstName) {
        return Arrays.asList(firstName.split(" "));
    }

}





