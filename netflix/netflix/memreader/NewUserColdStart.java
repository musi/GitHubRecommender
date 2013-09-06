package netflix.memreader;


//delete this and may u have to write efficient code for 80% train and 20% test set

import java.io.BufferedWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;
import cern.colt.list.IntArrayList;
import cern.colt.list.LongArrayList;



public class NewUserColdStart 
{
  private MemReader 	mr;
  private int 			crossValidation;
	
  private String 		outFileTr,outFileTr2,outFileTr5,outFileTr10,outFileTr15,outFileTr20 ;				
  private String        outFileT;				//write buffers
  private String        outFileTAndTr;
	
							//train and test set files to be written
  private String        trainSetFileName,trainSetFileName2,trainSetFileName5,trainSetFileName10,trainSetFileName15,trainSetFileName20;
  private String        testSetFileName;
  private String        trainAndTestSetFileName;	//both Train and test set to be written
    
  private String        myPath;
  private Random 		rand;
  MemHelper 			mainMh;
  MemHelper             coldMh;
  int 					moviesSizeToBeConsidered;
  int 					userSizeToBeConsidered;
  int                   dataSetFlg;
  int	 		 		coldStartThreshold;
  
 /*************************************************************************************************/
  /**
   * @author Musi
   * @param int,    coldStartThreshold
   * @param String, MainMemHelper file to be divided
   * @param String, MainMemHelper file which contain info abt the cold users
   * @param String, Test buffer
   * @param String, Train buffer
   * 
   * @param String, Train buffer for 2 movies as a limit for cold start users
   * @param String, Train buffer for 5 movies as a limit for cold start users
   * @param String, Train buffer for 10 movies as a limit for cold start users
   * @param String, Train buffer for 15 movies as a limit for cold start users
   * @param String, Train buffer for 20 movies as a limit for cold start users
   * @param String, TrainAndTest buffer
   * @param String, TrainingSetFile Name for 2 movies as a limit for cold start users
   * @param String, TrainingSetFile Name for 5 movies as a limit for cold start users
   * @param String, TrainingSetFile Name for 10 movies as a limit for cold start users
   * @param String, TrainingSetFile Name for 15 movies as a limit for cold start users
   * @param String, TrainingSetFile Name for 20 movies as a limit for cold start users
   * @param String, TestSetFile Name
   * @param String, TrainAndTestSetFile Name
   * @param int,    movieSize to Be considered
   * @param int,    userSize to Be considered
   * @param int,    0=sml, 1=ml, 2=ft
   * 
   */
  public NewUserColdStart  ( int 	coldStartThreshold,
		  					 String myMh, 
		  					 String coldName,
		  					 String outFileTr,
		  					 String outFileTr2,
		  					 String outFileTr5, 
		  					 String outFileTr10,       
		  					 String outFileTr15, 
		  					 String outFileTr20, 
		  					 String outFileT, 
		  					 String outFileTAndTr,
		  					 String trainSetFileName,
		  					 String trainSetFileName2, 
		  					 String trainSetFileName5,
		  					 String trainSetFileName10,
		  					 String trainSetFileName15,
		  					 String trainSetFileName20,
		  					 String testSetFileName,		  				
		  					 String trainAndTestSetFileName,
			  				int moviesSizeToBeConsidered,
	  						int userSizeToBeConsidered,
	  						int dataSetFlg) 
  
  {
	  	this.outFileTr	 				= outFileTr;
	    this.outFileTr2 				= outFileTr2;
	    this.outFileTr5 				= outFileTr5;
	    this.outFileTr10 				= outFileTr10;
	    this.outFileTr15 				= outFileTr15;
	    this.outFileTr20 				= outFileTr20;
	    this.outFileT		 			= outFileT;
	    this.outFileTAndTr				= outFileTAndTr;
	    
	    this.trainSetFileName			= trainSetFileName;
	    this.trainSetFileName2			= trainSetFileName2;
	    this.trainSetFileName5			= trainSetFileName5;
	    this.trainSetFileName10			= trainSetFileName10;
	    this.trainSetFileName15			= trainSetFileName15;
	    this.trainSetFileName20			= trainSetFileName20;
	    this.testSetFileName			= testSetFileName;
	    this.trainAndTestSetFileName	= trainAndTestSetFileName;	  
	      
	    
	    this.moviesSizeToBeConsidered 	= moviesSizeToBeConsidered;
	    this.userSizeToBeConsidered 	= userSizeToBeConsidered;
	    this.dataSetFlg					= dataSetFlg;				//0=sml, 1=ml, 2=ft
	    this.coldStartThreshold			= coldStartThreshold;
	    mainMh 							= new  MemHelper(myMh);
	    coldMh							= new  MemHelper(coldName);
	    rand 							= new Random();
  }
  
 /************************************************************************************************/
  
  
  public NewUserColdStart(int n)
  {
	  crossValidation=n;
  }
  
 /************************************************************************************************/
 
  
  //read a file, write 10% in one file and the remaining 90% in another with same names (e.g. test and train set)
 // repeat it for many times
  
  public void readData(double div, boolean clustering)  
  {
	  
     int uid;
     LongArrayList movies; 
     IntArrayList allUsers;
              
   //Buffers for writing outut files
     BufferedWriter outT;		
     BufferedWriter outTr, outTr2,outTr5,outTr10,outTr15,outTr20;			
     BufferedWriter outTAndTr;
        
     int ts=0;
     int trs=0;
     int all=0;
     
     //------------------------------------------------------------------------------
     //Do Filtering here: Filter all movies, rated by less than a specified threshold
     //------------------------------------------------------------------------------     
     //--------------------------
     // Movie Filtering
     //--------------------------
     IntArrayList doNotIncludeTheseMovies = new IntArrayList();	//movies have less than prespecified threshold
     IntArrayList allMovies = mainMh.getListOfMovies();
     int noOfItems = allMovies.size();
     int movieSize = 0;
     LongArrayList usersWhoSawCurrentMovie;
     
  // for all items
  for(int j=1; j<noOfItems;j++)
  {				 
   	  int  mid = (j);
	  usersWhoSawCurrentMovie =  mainMh.getUsersWhoSawMovie(mid);
	  movieSize = usersWhoSawCurrentMovie.size();
	  
	  if(movieSize<moviesSizeToBeConsidered)
	  {
		  doNotIncludeTheseMovies.add(mid);		  
	
	  }
  }
  
  System.out.println("Movers are :" + allMovies.size());
  System.out.println("Movies less than "+ moviesSizeToBeConsidered + "size ="+ doNotIncludeTheseMovies.size());
	
     //--------------------------
     // User Filtering
     //--------------------------	  
	  
       IntArrayList doNotIncludeTheseUsers = new IntArrayList();	//users have less than prespecified threshold
	   IntArrayList  myUsers = mainMh.getListOfUsers(); //all users in the file
	   int userSize = 0;
	   LongArrayList moviesSeenByCurrentUser;
	     
	   for (int i = 0; i < myUsers.size(); i++) //go through all the users	    
	   {
	      	uid = myUsers.getQuick(i);
	      	moviesSeenByCurrentUser = mainMh.getMoviesSeenByUser(uid); //get movies seen by this user
	      	userSize = moviesSeenByCurrentUser.size();

	      	 if (userSize<userSizeToBeConsidered)
			 {
	      		doNotIncludeTheseUsers.add(uid);
	      	}
	      	 
       }// end of for
     
	   System.out.println("Users are :" + myUsers.size());
	   System.out.println("Users less than "+ userSizeToBeConsidered + "size ="+ doNotIncludeTheseUsers.size());
     
	   
	     //------------------------------------------------------------------------------
	     // Divide into Test and Train set
	     //------------------------------------------------------------------------------  
     
     allUsers = mainMh.getListOfUsers(); //all users in the file
    
      try      
	  {
    		outT 	= new BufferedWriter(new FileWriter(outFileT));				// we wanna write in o/p file
    		outTr 	= new BufferedWriter(new FileWriter(outFileTr));	
    		outTr2 	= new BufferedWriter(new FileWriter(outFileTr2));
    		outTr5 	= new BufferedWriter(new FileWriter(outFileTr5));		
    		outTr10 = new BufferedWriter(new FileWriter(outFileTr10));		
    		outTr15 = new BufferedWriter(new FileWriter(outFileTr15));		
    		outTr20 = new BufferedWriter(new FileWriter(outFileTr20));		
    		outTAndTr = new BufferedWriter(new FileWriter(outFileTAndTr));	
        		
    		
	    	int 		 allSamples					 = 0;    		
	       	IntArrayList TheseAreColdStartUsers 	 = coldMh.getListOfUsers();	      	
	      	userSize 								 = allUsers.size();
	     	    	    	
   //------------------------------------------------------------------------------------------- 	
    //Same as usual, but we will reduce the number of movies per CODLUSERS to a limit	
     for (int i = 0; i < userSize; i++) 			 //go through all the users    
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

    	int totalProcessed = 0;    	  
    	int del=0;
    	int mid=0;
    	long tMid=0;
    	double rating =0;
    	
    	//------------------------------------------------------------------------------
    	//New User Cold Start
    	// Assumption, (1) Take randomly selected 100 users...write these users into a MemReader (Object)
    	// (2) then write this dataset into memReadre (object)
    	// Idea is we can get MAE for these users or for the whole dataset.
    	// Define threshold, a user shld have very few items rated in the training set 
    
    	    	
	if(!doNotIncludeTheseUsers.contains(uid))
	{
    	IntArrayList movieAlreadyThere = new IntArrayList();
    	
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
  											
	  			if (!(movieAlreadyThere.contains(mid))) //if movie has not already been processed
	  			 {	  				
	  						movieAlreadyThere.add(mid);   	
		  			
			  				rating = mainMh.getRating(uid, mid);			  				
		  				    String oneSample = (uid + "," + mid + "," + rating) ; //very important, we write in term of  uid, mid, rating
		    			    
		  				    outT.write(oneSample);		
		  				    outT.newLine();
		  				    outTAndTr.write(oneSample);			    
							outTAndTr.newLine();
							ts++;
							
			  				allSamples++;
			  				totalProcessed++;
		  			    
						//System.out.println("ts"+ (ts) + " with size= "+ mySize + " processed = "+ totalProcessed );
		  				
	  			  }	 // finished writing test  
	  			
	  			
	  		 } //end of if
  			
    		
    		//--------------------------------------------------
    		//cold start generation randomly
    		
  			 else  	  				  
  	  		  {  	
  				 int currentPoint =0;
  				 int expectedMidIndex = 0;  				 
  				 IntArrayList MoviesRatedByAnExpectedColdStartUser = new IntArrayList();
  				
  				while(true)
  				{
  					//break when we finished processing the no. of movies we need 
  					if(currentPoint == coldStartThreshold)
  						break;
  					
  					if(totalProcessed == mySize-10)
  						break;

		  				//generate a random number 
		 		 		try  				{expectedMidIndex = (int) rand.nextInt(mySize-1);  //select some random movies to delete (take their indexes) 
		 		 							}
		 		 		
		 		 		catch (Exception no){ System.out.println(" error in random numbers");
		   			 					}

		 		 	
		 		 		
		 		 		mid = MemHelper.parseUserOrMovie(movies.getQuick(expectedMidIndex)); 
		 		  	
		 		 		//System.out.println(MoviesRatedByAnExpectedColdStartUser.contains(mid));
		 		 		
				 		 	// If we have not already processed this movie
				 		 	if(MoviesRatedByAnExpectedColdStartUser.contains(mid)==false)
				 		 	{		
				 		 		MoviesRatedByAnExpectedColdStartUser.add(mid);
				 		 		
				 		 		// Do movie Size Thresholding
								//if(!doNotIncludeTheseMovies.contains(mid))
								{	
												//if this movie is there in test set or already in training set?														
												if(movieAlreadyThere.contains(mid)==false)
												{
														    movieAlreadyThere.add(mid);
											  				rating = mainMh.getRating(uid, mid);
													  		String oneSample = (uid + "," + mid + "," + rating) ; //very important, we write in term of mid, uid, rating
													  	    
													  		//2
													  		if(currentPoint<2)
													  		{
														  		outTr2.write(oneSample);
														  		outTr2.newLine();
													  		}
													  		
													  		//5
													  		if(currentPoint<5)
													  		{
														  		outTr5.write(oneSample);
														  		outTr5.newLine();
													  		}
													  		
													  		//10
													  		if(currentPoint<10)
													  		{
														  		outTr10.write(oneSample);
														  		outTr10.newLine();
													  		}
													  		
													  		//15
													  		if(currentPoint<15)
													  		{
														  		outTr15.write(oneSample);
														  		outTr15.newLine();
													  		}
													  		
													  		//20
													  		if(currentPoint<20)
													  		{
														  		outTr20.write(oneSample);
														  		outTr20.newLine();
													  		}
													  		
													  		outTAndTr.write(oneSample);			    
															outTAndTr.newLine();
															outTr.write(oneSample);
													  		outTr.newLine();
													  		
													  		
															trs++;															
															currentPoint++;
															allSamples++;
											  				totalProcessed++;
												}											  						
								  						
								  		} //end if  			
								  				
								  }//end inner if
						} //end while
  				
  			//	System.out.println("End writing movies for cold-start for one user, totalProcessed="+totalProcessed);

  				
  				//Now add the remaing movies
  	  			 for (int k=0;k<mySize;k++)
  			  	  {
  			  				    mid = MemHelper.parseUserOrMovie(movies.getQuick(k));  					 		
  			  					
  			  				     			  					
  			  				    if ((movieAlreadyThere.contains(mid)==false))
  			  				    {  
  			  				    
  			  				    	rating = mainMh.getRating(uid, mid);
	  					  			String oneSample = (uid + "," + mid + "," + rating) ; //very important, we write in term of mid, uid, rating
	  					  	    	
		  					  		outTr.write(oneSample);
							  		outTr.newLine();
							  		
  					  					//Add extra movies, only if this user is not cold-start user
  					  					if(TheseAreColdStartUsers.contains(uid)==false)
  					  					{
	  					  				  
	  					  				    //2
	  					  				    outTr2.write(oneSample);
	  					  					outTr2.newLine();
	  					  					
	  					  					//5
		  					  				outTr5.write(oneSample);
	  					  					outTr5.newLine();
	  					  					
	  					  					//10
	  					  					outTr10.write(oneSample);
						  					outTr10.newLine();
						  					
						  					//15
						  					outTr15.write(oneSample);
	  					  					outTr15.newLine();
	  					  					
	  					  					//20
	  					  					outTr20.write(oneSample);
						  					outTr20.newLine();
						  					
						  					outTAndTr.write(oneSample);			    
	  										outTAndTr.newLine();
	  										trs++;	
	  					  				}
  					  					
  					  					else //for cold-start users
  					  					{
  					  					//2
									  		if(currentPoint<2)
									  		{
										  		outTr2.write(oneSample);
										  		outTr2.newLine();
									  		}
									  		
									  		//5
									  		if(currentPoint<5)
									  		{
										  		outTr5.write(oneSample);
										  		outTr5.newLine();
									  		}
									  		
									  		//10
									  		if(currentPoint<10)
									  		{
										  		outTr10.write(oneSample);
										  		outTr10.newLine();
									  		}
									  		
									  		//15
									  		if(currentPoint<15)
									  		{
										  		outTr15.write(oneSample);
										  		outTr15.newLine();
									  		}
									  		
									  		//20
									  		if(currentPoint<20)
									  		{
										  		outTr20.write(oneSample);
										  		outTr20.newLine();
									  		}
									  			
  					  					}
  					  					
  					  					allSamples++;
  					  				    totalProcessed++;
  				  					    //System.out.println("trs "+ (trs));
  					  				
  					  				
  				  				 }
  			  	  	 } //end for
  			  		   	    		  		  
  				
		 } //end of train set			  
    		 
    	
	       	
        } //end of while
	}//end of user Thresholing
	
    	if(i%200 ==0 && i>=0)
    		 System.out.println("processing user " +i);
    		
   } //end of all users 
      
      System.out.println("Test = " + ts + " Train= " +trs + " all= "+all + " sum = " + (ts+trs) + " all="+allSamples);
      
      //close the files
      outT.close();
      outTr.close();
      outTr2.close();
      outTr5.close();
      outTr10.close();
      outTr15.close();
      outTr20.close();
      outTAndTr.close(); 
      
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
	    int dataSetChoice 		   = 0;			// 0=sml, 1=ml, 2=ft
	   	int	trainingOrAllDivision  = 0;			// 0 =all, 1=training
	   	int NewUserThreshold       = 20;
	   	
	    //This movie should be included by that much no. of users for consideration, and same for users
	    int moviesSizeToBeConsidered = 0; //(e.g. 2 means>=1)
	    int usersSizeToBeConsidered  = 0;    
	   
	    //declare var
	    String coldStartUsersObj = "", t="", tAndTr="", p="", pm="", m="";
	    String testSetName="", trainAndTestSetName="";
	    String tr2="", tr5="", tr10="",tr15="",tr20="", tr25="", tr="";							//test set is same, the training set is diff
	    String trainSetName2 ="",  trainSetName5 ="", trainSetName10 ="", 
	    	   trainSetName15 ="", trainSetName20 ="" , trainSetName25 ="", trainSetName="";
	    
	    System.out.println(" Going to divide data into test and train data");
	    
	    if(dataSetChoice ==0)
	    {
	/*	    t  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\sml_TestSet.dat";
		    tr = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\sml_TrainSet.dat";
		    tAndTr = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\sml_MainSet.dat";
		    p  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\";
		    pm  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\";
	*/
		    
	/*	    t  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\NB\\sml_TestSet.dat";
		    tr = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\NB\\sml_TrainSet.dat";
		    tAndTr = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\NB\\sml_MainSet.dat";
		    p  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\NB\\";
		    pm  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\NB\\";	    
	*/ 	  
	    	p  		= "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/FiveFoldData/111/";
	    	t  		= p+ "sml_TestSet.dat";
		    tr2 	= p+ "sml_TrainSet2.dat";
		    tr5 	= p+ "sml_TrainSet5.dat";
		    tr10 	= p+ "sml_TrainSet10.dat";
		    tr15 	= p+ "sml_TrainSet15.dat";
		    tr20 	= p+ "sml_TrainSet20.dat";
		    tr25 	= p+ "sml_TrainSet25.dat";
		    tr 		= p+ "sml_TrainSet.dat";
		    tAndTr 	= p+ "sml_MainSet.dat";
		    
		    
		    //coldStartUsersObj = p+ "sml_StoredColdUsers100.dat";
		      coldStartUsersObj = p+ "sml_StoredColdUsers50.dat";
		    
		    pm  = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/FiveFoldData/111/";	    
	 	  
		    m = pm + "sml_storedFeaturesRatingsTF.dat";
	    	
	    	
/*	    	//To filter the movies
	    	trainSetName = p + "sml_clusteringTrainSetStoredTF" +(moviesSizeToBeConsidered-1)+".dat";
	    	testSetName = p + "sml_clusteringTestSetStoredTF" +(moviesSizeToBeConsidered-1)+".dat";		  	
		  	trainAndTestSetName = p + "sml_modifiedStoredFeaturesRatingsTF" +(moviesSizeToBeConsidered-1)+".dat";
			
*/
			//do not need to filter movies
		  	trainSetName2  = p + "sml_clusteringTrainSetStoredTF50_2.dat";
		  	trainSetName5  = p + "sml_clusteringTrainSetStoredTF50_5.dat";
		  	trainSetName10 = p + "sml_clusteringTrainSetStoredTF50_10.dat";
		  	trainSetName15 = p + "sml_clusteringTrainSetStoredTF50_15.dat";
		  	trainSetName20 = p + "sml_clusteringTrainSetStoredTF50_20.dat";
		  	trainSetName25 = p + "sml_clusteringTrainSetStoredTF50_25.dat";
		  	trainSetName   = p + "sml_clusteringTrainSetStoredTF50.dat";
	    	testSetName = p + "sml_clusteringTestSetStoredTF50.dat";		  	
		  	trainAndTestSetName = p + "sml_modifiedStoredFeaturesRatingsTF50.dat";
		  	  	
		  	//---------------------------------
		  	/*//Feature Play for SVD data division
		  	t = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/FeaturesPlay/sml_testSet.dat";
		    tr = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/FeaturesPlay/sml_trainSet.dat";
		    tAndTr = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/FeaturesPlay/sml_mainSet.dat";
			p = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/FeaturesPlay/";
			pm = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/FeaturesPlay/";
			m = pm + "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/FeaturesPlay/sml_clusteringTrainSetStoredTF.dat/";
	    	
			m = pm + "sml_storedFeaturesRatingsTF.dat";
			
			//do not need to filter movies
		  	trainSetName = p + "sml_train_trainSetStoredFold1.dat";
	    	testSetName = p + "sml_train_testSetStoredFold1.dat";		  	
		  	trainAndTestSetName = p + "sml_train_modifiedSetStoredFold1.dat";*/
			
	    	
	    	//----------------------------------------
	    	// Need to have X% data, with x=variable (x=training set size)
		  	   
	    }
	    
	    else if(dataSetChoice ==1)
	    {
	    
	        t  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\SVD\\ml_TestSet.dat";
		    tr2 = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\SVD\\ml_TrainSet.dat";
		    tAndTr = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\SVD\\ml_MainSet.dat";
		    p  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\SVD\\";
		    pm  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\SVD\\";
		    
		    
		    
		  //To filter the movies
/*	    	trainSetName = p + "ml_clusteringTrainSetStoredTF" +(moviesSizeToBeConsidered-1)+".dat";
	    	testSetName = p + "ml_clusteringTestSetStoredTF" +(moviesSizeToBeConsidered-1)+".dat";		  	
		  	trainAndTestSetName = p + "ml_modifiedStoredFeaturesRatingsTF" +(moviesSizeToBeConsidered-1)+".dat";
			*/
		  	
		    if(trainingOrAllDivision==0)
		    {
		    m = pm + "ml_storedFeaturesRatingsTF.dat";
		    
		  //To filter the movies
	    	/*trainSetName = p + "ml_clusteringTrainSetStoredTF" +(moviesSizeToBeConsidered-1)+".dat";
	    	testSetName = p + "ml_clusteringTestSetStoredTF" +(moviesSizeToBeConsidered-1)+".dat";		  	
		  	trainAndTestSetName = p + "ml_modifiedStoredFeaturesRatingsTF" +(moviesSizeToBeConsidered-1)+".dat";
			*/
		    
		  	//do not need to filter mvoies
		  	trainSetName2 = p + "ml_clusteringTrainSetStoredTF.dat";
	    	testSetName = p + "ml_clusteringTestSetStoredTF.dat";		  	
		  	trainAndTestSetName = p + "ml_modifiedStoredFeaturesRatingsTF.dat";
		    }
		    
		    else
		    {
		    	m = p + "ml_clusteringTrainSetStoredTF.dat";
		    	
			  	//For dividing training set into validation and test set
		    	//do not need to filter mvoies
			  	trainSetName2 = p + "ml_clusteringTrainingTrainSetStoredTF.dat";
		    	testSetName = p + "ml_clusteringTrainingValidationSetStoredTF.dat";		  	
			  	trainAndTestSetName = p + "ml_modifiedTrainingStoredFeaturesRatingsTF.dat";
		    }
	    }
	    
	    else if(dataSetChoice ==2)
	    {
	    	t  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\FT\\Clustering\\ft_TestSet.dat";
	    	tr2 = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\FT\\Clustering\\ft_TrainSet.dat";
	    	tAndTr = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\FT\\Clustering\\ft_MainSet.dat";
	    	p  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\FT\\Clustering\\";
	    	pm  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\FT\\Clustering\\";
	    
	    	 m = pm + "ft_storedFeaturesRatingsTF.dat";
	    	//m = pm + "ft_storedFeaturesRatingsTF10.dat";
	    	 
	    	/*//To filter the movies
		      trainSetName = p + "ft_clusteringTrainSetStoredTF" +(moviesSizeToBeConsidered-1)+".dat";
		      testSetName = p + "ft_clusteringTestSetStoredTF" +(moviesSizeToBeConsidered-1)+".dat";		  	
			  trainAndTestSetName = p + "ft_modifiedStoredFeaturesRatingsTF" +(moviesSizeToBeConsidered-1)+".dat";
			*/	

	    	 //SVDs
	    	 int xFactor = 20;	    	 
	    	 String path ="C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\FT\\SVD\\" + xFactor +"\\" ;

    	 	 t  	= 		path +  "ft_TestSet.dat";
	    	 tr2 	=  		path +  "ft_TrainSet.dat";
	    	 tAndTr =  	    path +  "ft_MainSet.dat";
	    	 p  	= 		path ;
	    	 pm  	= 		path;
		    
	    	 m = pm + "ft_storedFeaturesRatingsBothTF5.dat";
	    	 //m = pm + "ft_storedFeaturesRatingsTF10.dat";
		    	 
	    	 
	    	 
	    	 
	    	 //do not need to filter mvoies
			  trainSetName2 = p + "ft_clusteringTrainSetStoredTF5.dat";
		      testSetName = p + "ft_clusteringTestSetStoredTF5.dat";		  	
			  trainAndTestSetName = p + "ft_modifiedStoredFeaturesRatingsTF5.dat";
				
	    }
	  
  	      
  	      //call constructor and methods for dividing
	  	   NewUserColdStart dis= new NewUserColdStart ( NewUserThreshold,			//How much movies a new user shld have rated?
	  			   									    m,					   		//main memHelper file
	  			   									    coldStartUsersObj,			//cold-start users stored in a Memreader object
		  			   									tr,
	  			   									    tr2,						//Train write Buffers
				  			   							tr5,
				  			   							tr10,					
				  			   							tr15,
				  			   							tr20,
		  			   									t, 							//Test write Buffer
				  			   						    tAndTr,						//Modified Main write Buffer
				  			   						    trainSetName,
				  			   						    trainSetName2,				//TrainSet name to be written
				  			   							trainSetName5,				
				  			   							trainSetName10,				
				  			   							trainSetName15,				
				  			   							trainSetName20,				
				  			   							testSetName,				//TestSet name to be written
				  			   							trainAndTestSetName,		//Train and TestSet name to be written	  			   							
				  			   							moviesSizeToBeConsidered,	//Min Movies size
				  	   									usersSizeToBeConsidered,	//Min User Size
				  	   									dataSetChoice);				//0=sml, 1=ml,2=ft
	  	   	
	  	   //read the data and divide
	  	   dis.readData(0.8, false);	   
	  	   MemReader myReader = new MemReader();	 
	  	 
	  	 
	  	   //write into memReader objs
	 	   myReader.writeIntoDisk(t, testSetName, false); 		
		   myReader.writeIntoDisk(tr2,  trainSetName2, false);  //diff training sets
		   myReader.writeIntoDisk(tr5,  trainSetName5, false);	
		   myReader.writeIntoDisk(tr10,  trainSetName10, false);	
		   myReader.writeIntoDisk(tr15,  trainSetName15, false);	
		   myReader.writeIntoDisk(tr20,  trainSetName20, false);
		   myReader.writeIntoDisk(tr,  trainSetName, false);	
		   myReader.writeIntoDisk(tAndTr,  trainAndTestSetName, false);	  	  

		   // Analyze file for statistics
		   AnalyzeAFile testAnalyzer  = new AnalyzeAFile ();	   
		  // testAnalyzer.analyzeContent(testName, moviesSizeToBeConsidered, 20);
		 /*  testAnalyzer.analyzeContent(modifiedName, moviesSizeToBeConsidered, usersSizeToBeConsidered);
		   testAnalyzer.analyzeContent(testName, 0, 0);		   
		   testAnalyzer.analyzeContent(trainName, 0 , 0);
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
  //Imprtant: How to call this method from other class
  //1- Call the constrcutor with all parameters
  //2- then call this method
  
  /** 
   * Divide into test and train objects: we can do include only movies or users which have rated
   * a specified no. of movies
   * @param mainFile , main memHelper file String
   * @param trainFile , train memHelper file String
   * @param testFile , test memHelper file String
   * @param trainAndTestFile , testAndTrain memHelper file String (modified writing)
   * @param minMoviesSize, A movie should be rated by that much guys for including into the writing 
   * @param minUsersSize,  A user should have rated more than  this thrshold
   * @param divisionFactor, e.g. 20-80 (test set factor) 
   */
/*  public void divideIntoTestTrain (double divisionFactor, boolean clust )  
  {
	  
	  System.out.println(" Going to divide data into test and train data");	  
	  
	  //Call read data and divide into sets method
	  //note parameters have been set through the constrcutor
	   readData(divisionFactor, clust);
	   
	   //MemReader object
  	   MemReader myReader = new MemReader();	 
  	 
  	 
  	   //write into memReader objs
 	   myReader.writeIntoDisk(outFileT, testSetFileName, true); 		
	   myReader.writeIntoDisk(outFileTr,  trainSetFileName, true);	
	   //myReader.writeIntoDisk(outFileTAndTr, trainAndTestSetFileName);  //there is no need of it here	 
	 
	   System.out.println(" Done ");
	  
  }
  */
  /*************************************************************************************************/
  /*************************************************************************************************/
  
  
  
}
