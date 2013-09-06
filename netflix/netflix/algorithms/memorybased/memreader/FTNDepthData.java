package netflix.algorithms.memorybased.memreader;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import netflix.memreader.MemReader;

import com.mysql.jdbc.Blob;

import cern.colt.list.IntArrayList;
import cern.colt.map.OpenIntDoubleHashMap;
import cern.colt.map.OpenIntIntHashMap;
import cern.colt.map.OpenIntObjectHashMap;


public class FTNDepthData
{
		//class variables
		protected Connection 			con;
		protected String 				dbName;
		protected String 				ratingsName;
				
		private OpenIntObjectHashMap    usersAtEachDepth;
		private OpenIntObjectHashMap    neighboursAgainstEachUser[];
		public OpenIntObjectHashMap 	nicks_neighbours[];
		public OpenIntDoubleHashMap 	sumByCustInALayer[];
	    public OpenIntDoubleHashMap 	sumByMovieInALayer[];
	    public OpenIntObjectHashMap 	nicks;
	    
	    
	    private int 					depthK[];
	    private int 					totalInDepth13;
	    
		/**
		 * Default constructor.
		 * 
		 * Sets up a connection to the database "recommender", using
		 * the table name "ratings" for ratings and "movies" for movies.
		 */
		
		public FTNDepthData()		
		{
			dbName 		= "filmtrust1";
			ratingsName = "kd_root_neighbours";
		//	moviesName 	= "sml_movies";
			
			 nicks 						= new OpenIntObjectHashMap();
			 
			 usersAtEachDepth 			= new OpenIntObjectHashMap();
			 nicks_neighbours			= new OpenIntObjectHashMap [14];
			 neighboursAgainstEachUser	= new OpenIntObjectHashMap [14];
			 sumByCustInALayer 			= new OpenIntDoubleHashMap [14];
			 sumByMovieInALayer 		= new OpenIntDoubleHashMap [14];
			 
			 depthK 					= new int[14];
			 totalInDepth13=0;
			 
			 for (int i=1;i<=13;i++)
			 {
			    nicks_neighbours[i] =new OpenIntObjectHashMap();	
			    neighboursAgainstEachUser [i] =new OpenIntObjectHashMap();
			 }
		   		     
		}
		

	/******************************************************************************************************/

		/**
		 * @author steinbel - modified from Enchilada
		 * Opens the connection to the MySQL db "recommender".  If password changes
		 * are made, they should be made in here - password and db name are hard-
		 * coded in at present.
		 * @return boolean true on successful connection, false if problems
		 */
		
		public boolean openConnection()		
		{
			boolean success = false;
			
			try			
			{
				Class.forName("com.mysql.jdbc.Driver");
				con = DriverManager.getConnection("jdbc:mysql://" +
				"localhost:3306/" + dbName, "root", "ali5mas5");
			
				success = true;

			} catch (Exception e){
				System.err.println("Error getting connection.");
				e.printStackTrace();
			}

			return success;
		}

	/******************************************************************************************************/
		
		/**
		 * @author steinbel - lifted from Enchilada
		 * Closes the connection to the db.
		 * @return boolean true on successful close, false if problems
		 */
		
		public boolean closeConnection()
		
		{
			boolean success = false;
			
			try
			{
				con.close();
			
				success = true;
			} 
			catch (Exception e){
				System.err.println("Erorr closing the connection.");
				e.printStackTrace();
			}
			return success;
		}

/******************************************************************************************************/
		
		/**
		 * @author steinbel - lifted from Enchilada
		 * Returns the connection to the db.
		 * @return Connection con
		 */
		
		public Connection getConnection()
		
		{
			return con;
		}

/******************************************************************************************************/

		public ResultSet queryDB(String query)
		
		{
			ResultSet rs = null;
		
			try
			{
				Statement stmt = con.createStatement();
				rs = stmt.executeQuery(query);
				/* NOTE: cannot manually close Statment here or we
				 * lose the ResultSet access.
				 * May want to change this into a CachedRowSet to
				 * deal with that.  Also, what about memory limitations?
				 */
			}
			
			catch(SQLException e){ e.printStackTrace(); }
			return rs;
		}
		
/******************************************************************************************************/

/**
 * @return no of neighbours against a user in a specific layer
 * 
 */
		
		public IntArrayList getNeighboursInDepth(int userId, int depth)
		{
			//return  (IntArrayList)(nicks_neighbours[depth].get(userId));
			
			IntArrayList keys =  (IntArrayList)(nicks_neighbours[depth].keys());
			IntArrayList tempList;
			int myActiveUser=0;
			
			for (int i=0;i<keys.size();i++)
			{	
				myActiveUser = (keys.get(i));
				tempList = (IntArrayList) nicks_neighbours[depth].get(myActiveUser);
				
				if ( tempList.contains(userId))
				{
				   //tempList.remove(userId);		//as we are calculating against this user,		
				   return tempList;
				}
			}
			
			return (new IntArrayList());   //not found
		}

		
/******************************************************************************************************/

				
	public int getKForAUserInDepth(int userId, int depth)
	{
		 IntArrayList m= (IntArrayList) nicks_neighbours[depth].get(userId);
		 return m.size();
		 
	}
				
/******************************************************************************************************/

		/**
		 * @return number of neighbours against an active user in a specific layer
		 * 
		 */
				
		public IntArrayList getActiveUserInDepth( int depth)
		{
		  return  (IntArrayList)(nicks_neighbours[depth].keys());
		}
		

/******************************************************************************************************/
						
			public int getK( int depth)
			{
				IntArrayList activeUsers = getActiveUserInDepth(depth);
				int uid;
				int sum=0;
				int totalUsers = activeUsers.size();
				
				for(int i=0;i< totalUsers;i++)
				{
					uid = activeUsers.getQuick(i);					
					sum+=getKForAUserInDepth (uid, depth);				
				}
							
				return (int) ((sum * 1.0)/totalUsers);
			}
			

			public int getKForDepth(int depth)
			{
				
				return depthK[depth];
			}
/******************************************************************************************************/
			
	/**
	* @return no of neighbours against a user in a specific layer
	* 
	*/
						
	public IntArrayList getactive( int depth)
	{
	  return  (IntArrayList)(nicks_neighbours[depth].keys());
	}
				
/******************************************************************************************************/
				
				
/**
 * @return true if user is present at some specific layer
 */
		
		public boolean IfUserIsAnActiveUserInThisDepth(int userId, int depth)
		{
			if (nicks_neighbours[depth].keys().contains(userId))
			return true;
			
			else
				return false;
		}
		
		
/******************************************************************************************************/

	public void loadUsers()	
	 {
			IntArrayList activeUsers;
			IntArrayList tempUsers;
			int 		 dummyUser;	
			int 		 temp;	
			int 		countOfNickId=0;
			
		for(int depth=1;depth<=13;depth++)
	 	{
			 System.out.println("---------------------------");
			 System.out.println(depth);
			 System.out.println("---------------------------");
			  IntArrayList allUsersInALayer = new IntArrayList();
			  activeUsers= (IntArrayList)nicks_neighbours[depth].keys(); //active user at each layer
			
	//		  if(activeUsers.getQuick(0) ==518  /* && (nicks_neighbours[depth].get(j)) !=null */) 
	//			  {System.out.println("Nick =518 "+  (IntArrayList) nicks_neighbours[depth].get(activeUsers.getQuick(0)));
	//			   System.out.println( " size =" +((IntArrayList) nicks_neighbours[depth].get(activeUsers.getQuick(0))).size());
	//			  }
			  
			  
			  //first we add an active user at each layer and its friends
			  temp = activeUsers.getQuick(0);
			  allUsersInALayer.addAllOf((IntArrayList) nicks_neighbours[depth].get(temp));
			  allUsersInALayer.add(temp);
			  
			  //go through all the friends etc of an active user
		   for (int j=1;j< activeUsers.size();j++) 
			  {
			   
			//   if (depth ==13)
			//	   System.out.println(" Nick = 518 -->" + nicks_neighbours[depth].get(activeUsers.getQuick(j)));
			   // test 13 layer manually
			//   if(activeUsers.getQuick(j) ==1190 /* && (nicks_neighbours[depth].get(j)) !=null */) {
			//	   System.out.println( (IntArrayList) nicks_neighbours[depth].get(j));
			//	   System.out.println("depth =" + depth);
				   //System.out.println( " size =" +((IntArrayList) nicks_neighbours[depth].get(j)).size());
		//	   										
			//   										}
			  
	//		   if(activeUsers.getQuick(j) ==518  /* && (nicks_neighbours[depth].get(j)) !=null */) 
	//		   {
//				   System.out.println(" Nick = 518 -->" + nicks_neighbours[depth].get(activeUsers.getQuick(j)));
//				   System.out.println("depth =" + depth);
//				   System.out.println( " size =" +((IntArrayList) nicks_neighbours[depth].get(activeUsers.getQuick(j))).size());
			   		
	//		   }
			   
			 //  if(activeUsers.getQuick(j) ==879  /* && (nicks_neighbours[depth].get(j)) !=null */) {
			//	   System.out.println( "879 =" +(IntArrayList) nicks_neighbours[depth].get(j));
			//	   System.out.println("depth =" + depth);
				   //System.out.println( " size =" +((IntArrayList) nicks_neighbours[depth].get(j)).size());
			   		
			 //  }
			 //  if(activeUsers.getQuick(j) ==1297  /* && (nicks_neighbours[depth].get(j)) !=null */) {
			//	   System.out.println( "1297="+ (IntArrayList) nicks_neighbours[depth].get(j));
		//		   System.out.println("depth =" + depth);
				   //System.out.println( " size =" +((IntArrayList) nicks_neighbours[depth].get(j)).size());
			   		
	//		   }
			   
			  	 countOfNickId++;
				 tempUsers =  (IntArrayList) nicks_neighbours[depth].get(activeUsers.get(j));
				 				 
			 for (int k=0;k<tempUsers.size();k++) //a sub community within a layer
				 {
				     dummyUser = tempUsers.getQuick(k);
					if ((allUsersInALayer.contains(dummyUser))==false) //if we have not added this user, add him
					{
						allUsersInALayer.add(dummyUser);
					}
				 } //end of a sub-community
				 
			  }//end processing all active users in a depth
			  
			  //Now add the set of users which are related in a layer 
			  usersAtEachDepth.put(depth,allUsersInALayer );
			  //allUsersInALayer.clear(); (it is fuking shit....)
			//  System.out.println("depth =" + depth + ", User =" + ((IntArrayList)usersAtEachDepth.get(depth)).size() );
			 // System.out.println("count of nick id = " + (countOfNickId+1));
			 
			  
			  countOfNickId=0;
			  
			}//end of all depth
			
		}
	
/******************************************************************************************************/
/**
 * @return all the users in a layer
 */
	
	public IntArrayList getUsersInAdepth(int depth)
	{
		return (IntArrayList)usersAtEachDepth.get(depth);
	}
		
/******************************************************************************************************/

		public void getNickAndneighbours(int degree)
		{
			int nick = 0;			
			int dummyUser=0;
			
			System.out.println("-----------------------------------------");
			System.out.println( degree);
			System.out.println("-----------------------------------------");
			// ass first circle
			// Then for each depth, go and add previous list to it so d=2 --> add (2,1)
			// d=3 --> add (3,2) ....as 2 already have (2,1)
			// d=4 --> add (4,3) ...as 3 has 3,2,1
			try
			{
				Statement stmt = con.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT NickId, neighbours FROM " + ratingsName + " " 
											      + "WHERE degr_sep= " + degree + ";");
				
				System.out.println("query Ok" + degree);			
				
				while (rs.next())					
				{		
					   nick = rs.getInt(1);			
					   String myS = rs.getString(2);					   
					   String[] ss = myS.split(",");			
					   IntArrayList neighbourList = new IntArrayList();					   
					   if (nick==518 ) System.out.println("string size = " + ss.length);
					   
					   for(int t=0; t<ss.length;t++)
						  {						
							   neighbourList.add((int) Integer.parseInt(ss[t]));
							   if (nick==518 ) System.out.print( " Nick = " + nick + " Niegibours=" + ss[t] + ",");
						  }
					   if (nick==518 ) System.out.println();
					   
					 if(degree>1) //make circles
					 {
						 IntArrayList temp = new IntArrayList();
						 IntArrayList tempAll = new IntArrayList();
					
						 for (int k=0;k<neighbourList.size();k++) 	//add current list as well
						 {
								dummyUser= neighbourList.getQuick(k);
								if(tempAll.contains(dummyUser)==false)
									tempAll.add(dummyUser);
						 
						 }//end of adding neighbours from current layer 
						 
					
						 for(int t=degree; t>degree-1; t--) ///count inwards
						 {
							if(nicks_neighbours[t-1].get(nick) !=null) 
							 {
								temp =  (IntArrayList)nicks_neighbours[t-1].get(nick); //get previous depth
				//				if (nick==518 ) System.out.println("nick =" + nick + " degree = "+ t+ ", size=" + temp.size());
					
								for (int k=0;k<temp.size();k++) //do not add users already there from previous layers
								{
									dummyUser= temp.getQuick(k);
									if(tempAll.contains(dummyUser)==false)
										tempAll.add(dummyUser);
								}
								
							 }
							
						 } //end of adding neighbours from previous layer
						 
						 
						 nicks_neighbours[degree].put(nick, tempAll);
						 
						 if (nick==518 ) System.out.println("nick =" + nick + " degree = "+ degree+  ", size=" + neighbourList.size());
					 }
					 
					 
					 else //only one list is there						 
					 	 nicks_neighbours[degree].put(nick, neighbourList);
					 
					 
		//			 System.out.println("nick =" + nick);
					 if(nick==518)			{ System.out.println("nick_neighbour =" + nicks_neighbours[degree].get(nick));
					 						  System.out.println("nick_neighbour =" + ((IntArrayList)(nicks_neighbours[degree].get(nick))).size());
					 							}
					}
						
				
			  
				stmt.close();
			}
			
			catch(SQLException e){ e.printStackTrace(); } 
		/*	catch (IOException e) { 
				// 
				e.printStackTrace();
				}
			*/
		}
		
/******************************************************************************************************/
		 
		public static void main(String[] args)
		
		{
			FTNDepthData rw= new FTNDepthData();
			
			rw.openConnection();
						
			for (int i=1; i<=13;i++)
			{
				rw.getNickAndneighbours(i);
				  
			}
			
			
			
			//check somevalues
			
			for (int i=1;i<13;i++)
			{
			
				IntArrayList al= new IntArrayList();
				
				if(rw.nicks_neighbours[i].get(1100) !=null)
				{
				al = (IntArrayList)rw.nicks_neighbours[i].get(1100);			
				System.out.println("nick size=" + rw.nicks_neighbours[i].keys().size()  
						           + ", list =" + i +",size= " + al.size());
			   }
			
				
				//rw.depthK[i]= rw.getK(i);
			}
			
			System.out.println("nick size=" + rw.nicks_neighbours[1].get(2));
			System.out.println("nick size=" + rw.nicks_neighbours[2].get(63));
			System.out.println("nick size=" + rw.nicks_neighbours[3].get(63));
				
			System.out.println("==============================================");
			
			rw.loadUsers();
			
			System.out.println("==============================================");
			rw.closeConnection();
			
		}

/******************************************************************************************************/
		
		public void LoadNDepthData()		
		{
			FTNDepthData rw= new FTNDepthData();			
			
			openConnection();
						
			for (int i=1; i<=13;i++)
			{
				getNickAndneighbours(i);
				depthK[i]= getK(i);
				  
			}
			
			//load users at each layer into hashtable
			
			loadUsers();
			closeConnection();

		}
		
/******************************************************************************************************/

		//Serialize this object
		
		
	    public static void serialize(String fileName, FTNDepthData myObj) 
	    
	    {

	        try 
	        
	        {
	            FileOutputStream fos = new FileOutputStream(fileName);
	            ObjectOutputStream os = new ObjectOutputStream(fos);
	            os.writeObject(myObj);		//write the object
	            os.close();
	        }
	        
	        catch(FileNotFoundException e) {
	            System.out.println("Can't find file " + fileName);
	            e.printStackTrace();
	        }
	        
	        catch(IOException e) {
	            System.out.println("IO error");
	            e.printStackTrace();
	        }
	    }

//-----------------------------------------------------------
	     
	     public static FTNDepthData deserialize(String fileName)

	     {
	         try 
	         
	         {
	             FileInputStream fis    = new FileInputStream(fileName);
	             ObjectInputStream in   = new ObjectInputStream(fis);

	             return (FTNDepthData) in.readObject();	//deserilize into memReader class 
	         }
	         
	         catch(ClassNotFoundException e) {
	             System.out.println("Can't find class");
	             e.printStackTrace();
	         }
	         catch(IOException e) {
	             System.out.println("IO error");
	             e.printStackTrace();
	         }

	         //We should never get here
	         return null;
	     }

		
}
