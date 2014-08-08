package com.compassites.model.traveller;

import play.db.ebean.Model;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Created by mahendra-singh on 25/7/14.
 */
@Table(name="visa_details")
@Entity
public class VisaDetails extends Model implements Serializable {
    @OneToOne
    @JoinColumn(name="traveller",nullable = false)
    public Traveller traveller;

    @Column(name="id")
    @javax.persistence.Id
    private Integer Id;
    @Column(name="destination_visa_number")
    private String destinationVisaNumber;
    @Column(name="us_visa_number")
    private String usVisaNumber;
    @Column(name="schengen_visa_number")
    private String schengenVisaNumber;

    public Integer getId() {
        return Id;
    }

    public void setId(Integer id) {
        Id = id;
    }

    public String getDestinationVisaNumber() {
        return destinationVisaNumber;
    }

    public void setDestinationVisaNumber(String destinationVisaNumber) {
        this.destinationVisaNumber = destinationVisaNumber;
    }

    public String getUsVisaNumber() {
        return usVisaNumber;
    }

    public void setUsVisaNumber(String usVisaNumber) {
        this.usVisaNumber = usVisaNumber;
    }

    public String getSchengenVisaNumber() {
        return schengenVisaNumber;
    }

    public void setSchengenVisaNumber(String schengenVisaNumber) {
        this.schengenVisaNumber = schengenVisaNumber;
    }
}
