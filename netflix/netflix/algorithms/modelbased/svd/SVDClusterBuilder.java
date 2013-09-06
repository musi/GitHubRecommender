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
class SVDClusterBuilder 
{

	int userInfo[];
	int movieInfo[];
	int totalClusters;
	DenseDoubleMatrix2D matrix;
	SingularValueDecomposition svd;
	double[][] data;
	MemHelper helper;
	
	public SVDClusterBuilder()
	{
		userInfo = new int[6];
		movieInfo = new int[6];
		totalClusters =6;
	}
	
/****************************************************************************************************/
	public static void main(String args[]) 
    {
		
		SVDClusterBuilder mySVD = new SVDClusterBuilder(); //Just created to make calls to other functions
		Timer227 timer = new Timer227();
		
        try {
            
        	  int 		numMovies =0;
              int 		numUsers=0;
              String    datFile="";
              String    destfile="";
              String     myPath  ="";
              double rating =0;
              
   /*       int numMovies 	= Integer.parseInt(args[0]);
            int numUsers 	= Integer.parseInt(args[1]);
            String datFile 	= args[2];
            String destfile = args[3];
   */
        	
        	   
               //Simple
                myPath   = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\SVD\\";
              	datFile   = myPath + "sml_clusteringTrainSetStoredTF.dat";
              	destfile  = myPath + "SVDStored.dat";
                 
                numMovies 	= 1682;
                numUsers 	= 943;                                                                           
                
                         
                mySVD.helper = new MemHelper(datFile);
                mySVD.data  = new double[numMovies][numUsers];    
         
            
        	  //Clustered SVD
        	  //___________________________________________________________________________
        	   //Read each cluster info from a file
/*        		
        	String   myPath   = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\SVD\\Clusters\\";
        	mySVD.readClusterInfo(myPath+"ClusterInfo.dat");
               
            

     for (int  mm = 0;mm<mySVD.totalClusters;mm++)
      {
    	   System.out.println(" currently processing " + (mm+1)+"....");
            datFile  = myPath + "StoredCluster" + (mm+1) + ".dat";
            destfile = myPath + "SVDStored" + (mm+1) + ".dat";
              
            numMovies = mySVD.movieInfo[mm];
            numUsers = mySVD.userInfo[mm];
            
               
            mySVD.helper = new MemHelper(datFile);
            mySVD.data  = new double[numMovies][numUsers];
            
*/

            for(int i = 0; i < numMovies; i++) 
            {
                for(int j = 0; j < numUsers; j++) 
                {

                    rating = mySVD.helper.getRating(j, i);

                    //If the user did not rate the movie  (Here use content based or some other heuristic)
                    if(rating == -99) 
                    {
                    	mySVD.data[i][j] = mySVD.helper.getAverageRatingForMovie(i) -
                    	mySVD.helper.getAverageRatingForUser(j);
               
                    //    data[i][j] = 3.5 -
                    //    helper.getAverageRatingForUser(j);
               
                    
                    }
                    
                    else 
                    {
                    	mySVD.data[i][j] = rating - mySVD.helper.getAverageRatingForUser(j);  //normalize?
                    }

                }
            } //end of movie for

            //Constructs a matrix with the given cell values
            mySVD.matrix =  (DenseDoubleMatrix2D) DoubleFactory2D.dense.make(mySVD.data);
    
          
            
            //SVD
            System.out.println("starts SVD with " + numMovies + " x " + numUsers);
            timer.start();
	            try
	            {
	            	mySVD.svd =  new SingularValueDecomposition(mySVD.matrix);
	            
	            }
	            
	            catch (Exception e)
	            {
	            	 System.out.println("Exception is SVD calculation");
	            	 e.printStackTrace();
	            }
	            
            timer.stop();            
            System.out.println("SVD Calculation took: " + timer.getTime());

            //Write SVD into memory
            FileOutputStream fos = new FileOutputStream(destfile);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(mySVD.svd);
            os.close();
            
      //}//end of for
   } //end of try
   
        catch(Exception e) {
            System.out.println("usage: java SVDBuilder numMovies numUsers dataFile destFile");
            e.printStackTrace();
        }


    }

/**************************************************************************************************/

	public void readClusterInfo(String fileName)
	{
	
		int pointer=0;
		String line[];
		
		try
		{
			Scanner in = new Scanner (new File(fileName));
		
			while(in.hasNextLine()) //it is parsing line by line            
	        {
	
 	             line = in.nextLine().split(",");		//delimiter
	            
	             userInfo[pointer] = Integer.parseInt(line[1]);
	             movieInfo[pointer] = Integer.parseInt(line[2]);      
            
	             pointer++;
	        }
	 }
	     catch(FileNotFoundException e) {
	            System.out.println("Can't find file " + fileName);
	            e.printStackTrace();

	        }
	        catch(IOException e) {
	            System.out.println("IO error");
	            e.printStackTrace();
	        }

	} //end of function	


}