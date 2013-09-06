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
 * A class which simply divide data into test/train set
 * @author Musi
 *
 */
public class SimpleDivideIntoSets 

{
  private MemReader 	mr;
  private int 			crossValidation;
  private String 		outFileIdTr;			//train
  private String 		outFileIdT;				//test
  private String 		outFileTr;
  private String        outFileT;
  private String        myPath;
  private Random 		rand;
  MemHelper mainMh;
  
 /*************************************************************************************************/
  
  public SimpleDivideIntoSets()
  {
	  
  }
  
  
  /*************************************************************************************************/
  
  public SimpleDivideIntoSets(String myMh, String myTr, String myT ) //main, train, test
  
  {
	  // sml
	    outFileT		 	= myT;
	    outFileTr 			= myTr;
	//    myPath 				=  myP;
	    mainMh 	= new MemHelper(myMh);
	    
	    rand = new Random();
  }
  
 /************************************************************************************************/
  
  
  public SimpleDivideIntoSets(int n)

  {
	  crossValidation=n;
  }
  
 /************************************************************************************************/
 
  
  //read a file, write 10% in one file and the remaining 90% in another with same names (e.g. test and train set)
 // repeat it for many times
  
  public void readData(double div) 
  
  {
	  
     byte n=8; 				//e.g. for 90% train and 10% test
   
     int uid;
     IntArrayList movies, allUsers;
   
     
     BufferedWriter outT;
     BufferedWriter outTr;
     
     /* outFileT = outFileIdT + n + ".dat";   //e.g. sml_test1.dat
     	outFileTr = outFileIdTr + n + ".dat"; //e.g. sml_train1.dat
     */
     
     int ts=0;
     int trs=0;
     int all=0;
    //________________________________________________________________________________
     
        allUsers = mainMh.getListOfUsers(); //all users in the file
    
      try 
      
	  {
    		outT = new BufferedWriter(new FileWriter(outFileT));	// we wanna write in o/p file
    		outTr = new BufferedWriter(new FileWriter(outFileTr));	// we wanna write in o/p file
   		
     //________________________________________
     
        for (int i = 0; i < allUsers.size(); i++) //go through all the users 
    
         {

          uid = allUsers.getQuick(i);
    	  movies = mainMh.getMoviesSeenByUser(uid); //get movies seen by this user
    
    	  int mySize= movies.size();
    	  
    	     	  
    	  int trainSize = (int) ((div) * mySize);	//80%-20%
    	  int testSize  = mySize - trainSize;
    	  
    	  
        	  
    	 //start writing     	  
    	 for (int j = 0; j < mySize; j++) 
              
          {
    		   all++;
    		     		  
    		  //very important, we write in term of  uid, mid, rating
    		  
    		        int mid    = MemHelper.parseUserOrMovie(movies.getQuick(j));
	    		  	int rating = mainMh.getRating(uid, mid);
	    		  
    		   	if (j<trainSize)
    		  
    		  {
    			    String oneSample = (uid + "," + mid + "," + rating) ; //very important, we write in term of mid, uid, rating
    			    trs++;
    			    outTr.write(oneSample);
					outTr.newLine(); 
				  
    			  
    		  }
    		  
    		  else //write in test file
    		  
    		  {
    			String oneSample = (uid + "," + mid + "," + rating) ; //very important, we write in term of mid, uid, rating
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
  /************************************************************************************************/

  public String getTestingData(int n)
  
  {
	  return (outFileIdT + n + ".dat");
  }
  
  
public String getTrainingData(int n)
  
  {
	  return (outFileIdTr + n + ".dat");
  }
  

public String getPath(int n)

{
	  return (myPath);
}




/************************************************************************************************/
  
  public static void main(String arg[])
  
  {
	  
	  System.out.println(" Going to divide data into test and train data");
	  
	
	  // sml
	    String t  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\TestTrain\\sml_testSet.dat";
	    String tr = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\TestTrain\\sml_trainSet.dat";
	    String p  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\TestTrain\\";
	   
	    
	  //ML
	  /*    String t  = "C:\\Users\\Musi\\workspace\\MusiRec\\DataSets\\ML_ML\\TestTrain\\ml_testSet";
	    	String tr = "C:\\Users\\Musi\\workspace\\MusiRec\\DataSets\\ML_ML\\TestTrain\\ml_trainSet";
	    	String p  = "C:\\Users\\Musi\\workspace\\MusiRec\\DataSets\\ML_ML\\TestTrain\\";
	   */
	    
	  
	  String m = p + "sml_storedRatings.dat";
	    
	  SimpleDivideIntoSets dis= new SimpleDivideIntoSets(m, tr, t);
	  
	  //dis.readData(0.8);
	   dis.readData(0.8);
	   //;
	   System.out.println(" Done ");
	  
  }

  /************************************************************************************************/
  /*************************************************************************************************/
  
  public void divideIntoTestTrain(String mainFile, String trainFile, String testFile, double divisionFactor )
  
  {
	  
	  System.out.println(" Going to divide data into test and train data");
	  
	
	   
	  SimpleDivideIntoSets dis= new SimpleDivideIntoSets(mainFile, trainFile, testFile);
	  
	  //dis.readData(0.8);
	   dis.readData(divisionFactor);
	   //;
	   System.out.println(" Done ");
	  
  }
  
  /*************************************************************************************************/
  /*************************************************************************************************/
  
  
  
}
