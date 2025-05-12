package dto;

import java.math.BigInteger;

public class FreeMealsDetails {

    private String mealName;
    private String mealCode;
    private String mealStatus;
    private BigInteger mealQuantity;
    private String comments;
    private String amadeusPaxReference;
    private String paxName;

    public String getMealName() {
        return mealName;
    }

    public void setMealName(String mealName) {
        this.mealName = mealName;
    }

    public String getMealCode() {
        return mealCode;
    }

    public void setMealCode(String mealCode) {
        this.mealCode = mealCode;
    }

    public String getMealStatus() {
        return mealStatus;
    }

    public void setMealStatus(String mealStatus) {
        this.mealStatus = mealStatus;
    }

    public BigInteger getMealQuantity() {
        return mealQuantity;
    }

    public void setMealQuantity(BigInteger mealQuantity) {
        this.mealQuantity = mealQuantity;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getAmadeusPaxReference() {
        return amadeusPaxReference;
    }

    public void setAmadeusPaxReference(String amadeusPaxReference) {
        this.amadeusPaxReference = amadeusPaxReference;
    }

    public String getPaxName() {
        return paxName;
    }

    public void setPaxName(String paxName) {
        this.paxName = paxName;
    }

}
