package syncity;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import syncity.network.GridNetworkGenerator;
import syncity.population.RandomPopulationGenerator;
import syncity.scenarios.BaseScenarioCreator;
import syncity.scenarios.ScenarioFactory;
import utils.Consts.AlgorithmsNames;
import utils.Structs.NetworkArguments;
import utils.Structs.PopulationArguments;
import utils.Structs.TripTimeArguments;

public class SimulationRunner {

    private static final String RUN_ID = "ploop2";

    protected int vehiclesNum = 15;
    protected int iterations = 3;

    protected String[] algorithms = { AlgorithmsNames.HCRS,
	    AlgorithmsNames.DRT };

    public void run(Path workdir) throws Exception {

	workdir = workdir.resolve(RUN_ID);
	if (workdir.toFile().exists()) {
	    System.out.println("Work directory already exists, change RUN_ID");
	    return;
	} else {
	    Files.createDirectories(workdir);
	}

	TripTimeArguments timeParameters = new TripTimeArguments();
	PopulationArguments popParameters = new PopulationArguments();
	NetworkArguments networkParameters = new NetworkArguments();

	for (String algorithm : algorithms) {
	    
	    GridNetworkGenerator grid = new GridNetworkGenerator(networkParameters);
	    grid.generateGridNetwork();
	    String net = grid.writeNetwork(workdir.toString());
	    RandomPopulationGenerator popGen = new RandomPopulationGenerator(
		    grid.getNetwork(), popParameters);
	    popGen.populateNodes();
	    String plansFile = popGen.writePopulation(workdir.toString());
	    
	    BaseScenarioCreator scenario = ScenarioFactory.getScenario(workdir,
		    algorithm, plansFile, net, vehiclesNum, iterations,
		    (TripTimeArguments) timeParameters.clone());

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
