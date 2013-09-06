package netflix.memreader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


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

import cern.colt.list.ObjectArrayList;
import cern.colt.map.OpenIntObjectHashMap;

// this class reads tags (tags are provided by a user for many movies)
// I will read the tags and then will calculate the TF IDF values for them
// I will write this data into a csv, and will use in the MMR program
// FOR MMR
// 1) SML 2) ML(10M) 3) MyExp

 public class tagReaderFrom10M 
 {
	 	OpenIntObjectHashMap   midToTags_dum;					// raw tags
	 	OpenIntObjectHashMap   midToTags;						// stemmed version
	 	OpenIntObjectHashMap   midToTags_Final;					// stemmed, TF/IDF values version	
	 	String 				   fileNameToRead;
	 	String 				   myPath;	
	 	BufferedWriter     	   myWriter;
	 	int					   totalMovies;  
	 	
	 	//For making the dictionay of the features
	    HashMap <String,Double> myDictionary;
	    HashMap <String,Double> myDictionary_Max;			//Max value of a feature in all movies
	    HashMap <String,Double> myDictionary_Min;			//Min value of a feature in all movies
	    HashMap <String,Double> myDictionary_maxMinusMin;   //denomenator of normalisation
	    
	    
	 	
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
	    
	    
	    /**
	     *  Public contructor
	     */
	 	public tagReaderFrom10M ()
	 	{	 	
	 		totalMovies     = 10681;
	 		midToTags_dum 	= new OpenIntObjectHashMap();
	 		midToTags 		= new OpenIntObjectHashMap();
	 		midToTags_Final	= new OpenIntObjectHashMap();
	 		
	 		fileNameToRead  = "C:/Users/Musi/Desktop/movie_ml_5/movie_ml/ml_data_10M/tags.dat";
	 		myPath 			= "C:/Users/Musi/Desktop/movie_ml_5/movie_ml/ml_data_10M/";
	 		
	 	     //Dictionary
	         myDictionary 				= new HashMap <String, Double>();
	         myDictionary_Max 			= new HashMap <String, Double>();
	         myDictionary_Min 			= new HashMap <String, Double>();
	         myDictionary_maxMinusMin 	= new HashMap <String, Double>();
		
	 	
	 	}
	 	
/********************************************************************************************************************************/
	 	
	 	/**
	 	 * Read Genre file and parse different values
	 	 */
	 	 public void readData(String fileName)    
	 	  {
	 		 
	 		 	//open the file to write genres
	 		 	openFile ();	 		 	
	 		 
	 		 
	 	        try         
	 	        {
	 	        	 FileInputStream fstream = new FileInputStream(fileName);
	 	        	    
	 	        	// Get the object of DataInputStream
	 	        	  DataInputStream IN = new DataInputStream(fstream);
	 	        	  BufferedReader br = new BufferedReader(new InputStreamReader(IN));
	 	        	  String strLine;
	 	        	    
	 	        	       
	 		            String[] 		line 		 = {""};
	 		            String[]    	parsedGenres = {""};
	 		            String			tag   	 	 = "";
	 		            String      	dated 	 	 = "";
	 		            int 			mid 	 	 = 0;
	 		            int 			uid		 	 = 0;
	 		            int         	lineCounter  = 0; 
	 		            ObjectArrayList list;
	 		           
	 		            while ((strLine = br.readLine()) != null)                 
	 		            {			       
	 			            tag     = "";
	 			            dated   = "";
	 				        mid 	= 0;
	 				        uid 	= 0;
	 	         
	 				   		line     = strLine.split("::");		//delimiter
	 			
	 				   	    uid 		 = Integer.parseInt(line[0]);
	 				   	    mid 		 = Integer.parseInt(line[1]);				        
	 			            tag 		 = line[2];
	 				        dated    	 = line[3];
	 			
	 				       // Put the data into a hash map
	 				       // At this point, we are simply putting them down, then we may calculate the TF/IDF	 				         
	 				       if(midToTags_dum.containsKey(mid))	        		
	 			            {
	 				    	   list = (ObjectArrayList) midToTags_dum.get(mid);      	
	 			        	}	 			        	
	 			        	else
	 			        	{
	 			        		list = new ObjectArrayList();	
	 			        	}
	 			        	
	 			        	list.add(tag);
	 			        	midToTags_dum.put(mid, list);
	 				 		     
	 			            	 			            
	 			            //write results back into the file
	 			            //writeIntoAFile(mid,parsedGenres);        
	 			          
	 			            //break;
	 		             }
	 		            
	 		            //Now check if the tags have been written against each movie and how much movies are there
	 		            checkForTagsIntegrity();

	 		            //stemming part
	 		            System.out.println("Doing Stemming...");
	 		            doStemming();
	 		            
	 		            //IDF
	 		            System.out.println("Calculating IDF...");
	 		            calculateIDF();
	 		           
	 		            //Dictionary
	 		            System.out.println("Making dictionary...");
	 		            getAllKeywordAndMakeMatrix();
	 		           
	 		            //Nor weights
	 		            System.out.println("Calculating Normalized TF IDF...");
	 		            prepare_TFIDF_Nor_Values(totalMovies);
	 		                  
	 		            
	 		            //close file		            
	 		            closeFile();
	 		            
	 		        }
	 	        
	 	        catch(FileNotFoundException e) {
	 	            System.out.println("Can't find file " + fileName);
	 	            e.printStackTrace();

	 	        }
	 	        catch(IOException e) {
	 	            System.out.println("IO error");
	 	            e.printStackTrace();
	 	        }
	 	        
	 	        System.out.println("OUT!");
	 	    }


	
	 	 
	 	 //---------------------------------------------------------
	 	/**
	 	 * Check if the tags ve been written against each movie 
	 	 */
	 	 
	 	//It came out that, many movies have no no tag....nearlly 3500 total = 10681, found = 7601 
	 	public void checkForTagsIntegrity()
	 	{
	 	
	 		System.out.println("total movie having some tags"+ midToTags_dum.keys().size());
	 		System.out.println("Example: A single movie ve tags like this-->"+ midToTags_dum.get(2));
	 		
	 	}
	 	
	 	
	 	 //---------------------------------------------------------
	 	    public void openFile()    
	 	    {

	 	   	 try {
	 	   		   //sml
	 	   		myWriter  = new BufferedWriter(new FileWriter(myPath + "ml_Features.csv", true)); 
	 	   	 	}	        
	 	        catch (Exception E)
	 	        {
	 	      	  System.out.println("error opening the file pointer of rec");
	 	      	  System.exit(1);
	 	        }
	 	        
	 	        
	 	    }
	 	    
	 	   //---------------------------------------------------------
	 	    

	 	   public void closeFile()    
	 	   {
	 	    
	 	   	 try {
	 	   		 	myWriter.close();	   		 	
	 	   		 	System.out.println("Files closed");
	 	   		  }
	 	   	     
	 	        catch (Exception E)
	 	        {
	 	      	  System.out.println("error closing the roc file pointer");
	 	        }
	 	        
	 	        
	 	    }
	 	    
	 	 
/********************************************************************************************************************************/
	 	  /**
	 	   * Perform Stemming for each movie features
	 	   */
	 	   
	 	  public void doStemming()
		  {

			    ObjectArrayList list	=	null;
			    int type				=   0;			    
			    
			    for (int mid=1;mid<=totalMovies;mid++)
			    {

				    //-----------------------------------------------------
				    // Tags
				    //-----------------------------------------------------		    	
			    	
			    	if(	midToTags_dum.containsKey(mid))	        		
			       	{
			       	  list  		= (ObjectArrayList) midToTags_dum.get(mid);   	
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
											 writeTokenInMovieFeature(mid, myS);
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
				    	 } //end of all elements in a list for a movie				 
			    	} // end of reading type = 10
			    	
			    }//end outer for
		  }
	 	  
/********************************************************************************************************************************/
	 		/**
	 		 *  Calculate TF * IDF weights and put (mid, hashmap(word, weight))
	 		 */
	 		
	 		public void calculateIDF()
	 		{
	 			int inc 	= 0;
	 			int df		= 1;				//to avoid divide by zero
	 			double tf   = 0;
	 			String word = "";
	 			double idf  = 0.0;	 			
	 		    int size    = 0;
	 		    Set set;
	 		    
	 		    Map <String,Double> mapTF    	= null;
	 		    Map <String,Double> tempMap  	= null;
	 		    Map <String,Double>  mapTFIDF_K = new HashMap<String, Double>();
	 		    Map <String,Double>  mapTFIDF_T = new HashMap<String, Double>();
	 		    Map <String,Double>  mapTFIDF_A = new HashMap<String, Double>();
	 		    
	 	        for(int i=1; i<=totalMovies;i++)
	 	        {
	 	        	  if(midToTags.containsKey(i))	        		
	 		        	{
	 	        		  idf   = 0;  //It will store idf of each word
	 	    	      	  tf	= 0;	    	      	  
	 	    	      	  word  = "";
	 	    	      	  	  
	 	    	      	  	  mapTF 	 = (Map<String, Double>) midToTags.get(i);    	      	  
	 	    	      	  	  mapTFIDF_K = (Map<String, Double>) midToTags.get(i);
	 	    	      	  	
	 		    	      	  set = mapTF.entrySet();	    	      	       	  
	 		    	      	  Iterator j = set.iterator();
	 		    	      	  	    	      	  
	 		    	      	while(j.hasNext()) 
	 		    	      	{
	 		    	      	     Map.Entry words = (Map.Entry)j.next();	    	      	  
	 		    	      		 
	 		    	      	     word = (String)words.getKey();			//get the word
	 		    	      		 tf   = (Double)words.getValue();		//get the tf value
	 		    	      		 df = 1;
	 		    	      		  
	 			    	      	  for(int k=1; k<=totalMovies;k++) //check in each movie's list
	 			    	      	  {
	 				    	      		if (k!=i ) // not the same movie we are dealing with
	 				    	      			tempMap =  (Map<String, Double>) midToTags.get(k);
	 				    	      		
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
	 			    	      	 
	 			    	      	    mapTFIDF_K.put(word, tf * idf); 		    	      	  
	 			    	      	    mapTFIDF_T.put(word, tf * 1);
	 			    	      	
	 		    	      	  } //end of checking all words in a movie
	 		    	      	  
	 		    	        	    midToTags_Final.put(i, mapTFIDF_T);
	 		    	      		//	midToTags_Final.put(i, mapTFIDF_K);
	 		    	      			
	 		    	      //	System.out.println("size keys=" + mapTFIDF.size());
	 		        	} //end of if
	 	        } //end outer for
	 		}
	 		
	 	        	  
	 	 
		//------------------------------------------------------------------
			  	  
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
			  	  	  
		 			
			  	 		
	/**************************************************************************************************/
			  	  
			  		   /**
			  		     *    Adds an entry to the midToTags, movieToTags etc hashtable. 
			  		     *    
			  		     *    If a movie has no keywords (for example), then simply we do not put anything.
			  		     *    When we have to retrieve movie's keywords, check it is not null first
			  		     *    
			  		     *    @param Info = keywords, or tags (string form, single)
			  		     *    @param mid = ML mid
			  		     */
			  	  
			  		  
			  		public void  writeTokenInMovieFeature (int mid, String token)
			  		{
			  		    
			  		    double inc = 1;
			  		    Map <String,Double> mapTF;
			  		    
			  		  //----------------------------------------
			  		  // Keywords
			  		  //----------------------------------------  
			  		    
			  		      	if(midToTags.containsKey(mid))	        		
			  		       	{
			  		            mapTF = (Map<String, Double>) midToTags.get(mid);
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
			  		    
			  		        	 midToTags.put(mid, mapTF);
			              }
			  
	
	//-----------------------------------------------------------------------------------		  
			  		
		/**
		 * Get the feature set against a mid	  		
		 */
			  public HashMap<String, Double> getFeaturesAgainstAMid(int mid)
			  {
			  			if(midToTags_Final.containsKey(mid))
			  					return (HashMap<String, Double>)midToTags_Final.get(mid);
			  			
			  			//return  new HashMap<String, Double>();
			  			return null;
			  }
				  
			  
			  		
  /**************************************************************************************************/
			  		
			  		/**
			  		 * We will get features from all the movies, and build a dictionary, then we will
			  		 * use this this as a check-mark, if a movie do not has a keyword (term), put zero in that
			  		 * term....similarly build this model and generate model.
			  		 */
			  		
			  	  public void getAllKeywordAndMakeMatrix()
			  	  {
			  		  System.out.println("came for building dictionary");
			  		  
			  		  int numMovies = totalMovies;
			  		  HashMap<String, Double> FeaturesMovie;
			  		  
			  		  for(int i=1;i<=numMovies;i++)			//for all movies
			  		  {
			  			  FeaturesMovie = getFeaturesAgainstAMid(i);
			  			  
			  			  //If movies has some features
			  			  if(FeaturesMovie!=null)
			  			  {
				  			  //start building the dictionary
				  			  Set mySet = FeaturesMovie.entrySet();							  // Find set and iterators  
				  			  Iterator myIterator = mySet.iterator();    	   
				  			 
				  			//  myFeatures +="\"";											  // opening string "
				  			  int total =0;
				  			  
				  			  //Iterate over the words 
				  		     	while(myIterator.hasNext()) 
				  		     	 {
				  		     	     total++; 
				  		     		 Map.Entry words = (Map.Entry)myIterator.next();        // Next 		 
				  		     	     String word 	 = (String)words.getKey();				 // Get word	    
				  		     	     double TF 		 = FeaturesMovie.get(word);          	 // Get TF   	     	
				  		    
				  		     	     if(myDictionary.containsKey(word)==false && TF>=1)	
				  		     	    	myDictionary.put(word, 1.0);			     		//We do not care abt the tf 		
				  		     	     
				  		     	 }//end of while
			  			  }
			  		    } //end for
			  		 
			  		  System.out.println(myDictionary);
			  		  System.out.println(myDictionary.size());
			  		  
			  		  //--------------------------------------------------
			  		  int featureSize = myDictionary.size();
			  		  double max = 0;
			  		  double min = 0;
			  		  double TF  = 0;	
			  		  String singleFeature ="";
			  		  
			  		  System.out.println("Num distinct words found ="+ featureSize); 

			  		  //Iterate over the words 
			  		  Set mySet = myDictionary.entrySet();			  					// Find set and iterators  
			  		  Iterator myIterator = mySet.iterator();    	   

			  		    while(myIterator.hasNext())// && total <10) 
			  	     	 {     	
			  	     		 Map.Entry words = (Map.Entry)myIterator.next(); 	      // Next 		 
			  	     		 singleFeature 	 = (String)words.getKey();				  // Get word   			  	     	     
			  	     	     
			  	     	     //reset min and max for each feature	  
			  				  max = 0;
			  				  min = 10;			 
			  				  
			  				//Find Max and Min for each feature over all the movies
			  				  for(int i=1;i<=numMovies;i++)		
			  				  {
			  					  FeaturesMovie = getFeaturesAgainstAMid(i);
			  					  
			  					  //If movies has some features
					  			  if(FeaturesMovie!=null)
					  			  {
				  					  if(FeaturesMovie.containsKey(singleFeature))
				  					  {
				  						  TF = FeaturesMovie.get(singleFeature);					  // Get TF of the word
				  						  
				  						  //update min and max
				  						  //Problem: MiN will be zero always, as a feature vector is v v sparse, so a feature will nt
				  						  //occure in most the movies, means min =0 or min = max????
				  						  if(min > TF && TF!=0)
				  							  min = TF;					  
				  						  if(max<TF)
				  							  max = TF;
				  						  
				  					  }
					  			  }
			  				  }//end inner for of movies
			  				  
			  				  //store max and min of each feature into a map
			  				  myDictionary_Max.put(singleFeature, max);
			  				  myDictionary_Min.put(singleFeature, min);
			  				  if(min==10)   //to avoid,as most of the minimas are zero
			  					  min = 0;
			  				  myDictionary_maxMinusMin.put(singleFeature,(max-min));
			  				  
			  		    } //end while
			  		  
			  	  }
			  	  
 /**************************************************************************************************/
	/**
	 * 
	 * @author Musi
	 * @param int, num of movies, having the features			  		    
	 */
			  	public void prepare_TFIDF_Nor_Values (int numMovies)
			  	{
			  		//----------------------------------------------------------
 			  	   // The data matrix consisting of the TF/IDF Normalized values of the features 
			  		    
			  		int numFeatures = myDictionary.size();
			  	    double[][] data	= null;
		            
		            //Make data Matrix
		            data  = new double[numFeatures][numMovies]; //For SVD m>=n       	
		            double TF 			= 0;
		            double norWeight    = 0;								   //After normalisation, we assign this weight
		            int    featureIndex = 0;								   //keep track of which feature
		            int    i 			= 0;								   //feature index
		            String singleFeature= "";
		                     
		            
		          //start building the dictionary
		  		  Set mySet = myDictionary.entrySet();			  // Find set and iterators  
		  		  Iterator myIterator = mySet.iterator();
		  		  HashMap<String, Double> featuresMovie;
		  		  
		       	//Here get a feature for a movie, then (1) normalize it (2) if it is not there, put
		      	//zero there (3) while normalizing check denomenator shld not be zero
		  		  
		  		  //Iterate over the words of Test set until one of them finishes
		  	     	while(myIterator.hasNext()) 
		  	     	 {
		  	     		 i = featureIndex++;  	 
		  	     		 
		  	     		 if(featureIndex>1000 && featureIndex%1000 ==0)
		  	     			 System.out.println("Has processed "+ featureIndex+" features");
		  	     		 
		  	     		 Map.Entry words  = (Map.Entry)myIterator.next();        // Next 		 
		  	     		 singleFeature	  = (String)words.getKey();				 // Get word
			            	
		  	     		 //Get min and max of this feature
		  	     		 double min  = myDictionary_Min.get(singleFeature);
		  	     		 double max  = myDictionary_Max.get(singleFeature);
		  	     		 double diff = myDictionary_maxMinusMin.get(singleFeature);
		  	     	     
			                for(int j = 0; j < numMovies; j++) 
			                {	    
			                	//reset weights
			                	norWeight = 0;
			                	TF 		  = 0;
			                	
			                	featuresMovie = getFeaturesAgainstAMid(j+1);
			                	
			                	//first check if this mov has some features
			                	if(featuresMovie!=null && featuresMovie.size()>0)
			                	{
			                		//Now check if it has feature
			                		if(featuresMovie.containsKey(singleFeature))
			                		{
			                			//get and normalise the weight
			                			TF = featuresMovie.get(singleFeature);           		 
			                				
			                			//check if denomenator is zero or not
			                			if(diff!=0)
			                				norWeight = (TF - min) / diff;
			                			else
			                				{
			                					if(max!=0)
			                						norWeight = (TF - min) / max;
			                					else
			                						norWeight = (TF - min) / 1;
			                				}
			                			
			                			/*if(TF!=0 && TF!=1){
					                		System.out.println("TF weight="+ TF);
					                		System.out.println("norWeight="+ norWeight+", min="+min+", max="+max+", diff="+diff);
			                			}*/
			                		}
			                	  } //end if
			                	
			                	//if movie has no features, then put zero
			                	else
			                	{
			                		norWeight = 0;
			                	}      	
			                	
			                /*	if(norWeight!=0 && norWeight!=1)
			                		System.out.println("nor weight="+ norWeight);*/
			                		                	
			                	data[i][j] = norWeight;
			                	
			                	//write this data into a file, where each movie is in the row and each row 
			                	// has the movie features separated by comma.
			               
			                } //end of movie for
			            } //end of feature loop
		  	     	
		  	     	System.out.println("Writing features...");
		  	     	writeMovieFeatures(data,numMovies);
		     
			  	}
			  	
		   //----------------------------------------------------------
			  	
			  	/**
			  	 * @author Musi
			  	 * @param  double[][], feature matrix (movies x features)
			  	 * @param  int, The number of rows of the data matrix (corresponds to the movies)
			  	 */
			  	
			  	 public void writeMovieFeatures(double[][] data, int rows)
			  	  {
			  		  openFile();
			  		  int cols = myDictionary.size();	  
			  		  System.out.println("Came to write features with dictionay size=" +cols);
			  		  int i =0,j =0;			//for movies, features in SML

			  		  try
			  		  {
			  			  
			  		    for(j=0;i<rows;i++)
			  		    {
			  		    	myWriter.append(""+ (i+1));
			  				 
			  				 if(i>=1000 && i%1000==0)
			  					 System.out.println("mov writing is at mov="+ i);
			  				 
			  			   for(j=0;j<cols;j++)
			  			   {	
			  					 double f = data[j][i];
			  					 
			  					 if(f!=1.0 && f!=0)
			  						 System.out.print(f+",");
			  					 myWriter.append(",");
			  					 myWriter.append(""+ f);						  		
			  			   }
			  			   
			  			  // System.out.println();
			  			   myWriter.append("\n");
			  			   
			  				} //end outer for
			  		  }  
			  			  catch (Exception E){
			  				  System.out.println("error in writing i="+i+", j="+j);
			  				  E.printStackTrace();
			  				  System.exit(1);
			  			  }

			  			  closeFile();
			  	  }
			  	 
			  	 
 		     //----------------------------------------------------------
			  		     
		 	 public static void main(String args[])  
		 	  {
		 		 tagReaderFrom10M trw = new tagReaderFrom10M ();		 
		 		 trw.readData(trw.fileNameToRead);
		 		 
		 	  }
		 	 
		 	 
	 }

		

