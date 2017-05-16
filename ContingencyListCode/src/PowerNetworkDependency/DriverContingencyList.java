package PowerNetworkDependency;

import java.io.IOException;

public class DriverContingencyList {
	public static void main(String[] args) throws IOException{
		String caseFile = "case118";
		int K = 5;
		
		long startTime = System.nanoTime();
		IIRGenerator obj = new IIRGenerator(caseFile, 1);
		long endTime = System.nanoTime();
		long totalTime = endTime - startTime;
		System.out.println("Data Reading Time for IIR generation: " + totalTime / Math.pow(10,6) + " ms");
		
		startTime = System.nanoTime();
		obj.generateIIRForSolutions(1);
		endTime = System.nanoTime();
		totalTime = endTime - startTime;
		System.out.println("IIR Generation Time: " + totalTime / Math.pow(10,6) + " ms");
		
		startTime = System.nanoTime();
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
		System.out.println("Contingency List: " + Object.getFailedEntities());
	}
}
