package netflix.algorithms.modelbased.svd;

import java.util.*;
import java.io.*;
import netflix.algorithms.memorybased.memreader.FilterAndWeight;
import netflix.memreader.MemHelper;
import netflix.memreader.*;
import netflix.recommender.ItemItemRecommender;
import netflix.recommender.ItemItemRecommenderWithK;
import netflix.recommender.NaiveBayesRec;
import netflix.rmse.RMSECalculator;
import netflix.utilities.*;
import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import cern.colt.list.LongArrayList;
import cern.colt.map.OpenIntDoubleHashMap;
import cern.colt.matrix.*;
import cern.colt.matrix.linalg.*;
import cern.colt.matrix.impl.*;

/**
 * @author Musi
 */

public class RecursiveSVD1 
{

		//for memHelper and test train objs
		private     MemHelper 						myTestMh;
		private     MemHelper 						myTrainingMMh;
		private 	MemHelper						mainMMh;    //Full MemHelper without any test train (will be used for demo)
		private     String 							testSet;
		private     String 							trainSet;
		private     String 							mainSet;		
		private 	String     						myPath;							
		
		//for svd
	 	private 	SingularValueDecomposition 		svd;
	    private 	DoubleMatrix2D 					P;
	    private 	int 							k;
	    private     int								loopOfSvdWriting;
	    private     String							modelNormalization;
	    private     String							modelName;
	    private     int 							optK[];
	    private	    int								myClasses;
	    
	    
	    private DoubleMatrix2D 				Prediction_Matrix;	// p =left * right	    
	    DoubleMatrix2D						left;				// left = US
	    DoubleMatrix2D						right;				// right = SV
	    Algebra 							alg;
	    
	    //error
	    double								MAE;
	    double								MAEPerUser;	    
	    double								RMSE;
	    double								ROC;	    
	    double								SDMAE;		
	    double								SDROC;				//To calculate the devaition
	    double								SDPrecision;
	    double								SDRecall;
	    double								SDF1;
	    double								pairTValue;	    
	    double            					precision[];		//evaluations   
	    double              				recall[];   
	    double              				F1[];    
	    OpenIntDoubleHashMap 				midToPredictions;	//will be used for top_n metrics (pred*100, actual)
	    
	    
	    boolean 			FOLDING;
	    boolean				FTFlag;					//sml=true, ft=false
	    int 				dataset;				//Sml=0, ft =1
	    String 				CFORSimpleSVD;			// "SimpleSVD", "UBCF", "IBCF" 
	    int 				nor;					// 0-Simple, 1-UserNor 
	    int					FTDataWithMinRat;
	    int					loopNumber;				//at which loop we are
	    
	    Random              myRand;
	    FilterAndWeight     myFilter; 				//user-based CF
	    ItemItemRecommender myItemRec;		    	//item-based CF
	    NaiveBayesRec 		myNB;					//Naive Bayes Rec	  
	    FindAndReturnOptimalDim 	mySVDForFindingOptK;
	    boolean					 	optDimHasBeenFound;
	    
	  //File Writers
	    FileWriter 					myWriter[];
	    
	    
  //-----------------------------------------
	    
	public RecursiveSVD1(int dum_dataset, String dum_CFORSimpleSVD, int dum_nor)
	{	
		
		loopOfSvdWriting 	= 1;
		dataset	 			= dum_dataset;
		CFORSimpleSVD		= dum_CFORSimpleSVD;
		nor					= dum_nor;
		alg   				= new Algebra();
		
		if(dataset==0)		//SML 
		{
		 myPath     	= "/home/mag5v07/workspace/netflix/DataSets/SML_ML/SVD/Iterative/80/";
         trainSet		= myPath + "sml_clusteringTrainSetStoredTF.dat";
         testSet		= myPath + "sml_clusteringTestSetStoredTF.dat";         
         mainSet		= myPath + "sml_clusteringTestSetStoredTF.dat";
         FTFlag			= false;
         myClasses		= 5;
		}
		
		else if(dataset==1)	//FT 
		{
		  String info = "Both";			//Manullay change these parameters
		  FTDataWithMinRat	= 5;
		  
		  myPath     	= "/home/mag5v07/workspace/netflix/DataSets/FT/SVD/Iterative/80/";
	      trainSet		= myPath + "ft_clusteringTrainSetStored"+ info +"TF"+FTDataWithMinRat+".dat";
	      testSet		= myPath + "ft_clusteringTestSetStored"+ info +"TF"+FTDataWithMinRat+".dat";         
	      mainSet		= myPath + "ft_storedFeaturesRatings"+ info +"TF"+FTDataWithMinRat+".dat";
	      FTFlag		= true;
	      myClasses     = 10;
		}
		
		else
		{
			myPath     	= "/home/mag5v07/workspace/netflix/DataSets/ML_ML/SVD/Iterative/80/";
	        trainSet	= myPath + "ml_clusteringTrainSetStoredTF.dat";
	        testSet		= myPath + "ml_clusteringTestSetStoredTF.dat";         
	        mainSet		= myPath + "ml_clusteringTestSetStoredTF.dat";
	        FTFlag		= false;
	        myClasses	= 5;	
		}
		
         myTrainingMMh	= new MemHelper (trainSet);		
		 myTestMh		= new MemHelper (testSet);	//MemHelper objects	 
		 mainMMh		= new MemHelper (mainSet);
		
		 //mySVDForFindingOptK = new FindAndReturnOptimalDim(dataset, CFORSimpleSVD, nor, 80, FTDataWithMinRat);  //0 = sml, 1= ft
		 mySVDForFindingOptK = new FindAndReturnOptimalDim(dataset, "SimpleSVD", nor, 80, FTDataWithMinRat);  //0 = sml, 1= ft

		 //Files
	     myWriter = new FileWriter[22];				//see file open section
	        
	     //Errors
	     MAE 				= 0;	    
	     MAEPerUser			= 0;
	     ROC 				= 0;     
	     SDMAE				= 0;
	     SDROC				= 0;
	     SDPrecision		= 0;
	     SDRecall			= 0;
	     SDF1				= 0;	 
	     pairTValue			= 0;
	     loopNumber			= 0;
	     
	     precision    		= new double[6];		//topN; for six values of N (top5, 10, 15...30)
	     recall  			= new double[6];		// Most probably we wil use top10, or top20
	     F1					= new double[6];
	     midToPredictions   = new OpenIntDoubleHashMap();
	     optK      			= new int[6];
	     optDimHasBeenFound = false;	//give optimal parameters learnt from the other program
	     
	}
	
	
/****************************************************************************************************/
	/**
	 * main
	 */
	
	public static void main(String args[]) 
    {
		System.out.println(" Welcome..");
		while(true)
		{
			if(2>3)
				break;
		}
		
		String predictionMethod = "";	  
	    predictionMethod = "SimpleSVD";
	  //predictionMethod = "IBCF";
	 // predictionMethod = "UBCF";
	  
	  
	  //Normalization
	  int dum_nor =0;
	  
	  
	  //SVD object 
	  RecursiveSVD1 mySVD = new RecursiveSVD1(2, predictionMethod, dum_nor);  //0 = sml, 1= ft, 3=ml
		
	  mySVD.FTDataWithMinRat =5;
	  
		//-------------------------
		// Build Models		
	    //------------------------
	  
	  int end =0;
	 
	  for(int n=1;n<2;n++)
	  {
		  //Change nor
		  mySVD.nor = n;		//Simple, and UserNor
		  int start;
		  		  
		  if(n==1)
			  start =1;
		  else
			  start =1;		  
		  		  
		   for(int t=start;t<=5;t++)
		   {	
			 //modelName and nor 
			   mySVD.modelNameAndNor(t);
			   
			   if(t<=2)
				   end =15;
			   else 
				   end =15;
			   
			   //for each scheme we want to find optimal K
			   mySVD.optDimHasBeenFound = false;
			   
				//build models
				for(int i=1; i<=end;i++)
				{
					mySVD.svdBuildAndSaveIntoAFile(t, i);	
				}			
		   }
		}//end outer for
	  
	  
	  	//-------------------------
	    // Generate predictions		
	    //------------------------	
	  
	  
	  for(int n=1;n<2;n++)
	  {
		  //Change nor
		  mySVD.nor = n;		//Simple, and UserNor
		  
		  for(int m=0;m<=0;m++)
		  {
			  if(m==0)			
				  mySVD.CFORSimpleSVD = "SimpleSVD";
			  
			  else if (m==1)
				  mySVD.CFORSimpleSVD = "UBCF";
			  
			  else if (m==2)			  
				  mySVD.CFORSimpleSVD = "IBCF";
			  
			  mySVD.checkErrorForEachIetration();	   
		  }
	  }//end of nor for
	  
	  
	  
    }
	
/****************************************************************************************************/
	/**
	 * Gives model name and nor
	 */
	public void modelNameAndNor(int modelLoop)
	{
		  //Model name
		if(modelLoop==1)
			   modelName = "svdMovAvg";
		else if(modelLoop==2)
			   modelName = "svdUserAvg";
		else if (modelLoop==3)
			   modelName = "svdUserBasedCF";
		else if (modelLoop==4)
			   modelName = "svdItemBasedCF";
		else if (modelLoop==5)
			   modelName = "svdUserAndItemBasedCF";
	   
		 //Model Normalisation		 
		 if(nor == 0)
			 modelNormalization = "Simple";
		 else if (nor==1)
     		 modelNormalization = "UserNor";				//normalize by user Avg
		 else
		     modelNormalization = "MovNor";
	            
	}

	
/****************************************************************************************************/
	
	/**
	 * Build SVD model and save into a file
	 */
	
	public void svdBuildAndSaveIntoAFile(int modelLoop, int myLoop)
	{
			loopOfSvdWriting = myLoop;
		
			Timer227 timer = new Timer227();
		
			//Filter and weight
			myFilter = new FilterAndWeight(myTrainingMMh,1); 			 // FilterAndWeight, Random object, For CF
	           
            //Item based CF 	   
			 myItemRec = new ItemItemRecommender(true, 5);   //Item based CF
			          	
		    
		    
        try {
            
        	  int 		numMovies = 0;
              int 		numUsers  = 0;            
              String    destfile  = "";        
              
            //File To store the model  
            //destfile    = myPath + modelNormalization+ "/"+ modelName + modelNormalization + loopOfSvdWriting + ".dat";
            //destfile    = myPath + modelNormalization+ "/" + "Dim=15/" + modelName + modelNormalization + loopOfSvdWriting + ".dat";
            destfile    = myPath + modelNormalization+ "/" + "Dim=k/" +  modelName + modelNormalization + loopOfSvdWriting + ".dat";
             
            if(dataset==0) {			//SML
	            numMovies 	= 1682;
	            numUsers 	= 943; 
            }
            
            //Note: If I am reversing the role of users and movs here, as in SVD the condition is: rows > cols
            //So that means, while generating prediction, I have to check the Order of it, and same is true while 
            //Building model (uk, vk and sl);
            // Before adding any other thing, check the statistic of the FT dataset.
            
            
            else {						//FT, and ML (simple is to get mov and users from the mainMh)
            	
            	int numberOfMovs = mainMMh.getNumberOfMovies();
            	int numberOfUsers = mainMMh.getNumberOfUsers();
            
            	numMovies 	= numberOfMovs;
	            numUsers 	= numberOfUsers;
	            
	            System.out.println("mov="+ numMovies +", users="+numUsers);
            }
            
            //matrix initialisation
            double[][] data  = new double[numMovies][numUsers]; //For SVD m>=n
            double 	rating	 = 0;
            double  flag     = 0;
            
            
            for(int i = 0; i < numMovies; i++) 
            {
                for(int j = 0; j < numUsers; j++) 
                {
                	 double nor =0;                    
                     if(modelNormalization.equalsIgnoreCase("UserNor"))
                     		nor = myTrainingMMh.getAverageRatingForUser(j+1);      		// user norm                    
                     else if(modelNormalization.equalsIgnoreCase("MovNor"))
                     		nor = myTrainingMMh.getAverageRatingForMovie(i+1);   		// mov norm                    
                     else    
                     		nor = 0; 										            // no norm
                     
                                        
                    rating = myTrainingMMh.getRating(j+1, i+1);							//uid, mid

                    //If the user did not rate the movie  (Here use content based or some other heuristic)
                    if(rating == -99) 
                    {
                    	flag++;
                    	
                    	//First time model to make
                    	if(loopOfSvdWriting ==1)         
                    	{
                    		if(modelLoop==1)                    			
                    			data[i][j] = myTrainingMMh.getAverageRatingForMovie(i+1) - nor;
                		
                    		else if(modelLoop==2)                    			
                    			data[i][j] = myTrainingMMh.getAverageRatingForUser(j+1) - nor;                    	
                    		
                    		else if(modelLoop==3) {                			
                    			double tempRat;                    			
                    			if(FTFlag==true)
                    				 tempRat = myFilter.recommendS(j+1, i+1, 30, 2);
                    			else
                    				 tempRat = myFilter.recommendS(j+1, i+1, 70, 2);
                    		
                    			//improtant, as mostly it may return 0, if no neighbour is found 
                    			if(tempRat ==0)
                    				tempRat = myTrainingMMh.getAverageRatingForUser(j+1);
                    			
                    			data[i][j] = tempRat - nor;
                    		}
                    		
                    		else if(modelLoop==4) {              			
                    			double tempRat;                    			
                    			if(FTFlag==true) 
                    				tempRat = myItemRec.recommend(myTrainingMMh, j+1, i+1, 5, 10);
                    			else
                    				tempRat = myItemRec.recommend(myTrainingMMh, j+1, i+1, 10, 40);

                    			if(tempRat ==0)
                    				tempRat = myTrainingMMh.getAverageRatingForUser(j+1);
                    			
                    			
                    			data[i][j] = tempRat - nor;
                    		}
                    		
                    		else if(modelLoop==5)   {  
                    			
                    			double tempRatU;
                				double tempRatI;
                				double tempRat;
                				
                    			if(FTFlag==true) {                   				                    				
	                    			tempRatU = myFilter.recommendS(j+1, i+1, 40, 2);
	                    			tempRatI = myItemRec.recommend(myTrainingMMh, j+1, i+1, 5, 10);
                    			}
                    			
                    			else {                    				
                    				tempRatU = myFilter.recommendS(j+1, i+1, 70, 2);
	                    			tempRatI = myItemRec.recommend(myTrainingMMh, j+1, i+1, 10, 40);
                    			}                    			
                    		
                    			//check for zero
                    			tempRat = (tempRatI + tempRatU)/2.0;
                    			
                    			if(tempRat==0)
                    				tempRat = myTrainingMMh.getAverageRatingForUser(j+1);                   			
       				
                    				
                    			data[i][j] = (tempRatI + tempRatU)/2.0 - nor;
                    		}           	
                    	}
                    	
                    	//Get prediction from the previously build svd model
                    	else {
                    			//get previous svd model saved
                    		    //String previousSVDModel =  myPath + modelNormalization+ "/" +  modelName + modelNormalization + (loopOfSvdWriting-1) + ".dat";
                    			//String previousSVDModel =  myPath + modelNormalization+ "/" + "Dim=15/" + modelName + modelNormalization + (loopOfSvdWriting-1) + ".dat";
                    			  String previousSVDModel =  myPath + modelNormalization+ "/" + "Dim=k/"+  modelName + modelNormalization + (loopOfSvdWriting-1) + ".dat";
                    	                    			
                    			//get prediction
                    			if(flag==1) {
                    				
                    				if(optDimHasBeenFound == false) {
                    					optK[modelLoop]=mySVDForFindingOptK.FindOptimalK(modelLoop, 
                    																	dataset,	
                    																	this.nor,
                    																	CFORSimpleSVD
                    																	);
                    					optDimHasBeenFound = true;
                    				}
                    				
                    				prepareSVDModel (previousSVDModel, optK[modelLoop]);
                    				//prepareSVDModel (previousSVDModel, 10);
                    				System.out.println("Model builded @ flag="+ flag + "optK="+optK[modelLoop] );
                    			}
                    			
                    			//build new model, add prediction
	                    		double newPrediction = recommend(j+1, i+1, 10);
	                    		data[i][j] =  newPrediction - nor;	                    		
                    	    }                	
                    }
                    
                    else 
                    {
                        data[i][j] = rating - nor;
                    }

                } //end of inner for
            } //end of outer for

            //Constructs a matrix with the given cell values    
            DenseDoubleMatrix2D matrix = (DenseDoubleMatrix2D) DoubleFactory2D.dense.make(data);
                               
            //SVD
            SingularValueDecomposition mysvd; 
            timer.start();
            
            if(dataset==0)	 
            	mysvd  =  new SingularValueDecomposition(matrix);
            
            //control, the row x cols of svd according to the dimension of matrix
            //condition row > cols in svd
            //so reverse it to user = row, or mov = row accordingly
            else if(dataset==1){              	
            	if(	FTDataWithMinRat <=1 )        		
            		mysvd =  new SingularValueDecomposition(matrix);
            	else
            		mysvd  =  new SingularValueDecomposition(alg.transpose(matrix));
            	  //  svd  =  new SingularValueDecomposition(matrix);
            }
            
            else
        		mysvd  =  new SingularValueDecomposition(alg.transpose(matrix));
            
            timer.stop();            
            System.out.println("SVD " + modelName + " Calculation took: " + timer.getTime());

            

            //Write SVD into memory
            FileOutputStream fos  = new FileOutputStream(destfile);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(mysvd);
            os.close();
            
     
   } //end of try
   
        catch(Exception e) {
            System.out.println("usage: java RecursiveSVD numMovies numUsers dataFile destFile");
            e.printStackTrace();
        }


    }

/**************************************************************************************************/
  
	/**
	 * Make recommendations
	 */
	
 public void prepareSVDModel(String svdFile, int dim)
  {
		
	 //Read SVD
	 try {
		     FileInputStream fis  = new FileInputStream(svdFile);
		     ObjectInputStream in = new ObjectInputStream(fis);
		     svd 				  =	(SingularValueDecomposition) in.readObject();
		     
		     //call build model method
		     buildModel(dim);
	 }
	 
	 catch(Exception e){
		 	e.printStackTrace();
	 }
  }
 
 /****************************************************************************************************/
 
 /**
  * Computes the recommendation matrix from the 
  * SVD. See the paper "Application of Dimensionality
  * Reduction in Recommender Systems - A Case Study"
  * for more information. 
  */

 private void buildModel(int dim) 
 {
	    left  = null; 
		right = null;
		Prediction_Matrix   = null;
		
		
	 	k = dim;        

	 	DoubleMatrix2D rootSk = svd.getS().viewPart(0, 0, k, k);
        
        //compute singular value
        for(int i = 0; i < k; i++) 
        {
          rootSk.set(i,i,Math.sqrt(rootSk.get(i,i)));  //Sets the matrix cell at coordinate [row,column] to the specified value. 
        }
        
       // Compute U and V'
       DoubleMatrix2D U  = svd.getU();	
       DoubleMatrix2D Uk = U.viewPart(0, 0, U.rows(), k).copy(); // (int row, int column, int height, int width) 

       DoubleMatrix2D VPrime = alg.transpose(svd.getV());
       DoubleMatrix2D VPrimek = VPrime.viewPart(0, 0, k, VPrime.columns()).copy();
       DoubleMatrix2D rootSkPrime = alg.transpose(rootSk);
       
      // compute left and right by multiplying US, and SV'           
      // DoubleMatrix2D left  = alg.mult(Uk, rootSkPrime);
       DoubleMatrix2D tempLeft = alg.mult(Uk, rootSk);
       DoubleMatrix2D tempRight = alg.mult(rootSk, VPrimek);
       DoubleMatrix2D tempP = alg.mult(tempLeft, tempRight);
       
                   
       left  = alg.mult(Uk, rootSk);
       right = alg.mult(rootSk, VPrimek);

       // Multiply [(U * sqrt(S)) * (sqrt(S) * V')] = USV
       Prediction_Matrix = alg.mult(left, right);  
         
 }
 
 
 /****************************************************************************************************/
 
 public void checkErrorForEachIetration()
 {
	 if(nor == 0)
		 modelNormalization = "Simple";
	 else if (nor==1)
 		 modelNormalization = "UserNor";				//normalize by user Avg
	 else
		 modelNormalization = "MovNor";
        
	    //oprn file
	    openFiles();
	    int end =0;
   	
    	  for(int loop=1;loop<=5;loop++)
    	  {		
    		  loopNumber = loop;					//It will be used for pair-t
    		  
    		  if (loop<=2)
    			  end =15;
    		  else 
    			  end =15;
    		  
    	     for(int t=1;t<=end;t++)  //no. of iterations	 
 	     	{    	        		 
 	       	    
    	    	 for(int dim = 1;dim<40;dim++)	 
    	    	 {
    	    		  //Model name
    	    			if(loop==1)
    	    				   modelName = "svdMovAvg";
    	    			else if(loop==2)
    	    				   modelName = "svdUserAvg";
    	    			else if (loop==3)
    	    				   modelName = "svdUserBasedCF";
    	    			else if (loop==4)
    	    				   modelName = "svdItemBasedCF";
    	    			else if (loop==5)
    	    				   modelName = "svdUserAndItemBasedCF"; 	 	    	
    	    		
    	    			 
    	   	     //  String svdFile =  myPath + modelNormalization+ "/" + modelName + modelNormalization + t + ".dat";
    	    	   //String svdFile =  myPath + modelNormalization+ "/" + "Dim=15/" + modelName + modelNormalization + t + ".dat";
    	    		 String svdFile =  myPath + modelNormalization+ "/" + "Dim=k/" +  modelName + modelNormalization + t + ".dat";
    	   	       
				 prepareSVDModel (svdFile, dim);
				 testWithMemHelper(myTestMh, dim);				 
				 //System.out.println("loop=" + t+ ", MAE: " + testWithMemHelper(myTestMh, dim));
				 System.out.println("iteration=" + t+ ", MAE: " + MAE + ", MAEPerUser="+ MAEPerUser);
				 System.out.println("iteration" + t+ ", ROC: " + ROC);
					 
				 //-------------------------
				 //Write results in a file
				 //-------------------------
				 
				 try {
					 	myWriter[0].append(""+ MAE);
					 	myWriter[0].append(",");
					 	myWriter[1].append(""+ MAEPerUser);
					 	myWriter[1].append(",");					
					 	myWriter[2].append(""+ ROC);
					 	myWriter[2].append(",");
					 	
					 	myWriter[3].append(","+ precision[0]);		//top5
					 	myWriter[4].append(","+ recall[0]);
					 	myWriter[5].append(","+ F1[0]);
					 	
					 	myWriter[6].append(","+ precision[1]);		//top10
					 	myWriter[7].append(","+ recall[1]);
					 	myWriter[8].append(","+ F1[1]);
					 	
					 	myWriter[9].append(","+ precision[2]);		//top15
					 	myWriter[10].append(","+ recall[2]);
					 	myWriter[11].append(","+ F1[2]);
					 	
					 	myWriter[12].append(","+ precision[3]);		//top20
					 	myWriter[13].append(","+ recall[3]);
					 	myWriter[14].append(","+ F1[3]);
					 	
					 	myWriter[15].append(","+ SDMAE);
					 	myWriter[16].append(","+ SDROC);
					 	myWriter[17].append(","+ SDPrecision);
					 	myWriter[18].append(","+ SDRecall);
					 	myWriter[19].append(","+ SDF1);
					 	myWriter[20].append(","+ pairTValue);
					 	
					 	
				  } catch(Exception E) {
						   E.printStackTrace();
					 	}					 				 
					 				 
    	      }//end inner for
    	    	 
    	    	 try {		
    	    		   for(int temp=0;temp<=20;temp++)				//write a new line
    	    			   myWriter[temp].append("\n");
										
					
    	    	 	} catch(Exception E) {
						 E.printStackTrace();
					 }
				 
    	   }//end iteration for
    	    
    	     try {		
    	  	   	for(int temp=0;temp<=20;temp++)				//write new new lines for new scheme
				 	myWriter[temp].append("\n\n\n");
									
				
	    	 	} catch(Exception E) {
					 E.printStackTrace();
				 }
		 }//end outer for
    	    
    	    //close files
    	    closeFiles();
    	  
		 

	 }
 
 
 /****************************************************************************************************/
 /**
  * Predicts the rating that activeUser will give targetMovie.
  *
  * @param  activeUser   The user.
  * @param  targetMovie  The movie.
  * @param  neighbour	 The neighbours
  * @return The rating we predict activeUser will give to targetMovie. 
  */
 
 public double recommend(int activeUser, int targetMovie,int neighbours) 
 {
 	
 	double prediction =0;
 	
    
     {
	     	// Entry is retrieved in the correct way, i.e. rows x cols = movs x users
	     	   
    	 if(CFORSimpleSVD.equalsIgnoreCase("SimpleSVD"))
    		 prediction = recommendSVDBased(activeUser,targetMovie);
    	 
    	 else if(CFORSimpleSVD.equalsIgnoreCase("UBCF"))
    		 prediction = recommendUserBased (activeUser, targetMovie, 50);
    	 
    	 else if(CFORSimpleSVD.equalsIgnoreCase("IBCF"))
    		 prediction = recommendItemBased (activeUser, targetMovie, 10);  
     }	
     
     return prediction;
 }

 /****************************************************************************************************/
 // SVD based rec
 /****************************************************************************************************/
     
     /**
      * Predicts the rating that activeUser will give targetMovie.
      *
      * @param  activeUser  The user.
      * @param  targetMovie  The movie. 
      * @return The rating we predict activeUser will give to targetMovie. 
      */
     
   public double recommendSVDBased(int activeUser, int targetMovie) 
    {    	
    			double entry = 0;    		
    			double prediction = 0;        	
         
    			 // Entry is retrieved in the correct way, i.e. rows x cols = movs x users
     		   	if(dataset==0){
             		entry = Prediction_Matrix.get(targetMovie-1, activeUser-1);	   		 // SML 
             	}
             	
             	else if(dataset==1){
             		if(FTDataWithMinRat <=1)
             			entry = Prediction_Matrix.get(targetMovie-1, activeUser-1);	   		 // ft
             		else
             			entry = Prediction_Matrix.get(activeUser-1,targetMovie-1);	    	 // ft
             	}
     		   	
             	else {
             		entry = Prediction_Matrix.get(activeUser-1, targetMovie-1);	   		 // ML	
             	}
            	    
            	    if(modelNormalization.equalsIgnoreCase("MovNor"))
               		prediction = entry + myTrainingMMh.getAverageRatingForMovie(targetMovie);
            	    
               	else if(modelNormalization.equalsIgnoreCase("UserNor"))              		
             	    prediction = entry + myTrainingMMh.getAverageRatingForUser(activeUser);
            	    
               	else
               		prediction = entry;              	
                       
	           // System.out.println(prediction);
	            return prediction;
	    	 
    }
     
 /****************************************************************************************************/
 // Item-Based CF
 /****************************************************************************************************/
     
     /**
      * Predicts the rating that activeUser will give targetMovie.
      *
      * @param  activeUser  The user.
      * @param  targetMovie  The movie.
      * @param  neigh the no. of neighbour to use 
      * @return The rating we predict activeUser will give to targetMovie. 
      */
     
   public double recommendItemBased(int activeUser, int targetMovie, int totalNeighbours) 
   {
   	double entry = 0;
   	double prediction = 0;
       int mid = 0;
       int totalMoviesSeenByActiveUser = 0;
       double activeUserRatingOnSimItem = 0.0;
       double weightSum = 0.0;
       double voteSum   = 0.0;
       double activeUserAverage = 0.0;
       double answer = 0.0;
               
       LongArrayList movies = myTrainingMMh.getMoviesSeenByUser(activeUser);   //get movies seen by this user
   	totalMoviesSeenByActiveUser = movies.size();   
   	activeUserAverage = myTrainingMMh.getAverageRatingForUser(activeUser);  //active user average
   	
   	OpenIntDoubleHashMap itemIdToWeight = new OpenIntDoubleHashMap();
	    IntArrayList myItems      			= new IntArrayList();
	    DoubleArrayList myWeights 			= new DoubleArrayList();
	    double currentWeight;
	    
	    //GO through all the movies seen by active user and find similar items
   	for (int m =0; m<totalMoviesSeenByActiveUser; m++)
   	{    		
   		 mid = MemHelper.parseUserOrMovie(movies.getQuick(m));  
   	     currentWeight  = findItemSimilarity_SVD(targetMovie, mid, activeUser); //active item, item we want to find sim with
   	   	 itemIdToWeight.put(mid, currentWeight);
   	 }
   	
   	//sort the weights
   	 myItems    = itemIdToWeight.keys();
        myWeights 	= itemIdToWeight.values();
        itemIdToWeight.pairsSortedByValue(myItems, myWeights);
   	
        // take weighted sum
        for (int i = totalMoviesSeenByActiveUser-1, myTotal=0; i >=0; i--)       
        {    	   
        		if(myTotal == totalNeighbours) break;              	
        		mid = myItems.get(i);       	
        		
        		//check index
        		{   
        			//positive         			
        			currentWeight = myWeights.get(i);	             	
   /*     			if(currentWeight>0){
        				weightSum += Math.abs(currentWeight);				// see no Math.abs(), for adjusted cosine I think there is no need (weight >0 always)
		             	activeUserRatingOnSimItem = Prediction_Matrix.get(mid-1,activeUser-1);           	
   	             	if((modelNormalization.equalsIgnoreCase("UserNor")))
		             		voteSum += (currentWeight * (activeUserRatingOnSimItem));
   	             	else if((modelNormalization.equalsIgnoreCase("MovNor")))
		             		voteSum += (currentWeight * (myTrainingMMh.getAverageRatingForMovie(mid)));
   	             	if((modelNormalization.equalsIgnoreCase("Simple")))
		             		voteSum += (currentWeight * (activeUserRatingOnSimItem));
   	             	if((modelNormalization.equalsIgnoreCase("SigmaNor")))
		             		voteSum += (currentWeight * (activeUserRatingOnSimItem * myTrainingMMh.getStandardDeviationForUser(activeUser)
		             																+ myTrainingMMh.getAverageRatingForUser(activeUser)));
		             		
		             	myTotal++;
        		 }
        			*/
	             	
        			//all
      				weightSum += Math.abs(currentWeight);					// see no Math.abs(), for adjusted cosine I think there is no need (weight >0 always)
      				
      				//SML
      				if(FTFlag==false)
      					activeUserRatingOnSimItem = Prediction_Matrix.get(mid-1,activeUser-1);
      				
      				//FT
      				else{
      					if(FTDataWithMinRat <=1)
      						activeUserRatingOnSimItem = Prediction_Matrix.get(mid,activeUser-1);
      					else
      						activeUserRatingOnSimItem = Prediction_Matrix.get(activeUser-1, mid);
      					}
      					
	             	if((modelNormalization.equalsIgnoreCase("UserNor")))
	             		voteSum += (currentWeight * (activeUserRatingOnSimItem));
	             	else if((modelNormalization.equalsIgnoreCase("MovNor")))
	             		voteSum += (currentWeight * (myTrainingMMh.getAverageRatingForMovie(mid)));
	             	if((modelNormalization.equalsIgnoreCase("Simple")))
	             		voteSum += (currentWeight * (activeUserRatingOnSimItem));
	             	if((modelNormalization.equalsIgnoreCase("SigmaNor")))
	             		voteSum += (currentWeight * (activeUserRatingOnSimItem * myTrainingMMh.getStandardDeviationForUser(activeUser)
	             																+ myTrainingMMh.getAverageRatingForUser(activeUser)));
	             		
	             	myTotal++;
    		 
		             		
	             	myTotal++;
	             	

        			//weight+1
        /*			currentWeight = myWeights.get(i) + 1;
	             	weightSum += Math.abs(currentWeight);				// see no Math.abs(), for adjusted cosine I think there is no need (weight >0 always)
	             	activeUserRatingOnSimItem = P.get(mid-1,activeUser-1);           	
	             	if(!(modelNormalization.equalsIgnoreCase("Simple")))
	             		voteSum += (currentWeight * (activeUserRatingOnSimItem + activeUserAverage));
	             	else
	             		voteSum += (currentWeight * (activeUserRatingOnSimItem ));	             		
	             	 myTotal++;
        			*/
            	}
        		
         } //end of for
      
        //predict and send back         
        if(weightSum!=0) {
       	 				voteSum *= (1.0 / weightSum);
       	 				
	        	 			if((modelNormalization.equalsIgnoreCase("MovNor")))
	       	 					 answer = voteSum; 	
	       	 				else if((modelNormalization.equalsIgnoreCase("UserNor")))
	      	 					 answer = voteSum + activeUserAverage; 	
	       	 				if((modelNormalization.equalsIgnoreCase("SigmaNor")))
	      	 					 answer = voteSum ; 	
	       	 				else if((modelNormalization.equalsIgnoreCase("Simple")))             	 					
	       	 					answer = voteSum;      	 					
        				   }		
        else 
       	 answer = activeUserAverage ;
       // System.out.println("prediction="+ answer);
        
          			return answer;                  
          }


 /****************************************************************************************************/
     /**
      *   @param item_active, item_target, no. of neighbouring items to consider
      *   @return similarity between two items using Adjusted cosine similarity (Ratings came from SVD) 
      */
         
         // It can be used if I am using the simple Item based CF
         public double findItemSimilarity_SVD(int activeItem, int myItem, int activeUser)
         {
         	//----------------------------------------------------------------
         	// we have to calculate sim on left matrix which is of dimensions
         	// m x k --- i.e. ratings of k pseudo users on m items
         	// here I have: m x n = items x users
         	//----------------------------------------------------------------
         	        	
         	double topSum =0.0;
         	double bottomSumActive =0.0;
         	double bottomSumTarget =0.0;
         	double rating1 = 0.0;
         	double rating2 = 0.0;
         	double sim =0;        	
         	double activeUserAvg = myTrainingMMh.getAverageRatingForUser(activeUser);
         	
             // for all the common users
             for (int i = 0; i < k; i++)
             {            
                   // get their ratings from "SVD LEFT (SV) MATRIX"               
             	//if(activeItem<1682 && myItem<1682)
             	{
             		if(FTFlag==false) {
             		rating1 = left.get(activeItem-1,i);                
 	                rating2 = left.get(myItem-1,i);               
             		}
             		
               		//FT
            		else{
            			
            			if(FTDataWithMinRat <=1){            			
            				rating1 = left.get(activeItem,i);                
    		                rating2 = left.get(myItem,i);
            			}
            		
	            		else{
	            			rating1 = left.get(i,activeItem);                
    		                rating2 = left.get(i,myItem);
	            		}
            		}
             		
 	                //user or mov nor
 	                if(!(modelNormalization.equalsIgnoreCase("Simple")))
 	                {
 		                topSum += rating1 * rating2;		            
 		                bottomSumActive += Math.pow(rating1, 2);
 		                bottomSumTarget += Math.pow(rating2, 2);
 	                }
 	                
 	                //simple
 	                else
 	                {
 	         /*       	rating1-= activeUserAvg;
 	                	rating2-= activeUserAvg;		//make offset , no man not like this
 	                	*/
 	                	topSum += rating1 * rating2;		            
 		                bottomSumActive += Math.pow(rating1, 2);
 		                bottomSumTarget += Math.pow(rating2, 2);
 	                }	                	
             	}                
             }
             
             double  bottomSum = Math.sqrt(bottomSumTarget) * Math.sqrt(bottomSumActive);        	
             
             if (bottomSum == 0.0) 
             	sim = 0.0;            
             else 
             	sim = topSum / bottomSum;   
             
           /*  System.out.println("sim found is = " + sim);
               System.out.println("common users found are = " +myUsers.size());*/
             
             return sim;            
         	
         }
         
/****************************************************************************************************/
//User-based CF  
/****************************************************************************************************/
      
      /**
       * Predicts the rating that activeUser will give targetMovie.
       *
       * @param  activeUser  The user.
       * @param  targetMovie  The movie.
       * @param  neigh the no. of neighbour to use 
       * @return The rating we predict activeUser will give to targetMovie. 
       */
      
         public double recommendUserBased(int activeUser, int targetMovie, int totalNeighbours) 
         {
         	double entry = 0;
         	double prediction = 0;
             int uid = 0;
             int totalUsersWhoSawTargetMovie = 0;
             double NeighUserRatingOnTargetItem = 0.0;
             double weightSum = 0.0;
             double voteSum   = 0.0;
             double activeUserAverage = 0.0;
             double answer = 0.0;
                     
             LongArrayList users = myTrainingMMh.getUsersWhoSawMovie(targetMovie);   //get movies seen by this user
         	totalUsersWhoSawTargetMovie = users.size();   
         	activeUserAverage = myTrainingMMh.getAverageRatingForUser(activeUser);   //active user average
         	
         	OpenIntDoubleHashMap userIdToWeight = new OpenIntDoubleHashMap();
     	    IntArrayList myUsers      			= new IntArrayList();
     	    DoubleArrayList myWeights 			= new DoubleArrayList();
     	    double currentWeight;
     	    
     	    //GO through all the users, who saw target movie and find similar users
     	    //We are doing the usual steps, the only difference is that, we would measure the
     	    //similarity between items or users with their profiles limited to K dimesnions
     	    //So if K is small, then it is fine, else there is no need of it (means , one
     	    //cannot claim that, it is scalable)
         	for (int m =0; m<totalUsersWhoSawTargetMovie; m++)
         	{    		
         		 uid = MemHelper.parseUserOrMovie(users.getQuick(m));  
         //	     currentWeight  = findUserSimilarity_SVD(activeUser, uid, targetMovie); //active item, item we want to find sim with
         	     currentWeight  = findUserSimilarityViaCosine_SVD(activeUser, uid, targetMovie); 
         	   	 userIdToWeight.put(uid, currentWeight);
         	 }
         	
         	// sort the weights
         	 myUsers    = userIdToWeight.keys();
              myWeights 	= userIdToWeight.values();
              userIdToWeight.pairsSortedByValue(myUsers, myWeights);
         	
              // take weighted sum
              for (int i = totalUsersWhoSawTargetMovie-1, myTotal=0; i >=0; i-- )       
              {    	   
              		if(myTotal == totalNeighbours) break;              	
              		uid = myUsers.get(i); 	
              		
              		
              		         		
     	         	/*	currentWeight = myWeights.get(i);    	             	
     	         		weightSum += Math.abs(currentWeight);				// see no Math.abs(), for adjusted cosine I think there is no need (weight >0 always)
     	             	NeighUserRatingOnTargetItem = P.get(targetMovie-1, uid-1);            	
     	             	if(!(modelNormalization.equalsIgnoreCase("Simple")))
     	             		voteSum += (currentWeight * (NeighUserRatingOnTargetItem + activeUserAverage));
     	             	else
     	             		voteSum += (currentWeight * (NeighUserRatingOnTargetItem ));
     	             		myTotal++;
     	      */
     	      
              			//Weight +1
              			currentWeight = myWeights.get(i);    	             	
     	         		weightSum += Math.abs(currentWeight);				// see no Math.abs(), for adjusted cosine I think there is no need (weight >0 always)
     	             	
     	         		//SML
     	         		if(FTFlag==false)
     	         			NeighUserRatingOnTargetItem = Prediction_Matrix.get(targetMovie-1, uid-1);
     	         		
     	         		//FT
     	         		else{
     	         			if(FTDataWithMinRat <=1)
     	         				NeighUserRatingOnTargetItem = Prediction_Matrix.get(targetMovie, uid);
     	         			else
     	         				NeighUserRatingOnTargetItem = Prediction_Matrix.get(uid, targetMovie);
     	         		}
     	         		
     	             	if((modelNormalization.equalsIgnoreCase("UserNor")))
     	             		voteSum += (currentWeight * (NeighUserRatingOnTargetItem));
     	             	else if((modelNormalization.equalsIgnoreCase("MovNor")))
     	             		voteSum += (currentWeight * (NeighUserRatingOnTargetItem + myTrainingMMh.getAverageRatingForMovie(targetMovie)));
     	             	else if((modelNormalization.equalsIgnoreCase("Simple")))
     	             		voteSum += (currentWeight * (NeighUserRatingOnTargetItem - myTrainingMMh.getAverageRatingForUser(uid) ));
     	             	else if((modelNormalization.equalsIgnoreCase("SigmaNor")))
     	             		voteSum += (currentWeight * (NeighUserRatingOnTargetItem *  myTrainingMMh.getStandardDeviationForUser(uid) +  myTrainingMMh.getAverageRatingForUser(uid)));
     	             	myTotal++;        	
              		
               } //end of for
            
              //predict and send back         
              if(weightSum!=0) {
             	 				voteSum *= (1.0 / weightSum);
             	 				 
             	 				if((modelNormalization.equalsIgnoreCase("MovNor")))
             	 					 answer = voteSum; 	
             	 				else if((modelNormalization.equalsIgnoreCase("UserNor")))
            	 					 answer = voteSum + activeUserAverage; 	
             	 				if((modelNormalization.equalsIgnoreCase("SigmaNor")))
            	 					 answer = voteSum; 	
             	 				else if (modelNormalization.equalsIgnoreCase("Simple"))            	 					
             	 					 answer = voteSum + activeUserAverage;
             	 		
              				  }		
              else
 				 	answer = activeUserAverage;
             // System.out.println("prediction="+ answer);
              
                			return answer;                  
                }

/****************************************************************************************************/
      
    /**
     *   @param item_active, item_target, no. of neighbouring items to consider
     *   @return similarity between two items using Adjusted cosine similarity (Ratings came from SVD) 
     */
          
         public double findUserSimilarity_SVD(int activeUser, int expectedNeighbouringUser, int targetMovie)
         {
         	//----------------------------------------------------------------
         	// we have to calculate sim on right matrix which is of dimensions
         	// k x n --- i.e. ratings of n users on k pseudo items
         	// here I have: m x n = items x users
         	//----------------------------------------------------------------
         	        	
         	double topSum =0.0;
         	double bottomSumActive =0.0;
         	double bottomSumTarget =0.0;
         	double rating1 = 0.0;
         	double rating2 = 0.0;
         	double sim =0;            	
         	double targetMovieAvg = myTrainingMMh.getAverageRatingForMovie(targetMovie);
         	
             // for all the common items
             for (int i =0; i< k; i++)
             {            
                 // get their ratings from "SVD right (SV) MATRIX"               
               		
             		//SML
             		if(FTFlag==false){
	                		rating1 = right.get(i, activeUser-1);                
	    	                rating2 = right.get(i, expectedNeighbouringUser-1);                
             		}
             		
             		//FT
             		else{
             			if(FTDataWithMinRat <=1){
             				rating1 = right.get(i, activeUser);                
 	    	                rating2 = right.get(i, expectedNeighbouringUser);
             			}
             				
             			else{
             				rating1 = right.get(activeUser,i);                
 	    	                rating2 = right.get(expectedNeighbouringUser,i);
             			}          			
             		}
             		
 	                //user or mov nor
 	                if(!(modelNormalization.equalsIgnoreCase("Simple")))
 	                {
	    	                topSum += rating1 * rating2;    	            
	    	                bottomSumActive += Math.pow(rating1, 2);
	    	                bottomSumTarget += Math.pow(rating2, 2);
 	                }
 	                
 	                //simple
 	                else
 	                {
 	              /*  	rating1-=targetMovieAvg;			//make ofset
 	                	rating2-=targetMovieAvg;
 	               */
 	                	
 	                	topSum += rating1 * rating2;    	            
	    	                bottomSumActive += Math.pow(rating1, 2);
	    	                bottomSumTarget += Math.pow(rating2, 2);
 	                }
                             
             }
             
             double  bottomSum = Math.sqrt(bottomSumTarget) * Math.sqrt(bottomSumActive);        	
             
             if (bottomSum == 0.0) sim = 0.0;            
             else sim =topSum / bottomSum;   
             
           /*  System.out.println("sim found is = " + sim);
               System.out.println("common users found are = " +myUsers.size());*/
             
             return sim;            
         	
         }

//------------------------------------------------------------------------------------------------------------------------
 // Lets us see what is diff if we use cosine
         
         public double findUserSimilarityViaCosine_SVD(int activeUser, int expectedNeighbouringUser, int targetMovie)
         {

         	//----------------------------------------------------------------
         	// we have to calculate sim on right matrix which is of dimensions
         	// k x n --- i.e. ratings of n users on k pseudo items
         	// here I have: m x n = items x users
         	//----------------------------------------------------------------
         	        	
         	double topSum =0.0;
         	double bottomSumActive =0.0;
         	double bottomSumTarget =0.0;
         	double rating1 = 0.0;
         	double rating2 = 0.0;
         	double sim =0;            	
         	double targetMovieAvg = myTrainingMMh.getAverageRatingForMovie(targetMovie);
         	
             // for all the common items
             for (int i =0; i< k; i++)
             {            
                 // get their ratings from "SVD right (SV) MATRIX"               
         		//SML
         		if(FTFlag==false){
             		rating1 = right.get(i, activeUser-1);                
 	                rating2 = right.get(i, expectedNeighbouringUser-1);                
         		}
         		
         		//FT
         		else{
         			if(FTDataWithMinRat <=1){
         				rating1 = right.get(i, activeUser);                
	    	                rating2 = right.get(i, expectedNeighbouringUser);
         			}
         				
         			else{
         				rating1 = right.get(activeUser,i);                
	    	                rating2 = right.get(expectedNeighbouringUser,i);
         			}          			
         		}
         		
 	                
 	                //user or mov nor
 	                if(!(modelNormalization.equalsIgnoreCase("Simple")))
 	                {
	    	                topSum += rating1 * rating2;    	            
	    	                bottomSumActive += Math.pow(rating1, 2);
	    	                bottomSumTarget += Math.pow(rating2, 2);
 	                }
 	                
 	                //simple
 	                else
 	                {    	              	
 	                	topSum += rating1 * rating2;    	            
	    	                bottomSumActive += Math.pow(rating1, 2);
	    	                bottomSumTarget += Math.pow(rating2, 2);
 	                }
                             
             }
             
             double  bottomSum = Math.sqrt(bottomSumTarget) * Math.sqrt(bottomSumActive);        	
             
             if (bottomSum == 0.0) sim = 0.0;            
             else sim =topSum / bottomSum;   
             
           /*  System.out.println("sim found is = " + sim);
               System.out.println("common users found are = " +myUsers.size());*/
             
             return sim;            
         	
         }
         

/****************************************************************************************************/

 /**
  * Using RMSE as measurement, this will compare a test set
  * (in MemHelper form) to the results gotten from the recommender
  *  
  * @param testmh the memhelper with test data in it   //check this what it meant........................Test data?///
  * @return the rmse in comparison to testmh 
  */

      		    public void testWithMemHelper(MemHelper testmh, int neighbours)     
    		    {
    		        RMSECalculator rmse = new RMSECalculator();
    		        
    		        IntArrayList users;
    				LongArrayList movies;
    				double mov, pred,actual, uAvg;
    				String blank			 	= "";
    		        int uid, mid, total			= 0;    		       	
    		        int totalUsers				= 0;
    		        int totalExtremeErrors 		= 0 ;
    		        int totalEquals 			= 0;
    		        int totalErrorLessThanPoint5= 0;
    		        int totalErrorLessThan1 	= 0;    	        
    		        		        
    		        // For each user, make recommendations
    		        users = testmh.getListOfUsers();
    		        totalUsers= users.size();
    		        
    		        double uidToPredictions[][] = new double[totalUsers][101]; // 1-49=predictions; 50-99=actual; (Same order); 100=user average
    		        
    		        //________________________________________
    		        
    		        for (int i = 0; i < totalUsers; i++)        
    		        {
    		            uid = users.getQuick(i);       
    		            movies = testmh.getMoviesSeenByUser(uid);
    		           // System.out.println("now at " + i + " of total " + totalUsers );
    		            
    		            for (int j = 0; j < movies.size(); j++)             
    		            {
    		            	total++;
    		                mid = MemHelper.parseUserOrMovie(movies.getQuick(j));
    		                
    		           //     if (mid ==-1)  System.out.println(" rating error--> uid, mid -->" + uid + "," + mid );
    		                
    		               // double rrr = recommend(uid, mid, blank);                
    		                double rrr = recommend(uid, mid, neighbours);
    		                
    		                double myRating=0.0;
    		                
    		                //if (rrr!=0.0)                 
    		                      {
    		                	
    		                			myRating = testmh.getRating(uid, mid);			 		// get actual ratings?

    		                			//System.out.println(rrr+", "+ myRating);
    		                            
    		                			if (myRating==-99 )                           
    		                               System.out.println(" rating error, uid, mid, rating" + uid + "," + mid + ","+ myRating);
    		                           
    		                            if(rrr>5 || rrr<=0)
    		                            {
    		         /*                   		System.out.println("Prediction ="+ rrr + ", Original="+ myRating+ ", mid="+mid 
    		                            		+", NewMid="+ myMoviesMap.get(mid)+ ", uid="+uid
    		                            		+"No users who rated movie="+ mh.getNumberOfUsersWhoSawMovie(mid) + 
    											", User saw movies="+mh.getNumberOfMoviesSeen(uid));*/
    		                            }
    		                            
    		                            if(rrr>5 || rrr<-1)
    		                            	totalExtremeErrors++;
    		                            
    		                            else if(Math.abs(rrr-myRating)<=0.5)
    		                            	totalErrorLessThanPoint5++;
    		                            
    		                            
    		                            else if(Math.abs(rrr-myRating)<=1.0)
    		                            	totalErrorLessThan1++;
    		                            
    		                            else if (rrr==myRating)
    		                            	totalEquals++;
    		                            
    		                          		                            
    		                            //-------------
    		                            // Add ROC
    		                            //-------------
    		                           // if(rrr!=0)
    		                            	rmse.ROC4(myRating, rrr, myClasses, myTrainingMMh.getAverageRatingForUser(uid));		
    		                		                          
    		                            //-------------
    		                            //Add Error
    		                            //-------------
    		                           
    		                           // if(rrr!=0)
    		                            {
    		                            	rmse.add(myRating,rrr);    		                            	   
    		                            	midToPredictions.put(mid, rrr);                            	                                
    		                            }		
    		            
    		                            //-------------
    		                            //Add Coverage
    		                            //-------------

    		                             rmse.addCoverage(rrr);                                 
    		                		  }         
    		            
    		                      		//Add values to pair t
    		                      		if(loopNumber==1)
    		                      			rmse.addActualToPairT(rrr);			//BASE
    		                      		else
    		                      			rmse.addPredToPairT(rrr);			//TO WHICH WE LL COMPARE
    		                      			
    		            }//end of movies for
    		            
    		            //--------------------------------------------------------
    		            //A user has ended, now, add ROC and reset
    		            rmse.addROCForOneUser();
    		            rmse.resetROCForEachUser();
    		            rmse.addMAEOfEachUserInFinalMAE();
    		            rmse.resetMAEForEachUser();
    		            
    		            //sort the pairs (ascending order)
    		    		IntArrayList keys = midToPredictions.keys();
    		    		DoubleArrayList vals = midToPredictions.values();        		
    		    		midToPredictions.pairsSortedByValue(keys, vals);
    		    		
    		    		int movSize = midToPredictions.size();
    		    		if(movSize>50)
    		    			movSize = 50;      	
    		    		 
    		    		for(int x=0;x<movSize;x++)
    		    		{
    		    		  mov = keys.getQuick(x);
    		    		  pred = vals.getQuick(x);
    		    		  actual = testmh.getRating(uid,(int) mov);	
    		    		  uidToPredictions[i][x] = pred;
    		    		  uidToPredictions[i][50+x] = actual;
    		    		}//end for
    		    	    
    		    		 uidToPredictions[i][100] = myTrainingMMh.getAverageRatingForUser(uid);
    		    		 midToPredictions.clear();
    		    		 
    		        }//end of user for	   
    		    
    		        MAE =  rmse.mae(); 
    		        MAEPerUser = rmse.maeFinal(); 
    		        ROC = rmse.getSensitivity();
    		    
    		    
    		         //-------------------------------------------------
    		         //Calculate top-N    		            
    		    		
    		            for(int i=0;i<5;i++)	//N from 5 to 30
    		            {
    		            	for(int j=0;j<totalUsers;j++)//All users
    		            	{
    		            		//get user avg
    		            		uAvg =  uidToPredictions [j][100];	
    		            		
    		            		for(int k=0;k<((i+1)*5);k++)	//for topN predictions
    		            		{
    		            			//get prediction and actual vals
    		    	        		pred =  uidToPredictions [j][k];
    		    	        		actual =  uidToPredictions [j][50+k];
    		    	        		
    		    	        		//add to topN
    		    	        		rmse.addTopN(actual, pred, myClasses, uAvg);
    		            		}
    		            		
    		            		//after each user, first add TopN, and then reset
    		            		rmse.AddTopNPrecisionRecallAndF1ForOneUser();
    		            		rmse.resetTopNForOneUser();   		            		
    		            	
    		            	} //end for
    		            	
    		            	//Now we finish finding Top-N for a particular value of N
    		            	//Store it 
    		            	precision[i]=rmse.getTopNPrecision();
    		            	recall[i]=rmse.getTopNRecall();
    		            	F1[i]=rmse.getTopNF1(); 
    		            	
    		            	//Reset all topN values
    		            	
    		            	rmse.resetTopNForOneUser();
    		            	rmse.resetFinalTopN();
    		            	
    		            
    		            } //end of for   		        	
    		        	
    		     /* System.out.println("totalExtremeErrors="+totalExtremeErrors + ", Total ="+total);
    		        System.out.println("totalErrorLessThanPoint5="+totalErrorLessThanPoint5 );	       
    		        System.out.println("totalErrorLessThan1="+totalErrorLessThan1 );
    		        System.out.println("totalEquals="+totalEquals );  */    		        
    		        
    		        //Get variance and Mean of MAE    		          
    		          SDMAE  	 	= rmse.getVarianceInMAE();		            
    		          SDROC   		= rmse.getVarianceInROC();
    		          SDPrecision   = rmse.getVarianceInTopN(1);
    		          SDRecall   	= rmse.getVarianceInTopN(2);
    		          SDF1   		= rmse.getVarianceInTopN(3);
    		            
    		          
    		          //Get pair t value
    		          pairTValue = rmse.getPairT();
    		          
    		         //Reset final values
    		          rmse.resetValues();   
    		          rmse.resetFinalROC();
    		          rmse.resetFinalMAE();  	
    		          rmse.resetPairTPrediction();			//after each loop, we will reset predictions 
    		       
     }//end of function


	//---------------------------------------------
 
 public void openFiles()
	 {
		 String path =myPath + "/Results/" + CFORSimpleSVD+ "/" +modelNormalization + "/";
		 
		 
		 try{
			 myWriter[0] = new FileWriter(path +"MAE_iterativeSVD"+modelNormalization+".csv", true); //true, append in existing file
			 myWriter[1] = new FileWriter(path +"MAEPerUser_iterativeSVD"+modelNormalization+".csv", true); //true, append in existing file
			 myWriter[2] = new FileWriter(path +"ROC_iterativeSVD"+modelNormalization+".csv", true); //true, append in existing file
			 
			 myWriter[3] = new FileWriter(path +"Precision5_iterativeSVD"+modelNormalization+".csv", true); //true, append in existing file
			 myWriter[4] = new FileWriter(path +"Recall5_iterativeSVD"+modelNormalization+".csv", true); //true, append in existing file
			 myWriter[5] = new FileWriter(path +"F15_iterativeSVD"+modelNormalization+".csv", true); //true, append in existing file
			 
			 myWriter[6] = new FileWriter(path +"Precision10_iterativeSVD"+modelNormalization+".csv", true); //true, append in existing file
			 myWriter[7] = new FileWriter(path +"Recall10_iterativeSVD"+modelNormalization+".csv", true); //true, append in existing file
			 myWriter[8] = new FileWriter(path +"F110_iterativeSVD"+modelNormalization+".csv", true); //true, append in existing file
			 
			 myWriter[9] = new FileWriter(path +"Precision15_iterativeSVD"+modelNormalization+".csv", true); //true, append in existing file
			 myWriter[10] = new FileWriter(path +"Recall15_iterativeSVD"+modelNormalization+".csv", true); //true, append in existing file
			 myWriter[11] = new FileWriter(path +"F115_iterativeSVD"+modelNormalization+".csv", true); //true, append in existing file
			 
			 myWriter[12] = new FileWriter(path +"Precision20_iterativeSVD"+modelNormalization+".csv", true); //true, append in existing file
			 myWriter[13] = new FileWriter(path +"Recall20_iterativeSVD"+modelNormalization+".csv", true); //true, append in existing file
			 myWriter[14] = new FileWriter(path +"F120_iterativeSVD"+modelNormalization+".csv", true); //true, append in existing file
			 
			 myWriter[15] = new FileWriter(path +"MAE_SD_iterativeSVD"+modelNormalization+".csv", true); //true, append in existing file
			 myWriter[16] = new FileWriter(path +"ROC_SD_iterativeSVD"+modelNormalization+".csv", true); //true, append in existing file
			 myWriter[17] = new FileWriter(path +"Precision_SD_iterativeSVD"+modelNormalization+".csv", true); //true, append in existing file
			 myWriter[18] = new FileWriter(path +"Recall_SD_iterativeSVD"+modelNormalization+".csv", true); //true, append in existing file
			 myWriter[19] = new FileWriter(path +"F1_SD_iterativeSVD"+modelNormalization+".csv", true); //true, append in existing file
			 myWriter[20] = new FileWriter(path +"PairT_iterativeSVD"+modelNormalization+".csv", true); //true, append in existing file
		 }
		 
		 catch( Exception E) {
			 		E.printStackTrace(); 			 
		 }
	 }


	//---------------------------------------------
	   
	 public void closeFiles()
	 {
	  		 
		 try{
			 for(int i=0;i<=20;i++)
				 myWriter[i].close();
			 
		 }
		 
		 catch( Exception E) {
			 		E.printStackTrace(); 			 
		 }
	 }

	 
 
}
