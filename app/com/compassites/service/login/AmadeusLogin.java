package com.compassites.service.login;

import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.GDSWrapper.amadeus.SessionReply;
import com.compassites.model.amadeus.AmadeusSession;

import java.io.Serializable;

/**
 * Created by mahendra-singh on 26/5/14.
 */
public class AmadeusLogin implements Serializable{
    public AmadeusSession login(){

        ServiceHandler serviceHandler= null;
        try {
            serviceHandler = new ServiceHandler();
        } catch (Exception e) {
            e.printStackTrace();
        }
        AmadeusSession amadeusSession=map(serviceHandler.logIn());
        return amadeusSession;
    }

    private AmadeusSession map(SessionReply sessionReply){
        AmadeusSession amadeusSession=new AmadeusSession();
        amadeusSession.setSecurityToken(sessionReply.getSession().getSecurityToken());
        amadeusSession.setSequenceNumber(sessionReply.getSession().getSequenceNumber());
        amadeusSession.setSessionId(sessionReply.getSession().getSessionId());
        return amadeusSession;
    }
}
