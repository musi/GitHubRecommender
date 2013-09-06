package netflix.weka;

//It works fine, now I have to optimize it...delete all irrelevant code and try to understand what they mean

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


public class SVMRBFTextByAttribute 
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
    LibSVM myClassifier;
/*    String[] options =  {"-S", "0", "-k", "2",
    					 "-D", "2", "-G", "3", 
    					 "-C", "10", "W1", "1", "W2", "1", "W3", "1", "W4", "1", "W5", "1" };*/
		
       
   // weka.classifiers.functions.supportVector.RBFKernel myClassifier;
    
    //Start up RMSE count
    RMSECalculator rmse;
    
    //some other vaaible
    double  DF_THRESHOLD;
    int     totalMovWithNullFeaturesTest;
    int     totalMovWithNullFeaturesTrain;
    int     totalMovTrain;
    HashMap <String, Double> FeaturesMovie;
    
    //Memory counter
    Runtime runtime;
    
    
    //File Writers
    FileWriter myWriter[];
    String path;
    double  DF;
    String  whichPriors;
    
/**************************************************************************************************/

   /**
    * Constructor
    */
    
	public SVMRBFTextByAttribute(String path,
								 String mainObject, String trainObject, String testObject) throws Exception 
	{
	  	//Get test and train objects
		MainMh	= new MemHelper (mainObject);
		MMh		= new MemHelper (trainObject);
	    MTestMh = new MemHelper (testObject);
	    this.path = path;
	
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
		myTrainOptions[5] = "-S";		//lower case
		myTrainOptions[6] = "-M";		// min term frequency, I think two
		myTrainOptions[7] = "1";		 
		//myTrainOptions[8] = "-T";		//output word count
		  
		//String[] myTrainOptions = {"-T -I -W 1000 -N 1"};
		
		 try{
			 stwv.setOptions(myTrainOptions);
			 stwv.setIDFTransform(true);
			 //stwv.setMinTermFreq(1);
			 stwv.setOutputWordCounts(true);
		 }
	    	catch(Exception E) {
	    		E.printStackTrace();
	    		System.out.println("error");
	    	}
	    
 	    //create classifier
	    	 	         
	    	   /*   //LisbSVM
	    	      myClassifier = new weka.classifiers.functions.LibSVM();
	    	  */
	    	      
//	    	      options[0] = "-S 0";	   
//							    	      -S <int>
//	    	      							Set type of SVM (default: 0)
//							    	        0 = C-SVC
//							    	        1 = nu-SVC
//							    	        2 = one-class SVM
//							    	        3 = epsilon-SVR
//							    	        4 = nu-SVR
//							    	        
//	    	      options[1] = "-k 2";
//	    	      						   -K <int>
//							    	      	Set type of kernel function (default: 2)
//							    	        0 = linear: u'*v
//							    	        1 = polynomial: (gamma*u'*v + coef0)^degree
//							    	        2 = radial basis function: exp(-gamma*|u-v|^2)
//							    	        3 = sigmoid: tanh(gamma*u'*v + coef0)
//
	    	      
//	    	      options[2] = "-D 2";
//	    	      							-D <int>
//	    	      							Set degree in kernel function (default: 3)
//
	    	      
//	    	      options[3] = "-G 0.2";
//	    	      							-G <double>
//	    	      							Set gamma in kernel function (default: 1/k)
//	    	      
//	    	      options[4] = "-C 0.2";
//	    	      -C <double>
//	    	      Set the parameter C of C-SVC, epsilon-SVR, and nu-SVR
//	    	       (default: 1)
//
//	    	      options[5] = "-W 1";
//								    	      
//								    	      -W <double>
//								    	      Set the parameters C of class i to weight[i]*C, for C-SVC.
//								    	      E.g., for a 3-class problem, you could use "1 1 1" for equally
//								    	      weighted classes.
//								    	      (default: 1 for all classes)

//	    	      options[6] = "-Z";
//											   -Z
//											  Turns on normalization of input data (default: off)
											
/*

	    	      options[4] = "-R 0.2";
//								    	      -R <double>
//								    	      Set coef0 in kernel function (default: 0)
//								   
	    	      			  
	    	      options[6] = "-N 0.2";
//								    	      -N <double>
//								    	      Set the parameter nu of nu-SVC, one-class SVM, and nu-SVR
//								    	       (default: 0.5)
//
	    	      options[7] = "-P 0.2";
//								    	      -P <double>
//								    	      Set the epsilon in loss function of epsilon-SVR (default: 0.1)
	    	      options[8] = "-B";
//								    	      -B
//								    	      Trains a SVC model instead of a SVR one (default: SVR)

	    	     */
	    	      
	    	      //myClassifier.setOptions(options);	
	    	       
		     
	   //For MAE
	    rmse = new RMSECalculator();
		 
		//var
		DF = 0.0;
	    totalMovWithNullFeaturesTest = 0;
		totalMovWithNullFeaturesTrain =0;
		totalMovTrain =0;
		FeaturesMovie = null;
		runtime = Runtime.getRuntime();
		
	    //Files
        myWriter = new FileWriter[2];	//see file open section
	
	}
	
		
/*******************************************************************************************************/
	/**
	 * This function performs NB classification steps
	 */
	
	public void doNBSteps() throws Exception
	{
		 // For each user (in test set), make recommendations
        IntArrayList users = MMh.getListOfUsers();
        LongArrayList movies ;
        int moviesSize = 0;
        int uid, mid; 
        double rating = 0.0;
        String FeaturesTrainMovie_BagsOfWords ="";        
        double previousBestMAE =2;				//To keep track of previous best and print only the next, if best than previous
                
        //for parameter learning
        int totalIteration = 0;
        int iteration = 0;
        double C =0;							//Penalty parameter of the error term
        double G =0;							//Gamma, 		
       
   
                      
    //change parameters
     while(iteration<20)
     {  	    	   
    	 //LIB SCMS
    	 C = Math.pow(2.0, iteration-10);    	
    	 //C = iteration;
    	 iteration+=2;
    	 totalIteration =0;						//reset loop controller      	 
      	 
    	 whichPriors = "P";
    	 openFiles();
      	      	
    	while(totalIteration<=30)    	  	    	  
    	{  
    		//define feature selection to be done   
    		DF = totalIteration/10.0;    	
    		G = Math.pow(2.0, totalIteration-10);
    		totalIteration+=2;
	       
        //Create attributes
        FastVector myAttributes = getAttributes("Train");         
        
      //For all users
      //  for (int i = 0; i < users.size(); i++)        
        for (int i = 0; i < 50; i++)
        {    	
            if(i>100 && i%100==0)
        	System.out.println("currently at user =" +(i+1));
          
            uid = users.getQuick(i);          
            movies = MMh.getMoviesSeenByUser(uid); 							//get movies seen by this user
            moviesSize = movies.size();                     
                                     
            //Get priors weights of the active user
            OpenIntDoubleHashMap priors =  getActiveUserPriors(uid, 5);
            OpenIntDoubleHashMap weightedPriors =  getWeightedPriors(1, uid, 5);

    
            //LIbSVM
       	    myClassifier = new weka.classifiers.functions.LibSVM();
       	    
     	    //Options, use RBF
         /*   String[] options = new String [8];
       	 	options[0] = "-S";
	       	options[1] = "0";
	       	options[2] = "-K";
	       	options[3] = "2";    	       	    	       	
	       	options[4] = "-C";
	       	options[5] = ""+C;
	       	options[6] = "-G";
	       	options[7] = ""+G;  
	       	*/
	       	
       	    //Options
     	/*  String[] options = new String [8];
       	 	options[0] = "-S";
	       	options[1] = "0";
	       	options[2] = "-K";
	       	options[3] = "0";
	       	options[4] = "-C";
	       	options[5] = ""+C;
	       	options[6] = "-W";
	       	options[7] =     (1/priors.get(0)) + " "
					       	+ (1/priors.get(1)) + " "
					       	+ (1/priors.get(2)) + " "
					       	+ (1/priors.get(3)) + " "
					       	+ (1/priors.get(4));
			
					       	*/
       	 
       	 
	            String[] options = new String [10];
           	 	options[0] = "-S";
    	       	options[1] = "0";
    	       	options[2] = "-K";
    	       	options[3] = "2";    	       	    	       	
    	       	options[4] = "-C";
    	       	options[5] = ""+C;
    	       	options[6] = "-G";
    	       	options[7] = ""+G;  
    	       	options[8] = "-W";
	       		options[9] =      (priors.get(0)) + " "
					       	+ (priors.get(1)) + " "
					       	+ (priors.get(2)) + " "
					       	+ (priors.get(3)) + " "
					       	+ (priors.get(4));
	    
	       	//options[8] = "-Z";
	       	
	      /*String[] options = new String [8];
       	 	options[0] = "-S";
	       	options[1] = "0";
	       	options[2] = "-K";
	       	options[3] = "0";
	       	options[4] = "-C";
	       	options[5] = ""+C;
	       	options[6] = "-W";
	       	options[7] = "" + (1/weightedPriors.get(0)) + " "
					       	+ (1/weightedPriors.get(1)) + " "
					       	+ (1/weightedPriors.get(2)) + " "
					       	+ (1/weightedPriors.get(3)) + " "
					       	+ (1/weightedPriors.get(4));
			
						         	    */
       	
    /*   	String[] options = new String [8];
       	 	options[0] = "-S";
	       	options[1] = "0";
	       	options[2] = "-K";
	       	options[3] = "0";
	       	options[4] = "-C";
	       	options[5] = ""+C;
	       	options[6] = "-W";
	       	options[7] = "" + (weightedPriors.get(0)) + " "
					       	+ (weightedPriors.get(1)) + " "
					       	+ (weightedPriors.get(2)) + " "
					       	+ (weightedPriors.get(3)) + " "
					       	+ (weightedPriors.get(4));
		
					       	*/
						         	    
       /*	    
	       	String[] options = new String [6];
       	 	options[0] = "-S";
	       	options[1] = "0";
	       	options[2] = "-K";
	       	options[3] = "0";
	       	options[4] = "-C";
	       	options[5] = ""+C;  */    	
       	
          /*System.out.println("Cost="+myClassifier.getCost());
      	    System.out.println("Weight="+myClassifier.getWeights());
      	    System.out.println("svm="+myClassifier.getSVMType());
      	    System.out.println("kernel="+myClassifier.getKernelType());
      	  
      	    String opt[] = myClassifier.getOptions();            	    
    	    for(int tt=0;tt<opt.length;tt++)
    	    	System.out.println("options="+opt[tt]);
    	    
      	                  	    
      	    myClassifier.setCost(230);       	    
      	    myClassifier.setWeights("1 2 3 4 332");
      	  
      	    System.out.println("Cost="+myClassifier.getCost());   
      	    
      	    opt = myClassifier.getOptions();              	    
      	    for(int tt=0;tt<opt.length;tt++)
      	    	System.out.println("options="+opt[tt]);      	    
      	    System.exit(1);
      	    */
            
	       	myClassifier.setOptions(options);	      

            IntArrayList nonZeroFeatures = new IntArrayList();				//one feature is found
                                   
            //For all movies seen by this user
            for (int j = 0; j < moviesSize; j++)
             {
            	totalMovTrain++;            	
            	FeaturesMovie =null;
            	
            	mid  = MemHelper.parseUserOrMovie(movies.getQuick(j));		    // get mid
            	FeaturesMovie = MainMh.getFeaturesAgainstAMovie(mid);			// to check size of features
            	//FeaturesMovie = MMh.getKeywordsAgainstAMovie(mid);
         
            	if((FeaturesMovie.size() >0)){    	  
            	     nonZeroFeatures.add(mid);
            	  }
            	
            	else {
            		 totalMovWithNullFeaturesTrain++;            
            	}
            }
           
            Instances myDataSet = null;
            
                      
            for (int j = 0; j <nonZeroFeatures.size() ; j++)
            {             	
            	mid = nonZeroFeatures.getQuick(j);
            	rating = MMh.getRating(uid, mid);            	   				// get rating
            	
            	//FeaturesMovie = MMh.getKeywordsAgainstAMovie(mid);
            	FeaturesMovie = MainMh.getFeaturesAgainstAMovie(mid);
        /*    	
            	HashMap <String, Double> FeaturesMovieDummy;
            	FeaturesMovieDummy = (HashMap <String, Double>)FeaturesMovie.clone();
            	
            	//DO Feature Selection
            	FeaturesMovie = doFeatureSelectionByDFThresholding (FeaturesMovieDummy, 5, uid);*/
            	
            	//Make Bags of Words
            	FeaturesTrainMovie_BagsOfWords = getBagsOfWords(FeaturesMovie);		// get bags of words
            	
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
                	
             //  System.out.println("train size = " + myDataSet.numInstances());
            
		    if(nonZeroFeatures.size()>0)
		     {
                // Apply string To Word Vector Filter
                   Instances new_DataSet = applyStringToWordFilter(myDataSet);
                   //System.out.println("train size = " + new_DataSet.numInstances());
              
                   //----------------------------------
                   // Normalize the data here
                   //----------------------------------                  
             
                // Learn the model                
                   myClassifier.buildClassifier(new_DataSet);     
       
              	    
                // Evaluate the  model
            	   evaluatePredictiveModel(uid, 					// uid
            							myClassifier,				// learned model
            							new_DataSet);				// myTrain Instances                        
      		  }		    
        } //end of outer for        
         
         
         //if(previousBestMAE> rmse.mae() || totalIteration==30)
          {
        	 	 previousBestMAE = rmse.mae();  
		         System.out.println("G = "+G + ", C="+C);
		         System.out.println("ROC Sensitivity =" + rmse.getSensitivity());
		         System.out.println("ROC specificity =" + rmse.getSpecificity());
		         System.out.println("MAE  =" + rmse.mae());
		/*       System.out.println("totalMovWithNullFeaturesTest= "+totalMovWithNullFeaturesTest);
		         System.out.println("totalMovWithNullFeaturesTrain= "+totalMovWithNullFeaturesTrain);
		         System.out.println("totalMovTrain= "+totalMovTrain);*/
		         System.out.println("-----------------------------------------------");
        	} //end of if
         
             
          	 
             if(totalIteration ==2) {
	          	 myWriter[0].append(""+ C);
	             myWriter[0].append(",");
	             myWriter[1].append(""+ C);
	             myWriter[1].append(",");
             }
             
          	 myWriter[0].append(""+ rmse.mae());
			 myWriter[0].append(",");
			 
			 
			 myWriter[1].append(""+ rmse.getSensitivity());
			 myWriter[1].append(",");
			 
	         rmse.resetValues();
	         rmse.resetROC();
	         
     	/*
 		System.out.println( "Memory = "+
 							runtime.freeMemory() +
 							" out of a possible " +
 							runtime.totalMemory() );*/ 		
    	 }//end of inner while
    	 
	    	 myWriter[0].append("\n");
	    	 myWriter[1].append("\n");
	    	 closeFiles();
      } //end of while
       
       
       
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
	
	 public void evaluatePredictiveModel (int uid, 										// uid
			 							  Classifier myLearnedModel,					// learned classiifer 
			 							  Instances trainInstaces) throws Exception		// train dataset
	 {		
		 //create local variables
		 LongArrayList movies;
		 DoubleArrayList myActualClasses = new DoubleArrayList();
		 IntArrayList nonZeroFeatures = new IntArrayList();				//one feature is found
		 HashMap <String, Double> FeaturesMovie =null;
         String FeaturesTestMovie_BagsOfWords ="";         
         boolean  doNotPredict = false;   
         Instances myDataSet = null;
         Instances new_DataSet = null;
         double rating;
         int mid;
         int moviesSize = 0;         
                   
         //get movies seen  by this user
         movies = MTestMh.getMoviesSeenByUser(uid); 							//get movies seen by this user
         moviesSize = movies.size();   
                 
       //  System.out.println("train size = " + trainInstaces.numInstances());
       //  System.out.println ("test movie=" + moviesSize);
         
         //For all movies seen by this user (Test Set)
         for (int j = 0; j < moviesSize; j++)
         {
        	 
	       	   mid = MemHelper.parseUserOrMovie(movies.getQuick(j));			    // get mid	       	 	
	   	        FeaturesMovie = MainMh.getFeaturesAgainstAMovie(mid);				// to check size of features
	   	    //  FeaturesMovie = MTestMh.getKeywordsAgainstAMovie(mid);		
	   	    //System.out.println(FeaturesMovie);
		   	 
	   	     if(!(FeaturesMovie == null || FeaturesMovie.size() <=0))
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
        	mid = nonZeroFeatures.getQuick(j);
            rating = MTestMh.getRating(uid, mid);            	  				// get rating
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
            
    	/*    System.out.println ("test size=" + myDataSet.numInstances());
	        System.out.println ("test set=" + myDataSet);*/
    	 
         //--------------------------------------------------
         // Evaluate Test Set : Separate Test & Training Set
         //--------------------------------------------------
        
         
            //Apply stringToWordVector Filter (Filter should be formatted on same object)
             new_DataSet = Filter.useFilter( myDataSet, stwv);
         
         
	         //-----------------------------------------------
	         // Evaluate the model, through a test instance
	         //-----------------------------------------------
	
           
            double pred=0, prediction=0, act=0, actual=0, error=0;  
            
          for (int i = 0; i < new_DataSet.numInstances(); i++)
          {
	       	Instance testInstance = new_DataSet.instance(i);
	       		        	        
	       	//Classify instance 
	       	pred =  myLearnedModel.classifyInstance(testInstance);
	    	prediction = Double.parseDouble(new_DataSet.classAttribute().value((int) pred));	       	
	       	
	 	    act = testInstance.classValue();   	 	    
	 	    actual = Double.parseDouble(new_DataSet.classAttribute().value((int) act));
	 	
	 	
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
	       	error = Math.abs(prediction - actual); 
/*	       	System.out.println("uid, mid" + uid +"," + mid + "--> Error = (Actual - predicted)"+
	         					error + " =" + rating + "-" + prediction);	       	
*/	       	 //rating = actual;
	         rmse.add(actual, prediction);  
	         rmse.ROC4(actual, prediction, 5, MMh.getAverageRatingForUser(uid));	           
	    	
           } //end of if  
      } 

		
	 }
	
/*******************************************************************************************************/
/**
 * @throws Exception 
 * *****************************************************************************************************/	

   public static void main (String arg[]) throws Exception
	{

	    int xFactor = 80;
	    
	    String path = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\" + xFactor+ "\\";
	    String test  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\" + xFactor+ "\\sml_clusteringTestSetStoredTF.dat";
		String train  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\" + xFactor+ "\\sml_clusteringTrainSetStoredTF.dat";
		String main  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\" + xFactor+ "\\sml_storedFeaturesRatingsTF.dat";

		SVMRBFTextByAttribute NBT = new SVMRBFTextByAttribute(path, main, train, test);
	
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
	   
	 //---------------------------------------------
	/**
	 *  Get priors multiplied by wiehgts of active user's neighbours via CF
	 */
	   
	   public OpenIntDoubleHashMap getWeightedPriors(int whichWeight, int uid, int classes)
	   {
		   return myFilter.getPriorWeights(whichWeight, uid, classes); 
	   }
	   
	   
/******************************************************************************************************/
	   
	//---------------------------------------------
	   
	 public void openFiles()
  	 {
  		 String myPath =path + "Results\\";
  		 
  		 try{
  			 myWriter[0] = new FileWriter(myPath +"MAE_FS_RBF_" + whichPriors +".csv", true); //true, append in exisiting file
  			 myWriter[1] = new FileWriter(myPath +"ROC_FS_RBF_" + whichPriors +".csv", true); //true, append in exisiting file
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

