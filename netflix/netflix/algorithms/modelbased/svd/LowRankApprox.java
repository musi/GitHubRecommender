package netflix.algorithms.modelbased.svd;

import java.util.*;
import java.io.*;
import netflix.memreader.*;
import netflix.rmse.RMSECalculator;
import netflix.utilities.*;
import cern.colt.list.IntArrayList;
import cern.colt.list.LongArrayList;
import cern.colt.map.OpenIntIntHashMap;
import cern.colt.map.OpenIntObjectHashMap;
import cern.colt.matrix.*;
import cern.colt.matrix.linalg.*;
import cern.colt.matrix.impl.*;
import netflix.utilities.*;

public class LowRankApprox 
{
	//Constants
	final double THRESHOLD 	= 0.01; //constant threshold;
	public static final double DEFAULT = 1.0E-50;
	
	// 2d Matrix
	DoubleMatrix2D A; 					// |U| x k
	DoubleMatrix2D B; 					// k x |I|
	DoubleMatrix2D P; 					// |U| x |I|
	DoubleMatrix2D c; 					// k x 1
	DoubleMatrix2D M; 					// k x k
	DoubleMatrix2D d; 					// k x 1
	DoubleMatrix2D N; 					// k x k
	
	//1d Matrix, Vector
	DoubleMatrix2D a; 					// 1 x k
	DoubleMatrix2D b; 					// 1 x k
	
	//users, items
	int 				noOfUsers;				//training set users
	int 				noOfItems;				//training set items
	int 				totalItems;				// total movies in the main Set
	IntArrayList        trainingItems;		    // total training movies
	IntArrayList	    trainingUsers;		
	double              myVal[][];				// values for initialisation
	OpenIntIntHashMap   myMoviesMap;			// From movie_ID (in {1-1682}) to movie_ID (in {1-1200});
	
	//objects
	MemHelper 					mh;
	MemHelper 					mainMh;
	MemHelper 					testMh;
	Algebra   					myAlgebra;
	Random    					rand;
	Timer227 					timer;
	SingularValueDecomposition  svd;
	Property  					property;
	
	//results, Error related,
	double 				MAE;
	double 				Roc;
	int 				k;					// reduced dimensions
	double				lambda1;			// regularisation constant for M 
	double				lambda2;			// regularisation constant for N
	double 				error;				// error, which will reduce with no. of iterations
	double 				oldError;			// error, which will reduce with no. of iterations
	
	//----------------------------------------------------------------
	
	LowRankApprox(String mainF, String trainF, String testF, int dimensions)
	{
		
		mainMh			= new MemHelper (mainF);					//MemHelper object ( main)		
		mh 				= new MemHelper (trainF);					//MemHelper object (train)
		testMh			= new MemHelper (testF);					//MemHelper object (Test)
		
		//mh 				= mainMh;
		
		myAlgebra 		= new Algebra ();
		rand      		= new Random ();
		property 		= new Property (DEFAULT);
		timer 			= new Timer227();
		/*noOfUsers   	= mh.getNumberOfUsers();
		noOfItems   	= mh.getNumberOfMovies();
		*/
		
		noOfUsers   	= 943;
		noOfItems		= 1682;
		//totalItems 		= 1682;
		
		//Only use here 1682, if you are using noMap, if using map then use it
		 totalItems 	= mainMh.getNumberOfMovies();		
		
		int testItems  	= testMh.getNumberOfMovies();
		int trainItems 	= mh.getNumberOfMovies();
		
		myMoviesMap		= new OpenIntIntHashMap();
		
		
		trainingUsers	= mh.getListOfUsers();
		trainingItems	= mh.getListOfMovies();
		k 				= dimensions;		
		error 			= 0;
		
		//Data Staructure required for low rank
		A = new DenseDoubleMatrix2D (noOfUsers, k);
		B = new DenseDoubleMatrix2D (k, totalItems);
		P = new DenseDoubleMatrix2D (noOfUsers, totalItems);
		c = new DenseDoubleMatrix2D (k, 1);
		M = new DenseDoubleMatrix2D (k, k);
		d = new DenseDoubleMatrix2D (k, 1);
		N = new DenseDoubleMatrix2D (k, k);				
		a = new DenseDoubleMatrix2D (1, k);
		b = new DenseDoubleMatrix2D (1, k);
		myVal = new double[k][totalItems];
		
		//Assign the old Movie_ID to new Movie_ID
		assignNewMovieIds(totalItems);
		
		//initialise A, B
		for(int i=0;i<k;i++)
		  {
			  for(int j=0;j<totalItems;j++) // e.g. item =1 is at 0 index
			  {
				     // myVal[i][j] = cern.jet.random.Normal.staticNextDouble(3.5, 0.95);  //mean = 3.5, standard deviation =0.95
				        myVal[i][j] = cern.jet.random.Uniform.staticNextDoubleFromTo(1.0, 5.0);
				    //   myVal[i][j] = cern.jet.random.Uniform.staticNextDouble();
				   //    myVal [i][j] = 4.1;
			  }
		  }
		
		A.assign(0);
		B.assign(myVal);
	//	System.out.println("B="+B);
		
		 System.out.println("Test Ratings ="+ testMh.getAllRatingsInDB());
	     System.out.println("Train Ratings ="+ mh.getAllRatingsInDB());
	     System.out.println("Total movies after filtering ="+ totalItems);
	}	
	
/*************************************************************************************************/
  /**
   * Update A, keeping B fixed
   */
	
	public void updateAB(double c1, double c2)
	{
		timer.start();
		
		lambda1 = c1;
		lambda2 = c2;
		
		int count = 0;
		int uid = 0;
		int mid = 0;
		int movieSize = 0;
		double finalError =0;		
		LongArrayList moviesSeenByCurrentUser;		
		
		while (true)
		{		
		  // keep track of the previous error
		  oldError = error;
		  count++;		 
		  int totalSingular = 0;
		  int newMid		= 0;
		  
		  for (int i =1; i<=noOfUsers;i++)
		  {
			  c.assign(0);    			  //Initialise with zeros
			  M.assign(0); 

			  //Add regularization values to M
			  for(int t=0;t<k;t++)
			  {
				  M.set(t, t, lambda1);
			  }
			  
			  uid = (i);
			  moviesSeenByCurrentUser = mh.getMoviesSeenByUser(uid);
			  movieSize = moviesSeenByCurrentUser.size();
	
		    //For each Movie seen by current user
			  for(int j=0;j<movieSize;j++)
			  {
				    //get a mid
				  	 mid = MemHelper.parseUserOrMovie(moviesSeenByCurrentUser.getQuick(j));
				  
				  	//get new assigned mid
				  	 newMid = myMoviesMap.get(mid);
				  					  	
				    //loop to k
					for (int x=0;x<k;x++)
					{
						 double bb  = B.get(x, newMid);
						 double rat = mh.getRating(uid, mid);
						
						double temp =  rat * bb;						
						double dumC = c.get(x, 0);				   // previous val						
						c.set(x, 0, temp+dumC); 			       // c(x) = R(i,j) * B(x,j); 
					
						//loop to k
						for(int y=0;y<k;y++)
						{
							double bb1 = B.get(x, newMid);
							double bb2 = B.get(y, newMid);
												
							temp = bb1 * bb2;
							double dumM = M.get(x ,y);  // get previous value							
							M.set(x, y, temp + dumM);	// previous val + current val
							
					   } //end of for k'
						
				    } // end of for k
				
			     } //end of item for			  

				     //----------------------------------------------- 
				  	 // Solve Linear Equation: AM = C --> A = C'M^-1
				     //-----------------------------------------------
			  
			  		DoubleMatrix2D MInverse;
					int nonZeroDiagonal = 0;
					boolean flag = false;
			
					MInverse = myAlgebra.inverse(M); 						// Inverse of N
					DoubleMatrix2D cPrime = myAlgebra.transpose(c);  		// Transpose of c
					a =  myAlgebra.mult(cPrime , MInverse);

					// System.out.println("User = "+ uid +" Count =" + count + ", a="+a);
				
			     for (int myK =0; myK <k; myK++)
					{
						double temp = a.get(0, myK);
						//System.out.println("val of A="+ temp);
						A.set(uid-1, myK, temp);
					}			
					
		  }//end of user for
			
		  System.out.println(" Total Singular M="+ totalSingular + ", Non Singular ="+ (noOfUsers-totalSingular));
		  
		//----------------------------------------------------------------------------
		// Update B, keeping A fixed
		//----------------------------------------------------------------------------
		// convention: i = users, j= items
		  
		  LongArrayList usersWhoSawCurrentMovie;
		  int userSize = 0;
		  totalSingular = 0;
		  int totalMoviesNotRatedByAnyOne = 0;
		  boolean go = false;
		  
		  // for all items (We will go through all the items, as mid ={1-1682}, we have to go to 1682, as
		  // may be movie 1600 is present 1200 is not present in the main set
		  for(int j=1; j<=noOfItems;j++)
		  {
			  d.assign(0);    			  //Initialise with zeros
			  N.assign(0); 
			  
			//Add regularization values to N
			  for(int t=0;t<k;t++)
			  {
				  N.set(t, t, lambda2);
			  }
			  
			  //a mid
			  mid = (j);
			  
			  //get new assigned mid
			   newMid = myMoviesMap.get(mid);
			  
			   
			  if(newMid==0) go =false;
			  else go =true;
			  
			  if(j==1) go =true;
			  
			  if(go) //CHECK WHAT IS THE EFFECT, IF WE DO NOT USE IT?
			{
			
			  usersWhoSawCurrentMovie =  mh.getUsersWhoSawMovie(mid);
			  userSize = usersWhoSawCurrentMovie.size();
			  
			  if(userSize<1)
				  {
				       totalMoviesNotRatedByAnyOne++;
				    // System.out.println("User Size ="+userSize);
				  }
	
			  // for each user who rated this item
				  for(int i=0; i<userSize;i++)
				  {
					  
					    //get a uid
					    uid = MemHelper.parseUserOrMovie(usersWhoSawCurrentMovie.getQuick(i));
	
					    // loop to k
						for (int x=0;x<k;x++)
						{
							double aa = A.get(uid-1, x);
							// double rat = mh.getRating(uid, mid) - mh.getAverageRatingForUser(uid);;
							   double rat = mh.getRating(uid, mid);
							
							
							double temp = rat * aa;
							double dumD = d.get(x, 0);	//previous val									
							d.set(x, 0, temp + dumD); 	//previous val + new val		 
							
							// loop to k'
							for(int y=0;y<k;y++)
							{
								double aa1 = A.get(uid-1, x);								
								double aa2 = A.get(uid-1, y);
					
								temp = aa1 * aa2;
								double dumN = N.get(x, y);								
								N.set(x, y, temp + dumN);
								
							} //end of inner for
							
					    } // end of for
			
				    } //end of user for
				  
			  	 //---------------------- 
			  	 // Solve Linear Equation
			     //----------------------
				  
				  DoubleMatrix2D NInverse;
				  int nonZeroDiagonal =0;
				  

					NInverse = myAlgebra.inverse(N); 		// Inverse of N					
					DoubleMatrix2D dPrime = myAlgebra.transpose(d);  		// Transpose of d
					b =  myAlgebra.mult(dPrime , NInverse);
				
			
				 // System.out.println("item = " + mid + ", count =" + count +  ", b="+ b);
				  
				for (int myK =0; myK <k; myK++)
				{
					double temp = b.get(0, myK);
					//System.out.println("val of B="+ temp);
					B.set( myK,newMid, temp);
				}
				
			  }//end of filtering if
		  } //end of item for
		
		  
		  System.out.println(" Total Singular N="+ totalSingular + ", Non Singular ="+ (noOfItems-totalSingular));
		  System.out.println("totalMoviesNotRatedByAnyOne="+totalMoviesNotRatedByAnyOne);
		  
		  error = computeError(A,B, count);
		  finalError = oldError - error;
		  System.out.println("Current error= " + error);
		  System.out.println("Error reduced to =" + finalError);
		  System.out.println("------K=" + k + "-------Iteration=" + count + "---------");
		 		  
		  if(finalError ==0)
			  break;
		  
		  //if(count==1000) break;
		  if(count==2) break;
		  //if(error <0.001) break; //for >15 movies
		  if(error <0.5) break;
		   if(finalError<0.003 && finalError>0) break;	  
		  
		} //end of while (true)
		
		timer.stop();
		
		System.out.println("Converged after " + count + " Iteration, Error=" +error +", FinalError is ="+ finalError);
		System.out.println("Time taken ="+timer.getTime());
	}	
	

/*************************************************************************************************/

	/**
	 * Compute Error given updated A and B
	 */
				
		public double computeError(DoubleMatrix2D myA, DoubleMatrix2D myB, int iterations)
		{		
			double err =0;
			double prediction =0;
			int mid, uid, userSize, newMid;
			LongArrayList usersWhoSawCurrentMovie;
			int errorFoundInSamples =0;
			
			//for all items
			for(int j =1; j<=noOfItems;j++) //1-1682
			{
				  boolean flag = true;
				  mid = (j);
				  
				 //get new assigned mid
				  newMid = myMoviesMap.get(mid);	
				  
				  
				  usersWhoSawCurrentMovie =  mh.getUsersWhoSawMovie(mid);
				  userSize = usersWhoSawCurrentMovie.size();
						 
					  	//for all users who rated current item
						for(int i=0;i<userSize;i++)
						{	
							//reinitialize 
							prediction =0;
							
							//original rating
							uid = MemHelper.parseUserOrMovie(usersWhoSawCurrentMovie.getQuick(i));
							
							//predicited rating
							for(int myK =0; myK <k; myK++)
							{
								double t1 = A.get(uid-1, myK); 
								double t2 = B.get(myK, newMid);
								double temp = (t1 * t2);
							//	System.out.println("temp=t1*t2==>"+ temp +"="+ t1 + "*" +  t2);
								prediction += temp;
							}
						   	
							//double orgRat = mh.getRating(uid, mid)- mh.getAverageRatingForUser(uid);
							double orgRat = mh.getRating(uid, mid);
							
							//if(iterations%10==0 && iterations>0)
							{
								if(Math.abs(orgRat-prediction)>2)
									{
										/*System.out.println("prediction ="+ prediction+ ", Orginal="+orgRat
																+", mid ="+ mid +", newMid ="+ myMoviesMap.get(mid)+ ", uid="+uid);
										System.out.println("No users who rated movie="+ mh.getNumberOfUsersWhoSawMovie(mid) + 
												", User saw movies="+mh.getNumberOfMoviesSeen(uid));*/
									
										errorFoundInSamples++;
										
									}
								
								
								
							}
							
							err+= Math.pow( (orgRat - prediction), 2);						
					
						}//end of Filtering movies, not there in the Main set
				  
			   } //end of items for
			
			System.out.println ("errorFoundInSamples = "+ errorFoundInSamples);
			
			return err;			
			
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
		    	int newMid =0;
		    	
		        if ( activeUser<=943 && targetMovie <=1682)
		        {
		        	// Entry is retrieved in the correct way, i.e. rows x cols = movs x users
		        	
		        	//get new assigned mid
				  	newMid = myMoviesMap.get(targetMovie);
				  	
				  	
		        	for(int i=0;i<k;i++)
		        	{
		        		double temp1 = A.get(activeUser-1, i);
		        		double temp2 = B.get(i, newMid);		        		
		        		prediction	 +=	(temp1 * temp2);
		        	}    	
		        	
		        	// System.out.println("Prediction =" + prediction);
		        }	
		        
		       // prediction +=mh.getAverageRatingForUser(activeUser);
		        
		   /*     if(prediction < 1)
		            return 1;
		        else if(prediction > 5)
		            return 5;
		        else
		            return prediction;*/
		        
		        return prediction;
		    }

		
/*************************************************************************************************/
/**
 * Check statistics of the movies and users
 */
/*		    public void statistics ()
		    {
		    	int totalUsersLessThan20 =0;
		    	LongArrayList moviesSeenByCurrentUser;
		    	int movieSize;
		    	
		    	int totalMoviesLessThan20 =0;
		    	LongArrayList usersWhoSawCurrentMovie;
				int userSize = 0;
				  
		    // For each users	
			  for (int i =1; i<=noOfUsers;i++)
			  {				
				 int uid = (i);
				 moviesSeenByCurrentUser = mh.getMoviesSeenByUser(uid);
				 movieSize = moviesSeenByCurrentUser.size();
				 if (movieSize<20)
					 {
					 	System.out.println("Movies seen by user "+ i + " = " + movieSize);
					 	totalUsersLessThan20++;
					 }
				  
			  } //end of for	  
			

			  
			  // for all items
			  for(int j=1; j<noOfItems;j++)
			  {				 
			   	  int  mid = (j);
				  usersWhoSawCurrentMovie =  mh.getUsersWhoSawMovie(mid);
				  userSize = usersWhoSawCurrentMovie.size();
				  
				  if(userSize<2)
				  {
						System.out.println("No of users who saw movie "+ j + " = " + userSize );
					 	totalMoviesLessThan20++;
				  }
				  
			  }// end of for
			  
			  System.out.println("Users found "+ totalUsersLessThan20);
			  System.out.println("Movies found "+ totalMoviesLessThan20);
			  
		    }
		    */
			  
/*************************************************************************************************/
		
		public static void main (String args[])
		{
			String path 	="C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\SVD\\";
	    	String train 	= path +  "sml_clusteringTrainSetStoredTF1.dat";
	    	String mainF 	= path +  "sml_modifiedStoredFeaturesRatingsTF1.dat";
	    	String test		= path +  "sml_clusteringTestSetStoredTF1.dat";	    	
			int dimensions  = 10;	
			int myDimensions = 0;
			
			double bestError = 2.0;
			double bestRoc = 0;
			double bestEC1 = 0;			//will store best values of each of them Error, c1, c2
			double bestEC2 = 0;
			double bestRC1 = 0;			//will store best values of each of them Roc, c1, c2
			double bestRC2 = 0;
			String EDetails = "";
			
			OpenIntObjectHashMap errorMap = new OpenIntObjectHashMap();			
			
			for(myDimensions =10; myDimensions<=12;myDimensions++)
			{
				EDetails="";
				
				//compute low rank approx
				LowRankApprox lowRank = new LowRankApprox(mainF, train, test, myDimensions);
				
				//check statistics
				//lowRank.statistics();
				
				//calculate low rank approx				
				for(double d1=0.1, d2=0;d1<5.0;d1+=0.5)
				{
					d2 = 0.2;
					
					//call low rank approx with diff values of regularizations
					lowRank.updateAB(d1, d2);	
					double E = lowRank.testWithMemHelper(lowRank.testMh, 25);
					
					if(E<bestError)				//update best values when MAE is min
					{
						bestError  = E;
						bestEC1    = d1;
						bestEC2    = d2;
					}
					
					if(lowRank.Roc>bestRoc)		//update best values when Roc is min
					{
						bestRoc    = lowRank.Roc;
						bestRC1    = d1;
						bestRC2    = d2;
					}
					
					//test with memHelper
				    System.out.println("===============================================================================");
			        System.out.println("k= "+ myDimensions+ ",  MAE : " +E + ", Roc="+ lowRank.Roc);
			        System.out.println("===============================================================================");
			        
			        EDetails += "Error ="+ E + ", ROC=" + lowRank.Roc + ",c1=" + d1 + ",c2=" + d2;
			        EDetails += "\n";
			        
				} //add regularization constant
		
				errorMap.put(myDimensions, EDetails);
								
			} //end of one dimension
			
			//Print Final Results
			IntArrayList keys = errorMap.keys();			
		    System.out.println("======================Final Results============================================");
	        for(int i=0;i<keys.size();i++)	        	
	        	{
	        		int dim = keys.get(i);
	        		System.out.println("Error Details for Dimension="+ dim +" is =\n"+ errorMap.get(dim));	        	
	        	}
	        System.out.println("===============================================================================");
			
		} 

/**********************************************************************************************************/
 /**
  * Assign a new ID to a movie ID
  */

		
	public void assignNewMovieIds (int totalItems)
	{
		IntArrayList myItemsInMainSet = mainMh.getListOfMovies();
		int newIndex =0;
		
		//see, it starts from zero
		for (int i=1;i<=1682;i++)
		{
			if(myItemsInMainSet.contains(i)) 	//if we have this movie in the set
			{
				//Assigning this will make sure that, if u r using Top10 movies, so totalItems=1100 etc
				//Then it will make newMid = index; which is required (index ={0-totalMovies}

				  myMoviesMap.put(i,newIndex);	
				
				//Assigning this will make sure that, no matter u r using all movies, so totalItems=1682
				//Then it will make newMid = mid-1; which is required (index ={0-1681}

				//myMoviesMap.put(i,i-1);			
				newIndex++;
				
			}
		}	
		
	}
		

/**********************************************************************************************************/
		 
		    /**
		     * Using RMSE as measurement, this will compare a test set
		     * (in MemHelper form) to the results gotten from the recommender
		     *  
		     * @param testmh the memhelper with test data in it   //check this what it meant........................Test data?///
		     * @return the rmse in comparison to testmh 
		     */

		    public double testWithMemHelper(MemHelper testmh, int neighbours)     
		    {
		        RMSECalculator rmse = new RMSECalculator();
		        
		        IntArrayList users;
				LongArrayList movies;
		        String blank = "";
		        int uid, mid, total=0;
		        int totalUsers=0;
		        int totalExtremeErrors =0;
		        int totalEquals =0;
		        int totalErrorLessThanPoint5 =0;
		        int totalErrorLessThan1 =0;
		        		        
		        // For each user, make recommendations
		        users = testmh.getListOfUsers();
		        totalUsers= users.size(); 
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

		                            if (myRating==-99 )                           
		                               System.out.println(" rating error, uid, mid, rating" + uid + "," + mid + ","+ myRating);
		                           
		                            if(rrr>5.3 || rrr<=0)
		                            {
		                            	System.out.println("Prediction ="+ rrr + ", Original="+ myRating+ ", mid="+mid 
		                            		+", NewMid="+ myMoviesMap.get(mid)+ ", uid="+uid
		                            		+"No users who rated movie="+ mh.getNumberOfUsersWhoSawMovie(mid) + 
											", User saw movies="+mh.getNumberOfMoviesSeen(uid));
		                            }
		                            
		                            if(rrr>6 || rrr<-1)
		                            	totalExtremeErrors++;
		                            
		                            else if(Math.abs(rrr-myRating)<=0.5)
		                            	totalErrorLessThanPoint5++;
		                            
		                            
		                            else if(Math.abs(rrr-myRating)<=1.0)
		                            	totalErrorLessThan1++;
		                            
		                            else if (rrr==myRating)
		                            	totalEquals++;
		                            
		                         //    if(rrr>5.5 ||rrr<0) rrr = (mh.getAverageRatingForMovie(mid)+mh.getAverageRatingForUser(uid))/2.0;
		                            
		                            //-------------
		                            // Add ROC
		                            //-------------
		                            rmse.ROC4(myRating, rrr, 5);		
		            
		                            //-------------
		                            //Add Error
		                            //-------------
		                            rmse.add(myRating,rrr);		
		            
		                            //-------------
		                            //Add Coverage
		                            //-------------

		                             rmse.addCoverage(rrr);                            
		                           
		                             
		                		  }         
		            
		            }
		        }
		   
		        double dd= rmse.mae();
		        MAE = dd;
		        Roc = rmse.getSensitivity();
		        
		        System.out.println("totalExtremeErrors="+totalExtremeErrors + ", Total ="+total);
		        System.out.println("totalErrorLessThanPoint5="+totalErrorLessThanPoint5 );	       
		        System.out.println("totalErrorLessThan1="+totalErrorLessThan1 );
		        System.out.println("totalEquals="+totalEquals );
		        
		       
		        
		        
		        //rmse.resetValues();        
		        return dd;
		    }

 }

//-------------------------------------------------------------------------------------
// results of Movies Filtering
//-------------------------------------------------------------------------------------

// 1- Movie>1 ratings ... It got suspended, A lot of N singulars, initially there were no M singulars
// but with iterations 2 they start appearing. suspensions can be due to SVD computations
// 2- Movie>5 ratings ... No M singular, N wer singualrs like 550, and there were 383 movies which were
// not rated by anyone in the training set. Error were big like 800,000 and error reduction was also big
// like +- 50,000
// There were some N singular, and I made threshold to be >1, 0.5, 0.001, and error was changing with it.
// e..g with 0.001, error was going below 1000.
// Furthermore, interesting with iteration like 10 M singular become like 300.
// 3- Movie>10 ratings ..  No M singular, N wer singualrs like 10-40. No movie which is not rated by anyone.
// 1120 Movies , users =943 ; sparsity =0.90. Error is like 2000. I am breaking at if error<2000. MAE =1.02
// SVD gives like 0.90
// 3- Movie>15 ratings ..  No M singular, N wer singualrs like 570, ... error reduces to like 0.003, but final
// answer is MAE =1.03. We have reduced the sparsity as items =1011, users =943.....sparsity =0.89
// original is 0.93.  SVD gives you 0.91 here.


// Diagonal mAtrix in the SVD
//It is looking that, this matrix is totally sparse, i.e. there is no diagonal
//entry greater than zero.  (It was with movies rated by at-least 15 users)


//Zero Predictions
//At final stage, when u check the converged matrix with test set, some predictions are zero, why it is there?

//------------------
// Experiments
//------------------

//1- With all ratings, 3000 iterations: c1=0.1, c2=0.1
// k= 10,  MAE : 0.8955941349601351, Roc=0.47102853117294824, 300 errors

//2- With top10, no average was assigned-----IMP: we terminated when finalError reduces to min (0.002), iteratiosn =169
//Error was like 32144
//totalExtremeErrors=144, Total =20381
//totalErrorLessThanPoint5=8016
//totalErrorLessThan1=5935
//totalEquals=0
//===============================================================================
//k= 10,  MAE : 0.8406133996405423, Roc=0.46415991546319124

// 3- with TF1, amazing,no average
//totalExtremeErrors=91, Total =20366
//totalErrorLessThanPoint5=8388
//totalErrorLessThan1=5854
//totalEquals=0
//===============================================================================
//k= 11,  MAE : 0.805998129275589, Roc=0.4626667844835204
















