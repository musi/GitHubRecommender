package netflix.recommender;

// I think, PCC is good for making TOPN list etc....so my next task should be to make Top N list for recommendation

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import cern.colt.list.LongArrayList;
import cern.colt.list.ObjectArrayList;
import cern.colt.map.OpenIntDoubleHashMap;
import netflix.algorithms.memorybased.memreader.FilterAndWeight;
import netflix.memreader.MemReader;
import netflix.memreader.MemHelper;
import netflix.rmse.RMSECalculator;

public class NaiveBayesRecHybridGoodBad 
{

    /** Flag to set Laplace smoothing when estimating probabilities */
     boolean isLaplace = false;

    /** Flag to set Laplace smoothing when estimating probabilities */
    boolean isLog =   true;			//It is important for overcoming the UNDERFLOW (Nearlly at E-300 -->0)
    
    /** Small value to be used instead of 0 in probabilities, if Laplace smoothing is not used */    
    double EPSILON = 1e-6; 

    /** Small value to be used, if a class got no common features */    
    double NEG_EPSILON = -1e6; 

    /** Flag to debug */
    boolean isDebug   = false;

    /** Small pseudo count value to be used instead of 0 in probabilities, if Laplace smoothing is not used */    
    double pCount = 1;  
    
    /** Threshold for DF Thresholding, and X2 */
    int DF_THRESHOLD 	 = 30;
    double X2_THRESHOLD  = 30;
    
    /** Name of classifier */
    public static final String name = "NaiveBayes";
    
    /** Number of categories */
    int numCategories; 

    /** Number of features */
    int numFeatures;

    /** Number of training examples, set by train function */
    int numExamples;

    /** Flag for debug prints */
    boolean debug = false;
    
    /** Flag which is true, when any one of the feature set (test ot train) is zero */
    boolean doNotIncludeThisMovie = false;
    
    int total_doNotIncludeThisMovie = 0; 
    int total_NullValuesAfterThreshold = 0;
    
      
    // Recommender related object and varaibes
	MemHelper 		MMh;						//train set
    MemHelper 		MTestMh;					// test set	
    
    //Filtr and Weight
    FilterAndWeight myFilter;
    
    //Start up RMSE count
    RMSECalculator rmse;

    //rand object
    Random rand;
	
    //------------------------------
    // General NB
    //-----------------------------
    
    //Classes
    public int myClasses = 5;
    
    //Extreme errors & prediction answers
    int extremeError5;
    int extremeError4;
    int extremeError3;
    int extremeError2;
    int extremeError1;
    int correctlyPredicted;
    int totalPredicted;

    // HashMap for all features
    HashMap <String, Double> []  AllFeaturesInASlot;
    HashMap <String, Double> 	 AllFeaturesForEvidence;
    HashMap <String, Double> []  AllBinaryFeaturesInASlot;
    HashMap <String, Double>  	 AllBinaryFeaturesForEvidence;
    double priors[] 		 = new double [myClasses];
    
    //types to be extracted 
	IntArrayList typeToExtract;
	
    //Prior*likelihood = psedu count
    int totalResultsUnAnswered;
    int totalZeroPriors;
    int totalZeroLikelihood;
    int totalCasesWhereProbIsConstant;			// i.e. posterior = constant
    int totalTieCases;  						//Tie Cases
    

    double  evidence;			 				// evidence
    double  sum;					    		// sum of prob
    int 	nullTestFeatures ;					// null features
    boolean currentMovieHasNullFeatures;
    boolean isProbIsConstant;					// for all classes, prob = constant means (no common feature for any class)
    
    //No Commonality between test and train set
    boolean noCommonFeatureFound;
    int 	noCommonality;
    
    //------------------------------------
    //For Binary Confidence and Posterior
    //------------------------------------
    
    double  prob_Good;
    double  prob_Bad;					    			//binary confidence		
    int     goodAndBadEqual;							// cases for equal probx (good, bad)
    int     goodAndBadUnequal;
    boolean noCommonBinaryFeatureFound_Good;				
    boolean noCommonBinaryFeatureFound_Bad;				
    int 	noBinaryCommonality;						
    double  binaryEvidence;
    int 	totalZeroBinaryPriors;
    int 	totalZeroBinaryLikelihood;
    int 	commonFeatureFound_Good;				// common features for the current movie
    int 	commonFeatureFound_Bad;
    int 	noCommonBinaryFeatureFound;				// Cases, where both classes have zero zero features with test set		
    double  currentLikelihood_Good;					// To check why binary probs are coming equal
    double  currentLikelihood_Bad;
    
    
/**********************************************************************************************/	
/**
 * Constructor
 * @param train Object
 * @param test Object
 */
    
    public  NaiveBayesRecHybridGoodBad (String trainObject, String testObject)
    {    
    	//Get test and train objects
	    MMh			= new MemHelper (trainObject);
	    MTestMh 	= new MemHelper (testObject);
	
	    //FilterAndWeight, Random object, For MAE
	    myFilter = new FilterAndWeight(MMh,1);
	    rand = new Random();
	    rmse = new RMSECalculator();
		
		//assign how much classes, we want
	    myClasses = 5; 						// {5,10} for{ML,FT}
	    
	    // Features against a class
	      AllFeaturesInASlot  = (HashMap<String, Double>[])Array.newInstance(HashMap.class, myClasses);  	   
		   for (int i=0;i<myClasses;i++)
		   {
			 AllFeaturesInASlot[i] = new  HashMap <String, Double>();
		   }

		// Features for evidence
		  AllFeaturesForEvidence = new  HashMap <String, Double>();
	           
	    //Just to check for how many cases, it is unsuccessful extremely
	    extremeError1 = 0;
	    extremeError2 = 0;
	    extremeError3 = 0;
	    extremeError4 = 0;
	    extremeError5 = 0;
	
	    //correct answer 
	    correctlyPredicted  =  totalPredicted = 0;

	    //evidence
	    evidence 		   = 0;
	    
	    // For checking how much tie cases occured
	    totalTieCases = 0;
	    
	    // null features
	     nullTestFeatures =0;
	     currentMovieHasNullFeatures =false;
	     isProbIsConstant = true;						//It will become false, if not	    
	     totalCasesWhereProbIsConstant =0;				// cases, where all classes have same prob
	     
	     //--------------------------------------
	     //BINARY
	     //--------------------------------------	    
	     //Training features
	       AllBinaryFeaturesInASlot  = (HashMap<String, Double>[])Array.newInstance(HashMap.class, 2);  	   
		   for (int i=0;i<2;i++)
		   {
			  AllBinaryFeaturesInASlot[i] = new  HashMap <String, Double>();
		   }
	
		   // Features for evidence
		   AllBinaryFeaturesForEvidence = new  HashMap <String, Double>();
				   
	    //No Commonality between test and train set
	    noCommonFeatureFound 				= false;
	    noCommonBinaryFeatureFound_Good 	= false;
	    noCommonBinaryFeatureFound_Bad 		= false;
	    noCommonality		 				= 0;	
	    noBinaryCommonality 				= 0;
	    
	    //Count how much of the prior*likelihood are not contributing anything
	    totalResultsUnAnswered 		= 0;
	    totalZeroPriors 			= 0;
	    totalZeroLikelihood 		= 0;
	    totalZeroBinaryPriors 		= 0;
	    totalZeroBinaryLikelihood 	= 0;
	
	    //evidence
	    binaryEvidence				= 0;
	
	   //sum of prob
	    sum = 0;
	    
	   // Binary confidence
	    prob_Good = 0;
	    prob_Bad  =	0;
	    
	    //check in how many instances binary confidence are equal or unequal
	    goodAndBadEqual   = 0;
	    goodAndBadUnequal = 0;
	    
	    //type
		typeToExtract = new IntArrayList();
//	    typeToExtract.add(0);
		
		typeToExtract.add(10);
		typeToExtract.add(9);
		typeToExtract.add(15);
		typeToExtract.add(16);
		typeToExtract.add(17);
		typeToExtract.add(18);
		typeToExtract.add(98);

/*		typeToExtract.add(4);
		typeToExtract.add(2);
		typeToExtract.add(100);
		typeToExtract.add(101);
		typeToExtract.add(94);
		typeToExtract.add(19);
*/	//	typeToExtract.add(5);	
	  
		
    }

/*****************************************************************************************************/
       
            
             /** Estimates the "PRIOR PROBS" for different categories
             *
             *   @param user id, and classes {1,2,3,4,5} or {1,2,3,4,5,6,7,8,910}
             */
            
            public double[] getPrior(int uid, int classes)
            {
            	
                LongArrayList movies;
                double rating   	= 0.0;
                int moviesSize  	= 0;              
                int mid         	= 0;
                
                //Init the priors
                for (int i=0;i<classes;i++)
                	priors[i] =0;
                
        		//Get all movies seen by this user from the training set               
                movies = MMh.getMoviesSeenByUser(uid); 
                moviesSize = movies.size();
                //--------------------------------------------------------------------
                //Imp: (Index -->Class) -----> (0-->1), (1-->2)
                // Mens prior(0) contains class 1 information
                //--------------------------------------------------------------------
                
                //Calculate the probability that this movie will be in the given class                
                for (int i=0;i<moviesSize;i++)
                {                	
                	   mid 	 	 = MemHelper.parseUserOrMovie(movies.getQuick(i));
                	   rating 	 = MMh.getRating(uid, mid);
                	   int index = (int) rating;
                	   
                	  //Find counts for each class
                		priors[index-1]++;
                 	         	
                }
                
                //Count the probabilities for each class                
                for(int i=0;i<classes;i++)
                {
                	//Perform Laplace smoothing
                	if(isLaplace)
                	{
                		double num = priors[i] + (1.0)/moviesSize;
                		double den = moviesSize + ((classes*1.0)/moviesSize);
                		priors[i]  = num/den;
                	}
                	
                	else if(isLog)
                	{
                		    double num = priors[i] + (1.0)/moviesSize;
                    		double den = moviesSize + ((classes*1.0)/moviesSize);
                    		priors[i]  = Math.log10(num/den);                    	
                	}
               	
                	//Add Psudo count
                	else
                	{
	                	if(priors[i]==0) 
	                		priors[i] = priors[i] + EPSILON;	                	
	                  		priors[i] = Math.log10(priors[i]/moviesSize);
                		
	                  		//priors[i] = (priors[i] + pCount)/(moviesSize *1.0);
                	}
                }
                
                //Do some laplace smoothing or pseudo count here
                
                //return priors
                return priors;                    
        		
            }
   
/*****************************************************************************************************/
                  
           /** Estimates the "PRIOR PROBS" for different categories
            *	good = {4,5}
            *	bad  = {1,2,3}
            *
            *   @param user id, and classes {good}, {bad}, i.e. classes=2
            *   
            */
           
           public double[] getBinaryPrior(int uid, int classes)
           {
           	
               LongArrayList movies;
               double rating   	= 0.0;
               int moviesSize  	= 0;
               double priors[] 	= new double [classes];	//binary
               int mid         	= 0;
               
               //Init the priors
               for (int i=0;i<classes;i++)
               	priors[i] =0;
               
       		  //Get all movies seen by this user from the training set               
               movies = MMh.getMoviesSeenByUser(uid); 
               moviesSize = movies.size();
               
               //Calculate the probability that this movie will be in the given class                
               for (int i=0;i<moviesSize;i++)
               {                	
               	   mid 	 	 = MemHelper.parseUserOrMovie(movies.getQuick(i));
               	   rating 	 = MMh.getRating(uid, mid);
               	   int index = (int) rating;
               	  
               	//Find counts for each class
               	   if(index <=3) priors[0]++;			//Bad 
               	   else priors[1]++;					//Good
               	        		                	         	
               }
               
               //Count the probabilities for each class                
               for(int i=0;i<2;i++)
               {
	               	//Perform Laplace smoothing
	            	if(isLaplace)
	               	{
	               		double num = priors[i] + (1.0)/moviesSize;
	               		double den = moviesSize + ((classes*1.0)/moviesSize);
	               		priors[i]  = num/den;
	               	}
	               	
	               	else if(isLog)
	               	{
	               		    double num = priors[i] + (1.0)/moviesSize;
	                   		double den = moviesSize + ((classes*1.0)/moviesSize);
	                   		priors[i]  = Math.log10(num/den);
	                   	
	               	}
	              	
	               	//Add Psudo count
	               	else
	               	{
		                	if(priors[i]==0) 
		                		priors[i] = priors[i] + EPSILON;	                	
		                  		priors[i] = priors[i]/moviesSize;
	               		
	               		//priors[i] = (priors[i] + pCount)/(moviesSize *1.0);
	               	}
               }
               
               //Do some laplce smoothing or pseudo count here
               
               //return priors
               return priors;                    
       		
           }
  
/*****************************************************************************************************/

      /**
       *  Return the likelihood for a all classes for a user 
       *  @param uid, the user id for which we want to get the likelihood
       *  @param mid, the mid which we want to predict (in test set)
       *  @param classes, how much classes we have (5 for ML, 10 for FT)
       */       
   
            public double[] getLikelihood (int uid, int mid, int classes)
            {
           	    // Features stored in the database    	 
           	    HashMap<String,Double> FeaturesTestMovie  = null; 
                       	    
          	     //Local variables
                LongArrayList movies;
                int    moviesSize 				= 0;               
                double likelihood[] 			= new double [classes];
                double likelihoodIndividual[] 	= new double [classes];
                double likelihoodNum[] 			= new double [classes];
                double likelihoodDen[] 			= new double [classes];
                
                //Initialise the likelihoods
                 for(int i=0;i<classes; i++)
                 {
               	  likelihood [i] 			= 0.0;			
               	  likelihoodIndividual [i] 	= 0.0;
               	  likelihoodNum [i] 		= 0.0;				//should be 0
               	  likelihoodDen [i] 		= 0.0;               	  
                 }     
                 
                 int sizesOfTestInASlot[] = new int [typeToExtract.size()];
                 int commonFeaturesLoopIndex = 0;
                 
               //-----------------------------------------------------
        	   //Get all movies seen by this user from the training set               
               //-----------------------------------------------------
                
                movies = MMh.getMoviesSeenByUser(uid); 
                moviesSize = movies.size();
             	   
              //For each slot, we have to get all distinct words, and their count
              for (int t =0;t<typeToExtract.size();t++)
           	  {
               	    int sizeTestMovie  = 0;
       	           	   
               	    //get a type
           		    int type = typeToExtract.get(t);    		 
           		   
           		    //---------------------------------- 
       	    		//Get a test feature for this movie
          		    //----------------------------------           		   
     	        	  // Get test features
           		     FeaturesTestMovie = getFeaturesAgainstASlot("Test", type, mid);   
           		   
                      if (FeaturesTestMovie !=null) 
                    	  sizesOfTestInASlot [t] = sizeTestMovie = FeaturesTestMovie.size();              							   	
                      else 
                    	  sizesOfTestInASlot [t] = sizeTestMovie = 0;                 	   
       	         
       	        	   if(isDebug && sizeTestMovie ==0)
       	        	   {
       	        		   System.out.println(" feature test size for type= " + type + ". and movie =" + mid + " is -->"+ sizeTestMovie);
       	        	   }
       	        	   
       	        	 //------------------------------------- 
          	    	 //Get training features against a slot 
       	        	 //-------------------------------------       	        	   
       	        	  // Get training features
	       	        	 GetTrainingSetFeatures (uid,myClasses, type);	  
	       	     	        	 

       	    //---------------------------------------------------------        
            // Get the common keywords, for each class in a certain slot
            // in the training set with the test set
            //---------------------------------------------------------
                	 
       	        double vocSize =0;         	 		//set of all distinct words in a slot       	        
       	        vocSize = getVocSize(classes);
                
       	        // Find movies, we can not predict with this data set
       	   /*     if(vocSize == 0 || sizeTestMovie ==0)
       	        {
       	         doNotIncludeThisMovie = true;
         		 total_doNotIncludeThisMovie++;
       	        }
       	        	*/
              // For all classes  	
              for (int m =0;m<classes;m++)
              {
            	//Get frequency count for the feature (All distinct words in that a slot, and in a class)
              	 double w2 = AllFeaturesInASlot[m].size();
              
              	 //Find words count in the vector
              	  double denomenatorSize = findSizeOfVector(AllFeaturesInASlot[m]);
       			
              	  if(isDebug) {
	              	  System.out.println(" test size=" + sizeTestMovie + ", all feature size="+ AllFeaturesInASlot[m].size());
	              	  System.out.println(" test set=" + FeaturesTestMovie + ",\n all feature set="+ AllFeaturesInASlot[m]);
              	  }
              	  
                 if(sizeTestMovie!=0 && AllFeaturesInASlot[m].size()!=0)
                  {  
                     //Get entry sets for both vectors (test movie and train class)
                   	  Set setTestMovie 	   = FeaturesTestMovie.entrySet();      	  
                   	  Iterator jTestMovie  = setTestMovie.iterator(); 
                     	  
                   	  commonFeaturesLoopIndex =0;
                   	  
                   	  //Iterate over the words of Test set until one of them finishes
       	              	while(jTestMovie.hasNext()) 
       	              	 {
       	              	     Map.Entry words = (Map.Entry)jTestMovie.next();         // Next 		 
       	              	     String word 	 = (String)words.getKey();			     // Get a word from the train class
       	
       	              	     //----------------------------------
       	              	     // Find Evidence
         	              	 //----------------------------------       	              	     	
       	              	                       	     
       	              	     //If the Train set contain that word
       	              	    if(AllFeaturesInASlot[m].containsKey(word))
       	              	    {	
       	              	    		commonFeaturesLoopIndex++;
       	              	   
	       	              	    	//-----------------
	       	              	    	// TF of test movie
	 	              	    		//----------------- 	              	    	
	 	              	    		double testWord_TF =  FeaturesTestMovie.get(word); 	              	    
       	              	    		
       	              	    		//-----------------
       	              	    		// Add Numerator
       	              	    		//-----------------
       	              	    	
       	                	 		//Get frequency count for the feature
       	                			 double  w1 = AllFeaturesInASlot[m].get(word);
       	                			 
       	                			 //Add it in the respective class Numerator 
       	                			 Double N =	w1;     			 	           			 
       			              	    	              	    
       			              	    //-----------------
       		          	    		// Add Denomenator
       		          	    		//-----------------
       		          	    	 
       			              	     //Add it in the respective class Numerator 
       			              	        double D = denomenatorSize;      	                			 
       	                		   //   double D = w2;
       			              	    
       			              	            			              	     
       			              	     //-------------------------------------------------------
       			          	    	 // Get likelihood for a word in a slot in a certain class
       			              	     //-------------------------------------------------------
       			          	    	 
       			              	     //Multiply each words likelihood for each slot into that class likelihood       			              	     
       			              	    if(isLaplace)
       			              	    {
       				              	     likelihoodNum[m]= N + (1.0/moviesSize);
       				              	     likelihoodDen[m]= D + (vocSize /moviesSize);	              
	   				              	    				              	     
       				              	     
       					              	 if(commonFeaturesLoopIndex==1)                
       					              		 	 likelihoodIndividual[m] = testWord_TF *(likelihoodNum[m]/likelihoodDen[m]);
	       					              		       					              		 	       					              	    	
       					              	 else {
       					              		  		likelihoodIndividual[m] =  testWord_TF * (likelihoodIndividual[m] * (likelihoodNum[m]/likelihoodDen[m]));
       					              		  	//	evidence *= (likelihoodNum[m]/likelihoodDen[m]); 
       					              		   }
       					            
       					              	 // Check the words
       					              //	 if(debug)
       					              	 
       					              	 {
	       					       	/*		System.out.println("Likelihood="+likelihoodIndividual[m]);
	   				              			System.out.println("N="+N);
	   				              			System.out.println("D="+D);
	   				              			System.out.println("word="+word);
	   				              			System.out.println("vector="+AllFeaturesInASlot[m]);
	   				              			System.out.println("LikelihoodNum="+likelihoodNum[m]);
	   				              		    System.out.println("LikelihoodDen="+likelihoodDen[m]);
	   				              		    System.out.println("Voc="+vocSize);
	   				              		    System.out.println("movie size="+moviesSize);
	   				              		    System.out.println("------------------------------");*/
	   				              		    if(likelihoodIndividual[m] ==0) System.exit(1);
       					              	 }
       			              	    }
       			          
       			              	    else if(isLog)
	   			              	    {
	   				              	     likelihoodNum[m]= N + (1.0/moviesSize);
	   				              	     likelihoodDen[m]= D + (vocSize /moviesSize);              	
	   				              	     
	   				              	if(commonFeaturesLoopIndex==1)	   				              	    
	   				              			likelihoodIndividual[m] = (testWord_TF * (Math.log10(likelihoodNum[m]/likelihoodDen[m])));
	   				              	else
	   				              			likelihoodIndividual[m] += (testWord_TF * (Math.log10(likelihoodNum[m]/likelihoodDen[m])));
	   				              	
	   				              	     //For debugging	
	   				              	if(isDebug)
	   				              	     {
	   				              			System.out.println("Likelihood="+likelihoodIndividual[m]);
	   				              			System.out.println("N="+N);
	   				              			System.out.println("D="+D);
	   				              			System.out.println("word="+word);
	   				              			System.out.println("vector="+AllFeaturesInASlot[m]);
	   				              			System.out.println("LikelihoodNum="+likelihoodNum[m]);
	   				              		    System.out.println("LikelihoodDen="+likelihoodDen[m]);
	   				              		    System.out.println("Voc="+vocSize);
	   				              		    System.out.println("movie size="+moviesSize);
	   				              		    System.out.println("------------------------------");	   				              		    
	   				              	      }  	   				              	     
	   			              	    }
   			              	    
   			               	     else
       			              	    {
       			              	    	 likelihoodNum[m]  = N;
       				              	     likelihoodDen[m]  = D;
       				              	   
       				              	    if(commonFeaturesLoopIndex==1) //FIRST TIME (ASSIGN) 
       				              	     {
       				              	  /*     if(likelihoodNum[m] !=0 &&  likelihoodDen[m] !=0)	 
       				              	    	     likelihoodIndividual[m] = likelihoodNum[m]/likelihoodDen[m];
       				              	       else likelihoodIndividual[m] +=EPSILON;
       				              	  */ 
       				              	    	
       				              	    	likelihoodIndividual[m] = testWord_TF * (Math.log10((likelihoodNum[m] + EPSILON+2)/(likelihoodDen[m] + EPSILON +4)));	
       				              	     }
       				              	  
       				              	    else //NEXT TIME (MULTIPLY)		
       				              		   likelihoodIndividual[m] += testWord_TF * (Math.log10((likelihoodNum[m] + EPSILON+2)/(likelihoodDen[m] + EPSILON +4)));
       			              	    }
       			              	             	   
       	              	    } //common words           			 
       	              	 } //end of while
       	              	 
       	              	//------------------------------------------------------
       	              	// No common word is found between test and train docs
       	              	//------------------------------------------------------
       	              	// Then this likelihood[m]] will be given the highest probability, as 
       	              	// we are assigning it a constant, which is not fair,.... at the end this
       	              	// class likelihood may win.
       	              	
       	          /*    	if(commonFeaturesLoopIndex==0)
       	              	{	       
       	              		if(isLaplace)
       	              		{       		              		
       	              		     likelihoodNum[m]=  (1.0/moviesSize);
       		              		 likelihoodDen[m]=  (vocSize /moviesSize); //vocSize!=0 		         	    	
       		              		 likelihoodIndividual[m] = (likelihoodNum[m]/likelihoodDen[m]);
       		         	    	 //evidence = (likelihoodNum[m]/likelihoodDen[m]);
       		         	    	        		         	    	 
       	              		}
       	              		
       	              		else if(isLog)
	   	              		{	   		              		 
	   		              		 //Just assign it very small probability
	   		              	       likelihoodIndividual[m] = NEG_EPSILON;
	   		         	    	        		         	    	 
       	              		     likelihoodNum[m]=  (1.0/moviesSize);
	       	              		 likelihoodDen[m]=  (vocSize /moviesSize); //vocSize!=0 		         	    	
	   		              		 likelihoodIndividual[m] = Math.log10(likelihoodNum[m]/likelihoodDen[m]);
	   	              		}
       	              		else
       	              		{              	    	
       	              	         likelihoodIndividual[m] =NEG_EPSILON;
       	              	        //likelihoodIndividual[m] = (likelihoodNum[m] + pCount)/(likelihoodDen[m] + pCount +4);
       	              		
       	              		}
       	              	}
       	              	*/
       	              	if(commonFeaturesLoopIndex<1)
       	              	{
       	              		noCommonFeatureFound = true; 
       	              		noCommonality ++;
       	              	}
       	              	
                     } //end of if size >0
                 
	              	//------------------------------------------------------------------
                    // One of the doc (test or train) or both of them have zero sizes. 
	              	//------------------------------------------------------------------
                 
              /*   	else	//overcome the zero probabilities 
       		          {	                 		                 		
       		        	  if(isLaplace) 
       	              		{
       		              		 likelihoodNum[m]=  (1.0/moviesSize);
       		              		 if(vocSize!=0)  likelihoodDen[m]=  (vocSize /moviesSize); //vocSize!=0
       		              		 else   likelihoodDen[m]=  (1.0/moviesSize);	              	     
       		             
       		              		 likelihoodIndividual[m] = (likelihoodNum[m]/likelihoodDen[m]);
   		         	       		         	    
       	              		}
       	            
       		        	  else  if(isLog) 
     	              		{
     		               		 //Just assign it very small probability
	   		              	     // likelihoodIndividual[m] = NEG_EPSILON;
       		        		  
    	              		     likelihoodNum[m]=  (1.0/moviesSize);
	       	              		 if(vocSize!=0) likelihoodDen[m]=  (vocSize /moviesSize); //vocSize!=0
	       	              		 else likelihoodDen[m]=  (1.0/moviesSize);
	   		              		 likelihoodIndividual[m] = Math.log10(likelihoodNum[m]/likelihoodDen[m]);
     	              		}
       		        	  
       	              		else
       	              		{
       	              		     likelihoodIndividual[m] +=NEG_EPSILON;  // add a small probability
       	              			//likelihoodIndividual[m] = (likelihoodNum[m] + pCount)/(likelihoodDen[m] + pCount +4);
       	              		}
       		        	 }*/
                 
                	   }//end of for classes
           
                //-----------------------------------
                // Multiply likelihood && re-init
                //-----------------------------------
                            
                //Multiply likelihood obtained for a slot
                for (int k=0;k<classes;k++)
                {               	
               	   //Check which version is called, do add or multiply probabilities and return
               	   if(isLaplace)  // multiply prior and likelihood and send back
               	   {
		               	   
		               	  if (t==0) // type of extraction
		               		 {
		               		    likelihood[k] = likelihoodIndividual[k];
		               		    evidence = likelihoodIndividual[k];
		               		 }
		               	  else 
			               	  {
			               		  likelihood[k] =  likelihood[k] * likelihoodIndividual[k];
			               		  evidence  *=   likelihoodIndividual[k];
			               	  }
               	   }
               	   
               	   else if(isLog)					//Add
               	   {
	               		   if(t==0) 
		               		 {
		               			   likelihood[k] = likelihoodIndividual[k];
		               			   evidence = likelihoodIndividual[k] + priors[k];
		               		 }
	               		   else 
	               		   {
	               			   likelihood[k] += likelihoodIndividual[k];
	               			   evidence *=  likelihoodIndividual[k] + priors[k];
	               		   }
               	   }
               	   
               	   else 							//Multiply
               	   {
	               	   	  if (t==0) // type of extraction
		               		 {
		               		    likelihood[k] = likelihoodIndividual[k];
		               		    evidence = likelihoodIndividual[k];
		               		 }
		               	  else 
			               	  {
			               		  likelihood[k] =  likelihood[k] + likelihoodIndividual[k];
			               		  evidence =  likelihood[k] + likelihoodIndividual[k];
			               	  }            		   
               	   } 
               	}
                
                //Initialise the likelihoods
                for(int k=0;k<classes; k++)
                {
       	       	  likelihoodIndividual [k] 	= 0.0;		//shoULD BE 0
       	       	  likelihoodNum [k] 		= 0.0;
       	       	  likelihoodDen [k] 		= 0.0;
       	       	         	       	  
                }
                        
              }//end of type for              	 
              
              //-------------------------------------
              // Max posterior and find evidence
              //-------------------------------------
              evidence=0;
              
              if(isLog)
              {
          		//Find max posterior
            	/*double val_posterior_max = -1 * Math.pow(10, 10);
            	int    index_posterior_max =0;            	
            	for (int i=0;i<classes;i++)
            	{
            		//System.out.println("class= " + (i+1) + ", prior="+ priors[i]+ ", likelihood="+ likelihood[i]);
            		
            		if(likelihood[i] + priors[i] > val_posterior_max)
            		{
            			val_posterior_max = likelihood[i] + priors[i] ;		//value
            			index_posterior_max = i;							//index
            		}
            	}
            	
            	double dummyEvidence =0;*/
            	
            	//Find evidence
            	for (int i=0;i<classes;i++)
            	{
            		//dummyEvidence += Math.exp( likelihood[i] + priors[i] - val_posterior_max);
            		evidence += likelihood[i] + priors[i];
            	}
            	
            		//evidence = val_posterior_max + Math.log10(dummyEvidence);            	
            		//System.out.println("Evidence="+evidence);
            		
              } //end of if log
              
              //-----------------------------------------------------------------------------------
              // What we are doing is that, take a case, a class may have no match with the test set
              // in two slots, but very good for the remaining slots. (2* (NEG_EPSILON)). In the same case,
              // a class may have little little in evry slot an will win.
              // To overcome this, we do all the slots and at end check if likelihood is still zero,
              // If this is the case, we do not need that class, assign it very low prob (NEG_EPSILON)
              // and return from function
              
              boolean allNull = true;
        /*      //Initialise the likelihoods
              for(int k=0;k<classes; k++)
              {
     	         if(likelihood[k] == 0)
     	        	 likelihood[k] = NEG_EPSILON;
     	         else 
     	        	 allNull = false;       
              }
        */
              
              if (allNull == true )
            	  {
            	  	doNotIncludeThisMovie = true;
            	  	total_doNotIncludeThisMovie++;
            	  }
              
              
         /*     boolean isNullAll = true;
                boolean isNullTest = true;
                boolean isNullTrain = true;
                
                //check for nulls in all slots
                for (int t=0;t<typeToExtract.size();t++)
                {
               	 //for both to be zero for repective slots
               	 if (!(sizesOfTestInASlot [t] == 0 && sizesOfTrainInASlots [t]==0))
               		 isNullAll = false; 
               	 
               	 //If one of the slot was not empty for test set, flag become flase
               	 if (!(sizesOfTestInASlot [t] <= 5)) isNullTest = false; 
               	 
               	//If one of the slot was not empty for test set, flag become flase
               	 if (!(sizesOfTrainInASlots [t] <= 5)) isNullTrain = false;
               			 
               }
               
                if(isNullAll==true)
                {// System.out.println("Null is there with movie = "+ mid);
               	 
                }
                
                if(isNullTest==true)
               	 {	//System.out.println("Null is there (Test) with uid, mid = "+ uid + "," +mid + ", Train is " + isNullTrain);
               	 	nullTestFeatures++;
               	 	currentMovieHasNullFeatures = true;
               	 }
               
                if(isNullTrain==true)
                {//	 System.out.println("Null is there (Train) with uid, mid = "+ uid + "," +mid + ", Test is " + isNullTest);
               	     currentMovieHasNullFeatures = true;	
                }
*/         
          //    System.out.println("loop="+commonFeaturesLoopIndex);
          //return the likelihood for each class
          return likelihood;    	 
        
        }
            
/********************************************************************************************************/
 /**
  * Return the size of the vector (count of all words)
  */
            
       public double findSizeOfVector( HashMap<String, Double> features)
       {
    	   int size = features.size();
    	   double count =0;
    	   
    	   	  // Find set and iterators
        	  Set setTrainClass = features.entrySet();  
        	  Iterator jTrainClass = setTrainClass.iterator();    	   
        	  
        	  //Iterate over the words of Test set until one of them finishes
              	while(jTrainClass.hasNext()) 
              	 {
              	     Map.Entry words = (Map.Entry)jTrainClass.next();         // Next 		 
              	     String word 	 = (String)words.getKey();			     
              	     
              	     double TF 		= features.get(word);              	     
              	     count 		   +=TF;
              	 }//end of while    	   
    	   
              	return count;    	   
       }
       
/********************************************************************************************************/
/**
 * Get distinct token in all examples
 */
       public double getVocSize(int classes)
       {
    	   double vocSize =0;
    	   HashMap <String, Double> myVocabulary = new HashMap <String, Double>();
    	   
	       for (int m =0;m<classes;m++)
	       {
	    		  // Find set and iterators
	        	  Set setTrainClass = AllFeaturesInASlot[m].entrySet();  
	        	  Iterator jTrainClass = setTrainClass.iterator();    	   
	        	  
	        	  //Iterate over the words of Test set until one of them finishes
	              	while(jTrainClass.hasNext()) 
	              	 {
	              	     Map.Entry words = (Map.Entry)jTrainClass.next();         // Next 		 
	              	     String word 	 = (String)words.getKey();			     
	              	     
	              	   if (!(myVocabulary.containsKey(word)))       		   
	              		                 myVocabulary.put(word, 0.0);			 //we do not need TF 
	              	    
	              	 }//end of while
	          }
	       
	       return myVocabulary.size();
       }
       
       
/********************************************************************************************************/

            /**
             *  Return the likelihood for a all classes for a user 
             *  @param uid, the user id for which we want to get the likelihood
             *  @param mid, the mid which we want to predict (in test set)
             *  @param classes, how much classes we have [binary,i.e classes =2]
             */       
         
    public double[] getBinaryLikelihood(int uid, int mid, int classes)
    {
     	    // Features stored in the database    	 
         	    HashMap<String,Double> FeaturesTestMovie  = null; 
                HashMap<String,Double> FeaturesTrainClass = null; 
                            	    
      	     //Local variables
                LongArrayList movies;
                double rating 	 = 0.0;
                int moviesSize   = 0;
                int tempMid		 = 0;
                      
                double likelihood[] 			= new double [classes];
                double likelihoodIndividual[] 	= new double [classes];
                double likelihoodNum[] 			= new double [classes];
                double likelihoodDen[] 			= new double [classes];
                      
              //Initialise the likelihoods
               for(int i=0;i<classes; i++)
                {
               	  likelihood [i] 			= 0.0;			
               	  likelihoodIndividual [i] 	= 0.0;
               	  likelihoodNum [i] 		= 0.0;			//we have to add pseudo counts
               	  likelihoodDen [i] 		= 0.0;
                   	  
                }
                      
                  //-----------------------------------------------------
              	  //Get all movies seen by this user from the training set               
                  //-----------------------------------------------------
                      
                      movies = MMh.getMoviesSeenByUser(uid); 
                      moviesSize = movies.size();
                      

             	   //-------------------------------------------
              	   // Get features for test movie and train set
              	   //-------------------------------------------
                	   
                  // For checking, if A test Movie and Train Class contain no commonality in all slots/?
                     int sizesOfTestInASlot[] = new int [typeToExtract.size()];
                          
                    //------------------------------
          	   		// For each slot
           	   		//------------------------------
               	   
	               	    currentLikelihood_Bad =0;
		               	currentLikelihood_Good =0;
		               	
                      //For each slot, we have to get all distinct words, and their count 
                      for (int t =0;t<typeToExtract.size();t++)
                 	  {
                     	 	//define varaibales
                     	    int  sizeTestMovie  = 0;
                     	    int  sizeTrainClass = 0;
                                    	           	   
                     	   //get a type
                 		   int type = typeToExtract.get(t);    		 
                 		   
                 		    //---------------------------------- 
             	    		//Get a test feature for this movie
                		    //----------------------------------
                 		   
                 		   FeaturesTestMovie   = getFeaturesAgainstASlot("Test",  type, mid);    	               	         
                            if (FeaturesTestMovie !=null)  {
                         	   								sizeTestMovie  = FeaturesTestMovie.size();
                         	   								sizesOfTestInASlot [t] = sizeTestMovie;
                            									}
                            else sizesOfTestInASlot [t] =0;   
                            
                   		   //System.out.println(" feature test size for type= " + type + ". and movie =" + mid + " is -->"+ sizeTestMovie);
           	        	   if(isDebug && sizeTestMovie ==0)
           	        	   {
           	        		   System.out.println(" feature test size for type= " + type + ". and movie =" + mid + " is -->"+ sizeTestMovie);
          	        	   }

           	       		 	 //------------------------------------- 
                	    	 //Get training features against a slot 
             	        	 //-------------------------------------       	        	   
             	        	  // Get training features
      	       	        	 GetTrainingSetFeatures (uid,2, type);
                 //---------------------------------------------------------        
                 // Get the common keywords, for each class in a certain slot
                 // in the training set with the test set
                 //---------------------------------------------------------

             	       commonFeatureFound_Good = 0;
             	       commonFeatureFound_Bad  = 0;             	        
             	       double vocSize =0;         	 		//set of all distinct words in a slot
                  	
             	    for (int m =0;m<classes;m++)
                   	{
                  		vocSize += AllBinaryFeaturesInASlot[m].size();
                  	}
             	         
                    for (int m =0;m<classes;m++)
                    {
                    	int commonFeaturesLoopIndex =0;
                 	    //System.out.println();
                 	     
                       if(sizeTestMovie!=0 && AllBinaryFeaturesInASlot[m].size()!=0)
                        {  
                      		  //Get entry sets for both vectors (test movie and train class)
                         	  Set setTestMovie = FeaturesTestMovie.entrySet();	    	      	       	  
                         	  Set setTrainClass = AllBinaryFeaturesInASlot[m].entrySet();
                           	  
                         	  Iterator jTestMovie  = setTestMovie.iterator();
                         	  Iterator jTrainClass = setTrainClass.iterator();                           	  
                   
                        	  //Find words count in the vector
                           	  double denomenatorSize = findSizeOfVector(AllBinaryFeaturesInASlot[m]);
                           	  
                           	  //For displaying words
                           	  HashMap <String, Double> commonWords = new HashMap<String, Double>();
                           
                         	  //Iterate over the words of Test set until one of them finishes
             	              while(jTestMovie.hasNext()) 
             	               {
             	              	     Map.Entry words = (Map.Entry)jTestMovie.next();         // Next 		 
             	              	     String word 	 = (String)words.getKey();			     // Get a word from the train class
             	              	 
             	              	     
             	              	     //Get frequency count for the feature (All distinct words in that a slot, and in a class)
     			              	     double w2 = AllBinaryFeaturesInASlot[m].size();
     		       	              	 
             	              	     //If the Train set contain that word
             	              	    if(AllBinaryFeaturesInASlot[m].containsKey(word))
             	              	     {	
             	              	    		//-----------------
             	              	    		// TF of test movie
             	              	    		//-----------------
             	              	    	
             	              	    		double testWord_TF =  FeaturesTestMovie.get(word);
             	              	    		
             	              	    		//----------------
             	              	    		// count them
               	              	    	    //----------------
             	              	    	
             	              	    	 	if (m==0)  commonFeatureFound_Bad++;
             	              	    	 	else	   commonFeatureFound_Good++;
             	              	    		commonFeaturesLoopIndex ++;
             	              	    		//System.out.println(" word =" + word+ ", m= "+ m + ", " +commonFeaturesLoopIndex);
             	              	    		
             	              	    		//-----------------
             	              	    		// Add Numerator
             	              	    		//-----------------
             	              	    	
             	                	 		 //Get frequency count for the feature
             	                			 double w1 = AllBinaryFeaturesInASlot[m].get(word);
             	                			 
             	                			 //Add it in the respective class Numerator 
             	                			  Double N = w1;      			              	    	  
             	                			
             	                			 //Add in common words
             	                			  commonWords.put(word, N);
             	                			  
             			              	    //-----------------
             		          	    		// Add Denomenator
             		          	    		//-----------------
             		          	    	
             			            		 //Add it in the respective class Numerator 
             			              	     //double D =w2;
             	                			 Double D = denomenatorSize;
             			              	    
             			              	     if(isDebug)
             			              	     {
	             			              	     if(N==0 || D==0)		//It should not be the case
	             			              	     {
	             			           	    	 	System.out.print(" wordTF ="+ N + ", SLOT SIZE ="+ D);
	             			              	     	System.exit(1);
	             			              	     }
             			              	     }
             			              	  
             			              	     //-------------------------------------------------------
             			          	    	 // Get likelihood for a word in a slot in a certain class
             			              	     //-------------------------------------------------------
             			          	    	 
             			              	     //Multiply each words likelihood for each slot into that class likelihood
		             			              	 
             			                          			              	     
             			              	    if(isLaplace)
             			              	    {
             				              	     likelihoodNum[m]= N + (1.0/moviesSize);
             				              	     if (vocSize!=0) likelihoodDen[m]= D + (vocSize *1.0/moviesSize);
             				              	     else  likelihoodDen[m]= D + (1.0/moviesSize);
             				              	     
	             					              	 if(commonFeaturesLoopIndex==1)       					              	    	 
	      	       					              	 {
	      	       					              		 likelihoodIndividual[m] = (likelihoodNum[m]/likelihoodDen[m]);
	      	       					              	     
	      	       					              		 if (m==0) currentLikelihood_Bad = N/D;			//for  checking why it is given equal probs even for diff common loop?
	      	       					              	     else 	currentLikelihood_Good = N/D;
	      	       					              	 }
	      	       					              		 	       					              	    	
             					              	  else {
             					              		  			if(m==0)	currentLikelihood_Bad *= N/D;
             					              		  			else 		currentLikelihood_Good *= N/D;
             					              		  			//System.out.println("N/D for good =" +currentLikelihood_Good);
             					              		  		
             					              		  		likelihoodIndividual[m] =  likelihoodIndividual[m] * (likelihoodNum[m]/likelihoodDen[m]);
             					              		 
             					              		   }
             					              	    
             			              	    }
             			          
             			              	    //For log we do the addition of probs rather than multiplication 
             			              	    else  if(isLog)
      	   			              	    	{
	      	   				              	     likelihoodNum[m]= N + (1.0/moviesSize);
	      	   				              	     if (vocSize!=0) likelihoodDen[m]= D + (vocSize *1.0/moviesSize);
	      	   				              	     else  likelihoodDen[m]= D + (1.0/moviesSize);
	      	   				              	     
	      	   					              	 likelihoodIndividual[m] += (testWord_TF * (Math.log10(likelihoodNum[m]/likelihoodDen[m])));
	      	   					              	      	   					              	
	      	   					              	    
      	   			              	    	 }
         			              	    
         			          
             			              	    else
             			              	    {
             			              	    	 likelihoodNum[m]  = N + EPSILON;
             				              	     likelihoodDen[m]  = D + EPSILON +2;             				           
             				              	   
             				              	    if(commonFeaturesLoopIndex==1) //FIRST TIME (ASSIGN) 
             				              	     {
             				              	    	
             				              	    	likelihoodIndividual[m] = (likelihoodNum[m] )/(likelihoodDen[m]);	
             				              	     }
             				              	  
             				              	    else //NEXT TIME (MULTIPLY)		
             				              		   likelihoodIndividual[m] =  likelihoodIndividual[m] * likelihoodNum[m]/likelihoodDen[m];
             			              	    }
             			              	             	   
             	              	    } //common words           			 
             	              	 }//end of while
             	              	 
             	            //------------------------------------------------------
             	            // No common word is found between test and train docs
             	            //------------------------------------------------------
             	/*                System.out.println("common loop index for m="+m + "-->"+ commonFeaturesLoopIndex );
             	               System.out.println("common words m="+m + "-->"+ commonWords );
             	              System.out.println("Test words m="+m + "-->"+ FeaturesTestMovie );
             	*/              	
             	            if(commonFeaturesLoopIndex==0)
             	            {     	              		
             	             	  if(isLaplace)
             	              		{
             		              		 likelihoodNum[m]=  (1.0/moviesSize);
             		              		 if(vocSize!=0)  {
             		              			 				likelihoodDen[m]=  ((vocSize *1.0)/moviesSize); //vocSize!=0
             		              		 				  }	
             		              		 else   likelihoodDen[m]=  (1.0/moviesSize);
             		         	    	
             		              		 likelihoodIndividual[m] = (likelihoodNum[m]/likelihoodDen[m]);
             		         	    	        		         	    	 
             	              		}
             	              		
	             	              	else if(isLog)
	      	   	              		{	      	   		              		    	   		         	    	
	      	   		              		 likelihoodIndividual[m] = NEG_EPSILON;
      	   		         	    	        		         	    	 
	      	   	              		}
             	              		
	             	              	else
             	              		{              	    	
		             	               	 likelihoodNum[m]  = EPSILON;
	 				              	     likelihoodDen[m]  = EPSILON +2;       	              		
             	              	         likelihoodIndividual[m] = (likelihoodNum[m])/(likelihoodDen[m]);
             	              		
             	              		}
             	              	}
             	              	
             	              	if(commonFeaturesLoopIndex==0)
             	              	{
             	              		if(m==0) noCommonBinaryFeatureFound_Bad  = true;	// no coomon feature for bad {1,2,3}s
             	              		else     noCommonBinaryFeatureFound_Good = true;
             	              		noBinaryCommonality++;
             	              	}
             	              	
                              } //end of if size >0

    	              	//--------------------------------------------------------------------
                       	// One of the doc (test or train) or both of them have zero sizes. 
   	              	    //--------------------------------------------------------------------
                       
              		     else			//overcome the zero probabilities 
             		          {		        	     
             		        	  if(isLaplace) // but may be the vocabulary is zero
             	              		{
             		              		 likelihoodNum[m]=  (1.0/moviesSize);
             		              		 if(vocSize!=0)  likelihoodDen[m]=  (vocSize *1.0/moviesSize); //vocSize!=0
             		              		 else   likelihoodDen[m]=  (1.0/moviesSize);	              	     
             		             
             		              		 likelihoodIndividual[m] = (likelihoodNum[m]/likelihoodDen[m]);
         		         	             		         	    
             	              		}
             	            
             		           else  if(isLog) 
           	              		{
             		        		 likelihoodIndividual[m] = NEG_EPSILON;	        		         	    
           	              		}
             		        	  
             	                else
             	              		{
             	              			 likelihoodNum[m]  = EPSILON;
	 				              	     likelihoodDen[m]  = EPSILON +2;       	              		
            	              	         likelihoodIndividual[m] = (likelihoodNum[m])/(likelihoodDen[m]);
            	              		
             	              		}
             		        	 }
                      	   } //end of for
                 
                      //----------------------------------
                      // Multiply likelihood && re-init
                      //----------------------------------
                       
                      //Multiply likelihood obtained for a slot
                      for (int k=0;k<classes;k++)
                      {
                     	 
                     	  if (likelihoodIndividual[k]==0)
                     	   {
                     		   if(isLaplace)   //But it should not be the case (only if voc is zero)
                     		   {
             	        	  	     likelihoodNum[k]=  (1.0/moviesSize);
             	              		 if(vocSize!=0)  likelihoodDen[k]=  (vocSize *1.0/moviesSize); //vocSize!=0
             	              		 else   likelihoodDen[k]=  (1.0/moviesSize);	              	     
             	         	    
             	              		 likelihoodIndividual[k] = (likelihoodNum[k]/likelihoodDen[k]);
      		         	    
                      	       }
                      	    

                     		   else if(isLog) 
                     		   {
                     				 likelihoodIndividual[k] = NEG_EPSILON;      		         	    
                     		   }
                     		  
                     		   else
                     		   {	        		                   	     
                     			  likelihoodNum[k]  = EPSILON;
				                  likelihoodDen[k]  = EPSILON +2;       	              		
      	              	          likelihoodIndividual[k] = (likelihoodNum[k])/(likelihoodDen[k]);
      	              		
                     		   }
                     	   }
                     	
                     	 //Check which version is called, do add or multiply probabilities and return
                      	   if(isLaplace)  // multiply prior and likelihood and send back
                      	   {
	       	               	   
	       	               	  if (t==0) // type of extraction
	       	               		 {
	       	               		    likelihood[k] = likelihoodIndividual[k];
	       	               		    evidence = likelihoodIndividual[k];
	       	               		 }
	       	               	  else 
	       		               	  {
	       		               		  likelihood[k] =  likelihood[k] * likelihoodIndividual[k];
	       		               		  evidence  *=   likelihoodIndividual[k];
	       		               	  }
                      	   }
                      	   
                      	   else if(isLog)					//Add
                      	   {
	                      		  if(t==0) 
	       	               		  {
	       	               			   likelihood[k] = likelihoodIndividual[k];
	       	               			   evidence = likelihoodIndividual[k];
	       	               		  }
	                      		 else 
	                      		   {
	                      			   likelihood[k] += likelihoodIndividual[k];
	                      			  evidence +=  likelihoodIndividual[k];
	                      		   }
	                      	   }
                      	   
                      	   else 							//Multiply
                      	   {
                      		   if (t==0) // type of extraction
	       	               		 {
	       	               		    likelihood[k] = likelihoodIndividual[k];
	       	               		    evidence = likelihoodIndividual[k];
	       	               		 }
	       	               	   else 
	       		               	  {
	       		               		  likelihood[k] =  likelihood[k] * likelihoodIndividual[k];
	       		               		  evidence *= likelihoodIndividual[k];
	       		               	  }
	                   		   
	                      	   } 
                       }
                      
                      //Initialise the likelihoods
                      for(int k=0;k<classes; k++)
                      {
             	       	  likelihoodIndividual [k] 	= 0.0;		//shoULD BE 0
             	       	  likelihoodNum [k] 		= 0.0;
             	       	  likelihoodDen [k] 		= 0.0;
             	       	             	       	  
                      }
                              
                    }//end of type for
                    	 
                             
                //return the likelihood for each class
                return likelihood;    	 
              
              }
                  

  //----------------------------------------------------------------------------------------------------
        /**
         * Return features stored against a slot
         * @param TestOrTrainObject
         * @param type of feature to be extracted
         * @param int mid to be extract feature for
         * @return hashmap of the features
         */
               
     public HashMap<String, Double> getFeaturesAgainstASlot(String whichObj, int type, int mid)
     {
              
              	 MemHelper myObj = null;
              	 HashMap<String,Double> FeaturesTrainClass =null;
              	 
              	 if(whichObj.equalsIgnoreCase("Train"))
              		 myObj = MMh;
              	 else 
              		 myObj = MTestMh;
              		 
          		switch(type)
          		{
          		  case 0: 	FeaturesTrainClass = myObj.getFeaturesAgainstAMovie(mid);  		break;
          		  case 2: 	FeaturesTrainClass = myObj.getColorsAgainstAMovie(mid);  		break;
          		  case 4:	FeaturesTrainClass = myObj.getLanguageAgainstAMovie(mid); 		break;
          		  case 5:	FeaturesTrainClass = myObj.getCertificateAgainstAMovie(mid);	break;
          		  case 9:	FeaturesTrainClass = myObj.getTagsAgainstAMovie(mid); 			break;
          		  case 10:	FeaturesTrainClass = myObj.getKeywordsAgainstAMovie(mid); 		break;
          		  case 19:	FeaturesTrainClass = myObj.getBiographyAgainstAMovie(mid); 		break;
          		  case 94:	FeaturesTrainClass = myObj.getPrintedReviewsAgainstAMovie(mid); break;
          		  case 98:	FeaturesTrainClass = myObj.getPlotsAgainstAMovie(mid); 			break;
          		  case 15:	FeaturesTrainClass = myObj.getActorsAgainstAMovie(mid);	 		break;
          		  case 16:	FeaturesTrainClass = myObj.getDirectorsAgainstAMovie(mid);		break;
          		  case 17:	FeaturesTrainClass = myObj.getProducersAgainstAMovie(mid); 		break;
          		  case 100:	FeaturesTrainClass = myObj.getVotesAgainstAMovie(mid); 			break;
          		  case 101:	FeaturesTrainClass = myObj.getRatingsAgainstAMovie(mid); 		break;
          		  default:  																break; 
          			
          		}
          		
          		//for debugging
          		/*if(isDebug)
          		{
          			if(FeaturesTrainClass ==null)
          				System.out.println(" Size of Feature is Null");
          			else 
          				System.out.println(" Size of Feature is =" + FeaturesTrainClass.size() );
          		}
          		*/
          		
          		//--------------------------------
          		// Do feature selection here, for 
          		// each user's movies
          	    //--------------------------------
          		          		
          		return FeaturesTrainClass;
       }


  /***************************************************************************************************/
 
     /**
      * Return the class with the Max(Priors, Likelihood)
      * @param priors, priors for each class
      * @param likelihood, likelihood for each class
      * @param class
      */
     
     public double getMaxClass (int uid, int mid, 
    		 					double priors[], double likelihood[], 
    		 					double binaryPriors[], double binaryLikelihood[],
    		 					int classes, double thresholdProb, double thresholdRat)
     {
    	 
    	OpenIntDoubleHashMap results = new OpenIntDoubleHashMap();					// from class,prior*likelihood
     	//OpenIntDoubleHashMap mySimPriors = myFilter.getPriorWeights(3, uid, mid);	//from simple CF priors
    	OpenIntDoubleHashMap finalResults = new OpenIntDoubleHashMap();				// addition of NB and simPriors from CF
    	OpenIntDoubleHashMap binaryResults = new OpenIntDoubleHashMap();			// from class,binaryPrior*binaryLikelihood
    	
    	//-----------------------------------------------------------------
    	// IMP: Start from (1) -->class 1
    	// From (Keys -->Class) -----> 1-->1, 2-->2
    	// i.e. keys = class
    	//-----------------------------------------------------------------    	
       //First add priors and likelihood results into an array 
    	
       for(int i=0;i<myClasses; i++)
       {
    	   if(isLaplace)
    	   {
    	         //  results.put(i+1,  priors[i] * likelihood[i]/evidence);	
    	             results.put(i+1,  priors[i] * likelihood[i]);			
    		   	 //  results.put(i+1,  likelihood[i]);
    		   
    	        // count the cases where the probs are zeros 
    	    	//   if (priors[i] * likelihood[i] ==0)
    	      		   		totalResultsUnAnswered++;
    	    	   
    	   }
    	   
    	   else if(isLog)
    	   {
    		   //  	results.put(i+1, (priors[i] + likelihood[i]) - evidence);	
    		   // 	results.put(i+1, (priors[i] + likelihood[i]) /evidence);
    	     	    results.put(i+1, (priors[i] + likelihood[i]));
    	   // 		results.put(i+1,  0.8* priors[i] + 0.2 * likelihood[i]);				    		   
    		//    	results.put(i+1,  likelihood[i]);							
    	     //     results.put(i+1,  priors[i]);								
 	     
    		      //  System.out.println("priors["+i+"]=" + priors[i] + ", likelihiood["+i+"]="+likelihood[i]);
    	   }
    	   
    	   else
    	   {	
    		 //  	results.put(i+1, (priors[i] + likelihood[i])/evidence);	
    		 		results.put(i+1,  priors[i] + likelihood[i]);				    		   
     		  //   	results.put(i+1,  likelihood[i]);							
     	     //     results.put(i+1,  priors[i]);								
  	     
    	   }
    	  
    	   
    	   //add both cases (NB + sim Priors)
    	   //finalResults.put(i+1, results.get(i) + mySimPriors.get(i));
    	//   finalResults.put(i+1, results.get(i) * mySimPriors.get(i));
    	   
       }
       

       //First add binaryPriors and binaryLikelihood results into an array 
       for(int i=0;i<2; i++)
       {
    	     if (isLaplace)
    	     {
    	       //       binaryResults.put(i, binaryPriors[i] * binaryLikelihood[i]/binaryEvidence);		//classes =2
    	       //      	binaryResults.put(i, binaryPriors[i] * binaryLikelihood[i]);						//classes =2
    	                binaryResults.put(i,  binaryLikelihood[i]);						
    	     }
    	     
    	     else if (isLog)
    	     {
    	         //        binaryResults.put(i, binaryPriors[i] + binaryLikelihood[i]/binaryEvidence);		//classes =2
      	         //        binaryResults.put(i, binaryPriors[i] + binaryLikelihood[i]);
    	    	     	   binaryResults.put(i, binaryLikelihood[i]);
    	     }
    	     
    	     else 
    	     {
    	         //   binaryResults.put(i, binaryPriors[i] * binaryLikelihood[i]/binaryEvidence);		//classes =2
      	              binaryResults.put(i, binaryPriors[i] + binaryLikelihood[i]);	
    	     }
    	 }                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            
       
       // System.out.println(" P(i=0) = "+ binaryResults.get(0) + " P(i=1) = "+ binaryResults.get(1));
       
       //---------------------------------------------------
       //Go through all the classes and find the max of them
       //---------------------------------------------------
       
       //Add tied cases into it
       IntArrayList tieCases = new IntArrayList();	//Max tie cases will be equal to the no of classes
       
       //Sort the array into ascending order
       IntArrayList myKeys 		= results.keys(); 
       DoubleArrayList myVals 	= results.values();       
       results.pairsSortedByValue(myKeys, myVals);       
       
       //Sort the binaryResult into ascending order
       IntArrayList myBinaryKeys 		= binaryResults.keys(); 
       DoubleArrayList myBinaryVals 	= binaryResults.values();       
       binaryResults.pairsSortedByValue(myBinaryKeys, myBinaryVals);
       
       //------------------------------------
       // check if all classes's likelihood
       // are constant 
       //------------------------------------
       
       for (int i=0;i<classes;i++)
       {
    	   if (myVals.get(i) != NEG_EPSILON)
    	    { 
    		   		isProbIsConstant = false;
    		   		break;
    		}
       }
       
       //--------------
       //print probs
       //--------------
       
       //sum = sumOfProbabilities(evidence, myVals, myKeys);

       //last index should have the highest value
       boolean tieFlag = false;
       for(int i=classes-1;i>=0;i--)
       {
    	   if(i>0)
    	   {
    		   if(myVals.get(classes-1) == myVals.get(i-1)) 
    		   {
    			 tieCases.add(myKeys.get(i-1));		//This index contains value as that of highest result
    			 tieFlag = true;
    		   }    		   
    	   }    	   
    	   
       } //end of finding tied cases
       
       //---------------------------
       //Determine the winner index
       //---------------------------       
       if (tieFlag == true)
    	   		totalTieCases++;
       
/*       // By Default it should be the last index  in the array and its key corresponds to the class
       for (int i = 0;i<myClasses;i++)
  	   {
  	   		sum += myVals.get(i)/evidence;
  	   	    //System.out.println("index = " + i +", Keys ="+ myKeys.get(i) + ", Values=" + myVals.get(i));  	   	    
  	   	    System.out.println(" Keys ="+ myKeys.get(i) + ", Values=" + myVals.get(i));
  	   }*/
     
       double winnerIdx = (double) myKeys.get(myClasses-1);
       
   /*  //But if tie, then do random break
       if(tieCases.size()>0)
       {
	       //Break the ties through random return
	       int randIdx   = rand.nextInt(tieCases.size());
	       winnerIdx     = tieCases.get(randIdx); 
	    	   
	   }*/

       //-------------------------------------------------------------------------------------------
       //  If I am able to identify the correctly classified objects, then the accuracy will increase
       //  To a great extent
       //-------------------------------------------------------------------------------------------      

       	    //    if(3>2) return myKeys.get(0);
                  if(3>2) return winnerIdx;
                  
        System.out.println("error");
        //------------------------------------
        // CF
        //------------------------------------
        
       double rating_CF	= 0; 
       rating_CF = myFilter.recommendS(uid, mid, 80, 1); //uid, mid, neighbours, version       
       
      // if(3>2) return rating_CF + 0;		//added pseudo-count (0.2 etc)
       
       //------------------------------------
       // Do Hybrid Reasoning
       //------------------------------------
      
      // double finalReasoningAnswer = doHybridReasoning(results, binaryResults, rating_CF, threshold); 
       
      // return finalReasoningAnswer;
       //  return winnerIdx;
       
       //--------------
       //Learn priors
       //--------------
       
       //Sim priors sorting
    /*   IntArrayList mySimKeys 		= mySimPriors.keys(); 
       DoubleArrayList mySimVals 	= mySimPriors.values();       
       results.pairsSortedByValue(mySimKeys, mySimVals);
       
       //final answer sorting
       IntArrayList myFinalKeys 		= mySimPriors.keys(); 
       DoubleArrayList myFinalVals 		= mySimPriors.values();       
       results.pairsSortedByValue(myFinalKeys, myFinalVals);*/
       
       //-----------------
       // final tie cases
       //-----------------
       	 double min = 0.5;
		 int   category = 0;
		 
		 boolean finalTieFlag = false;
		 IntArrayList  finalTieCases = new IntArrayList();
		 
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
	    	
	    	   
	  		//-------------------------------------
			// Learn some confidence function
	      	// L(0) - L(1)
			//-------------------------------------
	      
/*	      	//Max value 
	      	double L0 = myVals.getQuick(classes-1);
	      
	      	// If not tie cases
	      	 if(tieFlag ==false)
	      	  {
	      		 double diff = L0- myVals.getQuick(classes-2);
	      		 
	      		 if(diff>threshold)
	      			 return (double)myKeys.getQuick(classes-1);
	      	   }
	      	 
	      	 //else return CF rating
	      	 return rating_CF;

       */
       
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
	     		  if(diff>0.05)
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
	 	 //if(Math.abs(ans - rating_CF) <thresholdRat)
     	 if(Math.abs(ans - rating_CF) <0.5)
	 			return ans;
     	 
     	 if(totalTies++ == finalTieCases.size())
     		break;
     	}	     	 
     	
     	
 		 //else return CF rating
 	     	 return rating_CF;

     // 1- Gave good results, with threshold =0.4 and diff of predictions = 0.6  (x2)
     //	2- Gave good results, with threshold =0.04 and diff of predictions = 0.6  (DF)  
     // 3- 0.70 with >0.1 and diff =0.4  
       
		//-------------------------------------
		//Learn some confidence function (try1)
		//-------------------------------------	      
/*
		   if(finalTieFlag==false)
		   {	   
			   if(Math.abs(myFinalKeys.get(4) - rating_CF)  < threshold) return myFinalKeys.get(4);
			   if(Math.abs(myFinalKeys.get(3) - rating_CF)  < threshold) return myFinalKeys.get(3);
			   
		   }   	   
		   
		   else 
		    { 
			   for (int i=0;i<finalTieCases.size();i++)
			   {
				   for (int j=i;j<finalTieCases.size();j++)
				   {
					   double d1= Math.abs(myFinalKeys.get(i) - rating_CF);
					   double d2= Math.abs(myFinalKeys.get(j) - rating_CF);
					   
				     if (d2  < d1  && d2 < threshold) return (myFinalKeys.get(i));
				     else  return (myFinalKeys.get(j));
				     
				
				   }
				   
			   } //end of outer for
			   
			   if(Math.abs(myKeys.get(4) 	 - rating_CF)      < threshold) return myKeys.get(4);
			   else if(Math.abs(mySimKeys.get(4) - rating_CF)  < threshold) return mySimKeys.get(4);
			   else if(Math.abs(myKeys.get(3) 	 - rating_CF)  < threshold) return myKeys.get(3);
			   else if(Math.abs(mySimKeys.get(3) - rating_CF)  < threshold) return mySimKeys.get(3);
		   
		    }
		       
		 
*/	       
		
	   	//------------------------------------
		//Learn some confidence function (try2)
	   	//------------------------------------
	   
	   // Naive Bayes
//	   if(Math.abs(winnerIdx - rating_CF) < Math.abs(myKeys.get(3) - rating_CF))
//	   {
//		   if(Math.abs(winnerIdx - rating_CF)  < threshold) return winnerIdx;
//	   }
//	   	   
//       else if(Math.abs(myKeys.get(3) - rating_CF)  < threshold) return myKeys.get(3);
//      
//       
//	   //priors
//	   if(Math.abs(mySimKeys.get(4) - rating_CF) < Math.abs(mySimKeys.get(3) - rating_CF))
//	   {
//		   if(Math.abs(mySimKeys.get(4) - rating_CF)  < threshold) return mySimKeys.get(4);
//	   }
//	   
//       else if(Math.abs(mySimVals.get(3) - rating_CF)  < threshold) return mySimKeys.get(3);
//       
       

		   	//------------------------------------
			//Learn some confidence function (try3)
		   	//------------------------------------
/*
	   if(tieFlag==false)
	   {	   
			   if(Math.abs(winnerIdx - rating_CF)  < threshold) return winnerIdx;		  
	   }   	   
	   
	   else 
		 { 

		   for (int i=0;i<tieCases.size();i++)
		   {
			   for (int j=i;j<tieCases.size();j++)
			   {
				   double d1= Math.abs(myKeys.get(i) - rating_CF);
				   double d2= Math.abs(myKeys.get(j) - rating_CF);
				   
			     if (d2  < d1  && d2 < threshold) return ((myKeys.get(i) + rating_CF)/2);
			     else if (d1  < d2  && d1 < threshold)  return ((myKeys.get(j) + rating_CF)/2);
			   }
		   }
		   
		 
		 }

	   return rating_CF;
*/
		   	//------------------------------------
			//Learn some confidence function (try3)
		   	//------------------------------------
/*
	   if(tieFlag==false)
	   {	   
			    if(Math.abs(winnerIdx - rating_CF)  < threshold) return winnerIdx;		  
	   }   	   
	   
	   else 
		 { 

		   for (int i=0;i<tieCases.size();i++)
		   {
			   for (int j=i;j<tieCases.size();j++)
			   {
				   double d1= Math.abs(myKeys.get(i) - rating_CF);
				   double d2= Math.abs(myKeys.get(j) - rating_CF);
				   
			     if (d2  < d1  && d2 < threshold) return (myKeys.get(i));
			     else  return (myKeys.get(j));
			     
			
			   }
			   
		   } //end of outer for
		   
		   double d=0;
		   for (int i=0;i<tieCases.size();i++)
		   { 
			    d +=myKeys.get(i);
			   
		   }
		   
		   return (d/tieCases.size());
		 }
*/	      
	       /*
		   //priors
		   if(Math.abs(mySimKeys.get(4) - rating_CF) < Math.abs(mySimKeys.get(3) - rating_CF))
		   {
			   if(Math.abs(mySimKeys.get(4) - rating_CF)  < threshold) return mySimKeys.get(4);
		   }
		   
	       else if(Math.abs(mySimVals.get(3) - rating_CF)  < threshold) return mySimKeys.get(3);
	     
*/
          	
	  /*    if(rating_CF<=0) 
	      		return 	(MMh.getAverageRatingForUser(uid));
	    	   	*/   	
       
     }
     

/***************************************************************************************************/
  /**
   * Sum of probabilities in the NB
   */
     
   public double sumOfProbabilities(double myEvidence, DoubleArrayList myVals, IntArrayList myKeys)
   {
      
     double sum = 0;
     for (int i = 0;i<myClasses;i++)
  	   {
  	   		sum += myVals.get(i);
  	   	    //System.out.println("index = " + i +", Keys ="+ myKeys.get(i) + ", Values=" + myVals.get(i));  	   	    
  	   	    System.out.println(" Keys ="+ myKeys.get(i) + ", Values=" + myVals.get(i));
  	   }
     
     if(sum == 5* NEG_EPSILON) total_NullValuesAfterThreshold++;
     System.out.println("---------------");
/*     
     for (int i=0;i<myVals.size();i++)
	   {
	  	    System.out.print(myKeys.get(i)+ ", ");  	   	    
	   }
     */
    // System.out.println();
   
     /*  System.out.println("\n Sum = "+ sum);
     System.out.println("\n--------------------");

       */
     return sum;
     
     }
     
/***************************************************************************************************/
/**
 * Do hybrid reasoning and send the results back
 * @param results NB
 * @param binaryResults binaryNB
 * @param result_CF CF rating prediction
 * @return prediction
 */
   public double doHybridReasoning(	OpenIntDoubleHashMap results, 
		   							OpenIntDoubleHashMap binaryResults, 
		   							double result_CF,
		   							double threshold)
	{	     
	    int classes = 5;
	    IntArrayList tieCases = new IntArrayList();
	    
		//Sort the array into ascending order
	    IntArrayList myKeys     = results.keys(); 
	    DoubleArrayList myVals 	= results.values();       
	    results.pairsSortedByValue(myKeys, myVals);
	    	    
	    //Sort the binaryResult into ascending order
	    IntArrayList myBinaryKeys 	 = binaryResults.keys(); 
	    DoubleArrayList myBinaryVals = binaryResults.values();       
	    binaryResults.pairsSortedByValue(myBinaryKeys, myBinaryVals);
	    
	    double binaryConfidence 	= Math.abs(myBinaryVals.get(1) - myBinaryVals.get(0));
	    double probConfidence	 	= Math.abs(myVals.get(1) - myVals.get(0));
	    double diffOfNBAndCFFirst 	= myKeys.get(4) - result_CF;
	    double diffOfNBAndCFSecond 	= myKeys.get(3) - result_CF;
	    double diffOfNBAndCFThird 	= myKeys.get(2) - result_CF;
	    double diffOfNBAndCFourth 	= myKeys.get(1) - result_CF;
	    
	    //------------------------
	    // Tie-cases
	    //-----------------------
	    
	 /*   boolean tieFlag =false;
	       for(int i=classes-1;i>=0;i--)
	       {
	    	   if(i>0)
	    	   {
	    		   if(myVals.get(classes-1) == myVals.get(i-1)) 
	    		   {
	    			 tieCases.add(myKeys.get(i-1));		//This index contains value as that of highest result
	    			 tieFlag =true;
	    		   }
	    		   
	    	   }
	    	   
	    	   
	       }//end of finding tied cases
	       
	       
	    //---------------------------
	    // Do the reasoning
	    //---------------------------
	    
	    if(myBinaryKeys.get(1) ==1) 	//good case
	    {	    
	    	if(myKeys.get(4) >=4 && result_CF >=4)				//good, good
	    	{	    		
	    		if(tieFlag==false)
	    			if(probConfidence >threshold) return myKeys.get(4);
	    		
	    		if(diffOfNBAndCFFirst <threshold)  return myKeys.get(4);
	    		
	    		//handle Tie cases
	    		if(tieFlag) // for tie-cases
	    		{
	    		   for (int i=0;i<tieCases.size();i++)
	 			   {		
	    			   int tieKey 		= tieCases.get(i);
	    			   double oneTieVal = myKeys.get(tieKey);
	   
	    			   double d1 = Math.abs(oneTieVal - result_CF);	 				   
	 				   if(d1<threshold)
	 					   		return oneTieVal;	 					   
	 					
	 			   }
	    		   
	    		} //end of tie cases
	    		
	    	}
	    	
	    	else  if(myKeys.get(4) >=4 && result_CF <4)			//good, bad
	    	{
	    		double ceiled_CF  = Math.ceil(result_CF);	    		
	    		double floored_CF = Math.floor(result_CF);
	    		
	    		if(ceiled_CF >=4)
	    		    if(ceiled_CF == myKeys.get(4)) return ceiled_CF;
	    		
	    		if(floored_CF >=4)
		    		if(floored_CF == myKeys.get(4)) return floored_CF;	
	    		    		
	    		if(tieFlag) // for tie-cases
	    		{
	    		   for (int i=0;i<tieCases.size();i++)
	 			   {		
	    			   int tieKey 		= tieCases.get(i);
	    			   double oneTieVal = myKeys.get(tieKey);
	   
	    			   //ceiled
	    			   double d1 = Math.abs(oneTieVal - ceiled_CF);	 				   
	 				   if(d1<threshold && ceiled_CF>=4)
	 					   		return oneTieVal;	 			
	 				   
	 				   //floored
	 				   d1 = Math.abs(oneTieVal - floored_CF);	 				   
	 				   if(d1<threshold&&floored_CF>=4)
	 					   		return oneTieVal;	 			
	 				   
	 			   }
	    		   
	    		} //end of tie cases
	    	}
	    	
	    	else  if(myKeys.get(4) <4 && result_CF >=4)			//bad, good
	    	{
	    	
	    		if(tieFlag) // for tie-cases
	    		{
	    		   for (int i=0;i<tieCases.size();i++)
	 			   {		
	    			   int tieKey 		= tieCases.get(i);
	    			   double oneTieVal = myKeys.get(tieKey);
	    			   
	 				   double d1 = Math.abs(oneTieVal - result_CF);
	 				   
	 				   if(oneTieVal >=4) 
	 					{	 					   
	 					   if(d1<threshold)
	 					   		return oneTieVal;	 					   
	 					}
	 				  	 				   
	 			   }
	    		   
	    		} //end of tie cases
	    			
	    		//May be there are no tie cases, but we can stil check the previous max value
	    		
	    		   double d1 = Math.abs(myKeys.get(3) - result_CF);
	    		   if(myKeys.get(3) >=4 && d1<threshold)
	    			   return myKeys.get(3);
	    		
	    		
	    	}
	    	
	    	else  if(myKeys.get(4) <4 && result_CF <4)			//bad, bad
	    	{
	    		double ceiled_CF  = Math.ceil(result_CF);	    		
	    		double floored_CF = Math.floor(result_CF);
	    		
	    		if(tieFlag) // for tie-cases
	    		{
	    		   for (int i=0;i<tieCases.size();i++)
	 			   {		
	    			   int tieKey 		= tieCases.get(i);
	    			   double oneTieVal = myKeys.get(tieKey);
	    			   
	 				   double d1 = Math.abs(oneTieVal - ceiled_CF);
	 				   double d2 = Math.abs(oneTieVal - floored_CF);
	 				   
	 				   if(oneTieVal >=4) 
	 					{	 					   
	 					   if(d1<threshold)
	 					   		return oneTieVal;
	 					   
	 					   if(d2<threshold)
	 					   		return oneTieVal;	
	 					}
	 				  	 				   
	 			   }
	    		   
	    		} //end of tie cases
	    		
	    		//May be it is not tie case
	    		 double d1 = Math.abs(myKeys.get(3) - floored_CF);
	    		 	if(d1<threshold) return myKeys.get(3);
	    		
	    		 d1 = Math.abs(myKeys.get(3) - ceiled_CF);
		    	 	if(d1<threshold) return myKeys.get(3);
		    		    	
	    	 }
	    	
	    }
	    	
	    //----------------------------------
	    //  bad case, movie is bad
	    //----------------------------------
	    else							 //bad
	    {
		  	if(myKeys.get(4) <4&& result_CF <4)				//bad,bad
	    	{	    		
	    		if(tieFlag==false)
	    			if(probConfidence >threshold) return myKeys.get(4);
	    		
	    		if(diffOfNBAndCFFirst <threshold)  return myKeys.get(4);
	    		
	    		//handle Tie cases
	    		if(tieFlag) // for tie-cases
	    		{
	    		   for (int i=0;i<tieCases.size();i++)
	 			   {		
	    			   int tieKey 		= tieCases.get(i);
	    			   double oneTieVal = myKeys.get(tieKey);
	   
	    			   double d1 = Math.abs(oneTieVal - result_CF);	 				   
	 				   if(d1<threshold)
	 					   		return oneTieVal;	 					   
	 					
	 			   }
	    		   
	    		} //end of tie cases
	    		
	    	}
	    	
	    	else  if(myKeys.get(4) <4 && result_CF >=4)			//bad, good
	    	{
	    		//double ceiled_CF  = Math.ceil(result_CF);	    		
	    		double floored_CF = Math.floor(result_CF);
	    	    		
	    		if(floored_CF <4)
		    		if(floored_CF == myKeys.get(4)) return floored_CF;	
	    		    		
	    		if(tieFlag) // for tie-cases
	    		{
	    		   for (int i=0;i<tieCases.size();i++)
	 			   {		
	    			   int tieKey 		= tieCases.get(i);
	    			   double oneTieVal = myKeys.get(tieKey);
	   
	    			   
	 				   //floored
	 				   double d1 = Math.abs(oneTieVal - floored_CF);	 				   
	 				   		if(d1<threshold && floored_CF<4)
	 				   				return oneTieVal;	 			
	 				   
	 			   }
	    		   
	    		} //end of tie cases
	    	}
	    	
	    	else  if(myKeys.get(4) >=4 && result_CF <4)			//good, bad
	    	{
	    	
	    		if(tieFlag) // for tie-cases
	    		{
	    		   for (int i=0;i<tieCases.size();i++)
	 			   {		
	    			   int tieKey 		= tieCases.get(i);
	    			   double oneTieVal = myKeys.get(tieKey);
	    			   
	 				   double d1 = Math.abs(oneTieVal - result_CF);
	 				   
	 				   if(oneTieVal <4) 
	 					{	 					   
	 					   if(d1<threshold)
	 					   		return oneTieVal;	 					   
	 					}
	 				  	 				   
	 			   }
	    		   
	    		} //end of tie cases
	    			
	    		//May be there are no tie cases, but we can stil check the previous max value
	    		
	    		   double d1 = Math.abs(myKeys.get(3) - result_CF);
	    		   if(myKeys.get(3) <4 && d1<threshold)
	    			   return myKeys.get(3);
	    		
	    		
	    	}
	    	
	    	else  if(myKeys.get(4) >=4 && result_CF >=4)			//good, good
	    	{
	    		double ceiled_CF  = Math.ceil(result_CF);	    		
	    		double floored_CF = Math.floor(result_CF);
	    		
	    		if(tieFlag) // for tie-cases
	    		{
	    		   for (int i=0;i<tieCases.size();i++)
	 			   {		
	    			   int tieKey 		= tieCases.get(i);
	    			   double oneTieVal = myKeys.get(tieKey);
	    			   
	 				   double d1 = Math.abs(oneTieVal - floored_CF);
	 				   
	 				   if(oneTieVal <4) 
	 					{	 					   
	 					   if(d1<threshold)
	 					   		return oneTieVal;
	 					   
	 					}
	 				  	 				   
	 			   }
	    		   
	    		} //end of tie cases
	    		
	    		//May be it is not tie case
	    		 double d1 = Math.abs(myKeys.get(3) - floored_CF);
	    		 	if(d1<threshold) return myKeys.get(3);
	    			    	
	    	 }
	    	
	        }
	    
	    //by defualt then
	    return result_CF;*/
	    
	    
	    //return the final prediction	    
    for (int i=0;i<2;i++)
	    {
	    	System.out.print(myBinaryKeys.get(i)+" =" + myBinaryVals.get(i)+ ", " );
	    }
	    
	    System.out.println();
	    

	    prob_Good = binaryResults.get(1);			// we stored good at index =1
	    prob_Bad  = binaryResults.get(0);			// bad at index =0	
	    
	    if(myBinaryKeys.get(1) == 1)   				// good case
	    	return 5;										//(For log we reverse the way)
	      
	    else 
	    	return 3;
	    

	    
	    //----------------------------------------------------------------
	    // Just  a random pick, to increase the sensitivity of CF
	    //----------------------------------------------------------------
	    
	    /*if(myBinaryKeys.get(1) ==1 && result_CF >=4)
	    		return result_CF;								//good, good
	    
	    if(myBinaryKeys.get(1) ==1 && result_CF <4)				//good, bad
    		{
	    		double ceiled_CF  = Math.ceil(result_CF);	    		
	    		double floored_CF = Math.floor(result_CF);
	    		
	    		if(myBinaryKeys.get(1) ==1 && ceiled_CF >=4)
	    			return ceiled_CF;
	    		
	    		if(myBinaryKeys.get(1) ==1 && floored_CF >=4)
	    			return floored_CF;	
	    		
    		}
    
	    if(myBinaryKeys.get(1) ==0 && result_CF <4)
	    		return result_CF;									//bad, bad
	    
	    if(myBinaryKeys.get(1) == 0 && result_CF >=4)				//bad, good
    	 {
	    		double ceiled_CF  = Math.ceil(result_CF);	    		
	    		double floored_CF = Math.floor(result_CF);
	    		
	    		if(myBinaryKeys.get(1) ==0 && ceiled_CF <4)
	    			return ceiled_CF;
	    		
	    		if(myBinaryKeys.get(1) ==1 && floored_CF <4)
	    			return floored_CF;
	    		
	    		
    	 }
	    
	    return result_CF;
	*/   
	}

/*******************************************************************************************************/
/**
 * Get binary features against a user's training movies
 */   
   
   public void GetBinaryTrainingSetFeatures(int uid, int classes)
   {
	
	   //clear the features first
	   for (int i=0;i<classes;i++)
	   {
		   AllBinaryFeaturesInASlot[i].clear();
	   }
	   
	   
  	   // Features stored in the database    	 
  	   HashMap<String,Double> FeaturesTrainClass = null; 
              	    
 	   //Local variables
       LongArrayList movies;
       double rating 	 = 0.0;
       int moviesSize    = 0;
       int tempMid	     = 0;
       int mid		     = 0;

	   //-----------------------------------------------------
   	   //Get all movies seen by this user from the training set               
       //-----------------------------------------------------
           
           movies = MMh.getMoviesSeenByUser(uid); 
           moviesSize = movies.size();
           

  	   //-------------------------------------------
   	   // Get features for test movie and train set
   	   //-------------------------------------------
     	   
       // For checking, if A test Movie and Train Class contain no commonality in all slots/?
           int sizesOfTrainInASlots[] = new int [typeToExtract.size()];
  
            //------------------------------
    	   	// For each slot
    	   	//------------------------------     	
           //For each slot, we have to get all distinct words, and their count 
           for (int t =0;t<typeToExtract.size();t++)
      	   {
          	 	//define varaibales
          	    int  sizeTrainClass = 0;
                         	           	   
          	   //get a type
      		   int type = typeToExtract.get(t);    		 
  
  	         //-------------------------------
  	         // For all movies in training set
    		 //-------------------------------
  	        	   
  	         for (int i=0;i<moviesSize;i++)
  	         {
  	        	//define and reset variables for each train movie
  	        	sizeTrainClass =0;
  	        	 
  	        	//Get a movie seen by the user
           	   tempMid 	 = MemHelper.parseUserOrMovie(movies.getQuick(i));
           	   rating 	 = MMh.getRating(uid, tempMid);
           	   
           	   //Get a training feature for this movie
      		   FeaturesTrainClass = getFeaturesAgainstASlot("Train",  type, tempMid);
                 if (FeaturesTrainClass !=null) 
                 {
                	 //---------------------------------------------
                	 // Do DF Thresholding: For Each Movie Features
                	 //---------------------------------------------            	 
                	 FeaturesTrainClass = doFeatureSelectionByDFThresholding 
                	 								(FeaturesTrainClass, classes,uid, type);
                	 
              	  	 sizeTrainClass = FeaturesTrainClass.size();
              	     sizesOfTrainInASlots [t] = sizeTrainClass;
                 }
    
  	               if(isDebug && sizeTrainClass ==0)
  	        	   {
  	            	   System.out.println(" feature train size for type= " + type + ". and movie =" + mid + " is -->"+ sizeTrainClass);
  	        	   }
  	               
           	   //Which class this movie lies {1,2,3}, {4,5}s
           	   int classIndex = (int) rating;

           	   if (classIndex <=3) classIndex =0;		//bad
           	   else classIndex =1;						//good
           	   
           	   //----------------------------------------      
           	   // Get All features in this slot for all
           	   // training movies
           	   //-----------------------------------------
           	  
           	   if(sizeTrainClass!=0)
           	   {
           		   Set setTrainClass = FeaturesTrainClass.entrySet();    	  
              	   Iterator jTrainClass = setTrainClass.iterator();
                	
              		while(jTrainClass.hasNext()) 
  	              	{
  	              	     Map.Entry words = (Map.Entry)jTrainClass.next();         // Next 		 
  	              	     String word 	 = (String)words.getKey();			      // Get a word from the train class
  	        			 double word_TF1 =  FeaturesTrainClass.get(word);		  // Get its TF
  	        			 
  	        			 //If word is there
  	        			 if(AllBinaryFeaturesInASlot[classIndex].containsKey(word))   // get the TF count and add it in newly TF 
  	        			 {
  	        			   double word_TF2 = AllBinaryFeaturesInASlot[classIndex].get(word);
  	        			   AllBinaryFeaturesInASlot[classIndex].put(word, word_TF1 + word_TF2); 
  	        			 }
  	        			 
  	        			 else // simply put the word, with its count
  	        			 {
  	        				AllBinaryFeaturesInASlot[classIndex].put(word, word_TF1 );
  	        			 }

  	        			 
  	        		  //-------------------------------------------------
  	        		  // Now ass all thes featurs into a hashmap for 
  	        		  // Finind evidence
  	        		  //-------------------------------------------------
  	 			 
  		        		//If word is there
  		        		 if(AllBinaryFeaturesForEvidence.containsKey(word)) // get the TF count and add it in newly TF 
  		        		 {
  		        			 double word_TF2 = AllBinaryFeaturesForEvidence.get(word);
  		        			 AllBinaryFeaturesForEvidence.put(word, word_TF1 + word_TF2); 
  		        		 }
  		        			 
  		        		 else // simply put the word, with its count
  		        		 {
  		        			 AllBinaryFeaturesForEvidence.put(word, word_TF1 );
  		        		 }

  		        			 
  		        			 
  	              	}
           	     } //end of if  
           	   } //end of finding all features against a type for all classes 
        
   	                 	        
	   }//end of for all types
   }
   
/***************************************************************************************************/
   /**
    * Get All features against a user's training movies
    */   
      
   public void GetTrainingSetFeatures(int uid, int classes, int type)
   {
	   //clear the features first
	   for (int i=0;i<classes;i++)
	   {
		   AllFeaturesInASlot[i].clear();
		   AllFeaturesForEvidence.clear();
	   }	   
	   
  	   // Features stored in the database    	 
  	   HashMap<String,Double> FeaturesTrainClass = null; 
              	    
 	   //Local variables
       LongArrayList movies;
       double rating 	 = 0.0;
       int moviesSize    = 0;
       int tempMid	     = 0;
       int mid		     = 0;

       //-----------------------------------------------------
   	   //Get all movies seen by this user from the training set               
       //-----------------------------------------------------
       
       movies = MMh.getMoviesSeenByUser(uid); 
       moviesSize = movies.size();       

 	   //-------------------------------------------
 	   // Get features for test movie and train set
       // For Each TYPE
 	   //-------------------------------------------
 	   
       //For checking, if A test Movie and Train Class contain no commonality in all slots/?
       int sizesOfTrainInASlots[] = new int [typeToExtract.size()];
       
       // Train class size
      	 int  sizeTrainClass = 0;  		 
  		   
	     //------------------------------
	     //For ALL MOVIES in training set
		 //------------------------------
	        	   
	for (int i=0;i<moviesSize;i++)
	 {
	   	 //define and reset variables for each train movie
	   	  sizeTrainClass =0;
	        	 
	      //Get a movie seen by the user
       	   tempMid 	 = MemHelper.parseUserOrMovie(movies.getQuick(i));
       	   rating 	 = MMh.getRating(uid, tempMid);
       	   
       	   //Get a training feature for this movie
       	   FeaturesTrainClass = getFeaturesAgainstASlot("Train",  type, tempMid);
             if (FeaturesTrainClass !=null) 
             {
            	 //---------------------------------------------
            	 // Do DF Thresholding: For Each Movie Features
            	 //--------------------------------------------
            	 
            	 FeaturesTrainClass = doFeatureSelectionByDFThresholding (FeaturesTrainClass, 
            			 												  classes,
            			 												  uid, 
            			 												  type);
         
            	 
              /* FeaturesTrainClass = doFeatureSelectionByX2Thresholding (FeaturesTrainClass, 
																		  classes,
																		  uid, 
																		  type);
            	*/
            	 
            	 //Size of the train movie
            	sizeTrainClass = FeaturesTrainClass.size();
            	sizesOfTrainInASlots [0] = sizeTrainClass;
            		
              }  
             
	         if(isDebug && sizeTrainClass ==0)
	          {
	              System.out.println(" feature train size for type= " + type + ", and movie =" + mid + " is -->"+ sizeTrainClass);
	          }
	               
       	   //Which class this movie lies {1,2,3,4,5,6,7,8,9,10} - 1
       	   int classIndex = (int) rating;
       	   
       	   //----------------------------------------      
       	   // Get All features in this slot for all
       	   // training movies
       	   //-----------------------------------------
       	  
       	   if(sizeTrainClass!=0)
       	   {
       		   Set setTrainClass = FeaturesTrainClass.entrySet();    	  
       		   Iterator jTrainClass = setTrainClass.iterator();
            	
          		while(jTrainClass.hasNext()) 
	              	{
	              	     Map.Entry words = (Map.Entry)jTrainClass.next();         // Next 		 
	              	     String word 	 = (String)words.getKey();			      // Get a word from the train class
	        			 double word_TF1 =  FeaturesTrainClass.get(word);		  // Get its TF
	        			 
	        			 //If word is there
	        			 if(AllFeaturesInASlot[classIndex-1].containsKey(word))  // get the TF count and add it in newly TF 
	        			 {
	        				 double word_TF2 = AllFeaturesInASlot[classIndex-1].get(word);
	        				 AllFeaturesInASlot[classIndex-1].put(word, word_TF1 + word_TF2); 
	        			 }
	        			 
	        			 else // simply put the word, with its count
	        			 {
	        				 AllFeaturesInASlot[classIndex-1].put(word, word_TF1 );
	        			 }
	        			 
        		      //-------------------------------------------------
        		      // Now add all thes featurs into a hashmap for 
        		      // Finding evidence
        		      //-------------------------------------------------
 			 
	        			//If word is there
	        			 if(AllFeaturesForEvidence.containsKey(word)) // get the TF count and add it in newly TF 
	        			 {
	        				 double word_TF2 = AllFeaturesForEvidence.get(word);
	        				 AllFeaturesForEvidence.put(word, word_TF1 + word_TF2); 
	        			 }
	        			 
	        			 else // simply put the word, with its count
	        			 {
	        				 AllFeaturesForEvidence.put(word, word_TF1 );
	        			 }
	        			 
 			 
	              	  }//end of while
       	     } //end of if  
       	   } //end of finding all features against a type for all classes 
    
       
       if(isDebug)
       {
     /*  //print sizes
       for (int i=0;i<classes;i++)
    	   System.out.println("size ="+  AllFeaturesInASlot[i].size());*/
       }
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
		   										int classes,									// no of classes		
		   										int uid,										// uid
		   										int type)										// type
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
 	              	       boolean word_OK = checkDFThresholding(word, uid, type);
 	              	       
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
   
   public boolean checkDFThresholding(String word, int uid, int type)
   {
	  
	   // Get all movies seen by this user
	  LongArrayList movies = MMh.getMoviesSeenByUser(uid); 
      int moviesSize = movies.size();
      int mid = 0; 
             
      // Define DF threshold per user's rating
      DF_THRESHOLD = (int)(moviesSize * (0.25));  // at 3 = there was no match
      
      //how many times this word occures across the doc
      int count =0;
	  		   
	  //For all movies
	  for (int i=0;i<moviesSize;i++)
	   {
		     //Get a movie seen by the user
	       	 mid = MemHelper.parseUserOrMovie(movies.getQuick(i));
	       		       	   
	       	 //Get a training feature for this movie
	       	 HashMap<String, Double>FeaturesAgainstAMovie  = getFeaturesAgainstASlot("Train",  type, mid);
	         
	       	 //check for match
	       	 if (FeaturesAgainstAMovie !=null) 
	       	 {	          
	       		 if(FeaturesAgainstAMovie.containsKey(word))
	       			 count++;   			 
	       		 
	       	 } //end of if	       	 
	       	
	     } //end of for
	 	 
	  		// If this word occures across DF_THRESHOLD no. of docs (movies), send true	
	  		if(count>=DF_THRESHOLD && count <moviesSize-2)
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
	       	 HashMap<String, Double>FeaturesAgainstAMovie  = getFeaturesAgainstASlot("Train",  type, mid);
	         
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
		  
/*		  System.out.print("class="+i);
		  System.out.print(",A =" +A[i]);
		  System.out.print(",B =" +B[i]);
		  System.out.print(",C =" +C[i]);
		  System.out.print(",D =" +D[i]);
		  System.out.print(",N =" +N);
		  System.out.print(",Num ="+num);
		  System.out.print(",Den ="+ den);
		  System.out.print(",x2="+ x2[i]+ ", ");
		  System.out.println();*/
		  		  
		  //find x2_Average and x2_Max
		  x2_Total += x2[i];
		  x2_Average += (priors[i]*x2[i]);		  
		  if(x2[i] > x2_Max && x2[i]!=0.0)
			  x2_Max = x2[i];
		  
		  
	  }
	  
      // System.out.println(x2_Average +","+x2_Max);	  
	  
	  // If this word occures across DF_THRESHOLD no. of docs (movies), send true	
	  		if(x2_Max>=X2_THRESHOLD)//  && count <moviesSize-5 )
	  			return true;
	 
	   //else we return false
	  		return false;
	   
   }
   
/***************************************************************************************************/	
     /**
	 * We can call this method from outside this method
	 * @param uid, active user id
	 * @param mid, movie to recomemnd
	 * @return prediction via naive bayes 
	 */
     
     public double GenerateRecViaNB (int uid, int mid)
     {    	
    	 LongArrayList movies = MTestMh.getMoviesSeenByUser(uid); //get movies seen by this user
    	     	         
        //get class priors
         double myPrior[] = getPrior(uid, myClasses);
	                
        //get class Likelihood
         double myLikelihood[] = getLikelihood(uid, mid, myClasses); //uid, mid, classes
	             
         //get result
         double myResult = getMaxClass (uid, mid, myPrior, myLikelihood, myPrior, myLikelihood, myClasses, 0.4, 0.4);
        	 
    	 return myResult;
    	 
     }     
     
     
/***************************************************************************************************/  
/***************************************************************************************************/    
   
  /**
    * Start recommending 
    */  
	
    public void makePrediction()	
 	{
 		System.out.println("Come to make prediction");
 		int moviesSize = 0;
 		double expectedError = 0;
 		int totalSamplesProcessed =0;
 		
         // For each user (in test set), make recommendations
         IntArrayList users = MTestMh.getListOfUsers(); 		        
         LongArrayList movies;
         double rating;
         int uid, mid;
         double thresholdProb = 1.5;
         double thresholdRat  = 0.1;

     // For mainLoop, learn Alpha   
     for(int mainLoop = 0;mainLoop<15;mainLoop++ )
     {
     	  thresholdProb += 0.5;
     	  thresholdRat	= 0.2;
     	  
     	  //Learn beta
    	for(int innerLoop = 0; innerLoop<3;innerLoop++ )
    	{
     	  thresholdRat += 0.1;   
     	  
     	  for (int i = 0; i < users.size(); i++)        
          {        	
        	 //System.out.println("currently at user =" +(i+1));
             uid = users.getQuick(i);          
             movies = MTestMh.getMoviesSeenByUser(uid); 				 //get movies seen by this user
             moviesSize = movies.size();             
            
         	 //get class priors
        	 double myPrior[] = getPrior(uid, myClasses);
	              
        	 //get binary class priors
        	// double myBinaryPrior[] = getBinaryPrior(uid, 2);			//uid, 2 classes	          

        	  	   
        	 totalSamplesProcessed++;
        	/* if(totalSamplesProcessed>0 && totalSamplesProcessed%100==0)
        		 System.out.println("At user.."+ totalSamplesProcessed + "with error" + rmse.mae());
          */
               for (int j = 0; j < moviesSize; j++)            
 	            {
            	 //Flag to detect the movies, with NULL feature set
            	 doNotIncludeThisMovie  = false;			
            	 
 	             //get Movie   
            	 mid = MemHelper.parseUserOrMovie(movies.getQuick(j));                
 	                 	         	 
            	 //get class Likelihood
            	 double myLikelihood[] = getLikelihood(uid, mid, myClasses);			 //uid, mid, classes
 	            
            	//get class Likelihood
                //  double myBinaryLikelihood[] = getBinaryLikelihood(uid, mid, 2); 	//uid, mid, 2 clases 	          
            	 
            	 //get result
            	 double myResult = getMaxClass (uid, mid, 							
            			 						myPrior, myLikelihood, 
            			 						myPrior, myLikelihood,
            			 						myClasses, thresholdProb, thresholdRat);
            	 
            //   if(isProbIsConstant ==false)
           //	 if(doNotIncludeThisMovie == false)            			 
            	 {
	            	 // add error
	 	             double myActual = getAndAddError(myResult, uid, mid, myClasses);
	 	            //double myActual = getExpectedError(myResult, uid, mid);	 	             
	 	            
	 	            // get Extreme errors and correct answers 	            
	 	             getExtremeErrorCount(myResult, myActual);
	 	             
	 	            // error
	 	              double ErrorFound = Math.abs(myActual - myResult);
	 	              
	 	            // error
	  	         
	 	   /*           System.out.println("Currently at user = "+ (i+1)+", error = actual - predicted ="
	           			+ ErrorFound + ", " + myActual+ ", "+ myResult );	  	  	           
	  	             System.out.println("--------------------------------------------------------------");
	 	              */
            	 }            	 
 	             
 	             //if(ErrorFound>=3)
 	              //if(prob_Bad > prob_Good)
 	              
 	              if(isDebug)
 	              {
 	            	 if(noCommonBinaryFeatureFound_Good ==true && noCommonBinaryFeatureFound_Bad==true)
 	            		System.out.println("no common feature for both classes (good and bad");
 	            	 if(prob_Good > prob_Bad)
 	            		 System.out.println("bad > good");
 	              }
 	              
 	              //If probs are constants throughout then add cases
 	              if(isProbIsConstant)
 	            	 totalCasesWhereProbIsConstant++; 	            	  
 	           
 	                  /*  	 
 	            		 System.out.println("Good =" + prob_Good+", Bad ="+prob_Bad+ "\n" +
          				"Good_CommonFeatures = "+commonFeatureFound_Good  +
          				", Bad_CommonFeatures  ="+ commonFeatureFound_Bad + "\n" +
          				", currentLikelihood_Good =" +currentLikelihood_Good );
 	            	     */
 	              	                
 	         } //end of all movies            
          } //end processing all users
     	  
	         //printError etc
	         printError(thresholdProb, thresholdRat);
	         
    	}//end of inner for         
    }// end of loop for 
          
 }//end of FUNCTION

 /*******************************************************************************************************/
    
    
    public void printError(double thrProb, double thrRat)
    {
    	 System.out.println(" ------------------------------------------------------------------------");
         System.out.println(" Threshold_Prob =" + thrProb);
         System.out.println(" Threshold_Rat =" + thrRat);
        // System.out.println("Final RMSE --:" 			+ rmse.rmse());
         System.out.println("Final MAE --:" 			+ rmse.mae());
         System.out.println("Final Coverage --:" 		+ rmse.getItemCoverage());
         System.out.println("ROC Sensitivity --:" 		+ rmse.getSensitivity());
         //System.out.println("ROC Specificity --:" 		+ rmse.getFalsePositiveRate());
         
      /*   System.out.println("Total equal cases = " + goodAndBadEqual);
         System.out.println("total_doNotIncludeThisMovie =" + total_doNotIncludeThisMovie); 
         System.out.println("Total Unequal cases = " + goodAndBadUnequal);
         System.out.println("No Binary feature Founds = " +  noCommonBinaryFeatureFound);
         System.out.println("Same prob cases = "+ totalCasesWhereProbIsConstant);
         System.out.println("Total Nulls after threshold="+total_NullValuesAfterThreshold);*/
         
         //System.out.println("Correctly Predicted --:"   + correctlyPredicted);
         System.out.println("% of correct --:"          + (correctlyPredicted * 100.0) / (totalPredicted));
         System.out.println("% of Error (>0 && <=1) --:" + (extremeError1 * 100.0) / (totalPredicted));
         System.out.println("% of Error (>1 && <=2) --:" + (extremeError2 * 100.0) / (totalPredicted));
         System.out.println("% of Error (>2 && <=3) --:" + (extremeError3 * 100.0) / (totalPredicted));
         System.out.println("% of Error (>3 && <=4) --:" + (extremeError4 * 100.0) / (totalPredicted));
         System.out.println("% of Error (>4) --:" 		+ (extremeError5 * 100.0) / (totalPredicted));
         
         /*//Print Extreme Error for individual user
         System.out.println("Error >=4 " + extremeError5); 
         System.out.println("Error >=3 " + extremeError4);
         System.out.println("Error >=2 " + extremeError3);
         System.out.println("Error >=1 " + extremeError2);
         System.out.println("Error >=0 " + extremeError1);
         */
         
    /*   System.out.println("Tie Cases " + totalTieCases);
         System.out.println("ZEROProb " + totalResultsUnAnswered);         
         System.out.println("No Binary Commonality " + noBinaryCommonality );*/

         
        // Here, we can re-set values in the class RMSE and other local variable
         rmse.resetValues();
         rmse.resetROC();
         
         extremeError1 = extremeError2 = extremeError3 = extremeError4= extremeError5= 0;
         totalTieCases = 0;
         totalResultsUnAnswered = 0;
         noBinaryCommonality =0;
         totalPredicted =0;
         correctlyPredicted =0;
         prob_Good = prob_Bad =0;
         noCommonBinaryFeatureFound_Bad = noCommonBinaryFeatureFound_Good =false;         
         commonFeatureFound_Good= commonFeatureFound_Bad=0;
         noCommonBinaryFeatureFound =0;
         isProbIsConstant = true;
         doNotIncludeThisMovie = false;
         total_doNotIncludeThisMovie =0;
         
    }//end of function
 	
/****************************************************************************************************/

 	public double getAndAddError(double rating, int uid, int mid, int classes)	
 	{
 	   double actual = MTestMh.getRating(uid, mid);	//get actual rating against these uid and movieids      
       rmse.add	(actual, rating);					//add (actual rating, Predicted rating)      
       rmse.addCoverage(rating);					//Add coverage
       rmse.ROC4(actual, rating, classes, MMh.getAverageRatingForUser(uid));			//Add ROC
       return actual;

 	}
 	
/****************************************************************************************************/

 	public double getExpectedError(double rating, int uid, int mid, int classes)	
 	{
 		double actual = MTestMh.getRating(uid, mid);	//get actual rating against these uid and movieids      
     
 	   if(actual - rating == 0) 
 		   	rmse.add(actual, rating);					//add (actual rating, Predicted rating)      
       
 	   else 
 		  rmse.add(actual, MMh.getAverageRatingForUser(uid));					//add (actual rating, Predicted rating)
 	  
       return actual;
 	}
/****************************************************************************************************/
/**
 * Count how much cases have extreme error
 */ 	
 	public void getExtremeErrorCount(double predicted, double actual)
 	{
 		
 		//count cases in which good and bad confidence are equal or unequal
 		if(prob_Good == prob_Bad)
 			goodAndBadEqual++;
 		
 		else 
 			goodAndBadUnequal++;
 		
 		//No Binary common feature found in both classes 
 		if (noCommonBinaryFeatureFound_Bad == true || noCommonBinaryFeatureFound_Good==true)
 			noCommonBinaryFeatureFound++;
 		
 		//error
 		double error = Math.abs(predicted-actual);
 		
 		// total predicted 
 		 totalPredicted++;
 		
 		// extreme errors
 		if (error==0) 
 			correctlyPredicted ++;
 		
 		else if (error <=1)
 		{
 			extremeError1++; 			
 		}
 		
 		else if (error >1 && error <=2)
 		{
 			extremeError2++; 			
 		}
 		
 		else if (error >2 && error <=3)
 		{
 			extremeError3++; 			
 		}
 		
 		else if (error >3 && error <=4)
 		{
 			extremeError4++; 			
 		}
 		
 		else if (error >4 && error <=5)
 		{
 			extremeError5++; 			
 		}
 		
 	}

/****************************************************************************************************/
/****************************************************************************************************/
 	
     
 public static void main(String args[])    
 {

     //SML
/*      String test  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\sml_clusteringTestSetStored.dat";
	  String train  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\sml_clusteringTrainSetStored.dat";
*/	        	

      String test  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\NB\\sml_clusteringTestSetStoredTF.dat";
	  String train  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\NB\\sml_clusteringTrainSetStoredTF.dat";

	  NaiveBayesRecHybridGoodBad myNB = new NaiveBayesRecHybridGoodBad(train, test);    	
	  myNB.makePrediction();
    	
    }
    
	
}
