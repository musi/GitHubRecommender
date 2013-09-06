
package netflix.utilities;

public class Pair 

{
	/*public int a;
	public int b;
	*/
	public long a;
	public long b;
	
	//check where it is called
	public double d1;
	public double d2;
	


	//V. Imp, we are using the same names (a,b in other files as well)
   public Pair(long a, long b)	
	{
		this.a= a;
		this.b = b;
	}
   

   public Pair(double d1, double d2)	
	{
		this.d1=d1;
		this.d2=d2;
	}
  
   
	
}
