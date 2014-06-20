package com.compassites.model.amadeus;

import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class AmadeusSession implements Serializable{


    private String sessionId;
    private String sequenceNumber;

    public String getSecurityToken() {
        return securityToken;
    }

    public void setSecurityToken(String securityToken) {
        this.securityToken = securityToken;
    }

    public String getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(String sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    private String securityToken;
}
