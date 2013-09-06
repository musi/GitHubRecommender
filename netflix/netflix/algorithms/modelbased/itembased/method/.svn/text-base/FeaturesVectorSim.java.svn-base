package netflix.algorithms.modelbased.itembased.method;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import netflix.memreader.MemHelper;
import netflix.algorithms.modelbased.reader.DataReader;
import netflix.algorithms.modelbased.reader.DataReaderFromMem;
import netflix.utilities.Pair;
import cern.colt.list.IntArrayList;
import cern.colt.list.LongArrayList;
import cern.colt.list.ObjectArrayList;
import weka.core.Instances;
import weka.classifiers.meta.FilteredClassifier;
import weka.filters.unsupervised.attribute.StringToWordVector;
import weka.classifiers.bayes.NaiveBayes;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;

public class FeaturesVectorSim implements SimilarityMethod 
{
	double DF_THRESHOLD;
	
/******************************************************************************************************/
	//total keywords = vraiables , lenght of vector
	
	// I think we may have to do some sort of stemming...as keywords can have S at then end, etc
	
	/**
	 * @author Musi
	 * @param  dataReader object, active movie, target movie
	 * @return  demographic sim between two mobvies 
	 */
	
    public double findSimilarity(DataReader dataReader, int mid1, int mid2, int version)    
    {
    	
		 //create object of text handling for stemming etc
		TextHandling TH= new TextHandling();
		double bottomActive = 0, bottomTarget = 0, bottom=0;
        double top = 0;
        double weight = 0;  
        String kActive="",kTarget="";  
        int match = 0;
        HashMap<String,Double> keywordsActive = null; 
        HashMap<String,Double> keywordsTarget = null; 
         
        //Send version =1 , if want similarity  between keywords 
        if (version ==1)
        {
	       keywordsActive = dataReader.getKeywords(mid1);
	       keywordsTarget = dataReader.getKeywords(mid2);
        }
        
        //version = 2, for tags similarity 
        else if(version ==2)
        {
           keywordsActive = dataReader.getTags(mid1);
   	       keywordsTarget = dataReader.getTags(mid2);
        }
        
        //All Features
        else         
        {
            keywordsActive = dataReader.getFeatures(mid1);
    	    keywordsTarget = dataReader.getFeatures(mid2);
         }
         
        
        int  sizeActive = keywordsActive.size();
        int  sizeTarget = keywordsTarget.size();
        
   //----------------------------------------------------------
   // Now take only highly weighted words: DF Thresholding
   //----------------------------------------------------------   
           
       /* keywordsActive = doDFThresholding(dataReader, keywordsActive);
        keywordsTarget = doDFThresholding(dataReader, keywordsTarget);
        */
     
     if(sizeActive!=0 && sizeTarget!=0)
      {  
    	  Set setActive = keywordsActive.entrySet();	   	  
    	  Iterator jActive = setActive.iterator();
    	      	  	    	      	  
      	while(jActive.hasNext()) 
      	 {
      	     Map.Entry words = (Map.Entry)jActive.next();		 
      	    String word = (String)words.getKey();			//get the word
      		
      	    if(keywordsTarget.containsKey(word))
      	    {	
        	 		 
        			 match++;
        			// boolean wordOK = checkDFThresholding(word, dataReader);
        			 
        			// if(wordOK)
        			 {
	        			 double w1= keywordsActive.get(word);
	        			 double w2= keywordsTarget.get(word);
	        			 
	        			 top +=  (w1) *(w2);
	        			 bottomActive += Math.pow(w1,2);				// we should square it first (but as it contains only '1', so no need)
	        			 bottomTarget += Math.pow(w2,2); 
        			 }
        			 
        			 
        			 
        		 }
        
          } //end of while
       }//to avoid null (if)
      
        if(match!=0) //there is atleast one commonality
        {
	        bottomTarget = Math.sqrt(bottomTarget);
	        bottomActive = Math.sqrt(bottomActive);     
	        bottom 		 = bottomTarget * bottomActive;
	          
        }
        
       // if(weight==Double.NaN) System.out.println("nan"); 
        
        if(bottom ==0 ) return 0;  
        
        //----------------------------------------------
        //  Common words = 10, or 5, or others check it
        //----------------------------------------------
        
        //To Avoid a lot of 1's(At least 10 words match
        weight = top *1.0/bottom;
        weight = weight *( match *1.0/5.0);
     
      //  if(weight !=1.0) System.out.println("weight =" + weight);  //now there are weight >0 
        
        if(weight >0 || weight <0) return weight;	// To Avoid NAN        
        return 0;
        
     }

/******************************************************************************************************/
    
   //defualt methods to be implemented
    public double findUserSimilarity(DataReader dataReader, int uid1, int uid2)   
    {
      return 0;	
    }
    
    public void setNumMinMovies(int numMinMovies) 
    {
    	
    }
    
    public void setNumMinUsers(int numMinMovies) 
    {
    	
    }

    
 /******************************************************************************************************/
 
    /**
     * Do DF thresholding
     */
    
    public HashMap <String,Double> doDFThresholding(DataReader dataReader,
    												HashMap<String,Double> movieFeatures)
    {
   
    	ObjectArrayList removeTheseWords = new ObjectArrayList();
    		   
    		   	//proceed if movie size is not zero
    		   	if(movieFeatures.size() !=0)
    		   	{
    	          		  //Get entry sets for train class vector  	      	       	  
    	             	  Set setTrainClass = movieFeatures.entrySet();               	  
    	             	  Iterator jTrainClass = setTrainClass.iterator();
    	               	               	              	  
    	             	  //Iterate over the words of Test set until all are processed
    	 	              	while(jTrainClass.hasNext()) 
    	 	              	 {
    	 	              	     Map.Entry words = (Map.Entry)jTrainClass.next();         // Next 		 
    	 	              	     String word 	 = (String)words.getKey();			      // Get a word from the train class
    	 	              	      	              	     
    	 	              	     // Check in how many movies this word occures
    	 	              	       boolean word_OK = checkDFThresholding(word, dataReader);
    	 	              	       
    	 	              	       if(word_OK == false) 
    	 	              	    	   removeTheseWords.add(word); 	              	    	   
    	 	              	     	
    	 	              	  } //end of while	              	
    	            }//end of if	   

    		   
    				   //------------------------
    				   //Remove the words
    				   //------------------------
    				   
    				   int size = removeTheseWords.size();
    				   
    				   for (int i=0;i<size; i++)
    				   {
    					   //get a word to be removed
    					   String oneWordToBeRemoved = (String)removeTheseWords.get(i);
    					   movieFeatures.remove(oneWordToBeRemoved);

    				   } //end of for  
    	   
    	  return movieFeatures;

   }
    	
  //----------------------------------------------------------------------------------------------

  public boolean checkDFThresholding(String word, DataReader dataReader)
  { 
	  
	  IntArrayList totalMovies = dataReader.getListOfMovies();
  	  int moviesSize = totalMovies.size();
         
        //Define DF threshold per user's rating
        DF_THRESHOLD = (int)(moviesSize * (0.25));
        
        //how many times this word occures across the doc
        int count =0;
  	  		   
  	  //For all movies
  	  for (int i=0;i<moviesSize;i++)
  	   {   		       	   
  		     int mid = totalMovies.getQuick(i);
  		     
  	       	 //Get a training feature for this movie
  	       	 HashMap<String, Double>FeaturesAgainstAMovie  = dataReader.getFeatures(mid);
  	         
  	       	 //check for match
  	       	 if (FeaturesAgainstAMovie !=null) 
  	       	 {	          
  	       		 if(FeaturesAgainstAMovie.containsKey(word))
  	       			 count++;   			 
  	       		 
  	       	 } //end of if
  	       	 
  	       	
  	    } //end of for
  	  
  	  //Break if this word occures in pre-defined movies in the train set
      	 // and return true as well
  	  		if(count>=DF_THRESHOLD  && count <moviesSize-5 )
  	  			return true;
  	 
  	   //else we return false
  	  		return false;
  	   
     }
     

     
//---------------------------------------------------------------------------------------------------    
    
}
