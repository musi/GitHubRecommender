package netflix.weka;

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
import netflix.recommender.ItemItemRecommender;
import netflix.recommender.ItemItemRecommenderWithK;
import netflix.rmse.RMSECalculator;


public class SVMIBCFFT 
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
    RMSECalculator[] rmseBig;
    
    //some other vaaible
    double  DF_THRESHOLD;
    int     totalMovWithNullFeaturesTest;
    int     totalMovWithNullFeaturesTrain;
    int     totalMovTrain;
    HashMap <String, Double> FeaturesMovie;
    
    //Item-based CF
    ItemItemRecommender myItemRec;		    	//item-based CF
    
    //Memory counter
    Runtime runtime;
    
    //File Writers
    FileWriter myWriter[];
    String  path;
    double  DF;
    double  alpha;  //parameters of switching hybrid
    double  beta;  
    double   correctlyClassified;
    double   totalClassified;
    
    
    
/**************************************************************************************************/

   /**
    * Constructor
    */
    
	public SVMIBCFFT(	String path,
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
	    
	  //Item based CF 	   
		myItemRec = new ItemItemRecommender(true, 5); 			     //Item based CF
		 
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
	    rmseBig = new RMSECalculator[20];
	    
	    for(int i=0;i<20;i++)
	    	rmseBig[i] = new RMSECalculator();
		 
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
	
    	//samples
    	correctlyClassified = 0;
    	totalClassified = 0;
    	
    	
	}
	
		
/*******************************************************************************************************/
	/**
	 * This function performs NB classification steps
	 */
	
	public void doNBSteps() throws Exception
	{
		 // For each user (in test set), make recommendations
        //IntArrayList users = MMh.getListOfUsers();
        IntArrayList users = MainMh.getListOfUsers();
        
        LongArrayList movies ;
        int moviesSize = 0;
        int uid, mid; 
        double rating = 0.0;
        String FeaturesTrainMovie_BagsOfWords ="";        
        double previousBestMAE =2;				//To keep track of previous best and print only the next, if best than previous
                
        //for parameter learning
        int totalIteration = 0;
        int iteration = 0;
        double C = 10;							//Penalty parameter of the error term
        double G =0;							//Gamma, 		
        
        double myDist[] = new double[41];
        for(int i=0;i<41;i++)
        	myDist[i]= 0;
       		   
        //change parameters
        while(iteration<=0)
        {  	    	   
       	 iteration+=1;    	 
       	 //alpha = (iteration/10.0)* 0.2;
       	 
       	 alpha =0.36;
       	 
       	 
      	 
       	 totalIteration =0;						//reset loop controller     	 
       	// openFiles();
         	      	
       	while(totalIteration<=0) 				//14, first we keep it 10 and learn alpha    	  	    	  
       	{
       		totalIteration+=2;
       		
       		//define feature selection to be done   
       		  
       		   beta = (totalIteration/10.0) * 0.5;        
       		// beta = -10;
       		   
        
        //Create attributes
        FastVector myAttributes = getAttributes("Train");         
        
      //For all users
          for (int i = 0; i < users.size(); i++)        
        //  for (int i = 0; i < 5; i++)
        {    	
            if(i>100 && i%100==0)
        	System.out.println("currently at user =" +(i+1));
          
            uid = users.getQuick(i);          
            //movies = MMh.getMoviesSeenByUser(uid); 							//get movies seen by this user
            movies = MainMh.getMoviesSeenByUser(uid);
            moviesSize = movies.size();                     
                                     
            //Get priors weights of the active user
        /*    OpenIntDoubleHashMap priors 		=  getActiveUserPriors(uid, 5);
            OpenIntDoubleHashMap norPriors 		=  getActiveUserNormalizedPriors(uid, 5);
            OpenIntDoubleHashMap weightedPriors =  getWeightedPriors(1, uid, 5);*/

    
            //LIbSVM
       	    myClassifier = new weka.classifiers.functions.LibSVM();   	    	
	      	
 		   //Get priors (un-normalized)		
 	 /*  	    String[] options = new String [6];
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
*/       	    
	 	       	
	 		   //Get priors (normalized)		
	 	   	    String[] options = new String [6];
	       	 	options[0] = "-S";
	 	       	options[1] = "0";
	 	       	options[2] = "-K";
	 	       	options[3] = "0";
	 	       	options[4] = "-C";
	 	       	options[5] = ""+C;
	 	       	
	       	  /*  String weight = 	   norPriors.get(0) + " "
	 	       	    				+ (norPriors.get(1)) + " "       	    
	 	       	    				+ (norPriors.get(2)) + " "
	 	       	    				+ (norPriors.get(3)) + " "
	 	       	    				+ (norPriors.get(4));*/
	        	    
		       	    myClassifier.setOptions(options);       	
		 	      	myClassifier.setCost(C);	      	
		 	       //	myClassifier.setWeights(weight);
		 	       	//myClassifier.setNormalize(true);  //this is diff from others
		 	       	myClassifier.setProbabilityEstimates(true);
 	   
       	 	//Priors = 1/priors     
       	/*  String[] options = new String [6];
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
	 
	 
            IntArrayList nonZeroFeatures = new IntArrayList();				//one feature is found
                
         
            
            //For all movies seen by this user
            for (int j = 0; j < moviesSize; j++)
             {
            	totalMovTrain++;            	
            	FeaturesMovie =null;
            	
            	mid  = MemHelper.parseUserOrMovie(movies.getQuick(j));		    // get mid            	
            	
            	double rat  = MainMh.getRating(uid, mid);
            	int index  = (int)(rat*4);
            	myDist[index]+=1;; 
            	System.out.println("rat="+rat+", index="+ (index));
            	
              	//get features         
             	LongArrayList individualFeatures = MainMh.getUsersWhoSawMovie(mid);  	  	       	 	
    	   	     
            	if( (individualFeatures != null) && (individualFeatures.size() >0)) {
    	   	    	 nonZeroFeatures.add(mid);
            	  }
            	
            	else {
            		 totalMovWithNullFeaturesTrain++;            
            	}
            }
        }
      	          	    
            //--------------------------------------
            for (int temp=0;temp<41;temp++)
            {
            	//System.out.println("dist["+(temp*1.0/4)+"]="+ myDist[temp]);
            	System.out.println("dist["+(temp)+"]="+ myDist[temp]);
            }
            
            
            
           /* //------------------------------------
           
            //System.out.println("out");
            Instances myDataSet = null;
            
            //--------------------------------------------
                      
            for (int j = 0; j <nonZeroFeatures.size() ; j++)
            {             	
            	mid = nonZeroFeatures.getQuick(j);
            	rating = MMh.getRating(uid, mid);            	   				// get rating
            	            	
            	rating = (int) rating;
            	//System.out.println("rat="+rating);
            	
              	LongArrayList myFeat = MainMh.getUsersWhoSawMovie(mid);         	            	
            	
              	FeaturesTrainMovie_BagsOfWords = "";    	
  
            	//Make Bags of Words
            	 FeaturesTrainMovie_BagsOfWords = getBagsOfWords(myFeat);		// get bags of words
            	
                //Create learning dataset (only once -- one for each user)          
                if(j==0)  myDataSet = getLearningDataSet("TrainNB",
                										 myAttributes,		 	  // created attributes 
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
                         
                   Instances new_red_DataSet = new_DataSet; 
                   
                // Learn the model                
                   myClassifier.buildClassifier(new_red_DataSet);               
              	    
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
		         System.out.println("totalMovWithNullFeaturesTest= "+totalMovWithNullFeaturesTest);
		         System.out.println("totalMovWithNullFeaturesTrain= "+totalMovWithNullFeaturesTrain);
		         System.out.println("totalMovTrain= "+totalMovTrain);     
		         
		         System.out.println("totalClassified= "+totalClassified);
		         System.out.println("correctlyClassfied= "+correctlyClassified);
		         
		         System.out.println("-----------------------------------------------");
		         
        	} //end of if
         
             
          	 
             if(totalIteration ==1) {
	          	 myWriter[0].append(""+ beta);
	             myWriter[0].append(",");
	             myWriter[1].append(""+ beta);
	             myWriter[1].append(",");
             }
             
             for(int writingLoop =0;writingLoop<20;writingLoop++)
             {
	          	 myWriter[0].append(""+ rmseBig[writingLoop].mae());
				 myWriter[0].append(",");
				 myWriter[1].append(""+ rmseBig[writingLoop].getSensitivity());
				 myWriter[1].append(",");
             }
             
             
             //reset values
	         rmse.resetValues();
	         rmse.resetFinalROC();         
	         
	        for(int resetLoop =0;resetLoop<20;resetLoop++)
	        {
		        rmseBig[resetLoop].resetValues();
		        rmseBig[resetLoop].resetFinalROC();
	        }
	         
     	
 		System.out.println( "Memory = "+
 							runtime.freeMemory() +
 							" out of a possible " +
 							runtime.totalMemory() );
 							*/ 		
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
	public FastVector getAttributes(String testOrTrain)
	{
		
		FastVector allAttributes = new FastVector(2);	//we have total 3 attributes (uid, mid, string words, rating)
														// we discard uid, as it will be same for each user
		//mid attribute
		Attribute movieID = new Attribute("mid" );
		
		//rating Attribute		
		FastVector ratingAtt = new FastVector(40); 
		ratingAtt.addElement("0.25");
		ratingAtt.addElement("0.5");						//rating ={1,2,3,4,5}
		ratingAtt.addElement("0.75");
		ratingAtt.addElement("1.0");
		ratingAtt.addElement("1.25");
		ratingAtt.addElement("1.5");						//rating ={1,2,3,4,5}
		ratingAtt.addElement("1.75");
		ratingAtt.addElement("2.0");
		ratingAtt.addElement("2.25");
		ratingAtt.addElement("2.5");						//rating ={1,2,3,4,5}
		ratingAtt.addElement("2.75");
		ratingAtt.addElement("3.0");
		ratingAtt.addElement("3.25");
		ratingAtt.addElement("3.5");						//rating ={1,2,3,4,5}
		ratingAtt.addElement("3.75");
		ratingAtt.addElement("4.0");
		ratingAtt.addElement("4.25");
		ratingAtt.addElement("4.5");						//rating ={1,2,3,4,5}
		ratingAtt.addElement("4.75");
		ratingAtt.addElement("5.0");
		ratingAtt.addElement("5.25");
		ratingAtt.addElement("5.5");						//rating ={1,2,3,4,5}
		ratingAtt.addElement("5.75");
		ratingAtt.addElement("6.0");
		ratingAtt.addElement("6.25");
		ratingAtt.addElement("6.5");						//rating ={1,2,3,4,5}
		ratingAtt.addElement("6.75");
		ratingAtt.addElement("7.0");
		ratingAtt.addElement("7.25");
		ratingAtt.addElement("7.5");						//rating ={1,2,3,4,5}
		ratingAtt.addElement("7.75");
		ratingAtt.addElement("8.0");
		ratingAtt.addElement("8.25");
		ratingAtt.addElement("8.5");						//rating ={1,2,3,4,5}
		ratingAtt.addElement("8.75");
		ratingAtt.addElement("9.0");		
		ratingAtt.addElement("9.25");
		ratingAtt.addElement("9.5");						//rating ={1,2,3,4,5}
		ratingAtt.addElement("9.75");
		ratingAtt.addElement("10.0");
		
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
									   String bagsOfWords,		// bags of words against a movie
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
		  
		  //System.out.println("rat="+myRating);
		  return myRating;
	  }
	 
	 
/*******************************************************************************************************/
	  
	 /**
	  * Create bags of words against a movie
	  */
				
		 public String getBagsOfWords (LongArrayList FeaturesTrainMovie)
		 {
					 
			 String myFeatures ="";
			  
			 for (int i=0;i<FeaturesTrainMovie.size();i++)
			 {
				 myFeatures += FeaturesTrainMovie.get(i);	
		     	 myFeatures +=" ";	     	     
		     	     
	     	 } //end of while
		  
		     	myFeatures+=" ";
		     //	myFeatures +="\"";										
		     	//myFeatures.trim();
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
		// Instances newNew_trainData = new_trainData;
		 
		nr.setInputFormat(new_trainData);		 
		Instances newNew_trainData = Filter.useFilter(new_trainData, nr);
		 
		 
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
	
	 public void evaluatePredictiveModel (int uid, 										// uid
			 							  Classifier myLearnedModel,					// learned classiifer 
			 							  Instances trainInstaces) throws Exception		// train dataset
	 {		
		 //create local variables
		 LongArrayList 				movies;
		 DoubleArrayList 			myActualClasses 				= new DoubleArrayList();
		 IntArrayList 				nonZeroFeatures		 			= new IntArrayList();				//one feature is found
		 HashMap <String, Double> 	FeaturesMovie 					= null;
         String 					FeaturesTestMovie_BagsOfWords 	= "";          
         Instances 					myDataSet 						= null;
         Instances 					new_DataSet 					= null;
         double 					rating							= 0;
         int 						mid								= 0;
         int 						moviesSize 						= 0;         
         double 					CFRat   						= 0;
  
         
     	//get movies seen  by this user
         movies = MTestMh.getMoviesSeenByUser(uid); 						//get movies seen by this user
         moviesSize = movies.size();   
        
         for(int j=0;j<moviesSize;j++)
         {
          // System.out.println("j="+ j+ ", moviesSize="+ moviesSize);
    
           //get mid
     	  mid = MemHelper.parseUserOrMovie(movies.getQuick(j));			    // get mid
	 	   
 /*    	 //---------------------------------------------------------------------------------
     	 double actual = MTestMh.getRating(uid, mid); 
 		 double finalPrediction =0;
 		
 		   //---------------
	 	   // CF Prediction
	 	   //---------------
	 	   
 		 finalPrediction = myItemRec.recommend(MMh, uid, mid, 10, 30);
	 	
 		 //System.out.println("pred="+ finalPrediction);
		   
 		 //add error
	         if(finalPrediction!=0)
 		 {
	        	 rmse.add(actual, finalPrediction);  
	        	 rmse.ROC4(actual, finalPrediction, 5, MMh.getAverageRatingForUser(uid));
 		 }  
              */	  
	   	 //---------------------------------------------------------------------------------
        	 
        	 //Determine, if this mov has some features
          	HashMap <String, Double> individualFeatures = null;
        	boolean movHasSomeFeatures = false;
        	
          	//get features        	
        	FeaturesMovie = MainMh.getFeaturesAgainstAMovie(mid);   		       	  	       	 	
	   	     
        	if( (FeaturesMovie != null) && (FeaturesMovie.size() >0)) 
		   	  {
		   	    	nonZeroFeatures.add(mid);
		   	  }
	   	     
	   	     else 
	   	    	totalMovWithNullFeaturesTest++;
	   	 
         }
         
      
         double pred=0, prediction=0, act=0, actual=0, error=0;  
         double finalPrediction = 0;		//final predction   
           
         for (int j = 0; j < moviesSize; j++)        
         {    
        	 //reset variables
        	 pred=0; prediction=0; act=0; actual=0; error=0;  
             finalPrediction = 0;		//final predction   
             
        	 //get mid
        	 mid = MemHelper.parseUserOrMovie(movies.getQuick(j));			    // get mid
         
        	 //---------------------------------------------------
        	 // check if this movie has some non zero features
        	 //---------------------------------------------------        	 
        	if(nonZeroFeatures.contains(mid))        	
        	{
        		        	
            rating = MTestMh.getRating(uid, mid);            	  				// get rating
            LongArrayList myFeat = MainMh.getUsersWhoSawMovie(mid);              
           
           //---------------
   	 	   // CF Prediction
   	 	   //---------------
   	 	   
   	 	   CFRat = myItemRec.recommend(MMh, uid, mid, 5, 30);
   	 	   
            //Create attributes
            FastVector myAttributes = getAttributes("Test");   	
        	FeaturesTestMovie_BagsOfWords = "";        	         	
        	
            FeaturesTestMovie_BagsOfWords = getBagsOfWords(myFeat);		// get bags of words
	       	
	        //Create learning dataset 
            myDataSet = getLearningDataSet(	"testNB",
	        			  					 myAttributes,			 		 // created attributes 
	        		  						 nonZeroFeatures.size()); 		 // no of examples
            
	     
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
		      
          // } //end of building dataSet
   
    	 
         //--------------------------------------------------
         // Evaluate Test Set : Separate Test & Training Set
         //--------------------------------------------------
        
         
            //Apply stringToWordVector Filter (Filter should be formatted on same object)
             new_DataSet = Filter.useFilter( myDataSet, stwv);
         
            //Apply Normalization Filter (Filter should be formatted on same object)
             Instances new_NorDataSet = Filter.useFilter( new_DataSet, nr);
             
            // Instances new_NorDataSet = new_DataSet;
             
             
	         //-----------------------------------------------
	         // Evaluate the model, through a test instance
	         //-----------------------------------------------
                   
             
          for (int index = 0; index < new_NorDataSet.numInstances(); index++)
          {
        		 Instance ins   = new_NorDataSet.instance(index);
                 double[] score = myClassifier.distributionForInstance(ins);
        
                             /*     for(int k=0;k<5;k++)
               	 System.out.println("score["+k+"]="+ score[k]);*/   
                 
                        
                 
	       	Instance testInstance = new_NorDataSet.instance(index);
	       		        	        
	       	//Classify instance 
	       	pred =  myLearnedModel.classifyInstance(testInstance);
	    	prediction = Double.parseDouble(new_NorDataSet.classAttribute().value((int) pred));	       	
	       	
	 	    act = testInstance.classValue();   	 	    
	 	    actual = Double.parseDouble(new_NorDataSet.classAttribute().value((int) act));
	 	
	 	//   System.out.println("prediction="+prediction);
	  	  	  
	 	    
	 	    
	 	   //-------------------------------------------------
	 	   // Switching Hybrid Algorithm using svm and CF
	 	   //-------------------------------------------------
	     	        
	        OpenIntDoubleHashMap classToProb = new OpenIntDoubleHashMap();
	 	    
	        for(int k=0;k<5;k++)
              	 {
	        		//System.out.println("score["+k+"]="+ score[k]);
	        		 classToProb.put(k+1,  score[k] );
	        		
	        		/* double tempPred= Double.parseDouble(new_NorDataSet.classAttribute().value((int) pred));	
	        	  	   classToProb.put((int)tempPred-1,  score[k] );
	        		 */
              	 }
	        
	 	    //Sort the array into ascending order
	        IntArrayList myKeys 	= classToProb.keys(); 
	        DoubleArrayList myVals 	= classToProb.values();       
	        classToProb.pairsSortedByValue(myKeys, myVals);       
	        
	        
	        //Change Beta here, it ll save a lot of resources
	        int totalIteration =0;
	        int rmseIndex =0;
			        
	        	// for each prediction of SVM and CF, change the parameter beta
	        	// and store the results.
	        
			    	//while(totalIteration<20) 				//14, first we keep it 10 and learn alpha    	  	    	  
			       	{
				       		totalIteration+=1;
				       		
				       		//define feature selection to be done   
				       		  
				       		 //  beta = (totalIteration/10.0) * 0.5;  
				       	
				       		beta = 0.7;
				       		
				    	
				        //finalPrediction = prediction;				       		
				        finalPrediction = (prediction + CFRat)/2.0;
				        
				        //finalPrediction = switchingLogic(CFRat, prediction, classToProb, myKeys, myVals);
				          
				        if(finalPrediction==actual)
				        	    correctlyClassified++;
				        	
				        totalClassified++;
				        
				       // finalPrediction = prediction;
				        
				       /* if(finalPrediction ==0)
					 		   finalPrediction = MMh.getAverageRatingForUser(uid);
					 	*/   
					 	   	 	   
					 	   //add error
				        if(finalPrediction!=0)
				        {
				        	 rmseBig[rmseIndex].add(actual, finalPrediction);
				        	 rmseBig[rmseIndex].ROC4(actual, finalPrediction, 5, MMh.getAverageRatingForUser(uid));
				        	 rmseIndex++;
				        	 
					         rmse.add(actual, finalPrediction);  
					         rmse.ROC4(actual, finalPrediction, 5, MMh.getAverageRatingForUser(uid));
					  
				        }
			        
			       	} //end beta
			    	
              } //end of inner for (one instance)  
        	}//end if
        	
        	//Do CF prediction, and Add user average if not available
    /*    	else
        	{
        		   actual = MTestMh.getRating(uid, mid); 
        		 
                    //---------------
         	 	   // CF Prediction
         	 	   //---------------
         	 	   
         	 	   CFRat = myItemRec.recommend(MMh, uid, mid, 5, 30);
         	 	   
        		 if(finalPrediction ==0)
  		 		   finalPrediction = MMh.getAverageRatingForUser(uid);
        		 
        		  //add error
        		if(finalPrediction!=0)
     	        {     		    
        		 rmse.add(actual, finalPrediction);  
		         rmse.ROC4(actual, finalPrediction, 5, MMh.getAverageRatingForUser(uid));
     	        } 
        	}*/
        	
        	
        } //end of outer for (for all movies)   


         for (int i=0;i<=0;i++)
         {
        	//add roc for one user and reset
             rmseBig[i].addROCForOneUser();
             rmseBig[i].resetROCForEachUser();
               	 
         }
         
      //add roc for one user and reset
      rmse.addROCForOneUser();
      rmse.resetROCForEachUser();
      
		
	 }
	 
/*****************************************************************************************************/
	 
	 public double switchingLogic(  double CFRat, 
			 					    double classifierPred,
			 						OpenIntDoubleHashMap classToProb, 
			 						IntArrayList myKeys,
			 						DoubleArrayList myVals)
	 {
		    //-----------------
	        // final tie cases
	        //-----------------
	        
	         double min 			= 0.5;
	 		 int   category 		= 0;
	 		 int   classes 			= 5;
	 		 double finalPrediction = 0;
	 		 	 		 
	 		 boolean finalTieFlag = false;
	 		 IntArrayList  finalTieCases = new IntArrayList();
	 		 	
	 	/*	 for(int k=0;k<5;k++)
	 			 System.out.print(myKeys.get(k)+", ");
	 		System.out.println();
	
	 		for(int k=0;k<5;k++)
	 			 System.out.print(myVals.get(k)+ ", ");	 		
	 		System.out.println();*/
	 		
	 		for(int i=classes-1;i>=0;i--)
	 	      {
	 	    	   if(i>0)
	 	    	   {
	 	    		   if(myVals.get(classes-1) == myVals.get(i-1)) 
	 	    		   {
	 	    			 finalTieCases.add((i-1));		//Just add index
	 	    			 finalTieFlag =true;
	 	    		   }	 
	 	    		   
	 	    		   else
	 	    			   break;	    		   
	 	    	   }
	 	       } //end of for 


	 	      //First step
		 		if(CFRat ==0)
		 			 return classifierPred;
		 		 
	  		//-------------------------------------
	 		// Learn some confidence function
	      	// [L(0) - L(1)] and [CF - L(0)] 
	 		//-------------------------------------
	      
	      	//Max value 
	      	double L0 = myVals.getQuick(classes-1);
	      	double ans = L0;
	      	
	      	// If we are confident abt NB prediction which have no tie as well
	      	if(finalTieFlag ==false)
	      	{     		      			     		
	 	     		 double diff = ((myVals.getQuick(classes-1))) - myVals.getQuick(classes-2);	     		 
	 	     		 diff = Math.abs(diff);
	 	     		 
	 	     		 //if(diff>thresholdProb)
	 	     		  if(diff> alpha)
	 	     		  {
	 	     			    //System.out.println((myVals.getQuick(classes-1))  + "," + (- myVals.getQuick(classes-2)) + "="+ + diff); 			    
	 	     			 	ans = (double)myKeys.getQuick(classes-1);  
	 	     			 	return ans;
	 	     		  }     		  
	      	  }
	      		      	     	 
	      	
	      	int totalTies=0;
	      	
	      	 //for all tie cases, see the diff of two predictors
	      	for (int j=classes-1; j>0; j--)
	      	{
	      		ans  = (double)myKeys.getQuick(j);     		
	 	
	      	  if(Math.abs(ans - CFRat) < beta)
	 	 			return ans;
	      	 
		      if(totalTies++ >= finalTieCases.size())
		      	    break;
	      	}
	 	   
	 	    	   
		 return CFRat;		 
	 }
	
	 
/*******************************************************************************************************/
/**
 * @throws Exception 
 * *****************************************************************************************************/	

   public static void main (String arg[]) throws Exception
	{

	    int xFactor = 80;
	    
	/*  String path = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\" + xFactor+ "\\";
	    String test  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\" + xFactor+ "\\sml_clusteringTestSetStoredTF.dat";
		String train  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\" + xFactor+ "\\sml_clusteringTrainSetStoredTF.dat";
		String main  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\" + xFactor+ "\\sml_storedFeaturesRatingsTF.dat";
*/
	    
	    //testing
		String path   = "I:/Backup main data march 2010/workspace/MusiRecommender/DataSets/FT/Itembased/FiveFoldData/";
	    String test   = path + "ft_testSetStoredBothFold11.dat";
		String train  = path + "ft_trainSetStoredBothFold11.dat";
		String main   = path + "ft_myNorStoredRatingsBoth1.dat";
	    
		
	    //validations
	/*	String path   = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\" + xFactor+ "\\";
	    String test   = path + "sml_clusteringTrainingValidationSetStoredTF.dat";
		String train  = path + "sml_clusteringTrainingTrainSetStoredTF.dat";
		String main   = path + "sml_storedFeaturesRatingsTF.dat";*/
		
	    
	    SVMIBCFFT NBT = new SVMIBCFFT(path, main, train, test);
	
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
  			 myWriter[0] = new FileWriter(myPath +"MAE_nor_svmCF.csv", true); //true, append in existing file
  			 myWriter[1] = new FileWriter(myPath +"ROC_nor_svmCF" +
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

