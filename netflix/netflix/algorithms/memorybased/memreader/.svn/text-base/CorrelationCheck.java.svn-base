package netflix.algorithms.memorybased.memreader;

import java.io.Console;
import java.util.Scanner;

//Class which will simply check correlation between two vectors

public class CorrelationCheck 
{


/************************************************************************************************/	
	
	public double correlation (double a[], double b[])
	{
		double topSum=0;
		double bottomSumActive=0;
		double bottomSumTarget=0;
		
		for(int t=0;t<a.length;t++)
		{
		
			topSum += a[t] * b[t];	    
		    bottomSumActive += Math.pow(a[t], 2);
		    bottomSumTarget += Math.pow(b[t], 2);	
	
		}
	
	
	// This handles an emergency case of dividing by zero
	if (bottomSumActive != 0 && bottomSumTarget != 0)
	{ 
		double functionResult = (1 * topSum) / Math.sqrt(bottomSumActive * bottomSumTarget);  //why multiply by n?		 
		return  functionResult;
 
		
	}
	
	else
	//   return 1;			// why return 1:?????
		return 0;			// So in prediction, it will send average back 

	}
	
/************************************************************************************************/	
	
	public static void main (String args[])
	{
		double user1Ratings[] = new double [5];
		double user2Ratings[] = new double [5];
		Scanner in = new Scanner(System.in);
		
		CorrelationCheck myCorr = new CorrelationCheck();
		
		int loop = 0;
				
		while(true)
		{
			// Take Input
			System.out.println("Enter U1's Ratings = ");			
			for(int i=0;i<5;i++)
				user1Ratings[i] = in.nextDouble();			
			System.out.println("Enter U2's Ratings = ");			
			for(int i=0;i<5;i++)
				user2Ratings[i] = in.nextDouble();
			
			double r = myCorr.correlation(user1Ratings, user2Ratings);
			
			System.out.println("Corr =" + r);
			if (loop++ == 10) break;
		}	
		
	}
}
