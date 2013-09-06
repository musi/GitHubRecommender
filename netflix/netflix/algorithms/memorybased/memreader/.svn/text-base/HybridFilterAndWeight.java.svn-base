package netflix.algorithms.memorybased.memreader;

import java.util.ArrayList;
import java.util.HashMap;

import netflix.FtMemreader.FTMemHelper;
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
public class HybridFilterAndWeight 
/************************************************************************************************************************/

{
    //Codes for options variable
    public static final int CORRELATION 					= 1;
    public static final int CORRELATION_DEFAULT_VOTING 		= 2;
    public static final int VECTOR_SIMILARITY 				= 4;
    public static final int VS_INVERSE_USER_FREQUENCY 		= 8;
    public static final int CASE_AMPLIFICATION 				= 16;
    public static final int SAVE_WEIGHTS 					= 32;
  
    // Debug the program
      boolean ifDebug = false; 
    
    //we will pass correlation and save weights as option (so it is 1+32 =33)  
    
    
    // Important variables for all processes
    private MemHelper	 mh;
    private MemHelper	 filledMh;
    private int 		 options;
    private int			 whichVersionIsCalled;	// 1 = simple CF, 2-Deviation based, 3- Mixed
    private int 		 thisIsTargetMovie;
    private int 		 pearsonDeviationNotWorking;
    
    // for content boosted
    private double 		 selfWeight ;
    private double 		 harmonicWeight;
   
    
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
     * MemHelper, using correlation.
     * @param tmh the MemHelper object
     */    

    
    public HybridFilterAndWeight(MemHelper mh) 
    
    {
        this.mh = mh;
        options = CORRELATION;					//by default option is correlation
        setOptions(options);
        
        //whichVersionIsCalled =0;
        pearsonDeviationNotWorking = 0;
        harmonicWeight = 0;
        selfWeight     = 0;
        
    }
    
    
  /************************************************************************************************************************/
    /**
     * Creates a new FilterAndWeight with a given MemHelper,
     * using whatever options you want.  The options can
     * be set using the public constants in the class. 
     * 
     * @param tmh the MemHelper object
     * @param options the options to use
     */
 
    
    public HybridFilterAndWeight(MemHelper mh, int options) 
    
    {
        this.mh = mh;
        setOptions(options);
        
    //    whichVersionIsCalled = version;
        pearsonDeviationNotWorking=0;
        
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
            
       		uid = MemHelper.parseUserOrMovie(users.getQuick(i));            
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
    /**
     * Print in how many cases, user avg was same as his rating, so pearson ll not work in that rating
     */
    
    public void printPearsonError()
    {
    	System.out.println("Total cases = "+ pearsonDeviationNotWorking);
    	pearsonDeviationNotWorking=0;
    }
    
 /************************************************************************************************************************/
  
    /**
     * make content-boosted based recommendations
     */
    
  public double recommendS(int activeUser, int targetMovie, int howMuchNeighbours, MemHelper filledTrObj)    
  {
	   //System.out.println ("Came in recommendS for ....");
	   filledMh				= filledTrObj;	   
	   thisIsTargetMovie    = targetMovie;
	   
	   //get the content based rating (predicted by naive bayes classifier)
	   double contentBasedSelfRating = filledMh.getRating(activeUser, targetMovie);
	   
	   if(ifDebug)
	   {
		   System.out.println(" content-basedSelfrating =" + contentBasedSelfRating);
	   }
	   
	   //define local variables
	   double currWeight, weightSum = 0, voteSum = 0, weightSumAbs = 0;
       int uid;              
             
       // get no of user who was target movie       			
       IntArrayList users			= new IntArrayList();
       LongArrayList usersTraining 	= mh.getUsersWhoSawMovie(targetMovie);       

       int limitTraining = usersTraining.size();
       
       for (int j=0;j<limitTraining; j++)
    	   users.add(MemHelper.parseUserOrMovie(usersTraining.getQuick(j)));
        
       int limit = users.size();
           
       //  if(limit <=0) return -10;	//filter movies rated by only one guy (Filtr2:) (Check the filters)
       
       //----------------------------------------------------------------
       //go through all users who saw the target movie, to find weights
       //----------------------------------------------------------------
       
       OpenIntDoubleHashMap uidToWeight  = new  OpenIntDoubleHashMap();
       IntArrayList myUsers     		 = new IntArrayList();
       DoubleArrayList myWeights 		 = new DoubleArrayList();
       double currentWeight;
       
       //----------------------------------------------------------------
       //go through all users who saw the target movie, to find weights
       //----------------------------------------------------------------
       
       //get all weights
       for (int i = 0; i < limit; i++)       
       {
    	    
    	    uid = users.getQuick(i);
    	    currentWeight  = weight(activeUser, uid);
    	    uidToWeight.put(uid, currentWeight);    	      	    
       }
       
       
       myUsers 		= uidToWeight.keys();
       myWeights 	= uidToWeight.values();
       uidToWeight.pairsSortedByValue(myUsers, myWeights);
       
       if (howMuchNeighbours < limit) limit = howMuchNeighbours; 	//by default all
            
       int totalNeighbourFound = myUsers.size();
       double neighRating	   = 0;       
       
       for (int i = totalNeighbourFound-1, myTotal=0; i >=0; i--, myTotal++)       
       {    	   
       		if(myTotal == limit) break;     	
       	
       		uid = myUsers.get(i);       	
       		
       		//--------------------
       		// simple
       		//--------------------
       		
       		//get weights by pearson
       		currentWeight= myWeights.get(i);       		
       		
       		
       		//weighted sum as given in content-boosted sim	       	
    		   //weightSum += ( Math.abs(currentWeight));
   		        weightSum += ( Math.abs(currentWeight) * (harmonicWeight));
	    	 
       		//get user rating from two diff training files we have 
       		   neighRating = mh.getRating(uid, targetMovie);	    	
        	
       		if (ifDebug)
       		{
       			//	   if( (!(neighRating <=0) && !(neighRating>0)) ||(neighRating ==Double.NaN))
       			{
       			   System.out.println(" neighRating = "+ neighRating);
       			   System.out.println(" H.M. = "+ harmonicWeight);
       			}
       		}
       		
        	//voted sum
       		double combinedAverageRating_neighbour = (filledMh.getAverageRatingForUser(uid) + mh.getAverageRatingForUser(uid))/2.0;
       		
       		if(ifDebug)
       		{
	       		//if( (!(combinedAverageRating_neighbour>=0) && !(combinedAverageRating_neighbour <0))  || (combinedAverageRating_neighbour == Double.NaN))
	       		{
	       			System.out.println(" filled avg = " +filledMh.getAverageRatingForUser(uid) );
	       			System.out.println(" mh avg = " +mh.getAverageRatingForUser(uid) );
	       			
	       		}
       		}
       		voteSum+= 	(currentWeight* harmonicWeight * (neighRating  - combinedAverageRating_neighbour)) ;    
  		
       	/*	   // Taste approach
            	currentWeight= (myWeights.get(i)+1);
            	weightSum += Math.abs(currentWeight+1);
       	    	neighRating = mh.getRating(uid, targetMovie);        
         	    voteSum+= (currentWeight* (neighRating  - mh.getAverageRatingForUser(uid))) ;
       	*/
       		
           	//Simple, but do not take -ve into accounts
       /*			currentWeight= (myWeights.get(i));      		
       			System.out.println(" weight = " + currentWeight);
       	 
       		if (currentWeight>0)
       		{	
       			weightSum += Math.abs(currentWeight);      		
           		neighRating = mh.getRating(uid, targetMovie);        
           		// System.out.println(" neig rating =" + neighRating);
           		voteSum+= ( currentWeight* (neighRating  - mh.getAverageRatingForUser(uid))) ;
       		}//end of weight should be positive
       */
       		
       }//end for

        double combinedAverageRating_activeUser = (filledMh.getAverageRatingForUser(activeUser) + mh.getAverageRatingForUser(activeUser))/2.0;
        voteSum +=  (selfWeight * (contentBasedSelfRating  - combinedAverageRating_activeUser));
        weightSum += selfWeight;
       
        if(ifDebug)
        {
	        if(contentBasedSelfRating <=0)        	
	        	 System.out.println("uid, mid, rating="+ activeUser + "," + targetMovie + ","+contentBasedSelfRating );
        }
       if (weightSum==0)				// If no weight, then it is not able to recommend????
    	   return 0;
       
        voteSum *= 1.0 / weightSum;
        double answer = combinedAverageRating_activeUser + voteSum;

        if (ifDebug)  
        {
           System.out.println("-----------------------------");
           System.out.println(" vote Sum="+ voteSum);
           System.out.println(" weight Sum="+ weightSum);
           System.out.println(" Harmonic Weight="+ harmonicWeight);
           System.out.println(" active user avg="+ combinedAverageRating_activeUser);
           System.out.println(" active user content rating="+ contentBasedSelfRating);
           
                     
        }
       
       //This implies that there was no one associated with the current user.
       if (answer <=0)  
       {
          return combinedAverageRating_activeUser;          
       }
       
       
       else
           return answer;     


    }

/*****************************************************************************************************/
  
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
    	 int amplifyingFactor = 50;			//give more weight if users have more than 50 movies in common
    	 double functionResult= 0.0;    	 
    	 double topSum, bottomSumActive, bottomSumTarget, rating1, rating2;
         topSum = bottomSumActive = bottomSumTarget = 0;
    	 
    	 //---------------------------
    	 // Content boosted
    	 //---------------------------
    	 //define variables
         double  max			  = 2;	
         double harmonicMean 	  = 0.0;
         double significantWeight = 0.0;
    	 double m_i				  = 0.0;
    	 double m_j				  = 0.0;
    	 
    	 int n_i = mh.getNumberOfMoviesSeen(activeUser);
    	 int n_j = mh.getNumberOfMoviesSeen(targetUser);
    	 
    	 if(ifDebug)
    	 {
	    //	 if(((( !(n_i <=0) && !(n_i>0)) || (!(m_j <=0) && !(m_j>0)))) || (m_i == Double.NaN || m_j == Double.NaN))
	    	 {
	    		 System.out.println(" n_i =" +n_i);
	    		 System.out.println(" n_j =" +n_j);
	    		 
	    	 }    	 
    	 }
    	 
    	 if(n_i<50) m_i = (n_i * 1.0)/50;			//check how he checked 50
    	 else       m_i = 1;   	 
    	 if(n_j<50) m_j = (n_j * 1.0)/50;
    	 else       m_j = 1;

    	 harmonicMean = (2 * m_i * m_j) / (m_i + m_j);
         
         double activeAvg = mh.getAverageRatingForUser(activeUser);
         double targetAvg = mh.getAverageRatingForUser(targetUser);         
    	 
         ArrayList<Pair> ratings = mh.innerJoinOnMoviesOrRating(activeUser,targetUser, true);
         
         //------------------------
         //significant weighting
         //------------------------
         
         if(ratings.size()!=0)
         {
	         if (ratings.size() > amplifyingFactor)    significantWeight = 1;
	         else 									   significantWeight = ratings.size()/amplifyingFactor;
         }
         
         else significantWeight = 0;
         
         //------------------------
         //harmonicWeight is now   
         //------------------------
         
           harmonicWeight = significantWeight + harmonicMean;

           if(ifDebug)
           {
	       //    if ((!(harmonicWeight <=0) && !(harmonicWeight >0)) || harmonicWeight == Double.NaN)
	           {
	        	   System.out.println("significantWeight = "+significantWeight);
	        	   System.out.println("harmonicMean = "+harmonicMean);
	        	   
	           }
           }
           
          //-------------------- 
          //Self Weight is
          //-------------------
         
           if (n_i<50) 		selfWeight	= (n_i/50) * max;
           else			 	selfWeight  =  max;   
           
  
         //_______________________________________________________________
         // Do the summations
         //_______________________________________________________________

        // for all the common movies in train set
         for (Pair pair : ratings)         
         {
             rating1 = (double) MemHelper.parseRating(pair.a) - activeAvg;
             rating2 = (double) MemHelper.parseRating(pair.b) - targetAvg;
             
         /*   if(rating1==0) pearsonDeviationNotWorking++;
              if(rating2==0) pearsonDeviationNotWorking++;
         */    
             topSum += rating1 * rating2;
         
             bottomSumActive += Math.pow(rating1, 2);
             bottomSumTarget += Math.pow(rating2, 2);
         }
         
         //for all common movies in filled train set         
         ratings = filledMh.innerJoinOnMoviesOrRating(activeUser,targetUser, true);
         
         for (Pair pair : ratings)         
         {
             rating1 = (double) MemHelper.parseRating(pair.a) - activeAvg;
             rating2 = (double) MemHelper.parseRating(pair.b) - targetAvg;
                     
             topSum += rating1 * rating2;
         
             bottomSumActive += Math.pow(rating1, 2);
             bottomSumTarget += Math.pow(rating2, 2);
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
        	//return  functionResult * (n/amplifyingFactor); //amplified send 
        	
        }
        
        else
         //   return 1;			// why return 1:?????
        	return 0;			// So in prediction, it will send average back 
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
        int parta, partb, partc, partd, parte, n;
		double rating1;
		double rating2;
        
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
                rating1 = MemHelper.parseRating(pair.a);
            
            if(pair.b == 0)
                rating2 = d;
            else
                rating2 = MemHelper.parseRating(pair.b);

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
    	int amplifyingFactor =50;
        double bottomActive, bottomTarget, weight;
        LongArrayList ratings;
        
        ArrayList<Pair> commonRatings = mh.innerJoinOnMoviesOrRating(
                activeUser, targetUser, true);
        
        bottomActive = bottomTarget = weight = 0;

        // Find out the bottom portion for summation on active user
        // But what if we are having five fold?... a user may be there in different folds, with
        // Different movie....It is not gonna help
       
     /*   if (vectorNorms.containsKey(activeUser))        
        {
            bottomActive = vectorNorms.get(activeUser);
        }
       */
        
        if(2>3)
        {
        	
        }
        
        else         
        {
            ratings = mh.getMoviesSeenByUser(activeUser);
        
           if ((options & VS_INVERSE_USER_FREQUENCY) == 0)  //simple VS
            {
                for (int i = 0; i < ratings.size(); i++) 
                
                {
                    bottomActive += Math.pow(MemHelper.parseRating(ratings	// sqrt(sum(all movies seen by user(sq(vote))));
                            .getQuick(i)), 2);
                }
            }
            
            else //VS + IUF            
            {
                for (int i = 0; i < ratings.size(); i++) 
                
                {
                    bottomActive += Math.pow(frequencies.get(MemHelper
                            .parseUserOrMovie(ratings.getQuick(i)))
                            * MemHelper.parseRating(ratings.getQuick(i)), 2);
                }
            }
            
            bottomActive = Math.sqrt(bottomActive);
            vectorNorms.put(activeUser, bottomActive);
        }

        
        // Find out the bottom portion for summation on target user
      
        /* if (vectorNorms.containsKey(targetUser))        
        {
            bottomTarget = vectorNorms.get(targetUser);
        }
         */
        
        if(2>3)
        {
        	
        }
        
        else        
        {
            ratings = mh.getMoviesSeenByUser(targetUser);		//all the votes provided by this user
           
            if ((options & VS_INVERSE_USER_FREQUENCY) == 0)	//VS            
            {
                for (int i = 0; i < ratings.size(); i++)             
                {
                    bottomTarget += Math.pow(MemHelper.parseRating(ratings
                            .getQuick(i)), 2);
                }
            }
        
            else // VS + IUF
            {
                for (int i = 0; i < ratings.size(); i++)                 
                {
                    bottomTarget += Math.pow(frequencies.get(MemHelper
                            .parseUserOrMovie(ratings.getQuick(i)))
                            * MemHelper.parseRating(ratings.getQuick(i)), 2);
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
                weight += MemHelper.parseRating(pair.a) * MemHelper.parseRating(pair.b);
            }
        }
        
        else 
        { 
            for (Pair pair : commonRatings) 
            {
                weight += (frequencies.get(MemHelper.parseUserOrMovie(pair.a)) * MemHelper
                        .parseRating(pair.a))
                        * (frequencies.get(MemHelper.parseUserOrMovie(pair.b)) * MemHelper
                        .parseRating(pair.b));
            }
        }
        
        weight /= bottomActive * bottomTarget;
        
        //return weight;
        return (weight * (commonRatings.size()/amplifyingFactor));
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

