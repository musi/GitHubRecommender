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
import netflix.weka.Writers.SVMWriter;
import cern.colt.list.LongArrayList;
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
class FeatureSVD 
{

	BufferedWriter      writeData;
	String    			myPath="";
	boolean 			FOLDING;
	boolean 			FTFlag;
	int					modelType;	     				//1=feature, 2=demo, 3=rat
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
    
    MemHelper 					helper;					//train object
    MemHelper 					mainHelper;				//object (test and train)
    Random              		myRand;
    FilterAndWeight     		myFilter; 				//user-based CF
    ItemItemRecommender			myItemRec;		    	//item-based CF
    TextNBRecWriter 			myNB;					//Naive Bayes Rec
    Algebra						myAlg; 
    
    //For making the dictionay of the features
    HashMap <String,Double> myDictionary;
    HashMap <String,Double> myDictionary_Max;			//Max value of a feature in all movies
    HashMap <String,Double> myDictionary_Min;			//Min value of a feature in all movies
    HashMap <String,Double> myDictionary_maxMinusMin;   //denomenator of normalisation
    
    //--------------------------------------------
    
	public FeatureSVD(int myClasses, boolean folding, boolean FTFlag,  String trainingOrFullSet)
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
  
        //Dictionary
         myDictionary = new HashMap <String, Double>();
         myDictionary_Max = new HashMap <String, Double>();
         myDictionary_Min = new HashMap <String, Double>();
         myDictionary_maxMinusMin = new HashMap <String, Double>();
	}
	
/****************************************************************************************************/
	/**
	 * Main Method
	 */
	
	public static void main(String args[]) 
    {
		 String trainingOrFullSet = "training";
		//String trainingOrFullSet = "full";
		
		FeatureSVD mySVD = new FeatureSVD(10,		 					//Classes 
												  false, 	 			//Folding	
												  false, 	 			//FT
												  trainingOrFullSet); 	//trainingOrFullSet
		mySVD.prepareModelParameters();
    }

/****************************************************************************************************/
	
	/**
	 * Prepare Modeol parameters
	 */
	
	public void prepareModelParameters ()
	{
		
		
		int myEnd =0;
		
		if(FTFlag==false)
			myEnd = 1;
		else
			myEnd = 1;
		
	for(int m = 1 ;m<=myEnd;  m++)
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
		
	
			// XFactor (Training size)
			for(int j=80;j<=80;j+=20)
			{			 
				/*if(j==80)
					j =90;
				*/
				
				xFactor = j;
				modelType = 3;			//1,2,3-->feature,demo,rat
				normalization = "Simple";
				//normalization = "UserNor";
				
				buildModels(2);	
					
			
			} //end of midlle for	
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
        int 	 numFeatures 		= 0;                
        int 	 rows      			= 0;
        int 	 cols 	   			= 0;
        int      epoch     			= 0;
        String   myPath	   			= "";
  
        //determine folding and simple 20-80 settings
        if(FOLDING == false){		 
        	epoch =1;						// No. of times a loop will run
        			
        	if(FTFlag==false)	{			// Path		
        		//myPath = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/" + xFactor + "/";
        	 	  myPath  = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/FeaturesPlay/FeatureModels/";
        	}
        	
        	else
        		myPath = "C:/Users/Musi/workspace/MusiRecommender/DataSets/FT/SVD/"+ xFactor + "/";
        		
        }
        
        else{
        	epoch =5;						// No. of times a loop will run
			
        	if(FTFlag==false){				// Path		
        		//myPath = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/FiveFoldData/"+ xFactor + "/";
        	 	myPath = "C:/Users/Musi/workspace/MusiRecommender/DataSets/SML_ML/SVD/FeaturesPlay/FeatureModels/";
        	}
        	
        	else
        		myPath = "C:/Users/Musi/workspace/MusiRecommender/DataSets/FT/SVD/FiveFoldData/"+ xFactor + "/";
        		
        }
 
  //Build Model     
  try { 	
      
       for (int fold=1;fold <=epoch;fold++)
       {
           if(FOLDING == false) // No folding
           {                   	   
	        	//SML   
	        	if(FTFlag==false)
	        	{	        	   
	        	
	        	myClasses = 5;
	        		
	        	//Usual
	        	/*datFile     = myPath + "sml_clusteringTrainSetStoredTF.dat";
	            testFile    = myPath + "sml_clusteringTestSetStoredTF.dat";*/
	            
	          	 //Feature Play		 	      
	            datFile     = myPath + "sml_clusteringTrainSetStoredTF.dat";
	            testFile    = myPath + "sml_clusteringTestSetStoredTF.dat";
	            mainFile    = myPath + "sml_storedFeaturesRatingsTF.dat";
	            
	            
	            
	            numMovies 	= 1682;
		        numUsers 	= 943;
		        rows        = numMovies;
		        cols        = numUsers;
		        FTFlag		= false;	 
	        	
	        	}
	        	 
	        	 //FT 
	        	 else
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
	 	        	
	             	//make some objects
	 	        	MemHelper mainHelper = new MemHelper(mainFile);		
	 	        	myFilter = new FilterAndWeight(helper,1); 		 //FilterAndWeight, Random object, For CF	                 	   
	 	            myItemRec = new ItemItemRecommender(true, 5);     //Item based CF     
	 	        
	 	             
	 	           numMovies 	= mainHelper.getNumberOfMovies();			
	 	           numUsers 	= mainHelper.getNumberOfUsers(); 	
	 	          
	 	            
	 	           System.out.println("Num Mov="+ numMovies);
		           System.out.println("Num Users="+ numUsers);
		            
		          /* numMovies 	= 1922;			
	 	           numUsers 	= 1214;	 */	           
	 	                   
	                FTFlag		= true;	           	
	    
	           }       	
           } //end if
           
           //--------------------------------------------------------------------------
           // For Folding case.....only dat file and the dest file will change
           //--------------------------------------------------------------------------
           else
        	{
	        	//SML   
		        if(FTFlag==false)
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
	        	else
	        	{	           
	        
	        	myClasses = 10;
	        	  
	        	if(trainingOrFullSet.equalsIgnoreCase("full")){
	        		  	        		  	        	  
		        	datFile     = myPath + "ft_clusteringTrainSetStoredTF"+ minUserAndMov + ".dat";
		            testFile    = myPath + "ft_clusteringTestSetStoredTF"+ minUserAndMov + ".dat";
		            mainFile    = myPath + "ft_modifiedStoredFeaturesRatingsTF"+ minUserAndMov + ".dat";
	        	}
	        	
	        	else{
	        		
	        		datFile     = myPath + "ft_clusteringTrainingTrainSetStoredTF"+ minUserAndMov + ".dat";
		            testFile    = myPath + "ft_clusteringTrainingValSetStoredTF"+ minUserAndMov + ".dat";
		            mainFile    = myPath + "ft_modifiedStoredTrainingFeaturesRatingsTF"+ minUserAndMov + ".dat";
	            
	        	}

	        	//make some objects
	        	MemHelper mainHelper = new MemHelper(mainFile);		
	        	myFilter = new FilterAndWeight(helper,1); 		 //FilterAndWeight, Random object, For CF	                 	   
	             myItemRec = new ItemItemRecommender(true, 5);     //Item based CF     
	        
	             
	            numMovies 	= mainHelper.getNumberOfMovies();			
	            numUsers 	= mainHelper.getNumberOfUsers();

	     /*     numMovies 	= 1922;			
	            numUsers 	= 1214;
            */
	            FTFlag		= true;
	           	
	           }    	
         
        	}//end else
           
                     	      
            //Create memhelper objects
            helper = new MemHelper(datFile);		       
            mainHelper = new MemHelper(mainFile);
         
            //----------------------------------------------------
            // Decide which model to build
            //----------------------------------------------------
            
            double[][] data;
            
            //------------------
    	   	//Build feature svd
    	   	//------------------
            
       if(modelType==1)
       {
    	    modelName = "FeatureModel";
    	    
            //Check the dictionary words
            getAllKeywordAndMakeMatrix();                        
            numFeatures = myDictionary.size();
            
            //Make data Matrix
            data  = new double[numFeatures][numMovies]; //For SVD m>=n       	
            double TF 			= 0;
            double norWeight    = 0;								   //After normalisation, we assign this weight
            int    featureIndex = 0;								   //keep track of which feature
            int    i 			= 0;								   //feature index
            String singleFeature= "";
                     
            
          //start building the dictionary
  		  Set mySet = myDictionary.entrySet();			  // Find set and iterators  
  		  Iterator myIterator = mySet.iterator();
  		  HashMap<String, Double> featuresMovie;
  		  
       	//Here get a feature for a movie, then (1) normalize it (2) if it is not there, put
      	//zero there (3) while normalizing check denomenator shld not be zero

  		  
  		  //Iterate over the words of Test set until one of them finishes
  	     	while(myIterator.hasNext()) 
  	     	 {
  	     		 i = featureIndex++;  	 
  	     		 
  	     		 if(featureIndex>1000 && featureIndex%1000 ==0)
  	     			 System.out.println("Has processed "+ featureIndex+" features");
  	     		 
  	     		 Map.Entry words  = (Map.Entry)myIterator.next();        // Next 		 
  	     		 singleFeature	  = (String)words.getKey();				 // Get word
	            	
  	     		 //Get min and max of this feature
  	     		 double min  = myDictionary_Min.get(singleFeature);
  	     		 double max  = myDictionary_Max.get(singleFeature);
  	     		 double diff = myDictionary_maxMinusMin.get(singleFeature);
  	     	     
	                for(int j = 0; j < numMovies; j++) 
	                {	    
	                	//reset weights
	                	norWeight = 0;
	                	TF 		  = 0;
	                	
	                	featuresMovie = mainHelper.getFeaturesAgainstAMovie(j+1);
	                	
	                	//first check if this mov has some features
	                	if(featuresMovie!=null && featuresMovie.size()>0)
	                	{
	                		//Now check if it has feature
	                		if(featuresMovie.containsKey(singleFeature))
	                		{
	                			//get and normalise the weight
	                			TF = featuresMovie.get(singleFeature);
	                			
	                			//check if denomenator is zero or not
	                			if(diff!=0)
	                				norWeight = (TF - min) / diff;
	                			else
	                				{
	                					if(max!=0)
	                						norWeight = (TF - min) / max;
	                					else
	                						norWeight = (TF - min) / 1;
	                				}           			
	                		}
	                	} //end if
	                	
	                	//if movie has no features, then put zero
	                	else
	                	{
	                		norWeight = 0;
	                	}      	
	                	
	             
	                	data[i][j] = norWeight;
	                	
	               
	                } //end of movie for
	            } //end of feature loop
         }
       
    
	    //----------------
	   	//Build demo svd
	   	//----------------
   	       
       else if(modelType==2)
       {
    	   modelName = "DemoModel";
    	   
    	  //Make data Matrix
          data  = new double[numMovies][19]; //For SVD m>=n       	
           
         for(int j = 0; j < numMovies; j++) 
         {
           
  		 //Get their genres (MemHelper have two methods, one return map and second returns arraylist)
           LongArrayList genreActive = mainHelper.getGenreAgainstAMovie(j+1);                          
           int  sizeActive = genreActive.size();                   
              
              //vector is of size =19
          	  for(int i=0;i<19;i++)
               {
                 	 //active
                 	 if(genreActive.contains(i))			//if it contains the genre, then put it
                 		 	data[j][i] = i;
                 	 else 
                 		 	data[j][i] = 0;               	//else put zero
                }     
            }//end for
         }//end else
       
        //----------------
	   	//Build rat svd
	   	//----------------
       
       else
       {
    	   double rating 	= 0;
    	   double sigma     = 1;
    	   double nor       = 0;  
    	   modelName 		= "RatModel";
    	   
     	  //Make data Matrix
           data  = new double[numMovies][numUsers]; //For SVD m>=n       	
           
           for(int i = 0; i < numMovies; i++) 
           {
               for(int j = 0; j < numUsers; j++) 
               {
                   rating = helper.getRating(j+1, i+1);						//uid, mid

                   if(normalization.equalsIgnoreCase("user"))  		   		// user norm
                	   		nor = helper.getAverageRatingForUser(j+1);         
                   else  if (normalization.equalsIgnoreCase("simple"))  	// no norm     
                	  		nor = 0;     	  
                   
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
                   			temp = myFilter.recommendS(j+1, i+1, 30, 1);
                   		else
                   			temp = myFilter.recommendS(j+1, i+1, 70, 1);
	                    	data[i][j] = temp - nor/sigma;
	                    	modelName  = "SVDUserBasedCF";
                   	}
                   	
                   	// Item-Based CF
                   	else if(whichModel==9) {
                   		double temp =0;
                   		
                   		if(FTFlag)
	                    		temp = myItemRec.recommend(helper, j+1, i+1, 2, 2);
                   		else
                   			temp = myItemRec.recommend(helper, j+1, i+1, 10, 40);
	                    	data[i][j] = temp - nor/sigma;
	                    	modelName  = "SVDItemBasedCF";
                   	}

                   	//(User+Item CF)/2
                   	else if(whichModel==10) {
	                    	double tempU, tempI;
	                    	
	                    	if(FTFlag){
                   		 tempU = myFilter.recommendS(j+1, i+1, 30, 1);
	                    	 tempI = myItemRec.recommend(helper, j+1, i+1, 2, 2); //train obj, uid, mind, neigh, alpha
	                    	}
	                    	
	                    	else{
	                    		tempU = myFilter.recommendS(j+1, i+1, 70, 1);
		                    	 tempI = myItemRec.recommend(helper, j+1, i+1, 5, 40); //train obj, uid, mind, neigh, alpha
	                    	}
	                    	
	                    	data[i][j] = (tempU + tempI)/2 - nor/sigma;
	                    	modelName  = "SVDUserAndItemBasedCF";
                   		}
                   }
                   
                   // We have ratings, insert them into matrix
                   else 
                   {
                        data[i][j] = rating - nor/sigma;  //normalize?
                   	// data[i][j] = rating;
                   }
                   
               	}//end inner for           
          }//end outer for
       
       }//end if else if
       
            //----------------------------------
            // Construct SVD 
       		//----------------------------------
       
            //Constructs a matrix with the given cell values
            //Use idioms like DoubleFactory2D.dense.make(4,4) to construct dense matrices
            DenseDoubleMatrix2D matrix = (DenseDoubleMatrix2D) DoubleFactory2D.dense.make(data);
          
           //checkTheMethodMake();
            
            //SVD
            timer.start();
            SingularValueDecomposition svd;            
            if(!FTFlag)	 
            	svd  =  new SingularValueDecomposition(matrix);
            
            //control, the row x cols of svd according to the dimension of matrix
            //condition row > cols in svd
            //so reverse it to user = row, or mov = row accordingly
            else {              	
            	if(	minUserAndMov <=1 )        		
            		svd  =  new SingularValueDecomposition(matrix);
            	else
            	      svd  =  new SingularValueDecomposition(myAlg.transpose(matrix));
            	  //  svd  =  new SingularValueDecomposition(matrix);
            }
            timer.stop();            
            System.out.println("SVD " + modelName + " Calculation took: " + timer.getTime());

            //----------------------------------
            // WRITE SVD 
       		//----------------------------------
        
            if(FOLDING == false)
            {
	            if(FTFlag==false)
	            {	           
	            	destFile = myPath + modelName +"_"+ trainingOrFullSet + ".dat";
	            }
            
          	
	        	//Ft location is diff for each min user and mov 
	        	else
	        	{
	        		   String info = FTInfo + "Red";
	        		// String info = FTInfo;	        		
	        	
	        		destFile = myPath +minUserAndMov+ "/"+modelName  +"_"+ trainingOrFullSet + info  +".dat";
	        	
	        	}
            
            }
            
            else  //For Folding
            {            	
   	  	       //select respective dest (depend on norlaization method used)
            	if(FTFlag==false)
            	{            	
            		destFile = myPath +modelName  +"_"+ trainingOrFullSet + (fold) +".dat";
            	}
            	
            	//Ft location is diff for each min user and mov 
            	else
            	{            	
            		destFile = myPath +minUserAndMov+ "/"+modelName  +"_"+ trainingOrFullSet +".dat";
            	           	
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



    }

/**************************************************************************************************/
	
	/**
	 * We will get features from all the movies, and build a dictionary, then we will
	 * use this this as a check-mark, if a movie do not has a keyword (term), put zero in that
	 * term....similarly build this model and generate model.
	 */
	
  public void getAllKeywordAndMakeMatrix()
  {
	  System.out.println("came for building dictionary");
	  
	  int numMovies = mainHelper.getNumberOfMovies();
	  HashMap<String, Double> FeaturesMovie;
	  
	  for(int i=1;i<=numMovies;i++)			//for all movies
	  {
		  FeaturesMovie = mainHelper.getFeaturesAgainstAMovie(i);
		  
		  //start building the dictionary
		  Set mySet = FeaturesMovie.entrySet();			  // Find set and iterators  
		  Iterator myIterator = mySet.iterator();    	   
		 
		//  myFeatures +="\"";											  // opening string "
		  int total =0;
		  
		  //Iterate over the words of Test set until one of them finishes
	     	while(myIterator.hasNext())// && total <10) 
	     	 {
	     	     total++; 
	     		 Map.Entry words  = (Map.Entry)myIterator.next();        // Next 		 
	     	     String word 	 = (String)words.getKey();				 // Get word	    
	     	     double TF 		 = FeaturesMovie.get(word);          	 // Get TF   	     	
	    
	     	     if(myDictionary.containsKey(word)==false && TF>2)	
	     	    	myDictionary.put(word, 1.0);			     		//We do not care abt the tf 		
	     	     
	     	 }//end of while
	     	
	    } //end for
	 
	  //--------------------------------------------------
	  int featureSize = myDictionary.size();
	  double max = 0;
	  double min = 0;
	  double TF  = 0;	
	  String singleFeature ="";
	  
	  System.out.println("Num distinct words found ="+ featureSize); 

	  //Iterate over the words of Test set until one of them finishes
	  Set mySet = myDictionary.entrySet();			  					// Find set and iterators  
	  Iterator myIterator = mySet.iterator();    	   

	    while(myIterator.hasNext())// && total <10) 
     	 {     	
     		 Map.Entry words = (Map.Entry)myIterator.next(); 	      // Next 		 
     		 singleFeature 	 = (String)words.getKey();				  // Get word   
     	     
     	     
     	     //reset min and max for each feature	  
			  max = 0;
			  min = 0;			 
			  
			//Find Max and Min for each feature over all the movies
			  for(int i=1;i<=numMovies;i++)		
			  {
				  FeaturesMovie = mainHelper.getFeaturesAgainstAMovie(i);
				  
				  if(FeaturesMovie.containsKey(singleFeature))
				  {
					  TF = FeaturesMovie.get(singleFeature);					  // Get TF of the word
					  
					  //update min and max
					  if(min > TF)
						  min = TF;					  
					  if(max<TF)
						  max = TF;
					  
				  }				  		  	  
			  }//end inner for of movies
			  
			  //store max and min of each feature into a map
			  myDictionary_Max.put(singleFeature, max);
			  myDictionary_Min.put(singleFeature, min);
			  myDictionary_maxMinusMin.put(singleFeature,(max-min));
			  
	    } //end while
	  
	  
  }
 
  
	
	
}