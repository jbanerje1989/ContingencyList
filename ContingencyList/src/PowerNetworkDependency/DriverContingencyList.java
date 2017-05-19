package PowerNetworkDependency;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DriverContingencyList {
	public static void main(String[] args) throws IOException{
		String caseFile = "case9";
		
		long startTime = System.nanoTime();
		IIRGenerator obj = new IIRGenerator(caseFile, 1);
		long endTime = System.nanoTime();
		long totalTime = endTime - startTime;
		double dataReadingTime = totalTime / Math.pow(10,6);
		int totalComp = obj.getTotalComp() - 2;
		System.out.println("Data Reading Time for IIR generation: " + totalTime / Math.pow(10,6) + " ms");
		
		startTime = System.nanoTime();
		obj.generateIIRForSolutions(1);
		endTime = System.nanoTime();
		totalTime = endTime - startTime;
		double IIRGenerationTime = totalTime / Math.pow(10,6);
		System.out.println("IIR Generation Time: " + totalTime / Math.pow(10,6) + " ms");
		System.out.println("----------------------------------------------------------------------------------");
		
		// This will take some time to run but will reduce the ILP space complexity by great extend
		int maxPathLength = obj.getMaximumPathLength();
		
		List<Double> ilpExecTime = new ArrayList<Double>();
		List<Integer> compDeadILP = new ArrayList<Integer>();
		for(int contingencyListK = 1; contingencyListK <= 5; ++ contingencyListK){
			startTime = System.nanoTime();
			ILPContingencyList ilpObject = new ILPContingencyList(contingencyListK, obj, maxPathLength);
			ilpObject.optimize(obj);
			endTime   = System.nanoTime();
			totalTime = endTime - startTime;
			compDeadILP.add(ilpObject.printReport());
			ilpExecTime.add(totalTime / Math.pow(10,9));
		}
		
		System.out.println("----------------------------------------------------------------------------------");
		System.out.println("Report");
		System.out.println("Data Reading Time for IIR generation: " + dataReadingTime + " ms");
		System.out.println("IIR Generation Time: " + IIRGenerationTime + " ms");
		System.out.println("Total Components: " + totalComp);
		System.out.println("Maximum Path Length in Flow Graph :" + maxPathLength);
		System.out.println("Components Dead in ILP: " + compDeadILP);
		System.out.println("ILP Execution Time (in sec): " + ilpExecTime);
		System.out.println("----------------------------------------------------------------------------------");
		
		
		/*System.out.println(obj.getEntityPowerVal());
		System.out.println(obj.getToBus());
		for(String s: obj.getIIRSol().keySet())
			System.out.println(s + " " + obj.getIIRSol().get(s));*/
		
		/*startTime = System.nanoTime();
		HeuristicContingencyList Object = new HeuristicContingencyList(K, obj);
		endTime   = System.nanoTime();
		totalTime = endTime - startTime;
		System.out.println("Data Reading Time for Heuristic: " + totalTime / Math.pow(10,6) + " ms");
		
		startTime = System.nanoTime();
		Object.computeContingencyList();
		endTime   = System.nanoTime();
		totalTime = endTime - startTime;
		System.out.println("Heuristic Execution Time: " + totalTime / Math.pow(10,6) + " ms");
		
		System.out.println("Total Entities Failed: " +  Object.getFailedEntitiesCount());
		System.out.println("Contingency List: " + Object.getInitFailedEntities());
		System.out.println("Contingency List: " + Object.getFailedEntities());*/
	}
}
