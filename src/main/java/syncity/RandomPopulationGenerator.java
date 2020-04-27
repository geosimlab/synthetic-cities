package syncity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
	
	private final JDKRandomGenerator uniformSampler;
	private final PoissonDistribution poissonSampler;

	// counter to be used as person id, increment every time
	private int personId = 1;
	private int popSize;
	private float leaveHomeTime = 6;
	private float leaveHomeWindowSize = 3;
	private float workdayLength = 6;
	private float workdayWindowSize = 4;
	private Network network;
	private Population population;
	
	public RandomPopulationGenerator(Config config, Network network, int popSize) {
		if (network == null) {
			network = ScenarioUtils.loadScenario(config).getNetwork();
		} else if (config == null) {
			config = ConfigUtils.createConfig();
		}
		this.popSize = popSize;
		float avgPeopleInNode = popSize / (float) network.getNodes().size();
		this.network = network;
		this.population = PopulationUtils.createPopulation(config, network);
		this.uniformSampler  = new JDKRandomGenerator(CONSTANT_SEED);
		this.poissonSampler = new PoissonDistribution(uniformSampler, avgPeopleInNode, 
				PoissonDistribution.DEFAULT_EPSILON, PoissonDistribution.DEFAULT_MAX_ITERATIONS);
	}
	
	public RandomPopulationGenerator(Config config, int popSize) {
		this(config, null, popSize);
	}
	
	public RandomPopulationGenerator(Network network, int popSize) {
		this(null, network, popSize);
	}

	public Population getPopulation() {
		return this.population;
	}
	
	/**
	 * Writes the population to a file in the given path, if path is folder the
	 * default name for the population file would be "Population-(popSize).xml"
	 * 
	 * @param outPath the path to write the population xml to
	 * @return the absolute path of the created file
	 * @throws IOException
	 */
	public String writePopulation(String outPath) throws IOException {
		Path out = Paths.get(outPath);
		Path outputFolder;
		if (Files.isDirectory(out)) {
			outputFolder = out;
			out = out.resolve(this.getTitle() + ".xml");
		} else
			outputFolder = out.getParent();
		// create output folder if necessary
		Files.createDirectories(outputFolder);

		// write network
		new PopulationWriter(population).write(out.toString());
		return out.toAbsolutePath().toString();
	}

	private String getTitle() {
		return String.format("Population-%d", this.popSize);
	}

	/**
	 * create population with a random spread over the network
	 */
	public void populateNodes() {
		Node[] nodesArray = new Node[this.network.getNodes().size()]; 
		this.network.getNodes().values().toArray(nodesArray);
		System.out.println("The number of nodes is: " + nodesArray.length);
		for (Node homeNode : nodesArray) {
//			Now we are just using constant number of people in each node
//			int popInNode = this.poissonSampler.sample();
			int popInNode = popSize / network.getNodes().size();
			for (int j = 0; j < popInNode; j++) {
				Person person = this.createPerson();
				createPlanToPerson(nodesArray, homeNode, person);
				this.population.addPerson(person);
			}
		}
	}
	
	/**
	 * A different name for the same functionality :*
	 */
	public void generatePopulation() {
		populateNodes();
	}

	/**
	 * creates a how work home plane for a person living on homeNode
	 * work will be a random node in the network
	 * work start and end times are randomized based on the object attribute
	 * 
	 * @param nodes    array of all the nodes in the network
	 * @param homeNode the home node for that person
	 * @param person   the person to add the plan on
	 */
	private void createPlanToPerson(Node[] nodes, Node homeNode, Person person) {
		PopulationFactory populationFactory = population.getFactory();
		int workNodeId = uniformSampler.nextInt(nodes.length);
		float leaveHome = this.randInWindow(this.leaveHomeTime, this.leaveHomeWindowSize);
		float leaveWork = leaveHome + this.randInWindow(this.workdayLength, this.workdayWindowSize);
		Plan plan = createHomeWorkHomePlan(populationFactory, homeNode, leaveHome, nodes[workNodeId], leaveWork);
		person.addPlan(plan);
	}
	
	/**
	 * return a random number between base and (base + window)
	 * 
	 * @param base   the base size
	 * @param window the size of the random window
	 * @return a random number within the window
	 */
	private float randInWindow(float base, float window) {
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

	public static void main(String[] args) {
		
		Config config = ConfigUtils.loadConfig(args[0]);
		RandomPopulationGenerator popGen = new RandomPopulationGenerator(config, 2500);
		
		popGen.populateNodes();
		
		// Write the population to a file.
		MatsimWriter popWriter = new PopulationWriter(popGen.getPopulation());
		popWriter.write("./output/population.xml");
	}
}
