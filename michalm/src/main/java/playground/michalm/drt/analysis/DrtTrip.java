/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.michalm.drt.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

public class DrtTrip implements Comparable<DrtTrip>{
	private final double departureTime; 
	private final Id<Person> person; 
	private final Id<Vehicle> vehicle; 
	private final Id<Link> fromLinkId; 
	private final double waitTime;
	private double travelTime = Double.NaN;
	private double travelDistance_m = Double.NaN;
	private Id<Link> toLink = null;
	private double arrivalTime = Double.NaN;
	static final String demitter = ";";
	public static final String HEADER = "departureTime"+demitter+"personId"+demitter+"vehicleId"+demitter+"fromLinkId"+demitter+"toLinkId"+demitter+"waitTime"+demitter+"arrivalTime"+demitter+"travelTime"+"travelDistance_m";
	
	DrtTrip(double departureTime, Id<Person> person, Id<Vehicle> vehicle, Id<Link> fromLinkId,
			double waitTime) {
		this.departureTime = departureTime;
		this.person = person;
		this.vehicle = vehicle;
		this.fromLinkId = fromLinkId;
		this.waitTime = waitTime;
	}

	public Double getDepartureTime() {
		return departureTime;
	}

	public Id<Person> getPerson() {
		return person;
	}

	public Id<Vehicle> getVehicle() {
		return vehicle;
	}

	public Id<Link> getFromLinkId() {
		return fromLinkId;
	}

	public double getWaitTime() {
		return waitTime;
	}
	

	public double getTravelTime() {
		return travelTime;
	}

	public void setTravelTime(double travelTime) {
		this.travelTime = travelTime;
	}

	public double getTravelDistance() {
		return travelDistance_m;
	}

	public void setTravelDistance(double travelDistance_m) {
		this.travelDistance_m = travelDistance_m;
	}

	public Id<Link> getToLink() {
		return toLink;
	}

	public void setToLink(Id<Link> toLink) {
		this.toLink = toLink;
	}

	public double getArrivalTime() {
		return arrivalTime;
	}

	public void setArrivalTime(double arrivalTime) {
		this.arrivalTime = arrivalTime;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(DrtTrip o) {
		return getDepartureTime().compareTo(o.getDepartureTime());
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getDepartureTime()+demitter+
				getPerson()+demitter+
				getVehicle()+demitter+
				getFromLinkId()+demitter+
				getToLink()+demitter+
				getWaitTime()+demitter+
				getArrivalTime()+demitter+
				getTravelTime()+demitter+
				getTravelDistance();
	}
	
	
}