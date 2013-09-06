package netflix.weka;

import weka.core.FastVector;
import cern.colt.list.IntArrayList;
import cern.colt.list.LongArrayList;
import netflix.memreader.MemHelper;
import netflix.rmse.RMSECalculator;

public class CheckErrorInFilledObject {

	   //Start up RMSE count
    	RMSECalculator rmse;
    
    	CheckErrorInFilledObject ()
    	{
    		//For MAE
   		 	rmse = new RMSECalculator();
    	}	

	

	   
	  //----------------------------------------------------
	   
	  public void checkError(MemHelper MainMh, MemHelper myTestMh)
	  {
		  	IntArrayList users = MainMh.getListOfUsers();
	        int allUsers = users.size();
	        int uid, moviesSize, mid;
	        
	        
	        for(int i=0;i<allUsers;i++)
	        {
	            uid = users.getQuick(i);          
	            LongArrayList movies = myTestMh.getMoviesSeenByUser(uid);
	            moviesSize = movies.size();
	            
	           for (int j = 0; j < moviesSize; j++)
	           {      	
	            	mid = MemHelper.parseUserOrMovie(movies.getQuick(j));		    // get mid
	      
	            	double actual = myTestMh.getRating(uid, mid);
	            	double predicted = MainMh.getRating(uid, mid);
	            	
	            	//System.out.println("a:p -->" + actual +" : " + predicted);
	            	
	            	if(predicted != -99)
	            		rmse.add(actual, predicted);
	            	
	           }
	            
		  
	       }
	        
	        System.out.println("Error ="+ rmse.mae());
	        
	  } //end function+ 
	  
		//----------------------------------------------------
	   
	  public static void main (String arg[]) throws Exception
      {
		    String path   = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\SVD\\FeaturesPlay\\";
		    String test   = path + "sml_clusteringTestSetStoredTF.dat";
			String train  = path + "sml_clusteringTrainSetStoredTF.dat";
			String main   = path + "DummySparse\\sml_storedSparseRatingsTF_11.dat";
		    
			
			MemHelper mainH = new MemHelper(main);
			MemHelper testH = new MemHelper(test);			
			
			CheckErrorInFilledObject chk = new CheckErrorInFilledObject();
			chk.checkError(mainH, testH);		
			
    	}
	   
	  
	   
}
