package utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import org.springframework.stereotype.Service;

/**
 * Created by yaseen on 30-09-2015.
 */
@Service
public class CustomObjectMapper extends ObjectMapper {
    public CustomObjectMapper() {
        this.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        this.registerModule(new JodaModule());
    }
}
