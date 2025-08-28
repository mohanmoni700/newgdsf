package services;

import com.avaje.ebean.Expr;
import com.compassites.constants.CacheConstants;
import models.ConfigurationMaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ConfigurationMasterServiceImpl implements ConfigurationMasterService {

    static Logger logger = LoggerFactory.getLogger("gds");

    @Autowired
    private RedisTemplate redisTemplate;

    public RedisTemplate getRedisTemplate() {
        return redisTemplate;
    }

    public void setRedisTemplate(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    @Override
    public Map<String,String> getAllConfigurations(long accountId, long crewopId, String category) {
        boolean isAccountSpecific = false;
        boolean isCrewopSpecific = false; // Assuming crewop specific by default
        boolean isSystemConfig = false; // Assuming system config by default
        if(crewopId != 0) {
            isCrewopSpecific = true; // If crewopId is 0, then it is not crewop specific
        }
        if(accountId != 0) {
            isAccountSpecific = true; // If accountId is 0, then it is not account specific
        }
        if(!isCrewopSpecific && !isAccountSpecific) {
            logger.warn("No specific configuration found for accountId: {}, crewopId: {}, category: {}, key: {}", accountId, crewopId, category);
            isSystemConfig = true; // If neither account nor crewop specific, then it is system config
        }
        Map<String,String> configMap = new HashMap<>();
        List<ConfigurationMaster> configurations = ConfigurationMaster.find
                .where()
                .or(
                        Expr.eq("account_id", accountId),
                        Expr.isNull("account_id")
                )
                .or(
                        Expr.eq("crewop_id", crewopId),
                        Expr.isNull("crewop_id")
                )
                .eq("is_account_specific", isAccountSpecific)
                .eq("is_crewop_specific", isCrewopSpecific)
                .eq("is_system_config", isSystemConfig)
                .eq("config_type", category)
                .findList();
        if (configurations != null && !configurations.isEmpty()) {
            for (ConfigurationMaster config : configurations) {
                configMap.put(config.getConfigKey(), config.getConfigValue());
            }
            logger.info("Configurations fetched successfully for accountId: {}, crewopId: {}, category: {}", accountId, crewopId, category);
            return configMap;
        } else {
            logger.warn("No configurations found for accountId: {}, crewopId: {}, category: {}", accountId, crewopId, category);
        }
        return null;
    }

    @Override
    public String getValueByKey(long accountId, long crewopId, String category, String key) {
        Map<String,String> configMap = getAllConfigurations(accountId, crewopId, category);
        return configMap != null ? configMap.get(key) : null;
    }

    @PostConstruct
    public void initConfig() {
        logger.info("Initializing ConfigurationMasterServiceImpl...");
        if (redisTemplate == null) {
            logger.error("RedisTemplate is not initialized. Please check your configuration.");
            throw new IllegalStateException("RedisTemplate is not initialized.");
        } else {
            logger.info("RedisTemplate is initialized successfully.");
            if (redisTemplate.opsForValue().get("Config") == null) {
                logger.info("Setting initial configuration values in Redis.");
                List<ConfigurationMaster> configurations = ConfigurationMaster.find.all();
                redisTemplate.opsForValue().set("Config", CacheConstants.CONFIG_INIT);
                for (ConfigurationMaster configurationMaster: configurations) {
                    redisTemplate.opsForValue().set(
                            configurationMaster.getConfigKey(),
                            configurationMaster.getConfigValue()
                    );
                }
            } else {
                logger.info("Configuration values already exist in Redis. Skipping initialization.");
            }
        }
        logger.info("ConfigurationMasterServiceImpl initialized successfully.");
    }

    @Override
    public void refreshConfigs() {
        logger.info("Refreshing configurations...");
        initConfig();
        logger.info("Configurations refreshed successfully.");
    }

    @Override
    public String getConfig(String key) {
        String value = "";
        logger.info("Fetching configuration for key: {}", key);
       if(redisTemplate.opsForValue().get(key) != null) {
            value = redisTemplate.opsForValue().get(key).toString();
            logger.info("Configuration value for key {}: {}", key, value);
            return value;
        } else {
           refreshConfigs();
       }
        logger.warn("No configuration found for key: {}", key);
        return null;
    }
}
