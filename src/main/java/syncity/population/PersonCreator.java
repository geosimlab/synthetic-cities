package syncity.population;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;

import syncity.Utils;

public class PersonCreator {

	public static final String WORK_ACTIVITY_TYPE = "work";
	public static final String HOME_ACTIVITY_TYPE = "home";
	

	protected final static int SECONDS_IN_HOUR = 3600;
	
	protected static float leaveHomeTime = 6;
	protected static float leaveHomeWindowSize = 3;
	protected static float workdayLength = 6;
	protected static float workdayWindowSize = 4;
	
	public static Person createPersonWithStandardPlan(Node homeNode, Node WorkNode, Population population) {
		Person person = createPerson(population);
		Plan plan = createPlanToPerson(homeNode, WorkNode, population);
		person.addPlan(plan);
		return person;
	}
	
	/**
	 * A method to prevent double use of ids
	 * 
	 * @return a person instance
	 */
	protected static Person createPerson(Population population) {
		PopulationFactory populationFactory = population.getFactory();
		return populationFactory.createPerson(Id.createPersonId(population.getPersons().size()));
	}	
	
	/**
	 * creates a how work home plane for a person living on homeNode work will be a
	 * random node in the network work start and end times are randomized based on
	 * the object attribute
	 * 
	 * @param nodes    array of all the nodes in the network
	 * @param homeNode the home node for that person
	 * @param person   the person to add the plan on
	 */
	protected static Plan createPlanToPerson(Node homeNode, Node WorkNode, Population population) {
		PopulationFactory populationFactory = population.getFactory();
		float leaveHome = Utils.randInWindow(leaveHomeTime, leaveHomeWindowSize);
		float leaveWork = leaveHome + Utils.randInWindow(workdayLength, workdayWindowSize);
		Plan plan = createHomeWorkHomePlan(populationFactory, homeNode, leaveHome, WorkNode, leaveWork);
		return plan;
	}

	/**
	 * creates a plan from home to work and back again uses link coming into the
	 * given nodes (notice comments in the class doc for more information
	 * 
	 * @param populationFactory matsim population factory
	 * @param homeNode          the home node
	 * @param leaveHomeTime     time to leave home (in hours 0-24)
	 * @param workNode          the work node
	 * @param leaveWorkTime     time to leave work (in hours 0-24)
	 * @return the created plan
	 */
	protected static Plan createHomeWorkHomePlan(PopulationFactory populationFactory, Node homeNode, float leaveHomeTime,
			Node workNode, float leaveWorkTime) {

		Plan plan = populationFactory.createPlan();
		Link homeLink = homeNode.getInLinks().values().iterator().next();
		Link workLink = workNode.getInLinks().values().iterator().next();

		Activity morningActivity = populationFactory.createActivityFromLinkId(HOME_ACTIVITY_TYPE, homeLink.getId());
		morningActivity.setEndTime(leaveHomeTime * SECONDS_IN_HOUR);
		plan.addActivity(morningActivity); // add the Activity to the Plan

		Leg leg = populationFactory.createLeg("av");
		leg.setDepartureTime(leaveHomeTime * SECONDS_IN_HOUR);
		plan.addLeg(leg);

		Activity WorkActivity = populationFactory.createActivityFromLinkId(WORK_ACTIVITY_TYPE, workLink.getId());
		WorkActivity.setStartTime(leaveHomeTime * SECONDS_IN_HOUR);
		WorkActivity.setEndTime(leaveWorkTime * SECONDS_IN_HOUR);
		plan.addActivity(WorkActivity);

		leg = populationFactory.createLeg("av");
		leg.setDepartureTime(leaveWorkTime * SECONDS_IN_HOUR);
		plan.addLeg(leg);

		Activity eveningActivity = populationFactory.createActivityFromLinkId(HOME_ACTIVITY_TYPE, homeLink.getId());
		plan.addActivity(eveningActivity);
		return plan;
	}
}
