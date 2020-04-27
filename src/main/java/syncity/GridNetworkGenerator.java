package syncity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkWriter;

/**
 * Generate a bidirectional grid
 * 
 * @author theFrok
 */
public class GridNetworkGenerator {

	// capacity at all links
	private static final long DEFAULT_CAPACITY = 1800; // [veh/h]
	// default link length for all links
	private static final int DEFAULT_LINK_LENGTH = 100; // [m]
	// default num of Streets (north-south) and avenues (east-west) in the grid
	private static final int DEFALUT_STREETS_NUM = 50;
	private static final int DEFAULT_AVENUES_NUM = 50;
	// default speed for both streets and avenues
	private static final double DEFAULT_SPEED = 15; // [km/h]
	
	private long capacity;
	private int linkLength;
	private int numOfStreets;
	private int numOfAvenues;
	private double driveSpeedStreets;
	private double driveSpeedAvenues;
	private Network net;
	
	public GridNetworkGenerator() {
		this(DEFAULT_CAPACITY, DEFALUT_STREETS_NUM, DEFAULT_AVENUES_NUM, 
				DEFAULT_LINK_LENGTH, DEFAULT_SPEED, DEFAULT_SPEED);
	}

	public GridNetworkGenerator(int numOfStreets, int numOfAvenues) {
		this(DEFAULT_CAPACITY, numOfStreets, numOfAvenues, 
				DEFAULT_LINK_LENGTH, DEFAULT_SPEED, DEFAULT_SPEED);
	}
	
	/**
	 * generate a grid network in the given size, the links are all bidirectional
	 * 
	 * @param capacity the number of vehicles that can be in the same link
	 * @param numOfStreets number of streets north to south
	 * @param numOfAvenues number of avenues east to west
	 * @param linkLength   the link length (in meters)
	 * @param streetSpeed  vehicles speed in streets (in Km/h)
	 * @param avenueSpeed  vehicles speed in avenues (in Km/h)
	 */
	public GridNetworkGenerator(long capacity, int numOfStreets, int numOfAvenues, int linkLength, double streetSpeed,
			double avenueSpeed) {
		this.capacity = capacity;
		this.linkLength = linkLength;
		this.numOfStreets = numOfStreets;
		this.numOfAvenues = numOfAvenues;
		this.driveSpeedStreets = streetSpeed;
		this.driveSpeedAvenues = avenueSpeed;
		this.net = NetworkUtils.createNetwork();
	}
	
	public static String writeDefaultNetwork(String outPath) throws IOException {
		GridNetworkGenerator grid = new GridNetworkGenerator();
		grid.generateGridNetwork();
		return grid.writeNetwork(outPath);
	}
	
	public Network getNetwork() {
		return this.net;
	}

	public int getNumOfStreets() {
		return this.numOfStreets;
	}
	
	public int getNumOfAvenues() {
		return this.numOfAvenues;
	}
	
	public String getTitle() {
		return String.format("GridNetwork-%d_%d", this.numOfStreets, this.numOfAvenues);
	}

	/**
	 * Writes the network to a file in the given path, if path is folder the
	 * default name for the network file would be "GridNetwork-(numOfStreets)_(numOfAvenues).xml"
	 * 
	 * @param outPath the path to write the network xml to
	 * @return the absolute path of the created file
	 * @throws IOException
	 */
	public String writeNetwork(String outPath) throws IOException {
		Path out = Paths.get(outPath);
		Path outputFolder;
		if (Files.isDirectory(out)) {
			outputFolder = out;
			out = out.resolve(this.getTitle() + ".xml");
		} else
			outputFolder = out.getParent();
		// create output folder if necessary
		Files.createDirectories(outputFolder);

		// write network
		new NetworkWriter(this.net).write(out.toString());
		return out.toAbsolutePath().toString();
	}

	public void generateGridNetwork() {
		// create an empty network
		NetworkFactory fac = this.net.getFactory();

		for (int st = 0; st < this.numOfStreets; ++st) {
			for (int av = 0; av < this.numOfAvenues; ++av) {
				// create new node
				String id = getNodeIdString(st, av);
				Node newNode = fac.createNode(Id.createNodeId(id), new Coord(st * this.linkLength, av * this.linkLength));
				this.net.addNode(newNode);
				// connect new node to the previous nodes
				if (av > 0) {
					Node prevAvNode = getNodeByStAv(st, av - 1, this.net);
					connectNodes(newNode, prevAvNode, this.net, this.linkLength, this.driveSpeedStreets, this.capacity, true);
				}
				if (st > 0) {
					Node prevStNode = getNodeByStAv(st - 1, av, this.net);
					connectNodes(newNode, prevStNode, this.net, this.linkLength, this.driveSpeedAvenues, this.capacity, true);
				}
			}
		}
	}

	/**
	 * connect to given nodes in the given network
	 * 
	 * @param srcNode     the origin of the connection
	 * @param dstNode     the destination of the connection
	 * @param net         the network the two nodes resides in
	 * @param linkLength  the desired distance between the two node (in meters)
	 * @param driveSpeed  the driving speed in he link between the two nodes
	 * @param linkCapacity the capacity of the link
	 * @param bidirection whether to create a link from dstNode to srcNode as well
	 */
	public static void connectNodes(Node srcNode, Node dstNode, Network net, int linkLength, double driveSpeed, 
			long linkCapacity, Boolean bidirection) {
		NetworkFactory fac = net.getFactory();
		String srcId = srcNode.getId().toString();
		String dstId = dstNode.getId().toString();
		double travelTime = linkLength / driveSpeed * 3.6; // [s]
		// create links
		Link l = fac.createLink(Id.createLinkId(srcId + "->" + dstId), srcNode, dstNode);
		setLinkAttributes(l, linkCapacity, linkLength, travelTime);
		net.addLink(l);
		if (bidirection) {
			// create reverse link
			l = fac.createLink(Id.createLinkId(dstId + "->" + srcId), dstNode, srcNode);
			setLinkAttributes(l, linkCapacity, linkLength, travelTime);
			net.addLink(l);
		}
	}

	/**
	 * get a node by it's location
	 * 
	 * @param streetNum the street number the nodes is in (starts from 0)
	 * @param avenueNum the avenue number the nodes is in (starts from 0)
	 * @param net       the network the node is in
	 * @return the node in the specified location
	 */
	private static Node getNodeByStAv(int streetNum, int avenueNum, Network net) {
		String id = getNodeIdString(streetNum, avenueNum);
		return getNodeById(id, net);
	}

	/**
	 * get a node from the network by it's String id
	 * 
	 * @param nodeId the node's id string
	 * @param net    the network
	 * @return the nodes with that id
	 */
	private static Node getNodeById(String nodeId, Network net) {
		return net.getNodes().get(Id.createNodeId(nodeId));
	}

	/**
	 * generate a descriptive node id to be used in the grid
	 * 
	 * @param streetNum
	 * @param avenueNum
	 * @return
	 */
	private static String getNodeIdString(int streetNum, int avenueNum) {
		return "(" + streetNum + "," + avenueNum + ")";
	}

	/**
	 * sets the link attribute, this method was copied as-is from the matsim code
	 * examples
	 * 
	 * @param link       the link to set the attributes on
	 * @param capacity   link capacity (in vh/hr)
	 * @param length     link length (in meters)
	 * @param travelTime link travel time in seconds
	 */
	private static void setLinkAttributes(Link link, double capacity, double length, double travelTime) {
		link.setCapacity(capacity);
		link.setLength(length);
		// agents have to reach the end of the link before the time step ends to
		// be able to travel forward in the next time step (matsim time step logic)
		link.setFreespeed(link.getLength() / (travelTime - 0.1));
	}
	
	public static void main(String[] args) throws IOException {
		GridNetworkGenerator grid;
		
		if (args.length == 2) {
			int streetsNum = Integer.parseInt(args[0]);
			int avenueNum = Integer.parseInt(args[1]);
			grid = new GridNetworkGenerator(streetsNum, avenueNum);
		} else {
			grid = new GridNetworkGenerator();			
		}
		
		grid.generateGridNetwork();
		grid.writeNetwork("output/");;
	}

}
