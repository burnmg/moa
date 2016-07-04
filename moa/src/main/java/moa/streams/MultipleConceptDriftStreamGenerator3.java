package moa.streams;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.PrivilegedActionException;
import java.util.Random;
import java.util.stream.Stream;

import com.github.javacliparser.FileOption;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import moa.core.Example;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.streams.generators.DriftingHyperplaneGenerator;
import moa.streams.generators.HyperplaneGenerator;
import moa.tasks.TaskMonitor;


/**
 * This version uses DriftingHyperplaneGenerator
 * @author rl
 *
 */
public class MultipleConceptDriftStreamGenerator3 extends AbstractOptionHandler implements
InstanceStream{

	private static final long serialVersionUID = 1L;
	
    @Override
    public String getPurposeString() {
        return "Adds multiple Concept Drift to examples in a stream. (Use generators.HyperplaneGenerator as the base generator)";
    }
	@Override
	public void getDescription(StringBuilder sb, int indent) {

	}
    
    public IntOption streamLengthOption = new IntOption("streamlen", 'l', 
    		"Length of the stream", 1000000);
    
    public IntOption numDriftsOption = new IntOption("numdrifts", 'd', 
    		"Number of Drifts in the Stream. Must be greater than 1", 1, 1, Integer.MAX_VALUE);
    
    public IntOption widthOption = new IntOption("width",
            'w', "Width of concept drift change.", 1000);
    
    public FloatOption standardDev = new FloatOption("standardDeviation", 'S', "Standard deviation for the gaussian random number used to decide the location of each drift point.", 0.0, 0.0, 2.0);
    
    public IntOption drifIntOptiontRandomSeedOption = new IntOption("driftRandomSeed", 'r',
            "Seed for generating drift streams", 1);

    
    // Options for Hyperplane Generator. In current version, the Hyperplane cannot rotate.
    public IntOption numClassesOption = new IntOption("numClasses", 'c',
            "The number of classes to generate.", 2, 2, Integer.MAX_VALUE);
    
    public IntOption numAttsOption = new IntOption("numAtts", 't',
            "The number of attributes to generate.", 5, 0, Integer.MAX_VALUE);
    
    public IntOption noisePercentageOption = new IntOption("noisePercentage",
            'n', "Percentage of noise to add to the data.", 5, 0, 100);
    
    // Options for partial drifting
    public IntOption numDriftAttsOption = new IntOption("numDriftAtts", 'p',
            "Number of drifting atts", 3);

    protected int numberInstance;
    
    private DriftingHyperplaneGenerator stream1;
   
    private DriftingHyperplaneGenerator stream2;
    
    protected int switchPoint; 
    
    protected int driftPosition;
    
    protected int previousSwitchPoint;
    
    protected Random driftRandom;
    
    private boolean isSwitching;
    
	@Override
	public InstancesHeader getHeader() {
		return this.stream1.getHeader();
	}
	
	@Override
	protected void prepareForUseImpl(TaskMonitor monitor,
			ObjectRepository repository) {
		
		driftRandom = new Random(drifIntOptiontRandomSeedOption.getValue());
		switchPoint = streamLengthOption.getValue() / numDriftsOption.getValue();
		previousSwitchPoint = 0;
		driftPosition = computeNextDriftPosition(switchPoint, previousSwitchPoint);
		
		// set stream options. 
//		stream1 = getInitStream();
//		stream2 = getEvolvedStream(stream1);
	}
	
	private DriftingHyperplaneGenerator getInitStream()
	{
		
		DriftingHyperplaneGenerator newStream = new DriftingHyperplaneGenerator();
		newStream.getOptions().resetToDefaults();
		newStream.numClassesOption = this.numClassesOption;
		newStream.numAttsOption = this.numAttsOption;
		newStream.instanceRandomSeedOption.setValue(drifIntOptiontRandomSeedOption.getValue());
		newStream.noisePercentageOption = this.noisePercentageOption;
		newStream.prepareForUse();
		
		return newStream;
	}
	
	public void initStream1AndStream2()
	{
		this.stream1 = getInitStream();
		this.stream2 = getEvolvedStream(stream1);
	}
	
	public void setStream1(DriftingHyperplaneGenerator newStream1)
	{
		this.stream1 = newStream1;
		this.stream2 = getEvolvedStream(stream1);
	}
	
	public DriftingHyperplaneGenerator getStream2()
	{
		return stream2;
	}
	
	

	private DriftingHyperplaneGenerator getEvolvedStream(DriftingHyperplaneGenerator stream)
	{
		DriftingHyperplaneGenerator newStream = (DriftingHyperplaneGenerator) stream.copy();
		newStream.addPartialDrift(numDriftAttsOption.getValue());
		
		return newStream;
	}
	@Override
	public long estimatedRemainingInstances() {
		return streamLengthOption.getValue() - numberInstance;
	}

	@Override
	public boolean hasMoreInstances() {
		return numberInstance < streamLengthOption.getValue();
	}

	public boolean getIsSwitching()
	{
		return isSwitching;
	}
	
	
	@Override
	public Example nextInstance() {
		
		numberInstance++;
		
		
		/*
		 * One block contains one drift in the middle. 
		 * Switch is performed at the end of each block. 
		 */
		// perform the switch
		
		if(numberInstance >= switchPoint){
			stream1 = stream2;
			stream2 = getEvolvedStream(stream1);
			previousSwitchPoint = switchPoint;
			switchPoint += streamLengthOption.getValue() / numDriftsOption.getValue();
			driftPosition = computeNextDriftPosition(switchPoint, previousSwitchPoint);
//			driftPosition = (switchPoint - previousSwitchPoint) / 2;
			isSwitching = true;
		}
		else
		{
			isSwitching = false;
		}
		
		
		double x = -4.0 * (double) (numberInstance - driftPosition) / (double) this.widthOption.getValue();
		double probabilityDrift = 1.0 / (1.0 + Math.exp(x));
		
        if (this.driftRandom.nextDouble() > probabilityDrift) {
            return this.stream1.nextInstance();
        } else {
            return this.stream2.nextInstance();
        }
	}
	
	public int computeNextDriftPosition(int switchPoint, int previousSwitchPoint){
		
		double gaussianRandom = driftRandom.nextGaussian()*standardDev.getValue();
		
		
		// limit the range of gaussianRandom within [-0.9, 0.9]. 
		if (gaussianRandom > 0.9)
		{
			gaussianRandom = 0.9;
		}else if (gaussianRandom < -0.9)
		{
			gaussianRandom = -0.9;
		}
		
		
		int mean = (switchPoint - previousSwitchPoint) / 2;
		int blockCentrePosition = previousSwitchPoint + mean;
		int newDriftPosition = blockCentrePosition + (int)(mean * gaussianRandom); 
		//int newDriftpos = driftPositionMean + (int)(blockCentrePoint); 
		return newDriftPosition;
	}

	@Override
	public boolean isRestartable() {
		return this.stream1.isRestartable() && this.stream2.isRestartable();
	}

	@Override
	public void restart() {
		this.stream1.restart();
		this.stream2.restart();
		
		numberInstance = 0;
		switchPoint = streamLengthOption.getValue() / numDriftsOption.getValue();
		previousSwitchPoint = 0;
		
		
	}
	
	public void restartOnlyParameters()
	{
		numberInstance = 0;
		switchPoint = streamLengthOption.getValue() / numDriftsOption.getValue();
		previousSwitchPoint = 0;
	}
}
