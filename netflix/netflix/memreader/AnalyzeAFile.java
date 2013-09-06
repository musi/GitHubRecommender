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
import cern.colt.list.LongArrayList;





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
	  int 			totalUsersLessThan20;
	  int 			totalMoviesLessThan20;
	  
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
		    totalUsersLessThan20 = 0;
		    totalMoviesLessThan20 = 0;
		    
		    
		//    calculateSparsity(f);
     }
	    
	  	  
/************************************************************************************************/
	 
	  
	  //read a file, write 10% in one file and the remaining 90% in another with same names (e.g. test and train set)
	 // repeat it for many times
	  
	  public void checkFile(int movieLimit, int userLimit)	  
	  {
	     int uid;
	     IntArrayList  allUsers;
	     LongArrayList  movies;
	     int ts=0;
	     int trs=0;
	     int all=0;
	     int mySize=0;
	 	
		LongArrayList usersWhoSawCurrentMovie;
		int userSize = 0;
		  
		   //------------------
		   //check Users
		   //------------------
		   
	         	     
	   allUsers = mainmh.getListOfUsers(); //all users in the file
	    
	   for (int i = 0; i < allUsers.size(); i++) //go through all the users	    
	    {
	      	  uid 	 = allUsers.getQuick(i);
	    	  movies = mainmh.getMoviesSeenByUser(uid); //get movies seen by this user
	      	  mySize = movies.size();

	      	 if (mySize<userLimit)
			 {
			 	//System.out.println("Movies seen by user "+ i + " = " + mySize);
			 	totalUsersLessThan20++;
			 }
		
	      	  listOfUsers [mySize]++;
	      	  
	    	   	for (int j = 0; j < mySize; j++) //for all movies	    		
	    		 {
	    		   			
	    			int mid = MemHelper.parseUserOrMovie(movies.getQuick(j));
		   	        double rating 	= mainmh.getRating(uid, mid);		  
		    		
		   	        if(rating ==0)	System.out.println("uid, mid, rating,=" + uid + "," +mid + ","+ rating);
	    		 }
	        
	    	     
	      }//end of all user for
	   
	   //------------------
	   //check Items
	   //------------------	   
	   
	     IntArrayList allMovies = mainmh.getListOfMovies();
	     int noOfItems = allMovies.size();
	     
	  // for all items
	  for(int j=1; j<noOfItems;j++)
	  {				 
	   	  int  mid = (j);
		  usersWhoSawCurrentMovie =  mainmh.getUsersWhoSawMovie(mid);
		  userSize = usersWhoSawCurrentMovie.size();
		  
		  if(userSize< movieLimit)
		  {
//			 /	System.out.println("No of users who saw movie "+ j + " = " + userSize );
			 	totalMoviesLessThan20++;
		  }
		  
	  }// end of for
  
	  System.out.println("Users found with less than " + userLimit +" ratings ="+ totalUsersLessThan20);
	  System.out.println("Movies found with less than " + movieLimit + " ratings ="+ totalMoviesLessThan20);
	  System.out.println("So total user after filtering ="+ (mainmh.getNumberOfUsers()-totalUsersLessThan20));
	  System.out.println("So total movies after filtering ="+ (mainmh.getNumberOfMovies()-totalMoviesLessThan20));
  
 }
	  	    
	  
/************************************************************************************************/
	 
	  
	public static void main(String arg[])	  
	{
		  	  
	/*	  for (int i=1;i<=5;i++)
		  {
			  String mFile = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Item based\\FiveFoldData\\DataFD\\sml_trainSetStoredFold" + (i) + ".dat";
			  System.out.println("Fold =" +(i));
			  AnalyzeAFile analyze =  new AnalyzeAFile(mFile);
			  analyze.checkFile();
			  
			  System.out.println("Done");
			  //analyze.statistics();
			  //analyze.calculateSparsity(mFile);
		 }*/
		  
		      
		      String path ="C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\SVD\\";
		      String train 	= path +  "sml_clusteringTrainSetStoredTF.dat";
		      String test	= path +  "sml_clusteringTestSetStoredTF.dat";	  
		      String  mFile =  path +  "sml_storedFeaturesRatingsTF.dat";
		    
		      System.out.println("Main File");
			  AnalyzeAFile analyze =  new AnalyzeAFile(mFile);
			  analyze.checkFile(3, 20);
			  
			  System.out.println("train File");
			  AnalyzeAFile trainAnalyze =  new AnalyzeAFile(train);
			  analyze.checkFile(3,20);
			  
			  System.out.println("test File");
			  AnalyzeAFile testAnalyze =  new AnalyzeAFile(test);
			  analyze.checkFile(3,20);
			  
			  		  
			  
			   System.out.println("Done");
			  //analyze.statistics();
			  //analyze.calculateSparsity(mFile);
	
		  
	  }
	  

	  
/**************************************************************************************************/
/**
 * Alanyze the contents of a file
 */
	//It will be called from other program. First call the default constructor
	public void analyzeContent(String mFile, int movieLimit, int userLimit)	  
	{
		  System.out.println("File="+mFile);
		  //AnalyzeAFile analyze =  new AnalyzeAFile(mFile);
		  //analyze.checkFile(movieLimit,userLimit);
		  System.out.println("Done");
//		  analyze.statistics();		  
		   
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
	  
	  /**
	   * Calculate the sparsity of the object passed 
	   */
	  
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
		 
		  double currentSparsityLevel  =  1- (actual/possible);	// 1 - (non-zero entries/total entries)
		  
		      System.out.println(" Sparsity "+ (currentSparsityLevel));
		//    System.out.println(" Sparsity in Current set is: " + (formatter.format(currentSparsityLevel)));
	    //    why null pointer error, even though formatter is not null
		  
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
// Important to Know
	
	//1: No. of Users who have rated less than 2 movies  = 0
	//2: No. of Movies rated by only one user = 140; (User-based will fail here)
	//3: No. of Movies rated by only two user = 68;  (User-based may fail here)
	
	// So dataset is baised, means user having atleast 20 movies are included and movies
	// rated by any of the user has been included.
	
	

/**************************************************************************************************/
	
	
	
	
	
	
}
