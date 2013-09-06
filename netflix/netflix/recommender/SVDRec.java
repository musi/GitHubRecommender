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
 * Then aspply your concept (filling) and compare SVD with simple SVD, and simple CF.
 *                                                                                 
 *                                              
 * Make this program generic, i.e. it should return SVD, UBCF, IBCF results.
 * 
 *      
 *                                              


*/
//-----------------------------------------------------------------------------------------

import java.io.*;

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
public class SVDRec  
/****************************************************************************************************/
{

    private SingularValueDecomposition 	svd;
    private DoubleMatrix2D 				Prediction_Matrix;	// p =left * right
    DoubleMatrix2D						left;				// left = US
    DoubleMatrix2D						right;				// right = SV
    Algebra 							alg;
    private int 						myK;
    
    //some objects
    String  							myPath;    	
    DataReader 							dataReader;		   //has methods like finding all users who have rated this item etc
    SimilarityMethod 					similarityMethod;  //e.g. demo sim, feature sim, etc.
    FilterAndWeight 					myFilter;		   //to find all users who have rated two items 
    MemHelper							myTrainingMMh;
    MemHelper							myTestMMh;
    
    //Regarding Results
    double 								MAE;

    //some parameter flags
    boolean 			fiveFoldFlag;
    String				myChoice;
    String				modelNormalization;
    
/****************************************************************************************************/
    
    /**
     * Constructor. 
     *
     * @param  memReaderFile  File containing serialized MemReader.
     * @param  svdFile  File containing serialized SVD.
     * @param  myK  Number of singular values to use.
     */

    public SVDRec (MemHelper trainingMMh,MemHelper testMMh,
    								int k) 
    {
    		myPath = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/";
    	
	    	MAE 			   = 0;	    	    	
	    	alg 			   = new Algebra();	    		
	    	myK				   = k;
        
            // For no folding, they stay the same, for folding, we change them within five fold loop
            myTrainingMMh = trainingMMh;
            myTestMMh = testMMh;
            
    
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
            
            System.out.println("done reading svd");
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
    	/*	left  = null; 
    		right = null;
    		P     = null;
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
            DoubleMatrix2D tempP = alg.mult(tempLeft, tempRight);
            
                        
            left  = alg.mult(Uk, rootSk);
            right = alg.mult(rootSk, VPrimek);

            // Multiply [(U * sqrt(S)) * (sqrt(S) * V')] = USV
            P = alg.mult(left, right);    
            
            
            left  = tempLeft.copy();
            right = tempRight.copy();
            P	  = tempP.copy();            
            */
            
        Algebra alg = new Algebra();

        DoubleMatrix2D rootSk = svd.getS().viewPart(0, 0, myK, myK);
              
        //compute singular value
        for(int i = 0; i < myK; i++) 
        {
          rootSk.set(i,i,Math.sqrt(rootSk.get(i,i)));  //Sets the matrix cell at coordinate [row,column] to the specified value. 
        }

        // Compute U and V'
        DoubleMatrix2D U  = svd.getU();	
        DoubleMatrix2D Uk = U.viewPart(0, 0, U.rows(), myK).copy(); // (int row, int column, int height, int width) 

        DoubleMatrix2D VPrime = alg.transpose(svd.getV());
        DoubleMatrix2D VPrimek = VPrime.viewPart(0, 0, myK, VPrime.columns()).copy();
        DoubleMatrix2D rootSkPrime = alg.transpose(rootSk);
        
        //compute left and right by multiplying US, and SV'           
       // DoubleMatrix2D left  = alg.mult(Uk, rootSkPrime);
        DoubleMatrix2D left  = alg.mult(Uk, rootSk);
        DoubleMatrix2D right = alg.mult(rootSk, VPrimek);

        // Multiply [(US)(SV')]
        Prediction_Matrix = alg.mult(left, right);
        
            
    /*        if(FOLD_VALUE==1)   
            	previous_P=P.copy();*/
            
            /*if(FOLD_VALUE==1)
            {
            	for(int i=0;i<900;i++);
            		//System.out.println("P="+ P.get(i, i) + ", Previous_P="+ previous_P.get(i, i));
            	
            	//System.out.println(P);
            }
            */
            
           System.out.println("came and build model");
    }

/****************************************************************************************************/
    
    /**
     * Decide which method to call, based on a global boolean
     */
    
    public double recommend (int activeUser, int targetMovie, int neighbours)
    {    	
    	
      		double entry=0, previous_Entry =0;
    		
        	double prediction =0;
        	
            if ( activeUser<943 && targetMovie <1682)
            {
           	 // Entry is retrieved in the correct way, i.e. rows x cols = movs x users
           	    entry = Prediction_Matrix.get(targetMovie-1, activeUser-1);	    // ML , FT
             // entry = P.get(activeUser-1, targetMovie-1);     // FT1, 2 and so on              
           	  
           	       
          /* 	     if(FOLD_VALUE==1){
           	    	previous_Entry = previous_P.get(targetMovie-1, activeUser-1);	    // ML , FT     	     
           	    	System.out.println("P="+ entry + ", Previous_P="+ previous_Entry);
           	     }
           	     */    	     
           	  
              		prediction = entry + myTrainingMMh.getAverageRatingForUser(activeUser);              	
            }             
           
           //System.out.println(FOLD_VALUE);
            return prediction;
    	 
     }

/****************************************************************************************************/            
/****************************************************************************************************/
            
    /**
     * Main method, just call another method for computing results 
     * Tests this method and computes rmse.
     */
    
    public static void main(String[] args) 
    { 
       //Set the parameters    	 
    	String modelNor = "";
        // modelNor = "Simple";
    	 modelNor = "UserNor";
       //modelNor = "MovNor";
         
    	 String  whichMethodToCall = "";    	 
    	// whichMethodToCall = "IBCF";
    	 //whichMethodToCall = "UBCF";
    	 whichMethodToCall = "SVD";
    	
    	 boolean foldingFlg  = false;
    	 
    	   
    	 //SML
    	String path = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/FeaturesPlay/80/";
    				 
    	String base = path + "sml_clusteringTrainSetStoredTF.dat";
    	String test = path + "sml_clusteringTestSetStoredTF.dat";
    	
    	//FT
    /*	String path = "C:/Users/Musi/workspace/MusiRecommender/DataSets/FT/SVD/";   	
    	String base = path + "ft_clusteringTrainSetStoredTF.dat";
    	String test = path + "ft_clusteringTestSetStoredTF.dat";
    */	    	
    	
    									  SVDRec svdRec = new SVDRec(new MemHelper (base),				//train obj 
    																 new MemHelper (test),      		//test obj 	
    																 10		//model Nor
													    			 );    	
    	 //call compute results
    	 svdRec.computerResults(path);
    }
    
/****************************************************************************************************/

    /**
     * Call other methods and Compute the results. 
     */
    
    public void computerResults(String path)
    {
    	
    	String svdFile = "";
     	String modelName = "";
    	
     	
    	 modelName = "SVDMovAvg";       
	     System.out.println("model ="+ modelName);        
	    
	    	//Normalized or simple
	         //
	   svdFile = path + "UserNor/SVDSVM_full_UserNor.dat";	    
	        //  svdFile = path + "UserNor/SVDSVMReg_full_UserNor.dat";
	     		//svdFile = path + "UserNor/SVDUserAvg_full_UserNor.dat";
    	      System.out.println("svd file="+svdFile);
    	      
    	      for (int k=1;k<40;k+=2)
    	      {
    	    	  this.myK= k;
	    	      
	    	    //build the model
	    	     callBuildModel(svdFile);
	    	       
	    	    //Loop over Dimensions = 1-25   
		 
	    	          
		          testWithMemHelper(myTestMMh);		          
		          System.out.println("Model =" +modelName + ", MAE @ myK = "+ k + " is= " + MAE);
		 			        
    	      } 
    }//end of function

    	  	
   //--------------------------------------------------------------
    		    
    		    public void testWithMemHelper(MemHelper testmh)     
    		    {
    		        RMSECalculator rmse = new RMSECalculator();
    		        
    		        IntArrayList users;
    				LongArrayList movies;
    		        String blank = "";
    		        int uid, mid, total=0;
    		        int totalUsers=0;
    		        
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
    		                  double rrr = recommend(uid, mid, 10);
    		                
    		                double myRating=0.0;
    		                
    		                //if (rrr!=0.0)                 
    		                      {
    		                	
    		                			myRating = testmh.getRating(uid, mid);			 		// get actual ratings?

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
    		   
    		     MAE = rmse.mae();
    		 }

/***************************************************************************************************/
 	 
    		    	    
}//end of class