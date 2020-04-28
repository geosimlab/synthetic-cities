package syncity;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import syncity.scenarios.AmodScenarioCreator;
import syncity.scenarios.DrtScenarioCreator;

public class SimulationRunner {

	private static final int RUN_ID = 5;
	
	public static void run(Path workdir) throws Exception {
		
		workdir = workdir.resolve(String.valueOf(RUN_ID));
		
		final int popSize = 400;
		final int numOfSt = 20;
		final int numOfAv = 20;
		final int vhiclesNum = 80;
		final int iterations = 1;

		// Drt Scenario
		for (String algorithm : DrtScenarioCreator.getDispatchigAlgorithms()) {
			boolean rebalance = true;
			boolean enableRejection = false;
			Path algoDirPath = workdir.resolve(algorithm);
			if (!algoDirPath.toFile().exists())
				Files.createDirectories(algoDirPath);
			DrtScenarioCreator scenario = new DrtScenarioCreator(
					null, algoDirPath.toString(), popSize, numOfSt, numOfAv, 
					vhiclesNum, iterations, algorithm, rebalance, enableRejection);
			scenario.prepare();
			scenario.run();
		}
		// Amod Scenarios
		for (String algorithm : AmodScenarioCreator.getDispatchigAlgorithms()) {
			int dispatchPeriod = 15;
			Path algoDirPath = workdir.resolve(algorithm);
			if (!algoDirPath.toFile().exists())
				Files.createDirectories(algoDirPath);
			AmodScenarioCreator scenario = new AmodScenarioCreator(
					null, algoDirPath.toString(), popSize, numOfSt, numOfAv, 
					vhiclesNum, iterations, algorithm, dispatchPeriod);
			scenario.prepare();
			scenario.run();
		}
	}
	
	public static void main(String[] args) throws Exception {
		Path workdir = Paths.get(".").toAbsolutePath();
		if (args.length > 0) {
			workdir = Paths.get(args[0]).toAbsolutePath();
		}
		run(workdir);
	}

}
