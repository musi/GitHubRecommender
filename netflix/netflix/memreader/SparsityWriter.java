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
import cern.colt.list.LongArrayList;

/**
 * Class which can write different kind of data, like all users ...who saw less the 20 movies etc
 * 
 * @author Musi
 */
/**************************************************************************************************/
public class SparsityWriter
{
	  private MemReader 	mr;
	  private int 			mySparsityLevel;
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
	  
	  //For checking the distribution Of Data
	  int userWhoSawExactly1;
	  int userWhoSawExactly2;
	  int userWhoSawExactly3;
	  int userWhoSawExactly4;
	  int userWhoSawExactly5;
	  int userWhoSawExactly6;
	  int userWhoSawExactly7;
	  int userWhoSawExactly8;
	  int userWhoSawExactly9;
	  int userWhoSawExactly10;
	  int usersWhoSawLessThan20;
	  int usersWhoSawMoreThan20;
	  int usersWhoSawLessThan5;
	  int usersWhoSawLessThan10;
	  int usersWhoSawBetween10And20;
	  int usersWhoSawBetween20And30;
	  int usersWhoSawBetween30And40;
	  int usersWhoSawBetween40And50;
	  int usersWhoSawBetween50And100;
	  int usersWhoSawBetween100And150;
	  int usersWhoSawBetween150And200;
	  int usersWhoSawBetween200And250;
	  int usersWhoSawMoreThan250;
	  
	     
  
/*************************************************************************************************/
	 /**
	  * default constructor 
	  */
	    
	  public SparsityWriter()
	  {
		  formatter = new DecimalFormat("#.#####");	//delare in both construcor, so it will be available in both objects
	  }

/*************************************************************************************************/	  
/**
 * 
 */
	 
	  public SparsityWriter( String mainFile, String writeHere,	String path	)	  
	  {
		  		  
			 outFileId	  = mainFile;
			 outFileIdTr  = writeHere;
			 myPath 	  = path;
			  
		  
		     rand = new Random();
		     currentSparsity=0.0;
	
		       
		     size = new int[21];
		     
		      userWhoSawExactly1 = 0;
			  userWhoSawExactly2 = 0;
			  userWhoSawExactly3 = 0;
			  userWhoSawExactly4 = 0;
			  userWhoSawExactly5 = 0;
			  userWhoSawExactly6 = 0;
			  userWhoSawExactly7 = 0;
			  userWhoSawExactly8 = 0;
			  userWhoSawExactly9 = 0;
			  userWhoSawExactly10 = 0;
			  
			 usersWhoSawLessThan5 = 0;
			 usersWhoSawLessThan10 = 0;
		     usersWhoSawLessThan20 = 0;
			 usersWhoSawMoreThan20 = 0;		
		
			 usersWhoSawMoreThan250 = 0;
			 usersWhoSawBetween10And20 = 0;
			 usersWhoSawBetween20And30 = 0;
			 usersWhoSawBetween30And40 = 0;
			 usersWhoSawBetween40And50 = 0;
			 usersWhoSawBetween50And100 = 0;
			 usersWhoSawBetween100And150 = 0;
			 usersWhoSawBetween150And200 = 0;
			 usersWhoSawBetween200And250 = 0;
			  
			  
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
	     IntArrayList allUsers;
	     LongArrayList movies;	     
	     int ts=0;
	     int trs=0;
	     int all=0;
	     int myMovSize=0;
	     int removeSize=0;
	     boolean sizeOne =false;		//so it shows, we shoudl remove users, who have seen only 1-2 movies etc?
	     boolean sizeTwo =false;
	     
	    
	     allUsers = mainmh.getListOfUsers(); //all users in the file	     
	     System.out.println("List of users: " + mainmh.getListOfUsers().size());	   
	     System.out.println("Number of users: " + mainmh.getNumberOfUsers());
	     	     
	     	     
	   try 	      
		{
	    	
	      outTr = new BufferedWriter(new FileWriter(outFileTr));	// write sparse file here
	      //outT = new BufferedWriter(new FileWriter(outFileT ));	// we wanna write in o/p file
	      
	      for (int i = 0; i < allUsers.size(); i++) //go through all the users 	    
	      {
	      	  uid 	 = allUsers.getQuick(i);
	    	  movies = mainmh.getMoviesSeenByUser(uid); //get movies seen by this user
	    	  
	    	  myMovSize  = movies.size();   
	    	  removeSize = (int) ((div) * myMovSize);	// 10% removing 	    	  
	    	  IntArrayList   movToBeRemoved  = new IntArrayList();	   	  
	    	  int index=0;  
	    	  
			  if (myMovSize==1) {size[myMovSize]++; sizeOne=true;}
			  if (myMovSize==2) {size[myMovSize]++; sizeTwo=true;}
			  
			  if (myMovSize <=20) 			  
			  		{ size[myMovSize]++;  usersWhoSawLessThan20 ++;}
			  else usersWhoSawMoreThan20++;	  				  
			   
	      if(removeSize>1)	    		  
	       {
	    	 //code to remove some rendom movies against a user  
	    	 // System.out.println("Current user saw Movies: " + myMovSize);
	    	  long del=0;	
	    	
	    	   while (true)	//loop untill true	    		 
	    		{	    		  
	    	  	    	   //generate a random number 
	    			 		try  				{del = rand.nextInt(myMovSize-1);  //select some random movies to delete (take their indexes) 
	    			 								
	    			 							}
	    			 		catch (Exception no){ System.out.println(" error in random numbers");
	    	    			 					}	    			 		
	    			 		
	    			      int  myDel = (int)del;							//Index
	    			      int dummyMid = (int) MemHelper.parseUserOrMovie (movies.getQuick(myDel));		//Movie id
	    			        			 
	    			      //If we have not already added this movie, add it and increase count
	    			      if(!(movToBeRemoved.contains(dummyMid)))		
	    			      {
	    			    	  movToBeRemoved.add(dummyMid);
	    			    	  index++;
	    			      }	    			      
	    			 
	    			 if(index== removeSize) break;
	    			 
	    		 }//end of while	    	  	
	    }// if removeSize >1
	    	  
	    	   // start writing	          
	    		for (int j = 0; j < myMovSize; j++) //for all movies	    		
	    		 {
	    			int mid = (int) MemHelper.parseUserOrMovie(movies.getQuick(j));
	    			
	    			if(!(movToBeRemoved.contains(mid)))		//we will write it	    	
	    			 {
	    				double rating = mainmh.getRating(uid, mid);	    				
	   	         	 	String oneSample = (uid + "," + mid + "," +  rating) ; //very important, we write in term of mid, uid, rating
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
	    	  
	    	//	System.out.println(" Done doing user " + i);
	    	     
	      }//end of all user for
	      
	       outTr.close(); 
	     //  outT.close();
	       
	  }// end of try
	      
	    catch (IOException e)		  	
		  	{
			  System.out.println("Write error!  Java error: " + e);
			  System.exit(1);

		    } //end of try-catch     
	    
	    System.out.println("myMovSize --------=" +myMovSize);
	    	
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
		boolean feature         = false;  

 //if(true == false) 
  {
		
 	for (int i= 0; i<=0;i++) //ML or SML (we are doing only SML)		
   	 {
		
	      for (int j=1; j<=10; j++)
	     //  for (double j=1; j<=1; j+=0.5)
 		 { 
			howMuchSparse = j/10.0;
			
		  //SML
		  
			if (i==0)				
			{
				//myMainTrain = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Sparsity\\sml_trainSetStoredAll_80.dat";
				//mySparseTrain = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Sparsity\\sml_dummyTrainAll_80";
				
				//Clusters
				myMainTrain = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\Sparsity\\sml_clusteringTrainSetStoredTF.dat";
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
		  
	     //FT
	     // myMainTrain = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\FT\\ft_storedFeaturesRatingsTF";
			  
			
		  SparsityWriter dis= new SparsityWriter(   myMainTrain,
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
		 		myRd.writeIntoDisk(dis.outFileTr, P + "sml_trainSetStoredAll_" + (j)+ ".dat", feature); //source, dest
		 		MemHelper myH = new MemHelper(P + "sml_trainSetStoredAll_" + (j)+ ".dat" );
				sLevel = dis.calculateSparsity(myH);
				myRd.writeIntoDisk(dis.outFileTr, P + "sml_trainSetStoredAll_" + dis.formatter.format(sLevel)+ ".dat", feature); //source, dest
		 	}
		 	
		 	else		 	
		 	    {	
		 			myRd.writeIntoDisk(dis.outFileTr, P + "sml_trainSetStoredTop20_80_" +(j*10) + ".dat", feature); //source, dest
		 			MemHelper myH = new MemHelper(P + "sml_trainSetStoredTop20_80_" +(j*10) + ".dat" );
					sLevel = dis.calculateSparsity(myH);
		 			myRd.writeIntoDisk(dis.outFileTr, P + "sml_trainSetStoredTop20_80_" + dis.formatter.format(sLevel)+ ".dat", feature); //source, dest
		 		}
		 	
		
		} //end of inner loop
	  }//end of outer loop	
  } //end of duumy id
 
   
  /*  myMainTrain = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\FT\\FTDataSet\\ft_storedFeaturesRatingsTF2.dat";
   //  myMainTrain = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\sml_storedFeaturesRatingsTF.dat";
     Sparsity mySpa = new Sparsity( myMainTrain, myMainTrain, myMainTrain);
     mySpa.checkSizes(new MemHelper(myMainTrain), true); //true for user's statistics
     mySpa.checkSizes(new MemHelper(myMainTrain), false);*/
 }
	
 /**************************************************************************************************/
 /**
  * 
  */
 	  public void checkSizes(MemHelper myHelper, boolean user)	
 	  {

		     IntArrayList allUsers 			= myHelper.getListOfUsers(); 		//all users in the file
		     IntArrayList allItems 			= myHelper.getListOfMovies(); 		//all users in the file
		     int myUserSize 				= allUsers.size();
		     int myItemSize 				= allItems.size();
		     int mySizes[]  				= new int [300]; 
		     int maxNoOfRatingsGivenToAMovie 	= 0;
		     int maxNoOfRatingsGivenByAUser  	= 0;
		     
		     int userSawAtLeastOneMovie 	= 0;
		     int MovIsSeenByAtLeastOneUser 	= 0;		     
		     int userSawMoreThanOneMovie 	= 0;
		     int MovIsSeenByMoreThanOneUser = 0;
		     
		     System.out.println("Ratings="+ myHelper.getAllRatingsInDB());
		     System.out.println("Avg Rating="+ myHelper.getGlobalAverage());
		     
		     if(user)
 		     {
 		    	// System.out.println("List of users: " + myUserSize);	
 			      for (int i = 0; i < myUserSize; i++) 				//go through all the users 	    
 			      {
 			      	  int uid = allUsers.getQuick(i);
 			    	  int movSeen =  myHelper.getNumberOfMoviesSeen(uid);   //get no. of movies seen by this user
 			    
 			    	  if(movSeen>0)
 			    		  userSawAtLeastOneMovie++;			    	  
 			    	  if(movSeen>1)
 			    		  userSawMoreThanOneMovie++;
 			    	  
 			    	  if(movSeen < 250)
 			    		  mySizes[movSeen]++;
 			    	  else 
 			    		  mySizes[299]++; 									//contains more than 300 movies size
 			    	  
 			    	 //Find a user, which has rated the max no. of movies, count this no. 
			    	  if(movSeen > maxNoOfRatingsGivenByAUser)
			    		  maxNoOfRatingsGivenByAUser = movSeen;
			    	  
 			       }	  
 		      } 
 		     
 		     
 		     else //Items's statistics
 		     {
 		    	// System.out.println("List of items: " + myItemSize);	
 				      for (int i = 0; i < myItemSize; i++) 							  //go through all the items 	    
 				      {
 				      	  int mid = allItems.getQuick(i);
 				    	  int movSeenBy =  myHelper.getNumberOfUsersWhoSawMovie(mid);   //get no. of movies seen by this user
 				    	  
 				    	 				    	  
 				    	  if(movSeenBy>0)
 				    		  MovIsSeenByAtLeastOneUser++;
 				    	  if(movSeenBy>1)
 				    		  MovIsSeenByMoreThanOneUser++;
 				    	  
 				    	  if(movSeenBy <=250)
 				    		  mySizes[movSeenBy]++;
 				    	  else 
 				    		  mySizes[299]++; 									//contains more than 300 movies size
 				    	  
 				    	  //find the mov, which has been seen by the max no. of users, find this count
				    	  if(movSeenBy > maxNoOfRatingsGivenToAMovie)
				    		  maxNoOfRatingsGivenToAMovie = movSeenBy;
				    	  
 				       }	       
 		     }
 		    	  
 		 
 		     		     
 		    // Users with less than movies 	  
 		  	for (int a=0;a<300;a++)		  
 		  	{
 		  		
 		  		if(a>=0 && a<=1){
 		  			userWhoSawExactly1+= mySizes[a];
 		  		  }		  		
 				if(a>1&& a<=2){
 					userWhoSawExactly2+= mySizes[a];
 				   }
 				if(a>2 && a<=3){
 					userWhoSawExactly3+= mySizes[a];	
 					}
 				if(a>3 && a<=4){
 					userWhoSawExactly4+= mySizes[a];
 					}
 				if(a>4 && a<=5){
 					userWhoSawExactly5+= mySizes[a];
 					}	  	     
 				   
 				  
 		  		if(a>=0 && a<=5)
 		  		{
 		  			//System.out.println(" Total users who saw " +(a) + " movies : " + mySizes[a]);
 		  			usersWhoSawLessThan5+=mySizes[a];
 		  			//System.out.println("usersWhoSawLessThan5-->"+usersWhoSawLessThan5);
 		  		}
 		  		
 		  		else if(a>5 && a<=10)
 		  		{
 		  			//System.out.println(" Total users who saw " +(a) + " movies : " + mySizes[a]);
 		  			usersWhoSawLessThan10+=mySizes[a];
 		  		}
 		  		
 		  		else if(a>10 && a<=20)
 		  		{
 		  			//System.out.println(" Total users who saw " +(a+1) + " movies : " + mySizes[a+1]);
 		  			usersWhoSawLessThan20+=mySizes[a];
 		  		}
 		  		
 		  		else if(a>20 && a<=30)
 		  		{
 		  			//System.out.println(" Total users who saw " +(a+1) + " movies : " + mySizes[a+1]);
 		  			usersWhoSawBetween20And30+=mySizes[a];
 		  		}
 		  		
 		  		else if(a>30 && a<=40)
 		  		{
 		  			//System.out.println(" Total users who saw " +(a+1) + " movies : " + mySizes[a+1]);
 		  			usersWhoSawBetween30And40+=mySizes[a];
 		  		}
 		  		
 		  		else if(a>40 && a<=50)
 		  		{
 		  			//System.out.println(" Total users who saw " +(a+1) + " movies : " + mySizes[a+1]);
 		  			usersWhoSawBetween40And50+=mySizes[a];
 		  		}

 		  		else if(a>50 && a<=100)
 		  		{
 		  			//System.out.println(" Total users who saw " +(a+1) + " movies : " + mySizes[a+1]);
 		  			usersWhoSawBetween50And100+=mySizes[a];
 		  		}

 		  		else if(a>100 && a<=150)
 		  		{
 		  			//System.out.println(" Total users who saw " +(a+1) + " movies : " + mySizes[a+1]);
 		  			usersWhoSawBetween100And150+=mySizes[a];
 		  		}
 		  	
 		  		else if(a>150 && a<=200)
 		  		{
 		  			//System.out.println(" Total users who saw " +(a+1) + " movies : " + mySizes[a+1]);
 		  			usersWhoSawBetween150And200+=mySizes[a];
 		  		}

 		  		else if(a>200 && a<=250)
 		  		{
 		  			//System.out.println(" Total users who saw " +(a+1) + " movies : " + mySizes[a+1]);
 		  			usersWhoSawBetween200And250+=mySizes[a];
 		  		}
 		  		
 		  		else 
 		  			{
 			  			//System.out.println(" Total users who saw " +(a+1) + " movies : " + mySizes[a+1]);
 			  			usersWhoSawMoreThan250+=mySizes[a];
 			  		}
 		  	}			

 		   
		     
		     
 		  	if(user)
 		  	{ 		  	 
 		  		System.out.println("Total Users who saw >0 mov ="+ userSawAtLeastOneMovie);
 		  		System.out.println("Total Users who saw >1 mov ="+ userSawMoreThanOneMovie);
 		  		
 		  		System.out.println("Max ratings given by a user ="+ maxNoOfRatingsGivenByAUser); 
 		     
 			    System.out.println("usersWhoSawLessThan5 =" + usersWhoSawLessThan5);
 			    System.out.println("usersWhoSawLessThan10 = " + usersWhoSawLessThan10);
 			    System.out.println("usersWhoSawLessThan20 = " + usersWhoSawLessThan20);
 			    System.out.println("usersWhoSawBetween20And30 = "+ usersWhoSawBetween20And30);
 			    System.out.println("usersWhoSawBetween30And40 = "+ usersWhoSawBetween30And40);
 			    System.out.println("usersWhoSawBetween40And50 = "+ usersWhoSawBetween40And50);		    														
 			    System.out.println("usersWhoSawBetween50And100 = "+ usersWhoSawBetween50And100);
 			    System.out.println("usersWhoSawBetween100And150 = "+ usersWhoSawBetween100And150);
 			    System.out.println("usersWhoSawBetween150And200 = "+ usersWhoSawBetween150And200);
 			    System.out.println("usersWhoSawBetween200And250 = "+ usersWhoSawBetween200And250);
 			    System.out.println("usersWhoSawMoreThan250 = "+ usersWhoSawMoreThan250);
 		  	}
 		  	
 		  	else
 		  	{  	 		     
 	 		    System.out.println("Movies Seen by >1 Users ="+ MovIsSeenByMoreThanOneUser);
 	 		    System.out.println("Movies Seen by >0 Users="+ MovIsSeenByAtLeastOneUser);
 	 		  
 	 		    System.out.println("Max ratings given to a movie ="+ maxNoOfRatingsGivenToAMovie);
 	 		  
 		  		System.out.println("MovieSeenByOnly1User =" + userWhoSawExactly1);
 		  		System.out.println("MovieSeenByOnly2User =" + userWhoSawExactly2);
 		  		System.out.println("MovieSeenByOnly3User =" + userWhoSawExactly3);
 		  		System.out.println("MovieSeenByOnly4User =" + userWhoSawExactly4);
 		  		System.out.println("MovieSeenByOnly5User =" + userWhoSawExactly5);
 		  		 
 			    System.out.println("MovieSeenByLessThan5 =" + usersWhoSawLessThan5);
 			    System.out.println("MovieSeenByLessThan10 = " + usersWhoSawLessThan10);
 			    System.out.println("MovieSeenByLessThan20 = " + usersWhoSawLessThan20);
 			    System.out.println("MovieSeenByBetween20And30 = "+ usersWhoSawBetween20And30);
 			    System.out.println("MovieSeenByBetween30And40 = "+ usersWhoSawBetween30And40);
 			    System.out.println("MovieSeenByBetween40And50 = "+ usersWhoSawBetween40And50);		    														
 			    System.out.println("MovieSeenByBetween50And100 = "+ usersWhoSawBetween50And100);
 			    System.out.println("MovieSeenByBetween100And150 = "+ usersWhoSawBetween100And150);
 			    System.out.println("MovieSeenByBetween150And200 = "+ usersWhoSawBetween150And200);
 			    System.out.println("MovieSeenByBetween200And250 = "+ usersWhoSawBetween200And250);
 			    System.out.println("MovieSeenByMoreThan250 = "+ usersWhoSawMoreThan250);
 		  	}
 		  	
 		    usersWhoSawLessThan5 		= 0;
 		    usersWhoSawLessThan10 		= 0;
 		    usersWhoSawLessThan20 		= 0;
 		    usersWhoSawBetween20And30	= 0;
 		    usersWhoSawBetween30And40	= 0;
 		    usersWhoSawBetween40And50	= 0;		    														
 		    usersWhoSawBetween50And100 	= 0;
 		    usersWhoSawBetween100And150	= 0;
 		    usersWhoSawBetween150And200 = 0;
 		    usersWhoSawBetween200And250 = 0;
 		    usersWhoSawMoreThan250		= 0;
 		    
 		      userWhoSawExactly1 = 0;
 			  userWhoSawExactly2 = 0;
 			  userWhoSawExactly3 = 0;
 			  userWhoSawExactly4 = 0;
 			  userWhoSawExactly5 = 0;
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
	 * For 100,000:Sparsity level = 0.9369
	 * For 80,000: Sparsity level = 0.9495
	 * For 70,000: Sparsity level = 0.9558
	 * For 60,000: Sparsity level = 0.9621
	 * For 50,000: Sparsity level = 0.9684
	 * For 40,000: Sparsity level = 0.9747
	 * For 30,000: Sparsity level = 0.9810
	 * For 20,000: Sparsity level = 0.9872
	 * For 10,000: Sparsity level = 0.9936
	 * For 5,000: Sparsity level = 0.9968
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
