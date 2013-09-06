package netflix.utilities;

import java.io.FileWriter;
import java.io.IOException;
 
public class GenerateCsv
{
   public static void main(String [] args)
   {
	   String path = "C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\SML_ML\\SVD\\";
	  // generateCsvFile(path + "\\test.csv");
	    checkCoff();
   }
 
   //----------------------------------------   
   public static void checkCoff()
   {
	   for (int i=0;i<=10;i++)
	   {
		   for(int j=0;j<=10;j++)
		   {
			   if(i+j==10)
				   System.out.println(i/10.0+", "+j/10.0);
		   }
	   }
   }

   //----------------------------------------
   private static void generateCsvFile(String sFileName)
	{
		try
		{
		    FileWriter writer = new FileWriter(sFileName, true);
 
			writer.append("");
			writer.append(',');
			writer.append("20");
			writer.append(',');
			writer.append("30");
			writer.append(',');
			writer.append("40");			
			writer.append('\n');
			
			writer.append("A");
			writer.append(',');
			writer.append(".02");
			writer.append(',');
			writer.append("0.8");
			writer.append(',');
			writer.append("0.7");			
			writer.append('\n');			
			
			
			writer.append("B");
			writer.append(',');
			writer.append("1.02");
			writer.append(',');
			writer.append("1.8");
			writer.append(',');
			writer.append("1.7");			
			writer.append('\n');
			
			writer.append("C");
			writer.append(',');
			writer.append("3.02");
			writer.append(',');
			writer.append("4.8");
			writer.append(',');
			writer.append("2.7");			
			writer.append('\n');
			
 
			 
			//generate whatever data you want
 
			writer.flush();
			writer.close();
		}
		catch(IOException e)
		{
		 e.printStackTrace();
		} 
	}
}