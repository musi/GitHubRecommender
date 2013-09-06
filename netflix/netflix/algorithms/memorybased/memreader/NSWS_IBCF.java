package netflix.algorithms.memorybased.memreader;

/**
 * This class uses the item-item similarity table to predict ratings by a 
 * user on an unrated movie.
 */

//-Xms40m-Xmx512m 

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Random;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import cern.colt.list.LongArrayList;
import cern.colt.map.OpenIntDoubleHashMap;
import netflix.algorithms.modelbased.itembased.DatabaseImpl;
import netflix.algorithms.modelbased.reader.DataReader;
import netflix.algorithms.modelbased.writer.UserSimKeeper;
import netflix.memreader.MemHelper;
import netflix.rmse.RMSECalculator;
import netflix.utilities.IntDoublePair;
import netflix.utilities.MeanOrSD;
import netflix.utilities.Pair;
import netflix.utilities.Timer227;
import netflix.utilities.Triple;

/************************************************************************************************************************/
public class NSWS_IBCF //extends AbstractRecommender
/************************************************************************************************************************/
{

    private boolean 		method; 					//true for weighted sums, false for linear regression
    BufferedWriter      	writeData[];				//for writing in file
    BufferedWriter      	writeDemoData[];			//for writing in file
    BufferedWriter      	writeRatingData[];			//for writing in file
    BufferedWriter      	writeDemoAndRatingData[];	//for writing in file
    BufferedWriter      	writeWeights;				//for weight writing in file
    NumberFormat 			nf;
    
    private String      	myPath;		
    private int 			dataset;	 				//0 =SML, 1=ML, 2=FT
    private String          CF;							//item or user
      
    private UserSimKeeper   myStoredRSim;   //Rsim
    private UserSimKeeper   myStoredDSim;	//DSim
    private UserSimKeeper   myStoredFSim;	//FSim
    private int             totalK;
    private int 			incrementInK;  
    private int 			startK;
    private int             totalNegativeAnswers,totalNegativeAnswers1, totalNegativeAnswers2,totalNegativeAnswers3;
    private int 			totalZeroAnswers, totalZeroAnswers1, totalZeroAnswers2, totalZeroAnswers3;
    private int				negPcc, posPcc,	totalPcc; 		//for measuring, how much correlations are pos and how much neg
    private int             negNeigh, posNeigh, totalNeigh;
    private int             myClasses;
    String 					infoAbtComb;					//contain information abt the combination we have
    String                  bigInfo;						// contain all info to write into a file
    
    //RMSE
    RMSECalculator 			rmse;
    
    //Filter and Weight
    FilterAndWeight 		myUserBasedFilter;
    
    //Training and Test set
    private MemHelper       myTrainingSet;
    private MemHelper       myTestSet;
    
    //Mean etc
    MeanOrSD 				myMeans;
    int     				avgNeigAgainstAnActiveItem;
    
    //random
    private Random 		rand;
    
/************************************************************************************************************************/

    /**
	 *  constructor sets up the database-access layer and determines which method will
	 *  be used to make recommendations (weighted sums or linear regression)
	 * 
	 */
    
    public NSWS_IBCF( boolean weighted)    
    {
    	//rmse
    	 rmse = new RMSECalculator();
    	
    //    db = new DatabaseImpl(dbName, rateName, movieName, userName, simName);    
         this.method = weighted;
        

         //SML Data paths
         // myPath ="C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/Item based/FiveFoldData/Data2/";
          //myPath ="C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/Item based/FiveFoldData/DataFD/";
         //  myPath ="C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/Item based/";
          
         /*  myPath = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/Clustering/Sparsity/";             
           dataset =0;
           myClasses =5;
         	*/

        //ML data paths
        //myPath ="C:/Users/Musi/workspace/MusiRecommender/DataSets/ML_ML/Item based/FiveFoldData/DataFD/";
        //  myPath ="C:/Users/Musi/workspace/MusiRecommender/DataSets/ML_ML/Item based/";
         
         /* myPath = "C:/Users/Musi/workspace/MusiRecommender/DataSets/ML_ML/Clustering/Sparsity/";
          dataset = 1;
          myClasses =5;
         */
        
         //FT
          myPath ="C:/Users/Musi/workspace/MusiRecommender/DataSets/FT/Item based/FiveFoldData/";
          //myPath ="C:/Users/Musi/workspace/MusiRecommender/DataSets/FT/Item based/";
          dataset   = 2;
          myClasses = 10; 
         
         
           //decide about CF's type
          CF	  = "item";
          //CF	  = "user";
          
        writeData 			     = new BufferedWriter[10];		
        writeDemoData 			 = new BufferedWriter[10];		
        writeRatingData 		 = new BufferedWriter[10];		
        writeDemoAndRatingData 	 = new BufferedWriter[10];		
        nf						 = new DecimalFormat("#.#####");	//upto 4 digits
        
        //Item based and //User based neighbours
        if(CF.equalsIgnoreCase("item")) {
	        totalK 			 = 40;
	        incrementInK	 = 5;
	        startK			 = 5;	
        }
        
        else{
	        totalK 			 = 100;
	        incrementInK	 = 20;
	        startK			 = 70;
        }
        
        totalNegativeAnswers = totalNegativeAnswers1 = totalNegativeAnswers2 = totalNegativeAnswers3 =0; 							 // -ve corr cases
        totalZeroAnswers =totalZeroAnswers1 = totalZeroAnswers2 = totalZeroAnswers3 = 0;     						 //active user has not rated any of the similar movies
        negPcc = posPcc = totalPcc =0;
        negNeigh = posNeigh = totalNeigh = 0;
        infoAbtComb ="";
        bigInfo="";
        
        //Means
        myMeans = new MeanOrSD();
        avgNeigAgainstAnActiveItem = 0;
        
        //random
        rand = new Random();
        
    }
    
/******************************************************************************************************/
  /**
   *  Recommend an item by combining user and item based 
   */
   
    public double UserAndItemBased(int activeUser, int targetMovie, int howMuchNeighbours, int comb)
    {  	     
    	
    	//-------------------------------------------------
    	// FInd Similar User --> Neighbouring Users
    	//-------------------------------------------------
    	double sumTop		= 0;
        double sumBottom	= 0;
        double answer       = 0;
        
  	    double currWeight, weightSum = 0, voteSum = 0, weightSumAbs = 0;
        int uid;

         LongArrayList users = myTrainingSet.getUsersWhoSawMovie(targetMovie);		//this should also be done in (total set - test set)                  
         
         //Filter movies, seen by only one user
         if(users.size()<=1) return //myTrainingSet.getAverageRatingForUser(activeUser);
         							myTrainingSet.getAverageRatingForMovie(targetMovie);
         
         int limit = users.size();    
                 
         OpenIntDoubleHashMap uidToWeight = new  OpenIntDoubleHashMap();
         IntArrayList myUsers     		  = new IntArrayList();
         DoubleArrayList myUsersWeights   = new DoubleArrayList();
         double currentWeight;
         
         //go through all users who saw the target movie, to find weights        
         //get all weights
         for (int i = 0; i < limit; i++)       
         {
      	    uid = MemHelper.parseUserOrMovie(users.getQuick(i));
      	    currentWeight  = myUserBasedFilter.weight(activeUser, uid);
      	  
      	    //It is a temporary step to check, what happens if we find similar items by checking neighbouring info 
      	    if (currentWeight >0)
      	    uidToWeight.put(uid, currentWeight);      	      	    
         }
         
         // My Weights: Sort the weight       
         myUsers 			= uidToWeight.keys();
         myUsersWeights 	= uidToWeight.values();
         uidToWeight.pairsSortedByValue(myUsers, myUsersWeights);         
         
        //-------------------------------------------------
     	// FInd Similar Items --> Neighbouring Items
     	//-------------------------------------------------
     	
         //Movies seen by active user
         LongArrayList moviesSeenByActiveUser = myTrainingSet.getMoviesSeenByUser(activeUser);
            
         // All similar item, define variables        
         OpenIntDoubleHashMap itemIdToWeight = new  OpenIntDoubleHashMap();
         IntArrayList myItems      		 	 = new IntArrayList();
         DoubleArrayList myItemsWeights 	 = new DoubleArrayList();
                  
         //Get all movies seen by active user: store their rating 
         int activeUserMovSize = moviesSeenByActiveUser.size();
         for (int i=0;i<activeUserMovSize;i++)        
         {
         	int mid = MemHelper.parseUserOrMovie(moviesSeenByActiveUser.getQuick(i));        	
         	currentWeight = findHybridSimilarity (targetMovie, mid,myUsers);		//target movie, movie we want to find similarity with
         	
         	if(currentWeight!=-100)
         		itemIdToWeight.put(mid, currentWeight);
         }       
         
         // My Weights: Sort the weight
         myItems 			= itemIdToWeight.keys();
         myItemsWeights 	= itemIdToWeight.values();
         itemIdToWeight.pairsSortedByValue(myItems, myItemsWeights);
         int totalSimialrItems = myItems.size();  
         
         //---------------------------------------------------------------------------------------
      	 // Now we have similar items (to the target item) and similar users (to the active user)
         // 
      	 //---------------------------------------------------------------------------------------
         
     
    //---------------------------------------------------------------------------------------
    // (1)
    // Approach first: find similarity between items, over only those users, who are neighbours 
    // of the active user (found after applying user-based CF)
    //---------------------------------------------------------------------------------------
         
         // Go through total Similar items and return weighted sum /regression
         for (int i = totalSimialrItems-1, myTotal=0; i >=0; i--, myTotal++)       
         {    	   
         		if(myTotal == howMuchNeighbours) break;	
         		int itemId = myItems.get(i);       	
         
         	if(comb==0)
         	{
         		//simple
         		currentWeight= myItemsWeights.get(i);
             	double ActiveUserRating= myTrainingSet.getRating(activeUser, itemId);

             	// Consider All Weights            	
 	           /*  	sumBottom+= Math.abs(currentWeight);
 	             	sumTop+= ActiveUserRating * currentWeight;*/
 
 	             // Taste Approach
 	             	sumBottom+= Math.abs(currentWeight+1);
 	             	sumTop+= ActiveUserRating * (currentWeight+1);
 	             	
 /*	             // +ve Weights
 	             	if(currentWeight >0)
 	             	{
 		             	sumBottom+= Math.abs(currentWeight);
 		             	sumTop+= ActiveUserRating * currentWeight;
 	             	}
 	             	
 */          }//end of if
         }//end of for
         
         
         	  //if user didn't see any similar movies give avg rating for user
            if (sumBottom == 0)
            	//return db.getAverageRatingForUser(userID);
            	{
            	    totalZeroAnswers1++;
            	//	System.out.println(" bottonSum=0 -->" + sumBottom);
            	//	return  myTrainingSet.getAverageRatingForUser(userID);        	
            	    return  0; 		
            	}
            
            answer = sumTop/sumBottom;
            
             if (answer<0) 
            	 {
            	     totalNegativeAnswers1++;
            	 	// System.out.println(" answer<0 -->" + answer);
            	 	// return myTrainingSet.getAverageRatingForUser(userID);
            	     return 0;
            	 
            	 }
            
             else return answer;
   
    } //end of function

 

    
/************************************************************************************************************************/    

    /**
     * Find similarity between two items
     *  
     */
    //Triple= for each user who have rated both movies [rating1, rating2, user avg]
    public double findSimilarity( int mid1, int mid2, int version, int alpha)    
    {
        ArrayList<Triple> commonUsers = myTrainingSet.getCommonUserRatAndAve(mid1, mid2);
        double commonUsersSize = commonUsers.size();
    
        if (commonUsers.size() < 1) return  -100.0;	//just like threshold
        double num = 0.0, den1 = 0.0, den2 = 0.0;
        
        for (Triple u : commonUsers)        
        {        	
            double diff1 = u.r1 - u.a;       // For Adjusted Cosine sim
            double diff2 = u.r2 - u.a;

      /*    double diff1 = u.r1 ;			 // For Cosine sim
            double diff2 = u.r2 ;*/
            
            num  += diff1 * diff2;            
            den1 += diff1 * diff1;
            den2 += diff2 * diff2;
        }
        
        double den = Math.sqrt(den1) * Math.sqrt(den2);               
        if (den == 0.0) 
        	return 0.0;        
        double functionResult = num/den;
   
        double simFactor =0;

        //Measure positive and negative PCCs
        if(functionResult >=0)
        	posPcc++;
        else 
        	negPcc++;        
        
        totalPcc++;
        
                
     /*   if(functionResult>=-0.7)
        	functionResult+=1;*/  
        
    
    if(functionResult <=0)  //for +ve weight, we have to deal these cases here
    {
   /*  	//---------------------------------------
    	// No weighting 
    	//---------------------------------------        	
    	 if (version ==0){
    		 		infoAbtComb="No; ";
    		 		return  functionResult;
    		 	}
    		 	        	
    	
      	//---------------------------------------
     	// CA
     	//---------------------------------------        	
     	 if (version ==1){
     		 infoAbtComb="CA";
     		 if(functionResult>=0) {
	     		 	return  Math.pow(functionResult,3);
	     	 	}
     		 else {
     			    functionResult = functionResult * -1;
     			    functionResult = (Math.pow(functionResult,2.5));
     			    return (-1 *functionResult);
     		 	}
     		 }  
     	 
    	//---------------------------------------
     	// n/r 
     	//---------------------------------------        	
    	 else if (version ==2){
    		 	 infoAbtComb="N/R";
    		     simFactor = (commonUsersSize/alpha);   		 	 
	    		 return  (functionResult * simFactor);
     		 	}        	
    
    	//---------------------------------------
      	// n/(r+n) 
      	//---------------------------------------        	
     
    	 else if (version ==3){
    		 	 infoAbtComb="N/N+R";
    		 	 simFactor = (commonUsersSize/(alpha + commonUsersSize));   		 	 
	    		 return  (functionResult * simFactor);
		 	}        	
	
    	//---------------------------------------
    	// Max of (Ia and Ib) and alpha
    	//---------------------------------------
    	 else if (version ==4){
    		 		 infoAbtComb="MAX";
	         		 double max = Math.max(commonUsersSize, alpha);        	     	
		    		 simFactor = (max/alpha);
		    		 return  (functionResult * simFactor);
	    		 }
    	
    	//---------------------------------------
    	// Min of (Ia and Ib) and alpha
    	//---------------------------------------
    	 else if(version==5) {
    		 		 infoAbtComb="MIN";
		    		 double min = Math.min(commonUsersSize, alpha);   	 
		    		 simFactor = (min/alpha);
		    		 return  (functionResult * simFactor);
    	 		}

       	//---------------------------------------
       	// My Approach Log, +ve (n/r) and -ve (log10(s+2))
       	//---------------------------------------
       	 else if(version==6) {
       		 		 infoAbtComb="+ve(n/r), -ve(log s+2)";
   		    		 double min = Math.min(commonUsersSize, alpha);   	
   		    		 double max = Math.max(commonUsersSize, alpha);
   		    		 
   		    		 if(functionResult>=0) {					    //+ve case
   		    			simFactor = (commonUsersSize/alpha);
   		    			return  functionResult * simFactor;  		    		     		    		 
       	 			}
   		    		
       	 			else { 										//-ve case
   		    		     functionResult = (functionResult+2);
   		    		     return Math.log10(functionResult);
   		    		  } 		    		 
   		    		 
       	 }
     	 

     	//---------------------------------------
        // My Approach Log, and then de-valuate
     	// r/n+r
        //---------------------------------------
        
       	  else if(version==7) {
        		 		 infoAbtComb="+ve(n/r), N/N+R[(log (s+2))]";
    		    		 double min = Math.min(commonUsersSize, alpha);   	
    		    		 double max = Math.max(commonUsersSize, alpha);
    		    		 
    		    		 if(functionResult>=0) {					    //+ve case
    		    			simFactor = (commonUsersSize/alpha);
    		    		  		    		     		    		 
        	 		        }
    		    		
        	 			else { 										//-ve case
    		    		     functionResult =  Math.log10(functionResult+2);
    		    		     simFactor = commonUsersSize/(commonUsersSize + alpha);   		    		     
    		    		  } 		    		 
    		    		 
    		    		return  (functionResult * simFactor);
        	         }  
     	

     	//---------------------------------------
      	// My Approach Log, and then de-valuate
   		// CA like thing
      	//---------------------------------------
      	
       	  else if(version==8) {
      		         infoAbtComb="+ve(n/r), CA (2,3)[(log (s+2))]";
  		    		 double min = Math.min(commonUsersSize, alpha);   	
  		    		 double max = Math.max(commonUsersSize, alpha);
  		    		 
  		    		 if(functionResult>=0) {					    //+ve case
  		    			simFactor = (commonUsersSize/alpha);
  		    			return  (functionResult * simFactor);
      	 			}
  		    		
      	 			else { 						    				//-ve case
  		    		     functionResult =  Math.log10(functionResult+2);
  		    		     if(commonUsersSize < alpha)
  		    		    	 return Math.pow(functionResult, 3);   // more devaluate
  		    		     else 
  		    		    	 return Math.pow(functionResult, 2);  // less devaluate
  		    		     
  		    		  }  		
      	  }      	

     	 	//---------------------------------------
        	// My Approach, +ve and -ve Separate
       		// min/max (n/n+r)
        	//---------------------------------------
        	
       	  else if(version==9) {
        		 		 infoAbtComb="+ve(n/n+r), n/max+n[log(s+2)]";
    		    		 double min = Math.min(commonUsersSize, alpha);   	
    		    		 double max = Math.max(commonUsersSize, alpha);
    		    		 
    		    		 if(functionResult>=0) 						//+ve case	 
    		    			 simFactor = commonUsersSize/(alpha); 		    		 
    		    		 else 										//-ve case
    		    			 simFactor = commonUsersSize/(max + commonUsersSize);
    		    			 
    		    		 return  (functionResult * simFactor);
        	 		}     	
     	 
  	    //---------------------------------------
      	// My Approach, +ve (n/r) and -ve (s+1/10)
      	//---------------------------------------
    	 else if(version==10) {
    		 		 infoAbtComb="+ve(n/r), -ve(s+1/10)";
  		    		 double min = Math.min(commonUsersSize, alpha);   	
  		    		 double max = Math.max(commonUsersSize, alpha);
  		    		 
  		    		 if(functionResult>=0) { 							//+ve case	 
  		    			 simFactor = (commonUsersSize/alpha);
  		    			 return  functionResult * simFactor;
  		    		 }
  		    		 
  		    		 else { 										//-ve case
  		    		     functionResult = (functionResult+1)/10.0;
  		    		      return  functionResult;
  		    		  }  		    		 
  		    		 
      	 }   	 


   	    //---------------------------------------
       	// My Approach, +ve (n/r) and -ve n/n+r[(s+1/10)]
       	//---------------------------------------
     	 else if(version==11) {
     		 		 infoAbtComb="+ve(n/r), -ve(n/n+r[s+1/10])";
   		    		 double min = Math.min(commonUsersSize, alpha);   	
   		    		 double max = Math.max(commonUsersSize, alpha);
   		    		 
   		    		 if(functionResult>=0) 							//+ve case	 
   		    			 simFactor = (commonUsersSize/alpha); 		    		 
   		    		 
   		    		 else { 										//-ve case
   		    		     functionResult = (functionResult+1)/10.0;
   		    		     simFactor = commonUsersSize/(commonUsersSize + alpha);
   		    		  }
   		    		 
   		    		 return  functionResult * simFactor;
       	 }   	 

     	 // it is not good, see min/max
  

 
        
     	//---------------------------------------
        // My Approach, +ve and -ve Separate
       	// min/max (n/n+r)
        //---------------------------------------
        	 else if(version==13) {
        		         infoAbtComb="+ve(n/r), n+r/n";
    		    		 double min = Math.min(commonUsersSize, alpha);   	
    		    		 double max = Math.max(commonUsersSize, alpha);
    		    		 
    		    		 if(functionResult>=0) 						//+ve case	 
    		    			 simFactor = (commonUsersSize/(alpha)); 		    		 
    		    		 else 										//-ve case
    		    			 simFactor = ((commonUsersSize+alpha)/(commonUsersSize));
    		    			 
    		    		 return  (functionResult * simFactor);
        	 		}
     	 
     	 //---------------------------------------
         // My Approach, +ve and -ve Separate
         // min/max (n/n+r)
         //---------------------------------------
         	 else if(version==14) {
         		         infoAbtComb="+ve(n/r), n/n+r";
     		    		  		    		 
     		    		 if(functionResult>=0) 						//+ve case	 
     		    			 simFactor = (commonUsersSize/(alpha)); 		    		 
     		    		 else 										//-ve case
     		    			 simFactor = (commonUsersSize/(alpha + commonUsersSize));
     		    			 
     		    		 return  (functionResult * simFactor);
         	 		}
         	 		
      	 //---------------------------------------
         // My Approach, sim = sim +1
         //---------------------------------------
         	 else if(version==15) {     		
         		 		 infoAbtComb="n/n+r [s+1]";
         		         functionResult+=1; 
     		    		 simFactor = (commonUsersSize/(alpha + commonUsersSize));   		 	 
     		    		 return  (functionResult * simFactor);
         	 		}
    
      	 //---------------------------------------
         // My Approach, sim = sim +1
         //---------------------------------------
         	 else if(version==16) {     		
         		 		 infoAbtComb="[s+1]";
         		         functionResult+=1;     		    		   		 	 
     		    		 return  (functionResult);
         	 		}
     	
     	 //---------------------------------------
         // My Approach, sim = sim +1
         //---------------------------------------
        
         	 else if(version==17) {     		
 		 		 infoAbtComb="n+min/n+max";
 		 		 double min = Math.min(commonUsersSize, alpha);   	
		    		 double max = Math.max(commonUsersSize, alpha);
		    		 
		    		 if(functionResult>=0) 						//+ve case	 
		    			 simFactor = (commonUsersSize/(alpha)); 		    		 
		    		 else 										//-ve case
		    			 simFactor = ((commonUsersSize + min)/(commonUsersSize +max));
		    			 
		    		 functionResult=  (functionResult * simFactor);
 		     
 	 		}
     	
     	 //---------------------------------------
         // My Approach, sim = sim +1
         //---------------------------------------        
     	 
         	 else if(version==17) {     		
 		 		 infoAbtComb="n/r, n/n+max";
 		 		 double min = Math.min(commonUsersSize, alpha);   	
		    		 double max = Math.max(commonUsersSize, alpha);
		    		 
		    		 if(functionResult>=0) 						//+ve case	 
		    			 simFactor = (commonUsersSize/(alpha)); 		    		 
		    		 else 										//-ve case
		    			 simFactor = ((commonUsersSize)/(commonUsersSize +max));
		    			 
		    		 functionResult=  (functionResult * simFactor);
 		     
 	 		}

     	 //---------------------------------------
         // My Approach, +ve , -ve separate
         //---------------------------------------        
     	 
         	 else if(version==18) {     		
 		 		 infoAbtComb="n/r, n/n+max";
 		 		 double min = Math.min(commonUsersSize, alpha);   	
		    		 double max = Math.max(commonUsersSize, alpha);
		    		 
		    		 if(functionResult>=0) 						//+ve case	 
		    			 simFactor = (commonUsersSize/(alpha)); 		    		 
		    		 else 										//-ve case
		    			 simFactor = ((commonUsersSize)/(commonUsersSize +max + alpha));
		    			 
		    		 functionResult=  (functionResult * simFactor);
 		     
 	 		}
     	 
    	 //---------------------------------------
         // My Approach, +ve , -ve separate
         //---------------------------------------        
     	 
         	 else if(version==19) {     		
 		 		 infoAbtComb="n/r+max";
 		 		 double min = Math.min(commonUsersSize, alpha);   	
		    		 double max = Math.max(commonUsersSize, alpha);
		    		 
		    		 if(functionResult>=0) 						//+ve case	 
		    			 simFactor = (commonUsersSize/(alpha)); 		    		 
		    		 else 										//-ve case
		    			 simFactor = ((commonUsersSize)/(max + alpha));
		    			 
		    		 functionResult=  (functionResult * simFactor);
 		     
 	 		}
     	 
    */ 	 
     	return functionResult;
     	 
    	}
    
     	 //return functionResult;
     	 return -100;
    	 
    }
    

/************************************************************************************************************************/
    /**
     * Find similarity between two items, filtered by neighbours
     *  
     */

    public double findHybridSimilarity( int mid1, int mid2, IntArrayList neighbours)    
    {
        ArrayList<Triple> commonUsers = myTrainingSet.getCommonUserRatAndAve(mid1, mid2, neighbours);
    
        if (commonUsers.size() < 1) return  -100.0;	//just like threshold
        double num = 0.0, den1 = 0.0, den2 = 0.0;
        
        for (Triple u : commonUsers)        
        {
            double diff1 = u.r1 - u.a;
            double diff2 = u.r2 - u.a;
    
       /*     if (!(u.r1 <=5 && u.r1>0)) System.out.println("r1 =" + (u.r1));
            if (!(u.r2 <=5 && u.r2>0)) System.out.println("r2 =" + (u.r2));*/
            
            num += diff1 * diff2;
            
            den1 += diff1 * diff1;
            den2 += diff2 * diff2;
        }
        
        double den = Math.sqrt(den1) * Math.sqrt(den2);
        
                
        if (den == 0.0) return 0.0;
        
      //  if(num/den <0) System.out.println("Less than zero sim =" + (num/den));
      //  if(num/den >1.1) System.out.println("Greater than one sim =" + (num/den));
        
        return num / den;
    }

    
/************************************************************************************************************************/
     
      
    /**
     *  Recommend the target item to active user 
     * @param uid - the user to predict the rating for
     * @param mid - the movie the predict against
     * @param date - the date the rating would be made (irrelevant)
     * @return	the predicted rating for this user on this movie.
     */

    public double recommend(int uid, int mid,  int totalNeighbours, int comb, int alpha) //called from above    
    {
    	    double result = 0;
    		//---------------
    		//Item-Based CF
			//---------------
    	    if(CF.equalsIgnoreCase("item")) { 
    		 //double resultItemBasedCF = weightedSum(mid, uid,totalNeighbours,  comb, alpha);
    		 double resultItemBasedCF = weightedNegSum(mid, uid,totalNeighbours,  comb, alpha); 
    		 //return resultItemBasedCF;
    		 result = resultItemBasedCF;
    	    }
    		
    	
    		//---------------
    		//User-Based CF
			//---------------
    	    if(CF.equalsIgnoreCase("user")) {
    	    	double resultUserBasedCF=0;
    		//double resultUserBasedCF = myUserBasedFilter.recommendS(uid, mid, totalNeighbours, comb, alpha);
    	    	//double resultUserBasedCF = myUserBasedFilter.recommendEnemies(uid, mid, totalNeighbours, comb, alpha);
    		//return resultUserBasedCF;
    		result = resultUserBasedCF;
    	    }
    		 
    	    return result;
    		//---------------
    		// Hybrid
			//---------------
    	
    		//double resultUserItemBasedCF = UserAndItemBased(uid, mid, totalNeighbours, comb);    		
    		
    		
    		// Check the corrsponsing answers 
    		/*System.out.println("Item-Based Prediction = " +resultItemBasedCF);
    		System.out.println("User-Based Prediction = " +resultUserBasedCF);
    		System.out.println("Hybrid Prediction = " +resultUserItemBasedCF);
    		System.out.println("Acttual Rating = " +myTestSet.getRating(uid, mid));    		
    		System.out.println("Total items active user rated = " +myTrainingSet.getMoviesSeenByUser(uid).size());
    		System.out.println("Total Users who saw this item= " +myTrainingSet.getUsersWhoSawMovie(mid).size());
    		System.out.println("k ="+ totalNeighbours);
    		System.out.println("-------------------------------------------------------------------------");
    		*/
    		
    		//return resultUserItemBasedCF;
    		
    		
    	/*else
    		return regression(mid, uid,totalUsers);					// linear regression
    	 */    		
    }
 
 /************************************************************************************************************************/
    
    /**
     * Find weightedNegSum of Item-Based CF
     * @return item-based prediction
     * 
     */
    
    //In weighted sum, If we are not able to rtle to ansr a prediction, then we return 0, as
    //this is in accordance with the formula
    
  private double weightedNegSum(int movieID, int userID, int NumberOfNeighbours, int comb, int alpha)    
  {   
	    double answer  	   	    = 0;	  
     	double sumTop			= 0;
        double sumBottom		= 0;             

        //Movies seen by active user
        LongArrayList moviesSeenByActiveUser = myTrainingSet.getMoviesSeenByUser(userID);
        //if(moviesSeenByActiveUser.size()<=1) return 0;
                
        // All similar item, define variables        
        OpenIntDoubleHashMap itemIdToWeight = new  OpenIntDoubleHashMap();
        IntArrayList myItems      		 	= new IntArrayList();
        DoubleArrayList myWeights 		 	= new DoubleArrayList();
        double currentWeight =0;
        
        //Get all movies seen by active user: store their rating 
        int activeUserMovSize = moviesSeenByActiveUser.size();
        for (int i=0;i<activeUserMovSize;i++)        
        {
        	int mid = MemHelper.parseUserOrMovie(moviesSeenByActiveUser.getQuick(i));        	
        	
        	//To add the pair t results, with no SW and a SW scheme.
        	    currentWeight = findSimilarity (movieID, mid, comb, alpha);	
        	 //  currentWeight = findVectorSimilarity (movieID, mid, comb, alpha);
        		
        	if(currentWeight!=-100)
        		itemIdToWeight.put(mid, currentWeight);
        }       
        
        //Sort similar items, according to their weights
        myItems = itemIdToWeight.keys();
        myWeights = itemIdToWeight.values();
        itemIdToWeight.pairsSortedByValue(myItems, myWeights);
        int totalSimialrItems = myItems.size();

        IntArrayList midAlreadyThere = new IntArrayList();
        int dummy = 0;
        int dummyId = 0;
        
        
        // Go through total Similar items and return weighted sum /regression
        for (int i = totalSimialrItems-1, myTotal=0; i >=0; i--, myTotal++)       
        {    	   
        		if(myTotal == NumberOfNeighbours) break;        		
        		
        		//-------------------------------------
        		//For random picking of -ve users
        		//-------------------------------------
        		
        		// get some random item, The idea is we randomly, pick some neighbouring items rather than 
        		// the top rated and try to generate recommendations

        		while(true)
        		{
	        			try{
	        				  dummy = rand.nextInt(totalSimialrItems);  //select some random movies to delete (take their indexes) 
				 				}
	        			
	        			catch (Exception E){ 
	        					System.out.println(" error in random numbers");
	        					E.printStackTrace();
	 							}        			
	        			
	        			if(midAlreadyThere.contains(dummy)==false)
	        			{
	        				midAlreadyThere.add(dummy);
	        				break;
	        			}        			
        		}//end while
        		
        		int itemId = myItems.get(dummy);       
        		ArrayList<Triple> commonUsers = myTrainingSet.getCommonUserRatAndAve(movieID,itemId);
        	    double commonUsersSize = commonUsers.size();       
     		     currentWeight= myWeights.get(dummy);
        		          
        
        		
        		
        		//-------------------------------------
        		// For picking of Top -ve users
        		//-------------------------------------
        		
        		
        		/*int itemId = myItems.get(i);
        		
        		//find common user size between two movies,
        		ArrayList<Triple> commonUsers = myTrainingSet.getCommonUserRatAndAve(movieID,itemId);
        		double commonUsersSize = commonUsers.size();	
        		currentWeight= myWeights.get(i);
            	*/
            	
            	
            	//-------------------------------------
        		// Common steps for both approaches
        		//-------------------------------------
     		     
     		    double ActiveUserRating= myTrainingSet.getRating(userID, itemId);            	
            	totalNeigh++;
            	if (currentWeight >=0)			//count toal Pos and neg neighbours
            		posNeigh++;
            	else
            		negNeigh++;         
            		
            	// Consider All Weights            	
	             	/*sumBottom+= Math.abs(currentWeight);  //ADD ABSOLUTE WEIGHT
	             	sumTop+= ActiveUserRating * currentWeight;*/
	             	//sumTop+= (ActiveUserRating - myTrainingSet.getAverageRatingForUser(userID)) * currentWeight;
              
	             // Taste Approach
	           /*  	sumBottom+= Math.abs(currentWeight+1);
	             	sumTop+= ActiveUserRating * (currentWeight+1);*/
	             	
	             // +ve Weights
	             /*	if(currentWeight >0)
	             	{
		             	sumBottom+= Math.abs(currentWeight);
		             	sumTop+= ActiveUserRating * currentWeight;
	             	}*/
            	
            	
	            //-------------------------------------------------
	            // Transform the weight here
             	//-------------------------------------------------            	
            	
            	int version = comb;
            	double simFactor = 0;

             	//---------------------------------------
            	// No weighting 
            	//---------------------------------------        	
            	 if (version ==0){
            		 		infoAbtComb="No; ";
            		 		currentWeight  = currentWeight ; 
            		 	}
            		 	        	
            	
              	//---------------------------------------
             	// CA
             	//---------------------------------------        	
             	 if (version ==1){
             		 infoAbtComb="CA";
             		 if(currentWeight>=0) {
        	     		 	return  Math.pow(currentWeight,3);
        	     	 	}
             		 else {
             			    currentWeight = currentWeight * -1;
             			    currentWeight = (Math.pow(currentWeight,2.5));
             			   currentWeight =  -1 *currentWeight;
             		 	}
             		 }  
             	 
            	//---------------------------------------
             	// n/r 
             	//---------------------------------------        	
            	 else if (version ==2){
            		 	 infoAbtComb="N/R";
            		     simFactor = (commonUsersSize/alpha);   		 	 
            		     currentWeight=  (currentWeight * simFactor);
             		 	}        	
            
            	//---------------------------------------
              	// n/(r+n) 
              	//---------------------------------------        	
             
            	 else if (version ==3){
            		 	 infoAbtComb="N/N+R";
            		 	 simFactor = (commonUsersSize/(alpha + commonUsersSize));   		 	 
            		 	currentWeight=  (currentWeight * simFactor);
        		 	}        	
        	
            	//---------------------------------------
            	// Max of (Ia and Ib) and alpha
            	//---------------------------------------
            	 else if (version ==4){
            		 		 infoAbtComb="MAX";
        	         		 double max = Math.max(commonUsersSize, alpha);        	     	
        		    		 simFactor = (max/alpha);
        		    		 currentWeight=  (currentWeight * simFactor);
        	    		 }
            	
            	//---------------------------------------
            	// Min of (Ia and Ib) and alpha
            	//---------------------------------------
            	 else if(version==5) {
            		 		 infoAbtComb="MIN";
        		    		 double min = Math.min(commonUsersSize, alpha);   	 
        		    		 simFactor = (min/alpha);
        		    		 currentWeight=  (currentWeight * simFactor);
            	 		}

               	//---------------------------------------
               	// My Approach Log, +ve (n/r) and -ve (log10(s+2))
               	//---------------------------------------
               	 else if(version==6) {
               		 		 infoAbtComb="+ve(n/r), -ve(log s+2)";
           		    		 double min = Math.min(commonUsersSize, alpha);   	
           		    		 double max = Math.max(commonUsersSize, alpha);
           		    		 
           		    		 if(currentWeight>=0) {					    //+ve case
           		    			simFactor = (commonUsersSize/alpha);
           		    			currentWeight=  currentWeight * simFactor;  		    		     		    		 
               	 			}
           		    		
               	 			else { 										//-ve case
           		    		     currentWeight = (currentWeight+2);
           		    		     currentWeight= Math.log10(currentWeight);
           		    		  } 		    		 
           		    		 
               	 }
             	 

             	//---------------------------------------
                // My Approach Log, and then de-valuate
             	// r/n+r
                //---------------------------------------
                
               	  else if(version==7) {
                		 		 infoAbtComb="+ve(n/r), N/N+R[(log (s+2))]";
            		    		 double min = Math.min(commonUsersSize, alpha);   	
            		    		 double max = Math.max(commonUsersSize, alpha);
            		    		 
            		    		 if(currentWeight>=0) {					    //+ve case
            		    			simFactor = (commonUsersSize/alpha);
            		    		  		    		     		    		 
                	 		        }
            		    		
                	 			else { 										//-ve case
            		    		     currentWeight =  Math.log10(currentWeight+2);
            		    		     simFactor = commonUsersSize/(commonUsersSize + alpha);   		    		     
            		    		  } 		    		 
            		    		 
            		    		currentWeight=  (currentWeight * simFactor);
                	         }  
             	

             	//---------------------------------------
              	// My Approach Log, and then de-valuate
           		// CA like thing
              	//---------------------------------------
              	
               	  else if(version==8) {
              		         infoAbtComb="+ve(n/r), CA (2,3)[(log (s+2))]";
          		    		 double min = Math.min(commonUsersSize, alpha);   	
          		    		 double max = Math.max(commonUsersSize, alpha);
          		    		 
          		    		 if(currentWeight>=0) {					    //+ve case
          		    			simFactor = (commonUsersSize/alpha);
          		    			currentWeight=  (currentWeight * simFactor);
              	 			}
          		    		
              	 			else { 						    				//-ve case
          		    		     currentWeight =  Math.log10(currentWeight+2);
          		    		     if(commonUsersSize < alpha)
          		    		    	 currentWeight= Math.pow(currentWeight, 3);   // more devaluate
          		    		     else 
          		    		    	 currentWeight= Math.pow(currentWeight, 2);  // less devaluate
          		    		     
          		    		  }  		
              	  }      	

          	    //---------------------------------------
              	// My Approach, +ve (n/r) and -ve (s+1/10)
              	//---------------------------------------
            	 else if(version==9) {
            		 		 infoAbtComb="+ve(n/r), -ve(s+1/10)";
          		    		 double min = Math.min(commonUsersSize, alpha);   	
          		    		 double max = Math.max(commonUsersSize, alpha);
          		    		 
          		    		 if(currentWeight>=0) { 							//+ve case	 
          		    			 simFactor = (commonUsersSize/alpha);
          		    			 currentWeight=  currentWeight * simFactor;
          		    		 }
          		    		 
          		    		 else { 										//-ve case
          		    		     currentWeight = (currentWeight+1)/10.0;
          		    		      currentWeight=  currentWeight;
          		    		  }  		    		 
          		    		 
              	 }   	 


           	    //---------------------------------------
               	// My Approach, +ve (n/r) and -ve n/n+r[(s+1/10)]
               	//---------------------------------------
             	 else if(version==10) {
             		 		 infoAbtComb="+ve(n/r), -ve(n/n+r[s+1/10])";
           		    		 double min = Math.min(commonUsersSize, alpha);   	
           		    		 double max = Math.max(commonUsersSize, alpha);
           		    		 
           		    		 if(currentWeight>=0) 							//+ve case	 
           		    			 simFactor = (commonUsersSize/alpha); 		    		 
           		    		 
           		    		 else { 										//-ve case
           		    		     currentWeight = (currentWeight+1)/10.0;
           		    		     simFactor = commonUsersSize/(commonUsersSize + alpha);
           		    		  }
           		    		 
           		    		 currentWeight=  currentWeight * simFactor;
               	 }   	 

             	 // it is not good, see min/max
                
              	//---------------------------------------
               	// My Approach Log, and then de-valuate
            	// max
               	//---------------------------------------
               	 else if(version==11) {
               		 		 infoAbtComb="+ve(n/r), max[(log (s+2))]";
           		    		 double min = Math.min(commonUsersSize, alpha);   	
           		    		 double max = Math.max(commonUsersSize, alpha);
           		    		 
           		    		 if(currentWeight>=0) {					    //+ve case
           		    			simFactor = (commonUsersSize/alpha);
           		    		  		    		     		    		 
               	 		        }
           		    		
               	 			else { 										//-ve case
           		    		     currentWeight =  Math.log10(currentWeight+2);
           		    		     
           		    		     simFactor = commonUsersSize/(10 * max + commonUsersSize);   		    		     
           		    		  } 		    		 
           		    		 
           		    		currentWeight=  (currentWeight * simFactor);
               	         }  
            	

         
            	 
            	//Just to tell, min/max, or max/min...they do nothing
                //---------------------------------------
               	// My Approach, +ve and -ve Separate
              	// min/max (n/n+r)
               	//---------------------------------------
    /*           	 else if(version==7) {
               		 		 infoAbtComb="+ve(n/n+r), max/min+n[-ve]";
           		    		 double min = Math.min(commonUsersSize, alpha);   	
           		    		 double max = Math.max(commonUsersSize, alpha);
           		    		 
           		    		 if(currentWeight>=0) 						//+ve case	 
           		    			 simFactor = (commonUsersSize/(alpha + commonUsersSize)); 		    		 
           		    		 else 										//-ve case
           		    			 simFactor = max/(min+commonUsersSize);
           		    			 
           		    		 currentWeight=  (currentWeight * simFactor);
               	 		}     	
            	 */
                
             	//---------------------------------------
                // My Approach, +ve and -ve Separate
               	// min/max (n/n+r)
                //---------------------------------------
                	 else if(version==12) {
                		         infoAbtComb="+ve(n/r), n+r/n";
            		    		 double min = Math.min(commonUsersSize, alpha);   	
            		    		 double max = Math.max(commonUsersSize, alpha);
            		    		 
            		    		 if(currentWeight>=0) 						//+ve case	 
            		    			 simFactor = (commonUsersSize/(alpha)); 		    		 
            		    		 else 										//-ve case
            		    			 simFactor = ((commonUsersSize+alpha)/(commonUsersSize));
            		    			 
            		    		 currentWeight=  (currentWeight * simFactor);
                	 		}
             	 
             	 //---------------------------------------
                 // My Approach, +ve and -ve Separate
                 // min/max (n/n+r)
                 //---------------------------------------
                  	 else if(version==13) {
           		 		 infoAbtComb="+ve(n/r), max[(log (s+2))]";
       		    		 double min = Math.min(commonUsersSize, alpha);   	
       		    		 double max = Math.max(commonUsersSize, alpha);
       		    		 
       		    		 if(currentWeight>=0) {					    //+ve case
       		    			simFactor = (commonUsersSize/alpha);
       		    		  		    		     		    		 
           	 		        }
       		    		
           	 			else { 										//-ve case
       		    		     currentWeight =  Math.log10(currentWeight+2);       		    		     
       		    		     simFactor = commonUsersSize/(max + commonUsersSize);   		    		     
       		    		  } 		    		 
       		    		 
       		    		currentWeight=  (currentWeight * simFactor);
           	         }  
        	

             	 
              	 //---------------------------------------
                 // My Approach, sim = sim +1
                 //---------------------------------------
                 	 else if(version==14) {     		
                 		 		 infoAbtComb="n/n+r [s+1]";
                 		         currentWeight+=1;             		    		
                 		         simFactor = (commonUsersSize/(alpha + commonUsersSize));   		 	 
             		    		
             		    		 /*if(NumberOfNeighbours==9)
             		    			 System.out.print("w="+ (currentWeight-1)+ ",  W=" +currentWeight);
             		    		 */
             		    		 currentWeight=  (currentWeight * simFactor);
             		    		 
             		 /*   		 if(NumberOfNeighbours==9)
             		    			 System.out.println("\t n= " + commonUsersSize+ "\t w'="+ currentWeight); 
                 	 */		
                 	 }
            
              	 //---------------------------------------
                 // My Approach, sim = sim +1
                 //---------------------------------------
                 	 else if(version==15) {     		
                 		 		 infoAbtComb="[s+1]";
                 		         currentWeight+=1;     		    		   		 	 
             		    		 currentWeight=  (currentWeight);
                 	 		}
             	 
              	 //---------------------------------------
                 // My Approach, sim = sim +1
                 //---------------------------------------
                 	 else if(version==16) {     		
                 		 		 infoAbtComb="n+min/n+max";
                 		 		 double min = Math.min(commonUsersSize, alpha);   	
               		    		 double max = Math.max(commonUsersSize, alpha);
               		    		 
             		    		 if(currentWeight>=0) 						//+ve case	 
             		    			 simFactor = (commonUsersSize/(alpha)); 		    		 
             		    		 else 										//-ve case
             		    			 simFactor = ((commonUsersSize + min)/(commonUsersSize +max));
             		    			 
             		    		 currentWeight=  (currentWeight * simFactor);
                 		     
                 	 		}
            	
         	    //---------------------------------------
                // My Approach, +ve (n/r) and -ve n/n+r[(s+1/10)]
                //---------------------------------------
              	
                	  else if(version==17) {
         		 		 infoAbtComb="+ve(n/r), N/R[(log (s+2))]";
     		    		 double min = Math.min(commonUsersSize, alpha);   	
     		    		 double max = Math.max(commonUsersSize, alpha);
     		    		 
     		    		 if(currentWeight>=0) {					    //+ve case
     		    			simFactor = (commonUsersSize/alpha);
     		    		  		    		     		    		 
         	 		        }
     		    		
         	 			else { 										//-ve case
     		    		     currentWeight =  Math.log10(currentWeight+2);
     		    		     simFactor = commonUsersSize/( alpha);   		    		     
     		    		  } 		    		 
     		    		 
     		    		currentWeight=  (currentWeight * simFactor);
         	         }  
      	
            	//if(currentWeight <0)
	             	{
		             	sumBottom+= Math.abs(currentWeight);
		             	//sumTop+= ActiveUserRating * currentWeight;
		             	sumTop+= (ActiveUserRating - myTrainingSet.getAverageRatingForUser(userID)) * currentWeight;
	             	}
	             	
      
        	
        	
        	/*else if(comb ==1)
        	{
        		
    	    	 double approxRating = predictKnownRating(movieID, itemId, userID);	

         		//simple
         		currentWeight= myWeights.get(i);
             	double ActiveUserRating= myTrainingSet.getRating(userID, itemId);

             	// Consider All Weights            	
 	             	sumBottom+= Math.abs(currentWeight);
 	             	sumTop+= ActiveUserRating * currentWeight;
 
 	             // Taste Approach
 	             	sumBottom+= Math.abs(currentWeight+1);
 	             	sumTop+= ActiveUserRating * (currentWeight+1);
 	             	
             	
 	             // +ve Weights
 	             	if(currentWeight >0)
 	             	{
 		             	sumBottom+= Math.abs(currentWeight);
 		             	sumTop+= ActiveUserRating * currentWeight;
 	             	} 	             	
       	
    	    	 
        		
        	} //end of else if    	
        	
*/        	
	     } //end of for
        
               	
	        // if user didn't see any similar movies give avg rating for user
        	// However, we must take this into account that, CF has not been able to
            // predict that rating....as its actual function is not working        
        
	        if (sumBottom == 0)
	        	//return db.getAverageRatingForUser(userID);
	        	{
	        	    totalZeroAnswers1++;
	        	//	System.out.println(" bottonSum=0 -->" + sumBottom);
	        		//return  myTrainingSet.getAverageRatingForUser(userID);        	
	        	  //  return (answer =0); 		//sparsity challenge (active user have not rated any similar movie)
	        	    return -1;
	        	}
	        
	        	//	answer = sumTop/sumBottom;
	        		answer = myTrainingSet.getAverageRatingForUser(userID) + sumTop/sumBottom;	 
	        
	      /*   if (answer<0) 
	        	 {
	        	     totalNegativeAnswers1++;
	        	 	// System.out.println(" answer<0 -->" + answer);
	        	 	// return myTrainingSet.getAverageRatingForUser(userID);
	        	   return  (answer =0);
	        	 
	        	 }*/	        
	 
      return answer;
    }
  
/******************************************************************************************************/  
  /**
   * Find weightedSum of Item-Based CF
   * @return item-based prediction
   * 
   */
  
  //In weighted sum, If we are not able to rtle to ansr a prediction, then we return 0, as
  //this is in accordance with the formula
  
private double weightedSum(int movieID, int userID, int NumberOfNeighbours, int comb, int alpha)    
{   
	    double answer  	   	    = 0;	  
   	double sumTop			= 0;
      double sumBottom		= 0;             

      //Movies seen by active user
      LongArrayList moviesSeenByActiveUser = myTrainingSet.getMoviesSeenByUser(userID);
      //if(moviesSeenByActiveUser.size()<=1) return 0;
              
      // All similar item, define variables        
      OpenIntDoubleHashMap itemIdToWeight = new  OpenIntDoubleHashMap();
      IntArrayList myItems      		 	= new IntArrayList();
      DoubleArrayList myWeights 		 	= new DoubleArrayList();
      double currentWeight =0;
      
      //Get all movies seen by active user: store their rating 
      int activeUserMovSize = moviesSeenByActiveUser.size();
      for (int i=0;i<activeUserMovSize;i++)        
      {
      	int mid = MemHelper.parseUserOrMovie(moviesSeenByActiveUser.getQuick(i));        	
      	
      	//To add the pair t results, with no SW and a SW scheme.
      	    currentWeight = findSimilarity (movieID, mid, comb, alpha);	
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
      		
      	
//      	if(comb==0)
      	{
      		//simple
      		currentWeight= myWeights.get(i);
          	double ActiveUserRating= myTrainingSet.getRating(userID, itemId);
          	
          	totalNeigh++;
          	if (currentWeight >=0)			//count toal Pos and neg neighbours
          		posNeigh++;
          	else
          		negNeigh++;         
          		
          	// Consider All Weights            	
	             	/*sumBottom+= Math.abs(currentWeight);  //ADD ABSOLUTE WEIGHT
	             	sumTop+= ActiveUserRating * currentWeight;*/
	             	//sumTop+= (ActiveUserRating - myTrainingSet.getAverageRatingForUser(userID)) * currentWeight;
            
	             // Taste Approach
	           /*  	sumBottom+= Math.abs(currentWeight+1);
	             	sumTop+= ActiveUserRating * (currentWeight+1);*/
	             	
	             // +ve Weights
	             /*	if(currentWeight >0)
	             	{
		             	sumBottom+= Math.abs(currentWeight);
		             	sumTop+= ActiveUserRating * currentWeight;
	             	}*/
          	
          	
          	
          	//if(currentWeight <0)
	             	{
		             	sumBottom+= Math.abs(currentWeight);
		             	sumTop+= ActiveUserRating * currentWeight;
		             	//sumTop+= (ActiveUserRating - myTrainingSet.getAverageRatingForUser(userID)) * currentWeight;
	             	}
	             	
        }//end of if
      	
      	
      	/*else if(comb ==1)
      	{
      		
  	    	 double approxRating = predictKnownRating(movieID, itemId, userID);	

       		//simple
       		currentWeight= myWeights.get(i);
           	double ActiveUserRating= myTrainingSet.getRating(userID, itemId);

           	// Consider All Weights            	
	             	sumBottom+= Math.abs(currentWeight);
	             	sumTop+= ActiveUserRating * currentWeight;

	             // Taste Approach
	             	sumBottom+= Math.abs(currentWeight+1);
	             	sumTop+= ActiveUserRating * (currentWeight+1);
	             	
           	
	             // +ve Weights
	             	if(currentWeight >0)
	             	{
		             	sumBottom+= Math.abs(currentWeight);
		             	sumTop+= ActiveUserRating * currentWeight;
	             	} 	             	
     	
  	    	 
      		
      	} //end of else if    	
      	
*/        	
	     } //end of for
      
             	
	        // if user didn't see any similar movies give avg rating for user
      	// However, we must take this into account that, CF has not been able to
          // predict that rating....as its actual function is not working        
      
	        if (sumBottom == 0)
	        	//return db.getAverageRatingForUser(userID);
	        	{
	        	    totalZeroAnswers1++;
	        	//	System.out.println(" bottonSum=0 -->" + sumBottom);
	        		//return  myTrainingSet.getAverageRatingForUser(userID);        	
	        	    return (answer =0); 		//sparsity challenge (active user have not rated any similar movie)
	        	   // return -1;
	        	}
	        
	        		answer = sumTop/sumBottom;
	        	//	answer = myTrainingSet.getAverageRatingForUser(userID) + sumTop/sumBottom;	 
	        
	         if (answer<0) 
	        	 {
	        	     totalNegativeAnswers1++;
	        	 	// System.out.println(" answer<0 -->" + answer);
	        	 	// return myTrainingSet.getAverageRatingForUser(userID);
	        	   return  (answer =0);
	        	 
	        	 }	        
	 
    return answer;
  }

/************************************************************************************************************************/
    /**
     * @author steinbel
     * Uses a regression method to find a predicted integer rating
     * from 1-5 for a user on an unrated movie.
     * 
     * @param movieID   The movie for which a rating should be predicted.
     * @param userID    The active user for whom we make the prediction.
     * @return  the predicted rating for this user on this movie.
     */ 
    
    
    /**
     * @author steinbel
     * Builds a linear regression model combining the rating vectors for both movies
     * and using the userIDs as the independent variable.
     * 
     * @param movie1	The movie (id #) to use as an independent variable.
     * @param movie2	The movie (id #) to use for a dependent variable
     * @param userID	The user for whom the prediction on movie2 should be made.
     *
     * @return 	The predicted rating of the user on movie2.
     */

    // y = bX + m ( b= slope, m = y intercept)
    // b = sum(x-x') sum(y-y')/[sum(x-x')^2]    ???
    // m = y' - bx'
    //
    // movie, similar movie, userId
    
    private double predictKnownRating(int movie1, int movie2, int userID)    
    {
    	double predicted = -99;

    	//________________
	    //build the model:
    	//________________
    	
	    //grab the rating vector for movie1 in <user, rating> pairs
        //ArrayList<Pair> targetV = db.getRatingVector(movie1);
        ArrayList<Pair> targetV = myTrainingSet.getRatingVector(movie1);
	    
        //grab the rating vector for movie2
        //ArrayList<Pair> simV = db.getRatingVector(movie2);
        ArrayList<Pair> simV = myTrainingSet.getRatingVector(movie2);
        
        //create one list of rating instances  (?????????????/)
        for (Pair p : simV)
        	targetV.add(p);

       /* grab mean (avg) ratings for movies 1 and 2 and for the userID
	    * calculate standard deviation for ratings at the same time for efficiency
	    */
        double meanRate = (myTrainingSet.getAverageRatingForMovie(movie1)
                            + myTrainingSet.getAverageRatingForMovie(movie2))/2;
        double meanUser = 0;
        double sdRate = 0;
        
        for (Pair p : targetV)         
        {
        	meanUser += p.d1;
            sdRate += ( ((double)p.d2 - meanRate)*((double)p.d2 - meanRate) );
        }        
        meanUser /= (targetV.size()-1);
        sdRate = Math.sqrt( sdRate/(targetV.size() - 1) );
	    
        //now find standard deviation for userIDs
        double sdUser = 0;       
        for (Pair p: targetV)
        	sdUser += ( ((double)p.d1 - meanUser)*((double)p.d1 - meanUser) );
        sdUser = Math.sqrt( sdUser/(targetV.size() - 1) );
         
        
	    //find correlation between user and rating
        double r = 0;        
        for (Pair p : targetV) 
        	r += ( ((p.d1 - meanUser)/sdUser) * ((p.d2 - meanRate)/sdRate) );
        r /= (targetV.size() - 1);

        //calculate the coefficients
        double c2 = r * (sdUser/sdRate);
        double c1 = meanUser - c2*meanRate;

        //TODO: calculate error for epsilon
        
        double rat = myTrainingSet.getRating(userID, movie2);		//I think, it is wrong, if correct, what is use of using regrssion 
        														//Means we are using the user's actual rating as well 
        
        if(rat ==-99) rat = myTrainingSet.getAverageRatingForMovie(movie2);
	    //assemble formula and use model to predict rating for user on movie2
        predicted = (int) Math.round(c1 + c2*rat);
     
       /*   
        System.out.println("prediction ="+ predicted);
        System.out.println("mean user ="+ meanUser);
        System.out.println("sdUser ="+ sdUser);
        System.out.println("meanRate ="+ meanRate);
        System.out.println("sdRate ="+ sdRate);        
        System.out.println("r ="+ r);
        System.out.println("c1 ="+ c1);
        System.out.println("c2 ="+ c2);*/
        
	    return predicted;
    }	   
    

 /************************************************************************************************************************/
    /**
     * Main Method,  
     */
    
    public static void main (String[] args)    
    {
       NSWS_IBCF 	rec;
       MemHelper h;
       Timer227  time= new Timer227();
       double 	 mae = 0,rmse; 
       double[]  pos = new double[10];
       double[]  neg = new double[10];
       
       // double final rmse of each version for 5 folds
       double myFinalMAE[] = new double[5];
       double myFinalROC[] = new double[5];
       double myFinalCov[] = new double[5];
       double myFinalTVal[] = new double[5];
       
       //Make 5 objects, to be used repeaetedly
       MemHelper localTrainObj[] = new MemHelper[5];
       MemHelper localTestObj[] = new MemHelper[5];    
       
       //pass parameters to open the databaseImpl object    	   
       rec = new NSWS_IBCF(true); 		//every time, different sim table
       
       //------------------------------------------------------------ 
       // Start Five Fold
       //------------------------------------------------------------
        
       //creat 5 object
       for (int i=1;i<=5;i++)
       {
    	   localTrainObj[i-1] = new MemHelper (rec.myPath + "ft_trainSetStoredFold" + (i) + ".dat");
    	   localTestObj[i-1] = new MemHelper(rec.myPath+ "ft_testSetStoredFold" + (i) + ".dat");
      
       }
       
      for (int i=1; i<=1; i++) //for five fold, i =6; for 20-80, i=2      
      {    	     	  
    	  System.out.println(" Currently at fold ="+ (i));
    	    	             
          //---------------------- 
          //SML
          //----------------------             
            
           if(rec.dataset==0) {
        	  
        	 //create training set which is stored in memory (for fast processing), each time different
        	   /*rec.myTrainingSet = new MemHelper (rec.myPath + "sml_trainSetStoredFold" + (i) + ".dat");
               rec.myTestSet = new MemHelper(rec.myPath+ "sml_testSetStoredFold" + (i) + ".dat");
               //User based Filter setting
               rec.myUserBasedFilter = new FilterAndWeight(rec.myTrainingSet,1);        //with mmh object
               */
        	   
	        /*//20-80
	         rec.myTrainingSet = new MemHelper (rec.myPath + "sml_clusteringTrainSetStoredTF.dat");
	         rec.myTestSet = new MemHelper(rec.myPath+ "sml_clusteringTestSetStoredTF.dat");
        
           //User based Filter setting
             rec.myUserBasedFilter = new FilterAndWeight(rec.myTrainingSet,1);        //with mmh object
          */   
        	   
             //Sparsity
        	  rec.myTrainingSet = new MemHelper (rec.myPath + "sml_clusteringTrainSetStoredTF.dat");
    	      rec.myTestSet = new MemHelper(rec.myPath+ "sml_clusteringTestSetStoredTF.dat");
            
              //User based Filter setting
              rec.myUserBasedFilter = new FilterAndWeight(rec.myTrainingSet,1);        //with mmh object
               
             
/*            // user-rating item sim (Pearson or Adjusted Cosine)
               //rec.myStoredSim = UserSimKeeper.deserialize(rec.myRSimPath + "/StoredSimP/SimFold" + (i)+ ".dat" );
               rec.myStoredRSim = UserSimKeeper.deserialize(rec.myPath + "/StoredRCSim/sml_SimFold" + (i)+ ".dat" );
        
            // demo sim 
               rec.myStoredDSim = UserSimKeeper.deserialize(rec.myPath + "/StoredDSim/sml_SimFold" + (i)+ ".dat" );
               
            //Feature Sim
               rec.myStoredFSim = UserSimKeeper.deserialize(rec.myPath + "/StoredFSim/sml_SimFold" + (i)+ ".dat" );
               
*/
     
             }
           
            //---------------------- 
            //ML
            //----------------------
           
           else if(rec.dataset==1){
/*        //create training set which is stored in memory (for fast processing), each time different         
            rec.myTrainingSet = new MemHelper (rec.myPath + "ml_trainSetStoredFold" + (i) + ".dat");
            rec.myTestSet = new MemHelper(rec.myPath+ "ml_testSetStoredFold" + (i) + ".dat");
*/          
           //20-80
	   /*      rec.myTrainingSet = new MemHelper (rec.myPath + "ml_clusteringTrainSetStoredTF.dat");
	         rec.myTestSet = new MemHelper(rec.myPath+ "ml_clusteringTestSetStoredTF.dat");
	     	      
            //User based Filter setting
              rec.myUserBasedFilter = new FilterAndWeight(rec.myTrainingSet,1); //with mmh object
*/

               //Sparsity
         	  rec.myTrainingSet = new MemHelper (rec.myPath + "ml_clusteringTrainSetStoredTF.dat");
     	      rec.myTestSet = new MemHelper(rec.myPath+ "ml_clusteringTestSetStoredTF.dat");
             
               //User based Filter setting
               rec.myUserBasedFilter = new FilterAndWeight(rec.myTrainingSet,1);        //with mmh object
                 
               
        	 

           }
            //---------------------- 
            //FT
            //----------------------
            else if(rec.dataset==2){
             //create training set which is stored in memory (for fast processing), each time different         
              /* rec.myTrainingSet = new MemHelper (rec.myPath + "ft_trainSetStoredFold" + (i) + ".dat");
               rec.myTestSet = new MemHelper(rec.myPath+ "ft_testSetStoredFold" + (i) + ".dat");
               
               //User based Filter setting
               rec.myUserBasedFilter = new FilterAndWeight(rec.myTrainingSet,3); //with mmh object
              */
            	
      /*     //20-80
	         rec.myTrainingSet = new MemHelper (rec.myPath + "ft_clusteringTrainSetStoredTF.dat");
	         rec.myTestSet = new MemHelper(rec.myPath+ "ft_clusteringTestSetStoredTF.dat");
	     
            //User based Filter setting
              rec.myUserBasedFilter = new FilterAndWeight(rec.myTrainingSet,3); //with mmh object
      */        
           
           }
          //-----------------------
          //Open File To write
          //-----------------------
             
           //open the connection with databaseImlp, result files
           if(i==1)    {	
        		  		// 	  rec.openFile(4, 1);  //mae, rmse, roc, coverage
           				//	  rec.openFile(5, 2);  //demo
           				//	  rec.openFile(5, 3);  //rating
           				//	  rec.openFile(5, 4);  //both
           			    //    rec.openSingleFile();
           			   }
          

           //-----------------------
           //Timers
           //-----------------------
           
           System.out.println(" ");
           time.resetTimer();
           time.start();
           

           //---------------------------
           // Go throug all Neighbours
           //---------------------------    
         
      for(int sparsity =0;sparsity<=0;sparsity++)
      {
    	   	System.out.println("====================================================");
        	System.out.println("              Sparsity =" + sparsity);
        	//System.out.println(rec.infoAbtComb);
        	System.out.println("====================================================");
        	
        	/*rec.bigInfo+="==============================\n";
        	rec.bigInfo+="Sparsity="+(sparsity);
        	rec.bigInfo+="==============================\n";
        	*/
        	
    	/*  if(sparsity>0)
    	  {
    		  //Sparsity, create sparse objects at each iteration
        	  
    		  if(rec.dataset==0)
    			 rec.myTrainingSet = new MemHelper (rec.myPath + "ml_trainSetStoredAll_80_"+ (sparsity) +".dat");
    		  else
    			  rec.myTrainingSet = new MemHelper (rec.myPath + "ml_trainSetStoredAll_80_"+ (sparsity) +".dat");
    	                 
              //User based Filter setting
              rec.myUserBasedFilter = new FilterAndWeight(rec.myTrainingSet,1);        //with mmh object
                
    	  }
        */   
        
        rec.incrementInK = 2;
        	
        	//we have to make loop here
        for (int myK=1, index=0;myK<=rec.totalK;myK+=rec.incrementInK, index++)    
         {      
        	//open file
        	rec.openSingleFile();
        	
        	System.out.println("---------------------------------------------------");
        	System.out.println("             neighbours= "+ myK);
        	//System.out.println(rec.infoAbtComb);
        	System.out.println("---------------------------------------------------");
        	        	
        	rec.bigInfo ="------------------------------\n";
        	rec.bigInfo+="neighbour="+(myK);
        	//rec.bigInfo+="actual neighbour= "+ ((rec.negPcc *100.0)/rec.totalPcc);
        	rec.bigInfo+="------------------------------\n";
        
	    	 //write results
	        try{
	      	  	rec.writeWeights.write(rec.bigInfo);
	        
	           }
	        catch(Exception E){
	      	  E.printStackTrace();
	        }
        	
	        //reset bigInfo
	        rec.bigInfo = "";
        	
	/*        rec.myTrainingSet = localTrainObj[0];
			rec.myTestSet = localTestObj[0];
			
	        mae = rec.GoTroughTestSet(rec.myTestSet,myK,0, 2, 0,0,0);   	
	    	
        	System.out.println("================================================================");
        	System.out.println("pos ="+ rec.posPcc + ", neg="+ rec.negPcc + ", total="+ rec.totalPcc);
        	System.out.println("actual neg ="+ ((rec.negPcc  *100 )/rec.totalPcc));
        	System.out.println("actual pos ="+ ((rec.posPcc  *100 )/rec.totalPcc));
        	System.out.println("K="+ myK);
        	System.out.println("================================================================");
        	
        	
	        System.out.println("================================================================");
        	System.out.println("pos ="+ rec.posNeigh + ", neg="+ rec.negNeigh + ", total="+ rec.totalNeigh);
        	System.out.println("actual neg considered ="+ ((rec.negNeigh  *100 )/rec.totalNeigh));
        	System.out.println("actual pos ="+ ((rec.posNeigh  *100 )/rec.totalNeigh));
        	System.out.println("K="+ myK);
        	System.out.println("================================================================");
        	
        	rec.posPcc = rec.negPcc = rec.totalPcc 		= 0;
        	rec.posNeigh = rec.negNeigh =rec.totalNeigh = 0;	*/
        	
	        
        	//-------------
        	// Learn Alpha
        	//-------------       	
          for (int alpha=2;alpha<30;alpha+=4)
          { 
        	  boolean newLine = false;
        	  System.out.println("------------------alpha =" + alpha +"-------------------------");        	 
        	  
        	  rec.bigInfo="-------------alpha="+ alpha +"-----------------\n";
        	 
        	  //write results
  	        try{
  	      	  rec.writeWeights.write(rec.bigInfo);
  	        
  	           }
  	        catch(Exception E){
  	      	  E.printStackTrace();
  	        }  
        	  
        	for(int version = 0; version<= 17; version++)
        	{ 
        		//reset bigInfo
        		rec.bigInfo = "";
        		
        		for(int folds =1;folds <=1;folds++)
        		{        		 
        			rec.myTrainingSet = localTrainObj[folds-1];
        			rec.myTestSet = localTestObj[folds-1];
        			                        	   
	           	 	if(version==6) newLine = true;
	        		mae = rec.GoTroughTestSet(rec.myTestSet,myK,version, alpha, 0,0,0);			 
	           	
	        		//for mean results
	        		myFinalMAE[folds-1]	= rec.rmse.mae();
	           	 	myFinalROC[folds-1]	= rec.rmse.getSensitivity();
	           	 	myFinalCov[folds-1]	= rec.rmse.getItemCoverage();
	           	   // myFinalTVal[folds-1]= rec.rmse.getPairT();
	       
	           	    
	           	    //reset rmse
	           		rec.rmse.resetPairTPrediction();	//reset prediction pair t
			      	rec.rmse.resetROC();
			      	rec.rmse.resetValues();   
			      	
	        		
	        		//time.stop(); 	           	 	
        		}
        		
           	  	           	 	
           	 			
           	 /*	pos[index]+= (rec.posNeigh *100.0)/rec.totalNeigh;  //store neighbour distirbution at each k
           	    neg[index]+= (rec.negNeigh *100.0)/rec.totalNeigh;
           	 	*/
           	 	/*	
           	 		if(version==0 && alpha ==10) {
           	 				System.out.println("Pos=" + (rec.posPcc *100.0)/rec.totalPcc +
           	 						", Neg="+(rec.negPcc *100.0)/rec.totalPcc); 
           	 			   System.out.println("PosAcive=" + (rec.posNeigh *100.0)/rec.totalNeigh +
       	 						", NegActive="+(rec.negNeigh *100.0)/rec.totalNeigh);
           	 	          
           	 			}*/
        				
        				int size =1;
           	 	 	
           	 	        String mean_meanAbsError =rec.nf.format(rec.myMeans.calculateMeanOrSD( myFinalMAE, size, 0));
           	 	        String SD_meanAbsError =rec.nf.format(rec.myMeans.calculateMeanOrSD( myFinalMAE,size, 1));
           	 	        String mean_sensitivity = rec.nf.format(rec.myMeans.calculateMeanOrSD( myFinalROC, size, 0));
           	 	        String SD_sensitivity = rec.nf.format(rec.myMeans.calculateMeanOrSD( myFinalROC, size, 1));
           	 	        
           	 	        String mean_coverage = rec.nf.format(rec.myMeans.calculateMeanOrSD( myFinalCov, size, 0));
           	 	        String SD_coverage = rec.nf.format(rec.myMeans.calculateMeanOrSD( myFinalCov, size, 1));
           	 	        String mean_pairT = rec.nf.format(rec.myMeans.calculateMeanOrSD( myFinalTVal, size, 0));
           	 	        String SD_pairT = rec.nf.format(rec.myMeans.calculateMeanOrSD( myFinalTVal, size, 1));
           	 	         
				    					    	
				    	rec.bigInfo+=version +"\t MAE ="+ mean_meanAbsError +    " : "+  SD_meanAbsError  
				    						 +",\t ROC =" + mean_sensitivity  +  " : "+  SD_sensitivity
				    						 + ",\n\t Cov ="+ mean_coverage +      " : "+  SD_coverage;
				    	
				    	if(version!=0) {
				    		//System.out.println(",Pair t="+mean_pairT);	
				    		rec.bigInfo+=",\tpairt ="+ mean_pairT+" : " + SD_pairT + "\n";
				    	}
				    	
				    	else{ 	System.out.println();
				    			rec.bigInfo+="\n";
				    	}
				    	
				    	System.out.print(rec.bigInfo);
				    	
				    	 //write results
				        try{
				      	  rec.writeWeights.write(rec.bigInfo);
				        
				           }
				        catch(Exception E){
				      	  E.printStackTrace();
				        }
				        
           	 	        //rec.writeIntoFile(version, newLine, myK, alpha);
				    	
				    	rec.totalNegativeAnswers					= 0; 
				      	rec.totalZeroAnswers						= 0;
				      	rec.posPcc = rec.negPcc = rec.totalPcc 		= 0;
				      	rec.posNeigh = rec.negNeigh =rec.totalNeigh = 0;		
				      	
				      	rec.rmse.resetPairTPrediction();	//reset prediction pair t
				      	rec.rmse.resetPairT();				//reset all pair-t values after all version
				      	rec.rmse.resetROC();
				      	rec.rmse.resetValues();
            		  
        	} //end of version for 
        	
        	System.gc();      
        	
          }// end of alpha for
          
              
          rec.closeSingleFile();
         }//end of K for  	
      
      }//end of sparsity for
    }//end of folds for 
      
    //-------------------------------------
      
      /*for (int i=0;i<10;i++)
    	  System.out.println("pos at " + ((i+1)*5) + (pos[i]/5.0) + ", neg= at " + ((i+1)*5)+ (neg[i]/5.0));
      */     
      
      //rec.closeSingleFile();
      //rec.closeFile(4, 1);
      
 
      
      
    }//end of main function

 /************************************************************************************************************************/
 
    /**
     * @param Memhelper Object, How much Neighbours
     * @return MAE
     */
    
    public double GoTroughTestSet(MemHelper testmh, int myNeighbours,
    							  int comb, int alpha, 
    							  double rW, double dW, double fW)     
    {
                
        IntArrayList users;
		LongArrayList movies;
        String blank = "";
        int uid, mid, total=0;
        int totalUsers=0;
        
        // For each user, make recommendations
        users		 = testmh.getListOfUsers();
        totalUsers   = users.size(); 
                
        //-----------------------
        // All test users
        //-----------------------
        
           for (int i = 0; i < totalUsers; i++)                                
            {
            	uid = users.getQuick(i);       
                movies = testmh.getMoviesSeenByUser(uid);
                double myRating=0.0;                
            	total++;         
           
           // if(movies.size()>=20)
            {
            	//-----------------------
                // Movies seen by a user
                //-----------------------
                
                for (int j = 0; j < movies.size(); j++)     
                {
                  mid = MemHelper.parseUserOrMovie(movies.getQuick(j));   
                  
                  double rrr = recommend  (uid,
                		  				   mid,
                		  				   myNeighbours,
                		  				   comb,
                		  				   alpha
                  						 );
                
                  //-----------------------
                  // Add error
                  //-----------------------
                   
                  //System.out.println("  uid, mid, ratingP, ratingA" + uid + "," + mid + ","+ rrr +","+ myRating);
                  
                 // if(rrr!=0) //challenge sparsity (Active user has not rated any similar movie)
                  
                /*  if(rrr<0) rrr=0;
                    if(rrr>5) rrr=5;
                */  
                
                                    	
                			myRating = testmh.getRating(uid, mid);			 		// get actual ratings?

                            if (myRating==-99 )                           
                               System.out.println(" rating error, uid, mid, ratingP" + uid + "," + mid + ","+ myRating);
                           
                            //Add MAE n ROC
                            if(rrr!=0 && rrr!=-1) {
                            	rmse.add(myRating,rrr);		   							 // get prediction for these users ....from where it is calling it?
                            	rmse.ROC4(myRating, rrr, myClasses,myTrainingSet.getAverageRatingForUser(uid) );
                            	
                            	//Add Pair-t test
                                if(comb==0) //add actual value (i.e base with which we will compare)
                                	rmse.addActualToPairT(rrr);
                                else		//add prediction, i.e. we will compare this with base
                                	rmse.addPredToPairT(rrr);
                              
                            }

                                                  
                            //rmse.addToPairT(rrr, myRating);
                            
                            //Add coverage
                            if(rrr!=-1)
                            	rmse.addCoverage(rrr);
                            
                           /* System.out.println("=====================================================");
                              System.out.println(" error is = (actual - predicted=" + myRating + "-" + rrr);
                              System.out.println("=====================================================");
                           */
                       
                }//end of all movies for
            }//filter >20 movies 
            }//end of all users for
        

       //System.out.println(", total =" + total);
        //  double dd= rmse.rmse();
        double dd= rmse.mae();
      
        return dd;
    }
    
/*****************************************************************************************************/
    
//File writing etc

    public void openFile(int howManyFiles, int which)    
    {     
   	 try {

   		 for(int i=0;i<howManyFiles;i++)
   		 { 
   			if(which==1)  //general
   				writeData[i] = new BufferedWriter(new FileWriter(myPath + "Results/Results" + (i+1)+ ".dat", true));
   			
   			else if (which==2) //demo
   				writeDemoData[i] = new BufferedWriter(new FileWriter(myPath + "Results/ResultD" + (i+1)+ ".dat", true));
   		
   			else if (which==3) //rating
   				writeRatingData[i] = new BufferedWriter(new FileWriter(myPath + "Results/ResultR" + (i+1)+ ".dat", true));
   		
   			else  //demo and rating both
   				writeDemoAndRatingData[i] = new BufferedWriter(new FileWriter(myPath + "Results/ResultDR" + (i+1)+ ".dat", true));
   		    		 
   		 }
   			
   	   }
        
        catch (Exception E)
        {
      	  System.out.println("error opening the file pointer of rec");
      	  //System.exit(1);
        }
        
        System.out.println("Rec File Created");
    }
    
    //----------------------------
    

    public void closeFile(int howMany, int which)    
    {
    
   	 try {
   		    for(int i=0;i<howMany;i++)
   		    	
   		 	    {
   		    	
   		    	if (which==1)						// 3 files; 1= mae, 2= roc, 3= coverage
   		    		writeData[i].close();
   		    	
   		    	else if (which==1)
   		    		writeDemoData[i].close();
   		    	
   		    	else if (which==1)
   		    		writeRatingData[i].close();
   		    	
   		    	else
   		    		writeDemoAndRatingData[i].close();
   		 	    
   		 	    }
   		  }
   	     
        catch (Exception E)
        {
      	  System.out.println("error closing the roc file pointer");
        }
        
    }
    
//--------------------------------------------------------
    
    public void openSingleFile()    
    {
    
   	 try {	 
   		writeWeights = new BufferedWriter(new FileWriter(myPath + "Results/Indv/recSigWeight_-ve_WS_RanNegAndAllZeros_1.dat" , true));

   			
   	   }
        
        catch (Exception E)
        {
      	  System.out.println("error opening the file pointer of rec");
      	  System.exit(1);
        }
        
        System.out.println("Rec File Created");
    }
    
    //------------------------------------
    
    public void closeSingleFile()    
    {
    
   	 try{
   	      writeWeights.close();   		 	    
   	  }
   	 
      catch (Exception E){
          System.out.println("error closing the roc file pointer");
        }        
    }
    
    //--------------------------------------
    
    /**
     * Write data into a file, mae, rmse, roc, coverage 
     */
    
    public void writeIntoFile(int tab, boolean newLine, int neighbours, int alpha)
    {  
    	try {
    			if(tab==0)  {  
	    			writeData[0].write(neighbours + " " +
	    					   			alpha      + " ");
	    			writeData[1].write(neighbours + " " +
				   						alpha      + " ");
	    			writeData[2].write(neighbours + " " +
							   			alpha      + " ");
	    			writeData[3].write(neighbours + " " +
	    					   			alpha      + " ");
    			}
    			
    			writeData[0].write(nf.format(rmse.mae()) + " ");
    			writeData[1].write(nf.format(rmse.rmse()) + " ");
    			writeData[2].write(nf.format(rmse.getSensitivity()) + " ");
    			writeData[3].write(nf.format(rmse.getItemCoverage()) + " ");
    			
    			if(newLine == true) {
    				writeData[0].newLine();
    				writeData[1].newLine();
    				writeData[2].newLine();
    				writeData[3].newLine();
    			}
    	}
    	
    	catch(Exception E) {
    		E.printStackTrace();
    		System.out.println("Error handling file");
    	}
   }
    
}//end of class
//---------------------------------------------------------------------
//Item based CF
// 1- with all weights, (as mostly sim<0), all version except mine went down to inferior
//2- weight +1, still we can calaim that with more neighbours, their oerformance is degrading.
		
