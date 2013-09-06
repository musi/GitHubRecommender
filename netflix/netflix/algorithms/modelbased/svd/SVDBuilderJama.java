package netflix.algorithms.modelbased.svd;

import java.util.*;
import java.io.*;

import cern.colt.matrix.linalg.Algebra;

import netflix.memreader.*;
import netflix.utilities.*;
import Jama.SingularValueDecomposition;
import Jama.Matrix;

public class SVDBuilderJama 
{
	BufferedWriter      writeData;
	String    			myPath="";
    String    			datFile="";
    String    			destFile="";
    MemHelper 			helper;
    boolean 			FOLDING;
    
	public SVDBuilderJama()
	{
            myPath   = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\SVD\\FiveFoldData\\";
         // myPath   = "I:/Backup main data march 2010/workspace/MusiRecommender/DataSets/FT/Itembased/FiveFoldData/";         
          
          
            datFile  = myPath + "sml_clusteringTrainSetStoredTF.dat";       
            //datFile  = myPath + "sml_storedFeaturesRatingsTF.dat";          
            destFile = myPath + "SVDStoredJama2.dat";
            
          
          /*datFile  = myPath + "ft_clusteringTrainSetStoredTF1.dat";
          destFile = myPath + "SVDStoredJamaUserAvgFT.dat";
          */    
          
            FOLDING = false;
    
          
	}
	
/****************************************************************************************************/
	public static void main(String args[]) 
    {
		
		SVDBuilderJama mySVD = new SVDBuilderJama(); //Just created to make calls to other functions
		Timer227 timer = new Timer227();
		
	 	  int numMovies =0;
          int numUsers=0;         
          
           numMovies = 1683;
           numUsers  = 944;
          
          
          
          //open file to write data
          mySVD.openFile();
          
          
  /*        numMovies = 1922;          
          numUsers  = 1214;   */
          
      
  if(mySVD.FOLDING==false)
    { 	     
        try {             
	          
        	mySVD.helper = new MemHelper(mySVD.datFile);
            //double[][] data  = new double[numUsers][numMovies]; //For SVD m>=n
            double[][] data  = new double[numMovies][numUsers]; //For SVD m>=n
            double rating;

            // origional was mov (i), and then users (j) 
            for(int i = 1; i < numMovies; i++)
           //  for(int i = 0; i < numUsers; i++) 
              {
                for(int j =1; j < numUsers; j++) 
                //  for(int j = 0; j < numMovies; j++)
                  {
                    //rating = mySVD.helper.getRating(j+1, i+1);	//uid, mid
                    rating = mySVD.helper.getRating(j, i);	//uid, mid

                    //If the user did not rate the movie  (Here use content based or some other heuristic)
                    if(rating == -99) 
                    {
                        //But may be this value is not there, so data[j][i] = 0 - 3.5 = -3.5 ???                    	
                    	//data[i][j] = helper.getAverageRatingForMovie(i);
                    
                    	//Item Average
                    	/*  data[i][j]  =  mySVD.helper.getAverageRatingForUser(j+1) -
                          			       mySVD.helper.getAverageRatingForUser(j+1);  */
                    		
/*                    	  data[i][j]  =  mySVD.helper.getAverageRatingForMovie(i+1) -
           			     				 mySVD.helper.getAverageRatingForUser(j+1);
*/                    	  

                  	  	 data[i][j]  =  mySVD.helper.getAverageRatingForMovie(i) -
                  	  	 				mySVD.helper.getAverageRatingForUser(j);

                  	  
                    	//constant
                    
                    	/*  data[i][j] = 3 - helper.getAverageRatingForUser(j);            
                    		data[i][j] = 3 - helper.getAverageRatingForUser(j);
                    		data[i][j] = 3 - helper.getAverageRatingForUser(j);
                    	*/         
                    
                    }
                    
                    else 
                    {
                                //data[i][j] = rating - mySVD.helper.getAverageRatingForUser(j+1);  //normalize?
                                
                    			  data[i][j] = rating - mySVD.helper.getAverageRatingForUser(j);  //normalize?
                       
                    	//       data[i][j] = rating;
                  	  //     	data[i][j] = 4;
                            
                                  // System.out.println(j + "," + i + "," + rating);
                                 // mySVD.writeData.write((int) rating);
                                  //mySVD.writeData.write("\t");
                    }

                } //end of user for         
             } //end of movie for
        	               
                                  
            Matrix mySVDMatrix = new Matrix (data);
            //Matrix mySVDMatrixTranspose=(mySVDMatrix.transpose());
            // mySVD.checkTheMethodMake();
            
            
            //SVD            
            timer.start();
            //SingularValueDecomposition svd  =  new SingularValueDecomposition(mySVDMatrixTranspose); 
            SingularValueDecomposition svd  =  new SingularValueDecomposition(mySVDMatrix);
            timer.stop();            
            System.out.println("SVD Calculation took: " + timer.getTime());

            //Write SVD into memory
            FileOutputStream fos  = new FileOutputStream(mySVD.destFile);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(svd);
            os.close();
            timer.resetTimer();
            
        } //end of try
        
             catch(Exception e) {
                 System.out.println("usage: java SVDBuilder numMovies numUsers dataFile destFile");
                 e.printStackTrace();
             }
         
    }//if no folding
  
    //-----------------------------------------------------------------------------
    //5-FOLD
    //-----------------------------------------------------------------------------
  
 else
 {
	 mySVD.myPath= mySVD.myPath + "FiveFoldData\\";
	 	  
     try 
       {     	
            
       	 for (int fold=1;fold <=5;fold++)
          {
       	 	mySVD.datFile = mySVD.myPath  + "sml_trainSetStoredFold" +(fold) + ".dat";
    	 	mySVD.destFile = mySVD.myPath + "svdStoredJama" +(fold) + ".dat";
       	 	mySVD.helper = new MemHelper(mySVD.datFile);
            double[][] data  = new double[numMovies][numUsers]; //For SVD m>=n
            double rating;

            for(int i = 0; i < numMovies; i++) 
            {
                for(int j = 0; j < numUsers; j++) 
                {
                    rating = mySVD.helper.getRating(j, i);	//uid, mid

                  if(rating == -99) 
                    {
                	  data[i][j] =   mySVD.helper.getAverageRatingForMovie(i) - mySVD.helper.getAverageRatingForUser(j) ;
                    
                    }
                    
                    else 
                    {
                       data[i][j] = rating - mySVD.helper.getAverageRatingForUser(j);                    
                    }

                } //end of user for         	                
              } //end of movie for
        	                                                 
            Matrix mySVDMatrix = new Matrix (data);        
                       
            // SVD
            timer.start();
            SingularValueDecomposition svd  =  new SingularValueDecomposition(mySVDMatrix);
            timer.stop();            
            System.out.println("SVD Calculation of Fold " + fold + "took: " + timer.getTime());

            //Write SVD into memory
            FileOutputStream fos  = new FileOutputStream(mySVD.destFile);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(svd);
            os.close();
            timer.resetTimer();
            
       	  }//end of fold for
     }//end of try
   
		        catch(Exception e) {
		            System.out.println("usage: java SVDBuilder numMovies numUsers dataFile destFile");
		            e.printStackTrace();
		        }
  		}//end of else

    } //end of class  
/**************************************************************************************************/

 public void checkTheMethodMake()
  {
	 System.out.println("Just a random check how svd works");
		
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
	  
	  		System.out.println("Data =\n" + data);
	  
	  		Matrix mySVDMatrix = new Matrix (data);
	  		 mySVDMatrix.print(2, 3);
	  		
	   		SingularValueDecomposition svd  =  new SingularValueDecomposition(mySVDMatrix);
	  	    Matrix rootSk = svd.getS().getMatrix(0, k-1, 0, k-1);
	  	    System.out.println("S =\n" );
	  	    rootSk.print(2, 3);
	  	  
	  	    //compute singular value
            for(int i = 0; i < k; i++) 
            {
              rootSk.set(i,i,Math.sqrt(rootSk.get(i,i)));  //Sets the matrix cell at coordinate [row,column] to the specified value. 
            }
            
            System.out.println("Sk =\n" );
            rootSk.print(2,3);

            // Compute U and V'
            Matrix U  = svd.getU();	
            System.out.println("U =\n");
            U.print(2, 3);
            
            Matrix Uk = U.getMatrix(0, U.getRowDimension()-1,0, k-1); // (int row, int column, int height, int width) 
            System.out.println("Uk =\n");
            Uk.print(2, 3);
            
            Matrix VPrime = svd.getV().transpose();
            System.out.println("V'k =\n");
            VPrime.print(2, 3);
            
            Matrix VPrimek = VPrime.getMatrix(0, k-1, 0,VPrime.getColumnDimension()-1);
            System.out.println("V'k =\n" );
             VPrimek.print(2, 3);
            
            Matrix rootSkPrime = rootSk.transpose();
            System.out.println("S' =\n" );
            rootSkPrime.print(2,3);
            
            //compute left and right by multiplying US, and SV'           
           // Matrix left  = alg.mult(Uk, rootSkPrime);
            Matrix left  = Uk.times(rootSk);
            Matrix right = rootSk.times(VPrimek);

            // Multiply [(US)(SV')]
           Matrix P = left.times(right);     
            
	      System.out.println("Uk * Sk =\n");
	      left.print(2,3);
	      System.out.println("Sk * V'k =\n");
	      right.print(2,3);      
	      System.out.println("P =\n");
	      P.print(2,3);
		  
	  
  }
 
 //--------------------------------------------------------------------------------------
 public void openFile()    
 {

	 try {
		   writeData = new BufferedWriter(new FileWriter(myPath + "data.txt", true));   			
	      } 
     
     catch (Exception E)
     {
   	  System.out.println("error opening the file pointer of rec");
   	  System.exit(1);
     }
     
     System.out.println("Rec File Created");
 }
 
 //----------------------------
 

 public void closeFile()    
 {
 
	 try {
		 	writeData.close();
		  }
	     
     catch (Exception E)
     {
   	  System.out.println("error closing the roc file pointer");
     }
     
 }
	
	
}