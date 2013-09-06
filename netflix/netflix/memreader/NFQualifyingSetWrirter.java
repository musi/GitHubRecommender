package netflix.memreader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import cern.colt.list.IntArrayList;
import cern.colt.map.OpenIntObjectHashMap;

public class NFQualifyingSetWrirter {
	
	String 				   myPath;	
	BufferedWriter     	   writeData;
	OpenIntObjectHashMap   midToUsers;
	OpenIntObjectHashMap   uidToMovies;
	IntArrayList		   allMovies;
	IntArrayList		   allUsers;
		
	//--------------------------------
	// constructor 
	
	public NFQualifyingSetWrirter()
	{
		midToUsers  = new OpenIntObjectHashMap ();
		uidToMovies = new OpenIntObjectHashMap ();
		allMovies   = new IntArrayList();
		allUsers    = new IntArrayList();
		
		myPath	    = "I:/Backup main data march 2010/Labs and datasets/Compiled Datasets/netflix data set/download/";
	}
	
	//--------------------------------
	
	 /**
     * Given an input file of data, will output properly formatted results.
     * This should only be used for Netflixprize entries.
     * 
     * Input should be formatted thus:
     * mid:
     * uid,date
     * uid,date
     * ...
     * 
     * Output should be formatted thus:
     * mid:
     * rating
     * rating
     * ...
     * 
     * @param MemHelper object, which has the probe ratings as well
     * @param inFile the name of the input file
     * @param outFile, write predictions to the output according to the format
     * 
     */
    
    public void readQualifyingFile(String inFile)    
    {
        File 			in 			= new File(inFile);			//for netflix this is a qulaifyiing file        
        Scanner 		sc 			= null;
        int 			currMovie	= 0;
        int				uid			= 0;
        BufferedWriter 	out;
        String 			currLine;
        String[] 		split;
        

        try {
               sc = new Scanner(in);		//new scanner of the file we want to read
           }       
        catch (FileNotFoundException e)         
        {
            System.out.println("Infile error, file not found!  Java error: "
                    + e);
            return;
        }

        try         
        {         

            while (sc.hasNextLine())            
            {
                currLine = sc.nextLine().trim();                
                split = currLine.split(",");

                // Get the mid
                if (split.length == 1)                
                {
                    currMovie = Integer.parseInt(currLine.substring(0, currLine.length() - 1));
                    
                    //add to all movies
                    if(allMovies.contains(currMovie)==false)
                    	allMovies.add(currMovie);
                }
                
                //get the users, who have seen this movie
                else                 
                {                	
                	uid = Integer.parseInt(split[0]);
                	
                    //add to all users
                    if(allUsers.contains(uid)==false)
                    	allUsers.add(uid);
                }

                //write data into hash map
                 if(midToUsers.containsKey(currMovie))
                 {
                	IntArrayList users = (IntArrayList) midToUsers.get(currMovie);
                	users.add(uid);
                	midToUsers.put(currMovie,users);
                 }
                 else
                 {
                	IntArrayList users = new IntArrayList();
                 	users.add(uid);
                 	midToUsers.put(currMovie,users);
                 }                 
                  
             } 
         
            System.out.println("all users="+ allUsers.size());
        }
        
        catch (Exception e) {
            System.out.println("Write error!  Java error: " + e);
            System.exit(1);
        }
    }
    
 //------------------------------------------------------------------------------------  

    public void openFile()    
    {

   	 try {
   		   //sml
   		   writeData  = new BufferedWriter(new FileWriter(myPath + "qualifyingRatings.dat", true)); 
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
   
   
  /*************************************************************************************************/
   
   /**
    * Main file
    */
   
   public static void main (String args[])
   {
	   NFQualifyingSetWrirter myQualifying = new NFQualifyingSetWrirter();
	   myQualifying.readAndWriteFiles();
   }
   
   //----------------------------------------------
   
    public void readAndWriteFiles()
    {
   
    	String inputFileName = myPath + "qualifying.txt"; 
    	
    	//read file
    	readQualifyingFile(inputFileName);
    	
    	//open output files
    	openFile();
    	
    	//writeData    	
    	writeIntoOutputFile();
    	
    	//close files
    	closeFile();
    	
    }

    //--------------------------------------------------
    
	public void writeIntoOutputFile()
	{
	/*     int totalUsers   = allUsers.size();
	     int totalMovies  = allMovies.size();
	     
	     allUsers.quickSort();
	     
	     for(int i=0;i<totalUsers;i++)
	     {	    	 
	    	 //uid
	    	 int uid = allUsers.get(i); 
	    	 IntArrayList   moviesSennByCurrentUser = new IntArrayList();
	    	 
	    	 for(int j=0;j<totalMovies;j++)
	    	 {	    	
		    	   //mid	 
		    	   int mid = allMovies.get(j);	    	   
		    	   IntArrayList tempUsers  =  (IntArrayList) midToUsers.get(mid);
		    	
		    	   //add this movie to the current user's list
		    	   if(tempUsers.contains(uid))
		    		   moviesSennByCurrentUser.add(mid);
	    	   	 
	          }//end inner for	    	 
	    	 
	    	 uidToMovies.put(uid, moviesSennByCurrentUser);
	    	 
	      } //end outer for     
	   
	   */
	
/*	     
	     //Go through all users
	     for(int i=0;i<totalUsers;i++)
	     {	    	 
	    	 
	    	 int		   uid		  = allUsers.get(i);	 	  
		 	 IntArrayList  myMovies   = (IntArrayList)uidToMovies.get(uid);
		 	 
		 	 //sort them
		 	 myMovies.quickSort(); 
		 	 int tempSize  = myMovies.size();
		 	 
		 	 for(int j=0;j<tempSize;j++)
		 	 {
		 		 int mid = myMovies.get(j);
			 	 
		 		 //Start writing
		 		 try{
			 	 writeData.write(uid+ ","+ mid);
			 	 writeData.newLine();
		 		 }
		 		 catch(Exception E){
		 			 E.printStackTrace();
		 			 System.exit(1);
		 		 }
			 	 
		 	 }//end inner for
	     }//end outer for
*/	     
	}//end function
	
	
	//----------------------------------------------------------------------------
}

