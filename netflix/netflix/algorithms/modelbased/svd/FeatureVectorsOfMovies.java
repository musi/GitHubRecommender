package netflix.algorithms.modelbased.svd;

import java.util.*;
import java.io.*;

import netflix.algorithms.memorybased.memreader.FilterAndWeight;
import netflix.memreader.*;
import netflix.recommender.ItemItemRecommender;
import netflix.recommender.NaiveBayesRec;
import netflix.utilities.*;
import netflix.weka.TextNBRecWriter;
import netflix.weka.TextPartialWriter;
import netflix.weka.Writers.SVMWriter;
import cern.colt.list.LongArrayList;
import cern.colt.list.ObjectArrayList;
import cern.colt.matrix.*;
import cern.colt.matrix.linalg.*;
import cern.colt.matrix.impl.*;

/**
 * This class uses the colt library's 
 * SingularValueDecomposition class to create
 * and serialize an SVD object for the movielens
 * data. The program takes 4 arguments:
 *     1) The number of movies.
 *     2) The number of users.
 *     3) The file containing the MemReader.
 *     4) the file to write the SVD to. 
 * The program requires that the number of 
 * movies and number of users be explicitely input
 * to allow for both 0 and 1 indexed datasets. 
 *
 * @author sowellb
 */

class FeatureVectorsOfMovies 
{

	BufferedWriter      writeData;
	String    			myPath="";
	boolean 			FOLDING;
	boolean 			FTFlag;
	int					modelType;	     				//1=feature, 2=demo, 3=rat
	int 				myClasses;
	String				normalization;					// user, mov, or simple
	int 				minUserAndMov;
	
	String    			datFile="";
    String    			testFile="";
    String    			destFile="";
    String				mainFile ="";
    String  			modelName ="";					//model name to be written
    String              trainingOrFullSet = "";			//full fold data is to be svded or training set of a fold is to be svded
    String				FTInfo;
    int					xFactor;
    double 				DF_THRESHOLD;
    
    MemHelper 					helper;					//train object
    MemHelper 					mainHelper;				//object (test and train)
    Random              		myRand;
    FilterAndWeight     		myFilter; 				//user-based CF
    ItemItemRecommender			myItemRec;		    	//item-based CF
    TextNBRecWriter 			myNB;					//Naive Bayes Rec
    Algebra						myAlg; 
    
    FileWriter                  myWriter;
    
    //For making the dictionay of the features
    HashMap <String,Double> myDictionary;
    HashMap <String,Double> myDictionary_Max;			//Max value of a feature in all movies
    HashMap <String,Double> myDictionary_Min;			//Min value of a feature in all movies
    HashMap <String,Double> myDictionary_maxMinusMin;   //denomenator of normalisation
    
    //--------------------------------------------
    
	public FeatureVectorsOfMovies(int myClasses, boolean folding, boolean FTFlag)
	{
		myPath   = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/FeaturesPlay/FeatureModels/";
        
      // datFile  = myPath + "sml_clusteringTrainSetStoredTF.dat";
	 //  datFile  = myPath + "sml_storedFeaturesRatingsTF.dat";
      // destfile = myPath + "SVDStoredColt.dat";
      // destFile = myPath + "SVDStoredColtI.dat";	
         
        myRand  				= new Random();
        this.myClasses			= myClasses;			//classes=5, SML, ML; 10=FT
        FOLDING 				= folding;				//folding flag
        this.FTFlag				= FTFlag;				//Ft flag
  
  
        //Dictionary
         myDictionary 				= new HashMap <String, Double>();
         myDictionary_Max 			= new HashMap <String, Double>();
         myDictionary_Min 			= new HashMap <String, Double>();
         myDictionary_maxMinusMin 	= new HashMap <String, Double>();
	}
	
/****************************************************************************************************/
	/**
	 * Main Method
	 */
	
	public static void main(String args[]) 
    {
	
		
		FeatureVectorsOfMovies mySVD = new FeatureVectorsOfMovies(  10,						//Classes 
																	false, 			 		//Folding	
																	false					//FT
												  				 );
		mySVD.prepareModelParameters();
    }

/****************************************************************************************************/
	
	/**
	 * Prepare Modeol parameters
	 */
	
	public void prepareModelParameters ()
	{
		
		
		int myEnd =0;
		
		if(FTFlag==false)
			myEnd = 1;
		else
			myEnd = 1;
		
	for(int m = 1 ;m<=myEnd;  m++)
	{
		if(m==1)
			minUserAndMov = 1;
		else if(m==2)
			minUserAndMov = 2;
		else if(m==3)
			minUserAndMov = 5;
		else if(m==4)
			minUserAndMov = 10;
		

		
	
			// XFactor (Training size)
			for(int j=80;j<=80;j+=20)
			{			 
				/*if(j==80)
					j =90;
				*/
				
				xFactor = j;
				modelType = 3;			//1,2,3-->feature,demo,rat
				normalization = "Simple";
				//normalization = "UserNor";
				
				buildModels(2);	
					
			
			} //end of midlle for	
			
	}//end of outer for (for minUser and mov)
  }
	
/****************************************************************************************************/
/**
 *  Build models, may be 1-fold or 5-fold
 */
	
	public void buildModels(int whichModel)
	{
		Timer227 timer = new Timer227();
		  
		int 	   numMovies 			= 0;
        int 	   numUsers  			= 0;
        int 	   numFeatures 			= 0;                
        int 	   rows      			= 0;
        int 	   cols 	   			= 0;
        int        epoch     			= 0;
        String     myPath	   			= "";
        double[][] data					= null;
        
        
        //determine folding and simple 20-80 settings
        if(FOLDING == false){		 
         			
        	if(FTFlag==false)	{			// Path		
        		//myPath = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/" + xFactor + "/";
        	 	  myPath  = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/FeaturesPlay/FeatureModels/";
        	}
        	
        	else
        		myPath = "C:/Users/Musi/workspace/MusiRecommender/DataSets/FT/SVD/"+ xFactor + "/";
        		
        }
        
     
 
  //Build Model     
  try { 	
      
    
           if(FOLDING == false) // No folding
           {                   	   
	        	//SML   
	        	if(FTFlag==false)
	        	{	        	   
	        	
	        	myClasses = 5;
	        		
	        	//Usual
	        	/*datFile     = myPath + "sml_clusteringTrainSetStoredTF.dat";
	            testFile    = myPath + "sml_clusteringTestSetStoredTF.dat";*/
	            
	          	 //Feature Play		 	      
	          /*  datFile     = myPath + "sml_clusteringTrainSetStoredTF.dat";
	            testFile    = myPath + "sml_clusteringTestSetStoredTF.dat";
	            mainFile    = myPath + "sml_storedFeaturesRatingsTF.dat";
	            */
	            
	            //TF-IDF
	            /*datFile     = myPath + "sml_clusteringTrainSetStoredTF.dat";
	            testFile    = myPath + "sml_clusteringTestSetStoredTF.dat";
	            mainFile    = myPath + "sml_storedFeaturesRatingsTFIDF.dat";
	            */
	            
	            //TF-Only
	            datFile     = myPath + "sml_clusteringTrainSetStoredTF.dat";
	            testFile    = myPath + "sml_clusteringTestSetStoredTF.dat";
	            mainFile    = myPath + "sml_storedFeaturesRatingsTFOnly.dat";
	            
	            numMovies 	= 1682;
		        numUsers 	= 943;
		        rows        = numMovies;
		        cols        = numUsers;
		        FTFlag		= false;	 
	        	
	        	}
	        	 
	        	 //FT 
	        	 else
	        	 {		             
	        		 if(trainingOrFullSet.equalsIgnoreCase("full")){
 		  	        	  
	        			  FTInfo = "Both";	 	        		
	 	        		 // FTInfo = "OnlyUsers";
	 	        		  //  FTInfo = "NoNor";
	 	        		
	        			// FTInfo = "";
	        			 
	 		        	datFile     = myPath + "ft_clusteringTrainSetStored"+ FTInfo+ "TF"+ minUserAndMov + ".dat";
	 		            testFile    = myPath + "ft_clusteringTestSetStored"+ FTInfo+ "TF"+ minUserAndMov + ".dat";
	 		            mainFile    = myPath + "ft_modifiedStoredFeaturesRatings"+ FTInfo+ "TF"+ minUserAndMov + ".dat";
	 	        	}
	 	        	
	 	        	else{
	 	        		
	 	        		   FTInfo = "Both";	 	        		
	 	        		 //  FTInfo = "OnlyUsers";
	 	        		 //  FTInfo = "NoNor";
	 	        		
	 	        		datFile   = myPath + "ft_clusteringTrainingTrainSetStored"+ FTInfo+ "TF"+ minUserAndMov + ".dat";
	 		            testFile  = myPath + "ft_clusteringTrainingValSetStored"+ FTInfo+ "TF"+ minUserAndMov + ".dat";
	 		            mainFile  = myPath + "ft_modifiedStoredTrainingFeaturesRatings"+ FTInfo+ "TF"+ minUserAndMov + ".dat";
	 	            
	 	        	}
	 	        	
	             	//make some objects
	 	        	MemHelper mainHelper = new MemHelper(mainFile);		
	 	        	myFilter = new FilterAndWeight(helper,1); 		 //FilterAndWeight, Random object, For CF	                 	   
	 	            myItemRec = new ItemItemRecommender(true, 5);     //Item based CF     
	 	        
	 	             
	 	           numMovies 	= mainHelper.getNumberOfMovies();			
	 	           numUsers 	= mainHelper.getNumberOfUsers(); 	
	 	          
	 	            
	 	           System.out.println("Num Mov="+ numMovies);
		           System.out.println("Num Users="+ numUsers);
		            
		          /* numMovies 	= 1922;			
	 	           numUsers 	= 1214;	 */	           
	 	                   
	                FTFlag		= true;	           	
	    
	           }       	
           } //end if
           
                     
                     	      
            //Create memhelper objects
            helper = new MemHelper(datFile);		       
            mainHelper = new MemHelper(mainFile);
         
      
    	    
            //Check the dictionary words
            getAllKeywordAndMakeMatrix();                        
            numFeatures = myDictionary.size();
            
            //Make data Matrix
            data  = new double[numFeatures][numMovies]; //For SVD m>=n       	
            double TF 			= 0;
            double norWeight    = 0;								   //After normalisation, we assign this weight
            int    featureIndex = 0;								   //keep track of which feature
            int    i 			= 0;								   //feature index
            String singleFeature= "";
                     
            
          //start building the dictionary
  		  Set mySet = myDictionary.entrySet();			  // Find set and iterators  
  		  Iterator myIterator = mySet.iterator();
  		  HashMap<String, Double> featuresMovie;
  		  
       	//Here get a feature for a movie, then (1) normalize it (2) if it is not there, put
      	//zero there (3) while normalizing check denomenator shld not be zero

  		  
  		  //Iterate over the words of Test set until one of them finishes
  	     	while(myIterator.hasNext()) 
  	     	 {
  	     		 i = featureIndex++;  	 
  	     		 
  	     		 if(featureIndex>1000 && featureIndex%1000 ==0)
  	     			 System.out.println("Has processed "+ featureIndex+" features");
  	     		 
  	     		 Map.Entry words  = (Map.Entry)myIterator.next();        // Next 		 
  	     		 singleFeature	  = (String)words.getKey();				 // Get word
	            	
  	     		 //Get min and max of this feature
  	     		 double min  = myDictionary_Min.get(singleFeature);
  	     		 double max  = myDictionary_Max.get(singleFeature);
  	     		 double diff = myDictionary_maxMinusMin.get(singleFeature);
  	     	     
	                for(int j = 0; j < numMovies; j++) 
	                {	    
	                	//reset weights
	                	norWeight = 0;
	                	TF 		  = 0;
	                	
	                	featuresMovie = mainHelper.getFeaturesAgainstAMovie(j+1);
	                	
	                	//first check if this mov has some features
	                	if(featuresMovie!=null && featuresMovie.size()>1)
	                	{
	                		//Now check if it has feature
	                		if(featuresMovie.containsKey(singleFeature))
	                		{
	                			//get and normalise the weight
	                			TF = featuresMovie.get(singleFeature);
	                			norWeight = TF;
	                			
	                			//check if denomenator is zero or not
	                			if(diff!=0)
	                				norWeight = (TF - min) / diff;
	                			else
	                				{
	                					if(max!=0)
	                						norWeight = (TF - min) / max;
	                					else
	                						norWeight = (TF - min) / 1;
	                				} 
	                			
	                			
	                			
	                			
	                		}
	                	} //end if
	                	
	                	//if movie has no features, then put zero
	                	else
	                	{
	                		norWeight = 0;
	                	}      	
	                	
	             
	                		                	
	                	data[i][j] = norWeight;
	                	
	                	//write this data into a file, where each movie is in the row and each row 
	                	// has the movie features separated by comma.
	               
	                } //end of movie for
	            } //end of feature loop
     
       
               
       writeMovieFeatures(data);
       
   } //end of try
   
        catch(Exception e) {
                    e.printStackTrace();
        }



    }

/**************************************************************************************************/
	
	/**
	 * We will get features from all the movies, and build a dictionary, then we will
	 * use this this as a check-mark, if a movie do not has a keyword (term), put zero in that
	 * term....similarly build this model and generate model.
	 */
	
  public void getAllKeywordAndMakeMatrix()
  {
	  System.out.println("came for building dictionary");
	  
	  int numMovies = mainHelper.getNumberOfMovies();
	  HashMap<String, Double> FeaturesMovie;
	  
	  for(int i=1;i<=numMovies;i++)			//for all movies
	  {
		  FeaturesMovie = mainHelper.getFeaturesAgainstAMovie(i);
		  
		  //Do feature selection
		   HashMap <String, Double> FeaturesMovieDummy;
	       FeaturesMovieDummy = (HashMap <String, Double>)FeaturesMovie.clone();
	      	
	       //DO Feature Selection
	       FeaturesMovie = doFeatureSelectionByDFThresholding (FeaturesMovieDummy, 5);
		  
		  //start building the dictionary
		  Set mySet = FeaturesMovie.entrySet();							  // Find set and iterators  
		  Iterator myIterator = mySet.iterator();    	   
		 
		//  myFeatures +="\"";											  // opening string "
		  int total =0;
		  
		  //Iterate over the words 
	     	while(myIterator.hasNext()) 
	     	 {
	     	     total++; 
	     		 Map.Entry words = (Map.Entry)myIterator.next();        // Next 		 
	     	     String word 	 = (String)words.getKey();				 // Get word	    
	     	     double TF 		 = FeaturesMovie.get(word);          	 // Get TF   	     	
	    
	     	     if(myDictionary.containsKey(word)==false && TF>1)	
	     	    	myDictionary.put(word, 1.0);			     		//We do not care abt the tf 		
	     	     
	     	 }//end of while
	     	
	    } //end for
	 
	  //--------------------------------------------------
	  int featureSize = myDictionary.size();
	  double max = 0;
	  double min = 0;
	  double TF  = 0;	
	  String singleFeature ="";
	  
	  System.out.println("Num distinct words found ="+ featureSize); 

	  //Iterate over the words 
	  Set mySet = myDictionary.entrySet();			  					// Find set and iterators  
	  Iterator myIterator = mySet.iterator();    	   

	    while(myIterator.hasNext())// && total <10) 
     	 {     	
     		 Map.Entry words = (Map.Entry)myIterator.next(); 	      // Next 		 
     		 singleFeature 	 = (String)words.getKey();				  // Get word   
     	     
     	     
     	     //reset min and max for each feature	  
			  max = 0;
			  min = 0;			 
			  
			//Find Max and Min for each feature over all the movies
			  for(int i=1;i<=numMovies;i++)		
			  {
				  FeaturesMovie = mainHelper.getFeaturesAgainstAMovie(i);
				  
				  if(FeaturesMovie.containsKey(singleFeature))
				  {
					  TF = FeaturesMovie.get(singleFeature);					  // Get TF of the word
					  
					  //update min and max
					  if(min > TF)
						  min = TF;					  
					  if(max<TF)
						  max = TF;
					  
				  }				  		  	  
			  }//end inner for of movies
			  
			  //store max and min of each feature into a map
			  myDictionary_Max.put(singleFeature, max);
			  myDictionary_Min.put(singleFeature, min);
			  myDictionary_maxMinusMin.put(singleFeature,(max-min));
			  
	    } //end while
	  
	    
	  
  }
 
  /*******************************************************************************************************/   
  
  /**
	 * Do DF Thresholding
	 * @param hashMap, consisiting of features against a movie
	 * @param Classes, co. of classes 
	 * @param uid,     user id for which a classifier is to be trainied
	 * @param type,    type of the feature 
	 * @return HashMap,	Seleceted features
	 */
	   public HashMap<String, Double> doFeatureSelectionByDFThresholding( 
			   										HashMap<String, Double> movieFeatures,			   
			   										int classes									// no of classes							
			   										)				
	   {
		   // define list of words to be removed
		   ObjectArrayList removeTheseWords = new ObjectArrayList();
		   
		   	//proceed if movie size is not zero
		   	if(movieFeatures.size() !=0)
		   	{
	          		  //Get entry sets for train class vector  	      	       	  
	             	  Set setTrainClass = movieFeatures.entrySet();               	  
	             	  Iterator jTrainClass = setTrainClass.iterator();
	               	               	              	  
	             	  //Iterate over the words of Test set until all are processed
	 	              	while(jTrainClass.hasNext()) 
	 	              	 {
	 	              	     Map.Entry words = (Map.Entry)jTrainClass.next();         // Next 		 
	 	              	     String word 	 = (String)words.getKey();			      // Get a word from the train class (word was as a key)
	 	              	      	              	     
	 	              	     // Check in how many movies this word occurs
	 	              	       boolean word_OK = checkDFThresholding(word);
	 	              	       
	 	              	       if(word_OK == false) 
	 	              	    	   removeTheseWords.add(word); 	              	    	   
	 	              	     	
	 	              	  } //end of while	              	
	            }//end of if	   

		   
				   //------------------------
				   //Remove the words
				   //------------------------
				   
				   int size = removeTheseWords.size();
				   
				   for (int i=0;i<size; i++)
				   {
					   //get a word to be removed
					   String oneWordToBeRemoved = (String)removeTheseWords.get(i);
					   
					   //remove the word
					   if(classes ==5)
						   movieFeatures.remove(oneWordToBeRemoved);				   
					   else
						   movieFeatures.remove(oneWordToBeRemoved);
				   } //end of for  
	   
		   //Return the HashMap consisting of Selected Features
		   return movieFeatures;
		     
	   }
	           
	//----------------------------------------------------------------------------------------------

	 /**
	  * DF Threshold cheking for each word
	  */
	   
	   public boolean checkDFThresholding(String word)
	   {		  
		   // Get all movies seen by this user		   
	      int moviesSize 	= 1682;
	      int mid 			= 0; 
	      double DF			= 0.01;
	      
	      // Define DF threshold per user's rating
	       DF_THRESHOLD = (int)(moviesSize * (DF));  // at 3 = there was no match
	      
	      //how many times this word occures across the doc
	      int count =0;
		  		   
		  //For all movies
		  for (int i=1;i<=moviesSize;i++)		   {
			     		       		       	   
		       	 //Get a training feature for this movie
		       	 HashMap<String, Double>FeaturesAgainstAMovie  = mainHelper.getFeaturesAgainstAMovie(i);
		         
		       	 //check for matchi
		       	 if (FeaturesAgainstAMovie !=null) 
		       	 {	          
		       		 if(FeaturesAgainstAMovie.containsKey(word))
		       			 count++;   			 
		       		 
		       	 } //end of if	    	
		     } //end of for
		 	 
		  		// If this word occures across DF_THRESHOLD no. of docs (movies), send true	
		  		if(count>=DF_THRESHOLD)// && count <moviesSize-2 )
		  			return true;
		 
		   //else we return false
		  		return false;
		   
	   }
	   
/******************************************************************************************************/
	   
	   
  
  //---------------------------------------------------------------------------
  
  public void writeMovieFeatures(double[][] data)
  {
	  openFile();
	  int cols = myDictionary.size();	  
	  System.out.println("Came to write features with dictionay size=" +cols);	  
	  int rows = 1681;
	  int i =0,j =0;			//for movies, features in SML

	  try
	  {
		  
	    for(j=0;i<=rows;i++)
	    {
	    	myWriter.append(""+ (i+1));
			 
			 if(i>=200 && i%200==0)
				 System.out.println("mov writing is at mov="+ i);
			 
		   for(j=0;j<cols;j++)
		   {	  
			
				 double f = data[j][i];
				
				 myWriter.append(",");
				 myWriter.append(""+ f);
			
				 
						  		
				}
		   
		   myWriter.append("\n");
		   
			}//end outer for
	  }  
		  catch (Exception E){
			  System.out.println("error in writing i="+i+", j="+j);
			  E.printStackTrace();
			  System.exit(1);
		  }

		  closeFile();
  }
  
  //----------------------------------------------------------------------------
 

  
	 public void openFile()
	 {
		 String myPath = this.myPath;
		 
		 try{
			
			myWriter = new FileWriter(myPath +"sml_MovFeaturesSetTFOnly_Nor_0.01_1.csv", true); //true, append in existing file
		    System.out.println("File written in="+ myPath);
		 }
		 
		 catch( Exception E) {
			 		E.printStackTrace(); 			 
		 }
	 }

	//---------------------------------------------
	   
	 public void closeFile()
	 {
	  		 
		 try{		
			 myWriter.close();
		 }
		 
		 catch( Exception E) {
			 		E.printStackTrace(); 			 
		 }
	 }
	 
	
}