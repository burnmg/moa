package cutpointdetection;

public class SingBlock
{
    private SingBlock next;
    private SingBlock previous;

    // private double[] items;
    private int blockSize;
    private double total;
    private double variance;
    private int itemCount;

    public SingBlock(int blockSize)
    {
	this.next = null;
	this.previous = null;
	this.blockSize = blockSize;

	// this.items = new double[blockSize];
	this.total = 0;
	this.variance = 0;
	this.itemCount = 0;
    }

    public SingBlock(SingBlock block)
    {
	this.next = block.getNext();
	this.previous = block.getPrevious();
	this.blockSize = block.blockSize;

	// this.items = new double[blockSize];
	this.total = block.total;
	this.variance = block.variance;
	this.itemCount = block.itemCount;
    }

    public void setNext(SingBlock next)
    {
	this.next = next;
    }

    public SingBlock getNext()
    {
	return this.next;
    }

    public void setPrevious(SingBlock previous)
    {
	this.previous = previous;
    }

    public SingBlock getPrevious()
    {
	return this.previous;
    }

    public int getBlockSize()
    {
	return blockSize;
    }

    public void setBlockSize(int blockSize)
    {
	this.blockSize = blockSize;
    }

    public void add(double value)
    {
	// items[itemCount++] = value;
	itemCount++;
	total += value;
    }

    public boolean isFull()
    {
	if (itemCount == blockSize)
	// if(itemCount == items.length)
	{
	    return true;
	} else
	{
	    return false;
	}
    }

    public double getMean()
    {
	return this.total / this.itemCount;
    }

    public void setTotal(double value)
    {
	this.total = value;
    }

    public double getTotal()
    {
	return this.total;
    }

    public void setItemCount(int value)
    {
	this.itemCount = value;
    }

    public int getItemCount()
    {
	return this.itemCount;
    }

    public void setVariance(double value)
    {
	this.variance = value;
    }

    public double getVariance()
    {
	return this.variance;
    }

}