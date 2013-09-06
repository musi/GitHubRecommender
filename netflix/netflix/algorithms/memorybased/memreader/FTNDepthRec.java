package netflix.algorithms.memorybased.memreader;

import java.io.BufferedWriter;
import java.io.File;
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
import netflix.FtMemreader.DivideDataIntoKFolds;
import netflix.FtMemreader.UserKFoldAndWrite;

/**
 * @author Musi
 *
 */

/************************************************************************************************************************/
public class FTNDepthRec

{
    private int    givenN;					// Only n values are given for a user in a training set (only n are given)
    private int    allButOne;				// All values except one is given for a user (only one is not there)
    private int	   givenOption [];
    private int    numberOfSample;	
    private Random 			rand;
    
    //for N depth, divide a subset in a specific layer into K test and K trains set    
    private OpenIntObjectHashMap[] kTestSet;
    private OpenIntObjectHashMap[] kTrainSet;
    
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
    FTNDepthData    FTData;
    DivideAndWriteIntoKFolds DK;
    UserKFoldAndWrite UK;
    RatingKFoldAndWrite RK;
    //want to write Data into a file?
    boolean 		writeSentitivityData;
    boolean 		writeRecData;
    BufferedWriter  roc4Writer;
    BufferedWriter  recWriterRough;
    BufferedWriter  recWriterNeighbourAndTime[];    
    NumberFormat    nf, nf1;
    
    
  //____________________________
    
    public FTNDepthRec( )
    {
    	
    	
    }
    
  /**********************************************************************************************/
    
    public FTNDepthRec(   String trainObject, 
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
    	givenN				=2;				 // can be anything
    	givenOption			= new int[8]; 	// coorelation
    	numberOfSample      =1;  			// 2 for one set
    	
    	RMSEUAvg 			= new double [10];
    	RMSEMAvg 			= new double [10];
    	RMSEPre 			= new double [10]; 	//these will be used to write results into a file     	
    	MAEUAvg 			= new double [10];
    	MAEMAvg 			= new double [10];
    	MAEPre	 			= new double [10];
    	totalEntries		=0;
    
    	
    	
        finalCorrError 		= 0.0;
        finalUserAvgError 	= 0.0;
        finalMovAvgError 	= 0.0;
        corrPrediction 		= 0.0;
        userPrediction 		= 0.0;
        movPrediction 		= 0.0;
        totalRec			= 0;
        
        writeSentitivityData = roc; 							//for writing roc4 related data make it true
        writeRecData 		 = rec; 							//for writing rec related data make it true
        recWriterNeighbourAndTime = new BufferedWriter[15];    	//Write 8 algorithms results
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
      
        
        
    	 nf = new DecimalFormat("#.######");		//upto 5 digits
    	 nf1 = new DecimalFormat("#.#########");	//upto 8 digits
       	 simChoices  = new String [8];
    	
    	//make objects
    	TTS 		= new TrainingTestSet();    //just to get files of training sets
		Ds  		= new DivideIntoSets();
		DK          = new DivideAndWriteIntoKFolds();
		UK			= new UserKFoldAndWrite();
		RK			= new RatingKFoldAndWrite();
		
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
    
    	kTestSet  = new OpenIntObjectHashMap[5];
    	kTrainSet = new OpenIntObjectHashMap[5];
    	rand 		= new Random();
    	
        for(int i=0;i<5;i++)
        {
        	kTestSet[i] = new OpenIntObjectHashMap();
        	kTrainSet[i] = new OpenIntObjectHashMap();
        }
        myT2 = myT1 = 0;
        
    }
    
     
/**********************************************************************************************/
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
		

	    FTNDepthRec mr= new FTNDepthRec(				//call full constructore
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
                     
        System.out.println("Fianl Total time taken: " 		+ (((endTime - startTime)*1.0)/(1000*60))      + " mims.");
        
           
    }//end of main


	
/**********************************************************************************************/
/**********************************************************************************************/
	
  public void makePrediction()	
   {
		//System.out.println("Come to make prediction");
		
		//first load data
		FTData.LoadNDepthData();
				
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
        int 			mid=0, uid=0, tempUser, subUid;        
        double 			rating=0;
        int 			kForThisDepth=0;
         

        int del=0;
	    int tempM;
	    int breakIt=0;
	    int conditionalBreak=0;
	    int moviesSize;
	    int fixedK=0;        
      //_____________________________________________________________________________________________
      //Leave one out, so loop through all the users in this depth   
      //_____________________________________________________________________________________________
 
    if(whichRecommender.equalsIgnoreCase("LOO"))
    {

    	double averageRatingForActiveUser=0.0;
    	IntArrayList totalMovies = new IntArrayList();
    	int totalPredcitions =0;
    	int possiblePredcitions =0;
    	int totalUsers =0;
    	
		if (writeRecData) openRecFile(1);					// how much files to open
		
for (int depth =1; depth <=13;depth++, fixedK+=50)
  {  
	
//	   kForThisDepth = fixedK;
	  
	  kForThisDepth = FTData.getKForDepth(depth);
	  System.out.println(" curently in depth ="+ depth + ", k ="+kForThisDepth);
	  
   for (int algorithm=0;algorithm<8;algorithm++) //0-7        
     {    	    
    	if (algorithm >=1) continue;
    	
    	myT1 = System.currentTimeMillis();        
    	FTNDepthFilterAndWeight f = new FTNDepthFilterAndWeight(MMh, 1); //with mmh object	
        //users = MMh.getListOfUsers();
        IntArrayList roots = FTData.getUsersInAdepth(depth); 
      //  System.out.println(" user ------->" + roots.size());

            
      for(int i=0;i<roots.size();i++)
        {
    //	    System.out.println(" currently in depth "+ depth + ", active user "+ i+ " of total "+roots.size());
        	uid = roots.getQuick(i);        //these r the users which are active in a certain depth
            IntArrayList subset = FTData.getNeighboursInDepth(uid, depth);
            //subset.add(uid);
            //System.out.println(" subset= " + subset.size());    
              
       //______________________________________________________
         //All movies seen by this user

           	movies = MMh.getMoviesSeenByUser(uid);	 //get movies seen by this user        
           	moviesSize = movies.size();
           
           	if (moviesSize >=5) //for user who have rated more than 5 movies            	
                {
           		   totalUsers++;
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

                        //code to make sure that we are not keeping target movie into account when we are adding average rating of the acive user into prediction
                        averageRatingForActiveUser = MMh.getAverageRatingForUser(uid);
                        averageRatingForActiveUser = mySize * averageRatingForActiveUser;
                        double subtract = MMh.getRating(uid, mid);
                        averageRatingForActiveUser  = (averageRatingForActiveUser - subtract)/(mySize-1);
                        
                        rating = f.recommendSLOO(uid, mid, depth, subset, AllButOne,kForThisDepth, averageRatingForActiveUser);
                   
     					  if(rating!=-10 && rating!= -1) {  
                       	   getAndAddErrorLOO(rating,averageRatingForActiveUser, uid, mid,1); //filtered movies
                           if (!(totalMovies.contains(mid))) totalMovies.add(mid);
                           totalPredcitions++;
                    //       if(depth ==1) System.out.println(rating+", "+averageRatingForActiveUser + ", " + MMh.getRating(uid, mid));
                       					}
    
                        if(rating == -1) {  
                        	getAndAddErrorLOO(averageRatingForActiveUser,averageRatingForActiveUser, uid, mid,1); //filtered movies
                            if (!(totalMovies.contains(mid))) totalMovies.add(mid);
                      //    System.out.println(" rating =" + rating);
                            totalPredcitions++;
                        					}
                                  
                            possiblePredcitions++;
                    } //end of all movies
            	   


            	   //____________________
            	   //Given5 Protocol
            	   //____________________
            	   	    
            	   breakIt =0;
            	   if (mySize==5) conditionalBreak = mySize-1;
          	    	else conditionalBreak=5;
            	   
          	    	double given5Average=0.0;
          	    	averageRatingForActiveUser =0.0; //now we are only taking 5 movies into account
          	    	
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
          			      
          			 		if(breakIt ==conditionalBreak) break;
            		   }//end of adding some random votes as observed votes
            	    
            	                     
            	      averageRatingForActiveUser= averageRatingForActiveUser/conditionalBreak;
            	      
            	   for (int j = 0; j < mySize; j++)            
                   {
               	       totalEntries++;
                       mid = moviesForActiveUser.get(j);
                       
                       if(!(Given5.contains(mid)))
                       {
                           rating = f.recommendSLOO(uid, mid, depth, subset, Given5,kForThisDepth,averageRatingForActiveUser);         
                       
                    	   //add errors
                    	   if(rating!=-10 && rating !=-1) 
                    		   getAndAddErrorLOO(rating,averageRatingForActiveUser, uid, mid,5); //filtered movies         
                    	   if(rating ==-1) 
                    		   getAndAddErrorLOO(0,averageRatingForActiveUser, uid, mid,5); //filtered movies
                    	        
                    	   
                       }
                       
                   } //end of all movies
           	   

            	   //____________________
            	   //Given10 Protocol
            	   //____________________
            	   
            	   
           	    	breakIt=0;
           	    	if (mySize<=11) conditionalBreak = mySize-1;
           	    	else conditionalBreak=10;
           	    	averageRatingForActiveUser =0.0; 		//now average of 10           	    	

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
                       rating = f.recommendSLOO(uid, mid, depth, subset, Given10,kForThisDepth, averageRatingForActiveUser);
        	          //add errors
        	           if(rating!=-10 && rating !=-1) 
        	        	   getAndAddErrorLOO(rating,averageRatingForActiveUser, uid, mid,10); //filtered movies         
        	           if(rating ==-1)
        	        	   getAndAddErrorLOO(0,averageRatingForActiveUser, uid, mid,10); //filtered movies
        	        	                 
        	           //let us write after 100 entires
        	           
                   }
               } //end of all movies      	   
            
   


                
                
                }//EDD of movie filtering
           	
      }//end of one subset
     	
     

   System.out.println("Total rec in this depth = " +totalEntries);
   totalEntries=0;
   
        //Print results
        myT2 = System.currentTimeMillis();
        
        
        RMSEUAvg [algorithm]+=usermse.rmse();
    	RMSEMAvg [algorithm]+=movrmse.rmse();
    	RMSEPre [algorithm]+=rmse.rmse();    	
    	MAEUAvg [algorithm]+=usermse.mae();
    	MAEMAvg [algorithm]+=movrmse.mae();
    	MAEPre [algorithm]+=rmse.mae();
    	   	
     
    	  System.out.println("===*************==");
          System.out.println("totalusers = " + totalUsers); totalUsers=0;
          System.out.println("===*************==");          
          System.out.println("Final Mae ALL But One: " 			+ rmse.mae());
          System.out.println("Final Mae Given 5: " 				+ rmseGiven5.mae());
          System.out.println("Final Mae Give10: " 				+ rmseGiven10.mae());
          System.out.println("Final Movie all Avg Mae: " 		+ movrmse.mae());
          System.out.println("Final Movie  given 10 Avg Mae: " 	+ movrmseGiven10.mae());
          System.out.println("Final Movie given 5Avg Mae: " 	+ movrmseGiven5.mae());
          System.out.println("Final User  all Avg  Mae: " 		+ usermse.mae());        
          System.out.println("Final User  given10 Avg  Mae: " 	+ usermseGiven10.mae());          
          System.out.println("Final User  given 5Avg  Mae: " 	+ usermseGiven10.mae()); 
          
          
          System.out.println("Total time taken:----- "	+ ((myT2 - myT1)/(1000*60))      + " min.");

                  

          if (writeRecData==true)    {writeRecToFile(0, kForThisDepth, (myT2-myT1),
										rmse.mae(), rmseGiven10.mae(), rmseGiven5.mae(),
										usermse.mae(), usermseGiven10.mae(), usermseGiven5.mae(),
										movrmse.mae(),movrmseGiven10.mae(), movrmseGiven5.mae() );
	          						}

        //let us write after 100 entires
        if(writeSentitivityData == true )      
        { 
        	writeRocIntoFile(); 
        	rmse.resetROC();
        	rmseGiven5.resetROC();   
        	rmseGiven10.resetROC();   
        }                              
        
        totalRec++;
        // Here, we can re-set values in the class RMSE
        rmse.resetValues();
        rmseGiven5.resetValues();
        rmseGiven10.resetValues();
        movrmse.resetValues();
        usermse.resetValues();        
        
        }//end of which algorithm for
    
      // write neighbourhood size and time results in a file      
              
   }//end of outer for .... depth for
    if(writeSentitivityData ==true) closeRocFile();
    System.out.println(" total Movies= " + totalMovies.size());
    System.out.println(" total pred= " + totalPredcitions);
    System.out.println(" total pred= " + possiblePredcitions);
    
	closeRecFile(1);    
   }//LOO


//_____________________________________________________________________________________________
// UK folds   
//_____________________________________________________________________________________________

 if(whichRecommender.equalsIgnoreCase("UKFold"))   
 {
	 
for (int depth =1; depth <=13;depth++)
 {  	
	 kForThisDepth = FTData.getKForDepth(depth);
	 System.out.println(" curently in depth ="+ depth);

	 for (int algorithm=0;algorithm<8;algorithm++) //0-7        
	 {    	    
		 if (algorithm >=1) continue;
	
		 myT1 = System.currentTimeMillis();		 
		 //users = MMh.getListOfUsers();
		 IntArrayList roots = FTData.getUsersInAdepth(depth); 
		
		//______________________________________________________
	      //Now this is the dataset ovr which we can perform the K-FOld Cross validation 
	    
		  
		   UK.divideIntoKFold(roots,MMh, 5, TestTrainMMh, myPath+ "\\tempFiles\\");
		  System.out.println(" done folding");
		  
			 for (int k=0;k<5;k++) // 5 fold test for this subset            
			 {           	
			//	 if(k>0) continue;
			     
				 System.gc();
				 IntArrayList testUsers = TestTrainMMh[k].getListOfUsers();	    	
				 FTNDepthFilterAndWeight f = new FTNDepthFilterAndWeight(TestTrainMMh[k+1], givenOption[algorithm]); //with mmh object	
				 System.out.println(" test users = "+ testUsers.size());
				 
				 for (int t=0;t<testUsers.size();t++) // size of each fold test set
				 {
					    subUid = testUsers.getQuick(t);
			            IntArrayList subset = FTData.getNeighboursInDepth(subUid, depth);
			        
					 
					 movies = MMh.getMoviesSeenByUser(subUid);	 //get movies seen by this user
					 moviesSize = movies.size();
					if(subUid==980) System.out.println(" uid = "+ subUid+ ", movies size = "+ moviesSize);
					 
					 if(moviesSize >=5) //filter user which have rated more than 5 movies					 
					 {	
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
		            	   System.out.println(" yes >5");
		            	 //____________________
		            	   //All But One Protocol
		            	   //____________________
		            	   
		            	   for (int j = 0; j < mySize; j++)
		            	   {
		                        mid = moviesForActiveUser.get(j);
		                                 
		                        rating = f.recommendSKU(subUid, mid, depth, subset, TestTrainMMh[k], AllButOne,kForThisDepth);
		                        if(rating!=-10 && rating!= -1) getAndAddErrorK(rating, uid, mid, k); //filtered movies
		                    	if(rating== -1)					getAndAddErrorK(0, uid, mid, k);    // we can not predict, so send 0
		                    	                     
		                    } //end of all movies
		            	   

			 				 System.out.println(" Done with fold="+ (k+1) +  " at leyer ="+ depth);
	
		   //____________________
		   //Given5 Protocol
		   //____________________
		   	    
		   breakIt =0;
		   if (mySize==5) conditionalBreak = mySize-1;
	    	else conditionalBreak=5;
	    	    
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
			 		}
			      
			 		if(breakIt ==conditionalBreak) break;
			   }//end of adding some random votes as observed votes
		    
		             	    	   
		 for (int j = 0; j < mySize; j++)            
	     {
	 	       totalEntries++;
	         mid = moviesForActiveUser.get(j);
	         
	         if(!(Given5.contains(mid)))
	         {
	        //     rating = f.recommendSKU(subUid, mid, depth, subset, testUsers, Given5,kForThisDepth);         
	         
	      	   //add errors
	      	   if(rating!=-10 && rating !=-1) { rmseGiven5.add(MMh.getRating(uid, mid), rating);}         
	      	   if(rating ==-1) {rmseGiven5.add(MMh.getRating(uid, mid), 0);}
	      	       	   
	         }
	         
	     } //end of all movies (given 10)
	  }//end of condition (if movies>5)
	
	}//end of one fold
		 
				 
				 
				//Print results
				  myT2 = System.currentTimeMillis();
				  
			if (writeRecData==true)    {writeRecToFile(k, kForThisDepth, (myT2-myT1),
									rmse.mae(), rmseGiven10.mae(), rmseGiven5.mae(),
									usermse.mae(),usermse.mae(),usermse.mae(),
									movrmse.mae(),movrmse.mae(),movrmse.mae() );}


			  
				 System.out.println("===*************==");
		          System.out.println("Entries = " + totalEntries); totalEntries=0;
		          System.out.println("===*************==");          
		          System.out.println("Final Mae ALL But One: " 			+ rmse.mae());
		          System.out.println("Final Mae Given 5: " 				+ rmseGiven5.mae());
		          System.out.println("Final Mae Give10: " 				+ rmseGiven10.mae());
		          System.out.println("Final Movie Avg Mae: " 		+ movrmse.mae());
		          System.out.println("Final User  Avg  Mae: " 	+ usermse.mae());       
		          System.out.println("Total time taken:----- "	+ ((myT2 - myT1)/(1000*60))      + " min.");

		          
			  // Here, we can re-set values in the class RMSE
			  rmse.resetValues();
			  rmseGiven10.resetValues();
			  rmseGiven5.resetValues();
			  movrmse.resetValues();
			  usermse.resetValues();
			  
			
			  
			 } //end of all users
  
  }//end of which algorithm for

	 // write neighbourhood size and time results in a file      
	 if(writeSentitivityData ==true) closeRocFile();
	 System.out.println("depth preicted is -->" + depth);
	 //closeRecFile(depth);      

 }//end of outer for .... depth for

    closeRecFile(0);
 	System.out.println(" done with all");
   
 }//end of KFold If

 

//_____________________________________________________________________________________________
//RK folds   
//_____________________________________________________________________________________________

	if(whichRecommender.equalsIgnoreCase("RKFold"))   
	{
		if (writeRecData) openRecFile(5);					// how much files to open
		
	for (int depth =1; depth <=13;depth++)
	{  	
		 kForThisDepth = FTData.getKForDepth(depth);
		 System.out.println(" curently in depth ="+ depth);
	
		 for (int algorithm=0;algorithm<8;algorithm++) //0-7        
		 {    	    
			 if (algorithm >=1) continue;
		
			 myT1 = System.currentTimeMillis();		 
			 
			 IntArrayList roots = FTData.getUsersInAdepth(depth); 
			
			//______________________________________________________
		      //Now this is the dataset over which we can perform the K-FOld Cross validation 
		    
			  
			   DK.divideIntoKFold(roots,MMh, 5, TestTrainMMh, myPath+ "\\tempFiles\\");
			//   System.out.println(" done folding");
			  
				 for (int k=0;k<5;k++) // 5 fold test for this subset            
				 {           	
				//	 if(k>0) continue;
			     
					 //System.gc();
					 IntArrayList testUsers = TestTrainMMh[k].getListOfUsers();	    	
					 FTNDepthFilterAndWeight f = new FTNDepthFilterAndWeight(TestTrainMMh[k+1], givenOption[algorithm]); //with mmh object	
				//	 System.out.println(" test users = "+ testUsers.size());
					 
					 for (int t=0;t<testUsers.size();t++) // size of each fold test set
					 {
						    subUid = testUsers.getQuick(t);
				            IntArrayList subset = FTData.getNeighboursInDepth(subUid, depth);
				        
						 
						 movies = TestTrainMMh[k].getMoviesSeenByUser(subUid);	 //get movies seen by this user
						 moviesSize = movies.size();
						// System.out.println(" uid = "+ subUid+ ", movies size = "+ moviesSize);
						 
						 if(moviesSize >=5) //filter user which have rated more than 5 movies					 
						 {	
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
		                        rating = f.recommendSKR(subUid, mid,kForThisDepth, subset);
		                    //    if(subUid==0) {System.out.println("uid in rec =0"); System.exit(1);}
		                        if(rating!=-10 && rating!= -1)  getAndAddErrorK(rating, subUid, mid, k); //filtered movies
		                    	if(rating== -1)				    getAndAddErrorK(0, subUid, mid, k);    // we can not predict, so send 0
		                    	                     
		                    } //end of all movies
		            	   

			 			//	 System.out.println(" Done with fold="+ (k+1) +  " at leyer ="+ depth);
				}//end of condition (if movies>5)
			} //end of all test users in a fold
				 
				 
				//Print results
				  myT2 = System.currentTimeMillis();
				  
				  
					if (writeRecData==true)    {writeRecToFile(k, kForThisDepth, (myT2-myT1),
												rmse.mae(), rmseGiven10.mae(), rmseGiven5.mae(),
												usermse.mae(),usermse.mae(),usermse.mae(),
												movrmse.mae(),movrmse.mae(),movrmse.mae() );}	

			  
				 System.out.println("===*************==");
		          System.out.println("Entries = " + totalEntries); totalEntries=0;
		          System.out.println("===*************==");          
		          System.out.println("Final Mae ALL But One: " 			+ rmse.mae());
		          System.out.println("Final Mae Given 5: " 				+ rmseGiven5.mae());
		          System.out.println("Final Mae Give10: " 				+ rmseGiven10.mae());
		          System.out.println("Final Movie Avg Mae: " 		+ movrmse.mae());
		          System.out.println("Final User  Avg  Mae: " 	+ usermse.mae());       
		          System.out.println("Total time taken:----- "	+ ((myT2 - myT1)/(1000*60))      + " min.");

		          
			  // Here, we can re-set values in the class RMSE
			  rmse.resetValues();
			  rmseGiven10.resetValues();
			  rmseGiven5.resetValues();
			  movrmse.resetValues();
			  usermse.resetValues();
			  
			
			  
			 } //end of all folds
	 
	 }//end of which algorithm for
	
		 // write neighbourhood size and time results in a file      
		 if(writeSentitivityData ==true) closeRocFile();
		 System.out.println("depth preicted is -->" + depth);
		 //closeRecFile(depth);      
	
	}//end of outer for .... depth for
	
	    closeRecFile(5);
		System.out.println(" done with all");
	  
  }//end of KFold If
	        
 }//end of function

  
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
	      movrmseGiven5.add(actual, movPrediction);		//add (actual rating, Movie_Avg)
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
	
	public void getAndAddErrorK(double rating, int ud, int md, int k)	
	{
      double actual;  
	  actual = TestTrainMMh[k].getRating(ud, md);						//get actual rating against these uid and movieids
      movPrediction = MMh.getAverageRatingForMovie(md);
      userPrediction = MMh.getAverageRatingForUser(ud);
     
      rmse.add	(actual, rating);				//add (actual rating, Predicted rating)
      movrmse.add(actual, movPrediction);		//add (actual rating, Movie_Avg)
      usermse.add(actual, userPrediction);		//add (actual rating, User_Avg)      

	}



/****************************************************************************************************/
	
 public void writeRocIntoFile()
 
 {
	 // write FPR (first), Sensitivity (second) separated by comma
	 try 
	 
	 {
		// System.out.println(Double.toString(rmse.getFalsePositiveRate()) + "  " +
		 //			Double.toString(rmse.getSensitivity()));
		 
		 		 
	 	 roc4Writer.write(  nf1.format(rmse.getFalsePositiveRate()) + "\t\t" +
	 			 			nf1.format(rmse.getSensitivity())+ "\t\t" +
	 			 			
	 			 			nf1.format(rmseGiven10.getFalsePositiveRate()) + "\t\t" +
	 			 			nf1.format(rmseGiven10.getSensitivity()) + "\t\t" +
	 			 			
	 			 			nf1.format(rmseGiven5.getFalsePositiveRate()) + "\t\t" +
	 			 			nf1.format(rmseGiven5.getSensitivity())
	 			 			
	 	 					);
	 	
	//	 roc4Writer.write( (rmse.getFalsePositiveRate()) + "\t\t\t\t" +
	//			 			(rmse.getSensitivity()));
				 	
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
		 
			 roc4Writer = new BufferedWriter(new FileWriter(myPath + "\\Results1\\" + "roc4.dat"));
		 
			 
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

		   for (int t=0;t<nFiles; t++)  //for K fold as well
		   {
			  // File dir = new File("Depth" + (t+1));  
			  // dir.mkdir(); 
			 
			   recWriterNeighbourAndTime [t] = new BufferedWriter(new FileWriter(fileToWriteRecResults + "Fold" +(t+1) + ".dat", true));
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
						//				"\t" + nf.format(g10MaeU)+
						//				"\t" + nf.format(g5MaeU) + 
										"\t" + nf.format(allMaeM) 
						//				"\t" + nf.format(g10MaeM)+
						//				"\t" + nf.format(g5MaeM)
																					
										);

	recWriterNeighbourAndTime[whichFile].newLine();
 }
	catch (Exception e)
	{
		System.out.println (" Error while writin rec neighbour etc data into a file" + e);		
	}
	
	//	System.out.println (" Success while writin rec neighbour etc data into a file");
		
		
	}
	

	
/********************************************************************************************/
	

	
}
