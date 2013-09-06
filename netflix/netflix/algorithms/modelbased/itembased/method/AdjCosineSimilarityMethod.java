package netflix.algorithms.modelbased.itembased.method;

import java.util.ArrayList;

import netflix.algorithms.modelbased.reader.DataReader;
import netflix.utilities.Triple;
/******************************************************************************************************/
public class AdjCosineSimilarityMethod implements SimilarityMethod 
/******************************************************************************************************/
{
    private int numMinUsers = 1;
    private int numMinMovies = 2;
    
    /* (non-Javadoc)
     * @see netflix.algorithms.modelbased.itembased.method.SimilarityMethod#setNumMinUsers(int)
     */

/******************************************************************************************************/
    
    public void setNumMinUsers(int numMinUsers)  
    {
        this.numMinUsers = numMinUsers;
    }
    
    /* (non-Javadoc)
     * @see netflix.algorithms.modelbased.itembased.method.SimilarityMethod#findSimilarity(netflix.algorithms.modelbased.reader.DataReader, int, int)
     */
/******************************************************************************************************/
    //cosine between two items (mid1, mid2)
    
    public double findSimilarity(DataReader dataReader, int mid1, int mid2, int version)    
    {

    	
        ArrayList<Triple> commonUsers = dataReader.getCommonUserRatAndAve(mid1, mid2);
    
        if (commonUsers.size() < numMinUsers) return  -100.0;	//just like threshold
        double num = 0.0, den1 = 0.0, den2 = 0.0;
        
        for (Triple u : commonUsers)        
        {
            double diff1 = u.r1 - u.a;
            double diff2 = u.r2 - u.a;
    
          /*  if (!(u.r1 <=5 && u.r1>0)) System.out.println("r1 =" + (u.r1));
            if (!(u.r2 <=5 && u.r2>0)) System.out.println("r2 =" + (u.r2));*/
            
            num += diff1 * diff2;
            
            den1 += diff1 * diff1;
            den2 += diff2 * diff2;
        }
        
        double den = Math.sqrt(den1) * Math.sqrt(den2);
        
                
        if (den == 0.0) return 0.0;
        
      //  if(num/den <0) System.out.println("Less than zero sim =" + (num/den));
      //  if(num/den >1.1) System.out.println("Greater than one sim =" + (num/den));
        
        return num / den;
    }
    
/******************************************************************************************************/
    //cosine between two items (mid1, mid2)
    
/*    public double findItemSimilarity_SVD(DataReader dataReader, int mid1, int mid2, int version)    
    {
    	//get all the users who have seen these two movies
        ArrayList<Triple> commonUsers = dataReader.getCommonUserRatAndAve(mid1, mid2);
    
        if (commonUsers.size() < numMinUsers) return  -100.0;	//just like threshold
        double num = 0.0, den1 = 0.0, den2 = 0.0;
        
        for (Triple u : commonUsers)        
        {
            double diff1 = u.r1 - u.a;
            double diff2 = u.r2 - u.a;
    
            if (!(u.r1 <=5 && u.r1>0)) System.out.println("r1 =" + (u.r1));
            if (!(u.r2 <=5 && u.r2>0)) System.out.println("r2 =" + (u.r2));
            
            num += diff1 * diff2;
            
            den1 += diff1 * diff1;
            den2 += diff2 * diff2;
        }
        
        double den = Math.sqrt(den1) * Math.sqrt(den2);
        
                
        if (den == 0.0) return 0.0;
        
      //  if(num/den <0) System.out.println("Less than zero sim =" + (num/den));
      //  if(num/den >1.1) System.out.println("Greater than one sim =" + (num/den));
        
        return num / den;
    }
    
*/
    

/******************************************************************************************************/
    
    /**
     * @author steinbel, based off findSimilarity by tuladhaa
     * Uses adjusted cosine similarity to find the similarity between two users.
     * @param dataReader - reads from the data on this dataset
     * @param uid1 - one of the users to compare
     * @param uid2 - the other user to compare
     * @return - the similarity between user 1 and user 2
     */

    //calculating simple cosine formula between the common movies seen by uid1, uid2 
    
    public double findUserSimilarity(DataReader dataReader, int uid1, int uid2)    
    {
		ArrayList<Triple> commonMovies = dataReader.getCommonMovieRatAndAve(uid1, uid2);
	
		if (commonMovies.size() < numMinMovies)
			return -100.0;
	
		double num = 0.0, den1 = 0.0, den2 = 0.0, diff1 = 0.0, diff2 = 0.0;
		
		for (Triple m : commonMovies) 		
		{
			diff1 = m.r1 - m.a;
			diff2 = m.r2 - m.a;
		
			num += diff1 * diff2;
			den1 = diff1 * diff1;
			den2 = diff2 * diff2;
		}
		
		double den = Math.sqrt(den1) * Math.sqrt(den2);
		
		if (den == 0.0)
			return 0.0;
	
		return num / den;
	}
/******************************************************************************************************/

    /**
	 * @author steinbel, based off setNumMinUsers by tuladhaa
	 * Accessor method to set the minimum number of movies needed in common to determine
	 * similarity between two users.
	 * @param numMinMovies - the minimum number of movies
	 */

    public void setNumMinMovies(int numMinMovies)    
    {
		this.numMinMovies = numMinMovies;		
	}


}
