package services;

import com.amadeus.xml.qdqlrr_11_1_1a.QueueListReply;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by ritesh on 9/10/15.
 */
@Service
public class QueueListServiceWrapper {


    private AmadeusQueueListServiceImpl amadeusQueueListService;

    public AmadeusQueueListServiceImpl getAmadeusQueueListService() {
        return amadeusQueueListService;
    }


    @Autowired
    public void setAmadeusQueueListService(AmadeusQueueListServiceImpl amadeusQueueListService) {
        this.amadeusQueueListService = amadeusQueueListService;
    }

    public QueueListReply getQueueListResponse(){
        return amadeusQueueListService.getQueueResponse();
    }
}
