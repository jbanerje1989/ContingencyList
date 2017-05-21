package PowerNetworkDependency;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DriverContingencyList {
	public static void main(String[] args) throws IOException{
		String caseFile = "case300";
		
		long startTime = System.nanoTime();
		IIRGenerator obj = new IIRGenerator(caseFile, 1);
		long endTime = System.nanoTime();
		long totalTime = endTime - startTime;
		double dataReadingTime = totalTime / Math.pow(10,6);
		int totalComp = obj.getTotalComp();
		
		startTime = System.nanoTime();
		obj.generateIIRForSolutions(1);
		endTime = System.nanoTime();
		totalTime = endTime - startTime;
		double IIRGenerationTime = totalTime / Math.pow(10,6);
		
		// This will take some time to run but will reduce the ILP space complexity by great extend
		int maxPathLength = obj.getMaximumPathLength();
		
		/* Just an initial run (results not stores) so that cache is filled with values which
		 * gives more accurate run time
		 */
		ILPContingencyList ob = new ILPContingencyList(1, obj, maxPathLength);
		ob.optimize(obj,1,new ArrayList<String>());
		
		List<Double> ilpExecTime = new ArrayList<Double>();
		List<Integer> compDeadILP = new ArrayList<Integer>();
		List<Double> heuExecTime = new ArrayList<Double>();
		List<Integer> compDeadHeu = new ArrayList<Integer>();
		for(int contingencyListK = 1; contingencyListK <= 5; ++ contingencyListK){
			startTime = System.nanoTime();
			ILPContingencyList ilpObject = new ILPContingencyList(contingencyListK, obj, maxPathLength);
			ilpObject.optimize(obj, 1, new ArrayList<String>());
			endTime   = System.nanoTime();
			totalTime = endTime - startTime;
			compDeadILP.add(ilpObject.printReport());
			ilpExecTime.add(totalTime / Math.pow(10,9));
			
			startTime = System.nanoTime();
			HeuristicContingencyList heuObject = new HeuristicContingencyList(contingencyListK, obj);
			heuObject.computeContingencyList();
			endTime   = System.nanoTime();
			ILPContingencyList ilpObject2 = new ILPContingencyList(contingencyListK, obj, maxPathLength);
			ilpObject2.optimize(obj, 2, heuObject.getInitFailedEntities());
			totalTime = endTime - startTime;
			compDeadHeu.add(ilpObject2.printReport());
			heuExecTime.add(totalTime / Math.pow(10,9));
		}
		
		System.out.println("----------------------------------------------------------------------------------");
		System.out.println("Report For " + caseFile);
		System.out.println("Data Reading Time for IIR generation: " + dataReadingTime + " ms");
		System.out.println("IIR Generation Time: " + IIRGenerationTime + " ms");
		System.out.println("Total Components: " + totalComp);
		System.out.println("Maximum Path Length in Flow Graph :" + maxPathLength);
		System.out.println("Components Dead in ILP: " + compDeadILP);
		System.out.println("ILP Execution Time (in sec): " + ilpExecTime);
		System.out.println("Components Dead in Heuristic: " + compDeadHeu);
		System.out.println("Heuristic Execution Time (in sec): " + heuExecTime);
		System.out.println("----------------------------------------------------------------------------------");
	}
}
