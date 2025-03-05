package com.compassites.GDSWrapper.indigo;

import com.navitaire.schemas.webservices.LogonResponse;
import org.springframework.stereotype.Service;

import java.io.Serializable;

@Service
public class IndigoLogin implements Serializable {
    public LogonResponse login() {
        SessionHandler sessionHandler = null;
        try {
            sessionHandler = new SessionHandler();
        } catch (Exception e) {
            e.printStackTrace();
        }
        LogonResponse logonResponse = sessionHandler.login();
        return logonResponse;
    }
}
