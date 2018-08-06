package models;

import com.avaje.ebean.annotation.CreatedTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnore;
import play.db.ebean.Model;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;


public class MiniRule extends Model {

    private Long id;

    private BigDecimal changeFeeBeforeDept;

    private BigDecimal changeFeeAfterDept;

    private BigDecimal changeFeeNoShow;

    private BigDecimal cancellationFeeBeforeDept;

    private BigDecimal cancellationFeeAfterDept;

    private BigDecimal cancellationFeeNoShow;

    private String changeFeeBeforeDeptCurrency;

    private String changeFeeFeeAfterDeptCurrency;

    private String changeFeeNoShowFeeCurrency;

    private String cancellationFeeBeforeDeptCurrency;

    private String cancellationFeeAfterDeptCurrency;


    private String cancellationNoShowCurrency;

    private String apiCall;

    @Transient
    private Date insertDate;

    private Boolean isCancellationRefundableBeforeDept;

    private Boolean isCancellationRefundableAfterDept;

    private Boolean isCancellationNoShowBeforeDept;

    private Boolean isCancellationNoShowAfterDept;

    private Boolean isChangeRefundableBeforeDept;

    private Boolean isChangeRefundableAfterDept;

    private Boolean isChangeNoShowBeforeDept;

    private Boolean isChangeNoShowAfterDept;


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

    public Boolean getCancellationRefundableBeforeDept() {
        return isCancellationRefundableBeforeDept;
    }

    public void setCancellationRefundableBeforeDept(Boolean cancellationRefundableBeforeDept) {
        isCancellationRefundableBeforeDept = cancellationRefundableBeforeDept;
    }

    public Boolean getCancellationRefundableAfterDept() {
        return isCancellationRefundableAfterDept;
    }

    public void setCancellationRefundableAfterDept(Boolean cancellationRefundableAfterDept) {
        isCancellationRefundableAfterDept = cancellationRefundableAfterDept;
    }

    public Boolean getCancellationNoShowBeforeDept() {
        return isCancellationNoShowBeforeDept;
    }

    public void setCancellationNoShowBeforeDept(Boolean cancellationNoShowBeforeDept) {
        isCancellationNoShowBeforeDept = cancellationNoShowBeforeDept;
    }

    public Boolean getCancellationNoShowAfterDept() {
        return isCancellationNoShowAfterDept;
    }

    public void setCancellationNoShowAfterDept(Boolean cancellationNoShowAfterDept) {
        isCancellationNoShowAfterDept = cancellationNoShowAfterDept;
    }

    public Boolean getChangeRefundableBeforeDept() {
        return isChangeRefundableBeforeDept;
    }

    public void setChangeRefundableBeforeDept(Boolean changeRefundableBeforeDept) {
        isChangeRefundableBeforeDept = changeRefundableBeforeDept;
    }

    public Boolean getChangeRefundableAfterDept() {
        return isChangeRefundableAfterDept;
    }

    public void setChangeRefundableAfterDept(Boolean changeRefundableAfterDept) {
        isChangeRefundableAfterDept = changeRefundableAfterDept;
    }

    public Boolean getChangeNoShowBeforeDept() {
        return isChangeNoShowBeforeDept;
    }

    public void setChangeNoShowBeforeDept(Boolean changeNoShowBeforeDept) {
        isChangeNoShowBeforeDept = changeNoShowBeforeDept;
    }

    public Boolean getChangeNoShowAfterDept() {
        return isChangeNoShowAfterDept;
    }

    public void setChangeNoShowAfterDept(Boolean changeNoShowAfterDept) {
        isChangeNoShowAfterDept = changeNoShowAfterDept;
    }
}
