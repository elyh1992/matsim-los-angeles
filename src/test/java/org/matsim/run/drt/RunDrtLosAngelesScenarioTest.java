/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.run.drt;

import java.util.Arrays;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.analysis.ScoreStatsControlerListener.ScoreItem;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.run.RunLosAngelesScenario;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author ikaddoura
 *
 */
public class RunDrtLosAngelesScenarioTest {
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;
	
	// tests the choice set extension
	@Test
	public final void test0() {
		try {			
			String[] args = new String[] { "test-agent-from-base-case.xml" , "./test/input/drt/los-angeles-wsc-reduced-drt-v1.1-1pct.config.xml"};
			
			String[] argsWithoutCustomAttributes;
			String populationFile;
			if (args.length > 0) {
				argsWithoutCustomAttributes = Arrays.copyOfRange( args, 1, args.length );
				populationFile = args[0];
			} else {
				argsWithoutCustomAttributes = args;
				populationFile = null;
			}
			
			Config config = RunDrtLosAngelesScenario.prepareConfig(argsWithoutCustomAttributes);
			config.controler().setOutputDirectory( utils.getOutputDirectory() );
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			config.plans().setInputFile("test-drt-agent.xml");
			config.transit().setUseTransit(false); // disable simulated pt
			config.network().setInputFile(null);
			Scenario scenario = RunDrtLosAngelesScenario.prepareScenario(config, populationFile);
			
			Assert.assertEquals("Wrong number of plans.", 2, scenario.getPopulation().getPersons().get(Id.createPersonId("test-agent_drt-user_monomodal")).getPlans().size());

		} catch ( Exception ee ) {
			ee.printStackTrace();
			Logger.getLogger(this.getClass()).fatal("there was an exception: \n" + ee ) ;
			Assert.fail();
		}

	}
	
	// tests the score of a specific agent (drt user)
	// simulated pt disabled
	@Test
	public final void test1() {
		try {			
			String[] args = new String[] { "./test/input/drt/los-angeles-wsc-reduced-drt-v1.1-1pct.config.xml" };
			
			Config config = RunDrtLosAngelesScenario.prepareConfig(args);
			config.controler().setLastIteration(0);
			config.global().setNumberOfThreads(1); // only one thread available on travis
			config.controler().setOutputDirectory( utils.getOutputDirectory() );
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			config.plans().setInputFile("test-drt-agent.xml");
			config.transit().setUseTransit(false); // disable simulated pt
			for (DrtConfigGroup drtCfg : ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class).getModalElements()) {
				drtCfg.setNumberOfThreads(1); // only one thread available on travis
			}
			
			Scenario scenario = RunDrtLosAngelesScenario.prepareScenario(config);
			Controler controler = RunDrtLosAngelesScenario.prepareControler(scenario);
			controler.run();
			
			RunLosAngelesScenario.runAnalysis(config);

//			Assert.assertEquals("Wrong score in iteration 0.", 149.3797713901901 + (0.16492491823344851 + 0.10527122440432883) * 4.673641850220264, controler.getScoreStats().getScoreHistory().get(ScoreItem.executed).get(0), MatsimTestUtils.EPSILON);
			Assert.assertEquals("Wrong score in iteration 0.", 149.3797713901901 + (0.15205843525069718 + 0.10527122440432883) * 4.673641850220264, controler.getScoreStats().getScoreHistory().get(ScoreItem.executed).get(0), MatsimTestUtils.EPSILON);

		} catch ( Exception ee ) {
			ee.printStackTrace();
			Logger.getLogger(this.getClass()).fatal("there was an exception: \n" + ee ) ;
			Assert.fail();
		}

	}
	
	// tests the score of a specific agent (drt user)
	// simulated pt enabled
	@Test
	public final void test2() {
		try {			
			String[] args = new String[] { "./test/input/drt/los-angeles-wsc-reduced-drt-v1.1-1pct.config.xml" };
			
			Config config = RunDrtLosAngelesScenario.prepareConfig(args);
			config.controler().setLastIteration(1);
			config.controler().setWriteEventsInterval(1); // don't write events files
			config.global().setNumberOfThreads(1); // only one thread available on travis
			config.controler().setOutputDirectory( utils.getOutputDirectory() );
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			config.plans().setInputFile("test-pt-agent.xml");
			for (DrtConfigGroup drtCfg : ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class).getModalElements()) {
				drtCfg.setNumberOfThreads(1); // only one thread available on travis
			}
			
			Scenario scenario = RunDrtLosAngelesScenario.prepareScenario(config);
			Controler controler = RunDrtLosAngelesScenario.prepareControler(scenario);
			controler.run();
			
			RunLosAngelesScenario.runAnalysis(config);
			
			Assert.assertEquals("Wrong score in iteration 0.", 74.39183890489247, controler.getScoreStats().getScoreHistory().get(ScoreItem.executed).get(0), MatsimTestUtils.EPSILON);

		} catch ( Exception ee ) {
			ee.printStackTrace();
			Logger.getLogger(this.getClass()).fatal("there was an exception: \n" + ee ) ;
			Assert.fail();
		}

	}

}
