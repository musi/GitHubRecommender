package netflix.memreader;

import java.io.BufferedWriter;
import netflix.algorithms.memorybased.memreader.MyRecommender;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.text.*;
import java.util.Random;
import cern.colt.list.IntArrayList;





/**
 * Class which can write different kind of data, like all users ...who saw less the 20 movies etc
 * 
 * @author Musi
 */
/**************************************************************************************************/
public class AnalyzeAFile 
/**************************************************************************************************/


{
	  
	  NumberFormat  formatter;
	  double 		currentSparsity;
	  MemHelper 	mainmh;
	  int 			size[];
	  int 			listOfUsers[];
	  
/*************************************************************************************************/
	  
	  public AnalyzeAFile()
	  {
		  
	  }
	  
/*************************************************************************************************/

	  public AnalyzeAFile(String f)
	  
	  {
		  	formatter		 = 	new DecimalFormat("#.#####");	//upto 4 digits
		    currentSparsity	 =	0.0;
		    mainmh 			 = 	new MemHelper(f);
		    listOfUsers      = new int[1000];
		    
		    calculateSparsity(f);
     }
	    
	  	  
/************************************************************************************************/
	 
	  
	  //read a file, write 10% in one file and the remaining 90% in another with same names (e.g. test and train set)
	 // repeat it for many times
	  
	  public void checkFile()
	  
	  {
		
	     
	     int uid;
	     IntArrayList movies, allUsers;
	     int ts=0;
	     int trs=0;
	     int all=0;
	     int mySize=0;
	     
	     //_________________________________________________
	     
	      
	     	     
	     allUsers = mainmh.getListOfUsers(); //all users in the file
	    
	   for (int i = 0; i < allUsers.size(); i++) //go through all the users 
	    
	      {
	      	  uid 	 = allUsers.getQuick(i);
	    	  movies = mainmh.getMoviesSeenByUser(uid); //get movies seen by this user
	      	  mySize     = movies.size();

	      	  listOfUsers [mySize]++;
	      	  
	    	   	for (int j = 0; j < mySize; j++) //for all movies
	    		
	    		 {
	    		   			
	    			int mid = MemHelper.parseUserOrMovie(movies.getQuick(j));
		   	        int rating 	= mainmh.getRating(uid, mid);		  
		    		
	    	  
	    		 }
	        
	    	     
	      }//end of all user for
	      
	 }
	  	    
	     	
	      
	  
/************************************************************************************************/
	 
	  
	public static void main(String arg[])
	  
	  {
		  
		  System.out.println(" Going to divide data into test and train data");
		  System.out.println(" Done ");
		  String mFile = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\TestTrain\\sml_80trainSetStored.dat";
		  AnalyzeAFile analyze =  new AnalyzeAFile(mFile);
		  analyze.checkFile();
		  System.out.println("Done");
		  analyze.statistics();
		  analyze.calculateSparsity(mFile);
		  
		   
	  }
	  

	  

	public void analyzeContent(String mFile)
	  
	  {
		  
		  System.out.println(" Going to divide data into test and train data");
		  	  
		  AnalyzeAFile analyze =  new AnalyzeAFile(mFile);
		  analyze.checkFile();
		  System.out.println("Done");
		  analyze.statistics();
		  
		   
	  }
	  
	

/**************************************************************************************************/

	  public void statistics()
	
	  {
		  	for (int i=0;i<999;i++)
		  
		  	{
		  		if (listOfUsers [i] >=1)
		  		System.out.println(" Total users who saw " + (i+1) + " movies : " + listOfUsers[i]);
		  		
		  	}
			
		  	System.out.println(" Sparsity is : " + currentSparsity );
		  	
	  }
	  
	  
 /**************************************************************************************************/
	  
	public double calculateSparsity(String file)
	  
	 {
		 MemHelper myObj = new MemHelper(file);

		 System.out.println(" Total no of entries " + myObj.getAllRatingsInDB());
		 int users  = myObj.getNumberOfUsers();
		 int movies = myObj.getNumberOfMovies();
		 
		 System.out.println(" Number of users:" + users);
		 System.out.println(" Number of movies:" + movies);
		 
		 		
		 double possible = users * movies;
		 double actual = myObj.getAllRatingsInDB();
		 
		  double currentSparsityLevel  = 	 1- (actual/possible);	// 1 - (non-zero entries/total entries)
		  
		   System.out.println(" Sparsity "+ (currentSparsityLevel));
		//  System.out.println(" Sparsity in Current set is: " + (formatter.format(currentSparsityLevel)));
		   // why null pointer error, even though formatter is not null
		  
		  currentSparsity = currentSparsityLevel;
		  return currentSparsityLevel;
	  }

//Note (?....U can do this by looop)???
	
	/*
	 * For 100,000:sparsity level = 0.9369
	 * For 80,000: sparsity level = 0.9495
	 * For 70,000: sparsity level = 0.9558
	 * For 60,000: sparsity level = 0.9621
	 * For 50,000: sparsity level = 0.9684
	 * For 40,000: sparsity level = 0.9747
	 * For 30,000: sparsity level = 0.9810
	 * For 20,000: sparsity level = 0.9872
	 * For 10,000: sparsity level = 0.9936
	 * For 5,000: sparsity level = 0.9968
	 * 
	 * 
	 */
	 
	
/**************************************************************************************************/	  

	
	

/**************************************************************************************************/
	
	
	
	
	
	
}
