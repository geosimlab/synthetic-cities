package syncity.scenarios;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import com.google.inject.Key;
import com.google.inject.name.Names;

import ch.ethz.idsc.amodeus.analysis.Analysis;
import ch.ethz.idsc.amodeus.data.ReferenceFrame;
import ch.ethz.idsc.amodeus.linkspeed.LinkSpeedDataContainer;
import ch.ethz.idsc.amodeus.linkspeed.LinkSpeedUtils;
import ch.ethz.idsc.amodeus.linkspeed.TrafficDataModule;
import ch.ethz.idsc.amodeus.matsim.mod.AmodeusDatabaseModule;
import ch.ethz.idsc.amodeus.matsim.mod.AmodeusDispatcherModule;
import ch.ethz.idsc.amodeus.matsim.mod.AmodeusModule;
import ch.ethz.idsc.amodeus.matsim.mod.AmodeusVehicleGeneratorModule;
import ch.ethz.idsc.amodeus.matsim.mod.AmodeusVehicleToVSGeneratorModule;
import ch.ethz.idsc.amodeus.matsim.mod.AmodeusVirtualNetworkModule;
import ch.ethz.idsc.amodeus.matsim.utils.AddCoordinatesToActivities;
import ch.ethz.idsc.amodeus.net.DatabaseModule;
import ch.ethz.idsc.amodeus.net.MatsimAmodeusDatabase;
import ch.ethz.idsc.amodeus.net.SimulationServer;
import ch.ethz.idsc.amodeus.options.ScenarioOptions;
import ch.ethz.idsc.amodeus.options.ScenarioOptionsBase;
import ch.ethz.idsc.amodeus.prep.ConfigCreator;
import ch.ethz.idsc.amodeus.prep.NetworkPreparer;
import ch.ethz.idsc.amodeus.prep.PopulationPreparer;
import ch.ethz.idsc.amodeus.prep.VirtualNetworkPreparer;
import ch.ethz.idsc.amodeus.util.math.GlobalAssert;
import ch.ethz.matsim.av.config.AVConfigGroup;
import ch.ethz.matsim.av.config.operator.GeneratorConfig;
import ch.ethz.matsim.av.config.operator.OperatorConfig;
import ch.ethz.matsim.av.framework.AVModule;
import ch.ethz.matsim.av.framework.AVQSimModule;
import utils.AmodeusReferenceFrame;
import utils.Consts;
import utils.Structs.*;

public class AmodScenarioCreator extends BaseScenarioCreator {

    private static final Logger log = Logger
	    .getLogger(AmodScenarioCreator.class);

    private static final String[] SCENARIO_TEMPLATE_FILES = {
	    "LPOptions.properties", "AmodeusOptions.properties" };
    private static final String[] DISPATCHING_ALGORITHMS = {
	    Consts.AlgorithmsNames.HCRS, Consts.AlgorithmsNames.TShare,
	    Consts.AlgorithmsNames.DRSS, Consts.AlgorithmsNames.ExtDS, };

    private AVConfigGroup avConfig;

    public AmodScenarioCreator(Config baseConfig, String scenarioDirPath,
	    int popSize, int numOfStreets, int numOfAvenues, 
	    int numOfIterations, String dispatcherAlgorithm,
	    DispatcherArguments dispatcherParams) throws IOException {
	super(baseConfig, scenarioDirPath, popSize, numOfStreets, numOfAvenues,
		numOfIterations, dispatcherAlgorithm,
		dispatcherParams);
    }

    public AmodScenarioCreator(Config baseConfig, String scenarioDirPath,
	    String populationPath, String networkPath,
	    int numOfIterations, String dispatcherAlgorithm,
	    DispatcherArguments dispatcherParams) throws IOException {
	super(baseConfig, scenarioDirPath, populationPath, networkPath,
		numOfIterations, dispatcherAlgorithm,
		dispatcherParams);
    }

    protected String[] getScenarioTemplateFiles() {
	return SCENARIO_TEMPLATE_FILES;
    }

    public static String[] getDispatchigAlgorithms() {
	return Arrays.copyOf(DISPATCHING_ALGORITHMS,
		DISPATCHING_ALGORITHMS.length);
    }

    @Override
    protected void addDispatcherConfigGroup() {
	AVConfigGroup avConfigGroup = new AVConfigGroup();
	avConfigGroup.setAllowedLinkMode(BaseScenarioCreator.ALLOWED_LINK_MODE);

	OperatorConfig operator = (OperatorConfig) avConfigGroup
		.createParameterSet(OperatorConfig.GROUP_NAME);
	operator.setPredictRouteTravelTime(true);
	operator.getDispatcherConfig().addParam("SkipConsistencyCheck", "true");
	// Dispatcher
	operator.getDispatcherConfig().setType(this.dispatcherAlgorithm);
	// TODO add rebalancePeriod as in DynamicRideSharingStrategy
	operator.getDispatcherConfig().addParam("dispatchPeriod",
		String.valueOf(timeArguments.dispatchPeriod));
	operator.getDispatcherConfig().addParam("MaxAlphaTravelTime",
		String.valueOf(timeArguments.alpha));
	operator.getDispatcherConfig().addParam("MaxBetaTravelTime",
		String.valueOf(timeArguments.beta));
	operator.getDispatcherConfig().addParam("MaxWaitTime",
		String.valueOf(timeArguments.maxWaitTime));
	operator.getDispatcherConfig().addParam("pickupDurationPerStop",
		String.valueOf(timeArguments.stopTime));
	operator.getDispatcherConfig().addParam("dropoffDurationPerStop",
		String.valueOf(timeArguments.stopTime));
	// Vehicle Generator
	operator.getGeneratorConfig().setNumberOfVehicles(this.numOfVehicles);

	avConfigGroup.addParameterSet(operator);
	this.config.addModule(avConfigGroup);
	this.avConfig = avConfigGroup;
    }

    @Override
    public void setNumOfVehicles(int numOfVehicles) {
	this.numOfVehicles = numOfVehicles;
	if (this.avConfig != null) {
	    GeneratorConfig generator = getOperatorConfig()
		    .getGeneratorConfig();
	    generator.setNumberOfVehicles(numOfVehicles);
	}
    }

    private OperatorConfig getOperatorConfig() {
	return avConfig.getOperatorConfigs().values().iterator().next();
    }

    public void setDispatchPeriod(int dispatchPeriod) {
	this.timeArguments.dispatchPeriod = dispatchPeriod;
	if (this.avConfig != null) {
	    OperatorConfig operator = getOperatorConfig();
	    operator.getDispatcherConfig().addParam("dispatchPeriod",
		    String.valueOf(dispatchPeriod));
	}

    }

    @Override
    public void setDispatcherAlgorithm(String dispatcherAlgorithm) {
	if (!Arrays.asList(DISPATCHING_ALGORITHMS)
		.contains(dispatcherAlgorithm)) {
	    log.warn(
		    "The specified dispatching algorithm is unknown, which might cause problems later. "
			    + "The known algorithms are: "
			    + Arrays.deepToString(DISPATCHING_ALGORITHMS));
	}
	this.dispatcherAlgorithm = dispatcherAlgorithm;
	if (this.avConfig != null) {
	    OperatorConfig operator = getOperatorConfig();
	    operator.getDispatcherConfig().setType(dispatcherAlgorithm);
	}

    }

    @Override
    protected int getNumberOfThreads() {
	return 4;
    }
    
    
    // ******************************
    // * From here the code is mainly copied from amodeus, 
    // * with small changes to allow external usa
    // ******************************

    @Override
    public void prepare() throws Exception {
	setAllConfigParams();
	writeScenarioFiles();
	File workingDirectory = getScenarioDir().toFile();

	/**
	 * The {@link ScenarioOptions} contain amodeus specific options.
	 * Currently there are 3
	 * options files:
	 * - MATSim configurations (config.xml)
	 * - AV package configurations (av.xml)
	 * - AMoDeus configurations (AmodeusOptions.properties).
	 * 
	 * The number of configs is planned to be reduced in subsequent
	 * refactoring steps.
	 */
	ScenarioOptions scenarioOptions = new ScenarioOptions(workingDirectory,
		ScenarioOptionsBase.getDefault());

	/** MATSim config */
	AVConfigGroup avConfigGroup = new AVConfigGroup();
	Config config = ConfigUtils.loadConfig(
		scenarioOptions.getPreparerConfigName(), avConfigGroup);
	Scenario scenario = ScenarioUtils.loadScenario(config);
	GeneratorConfig genConfig = avConfigGroup.getOperatorConfigs().values()
		.iterator().next().getGeneratorConfig();
	int numRt = genConfig.getNumberOfVehicles();
	System.out.println("NumberOfVehicles=" + numRt);

	/** adaption of MATSim network, e.g., radius cutting */
	Network network = scenario.getNetwork();
	network = NetworkPreparer.run(network, scenarioOptions);

	/** adaption of MATSim population, e.g., radius cutting */
	Population population = scenario.getPopulation();
	long apoSeed = 1234;
	PopulationPreparer.run(network, population, scenarioOptions, config,
		apoSeed);

	/**
	 * creating a virtual network, e.g., for operational policies requiring
	 * a graph structure on the city
	 */
	int endTime = (int) config.qsim().getEndTime();
	VirtualNetworkPreparer.INSTANCE.create(network, population,
		scenarioOptions, numRt, endTime); //

	/**
	 * create a simulation MATSim config file linking the created input data
	 */
	ConfigCreator.createSimulationConfigFile(config, scenarioOptions);
    }

    @Override
    public void run() throws Exception {
	File workingDirectory = getScenarioDir().toFile();

	/** working directory and options */
	ScenarioOptions scenarioOptions = new ScenarioOptions(workingDirectory,
		ScenarioOptionsBase.getDefault());

	/**
	 * set to true in order to make server wait for at least 1 client, for
	 * instance viewer client, for fals the ScenarioServer starts the
	 * simulation
	 * immediately
	 */
	boolean waitForClients = scenarioOptions.getBoolean("waitForClients");
	File configFile = new File(scenarioOptions.getSimulationConfigName());

	/** geographic information */
	ReferenceFrame referenceFrame = AmodeusReferenceFrame.IDENTITY;

	/** open server port for clients to connect to */
	SimulationServer.INSTANCE.startAcceptingNonBlocking();
	SimulationServer.INSTANCE.setWaitForClients(waitForClients);

	/**
	 * load MATSim configs - including av.xml configurations, load routing
	 * packages
	 */
	GlobalAssert.that(configFile.exists());
	DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
	dvrpConfigGroup.setTravelTimeEstimationAlpha(0.05);
	Config config = ConfigUtils.loadConfig(configFile.toString(),
		new AVConfigGroup(), dvrpConfigGroup);
	config.planCalcScore()
		.addActivityParams(new ActivityParams("activity"));
	/**
	 * MATSim does not allow the typical duration not to be set, therefore
	 * for scenarios
	 * generated from taxi data such as the "SanFrancisco" scenario, it is
	 * set to 1 hour.
	 */
	for (ActivityParams activityParams : config.planCalcScore()
		.getActivityParams()) {
	    // TODO set typical duration in scenario generation and remove
	    activityParams.setTypicalDuration(3600.0);
	}

	/** output directory for saving results */
	String outputdirectory = config.controler().getOutputDirectory();

	/** load MATSim scenario for simulation */
	Scenario scenario = ScenarioUtils.loadScenario(config);
	AddCoordinatesToActivities.run(scenario);
	Network network = scenario.getNetwork();
	Population population = scenario.getPopulation();
	GlobalAssert.that(Objects.nonNull(network));
	GlobalAssert.that(Objects.nonNull(population));

	MatsimAmodeusDatabase db = MatsimAmodeusDatabase.initialize(network,
		referenceFrame);
	Controler controler = new Controler(scenario);
	controler.addOverridingModule(new DvrpTravelTimeModule());

	try {
	    // load linkSpeedData if possible
	    File linkSpeedDataFile = new File(
		    scenarioOptions.getLinkSpeedDataName());
	    System.out.println(linkSpeedDataFile.toString());
	    LinkSpeedDataContainer lsData = LinkSpeedUtils
		    .loadLinkSpeedData(linkSpeedDataFile);
	    controler.addOverridingQSimModule(new TrafficDataModule(lsData));
	} catch (Exception exception) {
	    System.err.println(
		    "Could not load static linkspeed data, running with freespeeds.");
	}

	controler.addOverridingModule(new DvrpModule());
	controler.addOverridingModule(new DvrpTravelTimeModule());
	controler.addOverridingModule(new AVModule(false));
	controler.addOverridingModule(new DatabaseModule());
	controler.addOverridingModule(new AmodeusVehicleGeneratorModule());
	controler.addOverridingModule(new AmodeusDispatcherModule());
	controler.addOverridingModule(
		new AmodeusVirtualNetworkModule(scenarioOptions));
	controler.addOverridingModule(new AmodeusDatabaseModule(db));
	controler.addOverridingModule(new AmodeusVehicleToVSGeneratorModule());
	controler.addOverridingModule(new AmodeusModule());
	controler.addOverridingModule(new AbstractModule() {
	    @Override
	    public void install() {
		bind(Key.get(Network.class, Names.named("dvrp_routing")))
			.to(Network.class);
	    }
	});

	/** run simulation */
	controler.configureQSimComponents(AVQSimModule::configureComponents);
	controler.run();

	/** close port for visualizaiton */
	SimulationServer.INSTANCE.stopAccepting();

	/**
	 * perform analysis of simulation, a demo of how to add custom analysis
	 * methods
	 * is provided in the package amod.demo.analysis
	 */
	Analysis analysis = Analysis.setup(scenarioOptions,
		new File(outputdirectory), network, db);
	analysis.run();

    }

}
