
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

    public OpenIntObjectHashMap     movieToCust;
    public OpenIntObjectHashMap     custToMovie;
    public OpenIntDoubleHashMap 	sumByCust;
    public OpenIntDoubleHashMap 	sumByMovie;
    public OpenIntObjectHashMap 	custToMovieRating;
    public OpenIntObjectHashMap 	movieToCustRating;
    
    public OpenIntObjectHashMap 	movieToGenre;		// From a separate file (MovieLens)
    public OpenIntObjectHashMap 	movieToKeywords;
    public OpenIntObjectHashMap 	movieToTags;
    public OpenIntObjectHashMap 	movieToFeatures;
    public OpenIntObjectHashMap 	movieToUnStemmedFeatures;
    
    public OpenIntObjectHashMap 	movieToPlots;  
    public OpenIntObjectHashMap 	movieToCertificates;
    public OpenIntObjectHashMap 	movieToBiography;
    public OpenIntObjectHashMap 	movieToPrintedReviews;
    public OpenIntObjectHashMap 	movieToVotes;
    public OpenIntObjectHashMap 	movieToRatings;
    public OpenIntObjectHashMap 	movieToColors;
    public OpenIntObjectHashMap 	movieToLanguages;
    public OpenIntObjectHashMap 	movieToDirectors;       
    public OpenIntObjectHashMap 	movieToProducers;       
    public OpenIntObjectHashMap 	movieToActors;
    public OpenIntObjectHashMap 	movieToGenres;   	// From the jmdb	
    public IntArrayList				moviesNotMatched;
    
    private 	String smlGenrePath;
    private 	String smlFeaturePath;
    private     FeatureWriter myFeatures;
    
    //For bc dataset
    public boolean     isBC;
    public int         bookMapper;
    public HashMap <String,Integer> bookISBNToFixedID;
    public boolean 	  chooseItem;	
    
    //Random no generator
    private Random 		rand;
    
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
         custToMovieRating = new OpenIntObjectHashMap();
         movieToCustRating = new OpenIntObjectHashMap();
         
         sumByCust =  new OpenIntDoubleHashMap();
         sumByMovie = new OpenIntDoubleHashMap();
         
        
         	movieToGenre 				= new OpenIntObjectHashMap();    //for movie to genre        
	        movieToKeywords 			= new OpenIntObjectHashMap();    //for movie to keywords
	        movieToTags 				= new OpenIntObjectHashMap();    //for movie to tags
	        movieToFeatures				= new OpenIntObjectHashMap();    //for movie to features
	        movieToUnStemmedFeatures	= new OpenIntObjectHashMap();    //for movie to all unstemmed features
	        movieToPlots 				= new OpenIntObjectHashMap();
			movieToCertificates	 		= new OpenIntObjectHashMap();
			movieToBiography 			= new OpenIntObjectHashMap();
			movieToPrintedReviews		= new OpenIntObjectHashMap();			 
			movieToVotes	 			= new OpenIntObjectHashMap();
			movieToRatings 				= new OpenIntObjectHashMap();
			movieToColors 				= new OpenIntObjectHashMap();
			movieToLanguages	 		= new OpenIntObjectHashMap();
			movieToDirectors      		= new OpenIntObjectHashMap();					
			movieToProducers 	    	= new OpenIntObjectHashMap(); 					
			movieToActors	        	= new OpenIntObjectHashMap();
			movieToGenres	        	= new OpenIntObjectHashMap();	
			moviesNotMatched			= new IntArrayList(); 
			 
		 // BC
		 isBC						= false;						//true for BC data
		 bookISBNToFixedID			= new HashMap <String,Integer>();
		 bookMapper					= 1;
		 chooseItem					= true;
		 
		 //Random 
		 rand = new Random();
		 
        smlGenrePath= "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\sml_Genres.dat";
    	//smlFeaturePath = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\sml_storedFeaturesTFIDF.dat"; //For TF-IDF
        smlFeaturePath = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\sml_storedFeaturesTFOnly.dat"; //for TF
    }

 /************************************************************************************************************************/

 /**
  *  We will strip the "X" from Book ID in the Book Crossing Dataset.
  */
    
    public int parseBookAsMovie (String bookISBN)
    {
    	int ID =0;
    	
    	// IF it does not already contained the ISBN
    	if(!(bookISBNToFixedID.containsKey(bookISBN)))
    			{
    				bookISBNToFixedID.put(bookISBN, bookMapper);
    				ID = bookMapper;
    				
    				bookMapper++;
    			}
    	
    	//IF this ISBN has already been assigned a number
    	else 
    		ID = bookISBNToFixedID.get(bookISBN);
    	
    	
    	return ID;
    	
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
            short 		rating;
            double      dummyRating = 0;
            String		date;
            boolean     errorFound;
            int         index =0;
            int         farctionOfItemsToChoose =0;
            
           // int myCheck=0;

            while(in.hasNextLine()) //it is parsing line by line            
            {
            	errorFound = false;
            	farctionOfItemsToChoose++;
            	
            //if(farctionOfItemsToChoose==100 && isBC)     
            {
            	farctionOfItemsToChoose =0;
            	index++;
            	if(index >10000 && index %10000 ==0)
            		System.out.println(index);
            	
            	//if(index > 1000000 && index%10000==0) System.gc();
            	
                line = in.nextLine().split(",");		//delimiter
                
                uid = Integer.parseInt(line[0]);
                if (!(isBC))
                	mid = Integer.parseInt(line[1]);      
                else mid = parseBookAsMovie(line[1]);
                
	          try {
	        	  		dummyRating = Double.parseDouble(line[2]);                
	          	  }
	    
		      	catch(Exception e)
		    	{
		    		//e.printStackTrace();
		    		System.out.println(line[0]);
		    		System.out.println(line[1]);
		    		System.out.println(line[2]);
		    		errorFound = true;
		    	}
	    	
		      	//If there are some faulty ISBNs, skip them
				if(errorFound==false && dummyRating!=0.0)
				{
				     rating  = (short)(dummyRating*100);		//to make 3.45 -->345 and at other end do (345*1.0/100)                   
				     addToMovies (mid, uid, rating);
				     addToCust	(mid, uid, rating);		//call methods to put these things into hashmap
				     
				     //System.gc();
				}
            }//end of if chooseItem
            
	        
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
    // mid as a key, and list of users as a vlaue
    
    public void addToMovies(int mid, int uid, short rating)    
    {

    	OpenIntDoubleHashMap temp;   	 
        LongArrayList list; 							//long list
        IntArrayList  myList;
        if(mid == 0 && uid == 0)
            return;

        if(movieToCust.containsKey(mid)) 				  //Returns true if the receiver contains the specified key        
        {
            list = (LongArrayList) movieToCust.get(mid); //Returns the value to which the specified key is mapped in this identity hash map, or null if the map contains no mapping for this key
            											 //Java class ArrayList(java.util.ArrayList) is a fast and easy to use class representing one-dimensional array
        }												 //It will return list (INT) of customers 
        
        else        
        {
            list = new LongArrayList();
        }
  
        long uuid = uid;
        list.add(uuid<<16 | rating);
        movieToCust.put(mid, list);  						// Associates the given key with the given value. Replaces any old (key,someOtherValue) association, if existing.
         
        //----------------------------
        
        if(movieToCustRating.containsKey(mid)) 				         
        {
             temp =  (OpenIntDoubleHashMap)movieToCustRating.get(mid);            					
        }						 
        
        else        
        {
        	 temp = new OpenIntDoubleHashMap();
        }
              
        temp.put(uid, (rating*1.0/100));        
        movieToCustRating.put(mid, temp);
        
        double sum = sumByMovie.get(mid);					//return 0 if no value is associated with that key
        sumByMovie.put(mid, sum + ((rating*1.0)/100));		//All ratings assigned to a movie
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
    public void addToCust(int mid, int uid, short rating)     
    {

    	LongArrayList list;
        //DoubleArrayList myRatings;
        //IntArrayList myMovies;
    	OpenIntDoubleHashMap temp;
    	
        if(mid == 0 && uid == 0)
            return;

        if(custToMovie.containsKey(uid))					
            list = (LongArrayList) custToMovie.get(uid);
    
        else
            list = new LongArrayList();	 // Constructs an empty list with an initial capacity of ten

        long mmid = mid;
        list.add(mmid<<16 | rating);	// Appends the specified element to the end of this list. 
        custToMovie.put(uid, list);     // Associates the given key with the given value. 
        							    // Replaces any old (key,someOtherValue) association, if existing.
        
     // --------------------------------------
        
        if(custToMovieRating.containsKey(uid))					
        {   
        	  temp = (OpenIntDoubleHashMap) custToMovieRating.get(uid);
          //  myMovies  = temp.keys();
           // myRatings = temp.values(); 
            
        }
    
        else
        	 { 
        		temp = new OpenIntDoubleHashMap();
        	 //	myRatings = new DoubleArrayList();
        	 //	myMovies  = new IntArrayList();        
        	 
        	 }
        
        
      //  myMovies.add(mid);
      //  myRatings.add(rating*1.0/100);
        temp.put(mid,(rating*1.0/100));   // uid --> hashtable (mid, rating)
        custToMovieRating.put(uid, temp);    
         
        
       // if (uid==480)	System.out.println(" uid =" +uid);
         double sum = sumByCust.get(uid); 
         sumByCust.put(uid, sum + ((rating*1.0)/100));  //Put all the ratings given by a particular user/         
        
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

        LongArrayList list;

      //  if(mid == 0 && genre == 0)
      //      return;

        if(movieToGenre.containsKey(mid)) 				  //Returns true if the receiver contains the specified key        
        {
           list = (LongArrayList) movieToGenre.get(mid);  //Returns the value to which the specified key is mapped in this identity hash map, or null if the map contains no mapping for this key
            											  //Java class ArrayList(java.util.ArrayList) is a fast and easy to use class representing one-dimensional array
        }												  //It will return list (INT) of customers 
        
        else        
        {
            list = new LongArrayList();
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

 /***********************************************************************************************************************/

    /**
     *  Verify that our hashes in this object contains the mid as key and features as values
     */
    
   
    public void verifyFeatures()
    {
    	System.out.println("came to verify");
    	int t =0;
    	
    	for (int i=1;i<1600;i++)
    	 {
/*    		if(movieToKeywords.containsKey(i))    		
    				System.out.println("mid =" + i + " keywords size =" + ((HashMap)(movieToKeywords.get(i))).size());
    	
    		if(movieToTags.containsKey(i))
				System.out.println("mid =" + i + " Tags size =" + ((HashMap)(movieToTags.get(i))).size());
    	*/
    	
    		if(movieToFeatures.containsKey(i))
    		{
    			
    			if(((HashMap<String, Double>)movieToFeatures.get(i)).size() <= 10)
    			//if(i==1197)
    			{
    				t++;
    				System.out.println("mid =" + i + " features size =" + ((HashMap<String, Double>)(movieToFeatures.get(i))).size());
    				//System.out.println(movieToFeatures.get(i));
    			}
    	  }   		
    		
    		else 
    			System.out.println("mid ="+i);
    			
    	  } //end of for
    	
    	System.out.println("t=" + t);	
    
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
    		  LongArrayList list = (LongArrayList) second;
        
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
        
        
        String sourceFile = null;
        String destFile = null;
        
    // try         
        {
        	
         //  ML
        /*    sourceFile  =  "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\ml_myNorRatings.dat";
      	    //destFile    =  "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\ml_storedFeaturesRatings.dat";
            destFile    =  "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\ml_storedFeaturesRatingsNorTF.dat"; //for TF
        */
        	//ML Marlin 
        /*    sourceFile  =  "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\SVD\\ml_MainSetMarlinNor20_3.dat";
            destFile    =  "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\SVD\\ml_storedRatingsMarlinNorTF20_3.dat"; //for TF
           */ 
        	// FT
//        	 sourceFile  =  "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\FT\\ft_myRatings.dat";
//        	 destFile    =  "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\FT\\ft_storedFeaturesRatingsTF.dat";
//           	
//        	
        	//SML
       /* 	sourceFile  =  "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\sml_myTimedMovieRatings.dat";
       // 	destFile    =  "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\sml_storedFeaturesRatings.dat";
        	destFile    =  "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\sml_storedFeaturesRatingsTimedMovieTF.dat";
           	*/
        	
        	//BC
         /*	sourceFile  =  "I:/Backup main data march 2010/workspace/MusiRecommender/DataSets/BC/Bc_NorRatings.dat";
            destFile    =  "I:/Backup main data march 2010/workspace/MusiRecommender/DataSets/BC/Bc_NorStoredRatings.dat";
          */
             	
            //NF
       /* 	String myPath = "I:/Backup main data march 2010/Labs and datasets/Compiled Datasets/netflix data set/download";          
        	sourceFile	  =  myPath + "/flatFile.txt";
        	destFile      =  myPath + "/StoredFlatFile.dat";*/
        
        	//ML10
        /*	sourceFile  = "C:/Users/Musi/Desktop/movie_ml_5/movie_ml/ml_data_10M/ratings_Normalized.dat";
        	destFile    = "C:/Users/Musi/Desktop/movie_ml_5/movie_ml/ml_data_10M/ml10_StoredNorRatings.dat";*/
        		
        	sourceFile  = "I:/Backup main data march 2010/workspace/MusiRecommender/DataSets/FT/Itembased/FiveFoldData/ft_myNorRatingsBoth5.dat";
        	destFile 	= "I:/Backup main data march 2010/workspace/MusiRecommender/DataSets/FT/Itembased/FiveFoldData/ft_myNorStoredRatingsBoth5.dat";
            
        	/*sourceFile = "C:/Users/Musi/Desktop/movie_ml_5/movie_ml/ft_data_5/ft_myNorRatingsBoth1.dat";
            destFile   = "C:/Users/Musi/Desktop/movie_ml_5/movie_ml/ft_data_5/ft_myNorStoredRatingsBoth1.dat";
            */
        	
        	//-----------------------------------------------
        	//Read the repective data from files and DB
        	//-----------------------------------------------
        	
        	//Read ratings File
        	  reader.readData(sourceFile);
        /*	
        	//Read Genre File
           	   reader.readGenre(reader.smlGenrePath);
           	
       */     // Verify Genre
            // reader.verifyGenre();
     	   	
           	// Read Features from Mem  
           	// this method will deserialize feature writer object 
           	// and will assign hashes of the memReader class to the featurewriter hashes 
               reader.readFeaturesFromMem();
              
           	// Verify Features
           	   //reader.verifyFeatures();
           	
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
        movieToKeywords	 		= myFeatures.getKeywordsFeatures();
        movieToTags     		= myFeatures.getTagsFeatures();
        movieToFeatures    	    = myFeatures.getAllFeatures();
        movieToUnStemmedFeatures= myFeatures.getAllUnStemmedFeatures();
        
		movieToPlots	 		= myFeatures.getPlotsFeatures();
		movieToPrintedReviews 	= myFeatures.getPrintedReviewsFeatures();
		movieToCertificates     = myFeatures.getCertificatesFeatures();
		movieToBiography 		= myFeatures.getBiographyFeatures();
		movieToColors 			= myFeatures.getColorsFeatures();
		movieToLanguages 		= myFeatures.getLanguagesFeatures();
		movieToVotes 			= myFeatures.getVotesFeatures();
		movieToRatings 			= myFeatures.getRatingsFeatures();
		movieToActors 			= myFeatures.getActorsFeatures();
		movieToDirectors		= myFeatures.getDirectorsFeatures();
		movieToProducers		= myFeatures.getProducersFeatures();
		movieToGenres			= myFeatures.getGenresFeatures();	
		moviesNotMatched	    = myFeatures.getNonMatchingMovies();
    }
    
    
/************************************************************************************************************************/

    //Write First File Into second destination
    /**
     * @param String, source file
     * @param String, dest file
     * @param Boolean, true if we want to write the features
     */
    
    public void writeIntoDisk(String sourceFile, String destFile, boolean feature)    
    {       	
        MemReader reader = new MemReader();
        isBC = false;
        // Read Rating data
 		reader.readData(sourceFile);
	   	reader.sortHashes();
	   	
	   	if(feature == true)
	   	{
		   	// Read Genre from a file
		   	  reader.readGenre(smlGenrePath);
		   	
		   	//Read Features from Memory
	       	   reader.readFeaturesFromMem();   	
	   	}
	   	
       	//Serialize the mem reader object with all the info we have (features etc)
       	serialize(destFile, reader);
	    System.out.println("done writing into desk " + destFile);  
    	
    }
    
    
/************************************************************************************************************************/
    
    
    
}
