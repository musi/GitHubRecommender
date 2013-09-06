package netflix.weka.Classifiers;

import java.io.FileWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import cern.colt.list.LongArrayList;
import cern.colt.list.ObjectArrayList;

import weka.classifiers.*;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.bayes.NaiveBayesMultinomial;
import weka.classifiers.bayes.NaiveBayesSimple;
import weka.classifiers.evaluation.EvaluationUtils;
import weka.classifiers.evaluation.NominalPrediction;
import weka.classifiers.evaluation.Prediction;
import weka.classifiers.meta.AdaBoostM1;
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
import netflix.memreader.*;
import netflix.rmse.RMSECalculator;


public class DTBoostingByTextAttribute 
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
    weka.classifiers.meta.AdaBoostM1  myClassifier;
    
    //Start up RMSE count
    RMSECalculator rmse;
    
    //some other vaaible
    double  DF_THRESHOLD;
    double  X2_THRESHOLD;
    double	DF;
    int     totalMovWithNullFeaturesTest;
    int     totalMovWithNullFeaturesTrain;
    int     totalMovTrain;
    int 	totalPredictionsMade;
    int 	totalSamples;
    HashMap <String, Double> FeaturesMovie;
    
  
    //File Writers
    FileWriter myWriter[];
    String	path;
    
    //-----------------------------------------------
    
	public DTBoostingByTextAttribute(String path, 
							 String mainObject, 
							 String trainObject,
							 String testObject) throws Exception 
	{
	  	//Get test and train objects
		MainMh		= new MemHelper (mainObject);
		MMh			= new MemHelper (trainObject);
	    MTestMh 	= new MemHelper (testObject);
	    this.path 	= path;
	    
	    //filters
	    batchFilter = new Standardize();
	    
	    //String To word Filter
	    stwv = new StringToWordVector();	    
		String[] myTrainOptions = new String[8];		
		
		//myTrainOptions[1] = "-C";	    //log tf
		myTrainOptions[0] = "-I";		//fij*log(num of Documents/num of documents containing word i)	
		myTrainOptions[1] = "-W";
		myTrainOptions[2] = "5000";
		myTrainOptions[3] = "-N";
		myTrainOptions[4] = "1";
		myTrainOptions[5] = "-S";		//lower case
		myTrainOptions[6] = "-M";		// min term frequency, I think two
		myTrainOptions[7] = "1";		// with two it changes? 
		//myTrainOptions[8] = "-T";		//output word count
		
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
	    	 	       	       
		     
		//For MAE
		 rmse = new RMSECalculator();
		
	    //Files
	     myWriter = new FileWriter[2];	//see file open section
		
		 
		//var
		totalMovWithNullFeaturesTest = 0;
		totalMovWithNullFeaturesTrain =0;
		totalPredictionsMade=0;
		totalSamples=0;
		totalMovTrain =0;
		FeaturesMovie = null;
	}
	
		
/*******************************************************************************************************/
	/**
	 * This function performs NB classification steps
	 */
	
/*	 -P <num>
	  Percentage of weight mass to base training on.
	  (default 100, reduce to around 90 speed up)

	 -Q
	  Use resampling for boosting.

	 -S <num>
	  Random number seed.
	  (default 1)

	 -I <num>
	  Number of iterations.
	  (default 10)

	 -D
	  If set, classifier is run in debug mode and
	  may output additional info to the console

	 -W
	  Full name of base classifier.
	  (default: weka.classifiers.trees.DecisionStump)

	 
	 Options specific to classifier weka.classifiers.trees.DecisionStump:
	 

	 -D
	  If set, classifier is run in debug mode and
	  may output additional info to the console*/
	
	
	public void doNBSteps() throws Exception
	{
		int loop=4;
		
		myClassifier = new weka.classifiers.meta.AdaBoostM1();
	 	myClassifier.setClassifier(new weka.classifiers.bayes.NaiveBayes());
		
		// For each user (in test set), make recommendations
        IntArrayList users = MMh.getListOfUsers();
        LongArrayList movies ;
        int moviesSize = 0;
        int uid, mid; 
        double rating = 0.0;
        String FeaturesTrainMovie_BagsOfWords ="";        
        
        
        //Create attributes
        FastVector myAttributes = getAttributes("Train"); 
		
             	 
     	 
    for(int I =10;I<30;I+=5)
    {   
    	myClassifier.setNumIterations(I);
   	 	openFiles();
   	 	int totalIteration =0;						//reset loop controller
   	 	
   	 
   	while(totalIteration<20)    	  	    	  
   	{  
   		//define feature selection to be done   
   		    DF = totalIteration/10.0;    	
	        totalIteration+=2;
	       
	        
        //For all users
        for (int i = 0; i < users.size(); i++)        
      // for (int i = 0; i < 1; i++)
        {    	
            if(i>100 && i%100==0)
        	System.out.println("currently at user =" +(i+1));
          
            uid = users.getQuick(i);          
            movies = MMh.getMoviesSeenByUser(uid); 							//get movies seen by this user
            moviesSize = movies.size();                     
            Instances myDataSet = null;            
            
            //System.out.println("train movies = " + moviesSize);
            
            IntArrayList nonZeroFeatures = new IntArrayList();				//one feature is found
                                   
            //For all movies seen by this user
            for (int j = 0; j < moviesSize; j++)
             {
            	totalMovTrain++;            	
            	FeaturesMovie =null;
            	
            	mid  = MemHelper.parseUserOrMovie(movies.getQuick(j));		    // get mid
            	FeaturesMovie = MainMh.getFeaturesAgainstAMovie(mid);			// to check size of features
            	         
            	if((FeaturesMovie.size() >0)){    	  
            	     nonZeroFeatures.add(mid);
            	  }
            	
            	else {
            		 totalMovWithNullFeaturesTrain++;            
            	}
            }
           
           // System.out.println("train movies with features = " + nonZeroFeatures.size());
            
            for (int j = 0; j <nonZeroFeatures.size() ; j++)
            {             	
            	mid = nonZeroFeatures.getQuick(j);
            	rating = MMh.getRating(uid, mid);            	   				// get rating
            	
            	FeaturesMovie = MMh.getKeywordsAgainstAMovie(mid);
            	//FeaturesMovie = MainMh.getFeaturesAgainstAMovie(mid);
            	
            	HashMap <String, Double> FeaturesMovieDummy;
            	FeaturesMovieDummy = (HashMap <String, Double>)FeaturesMovie.clone();
            	
            	//DO Feature Selection
            	FeaturesMovie = doFeatureSelectionByDFThresholding (FeaturesMovieDummy, 5, uid);
            	
            	//Make Bags of Words
            	FeaturesTrainMovie_BagsOfWords = getBagsOfWords(FeaturesMovie);		// get bags of words
            	
                //Create learning dataset (only once -- one for each user)          
                if(j==0)  myDataSet = getLearningDataSet("TrainNB",
                										 myAttributes,		 	 // created attributes 
                										 nonZeroFeatures.size()); //  no of examples
           
		       //Get String equivalent of the rating
	            String myRating = getStringEquivalent(rating);           
		        
	            //System.out.println("myRating ="+ myRating);
	                	
		        // create data set (add instance)												 
		          myDataSet = createDataSet( 		myDataSet,							// Instances
		                							mid,                				// mid										
		                							FeaturesTrainMovie_BagsOfWords, 	// features against a movie
		                							myRating);				     		// rating		                
	                        
	         
             } //end of inner for
                	
             //  System.out.println("train size = " + myDataSet.numInstances());
            
		    if(nonZeroFeatures.size()>0)
		     {
                // Apply string To Word Vector Filter
                   Instances new_DataSet = applyStringToWordFilter(myDataSet);
                   //System.out.println("train size = " + new_DataSet.numInstances());
              
                // Learn the model                
                   myClassifier.buildClassifier(new_DataSet);
                
                // Evaluate the  model
            	   evaluatePredictiveModel(uid, 					// uid
            							myClassifier,				// learned model
            							new_DataSet);				// myTrain Instances                        
      		  }		    
        } //end of outer for        
         
         // rmse.add(MTestMh.getRating(uid, mid), prediction);
         System.out.println(loop-1); 
         System.out.println("ROC Sensitivity =" + rmse.getSensitivity());
         System.out.println("ROC specificity =" + rmse.getSpecificity());
         System.out.println("MAE  =" + rmse.mae());
         System.out.println("totalMovWithNullFeaturesTest= "+totalMovWithNullFeaturesTest);
         System.out.println("totalMovWithNullFeaturesTrain= "+totalMovWithNullFeaturesTrain);
         System.out.println("totalMovTrain= "+totalMovTrain);
         System.out.println("total samples= "+totalSamples);
         System.out.println("total predictions made= "+totalPredictionsMade);
         System.out.println("--------------------------------------------------");
         
    
      	 myWriter[0].append(""+ rmse.mae());
		 myWriter[0].append(",");		 
		 myWriter[1].append(""+ rmse.getSensitivity());
		 myWriter[1].append(",");
		 
         rmse.resetValues();
         rmse.resetFinalROC();
         
		}//end of inner while 
   	
   	myWriter[0].append(""+ I);
   	myWriter[1].append(""+ I);
   	
   	 myWriter[0].append("\n");
	 myWriter[1].append("\n");
	 myWriter[0].close();
	 myWriter[1].close();
	
    
    } //end for
    
	
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
	     	while(jTrainClass.hasNext() )//&& total <10) 
	     	 {
	     	    total++; 
	     		Map.Entry words  = (Map.Entry)jTrainClass.next();          // Next 		 
	     	     String word 	 = (String)words.getKey();				  // Get word	    
	     	     double TF 		 = FeaturesTrainMovie.get(word);          // Get TF   	     	
	    
	     	     if(TF>=1)	
	     	     {														//A sort of filter
		     	     //build bags of words
		     	     for (int i=0; i<TF;i++)
		     	     {
		     	    	 myFeatures += word;							  // words separated by space	
		     	    	 myFeatures +=" ";
		     	     }
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
		
		//StringToWordVector for train set
		 stwv.setInputFormat(myData);
		 Instances new_trainData = Filter.useFilter(myData, stwv); 	//instances, stringToWordFilter		
	
		 return new_trainData;
	}

	
/*******************************************************************************************************/

	/**
	 * Make prediction for the test set 
	 */
	
	 public void evaluatePredictiveModel (int activeUser, 								//uid
			 							 weka.classifiers.meta.AdaBoostM1 myLearnedModel,	   // learned classiifer 
			 							  Instances trainInstaces) throws Exception		// train dataset
	 {		
		 //create local variables
		 LongArrayList movies; 
		 IntArrayList nonZeroFeatures 			= 	new IntArrayList();				//one feature is found
		 HashMap <String, Double> FeaturesMovie =	null;            
         Instances myDataSet 					= 	null;
         Instances new_DataSet 					= 	null;
         String FeaturesTestMovie_BagsOfWords 	=	"";
         double rating							=	0;
         double	prediction 						=	0;
         int mid								=   0;
         int moviesSize 						=	0;         
                   
         //get movies seen  by this user
         movies = MTestMh.getMoviesSeenByUser(activeUser); 							//get movies seen by this user
         moviesSize = movies.size();   
         
         //For all movies seen by this user (Test Set)
         for (int j = 0; j < moviesSize; j++)
         {
        	 	totalSamples++;
	       	    mid = MemHelper.parseUserOrMovie(movies.getQuick(j));			    // get mid	       	 	
	   	        FeaturesMovie = MainMh.getFeaturesAgainstAMovie(mid);				// to check size of features
	   	    //  FeaturesMovie = MTestMh.getKeywordsAgainstAMovie(mid);		

		   	 
	   	     if(!(FeaturesMovie == null || FeaturesMovie.size() <=1))
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
        	totalPredictionsMade++;
        	mid = nonZeroFeatures.getQuick(j);
            rating = MTestMh.getRating(activeUser, mid);            	  				// get rating
            FeaturesMovie = MainMh.getFeaturesAgainstAMovie(mid);              
           
            //Create attributes
            FastVector myAttributes = getAttributes("Test");
            
            FeaturesTestMovie_BagsOfWords = getBagsOfWords(FeaturesMovie);		// get bags of words
	       	
	        //Create learning dataset (only once -- one for each user)
            if(j==0)
            myDataSet = getLearningDataSet(				"testNB",
	        			  								myAttributes,			 		 // created attributes 
	        		  									nonZeroFeatures.size()); 		 //  no of examples
	     
	           //System.out.println ("test movie is=" + (j) + "Feature =" + doNotPredict);       		
		       			       	
		    	//Get String equivalent of the rating
	        	String myRating = getStringEquivalent(rating);
	     
	            
		         //-------------------
		         // Create Test set 
		         //-------------------	    
	        	
	             // create data set (add instance)												 
	            myDataSet = createDataSet( 	myDataSet,							// Instances
	            							mid,                				// mid										
	            							FeaturesTestMovie_BagsOfWords, 		// features against a movie
	            							myRating);
		      
           } //end of building dataSet
            
    	 
         //--------------------------------------------------
         // Evaluate Test Set : Separate Test & Training Set
         //--------------------------------------------------
                 
            //Apply stringToWordVector Filter (Filter should be formatted on same object)
             new_DataSet = Filter.useFilter( myDataSet, stwv);
                
	         //-----------------------------------------------
	         // Evaluate the model, through a test instance
	         //-----------------------------------------------
	
           
            double pred=0,  act=0, actual=0, error=0;  
            
          for (int i = 0; i < new_DataSet.numInstances(); i++)
          {
	       	Instance testInstance = new_DataSet.instance(i);
	       		        	        
	       	//Classify instance 
	       	pred =  myLearnedModel.classifyInstance(testInstance);
	    	prediction = Double.parseDouble(new_DataSet.classAttribute().value((int) pred));	       	
	       	
	 	    act = testInstance.classValue();   	 	    
	 	    actual = Double.parseDouble(new_DataSet.classAttribute().value((int) act));
	 	
	 	
	   /* 	if(actual !=rating)
	    	{
	 	    //System.out.println("rating="+rating);
	    	    System.out.println("actual internal format ="+ act + ", actual val="+actual);
	    	    System.out.println("predicted index="+ pred+ ",prediction="+ prediction);	        	    
	    	   //b System.exit(1);
	    	    System.out.println("--------------------------------");
	    	}
	 	    */
    	
	 	    //print error
	       	error = Math.abs(prediction - actual); 
/*	       	System.out.println("uid, mid" + uid +"," + mid + "--> Error = (Actual - predicted)"+
	         					error + " =" + rating + "-" + prediction);	       	
*/	       	 //rating = actual;
	         rmse.add(actual, prediction);  
	         rmse.ROC4(actual, prediction, 5, MMh.getAverageRatingForUser(activeUser));	           
	    	
           } //end of if  
      }
   // }//dummy for end
        	  
  }
	
/*******************************************************************************************************/
/**
 * @throws Exception 
 * *****************************************************************************************************/	

   public static void main (String arg[]) throws Exception
	{
 	    int xFactor = 80;
		String path   = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\SVD\\FeaturesPlay\\";
	    String test   = path + "sml_clusteringTestSetStoredTF.dat";
		String train  = path + "sml_clusteringTrainSetStoredTF.dat";
		String main   = path + "sml_storedFeaturesRatingsTF.dat";
		
		DTBoostingByTextAttribute NBT = new DTBoostingByTextAttribute(path, main, train, test);
	
		try
		{
			NBT.doNBSteps();
		}
		
		catch(Exception E)
		{
			System.out.println("exception" + E);
			E.printStackTrace();
		}
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
		  		if(count>=DF_THRESHOLD)
		  			return true;
		 
		   //else we return false
		  		return false;
		   
	   }
	   

	   
/*******************************************************************************************************/   
	   	 /**
	   	 * Do X2 Thresholding
	   	 * @param hashMap,  consisiting of features against a movie
	   	 * @param Classes,  co. of classes 
	   	 * @param uid,      user id for which a classifier is to be trainied
	   	 * @param type,     type of the feature 
	   	 * @return HashMap,	Seleceted features
	   	 */

	      //This fnctions receive a list of features and then check x2 value for each word
	      //It then removes words having x2 less than a predefined threshold.
	      
	      public HashMap<String, Double> doFeatureSelectionByX2Thresholding( 
	   		   										HashMap<String, Double> movieFeatures,			   
	   		   										int classes,									// no of classes		
	   		   										int uid,										// uid
	   		   										int type)										// type
	      {
	   	   // define list of words to be removed
	   	   ObjectArrayList removeTheseWords = new ObjectArrayList();
	   	   
	   	   	//proceed if movie feature size is not zero
	   	   	if(movieFeatures.size() !=0)
	   	   	{
	             		  //Get entry sets for train class vector  	      	       	  
	                	  Set setTrainClass    = movieFeatures.entrySet();               	  
	                	  Iterator jTrainClass = setTrainClass.iterator();
	                  	               	              	  
	                	  //Iterate over the words of Test set until all are processed
	    	              	while(jTrainClass.hasNext()) 
	    	              	 {
	    	              	     Map.Entry words = (Map.Entry)jTrainClass.next();         // Next 		 
	    	              	     String word 	 = (String)words.getKey();			      // Get a word from the train class
	    	              	      	              	     
	    	              	     // Check in how many movies this word occures
	    	              	       boolean word_OK = checkX2Thresholding(word, uid, type);
	    	              	       
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
	       * X2 Threshold cheking for each word
	       */
	      
	      public boolean checkX2Thresholding(String word, int uid, int type)
	      {	  
	   	   
	   	   //define X2 varaibles
	   	   int A[] = new int[5];	//is the number of documents from class Cj that contain word i,
	   	   int B[] = new int[5];	//is the number of documents that contain word i but are not from class Cj
	   	   int C[] = new int[5];	//is the number of documents from class Cj that does not contain word i,
	   	   int D[] = new int[5];	//is the number of documents that neither contain word i nor they are from class Cj
	   	   double x2[] = new double[5];
	   	   int N = 0;
	   	   
	   	   double x2_Average = 0;
	   	   double x2_Max	 = -500000;	   
	   	   
	   	   // Get all movies seen by this user
	   	   LongArrayList movies = MMh.getMoviesSeenByUser(uid); 
	       int moviesSize = movies.size();
	       int mid = 0;
	       double rating = 0;
	                
	         // Define DF threshold per user's rating
	         X2_THRESHOLD = 0.002;						//(For Max it is 0.004, for avg it is like -ve) 
	         
	         //how many times this word occures across the doc
	         int count = 0;
	         
	         //-------------------------------
	   	  // A,B,C,D
	    	  //-------------------------------
	   	  
	   	  //For all movies
	   	  for (int i=0;i<moviesSize;i++)
	   	   {
	   		     //Get a movie seen by the user
	   	       	 mid = MemHelper.parseUserOrMovie(movies.getQuick(i));
	   	         rating = MMh.getRating(uid, mid);
	   	    	 int classIndex = (int) rating;
	   	    	   
	   	       	 //Get a training feature for this movie
	   	       	 HashMap<String, Double>FeaturesAgainstAMovie  = MainMh.getFeaturesAgainstAMovie(mid);
	   	         
	   	       	 //check for match
	   	       	 if (FeaturesAgainstAMovie !=null) 
	   	       	 {	          
	   	       		 if(FeaturesAgainstAMovie.containsKey(word))
	   	       		 {
	   	       			A[classIndex-1]++; 
	   	       		 }	       		 
	   	       		 else
	   	       		 {
	   	       			C[classIndex-1]++;
	   	       		 }	       		 
	   	       		 
	   	       	 } //end of if	       	 
	   	       	
	   	     } //end of for
	   	 	 
	   	  //-------------------------------
	   	  // Now compute X2
	      //-------------------------------
/*
	   	  //priors
	   	  double priors[] = getPrior(uid,5);
	   	  N = moviesSize;
	   	  
	   	  int x2_Total = 0;
	   	  for(int i=0;i<myClasses;i++)
	   	  {
	   		  for(int j=0;j<myClasses;j++) //Compute D, B
	   		  {
	   			  if(j!=i)
	   			  { 
	   				  D[i]+= C[j];
	   				  B[i]+= A[i];
	   			  }
	   			  
	   		  }
	   		  
	   		  double num = N * ((A[i] * D[i]) - (C[i] * B[i])); //N * (AD -CB)
	   		  double den = (A[i] + C[i]) * (B[i] + D[i]) * (A[i] + B[i]) * (C[i] + D[i]);
	   		  
	   		  if(den!=0) x2[i] = num/den;		//avoid divide by zero
	   		  else 		 x2[i] = 0;	  
	   		  
	   		  System.out.print("class="+i);
	   		  System.out.print(",A =" +A[i]);
	   		  System.out.print(",B =" +B[i]);
	   		  System.out.print(",C =" +C[i]);
	   		  System.out.print(",D =" +D[i]);
	   		  System.out.print(",N =" +N);
	   		  System.out.print(",Num ="+num);
	   		  System.out.print(",Den ="+ den);
	   		  System.out.print(",x2="+ x2[i]+ ", ");
	   		  System.out.println();
	   		  		  
	   		  //find x2_Average and x2_Max
	   		  x2_Total += x2[i];
	   		  x2_Average += (priors[i]*x2[i]);		  
	   		  if(x2[i] > x2_Max && x2[i]!=0.0)
	   			  x2_Max = x2[i];
	   		  
	   		  
	   	  }
	   	  
	         // System.out.println(x2_Average +","+x2_Max);	  
*/	   	  
	   	  // If this word occures across DF_THRESHOLD no. of docs (movies), send true	
	   	  		if(x2_Max>=X2_THRESHOLD)//  && count <moviesSize-5 )
	   	  			return true;
	   	 
	   	   //else we return false
	   	  		return false;
	   	   
	      }
	      
	  
/******************************************************************************************************/
		   
	  	//---------------------------------------------
	  	   
	  	 public void openFiles()
	    	 {
	    		 String myPath =path + "Results\\";
	    		 
	    		 try{
	    			 myWriter[0] = new FileWriter(myPath +"MAE_NBBoostingByTextAttribute.csv", true); //true, append in existing file
	    			 myWriter[1] = new FileWriter(myPath +"ROC_NBBoostingByTextAttribute.csv", true); //true, append in existing file
	    		 }
	    		 
	    		 catch( Exception E) {
	    			 		E.printStackTrace(); 			 
	    		 }
	    	 }

	  	//---------------------------------------------
	  	   
	  	 public void closeFiles()
	    	 {
	  	  		 
	    		 try{
	    			 myWriter[0].close();
	    			 myWriter[1].close();
	    		 }
	    		 
	    		 catch( Exception E) {
	    			 		E.printStackTrace(); 			 
	    		 }
	    	 }
	   
	  	 
	      
}

//-------------------------------------------------------------------------------------------
// NB, no FS --> 1.17
// NBM, no FS --> 1.4245944322050872
// AdaBoost, no FS --> 1.3541458041257761








