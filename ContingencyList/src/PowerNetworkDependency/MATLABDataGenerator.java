package PowerNetworkDependency;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import matlabcontrol.MatlabConnectionException;
import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;
import matlabcontrol.MatlabProxyFactory;

public class MATLABDataGenerator {
	
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
}
