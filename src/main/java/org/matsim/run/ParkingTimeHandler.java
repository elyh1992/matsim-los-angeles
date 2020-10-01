/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.run;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ScoringParameterSet;
import org.matsim.core.router.StageActivityTypeIdentifier;

import com.google.inject.Inject;

/**
* @author ikaddoura
*/

final class ParkingTimeHandler implements ActivityEndEventHandler, PersonDepartureEventHandler, PersonLeavesVehicleEventHandler {
	
	private final Map<Id<Person>, Double> personId2lastLeaveVehicleTime = new HashMap<>();
	private final Map<Id<Person>, String> personId2previousActivity = new HashMap<>();
	
	private final String modes = "car,ride";
	private final String prefixesActivitiesToIgnore = "home,work";
	
	final double parkingSearchSeconds = 12. * 60;
	final double additionalWalkingSeconds = 3. * 60;
	final double additionalTimeLossSeconds = parkingSearchSeconds + 2 * additionalWalkingSeconds;
	
	// ###########
	
	Set<String> modesSet = new HashSet<>(Arrays.asList(modes.replaceAll("\\s", "").split(",")));
	Set<String> ignoreActivitiesSet = new HashSet<>(Arrays.asList(prefixesActivitiesToIgnore.replaceAll("\\s", "").split(",")));

	@Inject
	private EventsManager events;
	
	@Inject
	private Scenario scenario;
	
	@Override
    public void reset(int iteration) {
       this.personId2lastLeaveVehicleTime.clear();
       this.personId2previousActivity.clear();
    }
	
	@Override
	public void handleEvent(ActivityEndEvent event) {		
		if (scenario.getPopulation().getPersons().get(event.getPersonId()) == null) {
			// skip pt and taxi drivers
		} else {
			if (!(StageActivityTypeIdentifier.isStageActivity(event.getActType()))) {	
				personId2previousActivity.put(event.getPersonId(), event.getActType());
			}
		}	
	}
	
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (scenario.getPopulation().getPersons().get(event.getPersonId()) == null) {
			// skip pt and taxi drivers
		} else {
			// There might be several departures during a single trip.
			if (modesSet.contains(event.getLegMode())) {
			
				boolean previousActivityIgnored = false;
				for (String activityToBeIgnored : ignoreActivitiesSet) {
					if (personId2previousActivity.get(event.getPersonId()).startsWith(activityToBeIgnored)) {
						previousActivityIgnored = true;
					}
				}
				
				if (!previousActivityIgnored) {
					
					Person person = scenario.getPopulation().getPersons().get(event.getPersonId());
					String subpopulation = (String) person.getAttributes().getAttribute("subpopulation");

					ScoringParameterSet scoringParams = scenario.getConfig().planCalcScore().getScoringParameters(subpopulation);
					
					final double disutilityPerHour = scoringParams.getPerforming_utils_hr() + (-1. * scoringParams.getModes().get(event.getLegMode()).getMarginalUtilityOfTraveling());	
					final double additionalTimeLossDisutility = additionalTimeLossSeconds / 3600. * disutilityPerHour;
					
					double marginalUtilityOfMoney;
					if (person.getAttributes().getAttribute("marginalUtilityOfMoney") == null) {
						throw new RuntimeException("Person does not have a marginal utility of money. Aborting...");
//						marginalUtilityOfMoney = scoringParams.getMarginalUtilityOfMoney();
					} else {
						marginalUtilityOfMoney = (double) person.getAttributes().getAttribute("marginalUtilityOfMoney");
					}
					
					final double costs = additionalTimeLossDisutility / marginalUtilityOfMoney;

					double amount = -1. * costs;
					
					if (amount > 0.) {
						throw new RuntimeException("This should be a negative monetary amount. Aborting...");
					}
					
					String purpose = "additionalParkingSearchAndWalkingTime_mode-" + event.getLegMode() + "_act-" + personId2previousActivity.get(event.getPersonId());
					events.processEvent(new PersonMoneyEvent(event.getTime(), event.getPersonId(), amount, purpose, "fictiveTransactionPartner"));		
				}	
			}
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if (scenario.getPopulation().getPersons().get(event.getPersonId()) == null) {
			// skip pt and taxi drivers
		} else {
			personId2lastLeaveVehicleTime.put(event.getPersonId(), event.getTime());
		}
	}

	
}

