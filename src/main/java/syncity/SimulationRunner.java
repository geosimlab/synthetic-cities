package syncity;

import java.nio.file.Path;
import java.nio.file.Paths;

import syncity.scenarios.BaseScenarioCreator;
import syncity.scenarios.ScenarioFactory;
import utils.Consts.AlgorithmsNames;
import utils.Structs.TripTimeArguments;

public class SimulationRunner {

    private static final String RUN_ID = "test-time-parameters";

    protected int popSize = 1000;
    protected int vehiclesNum = 15;
    protected int iterations = 3;
    protected int numOfSt = 20;
    protected int numOfAv = 20;
    protected String[] algorithms = {AlgorithmsNames.HCRS, AlgorithmsNames.DRT};

    public void run(Path workdir) throws Exception {

	workdir = workdir.resolve(String.valueOf(RUN_ID));
	if (workdir.toFile().exists()) {
	    System.out.println("Work directory already exists, change RUN_ID");
	    return;
	}

	TripTimeArguments defaultTimeParameters = new TripTimeArguments();
	

	for (String algorithm : algorithms) {

	    BaseScenarioCreator scenario = ScenarioFactory.getScenario(workdir,
		    algorithm, popSize, numOfSt, numOfAv,
		    vehiclesNum, iterations,
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
