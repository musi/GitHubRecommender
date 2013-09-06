package netflix.recommender;

//-----------------------------------------------------------------------------------------
/* Things to do:


 * 
 * 1- SVD do not outperform simple one, if we do like 20-80, so what I have to do is to 
 * Do svd for each method, by randomly dividing whole set into x% test set, where x=10,20,30,40,50,60
 * And then check which is best.
 * 
 * 2- It is also good to show, how simple CF and SVD based performs with the increase in x%......so
 * idea will be to show when we have very less training data what would happens.
 * 
 * 3- Or say that simple SVD based recommendations do not outperform (MAE) CF ones, in 20-80...
 * this was the case with sarwar as well, but afterwards som paper claims that it poutperform?
 * apply item-based and user-based simple one and compare them with SVD-simple rec and with simple 
 * CFs.
 * Then apply your concept (filling) and compare SVD with simple SVD, and simple CF. Choose ur best imputation
 * source, and show the results with simple SVD in all x%. You can have the simple CF etc. for
 * performance measure as well.
 *                                                                                 
 *                                              
 * Make this program generic, i.e. it should return SVD, UBCF, IBCF results.
 * 
 *      
 *                                              


*/
//-----------------------------------------------------------------------------------------

import java.io.*;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import netflix.memreader.*;
import netflix.algorithms.memorybased.memreader.FilterAndWeight;
import netflix.algorithms.modelbased.reader.DataReader;
import netflix.algorithms.modelbased.itembased.method.SimilarityMethod;
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
 * Recommends movies using the SVD-based technique 
 * described by Sarwar et. al. in "Application of 
 * Dimensionality Reduction in Recommender Systems -
 * A Case Study"
 *
 * Note that this class does not implement the resort() 
 * method in AbstractRecommender. Since the SVD is 
 * precomputed, new users cannot be added to the system.
 *
 * @author sowellb
 */

/****************************************************************************************************/
public class SVDItemBasedRecommender  
/****************************************************************************************************/
{

    private SingularValueDecomposition 	svd, simpleSVD;
    private DoubleMatrix2D 				Prediction_Matrix;				// p =left * right
    private DoubleMatrix2D 				Prediction_Matrix_SVD;			// p =left * right
    private DoubleMatrix2D 				previous_P;						// p =left * right
    DoubleMatrix2D						left, left_SVD;					// left = US
    DoubleMatrix2D						right, right_SVD;				// right = SV
    Algebra 							alg;
    private int 						k, k_SVD;
    private int							xFactor;						//training set size
    private int 						FOLD_VALUE;
    int 								totalNegSVDPred;
    int 								totalPosSVDPred;
    int 								totalZeroSVDPred;
    
    //some objects
    String  							myPath;    	
    DataReader 							dataReader;		   //has methods like finding all users who have rated this item etc
    SimilarityMethod 					similarityMethod;  //e.g. demo sim, feature sim, etc.
    FilterAndWeight 					myFilter;		   //to find all users who have rated two items 
    MemHelper							myTrainingMMh;
    MemHelper							myTestMMh;
    MemHelper							myMainMMh;    
    NumberFormat 	   					nf;
    int 								FTDataWithMinRat;
    int									myTotalFolds;	   // FT=1, sml=5
    int 								myClasses;
    int									TopNThreshold;	   // 4 for sml, 7 for ft
    int									dataset;		  // 0=sml, 1=ft, 2=ml						
    int									ImputationMethod;
    
    
    //Regarding Results
    double 								MAE;
    double								MAEPerUser;
    double 								RMSE;
    double								Roc;
    double								pValue;
    
    //SD in one fold or when we do hold-out like 20-80
    double								SDInMAE;
    double								SDInROC;
	double 								SDInTopN_Precision[];
	double 								SDInTopN_Recall[];
	double 								SDInTopN_F1[];	
	
    double            					precision[];		//evaluations   
    double              				recall[];   
    double              				F1[];    
    private OpenIntDoubleHashMap 		midToPredictions;	//will be used for top_n metrics (pred*100, actual)
    
    //1: fold, 2: k, 3:dim
    double              array_MAE[][][];	      			// array of results, got from diff folds
    double              array_MAEPerUser[][][];
    double              array_NMAE[][][];
    double              array_NMAEPerUser[][][];
    double              array_RMSE[][][];
    double              array_RMSEPerUser[][][];
    double              array_Coverage[][][];
    double              array_ROC[][][];
    double              array_BuildTime[][][];
    double              array_Precision[][][][]; // [topnN][fold][][]
    double              array_Recall[][][][];
    double              array_F1[][][][];    
    
    //will store the grid results in the form of mean and sd
    double				gridResults_Mean_MAE[][];
    double				gridResults_Mean_MAEPerUser[][];
    double				gridResults_Mean_NMAE[][];
    double				gridResults_Mean_NMAEPerUser[][];
    double				gridResults_Mean_RMSE[][];
    double				gridResults_Mean_RMSEPerUser[][];
    double				gridResults_Mean_ROC[][];
    double				gridResults_Mean_Precision[][][];   //[TOPn][][]
    double				gridResults_Mean_Recall[][][];
    double				gridResults_Mean_F1[][][];
    
    double				gridResults_Sd_MAE[][];
    double				gridResults_Sd_MAEPerUser[][];
    double				gridResults_Sd_NMAE[][];
    double				gridResults_Sd_NMAEPerUser[][];
    double				gridResults_Sd_RMSE[][];
    double				gridResults_Sd_RMSEPerUser[][];
    double				gridResults_Sd_ROC[][];
    double				gridResults_Sd_Precision[][][];
    double				gridResults_Sd_Recall[][][];
    double				gridResults_Sd_F1[][][];
    
    double              mean_MAE[];	      					// Means of results, got from diff folds
    double              mean_MAEPerUser[];
    double              mean_NMAE[];						// for each version
    double              mean_NMAEPerUser[];
    double              mean_RMSE[];
    double              mean_RMSEPerUser[];
    double              mean_Coverage[];
    double              mean_ROC[];
    double              mean_BuildTime[];
    double              mean_Precision[];   
    double              mean_Recall[];   
    double              mean_F1[];       
    
    double              sd_MAE[];	      					// SD of results, got from diff folds
    double              sd_MAEPerUser[];
    double              sd_NMAE[];							// for each version
    double              sd_NMAEPerUser[];
    double              sd_RMSE[];
    double              sd_RMSEPerUser[];
    double              sd_Coverage[];
    double              sd_ROC[];
    double              sd_BuildTime[];
    double              sd_Precision[];   
    double              sd_Recall[];   
    double              sd_F1[];   
    
    //Some parameter flags
    boolean 			fiveFoldFlag;
    boolean 			FTFlag;   
    boolean				sparse;	
    String				myChoice;
    String				modelNormalization;
    String              trainingOrFullSet; 					//training or test set sed
    
    
    //Parameters for hybrid recommender
    double alpha;											//UB coff
    double beta;											//IB coff
    
    //I should know them, and manulaly assign  them, for hybrid recommender testing
    int    ub_Opt_neigh;									//optimum no. of neighbours for ub
    int    ib_Opt_neigh;									//optimum no. of neighbours for ib
    int    ub_Opt_K;										//optimum no. of dimensions for ub
    int    ib_Opt_K;										//optimum no. of dimensions for ib
    
    //Used to control loop indexesz of neighbouring and dim fors
	int upperN 		= 0;		// No. of Neighbours
	int incInN 		= 0;		// Inc in N
	int upperDim 	= 50;		// Total dimensions to be tested
	int incInDim 	= 2;		// Inc in dimensions
	
	//Related to cold start
	int coldStartUsers; 		
	int coldStartItems; 		
	int coldStartThreshold;	
	boolean newUserScenario;
	MemHelper    coldUsersMMh;
	MemHelper    coldItemsMMh;
	
    //File Writers
     FileWriter myWriter[][];
     RMSECalculator rmse;
    
/****************************************************************************************************/
    
    /**
     * Constructor. 
     *
     * @param  memReaderFile  File containing serialized MemReader.
     * @param  string, which rec sys, IBCF, UBCF 
     * @param  boolean, true for folding
     * @param  string, model normalization
     * @param  int, traiining set size, e.g. 80, 60
     */

    public SVDItemBasedRecommender (boolean foldFlag, int X, int dataset) 
    {
    		
    		myPath = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/";
    	
    		this.dataset 		= dataset;
    		MAE 				= 0;
	    	MAEPerUser			= 0;
	    	RMSE 				= 0;
	    	Roc 				= 0;
	    	pValue				= 0;
	    	SDInMAE				= 0;
	    	SDInROC				= 0;
	    	SDInTopN_Precision	= new double[8];
	    	SDInTopN_Recall		= new double[8];
	    	SDInTopN_F1			= new double[8];
	    	
	    	alg 				= new Algebra();
	    	midToPredictions    = new OpenIntDoubleHashMap();     	  
	        precision    		= new double[8];		//topN; for six values of N (top5, 10, 15...30)
	    	recall  			= new double[8];		// Most probably we wil use top10, or top20
	    	F1					= new double[8];
	    	
	        //Initialize results, Mean and SD	    	
	    	 array_MAE  	 	= new double[5][11][51];  //IBCF: [10]--> k=5;k<50;K+=5
	    	 array_MAEPerUser	= new double[5][11][51]; 
	    	 array_NMAE		 	= new double[5][11][51];  //IBCF: [10]--> k=10;k<100;K+=10
	    	 array_NMAEPerUser	= new double[5][11][51];
	    	 array_RMSE 	 	= new double[5][11][51];
	    	 array_RMSEPerUser 	= new double[5][11][51];
	         array_Coverage  	= new double[5][11][51];
	         array_ROC 		 	= new double[5][11][51];
	         array_BuildTime 	= new double[5][11][51];
	         
	         array_Precision 	= new double[8][5][11][51]; //[topN][fold][neigh][dim]
	         array_Recall 	 	= new double[8][5][11][51];
	         array_F1 		 	= new double[8][5][11][51];
	         	         
	         //So we have to print this grid result for each scheme,
	         //Print in the form of "mean + sd &" 
	         gridResults_Mean_MAE 			= new double[11][51];	// neigh, dim, see IBCF, and UBCF diff above	        
	         gridResults_Mean_NMAE			= new double[11][51];	         
	         gridResults_Mean_RMSE			= new double[11][51];
	         gridResults_Mean_MAEPerUser	= new double[11][51];
	         gridResults_Mean_RMSEPerUser	= new double[11][51];
	         gridResults_Mean_NMAEPerUser	= new double[11][51];
	         gridResults_Mean_ROC			= new double[11][51];
	         
	         gridResults_Mean_Precision		= new double[8][11][51];  // [toppN][neigh][dim]
	         gridResults_Mean_Recall		= new double[8][11][51];
	         gridResults_Mean_F1			= new double[8][11][51];       
	         	         
	         gridResults_Sd_MAE			= new double[11][51];	         
	         gridResults_Sd_NMAE		= new double[11][51];	         
	         gridResults_Sd_RMSE		= new double[11][51];
	         gridResults_Sd_NMAEPerUser	= new double[11][51];
	         gridResults_Sd_MAEPerUser	= new double[11][51];
	         gridResults_Sd_RMSEPerUser = new double[11][51];
	         gridResults_Sd_ROC			= new double[11][51];
	         
	         gridResults_Sd_Precision	= new double[8][11][51];
	         gridResults_Sd_Recall		= new double[8][11][51];
	         gridResults_Sd_F1			= new double[8][11][51];
	         
	        // mean and sd, may be not required
	        mean_MAE 		= new double[5];	        
	        mean_NMAE 		= new double[5];	        
	        mean_RMSE 		= new double[5];
	        
	        mean_NMAEPerUser= new double[5];
	        mean_RMSEPerUser= new double[5];
	        mean_MAEPerUser = new double[5];
	        
	        mean_Coverage 	= new double[5];
	        mean_ROC 		= new double[5];
	        mean_BuildTime  = new double[5];
	        mean_Precision	= new double[5];
	        mean_Recall		= new double[5];
	        mean_F1			= new double[5];	        
	        
	        sd_MAE 			= new double[5];	        
	        sd_NMAE 		= new double[5];
	        sd_RMSE 		= new double[5];
	        
	        sd_MAEPerUser	= new double[5];
	        sd_NMAEPerUser 	= new double[5];
	        sd_RMSEPerUser	= new double[5];
	        
	        
	        sd_Coverage 	= new double[5];
	        sd_ROC 			= new double[5];
	        sd_BuildTime 	= new double[5];
	        sd_Precision 	= new double[5];
	        sd_Recall 		= new double[5];
	        sd_F1		 	= new double[5];       
	    			
	    	
	    	fiveFoldFlag 	   = foldFlag;	// 5-fold or simple one	    	
	    	xFactor			   = X;    	
	       	 
    	
    	    totalNegSVDPred		 = 0;
            totalPosSVDPred		 = 0;
            totalZeroSVDPred	 = 0;
    
            // For no folding, they stay the same, for folding, we change them within five fold loop
            nf = new DecimalFormat("#.#####");	//upto 4 digits
    
            //Files
            myWriter = new FileWriter[11][8];	//see file open section
            
            //FTFlag
            if(this.dataset == 1)
            	FTFlag  = true;				   // FT dataset
            else
            	FTFlag = false;				   // Movielens dataset	
            
    }											
    
/****************************************************************************************************/
 /**
  * Prepare SVD for building model
  * @param String, file name of the stored model
  */
    
   public void callBuildModel(String svdFile)
   {
    try 
        {
    		//svd = null;
    		
            //Read SVD
            FileInputStream fis  = new FileInputStream(svdFile);
            ObjectInputStream in = new ObjectInputStream(fis);
            svd 				 = (SingularValueDecomposition) in.readObject();
            
            FileInputStream fis_Simple  = new FileInputStream(svdFile);
            ObjectInputStream in_Simple = new ObjectInputStream(fis_Simple);            
            simpleSVD					= (SingularValueDecomposition) in_Simple.readObject();
            
           // System.out.println("done reading svd");
            //System.out.println(svd.cond());
            // Call the build model method
            buildModel();
         }
        catch(Exception e) {
            e.printStackTrace();
            System.exit(1);
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
    		left  = null; 
    		right = null;
    		Prediction_Matrix   = null;
    		alg   = new Algebra();
    		
    	     DoubleMatrix2D rootSk = svd.getS().viewPart(0, 0, k, k);
             
             //compute singular value
             for(int i = 0; i < k; i++) 
             {
               rootSk.set(i,i,Math.sqrt(rootSk.get(i,i)));  //Sets the matrix cell at coordinate [row,column] to the specified value. 
             }
             
            // Compute U and V'
            DoubleMatrix2D U  = svd.getU();	
            DoubleMatrix2D Uk = U.viewPart(0, 0, U.rows(), k).copy(); // (int row, int column, int height, int width) 

            DoubleMatrix2D VPrime = alg.transpose(svd.getV());
            DoubleMatrix2D VPrimek = VPrime.viewPart(0, 0, k, VPrime.columns()).copy();
            DoubleMatrix2D rootSkPrime = alg.transpose(rootSk);
            
           // compute left and right by multiplying US, and SV'           
           // DoubleMatrix2D left  = alg.mult(Uk, rootSkPrime);
            DoubleMatrix2D tempLeft = alg.mult(Uk, rootSk);
            DoubleMatrix2D tempRight = alg.mult(rootSk, VPrimek);
            System.gc();
            DoubleMatrix2D tempP = alg.mult(tempLeft, tempRight);
            
                        
            left  = alg.mult(Uk, rootSk);
            right = alg.mult(rootSk, VPrimek);

            // Multiply [(U * sqrt(S)) * (sqrt(S) * V')] = USV
            Prediction_Matrix = alg.mult(left, right);       
        
    }

    //------------------------------
    
    private void buildModel_SVD() 
    {
    		left_SVD 			 	= null; 
    		right_SVD 				= null;
    		Prediction_Matrix_SVD   = null;
    		alg  					= new Algebra();
    		
    	     DoubleMatrix2D rootSk = simpleSVD.getS().viewPart(0, 0, k_SVD, k_SVD);
             
             //compute singular value
             for(int i = 0; i < k_SVD; i++) 
             {
               rootSk.set(i,i,Math.sqrt(rootSk.get(i,i)));  //Sets the matrix cell at coordinate [row,column] to the specified value. 
             }
             
            // Compute U and V'
            DoubleMatrix2D U  = simpleSVD.getU();	
            DoubleMatrix2D Uk = U.viewPart(0, 0, U.rows(), k_SVD ).copy(); // (int row, int column, int height, int width) 

            DoubleMatrix2D VPrime = alg.transpose(simpleSVD.getV());
            DoubleMatrix2D VPrimek = VPrime.viewPart(0, 0, k_SVD, VPrime.columns()).copy();
            DoubleMatrix2D rootSkPrime = alg.transpose(rootSk);
            
           // compute left and right by multiplying US, and SV'           
           // DoubleMatrix2D left  = alg.mult(Uk, rootSkPrime);
            DoubleMatrix2D tempLeft = alg.mult(Uk, rootSk);
            DoubleMatrix2D tempRight = alg.mult(rootSk, VPrimek);
            DoubleMatrix2D tempP = alg.mult(tempLeft, tempRight);
            
                        
            left_SVD  = alg.mult(Uk, rootSk);
            right_SVD = alg.mult(rootSk, VPrimek);

            // Multiply [(U * sqrt(S)) * (sqrt(S) * V')] = USV
            Prediction_Matrix_SVD = alg.mult(left_SVD, right_SVD);       
        
    }
    
/****************************************************************************************************/
    
    /**
     * Decide which method to call, based on a global boolean
     */
    
    public double recommend (int userId, int movieId, int neighbours)
    {    	
    	if(myChoice.equalsIgnoreCase("IBCF"))
    	   return (recommendItemBased(userId, movieId, neighbours));
    	else if(myChoice.equalsIgnoreCase("UBCF"))
    		return (recommendUserBased(userId, movieId, neighbours));
      	else if(myChoice.equalsIgnoreCase("UBIBCF"))
    		return (recommendUserItemBased(userId, movieId));  
      	else if(myChoice.equalsIgnoreCase("Switch"))
    		return (recommendSwitchBased(userId, movieId));
    	else
    		return (recommendSVDBased(userId, movieId));
    }
  
 /****************************************************************************************************/
 // SVD based rec
 /****************************************************************************************************/
     
     /**
      * Predicts the rating that activeUser will give targetMovie.
      *
      * @param  activeUser  The user.
      * @param  targetMovie  The movie. 
      * @return The rating we predict activeUser will give to targetMovie. 
      */
     
     public double recommendSVDBased(int activeUser, int targetMovie) 
     {
    	
    		double entry=0, previous_Entry =0;
    		
        	double prediction =0;
        	
            //if ( activeUser<943 && targetMovie <1682)
            {
           	 // Entry is retrieved in the correct way, i.e. rows x cols = movs x users
           	   
            	if(dataset==0){
            		entry = Prediction_Matrix_SVD.get(targetMovie-1, activeUser-1);	   			 // SML 
            	}
            	
            	else if(dataset==1){
            		if(FTDataWithMinRat <=1 && trainingOrFullSet.equalsIgnoreCase("full"))
            			entry = Prediction_Matrix_SVD.get(targetMovie-1, activeUser-1);	   		 // ft
            		else if(FTDataWithMinRat <=1 && trainingOrFullSet.equalsIgnoreCase("training"))
            			entry = Prediction_Matrix_SVD.get(targetMovie-1, activeUser-1);	     	 // ft
            		else
            			entry = Prediction_Matrix_SVD.get(activeUser-1,targetMovie-1);	    	 // ft
            	}
            	
            	else{
            		entry = Prediction_Matrix_SVD.get(activeUser-1, targetMovie-1);	   			 // ML
            	}
           	     
           	    if(modelNormalization.equalsIgnoreCase("MovNor"))
              		prediction = entry + myTrainingMMh.getAverageRatingForMovie(targetMovie);
              	else if(modelNormalization.equalsIgnoreCase("UserNor"))              		
            	    prediction = entry + myTrainingMMh.getAverageRatingForUser(activeUser);
              	else if(modelNormalization.equalsIgnoreCase("Simple")) 
              		prediction = entry;              	
              	else if(modelNormalization.equalsIgnoreCase("SigmaNor")) 
              		prediction = entry + myTrainingMMh.getStandardDeviationForUser( activeUser-1);
            }             
           
           //System.out.println(FOLD_VALUE);
            return prediction;
    	 
     }
     
/****************************************************************************************************/
// Hybrid
/****************************************************************************************************/
     // Do not print any file, etc for it.....just run it separately from main program
     // and print the results in a format to be copied easily into table
     
     /**
      * Hybrid rec sys
      * @param int, active user
      * @param int, target movie   
      */
     
     public double recommendUserItemBased(int activeUser, int targetMovie)     		 							
     {
    	 //call it with optimal parameters    	   	 
    	 double UBPrediction = recommendUserBased(activeUser,targetMovie,ub_Opt_neigh);

    	 //call it with optimal parameters    	 
    	 double IBPrediction = recommendItemBased(activeUser,targetMovie,ib_Opt_neigh);
    	     	 
    	 //Return a linear combination of individual predictions
    	 return ((alpha * UBPrediction + beta * IBPrediction));
    	 //return (UBPrediction);
     }

/****************************************************************************************************/
// Hybrid Switching
/****************************************************************************************************/
       // Do not print any file, etc for it.....just run it separately from main program
       // and print the results in a format to be copied easily into table
       
       /**
        * Hybrid rec sys
        * @param int, active user
        * @param int, target movie   
        */
       
       public double recommendSwitchBased(int activeUser, int targetMovie)     		 							
       {
    	   double UBPrediction  = 0;
    	   double IBPrediction  = 0;
    	   double SVDPrediction = 0;    	   
    	   
    	 //get movies seen by this user   
         LongArrayList movies = myTrainingMMh.getMoviesSeenByUser(activeUser);  
       	 int totalMoviesSeenByActiveUser = movies.size();
       	
       //get number of users who saw this movie
       	LongArrayList users = myTrainingMMh.getUsersWhoSawMovie(targetMovie);   //get movies seen by this user
     	int totalUsersWhoSawTargetMovie = users.size();   
     
     	if(totalUsersWhoSawTargetMovie >=60)   	   	 
      	  UBPrediction = recommendUserBased(activeUser,targetMovie,ub_Opt_neigh);
      	  
     	else if(totalMoviesSeenByActiveUser >=60)     	 
      	  IBPrediction = recommendItemBased(activeUser,targetMovie,ib_Opt_neigh);
      	     	 
     	else
     		SVDPrediction = recommendSVDBased(activeUser, targetMovie);
     	  	 
      	
     	
     	//Return a linear combination of individual predictions	
      	 //return ((alpha * UBPrediction + beta * IBPrediction));
     	
     	double prediction = UBPrediction + IBPrediction + SVDPrediction;      	 
     	return prediction;
     	
       }
       
/****************************************************************************************************/
// Item-Based CF
/****************************************************************************************************/
    
    /**
     * Predicts the rating that activeUser will give targetMovie.
     *
     * @param  activeUser  The user.
     * @param  targetMovie  The movie.
     * @param  neigh the no. of neighbour to use 
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
                
/*        LongArrayList movies = myTrainingMMh.getMoviesSeenByUser(activeUser);   //get movies seen by this user
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
    	   	 itemIdToWeight.put(mid, currentWeight);
    	 }*/
    	
    	
        
    	
    	//All movies, as in case of SVD, a user have rated all movies (100% coverage)
        IntArrayList movies = myMainMMh.getListOfMovies();
        totalMoviesSeenByActiveUser = movies.size();    	
    	
    	activeUserAverage = myTrainingMMh.getAverageRatingForUser(activeUser);  //active user average
    	
    	OpenIntDoubleHashMap itemIdToWeight = new OpenIntDoubleHashMap();
	    IntArrayList myItems      			= new IntArrayList();
	    DoubleArrayList myWeights 			= new DoubleArrayList();
	    double currentWeight;
	    
	    //GO through all the movies seen by active user and find similar items
    	for (int m =0; m<totalMoviesSeenByActiveUser; m++)
    	{    		
    		 mid = (movies.getQuick(m));  
    	     currentWeight  = findItemSimilarity_SVD(targetMovie, mid, activeUser); //active item, item we want to find sim with
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
       				if(dataset==0)
       					activeUserRatingOnSimItem = Prediction_Matrix.get(mid-1,activeUser-1);
       				
       				//FT
       				else if(dataset==1){
       					if(FTDataWithMinRat <=1)
       						activeUserRatingOnSimItem = Prediction_Matrix.get(mid-1,activeUser-1);
       					else
       						activeUserRatingOnSimItem = Prediction_Matrix.get(activeUser-1, mid-1);
       					}

       				//ML	
       				else{
       					activeUserRatingOnSimItem = Prediction_Matrix.get(activeUser-1, mid-1);
       				}
       				
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
	             	

         			//weight+1
         /*			currentWeight = myWeights.get(i) + 1;
	             	weightSum += Math.abs(currentWeight);				// see no Math.abs(), for adjusted cosine I think there is no need (weight >0 always)
	             	activeUserRatingOnSimItem = P.get(mid-1,activeUser-1);           	
	             	if(!(modelToBuild.equalsIgnoreCase("Simple")))
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
        	double activeUserAvg = myTrainingMMh.getAverageRatingForUser(activeUser);
        	
            // for all the common users
            for (int i = 0; i < k; i++)
            {            
                // get their ratings from "SVD LEFT (SV) MATRIX"               
            	// if(activeItem<1682 && myItem<1682)
            	{
            		//SML
            		if(dataset==0){
	            		rating1 = left.get(activeItem-1,i);                
		                rating2 = left.get(myItem-1,i);               
            		}
            		
            		//FT
            		else if(dataset==1){
            			
            			if(FTDataWithMinRat <=1){            			
            				rating1 = left.get(activeItem-1,i);                
    		                rating2 = left.get(myItem-1,i);
            			}
            		
	            		else{
	            			rating1 = right.get(i,activeItem-1);                
    		                rating2 = right.get(i,myItem-1);
	            		}
            		}
            		
            		//ML (it is also like FT5)
            		else{
            				rating1 = left.get(i, activeItem-1);                
    		                rating2 = left.get(i, myItem-1);
            			}
            		
            		
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
	         /*       	rating1-= activeUserAvg;
	                	rating2-= activeUserAvg;		//make offset , no man not like this
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
         * @param  neigh the no. of neighbour to use 
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
                    
  /*          LongArrayList users = myTrainingMMh.getUsersWhoSawMovie(targetMovie);   //get movies seen by this user
        	totalUsersWhoSawTargetMovie = users.size();
        	
        	activeUserAverage = myTrainingMMh.getAverageRatingForUser(activeUser);   //active user average
        	
        	OpenIntDoubleHashMap userIdToWeight = new OpenIntDoubleHashMap();
    	    IntArrayList myUsers      			= new IntArrayList();
    	    DoubleArrayList myWeights 			= new DoubleArrayList();
    	    double currentWeight;
    	    
    	    //GO through all the users, who saw target movie and find similar users
    	    //We are doing the usual steps, the only difference is that, we would measure the
    	    //similarity between items or users with their profiles limited to K dimesnions
    	    //So if K is small, then it is fine, else there is no need of it (means , one
    	    //cannot claim that, it is scalable)
        	for (int m =0; m<totalUsersWhoSawTargetMovie; m++)
        	{    		
        		 uid = MemHelper.parseUserOrMovie(users.getQuick(m));  
        //	     currentWeight  = findUserSimilarity_SVD(activeUser, uid, targetMovie); //active item, item we want to find sim with
        	     currentWeight  = findUserSimilarityViaCosine_SVD(activeUser, uid, targetMovie); 
        	   	 userIdToWeight.put(uid, currentWeight);
        	 }
        	*/
        	
        	
            //All users have seen the target movie, as 100% coverage
            IntArrayList users = myTrainingMMh.getListOfUsers();
        	totalUsersWhoSawTargetMovie = users.size();        	
        	
        	activeUserAverage = myTrainingMMh.getAverageRatingForUser(activeUser);   //active user average
        	
        	OpenIntDoubleHashMap userIdToWeight = new OpenIntDoubleHashMap();
    	    IntArrayList myUsers      			= new IntArrayList();
    	    DoubleArrayList myWeights 			= new DoubleArrayList();
    	    double currentWeight;
    	    
    	    //GO through all the users, who saw target movie and find similar users
    	    //We are doing the usual steps, the only difference is that, we would measure the
    	    //similarity between items or users with their profiles limited to K dimesnions
    	    //So if K is small, then it is fine, else there is no need of it (means , one
    	    //cannot claim that, it is scalable)
        	for (int m =0; m<totalUsersWhoSawTargetMovie; m++)
        	{    		
        		 uid = (users.getQuick(m));  
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
    	             	if(!(modelToBuild.equalsIgnoreCase("Simple")))
    	             		voteSum += (currentWeight * (NeighUserRatingOnTargetItem + activeUserAverage));
    	             	else
    	             		voteSum += (currentWeight * (NeighUserRatingOnTargetItem ));
    	             		myTotal++;
    	      */
    	      
             			//Weight +1
             			currentWeight = myWeights.get(i);    	             	
    	         		weightSum += Math.abs(currentWeight);				// see no Math.abs(), for adjusted cosine I think there is no need (weight >0 always)
    	             	
    	         		//SML
    	         		if(dataset==0)
    	         			NeighUserRatingOnTargetItem = Prediction_Matrix.get(targetMovie-1, uid-1);
    	         		
    	         		//FT
    	         		else if(dataset==1){
    	         			if(FTDataWithMinRat <=1)
    	         				NeighUserRatingOnTargetItem = Prediction_Matrix.get(targetMovie-1, uid-1);
    	         			else
    	         				NeighUserRatingOnTargetItem = Prediction_Matrix.get(uid-1, targetMovie-1);    	         	
             					}
				        //ML    
	             		else{
	             			NeighUserRatingOnTargetItem = Prediction_Matrix.get(uid-1, targetMovie-1);
				             }
            	 
    	             	if((modelNormalization.equalsIgnoreCase("UserNor")))
    	             		voteSum += (currentWeight * (NeighUserRatingOnTargetItem));
    	             	else if((modelNormalization.equalsIgnoreCase("MovNor")))
    	             		voteSum += (currentWeight * (NeighUserRatingOnTargetItem + myTrainingMMh.getAverageRatingForMovie(targetMovie)));
    	             	else if((modelNormalization.equalsIgnoreCase("Simple")))
    	             		voteSum += (currentWeight * (NeighUserRatingOnTargetItem - myTrainingMMh.getAverageRatingForUser(uid) ));
    	             	else if((modelNormalization.equalsIgnoreCase("SigmaNor")))
    	             		voteSum += (currentWeight * (NeighUserRatingOnTargetItem *  myTrainingMMh.getStandardDeviationForUser(uid) +  myTrainingMMh.getAverageRatingForUser(uid)));
    	             	myTotal++;        	
             		
              } //end of for
           
             //predict and send back         
             if(weightSum!=0) {
            	 				voteSum *= (1.0 / weightSum);
            	 				 
            	 				if((modelNormalization.equalsIgnoreCase("MovNor")))
            	 					 answer = voteSum; 	
            	 				else if((modelNormalization.equalsIgnoreCase("UserNor")))
           	 					 answer = voteSum + activeUserAverage; 	
            	 				if((modelNormalization.equalsIgnoreCase("SigmaNor")))
           	 					 answer = voteSum; 	
            	 				else if (modelNormalization.equalsIgnoreCase("Simple"))            	 					
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
                  		
                		//SML
                		if(dataset==0){
	                		rating1 = right.get(i, activeUser-1);                
	    	                rating2 = right.get(i, expectedNeighbouringUser-1);                
                		}
                		
                		//FT
                		else if(dataset==1){
                			if(FTDataWithMinRat <=1){
                				rating1 = right.get(i, activeUser-1);                
    	    	                rating2 = right.get(i, expectedNeighbouringUser-1);
                			}
                				
                			else{  //it must be different here
                				rating1 = left.get(activeUser-1,i);                
    	    	                rating2 = left.get(expectedNeighbouringUser-1,i);
                			}          			
                		}
                		
                		//ML
                		else{
                			rating1 = right.get(activeUser-1, i);                
	    	                rating2 = right.get(expectedNeighbouringUser-1, i);
                		}
                		
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
    	              /*  	rating1-=targetMovieAvg;			//make ofset
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
                	//SML
            		if(dataset==0){
                		rating1 = right.get(i, activeUser-1);                
    	                rating2 = right.get(i, expectedNeighbouringUser-1);                
            		}
            		
            		//FT
            		else if(dataset==1){
            			if(FTDataWithMinRat <=1){
            				rating1 = right.get(i, activeUser-1);                
	    	                rating2 = right.get(i, expectedNeighbouringUser-1);
            			}
            				
            			else{
            				rating1 = left.get(activeUser-1,i);                
	    	                rating2 = left.get(expectedNeighbouringUser-1,i);
            			}          			
            		}
            		
            		//ML
            		else{
            			rating1 = right.get(activeUser-1, i);                
    	                rating2 = right.get(expectedNeighbouringUser-1, i);
            		}
            		
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
/****************************************************************************************************/
            
    /**
     * Main method, just call another method for computing results 
     * Tests this method and computes rmse.
     */
    
    public static void main(String[] args) 
    {
    	
    	 boolean foldingFlg  = true;    	 
    	 int      X			 = 80;				// 100 for sml, 80 for ft (infat x=0.8 in both cases)
    	 int      dataset	 = 1;				// 0=sml, 1=ft, 2=ml 
    	  
    	SVDItemBasedRecommender svdRec = new SVDItemBasedRecommender (foldingFlg,		// FiveFold?													    			 
													    			  X,				// training set ratio
													    			  dataset    		// dataset 
    																 );

    	//We have to change it for FT only
    	svdRec.FTDataWithMinRat = 5;
    	
    	//Prepare model parameters
    	svdRec.prepareParameters();
    }
    
/****************************************************************************************************/
/**
 * Prepare the model parameters
 */
    public void prepareParameters()
    {   
    	//Related to cold start
		coldStartUsers 		= 50;
		coldStartItems 		= 50;
		coldStartThreshold	= 2;
		newUserScenario     = false;
		
    	//Prepare the parameters and call the method
    	//	trainingOrFullSet = "training";
    	  trainingOrFullSet = "full";    	  
    	  sparse = false;
    	  int end = 0;
        	
    	//It will just control, 5-folds and 1-fold (as is the case with FT)
    	if(FTFlag==false){
    		
    		if(sparse ==false)
    		{
	    		 myTotalFolds 	= 1;
	    		 myClasses 		= 5;
	    		 TopNThreshold 	= 4;
	    		 end = 1;
    		}
    		
    		else
    		{
    			 myTotalFolds 	= 1;			//only one fold
	    		 myClasses 		= 5;
	    		 TopNThreshold 	= 4;
	    		 end = 1;
    		}
    	}
    	else{
    		 
    		 myTotalFolds   = 1;
    		 myClasses 		= 10;
    		 TopNThreshold  = 6;
    		 end = 1;
    	}
    	
    	for(int m=0;m<=0;m++)  //loop to control the minUsers and Mov dataset in FT
    	{
    		if(m==1) continue;
    	
    		if(m==0)
    			FTDataWithMinRat = 1;
    		else if(m==1)
    			FTDataWithMinRat = 2;    		
    		else if(m==2)
    			FTDataWithMinRat = 5;
    		else   			
        		FTDataWithMinRat = 10;
        		
	    	for(int i=1;i<=2;i++)
	    	{        		
	    		if(i==0) myChoice = "SVDRec";    		
	    		else if(i==1) myChoice = "UBCF";
	    		else if(i==2) myChoice = "IBCF";
	    		else if(i==3) myChoice = "UBIBCF";
	
	    		for(int j=1;j<=1;j++)
	    		{
	    			if(j==0) 	  modelNormalization = "Simple";   
	    			else if(j==1) modelNormalization = "UserNor";
	    			else if(j==2) modelNormalization = "MovNor";
	    			else if(j==3) modelNormalization = "SigmaNor";    			 	 	        
	    			
	    			//call compute results
	    			computeResults();
	    		}
	    	}
    	}//end outer for
    	
    	
    	//--------------------
    	//  For checking hybrid sys, we should know, which model Nor was best (I hope, both user and item-based should
    	//  have the same model normalization)
    	//  Then call the computerResults() method with it
    	
    /*	FTDataWithMinRat = 1;
    	
    	 myChoice = "UBIBCF";
    	// myChoice = "Switch";
    	 ub_Opt_neigh = 5;					
	     ib_Opt_neigh = 5;
	     ub_Opt_K	  = 60;			//should change them to optimal				
	     ib_Opt_K 	  = 60;	        
		 modelNormalization = "UserNor";
	    // modelNormalization = "Simple";
		  
		 //call compute results
		  computeResults();*/

		
    }
    
/****************************************************************************************************/

    /**
     * Call other methods and Compute the results. 
     */
    
    public void computeResults()
    {
    	
    	String path, train, test, mainObj = null;
        int      sparsityLevel      = 0;				// Control the sparse training set object        
        int      sparsityLoopLimit  = 0;				// Control the sparse training set object
  
        if(sparse ==false)
        	sparsityLoopLimit = 1;
        else
        	sparsityLoopLimit = 9;        
        
  for( sparsityLevel = 1; sparsityLevel<=sparsityLoopLimit; sparsityLevel++)
  {      	
    	//SML
    	if(dataset==0){
    		if(sparse ==false){
		    	path	= "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/";   	
		    	train 	= path + "sml_clusteringTrainSetStoredTF.dat";
		    	test  	= path + "sml_clusteringTestSetStoredTF.dat";
		    	mainObj = path + "sml_storedFeaturesRatingsTF.dat";
    		}
    		
	    	else{
	    		path = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/" +xFactor + "/";   	
	    		train =  path + "SparseRatings/sml_trainSetAll_"+ sparsityLevel + ".dat";	    		
		    	test = path + "sml_clusteringTestSetStoredTF.dat";
	    		}
    	
    	}
    	
    	//FT
    	else if(dataset==1){
	    	path = "C:/Users/Musi/workspace/MusiRecommender/DataSets/FT/SVD/80/";   	
	    	train = path + "ft_clusteringTrainSetStoredTF.dat";
	    	test = path + "ft_clusteringTestSetStoredTF.dat";
    	}
    	
    	//ML
    	else{
    		path = "C:/Users/Musi/workspace/MusiRecommender/DataSets/ML_ML/SVD/";   	
	    	train = path + "ml_clusteringTrainSetStoredTF.dat";
	    	test = path + "ml_clusteringTestSetStoredTF.dat";    		
    	}
    	
    	String svdFile = "";
     	String modelName = "";
    	
    	//Initialize the rmse object here, for pvalue, 
     	//for other experiments you can put it in testwithmemhelper method
     	 rmse = new RMSECalculator();
     	
    	//loop to the number of models we have (filled by diff sources)
    	for(int mm=0;mm<=10;mm++)
    	{    		
    		/*
    		if(mm<=1)  continue;
    		if(mm>=4 && mm<=7) continue;
    		if(mm>=11 && mm<=12)  continue;*/
    		     		
    		
    		if(mm<=1)  continue;
    		if(mm>=4 && mm<=7) continue;   		
    		if(mm>=11 &&mm<=12) continue;
    		
    		
    		ImputationMethod =mm;
    		
	    	switch(mm)
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
	    	  case 13:  modelName = "SVDSVMReg"; break;
	    	  case 14:  modelName = "SVDIBK"; break;
	    	  
	    	/*  case 14:  modelName = "SVDUserMovNormal"; break;
	    	  case 15:  modelName = "SVDMovUserNormal"; break;*/
	    	  default:  break;
	    	}
    	    	
	    	System.out.println("mm="+ modelName);
	    	
/*	    if(fiveFoldFlag==false)
	     {	
	    	//SML
	    	if(dataset==0){
	    		if(sparse==false){
			    	// path ="C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/";  
			    	  path ="C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/FeaturesPlay/";
			    	  train = path + "80/"+ "sml_clusteringTrainSetStoredTF.dat";
			          test = path + "80/"+ "sml_clusteringTestSetStoredTF.dat";
			      		mainObj = path + "80/"+ "sml_storedFeaturesRatingsTF.dat";
	    		}
	    		
	    		//sparse ratings
	    		else{
	    			path = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/" + xFactor + "/";   	
		    		train =  path + "SparseRatings/sml_trainSetStoredAll_"+ sparsityLevel + ".dat";	    		
			    	test = path + "sml_clusteringTestSetStoredTF.dat";    			
	    		}
	    	}
	    	
	    	//FT
	    	else if(dataset==1){
	    		  path ="C:/Users/Musi/workspace/MusiRecommender/DataSets/FT/SVD/80/";
	    		  train = path + "ft_clusteringTrainSetStoredTF"+ FTDataWithMinRat +".dat";
		          test = path + "ft_clusteringTestSetStoredTF"+ FTDataWithMinRat +".dat";
	    	  }
	    	
	    	//ML
	    	else{
	    		  path ="C:/Users/Musi/workspace/MusiRecommender/DataSets/ML_ML/SVD/FeaturesPlay/";
		    	  train = path + "ml_clusteringTrainSetStoredTF.dat";
		          test = path + "ml_clusteringTestSetStoredTF.dat";
	    	}
	    	
	    	//create test and train objects
	    	 myTrainingMMh = new MemHelper (train);
	    	 myTestMMh = new MemHelper (test);	    	
	    
	    	 //SML
	    	 if(FTFlag==false){	    	 
	    		 	if(sparse==false)
	    		 		svdFile = path + "80/"+ modelNormalization +"/" + modelName + "_full_" + modelNormalization + ".dat";
	    		 	else
	    		 		svdFile = path + modelNormalization + "/Sparsity/"+modelName +"_"+ trainingOrFullSet +"_"+ sparsityLevel +"_" + modelNormalization + ".dat";
	    		 		 
	    	 }
	    	 
	    	 //FT
	    	 else{
	 	    	//Normalized or simple
	    		 svdFile = path + modelNormalization + "/" + FTDataWithMinRat + "/" + modelName +
	    		 					"_" + trainingOrFullSet + "_" + sparsityLevel + "_" + modelNormalization+ ".dat";	    	     
	    	 }
	    	 
    	    //Loop over Dimensions = 1-25   
	        for(int i=2;i<=upperDim;i+=2)
	        {	        	
	        	//change dim
	            this.k = k_SVD = i;
	          
	     
	            //build the model (It needs K, so it shld be inside the loop)
	    	     callBuildModel(svdFile);
	    	 	 buildModel_SVD();	
	    	     
		        //Loop over neighbours= 1-70
		        for(int n=70;n<=70;n+=5)
		        {
		          testWithMemHelper(myTestMMh, n);		          
		          
		          System.out.println("Model =" + modelName + ", MAE @ k = "+ i + ", neigh = "+ n + " is= " + MAE);
		          System.out.println("precision="+ precision[1]+", recall="+recall[1]+", F1="+F1[1]);
		        		 
				    	
			      //  System.out.println("Total SVD pred <0 = " + svdRec.totalNegSVDPred);
			      //  System.out.println("Total SVD pred >0 = " + svdRec.totalPosSVDPred);
			        
		         } //end neighbours for
		        System.out.println();
	          }//end for
	     } //end if
*/	    
	    
	    
	//else 
	{
    	
		String svdFileSimple="";
	    String svdFileUserNor="";
	    
      //-----------------------------
      // 5-FOLD
      //-----------------------------
       
    	//It is good to store and print the results in a grid, where
    	//K is the rows and dimensions is the cols, so what I have to do is
    	//to calculate the result (mean and sd for each coordinate and print it, or store it
    	//in a 2-d array (separate for mean and sd), then print.
       
		if(dataset==0){
			
			if(sparse ==false){
			/*     path ="C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/FiveFoldData/"+ xFactor + "/";
				 train = path + "sml_clusteringTrainSetStoredTF.dat";
		          test = path + "sml_clusteringTestSetStoredTF.dat";
		      	  mainObj = path + "sml_storedFeaturesRatingsTF.dat";	
		      	  */
				
				/*//feature plays
				 path ="C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/FeaturesPlay/" + xFactor + "/";
		    	 train 	 = 	path + "sml_clusteringTrainSetStoredTF.dat";
		         test 	 = 	path + "sml_clusteringTestSetStoredTF.dat";
		      	 mainObj = 	path + "sml_storedFeaturesRatingsTF.dat";
		    */
		      	  
		      	  //New User cold-Start
					path 	  = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/FiveFoldData/"+ xFactor + "/";
					train     = path + "sml_clusteringTrainSetStoredTF"+coldStartUsers+"_"+coldStartThreshold+".dat";
				    test      = path + "sml_clusteringTestSetStoredTF"+coldStartUsers+".dat";
				    mainObj   = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/FeaturesPlay/" + "sml_storedFeaturesRatingsTF.dat";
	      			coldUsersMMh   = new MemHelper(path + "sml_StoredColdUsers50.dat");  
	      			coldItemsMMh   = new MemHelper(path + "sml_StoredColdItems50.dat");
	      			
			}
			
			else
				path = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/" + xFactor + "/";
				
		}
		
		else if(dataset==1){
			//80-20
			//  path ="C:/Users/Musi/workspace/MusiRecommender/DataSets/FT/SVD/" + xFactor +"/";
			 
			//5 fold
				path ="C:/Users/Musi/workspace/MusiRecommender/DataSets/FT/SVD/FiveFoldData/"+ xFactor + "/";
			
		}
		
		else{
			path ="C:/Users/Musi/workspace/MusiRecommender/DataSets/ML_ML/SVD/"+ xFactor + "/";
		}
		
    	//--------------------------------------------------
    	// Set some parameters here, based on UB, IB, UBIB etc., So that you do not
    	// need to alter them manually
    	
    
    	int limit =0, loop=0;  //Will determine, if hybrid version is called, run 11 times to determine the coff   
    	      	   
    	if(myChoice.equalsIgnoreCase("IBCF")){		//For IBCF, neighbours are diff
    		upperN  = 50;
    		incInN  = 5;
    		limit   = 1;
    	}
    	
    	if(myChoice.equalsIgnoreCase("UBCF")){		//For UBCF, neighbours are diff
    		upperN = 100;
    		incInN  = 10;
    		limit   = 1;
    	}
    	
    	if(myChoice.equalsIgnoreCase("SVDRec")){    //For SVDRec, neighbours are diff
    		upperN = 5;								// we wanna run the neighbour loop only one time, 
    		incInN  = 5;
    		limit   = 1;							//	as it has no effect with change in enighbours
    		
    	}
    	
    	if(myChoice.equalsIgnoreCase("UBIBCF") ||
    	   myChoice.equalsIgnoreCase("Switch") ){    //For Hybrid Rec, neighbours and Dim are constant
    		upperN 	 = 5;							// we wanna run the neighbour & Dim loop only one time, 
    		incInN   = 5;  							// as it has no effect with change in enighbours
    		upperDim = 2;
    		incInDim = 2;
    		myTotalFolds = 1;
    		limit    = 11;
    	}
    		
  //Start outer Loop, and Determine coff to be tested    
   for (int ubCoff=0;ubCoff<=10;ubCoff++)
   {
    for(int ibCoff=0;ibCoff<=10;ibCoff++)
    {
      if(ubCoff+ibCoff == 10)
      {  
    	  //loop 11 times for hybrid and one times for simple approaches
    	  if(loop++ >= limit)  break;
    	  
    	  //get coff to be tested
    	  alpha = ubCoff/10.0;
    	  beta  = ibCoff/10.0;
    	  
    	/*alpha = 0.4;
    	  beta  = 0.6; 	*/

    	//-----------------------------------------------------------
    	//Run 5-fold for SML, for FT just a split and thats it      
    	  
   		for (int fold =1; fold<=myTotalFolds;fold++)    	
    	{    
   			if(dataset==0){   				
   				
   				if(sparse ==false){
	    		
   				//Model: usual 5-folds 
	    /*		 svdFile = path  + modelNormalization +"/" + modelName + "_" + trainingOrFullSet + "_"+ modelNormalization + fold  +".dat";
	    		 svdFileSimple= path  + modelNormalization +"/" + modelName + "_" + trainingOrFullSet + "_Simple"  + fold  +".dat";
	    		 svdFileUserNor= path  + modelNormalization +"/" + modelName + "_" + trainingOrFullSet + "_UserNor" + fold  +".dat";
	    		  	*/			
	    		 //usual 20-80
	    		// svdFile = path  + modelNormalization +"/" + modelName + "_" + trainingOrFullSet + "_"+ modelNormalization +".dat";
	    		 
				//Feature Play
	    		// svdFile = path + modelNormalization +"/" + modelName + "_full_" + modelNormalization + ".dat";
	    		 
	    		 //New User Cold-Start Problem
	    		  svdFile = path  + modelNormalization +"/" + modelName + "_" + trainingOrFullSet + "_"+ modelNormalization + 
	    		 																				+coldStartUsers+"_"+coldStartThreshold+".dat";
	    		
	    		    //each time, new test file    
		    		 if(trainingOrFullSet.equalsIgnoreCase("full")){
		    			 
		    			 //usual 20-80
		/*    			 train = path + "sml_clusteringTrainSetStoredTF.dat";
				          test = path + "sml_clusteringTestSetStoredTF.dat";
				      	  mainObj = path + "sml_storedFeaturesRatingsTF.dat";*/	
				      	  
		    			 //usual 5-folds
		    			/* train  = path + "sml_trainSetStoredFold" +(fold) + ".dat";
		    			 test   = path + "sml_testSetStoredFold" +(fold) + ".dat"; 	
		    			 mainObj = path + "sml_storedFeaturesRatingsTF.dat";*/		    			 
		    			 
		    			
		    			 /* //feature play
				    	  train = path + "sml_clusteringTrainSetStoredTF.dat";
				          test = path + "sml_clusteringTestSetStoredTF.dat";
				      	  mainObj = path + "sml_storedFeaturesRatingsTF.dat";
				      	 */
		    			 
				    	 //New User cold-Start
		    				path ="C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/FiveFoldData/"+ xFactor + "/";
							train     = path + "sml_clusteringTrainSetStoredTF"+coldStartUsers+"_"+coldStartThreshold+".dat";
						    test      = path + "sml_clusteringTestSetStoredTF"+coldStartUsers+".dat";
						    mainObj   = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/FeaturesPlay/" + "sml_storedFeaturesRatingsTF.dat";
			      			  
				      	   	
		    		 }
		    		 
		    		 else{    			      
				    	 train  = path + "sml_trainingTrainSetStoredFold" +(fold) + ".dat";
				    	 test   = path + "sml_trainingValSetStoredFold" +(fold) + ".dat";
				    	 mainObj = path + "sml_storedFeaturesRatingsTF.dat";
		    		 }
   				}
   				
   				else{   					
   					svdFile = path + modelNormalization + "/Sparsity/"+modelName +"_"+ trainingOrFullSet +"_"+ sparsityLevel +"_" + modelNormalization + ".dat";
   				
	   				//each time, new test file    
	   	    		 if(trainingOrFullSet.equalsIgnoreCase("full")){
	   	    			train =  path + "SparseRatings/sml_trainSetStoredAll_"+ sparsityLevel + ".dat";	    		
				    	test = path + "sml_clusteringTestSetStoredTF.dat";      	 
	   	    		 }
   	    		
   				}
   			}
   			
   			else if(dataset==1){
   				
	   			/*String FTInfo = "Both";
	   			String info = "Both";	*/   			
   				
	   			/*String FTInfo = "Both";
	   			String info = FTInfo + "Red";*/
	   			
	   			
   			/*	String FTInfo = "OnlyUsers";   				
   				String info = FTInfo;
   			*/
   				/*String FTInfo = "OnlyUsers";   				
   				String info = FTInfo + "Red";*/
   			
   				/*String FTInfo = "";
	   			String info = "";	*/   			
   				
   				//for 5 fold
   				String FTInfo = "Both";
	   			String info =   "All";
   				
   				
   			 svdFile = path + modelNormalization + "/" + FTDataWithMinRat + "/" + modelName + "_" + trainingOrFullSet + "_"+ modelNormalization+ info+ fold+".dat";
   			 
   				//Normalized or simple
   			 if(trainingOrFullSet.equalsIgnoreCase("full")){				   
   				 
   				 //80-20
	   		/*	  train = path + "ft_clusteringTrainSetStored"+ FTInfo + "TF" + FTDataWithMinRat +".dat";
		          test = path + "ft_clusteringTestSetStored"+ FTInfo + "TF" + FTDataWithMinRat +".dat";
	   			     */
		          
   				 //5 fold		          
		          train = path + "ft_trainSetStored"+ FTInfo + "Fold" + FTDataWithMinRat + fold+".dat";
		          test = path + "ft_testSetStored"+ FTInfo + "Fold" +   FTDataWithMinRat + fold+".dat";
		          mainObj = path + "ft_storedFeaturesRatingsBothTF"+    FTDataWithMinRat + ".dat";
		          
   			 }
   			 
   			 else{   				
	   			  train = path + "ft_clusteringTrainingTrainSetStored"+ FTInfo + "TF" +  FTDataWithMinRat +".dat";
		          test = path + "ft_clusteringTrainingValSetStored"+ FTInfo + "TF" +  FTDataWithMinRat +".dat";
		          String dumMain = path + "ft_modifiedStoredTrainingFeaturesRatings"+ FTInfo + "TF" +  FTDataWithMinRat +".dat";
		          mainObj        = dumMain;
		          MemHelper myMainHel = new MemHelper(dumMain);
		          
		  /*       System.out.println("mov="+ myMainHel.getNumberOfMovies());			
		         System.out.println("user="+ myMainHel.getNumberOfUsers());	 	           
   				 */
   			 }   				 
   	
   			}
   			
   			//ML
   			else{
   				//model normalization or simple
	    		 svdFile = path  + modelNormalization +"/" + modelName + "_" + trainingOrFullSet + "_"+ modelNormalization  +".dat";
	    		 
	    		   	//each time, new test file    
	    		 if(trainingOrFullSet.equalsIgnoreCase("full")){
	    			 train  = path + "ml_clusteringTrainSetStoredTF" + ".dat";
	    			 test   = path + "ml_clusteringTestSetStoredTF" +".dat";
	    			 mainObj = path + "ml_storedFeaturesRatingsTF.dat";	    			 
	    		 }
	    		 
	    		 else{    			      
			    	 train  = path + "ml_clusteringTrainingTrainSetStoredTF" + ".dat";
			    	 test   = path + "ml_clusteringTrainingValidationSetStoredTF" + ".dat";
	    		 }
	    		
   			}
   			
    		//System.out.println(svdFile);
    		
   		
	    	 //make each time diff test and train objects
	    	   myTestMMh = new MemHelper(test);
		       myTrainingMMh = new MemHelper(train);	 
		       //myMainMMh = new MemHelper (mainObj);
	   	   	// Loop over Dimensions 
		     for(int d=2,dim=0;d<=upperDim;d+=incInDim, dim++) 
      	  // for(int d=2,dim=0;d<=2;d+=2, dim++)				//for UBIBCF
		 //  for(int d=2,dim=0;d<=2;d+=incInDim, dim++)  			 //For Pair-T, wanna run only once
		      {  	
		        	
		    	 //Loop over neighbours    
			    for(int N = 5, neigh=0; N<=upperN;N+=incInN, neigh++) 
		       //   for(int N =20, neigh=0; N<=20;N+=1, neigh++)   			//for UBIBCF
		        {		    		
		        	
		        	//change dimensions
			    /*   k 		=	d;     
			         k_SVD 	= 	15;
			    */ 
		        	
		        	  k = k_SVD = d;
		        	
				   //   k = k_SVD = ib_Opt_K;
				   
				   //Hard-coded Dimensions for determining the PValue (SML) 
		/*		   if(ImputationMethod ==2)
					   k = k_SVD = 10;
				   else if(ImputationMethod ==3)
					   k = k_SVD = 8; 				   
				   else if(ImputationMethod ==8)
					   k = k_SVD = 36;
				   else if(ImputationMethod ==9)
					   k = k_SVD = 36;
				   else if(ImputationMethod ==10)
					   k = k_SVD = 36;
				   else if(ImputationMethod ==13)
					   k = k_SVD = 15;*/
				   
				   //ft1
			/*	   if(ImputationMethod ==2)
					   k = k_SVD = 10;
				   else if(ImputationMethod ==3)
					   k = k_SVD = 4;				   
				   else if(ImputationMethod ==8)
					   k = k_SVD = 4;
				   else if(ImputationMethod ==9)
					   k = k_SVD = 6;
				   else if(ImputationMethod ==10)
					   k = k_SVD = 6;
				   */
				   
				   //FT5
			/*	   if(ImputationMethod ==2)
					   k = k_SVD = 4;
				   else if(ImputationMethod ==3)
					   k = k_SVD = 4;				   
				   else if(ImputationMethod ==8)
					   k = k_SVD = 10;
				   else if(ImputationMethod ==9)
					   k = k_SVD = 4;
				   else if(ImputationMethod ==10)
					   k = k_SVD = 10;*/
				   
				   
		        
				    ///build the model, each time with diff svd file
			   	  	callBuildModel(svdFile);			   	  	
			   	  	buildModel_SVD();								//build simple SVD model
			   	  	   
			   	  	//test with MemHelper
			        testWithMemHelper(myTestMMh, N);			        
			         // System.out.println( ", Neigh = "+ N + ",Dim=" + d + ",MAE= "+ MAE);
			         // System.out.println(MAE);
			        					
			    /*	if(mm>2)
		        		System.out.println("mm="+ mm + ", pval="+ pValue);
		        	*/
			    	
			          //--------------------------------
			          //calculate results
				        array_MAE[fold-1][neigh][dim]			= MAE;
				        array_MAEPerUser[fold-1][neigh][dim]	= MAEPerUser;				        
				        array_RMSE[fold-1][neigh][dim]			= RMSE;
				        array_ROC[fold-1][neigh][dim]			= Roc;
				        
					   //get variance (only if fold =1, or 20-80)
				        if(myTotalFolds ==1)
				        {				        
					        gridResults_Sd_MAE [neigh][dim] 		= SDInMAE;
						    gridResults_Sd_MAEPerUser [neigh][dim] 	= SDInMAE;		//need to make changes in RMSE if I need SDInRMSE etc
						    gridResults_Sd_RMSE[neigh][dim] 		= SDInMAE;
						    gridResults_Sd_ROC [neigh][dim] 		= SDInROC;
				        }	 
					    	 
				        // top-N with N=8
				        for(int x =0;x<8;x++)
				        {
					        array_Precision[x][fold-1][neigh][dim]	= precision[x];
					        array_Recall[x][fold-1][neigh][dim]		= recall[x];
					        array_F1[x][fold-1][neigh][dim]			= F1[x];

						    //get variance (only if fold =1, or 20-80)
					        if(myTotalFolds ==1)
					        {	
						   	    gridResults_Sd_Precision[x][neigh][dim] = SDInTopN_Precision[x]; 
						        gridResults_Sd_Recall[x][neigh][dim]    = SDInTopN_Recall[x];
						        gridResults_Sd_F1[x][neigh][dim]        = SDInTopN_F1[x];
					        }
					   	 
				        }
				    	
		          } //end k for 
		        
		        System.out.print(dim+",");
		        
		            //System.out.println("--------------------------------------------------");
	    
	    	} //end neighbour for
    	   } //end of fold for
   		
   	    //Store the MAE, at the [0][0] and print after each loop
	    //Get the optimal alpha, beta from here, and then, change
	    //all parameters to optimal and run the hybrid approach and 
	    //see results of F1, etc (MAy have to write them into a file, for easy graphs)
	 
   		if(myChoice.equalsIgnoreCase("UBIBCF") ||
   		   myChoice.equalsIgnoreCase("Switch"))
   		{
			  double tempMeanForHybrid[]		 = new double[myTotalFolds]; 
			  double tempMeanPerUserForHybrid[]  = new double[myTotalFolds];
		   	  for(int f=0;f<myTotalFolds;f++)
			  {
		   		  tempMeanForHybrid[f]  		=  array_MAE[f][0][0];
		   		  tempMeanPerUserForHybrid[f]   =  array_MAE[f][0][0];
			      
			  }		   	  
		   	  
		   	
		   	//System.out.println(alpha + ", "+ beta + " ="+ calculateMeanOrSD(tempMeanForHybrid, 1, 0));						
		   	System.out.println(alpha + ", "+ beta + " ="+ MAE);
		   	System.out.println("ROC="+ Roc);
		   	System.out.println("precision="+ precision[3]+", recall="+recall[3]+", F1="+F1[3]);
			   // System.out.println("opened and closed pointers");
   		}
	    
      }//end of outer if
    }//end of ub coff
   }//end of ib coff
    
    	System.out.println("Done with"+ mm);
    	
    	//-------------------------------------------------------------
    	//finished calculating results for each fold, now we can find mean and
    	//sd and can store it in gridResults
    	
    	double tempMAE[] 		= new double[myTotalFolds];		  	   //will store temp values
    	double tempMAEPerUser[]	= new double[myTotalFolds];		    
    	double tempRMSE[] 		= new double[myTotalFolds];		
    	double tempROC[] 		= new double[myTotalFolds];		
    	double tempPrecision[][] 	= new double[8][myTotalFolds];		//[topN][fold]
    	double tempRecall[][] 		= new double[8][myTotalFolds];		
    	double tempF1[][]		 	= new double[8][myTotalFolds];		
    	
    	
	    for(int neigh=0;neigh<=10;neigh++)  							//[10][15]
	    {    	
	      for(int dim=0;dim<=(upperDim/2);dim++)
	       {
	    	 for(int fold=0;fold<myTotalFolds;fold++)
	    	  {
	    		 tempMAE[fold] 		  =   array_MAE[fold][neigh][dim];
	    		 tempMAEPerUser[fold] =   array_MAEPerUser[fold][neigh][dim];
	    		 tempRMSE[fold] 	  =   array_RMSE[fold][neigh][dim];
	    		 tempROC[fold] 		  =   array_ROC[fold][neigh][dim];
	    		 
	    		 //topN with N=8
	    		 for(int x=0;x<8;x++)
	    		 {
		    		 tempPrecision[x][fold]   =   array_Precision[x][fold][neigh][dim];
		    		 tempRecall[x][fold] 	  =   array_Recall[x][fold][neigh][dim];
		    		 tempF1[x][fold] 		  =   array_F1[x][fold][neigh][dim];
	    		 }
	    		 
	    	  } //end for
	    	  
	    	 //calculate Means
	    	 gridResults_Mean_MAE [neigh][dim] = calculateMeanOrSD ( tempMAE, myTotalFolds, 0);
	    	 gridResults_Mean_MAEPerUser [neigh][dim] = calculateMeanOrSD ( tempMAEPerUser, myTotalFolds, 0);
	    	 gridResults_Mean_RMSE [neigh][dim] = calculateMeanOrSD ( tempRMSE, myTotalFolds, 0);
	    	 gridResults_Mean_ROC [neigh][dim] = calculateMeanOrSD ( tempROC, myTotalFolds, 0);

    		 //topN with N=8
	    	 for(int x=0;x<8;x++)
	    	 {
		    	 gridResults_Mean_Precision[x][neigh][dim] = calculateMeanOrSD (tempPrecision[x], myTotalFolds, 0);
		    	 gridResults_Mean_Recall[x][neigh][dim] = calculateMeanOrSD ( tempRecall[x], myTotalFolds, 0);
		    	 gridResults_Mean_F1 [x][neigh][dim] = calculateMeanOrSD ( tempF1[x], myTotalFolds, 0);
	    	 }
	    	 
	    	//calculate sd
	    	 if(myTotalFolds >1)
	    	 {	
		    	 gridResults_Sd_MAE [neigh][dim] = calculateMeanOrSD ( tempMAE, myTotalFolds, 1);
		    	 gridResults_Sd_MAEPerUser [neigh][dim] = calculateMeanOrSD ( tempMAEPerUser, myTotalFolds, 1);
		    	 gridResults_Sd_RMSE[neigh][dim] = calculateMeanOrSD ( tempRMSE, myTotalFolds, 1);
		    	 gridResults_Sd_ROC [neigh][dim] = calculateMeanOrSD ( tempROC, myTotalFolds, 1);
	    	 	    	    		
		    	 //topN with N=8
		    	 for(int x=0;x<8;x++)
		    	 {
			    	 gridResults_Sd_Precision[x][neigh][dim] = calculateMeanOrSD ( tempPrecision[x], myTotalFolds, 1);
			    	 gridResults_Sd_Recall[x][neigh][dim] = calculateMeanOrSD ( tempRecall[x], myTotalFolds, 1);
			    	 gridResults_Sd_F1[x][neigh][dim] = calculateMeanOrSD ( tempF1[x], myTotalFolds, 1);
		    	 }	    	
	    	 }// end if 
	       }//end for
	    }//end of outer for    	
    
	  //--------------------------------------------------
	   //write Results
	   //I think, first comparison is ok with top-20, then you can further draw a 

	    if(myChoice.equalsIgnoreCase("UBIBCF")==false && myChoice.equalsIgnoreCase("Switch")==false) //do not write into file for hybrid approach
	    {
		   	openFiles(path);	    
		    writeDataIntoFiles();
		    closeFiles();
	    }	    
	    
	} //end else  (fold case)
  
    }//end of outer for, (which scheme)
  
   }//end of outer most for (controling sparsity) 	
  }//end of function

    /************************************************************************************************/
         		 
    		    /**
    		     * Using RMSE as measurement, this will compare a test set
    		     * (in MemHelper form) to the results gotten from the recommender
    		     *  
    		     * @param testmh the memhelper with test data in it   //check this what it meant........................Test data?///
    		     * @return the rmse in comparison to testmh 
    		     */

    		    public void testWithMemHelper(MemHelper testmh, int neighbours)     
    		    {
    		           		        
    		        IntArrayList users;
    				LongArrayList movies;
    				//IntArrayList coldUsers = coldUsersMMh.getListOfUsers();
    				//IntArrayList coldItems = coldItemsMMh.getListOfMovies();
    				double mov, pred,actual, uAvg;
    				String blank			 	= "";
    		        int uid, mid, total			= 0;    		       	
    		        int totalUsers				= 0;
    		        int totalExtremeErrors 		= 0 ;
    		        int totalEquals 			= 0;
    		        int totalErrorLessThanPoint5= 0;
    		        int totalErrorLessThan1 	= 0;    	        
    		        		        
    		        // For each user, make recommendations
    		        users = testmh.getListOfUsers();
    		        totalUsers= users.size();
    		        
    		        double uidToPredictions[][] = new double[totalUsers][101]; // 1-49=predictions; 50-99=actual; (Same order); 100=user average
    		        
    		        //________________________________________
    		        
    		        for (int i = 0; i < totalUsers; i++)        
    		        {
    		        	uid = users.getQuick(i);    
    		        	
    		        	//if(coldUsers.contains(uid))
    		        	{   
	    		            movies = testmh.getMoviesSeenByUser(uid);
	    		           // System.out.println("now at " + i + " of total " + totalUsers );
	    		            
	    		            for (int j = 0; j < movies.size(); j++)             
	    		            {	    		            
	    		            	total++;
	    		                mid = MemHelper.parseUserOrMovie(movies.getQuick(j));
	    		             
	    		              //  if(coldItems.contains(mid))
	    		                {
	    		           //     if (mid ==-1)  System.out.println(" rating error--> uid, mid -->" + uid + "," + mid );
	    		                
	    		               // double rrr = recommend(uid, mid, blank);                
	    		                double rrr = recommend(uid, mid, neighbours);
	    		                
	    		                /*//Add values to Pair-t
	    		                if(ImputationMethod ==2)
	    		                	rmse.addActualToPairT(rrr);
	    		                else
	    		                	rmse.addPredToPairT(rrr);
	    		                */
	    		                
	    		                double myRating=0.0;
	    		                
	    		                //if (rrr!=0.0)                 
	    		                      {
	    		                	
	    		                			myRating = testmh.getRating(uid, mid);			 		// get actual ratings?
	
	    		                			//System.out.println(rrr+", "+ myRating);
	    		                            
	    		                			if (myRating==-99 )                           
	    		                               System.out.println(" rating error, uid, mid, rating" + uid + "," + mid + ","+ myRating);
	    		                           
	    		                            if(rrr>5 || rrr<=0)
	    		                            {
	    		         /*                   		System.out.println("Prediction ="+ rrr + ", Original="+ myRating+ ", mid="+mid 
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
	    		                           // if(rrr!=0)
	    		                            	rmse.ROC4(myRating, rrr, myClasses, myTrainingMMh.getAverageRatingForUser(uid));		
	    		                            	//rmse.ROC4(myRating, rrr, myClasses, TopNThreshold);
	    		                		                          
	    		                            //-------------
	    		                            //Add Error
	    		                            //-------------
	    		                           
	    		                           // if(rrr!=0)
	    		                            {
	    		                            	rmse.add(myRating,rrr);                            	
	    		                            	midToPredictions.put(mid, rrr);                            	                                
	    		                            }		
	    		            
	    		                            //-------------
	    		                            //Add Coverage
	    		                            //-------------
	
	    		                             rmse.addCoverage(rrr);                                 
	    		                		  }         
	    		                }
	    		            } //end of movies for
	    		            
	    		            //--------------------------------------------------------
	    		            //A user has ended, now, add ROC and reset
	    		            rmse.addROCForOneUser();
	    		            rmse.resetROCForEachUser();
	    		            rmse.addMAEOfEachUserInFinalMAE();
	    		            rmse.resetMAEForEachUser();
	    		            
	    		            //sort the pairs (ascending order)
	    		    		IntArrayList keys = midToPredictions.keys();
	    		    		DoubleArrayList vals = midToPredictions.values();        		
	    		    		midToPredictions.pairsSortedByValue(keys, vals);
	    		    		
	    		    		int movSize = midToPredictions.size();
	    		    		if(movSize>50)
	    		    			movSize = 50;      	
	    		    		 
	    		    		for(int x=0;x<movSize;x++)
	    		    		{
	    		    		  mov = keys.getQuick(x);
	    		    		  pred = vals.getQuick(x);
	    		    		  actual = testmh.getRating(uid,(int) mov);	
	    		    		  uidToPredictions[i][x] = pred;
	    		    		  uidToPredictions[i][50+x] = actual;
	    		    		}//end for
	    		    	    
	    		    		 uidToPredictions[i][100] = myTrainingMMh.getAverageRatingForUser(uid);
	    		    		 midToPredictions.clear();
    		        	 } //end of if (cold start checkng) 
	    		       } //end of user for	   
	    		    
	    		        MAE		 	= rmse.mae(); 
	    		        SDInMAE		= rmse.getMeanErrorOfMAE();
	    		        SDInROC 	= rmse.getMeanSDErrorOfROC();
	    		        Roc 		= rmse.getSensitivity();
	    		        MAEPerUser 	= rmse.maeFinal();
	    		        RMSE 		= rmse.rmse();
	    		       /* if(ImputationMethod >2)
	    		        	pValue  = rmse.getPairT();
	    		        */
	    		        
	    		         //-------------------------------------------------
	    		         //Calculate top-N    		            
	    		    		
	    		            for(int i=0;i<8;i++)	//N from 5 to 30
	    		            {
	    		            	for(int j=0;j<totalUsers;j++)//All users
	    		            	{
	    		            		//get user avg
	    		            		uAvg =  uidToPredictions [j][100];	
	    		            		
	    		            		for(int k=0;k<((i+1)*5);k++)	//for topN predictions
	    		            		{
	    		            			//get prediction and actual vals
	    		    	        		pred =  uidToPredictions [j][k];
	    		    	        		actual =  uidToPredictions [j][50+k];
	    		    	        		
	    		    	        		//add to topN
	    		    	        		   rmse.addTopN(actual, pred, myClasses, uAvg);
	    		    	        		 // rmse.addTopN(actual, pred, myClasses, TopNThreshold);
	    		            		}
	    		            		
	    		            		//after each user, first add TopN, and then reset
	    		            		rmse.AddTopNPrecisionRecallAndF1ForOneUser();
	    		            		rmse.resetTopNForOneUser();   		            		
	    		            	
	    		            	} //end for
	    		            	
	    		            	//Now we finish finding Top-N for a particular value of N
	    		            	//Store it 
	    		            	precision[i]=rmse.getTopNPrecision();
	    		            	recall[i]=rmse.getTopNRecall();
	    		            	F1[i]=rmse.getTopNF1(); 
	    		            	
	    		            	//Get variance 
	    		            	SDInTopN_Precision[i] = rmse.getMeanSDErrorOfTopN(1); 
	    		            	SDInTopN_Recall[i] = rmse.getMeanSDErrorOfTopN(2);
	    		            	SDInTopN_F1 [i]= rmse.getMeanSDErrorOfTopN(2);
	    		            	
	    		            	//Reset all topN values    		            	
	    		            	rmse.resetTopNForOneUser();
	    		            	rmse.resetFinalTopN();
	    		       		            
    		            } //end of for   		        	
    		        	
    		     /* System.out.println("totalExtremeErrors="+totalExtremeErrors + ", Total ="+total);
    		        System.out.println("totalErrorLessThanPoint5="+totalErrorLessThanPoint5 );	       
    		        System.out.println("totalErrorLessThan1="+totalErrorLessThan1 );
    		        System.out.println("totalEquals="+totalEquals );  */    		        
    		        
    		        //Reset final values
    		        rmse.resetValues();   
    		        rmse.resetFinalROC();
    		        rmse.resetFinalMAE();
    		        /*if(ImputationMethod >2)
    		        	rmse.resetPairTPrediction();*/
    		       
     }//end of function
    	  	
   
 /***************************************************************************************************/
    		        
    		        /**
    		         * calculate Mean or SD of array of values
    		         * @param double[], values 
    		         * @param int, no of values
    		         * @param int, 0=mean and 1=sd
    		         * @return mean or sd
    		         */
    		    	    
    		    	 public double calculateMeanOrSD(double val[], int size, int whatToCalculate)
    		    	 {
    		    		// System.out.println("fold size="+ size);
    		    		 
    		    		 if(size==1)
    		    			 return val[0];
    		    		 
    		    		 double mean =0;
    		    		 double sd =0;
    		    		 double ans =0;
    		    		 
    		    		 //calculate mean
    		    		 for (int i=0;i<size;i++)
    		    		 {
    		    			 mean +=val[i];
    		    		 }
    		    		 
    		    			mean= mean/size;			//This is mean
    		    			
    		    		 //choose what to claculate based on flag
    		    		 if(whatToCalculate ==0)//mean
    		    			 ans = 	mean; 
    		    			 
    		    		 else //SD
    		    		 {
    		    			 for(int i=0;i<size;i++)
    		    			 {
    		    				 sd+= Math.pow((val[i] - mean), 2);
    		    			 }
    		    			 
    		    			 if(size==1)
    		    				 ans= Math.sqrt(sd);
    		    			 else
    		    				 ans= Math.sqrt(sd/(size-1));
    		    		 }
    		    		
    		    		 return ans;
    		    	 }
    		    	 
/***************************************************************************************************/
    /**
     * Files
     */
    	
    
  public void writeDataIntoFiles()
  {
    	 int end =0;
    	 int i=0, j=0;
   try{
    	
   		 //write column labels
   		 for(int dim =0;dim<=upperDim;dim+=2)    			 
   		 {
   			 
    	   for(i=0;i<10;i++)
    	 	{ 				 
    		     if (i == 0 || i == 1)						//cntrol j				
    		        	end = 4;
    		        else if (i==2 || i==3)
    		    	   	end =3;
    		        else if(i>3 && i<=9)
      		        	end =8;
      		        else
      		        	end=1;
    		     
    		  for(j=0;j<end;j++) 
    		   {
	    	
    			  if(dim<=(upperDim-2)) //Just to write "\n" at end of column
    			  {
	    			   if(dim==0)
		    				 myWriter[i][j].append("");
		    			 else
		    				 myWriter[i][j].append(""+dim);
		    				 
	    		 	   myWriter[i][j].append(",");
    			  }
    			  else
    		 		   myWriter[i][j].append("\n");  //start writing values
    		 	  
    		   } //end for	
    	 	 }//end for   	   
   		 }//end outer for
   		 
    //control neigboring limit    
    int neighbourLimit =0;
    if(myChoice.equalsIgnoreCase("SVDRec")){   	 
    	neighbourLimit = 1;	    	
    }    
    else {    	 
    	neighbourLimit =10;
    }  		     		 
    
    int topNLimit =0;
    for(i=0;i<10;i++)
    { 				 
        if (i == 0 || i == 1)						//cntrol j				
        	end = 4;
        else if (i==2 || i==3)
    	   	end =3;
        else if(i>3 && i<=9)
	        	end =8;
	        else
	        	end=1;
	    	
        if(i==2 || i==3)			//control, wehn we have to write topN in one file
        	topNLimit =5;
        else 
        	topNLimit =1;
        
    	  for(j=0;j<end;j++) 
    	  { 
    		 for(int neigh=0; neigh< neighbourLimit ; neigh++)
    	      {
    			 				 
			  for(int top =0;top<topNLimit;top++)
			  {
				  	//write major row labels		    				
					 myWriter[i][j].append(""+ ((neigh+1)*5));
					 myWriter[i][j].append(",");	
					 
    	    	for(int dim=0; dim<=(upperDim/2);dim++) 	 //at 16 write "\n"
    	    	 {    	    	    			
    	    		  if(dim<=((upperDim/2)-1))				//write values
    	    		   {		    				 
		    				 //start writing values
		    				 double result =0;		    			    				 
		    				 
		    				 if	(i==0)
		    				 {
			    				 if(j==0) 		result = gridResults_Mean_MAE[neigh][dim];
			    				 else if(j==1) 	result = gridResults_Mean_MAEPerUser[neigh][dim];
			    				 else if(j==2)  result = gridResults_Mean_RMSE[neigh][dim];
			    				 else if(j==3)  result = gridResults_Mean_ROC[neigh][dim];
		    				 }
		    				 
		    				 else if(i==1)		    					 
		    				 {
			    				 if(j==0)	    result = gridResults_Sd_MAE[neigh][dim];    					 
			    				 else if(j==1)	result = gridResults_Sd_MAEPerUser[neigh][dim];
			    				 else if(j==2)  result = gridResults_Sd_RMSE[neigh][dim];
			    			     else if(j==3)  result = gridResults_Sd_ROC[neigh][dim];
		    				 }
		    				 
		    				 else if(i==2)
		    				 {
		    					 if(j==0)  		result = gridResults_Mean_F1[top][neigh][dim];		    					 
		    					 else if(j==1)  result = gridResults_Mean_Precision[top][neigh][dim];
		    					 else if(j==2)  result = gridResults_Mean_Recall[top][neigh][dim];
		    				 }
		    				 
		    				 else if(i==3)		    					 
		    				 { 
		    					 	 if(j==0)  		result = gridResults_Sd_F1[top][neigh][dim];
				    				 else if(j==1)  result = gridResults_Sd_Precision[top][neigh][dim];
				    				 else if(j==2)  result = gridResults_Sd_Recall[top][neigh][dim];
		    				 }
		    				
		    				 // j=0, topn5; j=1, top10 and so on
		    				 // in i=4, I have Mean_F1; i=5, Mean_Precision and so on
		    				 else if(i==4)  result = gridResults_Mean_F1[j][neigh][dim];		    				 
		    				 else if(i==5)  result = gridResults_Mean_Precision[j][neigh][dim];		    				 
		    				 else if(i==6)  result = gridResults_Mean_Recall[j][neigh][dim];		    				 
		    				 else if(i==7)  result = gridResults_Sd_F1[j][neigh][dim];		    				 
		    				 else if(i==8)  result = gridResults_Sd_Precision[j][neigh][dim];		    				 
		    				 else if(i==9)  result = gridResults_Sd_Recall[j][neigh][dim];
		    				 else if(i==10)  result = 1;
			    				
		    			
		    				 myWriter[i][j].append(nf.format(result));
		    				 myWriter[i][j].append(",");
    	    			  }
    	    			  
    	    			  else			//write "\n"
    	    				  myWriter[i][j].append("\n");
    	    			  
    	    		   }//end inner for
    			    }//end for   			
    		    }//end dim for
    	      }//end of topN for
    	   }//end neig for
    	 }   	 
		 catch(Exception E){
			 E.printStackTrace();
			 System.exit(1); 
		 }

    	 
     }
    		    	 
     //------------------------------------------------    		    	 
    		    	 
	    	 public void openFiles(String path)
	    	 {
	    		if(FTFlag==false){
	    			if(sparse ==false)
	    				path +="Results/" + trainingOrFullSet + "/" + modelNormalization + "/"  + myChoice +  "/" + coldStartThreshold+"/";
	    			else
	    				path +="Results/" + trainingOrFullSet + "/" + modelNormalization + "/"  + myChoice +  "/Sparse/";
	    		}
	    		else
	    			path += modelNormalization + "/" + FTDataWithMinRat + "/" + "Results/"  + trainingOrFullSet + "/"
	    			+				"" + "/" + myChoice +  "/" ;   //+ "All/";
	    		 
	    		 try{
	    			 myWriter[0][0] = new FileWriter(path +trainingOrFullSet+ "_"+ "MeanMAE"+ myChoice+ modelNormalization+ ".csv", true); //true, append in exisiting file
	    			 myWriter[0][1] = new FileWriter(path +trainingOrFullSet+ "_"+"MeanMAEPerUser"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[0][2] = new FileWriter(path +trainingOrFullSet+ "_"+"MeanRMSE"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[0][3] = new FileWriter(path +trainingOrFullSet+ "_"+"MeanROC"+ myChoice+ modelNormalization+".csv", true);
	    			 
	    			 myWriter[1][0] = new FileWriter(path +trainingOrFullSet+ "_"+"SDMAE"+ myChoice+ modelNormalization+".csv", true); //true, append in exisiting file
	    			 myWriter[1][1] = new FileWriter(path +trainingOrFullSet+ "_"+"SDMAEPerUser"+ myChoice+ modelNormalization+".csv", true); //true, append in exisiting file
	    			 myWriter[1][2] = new FileWriter(path +trainingOrFullSet+ "_"+"SDRMSE"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[1][3] = new FileWriter(path +trainingOrFullSet+ "_"+"SDROC"+ myChoice+ modelNormalization+".csv", true);

	    			//One file for each topN, precision, recall , and F1 (total 3 files)
	    			//In each we will write topN with N from 5 to 25 (diff of 5)
	    			 myWriter[2][0] = new FileWriter(path +trainingOrFullSet+ "_"+"MeanF1"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[2][1] = new FileWriter(path +trainingOrFullSet+ "_"+"MeanPrecision"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[2][2] = new FileWriter(path +trainingOrFullSet+ "_"+"MeanRecall"+ myChoice+ modelNormalization+".csv", true);
	    			 
	    			 myWriter[3][0] = new FileWriter(path +trainingOrFullSet+ "_"+"SDF1"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[3][1] = new FileWriter(path +trainingOrFullSet+ "_"+"SDPrecision"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[3][2] = new FileWriter(path +trainingOrFullSet+ "_"+"SDRecall"+ myChoice+ modelNormalization+".csv", true);
	    			 
	    			 
	    			 //topnN
	    			 myWriter[4][0] = new FileWriter(path +trainingOrFullSet+ "_"+"MeanF1_5"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[4][1] = new FileWriter(path +trainingOrFullSet+ "_"+"MeanF1_10"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[4][2] = new FileWriter(path +trainingOrFullSet+ "_"+"MeanF1_15"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[4][3] = new FileWriter(path +trainingOrFullSet+ "_"+"MeanF1_20"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[4][4] = new FileWriter(path +trainingOrFullSet+ "_"+"MeanF1_25"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[4][5] = new FileWriter(path +trainingOrFullSet+ "_"+"MeanF1_30"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[4][6] = new FileWriter(path +trainingOrFullSet+ "_"+"MeanF1_35"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[4][7] = new FileWriter(path +trainingOrFullSet+ "_"+"MeanF1_40"+ myChoice+ modelNormalization+".csv", true);
	    			 
	    			 myWriter[5][0] = new FileWriter(path +trainingOrFullSet+ "_"+"MeanPrecision_5"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[5][1] = new FileWriter(path +trainingOrFullSet+ "_"+"MeanPrecision_10"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[5][2] = new FileWriter(path +trainingOrFullSet+ "_"+"MeanPrecision_15"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[5][3] = new FileWriter(path +trainingOrFullSet+ "_"+"MeanPrecision_20"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[5][4] = new FileWriter(path +trainingOrFullSet+ "_"+"MeanPrecision_25"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[5][5] = new FileWriter(path +trainingOrFullSet+ "_"+"MeanPrecision_30"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[5][6] = new FileWriter(path +trainingOrFullSet+ "_"+"MeanPrecision_35"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[5][7] = new FileWriter(path +trainingOrFullSet+ "_"+"MeanPrecision_40"+ myChoice+ modelNormalization+".csv", true);
   			 
	    			 
	    			 myWriter[6][0] = new FileWriter(path +trainingOrFullSet+ "_"+"MeanRecall_5"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[6][1] = new FileWriter(path +trainingOrFullSet+ "_"+"MeanRecall_10"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[6][2] = new FileWriter(path +trainingOrFullSet+ "_"+"MeanRecall_15"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[6][3] = new FileWriter(path +trainingOrFullSet+ "_"+"MeanRecall_20"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[6][4] = new FileWriter(path +trainingOrFullSet+ "_"+"MeanRecall_25"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[6][5] = new FileWriter(path +trainingOrFullSet+ "_"+"MeanRecall_30"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[6][6] = new FileWriter(path +trainingOrFullSet+ "_"+"MeanRecall_35"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[6][7] = new FileWriter(path +trainingOrFullSet+ "_"+"MeanRecall_40"+ myChoice+ modelNormalization+".csv", true);
	    			 
	    			 
	    			 // SD
	    			 myWriter[7][0] = new FileWriter(path +trainingOrFullSet+ "_"+"SDF1_5"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[7][1] = new FileWriter(path +trainingOrFullSet+ "_"+"SDF1_10"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[7][2] = new FileWriter(path +trainingOrFullSet+ "_"+"SDF1_15"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[7][3] = new FileWriter(path +trainingOrFullSet+ "_"+"SDF1_20"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[7][4] = new FileWriter(path +trainingOrFullSet+ "_"+"SDF1_25"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[7][5] = new FileWriter(path +trainingOrFullSet+ "_"+"SDF1_30"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[7][6] = new FileWriter(path +trainingOrFullSet+ "_"+"SDF1_35"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[7][7] = new FileWriter(path +trainingOrFullSet+ "_"+"SDF1_40"+ myChoice+ modelNormalization+".csv", true);
	    			 
	    			 myWriter[8][0] = new FileWriter(path +trainingOrFullSet+ "_"+"SDPrecision_5"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[8][1] = new FileWriter(path +trainingOrFullSet+ "_"+"SDPrecision_10"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[8][2] = new FileWriter(path +trainingOrFullSet+ "_"+"SDPrecision_15"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[8][3] = new FileWriter(path +trainingOrFullSet+ "_"+"SDPrecision_20"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[8][4] = new FileWriter(path +trainingOrFullSet+ "_"+"SDPrecision_25"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[8][5] = new FileWriter(path +trainingOrFullSet+ "_"+"SDPrecision_30"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[8][6] = new FileWriter(path +trainingOrFullSet+ "_"+"SDPrecision_35"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[8][7] = new FileWriter(path +trainingOrFullSet+ "_"+"SDPrecision_40"+ myChoice+ modelNormalization+".csv", true);
	    			 
	    			 myWriter[9][0] = new FileWriter(path +trainingOrFullSet+ "_"+"SDRecall_5"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[9][1] = new FileWriter(path +trainingOrFullSet+ "_"+"SDRecall_10"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[9][2] = new FileWriter(path +trainingOrFullSet+ "_"+"SDRecall_15"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[9][3] = new FileWriter(path +trainingOrFullSet+ "_"+"SDRecall_20"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[9][4] = new FileWriter(path +trainingOrFullSet+ "_"+"SDRecall_25"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[9][5] = new FileWriter(path +trainingOrFullSet+ "_"+"SDRecall_30"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[9][6] = new FileWriter(path +trainingOrFullSet+ "_"+"SDRecall_35"+ myChoice+ modelNormalization+".csv", true);
	    			 myWriter[9][7] = new FileWriter(path +trainingOrFullSet+ "_"+"SDRecall_40"+ myChoice+ modelNormalization+".csv", true);
		    		
	    			 myWriter[10][0] = new FileWriter(path +trainingOrFullSet+ "_"+"PValue"+ myChoice+ modelNormalization+".csv", true);
			    		
	    			 
	    		 }
	    		 catch(Exception E){
	    			 E.printStackTrace();
	    			 System.exit(1); 
	    		 }
	    		 
	    		 
	    	 }//end function
	    	 
	  //---------------------------------------------------------
	    	 
	   public void closeFiles()
	   {
		   int end =0;
			 
  		 try{
  			 
  			 for(int i=0;i<10;i++)
  			 {
  			      if (i == 0 || i == 1)						//cntrol j				
  		        	end = 4;
  		        else if (i==2 || i==3)
  		    	   	end =3;
  		        else if(i>3 && i<=9)
  		        	end =8;
  		        else
  		        	end=1;
  			      
  			      
  				 
  				 for(int j=0;j<end;j++)
  				 {
  					 //Flush and close the writer
  					 myWriter[i][j].flush();
  					 myWriter[i][j].close();
  				 }
  			 }
  			 
  		 }
  		 catch(Exception E){
  			 E.printStackTrace();
  			 System.exit(1); 
  		 }
  		 
  		 
		   
	   }
    		    	 
    		    	    
}//end of class