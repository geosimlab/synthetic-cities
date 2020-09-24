package syncity.scenarios;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingParams;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.FleetWriter;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import utils.Structs.TripTimeArguments;

public class DrtScenarioCreator extends BaseScenarioCreator {

    private static final Logger log = Logger
	    .getLogger(DrtScenarioCreator.class);

    private static final String[] SCENARIO_TEMPLATE_FILES = null;
    private static final String[] DISPATCHING_ALGORITHMS = { "DRT" };

    private boolean rebalance = true;
    private boolean enableRejection = true;
    private DrtConfigGroup drtConfig;

    public DrtScenarioCreator(Config baseConfig, String scenarioDirPath,
	    int popSize, int numOfStreets, int numOfAvenues, int numOfVehicles,
	    int numOfIterations, String dispatcherAlgorithm,
	    TripTimeArguments timeParameters) throws IOException {
	super(baseConfig, scenarioDirPath, popSize, numOfStreets, numOfAvenues,
		numOfVehicles, numOfIterations, dispatcherAlgorithm,
		timeParameters);
    }

    public DrtScenarioCreator(Config baseConfig, String scenarioDirPath,
	    String populationPath, String networkPath, int numOfVehicles,
	    int numOfIterations, String dispatcherAlgorithm,
	    TripTimeArguments timeParameters) throws IOException {
	super(baseConfig, scenarioDirPath, populationPath, networkPath,
		numOfVehicles, numOfIterations, dispatcherAlgorithm,
		timeParameters);
    }

    protected String[] getScenarioTemplateFiles() {
	return SCENARIO_TEMPLATE_FILES;
    }

    public static String[] getDispatchigAlgorithms() {
	return Arrays.copyOf(DISPATCHING_ALGORITHMS,
		DISPATCHING_ALGORITHMS.length);
    }

    protected int getNumberOfThreads() {
	return 1;
    }

    @Override
    public void setNumOfVehicles(int numOfVehicles) {
	this.numOfVehicles = numOfVehicles;
	String vehiclesFile = createVehiclesFile();
	this.drtConfig.setVehiclesFile(vehiclesFile);
    }

    private String createVehiclesFile() {
	int seatsPerVehicle = 4; // this is important for DRT, value is not used
				 // by taxi
	double operationStartTime = getConfig().qsim().getStartTime();
	;
	double operationEndTime = getConfig().qsim().getEndTime();
	Random random = MatsimRandom.getRandom();

	Scenario scenario = ScenarioUtils.loadScenario(config);
	final int[] i = { 0 };
	final String allowedMode = BaseScenarioCreator.ALLOWED_LINK_MODE;
	Stream<DvrpVehicleSpecification> vehicleSpecificationStream = scenario
		.getNetwork().getLinks().entrySet().stream()
		.filter(entry -> entry.getValue().getAllowedModes()
			.contains(allowedMode)) // drt can only start on links
						// with Transport mode 'car'
		.sorted((e1, e2) -> (random.nextInt(2) - 1)) // shuffle links
		.limit(this.numOfVehicles) // select the first
					   // *numberOfVehicles* links
		.map(entry -> ImmutableDvrpVehicleSpecification.newBuilder()
			.id(Id.create("drt_" + i[0]++, DvrpVehicle.class))
			.startLinkId(entry.getKey()).capacity(seatsPerVehicle)
			.serviceBeginTime(operationStartTime)
			.serviceEndTime(operationEndTime).build());
	final String filename = "vehicles-" + getNumOfVehicles() + ".xml";
	final String outpath = scenarioDir.resolve(filename).toString();
	new FleetWriter(vehicleSpecificationStream).write(outpath);
	return outpath;
    }

    @Override
    protected void addDispatcherConfigGroup() {
	DrtConfigGroup drtConfigGroup = new DrtConfigGroup();
	// We use av so we'll be able to use the same plans for DRT and AV
	drtConfigGroup.setMode(BaseScenarioCreator.LEG_MODE);
//		drtConfigGroup.setOperationalScheme(OperationalScheme.door2door.toString());;
	drtConfigGroup.setMaxTravelTimeAlpha(timeArguments.alpha);
	drtConfigGroup.setMaxTravelTimeBeta(timeArguments.beta);
	drtConfigGroup.setMaxWaitTime(timeArguments.maxWaitTime);
	drtConfigGroup.setStopDuration(timeArguments.stopTime);
	drtConfigGroup.setRequestRejection(this.enableRejection);
	String vehiclesFile = createVehiclesFile();
	drtConfigGroup.setVehiclesFile(vehiclesFile);

	if (this.rebalance) {
	    MinCostFlowRebalancingParams rebalance = new MinCostFlowRebalancingParams();
	    rebalance.setTargetAlpha(0.5);
	    rebalance.setTargetBeta(0.5);
	    rebalance.setInterval(1800);
	    rebalance.setCellSize(2000);
	    drtConfigGroup.addParameterSet(rebalance);
	}

	config.addModule(drtConfigGroup);
	DvrpConfigGroup dvrp = new DvrpConfigGroup();
	config.addModule(dvrp);
	OTFVisConfigGroup otfvis = new OTFVisConfigGroup();
	config.addModule(otfvis);

	this.drtConfig = drtConfigGroup;
    }

    @Override
    public void prepare() throws Exception {
	setAllConfigParams();
	writeScenarioFiles();
    }

    @Override
    public void run() throws Exception {
	Controler controler = DrtControlerCreator
		.createControlerWithSingleModeDrt(getConfig(), false);
	controler.run();
    }

    @Override
    public void setDispatcherAlgorithm(String dispatcherAlgorithm) {
	log.warn(
		"Only DRT algorithm is supported. This field has no effect on the simulation itself.");
	this.dispatcherAlgorithm = dispatcherAlgorithm;
    }

}
