package netflix.weka;

import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;

import java.io.File;

public class Converter {
/**
* takes 2 arguments:
* - CSV input file
* - ARFF output file
*/
public static void main(String[] args) throws Exception {
if (args.length != 2) {
System.out.println("\nUsage: Converter <input.csv> <output.arff>\n");
//System.exit(1);
}

// load CSV
CSVLoader loader = new CSVLoader();
loader.setSource(new File("C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\Alis\\Book1.csv"));
Instances data = loader.getDataSet();

// save ARFF
ArffSaver saver = new ArffSaver();
saver.setInstances(data);
saver.setFile(new File("C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\Alis\\Book2.arff"));
saver.setDestination(new File("C:\\Users\\Musi\\workspace\\MusiRecommender\\DataSets\\Alis\\Book3.arff"));
saver.writeBatch();
}
}