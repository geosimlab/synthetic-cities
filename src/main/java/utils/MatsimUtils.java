package utils;

import java.nio.file.Path;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;

public class MatsimUtils {
    /*
     * wraps the MATSim population writer with a standard filename
     */
    public static String writePopulation(Population population, Path outDir) {
	int size = population.getPersons().size();
	String filename = outDir.resolve(String.format("Population_of_%s.xml", size)).toString();
	PopulationUtils.writePopulation(population, filename);
	return filename;
    }
    
    /*
     * wraps the MATSim network writer with a standard filename
     */
    public static String writeNetwork(Network network, Path outDir) {
	int size = network.getNodes().size();
	String filename = outDir.resolve(String.format("Network_of_%s.xml", size)).toString();
	NetworkUtils.writeNetwork(network, filename);
	return filename;
    }

    /*
     * Euclidean distance between two nodes
     */
    public static double nodesDistance(Node nodeA, Node nodeB) {
        return NetworkUtils.getEuclideanDistance(nodeA.getCoord(), nodeB.getCoord());
    }
}
