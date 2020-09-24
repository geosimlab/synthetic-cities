package utils;

public class Structs {

    public static class BaseStruct implements Cloneable {
	public Object clone() throws CloneNotSupportedException {
	    return super.clone();
	}
    }

    public static class TripTimeArguments extends BaseStruct {
	public double alpha = 1.3;
	public int beta = 1800; // [s]
	public double maxWaitTime = 1800; // [s]
	public int dispatchPeriod = 30; // [s]
	public int stopTime = 60; // [s]
    }

    public static class PopulationArguments extends BaseStruct {
	public int popSize = 1000;
	
	// default minimal distance, based on the default value of
	// "maxBeelineWalkConnectionDistance"
	public int minHomeWorkDistance = 300; // [m]

	public float leaveHomeTime = 6; // [hr]
	public float leaveHomeWindowSize = 3; // [hr]
	public float workdayLength = 6; // [hr]
	public float workdayWindowSize = 4;  // [hr]
    }
    
    public static class NetworkArguments extends BaseStruct {
   	public int numOfSt = 50;
   	public int numOfAv = 50;
   	public int linkLength = 100; // [m]
   	public double nodeShift = 0.4; // [% of linkLength]
   	
   	public int capacity = 1800;
   	public double speed = 15; // [km/h]
       }
}
