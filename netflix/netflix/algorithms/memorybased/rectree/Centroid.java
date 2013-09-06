package netflix.algorithms.memorybased.rectree;

import java.util.*;

import netflix.memreader.*;
import netflix.utilities.*;
import cern.colt.list.*;
import cern.colt.map.*;

//This class makes K clusters giving seed from 1--K,
//Then It compute the sum and count for each cluster
//It also compute the distance from the centre of a cluster (uid) with a new user 

/************************************************************************************************/
class Centroid 
/************************************************************************************************/
{

    private OpenIntDoubleHashMap 	sum;
    private OpenIntDoubleHashMap 	deviatedSum;	 // contain the deviated (from respective average) sum for a movie
    private OpenIntIntHashMap 		count;
    private double 					average;		 // average of a cluster
    private double 					deviatedAverage; // deviated average of a cluster
    private double 					averageOfMovie;	 // average of an item in the cluster
    public int 						startingUid;

    
 
/************************************************************************************************/
    /**
     * Default constructor. Initializes
     * movies and average. 
     */
 
    public Centroid()     
    {
        sum 			= new OpenIntDoubleHashMap();
        deviatedSum		= new OpenIntDoubleHashMap();
        count 			= new OpenIntIntHashMap();    
        average 		= 0.0;
        averageOfMovie 	= 0.0;
        startingUid 	= -1;
    }

/************************************************************************************************/
    
    public Centroid(Centroid other)     
    {
        sum 		= other.cloneSum();
        count 		= other.cloneCount();
        average 	= other.getAverage();
        startingUid = -1;
    }

/************************************************************************************************/
    
    /**
     * Creates this centroid as a copy of an 
     * actual user. This is used when choosing
     * the random k initial seeds. 
     *
     * @param  uid     The user to use for this centroid. 
     * @param  helper  The MemHelper object containing
     *                 this user. 
     */
    
//so a centre contains all the movies seen by the seeded user (uid)
//against each user, it contains 
    // (mid, sumOverAllUsersinThisCluster(rating))
    // (mid, sumOverAllUsersinThisCluster(rated=1, not-rated =0))
    
 public Centroid(int uid, MemHelper helper)    
 {
	 	deviatedSum = new OpenIntDoubleHashMap();
        sum   		= new OpenIntDoubleHashMap();
        count 		= new OpenIntIntHashMap();
        startingUid = uid;

        LongArrayList mids = helper.getMoviesSeenByUser(uid);
        int mid;
		double rating;

     for(int j = 0; j < mids.size(); j++) 	//all the movies seen by this users       
      {
            mid 	= MemHelper.parseUserOrMovie(mids.get(j));
            rating  = MemHelper.parseRating(mids.get(j));
        
            deviatedSum.put(mid, rating- helper.getAverageRatingForUser(uid));
            sum.put(mid, rating);							
            count.put(mid, 1);
       }
        
        average = helper.getAverageRatingForUser(uid);	
    }
    
    // So in any cluster, we have the movies as key in sum and count  
    // sum(mid, rating)--> mid and all its rating (the above is used as seed only)
    // count (mid, 1++)-->mid and how many users have seen him in that cluster
    
/************************************************************************************************/
    
    public OpenIntDoubleHashMap cloneSum()     
    {
        return (OpenIntDoubleHashMap) sum.clone();
    }

/************************************************************************************************/
    
    public OpenIntDoubleHashMap cloneDeviatedSum()     
    {
        return (OpenIntDoubleHashMap) deviatedSum.clone();
    }
    
    
 /************************************************************************************************/
 
    public OpenIntIntHashMap cloneCount()    
    {
        return (OpenIntIntHashMap) count.clone();
    }

 /************************************************************************************************/
    
    /**
     * Gets this centroid's rating for the specified		
     * movie. 
     *
     * @param  mid The movie to get the rating for
     * @return The rating for movie mid
     */
 
    public double getRating(int mid)     
    {
//        System.out.println("Entering getRating " + mid);

        if(!count.containsKey(mid) || !sum.containsKey(mid))
            return 0.0;

//        System.out.println("In getRating: " + sum.get(mid) + " " + count.get(mid));

        return (sum.get(mid) / count.get(mid));
    }
    
/************************************************************************************************/
    
    /*Computes the average rating in this centroid, for a specific movie.
    * But this need to be the average of the deviated ratings rather than actual rating
    * Means, we first subtract the respective user average from his rating and add the ratings
    * then divide by the total number of users   
     */
    
    /**
     * Get the average deviated rating for a movie in a cluster
     *  
     * @param  mid The movie to get the rating for
     * @return The rating for movie mid
     */
    
     public double getDeviatedRating(int mid)    
     {

         if(!count.containsKey(mid) || !deviatedSum.containsKey(mid))
             return 0.0;

         averageOfMovie = (deviatedSum.get(mid) / count.get(mid));
                 
     	return averageOfMovie;
     }
    
 /************************************************************************************************/
 /**
  * get average of the whole cluster (counted over all movie)
  */
    public double getAverage()    
    {
        return average;
    }

 /************************************************************************************************/
    /**
     * get average of the whole cluster (counted over all movie)
     */
       public double getDeviatedAverage()    
       {
             return deviatedAverage;
             
       }


    
 /************************************************************************************************/  
 
    /** TESTING METHOD */
    
    public void printRatings()    
    {        
        IntArrayList keys = sum.keys();
        
        int mid;
        keys.sort();

        for(int i = 0; i < keys.size(); i++)        
        {
            mid = keys.get(i);
         //   System.out.println(mid + " " + sum.get(mid) + " " + count.get(mid));
        }

    }

 /************************************************************************************************/
    
    /**
     *
     * DOES NOT UPDATE AVERAGE!
     */

    // we add a user into a cluster, it means we add all its movies and ratings and update
    // sum and count as well
    
    public void addPoint(int uid, MemHelper helper)    
    {

        LongArrayList movies = helper.getMoviesSeenByUser(uid);
        int mid;
		double rating;

        for(int i = 0; i < movies.size(); i++)         
        {
            mid    = MemHelper.parseUserOrMovie(movies.get(i));
            rating = MemHelper.parseRating(movies.get(i));
            
            //count
            if(!count.containsKey(mid)) 
            {
                count.put(mid, 1);			//so it is (mid, no of times it has been seen)?
            }
            
            else 
            {
                count.put(mid, 1 + count.get(mid));
            }

            
            //sum
            if(!sum.containsKey(mid)) 
            {
                sum.put(mid, rating);
            }
            
            else 
            {
                sum.put(mid, rating + sum.get(mid));
            }
        }
    }

 /************************************************************************************************/
    /**
     * Note that this method does not make sure that 
     * the count and sum remain positive. Don't remove		//why not first check if that point is there?
     * a point that's not in the cluster.
     *
     * DOES NOT UPDATE AVERAGE!								//we can update it?	
     */

    public void removePoint(int uid, MemHelper helper)    
    {
        LongArrayList movies = helper.getMoviesSeenByUser(uid);
        int mid;
		double rating;

        for(int i = 0; i < movies.size(); i++)         
        {
            mid    = MemHelper.parseUserOrMovie(movies.get(i));
            rating = MemHelper.parseRating(movies.get(i));

            //This movie is no longer rated in the centroid, 
            //so remove it. It would probably be okay to leave
            //a 0 in the hash, but at this point I'm more concerned
            //with correctness than speed. 
        
            if(count.get(mid) - 1 <= 0) //This shows that this movie was only seen by this user (uid) ...so delete it
            {
                count.removeKey(mid);
                sum.removeKey(mid);					//sort hashes ???
            }
            
            else             
            {
                count.put(mid, count.get(mid) - 1);
                sum.put(mid, sum.get(mid) - rating);
            }
        }
    }


/************************************************************************************************/
    
    /**
     * Computes the average rating in this
     * centroid. 
     */

    public void findAverage()    
    {
        IntArrayList keys = sum.keys();		//sum = (mid, ratingzz)
        double avg = 0.0;

        if(keys.size() == 0)        
        {
            average = 0.0;
        }
        
        else        
        {
            for(int i = 0; i < keys.size(); i++)            
            {
                if(count.get(keys.get(i)) != 0)
                    avg += (sum.get(keys.get(i)) / count.get(keys.get(i)));	//ratings sum of the movie/total no of users who saw them
            }        
            average = avg / keys.size();	// Just like average of a list (where list contains average values)
        }

    }


 /************************************************************************************************/
        
        /**
         * Computes the average rating in this
         * centroid. 
         */

        public void findDeviatedAverage()    
        {
            IntArrayList keys = sum.keys();		//sum = (mid, ratingzz)
            double avg = 0.0;

            if(keys.size() == 0)        
            {
                deviatedAverage = 0.0;
            }
            
            else        
            {
                for(int i = 0; i < keys.size(); i++)            
                {
                    if(count.get(keys.get(i)) != 0)
                        avg += (deviatedSum.get(keys.get(i)) / count.get(keys.get(i)));	//ratings sum of the movie/total no of users who saw them
                }        
                
                deviatedAverage = avg / keys.size();	// Just like average of a list (where list contains average values)
            }
        }
        
        
 /************************************************************************************************/
    
    /**
     * 
     * @param  uid  The user to find the distance from. 
     * @param  helper  The MemHelper object containing uid. 
     *
     */
//     public double distance(int uid, MemHelper helper) {

//         int currMovie;
//         double rating1, rating2, topSum, bottomSumUser; 
//         double bottomSumCentroid, weight;

//         topSum = bottomSumUser = bottomSumCentroid = weight = 0.0;
        
//         double userAverage = helper.getAverageRatingForUser(uid);
//         IntArrayList userMovies = helper.getMoviesSeenByUser(uid);
       

//         for(int i = 0; i < userMovies.size(); i++) {
//             currMovie = userMovies.get(i);

//             if(sum.containsKey(MemHelper.parseUserOrMovie(currMovie))) {

//                 System.out.println(MemHelper.parseUserOrMovie(currMovie));

//                 rating1 = MemHelper.parseRating(currMovie) - userAverage;
//                 rating2 = getRating(MemHelper.parseUserOrMovie(currMovie)) 
//                     - average;

//                 topSum += rating1 * rating2;
//                 bottomSumUser += rating1 * rating1;
//                 bottomSumCentroid += rating2 * rating2;
//             }
//         }


//         // This handles an emergency case of dividing by zero
//         if(bottomSumUser != 0 && bottomSumCentroid != 0)
//             weight = topSum / Math.sqrt(bottomSumUser * bottomSumCentroid);
		
//         return weight;
//     }

/************************************************************************************************/

   // 
        
        
    public double distanceWithAverage (	int uid, 
    									int center, 
    									MemHelper helper)    
    {    	
	    
   	 int amplifyingFactor = 1;			//give more weight if users have more than 50 movies in common	 
   	 double functionResult = 0.0;
   	 
   	 double topSum, bottomSumActive, bottomSumTarget, rating1, rating2;
     topSum = bottomSumActive = bottomSumTarget = 0;
        
     double activeAvg = helper.getAverageRatingForUser(uid);
     double targetAvg = getDeviatedAverage();   
     
     LongArrayList myMovies = helper.getMoviesSeenByUser(uid);
     int moviesSeen = myMovies.size();
     
      for (int i=0;i<myMovies.size();i++)         
       {
    	  	int mid = (int) myMovies.getQuick(i);
            rating1 = (double) MemHelper.parseRating(mid) - activeAvg;
            rating2 = targetAvg;
            
            topSum += rating1 * rating2;
        
            bottomSumActive += Math.pow(rating1, 2);
            bottomSumTarget += Math.pow(rating2, 2);
        }
        
        
       if (bottomSumActive != 0 && bottomSumTarget != 0)
       {    	
       	
    	functionResult = (1 * topSum) / Math.sqrt(bottomSumActive * bottomSumTarget);  //why multiply by n?   	
       	
    	return  functionResult; //simple send    	
       	    	
       }
       
       else     
       	return 0;	
       
    	
    	
    }
    
 
/************************************************************************************************/
    
    
    //It is called for a specific centroid, we should not take the active user's info into account?
    
    public double distanceWithDefault(	int uid, 
    									double cliqueAverage, 
    									MemHelper helper)    
    {

        int 	currMovie, userIndex, centroidIndex, userMid, centroidMid;
        double 	rating1=0, rating2=0, topSum, bottomSumUser; 
        double 	bottomSumCentroid, weight;
    	double  divisionFactor = 20;				// If common movies are less than 30, 
    	int     totalCommonMovies = 0;
    	
  

        topSum = bottomSumUser = bottomSumCentroid = weight = 0.0;  //weight is initailized with zero, we will return it if any of the /{}{} the bottom sum is zero
        userIndex = centroidIndex = userMid = centroidMid = 0;
        
        //To do a sort merge join, both lists must be sorted. We know
        //userMovies is sorted (by MemReader), but we must sort clusterMovies.
        double userAverage 			= helper.getAverageRatingForUser(uid);
        LongArrayList userMovies 	= helper.getMoviesSeenByUser(uid);        
        IntArrayList centroidMovies = count.keys();
        centroidMovies.sort();			
        
        while(userIndex < userMovies.size() && 
              centroidIndex < centroidMovies.size())        
        {
            
            //System.out.println("\tin loop");

            userMid 	= MemHelper.parseUserOrMovie(userMovies.getQuick(userIndex));
            centroidMid = centroidMovies.getQuick(centroidIndex);

           //________________________________________________
            
            //Both the user and centroid rated the movie
            if(userMid == centroidMid)             
            {                
                //temp
//                int userRating = MemHelper.parseRating(userMovies.getQuick(userIndex));

//                System.out.println(userMid + " " + userRating + " | " 
//                + getRating(centroidMid));

            	//get deviation of the ratings
                rating1 = MemHelper.parseRating(userMovies.getQuick(userIndex))- userAverage;
                rating2 = getRating(centroidMid) - average;
                
             /*   if(rating1==0) {  
                	System.out.println("----------------------==-----------------------------------");
                    System.out.println("rating1= " + rating1 + " rating2= " + rating2);
                    System.out.println("user rating= " + MemHelper.parseRating(userMovies.getQuick(userIndex))
                    		+ ", user avg="+ userAverage);
                    System.out.println("user ID ="+ uid + ", saw movies= " + userMovies.size());
                                        
                  } */
                
                userIndex++; centroidIndex++;
                totalCommonMovies++;
            }
            
            
            //User rated movie, centroid didn't (Menas no one in the centroid has rated this movie yet)
            else if(userMid < centroidMid)            
            {
//                int userRating = MemHelper.parseRating(userMovies.getQuick(userIndex));
//                System.out.println(userMid + " " + userRating + " | --"); 

                rating1 = MemHelper.parseRating(userMovies.getQuick(userIndex))- userAverage;
                rating2 = cliqueAverage - average; 	//(global average - centroid average)
                     
            /*    if(rating1==0) {  
                    
                	System.out.println("-------------------user rating-------------------------------");
                	System.out.println("rating1= " + rating1 + " rating2= " + rating2);
                    System.out.println("user rating= " + MemHelper.parseRating(userMovies.getQuick(userIndex))
                    		+ ", user avg="+ userAverage);
                    System.out.println("user ID ="+ uid + ", saw movies= " + userMovies.size());
                        
                                            
                }
                */
                
                
                /*rating1=0;
            	rating2=0;
            	*/
                userIndex++;
                
            }
                        
            //Centroid rated movie, user didn't
            
            else             
            {
//             System.out.println(centroidMid + " --" + " | " 
//              + getRating(centroidMid));

                rating1 = cliqueAverage - userAverage;	//(global average - user average)
                rating2 = getRating(centroidMid) - average;
         
                
          /*      if(rating1==0) {  
                    System.out.println("rating1= " + rating1 + " rating2= " + rating2);
                    System.out.println("global avg= " + cliqueAverage+ ", user avg="+ userAverage);
                    System.out.println("---------------------------------------------------------");      
                    
                  }
          */
                /*
            		rating1=0;
          	  		rating2=0;
          */
                centroidIndex++;
            }
          
          //________________________________________________                  
            
            topSum += rating1 * rating2;
            bottomSumUser += rating1 * rating1;
            bottomSumCentroid += rating2 * rating2;
                         
        }
        
//		        System.out.println("userIndex: " + userIndex + 
//                         " centroidIndex: " + centroidIndex);

        
        int    tempMid;
        double tempRating;        
        

        //The sort-merge loop stops when one of the indices goes out 
        //of bounds. Here we take into account the movies left in the
        //other list. 
        //So If user has rated more movies than that of the centroid, then 
        //we take this into account and for centroid take global average and 
        //average of the centroid into account.        
   
        
        if(userIndex < userMovies.size()) 
        {
        	   // System.out.println("in if 1: " + bottomSumCentroid);

        	for(int i = userIndex; i < userMovies.size(); i++)            
            {
//                tempMid = MemHelper.parseUserOrMovie(userMovies.get(i));
//                tempRating = MemHelper.parseRating(userMovies.get(i));

//                      System.out.println(tempMid + " " + tempRating + " | --");


                rating1 = MemHelper.parseRating(userMovies.getQuick(i))- userAverage;
                rating2 = cliqueAverage - average;					
                
                // userIndex++;

//                System.out.println("\trating1: " + rating1 + " rating2: " + rating2);
//                System.out.println("topSum " + (rating1 * rating2));
                topSum += rating1 * rating2;
                bottomSumUser += rating1 * rating1;
                bottomSumCentroid += rating2 * rating2;
            }
        }
     
        
        else if(centroidIndex < centroidMovies.size())         
        {

//            System.out.println("in if 2: " + bottomSumCentroid);

            for(int i = centroidIndex; i < centroidMovies.size(); i++)            
            {
                
//                tempMid = centroidMovies.get(i);
//                tempRating = getRating(tempMid);
                
//                System.out.println(tempMid + " " +  "-- | " + tempRating);

                rating1 = cliqueAverage - userAverage;
                rating2 = getRating(centroidMovies.get(i)) - average;
//                centroidIndex++;

//                System.out.println("\trating1: " + rating1 + " rating2: " + rating2);

//                System.out.println("topSum " + (rating1 * rating2));

                topSum += rating1 * rating2;
                bottomSumUser += rating1 * rating1;
                bottomSumCentroid += rating2 * rating2;

            }        
            //System.out.println("Leaving if 2: " + bottomSumCentroid);
        }

     
        // This handles an emergency case of dividing by zero
        if(bottomSumUser != 0 && bottomSumCentroid != 0)        	
            weight = topSum / Math.sqrt(bottomSumUser * bottomSumCentroid);
    
        //System.out.println("topSum: " + topSum + "\nbottomSumUser " + bottomSumUser + "\nbottomSumCentroid " + bottomSumCentroid);
        
         //System.out.println("weight found is " + weight);
       
      //    return Math.abs(weight);       //why mod??
        
        /*if(weight ==0)
        {
        	 System.out.println("weight found is= " + weight);
        	 System.out.println("user saw movies= " + userMovies.size());
        	 System.out.println("centroid saw movies= " + centroidMovies.size());
        	 System.out.println("topSum= " + topSum + ", bottomSumUser= " + bottomSumUser + ", bottomSumCentroid= " + bottomSumCentroid);
        	 System.out.println("global avg= " + cliqueAverage+ ", centroid avg="+ average);
        	 System.out.println("-------------------------------------------------------------");
        }
        */
        
        // weight = weight * (totalCommonMovies/divisionFactor);
         return (weight);
    }
    
/*********************************************************************************************************/
    
    public double distanceWithDefaultVS	(	int uid, 
    										double cliqueAverage, 
    										MemHelper helper)    
	{
	
	int 	currMovie, userIndex, centroidIndex, userMid, centroidMid;
	double 	rating1 =0, rating2 =0, topSum, bottomSumUser; 
	double 	bottomSumCentroid, weight;
	double  divisionFactor = 50;				// If common movies are less than 30, 
	int     totalCommonMovies = 0;
	
	topSum = bottomSumUser = bottomSumCentroid = weight = 0.0;  //weight is initailized with zero, we will return it if any of the /{}{} the bottom sum is zero
	userIndex = centroidIndex = userMid = centroidMid = 0;
	
	//To do a sort merge join, both lists must be sorted. We know
	//userMovies is sorted (by MemReader), but we must sort clusterMovies.
	double userAverage 			= helper.getAverageRatingForUser(uid);
	LongArrayList userMovies 	= helper.getMoviesSeenByUser(uid);        
	IntArrayList centroidMovies = count.keys();
	centroidMovies.sort();			
	
	while(userIndex < userMovies.size() &&	centroidIndex < centroidMovies.size())        
	{
		
			//System.out.println("\tin loop");
			
			userMid 	= MemHelper.parseUserOrMovie(userMovies.getQuick(userIndex));
			centroidMid = centroidMovies.getQuick(centroidIndex);
		
		//________________________________________________
		
		//Both the user and centroid rated the movie
		if(userMid == centroidMid)             
		{                
			//temp
			//int userRating = MemHelper.parseRating(userMovies.getQuick(userIndex));
			
			//System.out.println(userMid + " " + userRating + " | " 
			//+ getRating(centroidMid));
			
			//get deviation of the ratings
			rating1 = MemHelper.parseRating(userMovies.getQuick(userIndex));
			rating2 = getRating(centroidMid) ;
			
			/*   if(rating1==0) {  
			System.out.println("----------------------==-----------------------------------");
			System.out.println("rating1= " + rating1 + " rating2= " + rating2);
			System.out.println("user rating= " + MemHelper.parseRating(userMovies.getQuick(userIndex))
			+ ", user avg="+ userAverage);
			System.out.println("user ID ="+ uid + ", saw movies= " + userMovies.size());
			            
			} */
			
			userIndex++; centroidIndex++;
			totalCommonMovies++;
		}
		
		
		//User rated movie, centroid didn't (Menas no one in the centroid has rated this movie yet)
		else if(userMid < centroidMid)            
		{
			//int userRating = MemHelper.parseRating(userMovies.getQuick(userIndex));
			//System.out.println(userMid + " " + userRating + " | --"); 
			
		    rating1 = MemHelper.parseRating(userMovies.getQuick(userIndex));
			rating2 = cliqueAverage;
			
			/*
			 if(rating1==0) {  
			
			System.out.println("-------------------user rating-------------------------------");
			System.out.println("rating1= " + rating1 + " rating2= " + rating2);
			System.out.println("user rating= " + MemHelper.parseRating(userMovies.getQuick(userIndex))
			+ ", user avg="+ userAverage);
			System.out.println("user ID ="+ uid + ", saw movies= " + userMovies.size());
			
			                
			}
			*/
			
			
			/*rating1=0;
			rating2=0;
			*/
			userIndex++;
			
		}
		
		//Centroid rated movie, user didn't
		
			else             
			{
				//System.out.println(centroidMid + " --" + " | " 
				//+ getRating(centroidMid));
				
		        rating1 = cliqueAverage ;	//(global average - user average)
				rating2 = getRating(centroidMid);
				
		/*		
				if(rating1==0) {  
				System.out.println("rating1= " + rating1 + " rating2= " + rating2);
				System.out.println("global avg= " + cliqueAverage+ ", user avg="+ userAverage);
				System.out.println("---------------------------------------------------------");      
				
				}
		*/		
			
				/*rating1=0;
				rating2=0;
				*/
				centroidIndex++;
			}
		
		
		//________________________________________________
		
		
		
		topSum += rating1 * rating2;
		bottomSumUser += rating1 * rating1;
		bottomSumCentroid += rating2 * rating2;
		
		
	}
	
	//System.out.println("userIndex: " + userIndex + 
	//" centroidIndex: " + centroidIndex);
	
	
	int    tempMid;
	double tempRating;        
	
	
	//The sort-merge loop stops when one of the indices goes out 
	//of bounds. Here we take into account the movies left in the
	//other list. 
	//So If user has rated more movies than that of the centroid, then 
	//we take this into account and for centroid take global average and 
	//average of the centroid into account
	
	
	
	if(userIndex < userMovies.size()) 
	{
	// System.out.println("in if 1: " + bottomSumCentroid);
	
		for(int i = userIndex; i < userMovies.size(); i++)            
		{
		//tempMid = MemHelper.parseUserOrMovie(userMovies.get(i));
		//tempRating = MemHelper.parseRating(userMovies.get(i));
		
		//System.out.println(tempMid + " " + tempRating + " | --");
		
		
		rating1 = MemHelper.parseRating(userMovies.getQuick(i));
		rating2 = cliqueAverage;
		
		//              userIndex++;
		
		//System.out.println("\trating1: " + rating1 + " rating2: " + rating2);
		//System.out.println("topSum " + (rating1 * rating2));
		topSum += rating1 * rating2;
		bottomSumUser += rating1 * rating1;
		bottomSumCentroid += rating2 * rating2;
		}
		
	}
	
	
	else if(centroidIndex < centroidMovies.size())         
	{
	
	//System.out.println("in if 2: " + bottomSumCentroid);
	
		for(int i = centroidIndex; i < centroidMovies.size(); i++)            
		{
		
		//tempMid = centroidMovies.get(i);
		//tempRating = getRating(tempMid);
		
		//System.out.println(tempMid + " " +  "-- | " + tempRating);
		
		rating1 = cliqueAverage ;
		rating2 = getRating(centroidMovies.get(i)) ;
		//centroidIndex++;
		
		//System.out.println("\trating1: " + rating1 + " rating2: " + rating2);
		
		//System.out.println("topSum " + (rating1 * rating2));
		
		topSum += rating1 * rating2;
		bottomSumUser += rating1 * rating1;
		bottomSumCentroid += rating2 * rating2;
		
		}
		
		
	//           System.out.println("Leaving if 2: " + bottomSumCentroid);
	
	}
	
	
	// This handles an emergency case of dividing by zero
	if(bottomSumUser != 0 && bottomSumCentroid != 0)        	
	weight = topSum / (Math.sqrt(bottomSumUser) * Math.sqrt(bottomSumCentroid));
	
	//System.out.println("topSum: " + topSum + "\nbottomSumUser " + bottomSumUser + "\nbottomSumCentroid " + bottomSumCentroid);
	
	//System.out.println("weight found is " + weight);
	
	//    return Math.abs(weight);       //why mod??
	
	/*if(weight ==0)
	{
	System.out.println("weight found is= " + weight);
	System.out.println("user saw movies= " + userMovies.size());
	System.out.println("centroid saw movies= " + centroidMovies.size());
	System.out.println("topSum= " + topSum + ", bottomSumUser= " + bottomSumUser + ", bottomSumCentroid= " + bottomSumCentroid);
	System.out.println("global avg= " + cliqueAverage+ ", centroid avg="+ average);
	System.out.println("-------------------------------------------------------------");
	}
	*/
	
	//if(totalCommonMovies<divisionFactor)
		weight = (totalCommonMovies/divisionFactor) * weight;   //devallue the weight
	//else;  //left unchanged
	
	return (weight);
	
	}
	
	
}
