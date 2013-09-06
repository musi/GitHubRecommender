package netflix.algorithms.memorybased.Clustering;

import java.util.*;
import netflix.memreader.*;
import netflix.utilities.*;
import cern.colt.list.*;
import cern.colt.map.*;
import cern.colt.function.*;

/**
 * Class to convert a hashtable from uid to cluster
 * to an array of IntArrayLists representing each 
 * cluster. We make this a separate class so that 
 * it can be used with the forEachPair method in the 
 * OpenIntIntHashMap class in the colt package. 
 * Cerns code to iterate through a hashtable is 
 * almost certainly faster than mine would be. 
 *
 * @author Ben Sowell
 */


// This class has cluster collections. Each of its instance consists of k clusters.
// For each cluster, we have the IntArrayList which show the clusters index and int [], double []
// as the sum and count arrays of that cluster

// object --> k clusters --> each cluster --> [intArrayList (this is basic one), sum, count]
// with same index
/************************************************************************************************/
public class ClusterCollection implements IntIntProcedure 
/************************************************************************************************/
{
    private ArrayList<IntArrayList> clusters; 
    private int[] 					count;
    private double[] 				sum;
    private MemHelper 				helper;

   /************************************************************************************************/
    /**
    *
    * @param  k  Number of clusters. 
    */
    
    //make K clusters
    
    public ClusterCollection(int k, MemHelper helper)     
    {
        clusters 		= new ArrayList<IntArrayList>(k); //cluster of size K
        count 			= new int[k];
        sum 			= new double[k];
        this.helper 	= helper;

        for(int i = 0; i < k; i++)        
        {
            clusters.add(new IntArrayList());	// so we are adding IntArrayList (which represents a cluster) into cluster collection
            count[i] 	= 0;
            sum[i] 		= 0.0;
        }
    }
 
 /************************************************************************************************/
    
    public ClusterCollection(ArrayList<IntArrayList> clusters, MemHelper helper)    
    {
        this.helper 	= helper;
        this.clusters 	= clusters;

    }

  /************************************************************************************************/
    //return the [array of arrays(cluster)] --> clusters
    
    public ArrayList<IntArrayList> getClusters()     
    {
        return clusters;		//it has many small clusters
    }
    
  /************************************************************************************************/
    
    public IntArrayList getCluster(int cluster)     
    {
        return clusters.get(cluster);
    }

 /************************************************************************************************/
  // so we can define object of this class and then
  // can call object.getAverage(give the index of the cluster)  
    public double getAverage(int cluster)     
    {
        return sum[cluster] / count[cluster];   //so the index of sum and count should be the same as that of
        										// sum[index], count[index], where index = cluster number?
    }

 /************************************************************************************************/
    
    public int size()     
    {
        return clusters.size();
    }

 /************************************************************************************************/ 

    public int getClusterSize(int cluster)    
    {
        return clusters.get(cluster).size();
    }

 /************************************************************************************************/
    

    /**
     * Adds user first to cluster second. 
     *
     * @return  true (not used).
    */
 
   // clusters is recognised by the integer id  
   // In that id, we have sum and count of each cluster
   // count () ... is the rating sum in a cluster
   // sum() is the no. of movies in a cluster
    
    // we add a uid in a cluster
    // add (int element) --> Appends the specified element to the end of this list.
    // get(int index)-- > Returns the element at the specified position in the receiver.
    
    public boolean apply(int first, int second)    
    {      	
        clusters.get(second).add(first);						//so it shows, clusters have users only?, and it their complements
        														// have sum and count?
        sum[second] += helper.getRatingSumForUser(first);		//sum   = sum of ratings by all users in a cluster
        count[second] += helper.getNumberOfMoviesSeen(first);	//count = all movies seen by users in that cluster
            
        return true;
    }


 /************************************************************************************************/
    
    public void printClusters()    
    {       
        for(int i = 0; i < clusters.size(); i++) 
        {
        	  //  System.out.print("Cluster " + i + ": ");            
            
        	for(int j = 0; j < clusters.get(i).size(); j++)
        	{
            	//System.out.print(clusters.get(i).get(j) + " ");
        	}
                //System.out.println();
         }
    }
    
    
    
}

