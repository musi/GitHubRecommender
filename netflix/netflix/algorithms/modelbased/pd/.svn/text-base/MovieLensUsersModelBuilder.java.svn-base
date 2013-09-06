package netflix.algorithms.modelbased.pd;

import netflix.algorithms.modelbased.itembased.ItemBasedModelBuilder;
import netflix.algorithms.modelbased.itembased.method.*;
import netflix.algorithms.modelbased.reader.*;
import netflix.algorithms.modelbased.writer.*;
import netflix.memreader.MemHelper;

/******************************************************************************************************/
public class MovieLensUsersModelBuilder 
/******************************************************************************************************/
{

/******************************************************************************************************/
	
	public static void main(String[] args)	
	{
		String myPath = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Item based\\FiveFoldData\\DataFD\\";
		
		try 		
		{
			for (int i=0; i<5; i++)			
			{
				String memHelperFile;
				String outputFile;


				 memHelperFile	     = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\PD\\FiveFoldData\\sml_testSetStoredFold" + (i+1) + ".dat";
				 outputFile 	   	 = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\PD\\FiveFoldData\\sml_SimFold" + (i+1) + ".dat";    	//Output file

				
				//Memhelper
				DataReader dr 		= new DataReaderFromMem(new MemHelper(memHelperFile));
				SimilarityWriter sw = new UserSimKeeper();
				//TODO: allow change here
				
				//Similarity
				SimilarityMethod sm = new AdjCosineSimilarityMethod();
				sm.setNumMinMovies(1);
				
				//Model
				ItemBasedModelBuilder userModelBuilder = 
										new ItemBasedModelBuilder(dr, sw, sm);
				
				//Set output file name path
				userModelBuilder.setFileName(myPath + "\\StoredRCSim\\SimFold" + (i+1) + ".dat");
				
				//Build Model
				userModelBuilder.buildModel(true, true);
				
				dr.close();
				//don't need to close the simKeeper because we only serialize with it and
				//that closes its own writers
			}
			
		} 
		
		catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

	}

}
