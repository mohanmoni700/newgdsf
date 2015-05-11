package com.compassites.model;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

/**
 * Created with IntelliJ IDEA.
 * User: Renu
 * Date: 5/22/14
 * Time: 2:27 PM
 * To change this template use File | Settings | File Templates.
 */
@XmlType(name = "cabinClass")
@XmlEnum

public enum  CabinClass {

    @XmlEnumValue("First")
    FIRST("First"),
    @XmlEnumValue("Business")
    BUSINESS("Business"),
    @XmlEnumValue("Economy")
    ECONOMY("Economy"),
    @XmlEnumValue("PremiumEconomy")
    PREMIUM_ECONOMY("PremiumEconomy"),
    @XmlEnumValue("PremiumFirst")
    PREMIUM_FIRST("PremiumFirst"),
    @XmlEnumValue("PremiumBusiness")
    PREMIUM_BUSINESS("PremiumBusiness");

    private final String value;

    CabinClass(String v) {
        value = v;
    }

    public String upperValue() {
        return value.toUpperCase();
    }

    public String value(){
        return  value;
    }

    public static CabinClass fromValue(String v) {
        for (CabinClass c: CabinClass.values()) {
            if (c.value.equalsIgnoreCase(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
