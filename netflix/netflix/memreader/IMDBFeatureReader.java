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


public class IMDBFeatureReader 
{
  
    
		//class variables
		private	  int 					totalMovies;
		private	  int 					totalMoviesML;
		private	  int 					totalMoviesFT;
		protected Connection 			conML, conIMDB, conFT;
		protected String 				dbNameML, dbNameIMDB, dbNameFT;
		protected String 				ratingsNameML, ratingsNameIMDB, ratingsNameFT;
		protected String 				imdbKeywords, 
										imdbPlots,
										imdbTags, 
										imdbActors, 
										imdbDirectors, 
										imdbGenres, 
										imdbProducers, 
										imdbCertificate,
										imdbEditors,
										imdbWriters, 
										imdbReasonText,
										imdbProdCompanies,
										imdbRatings,
										imdbAkaTitles;
		
		// TFIDF vs TF
		private boolean 				isIDF;
		
		//types to be extracted 
		 IntArrayList typesToExtract;

		 // hashMaps
	    public OpenIntObjectHashMap 	movieToKeywords;  //movies, and then a list of keywords (Strings): 10    
	    public OpenIntObjectHashMap 	movieToTags; 	  //movies, and then a list of keywords (Strings): 9
	    public OpenIntObjectHashMap 	movieToCasts;     //movies, and then a list of keywords (Strings): 10    
	    public OpenIntObjectHashMap 	movieToPlots; 	  //movies, and then a list of keywords (Strings): 9
	    public OpenIntObjectHashMap 	movieToCertificates;
	    public OpenIntObjectHashMap 	movieToBiography;
	    public OpenIntObjectHashMap 	movieToPrintedReviews;
	    public OpenIntObjectHashMap 	movieToVotes;
	    public OpenIntObjectHashMap 	movieToRatings;
	    public OpenIntObjectHashMap 	movieToColors;
	    public OpenIntObjectHashMap 	movieToLanguages;
	    public OpenIntObjectHashMap 	movieToFeatures; 	  //movies, and then a list of keywords (Strings): 9	   
	    public OpenIntObjectHashMap 	movieToDirectors;       
	    public OpenIntObjectHashMap 	movieToProducers;       
	    public OpenIntObjectHashMap 	movieToActors;  
	    public OpenIntObjectHashMap 	movieToGenres;
	    public OpenIntObjectHashMap 	movieToEditors;   
	    public OpenIntObjectHashMap 	movieToWriters;
	    public OpenIntObjectHashMap 	movieToAkaTitles;
	    public OpenIntObjectHashMap 	movieToReasonText;
	    public OpenIntObjectHashMap 	movieToProdCompanies;
	    
	    
	    public OpenIntObjectHashMap 	movieToKeywordsFinal; //movies, and then a list of keywords (Strings): 10    
	    public OpenIntObjectHashMap 	movieToTagsFinal; 	  //movies, and then a list of keywords (Strings): 9
	    public OpenIntObjectHashMap 	movieToCastsFinal;    //movies, and then a list of keywords (Strings): 10    
	    public OpenIntObjectHashMap 	movieToPlotsFinal; 	
	    public OpenIntObjectHashMap 	movieToCertificatesFinal;
	    public OpenIntObjectHashMap 	movieToBiographyFinal;
	    public OpenIntObjectHashMap 	movieToPrintedReviewsFinal;
	    public OpenIntObjectHashMap 	movieToVotesFinal;
	    public OpenIntObjectHashMap 	movieToRatingsFinal;
	    public OpenIntObjectHashMap 	movieToColorsFinal;
	    public OpenIntObjectHashMap 	movieToLanguagesFinal;
	    public OpenIntObjectHashMap 	movieToFeaturesFinal; 	  //movies, and then a list of keywords (Strings): 9
	    public OpenIntObjectHashMap 	movieToDirectorsFinal;       
	    public OpenIntObjectHashMap 	movieToProducersFinal;       
	    public OpenIntObjectHashMap 	movieToActorsFinal;         
	    public OpenIntObjectHashMap 	movieToGenresFinal;   	
	    public OpenIntObjectHashMap 	movieToEditorsFinal;  
	    public OpenIntObjectHashMap 	movieToWritersFinal;
	    public OpenIntObjectHashMap 	movieToAkaTitlesFinal;
	    public OpenIntObjectHashMap 	movieToReasonTextFinal;	   
	    public OpenIntObjectHashMap 	movieToProdCompaniesFinal;
	    public OpenIntObjectHashMap 	movieToAllUnStemmedFeaturesFinal;	//Just to make sure, my data is god or not
	    
	    public OpenIntObjectHashMap 	dumMovieToKeywords;       // 10     
	    public OpenIntObjectHashMap 	dumMovieToTags;			  // 9	
	    public OpenIntObjectHashMap 	dumMovieToCasts; 		  // 
	    public OpenIntObjectHashMap 	dumMovieToPlots; 		  // 98
	    public OpenIntObjectHashMap 	dumMovieToCertificates;   // 5 
	    public OpenIntObjectHashMap 	dumMovieToBiography;      // 19
	    public OpenIntObjectHashMap 	dumMovieToPrintedReviews; // 94	    
	    public OpenIntObjectHashMap 	dumMovieToColors;         // 2
	    public OpenIntObjectHashMap 	dumMovieToLanguages;      // 4
	    public OpenIntObjectHashMap 	dumMovieToActors;         // 15
	    public OpenIntObjectHashMap 	dumMovieToDirectors;      // 16
	    public OpenIntObjectHashMap 	dumMovieToProducers;      // 17
	    public OpenIntObjectHashMap 	dumMovieToGenres;   	  // 18
	    public OpenIntObjectHashMap 	dumMovieToEditors;		   //19
	    public OpenIntObjectHashMap 	dumMovieToWriters;		   //20
	    public OpenIntObjectHashMap 	dumMovieToReasonText;	   //21
	    public OpenIntObjectHashMap 	dumMovieToProdCompanies;	//22
	    public OpenIntObjectHashMap 	dumMovieToVotes;            // 23
	    public OpenIntObjectHashMap 	dumMovieToRatings;          // 23
	    public OpenIntObjectHashMap 	dumMovieToAkaTitles;  		 //24
	    
	    
	    private String 		 destFile;       					    // where we wanna write our dest file
		private IntArrayList typeToExtract;
		IntArrayList     	 moviesWithOutAMatch;					// For movies, not matching exactly
	    
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
	        "s", "t", "u", "v", "w", "x", "y", "z",
	        "a","associates","able","about","above","according","accordingly","across","actually","after",
	        "afterwards","again","against","all","allow","allows","almost","alone","along","already",
	        "also","although","always","am","among","amongst","an","and","another","any",
	        "anybody","anyhow","anyone","anything","anyway","anyways","anywhere","apart","appear","appreciate",
	        "appropriate","are","around","as","aside","ask","asking","associated","at","available",
	        "away","awfully","b","be","became","because","become","becomes","becoming","been",
	        "before","beforehand","behind","being","believe","below","beside","besides","best","better",
	        "between","beyond","both","brief","but","by","c","came","can","cannot",
	        "cant","cause","causes","certain","certainly","changes","clearly","com","come","comes",
	        "concerning","consequently","consider","considering","contain","containing","contains","corresponding","could","course",
	        "currently","d","definitely","described","despite","did","different","do","does","doing",
	        "done","down","downwards","during","e","each","edu","eg","eight","either",
	        "else","elsewhere","enough","entirely","especially","et","etc","even","ever","every",
	        "everybody","everyone","everything","everywhere","ex","exactly","example","except","f","far",
	        "few","fifth","first","five","followed","following","follows","for","former","formerly",
	        "forth","four","from","further","furthermore","g","get","gets","getting","given",
	        "gives","go","goes","going","gone","got","gotten","greetings","h","had",
	        "happens","hardly","has","have","having","he","hello","help","hence","her",
	        "here","hereafter","hereby","herein","hereupon","hers","herself","hi","him","himself",
	        "his","hither","hopefully","how","howbeit","however","i","ie","if","ignored",
	        "immediate","in","inasmuch","inc","indeed","indicate","indicated","indicates","inner","insofar",
	        "instead","into","inward","is","it","its","itself","j","just","k",
	        "keep","keeps","kept","know","knows","known","l","last","lately","later",
	        "latter","latterly","least","less","lest","let","like","liked","likely","little",
	        "look","looking","looks","ltd","m","mainly","many","may","maybe","me",
	        "mean","meanwhile","merely","might","more","moreover","most","mostly","much","must",
	        "my","myself","n","name","namely","nd","near","nearly","necessary","need",
	        "needs","neither","never","nevertheless","new","next","nine","no","nobody","non",
	        "none","noone","nor","normally","not","nothing","novel","now","nowhere","o",
	        "obviously","of","off","often","oh","ok","okay","old","on","once",
	        "one","ones","only","onto","or","other","others","otherwise","ought","our",
	        "ours","ourselves","out","outside","over","overall","own","p","particular","particularly",
	        "per","perhaps","placed","please","plus","possible","presumably","probably","provides","q",
	        "que","quite","qv","r","rather","rd","re","really","reasonably","regarding",
	        "regardless","regards","relatively","respectively","right","s","said","same","saw","say",
	        "saying","says","second","secondly","see","seeing","seem","seemed","seeming","seems",
	        "seen","self","selves","sensible","sent","serious","seriously","seven","several","shall",
	        "she","should","since","six","so","some","somebody","somehow","someone","something",
	        "sometime","sometimes","somewhat","somewhere","soon","sorry","specified","specify","specifying","still",
	        "sub","such","sup","sure","t","take","taken","tell","tends","th",
	        "than","thank","thanks","thanx","that","thats","the","their","theirs","them",
	        "themselves","then","thence","there","thereafter","thereby","therefore","therein","theres","thereupon",
	        "these","they","think","third","this","thorough","thoroughly","those","though","three",
	        "through","throughout","thru","thus","to","together","too","took","toward","towards",
	        "tried","tries","truly","try","trying","twice","two","u","un","under",
	        "unfortunately","unless","unlikely","until","unto","up","upon","us","use","used",
	        "useful","uses","using","usually","uucp","v","value","various","very","via",
	        "viz","vs","w","want","wants","was","way","we","welcome","well",
	        "went","were","what","whatever","when","whence","whenever","where","whereafter","whereas",
	        "whereby","wherein","whereupon","wherever","whether","which","while","whither","who","whoever",
	        "whole","whom","whose","why","will","willing","wish","with","within","without",
	        "wonder","would","would","x","y","yes","yet","you","your","yours",
	        "yourself","yourselves","z","zero","nbsp","http","www","writeln","pdf","html",
	        "endobj","obj","aacute","eacute","iacute","oacute","uacute","agrave","egrave","igrave",
	        "ograve","ugrave",
	    };
	    
	    
/********************************************************************************************************/
	    
		/**
		 * Default constructor.
		 * 
		 * Sets up a connection to the database "recommender", using
		 * the table name "ratings" for ratings and "movies" for movies.
		 */
	    		
	    public IMDBFeatureReader()		
		{
	    	
	    	  isIDF				= false;	    	
		      totalMoviesML 	= 1682;
		      totalMoviesFT 	= 1930;
			
		     // DataBases
		    dbNameML 		= "jmdb";
			dbNameIMDB 		= "jmdb";					// jmdb data base
			dbNameFT 		= "filmtrust";
			
			//Table Names
			ratingsNameML   = "usr_ml_items";			// It has mapping from MovieLens to IMDB
			ratingsNameIMDB = "movie_info";
			ratingsNameFT 	= "ratings";
		
			imdbKeywords    =	"keywords"; 
		    imdbPlots		=   "plots";
		    imdbTags		=   "taglines";
		    imdbActors		=   "movies2actors";		// table names in the jmdb
		    imdbDirectors	=   "movies2directors";
		    imdbProducers	=   "movies2producers";
		    imdbEditors     =   "movies2editors";
		    imdbGenres		=   "genres";
		    imdbCertificate =   "certificates";
		    imdbWriters		=   "movies2writers";
		    imdbReasonText  =   "mpaaratings";			// e.g. sexually emotional etc.
		    imdbProdCompanies = "prodCompanies";
		    imdbRatings		  = "ratings";
		    imdbAkaTitles	  = "akatitles";
		    
			//Type
			typeToExtract = new IntArrayList();
			typeToExtract.add(9);
			typeToExtract.add(10);
			typeToExtract.add(98);
			typeToExtract.add(15);
			typeToExtract.add(16);
			typeToExtract.add(17);
			typeToExtract.add(18);			
			typeToExtract.add(4);
			typeToExtract.add(2);
			typeToExtract.add(100);
			typeToExtract.add(101);
			typeToExtract.add(94);
			typeToExtract.add(19);
			typeToExtract.add(5);
			typeToExtract.add(20);
			typeToExtract.add(21);
			typeToExtract.add(22);
			typeToExtract.add(23);
		
			
			//Maps
			 movieToKeywords 			= new OpenIntObjectHashMap();
			 movieToTags	 			= new OpenIntObjectHashMap();
			 movieToFeatures 			= new OpenIntObjectHashMap();		
			 movieToPlots 				= new OpenIntObjectHashMap();
			 movieToCertificates	 	= new OpenIntObjectHashMap();
			 movieToBiography 			= new OpenIntObjectHashMap();
			 movieToPrintedReviews		= new OpenIntObjectHashMap();			 
			 movieToVotes	 			= new OpenIntObjectHashMap();
			 movieToRatings 			= new OpenIntObjectHashMap();
			 movieToColors 				= new OpenIntObjectHashMap();
			 movieToLanguages	 		= new OpenIntObjectHashMap();
			 movieToDirectors      		= new OpenIntObjectHashMap();					
			 movieToProducers 	    	= new OpenIntObjectHashMap(); 					
			 movieToActors	        	= new OpenIntObjectHashMap();
			 movieToGenres	        	= new OpenIntObjectHashMap();				 
			 movieToEditors       	= new OpenIntObjectHashMap();;   
			 movieToWriters       	= new OpenIntObjectHashMap();;
			 movieToAkaTitles       = new OpenIntObjectHashMap();;
			 movieToReasonText      = new OpenIntObjectHashMap();;
			 movieToProdCompanies   = new OpenIntObjectHashMap();;
			    
			    
			 movieToKeywordsFinal			= new OpenIntObjectHashMap();
			 movieToTagsFinal	 			= new OpenIntObjectHashMap();
			 movieToFeaturesFinal			= new OpenIntObjectHashMap();
			 movieToPlotsFinal 				= new OpenIntObjectHashMap();
			 movieToCertificatesFinal	 	= new OpenIntObjectHashMap();
			 movieToBiographyFinal 			= new OpenIntObjectHashMap();
			 movieToPrintedReviewsFinal		= new OpenIntObjectHashMap();			 
			 movieToVotesFinal	 			= new OpenIntObjectHashMap();
			 movieToRatingsFinal 			= new OpenIntObjectHashMap();
			 movieToColorsFinal 			= new OpenIntObjectHashMap();
			 movieToLanguagesFinal	 		= new OpenIntObjectHashMap();			 
			 movieToDirectorsFinal     		= new OpenIntObjectHashMap();					
			 movieToProducersFinal 	    	= new OpenIntObjectHashMap(); 					
			 movieToActorsFinal	        	= new OpenIntObjectHashMap();		
			 movieToGenresFinal        		= new OpenIntObjectHashMap();			 
			 movieToEditorsFinal       		= new OpenIntObjectHashMap();  
			 movieToWritersFinal       		= new OpenIntObjectHashMap();
			 movieToAkaTitlesFinal      	 = new OpenIntObjectHashMap();
			 movieToReasonTextFinal     	 = new OpenIntObjectHashMap();
			 movieToProdCompaniesFinal  	 = new OpenIntObjectHashMap();
			 movieToAllUnStemmedFeaturesFinal =  new OpenIntObjectHashMap();
			 
			  dumMovieToTags				= new OpenIntObjectHashMap();			 
			  dumMovieToKeywords 			= new OpenIntObjectHashMap();
			  dumMovieToPlots 				= new OpenIntObjectHashMap();
			  dumMovieToCertificates	 	= new OpenIntObjectHashMap();
			  dumMovieToBiography 			= new OpenIntObjectHashMap();
			  dumMovieToPrintedReviews		= new OpenIntObjectHashMap();			 
			  dumMovieToVotes	 			= new OpenIntObjectHashMap();
			  dumMovieToRatings 			= new OpenIntObjectHashMap();
			  dumMovieToColors 				= new OpenIntObjectHashMap();
			  dumMovieToLanguages	 		= new OpenIntObjectHashMap();
			  dumMovieToDirectors	       	= new OpenIntObjectHashMap();					
			  dumMovieToProducers 		    = new OpenIntObjectHashMap(); 					
			  dumMovieToActors	        	= new OpenIntObjectHashMap();		
			  dumMovieToGenres        		= new OpenIntObjectHashMap();
			  dumMovieToEditors       	= new OpenIntObjectHashMap();  
			  dumMovieToWriters       	= new OpenIntObjectHashMap();
			  dumMovieToAkaTitles       = new OpenIntObjectHashMap();
			  dumMovieToReasonText      = new OpenIntObjectHashMap();
			  dumMovieToProdCompanies   = new OpenIntObjectHashMap();
			 
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
				
				if(name.equals("jmdb"))
				{
					conML = DriverManager.getConnection("jdbc:mysql://" +
					"localhost:3306/" + name, "root", "ali5");			
					success = true;
				}
				
				else
				{
					conIMDB = DriverManager.getConnection("jdbc:mysql://" +
					"localhost:3306/" + name, "root", "ali5");			
					success = true;
				}
				

				if(name.equals("ft"))
				{
					conFT = DriverManager.getConnection("jdbc:mysql://" +
					"localhost:3306/" + name, "root", "ali5");			
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

		//-------------------------------------------
		
		public Connection getConnectionIMDB()		
		{
			return conML;
		}
		
    	//-------------------------------------------
		
		public Connection getConnectionFT()		
		{
			return conML;
		}
		


/******************************************************************************************************/
	    
	    /**
	     *  Get information, e.g. keywords, tags from different tables from JMDB database
	     *  @param imdbId, MlId
	     *  @return void
	     */
	    
	// We have different tables for different information, e.g. keywords etc
	// So from here, we call the same function "addFeaturesToMovie" with self defined types 
	// .... It is easy to make the type as the same, e.g. tags = 9, keywords = 10;    
	    
	    
		public void getInfo(int imdbId, int mlId)
		{
			int type 	= 0;
			String info = "";
			
		  try
			{
			    // Get keywords from the table "keywords" .... type = 10
			    type= 10;
				Statement stmt = conML.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT keyword FROM " + imdbKeywords + " " 
											      + "WHERE movieid= " + imdbId + ";");							
				while (rs.next())					
				{		
										   
					  info = rs.getString(1);					  					  					   			   
					  if(typeToExtract.contains(type))
						      addFeaturesToMovies(type, info, mlId);
				}
								
				// Get tags from the table "tagsline" .... type = 9
				type= 9;
				stmt = conML.createStatement();
				rs = stmt.executeQuery("SELECT taglinetext FROM " + imdbTags + " " 
											      + "WHERE movieid= " + imdbId + ";");							
				while (rs.next())					
				{						   				   
					  info = rs.getString(1);					  					  					   			   
					  if(typeToExtract.contains(type))
						      addFeaturesToMovies(type, info, mlId);
				}
				
				// Get Plots from the table "Plots" .... type = 98
				type= 98;
				stmt = conML.createStatement();
				rs = stmt.executeQuery("SELECT plottext FROM " + imdbPlots + " " 
											      + "WHERE movieid= " + imdbId + ";");							
				while (rs.next())					
				{						   				   
					  info = rs.getString(1);					  					  					   			   
					  if(typeToExtract.contains(type))
						      addFeaturesToMovies(type, info, mlId);
				}
				
				// Get Actors from the table "actor" .... type = 15
				type= 15;
				int total = 0;
				stmt = conML.createStatement();
				rs = stmt.executeQuery("SELECT actorid FROM " + imdbActors + " " 
											      + "WHERE movieid= " + imdbId + ";");							
				while (rs.next() && total++<=5)											//Picking top 3 actors			
				{						   				   
					  info = rs.getString(1);					  					  					   			   
					  if(typeToExtract.contains(type))
						      addFeaturesToMovies(type, info, mlId);
				}
				
				
				// Get Directors from the table "Directors" .... type = 16
				type= 16;
				total = 0;
				stmt = conML.createStatement();
				rs = stmt.executeQuery("SELECT directorid FROM " + imdbDirectors + " " 
											      + "WHERE movieid= " + imdbId + ";");							
				while (rs.next() && total++<=5)					
				{						   				   
					  info = rs.getString(1);					  					  					   			   
					  if(typeToExtract.contains(type))
						      addFeaturesToMovies(type, info, mlId);
				}
				
				
				// Get Producers from the table "Producers" .... type = 17
				type= 17;
				total = 0;
				stmt = conML.createStatement();
				rs = stmt.executeQuery("SELECT producerid FROM " + imdbProducers + " " 
											      + "WHERE movieid= " + imdbId + ";");							
				while (rs.next() && total++<=5)					
				{						   				   
					  info = rs.getString(1);					  					  					   			   
					  if(typeToExtract.contains(type))
						      addFeaturesToMovies(type, info, mlId);
				}
				
				
				// Get Genres from the table "Genres" .... type = 18
				type = 18;				
				stmt = conML.createStatement();
				rs 	 = stmt.executeQuery("SELECT genre FROM " + imdbGenres + " " 
											      + "WHERE movieid= " + imdbId + ";");							
				while (rs.next())					
				{						   				   
					  info = rs.getString(1);					  					  					   			   
					  if(typeToExtract.contains(type))
						      addFeaturesToMovies(type, info, mlId);
				}
				
				// Get Genres from the table "Cerificates" .... type = 5
				type = 5;				
				stmt = conML.createStatement();
				rs 	 = stmt.executeQuery("SELECT certification FROM " + imdbCertificate + " " 
											      + "WHERE movieid= " + imdbId + ";");							
				while (rs.next())					
				{						   				   
					  info = rs.getString(1);					  					  					   			   
					  if(typeToExtract.contains(type))
						      addFeaturesToMovies(type, info, mlId);
				}			
				
				//Get Country of certificate
				stmt = conML.createStatement();
				rs 	 = stmt.executeQuery("SELECT country FROM " + imdbCertificate + " " 
											      + "WHERE movieid= " + imdbId + ";");							
				while (rs.next())					
				{						   				   
					  info = rs.getString(1);					  					  					   			   
					  if(typeToExtract.contains(type))
						      addFeaturesToMovies(type, info, mlId);
				}
				
				
				// Get editor from the table "Editors" .... type = 19
				type = 19;				
				stmt = conML.createStatement();
				rs 	 = stmt.executeQuery("SELECT editorid FROM " + imdbEditors + " " 
											      + "WHERE movieid= " + imdbId + ";");							
				while (rs.next())					
				{						   				   
					  info = rs.getString(1);					  					  					   			   
					  if(typeToExtract.contains(type))
						      addFeaturesToMovies(type, info, mlId);
				}			
				
				
				
				// Get writers from the table "writers" .... type = 20
				type = 20;				
				stmt = conML.createStatement();
				rs 	 = stmt.executeQuery("SELECT writerid FROM " + imdbWriters + " " 
											      + "WHERE movieid= " + imdbId + ";");							
				while (rs.next())					
				{						   				   
					  info = rs.getString(1);					  					  					   			   
					  if(typeToExtract.contains(type))
						      addFeaturesToMovies(type, info, mlId);
				}			
				
			
				// Get reasontext from the table "paaratings" .... type = 21
				type = 21;				
				stmt = conML.createStatement();
				rs 	 = stmt.executeQuery("SELECT reasontext FROM " + imdbReasonText + " " 
											      + "WHERE movieid= " + imdbId + ";");							
				while (rs.next())					
				{						   				   
					  info = rs.getString(1);					  					  					   			   
					  if(typeToExtract.contains(type))
						      addFeaturesToMovies(type, info, mlId);
				}			
				
				
				// Get name from the table "prodCompaies" .... type = 22
				type = 22;				
				stmt = conML.createStatement();
				rs 	 = stmt.executeQuery("SELECT name FROM " + imdbProdCompanies + " " 
											      + "WHERE movieid= " + imdbId + ";");							
				while (rs.next())					
				{						   				   
					  info = rs.getString(1);					  					  					   			   
					  if(typeToExtract.contains(type))
						      addFeaturesToMovies(type, info, mlId);
				}			
				
				// Get ratings/rank from the table "rating" .... type = 23
				type = 23;				
				stmt = conML.createStatement();
				rs 	 = stmt.executeQuery("SELECT rank FROM " + imdbRatings + " " 
											      + "WHERE movieid= " + imdbId + ";");							
				while (rs.next())					
				{						   				   
					  info = rs.getString(1);	
					  
					  //Just a dummy scheme to make sure movies are famous or not
					 if(info!=null || info.equalsIgnoreCase("") || (info.isEmpty() == false))
					  {
						  double temp = Double.parseDouble(info);
						
						  if(temp>=8)
							  info = "8.0";
						  else if (temp>=6 && temp <8)
							  info = "6.0";
						  else if (temp>=4 && temp <6)
							  info = "4.0";						   
						  else if (temp>=2 && temp <4)
							  info = "2.0";						   
						  else if (temp <2)
							  info = "0.0";
						   						  
						  if(typeToExtract.contains(type))
						      addFeaturesToMovies(type, info, mlId);
					  }				  					  
					  
				}			
				
				//Get Votes
				stmt = conML.createStatement();
				rs 	 = stmt.executeQuery("SELECT votes FROM " + imdbRatings + " " 
											      + "WHERE movieid= " + imdbId + ";");							
				while (rs.next())					
				{						   				   
					  info = rs.getString(1);	
					  
					  //Just a dummy scheme to make sure movies are famous or not
					 if(info!=null || info.equalsIgnoreCase("") || (info.isEmpty() == false))
					  {
						  double temp = Double.parseDouble(info);
						  
						  if(temp<=10)
							  info = "10.0";
						  else if (temp > 10 && temp<=20)
							  info = "20.0";
						  else if (temp>20 && temp <=30)
							  info = "30.0";
						  else if (temp>30 && temp <=40)
							  info = "40.0";
						  else if (temp>40 && temp <=50)
							  info = "50.0";
						  else if (temp>50 && temp <=100)
							  info = "100.0";						   
						  else 
							  info = "200.0";
						   						  
						  if(typeToExtract.contains(type))
						      addFeaturesToMovies(type, info, mlId);
					  }				  					  
					  
				}			
				
				
				// Get title from the table "imdbAkaTiles" .... type = 24
				type = 24;				
				stmt = conML.createStatement();
				rs 	 = stmt.executeQuery("SELECT title FROM " + imdbAkaTitles + " " 
											      + "WHERE movieid= " + imdbId + ";");							
				while (rs.next())					
				{						   				   
					  info = rs.getString(1);					  					  					   			   
					  if(typeToExtract.contains(type))
						      addFeaturesToMovies(type, info, mlId);
				}			
				
				
				
				
				//---------------------------
				//close the statement
				stmt.close();
				
			}
			
			catch(SQLException e){ e.printStackTrace(); } 
		}
	
		
/******************************************************************************************************/

				/**
			     * Adds an entry to the movieToKeywords, movieToTags etc. hashtable. 
			     *    
			     *    If a movie has no keywords (for example), then simply we do not put anything.
			     *    When we have to retrieve movie's keywords, check it is not null first
			     *    
			     *    @param type = 9, 10 etc
			     *    @param Info = keywords, or tags (string form, single)
			     *    @param mid  = ML mid
			     */
		   
		// It will build a list against each movie and will store info in that list
		// List will be added to Map with mid
		
			    public void addFeaturesToMovies(int type, String info, int mid)    
			    {
			    	
			        ObjectArrayList list;
			        
			      //----------------------------------------
			        if(type==10) //keywords
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
			       
			        if(type==9) //tags
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

			      //----------------------------------------
				       
			        if(type==98) //Plots
			        {
			        	if(	dumMovieToPlots.containsKey(mid))	        		
			        	{
			        	  list = (ObjectArrayList) dumMovieToPlots.get(mid);	        	
			        	
			        	}
			        	
			        	else
			        	{
			        	 list = new ObjectArrayList();	
			        	}
			        	
			        	list.add(info);
			        	dumMovieToPlots.put(mid, list);
			        }
			
			      //----------------------------------------
				       
			        if(type==5) //Certificates
			        {
			        	if(	dumMovieToCertificates.containsKey(mid))	        		
			        	{
			        	  list = (ObjectArrayList) dumMovieToCertificates.get(mid);	        	
			        	
			        	}
			        	
			        	else
			        	{
			        	 list = new ObjectArrayList();	
			        	}
			        	
			        	list.add(info);
			        	dumMovieToCertificates.put(mid, list);
			        }
			      //----------------------------------------
				       
			        if(type==19) //Editors
			        {
			        	if(	dumMovieToEditors.containsKey(mid))	        		
			        	{
			        	  list = (ObjectArrayList) dumMovieToEditors.get(mid);	        	
			        	
			        	}
			        	
			        	else
			        	{
			        	 list = new ObjectArrayList();	
			        	}
			        	
			        	list.add(info);
			        	dumMovieToEditors.put(mid, list);
			        }
			      //----------------------------------------
				       
			        if(type==2) //colors
			        {
			        	if(	dumMovieToColors.containsKey(mid))	        		
			        	{
			        	  list = (ObjectArrayList) dumMovieToColors.get(mid);	        	
			        	
			        	}
			        	
			        	else
			        	{
			        	 list = new ObjectArrayList();	
			        	}
			        	
			        	list.add(info);
			        	dumMovieToColors.put(mid, list);
			        }
			      //----------------------------------------
				       
			        if(type==4) //Language
			        {
			        	if(	dumMovieToLanguages.containsKey(mid))	        		
			        	{
			        	  list = (ObjectArrayList) dumMovieToLanguages.get(mid);	        	
			        	
			        	}
			        	
			        	else
			        	{
			        	  list = new ObjectArrayList();	
			        	}
			        	
			        	list.add(info);
			        	dumMovieToLanguages.put(mid, list);
			        }
			        
			        //----------------------------------------
				       
			        if(type==15) //ACTORS
			        {
			        	if(	dumMovieToActors.containsKey(mid))	        		
			        	{
			        	  list = (ObjectArrayList) dumMovieToActors.get(mid);	        	
			        	
			        	}
			        	
			        	else
			        	{
			        	  list = new ObjectArrayList();	
			        	}
			        	
			        	list.add(info);
			        	dumMovieToActors.put(mid, list);
			        }
			        
			        //----------------------------------------
				       
			        if(type==16) // DIRECTORS
			        {
			        	if(	dumMovieToDirectors.containsKey(mid))	        		
			        	{
			        	  list = (ObjectArrayList) dumMovieToDirectors.get(mid);	        	
			        	
			        	}
			        	
			        	else
			        	{
			        	  list = new ObjectArrayList();	
			        	}
			        	
			        	list.add(info);
			        	dumMovieToDirectors.put(mid, list);
			        }
			        
			        //----------------------------------------
				       
			        if(type==17) // PRODUCERS
			        {
			        	if(	dumMovieToProducers.containsKey(mid))	        		
			        	{
			        	  list = (ObjectArrayList) dumMovieToProducers.get(mid);	        	
			        	
			        	}
			        	
			        	else
			        	{
			        	 list = new ObjectArrayList();	
			        	}
			        	
			        	list.add(info);
			        	dumMovieToProducers.put(mid, list);
			        }
			        
			        //----------------------------------------
				       
			        if(type==18) // GENRES
			        {
			        	if(	dumMovieToGenres.containsKey(mid))	        		
			        	{
			        	  list = (ObjectArrayList) dumMovieToGenres.get(mid);	        	
			        	
			        	}
			        	
			        	else
			        	{
			        	 list = new ObjectArrayList();	
			        	}
			        	
			        	list.add(info);
			        	dumMovieToGenres.put(mid, list);
			        }  
			        
				   //----------------------------------------			        
			        	
			        if(type==20) // Writer
			        {
			        	if(dumMovieToWriters.containsKey(mid))	        		
			        	{
			        	  list = (ObjectArrayList) dumMovieToWriters.get(mid);	        	
			        	
			        	}
			        	
			        	else
			        	{
			        	 list = new ObjectArrayList();	
			        	}
			        	
			        	list.add(info);
			        	dumMovieToWriters.put(mid, list);
			        }
			        
					 //----------------------------------------	
			        
			        if(type==21) // ReasonText
			        {
			        	if(dumMovieToReasonText.containsKey(mid))	        		
			        	{
			        	  list = (ObjectArrayList) dumMovieToReasonText.get(mid);	        	
			        	
			        	}
			        	
			        	else
			        	{
			        	 list = new ObjectArrayList();	
			        	}
			        	
			        	list.add(info);
			        	dumMovieToReasonText.put(mid, list);
			        }
					 //----------------------------------------	
			        
			        if(type==22) // prodCompanies
			        {
			        	if(dumMovieToProdCompanies.containsKey(mid))	        		
			        	{
			        	  list = (ObjectArrayList) dumMovieToProdCompanies.get(mid);	        	
			        	
			        	}
			        	
			        	else
			        	{
			        	 list = new ObjectArrayList();	
			        	}
			        	
			        	list.add(info);
			        	dumMovieToProdCompanies.put(mid, list);
			        }
					 //----------------------------------------			        
				    
			        if(type==23) // Votes/Rank
			        {
			        	if(dumMovieToRatings.containsKey(mid))	        		
			        	{
			        	  list = (ObjectArrayList) dumMovieToRatings.get(mid);	        	
			        	
			        	}
			        	
			        	else
			        	{
			        	 list = new ObjectArrayList();	
			        	}
			        	
			        	list.add(info);
			        	dumMovieToRatings.put(mid, list);
			        }
					 //----------------------------------------			        
				       
			        if(type==24) //akatitles
			        {
			        	if(dumMovieToAkaTitles.containsKey(mid))	        		
			        	{
			        	  list = (ObjectArrayList) dumMovieToAkaTitles.get(mid);	        	
			        	
			        	}
			        	
			        	else
			        	{
			        	 list = new ObjectArrayList();	
			        	}
			        	
			        	list.add(info);
			        	dumMovieToAkaTitles.put(mid, list);
			        }
			        
			       //----------------------------------------			        
			       //----------------------------------------
				       
			         //All 
			       
			        	if(movieToAllUnStemmedFeaturesFinal.containsKey(mid))	        		
			        	{
			        	  list = (ObjectArrayList) movieToAllUnStemmedFeaturesFinal.get(mid);	        	
			        	
			        	}
			        	
			        	else
			        	{
			        	 list = new ObjectArrayList();	
			        	}
			        	
			        	list.add(info);
			        	movieToAllUnStemmedFeaturesFinal.put(mid, list);
			       			        
			    } //end function

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
		    
		    for (int mid=1;mid<=totalMovies;mid++)
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
			    // Tags
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



		    	//-----------------------------------------------------
			    // Plots
			    //-----------------------------------------------------		    	
		    	
		    	if(	dumMovieToPlots.containsKey(mid))	        		
		       	{
		       	  list  		= (ObjectArrayList) dumMovieToPlots.get(mid);	      
		       	  type  		= 98;	       	  
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
		    	} // end of reading type = plots
		    	

		    	//-----------------------------------------------------
			    // Certificatess
			    //-----------------------------------------------------		    	
		    	
		    	if(	dumMovieToCertificates.containsKey(mid))	        		
		       	{
		       	  list  		= (ObjectArrayList) dumMovieToCertificates.get(mid);	      
		       	  type  		= 5;	       	  
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
		    	} // end of reading type = Certificate



		    /*	//-----------------------------------------------------
			    // Color
			    //-----------------------------------------------------		    	
		    	
		    	if(	dumMovieToColors.containsKey(mid))	        		
		       	{
		       	  list  		= (ObjectArrayList) dumMovieToColors.get(mid);	      
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
		    	} // end of reading type = colors
		    	

		    	//-----------------------------------------------------
			    // Language
			    //-----------------------------------------------------		    	
		    	
		    	if(	dumMovieToLanguages.containsKey(mid))	        		
		       	{
		       	  list  		= (ObjectArrayList) dumMovieToLanguages.get(mid);	      
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
		    	} // end of reading type = Lanuage

*/
		    /*	//-----------------------------------------------------
			    // Biograghy
			    //-----------------------------------------------------		    	
		    	
		    	if(	dumMovieToBiography.containsKey(mid))	        		
		       	{
		       	  list  		= (ObjectArrayList) dumMovieToBiography.get(mid);	      
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
		    	} // end of reading type = biography

*/		    	
		    	//-----------------------------------------------------
			    // aka Titles
			    //-----------------------------------------------------		    	
		    	
		    	if(	dumMovieToAkaTitles.containsKey(mid))	        		
		       	{
		       	  list  		= (ObjectArrayList) dumMovieToAkaTitles.get(mid);	      
		       	  type  		= 24;	       	  
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
		    	} // end of reading type = printed review

		    	//-----------------------------------------------------
			    // Prod Companies
			    //-----------------------------------------------------		    	
		    	
		    	if(dumMovieToProdCompanies.containsKey(mid))	        		
		       	{
		       	  list  		= (ObjectArrayList) dumMovieToProdCompanies.get(mid);	      
		       	  type  		= 22;	       	  
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
		    	} // end of reading type = printed review

		    	//-----------------------------------------------------
			    // paaratings
			    //-----------------------------------------------------		    	
		    	
		    	if(dumMovieToReasonText.containsKey(mid))	        		
		       	{
		       	  list  		= (ObjectArrayList) dumMovieToReasonText.get(mid);	      
		       	  type  		= 21;	       	  
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
		    	} // end of reading type = printed review

		    	
		    	//-----------------------------------------------------
			    // Actors
			    //-----------------------------------------------------		    	
		    	
		    	if(	dumMovieToActors.containsKey(mid))
		    	{        					       	
			       	  list  		= (ObjectArrayList) dumMovieToActors.get(mid);	      
			       	  type  		= 15;	       	  
			       	  int listSize  = list.size(); 
				    	
			    		// All features found in one movie
			    		for(int i=0;i<listSize;i++)
				    	 {
				    		
			    			//---------------------------
			    			// Token reading
			    			//---------------------------

			    			//Read string to be checked
				   		  	String myS = ((String)list.get(i));
				   		  	writeTokenInMovieFeature(type, mid, myS);
								 
				    	 }//end of all elements in a list for a movie				 
			     } // end of reading type = actors
		    	

			     //-----------------------------------------------------
				 // Directors
				 //-----------------------------------------------------		    	
			    	
		    	if(	dumMovieToDirectors.containsKey(mid))
		    	{
		    		  list  		= (ObjectArrayList) dumMovieToDirectors.get(mid);	      
			       	  type  		= 16;	       	  
			       	  int listSize  = list.size(); 
				    	
			    		// All features found in one movie
			    		for(int i=0;i<listSize;i++)
				    	 {
				    		
			    			//---------------------------
			    			// Token reading
			    			//---------------------------

			    			//Read string to be checked
				   		  	String myS = ((String)list.get(i));
				   		  	writeTokenInMovieFeature(type, mid, myS);
								 
				    	 }//end of all elements in a list for a movie			    		
		    	}// end of reading type = directors
	

			      //-----------------------------------------------------
				  // Producers
				  //-----------------------------------------------------		    	

		    	if(	dumMovieToProducers.containsKey(mid))
		    	{
		    		  list  		= (ObjectArrayList) dumMovieToProducers.get(mid);	      
			       	  type  		= 17;	       	  
			       	  int listSize  = list.size(); 
				    	
			    		// All features found in one movie
			    		for(int i=0;i<listSize;i++)
				    	 {
				    		
			    			//---------------------------
			    			// Token reading
			    			//---------------------------

			    			//Read string to be checked
				   		  	String myS = ((String)list.get(i));
				   		  	writeTokenInMovieFeature(type, mid, myS);
								 
				    	 }//end of all elements in a list for a movie			    		
		    	} //// end of reading type = producers

			      //-----------------------------------------------------
				  // Genres
				  //-----------------------------------------------------		    	

		    	if(	dumMovieToGenres.containsKey(mid))
		    	{
		    		  list  		= (ObjectArrayList) dumMovieToGenres.get(mid);	      
			       	  type  		= 18;	       	  
			       	  int listSize  = list.size(); 
				    	
			    		// All features found in one movie
			    		for(int i=0;i<listSize;i++)
				    	 {
				    		
			    			//---------------------------
			    			// Token reading
			    			//---------------------------

			    			//Read string to be checked
				   		  	String myS = ((String)list.get(i));
				   		  	writeTokenInMovieFeature(type, mid, myS);
								 
				    	 }//end of all elements in a list for a movie			    		
		    	} //// end of reading type = genre

		    	  //-----------------------------------------------------
				  // Editors
				  //-----------------------------------------------------		    	

		    	if(	dumMovieToEditors.containsKey(mid))
		    	{
		    		  list  		= (ObjectArrayList) dumMovieToEditors.get(mid);	      
			       	  type  		= 19;	       	  
			       	  int listSize  = list.size(); 
				    	
			    		// All features found in one movie
			    		for(int i=0;i<listSize;i++)
				    	 {
				    		
			    			//---------------------------
			    			// Token reading
			    			//---------------------------

			    			//Read string to be checked
				   		  	String myS = ((String)list.get(i));
				   		  	writeTokenInMovieFeature(type, mid, myS);
								 
				    	 }//end of all elements in a list for a movie			    		
		    	} //// end of reading type = producers

		    	  //-----------------------------------------------------
				  // Writer
				  //-----------------------------------------------------		    	

		    	if(	dumMovieToWriters.containsKey(mid))
		    	{
		    		  list  		= (ObjectArrayList) dumMovieToWriters.get(mid);	      
			       	  type  		= 19;	       	  
			       	  int listSize  = list.size(); 
				    	
			    		// All features found in one movie
			    		for(int i=0;i<listSize;i++)
				    	 {
				    		
			    			//---------------------------
			    			// Token reading
			    			//---------------------------

			    			//Read string to be checked
				   		  	String myS = ((String)list.get(i));
				   		  	writeTokenInMovieFeature(type, mid, myS);
								 
				    	 }//end of all elements in a list for a movie			    		
		    	} //// end of reading type = producers

		    	  //-----------------------------------------------------
				  // Rating/votes
				  //-----------------------------------------------------		    	

		    	if(	dumMovieToRatings.containsKey(mid))
		    	{
		    		  list  		= (ObjectArrayList) dumMovieToRatings.get(mid);	      
			       	  type  		= 19;	       	  
			       	  int listSize  = list.size(); 
				    	
			    		// All features found in one movie
			    		for(int i=0;i<listSize;i++)
				    	 {
				    		
			    			//---------------------------
			    			// Token reading
			    			//---------------------------

			    			//Read string to be checked
				   		  	String myS = ((String)list.get(i));
				   		  	writeTokenInMovieFeature(type, mid, myS);
								 
				    	 }//end of all elements in a list for a movie			    		
		    	} //// end of reading type = producers

		    	
		    	
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
	  
	  // It will build a map from tokens to TF and then store this
	  // map in a OpenIntObjectHashMap against each movie
	  
	public void  writeTokenInMovieFeature (int type, int mid, String token)
	{
	    
	    double inc = 1;
	    Map <String,Double> mapTF;
	    
	  //----------------------------------------
	  // Keywords
	  //----------------------------------------  
	    
	     if(type==10) //Keywords
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
       // Tags
	   //----------------------------------------
	      
	     mapTF  = null;
	     inc	= 1;
	    
	     if(type==9) //Tags
         {
	        	if(movieToTags.containsKey(mid))	        		
	        	{
	    	        	
	        	  mapTF = (Map<String, Double>) movieToTags.get(mid);
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
	             
	        	movieToTags.put(mid, mapTF);
	       }



		  //----------------------------------------
		  // Plots
		  //----------------------------------------
		      
		     mapTF  = null;
		     inc	= 1;
		    
		     if(type==98) //plots
	         {
		        	if(movieToPlots.containsKey(mid))	        		
		        	{
		    	        	
		        	  mapTF = (Map<String, Double>) movieToPlots.get(mid);
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
		             
		        	movieToPlots.put(mid, mapTF);
		       }


			  //----------------------------------------
			  // Certificate
			  //----------------------------------------
			      
			     mapTF  = null;
			     inc	= 1;
			    
			     if(type==5) //certifictae
		         {
			        	if(movieToCertificates.containsKey(mid))	        		
			        	{
			    	        	
			        	  mapTF = (Map<String, Double>) movieToCertificates.get(mid);
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
			             
			        	movieToCertificates.put(mid, mapTF);
			       }




				  //----------------------------------------
				  // Editor
				  //----------------------------------------
				      
				     mapTF  = null;
				     inc	= 1;
				    
				     if(type==19) //Editor
			         {
				        	if(movieToEditors.containsKey(mid))	        		
				        	{
				    	        	
				        	  mapTF = (Map<String, Double>) movieToEditors.get(mid);
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
				             
				        	movieToEditors.put(mid, mapTF);
				       }


					 /* //----------------------------------------
					  // Colors
					  //----------------------------------------
					      
					     mapTF  = null;
					     inc	= 1;
					    
					     if(type==2) //Colors
				         {
					        	if(movieToColors.containsKey(mid))	        		
					        	{
					    	        	
					        	  mapTF = (Map<String, Double>) movieToColors.get(mid);
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
					             
					        	movieToColors.put(mid, mapTF);
					       }




						  //----------------------------------------
						  // Language
						  //----------------------------------------
						      
						     mapTF  = null;
						     inc	= 1;
						    
						     if(type==4) //Language
					         {
						        	if(movieToLanguages.containsKey(mid))	        		
						        	{
						    	        	
						        	  mapTF = (Map<String, Double>) movieToLanguages.get(mid);
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
						             
						        	movieToLanguages.put(mid, mapTF);
						       }						     
						     */
							     
				//----------------------------------------
				// Actors
				//----------------------------------------
							
						     mapTF  = null;
						     inc	= 1;
						    
						     if(type==15) 
					         {
						        	if(movieToActors.containsKey(mid))	        		
						        	{						    	        	
						        	  mapTF = (Map<String, Double>) movieToActors.get(mid);
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
						             
						        	movieToActors.put(mid, mapTF);
						       }						     
						     						     
				 //----------------------------------------
				 // Directors
				 //----------------------------------------
										     
						     mapTF  = null;
						     inc	= 1;
						    
						     if(type==16) 
					         {
						        	if(movieToDirectors.containsKey(mid))	        		
						        	{						    	        	
						        	  mapTF = (Map<String, Double>) movieToDirectors.get(mid);
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
						             
						        	movieToDirectors.put(mid, mapTF);
						       }						     
						     		     
				 //----------------------------------------
				 // Producers
				 //----------------------------------------
										     
						     mapTF  = null;
						     inc	= 1;
						    
						     if(type==17) 
					         {
						        	if(movieToProducers.containsKey(mid))	        		
						        	{						    	        	
						        	  mapTF = (Map<String, Double>) movieToProducers.get(mid);
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
						             
						        	movieToProducers.put(mid, mapTF);
						       }	  
							 //----------------------------------------
							 // Genres
							 //----------------------------------------
													     
								    mapTF  = null;
								    inc	   = 1;
									    
								     if(type == 18) 
								        {
								        	if(movieToGenres.containsKey(mid))	        		
								        	{						    	        	
								        	  mapTF = (Map<String, Double>) movieToGenres.get(mid);
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
									             
									        	movieToGenres.put(mid, mapTF);
									       }
								     
		  
									 //----------------------------------------
									 // Writer
									 //----------------------------------------
															     
										    mapTF  = null;
										    inc	   = 1;
											    
										     if(type == 20) 
										        {
										        	if(movieToWriters.containsKey(mid))	        		
										        	{						    	        	
										        	  mapTF = (Map<String, Double>) movieToWriters.get(mid);
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
											             
										       		movieToWriters.put(mid, mapTF);
											       }     
										     
										     
										     //----------------------------------------
											 // paaratings
											 //----------------------------------------
																	     
												    mapTF  = null;
												    inc	   = 1;
													    
												     if(type == 21) 
												        {
												        	if(movieToReasonText.containsKey(mid))	        		
												        	{						    	        	
												        	  mapTF = (Map<String, Double>) movieToReasonText.get(mid);
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
													             
												       	movieToReasonText.put(mid, mapTF);
													    }								     
										     
										     
										     
												     //----------------------------------------
													 // ProdCompanies
													 //----------------------------------------
																			     
														    mapTF  = null;
														    inc	   = 1;
															    
														     if(type == 22) 
														        {
														        	if(movieToProdCompanies.containsKey(mid))	        		
														        	{						    	        	
														        	  mapTF = (Map<String, Double>) movieToProdCompanies.get(mid);
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
															             
														       	movieToProdCompanies.put(mid, mapTF);
															    }						     
	
											
														     //----------------------------------------
															 // ratings
															 //----------------------------------------
																					     
																    mapTF  = null;
																    inc	   = 1;
																	    
																     if(type == 23) 
																        {
																        	if(movieToRatings.containsKey(mid))	        		
																        	{						    	        	
																        	  mapTF = (Map<String, Double>) movieToRatings.get(mid);
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
																	             
																       	movieToRatings.put(mid, mapTF);
																	    }						     
			    			     

																     //----------------------------------------
																	 // aka titles
																	 //----------------------------------------
																							     
																		    mapTF  = null;
																		    inc	   = 1;
																			    
																		     if(type == 24) 
																		        {
																		        	if(movieToAkaTitles.containsKey(mid))	        		
																		        	{						    	        	
																		        	  mapTF = (Map<String, Double>) movieToAkaTitles.get(mid);
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
																			             
																		       	movieToAkaTitles.put(mid, mapTF);
																			    }						     
					    			     
					     
	
             //----------------------------------------
			 // Features all
			 //----------------------------------------
						     
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
	        
	             movieToFeatures.put(mid, mapTF);
	                
	} //end of function
	
	
/*****************************************************************************************************/
	
	//-------------------------------------------------
	// Method that verifies the TF 
	// This method was showing that same words in diff movies having the diff TF

	public void verifyTF()
	{ 
		Map <String,Double> myMap;
		Map <String,Double> tempMap;
		
		for (int i=1;i<=totalMovies;i++)
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
  	     
  	      		for (int k=1;k<=totalMovies;k++)
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
	    
	    Map <String,Double> mapTF  		= null;
	    Map <String,Double> tempMap  	= null;
	    
	    
	    Map <String,Double>  mapTFIDF_A = new HashMap<String, Double>();
	    
        for(int i=1; i<=totalMovies;i++)
        {
        	  if(movieToKeywords.containsKey(i))	        		
	          {
        		  Map <String,Double>  mapTFIDF_T = new HashMap<String, Double>();
        		  Map <String,Double>  mapTFIDF_K = new HashMap<String, Double>();
        		  
        		  idf   = 0;  //It will store idf of each word
    	      	  tf	= 0;	    	      	  
    	      	  word  = "";
    	      	  	  
    	      	  	  mapTF = (Map<String, Double>) movieToKeywords.get(i);    	      	  
    	      	  	  mapTFIDF_K = (Map<String, Double>) movieToKeywords.get(i);
    	      		  mapTFIDF_T = (Map<String, Double>) movieToKeywords.get(i);
    	      	  	
	    	      	  set = mapTF.entrySet();	    	      	       	  
	    	      	  Iterator j = set.iterator();
	    	      	  	    	      	  
	    	      	while(j.hasNext()) 
	    	      	{
	    	      	     Map.Entry words = (Map.Entry)j.next();	    	      	  
	    	      		 
	    	      	     word = (String)words.getKey();			//get the word
	    	      		 tf   = (Double)words.getValue();		//get the tf value
	    	      		 df   = 1;
	    	      		  
		    	      	  for(int k=1; k<=totalMovies;k++) // check in each movie's list
		    	      	  {
			    	      		if (k!=i ) 					// not the same movie we are dealing with
			    	      			tempMap =  (Map<String, Double>) movieToKeywords.get(k);
			    	      		
			    	      		if(tempMap!=null && tempMap.containsKey(word)) //if this movie has words 
			    	      		{
			    	      			df += 1;		//This doc contains the word
			    	      		}
			    	      		    	      		  
		    	      	  } //end of all movies
		    	      	  
		    	      	  		    	      	  
		    	      	  //------------------
		    	      	  //IDF = lod (N/df)
		    	      	  //------------------
		    	      	  
		    	      	
		    	      		  idf = Math.log10(totalMovies/(df*1.0));
		    	      	 
		    	      	  //--------------------
		    	      	  //Put TF * IDF weights
		    	      	  //--------------------
		    	        if(isIDF) 
		    	      	    mapTFIDF_T.put(word, tf * idf);     	   
		    	        else
		    	        	mapTFIDF_T.put(word, tf * 1);
		    	      	
	    	      	  } //end of checking all words in a movie
	    	      	  
	    	      	  movieToKeywordsFinal.put(i, mapTFIDF_T);
	    	      	  
	    	      	  
	    	      //	System.out.println("size keys=" + mapTFIDF.size());
	        	} //end of if       	

        //---------------------------------------------------------------------------
        //Tags	  	
     	  
        	  
        	  
        	  if(movieToTags.containsKey(i))	        		
        	   	{
        		  Map <String,Double>  mapTFIDF_T = new HashMap<String, Double>();					
        		  			
        		  idf   = 0;  						//It will store idf of each word
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
        	    	      		  
        		    	      	  for(int k=1; k<totalMovies;k++) //check in each movie's list
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
        		    	      	  
        		    	      	  idf = Math.log10(totalMovies/(df*1.0));
        		    	      	 
        		    	      	  //--------------------
        		    	      	  //Put TF * IDF weights
        		    	      	  //--------------------
        		    	      	  
        		    	      	if(isIDF) 
        		    	      	    mapTFIDF_T.put(word, tf * idf);     	   
        		    	        else
        		    	        	mapTFIDF_T.put(word, tf * 1);
        		    	      	  
        	    	      	  } //end of checking all words in a movie
        	    	      	  
        	    	      	  movieToTagsFinal.put(i, mapTFIDF_T);
        	    	      	//System.out.println("size tags=" + mapTFIDF.size());
        	    	      	  
        	        	} //end of if       	

        	   //---------------------------------------------------------------------------
              //Plots	  	
           	  
              	  if(movieToPlots.containsKey(i))	        		
              	   	{
              		  Map <String,Double>  mapTFIDF_T = new HashMap<String, Double>();
              		  
              		  idf   = 0;  //It will store idf of each word
                  	  tf	= 0;	    	      	  
                  	  word  = "";
                  	      	  	  
                  	   mapTF = (Map<String, Double>) movieToPlots.get(i);
                  	   mapTFIDF_T = (Map<String, Double>) movieToPlots.get(i);
              	       
                  	   set = mapTF.entrySet();	    	      	       	  
              	       Iterator j = set.iterator();
              	    	      	  	    	      	  
              	    	 while(j.hasNext()) 
              	    	{
              	    	    Map.Entry words = (Map.Entry)j.next();	    	      	  
              	    	      		 
              	    	       word = (String)words.getKey();			//get the word
              	    	       tf   = (Double)words.getValue();		//get the tf value
              	    	       df   = 1;
              	    	      		  
              		    	      	  for(int k=1; k<totalMovies;k++) //check in each movie's list
              		    	      	  {
              			    	      		if (k!=i) // not the same movie we are dealing with
              			    	      			tempMap =  (Map<String, Double>) movieToPlots.get(k);
              			    	      		
              			    	      		if(tempMap!=null && tempMap.containsKey(word)) //if this movie has words 
              			    	      		{
              			    	      			df += 1;		//This doc contains the word
              			    	      		}
              			    	      		    	      		  
              		    	      	  } //end of all movies
              		    	      	  
              		    	      	  //Here add this DF into a list etc
              		    	      	  
              		    	      	  //------------------
              		    	      	  //IDF = lod (N/df)
              		    	      	  //------------------
              		    	      	  
              		    	      	  idf = Math.log10(totalMovies/(df*1.0));
              		    	      	 
              		    	      	  //--------------------
              		    	      	  //Put TF * IDF weights
              		    	      	  //--------------------
              		    	      	if(isIDF) 
            		    	      	    mapTFIDF_T.put(word, tf * idf);     	   
            		    	        else
            		    	        	mapTFIDF_T.put(word, tf * 1);
              		    	      	 
              	    	      	  } //end of checking all words in a movie
              	    	      	  
              	    	      	  movieToPlotsFinal.put(i, mapTFIDF_T);
              	    	      	//System.out.println("size tags=" + mapTFIDF.size());
              	    	      	  
              	        	} //end of if       	

              	  
                  //---------------------------------------------------------------------------
                  //Certificates	  	
               	  
                  	  if(movieToCertificates.containsKey(i))	        		
                  	   	{
                  		 Map <String,Double>  mapTFIDF_T = new HashMap<String, Double>();
                  		 
                  		  idf   = 0;  //It will store idf of each word
                      	  tf	= 0;	    	      	  
                      	  word  = "";
                      	      	  	  
                      	   mapTF = (Map<String, Double>) movieToCertificates.get(i);
                      	   mapTFIDF_T = (Map<String, Double>) movieToCertificates.get(i);
                  	       
                      	   set = mapTF.entrySet();	    	      	       	  
                  	       Iterator j = set.iterator();
                  	    	      	  	    	      	  
                  	    	 while(j.hasNext()) 
                  	    	{
                  	    	    Map.Entry words = (Map.Entry)j.next();	    	      	  
                  	    	      		 
                  	    	       word = (String)words.getKey();			//get the word
                  	    	       tf   = (Double)words.getValue();		//get the tf value
                  	    	       df   = 1;
                  	    	      		  
                  		    	      	  for(int k=1; k<totalMovies;k++) //check in each movie's list
                  		    	      	  {
                  			    	      		if (k!=i) // not the same movie we are dealing with
                  			    	      			tempMap =  (Map<String, Double>) movieToCertificates.get(k);
                  			    	      		
                  			    	      		if(tempMap!=null && tempMap.containsKey(word)) //if this movie has words 
                  			    	      		{
                  			    	      			df += 1;		//This doc contains the word
                  			    	      		}
                  			    	      		    	      		  
                  		    	      	  } //end of all movies
                  		    	      	  
                  		    	      	  //Here add this DF into a list etc
                  		    	      	  
                  		    	      	  //------------------
                  		    	      	  //IDF = lod (N/df)
                  		    	      	  //------------------
                  		    	      	  
                  		    	      	  idf = Math.log10(totalMovies/(df*1.0));
                  		    	      	 
                  		    	      	  //--------------------
                  		    	      	  //Put TF * IDF weights
                  		    	      	  //--------------------
                  		    	      	 
                  		    	      	if(isIDF) 
                		    	      	    mapTFIDF_T.put(word, tf * idf);     	   
                		    	        else
                		    	        	mapTFIDF_T.put(word, tf * 1);
                  		    	      	
                  	    	      	  } //end of checking all words in a movie
                  	    	      	  
                  	    	      	  movieToCertificatesFinal.put(i, mapTFIDF_T);
                  	    	      	  
                  	    	      	//System.out.println("size tags=" + mapTFIDF.size());
                  	    	      	  
                  	        	} //end of if       	
          
          
                      //---------------------------------------------------------------------------
                      // Editores	  	
                   	  
                      	  if(movieToEditors.containsKey(i))	        		
                      	   	{
                      		Map <String,Double>  mapTFIDF_T = new HashMap<String, Double>();
                      		 
                      		  idf   = 0;  //It will store idf of each word
                          	  tf	= 0;	    	      	  
                          	  word  = "";
                          	      	  	  
                          	   mapTF = (Map<String, Double>) movieToEditors.get(i);
                          	   mapTFIDF_T = (Map<String, Double>) movieToEditors.get(i);
                      	       
                          	   set = mapTF.entrySet();	    	      	       	  
                      	       Iterator j = set.iterator();
                      	    	      	  	    	      	  
                      	    	 while(j.hasNext()) 
                      	    	{
                      	    	    Map.Entry words = (Map.Entry)j.next();	    	      	  
                      	    	      		 
                      	    	       word = (String)words.getKey();			//get the word
                      	    	       tf   = (Double)words.getValue();		//get the tf value
                      	    	       df   = 1;
                      	    	      		  
                      		    	      	  for(int k=1; k<totalMovies;k++) //check in each movie's list
                      		    	      	  {
                      			    	      		if (k!=i) // not the same movie we are dealing with
                      			    	      			tempMap =  (Map<String, Double>) movieToEditors.get(k);
                      			    	      		
                      			    	      		if(tempMap!=null && tempMap.containsKey(word)) //if this movie has words 
                      			    	      		{
                      			    	      			df += 1;		//This doc contains the word
                      			    	      		}
                      			    	      		    	      		  
                      		    	      	  } //end of all movies
                      		    	      	  
                      		    	      	  //Here add this DF into a list etc
                      		    	      	  
                      		    	      	  //------------------
                      		    	      	  //IDF = lod (N/df)
                      		    	      	  //------------------
                      		    	      	  
                      		    	      	  idf = Math.log(totalMovies/(df*1.0));
                      		    	      	 
                      		    	      	  //--------------------
                      		    	      	  //Put TF * IDF weights
                      		    	      	  //--------------------
                      		    	      	 
                      		    	      	if(isIDF) 
                    		    	      	    mapTFIDF_T.put(word, tf * idf);     	   
                    		    	        else
                    		    	        	mapTFIDF_T.put(word, tf * 1);
                      		    	      	
                      	    	      	  } //end of checking all words in a movie
                      	    	      	  
                      	    	 			movieToEditorsFinal.put(i, mapTFIDF_T);
                      	    	 			
                      	    	      	//System.out.println("size tags=" + mapTFIDF.size());
                      	    	      	  
                      	        	} //end of if       	

                        /*  //---------------------------------------------------------------------------
                          // PrintedReview	  	
                       	  
                          	  if(movieToPrintedReviews.containsKey(i))	        		
                          	   	{
                          		Map <String,Double>  mapTFIDF_T = new HashMap<String, Double>();
                          		 
                          		  idf   = 0;  //It will store idf of each word
                              	  tf	= 0;	    	      	  
                              	  word  = "";
                              	      	  	  
                              	   mapTF = (Map<String, Double>) movieToPrintedReviews.get(i);
                              	   mapTFIDF_T = (Map<String, Double>) movieToPrintedReviews.get(i);
                          	       
                              	   set = mapTF.entrySet();	    	      	       	  
                          	       Iterator j = set.iterator();
                          	    	      	  	    	      	  
                          	    	 while(j.hasNext()) 
                          	    	{
                          	    	    Map.Entry words = (Map.Entry)j.next();	    	      	  
                          	    	      		 
                          	    	       word = (String)words.getKey();			//get the word
                          	    	       tf   = (Double)words.getValue();		//get the tf value
                          	    	       df   = 1;
                          	    	      		  
                          		    	      	  for(int k=1; k<totalMovies;k++) //check in each movie's list
                          		    	      	  {
                          			    	      		if (k!=i) // not the same movie we are dealing with
                          			    	      			tempMap =  (Map<String, Double>) movieToPrintedReviews.get(k);
                          			    	      		
                          			    	      		if(tempMap!=null && tempMap.containsKey(word)) //if this movie has words 
                          			    	      		{
                          			    	      			df += 1;		//This doc contains the word
                          			    	      		}
                          			    	      		    	      		  
                          		    	      	  } //end of all movies
                          		    	      	  
                          		    	      	  //Here add this DF into a list etc
                          		    	      	  
                          		    	      	  //------------------
                          		    	      	  //IDF = lod (N/df)
                          		    	      	  //------------------
                          		    	      	  
                          		    	      	  idf = Math.log(totalMovies/(df*1.0));
                          		    	      	 
                          		    	      	  //--------------------
                          		    	      	  //Put TF * IDF weights
                          		    	      	  //--------------------
                          		    	      	 
                          		    	      	// mapTFIDF_T.put(word, tf * idf); 		    	      	  
                          		    	           mapTFIDF_T.put(word, tf * 1);
                          		    	      	  
                          	    	      	  } //end of checking all words in a movie
                          	    	      	  
                          	    	      	  movieToPrintedReviewsFinal.put(i, mapTFIDF_T);
                          	    	      	//System.out.println("size tags=" + mapTFIDF.size());
                          	    	      	  
                          	        	} //end of if       	

                          	  
                              //---------------------------------------------------------------------------
                              // Language	  	
                           	  
                              	  if(movieToLanguages.containsKey(i))	        		
                              	   	{
                              		Map <String,Double>  mapTFIDF_T = new HashMap<String, Double>();
                              		 
                              		  idf   = 0;  //It will store idf of each word
                                  	  tf	= 0;	    	      	  
                                  	  word  = "";
                                  	      	  	  
                                  	   mapTF = (Map<String, Double>) movieToLanguages.get(i);
                                  	   mapTFIDF_T = (Map<String, Double>) movieToLanguages.get(i);
                              	       
                                  	   set = mapTF.entrySet();	    	      	       	  
                              	       Iterator j = set.iterator();
                              	    	      	  	    	      	  
                              	    	 while(j.hasNext()) 
                              	    	{
                              	    	    Map.Entry words = (Map.Entry)j.next();	    	      	  
                              	    	      		 
                              	    	       word = (String)words.getKey();			//get the word
                              	    	       tf   = (Double)words.getValue();		//get the tf value
                              	    	       df   = 1;
                              	    	      		  
                              		    	      	  for(int k=1; k<totalMovies;k++) //check in each movie's list
                              		    	      	  {
                              			    	      		if (k!=i) // not the same movie we are dealing with
                              			    	      			tempMap =  (Map<String, Double>) movieToLanguages.get(k);
                              			    	      		
                              			    	      		if(tempMap!=null && tempMap.containsKey(word)) //if this movie has words 
                              			    	      		{
                              			    	      			df += 1;		//This doc contains the word
                              			    	      		}
                              			    	      		    	      		  
                              		    	      	  } //end of all movies
                              		    	      	  
                              		    	      	  //Here add this DF into a list etc
                              		    	      	  
                              		    	      	  //------------------
                              		    	      	  //IDF = lod (N/df)
                              		    	      	  //------------------
                              		    	      	  
                              		    	      	  idf = Math.log(totalMovies/(df*1.0));
                              		    	      	 
                              		    	      	  //--------------------
                              		    	      	  //Put TF * IDF weights
                              		    	      	  //--------------------
                              		    	      	 
                              		    	      	// mapTFIDF_T.put(word, tf * idf); 		    	      	  
                              		    	           mapTFIDF_T.put(word, tf * 1);
                              		    	       
                              	    	      	  } //end of checking all words in a movie
                              	    	      	  
                              	    	      	  movieToLanguagesFinal.put(i, mapTFIDF_T);
                              	    	      	//System.out.println("size tags=" + mapTFIDF.size());
                              	    	      	  
                              	        	} //end of if
                              	  

                              	 //---------------------------------------------------------------------------
                                  //Colors	  	
                               	  
                                  	  if(movieToColors.containsKey(i))	        		
                                  	   	{
                                  		  Map <String,Double>  mapTFIDF_T = new HashMap<String, Double>();
                                  		 
                                  		  idf   = 0;  //It will store idf of each word
                                      	  tf	= 0;	    	      	  
                                      	  word  = "";
                                      	      	  	  
                                      	   mapTF = (Map<String, Double>) movieToColors.get(i);
                                      	   mapTFIDF_T = (Map<String, Double>) movieToColors.get(i);
                                  	       
                                      	   set = mapTF.entrySet();	    	      	       	  
                                  	       Iterator j = set.iterator();
                                  	    	      	  	    	      	  
                                  	    	 while(j.hasNext()) 
                                  	    	{
                                  	    	    Map.Entry words = (Map.Entry)j.next();	    	      	  
                                  	    	      		 
                                  	    	       word = (String)words.getKey();			//get the word
                                  	    	       tf   = (Double)words.getValue();		//get the tf value
                                  	    	       df   = 1;
                                  	    	      		  
                                  		    	      	  for(int k=1; k<totalMovies;k++) //check in each movie's list
                                  		    	      	  {
                                  			    	      		if (k!=i) // not the same movie we are dealing with
                                  			    	      			tempMap =  (Map<String, Double>) movieToColors.get(k);
                                  			    	      		
                                  			    	      		if(tempMap!=null && tempMap.containsKey(word)) //if this movie has words 
                                  			    	      		{
                                  			    	      			df += 1;		//This doc contains the word
                                  			    	      		}
                                  			    	      		    	      		  
                                  		    	      	  } //end of all movies
                                  		    	      	  
                                  		    	      	  //Here add this DF into a list etc
                                  		    	      	  
                                  		    	      	  //------------------
                                  		    	      	  //IDF = lod (N/df)
                                  		    	      	  //------------------
                                  		    	      	  
                                  		    	      	  idf = Math.log(totalMovies/(df*1.0));
                                  		    	      	 
                                  		    	      	  //--------------------
                                  		    	      	  //Put TF * IDF weights
                                  		    	      	  //--------------------
                                  		    	      	 
                                  		    	      	// mapTFIDF_T.put(word, tf * idf); 		    	      	  
                                  		    	      	 mapTFIDF_T.put(word, tf * 1.0);
                                  		    	      	  
                                  	    	      	  } //end of checking all words in a movie
                                  	    	      	  
                                  	    	      	  movieToColorsFinal.put(i, mapTFIDF_T);
                                  	    	      	//System.out.println("size tags=" + mapTFIDF.size());
                                  	    	      	  
                                  	        	} //end of if       	

*/
              //---------------------------------------------------------------------------
              // Actors      
                             
                              if(movieToActors.containsKey(i))	        		
                               {     
                            	  Map <String,Double>  mapTFIDF_T = new HashMap<String, Double>();
                           		 
                          		  idf   = 0;  //It will store idf of each word
                              	  tf	= 0;	    	      	  
                              	  word  = "";
                              	      	  	  
                              	   mapTF = (Map<String, Double>) movieToActors.get(i);
                              	   mapTFIDF_T = (Map<String, Double>) movieToActors.get(i);
                          	       
                              	   set = mapTF.entrySet();	    	      	       	  
                          	       Iterator j = set.iterator();
                          	    	      	  	    	      	  
                          	    	 while(j.hasNext()) 
                          	    	{
                          	    	    Map.Entry words = (Map.Entry)j.next();	    	      	  
                          	    	      		 
                          	    	       word = (String)words.getKey();			//get the word
                          	    	       tf   = (Double)words.getValue();		//get the tf value
                          	    	       df   = 1;
                          	    	      		  
                          		    	      	  for(int k=1; k<totalMovies;k++) //check in each movie's list
                          		    	      	  {
                          			    	      		if (k!=i) // not the same movie we are dealing with
                          			    	      			tempMap =  (Map<String, Double>) movieToActors.get(k);
                          			    	      		
                          			    	      		if(tempMap!=null && tempMap.containsKey(word)) //if this movie has words 
                          			    	      		{
                          			    	      			df += 1;		//This doc contains the word
                          			    	      		}
                          			    	      		    	      		  
                          		    	      	  } //end of all movies
                          		    	      	  
                          		    	      	  //Here add this DF into a list etc
                          		    	      	  
                          		    	      	  //------------------
                          		    	      	  //IDF = lod (N/df)
                          		    	      	  //------------------
                          		    	      	  
                          		    	      	  idf = Math.log(totalMovies/(df*1.0));
                          		    	      	 
                          		    	      	  //--------------------
                          		    	      	  //Put TF * IDF weights
                          		    	      	  //--------------------
                          		    	      	 
                          		    	      	if(isIDF) 
                        		    	      	    mapTFIDF_T.put(word, tf * idf);     	   
                        		    	        else
                        		    	        	mapTFIDF_T.put(word, tf * 1);
                          		    	      	  
                          	    	      	  } //end of checking all words in a movie
                          	    	      	  
                          	    	      	  movieToActorsFinal.put(i, mapTFIDF_T);
                          	    	      	  
                          	    	      	//System.out.println("size tags=" + mapTFIDF.size());
                          	    	      	  
                             	  
                               } //end of if                         	  
                             
              //---------------------------------------------------------------------------
              // Directors        

                              if(movieToDirectors.containsKey(i))	        		
                               {                               	                                 	      	  	  
                              	  Map <String,Double>  mapTFIDF_T = new HashMap<String, Double>();
                            		 
                          		  idf   = 0;  //It will store idf of each word
                              	  tf	= 0;	    	      	  
                              	  word  = "";
                              	      	  	  
                              	   mapTF = (Map<String, Double>) movieToDirectors.get(i);
                              	   mapTFIDF_T = (Map<String, Double>) movieToDirectors.get(i);
                          	       
                              	   set = mapTF.entrySet();	    	      	       	  
                          	       Iterator j = set.iterator();
                          	    	      	  	    	      	  
                          	    	 while(j.hasNext()) 
                          	    	{
                          	    	    Map.Entry words = (Map.Entry)j.next();	    	      	  
                          	    	      		 
                          	    	       word = (String)words.getKey();			//get the word
                          	    	       tf   = (Double)words.getValue();		//get the tf value
                          	    	       df   = 1;
                          	    	      		  
                          		    	      	  for(int k=1; k<totalMovies;k++) //check in each movie's list
                          		    	      	  {
                          			    	      		if (k!=i) // not the same movie we are dealing with
                          			    	      			tempMap =  (Map<String, Double>) movieToDirectors.get(k);
                          			    	      		
                          			    	      		if(tempMap!=null && tempMap.containsKey(word)) //if this movie has words 
                          			    	      		{
                          			    	      			df += 1;		//This doc contains the word
                          			    	      		}
                          			    	      		    	      		  
                          		    	      	  } //end of all movies
                          		    	      	  
                          		    	      	  //Here add this DF into a list etc
                          		    	      	  
                          		    	      	  //------------------
                          		    	      	  //IDF = lod (N/df)
                          		    	      	  //------------------
                          		    	      	  
                          		    	      	  idf = Math.log(totalMovies/(df*1.0));
                          		    	      	 
                          		    	      	  //--------------------
                          		    	      	  //Put TF * IDF weights
                          		    	      	  //--------------------
                          		    	      	 
                          		    	      	if(isIDF) 
                        		    	      	    mapTFIDF_T.put(word, tf * idf);     	   
                        		    	        else
                        		    	        	mapTFIDF_T.put(word, tf * 1);
                          		    	      	  
                          	    	      	  } //end of checking all words in a movie
                          	    	      	  
                          	    	 			movieToDirectorsFinal.put(i, mapTFIDF_T);
                          	    	      	//System.out.println("size tags=" + mapTFIDF.size());
                          	    	      	  
                             	  
                               } //end of if                                  	  
              
              //---------------------------------------------------------------------------
              // Producers      

                              if(movieToProducers.containsKey(i))	        		
                               {                               	                                 	      	  	  
                            	  Map <String,Double>  mapTFIDF_T = new HashMap<String, Double>();
                         		 
                          		  idf   = 0;  //It will store idf of each word
                              	  tf	= 0;	    	      	  
                              	  word  = "";
                              	      	  	  
                              	   mapTF = (Map<String, Double>) movieToProducers.get(i);
                              	   mapTFIDF_T = (Map<String, Double>) movieToProducers.get(i);
                          	       
                              	   set = mapTF.entrySet();	    	      	       	  
                          	       Iterator j = set.iterator();
                          	    	      	  	    	      	  
                          	    	 while(j.hasNext()) 
                          	    	{
                          	    	    Map.Entry words = (Map.Entry)j.next();	    	      	  
                          	    	      		 
                          	    	       word = (String)words.getKey();			//get the word
                          	    	       tf   = (Double)words.getValue();		//get the tf value
                          	    	       df   = 1;
                          	    	      		  
                          		    	      	  for(int k=1; k<totalMovies;k++) //check in each movie's list
                          		    	      	  {
                          			    	      		if (k!=i) // not the same movie we are dealing with
                          			    	      			tempMap =  (Map<String, Double>) movieToProducers.get(k);
                          			    	      		
                          			    	      		if(tempMap!=null && tempMap.containsKey(word)) //if this movie has words 
                          			    	      		{
                          			    	      			df += 1;		//This doc contains the word
                          			    	      		}
                          			    	      		    	      		  
                          		    	      	  } //end of all movies
                          		    	      	  
                          		    	      	  //Here add this DF into a list etc
                          		    	      	  
                          		    	      	  //------------------
                          		    	      	  //IDF = lod (N/df)
                          		    	      	  //------------------
                          		    	      	  
                          		    	      	  idf = Math.log(totalMovies/(df*1.0));
                          		    	      	 
                          		    	      	  //--------------------
                          		    	      	  //Put TF * IDF weights
                          		    	      	  //--------------------
                          		    	      	 
                          		    	      	if(isIDF) 
                        		    	      	    mapTFIDF_T.put(word, tf * idf);     	   
                        		    	        else
                        		    	        	mapTFIDF_T.put(word, tf * 1);
                          		    	      	  
                          	    	      	  } //end of checking all words in a movie
                          	    	      	  
                          	    	 		 movieToProducersFinal.put(i, mapTFIDF_T);
                          	    	      	//System.out.println("size tags=" + mapTFIDF.size());
                          	    	      	  
                             	  
                               } //end of if                          	  
                              //---------------------------------------------------------------------------
                              // Genres      

                                              if(movieToGenres.containsKey(i))	        		
                                               {                               	                                 	      	  	  
                                            	  Map <String,Double>  mapTFIDF_T = new HashMap<String, Double>();
                                         		 
                                          		  idf   = 0;  //It will store idf of each word
                                              	  tf	= 0;	    	      	  
                                              	  word  = "";
                                              	      	  	  
                                              	   mapTF = (Map<String, Double>) movieToGenres.get(i);
                                              	   mapTFIDF_T = (Map<String, Double>) movieToGenres.get(i);
                                          	       
                                              	   set = mapTF.entrySet();	    	      	       	  
                                          	       Iterator j = set.iterator();
                                          	    	      	  	    	      	  
                                          	    	 while(j.hasNext()) 
                                          	    	{
                                          	    	    Map.Entry words = (Map.Entry)j.next();	    	      	  
                                          	    	      		 
                                          	    	       word = (String)words.getKey();			//get the word
                                          	    	       tf   = (Double)words.getValue();		//get the tf value
                                          	    	       df   = 1;
                                          	    	      		  
                                          		    	      	  for(int k=1; k<totalMovies;k++) //check in each movie's list
                                          		    	      	  {
                                          			    	      		if (k!=i) // not the same movie we are dealing with
                                          			    	      			tempMap =  (Map<String, Double>) movieToGenres.get(k);
                                          			    	      		
                                          			    	      		if(tempMap!=null && tempMap.containsKey(word)) //if this movie has words 
                                          			    	      		{
                                          			    	      			df += 1;		//This doc contains the word
                                          			    	      		}
                                          			    	      		    	      		  
                                          		    	      	  } //end of all movies
                                          		    	      	  
                                          		    	      	  //Here add this DF into a list etc
                                          		    	      	  
                                          		    	      	  //------------------
                                          		    	      	  //IDF = lod (N/df)
                                          		    	      	  //------------------
                                          		    	      	  
                                          		    	      	  idf = Math.log(totalMovies/(df*1.0));
                                          		    	      	 
                                          		    	      	  //--------------------
                                          		    	      	  //Put TF * IDF weights
                                          		    	      	  //--------------------
                                          		    	      	 
                                          		    	      	if(isIDF) 
                                        		    	      	    mapTFIDF_T.put(word, tf * idf);     	   
                                        		    	        else
                                        		    	        	mapTFIDF_T.put(word, tf * 1);
                                          		    	      	  
                                          	    	      	  } //end of checking all words in a movie
                                          	    	      	  
                                          	    	 		 movieToGenresFinal.put(i, mapTFIDF_T);
                                          	    	      	//System.out.println("size tags=" + mapTFIDF.size());
                                          	    	      	  
                                             	  
                                               } //end of if   
                         
                                              //---------------------------------------------------------------------------
                                              // Certificates      

                                                              if(movieToCertificates.containsKey(i))	        		
                                                               {                               	                                 	      	  	  
                                                            	  Map <String,Double>  mapTFIDF_T = new HashMap<String, Double>();
                                                         		 
                                                          		  idf   = 0;  //It will store idf of each word
                                                              	  tf	= 0;	    	      	  
                                                              	  word  = "";
                                                              	      	  	  
                                                              	   mapTF = (Map<String, Double>) movieToCertificates.get(i);
                                                              	   mapTFIDF_T = (Map<String, Double>) movieToCertificates.get(i);
                                                          	       
                                                              	   set = mapTF.entrySet();	    	      	       	  
                                                          	       Iterator j = set.iterator();
                                                          	    	      	  	    	      	  
                                                          	    	 while(j.hasNext()) 
                                                          	    	{
                                                          	    	    Map.Entry words = (Map.Entry)j.next();	    	      	  
                                                          	    	      		 
                                                          	    	       word = (String)words.getKey();			//get the word
                                                          	    	       tf   = (Double)words.getValue();		//get the tf value
                                                          	    	       df   = 1;
                                                          	    	      		  
                                                          		    	      	  for(int k=1; k<totalMovies;k++) //check in each movie's list
                                                          		    	      	  {
                                                          			    	      		if (k!=i) // not the same movie we are dealing with
                                                          			    	      			tempMap =  (Map<String, Double>) movieToCertificates.get(k);
                                                          			    	      		
                                                          			    	      		if(tempMap!=null && tempMap.containsKey(word)) //if this movie has words 
                                                          			    	      		{
                                                          			    	      			df += 1;		//This doc contains the word
                                                          			    	      		}
                                                          			    	      		    	      		  
                                                          		    	      	  } //end of all movies
                                                          		    	      	  
                                                          		    	      	  //Here add this DF into a list etc
                                                          		    	      	  
                                                          		    	      	  //------------------
                                                          		    	      	  //IDF = lod (N/df)
                                                          		    	      	  //------------------
                                                          		    	      	  
                                                          		    	      	  idf = Math.log(totalMovies/(df*1.0));
                                                          		    	      	 
                                                          		    	      	  //--------------------
                                                          		    	      	  //Put TF * IDF weights
                                                          		    	      	  //--------------------
                                                          		    	      	 
                                                          		    	      	if(isIDF) 
                                                        		    	      	    mapTFIDF_T.put(word, tf * idf);     	   
                                                        		    	        else
                                                        		    	        	mapTFIDF_T.put(word, tf * 1);
                                                          		    	      	  
                                                          	    	      	  } //end of checking all words in a movie
                                                          	    	      	  
                                                          	    	 			movieToCertificatesFinal.put(i, mapTFIDF_T);
                                                          	    	      	//System.out.println("size tags=" + mapTFIDF.size());
                                                          	    	      	  
                                                             	  
                                                               } //end of if   
                                                              
                                              
                                                              //---------------------------------------------------------------------------
                                                              // Writers      

                                                                              if(movieToWriters.containsKey(i))	        		
                                                                               {                               	                                 	      	  	  
                                                                            	  Map <String,Double>  mapTFIDF_T = new HashMap<String, Double>();
                                                                         		 
                                                                          		  idf   = 0;  //It will store idf of each word
                                                                              	  tf	= 0;	    	      	  
                                                                              	  word  = "";
                                                                              	      	  	  
                                                                              	   mapTF = (Map<String, Double>) movieToWriters.get(i);
                                                                              	   mapTFIDF_T = (Map<String, Double>) movieToWriters.get(i);
                                                                          	       
                                                                              	   set = mapTF.entrySet();	    	      	       	  
                                                                          	       Iterator j = set.iterator();
                                                                          	    	      	  	    	      	  
                                                                          	    	 while(j.hasNext()) 
                                                                          	    	{
                                                                          	    	    Map.Entry words = (Map.Entry)j.next();	    	      	  
                                                                          	    	      		 
                                                                          	    	       word = (String)words.getKey();			//get the word
                                                                          	    	       tf   = (Double)words.getValue();		//get the tf value
                                                                          	    	       df   = 1;
                                                                          	    	      		  
                                                                          		    	      	  for(int k=1; k<totalMovies;k++) //check in each movie's list
                                                                          		    	      	  {
                                                                          			    	      		if (k!=i) // not the same movie we are dealing with
                                                                          			    	      			tempMap =  (Map<String, Double>) movieToWriters.get(k);
                                                                          			    	      		
                                                                          			    	      		if(tempMap!=null && tempMap.containsKey(word)) //if this movie has words 
                                                                          			    	      		{
                                                                          			    	      			df += 1;		//This doc contains the word
                                                                          			    	      		}
                                                                          			    	      		    	      		  
                                                                          		    	      	  } //end of all movies
                                                                          		    	      	  
                                                                          		    	      	  //Here add this DF into a list etc
                                                                          		    	      	  
                                                                          		    	      	  //------------------
                                                                          		    	      	  //IDF = lod (N/df)
                                                                          		    	      	  //------------------
                                                                          		    	      	  
                                                                          		    	      	  idf = Math.log(totalMovies/(df*1.0));
                                                                          		    	      	 
                                                                          		    	      	  //--------------------
                                                                          		    	      	  //Put TF * IDF weights
                                                                          		    	      	  //--------------------
                                                                          		    	      	 
                                                                          		    	      	if(isIDF) 
                                                                        		    	      	    mapTFIDF_T.put(word, tf * idf);     	   
                                                                        		    	        else
                                                                        		    	        	mapTFIDF_T.put(word, tf * 1);
                                                                          		    	      	  
                                                                          	    	      	  } //end of checking all words in a movie
                                                                          	    	      	  
                                                                          	    	 		movieToWritersFinal.put(i, mapTFIDF_T);
                                                                          	    	      	//System.out.println("size tags=" + mapTFIDF.size());
                                                                          	    	      	  
                                                                             	  
                                                                               } //end of if   
                                                                              
                                                                              
                                                                              //---------------------------------------------------------------------------
                                                                              // Writers      

                                                                                              if(movieToReasonText.containsKey(i))	        		
                                                                                               {                               	                                 	      	  	  
                                                                                            	  Map <String,Double>  mapTFIDF_T = new HashMap<String, Double>();
                                                                                         		 
                                                                                          		  idf   = 0;  //It will store idf of each word
                                                                                              	  tf	= 0;	    	      	  
                                                                                              	  word  = "";
                                                                                              	      	  	  
                                                                                              	   mapTF = (Map<String, Double>) movieToReasonText.get(i);
                                                                                              	   mapTFIDF_T = (Map<String, Double>) movieToReasonText.get(i);
                                                                                          	       
                                                                                              	   set = mapTF.entrySet();	    	      	       	  
                                                                                          	       Iterator j = set.iterator();
                                                                                          	    	      	  	    	      	  
                                                                                          	    	 while(j.hasNext()) 
                                                                                          	    	{
                                                                                          	    	    Map.Entry words = (Map.Entry)j.next();	    	      	  
                                                                                          	    	      		 
                                                                                          	    	       word = (String)words.getKey();			//get the word
                                                                                          	    	       tf   = (Double)words.getValue();		//get the tf value
                                                                                          	    	       df   = 1;
                                                                                          	    	      		  
                                                                                          		    	      	  for(int k=1; k<totalMovies;k++) //check in each movie's list
                                                                                          		    	      	  {
                                                                                          			    	      		if (k!=i) // not the same movie we are dealing with
                                                                                          			    	      			tempMap =  (Map<String, Double>) movieToReasonText.get(k);
                                                                                          			    	      		
                                                                                          			    	      		if(tempMap!=null && tempMap.containsKey(word)) //if this movie has words 
                                                                                          			    	      		{
                                                                                          			    	      			df += 1;		//This doc contains the word
                                                                                          			    	      		}
                                                                                          			    	      		    	      		  
                                                                                          		    	      	  } //end of all movies
                                                                                          		    	      	  
                                                                                          		    	      	  //Here add this DF into a list etc
                                                                                          		    	      	  
                                                                                          		    	      	  //------------------
                                                                                          		    	      	  //IDF = lod (N/df)
                                                                                          		    	      	  //------------------
                                                                                          		    	      	  
                                                                                          		    	      	  idf = Math.log(totalMovies/(df*1.0));
                                                                                          		    	      	 
                                                                                          		    	      	  //--------------------
                                                                                          		    	      	  //Put TF * IDF weights
                                                                                          		    	      	  //--------------------
                                                                                          		    	      	 
                                                                                          		    	      	if(isIDF) 
                                                                                        		    	      	    mapTFIDF_T.put(word, tf * idf);     	   
                                                                                        		    	        else
                                                                                        		    	        	mapTFIDF_T.put(word, tf * 1);
                                                                                          		    	      	  
                                                                                          	    	      	  } //end of checking all words in a movie
                                                                                          	    	      	  
                                                                                          	    	 			movieToReasonTextFinal.put(i, mapTFIDF_T);
                                                                                          	    	      	//System.out.println("size tags=" + mapTFIDF.size());
                                                                                          	    	      
                                                                                             	  
                                                                                               } //end of if   
                                                                                              
                                                                                              //---------------------------------------------------------------------------
                                                                                              // Prod Companies      

                                                                                                              if(movieToProdCompanies.containsKey(i))	        		
                                                                                                               {                               	                                 	      	  	  
                                                                                                            	  Map <String,Double>  mapTFIDF_T = new HashMap<String, Double>();
                                                                                                         		 
                                                                                                          		  idf   = 0;  //It will store idf of each word
                                                                                                              	  tf	= 0;	    	      	  
                                                                                                              	  word  = "";
                                                                                                              	      	  	  
                                                                                                              	   mapTF = (Map<String, Double>) movieToProdCompanies.get(i);
                                                                                                              	   mapTFIDF_T = (Map<String, Double>) movieToProdCompanies.get(i);
                                                                                                          	       
                                                                                                              	   set = mapTF.entrySet();	    	      	       	  
                                                                                                          	       Iterator j = set.iterator();
                                                                                                          	    	      	  	    	      	  
                                                                                                          	    	 while(j.hasNext()) 
                                                                                                          	    	{
                                                                                                          	    	    Map.Entry words = (Map.Entry)j.next();	    	      	  
                                                                                                          	    	      		 
                                                                                                          	    	       word = (String)words.getKey();			//get the word
                                                                                                          	    	       tf   = (Double)words.getValue();		//get the tf value
                                                                                                          	    	       df   = 1;
                                                                                                          	    	      		  
                                                                                                          		    	      	  for(int k=1; k<totalMovies;k++) //check in each movie's list
                                                                                                          		    	      	  {
                                                                                                          			    	      		if (k!=i) // not the same movie we are dealing with
                                                                                                          			    	      			tempMap =  (Map<String, Double>) movieToProdCompanies.get(k);
                                                                                                          			    	      		
                                                                                                          			    	      		if(tempMap!=null && tempMap.containsKey(word)) //if this movie has words 
                                                                                                          			    	      		{
                                                                                                          			    	      			df += 1;		//This doc contains the word
                                                                                                          			    	      		}
                                                                                                          			    	      		    	      		  
                                                                                                          		    	      	  } //end of all movies
                                                                                                          		    	      	  
                                                                                                          		    	      	  //Here add this DF into a list etc
                                                                                                          		    	      	  
                                                                                                          		    	      	  //------------------
                                                                                                          		    	      	  //IDF = lod (N/df)
                                                                                                          		    	      	  //------------------
                                                                                                          		    	      	  
                                                                                                          		    	      	  idf = Math.log(totalMovies/(df*1.0));
                                                                                                          		    	      	 
                                                                                                          		    	      	  //--------------------
                                                                                                          		    	      	  //Put TF * IDF weights
                                                                                                          		    	      	  //--------------------
                                                                                                          		    	      	 
                                                                                                          		    	      	if(isIDF) 
                                                                                                        		    	      	    mapTFIDF_T.put(word, tf * idf);     	   
                                                                                                        		    	        else
                                                                                                        		    	        	mapTFIDF_T.put(word, tf * 1);
                                                                                                          		    	      	  
                                                                                                          	    	      	  } //end of checking all words in a movie
                                                                                                          	    	      	  
                                                                                                          	    	 			movieToProdCompaniesFinal.put(i, mapTFIDF_T);
                                                                                                          	    	      	//System.out.println("size tags=" + mapTFIDF.size());
                                                                                                          	    	      
                                                                                                             	  
                                                                                                               } //end of if   
                                                                                                              
                                                                                                              //---------------------------------------------------------------------------
                                                                                                              // Rating    

                                                                                                                              if(movieToRatings.containsKey(i))	        		
                                                                                                                               {                               	                                 	      	  	  
                                                                                                                            	  Map <String,Double>  mapTFIDF_T = new HashMap<String, Double>();
                                                                                                                         		 
                                                                                                                          		  idf   = 0;  //It will store idf of each word
                                                                                                                              	  tf	= 0;	    	      	  
                                                                                                                              	  word  = "";
                                                                                                                              	      	  	  
                                                                                                                              	   mapTF = (Map<String, Double>) movieToRatings.get(i);
                                                                                                                              	   mapTFIDF_T = (Map<String, Double>) movieToRatings.get(i);
                                                                                                                          	       
                                                                                                                              	   set = mapTF.entrySet();	    	      	       	  
                                                                                                                          	       Iterator j = set.iterator();
                                                                                                                          	    	      	  	    	      	  
                                                                                                                          	    	 while(j.hasNext()) 
                                                                                                                          	    	{
                                                                                                                          	    	    Map.Entry words = (Map.Entry)j.next();	    	      	  
                                                                                                                          	    	      		 
                                                                                                                          	    	       word = (String)words.getKey();			//get the word
                                                                                                                          	    	       tf   = (Double)words.getValue();		//get the tf value
                                                                                                                          	    	       df   = 1;
                                                                                                                          	    	      		  
                                                                                                                          		    	      	  for(int k=1; k<totalMovies;k++) //check in each movie's list
                                                                                                                          		    	      	  {
                                                                                                                          			    	      		if (k!=i) // not the same movie we are dealing with
                                                                                                                          			    	      			tempMap =  (Map<String, Double>) movieToRatings.get(k);
                                                                                                                          			    	      		
                                                                                                                          			    	      		if(tempMap!=null && tempMap.containsKey(word)) //if this movie has words 
                                                                                                                          			    	      		{
                                                                                                                          			    	      			df += 1;		//This doc contains the word
                                                                                                                          			    	      		}
                                                                                                                          			    	      		    	      		  
                                                                                                                          		    	      	  } //end of all movies
                                                                                                                          		    	      	  
                                                                                                                          		    	      	  //Here add this DF into a list etc
                                                                                                                          		    	      	  
                                                                                                                          		    	      	  //------------------
                                                                                                                          		    	      	  //IDF = lod (N/df)
                                                                                                                          		    	      	  //------------------
                                                                                                                          		    	      	  
                                                                                                                          		    	      	  idf = Math.log(totalMovies/(df*1.0));
                                                                                                                          		    	      	 
                                                                                                                          		    	      	  //--------------------
                                                                                                                          		    	      	  //Put TF * IDF weights
                                                                                                                          		    	      	  //--------------------
                                                                                                                          		    	      	 
                                                                                                                          		    	      	if(isIDF) 
                                                                                                                        		    	      	    mapTFIDF_T.put(word, tf * idf);     	   
                                                                                                                        		    	        else
                                                                                                                        		    	        	mapTFIDF_T.put(word, tf * 1);
                                                                                                                          		    	      	  
                                                                                                                          	    	      	  } //end of checking all words in a movie
                                                                                                                          	    	      	  
                                                                                                                          	    	 			movieToRatingsFinal.put(i, mapTFIDF_T);
                                                                                                                          	    	      	//System.out.println("size tags=" + mapTFIDF.size());
                                                                                                                          	    	      
                                                                                                                             	  
                                                                                                                               } //end of if  
                                                                                                              
                                                                                                                              
                                                                                                                              //---------------------------------------------------------------------------
                                                                                                                              // Aka titles    

                                                                                                                                              if(movieToAkaTitles.containsKey(i))	        		
                                                                                                                                               {                               	                                 	      	  	  
                                                                                                                                            	  Map <String,Double>  mapTFIDF_T = new HashMap<String, Double>();
                                                                                                                                         		 
                                                                                                                                          		  idf   = 0;  //It will store idf of each word
                                                                                                                                              	  tf	= 0;	    	      	  
                                                                                                                                              	  word  = "";
                                                                                                                                              	      	  	  
                                                                                                                                              	   mapTF = (Map<String, Double>) movieToAkaTitles.get(i);
                                                                                                                                              	   mapTFIDF_T = (Map<String, Double>) movieToAkaTitles.get(i);
                                                                                                                                          	       
                                                                                                                                              	   set = mapTF.entrySet();	    	      	       	  
                                                                                                                                          	       Iterator j = set.iterator();
                                                                                                                                          	    	      	  	    	      	  
                                                                                                                                          	    	 while(j.hasNext()) 
                                                                                                                                          	    	{
                                                                                                                                          	    	    Map.Entry words = (Map.Entry)j.next();	    	      	  
                                                                                                                                          	    	      		 
                                                                                                                                          	    	       word = (String)words.getKey();			//get the word
                                                                                                                                          	    	       tf   = (Double)words.getValue();		//get the tf value
                                                                                                                                          	    	       df   = 1;
                                                                                                                                          	    	      		  
                                                                                                                                          		    	      	  for(int k=1; k<totalMovies;k++) //check in each movie's list
                                                                                                                                          		    	      	  {
                                                                                                                                          			    	      		if (k!=i) // not the same movie we are dealing with
                                                                                                                                          			    	      			tempMap =  (Map<String, Double>) movieToAkaTitles.get(k);
                                                                                                                                          			    	      		
                                                                                                                                          			    	      		if(tempMap!=null && tempMap.containsKey(word)) //if this movie has words 
                                                                                                                                          			    	      		{
                                                                                                                                          			    	      			df += 1;		//This doc contains the word
                                                                                                                                          			    	      		}
                                                                                                                                          			    	      		    	      		  
                                                                                                                                          		    	      	  } //end of all movies
                                                                                                                                          		    	      	  
                                                                                                                                          		    	      	  //Here add this DF into a list etc
                                                                                                                                          		    	      	  
                                                                                                                                          		    	      	  //------------------
                                                                                                                                          		    	      	  //IDF = lod (N/df)
                                                                                                                                          		    	      	  //------------------
                                                                                                                                          		    	      	  
                                                                                                                                          		    	      	  idf = Math.log(totalMovies/(df*1.0));
                                                                                                                                          		    	      	 
                                                                                                                                          		    	      	  //--------------------
                                                                                                                                          		    	      	  //Put TF * IDF weights
                                                                                                                                          		    	      	  //--------------------
                                                                                                                                          		    	      	 
                                                                                                                                          		    	      	if(isIDF) 
                                                                                                                                        		    	      	    mapTFIDF_T.put(word, tf * idf);     	   
                                                                                                                                        		    	        else
                                                                                                                                        		    	        	mapTFIDF_T.put(word, tf * 1);
                                                                                                                                          		    	      	  
                                                                                                                                          	    	      	  } //end of checking all words in a movie
                                                                                                                                          	    	      	  
                                                                                                                                          	    	 			movieToAkaTitlesFinal.put(i, mapTFIDF_T);
                                                                                                                                          	    	      	//System.out.println("size tags=" + mapTFIDF.size());
                                                                                                                                          	    	      
                                                                                                                                             	  
                                                                                                                                               } //end of if  
                                                                                                                              
                                                                                                              
              // --------------------------------------------------------------------------
        	  // All features

                      	         	  
        	  if(movieToFeatures.containsKey(i))	        		
        	   	{
        		  Map <String,Double>  mapTFIDF_T = new HashMap<String, Double>();
        		  
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
        	    	      		  
        		         	 for(int k=1; k<totalMovies;k++) //check in each movie's list
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
        		    	      	  
        		    	      	  idf = Math.log(totalMovies *1.0/df);
        		    	      	  
        		    	      	  /*  System.out.println(" i ="+ i + "words="+ mapTF.size());
        		    	      	      System.out.println("tf, df, idf feature ="+ tf+ "," + df + ", " + idf);
        		    	      	      System.out.println("log(1)" + Math.log(1));
        		    	      	  */
        		    	      	//  if(idf >0)  System.out.println("idf = " + idf);
        		    	      	
        		    	      	  //--------------------
        		    	      	  //Put TF * IDF weights
        		    	      	  //--------------------
        		    	      	 
        		    	      	if(isIDF) 
        		    	      	    mapTFIDF_T.put(word, tf * idf);     	   
        		    	        else
        		    	        	mapTFIDF_T.put(word, tf * 1);
        		    	      	  
        	    	      	  } //end of checking all words in a movie
        	    	      	  
        	    	 //-------------------------------------
        	    	 // Write movies Final
        	    	 //--------------------------------------
        	    	    
        	    	 movieToFeaturesFinal.put(i, mapTFIDF_T);      	    	 
        	    	         	    	 
        	    	 //System.out.println("size features=" + mapTFIDF.size());
        	    	      	  
        	        	} //end of if  	  
        }//end of all movies		
        
       
        
} //end of function

    
/**********************************************************************************************/
	/**
	 *  Verify the weights
	 *  
	 */        

	public void verifyWeights(OpenIntObjectHashMap MymovieToMyFeaturesFinal)
	{
		  Map<String, Double> temp1;
		  Map<String, Double> temp2;
		  
        for (int i=1;i<totalMovies; i++)
        {
        	
        if(MymovieToMyFeaturesFinal.containsKey(i))
        {
        	  temp1 = ( Map<String, Double>) MymovieToMyFeaturesFinal.get(i);  	
        	  Set setActive = temp1.entrySet();      	  
           	  Iterator jActive = setActive.iterator();           	  
           	  
           	while(jActive.hasNext()) 
         	 {
         	     Map.Entry words = (Map.Entry)jActive.next();  		 
         	     String word = (String)words.getKey();			//get the word

         	    for(int k=1; k<totalMovies;k++) 				//check in each movie's list
         	    { 
         		
	        	  if(k!=i && MymovieToMyFeaturesFinal.containsKey(k))
	        	  {
		        	  temp2 = ( Map<String, Double>) MymovieToMyFeaturesFinal.get(k);
		        	  
		         	    if(temp2.containsKey(word))
		         	    {
		         	    	 double w1= temp1.get(word);
		        			 double w2= temp2.get(word);
		        			 if (w1!=w2) System.out.println(w1 + "," +w2);
		         	    
		         	    } 
      		    	
         	    } //end while
      	     } //end of all movies
         	    
      	  } //end while
        }//end if
       } //end of outer for
     }
	
/**********************************************************************************************/
	
	/**
	 *  Prints the weights
	 *  
	 */        

	public void printWeights(OpenIntObjectHashMap MymovieToMyFeaturesFinal)
	{
		  Map<String, Double> temp1;
		  Map<String, Double> temp2;
		  
        for (int i=1;i<totalMovies; i++)
        {
        	
        if(MymovieToMyFeaturesFinal.containsKey(i))
        {
        	  temp1 = ( Map<String, Double>) MymovieToMyFeaturesFinal.get(i);  	
        	  Set setActive = temp1.entrySet();      	  
           	  Iterator jActive = setActive.iterator();           	  
           	  
           	while(jActive.hasNext()) 
         	 {
         	     Map.Entry words = (Map.Entry)jActive.next();  		 
         	     String word = (String)words.getKey();			//get the word
         	     System.out.print(word + ",");
      	  } //end while
            System.out.println();
        }//end if
       } //end of outer for
     }
        
/************************************************************************************************/
/******************************************************************************************************/
		
	/**
	 * return the imdbId associated with a movielens Id
	 * @param movielens mid
	 * @return imdb mid
	 */
			
		public int getIMDBId (int mid)		
		{
				int imdbId = 0;
				
			try		
			{
				Statement stmt = conML.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT movieid FROM " + ratingsNameML + " " 
										        + "WHERE itemid= " + mid + ";");
		
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

		/**
		 *  Give the IntArrayList of the movies, that are not exactly matched in the movielens
		 *  and imdb (Exact title matching)  
		 */	
		
		public IntArrayList getMoviesNotExactlyMatched ()
		{
			IntArrayList nonMachingMovies = new IntArrayList();
			int movieID;
			
			try		
			{
				Statement stmt = conML.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT itemid FROM movies INNER JOIN usr_ml_items AS ml ON ml.movieid=movies.movieid " +
												"WHERE movies.title <> itemtitle ORDER BY movies.title" + ";"); 
				
		
					   while(rs.next())
					   {
						   movieID= rs.getInt(1);
						   nonMachingMovies.add(movieID);
						   break;
					   }
					   
					   stmt.close();
			}
			
			catch(SQLException e){
				e.printStackTrace();
				e.getMessage();
				} 
			
			
			
			return nonMachingMovies;			
			
			
		}
		
		
/******************************************************************************************************/		
/******************************************************************************************************/
// Main Mathod

		
		/**
		 * Main method
		 */
			public static void main(String[] args)		
			{
				IMDBFeatureReader rw= new IMDBFeatureReader();			
				rw.getAllData();
				
			   
				   
			}
			
/******************************************************************************************************/
/******************************************************************************************************/		

			
		// This method is called from other class, as I think, Java can not read class having database objects
		// The other class will get its hashes and then will write them into memory 	
		// Just deserialize the written object from anu file and call ites getKeywordsFeature(), and similar methods
		 	
		   public void getFeatures()		
		   {
					
				getAllData();
				
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
			  	
			  	openConnection("jmdb");			
				totalMovies = totalMoviesML;
				
				//get imdb id against a movielens id
				for(int i=1;i<=totalMovies;i++) 	//all movies in movielens (1682)
				{
					myId = getIMDBId(i);
					myIdMap.put(i, myId);
					
				} 	

				//get movies not exactlt matched in the movielens and imdb
				moviesWithOutAMatch = getMoviesNotExactlyMatched();
				
				
				// GO and Find Info against each movie
				for(int i=1;i<=totalMovies;i++) 	
				{
					//------------------------------------
					// Filter movies not matched exactly
					//------------------------------------
					
					// We can do filtering from the the Naive Bayes Recommender file from the
					// IntArrayList whcih contains movies not exactly matched
					
				//	if(!(moviesWithOutAMatch.contains(i)))
					{
						myId = myIdMap.get(i);
						 
						//get keywords etc against a movie (If Features are NULL for a movie, then myId=0)
						if(myId!=0)		
							getInfo(myId, i);
						
						 if (i>=100 && i%100==0) System.out.println(i);
						 
					} //This filter all the movies not matching exactly
					
				}
				
				closeConnection(conML);
				
	/*			//---------------------------------
				//open FT connection
			    //---------------------------------
			  	
			  	openConnection("ft");			
				totalMovies = totalMoviesFT;
				
				for(int i=1;i<=totalMovies;i++) 	//all movies in movielens (1682)
				{
					
					//get imdb id against a movielens id
					myId = getIMDBId(i);
					myIdMap.put(i, myId);
					
				} 	
					closeConnection(conFT);

	*/			
				//Check IF Keywords has been written?
				/*for(int i=1;i<1600;i++)
					System.out.println(dumMovieToKeywords.get(i));
				*/
				//------------------------------------
				//Here we can do stemming TF-IDF 
				//------------------------------------
				
				doStemming();
				System.out.println(" Finished writing stemmed tokens");
			
				//verifyTF();
				
				calculateIDF();
				System.out.println(" Finished doing TF-IDF");
				
				//verifyWeights();
			//	System.out.println(" Actors");
			//	printWeights(movieToActorsFinal);
			//	System.out.println(" Keywords");
			//	printWeights(movieToKeywordsFinal);
										
				
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
			

			/**
			 * @return OpenIntObjectHashMap of tags 
			 */
			
			public OpenIntObjectHashMap getTagsFeatures()
			{
				//return movieToTags;
				return movieToTagsFinal;
			}
			
			/**
			 * @return OpenIntObjectHashMap of features 
			 */
			
			public OpenIntObjectHashMap getAllFeatures()
			{
				//return movieToFeatures;
				return movieToFeaturesFinal;
			}
			
			
			/**
			 * @return OpenIntObjectHashMap of features 
			 */
			
			public OpenIntObjectHashMap getRatingsFeatures()
			{
				//return movieToFeatures;
				return movieToRatingsFinal;
			}
			
			/**
			 * @return OpenIntObjectHashMap of features 
			 */
			
			public OpenIntObjectHashMap getVotesFeatures()
			{
				//return movieToFeatures;
				return movieToVotesFinal;
			}
			
			/**
			 * @return OpenIntObjectHashMap of features 
			 */
			
			public OpenIntObjectHashMap getCertificatesFeatures()
			{
				//return movieToFeatures;
				return movieToCertificatesFinal;
			}
			
			/**
			 * @return OpenIntObjectHashMap of features 
			 */
			
			public OpenIntObjectHashMap getPrintedReviewsFeatures()
			{
				//return movieToFeatures;
				return movieToPrintedReviewsFinal;
			}
			
			/**
			 * @return OpenIntObjectHashMap of features 
			 */
			
			public OpenIntObjectHashMap getPlotsFeatures()
			{
				//return movieToFeatures;
				return movieToPlotsFinal;
			}
			
			/**
			 * @return OpenIntObjectHashMap of features 
			 */
			
			public OpenIntObjectHashMap getBiographyFeatures()
			{
				//return movieToFeatures;
				return movieToBiographyFinal;
			}
			
			/**
			 * @return OpenIntObjectHashMap of features 
			 */
			
			public OpenIntObjectHashMap getColorsFeatures()
			{
				//return movieToFeatures;
				return movieToColorsFinal;
			}
			
			/**
			 * @return OpenIntObjectHashMap of features 
			 */
			
			public OpenIntObjectHashMap getLanguagesFeatures()
			{
				//return movieToFeatures;
				return movieToLanguagesFinal;
			}
			
			public OpenIntObjectHashMap getActorsFeatures()
			{
				//return movieToFeatures;
				return movieToBiographyFinal;
			}
			
			/**
			 * @return OpenIntObjectHashMap of features 
			 */
			
			public OpenIntObjectHashMap getDirectorsFeatures()
			{
				//return movieToFeatures;
				return movieToColorsFinal;
			}
			
			/**
			 * @return OpenIntObjectHashMap of features 
			 */
			
			public OpenIntObjectHashMap getProducersFeatures()
			{
				//return movieToFeatures;
				return movieToLanguagesFinal;
			}
			
			
			/**
			 * @return OpenIntObjectHashMap of features 
			 */
			
			public OpenIntObjectHashMap getGenresFeatures()
			{
				//return movieToFeatures;
				return movieToGenresFinal;
			}
			
						
			/**
			 * 
			 * @return All UnStemmed Features, raw ones
			 */
			
			public OpenIntObjectHashMap getAllUnStemmedFeatures()
			{
				    return movieToAllUnStemmedFeaturesFinal;
			}
			
			/**
			 * 
			 * @return Movies id not exactly matched
			 */
			
			public IntArrayList getNonMatchingMovies()
			{
				    return moviesWithOutAMatch;
			}
			
}
