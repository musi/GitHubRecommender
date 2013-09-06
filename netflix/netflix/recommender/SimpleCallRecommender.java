package netflix.recommender;

//Debugging is twice as hard as writing the code in the first place. Therefore, 
//if you write the code as cleverly as possible, you are, by definition, not smart enough to debug it.

import java.text.DecimalFormat;
import netflix.memreader.*;
import netflix.algorithms.memorybased.memreader.MyRecommender;
import netflix.algorithms.memorybased.memreader.ContentBoostedRecommender;
import netflix.memreader.Sparsity;

public class SimpleCallRecommender
{
   	    String t; 
	    String tr;
	    String p; 
	    String f;
	    String storedt;
	    String storedtr;
	    String storedFilledtr;
	    
	    String what;				
	    String fileNameToWrite;
	    
	    int 	myNeighbourSize;
	    int		myNeighbourInc;
	    String 	myType;				//type of recommender algo
	    double  factor;				//division factor train size
	    int 	topN;				//topN
	    Sparsity mySp;
	    MemHelper mhh;
	    double    ss;

	    boolean mySmallCall;
	    boolean mySparseCall;
	    boolean myTopCall;
	    int     myLoopIndex;
	    
	    
    public SimpleCallRecommender()
    
    {
    				//used to call some functions like calculate sparsity
    	
    }
	    
/*******************************************************************************************************/
    
 /**
 * @param small
 * @param sparseCall
 * @param top
 * @param topNumber
 * @param trainNumber
 * @param loopIndex
 * 
 */	    
	
   public SimpleCallRecommender(boolean small,			// small or large set
							boolean sparseCall,		// if sparse call, rather than simple data
							boolean top, 			// wanna see top users?
							int topNumber, 		    // how much training set (e.g.80) 
							int trainNumber,		// top N 
							int loopIndex)			//require to calculate the sparse results	
	{
	
		    mySp = new Sparsity();	
		
			myNeighbourSize = 120;
			myNeighbourInc  = 10;
			factor			= (trainNumber * (1.0))/100.0;
			topN			= topNumber;
			//myType			= "simple";
		    //myType			= "deviation";
			

			 mySmallCall 		= small;
		     mySparseCall 		= sparseCall;
		     myTopCall 			= top;
		     myLoopIndex 		= loopIndex;
	}
	
		    
/*******************************************************************************************************/
	
	    public void callsetUpFunction	    
	    				(boolean small,			// small or large set
						 String  whichCall,		// Contains sparse etc
	    				 int topNumber, 		// how much training set (e.g.80) 
						 int trainNumber,		// top N 
						 int loopIndex,      	// require to calculate the sparse results
						 int factorX			// to determine which of the test-train ratio is there
	    				)		

 {			factor			= (trainNumber * (1.0))/100.0;
			topN			= topNumber;
			myType			= "simple";
		//	myType		= "deviation";

			
		if (small ==true)
			
		{
					
						//All users
			//10-90
			//20-80
			//30-70
			
		
			
			//all, non-sparse, cross user based
		if (whichCall.equalsIgnoreCase("AllNonSparseUCrossNeighbour")) //here we will check cross validation
				 
			  {
				String neighbourz = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\TestTrain\\Neighbourhood\\";
				// p = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\TestTrain\\TenFoldData\\";
				 p = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\TestTrain\\FiveFoldData\\";
				 f = p + "sml_storedRatings.dat";
				
				    
			  //  t  		 = p + "sml_testSetAll.dat"+ "_" + (100-trainNumber) + ".dat";;
		       // tr 		 = p + "sml_trainSetAll.dat" + "_" + trainNumber + ".dat";
				
				 //we have everything in the folder we just need to call recommender function with suitable names
				storedt	 = p + "DataU1\\"+ "sml_testSetStoredFold" + loopIndex + ".dat";			
				storedtr = p + "DataU1\\"+ "sml_trainSetStoredFold" + loopIndex + ".dat";
				what 	 ="ALL sml with  10 fold cross validation in 80% of traing set for checking parameter sensitivity";
//				fileNameToWrite = neighbourz + "\\Results\\" + myType + "Fold" + loopIndex;
				fileNameToWrite = p + "\\ResultsU1\\" + myType + "Fold" + loopIndex;
			 }
		
		
		
		//simple all cross neighbour
		if (whichCall.equalsIgnoreCase("AllNonSparseCrossNeighbour")) //here we will check cross validation
				 
			  {
				String neighbourz = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\TestTrain\\Neighbourhood\\";
				// p = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\TestTrain\\TenFoldData\\";
				 p = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\TestTrain\\FiveFoldData\\";
				 f = p + "sml_storedRatings.dat";
				
				    
			  //  t  		 = p + "sml_testSetAll.dat"+ "_" + (100-trainNumber) + ".dat";;
		       // tr 		 = p + "sml_trainSetAll.dat" + "_" + trainNumber + ".dat";
				
				 //we have everything in the folder we just need to call recommender function with suitable names
				storedt	 = p + "Data1\\"+ "sml_testSetStoredFold" + loopIndex + ".dat";			
				storedtr = p + "Data1\\"+ "sml_trainSetStoredFold" + loopIndex + ".dat";
				what 	 ="ALL sml with  10 fold cross validation in 80% of traing set for checking parameter sensitivity";
//				fileNameToWrite = neighbourz + "\\Results\\" + myType + "Fold" + loopIndex;
				fileNameToWrite = myType + "Fold" + loopIndex;
			 }
		
		
		
		if (whichCall.equalsIgnoreCase("AllNonSparseNeighbour")) 		 
		  {
		
			 p = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Neighbourhood\\";
			 f = p + "sml_storedRatings.dat";
			
			    
		    t  		 = p + "sml_testSetAll.dat"+ "_" + (100-trainNumber) + ".dat";;
	        tr 		 = p + "sml_trainSetAll.dat" + "_" + trainNumber + ".dat";
			storedt	 = p + "\\WrittenFiles\\"+"sml_testSetStoredAll" + "_" + (100-trainNumber) + ".dat";			
			storedtr = p + "\\WrittenFiles\\"+"sml_trainSetStoredAll" + "_" + trainNumber + ".dat";
			what 	 ="ALL sml with " + trainNumber + " train and " + (100-trainNumber) +" test data";
			fileNameToWrite = p + "\\Results\\" + myType + "All" + "_" + trainNumber + ".dat";
		 }
		  
		if (whichCall.equalsIgnoreCase("AllSparse"))	//now we will play with sparse data
			
		  {
			    p = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Sparsity\\";
				f = p + "sml_storedRatings.dat";
								    
	//		    t  		 = p + "sml_testSetAll.dat"+ "_" + (100-trainNumber) + ".dat";;
	//	        tr 		 = p + "sml_trainSetAll.dat" + "_" + trainNumber + ".dat";
				storedt	 = p + "\\WrittenFiles\\"+"sml_testSetStoredaAll" + "_" + (100-trainNumber) + ".dat";
				
				storedtr = p + "\\WrittenFiles\\"+"sml_trainSetStoredAll" + "_" + (trainNumber) + "_" + (loopIndex) + ".dat";
				what 	 ="ALL sml with " + trainNumber + " train and " + (100-trainNumber) +" test data" + " 10% sparsity";
				mhh= new MemHelper(storedtr);
				ss = mySp.calculateSparsity(mhh);
				fileNameToWrite = p + "\\Results2\\" +  myType + "All" + "_" + trainNumber + "_" + ss ;
				//fileNameToWrite = myType + "Fold" + loopIndex;
			  			  
		  }
		 
	   
		if (whichCall.equalsIgnoreCase("AllSimpleCheck")) //here we will chcek simple rmse, user and movie average
     	  {
	
			p = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\TestTrain\\SimpleCheck\\";
			f = p + "sml_storedRatings.dat";
			
			    
		    t  		 = p + "sml_testSetAll.dat"+ "_" + (100-trainNumber) + ".dat";;
	        tr 		 = p + "sml_trainSetAll.dat" + "_" + trainNumber + ".dat";
			
			 //we have everything in the folder we just need to call recommender function with suitable names
			storedt	 = p + "sml_testSetStoredFold" + trainNumber + ".dat";			
			storedtr = p + "sml_trainSetStoredFold" + trainNumber + ".dat";
			what 	 ="ALL sml with  10 fold cross validation in 80% of traing set for checking parameter sensitivity";
			fileNameToWrite = p + "\\Results2\\" + myType ;
		 }
		
		//BookCrossing simple check
		if (whichCall.equalsIgnoreCase("AllSimpleCheckBC")) //here we will chcek simple rmse, user and movie average
		{
	
			p = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\BC\\SimpleCheck\\";
			f = p + "Bc_ratings.dat";
						    
		    t  		 = p + "sml_testSetAll.dat"+ "_" + (100-trainNumber) + ".dat";;
	        tr 		 = p + "sml_trainSetAll.dat" + "_" + trainNumber + ".dat";
			
			 //we have everything in the folder we just need to call recommender function with suitable names
			storedt	 = p + "Bc_testSetStored"  + ".dat";			
			storedtr = p + "Bc_trainSetStored" + ".dat";
			what 	 ="ALL sml with  10 fold cross validation in 80% of traing set for checking parameter sensitivity";
			fileNameToWrite = p + "\\Results1\\" + myType ;
		 }
	
		
		//here we will check mae for the data we created for cluster (why it is diff for 1 cluster and simple mae)
		if (whichCall.equalsIgnoreCase("AllSimpleCheckClustering"))
   	    {
	
			p = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\";
			f = p + "sml_storedFeaturesRatingsTF.dat";
			
			    
		    t  		 = p + "sml_testSetAll.dat"+ "_" + (100-trainNumber) + ".dat";;
	        tr 		 = p + "sml_trainSetAll.dat" + "_" + trainNumber + ".dat";
			
			 //we have everything in the folder we just need to call recommender function with suitable names
			storedt	 = p + "sml_clusteringTestSetStoredTF.dat";			
			storedtr = p + "sml_clusteringTrainSetStoredTF.dat";
			what 	 ="ALL sml with  10 fold cross validation in 80% of traing set for checking parameter sensitivity";
			fileNameToWrite = p + "\\Results2\\" + myType ;
		 }
		 
		if (whichCall.equalsIgnoreCase("AllSimpleCheckClusteringML"))
   	    {
	
			p = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\Clustering\\";
			f = p + "ml_storedFeaturesRatingsTF.dat";
			
			    
		    t  		 = p + "ml_testSetAll.dat"+ "_" + (100-trainNumber) + ".dat";;
	        tr 		 = p + "ml_trainSetAll.dat" + "_" + trainNumber + ".dat";
			
			 //we have everything in the folder we just need to call recommender function with suitable names
			storedt	 = p + "ml_clusteringTestSetStoredTF.dat";			
			storedtr = p + "ml_clusteringTrainSetStoredTF.dat";
			what 	 ="ALL sml with  10 fold cross validation in 80% of traing set for checking parameter sensitivity";
			fileNameToWrite = p + "\\Results2\\" + myType ;
		 }
	
		//---------------------
		//content boosted CF
		//---------------------
	
		//here we will check mae for the data we created for cluster (why it is diff for 1 cluster and simple mae)
		if (whichCall.equalsIgnoreCase("ContentBoosted"))
   	    {
	
			p = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Clustering\\";
			f = p + "sml_storedRatings.dat";
			
			    
		    t  		 = p + "sml_testSetAll.dat"+ "_" + (100-trainNumber) + ".dat";;
	        tr 		 = p + "sml_trainSetAll.dat" + "_" + trainNumber + ".dat";
			
			 //we have everything in the folder we just need to call recommender function with suitable names
			storedt	 = p + "sml_clusteringTestSetStoredTF.dat";			
			storedtr = p + "sml_clusteringTrainSetStoredTF.dat";
			storedFilledtr = p + "sml_clusteringFilledTrainSetStoredTF.dat";
			
			what 	 ="ALL sml with  10 fold cross validation in 80% of traing set for checking parameter sensitivity";
			fileNameToWrite = p + "\\Results2\\" + myType ;
		 }
		
		
		
		
		
		
		if (whichCall.equalsIgnoreCase("AllSimpleCheckFT")) //here we will chcek simple rmse, user and movie average
     	  {
	
			//p = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\FT\\FTDataSet\\";
			
			p = "I:\\Backup main data march 2010\\workspace\\MusiRecommender\\DataSets\\FT\\Itembased\\FiveFoldData\\";			
			f = p + "sml_storedRatings.dat";
			
			    
		    t  		 = p + "sml_testSetAll.dat"+ "_" + (100-trainNumber) + ".dat";;
	        tr 		 = p + "sml_trainSetAll.dat" + "_" + trainNumber + ".dat";
			
			 //we have everything in the folder we just need to call recommender function with suitable names
			/*storedt	 = p + "ft_clusteringTestSetStored" + trainNumber + ".dat";			
			storedtr = p + "ft_clusteringTrainSetStored" + trainNumber + ".dat";*/
	       
	        /* storedt	 = p + "ft_clusteringTestSetStoredOnlyUsersTF10.dat";			
			storedtr = p + "ft_clusteringTrainSetStoredOnlyUsersTF10.dat";*/
	        
	        storedt	 = p + "ft_clusteringTestSetStoredTF5.dat";			
			storedtr = p + "ft_clusteringTrainSetStoredTF5.dat";
	        
	        
			what 	 ="ALL sml with  10 fold cross validation in 80% of traing set for checking parameter sensitivity";
			fileNameToWrite = p + "\\Results2\\" + myType ;
		 }
		
		//users who saw > N movies
		//10-90
		//20-80
		//30-70
		
		if (whichCall.equalsIgnoreCase("TopNonSparse"))
			
			{
			
				p = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\TestTrain\\";
				f = p + "sml_storedRatings" + topNumber + ".dat";
			
			
				t  		 = p + "sml_testSetTop" + topNumber + "_" + (100-trainNumber) + ".dat";
				tr 		 = p + "sml_trainSetTop" + topNumber +  "_" + trainNumber + ".dat";
				
				storedt	 = p + "\\WrittenFiles\\" + "sml_testSetStoredTop" + topNumber + "_" + (100-trainNumber) + ".dat";
				storedtr = p + "\\WrittenFiles\\"+ "sml_trainSetStoredTop" + topNumber + "_" + trainNumber + ".dat";
				what 	 ="Top"  + topNumber +  "sml with " + trainNumber + " train and " + (100-trainNumber) +" test data";
				fileNameToWrite = p + "\\Results\\"+ myType + topNumber + "_" + trainNumber + ".dat";
			} 
			
		if (whichCall.equalsIgnoreCase("TopSparse")) 	//it is a sparse call with top 20 movies
			
			{
				p = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\Sparsity\\";
				f = p + "sml_storedRatings" + topNumber + ".dat";
			
			
				t  		 = p + "sml_testSetTop" + topNumber + "_" + (100-trainNumber) + ".dat";
				tr 		 = p + "sml_trainSetTop" + topNumber +  "_" + trainNumber + ".dat";
				storedt	 = p + "\\WrittenFiles\\"+"sml_testSetStoredTop" + topNumber + "_" + (100-trainNumber) + ".dat";
				
				//this should change in every step
				storedtr = p + "\\WrittenFiles\\" + "sml_trainSetStoredTop" + topNumber + "_" + trainNumber + "_" + (loopIndex *10) + ".dat";
				what 	 ="Top"  + topNumber +  "sml with " + trainNumber + " train and " + (100-trainNumber) +" test data" + (loopIndex*10) + "% Sparsity"; 
				mhh= new MemHelper(storedtr);
				ss = mySp.calculateSparsity(mhh); 
				fileNameToWrite = p + "\\Results\\" + myType + topNumber + "_" + trainNumber + "_" + ss + ".dat";
			
				
			}
		

	}//end of small
		
		
		else //ML 
		
		{
			
			

			//All users
			//10-90
			//20-80
			//30-70
			
	/*	if(top == false) 
		
		 {
			p = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\TestTrain\\";
			f = p + "ml_storedRatings.dat";
			
			    
		    t  		 = p + "ml_testSetAll.dat"+ "_" + (100-trainNumber) + ".dat";
	        tr 		 = p + "ml_trainSetAll.dat"+ "_" + trainNumber + ".dat";
			storedt	 = p + "ml_testSetStoredaAll" + "_" + (100-trainNumber) + ".dat";
			storedtr = p + "ml_trainSetStoredAll" + "_" + trainNumber + ".dat";
			what 	 ="ALL ml with " + trainNumber + " train and " + (100-trainNumber) +" test data";
			fileNameToWrite = p + "Results" + myType + "All" + "_" + trainNumber + ".dat";
		 }
		
		//users who saw > N movies
		//10-90
		//20-80
		//30-70
		
		else 
		{
			p = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\TestTrain\\";
			f = p + "ml_storedRatings.dat";
			
			t  		 = p + "ml_testSetTop" + topNumber + "_" + (100-trainNumber) + ".dat";
	        tr 		 = p + "ml_trainSetTop" + topNumber + "_" + trainNumber + ".dat";
			storedt	 = p + "ml_testSetStored" + topNumber + "_" + (100-trainNumber) + ".dat";
			storedtr = p + "ml_trainSetStoredTop20" + topNumber + "_" + trainNumber + ".dat";
			what 	 ="Top"  + topNumber +  "ml with " + trainNumber + " train and " + (100-trainNumber) +" test data";
			fileNameToWrite = p + "\\Results\\" + myType + topNumber + "_" + trainNumber + ".dat";
		}
	*/

			if (whichCall.equalsIgnoreCase("AllNonSparseNeighbour")) 
				 
			  {
			
				 p = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\Neighbourhood\\";
				 f = p + "ml_storedRatings.dat";
				
				    
			    t  		 = p + "ml_testSetAll.dat"+ "_" + (100-trainNumber) + ".dat";;
		        tr 		 = p + "ml_trainSetAll.dat" + "_" + trainNumber + ".dat";
				storedt	 = p + "\\WrittenFiles\\"+"ml_testSetStoredAll" + "_" + (100-trainNumber) + ".dat";			
				storedtr = p + "\\WrittenFiles\\"+"ml_trainSetStoredAll" + "_" + trainNumber + ".dat";
				what 	 ="ALL sml with " + trainNumber + " train and " + (100-trainNumber) +" test data";
				fileNameToWrite = p + "\\Results\\" + myType + "All" + "_" + trainNumber + ".dat";
			 }

			
			//all, non-sparse, cross
			if (whichCall.equalsIgnoreCase("AllNonSparseCrossNeighbour")) //here we will check cross validation
					 
				  {
					String neighbourz = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\TestTrain\\Neighbourhood\\";
					 p = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\ML_ML\\TestTrain\\FiveFoldData\\";
					 f = p + "ml_storedRatings.dat";
					
					    
				  //  t  		 = p + "sml_testSetAll.dat"+ "_" + (100-trainNumber) + ".dat";;
			       // tr 		 = p + "sml_trainSetAll.dat" + "_" + trainNumber + ".dat";
					
					 //we have everything in the folder we just need to call recommender function with suitable names
					storedt	 = p + "DataR1\\"+ "ml_testSetStoredFold" + loopIndex + ".dat";			
					storedtr = p + "DataR1\\"+ "ml_trainSetStoredFold" + loopIndex + ".dat";
					what 	 ="ALL ml with  10 fold cross validation in 80% of traing set for checking parameter sensitivity";
					//fileNameToWrite = neighbourz + "\\Results\\" + myType + "Fold" + loopIndex;
					fileNameToWrite = p+ "\\ResultsR1\\" + myType + "Fold" + loopIndex;
				 }
				

	}
		
		
			
		    
	}
	
	
/******************************************************************************************/
	
	public static void main (String arg[])
	
	{
	
		boolean smallCall	=true;
		boolean sparseCall	=false;
		boolean topCall  	=false;
		boolean crossCall  	=false;
		
		String myCallChoice ="";
		
		int topNo	 		=20;		
		int train			=80;		//by default for sparsity we are making 20-80%
		double sparseWrite  =0.0;
		
		//I am considering only small set and topset with top20
		
		SimpleCallRecommender rrr = new SimpleCallRecommender();
		
		
		SimpleCallRecommender cr = new SimpleCallRecommender(smallCall,
													sparseCall,
													topCall,
													topNo,
													train,
													0
													);
		
		
	      //_____________________________________________________________________
	     // make a sparse call 
		 //_____________________________________________________________________
		
	/*	
 			myCallChoice = "AllSparse";
 			train= 80;
							
				   for (int j = 0; j<100; j+=10) // train size = 90,80,70 ...
				
					
					{
											
						 cr.callsetUpFunction ( smallCall,		// sml_dataset
												myCallChoice,	// which type of data set to call and parameter to check for
												topNo,			// top20 or all
												train,			// 80
												j,				// loop index for sparse
												0				// for checking test train ratio
						 					  );
			
						cr.call();
					
					}

		*/		
			
	     	//_____________________________________________________________________
		     // 
			 //_____________________________________________________________________
			
			
			
	/*		else	//now we will simply play with sparse data (THis is called once) 
			  
			 {
				 for (int j = 1; j <10; j++) //there are 9 sparse dataset against each (i.e. all, and top20) 
				 
				 {	 
					    					 
					 cr.callsetUpFunction ( smallCall,
											sparseCall,
											topCall,
											crossCall,
											topNo,
											train,
											j 
										  );

						cr.call();
				 }
				 
			  }//end of sparse call
		*/		
			//_____________________________________________________________________
		     // corss validation with data set 20 test and 80 train 
			 // train is further divided into 10- fold corss validation
			 // to check up for paramter sensitivity
			 //_____________________________________________________________________
			
		
	/*	myCallChoice = "AllNonSparseNeighbour";
		
		for (int i=0; i<9; i++)				// for factor x (test train ration) x=0.1 means 10% test set
		{
		 for (int j = 0; j <10; j++)      //there are 10 folds 
			 
		 {	 
			    					 
			 cr.callsetUpFunction ( smallCall,
									myCallChoice,
					 				topNo,
									train,
									j+1,
									i+1
								  );

				cr.call();
		 }
		 
		}
		
		*/

		//_____________________________________________________________________
	     // 5- fold corss validation with data set 20 test and 80 trian 
		 // for neighbour hood size
		 //
		 //_____________________________________________________________________
		/*
		
		int howMuchFolds =5;
		myCallChoice = "AllNonSparseCrossNeighbour";
		smallCall =true;
		
		 for (int j = 0; j <howMuchFolds; j++)      //there are 10 folds 
			 
		 {	 
			    					 
			 cr.callsetUpFunction ( smallCall,		// sml_dataset
									myCallChoice,	// which type of data set to call and parameter to check for
					 				topNo,			// top20 or all
									train,			// 80
									j+1,			// fold index
									0				//used for sparsity cal
								  );

				cr.call();
		 }
	 
	
		
*/
		//_____________________________________________________________________
	    /* // 
		 // Just to check why user and movie average is going good than that of 
		 // predicited?
		 //_____________________________________________________________________
		

	    myCallChoice = "AllSimpleCheck";
		smallCall = true;
		train = 80;		
		{
			
			
		 cr.callsetUpFunction ( smallCall,		// sml_dataset
								myCallChoice,	// which type of data set to call and parameter to check for
				 				topNo,			// top20 or all
								train,			// 80
								0,				// fold index
								0				//used for sparsity cal
							  );

			cr.call();
		}*/
		

		 //_____________________________________________________________________
	     // 
		 // FT, simple 20-80 check
		 //_____________________________________________________________________
		

	 /*   myCallChoice = "AllSimpleCheckFT";
		smallCall = true;
		train = 80;
		
		{
		
		 cr.callsetUpFunction ( smallCall,		// sml_dataset
								myCallChoice,	// which type of data set to call and parameter to check for
				 				topNo,			// top20 or all
								train,			// 80
								0,				// fold index
								0				//used for sparsity cal
							  );

			cr.call();
		}
*/
	

		 //_____________________________________________________________________
	     // 
		 // Clustering check
		 //_____________________________________________________________________
		

/*	    myCallChoice = "AllSimpleCheckClusteringML";
		smallCall = true;
		train = 80;
		
		{
		
		 cr.callsetUpFunction ( smallCall,		// sml_dataset
								myCallChoice,	// which type of data set to call and parameter to check for
				 				topNo,			// top20 or all
								train,			// 80
								0,				// fold index
								0				//used for sparsity cal
							  );

			cr.call();
		}

*/
		
	    myCallChoice = "AllSimpleCheckClusteringML";
		smallCall = true;
		train = 80;
		
		{
		
		 cr.callsetUpFunction ( smallCall,		// sml_dataset
								myCallChoice,	// which type of data set to call and parameter to check for
				 				topNo,			// top20 or all
								train,			// 80
								0,				// fold index
								0				//used for sparsity cal
							  );

			cr.call();
		}


		

/*		 //_____________________________________________________________________
	     // 
		 // Clustering check
		 //_____________________________________________________________________
		

	    myCallChoice = "ContentBoosted";
		smallCall = true;
		train = 80;
		
		{
		
		 cr.callsetUpFunction ( smallCall,		// sml_dataset
								myCallChoice,	// which type of data set to call and parameter to check for
				 				topNo,			// top20 or all
								train,			// 80
								0,				// fold index
								0				//used for sparsity cal
							  );

			cr.call();
		}*/

/*		 //_____________________________________________________________________
	     // 
		 // ALL Simple check Book Crossing
		 //_____________________________________________________________________
		
	    myCallChoice = "AllSimpleCheckBC";
		smallCall = true;
		train = 80;
		
		{		
		 cr.callsetUpFunction ( smallCall,		// sml_dataset
								myCallChoice,	// which type of data set to call and parameter to check for
				 				topNo,			// top20 or all
								train,			// 80
								0,				// fold index
								0				//used for sparsity cal
							  );

			cr.call();
		}	
		*/
		
	}//end of main
	
	
/******************************************************************************************/
	
	public void call()	
	{		
		//objects of related classes
		DivideIntoSets div 					= new DivideIntoSets();
		SimpleDivideIntoSets Sdiv 			= new SimpleDivideIntoSets();
		MemReader       mr		    		= new MemReader ();
		MyRecommender   myRec 				= new MyRecommender();
		ContentBoostedRecommender   CBRec 	= new ContentBoostedRecommender();
		
		AnalyzeAFile    ana			= new AnalyzeAFile();
		
		double sparse =0.0;
			
/*	
		// divide main file into test and train file
		div.divideIntoTestTrain(f,							// main object already there
								tr, 						// training object to be written	
								t, 							// test object to be written	
								factor); 					// train-test propotion factor
		
	//	Sdiv.divideIntoTestTrain(f, tr, t, 0.8); 			//wtf....good results
		System.out.println("Done division");
	
		
		mr.writeIntoDisk(t, storedt); 							//write test set into memory
		mr.writeIntoDisk(tr, storedtr);							//write train set into memory
		
		
	//	ana.analyzeContent(storedtr);							//analyze the content of training object	
//		sparse = ana.calculateSparsity(storedtr);				//calculate sparsity level
		
		System.out.println("Done writing");
*/

		
		
/*
		myRec.makeCorrPrediction(storedtr,		 //training set object 
				storedt, 						 //test set object
				p, 								 //path where these objects are present
				what + ", Sparsity = "+ sparse,  //information about training set
				false, 							 //write Roc related results into the file?
				false,							 //write REComendation related results into the file?
				myType,							 //"simple", "deviation"
				myNeighbourSize,
				myNeighbourInc
				);		

	*/	
		
	/*	
		CBRec.makeCorrPrediction
								(storedtr,						 //training set object 
								 storedFilledtr,				 //filled train object	
								 storedt, 						 //test set object
								 p, 								 //path where these objects are present
								what + ", Sparsity = "+ sparse,  //information about training set
								false, 							 //write Roc related results into the file?
								true,							 //write REComendation related results into the file?
								myType,							 //"simple", "deviation"
								fileNameToWrite,				 //write results in this file
								myNeighbourSize,
								myNeighbourInc
								);		
	*/		

				myRec.makeCorrPrediction
								(storedtr,						 //training set object 
								 storedt, 						 //test set object
								 p, 								 //path where these objects are present
								what + ", Sparsity = "+ sparse,  //information about training set
								false, 							 //write Roc related results into the file?
								true,							 //write REComendation related results into the file?
								myType,							 //"simple", "deviation"
								fileNameToWrite,				 //write results in this file
								myNeighbourSize,
								myNeighbourInc
								);		
		
	}
	
/*****************************************************************************************************/
	
	
	
}
