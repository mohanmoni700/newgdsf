package services;

import com.amadeus.xml.qdqlrr_11_1_1a.QueueListReply;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by ritesh on 9/10/15.
 */
@Service
public class QueueListServiceWrapper {

    @Autowired
    private AmadeusQueueListServiceImpl amadeusQueueListService;

    public QueueListReply getQueueListResponse(){
        return amadeusQueueListService.getQueueResponse();
    }
}
