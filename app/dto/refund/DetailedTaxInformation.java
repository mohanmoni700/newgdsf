package dto.refund;

import java.math.BigDecimal;
import java.math.BigInteger;

public class DetailedTaxInformation {

    private BigDecimal amount;

    private String currency;

    private BigInteger decimalPlaces;

    private String category;

    private String isoCode;

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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getIsoCode() {
        return isoCode;
    }

    public void setIsoCode(String isoCode) {
        this.isoCode = isoCode;
    }

}
