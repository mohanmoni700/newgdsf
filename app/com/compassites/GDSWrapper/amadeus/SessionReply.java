package com.compassites.GDSWrapper.amadeus;

import com.amadeus.xml.vlsslr_06_1_1a.SecurityAuthenticateReply;
import com.amadeus.xml.ws._2009._01.wbs_session_2_0.Session;

/**
 * Created by mahendra-singh on 26/5/14.
 */
public class SessionReply {
    private SecurityAuthenticateReply securityAuthenticateReply;
    private Session session;

    public SecurityAuthenticateReply getSecurityAuthenticateReply() {
        return securityAuthenticateReply;
    }

    public void setSecurityAuthenticateReply(SecurityAuthenticateReply securityAuthenticateReply) {
        this.securityAuthenticateReply = securityAuthenticateReply;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }
}
