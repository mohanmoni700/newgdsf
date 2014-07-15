package com.compassites.model;

import java.io.Serializable;

/**
 * Created by mahendra-singh on 11/7/14.
 */
public class PricingMessage implements Serializable{
    private String textSubjectQualifier;
    private String description;

    public String getTextSubjectQualifier() {
        return textSubjectQualifier;
    }

    public void setTextSubjectQualifier(String textSubjectQualifier) {
        this.textSubjectQualifier = textSubjectQualifier;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
