package netflix.recommender;

/**
 * This class uses the item-item similarity table to predict ratings by a 
 * user on an unrated movie.
 */
//-Xms40m-Xmx512m 

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import cern.colt.list.LongArrayList;
import cern.colt.map.OpenIntDoubleHashMap;
import netflix.algorithms.modelbased.itembased.DatabaseImpl;
import netflix.algorithms.modelbased.writer.UserSimKeeper;
import netflix.memreader.MemHelper;
import netflix.rmse.RMSECalculator;
import netflix.utilities.IntDoublePair;
import netflix.utilities.Pair;
import netflix.utilities.Timer227;
import netflix.utilities.Triple;

/************************************************************************************************************************/
public class ItemItemRecommender 
/************************************************************************************************************************/
{  
	//Memhelper obj
    private String      	myPath;
    private MemHelper       myTrainingSet;
    private MemHelper       myTestSet;    

    private double 			RMSE;
    private double 			MAE;			//metrics
    private double 			ROC;
    private double 			coverage;
    private double 			precision[];
    private double 			recall[];
    private double 			F1[];
    
    //RMSECal obj
    RMSECalculator 			rmseCal;
    
    //classes to be predicted
    private int 				 myClasses;
    private OpenIntDoubleHashMap midToPredictions;		   //will be used for top_n metrics (pred*100, actual)
    boolean 					 whcihMethod;			   //Weighted sum vs regression
    
/************************************************************************************************************************/

    /**
     * We will initialise only the test and train object here 
     */
    
    public ItemItemRecommender(boolean whcihMethod, int classes)    
    {
    	//create MemHelper  obj
    	myTrainingSet = null;
    	myTestSet =  null;
    	
    	//rmseCal obj
    	rmseCal = new RMSECalculator();
    	   
    	//initilaise variables
    	RMSE= MAE=ROC=coverage=0;
    	precision= new double[5];
    	recall= new double[5];
    	F1= new double[5];
    	                      
    	myClasses= classes;
    	this.whcihMethod = whcihMethod;
    	midToPredictions = new OpenIntDoubleHashMap();  
    	
    }
    
/************************************************************************************************************************/
 
    /**
     * @author Musi
     * 
     * @param uid - the user to predict the rating for
     * @param mid - the movie the predict against
     * @param neigh - no. of neighbours to consider
     * @param alpha - alpha parameter
     * @return	the predicted rating for this user on this movie.
     */
    
    public double recommend(	MemHelper trainObj, 
    							int uid, int mid,  int neigh, 
    							int alpha)    
    {
    	myTrainingSet = trainObj;
    	
    	if (whcihMethod)
    		return weightedSum(mid, uid, neigh, alpha);		// weighted sum
    	/*else
    		return regression(mid, uid);					// linear regression
    	*/
    	
    	else
    		return 0;
    }
    
/************************************************************************************************************************/
    /**
     * @author steinbel
     * Uses a weighted sum method to find a predicted integer rating
     * from 1-5 for a user on an unrated movie.
     * 
     * @param movieID   The movie for which a rating should be predicted.
     * @param userID    The active user for whom we make the prediction.
     * @return the predicted rating for this user on this movie.
     */

    //so the idea is, similar items have already been computed, what u have to do is just to
    //pick all the similar items (to the active item) and make their weighted averages
    
    private double weightedSum(int movieID, int userID, int NumberOfNeighbours, int alpha)    
    {    	
    	 double answer  	    = 0;	  
      	 double sumTop			= 0;
         double sumBottom		= 0;             

         //Movies seen by active user
         LongArrayList moviesSeenByActiveUser = myTrainingSet.getMoviesSeenByUser(userID);
         //if(moviesSeenByActiveUser.size()<=1) return 0;
                 
         // All similar item, define variables        
         OpenIntDoubleHashMap itemIdToWeight = new  OpenIntDoubleHashMap();
         IntArrayList myItems      		 	 = new IntArrayList();
         DoubleArrayList myWeights 		 	 = new DoubleArrayList();
         double currentWeight 				 = 0;
         
         int totalUsersWhoSawMovie =  	myTrainingSet.getNumberOfUsersWhoSawMovie(movieID);
         
         if(totalUsersWhoSawMovie <=40)
        	 return 0;
         
         
         //Get all movies seen by active user: store their rating 
         int activeUserMovSize = moviesSeenByActiveUser.size();
         for (int i=0;i<activeUserMovSize;i++)        
         {
         	int mid = MemHelper.parseUserOrMovie(moviesSeenByActiveUser.getQuick(i));        	
         	
         	//To add the pair t results, with no SW and a SW scheme.
         	    currentWeight = findSimilarity (movieID, mid,alpha);	
         	 //  currentWeight = findVectorSimilarity (movieID, mid, comb, alpha);
         		
         	if(currentWeight!=-100)
         		itemIdToWeight.put(mid, currentWeight);
         }       
         
         //Sort similar items, according to their weights
         myItems = itemIdToWeight.keys();
         myWeights = itemIdToWeight.values();
         itemIdToWeight.pairsSortedByValue(myItems, myWeights);
         int totalSimialrItems = myItems.size();

         // Go through total Similar items and return weighted sum /regression
         for (int i = totalSimialrItems-1, myTotal=0; i >=0; i--, myTotal++)       
         {    	   
         		if(myTotal == NumberOfNeighbours) break;	
         		int itemId = myItems.get(i);       	
         
         		//         
         		//simple
         		currentWeight= myWeights.get(i);
             	double ActiveUserRating= myTrainingSet.getRating(userID, itemId);         
             		
             	// Consider All Weights            	
 	             	sumBottom+= Math.abs(currentWeight);  //ADD ABSOLUTE WEIGHT
 	             	//sumTop+= ActiveUserRating * currentWeight;
 	             	sumTop+= (ActiveUserRating - myTrainingSet.getAverageRatingForUser(userID)) * currentWeight;
               
 	           
             	// Taste Approach
 	            /* 	sumBottom+= Math.abs(currentWeight+1);
 	             	sumTop+= (ActiveUserRating  - myTrainingSet.getAverageRatingForUser(userID))* (currentWeight+1);
 	             	*/
             	
 	           /*  // +ve Weights
 	             	if(currentWeight >0)
 	             	{
 		             	sumBottom+= Math.abs(currentWeight);
 		             	sumTop+= ActiveUserRating * currentWeight;
 	             	}*/
 	                    	
 	     } //end of for
         
                	
 	        //if user didn't see any similar movies give avg rating for user
 	        if (sumBottom == 0) 	        
 	        	{
 	        	 	//return myTrainingSet.getAverageRatingForUser(userID);
 	        	    return (answer =0); 		//sparsity challenge (active user have not rated any similar movie)
 	        	}
 	      
 	        		//answer = sumTop/sumBottom;
 	        		answer = myTrainingSet.getAverageRatingForUser(userID) + sumTop/sumBottom;	 
 	        
 	         if (answer<0) 
 	        	 {
 	        	 	// return myTrainingSet.getAverageRatingForUser(userID);
 	        	 	//return  (answer =0);
 	        	 
 	        	 }	        
 	 
       return answer;
          
    
    }


/************************************************************************************************************************/
	/**
	 * Find similarity between two items
	 *  
	 */
	//Triple= for each user who have rated both movies [rating1, rating2, user avg]
	public double findSimilarity( int mid1, int mid2, int alpha)    
	{
	    ArrayList<Triple> commonUsers = myTrainingSet.getCommonUserRatAndAve(mid1, mid2);
	    double commonUsersSize = commonUsers.size();
	
	    if (commonUsers.size() < 1) return  -100.0;	//just like threshold
	    double num = 0.0, den1 = 0.0, den2 = 0.0;
	    
	    for (Triple u : commonUsers)        
	    {
	    	
	        double diff1 = u.r1 - u.a;       // For Adjusted Cosine sim
	        double diff2 = u.r2 - u.a;
	
	     /*   double diff1 = u.r1 ;			 // For Cosine sim
	        double diff2 = u.r2 ;*/
	        
	        num += diff1 * diff2;
	        
	        den1 += diff1 * diff1;
	        den2 += diff2 * diff2;
	    }
	    
	    double den = Math.sqrt(den1) * Math.sqrt(den2);               
	    
	    if (den == 0.0) 
	    	return 0.0;   
	    
	    double functionResult = num/den;
	
	    double simFactor = (commonUsersSize/alpha);   		 	 
		 
	     return  (functionResult * simFactor);		   
	}

/************************************************************************************************************************/

	/**
     * @param Memhelper Object, How much Neighbours
     * @return MAE
     */
    
    public void GoTroughTestSet( MemHelper trainSet, MemHelper testSet, 
    							int myNeighbours, int alpha
    							  )     
    {
        myTrainingSet = trainSet;
        myTestSet 	  = testSet;
        
        IntArrayList users;
		LongArrayList movies;
        String blank = "";
        int uid, mid, total=0;
        int totalUsers=0;
        double mov, pred,actual, uAvg;
        
        // For each user, make recommendations
        users		 = testSet.getListOfUsers();
        totalUsers   = users.size(); 
        
        double uidToPredictions[][] = new double[totalUsers][101]; // 1-49=predictions; 50-99=actual; (Same order); 100=user average
        
                
        //-----------------------
        // All test users
        //-----------------------
        
           for (int i = 0; i < totalUsers; i++)                                
            {
            	uid = users.getQuick(i);       
                movies = testSet.getMoviesSeenByUser(uid);
                double myRating=0.0;                
            	total++;         
      
            	//-----------------------
                // Movies seen by a user
                //-----------------------
                
                for (int j = 0; j < movies.size(); j++)     
                {
                  mid = MemHelper.parseUserOrMovie(movies.getQuick(j));   
                  
                  double rrr = recommend  (trainSet,
                		  				   uid, mid,
                		  				   myNeighbours,alpha                		  				   
                  						 );
                          
                                    	
                			myRating = testSet.getRating(uid, mid);			 		// get actual ratings?

                            if (myRating==-99 )                           
                               System.out.println(" rating error, uid, mid, ratingP" + uid + "," + mid + ","+ myRating);
                           
                            //---------------------
                            //Add Roc and MAE
                            //---------------------
                            
                            if(rrr!=0) {
                            	rmseCal.add(myRating,rrr);		   							 // get prediction for these users ....from where it is calling it?
                            	rmseCal.ROC4(myRating, rrr, myClasses,myTrainingSet.getAverageRatingForUser(uid) );
                            	midToPredictions.put(mid, rrr); 
                            }
                                                                  
                            //-------------
                            //Add Coverage
                            //-------------
                             rmseCal.addCoverage(rrr);   
                            
                           /* System.out.println("=====================================================");
                            System.out.println(" error is = (actual - predicted=" + myRating + "-" + rrr);
                            System.out.println("=====================================================");
                                */
                       
                }//end of all movies for
          
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
        		  actual = testSet.getRating(uid,(int) mov);	
        		  uidToPredictions[i][x] = pred;
        		  uidToPredictions[i][50+x] = actual;
        		}//end for
        	    
        		 uidToPredictions[i][100] = testSet.getAverageRatingForUser(uid);
        		 midToPredictions.clear();
        		 
            }//end of all users for
           
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
   	        		rmseCal.addTopN(actual, pred, 5, uAvg);
           		}
           		
           	}//end for
           	
           	//Now we finsih finding Top-N for a particular value of N
           	//Store it 
           	precision[i]=rmseCal.getTopNPrecision();
           	recall[i]=rmseCal.getTopNRecall();
           	F1[i]=rmseCal.getTopNF1();
           	
           	//reset values
          // 	rmseCal.resetTopN();      
           }//end of for
           

        MAE= rmseCal.mae();
        RMSE= rmseCal.rmse();
        ROC= rmseCal.getSensitivity();
     
         
    }

 /************************************************************************************************************************/
 
    //retuen results
    //--------------------------------------------
    public double getMAE()
    {
    	return MAE;
    }
    
  //--------------------------------------------
    public double getRMSE()
    {
    	return RMSE;
    }
    
  //--------------------------------------------
    public double getROC()
    {
    	return ROC;
    }
    
    //--------------------------------------------
    public double getCoverage()
    {
    	return coverage;
    }
    
  //--------------------------------------------
    public double getTopNPrecision(int N)
    {
    	int index = (int)(N/5.0);
    	return precision[index];
    }
    
  //--------------------------------------------
    public double getTopNRecall(int N)
    {
    	int index = (int)(N/5.0);
    	return recall[index];
    }
    
  //--------------------------------------------
    public double getTopNF1(int N)
    {
    	int index = (int)(N/5.0);
    	return F1[index];
    }
    
 /************************************************************************************************************************/
    
    public static void main (String[] args)    
    {
    	int classes =5;
    	String trainSet = "";
    	String testSet  = "";
    	String myPath   = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/FiveFoldData/100/";
		//myPath   = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/Clustering/100/";
		
    	MemHelper myTrain, myTest;
    	
    	ItemItemRecommender myIBCF = new ItemItemRecommender(true ,classes); //ture= weighted sum    	
   	 
    	
	
		
		for (int fold=1;fold <=1;fold++)
         {
      	/*	trainSet = myPath  + "sml_trainSetStoredFold" +(fold) + ".dat";
      		testSet  = myPath  + "sml_testSetStoredFold" +(fold) + ".dat";  */    		
      		
			trainSet  = myPath + "sml_trainingTrainSetStoredFold1.dat";
			testSet   = myPath + "sml_trainingValSetStoredFold1.dat";			
			
			
      		/*trainSet  = myPath + "sml_clusteringTrainSetStoredTF.dat";
      		testSet   = myPath + "sml_clusteringTestSetStoredTF.dat";
    		*/
    		
      		myTrain  = new MemHelper(trainSet);
      		myTest   = new MemHelper(testSet);
      		
      		//GoTroughTestSet
      		myIBCF.GoTroughTestSet(myTrain, myTest, 25, 40);
      		
      		//print some results      		
      		System.out.println("MAE="+ myIBCF.getMAE());
      		System.out.println("ROC="+ myIBCF.getROC());
      		System.out.println("F1 top 10="+ myIBCF.getTopNF1(10));
      		System.out.println("F1 top 20="+ myIBCF.getTopNF1(20));
         }
      	 
    }//end of main function
}
		
