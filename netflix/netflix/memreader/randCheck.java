package netflix.memreader;

import cern.colt.list.DoubleArrayList;
import cern.colt.map.OpenIntDoubleHashMap;
import cern.colt.map.OpenIntObjectHashMap;

public class randCheck {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		
		//To check what happend when one wants to retive the element that is not there
		//------------------------------------------------------
		OpenIntObjectHashMap myMap = new OpenIntObjectHashMap();
		
		DoubleArrayList m1 =  new DoubleArrayList ();
		m1.add(2.0);
		m1.add(3.0);	
		DoubleArrayList m2 =  new DoubleArrayList ();
		m2.add(2.0);
		m2.add(3.0);
		
		myMap.put(1, m1);
		myMap.put(2, m2);
		
		DoubleArrayList md = (DoubleArrayList) myMap.get(3);
		System.out.println("-->"+ myMap.get(3));
		System.out.println("-->"+ md.size());
	    //------------------------------------------------------
	}

}
