package netflix.weka;

import java.util.HashMap;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import cern.colt.list.LongArrayList;
import cern.colt.list.ObjectArrayList;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.evaluation.EvaluationUtils;
import weka.classifiers.evaluation.NominalPrediction;
import weka.classifiers.evaluation.Prediction;
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
import netflix.memreader.*;
import netflix.rmse.RMSECalculator;


public class TextRec 
{

	 // Recommender related object and varaibes
	MemHelper 		MMh;						// train set
    MemHelper 		MTestMh;					// test set	
    MemHelper 		MainMh;						// All Data, will be used to get movie features
   
    //Batch Filter
    Standardize batchFilter;
    
    //StringToWord Filter
    StringToWordVector stwv;	
    
    //Classifier
    Classifier myClassifier;
    
    //Start up RMSE count
    RMSECalculator rmse;
    
    //some other vaaible
    double  DF_THRESHOLD;
    int     totalMovWithNullFeaturesTest;
    int     totalMovWithNullFeaturesTrain;
    int     totalMovTrain;
    HashMap <String, Double> FeaturesMovie;
    
	public TextRec(String mainObject, String trainObject, String testObject) throws Exception 
	{
	  	//Get test and train objects
		MainMh	= new MemHelper (mainObject);
		MMh		= new MemHelper (trainObject);
	    MTestMh = new MemHelper (testObject);
	
	    //filters
	    batchFilter = new Standardize();
	    
	    //String To word Filter
	    stwv = new StringToWordVector();	    
		String[] myTrainOptions = new String[3];	
		myTrainOptions[0] = "-T";
		myTrainOptions[1] = "-C";		//output word count
		myTrainOptions[2] = "-N=1";		//output word count
		
		// myTrainOptions[1] = "-W<2000>";
		 try{
			 stwv.setOptions(myTrainOptions);
			 stwv.setIDFTransform(true);
			 stwv.setMinTermFreq(2);
			 stwv.setOutputWordCounts(true);
		 }
	    	catch(Exception E) {
	    		E.printStackTrace();
	    		System.out.println("error");
	    	}
	    
 	    //create classifier
	    	//LIBSVM
	    /*	 String[] myClassifierOptions = new String[2];	
			 myClassifierOptions[0] = "-T=0";
			 myClassifierOptions[1] = "-K=0";
		     myClassifier = new wlsvm.WLSVM();
		     myClassifier.setOptions(myClassifierOptions);
		     */
	    	
	    	   //  myClassifier = new weka.classifiers.functions.LibSVM();
		           myClassifier = new weka.classifiers.bayes.NaiveBayes();
		 //        myClassifier = new weka.classifiers.bayes.NaiveBayesSimple();	
			    // myClassifier = new weka.classifiers.bayes.NaiveBayesMultinomial();
		     
		//For MAE
		 rmse = new RMSECalculator();
		 
		//var
		totalMovWithNullFeaturesTest = 0;
		totalMovWithNullFeaturesTrain =0;
		totalMovTrain =0;
		FeaturesMovie = null;
	}
	
		
/*******************************************************************************************************/
	/**
	 * This function performs NB classification steps
	 */
	
	public double GeneratePrediction(int activeUser, int targetMovie) throws Exception
	{
		 // For each user (in test set), make recommendations
        double prediction =0;
		LongArrayList movies ;
        int moviesSize = 0;
        double rating = 0.0;
        int mid =0, uid =0;
        String FeaturesTrainMovie_BagsOfWords ="";   
        
        //Create attributes
        FastVector myAttributes = getAttributes("Train"); 

        //Get 
        movies = MMh.getMoviesSeenByUser(activeUser); 					//get movies seen by this user
        moviesSize = movies.size();                     
        Instances myDataSet = null;     
        IntArrayList nonZeroFeatures = new IntArrayList();				//one feature is found
                                   
            //For all movies seen by this user
            for (int j = 0; j < moviesSize; j++)
             {
            	totalMovTrain++;
            	FeaturesMovie =null;
            	
            	mid = MemHelper.parseUserOrMovie(movies.getQuick(j));		    // get mid
            	FeaturesMovie = MMh.getFeaturesAgainstAMovie(mid);			// to check size of features
            	//FeaturesMovie = MMh.getKeywordsAgainstAMovie(mid);
            	              	
            	if(FeaturesMovie!=null )    	  
            	     nonZeroFeatures.add(mid);
              
             } //finished getting train movies size with non-null features
           
           // System.out.println("train movies with features = " + nonZeroFeatures.size());
            
            //Get the train movies feature size
            for (int j = 0; j <nonZeroFeatures.size() ; j++)
            {            
            	mid = nonZeroFeatures.getQuick(j);
            	rating = MMh.getRating(uid, mid);            	   				// get rating
            	
            	 //     FeaturesMovie = MMh.getKeywordsAgainstAMovie(mid);
            	        FeaturesMovie = MMh.getFeaturesAgainstAMovie(mid);
            	
            	HashMap <String, Double> FeaturesMovieDummy;
            	FeaturesMovieDummy = (HashMap <String, Double>)FeaturesMovie.clone();
            	
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

            //  System.out.println("train size = " + myDataSet.numInstances());
            
	            //-------------------------------------
	            // NB data is there, Learn and predict
	            //-------------------------------------       
		    if(nonZeroFeatures.size()>0)
		     {
                // Apply string To Word Vector Filter
                    Instances new_DataSet = applyStringToWordFilter(myDataSet);
                   //System.out.println("train size = " + new_DataSet.numInstances());
                   
                
                // Learn the model
                Classifier predictiveModel = myClassifier;
                predictiveModel = learnPredictiveModel(new_DataSet);
                
                // Evaluate the  model, Make Prediction
            	prediction = evaluatePredictiveModel(activeUser, 				// uid
            										 targetMovie,				// mid
            										 predictiveModel,			// learned model
            										 new_DataSet);				// myTrain Instances
                        
      		  }
		    
            
		    // Now Prediction can be some value if Train and test features are not empty
		    // Else it will be zero		    
		    return prediction;
         

}

/*******************************************************************************************************/	
	/**
	 * build attributes and return them
	 */
 
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
		Instances newData = null;
		
       /*// convert to Non-Sparse
		try{
			NonSparseToSparse sp = new NonSparseToSparse();
		    sp.setInputFormat(myDataSet);
		    newData = Filter.useFilter(myDataSet, sp);
		}
		
		catch (Exception E)
		{
		  E.printStackTrace();
		
		}
		
		// set class index to be rating to be predicted (mid, features, rating)
		   newData.setClassIndex(1);		
		   return newData;
		*/
		
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
	     	while(jTrainClass.hasNext() && total <10) 
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
	
	      // convert to Non-Sparse
		/*	try{
				NonSparseToSparse sp = new NonSparseToSparse();
			    sp.setInputFormat(new_trainData);
			    newData = Filter.useFilter(new_trainData, sp);
			}
			
			catch (Exception E)
			{
			  E.printStackTrace();
			
			}
			
		 return newData;
		 */
		 
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
	
	 public double evaluatePredictiveModel (int activeUser, 								// active User
			 								int targetMovie,								// target Movie	
			 								Classifier myLearnedModel,					// learned classiifer 
			 								Instances trainInstaces) throws Exception		// train dataset
	 {		
		 //create local variables
		 LongArrayList movies;
		 double prediction =0;
		 DoubleArrayList myActualClasses = new DoubleArrayList();
		 IntArrayList nonZeroFeatures = new IntArrayList();				//one feature is found
		 HashMap <String, Double> FeaturesMovie =null;
         double rating;
         int mid;
         int moviesSize = 0;  
         String FeaturesTestMovie_BagsOfWords ="";         
         boolean  doNotPredict = false;
         
         
         Instances myDataSet = null;
         Instances new_DataSet = null;
        
        	 
	     FeaturesMovie = MainMh.getFeaturesAgainstAMovie(targetMovie);				// to check size of features
	   	 //FeaturesMovie =  MainMh.getKeywordsAgainstAMovie(mid);		
	   	 //System.out.println(FeaturesMovie);	   	   
         
	   if(FeaturesMovie!=null)
	    {
          rating = 1;													     	//as we donot have rating for it	
            
          //Create attributes
          FastVector myAttributes = getAttributes("Test");            
          FeaturesTestMovie_BagsOfWords = getBagsOfWords(FeaturesMovie);		// get bags of words
	      	
          myDataSet = getLearningDataSet("testNB",
	    								  myAttributes,			 		 // created attributes 
	    								  nonZeroFeatures.size()); 		 //  no of examples
	     
	        //Get String equivalent of the rating
	        String myRating = getStringEquivalent(rating);
	            
	            
	         //-------------------
	         // Create Test set 
	         //-------------------	    
	        	
            // create data set (add instance)												 
            myDataSet = createDataSet( 	myDataSet,							// Instances
            							targetMovie,                		// mid										
            							FeaturesTestMovie_BagsOfWords, 		// features against a movie
            							myRating);
		      
           
    	 //  System.out.println ("test size=" + myDataSet.numInstances());
    	 
        
         
            //Apply stringToWordVector Filter (Filter should be formatted on same object)
    	       new_DataSet =  Filter.useFilter( myDataSet, stwv); 	            
/*            System.out.println ("new test size=" + new_DataSet.numInstances());
              System.out.println ("new train data=" + trainInstaces);
              System.out.println ("new test data=" + new_DataSet);*/
            
    	    //Predict through an instance
	       	Instance testInstance = new_DataSet.lastInstance();
	       		        	        
	       	//Classify instance 
	       	double pred =  myLearnedModel.classifyInstance(testInstance);
	    	prediction = Double.parseDouble(testInstance.classAttribute().value((int) pred));
	       	//double prediction = pred + 1;
	       	
	 	    //double act = testInstance.classValue();   
	 	    //double actual = act +1 ;
	 	    //double actual = Double.parseDouble(testInstance.classAttribute().value((int) act));
	 	    
    	    /*System.out.println("rating="+rating);
    	    System.out.println("actual internal format ="+ act + ", actual val="+actual);
    	    System.out.println("predicted index="+ pred+ ",prediction="+ prediction);
    	    System.out.println("--------------------------------");
	       */
	    }
	   
	   //Prediction will be zero, if test features are null, else we have some prediction
    	return prediction;   	
	 		
      }
	
/*******************************************************************************************************/
/**
 * @throws Exception 
 *******************************************************************************************************/	

	 /*
   public static void main (String arg[]) throws Exception
	{

	    String test  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\sml_clusteringTestSetStoredTF.dat";
		String train  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\sml_clusteringTrainSetStoredTF.dat";
		String main  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\sml_storedFeaturesRatingsTF.dat";

		TextRec NBT = new TextRec(main, train, test);
	
		try
		{
			NBT.recommendViaTextClassifier();
		}
		
		catch(Exception E)
		{
			System.out.println("exception" + E);
			E.printStackTrace();
		}
	}*/
   
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
		       	 HashMap<String, Double>FeaturesAgainstAMovie  = MMh.getFeaturesAgainstAMovie(mid);
		         
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
	   
	
}
