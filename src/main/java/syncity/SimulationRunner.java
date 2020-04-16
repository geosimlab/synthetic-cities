package syncity;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import ch.ethz.idsc.amod.ScenarioPreparer;
import ch.ethz.idsc.amod.ScenarioServer;

public class SimulationRunner {

	private static final int RUN_ID = 3;

	public static void main(String[] args) throws Exception {
		Path workdir = Paths.get(".").toAbsolutePath();
		if (args.length > 0) {
			workdir = Paths.get(args[0]).toAbsolutePath();
		}
		workdir = workdir.resolve(String.valueOf(RUN_ID));

		for (String algorithm : ScenarioCreator.DISPATCHING_ALGORITHMS) {
			Path algoDirPath = workdir.resolve(algorithm);
			if (!algoDirPath.toFile().exists())
				Files.createDirectories(algoDirPath);
			ScenarioCreator scenario = new ScenarioCreator(algoDirPath.toString(), 100, 20, 20, algorithm);
			scenario.setAllConfigParams();
			scenario.writeScenarioFiles();
			ScenarioPreparer.run(algoDirPath.toFile());
			ScenarioServer.simulate(algoDirPath.toFile());
		}
	}

}
