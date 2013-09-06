package netflix.memreader;

import cern.colt.list.IntArrayList;
import cern.colt.map.OpenIntIntHashMap;

public class dummyTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
	// TODO Auto-generated method stub
		System.out.println("Yes running!");

		IntArrayList userAlreadyThere = new IntArrayList();
		IntArrayList userAlreadyThere1 = new IntArrayList();
		OpenIntIntHashMap myMap = new OpenIntIntHashMap();
		
		
		for (int i=0;i<=10;i++)
		{	
			userAlreadyThere.add(i);
			userAlreadyThere1.add(i);
		    
		}
		
		System.out.println(userAlreadyThere);
		
		for (int i=15;i>=5;i--)
		{
			userAlreadyThere.add(i);
			userAlreadyThere1.add(i);
				
		}

		userAlreadyThere1.sort();
				
				
		for (int i=0;i<=userAlreadyThere1.size();i++)
		{	
			System.out.print(userAlreadyThere.getQuick(i)+"," + userAlreadyThere1.getQuick(i)+"===");					
			System.out.println(userAlreadyThere.indexOf(userAlreadyThere1.getQuick(i)));
		
		}  //userAlreadyThere.shuffle();
		

		
	}

}
