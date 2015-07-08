package utils;

import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.constants.AmadeusConstants;
import models.AmadeusSessionWrapper;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by Yaseen on 29-06-2015.
 */
@Component
public class CronJobs {


    @Scheduled(fixedRate = 120000)
    public void amadeusSessionProcess() {
        System.out.println("amadeusSessionProcess  cron job called ..................");

        List<AmadeusSessionWrapper> amadeusSessionWrapperList = AmadeusSessionWrapper.findAllInactiveContextList();
        if(amadeusSessionWrapperList != null && amadeusSessionWrapperList.size() > 0) {
            for(AmadeusSessionWrapper amadeusSessionWrapper : amadeusSessionWrapperList) {
               if(!amadeusSessionWrapper.isQueryInProgress())  {
                    Period p = new Period(new DateTime(amadeusSessionWrapper.getLastQueryDate()), new DateTime(), PeriodType.minutes());
                    int inactivityTimeInMinutes = p.getMinutes();
                    if(inactivityTimeInMinutes >= AmadeusConstants.INACTIVITY_TIMEOUT){
                        try {
                            ServiceHandler serviceHandler = new ServiceHandler();
                            serviceHandler.setSession(amadeusSessionWrapper.getmSession().value);
                            serviceHandler.logOut();
                            System.out.println("Deleted an session .................... " + amadeusSessionWrapper.getmSession().value.getSessionId());
                            amadeusSessionWrapper.delete();

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
/*
                        SessionHandler sessionHandler = new SessionHandler(amadeusSessionWrapper.getmSession());
*/
                    }
                }
            }
        }


    }
}
