package syncity;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import ch.ethz.idsc.amod.ScenarioPreparer;
import ch.ethz.idsc.amod.ScenarioServer;

public class SimulationRunner {

	private static final int RUN_ID = 4;

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
			ScenarioCreator scenario = new ScenarioCreator(algoDirPath.toString(), 400, 20, 20, algorithm);
			scenario.setAllConfigParams();
			scenario.writeScenarioFiles();
			if (algorithm.equals(ScenarioCreator.DRT_DISPATCHER)) {
				Config conf = ConfigUtils.loadConfig(scenario.getConfigPath(), new DrtConfigGroup(),
						new DvrpConfigGroup(), new OTFVisConfigGroup());
				Controler controler = DrtControlerCreator.createControlerWithSingleModeDrt(conf, false);
				controler.run();
			} else {
				ScenarioPreparer.run(algoDirPath.toFile());
				ScenarioServer.simulate(algoDirPath.toFile());
			}
		}
	}

}
