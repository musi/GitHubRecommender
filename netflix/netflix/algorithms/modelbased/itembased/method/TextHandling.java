package netflix.algorithms.modelbased.itembased.method;

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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/*****************************************************************************************************/

/**
 * Adapted from code which first appeared in a java.net article
 * written by Erik
 */
/*****************************************************************************************************/

 public class TextHandling 
{
  
	 
 int totalExamples;
	                       
  private static final String[] examples = 
  {
    "The quick brown fox jumped over the lazy dogs",
    "XY&Z Corporation - xyz@example.com",
    "This is a program, created by Musi. I tried to use simple progrmatic effort and efforts."
  };
  
  
  		public String[]   testString =
		  {
		    "The quick brown fox jumped over the lazy dogs",
		    "XY&Z Corporation - xyz@example.com",
		    "This is a program, created by Musi. I tried to use simple progrmatic effort and efforts."
		  };
		  
		  
		  //assume to be the Google's stop list, But Lucene has its own I think
		  public String[] stop_Word_List =
		  
		   { "I","a",  "about", "an",  "are",  "as",  "at",  "be",  "by",  "com",  "de", 
			 "en",  "for",  "from", "how",  "in",  "is",  "it",  "la",  "of",  "on",  "or",  "that",  "the",  
			 "this",  "to",  "was",  "what", "when",  "where",  "who",  "will",  "with",  "und",  
			 "the",  "www"};

  
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

		  
		  /*private static final String DEFAULT_STOPWORDS = 
			    "a about add ago after all also an and another any are as at be " +
			    "because been before being between big both but by came can come " +
			    "could did do does due each else end far few for from get got had " +
			    "has have he her here him himself his how if in into is it its " +
			    "just let lie like low make many me might more most much must " +
			    "my never no nor not now of off old on only or other our out over " +
			    "per pre put re said same see she should since so some still such " +
			    "take than that the their them then there these they this those " +
			    "through to too under up use very via want was way we well were " +
			    "what when where which while who will with would yes yet you your";
*/
		  
		    
  
/*****************************************************************************************************/
		  
  private static final Analyzer[] analyzers = new Analyzer[]
  {
    new WhitespaceAnalyzer(),
    new SimpleAnalyzer(),
    new StopAnalyzer(),
    new StandardAnalyzer()
  };

 /*****************************************************************************************************/
 
  //default constructror
  public TextHandling()
  {
	  totalExamples = 3;
  }

 /*****************************************************************************************************/
  
  
  public static void main(String[] args) throws IOException 
  {
	  TextHandling TA = new TextHandling();
	  
    // Use the embedded example strings, unless
    // command line arguments are specified, then use those.
    String[] strings = examples;
  
    if (args.length > 0) 
    {
      strings = args;
    }

    for (int i = 0; i < strings.length; i++) 
    {
      analyze(strings[i]);
    }
    
    //---------------------------------------------------------------------
    //lets call the method giving us stop word removed and stemmmed verison
    //---------------------------------------------------------------------
    
    TA.testStemmerAndStopWordRemoval();
     
    
  }

 /*****************************************************************************************************/
  
  private static void analyze(String text) throws IOException 
  {
    System.out.println("Analyzing \"" + text + "\"");
  
    for (int i = 0; i < analyzers.length; i++) 
    {
      Analyzer analyzer = analyzers[i];
      
      String name = analyzer.getClass().getName();
      name = name.substring(name.lastIndexOf(".") + 1);
      
      System.out.println("  " + name + ":");
      System.out.print("    ");
    
    //  AnalyzerUtils.displayTokens(analyzer, text);
      //AnalyzerUtils.displayTokensWithFullDetails(analyzer, text);
     
      System.out.println("\n");
    }
  } //end of function
 
 /*****************************************************************************************************/
 
  
  private void testStemmerAndStopWordRemoval() throws IOException 
  {
	  Token token;
	  
	  //Read string to be checked
	  for (int i = 0; i < totalExamples; i++) 
	    {
	    	Reader myReader = new StringReader(testString[i]);	    	    
	    	System.out.println("Input: "+ testString[i]);
			  
	    	  //TokenStream
			  // TokenStream ts = tokenStream (null, myReader);
			  TokenStream ts = porterStemming (null, myReader);
			  
			  //read tokens
			  token = ts.next();
			  
				  while(token!=null)
				  {
					  
					  System.out.println(token.termText());		//print token
					  token =ts.next();							//read next token
				  }
				  
			  System.out.println("\n");
	    }
	  
	  
	  
	  
	  
  } //end of function
 
 /*****************************************************************************************************/
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
  
  public TokenStream porterStemming(String fieldName, Reader reader) 
  {
      //Tokenizer tokenizer = new StandardTokenizer(reader);
      Tokenizer lowerCase = new LowerCaseTokenizer(reader);
      //TokenFilter stopFilter = new StopFilter(lowerCaseFilter, stop_Word_List);
      TokenFilter stemFilter =  new PorterStemFilter(lowerCase);
      return stemFilter;
  } 
  
/*****************************************************************************************************/

  /**
   *  retunr the lower case, stop word removal, stemmed version of the text
   *  @author Musi
   *  
   */
  
  public String stemmednText(String text)
  {
	  Reader myReader = new StringReader(text);
	  TokenStream ts = tokenStream (null, myReader);
	  
	  return "";
  }
  
  
  
  
  
  
  
  
  
  
 }

