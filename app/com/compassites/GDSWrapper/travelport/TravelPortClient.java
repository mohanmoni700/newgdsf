package com.compassites.GDSWrapper.travelport;

import javax.xml.ws.BindingProvider;
import java.text.SimpleDateFormat;

/**
 * Created with IntelliJ IDEA.
 * User: Renu
 * Date: 5/15/14
 * Time: 4:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class TravelPortClient {
    static final String REMOTE_HOST = "https://apac.universal-api.pp.travelport.com/B2BGateway/connect/uAPI";
    static final String USERNAME = "Universal API/uAPI2652954893-bf4f2606";
    static final String PASSWORD = "9n-L=Zg87i";
    static final String BRANCH = "P7024203";
    static final String GDS ="1G";
    static final String UAPI = "UAPI";
    public static SimpleDateFormat searchFormat = new SimpleDateFormat("yyyy-MM-dd");


    public static void setRequestContext(BindingProvider provider, String serviceName) {
        provider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                REMOTE_HOST + serviceName);

        provider.getRequestContext().put(BindingProvider.USERNAME_PROPERTY,
                USERNAME);
        provider.getRequestContext().put(BindingProvider.PASSWORD_PROPERTY,
                PASSWORD);
    }
}
