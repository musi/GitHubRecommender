package netflix.recommender;

import java.util.*;
import netflix.algorithms.memorybased.rectree.*;
import netflix.memreader.*;
import netflix.utilities.*;
import cern.colt.list.*;
import cern.colt.map.*;

/************************************************************************************************/
public class RecTreeRecommender extends AbstractRecommender 
/************************************************************************************************/

{

    private RecTree2 	tree;
	private MyRecTree 	mixedTree;
	private SimpleKMean	simpleKTree;
    MemHelper 			helper;
    Timer227 			timer;
    private int 		totalNonRecSamples;	 //Total number of sample for which we did not recommend anything
    private int 		totalRecSamples;
    private int 		howMuchClusterSize;
    private double 		threshold = 0.1;
    
/************************************************************************************************/
    
    public RecTreeRecommender(String memReaderFile)    
    {
       //________________________
    	 totalNonRecSamples =0;
    	 totalRecSamples 	=0;
    	 howMuchClusterSize =0;
    		   	
        timer  = new Timer227();
        helper = new MemHelper(memReaderFile);		//with train set        
       //_____________________________
        timer.start();
        simpleKTree = new SimpleKMean(helper);
        simpleKTree.cluster(4, 10);       
        timer.stop();
        System.out.println("Tree took " + timer.getTime() + " s to build");        
        //_____________________________
       /*
       timer.start();
       tree = new RecTree2(helper);
       tree.cluster();       
       timer.stop();
       System.out.println("Tree took " + timer.getTime() + " s to build");
        */
        //_____________________________
        timer.start(); //Ramanan concept                
        mixedTree = new MyRecTree(helper);
        mixedTree.cluster(threshold);
        timer.stop();
        System.out.println("Tree took " + timer.getTime() + " s to build");
        //_____________________________
    }

/************************************************************************************************/

    public void callMixedTree(double th)    
    {
        mixedTree.cluster(th);
    	
    }
    
    
    
/************************************************************************************************/
    
    /**
     * Correlation weighting between two users, as provided in "Empirical
     * Analysis of Predictive Algorithms for Collaborative Filtering."
     * 
     * @param mh the database to use
     * @param activeUser the active user
     * @param targetUser the target user
     * @return their correlation
     */
    
    private double correlation(int activeUser, int targetUser)     
    {
        double topSum, bottomSumActive, bottomSumTarget, rating1, rating2;
        topSum = bottomSumActive = bottomSumTarget = 0;
        
        double activeAvg = helper.getAverageRatingForUser(activeUser);
        double targetAvg = helper.getAverageRatingForUser(targetUser);
    
        ArrayList<Pair> ratings = helper.innerJoinOnMoviesOrRating(activeUser, targetUser, true);
		
        // Do the summations
        for(Pair pair : ratings)         
        {
            rating1 = (double)MemHelper.parseRating(pair.a) - activeAvg;
            rating2 = (double)MemHelper.parseRating(pair.b) - targetAvg;
			
            topSum += rating1 * rating2;
            bottomSumActive += Math.pow(rating1, 2);
            bottomSumTarget += Math.pow(rating2, 2);
        }
		
        // This handles an emergency case of dividing by zero
        if(bottomSumActive != 0 && bottomSumTarget != 0)
            return topSum / Math.sqrt(bottomSumActive * bottomSumTarget);
        else
            return 0;
    }

 /************************************************************************************************/
    
    /**
     * Basic recommendation method for memory-based algorithms.
     * 
     * @param user
     * @param movie
     * @return the predicted rating, or -99 if it fails (mh error)
     */
 
    //We call it for active user and a target movie
    public double recommend(int activeUser, int targetMovie, String date)    
    {
        double currWeight, weightSum = 0, voteSum = 0;
        int uid;
		double rating;

        // Get the cluster in which this user is in
        // ...So this means active user is already therein the cluster for which we want to predict some movies?
        // So how we will construct the test set?,
        // It means, that the tree or cluster should be created only on training set which has say 80% ratings of each user
        // and when from test set we want to predict some movie for active user, then 
        
        
        //furthermore, This assume that active user is already there in the cluster, what about if a user is new
        //Don't have any rating? ... Or we can assume that he has rated let 2-3 movies, so in which cluster he
        //fits the best and then recommend him the movies. 
        
        // One scenario is we can divide the dataset based on the users only, Users in the test set are not
        // There in the traing set... but here we have question that if test users are not highly correlated with 
        // any of the training set cluster then performance will not b good. 
        
        //One more scenario is take active user, see his similarity with all the clusters and just take the weighted 
        //average of the clusters rather than users
        
        //
        
        IntArrayList mixedUsers 	= mixedTree.getClusterByUID(activeUser); 	//mixed tree and K users
        IntArrayList simpleKUsers 	= simpleKTree.getClusterByUID(activeUser);  //simpleK tree users
    //    IntArrayList treeUsers 	= tree.getClusterByUID(activeUser);		 	//simple tree users
       // int userClusterIndex    = tree.getClusterIDByUID(activeUser);
        LongArrayList userWhichSawThisMovie = helper.getUsersWhoSawMovie(targetMovie);
        
        double recommendation   = 0.0; 

        // Basically, what we are doing here is to pre-defined the users into clusters and 
        // then only use these users for making correlation with the ctive users
        
                
  // if (mixedUsers.size() >=howMuchClusterSize)
 //  if (simpleKUsers.size() >=0)
     if (3>1)
      { 
    	 totalRecSamples++;
    	     	 
        for(int i = 0; i < simpleKUsers.size(); i++) //go through all the users in the cluster - (created by Kmean)        
        {
            uid = simpleKUsers.getQuick(i);
        
            if (userWhichSawThisMovie.contains(uid)) 		//so this user has seen movie
            {
              rating = helper.getRating(uid, targetMovie);	//get rating of ratings of each user for the target movie
            
              if (rating==-99 )
              {
                 System.out.println(" rating error");                                 
              }
              
            //If the user rated the target movie and the target
            //user is not the same as the active user. 
        
            if(rating != -99 && uid != activeUser)            
            {              
                currWeight = correlation(activeUser, uid);
                weightSum += Math.abs(currWeight);
                voteSum += currWeight * (rating - helper.getAverageRatingForUser(uid));			
            }
          } //end of pre-check
        }

        // Normalize the sum, such that the unity of the weights is one
        voteSum *= 1.0 / weightSum;

        // Add to the average vote for user (rounded) and return
        recommendation =  Math.round((helper.getAverageRatingForUser(activeUser) + voteSum)*10) / 10.0;

      } //end of if that cluster have more than 10 users
     
     //if cluster size is very less, these are the users who have unusual taste
     //We can use some content based filtering etc here
     
     
    /* 
     else 
     {
    	 //simple case recommendation is average of the two global averages
    	 	recommendation += helper.getAverageRatingForUser(activeUser);
    	 	recommendation += helper.getAverageRatingForMovie(targetMovie);    	 
    	 	recommendation/=2;     	 
    	 	recommendation =0;
    	
    	 
    	 recommendation = -1;
    	 
    	 totalNonRecSamples++;
     }
     */
     
   //     System.out.println("uid: " + activeUser + " mid: " + targetMovie + " rating: " + recommendation);

        return recommendation;
    }

/************************************************************************************************/
    
    public static void main(String[] args) 
    
    {
    	double finalError=0.0;
        //String base = "uabase.dat", test = "uatest.dat";					//??? what are these sets?
		
   //     String test  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\TestTrain\\sml_20testSetStored.dat";
//	    String base  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\TestTrain\\sml_80trainSetStored.dat";
	    
    	
	    String test  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\sml_clusteringTestSetStored.dat";
	    String base = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\sml_clusteringTrainSetStored.dat";
	   
        
	 //   String test  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\TestTrain\\ml_20testSetStored.dat";
	 //   String base = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\TestTrain\\ml_80trainSetStored.dat";
	    
	  //  System.out.println("Training set: " + base + ", test set: " + test);
        
        
	    //Build the tree based on training set only
        RecTreeRecommender rec = new RecTreeRecommender(base);
        
        //build the MemHelper based on the test set
        MemHelper mh = new MemHelper(test);
     //   System.out.println("---->RMSE: " + finalError);
        //_____________________________
        // Check for thrshold in Kmean
        //_____________________________
     
   
         
        
   for(double i=0.1;i<5; i+=0.1)
    {
	   if (i!=0.1) rec.callMixedTree(i);
	   
	   System.out.println("current threshol is" + i);       
	   
        for (int t=1;t < 20;t++)
        {
        	rec.howMuchClusterSize =t;
        	rec.totalRecSamples =0;
        	rec.totalNonRecSamples =0;
       
        	finalError = rec.testWithMemHelper(mh, 25);
     		System.out.println("---->RMSE: " + finalError);       	
    		System.out.print(", total rec samples: " + rec.totalRecSamples);
    		System.out.print(", total non rec samples: " + rec.totalNonRecSamples);
    		System.out.println(", cluster size: " + t);
    	
        	
   /*     	
          if(finalError<.96)        	
          {
        		System.out.println("---->RMSE: " + finalError);       	
        		System.out.print(", total rec samples: " + rec.totalRecSamples);
        		System.out.print(", total non rec samples: " + rec.totalNonRecSamples);
        		System.out.println(", cluster size: " + t);
        	}
        
            
        	if(Double.isNaN(finalError)) break;
     */
        	
        }
     }



    }

@Override
public double recommend(int uid, int mid, int neighbours) {
	// TODO Auto-generated method stub
	return 0;
}


        
}