package utils;

import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.constants.AmadeusConstants;
import com.sun.xml.ws.fault.ServerSOAPFaultException;
import models.AmadeusSessionWrapper;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import services.QueueListService;

import java.util.List;

/**
 * Created by Yaseen on 29-06-2015.
 */
@Component
public class CronJobs {

    static Logger logger = LoggerFactory.getLogger("gds");

    private QueueListService queueListService;

    public QueueListService getQueueListService() {
        return queueListService;
    }

    @Autowired
    public void setQueueListService(QueueListService queueListService) {
        this.queueListService = queueListService;
    }

    @Scheduled(fixedRate = 120000)
    public void amadeusSessionProcess() {
        logger.debug("amadeusSessionProcess  cron job called ..................");

        List<AmadeusSessionWrapper> amadeusSessionWrapperList = AmadeusSessionWrapper.findAllContextList();
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
                            logger.debug("Deleted an session .................... " + amadeusSessionWrapper.getmSession().value.getSessionId());
                            amadeusSessionWrapper.delete();

                        }catch (ServerSOAPFaultException e){

                            amadeusSessionWrapper.delete();
                            logger.debug("Exception in cron job" + e);
                            e.printStackTrace();
                        }catch (Exception e) {
                            logger.debug("Exception in cron job : " + e);
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

    @Scheduled(fixedRate = 3600000)
    public void queueJobs(){
        queueListService.getWaitListConfirmRequest();
        queueListService.getScheduleChange();
        queueListService.getExpiryTimeQueueRequest();
        queueListService.getWaitListConfirmRequest();
    }


}

