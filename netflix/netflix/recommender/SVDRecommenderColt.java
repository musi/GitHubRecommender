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
public class SVDRecommenderColt 
/****************************************************************************************************/
{

    private SingularValueDecomposition 	svd;
    private DoubleMatrix2D 				P;
    private int 						k;
    int 								totalNegSVDPred;
    int 								totalPosSVDPred;
    int 								totalZeroSVDPred;
    String  							myPath;    	
    
    MemHelper mh;
    
    // Results
    double Roc;
    double MAE;
    double FPR;
    double Specificity;
    double Accuracy;
    double PPV, NPV, FDR, MCC; 
    
/****************************************************************************************************/
    
    /**
     * Constructor. 
     *
     * @param  memReaderFile  File containing serialized MemReader.
     * @param  svdFile  File containing serialized SVD.
     * @param  k  Number of singular values to use.
     */

    public SVDRecommenderColt(String memReaderFile, String svdFile, int k) 
    {
    	  this(new MemHelper(memReaderFile), svdFile, k);
    }

 /****************************************************************************************************/
    
    /**
     * Constructor. 
     *
     * @param  mh  		MemHelper object for training set. 
     * @param  svdFile  File containing serialized SVD.
     * @param  k 		Number of singular values to use.
     */
    public SVDRecommenderColt(MemHelper mh, String svdFile, int k) 
    {
    	//myPath ="C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\SVD\\";
    	myPath ="C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\FT\\SVD\\FeaturesPlay\\";
    	
        try 
        {
            this.k  = k;
            this.mh = mh;

            //check which SVD is called
            
            //Read SVD
            FileInputStream fis  = new FileInputStream(svdFile);
            ObjectInputStream in = new ObjectInputStream(fis);
            svd 				 = (SingularValueDecomposition) in.readObject();
            totalNegSVDPred		 = 0;
            totalPosSVDPred		 = 0;
            totalZeroSVDPred	 = 0;
            
            buildModel();
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
    
    public double recommend(int activeUser, int targetMovie, int neighbours) 
    {
    	double entry=0;
    	double prediction =0;
    	
       // if ( activeUser<943 && targetMovie <1682)
        {
        	// Entry is retrieved in the correct way, i.e. rows x cols = movs x users
        	 //  entry = P.get(targetMovie-1, activeUser-1);	    // ML , FT
        	     entry = P.get(activeUser-1, targetMovie-1);      // FT1, 2 and so on
        	 double avg = mh.getAverageRatingForUser(activeUser);
        	//     prediction = entry + avg;
        	   prediction = entry;
        	 
        	 if(entry <0) totalNegSVDPred ++;
        	 if(entry >0) totalPosSVDPred ++;
        	 if(entry == 0) totalZeroSVDPred ++;
        	//	System.out.println("entry + avg = pred-->"+ entry + " + "+ avg + "=" + prediction);
        }	
        
   /*     if(prediction < 1)
            return 1;
        else if(prediction > 5)
            return 5;
        else
            return prediction;*/
        
        return prediction;
    }

/****************************************************************************************************/    

    /**
     * Tests this method and computes rmse.
     */
    
    public static void main(String[] args) 
    {    

    	String path = "";   	
    	String test = "";
    	String base = "";
    	String svdFile = "";
    	
      /*path ="C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\SVD\\";    	
    	test = path + "sml_clusteringTestSetStoredTF.dat";
    	base = path + "sml_clusteringTrainSetStoredTF.dat";*/
    	
    	path ="C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\FT\\SVD\\";
    	test = path + "ft_clusteringTestSetStoredTF10.dat";
    	base = path + "ft_clusteringTrainSetStoredTF10.dat";
    	
    	String modelName = "";
    	
    	for(int m=0;m<15;m++)
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
    	    // svdFile = path + "\\UserNorm\\" + modelName + "UserNor.dat";

    	   //Simpe
    	     //svdFile = path + "\\Simple\\10\\" + modelName + "Simple.dat";
    	     
    	System.out.println("-------------------------------------------------");
        System.out.println(" Model = "+modelName);     
        System.out.println("-------------------------------------------------");
       
        for(int i=1;i<=15;i++)
        {
	        SVDRecommenderColt svdRec = new SVDRecommenderColt(base, svdFile, i);
	        MemHelper mhTest = new MemHelper(test);
	        System.out.print("k="+ i +", ");
	        svdRec.testWithMemHelper(mhTest,20);
	        
	        /*System.out.println("Total SVD pred <0 = " + svdRec.totalNegSVDPred);
	        System.out.println("Total SVD pred >0 = " + svdRec.totalPosSVDPred);*/
        }
        
      //-----------------------------
      // 5-FOLD
      //-----------------------------
        
       
/*    	path ="C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\SVD\\FiveFoldData\\";  
               
    	for (int fold =1; fold<=5;fold++)
    	{
	    	svdFile = path +  "SVDStoredColt" + (fold)+ ".dat";
	    	base 	= path +  "sml_trainSetStoredFold" + (fold)+ ".dat";
	    	test	= path +  "sml_testSetStoredFold" + (fold)+ ".dat";
	    	
	        for(int i=5;i<40;i++)
	        {
		        SVDRecommenderColt svdRec = new SVDRecommenderColt(base, svdFile, i);
		        MemHelper mhTest = new MemHelper(test);
		        System.out.println("FOld: "+ fold + ",k: "+ i +",MAE= " + svdRec.testWithMemHelper(mhTest, 25));
		 
	        }
	        
	        System.out.println("--------------------------------------------------");
    	}*/
    	
    }
    	
     }

    
/**********************************************************************************************************/
    
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
                           
                           // if(rrr>5 || rrr<0)
                           // System.out.println("Prediction ="+ rrr + ", Original="+ myRating);
                        
                            //-------------
                            //Add Error
                            //-------------
                            rmse.add(myRating,rrr);		
                            	
                            //-------------
                            // Add ROC
                            //-------------
                            rmse.ROC4(myRating, rrr, 5, mh.getAverageRatingForUser(uid));		
            
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
        Accuracy = rmse.getAccuracy();
        MCC = rmse.getMetthewsCorrCoff();
        PPV = rmse.getPositivePredictedvalue();
        FDR = rmse.getFalseDiscoveryrRate();
        NPV = rmse.getNegativePredictedValue();
        FPR = rmse.getFalsePositiveRate();
        Specificity = rmse.getSpecificity();
        
        System.out.println("MAE ="+MAE);
/*        System.out.println("Acc ="+Accuracy);
        System.out.println("Roc ="+Roc);
        System.out.println("Specificity ="+Specificity);*/
 /*       System.out.println("MCC ="+MCC);
        System.out.println("PPV ="+PPV);
        System.out.println("FDR ="+FDR);
        System.out.println("NPV ="+NPV);*/
        //System.out.println("---------------------------");
                
        //rmse.resetValues();        
        return dd;
    }

    
} //end of class 		    


