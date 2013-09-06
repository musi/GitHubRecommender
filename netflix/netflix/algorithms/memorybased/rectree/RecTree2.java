package netflix.algorithms.memorybased.rectree;

import java.util.*;
import netflix.memreader.*;
import netflix.utilities.*;
import cern.colt.list.*;
import cern.colt.map.*;

/************************************************************************************************/
public class RecTree2 
/************************************************************************************************/

{

    private MemHelper helper;
    private final int MAX_ITERATIONS	= 20;
    private final int PARTION_MAX_SIZE 	= 400;
    private final int MAX_DEPTH 		= 2;

    private ArrayList<IntArrayList> 	finalClusters;
    private OpenIntIntHashMap 			uidToCluster;

    /**
     * Builds the RecTree and saves the resulting clusters.
     */
 /************************************************************************************************/
    
    public RecTree2(MemHelper helper) 
    
    {
        this.helper   = helper;
        finalClusters = new ArrayList<IntArrayList>(); //Creates ArrayList with initial default capacity 10.
        uidToCluster  = new OpenIntIntHashMap();       // <E> is for an element in the arraylist
    }

 /************************************************************************************************/ 
 //This is called after the constructor call 
    
    public void cluster()    
    {
        finalClusters = constructRecTree(helper.getListOfUsers(), 
                                         0, 
                                         helper.getGlobalAverage());
      
 /*   	
    	
    	finalClusters = constructRecTreeM(helper.getListOfUsers(), 
    									1, 
    									helper.getGlobalAverage());
*/
    	
        IntArrayList cluster;

        //This is basically to make a map, a particular user is in which cluster
        for(int i = 0; i < finalClusters.size(); i++) 
        {  cluster = finalClusters.get(i);				
           for(int j = 0; j < cluster.size(); j++)	//a cluster is a collection of users, go through this             
            {   uidToCluster.put(cluster.get(j), i);
            }
        }
    }

 /************************************************************************************************/
    

    /**
     * Gets the specified cluster by its positional id. 
     * @return  The cluster at location id in the clusters list.
     */
    
    public IntArrayList getClusterByID(int id)    
    {
        return finalClusters.get(id);
    }

 /************************************************************************************************/
    
    /**
     * Gets the id for the cluster containing the specified
     * user. 
     * @return  The location of the cluster containing
     *          the specified uid in the clusters list. 
     */
    
    public int getClusterIDByUID(int uid)    
    {
        return uidToCluster.get(uid);	//it will return the index of a single cluster within many (which are stored by index wise)
    }

/************************************************************************************************/
    
    /**
     * Gets the cluster containing the specified user. 
     * @return  The cluster containing the speficied user. 
     */

    public IntArrayList getClusterByUID(int uid)    
    {
        return finalClusters.get(uidToCluster.get(uid));	//it return the cluster
    }
   
/************************************************************************************************/
    
    //I think Rec tree is something other than the Kmeans? 
    
    public ArrayList<IntArrayList> constructRecTreeM(IntArrayList dataset,    // helper.getListOfUsers(),
                                                     int currDepth,           // 0,  
                                                     double cliqueAverage)    // helper.getGlobalAverage());                                              
    {
       ArrayList<IntArrayList> clusters = new ArrayList<IntArrayList>(currDepth);
               
       /*
       if(dataset.size() <= PARTION_MAX_SIZE || currDepth > MAX_DEPTH)       
       {
            clusters.add(dataset);
            return clusters;
       }
       
       */
       
 //       currDepth++;


        //KMean -->what this returns
        ClusterCollection subClusters = kMeans (dataset, 	
        										currDepth, 
        									    cliqueAverage);

       
       for(int i = 0; i < currDepth; i++) //for 2???       
       {
            clusters.add(subClusters.getCluster(i));  // recursive call
            		
        }

        
        //return subClusters;
        return clusters;
    }
    
        
    
 /************************************************************************************************/
    
    
    public ArrayList<IntArrayList> constructRecTree(IntArrayList dataset,    // helper.getListOfUsers(),
                                                    int currDepth,           // 0,  
                                                    double cliqueAverage)    // helper.getGlobalAverage());                                              
    {
       ArrayList<IntArrayList> clusters = new ArrayList<IntArrayList>();
               
       if(dataset.size() <= PARTION_MAX_SIZE || currDepth > MAX_DEPTH-1)       
       {
            clusters.add(dataset);
            System.out.println(" returned with condition: depth " + currDepth + ", max size = " + dataset.size() );
            return clusters;
       }
       
        currDepth++;


        //KMean -->returns the object of clusterColection initialised with 2 (K) clusters
        ClusterCollection subClusters = kMeans(dataset, 	
        										2, 
        									   cliqueAverage);
        
        
        
        for(int i = 0; i < 2; i++) //for 2???       
       {
    	   // recursive call
        	ArrayList<IntArrayList> myColl = (constructRecTree(subClusters.getCluster(i),	  //return an intArrayList
                                             currDepth, 
                                             subClusters.getAverage(i)));
        	//System.out.println(" added one with size = " + (subClusters.getCluster(i));
            clusters.addAll(myColl);
        }

        return clusters;
    }

 /************************************************************************************************/
    
    //It returns the cluster collection object
    
    public ClusterCollection kMeans(IntArrayList dataset, 		//all users in the database
    								int k, 					
                                    double cliqueAverage) 		//golbal average in the database    
    {
        int count = 0, newCluster = -1, point;        
        boolean converged = false;
        
        OpenIntIntHashMap clusterMap = new OpenIntIntHashMap();

        
        //Initialise the centroids as k random points in the dataset.
        Centroid[] centroids = chooseRandomCentroids(dataset, k);


//        Centroid[] centroids = new Centroid[2];
//        centroids[0] = new Centroid(1, helper);
//        centroids[1] = new Centroid(6, helper);

//        System.out.println("centroid 0 from: " + centroids[0].startingUid);
//        System.out.println("centroid 1 from: " + centroids[1].startingUid);

        //We can't update the current centroids during each iteration
        //because we need the old values, so we create new Centroid
        //objects to modify. 
        Centroid[] newCentroids;


        //Perform the clustering until the clusters converge or until
        //we reach the maximum number of iterations. 
        
    while(!converged && count < MAX_ITERATIONS)        
        //while(!converged)
    {
//          System.out.println("count " + count);
            converged 	 = true;
            newCentroids = new Centroid[k];
        
            for(int i = 0; i < k; i++)             
            {
                newCentroids[i] = new Centroid(centroids[i]);   	//assign the previously created centroid
            }

            //___________________________________________________
            
            //For every point in the dataset, find the closest
            //centroid. If this centroid is different from the 
            //points previously assigned cluster then the 
            //algorithm has not converged. 
            
            for(int i = 0; i < dataset.size(); i++)            
            {
                point = dataset.get(i); // a uid
                newCluster = findClosestCentroid(point, 			//This point is closest to this newCluster (return index of that centroid from centroids) 	
                								 centroids, 
                								 cliqueAverage);

                //This is the first pass through the data. We add
                //the point to the appropriate cluster, and update
                //the new version of that clusters centroid. 
                //Infact..... This point is not in the clusterMap (which contains point-to-cluster mappping)	
                //So what we do, is to add this point to this clusterMap and in newCentroids 's cluster (if it is not a starting point)
                

                if(!clusterMap.containsKey(point))     				//update the point to clusterMap           
                {
                    converged = false;
                    clusterMap.put(point, newCluster); 		

                    //If the centroid was initialised to this point, we don't 
                    //want to add it again. 
                    
                    if(centroids[newCluster].startingUid != point)  //update the point to newCentroid 
                    {
                        // System.out.println("Adding " + point + " to " + newCluster);
                        newCentroids[newCluster].addPoint(point, helper);	
                    }
                }

                
                //The point has changed clusters. We add the
                //point to the new cluster and modify the centroid
                //for both the new cluster and the old cluster. 
                //Infact....Here point is already there in the clusterMap, so we check that if this mapping has been changed or not
                //Mapping = (point, clusterid), If this has been changed in the current iteration (point is given a new cluster as in Kmean iterations)
                //then we have to do two things: Update clusterMap (point, new cluster) by deleting the previous map and mapping the new onr
                // and second, update this point in the newCentroid (Mean add this point to the new cluster as well)
                
                
                else if(clusterMap.get(point) != newCluster)        // this is because, this while is called multiple times        
                {
//                System.out.println("Current cluster is " + clusterMap.get(i));
//                System.out.println("moving " + point + " to " + newCluster);

                    newCentroids[clusterMap.get(point)].removePoint(point, helper);
//                  System.out.println("newCluster: " + newCluster);
                    newCentroids[newCluster].addPoint(point, helper);
                    converged = false;
                    clusterMap.put(point, newCluster);
                }
            }
            
            //Replace centroids with newCentroids and 
            //recompute the average for each one.
            //Infact....This particular cluster has been changed (means has added/deleted some points) and what we do is to update this cluster as well
            
            centroids = newCentroids;


            
            //TEMP: Goes through every point and finds the total distance
            //to the centroids. If everything is working correctly, this 
            //number should never increase. 
            
            double 	totalError = 0.0;
            int 	tempCluster;

            for(int i = 0; i < k; i++)					//Compute for all centroids            
            {
            	centroids[i].findAverage();				//compute average ratings in a centoid 
            
      //      	System.out.println("Centroid " + i);
      //        centroids[i].printRatings();
      //        System.out.println("New average: " + centroids[i].getAverage());
      //        System.out.println();
             }
            
            //As In the previous for, all points has been assigned to their respective clusters (depends on the distance)
            //So we can go through these points, find their particular cluster (by clusterMap) and the can compute 
            //the total distance from this point to that centroid
            //????......WE should not take into account the point for which we are taking the computing this distance
            //          in the distance  computation function? (it is because this point is already there in the cnetroid)
            
            for(int i=0; i < dataset.size(); i++)           
            {
                point = dataset.get(i);
                tempCluster =  clusterMap.get(point);
                totalError +=  centroids[tempCluster].distanceWithDefault(point, cliqueAverage, helper);
            }
         //     System.out.println("Total Error: " + totalError);
            
             //increment count
            count++;
     
     }//end of while
        
    
    
        ClusterCollection clusters = new ClusterCollection(k, helper);
        clusterMap.forEachPair(clusters); //???????????????
        
        return clusters;
    }

 /*********************************************************************************************/
    

    /**
     * Finds the closest centroid to a specified  //more close a user is to a centroid, the more is the sim or distance
     * user. 
     *
     * @param  uid  The user to find a centroid for.
     * @param  centroids  The list of centroids. 
     * @retrun The index of the closest centroid to uid. 
     */
    
    
    //what is really he wants to find here?, max value or min value?
    
    private int findClosestCentroid(int uid, 
    								Centroid[] centroids, 
                                    double cliqueAverage)     
    {
        double distance;
        double min = -1.0;
        int minIndex = -1;

        for(int i = 0; i < centroids.length; i++)        
        {
            distance = centroids[i].distanceWithDefault(uid, cliqueAverage, helper);

//                 System.out.println("distance from " + uid + " to cluster " + i + " is " + distance);

       //    if(Math.abs(distance) > min)  // we wanna find maximum distance        
            if((distance) > min)  // we wanna find maximum distance
            {
                min = distance;
                minIndex = i;
            }
        }
        
        return minIndex;
    }

/**********************************************************************************************/
    //It gives different RMSE each time, as we are using K random points each time
    
    /**
     * Randomly chooses k users to serve as intial centroids for 
     * the kMeans algorithm. 
     *
     * @param  dataset  The list uids. 
     * @param  k  The number of centroids (clusters) desired. 
     * @return A List of randomly chosen centroids. 
     */
    
    private Centroid[] chooseRandomCentroids(IntArrayList dataset,	//no of users in the database 
    										 int k					//how much clusters?
    										 )     
    {
        Random rand = new Random();
        Centroid[] centroids = new Centroid[k];		//array of centroid (which is a single cluster)
        
        //_______________________________
        //code to check duplicat eentries
        
        int alreadyThere[] = new int[k];
        int number;
        int myIndex=0;
        for (int m=0;m<k;m++)
        	alreadyThere[m]=-1;
        boolean ok=true;
        
        for(int i = 0; i < k; i++)         
        {
        
        	while(true)        		
        	{        		
        		number = rand.nextInt(dataset.size());
        		for(int m=0;m<i;m++)
        		{
        			if (number == alreadyThere[m]) { ok=false; break;}
        		}
        		
        		if(ok==true) break;
        	}
        	
        	//number = rand.nextInt(dataset.size());
        	
        	centroids[i] = new Centroid (number,helper); 		//repetition?
        	alreadyThere[i]=number;
            							 
        }

        return centroids;
    }


}

