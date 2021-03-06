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

package playground.agarwalamit.opdyts.patna.allModes;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import com.google.common.io.Files;
import floetteroed.opdyts.DecisionVariableRandomizer;
import floetteroed.opdyts.ObjectiveFunction;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.analysis.kai.KaiAnalysisListener;
import org.matsim.contrib.opdyts.MATSimSimulator2;
import org.matsim.contrib.opdyts.MATSimStateFactoryImpl;
import org.matsim.contrib.opdyts.useCases.modeChoice.EveryIterationScoringParameters;
import org.matsim.contrib.opdyts.utils.MATSimOpdytsControler;
import org.matsim.contrib.opdyts.utils.OpdytsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.analysis.modalShare.ModalShareControlerListener;
import playground.agarwalamit.analysis.modalShare.ModalShareEventHandler;
import playground.agarwalamit.analysis.tripTime.ModalTravelTimeControlerListener;
import playground.agarwalamit.analysis.tripTime.ModalTripTravelTimeHandler;
import playground.agarwalamit.clustering.ClusterAlgorithm;
import playground.agarwalamit.clustering.ClusterUtils;
import playground.agarwalamit.mixedTraffic.patnaIndia.scoring.PtFareEventHandler;
import playground.agarwalamit.opdyts.*;
import playground.agarwalamit.opdyts.analysis.OpdytsModalStatsControlerListener;
import playground.agarwalamit.opdyts.patna.PatnaOneBinDistanceDistribution;
import playground.agarwalamit.opdyts.plots.BestSolutionVsDecisionVariableChart;
import playground.agarwalamit.opdyts.plots.OpdytsConvergenceChart;
import playground.agarwalamit.opdyts.teleportationModes.TeleportationODCoordAnalyzer;
import playground.agarwalamit.opdyts.teleportationModes.Zone;
import playground.agarwalamit.utils.FileUtils;

/**
 * @author amit
 */

public class PatnaUrbanOpdytsCalibrator {

	public enum PatnaTeleportationModesZonesType {
		wardFile,
		simpleGrid,
		clusterAlgoKmeans,
		clusterAlgoEqualPoints
	}

	private static final OpdytsScenario PATNA_1_PCT = OpdytsScenario.PATNA_1Pct;

	public static void main(String[] args) {
		String configFile;
		String OUT_DIR = null;
		String relaxedPlans ;
		ModeChoiceRandomizer.ASCRandomizerStyle ascRandomizeStyle;
		PatnaTeleportationModesZonesType patnaTeleportationModesZonesType ;

		if ( args.length>0 ) {
			configFile = args[0];
			OUT_DIR = args[1];
			relaxedPlans = args[2];
			ascRandomizeStyle = ModeChoiceRandomizer.ASCRandomizerStyle.valueOf(args[3]);
			patnaTeleportationModesZonesType = PatnaTeleportationModesZonesType.valueOf(args[4]);
		} else {
			configFile = FileUtils.RUNS_SVN+"/opdyts/patna/input_allModes/"+"/config_allModes.xml";
			OUT_DIR = FileUtils.RUNS_SVN+"/opdyts/patna/output_allModes/calib_trails/";
			relaxedPlans = FileUtils.RUNS_SVN+"/opdyts/patna/output_allModes/initialPlans2RelaxedPlans/output_plans.xml.gz";
			ascRandomizeStyle = ModeChoiceRandomizer.ASCRandomizerStyle.axial_randomVariation;
			patnaTeleportationModesZonesType = PatnaTeleportationModesZonesType.clusterAlgoKmeans;
		}

		OUT_DIR += ascRandomizeStyle+"_"+patnaTeleportationModesZonesType+"/";

		Config config = ConfigUtils.loadConfig(configFile, new OpdytsConfigGroup());
		config.plans().setInputFile(relaxedPlans);
		config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn); // must be warn, since opdyts override few things
		config.controler().setOutputDirectory(OUT_DIR);

		OpdytsConfigGroup opdytsConfigGroup = ConfigUtils.addOrGetModule(config, OpdytsConfigGroup.GROUP_NAME, OpdytsConfigGroup.class ) ;
		opdytsConfigGroup.setOutputDirectory(OUT_DIR);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		// adding pt fare system based on distance
		// for above make sure that util_dist and monetary dist rate for pt are zero.
		PlanCalcScoreConfigGroup.ModeParams mp = scenario.getConfig().planCalcScore().getModes().get("pt");
		mp.setMarginalUtilityOfDistance(0.0);
		mp.setMonetaryDistanceRate(0.0);

		// == opdyts settings
		MATSimOpdytsControler<ModeChoiceDecisionVariable> opdytsControler = new MATSimOpdytsControler<ModeChoiceDecisionVariable>(scenario);
		Set<String> networkModes = new HashSet<>(scenario.getConfig().qsim().getMainModes());
		Set<String> teleportationModes = new HashSet<>(scenario.getConfig().plansCalcRoute().getModeRoutingParams().keySet());

		Set<String> allModes = new LinkedHashSet<>(networkModes);
		allModes.addAll(teleportationModes);

		DistanceDistribution referenceStudyDistri = new PatnaOneBinDistanceDistribution(PATNA_1_PCT);
		OpdytsModalStatsControlerListener stasControlerListner = new OpdytsModalStatsControlerListener(allModes, referenceStudyDistri);

		// following is the  entry point to start a matsim controler together with opdyts
		MATSimSimulator2<ModeChoiceDecisionVariable> simulator = new MATSimSimulator2<>(new MATSimStateFactoryImpl<>(), scenario);

		// getting zone info
		String path = new File(configFile).getParentFile().getAbsolutePath();
		PatnaZoneIdentifier patnaZoneIdentifier = null ;
		switch (patnaTeleportationModesZonesType){
			case wardFile:
				patnaZoneIdentifier = new PatnaZoneIdentifier(scenario.getPopulation(), path+"/Wards.shp"); // use plans instead to get the
				break;
			case simpleGrid:
				patnaZoneIdentifier = new PatnaZoneIdentifier(scenario.getPopulation(), scenario.getNetwork(), 250);
				break;
			case clusterAlgoKmeans:
				patnaZoneIdentifier = new PatnaZoneIdentifier(scenario.getPopulation(), ClusterUtils.getBoundingBox(scenario.getNetwork()), 2000,
						ClusterAlgorithm.ClusterType.K_MEANS);
				break;
			case clusterAlgoEqualPoints:
				patnaZoneIdentifier = new PatnaZoneIdentifier(scenario.getPopulation(), ClusterUtils.getBoundingBox(scenario.getNetwork()), 2000, // 18 origins in each cluster.
						ClusterAlgorithm.ClusterType.EQUAL_POINTS);
				break;
				default:throw new RuntimeException("not implemented yet.");
		}
		Set<Zone> relevantZones = patnaZoneIdentifier.getZones();
		simulator.addSimulationStateAnalyzer(new TeleportationODCoordAnalyzer.Provider(opdytsControler.getTimeDiscretization(), teleportationModes, relevantZones, scenario));

		String finalOUT_DIR = OUT_DIR;
		simulator.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				addControlerListenerBinding().to(KaiAnalysisListener.class);
				addControlerListenerBinding().toInstance(stasControlerListner);

				this.bind(ModalShareEventHandler.class);
				this.addControlerListenerBinding().to(ModalShareControlerListener.class);

				this.bind(ModalTripTravelTimeHandler.class);
				this.addControlerListenerBinding().to(ModalTravelTimeControlerListener.class);

				bind(ScoringParametersForPerson.class).to(EveryIterationScoringParameters.class);

				// adding pt fare system based on distance
				this.addEventHandlerBinding().to(PtFareEventHandler.class);

				addControlerListenerBinding().toInstance(new ShutdownListener() {
					@Override
					public void notifyShutdown(ShutdownEvent event) {
						// copy the state vector elements files before removing ITERS dir
						String outDir = event.getServices().getControlerIO().getOutputPath()+"/vectorElementSizeFiles/";
						new File(outDir).mkdirs();

						int firstIt = event.getServices().getConfig().controler().getFirstIteration();
						int lastIt = event.getServices().getConfig().controler().getLastIteration();
						int plotEveryItr = 50;

						for (int itr = firstIt+1; itr <=lastIt; itr++) {
							if ( (itr == firstIt+1 || itr%plotEveryItr ==0) && new File(event.getServices().getControlerIO().getIterationPath(itr)).exists() ) {
								{
									String sourceFile = event.getServices().getControlerIO().getIterationFilename(itr,"stateVector_networkModes.txt");
									String sinkFile =  outDir+"/"+itr+".stateVector_networkModes.txt";
									try {
										Files.copy(new File(sourceFile), new File(sinkFile));
									} catch (IOException e) {
										throw new RuntimeException("Data is not copied. Reason : " + e);
									}
								}
								{
									String sourceFile = event.getServices().getControlerIO().getIterationFilename(itr,"stateVector_teleportationModes.txt");
									String sinkFile =  outDir+"/"+itr+".stateVector_teleportationModes.txt";
									try {
										Files.copy(new File(sourceFile), new File(sinkFile));
									} catch (IOException e) {
										throw new RuntimeException("Data is not copied. Reason : " + e);
									}
								}
							}
						}

						// remove the unused iterations
						String dir2remove = event.getServices().getControlerIO().getOutputPath()+"/ITERS/";
						IOUtils.deleteDirectoryRecursively(new File(dir2remove).toPath());

						// post-process
						String opdytsConvergencefile = finalOUT_DIR +"/opdyts.con";
						if (new File(opdytsConvergencefile).exists()) {
							OpdytsConvergenceChart opdytsConvergencePlotter = new OpdytsConvergenceChart();
							opdytsConvergencePlotter.readFile(finalOUT_DIR +"/opdyts.con");
							opdytsConvergencePlotter.plotData(finalOUT_DIR +"/convergence.png");
						}

						BestSolutionVsDecisionVariableChart bestSolutionVsDecisionVariableChart = new BestSolutionVsDecisionVariableChart(new ArrayList<>(allModes));
						bestSolutionVsDecisionVariableChart.readFile(finalOUT_DIR +"/opdyts.log");
						bestSolutionVsDecisionVariableChart.plotData(finalOUT_DIR +"/decisionVariableVsASC.png");
					}
				});
			}
		});

		// this is the objective Function which returns the value for given SimulatorState
		// in my case, this will be the distance based modal split
		ObjectiveFunction objectiveFunction = new ModeChoiceObjectiveFunction(referenceStudyDistri); // in this, the method argument (SimulatorStat) is not used.

		// randomize the decision variables (for e.g.\ utility parameters for modes)
		DecisionVariableRandomizer<ModeChoiceDecisionVariable> decisionVariableRandomizer = new ModeChoiceRandomizer(scenario,
				RandomizedUtilityParametersChoser.ONLY_ASC, PATNA_1_PCT, null, allModes, ascRandomizeStyle);

		// what would be the decision variables to optimize the objective function.
		ModeChoiceDecisionVariable initialDecisionVariable = new ModeChoiceDecisionVariable(scenario.getConfig().planCalcScore(),scenario, allModes, PATNA_1_PCT);

		opdytsControler.addNetworkModeOccupancyAnalyzr(simulator);
		opdytsControler.run(simulator, decisionVariableRandomizer, initialDecisionVariable, objectiveFunction);
	}
}
