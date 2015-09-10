package services;


import com.amadeus.xml.qdqlrr_11_1_1a.QueueListReply;
import com.compassites.GDSWrapper.amadeus.QueueListReq;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by ritesh on 9/10/15.
 */
@Service
public class AmadeusQueueListServiceImpl implements QueueListService {

    private ServiceHandler serviceHandler;

    public AmadeusQueueListServiceImpl(){
        try {
            serviceHandler = new ServiceHandler();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public QueueListReply getQueueResponse() {
        return serviceHandler.queueListResponse(QueueListReq.getQueueNum_1_Request());
    }
}
