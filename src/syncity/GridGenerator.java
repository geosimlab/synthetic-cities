package syncity;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Generate a bidirectional grid
 * 
 * @author theFrok
 */
public class GridGenerator {

	// capacity at all links
	private static final long CAP_MAIN = 1800; // [veh/h]

	// default link length for all other links
	private static final int DEFAULT_LINK_LENGTH = 100; // [m]

	// default num of Streets (north-south) in the grid
	private static final int DEFALUT_STREETS_NUM = 20;

	// default num of Avenues (east-west) in the grid
	private static final int DEFAULT_AVENUE_NUM = 20;

	// default speed for streets and avenues
	private static final double DEFAULT_SPEED = 15; // [km/h]

	public static void main(String[] args) throws IOException {

		Network net = generateGridNetwork(DEFALUT_STREETS_NUM, DEFAULT_AVENUE_NUM);

		writeNetwork(net, "output/network.xml");
	}

	/**
	 * Writes the net work to a file in the given path, if path is folder the
	 * default name for the network file would be network.xml
	 * 
	 * @param net
	 * @throws IOException
	 */
	public static void writeNetwork(Network net, String outPath) throws IOException {
		Path out = Paths.get(outPath);
		Path outputFolder;
		if (Files.isDirectory(out)) {
			outputFolder = out;
			out = out.resolve("network.xml");
		} else
			outputFolder = out.getParent();
		// create output folder if necessary
		Files.createDirectories(outputFolder);

		// write network
		new NetworkWriter(net).write(out.toString());
	}

	/**
	 * generate a grid net work in a given size, other parameters are default
	 * 
	 * @param numOfStreets number of streets north to south
	 * @param numOfAvenues number of avenues east to west
	 * @return the generated grid network object
	 */
	public static Network generateGridNetwork(int numOfStreets, int numOfAvenues) {
		return generateGridNetwork(numOfStreets, numOfAvenues, DEFAULT_LINK_LENGTH, DEFAULT_SPEED, DEFAULT_SPEED);
	}

	/**
	 * generate a grid network in the given size, the links are all bidirectional
	 * 
	 * @param numOfStreets number of streets north to south
	 * @param numOfAvenues number of avenues east to west
	 * @param linkLength   the link length (in meters)
	 * @param streetSpeed  vehicles speed in streets (in Km/h)
	 * @param avenueSpeed  vehicles speed in avenues (in Km/h)
	 * @return the generated grid network object
	 */
	public static Network generateGridNetwork(int numOfStreets, int numOfAvenues, int linkLength, double streetSpeed,
			double avenueSpeed) {
		// create an empty network
		Network net = NetworkUtils.createNetwork();
		NetworkFactory fac = net.getFactory();

		for (int st = 0; st < numOfStreets; ++st) {
			for (int av = 0; av < numOfAvenues; ++av) {
				// create new node
				String id = getNodeIdString(st, av);
				Node newNode = fac.createNode(Id.createNodeId(id), new Coord(st * linkLength, av * linkLength));
				net.addNode(newNode);
				// connect new node to the previous nodes
				if (av > 0) {
					Node prevAvNode = getNodeByStAv(st, av - 1, net);
					connectNodes(newNode, prevAvNode, net, linkLength, streetSpeed, true);
				}
				if (st > 0) {
					Node prevStNode = getNodeByStAv(st - 1, av, net);
					connectNodes(newNode, prevStNode, net, linkLength, avenueSpeed, true);
				}
			}
		}
		return net;
	}

	/**
	 * connect to given nodes in the given network
	 * 
	 * @param srcNode     the origin of the connection
	 * @param dstNode     the destination of the connection
	 * @param net         the network the two nodes resides in
	 * @param linkLength  the desired distance between the two node (in meters)
	 * @param driveSpeed  the driving speed in he link between the two nodes
	 * @param bidirection whether to create a link from dstNode to srcNode as well
	 */
	public static void connectNodes(Node srcNode, Node dstNode, Network net, int linkLength, double driveSpeed,
			Boolean bidirection) {
		NetworkFactory fac = net.getFactory();
		String srcId = srcNode.getId().toString();
		String dstId = dstNode.getId().toString();
		double travelTime = linkLength / driveSpeed * 3.6; // [s]
		// create links
		Link l = fac.createLink(Id.createLinkId(srcId + "->" + dstId), srcNode, dstNode);
		setLinkAttributes(l, CAP_MAIN, linkLength, travelTime);
		net.addLink(l);
		if (bidirection) {
			// create reverse link
			l = fac.createLink(Id.createLinkId(dstId + "->" + srcId), dstNode, srcNode);
			setLinkAttributes(l, CAP_MAIN, linkLength, travelTime);
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

}
