package syncity.population;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.lang3.tuple.Pair;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.population.PopulationUtils;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;;

public class PopulationCSV {
    
    protected static final String[] HEADER = {"fromLinkId", "toLinkId", "departureTime", "returnTime"};
    protected static int FROM_LINK_ID = 0;
    protected static int TO_LINK_ID = 1;
    protected static int DEP_TIME_ID = 2;
    protected static int RETURN_TIME_ID = 3;

    /*
     * Creates a population from the csv.
     * The format should be: fromLinkId,toLinkId,departueTime,returnTime
     * columns separated with a comma, as seen in /synthetic-cities/src/main/resources/PopulationCSVExample.csv
     */
    public static Population readPopulationCSV(String filename) throws IOException {
	CSVReader reader = new CSVReader(new FileReader(filename));
	Population pop = PopulationUtils.createPopulation( new PlansConfigGroup(), null  );
	String[] line;
	while ( (line = reader.readNext()) != null ) {
	    if (Arrays.equals(line, HEADER)) {
		continue;
	    }
	    Person person = PersonFactory.createPerson(pop);
	    Plan plan = line2Plan(line, pop.getFactory());
	    person.addPlan(plan);
	    pop.addPerson(person);
	}
	reader.close();
	return pop;
    }
    
    /*
     * write population to a csv, with columns for home link, work link, departure time and return time
     */
    public static String writePopulationCSV(Population pop, String filename) throws IOException {
	CSVWriter writer = new CSVWriter(new FileWriter(filename));
	writer.writeNext(HEADER);
	for (Person per : pop.getPersons().values()) {
	    Pair<Activity, Activity> homeWork = PersonAnalysis.getPersonHomeWorkActivities(per);
	    Id<Link> homeLink = homeWork.getLeft().getLinkId(); 
	    Id<Link> workLink = homeWork.getRight().getLinkId(); 
	    String homeDepTime = createTimeString(homeWork.getLeft().getEndTime()); 
	    String workDepTime = createTimeString(homeWork.getRight().getEndTime());
	    String[] nextLine = {homeLink.toString(), workLink.toString(), 
		    homeDepTime, workDepTime};
	    writer.writeNext(nextLine, true);
	}
	writer.close();
	return filename;
    }
    
    /*
     * Turns a csv line into a person plan that can be attached to a person 
     */
    public static Plan line2Plan(String[] csvLine, PopulationFactory factory) {
	Id<Link> fromLink = Id.createLinkId(csvLine[FROM_LINK_ID]),
		 toLink = Id.createLinkId(csvLine[TO_LINK_ID]);
	double depTime = parseTimeString(csvLine[DEP_TIME_ID]),
	       returnTime = parseTimeString(csvLine[RETURN_TIME_ID]);
	
	return new PersonFactory().createHomeWorkHomePlan(factory, depTime, returnTime,
		fromLink, toLink);
    }
    
    /*
     * parse time like "HH:mm:ss" or "HH:mm" into seconds from the beginning of the day
     */
    public static double parseTimeString(String time) {
	int hh = 0, mm = 0, ss = 0;
	String[] t = time.split(":");
	if (t.length == 3) {
	    hh = Integer.parseInt(t[0]);
	    mm = Integer.parseInt(t[1]);
	    ss = Integer.parseInt(t[2]);
	} else  if (t.length == 2){
	    hh = Integer.parseInt(t[0]);
	    mm = Integer.parseInt(t[1]);
	} else {
	    System.out.println("Error with time format: "+ time);
	    return 0;
	}
	
	return hh * 60*60 + mm * 60 + ss;
    }
    
    /*
     * format seconds as human readable time "hh:mm:ss" format
     */
    public static String createTimeString(double time) {
	int timeInt = (int)time;
	int ss = timeInt % 60;
	timeInt /= 60;
	int mm = timeInt % 60;
	timeInt /= 60;
	int hh = timeInt;
	return String.format("%02d:%02d:%02d", hh, mm, ss);
    }
    
    public static void main(String[] args) throws IOException {
	Population pop = readPopulationCSV("output/testCSVwrite.csv");
	new PopulationWriter(pop).write("output/testPopCSV.xml");
	writePopulationCSV(pop, "output/testCSVwrite.csv");
    }
}
