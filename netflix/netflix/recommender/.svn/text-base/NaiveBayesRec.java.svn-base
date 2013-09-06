package netflix.recommender;


// Correct it, why it is not producing correct results for NB 

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

/***************************************************************************************************/

public class NaiveBayesRec 
{

    /** Flag to set Laplace smoothing when estimating probabilities */
    boolean isLaplace = true;

    /** Flag to set Log when estimating probabilities */
    boolean isLog 	  = false;

    /** Flag to debug */
    boolean isDebug   = false;
    
    /** Small value to be used instead of 0 in probabilities, if Laplace smoothing is not used */    
    int pCount   = 1; 

    /** Small value to be used instead of 0 in probabilities, if Laplace smoothing is not used */    
    double EPSILON    =   1e-6; 
    
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
    
    //correct answers;
    int correctlyPredicted;
    int totalPredicted;
    
    //Tie Cases
    int totalTieCases;
    
    // null features
    int nullTestFeatures ;
    boolean currentMovieHasNullFeatures;
    
    //No Commonality between test and train set
    boolean noCommonFeatureFound;
    int 	noCommonality;	
    
    //Prior*likelihood = psedu count
    int totalResultsUnAnswered;
    int totalZeroPriors;
    int totalZeroLikelihood;
    
    
	
/**********************************************************************************************/	
/**
 * Constructor
 * @param train Object
 * @param test Object
 */
    
    public  NaiveBayesRec (String trainObject, String testObject)
    {
    
    	//Get test and train objects
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
	
	    //correct answer 
	    correctlyPredicted  = 0;
	    totalPredicted      = 0;
	    
	    // For checking how much tie cases occured
	    totalTieCases = 0;
	    
	    //how many movies have null features
	    nullTestFeatures = 0;
	    currentMovieHasNullFeatures = false;
	    
	    //common features
	    noCommonFeatureFound =false;
	    noCommonality	= 0;
	    
	    //Count how much of the prior*likelihood are not contributing anything
	    totalResultsUnAnswered 	= 0;
	    totalZeroPriors 		= 0;
	    totalZeroLikelihood 	= 0;
	   
	    //Types of slots
		
		//Type
		typeToExtract = new IntArrayList();
		//typeToExtract.add(0);
		typeToExtract.add(10);
		//typeToExtract.add(9);
		//typeToExtract.add(98);
		/*typeToExtract.add(4);
		typeToExtract.add(2);
		typeToExtract.add(100);
		typeToExtract.add(101);
		typeToExtract.add(94);
		typeToExtract.add(19);
		typeToExtract.add(5);
		*/
	   		
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
             *   @param user id, and classes {1,2,3,4,5} or {1,2,3,4,5,6,7,8,9,10}
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
                		double num = priors[i] + (1.0)/moviesSize;
                		double den = moviesSize + ((classes*1.0)/moviesSize);
                		priors[i]  = num/den;
                	}
                	
                	else if(isLaplace)
                	{
                		double num = priors[i] + (1.0)/moviesSize;
                		double den = moviesSize + ((classes*1.0)/moviesSize);
                		priors[i]  = Math.log10(num/den);
                	}
                	
                	
                	//Add Psudo count
                	else
                	{
                   		double num = priors[i];
                		double den = moviesSize;         	
                		
         /*       		if (num!=0 && den!=0)
                			priors[i]  = num/den;
                		else priors[i] =   EPSILON;
         */       		
                		priors[i] = ((num + pCount)*1.0) / (moviesSize + pCount+4);
                		
                	}
                }
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

	         //------------------------------
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
         		//System.out.println(AllFeaturesInASlot[m]);
         		//System.out.println();
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
					              	    	 likelihoodIndividual[m] = likelihoodNum[m]/likelihoodDen[m];
					              	  else 
					              		   likelihoodIndividual[m] =  likelihoodIndividual[m] * likelihoodNum[m]/likelihoodDen[m];
					              	    
			              	    }
			              	    
			            	     
			              	    else if(isLog)
			              	    {
				              	     likelihoodNum[m]= N + (1.0/moviesSize);
				              	     if (vocSize!=0) likelihoodDen[m]= D + (vocSize *1.0/moviesSize);
				              	     else  likelihoodDen[m]= D + (1.0/moviesSize);          
					                 likelihoodIndividual[m] +=  Math.log10(likelihoodNum[m]/likelihoodDen[m]);
					              	    
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
		              		 if(vocSize!=0)  likelihoodDen[m]=  (vocSize *1.0/moviesSize); //vocSize!=0
		              		 else   likelihoodDen[m]=  (1.0/moviesSize);
		         	    	 likelihoodIndividual[m] = likelihoodNum[m]/likelihoodDen[m];
	              		}
	              		
	              		else if(isLog)
	              		{		              	
		         	    	 likelihoodIndividual[m] = -1e6;
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
		         	    	 likelihoodIndividual[m] = likelihoodNum[m]/likelihoodDen[m];
	              		}
	              		
		        		else if(isLog)
	              		{		              	
		         	    	 likelihoodIndividual[m] = -1e6;
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
	         	    	 likelihoodIndividual[k] = likelihoodNum[k]/likelihoodDen[k];
         	    	}
         	    	
        		   else if(isLog)
             		{		              	
	         	    	 likelihoodIndividual[k] = -1e6;
             		}
        		   
        		   else
        		   {	        		                   	     
	        	    	 //likelihoodIndividual[k] += EPSILON;
	        	    	 likelihoodIndividual[k] = (likelihoodNum[k] + pCount)/(likelihoodDen[k] + pCount +4);
        		   }
        	   }
        	
        	   if(isLaplace)
        	   {
	        	 // multiply prior and likelihood and send back
	        	  if (t==0)
	        		 likelihood[k] = likelihoodIndividual[k];
	        	  else 
	        		  likelihood[k] =  likelihood[k] * likelihoodIndividual[k];
        	   }
        	  
        	   else if(isLog)
        	   {
        			 likelihood[k] += likelihoodIndividual[k];
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
       /*if(tieCases.size()>0)
       {
	       //Break the ties through random return
	       int randIdx   = rand.nextInt(tieCases.size());
	       winnerIdx     = tieCases.get(randIdx); 
	    	   
	   }
*/    
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
     
     public void checkSizesOfFeatures()
     {
    	     	 
    	 int lessThan5 =0;
    	 for(int i=0;i<1682;i++)
    	 {
    		 HashMap<String, Double> f = MMh.getFeaturesAgainstAMovie(i);
    		 System.out.println(f);
    		 
    		 if(f == null)
    			 System.out.println("null");
    		 
    		 if(f.size() <= 5)
    			  lessThan5++;
    	 }
    	 
    	 System.out.println("less than 5 =" + lessThan5);
     }
     
     
/***************************************************************************************************/  
/***************************************************************************************************/    
   /**
    * Start recommending 
    */  
	
    public void makePrediction(int whichVersionOfTest)	
 	{
 		System.out.println("Come to make prediction");
 		int moviesSize = 0;
 		
         // For each user (in test set), make recommendations
         IntArrayList users = MTestMh.getListOfUsers(); 		        
         LongArrayList movies;
         double rating;
         int uid, mid, randMid = 0;
         int whenToBreak =0;
         double myActual =0;

    	 // get the sizs of features 
    	 checkSizesOfFeatures();
    	 
    	 for (int i = 0; i < users.size(); i++)        
         {
        	 
             uid = users.getQuick(i);        
             
             movies = MTestMh.getMoviesSeenByUser(uid); //get movies seen by this user     
             moviesSize = movies.size();
          
          	 System.out.println("currently at user =" +(i+1));
          	 
             //----------------------------------------------
             // Version = 2, A user is in test And train set
             //----------------------------------------------
            
          if(whichVersionOfTest ==2)
          {  
             for (int j = 0; j < moviesSize; j++)            
 	           {
            	 
            	 // It has features/no features
            	 currentMovieHasNullFeatures  = false;    
            	 noCommonFeatureFound		  = false;	       	 
            	 
 	             //get Movie   
            	 mid = MemHelper.parseUserOrMovie(movies.getQuick(j));                
 	          
            	 //get class priors
            	 double myPrior[] = getPrior(uid, myClasses);
 	                
            	//get class Likelihood
            	 double myLikelihood[] = getLikelihood(uid, mid, myClasses); //uid, mid, classes
 	             
            	 //get result
            	 double myResult = getMaxClass (myPrior, myLikelihood, myClasses);
            	 
            	 //add error
 	             if (currentMovieHasNullFeatures==false && noCommonFeatureFound ==false )
 	            	myActual = getAndAddError(myResult, uid, mid, myClasses);	                
 	            
 	            //get Extreme errors and correct answers 	            
 	            getExtremeErrorCount(myResult, myActual);
 	            
 	            //error
 	            double errorFound =  Math.abs(myActual - myResult) ;
 	            if(errorFound>3) System.out.println("Currently at user = "+ i +", error = actual - predicted ="
 	            		+ errorFound + ", " + myActual+ ", "+ myResult);
           
 	         } //end of all movies           
             
          }//end of version  
             
             //----------------------------------------------
             // Version = 2, A user is in test And train set
             //----------------------------------------------

      if (whichVersionOfTest==1)
        {	
        	IntArrayList movieHaveBeenPredicted = new IntArrayList();
        	whenToBreak = Math.min(moviesSize, 25);
        	
 	          while(true)
	          {
	           
            	 //get Movie   
           		 randMid = rand.nextInt(moviesSize-1);
           		 mid = MemHelper.parseUserOrMovie(movies.getQuick(randMid));                

		         	if(!movieHaveBeenPredicted.contains(mid))
		         	{
		         		
		         		whenToBreak++;
		         		movieHaveBeenPredicted.add(mid);
			         		
			        	 //get class priors
			        	 double myPrior[] = getPrior(uid, myClasses);
				                
			        	//get class Likelihood
			        	 double myLikelihood[] = getLikelihood(uid, mid, myClasses); //uid, mid, classes
				             
			        	 //get result
			        	 double myResult = getMaxClass (myPrior, myLikelihood, myClasses);
			        	 
			        	 //add error
				         myActual = getAndAddError(myResult, uid, mid, myClasses);	                
				           
				         //get Extreme errors and correct answers 	            
				         getExtremeErrorCount(myResult, myActual);
				         System.out.println("Currently at user = "+ i +", error = actual - predicted ="
				            		+ Math.abs(myActual - myResult) + ", " + myActual+ ", "+ myResult);
		         	 } //end of if
		
	          	} //end of while true
        	} //end of version ==1            
            
        
         } //end processing all users

         
          
         //Print Error
         System.out.println();
         System.out.println("Final RMSE --:" 					+ rmse.rmse());
         System.out.println("Final MAE --:" 					+ rmse.mae());
         System.out.println("Final Coverage --:" 				+ rmse.getItemCoverage());
         System.out.println("ROC Sensitivity --:" 				+ rmse.getSensitivity());
         System.out.println(" null test movies ="				+ nullTestFeatures);
         System.out.println(" no commonality is found in ="		+ noCommonality);
         
         System.out.println("Correctly Predicted --:"   + correctlyPredicted);
         System.out.println("% of correct --:"          + (correctlyPredicted * 100.0) / (totalPredicted));
         System.out.println("% of Error (>0 && <1) --:" + (extremeError1 * 100.0) / (totalPredicted));
         System.out.println("% of Error (>1 && <2) --:" + (extremeError2 * 100.0) / (totalPredicted));
         System.out.println("% of Error (>2 && <3) --:" + (extremeError3 * 100.0) / (totalPredicted));
         System.out.println("% of Error (>3 && <4) --:" + (extremeError4 * 100.0) / (totalPredicted));
         System.out.println("% of Error (>4) --:" 		+ (extremeError5 * 100.0) / (totalPredicted));
         
         //Print Extreme Error for individual user
         System.out.println("Error >=4 " + extremeError5); 
         System.out.println("Error >=3 " + extremeError4);
         System.out.println("Error >=2 " + extremeError3);
         System.out.println("Error >=1 " + extremeError2);
         System.out.println("Error >=0 " + extremeError1);
                  
         System.out.println("Tie Cases " + totalTieCases);
         System.out.println("ZEROProb " + totalResultsUnAnswered);
         
        // Here, we can re-set values in the class RMSE and other local variable
         rmse.resetValues();
         rmse.resetROC();
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

       rmse.ROC4(actual, rating, classes);
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
  /*  String test  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\sml_clusteringTestSetStored.dat";
	  String train  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\sml_clusteringTrainSetStored.dat";
	    */    	

	 //-------------------------
	 // A user is there in test 
	 // And train set
	 //-------------------------
	 
      String test  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\sml_clusteringTestSetStoredTF.dat";
	  String train  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\sml_clusteringTrainSetStoredTF.dat";
	  NaiveBayesRec myNB = new NaiveBayesRec(train, test);
	  myNB.makePrediction(2);		//predict all movies (simple has divided the set into test and train where each user is there in test and train set as well
	// myNB.checkSizesOfFeatures();

	 //-------------------------
	 // A user is there in test 
	 // or train set
	 //-------------------------
/*
	  String test  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\sml_clusteringTestSetStoredTF_Users.dat";
	  String train  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\sml_clusteringTrainSetStoredTF_Users.dat";
	  NaiveBayesRec myNB = new NaiveBayesRec(train, test);
      myNB.makePrediction(1);		//e.g. 10% users are there in test set and remaining into train set
	     */
    }
    	
}
