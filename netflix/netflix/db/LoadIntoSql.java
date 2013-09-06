package netflix.db;


import java.util.*;
import java.io.*;
import netflix.utilities.*;							//some utilities
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


/**
 * class to write values from file into mysql table (table should there)
 *  
 * @author Musi
 *
 */

public class LoadIntoSql extends Database

{  
	  int 		totRecord;
	  String 	tableName;
	  Database  myDb;
	  String LocationToWriteFileC;

/*************************************************************************************************************/
/**
 * function to read data from file, separte it into entries and loading into a mySql table
 * author Musi
 * @return  nothing
 * 
 */	  
	  LoadIntoSql()
	  {
		  totRecord=0;
		  tableName= "sml_sim_C";
		  LocationToWriteFileC = "C:\\Users\\Musi\\workspace\\MusiRec\\DataSets\\SML_ML\\sml_sim_C.dat";
		  
		  myDb  = new Database();	//create a new DB object
		  myDb.openConnection();
		  
		  
	  }

/*************************************************************************************************************/

	  public void readData(String fileName) 
	    
	    {

	        try 
	        
	        {

	            Scanner in = new Scanner(new File(fileName));    // read from file the movies, users, and ratings, 
	            


	            String[] 	line;
	            int 		mid1;
	            int 		mid2;
	            double 		sim;
	            
	            String		myQuery;
	            
	            

	            while(in.hasNextLine()) //it is parsing line by line
	            
	            {

	            	
	                line = in.nextLine().split(",");		//delimiter
	                
	                mid1 	= 	Integer.parseInt(line[0]);
	                mid2 	= 	Integer.parseInt(line[1]);
	                sim  	=   Double.parseDouble(line[2]);
	        		
	                totRecord++;

	                myQuery = "INSERT INTO similarities Values(" + mid1 + "," + mid2 + "," + sim+ ");";
	                myDb.updateDB(myQuery);
	            }
	            
	            	

            }//end of try
	        
	        catch(FileNotFoundException e) {
	            System.out.println("Can't find file " + fileName);
	            e.printStackTrace();

	        }
	        catch(IOException e) {
	            System.out.println("IO error");
	            e.printStackTrace();
	        }
	    }

/*************************************************************************************************************/

		  
	  public static void main (String[] arg) 
	    
	    {  

		    LoadIntoSql loader = new LoadIntoSql();
		    
	        
		    
		    loader.readData(loader.LocationToWriteFileC);
		    loader.myDb.closeConnection();
		    
		    System.out.printf("Writing done");  
	  
	        	  
	    }  
	  
/*************************************************************************************************************/
	 


}  
	  