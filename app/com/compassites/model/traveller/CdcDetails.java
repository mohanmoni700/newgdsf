package com.compassites.model.traveller;

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
public class CdcDetails extends Model implements Serializable{
    @Column(name="id")
    @javax.persistence.Id
    private Integer Id;
    @Column(name="cdc_number")
    private String cdcNumber;
    @Column(name="place_of_issue")
    private String placeOfIssue;
    @Column(name="date_of_expiry")
    private Date dateOfExpiry;

    public Integer getId() {
        return Id;
    }

    public void setId(Integer id) {
        Id = id;
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

    public Date getDateOfExpiry() {
        return dateOfExpiry;
    }

    public void setDateOfExpiry(Date dateOfExpiry) {
        this.dateOfExpiry = dateOfExpiry;
    }
}
