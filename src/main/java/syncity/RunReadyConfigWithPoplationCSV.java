package syncity;

import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import syncity.population.PopulationCSV;
import syncity.scenarios.BaseScenarioCreator;
import syncity.scenarios.DrtScenarioCreator;
import utils.Structs.DispatcherArguments;

public class RunReadyConfigWithPoplationCSV {

    public static void main(String[] args) throws Exception {
	DispatcherArguments dispatcherParams = new DispatcherArguments();
	
	dispatcherParams.alpha = 1;
	dispatcherParams.beta = 340;
	dispatcherParams.maxWaitTime = 15 * 60;
	
	dispatcherParams.stopTime = 1;
	dispatcherParams.speedEstimation = 60 / 3.6f;
	
	int interations = 8;
	
	
	String dir = "E:/Files/CodeProjects/MATSim/Scenarios/drt-line-one-car-reverse/";
	String configPath = dir + "/generated_config_base.xml";
	String populationCSVPath = dir + "/populationData.csv";
	Config config = ConfigUtils.loadConfig(configPath);
	
	Population pop = PopulationCSV.readPopulationCSV(populationCSVPath);
	new PopulationWriter(pop).write(config.plans().getInputFile());
	
	
	BaseScenarioCreator scenario = new DrtScenarioCreator(config, dir, interations, "DRT", dispatcherParams);
	
	scenario.prepare();
	scenario.run();
    }
}
