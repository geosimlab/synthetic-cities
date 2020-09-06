package syncity.scenarios;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import utils.BasicUtils;
import utils.Structs.TripTimeArguments;

public class ScenarioFactory {

    public static BaseScenarioCreator getScenario(Path workdir, String algorithm,
	    int popSize, int numOfSt, int numOfAv, int vehiclesNum,
	    int iterations, TripTimeArguments timeParameters)
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

}
