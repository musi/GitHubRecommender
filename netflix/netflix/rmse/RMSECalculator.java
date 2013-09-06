package netflix.rmse;

import cern.colt.list.DoubleArrayList;
import netflix.db.Database;


/***********************************************************************************************************************/
public class RMSECalculator 
/***********************************************************************************************************************/
{	
    Database 	database;
    
    //---------------
    // MAE
    //---------------

    int 			numValues;
    double 			sumSquaredValues;
    double 			sumValues;
    String 			ratingTableName;
    
    //To calculate the variance 
    DoubleArrayList myErrorInMAE;
    DoubleArrayList myErrorInRMSE;
    DoubleArrayList myErrorInMSE;
    DoubleArrayList myErrorInROC;
    DoubleArrayList myErrorInF1;
    DoubleArrayList myErrorInPrecision;
    DoubleArrayList myErrorInRecall;
    
    //To check MAE for each user and then averaging
    int 			numValuesForEachUser;
    double 			sumSquaredValuesForEachUser;;
    double 			sumValuesForEachUser;
    
    double 			MAEFinal;
    double 			RMSEFinal;
    double 			MSEFinal;     
    int				totalUsersMAE;
    
    
    //---------------
    //Coverage
    //---------------
    
    int totalItemsAnswered;
    int totalItemsUnAnswered;
    int totalItems;
    
    //---------------
    //Top-N Metrics
    //---------------
    
    double topN_PrecisionForEachUser;			
    double topN_RecallForEachUser;			//Per User
    double topN_F1ForEachUser;
    
    double topN_Precision;					//For All Users (final answer)			
    double topN_Recall;
    double topN_F1;
    double totalUsersF1;
    
    //Possible outcome of the predicted value
    double topN_TP;	 //true  positive, if (actual== positive && prediction == positive} (Hit)
    double topN_TN;  //true  negative, if (actual== negative && prediction == positive}
    double topN_FN;  //false positive, if (actual== negative && prediction == negative}
    double topN_FP;  //false negative, if (actual== negative && prediction == positive} (Miss)    
    
    //Binary class 
    double topN_P;			//ACTUAL
    double topN_N;
    double topN_PPrime;		//PREDICTED
    double topN_NPrime;
    
    //---------------
    // ROC-4
    //---------------
    
      double totalUsersROC;
    
     //binary classifier (from actual ratings of each user)
      boolean actualCategory ;
 	  boolean predictedCategory ;
 	  
       //possible outcome of the predicted value
       double TP;	//true  positive, if (actual== positive && prediction == positive} (Hit)
       double TN;  //true  negative, if (actual== negative && prediction == positive}
       double FN;  //false positive, if (actual== negative && prediction == negative}
       double FP;  //false negative, if (actual== negative && prediction == positive} (Miss)
      
       
       //binary class 
       double P;			//ACTUAL
       double N;
       double PPrime;		//PREDICTED
       double NPrime;
       
       //true positive rate
       double TPR;										//For All Users
       double TPRForEachUser;							//For One user
       
       //false postive rate
       double FPR;
       double FPRForEachUser;
       
     //positive predicted value
       double PPV;
       double PPVForEachUser;
       
       //negative predicted value
       double NPV;
       double NPVForEachUser;
       
       //false Discovery rate
       double FDR;
       double FDRForEachUser;
       
     //Metthew correlation coff
       double MCC;
       double MCCForEachUser;
     
       //Accuracy
       double ACC;
       double ACCForEachUser;
     
     //Specificity
       double SPC;
       double SPCForEachUser;
       
     //F1
       double  F1;
       double  F1ForEachUser;
 
       //---------------
       //T Test
       //---------------
       double sumOfPred, sumOfActual, total, meanOfPred, meanOfActual, 
       sumOfSquareOfPred,sumOfSquareOfActual, squareOfTotalOfPred, squareOfTotalOfActual,
       standardDevOfPred, standardDevOfActual, standardDev, tTest;
       
       //---------------
       //Pair-T test
       //---------------
       double  pairTStandardDev, pairTTotal, pairTMeanOfDiff, pairTSumOfSquareOfDiff,
       pairTSumOfDiff, pairTStandardErrorOfMeanDiff,  pairTValue ;
       DoubleArrayList pairTDiffArray, pairTActual, pairTPred;
       boolean calculateAtSameTime;
  
       
/***********************************************************************************************************************/       

       /**
        *  constructor 
        */
    
   public RMSECalculator()    
    {
	   	//MAE
        database 			= null;
        numValues 			= 0;
        sumSquaredValues 	= 0.0;
        sumValues 			= 0.0;
        
        //For Variance
        myErrorInMAE		= new DoubleArrayList();        
        myErrorInRMSE 		= new DoubleArrayList();;
        myErrorInMSE 		= new DoubleArrayList();;
        myErrorInROC 		= new DoubleArrayList();;
        myErrorInF1 		= new DoubleArrayList();;
        myErrorInPrecision  = new DoubleArrayList();;
        myErrorInRecall 	= new DoubleArrayList();;
        
        
        numValuesForEachUser		= 0;
        sumSquaredValuesForEachUser	= 0;
        sumValuesForEachUser		= 0;
        
        
        MSEFinal 				= 0;
        RMSEFinal 				= 0;   
        MAEFinal 				= 0;
        totalUsersMAE 			= 0;
        
        //coverage
        totalItemsAnswered = totalItemsUnAnswered =	totalItems = 0;       
      	    
        //Top-N variables
        topN_PrecisionForEachUser = topN_RecallForEachUser = topN_F1ForEachUser=0;
        topN_Precision = topN_Recall = topN_F1=0;
        topN_TP = topN_TN = topN_FN = topN_FP = topN_P = topN_N = topN_PPrime = topN_NPrime =0.0;
        totalUsersF1 =0;
 
        //Roc
        totalUsersROC =0;
        TP = TN = FN = FP = P = N = PPrime = NPrime =0.0;        
        TPR = FPR = PPV = NPV = FDR = MCC = ACC = SPC = F1 =0.0;
        TPRForEachUser = FPRForEachUser = PPVForEachUser = NPVForEachUser=
        FDRForEachUser = MCCForEachUser = ACCForEachUser = SPCForEachUser = F1ForEachUser =0.0;
        
        actualCategory    = false;
   	    predictedCategory = false;
   	    
        //t test variables
        sumOfPred= sumOfActual= total= meanOfPred= meanOfActual= 
        sumOfSquareOfPred=sumOfSquareOfActual= squareOfTotalOfPred= squareOfTotalOfActual=
        standardDevOfPred= standardDevOfActual= standardDev= tTest =0;
        
        //Pair-t 
        pairTStandardDev= pairTTotal= pairTMeanOfDiff= pairTSumOfSquareOfDiff=
        pairTSumOfDiff=  pairTStandardErrorOfMeanDiff= pairTValue=0 ;
        pairTDiffArray = new DoubleArrayList();
        pairTActual = new DoubleArrayList();
        pairTPred = new DoubleArrayList();
        calculateAtSameTime = false;
        
    }
    
 
 /***********************************************************************************************************************/

											//---------------
											// MAE, RMSE etc
											//---------------   
   /**
    * Add MAE
    * @param realRating
    * @param prediction
    */
   
    public void add(double realRating, double prediction)		//it is being called from recommender class 
    {
        double delta = realRating - prediction;
        myErrorInMAE.add(delta);									//add absolute error
        sumValues+= Math.abs(delta);							//MAE
        sumSquaredValues += delta * delta;						//RMSE
        numValues++;
             
        //Add for Each user        
        sumValuesForEachUser+= Math.abs(delta);							//MAE
        sumSquaredValuesForEachUser += delta * delta;					//RMSE
        numValuesForEachUser++;
   
    }
    
/**
 * For each user, we add its MAE in the final MAE, then we will make average of all the user's MAE
 */
    
    public void addMAEOfEachUserInFinalMAE()		 
    {
    	if(numValuesForEachUser!=0)
    	{
	    	RMSEFinal +=  Math.sqrt((sumSquaredValuesForEachUser / numValuesForEachUser));
	    	MAEFinal  += (sumValuesForEachUser / numValuesForEachUser);
	    	MSEFinal  += (sumSquaredValuesForEachUser / numValuesForEachUser);
	    	
	    	totalUsersMAE++;			//keep track of the users we are making prediction
    	}
   
    }
     
 
/***********************************************************************************************************************/

    /**
     * GEt RMSE    
     */
    
    public double rmse()     
    {
        return Math.sqrt(sumSquaredValues / numValues);
    }

    public double rmseFinal()     
    {
        return RMSEFinal/totalUsersMAE;
    }


  //----------------------------

    /**
     * Get NMAE
     */
    
    // Normalized RMSE, with lowest and higHest rating index (i.e, 1-5, or 1-10)
    // As given in EigenTaste paper
    
    public double nmae_Eigen(double min, double max)
    {
    	return (mae()/(max-min));
    }
    
    public double nmae_EigenFinal(double min, double max)
    {
    	double temp = maeFinal();
    	return (temp/(max-min));
    }
    
    
//----------------------------
    /**
     * Get NMAE
     */
    // Normalized RMSE, with lowest and higehst rating index (i.e, 1-5, or 1-10)
    // As given in the ClusterKNN paper
    public double nmae_ClusterKNN(double min, double max)
    {
    	double d = 0;
    	
    	for (int i= (int)min;i<max;i++)
    		for (int j=(int)min;j<max;j++)
    		{
    			d+= Math.abs(i-j);
    		}
    		
    	   d/= (min*max);
    	   
    	return (mae()/d);
    }
    
    public double nmae_ClusterKNNFinal(double min, double max)
    {
    	double d = 0;
    	
    	for (int i= (int)min;i<max;i++)
    		for (int j=(int)min;j<max;j++)
    		{
    			d+= Math.abs(i-j);
    		}
    		
    	   d/= (min*max);
    	   
    	return (maeFinal()/d);
    }
    
    
  //----------------------------
    /**
     * Get MAE
     */
  public double mae()     
    {
        return (sumValues / numValues);
    }

  public double maeFinal()     
  {
      return (MAEFinal/totalUsersMAE);
  }
  

//-------------------------------
  /**
   * Get MSE
   */
  public double mse()     
    {
        return (sumSquaredValues / numValues);
    }
  
  public double mseFinal()     
  {
      return (MSEFinal/totalUsersMAE);
  }

  //-----------------------------
  /**
   * Get Variance of MAE of the individual errors
   */

  public double getVarianceInMAE()
  {
	  int size 			 = myErrorInMAE.size();
	  double MAE 		 = mae();
	  double variance_sq = 0;
	  double error 		 = 0;
	  
	  for(int i=0;i<size;i++)
	  {
		  error = myErrorInMAE.getQuick(i);
		  variance_sq+= Math.pow((error-MAE),2);  
		  
	  }
	  
	  //divide by size-1
	  variance_sq/= (size-1);
	  
	  return variance_sq;
	  
  }

  //-----------------------------
  /**
   * Get Error_Mean of MAE
   */

  public double getMeanErrorOfMAE()
  {
	  int size 			 = myErrorInMAE.size();	
	  double variance_sq = getVarianceInMAE();
	  return Math.sqrt(variance_sq)/Math.sqrt(size);		//variance/sqrt(size)
	  
  }
  

 /***********************************************************************************************************************/ 

								//-------------
								// TopN
								//-------------
							  
  /**
   * This versiion add the topN metrics, we have to make sure, how much elements we want to add
   * i.e. TopN, with N = variables, this can be called from the main program, with different values of
   * N. Here we do not have N, you wiol call it from main function with different values of N.
   */
  
  public void addTopN(double actual, double predicted, int classes, double userAvg)
  {

	//------------------
	  // FT, Bookcrossing
	  //------------------

	  
	  if(classes ==10)
	  {		
		  if (actual>=userAvg) actualCategory = true;    	//{7,8,9,10     = true;}		  
		  else actualCategory = false;			         	//{1,2,3,4,5,6  = flase;}	
		  
		  if (predicted>=userAvg) predictedCategory = true;		  
		  else predictedCategory = false;
	  }
	  
	  //------------------
	  // SML
	  //------------------
	  
	  else 
	  {		  
		  if (actual>=userAvg) actualCategory = true;  	   // {4,5   = true;}		  
		  else actualCategory = false;					   // {1,2,3 = false;}	
		  
		  if (predicted>=userAvg) predictedCategory = true;		  
		  else predictedCategory = false;
	  }

	  //increment respective values
	  if(actualCategory==true  		&& predictedCategory==true)		 {topN_TP++; topN_P++; topN_PPrime++;}
	  else if(actualCategory==true  && predictedCategory==false) 	 {topN_FN++; topN_P++; topN_NPrime++;}
	  
	  else if(actualCategory==false && predictedCategory==true)  {topN_FP++; topN_N++; topN_PPrime++; }
	  else if(actualCategory==false && predictedCategory==false) {topN_TN++; topN_N++; topN_NPrime++; }
	  
  }
  
  //----------------------------
  // number of relevant documents retrieved by a search divided by 
  // the total number of existing relevant documents
  
  //How much, I precited true from all actual true
  /**
   * return Top_N recall
   */
  
  
  public void AddTopNPrecisionRecallAndF1ForOneUser()
	{
		if((topN_TP + topN_FN)!=0)
			topN_RecallForEachUser = topN_TP/(topN_TP + topN_FN);
		if((topN_TP + topN_FP)!=0)
			topN_PrecisionForEachUser = topN_TP/(topN_TP + topN_FP);
		
		if((topN_PrecisionForEachUser + topN_RecallForEachUser) !=0)
			topN_F1ForEachUser = (2 * topN_PrecisionForEachUser * topN_RecallForEachUser) /
										(topN_PrecisionForEachUser + topN_RecallForEachUser);
		
		//Add for calculating the variance
		myErrorInPrecision.add(topN_RecallForEachUser);
		myErrorInRecall.add(topN_PrecisionForEachUser);
		myErrorInF1.add(topN_F1ForEachUser);
		
		//Add to the Final precisions
		topN_Recall += topN_RecallForEachUser;
		topN_Precision += topN_PrecisionForEachUser;
		topN_F1 += topN_F1ForEachUser;
		
		//all users, keep track of them
		totalUsersF1++;
	}
  
	public double getTopNRecall()
	{	
		return 	topN_Recall/totalUsersF1;	
	}

	//-----------------------------
	// number of relevant documents retrieved by a search divided by the 
	// total number of documents retrieved by that search
	
	//How much I predicted true from all predicted true
	  /**
	   * return Top_N Precision
	   */
	  
		public double getTopNPrecision()
		{	
			return topN_Precision/totalUsersF1;
		}
	
		//-----------------------------

	  /**
	   * return Top_N F1
	   */
		  
		public double getTopNF1()
		{		
			return topN_F1/totalUsersF1;
		}
  
  
		  //-----------------------------
		  /**
		   * Get Variance of Top_N of the individual errors
		   * @param int, 1=precision, 2=recall, 3=F1
		   */

		  public double getVarianceInTopN(int choice)
		  {
			  DoubleArrayList myList;
			  double MeanError;
			  
			  if(choice ==1){
				  myList = myErrorInPrecision;
				  MeanError = getTopNPrecision();
			  }
			  else if(choice ==2){
				  myList = myErrorInRecall;
				  MeanError = getTopNRecall();
			  }
			  else{
				  myList = myErrorInF1;
				  MeanError = getTopNF1();
			  }
				  
			  int size 			 = myList.size();
			  
			  double variance	 = 0;
			  double error 		 = 0;
			  
			  for(int i=0;i<size;i++)
			  {
				  error = myList.getQuick(i);
				  variance+= Math.pow((error-MeanError),2);				  
			  }
			  
			  //divide by size-1
			  variance/= (size-1);			  
			  return variance;
			  
		  }

		  //-----------------------------
		  /**
		   * Get Error_Mean of top-N
		   *  @param int, 1=precision, 2=recall, 3=F1
		   */

		  public double getMeanSDErrorOfTopN(int choice)
		  {
			  DoubleArrayList myList;
			  
			  if(choice ==1)
				  myList = myErrorInPrecision;
			  else if(choice ==2)
				  myList = myErrorInRecall;
			  else
				  myList = myErrorInF1;
			  
			  int size = myList.size();			  
			  double variance= getVarianceInTopN(choice);
			  double SD = Math.sqrt(variance); 
			  return SD/Math.sqrt(size);		//variance/sqrt(size)
		  }
		  
/***********************************************************************************************************************/
  
										//-------------
										//    Roc
										//-------------
/**
 *   The decision factor is the User average, as taking 1,2,3 as noise is not good. As
 *   One user may rate all movies low
 */
  
  public void ROC4(double actual, double predicted, int classes, double userAvg)
  {
	 	  
	  //------------------
	  // FT, Bookcrossing
	  //------------------
	  
	  if(classes ==10)
	  {
		
		  if (actual>=userAvg) actualCategory = true;    //{7,8,9,10     = true;}		  
		  else actualCategory = false;			         //{1,2,3,4,5,6  = flase;}	
		  
		  if (predicted>=userAvg) predictedCategory = true;		  
		  else predictedCategory = false;
	  }
	  
	  //------------------
	  // SML
	  //------------------
	  
	  else 
	  {
		  
		  if (actual>=userAvg) actualCategory = true;   //{4,5   = true;}		  
		  else actualCategory = false;			  //{1,2,3 = false;}	
		  
		  if (predicted>=userAvg) predictedCategory = true;		  
		  else predictedCategory = false;
	  }
	  
	  // we will check only positive case, by default they should be false
	  // Four possible cases
	  // when prediction is true , PPrime = true, when actual is true, P=true
	  if(actualCategory==true  		&& predictedCategory==true)		 {TP++; P++; PPrime++;}
	  else if(actualCategory==true  && predictedCategory==false) 	 {FN++; P++; NPrime++;}
	  
	  else if(actualCategory==false && predictedCategory==true)  {FP++; N++; PPrime++; }
	  else if(actualCategory==false && predictedCategory==false) {TN++; N++; NPrime++; }
	  
  }
	
   
 
  //--------------------------------------------
  // We already have computed Roc for one user, what we do afterwards, we
  // Cstore it, and then we can just make simple average over all users
  public void addROCForOneUser()
  {
	   totalUsersROC++;
	  
	  //TPR = TP/ P = TP / (TP + FN)
	  	if(P!=0)
	  		TPRForEachUser = TP/ (P);	  	
	  	TPR += TPRForEachUser;
	  	
	  	myErrorInROC.add(TPRForEachUser);
	  	
	  //FPR = FP / N = FP / (FP + TN)
	  	if(N!=0)
	  		FPRForEachUser = FP / (N);
	  	FPR += FPRForEachUser;
	  	
	  // ACC = (TP + TN) / (P + N)	
	  	if((P+N)!=0)
	  		ACCForEachUser = (TP + TN) / (P + N);
	  	ACC += ACCForEachUser;
	  	
	  // SPC = TN / N = TN / (FP + TN) = 1- FPR	
	  	if(N!=0)
	  		SPCForEachUser = TN / N;
	  	SPC += SPCForEachUser;
	  	
	  //PPV = TP / (TP + FP);
	  	if((TP+FP)!=0)
	  		PPVForEachUser = TP / (TP + FP);
	  	PPV += PPVForEachUser;
	  	
	  //NPV = TN / (TN + FN)
	  	if((TN+FN)!=0)
	  		NPVForEachUser = TN / (TN + FN);
	  	NPV  += NPVForEachUser ;
	  	
	  //FDR = FP / (FP +TP)
	  	if((FP+TP)!=0)
	  		FDRForEachUser = FP / (FP +TP);
	  	FDR += FDRForEachUser;
	  	
	  //MCC
	  	if(((P) *(N)* (PPrime) * (NPrime))!=0)
	  			MCCForEachUser = ((TP * TN) - (FP * FN)) / Math.sqrt((P) *(N)* (PPrime) * (NPrime));
	  	MCC += MCCForEachUser;
	  	
	  //F1 (2* Precision * Recall) / (Precision+Recall)
	  	if((PPV + TPR)!=0)
	  		F1ForEachUser = 2 * (PPV * TPR)/(PPV + TPR);  	
	  	F1 += F1ForEachUser;
	  	
	  	
	
	  
	  
	  
  
  
  }
  
 
  
  
  //get ROC related values
  
  //TPR (Sensitivity, recall, Hitrate)
  public double getSensitivity ()   // needed for graph  
  {
	  
	  return TPR/totalUsersROC;
  }
  
//FPR (fall out) = 1-specificity
  public double getFalsePositiveRate () // needed for graph  
  {
	  
	  return FPR/totalUsersROC;
  }
  
  //TPR determines a classifier or a diagnostic test performance on classifying positive instances 
  //correctly among all positive samples available during the test. 
  //FPR, on the other hand, defines how many incorrect positive results occur among all negative
  //samples available during the test.
  
  /*A ROC space is defined by FPR and TPR as x and y axes respectively, which depicts relative    
   trade-offs between true positive (benefits) and false positive (costs). Since TPR is 
   equivalent with sensitivity and FPR is equal to 1 - specificity, the ROC graph is sometimes 
   called the sensitivity vs (1 - specificity) plot. Each prediction result or one instance of a 
   confusion matrix represents one point in the ROC space */
  

//ACC
  public double getAccuracy ()  
  {
	  
	  return ACC/totalUsersROC;
  }
  

//SPC (true negative rate) --> TNR = (1- FNR) --> FNR = 1- TNR = 1- speicificty
  public double getSpecificity ()  
  {
	  
	  return SPC/totalUsersROC;
  }
  

//PPV (Precision)
  public double getPositivePredictedvalue ()  
  {
	  
	  return PPV/totalUsersROC;
  }


//NPV
  public double getNegativePredictedValue ()  
  {
	  
	  return NPV/totalUsersROC;
  }

//FDR
  public double getFalseDiscoveryrRate ()  
  {
	  
	  return FDR/totalUsersROC;
  }

 
//MCC 
  public double getMetthewsCorrCoff ()
  {
	  
	  return MCC/totalUsersROC;
  }

 //F1
  public double getF1()
  {	  
	  return F1/totalUsersROC;
  }
  
  //-----------------------------
  /**
   * Get Variance of ROC of the individual errors
   * 
   */

  public double getVarianceInROC()
  {
	  int size 			 = myErrorInROC.size();
	  double MeanError	 = getSensitivity();
	  double variance 	 = 0;
	  double error 		 = 0;
	  
	  for(int i=0;i<size;i++)
	  {
		  error = myErrorInROC.getQuick(i);
		  variance+= Math.pow((error-MeanError),2);				  
	  }
	  
	  //divide by size-1
	  variance/= (size-1);			  
	  return variance;
	  
  }

  //-----------------------------
  /**
   * Get Error_Mean of ROC
   *   
   */

  public double getMeanSDErrorOfROC()
  {
	  int size  = myErrorInROC.size();	  
	  
	  double variance= getVarianceInROC();
	  double SD = Math.sqrt(variance); 
	  return SD/Math.sqrt(size);		//variance/sqrt(size)
  }
  
  
    
/***********************************************************************************************************************/  
								     //---------
									 // Pair-T
									 //---------
  //This is required to find the diff beween two values
  
  /**
   * Add to pair t Test, values are: 
   * @param double prediction, 
   * @param double actual
   * 
   */
    public void addToPairT(double prediction, double actual)
    {	  
      double diff = prediction- actual;						//post - pre
      pairTDiffArray.add(diff);
      pairTSumOfDiff+=diff;									//sum of square 
      pairTSumOfSquareOfDiff+=Math.pow(diff,2);				//sum of square 	  
  	  pairTTotal++;											//total
  	  calculateAtSameTime= true;
   }
    
  //----------------------
    /** add prediction (post)
     * @param prediction
     */
      public void addPredToPairT(double prediction)
      {	  
    	  pairTPred.add(prediction);	 
    	  pairTTotal++;										//total;  
     }

    //----------------------
    /**
     * add actual (pre)
     * @param actual
     */
      public void addActualToPairT(double actual)
      {	  
    	  pairTActual.add(actual);
    	
    	      	  
     }
      //----------------------
/**
 * 
 * @return pair-t value
 */
    
    public double getPairT()
    {	
    	     	    
    	//If we are not sending predictions and actual at the same time, then we have to first find diff and means etc
    	if(calculateAtSameTime==false)
    	{
    		int total = pairTPred.size();
    		for(int i=0;i<total;i++)
    		{
    			double diff = pairTPred.getQuick(i) - pairTActual.getQuick(i);		//diff
    			pairTDiffArray.add(diff);											//add to array	
    			pairTSumOfDiff+=diff;												//sum of diff
    			
    		}//end for    		
    	} //end if  	
    	
    	//System.out.println("diff="+ pairTSumOfDiff);
    	
    	pairTMeanOfDiff = pairTSumOfDiff/pairTTotal;
    	int size = pairTDiffArray.size();
    	double sum = 0;
    	
    	//go through all the values in the diff array and find the s.d    	
    	for (int i=0;i<size;i++)
    	{
    		double val =  pairTDiffArray.getQuick(i);
    		sum +=  Math.pow((val - pairTMeanOfDiff),2);
    	}
    	
    	 pairTStandardDev = Math.sqrt(sum/(pairTTotal-1));
    	 pairTStandardErrorOfMeanDiff = pairTStandardDev/Math.sqrt(pairTTotal);    	 
    	 pairTValue = Math.abs(pairTMeanOfDiff)/pairTStandardErrorOfMeanDiff;
    	 
    	 return pairTValue;
    	
    }

    
/***********************************************************************************************************************/
										 //---------
										 // T-Test
										 //---------
										    
  /**
 * Add to t Test, values are: 
 * @param double prediction, 
 * @param double actual
 * 
 */
  public void addToTTest(double prediction, double actual)
  {	  
	  sumOfPred+=prediction;							//sum
	  sumOfActual+=actual;
	  sumOfSquareOfPred += Math.pow(prediction, 2);		//sum of square
	  sumOfSquareOfActual += Math.pow(actual, 2);
	  total++;											//total
 }

//----------------------
/** add prediction
 * @param prediction
 */
  public void addPredToTTest(double prediction)
  {	  
	  sumOfPred+=prediction;							//sum	  
	  sumOfSquareOfPred += Math.pow(prediction, 2);		//sum of square	  	  			
 }

//----------------------
/**
 * add actual
 * @param actual
 */
  public void addActualToTTest(double actual)
  {	  
	  sumOfActual+=actual;								//sum
	  sumOfSquareOfActual += Math.pow(actual, 2);		//sum of square
	  total++;											//total
	  
 }
//----------------------
  /**
   * Add to pair t Test, values are: 
   * @return double,  t test value 
   * 
   */
    public double getTTest()
    {	  	 
    		meanOfPred = sumOfPred /total;											//mean
	    	meanOfActual = sumOfActual /total;
	    	squareOfTotalOfPred = Math.pow(sumOfPred,2);							//square of total
	    	squareOfTotalOfActual = Math.pow(sumOfActual,2);
	    	
	    	double mean_squareOfTotalOfActual = squareOfTotalOfActual/total;		//mean_squareOfTotalOfActual
	    	double mean_squareOfTotalOfPred = squareOfTotalOfPred/total;
	    	
	    	double sdSquareActual =  sumOfSquareOfActual - mean_squareOfTotalOfActual;
	    	double sdSquarePred =  sumOfSquareOfPred - mean_squareOfTotalOfPred;
	    	
	    	standardDevOfActual = sdSquareActual/(total-1);
	    	standardDevOfPred = sdSquarePred/(total-1);
	    	
	    	double standardDevSquare = (standardDevOfActual/total) + (standardDevOfPred/total);
	    	standardDev =  Math.sqrt(standardDevSquare);    	
	    	tTest = Math.abs((meanOfActual - meanOfPred))/standardDev;

    	return tTest;
    	
     }  

/***********************************************************************************************************************/
							    	//-----------------
							    	// Reset variables
							    	//-----------------
							    
  /**
 * Reset MAE related variables
 */
  public void resetValues()  
  {
	  numValues 			= 0;
      sumSquaredValues 		= 0.0;
      sumValues 			= 0.0;
      totalItemsAnswered	= totalItemsUnAnswered =totalItems =0;     	  
  }

  public void resetMAEForEachUser()
  {	          
      sumValuesForEachUser		  = 0;
      sumSquaredValuesForEachUser = 0;	  
      numValuesForEachUser		  = 0;
  }
  
  public void resetFinalMAE()
  {
	  totalUsersMAE	= 0;
      MAEFinal 		= 0.0;
      RMSEFinal		= 0.0;
      MSEFinal 		= 0.0;
      
      myErrorInMAE.clear();
  }
  
  //-------------------
  /**
   * Reset ROC related vars
   */
  //reset after we process one user?
  public void resetROCForEachUser()
  {
	      actualCategory    = false;
	 	  predictedCategory = false;
	 	  
		  TP = TN = FN = FP = P = N = PPrime = NPrime = 0.0;
		  TPRForEachUser = FPRForEachUser = PPVForEachUser = NPVForEachUser = 
		  FDRForEachUser = MCCForEachUser = ACCForEachUser = SPCForEachUser = 0.0;   
  }
  
  public void resetFinalROC()
  {
	  TPR = FPR = PPV = NPV = FDR = MCC = ACC = SPC = 0.0;
	  totalUsersROC =0;
	  
	  myErrorInROC.clear();
  }
  
  //-------------------
  /**
   * Reset topN related vars
   */
  //We have to reset them, after we start processing each N.
  //Good idea is to store Top_50; and loop it from 1-50 (10, 20, 30, 40, 50) etc.
  //And after getting metric results after each value of N, reset it.
  
  public void resetTopNForOneUser()
  {
	  topN_PrecisionForEachUser = topN_RecallForEachUser = topN_F1ForEachUser=0;
      topN_TP = topN_TN = topN_FN = topN_FP = topN_P = topN_N = topN_PPrime = topN_NPrime =0.0;
      
  }
  
  
  public void resetFinalTopN()
  {
	  topN_Precision = topN_Recall = topN_F1=0;    
	  totalUsersF1 =0;
	  
	  myErrorInPrecision.clear();
	  myErrorInRecall.clear();
	  myErrorInF1.clear();
  }
  
  
  
  //-------------------  
  
  /**
   *  Reser All pair-t varaibles
   */
  public void resetPairT()
  {
	   pairTStandardDev= pairTTotal= pairTMeanOfDiff= pairTSumOfSquareOfDiff=
	   pairTSumOfDiff=  pairTStandardErrorOfMeanDiff= pairTValue=0 ;
	   pairTDiffArray.clear();
	   pairTActual.clear();
	   pairTPred.clear();
	   calculateAtSameTime = false;
  }

  //-------------------
  
  public void resetPairTPrediction()
  {	  	   	
	    pairTPred.clear();    	
	    pairTDiffArray.clear();	
	    pairTSumOfDiff =0;		
	    pairTSumOfSquareOfDiff=0; 	  
	    
	    //We have to reset it zero, bcuz, we are adding predictions from diff approaches
	    pairTTotal = 0;
	    calculateAtSameTime = false;
    		
  }
  
  //-------------------
/**
 * Reset all -t values
 */
  public void resetTTest()
  {
	  sumOfPred= sumOfActual= total= meanOfPred= meanOfActual= 
	  sumOfSquareOfPred=sumOfSquareOfActual= squareOfTotalOfPred= squareOfTotalOfActual=
	  standardDevOfPred= standardDevOfActual= standardDev= tTest =0;
	         
  }
  
  //-------------------
  /**
   * Reset pair-t test prediction only
   */
  public void resetTTestPred()
  {
	  sumOfPred=  meanOfPred=  sumOfSquareOfPred= squareOfTotalOfPred= standardDevOfPred =0;
	         
  }
  
/***********************************************************************************************************************/
  
									  //-------------
									  // Coverage
									  //-------------
  
  /**
   * add the coverage, increase the count of items this algorithm was able to answer
   */
  
  public void addCoverage(double r)
  {
	   if (r!=0)
		   totalItemsAnswered++;
	   else 
		   totalItemsUnAnswered++;		//Algo was unable to answer this item
	   
	   totalItems++;	  
  }
  

//---------------------------
  
  /**
   * add the coverage, increase the count of items this algorithm was able to answer
   */
  
  public double getItemCoverage()
  {
	   if (totalItemsAnswered!=0)
		   return ( ((totalItemsAnswered *1.0)/ totalItems) *100);  // answered/totalItems
	   
	   return 0;
  }
    
}  
/***********************************************************************************************************************/  


