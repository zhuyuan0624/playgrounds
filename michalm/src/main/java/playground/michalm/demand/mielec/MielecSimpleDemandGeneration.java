/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.michalm.demand.mielec;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.naming.ConfigurationException;
import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.dvrp.run.VrpConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.matrices.Matrix;
import org.xml.sax.SAXException;

import pl.poznan.put.util.array2d.Array2DReader;
import pl.poznan.put.util.array2d.Array2DUtils;
import playground.michalm.demand.ActivityCreator;
import playground.michalm.demand.DefaultActivityCreator;
import playground.michalm.demand.ODDemandGenerator;
import playground.michalm.demand.taxi.PersonCreatorWithRandomTaxiMode;
import playground.michalm.util.matrices.MatrixUtils;
import playground.michalm.zone.Zone;
import playground.michalm.zone.Zones;


public class MielecSimpleDemandGeneration
{
    public static void main(String[] args)
        throws ConfigurationException, IOException, SAXException, ParserConfigurationException
    {
        String dirName = "D:\\michalm\\2013_07\\mielec-2-peaks-new\\";
        String networkFile = dirName + "network.xml";
        String zonesXmlFile = dirName + "zones.xml";
        String zonesShpFile = dirName + "GIS\\zones.SHP";
        String odMatrixFile = dirName + "odMatrix.dat";
        String plansFile = dirName + "plans.xml";

        String taxiFile = dirName + "taxiCustomers_03_pc.txt";

        // double hours = 2;
        // double flowCoeff = 1;
        // double taxiProbability = 0;

        double duration = 3600;
        double[] flowCoeffs = { 0.2, 0.4, 0.6, 0.8, 0.6, 0.4, 0.2 };
        double taxiProbability = 0.03;

        Scenario scenario = ScenarioUtils.createScenario(VrpConfigUtils.createConfig());
        new MatsimNetworkReader(scenario).readFile(networkFile);
        Map<Id<Zone>, Zone> zones = Zones.readZones(scenario, zonesXmlFile, zonesShpFile);

        ActivityCreator ac = new DefaultActivityCreator(scenario);
        PersonCreatorWithRandomTaxiMode pc = new PersonCreatorWithRandomTaxiMode(scenario,
                taxiProbability);
        ODDemandGenerator dg = new ODDemandGenerator(scenario, zones, true, ac, pc);

        double[][] matrix = Array2DReader.getDoubleArray(new File(odMatrixFile), zones.size());
        Matrix afternoonODMatrix = MatrixUtils.createSparseMatrix("afternoon", zones.keySet(),
                matrix);
        double[][] transposedMatrix = Array2DUtils.transponse(matrix);
        Matrix morningODMatrix = MatrixUtils.createSparseMatrix("morning", zones.keySet(),
                transposedMatrix);

        double startTime = 6 * 3600;
        dg.generateMultiplePeriods(morningODMatrix, "dummy", "dummy", TransportMode.car, startTime,
                duration, flowCoeffs);

        startTime += 3600 * flowCoeffs.length;
        dg.generateMultiplePeriods(afternoonODMatrix, "dummy", "dummy", TransportMode.car,
                startTime, duration, flowCoeffs);

        dg.write(plansFile);
        pc.writeTaxiCustomers(taxiFile);
    }
}
