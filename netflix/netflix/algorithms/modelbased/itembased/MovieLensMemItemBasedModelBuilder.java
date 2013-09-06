package netflix.algorithms.modelbased.itembased;

import netflix.algorithms.modelbased.itembased.method.AdjCosineSimilarityMethod;
import netflix.algorithms.modelbased.itembased.method.PearsonSimilarityMethod;
import netflix.algorithms.modelbased.itembased.method.SimilarityMethod;
import netflix.algorithms.modelbased.reader.DataReader;
import netflix.algorithms.modelbased.reader.DataReaderFromDB;
import netflix.algorithms.modelbased.reader.DataReaderFromMem;
import netflix.algorithms.modelbased.writer.SimilarityWriter;
import netflix.algorithms.modelbased.writer.SimilarityWriterToFile;
import netflix.algorithms.modelbased.writer.SimiliarityWriterToDB;
import netflix.algorithms.modelbased.writer.UserSimKeeper;
import netflix.memreader.MemHelper;

/******************************************************************************************************/
public class MovieLensMemItemBasedModelBuilder
/******************************************************************************************************/
{
    /**
     * @param args
     */

	public static void main(String[] args)	
	{
		  String memHelperFile="";
          String outputFile="";
          String outputDB="";
          String myPath = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Item based\\FiveFoldData\\DataFD\\";
          //String myPath = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\ItemBased\\FiveFoldData\\Data1\\";
    
    
		try 		
		{
           // memHelperFile	 = "C:\\uabase.dat";						//MemHelperFile
           // outputFile 	   	 = "C:\\movielens_itemsim_adjcos_a.txt";    //Output file
       
			//____________________________
			// Build Model for 5-Fold Data			
			//____________________________
			
			for (int i=0;i<5;i++)
			{
			 
				//We have movies like (1150) in train set
				memHelperFile	     = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Item based\\FiveFoldData\\DataFD\\sml_trainSetStoredFold" + (i+1) + ".dat";
								
				//I want to see how much movies, I have in this test set
			//	memHelperFile	     = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Item based\\FiveFoldData\\DataD\\sml_testSetStoredFold" + (i+1) + ".dat";
				outputFile 	   	     = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Item based\\FiveFoldData\\DataFD\\sml_SimFold" + (i+1) + ".dat";    	//Output file

			//	 memHelperFile	     = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\ItemBased\\FiveFoldData\\Data1\\ml_trainSetStoredFold" + (i+1) + ".dat";
			//    outputFile 	   	 = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\ItemBased\\FiveFoldData\\Data1\\ml_SimFold" + (i+1) + ".dat";    	//Output file
			     
		         outputDB 	   	     = "sml_SimFold" + (i+1) + ".dat";    	//Output DB
				       

            //Data Reader	
            DataReader movielensDataReader = new DataReaderFromMem( 
            								 new MemHelper(memHelperFile)); //it receive a memHelper objects


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
        
            
            //build the model
            movielensModelBuilder.setFileName(myPath + "\\StoredRCSim\\SimFold" + (i+1) + ".dat");
            movielensModelBuilder.buildModel();

            
            movielensDataReader.close();
            movielensSimWriter.close();
        }
	}//end of five folds for	
		
        catch(Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

}
