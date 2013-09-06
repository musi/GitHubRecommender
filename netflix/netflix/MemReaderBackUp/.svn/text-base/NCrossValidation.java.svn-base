package netflix.memreader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;
import cern.colt.list.IntArrayList;

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


public class NCrossValidation 

{
   private int 			nFold;
   private int 			totalFolds;
  
  BufferedWriter 		outT[];
  BufferedWriter 		outTr[];
  
  private String        myPath;
  private Random 		rand;
  
  
  
  private int 			usersNArray[][];
  private int 			moviesNArray[][];			//for each fold, three points --> will be combined to make one sample
  private int 			ratingsNArray[][];
  private boolean		foldNFlag[];
  private int			limitOfEachFold;
  private int 			foldIndex;
  private int			foldLimit[];
  
  
  
  MemHelper mainMh;
  
 /*************************************************************************************************/
  
  public NCrossValidation()
  
  {
	  
  }
  
  
  /*************************************************************************************************/
  
  public NCrossValidation(String myMh,			//main	 
		  				  String myP, 			//path	
		  				  int totalFold,		//how much fold
		  				  int nValidation )     //current fold
  
  {
	  // sml
	    myPath 				 =  myP;
	    mainMh 				 = new MemHelper(myMh);
	    totalFolds 			 = totalFold;
	    rand 				 = new Random();
	  
	    int 	all			 	= (int) mainMh.getAllRatingsInDB();
	    limitOfEachFold		 	= (int) all/nValidation;				//how much a chunk contains
	
	    
	    usersNArray 			= new int[nValidation][limitOfEachFold + 2];
	    moviesNArray 			= new int[nValidation][limitOfEachFold + 2];
	    ratingsNArray 			= new int[nValidation][limitOfEachFold + 2];
	    
	    foldNFlag		 		= new boolean[nValidation];
	    nFold 	 			 	= nValidation;
	    foldIndex				= 0;
	    foldLimit				= new int[nValidation];
	    
	    java.util.Arrays.fill(foldNFlag,true);
	    
	    System.out.println(" each flod size "  + ",all " + all);
	    System.out.println(" combined = " + (limitOfEachFold * nFold));
	    
	    outTr= new BufferedWriter[totalFolds];
	    outT= new BufferedWriter[totalFolds];
	    
	    
	        
	    
  }
  
 /************************************************************************************************/
   
  public void doCrossValidation(boolean smallOrBig) //small =1  
  {
	  
     int uid;
     IntArrayList movies, allUsers;
     int total=0;
     allUsers = mainMh.getListOfUsers(); //all users in the file
     //________________________________________
     
        for (int i = 0; i < allUsers.size(); i++) //go through all the users    
         {
        	        	
          uid = allUsers.getQuick(i);
    	  movies = mainMh.getMoviesSeenByUser(uid); //get movies seen by this user
    
    	  
    	  int mySize    = movies.size();
    	  
    //	  System.out.println(" size = " + mySize + ", test =" + testSize + ", train =" + trainSize);
    	  
    //	  System.out.println(" current user "  + i+ "of total "+ allUsers.size()+ " size =" + mySize);
    	  
        
     //Enter some sort of randomization
    //______________________________________________________________________________________

 if(mySize>=5)
  {	   
    	  
    	if (mySize >1) //else go and copy any where    	
    	{    	  
    	  int doNotWriteTheseMovies[]	 = new int[mySize];  
    	  int writeTheseMovies[] 		 = new int[mySize];
    	  int writeTheseRatings[] 		 = new int[mySize];
    	  
    	  
    	  int index=0;
    	    	  
    	  for (int j = 0; j < mySize; j++) //for all movies	    		
    		 {
    	   		  writeTheseMovies[j] = MemHelper.parseUserOrMovie(movies.getQuick(j));
   	    		  writeTheseRatings[j]	= mainMh.getRating(uid, writeTheseMovies[j]);		  
    		 }
    	      	
    	 //code to remove some rendom movies against a user
    		  
    		  for (int a=0;a<mySize; a++)
	    		  doNotWriteTheseMovies[a]=-1;	//initialise
	    	  
    	  //System.out.println("Current user saw Movies: " + mySize);
    	  long del=0;
    	  
    	  // Some randomisation
    	  //____________________________________________
    		
    	  	while (true)    		 
    		 {
    		  
    			 boolean dontWrite=false;
    			 
    			   //generate a random number 
    			 		try  				{del = rand.nextInt(mySize);  //select some random movies to delete (take their indexes) 
    			 		
    			 							}
    			 		catch (Exception no){ System.out.println(" error in random numbers");
    	    			 					}
    			 		
    			 		
    			      int  myDel = (int)del;
    			    
    			        			 
    			 for (int a= 0;a<mySize;a++)
    		
    			 {
    				 
    				 if (myDel==doNotWriteTheseMovies [a]) {dontWrite=true; break;} //if already want to delete this
    				 
    			 }
    			 
    			//___________________________________________________________________________________
    			 
    			 if (dontWrite == false)     				 
    			   {
    	
    				    				 
    				   // Here, we should have code, to write in K folds
    	
    				    boolean ok = false;
    				    
    				    while (true)    				    
    				    {
    				    	if (foldIndex == nFold) foldIndex =0;		//e.g. 0-9
    				    	if (foldLimit[foldIndex] == limitOfEachFold+1) foldNFlag[foldIndex] =false; 		//this fold is full
    				    	
    		     	    	
    				    //	System.out.println(" each index is " + foldLimit[foldIndex] + " Limit " + limitOfEachFold);
    				    	if (foldNFlag[foldIndex] == true)    				    	
    				    	{
    		   				    		
    				    	  usersNArray	[foldIndex][foldLimit[foldIndex]] = uid;
    				    	  moviesNArray	[foldIndex][foldLimit[foldIndex]] = writeTheseMovies[myDel];
    				    	  ratingsNArray [foldIndex][foldLimit[foldIndex]] = writeTheseRatings[myDel];
    				    	  
    				    	  foldLimit[foldIndex]++;
    				    	  ok =true;
    					        				    	    				    		
    				    	}
    				    	
    				    //	System.out.println(" inside first" + "foldNFlag[foldIndex]" + foldNFlag[foldIndex] + "[foldIndex]= " + foldIndex);	    	
    				    	foldIndex++;		 // go to next fold	
    				    	if (ok==true) break; //to ensure we have write this data into any of the folds
    				    
    				    //	System.out.println(" inside one " + myDel);
    	 			      }//end of while 
    				    
    			    	
    			    	
    				    doNotWriteTheseMovies[index]= myDel;
    				    index++;
    			
    			   } //if donot write == false
    			 
    			//___________________________________________________________________________________
    			 
    	       	 if(index == mySize) break;
    			 
    		 }//end of while true
		
     	  	
         }// if current user saw say more than than 3 movies
         
      	  
    	  //user has less than 3 movies, just copy them in any fold
    	  //____________________________________________________
    	  
    	  else    		  
    	  {

    		  for (int j = 0; j < mySize; j++) //for all movies  	    		
     		 {
    			  	int mid 	= MemHelper.parseUserOrMovie(movies.getQuick(j));
    	    		int rating	= mainMh.getRating(uid, mid);		  
     		 

				    boolean ok = false;
				    
				    while (true)				    
				    {
				    	if (foldIndex == nFold) foldIndex =0;		//e.g. 0-9
				    	if (foldLimit[foldIndex] ==limitOfEachFold+1) foldNFlag[foldIndex] =false; 		//this fold is full
				    	
		     	    	
				    	if (foldNFlag[foldIndex] == true) 				    	
				    	{
				    	
				    	  usersNArray	[foldIndex][foldLimit[foldIndex]] = uid;
				    	  moviesNArray	[foldIndex][foldLimit[foldIndex]] = mid;
				    	  ratingsNArray [foldIndex][foldLimit[foldIndex]] = rating;
				    	  
				    	  foldLimit[foldIndex]++;
				    	  total++;
				    	  ok =true;
				    	  
				    	    				    		
				    	}
				    	
	
				    	foldIndex++;		 // go to next fold	
				    	if (ok==true) break; //to ensure we have write this data into any of the folds
				    
	
    	    			}//end of while
    				}//end of for
     		 } //end of else
  			} //end of filtering
 
         } //end of all users for
   //_________________________________________________________________________________ 	  
  
        
  
        System.out.println("-----------------done folding---------------------");
        
    	//Now write these folds into a file
    	
        String testSample="", trainSample="";
        String testName ="", testStoredName="", trainName="", trainStoredName="";
    
        try {
        //create buffers
    	for (int i=0; i<totalFolds;i++)    		
    	{
    	
    		if (smallOrBig ==true)
    		{  outT[i]  = new BufferedWriter(new FileWriter(myPath + "sml_testSetFold" + (i+1) + ".dat"));
    		   outTr[i] = new BufferedWriter(new FileWriter(myPath + "sml_trainSetFold" + (i+1) + ".dat"));
    		}
    		
    		else
    		{  outT[i]  = new BufferedWriter(new FileWriter(myPath + "ml_testSetFold" + (i+1) + ".dat"));
 		       outTr[i] = new BufferedWriter(new FileWriter(myPath + "ml_trainSetFold" + (i+1) + ".dat"));
 		    }
 		
    			
    	}
    	
    	
    	// write into test and train
    	for (int i=0; i<totalFolds;i++)
    	{
    	   for (int j=0; j<totalFolds; j++)
    		{
    		   for(int k=0;k<limitOfEachFold + 2; k++)
    		   {
    	  
    		    if(j==i) {
    		    	if (usersNArray[i][k]!=0) { 
    		    		testSample = (usersNArray[i][k] + "," + moviesNArray[i][k] + "," + ratingsNArray[i][k]);
    		    	    outT[i].write(testSample);
    		    	    outT[i].newLine();
    		    		}
    		    	}//end of test 
    		    
    		    else {
    		    	if (usersNArray[j][k]!=0) { 
    		    		testSample = (usersNArray[j][k] + "," + moviesNArray[j][k] + "," + ratingsNArray[j][k]);
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
    
    int countAll[] = new int [nFold];
   
    for (int i =0; i <nFold; i++)
    
    {
    	for (int j =0; j <=limitOfEachFold; j++)
        {
    		if (usersNArray[i][j] !=0)countAll[i]++;
    		
        }
    	
    	System.out.println("FOld n contains " + countAll[i]);
    	
    }   

    
    
    //_______________________________________________________________________________
    //Check by deserializing, how many ratings a test and train fold contains
   //_______________________________________________________________________________
    
 
    for (int i=0;i<5;i++)
    {
    	MemHelper mTest = new MemHelper (myPath + "sml_testSetStoredFold" + (i+1) + ".dat");
    	MemHelper mTrain = new MemHelper (myPath + "sml_trainSetStoredFold" + (i+1) + ".dat");
    	
    	System.out.println("Train FOld" + (i+1) + " Users:" + mTrain.getNumberOfUsers()); 	
    	System.out.println("Train FOld" + (i+1) + " Movies:" + mTrain.getNumberOfMovies());
    	System.out.println("Train FOld" + (i+1) + " Ratings:" + mTrain.getAllRatingsInDB());
    	System.out.println("---------------------------------------------------------------");
    	System.out.println("Test FOld" + (i+1) + " Users:" + mTest.getNumberOfUsers()); 	
    	System.out.println("Test FOld" + (i+1) + " Movies:" + mTest.getNumberOfMovies());
    	System.out.println("Test FOld" + (i+1) + " Ratings:" + mTest.getAllRatingsInDB());
    	
    }
    
  }
      
  
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
	    String m = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\TestTrain\\FiveFoldData\\sml_storedDemoRatings.dat";
	    String p  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\TestTrain\\FiveFoldData\\";
    
	
		  
	  
	  	    	
	   	//ML
	    	
	  //  	String m = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\TestTrain\\FiveFoldData\\ml_storedRatings.dat";
	  //  	String p  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\TestTrain\\FiveFoldData\\";
	   
	    	
	  NCrossValidation dis= new NCrossValidation(m,						//main 
			  									 p, 					//train
			  									 folding,				//5 fold, 10 etc
			  									 folding);			    //current index of fold
	  
	  
	  dis.doCrossValidation(true);   //false = big
	  
	  }
	  
	   
	  System.out.println(" Done ");
	  
  }

  }
