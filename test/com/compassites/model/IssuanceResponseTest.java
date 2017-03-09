package com.compassites.model;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import com.compassites.model.IssuanceResponse;

import java.util.Date;

import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.inMemoryDatabase;
import static play.test.Helpers.start;

/**
 * Created by jaya on 9/3/17.
 */
public class IssuanceResponseTest {

    private IssuanceResponse issuanceResponse;

    @Before
    public void setup(){
        issuanceResponse=new IssuanceResponse();
        issuanceResponse.setErrorCode("xyz");
    }

    @Test
    public void testgetErrorCode(){
        Assert.assertNotNull(issuanceResponse.getErrorCode());
    }

    @Test
    public void testgetErrorCode2(){
        Assert.assertNull(issuanceResponse.getErrorCode());
    }


    @Test
    public void testObject(){
        Assert.assertNotNull(issuanceResponse);
    }




}
