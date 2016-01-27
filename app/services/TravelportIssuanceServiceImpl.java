package services;

import com.compassites.GDSWrapper.travelport.AirTicketClient;
import com.compassites.model.IssuanceRequest;
import com.compassites.model.IssuanceResponse;
import com.travelport.schema.air_v26_0.AirTicketingRsp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Created by yaseen on 26-01-2016.
 */
@Service
public class TravelportIssuanceServiceImpl {

    static Logger logger = LoggerFactory.getLogger("gds");


    public IssuanceResponse priceBookedPNR(IssuanceRequest issuanceRequest) {
        logger.debug("======================= travelport  pricePNR called =========================");

        IssuanceResponse issuanceResponse = new IssuanceResponse();
        AirTicketClient airTicketClient = new AirTicketClient();
        AirTicketingRsp airTicketingRsp = airTicketClient
                .issueTicket(issuanceRequest.getGdsPNR());

        return null;
    }

}


