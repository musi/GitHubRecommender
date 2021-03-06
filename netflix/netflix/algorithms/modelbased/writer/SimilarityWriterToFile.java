package netflix.algorithms.modelbased.writer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * A SimilarityWriter to write similarity values to a text file
 * @author Amrit Tuladhar
 *
 */
/******************************************************************************************************/
public class SimilarityWriterToFile implements SimilarityWriter 
/******************************************************************************************************/
{
    private FileWriter similarityModelWriter = null;
 
/******************************************************************************************************/
    
    public SimilarityWriterToFile(String outputFile) //receive a file path    
    {
        try         
        {
            similarityModelWriter = new FileWriter(new File(outputFile)); //create new file writer
        }
        
        catch(IOException iOE) 
        {
            iOE.printStackTrace();
            System.exit(1);
        }
    }

 /******************************************************************************************************/
    
    public void write(int movieId1, int movieId2, double similarity) throws Exception    
    {
        similarityModelWriter.write(movieId1 + "," + movieId2 + "," + similarity + "\n"); //write mid, mid2, sim
    }

 /******************************************************************************************************/
    
    public void close() throws Exception    
    {
        similarityModelWriter.close();
    }

}
