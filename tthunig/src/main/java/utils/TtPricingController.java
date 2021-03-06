/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;

import analysis.signals.TtSignalAnalysisListener;
import analysis.signals.TtSignalAnalysisTool;
import analysis.signals.TtSignalAnalysisWriter;
import playground.vsp.congestion.controler.MarginalCongestionPricingContolerListener;
import playground.vsp.congestion.handlers.CongestionHandlerImplV10;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.CongestionHandlerImplV4;
import playground.vsp.congestion.handlers.CongestionHandlerImplV7;
import playground.vsp.congestion.handlers.CongestionHandlerImplV8;
import playground.vsp.congestion.handlers.CongestionHandlerImplV9;
import playground.vsp.congestion.handlers.TollHandler;
import playground.vsp.congestion.routing.CongestionTollTimeDistanceTravelDisutilityFactory;
import scenarios.illustrative.analysis.TtAbstractAnalysisTool;
import scenarios.illustrative.analysis.TtAnalyzedResultsWriter;
import scenarios.illustrative.analysis.TtListenerToBindAndWriteAnalysis;
import scenarios.illustrative.braess.analysis.TtAnalyzeBraess;

/**
 * @author tthunig
 *
 */
public class TtPricingController {

	enum PricingType {
		V3, V4, V7, V8, V9, V10
	}
	private static final PricingType PRICING_TYPE = PricingType.V9;

	// choose a sigma for the randomized router
	// (higher sigma cause more randomness. use 0.0 for no randomness.)
	private static final double SIGMA = 0.0;

	/**
	 * @param args the config file
	 */
	public static void main(String[] args) {
		Controler controler = TtBasicController.prepareBasicControler(args);
		Scenario scenario = controler.getScenario();
		Config config = controler.getConfig();

		// add tolling
		TollHandler tollHandler = new TollHandler(scenario);

		// add correct TravelDisutilityFactory for tolls if ReRoute is used
		StrategySettings[] strategies = config.strategy().getStrategySettings().toArray(new StrategySettings[0]);
		for (int i = 0; i < strategies.length; i++) {
			if (strategies[i].getStrategyName().equals(DefaultStrategy.ReRoute.toString())) {
				if (strategies[i].getWeight() > 0.0) { // ReRoute is used
					final CongestionTollTimeDistanceTravelDisutilityFactory factory = new CongestionTollTimeDistanceTravelDisutilityFactory(
							new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, config.planCalcScore()), tollHandler, config.planCalcScore());
					factory.setSigma(SIGMA);
					controler.addOverridingModule(new AbstractModule() {
						@Override
						public void install() {
							this.bindCarTravelDisutilityFactory().toInstance(factory);
						}
					});
				}
			}
		}

		// choose the correct congestion handler and add it
		EventHandler congestionHandler = null;
		switch (PRICING_TYPE) {
		case V3:
			congestionHandler = new CongestionHandlerImplV3(controler.getEvents(), scenario);
			break;
		case V4:
			congestionHandler = new CongestionHandlerImplV4(controler.getEvents(), scenario);
			break;
		case V7:
			congestionHandler = new CongestionHandlerImplV7(controler.getEvents(), scenario);
			break;
		case V8:
			congestionHandler = new CongestionHandlerImplV8(controler.getEvents(), scenario);
			break;
		case V9:
			congestionHandler = new CongestionHandlerImplV9(controler.getEvents(), scenario);
			break;
		case V10:
			congestionHandler = new CongestionHandlerImplV10(controler.getEvents(), scenario);
			break;
		default:
			break;
		}
		controler.addControlerListener(new MarginalCongestionPricingContolerListener(scenario, tollHandler, congestionHandler));
		
		controler.run();
	}

}
