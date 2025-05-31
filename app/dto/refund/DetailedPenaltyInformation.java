package dto.refund;

import java.math.BigDecimal;
import java.math.BigInteger;

public class DetailedPenaltyInformation {

    private BigDecimal percent;

    private BigDecimal amount;

    private String currency;

    private BigInteger decimalPlaces;

    private String penaltyType;

    public BigDecimal getPercent() {
        return percent;
    }

    public void setPercent(BigDecimal percent) {
        this.percent = percent;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigInteger getDecimalPlaces() {
        return decimalPlaces;
    }

    public void setDecimalPlaces(BigInteger decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
    }

    public String getPenaltyType() {
        return penaltyType;
    }

    public void setPenaltyType(String penaltyType) {
        this.penaltyType = penaltyType;
    }

}
