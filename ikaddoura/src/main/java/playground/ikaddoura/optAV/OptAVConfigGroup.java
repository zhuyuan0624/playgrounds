/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.ikaddoura.optAV;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * 
 * @author ikaddoura
 */

public class OptAVConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "optAV" ;
	
	public OptAVConfigGroup() {
		super(GROUP_NAME);
	}
	
	private boolean accountForCongestion = false;
	private boolean accountForNoise = false;
	private boolean chargeOperatingCostsFromPassengers = true;
	private boolean chargeSAVTollsFromPassengers = true;
	
	private boolean tagInitialCarUsers = true;
	
	private boolean runDefaultAnalysis = true;
	
	private double fixCostsSAVinsteadOfCar = -10.; // negative values are interpreted as a revenue for no longer owning a car
	private double dailyFixCostAllSAVusers = 5.;

	private SAVTollingApproach optAVApproach = SAVTollingApproach.NoPricing;
			
	public enum SAVTollingApproach {
		NoPricing, PrivateAndExternalCost, ExternalCost
	}
	
	@StringGetter( "accountForCongestion" )
	public boolean isAccountForCongestion() {
		return accountForCongestion;
	}

	@StringSetter( "accountForCongestion" )
	public void setAccountForCongestion(boolean accountForCongestion) {
		this.accountForCongestion = accountForCongestion;
	}

	@StringGetter( "accountForNoise" )
	public boolean isAccountForNoise() {
		return accountForNoise;
	}
	
	@StringSetter( "accountForNoise" )
	public void setAccountForNoise(boolean accountForNoise) {
		this.accountForNoise = accountForNoise;
	}

	@StringGetter( "optAVApproach" )
	public SAVTollingApproach getOptAVApproach() {
		return optAVApproach;
	}

	@StringSetter( "optAVApproach" )
	public void setOptAVApproach(SAVTollingApproach optAVApproach) {
		this.optAVApproach = optAVApproach;
	}

	@StringGetter( "chargeOperatingCostsFromPassengers" )
	public boolean isChargeOperatingCostsFromPassengers() {
		return chargeOperatingCostsFromPassengers;
	}

	@StringSetter( "chargeOperatingCostsFromPassengers" )
	public void setChargeOperatingCostsFromPassengers(boolean chargeOperatingCostsFromPassengers) {
		this.chargeOperatingCostsFromPassengers = chargeOperatingCostsFromPassengers;
	}

	@StringGetter( "chargeSAVTollsFromPassengers" )
	public boolean isChargeSAVTollsFromPassengers() {
		return chargeSAVTollsFromPassengers;
	}

	@StringSetter( "chargeSAVTollsFromPassengers" )
	public void setChargeSAVTollsFromPassengers(boolean chargeSAVTollsFromPassengers) {
		this.chargeSAVTollsFromPassengers = chargeSAVTollsFromPassengers;
	}

	@StringGetter( "tagInitialCarUsers" )
	public boolean isTagInitialCarUsers() {
		return tagInitialCarUsers;
	}

	@StringSetter( "tagInitialCarUsers" )
	public void setTagInitialCarUsers(boolean tagInitialCarUsers) {
		this.tagInitialCarUsers = tagInitialCarUsers;
	}

	@StringGetter( "fixCostsSAVinsteadOfCar" )
	public double getFixCostsSAVinsteadOfCar() {
		return fixCostsSAVinsteadOfCar;
	}

	@StringSetter( "fixCostsSAVinsteadOfCar" )
	public void setFixCostsSAVinsteadOfCar(double fixCostsSAVinsteadOfCar) {
		this.fixCostsSAVinsteadOfCar = fixCostsSAVinsteadOfCar;
	}

	@StringGetter( "dailyFixCostAllSAVusers" )
	public double getDailyFixCostAllSAVusers() {
		return dailyFixCostAllSAVusers;
	}

	@StringSetter( "dailyFixCostAllSAVusers" )
	public void setDailyFixCostAllSAVusers(double fixCostSAV) {
		this.dailyFixCostAllSAVusers = fixCostSAV;
	}

	@StringGetter( "runDefaultAnalysis" )
	public boolean isRunDefaultAnalysis() {
		return runDefaultAnalysis;
	}

	@StringSetter( "runDefaultAnalysis" )
	public void setRunDefaultAnalysis(boolean runDefaultAnalysis) {
		this.runDefaultAnalysis = runDefaultAnalysis;
	}
	
	
	
}

