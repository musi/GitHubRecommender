package netflix.algorithms.modelbased.svd;


import java.io.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

import netflix.memreader.*;
import netflix.algorithms.memorybased.memreader.FilterAndWeight;
import netflix.algorithms.modelbased.reader.DataReader;
import netflix.algorithms.modelbased.itembased.method.AdjCosineSimilarityMethod;
import netflix.algorithms.modelbased.itembased.method.SimilarityMethod;
import netflix.algorithms.modelbased.reader.DataReaderFromMem;
import netflix.rmse.RMSECalculator;
import netflix.utilities.*;
import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import cern.colt.list.LongArrayList;
import cern.colt.map.OpenIntDoubleHashMap;
import cern.colt.matrix.*;
import cern.colt.matrix.linalg.*;
import cern.colt.matrix.impl.*;
import cern.colt.function.*;


/**
 * We want to build the demographic based recommender, where the correlation between two items from
 * two item demographic vectors is obtained after applying SVD over them.....I may think some extension,
 * like ....one is to add feature svd as well
 * @author Musi
 */

/****************************************************************************************************/
public class SVDFeatureBasedRecommender  
/****************************************************************************************************/
{

	// For user-item ratings matrix svd
    private SingularValueDecomposition 	svd;
    private DoubleMatrix2D 				Prediction_Matrix;// p =left * right
    DoubleMatrix2D						left;		// left = US
    DoubleMatrix2D						right;		// right = SV
    
	// For movie-demo matrix svd
    private SingularValueDecomposition 	svd_demo;
    private DoubleMatrix2D 				P_demo;			// p =left * right
    DoubleMatrix2D						left_demo;		// left = US
    DoubleMatrix2D						right_demo;		// right = SV
    DoubleMatrix2D						Uk_demo;		// 
    DoubleMatrix2D						VPrimeK_demo;		// 
    DoubleMatrix2D					    sigmaK_demo;		// 
    
    //For movie-Feature svd
    private SingularValueDecomposition 	svd_feature;
    private DoubleMatrix2D 				P_feature;			// p =left * right
    DoubleMatrix2D						left_feature;		// left = US
    DoubleMatrix2D						right_feature;		// right = SV
    DoubleMatrix2D						Uk_feature;		   // 
    DoubleMatrix2D						VPrimeK_feature;	// 
    DoubleMatrix2D					    sigmaK_feature;		// 
    
    private int 						k;
    int 								totalNegSVDPred;
    int 								totalPosSVDPred;
    int 								totalZeroSVDPred;
    
    //Just some dummy variables
    String  							myPath;    	
    DataReader 							dataReader;		   //has methods like finding all users who have rated this item etc
    SimilarityMethod 					similarityMethod;  //e.g. demo sim, feature sim, etc.
    FilterAndWeight 					myFilter;		   //to find all users who have rated two items 
    MemHelper							myTrainingMMh;
    MemHelper							fullMMh;		   //Full MemHelper without any test train (will be used for demo)
    
    
    //Regarding Results
    double 								MAE;
    double								Roc;
    boolean								itemBased;
    String								modelNormalization;
    String								infoAbtComb;
    String                              gridSearchOutput;
    NumberFormat 						nf;
    Algebra 							alg;
    boolean 							enhanced_corr_flg;	//whether, we want to use enhance corr or not
    
/****************************************************************************************************/
    
    /**
     * Constructor. 
     *
     * @param  mh  		MemHelper object for training set. 
     * @param  svdFile  File containing serialized SVD.
     * @param  k 		Number of singular values to use.
     */
    public SVDFeatureBasedRecommender() 
    {
    	nf        = new DecimalFormat("#.#####");	//upto 4 digits
    	myPath    = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/FeaturesPlay/FeatureModels/";
    	MAE		  = 0;
    	Roc 	  = 0;
    	itemBased = true;
    	
    	
    	 //  modelNormalization = "Simple";
    	//   modelNormalization = "MovNor";
    	   modelNormalization = "UserNor";
    	   infoAbtComb ="";
    	   gridSearchOutput ="";
    	   alg = new Algebra();
    	  
    }
    
/****************************************************************************************************/    

    public void prepareModelParameters( String base, 
    									String svdFile, 
    									String svdDemoFile, 
    									String svdFeatureFile,
    									int k)
    {
    
    try 
        {
        	//set objects
            this.k  = k;
            this.myTrainingMMh = new MemHelper(base);
            fullMMh = new MemHelper(myPath+"sml_storedFeaturesRatingsTF.dat");
            
           /* //make datareader object
            dataReader= new DataReaderFromMem(this.mh); 	    //it receive a memHelper objects
            
            //Similarity method
            similarityMethod =	new AdjCosineSimilarityMethod(); //assign class object to interface
            
            // filter and weight
            myFilter = new FilterAndWeight(this.mh, 1); 		//with mmh object
           */
           
            //Read SVD
            FileInputStream fis  = new FileInputStream(svdFile);
            ObjectInputStream in = new ObjectInputStream(fis);
            svd 				 = (SingularValueDecomposition) in.readObject();
            
           //Read SVD_demo
            FileInputStream fis_demo  = new FileInputStream(svdDemoFile);
            ObjectInputStream in_demo = new ObjectInputStream(fis_demo);
            svd_demo 				  = (SingularValueDecomposition) in_demo.readObject();
            
          //Read SVD_feature
            FileInputStream   fis_feature  = new FileInputStream(svdFeatureFile);
            ObjectInputStream in_feature   = new ObjectInputStream(fis_feature);       
            svd_feature				       = (SingularValueDecomposition) in_feature.readObject();
            
            totalNegSVDPred		 = 0;
            totalPosSVDPred		 = 0;
            totalZeroSVDPred	 = 0;
            
            buildModel();
            buildDemoModel();
            buildFeatureModel();
         }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

/****************************************************************************************************/
    
    /**
     * Computes the recommendation matrix from the 
     * SVD. See the paper "Application of Dimensionality
     * Reduction in Recommender Systems - A Case Study"
     * for more information. 
     */

    private void buildModel() 
    {

            DoubleMatrix2D rootSk = svd.getS().viewPart(0, 0, k, k);
                  
            //compute singular value
            for(int i = 0; i < k; i++) 
            {
              rootSk.set(i,i,Math.sqrt(rootSk.get(i,i)));  //Sets the matrix cell at coordinate [row,column] to the specified value. 
            }
            
            // Compute U and V'
            // US = m x k, Sv' = k x n ( m= rows, n =col of original matrix)
            DoubleMatrix2D U  = svd.getU();	
            DoubleMatrix2D Uk = U.viewPart(0, 0, U.rows(), k).copy(); // (int row, int column, int height, int width) 

            DoubleMatrix2D VPrime = alg.transpose(svd.getV());
            DoubleMatrix2D VPrimek = VPrime.viewPart(0, 0, k, VPrime.columns()).copy();
            DoubleMatrix2D rootSkPrime = alg.transpose(rootSk);
            
            //compute left and right by multiplying US, and SV'           
           // DoubleMatrix2D left  = alg.mult(Uk, rootSkPrime);
            left  = alg.mult(Uk, rootSk);
            right = alg.mult(rootSk, VPrimek);

            // Multiply [(U * sqrt(S)) * (sqrt(S) * V')] = USV
            Prediction_Matrix = alg.mult(left, right);     
    }
    
    //-----------------------------
    /**
     * Build demo model
     */
    
    private void buildDemoModel() 
    {
            
    		//System.out.println("svd_demo="+ svd_demo);
            sigmaK_demo = svd_demo.getS().viewPart(0, 0, k, k);
                  
            //compute singular value
            for(int i = 0; i < k; i++) 
            {
              sigmaK_demo.set(i,i,Math.sqrt(sigmaK_demo.get(i,i)));  //Sets the matrix cell at coordinate [row,column] to the specified value. 
            }
            
            // Compute U and V'
            // US = m x k, Sv' = k x n ( m= rows, n =col of original matrix)
            DoubleMatrix2D U  = svd_demo.getU();	
            Uk_demo = U.viewPart(0, 0, U.rows(), k).copy(); // (int row, int column, int height, int width) 

            DoubleMatrix2D VPrime = alg.transpose(svd_demo.getV());
            VPrimeK_demo = VPrime.viewPart(0, 0, k, VPrime.columns()).copy();
            DoubleMatrix2D rootSkPrime = alg.transpose(sigmaK_demo);
            
            //compute left and right by multiplying US, and SV'           
           // DoubleMatrix2D left  = alg.mult(Uk, rootSkPrime);
            left_demo  = alg.mult(Uk_demo, sigmaK_demo);
            right_demo = alg.mult(sigmaK_demo, VPrimeK_demo);

            //System.out.println("left_demo="+ left_demo);
            //System.out.println("right_demo="+ right_demo);
            
            // Multiply [(U * sqrt(S)) * (sqrt(S) * V')] = USV
            P_demo = alg.mult(left_demo, right_demo);     
    }
    
 //---------------------------------
 /**
  * Build feature model   
  */
    
    private void buildFeatureModel() 
    {
            
    		//System.out.println("svd_demo="+ svd_demo);
            sigmaK_feature = svd_feature.getS().viewPart(0, 0, k, k);
                  
            //compute singular value
            for(int i = 0; i < k; i++) 
            {
              sigmaK_feature.set(i,i,Math.sqrt(sigmaK_feature.get(i,i)));  //Sets the matrix cell at coordinate [row,column] to the specified value. 
            }
            
            // Compute U and V'
            // US = m x k, Sv' = k x n ( m= rows, n =col of original matrix)
            DoubleMatrix2D U  = svd_feature.getU();	
            Uk_feature = U.viewPart(0, 0, U.rows(), k).copy(); // (int row, int column, int height, int width) 

            DoubleMatrix2D VPrime = alg.transpose(svd_feature.getV());
            VPrimeK_feature = VPrime.viewPart(0, 0, k, VPrime.columns()).copy();
            DoubleMatrix2D rootSkPrime = alg.transpose(sigmaK_feature);
            
            //compute left and right by multiplying US, and SV'           
           // DoubleMatrix2D left  = alg.mult(Uk, rootSkPrime);
            left_feature  = alg.mult(Uk_feature, sigmaK_feature);
            right_feature = alg.mult(sigmaK_feature, VPrimeK_feature);

            //System.out.println("left_feature="+ left_feature);
            //System.out.println("right_feature="+ right_feature);
            
            // Multiply [(U * sqrt(S)) * (sqrt(S) * V')] = USV
            P_feature = alg.mult(left_feature, right_feature);     
    }

/****************************************************************************************************/
    
    /**
     * Decide which method to call, based on a global boolean
     */
    
    public double recommend (int userId, int movieId, int neighbours)
    
    {
    	return (recommendItemBased(userId, movieId, neighbours));
    
    /*	if(itemBased)
    	   return (recommendItemBased(userId, movieId, neighbours));
    	else
    		return (recommendUserBased(userId, movieId, neighbours)); 
    */	
    }
    
/****************************************************************************************************/
// Item-Based CF
/****************************************************************************************************/
    
    /**
     * Predicts the rating that activeUser will give targetMovie.
     *
     * @param  activeUser  The user.
     * @param  targetMovie  The movie.
     * @param  date  The date the rating was given. 
     * @return The rating we predict activeUser will give to targetMovie. 
     */
    
    public double recommendItemBased(int activeUser, int targetMovie, int totalNeighbours) 
    {
    	double entry = 0;
    	double prediction = 0;
        int mid = 0;
        int totalMoviesSeenByActiveUser = 0;
        double activeUserRatingOnSimItem = 0.0;
        double weightSum = 0.0;
        double voteSum   = 0.0;
        double activeUserAverage = 0.0;
        double answer = 0.0;
             
        double demoWeight = 0;
        double featureWeight = 0;
        
        LongArrayList movies = myTrainingMMh.getMoviesSeenByUser(activeUser);   //get movies seen by this user
    	totalMoviesSeenByActiveUser = movies.size();   
    	activeUserAverage = myTrainingMMh.getAverageRatingForUser(activeUser);  //active user average
    	
    	OpenIntDoubleHashMap itemIdToWeight = new OpenIntDoubleHashMap();
	    IntArrayList myItems      			= new IntArrayList();
	    DoubleArrayList myWeights 			= new DoubleArrayList();
	    double currentWeight;
	    
	    //GO through all the movies seen by active user and find similar items
    	for (int m =0; m<totalMoviesSeenByActiveUser; m++)
    	{    		
    		 mid = MemHelper.parseUserOrMovie(movies.getQuick(m));    	     
    		 currentWeight  = findItemSimilarity_SVD(targetMovie, mid, activeUser); //active item, item we want to find sim with
    	   	 demoWeight     = demoSVDCorrelation(targetMovie, mid);
    	   	 featureWeight  = featureSVDCorrelation(targetMovie, mid);    		 
    		 
    		 itemIdToWeight.put(mid, currentWeight);
    	 }
    	
    	//sort the weights
    	 myItems    = itemIdToWeight.keys();
         myWeights 	= itemIdToWeight.values();
         itemIdToWeight.pairsSortedByValue(myItems, myWeights);
    	
         // take weighted sum
         for (int i = totalMoviesSeenByActiveUser-1, myTotal=0; i >=0; i--)       
         {    	   
         		if(myTotal == totalNeighbours) break;              	
         		mid = myItems.get(i);       	
         		
         		//check index
         		{   
         			//positive         			
         			currentWeight = myWeights.get(i);	             	
    /*     			if(currentWeight>0){
         				weightSum += Math.abs(currentWeight);				// see no Math.abs(), for adjusted cosine I think there is no need (weight >0 always)
		             	activeUserRatingOnSimItem = Prediction_Matrix.get(mid-1,activeUser-1);           	
    	             	if((modelNormalization.equalsIgnoreCase("UserNor")))
		             		voteSum += (currentWeight * (activeUserRatingOnSimItem));
    	             	else if((modelNormalization.equalsIgnoreCase("MovNor")))
		             		voteSum += (currentWeight * (myTrainingMMh.getAverageRatingForMovie(mid)));
    	             	if((modelNormalization.equalsIgnoreCase("Simple")))
		             		voteSum += (currentWeight * (activeUserRatingOnSimItem));
    	             	if((modelNormalization.equalsIgnoreCase("SigmaNor")))
		             		voteSum += (currentWeight * (activeUserRatingOnSimItem * myTrainingMMh.getStandardDeviationForUser(activeUser)
		             																+ myTrainingMMh.getAverageRatingForUser(activeUser)));
		             		
		             	myTotal++;
         		 }
         			*/
	             	
         			//all
       				weightSum += Math.abs(currentWeight);					// see no Math.abs(), for adjusted cosine I think there is no need (weight >0 always)
       				
       				//SML       		
       					activeUserRatingOnSimItem = Prediction_Matrix.get(mid-1,activeUser-1);
       		
       					
	             	if((modelNormalization.equalsIgnoreCase("UserNor")))
	             		voteSum += (currentWeight * (activeUserRatingOnSimItem));
	             	else if((modelNormalization.equalsIgnoreCase("MovNor")))
	             		voteSum += (currentWeight * (myTrainingMMh.getAverageRatingForMovie(mid)));
	             	if((modelNormalization.equalsIgnoreCase("Simple")))
	             		voteSum += (currentWeight * (activeUserRatingOnSimItem));
	             	if((modelNormalization.equalsIgnoreCase("SigmaNor")))
	             		voteSum += (currentWeight * (activeUserRatingOnSimItem * myTrainingMMh.getStandardDeviationForUser(activeUser)
	             																+ myTrainingMMh.getAverageRatingForUser(activeUser)));
	             		
	             	myTotal++;
     		 
		             		
	             	myTotal++;
	             	

         			//weight+1
         /*			currentWeight = myWeights.get(i) + 1;
	             	weightSum += Math.abs(currentWeight);				// see no Math.abs(), for adjusted cosine I think there is no need (weight >0 always)
	             	activeUserRatingOnSimItem = P.get(mid-1,activeUser-1);           	
	             	if(!(modelNormalization.equalsIgnoreCase("Simple")))
	             		voteSum += (currentWeight * (activeUserRatingOnSimItem + activeUserAverage));
	             	else
	             		voteSum += (currentWeight * (activeUserRatingOnSimItem ));	             		
	             	 myTotal++;
         			*/
             	}
         		
          } //end of for
       
         //predict and send back         
         if(weightSum!=0) {
        	 				voteSum *= (1.0 / weightSum);
        	 				
	        	 			if((modelNormalization.equalsIgnoreCase("MovNor")))
	       	 					 answer = voteSum; 	
	       	 				else if((modelNormalization.equalsIgnoreCase("UserNor")))
	      	 					 answer = voteSum + activeUserAverage; 	
	       	 				if((modelNormalization.equalsIgnoreCase("SigmaNor")))
	      	 					 answer = voteSum ; 	
	       	 				else if((modelNormalization.equalsIgnoreCase("Simple")))             	 					
	       	 					answer = voteSum;      	 					
         				   }		
         else 
        	 answer = activeUserAverage ;
        // System.out.println("prediction="+ answer);
         
           			return answer;                  
           }

   
   

/****************************************************************************************************/
    /**
     *   @param item_active, item_target, no. of neighbouring items to consider
     *   @return similarity between two items using Adjusted cosine similarity (Ratings came from SVD) 
     */
        
        // It can be used if I am using the simple Item based CF
        public double findItemSimilarity_SVD(int activeItem, int myItem, int activeUser) 
        									
        {

        	//----------------------------------------------------------------
        	// we have to calculate sim on left matrix which is of dimensions
        	// m x k --- i.e. ratings of k pseudo users on m items
        	// here I have: m x n = items x users
        	//----------------------------------------------------------------
        	        	
        	double topSum =0.0;
        	double bottomSumActive =0.0;
        	double bottomSumTarget =0.0;
        	double rating1 = 0.0;
        	double rating2 = 0.0;
        	double sim =0;        	
        	double  activeUserAvg = myTrainingMMh.getAverageRatingForUser(activeUser);
        	
            // for all the common users
            for (int i =0; i< k; i++)
            {            
                  // get their ratings from "SVD LEFT (SV) MATRIX"               
            	//if(activeItem<1682 && myItem<1682)
            	{
            		rating1 = left.get(activeItem-1,i);                
	                rating2 = left.get(myItem-1,i);               
	                
	                //user or mov nor
	                if(!(modelNormalization.equalsIgnoreCase("Simple")))
	                {
		                topSum += rating1 * rating2;		            
		                bottomSumActive += Math.pow(rating1, 2);
		                bottomSumTarget += Math.pow(rating2, 2);
	                }
	                
	                //simple
	                else
	                {
/*	                	rating1-= activeUserAvg;
	                	rating2-= activeUserAvg;		//make offset
*/	                	
	                	topSum += rating1 * rating2;		            
		                bottomSumActive += Math.pow(rating1, 2);
		                bottomSumTarget += Math.pow(rating2, 2);
	                }
	                	
            	}                
            }
            
            double  bottomSum = Math.sqrt(bottomSumTarget) * Math.sqrt(bottomSumActive);        	
            
            if (bottomSum == 0.0) 
            	sim = 0.0;            
            else 
            	sim = topSum / bottomSum;   
          	             
          /*  System.out.println("sim found is = " + sim);
              System.out.println("common users found are = " +myUsers.size());*/
            
            return sim;            
        	
        }
        
/****************************************************************************************************/
// User-based CF  
/****************************************************************************************************/
        
        /**
         * Predicts the rating that activeUser will give targetMovie.
         *
         * @param  activeUser  The user.
         * @param  targetMovie  The movie.
         * @param  date  The date the rating was given. 
         * @return The rating we predict activeUser will give to targetMovie. 
         */
        
        public double recommendUserBased(int activeUser, int targetMovie, int totalNeighbours) 
        {
        	double entry = 0;
        	double prediction = 0;
            int uid = 0;
            int totalUsersWhoSawTargetMovie = 0;
            double NeighUserRatingOnTargetItem = 0.0;
            double weightSum = 0.0;
            double voteSum   = 0.0;
            double activeUserAverage = 0.0;
            double answer = 0.0;
                    
            LongArrayList users = myTrainingMMh.getUsersWhoSawMovie(targetMovie);   //get movies seen by this user
        	totalUsersWhoSawTargetMovie = users.size();   
        	activeUserAverage = myTrainingMMh.getAverageRatingForUser(activeUser);   //active user average
        	
        	OpenIntDoubleHashMap userIdToWeight = new  OpenIntDoubleHashMap();
    	    IntArrayList myUsers      			= new IntArrayList();
    	    DoubleArrayList myWeights 			= new DoubleArrayList();
    	    double currentWeight;
    	    
    	    //GO through all the users, who saw target movie and find similar users
        	for (int m =0; m<totalUsersWhoSawTargetMovie; m++)
        	{    		
        		 uid = MemHelper.parseUserOrMovie(users.getQuick(m));  
        //	     currentWeight  = findUserSimilarity_SVD(activeUser, uid, targetMovie); //active item, item we want to find sim with
        	     currentWeight  = findUserSimilarityViaCosine_SVD(activeUser, uid, targetMovie); 
        	   	 userIdToWeight.put(uid, currentWeight);
        	 }
        	
        	// sort the weights
        	 myUsers    = userIdToWeight.keys();
             myWeights 	= userIdToWeight.values();
             userIdToWeight.pairsSortedByValue(myUsers, myWeights);
        	
             // take weighted sum
             for (int i = totalUsersWhoSawTargetMovie-1, myTotal=0; i >=0; i-- )       
             {    	   
             		if(myTotal == totalNeighbours) break;              	
             		uid = myUsers.get(i);       	
             		
             		
             		         		
    	         	/*	currentWeight = myWeights.get(i);    	             	
    	         		weightSum += Math.abs(currentWeight);				// see no Math.abs(), for adjusted cosine I think there is no need (weight >0 always)
    	             	NeighUserRatingOnTargetItem = P.get(targetMovie-1, uid-1);            	
    	             	if(!(modelNormalization.equalsIgnoreCase("Simple")))
    	             		voteSum += (currentWeight * (NeighUserRatingOnTargetItem + activeUserAverage));
    	             	else
    	             		voteSum += (currentWeight * (NeighUserRatingOnTargetItem ));
    	             		myTotal++;
    	      */
    	      
             			//Weight +1
             			currentWeight = myWeights.get(i) + 1;    	             	
    	         		weightSum += Math.abs(currentWeight);				// see no Math.abs(), for adjusted cosine I think there is no need (weight >0 always)
    	             	NeighUserRatingOnTargetItem = Prediction_Matrix.get(targetMovie-1, uid-1);            	
    	             	if(!(modelNormalization.equalsIgnoreCase("Simple")))
    	             		voteSum += (currentWeight * (NeighUserRatingOnTargetItem + myTrainingMMh.getAverageRatingForUser(uid)));
    	             	else
    	             		voteSum += (currentWeight * (NeighUserRatingOnTargetItem - myTrainingMMh.getAverageRatingForUser(uid) ));
    	             	myTotal++;        	
             		
              } //end of for
           
             //predict and send back         
             if(weightSum!=0) {
            	 				voteSum *= (1.0 / weightSum);
            	 				 
            	 				if(!(modelNormalization.equalsIgnoreCase("Simple")))
            	 					 answer = voteSum ; //+ activeUserAverage; 	
            	 				 else             	 					
            	 					 answer = voteSum + activeUserAverage;
            	 		
             				  }		
             else
				 	answer = activeUserAverage;
            // System.out.println("prediction="+ answer);
             
               			return answer;                  
               }

/****************************************************************************************************/
        
     /**
       *   @param item_active, item_target, no. of neighbouring items to consider
       *   @return similarity between two items using Adjusted cosine similarity (Ratings came from SVD) 
       */
            
            public double findUserSimilarity_SVD(int activeUser, int expectedNeighbouringUser, int targetMovie)
            {

            	//----------------------------------------------------------------
            	// we have to calculate sim on right matrix which is of dimensions
            	// k x n --- i.e. ratings of n users on k pseudo items
            	// here I have: m x n = items x users
            	//----------------------------------------------------------------
            	        	
            	double topSum =0.0;
            	double bottomSumActive =0.0;
            	double bottomSumTarget =0.0;
            	double rating1 = 0.0;
            	double rating2 = 0.0;
            	double sim =0;            	
            	double targetMovieAvg = myTrainingMMh.getAverageRatingForMovie(targetMovie);
            	
                // for all the common items
                for (int i =0; i< k; i++)
                {            
                    // get their ratings from "SVD right (SV) MATRIX"               
                  		rating1 = right.get(i, activeUser-1);                
    	                rating2 = right.get(i, expectedNeighbouringUser-1);                
    	                
    	                //user or mov nor
    	                if(!(modelNormalization.equalsIgnoreCase("Simple")))
    	                {
	    	                topSum += rating1 * rating2;    	            
	    	                bottomSumActive += Math.pow(rating1, 2);
	    	                bottomSumTarget += Math.pow(rating2, 2);
    	                }
    	                
    	                //simple
    	                else
    	                {
    	            /*    	rating1-=targetMovieAvg;			//make ofset //really bull shit
    	                	rating2-=targetMovieAvg;
    	            */    	
    	                	topSum += rating1 * rating2;    	            
	    	                bottomSumActive += Math.pow(rating1, 2);
	    	                bottomSumTarget += Math.pow(rating2, 2);
    	                }
                                
                }
                
                double  bottomSum = Math.sqrt(bottomSumTarget) * Math.sqrt(bottomSumActive);        	
                
                if (bottomSum == 0.0) sim = 0.0;            
                else sim =topSum / bottomSum;   
                
              /*  System.out.println("sim found is = " + sim);
                  System.out.println("common users found are = " +myUsers.size());*/
                
                return sim;            
            	
            }

   //------------------------------------------------------------------------------------------------------------------------
    // Lets us see what is diff if we use cosine
            
            public double findUserSimilarityViaCosine_SVD(int activeUser, int expectedNeighbouringUser, int targetMovie)
            {

            	//----------------------------------------------------------------
            	// we have to calculate sim on right matrix which is of dimensions
            	// k x n --- i.e. ratings of n users on k pseudo items
            	// here I have: m x n = items x users
            	//----------------------------------------------------------------
            	        	
            	double topSum =0.0;
            	double bottomSumActive =0.0;
            	double bottomSumTarget =0.0;
            	double rating1 = 0.0;
            	double rating2 = 0.0;
            	double sim =0;            	
            	double targetMovieAvg = myTrainingMMh.getAverageRatingForMovie(targetMovie);
            	
                // for all the common items
                for (int i =0; i< k; i++)
                {            
                    // get their ratings from "SVD right (SV) MATRIX"               
                  		rating1 = right.get(i, activeUser-1);                
    	                rating2 = right.get(i, expectedNeighbouringUser-1);                
    	                
    	                //user or mov nor
    	                if(!(modelNormalization.equalsIgnoreCase("Simple")))
    	                {
	    	                topSum += rating1 * rating2;    	            
	    	                bottomSumActive += Math.pow(rating1, 2);
	    	                bottomSumTarget += Math.pow(rating2, 2);
    	                }
    	                
    	                //simple
    	                else
    	                {
    	              	
    	                	topSum += rating1 * rating2;    	            
	    	                bottomSumActive += Math.pow(rating1, 2);
	    	                bottomSumTarget += Math.pow(rating2, 2);
    	                }
                                
                }
                
                double  bottomSum = Math.sqrt(bottomSumTarget) * Math.sqrt(bottomSumActive);        	
                
                if (bottomSum == 0.0) sim = 0.0;            
                else sim =topSum / bottomSum;   
                
              /*  System.out.println("sim found is = " + sim);
                  System.out.println("common users found are = " +myUsers.size());*/
                
                return sim;            
            	
            }

/****************************************************************************************************/            
        
    /**
     * Tests this method and computes rmse.
     */
    
    public static void main(String[] args) 
    {
    
    	SVDFeatureBasedRecommender myRec = new SVDFeatureBasedRecommender();
    	myRec.makeRecommendations();
    }
    
    
/****************************************************************************************************/    
/**
 *  Make Recommendations, using different neighbours and dimensions,
 */
    
    public void makeRecommendations()
    {
    	String path = "";   	
    	String test = "";
    	String base = "";
    	String svdFile = "";
    	String svdDemoFile = "";
    	String svdFeatureFile = "";
    	String modelName = "";
    	
    	//boolean sparse = true;
    	boolean sparse = false;
    
    	int loop =1;
    	if(sparse) 
    		loop =5;
    	
    for(int t=0;t<loop;t++)
     {
       	
       	if(sparse ==false) {
	   	        path ="C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/FeaturesPlay/FeatureModels/";    	
	   	    	test = path + "sml_clusteringTestSetStoredTF.dat";
	   	    	base = path + "sml_clusteringTrainSetStoredTF.dat";
	   	    	svdDemoFile = path + "DemoModel_full.dat";
	   	    	svdFeatureFile = path + "FeatureModel_full.dat";
       		}
       	
       	else{
	       	    path ="C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/";    	
	   	    	test = path + "sml_clusteringTestSetStoredTF.dat";
	   	    	base = path + "Simple/Sparsity"+ (95+t)+"/sml_trainSetStoredAll_0.9" + (5+t) + "3" + ".dat";
	       	}
       	
    	for(int m=2;m<=2;m++)
    	{
    		
    		//if (!(m == 9 || m == 10))  continue;
    		
	    	switch(m)
	    	{
	    	  case 0:   modelName = "SVDZeros"; break;
	    	  case 1:   modelName = "SVDRandom"; break;
	    	  case 2:   modelName = "SVDMovAvg"; break;
	    	  case 3:   modelName = "SVDUserAvg"; break;
	    	  case 4:   modelName = "SVDMovAndUserAvg"; break;
	    	  case 5:   modelName = "SVDUniform"; break;
	    	  case 6:   modelName = "SVDuserNormal"; break;
	    	  case 7:   modelName = "SVDMovNormal"; break;
	    	  case 8:   modelName = "SVDUserBasedCF"; break;
	    	  case 9:   modelName = "SVDItemBasedCF"; break;
	    	  case 10:  modelName = "SVDUserAndItemBasedCF"; break;
	    	  case 11:  modelName = "SVDNB"; break;
	    	  case 12:  modelName = "SVDSVM"; break;
	    	  case 13:  modelName = "SVDUserMovNormal"; break;
	    	  case 14:  modelName = "SVDMovUserNormal"; break;
	    	  default:  break;
	    	}
    	    	
	    	//Normalized
    	      //    svdFile = path + "/MovNorm/" + modelName + "MovNor.dat";
	    	        svdFile = path + modelName + "_full.dat";

    	   //Simpe	    	  
		      //   svdFile = path + "/Simple/" + modelName + "Simple.dat";
		    	
		    	if(sparse ==true) 
		    		svdFile = path + "Simple/Sparsity"+ (95+m) +"/"+ modelName + "Simple.dat";	            	
	    	      
	        for(int i=5;i<=25;i++)
	        {
	        	
	        	//call prepare model parameters method
	        	prepareModelParameters(base, svdFile, svdDemoFile, svdFeatureFile, i);	        	
		        MemHelper mhTest = new MemHelper(test);		        
		        
		        for(int n=5;n<=25;n+=5)
		        {
		        	double error =0;
		        	
		        	for(int temp=0;temp<2;temp++)
		        	{      	
		        
		        	  if(temp==0) enhanced_corr_flg = false;
		        	  else  enhanced_corr_flg = true;
		        	  
			          error = testWithMemHelper(mhTest, n);
			          System.out.println("Model =" +modelName + ", MAE @ k = "+ i + ", neigh = "+ n + " is= " + error); 
		        	}//end for
		        	
		          //write for latex
		          if(n==1)
		             gridSearchOutput+=i+" & ";		          
		             gridSearchOutput+=nf.format(error)+" & ";
		          
		          
		      //  System.out.println("Total SVD pred <0 = " + svdRec.totalNegSVDPred);
		      //  System.out.println("Total SVD pred >0 = " + svdRec.totalPosSVDPred);
		        
		        } //to check effect of neighbours
		        
		        gridSearchOutput+="\n";		        
		        System.out.println();
	        } //end of k for
    	} //end of differer SVDs' for
      }//end of sparse for
    
    System.out.println(gridSearchOutput);
    
      //-----------------------------
      // 5-FOLD
      //-----------------------------
        
       
    	/*path ="C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/FiveFoldData/";  
               
    	for (int fold =1; fold<=5;fold++)
    	{
	    	svdFile = path +  "SVDStoredColt" + (fold)+ ".dat";
	    	base 	= path +  "sml_trainSetStoredFold" + (fold)+ ".dat";
	    	test	= path +  "sml_testSetStoredFold" + (fold)+ ".dat";
	    	
	    	for(int n = 5; n<35;n+=5)
	    	{
		        for(int i=5;i<=20;i++)
		        {		        	
			        SVDItemBasedRecommender svdRec = new SVDItemBasedRecommender(base, svdFile, i);
			        MemHelper mhTest = new MemHelper(test);
			        System.out.println("FOld: "+ fold + ",k: "+ i + ", Neighbours:" + n +", MAE= "
			        					+ svdRec.testWithMemHelper(mhTest, n));
			 
		        } //end k for 
		        
		        System.out.println("--------------------------------------------------");
	    	} //end neighbour for
    	}
    	*/    	
     }    
    
/************************************************************************************************/
/**
 * Find demographic correlation between two items
 * Apply SVD over them first.
 */
    
    public double demoSVDCorrelation (int activeItem, int targetItem)
    {       
         double den_active =0, den_target=0, num=0;
         double corr = 0;
// 	     System.out.println("svd_demo="+ svd_demo);
        
 	    // System.out.println("left_demo="+ left_demo);
 	    
    	 for(int i=0;i<k;i++)
    	 {
    		 double rat_active = left_demo.get(activeItem-1, i);
    		 double rat_target = left_demo.get(targetItem-1, i);
    		 
    		 //get num and den
    		 num +=rat_active * rat_target;
    		 den_active += Math.pow(rat_active,2);
    		 den_target += Math.pow(rat_target,2);
    		     		 
    	 }
    	 
    	 //sqrt den (as in vector sim)
    	 den_active = Math.sqrt( den_active);
    	 den_target = Math.sqrt( den_target);
    	 
    	 //find corr
    	 double temp = (den_active * den_target);
    	 
    	 if(temp!=0)
    		 corr = num/(temp);
    	 
    	 return corr;
    	 
    }
    
    //------------------------------
    /**
     * Cosine simi measure based on the query trnasformation
     */
    public double demoSVDCorrelationViaQuery (int activeItem, int targetItem)
    {       
    	
    		 double[][] data  = new double[19][1]; 	//Need its transpose         
             
    		 //Get their genres (MemHelper have two methods, one return map and second returns arraylist)
             LongArrayList genreActive = fullMMh.getGenreAgainstAMovie(activeItem);                          
             int  sizeActive = genreActive.size();                   
                 
            	 for(int j=0;j<19;j++)
                 {
                   	 //active
                   	 if(genreActive.contains(j))
                   		 	data[j][0] = j;
                   	 else 
                   		 	data[j][0] = 0;               	 
                  }        
        
             
         DenseDoubleMatrix2D query = (DenseDoubleMatrix2D) DoubleFactory2D.dense.make(data);      
         DoubleMatrix2D temp_query = alg.mult(alg.transpose(query), alg.transpose(VPrimeK_demo));
         DoubleMatrix2D transformed_query = alg.mult(temp_query, sigmaK_demo);
         
         double den_active =0, den_target=0, num=0;
         double corr = 0;
         
    	 for(int i=0;i<k;i++)
    	 {
    		 double rat_active = transformed_query.get(0, i);
    		 double rat_target = left_demo.get(targetItem-1, i);
    		 
    		 //get num and den
    		 num +=rat_active * rat_target;
    		 den_active += Math.pow(rat_active,2);
    		 den_target += Math.pow(rat_target,2);
    		     		 
    	 }
    	 
    	 //sqrt den (as in vector sim)
    	 den_active = Math.sqrt( den_active);
    	 den_target = Math.sqrt( den_target);
    	 
    	 //find corr
    	 double temp = (den_active * den_target);
    	 
    	 if(temp!=0)
    		 {
    		 	corr = num/(temp);
    		 	//System.out.println("corr="+corr);
    		 }
    	 
    	 
    	 return corr;
    	 
    }
    
 /************************************************************************************************/
    /**
     * Find feature correlation between two items
     * Apply SVD over them first.
     */
        
        public double featureSVDCorrelation (int activeItem, int targetItem)
        {       
             double den_active =0, den_target=0, num=0;
             double corr = 0;
//     	     System.out.println("svd_demo="+ svd_demo);
            
     	    // System.out.println("left_demo="+ left_demo);
     	    
        	 for(int i=0;i<k;i++)
        	 {
        		 double rat_active = right_feature.get(i, activeItem-1);
        		 double rat_target = right_feature.get(i,targetItem-1);
        		 
        		 //get num and den
        		 num +=rat_active * rat_target;
        		 den_active += Math.pow(rat_active,2);
        		 den_target += Math.pow(rat_target,2);
        		     		 
        	 }
        	 
        	 //sqrt den (as in vector sim)
        	 den_active = Math.sqrt( den_active);
        	 den_target = Math.sqrt( den_target);
        	 
        	 //find corr
        	 double temp = (den_active * den_target);
        	 
        	 if(temp!=0)
        		 corr = num/(temp);
        	 
        	 return corr;
        	 
        }
/************************************************************************************************/
         		 
    		    /**
    		     * Using RMSE as measurement, this will compare a test set
    		     * (in MemHelper form) to the results gotten from the recommender
    		     *  
    		     * @param testmh the memhelper with test data in it   //check this what it meant........................Test data?///
    		     * @return the rmse in comparison to testmh 
    		     */

    		    public double testWithMemHelper(MemHelper testmh, int neighbours)     
    		    {
    		        RMSECalculator rmse = new RMSECalculator();
    		        
    		        IntArrayList users;
    				LongArrayList movies;
    		        String blank = "";
    		        int uid, mid, total=0;
    		        int totalUsers=0;
    		        int totalExtremeErrors =0;
    		        int totalEquals =0;
    		        int totalErrorLessThanPoint5 =0;
    		        int totalErrorLessThan1 =0;
    		        		        
    		        // For each user, make recommendations
    		        users = testmh.getListOfUsers();
    		        totalUsers= users.size(); 
    		        //________________________________________
    		        
    		        for (int i = 0; i < totalUsers; i++)        
    		        {
    		            uid = users.getQuick(i);       
    		            movies = testmh.getMoviesSeenByUser(uid);
    		           // System.out.println("now at " + i + " of total " + totalUsers );
    		            
    		            for (int j = 0; j < movies.size(); j++)             
    		            {
    		            	total++;
    		                mid = MemHelper.parseUserOrMovie(movies.getQuick(j));
    		                
    		           //     if (mid ==-1)  System.out.println(" rating error--> uid, mid -->" + uid + "," + mid );
    		                
    		               // double rrr = recommend(uid, mid, blank);                
    		                double rrr = recommend(uid, mid, neighbours);
    		                
    		                double myRating=0.0;
    		                
    		                //if (rrr!=0.0)                 
    		                      {
    		                	
    		                			myRating = testmh.getRating(uid, mid);			 		// get actual ratings?

    		                            if (myRating==-99 )                           
    		                               System.out.println(" rating error, uid, mid, rating" + uid + "," + mid + ","+ myRating);
    		                           
    		                            if(rrr>5 || rrr<=0)
    		                            {
    		         /*                   	System.out.println("Prediction ="+ rrr + ", Original="+ myRating+ ", mid="+mid 
    		                            		+", NewMid="+ myMoviesMap.get(mid)+ ", uid="+uid
    		                            		+"No users who rated movie="+ mh.getNumberOfUsersWhoSawMovie(mid) + 
    											", User saw movies="+mh.getNumberOfMoviesSeen(uid));*/
    		                            }
    		                            
    		                            if(rrr>5 || rrr<-1)
    		                            	totalExtremeErrors++;
    		                            
    		                            else if(Math.abs(rrr-myRating)<=0.5)
    		                            	totalErrorLessThanPoint5++;
    		                            
    		                            
    		                            else if(Math.abs(rrr-myRating)<=1.0)
    		                            	totalErrorLessThan1++;
    		                            
    		                            else if (rrr==myRating)
    		                            	totalEquals++;
    		                            
    		                          		                            
    		                            //-------------
    		                            // Add ROC
    		                            //-------------
    		                            rmse.ROC4(myRating, rrr, 5, myTrainingMMh.getAverageRatingForUser(uid));		
    		            
    		                            //-------------
    		                            //Add Error
    		                            //-------------
    		                            rmse.add(myRating,rrr);		
    		            
    		                            //-------------
    		                            //Add Coverage
    		                            //-------------

    		                             rmse.addCoverage(rrr);                            
    		                           
    		                             
    		                		  }         
    		            
    		            }
    		        }
    		   
    		        double dd= rmse.mae();
    		        MAE = dd;
    		        Roc = rmse.getSensitivity();
    		        
    		     /* System.out.println("totalExtremeErrors="+totalExtremeErrors + ", Total ="+total);
    		        System.out.println("totalErrorLessThanPoint5="+totalErrorLessThanPoint5 );	       
    		        System.out.println("totalErrorLessThan1="+totalErrorLessThan1 );
    		        System.out.println("totalEquals="+totalEquals );  */
    		        
    		        
    		        //rmse.resetValues();        
    		        return dd;
    		    }

    		    
}