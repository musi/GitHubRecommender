package netflix.memreader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

public class NormalizeBookCrossingData 
{

	//data base related
	protected Connection 			conBC;
	protected String 				dbNameBC;
	protected String 				ratingsNameBC, bookNameBC, usersNameBc; 
	protected String 				createdRatingsNameBC, createdBookNameBC;
	
	// for mapping book id from string to a fixed Integer
	public HashMap<String, Integer>  bookIDMapper;
	
	
	public NormalizeBookCrossingData()
	{
		//db
		ratingsNameBC = "`bx-book-ratings`"; 
		bookNameBC    = "`bx-books`"; 
		usersNameBc   = "`bx-users`";
		
		createdRatingsNameBC = "`BC_books_ratings`";	//i HAVE TO PUT MANUAL NAMES?
		createdBookNameBC    = "`BC_books`"; 	
		
		dbNameBC      		 = "bc_ratings";
		
		//mapping
		bookIDMapper = new HashMap<String, Integer>();
		
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
			conBC = DriverManager.getConnection("jdbc:mysql://" +
				"localhost:3306/" + dbNameBC, "root", "ali5mas5");			
				success = true;
			
			
		} catch (Exception e){
			System.err.println("Error getting connection.");
			e.printStackTrace();
		}

		System.out.println("Connection created ");
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
					conBC.close();
				
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
	  *  Map Book_id to a fixed integer id
	  *  @return void
      */
					    
		public void mapBookIDToFixedInteger()
		{
			int i = 1;
			
			try
			{
		  	
			  Statement stmt = conBC.createStatement();	
			  //ResultSet rs = stmt.executeQuery("SELECT b.ISBN FROM" +  bookNameBC + "b;");
			  ResultSet rs = stmt.executeQuery("SELECT b.ISBN FROM" +  ratingsNameBC + "b;");
			
			  while (rs.next())					
					{						   				   
					   String ISBN	= rs.getString(1);	 //we have to change this with a fixed number.
					   if(ISBN!=null && ISBN !="") 
						  if(!(bookIDMapper.containsKey(ISBN)))
							  bookIDMapper.put(ISBN, i++);					   					   			  					   
					}
			  
						//close the statement
						stmt.close();
						System.out.println("Finished Mapping ");
				}
					
				catch(SQLException e){ e.printStackTrace(); } 
			
				System.out.println("total ISBNs =" + bookIDMapper.size());
		}
		
/******************************************************************************************************/

    /**
      *  alter Tables, add new columns 
	  */
		
	public void alterTablesForMapping()
	{
		
		try
		{
			// add new column in both tables
			Statement stmt = conBC.createStatement();		
			stmt.executeUpdate("ALTER TABLE" + bookNameBC + "DROP COLUMN `New-ISBN`;");
			stmt.executeUpdate("ALTER TABLE" + bookNameBC + "ADD `New-ISBN` int" + ";");
			
			stmt.executeUpdate("ALTER TABLE" + ratingsNameBC + "DROP COLUMN `New-ISBN`;");
			stmt.executeUpdate("ALTER TABLE" + ratingsNameBC + "ADD `New-ISBN` int" + ";");
			
			System.out.println("both tables have been altered");
		}
		catch(SQLException e){ e.printStackTrace(); }
	}
		
/******************************************************************************************************/

     /**
      *  Add mapping values into the columns 
	  */
		
	public void addMappingValues()
	{
		int n=0;
		
		try
		{
			// add new column values in both tables
			Statement stmt1 = conBC.createStatement();
			Statement stmt2 = conBC.createStatement();
			Statement adding_stmt1 = conBC.createStatement();
			Statement adding_stmt2 = conBC.createStatement();	
			
			// Insert in BookNamBC
			 ResultSet rs1 = stmt1.executeQuery("SELECT b.ISBN FROM" +  bookNameBC + "b;");				
			  while (rs1.next() )//&& n<50)					
					{					
				    	n++;
				    	
					   //Get ISBN
				  	   String ISBN	= rs1.getString(1);
				  	   
				  	   //Get Mapped ID against ISBN
					   int ISBNToIntegerID = bookIDMapper.get(ISBN);
									   
					    System.out.println("ISBN, New Id" + ISBN + "," + ISBNToIntegerID);
					   
					   //Insert Mapped ID into table
				     
					   //if (ISBN !="" && ISBN !=null && ISBNToIntegerID!=1 )
						   adding_stmt1.executeUpdate("UPDATE "+bookNameBC + " SET `New-ISBN`=" + ISBNToIntegerID + 
							   				" WHERE ISBN=" + ISBN +";");  
					   
					}
			  
				/*// Insert in BookRatingBC
				 ResultSet rs2 = stmt2.executeQuery("SELECT b.ISBN FROM" +  ratingsNameBC + "b;");				
				  while (rs2.next() )//&& n<50)					
						{					
					    						    	
						   //Get ISBN
					  	   String ISBN	= rs2.getString(1);
					  	   
					  	   //Get Mapped ID against ISBN
						   int ISBNToIntegerID = bookIDMapper.get(ISBN);
						  // ISBNToIntegerID = 999;
						   
						   System.out.println("ISBN, New Id" + ISBN + "," + ISBNToIntegerID);
						   
						   //Insert Mapped ID into table
					     
						   //if (ISBN !="" && ISBN !=null && ISBNToIntegerID!=1 )
							   adding_stmt2.executeUpdate("INSERT INTO "+ratingsNameBC + "(`New-ISBN`)" + 
								   				"VALUES (" + ISBNToIntegerID+ ");");
						   
						   
						   
						}
*/
		  	  System.out.println("Done writing into table Books..");
		} //end of try
		
		catch(SQLException e){ e.printStackTrace(); }
	}

	
/******************************************************************************************************/

	    /**
		  *  Create Tables 
	      */
		
 // we will keep track of the previous IDs and will insert new IDs (integers) in the table.
		
			public void createTables()
			{
				
  			try
				{
					Statement stmt1 = conBC.createStatement();
					Statement stmt2 = conBC.createStatement();
					
					stmt1.executeUpdate(
					  "DROP TABLE IF EXISTS" + createdBookNameBC +";");
					
					stmt1.executeUpdate(
					  "CREATE TABLE " + createdBookNameBC + "("+
					  "ISBN int,"+
					  "ISBN_Previous varchar(255),"+
					  "Book_Title varchar(255) DEFAULT NULL,"+
					  "Book_Author varchar(255) DEFAULT NULL,"+
					  "Year_Of_Publication int unsigned DEFAULT NULL,"+
					  "Publisher varchar(255) DEFAULT NULL,"+
					  "Image_URL_S varchar(255) CHARACTER SET latin1 COLLATE latin1_bin DEFAULT NULL,"+
					  "Image_URL_M varchar(255) CHARACTER SET latin1 COLLATE latin1_bin DEFAULT NULL,"+
					  "Image_URL_L varchar(255) CHARACTER SET latin1 COLLATE latin1_bin DEFAULT NULL"+
					 
					   ");");
					
					stmt2.executeUpdate(
					  "DROP TABLE IF EXISTS " + createdRatingsNameBC+ ";");
					
					stmt2.executeUpdate(
							"CREATE TABLE " + createdRatingsNameBC +"("+
						    "User_ID int(11) NOT NULL DEFAULT 0,"+
						    "User_ID_Previous int(11) NOT NULL DEFAULT 0,"+
						    "ISBN varchar(13) NOT NULL DEFAULT '',"+
						    "Book_Rating int(11) NOT NULL DEFAULT '0'"+
						    
							");");
					
				}							
					catch(SQLException e){ e.printStackTrace(); }
					
					System.out.println("Table created");
			}


/******************************************************************************************************/		
	    /**
	     *  Write newly mapped ID into the Bc_book and Bc_rating table
	     */
				    
	public void insertIntoTables()
	{
			//System.out.println(imdbId);
			//System.out.println("-----------------------------------------");
						
		try
		{
	  	  Statement stmt1 = conBC.createStatement();
	  	  Statement stmt2 = conBC.createStatement();
	  	  Statement stmt3 = conBC.createStatement();
	  	  Statement stmt4 = conBC.createStatement();
		  
			
		  ResultSet rs = stmt1.executeQuery(
				  "SELECT `ISBN`, `Book-Title`, `Book-Author`, `Year-Of-Publication`," +
				  "`Publisher`, `Image-URL-S`, `Image-URL-M`, `Image-URL-L` FROM" + bookNameBC + ";");
	
		
	 
	  	  System.out.println("Going to writing..");
		  int n =0;
		  
		  while (rs.next() && n<50)					
			{		
			      System.out.println(n);				  
			       n++;
				   String ISBN				 	= rs.getString(1);	 //we have to change this with a fixed number.
				   String book_Title 		 	= rs.getString(2);
				   String book_Author 			= rs.getString(3);  
				   int year_Of_Publication 		= rs.getInt(4);
				   String publisher 			= rs.getString(5);
				   String image_URL_S 			= rs.getString(6);
				   String image_URL_M 			= rs.getString(7);
				   String image_URL_L 			= rs.getString(8);
				   
			
				   
			   if(bookIDMapper.containsKey(ISBN) && ISBN !=null)
			   {
				   int ISBNToIntegerID = bookIDMapper.get(ISBN);				   
				
				   //put values into the newly created table in order
				   stmt2.executeUpdate("INSERT INTO bc_books VALUES (" +ISBNToIntegerID 	+"," 
											     			 			+ISBN  				+"," 
											     			 			+book_Title 		+","
											     			 			+book_Author		+","
											     			 			+year_Of_Publication +"," 
											     			 			+publisher 			+","
											     			 			+image_URL_S 		+","  
											     						+image_URL_M 		+"," 
											     						+image_URL_L 		+ ");");					     	
					
   
			/*			stmt2.executeUpdate("INSERT INTO bc_books VALUES (" +1 	+"," 
											     			 			+1  	+"," 
											     			 			+1 		+","
											     			 			+1		+","
											     			 			+1 		+"," 
											     			 			+1 		+","
											     			 			+1 		+","  
											     						+1 		+"," 
											     						+1 		+ ");");		*/			     				   
			   }//end of if   
			}						
										
					//close the statement
					stmt1.close();
					stmt2.close();
			}
					
			catch(SQLException e){ e.printStackTrace(); } 
		
	}
	
/******************************************************************************************************/
/**
 * Main Method
 */
	public static void main (String arg[])
	{
		NormalizeBookCrossingData NBC = new NormalizeBookCrossingData();
		NBC.openConnection();
		NBC.mapBookIDToFixedInteger();
	
		
		//Add column and add values from mapping   [It is not gonna work]
		//NBC.alterTablesForMapping();
		NBC.addMappingValues();
	
		//It is failing
		/*NBC.createTables();
		NBC.insertIntoTables();*/
		
		
		NBC.closeConnection();
	}
	
	
}
