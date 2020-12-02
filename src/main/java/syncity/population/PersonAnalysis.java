package syncity.population;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import utils.MatsimUtils;

public class PersonAnalysis {

    /*
     * Return a Pair of the person's home node and work node
     * both are extracted from the plan
     */
    public static Pair<Node, Node> getPersonHomeWork(Person person, Network network){
        List<PlanElement> planElements = person.getSelectedPlan()
        	.getPlanElements();
        Node homeNode = null;
        Node workNode = null;
        for (PlanElement planElement : planElements) {
            if (!(planElement instanceof Activity))
        	continue;
            Activity act = (Activity) planElement;
            if (act.getType() == PersonFactory.HOME_ACTIVITY_TYPE) {
        	homeNode = network.getLinks().get(act.getLinkId())
        		.getToNode();
            }
            if (act.getType() == PersonFactory.WORK_ACTIVITY_TYPE) {
        	workNode = network.getLinks().get(act.getLinkId())
        		.getToNode();
            }
        }
        Pair<Node, Node> homeWorkPair = Pair.of(homeNode, workNode);
        return homeWorkPair;
    }
    
    /*
     * Return a Pair of the person's home avtivity and work activity
     * both are extracted from the plan
     */
    public static Pair<Activity, Activity> getPersonHomeWorkActivities(Person person){
        List<PlanElement> planElements = person.getSelectedPlan()
        	.getPlanElements();
        Activity home = null;
        Activity work= null;
        for (PlanElement planElement : planElements) {
            if (!(planElement instanceof Activity))
        	continue;
            Activity act = (Activity) planElement;
            if (act.getType() == PersonFactory.HOME_ACTIVITY_TYPE && home == null) {
        	home = act;
            }
            if (act.getType() == PersonFactory.WORK_ACTIVITY_TYPE) {
        	work= act;
            }
        }
        Pair<Activity, Activity> homeWorkPair = Pair.of(home, work);
        return homeWorkPair;
    }
    
    /*
     * Return the home-work distance of a person
     * useful for analysis of the population 
     */
    public static double getHomeWorkDistance(Person person, Network network) {
	Pair<Node, Node> pair = getPersonHomeWork(person, network);
	return MatsimUtils.nodesDistance(pair.getLeft(), pair.getRight());
    }

}
