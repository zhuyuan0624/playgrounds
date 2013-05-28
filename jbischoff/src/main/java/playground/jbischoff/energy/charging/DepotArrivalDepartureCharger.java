package playground.jbischoff.energy.charging;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.transEnergySim.vehicles.api.BatteryElectricVehicle;
import org.matsim.contrib.transEnergySim.vehicles.api.Vehicle;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;

import playground.jbischoff.energy.log.SoCLog;
import playground.jbischoff.energy.log.SocLogRow;


public class DepotArrivalDepartureCharger implements AgentArrivalEventHandler,
		AgentDepartureEventHandler, ShutdownListener {

	
	private List<Id> depotLocations;
	private HashMap<Id, Vehicle> vehicles;
	private HashMap<Id, Double> arrivalTimes;
	private HashMap<Id, Double> socUponArrival;
	private final double MINIMUMCHARGETIME = 120.;
	private final double POWERINKW = 50.0; //max charge for Nissan Leaf
	private static final Logger log = Logger.getLogger(DepotArrivalDepartureCharger.class);
	private SoCLog soCLog;

	
	public DepotArrivalDepartureCharger(HashMap<Id, Vehicle> vehicles){
		this.vehicles=vehicles;
		this.arrivalTimes = new HashMap<Id, Double>();
		this.socUponArrival = new HashMap<Id, Double>();
		this.soCLog = new SoCLog();
	}
	
	@Override
	public void reset(int iteration) {
		this.arrivalTimes.clear();
	}
	


	@Override
	public void notifyShutdown(ShutdownEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		if (!isMonitoredVehicle(event.getPersonId())) return;
		if (!isBatteryElectricVehicle(event.getPersonId())) return;
		//technically no battery-electric-vehicles (e.g. trolleybusses) could also be handled here, therefore the exclusion
		if (!isAtDepotLocation(event.getLinkId())) return;
		if (!this.arrivalTimes.containsKey(event.getPersonId())) return;
		//assumption: Charging before first activity does not take place as cars are assumed to be charged in the morning, this goes in Line with the BatteryElectricVehicleImpl
		
		double chargetime = event.getTime() - this.arrivalTimes.get(event.getPersonId())-MINIMUMCHARGETIME;
		if (chargetime>0)
		log.info("Charged v: "+event.getPersonId()+" for "+chargetime+" old Soc: " + (this.socUponArrival.get(event.getPersonId())/3600/1000)+ " new SoC: " + (((BatteryElectricVehicle)this.vehicles.get(event.getPersonId())).getSocInJoules()/3600/1000)  );
		
		this.arrivalTimes.remove(event.getPersonId());
		this.socUponArrival.remove(event.getPersonId());

	}

	public void chargeAllVehiclesInDepots(double time, double duration){
		
	for (Entry<Id,Double> e : arrivalTimes.entrySet()){
		double chargeWatt = duration * POWERINKW * 1000;
		if (time-e.getValue() < MINIMUMCHARGETIME) continue;
		if (((BatteryElectricVehicle)this.vehicles.get(e.getKey())).getSocInJoules() >= ((BatteryElectricVehicle)this.vehicles.get(e.getKey())).getUsableBatteryCapacityInJoules()*0.8) continue;

		//max charge = 80% of usable battery according to CHAdeMO
		if (((BatteryElectricVehicle)this.vehicles.get(e.getKey())).getSocInJoules() + chargeWatt >= ((BatteryElectricVehicle)this.vehicles.get(e.getKey())).getUsableBatteryCapacityInJoules()*0.8)
		{
			chargeWatt = ((BatteryElectricVehicle)this.vehicles.get(e.getKey())).getUsableBatteryCapacityInJoules()*.8 -((BatteryElectricVehicle)this.vehicles.get(e.getKey())).getSocInJoules() ; 
		}
		((BatteryElectricVehicle)this.vehicles.get(e.getKey())).chargeBattery(chargeWatt);
//		log.info("Charged v"+e.getKey()+" with" + (chargeWatt/1000.) +" kW");

		
		}
	
	}
	
	public void refreshLog(double time){
		for (Entry<Id,Vehicle> e : this.vehicles.entrySet() ){
			if (!isBatteryElectricVehicle(e.getKey())) continue;
			double soc = ((BatteryElectricVehicle)e.getValue()).getSocInJoules();
			double rsoc = soc / ((BatteryElectricVehicle)e.getValue()).getUsableBatteryCapacityInJoules();
			this.soCLog.add(new SocLogRow(e.getKey(),time,soc,rsoc));
		}
		
	}
	
	@Override
	public void handleEvent(AgentArrivalEvent event) {
		if (!isMonitoredVehicle(event.getPersonId())) return;
		if (!isBatteryElectricVehicle(event.getPersonId())) return;
		if (!isAtDepotLocation(event.getLinkId())) return;
		
		this.socUponArrival.put(event.getPersonId(), ((BatteryElectricVehicle)this.vehicles.get(event.getPersonId())).getSocInJoules());
		this.arrivalTimes.put(event.getPersonId(),event.getTime());
		
	}
	
	public void setDepotLocations(List<Id> depots){
		this.depotLocations=depots;
	}
	
	private boolean isMonitoredVehicle(Id agentId){
		return (this.vehicles.containsKey(agentId));
	}
	private boolean isAtDepotLocation(Id linkId){
		return (this.depotLocations.contains(linkId));
	}
	private boolean isBatteryElectricVehicle(Id agentId){
		return (this.vehicles.get(agentId) instanceof BatteryElectricVehicle);
	}
	public SoCLog getSoCLog() {
		return soCLog;
	}

}
