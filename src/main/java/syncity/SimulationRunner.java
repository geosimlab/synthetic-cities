package syncity;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.matsim.core.config.Config;

import syncity.scenarios.AmodScenarioCreator;
import syncity.scenarios.DrtScenarioCreator;

public class SimulationRunner {

	private static final int RUN_ID = 100;
	private static final String SINGLE_DISPATCHER = "HighCapacityDispatcher";
	private static final String[] SKIP_DISPATCHERS = null; //{"HighCapacityDispatcher", "TShareDispatcher"};
	
	public static void run(Path workdir) throws Exception {
		
		workdir = workdir.resolve(String.valueOf(RUN_ID));
		if (workdir.toFile().exists()) {
			System.out.println("Work directory already exists, change RUN_ID");
			return;
		}
        
		final int popSize = 800;
		final int vehiclesNum = 160;
		final int iterations = 3;
		
		final int numOfSt = 20;
		final int numOfAv = 20;

		// Drt Scenario
		for (String algorithm : DrtScenarioCreator.getDispatchigAlgorithms()) {
			if (SINGLE_DISPATCHER != null && !SINGLE_DISPATCHER.equals(algorithm))
				continue;
			if (SKIP_DISPATCHERS != null && stringInArray(SKIP_DISPATCHERS, algorithm))
				continue;
			
			boolean rebalance = true;
			boolean enableRejection = false;
			
			Path algoDirPath = workdir.resolve(algorithm);
			if (!algoDirPath.toFile().exists())
				Files.createDirectories(algoDirPath);
			DrtScenarioCreator scenario = new DrtScenarioCreator(
					null, algoDirPath.toString(), popSize, numOfSt, numOfAv, 
					vehiclesNum, iterations, algorithm, rebalance, enableRejection);
			
//			additionalSetup(scenario.getConfig());
			scenario.prepare();
			scenario.run();
		}
		
		// Amod Scenarios
		for (String algorithm : AmodScenarioCreator.getDispatchigAlgorithms()) {
			if (SINGLE_DISPATCHER != null && !SINGLE_DISPATCHER.equals(algorithm))
				continue;
			
			if (SKIP_DISPATCHERS != null && stringInArray(SKIP_DISPATCHERS, algorithm))
				continue;
			
			int dispatchPeriod = 15;
			Path algoDirPath = workdir.resolve(algorithm);
			if (!algoDirPath.toFile().exists())
				Files.createDirectories(algoDirPath);
			AmodScenarioCreator scenario = new AmodScenarioCreator(
					null, algoDirPath.toString(), popSize, numOfSt, numOfAv, 
					vehiclesNum, iterations, algorithm, dispatchPeriod);
			
//			additionalSetup(scenario.getConfig());
			scenario.prepare();
			scenario.run();
		}
	}
	
	public static void additionalSetup(Config config) {
		config.qsim().setStartTime(6 * 3600);
		config.qsim().setEndTime(12 * 3600);
	}
	
	public static boolean stringInArray(String[] arr, String name) {
		for (String string : arr) {
			if (string.equals(name)) return true;
		}
		return false;
	}
	
	public static void main(String[] args) throws Exception {
		Path workdir = Paths.get(".").toAbsolutePath();
		if (args.length > 0) {
			workdir = Paths.get(args[0]).toAbsolutePath();
		}
		run(workdir);
	}

}
