
package netflix.memreader;



import java.util.*;
import java.io.*;
import netflix.utilities.*;							//some utilities
import netflix.memreader.FeatureReader;

import cern.colt.list.*;
import cern.colt.map.*;
import cern.colt.function.*;			   //colt



/**
 * Stores a collection of movie ratings efficiently in  
 * memory. Ratings are hashed by both user and movie
 * ids, and additional hashes contain the sum of the 
 * ratings for each user and movie, which allows 
 * for fast calculation of user and movie averages. 
 *
 * This code is modified from an example provided by user
 * "voidanswer" on the Netflixprize forums. 
 * http://www.netflixprize.com/community/viewtopic.php?id=323
 *
 * @author Ben Sowell
 * @author Dan Lew
 */


/**
 * modified by 
 * @author Musi
 */
/************************************************************************************************************************/
public class MemReader implements Serializable
/************************************************************************************************************************/

{

    private static final long serialVersionUID = 7526472295622776147L;    //what is meant by this constant?

    public OpenIntObjectHashMap movieToCust;
    public OpenIntObjectHashMap custToMovie;
    public OpenIntIntHashMap 	sumByCust;
    public OpenIntIntHashMap 	sumByMovie;
    
    public OpenIntObjectHashMap movieToGenre;
    public OpenIntObjectHashMap movieToKeywords;
    public OpenIntObjectHashMap movieToTags;
    public OpenIntObjectHashMap movieToFeatures;
    
    private 	String smlGenrePath;
    private 	String smlFeaturePath;
    private     FeatureWriter myFeatures;
    

    // OpenIntObjectHashMap 
    //Hash map holding (key,value) associations of type (int-->Object); Automatically grows and shrinks as needed;
    //Hash map holding (key,value) associations of type (int-->int); Automatically grows and shrinks as needed

/************************************************************************************************************************/
    /**
     * Default constructor. Initializes hashtables. 
     */
    public MemReader()    
    {
        movieToCust = new OpenIntObjectHashMap();
        custToMovie = new OpenIntObjectHashMap();    
        
        sumByCust =  new OpenIntIntHashMap();
        sumByMovie = new OpenIntIntHashMap();
        
        movieToGenre 	= new OpenIntObjectHashMap();    //for movie to genre
        movieToKeywords = new OpenIntObjectHashMap();    //for movie to genre
        movieToTags 	= new OpenIntObjectHashMap();    //for movie to genre
        movieToFeatures = new OpenIntObjectHashMap();    //for movie to genre
        
        smlGenrePath= "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\sml_Genres.dat";
    	smlFeaturePath = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\sml_storedFeatures.dat";
    }

  /************************************************************************************************************************/
    
    /**
     * Reads a text file in the form 
     *
     * mid,uid,rating
     *
     * and stores this data in the custToMovie and 
     * movieToCust hashtables. 
     *
     * @param  fileName  The file containing the movie
     *                   data in the specified format.
     */
  
    
    public void readData(String fileName)    
    {
        try         
        {

        	Scanner in = new Scanner(new File(fileName));    // read from file the movies, users, and ratings, 

            String[] 	line;
            int 		mid;
            int 		uid;
            byte 		rating;
            String		date;
            
           // int myCheck=0;

            while(in.hasNextLine()) //it is parsing line by line            
            {

            //	if(myCheck++==100) break;
            	
                line = in.nextLine().split(",");		//delimiter
                
                uid = Integer.parseInt(line[0]);
                mid = Integer.parseInt(line[1]);      
                //System.out.println ("rating =" + line[2]);
                rating = Byte.parseByte(line[2]);
				
            // System.out.println ("mid=" + mid + ", uid=" + uid + ", rating=" + rating);
                
                addToMovies	(mid, uid, rating);
                addToCust	(mid, uid, rating);		//call methods to put these things into hashmap

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
    }

 /************************************************************************************************************************/
    
    /**
     * Reads a text file in the form 
     *
     * mid, genre (mid can lie in multiple genres)
     *
     * Store this information, in MovieToGenre openIntObjectHashMap 
     *  
     *
     * @param  fileName  The file containing the genre
     *                   data in the specified format.
     */
  
    
    public void readGenre(String fileName)     
    {
        try         
        {

            Scanner in = new Scanner(new File(fileName));    // read from file the movies, users, and ratings, 

            String[] 	line;
            int 		mid;
            int 		genre;
            
            while(in.hasNextLine()) //it is parsing line by line            
            {           	
                line  = in.nextLine().split(",");		//delimiter                
                mid   = Integer.parseInt(line[0]);
                genre = Integer.parseInt(line[1]);                       
            
                addToGenre(mid, genre); //add information into a hash table                
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
    }
    
/************************************************************************************************************************/
    
    /**
     * Serializes a MemReader object so that it can be
     * read back later. 
     *
     * @param  fileName  The file to serialize to. 
     * @param  obj  The name of the MemReader object to serialize.
     */
    public static void serialize(String fileName, MemReader obj)     
    {

        try        
        {
            FileOutputStream fos = new FileOutputStream(fileName);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(obj);		//write the object
            os.close();
        }
        
        catch(FileNotFoundException e) {
            System.out.println("Can't find file " + fileName);
            e.printStackTrace();
        }
        
        catch(IOException e) {
            System.out.println("IO error");
            e.printStackTrace();
        }
    }

  /************************************************************************************************************************/
    
    /**
     * Deserializes a previously serialized MemReader object. 
     *
     * @param  fileName  The file containing the serialized object. 
     * @return The deserialized MemReader object. 
     */
    
    public static MemReader deserialize(String fileName)
    {
        try         
        {
            FileInputStream fis    = new FileInputStream(fileName);
            ObjectInputStream in   = new ObjectInputStream(fis);

            return (MemReader) in.readObject();	//deserilize into memReader class 
        }
        
        catch(ClassNotFoundException e) {
            System.out.println("Can't find class");
            e.printStackTrace();
        }
        catch(IOException e) {
            System.out.println("IO error");
            e.printStackTrace();
        }

        //We should never get here
        return null;
    }


/************************************************************************************************************************/

    //Database consists of,
    
    /*
     *  (mid, all users who saw that movie + rating)
     *  (uid, all movies seen by that users + rating)   // it is basically a profile of that user in terms of ratings
     *   
     *   So I have to build another such that
     *   
     *   (uid, weighted keywords (depend on how much he rated that movie)
     *   (and also on tf-idf where tf in one movie and idf from all movies he has seen?)
     *   from all movies he has seen) 
     * 
     *   (mid, keywords for that movie)
     *   
     *   so Matching algorithm should be there, which matches a given active movie with active user profile and
     *   tells how much user will love it
     *
     *
     *  furthermore, we have
     *  
     *  (uid, sum of all ratings assigned to it by that user)
     *  (mid, sum of all ratings assiged to it)
     * 
     * 
     * 
     * 
     * 
     * 
     */
    
    
    /**
     * Adds an entry to the movieToCust hashtable. The
     * uid and rating are packed into one int to 
     * conserve memory. 
     *
     * @param  mid  The movie id. 
     * @param  uid  The user id. 
     * @param  rating  User uid's rating for movie mid.
     */

    //(mid,list of users who say that movie)
    //mid as a key, and list of users as a vlaue
    
    public void addToMovies(int mid, int uid, byte rating)    
    {

        IntArrayList list;

        if(mid == 0 && uid == 0)
            return;

        if(movieToCust.containsKey(mid)) 				  //Returns true if the receiver contains the specified key      
        {
            list = (IntArrayList) movieToCust.get(mid); //Returns the value to which the specified key is mapped in this identity hash map, or null if the map contains no mapping for this key
            											//Java class ArrayList(java.util.ArrayList) is a fast and easy to use class representing one-dimensional array
        }												//It will return list (INT) of customers 
        
        else        
        {
            list = new IntArrayList();
        }

        list.add(uid<<8 | rating);
        movieToCust.put(mid, list);  					// Associates the given key with the given value. Replaces any old (key,someOtherValue) association, if existing. 

        
        int sum = sumByMovie.get(mid);					//return 0 if no value is associated with that key
        sumByMovie.put(mid, sum + rating);				//All ratings assigned to a movie

    }

 /************************************************************************************************************************/
    
    /**
     * Adds an entry to the movieToGenre hashtable. The
     *
     * @param  mid  The movie id. 
     * @param  uid  The genre.     
     */

    //(mid,list of genres which lies under that movie)
    // mid as a key, and list of genres as a value
    
    public void addToGenre(int mid, int genre)    
    {

        IntArrayList list;

      //  if(mid == 0 && genre == 0)
      //      return;

        if(movieToGenre.containsKey(mid)) 				  //Returns true if the receiver contains the specified key        
        {
           list = (IntArrayList) movieToGenre.get(mid);  //Returns the value to which the specified key is mapped in this identity hash map, or null if the map contains no mapping for this key
            											 //Java class ArrayList(java.util.ArrayList) is a fast and easy to use class representing one-dimensional array
        }												 //It will return list (INT) of customers 
        
        else        
        {
            list = new IntArrayList();
        }

        list.add(genre);
        movieToGenre.put(mid, list);  					// Associates the given key with the given value. Replaces any old (key,someOtherValue) association, if existing. 
        //System.out.println(" genre(mid).size =" + ((IntArrayList)(movieToGenre.get(mid))).size()); //(So it is writing something)
        
    }
    
    //------------------------------
    
    //It is showing there are some movies with no genre information
    public void verifyGenre()
    {
    	for (int i=1;i<1600;i++)
    		if(movieToGenre.containsKey(i))
    				System.out.println("mid =" + i + " genre size =" + ((IntArrayList)(movieToGenre.get(i))).size());
    	
    }

    
 /************************************************************************************************************************/   
    
    /**
     * Adds an entry to the custToMovie hashtable. The
     * mid and rating are packed into one int to 
     * conserve memory. 
     *
     * @param  mid  The movie id. 
     * @param  uid  The user id. 
     * @param  rating  User uid's rating for movie mid.
     */
    
    // (uid, list of movies seen by that user)
    //  uid as a key, and list of movies as values
    public void addToCust(int mid, int uid, byte rating)     
    {

        IntArrayList list;

        if(mid == 0 && uid == 0)
            return;

        if(custToMovie.containsKey(uid))					
            list = (IntArrayList) custToMovie.get(uid);
    
        else
            list = new IntArrayList();		// Constructs an empty list with an initial capacity of ten

        list.add(mid<<8 | rating);			//Appends the specified element to the end of this list. 
        custToMovie.put(uid, list); 		// Associates the given key with the given value. 
        									//Replaces any old (key,someOtherValue) association, if existing.
        
         int sum = sumByCust.get(uid); 
         sumByCust.put(uid, sum + rating);  //Put all the ratings given by a particular user/         
        
    }
 

/************************************************************************************************************************/

    /**
     *  Verify that our hashes in this object contains the mid as key and features as values
     */
    
   
    public void verifyFeatures()
    {
    	System.out.println("came to verify");
    	
    	for (int i=1;i<1600;i++)
    		{
    		if(movieToKeywords.containsKey(i))    		
    				System.out.println("mid =" + i + " keywords size =" + ((HashMap)(movieToKeywords.get(i))).size());
    	
    		if(movieToTags.containsKey(i))
				System.out.println("mid =" + i + " Tags size =" + ((HashMap)(movieToTags.get(i))).size());
    	
    	if(movieToFeatures.containsKey(i))
			System.out.println("mid =" + i + " features size =" + ((HashMap)(movieToFeatures.get(i))).size());
		}

    
    }

    
 /************************************************************************************************************************/
    
    
    /**
     * Sorts each entry in the movieToCust and 
     * custToMovie hashes to allow for efficient
     * searching. 
     */
    
    public void sortHashes()    
    {
        Sorter sorter = new Sorter();
        
        movieToCust.forEachPair(sorter);
        custToMovie.forEachPair(sorter);	//apply to each pair so It goes like a loop?
        movieToGenre.forEachPair(sorter);
    }


/************************************************************************************************************************/
    /**
     * This class is used with the forEachPair method
     * of an OpenIntObjectHashMap when the Object is 
     * an IntArrayList. The apply method sorts the 
     * IntArrayList in ascending order. 
     */

/************************************************************************************************************************/
/************************************************************************************************************************/
    
    private class Sorter implements IntObjectProcedure //any of its created object should implement its methods?
    
    {

        /**
         * Sorts the IntArrayList in ascending order. 
         *
         * @param  first  uid or mid
         * @param  second IntArrayList of ratings. 
         * @return true
         */
        
    	public boolean apply(int first, Object second) 
        
        {
            IntArrayList list = (IntArrayList) second;
        
            list.trimToSize();
            list.sortFromTo(0, list.size() -1);		//this is sorting from first to last index
            
            return true;
        }
        
    } //end of sort class

 /************************************************************************************************************************/
    
    public static void main(String args[])    
    {

        MemReader reader = new MemReader();
        FeatureWriter frw = new FeatureWriter();
        
        //String smlGenrePath= "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\sml_Genres.dat";
        
        String netflixPath= "C://Mera data in IBM laptop//Musi Ke PhD//Compiled Datasets//netflix data set//download";
        String filmTrustPath= "C://Users//Musi//workspace//MusiRecommender//DataSets//FT//TestTrain//";
        
        String sourceFile = null;
        String destFile = null;
        
    // try 
        
        {
        	
// 	            sourceFile = args[0];
//              destFile   = args[1];				//recieve files from arguments

        //ML
      // 	sourceFile  =  "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\ml_ratings.dat";
      //	destFile    =  "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\ml_storedRatings.dat";
        
       
        	//for writing sml_ratings into memory;
        	sourceFile  =  "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\sml_ratings.dat";
        	destFile    =  "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\sml_storedFeaturesRatings.dat";
           	
        	
        	
        	//-----------------------------------------------
        	//Read the repective data from files and DB
        	//-----------------------------------------------
        	
        	//Read ratings File
        	reader.readData(sourceFile);
        	
        	//Read Genre File
           	reader.readGenre(reader.smlGenrePath);
           	
            // Verify Genre
            // reader.verifyGenre();
     	   	
           	// Read Features from Mem  
           	// this method will deserialize feature writer object 
           	// and will assign hashes of the memReader class to the featurewriter hashes 
              reader.readFeaturesFromMem();
              
           	// Verify Features
           	// reader.verifyFeatures();
           	
           	//Sort hashes
           	reader.sortHashes();                  
    	    
    	   	//Serialize all the data by serializing memreader Object
    	   	System.out.println("done reading data");
            serialize(destFile, reader);
    	    System.out.println("done writing");
         
        	//------------------------------------------------
    	    
    	    
        //netflix main data
  /*  	 
     		sourceFile  =  netflixPath + "//flatfile.txt";
    	   	destFile    =  netflixPath + "//storedFlatfile.dat";	//write in the form of dat file
    	        
    	   	reader.readData(sourceFile);
    	   	reader.sortHashes();
                  
    	    System.out.println("done netflix data read");

            serialize(destFile, reader);
    	    
*/
        
          /*  IntArrayList users = reader.custToMovie.keys();

            
            for(int i = 0; i < users.size(); i++) 
            
            {	   System.out.println(users.get(i) + ", ");			// Returns the element at the specified position in the receiver. 
                   System.out.println(reader.custToMovie.get(i));	//get all movies seen by a user								
            }
       
           */
            
      //      System.out.println("done");	

            
            //so after storing each record, we store it as an object and then will deserialize it
             
         //     serialize(destFile, reader);

            //________________________________________________________________
            //I want to create 10 objects and will store them into serialize...
            
       }
        
  
             
      
  /*      
        catch(Exception e) 
        
        {
            System.out.println("usage: java MemReader sourceFile destFile");
            e.printStackTrace();
        }
        
        System.out.println("Finish in MemReader");
        
    */    
    
  }//end of function
    
/************************************************************************************************************************/

    public void readFeaturesFromMem()
    {
    	
    	//call static method of the featureWriter class and deserialize the object
        myFeatures = FeatureWriter.deserialize(smlFeaturePath);

        //Now assign hashes from other file to MemReader hashes        
        movieToKeywords	 	= myFeatures.getKeywordsFeatures();
        movieToTags     	= myFeatures.getTagsFeatures();
        movieToFeatures     = myFeatures.getAllFeatures();
        
        
    }
    
    
/************************************************************************************************************************/

    //Write First File Into second destination
    public void writeIntoDisk(String sourceFile, String destFile)    
    {   
    	
        MemReader reader = new MemReader();
        
        // Read Rating data
 		reader.readData(sourceFile);
	   	reader.sortHashes();
	   	
	   	// Read Genre from a file
	   	reader.readGenre(smlGenrePath);
	   	
	   	//Read Features from Memory
       	reader.readFeaturesFromMem();   	
	   	
       	
       	//Serialize the mem reader object with all the info we have (features etc)
       	serialize(destFile, reader);
	    System.out.println("done writing into desk " + destFile);
	    

        
    	
    }
    
    /************************************************************************************************************************/
    
    
    
}
