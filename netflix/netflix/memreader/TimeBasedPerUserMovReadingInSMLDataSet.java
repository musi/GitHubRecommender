package netflix.memreader;

//delete this and may u have to write efficient code for 80% train and 20% test set

import java.io.BufferedWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import cern.colt.list.LongArrayList;
import cern.colt.map.OpenIntDoubleHashMap;
import cern.colt.map.OpenIntIntHashMap;
import cern.colt.map.OpenIntObjectHashMap;

public class TimeBasedPerUserMovReadingInSMLDataSet 
{
  				
  private String        outFileT;						//write buffers  
  private String        myPath;  
  

  OpenIntIntHashMap     myUserToTimeMap;  
  OpenIntObjectHashMap  myPerUserMovieToTimeMap;		//for each user, sort the movies in the form of time
  OpenIntObjectHashMap  myPerUserMovieToRatMap;			//for each user, store mid and rat
  
  IntArrayList 			myUsers;
  IntArrayList 			myMovies;
  DoubleArrayList		myRatings;   
  
  IntArrayList 			myUserWeights;
  IntArrayList 			myMovWeights;
  

 /*************************************************************************************************/
  
  public TimeBasedPerUserMovReadingInSMLDataSet(String outFileT)
  {
	this.outFileT 	 = outFileT;  
	
	

	myUserToTimeMap  = new OpenIntIntHashMap();
	
	myPerUserMovieToTimeMap = new OpenIntObjectHashMap();
	myPerUserMovieToRatMap  = new OpenIntObjectHashMap();
	
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
                    
   
                    //-----------------------
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
                               
                    //-----------------------
                    //For movies, store Time
                    if(!(myPerUserMovieToTimeMap.containsKey(uid)))
                    {
                    	OpenIntIntHashMap myMovMap = new OpenIntIntHashMap();
                    	myMovMap.put(mid,timeVal);
                    	myPerUserMovieToTimeMap.put(uid, myMovMap);				//uid to obj
                    }
                    else	//get the previous pair and add this as well
                    {                 
                    	
                    	//We don't need to worry that, this will be overwritten. It can not be the case, as
                    	//each user rate each movie, only once
                    	OpenIntIntHashMap tempMap = (OpenIntIntHashMap) myPerUserMovieToTimeMap.get(uid);
                    	tempMap.put(mid,timeVal);
                    	myPerUserMovieToTimeMap.put(uid, tempMap);                    	
                    	
                    }
                    
                    //-----------------------
                    //For Movies, Store Rat
                    if(!(myPerUserMovieToRatMap.containsKey(uid)))
                    {
                    	OpenIntDoubleHashMap myRatMap = new OpenIntDoubleHashMap();
                    	myRatMap.put(mid,rating);
                    	myPerUserMovieToRatMap.put(uid, myRatMap);				//uid to obj
                    }
                    else	//get the previous pair and add this as well
                    {                    	            	
                    	//We don't need to worry that, this will be overwritten. It can not be the case, as
                    	//each user rate each movie, only once
                    	OpenIntDoubleHashMap tempMap = (OpenIntDoubleHashMap) myPerUserMovieToRatMap.get(uid);
                    	tempMap.put(mid,rating);
                    	myPerUserMovieToRatMap.put(uid, tempMap);                    	
                    	
                    }                           
                
                 }   
                
                
                //sort the users based on the time value, the user having the min value shld be in the begining                
                myUsers 		= myUserToTimeMap.keys();
                myUserWeights 	= myUserToTimeMap.values();
                myUserToTimeMap.pairsSortedByValue(myUsers, myUserWeights);       
             
           /*     //MOVIES
                //sort each pair of the movies as well
                for(int i=1;i<=943;i++)
                {
	                OpenIntIntHashMap tempList = (OpenIntIntHashMap) (myPerUserMovieToTimeMap.get(i));
	                myMovies 				   = tempList.keys();
	                myMovWeights 		 	   = tempList.values();
	                tempList.pairsSortedByValue(myMovies, myMovWeights);
                }
              */
                		      
      	      System.out.println("total samples ="+total);
      	      
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

  public void writeDataIntoOutputFile(String fileName)
  {
      
	  
	     
	     BufferedWriter 	outT;
	     
	     int	 			mid 	  		= 0;	
	     int				mappedUid       = 0;
	     double				rating    		= 0;
	     int 				movIndex		= 1;		//from 1 to onwards
	     int 				userIndex 		= 1;		//from 1 to onwards
	     int				total			= 0;
	     int 				timeVal         = 0;  
	     
	  try{
		  
		 
		 	//open the output file to write the data
		      outT = new BufferedWriter(new FileWriter(outFileT));    
    	
		      int totalUsers = myPerUserMovieToRatMap.keys().size();
		      System.out.println("total users ="+totalUsers);
		      
		      for(int u_temp=0;u_temp<totalUsers;u_temp++)				//for all users
		      {
		    	  
		    	  
		    	  //infact, we  have sorted the myUser array based on the time value,a dn now we can
		    	  //directly get this value and can write directly
		    	  int u = myUsers.get(u_temp);
		    	  
                 //Get movies array for this user        
                 OpenIntIntHashMap tempMovToTimeList   = (OpenIntIntHashMap) (myPerUserMovieToTimeMap.get(u));
                 OpenIntDoubleHashMap tempMovToRatList = (OpenIntDoubleHashMap) (myPerUserMovieToRatMap.get(u));
     	   
                 //sort the mov based on time value
	              myMovies 				   		= tempMovToTimeList.keys();
	              myMovWeights 		 	   		= tempMovToTimeList.values();
	              tempMovToTimeList.pairsSortedByValue(myMovies, myMovWeights);
	               
	              int movSizeForThisUser  	    = myMovies.size();            

	                 //get the mapped uid
	                 if(myUsers.contains(u))
	                 {
	                 	mappedUid  = myUsers.indexOf(u) + 1;
	                 }
	                 
	                
                  //FOR ALL MOVIES AGAINST THIS USER
                  for(int m=0;m<movSizeForThisUser;m++)
                  {
                	     mid 	 = myMovies.get(m);         //get a mid
                	     timeVal = myMovWeights.get(m);	    //get time val                	     
                	     rating  = tempMovToRatList.get(mid);   //get rating, see it is from the other hash table
                	     
                	  	
					    //Start writing in file as well
					     String oneSample = (mappedUid) + "," + (mid) + "," + rating + "," + timeVal;
					     //String oneSample = (mappedUid) + "," + (mid);
					     outT.write(oneSample);
					     outT.newLine();
					     total++;
	                  
                  }
		            
		      }//end all users
          
    	       
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
	  
	    /*String pm  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\FT\\";
	    String input = pm + "ft_ratings.dat";
	    //String input = pm + "ft_mainSet2.dat";
	    String output = pm + "ft_myRatingsNor.dat";
	    */
	    
	  	String pm  = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\";
	    String input = pm + "u.txt";
	    String output = pm + "sml_myTimedPerUserAndMovRatings.dat";
	    
	/*    
	    String input = pm + "ml_MainSetMarlinWeak20_3.dat";
	    String output = pm + "ml_MainSetMarlinNor20_3.dat";*/
	    
	    TimeBasedPerUserMovReadingInSMLDataSet dis= new TimeBasedPerUserMovReadingInSMLDataSet(output);
	  	
	    //sort users based on the time value
	    dis.readDataAndReassign(input);
	    
	    //write the data into the output file (with the sorted users)
	  	dis.writeDataIntoOutputFile(input);
	  	
	  	
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
