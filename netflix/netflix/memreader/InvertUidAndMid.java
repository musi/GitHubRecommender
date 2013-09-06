package netflix.memreader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class InvertUidAndMid 
{
	
	String 				   fileNameToRead;
	String 				   myPath;	
	BufferedWriter     	   writeData;
	 
	
	//------------------------------
	public  InvertUidAndMid()
	{	
		fileNameToRead   = "C:/Users/Musi/Desktop/movie_ml_5/movie_ml/ml_data/u.data";
		myPath 			 = "C:/Users/Musi/Desktop/movie_ml_5/movie_ml/ml_data/";
	}
	
	
	//------------------------------
	
    /**
     * Reads a text file in the form 
     *
     * mid,uid,rating
     *
     */
  
    
    public void readData()    
    {
    	//open the file to write genres
	 	openFile ();
	 	
        try         
        {

        	Scanner in = new Scanner(new File(fileNameToRead));    // read from file the movies, users, and ratings, 

            String[] 	line;
            int 		mid;
            int 		uid;         
            double      rating = 0;
       
            int         index =0;
            int         farctionOfItemsToChoose =0;
            
         
            while(in.hasNextLine()) //it is parsing line by line            
            {             
            	
                line = in.nextLine().split("\t");		//delimiter
                
                
                uid = Integer.parseInt(line[0]);
                mid = Integer.parseInt(line[1]); 
                rating = Double.parseDouble(line[2]);
                
	          
                writeData.write(mid + "\t" + uid + "\t" + rating);
				writeData.newLine(); 
	        
            }
        }
        
        catch(FileNotFoundException e) {
            System.out.println("Can't find file " + fileNameToRead);
            e.printStackTrace();

        }
        catch(IOException e) {
            System.out.println("IO error");
            e.printStackTrace();
        }
        
        //close file		            
        closeFile();
        
    }
    
	 
	 //------------------------------------------------------------------
	 
	 public static void main(String args[])  
	  {
		 InvertUidAndMid rw = new InvertUidAndMid ();
		 rw.readData();
		 
	  }
	 
    
	 
	 //---------------------------------------------------------
	    public void openFile()    
	    {

	   	 try {
	   		   //sml
	   		   writeData  = new BufferedWriter(new FileWriter(myPath + "sml_ratings.dat", true)); 
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
	    
	   
}
