package netflix.memreader;

//delete this and may u have to write efficient code for 80% train and 20% test set

import java.io.BufferedWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;

import cern.colt.list.IntArrayList;
import cern.colt.list.LongArrayList;
import cern.colt.map.OpenIntIntHashMap;

public class TimeBasedReadingInSMLDataSet 
{
  				
  private String        outFileT;				//write buffers  
  private String        myPath;  
  OpenIntIntHashMap     myMoviesMap;
  OpenIntIntHashMap     myUsersMap;	
  OpenIntIntHashMap     myUserToTimeMap;
  OpenIntIntHashMap     myMovieToTimeMap;
  IntArrayList 			myUsers;
  IntArrayList 			myMovies; 
  IntArrayList 			myUsersWeights;
  IntArrayList 			myMoviesWeights;
  
  
 /*************************************************************************************************/
  
  public TimeBasedReadingInSMLDataSet(String outFileT)
  {
	this.outFileT 	  = outFileT;  
	myMoviesMap       = new OpenIntIntHashMap();
	myUsersMap    	  = new OpenIntIntHashMap();
	myUserToTimeMap   = new OpenIntIntHashMap();
	myMovieToTimeMap  = new OpenIntIntHashMap();
	
  }
      
/************************************************************************************************/

/**
 * Read data and reassign movie variables
 */  
  
  public void readDataAndReassign(String fileName)  
  {
	
     String[] 		line;
     int	 		mid;
     int 			uid;
     double			rating;
     String			date;
     
     int movIndex 		= 1;		//from 1 to onwards
     int userIndex 		= 1;		//from 1 to onwards
     int total			= 0;
     int timeVal        = 0;        // read time the rating was made
     
     
      try      
	  {
  		  	Scanner in = new Scanner(new File(fileName));    // read from file the movies, users, and ratings, 

                
                while(in.hasNextLine())            
                {                   	
                	total++;
                	line 		= in.nextLine().split("\t");		//delimiter                    
                    uid 		= Integer.parseInt(line[0]);
                    mid 		= Integer.parseInt(line[1]);
                    rating 		= Double.parseDouble(line[2]);    
                    timeVal 	= Integer.parseInt(line[3]);
                    
                    
              /*      if(!(myMoviesMap.containsKey(mid)))
                    {
                    	myMoviesMap.put(mid, movIndex);
                    	movIndex++;
                    }
                                
                    if(!(myUsersMap.containsKey(uid)))
                    {
                    	myUsersMap.put(uid, userIndex);
                    	userIndex++;
                    }*/
  
                    //put time value per user
                    if(!(myUserToTimeMap.containsKey(uid)))
                    {
                    	myUserToTimeMap.put(uid, timeVal);
                    	
                    }
                    else  //get the previous time value and store the min value 
                    {
                       int temp = myUserToTimeMap.get(uid);
                       
                       //replace the val if it is more than the current one
                       if (timeVal < temp)
                    	   myUserToTimeMap.put(uid, timeVal);                       	
                    }
                           
                    
                    //put time value per movie
                    if(!(myMovieToTimeMap.containsKey(mid)))
                    {
                    	myMovieToTimeMap.put(mid, timeVal);
                    	
                    }
                    else  //get the previous time value and store the min value 
                    {
                       int temp = myMovieToTimeMap.get(mid);
                       
                       //replace the val if it is more than the current one
                       if (timeVal < temp)
                    	   myMovieToTimeMap.put(mid, timeVal);                       	
                    }
                                      
                
                 }
                
                
                //sort the users based on the time value, the user having the min value shld be in the begining                
                myUsers		 	= myUserToTimeMap.keys();
                myUsersWeights 	= myUserToTimeMap.values();
                myUserToTimeMap.pairsSortedByValue(myUsers, myUsersWeights);       
                
                  
                //sort the movies based on the time value, the user having the min value shld be in the begining                
                myMovies 			= myMovieToTimeMap.keys();
                myMoviesWeights 	= myMovieToTimeMap.values();
                myMovieToTimeMap.pairsSortedByValue(myMovies, myMoviesWeights);  
                
              
                		                
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
  
  
  //-------------------------------------------------------------------------------------------
  /**
   * Here we write in the output file
   */
  
  //The idea was, we will just Re-MAP the users_ID into new user IDs, based on the time stamp.
  //The remaining data is still the same

  public void writeDataIntoOutputFile(String fileName, int sortingOption)
  {
      
	  
	     String[] 			line;
	     BufferedWriter 	outT;	  
	     
	     int	 			mid 	  = 0;
	     int 				uid 	  = 0;
	     int 			    mappedUid = 0;
	     int 			    mappedMid = 0;
	     double				rating    = 0;	        
	    
	     int 				movIndex		= 1;		//from 1 to onwards
	     int 				userIndex 		= 1;		//from 1 to onwards
	     int				total			= 0;
	     int 				timeVal         = 0;  
	     
	  try{
		  
		   Scanner in 	= new Scanner(new File(fileName));
		   outT 		= new BufferedWriter(new FileWriter(outFileT));    
    	
    	       while(in.hasNextLine())            
               {                   	
               	    total++;
               	    line 		= in.nextLine().split("\t");		//delimiter                    
                    uid 		= Integer.parseInt(line[0]);
                    mid 		= Integer.parseInt(line[1]);
                    rating 		= Double.parseDouble(line[2]);    
                    timeVal 	= Integer.parseInt(line[3]);            
                    

                    //check the uid with the myUserToTimeMap's myUsers
                    if(sortingOption==1)
                    {
	                    if(myUsers.contains(uid))
	                    {
	                    	mappedUid  = myUsers.indexOf(uid) + 1;
	                    }
	                    
		                 //Start writing in file as well
			            String oneSample = (mappedUid) + "," + (mid) + "," + rating + "," + timeVal;
			            outT.write(oneSample);
			            outT.newLine();
                    }
                    
                    //movie based sorting
                    else{
                    	 if(myMovies.contains(mid))
 	                    {
 	                    	mappedMid  = myMovies.indexOf(mid) + 1;
 	                    }
 	                    
 		                 //Start writing in file as well
 			            String oneSample = (uid) + "," + (mappedMid) + "," + rating + "," + timeVal;
 			            outT.write(oneSample);
 			            outT.newLine();
                    }
	                    
		    	
		            
		       
               }//end while  
    	       
    	       //close file
    	        outT.close();    	        
	            System.out.println("Finished writing");
	            System.out.println("Mov index is ="+movIndex);
	            System.out.println("user index ="+userIndex);
	            System.out.println("total ="+total);
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
      
  
/************************************************************************************************/
  
  public static void main(String arg[])  
  {
	  
	    int userBasedSorting = 0; //1=yes, 0=no it is movie based now
	    
	    /*String pm  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\FT\\";
	    String input = pm + "ft_ratings.dat";
	    //String input = pm + "ft_mainSet2.dat";
	    String output = pm + "ft_myRatingsNor.dat";
	    */
	    
	  	String pm  = "";
	    String input = "";
	    String output = "";
	    
	    //user based
	    if(userBasedSorting==1)
	    {
	    	pm  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\";
		    input = pm + "u.txt";
		    output = pm + "sml_myTimedUserRatings.dat";	
	    }
	    
	    //movie based
	    else
	    {	pm  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\";
	    	input = pm + "u.txt";
	    	output = pm + "sml_myTimedMovieRatings.dat";
	    	
	    }
	/*    
	 * 
	 *
	    String input = pm + "ml_MainSetMarlinWeak20_3.dat";
	    String output = pm + "ml_MainSetMarlinNor20_3.dat";*/
	    
	    TimeBasedReadingInSMLDataSet dis= new TimeBasedReadingInSMLDataSet(output);
	  	
	    //sort users based on the time value
	    dis.readDataAndReassign(input);
	    
	    //write the data into the output file (with the sorted users)
	  	dis.writeDataIntoOutputFile(input, userBasedSorting);
	  	
	  	
  }

/************************************************************************************************/
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
