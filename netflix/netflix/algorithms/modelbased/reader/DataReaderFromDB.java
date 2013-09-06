package netflix.algorithms.modelbased.reader;

import java.util.ArrayList;
import java.util.HashMap;

import cern.colt.list.IntArrayList;
import cern.colt.list.LongArrayList;
import cern.colt.list.ObjectArrayList;

import netflix.algorithms.modelbased.itembased.DatabaseImpl;
import netflix.utilities.Pair;
import netflix.utilities.Triple;


//This class will return results by calling the class databaseImpl (which is extension of dataBase)

/**
 * A DataReader that reads in movies data from a database
 * @author Amrit Tuladhar
 *
 */
/******************************************************************************************************/
public class DataReaderFromDB implements DataReader 
/******************************************************************************************************/
{
    DatabaseImpl databaseImpl;
    
 /******************************************************************************************************/
 
    public DataReaderFromDB(DatabaseImpl databaseImpl)  //so this receive databaseImpl object    
    {
        this.databaseImpl = databaseImpl;
        
        if (!databaseImpl.openConnection()) 
        {
            System.out.println("Could not open database connection.");
            System.exit(1);
        }
    }
    
 /******************************************************************************************************/
    
    public int getNumberOfMovies()     
    {
        Pair movieBounds = databaseImpl.getMaxAndMinMovie();
        return (int) movieBounds.b;
    }
    
    /******************************************************************************************************/
    
    public double getRating(int uid, int mid) 
    
    {
        return databaseImpl.getRatingForUserAndMovie(uid, mid);
    }
    
    /******************************************************************************************************/
    
    public ArrayList<Pair> getCommonUserRatings(int mId1, int mId2) 
    
    {
        return databaseImpl.getCommonUserRatings(mId1, mId2);
    }
    /******************************************************************************************************/
    
    public ArrayList<Triple> getCommonUserRatAndAve(int mId1, int mId2) 
    {
        return databaseImpl.getCommonUserAverages(mId1, mId2);
    }
    /******************************************************************************************************/
    
    public double getAverageMovieRating(int mid) 
    
    {
        return databaseImpl.getAverageMovieRating(mid);
    }
    /******************************************************************************************************/
    public double getRatingFromComposite(int composite) 
    {
        return composite;
    }
    /******************************************************************************************************/
    
    public void close() 
    
    {
        databaseImpl.closeConnection();
    }
    /******************************************************************************************************/
    
    public ArrayList<Pair> getCommonMovieRatings(int uid1, int uid2) 
    
    {
		// TODO Auto-generated method stub
		return null;
	}
    /******************************************************************************************************/
    
    public double getAverageRatingForUser(int uid1) 
    
    {
		// TODO Auto-generated method stub
		return 0;
	}
    /******************************************************************************************************/
    
    public ArrayList<Triple> getCommonMovieRatAndAve(int uid1, int uid2) 
    {
		// TODO Auto-generated method stub
		return null;
	}
    /******************************************************************************************************/
    
    public int getNumberOfUsers() 
    {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public LongArrayList getGenre(int mid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getGenreSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public HashMap<String,Double> getKeywords(int mid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HashMap<String,Double> getTags(int mid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HashMap<String,Double> getFeatures(int mid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IntArrayList getListOfMovies() {
		return null;
		// TODO Auto-generated method stub
		
	}

	@Override
	public IntArrayList getListOfUsers() {
		// TODO Auto-generated method stub
		return null;
	}
}
