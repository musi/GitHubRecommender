package netflix.algorithms.modelbased.svd;

import java.util.*;
import java.io.*;

import netflix.algorithms.memorybased.memreader.FilterAndWeight;
import netflix.memreader.*;
import netflix.recommender.ItemItemRecommenderWithK;
import netflix.recommender.NaiveBayesRec;
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
class SVDBuilderColtWriter 
{

	BufferedWriter      writeData;
	String    			myPath="";
    String    			datFile="";
    String    			testFile="";
    String    			destFile="";
    String  			modelName ="";				//model name to be written
    
    MemHelper 			helper;
    boolean 			FOLDING;
    Random              myRand;
    FilterAndWeight     myFilter; 				//user-based CF
    ItemItemRecommenderWithK myItemRec;		    //item-based CF
    NaiveBayesRec 		myNB;					//Naive Bayes Rec
    
	public SVDBuilderColtWriter()
	{
		myPath   = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\SVD\\";
        
      //  datFile  = myPath + "sml_clusteringTrainSetStoredTF.dat";
      //  datFile  = myPath + "sml_storedFeaturesRatingsTF.dat";
     //   destfile = myPath + "SVDStoredColt.dat";
     //   destFile = myPath + "SVDStoredColtI.dat";   
        
        FOLDING = false;
        myRand  = new Random();      
  
	}
	
/****************************************************************************************************/
	public static void main(String args[]) 
    {
		
		SVDBuilderColtWriter mySVD = new SVDBuilderColtWriter(); //Just created to make calls to other functions
		
		for(int i=0;i<=0;i++)
		    mySVD.buildModels(i);
    }
	
	
/****************************************************************************************************/
/**
 *  Build models, may be 1-fold or 5-fold
 */
	
	public void buildModels(int whichModel)
	{
		Timer227 timer = new Timer227();
		BufferedWriter outT;		
		  
		int 	 numMovies =0;
        int 	 numUsers=0;
        String   myPath	="";        
        String   myOutFileWriter = "";
        String   myOutT = "";
        
        numMovies 	= 1682;
        numUsers 	= 943;
        
        try { 
        	   
           //Simple
            myPath      = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\SVD\\";
            datFile     = myPath + "sml_clusteringTrainSetStoredTF.dat";
            testFile    = myPath + "sml_clusteringTestSetStoredTF.dat";
            myOutT      = myPath + "sml_FilledTF.dat";
            myOutFileWriter  = myPath + "sml_FilledTrainSetStoredTF.dat";		//output file
                        
            MemHelper helper = new MemHelper(datFile);			 //MemReader File	
            myFilter = new FilterAndWeight(helper,1); 			 //FilterAndWeight, Random object, For CF
          
            //Item based CF 	   
            myItemRec = new ItemItemRecommenderWithK("movielens", "sml_ratings", "sml_movies", 
         		   						      		 "sml_averages", 
         		   						      		 "sml_SimFold", true);
            //buffer to write o/p data
        	outT = new BufferedWriter(new FileWriter(myOutT));
            
            double[][] data  = new double[numMovies][numUsers]; //For SVD m>=n
            double rating;

            // origional was mov (i), and then users (j) 
            for(int i = 1; i <= numMovies; i++) 
            {
                for(int j = 1; j <= numUsers; j++) 
                {
                    rating = helper.getRating(j, i);	//uid, mid

                    //Wanna normalized or not?
                    double nor = helper.getAverageRatingForUser(j);
                    nor = 0;
                    
                    //If the user did not rate the movie  (Here use content based or some other heuristic)
                    if(rating == -99) 
                    {                    	
                    	// User-Based CF
                    	if(whichModel==0) {
	                    	double temp = myFilter.recommendS(j, i, 70, 1);
	                    	if(temp<0 || temp>5)
	                    		temp = helper.getAverageRatingForUser(j);
	                    	String oneSample = (j+  "," + i + "," + temp ); //uid, mid, rating	                        
		  				    outT.write(oneSample);		
		  				    outT.newLine();
	                    	modelName  = "SVDUserBasedCF";
                    	}
                    	
                    	// Item-Based CF
                    	else if(whichModel==1) {
	                    	double temp = myItemRec.recommend(j, i, "", 15);
	                      	String oneSample = (j+  "," + i + "," + temp ); //uid, mid, rating	                        
		  				    outT.write(oneSample);		
		  				    outT.newLine();
	                    	modelName  = "SVDItemBasedCF";
                    	}

                    	//(User+Item CF)/2
                    	else if(whichModel==2) {
	                    	double tempU = myFilter.recommendS(j, i, 70, 1);
	                    	double tempI = myItemRec.recommend(j, i, "", 15);
	                        double temp = (tempU + tempI)/2 - nor;
	                     	String oneSample = (j+  "," + i + "," + temp ); //uid, mid, rating	                        
		  				    outT.write(oneSample);		
		  				    outT.newLine();
	                    	modelName  = "SVDUserAndItemBasedCF";
                    	}
                    }
                    
                    // We have ratings, insert them into matrix
                    else 
                    {
                         String oneSample = (j+  "," + i + "," + rating ); //uid, mid, rating	                        
		  				 outT.write(oneSample);		
		  				 outT.newLine();
                    	// data[i][j] = rating;
                    }

                } //end of users
            } //end of movie for
            
            outT.close();
   } //end of try
   
        catch(Exception e) {
            System.out.println("usage: java SVDBuilder numMovies numUsers dataFile destFile");
            e.printStackTrace();
        }
        
        //---------------------------------------------------
        //  Write into Memory
        //---------------------------------------------------
        
        System.out.println("Going to write");
        MemReader myReader = new MemReader();      
	 	myReader.writeIntoDisk(myOutT, myOutFileWriter); 		
	 	
	}

} //end of class