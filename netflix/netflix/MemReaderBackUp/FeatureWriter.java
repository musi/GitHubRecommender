package netflix.memreader;


import java.io.*;
import java.util.*;

import cern.colt.list.ObjectArrayList;
import cern.colt.map.OpenIntIntHashMap;
import cern.colt.map.OpenIntObjectHashMap;

public class FeatureWriter implements Serializable 
{
	 	public OpenIntObjectHashMap 	movieToKeywords; //movies, and then a list of keywords (Strings): 10    
	    public OpenIntObjectHashMap 	movieToTags; 	  //movies, and then a list of keywords (Strings): 9
	    public OpenIntObjectHashMap 	movieToFeatures; 	  //movies, and then a list of keywords (Strings): 9
	    
	    public OpenIntObjectHashMap 	rawMovieToKeywords; //movies, and then a list of keywords (Strings): 10    
	    public OpenIntObjectHashMap 	rawMovieToTags; 	  //movies, and then a list of keywords (Strings): 9
	    
	    private String 					destFile;       //where we wanna write our dest file
	    private FeatureReader			fr;
	    
	    
	    
	    
		
	public FeatureWriter()
	{
	     movieToKeywords 			= new OpenIntObjectHashMap();
		 movieToTags	 			= new OpenIntObjectHashMap();			 
		 movieToFeatures 			= new OpenIntObjectHashMap();
		 destFile  =  "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\sml_storedFeatures.dat";
		 
	}
	

/******************************************************************************************************/
// Main Mathod
/******************************************************************************************************/
		
		/**
		 * Main method
		 */
			public static void main(String[] args)		
			{
				FeatureWriter frw= new FeatureWriter();			
				FeatureReader frd= new FeatureReader();
				
				frd.getAllData();

				// Get features from FeatureReader class
				frw.movieToKeywords 	= frd.getKeywordsFeatures();
				frw.movieToTags 		= frd.getTagsFeatures();
				frw.movieToFeatures 	= frd.getAllFeatures();

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
				return movieToKeywords;
			}
			

/******************************************************************************************************/

			/**
			 * @return OpenIntObjectHashMap of tags 
			 */
			
			public OpenIntObjectHashMap getTagsFeatures()
			{
				return movieToTags;
			}
			
/******************************************************************************************************/

			/**
			 * @return OpenIntObjectHashMap of tags 
			 */
			
			public OpenIntObjectHashMap getAllFeatures()
			{
				return movieToFeatures;
			}
			
						
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
