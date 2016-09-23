package org.dcm4chee.web.war.tc.imageview;

import java.io.Serializable;
import java.util.List;

/**
 * @author Bernhard Ableitinger <bernhard.ableitinger@agfa.com>
 * @since Jan 25, 2012
 */
public interface TCImageViewStudy extends Serializable 
{
    public String getStudyUID();
    
    public String getStudyValue(int tag);
    
    public int getSeriesCount();
    
    public List<? extends TCImageViewSeries> getSeries();
    
    @Override
    public String toString();
}
