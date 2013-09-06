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

/************************************************************************************************************************/
public class ItemItemRecommenderWithK //extends AbstractRecommender
/************************************************************************************************************************/
{

    private DatabaseImpl 	db;
    private boolean 		method; 			//true for weighted sums, false for linear regression
    BufferedWriter      	writeData[];		//for writing in file
    BufferedWriter      	writeDemoData[];	//for writing in file
    BufferedWriter      	writeRatingData[];	//for writing in file
    BufferedWriter      	writeDemoAndRatingData[];	//for writing in file
    BufferedWriter      	writeWeights;	//for weight writing in file
       
    private String      	myPath;		
 
    private MemHelper       myTrainingSet;  
    private UserSimKeeper   myStoredRSim;   //Rsim
    private UserSimKeeper   myStoredDSim;	//DSim
    private UserSimKeeper   myStoredFSim;	//FSim
    private int             totalK;
    private int 			incrementInK;  
    private int             totalNegativeAnswers,totalNegativeAnswers1, totalNegativeAnswers2,totalNegativeAnswers3;
    private int 			totalZeroAnswers, totalZeroAnswers1, totalZeroAnswers2, totalZeroAnswers3;
    private int 			howMuchNeighboursReallyFound =0;
    
    String 	infoAbtComb;		//contain information abt the combination we have
    
    RMSECalculator rmse;
    
/************************************************************************************************************************/

    //constructor sets up the database-access layer and determines which method will
    //be used to make recommendations (weighted sums or linear regression)
    
    public ItemItemRecommenderWithK(String dbName, String rateName, 
    								String movieName, String userName,
    								String simName, boolean weighted)    
    {
    //    db = new DatabaseImpl(dbName, rateName, movieName, userName, simName);    
         this.method = weighted;
        

         //SML Data paths
         // myPath ="C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Item based\\FiveFoldData\\DataFD\\";
          myPath ="C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Item based\\";
          myStoredRSim = UserSimKeeper.deserialize(myPath + "sml_sim_C.dat" );
          myTrainingSet = new MemHelper (myPath + "sml_clusteringTrainSetStoredTF.dat");         
        
         //FT
     /*     myPath ="C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\FT\\Item based\\";
          myStoredRSim = UserSimKeeper.deserialize(myPath + "ft_sim_C10.dat" );
          myTrainingSet = new MemHelper (myPath + "ft_clusteringTrainSetStoredTF10.dat");   
         */

        //ML data paths
       // myPath ="C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\ItemBased\\FiveFoldData\\Data1\\";
       // myPath ="C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\ItemBased\\FiveFoldData\\DataD\\";
        
        writeData 			     = new BufferedWriter[10];		
        writeDemoData 			 = new BufferedWriter[10];		
        writeRatingData 		 = new BufferedWriter[10];		
        writeDemoAndRatingData 	 = new BufferedWriter[10];		
        
        totalK 				 = 60;
        incrementInK	 	 = 10;
        totalNegativeAnswers = totalNegativeAnswers1 = totalNegativeAnswers2 = totalNegativeAnswers3 =0; 							 // -ve corr cases
        totalZeroAnswers =totalZeroAnswers1 = totalZeroAnswers2 = totalZeroAnswers3 = 0;     						 //active user has not rated any of the similar movies
        infoAbtComb ="";
        howMuchNeighboursReallyFound = 0;
        
        
    }
    
/************************************************************************************************************************/
 
    /**
     * @author steinbel
     * Implements the abstract method AbstractRecommender using whichever method was
     * set by constructor.  (Discards the date information.)
     * 
     * @param uid - the user to predict the rating for
     * @param mid - the movie the predict against
     * @param date - the date the rating would be made (irrelevant)
     * @return	the predicted rating for this user on this movie.
     */

    public double recommend(int uid, int mid, String date, int totalUsers) //called from above    
    {
    	if (method)
    		return weightedSum(mid, uid,totalUsers);		// weighted sum
    	else
    		return regression(mid, uid);					// linear regression
    		
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
    
    //So why not just print the results for K, and we can use or return the one 
    //which gave the lowest error?
    
    private double weightedSumForIndividual(int movieID, int userID)    
    {    	
        double sumTop			= 0;
        double sumBottom		= 0;
        double answer       	= 0;
        int    K				= 5;    // increment in neighbours
        int    upperLimitOnK 	= 150;  //check from 2-200 neighbours
        DoubleArrayList  errors = new DoubleArrayList();
        
        // grab all similar movies and their similarities
      //   ArrayList<IntDoublePair> idSimList = db.getSimilarMovies(movieID, true);   //from smilarity table
      //   ArrayList<IntDoublePair> idSimList = myStoredSim.getSimilarMovies(movieID);  //from stored and deserialized object
           OpenIntDoubleHashMap idSimMap  = myStoredRSim.getTopSimilarMovies(movieID);	 
        
        //grab the ratings for all the similar movies the user has seen
        double temp;
        int dummy=0;
    	//_______________________
        
        // Formula: from item-based CF
        // All sim movies (similarity * active user rating) / sum over all sim movies (similarity)
       
       // if(idSimList == null) System.out.println(" with null"); 
        if(idSimMap!= null)
        {
        	IntArrayList myMovies = idSimMap.keys();
            DoubleArrayList myWeights = idSimMap.values();
            int totalSize = idSimMap.size();
            //_________________________________
	       
          for (int mainLoop =0; mainLoop <upperLimitOnK; mainLoop+=K) //for different sets of K each time
           {
        	 for (int i=totalSize-1, pointer=0; i>0; i--, pointer++)  //for how many neighbours to need each time
	           {	
        		if(pointer == mainLoop) break;         			
        		
	        	temp = myTrainingSet.getRating(userID, myMovies.get(i));
	            dummy++;
	                                                                                                                                                                                                                                                                        
	            //if the user hasn't rated this one, skip it
	            if (temp!=-99)
	            {
	            	//calculate the weighted sums
	            	/*sumTop += (temp * myWeights.get(i));			// Active user vote * sim factor (weight)
	            	  sumBottom += Math.abs(myWeights.get(i));		// K
	            	*/
	            	
	            	  //It is Tatse approach
	            	  
	            	  sumTop += (temp * (myWeights.get(i) + 1));			// Active user vote * sim factor (weight)
	            	  sumBottom +=(myWeights.get(i) + 1);		// K
	            	
	            }
	         } //end of for, 
           }//end of external for, for K
        }//end of null condition check
        
        //if user didn't see any similar movies give avg rating for user
        if (sumBottom == 0)
        	//return db.getAverageRatingForUser(userID);
        	return  myTrainingSet.getAverageRatingForUser(userID);
        
        answer = sumTop/sumBottom; 
         if (answer<0) return myTrainingSet.getAverageRatingForUser(userID);
         else return answer;
    }
 
 //------------------------------------------------------------------------
    
    private double weightedSum(int movieID, int userID, int NumberOfNeighbours)    
    {    	
        double sumTop			= 0;
        double sumBottom		= 0;
        double answer       	= 0;
        int    K				= 5;    // increment in neighbours
        int    upperLimitOnK 	= 200;  // check from 2-200 neighbours
                
       	 
        ArrayList<IntDoublePair> idSimList = myStoredRSim.getSimilarMovies(movieID);    //it should be sorted as it is        
        double temp;
        int dummy=0;
    	        
        // Formula: from item-based CF
        // All sim movies (similarity * active user rating) / sum over all sim movies (similarity)
       
        if(idSimList!= null) // //grab the ratings for all the similar movies the user has seen
        {
           int totalSize = idSimList.size();
            
            
        //---------------------------------------------------------------------------
      	// Loop: number of neighbours
      	//---------------------------------------------------------------------------
      	        	  
         for (int i=0, pointer=0; i<totalSize; i++)  //for how many neighbours to need each time
	       {	
        		if(pointer == NumberOfNeighbours) break;  
            
          		IntDoublePair pair = (IntDoublePair)idSimList.get(i);
        		dummy++;
        		temp = myTrainingSet.getRating(userID, pair.a);
        		
	            //if the user hasn't rated this one, skip it
	            if (temp!=-99)
	            {
	             	
	            	//calculate the weighted sums
	            	//sumTop += (temp * (pair.b));			// Active user vote * sim factor (weight)
	            	//sumBottom += Math.abs(pair.b);		// K
	            	//System.out.println(" weights -->" + myWeights.get(i));
	            	//System.out.println(" weights -->" + pair.b);
	            	
	            	  //It is Tatse approach	            	  
	             /*		sumTop += (temp * (pair.b + 1));
	             		sumBottom +=(pair.b + 1);
	             		howMuchNeighboursReallyFound++;
	             		pointer++;		
	                  */
	            	  
	             	//simple take all weights
	            /*	 	sumTop += (temp * (pair.b));
	            	  	sumBottom +=  Math.abs(pair.b);
	            	  	pointer++;
	            	  	howMuchNeighboursReallyFound++;
	            	 
	            	  	 */
		             	/*//simple take all weights
		            	 	sumTop += (temp * (pair.b));
		            	  	sumBottom +=  (pair.b);
		            	  	pointer++;
		            	  	howMuchNeighboursReallyFound++;
		            	  */
	            	
	            	 //simple takes only +ve
	            	  if(pair.b>0)
	            	  {
		            	  sumTop += (temp * (pair.b));
		            	  sumBottom +=  Math.abs(pair.b);
		            	  pointer++;
		            	  howMuchNeighboursReallyFound++;
	            	  }
	            
	             	
	            }
	         } //end of for, 
           }//end of null condition check
    	
        
        //System.out.println(" -------Active User rated no similar Movie--------");
    	
        //if user didn't see any similar movies give avg rating for user
        if (sumBottom == 0)
        	//return db.getAverageRatingForUser(userID);
        	{
        	    totalZeroAnswers1++;
        	//	System.out.println(" bottonSum=0 -->" + sumBottom);
        	  	return  myTrainingSet.getAverageRatingForUser(userID);        	
            //   return  0; 		//sparsity challenge (active user have not rated any similar movie)
        	}
        
        answer = sumTop/sumBottom;
        
         if (answer<0) 
        	 {
        	     totalNegativeAnswers1++;
        	 	// System.out.println(" answer<0 -->" + answer);
        	 	   return myTrainingSet.getAverageRatingForUser(userID);
        	    // return 0;
        	 
        	 }
         
         else return answer;
    }
 
 /***************************************************************************************************
 /***************************************************************************************************
    //----------------------------------------------------------
    // Linear Combination of Rating, Demo, and Feature Correlation
    //----------------------------------------------------------
***************************************************************************************************
**************************************************************************************************/    
    /**
     * @author  Musi
     * @param   mid, uid, and no. of neighbours to be considered
     * @return  the weighted prediction of the movie 
     */
    
    private double weightedSumHybrid(
							    		int movieID, 
							    		int userID, 
							    		int NumberOfNeighbours, 
							    		int combination,
									    double alpha,				//coff for determining the best MAE
									    double beta,
									    double gamma)
		    
    {    	        
        OpenIntDoubleHashMap   activeIndependentRatingSim      = new OpenIntDoubleHashMap(); 			//It will store active items' rating correlation weights
        OpenIntDoubleHashMap   activeDependentRatingSim        = new OpenIntDoubleHashMap(); 			//It will store active items' rating correlation weights
        OpenIntDoubleHashMap   activeFDependentRatingSim       = new OpenIntDoubleHashMap(); 			//It will store active items' rating correlation weights
        
        OpenIntDoubleHashMap   activeIndependentDemoSim   	   = new OpenIntDoubleHashMap(); 			//It will store active items' demo ...
        OpenIntDoubleHashMap   activeDoubleDependentDemoSim    = new OpenIntDoubleHashMap(); 			//It will store active items' demo ...
        OpenIntDoubleHashMap   activeDependentDemoSim   	   = new OpenIntDoubleHashMap(); 			//It will store active items' demo ...
        OpenIntDoubleHashMap   activeFDependentDemoSim   	   = new OpenIntDoubleHashMap(); 			//It will store active items' demo ...
        
        OpenIntDoubleHashMap   activeIndependentFeatureSim     = new OpenIntDoubleHashMap(); 			//It will store active items' demo ...
        OpenIntDoubleHashMap   activeDependentFeatureSim   	   = new OpenIntDoubleHashMap(); 			//It will store active items' demo ...
        OpenIntDoubleHashMap   activeFDependentFeatureSim   	   = new OpenIntDoubleHashMap(); 			//It will store active items' demo ...

        OpenIntDoubleHashMap   activeUserMovieAndRatingList_R  = new OpenIntDoubleHashMap(); 	  //active user saw this movie (first rating, find neighbours and then demo)
        OpenIntDoubleHashMap   activeUserMovieAndRatingList_D  = new OpenIntDoubleHashMap(); 	  //active user saw this movie (first demo, find neighbours and then rating)
        OpenIntDoubleHashMap   activeUserMovieAndRatingList_F  = new OpenIntDoubleHashMap(); 	  //active user saw this movie (first demo, find neighbours and then rating)
        OpenIntDoubleHashMap   activeUser_AllRelevant_MovieAndRatingList  = new OpenIntDoubleHashMap(); 	  //active user saw this movie (first demo, find neighbours and then rating)
                
        ArrayList<IntDoublePair> idSimRList = myStoredRSim.getSimilarMovies(movieID);       //it should be sorted as it is        
        ArrayList<IntDoublePair> idSimDList = myStoredDSim.getSimilarMovies(movieID);       //it should be sorted as it is
        ArrayList<IntDoublePair> idSimFList = myStoredFSim.getSimilarMovies(movieID);       //it should be sorted as it is
        
        double temp;
        int dummy=0;       
         
        //Movies seen by active user
        LongArrayList moviesSeenByActiveUser = myTrainingSet.getMoviesSeenByUser(userID);
        OpenIntDoubleHashMap  moviesAndRatingsOfActiveUser  = new OpenIntDoubleHashMap(); 	
        LongArrayList allMoviesSeenByActiveUser = new LongArrayList();   
        
        int activeUserMovSize = moviesSeenByActiveUser.size();
        for (int i=0;i<activeUserMovSize;i++)
        {
        	int mid = MemHelper.parseUserOrMovie(moviesSeenByActiveUser.getQuick(i));
        	allMoviesSeenByActiveUser.add(mid);
        	
        	double r= myTrainingSet.getRating(userID, mid);
        	moviesAndRatingsOfActiveUser.put(mid, r);    	
        	
        }
        
       //-----------------------------------------------------------------------------------------
       //-----------------------------------------------------------------------------------------
       // Rating similarity (Independent)
       // Formula: from item-based CF
       // All sim movies (similarity * active user rating) / sum over all sim movies (similarity)       
       //-----------------------------------------------------------------------------------------
       //-----------------------------------------------------------------------------------------
      	
        
      // if(idSimList == null) System.out.println(" with null"); 
        if(idSimRList!= null) // //grab the ratings for all the similar movies the user has seen
        {
        	  int totalSize = idSimRList.size();
        	  
            
            
        //---------------------------------------------------------------------------
      	// Loop: number of neighbours
      	//---------------------------------------------------------------------------
      	
        	  
         for (int i=0, pointer=0; i<totalSize; i++)  //for how many neighbours to need each time
	       {	
        		if(pointer == NumberOfNeighbours) break;        			
	        
        	 	//-----------------------
            	// Get similar items	 
             	//-----------------------           
        		//get similar items for a movie
        		
        		IntDoublePair pair = (IntDoublePair)idSimRList.get(i);  
        		dummy++;       		
        	
        		//--------------------------------------------------
        	 	// Save the weighted of all sim items
        		//--------------------------------------------------
        		
	            //if the user hasn't rated this one, skip it
        		if(allMoviesSeenByActiveUser.contains(pair.a))
	            {
        			temp = myTrainingSet.getRating(userID, pair.a);
	            	if (temp==0)	System.out.println("rating is zero (uid, mid, Rat)  =" + userID +"," + pair.a + "," + temp);
	        		
	            	//System.out.println("rating is Not zero  =" + temp);
	            	
	            	if (temp>5 || temp<0)	System.out.println("rating is zero (uid, mid, Rat)  =" + userID +"," + pair.a + "," + temp);
	            	
	            	activeUserMovieAndRatingList_R.put(pair.a, temp);             // movie seen by active user, and its rating	            
	            	activeUser_AllRelevant_MovieAndRatingList.put(pair.a, temp);  //keep track of all movies
	            	pointer++;	            	
	                	
	             	//-----------------------
	            	// Cosider All Weights	 
	             	//-----------------------
	             	
	             	activeIndependentRatingSim.put(pair.a, pair.b );
	             	
	             	//-----------------------
	            	// Tatse approach	 
	             	//-----------------------
	             	
	            
	            	//activeIndependentRatingSim.put(pair.a, pair.b +1);
	             	   
	             	//-----------------------
	            	// Take only +ve weights	 
	             	//-----------------------
	           /*  	
		             	if( pair.b >0)
		             	{
		             		activeIndependentRatingSim.put(pair.a, pair.b );
		             	}
		       */      	
	             	
	   
	            } //end of if active user has rated a similar movie
	         } //end of for, 
           } //end of null condition check
    	
      //  System.out.println(" Expected Answer ="+ d1/d2);
      //  return d1/d2;
        
   
        
       //-----------------------------------------------------------------------------------------
       //-----------------------------------------------------------------------------------------
       // Demo similarity (independent)
       // Formula: from item-based CF
       // All sim movies (similarity * active user rating) / sum over all sim movies (similarity)       
       //-----------------------------------------------------------------------------------------
       //-----------------------------------------------------------------------------------------
                         
        if(idSimDList!= null) // //grab the ratings for all the similar movies the user has seen
        {
        	  int totalSize = idSimDList.size();            
            
        //---------------------------------------------------------------------------
      	// Loop: number of neighbours
      	//---------------------------------------------------------------------------
      	        	  
         for (int i=0, pointer=0; i<totalSize; i++)  //for how many neighbours to need each time
	      {	
    	    	if ( pointer == NumberOfNeighbours) 
        			break;        			
	        
        	 	//-----------------------
            	// Get similar items	 
             	//-----------------------           
        		//get similar items for a movie
        		
        		IntDoublePair pair = (IntDoublePair)idSimDList.get(i);  
        		dummy++;
        		        		
        		//--------------------------------------------------
        	 	// Calculate the weighted sums of all sim items
        		//--------------------------------------------------

        		if(allMoviesSeenByActiveUser.contains(pair.a))
        		{
        			temp = myTrainingSet.getRating(userID, pair.a);
        			
        			//consider all weights
        			activeUserMovieAndRatingList_D.put(pair.a, temp);
        			activeIndependentDemoSim.put(pair.a, pair.b);
        			
        			//only +ve
        		/*	if(pair.b>0)
        			{
	        			activeUserMovieAndRatingList_D.put(pair.a, temp);
	        			activeIndependentDemoSim.put(pair.a, pair.b);
        			}
        		*/
        			//taste approach
        		/*	activeUserMovieAndRatingList_D.put(pair.a, temp);
        			activeIndependentDemoSim.put(pair.a, pair.b +1);
        */
        			
        			//System.out.println(" weights -->" + pair.b);
        			pointer++;
        			
        			// Adding all movies in a hashtable?
        			//It is not gonna work (As both have different simi ... means same mid in demo and in rating
        			//similarity can have diff similarity (And this is the thing we want)
        			
        			
        		} //end if active user rated this
	       } //end of neighbour loop
        } //end of if list is not null
            

      //-----------------------------------------------------------------------------------------
      //-----------------------------------------------------------------------------------------
      // Feature similarity (independent)
      // Formula: from item-based CF
      // All sim movies (similarity * active user rating) / sum over all sim movies (similarity)       
      //-----------------------------------------------------------------------------------------
      //-----------------------------------------------------------------------------------------
        
         if(idSimFList!= null)  //grab the ratings for all the similar movies the user has seen
         {
         	  int totalSize = idSimFList.size();            
             
         //---------------------------------------------------------------------------
       	// Loop: number of neighbours
       	//---------------------------------------------------------------------------
       	        	  
          for (int i=0, pointer=0; i<totalSize; i++)  //for how many neighbours to need each time
 	      {	
     	    	if ( pointer == NumberOfNeighbours ) 
         			break;        			
 	        
         	 	//-----------------------
             	// Get similar items	 
              	//-----------------------           
         		//get similar items for a movie
         		
         		IntDoublePair pair = (IntDoublePair)idSimFList.get(i);  
         		  		
         		//--------------------------------------------------
         	 	// Calculate the weighted sums of all sim items
         		//--------------------------------------------------

         		if(allMoviesSeenByActiveUser.contains(pair.a))
         		{
         		
         			temp = myTrainingSet.getRating(userID, pair.a);
         			
         			//consider all weights
         			//if(pair.b ==0 || pair.b >0 || pair.b<0) //to avoid NAN
         			{
	         			activeUserMovieAndRatingList_F.put(pair.a, temp);
	         			activeIndependentFeatureSim.put(pair.a, pair.b);
         			}
         			
         		//	System.out.println(" weights -->" + pair.b);
         			
         			//only +ve
        /* 			if(pair.b>0)
         			{
 	        			activeUserMovieAndRatingList_F.put(pair.a, temp);
 	        			activeIndependentFeatureSim.put(pair.a, pair.b);
         			}
        */ 		
         			//taste approach
         			/*activeUserMovieAndRatingList_F.put(pair.a, temp);
         			activeIndependentFeatureSim.put(pair.a, pair.b +1);
         */
         			
         			
         			pointer++;
         			
         			// Adding all movies in a hashtable?
         			//It is not gonna work (As both have different simi ... means same mid in demo and in rating
         			//similarity can have diff similarity (And this is the thing we want)
         			
         			
         		} //end if active user rated this
 	       } //end of neighbour loop
         } //end of if list is not null
             

         //-----------------------------------------------------------------------------------------
         //-----------------------------------------------------------------------------------------
         // Rating similarity (FDependent)
         // We will only take into account those movies that are relevant from Feature sim and
         // then we will refine them using rating corr
         //-----------------------------------------------------------------------------------------
         //-----------------------------------------------------------------------------------------
        	
          
         // if(idSimList == null) System.out.println(" with null"); 
          if(idSimRList!= null && idSimFList!= null) // //grab the ratings for all the similar movies the user has seen
          {
          	  int totalSize = idSimRList.size();
              
              
            //---------------------------------------------------------------------------
        	// Loop: number of neighbours
        	//---------------------------------------------------------------------------
        	         	  
           for (int i=0, pointer=0; i<totalSize; i++)  
  	      {	
          		if(pointer == NumberOfNeighbours) break;        			
  	        
          	 	//-----------------------
              	// Get similar items	 
               	//-----------------------           
          		//get similar items for a movie
          		
          		IntDoublePair pair = (IntDoublePair)idSimRList.get(i);  
          		dummy++;
          		         		
          		//--------------------------------------------------
          	 	// Save the weighted of all sim items
          		//--------------------------------------------------
          		
  	            //if this movie is selected as neighbour in demo corr
  	            if (activeUserMovieAndRatingList_F.containsKey(pair.a) )
  	            {
  	            		            
  	             	pointer++;	            	
  	                	
  	             	//-----------------------
  	            	// Cosider All Weights	 
  	             	//-----------------------
  	             	
  	             	activeFDependentRatingSim.put(pair.a, pair.b );
  	             	
  	             	//-----------------------
  	            	// Tatse approach	 
  	             	//-----------------------
  	             	
  	     //        	activeDependentRatingSim.put(pair.a, pair.b +1);
  	             	   
  	             	//-----------------------
  	            	// Take only +ve weights	 
  	             	//-----------------------
 /* 	             	
  		             	if( pair.b >0)
  		             	{
  		             		activeDependentRatingSim.put(pair.a, pair.b );
  		            	}
 */ 		             	
  	
  	             	   
  	            } //end of if active user has rated a similar movie
  	         } //end of for, 
             }//end of null condition check
      	
          
          
          
        //-----------------------------------------------------------------------------------------
        //-----------------------------------------------------------------------------------------
        // Rating similarity (Dependent)
        // We will only take into account those movies that are relevant from demo sim and
        // then we will refine them using rating corr
        //-----------------------------------------------------------------------------------------
        //-----------------------------------------------------------------------------------------
       	
         
        // if(idSimList == null) System.out.println(" with null"); 
         if(idSimRList!= null && idSimDList!= null) // //grab the ratings for all the similar movies the user has seen
         {
         	  int totalSize = idSimRList.size();
             
             
         //---------------------------------------------------------------------------
       	// Loop: number of neighbours
       	//---------------------------------------------------------------------------
       	         	  
          for (int i=0, pointer=0; i<totalSize; i++)  
 	      {	
         		if(pointer == NumberOfNeighbours) break;        			
 	        
         	 	//-----------------------
             	// Get similar items	 
              	//-----------------------           
         		//get similar items for a movie
         		
         		IntDoublePair pair = (IntDoublePair)idSimRList.get(i);  
         		dummy++;
         		         		
         		//--------------------------------------------------
         	 	// Save the weighted of all sim items
         		//--------------------------------------------------
         		
 	            //if this movie is selected as neighbour in demo corr
 	            if (activeUserMovieAndRatingList_D.containsKey(pair.a) )
 	            {
 	            		            
 	             	pointer++;	            	
 	                	
 	             	//-----------------------
 	            	// Cosider All Weights	 
 	             	//-----------------------
 	             	
 	             	activeDependentRatingSim.put(pair.a, pair.b );
 	             	
 	             	//-----------------------
 	            	// Tatse approach	 
 	             	//-----------------------
 	             	
 	     //        	activeDependentRatingSim.put(pair.a, pair.b +1);
 	             	   
 	             	//-----------------------
 	            	// Take only +ve weights	 
 	             	//-----------------------
/* 	             	
 		             	if( pair.b >0)
 		             	{
 		             		activeDependentRatingSim.put(pair.a, pair.b );
 		            	}
*/ 		             	
 	
 	             	   
 	            } //end of if active user has rated a similar movie
 	         } //end of for, 
            }//end of null condition check
     	
         
         //-----------------------------------------------------------------------------------------
         //-----------------------------------------------------------------------------------------
         // Demo similarity(FDependent) (measured after selection of neighbours from Feature
         // similarity)             
         //-----------------------------------------------------------------------------------------
         //-----------------------------------------------------------------------------------------
        
             
        if(idSimDList!= null && idSimFList!= null ) // //grab the ratings for all the similar movies the user has seen
        {
        	  int totalSize = idSimDList.size();            
            
        //---------------------------------------------------------------------------
      	// Loop: number of neighbours
      	//---------------------------------------------------------------------------
      	        	  
         for (int i=0, pointer=0; i<totalSize; i++)  //for how many neighbours to need each time
 	       {	
        		if ( pointer == NumberOfNeighbours ) 
        			
        		
        			break;        			
 	        
        	 	//-----------------------
            	// Get similar items	 
             	//-----------------------           
        		//get similar items for a movie
        		
        		IntDoublePair pair = (IntDoublePair)idSimDList.get(i);  
        		dummy++;
        		       		
        		//--------------------------------------------------
        	 	// Calculate the weighted sums of all sim items
        		//--------------------------------------------------

        		if(activeUserMovieAndRatingList_F.containsKey(pair.a) ) //if active user saw this movie && weight>0
        		{
        			//all weights
        			 activeFDependentDemoSim.put(pair.a, pair.b);
        			
        		   //only +ve
 /*       			if (pair.b>0)
        			activeDependentDemoSim.put(pair.a, pair.b);
 */       			
        			//taste approach
        		//	activeDependentDemoSim.put(pair.a, pair.b+1);
        			
        			
        			//System.out.println(" weights -->" + pair.b);
        			pointer++;
        			
        		} //end if active user rated this
 	       } //end of neighbour loop
        } //end of if list is not null
        

        
         
        //-----------------------------------------------------------------------------------------
        //-----------------------------------------------------------------------------------------
        // Demo similarity(Dependent) (measured after selection of neighbours from rating similarity)             
        //-----------------------------------------------------------------------------------------
        //-----------------------------------------------------------------------------------------
       
            
       if(idSimDList!= null && idSimRList!= null ) // //grab the ratings for all the similar movies the user has seen
       {
       	  int totalSize = idSimDList.size();            
           
       //---------------------------------------------------------------------------
     	// Loop: number of neighbours
     	//---------------------------------------------------------------------------
     	        	  
        for (int i=0, pointer=0; i<totalSize; i++)  //for how many neighbours to need each time
	       {	
       		if ( pointer == NumberOfNeighbours ) 
       			
       		
       			break;        			
	        
       	 	//-----------------------
           	// Get similar items	 
            	//-----------------------           
       		//get similar items for a movie
       		
       		IntDoublePair pair = (IntDoublePair)idSimDList.get(i);  
       		dummy++;
       		       		
       		//--------------------------------------------------
       	 	// Calculate the weighted sums of all sim items
       		//--------------------------------------------------

       		if(activeUserMovieAndRatingList_R.containsKey(pair.a) ) //if active user saw this movie && weight>0
       		{
       			//all weights
       			 activeDependentDemoSim.put(pair.a, pair.b);
       			
       		   //only +ve
/*       			if (pair.b>0)
       			activeDependentDemoSim.put(pair.a, pair.b);
*/       			
       			//taste approach
       		//	activeDependentDemoSim.put(pair.a, pair.b+1);
       			
       			
       			//System.out.println(" weights -->" + pair.b);
       			pointer++;
       			
       		} //end if active user rated this
	       } //end of neighbour loop
       } //end of if list is not null
       


       //-----------------------------------------------------------------------------------------
       //-----------------------------------------------------------------------------------------
       // Feature similarity(Dependent) (measured after selection of neighbours from rating similarity)             
       //-----------------------------------------------------------------------------------------
       //-----------------------------------------------------------------------------------------
      
           
      if(idSimFList!= null && idSimRList!= null ) // //grab the ratings for all the similar movies the user has seen
      {
      	  int totalSize = idSimFList.size();            
          
      //---------------------------------------------------------------------------
    	// Loop: number of neighbours
    	//---------------------------------------------------------------------------
    	        	  
       for (int i=0, pointer=0; i<totalSize; i++)  //for how many neighbours to need each time
	       {	
      		if ( pointer == NumberOfNeighbours )     		
      			break;        			
	        
      	 	//-----------------------
          	// Get similar items	 
           	//-----------------------           
      		//get similar items for a movie
      		
      		IntDoublePair pair = (IntDoublePair)idSimFList.get(i);  
      		dummy++;
      		       		
      		//--------------------------------------------------
      	 	// Calculate the weighted sums of all sim items
      		//--------------------------------------------------

      		if(activeUserMovieAndRatingList_R.containsKey(pair.a) ) //if active user saw this movie && weight>0
      		{
      			//all weights
      			activeDependentFeatureSim.put(pair.a, pair.b);
      			
      		   //only +ve
      			/*if (pair.b>0)
      			activeDependentFeatureSim.put(pair.a, pair.b);
      			*/
      			
      			//taste approach
      			//  activeDependentFeatureSim.put(pair.a, pair.b+1);
      			
      			
      			//System.out.println(" weights -->" + pair.b);
      			pointer++;
      			
      		} //end if active user rated this
	       } //end of neighbour loop
      } //end of if list is not null

      //-----------------------------------------------------------------------------------------
      //-----------------------------------------------------------------------------------------
      // Demo similarity(Double-Dependent) (measured after selection of neighbours from rating 
      //  & feature similarity)             
      //-----------------------------------------------------------------------------------------
      //-----------------------------------------------------------------------------------------
     
          
     if(idSimDList!= null && idSimRList!= null && idSimFList!=null ) // //grab the ratings for all the similar movies the user has seen
     {
     	  int totalSize = idSimDList.size();            
         
     //---------------------------------------------------------------------------
   	// Loop: number of neighbours
   	//---------------------------------------------------------------------------
   	        	  
      for (int i=0, pointer=0; i<totalSize; i++)  //for how many neighbours to need each time
	       {	
     		if ( pointer == NumberOfNeighbours ) 
     			
     		
     			break;        			
	        
     	 	//-----------------------
         	// Get similar items	 
          	//-----------------------           
     		//get similar items for a movie
     		
     		IntDoublePair pair = (IntDoublePair)idSimDList.get(i);  
     		dummy++;
     		       		
     		//--------------------------------------------------
     	 	// Calculate the weighted sums of all sim items
     		//--------------------------------------------------

     		if(activeUserMovieAndRatingList_R.containsKey(pair.a)  &&
     		   activeUserMovieAndRatingList_F.containsKey(pair.a) ) //if active user saw this movie && weight>0    		 
     		{
     			//all weights
     			activeDependentDemoSim.put(pair.a, pair.b);
     			
     		   //only +ve
/*       			if (pair.b>0)
     			activeDependentDemoSim.put(pair.a, pair.b);
*/       			
     			//taste approach
     		//	activeDoubleDependentDemoSim.put(pair.a, pair.b+1);
     			
     			
     			//System.out.println(" weights -->" + pair.b);
     			pointer++;
     			
     		} //end if active user rated this
	       } //end of neighbour loop
     } //end of if list is not null
     

       
 /********************************************************************************************************		
     //-----------------------------------------------------------------------------------------
     //-----------------------------------------------------------------------------------------
     // Ratings, Demo, and Feature similarities are there 
     // Now combine them using a linear weighted average scheme
     // 
     // Formula: All sim movies (similarity * active user rating) / sum over all sim movies (similarity)       
     //-----------------------------------------------------------------------------------------
     //-----------------------------------------------------------------------------------------        
 *******************************************************************************************************/
    
        double rIndependentLinear 		= 1.0, 		rIndependentWeight    	= 1.0;
        double rDependentLinear   		= 1.0, 		rDependentWeight      	= 1.0;		//dependent on demo
        double rFDependentLinear   		= 1.0, 		rFDependentWeight      	= 1.0;		// dependent on feature
        double dIndependentLinear 		= 1.0, 		dIndependentWeight    	= 1.0;        
        double dDependentLinear	  		= 1.0, 		dDependentWeight       	= 1.0;      // on demo
        double dFDependentLinear	  	= 1.0, 		dFDependentWeight      	= 1.0;  	// on feature
        double dDoubleDependentLinear 	= 1.0,		dDoubleDependentWeight 	= 1.0;       
        double fIndependentLinear 		= 1.0, 		fIndependentWeight 		= 1.0;
        double fDependentLinear 		= 1.0, 		fDependentWeight 		= 1.0;		// on demo
        double fFDependentLinear 		= 1.0, 		fFDependentWeight 		= 1.0;		// on feature

        
        double combinedWeight[]  = new double[25];  
        double sumTop[]          = new double[25]; 	
        double sumBottom[] 		 = new double[25]; 	
        double answer[]	         = new double[25];
        double divisionSim[]     = new double[25];	
        
        for(int i=0;i<12;i++)
        	combinedWeight[i] = sumTop[i] = sumBottom[i] = answer[i]=divisionSim[i] =0.0;
        
        int    mid =0;
                
        IntArrayList  movieKeys  		= activeUser_AllRelevant_MovieAndRatingList.keys();  //relevant movies found in rating corr (also active user has rated them)
        IntArrayList  movieKeys_R  		= activeUserMovieAndRatingList_R.keys();  //relevant movies found in rating corr (also active user has rated them)
        IntArrayList  movieKeys_D  		= activeUserMovieAndRatingList_D.keys();  //relevant movies found in rating corr (also active user has rated them)
        IntArrayList  movieKeys_F  		= activeUserMovieAndRatingList_F.keys();  //relevant movies found in rating corr (also active user has rated them)

       
        int 		  totalMoviesSize_R	= movieKeys_R.size();
        int 		  totalMoviesSize_D	= movieKeys_D.size();
        int 		  totalMoviesSize_F	= movieKeys_F.size();
        
        int totalMovies = allMoviesSeenByActiveUser.size();   
        	
	          
	          
	          //--------------------------------------------------------------------------
	          // combination = 1 -->Demo(I)
	          // Take demo sim movies
  	          //--------------------------------------------------------------------------
	           
	          if(combination==1) 
	           {
	        	  infoAbtComb = "Demo(I)";
	        	  
	        	   for (int m=0;m<totalMovies;m++) 
	               {	              
		               	 mid = (int) allMoviesSeenByActiveUser.get(m);
		               	
		             //Get weight demo (I)
		    	     if(activeIndependentDemoSim.containsKey(mid))
		    	      {	  
		    	  	         dIndependentWeight= activeIndependentDemoSim.get(mid);		//get value (sim) against the key (mid)	        	  
		    	 	    	        	
		    	    	   
				              combinedWeight[combination-1] = (dIndependentLinear * dIndependentWeight); 	  //demo (independent)
				              divisionSim[combination-1] = Math.abs( dIndependentWeight);
			
				            //  if(combinedWeight[combination-1]>0)
					           {
						    	   sumTop[combination-1] +=  moviesAndRatingsOfActiveUser.get(mid) * combinedWeight[combination-1];
						    	   sumBottom[combination-1] +=(divisionSim[combination-1]);
					           }
			          }
	              }//end of for
	            }
	          

	          //--------------------------------------------------------------------------
	          // combination = 2 --> Rating(I)
	          // Take Rating sim movies
  	          //--------------------------------------------------------------------------
	           

	          else if(combination==2) 
	           {
	        	  infoAbtComb = "Rating(I)";
	        	  
	       	   for (int m=0;m<totalMovies;m++) 
               {	              
               	
	       		   mid = (int) allMoviesSeenByActiveUser.get(m);               	
	               	
	               	 //Get weight Rating (I)
	    	         if(activeIndependentRatingSim.containsKey(mid))
	    	          {	  
			    	  	   rIndependentWeight= activeIndependentRatingSim.get(mid);		//get value (sim) against the key (mid)	        	  
			    	 	                 	        	   
			        	   combinedWeight[combination-1]  = ( rIndependentWeight);            //rating (independent)
			        	   divisionSim[combination-1]     = Math.abs( rIndependentWeight);    //rating (independent)
				           
			        	 //  if(combinedWeight[combination-1]>0)
				           {
					    	   sumTop[combination-1]    +=  moviesAndRatingsOfActiveUser.get(mid) * combinedWeight[combination-1];
					    	   sumBottom[combination-1] += divisionSim[combination-1];
					    	   
					    	//   System.out.println("rating = "+ activeUserMovieAndRatingList_R.get(mid));
					    	   
					    	   
				           }
	    	          }
	               } //end of for
	        	   
	        	/*   System.out.println("Top = "+   sumTop[combination-1]);
		    	   System.out.println("Bottom = "+   divisionSim[combination-1] );*/
	            }
	           
	          //--------------------------------------------------------------------------
	          // combination = 3 --> Feature(I)
	          // 
  	          //--------------------------------------------------------------------------
	           
	              else if (combination==3)
	              {
	            	  infoAbtComb = "Feature(I)";
	            	  
	           	   for (int m=0;m<totalMovies;m++) 
	               {	              
	               	 mid = (int) allMoviesSeenByActiveUser.get(m);
	               	
		               	 //Get weight Feature (I)
		    	         if(activeIndependentFeatureSim.containsKey(mid))
		    	          {	  
			    	  	         fIndependentWeight = activeIndependentFeatureSim.get(mid);		//get value (sim) against the key (mid)	        	  
			    	    	    	         
					             combinedWeight[combination-1] = ( fIndependentWeight); 	  //demo (independent)
					             divisionSim[combination-1] = Math.abs( fIndependentWeight);
			
					             
					           //if(combinedWeight[combination-1]>0 || combinedWeight[combination-1]<=0)
						       {
							  	   sumTop[combination-1] +=  moviesAndRatingsOfActiveUser.get(mid) * combinedWeight[combination-1];
							  	   sumBottom[combination-1] +=(divisionSim[combination-1]);
							    	   
						       }

		    	          }
		    	         
		                }//end of for

	              }

/*	       	  
	          //--------------------------------------------------------------------------
	          // combination = 4 --> Rating(I) * Demo (I)
	          // Take Both sim movies
  	          //--------------------------------------------------------------------------
	           
	         
	          else if(combination==4) 
	           {
	        	  infoAbtComb = "Demo(I) + Rating (I)";
	        	   for (int m=0;m<totalMoviesSize_R;m++) 
	               {	              
	               	 mid = movieKeys_R.get(m);
	               	
	               	 //Get weight Rating (I)
	    	         if(activeIndependentRatingSim.containsKey(mid))
	    	          {	  
	    	  	        	  rIndependentWeight= activeIndependentRatingSim.get(mid);		//get value (sim) against the key (mid)	        	  
	    	 	    	        	
	    	          }
	    	         
	    	         //Get weight Demo (I)
	    	         if(activeIndependentDemoSim.containsKey(mid))
	    	          {	  
	    	  	        	  dIndependentWeight= activeIndependentDemoSim.get(mid);		//get value (sim) against the key (mid)	        	  
	    	 	    	        	
	    	          }
	    	                     	   
	        	    combinedWeight[combination-1] = (dIndependentWeight) *( rIndependentWeight);   //combination add (independent)
	        	    divisionSim[combination-1] = (Math.abs(dIndependentWeight) *( rIndependentWeight));   //combination add (independent)
	      	     

	        	   	if(combinedWeight[combination-1]>0)
			           {
				    	   sumTop[combination-1] +=  activeUserMovieAndRatingList_R.get(mid) * combinedWeight[combination-1];
				    	   sumBottom[combination-1] +=divisionSim[combination-1];
			           }
	               }//end of for
	            }
*/
	          //--------------------------------------------------------------------------
	          // combination = 4 --> Rating(I) + Demo (I)
	          // Take Both sim movies
  	          //--------------------------------------------------------------------------
	           

	          else if(combination==4) 
	           {
	        	  infoAbtComb = "Demo(I) + Rating(I)";
	        	   
		       	   for (int m=0;m<totalMovies;m++) 
	               {	              
	               	 mid = (int) allMoviesSeenByActiveUser.get(m);
	               	 	
	               	 //Get weight Rating (I)
	    	         if(activeIndependentRatingSim.containsKey(mid))
	    	          {	  
	    	  	        	  rIndependentWeight= activeIndependentRatingSim.get(mid);		//get value (sim) against the key (mid)	        	  
	    	 	    	       	      	    	         
			    	         //Get weight Demo (I)
			    	         if(activeIndependentDemoSim.containsKey(mid))
			    	          {	  
			    	  	        	  dIndependentWeight= activeIndependentDemoSim.get(mid);		//get value (sim) against the key (mid)	        	  
			    	 	    	        	
			    	          }
			    	         
			                   	   
			        	    combinedWeight[combination-1] = (dIndependentLinear * dIndependentWeight) + (rIndependentLinear * rIndependentWeight);   //combination add (independent)
			        	    divisionSim[combination-1] = (dIndependentLinear * Math.abs(dIndependentWeight)) + (rIndependentLinear * Math.abs(rIndependentWeight));   //combination add (independent)
			      	   
		
			        	 //  	if(combinedWeight[combination-1]>0)
					           {
						    	   sumTop[combination-1] +=  moviesAndRatingsOfActiveUser.get(mid) * combinedWeight[combination-1];
						    	   sumBottom[combination-1] +=divisionSim[combination-1];
					           }
			    	          
	    	          }
	               }//end of for
	            }
	           	           
	           
	          //--------------------------------------------------------------------------
	          // combination = 5 --> Rating(I) + Feature (I)
	          // Take Both sim movies
  	          //--------------------------------------------------------------------------
	           

	          else if(combination==5) 
	           {
	        	  infoAbtComb = "Feature(I) + Rating(I)";
	       	   
	        	 for (int m=0;m<totalMovies;m++) 
	        	 {	              
	        		 mid = (int) allMoviesSeenByActiveUser.get(m);
               	
	               	 //Get weight Rating (I)
	    	         if(activeIndependentRatingSim.containsKey(mid))
	    	          {	  
	    	  	        	 rIndependentWeight= activeIndependentRatingSim.get(mid);		//get value (sim) against the key (mid)	        	  
	    	  	        	 
	    	  	        	 combinedWeight[combination-1] = (rIndependentLinear * rIndependentWeight);
	    		        	 divisionSim[combination-1] = (rIndependentLinear * Math.abs(rIndependentWeight));
	    		      	    
	    		        	sumTop[combination-1] +=  moviesAndRatingsOfActiveUser.get(mid) * combinedWeight[combination-1];
	    					sumBottom[combination-1] +=divisionSim[combination-1];
	    				  
	    	          }
	    	         
	    	         //Get weight Feature (I)
	    	         if(activeIndependentFeatureSim.containsKey(mid))
	    	          {	  
   	  	        	     fIndependentWeight= activeIndependentFeatureSim.get(mid);		//get value (sim) against the key (mid)	        	  
	    	  	       	 combinedWeight[combination-1] = (fIndependentLinear * fIndependentWeight) ;
    		        	 divisionSim[combination-1] = (fIndependentLinear * Math.abs(fIndependentWeight));
	    		      	   
    		             sumTop[combination-1] +=  moviesAndRatingsOfActiveUser.get(mid) * combinedWeight[combination-1];
   					     sumBottom[combination-1] +=divisionSim[combination-1];
	    					 	
	    	          }
	    	         
	                   	   
	        	   
	               }//end of for
	            }
	           	           
	           
	           
	          //--------------------------------------------------------------------------
	          // combination = 6 --> Demo(I) + Feature (I)
	          // Take Both sim movies
  	          //--------------------------------------------------------------------------
	           

	          else if(combination==6) 
	           {
	        	  infoAbtComb = "Feature(I) + Demo(I)";
	        
	        	  for (int m=0;m<totalMovies;m++) 
		        	{	              
		        		 mid = (int) allMoviesSeenByActiveUser.get(m);
		        		 
	               	 //Get weight Rating (I)
	    	         if(activeIndependentRatingSim.containsKey(mid))
	    	          {	  
	    	  	         rIndependentWeight= activeIndependentRatingSim.get(mid);		//get value (sim) against the key (mid)	        	  
    	  	        	 combinedWeight[combination-1] = (rIndependentLinear * rIndependentWeight);
    		        	 divisionSim[combination-1] = (rIndependentLinear * Math.abs(rIndependentWeight));
    		      	     sumTop[combination-1] +=  moviesAndRatingsOfActiveUser.get(mid) * combinedWeight[combination-1];
    					 sumBottom[combination-1] +=divisionSim[combination-1];
    				  
	    	          }
	    	         
	    	         //Get weight Feature (I)
	    	         if(activeIndependentFeatureSim.containsKey(mid))
	    	          {	  
	    	        	 fIndependentWeight= activeIndependentFeatureSim.get(mid);		//get value (sim) against the key (mid)	        	  
	    	  	         combinedWeight[combination-1] = (fIndependentLinear * fIndependentWeight) ;
	    		         divisionSim[combination-1] = (fIndependentLinear * Math.abs(fIndependentWeight));
	    		      	 sumTop[combination-1] +=  moviesAndRatingsOfActiveUser.get(mid) * combinedWeight[combination-1];
    					 sumBottom[combination-1] +=divisionSim[combination-1];
	    					        	
	    	          }
	    	         
		        	}//end of for
	            }
	           	           
	           
	          
	          
	          //--------------------------------------------------------------------------
	          // combination = 7 --> Demo(D)
	          // Take Rating sim movies
  	          //--------------------------------------------------------------------------
	           

	          if(combination==7) 
	           {
	        	  infoAbtComb = "Demo(D)";
	        	 

	        	  for (int m=0;m<totalMovies;m++) 
		        	{	              
		        		  mid = (int) allMoviesSeenByActiveUser.get(m);
		        		  dDependentWeight = 0;	        		   
	               	 	               	
	               	 //Get weight demo (d)
	    	         if(activeDependentDemoSim.containsKey(mid))
	    	          {	  
	    	  	         dDependentWeight= activeDependentDemoSim.get(mid);		//get value (sim) against the key (mid)	        	  
	    	 	    	        	
	    	          }
	    	       	             	        	   
	        	   	combinedWeight[combination-1] = (dDependentLinear * dDependentWeight);	      //demo (dependent)
	        		divisionSim[combination-1] = Math.abs( dDependentWeight);	      //demo (dependent)
	      	     
	        	   	//if(combinedWeight[combination-1]>0)
			           {
				    	   sumTop[combination-1] +=  moviesAndRatingsOfActiveUser.get(mid) * combinedWeight[combination-1];
				    	   sumBottom[combination-1] +=(divisionSim[combination-1]);
			           }
	               }//end of for
	            }
	           	       
	          //--------------------------------------------------------------------------
	          // combination = 8 --> Demo(FD)
	          // Take Rating sim movies
  	          //--------------------------------------------------------------------------
	           

	          if(combination==8) 
	           {
	        	  infoAbtComb = "Demo(FD)";
	        	  

	        	  for (int m=0;m<totalMovies;m++) 
		        	{	              
			        	 mid = (int) allMoviesSeenByActiveUser.get(m);		        		 
			             dDependentWeight = 0;	        		   
	               	
	               	
	               	 //Get weight demo (I)
	    	         if(activeFDependentDemoSim.containsKey(mid))
	    	          {	  
	    	  	         dFDependentWeight= activeFDependentDemoSim.get(mid);		//get value (sim) against the key (mid)	        	  
	    	 	    	        	
	    	          }
	    	       	             	        	   
	        	   	combinedWeight[combination-1] = (dFDependentLinear * dFDependentWeight);	      //demo (dependent)
	        		divisionSim[combination-1] = Math.abs( dFDependentWeight);	      //demo (dependent)
	      	     
	        	   	//if(combinedWeight[combination-1]>0)
			           {
				    	   sumTop[combination-1] +=  moviesAndRatingsOfActiveUser.get(mid) * combinedWeight[combination-1];
				    	   sumBottom[combination-1] +=(divisionSim[combination-1]);
			           }
	               }//end of for
	            }
	           	  
	          
	          //--------------------------------------------------------------------------
	          // combination = 9 --> Demo(DD)
	          // Take Feature sim movies
  	          //--------------------------------------------------------------------------
	           

	          if(combination==9) 
	           {
	        	  infoAbtComb = "Demo(DD)";

	        	  for (int m=0;m<totalMovies;m++) 
		        	{	              
		        	  mid = (int) allMoviesSeenByActiveUser.get(m);		        		 
	        		  dDoubleDependentWeight = 0;	        		   
	               	 
	               	
	               	 //Get weight demo (I)
	    	         if(activeDoubleDependentDemoSim.containsKey(mid))
	    	          {	  
	    	  	         dDoubleDependentWeight= activeDoubleDependentDemoSim.get(mid);		//get value (sim) against the key (mid)	        	  
	    	 	    	        	
	    	          }
	    	       	             	        	   
	        	   	combinedWeight[combination-1] = (dDoubleDependentLinear * dDoubleDependentWeight);	      //demo (dependent)
	        		divisionSim[combination-1] = Math.abs( dDoubleDependentWeight);	      //demo (dependent)
	      	     
	        	   	//if(combinedWeight[combination-1]>0)
			           {
				    	   sumTop[combination-1] +=  moviesAndRatingsOfActiveUser.get(mid) * combinedWeight[combination-1];
				    	   sumBottom[combination-1] +=(divisionSim[combination-1]);
			           }
	               }//end of for
	            }
	           	   

	          
	       //--------------------------------------------------------------------------
	       // combination = 10 --> Feature(D)
	       // Take Rating sim movies
  	       //--------------------------------------------------------------------------
	          
	          
	          if(combination==10) 
	           {
	        	  infoAbtComb = "Feature(D)";
	        	  

	        	  for (int m=0;m<totalMovies;m++) 
		        	{	              
		        		 mid = (int) allMoviesSeenByActiveUser.get(m);
		        		 fDependentWeight = 0;	        		   
	               	   
	               	
	               	 //Get weight Feature(D)
	    	         if(activeDependentFeatureSim.containsKey(mid))
	    	          {	  
	    	  	         fDependentWeight= activeDependentFeatureSim.get(mid);		//get value (sim) against the key (mid)	        	  
	    	 	    	        	
	    	          }
	    	     	             	        	   
	        	   	combinedWeight[combination-1] = (fDependentLinear * fDependentWeight);	      //demo (dependent)
	        		divisionSim[combination-1] = Math.abs( fDependentWeight);	      //demo (dependent)
	      	     
	        	   	//if(combinedWeight[combination-1]>0)
			           {
				    	   sumTop[combination-1] +=  moviesAndRatingsOfActiveUser.get(mid) * combinedWeight[combination-1];
				    	   sumBottom[combination-1] +=(divisionSim[combination-1]);
			           }
	               }//end of for

	           }
	          
	          //--------------------------------------------------------------------------
	          // combination = 11 --> Rating(D)
	          // Take demo sim movies
  	          //--------------------------------------------------------------------------
	           

	          else if(combination==11) 
	           {
	        	  infoAbtComb = "Rating(D)";

	        	  for (int m=0;m<totalMovies;m++) 
		        	{	              
		        		 mid = (int) allMoviesSeenByActiveUser.get(m);
		        		 rDependentWeight = 0;	
		        		 
	               	 //Get weight demo (d)
	    	         if(activeDependentRatingSim.containsKey(mid))
	    	          {	  
	    	  	         rDependentWeight= activeDependentRatingSim.get(mid);		//get value (sim) against the key (mid)	        	  
	    	 	    	        	
	    	          }
	               	        	   
	        	   	combinedWeight[combination-1] = (rDependentLinear * rDependentWeight);	      //rating (dependent)
	        	 	divisionSim[combination-1]    = Math.abs(rDependentLinear * rDependentWeight);	      //rating (dependent)
	        	 	
	        	  // 	if(combinedWeight[combination-1]>0)
			           {
				    	   sumTop[combination-1] +=  moviesAndRatingsOfActiveUser.get(mid) * combinedWeight[combination-1];
				    	   sumBottom[combination-1] +=divisionSim[combination-1];
			           }
	               } //end of for     
	            }
	           	           
	          //--------------------------------------------------------------------------
	          // combination = 12 --> Rating(FD)
	          // Take Feature sim movies
  	          //--------------------------------------------------------------------------
	           
	          else if(combination==12) 
	           {
	        	  infoAbtComb = "Rating(FD)";
	        	  for (int m=0;m<totalMovies;m++) 
		        	{	              
		        		 mid = (int) allMoviesSeenByActiveUser.get(m);
		        		 rFDependentWeight = 0;	
		        		 
	               	 //Get weight demo (fd)
	    	         if(activeFDependentRatingSim.containsKey(mid))
	    	          {	  
	    	        	 rFDependentWeight= activeDependentRatingSim.get(mid);		//get value (sim) against the key (mid)	        	  
	    	 	    	        	
	    	          }
	               	        	   
	        	   	combinedWeight[combination-1] = (rFDependentLinear * rFDependentWeight);	      //rating (dependent)
	        	 	divisionSim[combination-1]    = Math.abs(rFDependentLinear * rFDependentWeight);	      //rating (dependent)
	        	 	
	        	  // 	if(combinedWeight[combination-1]>0)
			           {
				    	   sumTop[combination-1] +=  moviesAndRatingsOfActiveUser.get(mid) * combinedWeight[combination-1];
				    	   sumBottom[combination-1] +=divisionSim[combination-1];
			           }
	               } //end of for     
	            }
	           	   
	          

	         
	          //--------------------------------------------------------------------------
	          // combination = 13 --> Rating(D) + Demo (D)
	          // Take Both sim movies
  	          //--------------------------------------------------------------------------
	           
	          else if(combination==13) 
	           {
	        	  infoAbtComb = "Demo(D) + Rating (D)";
	         	  for (int m=0;m<totalMovies;m++) 
		        	{	              
		        		 mid = (int) allMoviesSeenByActiveUser.get(m);
		        		 rDependentWeight = 0;	
		        		 dDependentWeight = 0;
		        		 
	               	 //Get weight Rating (D)
	    	         if(activeDependentRatingSim.containsKey(mid))
	    	          {	  
	    	        	 rDependentWeight= activeDependentRatingSim.get(mid);		//get value (sim) against the key (mid)	    	        		        	  
	    	 	    	        	
	    	          }
	    	         
	    	         //Get weight Demo (D)
	    	         if(activeDependentDemoSim.containsKey(mid))
	    	          {	  
	    	  	        	dDependentWeight= activeDependentDemoSim.get(mid);		//get value (sim) against the key (mid)	        	  
	    	 	    	        	
	    	          }
	    	         
	               	        	   
	        	    combinedWeight[combination-1] = (dDependentLinear * dDependentWeight) + (rDependentLinear * rDependentWeight);   //combination add (independent)
	        	    divisionSim[combination-1] = (dDependentLinear * Math.abs(dDependentWeight)) + (rDependentLinear * Math.abs(rDependentWeight));   //combination add (independent)
	      	       

	        	 //  	if(combinedWeight[combination-1]>0)
			           {
				    	   sumTop[combination-1] +=  moviesAndRatingsOfActiveUser.get(mid) * combinedWeight[combination-1];
				    	   sumBottom[combination-1] +=divisionSim[combination-1];
			           }
	               }//end of for
	            }
	        
	          //--------------------------------------------------------------------------
	          // combination = 11 --> Rating(D) * Demo (D)
	          // Take Both sim movies
  	          //--------------------------------------------------------------------------
	           
/*
	          else if(combination==11) 
	           {
	        	  infoAbtComb = "Demo(D) + Rating (D)";
	        	  
	           	  for (int m=0;m<totalMovies;m++) 
		        	{	              
		        		 mid = (int) allMoviesSeenByActiveUser.get(m);
		        		 rDependentWeight = 0;	
		        		 dDependentWeight = 0;
		        		 
	               	 //Get weight Rating (R)
	    	         if(activeDependentRatingSim.containsKey(mid))
	    	          {	  
	    	        		        	  
	    	        	 rDependentWeight= activeDependentRatingSim.get(mid);		//get value (sim) against the key (mid)	
	    	          }
	    	         
	    	         //Get weight Demo (D)
	    	         if(activeDependentDemoSim.containsKey(mid))
	    	          {	  
	    	  	        	  dDependentWeight= activeDependentDemoSim.get(mid);		//get value (sim) against the key (mid)	        	  
	    	 	    	        	
	    	          }
	    	         
	                       	   
	        	    combinedWeight[combination-1] = (dDependentWeight) * (rDependentWeight);   //combination add (independent)
	        	    divisionSim[combination-1] = (Math.abs(dDependentWeight) * (rDependentWeight));   //combination add (independent)
	      	       

	        	   	if(combinedWeight[combination-1]>0)
			           {
				    	   sumTop[combination-1] +=  allMoviesSeenByActiveUser.get(mid) * combinedWeight[combination-1];
				    	   sumBottom[combination-1] +=divisionSim[combination-1];
			           }
	               }//end of for
	        	   
	            }
     

*/
		         
	       
	          //--------------------------------------------------------------------------
	          // combination = 14 --> (Rating(I) + Demo (D))
	          // IDemo1
  	          //--------------------------------------------------------------------------
	           
	          
	              else if (combination==14) 
		           {  
	            	  infoAbtComb = "Demo(D) + Rating (I)";
	               	  
	            	  for (int m=0;m<totalMovies;m++) 
			        	{	              
			        		 mid = (int) allMoviesSeenByActiveUser.get(m);
			        		 rIndependentWeight = 0;	
			        		 dDependentWeight = 0;
			        		 
		               	
		               	 //Get weight Rating (I)
		    	         if(activeIndependentRatingSim.containsKey(mid))
		    	          {	  
		    	  	         rIndependentWeight= activeIndependentRatingSim.get(mid);		//get value (sim) against the key (mid)	        	  
		    	 	    	        	
		    	          }
		    	    
		    	         
		    	         //Get weight Demo (D)
		    	         if(activeDependentDemoSim.containsKey(mid))
		    	          {	  
		    	  	         dDependentWeight= activeDependentDemoSim.get(mid);		//get value (sim) against the key (mid)	        	  
		    	 	    	        	
		    	          }
		    	       
		                   	   
		        	    combinedWeight[combination-1] = (dDependentWeight) + (rIndependentWeight);   //combination add (independent)
		        	    divisionSim[combination-1] = (Math.abs(dDependentWeight)) + (Math.abs(rIndependentWeight));   //combination add (independent)
		      	   

		        	   //	if(combinedWeight[combination-1]>0)
				           {
					    	   sumTop[combination-1] +=  moviesAndRatingsOfActiveUser.get(mid) * combinedWeight[combination-1];
					    	   sumBottom[combination-1] +=divisionSim[combination-1];
				           }
		               }///end of for
	        	   
	           }

	          //--------------------------------------------------------------------------
	          // combination = 15 --> Rating(I) + Feature(D)
	          // 
  	          //--------------------------------------------------------------------------
	           
	          else if (combination==15) 
	           { 
	        	  infoAbtComb = "Feature(D) + Rating (I)";
	           	  for (int m=0;m<totalMovies;m++) 
		        	{	              
		        		 mid = (int) allMoviesSeenByActiveUser.get(m);
		        		 
	        		   rIndependentWeight = 0;
	        		   fDependentWeight   = 0;
	        		   
	           
	               	
	               	 //Get weight Rating (I)
	    	         if(activeIndependentRatingSim.containsKey(mid))
	    	          {	  
	    	  	        	  rIndependentWeight= activeIndependentRatingSim.get(mid);		//get value (sim) against the key (mid)	        	  
	    	 	    	        	
	    	          }
	    	         
	    	         	    	         
	    	         //Get weight Feature (D)
	    	         if(activeDependentFeatureSim.containsKey(mid))
	    	          {	  
	    	  	        	  fDependentWeight= activeDependentFeatureSim.get(mid);		//get value (sim) against the key (mid)	        	  
	    	 	    	        	
	    	          }
	    	         	    	         
	                   	   
	        	    combinedWeight[combination-1] = (fDependentWeight) + (rIndependentWeight);   //combination add (independent)
	        	    divisionSim[combination-1] = (Math.abs(fDependentWeight)) + (Math.abs(rIndependentWeight));   //combination add (independent)
	      	   

	        	   	//if(combinedWeight[combination-1]>0)
			           {
				    	   sumTop[combination-1] +=  moviesAndRatingsOfActiveUser.get(mid) * combinedWeight[combination-1];
				    	   sumBottom[combination-1] +=divisionSim[combination-1];
			           }
	               }//end of for
	        	   
	           }
	           
	      	
	          //--------------------------------------------------------------------------
	          // combination = 16 --> Demo(D) + Feature(D)
	          // 
  	          //--------------------------------------------------------------------------
	                
	         
	          else if (combination==16) 
	           { 
	        	  infoAbtComb = "Demo(D) + Feature (D)";
	           	  for (int m=0;m<totalMovies;m++) 
		        	{	              
		        		 mid = (int) allMoviesSeenByActiveUser.get(m);
		        		 fDependentWeight = 0;	
		        		 dDependentWeight = 0;
		        	    		   
	         
	               	
	               	 //Get weight Feature (D)
	    	         if(activeIndependentFeatureSim.containsKey(mid))
	    	          {	  
	    	  	        	  fDependentWeight= activeDependentFeatureSim.get(mid);		//get value (sim) against the key (mid)	        	  
	    	 	    	        	
	    	          }
	    	         
	    	         //Get weight Demo (D)
	    	         if(activeDependentDemoSim.containsKey(mid))
	    	          {	  
	    	  	        	  dDependentWeight= activeDependentDemoSim.get(mid);		//get value (sim) against the key (mid)	        	  
	    	 	    	        	
	    	          }
	    	         
	                   	   
	        	    combinedWeight[combination-1] = (fDependentWeight) + (dDependentWeight);   //combination add (independent)
	        	    divisionSim[combination-1] = (Math.abs(fDependentWeight)) + (Math.abs(dDependentWeight));   //combination add (independent)
	      	   

	        	   	//if(combinedWeight[combination-1]>0)
			           {
				    	   sumTop[combination-1] +=  moviesAndRatingsOfActiveUser.get(mid) * combinedWeight[combination-1];
				    	   sumBottom[combination-1] +=divisionSim[combination-1];
			           }
	               }//end of for
	        	   
	           }

	      //--------------------------------------------------------------------------
	      // combination = 17 --> Demo(D) + Feature(I) + Rating (D)
	      // 
  	      //--------------------------------------------------------------------------
	           	          
	          else if (combination==17) 
	           { 
	        	  infoAbtComb = "Demo(D)+Feature(I) + Rating (D)";
	           	 
	        	  for (int m=0;m<totalMovies;m++) 
		        	{	              
		        	   mid = (int) allMoviesSeenByActiveUser.get(m);
		        		
	        		   rDependentWeight   = 0;
	        		   fIndependentWeight = 0;
	        		   dDependentWeight   = 0;
	        		   
	        		      
	             
	               	 //Get weight rating(D)
	               	 if(activeDependentRatingSim.containsKey(mid))
	    	          {	  
	    	  	        	  rDependentWeight = activeDependentRatingSim.get(mid);		//get value (sim) against the key (mid)	        	  
	    	 	    	        	
	    	          }
	    	     
	               	 //Get weight Feature (I)
	    	         if(activeIndependentFeatureSim.containsKey(mid))
	    	          {	  
	    	  	        	  fIndependentWeight= activeIndependentFeatureSim.get(mid);		//get value (sim) against the key (mid)	        	  
	    	 	    	        	
	    	          }
	    	         
	    	         //Get weight Demo (D)
	    	         if(activeDependentDemoSim.containsKey(mid))
	    	          {	  
	    	  	        	  dDependentWeight= activeDependentDemoSim.get(mid);		//get value (sim) against the key (mid)	        	  
	    	 	    	        	
	    	          }
	    	         
	                   	   
	        	    combinedWeight[combination-1] = (rDependentWeight) + (fIndependentWeight) + (dDependentWeight);   //combination add (independent)
	        	    divisionSim[combination-1] = Math.abs(rIndependentWeight) + (Math.abs(fDependentWeight)) + (Math.abs(dDependentWeight));   //combination add (independent)
	      	   

	        	   	//if(combinedWeight[combination-1]>0)
			           {
				    	   sumTop[combination-1] +=  moviesAndRatingsOfActiveUser.get(mid) * combinedWeight[combination-1];
				    	   sumBottom[combination-1] +=divisionSim[combination-1];
			           }
	               }//end of for
	        	   
	           }

		      //--------------------------------------------------------------------------
		      // combination = 18 --> Demo(D) + Feature(D) + Rating (I)
		      // 
	  	      //--------------------------------------------------------------------------
		           	          
		          else if (combination==18) 
		           { 
		        	  infoAbtComb = "Demo(D)+Feature(D) + Rating (I)";
		           	  for (int m=0;m<totalMovies;m++) 
			        	{	              
			        	   mid = (int) allMoviesSeenByActiveUser.get(m);
			        		
		        		   rIndependentWeight = 0;
		        		   fDependentWeight   = 0;
		        		   dDependentWeight   = 0;
		        		   
		        		   

		             
		               	 //Get weight rating(I)
		               	 if(activeIndependentRatingSim.containsKey(mid))
		    	          {	  
		    	  	        	  rIndependentWeight = activeIndependentRatingSim.get(mid);		//get value (sim) against the key (mid)	        	  
		    	 	    	        	
		    	          }
		    	     
		               	 //Get weight Feature (D)
		    	         if(activeDependentFeatureSim.containsKey(mid))
		    	          {	  
		    	  	        	  fDependentWeight= activeDependentFeatureSim.get(mid);		//get value (sim) against the key (mid)	        	  
		    	 	    	        	
		    	          }
		    	         
		    	         //Get weight Demo (D)
		    	         if(activeDependentDemoSim.containsKey(mid))
		    	          {	  
		    	  	        	  dDependentWeight= activeDependentDemoSim.get(mid);		//get value (sim) against the key (mid)	        	  
		    	 	    	        	
		    	          }
		    	         
		                   	   
		        	    combinedWeight[combination-1] = (rIndependentWeight) + (fDependentWeight) + (dDependentWeight);   //combination add (independent)
		        	    divisionSim[combination-1] = Math.abs(rIndependentWeight) + (Math.abs(fDependentWeight)) + (Math.abs(dDependentWeight));   //combination add (independent)
		      	   

		        	   	//if(combinedWeight[combination-1]>0)
				           {
					    	   sumTop[combination-1] +=  moviesAndRatingsOfActiveUser.get(mid) * combinedWeight[combination-1];
					    	   sumBottom[combination-1] +=divisionSim[combination-1];
				           }
		               }//end of for
		        	   
		           }
		           
	      	  //--------------------------------------------------------------------------
		      // combination = 19 --> Demo(D) + Feature(D) + Rating (I) with Alpha, 
		      // Beta and Gamma coff
	  	      //--------------------------------------------------------------------------
		           	          
		          else if (combination==19) 
		           { 
		        	  infoAbtComb = "Demo(I)+Feature(I) + Rating (I) ";
		        	   for (int m=0;m<totalMoviesSize_R;m++) 
		               {	  
		        		   rIndependentWeight = 0;
		        		   fIndependentWeight   = 0;
		        		   dIndependentWeight   = 0;
		        		   
		        		   

		             
		               	 //Get weight rating(I)
		               	 if(activeIndependentRatingSim.containsKey(mid))
		    	          {	  
		    	  	        	  rIndependentWeight = activeIndependentRatingSim.get(mid);		//get value (sim) against the key (mid)	        	  
		    	 	    	        	
		    	          }
		    	     
		               	 //Get weight Feature (I)
		    	         if(activeIndependentFeatureSim.containsKey(mid))
		    	          {	  
		    	  	        	  fIndependentWeight= activeIndependentFeatureSim.get(mid);		//get value (sim) against the key (mid)	        	  
		    	 	    	        	
		    	          }
		    	         
		    	         //Get weight Demo (I)
		    	         if(activeIndependentDemoSim.containsKey(mid))
		    	          {	  
		    	  	        	  dIndependentWeight= activeIndependentDemoSim.get(mid);		//get value (sim) against the key (mid)	        	  
		    	 	    	        	
		    	          }
		    	         
		                   	   
		        	    combinedWeight[combination-1] = ( rIndependentWeight) + ( fIndependentWeight) + (dIndependentWeight);   //combination add (independent)
		        	    divisionSim[combination-1] = Math.abs(rIndependentWeight) + (Math.abs(fIndependentWeight)) + (Math.abs(dIndependentWeight));   //combination add (independent)
		      	   

		        	   	//if(combinedWeight[combination-1]>0)
				           {
					    	   sumTop[combination-1] +=  allMoviesSeenByActiveUser.get(mid) * combinedWeight[combination-1];
					    	   sumBottom[combination-1] +=divisionSim[combination-1];
				           }
		               }//end of for
		        	   
		           }
		           
		      		           	          
	          
	          
	          //--------------------------------------------------------------------------
	          // combination = 4 --> Rating(I) + (Demo (D) * Rating(I))
	          // IDemo4
  	          //--------------------------------------------------------------------------
	           
	    /*      
	           else if (combination==4) 
	           { 
	        	   for (int m=0;m<totalMoviesSize_R;m++) 
	               {	              
	               	 mid = movieKeys_R.get(m);
	               	
	               	 //Get weight Rating (I)
	    	         if(activeIndependentRatingSim.containsKey(mid))
	    	          {	  
	    	  	        	  rIndependentWeight= activeIndependentRatingSim.get(mid);		//get value (sim) against the key (mid)	        	  
	    	 	    	        	
	    	          }
	    	         
	    	         //Get weight Demo (D)
	    	         if(activeDependentDemoSim.containsKey(mid))
	    	          {	  
	    	  	        	  dDependentWeight= activeDependentDemoSim.get(mid);		//get value (sim) against the key (mid)	        	  
	    	 	    	        	
	    	          }
	    	         
	                   	   
	        	    combinedWeight[combination-1] =  rIndependentWeight + dDependentWeight * rIndependentWeight;   //combination add (independent)
	        	    divisionSim[combination-1] =    Math.abs(rIndependentWeight)+  (Math.abs(dDependentWeight * rIndependentWeight));   //combination add (independent)
	      	   

	        	   //	if(combinedWeight[combination-1]>0)
			           {
				    	   sumTop[combination-1] +=  activeUserMovieAndRatingList_R.get(mid) * combinedWeight[combination-1];
				    	   sumBottom[combination-1] +=divisionSim[combination-1];
			           }
	               }//end of for
	        	   
	             }
	          */
	          
	          //--------------------------------------------------------------------------
	          // combination = 5 --> Rating(I) + Demo (D) + (Rating(I) * Demo (D))
	          // IDemo5
  	          //--------------------------------------------------------------------------
	           
	         /* 
	           else if (combination==5) 
	           { 
	        	   for (int m=0;m<totalMoviesSize_R;m++) 
	               {	              
	               	 mid = movieKeys_R.get(m);
	               	
	               	 //Get weight Rating (I)
	    	         if(activeIndependentRatingSim.containsKey(mid))
	    	          {	  
	    	  	        	  rIndependentWeight= activeIndependentRatingSim.get(mid);		//get value (sim) against the key (mid)	        	  
	    	 	    	        	
	    	          }
	    	         
	    	         //Get weight Demo (D)
	    	         if(activeDependentDemoSim.containsKey(mid))
	    	          {	  
	    	  	        	  dDependentWeight= activeDependentDemoSim.get(mid);		//get value (sim) against the key (mid)	        	  
	    	 	    	        	
	    	          }
	    	         
	                   	   
	        	    combinedWeight[combination-1] = ( dDependentWeight) + (rIndependentWeight) + ( dDependentWeight * rIndependentWeight);   //combination add (independent)
	        	    divisionSim[combination-1] = Math.abs( dDependentWeight) + Math.abs(rIndependentWeight) + ( Math.abs (dDependentWeight * rIndependentWeight));  	      	   

	        	   	//if(combinedWeight[combination-1]>0)
			           {
				    	   sumTop[combination-1] +=  activeUserMovieAndRatingList_R.get(mid) * combinedWeight[combination-1];
				    	   sumBottom[combination-1] +=divisionSim[combination-1];
			           }
	               }//end of for
	        	   
	           }

	           */
	          //--------------------------------------------------------------------------
	          // combination = 6 --> Rating(I) + Demo (D)
	          // IDemo6
  	          //--------------------------------------------------------------------------
	           
	          /*
	           else if (combination==6) 
	           { 
	        	   for (int m=0;m<totalMoviesSize_R;m++) 
	               {	              
	               	 mid = movieKeys_R.get(m);
	               	
	               	 //Get weight Rating (I)
	    	         if(activeIndependentRatingSim.containsKey(mid))
	    	          {	  
	    	  	        	  rIndependentWeight= activeIndependentRatingSim.get(mid);		//get value (sim) against the key (mid)	        	  
	    	 	    	        	
	    	          }
	    	         
	    	         //Get weight Demo (D)
	    	         if(activeDependentDemoSim.containsKey(mid))
	    	          {	  
	    	  	        	  dDependentWeight= activeDependentDemoSim.get(mid);		//get value (sim) against the key (mid)	        	  
	    	 	    	        	
	    	          }
	    	         
	                   	   
	        	    combinedWeight[combination-1] = (dDependentWeight) + (rIndependentWeight);   //combination add (independent)
	        	    divisionSim[combination-1] = (Math.abs(dDependentWeight)) + (Math.abs(rIndependentWeight));   //combination add (independent)
	      	   

	        	   	//if(combinedWeight[combination-1]>0)
			           {
				    	   sumTop[combination-1] +=  activeUserMovieAndRatingList_R.get(mid) * combinedWeight[combination-1];
				    	   sumBottom[combination-1] +=divisionSim[combination-1];
			           }
	               }//end of for
	        	   
	           }
	           
	      		*/           
	          
        
	          
          //------------------------------  
	      // Check if bottom sum is zero
	      //------------------------------
        
        //if user didn't see any similar movies give avg rating for user
        if (sumBottom[combination-1] == 0)
        	{
        	    totalZeroAnswers++;
        	  //  System.out.println(" bottonSum=0 -->" + sumBottom[combination-1] + ", answer-->0");
        	//	return  myTrainingSet.getAverageRatingForUser(userID);        	
        	    return  0; 		//sparsity challenge (active user have not rated any similar movie)
        	}
        
        
         answer[combination-1] = sumTop[combination-1]/sumBottom[combination-1];
 
       //------------------------------  
	   // If prediction <0, return 0
       // Else return prediction
	   //------------------------------
   
 
         if (answer[combination-1]<0) 
        	 {
        	     totalNegativeAnswers++;
        	 //	   System.out.println(" answer<0 ||NAN -->" + ", " +answer[combination-1]);
        	 //	  System.out.println ("top =" + sumTop[combination-1]);
        	 //	 System.out.println ("bottom =" + sumBottom[combination-1]);
        	 	  
        	 
        	 	// return myTrainingSet.getAverageRatingForUser(userID);
        	     return 0;
        	  //   return answer[combination-1];
        	 
        	 }
         
           if( answer[combination-1] == Double.NaN) return 0;
      
      if(answer[combination-1] >0 || answer[combination-1] <0) // NO NAN
      { 
    	//  System.out.println(" answer -->" + answer[combination-1]);
    	  return answer[combination-1];
      
      }
    	  totalZeroAnswers++;
     // System.out.println(" answer-->0");
        return 0;
         
    }
 
/************************************************************************************************************************/

    //----------------------------------------------------------
    // First neighbour selection by ratings, then demo sim
    //----------------------------------------------------------
 
    
    /**
     * @author  Musi
     * @param   mid, uid, and no. of neighbours to be considered
     * @return  the weighted prediction of the movie 
     */
    
    private double weightedSumRatingAndThenDemo(int movieID, int userID, 
    											int NumberOfNeighbours, int combination
    											, double alpha, double beta)    
    {    	
        
                 
        OpenIntDoubleHashMap   activeRatingSim      = new OpenIntDoubleHashMap(); 			  //It will store active items' rating correlation weights
        OpenIntDoubleHashMap   activeDemoSim   		= new OpenIntDoubleHashMap(); 			  //It will store active items' demo ...
        OpenIntDoubleHashMap   activeFeatureSim 	= new OpenIntDoubleHashMap(); 			  //It will store active items' demo ...
        
        OpenIntDoubleHashMap   activeUserMovieAndRatingList  = new OpenIntDoubleHashMap(); 	  //active user saw this movie (first rating, find neighbours and then demo)
        //OpenIntDoubleHashMap   activeUserMovieAndRatingList  = new OpenIntDoubleHashMap();  //active user saw this movie (first demo, find neighbours and then rating)
        
        //Movies seen by active user
        LongArrayList allMoviesSeenByActiveUser = myTrainingSet.getMoviesSeenByUser(userID); 
        
        ArrayList<IntDoublePair> idSimRList = myStoredRSim.getSimilarMovies(movieID);       //it should be sorted as it is        
        ArrayList<IntDoublePair> idSimDList = myStoredDSim.getSimilarMovies(movieID);       //it should be sorted as it is
        ArrayList<IntDoublePair> idSimFList = myStoredFSim.getSimilarMovies(movieID);       //it should be sorted as it is
        
        double temp;
        int dummy=0;       
        double d1=0,d2=0,d3=0;
        
       //-----------------------------------------------------------------------------------------
       //-----------------------------------------------------------------------------------------
       // Rating similarity 
       // Formula: from item-based CF
       // All sim movies (similarity * active user rating) / sum over all sim movies (similarity)       
       //-----------------------------------------------------------------------------------------
       //-----------------------------------------------------------------------------------------
      	        
       // if(idSimList == null) System.out.println(" with null"); 
        if(idSimRList!= null) // //grab the ratings for all the similar movies the user has seen
        {
        	  int totalSize = idSimRList.size();
            
            
        //---------------------------------------------------------------------------
      	// Loop: number of neighbours
      	//---------------------------------------------------------------------------
      	
        	  
         for (int i=0, pointer=0; i<totalSize; i++)  //for how many neighbours to need each time
	       {	
        		if(pointer == NumberOfNeighbours) break;        			
	        
        	 	//-----------------------
            	// Get similar items	 
             	//-----------------------           
        		//get similar items for a movie
        		
        		IntDoublePair pair = (IntDoublePair)idSimRList.get(i);  
        		dummy++;
        		
        		
        		
        		
        		//--------------------------------------------------
        	 	// Save the weighted of all sim items
        		//--------------------------------------------------
        		
	            //if the user hasn't rated this one, skip it
        		//consider only +ve ratings (Filter: --------------------------)
		
        		if(allMoviesSeenByActiveUser.contains(pair.a))
	            {
        			temp = myTrainingSet.getRating(userID, pair.a);
        			activeUserMovieAndRatingList.put(pair.a, temp); // movie seen by active user, and its rating	            
	             	pointer++;	            	
	                	
	             	//-----------------------
	            	// Cosider All Weights	 (not giving good result, even in item-based CF)
	             	//-----------------------
	             	
	              	   activeRatingSim.put(pair.a, pair.b );
	             	
	             	//-----------------------
	            	// Tatse approach	 
	             	//-----------------------
	             	//( I think, we can not take this as well, as in this case IDemo3 is behaving same as Item-based CF)         	

	//             	activeRatingSim.put(pair.a, pair.b +1);
	             	   
	             	//-----------------------
	            	// Take only +ve weights	 
	             	//-----------------------
	                   //  For size =45, item-based score=0.78 and the Idemo3 proposed 0.77
	             	   //  It can be generalize I think
	             	
		    /*         	if( pair.b >0)
		             	{
		             		activeRatingSim.put(pair.a, pair.b );
		             	}
			*/
	             	
	    //         	   d1 += temp * pair.b;
	    //         	   d2 +=Math.abs(pair.b);
	             	   
	            } //end of if active user has rated a similar movie
	         } //end of for, 
           }//end of null condition check
    	
      //  System.out.println(" Expected Answer ="+ d1/d2);
      //  return d1/d2;
        
        //-----------------------------------------------------------------------------------------
         //-----------------------------------------------------------------------------------------
         // Demo similarity (Dependent)
         // Formula: from item-based CF
         // All sim movies (similarity * active user rating) / sum over all sim movies (similarity)       
         //-----------------------------------------------------------------------------------------
        //-----------------------------------------------------------------------------------------
        
        int totalSimMoviesSeenByActiveUser = activeUserMovieAndRatingList.size();
               
        if(idSimDList!= null && idSimRList!=null) // //grab the ratings for all the similar movies the user has seen
        {
        	  int totalSize = idSimDList.size();            
            
        //---------------------------------------------------------------------------
      	// Loop: number of neighbours
      	//---------------------------------------------------------------------------
      	        	  
         for (int i=0, pointer=0; i<totalSize; i++)  //for how many neighbours to need each time
	       {	
        		if ( pointer == NumberOfNeighbours) 
        				//&&	 pointer == totalSimMoviesSeenByActiveUser) 
        		
        			break;        			
	        
        	 	//-----------------------
            	// Get similar items	 
             	//-----------------------           
        		//get similar items for a movie
        		
        		IntDoublePair pair = (IntDoublePair)idSimDList.get(i);  
        		dummy++;
        		       		
        		//--------------------------------------------------
        	 	// Calculate the weighted sums of all sim items
        		//--------------------------------------------------

        		if(activeUserMovieAndRatingList.containsKey(pair.a)) //if active user saw this movie
        		{
        			
        			//All weights
        			activeDemoSim.put(pair.a, pair.b);
        			
        			//Taste approach
        //			activeDemoSim.put(pair.a, pair.b+1); 
        			
        			//Only >0
        //			if(pair.b>0)
       // 			activeDemoSim.put(pair.a, pair.b);
        			
        			//System.out.println(" weights -->" + pair.b);
        			pointer++;
        			
        		} //end if active user rated this
	       } //end of neighbour loop
        } //end of if list is not null
             		
             		
        //-----------------------------------------------------------------------------------------
        //-----------------------------------------------------------------------------------------
        // Ratings and Demo similarities are there 
        // Now combine them using a linear weighted average scheme
        // 
        // Formula: All sim movies (similarity * active user rating) / sum over all sim movies (similarity)       
        //-----------------------------------------------------------------------------------------
       //-----------------------------------------------------------------------------------------
    
        double rLinear = 1.0, dLinear = 1.0;
        double dWeight = 0.0, rWeight = 0.0;
        double combinedWeight[]  = new double[10];   //1 = demo
        double sumTop[]          = new double[10]; 	 //2 = ratings
        double sumBottom[] 		 = new double[10];  //3 =both
        double answer[]	         = new double[10];
        double divisionSim[]  	 = new double[10];   //1 = demo
        int    mid =0;
         
        
        IntArrayList  movieKeys = activeUserMovieAndRatingList.keys();
                
	      //--------------------------------------------------  
	      //go through all similar movies seen by active user
	      //--------------------------------------------------
        
        for (int i=0;i<totalSimMoviesSeenByActiveUser;i++) 
        {
       
        	 mid = movieKeys.get(i);     	

        	//---------------------------------
        	// Get demo and rating sim
        	//---------------------------------
          
	         if(activeDemoSim.containsKey(mid))
	          {
	        	  dWeight= activeDemoSim.get(mid);		//get value (sim) against the key (mid)
	        	  
	          }
	         
	         else dWeight =0;							//else weight =0 (OK, the reason the paper gave, that many of demographic corr will be zeros
	         	         
	          if(activeRatingSim.containsKey(mid))
	          {
	        	  rWeight= activeRatingSim.get(mid);		//get value (sim) against the key (mid)
	        	  
	          }
	         
	          else rWeight =0;
	          
         	 //---------------------------------
	         // Combine demo and rating sim
	         //---------------------------------
          
	          // Here we can do some sort of linear weights, but it will become very expensive
	          // Idea 1: Set the neighbours size constant, and then change the linear parameters
	                    
	          //We want to optimize Boosted_Sim3 here, means find Alpha and Beta,
	          //Which gives me lowest score
	          if(combination==1)
	          {
	        	  dLinear =alpha;
	        	  rLinear =beta;
	        	  
	        	combinedWeight[combination-1] = (dLinear * dWeight) + ( rLinear * rWeight);	  //combination add
	       	   	divisionSim[combination-1] =     Math.abs(dLinear * dWeight)+ Math.abs(rLinear * rWeight);
	           
	          
	          }
	          else if(combination ==2)//IDemo 4
	           {
	       	   	 // combinedWeight[combination-1] = (dWeight) + ( rWeight);	  //combination add
	       	   	 // divisionSim[combination-1] =     Math.abs(dWeight)+ Math.abs(rWeight);
	        	  
	        	  
	        	     combinedWeight[combination-1] = ( (dLinear * dWeight) );						  //rating
	        	   	 divisionSim[combination-1] =Math.abs((dLinear * dWeight) );
	        	   	 
	           }
	           
	          
	                     
	           else if(combination==3)
	        	   	{
	        	     combinedWeight[combination-1] = ( rWeight);						  //rating
	        	   	 divisionSim[combination-1] =Math.abs(rWeight);
	        	   	}
	           
	          
	           else if(combination==4)
      	       {
      	   		  combinedWeight[combination-1] = ( dWeight); 					 //demo
      	   		  divisionSim[combination-1] =Math.abs(dWeight);
      	       }
	          
	           else if(combination==5) //IDemo1
	        	    {
	        	   	combinedWeight[combination-1] = (rWeight * dWeight); 						  //combination multiply
	        		 divisionSim[combination-1] =     Math.abs(dWeight) * Math.abs(rWeight);
	        	    }

	           else if(combination==6)  //IDemo2 
	           {
	        	   combinedWeight[combination-1] =  dWeight+(rWeight * dWeight); 						  //combination multiply
      		 		divisionSim[combination-1] =    Math.abs(dWeight) + ( Math.abs(dWeight) * Math.abs(rWeight));
       	    	}

	           else if(combination==2)//IDemo3
	       	    {
		       	   		combinedWeight[combination-1] = (dWeight) + ( rWeight) + ((dWeight) * ( rWeight));	  //combination add
		       	   		divisionSim[combination-1] =     Math.abs(dWeight)+ Math.abs(rWeight) + (Math.abs(dWeight)* Math.abs(rWeight));

	       	   		
	       	    }

	         
	           
	          //--------------------------------------------------  
		      // Make Prediction using combined weights
		      //--------------------------------------------------
	     	        
	         // if(combinedWeight[combination-1]!=0)
	          {
		          sumTop[combination-1] +=  activeUserMovieAndRatingList.get(mid) * combinedWeight[combination-1];
		          sumBottom[combination-1] +=  divisionSim[combination-1];
	          }
	          
	        
        } //end of for (all similar movies seen by active user) 

        
          //---------------------------------------------------------------  
	      // Demo Corrlation only
	      //---------------------------------------------------------------
      
          //------------------------------  
	      // Check if bottom sum is zero
	      //------------------------------
        
        //if user didn't see any similar movies give avg rating for user
        if (sumBottom[combination-1] == 0)
        	{
        	    totalZeroAnswers1++;
        	//	System.out.println(" bottonSum=0 -->" + sumBottom);
        	//	return  myTrainingSet.getAverageRatingForUser(userID);        	
        	    return  0; 		//sparsity challenge (active user have not rated any similar movie)
        	}
        
        
         answer[combination-1] = sumTop[combination-1]/sumBottom[combination-1];
 
       //------------------------------  
	   // If prediction <0, return 0
       // Else return prediction
	   //------------------------------
   
 
         if (answer[combination-1]<0) 
        	 {
        	     totalNegativeAnswers1++;
        	 	// System.out.println(" answer<0 -->" + answer);
        	 	// return myTrainingSet.getAverageRatingForUser(userID);
        	     return answer[combination-1];
        	 
        	 }
      
         else if ( answer[combination-1] == Double.NaN)
        	 return 0;
         else 
        	 return answer[combination-1];
         
        
         
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
    
    
    //check its theory first
    
    private double regression(int movieID, int userID)    
    {
    	double predicted = -99;
        int    approxRating;
        double sumTop=0;
        double sumBottom=0;

    	//grab all similar movies and their similarities
        ArrayList<IntDoublePair> sims = db.getSimilarMovies(movieID, true);
    
	    //for each similar movie the user has seen
        for (IntDoublePair i : sims)        
        {
		    //use linear regression model to generate this user's rating for the sim movie
	 	    approxRating = predictKnownRating(movieID, i.a, userID);
       
		    //use above result as rating, calculate the weighted sums
            sumTop += (i.b * approxRating);
            sumBottom += Math.abs(i.b);

        }

        predicted = sumTop/sumBottom;
        return predicted;
    }

    
 /************************************************************************************************************************/
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
    private int predictKnownRating(int movie1, int movie2, int userID)    
    {
    	int predicted = -99;

    	//________________
	    //build the model:
    	//________________
    	
	    //grab the rating vector for movie1 in <user, rating> pairs
        ArrayList<Pair> targetV = db.getRatingVector(movie1);
	    
        //grab the rating vector for movie2
        ArrayList<Pair> simV = db.getRatingVector(movie2);
        
        //create one list of rating instances  (?????????????/)
        for (Pair p : simV)
        	targetV.add(p);

       /* grab mean (avg) ratings for movies 1 and 2 and for the userID
	    * calculate standard deviation for ratings at the same time for efficiency
	    */
        double meanRate = (db.getAverageRatingForMovie(movie1)
                            + db.getAverageRatingForMovie(movie2))/2;
        double meanUser = 0;
        double sdRate = 0;
        
        for (Pair p : targetV)         
        {
        	meanUser += p.a;
            sdRate += ( ((double)p.b - meanRate)*((double)p.b - meanRate) );
        }
        
        meanUser /= (targetV.size()-1);
        sdRate = Math.sqrt( sdRate/(targetV.size() - 1) );
	    
        //now find standard deviation for userIDs
        double sdUser = 0;
        
        for (Pair p: targetV)
        	sdUser += ( ((double)p.a - meanUser)*((double)p.a - meanUser) );
        sdUser = Math.sqrt( sdUser/(targetV.size() - 1) );
         
	    //find correlation between user and rating
        double r = 0;
        
        for (Pair p : targetV) 
        	r += ( ((p.a - meanUser)/sdUser) * ((p.b - meanRate)/sdRate) );
        r /= (targetV.size() - 1);

        //calculate the coefficients
        double c2 = r * (sdUser/sdRate);
        double c1 = meanUser - c2*meanRate;

        //TODO: calculate error for epsilon
        
	    //assemble formula and use model to predict rating for user on movie2
        predicted = (int) Math.round(c1 + c2*movie2);
        
	    return predicted;
    }	   
    
/************************************************************************************************************************/
    
    public void open()    
    {
    	db.openConnection();
    }
    
 /************************************************************************************************************************/
    
    public void close()    
    {
    	db.closeConnection();
    }

 /************************************************************************************************************************/
    
    public static void main (String[] args)    
    {
       ItemItemRecommenderWithK 	rec;
       MemHelper 					h;
       Timer227 					time	= new Timer227();
       double 						mae=0,rmse; 
       
       String whichScheme [] = {"Demo(I)             	",
    		   					"Demo(D)             	",
    		   					"Rating(I)           	",
    		   					"Rating(D)           	",
    		   					"Demo (I) + Rating (I)	", 
    		   					"Demo (D) + Rating (D)  ",
    		   					"Demo (I) * Rating (I)	", 
    		   					"Demo (I) * rating (D)	", 
    		   					"Rating            		" 
    		   					};
              
   

       //------------------------------------------------------------ 
       // Start Five Fold
       //------------------------------------------------------------
           
      for (int i=1; i<=1; i++) //for five fold   (i=1, 5)    
       {
    	     	  
    	  System.out.println(" Currently at fold ="+ (i));
    	  
    	   //pass parameters to open the databaseImpl object    	   
           rec = new ItemItemRecommenderWithK("movielens", "sml_ratings", "sml_movies", 
        		   						      "sml_averages", 
        		   						      "sml_SimFold"+(i), true); //every time, different sim table
           
                
          //---------------------- 
          //SML
          //----------------------
/*           
          //create training set which is stored in memory (for fast processing), each time different         
             rec.myTrainingSet = new MemHelper (rec.myPath + "sml_trainSetStoredFold" + (i) + ".dat");         
           
          // user-rating item sim (Pearson or Adjusted Cosine)
             //rec.myStoredSim = UserSimKeeper.deserialize(rec.myRSimPath + "\\StoredSimP\\SimFold" + (i)+ ".dat" );
             rec.myStoredRSim = UserSimKeeper.deserialize(rec.myPath + "\\StoredRCSim\\SimFold" + (i)+ ".dat" );
      
          // demo sim 
             rec.myStoredDSim = UserSimKeeper.deserialize(rec.myPath + "\\StoredDSim\\SimFold" + (i)+ ".dat" );
             
          //Feature Sim
             rec.myStoredFSim = UserSimKeeper.deserialize(rec.myPath + "\\StoredFSim\\SimFold" + (i)+ ".dat" );
             */
           
          // For simple 20-80, we need test object 
          // MemHelper test set
            // h = new MemHelper(rec.myPath+ "sml_testSetStoredFold" + (i) + ".dat");
               h = new MemHelper(rec.myPath+ "sml_clusteringTestSetStoredTF.dat");
             
             //---------------------- 
             //ML
             //----------------------
                
          // user-rating item sim (Pearson or Adjusted Cosine)
   //        rec.myTrainingSet = new MemHelper (rec.myPath + "ml_trainSetStoredFold" + (i) + ".dat");         
    //       rec.myStoredSim = UserSimKeeper.deserialize(rec.myPath + "\\StoredSimC\\SimFold" + (i)+ ".dat" );
           //rec.myStoredSim = UserSimKeeper.deserialize(rec.myPath + "\\StoredSimP\\SimFold" + (i)+ ".dat" );
             
            // demo sim 
             
             //MemHelper
             //h = new MemHelper(rec.myPath+ "ml_testSetStoredFold" + (i) + ".dat");
          

             //-----------------------
             //Open File To write
             //-----------------------
             
           //open the connection with databaseImlp, result files
           if(i==1)    {	
           				//	  rec.openFile(5, 2);  //demo
           				//	  rec.openFile(5, 3);  //rating
           				//	  rec.openFile(5, 4);  //both
           					  rec.openSingleFile();
           			   }
          

           //-----------------------
           //Timers
           //-----------------------
           
           System.out.println("Ready to start recommendations.");
           time.resetTimer();
           time.start();
           

           //-----------------------
           // Go throug all Neighbours
           //-----------------------
           
           //we have to make loop here
        for (int myK=5;myK<rec.totalK;myK+=rec.incrementInK)    
        {
        	for(int comb=1 ; comb<=1 ; comb++)		
        	{
        	  time.resetTimer();
              time.start();
                
           
             /*  for(int d1=0; d1<=10;)              
		        { 
            	   d1+=1;						//to break this loop
            	  for(int d2=0; d2<=10;)
            	  {
            		  d2+=1;
            		  for (int d3=0;d3<=10;)
            		  {
            			  d3+=1;
            			  
            			  if(comb==10) {            				   
            				  			if (d1+d2+d3 != 10) continue;
            			    			}
            			  
            			  							
            			  else {d1+=11; d2+=11; d3+=11; }
            		*/
            		  //{d1+=1.1; d2+=1.1; d3+=1.1 }
            		  
		               //Call Method for recommendation
				
		        	  	// mae = rec.GoTroughTestSet(h,myK,comb,(d1 *1.0)/10, (d2*1.0)/10, (d3*1.0)/10); 
              				
              			 mae = rec.GoTroughTestSet(h,myK,comb,0,0,0);
					     time.stop();
			           
		           
		           //-----------------------
		           //  Write Results into File
		           //-----------------------
		           
		           
		           		try {
					       		
		           		/*	if (comb==1)		       		
				       		{
				       			rec.writeDemoData[i-1].write(myK + "\t" + mae + "\t" + time.getTime()); //K, mae,time 
				       			rec.writeDemoData[i-1].newLine();
				       		}
				       		
		           			else if (comb==2)		       		
					       		{
					       			rec.writeRatingData[i-1].write(myK + "\t" + mae + "\t" + time.getTime()); //K, mae,time 
					       			rec.writeRatingData[i-1].newLine();
					       		}
					       	
		           			else          						       		
					       		{
					       			rec.writeDemoAndRatingData[i-1].write(myK + "\t" + mae + "\t" + time.getTime()); //K, mae,time 
					       			rec.writeDemoAndRatingData[i-1].newLine();
					       		}		       		
		           			*/
		           			
				    	    }
		           		
				    	catch (Exception E)
				          {
				       	    	System.out.println("error writing the file pointer of rec");
				       	    	E.printStackTrace();
				       	    	//System.exit(1);
				          }
				    
				    	// System.out.println( whichScheme[comb-1] +"\t\t: mae " + mae + ", with k =" + myK +
				    	//		", -ve answers =" + rec.totalNegativeAnswers3 + ", zero answers =" + rec.totalZeroAnswers3);
				    	//System.out.print(" demo ="+d1 + ", rating=" +d2 + "," + "feature =" + d3);
				    	System.out.println(rec.infoAbtComb);
				    	System.out.println(" mae " + mae + ", with k =" + myK +
						    		", -ve answers =" + rec.totalNegativeAnswers +
						    		", zero answers =" + rec.totalZeroAnswers);
				    	System.out.println("sensitivity =" + rec.rmse.getSensitivity() + ", Coverage ="+ rec.rmse.getItemCoverage());
				    			
				            	
				    	rec.totalNegativeAnswers=0; 
				      	rec.totalZeroAnswers=0;
            		  
            	/*	  		} //end of d3 for
            	  		}//end of d2 for
		              }//end of d1 for
              */
            
                  }//end of combination for
        	System.out.println();
         }//end of K for  
        
        
        //-----------------------
        // Close Files
        //-----------------------
        
     /*   if(i==5)  //write only 1 fold
        {
        	   		    rec.closeFile(5,2); //close demo files
    	   				rec.closeFile(5,3); //close rating files
    	   				rec.closeFile(5,4); //close demo and rating files
           			}*/
          
        if(i==1) rec.closeSingleFile();
        
           System.gc();
        }//end of folds for 
      
      //-------------------------------------
      
      
    }//end of main function

 /************************************************************************************************************************/
 
    /**
     * @param Memhelper Object, How much Neighbours
     * @return MAE
     */
    
    public double GoTroughTestSet(MemHelper testmh, int myNeighbours, int comb, double rW, double dW, double fW)     
    {
        rmse = new RMSECalculator();
        
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
            	   
           
           // if(movies.size()>=20)
            {
            	//-----------------------
                // Movies seen by a user
                //-----------------------
                
                for (int j = 0; j < movies.size(); j++)     
                {
                  mid = MemHelper.parseUserOrMovie(movies.getQuick(j));   
                  total++;
                  
/*                  double rrr = weightedSumHybrid  ( mid, 
                  									uid, 
                  									myNeighbours, 
                  									comb, 
                  									rW,
                  									dW,
                  									fW
                  									);*/
                
                
                  
                  									
        /*          double rrr = weightedSumRatingAndThenDemo  ( mid, 
                		   									   uid, 
                		   									   myNeighbours, 
                		   									   comb,				// which combination 
                		   									   rW, 					// coefficient for demo corr (0-1)
                		   									   dW);			// coefficient for rating corr (0-1)
                
                   
                  */
                  
                  double rrr = weightedSum(mid, uid, myNeighbours);
                  
                  //-----------------------
                  // Add error
                  //-----------------------
                   
                  //System.out.println("  uid, mid, ratingP, ratingA" + uid + "," + mid + ","+ rrr +","+ myRating);
                  
                 // if(rrr!=0) //challenge sparsity (Active user has not rated any similar movie)
                  
                /*  if(rrr<0) rrr=0;
                  if(rrr>5) rrr=5;
                */  
                
                //  if(!(rrr>0 || rrr<=0)) System.out.println("rrr error= "+rrr);
                 //  else System.out.println("rrr ok= "+rrr);
                  
                // if(rrr>0 )
                  {               	
                			myRating = testmh.getRating(uid, mid);			 		// get actual ratings?

                            if (myRating==-99 )                           
                               System.out.println(" rating error, uid, mid, ratingP" + uid + "," + mid + ","+ myRating);
                           
                            rmse.add(myRating,rrr);		   							 // get prediction for these users ....from where it is calling it?
                            rmse.addCoverage(rrr);
                          //  rmse.ROC4(myRating, rrr, 5, );
                           /* System.out.println("=====================================================");
                            System.out.println(" error is = (actual - predicted=" + myRating + "-" + rrr);
                            System.out.println("=====================================================");
                           */     
                   }
                }//end of all movies for
            }//filter >20 movies 
            }//end of all users for
        

        System.out.println(", Avg neighbours found  =" + howMuchNeighboursReallyFound * 1.0/total);
        howMuchNeighboursReallyFound = 0;
                
        //  double dd= rmse.rmse();
        double dd= rmse.mae();
      
        return dd;
    }
    
/*****************************************************************************************************/
    
//File writing etc

    public void openFile(int howManyFiles, int which)    
    {

     
   	 try {

   		 for(int i=0;i<5;i++)
   		 { 
   			if(which==1)  //general
   				writeData[i] = new BufferedWriter(new FileWriter(myPath + "Results\\ResultG" + (i+1)+ ".dat", true));
   			
   			else if (which==2) //demo
   				writeDemoData[i] = new BufferedWriter(new FileWriter(myPath + "Results\\ResultD" + (i+1)+ ".dat", true));
   		
   			else if (which==3) //rating
   				writeRatingData[i] = new BufferedWriter(new FileWriter(myPath + "Results\\ResultR" + (i+1)+ ".dat", true));
   		
   			else  //demo and rating both
   				writeDemoAndRatingData[i] = new BufferedWriter(new FileWriter(myPath + "Results\\ResultDR" + (i+1)+ ".dat", true));
   		 
   		 
   		 }
   			
   	   }
        
        catch (Exception E)
        {
      	  System.out.println("error opening the file pointer of rec");
      	  System.exit(1);
        }
        
        System.out.println("Rec File Created");
    }
    
    //----------------------------
    

    public void closeFile(int howMany, int which)    
    {
    
   	 try {
   		    for(int i=0;i<5;i++)
   		    	
   		 	    {
   		    	
   		    	if (which==1)
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
   		writeWeights = new BufferedWriter(new FileWriter(myPath + "Results\\Weights.dat" , true));

   			
   	   }
        
        catch (Exception E)
        {
      	  System.out.println("error opening the file pointer of rec");
      	 //System.exit(1);
        }
        
        System.out.println("Rec File Created");
    }
    
    //------------------------------------
    
    public void closeSingleFile()    
    {
    
   	 try {
   		    writeWeights.close();
   		 	    
   	 	}
   	 
        catch (Exception E)
        {
      	  System.out.println("error closing the roc file pointer");
        }
        
    }
    
       
}
	
//--------------------------------------------------------------------------------------------
// Adj cosine sim: [-1,+1]...here 20-80 case
// 1- taste approach --> 0.76 (25), 0.77 (15)
/*, Avg neighbours found  =4.993523379618272

mae 0.8451681145457492, with k =5, -ve answers =0, zero answers =0
sensitivity =0.34976670481556477, Coverage =100.0

, Avg neighbours found  =14.979686963348216

mae 0.7774957033154798, with k =15, -ve answers =0, zero answers =0
sensitivity =0.33788185579716523, Coverage =100.0

, Avg neighbours found  =24.638879348412736

mae 0.7656571477588493, with k =25, -ve answers =0, zero answers =0
sensitivity =0.3325997006778766, Coverage =100.0

, Avg neighbours found  =33.80589764977184

mae 0.7632855127660246, with k =35, -ve answers =0, zero answers =0
sensitivity =0.32071485165947705, Coverage =100.0

, Avg neighbours found  =42.52073009175212

mae 0.7625424207460272, with k =45, -ve answers =0, zero answers =0
sensitivity =0.3118232238753411, Coverage =100.0

, Avg neighbours found  =50.77312202541583

mae 0.7638595116859677, with k =55, -ve answers =0, zero answers =0
sensitivity =0.30231534466062154, Coverage =100.0
*/

//  2- only take pos  --> 
/*, Avg neighbours found  =4.991560767381385

mae 0.8516542572977238, with k =5, -ve answers =0, zero answers =0
sensitivity =0.3485342019543974, Coverage =100.0

, Avg neighbours found  =14.69290025023306

mae 0.782154105803674, with k =15, -ve answers =0, zero answers =0
sensitivity =0.3383220353904393, Coverage =100.0

, Avg neighbours found  =23.579510328246897

mae 0.7671072388245515, with k =25, -ve answers =0, zero answers =0
sensitivity =0.33973061008891625, Coverage =100.0

, Avg neighbours found  =31.726460919483834

mae 0.7614872442895514, with k =35, -ve answers =0, zero answers =0
sensitivity =0.3350647064002113, Coverage =100.0

, Avg neighbours found  =39.16333840341495

mae 0.7579805816770879, with k =45, -ve answers =0, zero answers =0
sensitivity =0.3348005986442468, Coverage =100.0

, Avg neighbours found  =45.88665914331976

mae 0.7563966956282114, with k =55, -ve answers =0, zero answers =0
sensitivity =0.3338322035390439, Coverage =100.0*/


// simple all weight --> weightSum+=weights
/*, Avg neighbours found  =4.993523379618272

mae 0.8516592729102128, with k =5, -ve answers =0, zero answers =0
sensitivity =0.3486222378730522, Coverage =100.0

, Avg neighbours found  =14.979686963348216

mae 0.8172845624655422, with k =15, -ve answers =0, zero answers =0
sensitivity =0.33973061008891625, Coverage =100.0

, Avg neighbours found  =24.638879348412736

mae 7.098888652970747, with k =25, -ve answers =0, zero answers =0
sensitivity =0.34800598644246855, Coverage =99.99509346940778

, Avg neighbours found  =33.80589764977184

mae 1.4265247980482298, with k =35, -ve answers =0, zero answers =0
sensitivity =0.3495025970596003, Coverage =100.0

, Avg neighbours found  =42.52073009175212

mae 1.5078962920286518, with k =45, -ve answers =0, zero answers =0
sensitivity =0.3586583325997007, Coverage =100.0

, Avg neighbours found  =50.77312202541583

mae 1.6698757693856578, with k =55, -ve answers =0, zero answers =0
sensitivity =0.36737388854652697, Coverage =100.0

*/

// simple all weight --> weightSum+=Math.abs(weights)


/*mae 0.851848214114592, with k =5, -ve answers =0, zero answers =0
sensitivity =0.3484461660357426, Coverage =100.0

, Avg neighbours found  =14.979686963348216

mae 0.8129640695112152, with k =15, -ve answers =0, zero answers =0
sensitivity =0.32221146227660885, Coverage =100.0

, Avg neighbours found  =24.638879348412736

mae 0.8631540487175524, with k =25, -ve answers =0, zero answers =0
sensitivity =0.30029051853156086, Coverage =99.99509346940778

, Avg neighbours found  =33.80589764977184

mae 0.9220657949277263, with k =35, -ve answers =0, zero answers =0
sensitivity =0.2719429527247117, Coverage =100.0

, Avg neighbours found  =42.52073009175212

mae 0.9786063227537048, with k =45, -ve answers =0, zero answers =0
sensitivity =0.2515186195967955, Coverage =100.0

, Avg neighbours found  =50.77312202541583

mae 1.0294034357329143, with k =55, -ve answers =0, zero answers =0
sensitivity =0.23355929219121402, Coverage =100.0

error closing the roc file pointer
*/
