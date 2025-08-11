package utils;

import com.amadeus.xml._2010._06.session_v3.Session;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.constants.AmadeusConstants;
import models.AmadeusSessionWrapper;
import models.FlightSearchOffice;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import services.AmadeusSourceOfficeService;

import javax.xml.ws.Holder;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Created by Yaseen on 18-06-2015.
 */
@Service
public class AmadeusSessionManager {

    static org.slf4j.Logger logger = LoggerFactory.getLogger("gds");

    @Autowired
    private ServiceHandler serviceHandler;

    @Autowired
    private AmadeusSourceOfficeService sourceOfficeService;

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

    public AmadeusSessionWrapper createSession(FlightSearchOffice office) {
        logger.debug("Creating a  new session with office_id : {}", office.getOfficeId());
        try {
            AmadeusSessionWrapper amadeusSessionWrapper = serviceHandler.logIn(office, true);
            return createSessionWrapper(amadeusSessionWrapper, office);
        } catch (Exception e) {
            logger.error("Amadeus createSession error ", e);
            e.printStackTrace();
        }
        return null;
    }

    public AmadeusSessionWrapper getSession() throws Exception {
        FlightSearchOffice office = sourceOfficeService.getAllOffices().get(0);
        logger.debug("Default officeId used in getSession :{}", office.getOfficeId());
        return getSession(office);
    }

    public AmadeusSessionWrapper createSessionWrapper(Session session) {
        AmadeusSessionWrapper amadeusSessionWrapper = new AmadeusSessionWrapper();
        amadeusSessionWrapper.setActiveContext(false);
        amadeusSessionWrapper.setQueryInProgress(false);
        amadeusSessionWrapper.setLastQueryDate(new Date());
        amadeusSessionWrapper.setmSession(new Holder<>(session));
        amadeusSessionWrapper.save();
        return amadeusSessionWrapper;
    }

    public AmadeusSessionWrapper createSessionWrapper(AmadeusSessionWrapper amadeusSessionWrapper, FlightSearchOffice office) {

        amadeusSessionWrapper.setActiveContext(false);
        amadeusSessionWrapper.setQueryInProgress(false);
        amadeusSessionWrapper.setLastQueryDate(new Date());

        amadeusSessionWrapper.save();
        return amadeusSessionWrapper;

    }

    public void updateAmadeusSession(AmadeusSessionWrapper amadeusSessionWrapper) {
        if (amadeusSessionWrapper != null) {
            amadeusSessionWrapper.setQueryInProgress(false);
            amadeusSessionWrapper.update();
        }
    }

    public String storeActiveSession(AmadeusSessionWrapper amadeusSessionWrapper, String pnr) {

        String uuid = UUID.randomUUID().toString();
        amadeusSessionWrapper.setSessionUUID(uuid);
        amadeusSessionWrapper.setActiveContext(true);
        amadeusSessionWrapper.setSessionId(amadeusSessionWrapper.getSessionId());
        amadeusSessionWrapper.setSecurityToken(amadeusSessionWrapper.getSecurityToken());
        amadeusSessionWrapper.setGdsPNR(pnr);
        amadeusSessionWrapper.save();
        return uuid;

    }

    public Session getActiveSession(String sessionIdRef) {
        AmadeusSessionWrapper amadeusSessionWrapper = AmadeusSessionWrapper.findSessionByUUID(sessionIdRef);
        return amadeusSessionWrapper.getmSession().value;
    }

    public AmadeusSessionWrapper getActiveSessionByRef(String sessionIdRef) {
        AmadeusSessionWrapper amadeusSessionWrapper = AmadeusSessionWrapper.findSessionByUUID(sessionIdRef);
        amadeusSessionWrapper.setSessionReUsed(true);
        amadeusSessionWrapper.setStateful(true);
        return amadeusSessionWrapper;
    }

    public void removeActiveSession(Session session) {
        AmadeusSessionWrapper amadeusSessionWrapper = AmadeusSessionWrapper.findBySessionId(session.getSessionId());
        if (amadeusSessionWrapper != null) {
            amadeusSessionWrapper.delete();
        }
    }

    public AmadeusSessionWrapper getActiveSessionByGdsPNR(String pnr) {

        AmadeusSessionWrapper amadeusSessionWrapper =  AmadeusSessionWrapper.findByPNR(pnr);
        amadeusSessionWrapper.setSessionReUsed(true);
        amadeusSessionWrapper.setStateful(true);
        return amadeusSessionWrapper;

    }

    public synchronized AmadeusSessionWrapper getSession(FlightSearchOffice office) throws InterruptedException {
        return getSession(office, 0);
    }

    private synchronized AmadeusSessionWrapper getSession(FlightSearchOffice office, int recursionDepth) throws InterruptedException {
        logger.debug("AmadeusSessionManager getSession called, recursionDepth={}", recursionDepth);
        List<AmadeusSessionWrapper> amadeusSessionWrapperList = AmadeusSessionWrapper.findAllInactiveContextListByOfficeId(office.getOfficeId());

        int count = 0;
        for (AmadeusSessionWrapper amadeusSessionWrapper : amadeusSessionWrapperList) {
            count++;
            if (amadeusSessionWrapper.isQueryInProgress()) {
                continue;
            }
            Period p = new Period(new DateTime(amadeusSessionWrapper.getLastQueryDate()), new DateTime(), PeriodType.minutes());
            int inactivityTimeInMinutes = p.getMinutes();
            if (inactivityTimeInMinutes >= AmadeusConstants.INACTIVITY_TIMEOUT) {
                amadeusSessionWrapper.delete();
                continue;
            }
            amadeusSessionWrapper.setQueryInProgress(true);
            amadeusSessionWrapper.setLastQueryDate(new Date());
            amadeusSessionWrapper.save();
            if (amadeusSessionWrapper.getmSession() != null) {
                logger.debug("Returning existing session .........................................{}", amadeusSessionWrapper.getmSession().value.getSessionId());
                System.out.println("Returning existing session ........................................." + amadeusSessionWrapper.getmSession().value.getSessionId());
            }
            return amadeusSessionWrapper;
        }

        if (count >= AmadeusConstants.SESSION_POOL_SIZE) {

            if (recursionDepth >= 2) {

                logger.error("Recursed Twice, Unsetting query In progress");
                for (AmadeusSessionWrapper amadeusSessionWrapper : amadeusSessionWrapperList) {

                    Period p = new Period(new DateTime(amadeusSessionWrapper.getLastQueryDate()), new DateTime(), PeriodType.minutes());
                    int inactivityTimeInMinutes = p.getMinutes();

                    if (amadeusSessionWrapper.isQueryInProgress() && inactivityTimeInMinutes >= AmadeusConstants.INACTIVITY_TIMEOUT) {
                        amadeusSessionWrapper.delete();
                        System.out.println("Unset Query In progress");
                        logger.debug("Unset Query In Progress {} ", amadeusSessionWrapper.getmSession().value.getSessionId());
                        continue;
                    }

                    amadeusSessionWrapper.setQueryInProgress(true);
                    amadeusSessionWrapper.setLastQueryDate(new Date());
                    amadeusSessionWrapper.save();
                    System.out.println("Returning existing session after 2 recursions........................................." + amadeusSessionWrapper.getmSession().value.getSessionId());
                    return amadeusSessionWrapper;
                }
            }
            logger.debug("Amadeus session pooling max connection size reached, waiting for connection...");
            Thread.sleep(2000);
            return getSession(office, recursionDepth + 1);
        } else {
            return createSession(office);
        }
    }

}
