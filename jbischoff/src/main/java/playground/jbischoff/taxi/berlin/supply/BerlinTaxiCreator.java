/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.jbischoff.taxi.berlin.supply;

import java.util.*;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.*;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import pl.poznan.put.util.random.WeightedRandomSelection;
import playground.jbischoff.taxi.berlin.demand.TaxiDemandWriter;
import playground.michalm.supply.VehicleCreator;
import playground.michalm.zone.Zone;

import com.vividsolutions.jts.geom.Point;


public class BerlinTaxiCreator
    implements VehicleCreator
{
    private static final Logger log = Logger.getLogger(BerlinTaxiCreator.class);
    private static final Random RND = new Random(42);
    private static final double PAXPERCAR = 4;

    private final CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(
            "EPSG:25833", TransformationFactory.DHDN_GK4);

    private final Scenario scenario;
    private final Map<Id, Zone> zones;
    private final NetworkImpl network;
    private final WeightedRandomSelection<Id> lorSelection;
    private final double evShare;

    private int currentVehicleId = 0;


    public BerlinTaxiCreator(Scenario scenario, Map<Id, Zone> zones,
            WeightedRandomSelection<Id> lorSelection, double evShare)
    {
        this.scenario = scenario;
        this.zones = zones;
        this.lorSelection = lorSelection;
        this.evShare = evShare;

        network = (NetworkImpl)scenario.getNetwork();
    }


    @Override
    public Vehicle createVehicle(double t0, double t1)
    {
        Id lorId = lorSelection.select();
        String vehIdString = "t_" + lorId + "_" + (t0 / 3600) + "_" + currentVehicleId;
        if (RND.nextDouble() < evShare) {
            vehIdString = "e" + vehIdString;
        }
        Id vehId = scenario.createId(vehIdString);

        Link link = getRandomLinkInLor(lorId);
        Vehicle v = new VehicleImpl(vehId, link, PAXPERCAR, t0, t1);
        return v;
    }


    private Link getRandomLinkInLor(Id lorId)
    {
        log.info(lorId);
        Id id = lorId;
        if (lorId.toString().length() == 7)
            id = new IdImpl("0" + lorId.toString());
        log.info(id);
        Point p = TaxiDemandWriter.getRandomPointInFeature(RND, this.zones.get(id)
                .getMultiPolygon());
        Coord coord = ct.transform(new CoordImpl(p.getX(), p.getY()));
        Link link = network.getNearestLinkExactly(coord);

        return link;
    }
}