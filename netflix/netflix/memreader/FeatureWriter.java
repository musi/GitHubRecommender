package netflix.memreader;


import java.io.*;
import java.util.*;

import cern.colt.list.IntArrayList;
import cern.colt.list.ObjectArrayList;
import cern.colt.map.OpenIntIntHashMap;
import cern.colt.map.OpenIntObjectHashMap;

public class FeatureWriter implements Serializable 
{
		public OpenIntObjectHashMap 	movieToKeywords;  //movies, and then a list of keywords (Strings):     
	    public OpenIntObjectHashMap 	movieToTags; 	  //movies, and then a list of keywords (Strings): 
	    public OpenIntObjectHashMap 	movieToPlots; 	  //movies, and then a list of keywords (Strings): 
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
	    public OpenIntObjectHashMap 	movieToGenres; 
	    public OpenIntObjectHashMap 	movieToFeatures; 	  //movies, and then a list of keywords (Strings): 9
	    public OpenIntObjectHashMap     movieToAllUnStemmedFeatures; //all data, without stemming etc.
   
	    public IntArrayList				moviesNotmatched;	    
	    private String 					destFile;       	 //where we wanna write our dest file
	    
	    
	public FeatureWriter()
	{

		//Hash Maps
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
		 
		 movieToAllUnStemmedFeatures = new OpenIntObjectHashMap();
		 
		 moviesNotmatched			= new IntArrayList();
		 
		destFile  =  "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\sml_storedFeaturesTFOnly.dat"; //for TF only
//		 destFile  =  "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\sml_storedFeatures.dat";     // for TF-IDF
		 
	}
	

/******************************************************************************************************/
// Main Mathod
/******************************************************************************************************/
		
		/**
		 * Main method
		 */
			public static void main(String[] args)		
			{
				FeatureWriter frw		= new FeatureWriter();			
				//FeatureReader frd		= new FeatureReader();
				IMDBFeatureReader frd	= new IMDBFeatureReader();   // see this file is different
				
				frd.getAllData();

				// Get features from FeatureReader class
				frw.movieToKeywords 		= frd.getKeywordsFeatures();
				frw.movieToTags 			= frd.getTagsFeatures();
				frw.movieToFeatures 		= frd.getAllFeatures();				
				frw.movieToPlots	 		= frd.getPlotsFeatures();
				frw.movieToPrintedReviews 	= frd.getPrintedReviewsFeatures();
				frw.movieToCertificates     = frd.getCertificatesFeatures();
				frw.movieToBiography 		= frd.getBiographyFeatures();
				frw.movieToColors 			= frd.getColorsFeatures();
				frw.movieToLanguages 		= frd.getLanguagesFeatures();
				frw.movieToVotes 			= frd.getVotesFeatures();
				frw.movieToRatings 			= frd.getRatingsFeatures();
				frw.movieToActors 			= frd.getActorsFeatures();
				frw.movieToDirectors 		= frd.getDirectorsFeatures();
				frw.movieToProducers 		= frd.getProducersFeatures();		
				frw.movieToGenres 			= frd.getGenresFeatures();	
				frw.movieToAllUnStemmedFeatures = frd.getAllUnStemmedFeatures();
				
				// get movies not matched
				frw.moviesNotmatched		= frd.getNonMatchingMovies();
				
				// store above bject into memory			
				serialize(frw.destFile, frw);
				System.out.println("Done writing");
				   
			}
		
			

/******************************************************************************************************/

					/**
					 * @ return openintObjectHashMap of keywords 
					 */
					
					public OpenIntObjectHashMap getKeywordsFeatures()
					{
					//	return movieToKeywords;
						return movieToKeywords;
					}
					

					/**
					 * @return OpenIntObjectHashMap of tags 
					 */
					
					public OpenIntObjectHashMap getTagsFeatures()
					{
						//return movieToTags;
						return movieToTags;
					}
					
					/**
					 * @return OpenIntObjectHashMap of features 
					 */
					
					public OpenIntObjectHashMap getAllFeatures()
					{
						//return movieToFeatures;
						return movieToFeatures;
					}
					
					
					/**
					 * @return OpenIntObjectHashMap of features 
					 */
					
					public OpenIntObjectHashMap getAllUnStemmedFeatures()
					{
						//return movieToFeatures;
						return movieToAllUnStemmedFeatures;
					}
					
					
					/**
					 * @return OpenIntObjectHashMap of features 
					 */
					
					public OpenIntObjectHashMap getRatingsFeatures()
					{
						//return movieToFeatures;
						return movieToRatings;
					}
					
					/**
					 * @return OpenIntObjectHashMap of features 
					 */
					
					public OpenIntObjectHashMap getVotesFeatures()
					{
						//return movieToFeatures;
						return movieToVotes;
					}
					
					/**
					 * @return OpenIntObjectHashMap of features 
					 */
					
					public OpenIntObjectHashMap getCertificatesFeatures()
					{
						//return movieToFeatures;
						return movieToCertificates;
					}
					
					/**
					 * @return OpenIntObjectHashMap of features 
					 */
					
					public OpenIntObjectHashMap getPrintedReviewsFeatures()
					{
						//return movieToFeatures;
						return movieToPrintedReviews;
					}
					
					/**
					 * @return OpenIntObjectHashMap of features 
					 */
					
					public OpenIntObjectHashMap getPlotsFeatures()
					{
						//return movieToFeatures;
						return movieToPlots;
					}
					
					/**
					 * @return OpenIntObjectHashMap of features 
					 */
					
					public OpenIntObjectHashMap getBiographyFeatures()
					{
						//return movieToFeatures;
						return movieToBiography;
					}
					
					/**
					 * @return OpenIntObjectHashMap of features 
					 */
					
					public OpenIntObjectHashMap getColorsFeatures()
					{
						//return movieToFeatures;
						return movieToColors;
					}
					
					/**
					 * @return OpenIntObjectHashMap of features 
					 */
					
					public OpenIntObjectHashMap getLanguagesFeatures()
					{
					
						return movieToLanguages;
					}
					

					/**
					 * @return OpenIntObjectHashMap of features 
					 */
					
					public OpenIntObjectHashMap getActorsFeatures()
					{
						return movieToActors;
					}
					
					/**
					 * @return OpenIntObjectHashMap of features 
					 */
					
					public OpenIntObjectHashMap getDirectorsFeatures()
					{					
						return movieToDirectors;
					}
					
					/**
					 * @return OpenIntObjectHashMap of features 
					 */
					
					public OpenIntObjectHashMap getProducersFeatures()
					{
						return movieToProducers;
					}
					
					/**
					 * @return OpenIntObjectHashMap of features 
					 */
					
					public OpenIntObjectHashMap getGenresFeatures()
					{
						return movieToGenres;
					}
					
					/**
					 * 
					 * @return Movies id not exactly matched
					 */
					
					public IntArrayList getNonMatchingMovies()
					{
						    return moviesNotmatched;
					}
					
					
					
/******************************************************************************************************/
/******************************************************************************************************/
	
			
		  /**
		   *  //Serialize this object
		   *  
		   */		
			
		    public static void serialize(String fileName, FeatureWriter myObj) 	    
		    {

		        try 	        
		        {
		            FileOutputStream fos = new FileOutputStream(fileName);
		            ObjectOutputStream os = new ObjectOutputStream(fos);
		            os.writeObject(myObj);		//write the object
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

	//-----------------------------------------------------------
		     
		     public static FeatureWriter deserialize(String fileName)
		     {
		         try	         
		         {
		             FileInputStream fis    = new FileInputStream(fileName);
		             ObjectInputStream in   = new ObjectInputStream(fis);

		             return (FeatureWriter) in.readObject();	//deserilize into memReader class 
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
		     

}
