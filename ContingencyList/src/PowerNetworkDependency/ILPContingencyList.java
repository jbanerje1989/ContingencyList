package PowerNetworkDependency;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ilog.concert.*;
import ilog.cplex.*;

public class ILPContingencyList {
	private HashMap<String, Integer> entityLabeltoIndexMap; 
	private HashMap<Integer, String> entityIndextoLabelMap;
	private HashMap<String, Integer> mintermLabelToIndexMap;
	private HashMap<Integer, Integer> entityVal; 
	private HashMap<Integer, Integer> entityBound; 
	private HashMap<String, List<String>> IIRs;
	private int K;
	private int XCOUNT;
	private int CCOUNT;
	private int STEPS;
	int ii = 0;
	
	
	// ILP variables
	IloCplex cplex;
	private IloIntVar[][] x;	
	private IloIntVar[][] c;
	private IloNumVar[][] y;
	private double constM = 100.0;
		
	public ILPContingencyList(int KVal, IIRGenerator object, int maxPathLength) {
		try {
			entityLabeltoIndexMap = new HashMap<String, Integer>();
			entityIndextoLabelMap = new HashMap<Integer, String>();
			mintermLabelToIndexMap = new HashMap<String, Integer>();
			entityVal = new HashMap<Integer, Integer>();
			entityBound = new HashMap<Integer, Integer>();
			IIRs = new HashMap<String, List<String>>();
			int eIndex = 0;
			
			for(String str: object.getEntityPowerVal().keySet()){
				entityLabeltoIndexMap.put(str, eIndex);
				entityIndextoLabelMap.put(eIndex, str);
				
				// some load bus has negative load value. Set them to 0
				if(str.charAt(0) == 'L'){
					if(object.getEntityPowerBound().get(str) < 0){
						entityVal.put(eIndex, 0);
						entityBound.put(eIndex, 0);
					}
					else{
						entityVal.put(eIndex, object.getEntityPowerVal().get(str));
						entityBound.put(eIndex, object.getEntityPowerBound().get(str));
					}
					eIndex ++;
					continue;
				}
				
				// for transmission line and generators set max cap to max cap + 1 (as some max caps may be 0)
				entityVal.put(eIndex, object.getEntityPowerVal().get(str));
				if(str.charAt(0) == 'G' || str.charAt(0) == 'T')
					entityBound.put(eIndex, object.getEntityPowerBound().get(str) + 1);
				else
					entityBound.put(eIndex, object.getEntityPowerBound().get(str));
		
				eIndex ++;
			}
			int cTermIndex = 0;
			for(String str : object.getIIRSol().keySet()){
				List<String> min = new ArrayList<String>();
				for(List<String> minterm: object.getIIRSol().get(str)){
					String minString = minterm.get(0) + " " + minterm.get(1);
					mintermLabelToIndexMap.put(minString, cTermIndex);
					min.add(minString);
					cTermIndex ++;
				}
				IIRs.put(str, min);
			}
			cplex = new IloCplex();
			XCOUNT = entityLabeltoIndexMap.size();
			CCOUNT = mintermLabelToIndexMap.size();
			STEPS = maxPathLength + 2;
			K = KVal;
			x = new IloIntVar[XCOUNT][STEPS];
			c = new IloIntVar[CCOUNT][STEPS];
			y = new IloNumVar[XCOUNT][STEPS];
		} catch (Exception e) {
			System.out.println("Hello0");
			e.printStackTrace();
		}
	}
	
	public void optimize(IIRGenerator object, int num, List<String> initFailed) {
		try {
			createXVariables();
			createCVariables();
			createYVariables();
			createConstraints(object);
			createKConstraint(num, initFailed);
			createObjective();
			cplex.solve();	
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void createXVariables() {
		try {
			for (int i = 0; i < XCOUNT; ++i)				
				x[i] = cplex.intVarArray(STEPS, 0, 1);	
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}	
	}

	public void createCVariables() {
		try {
			for (int i = 0; i < CCOUNT; ++i)	
				c[i] = cplex.intVarArray(STEPS, 0, 1);	
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}	
	}
	
	public void createYVariables() {
		try {
			for (int i = 0; i < XCOUNT; ++i) 			
				y[i] = cplex.numVarArray(STEPS, 0, entityBound.get(i));
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}	
	}
	
	public void createObjective() {
		try {
			IloIntExpr expr = cplex.constant(0);
			for (int i = 0; i < XCOUNT; ++i)
				expr = cplex.sum(expr, x[i][STEPS-1]);
			cplex.addMaximize(expr);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	public void createKConstraint(int num, List<String> initFailed) {
		try{
			if(num == 1){
				// K Constraint
				IloNumExpr expr = cplex.constant(0);
				for (int i = 0; i < XCOUNT; ++ i)
					expr = cplex.sum(expr, x[i][0]);
				cplex.addLe(expr, K);
			}
			// Constraint for Heuristic
			else{
				IloNumExpr expr = cplex.constant(0);
				for(int i = 0; i < XCOUNT; ++i){
					expr = cplex.sum(expr, x[i][0]);
					if(initFailed.contains(entityIndextoLabelMap.get(i)))
						cplex.addGe(x[i][0],0.1);
				}
				cplex.addLe(expr, K);
			}
		}
		catch(Exception e){
			System.out.println("Error in K constraint");
		}
	}
	
	public void createConstraints(IIRGenerator obj) {
		try {
			IloNumExpr expr = cplex.constant(0);
			// time step constraints	
			for (int i = 0; i < XCOUNT; ++i)
				for (int t = 1; t < STEPS;t++) 
					cplex.addGe(x[i][t], x[i][t-1]);
			
			// Initial flow value and flow values of load and neutral bus constraint
			for (int i = 0; i < XCOUNT; ++ i) { 
				cplex.addEq(y[i][0], entityVal.get(i));
				if(entityIndextoLabelMap.get(i).charAt(0) == 'L' ||
					entityIndextoLabelMap.get(i).charAt(0) == 'N'){
					for (int t = 1; t < STEPS; ++t) {
						cplex.addEq(y[i][t], entityBound.get(i));
					}	
				}
			}
			
			/* Entity (generator and transmission line) operational value based on
			 * their flow value
			 */
			for (int i = 0; i < XCOUNT; ++i) {
				if(entityIndextoLabelMap.get(i).charAt(0) == 'L' ||
					entityIndextoLabelMap.get(i).charAt(0) == 'N')
					continue;
				expr = cplex.constant(0);
				for (int t = 1; t < STEPS; ++t) {
					expr = cplex.prod(y[i][t], 1 / ((double) entityBound.get(i)));
					cplex.addLe(x[i][t], expr);
				}
			}
			
			// Construct the power flow equation constraint
			for(String entity: entityLabeltoIndexMap.keySet()){
				if(entity.charAt(0) == 'T') continue;
				else if(entity.charAt(0) == 'G'){
					for(int t = 1; t < STEPS-1; ++t){
						expr = cplex.constant(0);
						IloNumExpr expr2 = cplex.constant(0);
						for(String str: obj.getFromBus().get(entity)){
							cplex.addLe(x[entityLabeltoIndexMap.get(str)][t], 
								x[entityLabeltoIndexMap.get(entity)][t]);
						
							expr = cplex.constant(0);
							expr = cplex.sum(expr, x[entityLabeltoIndexMap.get(str)][t]);
							expr = cplex.prod(expr, entityBound.get(entityLabeltoIndexMap.get(str)));
							expr = cplex.diff(y[entityLabeltoIndexMap.get(str)][t],expr);
							expr2 = cplex.sum(expr2,expr);
						}	
						expr = cplex.constant(0);
						expr = cplex.sum(expr, x[entityLabeltoIndexMap.get(entity)][t]);
						expr = cplex.prod(expr, entityBound.get(entityLabeltoIndexMap.get(entity)));
						expr = cplex.diff(y[entityLabeltoIndexMap.get(entity)][t],expr);
						cplex.addEq(expr, expr2);	
					}
				}
				else if(entity.charAt(0) == 'L'){
					for(int t = 1; t < STEPS-1; ++t){
						expr = cplex.constant(0);
						IloNumExpr expr2 = cplex.constant(0);
						for(String str: obj.getFromBus().get(entity)){
							cplex.addLe(x[entityLabeltoIndexMap.get(str)][t], 
								x[entityLabeltoIndexMap.get(entity)][t]);
							
							expr = cplex.constant(0);
							expr = cplex.sum(expr, x[entityLabeltoIndexMap.get(str)][t]);
							expr = cplex.prod(expr, entityBound.get(entityLabeltoIndexMap.get(str)));
							expr = cplex.diff(expr,y[entityLabeltoIndexMap.get(str)][t]);
							expr2 = cplex.sum(expr2,expr);
						}
						for(String str: obj.getToBus().get(entity)){
							expr = cplex.constant(0);
							expr = cplex.sum(expr, x[entityLabeltoIndexMap.get(str)][t+1]);
							expr = cplex.prod(expr, entityBound.get(entityLabeltoIndexMap.get(str)));
							expr = cplex.diff(y[entityLabeltoIndexMap.get(str)][t],expr);
							expr2 = cplex.sum(expr2,expr);
						}
						expr = cplex.constant(0);
						expr = cplex.sum(expr, x[entityLabeltoIndexMap.get(entity)][t+1]);
						expr = cplex.prod(expr, entityBound.get(entityLabeltoIndexMap.get(entity)));
						expr = cplex.diff(y[entityLabeltoIndexMap.get(entity)][t],expr);
						cplex.addGe(expr, expr2);
					}
				}
				else{
					for(int t = 1; t < STEPS-1; ++t){
						expr = cplex.constant(0);
						IloNumExpr expr2 = cplex.constant(0);
						for(String str: obj.getFromBus().get(entity)){
							cplex.addLe(x[entityLabeltoIndexMap.get(str)][t], 
								x[entityLabeltoIndexMap.get(entity)][t]);
						
							expr = cplex.constant(0);
							expr = cplex.sum(expr, x[entityLabeltoIndexMap.get(str)][t]);
							expr = cplex.prod(expr, entityBound.get(entityLabeltoIndexMap.get(str)));
							expr = cplex.diff(expr,y[entityLabeltoIndexMap.get(str)][t]);
							expr2 = cplex.sum(expr2,expr);
						}
						for(String str: obj.getToBus().get(entity)){
							expr = cplex.constant(0);
							expr = cplex.sum(expr, x[entityLabeltoIndexMap.get(str)][t]);
							expr = cplex.prod(expr, entityBound.get(entityLabeltoIndexMap.get(str)));
							expr = cplex.diff(y[entityLabeltoIndexMap.get(str)][t],expr);
							expr2 = cplex.sum(expr2,expr);
						}
						cplex.addEq(expr2,0);
					}
				}
			}

			// Generating constraints for IIRs
			for(String str: IIRs.keySet()){
				for(String minterms : IIRs.get(str)){
					for(int t = 1; t < STEPS; ++t){
						expr = cplex.constant(0);
						for(String entity: minterms.split(" ")){
							cplex.addGe(c[mintermLabelToIndexMap.get(minterms)][t], x[entityLabeltoIndexMap.get(entity)][t-1]);
							expr = cplex.sum(expr, x[entityLabeltoIndexMap.get(entity)][t-1]);
						}
						cplex.addLe(c[mintermLabelToIndexMap.get(minterms)][t], expr);
					}
				}
				for(int t = 1; t < STEPS; ++ t){
					IloNumExpr expr2 = cplex.constant(0);
					IloNumExpr expr3 = cplex.constant(0);
					double minCount = 0;
					for(String minterms : IIRs.get(str)){
						expr2 = cplex.sum(expr2, c[mintermLabelToIndexMap.get(minterms)][t]);
						minCount ++;
					}
					expr2 = cplex.prod(expr2, 1.0 / minCount);
					expr2 = cplex.sum(expr2, x[entityLabeltoIndexMap.get(str)][0]);
					cplex.addLe(x[entityLabeltoIndexMap.get(str)][t], expr2);
					expr3 = cplex.sum(expr3, minCount);
					expr3 = cplex.diff(expr3, expr2);
					expr3 = cplex.prod(expr3, constM);
					expr3 = cplex.sum(expr3, x[entityLabeltoIndexMap.get(str)][t]);
					cplex.addGe(expr3, 1);
				}
			}
		
			// For entities having no dependency relation
			for(String str : entityLabeltoIndexMap.keySet()){
				if(!IIRs.containsKey(str)){
					// If it is a transmission line then continue
					if(str.charAt(0) == 'T') continue;
					for (int t = 1; t < STEPS; ++t)
						cplex.addEq(x[entityLabeltoIndexMap.get(str)][t], x[entityLabeltoIndexMap.get(str)][0]);
				}
			}
			
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	
	public void printX() {
		try {
			System.out.println("\nX: ");
			for(int i = 0; i < XCOUNT; i++) {
				System.out.println();
				for (int j = 0; j < STEPS; j++) {
					System.out.print(cplex.getValue(x[i][j]) + " ");
				}
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}	    	 
	}
	
	public int printReport() {
		int compnentsDead = 0;
		int componenetsKilledInitially = 0;
		try {
			for(int i = 0; i < XCOUNT; i++)				 
				if (cplex.getValue(x[i][STEPS-1]) >0)
					compnentsDead ++;
			
			for(int i = 0; i < XCOUNT; i++)
				if (cplex.getValue(x[i][0]) >0)
					componenetsKilledInitially ++;
				
			
			System.out.println("Time Steps       : " + STEPS);
			System.out.println("Total Components : " + XCOUNT);
			System.out.println("Components Dead  : " + compnentsDead);
			System.out.println("Components Killed Initially  : " + componenetsKilledInitially);			
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return compnentsDead;
	}

	public int[] getInitialFailureX() {
		int[] r = new int[XCOUNT];
		try {
			for(int i = 0; i < XCOUNT; i++) {
				if (cplex.getValue(x[i][0]) > 0)
					r[i] = 1;
				else
					r[i] = 0;
			}
		}
		catch(Exception e) {
			System.out.println(e.getMessage());
		}
		
		return r;
	}
}

