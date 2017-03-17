package com.compassites.GDSWrapper.mystifly;

import com.compassites.model.*;
import onepoint.mystifly.*;
import onepoint.mystifly.MessageQueuesDocument.MessageQueues;
import org.datacontract.schemas._2004._07.mystifly_onepoint.*;
import org.datacontract.schemas._2004._07.mystifly_onepoint.Item;
import utils.XMLFileUtility;

import java.rmi.RemoteException;

/**
 * Created by Satish Kumar on 14-10-2016.
 */
public class AirMessageQueue {
    public AirMessageQueueRS addMessage() throws RemoteException {
        SessionsHandler sessionsHandler = new SessionsHandler();
        OnePointStub onePointStub = sessionsHandler.getOnePointStub();
        //String sessoinId = sessionsHandler.mystiflySessionHandler();
        SessionCreateRS sessionRS = sessionsHandler.login();
        MessageQueuesDocument msgQueue = MessageQueuesDocument.Factory.newInstance();
        MessageQueues messageQueues = msgQueue.addNewMessageQueues();
        AirMessageQueueRQ airMessageQueueRQ = messageQueues.addNewRq();
        airMessageQueueRQ.setSessionId(sessionRS.getSessionId());
        airMessageQueueRQ.setTarget(Mystifly.TARGET);
        airMessageQueueRQ.setCategoryId(QueueCategory.TICKETED);
        //XMLFileUtility.createFile(airMessageQueueRQ.xmlText(), "AiMessageRQ.xml");
        MessageQueuesResponseDocument messageQueuesResponseDocument = onePointStub.messageQueues(msgQueue);
       // XMLFileUtility.createFile(messageQueuesResponseDocument.xmlText(), "AiMessageRS.xml");
        return messageQueuesResponseDocument.getMessageQueuesResponse().getMessageQueuesResult();
    }

    public AirRemoveMessageQueueRS removeMessageQueueRQ(String pnr) throws RemoteException{
        SessionsHandler sessionsHandler = new SessionsHandler();
        OnePointStub onePointStub = sessionsHandler.getOnePointStub();
        SessionCreateRS sessionRS = sessionsHandler.login();
        RemoveMessageQueuesDocument removeMessageQueuesDocument = RemoveMessageQueuesDocument.Factory.newInstance();
        RemoveMessageQueuesDocument.RemoveMessageQueues removeMessageQueues = removeMessageQueuesDocument.addNewRemoveMessageQueues();
        AirRemoveMessageQueueRQ airRemoveMessageQueueRQ = removeMessageQueues.addNewRq();
        ArrayOfItem arrayOfItem = airRemoveMessageQueueRQ.addNewItems();
        Item item = arrayOfItem.addNewItem();
        item.setCategoryId(QueueCategory.TICKETED);
        item.setUniqueId(pnr);
        arrayOfItem.setItemArray(0, item);
        airRemoveMessageQueueRQ.setSessionId(sessionRS.getSessionId());
        airRemoveMessageQueueRQ.setTarget(Mystifly.TARGET);
        XMLFileUtility.createFile(removeMessageQueuesDocument.xmlText(), "AirRemoveMessageRQ.xml");
        RemoveMessageQueuesResponseDocument removeMessageQueuesResponseDocument = onePointStub.removeMessageQueues(removeMessageQueuesDocument);
        XMLFileUtility.createFile(removeMessageQueuesResponseDocument.xmlText(), "AirRemoveMessageRS.xml");
        return removeMessageQueuesResponseDocument.getRemoveMessageQueuesResponse().getRemoveMessageQueuesResult();
        //airRemoveMessageQueueRQ.addNewItems().setItemArray();
        //MessageQueues messageQueues = msgQueue.addNewMessageQueues();
        // RemoveMessageQueuesDocument.RemoveMessageQueues removeMessageQueues = msgQueue.addNewMessageQueues();
        //RemoveMessageQueuesDocument.RemoveMessageQueues removeMessageQueues = messageQueues.getRq();
    }
}
