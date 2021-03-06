package testers;

import inputstream.BernoulliDistributionGenerator;
import moa.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import sizeof.agent.SizeOfAgent;
import cutpointdetection.ADWIN;
import cutpointdetection.CUSUM;
import cutpointdetection.CutPointDetector;
import cutpointdetection.EnsembleDetector;
import cutpointdetection.PHT;
import cutpointdetection.SEEDChangeDetector;
import cutpointdetection.SingDetector;
import cutpointdetection.OnePassDetector.EDD;

public class CUSUMTester implements Tester
{
    int numDriftInstances = 1000;
    //int numDriftInstances = 500000;
    int iterations = 100;

    private final int LINEAR_DECAY = 1;
    private final int EXPONENTIAL_DECAY = 2;
    private final int FIXED_TERM = 1;
    private final int PARETO = 2;

    private int DECAY_MODE = LINEAR_DECAY;
    private int COMPRESSION_MODE = FIXED_TERM;

    @Override
    public void doTest()
    {
	try
	{
	    //int[] numInst = {10000, 50000, 100000, 1000000};
	    //int[] numInst = {100000000, 250000000, 500000000};
	    int[] numInst = { 1000000 };
	    double[] slopes = { 0.0000, 0.0001, 0.0002, 0.0003, 0.0004 };
	    //double[] slopes = { 0.0000 };
	    //double[] epsilonPrimes = { 0.0025, 0.005, 0.0075, 0.01 };
	    double[] epsilonPrimes = { 0.01 };
	    //double[] linearAlphas = { 0.2, 0.4, 0.6, 0.8 };
	    double[] linearAlphas = { 0.8 };
	    double[] expAlphas = { 0.01, 0.025, 0.05, 0.075, 0.1 };
	    int[] fixedCompressionTerms = { 75 };
	    int[] paretoCompressionTerms = { 200, 400, 600, 800 };

	    int[] blocksizes = { 32, 64, 128, 256 };
	    int blockSize = 32;

	    double delta = 0.05;
	    
	    double[] alphas = null;
	    int[] compTerms = null;

	    if (DECAY_MODE == LINEAR_DECAY)
	    {
		alphas = linearAlphas;
	    } 
	    else if (DECAY_MODE == EXPONENTIAL_DECAY)
	    {
		alphas = expAlphas;
	    }

	    if (COMPRESSION_MODE == FIXED_TERM)
	    {
		compTerms = fixedCompressionTerms;
	    } 
	    else if (COMPRESSION_MODE == PARETO)
	    {
		compTerms = paretoCompressionTerms;
	    }	    

	    double[] prob = { 0.2 };

	    for (int e = 0; e < compTerms.length; e++)
	    {
		for (int w = 0; w < alphas.length; w++)
		{
		    for (int q = 0; q < epsilonPrimes.length; q++)
		    {
			for (int i = 0; i < numInst.length; i++)
			{
//			    BufferedWriter bWriter = new BufferedWriter(new FileWriter("src\\testers\\Volatility&ChangeDetection\\Test\\"+ blockSize + "_"+ DECAY_MODE + "_" + COMPRESSION_MODE + "_" + "L-" + numDriftInstances + "_" + compTerms[e] + "_" + epsilonPrimes[q] + "_" + alphas[w] + "_" + numInst[i] + ".csv"));
			    BufferedWriter bWriter = new BufferedWriter(new FileWriter("src\\testers\\Volatility&ChangeDetection\\Test\\CUSUM\\Ensemble_200_5_cusumadwinsingphtedd_0.4.csv"));
			    
//			    bWriter.write("Slope,Number of Drifts,TP Rate,Avg Delay,Delay Stdev,Avg Time,Time Stdev,Compression Rate,Avg No. Checks,Checks Stdev,Memory Size");
			    bWriter.write("Slope,Number of Drifts,TP Rate,Avg Delay,Delay Stdev,Avg Time,Time Stdev,Memory Size");
			    bWriter.newLine();
			    for (int j = 0; j < slopes.length; j++)
			    {
				int[] delays = new int[iterations];
				int totalDrift = 0;
				long[] times = new long[iterations];
				long totalTime = 0;

				int totalSize = 0;
				for (int k = 0; k < iterations; k++)
				{
				    generateSlopedInput(prob[0], slopes[j], numInst[i], numDriftInstances, k);

				    EDD edd = new EDD(0.05, 100, 1, 0);
				    ADWIN adwin = new ADWIN(delta);
				    SingDetector sing = new SingDetector(delta, blockSize, DECAY_MODE, COMPRESSION_MODE, epsilonPrimes[q], alphas[w], compTerms[e]);
				    SEEDChangeDetector seed = new SEEDChangeDetector();
				    CUSUM cusum = new CUSUM();
				    //CutPointDetector[] ens = new CutPointDetector[]{new CUSUM(50), new ADWIN(0.05), new SingDetector(0.05, 32, 1, 1, 0.01, 0.8, 75)};
				    CutPointDetector[] ens = new CutPointDetector[]{new CUSUM(50), new PHT(30), new ADWIN(0.05), new SingDetector(0.05, 32, 1, 1, 0.01, 0.8, 75), edd};
				    //CutPointDetector[] ens = new CutPointDetector[]{new PHT(30)};
				    EnsembleDetector ensembleDetector = new EnsembleDetector(ens, 0.39);
				    ensembleDetector.setWaitTime(200);
				    CutPointDetector detector = ensembleDetector;
				    
				    int y = k + 1;
//				    BufferedReader br = new BufferedReader(new FileReader("C:\\Users\\ACER\\Desktop\\kddcup99.csv"));
				    BufferedReader br = new BufferedReader(new FileReader("src\\testers\\TPData.txt"));
				    //BufferedReader br = new BufferedReader(new FileReader("src\\testers\\Volatility&ChangeDetection\\VolatilityStreamDriftPoint\\3\\VolatilityStream_3_"+k+".csv"));
				    String line = "";

				    int c = 0;

				    int delay = -1;
				    while ((line = br.readLine()) != null)
				    {
/*					    String[] pred = line.split(",");

						if(pred[0].equals(pred[1]))
						{
						    line = "1";
						}
						else
						{
						    line = "0";
						}
*/					
					long startTime = System.currentTimeMillis();
					if (detector.setInput(Double.parseDouble(line)) && c > (numInst[i] - numDriftInstances))
					{
					    long endTime = System.currentTimeMillis();
					    totalTime = totalTime + (endTime - startTime);
					    times[k] = times[k] + endTime - startTime;
					    delay = c - (numInst[i] - numDriftInstances);
					    delays[k] = delay;
					    totalDrift++;
					    break;
					}
					long endTime = System.currentTimeMillis();
					totalTime = totalTime + (endTime - startTime);
					times[k] = times[k] + endTime - startTime;
					c++;
				    }
				    totalSize += SizeOfAgent.fullSizeOf(detector);
				    br.close();
				}

				bWriter.write(slopes[j] + ",");
				bWriter.write(totalDrift + ",");
				bWriter.write(totalDrift / (double) iterations + ",");
				bWriter.write(calculateSum(delays) / totalDrift + ",");
				bWriter.write(calculateStdev(delays, calculateSum(delays) / totalDrift) + ",");
				bWriter.write((double) totalTime / iterations + ",");
				bWriter.write(calculateStdevLong(times, (double) totalTime / iterations) + ",");
				bWriter.write((double) totalSize / (double) iterations + ",");
				bWriter.newLine();
			    }
			    bWriter.close();
			}
		    }
		}
	    }
	} catch (Exception e)
	{
	    e.printStackTrace();
	}

    }

    public double calculateStdev(int[] times, double mean)
    {
	double sum = 0;
	int count = 0;
	for (int i : times)
	{
	    if (i > 0)
	    {
		count++;
		sum += Math.pow(i - mean, 2);
	    }
	}
	return Math.sqrt(sum / count);
    }

    public double calculateStdevLong(long[] times, double mean)
    {
	double sum = 0;
	int count = 0;
	for (Long i : times)
	{
	    if (i > 0)
	    {
		count++;
		sum += Math.pow(i - mean, 2);
	    }
	}
	return Math.sqrt(sum / count);
    }

    public double calculateSum(int[] delays)
    {
	double sum = 0.0;
	for (double d : delays)
	{
	    sum += d;
	}

	return sum;
    }

    public void generateSlopedInput(double driftProb, double slope, int numInstances, int numDriftInstances, int randomSeed)
    {
	try
	{

	    BufferedWriter bWriter = new BufferedWriter(new FileWriter("src\\testers\\TPData.txt"));

	    double[] driftMean = new double[1];
	    driftMean[0] = driftProb;
	    // System.out.println(driftMean[0]);
	    BernoulliDistributionGenerator gen = new BernoulliDistributionGenerator(driftMean, numInstances - numDriftInstances, randomSeed);
	    while (gen.hasNextTransaction())
	    {
		bWriter.write(gen.getNextTransaction() + "\n");
	    }

	    BernoulliDistributionGenerator genDrift = new BernoulliDistributionGenerator(driftMean, numDriftInstances, randomSeed);
	    while (genDrift.hasNextTransaction())
	    {
		driftMean[0] += slope;
		if (driftMean[0] >= 1.0)
		{
		    driftMean[0] = 1.0;
		}
		// System.out.println(driftMean[0]);
		genDrift.setMeans(driftMean);
		bWriter.write(genDrift.getNextTransaction() + "\n");
	    }

	    bWriter.close();

	} catch (Exception e)
	{
	    System.err.println("error");
	}
    }

    /*
     * public void generateInput(double[] driftProb, double driftIncrement, int
     * numInstances, int numDriftInstances, int randomSeed) { try {
     * 
     * BufferedWriter bWriter = new BufferedWriter(new
     * FileWriter("src\\testers\\TPData.txt"));
     * 
     * BernoulliDistributionGenerator gen = new
     * BernoulliDistributionGenerator(driftProb, numInstances -
     * numDriftInstances, randomSeed); while(gen.hasNextTransaction()) {
     * bWriter.write(gen.getNextTransaction() + "\n"); }
     * 
     * driftProb[0] += driftIncrement;
     * 
     * BernoulliDistributionGenerator genDrift = new
     * BernoulliDistributionGenerator(driftProb, numDriftInstances, randomSeed);
     * while(genDrift.hasNextTransaction()) {
     * bWriter.write(genDrift.getNextTransaction() + "\n"); }
     * 
     * driftProb[0] -= driftIncrement;
     * 
     * bWriter.close();
     * 
     * } catch(Exception e) { System.err.println("error"); } }
     */
}
