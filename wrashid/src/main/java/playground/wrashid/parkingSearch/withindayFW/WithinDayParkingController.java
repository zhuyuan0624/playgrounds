/* *********************************************************************** *
 * project: org.matsim.*
 * WithinDayParkingController.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.wrashid.parkingSearch.withindayFW;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map.Entry;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.MultiModalTravelTimeWrapperFactory;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelCostCalculatorFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelTimeFactory;
import org.matsim.facilities.algorithms.WorldConnectLocations;
import org.matsim.withinday.controller.WithinDayController;
import org.matsim.withinday.replanning.modules.ReplanningModule;

import playground.wrashid.parkingSearch.mobsim.ParkingQSimFactory;
import playground.wrashid.parkingSearch.withinday.InsertParkingActivities;
import playground.wrashid.parkingSearch.withinday.ParkingInfrastructure;
import playground.wrashid.parkingSearch.withindayFW.impl.ParkingStrategyActivityMapperFW;
import playground.wrashid.parkingSearch.withindayFW.impl.ParkingStrategyManager;
import playground.wrashid.parkingSearch.withindayFW.randomTestStrategy.RandomSearchIdentifier;
import playground.wrashid.parkingSearch.withindayFW.randomTestStrategy.RandomSearchReplannerFactory;
import playground.wrashid.parkingSearch.withindayFW.randomTestStrategyFW.ParkingStrategy;

public class WithinDayParkingController extends WithinDayController implements StartupListener, ReplanningListener, BeforeMobsimListener {

	/*
	 * How many parallel Threads shall do the Replanning.
	 */
	protected int numReplanningThreads = 8;

	//protected RandomSearchIdentifier randomSearchIdentifier;
	//protected RandomSearchReplannerFactory randomSearchReplannerFactory;

	protected LegModeChecker legModeChecker;
	protected ParkingAgentsTracker parkingAgentsTracker;
	protected InsertParkingActivities insertParkingActivities;
	protected ParkingInfrastructure parkingInfrastructure;
	
	public WithinDayParkingController(String[] args) {
		super(args);
		
		// register this as a Controller Listener
		super.addControlerListener(this);
	}

	protected void initParkingStrategyFactories(){
		ParkingStrategyActivityMapperFW parkingStrategyActivityMapperFW=new ParkingStrategyActivityMapperFW();
		Collection<ParkingStrategy> parkingStrategies=new LinkedList<ParkingStrategy>();
		ParkingStrategyManager parkingStrategyManager=new ParkingStrategyManager(parkingStrategyActivityMapperFW, parkingStrategies);
		
		
		
		LeastCostPathCalculatorFactory factory = new AStarLandmarksFactory(this.network, new FreespeedTravelTimeAndDisutility(this.config.planCalcScore()));
		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) this.scenarioData.getPopulation().getFactory()).getModeRouteFactory();

		// create a copy of the MultiModalTravelTimeWrapperFactory and set the TravelTimeCollector for car mode
		MultiModalTravelTimeWrapperFactory timeFactory = new MultiModalTravelTimeWrapperFactory();
		for (Entry<String, PersonalizableTravelTimeFactory> entry : this.getMultiModalTravelTimeWrapperFactory().getPersonalizableTravelTimeFactories().entrySet()) {
			timeFactory.setPersonalizableTravelTimeFactory(entry.getKey(), entry.getValue());
		}
		timeFactory.setPersonalizableTravelTimeFactory(TransportMode.car, super.getTravelTimeCollectorFactory());
		
		TravelDisutilityFactory costFactory = new OnlyTimeDependentTravelCostCalculatorFactory();
		
		AbstractMultithreadedModule router = new ReplanningModule(config, network, costFactory, timeFactory, factory, routeFactory);
	
		// adding first random factory
		RandomSearchReplannerFactory randomSearchReplannerFactory = new RandomSearchReplannerFactory(this.getReplanningManager(), router, 1.0, this.scenarioData, parkingAgentsTracker);
		RandomSearchIdentifier randomSearchIdentifier = new RandomSearchIdentifier(parkingAgentsTracker, parkingInfrastructure); 
		this.getFixedOrderSimulationListener().addSimulationListener(randomSearchIdentifier);
		randomSearchReplannerFactory.addIdentifier(randomSearchIdentifier);
		ParkingStrategy parkingStrategy = new ParkingStrategy(randomSearchIdentifier);
		parkingStrategies.add(parkingStrategy);
		this.getReplanningManager().addDuringLegReplannerFactory(randomSearchReplannerFactory);
		parkingStrategyActivityMapperFW.addSearchStrategy(null,"home", parkingStrategy);
		parkingStrategyActivityMapperFW.addSearchStrategy(null,"work", parkingStrategy);
		
		// adding first random factory
		randomSearchReplannerFactory = new RandomSearchReplannerFactory(this.getReplanningManager(), router, 1.0, this.scenarioData, parkingAgentsTracker);
		randomSearchIdentifier = new RandomSearchIdentifier(parkingAgentsTracker, parkingInfrastructure); 
		this.getFixedOrderSimulationListener().addSimulationListener(randomSearchIdentifier);
		randomSearchReplannerFactory.addIdentifier(randomSearchIdentifier);		
		parkingStrategy = new ParkingStrategy(randomSearchIdentifier);
		parkingStrategies.add(parkingStrategy);
		this.getReplanningManager().addDuringLegReplannerFactory(randomSearchReplannerFactory);
		parkingStrategyActivityMapperFW.addSearchStrategy(null,"work", parkingStrategy);
		parkingStrategyActivityMapperFW.addSearchStrategy(null,"shopping", parkingStrategy);
		
		this.addControlerListener(parkingStrategyManager);
		this.getFixedOrderSimulationListener().addSimulationListener(parkingStrategyManager);
		
	}
	/*
	protected void initIdentifiers() {

		this.randomSearchIdentifier = new RandomSearchIdentifier(parkingAgentsTracker, parkingInfrastructure); 
		this.getFixedOrderSimulationListener().addSimulationListener(this.randomSearchIdentifier);
	}*/
	
	/*
	 * New Routers for the Replanning are used instead of using the controler's.
	 * By doing this every person can use a personalised Router.
	 */
	/*
	protected void initReplanners() {

		LeastCostPathCalculatorFactory factory = new AStarLandmarksFactory(this.network, new FreespeedTravelTimeAndDisutility(this.config.planCalcScore()));
		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) this.scenarioData.getPopulation().getFactory()).getModeRouteFactory();

		// create a copy of the MultiModalTravelTimeWrapperFactory and set the TravelTimeCollector for car mode
		MultiModalTravelTimeWrapperFactory timeFactory = new MultiModalTravelTimeWrapperFactory();
		for (Entry<String, PersonalizableTravelTimeFactory> entry : this.getMultiModalTravelTimeWrapperFactory().getPersonalizableTravelTimeFactories().entrySet()) {
			timeFactory.setPersonalizableTravelTimeFactory(entry.getKey(), entry.getValue());
		}
		timeFactory.setPersonalizableTravelTimeFactory(TransportMode.car, super.getTravelTimeCollectorFactory());
		
		TravelDisutilityFactory costFactory = new OnlyTimeDependentTravelCostCalculatorFactory();
		
		AbstractMultithreadedModule router = new ReplanningModule(config, network, costFactory, timeFactory, factory, routeFactory);
	
		
		
		this.randomSearchReplannerFactory = new RandomSearchReplannerFactory(this.getReplanningManager(), router, 1.0, this.scenarioData, parkingAgentsTracker);
		this.randomSearchReplannerFactory.addIdentifier(this.randomSearchIdentifier);		
		this.getReplanningManager().addDuringLegReplannerFactory(this.randomSearchReplannerFactory);
	}
	*/
	/*
	 * When the Controller Startup Event is created, the EventsManager
	 * has already been initialized. Therefore we can initialize now
	 * all Objects, that have to be registered at the EventsManager.
	 */
	@Override
	public void notifyStartup(StartupEvent event) {
				
		// connect facilities to network
		new WorldConnectLocations(this.config).connectFacilitiesWithLinks(getFacilities(), (NetworkImpl) getNetwork());
		
		super.initReplanningManager(numReplanningThreads);
		super.createAndInitTravelTimeCollector();
		super.createAndInitLinkReplanningMap();
		
		// ensure that all agents' plans have valid mode chains
		legModeChecker = new LegModeChecker(this.scenarioData, this.createRoutingAlgorithm());
		legModeChecker.setValidNonCarModes(new String[]{TransportMode.walk});
		legModeChecker.setToCarProbability(0.5);
		legModeChecker.run(this.scenarioData.getPopulation());
		
		parkingInfrastructure = new ParkingInfrastructure(this.scenarioData);
		this.getEvents().addHandler(this.parkingInfrastructure);
		
		parkingAgentsTracker = new ParkingAgentsTracker(this.scenarioData, 2000.0);
		this.getFixedOrderSimulationListener().addSimulationListener(this.parkingAgentsTracker);
		this.getEvents().addHandler(this.parkingAgentsTracker);
		
		insertParkingActivities = new InsertParkingActivities(scenarioData, this.createRoutingAlgorithm(), parkingInfrastructure);
		
		MobsimFactory mobsimFactory = new ParkingQSimFactory(insertParkingActivities, parkingInfrastructure);
		this.setMobsimFactory(mobsimFactory);
		
		//this.initIdentifiers();
		//this.initReplanners();
		initParkingStrategyFactories();
	}

	@Override
	public void notifyReplanning(ReplanningEvent event) {
		/*
		 * During the replanning the mode chain of the agents' selected plans
		 * might have been changed. Therefore, we have to ensure that the 
		 * chains are still valid.
		 */
		for (Person person : this.scenarioData.getPopulation().getPersons().values()) {
			legModeChecker.run(person.getSelectedPlan());			
		}
	}
		
	/*
	 * ===================================================================
	 * main
	 * ===================================================================
	 */
	public static void main(String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println("using default config");
			args=new String[]{"test/input/playground/wrashid/parkingSearch/withinday/chessboard/config_plans1.xml"};
//			args=new String[]{"test/input/playground/wrashid/parkingSearch/withinday/chessboard/config.xml"};
		}
		final WithinDayParkingController controller = new WithinDayParkingController(args);
		controller.setOverwriteFiles(true);
		
		
		
		controller.run();
		
		System.exit(0);
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		// TODO: here the parking strategy selection should happen ("parking replanning appropriate word?")
		
		// TODO: check, if plan changed or mode changed of plan => must reset all replanning for that parking activity
		
		double probabilityOfHighScoreStrategy=0.9;
		
		
		
		if (event.getIteration()>0){
		
			// TODO: make function, which gives back for given agentId and activity the set of parking strategy objects back.
			// these objects have not only the strategies in them, but also the score of each strategy
			
			// a strategy should be general, so that it can be shared among people (only small variable part per Agent, which is stored in HashMap in strategy).
			
		}
		
	}
}
