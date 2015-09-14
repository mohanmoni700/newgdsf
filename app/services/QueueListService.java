package services;

import com.amadeus.xml.qdqlrr_11_1_1a.QueueListReply;
/**
 * Created by ritesh on 9/10/15.
 */
public interface QueueListService {

    QueueListReply getQueueResponse();

    QueueListReply getScheduleChange();

    QueueListReply getExpiryTimeQueueRequest();
}
