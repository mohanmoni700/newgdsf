package utils;

import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.GDSWrapper.amadeus.SessionHandler;
import com.compassites.constants.AmadeusConstants;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import play.cache.Cache;

import java.util.HashMap;

/**
 * Created by Yaseen on 29-06-2015.
 */
@Component
public class CronJobs {


    @Scheduled(fixedRate = 120000)
    public void amadeusSessionProcess() {
        System.out.println("amadeusSessionProcess called ..................");

        HashMap<String, AmadeusSessionWrapper> sessionHashMap = (HashMap<String, AmadeusSessionWrapper>) Cache.get(AmadeusConstants.AMADEUS_SESSION_LIST);
        if(sessionHashMap != null && sessionHashMap.size() > 0) {
            for(String amadeusSessionWrapperKey : sessionHashMap.keySet()) {
                AmadeusSessionWrapper amadeusSessionWrapper = sessionHashMap.get(amadeusSessionWrapperKey);
                if(!amadeusSessionWrapper.isQueryInProgress())  {
                    Period p = new Period(new DateTime(), new DateTime(amadeusSessionWrapper.getLastQueryDate()), PeriodType.minutes());
                    int inactivityTimeInMinutes = p.getMinutes();
                    if(inactivityTimeInMinutes > AmadeusConstants.INACTIVITY_TIMEOUT){
                        try {
                            ServiceHandler serviceHandler = new ServiceHandler();
                            serviceHandler.setSession(amadeusSessionWrapper.getmSession().value);
                            serviceHandler.logOut();
                            sessionHashMap.remove(amadeusSessionWrapperKey);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        SessionHandler sessionHandler = new SessionHandler(amadeusSessionWrapper.getmSession());
                    }
                }
            }
        }


    }
}
