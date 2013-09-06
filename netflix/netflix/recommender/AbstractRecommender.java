package netflix.recommender;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

import netflix.memreader.MemHelper;
import netflix.rmse.RMSECalculator;
import netflix.ui.Item;
import netflix.ui.ItemComparator;
import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import cern.colt.list.LongArrayList;
import cern.colt.map.OpenIntDoubleHashMap;

/**
 * An abstract class that is the basis for each recommender's actual run.
 * 
 * It includes one abstract function to implement - recommend(int, int, String).
 * 
 * Also, it includes some methods for adding rows to the database.
 * 
 * Note that in order for this to work, you must set MemHelper mh in
 * the extended classes' constructor.  Also, make sure to call resort()
 * after adding entries, so that the underlying database is sorted.
 * 
 * There are also a few helpful methods that apply to all recommenders.
 * 
 * @author lewda
 */

/******************************************************************************************************/
public abstract class AbstractRecommender 
/******************************************************************************************************/
{

    //The underlying database
    protected MemHelper mh;
    
    /**
     * Recommends a rating based on a uid and mid.
     * 
     * @param uid the user id
     * @param mid the movie id
     * @param date the date
     * @return a rating
     */
    public abstract double recommend(int uid, int mid, String date);

    /**
     * Recommends a rating based on a uid and mid.
     * 
     * @param uid the user id
     * @param mid the movie id
     * @param date the date
     * @return a rating
     */
    public abstract double recommend(int uid, int mid, int neighbours);

    
    // Variables for Error etc
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
        
    
 /**********************************************************************************************************/
 
    /**
     * Adds an entry to the database.
     * 
     * Be sure to call resort() after adding entries.
     * 
     * @param uid the user id
     * @param mid the movie id
     * @param rating the rating
     * @return true if successful, false if parameters were bad
     */
    
    public boolean add(int uid, int mid, int rating)     
    { 
        if(uid >= 0 && mid >= 0 && mid < Short.MAX_VALUE && rating >= 1 && rating <= 5)         
        {
            mh.getMemReader().addToCust((short)mid, uid, (byte)rating);
            mh.getMemReader().addToMovies((short)mid, uid, (byte)rating);
            
            return true;
        }
        
        return false;
    }
    
 /**********************************************************************************************************/
 
    /**
     * Resorts the values in the underlying database.
     * It is important to call this after adding entries.
     */
    
    public void resort()     
    {
        mh.getMemReader().sortHashes();
    }
    
/**********************************************************************************************************/

    /**
     * Given a user id, it finds what movies the user 
     * has *not* seen from among all the movies.
     * 
     * @param sid the user id
     * @return the movies the user has not rated
     */
    
    public ArrayList<Item> getUnratedMovies(int uid)    
    {
        ArrayList<Item> toTest = new ArrayList<Item>();			//making array of class objects
    
        IntArrayList movies = mh.getListOfMovies();
        
        
        for (int i = 0; i < movies.size(); i++) //for all movies (size)        
        {
            toTest.add(new Item(Integer.toString(movies.getQuick(i)), "", 0)); //new item with mid=given, des="", rating=0
        }
        
        
        
        return getUnratedMovies(uid, toTest);	//Now call another function, which tells which of he movies user has not seen
        
    }
    
/**********************************************************************************************************/

    /**
     * Given a user id and a list of movies, it takes
     * out those movies which have been rated by
     * the user.  Non-volatile to parameters.
     * 
     * @param uid the user id
     * @param movies a list of movies to test
     * @return all movies the user has *not* rated from the list
     */
    
   public ArrayList<Item> getUnratedMovies(int uid, ArrayList<Item> movies)     
    {
        ArrayList<Item> unrated = new ArrayList<Item>();
        
        for(Item i : movies)         
        {
            if(mh.getRating(uid, i.getIdAsInt()) < 0)  //IT CHCK IF CustToMovie (uid) && MovieToCust(mid) 
            { 										   //If customer has not rated it, it will return -99	
           
                unrated.add(i);		
            }
        }
        
        return unrated;
    }
    
 /**********************************************************************************************************/
 
    /**
     * Takes in a list of Items (as movies) and ranks them using
     * the recommender system.  Note that it ranks the movies in
     * the original ArrayList, so the old ratings are destroyed and
     * a new ordering is imposed on movies.
     * 
     * @param uid the user to rank the movies for
     * @param movies the movies to rank
     */
    
    public void rankMovies(int uid, ArrayList<Item> movies)     
    {
        for (Item m : movies)
            m.setRating(recommend(uid, Integer.parseInt(m.getId()), ""));

        Collections.sort(movies, new ItemComparator());
    }
    
 /**********************************************************************************************************/
 
    /**
     * Given an input file of data, will output properly formatted results.
     * This should only be used for Netflixprize entries.
     * 
     * Input should be formatted thus:
     * mid:
     * uid,date
     * uid,date
     * ...
     * 
     * Output should be formatted thus:
     * mid:
     * rating
     * rating
     * ...
     * 
     * @param MemHelper object, which has the probe ratings as well
     * @param inFile the name of the input file
     * @param outFile, write predictions to the output according to the format
     * 
     */
    
    public void recommendFile(MemHelper testMMh, String inFile, String outFile)    
    {
        File in = new File(inFile);			//for netflix this is a qulaifyiing file
        
        Scanner sc = null;
        BufferedWriter out;
        String currLine;
        String[] split;
        int currMovie = 0;

        try {
               sc = new Scanner(in);		//new scanner of the file we want to read
           }
       
        catch (FileNotFoundException e) 
        
        {
            System.out.println("Infile error, file not found!  Java error: "
                    + e);
            return;
        }

        try         
        {
            out = new BufferedWriter(new FileWriter(outFile));	// we wanna write in o/p file

            while (sc.hasNextLine())            
            {
                currLine = sc.nextLine().trim();
                
                split = currLine.split(",");

                if (split.length == 1)                
                {
                    currMovie = Integer.parseInt(currLine.substring(0, currLine.length() - 1));

                    out.write(currLine);		//write in the o/p file as a movies
                }
                
                else                 
                {
                	
                	int uid = Integer.parseInt(split[0]);
                	
                	double myPrediction= recommend(uid, currMovie, split[1]);
                	double myActual = testMMh.getRating(uid, currMovie);
                    out.write(  Double.toString( myPrediction));
                    
                	
                	//I must have the MemReader object, that have the rating for this
                	
                
                	
                
                       		
                    
                         
                }

                out.newLine(); //go to next line
            }

            out.close();
        }
        
        catch (IOException e) {
            System.out.println("Write error!  Java error: " + e);
            System.exit(1);
        }
    }

 /**********************************************************************************************************/
 
    /**
     *  
     *@author Musi
     *@param  MemHelper TestMMh
     *@param  Memhelper Training MMh
     *@param  int no. of classes
     *@param  int neighbourhood size
     */

    public void testWithMemHelper(MemHelper testmh, MemHelper trainMMh, 
    							  int myClasses,	int neighbours)     
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
  	           // System.out.println("now at " + i + " of total " + totalUsers );
  	            
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

    
 /**********************************************************************************************************/
 
    /**
     * Stub so one can test without having to initialize
     * their own MemHelper object.
     * @param testFile the MemHelper file
     * @return its rmse in testing
     */
    
    //so here if we give this a path to our test file, should it will give us required results.
    
    public double testWithMemHelper(String testFile)     
    {
        MemHelper testmh = new MemHelper(testFile);
    
        return 0;
        
    }
    
 /**********************************************************************************************************/
 public void calculateErrors(double p, int uid, int mid)
 
 {
	 //Start up RMSE count
     RMSECalculator rmse 		= new RMSECalculator();
     RMSECalculator movrmse 	= new RMSECalculator();
     RMSECalculator usermse 	= new RMSECalculator();
     
//     movavg = mh.getAverageRatingForMovie(mid);					//prediction, user_avg, movie_avg
 //    useavg = mh.getAverageRatingForUser(uid);

     
     
	 
 }






}

