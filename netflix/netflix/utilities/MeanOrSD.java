package netflix.utilities;

public class MeanOrSD {

	
/***************************************************************************************************/
    
    /**
     * calculate Mean or SD of array of values
     * @param double[], values 
     * @param int, no of values
     * @param int, 0=mean and 1=sd
     * @return mean or sd
     */
	    
	 public double calculateMeanOrSD(double val[], int size, int whatToCalculate)
	 {
		 if(size ==1)
			 return val[0];
		 
		 double mean =0;
		 double sd =0;
		 double ans =0;
		 
		 //calculate mean
		 for (int i=0;i<size;i++)
		 {
			 mean +=val[i];
			// System.out.println("value="+ val[i]);
		 }
		 
			mean= mean/size;			//This is mean
			
		 //choose what to claculate based on flag
		 if(whatToCalculate ==0)//mean
			 ans = 	mean; 
			 
		 else //SD
		 {
			 for(int i=0;i<size;i++)
			 {
				 sd+= Math.pow((val[i] - mean), 2);
			 }
			 
			 if(size==1)
				 ans= Math.sqrt(sd);
			 else
				 ans= Math.sqrt(sd/(size-1));
		 }
		
		 return ans;
	 }
	 
/***************************************************************************************************/
	 
}
