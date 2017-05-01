package com.compassites.GDSWrapper.mystifly;

import com.compassites.model.*;
import onepoint.mystifly.*;
import onepoint.mystifly.MessageQueuesDocument.MessageQueues;
import org.datacontract.schemas._2004._07.mystifly_onepoint.*;
import org.datacontract.schemas._2004._07.mystifly_onepoint.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.XMLFileUtility;

import java.rmi.RemoteException;
import java.util.Date;

/**
 * Created by Satish Kumar on 14-10-2016.
 */
public class AirMessageQueue {
    static Logger mystiflyLogger = LoggerFactory.getLogger("mystifly");
    public AirMessageQueueRS addMessage() throws RemoteException {
        SessionsHandler sessionsHandler = new SessionsHandler();
        OnePointStub onePointStub = sessionsHandler.getOnePointStub();
        String sessoinId = sessionsHandler.mystiflySessionHandler();
        //SessionCreateRS sessionRS = sessionsHandler.login();
        MessageQueuesDocument msgQueue = MessageQueuesDocument.Factory.newInstance();
        MessageQueues messageQueues = msgQueue.addNewMessageQueues();
        AirMessageQueueRQ airMessageQueueRQ = messageQueues.addNewRq();
        airMessageQueueRQ.setSessionId(sessoinId);
        airMessageQueueRQ.setTarget(Mystifly.TARGET);
        airMessageQueueRQ.setCategoryId(QueueCategory.TICKETED);
        XMLFileUtility.createFile(airMessageQueueRQ.xmlText(), "AiMessageRQ.xml");
        mystiflyLogger.debug("AiMessageRQ "+ new Date() +" ----->>" + airMessageQueueRQ.xmlText());
        MessageQueuesResponseDocument messageQueuesResponseDocument = onePointStub.messageQueues(msgQueue);
        mystiflyLogger.debug("AiMessageRS "+ new Date() +" ----->>" + messageQueuesResponseDocument.xmlText());
        XMLFileUtility.createFile(messageQueuesResponseDocument.xmlText(), "AiMessageRS.xml");
        return messageQueuesResponseDocument.getMessageQueuesResponse().getMessageQueuesResult();
    }

    public AirRemoveMessageQueueRS removeMessageQueueRQ(String pnr) throws RemoteException{
        SessionsHandler sessionsHandler = new SessionsHandler();
        OnePointStub onePointStub = sessionsHandler.getOnePointStub();
        String sessoinId = sessionsHandler.mystiflySessionHandler();
        RemoveMessageQueuesDocument removeMessageQueuesDocument = RemoveMessageQueuesDocument.Factory.newInstance();
        RemoveMessageQueuesDocument.RemoveMessageQueues removeMessageQueues = removeMessageQueuesDocument.addNewRemoveMessageQueues();
        AirRemoveMessageQueueRQ airRemoveMessageQueueRQ = removeMessageQueues.addNewRq();
        ArrayOfItem arrayOfItem = airRemoveMessageQueueRQ.addNewItems();
        Item item = arrayOfItem.addNewItem();
        item.setCategoryId(QueueCategory.TICKETED);
        item.setUniqueId(pnr);
        arrayOfItem.setItemArray(0, item);
        airRemoveMessageQueueRQ.setSessionId(sessoinId);
        airRemoveMessageQueueRQ.setTarget(Mystifly.TARGET);
        XMLFileUtility.createFile(removeMessageQueuesDocument.xmlText(), "AirRemoveMessageRQ.xml");
        mystiflyLogger.debug("AirRemoveMessageRQ "+ new Date() +" ----->>" + removeMessageQueuesDocument.xmlText());
        RemoveMessageQueuesResponseDocument removeMessageQueuesResponseDocument = onePointStub.removeMessageQueues(removeMessageQueuesDocument);
        mystiflyLogger.debug("AirRemoveMessageRS "+ new Date() +" ----->>" + removeMessageQueuesResponseDocument.xmlText());
        XMLFileUtility.createFile(removeMessageQueuesResponseDocument.xmlText(), "AirRemoveMessageRS.xml");
        return removeMessageQueuesResponseDocument.getRemoveMessageQueuesResponse().getRemoveMessageQueuesResult();
    }

    public AirMessageQueueRS getAllMessages(String category) throws RemoteException {
        SessionsHandler sessionsHandler = new SessionsHandler();
        OnePointStub onePointStub = sessionsHandler.getOnePointStub();
        String sessoinId = sessionsHandler.mystiflySessionHandler();
        MessageQueuesDocument msgQueue = MessageQueuesDocument.Factory.newInstance();
        MessageQueues messageQueues = msgQueue.addNewMessageQueues();
        AirMessageQueueRQ airMessageQueueRQ = messageQueues.addNewRq();
        airMessageQueueRQ.setSessionId(sessoinId);
        airMessageQueueRQ.setTarget(Mystifly.TARGET);
        airMessageQueueRQ.setCategoryId(QueueCategory.Enum.forString(category));
        XMLFileUtility.createFile(airMessageQueueRQ.xmlText(), "Air"+category+"MessageRQ.xml");
        mystiflyLogger.debug("Air"+category+"MessageRQ"+ new Date() +" ----->>" + airMessageQueueRQ.xmlText());
        MessageQueuesResponseDocument messageQueuesResponseDocument = onePointStub.messageQueues(msgQueue);
        mystiflyLogger.debug("Air"+category+"MessageRS "+ new Date() +" ----->>" + messageQueuesResponseDocument.xmlText());
        XMLFileUtility.createFile(messageQueuesResponseDocument.xmlText(), "Air"+category+"MessageRS.xml");
        return messageQueuesResponseDocument.getMessageQueuesResponse().getMessageQueuesResult();
    }
}
