package netflix.algorithms.memorybased.rectree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import cern.colt.list.CharArrayList;
import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import cern.colt.list.ObjectArrayList;
import cern.colt.map.OpenIntDoubleHashMap;
import cern.colt.map.OpenIntObjectHashMap;

public class RandCheck 

{
	Random rand;
	
	RandCheck()
	{
		rand = new Random();
		
	}


//No. 1 Random integers


//Create two random number generators with the same seed

//-------------------------------------

  public static void main(String arg[])
 
 {

	  RandCheck r= new RandCheck();

	 // r.checkNow();
	// r.maxCheck();
	//  r.bitCheck();
	  
	  ArrayList a = new ArrayList();	  
	  System.out.println("size =" + a.size());

	  ///-----------------------------------------
	  //HashMap Clear checking	  
/*	  HashMap <String, Double>  myF = new HashMap<String, Double>();
	  myF.put("ss", 2.0);
	  myF.put("s3s", 2.0);
	  System.out.println(myF);	  
	  myF.clear();
	  System.out.println("after clearing");
	  System.out.println(myF);
	  myF.put("mm", 2.0);
	  myF.put("mmm", 2.0);	  
	  System.out.println(myF);					//looking al-right
	  */
	  ///-----------------------------------------
	  //ArrayList checking
	/*  
	  String dd1="aaaa";
	  String dd2="bbb";
	  
	  CharArrayList ca = new CharArrayList();
	  ObjectArrayList oa = new ObjectArrayList();
	  ObjectArrayList ob = new ObjectArrayList();
	  OpenIntObjectHashMap map = new OpenIntObjectHashMap();
	  
	  
	  oa.add(dd1);
	  oa.add(dd2);
	  
	  map.put(1, oa);
	  
	  for (int i=0;i<oa.size();i++)
	     System.out.println(" =" + oa.get(i));
	  
	  
	  ob = (ObjectArrayList)map.get(1);
	  ObjectArrayList oc = (ObjectArrayList)map.get(1);
	  
	  for (int i=0;i<oc.size();i++)
		     System.out.println(" =" + oc.get(i));
		  */
	  
	  //----------------------------------------- 
	  //check what happens if key is not there and u want to retrieve?
	  
	  OpenIntDoubleHashMap map = new OpenIntDoubleHashMap();
	  map.put(1, 5.0);
	  map.put(3, 6.0);
	  map.put(4, 7.0);
	  
	  
	  System.out.println(map.get(1));
	  System.out.println(map.get(100));
	  System.out.println(map.get(101));
	  System.out.println(map.get(102));
	  
	//-----------------------------------------
	  
/*	  int count =0;
	  int myCount=0;
	  
	  for (double d1=0;d1<=1.0;d1+=0.1)
	  {
		  for (double d2=0;d2<=1.0;d2+=0.1)
	  	{	 
			  for (double d3=0;d3<=1.0;d3+=0.1)
	  			{		  
				  //System.out.println((count++) + ":\t" + d1 + ",\t\t\t" +d2 + ",\t\t\t" + d3 );
				  
				  if(d1+d2+d3==1) 
					  System.out.println((myCount++) + ":\t" + d1 + ",\t\t\t" +d2 + ",\t\t\t" + d3 );
	  			}
	  	}
	 }
	  
	  
	  for (int d1=0;d1<=10;d1+=1)
	  {
		  for (int d2=0;d2<=10;d2+=1)
	  	{	 
			  for (int d3=0;d3<=10;d3+=1)
	  			{		  
				  //System.out.println((count++) + ":\t" + d1 + ",\t\t\t" +d2 + ",\t\t\t" + d3 );
				  
				  if(d1+d2+d3==10 && (d1!=0) &&(d2!=0) &&(d3!=0)) 
					  System.out.println((count++) + ":\t" + d1 + ",\t\t\t" +d2 + ",\t\t\t" + d3 );
	  			}
	  	}
	 }
	*/

/*   Map <String,Double> mapTFIDF = new HashMap<String, Double>();   
   mapTFIDF.put("1ll", 2.0);
   
   Set set = mapTFIDF.entrySet();
    Iterator i = set.iterator();

   while(i.hasNext()){
     Map.Entry me = (Map.Entry)i.next();
     String k = (String) me.getKey();
     System.out.println(k + " : " + me.getValue() );
   }
 
   */
	  //--------------------- 
	  // Sorting
	  // It sorts in the way that largest value goes to end (Ascending).	  
/*		OpenIntDoubleHashMap simMap = new OpenIntDoubleHashMap();
		
		simMap.put(10, -2);
		simMap.put(1000, -98);
		simMap.put(6, -10);
		simMap.put(3, 100);
		
		IntArrayList keys 	 = simMap.keys();
		DoubleArrayList vals = simMap.values();
		
		System.out.println("Before sort:");
		for (int i=0;i<simMap.size();i++)
		System.out.println(keys.get(i) + ", " + vals.get(i));
						
	    simMap.pairsSortedByValue(keys, vals);
	    
	    System.out.println("After sort:");
	     for (int i=0;i<simMap.size();i++)
		     System.out.println(keys.get(i) + ", " + vals.get(i));
   */
   
	  
	  //--------------------------------------------------------- 
	  // Checking the objectArrayList 's entry set --> getKey
	  

	  
/*	  IntArrayList myArr = new IntArrayList();
	  
	  myArr.add(2);
	  myArr.add(1);
	  myArr.add(2);
	  myArr.add(2);
	  myArr.add(2);
	  myArr.add(-2);
	  myArr.add(3);
	  myArr.add(3);
	  myArr.add(-3);
	  
	  for (int i=0;i<myArr.size();i++)
		  System.out.println(myArr.get(i));
 
*/
	  
	  
	  // Ceil and Floor
/*	  double d1=3.45;
	  double d2=3.5;
	  double d3=3.6;
	  
	  System.out.println(Math.ceil(d1));
	  System.out.println(Math.ceil(d2));
	  System.out.println(Math.ceil(d3));
	  
	  System.out.println(Math.floor(d1));
	  System.out.println(Math.floor(d2));
	  System.out.println(Math.floor(d3));
	  
	  System.out.println(Math.round(d1));
	  System.out.println(Math.round(d2));
	  System.out.println(Math.round(d3));
	  
	  */

	  //String parsing check
/*	  double r1= 4;
	  double r2= 1;
	  double r3= 5;
	  
	  String S = "" + r1;
	  System.out.println(S);	  
	  S = "" + r2;
	  System.out.println(S);
	  S = "" + r3;
	  System.out.println(S);
*/
	 /* String S = "1.0";
	  
	  System.out.println(Double.parseDouble(S) + "," +(Double.parseDouble(S) == 1.0));
	  */
	  }
  
  //---------------------
  public void bitCheck()
  {
	  long   l1 =0, l2, l3, l4;
	  int    t1 = 1175419;
	  
	  l1= t1;
	  double d1 =8;
	  short   b  = 990;
	  int mask1 = 0xFFFF0000;
	  long mask2 = (long)0xFFFFFFFFFFFF0000L;
      
	      
	  for (int i=1;i<10;i++)
	  {
		    b = (short) (b+12);
		    
		    d1= 0.34 + d1;
		   
		    l2 = l1<<16  ;
		    l3 = (l2 | b );
		    l4= ((l3 & mask2) >>16);
		    
		    System.out.print(b + ", "+ l1);
		    System.out.print(", "+ l2); 
		    System.out.print(", "+ l3);
		    System.out.print(", "+ l4);
		    System.out.print(", "+ (int)l4);
		    short ss= (short) (d1*100);
		    //System.out.println("" + d1 + " ," + (short)(ss) + ", " + (ss*1.0/100) );
		    System.out.println();
		    
		    
	  }
	  
	  int i = (int) ((0.8)*1);	  
	  System.out.println(i);
	  
  }

  //----------------------
  
  public void checkNow()
  
  {
	  
	  for (int i=0;i<10;i++)
	  {
		  	int m= rand.nextInt(10);			//so it repeats what it sent already
		  	System.out.println (m);
	  }
	  
		 
  }
  
  //----------------------
  
public void maxCheck()
  
  {
	 double distance;
     double min = -1.0;
     int minIndex = -1;

/*     for(int i = 0; i < 9; i++)        
     {
         distance = rand.nextDouble();
         
         if (i%2==0) distance = -distance;

         System.out.println("min = " + min);

         if(Math.abs(distance) > min)  // we wanna find maximum distance        
         {
             min = distance;
             minIndex = i;
             System.out.println(" updated min = " + min);
         }
     }
     
     System.out.println(" last min = " + min);
	*/
     
     
     OpenIntDoubleHashMap uidToWeight = new  OpenIntDoubleHashMap();
     
     for(int i = 0; i < 5; i++)        
     { uidToWeight.put(i, i*.8);
       
     }
     
     uidToWeight.put(6, -.8);
     uidToWeight.put(7, 100);
     uidToWeight.put(8, 50);
     
     
     IntArrayList al = uidToWeight.keys();
     
     for (int i=0;i<al.size();i++)
     {
    	 int k =al.get(i);
    	 double d = uidToWeight.get(k);
    	 
    	 System.out.println(k + ", " + d);
    	 
     }
     
     IntArrayList kk = uidToWeight.keys();
     DoubleArrayList dd = uidToWeight.values();
     
     uidToWeight.pairsSortedByValue(kk,dd);
     
	 System.out.println("______________________________________________");
    
     al = uidToWeight.keys();
     
     for (int i=kk.size()-1;i>=0;i--)
     {
    	 int k =kk.get(i);
    	 double d = dd.get(i);
    	 
    	 System.out.println(k + ", " + d);
    	 
     }
    
		 
  }
  
  
  
  
  
  
  
}

