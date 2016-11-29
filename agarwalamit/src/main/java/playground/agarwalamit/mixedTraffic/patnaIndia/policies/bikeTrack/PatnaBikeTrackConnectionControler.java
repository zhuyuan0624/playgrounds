/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.mixedTraffic.patnaIndia.policies.bikeTrack;

import java.io.File;
import java.util.Collection;
import javax.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ScoringParameterSet;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.TerminationCriterion;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.*;
import playground.agarwalamit.analysis.controlerListner.ModalShareControlerListner;
import playground.agarwalamit.analysis.controlerListner.ModalTravelTimeControlerListner;
import playground.agarwalamit.analysis.modalShare.ModalShareEventHandler;
import playground.agarwalamit.analysis.modalShare.ModalShareFromEvents;
import playground.agarwalamit.analysis.travelTime.ModalTravelTimeAnalyzer;
import playground.agarwalamit.analysis.travelTime.ModalTripTravelTimeHandler;
import playground.agarwalamit.mixedTraffic.patnaIndia.input.others.PatnaVehiclesGenerator;
import playground.agarwalamit.mixedTraffic.patnaIndia.router.FreeSpeedTravelTimeForBike;
import playground.agarwalamit.mixedTraffic.patnaIndia.scoring.PtFareEventHandler;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.utils.FileUtils;

/**
 * @author amit
 */

public class PatnaBikeTrackConnectionControler {

	private static String dir = FileUtils.RUNS_SVN+"/patnaIndia/run108/jointDemand/policies/0.15pcu/";
	private static String bikeTrack = PatnaUtils.INPUT_FILES_DIR + "/simulationInputs/network/shpNetwork/bikeTrack.xml.gz";

	private static int numberOfConnectors = 499;
	private static int updateConnectorsAfterIteration = 2;
	private static double reduceLinkLengthBy = 1.;
	private static boolean useBikeTravelTime = true;

	public static void main(String[] args) {

		if(args.length>0){
			dir= args[0];
			numberOfConnectors = Integer.valueOf(args[1]);
			updateConnectorsAfterIteration = Integer.valueOf(args[2]);
			bikeTrack = args[3];
			reduceLinkLengthBy = Double.valueOf(args[4]);
			useBikeTravelTime = Boolean.valueOf(args[5]);
		}

		Scenario scenario = getScenario();
		scenario.getConfig().controler().setOutputDirectory(dir+"bikeTrackConnectors_"+numberOfConnectors+"_"+updateConnectorsAfterIteration+"_"+reduceLinkLengthBy+"/");
		BikeConnectorControlerListner bikeConnectorControlerListner = new BikeConnectorControlerListner(numberOfConnectors, updateConnectorsAfterIteration, bikeTrack,
				 reduceLinkLengthBy);

		final Controler controler = new Controler(scenario);

		TerminationCriterion terminationCriterion = new MyTerminationCriteria(bikeConnectorControlerListner);

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				// plotting modal share over iterations
				this.bind(ModalShareEventHandler.class);
				this.addControlerListenerBinding().to(ModalShareControlerListner.class);

				this.bind(ModalTripTravelTimeHandler.class);
				this.addControlerListenerBinding().to(ModalTravelTimeControlerListner.class);

				// adding pt fare system based on distance
				this.addEventHandlerBinding().to(PtFareEventHandler.class);

				if (useBikeTravelTime) {
					addTravelTimeBinding(TransportMode.bike).to(FreeSpeedTravelTimeForBike.class);
				}
				addControlerListenerBinding().toInstance(bikeConnectorControlerListner);
				bind(TerminationCriterion.class).toInstance(terminationCriterion);
			}
		});

		// for PT fare system to work, make sure that util_dist and monetary dist rate for pt are zero.
		ModeParams mp = controler.getConfig().planCalcScore().getModes().get("pt");
		mp.setMarginalUtilityOfDistance(0.0);
		mp.setMonetaryDistanceRate(0.0);

		// add income dependent scoring function factory
		addScoringFunction(controler);

		controler.run();

		String outputDir = controler.getScenario().getConfig().controler().getOutputDirectory();

		new File(outputDir+"/analysis/").mkdir();
		String outputEventsFile = outputDir+"/output_events.xml.gz";
		// write some default analysis
		String userGroup = PatnaPersonFilter.PatnaUserGroup.urban.toString();
		ModalTravelTimeAnalyzer mtta = new ModalTravelTimeAnalyzer(outputEventsFile, userGroup, new PatnaPersonFilter());
		mtta.run();
		mtta.writeResults(outputDir+"/analysis/modalTravelTime_"+userGroup+".txt");

		ModalShareFromEvents msc = new ModalShareFromEvents(outputEventsFile, userGroup, new PatnaPersonFilter());
		msc.run();
		msc.writeResults(outputDir+"/analysis/modalShareFromEvents_"+userGroup+".txt");

		//StatsWriter.run(outputDir);
	}

	private static Scenario getScenario() {
		Config config = ConfigUtils.createConfig();

		String inputDir = dir+"/input/";
		String configFile = inputDir + "configBaseCaseCtd.xml";

		ConfigUtils.loadConfig(config, configFile);

		config.network().setInputFile(inputDir+"networkWithAllConnectors.xml.gz");
//		config.network().setInputFile(inputDir+"network.xml.gz");
		// time dependent network for network change events
		config.network().setTimeVariantNetwork(true);

//		String inPlans = inputDir+"samplePlansForTesting.xml";
		String inPlans = inputDir+"/baseCaseOutput_plans.xml.gz";
		config.plans().setInputFile( inPlans );
		config.plans().setInputPersonAttributeFile(inputDir+"output_personAttributes.xml.gz");
		config.vehicles().setVehiclesFile(null); // see below for vehicle type info

		//==
		// after calibration;  departure time is fixed for urban; check if time choice is not present
		Collection<StrategySettings> strategySettings = config.strategy().getStrategySettings();
		for(StrategySettings ss : strategySettings){ // departure time is fixed now.
			if ( ss.getStrategyName().equals(DefaultStrategy.TimeAllocationMutator.toString()) ) {
				throw new RuntimeException("Time mutation should not be used; fixed departure time must be used after cadyts calibration.");
			} else if (  ss.getStrategyName().equals(DefaultStrategy.ReRoute.toString())  ) {
				// increase chances of re-routing to 25%
				ss.setWeight(0.25);
			}
		}
		//==
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setWriteEventsInterval(10);
		config.controler().setDumpDataAtEnd(true);

		config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.abort);
		config.vspExperimental().setWritingOutputEvents(true);

		config.travelTimeCalculator().setSeparateModes(true);
		config.travelTimeCalculator().setAnalyzedModes(String.join(",", PatnaUtils.ALL_MAIN_MODES));

		Scenario scenario = ScenarioUtils.loadScenario(config);

		String vehiclesFile = inputDir+"output_vehicles.xml.gz";
		PatnaVehiclesGenerator.addVehiclesToScenarioFromVehicleFile(vehiclesFile, scenario);

		// no vehicle info should be present if using VehiclesSource.modeVEhicleTypesFromVehiclesData
		if ( scenario.getVehicles().getVehicles().size() != 0 ) throw new RuntimeException("Only vehicle types should be loaded if vehicle source "+
				QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData +" is assigned.");
		scenario.getConfig().qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);
		return scenario;
	}

	private static void addScoringFunction(final Controler controler){
		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			final CharyparNagelScoringParametersForPerson parameters = new SubpopulationCharyparNagelScoringParameters( controler.getScenario() );
			@Inject
			Network network;
			@Inject
			Population population;
			@Inject
			PlanCalcScoreConfigGroup planCalcScoreConfigGroup; // to modify the util parameters
			@Inject
			ScenarioConfigGroup scenarioConfig;
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				final CharyparNagelScoringParameters params = parameters.getScoringParameters( person );

				SumScoringFunction sumScoringFunction = new SumScoringFunction();
				sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
				sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

				Double ratioOfInc = 1.0;

				if ( PatnaPersonFilter.isPersonBelongsToUrban(person.getId())) { // inc is not available for commuters and through traffic
					Double monthlyInc = (Double) population.getPersonAttributes().getAttribute(person.getId().toString(), PatnaUtils.INCOME_ATTRIBUTE);
					Double avgInc = PatnaUtils.MEADIAM_INCOME;
					ratioOfInc = avgInc/monthlyInc;
				}

				planCalcScoreConfigGroup.setMarginalUtilityOfMoney(ratioOfInc );				

				ScoringParameterSet scoringParameterSet = planCalcScoreConfigGroup.getScoringParameters( null ); // parameters set is same for all subPopulations 

				CharyparNagelScoringParameters.Builder builder = new CharyparNagelScoringParameters.Builder(
						planCalcScoreConfigGroup, scoringParameterSet, scenarioConfig);
				final CharyparNagelScoringParameters modifiedParams = builder.build();

				sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(modifiedParams, network));
				sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(modifiedParams));
				return sumScoringFunction;
			}
		});
	}

	private static class MyTerminationCriteria implements TerminationCriterion{

		private final BikeConnectorControlerListner bikeConnectorControlerListner;

		MyTerminationCriteria(final BikeConnectorControlerListner bikeConnectorControlerListner) {
			this.bikeConnectorControlerListner = bikeConnectorControlerListner;
		}

		@Override
		public boolean continueIterations(int iteration) {
			return !this.bikeConnectorControlerListner.isTerminating();
		}
	}
}