package dto;

public class FareType {

    private String ffd;
    private String fareFamily;
    private String ftc;
    private String fareBasis;

    public String getFtc() {
        return ftc;
    }

    public void setFtc(String ftc) {
        this.ftc = ftc;
    }

    public String getFfd() {
        return ffd;
    }

    public void setFfd(String ffd) {
        this.ffd = ffd;
    }

    public String getFareFamily() {
        return fareFamily;
    }

    public void setFareFamily(String fareFamily) {
        this.fareFamily = fareFamily;
    }

    public String getFareBasis() {
        return fareBasis;
    }

    public void setFareBasis(String fareBasis) {
        this.fareBasis = fareBasis;
    }

}
