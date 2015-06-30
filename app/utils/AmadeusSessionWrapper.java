package utils;

import com.amadeus.xml.ws._2009._01.wbs_session_2_0.Session;

import javax.xml.ws.Holder;
import java.util.Date;

/**
 * Created by Yaseen on 18-06-2015.
 */
public class AmadeusSessionWrapper {

    private Holder<Session> mSession;

    private Date lastQueryDate;

    private boolean queryInProgress;

    private boolean activeContext;


    public Holder<Session> getmSession() {
        return mSession;
    }

    public void setmSession(Holder<Session> mSession) {
        this.mSession = mSession;
    }

    public Date getLastQueryDate() {
        return lastQueryDate;
    }

    public void setLastQueryDate(Date lastQueryDate) {
        this.lastQueryDate = lastQueryDate;
    }

    public boolean isQueryInProgress() {
        return queryInProgress;
    }

    public void setQueryInProgress(boolean queryInProgress) {
        this.queryInProgress = queryInProgress;
    }

    public boolean isActiveContext() {
        return activeContext;
    }

    public void setActiveContext(boolean activeContext) {
        this.activeContext = activeContext;
    }
}
