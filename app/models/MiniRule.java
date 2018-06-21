package models;

import com.avaje.ebean.annotation.CreatedTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnore;
import play.db.ebean.Model;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "mini_rule_fares")
public class MiniRule extends Model {

    @Id
    private Long id;

    @Column(name ="changefee_before_dept")
    private BigDecimal changeFeeBeforeDept;

    @Column(name ="changefee_after_dept")
    private BigDecimal changeFeeAfterDept;

    @Column(name ="changefee_noshow")
    private BigDecimal changeFeeNoShow;

    @Column(name ="cancellationfee_before_dept")
    private BigDecimal cancellationFeeBeforeDept;

    @Column(name ="cancellationfee_after_dept")
    private BigDecimal cancellationFeeAfterDept;

    @Column(name ="cancellation_noshow")
    private BigDecimal cancellationFeeNoShow;

    @Column(name ="changefee_before_dept_currency")
    private String changeFeeBeforeDeptCurrency;

    @Column(name ="changefee_after_dept_currency")
    private String changeFeeFeeAfterDeptCurrency;

    @Column(name ="changefee_noshow_currency")
    private String changeFeeNoShowFeeCurrency;

    @Column(name ="cancellationfee_before_dept_currency")
    private String cancellationFeeBeforeDeptCurrency;

    @Column(name ="cancellationfee_after_dept_currency")
    private String cancellationFeeAfterDeptCurrency;


    @Column(name ="cancellation_noshow_currency")
    private String cancellationNoShowCurrency;

    @Column(name = "api_call")
    private String apiCall;

    @Column(name = "insert_date")
    @CreatedTimestamp
    private Date insertDate;

  /*  @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "ticket_id")
    private Tickets tickets;

    public Tickets getTickets() {
        return tickets;
    }

    public void setTickets(Tickets tickets) {
        this.tickets = tickets;
    }*/

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getChangeFeeBeforeDept() {
        return changeFeeBeforeDept;
    }

    public void setChangeFeeBeforeDept(BigDecimal changeFeeBeforeDept) {
        this.changeFeeBeforeDept = changeFeeBeforeDept;
    }

    public BigDecimal getChangeFeeAfterDept() {
        return changeFeeAfterDept;
    }

    public void setChangeFeeAfterDept(BigDecimal changeFeeAfterDept) {
        this.changeFeeAfterDept = changeFeeAfterDept;
    }

    public BigDecimal getChangeFeeNoShow() {
        return changeFeeNoShow;
    }

    public void setChangeFeeNoShow(BigDecimal changeFeeNoShow) {
        this.changeFeeNoShow = changeFeeNoShow;
    }

    public BigDecimal getCancellationFeeBeforeDept() {
        return cancellationFeeBeforeDept;
    }

    public void setCancellationFeeBeforeDept(BigDecimal cancellationFeeBeforeDept) {
        this.cancellationFeeBeforeDept = cancellationFeeBeforeDept;
    }

    public BigDecimal getCancellationFeeAfterDept() {
        return cancellationFeeAfterDept;
    }

    public void setCancellationFeeAfterDept(BigDecimal cancellationFeeAfterDept) {
        this.cancellationFeeAfterDept = cancellationFeeAfterDept;
    }

    public BigDecimal getCancellationFeeNoShow() {
        return cancellationFeeNoShow;
    }

    public void setCancellationFeeNoShow(BigDecimal cancellationFeeNoShow) {
        this.cancellationFeeNoShow = cancellationFeeNoShow;
    }

    public String getChangeFeeBeforeDeptCurrency() {
        return changeFeeBeforeDeptCurrency;
    }

    public void setChangeFeeBeforeDeptCurrency(String changeFeeBeforeDeptCurrency) {
        this.changeFeeBeforeDeptCurrency = changeFeeBeforeDeptCurrency;
    }

    public String getChangeFeeFeeAfterDeptCurrency() {
        return changeFeeFeeAfterDeptCurrency;
    }

    public void setChangeFeeFeeAfterDeptCurrency(String changeFeeFeeAfterDeptCurrency) {
        this.changeFeeFeeAfterDeptCurrency = changeFeeFeeAfterDeptCurrency;
    }

    public String getchangeFeeNoShowFeeCurrency() {
        return changeFeeNoShowFeeCurrency;
    }

    public void setChangeFeeNoShowFeeCurrency(String changeFeeNoShowFeeCurrency) {
        this.changeFeeNoShowFeeCurrency = changeFeeNoShowFeeCurrency;
    }

    public String getCancellationFeeBeforeDeptCurrency() {
        return cancellationFeeBeforeDeptCurrency;
    }

    public void setCancellationFeeBeforeDeptCurrency(String cancellationFeeBeforeDeptCurrency) {
        this.cancellationFeeBeforeDeptCurrency = cancellationFeeBeforeDeptCurrency;
    }

    public String getCancellationFeeAfterDeptCurrency() {
        return cancellationFeeAfterDeptCurrency;
    }

    public void setCancellationFeeAfterDeptCurrency(String cancellationFeeAfterDeptCurrency) {
        this.cancellationFeeAfterDeptCurrency = cancellationFeeAfterDeptCurrency;
    }

    public String getCancellationNoShowCurrency() {
        return cancellationNoShowCurrency;
    }

    public void setCancellationNoShowCurrency(String cancellationNoShowCurrency) {
        this.cancellationNoShowCurrency = cancellationNoShowCurrency;
    }

    public String getApiCall() {
        return apiCall;
    }

    public void setApiCall(String apiCall) {
        this.apiCall = apiCall;
    }

    public Date getInsertDate() {
        return insertDate;
    }

    public void setInsertDate(Date insertDate) {
        this.insertDate = insertDate;
    }
}
