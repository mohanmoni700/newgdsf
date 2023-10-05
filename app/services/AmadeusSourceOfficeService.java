package services;
import models.FlightSearchOffice;
import org.springframework.stereotype.Service;
import play.Play;
import play.Configuration;
import java.util.*;

@Service
public class AmadeusSourceOfficeService {
    private  List<FlightSearchOffice> sourceOffices;
    private  List<FlightSearchOffice> partnerOffices;

    public AmadeusSourceOfficeService() {
        this.sourceOffices = new ArrayList<>();
        this.partnerOffices = new ArrayList<>();
        populateOffices();
    }

    private void populateOffices() {
        Configuration config = Play.application().configuration();

        List<Object> sourceOfficeIds = config.getList("amadeus.SOURCE_OFFICE.id");
        List<Object> sourceOfficeNames = config.getList("amadeus.SOURCE_OFFICE.name");

        List<Object> partnerOfficeIds = config.getList("amadeus.SOURCE_OFFICE_OF_PARTNER.id");
        List<Object> partnerOfficeNames = config.getList("amadeus.SOURCE_OFFICE_OF_PARTNER.name");

        if (sourceOfficeIds == null || sourceOfficeNames == null) {
            throw new RuntimeException("Office configurations not found");
        }

        if (sourceOfficeIds.size() != sourceOfficeNames.size() ) {
            throw new RuntimeException("Mismatch between IDs and names in configuration");
        }

        for (int i = 0; i < sourceOfficeIds.size(); i++) {
            String id = (String) sourceOfficeIds.get(i);
            String name = (String) sourceOfficeNames.get(i);
            sourceOffices.add(new FlightSearchOffice(id, name, false));
        }

        if (partnerOfficeIds != null && partnerOfficeNames != null &&
                partnerOfficeIds.size() == partnerOfficeNames.size()) {
           // throw new RuntimeException("Office configurations not found");
            for (int i = 0; i < partnerOfficeIds.size(); i++) {
                String id = (String) partnerOfficeIds.get(i);
                String name = (String) partnerOfficeNames.get(i);
                partnerOffices.add(new FlightSearchOffice(id, name, true));
            }
        }
    }

    private void populateOfficesOld() {
        Configuration config = Play.application().configuration();
        //String officeId = Play.application().configuration().getString("amadeus.SOURCE_OFFICE_DEFAULT");
        Configuration sourceOfficeConfig = config.getConfig("amadeus.SOURCE_OFFICE");
        Configuration partnerOfficeConfig = config.getConfig("amadeus.SOURCE_OFFICE_OF_PARTNERS");

        if (sourceOfficeConfig == null || partnerOfficeConfig == null) {
            throw new RuntimeException("SOURCE_OFFICE or SOURCE_OFFICE_OF_PARTNERS not found");
        }

        Set<String> sourceKeys = sourceOfficeConfig.keys();
        for (String key : sourceKeys) {
            String value = sourceOfficeConfig.getString(key);
            FlightSearchOffice office = new FlightSearchOffice(key, value, false);
            sourceOffices.add(office);
        }

        Set<String> partnerKeys = partnerOfficeConfig.keys();
        for (String key : partnerKeys) {
            String value = partnerOfficeConfig.getString(key);
            FlightSearchOffice office = new FlightSearchOffice(key, value, true);
            partnerOffices.add(office);
        }
    }

    public List<FlightSearchOffice> getSourceOffices() {
        return sourceOffices;
    }

    public List<FlightSearchOffice> getPartnerOffices() {
        return partnerOffices;
    }

    public List<FlightSearchOffice> getAllOffices() {
        List<FlightSearchOffice> allOffices = new ArrayList<>(sourceOffices);
        allOffices.addAll(partnerOffices);
        return allOffices;
    }

    public FlightSearchOffice getOfficeById(String officeId) {
        Optional<FlightSearchOffice> foundOffice = getAllOffices().stream()
                .filter(office -> officeId.equals(office.getOfficeId()))
                .findFirst();
        if (foundOffice.isPresent()) {
            return foundOffice.get();
        } else {
            return null;
        }
    }
}
