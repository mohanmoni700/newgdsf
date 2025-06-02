package models;

import play.db.ebean.Model;

import javax.persistence.*;

@Entity
@Table(name = "BOMVS34C3_eligibility")
public class BOMVS34C3Eligibility extends Model {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column (name = "airline_code")
    private String airlineCode;

    @Column (name = "is_refund", columnDefinition = "TINYINT(1) default '0'")
    private boolean isRefund;

    @Column (name = "is_reissue", columnDefinition = "TINYINT(1) default '0'")
    private boolean isReissue;

    private static final Finder<Long, BOMVS34C3Eligibility> find = new Finder<>(Long.class, BOMVS34C3Eligibility.class);

    public static int findRowCount() {
        return find.findRowCount();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAirlineCode() {
        return airlineCode;
    }

    public void setAirlineCode(String airlineCode) {
        this.airlineCode = airlineCode;
    }

    public boolean isRefund() {
        return isRefund;
    }

    public void setRefund(boolean refund) {
        isRefund = refund;
    }

    public boolean isReissue() {
        return isReissue;
    }

    public void setReissue(boolean reissue) {
        isReissue = reissue;
    }

    public static BOMVS34C3Eligibility getEligibleAirlineCodeByValidatingCarrier (String validatingCarrierCode) {

        return  find.where().eq("airlineCode", validatingCarrierCode).findUnique();

    }

}
