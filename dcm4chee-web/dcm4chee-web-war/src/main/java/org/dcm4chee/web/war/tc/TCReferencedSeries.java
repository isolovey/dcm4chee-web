package org.dcm4chee.web.war.tc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4chee.archive.entity.Series;
import org.dcm4chee.archive.util.JNDIUtils;
import org.dcm4chee.web.dao.tc.TCQueryLocal;
import org.dcm4chee.web.war.tc.imageview.TCImageViewSeries;

/**
 * @author Bernhard Ableitinger <bernhard.ableitinger@agfa.com>
 * @version $Revision$ $Date$
 * @since May 04, 2011
 */
public class TCReferencedSeries implements TCImageViewSeries {
    private static final long serialVersionUID = 1L;
    
    private TCReferencedStudy study;
    private String suid;
    private String seriesDescription;
    private List<TCReferencedInstance> instances;
    private List<TCReferencedInstance> docs;
    private List<TCReferencedInstance> notImages;
    private Comparator<TCReferencedInstance> instanceComparator;
    private DicomObject dataset;
    private boolean dbQueryDone;
    private boolean resortInstances;
    
    public TCReferencedSeries(String suid, TCReferencedStudy study)
    {
    	this(suid, study, null);
    }
    
    public TCReferencedSeries(String suid, TCReferencedStudy study, String seriesDescription)
    {
        this.suid = suid;
        this.study = study;
        this.seriesDescription = seriesDescription;
        this.dbQueryDone = false;
        this.resortInstances = false;
        this.instances = new ArrayList<TCReferencedInstance>();
        this.instanceComparator = new InstanceComparator();
    }
    
    @Override
    public String getSeriesUID()
    {
        return suid;
    }
    
    @Override
    public TCReferencedStudy getStudy()
    {
        return study;
    }
    
    public String getSeriesDescription() {
    	if (seriesDescription==null) {
    		if (!dbQueryDone) {
    			seriesDescription = getSeriesValue(Tag.SeriesDescription);
    		}
    	}
    	return seriesDescription;
    }
    
    public int getInstanceCount()
    {
        return instances.size();
    }
    
    @Override
    public int getImageCount()
    {
        return instances.size() -
        		getNoneImagesCount() -
        		getDocumentCount();
    }
    
    public int getDocumentCount() {
    	return docs!=null ? docs.size() : 0;
    }
    
    @Override
    public String getSeriesValue(int tag)
    {
        if (!dbQueryDone)
        {
            try
            {
                TCQueryLocal dao = (TCQueryLocal) JNDIUtils
                    .lookup(TCQueryLocal.JNDI_NAME);
                Series series = dao.findSeriesByUID(suid);
                if (series!=null)
                {
                    dataset = series.getAttributes(true);
                }
            }
            finally
            {
                dbQueryDone = true;
            }
        }
        
        if (dataset!=null)
        {
            return dataset.getString(tag);
        }
        
        return null;
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
    public List<TCReferencedImage> getImages()
    {
        if (getNoneImagesCount()==0 &&
        	getDocumentCount()==0)
        {
            return (List)getInstances();
        }
        else
        {
            List<?> images = new ArrayList(getInstances());
            images.removeAll(notImages);
            return (List)images;
        }
    }

    public List<TCReferencedInstance> getDocuments()
    {
		if (docs!=null) {
			return docs;
		}
		return Collections.emptyList();
    }
    
    public List<TCReferencedInstance> getInstances()
    {
    	if (resortInstances)
    	{
            Collections.sort(instances, instanceComparator);
            
            resortInstances = false;
    	}
    	
        return Collections.unmodifiableList(instances);
    }
    
    public void addInstance(TCReferencedInstance instance)
    {
        if (!instances.contains(instance))
        {
            instances.add(instance);
            
            resortInstances = true;
            
            if (!instance.isImage())
            {
                if (notImages==null)
                {
                    notImages = new ArrayList<TCReferencedInstance>(5);
                }
                if (!notImages.contains(instance))
                {
                    notImages.add(instance);
                }
            }
            
            if (instance.isDocument()) {
            	if (docs==null) {
            		docs = new ArrayList<TCReferencedInstance>(5);
            	}
            	if (!docs.contains(instance)) {
            		docs.add(instance);
            	}
            }
        }
    }
    
    public void removeInstance(TCReferencedInstance instance)
    {
    	if (instances!=null)
    	{
    		instances.remove(instance);
    		if (notImages!=null)
    		{
    			notImages.remove(instance);
    			if (notImages.isEmpty())
    			{
    				notImages = null;
    			}
    		}
    		if (docs!=null)
    		{
    			docs.remove(instance);
    			if (docs.isEmpty())
    			{
    				docs = null;
    			}
    		}
    	}
    }
    
    private int getNoneImagesCount() {
    	return notImages!=null ? notImages.size() : 0;
    }
    
    private static class InstanceComparator implements Serializable, Comparator<TCReferencedInstance> 
    {
		private static final long serialVersionUID = -2944080049158009801L;

		@Override
		public int compare(TCReferencedInstance i1, TCReferencedInstance i2)
    	{
    		int n1 = i1.getInstanceNumber();
    		int n2 = i2.getInstanceNumber();
    		
    		if (n1==n2)
    		{
    			if (i1 instanceof TCReferencedImage && 
    					i2 instanceof TCReferencedImage)
    			{
    				n1 += ((TCReferencedImage)i1).getFrameNumber();
    				n2 += ((TCReferencedImage)i2).getFrameNumber();
    			}
    		}
    		
    		if (n1<=n2)
    		{
    			return -1; 
    		}
    		else
    		{
    			return 1;
    		}
    	}
    }
}