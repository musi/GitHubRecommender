
package netflix.memreader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;
import cern.colt.list.IntArrayList;
import netflix.memreader.MemReader;

public class DivideIntoSmallerSets 

{
  private MemReader 	mr;
  private int 			smallerSetNumber;
  private String 		outFileIdTr;			//train
  private String 		outFileIdT;				//test
  private String 		outFileTr;
  private String        outFileT;
  private String        myPath;
  private Random 		rand;
  MemHelper mainMh;
  
 /*************************************************************************************************/
  
  public DivideIntoSmallerSets()
  {
	  
  }
  
  
  /*************************************************************************************************/
  
  public DivideIntoSmallerSets(String myMh, String myTr, String myT, int smallerSet ) //main, train, test
  
  {
	  // sml
	    outFileT		 	= myT;
	    outFileTr 			= myTr;
	    mainMh 				= new MemHelper(myMh);
	    smallerSetNumber    = smallerSet;
	    
	    rand = new Random();
  }
  
 /************************************************************************************************/
  
  public void readData(double div, boolean clustering)  
  {
	  
     byte n=8; 				//e.g. for 90% train and 10% test
   
     int uid;
     IntArrayList movies, allUsers, subSetOfUsers = new IntArrayList();
   
     
     BufferedWriter outT;
     BufferedWriter outTr;  
     int ts=0;
     int trs=0;
     int all=0;
     
     allUsers = mainMh.getListOfUsers(); //all users in the file
     int  sizeOfUsers = allUsers.size();
     
      try      
	  {
    	outT = new BufferedWriter(new FileWriter(outFileT));	// we wanna write in o/p file
    	outTr = new BufferedWriter(new FileWriter(outFileTr));	// we wanna write in o/p file
  
    	
     //-----------------------------
     // select subset of 500 users	
     //-----------------------------    	
    
    	   for (int i = 0; i < sizeOfUsers; i++) //go through all the users    
           {
           	  uid = allUsers.getQuick(i);
	       	  movies = mainMh.getMoviesSeenByUser(uid); //get movies seen by this user	       
	       	 
	       	  int mySize= movies.size();
	       	  
	       	  if(mySize>40)								// Only users having more than 20 movies
	       	  {
	       		  subSetOfUsers.add(uid);
	       	  }
	       
           } // end for
    	   
    	   // Now select some random users from the set of users who have rated more than 40 movies
    	   
    	   int subSetSize = subSetOfUsers.size();
    	   int dummyUid=0;
    	   
    	   for (int i = 0; i < sizeOfUsers; i++) //go through all the users    
           {
           	             	  
           	  		try {
           	  		      dummyUid = rand.nextInt(subSetSize-i);  //select some random movies to delete (take their indexes) 
           	  			 }						
           				
           	  		catch (Exception no){
           	  			 		System.out.println(" error in random numbers");
           	  			  }
	       
           	  		//delete this uid
           	  		if(subSetOfUsers.contains(dummyUid))
           	  		{
           	  			subSetOfUsers.remove(dummyUid);
           	  		}
           	  		
           	  		if(subSetOfUsers.size() <=smallerSetNumber) 
           	  			break;           	  		
           } //end of for
    	   
    	   
    	   //-------------------------------
    	   // We have already chosen 500 
    	   // random users, Go through it
    	   //-------------------------------
    	   
    	   sizeOfUsers = subSetOfUsers.size();
    	   
    	   System.out.println(" totaol users now ="+ sizeOfUsers);
       for (int i = 0; i < sizeOfUsers; i++) //go through all the users    
        {
        	
          uid = subSetOfUsers.getQuick(i);
    	  movies = mainMh.getMoviesSeenByUser(uid); //get movies seen by this user
    
    	  int mySize= movies.size();
    	  
    	  int trainSize = (int) ((div) * mySize);	//80%-20%
    	  if (trainSize==0) trainSize=1;
    	  int testSize  = mySize - trainSize;
    	
    	  
    	  //Enter some sort of randomization
  
    	  int doNotWriteTheseMovies[]	 = new int[testSize]; 			// 0-removeSize (e..g 0-5)
    	  int writeTheseMovies[] 		 = new int[mySize];
    	  int writeTheseRatings[] 		 = new int[mySize];
    	  int writeTheseMovies1[] 		 = new int[mySize];
    	  
    	  int index=0;
    	    	  
    	  for (int j = 0; j < mySize; j++) //for all movies	    		
    		 {
    	   		  writeTheseMovies1[j] = writeTheseMovies[j] = MemHelper.parseUserOrMovie(movies.getQuick(j));
   	    		  writeTheseRatings[j]	= mainMh.getRating(uid, writeTheseMovies[j]);		  
    		 }
    	  
    	  
    	  if(testSize>1)    		  
    	  {
    	 //code to remove some rendom movies against a user
    		  
    		  for (int a=0;a<testSize; a++)
	    		  doNotWriteTheseMovies[a]=-1;	//initialise
	    	  
    	  //System.out.println("Current user saw Movies: " + mySize);
    	  long del=0;
    	  
    	  //____________________________________________
    		
    	  	while (true)	//loop untill true    		 
    		 {
    		  
    			 boolean dontWrite=false;
    			 
    			   //generate a random number 
    			 		try  				{del = rand.nextInt(mySize-1);  //select some random movies to delete (take their indexes) 
    			 		
    			 							}
    			 		catch (Exception no){ System.out.println(" error in random numbers");
    	    			 					}
    			 		
    			 		
    			      int  myDel = (int)del;
    			    
    			        			 
    			 for (int a= 0;a<testSize;a++)    		
    			 {
    				 
    				 if (myDel==doNotWriteTheseMovies [a]) {dontWrite=true; break;} //if already want to delete this
    				 
    			 }
    			 
    			 if (dontWrite == false) 
    				 {
    				    doNotWriteTheseMovies[index]= myDel;
    				    writeTheseMovies[myDel]= -1;			//If it is -1, it means we are not writing it
    				    index++;
    				 }
    			 
    			 if(index== testSize) break;
    			 
    		 }//end of while
     	  	
     }// if removeSize >1

   //_________________________________________________________________________________ 	  
    
    	  
    	 //start writing     	  
    	 for (int j = 0; j < mySize; j++)               
          {
    		   all++;
    		     		  
    		  //very important, we write in term of mid, uid, rating
    		     		    		  
    		  if(writeTheseMovies[j] != -1)		//we will write it    		  
    		  {
    			    String oneSample = (uid + ","+ writeTheseMovies[j]+ "," + writeTheseRatings[j]) ; //very important, we write in term of mid, uid, rating
    			    trs++;
    			    outTr.write(oneSample);
					outTr.newLine(); 		  
    			  
    		  }
    		  
    		  else //write in test file (writeTheseMovies1)    		  
    		  {
    			String oneSample = (uid + ","+ writeTheseMovies1[j]+ "," + writeTheseRatings[j]) ; //very important, we write in term of mid, uid, rating
  			    ts++;
  			    outT.write(oneSample);
				outT.newLine(); 
				   			  
    		  }
    		  
    		  		  
          }//end of movies for
    	  
    	     
      }//end of all user for
      
      System.out.println("Test = " + ts + " Train= " +trs + " all= "+all + " sum = " + (ts+trs));
      outT.close();
      outTr.close(); 
      
  }// end of try
      
    catch (IOException e)
	  	
	  	{
		  System.out.println("Write error!  Java error: " + e);
		  System.exit(1);

	    } //end of try-catch     

    	
  }
      

/************************************************************************************************/
  
  public static void main(String arg[])
  
  {
	  
	  System.out.println(" Going to divide data into test and train data");

	  // sml
	    String t  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\Sparsity\\dumTest.dat";
	    String tr = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\Sparsity\\dumTrain.dat";
	    String p  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\Sparsity\\";
	    String pm  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\Sparsity\\";
	   		
	
/*	    String t  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\sml_TestSet10.dat";
	    String tr = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\sml_TrainSet.dat";
	    String p  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\";
	    String pm  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\";
	   */
	  

		  String m = pm + "sml_storedRatings.dat";
		 
		  DivideIntoSmallerSets dis= new DivideIntoSmallerSets(m, 
															   tr, 
															   t, 
															   500);  					 // 500 users
		  dis.readData(0.8, false); 													 //true if clusters
		  
		  System.out.println("Going to write ");
		  MemReader myReader = new MemReader();
		  myReader.writeIntoDisk(t, p + "sml_TestSet20.dat"); 			 //write test set into memory
		  myReader.writeIntoDisk(tr,  p + "sml_TrainSet80.dat");				

		  System.out.println(" Done ");
	  
  }

  /************************************************************************************************/
  /*************************************************************************************************/
  
  public void divideIntoTestTrain(String mainFile, String trainFile, String testFile, double divisionFactor )
  
  {
	  
	  System.out.println(" Going to divide data into test and train data");
	  
	
	   
	  DivideIntoSmallerSets dis= new DivideIntoSmallerSets(mainFile, trainFile, testFile, 500);
	  
	  //dis.readData(0.8);
	   dis.readData(divisionFactor, false);
	   //;
	   System.out.println(" Done ");
	  
  }
  
  /*************************************************************************************************/
  /*************************************************************************************************/
  
  
  
}

