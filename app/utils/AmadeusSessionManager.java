package utils;

import com.amadeus.xml.ws._2009._01.wbs_session_2_0.Session;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.GDSWrapper.amadeus.SessionHandler;
import com.compassites.constants.AmadeusConstants;
import models.AmadeusSessionWrapper;
import models.FlightSearchOffice;
import org.hibernate.annotations.Synchronize;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import play.Play;

import javax.xml.ws.Holder;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Created by Yaseen on 18-06-2015.
 */
@Service
public class AmadeusSessionManager {

    static org.slf4j.Logger logger = LoggerFactory.getLogger("gds");
    List<String> officeIdList = Play.application().configuration().getStringList("amadeus.SOURCE_OFFICE");


//    private static final ConcurrentHashMap<String, BlockingQueue<AmadeusSessionWrapper>> officeSessionMap = new ConcurrentHashMap<>();

//    public AmadeusSessionWrapper getSession(String officeId) throws Exception {
//        logger.debug("AmadeusSessionManager getSession called");
//        // Check if there is a queue for the specified officeId
//        List<AmadeusSessionWrapper> amadeusSessionWrapperList = AmadeusSessionWrapper.findAllInactiveContextList();
//        BlockingQueue<AmadeusSessionWrapper> sessionQueue = officeSessionMap.get(officeId);
//        if (sessionQueue == null) {
//            sessionQueue = new LinkedBlockingQueue<>();
//            officeSessionMap.put(officeId, sessionQueue);
//        }
//        try {
//            // Try to take an available session from the queue
//            AmadeusSessionWrapper availableSession = sessionQueue.poll(2, TimeUnit.SECONDS);
//            if (availableSession != null) {
//                if (isValidSession(availableSession)) {
//                    return availableSession;
//                }
//            }
//            // Check the pool size
//            if (sessionQueue.size() >= AmadeusConstants.SESSION_POOL_SIZE) {
//                logger.debug("Amadeus session pooling max connection size reached. Waiting for connection...");
//                throw new TimeoutException("Max session pool size reached, cannot create new sessions.");
//            }
//
//            AmadeusSessionWrapper newSession = createSession(officeId);
//            if (newSession != null) {
//                sessionQueue.offer(newSession);
//                return newSession;
//            } else {
//                throw new Exception("Failed to create a new session.");
//            }
//        } catch (InterruptedException ie) {
//            logger.error("Thread was interrupted while waiting for a session.", ie);
//        }
//        return null;
//    }

    private boolean isValidSession(AmadeusSessionWrapper session) {
        if (session.isQueryInProgress()) {
            return false;
        }

        Period p = new Period(new DateTime(session.getLastQueryDate()), new DateTime(), PeriodType.minutes());
        int inactivityTimeInMinutes = p.getMinutes();

        if (inactivityTimeInMinutes >= AmadeusConstants.INACTIVITY_TIMEOUT) {
            session.delete();
            return false;
        }

        session.setQueryInProgress(true);
        session.setLastQueryDate(new Date());
        session.save();
        return true;
    }

    public AmadeusSessionWrapper createSession(FlightSearchOffice office){
        logger.debug("creating new  session .........................................office_id :" + office.getGetOfficeId());
        try {
            ServiceHandler serviceHandler = new ServiceHandler();
            AmadeusSessionWrapper amadeusSessionWrapper = serviceHandler.logIn(office);
            return createSessionWrapper(amadeusSessionWrapper, office);
        } catch (Exception e) {
            logger.error("Amadeus createSession error " ,e);
            e.printStackTrace();
        }
        return null;
    }

//    //todo
    public AmadeusSessionWrapper getSession() throws Exception {
        String officeId = Play.application().configuration().getString("amadeus.SOURCE_OFFICE_DEFAULT");
        logger.debug("default officeId used in getSession");
        return getSession(new FlightSearchOffice(officeId));
    }

    public synchronized AmadeusSessionWrapper getSession(FlightSearchOffice office) throws InterruptedException {

        logger.debug("AmadeusSessionManager getSession called");
       /* HashMap<String, AmadeusSessionWrapper> sessionHashMap = (HashMap<String, AmadeusSessionWrapper>) Cache.get(AmadeusConstants.AMADEUS_SESSION_LIST);

        if(sessionHashMap == null) {
            sessionHashMap = new HashMap<>();
        }*/
        List<AmadeusSessionWrapper> amadeusSessionWrapperList = AmadeusSessionWrapper.findAllInactiveContextListByOfficeId(office.getGetOfficeId());
        int count = 0;
        for(AmadeusSessionWrapper amadeusSessionWrapper : amadeusSessionWrapperList){
            count++;
            if(amadeusSessionWrapper.isQueryInProgress()){
                continue;
            }
            Period p = new Period(new DateTime(amadeusSessionWrapper.getLastQueryDate()), new DateTime(), PeriodType.minutes());
            int inactivityTimeInMinutes = p.getMinutes();
            if(inactivityTimeInMinutes >= AmadeusConstants.INACTIVITY_TIMEOUT){
                amadeusSessionWrapper.delete();
                continue;
            }
            amadeusSessionWrapper.setQueryInProgress(true);
            amadeusSessionWrapper.setLastQueryDate(new Date());
//            updateAmadeusSession(amadeusSessionWrapper);
            amadeusSessionWrapper.save();
            logger.debug("Returning existing session ........................................." + amadeusSessionWrapper.getmSession().value.getSessionId());
            System.out.println("Returning existing session ........................................." + amadeusSessionWrapper.getmSession().value.getSessionId());
            return amadeusSessionWrapper;
        }
        if(count >= AmadeusConstants.SESSION_POOL_SIZE){
            logger.debug("Amadeus session pooling max connection size reached waiting for connection...................");
            Thread.sleep(2000);
            getSession(office);
        }else {
            AmadeusSessionWrapper amadeusSessionWrapper = createSession(office);
/*            sessionHashMap.put(amadeusSessionWrapper.getmSession().value.getSecurityToken(),amadeusSessionWrapper);
            Cache.set(AmadeusConstants.AMADEUS_SESSION_LIST, sessionHashMap);*/
            return amadeusSessionWrapper;
        }
        return null;
    }

    public AmadeusSessionWrapper createSessionWrapper(Session session){
        AmadeusSessionWrapper amadeusSessionWrapper = new AmadeusSessionWrapper();
        amadeusSessionWrapper.setActiveContext(false);
        amadeusSessionWrapper.setQueryInProgress(false);
        amadeusSessionWrapper.setLastQueryDate(new Date());
        amadeusSessionWrapper.setmSession(new Holder<>(session));
        amadeusSessionWrapper.save();
        return amadeusSessionWrapper;
    }

    public AmadeusSessionWrapper createSessionWrapper(AmadeusSessionWrapper amadeusSessionWrapper,FlightSearchOffice office){
        //AmadeusSessionWrapper amadeusSessionWrapper = new AmadeusSessionWrapper();
        amadeusSessionWrapper.setActiveContext(false);
        amadeusSessionWrapper.setQueryInProgress(false);
        amadeusSessionWrapper.setLastQueryDate(new Date());
//        //amadeusSessionWrapper.setmSession(new Holder<>(session));
//        amadeusSessionWrapper.setOfficeId(office.getGetOfficeId());
//        if(office.isPartner()) {
//            amadeusSessionWrapper.setPartnerName("Benji");
//        }
        amadeusSessionWrapper.save();
        return amadeusSessionWrapper;
    }

//    public AmadeusSessionWrapper createSessionWrapper(AmadeusSessionWrapper amadeusSessionWrapper,FlightSearchOffice office){
//        //AmadeusSessionWrapper amadeusSessionWrapper = new AmadeusSessionWrapper();
//        amadeusSessionWrapper.setActiveContext(false);
//        amadeusSessionWrapper.setQueryInProgress(false);
//        amadeusSessionWrapper.setLastQueryDate(new Date());
//        //amadeusSessionWrapper.setmSession(new Holder<>(session));
//        amadeusSessionWrapper.setOfficeId(office.getGetOfficeId());
//        if(office.isPartner()) {
//            amadeusSessionWrapper.setPartnerName("Benji");
//        }
//        amadeusSessionWrapper.save();
//        return amadeusSessionWrapper;
//    }

    public void updateAmadeusSession(AmadeusSessionWrapper amadeusSessionWrapper){
        if(amadeusSessionWrapper != null) {
            amadeusSessionWrapper.setQueryInProgress(false);
            amadeusSessionWrapper.update();
        }
    }

    public String storeActiveSession(AmadeusSessionWrapper amadeusSessionWrapper, String pnr){

        String uuid = UUID.randomUUID().toString();
        //AmadeusSessionWrapper amadeusSessionWrapper = createSessionWrapper(session);
        amadeusSessionWrapper.setSessionUUID(uuid);
        amadeusSessionWrapper.setActiveContext(true);
        amadeusSessionWrapper.setGdsPNR(pnr);
        amadeusSessionWrapper.save();
        return uuid;
    }

    public Session getActiveSession(String sessionIdRef){
        AmadeusSessionWrapper amadeusSessionWrapper = AmadeusSessionWrapper.findSessionByUUID(sessionIdRef);
        return amadeusSessionWrapper.getmSession().value;
    }

    public AmadeusSessionWrapper getActiveSessionByRef(String sessionIdRef){
        AmadeusSessionWrapper amadeusSessionWrapper = AmadeusSessionWrapper.findSessionByUUID(sessionIdRef);
        return amadeusSessionWrapper;
    }

    public void removeActiveSession(Session session){
        AmadeusSessionWrapper amadeusSessionWrapper = AmadeusSessionWrapper.findBySessionId(session.getSessionId());
        amadeusSessionWrapper.delete();
    }

//    public Session getActiveSessionByGdsPNR(String pnr){
//        AmadeusSessionWrapper amadeusSessionWrapper = AmadeusSessionWrapper.findByPNR(pnr);
//        return amadeusSessionWrapper.getmSession().value;
//    }

    public AmadeusSessionWrapper getActiveSessionByGdsPNR(String pnr){
        AmadeusSessionWrapper amadeusSessionWrapper = AmadeusSessionWrapper.findByPNR(pnr);
        return amadeusSessionWrapper;
    }
}
