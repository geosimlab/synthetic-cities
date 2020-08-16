package syncity.scenarios;

public enum DispatcherParameterNames {
	DynamicRideSharingStrategy ("maxWaitTime", "maxDriveTimeIncrease"),
	TShareDispatcher ("maxWaitTime", "maxDriveTimeIncrease"),
	HighCapacityDispatcher ("maxWaitTime", "maxDriveTimeIncrease");
	
	
	protected final String maxWaitTimeParameter;
	protected final String directTripRatioParameter;
	
	
	DispatcherParameterNames (String maxWaitTimeParameter, String directTripRatioParameter){
		this.maxWaitTimeParameter = maxWaitTimeParameter;
		this.directTripRatioParameter = directTripRatioParameter;
	}
}
