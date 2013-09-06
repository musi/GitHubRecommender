package netflix.utilities;

import cern.colt.list.DoubleArrayList;

public class dummyChecking {

	/**
	 * @param args
	 */
	
	public static void main(String[] args) 
	{
		
		double d =2;
		
		//System.out.println(d+ ", "+ Double.parseDouble(""));

		
		//DoubleArrayList, checking how values are stored in order or ?
		//Result: It Add in order
		DoubleArrayList one = new DoubleArrayList();
		DoubleArrayList two = new DoubleArrayList();
		
		for(int i=0;i<10;i++)
		{
			one.add(i);
			two.add(i+1);
		}
		
		for(int i=0;i<5;i++)
		{
			one.add(5);
			two.add(6);
		}
		
		for(int i=0;i<10;i++)
		{
			one.add(i/10.0);
			two.add((i+1)/10.0);
		}
		
		
		int size = one.size();
		
		for(int i=0;i<size;i++)
		{
			System.out.print(one.getQuick(i)+",");
		
		}
		
		System.out.println();
		for(int i=0;i<size;i++)
		{
					System.out.print(two.getQuick(i)+",");
		}
	}

}
