package netflix.weka;

import java.io.IOException;

// Just a class to Dummyly check diff things
public class dummyCheck 
{
	double A[]= {5,4,3,5,5};
	double B[]= {2,1,1,1,4};
	//double B[]= {1,1,1,1,1};
	
/**********************************************************************************************/

	public dummyCheck()
	{
		//A = new double[5];
		//B = new double[5];
		
	}

/**********************************************************************************************/

	public void cosine()
	{
		// give values
		/*for(int i=0;i<4;i++)
		{
			A[i] = 5;
			if(i==0) B[i] = 5;
			else B[i] = 5;
			
			System.out.println("A="+A[i]);
			System.out.println("B="+B[i]);
		}*/
		
		//calculate the cosine
		double cos =0;
		double num =0, den1 =0, den2=0;
		for(int i=0;i<3;i++)			 //for two common items
		{
		  num += A[i] * B[i];
		  den1 += Math.pow(A[i],2);
		  den2 += Math.pow(B[i],2);
			
		}
		
		System.out.println("num="+num);
		System.out.println("den1="+den1);
		System.out.println("den2="+den2);
		System.out.println("sqrt(den1)="+Math.sqrt(den1));
		System.out.println("sqrt(den2)="+Math.sqrt(den2));
		
		double ans = num/(Math.sqrt(den1) * Math.sqrt(den2));		
		
		System.out.println("ans="+ans);
		System.out.println("------------------------------------------------");
	}
	
	
/*********************************************************************************************
 * @throws IOException 
 */
 	
// Alpha and Beta
	
	public void parameters() throws IOException
	{
		double alpha =0;
		double beta =0;
		
		System.out.println("press....");
		System.in.read();
		
		for(int i=0;i<10;i++)
		{
			for(int j=0;j<10;j++)
			{
				if(i+j==10)
				{
					System.out.println(i + ", "+ j);
				}
			}
		}
	}
	
	
	
/**
 * @throws IOException ********************************************************************************************/
	
	public void PCC() throws IOException
	{
		// give values
		double avg1=0, avg2=0;
		for(int i=0;i<3;i++)
		{
		  avg1+=A[i];
		  avg2+=B[i];			
		}
		
		avg1/=3;
		avg2/=3;
		
		System.out.println("A Avg="+avg1);
		System.out.println("B Avg="+avg2);
		
		//calculate the cosine
		double pcc =0;
		double num =0, den1 =0, den2=0;
		for(int i=0;i<2;i++)			 //for two common items
		{
		  num += (A[i]-avg1) * (B[i]-avg2);
		  den1 += Math.pow((A[i]-avg1),2);
		  den2 += Math.pow((B[i]-avg2),2);
			
		}
		
		
		System.out.println("num="+num);
		System.out.println("den1="+den1);
		System.out.println("den2="+den2);
		System.out.println("sqrt(den1)="+Math.sqrt(den1));
		System.out.println("sqrt(den2)="+Math.sqrt(den2));
		
		double ans = num/(Math.sqrt(den1) * Math.sqrt(den2));		
		
		System.out.println("ans="+ans);
	}
	
/**
 * @throws IOException ********************************************************************************************/

	public static void main (String args[]) throws IOException
	{
		dummyCheck dm = new dummyCheck();
	/*	dm.cosine();
		dm.PCC();*/
		dm.parameters();
		
	}

}
