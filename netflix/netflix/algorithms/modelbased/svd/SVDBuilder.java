package netflix.algorithms.modelbased.svd;

import java.util.*;
import java.io.*;

import netflix.memreader.*;
import netflix.utilities.*;
import cern.colt.matrix.*;
import cern.colt.matrix.linalg.*;
import cern.colt.matrix.impl.*;

/**
 * This class uses the colt library's 
 * SingularValueDecomposition class to create
 * and serialize an SVD object for the movielens
 * data. The program takes 4 arguments:
 *     1) The number of movies.
 *     2) The number of users.
 *     3) The file containing the MemReader.
 *     4) the file to write the SVD to. 
 * The program requires that the number of 
 * movies and number of users be explicitely input
 * to allow for both 0 and 1 indexed datasets. 
 *
 * @author sowellb
 */
class SVDBuilder 
{

	int userInfo[];
	int movieInfo[];
	int totalClusters;
	Random myRand;
	
	public SVDBuilder()
	{
		userInfo 		= new int[6];
		movieInfo 		= new int[6];
		totalClusters 	= 6;
		myRand        	= new Random();
	}
	
/****************************************************************************************************/
	public static void main(String args[]) 
    {
		
		SVDBuilder mySVD = new SVDBuilder(); //Just created to make calls to other functions
		Timer227 timer = new Timer227();
		
        try {
            
        	  int 		numMovies = 0;
              int 		numUsers  = 0;
              String    datFile   = "";
              String    destfile  = "";
              String    myPath    = "";              
     
        	   
           //Simple
            myPath      = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\SVD\\";
            datFile     = myPath + "sml_clusteringTrainSetStoredTF.dat";
            destfile    = myPath + "SVDStored.dat";
                 
            numMovies 	= 1682;
            numUsers 	= 943;         
                        	    
               
            MemHelper helper = new MemHelper(datFile);
            double[][] data  = new double[numMovies][numUsers]; //For SVD m>=n
            double rating;

            // origional was mov (i), and then users (j) 
            for(int i = 0; i < numMovies; i++) 
            {
                for(int j = 0; j < numUsers; j++) 
                {
                    rating = helper.getRating(j, i);	//uid, mid

                    //If the user did not rate the movie  (Here use content based or some other heuristic)
                    if(rating == -99) 
                    {
                        //But may be this value is not there, so data[j][i] = 0 - 3.5 = -3.5 ??? 
                    	
                    	//Zeros
                    	data[i][j] = 0 -
                        helper.getAverageRatingForUser(j);                	
                    	
                    	//Random No. (0.0-1.0)
                    	data[i][j] = mySVD.myRand.nextDouble() * 5.0;
                        helper.getAverageRatingForUser(j);                	
                    	
                        // Movie Average                    	
                    	data[i][j] = helper.getAverageRatingForMovie(i) -
                            helper.getAverageRatingForUser(j);
                    	
                    	// User Average                    	
                    	data[i][j] = helper.getAverageRatingForUser(j)-
                            helper.getAverageRatingForUser(j);
                    	
                    	// (Movie + User /2)                    	
                    	data[i][j] = ((helper.getAverageRatingForMovie(i) + helper.getAverageRatingForUser(j) )/2.0) -
                            helper.getAverageRatingForUser(j);
                    	
                    	// Uniform Distribution                    	
                    	data[i][j] = cern.jet.random.Uniform.staticNextDoubleFromTo(1.0, 5.0) -
                            helper.getAverageRatingForUser(j);

                    	// Normal Distribution (U)                    	
                    	data[i][j] = cern.jet.random.Normal.staticNextDouble( helper.getAverageRatingForUser(j), helper.getStandardDeviationForUser(j))- 
                            helper.getAverageRatingForUser(j);

                    	// Normal Distribution (M)                 	
                    	data[i][j] = cern.jet.random.Normal.staticNextDouble( helper.getAverageRatingForMovie(i), helper.getStandardDeviationForMovie(i))-
                            helper.getAverageRatingForUser(j);            	
                    
                    }
                    
                    else 
                    {
                        data[i][j] = rating - helper.getAverageRatingForUser(j);  //normalize?
                    	 // data[i][j] = rating;
                    }

                }
            } //end of movie for

            //Constructs a matrix with the given cell values
            //Use idioms like DoubleFactory2D.dense.make(4,4) to construct dense matrices
            DenseDoubleMatrix2D matrix = (DenseDoubleMatrix2D) DoubleFactory2D.dense.make(data);
          
           // mySVD.checkTheMethodMake();
            
            //SVD
            timer.start();
            SingularValueDecomposition svd  =  new SingularValueDecomposition(matrix);
            timer.stop();            
            System.out.println("SVD Calculation took: " + timer.getTime());

            //Write SVD into memory
            FileOutputStream fos  = new FileOutputStream(destfile);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(svd);
            os.close();
            
     
   } //end of try
   
        catch(Exception e) {
            System.out.println("usage: java SVDBuilder numMovies numUsers dataFile destFile");
            e.printStackTrace();
        }


    }

/**************************************************************************************************/
  public void checkTheMethodMake()
  {
	  double[][] data  = new double[10][5]; //For SVD m>=n
	  int k =3;
	  int r =0;
	  
	  for(int i=0;i<10;i++)
	  {
		  for (int j=0;j<5;j++) 
		  {	
			  if(j%2 ==0 && i%3 ==0)
				  data [i][j] = 0;
			  else 
				  data [i][j] = r++;
			  if(r==5) r=1;
		  }
	  }
	  
	  DenseDoubleMatrix2D myMatrix = (DenseDoubleMatrix2D) DoubleFactory2D.dense.make(data);	  
	  System.out.println("Matrix =\n" +myMatrix);
	  
	  //--------------------------------------------------------
	     SingularValueDecomposition svd  =  new SingularValueDecomposition(myMatrix);	   
	     Algebra alg = new Algebra();
         DoubleMatrix2D rootSk = svd.getS().viewPart(0, 0, k, k);
         
         System.out.println("S =\n" + rootSk);   
         for(int i = 0; i < k; i++) 
         {
           rootSk.set(i,i,Math.sqrt(rootSk.get(i,i)));  //Sets the matrix cell at coordinate [row,column] to the specified value. 
         }
         System.out.println("Sk =\n" + rootSk);
         
	   
	  DoubleMatrix2D U  = svd.getU();	
      System.out.println("U =\n" + U);
      
	  DoubleMatrix2D Uk = U.viewPart(0, 0, U.rows(), k).copy(); // (int row, int column, int height, int width) 
	  System.out.println("Uk =\n" + Uk);
	  
      DoubleMatrix2D VPrime  = alg.transpose(svd.getV());
      System.out.println("V' =\n" + VPrime);
      
      DoubleMatrix2D VPrimek = VPrime.viewPart(0, 0, k, VPrime.columns()).copy();
      System.out.println("V'k =\n" + VPrimek);
      
      DoubleMatrix2D rootSkPrime  = alg.transpose(rootSk);
      System.out.println("S' =\n" + rootSkPrime);
      
      //compute left and right by multiplying US, and SV'           
     // DoubleMatrix2D left  = alg.mult(Uk, rootSkPrime);
      DoubleMatrix2D left  = alg.mult(Uk, rootSk);
      DoubleMatrix2D right = alg.mult(rootSk, VPrimek);
      System.out.println("Uk * Sk =\n" + left);
      System.out.println("Sk * V'k =\n" + right);
      
      // Multiply [(US)(SV')]
      DoubleMatrix2D P = alg.mult(left, right);      
      System.out.println("P =\n" + P);
	  
	  
  }
	
	
}