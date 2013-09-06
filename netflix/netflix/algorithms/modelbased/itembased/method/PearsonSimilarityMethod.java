package netflix.algorithms.modelbased.itembased.method;

import java.util.ArrayList;
import netflix.algorithms.modelbased.reader.DataReader;
import netflix.utilities.Pair;

//TODO: combine methods better so less copied code - steinbel

//this similarity interface has only names of the methods which will be implemented in this class
/******************************************************************************************************/
public class PearsonSimilarityMethod implements SimilarityMethod 
/******************************************************************************************************/
{
    private int numMinUsers = 5;
    private int numMinMovies = 5;
    
    /* (non-Javadoc)
     * @see netflix.algorithms.modelbased.itembased.method.SimilarityMethod#findSimilarity(netflix.algorithms.modelbased.reader.DataReader, int, int)
     */
  
 /******************************************************************************************************/
 
    public double findSimilarity(DataReader dataReader, int mid1, int mid2, int version)    
    {
        ArrayList<Pair> commonUserRatings = dataReader.getCommonUserRatings(mid1, mid2);
        
        if (commonUserRatings.size() < numMinUsers) return -100.0;
    
        double num = 0.0, den1 = 0.0, den2 = 0.0;
    
        double avg1 = dataReader.getAverageMovieRating(mid1);
        double avg2 = dataReader.getAverageMovieRating(mid2);
        
        for (Pair u : commonUserRatings)         
        {
            double diff1 = u.a - avg1;
            double diff2 = u.b - avg2;
            
            num += diff1 * diff2;
        
            den1 += diff1 * diff1;
            den2 += diff2 * diff2;
        }
        
        double den = Math.sqrt(den1) * Math.sqrt(den2);
        if (den == 0.0) return 0.0;							//no sim?
        													//return 0 
        return num / den;	
    }

/******************************************************************************************************/   
    /* (non-Javadoc)
     * @see netflix.algorithms.modelbased.itembased.method.SimilarityMethod#setNumMinUsers(int)
     */

    public void setNumMinUsers(int numMinUsers)    
    {
        this.numMinUsers = numMinUsers;
    }
    
/******************************************************************************************************/
    /**
     * @author steinbel, based off setNumMinUsers by tuladhaa
     * Accessor method to set minimum number of common movies between two users.
     * @param numMinMovies
     */
    public void setNumMinMovies(int numMinMovies) 
    {
    	this.numMinMovies = numMinMovies;
    }
 
/******************************************************************************************************/
    /**
     * @author steinbel, based off findSimilarity by tuladhaa
     * Finds the Pearson similarity between two users.
     * @param dataReader - reads from the data on this dataset
     * @param uid1 - one of the users to compare
     * @param uid2 - the other user to compare
     * @return - the similarity between user 1 and user 2
     */

    //similarity between two users (uid1,uid2) based on pearson
    
   public double findUserSimilarity(DataReader dataReader, int uid1, int uid2)   
   {
	    //as each implementation of dataReader has the methods (defined in the interface), hence
	   // we do not need to worry about what is the object (i,e.it can be databse or memhelper)
	   
    	ArrayList<Pair> commonMovieRatings = dataReader.getCommonMovieRatings(uid1, uid2);
   
    	if (commonMovieRatings.size() < numMinMovies)
    		return -100.0;
    
    	double num = 0.0, den1 = 0.0, den2 = 0.0, diff1 = 0.0, diff2 = 0.0;
    	
    	double avg1 = dataReader.getAverageRatingForUser(uid1);
    	double avg2 = dataReader.getAverageRatingForUser(uid2);
    	
    	for (Pair m : commonMovieRatings)     	
    	{
    		diff1 = m.a - avg1;
    		diff2 = m.b - avg2;
    	
    		num += diff1*diff2;    		
    		den1 += diff1*diff1;
    		den2 += diff2*diff2;
      	}
    	
    	double den = Math.sqrt(den1)* Math.sqrt(den2);
    	
    	if (den == 0.0)
    		return 0.0;
    
    	return num / den;
    }
/******************************************************************************************************/
   
}
