
package netflix.memreader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import cern.colt.list.LongArrayList;
import cern.colt.map.OpenIntObjectHashMap;

/**
 * It will take training set (let us suppose 80% of dataset) and then perform 10 fold cross validation.
 * This validation will be used to determine the sensitivity of the parameters.
 * 
 *  So take training set, divide it into 10 non-overlapping chunks, (1....10) 
 *  loop1: Take 1 out as test set and remiang as train set....write them with same index
 *  loop2: Take 2 as ....
 *  
 *   
 *  loop10: write all, 
 * 
 * @author Musi
 *
 */


public class FiveFoldDataRatingGenerator
{
	
private int 			nFold;
private int 			totalFolds;

BufferedWriter 			outT[];
BufferedWriter 			outTr[];

private String        	myPath;
private Random 			rand;
private IntArrayList	subsetOfUsers;


 MemHelper 		mainMh; 
//private MemHelper myMMh[];


  

  
 /*************************************************************************************************/
  
  public FiveFoldDataRatingGenerator()
  
  {
	  
  }
  
  
  /*************************************************************************************************/
  

  public FiveFoldDataRatingGenerator  (    String myMh,					//main memHelper object
		  								   String myP,					//path
		  									int nValidation				//folds
		  						           
		  						       )  
  {
	  
	    mainMh 				 = new MemHelper(myMh);
	    totalFolds 			 = nValidation;
	    myPath				 = myP;
	    rand 				 = new Random();
	    

	    outTr= new BufferedWriter[totalFolds];
	    outT= new BufferedWriter[totalFolds];
	    
  }
  
 /************************************************************************************************/
   
  public void doCrossValidation(boolean smallOrBig)
  {							

	  int uid=0,mid=0, rating=0;
	  int del = 0;
	  int breakIt =0;
	  int testSize =0, trainSize=0;
	  
	  IntArrayList allUsers = mainMh.getListOfUsers();
	  int totalUsers = allUsers.size();
	  
	  //---------------------------
	  // Set variable for each fold
	  //---------------------------
	    
	  IntArrayList userFolds[] = new IntArrayList[totalFolds];  				//one for each fold
	  IntArrayList movieFolds[] = new IntArrayList[totalFolds];  				//one for each fold
	  IntArrayList ratingFolds[] = new IntArrayList[totalFolds];  		//one for each fold
	  
	  IntArrayList testMovies = new IntArrayList();
	  IntArrayList alreadyProcessed = new IntArrayList();
	  
	  
	  for (int i=0;i<totalFolds;i++)
		  {
		  userFolds[i] = new IntArrayList();
		  movieFolds[i] = new IntArrayList();
		  ratingFolds[i] = new IntArrayList();
		  
		  }
	  

	  //--------------------------------------------------------
	  // start loop, generate a movie and write it
	  //--------------------------------------------------------
 
	  int foldIndex=0;
		  
	  for (int i=0;i<totalUsers;i++)
	  {
		    uid = allUsers.getQuick(i);
		    IntArrayList userMovies= mainMh.getMoviesSeenByUser(uid);
		    int mySize = userMovies.size();
		 	
		    trainSize = (int) ((0.8) * mySize);
		    if (trainSize ==0) trainSize = 1;
		    testSize = mySize - trainSize;
		    
	
		  //--------------------------------
		  // Go through all movies of an 
		  // active user, and write
		  //--------------------------------
		 
		 
		 for (int j=0;j< mySize; j++)
		 {			 
			    mid = MemHelper.parseUserOrMovie(userMovies.getQuick(j)); //get a parsed movie			    
			    rating = mainMh.getRating(uid, mid);					  //get rating
			    		 
   				if(foldIndex == totalFolds) foldIndex=0;			//if 5-->0
		   		    
	   			 		userFolds[foldIndex].add(uid);				//write in separate folds each time
		   		    	movieFolds[foldIndex].add(mid);
		   		    	ratingFolds[foldIndex].add(rating);
		   		    				
	   			 		foldIndex++;							    //0,1,2,3,4,5
		   			 		
		   	} //if we have not add this user before
		   	 			 	
   			
   		 }// end of all users for
			        
  
       // System.out.println("-----------------done folding---------------------");
    

	  //--------------------------------------------------
	  // We have five folds now, make 4 folds combined as
	  // a train object and remaining as test object
	  //--------------------------------------------------
	 
	  
    	//Now write these folds into a file    	
        String testSample="", trainSample="";
    
    
        
        try {
        	
        //create buffers
    	for (int i=0; i<totalFolds;i++)    		
    	{
    	
    		outT[i]  = new BufferedWriter(new FileWriter(myPath + "\\myFData\\" + "sml_testSetFold" + (i+1) + ".dat"));
    		outTr[i] = new BufferedWriter(new FileWriter(myPath + "\\myFData\\" + "sml_trainSetFold" + (i+1) + ".dat"));
    	}
    	
    	// write into test and train
    	for (int i=0; i<totalFolds;i++)
    	{
    	   for (int j=0; j<totalFolds; j++)
    		{
    	  
    		    if(j==i) 
    		    {    		    		
    		    	for (int k=0;k<userFolds[i].size();k++) //user size in a fold
    		    	{
    		    		uid = userFolds[j].getQuick(k);
    		    		mid = movieFolds[j].getQuick(k);
    		    		rating = ratingFolds[j].getQuick(k);
    		    		
    		    		testSample = uid + "," + mid + "," + rating;
    		    	//	System.out.println(testSample);
    		    	    outT[i].write(testSample);
    		    	    outT[i].newLine();
    	
    		           }
    		    	} //end of test 
    		    
    		    else 
    		    {

    		    	for (int k=0;k<userFolds[j].size();k++) //all users in that fold
    		    	{
    		    		uid = userFolds[j].getQuick(k);
    		    		mid = movieFolds[j].getQuick(k);
    		    		rating = ratingFolds[j].getQuick(k);
    		    		
    		    		testSample = uid + "," + mid + "," + rating;
    		    	    outTr[i].write(testSample);
    		    	    outTr[i].newLine();
    		    	}  
    
    		       } //end of trains
    		    
    		   } //end of for
    	
    	   outT[i].close();
    	   outTr[i].close();
    	 
    	   }  	
    	   	
       }//end of try
    	 
        
                
      catch (IOException e){
		  System.out.println("Write error!  Java error: " + e);
		  System.exit(1);

	    } //end of try-catch     

   
          
      //-------------------------------------------------------------------------------
      // Write into memory 
     //--------------------------------------------------------------------------------

      System.out.println("-----------------done simple ewriting---------------------"); 
      
      
      MemReader myR = new MemReader();
    
      
      for (int i=0; i<totalFolds;i++)
  	{
  	
  		if (smallOrBig ==true)
  			
  			{	myR.writeIntoDisk((myPath + "\\myFData\\" + "sml_testSetFold" + (i+1) + ".dat"), (myPath + "\\myFData\\" + "sml_testSetStoredFold" + (i+1) + ".dat")); 				
  				myR.writeIntoDisk((myPath + "\\myFData\\" + "sml_trainSetFold" + (i+1) + ".dat"), (myPath + "\\myFData\\" + "sml_trainSetStoredFold" + (i+1) + ".dat"));  				
  			
  				System.gc();
  			}
  		
  		else
  			
			{	myR.writeIntoDisk((myPath + "\\myFData\\" + "ml_testSetFold" + (i+1) + ".dat"), (myPath + "\\myFData\\" + "ml_testSetStoredFold" + (i+1) + ".dat"));
				myR.writeIntoDisk((myPath + "\\myFData\\" + "ml_trainSetFold" + (i+1) + ".dat"), (myPath + "\\myFData\\" + "ml_trainSetStoredFold" + (i+1) + ".dat"));  		   
			}
		
  	}
      
      
      
      
      //-------------------------------------------------------------------------------
      //Check by deserializing, how many ratings a test and train fold contains
     //--------------------------------------------------------------------------------
      /*
   
      for (int i=0;i<5;i++)
      {
      	MemHelper mTest = new MemHelper (myPath +  "\\myFData\\" + "sml_testSetStoredFold" + (i+1) + ".dat");
      	MemHelper mTrain = new MemHelper (myPath +  "\\myFData\\" + "sml_trainSetStoredFold" + (i+1) + ".dat");
      	
      	System.out.println("Train FOld" + (i+1) + " Users:" + mTrain.getNumberOfUsers()); 	
      	System.out.println("Train FOld" + (i+1) + " Movies:" + mTrain.getNumberOfMovies());
      	System.out.println("Train FOld" + (i+1) + " Ratings:" + mTrain.getAllRatingsInDB());
      	System.out.println("---------------------------------------------------------------");
      	System.out.println("Test FOld" + (i+1) + " Users:" + mTest.getNumberOfUsers()); 	
      	System.out.println("Test FOld" + (i+1) + " Movies:" + mTest.getNumberOfMovies());
      	System.out.println("Test FOld" + (i+1) + " Ratings:" + mTest.getAllRatingsInDB());
      	
      }
  
*/
      }//end of function
    
/************************************************************************************************/

 public static void main(String arg[])
  
  {
	  
	  System.out.println(" Going to divide data into K folds");
	  int folding;
	  
	  //for (int i=0;i<folding;i++) //for test factor
	
	  {
	  // sml (for validation)
	  
	  //we have training data across each fold now and we want to convert that into valdiation and training data 
	  // wanna check against which test and train (bigget	  
	   
	/*	
	    folding = 10;
	    String m = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\TestTrain\\TenFoldData\\sml_storedRatings.dat";
	    String p  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\TestTrain\\TenFoldData\\";
	  
	  */
		  
		  
		  
	    folding = 5;
	    String m = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\TestTrain\\FiveFoldData\\sml_storedFeaturesRatings.dat";
	    String p  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\TestTrain\\FiveFoldData\\";
    
	
		  
	  
	  	    	
	   	//ML
	    	
	  //  	String m = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\TestTrain\\FiveFoldData\\ml_storedRatings.dat";
	  //  	String p  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\TestTrain\\FiveFoldData\\";
	   
	    	
	    FiveFoldDataRatingGenerator dis= 
	    			new FiveFoldDataRatingGenerator( m,						//main 
			  									     p, 					//train
			  									     folding				//5 fold, 10 etc
	    											);
	  
	  
	    dis.doCrossValidation(true);   //false = big
	  
	  }
	  
	   
	  System.out.println(" Done ");
	  
  }

 
  
  
  

  }
