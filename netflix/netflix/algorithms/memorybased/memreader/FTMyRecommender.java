package netflix.algorithms.memorybased.memreader;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.*;
import java.util.Random;

import netflix.FtMemreader.*;
import netflix.memreader.DivideIntoSets;
import netflix.memreader.TrainingTestSet;
import netflix.rmse.RMSECalculator;
import cern.colt.list.IntArrayList;
import cern.colt.list.LongArrayList;
import cern.colt.map.OpenIntObjectHashMap;

/**
 * @author Musi
 *
 */

/************************************************************************************************************************/
public class FTMyRecommender

{
    private int	   givenOption [];
    private int    numberOfSample;	
    private Random 			rand;
    
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
    int    totalEntries;
     
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
    int    totalUsers;
    
    //Start up RMSE count
    RMSECalculator rmse;
    RMSECalculator movrmse;
    RMSECalculator usermse; 
    RMSECalculator rmseGiven2;
    RMSECalculator rmseGiven5;
    RMSECalculator rmseGiven10; 
    RMSECalculator usermseGiven5;
    RMSECalculator usermseGiven10;    
    RMSECalculator movrmseGiven5;
    RMSECalculator movrmseGiven10; 
    
    
    TrainingTestSet TTS;
	DivideIntoSets  Ds;
	FTMemHelper 	MMh;	
    FTMemHelper 	MTestMh;
    FTMemHelper 	FTTestMh;
    FTMemHelper 	TestTrainMMh[];
    DivideAndWriteIntoKFolds DK;
    FTNDepthData    FTData;
    
    //want to write Data into a file?
    boolean 		writeSentitivityData;
    boolean 		writeRecData;
    BufferedWriter  roc4Writer;
    BufferedWriter  userVsMoviesWriter;
    BufferedWriter  recWriterRough;
    BufferedWriter  recWriterNeighbourAndTime[];    
    NumberFormat    nf;
    
    
  //____________________________
    
    public FTMyRecommender( )
    {
    	
    	
    }
    
  /**********************************************************************************************/
    
    public FTMyRecommender( String trainObject, 
    		              String testObject, 
    		              String path, 
    		              String info, 			//information about the train set to be write in the training set
    		              boolean roc,			//wanna write roc ? 
    		              boolean rec,			//wanna write rec?
    		              String whichRec,  	//which rec algo to be called
    		              String writeInThis,	//write results in this file
    		              int neighbourz,		//size of neighbours
    		              int neighbourzInc 	//incr in neighbours
    					 )
    
    {
       	givenOption			= new int[8]; 	// coorelation
    	numberOfSample      =1;  			// 2 for one set
    	
    	RMSEUAvg 			= new double [10];
    	RMSEMAvg 			= new double [10];
    	RMSEPre 			= new double [10]; 	//these will be used to write results into a file     	
    	MAEUAvg 			= new double [10];
    	MAEMAvg 			= new double [10];
    	MAEPre	 			= new double [10];
    	totalEntries		=0;
     
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
        totalUsers			= 0;
        
        writeSentitivityData = roc; 							//for writing roc4 related data make it true
        writeRecData 		 = rec; 							//for writing rec related data make it true
        recWriterNeighbourAndTime = new BufferedWriter[8];    	//Write 8 algorithms results
        //_____________________________
    
      
        myPath 					= path;
        MMh			 			= new FTMemHelper (trainObject);
        MTestMh 	 			= new FTMemHelper (testObject);
        FTTestMh 	 			= new FTMemHelper (testObject);
        TestTrainMMh 			= new  FTMemHelper[10];
        FTData					= new FTNDepthData();
        information  			= info;
        whichRecommender 		= whichRec;
        fileToWriteRecResults	= writeInThis;
        neighbourhoodSize		= neighbourz;			//or all the neighbours if they are present 
        neighbourhoodInc 		= neighbourzInc;		// for graph
      
        
        
    	 nf = new DecimalFormat("#.######");	//upto 5 digits
       	 simChoices  = new String [8];
    	
    	//make objects
    	TTS = new TrainingTestSet();    //just to get files of training sets
		Ds  = new DivideIntoSets();
	        
        //Start up RMSE count
        rmse 			= new RMSECalculator();
        movrmse	 		= new RMSECalculator();
        usermse 		= new RMSECalculator();
        rmseGiven2 		= new RMSECalculator();
        rmseGiven5 		= new RMSECalculator();
        rmseGiven10 	= new RMSECalculator();
        usermseGiven5	= new RMSECalculator();
        usermseGiven10 	= new RMSECalculator();    
        movrmseGiven5	= new RMSECalculator();
        movrmseGiven10 	= new RMSECalculator();
    
        DK          = new DivideAndWriteIntoKFolds();
        rand 		= new Random();
        
        myT2 = myT1 = 0;
        
    }
    
/**********************************************************************************************/

	public  void makeCorrPrediction(String mainFile, 
									String testFile, 
									String path, 
									String what, 
									boolean roc, 
									boolean rec, 
									String whichOne,
									String whichFileName,
									int nSize,
									int nInc
									)
    
    {
		

	    FTMyRecommender mr= new FTMyRecommender(				//call full constructore
	    									mainFile, 
	    									testFile, 
	    									path, 
	    									what, 
	    									roc, 
	    									rec, 
	    									whichOne,
	    									whichFileName,
	    									nSize,
	    									nInc
	    									);				//just to compile the testset
	    
	       
		long startTime = System.currentTimeMillis();
        
             
        mr.makePrediction();  						//if we by default call it, it will call from the caller (i.e. the object we created to call this method)
      // mr.DisplayResults();
               
        long endTime = System.currentTimeMillis();
                     
        System.out.println("Fianl Total time taken: " 		+ ((endTime - startTime)*1.0/(1000*60))      + " mins.");
        
           
    }//end of main


	
/**********************************************************************************************/
/**********************************************************************************************/
	
	public void makePrediction()	
	{
		System.out.println("Come to make prediction");
		FTData.LoadNDepthData();
		
		opneUserVsMovies();
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

			
        IntArrayList 	users;
        LongArrayList 	movies;
        int 			mid, uid, moviesSize;        
        double 			rating=0;
        int 			samples=0;
        int 			kForThisDepth;  
        int totalNumberOfMovies =0;
      //___________________________________________________________________
       //LOO
       //___________________________________________________________________
        
  if(whichRecommender.equalsIgnoreCase("LOO"))  
  {
	     double averageRatingForActiveUser=0.0;
	     int totalUsers =0;
	   	IntArrayList totalMovies = new IntArrayList();
    	int totalPredcitions =0;
    	
	     int fixedK =0;	
        if (writeRecData) openRecFile(1);					// how much files to open
        
        
 for (int depth=1;depth<=13;  depth++, fixedK+=50 ) //step from let 5 untill all neighbours
  {
	      kForThisDepth = FTData.getKForDepth(depth);	    
	  	  System.out.println (" Rec from neighbour size of " + kForThisDepth + " equal to layer" + depth);
		
	 //		kForThisDepth = fixedK;
	 
    for (int algorithm=0;algorithm<8;algorithm++) //0-7        
     {    	    
    	if (algorithm >=1) continue;
    	
    	myT1 = System.currentTimeMillis();        
        FTFilterAndWeight f = new FTFilterAndWeight(MMh, 1); //with mmh object	
                           
        // For each user (in test set), make recommendations
        // users = MTestMh.getListOfUsers(); 		//from CusToMovie.key
        users = MMh.getListOfUsers(); 				//from CusToMovie.key
        //users =FTData.getUsersInAdepth(depth);
        System.out.println("all users = "+ users.size());
        
        //___________________________________________
      
        int del=0;
	    int tempM;
	    int breakIt=0;
	    int conditionalBreak=0;
	    
    for (int i = 0; i < users.size(); i++)         
     {
        uid = users.getQuick(i);          
        movies = MMh.getMoviesSeenByUser(uid); //get movies seen by this user
        moviesSize = movies.size();
             
        
   
     
       if (moviesSize >=5) //for user who have rated more than 5 movies            	
        {
    	   totalUsers++;
    	// System.out.println("user " + (++totalUsers) + ", Movies seen =" + moviesSize);
    	   IntArrayList AllButOne = new IntArrayList();
    	   IntArrayList Given5 = new IntArrayList();
    	   IntArrayList Given10 = new IntArrayList();
    	   IntArrayList moviesForActiveUser = new IntArrayList();
    	   
    	   int mySize = movies.size();
    	   
    	   for (int j = 0; j < mySize; j++)            
           {
    		   int oneMovie =FTMemHelper.parseUserOrMovie(movies.getQuick(j));
    		   AllButOne.add( oneMovie);
    		   moviesForActiveUser.add( oneMovie);				   
    		   
           }
   	   
    	   //____________________
    	   //All But One Protocol
    	   //____________________
    	   
    	   for (int j = 0; j < mySize; j++)
    	   {
                mid = moviesForActiveUser.get(j);
                writeuserToFile (mid, MMh.getUsersWhoSawMovie(mid).size());
                
               //code to make sure that we are not keeping target movie into account when we are adding average rating of the acive user into prediction
                averageRatingForActiveUser = MMh.getAverageRatingForUser(uid);
                averageRatingForActiveUser = mySize * averageRatingForActiveUser;
                double subtract = MMh.getRating(uid, mid);
                averageRatingForActiveUser  = (averageRatingForActiveUser - subtract)/(mySize-1);
               
               rating = f.recommendSLOO(uid, mid, kForThisDepth, AllButOne, averageRatingForActiveUser);         
                
                if(rating!=-10 && rating !=-1) {
                	totalNumberOfMovies++; 
                	if (!(totalMovies.contains(mid))) totalMovies.add(mid);
                	totalPredcitions++;
                	getAndAddErrorLOO(rating, averageRatingForActiveUser, uid, mid, 1); //filter movies
                }
       
       
              if(rating ==-1) {
                	totalNumberOfMovies++;
                	totalPredcitions++;
                	if (!(totalMovies.contains(mid))) totalMovies.add(mid);
                	getAndAddErrorLOO(0,averageRatingForActiveUser, uid, mid, 1); // assume 0 is returned
                }
                
                //let us write after 100 entires
                if(writeSentitivityData == true && totalRec==50)      
                { 
                	writeRocIntoFile(); 
                	totalRec=0;
                	rmse.resetROC();                	
                }                              
                totalRec++;                 
            } //end of all movies
    	   

    	   //____________________
    	   //Given5 Protocol
    	   //____________________
    	   	    
    	   breakIt =0;
    	   if (mySize==5) conditionalBreak = mySize-1;
	    	else conditionalBreak=5;

    	   averageRatingForActiveUser =0.0;
    	   
    	      while (true)    		 
    		   { 		  
  			   			 
  			   //generate a random number 
  			 		try  				{del = (int) rand.nextInt(mySize);  //select some random movies to delete (take their indexes) 
  			 		
  			 							}
  			 		catch (Exception no){ //System.out.println(" error in random numbers");
  	    			 					}
  			 		
  			 		tempM = moviesForActiveUser.get(del);
  			  		
  			 		if (!(Given5.contains(tempM))) { 
  			 			Given5.add(tempM);
  			 			breakIt++;
  			 			averageRatingForActiveUser+= MMh.getRating(uid, tempM);
  			 		}
  			      
  			
  			 		if(breakIt ==5) break;
    		   }//end of adding some random votes as observed votes
    	    
    	 	    averageRatingForActiveUser= averageRatingForActiveUser/conditionalBreak;
    	 	    
    	   for (int j = 0; j < mySize; j++)            
           {
       	       totalEntries++;
               mid = moviesForActiveUser.get(j);
               
               if(!(Given5.contains(mid)))
               {
            	   rating = f.recommendSLOO(uid, mid, kForThisDepth, Given5,averageRatingForActiveUser);         
               
            	   //add errors
            	   if(rating!=-10 && rating !=-1) {
            		   getAndAddErrorLOO(rating, averageRatingForActiveUser, uid, mid, 5); //filter movies}         
            	   									}
            		   if(rating ==-1) {
            		   getAndAddErrorLOO(0, averageRatingForActiveUser, uid, mid, 5); //filter movies}
            		   
            		   					}
                                
	               //let us write after 100 entires
	               if(writeSentitivityData == true && totalRec==50)      
	               { 
	               	writeRocIntoFile(); 
	               	totalRec=0;
	               	rmse.resetROC();                	
	               }                              
	              
               }
               
           } //end of all movies

  	   
	   
    	   //____________________
    	   //Given10 Protocol
    	   //____________________
    	   
   	    	breakIt=0;
   	    	if (mySize<=10) conditionalBreak = mySize-1;
   	    	else conditionalBreak=10;

   	    	averageRatingForActiveUser =0.0;
   	    	
    	   while (true)    		 
		   { 		 
			   			 
			   //generate a random number 
			 		try  				{del = (int) rand.nextInt(mySize);  //select some random movies to delete (take their indexes) 
			 		
			 							}
			 		catch (Exception no){ System.out.println(" error in random numbers");
	    			 					}
			 		
			 		tempM = moviesForActiveUser.get(del);
			  		
			 		if (!(Given10.contains(tempM))) { 
			 			Given10.add(tempM);
			 			breakIt++;
			 		
			 			averageRatingForActiveUser+= MMh.getRating(uid, tempM);
  			 		}
  			      
  			 	
  			 		
  			 	    //	System.out.println(" "+ breakIt);
			 		if(breakIt ==conditionalBreak) break;
		   }//end of adding some random votes as observed votes
    	    averageRatingForActiveUser= averageRatingForActiveUser/conditionalBreak;
		   
	   for (int j = 0; j < mySize; j++)            
       {
   	       totalEntries++;
           mid = moviesForActiveUser.get(j);
           
           if(!(Given10.contains(mid)))
           {
	           rating = f.recommendSLOO(uid, mid, kForThisDepth, Given10, averageRatingForActiveUser);         
	          //add errors
	           if(rating!=-10 && rating !=-1) { 
	        	   getAndAddErrorLOO(rating, averageRatingForActiveUser, uid, mid,10); //filter movies
	           									}
	           if(rating ==-1) { getAndAddErrorLOO(0, averageRatingForActiveUser, uid, mid, 10); //filter movies
	           					}
	        	                 
	           //let us write after 100 entires
	           if(writeSentitivityData == true && totalRec==50)      
	           { 
	           	writeRocIntoFile(); 
	           	totalRec=0;
	           	rmse.resetROC();                	
	           }                              
	           
           }
       } //end of all movies      	   
    
       

    	   
   } //end of if           
    
        
   
  } //end processing all users

       
        //Print results
        myT2 = System.currentTimeMillis();
                     
        System.out.println("===*************==");
        System.out.println("total users " + totalUsers); totalUsers=0;
        System.out.println("total movies " + totalNumberOfMovies); totalUsers=0; totalNumberOfMovies=0;
        System.out.println("===*************==");          
        System.out.println("Final Mae ALL But One: " 			+ rmse.mae());
        System.out.println("Final Mae Given 5: " 				+ rmseGiven5.mae());
        System.out.println("Final Mae Give10: " 				+ rmseGiven10.mae());
        System.out.println("Final Movie Avg Mae: " 		+ movrmse.mae());
        System.out.println("Final User  Avg  Mae: " 	+ usermse.mae());       
        System.out.println("Total time taken:----- "	+ ((myT2 - myT1)/(1000*60))      + " min.");
 
        if (writeRecData==true)    {writeRecToFile(0, kForThisDepth, (myT2-myT1),
        											rmse.mae(), rmseGiven10.mae(), rmseGiven5.mae(),
        											usermse.mae(), usermseGiven10.mae(), usermseGiven5.mae(),
        											movrmse.mae(),movrmseGiven10.mae(), movrmseGiven5.mae() );
        							}
        
        // Here, we can re-set values in the class RMSE
    /*    rmse.resetValues();
        rmseGiven5.resetValues();
        rmseGiven10.resetValues();
        movrmse.resetValues();
        movrmseGiven5.resetValues();
        movrmseGiven10.resetValues();
        usermse.resetValues();
        usermseGiven5.resetValues();
        usermseGiven10.resetValues();
      */  
        }//end of algorithm for
    
      // write neighbourhood size and time results in a file      
      if(writeSentitivityData ==true) closeRocFile();
      //
      
      
    
   }//end of outer neighbourhood size for

 System.out.println(" total Movies= " + totalMovies.size());
 System.out.println(" total pred= " + totalPredcitions);

 closeRecFile(1);
       
  }//end of LOO
 
  //___________________________________________________________________
  // K Fold
  //___________________________________________________________________
          
  if(whichRecommender.equalsIgnoreCase("KFold"))
  {
  		
           int subUid=0;
           LongArrayList IfLessThanTwo;
           if (writeRecData) openRecFile(5);					// how much files to open
           opneUserVsMovies();
           
   for (int neighbourLoop=300;neighbourLoop<=neighbourhoodSize; neighbourLoop+=neighbourhoodInc ) //step from let 5 untill all neighbours
    {
  	  	System.out.println (" Rec from neighbour size of " + neighbourLoop);  		
          
      for (int algorithm=0;algorithm<8;algorithm++) //0-7        
       {    	    
      	if (algorithm >=1) continue;
      	
       	  myT1 = System.currentTimeMillis();        
          users = MMh.getListOfUsers(); 			//from CusToMovie.key
          //System.out.println("all users = "+ users.size());
          //___________________________________________
          System.gc();
          DK.divideIntoKFold(users,MMh, 5, TestTrainMMh, myPath+ "\\tempFiles\\");

			 for (int k=0;k<5;k++) // 5 fold test for this subset            
			 {           	
				 
				 IntArrayList testUsers = TestTrainMMh[k].getListOfUsers(); //test set	    	
				 FTFilterAndWeight f = new FTFilterAndWeight(TestTrainMMh[k+1], givenOption[algorithm]); //with mmh object	
				 
				 for (int t=0;t<testUsers.size();t++) // size of each fold test set
				 {
					 subUid = testUsers.getQuick(t);
					 movies = TestTrainMMh[k].getMoviesSeenByUser(subUid);	 //get movies seen by this user

				  	//______________________________________________________
					    //All movies seen by this user	
					 for (int j = 0; j < movies.size(); j++)            
					 {
						 
						 mid  = FTMemHelper.parseUserOrMovie(movies.getQuick(j));
						 IfLessThanTwo = MMh.getUsersWhoSawMovie(mid);		//this should also be done in (total set - test set)		       
					         
						 if (IfLessThanTwo.size() >=5) //filter movies
						 {
							 totalEntries++;						 
							 rating = f.recommendSK(subUid, mid, neighbourLoop);         
							 if (rating!=-10 && rating!=-1) getAndAddErrorK(rating, subUid, mid,k);             
						 } //end of filtering movies
						 
					 } //end of movies for
				    }//end of all test users against a fold
							 
			     
          //Print results
          myT2 = System.currentTimeMillis();
          
          
        RMSEUAvg [algorithm]+=usermse.rmse();
      	RMSEMAvg [algorithm]+=movrmse.rmse();
      	RMSEPre [algorithm]+=rmse.rmse();    	
      	MAEUAvg [algorithm]+=usermse.mae();
      	MAEMAvg [algorithm]+=movrmse.mae();
      	MAEPre [algorithm]+=rmse.mae();
      	   	
          
          System.out.println("===*************==");
          System.out.println("Entries = " + totalEntries); totalEntries=0;
          System.out.println("===*************==");          
          System.out.println("Final Mae: " 				+ rmse.mae());
          System.out.println("Final Movie Avg Mae: " 		+ movrmse.mae());
          System.out.println("Final User  Avg  Mae: " 	+ usermse.mae());       
          System.out.println("Total time taken:----- "	+ ((myT2 - myT1)/1000*60)      + " min.");

                  
          
          
          if (writeRecData==true)    {writeRecToFile(k, neighbourLoop, (myT2-myT1),
          											    rmse.mae(), rmseGiven10.mae(), rmseGiven5.mae(),
          											    usermse.mae(),usermse.mae(),usermse.mae(),
          											    movrmse.mae(),movrmse.mae(),movrmse.mae() );}
          
          // Here, we can re-set values in the class RMSE
          rmse.resetValues();
          movrmse.resetValues();
          usermse.resetValues();
          
			 }//end of k fold
			 
          }//end of algorithm for
      
        // write neighbourhood size and time results in a file      
        if(writeSentitivityData ==true) closeRocFile();
        System.out.println(" neighours=" + neighbourLoop );
        
     }//end of outer neighbourhood size for
  closeRecFile(5);
  
  }
        
    closeUserVsMovies();   
 	System.out.println(" done with all");
 	
         
}//end of function
	
/****************************************************************************************************/

	public void getAndAddErrorK(double rating, int ud, int md, int k)	
	{
		
      double actual;  
	  actual = TestTrainMMh[k].getRating(ud, md);						//get actual rating against these uid and movieids
      if (actual <0) System.out.println(" error while adding");
	  movPrediction = MMh.getAverageRatingForMovie(md);
      userPrediction = MMh.getAverageRatingForUser(ud);
     
           
      rmse.add	(actual, rating);				//add (actual rating, Predicted rating)
      movrmse.add(actual, movPrediction);		//add (actual rating, Movie_Avg)
      usermse.add(actual, userPrediction);		//add (actual rating, User_Avg)
      

	}

/****************************************************************************************************/
	
	public void getAndAddErrorLOO(double rating, double userAverage, int ud, int md, int whichProtocol)	
	{
		
      double actual;  
	  actual = MMh.getRating(ud, md);						//get actual rating against these uid and movieids
      
	  if (whichProtocol==1)
	  {
		  //for LOO, not the active user
		  movPrediction = MMh.getAverageRatingForMovie(md);      
	     int howManyUserRatedIT =MMh.getUsersWhoSawMovie(md).size();      
	     movPrediction = ((movPrediction *  howManyUserRatedIT) - rating)/ (howManyUserRatedIT-1); //just exclude tha cative user for this 
	      
	      userPrediction = userAverage;
	           
	      rmse.add	(actual, rating);				//add (actual rating, Predicted rating)
	      movrmse.add(actual, movPrediction);		//add (actual rating, Movie_Avg)
	      usermse.add(actual, userPrediction);		//add (actual rating, User_Avg)
	   }
	  
	  else if (whichProtocol==5)
	  {
		  //for LOO, not the active user
		  movPrediction = MMh.getAverageRatingForMovie(md);      
	      int howManyUserRatedIT =MMh.getUsersWhoSawMovie(md).size();      
	      movPrediction = ((movPrediction *  howManyUserRatedIT) - rating)/ (howManyUserRatedIT-1); //just exclude tha cative user for this 
	      
	      userPrediction = userAverage;	           
	      rmseGiven5.add	(actual, rating);				//add (actual rating, Predicted rating)
	      movrmseGiven5.add(actual, movPrediction);		    //add (actual rating, Movie_Avg)
	      usermseGiven5.add(actual, userPrediction);		//add (actual rating, User_Avg)
	   }

	  else //give10
	  {
		  //for LOO, not the active user
		  movPrediction = MMh.getAverageRatingForMovie(md);      
	      int howManyUserRatedIT =MMh.getUsersWhoSawMovie(md).size();      
	      movPrediction = ((movPrediction *  howManyUserRatedIT) - rating)/ (howManyUserRatedIT-1); //just exclude tha cative user for this 
	      
	      userPrediction = userAverage;	           
	      rmseGiven10.add	(actual, rating);				//add (actual rating, Predicted rating)
	      movrmseGiven10.add(actual, movPrediction);		//add (actual rating, Movie_Avg)
	      usermseGiven10.add(actual, userPrediction);		//add (actual rating, User_Avg)
	   }

	  
	}

/****************************************************************************************************/
	
 public void writeRocIntoFile() 
 {
	 // write FPR (first), Sensitivity (second) separated by comma
	 try 
	 
	 {
		// System.out.println(Double.toString(rmse.getFalsePositiveRate()) + "  " +
		 //			Double.toString(rmse.getSensitivity()));
		 
		 		 
	 //	 roc4Writer.write(  nf.format(rmse.getFalsePositiveRate()) + "\t\t" +
	 //			 			nf.format(rmse.getSensitivity()));
	 	
		 roc4Writer.write( (rmse.getFalsePositiveRate()) + "\t\t\t\t" +
				 			(rmse.getSensitivity()));
				 	
		 roc4Writer.newLine();
	 	 
	  }
	
	 catch (IOException e) 
	 
	 {
          System.out.println("Write error!  Java error: " + e);
          System.exit(1);
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
			 recWriterNeighbourAndTime [t] = new BufferedWriter(new FileWriter(fileToWriteRecResults + (t+1) + ".dat", true));
			// recWriterRough = new BufferedWriter(new FileWriter(myPath + "RoughResults.dat", true));
		   }
		   
	   }
     
     catch (Exception E)
     {
   	  System.out.println("error opening the file pointer of rec");
   	  System.exit(1);
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
 
	 try {
		 	//recWriterRough.close();
		 
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

public void closeUserVsMovies() 
{

	 try {
		 	userVsMoviesWriter.close();}
	     
    catch (Exception E)
    {
  	  System.out.println("error closing the roc file pointer");
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
								double allMaeP, double g10MaeP, double g5MaeP,
								double allMaeU, double g10MaeU, double g5MaeU,
								double allMaeM, double g10MaeM, double g5MaeM
								)
	
	{	
		try 		//write Number of Users, Time taken, RMSE, MSE 
		
		{
			
			recWriterNeighbourAndTime[whichFile].write 
			(   (nSize) +
				// "\t" + myTimeTaken +
				"\t" + nf.format(allMaeP)+ 
				"\t" + nf.format(g10MaeP)+
				"\t" + nf.format(g5MaeP) + 
				"\t" + nf.format(allMaeU)+ 
		//		"\t" + nf.format(g10MaeU)+
		//		"\t" + nf.format(g5MaeU) + 
				"\t" + nf.format(allMaeM)
		//		"\t" + nf.format(g10MaeM)+
		//		"\t" + nf.format(g5MaeM)
				 											
			);
			
			recWriterNeighbourAndTime[whichFile].newLine();
		}

	catch (Exception e)
	{
		System.out.println (" Error while writin rec neighbour etc data into a file" + e);		
	}
	
	//	System.out.println (" Success while writin rec neighbour etc data into a file");
		
		
	}
	
//------------------------
	
	public void opneUserVsMovies()
	{

		 try {
			 
			 userVsMoviesWriter = new BufferedWriter(new FileWriter(myPath + "user.dat"));	 
				 
		   }
	     
	     catch (Exception E)
	     {
	   	  System.out.println("error opening the file pointer");
	     }
		
	}
	
//------------------------
	
	public void writeuserToFile(int uid, int mid)
	  {	
		
		try 		//write Number of Users, Time taken, RMSE, MSE		
		{
		
		 userVsMoviesWriter.write(uid + "\t" + mid);
		 userVsMoviesWriter.newLine();
		}
		
		catch (Exception e)
		{
		System.out.println (" Error while writin rec neighbour etc data into a file" + e);		
		}
		
		//	System.out.println (" Success while writin rec neighbour etc data into a file");


}


	

	
	
}
