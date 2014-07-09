package com.compassites.service.login;

import configs.AppConfig;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;
import services.RetryOnFailure;

/**
 * Created by user on 08-07-2014.
 */
@Component
public class RetryOnFailureTest {

    @RetryOnFailure(attempts = 2, delay = 2000, exception = RuntimeException.class )
    public void testRetry()
    {
        System.out.println("Entered ....................");
        throw new RuntimeException("Forcing an exception");

    }

    public static void main(String[] args)
    {
        ApplicationContext ctx = new AnnotationConfigApplicationContext(AppConfig.class);
        // Get me my spring managed bean
        final RetryOnFailureTest retryFailureTest = ctx.getBean(RetryOnFailureTest.class);
        retryFailureTest.testRetry();

       // URL url = Play.application().classloader().getResource("amdeusErrorCodes.properties");
        //System.out.println(url);

       /* Properties prop = new Properties();
        InputStream input = null;
        try {
            input = new FileInputStream("conf/amadeusErrorCodes.properties");
            // load a properties file
            prop.load(input);
            System.out.println(prop.getProperty("1181")+" : "+ prop.containsKey("1181"));
        }catch (Exception e){
            e.printStackTrace();
        }*/
     }

}

