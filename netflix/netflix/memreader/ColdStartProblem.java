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
public class ColdStartProblem 
/**************************************************************************************************/


{
	  
	  private int 			sparsityLevel;
	  
	  private String 		outFileTr;
	  private String        outFileT;
	  private String 		mainFile;
	  private String        myPath;
	  private NumberFormat  formatter;
	  private Random 		rand;
	  private double 		currentSparsity;  
	  String 				destPath;
	  
	  int limitOnUsers;
	  int limitOnMovies;
	  
	  BufferedWriter outT;
	  BufferedWriter outTr;
	  MemHelper mainmh;
	     
	 /*************************************************************************************************/
	  
	  public ColdStartProblem (
			  					String mainSet,		// all the ratings
			  					int limitUsers, 	// choose 100 users e.g. 	
			  					int limitMovies,    //  e.g. select 1 movie for that user
			  					String dest		   //  where to write this file	
			  					)
			  					
	  
	  
	  {
		  	
		    formatter 		= new DecimalFormat("#.#####");	//upto 4 digits
		    rand 			= new Random();
		    currentSparsity	= 0.0;
		    limitOnUsers	= limitUsers;
		    limitOnMovies	= limitMovies;
		    destPath		= dest;	
		    mainmh			= new MemHelper(mainSet);
		    rand			= new Random();
		   
	  }
	    
	  	  
/************************************************************************************************/
	 
	  
	  //read a file, write 10% in one file and the remaining 90% in another with same names (e.g. test and train set)
	 // repeat it for many times
	  
	  public void getNewUserDataFile()
	  
	  {
		  
	         
	     int uid;
	     IntArrayList movies, allUsers;
	        
	     int ts=0;
	     int trs=0;
	     int all=0;
	     int mySize=0;
	     int howMuchUsers[] = new int [limitOnUsers];
	     int howMuchMovies[] = new int [limitOnMovies];
	     int usersCount=0;
	     int moviesCount=0;
	     
	     for (int t=0;t<limitOnUsers; t++)
	    	 howMuchUsers[t]= howMuchMovies[t]=-1;				//initailize
	     //_________________________________________________
	     
	      
	     	     
	     allUsers = mainmh.getListOfUsers(); //all users in the file
	     long randUser =0;
	     long randMovie =0;
	    
	    	     
	      try 
	      
		  {
	    	
	    	  outT = new BufferedWriter(new FileWriter(destPath));	// we wanna write in o/p file
	      
	      while (usersCount<limitOnUsers)
	    	  
	      {
	    	
	    	  boolean userAlreadyThere = false;
	    	    //randomly get any user, which is within the size (allUsers.size())
	    	  
	    		try  	{
  							randUser = rand.nextInt(allUsers.size()-1);  //select some random movies to delete (take their indexes) 
						}
	    		catch (Exception no){ System.out.println(" error in random numbers");
  								}

	       
	    		    int  myrandUser = (int)randUser;
	    		
	    			 for (int a= 0;a<limitOnUsers;a++)
	    		    		
	    			 {
	    				 
	    				 if (myrandUser==howMuchUsers [a]) {userAlreadyThere=true; break;} //if already want to delete this
	    				 
	    			 }
	    		
	    			 
	    	//__________________________________
	    	 // Now select some random movies
	    			 
	    		if (userAlreadyThere==false)
	    		
	    		{
	    			uid 	 = allUsers.getQuick(myrandUser);
	    	 		movies   = mainmh.getMoviesSeenByUser(uid); //get movies seen by this user
	    	 		mySize   = movies.size();
	    	 		
	    			while (moviesCount < limitOnMovies)
	    	
	    			{
	    				
	    				  boolean movieAlreadyThere = false;
	       			 	  
	      	    	    //randomly get any user, which is within the size (allUsers.size())
	      	    	  
	      	    		try  	{
	        							randMovie = rand.nextInt(mySize-1);  //select some random movies to delete (take their indexes) 
	      						}
	      	    		catch (Exception no){ System.out.println(" error in random numbers");
	        								}

	      	       
	      	    		    int  myrandMovie = (int)randMovie;
	      	    		
	      	    		    
	      	    			 for (int a= 0;a<limitOnMovies;a++)
	      	    		    		
	      	    			 {
	      	    			 
	      	    				 if (myrandMovie==howMuchMovies [a]) {movieAlreadyThere=true; break;} //if already want to delete this
	      	    				 
	      	    			 }
	      	    	
	      	    			 
	      	    			 if (movieAlreadyThere ==false) //writ thos movie and increment it as well
	      	    		
	      	    			 {
	      	    				int mid = MemHelper.parseUserOrMovie(movies.getQuick(myrandMovie));		 
	      	    				int rating 	= mainmh.getRating(uid, mid);
	    		
	      	    				String oneSample = (mid + "," + uid + "," + rating) ; //very important, we write in term of mid, uid, rating
	      		    			trs++;
	      		    			outTr.write(oneSample);
	      						outTr.newLine(); 
	      						 		
	      	    			 } //end of if movieAlreadyThere
	    			
	    			}// end of writing movies against one user
	    			
	    		}//end of if userAlreadythere
	    		
	      }//end the limitOnUser while
	      
	      System.out.println(" total>20 " + (trs+ts));
	       outTr.close(); 
	     
	       
	  }// end of try
	      
	    catch (IOException e)
		  	
		  	{
			  System.out.println("Write error!  Java error: " + e);
			  System.exit(1);

		    } //end of try-catch     
	    
	    System.out.println("OK");
	    	
	  }
	      
	  
/************************************************************************************************/
/************************************************************************************************/
	
	 public void getNewUserTestSet(String trainFile, int howMuchUsers, int howMuchMovies, String writeHere)
	  
	  {
		  
		  System.out.println(" Going to get a new user cold start problem test set");
		  
		  
		  ColdStartProblem csp= new ColdStartProblem (trainFile, howMuchUsers, howMuchMovies, writeHere );
		  
		  csp.getNewUserDataFile();
		  System.out.println(" Done ");
		  
	  }

	  
/************************************************************************************************/
/************************************************************************************************/
/*
	  
	  public static void main(String arg[])
	  
	  {
		  
		  System.out.println(" Going to divide data into test and train data");
		  System.out.println(" Done ");
		  
		  GreaterThanNMovies gtN =  new GreaterThanNMovies(20);
		
		  //get file
		  gtN.getDataFile(20);
		
		  //write file into memory
		  MemReader myRd = new MemReader();
		  myRd.writeIntoDisk(gtN.outFileTr, gtN.outFileTrStore); //source, dest
		  
		  
		  //make prediction for these files
		 // MyRecommender myR = new MyRecommender();
		 // myR.makeCorrPrediction(mainFile, testFile, path)
		  
		  
		  	  
		  System.out.println("Ok done with intoruduction");
		  
		   
	  }
	*/
	 
	

/**************************************************************************************************/
 /**************************************************************************************************/
	  
	public double calculateSparsity(MemHelper myObj)
	  
	 {
	   
		 int users  = myObj.getNumberOfUsers();
		 int movies = myObj.getNumberOfMovies();
		 
		 System.out.println(" Number of users:" + users);
		 System.out.println(" Number of movies:" + movies);
		 
		 		
		 double possible = users * movies;
		 double actual = myObj.getAllRatingsInDB();
		 
		  double currentSparsityLevel  = 	 1- (actual/possible);	// 1 - (non-zero entries/total entries)
		  System.out.println(" Sparsity in Current set is: " + formatter.format(currentSparsityLevel));
		  
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
	
	
	
	
	
	
}
