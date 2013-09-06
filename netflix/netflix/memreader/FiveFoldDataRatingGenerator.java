
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


 MemHelper 				mainMh; 
//private MemHelper myMMh[];

int 					minUsersAndMov;		//For FT1, and FT5
  

  
 /*************************************************************************************************/
  
  public FiveFoldDataRatingGenerator()
  
  {
	  
  }
  
  
  /*************************************************************************************************/
  

  public FiveFoldDataRatingGenerator  ( String myMh,				//main memHelper object
		  								String myP,					//path
		  								int nValidation,			//folds
		  								int minUsersAndM 		  						           
		  						       )  
  {
	  
	    mainMh 				 = new MemHelper(myMh);
	    totalFolds 			 = nValidation;
	    myPath				 = myP;
	    rand 				 = new Random();
	    minUsersAndMov  	 = minUsersAndM; 

	    outTr                = new BufferedWriter[totalFolds];
	    outT 				 = new BufferedWriter[totalFolds];
	    
  }
  
 /************************************************************************************************/
   
  public void doCrossValidation(int dataSetChoice, double xFactor)
  {							

	  int uid=0,mid=0; 
	  double rating=0;
	  int del = 0;
	  int breakIt =0;
	  int testSize =0, trainSize=0;
	  
	  IntArrayList allUsers = mainMh.getListOfUsers();
	  int totalUsers = allUsers.size();
	  
	  //---------------------------
	  // Set variable for each fold
	  //---------------------------
	    
	  IntArrayList userFolds[] 		= new IntArrayList[totalFolds];  				//one for each fold
	  IntArrayList movieFolds[] 	= new IntArrayList[totalFolds];  				//one for each fold
	  DoubleArrayList ratingFolds[] = new DoubleArrayList[totalFolds];  		//one for each fold
	  
	  IntArrayList testMovies 		= new IntArrayList();
	  IntArrayList alreadyProcessed = new IntArrayList();
	  
	  
	  for (int i=0;i<totalFolds;i++)
	  {
		  userFolds[i]    = new IntArrayList();
		  movieFolds[i]   = new IntArrayList();
		  ratingFolds[i]  = new DoubleArrayList();
		  
	  }	  

	  //--------------------------------------------------------
	  // start loop, generate a movie and write it
	  //--------------------------------------------------------
 
	  int foldIndex=0;
		  
	  for (int i=0;i<totalUsers;i++)
	  {
		    uid = allUsers.getQuick(i);
		    LongArrayList userMovies= mainMh.getMoviesSeenByUser(uid);
		    int mySize = userMovies.size();
		 	
		    trainSize = (int) ((xFactor) * mySize);
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
   		 } // end of all users for
			        
  
       // System.out.println("-----------------done folding---------------------");
    

	  //--------------------------------------------------
	  // We have five folds now, make 4 folds combined as
	  // a train object and remaining as test object
	  //--------------------------------------------------
	 
	  
    	//Now write these folds into a file    	
        String testSample="", trainSample="";
        IntArrayList verifyThatThereAreTotalMovsInAllFold  = new IntArrayList();
    
        
        try {
        	
        //create buffers
        if(dataSetChoice == 0)
        {
	    	for (int i=0; i<totalFolds;i++)    		
	    	{
	    	
	    		outT[i]  = new BufferedWriter(new FileWriter(myPath + "/" + "sml_testSetFold" + (i+1) + ".dat"));
	    		outTr[i] = new BufferedWriter(new FileWriter(myPath + "/" + "sml_trainSetFold" + (i+1) + ".dat"));
	    	}
        }
        
        else if(dataSetChoice == 1)
        {
        	for (int i=0; i<totalFolds;i++)    		
	    	{
	    	
	    		outT[i]  = new BufferedWriter(new FileWriter(myPath + "/" + "ml_testSetFold" + (i+1) + ".dat"));
	    		outTr[i] = new BufferedWriter(new FileWriter(myPath + "/" + "ml_trainSetFold" + (i+1) + ".dat"));
	    	}
        }
        
        else if(dataSetChoice == 2)
        {
        	for (int i=0; i<totalFolds;i++)    		
	    	{	    	
	    		outT[i]  = new BufferedWriter(new FileWriter(myPath + "/" + "ft_testSetFoldBoth" + minUsersAndMov + (i+1) + ".dat"));
	    		outTr[i] = new BufferedWriter(new FileWriter(myPath + "/" + "ft_trainSetFoldBoth" + minUsersAndMov+ (i+1) + ".dat"));
	    		
	    		
        
	    		
	    	}
        }
        
        
        	// write into test and train
    	for (int i=0; i<totalFolds;i++)
    	{
    	   verifyThatThereAreTotalMovsInAllFold.clear();
    		
    	   for (int j=0; j<totalFolds; j++)
    		{
    	  
    		    if(j==i) 
    		    {    		    		
    		    	for (int k=0;k<userFolds[j].size();k++) //user size in a fold
    		    	{
    		    		uid = userFolds[j].getQuick(k);
    		    		mid = movieFolds[j].getQuick(k);
    		    		rating = ratingFolds[j].getQuick(k);
    		    		
    		    		if(verifyThatThereAreTotalMovsInAllFold.contains(mid)==false)
    		    			verifyThatThereAreTotalMovsInAllFold.add(mid);
    		    		
    		    		testSample = uid + "," + mid + "," + rating;
    		    		// testSample = uid + "\t" + mid + "\t" + rating;
    		    	    if(rating ==0)	System.out.println(testSample);
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
    		    		
    		    		if(verifyThatThereAreTotalMovsInAllFold.contains(mid)==false)
    		    			verifyThatThereAreTotalMovsInAllFold.add(mid);
    		    		
    		    		testSample = uid + "," + mid + "," + rating;
    		    		//testSample = uid + "\t" + mid + "\t" + rating;
    		    		if(rating ==0)	System.out.println(testSample);
    		    	    outTr[i].write(testSample);
    		    	    outTr[i].newLine();
    		    	}  
    
    		       } //end of trains
    		    
    		   } //end of for
    	
    	   System.out.println("Size of mov found is ="+verifyThatThereAreTotalMovsInAllFold.size());
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

      System.out.println("-----------------done simple writing---------------------"); 
      
      
      MemReader myR = new MemReader();
    
      
      for (int i=0; i<totalFolds;i++)
  	{
  	
  		if (dataSetChoice == 0)
  			
  			{	myR.writeIntoDisk((myPath + "/" + "sml_testSetFold" + (i+1) + ".dat"), (myPath + "/" + "sml_testSetStoredFold" + (i+1) + ".dat"), true); 				
  				myR.writeIntoDisk((myPath + "/" + "sml_trainSetFold" + (i+1) + ".dat"), (myPath + "/" + "sml_trainSetStoredFold" + (i+1) + ".dat"), true);  				
  			
  				//Further divide it into two parts
  				SetDivision sd = new SetDivision(myPath + "/" + "sml_trainSetStoredFold" + (i+1) + ".dat" ,				//MemHelper, file to be divided
						  						 myPath + "/" + "sml_trainSetBuffFold" + (i+1) + ".dat" ,					//training buff
						  						 myPath + "/" + "sml_testSetBuffFold" + (i+1) + ".dat" ,					//test buff
						  						 myPath + "/" + "sml_trainAndTestSetBuffFold" + (i+1) + ".dat" ,			//trainingAndTest buff	
						  						 myPath + "/" + "sml_trainingTrainSetStoredFold" + (i+1) + ".dat" ,		//training set
						  						 myPath + "/" + "sml_trainingValSetStoredFold" + (i+1) + ".dat" ,          //test set
						  						 myPath + "/" + "sml_trainingTrainAndValSetStoredFold" + (i+1) + ".dat" ,  //trainingAndTest set
						  						 0,																			//no. movies		
  												 0,																			//no. users
  												 dataSetChoice);																		//dataset
  				
  				sd.divideIntoTestTrain(0.8, false);					
  												
  				
  				System.gc();
  			}
  		
  		else if (dataSetChoice == 1)
  			
			{	
  				myR.writeIntoDisk((myPath + "/" + "ml_testSetFold" + (i+1) + ".dat"), (myPath + "/" + "ml_testSetStoredFold" + (i+1) + ".dat"), true);
				myR.writeIntoDisk((myPath + "/" + "ml_trainSetFold" + (i+1) + ".dat"), (myPath + "/" + "ml_trainSetStoredFold" + (i+1) + ".dat"), true);  		   
				
				//Further divide it into two parts
  				SetDivision sd = new SetDivision(myPath + "/" + "ml_trainSetStoredFold" + (i+1) + ".dat" ,				//MemHelper, file to be divided
						  						 myPath + "/" + "ml_trainSetBuffFold" + (i+1) + ".dat" ,					//training buff
						  						 myPath + "/" + "ml_testSetBuffFold" + (i+1) + ".dat" ,					//test buff
						  						 myPath + "/" + "ml_trainAndTestSetBuffFold" + (i+1) + ".dat" ,			//trainingAndTest buff	
						  						 myPath + "/" + "ml_trainingTrainSetStoredFold" + (i+1) + ".dat" ,		//training set
						  						 myPath + "/" + "ml_trainingValSetStoredFold" + (i+1) + ".dat" ,          //test set
						  						 myPath + "/" + "ml_trainingTrainAndValSetStoredFold" + (i+1) + ".dat" ,  //trainingAndTest set
						  						 0,																			//no. movies		
  												 0,																			//no. users
  												 dataSetChoice);																		//dataset
  				
  				sd.divideIntoTestTrain(0.8, false);
  				
				System.gc();
			}
  		
  		else if (dataSetChoice == 2)
  		{
  				myR.writeIntoDisk((myPath + "/" + "ft_testSetFoldBoth" + minUsersAndMov+(i+1) + ".dat"), (myPath + "/" + "ft_testSetStoredBothFold" + minUsersAndMov+ (i+1) + ".dat"), true); 				
				myR.writeIntoDisk((myPath + "/" + "ft_trainSetFoldBoth" + minUsersAndMov+ (i+1) + ".dat"), (myPath + "/" + "ft_trainSetStoredBothFold" + minUsersAndMov+ (i+1) + ".dat"), true);  				
			
  			   //Further divide it into two parts
				SetDivision sd = new SetDivision(myPath + "/" + "ft_trainSetStoredBothFold" + minUsersAndMov+(i+1) + ".dat" ,				// MemHelper, file to be divided
					  						 myPath + "/" + "ft_trainSetBuffBothFold" + (i+1) + minUsersAndMov+ ".dat" ,					// training buff
					  						 myPath + "/" + "ft_testSetBuffBothFold" + (i+1) + minUsersAndMov+".dat" ,						// test buff
					  						 myPath + "/" + "ft_trainAndTestSetBuffBothFold" + minUsersAndMov+(i+1) + ".dat" ,				// trainingAndTest buff	
					  						 myPath + "/" + "ft_trainingTrainSetStoredBothFold" + minUsersAndMov+(i+1) + ".dat" ,			// training set
					  						 myPath + "/" + "ft_trainingValSetStoredBothFold" + minUsersAndMov+(i+1) + ".dat" ,          	// test set
					  						 myPath + "/" + "ft_trainingTrainAndValSetStoredBothFold" + minUsersAndMov+(i+1) + ".dat" ,  	// trainingAndTest set
					  						 0,																							// no. movies		
											 0,																							// no. users
											 dataSetChoice);																			// dataset
				
				 sd.divideIntoTestTrain(0.8, false);					
												
				
				System.gc();
			}
		
  		
		
  	}
      
      
      
      
      //-------------------------------------------------------------------------------
      //Check by deserializing, how many ratings a test and train fold contains
     //--------------------------------------------------------------------------------
      /*
   
      for (int i=0;i<5;i++)
      {
      	MemHelper mTest = new MemHelper (myPath +  "/myFData/" + "sml_testSetStoredFold" + (i+1) + ".dat");
      	MemHelper mTrain = new MemHelper (myPath +  "/myFData/" + "sml_trainSetStoredFold" + (i+1) + ".dat");
      	
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
	 	 	 
	  String 	m="", 	p="";
	  int folding 		= 5;
	  int dataSetChoice = 2;		//0 = sml, 1=ml, 2=ft
	  
	  System.out.println(" Going to divide data into K folds");
	  
	  //for (int i=0;i<folding;i++) //for test factor
	
	  {
	  // sml (for validation)
	  
	  //we have training data across each fold now and we want to convert that into valdiation and training data 
	  // wanna check against which test and train (bigget	  
	   
	/*	
	    folding = 10;
	    String m = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/TestTrain/TenFoldData/sml_storedRatings.dat";
	    String p  = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/TestTrain/TenFoldData/";
	  
	*/		  
		
		//sml
		if(dataSetChoice ==0)
		{
		    folding = 5;
		  /*  m = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/Clustering/FiveFoldData/sml_storedFeaturesRatingsTF.dat";
		    p  = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/Clustering/FiveFoldData/";
		    */
		    
	/*	    m = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/FiveFoldData/sml_storedFeaturesRatingsTF.dat";
		    p  = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/FiveFoldData/";*/
		    
		 
		   m = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/FiveFoldData/sml_storedFeaturesRatingsTF.dat";
		   p = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/FiveFoldData/";
		    
		}
	
		//ml
	    else if(dataSetChoice ==1){	  
			m = "C:/Users/Musi/workspace/MusiRecommender/DataSets/ML_ML/TestTrain/FiveFoldData/ml_storedFeaturesRatingsNor1TF.dat";
		    p  = "C:/Users/Musi/workspace/MusiRecommender/DataSets/ML_ML/TestTrain/FiveFoldData/";
		      	
		} 	
		
		//give the appropriate file of FT1 or FT5 
	    else if (dataSetChoice ==2){
	    	m =  "I:/Backup main data march 2010/workspace/MusiRecommender/DataSets/FT/Itembased/FiveFoldData/ft_myNorStoredRatingsBoth1.dat";
		    p  = "I:/Backup main data march 2010/workspace/MusiRecommender/DataSets/FT/Itembased/FiveFoldData/";
		}
	  	
	    
	    	
	    FiveFoldDataRatingGenerator dis = 
	    			new FiveFoldDataRatingGenerator( m,						//main 
			  									     p, 					//train
			  									     folding,				//5 fold, 10 etc
			  									     1						//FT1,FT5
	    											);
	  
	  
	    dis.doCrossValidation(dataSetChoice, 0.8);   //false = big
	  
	  }
	  
	   
	  System.out.println(" Done ");
	  
  }

 
  
  
  

  }
