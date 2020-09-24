package syncity;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import syncity.network.GridNetworkGenerator;
import syncity.population.RandomPopulationGenerator;
import syncity.scenarios.BaseScenarioCreator;
import syncity.scenarios.ScenarioFactory;
import utils.Consts.AlgorithmsNames;
import utils.Structs.TripTimeArguments;

public class SimulationRunner {

    private static final String RUN_ID = "ploop";

    protected int popSize = 1000;
    protected int vehiclesNum = 15;
    protected int iterations = 3;
    protected int numOfSt = 20;
    protected int numOfAv = 20;
    protected String[] algorithms = { AlgorithmsNames.HCRS, AlgorithmsNames.DRT};

    public void run(Path workdir) throws Exception {

	workdir = workdir.resolve(RUN_ID);
	if (workdir.toFile().exists()) {
	    System.out.println("Work directory already exists, change RUN_ID");
	    return;
	} else {
	    Files.createDirectories(workdir);
	}

	TripTimeArguments defaultTimeParameters = new TripTimeArguments();

	for (String algorithm : algorithms) {
	    GridNetworkGenerator grid = new GridNetworkGenerator(numOfSt,
		    numOfAv);
	    grid.generateGridNetwork();
	    String net = grid.writeNetwork(workdir.toString());
	    RandomPopulationGenerator popGen = new RandomPopulationGenerator(
		    grid.getNetwork(), popSize);
	    popGen.populateNodes();
	    String plansFile = popGen.writePopulation(workdir.toString());
	    BaseScenarioCreator scenario = ScenarioFactory.getScenario(workdir,
		    algorithm, plansFile, net, vehiclesNum, iterations,
		    (TripTimeArguments) defaultTimeParameters.clone());

	    scenario.prepare();
	    scenario.run();
	}
    }

    public static void main(String[] args) throws Exception {
	Path workdir = Paths.get(".").toAbsolutePath();
	if (args.length > 0) {
	    workdir = Paths.get(args[0]).toAbsolutePath();
	}
	new SimulationRunner().run(workdir);
    }

}
