package org.dcm4chee.web.war.tc.imageview;

import java.io.Serializable;
import java.util.List;

/**
 * @author Bernhard Ableitinger <bernhard.ableitinger@agfa.com>
 * @since Jan 25, 2012
 */
public interface TCImageViewSeries extends Serializable 
{
    public String getSeriesUID();
    
    public String getSeriesValue(int tag);
        
    public int getImageCount();
    
    public List<? extends TCImageViewImage> getImages();
    
    public TCImageViewStudy getStudy();
    
    @Override
    public String toString();
    
}
