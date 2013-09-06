package netflix.algorithms.modelbased.svd;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import cern.colt.map.OpenIntDoubleHashMap;

public class Hi {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		OpenIntDoubleHashMap	midToPredictions    = new OpenIntDoubleHashMap();   
		
		midToPredictions.put(1, 3.0);
		midToPredictions.put(1, 4.0);
		midToPredictions.put(1, 5.0);
		midToPredictions.put(1, 1.0);
		
		//sort the pairs (ascending order)
		IntArrayList keys = midToPredictions.keys();
		DoubleArrayList vals = midToPredictions.values();        		
		midToPredictions.pairsSortedByValue(keys, vals);
		
		int movSize = midToPredictions.size();
		
		for(int x=0;x<movSize;x++)
		{
			
			 int mov = keys.getQuick(x);
			 double  pred = vals.getQuick(x);
				 
			System.out.println("key="+mov+", val="+pred);
		}
		
		
		System.out.println("hi    ");
	}

}
