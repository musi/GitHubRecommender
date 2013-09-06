package netflix.utilities;

public class WeightFunction {

	double r;
	double n;

	//---------------------------
	
	public WeightFunction()
	{
		r= 0;
		n=0;		
		
	}
	
	//---------------------------
	public static void main(String arg[])
	{
	
		WeightFunction wf = new WeightFunction();
		wf.checkFunction();
	}
	
	//---------------------------
	
	public void checkFunction()
	{
		
	
		
		r = 6;
		double max =0;
		double min =0;
		double w = 0;
		double newW = 0;
		double sw=0;
		
		for(int m=1;m<=10;m++)
		{
			w = -m/10.0;
			//w = m/10.0;
			
			System.out.println("---------------------");
			System.out.println("w="+w);
			System.out.println("---------------------");
			
			for(n=0;n<20;n+=2)
			{
				max = Math.max(n,r);
				min = Math.min(n,r);				
				
				//For negative strategy
				 //sw = (n) / (n + max + r);
				 sw = (n) / (max + r);
				// sw = n / (n + max );						
				// sw = (n+ min) / (n + max);				//Not logical	
				 //sw = (n+ min) / (n + max + r);			//Not logical
				//sw = (n+ min) / (2*(n + max ));
				//sw = (n+ min) / (Math.pow((n + max ),2));
				
				//previous used for neg
				//sw = (n+r)/(n);
				
				//Logs
				//sw =  Math.log10(w+2);
				//sw = n/(n+r) * ( Math.log10(w+2));
		 	     //sw = (n)/(n+max) * ( Math.log10(w+2));
				//sw = (n)/(r+max) * ( Math.log10(w+2));
				 //sw = ((n)/(10*(n+max))) * ( Math.log10(w+2));
		 	    
				
				 //constant				   
				  // sw = (w+1)/10;
				  //sw = (n/(n+r)) * (w+1)/10;			
				// sw = (n/(r)) * (w+1);
				
						//sw = (n/(n+r)) * (w);
				     //sw = (n/(n+r)) * (w+1);
				   
				//For Postive
				//sw = n/r; 
				
				newW =w * sw;
				//newW = sw;		
				System.out.print("n ="+n+"               w'="+newW +"\n");			
			}
			
		}//end outer for
	}
}
