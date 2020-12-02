package syncity.population;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;

import utils.BasicUtils;
import utils.Structs.PopulationArguments;

public class PersonFactory {

    // The standard names for the activities
    public static final String WORK_ACTIVITY_TYPE = "work";
    public static final String HOME_ACTIVITY_TYPE = "home";

    protected final static int SECONDS_IN_HOUR = 3600;

    // Plan generation parameters
    protected float leaveHomeTime = 6;
    protected float leaveHomeWindowSize = 3;
    protected float workdayLength = 6;
    protected float workdayWindowSize = 4;
    
    public PersonFactory() {}
    public PersonFactory(PopulationArguments args) {
	updatePersonParameters(args);
    }
    
    /*
     * update person generation parameters with values from the configuration
     */
    public void updatePersonParameters(PopulationArguments args) {
	this.leaveHomeTime = args.leaveHomeTime;
	this.leaveHomeWindowSize = args.leaveHomeWindowSize;
	this.workdayLength = args.workdayLength;
	this.workdayWindowSize = args.workdayWindowSize;
    }
    
    /*
     * creates a person with a home-work-home plan,
     * times are chosen by random according to the class parameters.
     */
    public Person createPersonWithStandardPlan(Node homeNode,
	    Node WorkNode, Population population) {
	Person person = createPerson(population);
	Plan plan = createPlanToPerson(homeNode, WorkNode, population);
	person.addPlan(plan);
	return person;
    }

    /**
     * A method to prevent double use of ids
     * 
     * @return a person instance
     */
    protected static Person createPerson(Population population) {
	PopulationFactory populationFactory = population.getFactory();
	return populationFactory.createPerson(
		Id.createPersonId(population.getPersons().size()));
    }

    /**
     * creates a how work home plane for a person living on homeNode work will
     * be a
     * random node in the network work start and end times are randomized based
     * on
     * the object attribute
     * 
     * @param nodes    array of all the nodes in the network
     * @param homeNode the home node for that person
     * @param person   the person to add the plan on
     */
    protected Plan createPlanToPerson(Node homeNode, Node WorkNode,
	    Population population) {
	PopulationFactory populationFactory = population.getFactory();
	double leaveHome = BasicUtils.randInWindow(leaveHomeTime,
		leaveHomeWindowSize);
	double leaveWork = leaveHome
		+ BasicUtils.randInWindow(workdayLength, workdayWindowSize);
	Plan plan = createHomeWorkHomePlan(populationFactory, homeNode,
		leaveHome, WorkNode, leaveWork);
	return plan;
    }

    /**
     * creates a plan from home to work and back again uses link coming into the
     * given nodes (notice comments in the class doc for more information
     * 
     * @param populationFactory matsim population factory
     * @param homeNode          the home node
     * @param leaveHome         time to leave home (in hours 0-24)
     * @param workNode          the work node
     * @param leaveWork         time to leave work (in hours 0-24)
     * @return the created plan
     */
    public Plan createHomeWorkHomePlan(
	    PopulationFactory populationFactory, Node homeNode,
	    double leaveHome, Node workNode, double leaveWork) {

	Id<Link> homeLink = homeNode.getInLinks().values().iterator().next().getId();
	Id<Link> workLink = workNode.getInLinks().values().iterator().next().getId();

	return createHomeWorkHomePlan(populationFactory, 
		leaveHome * SECONDS_IN_HOUR, 
		leaveWork * SECONDS_IN_HOUR,
		homeLink, workLink);
    }
    
    /*
     * The same as above but with links instead of nodes
     */
    public Plan createHomeWorkHomePlan(PopulationFactory populationFactory,
	    double leaveHome, double leaveWork, Id<Link> homeLink, Id<Link> workLink) {
	Plan plan = populationFactory.createPlan();
	Activity morningActivity = populationFactory
		.createActivityFromLinkId(HOME_ACTIVITY_TYPE, homeLink);
	morningActivity.setEndTime(leaveHome);
	plan.addActivity(morningActivity); // add the Activity to the Plan

	Leg leg = populationFactory.createLeg("av");
	leg.setDepartureTime(leaveHome);
	plan.addLeg(leg);

	Activity WorkActivity = populationFactory
		.createActivityFromLinkId(WORK_ACTIVITY_TYPE, workLink);
	WorkActivity.setStartTime(leaveHome);
	WorkActivity.setEndTime(leaveWork);
	plan.addActivity(WorkActivity);

	leg = populationFactory.createLeg("av");
	leg.setDepartureTime(leaveWork);
	plan.addLeg(leg);

	Activity eveningActivity = populationFactory
		.createActivityFromLinkId(HOME_ACTIVITY_TYPE, homeLink);
	plan.addActivity(eveningActivity);
	return plan;
    }
}
