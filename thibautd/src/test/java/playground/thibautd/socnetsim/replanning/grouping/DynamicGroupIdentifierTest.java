/* *********************************************************************** *
 * project: org.matsim.*
 * DynamicGroupIdentifierTest.java
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
package playground.thibautd.socnetsim.replanning.grouping;

import java.util.Collection;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import org.junit.Assert;
import org.junit.Test;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import playground.thibautd.socnetsim.population.JointPlans;
import playground.thibautd.socnetsim.population.SocialNetwork;

/**
 * @author thibautd
 */
public class DynamicGroupIdentifierTest {

	@Test
	public void testNGroupsNoJointPlansNoSocialNet() {
		final Scenario scenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		scenario.addScenarioElement( JointPlans.ELEMENT_NAME , new JointPlans() );
		scenario.addScenarioElement( SocialNetwork.ELEMENT_NAME , new SocialNetwork() );

		final int nPersons = 100;

		for ( int i=0; i < nPersons; i++ ) {
			final Person p = scenario.getPopulation().getFactory().createPerson( new IdImpl( "person-"+i ) );
			scenario.getPopulation().addPerson( p );
		}

		test( new Fixture( scenario , nPersons ) );
	}

	@Test
	public void testNGroupsNoJointPlansCompleteSocialNet() {
		Logger.getLogger( DynamicGroupIdentifier.class ).setLevel( Level.TRACE );
		final Scenario scenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		scenario.addScenarioElement( JointPlans.ELEMENT_NAME , new JointPlans() );

		final SocialNetwork socnet = new SocialNetwork();
		scenario.addScenarioElement( SocialNetwork.ELEMENT_NAME , socnet );

		final int nPersons = 100;

		for ( int i=0; i < nPersons; i++ ) {
			final Person p = scenario.getPopulation().getFactory().createPerson( new IdImpl( "person-"+i ) );
			scenario.getPopulation().addPerson( p );
		}

		for ( Id ego : scenario.getPopulation().getPersons().keySet() ) {
			for ( Id alter : scenario.getPopulation().getPersons().keySet() ) {
				if ( ego == alter ) continue;
				// no need to add bidirectional, as other direction was or will be considered
				socnet.addMonodirectionalTie( ego , alter );
			}
		}

		test( new Fixture( scenario , 50 ) );
	}

	private void test( final Fixture fixture ) {
		final GroupIdentifier testee = new DynamicGroupIdentifier( fixture.scenario );

		final Collection<ReplanningGroup> groups = testee.identifyGroups( fixture.scenario.getPopulation() );
		Assert.assertEquals(
				"unexpected number of groups",
				fixture.expectedNGroups,
				groups.size() );
	}

	private static class Fixture {
		public final Scenario scenario;
		public final int expectedNGroups;

		public Fixture(
				final Scenario scenario,
				final int expectedNGroups) {
			this.scenario = scenario;
			this.expectedNGroups = expectedNGroups;
		}
	}
}

