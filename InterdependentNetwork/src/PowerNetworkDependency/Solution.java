package PowerNetworkDependency;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Scanner;

public class Solution {
	public int numEdges;
    public int numClusters;
    public static void main(String[] args) throws FileNotFoundException {
    	File file = new File("InputFiles/Input.txt");
		Scanner scan = new Scanner(file);
        Scanner in = new Scanner(System.in);
        int t = in.nextInt();
        System.out.println(t);
        System.out.println(1);
        for(int a0 = 0; a0 < t; a0++){
            int n = in.nextInt();
            int m = in.nextInt();
            ArrayList<Solution> sol = new ArrayList<Solution>(); 
            ArrayList<ArrayList<Integer>> edges = new ArrayList<ArrayList<Integer>>();
            System.out.println(1);
            for(int i = 0; i < n; i++) edges.add(new ArrayList<Integer>());
            for(int a1 = 0; a1 < m; a1++){
                int x = in.nextInt();
                int y = in.nextInt();
                // your code goes here
                edges.get(x-1).add(y-1);
                edges.get(y-1).add(x-1);
                System.out.println(x + " " + y);
            }
            // System.out.println(edges);
            boolean[] disc = new boolean[n];
            for(int i = 0; i < n; i++){
                if(!disc[i]){
                    ArrayList<Integer> cluster = new ArrayList<Integer>();
                    DFS(edges, disc, i, cluster);
                    Solution S = new Solution();
                    S.numClusters = cluster.size();
                    S.numEdges = numEdges(cluster, edges);
                    sol.add(S);
                }
            }
            Collections.sort(sol, new Comparator<Solution>() {
            @Override
                public int compare(Solution s2, Solution s1){
                    Integer x1 = s1.numClusters;
                    Integer x2 = s2.numClusters;
                    return  x1.compareTo(x2);
                }
            });
            int maxVal = 0;
            long returnVal = 0;
            int index = 0;
            for(Solution s: sol){
                long currentVal = 0;
                for(int i = 1; i < s.numClusters; i++) currentVal += i * (i+1);
                currentVal += ((s.numClusters-1) * s.numClusters * (s.numEdges - s.numClusters + 1));
                returnVal += currentVal;
                returnVal += ((long) maxVal * (long) s.numEdges);
                maxVal += (s.numClusters-1) * s.numClusters;   
                index++;
            }
            System.out.println(returnVal);
        }
    }
    
    public static void DFS(ArrayList<ArrayList<Integer>> edges, boolean[] disc, int vertex, ArrayList<Integer> cluster){
        cluster.add(vertex);
        disc[vertex] = true;
        for(int u: edges.get(vertex)){
            if(!disc[u]) DFS(edges, disc, u, cluster);
        }
    }
    
    public static int numEdges(ArrayList<Integer> cluster, ArrayList<ArrayList<Integer>> edges){
        int numEdge = 0;
        for(int v: cluster){
            numEdge += edges.get(v).size();
        }
        numEdge /= 2;
        return numEdge;
        /*long returnVal = 0;
        for(int i = 1; i < cluster.size(); i++) returnVal += (long) (i * (i+1));
        returnVal += (long) ((cluster.size() - 1) * cluster.size() * (numEdges - cluster.size() + 1));
        return returnVal;*/
    } 
}
