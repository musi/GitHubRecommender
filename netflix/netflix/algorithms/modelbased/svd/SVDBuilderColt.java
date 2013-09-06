package netflix.algorithms.modelbased.svd;

import java.util.*;


import java.io.*;
import netflix.algorithms.memorybased.memreader.FilterAndWeight;
import netflix.memreader.*;
import netflix.recommender.ItemItemRecommender;
import netflix.recommender.NaiveBayesRec;
import netflix.utilities.*;
import netflix.weka.TextNBRecWriter;
import netflix.weka.TextPartialWriter;
import netflix.weka.Writers.IBKWriter;
import netflix.weka.Writers.NBWriter;
import netflix.weka.Writers.SVMRegWriter;
import netflix.weka.Writers.SVMWriter;
import netflix.weka.Writers.ZeroRWriter;
import cern.colt.matrix.*;
import cern.colt.matrix.linalg.*;
import cern.colt.matrix.impl.*;

/**
 * This class uses the colt library's 
 * SingularValueDecomposition class to create
 * and serialize an SVD object for the movielens
 * data. The program takes 4 arguments:
 *     1) The number of movies.
 *     2) The number of users.
 *     3) The file containing the MemReader.
 *     4) the file to write the SVD to. 
 * The program requires that the number of 
 * movies and number of users be explicitely input
 * to allow for both 0 and 1 indexed datasets. 
 *
 * @author sowellb
 */
class SVDBuilderColt 
{

	BufferedWriter      writeData;
	String    			myPath="";
	boolean 			FOLDING;
	boolean 			FTFlag;
	boolean				sparse;
	int 				myClasses;
	String				normalization;					// user, mov, or simple
	int 				minUserAndMov;
	
	String    			datFile="";
    String    			testFile="";
    String    			destFile="";
    String				mainFile ="";
    String  			modelName ="";					//model name to be written
    String              trainingOrFullSet = "";			//full fold data is to be svded or training set of a fold is to be svded
    String				FTInfo;
    int					xFactor;
    int					dataset;
    
    MemHelper 					helper;					//train object
    Random              		myRand;
    FilterAndWeight     		myFilter; 				//user-based CF
    ItemItemRecommender			myItemRec;		    	//item-based CF    
    Algebra						myAlg; 
    
    //check statistics
    int 						totalFound;				// Samples, in which SVM was successful
    int 						totalNotFound;
	
    //Related to cold start
	int 		coldStartUsers;
	int 		coldStartThreshold;
	boolean 	newUserScenario;
	
    //--------------------------------------------
    
	public SVDBuilderColt(int myClasses, boolean folding, boolean FTFlag,  String trainingOrFullSet)
	{
		 myPath   = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/";
        
      // datFile  = myPath + "sml_clusteringTrainSetStoredTF.dat";
	 //  datFile  = myPath + "sml_storedFeaturesRatingsTF.dat";
      // destfile = myPath + "SVDStoredColt.dat";
      // destFile = myPath + "SVDStoredColtI.dat";	
         
        myRand  				= new Random();
        myAlg					= new Algebra();
        this.myClasses			= myClasses;			//classes=5, SML, ML; 10=FT
        FOLDING 				= folding;				//folding flag
        this.FTFlag				= FTFlag;				//Ft flag
        this.trainingOrFullSet 	= trainingOrFullSet;
        
        totalFound				= 0;				// Samples, in which SVM was successful
        totalNotFound			= 0;
        
  
	}
	
/****************************************************************************************************/
	/**
	 * Main Method
	 */
	
	public static void main(String args[]) 
    {
		// String trainingOrFullSet = "training";
		 String trainingOrFullSet = "full";
		
		 SVDBuilderColt mySVD = new SVDBuilderColt(5,		 			//Classes 
												   true, 	 			//Folding	
												   true, 	 			//FT
												   trainingOrFullSet); 	//trainingOrFullSet
		mySVD.prepareModelParameters();
    }

/****************************************************************************************************/
	
	/**
	 * Prepare Model parameters
	 */
	
	public void prepareModelParameters ()
	{	
		//Related to cold start
		coldStartUsers 		= 50;
		coldStartThreshold	= 5;
		//newUserScenario     = true;
		newUserScenario     = false;
		
		int myEnd = 0;
		dataset   =	2;			//1=sml, 2=ft, 3=ml
		sparse 	  = false;		//Sparsity building, under diff sparse datasets	
		 
		if(FTFlag==false)
			myEnd = 1;
		else
			myEnd = 3;
		
	for(int m = 1 ;m<=1;  m++)
	{
		if(m==1)
			minUserAndMov = 1;
		else if(m==2)
			minUserAndMov = 2;
		else if(m==3)
			minUserAndMov = 5;
		else if(m==4)
			minUserAndMov = 10;
		
	  //build model twice, one for full and other for training set only
	 for(int trainOrFull =1;trainOrFull<=1;trainOrFull++)
	  {
		 if(trainOrFull ==0)
			 trainingOrFullSet = "training";
		 else
			 trainingOrFullSet = "full";	
		
		//Normalizations
		for (int x = 2;x<=2;x++)
		{
			if(x==0)
				normalization = "simple";
			
			else if (x==1){
				normalization = "mov";
				continue;									//we do not want this, continue
			}
			
			else if( x==2)					
				normalization = "user";
			
			else if (x==3)
				normalization = "sigma";
			
			// XFactor (Training size)
			for(int j=80;j<=80;j+=20)			//111 is for new user cold start
			{			 
				/*if(j==80)
					j =90;
				*/
				
				xFactor = j;
				
				//for cold start
				for(int thr=2;thr<=2;thr+=5)
				{
					if(thr==25)
						continue;
					
					coldStartThreshold = thr;
					
					//Model imputation source
					 for(int i=9;i<=10;i++)
					 {	
						 
						if(i<=1) continue;
						if(i>=5 && i<=6) continue;
						
					
						
						 buildModels(i);
						
					 }
				}
			} //end of midlle for
		  } //end of for of normalization
		}//end of full or training 		
	}//end of outer for (for minUser and mov)
  }
	
/****************************************************************************************************/
/**
 *  Build models, may be 1-fold or 5-fold
 */
	
	public void buildModels(int whichModel)
	{
		Timer227 timer = new Timer227();
		  
		int 	 numMovies 			= 0;
        int 	 numUsers  			= 0;
        double   sigma     			= 1.0;				// We have to divide by it
        String   myPath	   			= "";        
        int 	 rows      			= 0;
        int 	 cols 	   			= 0;
        int      epoch     			= 0;
        int      sparsityLevel      = 0;				// Control the sparse training set object        
        int      sparsityLoopLimit  = 0;				// Control the sparse training set object
        String   sparseObjName 		= "";				// we will write memReader obj
        String   sparseDummyWriter 	= "";				// we will write prediction here

        //Number of times, main-loop will run, in case it is spare/not sparse
        if(sparse==false)
        	sparsityLoopLimit = 1;
        else
        	sparsityLoopLimit = 10;
        
        
     
     
   for( sparsityLevel = 1; sparsityLevel<=sparsityLoopLimit; sparsityLevel++)
   {
	   System.gc();
	   
	   //reset variable
       totalNotFound = 0;
       totalFound = 0;
       
        //determine folding and simple 20-80 settings
        if(FOLDING == false){		 
        	epoch =1;						// No. of times a loop will run
        			
        	if(dataset==1)	{
        		if(sparse ==false){
        			//usual 
	        		// myPath = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/" + xFactor + "/";
	        	 	
	        		 //feature play
	    //    		 myPath  = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/FeaturesPlay/"+ xFactor + "/";
        		
	        	 	//New User Cold-Start
	          		myPath = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/FiveFoldData/"+ xFactor + "/";
	          		
        		}
        		
        		else{ //sparse dataset
        			myPath  = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/" +xFactor + "/";
        		}
        	}
        	
        	else if(dataset==2)
        		myPath = "C:/Users/Musi/workspace/MusiRecommender/DataSets/FT/SVD/"+ xFactor + "/";
        
        	else
        		  myPath  = "C:/Users/Musi/workspace/MusiRecommender/DataSets/ML_ML/SVD/"+ xFactor + "/";
        	
        }
        
        //-------------------------------------------------------------------
        //Folding
        else{
        	epoch =5;						// No. of times a loop will run
			
        	if(dataset==1){					// Path		
        		
        		//usual
        	 	//myPath = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/FiveFoldData/"+ xFactor + "/";
        	 
        	//	//feature play
        		//myPath = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/FeaturesPlay/";
        		
        		//New User Cold-Start
        		myPath = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/FiveFoldData/"+ xFactor + "/";
        		
        	}
        	
        	else if(dataset==2)
        		myPath = "C:/Users/Musi/workspace/MusiRecommender/DataSets/FT/SVD/FiveFoldData/"+ xFactor + "/";
        		
        	else
        		myPath = "C:/Users/Musi/workspace/MusiRecommender/DataSets/ML_ML/SVD/"+ xFactor + "/";
        		
        }
 
  //Build Model     
  try { 	
      
       for (int fold=2;fold <=epoch;fold++)
       {
           if(FOLDING == false) // No folding
           {                   	   
	        	//SML   
	        	if(dataset==1)
	        	{	        	   
	        	
	        	myClasses = 5;
	        		
	        	//Usual
	        	datFile   = myPath + "sml_clusteringTrainSetStoredTF.dat";
	            testFile    = myPath + "sml_clusteringTestSetStoredTF.dat";
	            
	          if(sparse==false){	
	        	  
	        	 //Feature Play		 	      
	            /*datFile     = myPath + "sml_clusteringTrainSetStoredTF.dat";
	            testFile    = myPath + "sml_clusteringTestSetStoredTF.dat";
	            mainFile    = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/FeaturesPlay/" + "sml_storedFeaturesRatingsTF.dat";
	            */
	            
	        	//New User Cold-Start
	        	if(newUserScenario == true)
	        	{
		            datFile     = myPath + "sml_clusteringTrainSetStoredTF"+coldStartUsers+"_"+coldStartThreshold+".dat";
		            testFile    = myPath + "sml_clusteringTestSetStoredTF"+coldStartUsers+".dat";
		            mainFile    = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/FeaturesPlay/" + "sml_storedFeaturesRatingsTF.dat";
	        	     
		            //for feature play, cold-start scenarions
		            FileWriter dummy = new FileWriter (myPath + "/DummySparse/dummySparseFile"+ whichModel+ "_"+ coldStartThreshold+".dat");	            
		            sparseDummyWriter = myPath + "DummySparse/dummySparseFile"+ whichModel+ "_"+ coldStartThreshold+".dat";            
		            sparseObjName = myPath + "DummySparse/sml_storedSparseRatingsTF_" + whichModel + "_"+ coldStartThreshold+".dat";
		           
	        	
	        	}       		
	           
	        	//for feature play usual one
/*	            FileWriter dummy = new FileWriter (myPath + "/DummySparse/dummySparseFile"+ whichModel+ ".dat");	            
	            sparseDummyWriter = myPath + "DummySparse/dummySparseFile"+ whichModel+ ".dat";            
	            sparseObjName = myPath + "DummySparse/sml_storedSparseRatingsTF_" + whichModel + ".dat";
	           */
	           
	
	        	
	          }
	          
	         /* else{	//Sparse Ratings
	        	    //Diff sparse set each time
	        	   datFile     = myPath + "SparseRatings/sml_trainSetStoredAll_"+ sparsityLevel + ".dat";   	   
		           testFile    = myPath + "SparseRatings/sml_clusteringTestSetStoredTF.dat";
		           mainFile    = myPath + "SparseRatings/sml_storedFeaturesRatingsTF.dat";
		           
		           FileWriter dummy = new FileWriter (myPath + "SparseRatings/DummySparse/dummySparseFile"+ whichModel+ "_"+ sparsityLevel+ ".dat");	            
		           sparseDummyWriter = myPath + "SparseRatings/DummySparse/dummySparseFile"+ whichModel+ "_"+sparsityLevel +".dat";            
		           sparseObjName = myPath + "SparseRatings/DummySparse/sml_storedSparseRatingsTF_" + whichModel +"_"+ sparsityLevel+ ".dat";
		           
	          }	 */           
	            
	            
	            numMovies 	= 1682;
		        numUsers 	= 943;
		        rows        = numMovies;
		        cols        = numUsers;
		        FTFlag		= false;	 
	        	
	        	}
	        	 
	        	 //FT 
	        	 else if(dataset==2)
	        	 {		             
	        		 if(trainingOrFullSet.equalsIgnoreCase("full")){
 		  	        	  
	        			  FTInfo = "Both";	 	        		
	 	        		 // FTInfo = "OnlyUsers";
	 	        		  //  FTInfo = "NoNor";
	 	        		
	        			// FTInfo = "";
	        			 
	 		        	datFile     = myPath + "ft_clusteringTrainSetStored"+ FTInfo+ "TF"+ minUserAndMov + ".dat";
	 		            testFile    = myPath + "ft_clusteringTestSetStored"+ FTInfo+ "TF"+ minUserAndMov + ".dat";
	 		            mainFile    = myPath + "ft_modifiedStoredFeaturesRatings"+ FTInfo+ "TF"+ minUserAndMov + ".dat";
	 	        	}
	 	        	
	 	        	else{
	 	        		
	 	        		   FTInfo = "Both";	 	        		
	 	        		 //  FTInfo = "OnlyUsers";
	 	        		 //  FTInfo = "NoNor";
	 	        		
	 	        		datFile   = myPath + "ft_clusteringTrainingTrainSetStored"+ FTInfo+ "TF"+ minUserAndMov + ".dat";
	 		            testFile  = myPath + "ft_clusteringTrainingValSetStored"+ FTInfo+ "TF"+ minUserAndMov + ".dat";
	 		            mainFile  = myPath + "ft_modifiedStoredTrainingFeaturesRatings"+ FTInfo+ "TF"+ minUserAndMov + ".dat";
	 	            
	 	        	}
	 	        	
	 	           MemHelper mainHelper = new MemHelper(mainFile);         
	 	           numMovies 	= mainHelper.getNumberOfMovies();			
	 	           numUsers 	= mainHelper.getNumberOfUsers(); 	            
	 	            
	 	           System.out.println("Num Mov="+ numMovies);
		           System.out.println("Num Users="+ numUsers);
		            
		           /*numMovies 	= 1922;			
	 	           numUsers 	= 1214;	 */	           
	 	                   
	                FTFlag		= true;	    
	                myClasses   = 10;
	    
	           }       	
	        	 else
	        	 {
	        		 myClasses = 5;
		        		
	        		 FTFlag		= false;
	 	          	 //Feature Play		 	      
	 	            datFile     = myPath + "ml_clusteringTrainSetStoredTF.dat";
	 	            testFile    = myPath + "ml_clusteringTestSetStoredTF.dat";
	 	            mainFile    = myPath + "ml_storedFeaturesRatingsTF.dat";

	 	            
	 	      /*      FileWriter dummy = new FileWriter (myPath + "/DummySparse/dummySparseFile"+ whichModel+ ".dat");	            
	 	            sparseDummyWriter = myPath + "DummySparse/dummySparseFile"+ whichModel+ ".dat";            
	 	            sparseObjName = myPath + "DummySparse/sml_storedSparseRatingsTF_" + whichModel + ".dat";
	 	            
	 	        	*/
		 	           MemHelper mainHelper = new MemHelper(mainFile);         
		 	           numMovies 	= mainHelper.getNumberOfMovies();			
		 	           numUsers 	= mainHelper.getNumberOfUsers(); 	            
		 	            
		 	           System.out.println("Num Mov="+ numMovies);
			           System.out.println("Num Users="+ numUsers);
	        		 
	        	 }
	        	
           } //end if
           
           else	//For Folding case.....only dat file and the dest file will change
        	{
	        	//SML   
		        if(dataset==1)
		        {       	  
		        	myClasses = 5;
		        	
		        	//determien full set or train set of a fold is to be svded
		           if(trainingOrFullSet.equalsIgnoreCase("full")){		        	
			          
		        	   //Usual
		        	  datFile   = myPath  + "sml_trainSetStoredFold" +(fold) + ".dat";   	 
		 	       	  testFile  = myPath + "sml_testSetStoredFold" +(fold) + ".dat";		 	       	  
		     
		       	 }
		        	
		        	else{ 
		 	          datFile   = myPath  + "sml_trainingTrainSetStoredFold" +(fold) + ".dat";   	 
		 	       	  testFile  = myPath + "sml_trainingValSetStoredFold" +(fold) + ".dat";
		        	}
		 	      
		          numMovies	= 1682;
		 	      numUsers 	= 943;
		 	 
		 	      FTFlag	= false;	 
	 	         }
		          
	        	 //FT 
	        	else if(dataset==2)
	        	{	           
	        
	        	myClasses = 10;
	        	  
	        	if(trainingOrFullSet.equalsIgnoreCase("full")){
	        		  	        		  	        	  
		        /*	datFile     = myPath + "ft_clusteringTrainSetStoredTF"+ minUserAndMov + ".dat";
		            testFile    = myPath + "ft_clusteringTestSetStoredTF"+ minUserAndMov + ".dat";
		            mainFile    = myPath + "ft_modifiedStoredFeaturesRatingsTF"+ minUserAndMov + ".dat";
	        		*/
	        		
	        		datFile   = myPath  + "ft_trainSetStored" + "Both" + "Fold"+ minUserAndMov + (fold)+ ".dat";   	 
		 	       	testFile  = myPath + "ft_testSetStored" + "Both"  + "Fold"+ minUserAndMov +(fold)+ ".dat";		 	       	  
		 	       	mainFile    = myPath + "ft_storedFeaturesRatingsBothTF"+ minUserAndMov + ".dat";
			        
		 	       	  
	        	}
	        	
	        	else{
	        		
	        		datFile     = myPath + "ft_clusteringTrainingTrainSetStoredTF"+ minUserAndMov + ".dat";
		            testFile    = myPath + "ft_clusteringTrainingValSetStoredTF"+ minUserAndMov + ".dat";
		            mainFile    = myPath + "ft_modifiedStoredTrainingFeaturesRatingsTF"+ minUserAndMov + ".dat";
	            
	        	}
	        	
	        /*	//MemHelper mainHelper = new MemHelper(mainFile);			 
	            numMovies 	= mainHelper.getNumberOfMovies();			
	            numUsers 	= mainHelper.getNumberOfUsers();*/

	            numMovies 	= 1922;			
	            numUsers 	= 1214;
            
	            FTFlag		= true;
	            myClasses   = 10;
	           	
	           }    	
         
	        	else
	        	{
	        		myClasses = 5;
		        	
		        	//determien full set or train set of a fold is to be svded
		           if(trainingOrFullSet.equalsIgnoreCase("full")){		        	
			          
		        	   //Usual
		        	  datFile   = myPath  + "ml_trainSetStoredFold" +(fold) + ".dat";   	 
		 	       	  testFile  = myPath + "ml_testSetStoredFold" +(fold) + ".dat";		 	       	  
		     
		       	 }
		        	
		        	else{ 
		 	          datFile   = myPath  + "ml_trainingTrainSetStoredFold" +(fold) + ".dat";   	 
		 	       	  testFile  = myPath + "ml_trainingValSetStoredFold" +(fold) + ".dat";
		        	}
			       	
		            MemHelper mainHelper = new MemHelper(mainFile);			 
		            numMovies 	= mainHelper.getNumberOfMovies();			
		            numUsers 	= mainHelper.getNumberOfUsers();
 
		 	      FTFlag	= false;	 
	 	    		
	        	}
        	}//end else
           
            System.out.println("Mov =" +numMovies + ", users=" +numUsers);
            	      
            //Create some objects, like memhelper, CF etc.
            helper = new MemHelper(datFile);			 	 //MemReader File	
            myFilter = new FilterAndWeight(helper,1); 		 //FilterAndWeight, Random object, For CF
            MemHelper sparseHelper = null;    	   
            myItemRec = new ItemItemRecommender(true, 5);     //Item based CF     
            	

            //-------------------------------------------------------------------------------
            //We will call the respective mehtod one by one, and it will
            //write the obj into memory, then we make sparse object and 
            //get missing predictions in the training set from there.
            
   /*       //Text Rec
            //TextRecWriter  mySVM = new TextRecWriter(mainFile, datFile, testFile, sparseDummyWriter, sparseObjName);	//main, train, test
              SVMWriter  mySVM = new SVMWriter(mainFile, datFile, testFile, sparseDummyWriter, sparseObjName);	//main, train, test
              NBWriter  myNB = new NBWriter(mainFile, datFile, testFile, sparseDummyWriter, sparseObjName);	//main, train, test
           
              IBKWriter  myIBK = new IBKWriter(mainFile, datFile, testFile, sparseDummyWriter, sparseObjName);	//main, train, test
              ZeroRWriter  myZero = new ZeroRWriter(mainFile, datFile, testFile, sparseDummyWriter, sparseObjName);	//main, train, test
         
            
            TextRecRegWriter mySVMReg = new TextRecRegWriter(mainFile, datFile, testFile, sparseDummyWriter, sparseObjName);	//main, train, test            
         */
            //SVMRegWriter  mySVMReg = new SVMRegWriter(mainFile, datFile, testFile, sparseDummyWriter, sparseObjName);	//main, train, test
            
           	//TextNBRecWriter  myClassifer = new TextNBRecWriter(mainFile, datFile, testFile, sparseDummyWriter, sparseObjName, 1);	//main, train, test
            
           /* if(whichModel ==11)
            {
                myNB.GeneratePrediction();         		          	
         	    sparseHelper = new MemHelper (sparseObjName);
            }
            
            else if(whichModel==12)
            {
          	    mySVM.GeneratePrediction();         		          	
         	    sparseHelper = new MemHelper (sparseObjName);
            	
            } */
            
          /*   if(whichModel ==13) 									
            {
      
            	  mySVMReg.GeneratePrediction();
            	//myClassifer.GeneratePrediction();    		          	
            	 sparseHelper = new MemHelper (sparseObjName);
            }            	 
            */
            
            /*
            else if(whichModel==14)
            {
          	    myIBK.GeneratePrediction();         		          	
         	    sparseHelper = new MemHelper (sparseObjName);
            	
            }
            
            else if(whichModel==15)
            {
          	    myZero.GeneratePrediction();         		          	
         	    sparseHelper = new MemHelper (sparseObjName);
            	
            }
            */
            //Make data Matrix
            double[][] data  = new double[numMovies][numUsers]; //For SVD m>=n
            double rating;       
             	
            //----------------------------------	
            //Start Filling the matrix 

            // Ft, SML, i = movies, j = users   .... so use ML conventions
            // For Ft when users>mov, take transpose of the matrix to make svd condition to be satisfied
            // i.e. rows>cols
            
            for(int i = 0; i < numMovies; i++) 
            {
                for(int j = 0; j < numUsers; j++) 
                {
                    rating = helper.getRating(j+1, i+1);	//uid, mid

                    // Wanna normalized or not?
                    double nor =0;
                    if(normalization.equalsIgnoreCase("user"))  {   		// user norm
                         nor = helper.getAverageRatingForUser(j+1);
                         sigma = 1.0;
                    }
                    else if(normalization.equalsIgnoreCase("mov")){  	// mov norm
                    	 nor = helper.getAverageRatingForMovie(i+1);
                    	 sigma = 1.0;
                    }
                    else  if (normalization.equalsIgnoreCase("simple")){  	// no norm     
                    	 nor = 0;
                    	 sigma = 1.0;
                    }
                    else  if (normalization.equalsIgnoreCase("sigma")){  	// sigma norm ((rating - avgRating)/sigma)     
                    	nor = helper.getAverageRatingForUser(j+1);
                    	sigma = helper.getStandardDeviationForUser(j+1);
                    	if(sigma ==0.0)
                    		sigma =1.0;
                    }
                   
               
                    //If the user did not rate the movie  (Here use content based or some other heuristic)
                    if(rating == -99) 
                    {                    	
                    	//Zeros
                    	if(whichModel==0) {
                    		data[i][j] = 0 - nor/sigma;
                    		modelName  = "SVDZeros";
                    	}
                    	
                    	//Random No. (0.0-1.0) * 5(ML) or 10(FT)
                    	else if(whichModel==1) {
                    		data[i][j] = myRand.nextDouble() * myClasses  - nor/sigma;
                    		modelName  = "SVDRandom";
                    	}
                    	
                        // Movie Average
                    	else if(whichModel==2) {
                    		data[i][j] = helper.getAverageRatingForMovie(i+1) -   	nor/sigma;
                    		modelName  = "SVDMovAvg";
                    	}
                    	
                    	// User Average
                    	else if(whichModel==3) {
                    		data[i][j] = helper.getAverageRatingForUser(j+1)-
                    		nor/sigma;
                    		modelName  = "SVDUserAvg";
                    	}
                    	
                    	// (Movie + User /2)
                    	else if(whichModel==4) {
	                    	data[i][j] = ((helper.getAverageRatingForMovie(i+1)
	                    			+ helper.getAverageRatingForUser(j+1) )/2.0) -      nor/sigma;
	                    	modelName  = "SVDMovAndUserAvg";
                    	}
                    	
                    	// Uniform Distribution
                    	else if(whichModel==5) {
	                    	data[i][j] = cern.jet.random.Uniform.staticNextDoubleFromTo(1.0, myClasses) - nor/sigma;
	                   
	                    	modelName  = "SVDUniform";	
                    	}

                    	// Normal Distribution (UU)
                    	else if(whichModel==6) {
	                    	data[i][j] = cern.jet.random.Normal.staticNextDouble( helper.getAverageRatingForUser(j+1), 
	                    			helper.getStandardDeviationForUser(j+1))- nor/sigma;
	                    	modelName  = "SVDuserNormal";
                    	}

                    	// Normal Distribution (MM)
                    	else if(whichModel==7) {
	                    	data[i][j] = cern.jet.random.Normal.staticNextDouble( helper.getAverageRatingForMovie(i+1), 
	                    			helper.getStandardDeviationForMovie(i+1))- nor/sigma;
	                    	modelName  = "SVDMovNormal";
                    	}
                    	
                                        	
                    	// User-Based CF
                    	else if(whichModel==8) {
                    		double temp;
                    		
                    		if(FTFlag)
                    			temp = myFilter.recommendS(j+1, i+1, 20, 1);
                    		else
                    			temp = myFilter.recommendS(j+1, i+1, 70, 1);
	                    	data[i][j] = temp - nor/sigma;
	                    	modelName  = "SVDUserBasedCF";
                    	}
                    	
                    	// Item-Based CF
                    	else if(whichModel==9) {
                    		double temp =0;
                    		
                    		if(FTFlag)
	                    		temp = myItemRec.recommend(helper, j+1, i+1, 5, 4);
                    		else
                    			temp = myItemRec.recommend(helper, j+1, i+1, 10, 30);
	                    	data[i][j] = temp - nor/sigma;
	                    	modelName  = "SVDItemBasedCF";
                    	}

                    	//(User+Item CF)/2
                    	else if(whichModel==10) {
	                    	double tempU, tempI;
	                    	
	                    	if(FTFlag){
                    		 tempU = myFilter.recommendS(j+1, i+1, 30, 1);
	                    	 tempI = myItemRec.recommend(helper, j+1, i+1, 5, 4); //train obj, uid, mind, neigh, alpha
	                    	}
	                    	
	                    	else{
	                    		tempU = myFilter.recommendS(j+1, i+1, 70, 1);
		                    	 tempI = myItemRec.recommend(helper, j+1, i+1, 10, 30); //train obj, uid, mind, neigh, alpha
	                    	}
	                    	
	                    	data[i][j] = (tempU + tempI)/2 - nor/sigma;
	                    	modelName  = "SVDUserAndItemBasedCF";
                    	}
                    
                    	//Naive Bayes
                    	else if(whichModel==11) {                    	
                    		double tempN = sparseHelper.getRating(j+1, i+1);
                    		if(tempN==-99 || tempN>myClasses || tempN <=0)
                    							tempN = helper.getAverageRatingForMovie(i+1);
                    		//System.out.println("tempN="+ tempN);
                    		data[i][j] = tempN - nor/sigma;
                    		modelName  = "SVDNB";
                    	}             	
                        
                    	//SVM
                    	else if(whichModel==12) {                    	
                    		double tempN = sparseHelper.getRating(j+1, i+1);
                    		if(tempN==-99 || tempN>myClasses  || tempN <=0){
                    							tempN = helper.getAverageRatingForMovie(i+1);
                    							totalNotFound++;
                    			}
                    		else
                    			totalFound++;
                    		
                    		//System.out.println("tempN="+ tempN);
                    		data[i][j] = tempN - nor/sigma;
                    		modelName  = "SVDSVM";
                    	}
                    	
                     	//SVM Reg
                    	else if(whichModel==13) {                    	
                    		double tempN = sparseHelper.getRating(j+1, i+1);
                    		if(tempN==-99 || tempN>myClasses  || tempN <=0){
                    							tempN = helper.getAverageRatingForMovie(i+1);
                    							totalNotFound++;
                    			}
                    		else
                    			totalFound++;
                    		//System.out.println("tempN="+ tempN);
                    		data[i][j] = tempN - nor/sigma;
                    		modelName  = "SVDSVMReg";
                    	}
                    	
                    	
                       	//IBK Lazy
                    	else if(whichModel==14) {                    	
                    		double tempN = sparseHelper.getRating(j+1, i+1);
                    		if(tempN==-99 || tempN>myClasses  || tempN <=0)
                    							tempN = helper.getAverageRatingForMovie(i+1);
                    		//System.out.println("tempN="+ tempN);
                    		data[i][j] = tempN - nor/sigma;
                    		modelName  = "SVDIBK";
                    	}                    	
                    	
                      	
                       	//Zero
                    	else if(whichModel==15) {                    	
                    		double tempN = sparseHelper.getRating(j+1, i+1);
                    		if(tempN==-99 || tempN>myClasses  || tempN <=0)
                    							tempN = helper.getAverageRatingForMovie(i+1);
                    		//System.out.println("tempN="+ tempN);
                    		data[i][j] = tempN - nor/sigma;
                    		modelName  = "SVDZeroR";
                    	}
                    	
                    	/*//------------------------
                       	//NB Multi Nomian
                    	else if(whichModel==15) {                    	
                    		double tempN = sparseHelper.getRating(j+1, i+1);
                    		if(tempN==-99 || tempN>myClasses  || tempN <=0)
                    							tempN = helper.getAverageRatingForMovie(i+1);
                    		//System.out.println("tempN="+ tempN);
                    		data[i][j] = tempN - nor/sigma;
                    		modelName  = "SVDNBM";
                    	}
                    	
                    	//J48
                    	else if(whichModel==16) {                    	
                    		double tempN = sparseHelper.getRating(j+1, i+1);
                    		if(tempN==-99 || tempN>myClasses  || tempN <=0)
                    							tempN = helper.getAverageRatingForMovie(i+1);
                    		//System.out.println("tempN="+ tempN);
                    		data[i][j] = tempN - nor/sigma;
                    		modelName  = "SVDJ48";
                    	}
                    	
                     	//Ada Boost
                    	else if(whichModel==17) {                    	
                    		double tempN = sparseHelper.getRating(j+1, i+1);
                    		if(tempN==-99 || tempN>myClasses  || tempN <=0)
                    							tempN = helper.getAverageRatingForMovie(i+1);
                    		//System.out.println("tempN="+ tempN);
                    		data[i][j] = tempN - nor/sigma;
                    		modelName  = "SVDAda";
                    	}
                    	                    	
                     	//Bagging
                    	else if(whichModel==18) {                    	
                    		double tempN = sparseHelper.getRating(j+1, i+1);
                    		if(tempN==-99 || tempN>myClasses  || tempN <=0)
                    							tempN = helper.getAverageRatingForMovie(i+1);
                    		//System.out.println("tempN="+ tempN);
                    		data[i][j] = tempN - nor/sigma;
                    		modelName  = "SVDBagging";
                    	}
                    	
                 
                    	//---------------
                     	//Linear Reg
                    	else if(whichModel==19) {                    	
                    		double tempN = sparseHelper.getRating(j+1, i+1);
                    		if(tempN==-99 || tempN>myClasses  || tempN <=0)
                    							tempN = helper.getAverageRatingForMovie(i+1);
                    		//System.out.println("tempN="+ tempN);
                    		data[i][j] = tempN - nor/sigma;
                    		modelName  = "SVDSVM";
                    	}
                    	
                    	
                    	
                     	//Logistic Reg
                    	else if(whichModel==20) {                    	
                    		double tempN = sparseHelper.getRating(j+1, i+1);
                    		if(tempN==-99 || tempN>myClasses  || tempN <=0)
                    							tempN = helper.getAverageRatingForMovie(i+1);
                    		//System.out.println("tempN="+ tempN);
                    		data[i][j] = tempN - nor/sigma;
                    		modelName  = "SVDSVM";
                    	}
                    	
                    
                     	//Winnow
                    	else if(whichModel==20) {                    	
                    		double tempN = sparseHelper.getRating(j+1, i+1);
                    		if(tempN==-99 || tempN>myClasses  || tempN <=0)
                    							tempN = helper.getAverageRatingForMovie(i+1);
                    		//System.out.println("tempN="+ tempN);
                    		data[i][j] = tempN - nor/sigma;
                    		modelName  = "SVDSVM";
                    	}
                    	
                     	//SVM
                    	else if(whichModel==21) {                    	
                    		double tempN = sparseHelper.getRating(j+1, i+1);
                    		if(tempN==-99 || tempN>myClasses  || tempN <=0)
                    							tempN = helper.getAverageRatingForMovie(i+1);
                    		//System.out.println("tempN="+ tempN);
                    		data[i][j] = tempN - nor/sigma;
                    		modelName  = "SVDSVM";
                    	}    	
                    	*/
                    	//--------------------------
                    	// less imp concepts
                    	//--------------------------
                    	
                    	// Normal Distribution (UM)
                    	else if(whichModel==13) {
	                    	data[i][j] = cern.jet.random.Normal.staticNextDouble( helper.getAverageRatingForUser(j+1), helper.getStandardDeviationForMovie(i+1))- 
	                    	nor/sigma;
	                    	modelName  = "SVDUserMovNormal";
                    	}

                    	// Normal Distribution (MU)
                    	else if(whichModel==14) {
	                    	data[i][j] = cern.jet.random.Normal.staticNextDouble( helper.getAverageRatingForMovie(i+1), helper.getStandardDeviationForUser(j+1))-
	                    	nor/sigma;
	                    	modelName  = "SVDMovUserNormal";
                    	}

                    }
                    
                    // We have ratings, insert them into matrix
                    else 
                    {
                         data[i][j] = rating - nor/sigma;  //normalize?
                    	// data[i][j] = rating;
                    }

                    if(rating==0)
                    	System.out.println("rating =0");
                }
            } //end of movie for

            //----------------------------------
            // Construct SVD 
            
            //Constructs a matrix with the given cell values
            //Use idioms like DoubleFactory2D.dense.make(4,4) to construct dense matrices
            DenseDoubleMatrix2D matrix = (DenseDoubleMatrix2D) DoubleFactory2D.dense.make(data);
          
           //checkTheMethodMake();
            
            System.out.println("total not found="+totalNotFound);
            System.out.println("total found="+totalFound);
            
            //SVD
            timer.start();
            SingularValueDecomposition svd;            
            if(dataset==1)	 
            	svd  =  new SingularValueDecomposition(matrix);
            
            //control, the row x cols of svd according to the dimension of matrix
            //condition row > cols in svd
            //so reverse it to user = row, or mov = row accordingly
            else if(dataset==2){              	
            	if(	minUserAndMov <=1 )        		
            		svd  =  new SingularValueDecomposition(matrix);
            	else
            	      svd  =  new SingularValueDecomposition(myAlg.transpose(matrix));
            	  //  svd  =  new SingularValueDecomposition(matrix);
            }
            
            else
                svd  =  new SingularValueDecomposition(myAlg.transpose(matrix));
            
            timer.stop();            
            System.out.println("SVD " + modelName + " Calculation took: " + timer.getTime());

        
            if(FOLDING == false)
            {
	            if(FTFlag==false)
	            {
		           if(sparse ==false)
		           {     	   
			        	 //for new user cold start 
			            if(newUserScenario==true)
			            {
				            if(normalization.equalsIgnoreCase("user"))
				             	destFile = myPath +"/UserNor/"+modelName +"_"+ trainingOrFullSet +"_" + "UserNor"+coldStartUsers+"_"+coldStartThreshold+".dat";
				            else if(normalization.equalsIgnoreCase("mov"))
				            	 destFile = myPath + "/MovNor/"+  modelName +"_"+ trainingOrFullSet +"_" + "MovNor"+coldStartUsers+"_"+coldStartThreshold+".dat";             
				            else if(normalization.equalsIgnoreCase("simple"))
				            	destFile = myPath + "/Simple/"+ modelName +"_"+ trainingOrFullSet +"_" +  "Simple"+coldStartUsers+"_"+coldStartThreshold+".dat";
				            else if(normalization.equalsIgnoreCase("sigma"))
				            	destFile = myPath + "/SigmaNor/"+ modelName +"_"+ trainingOrFullSet +"_" +  "SigmaNor"+coldStartUsers+"_"+coldStartThreshold+".dat";
			            }
			            
			            else
			            {
				            //Write SVD into memory (depend on which normalization method you used)
				            if(normalization.equalsIgnoreCase("user"))
				             	destFile = myPath +"/UserNor/"+modelName +"_"+ trainingOrFullSet +"_" + "UserNor.dat";
				            else if(normalization.equalsIgnoreCase("mov"))
				            	 destFile = myPath + "/MovNor/"+  modelName +"_"+ trainingOrFullSet +"_" + "MovNor.dat";             
				            else if(normalization.equalsIgnoreCase("simple"))
				            	destFile = myPath + "/Simple/"+ modelName +"_"+ trainingOrFullSet +"_" +  "Simple.dat";
				            else if(normalization.equalsIgnoreCase("sigma"))
				            	destFile = myPath + "/SigmaNor/"+ modelName +"_"+ trainingOrFullSet +"_" +  "SigmaNor.dat";
			            }		            
		            
		           }
		           
		           else
		           {		        	   
		        	   if(normalization.equalsIgnoreCase("user"))
			             	destFile = myPath +"/UserNor/Sparsity/"+modelName +"_"+ trainingOrFullSet +"_"+ sparsityLevel +"_" + "UserNor.dat";
			            else if(normalization.equalsIgnoreCase("mov"))
			            	 destFile = myPath + "/MovNor/Sparsity/"+  modelName +"_"+ trainingOrFullSet +"_" + sparsityLevel +"_" + "MovNor.dat";             
			            else if(normalization.equalsIgnoreCase("simple"))
			            	destFile = myPath + "/Simple/Sparsity/"+ modelName +"_"+ trainingOrFullSet +"_" + sparsityLevel +"_" + "Simple.dat";
			            else if(normalization.equalsIgnoreCase("sigma"))
			            	destFile = myPath + "/SigmaNor/Sparsity/"+ modelName +"_"+ trainingOrFullSet +"_" +  sparsityLevel +"_" +"SigmaNor.dat";
		        	   
		           }
	            }
          	
	        	//Ft location is diff for each min user and mov 
	        	else
	        	{
	        	   String info = FTInfo + "Red";
	        	//	 String info = FTInfo;
	        		
	        		if(normalization.equalsIgnoreCase("user"))
	        		destFile = myPath +"UserNor/"+minUserAndMov+ "/"+modelName  +"_"+ trainingOrFullSet +"_" + "UserNor" + info  +(fold) +".dat";
	        	else if(normalization.equalsIgnoreCase("mov"))	
	        		destFile = myPath + "MovNor/"+ minUserAndMov+ "/"+ modelName +"_"+ trainingOrFullSet +"_" + "MovNor" + info +(fold) +".dat";
	        	else if(normalization.equalsIgnoreCase("simple"))
	        		destFile = myPath + "Simple/"+ minUserAndMov+ "/"+modelName +"_"+ trainingOrFullSet +"_" + "Simple" + info +(fold) +".dat";         
	        	else if(normalization.equalsIgnoreCase("sigma"))
	        		destFile = myPath + "SigmaNor/"+ minUserAndMov+ "/"+modelName +"_"+ trainingOrFullSet +"_" + "SigmaNor" + (fold) +".dat";
	        	}
            
            }
            
            else  //For Folding
            {            	
   	  	       //select respective dest (depend on norlaization method used)
            	if(FTFlag==false)
            	{
            		if(normalization.equalsIgnoreCase("user"))
            		destFile = myPath +"UserNor/"+modelName  +"_"+ trainingOrFullSet +"_" + "UserNor" + (fold) +".dat";
            	else if(normalization.equalsIgnoreCase("mov"))	
            		destFile = myPath + "MovNor/"+  modelName +"_"+ trainingOrFullSet +"_" + "MovNor" + (fold) +".dat";
            	else if(normalization.equalsIgnoreCase("simple"))
            		destFile = myPath + "Simple/"+ modelName +"_"+ trainingOrFullSet +"_" + "Simple" + (fold) +".dat";         
            	else if(normalization.equalsIgnoreCase("sigma"))
            		destFile = myPath + "SigmaNor/"+ modelName +"_"+ trainingOrFullSet +"_" + "SigmaNor" + (fold) +".dat";
            	}
      
            	
            	//Ft location is diff for each min user and mov 
            	else
            	{
            		if(normalization.equalsIgnoreCase("user"))
            		destFile = myPath +"UserNor/"+minUserAndMov+ "/"+modelName  +"_"+ trainingOrFullSet +"_" + "UserNorAll" + (fold) +".dat";
            	else if(normalization.equalsIgnoreCase("mov"))	
            		destFile = myPath + "MovNor/"+ minUserAndMov+ "/"+ modelName +"_"+ trainingOrFullSet +"_" + "MovNorAll" + (fold) +".dat";
            	else if(normalization.equalsIgnoreCase("simple"))
            		destFile = myPath + "Simple/"+ minUserAndMov+ "/"+modelName +"_"+ trainingOrFullSet +"_" + "SimpleAll" + (fold) +".dat";         
            	else if(normalization.equalsIgnoreCase("sigma"))
            		destFile = myPath + "SigmaNor/"+ minUserAndMov+ "/"+modelName +"_"+ trainingOrFullSet +"_" + "SigmaNor" + (fold) +".dat";
            	}
            		
            }
   	  	 
            FileOutputStream fos  = new FileOutputStream(destFile);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(svd);
            os.close();
            timer.resetTimer();
            
       } //end of fold for
   } //end of try
   
        catch(Exception e) {
                    e.printStackTrace();
        }

      } //end of outer sparse loop

    }

/**************************************************************************************************/
	
/*  public void checkTheMethodMake()
  {
	  double[][] data  = new double[10][5]; //For SVD m>=n
	  int k =3;
	  int r =0;
	  
	  for(int i=0;i<10;i++)
	  {
		  for (int j=0;j<5;j++) 
		  {	
			  if(j%2 ==0 && i%3 ==0)
				  data [i][j] = 0;
			  else 
				  data [i][j] = r++;
			  if(r==5) r=1;
		  }
	  }
	  
	  DenseDoubleMatrix2D myMatrix = (DenseDoubleMatrix2D) DoubleFactory2D.dense.make(data);	  
	  System.out.println("Matrix =\n" +myMatrix);
	  
	  //--------------------------------------------------------
	     SingularValueDecomposition svd  =  new SingularValueDecomposition(myMatrix);	   
	     Algebra alg = new Algebra();
         DoubleMatrix2D rootSk = svd.getS().viewPart(0, 0, k, k);
         
         System.out.println("S =\n" + rootSk);   
         for(int i = 0; i < k; i++) 
         {
           rootSk.set(i,i,Math.sqrt(rootSk.get(i,i)));  //Sets the matrix cell at coordinate [row,column] to the specified value. 
         }
         
         System.out.println("Sk =\n" + rootSk);
         
	   
	  DoubleMatrix2D U  = svd.getU();	
      System.out.println("U =\n" + U);
      
	  DoubleMatrix2D Uk = U.viewPart(0, 0, U.rows(), k).copy(); // (int row, int column, int height, int width) 
	  System.out.println("Uk =\n" + Uk);
	  
      DoubleMatrix2D VPrime  = alg.transpose(svd.getV());
      System.out.println("V' =\n" + VPrime);
      
      DoubleMatrix2D VPrimek = VPrime.viewPart(0, 0, k, VPrime.columns()).copy();
      System.out.println("V'k =\n" + VPrimek);
      
      DoubleMatrix2D rootSkPrime  = alg.transpose(rootSk);
      System.out.println("S' =\n" + rootSkPrime);
      
      //compute left and right by multiplying US, and SV'           
     // DoubleMatrix2D left = alg.mult(Uk, rootSkPrime);
      DoubleMatrix2D left  = alg.mult(Uk, rootSk);
      DoubleMatrix2D right = alg.mult(rootSk, VPrimek);
      System.out.println("Uk * Sk =\n" + left);
      System.out.println("Sk * V'k =\n" + right);
      
      // Multiply [(US)(SV')]
      DoubleMatrix2D P = alg.mult(left, right);      
      System.out.println("P =\n" + P);
	  
	  }
	*/  
  
	
	
}