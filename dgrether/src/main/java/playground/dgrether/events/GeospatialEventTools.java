/* *********************************************************************** *
 * project: org.matsim.*
 * GeospatialEventTools
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
package playground.dgrether.events;

import java.util.ArrayList;
import java.util.List;

import org.geotools.feature.Feature;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.AgentEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.LinkEvent;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.spatialschema.geometry.MismatchedDimensionException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;


/**
 * @author dgrether
 *
 */
public class GeospatialEventTools {
	
	private Network network;
	private CoordinateReferenceSystem networkCrs;
	private List<Geometry> transformedFeatureGeometries;
	
	public GeospatialEventTools(Network net, CoordinateReferenceSystem netCrs){
		this.network = net;
		this.networkCrs = netCrs;
		this.transformedFeatureGeometries = new ArrayList<Geometry>();

	}
	
	public boolean doFeaturesContainCoordinate(Coordinate coordinate) {
		Geometry linkPoint = MGC.coordinate2Point(coordinate);
		for (Geometry featureGeo : this.transformedFeatureGeometries){
				return featureGeo.contains(linkPoint);
		}
		return false;
	}
	
	public void addCrsFeatureTuple(Tuple<CoordinateReferenceSystem, Feature> featureTuple) {
		if ( !(this.networkCrs == null)){
			MathTransform transformation;
			try {
				transformation = CRS.findMathTransform(featureTuple.getFirst(), this.networkCrs, true);
				Geometry transformedFeatureGeometry = JTS.transform(featureTuple.getSecond().getDefaultGeometry(), transformation);
				this.transformedFeatureGeometries.add(transformedFeatureGeometry);
			} catch (FactoryException e) {
				e.printStackTrace();
			} catch (MismatchedDimensionException e) {
				e.printStackTrace();
			} catch (TransformException e) {
				e.printStackTrace();
			}
		}
		else {
			this.transformedFeatureGeometries.add(featureTuple.getSecond().getDefaultGeometry());
		}
	}

	public boolean doFeaturesContainEvent(Event event) {
		if (event instanceof AgentEvent) {
			AgentEvent e = (AgentEvent) event;
			Link link = this.network.getLinks().get(e.getLinkId());
			Coordinate coordinate = MGC.coord2Coordinate(link.getCoord());
			return this.doFeaturesContainCoordinate(coordinate);
		}
		else if (event instanceof LinkEvent){
			LinkEvent e = (LinkEvent) event;
			Link link = this.network.getLinks().get(e.getLinkId());
			Coordinate coordinate = MGC.coord2Coordinate(link.getCoord());
			return this.doFeaturesContainCoordinate(coordinate);
		}
		return false;
	}
	
}
