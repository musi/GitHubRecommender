package netflix.memreader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import cern.colt.map.OpenIntObjectHashMap;

public class ReadAndWriteGenre10M
{
	
	OpenIntObjectHashMap   midToGenre;
    Map <String,Integer>   genreMap;	
	String 				   fileNameToRead;
	String 				   myPath;	
	BufferedWriter     	   writeData;
	int					   totalOutputLines;
	
	
	
	public ReadAndWriteGenre10M ()
	{
		midToGenre 		 = new OpenIntObjectHashMap();
		genreMap   		 = new HashMap<String, Integer> ();
		fileNameToRead   = "C:/Users/Musi/Desktop/movie_ml_5/movie_ml/ml_data_10M/movies.dat";
		myPath 			 = "C:/Users/Musi/Desktop/movie_ml_5/movie_ml/ml_data_10M/";
		totalOutputLines = 0;
		
	}
	
	//-----------------------------------------------------
	/**
	 * Read Genre file and parse different values
	 */
	
	 public void readData(String fileName)    
	  {
		 
		 	//open the file to write genres
		 	openFile ();
		 	
		 	//call genre map
		 	genreMap();
		 
	        try         
	        {         

	        	 FileInputStream fstream = new FileInputStream(fileName);
	        	    
	        	// Get the object of DataInputStream
	        	  DataInputStream IN = new DataInputStream(fstream);
	        	  BufferedReader br = new BufferedReader(new InputStreamReader(IN));
	        	  String strLine;
	        	    
	        	       
		            String[] 	line = {""};
		            String[]    parsedGenres = {""};
		            String      movie   = "";
		            String      genres  = "";	           
		            int 		mid = 0;
		            int			previousMid = 0;
		            int         lineCounter = 0;
		            int 		movieCounter =0;
		        		      
		           
		           
		            while ((strLine = br.readLine()) != null)                 
		            {			       
			            movie   = "";
				        genres  = "";
				        mid 	= 0;	
	         
				   		line     = strLine.split("::");		//delimiter
				   	    	                
			
				   	    mid 		 = Integer.parseInt(line[0]);				               
			            movie 		 = line[1];
				        genres    	 = line[2];
			
				        if(mid!=previousMid)
				        		movieCounter++;
				        
				        parsedGenres = genres.split("\\|");				       
			            System.out.println(mid+ "::" + movie + "::"+  genres);
			            lineCounter++;
			            			            
			            //write results back into the file
			            writeIntoAFile(mid,parsedGenres);        
			          
			            previousMid = mid;
			            
			            //break;
		             }
		            
		            //close file		            
		            closeFile();
		            
		            System.out.println("lines="+lineCounter);
		            System.out.println("movies="+movieCounter);
		            System.out.println("output lines="+totalOutputLines);
		            
		            
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
	 
	 //--------------------------------------------------
	 /**
	  * Map the Genre from string to an int value  
	  */
	 
	 public void genreMap()
	 {		 
		    genreMap.put("Action",1);
		    genreMap.put("Adventure",2);
		    genreMap.put("Animation",3);
		    genreMap.put("Children",4);
		    genreMap.put("Comedy",5);
		    genreMap.put("Crime",6);
		    genreMap.put("Documentary",7);
		    genreMap.put("Drama",8);
		    genreMap.put("Fantasy",9);
		    genreMap.put("Film-Noir",10);
		    genreMap.put("Horror",11);
		    genreMap.put("Musical",12);
		    genreMap.put("Mystery",13);
		    genreMap.put("Romance",14);
		    genreMap.put("Sci-Fi",15);
		    genreMap.put("Thriller",16);
		    genreMap.put("War",17);
		    genreMap.put("Western",18);
		    genreMap.put("IMAX",19);   
		 
	 }

	 //------------------------------------------------------------------
	 /**
	  *  
	  */
	 
	 public void writeIntoAFile(int mid, String[] genres)
	 {		 
		int genreLength = genres.length;		
		
		try
		{	
			if(genres!=null)
			{
				for(int i=0;i<genreLength;i++)		
			    {
					System.out.println(genres[i]);				
					int g = genreMap.get(genres[i]);
					writeData.write(mid+","+g);
					writeData.newLine();	
					totalOutputLines++;
				}
			}
			
			//for the movie, which dont have any movie, put zero
			else{
				writeData.write(mid+","+0);
				writeData.newLine();
				totalOutputLines++;
			}
				
		}		
		catch (Exception E)
		{
			E.printStackTrace();
			System.out.println("Error in writing");
		}
	 }
	 
	 //---------------------------------------------------------
	    public void openFile()    
	    {

	   	 try {
	   		   //sml
	   		   writeData  = new BufferedWriter(new FileWriter(myPath + "ml_Genres1.dat", true)); 
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
	   		 	writeData.close();	   		 	
	   		 	System.out.println("Files closed");
	   		  }
	   	     
	        catch (Exception E)
	        {
	      	  System.out.println("error closing the roc file pointer");
	        }
	        
	        
	    }
	    
	   
  // ------------------------------------------------------------------
	   
	 public void verifyGenreIntegrity(String fileName)
	 {
		
		 fileName = myPath + fileName;		  
		 int counterForLine = 0 ;
		
		 try {
				 FileInputStream fstream = new FileInputStream(fileName);
		 	    
		     	// Get the object of DataInputStream
		     	  DataInputStream IN = new DataInputStream(fstream);
		     	  BufferedReader br = new BufferedReader(new InputStreamReader(IN));
		     	  String strLine;
		     	  
		     	  while ((strLine = br.readLine()) != null)                 
			      {			       
		     		 counterForLine++;
		     		 
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
		 
		  System.out.println("lines found are="+ counterForLine);
	        
	 } 
	 
	 //------------------------------------------------------------------
	 
	 public static void main(String args[])  
	  {
		 ReadAndWriteGenre10M rw = new ReadAndWriteGenre10M ();		 
		 //rw.readData(rw.fileNameToRead);
		 rw.verifyGenreIntegrity("ml_Genres.dat");
		 
	  }
}
