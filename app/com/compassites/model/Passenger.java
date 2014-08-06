package com.compassites.model;

/**
 * Created with IntelliJ IDEA.
 * User: Renu
 * Date: 5/22/14
 * Time: 2:22 PM
 * To change this template use File | Settings | File Templates.
 */
public class Passenger {
    private String name;
    private Integer age;
    private PassengerTypeCode passengerType;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public PassengerTypeCode getPassengerType() {
        return passengerType;
    }

    public void setPassengerType(PassengerTypeCode passengerType) {
        this.passengerType = passengerType;
    }
}
