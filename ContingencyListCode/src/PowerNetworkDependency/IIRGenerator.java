package PowerNetworkDependency;
// Author: Joydeep
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import java.util.List;

public class IIRGenerator {
	
	// --------------------------------------------------------------------------------- //
	// Class Variables
	private final int numBuses;
	private final int numTimeSteps;
	private final int numBranch;
	
	// the constant multiplied with transmission line flow to get the upper bound on the line
	private final double transCapConst = 1.5; 
	
	private HashMap<Integer,Integer> busNumToIndexMap; // map of the busID to a index in [0 , numBuses - 1]
	private HashMap<Integer, Integer> busIndexToNumMap; // map of the corresponding index to busID
	private HashMap<Integer, String> entityMap; // maintains a map of bus number to entityID
	private HashMap<String, Integer> entityMapRev; // maintains a map of entityID to bus number
	private HashMap<Integer, String> transMap; // maintains a map of transmission line to entityID
	private HashMap<String, Integer> transMapRev; // maintains a map of entityID to transmission line
	
	// maintains the adjacency list for flow between buses based on
	private ArrayList<ArrayList<Integer>> flowListReal; 
	
	// maintains the bound and values for the components in the power network
	/*
	 * For transmission lines just stores the bound of the line
	 * For buses it contains five values --- Load demand, Generation Capacity Max, Actual Generated, Power Input, Power Output
	 * (Following the given order)
	 */
	HashMap<String, List<Double>> componentBoundAndValuesReal;
	
	// voltage measurement at a given time step (the inverse flow direction is maintained for IIR generation)
	private HashMap<String, List<List<String>>> IIRSol = new HashMap<String, List<List<String>>>();
	// --------------------------------------------------------------------------------- //
	
	// --------------------------------------------------------------------------------- //
	// Load Data from Generator Code in MATLAB 
	// Takes in the caseFile name and populates the class variables 
	IIRGenerator(String caseVal, int time) throws FileNotFoundException{
		//Get Data from MATLAB
		File file = new File("InputFiles/" + caseVal + ".txt");
		Scanner scan = new Scanner(file);
		
		// Initialize class variables corresponding to buses
		numBuses = (int) scan.nextDouble();
		numTimeSteps = (int) scan.nextDouble();
		numBranch = (int) scan.nextDouble();
		
		if(time > numTimeSteps){
			System.out.println("No data available for this time");
			scan.close();
			return;
		}
		
		// skip lines to read data corresponding to the given time
		// Remember to exclude the time required to execute this loop from your time computation of data reading
		for(int i = 0; i < (time - 1) * (numBuses * 9 + numBranch * 6); i++)
			scan.nextLine();
		
		// variable for mapping busId with index and reverse
		busNumToIndexMap = new HashMap<Integer, Integer>();
		busIndexToNumMap = new HashMap<Integer, Integer>();
		componentBoundAndValuesReal = new HashMap<String, List<Double>>();
		
		// create map of buses with entity IDs
		entityMap = new HashMap<Integer, String>();
		entityMapRev = new HashMap<String, Integer>();
		int indexGen = 1;
		int indexLoad = 1;
		int indexNeutral = 1;
		
		// Load bus data, load and generator values in class variable
		for(int i = 0; i < numBuses; i++){
			// map the busID with index and reverse for processing
			int busID = (int) scan.nextDouble();
			busNumToIndexMap.put(busID, i);
			busIndexToNumMap.put(i, busID);
			
			int busType = (int) scan.nextDouble();
			double busLoadReal = scan.nextDouble();
			double busLoadImag = scan.nextDouble();
			double busGenMaxReal = scan.nextDouble();
			double busGenActualReal = scan.nextDouble();
			
			if((int) busLoadReal == 0 && (int) busLoadImag == 0 && busType == 1){
				entityMap.put(i, "N" + indexNeutral);
				entityMapRev.put("N" + indexNeutral, i);
				indexNeutral ++;
			}
			else if(busType == 2 || busType == 3){
				entityMap.put(i, "G" + indexGen);
				entityMapRev.put("G" + indexGen, i);
				indexGen ++;
			}
			else if(busType == 1){
				entityMap.put(i, "L" + indexLoad);
				entityMapRev.put("L" + indexLoad, i);
				indexLoad ++;
			}
			
			componentBoundAndValuesReal.put(entityMap.get(i), 
					Arrays.asList(busLoadReal, busGenMaxReal, busGenActualReal, 0.0, 0.0));
		}
			
		/// get the edges and corresponding power flow corresponding to index
		flowListReal = new ArrayList<ArrayList<Integer>>();
		for(int i = 0; i < numBuses; i++){
			flowListReal.add(new ArrayList<Integer>());
		}
		
		transMap = new HashMap<Integer, String>();
		transMapRev = new HashMap<String, Integer>();
		for(int i = 0; i < numBranch; i++){
			int firstNode = (int) scan.nextDouble();
			int secondNode = (int) scan.nextDouble();
			double powerRealFirstSecond = 100 * scan.nextDouble();
			double powerRealSecondFirst = 100 * scan.nextDouble();
			
			int edge1 = busNumToIndexMap.get(firstNode);
			int edge2 = busNumToIndexMap.get(secondNode);
			String line1 = Integer.toString(edge1) + Integer.toString(edge2);
			String line2 = Integer.toString(edge2) + Integer.toString(edge1);
			
			if(!transMap.containsKey(Integer.parseInt(line1))){
				transMap.put(Integer.parseInt(line1), "T" + i);
				transMapRev.put( "T" + i , Integer.parseInt(line1));
				transMap.put(Integer.parseInt(line2), "T" + i);
				transMapRev.put( "T" + i , Integer.parseInt(line2));
			}
			
			// get the real power flow and populate bound and values
			if(powerRealFirstSecond > powerRealSecondFirst){
				flowListReal.get(edge2).add(edge1);
				componentBoundAndValuesReal.put(transMap.get(Integer.parseInt(line2)), 
						Arrays.asList(transCapConst * -powerRealSecondFirst));
				
				List<Double> node1Vals = componentBoundAndValuesReal.get(entityMap.get(edge1));
				node1Vals.set(4, node1Vals.get(4) + powerRealFirstSecond);
				
				List<Double> node2Vals = componentBoundAndValuesReal.get(entityMap.get(edge2));
				node2Vals.set(3, node2Vals.get(3) - powerRealSecondFirst);
			}
			else{
				flowListReal.get(edge1).add(edge2);
				componentBoundAndValuesReal.put(transMap.get(Integer.parseInt(line1)), 
						Arrays.asList(transCapConst * -powerRealFirstSecond));
				
				List<Double> node1Vals = componentBoundAndValuesReal.get(entityMap.get(edge1));
				node1Vals.set(3, node1Vals.get(3) - powerRealFirstSecond);
				
				List<Double> node2Vals = componentBoundAndValuesReal.get(entityMap.get(edge2));
				node2Vals.set(4, node2Vals.get(4) + powerRealSecondFirst);
				
			}
		}
		scan.close();
	}
	// --------------------------------------------------------------------------------- //
	
	// --------------------------------------------------------------------------------- //
	//Generate the IIRs for the particular time step with the form required by solution of Heuristic and ILP
	public void generateIIRForSolutions(int flowDirectionProtocol){
		ArrayList<ArrayList<Integer>> flowList = flowListReal;
		
		// Generate the IIRs
		for(int bus: busIndexToNumMap.keySet()){
			if(flowList.get(bus).size() == 0) continue;
			List<List<String>> minterm = new ArrayList<List<String>>();
			for(int adjBus: flowList.get(bus)){
				String line = Integer.toString(bus) + Integer.toString(adjBus);
				minterm.add(Arrays.asList(transMap.get(Integer.parseInt(line)), entityMap.get(adjBus))); 
			}
			if(entityMap.get(bus).charAt(0) == 'G') minterm.add(Arrays.asList(entityMap.get(bus)));
			IIRSol.put(entityMap.get(bus), minterm);
		}
	}
	// --------------------------------------------------------------------------------- //
	
	// --------------------------------------------------------------------------------- //
	// Getter Files
	public HashMap<String, Integer> getMaps() { return entityMapRev;}
	public HashMap<String, List<List<String>>> getIIRSol() { return IIRSol;}
	public HashMap<String, List<Double>> getComponentBoundAndValuesReal() { return componentBoundAndValuesReal;}
	// --------------------------------------------------------------------------------- //
	
}
