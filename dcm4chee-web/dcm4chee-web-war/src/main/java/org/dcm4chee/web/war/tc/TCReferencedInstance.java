package org.dcm4chee.web.war.tc;

import java.io.Serializable;

import org.dcm4chee.web.war.folder.delegate.WADODelegate;

/**
 * @author Bernhard Ableitinger <bernhard.ableitinger@agfa.com>
 * @version $Revision$ $Date$
 * @since May 04, 2011
 */
public class TCReferencedInstance implements Serializable {

	private static final long serialVersionUID = -4595238153105340245L;
	
	private TCReferencedSeries series;
    private String iuid;
    private String cuid;
    private int instanceNumber;

    public TCReferencedInstance(TCReferencedSeries series, String iuid, String cuid, int instanceNumber) {
        this.iuid = iuid;
        this.cuid = cuid;
        this.series = series;
        this.instanceNumber = instanceNumber;
    }

    public TCReferencedSeries getSeries()
    {
        return series;
    }
    
    public String getStudyUID() {
        return series.getStudy().getStudyUID();
    }
    
    public String getSeriesUID() {
        return series.getSeriesUID();
    }

    public String getInstanceUID() {
        return iuid;
    }

    public String getClassUID() {
        return cuid;
    }
    
    public boolean isImage() {
    	return isImage(cuid) && 
    			!isDocument();
    }
    
    public boolean isDocument() {
    	return TCDocumentObject.isDocument(this);
    }

    public int getInstanceNumber()
    {
    	return instanceNumber;
    }
    
    public static boolean isImage(String cuid) {
        return WADODelegate.IMAGE == WADODelegate.getInstance()
                .getRenderType(cuid);
    }
        
    @Override
    public boolean equals(Object o)
    {
        if (o instanceof TCReferencedInstance)
        {
            TCReferencedInstance ref = (TCReferencedInstance)o;
            return getStudyUID().equals(ref.getStudyUID()) &&
                getSeriesUID().equals(ref.getSeriesUID()) &&
                getInstanceUID().equals(ref.getInstanceUID());
        }
        return super.equals(o);
    }
}