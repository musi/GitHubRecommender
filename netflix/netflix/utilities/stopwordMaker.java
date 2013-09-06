package netflix.utilities;

import java.io.File;
import java.io.FileWriter;
import java.util.Scanner;

public class stopwordMaker {

	 //Files
	FileWriter myWriter;
	String myPath;
    
	
	//----------------
	public stopwordMaker()
	{
		 myPath = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/";		
		
	}
	
	//----------------
	
	 public void readData(String fileName)    
	 {
	        try         
	        {
	        	Scanner in = new Scanner(new File(fileName));    // read from file the movies, users, and ratings, 

	            String[] 	line;	            
	            openFile(myPath);
	            
	            while(in.hasNextLine()) //it is parsing line by line            
	            {
	                line = in.nextLine().split(",");		//delimiter
	                int limit = line.length;
	                
	                for(int i=0;i<limit;i++)
	                {
	                	String dum = line[i].trim();
	                	
	                	myWriter.append(dum);
	                	myWriter.append("\n");
	                	
	                }
	         
	             }//end while	            
		   
	            myWriter.flush();
		        myWriter.close();
		        
	            } //end try
	        
	        catch(Exception E)
	        {
	        	E.printStackTrace();
	        }
	        
	       
	        
	    }//end function

	 //----------------------------------
	 
	 public void openFile(String myPath)
	 {
	 
		 try{			
			
			 myWriter = new FileWriter(myPath +"stopWords.csv");	
		}
		catch (Exception E)
		{
			E.printStackTrace();
		}
	 }
	 
	 //-------------------------------
	 
	 public static void main (String args[])
	 {
		 stopwordMaker mySW = new stopwordMaker();
		 mySW.readData("C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/stopWord.txt");
	 }
}//end class
	            

