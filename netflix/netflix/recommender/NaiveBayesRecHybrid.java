package netflix.recommender;



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
import cern.colt.map.OpenIntDoubleHashMap;
import netflix.algorithms.memorybased.memreader.FilterAndWeight;
import netflix.memreader.MemReader;
import netflix.memreader.MemHelper;
import netflix.rmse.RMSECalculator;


public class NaiveBayesRecHybrid 
{

    /** Flag to set Laplace smoothing when estimating probabilities */
     boolean isLaplace = false;

    /** Flag to set Laplace smoothing when estimating probabilities */
    boolean isLog =   true;
    
    /** Small value to be used instead of 0 in probabilities, if Laplace smoothing is not used */    
    double EPSILON = 1e-6; 

    /** Flag to debug */
    boolean isDebug   = false;
    
    /** Small pseudo count value to be used instead of 0 in probabilities, if Laplace smoothing is not used */    
    double pCount = 1;    
    
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
    
    // Recommender related object and varaibes
	MemHelper 		MMh;						//train set
    MemHelper 		MTestMh;					// test set	
    
    //Filtr and Weight
    FilterAndWeight myFilter;
    
    //Start up RMSE count
    RMSECalculator rmse;

    //rand object
    Random rand;
	
    //Classes
    int myClasses;
    
    //Extreme errors
    int extremeError5;
    int extremeError4;
    int extremeError3;
    int extremeError2;
    int extremeError1;
    
    //correct answers;
    int correctlyPredicted;
    int totalPredicted;
    
    //types to be extracted 
	IntArrayList typeToExtract;
	
    //Tie Cases
    int totalTieCases;
    
    //Prior*likelihood = psedu count
    int totalResultsUnAnswered;
    int totalZeroPriors;
    int totalZeroLikelihood;
    
    // evidence
    double evidence;
    
    //sum of prob
    double sum;
    
    // null features
    int nullTestFeatures ;
    boolean currentMovieHasNullFeatures;
    
    //No Commonality between test and train set
    boolean noCommonFeatureFound;
    int 	noCommonality;	
    
	
/**********************************************************************************************/	
/**
 * Constructor
 * @param train Object
 * @param test Object
 */
    
    public  NaiveBayesRecHybrid (String trainObject, String testObject)
    {
    
    	//Get test and train objects
	    MMh			= new MemHelper (trainObject);
	    MTestMh 	= new MemHelper (testObject);
	
	    //FilterAndWeight
	    myFilter = new FilterAndWeight(MMh,1);
	    
	    //Random object
		rand = new Random();    
		
		//For MAE
		rmse = new RMSECalculator();
		
		//assign how much classes, we want
	    myClasses = 5; 						// {5,10} for{ML,FT}	
	           
	    //Just to check for how many cases, it is unsuccessful extremely
	    extremeError1 = extremeError2 = extremeError3 = extremeError4 = extremeError5 = 0;
	
	    //correct answer 
	    correctlyPredicted  = 0;
	    totalPredicted      = 0;
	    
	    // For checking how much tie cases occured
	    totalTieCases = 0;
	    
	    // null features
	    int nullTestFeatures ;
	    boolean currentMovieHasNullFeatures;
	    
	    //No Commonality between test and train set
	    boolean noCommonFeatureFound;
	    int 	noCommonality;	
	    
	    //Count how much of the prior*likelihood are not contributing anything
	    totalResultsUnAnswered 	= 0;
	    totalZeroPriors 		= 0;
	    totalZeroLikelihood 	= 0;
	
	    //evidence
	    evidence 				= 0;
	
	  //sum of prob
	    sum = 0;
	    
	    //type
		typeToExtract = new IntArrayList();
		typeToExtract.add(0);
	/* 	typeToExtract.add(10);
		typeToExtract.add(9);
		typeToExtract.add(15);
		typeToExtract.add(16);
		typeToExtract.add(17);
		//typeToExtract.add(18);
		typeToExtract.add(98);*/
	  
		
    }
    
/**********************************************************************************************/
//Set methods
    
        /** Sets the debug flag */
        public void setDebug(boolean bool)
        {
        	debug = bool;
        }
            
        /** Sets the Laplace smoothing flag */
        public void setLaplace(boolean bool)
        {
        	isLaplace = bool;
        }
    	
        /** Sets the value of EPSILON (default 1e-6) */
        public void setEpsilon(double ep)
        {
        	EPSILON = ep;
        }

        

/*****************************************************************************************************/

        //Get methods
        
            /** Returns the name */
            public String getName() 
            {
              return name;
            }

            /** Returns value of EPSILON */
            public double getEpsilon()
            {
            	return EPSILON;
            }

            /** Returns value of isLaplace */
            public boolean getIsLaplace()
            {
            	return(isLaplace);
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
                double priors[] 	= new double [classes];
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
                		priors[index-1]++;
                 	         	
                }
                
                //Count the probabilities for each class                
                for(int i=0;i<classes;i++)
                {
                	//Perform Laplace smoothing
                	if(isLaplace)
                	{
                		 priors[i] = ((priors[i]+1)/(classes + moviesSize));
                	}
                	
                	else if(isLog)
                	{
                		priors[i] = ((priors[i]+1)/(classes + moviesSize));
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
            
/*     public double[] getLikelihood (int uid, int mid, int classes)
     {
    	 // Features stored in the database    	 
    	 HashMap<String,Double> FeaturesTestMovie  = null; 
         HashMap<String,Double> FeaturesTrainClass = null; 
         
         //Get Features for the Movie we want to predict
         FeaturesTestMovie   = MTestMh.getFeaturesAgainstAMovie(mid);
         int  sizeTestMovie  = FeaturesTestMovie.size();
 	    
 	    //Local variables
         LongArrayList movies;
         double rating 	 = 0.0;
         int moviesSize  = 0;
         int tempMid	 = 0;
         
         double likelihood[] 	= new double [classes];
         double likelihoodNum[] = new double [classes];
         double likelihoodDen[] = new double [classes];
         
         //Initialise the likelihoods
          for(int i=0;i<classes; i++)
          {
        	  likelihood 	[i] = 0.0;
        	  likelihoodNum [i] = 0.0;
        	  likelihoodDen [i] = 0.0;
        	  
          }
         
         //-----------------------------------------------------
 		 //Get all movies seen by this user from the training set               
         //-----------------------------------------------------
         
         movies = MMh.getMoviesSeenByUser(uid); 
         moviesSize = movies.size();
         
         //Calculate the probability that this movie will be in the given class                
         for (int i=0;i<moviesSize;i++)
         {
         	   //Get a movie seen by  user
         	   tempMid 	 = MemHelper.parseUserOrMovie(movies.getQuick(i));
         	   rating 	 = MMh.getRating(uid, tempMid);
         	   
         	   //Which class this movie lies {1,2,3,4,5,6,7,8,9,10} - 1
         	   int index = (int) rating;
         
         	   // Get features for this movie         	   
         	   FeaturesTrainClass  = MMh.getFeaturesAgainstAMovie(tempMid);
         	   int sizeTarainClass = FeaturesTrainClass.size();
         	   int count = 0;
         	   
               //------------------------        
         	   //Get the common keywords
         	   //------------------------
         	   
         	  if(sizeTestMovie!=0 && sizeTarainClass!=0)
              {  
         		  //Get entry sets for both vectors (test movie and train class)
            	  Set setTestMovie = FeaturesTestMovie.entrySet();	    	      	       	  
            	  Set setTrainClass = FeaturesTrainClass.entrySet();
              	  
            	  Iterator jTestMovie  = setTestMovie.iterator();
            	  Iterator jTrainClass = setTrainClass.iterator();
              	  	 
            	  //Iterate over the words of Train set until one of them finishes
	              	while(jTrainClass.hasNext()) 
	              	 {
	              	     Map.Entry words = (Map.Entry)jTrainClass.next();         // Next 		 
	              	     String word 	 = (String)words.getKey();			     // Get a word from the train class
	
	              	     //If the Test Movie contain that word
	              	    if(FeaturesTestMovie.containsKey(word))
	              	    {	
	              	    		//-----------------
	              	    		// Add Numerator
	              	    		//-----------------
	              	    	
	                	 		 //Get frequency count for the feature
	                			 double w1 = FeaturesTrainClass.get(word);
	                			 
	                			 //Add it in the respective class Numerator 
	                			 likelihoodNum[index-1] +=w1;     			 	                			 
	                			 
	              	     }
	              	    
	              	    //-----------------
          	    		// Add Denomenator
          	    		//-----------------
          	    	
	            		 //Get frequency count for the feature
	              	     double w1 = FeaturesTrainClass.get(word);
           			 
	              	     //Add it in the respective class Numerator 
	              	     likelihoodDen[index-1] +=w1;     			 	                			 
           	
           			 
	              	 }//end of while
               } //end of if size >0
         	    
         	 } //end of all movies seen by this user
         
         //--------------------------
         // Calculate the likelihood
         //--------------------------
         
         for (int i=0;i<classes;i++)
         {
        	 //Perform Laplace smoothning 
        	 if(isLaplace)
        	 {
        		
        		 
        	 }
        	 
        	 
        	//Add Epsilon
        	 else
        	 {        		 
	        	  if(likelihoodDen[i]==0)
	        		 likelihoodDen[i] = likelihoodDen[i] + EPSILON;
	        	  
	        	 if(likelihoodNum[i]==0)
	        		 likelihoodNum[i] = likelihoodDen[i] + (EPSILON * EPSILON);
	        	  
	        	   likelihood[i] = likelihoodNum[i]  /likelihoodDen[i];	//pesudo count =1;        	
        	 }
        	 
         }
             	     	 
         //return the likelihood for each class
    	 return likelihood;    	 
 
     }*/
            
/********************************************************************************************************/
            /**
             * Return the size of the vector (all word counts)
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
                         	     count 			+=TF;
                         	 }//end of while
                         	
                         	return count;               	   
                  }
                  
 /********************************************************************************************************/
 
          public double[] getLikelihood (int uid, int mid, int classes)
            {
           	    // Features stored in the database    	 
           	    HashMap<String,Double> FeaturesTestMovie  = null; 
                HashMap<String,Double> FeaturesTrainClass = null; 
                       	    
          	     //Local variables
                LongArrayList movies;
                double rating 	 = 0.0;
                int moviesSize   = 0;
                int tempMid	 	 = 0;
                
                double likelihood[] 			= new double [classes];
                double likelihoodIndividual[] 	= new double [classes];
                double likelihoodNum[] 			= new double [classes];
                double likelihoodDen[] 			= new double [classes];
                
                //Initialise the likelihoods
                 for(int i=0;i<classes; i++)
                 {
               	  likelihood [i] 			= 0.0;			//we have to multiply
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
                int sizesOfTestInASlots[] = new int [typeToExtract.size()];
                int sizesOfTrainInASlots[] = new int [typeToExtract.size()];
                    
             // For getting all features in a given class, and in a specific slot 
         	   HashMap <String, Double> [] AllFeaturesInASlot = (HashMap<String, Double>[])Array.newInstance(HashMap.class, classes);  	   
         	   for (int i=0;i<classes;i++)
         	   {
         		 AllFeaturesInASlot[i] = new  HashMap <String, Double>();
         	   }
         	   
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
                   	   								sizesOfTestInASlots [t] = sizeTestMovie;
                      									}
                      else sizesOfTestInASlots [t] =0;   
               	   
       	         
       	        	   if(isDebug && sizeTestMovie ==0)
       	        	   {
       	        		   System.out.println(" feature test size for type= " + type + ". and movie =" + mid + " is -->"+ sizeTestMovie);
       	        	   }

       	         //-------------------------------
       	         //For all movies in training set
         		 //-------------------------------
       	        	   
       	         for (int i=0;i<moviesSize;i++)
       	         {
       	        	//define and reset variabales for each train movie
       	        	sizeTrainClass =0;
       	        	 
       	        	//Get a movie seen by the user
                	   tempMid 	 = MemHelper.parseUserOrMovie(movies.getQuick(i));
                	   rating 	 = MMh.getRating(uid, tempMid);
                	   
                	   //Get a training feature for this movie
           		      FeaturesTrainClass = getFeaturesAgainstASlot("Train",  type, tempMid);
                      if (FeaturesTrainClass !=null) {
                   	   									sizeTrainClass = FeaturesTrainClass.size();
                   	   									sizesOfTrainInASlots [t] = sizeTrainClass;
                      									}
         
       	               if(isDebug && sizeTrainClass ==0)
       	        	   {
       	            	   System.out.println(" feature train size for type= " + type + ". and movie =" + mid + " is -->"+ sizeTrainClass);
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
       	              	     String word 	 = (String)words.getKey();			     // Get a word from the train class
       	        			 double word_TF1 =  FeaturesTrainClass.get(word);		 // Get its TF
       	        			 
       	        			 //If word is there
       	        			 if(AllFeaturesInASlot[classIndex-1].containsKey(word)) // get the TF count and add it in newly TF 
       	        			 {
       	        				 double word_TF2 = AllFeaturesInASlot[classIndex-1].get(word);
       	        				 AllFeaturesInASlot[classIndex-1].put(word, word_TF1 + word_TF2); 
       	        			 }
       	        			 
       	        			 else // simply put the word, with its count
       	        			 {
       	        				 AllFeaturesInASlot[classIndex-1].put(word, word_TF1 );
       	        			 }
       	        			 
       	              	}
                	     } //end of if  
                	   } //end of finding all features against a type for all classes 
                	            	    	   
                //---------------------------------------------------------        
                // Get the common keywords, for each class in a certain slot
                // in the training set with the test set
                //---------------------------------------------------------
                	 
       	        double vocSize =0;         	 		//set of all distinct words in a slot
                	for (int m =0;m<classes;m++)
                	{
                		vocSize += AllFeaturesInASlot[m].size();
                	}
       	         
              for (int m =0;m<classes;m++)
              {
                 if(sizeTestMovie!=0 && AllFeaturesInASlot[m].size()!=0)
                  {  
                		  //Get entry sets for both vectors (test movie and train class)
                   	  Set setTestMovie = FeaturesTestMovie.entrySet();	    	      	       	  
                   	  Set setTrainClass = AllFeaturesInASlot[m].entrySet();
                     	  
                   	  Iterator jTestMovie  = setTestMovie.iterator();
                   	  Iterator jTrainClass = setTrainClass.iterator();
                     	  
                   	  int commonFeaturesLoopIndex =0;
                   	  
                   	  //Iterate over the words of Test set until one of them finishes
       	              	while(jTestMovie.hasNext()) 
       	              	 {
       	              	     Map.Entry words = (Map.Entry)jTestMovie.next();         // Next 		 
       	              	     String word 	 = (String)words.getKey();			     // Get a word from the train class
       	
       	              	     //If the Train set contain that word
       	              	    if(AllFeaturesInASlot[m].containsKey(word))
       	              	    {	
       	              	    		commonFeaturesLoopIndex ++;
       	              	    		double testWord_TF =  FeaturesTestMovie.get(word); 	  
       	              	   	
       	              	    		//-----------------
       	              	    		// Add Numerator
       	              	    		//-----------------
       	              	    	
       	                	 		 //Get frequency count for the feature
       	                			 double w1 = AllFeaturesInASlot[m].get(word);
       	                			 
       	                			 //Add it in the respective class Numerator 
       	                			 Double N =w1;     			 	           			 
       			              	    	              	    
       			              	    //-----------------
       		          	    		// Add Denomenator
       		          	    		//-----------------
       		          	    	
       			            		 //Get frequency count for the feature (All distinct words in that a slot, and in a class)
       			              	     double w2 = findSizeOfVector(AllFeaturesInASlot[m]);
       		           			 
       			              	     //Add it in the respective class Numerator 
       			              	     double D =w2;     
       			              	    
       			              	     //Add Pseudocounts if zeros
       			              	     
       			              	     //-------------------------------------------------------
       			          	    	 // Get likelihood for a word in a slot in a certain class
       			              	     //-------------------------------------------------------
       			          	    	 
       			              	     //Multiply each words likelihood for each slot into that class likelihood
       			              	     
       			              	    if(isLaplace)
       			              	    {
       				              	     likelihoodNum[m]= N * (1.0/moviesSize);
       				              	     if (vocSize!=0) likelihoodDen[m]= D * (vocSize *1.0/moviesSize);
       				              	     else  likelihoodDen[m]= D * (1.0/moviesSize);
       				              	     
       					              	 if(commonFeaturesLoopIndex==1)       					              	    	 
	       					              	 {
	       					              		 //	evidence = likelihoodIndividual[m] = (likelihoodNum[m]/likelihoodDen[m]);
	       					              	 }
	       					              		 	       					              	    	
       					              	  else {
       					              		  		likelihoodIndividual[m] =  likelihoodIndividual[m] * (likelihoodNum[m]/likelihoodDen[m]);
       					              		  	//	evidence *= (likelihoodNum[m]/likelihoodDen[m]); 
       					              		   }
       					              	    
       			              	    }
       			          
       			              	    else  if(isLog)
	   			              	    {
	   				              	     likelihoodNum[m]= N * (1.0/moviesSize);
	   				              	     if (vocSize!=0) likelihoodDen[m]= D * (vocSize *1.0/moviesSize);
	   				              	     else  likelihoodDen[m]= D * (1.0/moviesSize);
	   				              	     
	   					              	 if(commonFeaturesLoopIndex==1)       					              	    	 
		   					              	 {
		   					              		 likelihoodIndividual[m] = testWord_TF * Math.log10(likelihoodNum[m]/likelihoodDen[m]);
		   					              	 }
	   					              		 	       					              	    	
	   					              	  else {
	   					              		  		likelihoodIndividual[m]+=  testWord_TF *( Math.log10(likelihoodNum[m]/likelihoodDen[m]));
	   					             // 		  		evidence *= Math.log10(likelihoodNum[m]/likelihoodDen[m]); 
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
       				              	       else  likelihoodIndividual[m] +=EPSILON;
       				              	  */ 
       				              	    	
       				              	    	likelihoodIndividual[m] = (likelihoodNum[m] + pCount)/(likelihoodDen[m] + pCount +4);	
       				              	     }
       				              	  
       				              	    else //NEXT TIME (MULTIPLY)		
       				              		   likelihoodIndividual[m] =  likelihoodIndividual[m] * likelihoodNum[m]/likelihoodDen[m];
       			              	    }
       			              	             	   
       	              	    } //common words           			 
       	              	 }//end of while
       	              	 
       	              	// No common word is found between test and train docs
       	              	if(commonFeaturesLoopIndex==0)
       	              	{	       
       	              		
       	              		
       	              		if(isLaplace)
       	              		{
       		              		 likelihoodNum[m]=  (1.0/moviesSize);
       		              		 if(vocSize!=0)  {
       		              			 				likelihoodDen[m]=  (vocSize *1.0/moviesSize); //vocSize!=0
       		              		 	//				evidence = (vocSize *1.0/moviesSize);
       		              		 				  }	
       		              		 else   likelihoodDen[m]=  (1.0/moviesSize);
       		         	    	
       		              		 likelihoodIndividual[m] = (likelihoodNum[m]/likelihoodDen[m]);
       		         	    	 //evidence = (likelihoodNum[m]/likelihoodDen[m]);
       		         	    	        		         	    	 
       	              		}
       	              		
       	              		else if(isLog)
	   	              		{
	   		              		 likelihoodNum[m]=  (1.0/moviesSize);
	   		              		 if(vocSize!=0)  {
	   		              			 				likelihoodDen[m]=  (vocSize *1.0/moviesSize); //vocSize!=0
	   		              		 	//				evidence = (vocSize *1.0/moviesSize);
	   		              		 				  }	
	   		              		 
	   		              		 else   likelihoodDen[m] = (1.0/moviesSize);
	   		         	    	
	   		              		 likelihoodIndividual[m] = Math.log10(likelihoodNum[m]/likelihoodDen[m]);
	   		         	    	// evidence = Math.log10(likelihoodNum[m]/likelihoodDen[m]);
	   		         	    	        		         	    	 
	   	              		}
       	              		else
       	              		{              	    	
       	              	        //likelihoodIndividual[m] +=EPSILON;
       	              	        likelihoodIndividual[m] = (likelihoodNum[m] + pCount)/(likelihoodDen[m] + pCount +4);
       	              		
       	              		}
       	              	}
       	              	
       	              	if(commonFeaturesLoopIndex<10)
       	              	{
       	              		noCommonFeatureFound =true; 
       	              		noCommonality ++;
       	              	}
       	              	
                        } //end of if size >0

                 	 // One of the doc (test or train) or both of them have zero sizes. 
        		     else	//overcome the zero probabilities 
       		          {		        	     
       		        	  if(isLaplace) // but may be the vocabulary is zero
       	              		{
       		              		 likelihoodNum[m]=  (1.0/moviesSize);
       		              		 if(vocSize!=0)  likelihoodDen[m]=  (vocSize *1.0/moviesSize); //vocSize!=0
       		              		 else   likelihoodDen[m]=  (1.0/moviesSize);	              	     
       		             
       		              		 likelihoodIndividual[m] = (likelihoodNum[m]/likelihoodDen[m]);
   		         	    	   //  evidence = (likelihoodNum[m]/likelihoodDen[m]);
   		         	    
       	              		}
       	            
       		        	  else  if(isLog) 
     	              		{
     		              		 likelihoodNum[m]=  (1.0/moviesSize);
     		              		 if(vocSize!=0)  likelihoodDen[m]=  (vocSize *1.0/moviesSize); //vocSize!=0
     		              		 else   likelihoodDen[m]=  (1.0/moviesSize);	              	     
     		             
     		              		 likelihoodIndividual[m] = Math.log10(likelihoodNum[m]/likelihoodDen[m]);
 		         	    	     //evidence = Math.log10(likelihoodNum[m]/likelihoodDen[m]);
 		         	    
     	              		}
       	              		else
       	              		{
       	              		    //likelihoodIndividual[m] +=EPSILON;  // add a small probability
       	              			likelihoodIndividual[m] = (likelihoodNum[m] + pCount)/(likelihoodDen[m] + pCount +4);
       	              		}
       		        	 }
                	   }//end of for
           
                //-----------------------------
                // Mult likelihood && re-init
                //-----------------------------
                 
                //Multiply likelihood obtained for a slot
                for (int k=0;k<classes;k++)
                {
               	 
               	 //actual wrong thing is starting here
               	   if (likelihoodIndividual[k]==0)
               	   {
               		   if(isLaplace)   //But it should not be the case (only if voc is zero)
               		   {
       	        	  	     likelihoodNum[k]=  (1.0/moviesSize);
       	              		 if(vocSize!=0)  likelihoodDen[k]=  (vocSize *1.0/moviesSize); //vocSize!=0
       	              		 else   likelihoodDen[k]=  (1.0/moviesSize);	              	     
       	         	    
       	              		 likelihoodIndividual[k] = (likelihoodNum[k]/likelihoodDen[k]);
		         	    //	 evidence = (likelihoodNum[k]/likelihoodDen[k]);
		         	    
                	    }
                	    

               		   else if(isLog) 
               		   {
       	        	  	     likelihoodNum[k]=  (1.0/moviesSize);
       	              		 if(vocSize!=0)  likelihoodDen[k]=  (vocSize *1.0/moviesSize); //vocSize!=0
       	              		 else   likelihoodDen[k]=  (1.0/moviesSize);	              	     
       	         	    
       	              		 likelihoodIndividual[k] = Math.log10(likelihoodNum[k]/likelihoodDen[k]);
		         	    	// evidence = Math.log10(likelihoodNum[k]/likelihoodDen[k]);
		         	    
                	    }
               		   else
               		   {	        		                   	     
       	        	    	 //likelihoodIndividual[k] += EPSILON;
       	        	    	 likelihoodIndividual[k] = (likelihoodNum[k] + pCount)/(likelihoodDen[k] + pCount +4);
               		   }
               	   }
               	
               	 // multiply prior and likelihood and send back
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
		               		  evidence =  likelihood[k] * likelihoodIndividual[k];
		               	  }
           		   
              	   } 
              	
               	
                }
                
                //Initialise the likelihoods
                for(int k=0;k<classes; k++)
                {
       	       	  likelihoodIndividual [k] 	= 0.0;		//shoULD BE 0
       	       	  likelihoodNum [k] 		= 0.0;
       	       	  likelihoodDen [k] 		= 0.0;
       	       	  AllFeaturesInASlot[k].clear();		//clear the features in a slot
       	       	  
                }
                        
              }//end of type for
              	 
                boolean isNullAll = true;
                boolean isNullTest = true;
                boolean isNullTrain = true;
                
                //check for nulls in all slots
                for (int t=0;t<typeToExtract.size();t++)
                {
               	 //for both to be zero for repective slots
               	 if (!(sizesOfTestInASlots [t] == 0 && sizesOfTrainInASlots [t]==0))
               		 isNullAll = false; 
               	 
               	 //If one of the slot was not empty for test set, flag become flase
               	 if (!(sizesOfTestInASlots [t] <= 5)) isNullTest = false; 
               	 
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
         
          //return the likelihood for each class
          return likelihood;    	 
        
        }
            

  //----------------------------------------------------------------------------------------------------
        /**
         * Return features stored against a slot
         */
               
     public HashMap<String,Double> getFeaturesAgainstASlot(String whichObj, int type, int mid)
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
          		return FeaturesTrainClass;
       }


  /***************************************************************************************************/
 
     /**
      * Return the class with the Max(Priors, Likelihood)
      * @param priors, priors for each class
      * @param likelihood, likelihood for each class
      * @param class
      */
     
     public double getMaxClass (int uid, int mid, double priors[], double likelihood[], 
    		 					int classes, double thresholdProb, double thresholdRat)
     {
    	 
    	OpenIntDoubleHashMap results = new OpenIntDoubleHashMap();	// from class,prior*likelihood
    	OpenIntDoubleHashMap mySimPriors = myFilter.getPriorWeights(3, uid, mid);	//from simple CF priors
    	OpenIntDoubleHashMap finalResults = new OpenIntDoubleHashMap();	// addition 
    	 
       //First add results into an array 
       for(int i=0;i<classes; i++)
       {
    	   // add combined weight
    	  //  results.put(i+1, priors[i] * likelihood[i]/evidence);		//class = index+1
    	    results.put(i+1,  likelihood[i]);							//class = index+1
    	   //   results.put(i+1,  priors[i]);							//class = index+1
    	   // results.put(i+1,  likelihood[i] * priors[i]);				//class = index+1
    	   
    	   // count the cases where the probs are zeros 
    	   if (priors[i] * likelihood[i] ==0)
      		   		totalResultsUnAnswered++;
    	   
    	   //add both cases (NB + sim Priors)
    	   //finalResults.put(i+1, results.get(i) + mySimPriors.get(i));
    	//   finalResults.put(i+1, results.get(i) * mySimPriors.get(i));
    	   
       }
       
       
       //---------------------------------------------------
       //Go through all the classes and find the max of them
       //---------------------------------------------------
       
       //Add tied cases into it
       IntArrayList tieCases = new IntArrayList();	//Max tie cases will be equal to the no of classes
       
       //Sort the array into ascending order
       IntArrayList myKeys 		= results.keys(); 
       DoubleArrayList myVals 	= results.values();       
       results.pairsSortedByValue(myKeys, myVals);
          
       //print probs
       for (int i=0;i<myVals.size();i++)
    	   { 
    	     //System.out.print(myVals.get(i)+", ");
    	   
    	   }

       // System.out.println("\n Sum = "+ sum);
       // System.out.println();
       
       sum =0;
       for (int i=0;i<myVals.size();i++)
    	   {
    	   		sum += myVals.get(i)/evidence;
    	   		//System.out.print(myVals.get(i)/evidence +", ");
    	   }
       
/*       System.out.println("\n Sum = "+ sum);
       System.out.println("\n--------------------");
*/       
       //last index should have the highest value
       boolean tieFlag =false;
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
       //Determine the winner index
       //---------------------------
       if (tieFlag == true)
    	   totalTieCases++;
       
       // By Default it should be the last index  in the array and its key corresponds to the class
        double winnerIdx = (double) myKeys.get(classes-1);
       
   /*    //But if tie, then do random break
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
       

        // if(3>2) return winnerIdx;
        
        //------------------------------------
        // CF
        //------------------------------------
        
       double rating_CF	= myFilter.recommendS(uid, mid, 80, 1); //uid, mid, neighbours, version       
       
      // if(3>2) return rating_CF + 0;		//added pseudo-count (0.2 etc)
       
       //--------------
       //Learn priors
       //--------------
       
       //Sim priors sorting
       IntArrayList mySimKeys 		= mySimPriors.keys(); 
       DoubleArrayList mySimVals 	= mySimPriors.values();       
       results.pairsSortedByValue(mySimKeys, mySimVals);
       
       //final answer sorting
       IntArrayList myFinalKeys 		= mySimPriors.keys(); 
       DoubleArrayList myFinalVals 		= mySimPriors.values();       
       results.pairsSortedByValue(myFinalKeys, myFinalVals);
       
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
	    		   if(myFinalVals.get(classes-1) == myFinalVals.get(i-1)) 
	    		   {
	    			 finalTieCases.add(myFinalKeys.get(i-1));		//This index contains value as that of highest result
	    			 finalTieFlag =true;
	    		   }
	    		   
	    	   }
	       } //end of for 
	    	
	    	   
	  		//-------------------------------------
			// Learn some confidence function
	      	// L(0) - L(1)
			//-------------------------------------
	      
	/*      	//Max value 
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
     	// L(0) - L(1)
		//-------------------------------------
     
     	//Max value 
     	double L0 = myVals.getQuick(classes-1);
     
     	// If not tie cases
     	 if(tieFlag ==false)
     	  {
     		 double diff = L0- myVals.getQuick(classes-2);
     		 
     		 if(diff>thresholdProb)
     			 {
     			 		double ans =	(double)myKeys.getQuick(classes-1);
     			 		
     			 		return ans;
     			 		/*if(Math.abs(ans-rating_CF) <0.60)
     			 			return ans;
     			 		
     			 		else return rating_CF;*/
     			 }
     	   }
     	 
     	 //else return CF rating
     	 return rating_CF;

       
       
       
       
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
         double myResult = getMaxClass (uid, mid, myPrior, myLikelihood, myClasses, 0.4, 0.4);
        	 
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
 		
         // For each user (in test set), make recommendations
         IntArrayList users = MTestMh.getListOfUsers(); 		        
         LongArrayList movies;
         double rating;
         int uid, mid;
         double thresholdProb = 0.0;
         double thresholdRat  = 0.0;
         
   for(int mainLoop = 0;mainLoop<10;mainLoop++ )
    {
	  thresholdProb += 0.01;	 
	 
	  for(int innerLoop = 0; innerLoop<15;innerLoop++ )
	  {
	    thresholdRat += 0.05;
	    
         for (int i = 0; i < users.size(); i++)        
         {
        	 
             uid = users.getQuick(i);          
             movies = MTestMh.getMoviesSeenByUser(uid); //get movies seen by this user
                          
            
             	moviesSize = movies.size();
	          
	             for (int j = 0; j < moviesSize; j++)            
	 	           {
	 	             //get Movie   
	            	 mid = MemHelper.parseUserOrMovie(movies.getQuick(j));                
	 	              
	            	 //get class priors
	            	 double myPrior[] = getPrior(uid, myClasses);
	 	                
	            	//get class Likelihood
	            	 double myLikelihood[] = getLikelihood(uid, mid, myClasses); //uid, mid, classes
	 	             
	            	 //get result
	            	 double myResult = getMaxClass (uid, mid, myPrior, myLikelihood, myClasses, thresholdProb, thresholdRat);
	            	 
	            	 //add error
	 	             double myActual = getAndAddError(myResult, uid, mid, myClasses);
	 	            //double myActual = getExpectedError(myResult, uid, mid);
	 	             
	 	            
	 	              //get Extreme errors and correct answers 	            
	 	              getExtremeErrorCount(myResult, myActual);
		 	        
	 	              // error
	 	              double ErrorFound = Math.abs(myActual - myResult);
	 	             /* System.out.println("Currently at user = "+ i +", error = actual - predicted ="
		 	            		+ ErrorFound + ", " + myActual+ ", "+ myResult);
		 	      
	 	            */
	 	                
	 	         } //end of all movies            
          } //end processing all users

	         //printError etc
	         printError(thresholdProb, thresholdRat);
	  }//end of inner loop
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
        // System.out.println("ROC Specificity --:" 		+ rmse.getSpecificity());
         
         System.out.println("Correctly Predicted --:"   + correctlyPredicted);
         System.out.println("% of correct --:"          + (correctlyPredicted * 100.0) / (totalPredicted));
         System.out.println("% of Error (>0 && <1) --:" + (extremeError1 * 100.0) / (totalPredicted));
         System.out.println("% of Error (>1 && <2) --:" + (extremeError2 * 100.0) / (totalPredicted));
         System.out.println("% of Error (>2 && <3) --:" + (extremeError3 * 100.0) / (totalPredicted));
         System.out.println("% of Error (>3 && <4) --:" + (extremeError4 * 100.0) / (totalPredicted));
         System.out.println("% of Error (>4) --:" 		+ (extremeError5 * 100.0) / (totalPredicted));
         
         /*//Print Extreme Error for individual user
         System.out.println("Error >=4 " + extremeError5); 
         System.out.println("Error >=3 " + extremeError4);
         System.out.println("Error >=2 " + extremeError3);
         System.out.println("Error >=1 " + extremeError2);
         System.out.println("Error >=0 " + extremeError1);
         */
         
       /*  System.out.println("Tie Cases " + totalTieCases);
         System.out.println("ZEROProb " + totalResultsUnAnswered);
         */
        // Here, we can re-set values in the class RMSE and other local variable
         rmse.resetValues();
         extremeError1 = extremeError2 = extremeError3 = extremeError4= extremeError5= 0;
         totalTieCases = 0;
         totalResultsUnAnswered = 0;
      
          
    }//end of function
 	
/****************************************************************************************************/

 	public double getAndAddError(double rating, int uid, int mid, int classes)	
 	{
         

 	   double actual = MTestMh.getRating(uid, mid);	//get actual rating against these uid and movieids      
       rmse.add	(actual, rating);					//add (actual rating, Predicted rating)      
       rmse.addCoverage(rating);					//Add coverage
       rmse.ROC4(actual, rating, classes,MMh.getAverageRatingForUser(uid));			//Add ROC
       return actual;

 	}
 	
/****************************************************************************************************/

 	public double getExpectedError(double rating, int uid, int mid, int classes)	
 	{
         

 	   double actual = MTestMh.getRating(uid, mid);	//get actual rating against these uid and movieids      
     
 	   if(actual - rating == 0) 
 		   	rmse.add	(actual, rating);					//add (actual rating, Predicted rating)      
       
 	   else 
 		  rmse.add	    (actual, MMh.getAverageRatingForUser(uid));					//add (actual rating, Predicted rating)

 	   
       return actual;

 	}
/****************************************************************************************************/
/**
 * Count how much cases have extreme error
 */ 	
 	public void getExtremeErrorCount(double predicted, double actual)
 	{
 		
 		double error = Math.abs(predicted-actual);
 		
 		 // correct answer and percentage
 		if (error==0) correctlyPredicted ++;
 		
 		// total 
 		 totalPredicted++;
 		
 		// extreme errors
 		
 		if (error <1)
 		{
 			extremeError1++; 			
 		}
 		
 		else if (error >=1 && error <2)
 		{
 			extremeError2++; 			
 		}
 		
 		else if (error >=2 && error <3)
 		{
 			extremeError3++; 			
 		}
 		
 		else if (error >=3 && error <4)
 		{
 			extremeError4++; 			
 		}
 		
 		else if (error >=4 && error <5)
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

      String test  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\sml_clusteringTestSetStoredTF.dat";
	  String train  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\sml_clusteringTrainSetStoredTF.dat";

	  NaiveBayesRecHybrid myNB = new NaiveBayesRecHybrid(train, test);
    	
	  myNB.makePrediction();
    	
    }
    
	
}
