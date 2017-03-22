package models;

import play.db.ebean.Model;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

/**
 * Created by Satish Kumar on 17-03-2017.
 */
@Entity
@Table(name = "mystifly_session")
public class MystiflySessionWrapper extends Model{

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

    @Column(name="session_created_time")
    private String sessionCreatedTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getSessionUUID() {
        return sessionUUID;
    }

    public void setSessionUUID(String sessionUUID) {
        this.sessionUUID = sessionUUID;
    }

    public String getSessionCreatedTime() {
        return sessionCreatedTime;
    }

    public void setSessionCreatedTime(String sessionCreatedTime) {
        this.sessionCreatedTime = sessionCreatedTime;
    }

    private static Finder<Integer, MystiflySessionWrapper> find = new Finder<Integer, MystiflySessionWrapper>(
            Integer.class, MystiflySessionWrapper.class);

    public static List<MystiflySessionWrapper> findByAllActiveSession(){
        return find.where().eq("active_context", true).findList();
    }
    public static MystiflySessionWrapper findByActiveSession(){
        return find.where().eq("active_context", true).orderBy("id desc").setMaxRows(1).findUnique();
    }
}
