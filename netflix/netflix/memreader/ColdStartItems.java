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



public class ColdStartItems 
{
  private MemReader 	mr;
  private int 			crossValidation;
  private String 		outFileIdTr;			//train
  private String 		outFileIdT;				//test
  private String 		outFileColdId;			//cold users
  
  private String 		outFileTr;				
  private String        outFileT;				//write buffers
  private String        outFileTAndTr;
  private String 		outFileCold;					
  
  private String        trainSetFileName;    		//train and test set files to be written
  private String        testSetFileName;
  private String        trainAndTestSetFileName;	//both Train and test set to be written
  private String        coldUsersFileName;
  
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
   * @param String, ColdUsers Buffer
   * @param String, ColdUsersFile Name
   * @param int,    movieSize to Be considered
   * @param int,    userSize to Be considered
   * @param int,    0=sml, 1=ml, 2=ft
   * 
   */
  public ColdStartItems   (  String myMh, 		  					
		  					 String outFileCold,
		  					 String coldUsersFileName,
		  					 int moviesSizeToBeConsidered,
		  					 int userSizeToBeConsidered,
		  					 int dataSetFlg) 
  
  {
	  
	    this.outFileCold				= outFileCold;	     
	    this.coldUsersFileName			= coldUsersFileName;   
	    this.moviesSizeToBeConsidered 	= moviesSizeToBeConsidered;
	    this.userSizeToBeConsidered 	= userSizeToBeConsidered;
	    this.dataSetFlg					= dataSetFlg;					//0=sml, 1=ml, 2=ft
	    
	    mainMh 							= new  MemHelper(myMh);
	    rand 							= new Random();
  }
  
 /************************************************************************************************/
  
  
  public ColdStartItems(int n)
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
              
     BufferedWriter outColdItems;
     
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
	   int noOfUsers = allMovies.size();
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
     
	   System.out.println("Going to find cold start users");
	   
     
    
      try      
	  {
    	 		outColdItems = new BufferedWriter(new FileWriter(outFileCold));	// we wanna write in o/p file
     //_________________________________________________________________________________
    		
    	
    	    		
    	movieSize = allMovies.size();
    
    	IntArrayList TheseAreColdStartItems 	 = new IntArrayList();
    	int		     dummyMid 					 = 0;
    	int			 totalColdItemsLimit 		 = 50;
    	int			 totalColdItems 	 	     = 0;	
    	
    	
    	for (int i = 0; i < movieSize; i++) 			 //go through all the users    
        {  
    		if(totalColdItems ==totalColdItemsLimit)
    			break;
    		
    		//generate a random number 
		 		try  				{dummyMid = (int) rand.nextInt(movieSize-1);  //select some random movies to delete (take their indexes) 
		 							}
		 		
		 		catch (Exception no){ System.out.println(" error in random numbers");
  			 					}
		 		
		 		int mid = allMovies.getQuick(dummyMid);
		 		String oneSample = (1 + "," + mid + "," + 1) ; //very important, we write in term of  uid, mid, rating
 			    
		 		  if(TheseAreColdStartItems.contains(mid)==false)       
		 	       {
		 			    TheseAreColdStartItems.add(mid);		 			  	
		 			    outColdItems.write(oneSample);		
		 			    outColdItems.newLine();
				 		
				 		totalColdItems++;
				 		
		 	       }          
         }
    	
    	//close the pointer
    	outColdItems.close();
	  }
      
      catch(Exception E){
    	System.out.println("End of finding cod start users");
    	E.printStackTrace();
      }
   
    	
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
	    int dataSetChoice 		   = 0;			//0=sml, 1=ml, 2=ft
	   	int	trainingOrAllDivision  = 0;			// 0 =all, 1=training
	   	
	    //This movie should be included by that much no. of users for consideration, and same for users
	    int moviesSizeToBeConsidered = 0; //(e.g. 2 means>=1)
	    int usersSizeToBeConsidered  = 0;    
	   
	    //declare var
	    String coldItemsBuffer = "", t="", tr="", tAndTr="", p="", pm="", m="";
	    String actualColdItems="", trainSetName ="", testSetName="", trainAndTestSetName="";
	    
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
		    tr 		= p+ "sml_TrainSet.dat";
		    tAndTr 	= p+ "sml_MainSet.dat";
		    coldItemsBuffer = p+ "sml_coldSet.dat";
		    
		    pm  = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/FiveFoldData/111/";	    
	 	  
		    m = pm + "sml_storedFeaturesRatingsTF.dat";
	    	
	    	
/*	    	//To filter the movies
	    	trainSetName = p + "sml_clusteringTrainSetStoredTF" +(moviesSizeToBeConsidered-1)+".dat";
	    	testSetName = p + "sml_clusteringTestSetStoredTF" +(moviesSizeToBeConsidered-1)+".dat";		  	
		  	trainAndTestSetName = p + "sml_modifiedStoredFeaturesRatingsTF" +(moviesSizeToBeConsidered-1)+".dat";
			
*/
			//do not need to filter movies
		  	trainSetName = p + "sml_clusteringTrainSetStoredTF100_5.dat";
	    	testSetName = p + "sml_clusteringTestSetStoredTF100_5.dat";		  	
		  	trainAndTestSetName = p + "sml_modifiedStoredFeaturesRatingsTF100_5.dat";
		  	actualColdItems = p + "sml_StoredColdItems50.dat";
		  	
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
		    tr = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\SVD\\ml_TrainSet.dat";
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
		  	trainSetName = p + "ml_clusteringTrainSetStoredTF.dat";
	    	testSetName = p + "ml_clusteringTestSetStoredTF.dat";		  	
		  	trainAndTestSetName = p + "ml_modifiedStoredFeaturesRatingsTF.dat";
		    }
		    
		    else
		    {
		    	m = p + "ml_clusteringTrainSetStoredTF.dat";
		    	
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
	  	   ColdStartItems dis= new ColdStartItems(m,					//main memHelper file
	  			   							coldItemsBuffer,			//cold users Buffer
	  			   							actualColdItems,			//Actual Cold users
	  			   							moviesSizeToBeConsidered,	//Min Movies size
	  	   									usersSizeToBeConsidered,	//Min User Size
	  	   									dataSetChoice);				//0=sml, 1=ml,2=ft
	  	   	
	  	   //read the data and divide
	  	   dis.readData(0.8, false);	   
	  	   MemReader myReader = new MemReader();	 
	  	 
	  	 
	  	   //write into memReader objs
	 	   myReader.writeIntoDisk(coldItemsBuffer,  actualColdItems, false);	  	  

		  // Analyze file for statistics

	 	   MemHelper cold = new MemHelper(actualColdItems);
		   
	 	   System.out.println("Movs ="+ cold.getListOfMovies().size());
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
