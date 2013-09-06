
package netflix.memreader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;
import cern.colt.list.IntArrayList;
import cern.colt.list.LongArrayList;

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

//This is dividing it ok, but we have to see how the PCC will be used e.g.
// active user avg etc?

public class TrainingTestSet 

{
   private int 			nFold;
   private int 			totalFolds;
  
  BufferedWriter 		outT[];
  BufferedWriter 		outTr[];
  
  private String        myPath;
  private Random 		rand;
  private int           howMuchRatings;
  
  
  private int 			usersNArray[][];
  private int 			moviesNArray[][];			//for each fold, three points --> will be combined to make one sample
  private double		ratingsNArray[][];
  private boolean		foldNFlag[];
  private int			limitOfEachFold;
  private int 			foldIndex;
  private int			foldLimit[];
  private int			userInc[];
  
  
  
  MemHelper mainMh;
  
 /*************************************************************************************************/
  
  public TrainingTestSet()
  
  {
	  
  }
  
  
  /*************************************************************************************************/
  
  public TrainingTestSet(String myMh,			//main	 
		  				  String myP, 			//path	
		  				  int totalFold,		//how much fold
		  				  int nValidation )     //current fold
  
  {
	  // sml
	    myPath 				 =  myP;
	    mainMh 				 = new MemHelper(myMh);
	    totalFolds 			 = totalFold;
	    rand 				 = new Random();
	  
	    int 	all			 	= (int) mainMh.getListOfUsers().size();  //all users
	    howMuchRatings 			= (int) mainMh.getAllRatingsInDB();
	    limitOfEachFold		 	= (int) all/nValidation +1;				//how much users a chunk contains
	
	    
	    usersNArray 			= new int[nValidation][howMuchRatings]; //not a good approach but
	    moviesNArray 			= new int[nValidation][howMuchRatings];
	    ratingsNArray 			= new double[nValidation][howMuchRatings];
	    
	    foldNFlag		 		= new boolean[nValidation];
	    foldLimit				= new int[nValidation];
	    userInc					= new int[nValidation];
	    nFold 	 			 	= nValidation;
	    foldIndex				= 0;
	    
	    java.util.Arrays.fill(foldNFlag,true);
	    
	    System.out.println(" each flod size "  + ",all " + all);
	    System.out.println(" combined = " + (limitOfEachFold * nFold));
	    
	    outTr= new BufferedWriter[totalFolds];
	    outT= new BufferedWriter[totalFolds];
	    
	    
	        
	    
  }
  
 /************************************************************************************************/
   
  public void doCrossValidation(boolean smallOrBig) //small =1  
  {
	  
     int uid,mid;
     double ratings =0;
     IntArrayList allUsers;
     LongArrayList movies;	
     int total=0;
     allUsers = mainMh.getListOfUsers(); //all users in the file
     int userSize = allUsers.size();
     //________________________________________
     
       int doNotWriteTheseUsers[] = new int[userSize];
    
		 for (int a=0;a<userSize; a++)
 		  doNotWriteTheseUsers[a]=-1;	//initialise

		  int I = 0;
		  long del=0;
		  boolean dontWrite=false;
		  int  myUser;    	
		  int index=0;
		  
		  
   while (I < userSize)		  
    {
        dontWrite=false;
			    	
			  
	   //generate a random user		  
	   while (true)	    		 
	    { 
		        dontWrite=false;
	    			 
	    	   //generate a random number 
	    			try  				{del = rand.nextInt(userSize);  //select some random movies to delete (take their indexes) 
	    			 		
	    	 							}
	    			catch (Exception no){ System.out.println(" error in random numbers");
	    			 					}
	    			     			 		
	    	         myUser = (int)del;    			        			 
	    			 for (int a= 0;a<index;a++)    		
	    			 {
	    				  if (myUser==doNotWriteTheseUsers [a]) {dontWrite=true; } //if already want to delete this
	    			 }
	    	
	    			 if (dontWrite ==false) break;
	    			 
	       } //end of generationof a user
	   
	   doNotWriteTheseUsers[index]= myUser;
	   index++;
	   
//	  System.out.println("userr =" + I);
	      //________________________________________________
	     //write the movies and ratings of his user in a file
	   
	 	 uid = allUsers.getQuick(myUser);         
    	 movies = mainMh.getMoviesSeenByUser(uid); //get movies seen by this user	    				    
	 	 int mySize = movies.size();
	    				 	 
	     boolean ok = false;
	    				 	
	     while (true)    	    				    
	       {
	    				 		
	        	if (foldIndex == nFold) foldIndex =0;		//e.g. 0-9	    				    	
	        	if (foldLimit[foldIndex] == limitOfEachFold) foldNFlag[foldIndex] =false; 		//this fold is full
	        	//System.out.println("insid  " + I + "flag "+ foldNFlag[foldIndex] + " Ok = " + ok);
	    				    	
	    				    	if (foldNFlag[foldIndex] == true)
	    				    	{		
	    				    		for (int j = 0; j < mySize; j++) //for all movies	    		
	    				    		{
	    				    			mid = MemHelper.parseUserOrMovie(movies.getQuick(j));
	    				    			ratings	= mainMh.getRating(uid, mid);    				 	    				 	    	    	    				    	
	    	    			  		   				    		
	    				    			  if (ratings ==-99) {System.out.println(" user "+uid +",movie "+mid ); System.exit(1);}
	    		   				    	  usersNArray	[foldIndex][userInc[foldIndex]] = uid;
	    	    				    	  moviesNArray	[foldIndex][userInc[foldIndex]] = mid;
	    	    				    	  ratingsNArray [foldIndex][userInc[foldIndex]] = ratings;
	    	    				    	  
	    	    				    	  userInc[foldIndex]++;	//one sample has been written	    	    				    	    					        				    	    				    		
	    	    				     } //write one user is ok
	    				    		
	    				    		  ok =true;	    	
	      	    				    } //end of fold writing
	    				    
	    				    	if (ok==true) break; //to ensure we have write this data into any of the folds
	    				    	foldIndex++;		 // go to next fold
	    				     	
	    				    }//end of while true
         
         
	    			foldLimit[foldIndex]++;
	    			I++;
	    			//System.out.println("User "+ I);
	    			
        } //end of all users while
   //_________________________________________________________________________________ 	  
  
        
  
        System.out.println("-----------------done folding---------------------");
        
    	//Now write these folds into a file
    	
        String testSample="", trainSample="";
        String testName ="", testStoredName="", trainName="", trainStoredName="";
    
        try {
        //create buffers
    	for (int i=0; i<totalFolds;i++)
    		
    	{
    	
    		outT[i]  = new BufferedWriter(new FileWriter(myPath + "sml_testSetFold" + (i+1) + ".dat"));
    		outTr[i] = new BufferedWriter(new FileWriter(myPath + "sml_trainSetFold" + (i+1) + ".dat"));
    	}
    	
    	// write into test and train
    	for (int i=0; i<totalFolds;i++)
    	{
    	   for (int j=0; j<totalFolds; j++)
    		{
    		   for(int k=0;k<howMuchRatings; k++)
    		   {
    	  
    		    if(j==i) {
    		    	if (usersNArray[i][k]!=0) { 
    		    		testSample = (moviesNArray[i][k] + "," + usersNArray[i][k] + "," + ratingsNArray[i][k]);
    		    	    outT[i].write(testSample);
    		    	    outT[i].newLine();
    		    		}
    		    	}//end of test 
    		    
    		    else {
    		    	if (usersNArray[j][k]!=0) { 
    		    		testSample = (moviesNArray[j][k] + "," + usersNArray[j][k] + "," + ratingsNArray[j][k]);
    		    		outTr[i].write(testSample);
    		    	    outTr[i].newLine();	   
    		    	   }
    		         }//end of train
    		    
    		   }//end of for
    		   
    		}//end of mid for
    	   
    	   outT[i].close();
    	   outTr[i].close();
    	 }//end of outer for
    	
    	   	
       }//end of try
    	 
        
                
      catch (IOException e){
		  System.out.println("Write error!  Java error: " + e);
		  System.exit(1);

	    } //end of try-catch     

    	//Now write thes files into memory
      
      System.out.println("-----------------done simple ewriting---------------------");
      
      MemReader myR = new MemReader();
    
      
      for (int i=0; i<totalFolds;i++)
  	{
  	
  		if (smallOrBig ==true)
  			
  			{	myR.writeIntoDisk((myPath + "sml_testSetFold" + (i+1) + ".dat"), (myPath + "sml_testSetStoredFold" + (i+1) + ".dat"));
  				myR.writeIntoDisk((myPath + "sml_trainSetFold" + (i+1) + ".dat"), (myPath + "sml_trainSetStoredFold" + (i+1) + ".dat"));  		   
  			}
  		
  		else
  			
			{	myR.writeIntoDisk((myPath + "ml_testSetFold" + (i+1) + ".dat"), (myPath + "ml_testSetStoredFold" + (i+1) + ".dat"));
				myR.writeIntoDisk((myPath + "ml_trainSetFold" + (i+1) + ".dat"), (myPath + "ml_trainSetStoredFold" + (i+1) + ".dat"));  		   
			}
		
  	}
      
     //_______________________________________________________________________________
    // now we inspect for their validity
    
      
    for (int i =0; i <nFold; i++)
    
    {	System.out.println("FOld n contains " + foldLimit[i]);
    	
    }   

    
    
  }
      
  
/************************************************************************************************/
  
  public static void main(String arg[])
  
  {
	  
	  System.out.println(" Going to divide data into K folds");
	  int folding =10;
	  
	  //for (int i=0;i<folding;i++) //for test factor
	
	  {
	  // sml (for validation)
	  
	  //we have training data across each fold now and we want to convert that into valdiation and training data 
	  // wanna check against which test and train (bigget	  
	   
	/*	folding = 10;
	    String m = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\TestTrain\\TenFoldData\\sml_storedRatings.dat";
	    String p  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\TestTrain\\DataU1\\TenFoldData\\";
	  */
		  
		  
	    folding = 5;
	    String m = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\TestTrain\\FiveFoldData\\sml_storedRatings.dat";
	    String p  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\TestTrain\\FiveFoldData\\DataU1\\";
  
	  
	  
	  // sml (for testing)
	  
	      
		 /*     String t  = "C:\\Users\\Musi\\workspace\\MusiRec\\DataSets\\SML_ML\\TestTrain\\sml_20testSetStore.dat";
		    	String tr = "C:\\Users\\Musi\\workspace\\MusiRec\\DataSets\\SML_ML\\TestTrain\\sml_80trainSetStore.dat";
		    	String p  = "C:\\Users\\Musi\\workspace\\MusiRec\\DataSets\\SML_ML\\TestTrain\\";
		        String m = p + "sml_storedRatings.dat";
		   */
		
	    	
	   	//ML
	    	
	/*    	String m = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\TestTrain\\FiveFoldData\\ml_storedRatings.dat";
	    	String p  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\TestTrain\\FiveFoldData\\";
	  */ 
	    	
	  TrainingTestSet dis= new TrainingTestSet(m,						//main 
			  									 p, 					//train
			  									 folding,				//5 fold, 10 etc
			  									 folding);			    //current index of fold
	  
	  
	  dis.doCrossValidation(true);
	  
	  }
	  
	   
	  System.out.println(" Done ");
	  
  }

  }