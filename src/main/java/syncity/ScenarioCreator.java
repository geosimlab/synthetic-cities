package syncity;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;

import ch.ethz.matsim.av.config.AVConfigGroup;
import ch.ethz.matsim.av.config.operator.OperatorConfig;
import ch.ethz.matsim.av.framework.AVModule;;

/*
 * [] Plan
 *    [] @pre: have a folder with these files: `config.xml`, `AmodeusOptions.properties` and `LPOptions.properties`
 *    [x] read config
 *    [x] if the (config is missing network):
 *        [x] create network file and add it to the config
 *        [x] create population and add it as well
 *        [x] else if (config is missing population file):
 *            [x] read network
 *            [x] create population
 *    [] for each algorithm
 *        [x] create an AVConfigGroup
 *        [x] add the ConfigGroup to the config (and update outputFolder)
 *        [] prepare aMoD scenario
 *        [] run aMoD server
 *        
 *  @author: theFrok
 */
public class ScenarioCreator {

	private static final Logger log = Logger.getLogger(ScenarioCreator.class);

	private static final String SCENARIO_CONFIG_FILENAME = "generated_config.xml";
	private static final String DEFAULT_DISPATCHER_ALGORITHM = "ExtDemandSupplyBeamSharing";
	private static final String OUTPUT_DIR = "output";
	private static final String SCENARIO_BASE_DIR = "ScenarioBaseFiles/";
	private static final String[] SCENARIO_BASE_FILES = {"LPOptions.properties", "AmodeusOptions.properties"};
	private static final int POP_SIZE = 100;

	@SuppressWarnings("unused")
	private Path configPath;
	private Path scenarioDir;
	private int numOfVehicles = 100;
	private Config config;

	public ScenarioCreator(String baseConfigPath, String scenarioDirPath, String dispatcher, int numOfIterations)
			throws IOException {
		this.configPath = Paths.get(baseConfigPath);
		this.scenarioDir = Paths.get(scenarioDirPath);
		this.config = ConfigUtils.loadConfig(baseConfigPath);
		this.setAllConfigParams(scenarioDirPath, dispatcher, numOfIterations);
		this.createScenarioDir();
	}

	private void createScenarioDir() throws IOException {
		ConfigUtils.writeConfig(this.config, this.scenarioDir.resolve(SCENARIO_CONFIG_FILENAME).toString());
		copyScenarioResources(this.scenarioDir);
		
	}

	private static void copyScenarioResources(Path destDir) throws IOException {
		ClassLoader classLoader = ClassLoader.getSystemClassLoader();

		for (String resource : SCENARIO_BASE_FILES) {
			String filename = SCENARIO_BASE_DIR + resource;
			File file = new File(classLoader.getResource(filename).getFile());
			Path destPath = destDir.resolve(resource);
			
			// Copy file to the scenario folder3
			Files.copy(file.toPath(), destPath, StandardCopyOption.REPLACE_EXISTING);
		}

	}

	public void setAllConfigParams(String scenarioDirPath, String dispatcher, int numOfIterations) throws IOException {
		this.setQSimParams();

		boolean newNetwork = this.addNetworkIfMissing(false);
		this.addPopulationIfMissing(newNetwork);

		this.addAVConfigGroup(dispatcher);
		this.setPlanSelectionParams();
		this.setPlanCalcScoreParams();
		setControlerParams(numOfIterations, false);
		this.config.checkConsistency();
	}

	public static void main(String[] args) throws IOException {
		new ScenarioCreator(args[0], args[1], DEFAULT_DISPATCHER_ALGORITHM, 1);
	}

	private boolean addNetworkIfMissing(boolean force) throws IOException {
		if (this.config.network().getInputFile() != null && !force) {
			log.info("Network file is already specified in the config");
			return false;
		} else {
			GridGenerator grid = new GridGenerator();
			grid.generateGridNetwork();
			String netwokFile = grid.writeNetwork(this.scenarioDir.toString());
			Path relativeNetworkFile = Paths.get(netwokFile).getFileName();
			this.config.network().setInputFile(relativeNetworkFile.toString());
			return true;
		}
	}

	private boolean addPopulationIfMissing(boolean force) throws IOException {
		if (this.config.plans().getInputFile() != null && !force) {
			log.info("Plans (Population) file is already specified in the config");
			return false;
		} else {
			RandomPopulationGenerator popGen = new RandomPopulationGenerator(this.config, POP_SIZE);
			popGen.populateNodes();
			String plansFile = popGen.writePopulation(this.scenarioDir.toString());
			Path relativeNetworkFile = Paths.get(plansFile).getFileName();
			this.config.plans().setInputFile(relativeNetworkFile.toString());
			return true;
		}
	}

	private AVConfigGroup addAVConfigGroup(String dispatcherAlgorithm) {
		int dispatchPeriod = 10; // Seconds
		String allowedLinkMode = "car";
		AVConfigGroup avConfigGroup = new AVConfigGroup();
		avConfigGroup.setAllowedLinkMode(allowedLinkMode);

		OperatorConfig operator = (OperatorConfig) avConfigGroup.createParameterSet(OperatorConfig.GROUP_NAME);
		operator.setPredictRouteTravelTime(true);
		// Dispatcher
		operator.getDispatcherConfig().setType(dispatcherAlgorithm);
		operator.getDispatcherConfig().addParam("dispatchPeriod", String.valueOf(dispatchPeriod));
		// Vehicle Generator
		operator.getGeneratorConfig().setNumberOfVehicles(this.numOfVehicles);

		avConfigGroup.addParameterSet(operator);
		this.config.addModule(avConfigGroup);
		return avConfigGroup;
	}

	private void setControlerParams(int NumberOfIterations, boolean overrideExistingOutput) {
		final Config config = this.config;
		config.controler().setWriteEventsInterval(1);
		config.controler().setWritePlansInterval(1);
		config.controler().setFirstIteration(1);
		if (overrideExistingOutput)
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		config.controler().setLastIteration(NumberOfIterations);
		config.controler().setMobsim("qsim");
		config.controler().setRoutingAlgorithmType(RoutingAlgorithmType.FastAStarLandmarks);
		String outputDir = this.scenarioDir.resolve(OUTPUT_DIR).toString();
		config.controler().setOutputDirectory(outputDir);
	}

	private void setQSimParams() {
		this.config.qsim().setStartTime(0);
		this.config.qsim().setEndTime(30 * 3600);
		this.config.qsim().setNumberOfThreads(4);
	}

	private void setPlanSelectionParams() {
		// Add strategy - plan selector
		StrategySettings changeExpStrategy = new StrategySettings();
		changeExpStrategy.setDisableAfter(-1);
		changeExpStrategy.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
		changeExpStrategy.setWeight(0.8);
		this.config.strategy().addStrategySettings(changeExpStrategy);

		// Add strategy - time-mutation
		StrategySettings timeMutatorStrategy = new StrategySettings();
		timeMutatorStrategy
				.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator.toString());
		timeMutatorStrategy.setWeight(0.1);
		this.config.strategy().addStrategySettings(timeMutatorStrategy);
	}

	private void setPlanCalcScoreParams() {
		this.config.planCalcScore().setEarlyDeparture_utils_hr(0.0);
		this.config.planCalcScore().setLateArrival_utils_hr(0);
		this.config.planCalcScore().setMarginalUtilityOfMoney(0.062);
		this.config.planCalcScore().setPerforming_utils_hr(0.96);
		this.config.planCalcScore().setUtilityOfLineSwitch(0);
		this.config.planCalcScore().setMarginalUtlOfWaitingPt_utils_hr(-0.18);

		PlanCalcScoreConfigGroup.ModeParams avCalcScoreParams = new PlanCalcScoreConfigGroup.ModeParams(
				AVModule.AV_MODE);
		avCalcScoreParams.setMode(AVModule.AV_MODE);
		avCalcScoreParams.setMarginalUtilityOfTraveling(-6.0);
		this.config.planCalcScore().addModeParams(avCalcScoreParams);

		ActivityParams home = new ActivityParams("home");
		home.setTypicalDuration(16 * 60 * 60);
		config.planCalcScore().addActivityParams(home);

		ActivityParams work = new ActivityParams("work");
		work.setOpeningTime(6 * 3600);
		work.setClosingTime(20 * 3600);
		work.setTypicalDuration(8 * 60 * 60);
		config.planCalcScore().addActivityParams(work);
	}

}
