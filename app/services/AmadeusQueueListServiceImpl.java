package services;

import com.amadeus.xml.pnracc_14_1_1a.PNRReply;
import com.amadeus.xml.pnracc_14_1_1a.PNRReply.OriginDestinationDetails.ItineraryInfo;
import com.amadeus.xml.qdqlrr_11_1_1a.QueueListReply;
import com.amadeus.xml.qdqlrr_11_1_1a.QueueListReply.QueueView.Item;
import com.compassites.GDSWrapper.amadeus.QueueListReq;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.constants.AmadeusConstants;
import com.compassites.model.QueuePNR;
import com.fasterxml.jackson.databind.JsonNode;
import models.AmadeusSessionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import play.libs.F;
import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Created by ritesh on 9/10/15.
 */
@Service
public class AmadeusQueueListServiceImpl implements QueueListService {

    Logger gdsLogger = LoggerFactory.getLogger("gds");

    @Autowired
    private ServiceHandler serviceHandler;

    @Override
    public QueueListReply getWaitListConfirmRequest() {
        gdsLogger.debug("getWaitListConfirmRequest called ........");
//        ServiceHandler serviceHandler = null;
//        try {
//            serviceHandler = new ServiceHandler();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        AmadeusSessionWrapper amadeusSessionWrapper = serviceHandler.logIn(true);

        QueueListReply queueListReply =  serviceHandler.queueListResponse(QueueListReq.getWaitListConfirmRequest(), amadeusSessionWrapper);

        List<QueuePNR> pnrList = new ArrayList<>();
        if(queueListReply.getQueueView() == null){
            return null;
        }
        for (Item item : queueListReply.getQueueView().getItem()){
            String pnr = item.getRecLoc().getReservation().getControlNumber();
            gdsLogger.debug("PNR Number returned =========>>> : " + pnr);
            PNRReply pnrReply = null;
            try {

                pnrReply = serviceHandler.retrievePNR(pnr, amadeusSessionWrapper);
                for(PNRReply.OriginDestinationDetails originDestinationDetails : pnrReply.getOriginDestinationDetails()){
                    for(ItineraryInfo itineraryInfo : originDestinationDetails.getItineraryInfo()){
                        String segmentStatus = itineraryInfo.getRelatedProduct().getStatus().get(0);
                        String confirmWaitListStatus = AmadeusConstants.SEGMENT_STATUS.CONFIRMAT_WAITLIST.getSegmentStatus();
                        if(confirmWaitListStatus.equalsIgnoreCase(segmentStatus)){
                            QueuePNR queuePNR = new QueuePNR();
                            queuePNR.setPnr(pnr);
                            queuePNR.setQueueType(AmadeusConstants.QUEUE_TYPE.CONFIRMATION);
                            pnrList.add(queuePNR);
                        }

                        gdsLogger.debug("Status of the segment : " + segmentStatus);
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
                gdsLogger.error("error in  getWaitListConfirmRequest ........" , e);
            }

        }

        String url =  play.Play.application().configuration().getString("gds.jocservice.url")+"notifyScheduleChange";
        final JsonNode jsonRequest = Json.toJson(pnrList);
        WS.url(url).post(jsonRequest).map(
                new F.Function<WSResponse, JsonNode>() {
                    public JsonNode apply(WSResponse response) {
                        JsonNode json = null;
                        if (response != null) {
                            json = response.asJson();
                        }
                        return json;
                    }
                }
        );
        return queueListReply;
    }

    @Override
    public QueueListReply getSegmentWaitListConfirmReq() {
        gdsLogger.debug("getWaitListConfirmRequest called ........");
        //ServiceHandler serviceHandler = null;
        AmadeusSessionWrapper amadeusSessionWrapper = null;
//        try {
//            serviceHandler = new ServiceHandler();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        amadeusSessionWrapper = serviceHandler.logIn(true);
        QueueListReply queueListReply =  serviceHandler.queueListResponse(QueueListReq.getSegmentWaitListConfirmReq(), amadeusSessionWrapper);
        if(queueListReply.getQueueView() == null){
            return null;
        }
        List<QueuePNR> pnrList = new ArrayList<>();

        for (Item item : queueListReply.getQueueView().getItem()){
            String pnr = item.getRecLoc().getReservation().getControlNumber();
            gdsLogger.debug("PNR Number returned =========>>> : " + pnr);
            try {
                PNRReply pnrReply = serviceHandler.retrievePNR(pnr, amadeusSessionWrapper);
                EnumSet<AmadeusConstants.CONFIRMATION_SEGMENT_STATUS> segmentStatuses = EnumSet.allOf(AmadeusConstants.CONFIRMATION_SEGMENT_STATUS.class);
                for (PNRReply.OriginDestinationDetails originDestinationDetails : pnrReply.getOriginDestinationDetails()) {
                    for (ItineraryInfo itineraryInfo : originDestinationDetails.getItineraryInfo()) {
                        String segmentStatus = itineraryInfo.getRelatedProduct().getStatus().get(0);
                        if (AmadeusConstants.CONFIRMATION_SEGMENT_STATUS.contains(segmentStatus)) {
                            QueuePNR queuePNR = new QueuePNR();
                            queuePNR.setPnr(pnr);
                            queuePNR.setQueueType(AmadeusConstants.QUEUE_TYPE.SEGMENT_CONFIRMATION);
                            pnrList.add(queuePNR);
                        }

                        gdsLogger.debug("Status of the segment : " + segmentStatus);
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
                gdsLogger.error("error in  getSegmentWaitListConfirmReq ........", e);
            }
        }

        String url =  play.Play.application().configuration().getString("gds.jocservice.url")+"notifyScheduleChange";
        final JsonNode jsonRequest = Json.toJson(pnrList);
        WS.url(url).post(jsonRequest).map(
                new F.Function<WSResponse, JsonNode>() {
                    public JsonNode apply(WSResponse response) {
                        JsonNode json = null;
                        if (response != null) {
                            json = response.asJson();
                        }
                        return json;
                    }
                }
        );
        return queueListReply;
    }

    @Override
    public QueueListReply getScheduleChange() {
        gdsLogger.debug("getScheduleChange called ........");
//        ServiceHandler serviceHandler = null;
//        try {
//            serviceHandler = new ServiceHandler();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        AmadeusSessionWrapper amadeusSessionWrapper = serviceHandler.logIn(true);
        QueueListReply queueListReply =  serviceHandler.queueListResponse(QueueListReq.getScheduleChangesRequest(),amadeusSessionWrapper);
        if(queueListReply.getQueueView() == null){
            return null;
        }
        List<QueuePNR> pnrList = new ArrayList<>();

        for (Item item : queueListReply.getQueueView().getItem()){
            String pnr = item.getRecLoc().getReservation().getControlNumber();
            gdsLogger.debug("PNR Number returned =========>>> : " + pnr);

            try {
                PNRReply pnrReply = serviceHandler.retrievePNR(pnr, amadeusSessionWrapper);

                for (PNRReply.OriginDestinationDetails originDestinationDetails : pnrReply.getOriginDestinationDetails()) {
                    for (ItineraryInfo itineraryInfo : originDestinationDetails.getItineraryInfo()) {

                        String segmentStatus = itineraryInfo.getRelatedProduct().getStatus().get(0);
                        String confirmWaitListStatus = AmadeusConstants.SEGMENT_STATUS.SCHEDULE_CHANGE.getSegmentStatus();
                        if (confirmWaitListStatus.equalsIgnoreCase(segmentStatus)) {

                            QueuePNR queuePNR = new QueuePNR();
                            queuePNR.setPnr(pnr);
                            queuePNR.setQueueType(AmadeusConstants.QUEUE_TYPE.SCHEDULE_CHANGE);
                            pnrList.add(queuePNR);
                        }
                        gdsLogger.debug("Status of the segment : " + segmentStatus);
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
                gdsLogger.error("error in  getScheduleChange ........", e);
            }
        }

        String url =  play.Play.application().configuration().getString("gds.jocservice.url")+"notifyScheduleChange";
        final JsonNode jsonRequest = Json.toJson(pnrList);
        WS.url(url).post(jsonRequest).map(
                new F.Function<WSResponse, JsonNode>() {
                    public JsonNode apply(WSResponse response) {
                        JsonNode json = null;
                        if (response != null) {
                            json = response.asJson();
                        }
                        return json;
                    }
                }
        );
        return queueListReply;
    }


    @Override
    public QueueListReply getExpiryTimeQueueRequest() {
        gdsLogger.debug("getExpiryTimeQueueRequest called ........");
        //ServiceHandler serviceHandler = null;
        AmadeusSessionWrapper amadeusSessionWrapper = null;
//        try {
//            serviceHandler = new ServiceHandler();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        amadeusSessionWrapper = serviceHandler.logIn(true);
        QueueListReply queueListReply =  serviceHandler.queueListResponse(QueueListReq.getExpiryTimeRequest(), amadeusSessionWrapper);
        if(queueListReply.getQueueView() == null){
            return null;
        }
        List<QueuePNR> pnrList = new ArrayList<>();

        for (Item item : queueListReply.getQueueView().getItem()){
            String pnr = item.getRecLoc().getReservation().getControlNumber();
            gdsLogger.debug("PNR Number returned =========>>> : " + pnr);
            try {
                PNRReply pnrReply = serviceHandler.retrievePNR(pnr, amadeusSessionWrapper);

                for (PNRReply.OriginDestinationDetails originDestinationDetails : pnrReply.getOriginDestinationDetails()) {
                    for (ItineraryInfo itineraryInfo : originDestinationDetails.getItineraryInfo()) {
                        String segmentStatus = itineraryInfo.getRelatedProduct().getStatus().get(0);
                        String confirmWaitListStatus = AmadeusConstants.SEGMENT_STATUS.EXPIRED_TIME_LIMIT.getSegmentStatus();
//                        if (confirmWaitListStatus.equalsIgnoreCase(segmentStatus)) {
                            QueuePNR queuePNR = new QueuePNR();
                            queuePNR.setPnr(pnr);
                            queuePNR.setQueueType(AmadeusConstants.QUEUE_TYPE.EXPIRE_TIME_LIMIT);
                            pnrList.add(queuePNR);
//                        }
                        gdsLogger.debug("Status of the segment : " + segmentStatus);
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
                gdsLogger.error("error in  getExpiryTimeQueueRequest ........", e);
            }

        }

        String url =  play.Play.application().configuration().getString("gds.jocservice.url")+"notifyScheduleChange";
        final JsonNode jsonRequest = Json.toJson(pnrList);
        WS.url(url).post(jsonRequest).map(
                new F.Function<WSResponse, JsonNode>() {
                    public JsonNode apply(WSResponse response) {
                        JsonNode json = null;
                        if (response != null) {
                            json = response.asJson();
                        }
                        return json;
                    }
                }
        );
        return queueListReply;
    }

}
