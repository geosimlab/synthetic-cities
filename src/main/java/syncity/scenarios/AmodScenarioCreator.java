package syncity.scenarios;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

import org.apache.log4j.Logger;
import org.matsim.amodeus.AmodeusConfigurator;
import org.matsim.amodeus.config.AmodeusConfigGroup;
import org.matsim.amodeus.config.AmodeusModeConfig;
import org.matsim.amodeus.config.modal.GeneratorConfig;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import amodeus.amod.analysis.CustomAnalysis;
import amodeus.amod.ext.Static;
import amodeus.amodeus.analysis.Analysis;
import amodeus.amodeus.data.LocationSpec;
import amodeus.amodeus.data.ReferenceFrame;
import amodeus.amodeus.net.MatsimAmodeusDatabase;
import amodeus.amodeus.net.SimulationServer;
import amodeus.amodeus.options.ScenarioOptions;
import amodeus.amodeus.options.ScenarioOptionsBase;
import amodeus.amodeus.prep.VirtualNetworkPreparer;
import amodeus.amodeus.util.math.GlobalAssert;
import amodeus.amodeus.util.matsim.AddCoordinatesToActivities;

public class AmodScenarioCreator extends BaseScenarioCreator{
	
	private static final Logger log = Logger.getLogger(AmodScenarioCreator.class);
	
	private static final String[] SCENARIO_TEMPLATE_FILES = { "LPOptions.properties", "AmodeusOptions.properties" };
	private static final String[] DISPATCHING_ALGORITHMS = { "TShareDispatcher", "ExtDemandSupplyBeamSharing",
			"DynamicRideSharingStrategy", "HighCapacityDispatcher" };

	private AmodeusConfigGroup avConfig;
	private int dispatchPeriod = 10;

	public AmodScenarioCreator(Config baseConfig, String scenarioDirPath, int popSize, int numOfStreets,
			int numOfAvenues, int numOfVehicles, int numOfIterations, String dispatcherAlgorithm, int dispatchPeriod) {
		super(baseConfig, scenarioDirPath, popSize, numOfStreets, numOfAvenues, numOfVehicles, numOfIterations, dispatcherAlgorithm);
		this.dispatchPeriod = dispatchPeriod;
	}
	
	protected String[] getScenarioTemplateFiles() {
		return SCENARIO_TEMPLATE_FILES;
	}
	
	public static String[] getDispatchigAlgorithms() {
		return Arrays.copyOf(DISPATCHING_ALGORITHMS, DISPATCHING_ALGORITHMS.length);
	}

	@Override
	protected void addDispatcherConfigGroup() {
		AmodeusConfigGroup avConfigGroup = new AmodeusConfigGroup();
		avConfigGroup.createParameterSet(AmodeusModeConfig.GROUP_NAME);

		AmodeusModeConfig modeOperatorConfig = (AmodeusModeConfig) avConfigGroup.createParameterSet(AmodeusModeConfig.GROUP_NAME);
		modeOperatorConfig.setMode(BaseScenarioCreator.LEG_MODE);
		modeOperatorConfig.setUseModeFilteredSubnetwork(false);
		modeOperatorConfig.setPredictRouteTravelTime(true);
		// Dispatcher
		modeOperatorConfig.getDispatcherConfig().setType(this.dispatcherAlgorithm);
		// TODO add rebalancePeriod as in DynamicRideSharingStrategy
		modeOperatorConfig.getDispatcherConfig().addParam("dispatchPeriod", String.valueOf(this.dispatchPeriod));
		// Vehicle Generator
		modeOperatorConfig.getGeneratorConfig().setNumberOfVehicles(this.numOfVehicles);

		avConfigGroup.addParameterSet(modeOperatorConfig);
		this.config.addModule(avConfigGroup);
		this.avConfig = avConfigGroup;
	}

	@Override
	public void setNumOfVehicles(int numOfVehicles) {
		this.numOfVehicles = numOfVehicles;
		if (this.avConfig != null) {
				GeneratorConfig generator = getModeOperatorConfig().getGeneratorConfig();
				generator.setNumberOfVehicles(numOfVehicles);
		}
	}
	
	private AmodeusModeConfig getModeOperatorConfig() {
		return avConfig.getModes().values().iterator().next();
	}

	public void setDispatchPeriod(int dispatchPeriod) {
		this.dispatchPeriod = dispatchPeriod;
		if (this.avConfig != null) {
			AmodeusModeConfig modeOperator = getModeOperatorConfig();
			modeOperator.getDispatcherConfig().addParam("dispatchPeriod", String.valueOf(this.dispatchPeriod));
		}
		
	}

	@Override
	public void setDispatcherAlgorithm(String dispatcherAlgorithm) {
		if (!Arrays.asList(DISPATCHING_ALGORITHMS).contains(dispatcherAlgorithm)) {
			log.warn("The specified dispatching algorithm is unknown, which might cause problems later. "
					+ "The known algorithms are: " + Arrays.deepToString(DISPATCHING_ALGORITHMS));
		}
		this.dispatcherAlgorithm = dispatcherAlgorithm;
		if (this.avConfig != null) {
			AmodeusModeConfig modeOperator = getModeOperatorConfig();
			modeOperator.getDispatcherConfig().setType(dispatcherAlgorithm);
		}
		
	}
	
	@Override
	protected int getNumberOfThreads() {
		return 4;
	}
	
	/**
	 * Preparing the Virtual network and amodeus "prepared config"
	 * This is mainly copied from {amodeus.amod.ScenarioPreparer} since it's visibility was reduced
	 * 
	 * @throws IOException 
	 * 
	 */
	protected void amodPreparer() throws IOException {
		File workingDirectory = getScenarioDir().toFile();
		ScenarioOptions scenarioOptions = new ScenarioOptions(workingDirectory, ScenarioOptionsBase.getDefault());
        Static.setLPtoNone(workingDirectory);
		/** creating a virtual network, e.g., for operational policies requiring a graph
         * structure on the city */
        int endTime = (int) config.qsim().getEndTime().seconds();
        Scenario scene = ScenarioUtils.loadScenario(getConfig());
        VirtualNetworkPreparer.INSTANCE.create(scene.getNetwork(), scene.getPopulation(), scenarioOptions, numOfVehicles, endTime); //

	}

	@Override
	public void prepare() throws IOException {
		setAllConfigParams();
		writeScenarioFiles();
		amodPreparer();
	}

	@Override
	public void run() throws Exception {
		Static.setup();
        System.out.println("\n\n\n" + Static.glpInfo() + "\n\n\n");
        
        /** working directory and options */
        File workingDirectory = getScenarioDir().toFile();
        ScenarioOptions scenarioOptions = new ScenarioOptions(workingDirectory, ScenarioOptionsBase.getDefault());

        /** set to true in order to make server wait for at least 1 client, for
         * instance viewer client, for fals the ScenarioServer starts the simulation
         * immediately */
        boolean waitForClients = scenarioOptions.getBoolean("waitForClients");
        File configFile = new File(scenarioOptions.getSimulationConfigName());

        /** geographic information */
        LocationSpec locationSpec = scenarioOptions.getLocationSpec();
        ReferenceFrame referenceFrame = locationSpec.referenceFrame();

        /** open server port for clients to connect to */
        SimulationServer.INSTANCE.startAcceptingNonBlocking();
        SimulationServer.INSTANCE.setWaitForClients(waitForClients);

        /** load MATSim configs - including av.xml configurations, load routing packages */
        System.out.println(configFile);
        GlobalAssert.that(configFile.exists());
        DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
        dvrpConfigGroup.setTravelTimeEstimationAlpha(0.05);
        Config config = ConfigUtils.loadConfig(configFile.toString(), new AmodeusConfigGroup(), dvrpConfigGroup);

        /** output directory for saving results */
        String outputdirectory = config.controler().getOutputDirectory();

        /** load MATSim scenario for simulation */
        Scenario scenario = ScenarioUtils.loadScenario(config);
        AddCoordinatesToActivities.run(scenario);
        Network network = scenario.getNetwork();
        Population population = scenario.getPopulation();
        GlobalAssert.that(Objects.nonNull(network));
        GlobalAssert.that(Objects.nonNull(population));

        MatsimAmodeusDatabase db = MatsimAmodeusDatabase.initialize(network, referenceFrame);
        Controler controller = new Controler(scenario);
        AmodeusConfigurator.configureController(controller, db, scenarioOptions);

        /** run simulation */
        controller.run();

        /** close port for visualizaiton */
        SimulationServer.INSTANCE.stopAccepting();

        /** perform analysis of simulation, a demo of how to add custom analysis methods
         * is provided in the package amod.demo.analysis */
        Analysis analysis = Analysis.setup(scenarioOptions, new File(outputdirectory), network, db);
        CustomAnalysis.addTo(analysis);
        analysis.run();
		
	}

}
