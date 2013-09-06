package netflix.memreader;


//delete this and may u have to write efficient code for 80% train and 20% test set

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;

import netflix.FtMemreader.FTMemHelper;
import cern.colt.list.IntArrayList;
import cern.colt.list.LongArrayList;



public class SetDivision 

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
  
  public SetDivision()
  {
	  
  }
  
  
  /*************************************************************************************************/
  
  public SetDivision (String myMh, String myTr, String myT ) //main, train, test
  
  {
	  // sml
	    outFileT		 	= myT;
	    outFileTr 			= myTr;
	//  myPath 				=  myP;
	    mainMh 				= new  MemHelper(myMh);
	    
	    rand = new Random();
  }
  
 /************************************************************************************************/
  
  
  public SetDivision(int n)

  {
	  crossValidation=n;
  }
  
 /************************************************************************************************/
 
  
  //read a file, write 10% in one file and the remaining 90% in another with same names (e.g. test and train set)
 // repeat it for many times
  
  public void readData(double div, boolean clustering)  
  {
	  
     byte n=8; 				//e.g. for 90% train and 10% test
   
     int uid;
     IntArrayList movies; 
     IntArrayList allUsers;
     IntArrayList movieAlreadyThere = new IntArrayList();
         
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
   		
     //________________________________________
     
        for (int i = 0; i < allUsers.size(); i++) //go through all the users    
         {
        	
          uid = allUsers.getQuick(i);
    	  movies = mainMh.getMoviesSeenByUser(uid); //get movies seen by this user
    
    	  int mySize= movies.size();
    	  all+=mySize;
    	  int trainSize = (int) ((div) * mySize);	//80%-20%
    	  if (trainSize==0) trainSize=1;
    	  int testSize  = mySize - trainSize;
    	
    	//  if (clustering == true) { testSize = 8; trainSize = mySize -8;}
    	  
    	//Enter some sort of randomization
    //______________________________________________________________________________________

    	int totalProcessed = 0;    	  
    	int del=0;
    	int mid=0;
    	int tMid=0;
    	int rating =0;
    	
    	while (totalProcessed <mySize)
    	{  		  	
    		if (totalProcessed <testSize && testSize >0)
    		{
    			   //generate a random number 
  			 		try  				{del = (int) rand.nextInt(mySize-1);  //select some random movies to delete (take their indexes) 
  			 							}
  			 		
  			 		catch (Exception no){ System.out.println(" error in random numbers");
  	    			 					}
  			 		
  			 		tMid = movies.getQuick(del);
  			 		mid = MemHelper.parseUserOrMovie(tMid); 			  	
  											
	  			if (!(movieAlreadyThere.contains(mid))) //if user has not already been processed
	  			 {	  				
		  				movieAlreadyThere.add(mid);   	
		  				rating = mainMh.getRating(uid, mid);
		  				
	  				    String oneSample = (uid + "," + mid + "," + rating) ; //very important, we write in term of mid, uid, rating
	    			    ts++;
	    			    outT.write(oneSample);
						outT.newLine();
						totalProcessed++;
						System.out.println("ts"+ (ts) + " with size= "+ mySize + " processed = "+ totalProcessed );
	  			  }	   
	  		 } //end of if
  			
  			 else  	  				  
  	  		  {  				 
  				 for (int k=0;k<mySize;k++)
		  	  		{
		  				    mid = MemHelper.parseUserOrMovie(movies.getQuick(k));
  					 		
		  				    if (!(movieAlreadyThere.contains(mid)))
		  				    {
			  				    String oneSample = (uid + "," + mid + "," + rating) ; //very important, we write in term of mid, uid, rating
			  	    		    trs++;
			  	    		    outTr.write(oneSample);
			  					outTr.newLine(); 
			  					totalProcessed++;
			  					System.out.println("trs"+ (trs));
			  				 }
		  	  		  }
		  	   } //end of train set			  
    		  		  
  				  		  
            }//end of while
   } //end of all users 
      
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


	    String t  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\sml_TestSet10.dat";
	    String tr = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\sml_TrainSet.dat";
	    String p  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\";
	    String pm  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\";


	  
	  
	   	  // String m = pm + "ft_storedRatings.dat";	    
	  	 //  String m = pm + "sml_storedRatings.dat";
	         String m = pm + "sml_Top20StoredRatings.dat";
	         
	  	   SetDivision dis= new SetDivision(m, tr, t);  
	  	   dis.readData(0.8, false);	   
	  	   MemReader myReader = new MemReader();	   
	 	   myReader.writeIntoDisk(t, p + "sml_clusteringTestSetStored.dat"); 							//write test set into memory
		   myReader.writeIntoDisk(tr,  p + "sml_clusteringTrainSetStored.dat");	
	  	  


	  
	  
	  
	  
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
		    
		  FTDivideIntoSets dis= new FTDivideIntoSets(m, tr, t);
		  
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
	  
	
	   
	  SetDivision dis= new SetDivision(mainFile, trainFile, testFile);
	  
	  //dis.readData(0.8);
	   dis.readData(divisionFactor, false);
	   //;
	   System.out.println(" Done ");
	  
  }
  
  /*************************************************************************************************/
  /*************************************************************************************************/
  
  
  
}
