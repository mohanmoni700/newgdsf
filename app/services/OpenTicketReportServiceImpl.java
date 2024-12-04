package services;

import com.amadeus.xml.tatreq_20_1_1a.*;
import com.amadeus.xml.tatres_20_1_1a.CouponInformationDetailsTypeI;
import com.amadeus.xml.tatres_20_1_1a.TicketProcessEDocReply;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import dto.OpenTicketDTO;
import dto.OpenTicketResponse;
import models.AmadeusSessionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import play.libs.Json;

import java.util.ArrayList;
import java.util.List;

@Service
public class OpenTicketReportServiceImpl implements OpenTicketReportService {

    @Autowired
    private ServiceHandler serviceHandler;

    private static final Logger logger = LoggerFactory.getLogger("gds");

    @Override
    public List<OpenTicketResponse> openTicketReport(List<OpenTicketDTO> ticketDTOList) {
        TicketProcessEDocReply ticketProcessEDocReply = null;
        AmadeusSessionWrapper amadeusSessionWrapper = null;
        List<OpenTicketResponse> openTicketResponse = null;
        try {
            logger.debug("openTicketReport request "+ Json.toJson(ticketDTOList));
            amadeusSessionWrapper = serviceHandler.logIn();
            ticketProcessEDocReply = serviceHandler.ticketProcessEDocReply(amadeusSessionWrapper, ticketDTOList);
            openTicketResponse = createOpenTicketResponse(ticketProcessEDocReply);
        } catch (Exception e) {
            logger.error("Error ",e.getMessage());
            e.printStackTrace();
        }
        return openTicketResponse;
    }

    private List<OpenTicketResponse> createOpenTicketResponse(TicketProcessEDocReply ticketProcessEDocReply) {
        List<OpenTicketResponse> openTicketResponses = new ArrayList<>();
        for (TicketProcessEDocReply.DocGroup docGroup: ticketProcessEDocReply.getDocGroup()) {
            for (TicketProcessEDocReply.DocGroup.DocDetailsGroup docDetailsGroup: docGroup.getDocDetailsGroup()) {
                OpenTicketResponse openTicketResponse1 = new OpenTicketResponse();
                openTicketResponse1.setTicketNumber(docDetailsGroup.getDocInfo().getDocumentDetails().getNumber());
                openTicketResponse1.setStatus(docDetailsGroup.getDocInfo().getDocumentDetails().getDataIndicator());
                for (TicketProcessEDocReply.DocGroup.DocDetailsGroup.CouponGroup couponGroup: docDetailsGroup.getCouponGroup()) {
                    for (CouponInformationDetailsTypeI couponInformationDetailsTypeI: couponGroup.getCouponInfo().getCouponDetails()) {
                        openTicketResponse1.setCpnStatus(couponInformationDetailsTypeI.getCpnStatus());
                        openTicketResponse1.setCpnNumber(couponInformationDetailsTypeI.getCpnNumber());
                    }
                }
                openTicketResponse1.setType(docDetailsGroup.getDocInfo().getDocumentDetails().getType());
                openTicketResponses.add(openTicketResponse1);
            }
        }
        return openTicketResponses;
    }
}
