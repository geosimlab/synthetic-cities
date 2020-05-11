package syncity.scenarios;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;

import ch.ethz.matsim.av.framework.AVModule;
import syncity.GridNetworkGenerator;
import syncity.RandomPopulationGenerator;

public abstract class BaseScenarioCreator {
	
	private static final Logger log = Logger.getLogger(BaseScenarioCreator.class);

	protected static final String SCENARIO_CONFIG_FILENAME = "generated_config.xml";
	protected static final String OUTPUT_DIR = "output";
	protected static final String SCENARIO_BASE_DIR = "ScenarioBaseFiles/";
	protected static final String ALLOWED_LINK_MODE = TransportMode.car;
	protected static final String LEG_MODE = AVModule.AV_MODE;
	
	protected abstract String[] getScenarioTemplateFiles();
	protected abstract void addDispatcherConfigGroup();
	public abstract void prepare() throws Exception;
	public abstract void run() throws Exception;
	
	protected Path scenarioDir;
	protected Config config;
	
	protected int popSize;
	protected int numOfStreets;
	protected int numOfAvenues;
	protected int numOfVehicles;
	protected int numOfIterations;
	protected String dispatcherAlgorithm;
	
	public BaseScenarioCreator(Config baseConfig, String scenarioDirPath, int popSize, int numOfStreets,
			int numOfAvenues, int numOfVehicles, int numOfIterations, String dispatcherAlgorithm) {
		this.scenarioDir = Paths.get(scenarioDirPath);
		this.dispatcherAlgorithm = dispatcherAlgorithm;
		this.popSize = popSize;
		this.numOfAvenues = numOfAvenues;
		this.numOfStreets = numOfStreets;
		this.numOfVehicles = numOfVehicles;
		this.numOfIterations = numOfIterations;
		if (baseConfig == null) {
			this.config = ConfigUtils.createConfig(getScenarioDir().toString());
		} else {
			this.config = baseConfig;
		}
	}
	
	public static String[] getDispatchigAlgorithms() {
		return null;
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
		this.addDispatcherConfigGroup();
		this.setPlanSelectionParams();
		this.setPlanCalcScoreParams();
		this.setControlerParams(true);

		this.checkConsistency();
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
		Config config = getConfig();
		Path scenarioDir = getScenarioDir();
		ConfigUtils.writeConfig(config, scenarioDir.resolve(SCENARIO_CONFIG_FILENAME).toString());
		copyScenarioResources(scenarioDir);

	}
	
	
	/**
	 * Copy the ScenarioBaseFiles to destDir
	 * 
	 * @param destDir the directory to write the files into
	 * @throws IOException
	 */
	private void copyScenarioResources(Path destDir) throws IOException {
		if (getScenarioTemplateFiles() == null) return;
		
		ClassLoader classLoader = ClassLoader.getSystemClassLoader();

		for (String resource : getScenarioTemplateFiles()) {
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
		if (getConfig().network().getInputFile() == null || force) {
			GridNetworkGenerator grid = new GridNetworkGenerator(getNumOfStreets(), getNumOfAvenues());
			grid.generateGridNetwork();
			String netwokFile = grid.writeNetwork(getScenarioDir().toString());
			log.info("network file is: " + netwokFile);
			Path relativeNetworkFile = Paths.get(netwokFile).getFileName();
			filename = relativeNetworkFile.toString();
			newNetwork = true;
		} else {
			log.info("Network file is already specified in the config, copying it to the scenario dir");
			String curFilename = getConfig().network().getInputFile();
			Path srcPath = getConfigPath().getParent().resolve(curFilename);
			Path destPath = copyFileToDir(srcPath, getScenarioDir());
			filename = destPath.getFileName().toString();
			newNetwork = false;
		}
		getConfig().network().setInputFile(filename);
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
		if (getConfig().plans().getInputFile() == null || force) {
			RandomPopulationGenerator popGen = new RandomPopulationGenerator(this.getConfig(), getPopSize());
			popGen.populateNodes();
			String plansFile = popGen.writePopulation(this.getScenarioDir().toString());
			filename = Paths.get(plansFile).getFileName().toString();
			newPopulation = true;
		} else {
			log.info("Plans (Population) file is already specified in the config, copying it to the scenario dir");
			String curFilename = this.getConfig().plans().getInputFile();
			Path srcPath = this.getConfigPath().getParent().resolve(curFilename);
			Path destPath = copyFileToDir(srcPath, getScenarioDir());
			filename = destPath.getFileName().toString();
			newPopulation = false;
		}
		getConfig().plans().setInputFile(filename);
		return newPopulation;
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
		final Config config = getConfig();
		config.controler().setWriteEventsInterval(1);
		config.controler().setWritePlansInterval(1);
		config.controler().setFirstIteration(1);
		if (overrideExistingOutput)
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		config.controler().setLastIteration(getNumOfIterations());
		config.controler().setMobsim("qsim");
		config.controler().setRoutingAlgorithmType(RoutingAlgorithmType.FastAStarLandmarks);
		String outputDir = getScenarioDir().resolve(OUTPUT_DIR).toString();
		config.controler().setOutputDirectory(outputDir);
	}
	
	private void setPlanSelectionParams() {
		// Add strategy - plan selector
		StrategySettings changeExpStrategy = new StrategySettings();
		changeExpStrategy.setDisableAfter(-1);
		changeExpStrategy.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
		changeExpStrategy.setWeight(0.8);
		this.getConfig().strategy().addStrategySettings(changeExpStrategy);

		// Add strategy - time-mutation
		StrategySettings timeMutatorStrategy = new StrategySettings();
		timeMutatorStrategy
				.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator.toString());
		timeMutatorStrategy.setWeight(0.1);
		this.getConfig().strategy().addStrategySettings(timeMutatorStrategy);
	}
	
	/**
	 * Set the plan calc score config group in this scenario config. Adds an AV mode
	 * and sets time parameters for to activities - (work and home) general utility
	 * parameters are copied from the Jerusalem Scenario.
	 * 
	 */
	private void setPlanCalcScoreParams() {
		final Config config = this.getConfig();

		PlanCalcScoreConfigGroup.ModeParams avCalcScoreParams = new PlanCalcScoreConfigGroup.ModeParams(
				LEG_MODE);
		avCalcScoreParams.setMode(LEG_MODE);
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
	
	protected void setQSimParams() {
		final QSimConfigGroup qsim = this.getConfig().qsim();
		qsim.setStartTime(0);
		qsim.setEndTime(30 * 3600);
		qsim.setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);
		qsim.setNumberOfThreads(getNumberOfThreads());
	}
	
	protected void checkConsistency() {
		getConfig().checkConsistency();
	}
	

	////////////////////////////////////////////////////////////
	// Setter and Getters
	//////////////////////////////////////////////////////////

	protected abstract int getNumberOfThreads();
	
	public int getNumOfVehicles() {
		return numOfVehicles;
	}

	public abstract void setNumOfVehicles(int numOfVehicles);

	public int getNumOfIterations() {
		return numOfIterations;
	}

	public void setNumOfIterations(int numOfIterations) {
		this.numOfIterations = numOfIterations;
		this.config.controler().setLastIteration(numOfIterations);
	}

	public String getDispatcherAlgorithm() {
		return dispatcherAlgorithm;
	}

	public abstract void setDispatcherAlgorithm(String dispatcherAlgorithm);

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
	
	public Path getScenarioDir() {
		return scenarioDir;
	}
	
	public Path getConfigPath() {
		return getScenarioDir().resolve(SCENARIO_CONFIG_FILENAME);
	}
	
	public Config getConfig() {
		return config;
	}
}
