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

    @Column(name = "office_id")
    private String officeId;

    @Column(name = "partner_name")
    private String partnerName;

    @Transient
    private Holder<Session> mSession;

    private static final Finder<Integer, AmadeusSessionWrapper> find = new Finder<Integer, AmadeusSessionWrapper>(
            Integer.class, AmadeusSessionWrapper.class);

    public Holder<Session> getmSession1() {
        mSession = new Holder<Session>();
        Session session = new Session();
        session.setSecurityToken(this.securityToken);
        session.setSequenceNumber(this.sequenceNumber);
        session.setSessionId(this.sessionId);
        Holder<Session> sessionHolder = new Holder<>();
        sessionHolder.value = session;
        return sessionHolder;
    }

    public Holder<Session> getmSession() {
        Holder<Session> mSession = new Holder<>();
        mSession.value = new Session();
        mSession.value.setSecurityToken(this.securityToken);
        mSession.value.setSequenceNumber(this.sequenceNumber);
        mSession.value.setSessionId(this.sessionId);
        return mSession;
    }
    //todo
    public String printSession(){
        Holder<Session> mSession = getmSession();
        String printString = "Stoken:" +mSession.value.getSecurityToken() + "  SNum:"+ mSession.value.getSequenceNumber()+ "  id:"+mSession.value.getSessionId();
        System.out.println(printString);
        return printString;
    }

    public void initSession() {
        mSession = new Holder<Session>();
        resetSession();
        setmSession(mSession);
    }

    public void setmSession(Holder<Session> mSession) {
        this.mSession = mSession;
        this.sessionId = mSession.value.getSessionId();
        this.securityToken = mSession.value.getSecurityToken();
        this.sequenceNumber = mSession.value.getSequenceNumber();
    }

    public String getOfficeId() { return officeId; }

    public void setOfficeId(String officeId) { this.officeId = officeId; }

    public String getPartnerName() { return partnerName; }

    public void setPartnerName(String partnerName) { this.partnerName = partnerName; }

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

    public static List<AmadeusSessionWrapper> findAllInactiveContextListByOfficeId(String officeId){

        List<AmadeusSessionWrapper> amadeusSessions = find.where().eq("active_context", 0).eq("session_uuid",null).eq("office_id",officeId).findList();

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

    /**************/
    public void resetSession() {
        mSession.value = new Session();
        mSession.value.setSecurityToken("");
        mSession.value.setSequenceNumber("");
        mSession.value.setSessionId("");
    }

    public void incrementSequenceNumber() {
        Integer sequenceNumber = Integer.parseInt(mSession.value
                .getSequenceNumber());
        sequenceNumber++;
        mSession.value.setSequenceNumber(sequenceNumber.toString());
        mSession.value.setSequenceNumber("1");
    }

}
