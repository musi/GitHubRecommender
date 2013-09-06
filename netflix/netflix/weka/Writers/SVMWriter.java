package netflix.weka.Writers;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import cern.colt.list.LongArrayList;
import cern.colt.list.ObjectArrayList;
import cern.colt.map.OpenIntDoubleHashMap;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.evaluation.EvaluationUtils;
import weka.classifiers.evaluation.NominalPrediction;
import weka.classifiers.evaluation.Prediction;
import weka.classifiers.functions.LibSVM;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Standardize;
import weka.filters.unsupervised.attribute.StringToWordVector;
import weka.filters.unsupervised.instance.NonSparseToSparse;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.bayes.NaiveBayesSimple;						//does not check underflow or so
import weka.classifiers.bayes.NaiveBayesMultinomial;
import netflix.algorithms.memorybased.memreader.FilterAndWeight;
import netflix.memreader.*;
import netflix.rmse.RMSECalculator;
import netflix.utilities.Timer227;


public class SVMWriter 
{

	 // Recommender related object and varaibes
	MemHelper 				MMh;						// train set
    MemHelper 				MTestMh;					// test set	
    MemHelper 				MainMh;						// All Data, will be used to get movie features
   
    //Batch Filter
    Standardize				 batchFilter;
    
    //StringToWord Filter
    StringToWordVector		 stwv;	
    
    //Classifier
    LibSVM					 myClassifier;
    
    //FilterAndWeight
    FilterAndWeight			 myFilter;
    
    //Start up RMSE count
    RMSECalculator			 rmse;
    
    //Timer 
    Timer227 				 myTimer;
    
    //some other vaaible
    double 					 DF_THRESHOLD;
    int     				 totalMovWithNullFeaturesTest;
    int     				 totalMovWithNullFeaturesTrain;
    int     				 totalMovTrain;
    HashMap <String, Double> FeaturesMovie;
    BufferedWriter 			 outT;  
    String         			 sparseMMh;
    String        			 predictedDataFile;
    
/*******************************************************************************************************/
 //Constructor
    
	public SVMWriter(String mainObject, 
						 String trainObject, 
						 String testObject,
						 String dataToWrite,			// dummy File to write data
						 String missingValFileName) 	// MemReader Obj will be written and will be send back
														throws Exception 
	{
	  	//Get test and train objects
		MainMh	= new MemHelper (mainObject);
		MMh		= new MemHelper (trainObject);
	    MTestMh = new MemHelper (testObject);

	    
	    //FilterAndWeight for CF
	    myFilter = new FilterAndWeight(MMh,1); 			 // FilterAndWeight, Random object, For CF
	    
	    //----------------------------------------------------
	    // Related to classifiers	    	 

        //LIbSVM
   	    myClassifier = new weka.classifiers.functions.LibSVM();      	
   	    
	    //filters
	    batchFilter = new Standardize();
	    
	    //String To word Filter
	    stwv = new StringToWordVector();	    
		String[] myTrainOptions = new String[6];		
		
		//myTrainOptions[1] = "-C";	    //log tf
		myTrainOptions[0] = "-I";		//fij*log(num of Documents/num of documents containing word i)	
		myTrainOptions[1] = "-W";
		myTrainOptions[2] = "5000";		
		myTrainOptions[3] = "-S";		//lower case
		myTrainOptions[4] = "-M";		// min term frequency, I think two
		myTrainOptions[5] = "1";		// with two it changes?		//
		/*myTrainOptions[6] = "-N";
		myTrainOptions[7] = "1";*/
		//myTrainOptions[6] = "-T";		//output word count
		
		
		 try{
			 stwv.setOptions(myTrainOptions);
			 //stwv.setIDFTransform(true);
			 //stwv.setMinTermFreq(1);
			 //stwv.setOutputWordCounts(true);
		 }
	    	 catch(Exception E) {
	    		E.printStackTrace();
	    		System.out.println("error");
	    	}

	    //----------------------------------------------------
		// Related to other vars
		           
		//For MAE
		 rmse = new RMSECalculator();
		 
		 //Timer
		 myTimer = new Timer227();
		 
		//var
		outT = new BufferedWriter(new FileWriter(dataToWrite));		// we will write the sparse data predictions here
		sparseMMh = missingValFileName;								// will write memReader obj with this name
		predictedDataFile = dataToWrite; 							// to write memReader obj, we need the file where predictions are stored
		totalMovWithNullFeaturesTest = 0;
		totalMovWithNullFeaturesTrain =0;
		totalMovTrain =0;
		FeaturesMovie = null;
	}
	
		
/*******************************************************************************************************/
	/**
	 * This function performs NB classification steps
	 */
	
	public void GeneratePrediction() throws Exception
	{
	
		{
		// For each user (in test set), make recommendations
        double prediction =0;
		LongArrayList movies ;
        int moviesSize = 0;
        double rating = 0.0;
        int mid =0, uid =0;
        int targetMovie = 0;
        String FeaturesTrainMovie_BagsOfWords ="";   
        
        int allUsers = MainMh.getNumberOfUsers();
        
       // allUsers = 100;
        
        for(int i=1;i<=allUsers;i++)
        {
        
        System.out.println("user is:"+ (i));
        uid =i;
        	
        //Create attributes
        FastVector myAttributes = getAttributes("Train"); 

        
        //Get priors weights of the active user
        OpenIntDoubleHashMap priors 		=  getActiveUserPriors(uid, 5);
        OpenIntDoubleHashMap norPriors 		=  getActiveUserNormalizedPriors(uid, 5);
        OpenIntDoubleHashMap weightedPriors =  getWeightedPriors(1, uid, 5);
        
	        //LIbSVM
	   	    myClassifier = new weka.classifiers.functions.LibSVM();   
 	       	
 		   //Get priors (normalized)		
 	   	    String[] options = new String [6];
       	 	options[0] = "-S";
 	       	options[1] = "0";
 	       	options[2] = "-K";
 	       	options[3] = "0";
 	       	options[4] = "-C";
 	       	options[5] = ""+2;
 	       	
 	     	
       	    String weight = 	   priors.get(0) + " "
 	       	    				+ (priors.get(1)) + " "       	    
 	       	    				+ (priors.get(2)) + " "
 	       	    				+ (priors.get(3)) + " "
 	       	    				+ (priors.get(4));
        	    
	       	    myClassifier.setOptions(options);       	
	 	      	myClassifier.setCost(2);	      	
	 	       	myClassifier.setWeights(weight);
	 	       	myClassifier.setNormalize(true);  //this is diff from others	    
	   
	 	       	
        //Get 
        movies = MMh.getMoviesSeenByUser(uid); 							//get movies seen by this user
        moviesSize = movies.size();                     
        Instances myDataSet = null;     
        IntArrayList nonZeroFeatures = new IntArrayList();				//one feature is found
                 
             myTimer.start();
             
            //For all movies seen by this user
            for (int j = 0; j < moviesSize; j++)
             {
            	totalMovTrain++;
            	FeaturesMovie =null;
            	
            	mid = MemHelper.parseUserOrMovie(movies.getQuick(j));		 // get mid
            	FeaturesMovie = MainMh.getFeaturesAgainstAMovie(mid);			// to check size of features
         
            	              	
            	if(FeaturesMovie!=null && FeaturesMovie.size() >0)    	  
            	     nonZeroFeatures.add(mid);
              
             } //finished getting train movies size with non-null features
           
            myTimer.stop();
            System.out.println("Time taken to check size of movies by user="+ i + " is ="+ myTimer.getTime());
            myTimer.resetTimer();           
        
            
            myTimer.start();
            
            //Get the train movies feature size
            for (int j = 0; j <nonZeroFeatures.size() ; j++)
            {            
            	mid = nonZeroFeatures.getQuick(j);
            	rating = MMh.getRating(uid, mid);            	   				// get rating           	
            	
            	FeaturesMovie = MainMh.getFeaturesAgainstAMovie(mid);
            	
            	//HashMap <String, Double> FeaturesMovieDummy;
            	//FeaturesMovieDummy = (HashMap <String, Double>)FeaturesMovie.clone();
            	
            	//DO Feature Selection
            	// FeaturesMovie = doFeatureSelectionByDFThresholding (FeaturesMovieDummy, 5, uid);
            	
            	//Make Bags of Words
            	FeaturesTrainMovie_BagsOfWords = getBagsOfWords(FeaturesMovie);		// get bags of words
            	
                //Create learning dataset (only once -- one for each user)          
                if(j==0)  myDataSet = getLearningDataSet("TrainNB",
                										 myAttributes,		 	 // created attributes 
                										 nonZeroFeatures.size()); //  no of examples
                 
	                
	                	//---------------------------
		                // Build data set
		                //---------------------------
		             
	                	//Get String equivalent of the rating
	                	String myRating = getStringEquivalent(rating);           
		        
	                	//System.out.println("myRating ="+ myRating);
	                	
		                // create data set (add instance)												 
		                myDataSet = createDataSet( 	myDataSet,							// Instances
		                							mid,                				// mid										
		                							FeaturesTrainMovie_BagsOfWords, 	// features against a movie
		                							myRating);							// rating		                
	               
             } //end of inner for

            myTimer.stop();
            //System.out.println(myDataSet);
            System.out.println("Time taken to make traing set for user="+ i + " is ="+ myTimer.getTime());
            myTimer.resetTimer();
            
            //  System.out.println("train size = " + myDataSet.numInstances());
            
	        //-------------------------------------
	        // Go through all the movies
	        //-------------------------------------       
		    
       int allMovies = MainMh.getNumberOfMovies();        
       IntArrayList myMoviesToPredict = new IntArrayList();            
            
        //why not send all movies at once, and write in the test set?    
        for(int j=1;j<allMovies;j++)
        {
         	//If rating (obtained from training set is -99, user has not seen this movie)	
          	 double myRating = MMh.getRating(uid, j);            
        
          	 if(myRating==-99)
          	 {
            		targetMovie = j;
            		myMoviesToPredict.add(j);
            		
          	 }
        }
	    
        //If this is zero, we may come up with rating ==-99 in the written object
        if(nonZeroFeatures.size()>0)
		 {
	            // myTimer.start();
	            // Apply string To Word Vector Filter
	             Instances new_DataSet = applyStringToWordFilter(myDataSet);
	            //System.out.println("train size = " + new_DataSet.numInstances());
	                   
	                
	                // Learn the model
	                LibSVM predictiveModel = myClassifier;
	                predictiveModel = learnPredictiveModel(new_DataSet);
	                
	                // Evaluate the  model, Make Prediction
	            	 evaluatePredictiveModel(uid, 						// uid
	            							 myMoviesToPredict,		    // mid's to predict
	            							 predictiveModel,			// learned model
	            							 new_DataSet);				// myTrain Instances
	            /*  myTimer.stop();
	                System.out.println("Time taken to make a prediction by user="+ i + " on movie="+ j+ " is ="+ myTimer.getTime());
	                myTimer.resetTimer();*/
	                        
	      		  } //end of if
	            
	      
       } // end of user for
        
        try {
        	outT.close();
        }        
    	catch (Exception E) {
    		E.printStackTrace();
    		System.out.println("Error writing prediciton into file");
    	}            	
	}
		
        //-----------------------------------------
        // write a memReader obj and send back
        //-----------------------------------------
    	MemReader myReader = new MemReader();	 
    	myReader.writeIntoDisk(predictedDataFile, sparseMMh, false); 	
    	
      }        
        

/*******************************************************************************************************/	
	/**
	 * build attributes and return them
	 */
 
	//In new version, ArrayList is used instead of fastvector
	public FastVector getAttributes(String testOrTrain)
	{
		
		FastVector allAttributes = new FastVector(2);	//we have total 3 attributes (uid, mid, string words, rating)
														// we discard uid, as it will be same for each user
		//mid attribute
		Attribute movieID = new Attribute("mid" );
		
		//rating Attribute		
		FastVector ratingAtt = new FastVector(5); 
		ratingAtt.addElement("1.0");
		ratingAtt.addElement("2.0");						//rating ={1,2,3,4,5}
		ratingAtt.addElement("3.0");
		ratingAtt.addElement("4.0");
		ratingAtt.addElement("5.0");
		
		Attribute movieRating = new Attribute(testOrTrain+"rating", ratingAtt);
		
		//Bags_Of_Words attribute
		Attribute movieDescription = new Attribute(testOrTrain+"movieDescription" , (FastVector) null);
					
		//Add Elements to the allAttribute							
		allAttributes.addElement(movieDescription);			// Must add in order
		//allAttributes.addElement(movieID);
		allAttributes.addElement(movieRating);				
		
		return allAttributes;
	}
	
	

/*******************************************************************************************************/
 /**
  *  Create Instances and set class
  */	
	
   public Instances getLearningDataSet (String name, 
											 FastVector allAttributes, 		// created attributes
											 int howManyExamples)			// no of examples										
	{		
		//create instances
		Instances myDataSet = new Instances (name, allAttributes, howManyExamples);
		myDataSet.setClassIndex(1);
		return myDataSet;	
	}

	
/*******************************************************************************************************/
	
	/**
	 * Add instance (examples)
	 */
	
	public Instances createDataSet (  Instances myInstances,			// created Instances									
									   int mid,							// mid
									   String bagsOfWords,				// bags of words against a movie
									   String rating)				 	// rating

	{	
		//Instances myNewInsatnces = null;
		
		//SparseInstance instance = new SparseInstance (2);
		Instance instance = new DenseInstance (2);
		instance.setDataset(myInstances);		
		instance.setValue(0, bagsOfWords);				 // only two attributes are enough --> (bags of words, rating)
		instance.setValue(1, rating);
		//instance.setValue(1, mid);
		
		myInstances.add(instance);						// add created instance in Instances
 
		return myInstances;
	}
	

	
	
/*******************************************************************************************************/

	 /**
	   * Get string equivalent of rating --> 1.0 = "1.0"
	   */
	
	  public String	getStringEquivalent (double rating)
	  {

		  String myRating ="";
		  myRating = "" + rating + "";
		  
		  return myRating;
	  }
	 
	 
/*******************************************************************************************************/
	  
	 /**
	  * Create bags of words against a movie
	  */
		
	 public String getBagsOfWords (HashMap <String, Double> FeaturesTrainMovie)
	  {
		 String myFeatures ="";
		// HashMap <String, Double> FeaturesTrainMovie = new HashMap <String, Double>();
		// FeaturesTrainMovie = dummyFeatures;
		  
		 // Find set and iterators
		  Set setTrainClass = FeaturesTrainMovie.entrySet();  
		  Iterator jTrainClass = setTrainClass.iterator();    	   
		 
		//  myFeatures +="\"";											  // opening string "
		  int total =0;
		  
		  //Iterate over the words of Test set until one of them finishes
	     	while(jTrainClass.hasNext())
	     	 {
	     	    total++; 
	     		Map.Entry words  = (Map.Entry)jTrainClass.next();          // Next 		 
	     	     String word 	 = (String)words.getKey();				  // Get word	    
	     	     double TF 		 = FeaturesTrainMovie.get(word);          // Get TF   	     	
	    
	     	     //build bags of words
	     	     for (int i=0; i<TF;i++)
	     	     {
	     	    	 myFeatures += word;								  // words separated by space	
	     	    	 myFeatures +=" ";
	     	     }
	     	     
	     	 }//end of while
	  
	     //	myFeatures +="\"";										  	   // closing string "			
	     	myFeatures.trim();
	     	//System.out.println(myFeatures);
	     	return myFeatures;
	 }
	 
/*******************************************************************************************************/
	 
	/**
	 * Apply StringToWordVector
	 */
	
	public Instances applyStringToWordFilter (Instances myData) throws Exception
	{
		 Instances newData = null; 
		
		//StringToWordVector for train set
		 stwv.setInputFormat(myData);
		 Instances new_trainData = Filter.useFilter(myData, stwv); 	//instances, stringToWordFilter		
	
		 
		 return new_trainData;
	}

	
/*******************************************************************************************************/
	/**
	 *  Init Batch Filter with Training set
	 */

	public void initBatchFilter (Instances trainData) throws Exception
	{
		batchFilter.setInputFormat(trainData); 	
	}
	
/*******************************************************************************************************/
	/**
	 * Build and learn the Naive Bayes classfier on training data set
	 */

	public LibSVM learnPredictiveModel (Instances myData) throws Exception
	{			     
		// build classifier
		   myClassifier.buildClassifier(myData);		   
		   return myClassifier;
	}
	
/*******************************************************************************************************/	

	/**
	 * Make prediction for the test set 
	 */
	
	 public void evaluatePredictiveModel (  int activeUser, 								// active User
			 								IntArrayList targetMovies,						// target Movie	
			 								LibSVM myLearnedModel,						// learned classiifer 
			 								Instances trainInstaces) throws Exception		// train dataset
	 {		
		 //create local variables		  
		 IntArrayList nonZeroFeatures 			= 	new IntArrayList();						// one feature is found
		 HashMap <String, Double> FeaturesMovie =	null;            
         Instances myDataSet 					= 	null;
         Instances new_DataSet 					= 	null;
         String FeaturesTestMovie_BagsOfWords 	=	"";
         double rating							=	0;
         double	prediction 						=	0;
         int mid								=   0;
         int moviesSize 						=	0;         
                   
         //get movies seen  by this user         
         moviesSize = targetMovies.size();   
         
         //For all movies seen by this user (Test Set)
         for (int j = 0; j < moviesSize; j++)
         {        	 
	       	 mid = targetMovies.getQuick(j);			    // get mid	       	 	
	   	     FeaturesMovie = MainMh.getFeaturesAgainstAMovie(mid);				// to check size of features
	   	 
	   	     if((FeaturesMovie != null && FeaturesMovie.size() >0))
		   	  {
		   	    	nonZeroFeatures.add(mid);
		   	  }
	   	     
	   	     else 
	   	    	totalMovWithNullFeaturesTest++;
	   	 
         }
         
      if( nonZeroFeatures.size() >0)
      {
         //System.out.println("test movies with features = " + nonZeroFeatures.size());
         for (int j = 0; j < nonZeroFeatures.size(); j++)        
         {  
        	//For each movie, new instance, and reset it as well
        	myDataSet = null;
        	
        	mid = nonZeroFeatures.getQuick(j);        	
            FeaturesMovie = MainMh.getFeaturesAgainstAMovie(mid);              
            rating = 5;															//Just dummy, write anyhting

            //Create attributes
            FastVector myAttributes = getAttributes("Test");
            
            FeaturesTestMovie_BagsOfWords = getBagsOfWords(FeaturesMovie);		// get bags of words
	       	
	        //Create learning dataset (only once -- one for each user)         
            myDataSet = getLearningDataSet(	"testNB",
	        			  					 myAttributes,			 		 // created attributes 
	        		  						 nonZeroFeatures.size()); 		 //  no of examples
	     
	           //System.out.println ("test movie is=" + (j) + "Feature =" + doNotPredict);       		
		       			       	
		    	//Get String equivalent of the rating
	        	String myRating = getStringEquivalent(rating);	     
	            
		         //-------------------
		         // Create Test set 
		         //-------------------	    
	        	
	             // create data set (add instance)												 
	            myDataSet = createDataSet(myDataSet,							// Instances
	            						  mid,	                				// mid										
	            						  FeaturesTestMovie_BagsOfWords, 		// features against a movie
	            						  myRating);          
            
    	 
	         //--------------------------------------------------
	         // Evaluate Test Set : Separate Test & Training Set
	         //--------------------------------------------------
	                 
              //Apply stringToWordVector Filter (Filter should be formatted on same object)
              new_DataSet = Filter.useFilter( myDataSet, stwv);
                
	         //-----------------------------------------------
	         // Evaluate the model, through a test instance
	         //-----------------------------------------------	
           
            double pred=0,  act=0, actual=0, error=0;  
           
	       	Instance testInstance = new_DataSet.firstInstance();		//it has only one instance
	       		        	        
	       	//Classify instance 
	       	pred =  myLearnedModel.classifyInstance(testInstance);
	    	prediction = Double.parseDouble(new_DataSet.classAttribute().value((int) pred));	       	
	       	
	    	//System.out.println("prediction=" +prediction);
	    	
	 	  /*  act = testInstance.classValue();   	 	    
	 	    actual = Double.parseDouble(new_DataSet.classAttribute().value((int) act));*/
	 	
	 	
	    	/*if(actual !=rating)
	    	{
	 	    //System.out.println("rating="+rating);
	    	    System.out.println("actual internal format ="+ act + ", actual val="+actual);
	    	    System.out.println("predicted index="+ pred+ ",prediction="+ prediction);	        	    
	    	    System.exit(1);
	    	    System.out.println("--------------------------------");
	    	}
	 	    */
    	
	 	    //print error
	     /*  	error = Math.abs(prediction - actual); 

	         rmse.add(actual, prediction);  
	         rmse.ROC4(actual, prediction, 5, MMh.getAverageRatingForUser(activeUser));	           
	    	*/     	
	  
	      //-----------------------------------------------------
	      // We have a prediction, write it now
	      //-----------------------------------------------------
		      if(prediction !=0)
		      {
		      	String oneSample = activeUser +"," + mid + "," + prediction;
		      	
		      	try {
		      		outT.write(oneSample);
		      		outT.newLine();
		      	}
		      	catch (Exception E) {
		      		E.printStackTrace();
		      		System.out.println("Error writing prediciton into file");
		      	}            	
		      } //end of if 
    	
          } //end movie for
         }//end if 
	 		
      }//end of function
	
/*******************************************************************************************************/
/**
 * @throws Exception 
 *******************************************************************************************************/	

	 
/*   public static void main (String arg[]) throws Exception
	{

	    String test  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\sml_clusteringTestSetStoredTF.dat";
		String train  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\sml_clusteringTrainSetStoredTF.dat";
		String main  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\sml_storedFeaturesRatingsTF.dat";

		TextRec NBT = new TextRec(main, train, test, test, test);
	
		try
		{
			NBT.recommendViaTextClassifier();
		}
		
		catch(Exception E)
		{
			System.out.println("exception" + E);
			E.printStackTrace();
		}
	}
   */
/*******************************************************************************************************/   
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
			   										int classes,									// no of classes		
			   										int uid											// uid
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
	 	              	       boolean word_OK = checkDFThresholding(word, uid);
	 	              	       
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
	   
	   public boolean checkDFThresholding(String word, int uid)
	   {
		  
		   // Get all movies seen by this user
		  LongArrayList movies = MMh.getMoviesSeenByUser(uid); 
	      int moviesSize = movies.size();
	      int mid = 0; 
	             
	      // Define DF threshold per user's rating
	       DF_THRESHOLD = (int)(moviesSize * (0.05));  // at 3 = there was no match
	      
	      //how many times this word occures across the doc
	      int count =0;
		  		   
		  //For all movies
		  for (int i=0;i<moviesSize;i++)
		   {
			     //Get a movie seen by the user
		       	 mid = MemHelper.parseUserOrMovie(movies.getQuick(i));
		       		       	   
		       	 //Get a training feature for this movie
		       	 HashMap<String, Double>FeaturesAgainstAMovie  = MainMh.getFeaturesAgainstAMovie(mid);
		         
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
	   	   
	   	   /**
	   	    * Find active user's priors
	   	    */
	   	   
	   	   public OpenIntDoubleHashMap getActiveUserPriors(int uid, int classes)
	   	   {
	   		   return myFilter.getActiveUserPriors(uid, classes); 
	   	   }
	   	   
	   	   /**
	   	    * Find active user's priors
	   	    */
	   	   
	   	   public OpenIntDoubleHashMap getActiveUserNormalizedPriors(int uid, int classes)
	   	   {
	   		   OpenIntDoubleHashMap myMap =  myFilter.getActiveUserPriors(uid, classes);
	   		   
	   		   double max =0;
	   		   
	   		   //find max
	   		   for(int i=0;i<classes;i++)
	   		   {
	   			   if(max< myMap.get(i))
	   				   max = myMap.get(i);
	   		   }
	   		   
	   		   //normalize
	   		   for(int i=0;i<classes;i++)
	   		   {			
	   			    myMap.put(i, myMap.get(i)/max);
	   		   }
	   		   
	   		   		   
	   		   return myMap;
	   		   
	   	   }
	   	   
	   	   
	   	 //---------------------------------------------
	   	/**
	   	 *  Get priors multiplied by wiehgts of active user's neighbours via CF
	   	 */
	   	   
	   	   public OpenIntDoubleHashMap getWeightedPriors(int whichWeight, int uid, int classes)
	   	   {
	   		   return myFilter.getPriorWeights(whichWeight, uid, classes); 
	   	   }
	   	   
	   	   
	   /******************************************************************************************************/

	   	   
	
}
