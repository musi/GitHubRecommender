package netflix.recommender;

import java.io.*;
import java.util.*;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import cern.colt.list.LongArrayList;
import cern.colt.map.OpenIntDoubleHashMap;
import netflix.memreader.*;
import netflix.rmse.RMSECalculator;
import netflix.utilities.*;
import Jama.Matrix;
import Jama.SingularValueDecomposition;
/**
 * Recommends movies using the SVD-based technique 
 * described by Sarwar et. al. in "Application of 
 * Dimensionality Reduction in Recommender Systems -
 * A Case Study"
 *
 * Note that this class does not implement the resort() 
 * method in AbstractRecommender. Since the SVD is 
 * precomputed, new users cannot be added to the system.
 *
 * @author sowellb
 */

/****************************************************************************************************/
public class SVDRecommenderJama 
/****************************************************************************************************/
{
    private SingularValueDecomposition 	svd;
    private Matrix 						P;
    private Matrix 						left;
    private Matrix 						right;
    private int 						k;
    String  							myPath;
    int 								totalNegSVDPred;
    int 								totalPosSVDPred;
    int 								totalZeroSVDPred;
    AbstractRecommender					myAbstractRecommender;
    MemHelper   						mh;
    int									neigh;
    int 								totNans;
    
/****************************************************************************************************/
    
    /**
     * Constructor. 
     *
     * @param  memReaderFile  File containing serialized MemReader.
     * @param  svdFile  File containing serialized SVD.
     * @param  k  Number of singular values to use.
     */

 /*   public SVDRecommenderJama(String memReaderFile, String svdFile, int k) 
    {
    	  
    	  this(new MemHelper(memReaderFile), svdFile, k);    	
    }*/

    public SVDRecommenderJama()
    {
    	totNans = 0;
    }



/****************************************************************************************************/
    
    /**
     * Computes the recommendation matrix from the 
     * SVD. See the paper "Application of Dimensionality
     * Reduction in Recommender Systems - A Case Study"
     * for more information. 
     */

    private void buildModel(int k12, String svdFile) 
    {
    	
        try 
        {
            this.k  = k12;            

            //Read SVD
            FileInputStream fis  = new FileInputStream(svdFile);
            ObjectInputStream in = new ObjectInputStream(fis);
            svd 				 = (SingularValueDecomposition) in.readObject();
            totalNegSVDPred		 = 0;
            totalPosSVDPred		 = 0;
            totalZeroSVDPred	 = 0;            
            
         }
        catch(Exception e) {
            e.printStackTrace();
        }
        
        
            Matrix rootSk = svd.getS().getMatrix(0, k-1, 0, k-1);        
      //    System.out.print("singular values getS= ");
     //     rootSk.print(9, 6);
/*            
            System.out.print("singular values = ");
            Matrix svalues = new Matrix(svd.getSingularValues(), 1);
            svalues.print(9, 6);
*/            
            //compute singular value
            for(int i = 0; i < k; i++) 
            {
              rootSk.set(i,i,Math.sqrt(rootSk.get(i,i)));  //Sets the matrix cell at coordinate [row,column] to the specified value. 
            }

            // Compute U and V'
            Matrix U  = svd.getU();	
            Matrix Uk = U.getMatrix(0, U.getRowDimension()-1,0, k-1); // (int row, int column, int height, int width) 

            Matrix VPrime = svd.getV().transpose();
            Matrix VPrimek = VPrime.getMatrix(0, k-1, 0,VPrime.getColumnDimension()-1);
            Matrix rootSkPrime = rootSk.transpose();
            
            //compute left and right by multiplying US, and SV'           
            left  = Uk.times(rootSkPrime);           
            right = rootSk.times(VPrimek);

              
            // Multiply [(US)(SV')]
            P = left.times(right);                   
            
           // P.print(2, 3);
    }

/****************************************************************************************************/
    
    /**
     * Predicts the rating that activeUser will give targetMovie.
     *
     * @param  activeUser  The user.
     * @param  targetMovie  The movie.
     * @param  date  The date the rating was given. 
     * @return The rating we predict activeUser will give to targetMovie. 
     */
    public double recommend(int activeUser, int targetMovie, int neighbours) 
    {
    	double entry=0;
    	double prediction =0;
    	
       // if ( activeUser<943 && targetMovie <1682)    	
        {
        	prediction =  recommendItemBased(activeUser,targetMovie, neigh );
        	//prediction =  recommendUserBased(activeUser,targetMovie, neigh );
        		
        			
        	/*		entry = P.get(targetMovie, activeUser);     
        			double avg = mh.getAverageRatingForUser(activeUser);
        	        prediction = entry + avg;        	 
        	 */
        			
        	
        	 if(entry >0) totalPosSVDPred ++;
        	 if(entry == 0) totalZeroSVDPred ++;	  
        	 if(entry <0)
        	  {		
        		 	totalNegSVDPred ++;        		 	
        	  }
        	 
        	
        }	
        
        /*
        if(prediction < 1)
            return 1;
        else if(prediction > 5)
            return 5;
        else
            return prediction;
        */
        return prediction;
    }

    
  //-------------------------------------------------
    
 /**
  * MAIN
  * @param args
  */
    
    public static void main(String[] args) 
    {
    
    	SVDRecommenderJama svdRec 	= new SVDRecommenderJama();
    	svdRec.MakeRec();
    	 
    }
    
    //-------------------------------------------------
    
    public void MakeRec()
    {
        String path = "";   	
    	String test = "";
    	String base = "";
    	String svdFile = "";
    	
    	path ="C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\SVD\\FiveFoldData\\";   	
    	test = path + "sml_clusteringTestSetStoredTF.dat";
    	base = path + "sml_clusteringTrainSetStoredTF.dat";
    	svdFile = path + "SVDStoredJama1.dat";
    	
    	
    	    	
    	/* path  = "I:/Backup main data march 2010/workspace/MusiRecommender/DataSets/FT/Itembased/FiveFoldData/";    	
    	 base  = path + "ft_clusteringTrainSetStoredTF1.dat";
    	 test  = path + "ft_clusteringTestSetStoredTF1.dat";
    	 //svdFile = path + "SVDStoredJamaFT.dat";
    	 svdFile = path + "SVDStoredJamaUserAvgFT.dat";*/
    	 
    	 
        System.out.println("Training set: " + base + ", test set: " + test);       
       
        for(int i=8;i<40;i=i+2)
        {        
	        
	        MemHelper mhTest 			= new MemHelper(test);
	        MemHelper mhTrain 			= new MemHelper(base);
	        buildModel(i,svdFile);
	        
	        mh = mhTrain;
	        
	        for(int j=5;j<50;j=j+10)
	        {
	        	neigh = j;
		        System.out.println(" K=" + i+", neigh="+neigh);
		        testWithMemHelper(mhTest, mhTrain, 10, 20);
		        System.out.println(" totNans ="+totNans);
		        totNans = 0;
	        }	
	        
	
	        System.out.println("Total SVD pred <0 = " + totalNegSVDPred);
	        System.out.println("Total SVD pred >0 = " + totalPosSVDPred);
        }
        
        
        
        //-----------------------------
        // 5-FOLD
        //-----------------------------
          
         
      	/*path ="C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\SVD\\FiveFoldData\\";  
                 
      	for (int fold =1; fold<=5;fold++)
      	{
  	    	svdFile = path +  "SVDStoredJama" + (fold)+ ".dat";
  	    	base 	= path +  "sml_trainSetStoredFold" + (fold)+ ".dat";
  	    	test	= path +  "sml_testSetStoredFold" + (fold)+ ".dat";
  	    	
  	        for(int i=5;i<40;i++)
  	        {
  		        SVDRecommenderJama svdRec = new SVDRecommenderJama(base, svdFile, i);
  		        MemHelper mhTest = new MemHelper(test);
  		        System.out.println("FOld: "+ fold + ", k: "+ i +", MAE= " + svdRec.testWithMemHelper(mhTest, 25));
  		 
  	        }
  	      System.out.println("--------------------------------------------------");
  	      
      	}*/
      	
     }

	//------------------------------------------------------------------
	
	public void testWithMemHelper(MemHelper testmh, MemHelper trainMMh, 
			  int myClasses,	int neighbours)     
    {
			
			RMSECalculator rmse = new  RMSECalculator();
			
			IntArrayList users;
			LongArrayList movies;
			//IntArrayList coldUsers = coldUsersMMh.getListOfUsers();
			//IntArrayList coldItems = coldItemsMMh.getListOfMovies();
			
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
			
			//  if(coldItems.contains(mid))
			 {
			//     if (mid ==-1)  System.out.println(" rating error--> uid, mid -->" + uid + "," + mid );
			 
			// double rrr = recommend(uid, mid, blank);                
			 double rrr = recommend(uid, mid, neighbours);
			 
			 /*//Add values to Pair-t
			 if(ImputationMethod ==2)
			 	rmse.addActualToPairT(rrr);
			 else
			 	rmse.addPredToPairT(rrr);
			 */
			 
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
			             if(rrr!=0)
			             	rmse.ROC4(myRating, rrr, myClasses, trainMMh.getAverageRatingForUser(uid));		
			             	//rmse.ROC4(myRating, rrr, myClasses, TopNThreshold);
			 		                          
			             //-------------
			             //Add Error
			             //-------------
			            
			             if(rrr!=0)
			             {
			             	rmse.add(myRating,rrr);                            	
			             	                            	                                
			             }		
			
			             //-------------
			             //Add Coverage
			             //-------------
			
			              rmse.addCoverage(rrr);                                 
			 		  }         
			 }
			} //end of movies for
			
			//--------------------------------------------------------
			//A user has ended, now, add ROC and reset
			rmse.addROCForOneUser();
			rmse.resetROCForEachUser();
			rmse.addMAEOfEachUserInFinalMAE();
			rmse.resetMAEForEachUser();
			
			    
			}
			
			System.out.println(" MAE: " + rmse.mae());
	        System.out.println(" ROC: " + rmse.getSensitivity());	        
	        
	        	//Reset final values
			rmse.resetValues();   
			rmse.resetFinalROC();
			rmse.resetFinalMAE();
			/*if(ImputationMethod >2)
			rmse.resetPairTPrediction();*/	
			
	        
  }//end of function

	
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
	                    	
	         	//All movies, as in case of SVD, a user have rated all movies (100% coverage)
	             IntArrayList movies = mh.getListOfMovies();
	             totalMoviesSeenByActiveUser = movies.size();    	
	         	
	         	activeUserAverage = mh.getAverageRatingForUser(activeUser);  //active user average
	         	
	         	OpenIntDoubleHashMap itemIdToWeight = new OpenIntDoubleHashMap();
	     	    IntArrayList myItems      			= new IntArrayList();
	     	    DoubleArrayList myWeights 			= new DoubleArrayList();
	     	    double currentWeight;
	     	    
	     	    //GO through all the movies seen by active user and find similar items
	         	for (int m =0; m<totalMoviesSeenByActiveUser; m++)
	         	{    		
	         		 mid = (movies.getQuick(m));  
	         	     currentWeight  = findItemSimilarity_SVD(targetMovie, mid, activeUser); //active item, item we want to find sim with
	         	   	 itemIdToWeight.put(mid, currentWeight);
	         	 }
	         	
	       /* 
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
	    	 }*/
	    	
	         	
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
	       			    activeUserRatingOnSimItem = P.get(mid-1,activeUser-1);             
		             	voteSum += (currentWeight * (activeUserRatingOnSimItem));		             		
		             	myTotal++;        	

	         			//weight+1
	         /*			currentWeight = myWeights.get(i) + 1;
		             	weightSum += Math.abs(currentWeight);				// see no Math.abs(), for adjusted cosine I think there is no need (weight >0 always)
		             	activeUserRatingOnSimItem = P.get(mid-1,activeUser-1);           	
		             	if(!(modelToBuild.equalsIgnoreCase("Simple")))
		             		voteSum += (currentWeight * (activeUserRatingOnSimItem + activeUserAverage));
		             	else
		             		voteSum += (currentWeight * (activeUserRatingOnSimItem ));	             		
		             	 myTotal++;
	         			*/
	             	}
	         		
	          } //end of for
	       
	         //predict and send back         
	         
	        	 				voteSum *= (1.0 / weightSum);
	        	 					answer = voteSum + activeUserAverage; 	
		       	 				      	 					
	        	 				if(answer==Double.NaN){
	        	 					answer = activeUserAverage;
	        	 					totNans++;
	        	 					}
	        	 				
	        	 					
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
	            	double activeUserAvg = mh.getAverageRatingForUser(activeUser);
	            	
	                // for all the common users
	                for (int i = 0; i < k; i++)
	                {            
	                      // get their ratings from "SVD LEFT (SV) MATRIX"               
	                	//if(activeItem<1682 && myItem<1682)
	                	{
	                		//SML	                		
	    	            	rating1 = left.get(activeItem-1,i);                
	    		            rating2 = left.get(myItem-1,i);               
	                	
	                	   	topSum += rating1 * rating2;		            
	    		            bottomSumActive += Math.pow(rating1, 2);
	    		            bottomSumTarget += Math.pow(rating2, 2);
	    	            	                	
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
	            // User-based CF  
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
	                                
	              /*          
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
	                    	 }*/
	                    	
	                    	
	                        //For all 100% coverage
	                    	//All movies, as in case of SVD, a user have rated all movies (100% coverage)
	                        IntArrayList users = mh.getListOfUsers();
	                        totalUsersWhoSawTargetMovie = users.size();	   
	                    	activeUserAverage = mh.getAverageRatingForUser(activeUser);   //active user average
	                    	
	                    	OpenIntDoubleHashMap userIdToWeight = new OpenIntDoubleHashMap();
	                	    IntArrayList myUsers      			= new IntArrayList();
	                	    DoubleArrayList myWeights 			= new DoubleArrayList();
	                	    double currentWeight;
	                	        	    
	                    	for (int m =0; m<totalUsersWhoSawTargetMovie; m++)
	                    	{    		
	                    		 uid =(users.getQuick(m));  
	                    //	     currentWeight  = findUserSimilarity_SVD(activeUser, uid, targetMovie); //active item, item we want to find sim with
	                    	     currentWeight  = findUserSimilarity_SVD(activeUser, uid, targetMovie); 
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
	                	             	if(!(modelToBuild.equalsIgnoreCase("Simple")))
	                	             		voteSum += (currentWeight * (NeighUserRatingOnTargetItem + activeUserAverage));
	                	             	else
	                	             		voteSum += (currentWeight * (NeighUserRatingOnTargetItem ));
	                	             		myTotal++;
	                	      */
	                	      
	                         			//Weight 
	                         			currentWeight = myWeights.get(i);    	             	
	                	         		weightSum += Math.abs(currentWeight);				// see no Math.abs(), for adjusted cosine I think there is no need (weight >0 always)
	                	           
	                	         		NeighUserRatingOnTargetItem = P.get(targetMovie-1, uid-1);
	                	         		
	                	             	
	                	             		          voteSum += (currentWeight * (NeighUserRatingOnTargetItem));	                	             	
	                	             		//voteSum += (currentWeight * (NeighUserRatingOnTargetItem - mh.getAverageRatingForMovie(targetMovie)));
	                	             	
	                	             	myTotal++;        	
	                         		
	                          } //end of for
	                       
	                         //predict and send back         
	                         if(weightSum!=0) {
	                        	 				voteSum *= (1.0 / weightSum);	                        	 				 
	                        	 		
	                        	 				 //answer = voteSum;	                        	 		
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
	                          	double targetMovieAvg = mh.getAverageRatingForMovie(targetMovie);
	                          	
	                              // for all the common items
	                              for (int i =0; i< k; i++)
	                              {            
	                                  // get their ratings from "SVD right (SV) MATRIX"               
	                                		
	                              		
	              	                		rating1 = right.get(i, activeUser-1);                
	              	    	                rating2 = right.get(i, expectedNeighbouringUser-1);                
	                              			                           
	                              			                              	
	              	    	                topSum += rating1 * rating2;    	            
	              	    	                bottomSumActive += Math.pow(rating1+mh.getAverageRatingForUser(activeUser-1), 2);
	              	    	                bottomSumTarget += Math.pow(rating2+ mh.getAverageRatingForUser(expectedNeighbouringUser-1), 2);
	                  	            
	              	    	           }                  
	                              
	                              
	                              double  bottomSum = Math.sqrt(bottomSumTarget) * Math.sqrt(bottomSumActive);        	
	                              
	                              if (bottomSum == 0.0) sim = 0.0;            
	                              else sim =topSum / bottomSum;   
	                       
	                              
	                              return sim;            
	                          	
	                          }

	                          
	            
}