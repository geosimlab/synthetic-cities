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
}
