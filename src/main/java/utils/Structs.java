package utils;

public class Structs {
    
    public static class BaseStruct implements Cloneable {
	private final static boolean DEFAULT_VALUES = false;
	
	public BaseStruct() {
	    if (DEFAULT_VALUES) {
		throw(new RuntimeException("Config contains default values"));
	    }
	}
	
	public Object clone() throws CloneNotSupportedException {
	    return super.clone();
	}
    }

    public static class DispatcherArguments extends BaseStruct {
	public double alpha = 1;
	public int beta = 0; // [s]
	public double maxWaitTime = 15 * 60; // [s]
	public int dispatchPeriod = 30; // [s]
	public int stopTime = 60; // [s]
	public double speedEstimation = 25 / 3.6f ; // [m/s]
	
	public int vehiclesNum = 250;
	public int seatsPerVehicle = 10;
	public boolean rejection = true;
	public boolean rebalance = true;
    }

    public static class PopulationArguments extends BaseStruct {
	public int popSize = 10 * 1000;

	// default minimal distance, based on the default value of
	// "maxBeelineWalkConnectionDistance"
	public int minHomeWorkDistance = 300; // [m]

	public float leaveHomeTime = 6; // [hr]
	public float leaveHomeWindowSize = 1; // [hr]
	public float workdayLength = 6; // [hr]
	public float workdayWindowSize = 0; // [hr]
    }

    public static class NetworkArguments extends BaseStruct {
	public int numOfSt = 1;
	public int numOfAv = 130;
	public int linkLength = 100; // [m]
	public double nodeShift = 0.4; // [% of linkLength]

	public int capacity = 1800;
	public double speed = 15; // [km/h]
    }
}
