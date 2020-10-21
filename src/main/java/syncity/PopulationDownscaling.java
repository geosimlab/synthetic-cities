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
import utils.Structs.DispatcherArguments;

public class PopulationDownscaling {
    
    
    private static final String RUN_ID = "downscaling-test-line-beta1800-rebalance";

    protected int iterations = 20;

    protected float[] kValues = {1, 0.5f, 0.25f, 0.05f, 0.01f};
    protected String algorithm = AlgorithmsNames.DRT;

    public void run(Path workdir) throws Exception {

	workdir = workdir.resolve(RUN_ID);
	if (workdir.toFile().exists()) {
	    System.out.println("Work directory already exists, change RUN_ID");
	    return;
	} else {
	    Files.createDirectories(workdir);
	}

	DispatcherArguments dispatcherParams = new DispatcherArguments();
	PopulationArguments popParameters = new PopulationArguments();
	NetworkArguments networkParameters = new NetworkArguments();

	GridNetworkGenerator grid = new GridNetworkGenerator(networkParameters);
	grid.generateGridNetwork();
	String net = grid.writeNetwork(workdir.toString());
	RandomPopulationGenerator popGen = new RandomPopulationGenerator(
		grid.getNetwork(), popParameters);
	popGen.populateNodes();
	
	for (float sampleSize: kValues) {
	    String plansFile = popGen.writePopulation(workdir.toString(), sampleSize);
	    Path scenarioPath = workdir.resolve(algorithm+"-k"+sampleSize);
	    Files.createDirectories(scenarioPath);
	    BaseScenarioCreator scenario = ScenarioFactory.getScenario(scenarioPath.toString(),
		    algorithm, plansFile, net, iterations,
		    (DispatcherArguments) dispatcherParams.clone());

	    scenario.prepare();
	    scenario.run();
	}
    }

    public static void main(String[] args) throws Exception {
	Path workdir = Paths.get(".").toAbsolutePath();
	if (args.length > 0) {
	    workdir = Paths.get(args[0]).toAbsolutePath();
	}
	new PopulationDownscaling().run(workdir);
    }
}
