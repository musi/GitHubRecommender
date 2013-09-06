
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
import netflix.memreader.MemReader;

public class DivideIntoSetsOfUsers 

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
  
  public DivideIntoSetsOfUsers()
  {
	  
  }
  
  
  /*************************************************************************************************/
  
  public DivideIntoSetsOfUsers(String myMh, String myTr, String myT ) //main, train, test
  
  {
	  // sml
	    outFileT		 	= myT;
	    outFileTr 			= myTr;
	//    myPath 				=  myP;
	    mainMh 	= new MemHelper(myMh);
	    
	    rand = new Random();
  }
  
 /************************************************************************************************/
  
  
  public DivideIntoSetsOfUsers(int n)

  {
	  crossValidation=n;
  }
  
 /************************************************************************************************/
 
  
  //read a file, write 10% in one file and the remaining 90% in another with same names (e.g. test and train set)
 // repeat it for many times
  
  public void readData(double div, boolean clustering) 
  
  {
	  
     byte n=8; 				//e.g. for 90% train and 10% test
   
     int uid, mid;
     double rating;
     IntArrayList   allUsers;
     IntArrayList   trainUsers = new IntArrayList();	// train users, like 90%
     IntArrayList   testUsers  = new IntArrayList();    // test users, content-boosted says it should be 10%
     LongArrayList  movies;   
    
     BufferedWriter outT;
     BufferedWriter outTr;
     
     int ts=0;
     int trs=0;
     int all=0;
     
    //________________________________________________________________________________
     
     allUsers = mainMh.getListOfUsers(); //all users in the file
    
      try      
	  {
    	outT = new BufferedWriter(new FileWriter(outFileT));	// we wanna write in o/p file
    	outTr = new BufferedWriter(new FileWriter(outFileTr));	// we wanna write in o/p file
   		
     //-----------------------------
     // select x% Users as test set	
     //-----------------------------
    	int temp =0;
    	int testSizeIsFilled =0;
    	int myUserSize = allUsers.size();
    	
    	int trainSize = (int) ((div) * myUserSize);	//80%-20%, 10%-90%
    	int testSize  = myUserSize - trainSize;
    	
     	while (true)	//loop until true    		 
		 {
		  
			 boolean dontWrite=false;
			 
			   //generate a random number 
			 		try  				{
			 							  temp = (int)rand.nextInt(myUserSize-1);  //select some random movies to delete (take their indexes) 
			 		
			 							}
			 		
			 		catch (Exception no){ 
			 								System.out.println(" error in random numbers");
	    			 					}
			 		
			 		
			 	  int tempUser = allUsers.getQuick(temp);	
			      
			 	  if(!testUsers.contains(tempUser))		//If user is not there already
			      {
			    	  testUsers.add(tempUser);
			    	  testSizeIsFilled++;
			      }
			    
			 	  if(testSizeIsFilled == testSize)	  //We got total no of users in test set		
			 		  				break;
			 	  
		 } //end of while 


          
   //_________________________________________________________________________________ 	  
    
    	  
    	 //start writing     	  
    	 for (int i = 0; i < myUserSize; i++)               
          {
    		  all++;
    		  uid = allUsers.getQuick(i);
    		  LongArrayList userMovies= mainMh.getMoviesSeenByUser(uid);
    		  int myMovieSize = userMovies.size();
    		  
    		  //-----------------------------------
    		  // Start writing with test set users
    		  // in test set
    		  //-----------------------------------
    		 		 
    		 for (int j=0;j< myMovieSize; j++)
    		 {			 
    			    mid = MemHelper.parseUserOrMovie(userMovies.getQuick(j)); //get a parsed movie			    
    			    rating = mainMh.getRating(uid, mid);    		     		    		  
    				String oneSample = uid + "," + mid + "," + rating;
    		 
    				if(testUsers.contains(uid))  //test set
    				{
    					ts++;
    	  			    outT.write(oneSample);
    					outT.newLine();
    					 		  
    				}
    		
	    		  else //write in test file (writeTheseMovies1)    		  
	    		  {	    			
	    			    trs++;
	    			    outTr.write(oneSample);
						outTr.newLine();		   			  
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
	  /*  String t  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\TestTrain\\sml_20testSet.dat";
	    String tr = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\TestTrain\\sml_80trainSet.dat";
	    String p  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\TestTrain\\";
	    String pm  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\TestTrain\\";
	   		*/
	
	    String t  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\sml_TestSet10.dat";
	    String tr = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\sml_TrainSet.dat";
	    String p  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\";
	    String pm  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\";
	   
	  
	    //FT
	   /* String t  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\FT\\TestTrain\\ft_20testSet.dat";
	    String tr = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\FT\\TestTrain\\ft_80trainSet.dat";
	    String p  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\FT\\TestTrain\\";
	    String pm  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\FT\\TestTrain\\";
	   	*/	

	   //ML
	/*    String t  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\ml_20testSet.dat";
	    String tr = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\ml_80trainSet.dat";
	    String p  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\";
	    String pm  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\";
	*/
	     		String m = pm + "sml_storedFeaturesRatingsTF.dat";
		 //		String m = pm + "ft_storedFeaturesRatings.dat";
		 //       String m = pm + "ml_storedFeaturesRatings.dat"; //only TF features are stored
		    
		  DivideIntoSetsOfUsers dis= new DivideIntoSetsOfUsers(m, tr, t);
		  dis.readData(0.9, false);  //true if clusters
		  MemReader myReader = new MemReader();
		  myReader.writeIntoDisk(t, p + "sml_clusteringTestSetStoredTF_Users.dat"); 							//write test set into memory
		  myReader.writeIntoDisk(tr,  p + "sml_clusteringTrainSetStoredTF_Users.dat");				
			
	
	
	  
	  /*
	  String m = pm + "Sml_clusteringStoredRatings10.dat";
	    
	  DivideIntoSets dis= new DivideIntoSets(m, tr, t);
	  
	  
	   dis.readData(0.8);
	   
	   MemReader myReader = new MemReader();
	   
	   myReader.writeIntoDisk(t, p + "sml_clusteringTestSetStored.dat"); 							//write test set into memory
	   myReader.writeIntoDisk(tr,  p + "sml_clusteringTrainSetStored.dat");				
		
		*/    

	  
/*
	  //to check sensitivity parameter for 80-20 main test train dividion and for 80 validation and test (x =0.1 to 0.6), then cross validation on each 
	  for (int i=0; i<6;i++)
	  
	  {

		  // sml
		    String t  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\TestTrain\\sml_"+ ((i+1)*10)+ "testSet.dat";
		    String tr = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\TestTrain\\sml_" + ((10- (i+1))*10)+ "trainSet.dat";
		    String p  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\TestTrain\\Trainer\\Actual20\\";
		    String pm  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\TestTrain\\";
		    
			
		 
		  String m = p + "sml_80trainSetStored.dat";
		    
		  DivideIntoSets dis= new DivideIntoSets(m, tr, t);
		  
		  //dis.readData(0.8);
		   dis.readData((10-(i+1))/10);
		   
		   System.out.println(" Done " +i);
		   
		   MemReader myReader = new MemReader();
		   myReader.writeIntoDisk(t, p + "Case" + ((i+1)*10)+ "sml_" + ((i+1)*10)+ "testSetStored.dat"); 							//write test set into memory
		   myReader.writeIntoDisk(tr,  p + "Case" + ((i+1)*10)+"sml_" + + ((10-(i+1))*10)+ "trainSetStored.dat");				
	  }
*/		    

	  
		   
	   System.out.println(" Done ");
	  
  }

  /************************************************************************************************/
  /*************************************************************************************************/
  
  public void divideIntoTestTrain(String mainFile, String trainFile, String testFile, double divisionFactor )
  
  {
	  
	  System.out.println(" Going to divide data into test and train data");
	  
	
	   
	  DivideIntoSetsOfUsers dis= new DivideIntoSetsOfUsers(mainFile, trainFile, testFile);
	  
	  //dis.readData(0.8);
	   dis.readData(divisionFactor, false);
	   //;
	   System.out.println(" Done ");
	  
  }
  
  /*************************************************************************************************/
  /*************************************************************************************************/
  
  
  
}

