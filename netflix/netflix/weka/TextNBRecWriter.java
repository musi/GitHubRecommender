package netflix.weka;

// We will call this program from the other class, and then write an object with predictions from
// the svm or other classifiers for the missing value. The written obj can be used in svd writer class
// to give prediction easily.


//This program, is for all other classification algos, like NB etc
//We will learn parameters from the training set and give manual parameters here

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import cern.colt.list.IntArrayList;
import cern.colt.list.LongArrayList;
import cern.colt.list.ObjectArrayList;
import cern.colt.map.OpenIntDoubleHashMap;
import weka.classifiers.Classifier;
//import weka.classifiers.Evaluation;
//import weka.classifiers.evaluation.EvaluationUtils;
//import weka.classifiers.evaluation.NominalPrediction;
//import weka.classifiers.evaluation.Prediction;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Standardize;
import weka.filters.unsupervised.attribute.StringToWordVector;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.bayes.NaiveBayesSimple;						//does not check underflow or so
import weka.classifiers.bayes.NaiveBayesMultinomial;
import netflix.algorithms.memorybased.memreader.FilterAndWeight;
import netflix.memreader.*;
import netflix.rmse.RMSECalculator;
import netflix.utilities.Timer227;

public class TextNBRecWriter 
{

	 // Recommender related object and varaibes
	MemHelper 		MMh;						// train set
    MemHelper 		MTestMh;					// test set	
    MemHelper 		MainMh;						// All Data, will be used to get movie features
   
    //Batch Filter
    Standardize batchFilter;
    
    //StringToWord Filter
    StringToWordVector stwv;	
    
    //FilterAndWeight
    FilterAndWeight myFilter;
    
    //Classifier
    Classifier myClassifier;
    
    
    //Start up RMSE count
    RMSECalculator rmse;
    
    //Timer 
    Timer227  myTimer;
    
    //some other vaaible
    double 					 DF_THRESHOLD;
    int     		 		 totalMovWithNullFeaturesTest;
    int     		 		 totalMovWithNullFeaturesTrain;
    int     				 totalMovTrain;
    int                      whichClassifier;
    HashMap <String, Double> FeaturesMovie;
    BufferedWriter 			 outT;  
    String         			 sparseMMh;
    String         			 predictedDataFile;
    double         			 DF;
    
/*******************************************************************************************************/
 //Constructor
    
	public TextNBRecWriter( String mainObject, 
						 	String trainObject, 
						    String testObject,
						    String dataToWrite,			// dummy File to write data
						    String missingValFileName, 	// MemReader Obj will be written and will be send back
							int	   whichClassifier)							
						    							throws Exception 
	{
	  	//Get test and train objects
		MainMh	= new MemHelper (mainObject);
		MMh		= new MemHelper (trainObject);
	    MTestMh = new MemHelper (testObject);
	    this.whichClassifier = whichClassifier;
	    
	    //----------------------------------------------------
	    // Related to classifiers
	    
	    //filters
	    batchFilter = new Standardize();
	    
	    //FilterAndWeight for CF
	    myFilter = new FilterAndWeight(MMh,1); 			 // FilterAndWeight, Random object, For CF
	    	    
	  //String To word Filter
	    stwv = new StringToWordVector();	    
		String[] myTrainOptions = new String[8];		
		
		//myTrainOptions[1] = "-C";	    //log tf
		myTrainOptions[0] = "-I";		//fij*log(num of Documents/num of documents containing word i)	
		myTrainOptions[1] = "-W";
		myTrainOptions[2] = "5000";
		myTrainOptions[3] = "-N";
		myTrainOptions[4] = "1";
		myTrainOptions[5] = "-S";		// lower case
		myTrainOptions[6] = "-M";		// min term frequency, I think two
		myTrainOptions[7] = "1";		// with two it changes? 
		//myTrainOptions[8] = "-T";		// output word count
		
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
	    
     	    
		        //   myClassifier = new weka.classifiers.bayes.NaiveBayes();
		 //        myClassifier = new weka.classifiers.bayes.NaiveBayesSimple();	
			    // myClassifier = new weka.classifiers.bayes.NaiveBayesMultinomial();
		 
	    	         
		           
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
		 // For each user (in test set), make recommendations
        double prediction 					= 0.0;		
        int    moviesSize 					= 0;
        double rating 						= 0.0;
        int    mid 							= 0;
        int    uid 							= 0;
        int    activeUser       			= 0;
        int    targetMovie       			= 0;
        String FeaturesTrainMovie_BagsOfWords ="";        
        LongArrayList movies ;
        
        //Get all users
        IntArrayList users = MainMh.getListOfUsers();
        int allUsers = users.size();
        
        double C =0.5;
        
        //-------------------------------------------
        // Go through all users, one classifier/user
        //-------------------------------------------
        
        for(int i=0;i<allUsers;i++)
        {       		
        	   //Create attributes
             	FastVector myAttributes = getAttributes("Train");    
           	
                if(i>100 && i%100==0)
            	System.out.println("currently at user =" +(i+1));
              
                //get user
                uid = users.getQuick(i);
                activeUser = uid;
            
                //get movies seen by this user in the training set, for building its profile
                movies = MMh.getMoviesSeenByUser(uid); 					
                moviesSize = movies.size();                     
                                         
                //Get priors weights of the active user
                OpenIntDoubleHashMap priors =  getActiveUserPriors(uid, 5);
                OpenIntDoubleHashMap weightedPriors =  getWeightedPriors(1, uid, 5);
                
                //Determine classifier, and set optimal parameters
                if(whichClassifier==11){
        	    	myClassifier = new weka.classifiers.bayes.NaiveBayes();        	    	
        	    	DF =1.2;
                }
                
        	    else if(whichClassifier==13){
        	    	myClassifier = new weka.classifiers.bayes.NaiveBayesMultinomial();
        	    	DF =1.2;
        	    }
        	    
        	    else if(whichClassifier==14){
        	    	myClassifier = new weka.classifiers.trees.J48();	       	
        	    	DF =1.2;
        	    }
                
        	    else if(whichClassifier==15){
        	    	myClassifier = new weka.classifiers.meta.AdaBoostM1();        	    	 
        	    	DF =1.2;
        	    }   
                
        	    else if(whichClassifier==16){
        	 	    myClassifier = new weka.classifiers.meta.Bagging();
        	 	   DF =1.2;    
        	    }

        	    else if(whichClassifier==17){
        	    	myClassifier = new weka.classifiers.lazy.IBk();
        	    	//new weka.classifiers.lazy.IBk().
        	    	DF =1.2;        	    	
        	    }
       	    
        	  
        	    else if(whichClassifier==18){
        	    	myClassifier = new weka.classifiers.meta.MultiBoostAB();		   
        	    	DF =1.2;
        	    }
        	    
        	    else if(whichClassifier==19){
        	    	myClassifier = new weka.classifiers.lazy.IBk();		   
        	    	DF =0.1;
        	    }
           	    
           	    IntArrayList nonZeroFeatures = new IntArrayList();				//one feature is found
                  
                //--------------------------------------------
                //For all movies seen by this user, Check if user has some features for any of the movie
                for (int j = 0; j < moviesSize; j++)
                 {
                	totalMovTrain++;            	
                	FeaturesMovie =null;
                	
                	mid = MemHelper.parseUserOrMovie(movies.getQuick(j));		    // get mid
                	
                	//determine if this mov has some features
                 	HashMap <String, Double> individualFeatures = null;
        			individualFeatures = MainMh.getFeaturesAgainstAMovie(mid);        	
                
        			if(individualFeatures!=null && individualFeatures.size() >0)
        			{
        	   	    	 nonZeroFeatures.add(mid);
                	  }
                	
                	else {
                		 totalMovWithNullFeaturesTrain++;            
                	}
                }
               
           //--------------------------------------------
           //Now add instances, one instance per movie
           //To Create the dataset (training set)
           //Go through all the movies, which have some features     
                                
                Instances myDataSet = null;                
                          
                for (int j = 0; j <nonZeroFeatures.size() ; j++)
                {             	
                	mid = nonZeroFeatures.getQuick(j);
                	rating = MMh.getRating(uid, mid);            	   				// get rating
                	
                  	FeaturesMovie = MainMh.getFeaturesAgainstAMovie(mid);          	
                	                	            	
                	HashMap <String, Double> FeaturesMovieDummy;
                	FeaturesMovieDummy = (HashMap <String, Double>)FeaturesMovie.clone();
                	
                	//DO Feature Selection
                	FeaturesMovie = doFeatureSelectionByDFThresholding (FeaturesMovieDummy, 5, uid);
                	
                	//Make Bags of Words
                	//FeaturesTrainMovie_BagsOfWords = getBagsOfWords(FeaturesMovie);		// get bags of words
                	
                    //Create learning dataset (only once -- one for each user)          
                    if(j==0)  myDataSet = getLearningDataSet("TrainNB",
                    										 myAttributes,		 	 // created attributes 
                    										 nonZeroFeatures.size()); //  no of examples
               
    		       //Get String equivalent of the rating
    	            String myRating = getStringEquivalent(rating);
    	            
    		       // create data set (add instance)												 
    		         myDataSet = createDataSet( 		myDataSet,							// Instances
    		                							mid,                				// mid										
    		                							FeaturesTrainMovie_BagsOfWords, 	// features against a movie
    		                							myRating);				     		// rating		                
    	                        
    	         
                 } //end of inner for
                    	
    
              
    //--------------------------------------------
    // Make prediction, if we have some features            
    if(nonZeroFeatures.size()>0)
     {
             // Apply string To Word Vector Filter
                Instances new_DataSet = applyStringToWordFilter(myDataSet);                            
                 
              // Learn the model                
                myClassifier.buildClassifier(new_DataSet);   

                // All movies
                int allMovies = MainMh.getNumberOfMovies();            
         
        //Go through all the movies, We have to predict all of them 
        for(int j=1;j<=allMovies;j++)
        {
        	
              // Evaluate the  model
               prediction = evaluatePredictiveModel (activeUser,				// uid
            		   								 j,							// mid
                									 myClassifier				// learned model
                								     );							// myTrain Instances                        
          				    
    		  //If rating (obtained from training set is -99, user has not seen this movie)	
          	 //double myRatingTr = MMh.getRating(activeUser, j);
          	 double myRatingT = MTestMh.getRating(activeUser, j);   
        
          	// if(myRatingT != -99)
          	 {
            		targetMovie = j;            
	         
	                // Evaluate the  model, Make Prediction
	            	prediction = evaluatePredictiveModel(activeUser, 				// uid
	            										 targetMovie,				// mid
	            										 myClassifier);				// learned model
	            			
	            	if(prediction ==0)
	            		prediction = MMh.getAverageRatingForMovie(targetMovie);
	            	if(myRatingT != -99) 		//that means, we have that rating in test set, and we want to predict it
	            		rmse.add(myRatingT, prediction);
	                    
	            //-----------------------------------------------------
	            // We have a prediction, write it now
	            //-----------------------------------------------------
	            if(prediction !=0)
	            {
	            	String oneSample = activeUser +"," + targetMovie + "," + prediction;
	            	
	            	try {
	            		outT.write(oneSample);
	            		outT.newLine();
	            	}
	            	catch (Exception E) {
	            		E.printStackTrace();
	            		System.out.println("Error writing prediciton into file");
	            	}            	
	            } //end of inner if	            
          	 } //end of if rating ==-99  	 
         }//end of for of movies     
	  } //end of if movie feature size is empty
   } // end of user for
        
        try {
        outT.close();
        }        
    	catch (Exception E) {
    		E.printStackTrace();
    		System.out.println("Error writing prediciton into file");
    	}            	

    	myTimer.stop();
        System.out.println("Time taken to make a predictions="+ myTimer.getTime());
        myTimer.resetTimer();
        
    	System.out.println("Error = "+ rmse.mae());
    	System.out.println("Press any key to continue....");    	
    	System.in.read();    	
    	
    	
        //-----------------------------------------
        // write a memReader obj and send back
        //-----------------------------------------
    	MemReader myReader = new MemReader();	 
    	myReader.writeIntoDisk(predictedDataFile, sparseMMh, true); 	
    	
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
		  
		if(FeaturesTrainMovie!=null && FeaturesTrainMovie.size()>0)
		{
		 // Find set and iterators
		  Set setTrainClass = FeaturesTrainMovie.entrySet();  
		  Iterator jTrainClass = setTrainClass.iterator();    	   
		 
		//  myFeatures +="\"";											  // opening string "
		  int total =0;
		  
		  //Iterate over the words of Test set until one of them finishes
	     	while(jTrainClass.hasNext())// && total <10) 
	     	 {
	     	     total++; 
	     		 Map.Entry words  = (Map.Entry)jTrainClass.next();        // Next 		 
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
	     //	myFeatures.trim();
	    	
	     	myFeatures+=" ";
	     	
	     	//System.out.println(myFeatures);
	     	return myFeatures;
		}
		
		return myFeatures;
	 }
	 
/*******************************************************************************************************/
	 
	/**
	 * Apply StringToWordVector
	 */
	
	public Instances applyStringToWordFilter (Instances myData) throws Exception
	{
		//StringToWordVector for train set
		 stwv.setInputFormat(myData);
		 Instances new_trainData = Filter.useFilter(myData, stwv); 	//instances, stringToWordFilter		
		 
		 return new_trainData;
	}


	
/*******************************************************************************************************/
	/**
	 * Build and learn the Naive Bayes classfier on training data set
	 */

	public Classifier learnPredictiveModel (Instances myData) throws Exception
	{			     
		// build classifier
		   myClassifier.buildClassifier(myData);		   
		   return myClassifier;
	}
	
/*******************************************************************************************************/	

	/**
	 * Make prediction for the test set 
	 */
	
	 public double evaluatePredictiveModel (int uid,				   // uid
			 							    int mid,				   // mid			
			 							    Classifier myLearnedModel  // learned classiifier 
			 							 )  throws Exception		   // train dataset
	 {		
		 //create local variables		 		 
		 IntArrayList nonZeroFeatures 			=	new IntArrayList();				//one feature is found
		 HashMap <String, Double> FeaturesMovie =	null;
         String FeaturesTestMovie_BagsOfWords 	=	"";           
         Instances myDataSet 					= null;
         Instances new_DataSet 					= null;                 
                  
                   
     	 
         //Create attributes
          FastVector myAttributes = getAttributes("Test");
          
          //get features
          FeaturesMovie = MainMh.getFeaturesAgainstAMovie(mid);
          
        if(FeaturesMovie != null && FeaturesMovie.size() >0)
         {
          //get bag-of-words
          FeaturesTestMovie_BagsOfWords = getBagsOfWords(FeaturesMovie);		// get bags of words	       	
 	
          //get dataset
          myDataSet = getLearningDataSet(	"testNB",
	        			  					 myAttributes,			 		 // created attributes 
	        		  						 nonZeroFeatures.size()); 		 //  no of examples
	     
	      //we do not know rating, assign a dummy one      		
          String myStringRating = "1.0";
	            
		      //-------------------
		      // Create Test set 
		      //-------------------	    
	        	
	             // create data set (add instance)												 
	            myDataSet = createDataSet( 	myDataSet,							// Instances
	            							mid,                				// mid										
	            							FeaturesTestMovie_BagsOfWords, 		// features against a movie
	            							myStringRating);
		      
          
         
            //Apply stringToWordVector Filter (Filter should be formatted on same object)
             new_DataSet = Filter.useFilter( myDataSet, stwv);
         
         
	         //-----------------------------------------------
	         // Evaluate the model, through a test instance
	         //-----------------------------------------------
	
           
            double pred=0, prediction=0, act=0, actual=0, error=0;  
            
           	Instance testInstance = new_DataSet.lastInstance();
	       		        	        
	       	//Classify instance 
	       	pred =  myLearnedModel.classifyInstance(testInstance);
	    	prediction = Double.parseDouble(new_DataSet.classAttribute().value((int) pred)); 	   
	 	    
	    	
	    	return prediction;
	    	
           } //end of if  
  
	 	return 0;
		
	 }
	

   
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
	       DF_THRESHOLD = (int)(moviesSize * (DF));  // at 3 = there was no match
	      
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
		  		if(count>=DF_THRESHOLD) // && count <moviesSize-1 )
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