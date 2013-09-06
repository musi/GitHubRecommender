package netflix.memreader;

//delete this and may u have to write efficient code for 80% train and 20% test set

import java.io.BufferedWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import cern.colt.list.IntArrayList;
import cern.colt.list.LongArrayList;
import cern.colt.map.OpenIntIntHashMap;

public class BookCrossingNormalization 
{
  				
  private String        outFileT;				//write buffers  
  private String        myPath;  
  Map <String,Integer>  myMoviesMap;
  Map <String,Integer>  myUsersMap;
  	
  
 /*************************************************************************************************/
  
  public BookCrossingNormalization(String outFileT)
  {
	this.outFileT   		= outFileT;  
	myMoviesMap      		= new HashMap<String,Integer>();
	myUsersMap     			= new HashMap<String,Integer>();
	
  }
      
/************************************************************************************************/

/**
 * Read data and reassign movie variables
 */  
  
  public void readDataAndReassi(String fileName)  
  {
	  
     BufferedWriter outT;
     
     String[] 		line;
     String			date;
     String	 		mid;     
     String 	 	uid;     
     String  	    rating;
     
     int     movIndex 	= 1;		//from 1 to onwards
     int     userIndex 	= 1;		//from 1 to onwards
     int     total		= 0;
     
      try      
	  {
  		    // We wanna write in o/p file
    		outT = new BufferedWriter(new FileWriter(outFileT));    
    		Scanner in = new Scanner(new File(fileName));    // read from file the movies, users, and ratings, 

                
                while(in.hasNextLine())            
                {                   	
                	total++;
                	line = in.nextLine().split(";");		//delimiter
                	
                	//avoid first line
                	if(total>2)
                	{
	                	                    
	                    uid 	= removeCharacters(line[0]);
	                    mid 	= removeCharacters(line[1]);
	                    rating  = removeCharacters(line[2]);
	                    
	                            
	                    if(myMoviesMap.containsKey(mid)==false)
	                    {                    	
	                    	myMoviesMap.put(mid, movIndex);
	                    	movIndex++;
	                    }
	                                
	                    if(!(myUsersMap.containsKey(uid)))
	                    {
	                    	myUsersMap.put(uid, userIndex);
	                    	userIndex++;
	                    }
	  
	                    //Start writing in file as well
	                    String oneSample = myUsersMap.get(uid) + "," + myMoviesMap.get(mid) + "," + rating;
	                    outT.write(oneSample);
	                    outT.newLine();
	                    
	                    /*String oneSample = (uid) + "," + (mid) + "," + rating;                    
	                    outT.write(oneSample);
	                    outT.newLine();*/
	                	}
                 }
                
                
                outT.close();
                System.out.println("Finished writing");
                System.out.println("Mov index is ="+movIndex);
                System.out.println("user index ="+userIndex);
                System.out.println("total ="+total);
                
                		                
	  }//end try
            
            catch(FileNotFoundException e) {
                System.out.println("Can't find file " + fileName);
                e.printStackTrace();

            }
            
            catch(IOException e) {
                System.out.println("IO error");
                e.printStackTrace();
            }

    
  }
      
  
  /*********************************************************************************************/
  /**
   * Remove a ""
   */
  
  public String removeCharacters(String text) {  
      StringBuffer buffer = new StringBuffer();  
      
      for(int i = 1; i < text.length()-1; i++) 
      {  
          char ch = text.charAt(i);           
           buffer.append(ch);  
        }  
       
      return buffer.toString();  
  }  
  
/************************************************************************************************/
  
  public static void main(String arg[])  
  {
	  
	    /*String pm  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\FT\\";
	    String input = pm + "ft_ratings.dat";
	    //String input = pm + "ft_mainSet2.dat";
	    String output = pm + "ft_myRatingsNor.dat";
	    */
	  
	    /*
	  	String pm  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\";
	    String input = pm + "ml_MyRatings.dat";
	    String output = pm + "ml_myNorRatings.dat";*/
	    
	    String pm  = "I:/Backup main data march 2010/Labs and datasets/Compiled Datasets/BookCrossing/BX-CSV-Dump/";
	    String input = pm + "BX-Book-Ratings.csv";
	    String output = pm + "BC_NorRatings.dat";
	        
	    BookCrossingNormalization dis= new BookCrossingNormalization(output);    
	    dis.readDataAndReassi(input);
	  	

  }
      
  
  /*Finished writing
  Mov index is =340557
  user index =105283
  total =1149781
*/
  
  
  
  
  
  
  
  
// greater than 10
/*  Mov index is =133
    user index =804
*/

//greater than 5
  /*  Mov index is = 258
      user index = 980
  */

//greater than 2
  /*  Mov index is = 567
    user index = 1094
  */

//greater than 1
  /*  Mov index is = 893
    user index = 1139
  */

    
    
  
} 
