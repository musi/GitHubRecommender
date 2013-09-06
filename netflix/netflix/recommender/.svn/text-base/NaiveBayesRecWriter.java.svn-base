package netflix.recommender;



import java.io.BufferedWriter;
import java.io.FileWriter;
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


public class NaiveBayesRecWriter 
{

    /** Flag to set Laplace smoothing when estimating probabilities */
    boolean isLaplace = false;

    /** Small value to be used instead of 0 in probabilities, if Laplace smoothing is not used */    
    double EPSILON = 1e-6; 

    /** Flag to debug */
    boolean isDebug   = false;
    
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
	MemHelper 		MMh;						// train set
    MemHelper 		MTestMh;					// test set	
    MemHelper 		mainMh;						// contain all movies
    
    //Start up RMSE count
    RMSECalculator rmse;

    //rand object
    Random rand;
    
    //types to be extracted 
	IntArrayList typeToExtract;
	
    //Classes
    int myClasses;
    
    //Extreme errors
    int extremeError5;
    int extremeError4;
    int extremeError3;
    int extremeError2;
    int extremeError1;
    
    //Tie Cases
    int totalTieCases;
    
    //Prior*likelihood = psedu count
    int totalResultsUnAnswered;
    int totalZeroPriors;
    int totalZeroLikelihood;
    
    //For writing files
    String myPath;
    BufferedWriter TrWriter;
	
/**********************************************************************************************/	
/**
 * Constructor
 * @param train Object
 * @param test Object
 */
    
    public  NaiveBayesRecWriter (String mainObject, String trainObject, String testObject)
    {
    
    	//Get test and train objects
    	mainMh		= new MemHelper (mainObject);
    	MMh			= new MemHelper (trainObject);
	    MTestMh 	= new MemHelper (testObject);
	    
	    //Random object
		rand = new Random();    
		
		//For MAE
		rmse = new RMSECalculator();
		
		//assign how much classes, we want
	    myClasses = 5; 						// {5,10} for{ML,FT}	
	           
	    //Just to check for how many cases, it is unsuccessful extremely
	    extremeError1 = extremeError2 = extremeError3 = extremeError4 = extremeError5 = 0;
	 
	    // For checking how much tie cases occured
	    totalTieCases = 0;
	    
	    //Count how much of the prior*likelihood are not contributing anything
	    totalResultsUnAnswered 	= 0;
	    totalZeroPriors 		= 0;
	    totalZeroLikelihood 	= 0;
	    
		//Type
		typeToExtract = new IntArrayList();
		typeToExtract.add(10);
		typeToExtract.add(11);
		typeToExtract.add(4);
		typeToExtract.add(2);
		typeToExtract.add(100);
		typeToExtract.add(101);
		typeToExtract.add(94);
		typeToExtract.add(19);
		typeToExtract.add(5);
		typeToExtract.add(98);
		
	    //paths
	     myPath =  "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\"; 
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
                		 priors[i] = Math.log((priors[i]+1)/(classes + moviesSize));
                	}
                	
                	//Add Psudo count
                	else
                	{
                		double num = priors[i] + (1.0)/moviesSize;
                		double den = priors[i] + ((classes*1.0)/moviesSize);
                		
	                	/*if(priors[i]==0) 
	                		priors[i] = priors[i] + PseudoCount;
	                	*/
	                	
	                	priors[i] = num/den;
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
         HashMap<String,Double> FeaturesTrainClass = null; 
         
         //Get Features for the Movie we want to predict
         FeaturesTestMovie   = MMh.getFeaturesAgainstAMovie(mid);
         if(FeaturesTestMovie == null)
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
         	   FeaturesTrainClass  = MMh.getFeaturesAgainstAMovie(mid);
           	   if (FeaturesTrainClass == null)           	   
           	   FeaturesTrainClass  = MTestMh.getFeaturesAgainstAMovie(mid);
           	   
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
	        		 likelihoodDen[i] = likelihoodDen[i]+EPSILON;
	        	 
	        	 if(likelihoodNum[i]==0)
	        		 likelihoodNum[i] = likelihoodDen[i]+EPSILON*2;
	        	 
	        	   likelihood[i] = likelihoodNum[i]  /likelihoodDen[i];	//pesudo count =1;        	
        	 }
        	 
         }
             	     	 
         //return the likelihood for each class
    	 return likelihood;    	 
 
     }
     
/*            public double[] getLikelihood (int uid, int mid, int classes)
            {
           	 // Features stored in the database    	 
           	 HashMap<String,Double> FeaturesTestMovie  = null; 
                HashMap<String,Double> FeaturesTrainClass = null; 
                      
        	    
        	    //Local variables
                LongArrayList movies;
                double rating 	 = 0.0;
                int moviesSize  = 0;
                int tempMid	 = 0;
                
                double likelihood[] 			= new double [classes];
                double likelihoodIndividual[] 	= new double [classes];
                double likelihoodNum[] 		= new double [classes];
                double likelihoodDen[] 		= new double [classes];
                
                //Initialise the likelihoods
                 for(int i=0;i<classes; i++)
                 {
               	  likelihood [i] 			= 1.0;			//we have to multiply
               	  likelihoodIndividual [i] 	= 1.0;
               	  likelihoodNum [i] 		= 0.0;			//we have to add pseudo counts
               	  likelihoodDen [i] 		= 0.0;
               	  
                 }
                
                //-----------------------------------------------------
        		 //Get all movies seen by this user from the training set               
                //-----------------------------------------------------
                
                movies = MMh.getMoviesSeenByUser(uid); 
                moviesSize = movies.size();
                

          	   //-----------------------------
          	   // Get features for test movie
          	   // and train set
          	   //-----------------------------
          	   
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
           		   
       	    		   if(isDebug)
       	        	   {
       	        		   System.out.println(" type = " + type);
       	        	   }
           		   
           		   //Get a test feature for this movie
           		   FeaturesTestMovie   = getFeaturesAgainstASlot("Test",  type, mid);    	               	         
                      if (FeaturesTestMovie !=null) 
                   	   sizeTestMovie  = FeaturesTestMovie.size();              
               	   
       	         
       	        	   if(isDebug)
       	        	   {
       	        		   System.out.println(" feature test size for " + type + " =" + sizeTestMovie);
       	        	   }
               	   
       	         //For all movies in training set                
       	         for (int i=0;i<moviesSize;i++)
       	         {
       	        	//define and reset varaibales for each train movie
       	        	sizeTrainClass =0;
       	        	 
       	        	//Get a movie seen by the user
                	   tempMid 	 = MemHelper.parseUserOrMovie(movies.getQuick(i));
                	   rating 	 = MMh.getRating(uid, tempMid);
                	   
                	   //Get a training feature for this movie
           		   FeaturesTrainClass = getFeaturesAgainstASlot("Train",  type, tempMid);
                      if (FeaturesTrainClass !=null) sizeTrainClass = FeaturesTrainClass.size();
                     
       	               if(isDebug)
       	        	   {
       	        		   System.out.println(" feature train size for " + type + " =" + sizeTrainClass);
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
                	   } //end of finding all features against a type 
                	            	    	   
                //---------------------------------------------------------        
                //Get the common keywords, for each class in a certain slot
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
       	                			 double w1 = AllFeaturesInASlot[m].get(word);
       	                			 
       	                			 //Add it in the respective class Numerator 
       	                			 Double N =w1;     			 	                			 
       	                			 
       			              	    	              	    
       			              	    //-----------------
       		          	    		// Add Denomenator
       		          	    		//-----------------
       		          	    	
       			            		 //Get frequency count for the feature (All distinct words in that a slot, and in a class)
       			              	     double w2 = AllFeaturesInASlot[m].size();
       		           			 
       			              	     //Add it in the respective class Numerator 
       			              	     double D =w2;     
       			              	    
       			              	     //Add Pseudocounts if zeros
       			              	     
       			              	     //---------------------
       			          	    	 // Get likelihood for a
       			              	     // word in a slot in a 
       			              	     // certain class
       			          	    	 //---------------------
       			          	    	 
       			              	     //Multiply each words likelihood for each slot into that class likelihood
       			              	     
       			              	     likelihoodNum[m]= N * (1.0/moviesSize);
       			              	     likelihoodDen[m]= D * (vocSize *1.0/moviesSize);
       			              	   
       			              	     likelihoodIndividual[m] =
       			              	    	 likelihoodIndividual[m] * likelihoodNum[m]/likelihoodDen[m];
       			              	     
       			              	            	   
       	              	    } //common words           			 
       	              	 }//end of while
                        } //end of if size >0
                	   }//end of for
           
                //-----------------------------
                // Mult likelihood && re-init
                //-----------------------------
                 
                //Multiply likelihood obtained for a slot
                for (int k=0;k<classes;k++)
                {
               	 if (likelihoodIndividual[k]==0) likelihoodIndividual[k]= 1;
               	 likelihood[k] =  likelihood[k] * likelihoodIndividual[k];
                }
                
                //Initialise the likelihoods
                for(int k=0;k<classes; k++)
                {
       	       	  likelihoodIndividual [k] 	= 1.0;		//should be 1
       	       	  likelihoodNum [k] 		= 0.0;
       	       	  likelihoodDen [k] 		= 0.0;
       	       	  AllFeaturesInASlot[k].clear();		//clear the features in a slot
       	       	  
                }
               
                
              }//end of type for
                 	 
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
       		  case 2: 	FeaturesTrainClass = myObj.getColorsAgainstAMovie(mid);  		break;
       		  case 4:	FeaturesTrainClass = myObj.getLanguageAgainstAMovie(mid); 		break;
       		  case 5:	FeaturesTrainClass = myObj.getCertificateAgainstAMovie(mid);	break;
       		  case 9:	FeaturesTrainClass = myObj.getTagsAgainstAMovie(mid); 			break;
       		  case 10:	FeaturesTrainClass = myObj.getKeywordsAgainstAMovie(mid); 		break;
       		  case 19:	FeaturesTrainClass = myObj.getBiographyAgainstAMovie(mid); 		break;
       		  case 94:	FeaturesTrainClass = myObj.getPrintedReviewsAgainstAMovie(mid); break;
       		  case 98:	FeaturesTrainClass = myObj.getPlotsAgainstAMovie(mid); 			break;
       		  case 100:	FeaturesTrainClass = myObj.getVotesAgainstAMovie(mid); 			break;
       		  case 101:	FeaturesTrainClass = myObj.getRatingsAgainstAMovie(mid); 		break;
       		  default:  																break; 
       			
       		}
       		
       		return FeaturesTrainClass;
            }
            
 /***************************************************************************************************/
                 
            
            
 /***************************************************************************************************/
 
     /**
      * Return the class with the Max(Priors, Likelihood)
      * @param priors, priors for each class
      * @param likelihood, likelihood for each class
      * @param class
      */
     
     public double getMaxClass (double priors[], double likelihood[], int classes)
     {
    	 
    	double myClass = 0; 
        OpenIntDoubleHashMap results = new OpenIntDoubleHashMap();	//from class,prior*likelihood
    	
        
       //First add results into an array 
       for(int i=0;i<classes; i++)
       {
    	   // add combined weight
    	   results.put(i+1, priors[i] * likelihood[i]);		//class = index+1
    	   
    	   // count the cases where the probs are zeros 
    	   if (priors[i] * likelihood[i] ==0)
      		   		totalResultsUnAnswered++;
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
              
       //last index should have the highest value
       boolean tieFlag =false;
       for(int i=classes-1;i>=0;i--)
       {
    	   if(i>0)
    	   {
    		   if(results.get(classes-1) == results.get(i-1)) 
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
       int winnerIdx = myKeys.get(classes-1);
       
       //But if tie, then do random break
   /*    if(tieCases.size()>0)
       {
	       //Break the ties through random return
	       int randIdx   = rand.nextInt(tieCases.size());
	       winnerIdx     = tieCases.get(randIdx); 
	    	   
	   }*/
   
       
       //return the result
       return (double)winnerIdx;
   
       
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
         double myResult = getMaxClass (myPrior, myLikelihood, myClasses);
        	 
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
 		
         // For each user (in test set), make recommendations
         IntArrayList users = MTestMh.getListOfUsers(); 		        
         LongArrayList movies;
         double rating;
         int uid, mid;
            
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
            	 double myResult = getMaxClass (myPrior, myLikelihood, myClasses);
            	 
            	 //add error
 	            double myActual = getAndAddError(myResult, uid, mid);	                
 	            
 	            //get Extreme errors
 	            
 	            getExtremeErrorCount(myResult, myActual);
 	            System.out.println("Currently at user = "+ i +", error = actual - predicted ="
 	            		+ Math.abs(myActual - myResult) + ", " + myActual+ ", "+ myResult);
 	            
 	                
 	         }
            
         } //end processing all users

         
          
         //Print Error
         System.out.println();
         System.out.println("Final RMSE --:" 			+ rmse.rmse());
         System.out.println("Final MAE --:" 			+ rmse.mae());
         System.out.println("Final Coverage --:" 		+ rmse.getItemCoverage());

         //Print Extreme Error for individual user
         System.out.println("Error >=5 " + extremeError5); 
         System.out.println("Error >=4 " + extremeError4);
         System.out.println("Error >=3 " + extremeError3);
         System.out.println("Error >=2 " + extremeError2);
         System.out.println("Error >=1 " + extremeError1);
         
         
         System.out.println("Tie Cases " + totalTieCases);
         System.out.println("ZEROProb " + totalResultsUnAnswered);
         
        // Here, we can re-set values in the class RMSE and other local variable
         rmse.resetValues();
         extremeError1 = extremeError2 = extremeError3 = extremeError4= extremeError5= 0;
         totalTieCases = 0;
         totalResultsUnAnswered = 0;
      
          
 }//end of function
 	
/****************************************************************************************************/

    /**
     * Start recommending 
     */  
 	
     public void fillMatrix(int totalMovies, String filledTrainPath)	
  	{
  		System.out.println("Come to make prediction");
  		int moviesSizeTest  = 0;
  		int moviesSizeTrain = 0;
  		
  		//For writing the results into a file  		
  		IntArrayList myUsers    	 = new IntArrayList();
  		IntArrayList myMovies  		 = new IntArrayList();
  		DoubleArrayList myRatings    = new DoubleArrayList();
  		
  		  		
          // For each user, make recommendations
          IntArrayList users = MMh.getListOfUsers(); 		        
          LongArrayList moviesInTrainSet;
          LongArrayList moviesInTestSet;
          int uid, mid;
             
          for (int i = 0; i < users.size(); i++)        
          {
         	 
              uid = users.getQuick(i);          
              
              //--------------------
              //Get Test Set Movies
              //--------------------
              
              moviesInTestSet = MTestMh.getMoviesSeenByUser(uid); //get movies seen by this user      
              moviesSizeTest = moviesInTestSet.size();          
              IntArrayList testMovies = new IntArrayList();
              
              
              for (int j = 0; j < moviesSizeTest; j++)            
	           {
	             //get Movie   
            	  mid = MemHelper.parseUserOrMovie(moviesInTestSet.getQuick(j));
            	  testMovies.add(mid);
            	  
	           }
              
              //--------------------
              //Get Train Set Movies
              //--------------------

              moviesInTrainSet = MMh.getMoviesSeenByUser(uid); //get movies seen by this user      
              moviesSizeTrain = moviesInTrainSet.size();          
              IntArrayList trainMovies = new IntArrayList();
              
              for (int j = 0; j < moviesSizeTrain; j++)            
	           {
	             //get Movie   
            	  mid = MemHelper.parseUserOrMovie(moviesInTrainSet.getQuick(j));
            	  trainMovies.add(mid);
            	  
	           }
                       
              //-----------------------------------------------------------
              // We want to fill the matrix for all the movies, but not for
              // Those in test set, Write this info as a separate memReader
              // Object which can be picked by other programs
              //-----------------------------------------------------------
              
              for (int j = 0; j < totalMovies; j++)            
  	           {
  	             //get Movie  (from 1--to 1182 in case of ML for example)  
             	 mid = j+1;                
  	              
             	 //--------------------------
             	 // Predict if this movie is
             	 // not there in test set and 
             	 // in train set
             	 //--------------------------
            
             	 // In fcat, we predict all movies, except which are there in the training set
          //   	 if( (!testMovies.contains(mid)) && (!trainMovies.contains(mid)) )
             	 if( (!trainMovies.contains(mid)) )
             	 {
	             	 //get class priors
	             	 double myPrior[] = getPrior(uid, myClasses);
	  	                
	             	//get class Likelihood
	             	 double myLikelihood[] = getLikelihood(uid, mid, myClasses); //uid, mid, classes
	  	             
	             	 //get result
	             	 double myResult = getMaxClass (myPrior, myLikelihood, myClasses);
	             	 
	             	//Add in the variables
	             	 myUsers.add(uid);
	             	 myMovies.add(mid);
	             	 myRatings.add(myResult);
             	 
	  	            
             	} //end of if
             	 
             	 
  	         } //end of all movies

              System.out.println(" Currently at user = "+ (i+1) + ", Error = "+ rmse.mae());
          } //end processing all users

                    
          //print info
          System.out.println("Tie Cases " + totalTieCases);
          System.out.println("ZEROProb " + totalResultsUnAnswered);
          
         // Here, we can re-set values in the class RMSE and other local variable
          totalTieCases = 0;
          totalResultsUnAnswered = 0;
       
         
          // Now add this info in a MemReader File
          writeResults(myUsers, myMovies,myRatings, filledTrainPath);
          
  }//end of function

/****************************************************************************************************/
     
   public void writeResults(IntArrayList users, IntArrayList movies, 
		   					DoubleArrayList ratings, String filledTrainPath)
    {
	   System.out.println("user ="+ users.size()+ ", movies= "+movies.size()+", ratings="+ratings.size());
     
       try
           {		 
    	 
    		 TrWriter = new BufferedWriter(new FileWriter(filledTrainPath));
    		 
    		 int totalSamples = ratings.size();
    		 
    		 for (int i=0;i<totalSamples;i++)
    		 {
			    int uid 	= users.get(i);
			    int mid 	= movies.get(i);
			    double rat	= ratings.get(i);
			   
    			String oneSample = (uid + ","+ mid + "," + rat) ;
    			System.out.println("i="+i + ", sample ="+ oneSample);
			    
    			TrWriter.write(oneSample);
				TrWriter.newLine();
				
    		 }
    		 
    		   //close the file
    		   TrWriter.close(); 
    		      
				
    	 }//end of try   	 
    	 
    	 catch (Exception E)
    	 {
    		 E.printStackTrace();
    		 System.out.println(" Can not create file");
    	 }
 	
     }
     
/****************************************************************************************************/

   public double getAndAddError(double rating, int uid, int mid)	
 	{
         

 	   double actual = MTestMh.getRating(uid, mid);	//get actual rating against these uid and movieids      
       rmse.add	(actual, rating);					//add (actual rating, Predicted rating)      
       rmse.addCoverage(rating);					//Add coverage

       return actual;

 	}
 	
/****************************************************************************************************/
/**
 * Count how much cases have extreme error
 */ 	
 	public void getExtremeErrorCount(double predicted, double actual)
 	{
 		
 		double error = Math.abs(predicted-actual);
 		
 		if (error >=1 && error <2)
 		{
 			extremeError1++;
 			
 		}
 		
 		if (error >=2 && error <3)
 		{
 			extremeError2++;
 			
 		}
 		if (error >=3 && error <4)
 		{
 			extremeError3++;
 			
 		}
 		if (error >=4 && error <5)
 		{
 			extremeError4++;
 			
 		}
 		
 		if (error >=5)
 		{
 			extremeError5++;
 			
 		}
 		
 	}

/****************************************************************************************************/
/****************************************************************************************************/
 	
     
 public static void main(String args[])    
 {

     //SML
/*      String test   = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\sml_clusteringTestSetStored.dat";
	  String train  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\sml_clusteringTrainSetStored.dat";
	  	  String filledTrain =  "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\sml_clusteringFilledTrainSet.dat";
	 */ 

	 
	 
	  
	 
	  String test  =  "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\sml_clusteringTestSetStoredTF.dat";
	  String train  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\sml_clusteringTrainSetStoredTF.dat";
	  String filledTrain =  "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\sml_clusteringFilledTrainSetTF.dat";
	  String mainFile  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\sml_storedFeaturesRatingsTF.dat";
	  
	  NaiveBayesRecWriter myNB = new NaiveBayesRecWriter(train, train, test);
    	
	//  myNB.makePrediction();
	  myNB.fillMatrix(1682, filledTrain);
    	
	//Write these movies
	  MemReader myReader = new MemReader();
	  myReader.writeIntoDisk(filledTrain,  myNB.myPath + "sml_clusteringFilledTrainSetStoredTF.dat");	
	  
    }
    
	
}
