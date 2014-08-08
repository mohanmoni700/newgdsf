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
@Table(name = "passport_details")
@Entity
public class PassportDetails extends Model implements Serializable{
    @Column(name="id")
    @javax.persistence.Id
    private Integer Id;
    @Column(name = "passport_number")
    private String passportNumber;
    @Column(name = "passport_country")
    private String passportCountry;
    @Column(name="place_of_issue")
    private String placeOfIssue;
    @Column(name="date_of_issue")
    private Date dateOfIssue;
    @Column(name="date_of_expiry")
    private Date dateOfExpiry;

    public Integer getId() {
        return Id;
    }

    public void setId(Integer id) {
        Id = id;
    }

    public String getPassportCountry() {
        return passportCountry;
    }

    public void setPassportCountry(String passportCountry) {
        this.passportCountry = passportCountry;
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

    public String getPassportNumber() {
        return passportNumber;
    }

    public void setPassportNumber(String passportNumber) {
        this.passportNumber = passportNumber;
    }
}
