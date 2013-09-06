package netflix.algorithms.modelbased.itembased;

import netflix.algorithms.modelbased.itembased.method.AdjCosineSimilarityMethod;
import netflix.algorithms.modelbased.itembased.method.SimilarityMethod;
import netflix.algorithms.modelbased.reader.DataReader;
import netflix.algorithms.modelbased.reader.DataReaderFromDB;
import netflix.algorithms.modelbased.reader.DataReaderFromMem;
import netflix.algorithms.modelbased.writer.SimilarityWriter;
import netflix.algorithms.modelbased.writer.SimilarityWriterToFile;
import netflix.algorithms.modelbased.writer.UserSimKeeper;
import netflix.memreader.MemHelper;

/******************************************************************************************************/
public class MovieLensDBItemBasedModelBuilder 
/******************************************************************************************************/
{
   /**
     * @param args
     */
	
   public static void main(String[] args)    
   {
	   String mainPathSML = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Item based\\";
	   String mainPathFT = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\FT\\Item based\\";
	   //SML
	   String myPath = mainPathSML;
	   String LocationToWriteFileC = mainPathSML + "sml_sim_C.dat";
	   String LocationToWriteFileP = mainPathSML + "sml_sim_P.dat";
	   String memHelperFile	       = mainPathSML + "sml_clusteringTrainSetStoredTF.dat";
	   

	   //FT
	   /*String myPath = mainPathFT;
	   String LocationToWriteFileC = mainPathFT + "ft_sim_C10.dat";
	   String LocationToWriteFileP = mainPathFT + "ft_sim_P10.dat";
	   String memHelperFile	       = mainPathFT + "ft_clusteringTrainSetStoredTF10.dat";
	
	*/   
        try        
        {
        	//DataReader        
            DataReader movielensDataReader = new DataReaderFromMem( 
					 new MemHelper(memHelperFile)); //it receive a memHelper objects

            //similarity writer
            SimilarityWriter movielensSimWriter = 	new UserSimKeeper();
           
            //similarity method
            SimilarityMethod movielensSimAdjCosineMethod = new AdjCosineSimilarityMethod();
            movielensSimAdjCosineMethod.setNumMinUsers(1);
         // SimilarityMethod PearsonSimilarityMethod = new PearsonSimilarityMethod();
        //  PearsonSimilarityMethod.setNumMinUsers(1);
                       
           
           ItemBasedModelBuilder movielensModelBuilder = new ItemBasedModelBuilder(
           														movielensDataReader,       
           														movielensSimWriter, 
           													    movielensSimAdjCosineMethod
           													//	PearsonSimilarityMethod
           													    );
            
            
           //call method of the above created object
           movielensModelBuilder.setFileName(LocationToWriteFileC);
            movielensModelBuilder.buildModel();		//this object has specified similarityMethods etc
            
            movielensDataReader.close();
            movielensSimWriter.close();
        }

        catch(Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

}
