package utils;

import com.amadeus.xml.ws._2009._01.wbs_session_2_0.Session;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.GDSWrapper.amadeus.SessionHandler;
import com.compassites.constants.AmadeusConstants;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import play.cache.Cache;

import javax.xml.ws.Holder;
import java.util.Date;
import java.util.HashMap;
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
        HashMap<String, AmadeusSessionWrapper> sessionHashMap = (HashMap<String, AmadeusSessionWrapper>) Cache.get(AmadeusConstants.AMADEUS_SESSION_LIST);
        if(sessionHashMap == null) {
            sessionHashMap = new HashMap<>();
        }
        /*if(sessionHashMap.size() == 0){
            AmadeusSessionWrapper amadeusSessionWrapper = createSession();
            sessionHashMap.put(amadeusSessionWrapper.getmSession().value.getSecurityToken(),amadeusSessionWrapper);
            Cache.set(AmadeusConstants.AMADEUS_SESSION_LIST, sessionHashMap);
            return  amadeusSessionWrapper;
        }*/
        int count = 0;
        for(String amadeusSessionWrapperKey : sessionHashMap.keySet()){
            AmadeusSessionWrapper amadeusSessionWrapper = sessionHashMap.get(amadeusSessionWrapperKey);
            count++;
            if(amadeusSessionWrapper.isQueryInProgress()){
                continue;
            }
            amadeusSessionWrapper.setQueryInProgress(true);
            amadeusSessionWrapper.setLastQueryDate(new Date());
            updateAmadeusSession(amadeusSessionWrapper);
            return amadeusSessionWrapper;
        }
        if(count == AmadeusConstants.SESSION_POOL_SIZE){
            logger.debug("Amadeus session pooling max connection size reached waiting for connection");
            Thread.sleep(2000);
            getSession();
        }else {
            AmadeusSessionWrapper amadeusSessionWrapper = createSession();
            sessionHashMap.put(amadeusSessionWrapper.getmSession().value.getSecurityToken(),amadeusSessionWrapper);
            Cache.set(AmadeusConstants.AMADEUS_SESSION_LIST, sessionHashMap);
            return amadeusSessionWrapper;
        }

        return null;
    }


    public AmadeusSessionWrapper createSession(){
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
        amadeusSessionWrapper.setmSession(new Holder<>(session));
        return amadeusSessionWrapper;
    }

    public void updateAmadeusSession(AmadeusSessionWrapper amadeusSessionWrapper){

        HashMap<String, AmadeusSessionWrapper> sessionHashMap = (HashMap<String, AmadeusSessionWrapper>) Cache.get(AmadeusConstants.AMADEUS_SESSION_LIST);
        sessionHashMap.put(amadeusSessionWrapper.getmSession().value.getSecurityToken(), amadeusSessionWrapper);
        Cache.set(AmadeusConstants.AMADEUS_SESSION_LIST, sessionHashMap);
    }

    public String storeActiveSession(Session session){
        HashMap<String, AmadeusSessionWrapper> sessionHashMap = (HashMap<String, AmadeusSessionWrapper>) Cache.get(AmadeusConstants.AMADEUS_ACTIVE_SESSION_LIST);
        if(sessionHashMap == null){
            sessionHashMap = new HashMap<>();
        }
        String uuid = UUID.randomUUID().toString();
        AmadeusSessionWrapper amadeusSessionWrapper = createSessionWrapper(session);
        sessionHashMap.put(uuid,amadeusSessionWrapper);
        Cache.set(AmadeusConstants.AMADEUS_ACTIVE_SESSION_LIST, sessionHashMap);
        return uuid;
    }

    public Session getActiveSession(String sessionIdRef){
        HashMap<String, AmadeusSessionWrapper> sessionHashMap = (HashMap<String, AmadeusSessionWrapper>) Cache.get(AmadeusConstants.AMADEUS_ACTIVE_SESSION_LIST);
        return sessionHashMap.get(sessionIdRef).getmSession().value;
    }
}
