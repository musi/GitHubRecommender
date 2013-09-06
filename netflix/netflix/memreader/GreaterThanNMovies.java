package netflix.memreader;

import java.io.BufferedWriter;
import netflix.algorithms.memorybased.memreader.MyRecommender;
import netflix.utilities.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
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
public class GreaterThanNMovies 
/**************************************************************************************************/


{
	  private MemReader 	mr;
	  private int 			sparsityLevel;
	  private String 		outFileIdTr;			//train
	  private String 		outFileIdT;				//test
	  
	  private String 		outFileTrStore;			 
	  private String 		outFileTr;
	  private String        outFileT;
	  private String        myPath;
	  private NumberFormat  formatter;
	  private Random 		rand;
	  private double 		currentSparsity;  
	  int size[];
	  BufferedWriter outT;
	  BufferedWriter outTr;
	  BufferedWriter out;
	  MemHelper mainmh;
	  int usersWhoSawLessThan20;
	  int usersWhoSawMoreThan20;
	  int N;
	   
	  int totalMoviesAfterElimination;
	  int totalUsersAfterElimination;
	  int totalMoviesEliminated;
	  int totalUsersEliminated;

/*************************************************************************************************/
	//V.Imp --->  MovieLens data (SML) is already normalized .... Same is with ML
	 //           user have rated more than 20 ratings are there  
	  
	  
 public GreaterThanNMovies(int limit, boolean clustering)	  
  {
		  	N = limit; 
		  
		  // sml
	/*	    outFileIdT 	= "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Sparsity\\sml_testSet";
		    outFileIdTr = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Sparsity\\sml_sparse_trainSet";
		    myPath 		= "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Sparsity\\";
*/
		  	outFileIdT 	= "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\TestTrain\\sml_ratings.dat";
		    outFileIdTr = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\TestTrain\\sml_top20Ratings";
		    myPath 		= "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\TestTrain\\";

		  	
		    //ML
	/*		 outFileIdT 	= "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\ml_ratings";
			 outFileIdTr = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\ml_Top20Set";
			 myPath 		= "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\";
		*/		 	
		  	
		  	if (clustering)
		  	{
		  		outFileIdT 	= "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\TestTrain\\";
		  		outFileIdTr = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\TestTrain\\";
		  		myPath 		= "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\";
		  	}
		  
		  
		 
		    formatter = new DecimalFormat("#.#####");	//upto 4 digits
		    rand = new Random();
		    currentSparsity=0.0;
	
		 
		  
		     //outFileT = outFileIdT + n + ".dat";   //e.g. sml_test1.dat
		     //outFileTr = outFileIdTr + n + ".dat"; //e.g. sml_train1.dat
		     
		     outFileTr = outFileIdTr + "only"+N+".dat";
		     outFileT = outFileIdT + "sml_storedTestingData8.dat";
		     outFileTrStore = outFileIdTr + "only"+ N + "Stored.dat";
		     
		     //read the memHelper object containing original file, which we have to divide into sets
		     //MemHelper mainmh = new MemHelper(myPathS+ "sml_storedRatings.dat");
 		     //MemHelper mainmh = new MemHelper(myPath+ "sml_storedTrainingData8.dat");
   		     
		     	//SML
		         mainmh = new MemHelper(myPath+ "sml_storedRatings.dat");
		    
		       //ML
		       //mainmh = new MemHelper(myPath+ "ml_storedRatings.dat");
		     	
		     
   		    totalUsersAfterElimination = 0;
   		    totalMoviesAfterElimination = 0;
   		       

	  
	  }
	    
	  	  
/************************************************************************************************/
	 
	  
	  //read a file, write 10% in one file and the remaining 90% in another with same names (e.g. test and train set)
	 // repeat it for many times
	  
	  public void getDataFile(int GreaterThan, boolean clustering)	  
	  {
		  
	     N=GreaterThan;
	     
	     int uid;
	     IntArrayList allUsers;
	     LongArrayList movies;
	   	     
	     int ts=0;
	     int trs=0;
	     int all=0;
	     int mySize=0;
	     int currentCount=0;
	     
	     //_________________________________________________
	     
	      
	     	     
	     allUsers = mainmh.getListOfUsers(); //all users in the file
	    
	    	    	     
	      try	      
		  {	    	
		      outTr = new BufferedWriter(new FileWriter(outFileTr));	// we wanna write in o/p file
		      outT = new BufferedWriter(new FileWriter(outFileT ));		// we wanna write in o/p file
		      	
	      
	      for (int i = 0; i < allUsers.size(); i++) //go through all the users	    
	      {
	      	  uid 	 = allUsers.getQuick(i);
	    	  movies = mainmh.getMoviesSeenByUser(uid); //get movies seen by this user
	    
	    	  currentCount=0;
	    	  
	    	  if (clustering == true)
	    	  {
	    		  for (int k = 0; k < allUsers.size(); k++) //go through all the users	    		  
	    		   {  		  
	    	  
	    			  //code to make sure that each user has at least 10 movies in common 
	    			  	if (i!=k) { ArrayList<Pair> myRatings = mainmh.innerJoinOnMoviesOrRating(i, k, true);
	    			  				currentCount+=myRatings.size();
	    			  				if(currentCount>N) break;
	    	                		
	    			  	          }
	    		  	}
	    	  }//end of clustering loop
	    	  
	    	  //If it is not clustering then we make the next decision ok
	    	  else {currentCount = N+1;}
	    	  
	    	   mySize  = movies.size();
	    	   
	    	  //________________________________________________________________
	    	   //Filter Users
	    	   if (mySize >= N && currentCount>N) // how much movies a user must see to
	    	    {
	    		   totalUsersAfterElimination++;
	    		  for (int j = 0; j < mySize; j++) //for all movies	    		
	    		  {
	    			int mid = MemHelper.parseUserOrMovie(movies.getQuick(j));
	    			double rating 	= mainmh.getRating(uid, mid);		  
	    			totalMoviesAfterElimination++;
	    			
	    			//Filter Movies
	    			if (mainmh.getNumberOfUsersWhoSawMovie(mid) >1)	    			
	    			{
	    				String oneSample = (uid + "," + mid + "," + rating) ; //very important, we write in term of mid, uid, rating
	    				trs++;
	    				outTr.write(oneSample);
	    				outTr.newLine(); 
	    			} 
	    			 		  
	    		 }
	    		   
	    	   } //end of movies for writing
	    	 else  //we have eliminated this user -- count it
	    	 {
	    		 totalUsersEliminated++;
	    	 }
	    		
	    	     
	      }//end of all user for
	      
	         outTr.close();
	      
	     
	       	       
	  }// end of try
	      
	    catch (IOException e)
		  	
		  	{
			  System.out.println("Write error!  Java error: " + e);
			  System.exit(1);

		    } //end of try-catch     
	    
	    System.out.println("OK");
	    System.out.println(" Total Users Who>= "+ N + " Movies -->" + totalUsersAfterElimination);
	    System.out.println(" Total Users Eliminated (Movies< "+ N +")" + "  -->" + totalUsersEliminated);
	    System.out.println(" Total Movies Who>= "+ N + "Movies -->" + totalMoviesAfterElimination);
	    	
	  }
	      
	  
 /************************************************************************************************/
 /************************************************************************************************/

	public String getPath(int n)

	{
		  return (myPath);
	}




/************************************************************************************************/
/************************************************************************************************/
	  public static void main(String arg[])	  
	  {
		  int minSize = 20;
		  System.out.println(" Going to divide data into test and train data");
		  System.out.println(" Done ");
		  
		  GreaterThanNMovies gtN =  new GreaterThanNMovies(minSize, false);
		
		  //get file
		  gtN.getDataFile(minSize, false);
		
		  //write file into memory
		  MemReader myRd = new MemReader();
		
		  //SML
		   myRd.writeIntoDisk(gtN.outFileTr, gtN.myPath + "Sml_clusteringStoredRatings" + minSize + ".dat", false); //source, dest
		  
		  //ML
		  //myRd.writeIntoDisk(gtN.outFileTr, gtN.myPath + "sml_Top20StoredRatings.dat"); 
		  
		  
		  //make prediction for these files
		 // MyRecommender myR = new MyRecommender();
		 // myR.makeCorrPrediction(mainFile, testFile, path)
		  
		  		  	  
		  System.out.println("Ok done with intoruduction");		  
		   
	  }
	  
	

/**************************************************************************************************/

	  public void checkSizes(int n)
	
	  {
		  	for (int a=0;a<n;a++)
		  
		  	{
		  		System.out.println(" Total users who saw " +(a+1) + " movies : " + size[a+1]);
		  		
		  	}
			
		  	System.out.println(" Total users who saw <20  movies : " + usersWhoSawLessThan20);
		  	System.out.println(" Total users who saw >20  movies : " + usersWhoSawMoreThan20);
			
	  }
	  
	  
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

	
	void checkForValidity(String fileName)
	
	{
    
		int error=0;
		int total=0;

     try {
    	    Scanner in = new Scanner(new File(fileName));    // read from file the movies, users, and ratings, 

            String[] 	line;
            short 		mid;
            int 		uid;
            byte 		rating;
            String		date;
            
           // int myCheck=0;

            while(in.hasNextLine()) //it is parsing line by line
            
            {
               	
            	total++;
            	line = in.nextLine().split(",");		//delimiter
                
                mid = Short.parseShort(line[0]);
                uid = Integer.parseInt(line[1]);
                rating = Byte.parseByte(line[2]);

                if (mid==-1) error++;
                
                   
                 }
            
             }
        
        catch(FileNotFoundException e) {
            System.out.println("Can't find file " + fileName);
            e.printStackTrace();

        }
        catch(IOException e) {
            System.out.println("IO error");
            e.printStackTrace();
        }

        
        System.out.println("Error found in file  "+ fileName + "= "+ error + ", Total = "+total);
        
	} //end of function
	

/**************************************************************************************************/
	
	
	
	
	
	
}
