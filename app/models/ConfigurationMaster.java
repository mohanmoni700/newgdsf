package models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.db.ebean.Model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.List;

@Entity
@Table(name = "configuration_master")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigurationMaster extends Model {

    static Logger logger = LoggerFactory.getLogger("gds");
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "config_key", unique = true)
    private String configKey;
    @Column(name = "config_value")
    private String configValue;
    @Column(name = "config_description")
    private String configDescription;
    @Column(name = "config_type")
    private String configType;
    @Column(name = "is_active")
    private boolean isActive;
    @Column(name = "account_id")
    private Long accountId;
    @Column(name = "crewop_id")
    private Long crewopId;
    @Column(name = "is_account_specific",columnDefinition = "boolean default false")
    private boolean isAccountSpecific;
    @Column(name = "is_crewop_specific",columnDefinition = "boolean default false")
    private boolean isCrewopSpecific;
    @Column(name = "is_system_config",columnDefinition = "boolean default true")
    private boolean isSystemConfig;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    public String getConfigDescription() {
        return configDescription;
    }

    public void setConfigDescription(String configDescription) {
        this.configDescription = configDescription;
    }

    public String getConfigType() {
        return configType;
    }

    public void setConfigType(String configType) {
        this.configType = configType;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getCrewopId() {
        return crewopId;
    }

    public void setCrewopId(Long crewopId) {
        this.crewopId = crewopId;
    }

    public boolean isAccountSpecific() {
        return isAccountSpecific;
    }

    public void setAccountSpecific(boolean accountSpecific) {
        isAccountSpecific = accountSpecific;
    }

    public boolean isCrewopSpecific() {
        return isCrewopSpecific;
    }

    public void setCrewopSpecific(boolean crewopSpecific) {
        isCrewopSpecific = crewopSpecific;
    }

    public boolean isSystemConfig() {
        return isSystemConfig;
    }

    public void setSystemConfig(boolean systemConfig) {
        isSystemConfig = systemConfig;
    }
    public static Model.Finder<Long, ConfigurationMaster> find = new Model.Finder<>(Long.class, ConfigurationMaster.class);

    public static ConfigurationMaster findConfigurationByKey(String configKey) {
        return find.query().where().eq("config_key", configKey).findUnique();
    }

    public static List<ConfigurationMaster> findAllConfigurations() {
        return find.query().where().eq("is_active", true).findList();
    }
    public static List<ConfigurationMaster> findAllConfigurationsByAccountId(Long accountId) {
        return find.query().where().eq("is_active", true).eq("account_id", accountId).findList();
    }
    public static List<ConfigurationMaster> findAllConfigurationsByCrewopId(Long crewopId) {
        return find.query().where().eq("is_active", true).eq("crewop_id", crewopId).findList();
    }
    public static List<ConfigurationMaster> findAllConfigurationsByAccountIdAndCrewopId(Long accountId, Long crewopId) {
        return find.query().where().eq("is_active", true).eq("account_id", accountId).eq("crewop_id", crewopId).findList();
    }
    public static List<ConfigurationMaster> findAllSystemConfigurations() {
        return find.query().where().eq("is_active", true).eq("is_system_config", true).findList();
    }
}
