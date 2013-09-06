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

public class NormalizeScale 
{
  				
  private String        outFileT;				//write buffers  
  private String        myPath;  
  OpenIntIntHashMap     myMoviesMap;
  OpenIntIntHashMap     myUsersMap;	
  
 /*************************************************************************************************/
  
  public NormalizeScale(String outFileT)
  {
	this.outFileT = outFileT;  
	myMoviesMap   = new OpenIntIntHashMap();
	myUsersMap    = new OpenIntIntHashMap();
  }
      
/************************************************************************************************/

/**
 * Read data and reassign movie variables
 */  
  
  public void readDataAndReassi(String fileName)  
  {
	  
     BufferedWriter outT;
     
     String[] 		line;
     int	 		mid;
     int 			uid;
     double			rating;
     String			date;
     int movIndex 		= 1;		//from 1 to onwards
     int userIndex 		= 1;		//from 1 to onwards
     int total			= 0;
     
      try      
	  {
  		    // We wanna write in o/p file
    		outT = new BufferedWriter(new FileWriter(outFileT));    
    		Scanner in = new Scanner(new File(fileName));    // read from file the movies, users, and ratings, 

                
                while(in.hasNextLine())            
                {                   	
                	total++;
                	line = in.nextLine().split("::");		//delimiter                    
                    uid = Integer.parseInt(line[0]);
                    mid = Integer.parseInt(line[1]);
                    rating = Double.parseDouble(line[2]);    
                    
                    if(!(myMoviesMap.containsKey(mid)))
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
                /*    String oneSample = myUsersMap.get(uid) + "," + myMoviesMap.get(mid) + "," + rating;
                    outT.write(oneSample);
                    outT.newLine();*/
                    
                    String oneSample = (uid) + "," + (mid) + "," + rating;                    
                    outT.write(oneSample);
                    outT.newLine();
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
	    
	/*    String pm  = "C:/Users/Musi/Desktop/movie_ml_5/movie_ml/ml_data_10M/";
	    String input = pm + "ratings_orig.dat";
	    String output = pm + "ratings_Normalized.dat";*/
	    
	    
	    
	    
	/*    
	    String input = pm + "ml_MainSetMarlinWeak20_3.dat";
	    String output = pm + "ml_MainSetMarlinNor20_3.dat";*/
	    
	 
	  	
	    
	    //dis.readDataAndReassi(input);
	  	
	  		
	  	
/*	  	// Check the 10 M dataset
	  	String trainFile = pm + "raTrain.txt";
	  	String testFile = pm + "raTest.txt";
	  	String outFile = pm + "raTestTrain.txt";*/
	  	
	    
	    //BC checking
	    String pm  = "I:/Backup main data march 2010/Labs and datasets/Compiled Datasets/BookCrossing/BX-CSV-Dump/";
	    String inputFile = pm + "BX-Book-Ratings.csv";
	    String outputFile = pm + "BC_NorRatings.dat";
	    
	    
	     NormalizeScale dis= new NormalizeScale(outputFile);
	    
	  	dis.readWrite10MData(outputFile,outputFile,"");
	  	
  }

/************************************************************************************************/
  
  /**
   * We will read .txt (a,b) files of 10M dataset, and make sure what is happening inside
   */
  public void readWrite10MData(String fileName1, String fileName2, String outFile)  
  {
	  
     BufferedWriter outT;
     
     String[] 		line;
     int	 		mid;
     int 			uid;
     double			rating;
     String			date;
     int movIndex 		= 1;		//from 1 to onwards
     int userIndex 		= 1;		//from 1 to onwards
     int total			= 0;
     
      try      
	  {
  		    // We wanna write in o/p file
    		//outT = new BufferedWriter(new FileWriter(outFile));    
    		Scanner in1 = new Scanner(new File(fileName1));    // read from file the movies, users, and ratings, 
    		Scanner in2 = new Scanner(new File(fileName2));   

                
                while(in1.hasNextLine())            
                {                   	
                	total++;
                	line = in1.nextLine().split(",");		//delimiter                    
                    uid = Integer.parseInt(line[0]);
                    mid = Integer.parseInt(line[1]);
                    rating = Double.parseDouble(line[2]);    
                    
                    if(!(myMoviesMap.containsKey(mid)))
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
               /*     String oneSample = myUsersMap.get(uid) + "," + myMoviesMap.get(mid) + "," + rating;
                    outT.write(oneSample);
                    outT.newLine();
                    */
                    
                  /*  String oneSample = (uid) + "," + (mid) + "," + rating;                    
                    outT.write(oneSample);
                    outT.newLine();*/
                 }
                
                
               
                System.out.println("Finished writing");
                System.out.println("Mov index is ="+movIndex);
                System.out.println("user index ="+userIndex);
                System.out.println("total ="+total);
               
                
              /*  while(in2.hasNextLine())            
                {                   	
                	total++;
                	line = in2.nextLine().split(",");		//delimiter                    
                    uid = Integer.parseInt(line[0]);
                    mid = Integer.parseInt(line[1]);
                    rating = Double.parseDouble(line[2]);    
                    
                    if(!(myMoviesMap.containsKey(mid)))
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
                    
                    
                    String oneSample = (uid) + "," + (mid) + "," + rating;                    
                    outT.write(oneSample);
                    outT.newLine();
                 }
                */
                
               
                System.out.println("Finished writing");
                System.out.println("Mov index is ="+movIndex);
                System.out.println("user index ="+userIndex);
                System.out.println("total ="+total);
               
                
               // outT.close();
                
                		                
	  }//end try
            
            catch(FileNotFoundException e) {
                System.out.println("Can't find file " + fileName1);
                System.out.println("Can't find file " + fileName2);
                e.printStackTrace();

            }
            
            catch(IOException e) {
                System.out.println("IO error");
                e.printStackTrace();
            }

    
  }
      
  
  
  
  
  
  
  
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
