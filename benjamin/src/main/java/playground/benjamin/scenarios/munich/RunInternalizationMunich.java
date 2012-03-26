/* *********************************************************************** *
 * project: org.matsim.*
 * RunMunich1pct.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.benjamin.scenarios.munich;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.controler.Controler;

import playground.benjamin.emissions.EmissionModule;
import playground.benjamin.internalization.EmissionCostModule;
import playground.benjamin.internalization.EmissionTravelDisutilityCalculatorFactory;
import playground.benjamin.internalization.InternalizeEmissionsControlerListener;

/**
 * @author benjamin
 *
 */
public class RunInternalizationMunich {
	
//	private static String configFile = "../../detailedEval/internalization/munich1pct/input/config_munich_1pct.xml";
	private static double emissionCostFactor;
	
	public static void main(String[] args) {
		
		Config config = new Config();
		config.addCoreModules();
		MatsimConfigReader confReader = new MatsimConfigReader(config);
//		confReader.readFile(configFile);
//		emissionCostFactor = 1.0;
		confReader.readFile(args[0]);
		emissionCostFactor = Double.parseDouble(args[1]);
		
		Controler controler = new Controler(config);
		Scenario scenario = controler.getScenario();

		EmissionModule emissionModule = new EmissionModule(scenario);
		emissionModule.createLookupTables();
		emissionModule.createEmissionHandler();
		
		EmissionCostModule emissionCostModule = new EmissionCostModule(emissionCostFactor);
		
		EmissionTravelDisutilityCalculatorFactory emissionTducf = new EmissionTravelDisutilityCalculatorFactory(emissionModule, emissionCostModule);
		controler.setTravelDisutilityFactory(emissionTducf);
		
		controler.addControlerListener(new InternalizeEmissionsControlerListener(emissionModule, emissionCostModule));
		
		controler.setOverwriteFiles(true);
		controler.run();
	}
}