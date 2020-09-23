package utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.math3.random.JDKRandomGenerator;
import org.matsim.api.core.v01.network.Node;

public class BasicUtils {

    protected final static int CONSTANT_SEED = 504000;

    protected static JDKRandomGenerator uniformSampler = null;

    public static JDKRandomGenerator getUniformRandomGenerator() {
	if (uniformSampler == null) {
	    uniformSampler = new JDKRandomGenerator(CONSTANT_SEED);
	}
	return uniformSampler;
    }

    /**
     * return a random number between base and (base + window)
     * 
     * @param base   the base size
     * @param window the size of the random window
     * @return a random number within the window
     */
    public static double randInWindow(double base, double window) {
	return base + (window * getUniformRandomGenerator().nextDouble());
    }

    /**
     * return a random number between (base - delta) and (base + delta)
     * 
     * @param base  the base size
     * @param delta the size of the random window
     * @return a random number within the window
     */
    public static double randAroundBase(double base, double delta) {
	return randInWindow(base - delta, delta * 2);
    }

    public static <T> T chooseRand(T[] arr) {
	int index = getUniformRandomGenerator().nextInt(arr.length);
	return arr[index];
    }

    public static double nodesDistance(Node nodeA, Node nodeB) {
	double xDistance = nodeA.getCoord().getX() - nodeB.getCoord().getX();
	double yDistance = nodeA.getCoord().getY() - nodeB.getCoord().getY();
	double distance = Math.hypot(xDistance, yDistance);
	return distance;
    }

    public static <T extends Comparable<T>> boolean arrayContains(T[] arr,
	    T obj) {
	for (T element : arr) {
	    if (element.equals(obj))
		return true;
	}
	return false;
    }

    public static <K, T> void  writeMapOfLists(Map<K, List<T>> map,
	    String filename, List<String> headers) throws IOException {
	BufferedWriter writer = new BufferedWriter(
		new FileWriter(new File(filename)));
	writer.write(String.join(";", headers) + "\n");
	for (Entry<K, List<T>> entry : map.entrySet()) {
	    String key = "\"" + entry.getKey().toString() + "\"";
	    List<String> stats = entry.getValue().stream() //
		    .map(s -> s.toString()).collect(Collectors.toList());

	    assert stats.size() + 1 == headers.size();
	    String line = key + ";" + String.join(";", stats) + "\n";
	    writer.write(line);
	}
	writer.close();
	System.out.println("Wrote Map as lists csv to:  " + filename);
    }

    public static <K, V> void writeSimpleMap(Map<K, V> map, String filename,
	    List<String> headers) throws IOException {
	BufferedWriter writer = new BufferedWriter(
		new FileWriter(new File(filename)));
	writer.write(String.join(";", headers) + "\n");
	for (Entry<K, V> entry : map.entrySet()) {
	    String key = "\"" + entry.getKey().toString() + "\"";
	    String stats = entry.getValue().toString();

	    assert 2 == headers.size();
	    String line = key + ";" + stats + "\n";
	    writer.write(line);
	}
	writer.close();
	System.out.println("Wrote Map as csv to:  " + filename);
    }

}
