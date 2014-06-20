package com.travelport.client;
import com.compassites.GDSWrapper.travelport.PingReqClient;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created with IntelliJ IDEA.
 * User: Renu
 * Date: 5/15/14
 * Time: 2:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestPingReqClient {
    @Test
    public void testPasses() {
        String expected = "Hello, JUnit!";
        String payloadResponse = PingReqClient.ping(expected);
        String payloadResponseTwo = PingReqClient.ping(expected + expected);
        assertEquals(payloadResponse, expected);
        assertEquals(payloadResponseTwo, expected+expected);
    }

}
