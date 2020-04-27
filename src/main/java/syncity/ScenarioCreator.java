package syncity;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingParams;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigGroup.OperationalScheme;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.FleetWriter;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import ch.ethz.matsim.av.config.AVConfigGroup;
import ch.ethz.matsim.av.config.operator.GeneratorConfig;
import ch.ethz.matsim.av.config.operator.OperatorConfig;
import ch.ethz.matsim.av.framework.AVModule;

/*  
 *  @author: theFrok
 */
public class ScenarioCreator {

	private static final Logger log = Logger.getLogger(ScenarioCreator.class);

	private static final String SCENARIO_CONFIG_FILENAME = "generated_config.xml";
	private static final String OUTPUT_DIR = "output";
	private static final String SCENARIO_BASE_DIR = "ScenarioBaseFiles/";
	private static final String[] SCENARIO_BASE_FILES = { "LPOptions.properties", "AmodeusOptions.properties" };
	public static final String DRT_DISPATCHER = "DRT";
	public static final String[] DISPATCHING_ALGORITHMS = { DRT_DISPATCHER, "TShareDispatcher", "ExtDemandSupplyBeamSharing",
			"DynamicRideSharingStrategy", "HighCapacityDispatcher" };

	private Path configPath;
	private Path scenarioDir;
	private Config config;
	private ConfigGroup vehicleConfig;
	private boolean isAVDispatcher;

	private int popSize = 1000;
	private int numOfStreets = 20;
	private int numOfAvenues = 20;
	private int numOfVehicles = 100;
	private int numOfIterations = 3;
	private int dispatchPeriod = 10;
	private String dispatcherAlgorithm;
	private String allowedLinkMode = TransportMode.car;

	public ScenarioCreator(String baseConfigPath, String scenarioDirPath, int popSize, int numOfStreets,
			int numOfAvenues, String dispatcherAlgorithm) throws IOException {
		this(scenarioDirPath, popSize, numOfStreets, numOfAvenues, dispatcherAlgorithm);
		this.configPath = Paths.get(baseConfigPath);
		this.config = ConfigUtils.loadConfig(this.configPath.toString());
		this.setAllConfigParams();
	}

	public ScenarioCreator(String scenarioDirPath, int popSize, int numOfStreets, int numOfAvenues,
			String dispatcherAlgorithm) {
		this(scenarioDirPath, dispatcherAlgorithm);
		this.popSize = popSize;
		this.numOfAvenues = numOfAvenues;
		this.numOfStreets = numOfStreets;
	}

	public ScenarioCreator(Config baseConfig, String scenarioDirPath, int popSize, int numOfStreets, int numOfAvenues,
			String dispatcherAlgorithm) {
		this(baseConfig, scenarioDirPath, dispatcherAlgorithm);
		this.popSize = popSize;
		this.numOfAvenues = numOfAvenues;
		this.numOfStreets = numOfStreets;
	}

	public ScenarioCreator(String scenarioDirPath, String dispatcherAlgorithm) {
		this.configPath = null;
		this.scenarioDir = Paths.get(scenarioDirPath);
		this.config = ConfigUtils.createConfig(scenarioDirPath.toString());
		this.dispatcherAlgorithm = dispatcherAlgorithm;
	}

	public ScenarioCreator(Config baseConfig, String scenarioDirPath, String dispatcherAlgorithm) {
		this.configPath = null;
		this.scenarioDir = Paths.get(scenarioDirPath);
		this.config = baseConfig;
		this.dispatcherAlgorithm = dispatcherAlgorithm;
	}

	/**
	 * Set all ConfigGroups that are not default (qsim, network, population, AV,
	 * planSelection, planCalcScore, controler) and checks consistency at the end
	 * 
	 * @param dispatcher      the string name of the Dispatcher algorithm
	 * @param numOfIterations number of iteration to for the simulation
	 * @throws IOException might get an IOException while writing population and
	 *                     network files to scenario directory
	 */
	public void setAllConfigParams() throws IOException {
		this.setQSimParams();
		boolean newNetwork = this.addNetworkIfMissing(false);
		this.addPopulationIfMissing(newNetwork);
		if (this.dispatcherAlgorithm.equals(DRT_DISPATCHER)) {
			this.addDRTConfigGroup(true); // maybe this needs zones file check (ZoneDemand..)
		} else {
			this.addAVConfigGroup();
		}
		this.setPlanSelectionParams();
		this.setPlanCalcScoreParams();
		this.setControlerParams(true);

		this.config.checkConsistency();
	}

	/**
	 * Write the created config to the scenario directory, and than copy the
	 * ScenarioBaseFiles from resources to that same directory.
	 * 
	 * Use this method after you finished setting all the parameters
	 * 
	 * @throws IOException
	 */
	public void writeScenarioFiles() throws IOException {
		ConfigUtils.writeConfig(this.config, this.scenarioDir.resolve(SCENARIO_CONFIG_FILENAME).toString());
		copyScenarioResources(this.scenarioDir);

	}

	/**
	 * Copy the ScenarioBaseFiles to destDir
	 * 
	 * @param destDir the directory to write the files into
	 * @throws IOException
	 */
	private static void copyScenarioResources(Path destDir) throws IOException {
		ClassLoader classLoader = ClassLoader.getSystemClassLoader();

		for (String resource : SCENARIO_BASE_FILES) {
			String filename = SCENARIO_BASE_DIR + resource;
			Path srcPath = new File(classLoader.getResource(filename).getFile()).toPath();
			copyFileToDir(srcPath, destDir);
		}

	}

	/**
	 * Copies a file to the given dir, while keeping it's basename.
	 * 
	 * @param srcPath The path to the source file to copy
	 * @param destDir The path to the dir to copy the file to
	 * @return the path to the newly created file
	 * @throws IOException
	 */
	private static Path copyFileToDir(Path srcPath, Path destDir) throws IOException {
		// Join the filename to the dest path
		Path destPath = destDir.resolve(srcPath.getFileName());

		// Copy file to the dest dir
		Files.copy(srcPath, destPath, StandardCopyOption.REPLACE_EXISTING);

		return destPath;
	}

	/**
	 * Checks if the config already has a network file specified, if so copies that
	 * file to the scenario dir, if not creates one using the {@link GridNetworkGenerator}
	 * and write it to the scenario dir.
	 * 
	 * @param force If true creates a new network even if there is already one
	 *              specified in the config
	 * @return true if a new network was created
	 * @throws IOException
	 */
	private boolean addNetworkIfMissing(boolean force) throws IOException {
		String filename;
		boolean newNetwork;
		if (this.config.network().getInputFile() == null || force) {
			GridNetworkGenerator grid = new GridNetworkGenerator(this.numOfStreets, this.numOfAvenues);
			grid.generateGridNetwork();
			String netwokFile = grid.writeNetwork(this.scenarioDir.toString());
			log.info("network file is: " + netwokFile);
			Path relativeNetworkFile = Paths.get(netwokFile).getFileName();
			filename = relativeNetworkFile.toString();
			newNetwork = true;
		} else {
			log.info("Network file is already specified in the config, copying it to the scenario dir");
			String curFilename = this.config.network().getInputFile();
			Path srcPath = this.configPath.getParent().resolve(curFilename);
			Path destPath = copyFileToDir(srcPath, this.scenarioDir);
			filename = destPath.getFileName().toString();
			newNetwork = false;
		}
		this.config.network().setInputFile(filename);
		return newNetwork;
	}

	/**
	 * Checks if the config already has a plans file specified, if so copies that
	 * file to the scenario dir, if not creates one using the
	 * {@link RandomPopulationGenerator} and write it to the scenario dir.
	 * 
	 * @param force create new population even if one already exists
	 * @return return true if a new population was created
	 * @throws IOException
	 */
	private boolean addPopulationIfMissing(boolean force) throws IOException {
		String filename;
		boolean newPopulation;
		if (this.config.plans().getInputFile() == null || force) {
			RandomPopulationGenerator popGen = new RandomPopulationGenerator(this.config, this.popSize);
			popGen.populateNodes();
			String plansFile = popGen.writePopulation(this.scenarioDir.toString());
			filename = Paths.get(plansFile).getFileName().toString();
			newPopulation = true;
		} else {
			log.info("Plans (Population) file is already specified in the config, copying it to the scenario dir");
			String curFilename = this.config.plans().getInputFile();
			Path srcPath = this.configPath.getParent().resolve(curFilename);
			Path destPath = copyFileToDir(srcPath, scenarioDir);
			filename = destPath.getFileName().toString();
			newPopulation = false;
		}
		this.config.plans().setInputFile(filename);
		return newPopulation;
	}

	/**
	 * Creates and sets the AV config for the dispatcher, including
	 * dispatcherAlgorithm, allowedLinkMode, dispatchPeriod, numberOfVehicles
	 * 
	 * @param dispatcherAlgorithm the name of the dispatcher algorithm
	 * 
	 */
	private void addAVConfigGroup() {
		AVConfigGroup avConfigGroup = new AVConfigGroup();
		avConfigGroup.setAllowedLinkMode(this.allowedLinkMode);

		OperatorConfig operator = (OperatorConfig) avConfigGroup.createParameterSet(OperatorConfig.GROUP_NAME);
		operator.setPredictRouteTravelTime(true);
		// Dispatcher
		operator.getDispatcherConfig().setType(this.dispatcherAlgorithm);
		// TODO add rebalancePeriod as in DynamicRideSharingStrategy
		operator.getDispatcherConfig().addParam("dispatchPeriod", String.valueOf(this.dispatchPeriod));
		// Vehicle Generator
		operator.getGeneratorConfig().setNumberOfVehicles(this.numOfVehicles);

		avConfigGroup.addParameterSet(operator);
		this.config.addModule(avConfigGroup);
		this.vehicleConfig = avConfigGroup;
		this.isAVDispatcher = true;
	}

	/**
	 * Creates and sets the DRT config for the dispatcher, including
	 * dispatcherAlgorithm, allowedLinkMode, dispatchPeriod, numberOfVehicles
	 * 
	 * @param dispatcherAlgorithm the name of the dispatcher algorithm
	 * 
	 */
	private void addDRTConfigGroup(boolean rebalancing) {
		DrtConfigGroup drtConfigGroup = new DrtConfigGroup();
		// We use av so we'll be able to use the same plans for DRT and AV
		drtConfigGroup.setMode("av");
		drtConfigGroup.setOperationalScheme(OperationalScheme.door2door.toString());;
		drtConfigGroup.setMaxTravelTimeAlpha(1.3);
		drtConfigGroup.setMaxTravelTimeBeta(1200);
		drtConfigGroup.setMaxWaitTime(1200);
		drtConfigGroup.setRequestRejection(false);
		drtConfigGroup.setStopDuration(60.0);
		String vehiclesFile = createVehiclesFile();
		drtConfigGroup.setVehiclesFile(vehiclesFile);
		
		if (rebalancing) {
			MinCostFlowRebalancingParams rebalance = new MinCostFlowRebalancingParams();
			rebalance.setTargetAlpha(0.5);
			rebalance.setTargetBeta(0.5);
			rebalance.setInterval(1800);
			rebalance.setCellSize(2000);
			drtConfigGroup.addParameterSet(rebalance);
		}
		
//		MultiModeDrtConfigGroup multiDrt = new MultiModeDrtConfigGroup();
//		multiDrt.addParameterSet(drtConfigGroup);
//		config.addModule(multiDrt);
		config.addModule(drtConfigGroup); 
		DvrpConfigGroup dvrp = new DvrpConfigGroup();
		config.addModule(dvrp);
		OTFVisConfigGroup otfvis = new OTFVisConfigGroup();
		config.addModule(otfvis);
		
		DrtControlerCreator.createControlerWithSingleModeDrt(config, false);

		this.vehicleConfig = drtConfigGroup;
		this.isAVDispatcher = false;
	}
	
	private String createVehiclesFile() {
		
		int seatsPerVehicle = 4; //this is important for DRT, value is not used by taxi
		double operationStartTime = 0;
		double operationEndTime = 30 * 60 * 60; //30h
		Random random = MatsimRandom.getRandom();
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
//		new MatsimNetworkReader(this.config.network());
		final int[] i = {0};
		Stream<DvrpVehicleSpecification> vehicleSpecificationStream = scenario.getNetwork().getLinks().entrySet().stream()
				.filter(entry -> entry.getValue().getAllowedModes().contains(this.allowedLinkMode)) // drt can only start on links with Transport mode 'car'
				.sorted((e1, e2) -> (random.nextInt(2) - 1)) // shuffle links
				.limit(this.numOfVehicles) // select the first *numberOfVehicles* links
				.map(entry -> ImmutableDvrpVehicleSpecification.newBuilder()
						.id(Id.create("drt_" + i[0]++, DvrpVehicle.class))
						.startLinkId(entry.getKey())
						.capacity(seatsPerVehicle)
						.serviceBeginTime(operationStartTime)
						.serviceEndTime(operationEndTime)
						.build());
		
		new FleetWriter(vehicleSpecificationStream).write(scenarioDir.resolve("vehicles.xml").toString());
		return scenarioDir.resolve("vehicles.xml").toString();
	}

	/**
	 * Set the controler params in the configuration, mainly number of iteration,
	 * overrideExistingFiles and writing intervals. setting output dir as
	 * {@value #OUTPUT_DIR}
	 * 
	 * @param overrideExistingOutput whether to override existing output files in
	 *                               the out put directory
	 */
	private void setControlerParams(boolean overrideExistingOutput) {
		final Config config = this.config;
		config.controler().setWriteEventsInterval(1);
		config.controler().setWritePlansInterval(1);
		config.controler().setFirstIteration(1);
		if (overrideExistingOutput)
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		config.controler().setLastIteration(this.numOfIterations);
		config.controler().setMobsim("qsim");
		config.controler().setRoutingAlgorithmType(RoutingAlgorithmType.FastAStarLandmarks);
		String outputDir = this.scenarioDir.resolve(OUTPUT_DIR).toString();
		config.controler().setOutputDirectory(outputDir);
	}

	private void setQSimParams() {
		final QSimConfigGroup qsim = this.config.qsim();
		qsim.setStartTime(0);
		qsim.setEndTime(30 * 3600);
		qsim.setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);
		if (this.isAVDispatcher) {
			qsim.setNumberOfThreads(4);
		} else {
			qsim.setNumberOfThreads(1);
		}
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

	/**
	 * Set the plan calc score config group in this scenario config. Adds an AV mode
	 * and sets time parameters for to activities - (work and home) general utility
	 * parameters are copied from the Jerusalem Scenario.
	 * 
	 */
	private void setPlanCalcScoreParams() {
		final Config config = this.config;

		PlanCalcScoreConfigGroup.ModeParams avCalcScoreParams = new PlanCalcScoreConfigGroup.ModeParams(
				AVModule.AV_MODE);
		avCalcScoreParams.setMode(AVModule.AV_MODE);
		avCalcScoreParams.setMarginalUtilityOfTraveling(-6.0);
		config.planCalcScore().addModeParams(avCalcScoreParams);

		ActivityParams home = new ActivityParams("home");
		home.setTypicalDuration(16 * 60 * 60);
		config.planCalcScore().addActivityParams(home);

		ActivityParams work = new ActivityParams("work");
		work.setOpeningTime(6 * 3600);
		work.setClosingTime(20 * 3600);
		work.setTypicalDuration(8 * 60 * 60);
		config.planCalcScore().addActivityParams(work);
	}

	////////////////////////////////////////////////////////////
	// Setter and Getters
	//////////////////////////////////////////////////////////

	public int getNumOfVehicles() {
		return numOfVehicles;
	}

	public void setNumOfVehicles(int numOfVehicles) {
		this.numOfVehicles = numOfVehicles;
		if (this.vehicleConfig != null) {
			if (this.isAVDispatcher) {
				GeneratorConfig generator = getOperatorConfig().getGeneratorConfig();
				generator.setNumberOfVehicles(numOfVehicles);
			} else {
				createVehiclesFile();
			}
		}
	}

	public int getNumOfIterations() {
		return numOfIterations;
	}

	public void setNumOfIterations(int numOfIterations) {
		this.numOfIterations = numOfIterations;
		this.config.controler().setLastIteration(numOfIterations);
	}

	public int getDispatchPeriod() {
		return dispatchPeriod;
	}

	public void setDispatchPeriod(int dispatchPeriod) {
		this.dispatchPeriod = dispatchPeriod;
		if (this.vehicleConfig != null) {
			OperatorConfig operator = getOperatorConfig();
			operator.getDispatcherConfig().addParam("dispatchPeriod", String.valueOf(this.dispatchPeriod));
		}
	}

	public String getDispatcherAlgorithm() {
		return dispatcherAlgorithm;
	}

	public void setDispatcherAlgorithm(String dispatcherAlgorithm) {
		if (!Arrays.asList(DISPATCHING_ALGORITHMS).contains(dispatcherAlgorithm)) {
			log.warn("The specified dispatching algorithm is unknown, which might cause problems later. "
					+ "The known algorithms are: " + Arrays.deepToString(DISPATCHING_ALGORITHMS));
		}
		this.dispatcherAlgorithm = dispatcherAlgorithm;
		if (this.vehicleConfig != null) {
			OperatorConfig operator = getOperatorConfig();
			operator.getDispatcherConfig().setType(dispatcherAlgorithm);
		}
	}

	public int getPopSize() {
		return popSize;
	}

	public void setPopSize(int popSize) throws IOException {
		this.popSize = popSize;
		this.addPopulationIfMissing(true);
	}

	public int getNumOfStreets() {
		return numOfStreets;
	}

	public int getNumOfAvenues() {
		return numOfAvenues;
	}

	public void setNetworkSize(int numOfStreets, int numOfAvenues) throws IOException {
		this.numOfStreets = numOfStreets;
		this.numOfAvenues = numOfAvenues;
		this.addNetworkIfMissing(true);
	}
	
	public String getConfigPath() {
		return this.scenarioDir.resolve(SCENARIO_CONFIG_FILENAME).toString();
	}

	private OperatorConfig getOperatorConfig() {
		if (this.isAVDispatcher)
			return ((AVConfigGroup) vehicleConfig).getOperatorConfigs().values().iterator().next();
		return null;
	}
}
