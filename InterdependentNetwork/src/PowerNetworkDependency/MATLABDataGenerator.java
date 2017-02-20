package PowerNetworkDependency;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import matlabcontrol.MatlabConnectionException;
import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;
import matlabcontrol.MatlabProxyFactory;

public class MATLABDataGenerator {
	
	// list of case files for which data is to be generated
	public static final String[] allCaseFiles = {
			"case3375wp", 
			"case9target",
			"case13659pegase", 
			"case2736sp", 
			"case30", 
			"case39", 
			"case14", 
			"case2737sop", 
			"case300", 
			"case6ww",	
			"case145", 
			"case2746wop", 
			"case3012wp", 
			"case5", 
			"case89pegase",
			"case2746wp", 
			"case30Q", 
			"case57", 
			"case9", 
			"case30pwl",
			"case9241pegase" };
	
	// Invoke MATLAB code to generate data
	MATLABDataGenerator(String caseVal) throws MatlabConnectionException, MatlabInvocationException{
		//Get Data from MATLAB
		MatlabProxyFactory factory = new MatlabProxyFactory();
		MatlabProxy proxy = factory.getProxy();
		proxy.eval("cd MatPower");
		proxy.eval("DataArray = DataGen('" + caseVal + "')");
		proxy.eval("cd ..");
		double[] data = (double[]) proxy.getVariable("DataArray");
		proxy.disconnect();
		
		// write data to a file
		try (
                PrintStream output = new PrintStream(new File("InputFiles/" + caseVal + ".txt"));
            ){
            for(int i =0;i<data.length;i++){
                output.println(data[i]);
            }
            output.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
	}
	
	public static void main(String[] args) throws MatlabConnectionException, MatlabInvocationException{
		for(String file: allCaseFiles){
			MATLABDataGenerator obj = new MATLABDataGenerator(file);
		}
	}
}
