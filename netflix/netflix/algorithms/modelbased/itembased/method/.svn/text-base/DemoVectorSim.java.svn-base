package netflix.algorithms.modelbased.itembased.method;

import java.util.ArrayList;

import netflix.memreader.MemHelper;
import netflix.algorithms.modelbased.reader.DataReader;
import netflix.algorithms.modelbased.reader.DataReaderFromMem;
import netflix.utilities.Pair;
import cern.colt.list.IntArrayList;
import cern.colt.list.LongArrayList;

public class DemoVectorSim implements SimilarityMethod 
{
	
/******************************************************************************************************/
	//total 19 genres (0 = unknown) , lenght of vector
	
	/**
	 * @author Musi
	 * @param  dataReader object, active movie, target movie
	 * @return  demographic sim between two mobvies 
	 */
	
    public double findSimilarity(DataReader dataReader, int mid1, int mid2, int version)    
    {
    	//System.out.println("came in vector sim for demo" +" total genre size =" + dataReader.getGenreSize());
    	
    		
        double bottomActive = 0, bottomTarget = 0, bottom =0;
        double top = 0;
        double weight = 0;  
        int gActive=0,gTarget=0;  
        int match = 0;
        int dA=1, dT=1;				//normalizing factors
        
        
        LongArrayList genreActive = dataReader.getGenre(mid1);
        LongArrayList genreTarget = dataReader.getGenre(mid2);
               
        int  sizeActive = genreActive.size();
        int  sizeTarget = genreTarget.size();
        
        //dA= sizeActive;
        //dT=sizeTarget;
        
       // if(sizeActive>0) System.out.println("size A =" +sizeActive);
       // if(sizeTarget>0) System.out.println("size T =" +sizeTarget);
        
      if(sizeActive!=0 && sizeTarget!=0)
      {  
        for (int i=0; i<sizeActive; i++)  			//vector of genre 1 	       
        { 
        	 for (int j=0; j<sizeTarget; j++) 		//vector of genre 2
        	 {
        		 //System.out.println("j =" +j);
        		 gActive = (int) genreActive.get(i);      // get both genre at specific index
        		 gTarget = (int) genreTarget.get(j);
        		 
        		//if(gActive >1) System.out.println("active element" + gActive);
        		//if(gTarget >1) System.out.println("active element" + gTarget);
        		 
        		 if (gActive == gTarget)  			//genre are the same
        		 {
        			 match++;
        			 top +=  ((1*1.0)/dA) *((1*1.0)/dT);
        			 bottomActive += (1*1.0)/dA;		// we should square it first (but as it contains only '1', so no need)
        			 bottomTarget += (1*1.0)/dT;
        		 }
        	 } //end of inner for
          } //end of outer for
       }//to avoid null (if)
      
        if(match!=0) //there is atleast one commonality
        {
	        bottomTarget = Math.sqrt(bottomTarget);
	        bottomActive = Math.sqrt(bottomActive);   
	        bottom = bottomTarget * bottomActive;	        
	         
        }
        
      // if(weight >1) System.out.println("weight =" + weight);  //now there are weight >0
        
        if ( bottomTarget * bottomActive ==0) return 0;       
          
        return top *1.0/bottom;			//else return '0'.
 }

/******************************************************************************************************/
    
   //defualt methods to be implemented
    public double findUserSimilarity(DataReader dataReader, int uid1, int uid2)   
    {
      return 0;	
    }
    
    public void setNumMinMovies(int numMinMovies) 
    {
    	
    }
    
    public void setNumMinUsers(int numMinMovies) 
    {
    	
    }

    
}
