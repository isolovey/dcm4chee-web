package org.dcm4chee.web.war.tc.imageview;

import java.io.Serializable;

/**
 * @author Bernhard Ableitinger <bernhard.ableitinger@agfa.com>
 * @since Jan 25, 2012
 */
public interface TCImageViewImage extends Serializable 
{
	public int getFrameNumber();
	
	public boolean isFrameOfMultiframeImage();
	
    public String getInstanceUID();
    
    public String getClassUID();
    
    public TCImageViewSeries getSeries();
    
    @Override
    public String toString();
}
