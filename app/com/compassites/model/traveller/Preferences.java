package com.compassites.model.traveller;

/**
 * Created by mahendra-singh on 25/7/14.
 */
public class Preferences {

	private Long id;

	private String seatPreference;

	private String meal;

	private String frequentFlyerAirlines;

	private String frequentFlyerNumber;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getSeatPreference() {
		return seatPreference;
	}

	public void setSeatPreference(String seatPreference) {
		this.seatPreference = seatPreference;
	}

	public String getMeal() {
		return meal;
	}

	public void setMeal(String meal) {
		this.meal = meal;
	}

	public String getFrequentFlyerAirlines() {
		return frequentFlyerAirlines;
	}

	public void setFrequentFlyerAirlines(String frequentFlyerAirlines) {
		this.frequentFlyerAirlines = frequentFlyerAirlines;
	}

	public String getFrequentFlyerNumber() {
		return frequentFlyerNumber;
	}

	public void setFrequentFlyerNumber(String frequentFlyerNumber) {
		this.frequentFlyerNumber = frequentFlyerNumber;
	}

}
