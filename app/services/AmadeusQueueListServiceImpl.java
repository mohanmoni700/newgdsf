package services;


import com.amadeus.xml.pnracc_11_3_1a.PNRReply;
import com.amadeus.xml.pnracc_11_3_1a.PNRReply.OriginDestinationDetails.ItineraryInfo;
import com.amadeus.xml.qdqlrr_11_1_1a.QueueListReply;
import com.amadeus.xml.qdqlrr_11_1_1a.QueueListReply.QueueView.Item;
import com.compassites.GDSWrapper.amadeus.QueueListReq;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import play.libs.F;
import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ritesh on 9/10/15.
 */
@Service
public class AmadeusQueueListServiceImpl implements QueueListService {

    Logger gdsLogger = LoggerFactory.getLogger("gds");

    @Override
    public QueueListReply getQueueResponse() {
        gdsLogger.debug("getQueueResponse called ........");
        ServiceHandler serviceHandler = null;
        try {
            serviceHandler = new ServiceHandler();
        } catch (Exception e) {
            e.printStackTrace();
        }
        serviceHandler.logIn();
        QueueListReply queueListReply =  serviceHandler.queueListResponse(QueueListReq.getConfirmQueueRequest());

        List<String> pnrList = new ArrayList<>();

        for (Item item : queueListReply.getQueueView().getItem()){
            String pnr = item.getRecLoc().getReservation().getControlNumber();
            gdsLogger.debug("PNR Number returned =========>>> : " + pnr);

            PNRReply pnrReply = serviceHandler.retrivePNR(pnr);

            for(PNRReply.OriginDestinationDetails originDestinationDetails : pnrReply.getOriginDestinationDetails()){
                for(ItineraryInfo itineraryInfo : originDestinationDetails.getItineraryInfo()){
                    String segmenStatus = itineraryInfo.getRelatedProduct().getStatus().get(0);
                    pnrList.add(pnr);
                    gdsLogger.debug("Status of the segment : " + segmenStatus);
                }
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
        ServiceHandler serviceHandler = null;
        try {
            serviceHandler = new ServiceHandler();
        } catch (Exception e) {
            e.printStackTrace();
        }
        serviceHandler.logIn();
        QueueListReply queueListReply =  serviceHandler.queueListResponse(QueueListReq.getScheduleChangesQueueRequest());

        List<String> pnrList = new ArrayList<>();

        for (Item item : queueListReply.getQueueView().getItem()){
            String pnr = item.getRecLoc().getReservation().getControlNumber();
            gdsLogger.debug("PNR Number returned =========>>> : " + pnr);

            PNRReply pnrReply = serviceHandler.retrivePNR(pnr);

            for(PNRReply.OriginDestinationDetails originDestinationDetails : pnrReply.getOriginDestinationDetails()){
                for(ItineraryInfo itineraryInfo : originDestinationDetails.getItineraryInfo()){
                    String segmenStatus = itineraryInfo.getRelatedProduct().getStatus().get(0);
                    pnrList.add(pnr);
                    gdsLogger.debug("Status of the segment : " + segmenStatus);
                }
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
        ServiceHandler serviceHandler = null;
        try {
            serviceHandler = new ServiceHandler();
        } catch (Exception e) {
            e.printStackTrace();
        }
        serviceHandler.logIn();
        QueueListReply queueListReply =  serviceHandler.queueListResponse(QueueListReq.getExpiryTimeQueueRequest());

        List<String> pnrList = new ArrayList<>();

        for (Item item : queueListReply.getQueueView().getItem()){
            String pnr = item.getRecLoc().getReservation().getControlNumber();
            gdsLogger.debug("PNR Number returned =========>>> : " + pnr);

            PNRReply pnrReply = serviceHandler.retrivePNR(pnr);

            for(PNRReply.OriginDestinationDetails originDestinationDetails : pnrReply.getOriginDestinationDetails()){
                for(ItineraryInfo itineraryInfo : originDestinationDetails.getItineraryInfo()){
                    String segmenStatus = itineraryInfo.getRelatedProduct().getStatus().get(0);
                    pnrList.add(pnr);
                    gdsLogger.debug("Status of the segment : " + segmenStatus);
                }
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
