import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.ApplicationContext;
import play.Configuration;
import play.GlobalSettings;
import play.Application;
import configs.PrometheusConfig;

import configs.AppConfig;
import configs.DataConfig;
import play.Logger;
import utils.SystemUtility;

import java.io.File;

public class Global extends GlobalSettings {

    private ApplicationContext ctx;

    @Override
    public void onStart(Application app) {
        PrometheusConfig.init();
        ctx = new AnnotationConfigApplicationContext(AppConfig.class, DataConfig.class);
    }

    @Override
    public <A> A getControllerInstance(Class<A> clazz) {
        return ctx.getBean(clazz);
    }

}