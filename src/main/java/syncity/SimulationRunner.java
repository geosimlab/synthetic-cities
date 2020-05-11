package syncity;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.matsim.core.config.Config;

import syncity.scenarios.AmodScenarioCreator;
import syncity.scenarios.DrtScenarioCreator;

public class SimulationRunner {

	private static final int RUN_ID = 12;
	private static final String SINGLE_DISPATCHER = "HighCapacityDispatcher";
	
	public static void run(Path workdir) throws Exception {
		
		workdir = workdir.resolve(String.valueOf(RUN_ID));
		if (workdir.toFile().exists()) {
			System.out.println("Work directory already exists, change RUN_ID");
			return;
		}
		
		final int popSize = 1600;
		final int numOfSt = 20;
		final int numOfAv = 20;
		final int vehiclesNum = 80;
		final int iterations = 20;

		// Drt Scenario
		for (String algorithm : DrtScenarioCreator.getDispatchigAlgorithms()) {
			if (SINGLE_DISPATCHER != null && !SINGLE_DISPATCHER.equals(algorithm))
				continue;
			boolean rebalance = true;
			boolean enableRejection = false;
			
			Path algoDirPath = workdir.resolve(algorithm);
			if (!algoDirPath.toFile().exists())
				Files.createDirectories(algoDirPath);
			DrtScenarioCreator scenario = new DrtScenarioCreator(
					null, algoDirPath.toString(), popSize, numOfSt, numOfAv, 
					vehiclesNum, iterations, algorithm, rebalance, enableRejection);
			
			additionalSetup(scenario.getConfig());
			scenario.prepare();
			scenario.run();
		}
		
		// Amod Scenarios
		for (String algorithm : AmodScenarioCreator.getDispatchigAlgorithms()) {
			if (SINGLE_DISPATCHER != null && !SINGLE_DISPATCHER.equals(algorithm))
				continue;
			int dispatchPeriod = 15;
			
			Path algoDirPath = workdir.resolve(algorithm);
			if (!algoDirPath.toFile().exists())
				Files.createDirectories(algoDirPath);
			AmodScenarioCreator scenario = new AmodScenarioCreator(
					null, algoDirPath.toString(), popSize, numOfSt, numOfAv, 
					vehiclesNum, iterations, algorithm, dispatchPeriod);
			
			additionalSetup(scenario.getConfig());
			scenario.prepare();
			scenario.run();
		}
	}
	
	public static void additionalSetup(Config config) {
		config.qsim().setStartTime(6 * 3600);
		config.qsim().setEndTime(12 * 3600);
	}
	
	public static void main(String[] args) throws Exception {
		Path workdir = Paths.get(".").toAbsolutePath();
		if (args.length > 0) {
			workdir = Paths.get(args[0]).toAbsolutePath();
		}
		run(workdir);
	}

}
