package com.compassites.model.traveller;

import play.db.ebean.Model;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Created by mahendra-singh on 25/7/14.
 */
@Entity
@Table(name = "traveller")
public class Traveller extends Model implements Serializable {
    @Id
    @Column(name = "id")
    @GeneratedValue
    private Integer id;

    @OneToOne(cascade=CascadeType.PERSIST)
    private PersonalDetails personalDetails;
    @OneToOne(cascade=CascadeType.PERSIST)
    private Preferences preferences;
    @OneToOne(cascade=CascadeType.PERSIST)
    private PassportDetails passportDetails;
    @OneToOne(cascade=CascadeType.PERSIST)
    private CdcDetails cdcDetails;
    @OneToOne(cascade=CascadeType.PERSIST)
    private VisaDetails visaDetails;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public PersonalDetails getPersonalDetails() {
        return personalDetails;
    }

    public void setPersonalDetails(PersonalDetails personalDetails) {
        this.personalDetails = personalDetails;
    }

    public Preferences getPreferences() {
        return preferences;
    }

    public void setPreferences(Preferences preferences) {
        this.preferences = preferences;
    }

    public PassportDetails getPassportDetails() {
        return passportDetails;
    }

    public void setPassportDetails(PassportDetails passportDetails) {
        this.passportDetails = passportDetails;
    }

    public CdcDetails getCdcDetails() {
        return cdcDetails;
    }

    public void setCdcDetails(CdcDetails cdcDetails) {
        this.cdcDetails = cdcDetails;
    }

    public VisaDetails getVisaDetails() {
        return visaDetails;
    }

    public void setVisaDetails(VisaDetails visaDetails) {
        this.visaDetails = visaDetails;
    }

    //public static Finder<Integer,Traveller> find=new Finder<Integer, Traveller>(Integer.class,Traveller.class);
}
