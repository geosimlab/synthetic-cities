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
	public static double randInWindow(double base, double window) {
		return base + (window * getUniformRandomGenerator().nextDouble());
	}
	
	/**
	 * return a random number between (base - delta) and (base + delta)
	 * 
	 * @param base   the base size
	 * @param delta the size of the random window
	 * @return a random number within the window
	 */
	public static double randAroundBase(double base, double delta) {
		return randInWindow(base - delta, delta*2);
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
