package netflix.weka;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import cern.colt.list.LongArrayList;


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
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Standardize;
import weka.filters.unsupervised.attribute.StringToWordVector;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.bayes.NaiveBayesSimple;						//does not check underflow or so
import weka.classifiers.bayes.NaiveBayesMultinomial;
import netflix.memreader.*;
import netflix.rmse.RMSECalculator;


public class SVMPredictor 
{

	 // Recommender related object and varaibes
	MemHelper 		MMh;						// train set
    MemHelper 		MTestMh;					// test set	
   
    //Batch Filter
    Standardize batchFilter;
    
    //StringToWord Filter
    StringToWordVector stwv;	
    
    //Classifier
    Classifier myClassifier;
    
    //Start up RMSE count
    RMSECalculator rmse;
    
	public SVMPredictor(String trainObject, String testObject) 
	{
	  	//Get test and train objects
	    MMh		= new MemHelper (trainObject);
	    MTestMh = new MemHelper (testObject);
	
	    //filters
	    batchFilter = new Standardize();
	    
	    //String To word Filter
	    stwv = new StringToWordVector();	    
		String[] myTrainOptions = new String[1];	
		myTrainOptions[0] = "-T";
		// myTrainOptions[1] = "-W<2000>";
		 try{
			 stwv.setOptions(myTrainOptions);
			 stwv.setIDFTransform(true);
		 }
	    	catch(Exception E) {
	    		E.printStackTrace();
	    		System.out.println("error");
	    	}
	    
 	    //create classifier
		 // myClassifier = new weka.classifiers.bayes.NaiveBayesMultinomial();
		    myClassifier = new weka.classifiers.bayes.NaiveBayes();
		 // myClassifier = new weka.classifiers.bayes.NaiveBayesSimple();	
		
		//For MAE
		 rmse = new RMSECalculator();
	}
	
	
/*******************************************************************************************************/
	/**
	 * This function performs NB classification steps
	 */
	
	public void makeData() throws Exception
	{
		 // For each user (in test set), make recommendations
        IntArrayList users = MMh.getListOfUsers();
        LongArrayList movies ;
        int moviesSize = 0;
        int uid, mid; 
        double rating = 0.0;
        String FeaturesTrainMovie_BagsOfWords ="";        
        HashMap <String, Double> FeaturesMovie =null;        
        
        //Create attributes
        FastVector myAttributes = getAttributes("Train"); 
		
        int myMinUser =0;
        int myMinMov = 100;
        
        for (int i = 1; i < users.size(); i++)
        {
        	uid = users.getQuick(i);
        	movies = MMh.getMoviesSeenByUser(uid); //get movies seen by this user
            if(movies.size() <myMinMov) {myMinMov = movies.size(); myMinUser =uid;}
        }
        
        //For all users
         for (int i = myMinUser; i < users.size(); i++)        
       // for (int i = 0; i < 10; i++)
        {       	
       	
        	System.out.println("currently at user =" +(i+1));
            uid = users.getQuick(i);          
            movies = MMh.getMoviesSeenByUser(uid); 							//get movies seen by this user
            moviesSize = movies.size();                     
            Instances myDataSet = null;            
            
            System.out.println("train movies = " + moviesSize);
            
            IntArrayList nonZeroFeatures = new IntArrayList();				//one feature is found
            
                       
            //For all movies seen by this user
            for (int j = 0; j < moviesSize; j++)
            {
            	mid = MemHelper.parseUserOrMovie(movies.getQuick(j));			// get mid
            	//FeaturesMovie = MMh.getFeaturesAgainstAMovie(mid);			// to check size of features
            	FeaturesMovie = MMh.getKeywordsAgainstAMovie(mid);
            	   
            	if(!(FeaturesMovie == null || FeaturesMovie.size() <=5))   
            	  {
            			   nonZeroFeatures.add(mid);
            	  }
            }
           
            System.out.println("train movies with features = " + nonZeroFeatures.size());
            
            for (int j = 0; j <nonZeroFeatures.size() ; j++)
            {
            
            	mid = nonZeroFeatures.getQuick(j);
            	rating = MMh.getRating(uid, mid);            	   				// get rating
            	FeaturesTrainMovie_BagsOfWords = getBagsOfWords(mid, MMh);		// get bags of words
            	
                //Create learning dataset (only once -- one for each user)          
                if(j==0)  myDataSet = getLearningDataSet(myAttributes,		 	// created attributes 
                										nonZeroFeatures.size()); 			//  no of examples
                 
	                
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
                	
	            //-------------------------------------
	            // Data is there, Learn and predict
	            //-------------------------------------
            
                   System.out.println("train size = " + myDataSet.numInstances());
            
                // Apply string To Word Vector Filter
                   Instances new_DataSet = applyStringToWordFilter(myDataSet);
                   //System.out.println("train size = " + new_DataSet.numInstances());
                   
                // Initialise the Batch Processing Filter and Apply Batch Filter
                   //  initBatchFilter(new_DataSet);
                   //  Instances new_TrainSet = Filter.useFilter(new_DataSet, batchFilter);
                   //  System.out.println("train size = " + new_TrainSet.numInstances());
                   
                // Learn the model
                Classifier predictiveModel = myClassifier;
                // predictiveModel = learnPredictiveModel(new_TrainSet);
                
                // Evaluate the  model
            	evaluatePredictiveModel(uid, 						// uid
            							predictiveModel,			// learned model
            							new_DataSet);				// myTrain Instances
                        
            
        } //end of outer for
        
        
        
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
	
public String getBagsOfWords (int mid, MemHelper myObj)
 {
	 String myFeatures ="";
	 HashMap <String, Double> FeaturesTrainMovie = new HashMap <String, Double>();
	 //FeaturesTrainMovie = myObj.getFeaturesAgainstAMovie(mid);
	 FeaturesTrainMovie = myObj.getKeywordsAgainstAMovie(mid);
	  
	 // Find set and iterators
	  Set setTrainClass = FeaturesTrainMovie.entrySet();  
	  Iterator jTrainClass = setTrainClass.iterator();    	   
	 
	//  myFeatures +="\"";											 // opening string "
	  int total =0;
	  
	  //Iterate over the words of Test set until one of them finishes
     	while(jTrainClass.hasNext() && total <10) 
     	 {
     	     total++; 
     		 Map.Entry words = (Map.Entry)jTrainClass.next();         // Next 		 
     	     String word 	 = (String)words.getKey();				  // Get word	    
     	     double TF 		 = FeaturesTrainMovie.get(word);          // Get TF   	     	
    
     	     //build bags of words
     	     for (int i=0; i<TF;i++)
     	     {
     	    	 myFeatures += word;									 // words separated by space	
     	    	 myFeatures +=" ";
     	     }
     	     
     	 }//end of while
  
     //	myFeatures +="\"";										  	 // closing string "			
     	myFeatures.trim();
     	//System.out.println(myFeatures);
     	return myFeatures;
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
		Attribute movieRating = new Attribute("rating", ratingAtt);
		
		//Bags_Of_Words attribute
		Attribute movieDescription = new Attribute("movieDescription" , (FastVector) null);
					
		//Add Elements to the allAttribute							
		allAttributes.addElement(movieDescription);			// Must add in order
		allAttributes.addElement(movieRating);		
		return allAttributes;
	}
	

/*******************************************************************************************************/
 /**
  *  Create Instances and set class
  */	
	
	public Instances getLearningDataSet (FastVector allAttributes, 		// created attributes
										 int howManyExamples)			// no of examples										
	{		
		//create instances
		Instances myDataSet = new Instances ("SVM", allAttributes, howManyExamples);
		
		// set class index to be rating to be predicted (mid, features, rating)
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
		Instance instance = new DenseInstance (2);
		instance.setDataset(myInstances);		
		instance.setValue(0, bagsOfWords);				// only two attributes are enough --> (bags of words, rating)
		instance.setValue(1, rating);
		
		myInstances.add(instance);						// add created instance in Instances
		return myInstances;
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
	
	 public void evaluatePredictiveModel (int uid, 										//uid
			 							  Classifier myLearnedModel,					// learned classiifer 
			 							  Instances trainInstaces) throws Exception		// train dataset
	 {
		
		 //create local variables
		 LongArrayList movies;
		 DoubleArrayList myActualClasses = new DoubleArrayList();
		 IntArrayList nonZeroFeatures = new IntArrayList();						 //one feature is found
		 HashMap <String, Double> FeaturesMovie = null;
         double rating;
         int mid;
         int moviesSize = 0;  
         String FeaturesTestMovie_BagsOfWords ="";         
         
         //Create attributes
         FastVector myAttributes = getAttributes("Test");
         
         Instances myDataSet = null;
         Instances new_DataSet = null;
                   
         //get movies seen by this user
         movies = MTestMh.getMoviesSeenByUser(uid); 							//get movies seen by this user
         moviesSize = movies.size();   
         boolean featureFlag = true;											// TO check if movie has some features
        
       //  System.out.println("train size = " + trainInstaces.numInstances());
           System.out.println ("test movie=" + moviesSize);
         
         //For all movies seen by this user (Test Set)
         for (int j = 0; j < moviesSize/10.0; j++)
         {
        	 
	       	mid  = MemHelper.parseUserOrMovie(movies.getQuick(j));					// get mid	       	 	
	   	    //FeaturesMovie = MTestMh.getFeaturesAgainstAMovie(mid);				// to check size of features
	   	    FeaturesMovie = MTestMh.getKeywordsAgainstAMovie(mid);		
	   	    //System.out.println(FeaturesMovie);
		   	 
	   	     if(!(FeaturesMovie == null || FeaturesMovie.size() <=5))
		   	    {
		   	    	nonZeroFeatures.add(mid);
		   	    }
	   	 
         }
         
         System.out.println("test movies with features = " + nonZeroFeatures.size());
         
    if( nonZeroFeatures.size() >1)
     {
         for (int j = 0; j < nonZeroFeatures.size(); j++)
         {
        	
        	mid = nonZeroFeatures.getQuick(j);
            rating = MTestMh.getRating(uid, mid);            	   				// get rating
	       	FeaturesTestMovie_BagsOfWords = getBagsOfWords(mid, MTestMh);		// get bags of words
	       	
	        //Create learning dataset (only once -- one for each user)     
	          if(j==0)  myDataSet = getLearningDataSet(myAttributes,			 		 // created attributes 
	        		  									nonZeroFeatures.size()); 		 //  no of examples
	     
	        //System.out.println ("test movie is=" + (j) + "Feature =" + FeaturesTestMovie_BagsOfWords);
	       		
		       	myActualClasses.add(rating);
		       	
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
            
    	 //  System.out.println ("test size=" + myDataSet.numInstances());
    	 
         //--------------------------------------------------
         // Evaluate Test Set : Separate Test & Training Set
         //--------------------------------------------------
        
         
            //Apply stringToWordVector Filter (Filter should be formatted on same object)
    	    //stwv.setInputFormat(myDataSet);
            new_DataSet =  Filter.useFilter( myDataSet, stwv); 	            
            System.out.println ("new test size=" + new_DataSet.numInstances());
       /*     System.out.println ("new train data=" + trainInstaces);
            System.out.println ("new test data=" + new_DataSet);*/
            
            //Evaluate model   
    	 	Evaluation eval = new Evaluation(trainInstaces);
    	 	eval.evaluateModel(myClassifier,new_DataSet);
            System.out.println(eval.toSummaryString());           

            
            
            // -----------------------------------------
            // 10 fold cross validation on Train Set
            // -----------------------------------------
            // A classifier should not be trained on the test set
            
           /* eval.crossValidateModel(myClassifier,trainInstaces,10, trainInstaces.getRandomNumberGenerator(1));           
            System.out.println(eval.toSummaryString());            
            FastVector myPredictions = eval.predictions();
            
            for (int i = 0; i < myPredictions.size(); i++)
            {	 
            	Object OneElementOFPrediction = myPredictions.elementAt(i);         
            	System.out.println(OneElementOFPrediction);
            }
            */
            
            EvaluationUtils myEvalUtil = new EvaluationUtils();
            FastVector evalUtilPredictions = myEvalUtil.getCVPredictions (myClassifier,trainInstaces,10);
         
            for (int i = 0; i < evalUtilPredictions.size(); i++)
            {	 
            	Object OneElementOFPrediction = evalUtilPredictions.elementAt(i);         
            	System.out.println(OneElementOFPrediction);
            	
            }
            
            System.out.println("ROC Sensitivity =" + rmse.getSensitivity());
            System.out.println("ROC specificity =" + rmse.getSpecificity());
            System.out.println("MAE  =" + rmse.mae());
            
            // ------------------------------------------------------
            //For checking statistics of individual Instances : test
            // ------------------------------------------------------
            
            for (int i = 0; i < new_DataSet.numInstances(); i++) 
            {
            	 System.out.println("Is String="+ new_DataSet.classAttribute().isString());
            	 System.out.println("Is Nominal="+ new_DataSet.classAttribute().isNominal());
            	 System.out.println("Is Numeric="+ new_DataSet.classAttribute().isNumeric());
            	 System.out.println("Is="+ new_DataSet.classAttribute());
            	 System.out.println("Istance classIndex="+ new_DataSet.instance(i).classIndex());	//it is added in class =0
            	 System.out.println("Istance classvalue="+ new_DataSet.instance(i).classValue());	
            	 System.out.println("Istance value="+ new_DataSet.instance(i).value(0));
            	 System.out.println("Actual="+ myActualClasses.get(i));
            	 
            	 Instance ins=new_DataSet.instance(i);
                 double[] score=myClassifier.distributionForInstance(ins);
                 System.out.println(score[0]+"\n");
                 
                 myClassifier.distributionForInstance(new_DataSet.instance(i));
                 
            	 double pred = myClassifier.classifyInstance(new_DataSet.instance(i));           	   
            	 System.out.print("ID: " + new_DataSet.instance(i).value(0));
            	 System.out.print(", actual: " + new_DataSet.classAttribute().value((int) new_DataSet.instance(i).classValue()));
            	 System.out.println(", predicted: " + new_DataSet.classAttribute().value((int) pred));
       	        
            }
     } //end if          
          	
	         /*
	          
	         //-----------------------------------------------
	         // Evaluate the model, through a test instance
	         //-----------------------------------------------
	
	       	Instance testInstance = new Instance(2);
	       	testInstance.setDataset(trainInstaces);
	       	testInstance.setValue(0, FeaturesTestMovie_BagsOfWords);
	       	testInstance.setValue(1, rating);
	       	
	        StringToWordVector filter = new StringToWordVector();
	        	        
	       	//Classify instance
	       	double prediction =  myLearnedModel.classifyInstance(testInstance);
	       	
	       	//print error
	       	double error = Math.abs(prediction - rating); 
	       	System.out.println("uid, mid" + uid +"," + mid + "--> Error = (Actual - predicted)"+
	       						error + " =" + rating + "-" + prediction);
	       						
	       						*/ 
	 }
	
/*******************************************************************************************************/
	
	public static void main (String arg[])
	{


		   int xFactor = 80;
		   
		    String test  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\" + xFactor+ "\\sml_clusteringTestSetStoredTF.dat";
			String train  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\" + xFactor+ "\\sml_clusteringTrainSetStoredTF.dat";
			String main  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\" + xFactor+ "\\sml_storedFeaturesRatingsTF.dat";

		SVMPredictor mySVM = new SVMPredictor(train, test);
	
		try
		{
			mySVM.makeData();
		}
		
		catch(Exception E)
		{
			System.out.println("exception" + E);
			E.printStackTrace();
		}
	}
	
	
}
