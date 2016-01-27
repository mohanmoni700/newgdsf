package models;

import com.amadeus.xml.ws._2009._01.wbs_session_2_0.Session;
import play.db.ebean.Model;

import javax.persistence.*;
import javax.xml.ws.Holder;
import java.util.Date;
import java.util.List;

/**
 * Created by Yaseen on 18-06-2015.
 */
@Entity
@Table(name = "amadeus_session")
public class AmadeusSessionWrapper extends Model{


    @Id
    @Column(name="id")
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @Column(name="session_id")
    private String sessionId;

    @Column(name="sequence_number")
    private String sequenceNumber;

    @Column(name="security_token")
    private String securityToken;

    @Column(name="last_query_date")
    private Date lastQueryDate;

    @Column(name="query_in_progress")
    private boolean queryInProgress;

    @Column(name="active_context")
    private boolean activeContext;

    @Column(name="session_uuid")
    private String sessionUUID;

    @Column(name = "gds_pnr")
    private String gdsPNR;

    @Transient
    private Holder<Session> mSession;

    private static Finder<Integer, AmadeusSessionWrapper> find = new Finder<Integer, AmadeusSessionWrapper>(
            Integer.class, AmadeusSessionWrapper.class);

    public Holder<Session> getmSession() {
        Session session = new Session();
        session.setSecurityToken(this.securityToken);
        session.setSequenceNumber(this.sequenceNumber);
        session.setSessionId(this.sessionId);
        Holder<Session> sessionHolder = new Holder<>();
        sessionHolder.value = session;
        return sessionHolder;
    }

    public void setmSession(Holder<Session> mSession) {
        this.mSession = mSession;
        this.sessionId = mSession.value.getSessionId();
        this.securityToken = mSession.value.getSecurityToken();
        this.sequenceNumber = mSession.value.getSequenceNumber();
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

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(String sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String getSecurityToken() {
        return securityToken;
    }

    public void setSecurityToken(String securityToken) {
        this.securityToken = securityToken;
    }

    public String getSessionUUID() {
        return sessionUUID;
    }

    public void setSessionUUID(String sessionUUID) {
        this.sessionUUID = sessionUUID;
    }

    public static List<AmadeusSessionWrapper> findAllInactiveContextList(){

        List<AmadeusSessionWrapper> amadeusSessions = find.where().eq("active_context", 0).eq("session_uuid",null).findList();

        return amadeusSessions;
    }

    public static List<AmadeusSessionWrapper> findAllContextList(){

        List<AmadeusSessionWrapper> amadeusSessions = find.where().eq("active_context", 0).findList();

        return amadeusSessions;
    }

    public static AmadeusSessionWrapper findSessionByUUID(String uuid){

        AmadeusSessionWrapper amadeusSessions = find.where().eq("session_uuid", uuid).findUnique();

        return amadeusSessions;
    }

    public static AmadeusSessionWrapper findBySessionId(String sessionId){
        AmadeusSessionWrapper amadeusSessions = find.where().eq("session_id", sessionId).findUnique();

        return amadeusSessions;
    }

    public String getGdsPNR() {
        return gdsPNR;
    }

    public void setGdsPNR(String gdsPNR) {
        this.gdsPNR = gdsPNR;
    }


    public static AmadeusSessionWrapper findByPNR(String gdsPNR){
        AmadeusSessionWrapper amadeusSessions = find.where().eq("gds_pnr", gdsPNR).orderBy("lastQueryDate desc").findList().get(0);

        return amadeusSessions;
    }
}
