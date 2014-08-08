package com.compassites.model.traveller;

import play.db.ebean.Model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * Created by mahendra-singh on 25/7/14.
 */
@Table(name="traveller_preferences")
@Entity
public class Preferences extends Model implements Serializable {
    @Column(name="id")
    @javax.persistence.Id
    private Integer Id;

    @Column(name="seat_preference")
    private String seatPreference;

    @Column(name="meal")
    private String meal;

    @Column(name="frequent_flyer_airlines")
    private String frequentFlyerAirlines;

    @Column(name="frequent_flyer_number")
    private String frequentFlyerNumber;

    public Integer getId() {
        return Id;
    }

    public void setId(Integer id) {
        Id = id;
    }

    public String getSeatPreference() {
        return seatPreference;
    }

    public void setSeatPreference(String seatPreference) {
        this.seatPreference = seatPreference;
    }

    public String getMeal() {
        return meal;
    }

    public void setMeal(String meal) {
        this.meal = meal;
    }

    public String getFrequentFlyerAirlines() {
        return frequentFlyerAirlines;
    }

    public void setFrequentFlyerAirlines(String frequentFlyerAirlines) {
        this.frequentFlyerAirlines = frequentFlyerAirlines;
    }

    public String getFrequentFlyerNumber() {
        return frequentFlyerNumber;
    }

    public void setFrequentFlyerNumber(String frequentFlyerNumber) {
        this.frequentFlyerNumber = frequentFlyerNumber;
    }
}
