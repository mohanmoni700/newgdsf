package services;

import models.ConfigurationMaster;

import java.util.List;
import java.util.Map;

public interface ConfigurationMasterService {
    public Map<String,String> getAllConfigurations(long accountId, long crewopId, String category);
    public String getValueByKey(long accountId,
                                       long crewopId, String category, String key);
    public void refreshConfigs();
    public String getConfig(String key);
}
