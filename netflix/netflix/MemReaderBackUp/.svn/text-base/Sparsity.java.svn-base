package netflix.memreader;

import java.io.BufferedWriter;
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
public class Sparsity 
/**************************************************************************************************/
{
	  private MemReader 	mr;
	  private int 			sparsityLevel;
	  private String 		outFileIdTr;			//train
	  private String 		outFileIdT;				//test
	  private String 		outFileId;				//main
	  private String 		outFileTr;
	  private String        outFileT;
	  private String        myPath;
	  private NumberFormat  formatter;
	  private Random 		rand;
	  private double 		currentSparsity;  
	  int size[];
	  BufferedWriter outT;
	  BufferedWriter outTr;
	  MemHelper mainmh;
	  int usersWhoSawLessThan20;
	  int usersWhoSawMoreThan20;
	     
	  int justForJoke;
	  
	 /*************************************************************************************************/
	  
	  public Sparsity()
	  {
		  formatter = new DecimalFormat("#.#####");	//delare in both construcor, so it will be available in both objects
	  }
	  
	  public Sparsity( String mainFile, String writeHere,	String path	)	  
	  {
		   // sml
		    /*outFileIdT 	= "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Sparsity\\sml_testSet";
		    outFileIdTr = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Sparsity\\sml_sparse_trainSet";
		    myPath 		= "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Sparsity\\";
		 */
		  
		  outFileId	  = mainFile;
		  outFileIdTr = writeHere;
		  myPath 	  = path;
		  
		  
		     rand = new Random();
		     currentSparsity=0.0;
	
		       
		     size = new int[21];
		     usersWhoSawLessThan20=0;
			 usersWhoSawMoreThan20=0;
			       
		     //outFileT = outFileIdT + n + ".dat";   //e.g. sml_test1.dat
		     //outFileTr = outFileIdTr + n + ".dat"; //e.g. sml_train1.dat
		     
		      mainmh = new MemHelper(outFileId);		
		      formatter = new DecimalFormat("#.#####");	//upto 4 digits
	  
	  }
	    
	  	  
/************************************************************************************************/
	 
	  
	  //read a file, write 10% in one file and the remaining 90% in another with same names (e.g. test and train set)
	 // repeat it for many times
	  
	  public void IntroduceSparsity(double div, boolean testFile) 	  
	  {
		 outFileTr = outFileIdTr + "0" + div + ".dat";
		          
	     int uid;
	     IntArrayList movies, allUsers;	     
	     int ts=0;
	     int trs=0;
	     int all=0;
	     int mySize=0;
	     int removeSize=0;
	     boolean sizeOne =false;		//so it shows, we shoudl remove users, who have seen only 1-2 movies etc?
	     boolean sizeTwo =false;
	     
	     //_________________________________________________	     	      
	     	     
	     allUsers = mainmh.getListOfUsers(); //all users in the file
	    
	     //As there were initially, 943 users, so nearlly we have to remove 10 ratings from each user
	     //so nearlly every user has rated 1000 * 80 movies = 80,000
	     /* If some user have 5 ratings, remove 1
	      * If some user have 10 ratings, remove 2
	      * If some user have 15 ratings, remove 3 ...
	      * Simple remove 10% ratings from each user account
	      */
	     	   
	   //Returns a list filled with all keys contained in the receiver. (custToMovie.keys())
	     System.out.println("List of users: " + mainmh.getListOfUsers().size());
	      
	   //Returns the number of (key,value) associations currently contained. 
	     System.out.println("Number of users: " + mainmh.getNumberOfUsers());
	     
	     
	     	     
	      try 	      
		  {
	    	
	      outTr = new BufferedWriter(new FileWriter(outFileTr));	// write sparse file here
	      //outT = new BufferedWriter(new FileWriter(outFileT ));	// we wanna write in o/p file
	      
	      for (int i = 0; i < allUsers.size(); i++) //go through all the users 	    
	      {
	      	  uid 	 = allUsers.getQuick(i);
	    	  movies = mainmh.getMoviesSeenByUser(uid); //get movies seen by this user
	    
	    	  
	    	   mySize     = movies.size();
	    	   removeSize = (int) ((div) * mySize);	// 10% removing
	    
	    	  
	    	  int doNotWriteTheseMovies[]	 = new int[removeSize]; 			// 0-removeSize (e..g 0-5)
	    	  int writeTheseMovies[] 		 = new int[mySize];
	    	  int writeTheseRatings[] 		 = new int[mySize];
	    	  int writeTheseMovies1[] 		 = new int[mySize];	    	  
	    	  
	    	  
	    	  int index=0;
	    	    
	    	  
	    	  for (int j = 0; j < mySize; j++) //for all movies		    		
	    	   {
	    			  all++;
	   	    		  writeTheseMovies[j]  = writeTheseMovies[j] = MemHelper.parseUserOrMovie(movies.getQuick(j));
	   	    		  writeTheseRatings[j]	= mainmh.getRating(uid, writeTheseMovies[j]);		  
	    	   }
	    	  
			  if (mySize==1) {size[mySize]++; sizeOne=true;}
			  if (mySize==2) {size[mySize]++; sizeTwo=true;}
			  
			  if (mySize <=20) 			  
			  		{ size[mySize]++;  usersWhoSawLessThan20 ++;}
			  else usersWhoSawMoreThan20++;	  
				  
			   
	    	  if(removeSize>1)	    		  
	    	  {
	    	 //code to remove some rendom movies against a user
	    		  
	    		  for (int a=0;a<removeSize; a++)
		    		  doNotWriteTheseMovies[a]=-1;	//initialise
		    	  
	    	  System.out.println("Current user saw Movies: " + mySize);
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
	    			    
	    			        			 
	    			 for (int a= 0;a<removeSize;a++)	    		
	    			 {
	    				 
	    				 if (myDel==doNotWriteTheseMovies [a]) {dontWrite=true; break;} //if already want to delete this
	    				 
	    			 }
	    			 
	    			 if (dontWrite == false) 
	    				 {
	    				    doNotWriteTheseMovies[index]= myDel;
	    				    writeTheseMovies[myDel]= -1;			//If it is -1, it means we are not writing it
	    				    index++;
	    				 }
	    			 
	    			 if(index== removeSize) break;
	    			 
	    		 }//end of while

	    	  	
	    	  //____________________________________________
	    	  	
	    	  }// if removeSize >1
	    	  
	    	  
	    	  
	    		
	    	  // start writing	          
	    		for (int j = 0; j < mySize; j++) //for all movies	    		
	    		 {
	    		
	    			
	    			if(writeTheseMovies[j] != -1)		//we will write it
	    	
	    			 {
	   	         	 	String oneSample = (writeTheseMovies[j] + "," + uid + "," + writeTheseRatings[j]) ; //very important, we write in term of mid, uid, rating
	    			    trs++;
	    			    outTr.write(oneSample);
						outTr.newLine(); 
					  
	    			  
	    			 }
	    	
	    		  //there is no need of test-set
	    		/*	
	    			else 
	    		     
	    		     {
	    		    	 if (testFile)
	    		    	 {
	    		    	   	 	String oneSample = (writeTheseMovies1[j] + "," + uid + "," + writeTheseRatings[j]) ; //very important, we write in term of mid, uid, rating
	    	    			    ts++;
	    	    			    outT.write(oneSample);
	    						outT.newLine(); 
	    				
	    		     	  }
	    		     }
	    		  */
	    			
	    		 }//end of movies for writing
	    	  
	    		System.out.println(" Done doing user " + i);
	    	     
	      }//end of all user for
	      
	       outTr.close(); 
	     //  outT.close();
	       
	  }// end of try
	      
	    catch (IOException e)		  	
		  	{
			  System.out.println("Write error!  Java error: " + e);
			  System.exit(1);

		    } //end of try-catch     
	    
	    System.out.println("mySize --------=" +mySize);
	    	
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
		  
		double howMuchSparse 	= 0.1;
		String myMainTrain 		= ""; 
		String mySparseTrain	= "";
		String P				= "";
		
		
 	for (int i= 0; i< 2;i++) //ML or SML (we are doing only SML)		
   	 {
		
		for (int j=1; j<10; j++)			
		{ 
			howMuchSparse = j/10.0;
			
		  //SML
		  
			if (i==0)				
			{
				//myMainTrain = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Sparsity\\sml_trainSetStoredAll_80.dat";
				//mySparseTrain = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Sparsity\\sml_dummyTrainAll_80";
				
				//Clusters
				myMainTrain = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\Sparsity\\sml_TrainSet80.dat";
				mySparseTrain = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\Sparsity\\sml_dumTrainAll.dat";
				 
			}
			
			else			
			{
				myMainTrain = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Sparsity\\sml_trainSetStoredTop20_80.dat";
				mySparseTrain = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Sparsity\\sml_dummtTrainTop20_80";
		
			}	
			
			//P = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Sparsity\\";
			P = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\Sparsity\\";
		 
		  //ML
		/*  String myMainTrain = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\Sparsity\\sml_trainSetStoredAll_80.dat";
		  //String myMain = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\Sparsity\\sml_trainSetStoredTop20_80.dat";
		    String mySparseTrain = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\Sparsity\\ml_testSetAll";
		    String P = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\Sparsity\\";
		*/
		  
		  
		  Sparsity dis= new Sparsity(myMainTrain,
				  					 mySparseTrain,
				  					 P);
		  
		  dis.currentSparsity = dis.calculateSparsity(dis.mainmh);
		  
		  dis.IntroduceSparsity(howMuchSparse, false);
		  
		  System.out.println("Ok done with intoruduction");
		 
		 //  dis.checkForValidity(dis.outFileTr);
		 // dis.checkForValidity(dis.outFileT);
		 // dis.checkSizes(20);
		  
		  //write into disc
		  MemReader myRd = new MemReader();
		  
		  double sLevel=0.0;
		  
		 	if (i==0)		 	
		 	{
		 		myRd.writeIntoDisk(dis.outFileTr, P + "sml_trainSetStoredAll_80_" + (j*10)+ ".dat"); //source, dest
		 		MemHelper myH = new MemHelper(P + "sml_trainSetStoredAll_80_" + (j*10)+ ".dat" );
				sLevel = dis.calculateSparsity(myH);
				myRd.writeIntoDisk(dis.outFileTr, P + "sml_trainSetStoredAll_80_" + dis.formatter.format(sLevel)+ ".dat"); //source, dest
		 	}
		 	
		 	else		 	
		 	    {	
		 			myRd.writeIntoDisk(dis.outFileTr, P + "sml_trainSetStoredTop20_80_" +(j*10) + ".dat"); //source, dest
		 			MemHelper myH = new MemHelper(P + "sml_trainSetStoredTop20_80_" +(j*10) + ".dat" );
					sLevel = dis.calculateSparsity(myH);
		 			myRd.writeIntoDisk(dis.outFileTr, P + "sml_trainSetStoredTop20_80_" + dis.formatter.format(sLevel)+ ".dat"); //source, dest
		 		}
		 	
		
		} //end of inner loop
	  }//end of outer loop	
		
		  
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
		  
		  
		  return (Double.parseDouble(formatter.format(currentSparsityLevel)));
		  
		  
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
	
	public void comeToMe()
	{
		System.out.println("I came to u");
	}
	
	
	
	
}
