package syncity.scenarios;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;

import utils.BasicUtils;
import utils.MatsimUtils;
import utils.Structs.DispatcherArguments;

public class ScenarioFactory {

    public static BaseScenarioCreator getScenario(String workdir,
	    String algorithm, int popSize, int numOfSt, int numOfAv,
	    int vehiclesNum, int iterations, DispatcherArguments dispatcherParams)
	    throws IOException {

	if (BasicUtils.arrayContains(
		DrtScenarioCreator.getDispatchigAlgorithms(), algorithm)) {
	    return new DrtScenarioCreator(null, workdir, popSize,
		    numOfSt, numOfAv, iterations, algorithm,
		    dispatcherParams);
	} else if (BasicUtils.arrayContains(
		AmodScenarioCreator.getDispatchigAlgorithms(), algorithm)) {
	    return new AmodScenarioCreator(null, workdir,
		    popSize, numOfSt, numOfAv, iterations,
		    algorithm, dispatcherParams);
	}
	throw new RuntimeException(
		"No matchig creator found for algorithm - " + algorithm);

    }

    public static BaseScenarioCreator getScenario(String workdir,
	    String algorithm, Population population, Network network,
	    int vehiclesNum, int iterations, DispatcherArguments dispatcherParams)
	    throws IOException {

	Path dirPath = Paths.get(workdir);
	String netPath = MatsimUtils.writePopulation(population, dirPath);
	String popPath = MatsimUtils.writeNetwork(network, dirPath);
	return getScenario(workdir, algorithm, popPath, netPath,
		iterations, dispatcherParams);
    }

    public static BaseScenarioCreator getScenario(String workdir,
	    String algorithm, String populationFile, String networkFile,
	    int iterations, DispatcherArguments dispatcherParams)
	    throws IOException {

	Path dirPath = Paths.get(workdir);
	Path popPath = BasicUtils.copyFiletoDir(Paths.get(populationFile),
		dirPath);
	Path netPath = BasicUtils.copyFiletoDir(Paths.get(networkFile),
		dirPath);

	if (BasicUtils.arrayContains(
		DrtScenarioCreator.getDispatchigAlgorithms(), algorithm)) {
	    return new DrtScenarioCreator(null, workdir,
		    popPath.toString(), netPath.toString(),
		    iterations, algorithm, dispatcherParams);
	} else if (BasicUtils.arrayContains(
		AmodScenarioCreator.getDispatchigAlgorithms(), algorithm)) {
	    return new AmodScenarioCreator(null, workdir,
		    popPath.toString(), netPath.toString(),
		    iterations, algorithm, dispatcherParams);
	}
	throw new RuntimeException(
		"No matchig creator found for algorithm - " + algorithm);
    }

}
