package com.compassites.model.traveller;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import play.db.ebean.Model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;

/**
 * Created by mahendra-singh on 25/7/14.
 */
@Table(name = "cdc_details")
@Entity
public class CdcDetails extends Model implements Serializable {
    @JsonIgnore
    @Column(name="id")
    @javax.persistence.Id
    private Integer id;
    @Column(name="cdc_number")
    private String cdcNumber;
    @Column(name="place_of_issue")
    private String placeOfIssue;

    @JsonProperty("cdcDateOfIssue")
    @Column(name="date_of_issue")
    private Date dateOfIssue;

    @JsonProperty("cdcDateOfExpiry")
    @Column(name="date_of_expiry")
    private Date dateOfExpiry;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCdcNumber() {
        return cdcNumber;
    }

    public void setCdcNumber(String cdcNumber) {
        this.cdcNumber = cdcNumber;
    }

    public String getPlaceOfIssue() {
        return placeOfIssue;
    }

    public void setPlaceOfIssue(String placeOfIssue) {
        this.placeOfIssue = placeOfIssue;
    }

    public Date getDateOfIssue() {
        return dateOfIssue;
    }

    public void setDateOfIssue(Date dateOfIssue) {
        this.dateOfIssue = dateOfIssue;
    }

    public Date getDateOfExpiry() {
        return dateOfExpiry;
    }

    public void setDateOfExpiry(Date dateOfExpiry) {
        this.dateOfExpiry = dateOfExpiry;
    }
}
