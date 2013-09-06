package netflix.algorithms.modelbased.svd;

import java.util.*;
import java.io.*;

import Jama.Matrix;
import netflix.memreader.*;
import netflix.utilities.*;
import cern.colt.matrix.*;
import cern.colt.matrix.linalg.*;
import cern.colt.matrix.impl.*;


public class MatrixChecking 
{

	// Data members required
	DenseDoubleMatrix2D A; 					// |U| x k
	DenseDoubleMatrix2D B; 					// k x |I|
	DenseDoubleMatrix1D C; 					// k x 1
	DenseDoubleMatrix2D C1; 				// k x 1
	Algebra 			myAlgebra;
	private Random 		rand;
	double  myVal[][];
	EigenvalueDecomposition eigen;
	Property property;

	MatrixChecking()
	{
		C = new DenseDoubleMatrix1D(1);
		C1 = new DenseDoubleMatrix2D(10,10);
		
		myAlgebra = new Algebra();
		rand = new Random();
		myVal = new double[10][10];
		property = new Property(1.0E-50);
		
	}
	
/*************************************************************************************************/
  public void check()
  {
	  System.out.println(C);
	  C.assign(5.0);
	  System.out.println(C);
	  //C1.assign(cern.jet.random.Normal.staticNextDouble(1, 5));
/*	  C1.assign(cern.jet.random.Uniform.staticNextDouble());
	  System.out.println(C1);
	  C1.assign(cern.jet.random.Exponential.staticNextDouble(0.2));
	  System.out.println(C1);
	*/
	  for(int i=0;i<10;i++)
	  {
		  for(int j=0;j<10;j++)
		  {
			  //myVal[i][j] = cern.jet.random.Normal.staticNextDouble(3.5, 0.95);
			  myVal[i][j] = cern.jet.random.Uniform.staticNextDoubleFromTo(1.0, 5.0);
		  }
	  }
	  
	  C1.assign(myVal);
	  System.out.println(C1);
	  
	  eigen = new EigenvalueDecomposition(C1);
	  
	  DoubleMatrix2D v = eigen.getV();  
	  DoubleMatrix2D d = eigen.getD(); 
	  DoubleMatrix2D dInverse = myAlgebra.inverse(d);
	  DoubleMatrix2D vInverse = myAlgebra.inverse(v);
	  DoubleMatrix2D left = myAlgebra.mult(v, dInverse);
	  DoubleMatrix2D right = myAlgebra.mult(left, vInverse);
	  DoubleMatrix2D ans = myAlgebra.mult(left, right);
	  
	  
	  System.out.println(ans);
	  
	  
	  DoubleMatrix2D mm = new DenseDoubleMatrix2D(10,10);	  
	  
	  for (int i=0;i<10;i++)
	  {
		  for (int j=0;j<10;j++)
		  {
			  if(i!=j) mm.set(i, j, 0);
			  else 
				  {
				  if(i==0)
				  	mm.set(i, j,8.140837E-041);
				  
				  else if (i==1)
					  mm.set(i, j,1.101021E-024);
				  
				  else if (i==2)
					  mm.set(i, j, 2.354365E-024);
				  
				  else if (i==3)
					  mm.set(i, j, 1.422648E-023);
				  
				  else if (i==4)
					  mm.set(i, j,1.886164E-008);
				  
				  else if (i==5)
					  mm.set(i, j, 5.97858E-007);
				  
				  else if (i==6)
					  mm.set(i, j,5.299126E-006);
				  
				  else if (i==7)
					  mm.set(i, j, 2.027049E+010);
				  
				  else if (i==8)
					  mm.set(i, j, 3.386349E+011);
				  
				  else
					  mm.set(i,j,0);
				  }
			  
		  }
	  }
	  
	  int r =0;
	  
	  for (int i=0;i<10;i++)
	  {
		  if(mm.get(i,i) >0)
			  r++;
		  
	  }
	  
	  DoubleMatrix2D mmk = mm.viewPart(0, 0, r,r);
	  DoubleMatrix2D mmkInv = myAlgebra.inverse(mmk);
	  
	  System.out.println("r="+r);
	  System.out.println("mm="+mm);
	  System.out.println("mmk="+mmk);
	  System.out.println("mmkInv="+mmkInv);
	  
	  
	  //SVD
	  SingularValueDecomposition svd = new SingularValueDecomposition (mm);
	  
	  DoubleMatrix2D U = svd.getU(); 
	  DoubleMatrix2D V = svd.getV();
	  DoubleMatrix2D D = svd.getS();
	  
	  System.out.println("D="+D);
	  
	  r =0;
	  
	  for (int i=0;i<10;i++)
	  {
		  if(D.get(i,i) >0)
			  r++;
		  
	  }
	  
	  DoubleMatrix2D Dk = D.viewPart(0, 0, r,r);
	  DoubleMatrix2D DkInv = myAlgebra.inverse(Dk);
	  
	  System.out.println("r="+r);
	  System.out.println("D="+D);
	  System.out.println("Dk="+Dk);
	  System.out.println("DKINV="+DkInv);
	  
  }
	
	
/*************************************************************************************************/
	
	public static void main (String args[])
	{
		MatrixChecking mc = new MatrixChecking();
		mc.check();
		
	}
	
}
