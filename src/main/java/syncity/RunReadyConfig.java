package syncity;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import syncity.scenarios.BaseScenarioCreator;
import syncity.scenarios.DrtScenarioCreator;
import utils.Structs.DispatcherArguments;

public class RunReadyConfig {

    public static void main(String[] args) throws Exception {
	DispatcherArguments dispatcherParams = new DispatcherArguments();
	
	dispatcherParams.alpha = 1;
	dispatcherParams.beta = 340;
	dispatcherParams.maxWaitTime = 15 * 60;
	
	dispatcherParams.stopTime = 1;
	dispatcherParams.speedEstimation = 60 / 3.6f;
	
	
	String dir = "E:/Files/CodeProjects/MATSim/Scenarios/drt-line-one-car-reverse/";
	String configPath = dir + "/generated_config_base.xml";
	
	Config config = ConfigUtils.loadConfig(configPath);
	
	BaseScenarioCreator scenario = new DrtScenarioCreator(config, dir, 8, "DRT", dispatcherParams);
	
	scenario.prepare();
	scenario.run();
    }
}
