package netflix.recommender;

import java.io.BufferedWriter;

import java.io.FileWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import netflix.algorithms.memorybased.rectree.*;
import netflix.memreader.*;
import netflix.rmse.RMSECalculator;
import netflix.utilities.*;
import cern.colt.list.*;
import cern.colt.map.*;

/************************************************************************************************/
public class SimpleKMeanRecWithCentroids extends AbstractRecommender 
/************************************************************************************************/
{
	//Objects of Cluster Schemes
    private RecTree2 						tree;
	private MyRecTree 						mixedTree;
	private SimpleKMean						simpleKTree;
	private SimpleKMeanPlus					simpleKPlusTree;
	private SimpleKMeanModifiedPlus			simpleKModifiedPlusTree;
	private SimpleKMeanPlusAndPower			simpleKPlusAndPowerTree;
	private SimpleKMeanPlusAndLogPower		simpleKPlusAndLogPowerTree;
	
	// coff for log and power
	private double alpha;													 				
	private double beta; 
	
	//MemHelper Objects
    MemHelper 			helper;
    MemHelper 			testHelper;    
    Timer227 			timer;
    NumberFormat 	    nf;
    
    
    private int 		totalNonRecSamples;	 //Total number of sample for which we did not recommend anything
    private int 		totalRecSamples;
    private int 		howMuchClusterSize;
    private double 		threshold = 0.1;
    
    private int         kClusters;
    BufferedWriter      writeData;
    private String      myPath;
    private String      SVDPath;
    private int         totalNan=0;
    private int         totalNegatives=0;
    private int			KMeansOrKMeansPlus;
    
    //----------------
    //For Results
    //----------------
    
    private long 		kMeanTime;							// KMeans results assigned
    private double      kMeanRmse;
    private double      kMeanMae;
    private double      kMeanEigen_Nmae;
    private double      kMeanCluster_Nmae;
    private double      kMeanSensitivity;
    private double      kMeanCoverage;   
    private double      kMeanPrecision[];				  // 6 values for [5,10,15,20,25,30]
    private double      kMeanRecall[];
    private double      kMeanF1[];   
    private OpenIntDoubleHashMap midToPredictions;		   //will be used for top_n metrics (pred*100, actual)
    private OpenIntObjectHashMap custToMoviePrediction;
    
    String 				KMeansOutputAccuracy;				// to store the output and print
    String 				KMeansOutputROC;					
    String 				KMeansOutputCoverage;				    
    String 				KMeansOutputBuildTime;			
    String 				KMeansOutputRecTime;
    String 				KMeansOutputPrecision;	
    String 				KMeansOutputRecall;	
    String 				KMeansOutputF1;	
    
    //first index:  version
    //second index: results, 25 per version
    double              array_MAE[][];	      				// array of results, got from diff folds
    double              array_NMAE[][];
    double              array_RMSE[][];
    double              array_Coverage[][];
    double              array_ROC[][];
    double              array_BuildTime[][];
    double              array_Precision[][];
    double              array_Recall[][];
    double              array_F1[][];
    
    double              mean_MAE[];	      					// Means of results, got from diff folds
    double              mean_NMAE[];						// for each version
    double              mean_RMSE[];
    double              mean_Coverage[];
    double              mean_ROC[];
    double              mean_BuildTime[];
    double              mean_Precision[];   
    double              mean_Recall[];   
    double              mean_F1[];   
    
    
    double              sd_MAE[];	      					// SD of results, got from diff folds
    double              sd_NMAE[];							// for each version
    double              sd_RMSE[];
    double              sd_Coverage[];
    double              sd_ROC[];
    double              sd_BuildTime[];
    double              sd_Precision[];   
    double              sd_Recall[];   
    double              sd_F1[];   
    
        
    //Answered
    private int totalPerfectAnswers;
    private int totalAnswers;
    
    
    
    
/************************************************************************************************/
    /**
     * Constructor
     */
   
    public SimpleKMeanRecWithCentroids()    
    {
     
    	//Initilaize variables
    	 totalNonRecSamples = 0;
    	 totalRecSamples 	= 0;
    	 howMuchClusterSize = 0;
    	 kMeanTime			= 0;    
    	 kMeanRmse 			= 0.0; 
    	 kMeanMae			= 0.0;
    	 kMeanEigen_Nmae	= 0.0;
    	 kMeanCluster_Nmae	= 0.0;
    	 kMeanCoverage		= 0.0;
      	 alpha 				= 0.0; 			// start from 0.0
    	 beta 				= 1.0; 			//start from 1.0 
    	 KMeansOrKMeansPlus = 0;
    	 
         timer  = new Timer227();                
         nf = new DecimalFormat("#.#####");	//upto 4 digits
         midToPredictions = new OpenIntDoubleHashMap();  
         custToMoviePrediction = new OpenIntObjectHashMap();			//(uid, (mid, rating)) 
         //Answers
         totalPerfectAnswers = 0;
         totalAnswers = 0;
         
         //Initialize strings of results
         KMeansOutputAccuracy ="";
         KMeansOutputROC ="";
         KMeansOutputCoverage = "";
         KMeansOutputBuildTime ="";			
         KMeansOutputRecTime ="";	
         KMeansOutputPrecision ="";
         KMeansOutputRecall ="";
         KMeansOutputF1 ="";
         
         //25 results per scheme, 5 fold, and 5 times per fold
         array_MAE  	 = new double[5][25];
         array_NMAE		 = new double[5][25];
         array_RMSE 	 = new double[5][25];
         array_Coverage  = new double[5][25];
         array_ROC 		 = new double[5][25];
         array_BuildTime = new double[5][25];
         array_Precision = new double[5][25];
         array_Recall 	 = new double[5][25];
         array_F1 		 = new double[5][25];
         
         //Initialize results, Mean and SD
         mean_MAE 		= new double[5];
         mean_NMAE 		= new double[5];
         mean_RMSE 		= new double[5];
         mean_Coverage 	= new double[5];
         mean_ROC 		= new double[5];
         mean_BuildTime = new double[5];
         mean_Precision	= new double[5];
         mean_Recall	= new double[5];
         mean_F1		= new double[5];
         
         
         sd_MAE 		= new double[5];
         sd_NMAE 		= new double[5];
         sd_RMSE 		= new double[5];
         sd_Coverage 	= new double[5];
         sd_ROC 		= new double[5];
         sd_BuildTime 	= new double[5];
         sd_Precision 	= new double[5];
         sd_Recall 		= new double[5];
         sd_F1		 	= new double[5];
    	 
         kMeanPrecision     = new double[6];		//topN; for six values of N (top5, 10, 15...30)
    	 kMeanRecall  		= new double[6];		// Most probably we wil use top10, or top20
    	 kMeanF1			= new double[6];
    	 
         
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
    	
    	if(KMeansOrKMeansPlus==0)
    	{
	    	timer.start();        
	        simpleKTree = new SimpleKMean(helper);        
	        simpleKTree.cluster(kClusters, 30);       
	        timer.stop();
	        
	        kMeanTime = timer.getTime();
	        System.out.println("KMeans Tree took " + kMeanTime + " s to build");    	
	        timer.resetTimer();
	        //System.gc();
    	}
    	
        //-----------------------
    	// K-Means Plus
    	//-----------------------    	
            	
    	else if(KMeansOrKMeansPlus==1)
    	{
	        timer.start();        
	        simpleKPlusTree = new SimpleKMeanPlus(helper);        
	        simpleKPlusTree.cluster(kClusters, 30);       
	        timer.stop();
	        
	        kMeanTime = timer.getTime();
	        System.out.println("KMeans Plus Tree took " + kMeanTime + " s to build");    	
	        timer.resetTimer();
	    }
        

    	//-----------------------
    	// K-Means Modified Plus
    	//-----------------------    	
    	//change : Vs and Prob as in KMenas++ paper
        
    	else if(KMeansOrKMeansPlus==2)
    	{
	        timer.start();        
	        simpleKModifiedPlusTree = new SimpleKMeanModifiedPlus(helper);        
	        simpleKModifiedPlusTree.cluster(kClusters, 30);       
	        timer.stop();
	        
	        kMeanTime = timer.getTime();
	        System.out.println("KMeans Modified Plus Tree took " + kMeanTime + " s to build");    	
	        timer.resetTimer();
	    }    

  
    	//-----------------------
    	// K-Means Plus and Power
    	//-----------------------    	
	
    	
    	else if(KMeansOrKMeansPlus==3)
    	{
	        timer.start();        
	        simpleKPlusAndPowerTree = new SimpleKMeanPlusAndPower(helper);        
	        simpleKPlusAndPowerTree.cluster(kClusters, 30);       
	        timer.stop();
	        
	        kMeanTime = timer.getTime();
	        System.out.println("KMeans Plus and Power Tree took " + kMeanTime + " s to build");    	
	        timer.resetTimer();
	    }    	
    	    	
      	//-----------------------
    	// K-Means Plus and 
    	// Log Power
    	//-----------------------    	
        
    	else if(KMeansOrKMeansPlus==4)
    	{
	        timer.start();        
	        simpleKPlusAndLogPowerTree = new SimpleKMeanPlusAndLogPower(helper, alpha, beta);        
	        simpleKPlusAndLogPowerTree.cluster(kClusters, 30);       
	        timer.stop();
	        
	        kMeanTime = timer.getTime();
	        System.out.println("KMeans Plus and Log Power Tree took " + kMeanTime + " s to build");    	
	        timer.resetTimer();
	    }
    	
    	//-----------------------
    	// Heuristics  
    	//-----------------------    	
        
    	else if(KMeansOrKMeansPlus==5)
    	{
	        timer.start();        
	        simpleKPlusAndLogPowerTree = new SimpleKMeanPlusAndLogPower(helper, alpha, beta);        
	        simpleKPlusAndLogPowerTree.cluster(kClusters, 30);       
	        timer.stop();
	        
	        kMeanTime = timer.getTime();
	        System.out.println("KMeans Plus and Log Power Tree took " + kMeanTime + " s to build");    	
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
	     double priorsMultipliedBySim[] = new double[5];
        
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
/*	   if(KMeansOrKMeansPlus ==0)
	   {
		   double priorsSim[] = new double[5];
		   LongArrayList tempUsers = helper.getUsersWhoSawMovie(targetMovie);
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
	 		if(maxClass == activeUserMaxClass)
	 			return maxClass;
	 		
	 		else	 		
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
	   */
        //------------------------
        // KMeans 
        //------------------------
        
        if (KMeansOrKMeansPlus == 0)
          {
        		simpleKUsers = simpleKTree.getClusterByUID(activeUser);                 //simpleK tree users
        		
        		int activeClusterID = simpleKTree.getClusterIDByUID(activeUser);
        		OpenIntDoubleHashMap simMap = new OpenIntDoubleHashMap();				//sim b/w an active user and the clusters
        		
        		// Find sim b/w a user and the cluster he lies in        		
        		// last arg determine which sim, = =vector sim
        		double simWithMainCluster = simpleKTree.findSimWithOtherClusters(activeUser, activeClusterID,0 );
        		
        		// Find sim b/w a user and all the other clusters
        		for(int i=0;i<kClusters; i++)
        		{
        			if(i!=activeClusterID)
        			{
        				double clusterRating = simpleKTree.getRatingForAMovieInACluster(i, targetMovie);        				
        				if(clusterRating!=0)
        				{
	        				double activeUserSim  = simpleKTree.findSimWithOtherClusters(activeUser, i, 0 );
	        				simMap.put(i,activeUserSim );
        				}
        			}        			
        		} //end for
        		
        		// Put the mainCluster sim as well
        		simMap.put(activeClusterID,simWithMainCluster );
        		
        		//sort the pairs (ascending order)
        		IntArrayList keys = simMap.keys();
        		DoubleArrayList vals = simMap.values();        		
        	    simMap.pairsSortedByValue(keys, vals);        		
        		int simSize = simMap.size();
        		LongArrayList tempUsers = helper.getUsersWhoSawMovie(targetMovie);
        		LongArrayList allUsers  = new LongArrayList();
        		
        		int total =0;
        		
        		for (int i=simSize-1;i>=0;i--)
        		{	
        			//Get a cluster id
        			int clusterId =keys.get(i);
        			
        			//Get currentCluster weight with the active user
        			double clusterWeight =vals.get(i);
        		//	System.out.println(" weight ("+ (clusterId)+") with i= " + i + ",-->"+ clusterWeight);
        			
					//Get rating, average given by a cluster
					double clusterRating = simpleKTree.getRatingForAMovieInACluster(clusterId, targetMovie);
					double clusterAverage = simpleKTree.getAverageForAMovieInACluster(clusterId, targetMovie);
					
//					System.out.println(" rating ="+clusterRating);
//		 	        System.out.println(" avg ="+ clusterAverage);
//		 	    	   
					
					//Prediction
		       		weightSum += Math.abs(clusterWeight);      		
		           	voteSum+= (clusterWeight*(clusterRating-clusterAverage)) ;
		           	
		           	if(total++ == 70) break;
		           	
        		
        		}// end of for
        		
		            if (weightSum!=0)
		 	    	   voteSum /= weightSum;        
		         
		 	       
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
		         //	 return 0; //we are unable to make prediction for this rating
		          }
		          
		          else {
		         	 totalRecSamples++;   
		         	 return answer;
		          }

          }     
        //-----------------------
        //KMeans Plus
        //-----------------------
        
        else  if (KMeansOrKMeansPlus == 1)        	
        	{
        		simpleKUsers = simpleKPlusTree.getClusterByUID(activeUser);            //simpleKPlus 
        		
        		int activeClusterID = simpleKPlusTree.getClusterIDByUID(activeUser);
        		OpenIntDoubleHashMap simMap = new OpenIntDoubleHashMap();	//sim b/w an active user and the clusters
        		
        		// Find sim b/w a user and the cluster he lies in        		
        		double simWithMainCluster = simpleKPlusTree.findSimWithOtherClusters(activeUser, activeClusterID,0 );
        		
        		// Find sim b/w a user and all the other clusters
        		for(int i=0;i<kClusters; i++)
        		{
        			if(i!=activeClusterID)
        			{
        				double clusterRating = simpleKPlusTree.getRatingForAMovieInACluster(i, targetMovie);        				
        				if(clusterRating!=0)
        				{
            				double activeUserSim  = simpleKPlusTree.findSimWithOtherClusters(activeUser, i,0 );
            				simMap.put(i,activeUserSim );
        				}      					
        		   } 
        			
        			
        			
        		} //end for
        		
        		// Put the mainCluster sim as well
        		simMap.put(activeClusterID,simWithMainCluster );
        		
        		//sort the pairs (ascending order)
        		IntArrayList keys = simMap.keys();
        		DoubleArrayList vals = simMap.values();        		
        	    simMap.pairsSortedByValue(keys, vals);        		
        		int simSize = simMap.size();
        		LongArrayList tempUsers = helper.getUsersWhoSawMovie(targetMovie);
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
        		
        		int total = 0 ;
        		for (int i=simSize-1;i>=0;i--)
        		{
        			//Get a cluster id
        			int clusterId =keys.get(i);
        			
        			//Get currentCluster weight with the active user
        			double clusterWeight =vals.get(i);
        			
					//Get rating, average given by a cluster
					double clusterRating = simpleKPlusTree.getRatingForAMovieInACluster(clusterId, targetMovie);
					double clusterAverage = simpleKPlusTree.getAverageForAMovieInACluster(clusterId, targetMovie);
					
					//Prediction
		       		weightSum += Math.abs(clusterWeight);      		
		           	voteSum+= (clusterWeight*(clusterRating-clusterAverage)) ;
		           	if(total++ == 70) break;
        		}
		            if (weightSum!=0)
		 	    	   voteSum /= weightSum;        
		         
		 	       
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
		         	//return 0; //we are unable to make prediction for this rating
		          }
		          
		          else {
		         	 totalRecSamples++;   
		         	 return answer;
		          }

        	} 		
        	
       
       
        //-----------------------
        //simpleKPlus Log Power
        //-----------------------
        
        else  if (KMeansOrKMeansPlus == 2)        	
         { 	      		
        	simpleKUsers = simpleKModifiedPlusTree.getClusterByUID(activeUser);            //simpleKPlus 
    		
    		int activeClusterID = simpleKModifiedPlusTree.getClusterIDByUID(activeUser);
    		OpenIntDoubleHashMap simMap = new OpenIntDoubleHashMap();	//sim b/w an active user and the clusters
    		
    		// Find sim b/w a user and the cluster he lies in        		
    		double simWithMainCluster = simpleKModifiedPlusTree.findSimWithOtherClusters(activeUser, activeClusterID,0 );
    		
    		// Find sim b/w a user and all the other clusters
    		for(int i=0;i<kClusters; i++)
    		{
    			if(i!=activeClusterID)
    			{
    				double clusterRating = simpleKModifiedPlusTree.getRatingForAMovieInACluster(i, targetMovie);        				
    				if(clusterRating!=0)
    				{
        				double activeUserSim  = simpleKModifiedPlusTree.findSimWithOtherClusters(activeUser, i,0 );
        				simMap.put(i,activeUserSim );
    				}      					
    		     } 
    			
    		
    			
    		} //end for
    		
    		// Put the mainCluster sim as well
    		simMap.put(activeClusterID,simWithMainCluster );
    		
    		//sort the pairs (ascending order)
    		IntArrayList keys = simMap.keys();
    		DoubleArrayList vals = simMap.values();        		
    	    simMap.pairsSortedByValue(keys, vals);        		
    		int simSize = simMap.size();
    		LongArrayList tempUsers = helper.getUsersWhoSawMovie(targetMovie);
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
    			//Get a cluster id
    			int clusterId =keys.get(i);
    			
    			//Get currentCluster weight with the active user
    			double clusterWeight =vals.get(i);
    			
				//Get rating, average given by a cluster
				double clusterRating = simpleKModifiedPlusTree.getRatingForAMovieInACluster(clusterId, targetMovie);
				double clusterAverage = simpleKModifiedPlusTree.getAverageForAMovieInACluster(clusterId, targetMovie);
				
				//Prediction
	       		weightSum += Math.abs(clusterWeight);      		
	           	voteSum+= (clusterWeight*(clusterRating-clusterAverage)) ;
	           	if(total++ == 70) break;
    		}
	            if (weightSum!=0)
	 	    	   voteSum /= weightSum;        
	         
	 	       
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
	         	// return 0; //we are unable to make prediction for this rating
	          }
	          
	          else {
	         	 totalRecSamples++;   
	         	 return answer;
	          }

    	        		
         } //end of else if
       
        //----------------------------
        // KPlusAndPower
        //----------------------------
               
        else  if (KMeansOrKMeansPlus == 3)        	
    	{
    		simpleKUsers = simpleKPlusAndPowerTree.getClusterByUID(activeUser); 
        	simpleKUsers = simpleKPlusAndPowerTree.getClusterByUID(activeUser);            //simpleKPlus 
    		
    		int activeClusterID = simpleKPlusAndPowerTree.getClusterIDByUID(activeUser);
    		OpenIntDoubleHashMap simMap = new OpenIntDoubleHashMap();	//sim b/w an active user and the clusters
    		
    		// Find sim b/w a user and the cluster he lies in        		
    		double simWithMainCluster = simpleKPlusAndPowerTree.findSimWithOtherClusters(activeUser, activeClusterID, 0 );
    		
    		// Find sim b/w a user and all the other clusters
    		for(int i=0;i<kClusters; i++)
    		{
    			if(i!=activeClusterID)
    			{
    				double clusterRating = simpleKPlusAndPowerTree.getRatingForAMovieInACluster(i, targetMovie);        				
    				if(clusterRating!=0)
    				{
        				double activeUserSim  = simpleKPlusAndPowerTree.findSimWithOtherClusters(activeUser, i,0 );
        				simMap.put(i,activeUserSim );
    				
    				}
    		   } 
    			
    		} //end for
    		
    		// Put the mainCluster sim as well
    		simMap.put(activeClusterID,simWithMainCluster );
    		
    		//sort the pairs (ascending order)
    		IntArrayList keys = simMap.keys();
    		DoubleArrayList vals = simMap.values();        		
    	    simMap.pairsSortedByValue(keys, vals);        		
    		int simSize = simMap.size();
    		LongArrayList tempUsers = helper.getUsersWhoSawMovie(targetMovie);
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
    			//Get a cluster id
    			int clusterId =keys.get(i);
    			
    			//Get currentCluster weight with the active user
    			double clusterWeight =vals.get(i);
    			
				//Get rating, average given by a cluster
				double clusterRating = simpleKPlusAndPowerTree.getRatingForAMovieInACluster(clusterId, targetMovie);
				double clusterAverage = simpleKPlusAndPowerTree.getAverageForAMovieInACluster(clusterId, targetMovie);
				
				//Prediction
	       		weightSum += Math.abs(clusterWeight);      		
	           	voteSum+= (clusterWeight*(clusterRating-clusterAverage)) ;
	           	if(total++ == 70) break;
    		}
	            if (weightSum!=0)
	 	    	   voteSum /= weightSum;        
	         
	 	       
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
	         	// return 0; //we are unable to make prediction for this rating
	          }
	          
	          else {
	         	 totalRecSamples++;   
	         	 return answer;
	          }
    	
    	} //end of if else
      
        //----------------------------
        // KPlusAndLogPower
        //----------------------------
        
        
        else  if (KMeansOrKMeansPlus == 4)        	
    	{
    		simpleKUsers = simpleKPlusAndLogPowerTree.getClusterByUID(activeUser);            //simpleKPlus 
    		
    		int activeClusterID = simpleKPlusAndLogPowerTree.getClusterIDByUID(activeUser);
    		OpenIntDoubleHashMap simMap = new OpenIntDoubleHashMap();	//sim b/w an active user and the clusters
    		
    		// Find sim b/w a user and the cluster he lies in        		
    		double simWithMainCluster = simpleKPlusAndLogPowerTree.findSimWithOtherClusters(activeUser, activeClusterID,0 );
    		
    		// Find sim b/w a user and all the other clusters
    		for(int i=0;i<kClusters; i++)
    		{
    			if(i!=activeClusterID)
    			{
    				double clusterRating = simpleKPlusAndLogPowerTree.getRatingForAMovieInACluster(i, targetMovie);        				
    				if(clusterRating!=0)
    				{
        				double activeUserSim  = simpleKPlusAndLogPowerTree.findSimWithOtherClusters(activeUser, i, 0 );
        				simMap.put(i,activeUserSim );
    				}
    			}
    			
    		} //end for
    		
    		// Put the mainCluster sim as well
    		simMap.put(activeClusterID,simWithMainCluster );
    		
    		//sort the pairs (ascending order)
    		IntArrayList keys = simMap.keys();
    		DoubleArrayList vals = simMap.values();        		
    	    simMap.pairsSortedByValue(keys, vals);        		
    		int simSize = simMap.size();
    		LongArrayList tempUsers = helper.getUsersWhoSawMovie(targetMovie);
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

    		int total=0;
    		for (int i=simSize-1;i>=0;i--)
    		{
    			//Get a cluster id
    			int clusterId =keys.get(i);
    			
    			//Get currentCluster weight with the active user
    			double clusterWeight =vals.get(i);
    			
				//Get rating, average given by a cluster
				double clusterRating = simpleKPlusAndLogPowerTree.getRatingForAMovieInACluster(clusterId, targetMovie);
				double clusterAverage = simpleKPlusAndLogPowerTree.getAverageForAMovieInACluster(clusterId, targetMovie);
				
				//Prediction
	       		weightSum += Math.abs(clusterWeight);      		
	           	voteSum+= (clusterWeight*(clusterRating-clusterAverage)) ;
	           	if(total++ == 70) break;
    		}
	            if (weightSum!=0)
	 	    	   voteSum /= weightSum;        
	         
	 	       
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
	         	//  return helper.getAverageRatingForUser(activeUser);	         	
	         	// return answer;
	         	 return 0; //we are unable to make prediction for this rating
	          }
	          
	          else {
	         	 totalRecSamples++;   
	         	 return answer;
	          }

    	}
     
       
        //---------------------------------------------------------------------------------------
        // Start Recommending
        //---------------------------------------------------------------------------------------
        
        
     //   IntArrayList treeUsers 		= tree.getClusterByUID(activeUser);		 	//simple tree users
     //   int userClusterIndex      	= tree.getClusterIDByUID(activeUser);
          LongArrayList tempUsers 		= helper.getUsersWhoSawMovie(targetMovie);
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
		           	neighRating = helper.getRating(uid, targetMovie);        
		           	voteSum+= ( currWeight* (neighRating  - helper.getAverageRatingForUser(uid))) ;
		      }
	       
	       if (weightSum!=0)
	    	   voteSum /= weightSum;        
        
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
    /**
     * Main, will just have dataset paths, and we will call another function for generating 
     * recommendations
     */
    		
    public static void main(String[] args)    
    {
        // Subset of SML
    	/*	  String test  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\Sparsity\\sml_TestSet20.dat";
//    		  String base  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\Sparsity\\sml_TrainSet80.dat";
    		  String base  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\Sparsity\\sml_trainSetStoredAll_80_40.dat";
    	*/	  
    	     //SML
	          String path  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\FiveFoldData\\";
    	      String test  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\sml_clusteringTestSetStoredTF.dat";
    		  String base  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\sml_clusteringTrainSetStoredTF.dat";    		  
    		    	     
    	     //ML
    	/*    String test  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\Clustering\\ml_clusteringTestSetStoredTF.dat";
    		  String base  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\Clustering\\ml_clusteringTrainSetStoredTF.dat";
    		  */
    		  
    	      //FT
    		  /*String test  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\FT\\TestTrain\\ft_clusteringTestSetStored.dat";
    		  String base  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\FT\\TestTrain\\ft_clusteringTrainSetStored.dat";
    		 */
    		  
//    	      String test = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\SVD\\sml_TestSetStored.dat";
//    		  String base = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\SVD\\sml_TrainSetStored.dat";
    		
    		  String mainFile = base;
    		  
    		//create object and build memhelper of train set
    		  SimpleKMeanRecWithCentroids rec = new SimpleKMeanRecWithCentroids();   		     	
    		  rec.generateRecommendations(rec,path);
    }
    
 /************************************************************************************************/
 
    /**
     * Method which call cluster build-up, generate recommendations etc
     * @param SimpleKMeanRecWithCentroids object
     * @param String path     
     */
    
    public void generateRecommendations(SimpleKMeanRecWithCentroids rec, String path)
    {
    	double finalError=0.0;      	
    	String trainFile="";		//main file
    	String testFile ="";		//test file
	    	
	    
	    //Go through how much cluster we want
	    for (int k=5;k<=10;k+=5)	    
	    {	
	    	System.out.println("------------------K="+ k+ "----------------------");
	    	
	    	//initialise variables
	    	alpha	  = 0.0;
	    	beta  	  = 1.0;
	    	kClusters = k;
	    	
	    	KMeansOutputAccuracy+=k + ", ";
	    	KMeansOutputROC+=k + ", ";
	    	KMeansOutputCoverage=k+ ", ";
	    	KMeansOutputBuildTime+=k + ", ";
	    	KMeansOutputPrecision+=k + ", ";
	    	KMeansOutputRecall=k+ ", ";
	    	KMeansOutputF1+=k + ", ";
	    	KMeansOutputRecTime+=k +", ";	    	
	    	
	    //For different schemss of clusters	
	    for (int version=0;version<5;version++)
	    {
	    	//counter, to tell which result we are writing
	        int resultNumber = 0;
	        
		     //For 5 Folds
		    for(int fold=0;fold<5;fold++)
		    {	
		      	//Create train and test files, different for each fold
		    	trainFile = path +  "sml_trainSetStoredFold" + (fold+1)+ ".dat";
		    	testFile	= path +  "sml_testSetStoredFold" + (fold+1)+ ".dat";
			    	
		    	//build the MemHelper based on the test set, and train set
			    testHelper = new MemHelper(testFile);
			    helper = new MemHelper(trainFile);				
			    
			      //Repeat each fold for 5 times, as centroid selection have effect on the results
			     for(int iteration=0;iteration<5;iteration++) 
			  	  {	       
			    		KMeansOrKMeansPlus = version;			    		
			    		
			    		//Build Clusters, based on training set
			    		callKTree();
			    		
			    		//Check Cluster on test set
				    	long t1= System.currentTimeMillis();
				    	timer.start();
				    	applyOnTestSet(testHelper);
				    	timer.stop();				    	
				    	long totalTime= timer.getMilliTime();
				    	long t2= System.currentTimeMillis();
				    	
				    	/* try {
				    		writeData.write(k+ "\t" + kMeanTime 
				    									+ "\t" + (totalTime) + "\t"  
				    									+ kMeanRmse );
				    		writeData.newLine();
				    	}
				    	catch (Exception E){
				       	  System.out.println("error writing the file pointer of rec");
				       	  System.exit(1);
				         } */
				    	 		    	
						//---------------------------------------------------------------------------------------------------
					    // Start writing output
					    //--------------------------------------------------------------------------------------------------- 	    	
				    	
				    		/*	    	System.out.println(" Cluster = " + k+ ", Tree Time = " + kMeanTime 
					    							+ ",Rec Time= " + (totalTime)  
					    							+ ", MAE =" + kMeanMae + ", RMSE= " + kMeanRmse);
					    	
					    	*/
					    	
					    	
					    	/*System.out.println("NMAE_EigenTaste =" + rec.kMeanEigen_Nmae);
					    	System.out.println("NMAE_Cluster =" + rec.kMeanCluster_Nmae);	    		    	
					    	System.out.println("Perfect Ans =" + (rec.totalPerfectAnswers *100.0)/rec.totalAnswers);
					    	*/
					    	
					/*    	System.out.println("Coverage =" + kMeanCoverage);
					    	System.out.println("Sensitivity =" + kMeanSensitivity);
					    	if(version ==4)  System.out.println(" alpha =" + (alpha -0.1) + ", beta ="+ (beta+0.1) );
					    	System.out.println(" total rec time ="+ (t2-t1)*1.0/1000 + ", answered  = "+ totalRecSamples + 
					    						", nan= "+ rec.totalNan+ ", -ve= "+ totalNegatives);
					    	System.out.println("-------");*/
							
				    /*	for(int d=0;d<5;d++)
				    	{
				    	    System.out.println("precision["+d+"]="+kMeanPrecision[d]);
				    	    System.out.println("Recall["+d+"]="+kMeanRecall[d]);
				    	    System.out.println("F1["+d+"]="+kMeanF1[d]);
				    	}*/   
					    	array_MAE[version][resultNumber]= kMeanMae;
					    	array_RMSE[version][resultNumber]= kMeanRmse;
					    	array_ROC[version][resultNumber]= kMeanSensitivity;
					    	array_BuildTime[version][resultNumber]= kMeanTime;
					    	array_Precision[version][resultNumber]= kMeanPrecision[1];
					    	array_Recall[version][resultNumber]= kMeanRecall[1];
					    	array_F1[version][resultNumber]= kMeanF1[1];
					    	resultNumber++;					    	
					    	
					    	timer.resetTimer();
					    	totalRecSamples=0;
					    	totalNan=0;
					    	totalNegatives=0; 
			  	  }//end of repetition for
		    }//end of fold for
	  	
			//calculate mean
	    	mean_MAE[version] = calculateMeanOrSD (array_MAE[version], 25, 0);
	    	mean_RMSE[version] = calculateMeanOrSD (array_RMSE[version], 25, 0);
	    	mean_BuildTime[version] = calculateMeanOrSD (array_BuildTime[version], 25, 0);
	    	mean_ROC[version] = calculateMeanOrSD (array_ROC[version], 25, 0);
	    	mean_Precision[version] = calculateMeanOrSD (array_Precision[version], 25, 0);
	    	mean_Recall[version] = calculateMeanOrSD (array_Recall[version], 25, 0);
	    	mean_F1[version] = calculateMeanOrSD (array_F1[version], 25, 0);
	    
	       //calculate SD
	    	sd_MAE[version] = calculateMeanOrSD (array_MAE[version], 25, 1);
	    	sd_RMSE[version] = calculateMeanOrSD (array_RMSE[version], 25, 1);
	    	sd_BuildTime[version] = calculateMeanOrSD (array_BuildTime[version], 25, 1);
	    	sd_ROC[version] = calculateMeanOrSD (array_ROC[version], 25, 1);		    	
	    	sd_Precision[version] = calculateMeanOrSD (array_Precision[version], 25, 1);
	    	sd_Recall[version] = calculateMeanOrSD (array_Recall[version], 25, 1);
	    	sd_F1[version] = calculateMeanOrSD (array_F1[version], 25, 1);
	    
    	System.out.println("--------------------------------------------------------------------------------------------------- ");    	
        System.out.println("version="+ version+ ", MAE ="+			mean_MAE[version] + 		",SD="+ sd_MAE[version]);
        System.out.println("version="+ version+ ", Build Time ="+ 	mean_BuildTime[version] +	",SD="+ sd_BuildTime[version]);
        System.out.println("version="+ version+ ", ROC ="+ 			mean_ROC[version] +			",SD="+ sd_ROC[version]);
        System.out.println("version="+ version+ ", Precision ="+ 	mean_Precision[version] + 	",SD="+ sd_Precision[version]);
        System.out.println("version="+ version+ ", Recall ="+		mean_Recall[version] + 		",SD="+ sd_Recall[version]);
        System.out.println("version="+ version+ ", F1 ="+ 			mean_F1[version] + 			",SD="+ sd_F1[version]);
        System.out.println("--------------------------------------------------------------------------------------------------- ");
        
	  	KMeansOutputAccuracy+=nf.format(mean_MAE[version]) +		":"+ nf.format(sd_MAE[version]) + "; ";
    	KMeansOutputROC+=nf.format(mean_ROC[version]) + 			":"+ nf.format(sd_ROC[version]) + "; ";
    	KMeansOutputBuildTime+=nf.format(mean_BuildTime[version]) + ":"+ nf.format(sd_BuildTime[version]) + "; ";    	
    	KMeansOutputPrecision+=nf.format(mean_Precision[version]) + ":"+ nf.format(sd_Precision[version]) + "; ";
    	KMeansOutputRecall+=nf.format(mean_Recall[version]) + 		":"+ nf.format(sd_Recall[version]) + "; ";
    	KMeansOutputF1+=nf.format(mean_F1[version]) + 				":"+ nf.format(sd_F1[version]) + "; ";

    	KMeansOutputCoverage+=nf.format(kMeanCoverage) + "; ";
    	
    	    	 	
    	
	     } // end of version for
	    		    
	    	//System.gc();	    	
	    	KMeansOutputAccuracy+="\n";
	    	KMeansOutputROC+="\n";
	    	KMeansOutputCoverage="\n";
	    	KMeansOutputCoverage="\n";	    	
	    	KMeansOutputBuildTime+="\n";
	    	KMeansOutputRecTime+="\n";
	    	KMeansOutputPrecision+="\n";
	    	KMeansOutputRecall+="\n";
	    	KMeansOutputF1+="\n";
	    	
	    	
	   } //end of No. of cluster for
 
	    
	    //----------------
	    // print result 	    
	    //----------------

	    System.out.println(KMeansOutputAccuracy);
	    System.out.println(KMeansOutputROC);
	    System.out.println(KMeansOutputCoverage);
	    System.out.println(KMeansOutputBuildTime);
	    System.out.println(KMeansOutputRecTime);
	    System.out.println(KMeansOutputPrecision);
	    System.out.println(KMeansOutputRecall);
	    System.out.println(KMeansOutputF1);
	    
	    
	    
	    
	    
//-------------------------------------------------------------------------------------------------
	    	    
	   
	    // closeFile();
 
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
    
    /**
     * calculate Mean or SD of array of values
     * @param double[], values 
     * @param int, no of values
     * @param int, 0=mean and 1=sd
     * @return mean or sd
     */
	    
	 public double calculateMeanOrSD(double val[], int size, int whatToCalculate)
	 {
		 double mean =0;
		 double sd =0;
		 double ans =0;
		 
		 //calculate mean
		 for (int i=0;i<size;i++)
		 {
			 mean +=val[i];
		 }
		 
			mean= mean/size;			//This is mean
			
		 //choose what to claculate based on flag
		 if(whatToCalculate ==0)//mean
			 ans = 	mean; 
			 
		 else //SD
		 {
			 for(int i=0;i<size;i++)
			 {
				 sd+= Math.pow((val[i] - mean), 2);
			 }
			 
			 if(size==1)
				 ans= Math.sqrt(sd);
			 else
				 ans= Math.sqrt(sd/(size-1));
		 }
		
		 return ans;
	 }
	    
    
/***************************************************************************************************/
// This is called from RecTree with test set object
    
    public void applyOnTestSet(MemHelper testmh)     
    {
        RMSECalculator rmse = new RMSECalculator();
    	OpenIntDoubleHashMap temp;
    	    	
        
        IntArrayList users;
        LongArrayList  movies;
        
        String blank = "";
        int uid, mid, total=0;
	   	double mov, pred,actual, uAvg;
	    int totalUsers=0;
     
        // For each user, make recommendations
        users = testmh.getListOfUsers();
        totalUsers= users.size();
        
        double uidToPredictions[][] = new double[totalUsers][101]; // 1-49=predictions; 50-99=actual; (Same order); 100=user average
         
        //________________________________________
        
        for (int i = 0; i < totalUsers; i++)        
        {
            uid = users.getQuick(i);       
            movies = testmh.getMoviesSeenByUser(uid);
       //     System.out.println("now at " + i + " of total " + totalUsers );
            
            if(i>0 && i%200 ==0)
             {
               	//System.out.println("now at " + i + " of total " + totalUsers );
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
                           
                            if(rrr!=0){
                            	rmse.add(myRating,rrr);                            	
                            	midToPredictions.put(mid, rrr);                            	                                
                            }
                            
                            //----------------
                            //Add Sensitivity
                            //----------------
                            if(rrr!=0)
                            	rmse.ROC4(rrr, myRating, 5, helper.getAverageRatingForUser(uid));
                            
                            if(myRating == rrr)
                            	totalPerfectAnswers++;
                            totalAnswers++;
                            //-------------
                            //Add Coverage
                            //-------------
                             rmse.addCoverage(rrr);                                                
                		  }            
                      
              }//end of all movies by current user
        	
            //sort the pairs (ascending order)
    		IntArrayList keys = midToPredictions.keys();
    		DoubleArrayList vals = midToPredictions.values();        		
    		midToPredictions.pairsSortedByValue(keys, vals);
    		
    		int movSize = midToPredictions.size();
    		if(movSize>50)
    			movSize = 50;      	
    		 
    		for(int x=0;x<movSize;x++)
    		{
    		  mov = keys.getQuick(x);
    		  pred = vals.getQuick(x);
    		  actual = testmh.getRating(uid,(int) mov);	
    		  uidToPredictions[i][x] = pred;
    		  uidToPredictions[i][50+x] = actual;
    		}//end for
    	    
    		 uidToPredictions[i][100] = helper.getAverageRatingForUser(uid);
    		 midToPredictions.clear();
        }//end of user for

        kMeanMae  			= rmse.mae();
        kMeanEigen_Nmae  	= rmse.nmae_Eigen(1.0, 5.0);
        kMeanCluster_Nmae  	= rmse.nmae_ClusterKNN(1.0, 5.0);
        kMeanRmse 			= rmse.rmse();
        kMeanCoverage  		= rmse.getItemCoverage();
        kMeanSensitivity 	= rmse.getAccuracy();    
        
        //-------------------------------------------------
        //Calculate top-N
        
		
        for(int i=0;i<5;i++)	//N from 5 to 30
        {
        	for(int j=0;j<totalUsers;j++)//All users
        	{
        		//get user avg
        		uAvg =  uidToPredictions [j][100];	
        		
        		for(int k=0;k<((i+1)*5);k++)	//for topN predictions
        		{
        			//get prediction and actual vals
	        		pred =  uidToPredictions [j][k];
	        		actual =  uidToPredictions [j][50+k];  		
	        		rmse.addTopN(actual, pred, 5, uAvg);
        		}
        		
        	}//end for
        	
        	//Now we finsih finding Top-N for a particular value of N
        	//Store it 
        	kMeanPrecision[i]=rmse.getTopNPrecision();
        	kMeanRecall[i]=rmse.getTopNRecall();
        	kMeanF1[i]=rmse.getTopNF1();
        	
        	//reset values
        	rmse.resetFinalTopN();   
        }//end of for
        
        rmse.resetValues();        

    }
 
/***************************************************************************************************/
    
 /**
  * Open and close files    
  */

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