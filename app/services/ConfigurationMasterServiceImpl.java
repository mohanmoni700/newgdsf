package services;

import models.ConfigurationMaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConfigurationMasterServiceImpl implements ConfigurationMasterService {

    static Logger logger = LoggerFactory.getLogger("gds");

    @Override
    public List<ConfigurationMaster> getAllConfigurations(long accountId, long crewopId, boolean isAccountSpecific, boolean isCrewopSpecific, boolean isSystemConfig, String category) {
        return null;
    }
}
