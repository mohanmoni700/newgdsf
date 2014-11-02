package com.compassites.model.traveller;

import play.db.ebean.Model;
import utils.DateUtility;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.joda.time.PeriodType;

import com.compassites.model.PassengerTypeCode;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by mahendra-singh on 25/7/14.
 */
@Table(name="personal_details")
@Entity
public class PersonalDetails extends Model implements Serializable  {

    @Column(name="id")
    @javax.persistence.Id
    private Integer Id;
    @Column(name = "salutaion")
    private String salutation;
    @Column(name = "first_name")
    private String firstName;
    @Column(name = "middle_name")
    private String middleName;
    @Column(name = "last_name")
    private String lastName;
    @Column(name = "date_of_birth")
    private Date dateOfBirth;
    @Column(name = "rank")
    private String rank;
    @Column(name = "gender")
    private String gender;
    
    public PassengerTypeCode getPassengerType() {
    	return DateUtility.getPassengerTypeFromDOB(dateOfBirth);
	}

    public Integer getId() {
        return Id;
    }

    public void setId(Integer id) {
        Id = id;
    }

    public String getSalutation() {
        return salutation;
    }

    public void setSalutation(String salutation) {
        this.salutation = salutation;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Date getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }
}
