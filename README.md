# ContingencyList

The project contains code for contingency List analysis using a new model of dependency called MIIR model (Paper Link: https://arxiv.org/abs/1705.07410). The source contains the following code.
1. Generation of dependency equations from MATPOWER for power network. MATLABDataGenerator.java calls MATPOWER data files (i.e. bus networks) to generate relevant data. This code is called once to generate data files for all required bus networks. The dependency equations and parameters for the MIIR model are generated using IIRGenerator.java. 
2. The ILP and heuristic solutions to the problem.
3. A driver code that takes in as input the system parameters to generate the ILP and heuristic solution
Note: A licensed version of IBM CPLEX optimizer is required for the ILP solution. I used a student license for the same.
