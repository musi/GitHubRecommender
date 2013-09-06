package netflix.algorithms.memorybased.memreader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.*;
import netflix.memreader.MemHelper;
import netflix.recommender.NaiveBayesRec;
import netflix.memreader.DivideIntoSets;
import netflix.memreader.TrainingTestSet;
import netflix.rmse.RMSECalculator;
import cern.colt.list.IntArrayList;
import cern.colt.list.LongArrayList;

/**
 * Dan's personal testing class.  Not recommended for people
 * who are not Dan.
 * 
 * @author lewda
 *
 */


/************************************************************************************************************************/
public class MyRecommenderCoverage 
/************************************************************************************************************************/

{
    private int    givenN;					// Only n values are given for a user in a training set (only n are given)
    private int    allButOne;				// All values except one is given for a user (only one is not there)
    private int	   givenOption [];
    private int    numberOfSample;	
    private int	   whichVersionIsCalled;	// We will send it to the filter class, to have diff weights for simple and deviation 
    private int	   pearsonDeviationNotWorking;	//In Pearson, user rating and user avg is the same	
    private double RMSEUAvg[];
    private double RMSEMAvg[];
    private double RMSEPre[];    
    private double MAEUAvg[];
    private double MAEMAvg[];
    private double MAEPre[];
    
    
    
    double finalCorrError;
    double finalUserAvgError;
    double finalMovAvgError;
    double corrPrediction;
    double userPrediction;
    double movPrediction;
    private int errorz;
    private int totalNans;
    
    
    String information;
    String whichRecommender;
    String fileToWriteRecResults;
    String myPath;
    String simChoices[];
    int    neighbourhoodSize;
    int    neighbourhoodInc;
    int    totalRec;
    long   myT1;
    long   myT2;
    
    //Start up RMSE count
    RMSECalculator rmse;
    RMSECalculator movrmse;
    RMSECalculator usermse; 
    
    TrainingTestSet TTS;
	DivideIntoSets  Ds;
	MemHelper 		MMh;	
    MemHelper 		MTestMh;
    
    //want to write Data into a file?
    boolean 		writeSentitivityData;
    boolean 		writeRecData;
    BufferedWriter  roc4Writer;
    BufferedWriter  recWriterRough;
    BufferedWriter  recWriterNeighbourAndTime[];    
    NumberFormat    nf;
    
    //Naive Bayes Rec
    NaiveBayesRec myNB;
    
    // Classes = {10,5}
     int classes;
    
  //____________________________
    
    public MyRecommenderCoverage( )
    {
    	
    	
    }
    
  /**********************************************************************************************/
    
    public MyRecommenderCoverage( String trainObject, 
    		              String testObject
    		            
    					 )
    
    {
    	givenN				=2;				 // can be anything
    	givenOption			= new int[8]; 	// coorelation
    	numberOfSample      =1;  			// 2 for one set
    	
    	errorz=0;
     
    //simple to send in filter weight in corr case --> simple, deviation based corr
    //	if (whichRec.equalsIgnoreCase("Simple")) whichVersionIsCalled = 1;
    //	else whichVersionIsCalled = 2;
    	
        finalCorrError 		= 0.0;
        finalUserAvgError 	= 0.0;
        finalMovAvgError 	= 0.0;
        corrPrediction 		= 0.0;
        userPrediction 		= 0.0;
        movPrediction 		= 0.0;
        totalRec			= 0;
        totalNans			= 0;
        pearsonDeviationNotWorking=0;
        
    
        //_____________________________
    
      
    
        MMh			 			= new MemHelper (trainObject);
        MTestMh 	 			= new MemHelper (testObject);
      
        
        
    	 nf = new DecimalFormat("#.######");	//upto 5 digits
       	 simChoices  = new String [8];
    	
    	//make objects
    	TTS = new TrainingTestSet();    //just to get files of training sets
		Ds  = new DivideIntoSets();
	        
        //Start up RMSE count
        rmse 		= new RMSECalculator();
        movrmse 	= new RMSECalculator();
        usermse 	= new RMSECalculator();
	
        //Make Naive Bayes Object
        myNB = new NaiveBayesRec (trainObject,testObject ); //train, test
        myT2 = myT1 = 0;
        
        //Classes
        classes = 5;
    }
    
 /**********************************************************************************************/    

	public static void main(String[] args)  
    {
		String p = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\sml_storedFeaturesRatingsTF.dat";
		MyRecommenderCoverage mr= new MyRecommenderCoverage(); 
	    
		long startTime = System.currentTimeMillis();      
           mr.makeCorrPrediction(p,p);  
           mr.DisplayResults();         
        long endTime = System.currentTimeMillis();
        
        System.out.println("Total time taken: " 		+ (endTime - startTime)      + " ms.");     
           
    }//end of main
	
/*********************************************************************************************/
/**********************************************************************************************/

	public  void makeCorrPrediction(String mainFile, String testFile)  
    {
		
		MyRecommenderCoverage mr= new MyRecommenderCoverage(mainFile,testFile);
		long startTime = System.currentTimeMillis();       
        mr.makePrediction();       
        long endTime = System.currentTimeMillis();
                     
        System.out.println("Fianl Total time taken: " 		+ (endTime - startTime)      + " ms.");
        
           
    }//end of main


	
/**********************************************************************************************/
/**********************************************************************************************/
	
	public void makePrediction()	
	{
		System.out.println("Come to make prediction");
		int moviesSize = 0;
		//Set options
		
		for (int iteration=0;iteration<8;iteration++) //0-7	        
	     {	        
	       	if     (iteration==0)   {	givenOption[0]=1;  simChoices[0] ="Corr";}
	        else if(iteration==1) 	{	givenOption[1]=4;  simChoices[1] ="VS";}
	        else if(iteration==2) 	{	givenOption[2]=2;  simChoices[2] ="CorrDV";}
	        else if(iteration==3) 	{	givenOption[3]=8;  simChoices[3] ="IUF";}
	        else if(iteration==4) 	{	givenOption[4]=17; simChoices[4] ="CorrCA";}
	        else if(iteration==5) 	{	givenOption[5]=18; simChoices[5] ="CorrDVCA";}
	        else if(iteration==6)	{	givenOption[6]=20; simChoices[6] ="VSCA";}
	        else 					{	givenOption[7]=24; simChoices[7] ="IUFCA";}
	        
	      }
		
	       	
		//open the files to write results for roc
		if (writeSentitivityData) openRocFile();
		if (writeRecData) openRecFile(2);					// how much files to open (8)
			
        
 for (int neighbourLoop=50;neighbourLoop<=200; neighbourLoop+=30 ) //step from let 5 untill all neighbours
  {
	   System.out.println (" Rec from neighbour size of " + neighbourLoop);
       
		//___________________________________________________________________
        
      for (int algorithm=0;algorithm<8;algorithm++) //0-7        
      {    	
    
    	if (algorithm !=2) continue;    	
    	
    	myT1 = System.currentTimeMillis();
        
        FilterAndWeight f = new FilterAndWeight(MMh, givenOption[algorithm]); //with mmh object        
                   
        // For each user (in test set), make recommendations
        IntArrayList users = MTestMh.getListOfUsers(); 		//from CusToMovie.key        
        LongArrayList movies;
        double rating;
        double nbRating;
        int uid, mid;

  	  double alpha = 0;
	  double beta  = 1;

  //for(int d1=0;d1<11;d1++, alpha+=0.1, beta-=0.1)
   {
	//  System.out.println("alpha ="+ (alpha) + ", beta =" + beta);
	  
           
        for (int i = 0; i < users.size(); i++)        
        {
            uid = users.getQuick(i);          
            movies = MTestMh.getMoviesSeenByUser(uid); //get movies seen by this user
          //  movies=allM;                
           
            moviesSize = movies.size();
          // If There are less than 4 movies in the training set, we eliminate this user from consideration
          // So Filter1: No of Movies see by user >=5  
            //System.out.println("user saw movies =" + moviesSize);
            
         //  if(moviesSize >5)
           {
              for (int j = 0; j < moviesSize; j++)            
	           {
	        	  
	                mid	= MemHelper.parseUserOrMovie(movies.getQuick(j));                
	                
	                     rating 	= f.recommendS(uid, mid, neighbourLoop, 2);
		
	                     getAndAddError(rating, uid, mid, classes);
		                	//getAndAddError((nbRating+rating)/2.0, uid, mid);
		                
		
		                if(rating==0) totalNans++;
		                
		                //let us write after 100 entires
		                if(writeSentitivityData == true && totalRec==50)	      
		                { 
		                	writeRocIntoFile(); 
		                	totalRec=0;
		                	rmse.resetROC();	                	
		                }           
		   
		                totalRec++;
	            } //filtering of movies (>5)
	         } //end of all movies	          
              
           // System.out.println(" User ="+ (i) + ",Error MAE=" + rmse.mae() + ", sensitivity =" + rmse.getSensitivity() + ", Coverage ="+ rmse.getItemCoverage());
              
	    } //end processing all users
   		
        
        //Print results
        myT2 = System.currentTimeMillis();
        
    
        
        System.out.println();
        System.out.println(" neighours=" + neighbourLoop );
       // System.out.println("Tatal sample failed in Pearson =" ); f.printPearsonError();
        System.out.println("Final RMSE --:" 			+ rmse.rmse());
        System.out.println("Final Movie Avg RMSE: " 	+ movrmse.rmse());
        System.out.println("Final User  Avg  RMSE: " 	+ usermse.rmse());       
        System.out.println("Final Mae: " 				+ rmse.mae());
        System.out.println("Final Movie Avg Mae: " 		+ movrmse.mae());
        System.out.println("Final User  Avg  Mae: " 	+ usermse.mae());       
        System.out.println("Total Nans: " 	+ totalNans);
        System.out.println("Coverage: " 	+ rmse.getItemCoverage());
        System.out.println("sensitivity --:" 			+ rmse.getSensitivity());
        System.out.println("Total time taken:----- "	+ (myT2 - myT1)      + " ms.");
       
        
        if (writeRecData==true)    {writeRecToFile(algorithm, neighbourLoop, (myT2-myT1),
        											rmse.rmse(), rmse.mae(), 
        											usermse.mae(),movrmse.mae() );}
        
        // Here, we can re-set values in the class RMSE
        rmse.resetValues();
        rmse.resetROC();        
        movrmse.resetValues();
        usermse.resetValues();
        totalNans=0;
   			
   			} //end of alpha beta for
        
        }//end of iteration for
    
      // write neighbourhood size and time results in a file      
      if(writeSentitivityData ==true) closeRocFile();

      
   }//end of outer main for

 	System.out.println(" done with all");
 	closeRecFile(2);
         
}//end of function
	
/****************************************************************************************************/

	public void getAndAddError(double rating, int uid, int mid, int classes)	
	{
        
	 // System.out.println(" yes came");
	  double actual 	 = MTestMh.getRating(uid, mid);						//get actual rating against these uid and movieids
      movPrediction		 = MMh.getAverageRatingForMovie(mid);
      userPrediction 	 = MMh.getAverageRatingForUser(uid);
        
      rmse.add	(actual, rating);				//add (actual rating, Predicted rating)
      movrmse.add(actual, movPrediction);		//add (actual rating, Movie_Avg)
      usermse.add(actual, userPrediction);		//add (actual rating, User_Avg)
      
      rmse.addCoverage(rating);					//Add coverage
      rmse.ROC4(actual, rating, classes);

	}

/****************************************************************************************************/
	
 public void writeRocIntoFile() 
 {
	 // write FPR (first), Sensitivity (second) separated by comma
	 try 
	 {

		 roc4Writer.write( (rmse.getFalsePositiveRate()) + "\t\t\t\t" +
				 			(rmse.getSensitivity()));
				 	
		 roc4Writer.newLine();
	 	 
	  }
	
	 catch (IOException e) 
	 
	 {
          System.out.println("Write error!  Java error: " + e);
          //System.exit(1);
      }
  
 }

 /**************************************************************************************************/
 
//-----------------------------------
 
 public void openRocFile() 
 {
 
	 try {		 
			 roc4Writer = new BufferedWriter(new FileWriter(myPath + "roc4.dat"));		 
	   }
     
     catch (Exception E)
     {
   	  System.out.println("error opening the file pointer");
     }
     
 }


 //-----------------------------------
 
 public void openRecFile(int nFiles) 
 {
 
	 try {
		   for (int t=0;t<nFiles; t++)
		   {
			
			  recWriterNeighbourAndTime [t] = new BufferedWriter(new FileWriter(fileToWriteRecResults + simChoices[t]+ ".dat", true));
			   /*String myDir =  myPath + "\\Results2\\" + simChoices[t];
			   new File(myDir).mkdirs(); 								//one folder for each algo
			   recWriterNeighbourAndTime [t] = new BufferedWriter(
					   new FileWriter(myDir + "\\" + fileToWriteRecResults + simChoices[t]+ ".dat", true));*/
				
		   }
		   
	   }
     
     catch (Exception E)
     {
   	  System.out.println("error opening the file pointer of rec");
   	  //System.exit(1);
     }
     
     System.out.println("Rec File Created");
 }


 /**************************************************************************************************/
 
 //---------------------------------
 
 public void closeRocFile() 
 {
 
	 try {
		 	roc4Writer.close();}
	     
     catch (Exception E)
     {
   	  System.out.println("error closing the roc file pointer");
     }
     
 }
 
//---------------------------------
 
public void closeRecFile(int nFiles) 
 {
 
	 try {	 	//recWriterRough.close();
		 
		 for (int i=0;i< nFiles;i++)
		 {
			 recWriterNeighbourAndTime[i].close();
		  }
	 }
	 
     catch (Exception E)
     {
   	  System.out.println("error closing the rec file pointer");
     }
     
 }

 
 /****************************************************************************************************/
 
	public void DisplayResults()	
	{
		System.out.println("Cross Validation results:");
		
		for (int i=0; i<8;i++)		
		{
		    System.out.println(simChoices[i] + "\n" +
		    					"  RMSE Prediction -->" + RMSEPre[i]/(numberOfSample) +
		    					", RMSE User Avg -->"   + RMSEUAvg[i]/(numberOfSample) +
		      					", RMSE Mov Avg -->"    + RMSEMAvg[i]/(numberOfSample) +
		    					"\n MAE Prediction -->" + MAEPre[i]/(numberOfSample) +
		    					", MAE User Avg -->"   + MAEUAvg[i]/(numberOfSample) +
		    					", MAE Mov Avg -->"    + MAEMAvg[i]/(numberOfSample));

		}
	
	}
	
	
/****************************************************************************************************/
	

	 
	public void writeRecToFile(int whichFile, int nSize, long myTimeTaken, 
								double myRmse, double myMae, 
								double userRmse, double movRmse)	
	{

		
		
		try 		//write Number of Users, Time taken, RMSE, MSE		
		{
			
			recWriterNeighbourAndTime[whichFile].write 
											(   nSize + "\t" + myTimeTaken + "\t" +
												nf.format(myRmse) + "\t" + nf.format(myMae) + "\t"
												+ nf.format(userRmse) + "\t" + nf.format(movRmse)											
											);
			
			recWriterNeighbourAndTime[whichFile].newLine();
		}

	catch (Exception e)
	{
		System.out.println (" Error while writin rec neighbour etc data into a file" + e);		
	}
	
	//	System.out.println (" Success while writin rec neighbour etc data into a file");
		
		
	}
	

	
	
}
