package syncity.scenarios;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;

import utils.BasicUtils;
import utils.MatsimUtils;
import utils.Structs.TripTimeArguments;

public class ScenarioFactory {

    public static BaseScenarioCreator getScenario(Path workdir,
	    String algorithm, int popSize, int numOfSt, int numOfAv,
	    int vehiclesNum, int iterations, TripTimeArguments timeParameters)
	    throws IOException {

	Path algoDirPath = workdir.resolve(algorithm);
	if (!algoDirPath.toFile().exists())
	    Files.createDirectories(algoDirPath);

	if (BasicUtils.arrayContains(
		DrtScenarioCreator.getDispatchigAlgorithms(), algorithm)) {
	    return new DrtScenarioCreator(null, algoDirPath.toString(), popSize,
		    numOfSt, numOfAv, vehiclesNum, iterations, algorithm,
		    timeParameters);
	} else if (BasicUtils.arrayContains(
		AmodScenarioCreator.getDispatchigAlgorithms(), algorithm)) {
	    return new AmodScenarioCreator(null, algoDirPath.toString(),
		    popSize, numOfSt, numOfAv, vehiclesNum, iterations,
		    algorithm, timeParameters);
	}
	throw new RuntimeException(
		"No matchig creator found for algorithm - " + algorithm);

    }

    public static BaseScenarioCreator getScenario(Path workdir,
	    String algorithm, Population population, Network network,
	    int vehiclesNum, int iterations, TripTimeArguments timeParameters)
	    throws IOException {
	Path algoDirPath = workdir.resolve(algorithm);
	if (!algoDirPath.toFile().exists()) {
	    Files.createDirectories(algoDirPath);
	}
	String netPath = MatsimUtils.writePopulation(population, algoDirPath);
	String popPath = MatsimUtils.writeNetwork(network, algoDirPath);
	return getScenario(workdir, algorithm, popPath, netPath, vehiclesNum,
		iterations, timeParameters);
    }

    public static BaseScenarioCreator getScenario(Path workdir,
	    String algorithm, String populationFile, String networkFile,
	    int vehiclesNum, int iterations, TripTimeArguments timeParameters)
	    throws IOException {

	Path algoDirPath = workdir.resolve(algorithm);
	if (!algoDirPath.toFile().exists()) {
	    Files.createDirectories(algoDirPath);
	}

	Path popPath = BasicUtils.copyFiletoDir(Paths.get(populationFile),
		algoDirPath);
	Path netPath = BasicUtils.copyFiletoDir(Paths.get(networkFile),
		algoDirPath);

	if (BasicUtils.arrayContains(
		DrtScenarioCreator.getDispatchigAlgorithms(), algorithm)) {
	    return new DrtScenarioCreator(null, algoDirPath.toString(),
		    popPath.toString(), netPath.toString(), vehiclesNum,
		    iterations, algorithm, timeParameters);
	} else if (BasicUtils.arrayContains(
		AmodScenarioCreator.getDispatchigAlgorithms(), algorithm)) {
	    return new AmodScenarioCreator(null, algoDirPath.toString(),
		    popPath.toString(), netPath.toString(), vehiclesNum,
		    iterations, algorithm, timeParameters);
	}
	throw new RuntimeException(
		"No matchig creator found for algorithm - " + algorithm);
    }

}
