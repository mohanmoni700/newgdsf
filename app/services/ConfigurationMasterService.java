package services;

import models.ConfigurationMaster;

import java.util.List;

public interface ConfigurationMasterService {
    public List<ConfigurationMaster> getAllConfigurations(long accountId,
                                                         long crewopId,
                                                         boolean isAccountSpecific,
                                                         boolean isCrewopSpecific,
                                                         boolean isSystemConfig, String category);
}
