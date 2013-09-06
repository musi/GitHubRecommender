package netflix.weka.Classifiers;

//It works fine, now I have to optimize it...delete all irrelevant code and try to understand what they mean

// I have build SVM using user-based approach, u can build it using item-based as well
// It is very easy, but first complete this work


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

import weka.attributeSelection.PrincipalComponents;
import weka.attributeSelection.Ranker;
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
import weka.core.SelectedTag;
import weka.core.SparseInstance;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Normalize;
import weka.filters.unsupervised.attribute.Standardize;
import weka.filters.unsupervised.attribute.StringToWordVector;
import weka.filters.unsupervised.instance.NonSparseToSparse;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.bayes.NaiveBayesSimple;						//does not check underflow or so
import weka.classifiers.bayes.NaiveBayesMultinomial;
import netflix.algorithms.memorybased.memreader.FilterAndWeight;
import netflix.memreader.*;
import netflix.rmse.RMSECalculator;


public class SVMBinary 
{
	 // Recommender related object and variables
	MemHelper 		MMh;						// train set
    MemHelper 		MTestMh;					// test set	
    MemHelper 		MainMh;						// All Data, will be used to get movie features
   
    //Batch Filter
    Standardize batchFilter;
    
    //StringToWord Filter, Normalization filter
    StringToWordVector stwv;	
    Normalize nr;
    
    //FilterAndWeight
    FilterAndWeight myFilter;
    
    //Classifier
    LibSVM myClassifier;
    
    //attribute selection
     weka.attributeSelection.PrincipalComponents pc;
    
    
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
    
/**************************************************************************************************/

   /**
    * Constructor
    */
    
	public SVMBinary(	String path,
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
	    	 
	    	 //PrincipalComponents
	    	 pc = new PrincipalComponents();
	    	 
	    	 
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
		totalMovWithNullFeaturesTrain = 0;
		totalMovTrain = 0;
		FeaturesMovie = null;
		runtime = Runtime.getRuntime();
		
	    //Files
        myWriter = new FileWriter[2];	//see file open section
        
      	//Normalize filter
    	nr = new Normalize();
    	nr.setScale(1.0);
	
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
        
        int E =0; 
        int D =0;
                      
    //change parameters
     while(iteration<20)
     {  	    	   
    	 //LIB SCMS
    	 C = Math.pow(2.0, iteration-10);
    	 //C= 0.5;
    	 
    	 //C = iteration;
    	 iteration+=2;
    	 totalIteration =0;						//reset loop controller      	 
      	 
    	 openFiles();
      	      	
    	while(totalIteration<15)    	  	    	  
    	{  
    		//define feature selection to be done   
    		DF = totalIteration/10.0;    	
	        totalIteration+=2;
   
        
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
            OpenIntDoubleHashMap priors 		=  getActiveUserPriors(uid, 5);
            OpenIntDoubleHashMap norPriors 		=  getActiveUserNormalizedPriors(uid, 5);
            OpenIntDoubleHashMap weightedPriors =  getWeightedPriors(1, uid, 5);

    
            //LIbSVM
       	    myClassifier = new weka.classifiers.functions.LibSVM();   	    	
	      	
 		   //Get priors (un-normalized)		
 	   	    String[] options = new String [6];
       	 	options[0] = "-S";
 	       	options[1] = "0";
 	       	options[2] = "-K";
 	       	options[3] = "0";
 	       	options[4] = "-C";
 	       	options[5] = ""+C;
 	       	
       	    String weight = 	   priors.get(0) + " "
 	       	    				+ (priors.get(1)) + " "       	    
 	       	    				+ (priors.get(2)) + " "
 	       	    				+ (priors.get(3)) + " "
 	       	    				+ (priors.get(4));
        	    
	       	    myClassifier.setOptions(options);       	
	 	      	myClassifier.setCost(C);	      	
	 	       	myClassifier.setWeights(weight);
	 	        myClassifier.setNormalize(true);  //this is diff from others
       	    
	 	       	
	 		   //Get priors (normalized)		
/*	 	   	    String[] options = new String [6];
	       	 	options[0] = "-S";
	 	       	options[1] = "0";
	 	       	options[2] = "-K";
	 	       	options[3] = "0";
	 	       	options[4] = "-C";
	 	       	options[5] = ""+C;
	 	       	
	       	    String weight = 	   norPriors.get(0) + " "
	 	       	    				+ (norPriors.get(1)) + " "       	    
	 	       	    				+ (norPriors.get(2)) + " "
	 	       	    				+ (norPriors.get(3)) + " "
	 	       	    				+ (norPriors.get(4));
	        	    
		       	    myClassifier.setOptions(options);       	
		 	      	myClassifier.setCost(C);	      	
		 	       	myClassifier.setWeights(weight);
		 	       	myClassifier.setNormalize(true);  //this is diff from others
*/ 	    
 	   
       	 	//Priors = 1/priors     
       	/*    String[] options = new String [6];
    	 	options[0] = "-S";
	       	options[1] = "0";
	       	options[2] = "-K";
	       	options[3] = "0";
	       	options[4] = "-C";
	       	options[5] = ""+C;
	       	
    	    String weight = 	  (1/priors.get(0)) + " "
	       	    				+ (1/(priors.get(1))) + " "       	    
	       	    				+ (1/(priors.get(2))) + " "
	       	    				+ (1/(priors.get(3))) + " "
	       	    				+ (1/(priors.get(4)));
    	    
    	    myClassifier.setOptions(options);       	
	      	myClassifier.setCost(C);	      	
	       	myClassifier.setWeights(weight);
	       		       	
     	/*
	        String[] options = new String [6];
       	 	options[0] = "-S";
	       	options[1] = "0";
	       	options[2] = "-K";
	       	options[3] = "0";
	       	options[4] = "-C";
	       	options[5] = ""+C;
	       	
	       	String weight =   (1/weightedPriors.get(0)) + " "
					       	+ (1/weightedPriors.get(1)) + " "
					       	+ (1/weightedPriors.get(2)) + " "
					       	+ (1/weightedPriors.get(3)) + " "
					       	+ (1/weightedPriors.get(4));
	       	
	       	myClassifier.setOptions(options);       	
	      	myClassifier.setCost(C);	      	
	       	myClassifier.setWeights(weight);*/
	       				         	    
       	
	/*		String[] options = new String [6];
       	 	options[0] = "-S";
	       	options[1] = "0";
	       	options[2] = "-K";
	       	options[3] = "0";
	       	options[4] = "-C";
	       	options[5] = ""+C;
	       	
	       	String weight =   (weightedPriors.get(0)) + " "
					       	+ (weightedPriors.get(1)) + " "
					       	+ (weightedPriors.get(2)) + " "
					       	+ (weightedPriors.get(3)) + " "
					       	+ (weightedPriors.get(4));*/
						         	    
       
				//NO PRIORS
			/*	String[] options = new String [6];
	       	 	options[0] = "-S";
		       	options[1] = "0";
		       	options[2] = "-K";
		       	options[3] = "0";
		       	options[4] = "-C";
		       	options[5] = ""+C;

		       	myClassifier.setOptions(options);       	
		      	myClassifier.setCost(C);	*/      	
		     
	
	       	
	        /*     	System.out.println("---------------------------");
	       	System.out.println(myClassifier.getWeights());
	       	System.out.println(myClassifier.getKernelType());
	       	System.out.println(myClassifier.getCost());
	       	System.out.println(myClassifier.getSVMType());       	
	       	
	       	System.exit(1);*/
	 
	 int myLevelnTree =0;
	 double prediction =0;
	 
     //--------------------------------------
     // Test set, For each test movie do as,
    //---------------------------------------
     
     //get movies seen  by this user
     LongArrayList testMovies = MTestMh.getMoviesSeenByUser(uid); 							//get movies seen by this user     
     int testMoviesSize = testMovies.size(); 
     String FeaturesTestMovie_BagsOfWords = "";
     int testMid = 0;
     double actualRat = 0;
     Instances myTestDataSet = null;
     
  for (int t = 0; t < testMoviesSize; t++)
  {
   	 //get mid
  	 testMid = MemHelper.parseUserOrMovie(testMovies.getQuick(t));			    // get mid
    	 
  	 //Determine, if this mov has some features
   	 HashMap <String, Double> individualTestFeatures = null;   
    	
     //get features and Bags of Words        	
     FeaturesMovie = MainMh.getFeaturesAgainstAMovie(testMid); 	         	         	
     FeaturesTestMovie_BagsOfWords = "";
     FeaturesTestMovie_BagsOfWords = getBagsOfWords(FeaturesMovie);		// get bags of words
 
  
	//Get test rating;
     actualRat = MTestMh.getRating(uid, testMid);
	 		
     	
  if(FeaturesMovie!=null && FeaturesMovie.size() >0)
  {
    	
	while(true)
	{	
	     //Create attributes and get dataset (Test Set)
	     FastVector myTestAttributes = getAttributes("Test", myLevelnTree);   
	     
		//Get String equivalent of the rating (test set)
     	String myTestRating = getStringEquivalent(actualRat, myLevelnTree, 0);    
     	
	     // create data set (add instance)
         //It is fine to create it here, as we do not care about the testing rating
         //As we can get test rating (actual) via Helper object
     		myTestDataSet = getLearningDataSet("testNB",
     						myTestAttributes,			 		 // created attributes 
     						1); 		 						// no of examples
	       	
	      myTestDataSet = createDataSet(myTestDataSet,							// Instances
	          							 testMid,                				// mid										
	          							 FeaturesTestMovie_BagsOfWords, 		// features against a movie
	          							 myTestRating);
	       
	        //Create attributes
	        FastVector myAttributes = getAttributes("Train",myLevelnTree);      
	        
            IntArrayList nonZeroFeatures = new IntArrayList();				//one feature is found
                                   
            //For all movies seen by this user
            for (int j = 0; j < moviesSize; j++)
             {
            	totalMovTrain++;            	
            	FeaturesMovie =null;
            	
            	mid  = MemHelper.parseUserOrMovie(movies.getQuick(j));		    // get mid
            	
            	//determine if this mov has some features
             	HashMap <String, Double> individualFeatures = null;          	
            	
              	//get features         
            	individualFeatures = MainMh.getFeaturesAgainstAMovie(mid);  	  	       	 	
    	   	     
            	if( (individualFeatures != null) && (individualFeatures.size() >0)) {
    	   	    	 nonZeroFeatures.add(mid);
            	  }
            	
            	else {
            		 totalMovWithNullFeaturesTrain++;            
            	}
            }           
            
            Instances myDataSet = null;
            
            //--------------------------------------------
                 
            int modelHasSize =0;
            
            for (int j = 0; j <nonZeroFeatures.size() ; j++)
            {             	 
            	mid = nonZeroFeatures.getQuick(j);
            	rating = MMh.getRating(uid, mid);            	   				// get rating
            	
              	FeaturesMovie = MainMh.getFeaturesAgainstAMovie(mid);         	            	
            	FeaturesTrainMovie_BagsOfWords = "";         	     
            	
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
	            String myRating = getStringEquivalent(rating,myLevelnTree, 1);
	           
	            if(!myRating.equalsIgnoreCase("0.0"))
	            {
		               modelHasSize++;
		            	
			       // create data set (add instance)												 
			           myDataSet = createDataSet( 		myDataSet,							// Instances
			                							mid,                				// mid										
			                							FeaturesTrainMovie_BagsOfWords, 	// features against a movie
			                							myRating);				     		// rating      
			    

				       
	            }//end if           
	         
             } //end of inner for
                	
            
            
		    if(nonZeroFeatures.size()>0  && modelHasSize>0 )
		     {
                // Apply string To Word Vector Filter
                   Instances new_DataSet = applyStringToWordFilter(myDataSet);    
                   
                 //Apply stringToWordVector Filter to test set (Filter should be formatted on same object)
                   Instances new_TestDataSet = Filter.useFilter( myTestDataSet, stwv);
               
                   
                // Learn the model                
                   myClassifier.buildClassifier(new_DataSet);          
                   
                // Evaluate the  model
            	  prediction = evaluatePredictiveModel(
            			 							   uid, 						// uid
            			 						   	   myClassifier,				// learned model
            			 						   	   new_TestDataSet);				// myTrain Instances                        
      		  }
		    
		    if(prediction == 1.0 || prediction == 2.0 || prediction == 3.0 ||
		    	 prediction == 4.0 ||prediction == 5.0 )
		    			break;
		    
		    	myLevelnTree++;												//increment level
		    	
		    	if(myLevelnTree==3)
		    		break;
			
    	}//end of while true   
    }//end if (test movie has some features
  
  else
  {
	   prediction = MMh.getAverageRatingForUser(uid);
  }
	
  
  	if(prediction >5 || prediction <0)
	  	prediction = MMh.getAverageRatingForUser(uid);
	
		//Add Error
  		rmse.add(actualRat, prediction);
  		rmse.ROC4(actualRat, prediction, 5, MMh.getAverageRatingForUser(uid));
  		System.out.println("actual="+ actualRat + ", pred="+ prediction);
  		
  }//end of movie for of test set
  
		
     } //end of outer for of users
	
  
         
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
 
	//In new version, ArrayList is used instead of fastvector
	public FastVector getAttributes(String testOrTrain, int whichLevelAndNode)
	{
		
		FastVector allAttributes = new FastVector(2);	//we have total 3 attributes (uid, mid, string words, rating)
														// we discard uid, as it will be same for each user
		//mid attribute
		Attribute movieID = new Attribute("mid" );
		
		//rating Attribute		
		FastVector ratingAtt = new FastVector(2); 
		
		//top node
		if(whichLevelAndNode ==0)	
		{
			ratingAtt.addElement("45.0");				// {4,5}
			ratingAtt.addElement("123.0");				// {1,2,3}
			
		}
		
		//level 1, node = left
		else if(whichLevelAndNode ==1)
		{
			ratingAtt.addElement("4.0");				// {4}
			ratingAtt.addElement("5.0");				// {5}
		}	
		
		//level 1, node = right
		else if(whichLevelAndNode ==2)	
		{
			ratingAtt.addElement("1.0");				// {1}
			ratingAtt.addElement("23.0");				// {2,3}
		}	
		
		//level 2, node = right
		else if(whichLevelAndNode ==3)	
		{
			ratingAtt.addElement("2.0");				// {2}
			ratingAtt.addElement("3.0");				// {3}
		}	
		
		
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

	  public String	getStringEquivalent (double rating, int whichLevelAndNode, int testOrTrain) //0=test
	  {

		  String myRating ="";
		  myRating = "" + rating + "";
		 
		  
			//top node  {{1,2,3}, {4,5}}
			if(whichLevelAndNode ==0)	
			{
				if(rating == 4.0 || rating ==5.0)
					  myRating = "45.0";
				else
					  myRating = "123.0";
			}
			
			//level 1, node = left {4,5}
			else if(whichLevelAndNode ==1)
			{
				if(rating ==4.0)
					  myRating = "4.0";
				else if(rating ==5.0)
					  myRating = "5.0";
				else
					  myRating = "0.0";				//It means, we will not add this sample into traning
				
				if(testOrTrain ==0)
					  myRating = "5.0";
			}	
			
			//level 1, node = right  {1,{2,3}}
			else if(whichLevelAndNode ==2)	
			{
				if(rating ==1.0)
					  myRating = "1.0";
				else if(rating ==2.0 || rating == 3.0)
					  myRating = "23.0";
				else
					  myRating = "0.0";		
				
				if(testOrTrain ==0)
					  myRating = "1.0";
				
			}	
			
			//level 2, node = right  {2,3}
			else if(whichLevelAndNode ==3)	
			{
				if(rating ==2.0)
					  myRating = "2.0";
				else if(rating ==3.0)
					  myRating = "3.0";
				else
					  myRating = "0.0";
				
				if(testOrTrain ==0)
					  myRating = "2.0";
			}
			
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
		  
		     	myFeatures+=" ";
		     //	myFeatures +="\"";										  	   // closing string "			
		     	//myFeatures.trim();
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
		 Instances newNew_trainData = new_trainData;
		 
	/*	nr.setInputFormat(new_trainData);		 
		Instances newNew_trainData = Filter.useFilter(new_trainData, nr);
		 */
		 
		 return newNew_trainData;
		 
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
	
	 public double evaluatePredictiveModel (
			 							    int uid, 										// uid
			 							    Classifier myLearnedModel,					// learned classiifer 
			 							    Instances testInstaces) throws Exception		// train dataset
	 {
		 
            double pred=0, prediction=0;  
        
	       	Instance testInstance = testInstaces.firstInstance();
	       		        	        
	       	//Classify instance 
	       	pred =  myLearnedModel.classifyInstance(testInstance);
	    	prediction = Double.parseDouble(testInstaces.classAttribute().value((int) pred));	       	
	     	
	 	
	 	    return prediction;
	 }
	 
/*******************************************************************************************************/
/**
 * @throws Exception 
 * *****************************************************************************************************/	

   public static void main (String arg[]) throws Exception
	{

	    int xFactor = 80;
	    
	/*    String path = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\" + xFactor+ "\\";
	    String test  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\" + xFactor+ "\\sml_clusteringTestSetStoredTF.dat";
		String train  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\" + xFactor+ "\\sml_clusteringTrainSetStoredTF.dat";
		String main  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\" + xFactor+ "\\sml_storedFeaturesRatingsTF.dat";
*/

		String path   = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\SVD\\FeaturesPlay\\";
	    String test   = path + "sml_clusteringTestSetStoredTF.dat";
		String train  = path + "sml_clusteringTrainSetStoredTF.dat";
		String main   = path + "sml_storedFeaturesRatingsTF.dat";
	    
	    
	    SVMBinary NBT = new SVMBinary(path, main, train, test);
	
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
   
/******************************************************************************************************   
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
	   
	//---------------------------------------------
	   
	 public void openFiles()
  	 {
  		 String myPath =path + "Results\\";
  		 
  		 try{
  			 myWriter[0] = new FileWriter(myPath +"MAE_Binary_svm.csv", true); //true, append in existing file
  			 myWriter[1] = new FileWriter(myPath +"ROC_Binary_svm" +
  			 		".csv", true); //true, append in existing file 
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

