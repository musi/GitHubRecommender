package netflix.recommender;

import java.io.*;
import java.util.*;
import netflix.memreader.*;
import netflix.rmse.RMSECalculator;
import netflix.utilities.*;
import cern.colt.list.IntArrayList;
import cern.colt.list.LongArrayList;
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
public class SVDRecommender  
/****************************************************************************************************/
{

    private SingularValueDecomposition 	svd;
    private DoubleMatrix2D 				P;
    private int 						k;
    MemHelper                           MMhTrain;
    MemHelper                           MMhTest;
    String  							myPath;
    int 								totalNegSVDPred;
    int 								totalPosSVDPred;
    int 								totalZeroSVDPred;
    
/****************************************************************************************************/
    
    /**
     * Constructor. 
     *
     * @param  memReaderFile  File containing serialized MemReader.
     * @param  svdFile  File containing serialized SVD.
     * @param  k  Number of singular values to use.
     */

    public SVDRecommender(MemHelper train, MemHelper test, String svdFile, int k) 
    {
 
    	myPath ="C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\SVD\\";
    	
        try 
        {
            this.k  			 = k;
            MMhTrain 			 = train;
            MMhTest 			 = test;
            totalNegSVDPred		 = 0;
            totalPosSVDPred		 = 0;
            totalZeroSVDPred	 = 0;
            
       
         }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    
    //---------------------------------
    
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
            Algebra alg = new Algebra();

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
            
            //compute left and right by multiplying US, and SV'           
           // DoubleMatrix2D left  = alg.mult(Uk, rootSkPrime);
            DoubleMatrix2D left  = alg.mult(Uk, rootSk);
            DoubleMatrix2D right = alg.mult(rootSk, VPrimek);

            // Multiply [(US)(SV')]
            P = alg.mult(left, right);
            
            
    /*        // Compute U and V'
            DoubleMatrix2D U  = svd.getU();	// As we have rows>cols in svd matrix, so it should contain movies????
            DoubleMatrix2D Uk = U.viewPart(0, 0, U.rows(), k).copy(); // (int row, int column, int height, int width) 

            DoubleMatrix2D VPrime  = alg.transpose(svd.getV());
            DoubleMatrix2D VPrimek = VPrime.viewPart(0, 0, k, VPrime.columns()).copy();

            //compute left and right by multiplying US, and SV'
            DoubleMatrix2D left  = alg.mult(Uk, rootSk);
            DoubleMatrix2D right = alg.mult(rootSk, VPrimek);

            // Multiply [(US)(SV')]
            P = alg.mult(left, right);
            */
            //We then multiplied the matrices UkSk1/2 and Sk1/2V'k  producing a 943 x 1682 matrix, P.
            // No it shuld be 1682 x 943?
            
            //----------------------------------------------------------------------------------------
            // AS it was requirement of colt to rows>cols, so in the rows we have movies now. Behaviour
            // of U and V should be changed
            
    
            
    }

/****************************************************************************************************/
    
    /**
     * Predicts the rating that activeUser will give targetMovie.
     *
     * @param  activeUser  The user.
     * @param  targetMovie  The movie.
     * @param  date  The date the rating was given. 
     * @return The rating we predict activeUser will give to targetMovie. 
     */
    public double recommend(int activeUser, int targetMovie) 
    {
    	double entry=0;
    	double prediction =0;
    	
        if ( activeUser<943 && targetMovie <1682)
        {
        	// Entry is retrieved in the correct way, i.e. rows x cols = movs x users
        	   entry = P.get(targetMovie-1, activeUser-1);
        	
        	 
        	 double avg = MMhTrain.getAverageRatingForUser(activeUser);
        //	 prediction = entry + avg;
        	 prediction = entry;
        	 
        	 if(entry <0) totalNegSVDPred ++;
        	 if(entry >0) totalPosSVDPred ++;
        	 if(entry == 0) totalZeroSVDPred ++;
        		// System.out.println("entry + avg = pred-->"+ entry + " + "+ avg + "=" + prediction);
        }	
        
   /*     if(prediction < 1)
            return 1;
        else if(prediction > 5)
            return 5;
        else
            return prediction;*/
        
        //System.out.println("pred="+ prediction);
        return prediction;
    }

    
  //-----------------------------------------------
    
    /**
     * Tests this method and computes rmse.
     */
    
    public static void main(String[] args) 
    {
    	String path ="C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\SVD\\FeaturesPlay\\";
        
    	//String base = "/Users/bsowell/recommender/movielens/0indexed/uabase.dat";
        //String test = "/Users/bsowell/recommender/movielens/0indexed/uatest.dat";
        
    	String test = path + "sml_clusteringTestSetStoredTF.dat";
    	String base = path + "sml_clusteringTrainSetStoredTF.dat";
    	String svdFile = path + "\\Simple\\SVDSVMSimple.dat";
        
        
        MemHelper mhTest = new MemHelper(test);
        MemHelper mhTrain = new MemHelper(base);
       
        for(int i=5;i<300;i+=20)
        {
	        SVDRecommender svdRec = new SVDRecommender(mhTrain, mhTest,  svdFile, i);
	        System.out.print(i+ "@ ");
	        svdRec.makeRec(svdFile);
        }
    }
        //----------------------------
        
	 
    public void makeRec(String svdFile)
    {	        
    	    callBuildModel(svdFile);
    	    System.out.println("MAE: " + testWithMemHelper(MMhTest, 20));     
	   
       }
        
        
     

    
    //-----------------------------------------------
    
    public double testWithMemHelper(MemHelper testmh, int neighbours)     
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
                  double rrr = recommend(uid, mid);
                
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
   
        double dd= rmse.mae();
        //rmse.resetValues();        
        return dd;
    }

}