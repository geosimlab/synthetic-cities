package syncity.scenarios;

import java.util.Arrays;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;

import ch.ethz.idsc.amod.ScenarioPreparer;
import ch.ethz.idsc.amod.ScenarioServer;
import ch.ethz.matsim.av.config.AVConfigGroup;
import ch.ethz.matsim.av.config.operator.GeneratorConfig;
import ch.ethz.matsim.av.config.operator.OperatorConfig;

public class AmodScenarioCreator extends BaseScenarioCreator{
	
	private static final Logger log = Logger.getLogger(AmodScenarioCreator.class);
	
	private static final String[] SCENARIO_TEMPLATE_FILES = { "LPOptions.properties", "AmodeusOptions.properties" };
	private static final String[] DISPATCHING_ALGORITHMS = { "TShareDispatcher", "ExtDemandSupplyBeamSharing",
			"DynamicRideSharingStrategy", "HighCapacityDispatcher" };

	private AVConfigGroup avConfig;
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
		AVConfigGroup avConfigGroup = new AVConfigGroup();
		avConfigGroup.setAllowedLinkMode(BaseScenarioCreator.ALLOWED_LINK_MODE);

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
		this.avConfig = avConfigGroup;
	}

	@Override
	public void setNumOfVehicles(int numOfVehicles) {
		this.numOfVehicles = numOfVehicles;
		if (this.avConfig != null) {
				GeneratorConfig generator = getOperatorConfig().getGeneratorConfig();
				generator.setNumberOfVehicles(numOfVehicles);
		}
	}
	
	private OperatorConfig getOperatorConfig() {
		return avConfig.getOperatorConfigs().values().iterator().next();
	}

	public void setDispatchPeriod(int dispatchPeriod) {
		this.dispatchPeriod = dispatchPeriod;
		if (this.avConfig != null) {
			OperatorConfig operator = getOperatorConfig();
			operator.getDispatcherConfig().addParam("dispatchPeriod", String.valueOf(this.dispatchPeriod));
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
			OperatorConfig operator = getOperatorConfig();
			operator.getDispatcherConfig().setType(dispatcherAlgorithm);
		}
		
	}
	
	@Override
	protected int getNumberOfThreads() {
		return 4;
	}

	@Override
	public void prepare() throws Exception{
		setAllConfigParams();
		writeScenarioFiles();
		ScenarioPreparer.run(getScenarioDir().toFile());
	}

	@Override
	public void run() throws Exception {
		ScenarioServer.simulate(getScenarioDir().toFile());
		
	}

}
