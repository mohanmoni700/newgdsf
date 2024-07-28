package configs;

import com.compassites.constants.TraveloMatrixConstants;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import play.Play;
import play.libs.ws.WS;
import play.libs.ws.WSRequestHolder;

import java.util.Collections;

@Configuration
public class WsConfig {

    static org.slf4j.Logger logger = LoggerFactory.getLogger("gds");

    static org.slf4j.Logger amadeusLogger = LoggerFactory.getLogger("travelomatrix");

    public String url;
   public String domain;
   public String userName;
    public String password;
    public String system;


    public WSRequestHolder wsrholder;


    public  WS ws = new WS();

    public WSRequestHolder getRequestHolder(String api){
        url = play.Play.application().configuration().getString(TraveloMatrixConstants.tmurlKey)+api;
        domain=play.Play.application().configuration().getString(TraveloMatrixConstants.domainKey);
        userName=play.Play.application().configuration().getString(TraveloMatrixConstants.userNameKey);
        password=play.Play.application().configuration().getString(TraveloMatrixConstants.passwordKey);
        system=play.Play.application().configuration().getString(TraveloMatrixConstants.systemKey);

        wsrholder= ws.url(url).setHeader(TraveloMatrixConstants.domainKey, domain).
                setHeader(TraveloMatrixConstants.userNameKey, userName).
                setHeader(TraveloMatrixConstants.passwordKey, password).
                setHeader(TraveloMatrixConstants.systemKey, system).
                setHeader(TraveloMatrixConstants.contentKey, TraveloMatrixConstants.contentValue);
        return wsrholder;
    }

}
