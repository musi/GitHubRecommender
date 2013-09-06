package netflix.memreader;

import java.io.*;
import java.util.Scanner;

public class ProbeRatings{

  public static void main(String[] args) {
    
	  ProbeRatings myProbe = new ProbeRatings();
	  
		String myPath= "I:/Backup main data march 2010/Labs and datasets/Compiled Datasets/netflix data set/download/";
	      
    	String fileInputName 		=  myPath + "probe2.txt";
    	String fileOutputName 		=  myPath + "probeRatings.txt";					//it is acc to the format in which the above method is working
    	String storedRatings        =  myPath + "nf_storedProbe.dat";
    	
	  myProbe.writeRatings(fileInputName, fileOutputName);
	  MemReaderNF myReader = new MemReaderNF();	  
	  myReader.writeIntoDisk(fileInputName, storedRatings, true); 		
		
  }
  
  //---------------------------------------
  
  public void writeRatings(String fileInputName, String fileOutputName) 
  {
  
   
        try         
        {
        	FileWriter fstream = new FileWriter(fileOutputName);
            BufferedWriter out = new BufferedWriter(fstream);
            
        	Scanner in = new Scanner(new File(fileInputName));    // read from file the movies, users, and ratings, 

            String[] 	line;
            int 		mid;
            int 		uid;
        
           // int myCheck=0;

            while(in.hasNextLine()) //it is parsing line by line            
            {
            	
                line = in.nextLine().split(",");		//delimiter
                
                mid = Integer.parseInt(line[0]);
                uid = Integer.parseInt(line[1]);               
                  
                   
                
                out.write(uid + "," + mid + "," + 1);
                out.newLine();
				  
            }
            
            out.flush();
            out.close();
            
        }
        catch(FileNotFoundException e) {
            System.out.println("Can't find file " + fileInputName);
            e.printStackTrace();

        }
        catch(IOException e) {
            System.out.println("IO error");
            e.printStackTrace();
        }
        
    
    try{
        // Create file 
        FileWriter fstream = new FileWriter("out.txt");
            BufferedWriter out = new BufferedWriter(fstream);
        out.write("Hello Java");
        //Close the output stream
        out.close();
        
        System.out.println("done");
        
        }catch (Exception e){//Catch exception if any
          System.err.println("Error: " + e.getMessage());
        }
      }
  
  
  
  
}