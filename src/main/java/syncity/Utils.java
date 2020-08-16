package syncity;

import org.apache.commons.math3.random.JDKRandomGenerator;
import org.matsim.api.core.v01.network.Node;

public class Utils {
	
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
	public static float randInWindow(float base, float window) {
		return base + (window * getUniformRandomGenerator().nextFloat());
	}
	
	public static double nodesDistance(Node nodeA, Node nodeB) {
		double xDistance = nodeA.getCoord().getX() - nodeB.getCoord().getX(); 
		double yDistance = nodeA.getCoord().getY() - nodeB.getCoord().getY();
		double distance = Math.hypot(xDistance, yDistance);
		return distance;
	}

	public static <T extends Comparable<T>> boolean arrayContains(T[] arr, T obj) {
		for (T element : arr) {
			if (element.equals(obj)) return true;
		}
		return false;
	}
}
