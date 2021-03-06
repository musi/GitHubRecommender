package netflix.algorithms.memorybased.memreader;

import java.util.ArrayList;
import java.util.HashMap;
import netflix.FtMemreader.*;
import netflix.memreader.MemHelper;
import netflix.utilities.Pair;
import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import cern.colt.list.LongArrayList;
import cern.colt.map.OpenIntDoubleHashMap;
import cern.colt.map.OpenIntObjectHashMap;

/**
 * A memory-based solution for recommendations for movie data.
 * 
 * For someone using the class, there's only three things you need
 * to know: how to use options, how to recommend, and when to reset.
 * 
 * First, options.  
 * This class actually contains a few memory-based
 * algorithms in one, due to their similarities.  As such, you 
 * need to define which algorithm to use.  This is made easier
 * via the options parameter in the constructor - simply input
 * the constants to define which memory-based algorithm to use.
 * 
 * Note that  correlation, 
 *            vector similarity, 
 *            correlation with
 *            default voting,                                      //mutually exclusive.   
 *            and vector similarity with inverse user frequency 
 *            
 *            
 *            Case amplification 
 *            saving weights                                       //can be used with any of these.
 * 
 * 
 * Though it seems like a good idea, I wouldn't use SAVE_WEIGHTS
 * unless you're trying to rank courses for a particular user.
 * 
 * This is because SAVE_WEIGHTS will actually slow the program down
 * if there are too many misses - that is, weights that need to be
 * retrieved.  However, if you're constantly ranking one user
 * in comparison to all others, it should definitely be used as it
 * will be a real time saver.
 * 
 * 
 * Second, recommendations.  
 * Once you've setup the options the 
 * actual recommendation process is a snap.  Just call 
 * recommend(int, int), where the first int is the user id
 * and the second int is the movie id.  It will return its 
 * recommendation.
 * 
 * What can be confusing are some of the results.  If everything
 * goes well, it will return a rating.
 *   
 * If there is absolutely no data to use for recommending (ex, no one has rated the target
 * movie) then it returns -1.  
 * 
 * If the user has already rated the movie that you're trying to predict, it will return -2.
 * 
 *   //but what about if we have diff objects of MemReader.
 *   
 * Third, resetting.  
 * If the underling database (the MemReader)
 * should ever change, you should call reset().  Some of the time
 * saving features stores data, and will not know that the database
 * has changed otherwise. 
 * 
 * @author lewda
 */
/************************************************************************************************************************/
public class FTFilterAndWeight

{
    //Codes for options variable
    public static final int CORRELATION 					= 1;
    public static final int CORRELATION_DEFAULT_VOTING 		= 2;
    public static final int VECTOR_SIMILARITY 				= 4;
    public static final int VS_INVERSE_USER_FREQUENCY 		= 8;
    public static final int CASE_AMPLIFICATION 				= 16;
    public static final int SAVE_WEIGHTS 					= 32;
  
    //we will pass correlation and save weights as option (so it is 1+32 =33)  
    
    
    // Important variables for all processes
    private FTMemHelper	 mh;
    private int 		 options;
    private int			 whichVersionIsCalled;	// 1 = simple CF, 2-Deviation based, 3- Mixed
    private int 		 totalError=0;
    private int 		 totalRec=0;
    private IntArrayList predictableVotesForAUser;
    private IntArrayList givenVotesForAUser; 
    
    // Constants for methods - feel free to change them!
    private final double amplifier 	= 2.5; 								//constant for amplifier - can be changed
    private final int d 			= 2; 								//constant for default voting
    private final int k 			= 10000; 							//constant for default voting
    private final int kd 			= k*d;
    private final int kdd 			= k*d*d;

    
    // Data that gets stored to speed up algorithms
    private HashMap<String, Double> 	savedWeights;
    private OpenIntDoubleHashMap 		vectorNorms;
    private OpenIntDoubleHashMap 		frequencies;
    private OpenIntDoubleHashMap 		stdevs;
    

 /************************************************************************************************************************/

    /**
     * Creates a new FilterAndWeight with a given 
     * FTFTMemHelper, using correlation.
     * @param tmh the FTFTMemHelper object
     */    

    
    public FTFilterAndWeight(FTMemHelper mh) 
    
    {
        this.mh = mh;
        options = CORRELATION;					//by default option is correlation
        setOptions(options);
        
        //whichVersionIsCalled =0;
    }
    
    
  /************************************************************************************************************************/
    /**
     * Creates a new FilterAndWeight with a given FTMemHelper,
     * using whatever options you want.  The options can
     * be set using the public constants in the class. 
     * 
     * @param tmh the FTMemHelper object
     * @param options the options to use
     */
 
    
    public FTFilterAndWeight(FTMemHelper mh, int options) 
    
    {
        this.mh = mh;
        setOptions(options);
        
    //    whichVersionIsCalled = version;
        
    }

  /************************************************************************************************************************/ 
 /**
  * Set the options we want
  */ 
    
    private void setOptions(int options) 
    
    {
        this.options = options;
        
        stdevs = new OpenIntDoubleHashMap();				//store--> uid, std
        IntArrayList users = mh.getListOfUsers();
    
        //_____________________________________________________
        
        //go through all the users
        for(int i = 0; i < users.size(); i++)        
        {
          	if((options & CORRELATION) != 0 
                    || (options & CORRELATION_DEFAULT_VOTING) != 0)
            
            	stdevs.put(users.getQuick(i), mh.getStandardDeviationForUser(users.getQuick(i)));
            
            else
                stdevs.put(users.getQuick(i), 1.0);	//if no correlation, std=1.0 against each uid
        }

        //_____________________________________________________
      
        if ((options & SAVE_WEIGHTS) != 0)		//we create a new object to store them
            
        	savedWeights = new HashMap<String, Double>();
       //______________________________________________________
        
        if ((options & VECTOR_SIMILARITY) != 0
                || (options & VS_INVERSE_USER_FREQUENCY) != 0)
    
        	vectorNorms = new OpenIntDoubleHashMap();

       //______________________________________________________
        
        // If using inverse user frequency,
        // Pre-calculate all of the data
        
        if ((options & VS_INVERSE_USER_FREQUENCY) != 0) 		//check them on the paper
        
        {
            frequencies = new OpenIntDoubleHashMap();
           
            double numUsers = mh.getNumberOfUsers();
            OpenIntObjectHashMap movies = mh.getMovieToCust();
            
            IntArrayList movieKeys = movies.keys();

            for (int i = 0; i < movieKeys.size(); i++) 
            
            {
                frequencies.put(movieKeys.getQuick(i), Math.log(numUsers /
                        (double) ((IntArrayList) movies.get(movieKeys.getQuick(i))).size()));
            }
        }
    }
    
    /************************************************************************************************************************/
    
    /**
     * This should be run if you change the underlying database.
     */
    
    public void reset() 
    
    {
        setOptions(options);
    }
  
  /************************************************************************************************************************/
  /************************************************************************************************************************/
    
    /**
     * Basic recommendation method for memory-based algorithms.
     * 
     * @param user the user id
     * @param movie the movie id
     * @return the predicted rating, -1 if nothing could be predicted, 
     *          -2 if already rated, or -99 if it fails (mh error)
     */

 /************************************************************************************************************************/
 /************************************************************************************************************************/

 // It gives good results, now we have to see why
    
    public double recommend(int activeUser, int targetMovie, int howMuchNeighbours, int version) 
    
    {
        // If the movie was already rated by the activeUser, return 02
        // If you want more accurate results, return the actual rating
        // (This is done just so that it can tell you what movies to
        // watch, but avoid the ones you have already watched)
    
    	/*
    	 * Basically, it is for movies, which a user have not seen before, 
    	 * (I think, It means, you can divide it into a test set, so in test set
    	 *  nobody has seen movie which we want to predict?)
    	 * 
    	 */
    	
    	whichVersionIsCalled = version;
    	
    	if (mh.getRating(activeUser, targetMovie) > 0) //it can not be the case now, as we are dealing with test and train set separately 
    	
    	{
            return -2;
        }
     
       	
        double currWeight, weightSum = 0, voteSum = 0;
        int uid;

        //But this will return the active user as well?
        LongArrayList users = mh.getUsersWhoSawMovie(targetMovie);		//this should also be done in (total set - test set)
       
        int limit = users.size();
        if (howMuchNeighbours < limit) limit = howMuchNeighbours; 	//by default all
       
        //__________________________________________________
        //go through all users who saw the target movie, to find weights
        
        for (int i = 0; i < limit; i++)         
        {
        	//do this if we are using same dataset (without train and test)

        	if(i!=activeUser) //not the active user .... I think they implemented it in such a way so that data are already separated into test and training 
        					  // so if we are calling this function for some user, then he will not be here in the training set	
        	{
            
       		uid = FTMemHelper.parseUserOrMovie(users.getQuick(i));            
            currWeight = weight(activeUser, uid);				//get weights of two users depending on the similarity function they r using
            weightSum += Math.abs(currWeight);
            
            //why std dev is required?
           
             voteSum += stdevs.get(activeUser) * 
                       (
                        (currWeight *(mh.getRating(uid, targetMovie)- mh.getAverageRatingForUser(uid)))
                        / stdevs.get(uid)
                    	) ;     
            
        	}
        	
        } //end for

        
        // Normalize the sum, such that the unity of the weights is one (K)
        voteSum *= 1.0 / weightSum;
        
        // Add to the average vote for user (rounded) and return
        double answer = mh.getAverageRatingForUser(activeUser) + voteSum;
        
        //This implies that there was no one associated with the current user.
        if (answer == 0 || Double.isNaN(answer))
            return -1;
        else
            return answer;
    }

    
 /************************************************************************************************************************/
    
  public double recommendSLOO(int activeUser, int targetMovie, 
		  					  int howMuchNeighbours, IntArrayList givenMovies,
		  					  double activeUserAverage 	)    
   {
	  
    /*	if (mh.getRating(activeUser, targetMovie) > 0) //it can not be the case now, as we are dealing with test and train set separately   	
    	{
            return -2;
        }
     */
	  
	  //These are the observale votes
	    givenVotesForAUser = givenMovies;	    
	    if (givenVotesForAUser.contains(targetMovie))
	    	givenVotesForAUser.delete(targetMovie);
	  
	    //_______________________________________________________
	    
        double currWeight, weightSum = 0, voteSum = 0, weightSumAbs = 0;
        int uid;

        //But this will return the active user as well?
        LongArrayList users = mh.getUsersWhoSawMovie(targetMovie);		//this should also be done in (total set - test set)
        int limit = users.size();  
        int orgLimit = limit;
        if(limit <=1) return -10;
        
        //__________________________________________________
        //go through all users who saw the target movie, to find weights
            
        OpenIntDoubleHashMap uidToWeight = new  OpenIntDoubleHashMap();
        IntArrayList myUsers = new IntArrayList();
        DoubleArrayList myWeights = new DoubleArrayList();               
        double currentWeight;
        //__________________________________________________
        //go through all users who saw the target movie, to find weights
        
        //get all weights
        for (int i = 0; i < limit; i++)       
        {
        	uid = FTMemHelper.parseUserOrMovie(users.getQuick(i));
        	
        	if (uid != activeUser)
        	{        		
        		currentWeight  = weight(activeUser, uid);
        		uidToWeight.put(uid, currentWeight);
        	}
       }
        
        //topWeights = quickSort.quicksort(topWeights);
        myUsers = uidToWeight.keys();
        myWeights = uidToWeight.values();
        uidToWeight.pairsSortedByValue(myUsers, myWeights);
        
       if (howMuchNeighbours < limit) limit = howMuchNeighbours; 	//by default all
       
       
        int totalNeighbourFound =myUsers.size();
        double neighRating=0;
        
        for (int i = totalNeighbourFound-1, myTotal=0; i >=0; i--, myTotal++)       
        {
        	if(myTotal==limit) break;
        	
        	uid = myUsers.get(i);
        	
            //if (uid != activeUser)
        	{
            	currentWeight= myWeights.get(i);
            	weightSum +=  currentWeight;
        		neighRating = mh.getRating(uid, targetMovie);        	
        		voteSum+= ( currentWeight* (neighRating  - mh.getAverageRatingForUser(uid))) ;
        		
        	} 
        
        }//end for
        
        //if (weightSumAbs != weightSum) System.out.println(" Abs = "+ weightSumAbs + ", weight= "+ weightSum);
          voteSum *= 1.0 / weightSum;
                
         // Add to the average vote for user (rounded) and return
        //As pearson can go from -1 to 1 so if no good user is found use my own global average
          double answer;        
          if (voteSum>0) answer= activeUserAverage + voteSum;
          else answer = activeUserAverage;
       
        
        //System.out.println(", total users " + orgLimit);
      //  System.out.println(", total rec " + (totalRec++) +", total errors ="+totalError);
        //This implies that there was no one associated with the current user.
        if (answer == 0 || Double.isNaN(answer))
            {
        		System.out.print(" Error found " + (totalError++));
        //		System.out.print(", answer=" + answer);
        //		System.out.print(", weight sum=" + weightSum);
        //		System.out.print(", total weights= " + totalNeighbourFound);
        //		System.out.print(", total users " + orgLimit);
        //		System.out.print(", uid, mid " + activeUser + "," + targetMovie);
        		
        		
        		
        		if (mh.checkIfMovieIsThere(targetMovie) == false) 
        			System.out.print(", movie not found" + targetMovie);
        		
        		//if (mh.checkIfUserIsThere() == false) 
        		//	System.out.print(", movie not found" + targetMovie);
        		
        	//	System.out.println();
        		
        		return -1;
            }
        
        
        else
            return answer;
    }

    
 /************************************************************************************************************************/
     
   public double recommendSK(int activeUser, int targetMovie, int howMuchNeighbours)    
    {
	   double currWeight, weightSum = 0, voteSum = 0, weightSumAbs = 0;
       int uid;

       //But this will return the active user as well?
       LongArrayList users = mh.getUsersWhoSawMovie(targetMovie);		//this should also be done in (total set - test set)
       
       int limit = users.size();
       int orgLimit = limit;
       //System.out.println("--------- total users " + orgLimit);
       //if(limit <=1) return -10;	//filter movies rated by only one guy
       //__________________________________________________
       //go through all users who saw the target movie, to find weights
       
       OpenIntDoubleHashMap uidToWeight = new  OpenIntDoubleHashMap();
       IntArrayList myUsers = new IntArrayList();
       DoubleArrayList myWeights = new DoubleArrayList();
       double currentWeight;
       //__________________________________________________
       //go through all users who saw the target movie, to find weights
       
       //get all weights
       for (int i = 0; i < limit; i++)       
       {
    	    uid = FTMemHelper.parseUserOrMovie(users.getQuick(i));    	    
    	    
    	    {
    	    	currentWeight  = weight(activeUser, uid);
    	    	uidToWeight.put(uid, currentWeight);
    	    }
      }
       
       //topWeights = quickSort.quicksort(topWeights);
       myUsers = uidToWeight.keys();
       myWeights = uidToWeight.values();
       uidToWeight.pairsSortedByValue(myUsers, myWeights);
       
      if (howMuchNeighbours < limit) limit = howMuchNeighbours; 	//by default all
      
      
       int totalNeighbourFound =myUsers.size();
       double neighRating=0;
       
       
       for (int i = totalNeighbourFound-1, myTotal=0; i >=0; i--, myTotal++)       
       {
       	if(myTotal==limit) break;
       	
       		uid = myUsers.get(i);       	
           	currentWeight= myWeights.get(i);
           	weightSum += currentWeight;
       		neighRating = mh.getRating(uid, targetMovie);        	
       		voteSum+= ( currentWeight* (neighRating  - mh.getAverageRatingForUser(uid))) ;
       
       }//end for
             
       //if (weightSumAbs != weightSum) System.out.println(" Abs = "+ weightSumAbs + ", weight= "+ weightSum);
         voteSum *= 1.0 / weightSum;
               
        // Add to the average vote for user (rounded) and return
       double answer = mh.getAverageRatingForUser(activeUser) + voteSum;
       
       //This implies that there was no one associated with the current user.
       if (answer == 0 || Double.isNaN(answer))
       {
     /*     System.out.println(" errror");
          System.out.print(" Error found " + (totalError++));
          System.out.print(", answer=" + answer);
          System.out.print(", weight sum=" + weightSum);
          System.out.print(", total weights= " + totalNeighbourFound);
          System.out.print(", total users " + orgLimit);
          System.out.println(", uid, mid " + activeUser + "," + targetMovie);
       */   
          return -1;
       }
       
       else
           return answer;     


}

     
  /************************************************************************************************************************/

  public double recommendSU(int activeUser, int targetMovie, int howMuchNeighbours, int version) 
     
     {
 	    whichVersionIsCalled = version;
 	    
     	if (mh.getRating(activeUser, targetMovie) > 0) //it can not be the case now, as we are dealing with test and train set separately 
     	
     	{
     		System.out.println("use is there in train set already");
     		return -2;
         }
      
     	
         double currWeight, weightSum = 0, voteSum = 0;
         int uid;

         //But this will return the active user as well?
         LongArrayList users = mh.getUsersWhoSawMovie(targetMovie);		//this should also be done in (total set - test set)
         
         //__________________________________________________
         //go through all users who saw the target movie, to find weights
         
         int limit = users.size();
         if (howMuchNeighbours < limit) limit = howMuchNeighbours; 	//by default all
          
         /*
         for (int i = 0; i < limit; i++)        
         {
             //find wighted sum	
               
        		uid = FTMemHelper.parseUserOrMovie(users.getQuick(i));
             currWeight = weight(activeUser, uid);
             weightSum += Math.abs(currWeight);            
         }
           */
         
         
         for (int i = 0; i < limit; i++)       
         {
        	 
         	uid = FTMemHelper.parseUserOrMovie((int)users.getQuick(i));
         
         	//if(uid !=activeUser)
         	{
         			currWeight = weight(activeUser, uid);	
         			weightSum += Math.abs(currWeight);
             
         				//simple weighted sum
         				voteSum+= ((currWeight *
             		    (mh.getRating(uid, targetMovie)- mh.getAverageRatingForUser(uid)))) ;
         	}
         	
         } //end for

         // Normalize the sum, such that the unity of the weights is one
         voteSum *= 1.0 / weightSum;
         
          // Add to the average vote for user (rounded) and return
         double answer = mh.getAverageRatingForMovie(targetMovie)+ voteSum;
         //  double answer = mh.getAverageRatingForUser(activeUser) + voteSum;
         
         //This implies that there was no one associated with the current user.
         if (answer == 0 || Double.isNaN(answer))
             return -1;
         else
             return answer;
     }

     
  /************************************************************************************************************************/


  public double recommendH(int activeUser, int targetMovie, int howMuchNeighbours, int version)  
  {
	 
  	if (mh.getRating(activeUser, targetMovie) > 0) //it can not be the case now, as we are dealing with test and train set separately 
  	
  	{
          return -2;
      }
   
  	
      double currWeight, weightSum = 0, voteSum = 0;
      int uid;

      //But this will return the active user as well?
      LongArrayList users = mh.getUsersWhoSawMovie(targetMovie);		//this should also be done in (total set - test set)
      
      //__________________________________________________
      //go through all users who saw the target movie, to find weights
      
      for (int i = 0; i < users.size(); i++) 
      
      {
          //find wighted sum	
            
     	  uid = FTMemHelper.parseUserOrMovie((int)users.getQuick(i));
          currWeight = weight(activeUser, uid);
          weightSum += Math.abs(currWeight);
          
      }

      
      for (int i = 0; i < users.size(); i++)
     
      {
      	uid = FTMemHelper.parseUserOrMovie((int)users.getQuick(i));
      	currWeight = weight(activeUser, uid);	
          
      	//simple weighted sum
           voteSum+= weightSum * 
           	(
           	 (currWeight *(mh.getRating(uid, targetMovie)- mh.getAverageRatingForUser(uid)))
           	 
           	) ;
          
          
              	
      } //end for

       // Add to the average vote for user (rounded) and return
      double answer = mh.getAverageRatingForUser(activeUser) + voteSum;
      
      //This implies that there was no one associated with the current user.
      if (answer == 0 || Double.isNaN(answer))
          return -1;
      else
          return answer;
  }

  
/************************************************************************************************************************/
  

    /**
     * Weights two users, based upon the constructor's options.
     * 
     * @param activeUser
     * @param targetUser
     * @return
     */
    private double weight(int activeUser, int targetUser) 
    
    {
        double weight = -99;
      
        //__________________________________________________
        
        // If active, sees if this weight is already stored
        if ((options & SAVE_WEIGHTS) != 0) 
        
        {
            weight = getWeight(activeUser, targetUser);	//we first check if weights are there, fine;
            											//else compute them and store as well(if option of saving is set)
            if (weight != -99)
                return weight;		
        }
        
        //__________________________________________________

        // Use an algorithm to weigh the two users
        if ((options & CORRELATION) != 0)
            weight = correlation(activeUser, targetUser);
        
        else if ((options & CORRELATION_DEFAULT_VOTING) != 0)
            weight = correlationWithDefaultVoting(activeUser, targetUser);
        
        else if ((options & VECTOR_SIMILARITY) != 0 
                || (options & VS_INVERSE_USER_FREQUENCY) != 0 )
            weight = vectorSimilarity(activeUser, targetUser);

        // If using case amplification, amplify the results
        if ((options & CASE_AMPLIFICATION) != 0)
            weight = amplifyCase(weight);

        //______________________________________________________
        // If saving weights, add this new weight to memory
        if ((options & SAVE_WEIGHTS) != 0)
            addWeight(activeUser, targetUser, weight);

        return weight;
    }

/************************************************************************************************************************/
    
    /**
     * Correlation weighting between two users, as provided in "Empirical
     * Analysis of Predictive Algorithms for Collaborative Filtering."
     * 
     * @param mh the database to use
     * @param activeUser the active user
     * @param targetUser the target user
     * @return their correlation
     */
    
    //I have to check them to make sure that they are right or not?, also after
    //sufficient familiarity, may change them to see results
    
    private double correlation(int activeUser, int targetUser) 
    
    {
    	 double functionResult=0.0;
    	 double topSum, bottomSumActive, bottomSumTarget, rating1, rating2;
         topSum = bottomSumActive = bottomSumTarget = 0;
         
         double activeAvg = mh.getAverageRatingForUser(activeUser);
         double targetAvg = mh.getAverageRatingForUser(targetUser);
         
         ArrayList<Pair> ratings = mh.innerJoinOnMoviesOrRating(activeUser,targetUser, true);
         
         // Do the summations
         //_______________________________________________________________
         // for all the common movies
         for (Pair pair : ratings)         
         {
        	 if (givenVotesForAUser.contains( FTMemHelper.parseUserOrMovie(pair.a))) //e.g. ALL But One 
        	 {
             rating1 = (double) FTMemHelper.parseRating(pair.a) - activeAvg;
             rating2 = (double) FTMemHelper.parseRating(pair.b) - targetAvg;
             
             topSum += rating1 * rating2;
         
             bottomSumActive += Math.pow(rating1, 2);
             bottomSumTarget += Math.pow(rating2, 2);
        	 }
         }
         
         double n = ratings.size() - 1;
         
         //So we get results even if they match on only one item
         //(Better than nothing, right?)
         if(n == 0)
             n++;
         
        // This handles an emergency case of dividing by zero
        if (bottomSumActive != 0 && bottomSumTarget != 0)
        { 
        //	if (whichVersionIsCalled==1)  functionResult = (1 * topSum) / Math.sqrt(bottomSumActive * bottomSumTarget);  //why multiply by n?
        //	if (whichVersionIsCalled==2)  functionResult = (n * topSum) / Math.sqrt(bottomSumActive * bottomSumTarget);  //why multiply by n?
        	
        	functionResult = (1 * topSum) / Math.sqrt(bottomSumActive * bottomSumTarget);  //why multiply by n?
        	
        	return  functionResult;
        }
        
        else
            return 1;
    }
    
 /************************************************************************************************************************/
    /**
     * Correlation weighting between two users, as provided in "Empirical
     * Analysis of Predictive Algorithms for Collaborative Filtering."
     * 
     * Also uses default voting, which uses a full outer join and adds
     * mythical votes to each user.  (It does work better, trust me.)
     * 
     * @param activeUser the active user id
     * @param targetUser the target user id
     * @return their correlation
     */
    private double correlationWithDefaultVoting(int activeUser, int targetUser) 
    
    {
        int parta, partb, partc, partd, parte, rating1, rating2, n;
        
        ArrayList<Pair> ratings = mh.fullOuterJoinOnMoviesOrRating(activeUser,
                targetUser, true);
        
        parta = partb = partc = partd = parte = 0;
       
        n = ratings.size();

        // Do the summations
        for (Pair pair : ratings) 
        
        {
            if(pair.a == 0)
                rating1 = d;
            else
                rating1 = FTMemHelper.parseRating(pair.a);
            
            if(pair.b == 0)
                rating2 = d;
            else
                rating2 = FTMemHelper.parseRating(pair.b);

            parta += rating1 * rating2;
            partb += rating1;
            partc += rating2;
            partd += Math.pow(rating1, 2);
            parte += Math.pow(rating2, 2);;
        }
        
        //Do some crazy calculations to come up with the correlation
        double answer = ((n+k)*(double)(parta+kdd) - (partb+kd)*(double)(partc+kd)) / 
                Math.sqrt(((n+k)*(double)(partd+kdd) - Math.pow(partb+kd, 2))
                     *((n+k)*(double)(parte+kdd) - Math.pow(partc+kd, 2)));
        
        //In case one student got the same grade all the time, etc.
        if(Double.isNaN(answer))
            return 1;
        else
            return answer;
    }
    
 /************************************************************************************************************************/
    
    /**
     * Treats two users as vectors and find out their cosine similarity.
     * 
     * It can also use inverse user frequency, if VS_INVERSE_USER_FREQUENCY
     * is active.
     * 
     * As described in "Empirical Analysis of Predictive Algorithms 
     * for Collaborative Filtering."
     * 
     * @param activeUser the active user id
     * @param targetUser the target user id
     * @return their similarity
     */

    private double vectorSimilarity(int activeUser, int targetUser) 
    
    {
    	double bottomActive, bottomTarget, weight;
        LongArrayList ratings;
        
        ArrayList<Pair> commonRatings = mh.innerJoinOnMoviesOrRating(
                activeUser, targetUser, true);
        
        bottomActive = bottomTarget = weight = 0;

        // Find out the bottom portion for summation on active user
        if (vectorNorms.containsKey(activeUser))        
        {
            bottomActive = vectorNorms.get(activeUser);
        }
        
        else         
        {
            ratings = mh.getMoviesSeenByUser(activeUser);
        
           if ((options & VS_INVERSE_USER_FREQUENCY) == 0)  //simple VS
            {
                for (int i = 0; i < ratings.size(); i++) 
                
                {
                    bottomActive += Math.pow(FTMemHelper.parseRating((int)ratings	// sqrt(sum(all movies seen by user(sq(vote))));
                            .getQuick(i)), 2);
                }
            }
            
            else //VS + IUF            
            {
                for (int i = 0; i < ratings.size(); i++) 
                
                {
                    bottomActive += Math.pow(frequencies.get(FTMemHelper
                            .parseUserOrMovie((int)ratings.getQuick(i)))
                            * FTMemHelper.parseRating((int)ratings.getQuick(i)), 2);
                }
            }
            
            bottomActive = Math.sqrt(bottomActive);
            vectorNorms.put(activeUser, bottomActive);
        }

        
        // Find out the bottom portion for summation on target user
        if (vectorNorms.containsKey(targetUser))        
        {
            bottomTarget = vectorNorms.get(targetUser);
        }
        
        
        else        
        {
            ratings = mh.getMoviesSeenByUser(targetUser);		//all te votes provided by this user
           
            if ((options & VS_INVERSE_USER_FREQUENCY) == 0)	//VS            
            {
                for (int i = 0; i < ratings.size(); i++)             
                {
                    bottomTarget += Math.pow(FTMemHelper.parseRating((int)ratings
                            .getQuick(i)), 2);
                }
            }
        
            else // VS + IUF
            {
                for (int i = 0; i < ratings.size(); i++)                 
                {
                    bottomTarget += Math.pow(frequencies.get(FTMemHelper
                            .parseUserOrMovie((int)ratings.getQuick(i)))
                            * FTMemHelper.parseRating((int)ratings.getQuick(i)), 2);
                }
            }
            
            bottomTarget = Math.sqrt(bottomTarget);
            vectorNorms.put(targetUser, bottomTarget);
        }

        
        // Do the full summation
        if ((options & VS_INVERSE_USER_FREQUENCY) == 0) //VS        
        {
            for (Pair pair : commonRatings)             
            {
                weight += FTMemHelper.parseRating(pair.a) * FTMemHelper.parseRating(pair.b);
            }
        }
        
        else 
        { 
            for (Pair pair : commonRatings) 
            {
                weight += (frequencies.get(FTMemHelper.parseUserOrMovie(pair.a)) * FTMemHelper
                        .parseRating(pair.a))
                        * (frequencies.get(FTMemHelper.parseUserOrMovie(pair.b)) * FTMemHelper
                        .parseRating(pair.b));
            }
        }
        
        weight /= bottomActive * bottomTarget;
        
        return weight;
    }

 /***********************************************************************************************************************/
    
    /**
     * "Amplifies" any weight, by a constant (defined at top).
     * 
     * @param weight the weight
     * @return the amplified weight
     */
 
     
    private double amplifyCase(double weight) 
    
    {
        if (weight >= 0)
            return Math.pow(weight, amplifier);
        else
            return -Math.pow(-weight, amplifier);
    }
  
 /************************************************************************************************************************/
    
    /**
     * Saves the weight between two users.
     *  
     * @param user1 
     * @param user2 
     * @param weight 
     */
    private void addWeight(int user1, int user2, double weight) 
    
    {
        savedWeights.put(user1 + ";" + user2, new Double(weight));
    }

/************************************************************************************************************************/
    
    /**
     * Returns a weight if this object has calculated the weight
     * between the two users before.
     * 
     * Returns -99 if there is no weight.
     * @param user1
     * @param user2
     * @return the weight if found, otherwise -99
     */
    private double getWeight(int user1, int user2) 
    
    {
        if(savedWeights.containsKey(user1 + ";" + user2))
            return savedWeights.get(user1 + ";" + user2);

        else if(savedWeights.containsKey(user2 + ";" + user1))
            return savedWeights.get(user2 + ";" + user1);

        return -99;
    }

/************************************************************************************************************************/
    
    /**
     * Prints out the options being used for easy viewing
     * @param options
     */
    public static void printOptions(int options) 
    
    {
        if ((options & CORRELATION) != 0)
            System.out.print("CORRELATION");
        
        else if ((options & VECTOR_SIMILARITY) != 0)
            System.out.print("VECTOR_SIMILARITY");
        
        else if ((options & CORRELATION_DEFAULT_VOTING) != 0)
            System.out.print("CORRELATION_DEFAULT_VOTING");
        
        else if ((options & VS_INVERSE_USER_FREQUENCY) != 0)
            System.out.print("VS_INVERSE_USER_FREQUENCY");

        if ((options & CASE_AMPLIFICATION) != 0)
            System.out.print(" with CASE_AMPLIFICATION");

        if ((options & SAVE_WEIGHTS) != 0)
            System.out.print(", SAVE_WEIGHTS active");

        System.out.println(".");
    }
}

