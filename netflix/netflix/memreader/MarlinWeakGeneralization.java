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



public class MarlinWeakGeneralization 
{
  private MemReader 	mr;
  private int 			crossValidation;
  private String 		outFileIdTr;			//train
  private String 		outFileIdT;				//test
  
  private String 		outFileTr;				
  private String        outFileT;				//write buffers
  private String        outFileTAndTr;
  
  private String        trainSetFileName;    		//train and test set files to be written
  private String        testSetFileName;
  private String        trainAndTestSetFileName;	//both Train and test set to be written
  
  
  private String        myPath;
  private Random 		rand;
  MemHelper 			mainMh;
  int 					moviesSizeToBeConsidered;
  int 					userSizeToBeConsidered;
  int                   dataSetFlg;
  
  
 /*************************************************************************************************/
  /**
   * @author Musi
   * @param String, MainMemHelper file to be divided
   * @param String, Training buffer
   * @param String, Test buffer
   * @param String, TrainAndTest buffer
   * @param String, TrainingSetFile Name
   * @param String, TestSetFile Name
   * @param String, TrainAndTestSetFile Name
   * @param int,    movieSize to Be considered
   * @param int,    userSize to Be considered
   * @param int,    0=sml, 1=ml, 2=ft
   * 
   */
  public MarlinWeakGeneralization   (String myMh, 
		  				String outFileTr, 
		  				String outFileT, 
		  				String outFileTAndTr,
		  				String trainSetFileName, 
		  				String testSetFileName,		  				
		  				String trainAndTestSetFileName,
  						int moviesSizeToBeConsidered,
  						int userSizeToBeConsidered,
  						int dataSetFlg) 
  
  {
	    this.outFileTr 					= outFileTr;
	    this.outFileT		 			= outFileT;
	    this.outFileTAndTr				= outFileTAndTr;
	    this.trainSetFileName			= trainSetFileName;    
	    this.testSetFileName			= testSetFileName;
	    this.trainAndTestSetFileName	= trainAndTestSetFileName;	    
	    
	    this.moviesSizeToBeConsidered 	= moviesSizeToBeConsidered;
	    this.userSizeToBeConsidered 	= userSizeToBeConsidered;
	    this.dataSetFlg					= dataSetFlg;		//0=sml, 1=ml, 2=ft
	    
	    mainMh 							= new  MemHelper(myMh);
	    rand 							= new Random();
  }
  
 /************************************************************************************************/
  
  
  public MarlinWeakGeneralization(int n)
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
     LongArrayList movies; 
     IntArrayList allUsers;
              
     BufferedWriter outT;		
     BufferedWriter outTr;			//Buffers for writing outut files
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
     IntArrayList doNotIncludeTheseMovies	 = new IntArrayList();	//movies have less than prespecified threshold
     IntArrayList allMovies 				 = mainMh.getListOfMovies();
     int noOfItems 							 = allMovies.size();
     int movieSize 							 = 0;
     LongArrayList 	usersWhoSawCurrentMovie	 = null;
     
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
	  
     // Users having less than 20 ratings are filtered out
       IntArrayList doNotIncludeTheseUsers	 	= new IntArrayList();	//users have less than prespecified threshold
	   IntArrayList  myUsers 					= mainMh.getListOfUsers(); 			//all users in the file
	   int noOfUsers 							= allMovies.size();
	   int userSize 							= 0;
	   LongArrayList moviesSeenByCurrentUser;
	     
	   for (int i = 0; i < myUsers.size(); i++) 						//go through all the users	    
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
    		outT = new BufferedWriter(new FileWriter(outFileT));			// we wanna write in o/p file
    		outTr = new BufferedWriter(new FileWriter(outFileTr));			// we wanna write in o/p file
    		outTAndTr = new BufferedWriter(new FileWriter(outFileTAndTr));	// we wanna write in o/p file
   		
     //_________________________________________________________________________________
    		
    		
    	
    	int allSamples =0;    		
    	userSize = allUsers.size();
    
    	//make the random set
    	allUsers.shuffle();
    	
    	IntArrayList userAlreadyThere = new IntArrayList();
    	int delUid = 0;
    	int tUid   = 0;
        int totalUsersForMarlin = 5000;
        int userCounter = 0;
    	
    	for (int i = 0; i < userSize; i++) 			 //go through all the users    
        {              	
    		   		
    		
          uid = allUsers.getQuick(i);
          
	      movies = mainMh.getMoviesSeenByUser(uid); //get movies seen by this user
    
    	  int mySize= movies.size();
    	  all+=mySize;
    	  
    	  /*int trainSize = mySize -1;				// All but one protocol
    	  if (trainSize==0) trainSize=1;
    	  int testSize  = 1;
    	    */
    	  
    	  // As all but one protocol, hence results would be diff for each parition.
    	  // we can claim, it was not a fair claim, so we randomly divided it into a test
    	  // set and a training set and then take the average.
    	  // We can claim that, diff partitions were creating different results, hence, we
    	  // opt for the second scheme, which was more "fair" 
    	  int trainSize = (int) ((div) * mySize);	//80%-20%
    	  if (trainSize==0) trainSize=1;
    	  int testSize  = mySize - trainSize;
    	  
    	//Enter some sort of randomization
    	int totalProcessed 	= 0;    	  
    	int del				= 0;
    	int mid				= 0;
    	long tMid			= 0;
    	double rating 		= 0;
    	
    	
	if(!doNotIncludeTheseUsers.contains(uid))
	{
		if(userCounter++ == totalUsersForMarlin)
			break;
		
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
		  				
		  				// Do movie Size Thresholding
		  				if(!doNotIncludeTheseMovies.contains(mid))
		  				{
			  				rating = mainMh.getRating(uid, mid);			  				
		  				    String oneSample = (uid + "," + mid + "," + rating) ; //very important, we write in term of  uid, mid, rating
		    			    
		  				    outT.write(oneSample);		
		  				    outT.newLine();
		  				    outTAndTr.write(oneSample);			    
							outTAndTr.newLine();
							ts++;
		  				}
		  				
		  				allSamples++;
		  				totalProcessed++;
		  			    
						//System.out.println("ts"+ (ts) + " with size= "+ mySize + " processed = "+ totalProcessed );
		  				
	  			  }	 // finished writing test  
	  		 } //end of if
  			
  			 else  	  				  
  	  		  {  				 
  				 for (int k=0;k<mySize;k++)
		  	  		{
		  				    mid = MemHelper.parseUserOrMovie(movies.getQuick(k));  					 		
		  					  				  
		  				    if (!(movieAlreadyThere.contains(mid)))
		  				    {
				  				
				  				// Do movie Size Thresholding
				  				if(!doNotIncludeTheseMovies.contains(mid))
				  				{
				  				    rating = mainMh.getRating(uid, mid);
				  				    String oneSample = (uid + "," + mid + "," + rating) ; //very important, we write in term of mid, uid, rating
				  	    	
				  				    outTr.write(oneSample);
				  					outTr.newLine();
				  					outTAndTr.write(oneSample);			    
									outTAndTr.newLine();
									trs++;	
				  				}
				  				
				  				allSamples++;
				  				totalProcessed++;
			  					//System.out.println("trs "+ (trs));
			  				 }
		  	  		  }
		  	   } //end of train set			  
    		  		  
  				  		  
            }//end of while
	}//end of user Thresholing
	
    	if(i%200 ==0 && i>=0)
    		 System.out.println("processing user " +i);
    		
   } //end of all users 
      
      System.out.println("Test = " + ts + " Train= " +trs + " all= "+all + " sum = " + (ts+trs) + " all="+allSamples);
      outT.close();
      outTr.close();
      outTAndTr.close(); 
      
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
	    int dataSetChoice 		   = 1;			//0=sml, 1=ml, 2=ft
	   	int	trainingOrAllDivision  = 0;			// 0 =all, 1=training
	   	
	    //This movie should be included by that much no. of users for consideration, and same for users
	    int moviesSizeToBeConsidered = 0; //(e.g. 2 means>=1)
	    int usersSizeToBeConsidered  = 0;    
	    int marlinSplit			     = 3 ;
	    
	    //declare var
	    String t="", tr="", tAndTr="", p="", pm="", m="";
	    String trainSetName ="", testSetName="", trainAndTestSetName="";
	    
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

	    	t  = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/FiveFoldData/sml_TestSet.dat";
		    tr = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/FiveFoldData/sml_TrainSet.dat";
		    tAndTr = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/FiveFoldData/sml_MainSet.dat";
		    p  = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/FiveFoldData/";
		    pm  = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/FiveFoldData/";	    
	 	  
		    m = pm + "sml_storedFeaturesRatingsTF.dat";
	    	
	    	
/*	    	//To filter the movies
	    	trainSetName = p + "sml_clusteringTrainSetStoredTF" +(moviesSizeToBeConsidered-1)+".dat";
	    	testSetName = p + "sml_clusteringTestSetStoredTF" +(moviesSizeToBeConsidered-1)+".dat";		  	
		  	trainAndTestSetName = p + "sml_modifiedStoredFeaturesRatingsTF" +(moviesSizeToBeConsidered-1)+".dat";
			
*/
			//do not need to filter movies
		  	trainSetName = p + "sml_clusteringTrainSetStoredTF.dat";
	    	testSetName = p + "sml_clusteringTestSetStoredTF.dat";		  	
		  	trainAndTestSetName = p + "sml_modifiedStoredFeaturesRatingsTF.dat";
		
		  	
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
	    
	        /*t  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\SVD\\ml_TestSetMarlinWeak20_"+marlinSplit+".dat";
		    tr = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\SVD\\ml_TrainSetMarlinWeak20_"+marlinSplit+".dat";
		    tAndTr = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\SVD\\ml_MainSetMarlinWeak20_"+marlinSplit+".dat";
		    */
		    
	    	t  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\SVD\\ml_TestSetMarlinWeakFixed20_"+marlinSplit+".dat";
		    tr = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\SVD\\ml_TrainSetMarlinWeakFixed20_"+marlinSplit+".dat";
		    tAndTr = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\SVD\\ml_MainSetMarlinWeakFixed20_"+marlinSplit+".dat";
		    
		    
/*	    	t  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\SVD\\ml_TestSetMarlin20_"+marlinSplit+".dat";
		    tr = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\SVD\\ml_TrainSetMarlin20_"+marlinSplit+".dat";
		    tAndTr = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\SVD\\ml_MainSetMarlin20_"+marlinSplit+".dat";
		    */
		    
		    p  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\SVD\\";
		    pm  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\SVD\\";
		    
		    
		    
		  //To filter the movies
/*	    	trainSetName = p + "ml_clusteringTrainSetStoredTF" +(moviesSizeToBeConsidered-1)+".dat";
	    	testSetName = p + "ml_clusteringTestSetStoredTF" +(moviesSizeToBeConsidered-1)+".dat";		  	
		  	trainAndTestSetName = p + "ml_modifiedStoredFeaturesRatingsTF" +(moviesSizeToBeConsidered-1)+".dat";
			*/
		  	
		    if(trainingOrAllDivision==0)
		    {
		    	m = pm + "ml_storedRatingsMarlinNorTF20_"+marlinSplit+".dat";
		    	//m = pm + "ml_storedFeaturesRatingsNorTF.dat";
		    
		    
		  //To filter the movies
	    	/*trainSetName = p + "ml_clusteringTrainSetStoredTF" +(moviesSizeToBeConsidered-1)+".dat";
	    	testSetName = p + "ml_clusteringTestSetStoredTF" +(moviesSizeToBeConsidered-1)+".dat";		  	
		  	trainAndTestSetName = p + "ml_modifiedStoredFeaturesRatingsTF" +(moviesSizeToBeConsidered-1)+".dat";
			*/
		    
		  	//do not need to filter mvoies
		  	trainSetName = p + "ml_clusteringTrainSetStoredTF.dat";
	    	testSetName = p + "ml_clusteringTestSetStoredTF.dat";		  	
		  	trainAndTestSetName = p + "ml_modifiedStoredFeaturesRatingsTF.dat";
		  	
		    }
		    
		    else
		    {
		    	    m = p + "ml_storedRatingsMarlinNorTF20_"+marlinSplit+".dat";
		        //      m = pm + "ml_storedFeaturesRatingsNorTF.dat";
		    	
			  	//For dividing training set into validation and test set
		    	//do not need to filter mvoies
			  	trainSetName = p + "ml_clusteringTrainingTrainSetStoredTF.dat";
		    	testSetName = p + "ml_clusteringTrainingValidationSetStoredTF.dat";		  	
			  	trainAndTestSetName = p + "ml_modifiedTrainingStoredFeaturesRatingsTF.dat";
		    }
	    }
	    
	    else if(dataSetChoice ==2)
	    {
	    	t  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\FT\\Clustering\\ft_TestSet.dat";
	    	tr = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\FT\\Clustering\\ft_TrainSet.dat";
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
	    	 tr 	=  		path +  "ft_TrainSet.dat";
	    	 tAndTr =  	    path +  "ft_MainSet.dat";
	    	 p  	= 		path ;
	    	 pm  	= 		path;
		    
	    	 m = pm + "ft_storedFeaturesRatingsBothTF5.dat";
	    	 //m = pm + "ft_storedFeaturesRatingsTF10.dat";
		    	 
	    	 
	    	 
	    	 
	    	 //do not need to filter mvoies
			  trainSetName = p + "ft_clusteringTrainSetStoredTF5.dat";
		      testSetName = p + "ft_clusteringTestSetStoredTF5.dat";		  	
			  trainAndTestSetName = p + "ft_modifiedStoredFeaturesRatingsTF5.dat";
				
	    }
	  
  	      
  	      //call constructor and methods for dividing
	  	   MarlinWeakGeneralization dis= new MarlinWeakGeneralization(m,							//main memHelper file
	  			   							tr,							//Train write Buffer
	  			   							t, 							//Test write Buffer
	  			   							tAndTr,						//Modified Main write Buffer
	  			   							trainSetName,				//TrainSet name to be written
	  			   							testSetName,				//TestSet name to be written
	  			   							trainAndTestSetName,		//Train and TestSet name to be written	  			   							
	  			   							moviesSizeToBeConsidered,	//Min Movies size
	  	   									usersSizeToBeConsidered,	//Min User Size
	  	   									dataSetChoice);				//0=sml,1=ml,2=ft
	  	   	
	  	   //read the data and divide
	  	   dis.readData(0.8, false);	   
	  	   MemReader myReader = new MemReader();	 
	  	 
	  	 
	  	/*   //write into memReader objs
	 	   myReader.writeIntoDisk(t, testSetName, false); 		
		   myReader.writeIntoDisk(tr,  trainSetName, false);	
		   myReader.writeIntoDisk(tAndTr,  trainAndTestSetName, false);	  	  
*/
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
  public void divideIntoTestTrain (double divisionFactor, boolean clust )  
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
  
  /*************************************************************************************************/
  /*************************************************************************************************/
  
  
  
}
