package syncity;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.matsim.core.config.Config;

import syncity.scenarios.AmodScenarioCreator;
import syncity.scenarios.DrtScenarioCreator;

public class SimulationRunner {

	private static final String RUN_ID = "100";
	private static final String SINGLE_DISPATCHER = "ExtDemandSupplyBeamSharing";
	private static final String[] SKIP_DISPATCHERS = null; //{"HighCapacityDispatcher", "TShareDispatcher"};
	
	protected int popSize = 10000;
	protected int vehiclesNum = 2500;
	protected int iterations = 1;
	protected int numOfSt = 100;
	protected int numOfAv = 100;
	
	public void run(Path workdir) throws Exception {
		
		workdir = workdir.resolve(String.valueOf(RUN_ID));
		if (workdir.toFile().exists()) {
			System.out.println("Work directory already exists, change RUN_ID");
			return;
		}
        
		

		// Drt Scenario
		for (String algorithm : DrtScenarioCreator.getDispatchigAlgorithms()) {
			if (SINGLE_DISPATCHER != null && !SINGLE_DISPATCHER.equals(algorithm))
				continue;
			if (SKIP_DISPATCHERS != null && Utils.arrayContains(SKIP_DISPATCHERS, algorithm))
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
			
			if (SKIP_DISPATCHERS != null && Utils.arrayContains(SKIP_DISPATCHERS, algorithm))
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
	
	public static void main(String[] args) throws Exception {
		Path workdir = Paths.get(".").toAbsolutePath();
		if (args.length > 0) {
			workdir = Paths.get(args[0]).toAbsolutePath();
		}
		new SimulationRunner().run(workdir);
	}

}
