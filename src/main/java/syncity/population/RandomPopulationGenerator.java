package syncity.population;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;

import utils.BasicUtils;

/**
 * A class to generate a random population in the given
 * network/config.
 * 
 * As I learned so far, amodeus has some restriction on the population format:
 * 1. Activity location must be specified by a link, and not with coordinates,
 * otherwise the population cutter will judge it out of network.
 * 2. Each leg must have a dep_time, and each activity (except for the last)
 * must have end_time, otherwise the population cutter will judge it out of time
 * window for simulation.
 * 3. To use an av leg mode needs to be "av", some time it is changed
 * automatically to "av" and some times not.
 * 
 * 1 & 2 can be avoided by setting populationCutter=NONE in
 * amodeusOptions.properties
 * 
 * @author theFrok
 *
 */
public class RandomPopulationGenerator {

    // default minimal distance, based on the default value of
    // "maxBeelineWalkConnectionDistance"
    public static final int MINIMUN_OD_DISTANCE = 300;
    public static final int HOME_NODE_ID = 0;
    public static final int WORK_NODE_ID = 1;

    protected int popSize;
    protected Network network;
    protected Population population;
    protected HashMap<String, Integer> nodeAsHomeWork;

    public RandomPopulationGenerator(Config config, Network network,
	    int popSize) {
	if (network == null) {
	    network = ScenarioUtils.loadScenario(config).getNetwork();
	} else if (config == null) {
	    config = ConfigUtils.createConfig();
	}
	this.popSize = popSize;
	this.network = network;
	this.nodeAsHomeWork = new HashMap<>();
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
	writeNodesStats(out);
	return out.toAbsolutePath().toString();
    }

    protected void writeDistanceInfo(Path populationFileName)
	    throws IOException {
	Map<Integer, Long> counts = population.getPersons().values().stream() //
		.collect(Collectors.groupingBy(p -> (int) PersonAnalysis
			.getHomeWorkDistance(p, network),
			Collectors.counting()));
	String out = populationFileName + ".DistanceInfo.csv";
	BasicUtils.writeSimpleMap(counts, out, //
		Arrays.asList("Distance", "Count"));
    }

    protected void writeNodesStats(Path populationFileName) throws IOException {
	String out = populationFileName + ".NodesStats.csv";
	BasicUtils.writeSimpleMap(nodeAsHomeWork, out, //
		Arrays.asList("NodeId;Type", "Count"));
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
	System.out.println(
		"The number of network nodes is: " + nodesArray.length);
	for (int j = 0; j < popSize; j++) {
	    Node homeNode = BasicUtils.chooseRand(nodesArray);
	    Node workNode = chooseWorkNode(nodesArray, homeNode);
	    Person person = PersonCreator.createPersonWithStandardPlan(homeNode, //
		    workNode, this.population);
	    this.population.addPerson(person);
	    updateStatsMaps(person);
	}
    }

    protected Node chooseWorkNode(Node[] nodesArray, Node homeNode) {
	return chooseWorkNode(nodesArray, homeNode, MINIMUN_OD_DISTANCE);
    }

    protected Node chooseWorkNode(Node[] nodesArray, Node homeNode,
	    double minimumDistance) {
	Node workNode = BasicUtils.chooseRand(nodesArray);
	while (BasicUtils.nodesDistance(workNode, homeNode) < minimumDistance) {
	    workNode = BasicUtils.chooseRand(nodesArray);
	}
	return workNode;
    }

    /**
     * A different name for the same functionality :*
     */
    public void generatePopulation() {
	populateNodes();
    }

    protected void updateStatsMaps(Person person) {
	Pair<Node, Node> homeWorkPair = PersonAnalysis.getPersonHomeWork(person, //
		network);
	Node home = homeWorkPair.getLeft();
	Node work = homeWorkPair.getRight();
	if (home != null && work != null) {
	    // update home node stats
	    updateNodeStat(home, HOME_NODE_ID);
	    // update work node stats
	    updateNodeStat(work, WORK_NODE_ID);
	}
    }

    protected void updateNodeStat(Node node, int type) {
	String key = node.getId().toString() + "\";\"" + type;
	int curValue = nodeAsHomeWork.getOrDefault(key, 0);
	nodeAsHomeWork.put(key, curValue + 1);
    }

    public static void main(String[] args) throws IOException {

	int popSize = 10000;
	RandomPopulationGenerator popGen;
	if (args[1].chars().allMatch(Character::isDigit)) {
	    popSize = Integer.parseInt(args[1]);
	}

	if (args[0].toLowerCase().contains("network")) {
	    Network network = NetworkUtils.readNetwork(args[0]);
	    popGen = new RandomPopulationGenerator(network, popSize);
	} else {
	    Config config = ConfigUtils.loadConfig(args[0]);
	    popGen = new RandomPopulationGenerator(config, popSize);
	}
	popGen.populateNodes();

	popGen.writePopulation("./output/");
    }
}
