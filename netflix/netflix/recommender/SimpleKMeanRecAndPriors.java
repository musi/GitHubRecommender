package netflix.recommender;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.*;
import netflix.algorithms.memorybased.rectree.*;
import netflix.memreader.*;
import netflix.rmse.RMSECalculator;
import netflix.utilities.*;
import cern.colt.list.*;
import cern.colt.map.*;

/************************************************************************************************/
public class SimpleKMeanRecAndPriors extends AbstractRecommender 
/************************************************************************************************/
{

    private RecTree2 						tree;
	private MyRecTree 						mixedTree;
	private SimpleKMean						simpleKTree;
	private SimpleKMeanPlus					simpleKPlusTree;
	private SimpleKMeanModifiedPlus			simpleKModifiedPlusTree;
	private SimpleKMeanPlusAndPower			simpleKPlusAndPowerTree;
	private SimpleKMeanPlusAndLogPower		simpleKPlusAndLogPowerTree;
	private double alpha;	// coff for log and power 				
	private double beta; 
	
    MemHelper 			helper;
    MemHelper 			allHelper;
    Timer227 			timer;
    
    private int 		totalNonRecSamples;	 //Total number of sample for which we did not recommend anything
    private int 		totalRecSamples;
    private int 		howMuchClusterSize;
    private double 		threshold = 0.1;
    private long 		kMeanTime;
    private double      kMeanEigen_Nmae;
    private double      kMeanRmse;
    private double      kMeanMae;
    private double      kMeanSensitivity;
    private double      kMeanCoverage; 
    private int         kClusters;
    BufferedWriter      writeData;
    private String      myPath;
    private String      SVDPath;
    private int         totalNan=0;
    private int         totalNegatives=0;
    private int			KMeansOrKMeansPlus; 
    
    
    //Answered
    private int totalPerfectAnswers;
    private int totalAnswers;
    
    
/************************************************************************************************/
    
    public SimpleKMeanRecAndPriors(String memReaderFile)    
    {
       //________________________
    	 totalNonRecSamples = 0;
    	 totalRecSamples 	= 0;
    	 howMuchClusterSize = 0;
    	 kMeanTime			= 0;    
    	 kMeanRmse 			= 0.0; 
    	 kMeanEigen_Nmae	= 0.0;
    	 kMeanMae			= 0.0;
    	 kMeanCoverage		= 0.0;
    	 alpha 				= 0.0; // start from 0.0
    	 beta 				= 1.0; //start from 1.0
    	 
    	 
    	 // Full Sets and sub-sets

    	 	   myPath = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\";
	      //   myPath = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\Sparsity\\";    	 
    	  //   myPath = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\FT\\TestTrain\\";
	    	   SVDPath= "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\SVD\\";
    	 
         timer  = new Timer227();
         helper = new MemHelper(memReaderFile);		//with train set        
         KMeansOrKMeansPlus = 0;
       //__________________________
        //___________________________
       /*
       timer.start();
       tree = new RecTree2(helper);
       tree.cluster();       
       timer.stop();
       System.out.println("Tree took " + timer.getTime() + " s to build");
        */
        //_____________________________
   /*   timer.start(); //Ramanan concept                
        mixedTree = new MyRecTree(helper);
        mixedTree.cluster(threshold);
        timer.stop();
        System.out.println("Tree took " + timer.getTime() + " s to build");
     */
        //_____________________________
         
         //Answers
         totalPerfectAnswers = 0;
         totalAnswers = 0;
    }

/************************************************************************************************/

/**
 *  It initialise an object and call the method for building the three 
 */
    public void callKTree()     
    {
        //-----------------------
    	// K-Means
    	//-----------------------
    	
    	if(KMeansOrKMeansPlus==1)
    	{
	    	timer.start();        
	        simpleKTree = new SimpleKMean(helper);        
	        simpleKTree.cluster(kClusters, 10);       
	        timer.stop();
	        
	        kMeanTime = timer.getTime();
	        System.out.println("KMeans Tree took " + timer.getTime() + " s to build");    	
	        timer.resetTimer();
	        //System.gc();
    	}
    	
        //-----------------------
    	// K-Means Plus
    	//-----------------------    	
        
    	
    	else if(KMeansOrKMeansPlus==2)
    	{
	        timer.start();        
	        simpleKPlusTree = new SimpleKMeanPlus(helper);        
	        simpleKPlusTree.cluster(kClusters, 10);       
	        timer.stop();
	        
	        kMeanTime = timer.getTime();
	        System.out.println("KMeans Plus Tree took " + timer.getTime() + " s to build");    	
	        timer.resetTimer();
	    }
        

    	//-----------------------
    	// K-Means Modified Plus
    	//-----------------------    	
    	//change : Vs and Prob as in KMenas++ paper
        
    	else if(KMeansOrKMeansPlus==3)
    	{
	        timer.start();        
	        simpleKModifiedPlusTree = new SimpleKMeanModifiedPlus(helper);        
	        simpleKModifiedPlusTree.cluster(kClusters, 10);       
	        timer.stop();
	        
	        kMeanTime = timer.getTime();
	        System.out.println("KMeans Modified Plus Tree took " + timer.getTime() + " s to build");    	
	        timer.resetTimer();
	    }    

    	//-----------------------
    	// K-Means Plus and 
    	// Log Power
    	//-----------------------    	
        
    	else if(KMeansOrKMeansPlus==5)
    	{
	        timer.start();        
	        simpleKPlusAndLogPowerTree = new SimpleKMeanPlusAndLogPower(helper, alpha, beta);        
	        simpleKPlusAndLogPowerTree.cluster(kClusters, 10);       
	        timer.stop();
	        
	        kMeanTime = timer.getTime();
	        System.out.println("KMeans Plus and Log Power Tree took " + timer.getTime() + " s to build");    	
	        timer.resetTimer();
	    }
    	    	
    	//-----------------------
    	// K-Means Plus and Power
    	//-----------------------    	
	
    	
    	else if(KMeansOrKMeansPlus==4)
    	{
	        timer.start();        
	        simpleKPlusAndPowerTree = new SimpleKMeanPlusAndPower(helper);        
	        simpleKPlusAndPowerTree.cluster(kClusters, 10);       
	        timer.stop();
	        
	        kMeanTime = timer.getTime();
	        System.out.println("KMeans Plus and Power Tree took " + timer.getTime() + " s to build");    	
	        timer.resetTimer();
	    }    	
    	    	
    }   
        
    
/************************************************************************************************/
    
    /**
     * Correlation weighting between two users
     * 
     * @param  mh the database to use
     * @param  activeUser the active user
     * @param  targetUser the target user
     * @return their correlation
     */
    
    private double correlation(int activeUser, int targetUser)    
    {
    	int amplifyingFactor = 1;			//give more weight if users have more than 50 movies in common
    	
    	double topSum, bottomSumActive, bottomSumTarget, rating1, rating2;
        topSum = bottomSumActive = bottomSumTarget = 0;
        double functionResult=0.0;
               
        double activeAvg = helper.getAverageRatingForUser(activeUser);
        double targetAvg = helper.getAverageRatingForUser(targetUser);
    
        ArrayList<Pair> ratings = helper.innerJoinOnMoviesOrRating(activeUser, targetUser, true);
		
        // Do the summations
        for(Pair pair : ratings)         
        {
            rating1 = (double)MemHelper.parseRating(pair.a) - activeAvg;
            rating2 = (double)MemHelper.parseRating(pair.b) - targetAvg;
			
            topSum += rating1 * rating2;
            bottomSumActive += Math.pow(rating1, 2);
            bottomSumTarget += Math.pow(rating2, 2);
        }
		
        double n = ratings.size() - 1;
        
        //So we get results even if they match on only one item
        //(Better than nothing, right?)
        if(n == 0)
            n++;
        
       // This handles an emergency case of dividing by zero
       if (bottomSumActive != 0 && bottomSumTarget != 0)
       { 
       	
       	functionResult = (1 * topSum) / Math.sqrt(bottomSumActive * bottomSumTarget);  //why multiply by n?
       	return  functionResult * (n/amplifyingFactor); //amplified send 
       	
       }
       
       else
        //   return 1;			// why return 1:?????
       	return 0;			// So in prediction, it will send average back 
       
    }

 /************************************************************************************************/
    
    /**
     * Basic recommendation method for memory-based algorithms.
     * 
     * @param user
     * @param movie
     * @return the predicted rating, or -99 if it fails (mh error)
     */
 
    //We call it for active user and a target movie
    public double recommend(int activeUser, int targetMovie, String date)    
    {
        double currWeight, weightSum = 0, voteSum = 0;
        int uid; 
        double  neighRating=0;
        IntArrayList simpleKUsers =null; 
        int limit = 50;
        
     // variable for priors, and sim * priors
	     double priors[] = new double[5];
	     double activePriorsMultipliedBySim[] = new double[5];
	     double neighPriorsMultipliedBySim[] = new double[5];
        
	     //Active User's class prior
	     double activeUserPriors[] = new double[5];
	     LongArrayList movies = helper.getMoviesSeenByUser(activeUser);         
         int moviesSize = movies.size();
         for (int i=0;i<moviesSize;i++)
         {                	
         	  int mid = MemHelper.parseUserOrMovie(movies.getQuick(i));
         	  double rating = helper.getRating(activeUser, mid);
         	  int index = (int) rating;
         	  activeUserPriors[index-1]++;
          	         	
         }

 		 for (int j=0;j<5;j++)
		 {
			 if(moviesSize!=0)				//divide by zero
				activeUserPriors[j]/=moviesSize;
			 
			 else activeUserPriors[j]= 0;
		 } 			
             
        //furthermore, This assume that active user is already there in the cluster, what about if a user is new
        //Don't have any rating? ... Or we can assume that he has rated let 2-3 movies, so in which cluster he
        //fits the best and then recommend him the movies. 
                
        //One more scenario is take active user, see his similarity with all the clusters and just take the weighted 
        //average of the clusters rather than users
        
                  
        //IntArrayList mixedUsers =  mixedTree.getClusterByUID(activeUser); 	  	  //mixed tree and K users
	   
	   //------------------------
	   //  neighbours priors
	   //------------------------
	   //Just to check how bad this approach can  be  
	   if(KMeansOrKMeansPlus ==0)
	   {
		   double priorsSim[] = new double[5];
		   LongArrayList tempUsers = allHelper.getUsersWhoSawMovie(targetMovie);
	 	   LongArrayList allUsers  = new LongArrayList();	 		
	 	  
	 		for(int i=0;i<tempUsers.size();i++)
	 		{
	 			allUsers.add(MemHelper.parseUserOrMovie(tempUsers.getQuick(i)));
	 		}	 	  
             
 		    //------------
			// priors
			//------------
			 for(int j = 0; j < allUsers.size(); j++)
			 {
				 uid = (int)allUsers.getQuick(j);	
				 
				 //find accumulation of sim
				 double mySim = correlation(activeUser, uid);
				 mySim = mySim+1;								//to avoid -ve
				 
				 neighRating = helper.getRating(uid, targetMovie);	
				 priors[(int)(neighRating-1)]++;
				 priorsSim[(int)(neighRating-1)]+=(mySim);		
				 
			 } //end of processing all users			 
			 
	 		 for (int j=0;j<5;j++)
			 {
				 if(allUsers.size()!=0)				//divide by zero
					 priors[j]/=allUsers.size();
				 
				 else priors[j]= 0;
				 
				 priors[j] *= priorsSim[j];
				 //priors[j] *=activeUserPriors[j];
				 
				// System.out.println("Priors =" + priors[j]);
			 }
								
   		    //sort the priors*sim of the neighbours
	 		double maxVal =0;
	 		double maxClass =0;
	 		for (int j=0;j<5;j++)
	 		{
	 			if(priors[j]>maxVal)
	 				{
	 					maxVal   = priors[j];	 			
	 					maxClass = j+1;
	 				}
	 		}
	 		
	 		//sort the  priors of the active user
	 		double activeUserMaxVal   = 0;
	 		double activeUserMaxClass = 0;
	 		for (int j=0;j<5;j++)
	 		{
	 			if(activeUserPriors[j]>activeUserMaxVal)
	 				{
	 				activeUserMaxVal   = activeUserPriors[j];	 			
	 				activeUserMaxClass = j+1;
	 				}
	 		}
	 			 		
	 		//-------------
	 		// A crude hack
	 		//-------------
	 		
	 		//return activeUserMaxClass;
	 		
	 		//See both max (activeUser's and neighbour's) 
	 	/*	if(maxClass == activeUserMaxClass)
	 			return maxClass;
	 		
	 		else*/	 		
	 		{
	 			double maxValFinal =0;
		 		double maxClassFinal =0;
		 		for (int j=0;j<5;j++)
		 		{
		 		//	System.out.println("priors=" + priors[j]);
		 		//	System.out.println("priors active =" + activeUserPriors[j]);
		 			priors[j] *= activeUserPriors[j];
		 			
		 			
		 			if(priors[j]>maxValFinal)
		 				{
		 					maxValFinal   = priors[j];	 			
		 					maxClassFinal = j+1;
		 				}
		 		}
		 		//System.out.println("------------------------------------");
		 		return maxClassFinal;
	 			
	 		}
	 			
	   }
	   
        //------------------------
        // KMeans 
        //------------------------
        
        if (KMeansOrKMeansPlus == 1)
        	{
        		simpleKUsers = simpleKTree.getClusterByUID(activeUser);                 //simpleK tree users
        		
        		int activeClusterID = simpleKTree.getClusterIDByUID(activeUser);
        		OpenIntDoubleHashMap simMap = new OpenIntDoubleHashMap();	//sim b/w an active user and the clusters
        		
        		// Find sim b/w a user and the cluster he lies in        		
        		double simWithMainCluster = simpleKTree.findSimWithOtherClusters(activeUser, activeClusterID );
        		
        		// Find sim b/w a user and all the other clusters
        		for(int i=0;i<kClusters; i++)
        		{
        			if(i!=activeClusterID)
        			{
        				double activeUserSim  = simpleKTree.findSimWithOtherClusters(activeUser, i );
        				simMap.put(i,activeUserSim );      					
        		   } 
        			
        		} //end for
        		
        		// Put the mainCluster sim as well
        		simMap.put(activeClusterID,simWithMainCluster );
        		
        		//sort the pairs (ascending order)
        		IntArrayList keys = simMap.keys();
        		DoubleArrayList vals = simMap.values();        		
        	    simMap.pairsSortedByValue(keys, vals);        		
        		int simSize = simMap.size();
        		LongArrayList tempUsers = allHelper.getUsersWhoSawMovie(targetMovie);
        		LongArrayList allUsers  = new LongArrayList();
        		
        		//System.out.println(" all users who saw movies ="+ tempUsers.size());
        		for(int i=0;i<tempUsers.size();i++)
        		{
        			allUsers.add(MemHelper.parseUserOrMovie(tempUsers.getQuick(i)));
        			//System.out.println("Actual Uids="+allUsers.get(i));
        		}       		
        		//-----------------------------------
        		// Find sim * priors
        		//-----------------------------------
        		// How much similar clusters to take into account? 
        		// Let us take upto a certain sim into account, e.g. (>0.10) sim
        		
        		int total = 0;        		
        		for (int i=simSize-1;i>=0;i--)
        		{
					IntArrayList myExtraNeighbours = simpleKTree.getClusterByID(i);
					//System.out.println("Extra Users="+myExtraNeighbours.size());
					
					int totalUsersWhoSawMovie = 0;
					double currentSim = vals.getQuick(i);
					
					//if(i>=
					{
						//------------
						// priors
						//------------
						 for(int j = 0; j < myExtraNeighbours.size(); j++)
						 {
							 uid = myExtraNeighbours.getQuick(j);
							 //System.out.println("uid in extraNeighbours="+uid);
							 
							 if(allUsers.contains(uid))				  // If user saw this movie
							 {
								 neighRating = helper.getRating(uid, targetMovie);	
								totalUsersWhoSawMovie++;
								priors[(int)(neighRating-1)]++;
							 }
							 
						 } //end of processing all users
						 
					//	 if(totalUsersWhoSawMovie>0)							
						/* System.out.println("totalUsersWhoSawMovie = "+totalUsersWhoSawMovie);
						 System.out.println("Sim = "+currentSim);
						 System.out.println("size = "+ myExtraNeighbours.size());*/
				    
						 
						 
						 // Make Priors
						 for (int j=0;j<5;j++)
						 {
							 if(totalUsersWhoSawMovie!=0)				//divide by zero
								 priors[j]/=totalUsersWhoSawMovie;
							 
							 else priors[j]= 0;
							// System.out.println("Priors =" + priors[j]);
						 }
												 
						//--------------
					    // priors * sim
						//--------------	
						 for (int j=0;j<5;j++)
						 {
							 neighPriorsMultipliedBySim[j]+= (priors[j] * currentSim);
							 activePriorsMultipliedBySim[j]+= (activeUserPriors[j] * currentSim);
						 }
												 						 
						 //------------
						 // Re-init
						 //------------
						 for (int j=0;j<5;j++)
						 {
							 priors[j]=0.0;
							 
						 }			 
					
	//					if(total++>70) break;
						
					} //end of if sim > threshold
        		} //end of for (Processing all clusters)		
               	
        		//Return the value back
        		double maxVal = 0;
        		double maxClass = 0;       
        		OpenIntDoubleHashMap myValsAndKeys = new OpenIntDoubleHashMap();
        		 
        		for (int j=0;j<5;j++)
        		 {
        			//myValsAndKeys.put(j+1, neighPriorsMultipliedBySim[j]);
        			  myValsAndKeys.put(j+1, neighPriorsMultipliedBySim[j] * activeUserPriors[j]);
        			//myValsAndKeys.put(j+1, neighPriorsMultipliedBySim[j] + activePriorsMultipliedBySim[j]);
        		 }
        		
        		IntArrayList myKeys = new IntArrayList();
        		DoubleArrayList myVals = new DoubleArrayList();
        		myValsAndKeys.pairsSortedByValue(myKeys, myVals);        		
        		      
        		//handle tie cases and return        		
        		IntArrayList tieKeys = new IntArrayList();
        		
        		 for (int j=4;j>=1;j--)
				 {    
        			 if(myVals.get(4)== myVals.get(j-1))
        			 {        				
           			 		tieKeys.add(myKeys.get(j-1));
        			 }
     
				 }
        		 
        		 tieKeys.add(myKeys.get(4));
        		 
        		 if(tieKeys.size() >1)		//we have tie cases	
        		 {
        			 double max = 0;
	        		 for(int j=0;j<tieKeys.size();j++)
	        		 {
	        			if(tieKeys.get(j)>max)			//send the max class to the output
	        				max = tieKeys.get(j);	        			 
	        		 }
	        		 
	        		 return max;
        		 }        
        		
        		if(1>0) return myKeys.get(4);			//only the winner one 
        	}
        
        //-----------------------
        //KMeans Plus
        //-----------------------
        
        else if (KMeansOrKMeansPlus == 2)        	
        	{
        		simpleKUsers = simpleKPlusTree.getClusterByUID(activeUser);            //simpleKPlus 
        		
        		int activeClusterID = simpleKPlusTree.getClusterIDByUID(activeUser);
        		OpenIntDoubleHashMap simMap = new OpenIntDoubleHashMap();	//sim b/w an active user and the clusters
        		
        		// Find sim b/w a user and the cluster he lies in        		
        		double simWithMainCluster = simpleKPlusTree.findSimWithOtherClusters(activeUser, activeClusterID );
        		
        		// Find sim b/w a user and all the other clusters
        		for(int i=0;i<kClusters; i++)
        		{
        			if(i!=activeClusterID)
        			{
        				double activeUserSim  = simpleKPlusTree.findSimWithOtherClusters(activeUser, i );
        				simMap.put(i,activeUserSim );      					
        		   } 
        			
        		} //end for
        		
        		// Put the mainCluster sim as well
        		simMap.put(activeClusterID,simWithMainCluster );
        		
        		//sort the pairs (ascending order)
        		IntArrayList keys = simMap.keys();
        		DoubleArrayList vals = simMap.values();        		
        	    simMap.pairsSortedByValue(keys, vals);        		
        		int simSize = simMap.size();
        		LongArrayList tempUsers = allHelper.getUsersWhoSawMovie(targetMovie);
        		LongArrayList allUsers  = new LongArrayList();
        		
        		//System.out.println(" all users who saw movies ="+ tempUsers.size());
        		for(int i=0;i<tempUsers.size();i++)
        		{
        			allUsers.add(MemHelper.parseUserOrMovie(tempUsers.getQuick(i)));
        			//System.out.println("Actual Uids="+allUsers.get(i));
        		}       		
        		//-----------------------------------
        		// Find sim * priors
        		//-----------------------------------
        		// How much similar clusters to take into account? 
        		// Let us take upto a certain sim into account, e.g. (>0.10) sim

        		int total =0;
        		for (int i=simSize-1;i>=0;i--)
        		{
					IntArrayList myExtraNeighbours = simpleKPlusTree.getClusterByID(i);
					//System.out.println("Extra Users="+myExtraNeighbours.size());
					
					int totalUsersWhoSawMovie = 0;
					double currentSim = vals.getQuick(i);
					
					//if(i>=
					{
						//------------
						// priors
						//------------
						 for(int j = 0; j < myExtraNeighbours.size(); j++)
						 {
							 uid = myExtraNeighbours.getQuick(j);
							 //System.out.println("uid in extraNeighbours="+uid);
							 
							 if(allUsers.contains(uid))				  // If user saw this movie
							 {
								neighRating = helper.getRating(uid, targetMovie);	
								totalUsersWhoSawMovie++;
								priors[(int)(neighRating-1)]++;
							 }
							 
						 } //end of processing all users
						 
					//	 if(totalUsersWhoSawMovie>0)							
						/* System.out.println("totalUsersWhoSawMovie = "+totalUsersWhoSawMovie);
						 System.out.println("Sim = "+currentSim);
						 System.out.println("size = "+ myExtraNeighbours.size());*/		    
						 
						 
						 // Make Priors
						 for (int j=0;j<5;j++)
						 {
							 if(totalUsersWhoSawMovie!=0)				//divide by zero
								 priors[j]/=totalUsersWhoSawMovie;
							 
							 else priors[j]= 0;
							// System.out.println("Priors =" + priors[j]);
						 }
												 
						//--------------
					    // priors * sim
						//--------------	
						 for (int j=0;j<5;j++)
						 {
							 neighPriorsMultipliedBySim[j]+= (priors[j] * currentSim);
							 activePriorsMultipliedBySim[j]+= (activeUserPriors[j] * currentSim);
							 
						 }
						
						 						 
						 //------------
						 // Re-init
						 //------------
						 for (int j=0;j<5;j++)
						 {
							 priors[j]=0.0;
							 
						 }			 
						 if(total++>70) break;
						
					}//end of if sim > threshold
        		} //end of for (Processing all clusters)		
               	
        		//Return the value back
        		double maxVal = 0;
        		double maxClass = 0;       
        		OpenIntDoubleHashMap myValsAndKeys = new OpenIntDoubleHashMap();
        		 
        		for (int j=0;j<5;j++)
        		 {
        			// myValsAndKeys.put(j+1, neighPriorsMultipliedBySim[j]);
        			   myValsAndKeys.put(j+1, neighPriorsMultipliedBySim[j] * activeUserPriors[j]);
        			// myValsAndKeys.put(j+1, neighPriorsMultipliedBySim[j] + activePriorsMultipliedBySim[j]);
        		 }
        		
        		IntArrayList myKeys = new IntArrayList();
        		DoubleArrayList myVals = new DoubleArrayList();
        		myValsAndKeys.pairsSortedByValue(myKeys, myVals);        		
        		      
        		//handle tie cases and return        		
        		IntArrayList tieKeys = new IntArrayList();
        		
        		 for (int j=4;j>=1;j--)
				 {    
        			 if(myVals.get(4)== myVals.get(j-1))
        			 {        				
           			 		tieKeys.add(myKeys.get(j-1));
        			 }
     
				 }
        		 
        		 tieKeys.add(myKeys.get(4));
        		 
        		 if(tieKeys.size() >1)		//we have tie cases	
        		 {
        			 double max = 0;
	        		 for(int j=0;j<tieKeys.size();j++)
	        		 {
	        			if(tieKeys.get(j)>max)			//send the max class to the output
	        				max = tieKeys.get(j);
	        			 
	        		 }
	        		 
	        		 return max;
        		 }        
        		
        		if(1>0) return myKeys.get(4);			//only the winner one 
        	}
       
       
        //-----------------------
        //simpleKPlus Log Power
        //-----------------------
        
        else  if (KMeansOrKMeansPlus == 3)        	
         { 		
        	
        	simpleKUsers = simpleKModifiedPlusTree.getClusterByUID(activeUser); 
       		
    		int activeClusterID = simpleKModifiedPlusTree.getClusterIDByUID(activeUser);
    		OpenIntDoubleHashMap simMap = new OpenIntDoubleHashMap();	//sim b/w an active user and the clusters
    		
    		// Find sim b/w a user and the cluster he lies in        		
    		double simWithMainCluster = simpleKModifiedPlusTree.findSimWithOtherClusters(activeUser, activeClusterID );
    		
    		// Find sim b/w a user and all the other clusters
    		for(int i=0;i<kClusters; i++)
    		{
    			if(i!=activeClusterID)
    			{
    				double activeUserSim  = simpleKModifiedPlusTree.findSimWithOtherClusters(activeUser, i );
    				simMap.put(i,activeUserSim );      					
    		   } 
    			
    		} //end for
    		
    		// Put the mainCluster sim as well
    		simMap.put(activeClusterID,simWithMainCluster );
    		
    		//sort the pairs (ascending order)
    		IntArrayList keys = simMap.keys();
    		DoubleArrayList vals = simMap.values();        		
    	    simMap.pairsSortedByValue(keys, vals);        		
    		int simSize = simMap.size();
    		LongArrayList tempUsers = allHelper.getUsersWhoSawMovie(targetMovie);
    		LongArrayList allUsers  = new LongArrayList();
    		
    		//System.out.println(" all users who saw movies ="+ tempUsers.size());
    		for(int i=0;i<tempUsers.size();i++)
    		{
    			allUsers.add(MemHelper.parseUserOrMovie(tempUsers.getQuick(i)));
    			//System.out.println("Actual Uids="+allUsers.get(i));
    		}       		
    		//-----------------------------------
    		// Find sim * priors
    		//-----------------------------------
    		// How much similar clusters to take into account? 
    		// Let us take upto a certain sim into account, e.g. (>0.10) sim

    		int total = 0;
    		for (int i=simSize-1;i>=5;i--)
    		{
				IntArrayList myExtraNeighbours = simpleKModifiedPlusTree.getClusterByID(i);
				//System.out.println("Extra Users="+myExtraNeighbours.size());
				
				int totalUsersWhoSawMovie = 0;
				double currentSim = vals.getQuick(i);
				
				//if(i>=
				{
					//------------
					// priors
					//------------
					 for(int j = 0; j < myExtraNeighbours.size(); j++)
					 {
						 uid = myExtraNeighbours.getQuick(j);
						 //System.out.println("uid in extraNeighbours="+uid);
						 
						 if(allUsers.contains(uid))				  // If user saw this movie
						 {
							 neighRating = helper.getRating(uid, targetMovie);	
							totalUsersWhoSawMovie++;
							priors[(int)(neighRating-1)]++;
						 }
						 
					 } //end of processing all users
					 
				//	 if(totalUsersWhoSawMovie>0)							
					/* System.out.println("totalUsersWhoSawMovie = "+totalUsersWhoSawMovie);
					 System.out.println("Sim = "+currentSim);
					 System.out.println("size = "+ myExtraNeighbours.size());*/
			    
					 
					 
					 // Make Priors
					 for (int j=0;j<5;j++)
					 {
						 if(totalUsersWhoSawMovie!=0)				//divide by zero
							 priors[j]/=totalUsersWhoSawMovie;
						 
						 else priors[j]= 0;
						// System.out.println("Priors =" + priors[j]);
					 }
											 
					//--------------
				    // priors * sim
					//--------------	
					 for (int j=0;j<5;j++)
					 {
						 neighPriorsMultipliedBySim[j]+= (priors[j] * currentSim);
						 activePriorsMultipliedBySim[j]+= (activeUserPriors[j] * currentSim);
						 
					 }
					
					 						 
					 //------------
					 // Re-init
					 //------------
					 for (int j=0;j<5;j++)
					 {
						 priors[j]=0.0;
						 
					 }			 
				
					 if(total++>70) break;
					
				}//end of if sim > threshold
    		} //end of for (Processing all clusters)		
           	
    		//Return the value back
    		double maxVal = 0;
    		double maxClass = 0;       
    		OpenIntDoubleHashMap myValsAndKeys = new OpenIntDoubleHashMap();
    		 
    		for (int j=0;j<5;j++)
    		 {
    			//myValsAndKeys.put(j+1, neighPriorsMultipliedBySim[j]);
    			// myValsAndKeys.put(j+1, neighPriorsMultipliedBySim[j] * activeUserPriors[j]);
    			myValsAndKeys.put(j+1, neighPriorsMultipliedBySim[j] + activePriorsMultipliedBySim[j]);
    		 }
    		
    		IntArrayList myKeys = new IntArrayList();
    		DoubleArrayList myVals = new DoubleArrayList();
    		myValsAndKeys.pairsSortedByValue(myKeys, myVals);        		
    		      
    		//handle tie cases and return        		
    		IntArrayList tieKeys = new IntArrayList();
    		
    		 for (int j=4;j>=1;j--)
			 {    
    			 if(myVals.get(4)== myVals.get(j-1))
    			 {        				
       			 		tieKeys.add(myKeys.get(j-1));
    			 }
 
			 }
    		 
    		 tieKeys.add(myKeys.get(4));
    		 
    		 if(tieKeys.size() >1)		//we have tie cases	
    		 {
    			 double max = 0;
        		 for(int j=0;j<tieKeys.size();j++)
        		 {
        			if(tieKeys.get(j)>max)			//send the max class to the output
        				max = tieKeys.get(j);
        			 
        		 }
        		 
        		 return max;
    		 }        
    		
    		if(1>0) return myKeys.get(4);		
        		
        		
         } //end of else if
       
               
        else  if (KMeansOrKMeansPlus == 4)        	
    	{
    		 simpleKUsers = simpleKPlusAndPowerTree.getClusterByUID(activeUser); 
       		
    		int activeClusterID = simpleKPlusAndPowerTree.getClusterIDByUID(activeUser);
    		OpenIntDoubleHashMap simMap = new OpenIntDoubleHashMap();	//sim b/w an active user and the clusters
    		
    		// Find sim b/w a user and the cluster he lies in        		
    		double simWithMainCluster = simpleKPlusAndPowerTree.findSimWithOtherClusters(activeUser, activeClusterID );
    		
    		// Find sim b/w a user and all the other clusters
    		for(int i=0;i<kClusters; i++)
    		{
    			if(i!=activeClusterID)
    			{
    				double activeUserSim  = simpleKPlusAndPowerTree.findSimWithOtherClusters(activeUser, i );
    				simMap.put(i,activeUserSim );      					
    		   } 
    			
    		} //end for
    		
    		// Put the mainCluster sim as well
    		simMap.put(activeClusterID,simWithMainCluster );
    		
    		//sort the pairs (ascending order)
    		IntArrayList keys = simMap.keys();
    		DoubleArrayList vals = simMap.values();        		
    	    simMap.pairsSortedByValue(keys, vals);        		
    		int simSize = simMap.size();
    		LongArrayList tempUsers = allHelper.getUsersWhoSawMovie(targetMovie);
    		LongArrayList allUsers  = new LongArrayList();
    		
    		//System.out.println(" all users who saw movies ="+ tempUsers.size());
    		for(int i=0;i<tempUsers.size();i++)
    		{
    			allUsers.add(MemHelper.parseUserOrMovie(tempUsers.getQuick(i)));
    			//System.out.println("Actual Uids="+allUsers.get(i));
    		}       		
    		//-----------------------------------
    		// Find sim * priors
    		//-----------------------------------
    		// How much similar clusters to take into account? 
    		// Let us take upto a certain sim into account, e.g. (>0.10) sim

    		int total =0;
    		for (int i=simSize-1;i>=5;i--)
    		{
				IntArrayList myExtraNeighbours = simpleKPlusAndPowerTree.getClusterByID(i);
				//System.out.println("Extra Users="+myExtraNeighbours.size());
				
				int totalUsersWhoSawMovie = 0;
				double currentSim = vals.getQuick(i);
				
				//if(i>=
				{
					//------------
					// priors
					//------------
					 for(int j = 0; j < myExtraNeighbours.size(); j++)
					 {
						 uid = myExtraNeighbours.getQuick(j);
						 //System.out.println("uid in extraNeighbours="+uid);
						 
						 if(allUsers.contains(uid))				  // If user saw this movie
						 {
							 neighRating = helper.getRating(uid, targetMovie);	
							totalUsersWhoSawMovie++;
							priors[(int)(neighRating-1)]++;
						 }
						 
					 } //end of processing all users
					 
				//	 if(totalUsersWhoSawMovie>0)							
					/* System.out.println("totalUsersWhoSawMovie = "+totalUsersWhoSawMovie);
					 System.out.println("Sim = "+currentSim);
					 System.out.println("size = "+ myExtraNeighbours.size());*/
			    
					 
					 
					 // Make Priors
					 for (int j=0;j<5;j++)
					 {
						 if(totalUsersWhoSawMovie!=0)				//divide by zero
							 priors[j]/=totalUsersWhoSawMovie;
						 
						 else priors[j]= 0;
						// System.out.println("Priors =" + priors[j]);
					 }
											 
					//--------------
				    // priors * sim
					//--------------	
					 for (int j=0;j<5;j++)
					 {
						 neighPriorsMultipliedBySim[j]+= (priors[j] * currentSim);
						 
					 }
					
					 						 
					 //------------
					 // Re-init
					 //------------
					 for (int j=0;j<5;j++)
					 {
						 priors[j]=0.0;
						 
					 }			 
					 
					 if(total++>70) break;
					
				}//end of if sim > threshold
    		} //end of for (Processing all clusters)		
           	
    		//Return the value back
    		double maxVal = 0;
    		double maxClass = 0;       
    		OpenIntDoubleHashMap myValsAndKeys = new OpenIntDoubleHashMap();
    		 
    		for (int j=0;j<5;j++)
    		 {
    			//myValsAndKeys.put(j+1, neighPriorsMultipliedBySim[j]);
    			myValsAndKeys.put(j+1, neighPriorsMultipliedBySim[j] * activeUserPriors[j]);
    		 }
    		
    		IntArrayList myKeys = new IntArrayList();
    		DoubleArrayList myVals = new DoubleArrayList();
    		myValsAndKeys.pairsSortedByValue(myKeys, myVals);        		
    		      
    		//handle tie cases and return        		
    		IntArrayList tieKeys = new IntArrayList();
    		
    		 for (int j=4;j>=1;j--)
			 {    
    			 if(myVals.get(4)== myVals.get(j-1))
    			 {        				
       			 		tieKeys.add(myKeys.get(j-1));
    			 }
 
			 }
    		 
    		 tieKeys.add(myKeys.get(4));
    		 
    		 if(tieKeys.size() >1)		//we have tie cases	
    		 {
    			 double max = 0;
        		 for(int j=0;j<tieKeys.size();j++)
        		 {
        			if(tieKeys.get(j)>max)			//send the max class to the output
        				max = tieKeys.get(j);
        			 
        		 }
        		 
        		 return max;
    		 }        
    		
    		if(1>0) return myKeys.get(4);		
    	}
      
        else  if (KMeansOrKMeansPlus == 5)        	
    	{
    		simpleKUsers = simpleKPlusAndLogPowerTree.getClusterByUID(activeUser);    //simpleKPlus Power 
    	}
     
       
        //---------------------------------------------------------------------------------------
        // Start Recommending
        //---------------------------------------------------------------------------------------
        
        
     //   IntArrayList treeUsers 		= tree.getClusterByUID(activeUser);		 	//simple tree users
     //   int userClusterIndex      	= tree.getClusterIDByUID(activeUser);
          LongArrayList tempUsers 		= allHelper.getUsersWhoSawMovie(targetMovie);
          IntArrayList userWhichSawThisMovie = new IntArrayList();
          
          for(int i = 0; i < tempUsers.size(); i++)
          {
        	  uid = MemHelper.parseUserOrMovie(tempUsers.getQuick(i));
        	  userWhichSawThisMovie.add(uid);
          }
          
         
          
          //----------------------------------------------
          // FILTER (where should we filter)?
          // Filter movies....i.e. less than 1 rating
          //---------------------------------------------- 
          
         // System.out.println ("uid, mid " + activeUser +","+ targetMovie);
          double recommendation   = 0.0;  
	  
          //----------------------------------------------
          // Go through all the users in that cluster
          //----------------------------------------------          
          
          OpenIntDoubleHashMap uidToWeight = new  OpenIntDoubleHashMap();
          IntArrayList myUsers      	   = new IntArrayList();
          DoubleArrayList myWeights 	   = new DoubleArrayList();
          int totalNeighbourFound   	   = 0;
         
          
	    for(int i = 0; i < simpleKUsers.size(); i++) //go through all the users in the cluster - (created by Kmean)
	     {
    		 uid = simpleKUsers.getQuick(i);    	 

          if (userWhichSawThisMovie.contains(uid)) 	//so this user has seen movie
           {       
        	  neighRating = helper.getRating(uid, targetMovie);	//get rating of ratings of each user for the target movie
             
            //If the user rated the target movie and the target
            //user is not the same as the active user. 
	            if(neighRating != -99 && uid != activeUser)
	             {              
	                currWeight = correlation(activeUser, uid);
	                uidToWeight.put(uid, currWeight);
	                weightSum += Math.abs(currWeight);
	                //voteSum += currWeight * (rating - helper.getAverageRatingForUser(uid));	
	                
	              } //end of if rating
             } //end of if user saw movie
           } // End of all users

	       myUsers 		= uidToWeight.keys();
	       myWeights 	= uidToWeight.values();
	       uidToWeight.pairsSortedByValue(myUsers, myWeights);
	       
	       //---------------------------
	       // get top weights and use
	       //---------------------------
	       	       
	       for (int i = totalNeighbourFound-1, myTotal=0; i >=0; i--, myTotal++)       
	       {    	   
	       		if(myTotal == limit) break;       	
	       		uid = myUsers.get(i);       	
	       
	   
     	 /*  	   // Taste approach
            	currWeight= (myWeights.get(i)+1);
            	weightSum += Math.abs(currWeight+1);
       	    	neighRating = mh.getRating(uid, targetMovie);        
         	    voteSum+= (currWeight* (neighRating  - mh.getAverageRatingForUser(uid))) ;
       
       	*/	
         	    //Simple, but do not take -ve into accounts
/*        		  currWeight= (myWeights.get(i));      	 
		       	  if (currWeight>0)
		       		{	
		       			weightSum += Math.abs(currWeight);      		
		           		neighRating = mh.getRating(uid, targetMovie);        
		           		voteSum+= ( currWeight* (neighRating  - mh.getAverageRatingForUser(uid))) ;
		       		} //end of weight should be positive
*/       
		       		// Take all weights into account		       		
		       		currWeight= (myWeights.get(i));	       			
		       		weightSum += Math.abs(currWeight);      		
		           	neighRating = mh.getRating(uid, targetMovie);        
		           	voteSum+= ( currWeight* (neighRating  - mh.getAverageRatingForUser(uid))) ;
		      }
	       
	       if (weightSum!=0)
	    	   voteSum *= 1.0 / weightSum;        
        
	       //-----------------------------
	       // Coverage?
	       //-----------------------------
	       
	       if (weightSum==0)				// If no weight, then it is not able to recommend????
	       { 
	    	   //   System.out.println(" errror =" + answer);
	           //   System.out.println(" vote sum =" +voteSum + ", weisghtSum ="+ weightSum);
	    	   
	         	 totalNan++;
	         	 return 0;	       
	       }
	       	       
	       double answer = helper.getAverageRatingForUser(activeUser) + voteSum;             
       // System.gc(); // It slows down the system to a great extent

        //------------------------
        // Send answer back
        //------------------------          
      
         if(answer<=0)
         {
        	 totalNegatives++;
        	  return helper.getAverageRatingForUser(activeUser);
        	// return answer;
         }
         
         else {
        	 totalRecSamples++;   
        	 return answer;
         }
         
    }

/************************************************************************************************/
    
    public static void main(String[] args)    
    {
    	
     double finalError=0.0;
    
     // Subset of SML
/*	  String test  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\Sparsity\\sml_TestSet20.dat";
//	  String base  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\Sparsity\\sml_TrainSet80.dat";
	  String base  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\Sparsity\\sml_trainSetStoredAll_80_40.dat";
*/	  
     //SML
      String test  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\sml_clusteringTestSetStoredTF.dat";
	  String base  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\sml_clusteringTrainSetStoredTF.dat";
	  
	  
/*     
     //ML
      String test  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\Clustering\\ml_clusteringTestSetStoredTF.dat";
	  String base  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\Clustering\\ml_clusteringTrainSetStoredTF.dat";
	  */
	  
      //FT
	  /*String test  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\FT\\TestTrain\\ft_clusteringTestSetStored.dat";
	  String base  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\FT\\TestTrain\\ft_clusteringTrainSetStored.dat";
	 */
	  
//    String test = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\SVD\\sml_TestSetStored.dat";
//	  String base = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\SVD\\sml_TrainSetStored.dat";
	
	  String mainFile = base;
	  
        
	    
	    //build the MemHelper based on the test set
	    MemHelper mh = new MemHelper(test);	  
	    
	    //create object and build memhelper of train set
	    SimpleKMeanRecAndPriors rec = new SimpleKMeanRecAndPriors(base);
	    rec.allHelper = new MemHelper(mainFile);				//with all set
	    
	/*   
	    //_____________________________
        // For Svd writing let 6 clusters
        //_____________________________ 
	    
		rec.kClusters = 6;
    	rec.callKTree ();
    */
	    
	    
	    //_____________________________
        // Make different clusters K
        //_____________________________ 
	    
	    rec.openFile();
	    for (int k=80;k<=100;k++)	    
	    {
	    	rec.alpha = 0.0;
	    	rec.beta  = 1.0;
	    	
	    	for (int version=1;version<=4;version++)
	    	{
	        
	    	//Build the tree based on training set only
	    	rec.kClusters = k;
	    	rec.KMeansOrKMeansPlus = version; 
	    	
	    	
	    	//----------------------
	    	// Call parametric KMean
	    	//----------------------
	    	
	    	   for (int j=0;j<12;)
	    	   {
	    		   rec.callKTree ();
	    			   
	    		   if (version ==4) {   rec.alpha +=0.10;
	    		   						rec.beta -=0.10;
	    		   						if(j==11) {rec.alpha =1.0; rec.beta=1.0;}
	    		   						
	    		   						j+=1;			// will run 11 times
	    		   					 }
	    		   else
	    		 
	    		   
	    		    j+=15;			//to break the loop	    		   
	    		   
	    	
	    		   
	    		    		    		    	
	    	long t1= System.currentTimeMillis();
	    	rec.timer.start();
	    	rec.applyOnTestSet(mh);
	    	rec.timer.stop();
	    	
	    	long totalTime= rec.timer.getMilliTime();
	    	
	    	try {
	    		rec.writeData.write(k+ "\t" + rec.kMeanTime + "\t" + (totalTime) + "\t" + rec.kMeanRmse );
	    		rec.writeData.newLine();
	    	}
	    	catch (Exception E)
	         {
	       	  System.out.println("error writing the file pointer of rec");
	       	  System.exit(1);
	         }
	    	 
	    	long t2= System.currentTimeMillis();	    	 

	    	//--------------------------------------------------------------------------------------------------- 	    	
	    	System.out.println(" Cluster = " + k+ ", Tree Time = " + rec.kMeanTime + ",Rec Time= " + (totalTime) + 
	    						", MAE =" + rec.kMeanMae + ", RMSE= " + rec.kMeanRmse + ", coverage= " + rec.kMeanCoverage);
	    	System.out.println("NMAE_EigenTaste =" + rec.kMeanEigen_Nmae);
	    	System.out.println("Sensitivity =" + rec.kMeanSensitivity);
	    	System.out.println("Coverage =" + rec.kMeanCoverage);
	    	System.out.println("Perfect Ans =" + (rec.totalPerfectAnswers *100.0)/rec.totalAnswers);
	    	
	    	if(version ==4)  System.out.println(" alpha =" + (rec.alpha -0.1) + ", beta ="+ (rec.beta+0.1) );
	    	System.out.println(" total rec time ="+ (t2-t1)*1.0/1000 + ", answered  = "+ rec.totalRecSamples + 
	    						", nan= "+ rec.totalNan+ ", -ve= "+ rec.totalNegatives);
	    	System.out.println("--------------------------------------------------------------------------------------------------- ");
	    	rec.timer.resetTimer();
	    	rec.totalRecSamples=0;
	    	rec.totalNan=0;
	    	rec.totalNegatives=0;
	    	
	    	   }// end of parametric for loop 
	      } //Which KMean is called
	    	//System.gc();
	   }
        
	    rec.closeFile();
 
        //_____________________________
        // Check for threshold in Kmeans
        //_____________________________
     
   /*
         
        
   for(double i=0.1;i<5; i+=0.1)
    {
	   if (i!=0.1) rec.callMixedTree(i);
	   
	   System.out.println("current threshol is" + i);       
	   
        for (int t=1;t < 20;t++)
        {
        	rec.howMuchClusterSize =t;
        	rec.totalRecSamples =0;
        	rec.totalNonRecSamples =0;
       
        	finalError = rec.testWithMemHelper(mh);
        	
        	
        	
          if(finalError<.96)        	
          {
        		System.out.println("---->RMSE: " + finalError);       	
        		System.out.print(", total rec samples: " + rec.totalRecSamples);
        		System.out.print(", total non rec samples: " + rec.totalNonRecSamples);
        		System.out.println(", cluster size: " + t);
        	}
        
            
        	if(Double.isNaN(finalError)) break;
        	
        }
     }

*/

    }

/***************************************************************************************************/
// This is called from RecTree with test set object
    
    public void applyOnTestSet(MemHelper testmh)     
    {
        RMSECalculator rmse = new RMSECalculator();
        
        IntArrayList users;
        LongArrayList  movies;
        
        String blank = "";
        int uid, mid, total=0;
        int totalUsers=0;
        
        // For each user, make recommendations
        users = testmh.getListOfUsers();
        totalUsers= users.size(); 
        //________________________________________
        
        for (int i = 0; i < totalUsers; i++)        
        {
            uid = users.getQuick(i);       
            movies = testmh.getMoviesSeenByUser(uid);
       //     System.out.println("now at " + i + " of total " + totalUsers );
            
            if(i>0 && i%200 ==0)
            	{
                	System.out.println("now at " + i + " of total " + totalUsers );
            		System.gc();            	
            	}
            	
            
            for (int j = 0; j < movies.size(); j++)             
            {
            	total++;
                mid = MemHelper.parseUserOrMovie(movies.getQuick(j));
                
           //     if (mid ==-1)  System.out.println(" rating error--> uid, mid -->" + uid + "," + mid );
                double rrr = recommend(uid, mid, blank);
                double myRating=0.0;
                
                //if (rrr!=0.0)                 
                      {       	
                	
                			myRating = testmh.getRating(uid, mid);			 		// get actual ratings?

                            if (myRating==-99 )                           
                               System.out.println(" rating error, uid, mid, rating" + uid + "," + mid + ","+ myRating);
                           
                            //-------------
                            //Add Error
                            //-------------
                            rmse.add(myRating,rrr);		
                            
                            if(myRating == rrr)
                            	totalPerfectAnswers++;
                            totalAnswers++;
                            //-------------
                            //Add Coverage
                            //-------------

                             rmse.addCoverage(rrr);
                            
                             //----------------
                             //Add Sensitivity
                             //----------------

                             rmse.ROC4(rrr, myRating, 5, helper.getAverageRatingForUser(uid));
                           
                		  }
            
            }
        }

        kMeanMae  = rmse.mae();
        kMeanRmse = rmse.rmse();
        kMeanEigen_Nmae  	= rmse.nmae_Eigen(1.0, 5.0);
        kMeanCoverage  = rmse.getItemCoverage();
        kMeanSensitivity = rmse.getAccuracy();        
        
        rmse.resetValues();        
     //   return dd;
    }
 
/***************************************************************************************************/
    
    //-----------------------------
    

    public void openFile()    
    {

   	 try {
   		   writeData = new BufferedWriter(new FileWriter(myPath + "kClustering.dat", true));   			
   	      } 
        
        catch (Exception E)
        {
      	  System.out.println("error opening the file pointer of rec");
      	  System.exit(1);
        }
        
        System.out.println("Rec File Created");
    }
    
    //----------------------------
    

    public void closeFile()    
    {
    
   	 try {
   		 	writeData.close();
   		  }
   	     
        catch (Exception E)
        {
      	  System.out.println("error closing the roc file pointer");
        }
        
    }

	@Override
	public double recommend(int uid, int mid, int neighbours) {
		// TODO Auto-generated method stub
		return 0;
	}
    

}