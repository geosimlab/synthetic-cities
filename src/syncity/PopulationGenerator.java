package syncity;

import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * A class to generate a random (poisson) population in the given
 * network/config.
 * 
 * As I learned so far, amodeus has some restriction on the population format:
 * 1. Activity location must be specefied by a link, and not with coordinates,
 * otherwise the population cutter will judge it out of network. 2. Each leg
 * must have a dep_time, and each activity (except for the last) must have
 * end_time, otherwise the population cutter will judge it out of time window
 * for simulation. 3. To use an av leg mode nedds to be "av", some time it is
 * changed automatically to "av" and some times not.
 * 
 * 1 & 2 can be avoided by setting populationCutter=NONE in
 * amodeusOptions.properties
 * 
 * @author theFrok
 *
 */
public class PopulationGenerator {

	private final static int SECONDS_IN_HOUR = 3600;

	private final static int CONSTANT_SEED = 504000;

	// counter to be used as person id, increment every time
	private static int personId = 1;

	public static void main(String[] args) {
		
		// First, create a new Config and a new Scenario.
		Config config = ConfigUtils.loadConfig(args[0]);
		Scenario sc = ScenarioUtils.loadScenario(config);
		Population population = sc.getPopulation();

		Network network = sc.getNetwork();

		Node[] nodes = new Node[network.getNodes().size()];
		network.getNodes().values().toArray(nodes);

		populateNodes(population, nodes);

		// Write the population to a file.
		MatsimWriter popWriter = new PopulationWriter(population);
		popWriter.write("./output/population.xml");
	}

	public static Population populateNodes(Population population, Node[] nodes) {
		PopulationFactory populationFactory = population.getFactory();
		Random rand = new Random(CONSTANT_SEED);
		System.out.println("The number of nodes is: " + nodes.length);
		for (int i = 0; i < nodes.length; i++) {
			int workNodeId = rand.nextInt(nodes.length);
			Person person = createPerson(populationFactory);
			Plan plan = createHomeWorkHomePlan(populationFactory, nodes[i], 6, nodes[workNodeId], 16);
			person.addPlan(plan);
			population.addPerson(person);
		}
		return population;
	}

	/** A method to prevent double use of ids
	 * @param populationFactory
	 * @return a person instance
	 */
	public static Person createPerson(PopulationFactory populationFactory) {
		return populationFactory.createPerson(Id.createPersonId(personId++));
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
	private static Plan createHomeWorkHomePlan(PopulationFactory populationFactory, Node homeNode, float leaveHomeTime,
			Node workNode, float leaveWorkTime) {

		Plan plan = populationFactory.createPlan();
		Link homeLink = homeNode.getInLinks().values().iterator().next();
		Link workLink = workNode.getInLinks().values().iterator().next();

		Activity morningActivity = populationFactory.createActivityFromLinkId("home", homeLink.getId());
		morningActivity.setEndTime(leaveHomeTime * SECONDS_IN_HOUR);
		plan.addActivity(morningActivity); // add the Activity to the Plan

		Leg leg = populationFactory.createLeg("av");
		leg.setDepartureTime(leaveHomeTime * SECONDS_IN_HOUR);
		plan.addLeg(leg);

		Activity WorkActivity = populationFactory.createActivityFromLinkId("work", workLink.getId());
		WorkActivity.setStartTime(leaveHomeTime * SECONDS_IN_HOUR);
		WorkActivity.setEndTime(leaveWorkTime * SECONDS_IN_HOUR);
		plan.addActivity(WorkActivity);

		leg = populationFactory.createLeg("av");
		leg.setDepartureTime(leaveWorkTime * SECONDS_IN_HOUR);
		plan.addLeg(leg);

		Activity eveningActivity = populationFactory.createActivityFromLinkId("home", homeLink.getId());
		plan.addActivity(eveningActivity);
		return plan;
	}

}
