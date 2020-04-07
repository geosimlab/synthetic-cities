package syncity;

import org.apache.commons.math3.distribution.PoissonDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.matsim.api.core.v01.Id;
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
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * A class to generate a random (poisson distributed) population in the given
 * network/config.
 * 
 * As I learned so far, amodeus has some restriction on the population format:
 *  1. Activity location must be specified by a link, and not with coordinates,
 *     otherwise the population cutter will judge it out of network. 
 *  2. Each leg must have a dep_time, and each activity (except for the last) must have
 *     end_time, otherwise the population cutter will judge it out of time window
 *     for simulation. 
 *  3. To use an av leg mode needs to be "av", some time it is changed automatically 
 *     to "av" and some times not.
 * 
 * 1 & 2 can be avoided by setting populationCutter=NONE in
 * amodeusOptions.properties
 * 
 * @author theFrok
 *
 */
public class RandomPopulationGenerator {

	private final static int SECONDS_IN_HOUR = 3600;

	private final static int CONSTANT_SEED = 504000;
	private final static int POISSON_MAX_ITERATIONS = 504000;
	
	// Uniform random generator 
	private final JDKRandomGenerator uniformSampler;
	
	// Poisson random generator
	private final PoissonDistribution poissonSampler;

	// counter to be used as person id, increment every time
	private int personId = 1;
	private float avgPeopleInNode;
	private float leaveHomeTime = 6;
	private float leaveHomeWindowSize = 3;
	private float workdayLength = 6;
	private float workdayWindowSize = 4;
	private Network network;
	private Population population;
	
	public RandomPopulationGenerator(Config config, Network network, long popSize) {
		if (network == null) {
			network = ScenarioUtils.loadScenario(config).getNetwork();
		} else if (config == null) {
			config = ConfigUtils.createConfig();
		}
		this.avgPeopleInNode = popSize / (float) network.getNodes().size();
		this.network = network;
		this.population = PopulationUtils.createPopulation(config, network);
		this.uniformSampler  = new JDKRandomGenerator(CONSTANT_SEED);
		this.poissonSampler = new PoissonDistribution(uniformSampler, this.avgPeopleInNode, 0.25, POISSON_MAX_ITERATIONS);
	}
	
	public RandomPopulationGenerator(Config config, long popSize) {
		this(config, null, popSize);
	}
	
	public RandomPopulationGenerator(Network network, long popSize) {
		this(null, network, popSize);
	}

	public static void main(String[] args) {
		
		Config config = ConfigUtils.loadConfig(args[0]);
		RandomPopulationGenerator popGen = new RandomPopulationGenerator(config, 2500);

		popGen.populateNodes();

		// Write the population to a file.
		MatsimWriter popWriter = new PopulationWriter(popGen.getPopulation());
		popWriter.write("./output/population.xml");
	}

	public Population getPopulation() {
		return population;
	}

	public Population populateNodes() {
		Node[] nodes = new Node[this.network.getNodes().size()]; 
		this.network.getNodes().values().toArray(nodes);
		System.out.println("The number of nodes is: " + nodes.length);
		for (Node node : nodes) {
			int popInNode = this.poissonSampler.sample();
			for (int j = 0; j < popInNode; j++) {
				Person person = this.createPerson();
				createPlanToPerson(nodes, node, person);
				this.population.addPerson(person);
			}
		}
		return population;
	}

	public void createPlanToPerson(Node[] nodes, Node homeNode, Person person) {
		PopulationFactory populationFactory = population.getFactory();
		int workNodeId = uniformSampler.nextInt(nodes.length);
		float leaveHome = this.shoot(this.leaveHomeTime, this.leaveHomeWindowSize);
		float leaveWork = leaveHome + this.shoot(this.workdayLength, this.workdayWindowSize);
		Plan plan = createHomeWorkHomePlan(populationFactory, homeNode, leaveHome, nodes[workNodeId], leaveWork);
		person.addPlan(plan);
	}
	
	private float shoot(float base, float window) {
		return base + (window * uniformSampler.nextFloat());
	}

	/** 
	 * A method to prevent double use of ids
	 * 
	 * @return a person instance
	 */
	public Person createPerson() {
		PopulationFactory populationFactory = population.getFactory();
		return populationFactory.createPerson(Id.createPersonId(this.personId++));
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
