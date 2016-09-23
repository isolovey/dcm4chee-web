package org.dcm4chee.web.war.tc;

import org.dcm4chee.web.war.tc.imageview.TCImageViewImage;


public class TCReferencedImage extends TCReferencedInstance implements TCImageViewImage
{
	private static final long serialVersionUID = 1391709251504113739L;

	private final int frameNumber;
	
	public TCReferencedImage(TCReferencedSeries series, String iuid, String cuid, int instanceNumber) 
    {
		this(series, iuid, cuid, instanceNumber, -1);
    }
	
	public TCReferencedImage(TCReferencedSeries series, String iuid, String cuid, int instanceNumber, int frameNumber) 
    {
    	super(series, iuid, cuid, instanceNumber);
    	this.frameNumber = frameNumber;
    }
	
	@Override
	public int getFrameNumber()
	{
		return frameNumber;
	}
	
	@Override
	public boolean isFrameOfMultiframeImage()
	{
		return frameNumber>0;
	}
    
    @Override
    public boolean equals(Object o)
    {
        if (o instanceof TCReferencedImage)
        {
            TCReferencedImage ref = (TCReferencedImage)o;
            return getStudyUID().equals(ref.getStudyUID()) &&
                getSeriesUID().equals(ref.getSeriesUID()) &&
                getInstanceUID().equals(ref.getInstanceUID()) &&
                getFrameNumber()==ref.getFrameNumber();
        }
        return super.equals(o);
    }
}
