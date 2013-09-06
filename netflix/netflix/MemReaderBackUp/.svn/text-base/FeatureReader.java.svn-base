package netflix.memreader;

import java.util.*;
import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

//Lucene Packages
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.LowerCaseTokenizer;
import org.apache.lucene.analysis.PorterStemFilter;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.Token;


import netflix.memreader.MemReader;
import com.mysql.jdbc.Blob;
import com.sun.xml.internal.fastinfoset.util.StringArray;

import cern.colt.list.IntArrayList;
import cern.colt.list.ObjectArrayList;
import cern.colt.map.OpenIntIntHashMap;
import cern.colt.map.OpenIntObjectHashMap;


/*********************************************************************************************************/


public class FeatureReader 
{
  
    
		//class variables
		public	  int 					totalMoviesML;
		protected Connection 			conML, conIMDB;
		protected String 				dbNameML, dbNameIMDB;
		protected String 				ratingsNameML, ratingsNameIMDB;
				
	    public OpenIntObjectHashMap 	movieToKeywords; //movies, and then a list of keywords (Strings): 10    
	    public OpenIntObjectHashMap 	movieToTags; 	  //movies, and then a list of keywords (Strings): 9
	    public OpenIntObjectHashMap 	movieToFeatures; 	  //movies, and then a list of keywords (Strings): 9
	   
	    public OpenIntObjectHashMap 	movieToKeywordsFinal; //movies, and then a list of keywords (Strings): 10    
	    public OpenIntObjectHashMap 	movieToTagsFinal; 	  //movies, and then a list of keywords (Strings): 9
	    public OpenIntObjectHashMap 	movieToFeaturesFinal; 	  //movies, and then a list of keywords (Strings): 9
	   
	    public OpenIntObjectHashMap 	dumMovieToKeywords;     
	    public OpenIntObjectHashMap 	dumMovieToTags; 	
	   
	    
	    private String 					destFile;       //where we wanna write our dest file
	    
	    /**
	     * An array containing some common English words
	     * that are usually not useful for searching.
	     */
	    public static final String[] STOP_WORDS =
	    {
	        "0", "1", "2", "3", "4", "5", "6", "7", "8",
	        "9", "000", "$",
	        "about", "after", "all", "also", "an", "and",
	        "another", "any", "are", "as", "at", "be",
	        "because", "been", "before", "being", "between",
	        "both", "but", "by", "came", "can", "come",
	        "could", "did", "do", "does", "each", "else",
	        "for", "from", "get", "got", "has", "had",
	        "he", "have", "her", "here", "him", "himself",
	        "his", "how","if", "in", "into", "is", "it",
	        "its", "just", "like", "make", "many", "me",
	        "might", "more", "most", "much", "must", "my",
	        "never", "now", "of", "on", "only", "or",
	        "other", "our", "out", "over", "re", "said",
	        "same", "see", "should", "since", "so", "some",
	        "still", "such", "take", "than", "that", "the",
	        "their", "them", "then", "there", "these",
	        "they", "this", "those", "through", "to", "too",
	        "under", "up", "use", "very", "want", "was",
	        "way", "we", "well", "were", "what", "when",
	        "where", "which", "while", "who", "will",
	        "with", "would", "you", "your",
	        "a", "b", "c", "d", "e", "f", "g", "h", "i",
	        "j", "k", "l", "m", "n", "o", "p", "q", "r",
	        "s", "t", "u", "v", "w", "x", "y", "z"
	    };
	    
	    
/********************************************************************************************************/
	    
		/**
		 * Default constructor.
		 * 
		 * Sets up a connection to the database "recommender", using
		 * the table name "ratings" for ratings and "movies" for movies.
		 */
	    
	    
		
		public FeatureReader()		
		{
		      // totalMoviesML 	= 100; //1682
		       totalMoviesML 	= 1682;
			dbNameML 		= "movielens";
			dbNameIMDB 		= "imdb";
			
			ratingsNameML   = "sml_movies";
			ratingsNameIMDB = "movie_info";
		
			
			 movieToKeywords 			= new OpenIntObjectHashMap();
			 movieToTags	 			= new OpenIntObjectHashMap();
			 movieToFeatures 			= new OpenIntObjectHashMap();
			 
			 movieToKeywordsFinal		= new OpenIntObjectHashMap();
			 movieToTagsFinal	 		= new OpenIntObjectHashMap();
			 movieToFeaturesFinal		= new OpenIntObjectHashMap();
			 
			 dumMovieToTags	 			= new OpenIntObjectHashMap();			 
			 dumMovieToKeywords 		= new OpenIntObjectHashMap();
			 
			 destFile  =  "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\sml_storedFeatures.dat";

			 
			 
		}
		

/******************************************************************************************************/

		/**
		 * @author steinbel - modified from Enchilada
		 * Opens the connection to the MySQL db "recommender".  If password changes
		 * are made, they should be made in here - password and db name are hard-
		 * coded in at present.
		 * @return boolean true on successful connection, false if problems
		 */
		
		public boolean openConnection(String name) //d.b. name		
		{
			boolean success = false;			
			
			try			
			{
			
				Class.forName("com.mysql.jdbc.Driver");
				
				if(name.equals("movielens"))
				{
					conML = DriverManager.getConnection("jdbc:mysql://" +
					"localhost:3306/" + name, "root", "ali5mas5");			
					success = true;
				}
				
				else
				{
					conIMDB = DriverManager.getConnection("jdbc:mysql://" +
					"localhost:3306/" + name, "root", "ali5mas5");			
					success = true;
				}
				
				

			} catch (Exception e){
				System.err.println("Error getting connection.");
				e.printStackTrace();
			}

			System.out.println("Connection created ");
			return success;
		}

/******************************************************************************************************/
		
		/**
		 * @author steinbel - lifted from Enchilada
		 * Closes the connection to the db.
		 * @return boolean true on successful close, false if problems
		 */
		
		public boolean closeConnection(Connection con)		
		{
			boolean success = false;
			
			try
			{
				con.close();
			
				success = true;
			} 
			catch (Exception e){
				System.err.println("Erorr closing the connection.");
				e.printStackTrace();
			}
			return success;
		}

/******************************************************************************************************/
		
		/**
		 * @author steinbel - lifted from Enchilada
		 * Returns the connection to the db.
		 * @return Connection con
		 */
		
		public Connection getConnectionML()		
		{
			return conML;
		}

		/**
		 * @author steinbel - lifted from Enchilada
		 * Returns the connection to the db.
		 * @return Connection con
		 */
		
		//-------------------------------------------
		
		public Connection getConnectionIMDB()		
		{
			return conML;
		}
		
/******************************************************************************************************/

		public ResultSet queryDB(String query, Connection con)		
		{
			ResultSet rs = null;
		
			try
			{
				Statement stmt = con.createStatement();
				rs = stmt.executeQuery(query);
				/* NOTE: cannot manually close Statment here or we
				 * lose the ResultSet access.
				 * May want to change this into a CachedRowSet to
				 * deal with that.  Also, what about memory limitations?
				 */
			}
			
			catch(SQLException e){ e.printStackTrace(); }
			return rs;
		}

		
/******************************************************************************************************/

		/**
	     * Adds an entry to the movieToKeywords, movieToTags etc hashtable. 
	     *    
	     *    If a movie has no keywords (for example), then simply we do not put anything.
	     *    When we have to retrieve movie's keywords, check it is not null first
	     *    
	     *    @param type = 9, 10 etc
	     *    @param Info = keywords, or tags (string form, single)
	     *    @param mid = ML mid
	     */
    
	    public void addFeaturesToMovies(int type, String info, int mid)    
	    {
	    	
	        ObjectArrayList list;
 
	        
	      //  if(mid == 0 && genre == 0)
	      //      return;

	        
	      //----------------------------------------
	        if(type==9) //tags
	        {
	        	if(	dumMovieToKeywords.containsKey(mid))	        		
	        	{
	        	  list = (ObjectArrayList) dumMovieToKeywords.get(mid);	        	
	        	
	        	}
	        	
	        	else
	        	{
	        	 list = new ObjectArrayList();	
	        	}
	        	
	        	list.add(info);
	        	dumMovieToKeywords.put(mid, list);
	        }
	       //----------------------------------------
	       
	        if(type==10) //keywords
	        {
	        	if(	dumMovieToTags.containsKey(mid))	        		
	        	{
	        	  list = (ObjectArrayList) dumMovieToTags.get(mid);	        	
	        	
	        	}
	        	
	        	else
	        	{
	        	 list = new ObjectArrayList();	
	        	}
	        	
	        	list.add(info);
	        	dumMovieToTags.put(mid, list);
	        }
	       
	        
	        	        
	    }


/******************************************************************************************************/
	    
	    /**
	     *  Get information about a movie from imdb
	     *  @param imdbId, MlId
	     *  @return void
	     */
	    
		public void getInfo(int imdbId, int mlId)
		{
			//System.out.println(imdbId);
			//System.out.println("-----------------------------------------");
			
			try
			{
				Statement stmt = conIMDB.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT info_type_id, info FROM " + ratingsNameIMDB + " " 
											      + "WHERE movie_id= " + imdbId + ";");
				
						
				while (rs.next())					
				{		
					   int type= rs.getInt(1);				   
					   String info = rs.getString(2);	
					  					   
					    // Now we can add the keywords info, tags info, into hashtables of movieToInfo
						// Actual ML mid is used as key and the value retrive from above (against each type) is used as
						// Value. A Separate hashtable is used for each type.
							   
					  if(type==10 || type==9)
						      addFeaturesToMovies(type, info, mlId);
				}
				
							
				//close the statement
				stmt.close();
			}
			
			catch(SQLException e){ e.printStackTrace(); } 
		}
		
/******************************************************************************************************/
	
/**
 * return the imdbId associated with a movielens Id
 * @param movielens mid
 * @return imdb mid
 */
		
	public int getIMDBId (int mid)		
	{
			int imdbId =0;
			
		try		
		{
			Statement stmt = conML.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT imdbPyId FROM " + ratingsNameML + " " 
									        + "WHERE MovieId= " + mid + ";");
	
				   while(rs.next())
				   {
					   imdbId= rs.getInt(1);
					   break;
				   }
				   
				   stmt.close();
		}
		
		catch(SQLException e){
			e.printStackTrace();
			e.getMessage();
			} 
		
		return imdbId;
		
	}		
		
		
/******************************************************************************************************/
// Main Mathod
/******************************************************************************************************/
	
	/**
	 * Main method
	 */
		public static void main(String[] args)		
		{
			FeatureReader rw= new FeatureReader();			
			rw.getAllData();
			
		   
			   
		}
		
/******************************************************************************************************/		

		
	// This method is called from other class, as I think, Java can not read class having database objects
	// The other class will get its hashes and then will write them into memory 	
	// Just deserialize the written object from anu file and call ites getKeywordsFeature(), and similar methods
	 	
	   public void getFeatures()		
	   {
				
			getAllData();
			
	   }

/******************************************************************************************************/
		/**
		 * @ return openintObjectHashMap of keywords 
		 */
		
		public OpenIntObjectHashMap getKeywordsFeatures()
		{
		//	return movieToKeywords;
			return movieToKeywordsFinal;
		}
		

/******************************************************************************************************/

		/**
		 * @return OpenIntObjectHashMap of tags 
		 */
		
		public OpenIntObjectHashMap getTagsFeatures()
		{
			//return movieToTags;
			return movieToTagsFinal;
		}
		
/******************************************************************************************************/
		
		/**
		 * @return OpenIntObjectHashMap of features 
		 */
		
		public OpenIntObjectHashMap getAllFeatures()
		{
			//return movieToFeatures;
			return movieToFeaturesFinal;
		}
		
		
/******************************************************************************************************/
/******************************************************************************************************/
	/**
	 * Get all the data from imdb
	 */

	  public void getAllData()
	  {
		  	int myId=0;
		  	OpenIntIntHashMap myIdMap = new OpenIntIntHashMap();  //for mlId-->imdbId
		  	
		  	//---------------------------------
			//open ML connection
		    //---------------------------------
		  	
		  	openConnection("movielens");			
			
			for(int i=1;i<=totalMoviesML;i++) 	//all movies in movielens (1682)
			{
				
				//get imdb id against a movielens id
				myId = getIMDBId(i);
				myIdMap.put(i, myId);
				
			} 	
			
			//check for propoer integration
/*			int howMuchIntegrated=0;
			IntArrayList myList1 = new IntArrayList();
			IntArrayList myList2 = new IntArrayList();
			
			for(int i=1;i<=totalMoviesML;i++) 	//all movies in movielens (1682)
			{
				if(myIdMap.containsKey(i))
						{
							int imdbid = myIdMap.get(i);
							System.out.println("mid= "+ imdbid);
							
							howMuchIntegrated++;
							myList1.add(imdbid);
							if(!(myList2.contains(imdbid)));
									myList2.add(imdbid);
							
						}
			}
			
			System.out.println("Map Size = "+ myIdMap.size());
			System.out.println("list1 Size = "+ myList1.size());
			System.out.println("list2 Size = "+ myList2.size());*/
			
			closeConnection(conML);
			
			//--------------------------------
			//open IMDB connection
			//--------------------------------
			
			openConnection("IMDB");
			
			for(int i=1;i<=totalMoviesML;i++) 	//all movies in movielens (1682)
			{
				myId = myIdMap.get(i);
				 //System.out.println("ml_id=" + i +",imdb_id " + myId);
				 
				//get keywords etc against a movie
				if(myId!=0)		
					getInfo(myId, i);
				
				 if (i>=100 && i%100==0) System.out.println(i);
				
			}
			
			//close IMDB connection
			closeConnection(conIMDB);
			
			//------------------------------------
			//Here we can do stemming TF-IDF 
			//------------------------------------
			
			doStemming();
			System.out.println(" Finished writing stemmed tokens");
		
			//verifyTF();
			
			calculateIDF();
			//verifyWeights();
			System.out.println(" Finished doing TF-IDF");
			
			
		}

/********************************************************************************************************/	  
/*********************************************************************************************************/
// stemming Part Start
/*********************************************************************************************************/
	  
	  
	  public void doStemming()
	  {

		    ObjectArrayList list	=	null;
		    int type				=   0;
		    
		    //------------------------------------------------------------------------------
		    // Loop to all movies 1-1682
		    //------------------------------------------------------------------------------
		    
		    for (int mid=1;mid<=totalMoviesML;mid++)
		    {

			    //-----------------------------------------------------
			    // Keywords
			    //-----------------------------------------------------		    	
		    	
		    	if(	dumMovieToKeywords.containsKey(mid))	        		
		       	{
		       	  list  		= (ObjectArrayList) dumMovieToKeywords.get(mid);	      
		       	  type 			= 10;
		       	  int listSize  = list.size();
		       	  
			    	// Now we have to add token in the stemmed version, i.e. single words 
			    	// Which can be used for Vector similairty as well,
	    	
		    		// All featrures found in one movie
		    		for(int i=0;i<listSize;i++)
			    	 {
			    		
		    			//---------------------------
		    			// Token reading
		    			//---------------------------

		    			//Read string to be checked
			   		  	Reader myReader = new StringReader ((String)list.get(i));	    	    
			   		    TokenStream ts = tokenStream (null, myReader);  		 
			
			   		    //start reading stemmed tokens
			   		    
							 try 
							 {
								 String myS ="";
								 
								//read tokens
								Token token = ts.next();
								if (token !=null)  myS  = token.termText();	//convert to string  
								
									// while we have tokens, read
									 while(token!=null)
									  {
										 //Write this token in the list
										 writeTokenInMovieFeature(type, mid, myS);
										 token = ts.next();		
										 if(token!=null) myS   = token.termText();
										 //System.out.println("token -->" + myS);
									  }
											  
			
							 }
						 
							 catch (Exception E)
							 {
								  System.out.println("Exception while making tokens");
								  E.printStackTrace();
							 }
			    	 }//end of all elements in a list for a movie				 
		    	} // end of reading type = 10
		    	
		    	
		    	//-----------------------------------------------------
			    // Keywords
			    //-----------------------------------------------------		    	
		    	
		    	if(	dumMovieToTags.containsKey(mid))	        		
		       	{
		       	  list  		= (ObjectArrayList) dumMovieToTags.get(mid);	      
		       	  type  		= 9;	       	  
		       	  int listSize  = list.size(); 
			    	
		    		// All featrures found in one movie
		    		for(int i=0;i<listSize;i++)
			    	 {
			    		
		    			//---------------------------
		    			// Token reading
		    			//---------------------------

		    			//Read string to be checked
			   		  	Reader myReader = new StringReader ((String)list.get(i));	    	    
			   		    TokenStream ts = tokenStream (null, myReader);  		 
			
			   		    //start reading stemmed tokens
			   		    
							 try 
							 {
								 String myS ="";
								 
								//read tokens
								Token token = ts.next();
								if(token!=null) myS  = token.termText();	//convert to string  
								
									// while we have tokens, read
									 while(token!=null)
									  {
										 //-------------------------------
										 //Write this token in the list
										 //TF
										 //-------------------------------
										 
										 writeTokenInMovieFeature(type, mid, myS);
										 token = ts.next();		
										 if(token!=null) myS   = token.termText();
									  }
											  
									  //System.out.println("\n");
							 }
						 
							 catch (Exception E)
							 {
								  System.out.println("Exception while making tokens");
								  E.printStackTrace();
							 }
							 
			    	 }//end of all elements in a list for a movie				 
		    	} // end of reading type = 9

   	
		    } //end of movie for (all movies loop)

} //end of the function
	  
	  
 /***********************************************************************************************************/
	  
	  /**
	   * Class with seriers of filters
	   * @return the seried filter 
	   */
	  
	  public TokenStream tokenStream(String fieldName, Reader reader) 
	  {
	      Tokenizer tokenizer = new StandardTokenizer(reader);
	      TokenFilter lowerCaseFilter = new LowerCaseFilter(tokenizer);
	      TokenFilter stopFilter = new StopFilter(lowerCaseFilter, STOP_WORDS);
	      TokenFilter stemFilter =  new PorterStemFilter(stopFilter);
	      return stemFilter;
	  } 
	 			
/***********************************************************************************************************/	 		
	  
	  /**
	     * Adds an entry to the movieToKeywords, movieToTags etc hashtable. 
	     *    
	     *    If a movie has no keywords (for example), then simply we do not put anything.
	     *    When we have to retrieve movie's keywords, check it is not null first
	     *    
	     *    @param type = 9, 10 etc
	     *    @param Info = keywords, or tags (string form, single)
	     *    @param mid = ML mid
	     */
  
	  
	public void  writeTokenInMovieFeature (int type, int mid, String token)
	{
	    
	    double inc = 1;
	    Map <String,Double> mapTF;
	    
	     if(type==9) //tags
         {
	        	if(movieToKeywords.containsKey(mid))	        		
	        	{
	    	        mapTF = (Map<String, Double>) movieToKeywords.get(mid);
	        	}
	        	
	        	else
	        	{
	    
	        	 mapTF = new HashMap<String, Double> ();
	        	}
	        	
	       // Only add if we have not already added this word
	        	if(mapTF.containsKey(token))
	        	{
	        		//TF increment
	        		inc = mapTF.get(token);
	        		inc++;
	        		mapTF.put(token, inc);
	        		
	        	}
	        
	        	else
	        		mapTF.put(token, inc);
	    
	        	 movieToKeywords.put(mid, mapTF);
	       }

	   //----------------------------------------
	      
	     mapTF  = null;
	     inc	= 1;
	    
	     if(type==10) //tags
         {
	        	if(movieToTags.containsKey(mid))	        		
	        	{
	    	        	
	        	  mapTF = (Map<String, Double>) movieToTags.get(mid);
	        	}
	        	
	        	else
	        	{
	    
	        	 mapTF = new HashMap<String, Double> ();
	        	}
	        	
	       // Only add if we have not aalready added this word
	        	if(mapTF.containsKey(token))
	        	{
	        		//TF increment
	        		inc = mapTF.get(token);
	        		inc++;
	        		mapTF.put(token, inc);
	        		
	        	}
	        	 
	        	else  
	        		mapTF.put(token, inc);
	             
	        	movieToTags.put(mid, mapTF);
	       }


	     
	     //---------------------------------------
	     //features (all)
	    mapTF = null;
	    inc   = 1;
	    
        	if(movieToFeatures.containsKey(mid))	        		
	        	{
	    	        	
	        	  mapTF = (Map<String, Double>) movieToFeatures.get(mid);
	        	}
	        	
	        	else
	        	{
	    
	        	 mapTF = new HashMap<String, Double> ();
	        	}
	        	
	       // Only add if we have not aalready added this word
	        	if(mapTF.containsKey(token))
	        	{
	        		//TF increment
	        		inc = mapTF.get(token);
	        		inc++;
	        		mapTF.put(token, inc);
	        		
	        	}
	        	
	        	else 
	        		mapTF.put(token, inc);
	        
	             movieToFeatures.put(mid, mapTF);
	                
	} //end of function
	
	
	//-------------------------------------------------
	// Method that verifies the TF 
	// This method was showing that same words in diff movies having the diff TF

	public void verifyTF()
	{ 
		Map <String,Double> myMap;
		Map <String,Double> tempMap;
		
		for (int i=1;i<=totalMoviesML;i++)
		{
			if(movieToFeatures.containsKey(i))
			{
				myMap = (HashMap<String,Double>) movieToFeatures.get(i);
				
			  Set	set = myMap.entrySet();	    	      	       	  
  	      	  Iterator j = set.iterator();
  	      	  	    	      	  
  	      	while(j.hasNext()) 
  	      	{
  	      	     Map.Entry words = (Map.Entry)j.next();	    	      	  
  	      		 
  	      	     String word = (String)words.getKey();			//get the word
  	      		 double tf   = (Double)words.getValue();	
  	     
  	      		for (int k=1;k<=totalMoviesML;k++)
  	      		{
  	      		
  	      			if(movieToFeatures.containsKey(k))
	  	      		{
  	      			    tempMap = (HashMap<String,Double>) movieToFeatures.get(k);
  	      			    
  	      			    if(tempMap.containsKey(word))
  	      			    {
  	      			    	double tf1 = tempMap.get(word);
  	      			        if(tf!=tf1) {
  	      			         System.out.print( "movie " + (i) + ": " + word + "," + tf);	    
  	      			         System.out.println( ", movie " + (k) + ": " + word + "," + tf1);
  	      			        }
  	      			    }
	  	      		}
				
  	      		 
  	      		} //end of inner for
  	      		 
			} //end of while
		}
			
			 System.out.println("----------------------------------------------");
	}
 }//end of fucntion
	
/***********************************************************************************************************/
	
	/**
	 *  Calculate TF * IDF weights and put (mid, hashmap(word, weight))
	 */
	
	public void calculateIDF()
	{
		int inc =0;
		int df	=1;				//to avoid divide by zero
		double tf  =0;
		String word="";
		double idf =0.0;
		Set set;
	    int size =0;
	    
	    Map <String,Double> mapTF    = null;
	    Map <String,Double> tempMap  = null;
	    Map <String,Double>  mapTFIDF_K = new HashMap<String, Double>();
	    Map <String,Double>  mapTFIDF_T = new HashMap<String, Double>();
	    Map <String,Double>  mapTFIDF_A = new HashMap<String, Double>();
	    
        for(int i=1; i<=totalMoviesML;i++)
        {
        	  if(movieToKeywords.containsKey(i))	        		
	        	{
        		  idf   = 0;  //It will store idf of each word
    	      	  tf	= 0;	    	      	  
    	      	  word  = "";
    	      	  	  
    	      	  	  mapTF = (Map<String, Double>) movieToKeywords.get(i);    	      	  
    	      	  	  mapTFIDF_K = (Map<String, Double>) movieToKeywords.get(i);
    	      	  	
	    	      	  set = mapTF.entrySet();	    	      	       	  
	    	      	  Iterator j = set.iterator();
	    	      	  	    	      	  
	    	      	while(j.hasNext()) 
	    	      	{
	    	      	     Map.Entry words = (Map.Entry)j.next();	    	      	  
	    	      		 
	    	      	     word = (String)words.getKey();			//get the word
	    	      		 tf   = (Double)words.getValue();		//get the tf value
	    	      		 df = 1;
	    	      		  
		    	      	  for(int k=1; k<=totalMoviesML;k++) //check in each movie's list
		    	      	  {
			    	      		if (k!=i ) // not the same movie we are dealing with
			    	      			tempMap =  (Map<String, Double>) movieToKeywords.get(k);
			    	      		
			    	      		if(tempMap!=null && tempMap.containsKey(word)) //if this movie has words 
			    	      		{
			    	      			df += 1;		//This doc contains the word
			    	      		}
			    	      		    	      		  
		    	      	  } //end of all movies
		    	      	  
		    	      	  		    	      	  
		    	      	  //------------------
		    	      	  //IDF = lod (N/df)
		    	      	  //------------------
		    	      	  
		    	      	  idf = Math.log10(totalMoviesML/df*1.0);
		    	      	 
		    	      	  //--------------------
		    	      	  //Put TF * IDF weights
		    	      	  //--------------------
		    	      	 
		    	      	 mapTFIDF_K.put(word, tf * idf); 		    	      	  
		    	      	  
	    	      	  } //end of checking all words in a movie
	    	      	  
	    	      	  movieToKeywordsFinal.put(i, mapTFIDF_K);
	    	      //	System.out.println("size keys=" + mapTFIDF.size());
	        	} //end of if       	

        //---------------------------------------------------------------------------
        //Tags	  	
        
        	  
        	  
        	  if(movieToTags.containsKey(i))	        		
        	   	{
        		  idf   = 0;  //It will store idf of each word
            	  tf	= 0;	    	      	  
            	  word  = "";
            	      	  	  
            	   mapTF = (Map<String, Double>) movieToTags.get(i);
            	   mapTFIDF_T = (Map<String, Double>) movieToTags.get(i);
        	       
            	   set = mapTF.entrySet();	    	      	       	  
        	       Iterator j = set.iterator();
        	    	      	  	    	      	  
        	    	 while(j.hasNext()) 
        	    	{
        	    	    Map.Entry words = (Map.Entry)j.next();	    	      	  
        	    	      		 
        	    	       word = (String)words.getKey();			//get the word
        	    	       tf   = (Double)words.getValue();		//get the tf value
        	    	       df   = 1;
        	    	      		  
        		    	      	  for(int k=1; k<totalMoviesML;k++) //check in each movie's list
        		    	      	  {
        			    	      		if (k!=i) // not the same movie we are dealing with
        			    	      			tempMap =  (Map<String, Double>) movieToTags.get(k);
        			    	      		
        			    	      		if(tempMap!=null && tempMap.containsKey(word)) //if this movie has words 
        			    	      		{
        			    	      			df += 1;		//This doc contains the word
        			    	      		}
        			    	      		    	      		  
        		    	      	  } //end of all movies
        		    	      	  
        		    	      	  //Here add this DF into a list etc
        		    	      	  
        		    	      	  //------------------
        		    	      	  //IDF = lod (N/df)
        		    	      	  //------------------
        		    	      	  
        		    	      	  idf = Math.log10(totalMoviesML/df*1.0);
        		    	      	 
        		    	      	  //--------------------
        		    	      	  //Put TF * IDF weights
        		    	      	  //--------------------
        		    	      	 
        		    	      	 mapTFIDF_T.put(word, tf * idf); 		    	      	  
        		    	      	  
        	    	      	  } //end of checking all words in a movie
        	    	      	  
        	    	      	  movieToTagsFinal.put(i, mapTFIDF_T);
        	    	      	//System.out.println("size tags=" + mapTFIDF.size());
        	    	      	  
        	        	} //end of if       	
                	  
              // ---------------------------------------------------------------------
        	  // All features

                      	         	  
        	  if(movieToFeatures.containsKey(i))	        		
        	   	{
        		  idf   = 0;  //It will store idf of each word
            	  tf	= 0;	    	      	  
            	  word  = "";
            	      	  	  
            	   mapTF      = (Map<String, Double>) movieToFeatures.get(i);
            	   mapTFIDF_A = (Map<String, Double>) movieToFeatures.get(i);    
            	   
        	       set        = mapTF.entrySet();	    	      	       	  
        	       Iterator j = set.iterator();
        	    	      	  	    	      	  
        	    	 while(j.hasNext()) 
        	    	 {
        	    	    Map.Entry words = (Map.Entry)j.next();	    	      	  
        	    	      		 
        	    	       word = (String)words.getKey();			//get the word
        	    	       tf   = mapTF.get(word);					//get the tf value
        	    	       df   = 1;
        	    	      		  
        		         	 for(int k=1; k<totalMoviesML;k++) //check in each movie's list
        		    	   	  {
        			    	     if (k!=i && movieToFeatures.containsKey(k)) // not the same movie we are dealing with
        			    	    	{
        			    	   		   tempMap =  (Map<String, Double>) movieToFeatures.get(k);
        			    	     	        			    	      		
	        			    	      	if(tempMap!=null && tempMap.containsKey(word)) //if this movie has words 
	        			    	      	{
	        			    	      		df += 1;		//This doc contains the word
	        			    	      	}
        			    	      		 
        			    	    	} //end of if 
        		    	       } //end of all movies
        		    	      	  
        		    	      	          		    	      	  
        		    	      	  //------------------
        		    	      	  //IDF = lod (N/df)
        		    	      	  //------------------
        		    	      	  
        		    	      	  idf = Math.log(totalMoviesML *1.0/df);
        		    	      	  
        		    	      	  /*  System.out.println(" i ="+ i + "words="+ mapTF.size());
        		    	      	      System.out.println("tf, df, idf feature ="+ tf+ "," + df + ", " + idf);
        		    	      	      System.out.println("log(1)" + Math.log(1));
        		    	      	  */
        		    	      	//  if(idf >0)  System.out.println("idf = " + idf);
        		    	      	
        		    	      	  //--------------------
        		    	      	  //Put TF * IDF weights
        		    	      	  //--------------------
        		    	      	 
        		    	      	  
        		    	      	      mapTFIDF_A.put(word, tf * idf); 		    	      	  
        		    	      	//    mapTFIDF_A.put(word, tf * 1.0 );
        		    	      	  
        	    	      	  } //end of checking all words in a movie
        	    	      	  
        	    	 //-------------------------------------
        	    	 // Write movies Final
        	    	 //--------------------------------------
        	    	    
        	    	 movieToFeaturesFinal.put(i, mapTFIDF_A);      	    	 
        	    	 
        	    	 
        	    	 //System.out.println("size features=" + mapTFIDF.size());
        	    	      	  
        	        	} //end of if  	  
        }//end of all movies		
        
       
        
} //end of function

    
/**********************************************************************************************/
	/**
	 *  Verify the weights
	 *  
	 */        

	public void verifyWeights()
	{
		  Map<String, Double> temp1;
		  Map<String, Double> temp2;
		  
        for (int i=1;i<totalMoviesML; i++)
        {
        	
        if(movieToFeaturesFinal.containsKey(i))
        {
          temp1 = ( Map<String, Double>) movieToFeaturesFinal.get(i);	
          
    	    	
        	  Set setActive = temp1.entrySet();      	  
           	  Iterator jActive = setActive.iterator();           	  
           	  
           	while(jActive.hasNext()) 
         	 {
         	     Map.Entry words = (Map.Entry)jActive.next();  		 
         	     String word = (String)words.getKey();			//get the word

         	    for(int k=1; k<totalMoviesML;k++) //check in each movie's list
         	  	  {

         		
	        	  if(k!=i && movieToFeaturesFinal.containsKey(k))
	        	  {
		        	  temp2 = ( Map<String, Double>) movieToFeaturesFinal.get(k);
		        	  
		         	    if(temp2.containsKey(word))
		         	    {
		         	    	 double w1= temp1.get(word);
		        			 double w2= temp2.get(word);
		        			 if (w1!=w2) System.out.println(w1 + "," +w2);
		         	    }		         	
	        	   }
      		    	
         	    } //end while
      	    } //end of all movies
      		    	      	  
      	
      	  } //end of if
        } //end of outer for
     }
        
/************************************************************************************************/
	
	
}
