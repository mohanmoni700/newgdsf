package com.compassites.model.amadeus;

public class AmadeusPaxInformation {

    private String salutation;

    private String firstName;

    private String lastName;

    private String fullName;

    private String paxRef;

    private String lineNumber;

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

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPaxRef() {
        return paxRef;
    }

    public void setPaxRef(String paxRef) {
        this.paxRef = paxRef;
    }

    public String getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(String lineNumber) {
        this.lineNumber = lineNumber;
    }

}
