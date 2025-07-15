package com.compassites.service.login;

import com.compassites.model.amadeus.AmadeusSession;
import junit.framework.Assert;
import org.junit.Test;

/**
 * Created by mahendra-singh on 26/5/14.
 */
public class AmadeusLoginTest {
    @Test
    public void shouldLoginIntoAmadeusAndReturnSession(){
        AmadeusLogin amadeusLogin=new AmadeusLogin();
        AmadeusSession amadeusSession=amadeusLogin.login(true);
        Assert.assertNotNull(amadeusLogin);
        Assert.assertNotNull(amadeusSession.getSecurityToken());
        Assert.assertNotNull(amadeusSession.getSequenceNumber());
        Assert.assertNotNull(amadeusSession.getSessionId());
    }
}
