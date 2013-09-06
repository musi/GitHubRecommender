package netflix.recommender;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.*;
import netflix.algorithms.memorybased.Clustering.*;
import netflix.memreader.*;
import netflix.rmse.RMSECalculator;
import netflix.utilities.*;
import cern.colt.list.*;
import cern.colt.map.*;

/************************************************************************************************/
public class SimpleKMeanAnytimeRec 
/************************************************************************************************/
{
    private RecTree2 						tree;
	private MyRecTree 						mixedTree;
	private SimpleKMean						simpleKTree;
	private SimpleKMeanPlus					simpleKPlusTree;
	private SimpleKMeanModifiedPlus			simpleKModifiedPlusTree;
	private SimpleKMeanPlusAndPower			simpleKPlusAndPowerTree;
	private SimpleKMeanPlusAndLogPower		simpleKPlusAndLogPowerTree;
	private double 							alpha;					// coff for log and power 				
	private double 							beta; 
	private int								myClasses;
	
    MemHelper 			trainMMh;
    MemHelper 			allHelper;
    MemHelper 			testMMh;
    Timer227 			timer;
    
    private int 		totalNonRecSamples;	 //Total number of sample for which we did not recommend anything
    private int 		totalRecSamples;
    private int 		howMuchClusterSize;
    private double 		threshold = 0.1;
    private long 		kMeanTime;
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
    //Regarding Results
    double 								MAE;
    double								MAEPerUser;
    double 								RMSE;
    double								Roc;
    double								coverage;
    double								pValue;
    double								kMeanEigen_Nmae;
	double								kMeanCluster_Nmae;
    
    //SD in one fold or when we do hold-out like 20-80
    double								SDInMAE;
    double								SDInROC;
	double 								SDInTopN_Precision[];
	double 								SDInTopN_Recall[];
	double 								SDInTopN_F1[];	
	
    double            					precision[];		//evaluations   
    double              				recall[];   
    double              				F1[];    
    private OpenIntDoubleHashMap 		midToPredictions;	//will be used for top_n metrics (pred*100, actual)
    
    //1: fold, 2: k, 3:dim
    double              array_MAE[][][];	      			// array of results, got from diff folds
    double              array_MAEPerUser[][][];
    double              array_NMAE[][][];
    double              array_NMAEPerUser[][][];
    double              array_RMSE[][][];
    double              array_RMSEPerUser[][][];
    double              array_Coverage[][][];
    double              array_ROC[][][];
    double              array_BuildTime[][][];
    double              array_Precision[][][][]; // [topnN][fold][][]
    double              array_Recall[][][][];
    double              array_F1[][][][];    
    
    //will store the grid results in the form of mean and sd
    double				gridResults_Mean_MAE[][];
    double				gridResults_Mean_MAEPerUser[][];
    double				gridResults_Mean_NMAE[][];
    double				gridResults_Mean_NMAEPerUser[][];
    double				gridResults_Mean_RMSE[][];
    double				gridResults_Mean_RMSEPerUser[][];
    double				gridResults_Mean_ROC[][];
    double				gridResults_Mean_Precision[][][];   //[TOPn][][]
    double				gridResults_Mean_Recall[][][];
    double				gridResults_Mean_F1[][][];
    
    double				gridResults_Sd_MAE[][];
    double				gridResults_Sd_MAEPerUser[][];
    double				gridResults_Sd_NMAE[][];
    double				gridResults_Sd_NMAEPerUser[][];
    double				gridResults_Sd_RMSE[][];
    double				gridResults_Sd_RMSEPerUser[][];
    double				gridResults_Sd_ROC[][];
    double				gridResults_Sd_Precision[][][];
    double				gridResults_Sd_Recall[][][];
    double				gridResults_Sd_F1[][][];
    
    double              mean_MAE[];	      					// Means of results, got from diff folds
    double              mean_MAEPerUser[];
    double              mean_NMAE[];						// for each version
    double              mean_NMAEPerUser[];
    double              mean_RMSE[];
    double              mean_RMSEPerUser[];
    double              mean_Coverage[];
    double              mean_ROC[];
    double              mean_BuildTime[];
    double              mean_Precision[];   
    double              mean_Recall[];   
    double              mean_F1[];       
    
    double              sd_MAE[];	      					// SD of results, got from diff folds
    double              sd_MAEPerUser[];
    double              sd_NMAE[];							// for each version
    double              sd_NMAEPerUser[];
    double              sd_RMSE[];
    double              sd_RMSEPerUser[];
    double              sd_Coverage[];
    double              sd_ROC[];
    double              sd_BuildTime[];
    double              sd_Precision[];   
    double              sd_Recall[];   
    double              sd_F1[];   
        
/************************************************************************************************/
    
    public SimpleKMeanAnytimeRec()    
    {
       
    	 totalNonRecSamples = 0;
    	 totalRecSamples 	= 0;
    	 howMuchClusterSize = 0;
    	 kMeanTime			= 0;    
    	 alpha 				= 0.0; // start from 0.0
    	 beta 				= 1.0; //start from 1.0
    	 myClasses			= 5;
    	 
    	 
    	 timer  = new Timer227();
         KMeansOrKMeansPlus = 0;


         //-------------------------------------------------------
         //Answers
	        totalPerfectAnswers = 0;
	        totalAnswers 		 = 0;
	         
         	MAE 				= 0;
	    	MAEPerUser			= 0;
	    	RMSE 				= 0;
	    	kMeanEigen_Nmae		= 0;
	    	kMeanCluster_Nmae	= 0;
	    	Roc 				= 0;
	    	coverage			= 0;
	    	pValue				= 0;
	    	SDInMAE				= 0;
	    	SDInROC				= 0;
	    	SDInTopN_Precision	= new double[8];
	    	SDInTopN_Recall		= new double[8];
	    	SDInTopN_F1			= new double[8];
	
	    	midToPredictions    = new OpenIntDoubleHashMap();     	  
	        precision    		= new double[8];		//topN; for six values of N (top5, 10, 15...30)
	    	recall  			= new double[8];		// Most probably we wil use top10, or top20
	    	F1					= new double[8];
	    	
	        //Initialize results, Mean and SD	    	
	    	 array_MAE  	 	= new double[5][11][51];  //IBCF: [10]--> k=5;k<50;K+=5
	    	 array_MAEPerUser	= new double[5][11][51]; 
	    	 array_NMAE		 	= new double[5][11][51];  //IBCF: [10]--> k=10;k<100;K+=10
	    	 array_NMAEPerUser	= new double[5][11][51];
	    	 array_RMSE 	 	= new double[5][11][51];
	    	 array_RMSEPerUser 	= new double[5][11][51];
	         array_Coverage  	= new double[5][11][51];
	         array_ROC 		 	= new double[5][11][51];
	         array_BuildTime 	= new double[5][11][51];
	         
	         array_Precision 	= new double[8][5][11][51]; //[topN][fold][neigh][dim]
	         array_Recall 	 	= new double[8][5][11][51];
	         array_F1 		 	= new double[8][5][11][51];
	         	         
	         //So we have to print this grid result for each scheme,
	         //Print in the form of "mean + sd &" 
	         gridResults_Mean_MAE 			= new double[11][51];	// neigh, dim, see IBCF, and UBCF diff above	        
	         gridResults_Mean_NMAE			= new double[11][51];	         
	         gridResults_Mean_RMSE			= new double[11][51];
	         gridResults_Mean_MAEPerUser	= new double[11][51];
	         gridResults_Mean_RMSEPerUser	= new double[11][51];
	         gridResults_Mean_NMAEPerUser	= new double[11][51];
	         gridResults_Mean_ROC			= new double[11][51];
	         
	         gridResults_Mean_Precision		= new double[8][11][51];  // [toppN][neigh][dim]
	         gridResults_Mean_Recall		= new double[8][11][51];
	         gridResults_Mean_F1			= new double[8][11][51];       
	         	         
	         gridResults_Sd_MAE			= new double[11][51];	         
	         gridResults_Sd_NMAE		= new double[11][51];	         
	         gridResults_Sd_RMSE		= new double[11][51];
	         gridResults_Sd_NMAEPerUser	= new double[11][51];
	         gridResults_Sd_MAEPerUser	= new double[11][51];
	         gridResults_Sd_RMSEPerUser = new double[11][51];
	         gridResults_Sd_ROC			= new double[11][51];
	         
	         gridResults_Sd_Precision	= new double[8][11][51];
	         gridResults_Sd_Recall		= new double[8][11][51];
	         gridResults_Sd_F1			= new double[8][11][51];
	         
	        // mean and sd, may be not required
	        mean_MAE 		= new double[5];	        
	        mean_NMAE 		= new double[5];	        
	        mean_RMSE 		= new double[5];
	        
	        mean_NMAEPerUser= new double[5];
	        mean_RMSEPerUser= new double[5];
	        mean_MAEPerUser = new double[5];
	        
	        mean_Coverage 	= new double[5];
	        mean_ROC 		= new double[5];
	        mean_BuildTime  = new double[5];
	        mean_Precision	= new double[5];
	        mean_Recall		= new double[5];
	        mean_F1			= new double[5];	        
	        
	        sd_MAE 			= new double[5];	        
	        sd_NMAE 		= new double[5];
	        sd_RMSE 		= new double[5];
	        
	        sd_MAEPerUser	= new double[5];
	        sd_NMAEPerUser 	= new double[5];
	        sd_RMSEPerUser	= new double[5];
	        
	        
	        sd_Coverage 	= new double[5];
	        sd_ROC 			= new double[5];
	        sd_BuildTime 	= new double[5];
	        sd_Precision 	= new double[5];
	        sd_Recall 		= new double[5];
	        sd_F1		 	= new double[5];       
	    			
	
	    	
    }

/************************************************************************************************/

/**
 *  It initialise an object and call the method for building the three 
 */
    public void callKTree(int callNo, int MAX_ITERATIONS )     
    {
    	//5,3,3,3,3; KMeans was like 1.13 and remaining 1.08 RMSE
    	// 5,5,5,5,5; the diff is like 1.11 and 1.06
        //-----------------------
    	// K-Means
    	//-----------------------
    	
    	
    	if(KMeansOrKMeansPlus==1)
    	{
	    	timer.start();	              
	        simpleKTree.cluster(kClusters, callNo, MAX_ITERATIONS);       
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
	        simpleKPlusTree.cluster(kClusters,callNo, MAX_ITERATIONS);       
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
	        simpleKModifiedPlusTree.cluster(kClusters,callNo, MAX_ITERATIONS);       
	        timer.stop();
	        
	        kMeanTime = timer.getTime();
	        System.out.println("KMeans Modified Plus Tree took " + timer.getTime() + " s to build");    	
	        timer.resetTimer();
	    }    

  
    	//-----------------------
    	// K-Means Plus and Power
    	//-----------------------    	
	    	
    	else if(KMeansOrKMeansPlus==4)
    	{
	        timer.start();           
	        simpleKPlusAndPowerTree.cluster(kClusters,callNo, MAX_ITERATIONS);       
	        timer.stop();
	        
	        kMeanTime = timer.getTime();
	        System.out.println("KMeans Plus and Power Tree took " + timer.getTime() + " s to build");    	
	        timer.resetTimer();
	    }    	
    	    	
      	//-----------------------
    	// K-Means Plus and 
    	// Log Power
    	//-----------------------    	
        
    	else if(KMeansOrKMeansPlus==5)
    	{
	        timer.start();           
	        simpleKPlusAndLogPowerTree.cluster(kClusters,callNo,  MAX_ITERATIONS);       
	        timer.stop();
	        
	        kMeanTime = timer.getTime();
	        System.out.println("KMeans Plus and Log Power Tree took " + timer.getTime() + " s to build");    	
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
               
        double activeAvg = trainMMh.getAverageRatingForUser(activeUser);
        double targetAvg = trainMMh.getAverageRatingForUser(targetUser);
    
        ArrayList<Pair> ratings = trainMMh.innerJoinOnMoviesOrRating(activeUser, targetUser, true);
		
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
    public double recommend(int activeUser, int targetMovie, int neighbours)    
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
	     LongArrayList movies = trainMMh.getMoviesSeenByUser(activeUser);         
         int moviesSize = movies.size();
         for (int i=0;i<moviesSize;i++)
         {                	
         	  int mid = MemHelper.parseUserOrMovie(movies.getQuick(i));
         	  double rating = trainMMh.getRating(activeUser, mid);
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
		   LongArrayList tempUsers = trainMMh.getUsersWhoSawMovie(targetMovie);
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
				 
				 neighRating = trainMMh.getRating(uid, targetMovie);	
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
        		simpleKUsers = simpleKTree.getClusterByUID(activeUser);                  //simpleK tree users
        		
        		int activeClusterID = simpleKTree.getClusterIDByUID(activeUser);
        		OpenIntDoubleHashMap simMap = new OpenIntDoubleHashMap();				//sim b/w an active user and the clusters
        		
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
        		IntArrayList keys		 = simMap.keys();
        		DoubleArrayList vals 	 = simMap.values();        		
        	    simMap.pairsSortedByValue(keys, vals);        		
        		int simSize 			  = simMap.size();
        		LongArrayList tempUsers   = trainMMh.getUsersWhoSawMovie(targetMovie);
        		LongArrayList allUsers    = new LongArrayList();
        		
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
					if(clusterRating!=0)
					{
						//Prediction
			       		weightSum += Math.abs(clusterWeight);      		
			           	voteSum+= (clusterWeight*(clusterRating-clusterAverage)) ;
			           	
			           	if(total++ == 70) break;
					}
        		
        		}// end of for
        		
		            if (weightSum!=0)
		 	    	   voteSum *= 1.0 / weightSum;        
		         
		 	       
		 	       if (weightSum==0)				// If no weight, then it is not able to recommend????
		 	       { 
		 	    	   //   System.out.println(" errror =" + answer);
		 	           //   System.out.println(" vote sum =" +voteSum + ", weisghtSum ="+ weightSum);
		 	    	   
		 	         	 totalNan++;
		 	         	 return 0;	       
		 	       }
		 	       	       
		 	       double answer = trainMMh.getAverageRatingForUser(activeUser) + voteSum;             
		        // System.gc(); // It slows down the system to a great extent

		         //------------------------
		         // Send answer back
		         //------------------------          
		       
		          if(answer<=0)
		          {
		         	 totalNegatives++;
		         	// return trainMMh.getAverageRatingForUser(activeUser);
		         	 return 0;
		          }
		          
		          else {
		         	 totalRecSamples++;   
		         	 return answer;
		          }

          }     
        //-----------------------
        //KMeans Plus
        //-----------------------
        
        else  if (KMeansOrKMeansPlus == 2)        	
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
        		LongArrayList tempUsers = trainMMh.getUsersWhoSawMovie(targetMovie);
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
					
					if(clusterRating!=0)
					{
						//Prediction
			       		weightSum += Math.abs(clusterWeight);      		
			           	voteSum+= (clusterWeight*(clusterRating-clusterAverage)) ;
			           	
			           	if(total++ == 70) break;
					}
        		}
		            if (weightSum!=0)
		 	    	   voteSum *= 1.0 / weightSum;        
		         
		 	       
		 	       if (weightSum==0)				// If no weight, then it is not able to recommend????
		 	       { 
		 	    	   //   System.out.println(" errror =" + answer);
		 	           //   System.out.println(" vote sum =" +voteSum + ", weisghtSum ="+ weightSum);
		 	    	   
		 	         	 totalNan++;
		 	         	 return 0;	       
		 	       }
		 	       	       
		 	       double answer = trainMMh.getAverageRatingForUser(activeUser) + voteSum;             
		        // System.gc(); // It slows down the system to a great extent

		         //------------------------
		         // Send answer back
		         //------------------------          
		       
		          if(answer<=0)
		          {
		         	 totalNegatives++;
		         	  //return trainMMh.getAverageRatingForUser(activeUser);
		         	 return 0;
		          }
		          
		          else {
		         	 totalRecSamples++;   
		         	 return answer;
		          }

        	} 		
        	
       
       
        //-----------------------
        //simpleKPlus Log Power
        //-----------------------
        
        else  if (KMeansOrKMeansPlus == 3)        	
         { 	      		
        	simpleKUsers = simpleKModifiedPlusTree.getClusterByUID(activeUser);            //simpleKPlus 
    		
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
    		IntArrayList keys 		= simMap.keys();
    		DoubleArrayList vals	= simMap.values();        		
    	    simMap.pairsSortedByValue(keys, vals);        		
    		int simSize 			= simMap.size();
    		LongArrayList tempUsers = trainMMh.getUsersWhoSawMovie(targetMovie);
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
				
				if(clusterRating!=0)
				{
					//Prediction
		       		weightSum += Math.abs(clusterWeight);      		
		           	voteSum+= (clusterWeight*(clusterRating-clusterAverage)) ;
		           	
		           	if(total++ == 70) break;
				}
    		}
	            if (weightSum!=0)
	 	    	   voteSum *= 1.0 / weightSum;        
	         
	 	       
	 	       if (weightSum==0)				// If no weight, then it is not able to recommend????
	 	       { 
	 	    	   //   System.out.println(" errror =" + answer);
	 	           //   System.out.println(" vote sum =" +voteSum + ", weisghtSum ="+ weightSum);
	 	    	   
	 	         	 totalNan++;
	 	         	 return 0;	       
	 	       }
	 	       	       
	 	       double answer = trainMMh.getAverageRatingForUser(activeUser) + voteSum;             
	        // System.gc(); // It slows down the system to a great extent

	         //------------------------
	         // Send answer back
	         //------------------------          
	       
	          if(answer<=0)
	          {

		         	 totalNegatives++;
		         	  //return trainMMh.getAverageRatingForUser(activeUser);
		         	 return 0;
	          }
	          
	          else {
	         	 totalRecSamples++;   
	         	 return answer;
	          }

    	        		
         } //end of else if
       
        //----------------------------
        // KPlusAndPower
        //----------------------------
               
        else  if (KMeansOrKMeansPlus == 4)        	
    	{
    		simpleKUsers = simpleKPlusAndPowerTree.getClusterByUID(activeUser); 
        	simpleKUsers = simpleKPlusAndPowerTree.getClusterByUID(activeUser);            //simpleKPlus 
    		
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
    		LongArrayList tempUsers = trainMMh.getUsersWhoSawMovie(targetMovie);
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
				
				if(clusterRating!=0)
				{
					//Prediction
		       		weightSum += Math.abs(clusterWeight);      		
		           	voteSum+= (clusterWeight*(clusterRating-clusterAverage)) ;
		           	
		           	if(total++ == 70) break;
				}
    		}
	            if (weightSum!=0)
	 	    	   voteSum *= 1.0 / weightSum;        
	         
	 	       
	 	       if (weightSum==0)				// If no weight, then it is not able to recommend????
	 	       { 
	 	    	   //   System.out.println(" errror =" + answer);
	 	           //   System.out.println(" vote sum =" +voteSum + ", weisghtSum ="+ weightSum);
	 	    	   
	 	         	 totalNan++;
	 	         	 return 0;	       
	 	       }
	 	       	       
	 	       double answer = trainMMh.getAverageRatingForUser(activeUser) + voteSum;             
	        // System.gc(); // It slows down the system to a great extent

	         //------------------------
	         // Send answer back
	         //------------------------          
	       
	          if(answer<=0)
	          {

		         	 totalNegatives++;
		         	  //return trainMMh.getAverageRatingForUser(activeUser);
		         	 return 0;
	          }
	          
	          else {
	         	 totalRecSamples++;   
	         	 return answer;
	          }

    	
    	} //end of if else
      
        //----------------------------
        // KPlusAndLogPower
        //----------------------------        
        
        else  if (KMeansOrKMeansPlus == 5)        	
    	{
    		simpleKUsers = simpleKPlusAndLogPowerTree.getClusterByUID(activeUser);            //simpleKPlus 
    		
    		int activeClusterID = simpleKPlusAndLogPowerTree.getClusterIDByUID(activeUser);
    		OpenIntDoubleHashMap simMap = new OpenIntDoubleHashMap();	//sim b/w an active user and the clusters
    		
    		// Find sim b/w a user and the cluster he lies in        		
    		double simWithMainCluster = simpleKPlusAndLogPowerTree.findSimWithOtherClusters(activeUser, activeClusterID );
    		
    		// Find sim b/w a user and all the other clusters
    		for(int i=0;i<kClusters; i++)
    		{
    			if(i!=activeClusterID)
    			{
    				double activeUserSim  = simpleKPlusAndLogPowerTree.findSimWithOtherClusters(activeUser, i );
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
    		LongArrayList tempUsers = trainMMh.getUsersWhoSawMovie(targetMovie);
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
				
				if(clusterRating!=0)
				{
					//Prediction
		       		weightSum += Math.abs(clusterWeight);      		
		           	voteSum+= (clusterWeight*(clusterRating-clusterAverage)) ;
		           	
		           	if(total++ == 70) break;
				}
    		}
	            if (weightSum!=0)
	 	    	   voteSum *= 1.0 / weightSum;        
	         
	 	       
	 	       if (weightSum==0)				// If no weight, then it is not able to recommend????
	 	       { 
	 	    	   //   System.out.println(" errror =" + answer);
	 	           //   System.out.println(" vote sum =" +voteSum + ", weisghtSum ="+ weightSum);
	 	    	   
	 	         	 totalNan++;
	 	         	 return 0;	       
	 	       }
	 	       	       
	 	       double answer = trainMMh.getAverageRatingForUser(activeUser) + voteSum;             
	        // System.gc(); // It slows down the system to a great extent

	         //------------------------
	         // Send answer back
	         //------------------------          
	       
	          if(answer<=0)
	          {

		         	 totalNegatives++;
		         	  //return trainMMh.getAverageRatingForUser(activeUser);
		         	 return 0;
	          }
	          
	          else {
	         	 totalRecSamples++;   
	         	 return answer;
	          }

    	}
     
       
        //---------------------------------------------------------------------------------------
        // Start Recommending
        //---------------------------------------------------------------------------------------
      else  if (KMeansOrKMeansPlus == 6)   
      {
        
     //   IntArrayList treeUsers 		= tree.getClusterByUID(activeUser);		 	//simple tree users
     //   int userClusterIndex      	= tree.getClusterIDByUID(activeUser);
          LongArrayList tempUsers 		= trainMMh.getUsersWhoSawMovie(targetMovie);
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
        	  neighRating = trainMMh.getRating(uid, targetMovie);	//get rating of ratings of each user for the target movie
             
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
	       
	   
     	   	   // Taste approach
            	currWeight= (myWeights.get(i)+1);
            	weightSum += Math.abs(currWeight+1);
       	    	neighRating = trainMMh.getRating(uid, targetMovie);        
         	    voteSum+= (currWeight* (neighRating  - trainMMh.getAverageRatingForUser(uid))) ;
       
       		
         	    //Simple, but do not take -ve into accounts
        		  currWeight= (myWeights.get(i));      	 
		       	 // if (currWeight>0)
		       		{	
		       			weightSum += Math.abs(currWeight);      		
		           		neighRating = trainMMh.getRating(uid, targetMovie);        
		           		voteSum+= ( currWeight* (neighRating  - trainMMh.getAverageRatingForUser(uid))) ;
		       		} //end of weight should be positive
       
		       		// Take all weights into account		       		
		       		currWeight= (myWeights.get(i));	       			
		       		weightSum += Math.abs(currWeight);      		
		           	neighRating = trainMMh.getRating(uid, targetMovie);        
		           	voteSum+= ( currWeight* (neighRating  - trainMMh.getAverageRatingForUser(uid))) ;
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
	       	       
	       double answer = trainMMh.getAverageRatingForUser(activeUser) + voteSum;             
       // System.gc(); // It slows down the system to a great extent

        //------------------------
        // Send answer back
        //------------------------          
      
         if(answer<=0)
         {

         	 totalNegatives++;
         	  //return trainMMh.getAverageRatingForUser(activeUser);
         	 return 0;
         }
         
         else {
        	 totalRecSamples++;   
        	 return answer;
         }
         
      }//end if
        
        
        return 0;
        
    }

/************************************************************************************************/
    
  public static void main(String[] args)    
  {
    	
    
   // Subset of SML
/*	  String test  = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/Clustering/Sparsity/sml_TestSet20.dat";
//	  String base  = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/Clustering/Sparsity/sml_TrainSet80.dat";
	  String base  = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/Clustering/Sparsity/sml_trainSetStoredAll_80_40.dat";
*/	  
     //SML
      String test  = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/sml_clusteringTestSetStoredTF.dat";
	  String base  = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/sml_clusteringTrainSetStoredTF.dat";
	  
     
     //ML
    /*  String test  = "C:/Users/Musi/workspace/MusiRecommender/DataSets/ML_ML/SVD/80/ml_clusteringTestSetStoredTF.dat";
	  String base  = "C:/Users/Musi/workspace/MusiRecommender/DataSets/ML_ML/SVD/80/ml_clusteringTrainSetStoredTF.dat";
	  */
	  
      //FT
	  /*String test  = "C:/Users/Musi/workspace/MusiRecommender/DataSets/FT/TestTrain/ft_clusteringTestSetStored.dat";
	  String base  = "C:/Users/Musi/workspace/MusiRecommender/DataSets/FT/TestTrain/ft_clusteringTrainSetStored.dat";
	 */
	  
//    String test = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/sml_TestSetStored.dat";
//	  String base = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/sml_TrainSetStored.dat";
	
	  String mainFile = base;
	       
	    

	    
	    //create object and build memhelper of train set
	    SimpleKMeanAnytimeRec rec = new SimpleKMeanAnytimeRec();
	 	
	    rec.computeResults(test, base, mainFile);
	    
  }
  

  /************************************************************************************************/
      
  public void computeResults(String testFile, String trainFile, String mainFile)
  {   
	  double finalError=0.0;
    
	  allHelper = new MemHelper(mainFile);
	  trainMMh  = new MemHelper(trainFile);
	  testMMh = new MemHelper(testFile);	  
	 

	    /*   
	    //_____________________________
        // For Svd writing let 6 clusters
        //_____________________________ 
	    
		rec.kClusters = 6;
    	rec.callKTree ();
    */    
   
	   
  // openFile();
	  
 for (int k=70;k<=110;k+=10)	    
 {
   	 //Build the tree based on training set only
	 kClusters = k;	
	 
	  //Make the objects and keep them fixed throughout the program
	  for (int version=1;version<=5;version++)
	  {	    	
		    KMeansOrKMeansPlus = version; 

			if(KMeansOrKMeansPlus==1) 	         
			        simpleKTree = new SimpleKMean(trainMMh);	
		 	else if(KMeansOrKMeansPlus==2)             
			        simpleKPlusTree = new SimpleKMeanPlus(trainMMh);          
		 	else if(KMeansOrKMeansPlus==3) 	         
			    simpleKModifiedPlusTree = new SimpleKMeanModifiedPlus(trainMMh);
			else if(KMeansOrKMeansPlus==4) 		           
			        simpleKPlusAndPowerTree = new SimpleKMeanPlusAndPower(trainMMh);   
			else if(KMeansOrKMeansPlus==5)           
			   simpleKPlusAndLogPowerTree = new SimpleKMeanPlusAndLogPower(trainMMh, alpha, beta);  	
	       
	   }
	  
	  System.out.println("==========================================================================");
 	  System.out.println(" Clusters = "+ k);
 	  System.out.println("==========================================================================");
 	  
	 
  for(int t=1;t<=10;t++)
  {
 	 
	  System.out.println("=====================");
 	  System.out.println(" Iterations = "+ t);
 	  System.out.println("=====================");
    	   
   for (int version=1;version<=5;version++)
   {	    	
	   		KMeansOrKMeansPlus = version; 

	 		if(version>2 && version<=4) continue;       
	      	    	
    	   //Build clusters
   		    callKTree (t,t);	    			   
	    		  		    	
	    	long t1= System.currentTimeMillis();
	    	timer.start();
	    	testWithMemHelper(testMMh,10);
	    	timer.stop();
	    	
	    	long totalTime= timer.getTime();
	    	
	    	/*try {
	    		writeData.write(k+ "\t" + kMeanTime + "\t" + (totalTime) + "\t" + RMSE );
	    		writeData.newLine();
	    	}
	    	catch (Exception E)
	         {
	       	  	  System.out.println("error writing the file pointer of rec");
	  //     	  System.exit(1);
	         }*/
	    	 
	    	long t2= System.currentTimeMillis();	    	 
	    	
	    	
	    	//--------------------------------------------------------------------------------------------------- 	    	
	    	System.out.println(" Cluster = " + k+ ", Tree Time = " + kMeanTime + ",Rec Time= " + (totalTime) + 
	    						", MAE =" + MAE + ", RMSE= " + RMSE);
	    	System.out.print("NMAE_EigenTaste =" + kMeanEigen_Nmae+", NMAE_Cluster =" + kMeanCluster_Nmae);
	    	System.out.println(",Sensitivity =" + Roc+", Coverage =" + coverage);
	    	System.out.println("F1="+ F1[3]+", Precision="+precision[3]+", Recall="+recall[3]);
	    	
	    	if(version ==4)  System.out.println(" alpha =" + (alpha -0.1) + ", beta ="+ (beta+0.1) );
	    	System.out.println("answered  = "+ totalRecSamples + 
	    						", nan= "+ totalNan+ ", -ve= "+ totalNegatives);
	    	System.out.println("--------------------------------------------------------------------------------------------------- ");
	    	
	    	timer.resetTimer();
	    	totalRecSamples=0;
	    	totalNan=0;
	    	totalNegatives=0;
	    	
	    
	      } //Which KMean is called
	      
	    	//System.gc();
	    	
	   }//end of k for
    } //end of iterations for       
	    
    
    	//closeFile();
 
       //_____________________________
       // Check for threshold in Kmeans
       //_____________________________
     
   /*
         
        
   for(double i=0.1;i<5; i+=0.1)
    {
	   if (i!=0.1) callMixedTree(i);
	   
	   System.out.println("current threshol is" + i);       
	   
        for (int t=1;t < 20;t++)
        {
        	howMuchClusterSize =t;
        	totalRecSamples =0;
        	totalNonRecSamples =0;
       
        	finalError = testWithMemHelper(mh);
        	
        	
        	
          if(finalError<.96)        	
          {
        		System.out.println("---->RMSE: " + finalError);       	
        		System.out.print(", total rec samples: " + totalRecSamples);
        		System.out.print(", total non rec samples: " + totalNonRecSamples);
        		System.out.println(", cluster size: " + t);
        	}
        
            
        	if(Double.isNaN(finalError)) break;
        	
        }
     }

*/

    }

/***************************************************************************************************/
  /************************************************************************************************/
	 
  /**
   * Using RMSE as measurement, this will compare a test set
   * (in MemHelper form) to the results gotten from the recommender
   *  
   * @param testmh the memhelper with test data in it   //check this what it meant........................Test data?///
   * @return the rmse in comparison to testmh 
   */

  public void testWithMemHelper(MemHelper testmh, int neighbours)     
  {
         	
	    RMSECalculator rmse = new  RMSECalculator();
	  
        IntArrayList users;
		LongArrayList movies;
		//IntArrayList coldUsers = coldUsersMMh.getListOfUsers();
		//IntArrayList coldItems = coldItemsMMh.getListOfMovies();
	  
	  double mov, pred,actual, uAvg;
	  String blank			 	= "";
      int uid, mid, total			= 0;    		       	
      int totalUsers				= 0;
      int totalExtremeErrors 		= 0 ;
      int totalEquals 			= 0;
      int totalErrorLessThanPoint5= 0;
      int totalErrorLessThan1 	= 0;    	        
      		        
      // For each user, make recommendations
      users = testmh.getListOfUsers();
      totalUsers= users.size();
      
      double uidToPredictions[][] = new double[totalUsers][101]; // 1-49=predictions; 50-99=actual; (Same order); 100=user average
      
      //________________________________________
      
      for (int i = 0; i < totalUsers; i++)        
      {
      	uid = users.getQuick(i);    
      	
      	//if(coldUsers.contains(uid))
      	{   
	            movies = testmh.getMoviesSeenByUser(uid);
	           if(i>100 && i%100==0)
	        	   System.out.println("now at " + i + " of total " + totalUsers );
	            
	            for (int j = 0; j < movies.size(); j++)             
	            {	    		            
	            	total++;
	                mid = MemHelper.parseUserOrMovie(movies.getQuick(j));
	             
	              //  if(coldItems.contains(mid))
	                {
	           //     if (mid ==-1)  System.out.println(" rating error--> uid, mid -->" + uid + "," + mid );
	                
	               // double rrr = recommend(uid, mid, blank);                
	                double rrr = recommend(uid, mid, neighbours);
	                
	                /*//Add values to Pair-t
	                if(ImputationMethod ==2)
	                	rmse.addActualToPairT(rrr);
	                else
	                	rmse.addPredToPairT(rrr);
	                */
	                
	                double myRating=0.0;
	                
	                //if (rrr!=0.0)                 
	                      {
	                	
	                			myRating = testmh.getRating(uid, mid);			 		// get actual ratings?

	                			//System.out.println(rrr+", "+ myRating);
	                            
	                			if (myRating==-99 )                           
	                               System.out.println(" rating error, uid, mid, rating" + uid + "," + mid + ","+ myRating);
	                           
	                            if(rrr>5 || rrr<=0)
	                            {
	         /*                   		System.out.println("Prediction ="+ rrr + ", Original="+ myRating+ ", mid="+mid 
	                            		+", NewMid="+ myMoviesMap.get(mid)+ ", uid="+uid
	                            		+"No users who rated movie="+ mh.getNumberOfUsersWhoSawMovie(mid) + 
										", User saw movies="+mh.getNumberOfMoviesSeen(uid));*/
	                            }
	                            
	                            if(rrr>5 || rrr<-1)
	                            	totalExtremeErrors++;
	                            
	                            else if(Math.abs(rrr-myRating)<=0.5)
	                            	totalErrorLessThanPoint5++;
	                            
	                            
	                            else if(Math.abs(rrr-myRating)<=1.0)
	                            	totalErrorLessThan1++;
	                            
	                            else if (rrr==myRating)
	                            	totalEquals++;
	                            
	                          		                            
	                            //-------------
	                            // Add ROC
	                            //-------------
	                            if(rrr!=0)
	                            	rmse.ROC4(myRating, rrr, myClasses, trainMMh.getAverageRatingForUser(uid));		
	                            	//rmse.ROC4(myRating, rrr, myClasses, TopNThreshold);
	                		                          
	                            //-------------
	                            //Add Error
	                            //-------------
	                           
	                            if(rrr!=0)
	                            {
	                            	rmse.add(myRating,rrr);                            	
	                            	midToPredictions.put(mid, rrr);                            	                                
	                            }		
	            
	                            //-------------
	                            //Add Coverage
	                            //-------------

	                             rmse.addCoverage(rrr);                                 
	                		  }         
	                }
	            } //end of movies for
	            
	            //--------------------------------------------------------
	            //A user has ended, now, add ROC and reset
	            rmse.addROCForOneUser();
	            rmse.resetROCForEachUser();
	            rmse.addMAEOfEachUserInFinalMAE();
	            rmse.resetMAEForEachUser();
	            
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
	    	    
	    		 uidToPredictions[i][100] = trainMMh.getAverageRatingForUser(uid);
	    		 midToPredictions.clear();
      	 } //end of if (cold start checkng) 
	       } //end of user for	   
	    
	        MAE		 	= rmse.mae(); 
	        SDInMAE		= rmse.getMeanErrorOfMAE();
	        SDInROC 	= rmse.getMeanSDErrorOfROC();
	        Roc 		= rmse.getSensitivity();
	        MAEPerUser 	= rmse.maeFinal();
	        RMSE 		= rmse.rmse();
	        coverage	= rmse.getItemCoverage();
	        
	        kMeanEigen_Nmae 	= rmse.nmae_Eigen(1,5);
	        kMeanCluster_Nmae 	= rmse.nmae_ClusterKNNFinal(1, 5);
	        		
	       /* if(ImputationMethod >2)
	        	pValue  = rmse.getPairT();
	        */
	        
	         //-------------------------------------------------
	         //Calculate top-N    		            
	    		
	            for(int i=0;i<8;i++)	//N from 5 to 30
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
	    	        		
	    	        		//add to topN
	    	        		   rmse.addTopN(actual, pred, myClasses, uAvg);
	    	        		 // rmse.addTopN(actual, pred, myClasses, TopNThreshold);
	            		}
	            		
	            		//after each user, first add TopN, and then reset
	            		rmse.AddTopNPrecisionRecallAndF1ForOneUser();
	            		rmse.resetTopNForOneUser();   		            		
	            	
	            	} //end for
	            	
	            	//Now we finish finding Top-N for a particular value of N
	            	//Store it 
	            	precision[i]=rmse.getTopNPrecision();
	            	recall[i]=rmse.getTopNRecall();
	            	F1[i]=rmse.getTopNF1(); 
	            	
	            	//Get variance 
	            	SDInTopN_Precision[i] = rmse.getMeanSDErrorOfTopN(1); 
	            	SDInTopN_Recall[i] = rmse.getMeanSDErrorOfTopN(2);
	            	SDInTopN_F1 [i]= rmse.getMeanSDErrorOfTopN(2);
	            	
	            	//Reset all topN values    		            	
	            	rmse.resetTopNForOneUser();
	            	rmse.resetFinalTopN();
	       		            
          } //end of for   		        	
      	
   /* System.out.println("totalExtremeErrors="+totalExtremeErrors + ", Total ="+total);
      System.out.println("totalErrorLessThanPoint5="+totalErrorLessThanPoint5 );	       
      System.out.println("totalErrorLessThan1="+totalErrorLessThan1 );
      System.out.println("totalEquals="+totalEquals );  */    		        
      
      //Reset final values
      rmse.resetValues();   
      rmse.resetFinalROC();
      rmse.resetFinalMAE();
      /*if(ImputationMethod >2)
      	rmse.resetPairTPrediction();*/
     
}//end of function

      
 
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



}