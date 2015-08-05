package utils;

import com.amadeus.xml.ws._2009._01.wbs_session_2_0.Session;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.GDSWrapper.amadeus.SessionHandler;
import com.compassites.constants.AmadeusConstants;
import models.AmadeusSessionWrapper;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.xml.ws.Holder;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Created by Yaseen on 18-06-2015.
 */
@Service
public class AmadeusSessionManager {

//    private static List<AmadeusSessionWrapper> sessionList = new ArrayList<>();
    static org.slf4j.Logger logger = LoggerFactory.getLogger("gds");

    /*private ServiceHandler serviceHandler;

    @Autowired
    public AmadeusSessionManager(ServiceHandler serviceHandler) {
        this.serviceHandler = serviceHandler;
    }*/

    public AmadeusSessionWrapper getSession() throws InterruptedException {

        logger.debug("AmadeusSessionManager getSession called");
       /* HashMap<String, AmadeusSessionWrapper> sessionHashMap = (HashMap<String, AmadeusSessionWrapper>) Cache.get(AmadeusConstants.AMADEUS_SESSION_LIST);

        if(sessionHashMap == null) {
            sessionHashMap = new HashMap<>();
        }*/
        List<AmadeusSessionWrapper> amadeusSessionWrapperList = AmadeusSessionWrapper.findAllInactiveContextList();
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
            System.out.println("Returning existing session ........................................." + amadeusSessionWrapper.getmSession().value.getSessionId());
            return amadeusSessionWrapper;
        }
        if(count >= AmadeusConstants.SESSION_POOL_SIZE){
            logger.debug("Amadeus session pooling max connection size reached waiting for connection...................");
            Thread.sleep(2000);
            getSession();
        }else {
            AmadeusSessionWrapper amadeusSessionWrapper = createSession();
/*            sessionHashMap.put(amadeusSessionWrapper.getmSession().value.getSecurityToken(),amadeusSessionWrapper);
            Cache.set(AmadeusConstants.AMADEUS_SESSION_LIST, sessionHashMap);*/
            return amadeusSessionWrapper;
        }

        return null;
    }


    public AmadeusSessionWrapper createSession(){
        System.out.println("creating new  session ........................................." );
        try {
            ServiceHandler serviceHandler = new ServiceHandler();
            SessionHandler sessionHandler = serviceHandler.logIn(new SessionHandler());
            return createSessionWrapper(sessionHandler.getSession().value);
        } catch (Exception e) {
            e.printStackTrace();
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

    public void updateAmadeusSession(AmadeusSessionWrapper amadeusSessionWrapper){
        amadeusSessionWrapper.setQueryInProgress(false);
        amadeusSessionWrapper.update();
    }

    public String storeActiveSession(Session session){

        String uuid = UUID.randomUUID().toString();
        AmadeusSessionWrapper amadeusSessionWrapper = createSessionWrapper(session);
        amadeusSessionWrapper.setSessionUUID(uuid);
        amadeusSessionWrapper.save();
        return uuid;
    }

    public Session getActiveSession(String sessionIdRef){
        AmadeusSessionWrapper amadeusSessionWrapper = AmadeusSessionWrapper.findSessionByUUID(sessionIdRef);
        return amadeusSessionWrapper.getmSession().value;
    }
}
