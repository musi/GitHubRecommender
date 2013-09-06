package netflix.algorithms.modelbased.itembased;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import netflix.db.Database;
import netflix.utilities.IntDoublePair;
import netflix.utilities.Pair;
import netflix.utilities.Triple;

//have methods for ratings, similarity Tables

/******************************************************************************************************/
public class DatabaseImpl extends Database
/******************************************************************************************************/
{

    protected String  usersName;
    private String 	  similarityTableName;

    public DatabaseImpl()    
    {
        super();
    }

 /******************************************************************************************************/
    
    public DatabaseImpl(String dbName, String ratingsName, String moviesName, String usersName)    
    {
        super(dbName, ratingsName, moviesName);
        this.usersName = usersName;
    }
    
/******************************************************************************************************/
    
    public DatabaseImpl(String dbName, String ratingsName, String moviesName, String usersName,
    					String similarityName)    
    {
    	super(dbName, ratingsName, moviesName);	//set in the database of netflix 
    	
    	this.usersName 				= usersName;   //it contains averages 
    	this.similarityTableName 	= similarityName;
    }
   
/******************************************************************************************************/
    
    /**
     * @author steinbel
     * Lets us set the name of the similarity table we're working with.
     * @param simTable - the name of the similarity table
     */
    
    public void setSimTableName(String simTable)    
    {
    	this.similarityTableName = simTable;
    }
 
/******************************************************************************************************/
    
    /**
     * @author steinbel
     * Gets the similarities and ratings of the (pre-calculated) similar movies
     * for a movie............ (Similar items)
     * 
     * @param movieID - the id of the movie we're predicting
     * @param trimList - true if only truly similar movieIDs are wanted (ignore
     *                  placeholders with similarity of -100) false for all results
     * @return ArrayList<IntDoubPair> the movieIDs and similarities
     */

    public ArrayList<IntDoublePair> getSimilarMovies(int movieID, boolean trimList)    
    {
        ArrayList<IntDoublePair> list = new ArrayList<IntDoublePair>();
    
        try         
        {
            Statement stmt = con.createStatement();
            String query = "SELECT MovieId2, similarity FROM " + similarityTableName +
                            " WHERE MovieId1 = " + movieID;	//we have already stored all similar movies againsta movi
            
            if (trimList)
                query += " AND similarity != -100";
            query += ";";

            ResultSet rs = stmt.executeQuery(query);
      
            while (rs.next())
                list.add(new IntDoublePair(rs.getInt(1), rs.getDouble(2))); //similar movie id, similarity
            stmt.close();
        }
      
        catch (SQLException sE) {
            sE.printStackTrace();
        }
        
        return list;
    }

/******************************************************************************************************/
    
    public double getSimilarity(int movieID1, int movieID2)    
    {
    	return getSimilarity(this.similarityTableName, movieID1, movieID2);
    }
    
    /******************************************************************************************************/

    public double getSimilarity(String similarityTableName, int movieId1, int movieId2)    
    {
        double sim = -1000.0;
    
        try         
        {
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT similarity FROM " + similarityTableName + 
                    " WHERE MovieId1 = " + movieId1 + " AND MovieId2 = " + movieId2 + ";");
        
            if(rs.next())
              sim = rs.getDouble(1);
            
            stmt.close();
        }
        catch (SQLException sE) {
            sE.printStackTrace();
        }
        return sim;
    }
    
/******************************************************************************************************/
    
    /**
     * @author steinbel
     * Gets the rating by the user for a movie.
     * @param userID - the id of the user in question
     * @param movieID - the id of the movie in question
     * @return int the rating of user with userID for movie with movieID
     *		-99 indicates no rating
     */

    public int getRatingForUserAndMovie(int userID, int movieID)    
    {
        int rating = -99;

        try        
        {
            Statement stmt = con.createStatement();
        
            ResultSet rs = stmt.executeQuery("SELECT Rating FROM " + ratingsName + " "
                    + "WHERE UserId = " + userID + " AND MovieID = " + movieID 
                    + ";");
    
            if (rs.next())
            	rating = rs.getInt(1);
            stmt.close();
        }
        
        catch(SQLException e){ e.printStackTrace(); }
        return rating;
    }

/******************************************************************************************************/
    
    /**
     * @auther steinbel
     * Gets all the ratings on a movie, matched with the userIDs of the users who rated.
     * @param movieID - the id of the movie
     * @return ArrayList<Pair> a list of <userid, rating>
     */

    //get all the userIds and with ratings who rated a movie
    public ArrayList<Pair> getRatingVector(int movieID)    
    {
    	
    	ArrayList<Pair> vector = new ArrayList<Pair>();
	
		try	
		{
			Statement stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT UserId, Rating FROM " + ratingsName
				+ " WHERE MovieId = " + movieID + ";");
		
			while(rs.next())
			{
				vector.add(new Pair(rs.getInt(1), rs.getInt(2)));
			}
			
			stmt.close();
		} catch(SQLException e){e.printStackTrace();}
	
		return vector;
    }
   
/******************************************************************************************************/
    
   /**
     * @author lewda, with code purloined from steinbel
     * 
     * Finds the average rating for a particular movie
     * 
     * @param movieID
     * @return
     */

    public double getAverageRatingForMovie(int movieID)    
    {
        double avgRating = 0;
    
        try{
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT AVG(Rating) FROM " + ratingsName + " "
                    + "WHERE MovieId = " + movieID + ";");
            rs.next();
            avgRating = rs.getDouble(1);
            stmt.close();
        } catch(SQLException e){ e.printStackTrace(); }
        return avgRating;
    }

/******************************************************************************************************/
    /**
     * @author lewda, with code massaged from steinbel's
     * 
     * Finds the average rating for a particular user
     * 
     * @param userID
     * @return the average rating of a user
     */
    public double getAverageRatingForUser(int userID)
    {
        double avgRating = 0;
        try        
        {
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT avgRating FROM " + usersName + " "
                    + "WHERE UserId = " + userID + ";");
            rs.next();
            avgRating = rs.getDouble(1);
            stmt.close();
        } catch(SQLException e){ e.printStackTrace(); }
        return avgRating;
    }

/******************************************************************************************************/   
    /**
     * @author lewda, with code lovingly stolen from steinbel
     * Selects all uids of people who have seen the target film
     * 
     * @param movieID - the movie that the user saw
     * @return a list of uids
     */

    public ArrayList<Integer> getUsersWhoSawMovie(int movieID)    
    {
        ArrayList<Integer> users = new ArrayList<Integer>();		
        
        try
        {
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT UserId FROM " + ratingsName + " " 
                    + "WHERE MovieId = " + movieID + ";");
    
            while (rs.next())
                users.add(rs.getInt(1));
            stmt.close();
        } catch(SQLException e){ e.printStackTrace(); }
        return users;
    }

/******************************************************************************************************/
    
    /**
     * @author lewda, with code lovingly stolen from steinbel
     * Selects all mids of movies that have been watched by target user
     * 
     * @param uid - the user id
     * @return a list of mids
     */

    public ArrayList<Integer> getRatingsForMoviesSeenByUser(int uid)    
    {
        ArrayList<Integer> users = new ArrayList<Integer>();		
    
        try        
        {
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT Rating FROM " + ratingsName + " "
                    + "WHERE UserId = " + uid + ";");
        
            while (rs.next())
                users.add(rs.getInt(1));
            
            stmt.close();
        } catch(SQLException e){ e.printStackTrace(); }
        return users;
    }

/******************************************************************************************************/
    
    /**
     * @author lewda, with code that is allegedly originating from steinbel
     * 
     * Finds the ratings common between two users.
     * @param userID1
     * @param userID2
     * @return
     */

    public ArrayList<Pair> getCommonRatings(int userID1, int userID2)    
    {
        ArrayList<Pair> list = new ArrayList<Pair>();
    
        try        
        {
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT R1.Rating, R2.Rating " 
                    + "FROM " + ratingsName + " R1 "
                    + "INNER JOIN " + ratingsName + " R2 " 
                    + "ON R1.MovieId = R2.MovieId "
                    + "WHERE R1.UserId = " + userID1 + " AND R2.UserId = " + userID2 + ";");
            while (rs.next())
                list.add(new Pair(rs.getInt(1), rs.getInt(2)));
            stmt.close();
            
        } catch(SQLException e){ e.printStackTrace(); }
        return list;
    }
 
/******************************************************************************************************/
    
    /**
     * This is a specialized function to get testing data from a database,
     * as in the movielens five-fold testing.
     * @param testTable
     * @return
     */

    // 5-fold?, how it is doing it?
    // where is the test table?
    
    public ArrayList<Pair> getTestingData(String testTable)    
    {
        ArrayList<Pair> list = new ArrayList<Pair>();
    
        try        
        {
            Statement stmt = con.createStatement();
        
            ResultSet rs = stmt.executeQuery("SELECT UserId, MovieId " 
            								+ "FROM " + testTable + ";");
            
            while (rs.next())
                list.add(new Pair(rs.getInt(1), rs.getInt(2)));
            
            stmt.close();
        } catch(SQLException e){ e.printStackTrace(); }
        return list;
    }

/******************************************************************************************************/
    
    /**
     * Finds the maximum and minimum movie id's in the database
     * @return a Pair of two integers: minimum and maximum movie id
     */

    public Pair getMaxAndMinMovie()    
    {
        Pair p = null;
    
        try        
        {
            Statement stmt = con.createStatement();
	    
            System.out.println("Getting max and min from table " + moviesName);
        
	        ResultSet rs = stmt.executeQuery("SELECT MIN(MovieId), MAX(MovieId)" 
                    + " FROM " + moviesName + ";");
        
	    if (rs.next())
                p = new Pair(rs.getInt(1), rs.getInt(2));
        
	    stmt.close();
        } catch(SQLException e){ e.printStackTrace(); }
        return p;
    }
    
/******************************************************************************************************/

    //This will be used for finding all the users who have rated two movies (item-based CF)
    
    /**
     * Gets all the users who have seen two movies, and their ratings
     * @param mid1 first movie
     * @param mid2 second movie
     * @return ArrayList of triples of values (int, int, double)
     */

    //I think, these pair and double are just like an object of the class, whenever we
    //want to store a entry, we create a new object and store 
    
    public ArrayList<Triple> getCommonUserAverages(int mid1, int mid2)    
    {
        ArrayList<Triple> triples = new ArrayList<Triple>();
    
        try         
        {
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT r1.Rating, r2.Rating, u.avgRating FROM " +
                    ratingsName + " r1, " +
                    ratingsName + " r2, " + 
                    usersName + " u " + 
                    "WHERE r1.MovieId = " + mid1 +
                    " AND r2.MovieId = " + mid2 +
                    " AND r1.UserId = r2.UserId " + 
            		" AND r1.UserId = u.UserId");
        
            while (rs.next())
                triples.add(new Triple(rs.getInt(1), rs.getInt(2), rs.getDouble(3))); //use of triple
            
            stmt.close();
        } catch(SQLException e){ e.printStackTrace(); }
        return triples;
    }
    
/******************************************************************************************************/
    
    /**
     * Gets the ratings of users common to two movies
     * @param mid1
     * @param mid2
     * @return
     */

    public ArrayList<Pair> getCommonUserRatings(int mid1, int mid2)    
    {
        	ArrayList<Pair> pairs = new ArrayList<Pair>();
    
    	try     	
    	{
            Statement stmt = con.createStatement();
        
            ResultSet rs = stmt.executeQuery("SELECT r1.Rating, r2.Rating FROM " +
                    ratingsName + " r1, " +
                    ratingsName + " r2, " +
                    usersName + " u " +
                    "WHERE r1.MovieId = " + mid1 +
                    " AND r2.MovieId = " + mid2 +
                    " AND r1.UserId = r2.UserId ");
            
            while (rs.next())
                pairs.add(new Pair(rs.getInt(1), rs.getInt(2)));
            
            stmt.close();
        } catch(SQLException e){ e.printStackTrace(); }
        return pairs;
    }
    
/******************************************************************************************************/
    
    public double getAverageMovieRating(int mid)     
    {
        double average = 0.0;
    
        try         
        {
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT avgRating FROM " 
                    + usersName + " WHERE MovieId = " + mid);				//Moviename?
            if (rs.next())
                average = rs.getDouble(1);
            stmt.close();
        } 
        
        catch(SQLException e) { e.printStackTrace(); }
        return average;
    }
}
