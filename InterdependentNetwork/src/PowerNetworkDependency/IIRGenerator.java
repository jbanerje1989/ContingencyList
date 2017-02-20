package PowerNetworkDependency;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

// Refer the corresponding MATLAB file for format of the data retrieved
//Author: Joydeep Banerjee

public class IIRGenerator {
	
	// --------------------------------------------------------------------------------- //
	// Class Variables
	private final int numBuses;
	private final int numTimeSteps;
	private final int timeStep;
	private final int numBranch;
	public final String caseFile;
	private int[] busID;
	private int[] busType;
	private double[] busBaseKV;
	private double[] realLoad;
	private double[] imagLoad;
	private double[][] busVoltReal; // bus voltage at a given measurement time
	private double[][] busVoltImag;
	private HashMap<Integer,Integer> busNumToIndexMap; // map of the busID to a index in [0 , numBuses - 1]
	private HashMap<Integer, Integer> busIndexToNumMap; 
	private int[][] edge; // edge between buses
	private HashMap<Integer, String> busMap = new HashMap<Integer, String>(); // maintains a map of bus number to entityID
	private HashMap<Integer, String> transMap = new HashMap<Integer, String>(); // maintains a map of transmission line to entityID
	private ArrayList<ArrayList<Integer>> flowList = new ArrayList<ArrayList<Integer>>(); //maintains the adjacency list for flow between buses based on
	// voltage measurement at a given time step (the inverse flow direction is maintained for IIR generation)
	private ArrayList<Integer> loadBus = new ArrayList<Integer>(); // bus index of load buses
	private ArrayList<ArrayList<ArrayList<Integer>>> IIRs = new ArrayList<ArrayList<ArrayList<Integer>>>(); //store the IIRs
	private static int numPaths; //stores total number of paths when generating the IIRs
	private static int lengthPaths; // stores the length of each paths for all IIRs
	// --------------------------------------------------------------------------------- //
	
	// --------------------------------------------------------------------------------- //
	// Load Data from Generator Code in MATLAB 
	IIRGenerator(String caseVal, int time) throws FileNotFoundException{
		//Get Data from MATLAB
		caseFile = caseVal;
		File file = new File("InputFiles/" + caseFile + ".txt");
		Scanner scan = new Scanner(file);
		System.out.println(caseFile);
		// Initialize class variables
		numBuses = (int) scan.nextDouble();
		numTimeSteps = (int) scan.nextDouble();
		timeStep = time;
		busID = new int[numBuses];
		busType = new int[numBuses];
		busBaseKV = new double[numBuses];
		realLoad = new double[numBuses];
		imagLoad = new double[numBuses];
		busVoltReal = new double[numBuses][numTimeSteps];
		busVoltImag = new double[numBuses][numTimeSteps];
		busNumToIndexMap = new HashMap<Integer, Integer>();
		busIndexToNumMap = new HashMap<Integer, Integer>();
		numBranch = (int) scan.nextDouble();
		edge = new int[numBranch][2];
		
		// Load data into class variables
		for(int i = 0; i < numBuses; i++){
			busID[i] = (int) scan.nextDouble();
			busType[i] = (int) scan.nextDouble();
			realLoad[i] = scan.nextDouble();
			imagLoad[i] = scan.nextDouble();
			// System.out.println(realLoad[i] + " " + imagLoad[i]);
			busBaseKV[i] = scan.nextDouble();
			if(busBaseKV[i] == 0) busBaseKV[i] = 1;
			for(int j = 0; j < numTimeSteps; j++){
				busVoltReal[i][j] = busBaseKV[i] * scan.nextDouble();
				busVoltImag[i][j] = busBaseKV[i] * scan.nextDouble();
			}
		}
		
		//map the busID with index for processing
		for(int i = 0; i < numBuses; i++){
			busNumToIndexMap.put(busID[i], i);
			busIndexToNumMap.put(i, busID[i]);
		}
	    
		///get the edges corresponding to index
		for(int i = 0; i < edge.length; i++){
			int firstNode = (int) scan.nextDouble();
			int secondNode = (int) scan.nextDouble();
			edge[i][0] = busNumToIndexMap.get(firstNode);
			edge[i][1] = busNumToIndexMap.get(secondNode);
		}
		
		scan.close();
		
		// create map of buses with entity IDs
		int indexGen = 1;
		int indexLoad = 1;
		int indexNeutral = 1;
		for(int i = 0; i < numBuses; i++){
			double absLoad = Math.pow(realLoad[i],2) + Math.pow(imagLoad[i], 2);
			if((int) absLoad == 0 && busType[i] == 1){
				busMap.put(i, "N" + indexNeutral);
				indexNeutral ++;
			}
			else if(busType[i] == 2 || busType[i] == 3){
				busMap.put(i, "G" + indexGen);
				indexGen ++;
			}
			else if(busType[i] == 1){
				busMap.put(i, "L" + indexLoad);
				indexLoad ++;
				loadBus.add(i);
			}
		}
		
		//create map of transmission lines with entity ID
		for(int i = 0; i < edge.length; i++){
			String line1 = Integer.toString(edge[i][0]) + Integer.toString(edge[i][1]);
			String line2 = Integer.toString(edge[i][1]) + Integer.toString(edge[i][0]);
			int index2 = i + 1;
			if(!transMap.containsKey(Integer.parseInt(line1))) transMap.put(Integer.parseInt(line1), "T" + index2);
			if(!transMap.containsKey(Integer.parseInt(line2))) transMap.put(Integer.parseInt(line2), "T" + index2);
		}
	    
		//create adjacency list
		for(int i = 0; i < numBuses; i++){
			flowList.add(new ArrayList<Integer>());
		}
		
		for(int i = 0; i < edge.length; i++){
			// System.out.println(edge[i][0] +" " + edge[i][1]);
			double node1Volt = Math.pow(busVoltReal[edge[i][0]][timeStep - 1], 2) + 
					Math.pow(busVoltImag[edge[i][0]][timeStep - 1], 2);
			double node2Volt = Math.pow(busVoltReal[edge[i][1]][timeStep - 1], 2) + 
					Math.pow(busVoltImag[edge[i][1]][timeStep - 1], 2);
			// System.out.println(node1Volt + " " + node2Volt);
			
			if(node1Volt > node2Volt) {
				if(!flowList.get(edge[i][1]).contains(edge[i][0])) flowList.get(edge[i][1]).add(edge[i][0]);
				else continue;
			}
			else if(node1Volt < node2Volt) 
				if(!flowList.get(edge[i][0]).contains(edge[i][1])) flowList.get(edge[i][0]).add(edge[i][1]);
				else continue;
			else{
				if(Math.toDegrees(Math.atan2(busVoltReal[edge[i][0]][timeStep - 1], busVoltImag[edge[i][0]][timeStep -1])) > 
				Math.toDegrees(Math.atan2(busVoltReal[edge[i][1]][timeStep - 1], busVoltImag[edge[i][1]][timeStep - 1]))){
					if(!flowList.get(edge[i][1]).contains(edge[i][1])) flowList.get(edge[i][1]).add(edge[i][0]);
					else continue;
				}
				else{
					if(!flowList.get(edge[i][0]).contains(edge[i][1])) flowList.get(edge[i][0]).add(edge[i][1]);
					else continue;
				}
			}
		}
		// for(ArrayList<Integer> e : flowList) System.out.println(e);
	}
	// --------------------------------------------------------------------------------- //
	
	// --------------------------------------------------------------------------------- //
	// Generate IIRs for all files
	public static void generateIIRForAllFiles(int timeStep) throws IOException{
		File InputFolder = new File("InputFiles");
		for(File fileEntry : InputFolder.listFiles()){
			String file = fileEntry.getName().replaceAll(".txt", "");
			if(file.equals(".DS_Store")) continue;
			IIRGenerator obj = new IIRGenerator(file, timeStep);
			long startTime = System.nanoTime();
			obj.generateIIRTimeStep();
			long stopTime = System.nanoTime();
		    long elapsedTime = stopTime - startTime;
		    numPaths = 0;
			lengthPaths = 0;
			for(ArrayList<ArrayList<Integer>> IIR: obj.getIIRs()){
				if(numPaths < IIR.size()) numPaths = IIR.size();
				for(ArrayList<Integer> minterm: IIR){
					if(lengthPaths < minterm.size()) lengthPaths = minterm.size();
				}
			}
		    System.out.println(obj.getNumBuses() + " " + obj.getNumBranch() + " " + obj.getNumLoadBuses() 
		    	+ " " + numPaths + " " + lengthPaths + " " + (double) elapsedTime / 1000000.0);
			// obj.generateFilesForIIRs();
		}
	}
	// --------------------------------------------------------------------------------- //
	
	// --------------------------------------------------------------------------------- //
	//Generate the IIRs for the particular time step
	public void generateIIRTimeStep(){
		for(int bus: loadBus){
			ArrayList<ArrayList<Integer>> getIIR = getIIR(bus);
			// long startTime = System.nanoTime();
			IIRs.add(getIIR);
			// long stopTime = System.nanoTime();
		    // long elapsedTime = stopTime - startTime;
			// System.out.println(IIRs.get(IIRs.size()-1));
			// System.out.println((double) elapsedTime / 1000000.0);
		}
	}
	
	private ArrayList<ArrayList<Integer>> getIIR(int busNode){
		ArrayList<Integer> leftSide = new ArrayList<Integer>(); //leftSide of IIR
		leftSide.add(busNode);
		ArrayList<ArrayList<Integer>> IIR = new ArrayList<ArrayList<Integer>>();
		IIR.add(leftSide);
		// System.out.println(flowList.get(busNode));
		for(int bus: flowList.get(busNode)){
			ArrayList<ArrayList<Integer>> paths = new ArrayList<ArrayList<Integer>>();
			ArrayList<Integer> currentPath = new ArrayList<Integer>();
			currentPath.add(bus);
			getPaths(bus, paths, currentPath);
			for(ArrayList<Integer> minterm: paths){
				IIR.add(minterm);
			}
		}
		return IIR;
	}
	
	private void getPaths(int busNode, ArrayList<ArrayList<Integer>> paths, ArrayList<Integer> currentPath){
		if(flowList.get(busNode).size() == 0) {
			ArrayList<Integer> pathToAdd = new ArrayList<Integer>();
			for(int node: currentPath){
				pathToAdd.add(node);
			}
			paths.add(pathToAdd);
			currentPath.remove(currentPath.size() - 1);
			return;
		}
		else{
			for(int bus: flowList.get(busNode)){
				currentPath.add(bus);
 				getPaths(bus, paths, currentPath);
			}
			currentPath.remove(currentPath.size() - 1);
		}
	}
	// --------------------------------------------------------------------------------- //
	
	// --------------------------------------------------------------------------------- //
	// Generate the IIRs in proper representation form
	public void generateFilesForIIRs() throws IOException{
		StringBuilder sb = new StringBuilder();
		for(ArrayList<ArrayList<Integer>> IIR: IIRs){
			sb.append(busMap.get(IIR.get(0).get(0)) + " <- ");
			for(int j = 1; j < IIR.size(); j++){
				ArrayList<Integer> minterm = IIR.get(j);
				String firstLine = Integer.toString(IIR.get(0).get(0)) + Integer.toString(minterm.get(0));
				sb.append(transMap.get(Integer.parseInt(firstLine)) + " ");
				for(int i = 0; i < minterm.size() - 1; i++){
					sb.append(busMap.get(minterm.get(i)) + " ");
					String line = Integer.toString(minterm.get(i)) + Integer.toString(minterm.get(i+1));
					sb.append(transMap.get(Integer.parseInt(line)) + " ");
				}
				sb.append(busMap.get(minterm.get(minterm.size() - 1)));
				if(j != IIR.size() - 1) sb.append(" + ");
			}
			sb.append("\n");
		}
		// write IIRs generated to file
		File file = new File("OutputFiles/" + caseFile + "IIRsAtTimeStep" + timeStep +".txt");
		BufferedWriter writer = null;
		try {
		    writer = new BufferedWriter(new FileWriter(file));
		    writer.append(sb);
		} finally {
		    if (writer != null) writer.close();
		}
		
		// write the busID to a file
		sb = new StringBuilder();
		sb.append("---------- Bus IDs to Entity ID Map ---------------\n");
		for(int i = 0; i < numBuses; i++){
			sb.append(busIndexToNumMap.get(i) + " " + busMap.get(i) + "\n");
		}
		sb.append("\n---------- Branch edge to Entity ID Map ------------\n");
		for(int i = 0; i < edge.length; i++){
			String line = Integer.toString(edge[i][0]) + Integer.toString(edge[i][1]);
			sb.append(busIndexToNumMap.get(edge[i][0]) + " " + busIndexToNumMap.get(edge[i][1]) + " " +
					transMap.get(Integer.parseInt(line)) + "\n");
		}
		
		file = new File("OutputFiles/" + caseFile + "IDToEntityID.txt");
		try {
		    writer = new BufferedWriter(new FileWriter(file));
		    writer.append(sb);
		} finally {
		    if (writer != null) writer.close();
		}
		
	}
	// --------------------------------------------------------------------------------- //
	
	// --------------------------------------------------------------------------------- //
	// Getter Files
	public int getNumBuses() { return numBuses;}
	public ArrayList<ArrayList<ArrayList<Integer>>> getIIRs() { return IIRs;}
	public int getNumBranch() { return numBranch;}
	public int getNumLoadBuses(){ return loadBus.size(); }
	// --------------------------------------------------------------------------------- //
	
	// Main Method
	public static void main(String[] args) throws IOException{
		/* IIRGenerator obj = new IIRGenerator("case5", 1);
		long startTime = System.nanoTime();
		obj.generateIIRTimeStep();
		long stopTime = System.nanoTime();
	    long elapsedTime = stopTime - startTime;
	    System.out.println(obj.getNumBuses() + " " + (double) elapsedTime / 1000000.0);
		obj.generateFilesForIIRs(); */
		generateIIRForAllFiles(1);
		
	}
}
