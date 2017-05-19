package PowerNetworkDependency;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Scanner;
import java.util.List;

public class IIRGenerator {
	
	// --------------------------------------------------------------------------------- //
	// Class Variables
	private final int numBuses;
	private final int numTimeSteps;
	private final int numBranch;
	private int totalComp;
	
	// the constant multiplied with transmission line flow to get the upper bound on the line
	private final double transCapConst = 1.5; 
	
	private HashMap<Integer,Integer> busNumToIndexMap; // map of the busID to a index in [0 , numBuses - 1]
	private HashMap<Integer, Integer> busIndexToNumMap; // map of the corresponding index to busID
	private HashMap<Integer, String> entityMap; // maintains a map of bus number to entityID
	private HashMap<String, Integer> entityMapRev; // maintains a map of entityID to bus number
	private HashMap<Integer, String> transMap; // maintains a map of transmission line to entityID
	private HashMap<String, Integer> transMapRev; // maintains a map of entityID to transmission line
	private HashMap<String, List<String>> transLine; // pair of entities connected by a transmission line (outEntity, inEntity)
	
	// maintains the adjacency list for flow between buses based on
	private ArrayList<ArrayList<Integer>> flowListReal; 
	
	// current value and bound on the entities
	HashMap<String, Integer> entityPowerVal;
	HashMap<String, Integer> entityPowerBound;
	
	// voltage measurement at a given time step (the inverse flow direction is maintained for IIR generation)
	private HashMap<String, List<List<String>>> IIRSol = new HashMap<String, List<List<String>>>();
	
	// list of transmission line entities going in and coming out of an entity
	private HashMap<String, List<String>> busInputLines;
	private HashMap<String, List<String>> busOutputLines;
	
	List<Integer> vertex;
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
		busInputLines = new HashMap<String, List<String>>();
		busOutputLines = new HashMap<String, List<String>>();
		entityPowerVal = new HashMap<String, Integer>();
		entityPowerBound = new HashMap<String, Integer>();
		
		// create map of buses with entity IDs
		entityMap = new HashMap<Integer, String>();
		entityMapRev = new HashMap<String, Integer>();
		
		transMap = new HashMap<Integer, String>();
		transMapRev = new HashMap<String, Integer>();
		transLine = new HashMap<String, List<String>>();
		
		/// get the edges and corresponding power flow corresponding to index
		flowListReal = new ArrayList<ArrayList<Integer>>();
		for(int i = 0; i < numBuses; i++){
			flowListReal.add(new ArrayList<Integer>());
		}
		
		// map for holding generator bus to auxiliary entity map
		HashMap<String, String> genToAuxMap = new HashMap<String, String>();
		
		int indexGen = 1;
		int indexLoad = 1;
		int indexNeutral = 1;
		int indexAuxBus = numBuses;
		int indexAuxBranch = numBranch;
		
		vertex = new ArrayList<Integer>();
		// Load bus data, load and generator values in class variable
		for(int i = 0; i < numBuses; ++i){
			vertex.add(i);
			// map the busID with index and reverse for processing
			int busID = (int) scan.nextDouble();
			busNumToIndexMap.put(busID, i);
			busIndexToNumMap.put(i, busID);
			
			int busType = (int) scan.nextDouble();
			int busLoadReal = (int) scan.nextDouble();
			int busLoadImag = (int) scan.nextDouble();
			int busGenMaxReal = (int) (scan.nextDouble()/0.8);
			int busGenActualReal = (int) scan.nextDouble();
			
			if(busLoadReal == 0 && (int) busLoadImag == 0 && busType == 1){
				entityMap.put(i, "N" + indexNeutral);
				entityMapRev.put("N" + indexNeutral, i);
				busInputLines.put("N" + indexNeutral, new ArrayList<String>());
				busOutputLines.put("N" + indexNeutral, new ArrayList<String>());
				entityPowerVal.put(entityMap.get(i), 0);
				entityPowerBound.put(entityMap.get(i), 0);
				indexNeutral ++;
			}
			else if(busType == 2 || busType == 3){
				entityMap.put(i, "G" + indexGen);
				entityMapRev.put("G" + indexGen, i);
				
				// create auxiliary entity for the generator bus
				if(busLoadImag == 0){
					busIndexToNumMap.put(indexAuxBus, indexAuxBus);
					entityMap.put(indexAuxBus, "N" + indexNeutral);
					entityMapRev.put("N" + indexNeutral, indexAuxBus);
					busInputLines.put("N" + indexNeutral, new ArrayList<String>());
					busOutputLines.put("N" + indexNeutral, new ArrayList<String>());
					entityPowerVal.put(entityMap.get(indexAuxBus), 0);
					entityPowerBound.put(entityMap.get(indexAuxBus), 0);
					genToAuxMap.put("G" + indexGen, "N" + indexNeutral);
					indexNeutral ++;
				}
				else{
					busIndexToNumMap.put(indexAuxBus, indexAuxBus);
					entityMap.put(indexAuxBus, "L" + indexLoad);
					entityMapRev.put("L" + indexLoad, indexAuxBus);
					busInputLines.put("L" + indexLoad, new ArrayList<String>());
					busOutputLines.put("L" + indexLoad, new ArrayList<String>());
					entityPowerVal.put(entityMap.get(indexAuxBus), busLoadReal);
					entityPowerBound.put(entityMap.get(indexAuxBus), busLoadReal);
					genToAuxMap.put("G" + indexGen, "L" + indexLoad);
					indexLoad ++;
				}
				
				String line = Integer.toString(i) + Integer.toString(indexAuxBus);
				transMap.put(Integer.parseInt(line), "T" + indexAuxBranch);
				transMapRev.put( "T" + indexAuxBranch , Integer.parseInt(line));
				entityPowerVal.put(transMap.get(Integer.parseInt(line)), 
						busGenActualReal);
				entityPowerBound.put(transMap.get(Integer.parseInt(line)), 
						(int) (busGenActualReal * transCapConst));
				transLine.put(transMap.get(Integer.parseInt(line)), 
						Arrays.asList(entityMap.get(i), entityMap.get(indexAuxBus)));
				flowListReal.add(new ArrayList<Integer>());
				flowListReal.get(indexAuxBus).add(i);
				indexAuxBranch ++;
				vertex.add(indexAuxBus);
				indexAuxBus ++;
				
				busInputLines.put("G" + indexGen, new ArrayList<String>());
				busOutputLines.put("G" + indexGen, new ArrayList<String>());
				entityPowerVal.put(entityMap.get(i), busGenActualReal);
				entityPowerBound.put(entityMap.get(i), busGenMaxReal);
				indexGen ++;
			}
			else if(busType == 1){
				entityMap.put(i, "L" + indexLoad);
				entityMapRev.put("L" + indexLoad, i);
				busInputLines.put("L" + indexLoad, new ArrayList<String>());
				busOutputLines.put("L" + indexLoad, new ArrayList<String>());
				entityPowerVal.put(entityMap.get(i), busLoadReal);
				entityPowerBound.put(entityMap.get(i), busLoadReal);
				indexLoad ++;
			}
		}
		
		for(int i = 0; i < numBranch; i++){
			int firstNode = (int) scan.nextDouble();
			int secondNode = (int) scan.nextDouble();
			int powerRealFirstSecond = (int) (100.0 *  scan.nextDouble());
			int powerRealSecondFirst = (int) (100.0 *  scan.nextDouble());
			int edge1, edge2;
			
			if(entityMap.get(busNumToIndexMap.get(firstNode)).charAt(0) == 'G'){
				String entity = entityMap.get(busNumToIndexMap.get(firstNode));
				String auxEntity = genToAuxMap.get(entity);
				edge1 = entityMapRev.get(auxEntity);
			}
			else
				edge1 = busNumToIndexMap.get(firstNode);
			
			if(entityMap.get(busNumToIndexMap.get(secondNode)).charAt(0) == 'G'){
				String entity = entityMap.get(busNumToIndexMap.get(secondNode));
				String auxEntity = genToAuxMap.get(entity);
				edge2 = entityMapRev.get(auxEntity);
			}
			else
				edge2 = busNumToIndexMap.get(secondNode);
			
			String line1 = Integer.toString(edge1) + Integer.toString(edge2);
			String line2 = Integer.toString(edge2) + Integer.toString(edge1);
			
			// get the real power flow and populate bound and values
			if(powerRealFirstSecond > powerRealSecondFirst){
				if(!transMap.containsKey(Integer.parseInt(line1))){
					flowListReal.get(edge2).add(edge1);
					transMap.put(Integer.parseInt(line1), "T" + i);
					transMapRev.put( "T" + i , Integer.parseInt(line1));
					entityPowerVal.put(transMap.get(Integer.parseInt(line1)), 
							- powerRealSecondFirst);
					entityPowerBound.put(transMap.get(Integer.parseInt(line1)), 
							(int) (transCapConst * (- powerRealSecondFirst)));
					transLine.put(transMap.get(Integer.parseInt(line1)), 
							Arrays.asList(entityMap.get(edge1), entityMap.get(edge2)));
				}
				else{
					entityPowerVal.put(transMap.get(Integer.parseInt(line1)), 
							- powerRealSecondFirst + 
							entityPowerVal.get(transMap.get(Integer.parseInt(line1))));
					entityPowerBound.put(transMap.get(Integer.parseInt(line1)), 
							(int) (transCapConst * (entityPowerVal.get(transMap.get(Integer.parseInt(line1))))));
				}
			}
			else{
				if(!transMap.containsKey(Integer.parseInt(line2))){
					flowListReal.get(edge1).add(edge2);
					transMap.put(Integer.parseInt(line2), "T" + i);
					transMapRev.put( "T" + i , Integer.parseInt(line2));
					entityPowerVal.put(transMap.get(Integer.parseInt(line2)), 
							-powerRealFirstSecond);
					entityPowerBound.put(transMap.get(Integer.parseInt(line2)), 
							(int) (transCapConst * (- powerRealFirstSecond)));
					transLine.put(transMap.get(Integer.parseInt(line2)), 
							Arrays.asList(entityMap.get(edge2), entityMap.get(edge1)));
				}
				else{
					entityPowerVal.put(transMap.get(Integer.parseInt(line2)), 
							-powerRealFirstSecond + 
							entityPowerVal.get(transMap.get(Integer.parseInt(line2))));
					entityPowerBound.put(transMap.get(Integer.parseInt(line2)), 
							(int) (transCapConst * (entityPowerVal.get(transMap.get(Integer.parseInt(line2))))));
				}
			}
		}
		scan.close();
		
		totalComp= indexAuxBus + indexAuxBranch;
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
				String line = Integer.toString(adjBus) + Integer.toString(bus);
				List<String> toBus = busInputLines.get(entityMap.get(bus));
				List<String> fromBus = busOutputLines.get(entityMap.get(adjBus));
				toBus.add(transMap.get(Integer.parseInt(line)));
				fromBus.add(transMap.get(Integer.parseInt(line)));
				minterm.add(Arrays.asList(transMap.get(Integer.parseInt(line)), entityMap.get(adjBus))); 
			}
			IIRSol.put(entityMap.get(bus), minterm);
		}
	}
	// --------------------------------------------------------------------------------- //
	
	// --------------------------------------------------------------------------------- //
	// Getter Files
	public HashMap<String, Integer> getMaps() { return entityMapRev;}
	public HashMap<String, List<List<String>>> getIIRSol() { return IIRSol;}
	public HashMap<String, Integer> getEntityPowerVal() { return entityPowerVal;}
	public HashMap<String, Integer> getEntityPowerBound() { return entityPowerBound;}
	public HashMap<String, List<String>> getToBus() { return busInputLines;}
	public HashMap<String, List<String>> getFromBus() { return busOutputLines;}
	public HashMap<String, List<String>> getTransLine() { return transLine;}
	public int getTotalComp() { return totalComp;}
	// --------------------------------------------------------------------------------- //
	
	// Compute maximum path length in the graph
	public int getMaximumPathLength(){
		HashMap<List<Integer>, Integer> shortestPath = new HashMap<List<Integer>, Integer>();
		int maxPathLength = 0;
		int maxValue = 10000;
		//Initialization
		for(int v: vertex){
			for(int u: vertex){
				if(v == u) shortestPath.put(Arrays.asList(v, v), 0);
				else shortestPath.put(Arrays.asList(v, u), maxValue); // if there is no edge the length is maxVal
			}
			List<Integer> vWeights = new ArrayList<Integer>(Collections.nCopies(flowListReal.get(v).size(), 1));
			int index = 0;
			for(int u: flowListReal.get(v)){
				shortestPath.put(Arrays.asList(v,u), vWeights.get(index));
				index ++;
			}
		}
		
		//Compute All Pair Shortest Path
		for(int k: vertex){	
			for(int i: vertex){
				for(int j: vertex){
					if(shortestPath.get(Arrays.asList(i,j)) >
						shortestPath.get(Arrays.asList(i,k)) + shortestPath.get(Arrays.asList(k,j)))	
						shortestPath.replace(Arrays.asList(i,j),
								shortestPath.get(Arrays.asList(i,k)) + shortestPath.get(Arrays.asList(k,j)));
				}
			}
		}
		
		// get the maximum path length
		for(int i: vertex){
			for(int j: vertex){
				if(maxPathLength < shortestPath.get(Arrays.asList(i,j))){
					if(shortestPath.get(Arrays.asList(i,j)) == maxValue) continue;
					maxPathLength = shortestPath.get(Arrays.asList(i,j));
				}
			}
		}
		
		return maxPathLength;
	}
	
}
