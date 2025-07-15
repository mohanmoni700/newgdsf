package models;

import com.amadeus.wsdl._2010._06.ws.link_v1.TransactionFlowLinkType;
import com.amadeus.xml._2010._06.security_v1.AMASecurityHostedUser;
import com.amadeus.xml._2010._06.session_v3.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.db.ebean.Model;

import javax.persistence.*;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by Yaseen on 18-06-2015.
 */
@Entity
@Table(name = "amadeus_session")
public class AmadeusSessionWrapper extends Model {

    private static final Logger logger = LoggerFactory.getLogger("gds");

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "sequence_number")
    private String sequenceNumber;

    @Column(name = "security_token")
    private String securityToken;

    @Column(name = "last_query_date")
    private Date lastQueryDate;

    @Column(name = "query_in_progress")
    private boolean queryInProgress;

    @Column(name = "active_context")
    private boolean activeContext;

    @Column(name = "session_uuid")
    private String sessionUUID;

    @Column(name = "gds_pnr")
    private String gdsPNR;

    @Column(name = "office_id")
    private String officeId;

    @Column(name = "office_name")
    private String officeName;

    @Column(name = "is_partner")
    private boolean isPartner;

    @Transient
    private Holder<Session> mSession;

    @Transient
    private boolean isStateful;

    @Transient
    private boolean isLogout;

    @Transient
    private boolean isSessionReUsed;

    private static final Finder<Integer, AmadeusSessionWrapper> find = new Finder<>(Integer.class, AmadeusSessionWrapper.class);

    public String getOfficeId() {
        return officeId;
    }

    public void setOfficeId(String officeId) {
        this.officeId = officeId;
    }

    public String getOfficeName() {
        return officeName;
    }

    public void setPartnerName(String partnerName) {
        this.officeName = officeName;
    }

    public boolean isPartner() {
        return isPartner;
    }

    public void setPartner(boolean partner) {
        isPartner = partner;
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

    public boolean isSessionReUsed() {
        return isSessionReUsed;
    }

    public void setSessionReUsed(boolean sessionReUsed) {
        isSessionReUsed = sessionReUsed;
    }

    public static List<AmadeusSessionWrapper> findAllInactiveContextList() {

        return find.where().eq("active_context", 0).eq("session_uuid", null).findList();
    }

    public static List<AmadeusSessionWrapper> findAllInactiveContextListByOfficeId(String officeId) {

        return find.where().eq("active_context", 0).eq("session_uuid", null).eq("office_id", officeId).findList();
    }

    public static List<AmadeusSessionWrapper> findAllContextList() {

        return find.where().eq("active_context", 0).findList();
    }

    public static AmadeusSessionWrapper findSessionByUUID(String uuid) {

        return find.where().eq("session_uuid", uuid).findUnique();
    }

    public static AmadeusSessionWrapper findBySessionId(String sessionId) {

        return find.where().eq("session_id", sessionId).findUnique();
    }

    public String getGdsPNR() {
        return gdsPNR;
    }

    public void setGdsPNR(String gdsPNR) {
        this.gdsPNR = gdsPNR;
    }

    public boolean isStateful() {
        return isStateful;
    }

    public void setStateful(boolean stateful) {
        isStateful = stateful;
    }

    public boolean isLogout() {
        return isLogout;
    }

    public void setLogout(boolean logout) {
        isLogout = logout;
    }


    public static AmadeusSessionWrapper findByPNR(String gdsPNR) {

        return find.where().eq("gds_pnr", gdsPNR).orderBy("lastQueryDate desc").findList().get(0);
    }

    public Holder<Session> getmSession() {
        if (mSession == null && sessionId.isEmpty()) {
            mSession = new Holder<>();
            mSession.value = new Session();
            mSession.value.setSequenceNumber(this.sequenceNumber != null ? this.sequenceNumber : "0");
            mSession.value.setSessionId(this.sessionId);
            mSession.value.setSecurityToken(this.securityToken);
        }
        return mSession;
    }

    public void setmSession(Holder<Session> mSession) {

        this.mSession = mSession;

        if (mSession.value != null) {
            this.sessionId = mSession.value.getSessionId();
            this.sequenceNumber = mSession.value.getSequenceNumber();
            this.securityToken = mSession.value.getSecurityToken();
        }

    }

    public Holder<Session> resetSession() {

        Holder<Session> mSession = new Holder<>();

        mSession.value = new Session();
        mSession.value.setSecurityToken("");
        mSession.value.setSequenceNumber("0");
        mSession.value.setSessionId("");
        mSession.value.setTransactionStatusCode("");

        setmSession(mSession);

        return this.mSession;
    }

    public void incrementSequenceNumber(AmadeusSessionWrapper amadeusSessionWrapper, BindingProvider bindingProvider) {

        if(amadeusSessionWrapper.isSessionReUsed || amadeusSessionWrapper.isLogout()) {
            mSession = getmSessionFromWrapper(amadeusSessionWrapper);
        } else {
            getmSession();
        }

        int sequenceNumber = Integer.parseInt(mSession.value.getSequenceNumber());
        sequenceNumber++;

        mSession.value.setSequenceNumber(Integer.toString(sequenceNumber));
        amadeusSessionWrapper.setmSession(mSession);

        Map<String, Object> reqContext = bindingProvider.getRequestContext();
        reqContext.put("amadeusSessionWrapper", amadeusSessionWrapper);

    }

    public void updateSessionFromResponse(Holder<Session> responseSession) {

        if (responseSession != null && responseSession.value != null) {
            String transactionStatus = responseSession.value.getTransactionStatusCode();
            if (!isStateful && "End".equals(transactionStatus)) {
                logger.debug("Stateless response with TransactionStatusCode=End, skipping session update");
                return;
            }
            this.sessionId = responseSession.value.getSessionId();
            this.sequenceNumber = responseSession.value.getSequenceNumber();
            this.securityToken = responseSession.value.getSecurityToken();
            this.mSession = responseSession;
            logger.debug("Session updated from response: SessionId={}, SequenceNumber={}, SecurityToken={}",
                    this.sessionId, this.sequenceNumber, this.securityToken);
        } else {
            logger.warn("No session data in response");
        }

    }

    public String printSession() {

        Holder<Session> mSession = getmSession();
        String printString = "Stoken:" + mSession.value.getSecurityToken() + "  SNum:" + mSession.value.getSequenceNumber() + "  id:" + mSession.value.getSessionId();
        return printString;

    }

    public AMASecurityHostedUser getAmaSecurityHostedUser() {

        AMASecurityHostedUser amaSecurityHostedUserX = new AMASecurityHostedUser();

        AMASecurityHostedUser.UserID userID = new AMASecurityHostedUser.UserID();
        userID.setRequestorType("U");
        userID.setAgentDutyCode("SU");
        userID.setPseudoCityCode(this.officeId);
        userID.setPOSType("1");

        amaSecurityHostedUserX.setUserID(userID);

        return amaSecurityHostedUserX;
    }

    public Holder<TransactionFlowLinkType> getTransactionFlowLinkTypeHolder() {
        return new Holder<>();
    }

    private Holder<Session> getmSessionFromWrapper(AmadeusSessionWrapper amadeusSessionWrapper) {

        mSession = new Holder<>();
        mSession.value = new Session();
        mSession.value.setSequenceNumber(amadeusSessionWrapper.getSequenceNumber());
        mSession.value.setSessionId(amadeusSessionWrapper.getSessionId());
        mSession.value.setSecurityToken(amadeusSessionWrapper.getSecurityToken());
        mSession.value.setTransactionStatusCode("InSeries");

        return mSession;
    }

}
