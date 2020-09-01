package syncity.population;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;

import utils.BasicUtils;

/**
 * A class to generate a random (poisson distributed) population in the given
 * network/config.
 * 
 * As I learned so far, amodeus has some restriction on the population format:
 * 1. Activity location must be specified by a link, and not with coordinates,
 * otherwise the population cutter will judge it out of network. 2. Each leg
 * must have a dep_time, and each activity (except for the last) must have
 * end_time, otherwise the population cutter will judge it out of time window
 * for simulation. 3. To use an av leg mode needs to be "av", some time it is
 * changed automatically to "av" and some times not.
 * 
 * 1 & 2 can be avoided by setting populationCutter=NONE in
 * amodeusOptions.properties
 * 
 * @author theFrok
 *
 */
public class RandomPopulationGenerator {

	// default minimal distance, based on the default value of "maxBeelineWalkConnectionDistance"
	public static final int MINIMUN_OD_DISTANCE = 300;

	protected int popSize;
	protected Network network;
	protected Population population;
	protected HashMap<Id<Person>, List<Double>> distanceDistribution;

	public RandomPopulationGenerator(Config config, Network network, int popSize) {
		if (network == null) {
			network = ScenarioUtils.loadScenario(config).getNetwork();
		} else if (config == null) {
			config = ConfigUtils.createConfig();
		}
		if (network.getNodes().size() > popSize) {
			throw new RuntimeException(
					"Populoation size is too small for the size of the network. The number of nodes is: "
							+ network.getNodes().size() + " " + "The population size is: " + popSize);
		}
		this.popSize = popSize;
		this.network = network;
		this.distanceDistribution = new HashMap<>();
		this.population = PopulationUtils.createPopulation(config, network);
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
		writeDistanceInfo(out);
		return out.toAbsolutePath().toString();
	}
	
	protected void writeDistanceInfo(Path populationFileName) throws IOException {
		String out = populationFileName + ".DistanceInfo.csv";
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(out)));
		String header = "PersonId,O-D Distance [m] \n";
		writer.write(header);
		for (Entry<Id<Person>, List<Double>> entry : this.distanceDistribution.entrySet()) {
			String key = entry.getKey().toString();
			for (double dist : entry.getValue()) {
				String line = key + "," + dist + "\n";
				writer.write(line);
			}
		}
		writer.close();
		System.out.println(out);
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
		System.out.println("The number of network nodes is: " + nodesArray.length);
		for (Node homeNode : nodesArray) {
			int popInNode = popSize / network.getNodes().size();
			for (int j = 0; j < popInNode; j++) {
				Node workNode = getWorkNode(nodesArray, homeNode);
				Person person = PersonCreator.createPersonWithStandardPlan(homeNode, workNode, this.population);
				this.population.addPerson(person);
				updateDistanceMap(person);
			}
		}
	}

	protected Node getWorkNode(Node[] nodesArray, Node homeNode) {
		return getWorkNode(nodesArray, homeNode, MINIMUN_OD_DISTANCE);
	}
	
	protected Node getWorkNode(Node[] nodesArray, Node homeNode, double minimumDistance) {
		int workNodeId = BasicUtils.getUniformRandomGenerator().nextInt(nodesArray.length);
		while (BasicUtils.nodesDistance(nodesArray[workNodeId], homeNode) < minimumDistance) {
			workNodeId = BasicUtils.getUniformRandomGenerator().nextInt(nodesArray.length);
		}
		return nodesArray[workNodeId];
	}

	/**
	 * A different name for the same functionality :*
	 */
	public void generatePopulation() {
		populateNodes();
	}

	
	protected void updateDistanceMap(Person person) {
		Id<Person> id = person.getId();
		List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
		Node homeNode = null;
		Node workNode = null;
		for (PlanElement planElement : planElements) {
			if (!(planElement instanceof Activity))
				continue;
			Activity act = (Activity) planElement;
			if (act.getType() == PersonCreator.HOME_ACTIVITY_TYPE) {
				homeNode = this.network.getLinks().get(act.getLinkId()).getToNode();
			}
			if (act.getType() == PersonCreator.WORK_ACTIVITY_TYPE) {
				workNode = this.network.getLinks().get(act.getLinkId()).getToNode();
			}
		}
		if (homeNode != null && workNode != null) {
			double distance = BasicUtils.nodesDistance(homeNode, workNode);
			this.distanceDistribution.put(id, Arrays.asList(distance));
		}
	}


	public static void main(String[] args) throws IOException {

		Config config = ConfigUtils.loadConfig(args[0]);
		RandomPopulationGenerator popGen = new RandomPopulationGenerator(config, 2501);

		popGen.populateNodes();

		popGen.writePopulation("./output/");
	}
}
