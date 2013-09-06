package netflix.algorithms.modelbased.reader;

import java.util.ArrayList;
import java.util.HashMap;

import cern.colt.list.IntArrayList;
import cern.colt.list.LongArrayList;
import cern.colt.list.ObjectArrayList;
import netflix.memreader.MemHelper;
import netflix.utilities.Pair;
import netflix.utilities.Triple;

//This class will return results by calling the class MemHelper

/**
 *  A DataReader that reads in movies data from a serialized object
 * @author Amrit Tuladhar
 *
 */
/******************************************************************************************************/
public class DataReaderFromMem implements DataReader 
/******************************************************************************************************/
{
    MemHelper memHelper;
    
/******************************************************************************************************/
    
    
    public DataReaderFromMem(MemHelper memHelper) //receive memHelper object    
    {
        this.memHelper = memHelper;
    }

    /******************************************************************************************************/
    
    public int getNumberOfUsers()     
    {
    	return memHelper.getNumberOfUsers();
    }
    /******************************************************************************************************/
    
    public int getNumberOfMovies() 
    {
        return memHelper.getNumberOfMovies();
    }
    
    /******************************************************************************************************/
    
    public double getRating(int uid, int mid) 
    {
        return memHelper.getRating(uid, mid);
    }
    /******************************************************************************************************/
    
    public ArrayList<Pair> getCommonUserRatings(int mId1, int mId2)    
    {
        ArrayList<Pair> blockUserRatings = memHelper.innerJoinOnMoviesOrRating(mId1, mId2, false);
        ArrayList<Pair> commonUserRatings = new ArrayList<Pair>();
    
        for (Pair blockUserRating : blockUserRatings)         
        {
            commonUserRatings.add(new Pair(
                    MemHelper.parseRating(blockUserRating.a),
                    MemHelper.parseRating(blockUserRating.b)));
        }
        
        return commonUserRatings;
    }
    
    /******************************************************************************************************/
    //It is returning common users ...means users who have seen two movies 
    //And return for each such users (its rating for mid1, its rating for mid2, its average)
    
    
    //Each justCommonUserAndAve contains a user (userID+ratingforMovie1, userID+ratingforMovie2)
    public ArrayList<Triple> getCommonUserRatAndAve(int mId1, int mId2)    
    {
        ArrayList<Pair> justCommonRatings = memHelper.innerJoinOnMoviesOrRating(mId1, mId2, false);
        ArrayList<Triple> commonUserAverages = new ArrayList<Triple>();
    
        for(Pair justCommonRating : justCommonRatings)         
        {
            commonUserAverages.add(new Triple(
                    MemHelper.parseRating(justCommonRating.a), 
                    MemHelper.parseRating(justCommonRating.b),
                    memHelper.getAverageRatingForUser(MemHelper.parseUserOrMovie(justCommonRating.a))));
        }
        
        return commonUserAverages;
    }
    
    /******************************************************************************************************/
    
    public double getRatingFromComposite(int composite) 
    {
        return MemHelper.parseRating(composite);
    }
    
    /******************************************************************************************************/
    
    public double getAverageMovieRating(int mid) 
    {
        return memHelper.getAverageRatingForMovie(mid);
    }
    
    /******************************************************************************************************/
    
    /* (non-Javadoc)
     * @see netflix.algorithms.modelbased.reader.DataReader#close()
     * Required for implementing DataReader
     */
    public void close() 
    
    {
    }

    /******************************************************************************************************/
    
    /**
     * @author steinbel, based off getCommonUserRatings() by tuladara
     * Finds ratings of movies seen by both of two users.
     * @param uid1 - id of one of the users
     * @param uid2 - the other user's id
     * @return - list with the ratings in the form <rating of user 1, rating of u2>
     */
	
    public ArrayList<Pair> getCommonMovieRatings(int uid1, int uid2) 
    
    {
		ArrayList<Pair> blockMovieRatings = memHelper.innerJoinOnMoviesOrRating(uid1, uid2, true);
		ArrayList<Pair> commonMovieRatings = new ArrayList<Pair>();
	
		for (Pair blockMovieRating: blockMovieRatings)		
		{
			commonMovieRatings.add(new Pair(
					MemHelper.parseRating(blockMovieRating.a),
					MemHelper.parseRating(blockMovieRating.b)));
		}
		return commonMovieRatings;
	}
    /******************************************************************************************************/
    
	/**
	 * @author steinbel, based off getAverageMovieRating by tuladara
	 * Gives us this user's average rating.
	 * @param uid - the id of the user
	 * @return - the user's average rating
	 */
	public double getAverageRatingForUser(int uid) 	
	{
		return memHelper.getAverageRatingForUser(uid);
	}

	/******************************************************************************************************/
	
	/**
	 * @author steinbel, based off getCommonUserRatAndAve by tuladara
	 * Gets a list of ratings and averages for movies seen in common by two users.
	 * @param uid1 - id of one of the users
	 * @param uid2 - id of the other user
	 * @return - list where each entry is
	 *  <user 1's rating on movie x, user2's rating on movie x, average rating for movie x>
	 */
	public ArrayList<Triple> getCommonMovieRatAndAve(int uid1, int uid2)	
	{
		ArrayList<Pair> justCommonRatings = memHelper.innerJoinOnMoviesOrRating(uid1, uid2, true);
		ArrayList<Triple> commonMovieAverages = new ArrayList<Triple>();
	
		for (Pair justCommon : justCommonRatings) 		
		{
			commonMovieAverages.add(new Triple(
					MemHelper.parseRating(justCommon.a),
					MemHelper.parseRating(justCommon.b),
					memHelper.getAverageRatingForMovie(MemHelper.parseUserOrMovie(justCommon.a))));
		}
		
		return commonMovieAverages;
	}
    
/******************************************************************************************************/
/******************************************************************************************************/
	
	/**
	 * @author Musi, Return genre against a movie
	 * @param  mid
	 * @return IntArrayList of the genres 
	 */

	@Override
	public LongArrayList getGenre(int mid) 
	{
		// TODO Auto-generated method stub
		 return memHelper.getGenreAgainstAMovie(mid);
	}

/******************************************************************************************************/
	
	@Override
	public int getGenreSize()
	{
		
		return memHelper.getGenreSize();
		
	}


/******************************************************************************************************/

	/**
	 * @author Musi, Return gkeywords against a movie
	 * @param  mid
	 * @return ObjectArrayList of the genres 
	 */

	
	@Override
	public HashMap<String,Double> getKeywords(int mid) 
	{
		
		return memHelper.getKeywordsAgainstAMovie(mid);
		
		
	}

/******************************************************************************************************/

	/**
	 * @author Musi, Return Tags against a movie
	 * @param  mid
	 * @return ObjectArrayList of the tags 
	 */
	
	
	public HashMap<String,Double> getTags(int mid) 
	{
	
		return memHelper.getTagsAgainstAMovie(mid);
	}

/******************************************************************************************************/

	/**
	 * @author Musi, Return Features against a movie
	 * @param  mid
	 * @return ObjectArrayList of the feature
	 */
	
	
	public HashMap<String,Double> getFeatures(int mid) {
		
		return memHelper.getFeaturesAgainstAMovie(mid);
	}

/******************************************************************************************************/

	/**
	 * @author Musi, list of movies in the object
	 * @return IntArrayList of movies
	 */

	public IntArrayList getListOfMovies() 
	{
	
		return memHelper.getListOfMovies();
	 
	}

/******************************************************************************************************/

	/**
	 * @author Musi, list of users in the object
	 * @return IntArrayList of users
	 */

	public IntArrayList getListOfUsers() 
	{
		
		return memHelper.getListOfUsers();
	}
	
/******************************************************************************************************/
	
	
}

