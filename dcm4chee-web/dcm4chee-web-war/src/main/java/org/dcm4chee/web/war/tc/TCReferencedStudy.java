package org.dcm4chee.web.war.tc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.dcm4che2.data.DicomObject;
import org.dcm4chee.archive.entity.Study;
import org.dcm4chee.archive.util.JNDIUtils;
import org.dcm4chee.web.dao.tc.TCQueryLocal;
import org.dcm4chee.web.war.tc.imageview.TCImageViewStudy;

/**
 * @author Bernhard Ableitinger <bernhard.ableitinger@agfa.com>
 * @version $Revision$ $Date$
 * @since May 04, 2011
 */
public class TCReferencedStudy implements TCImageViewStudy {
    private static final long serialVersionUID = 1L;
    private String stuid;
    private List<TCReferencedSeries> series;
    private DicomObject dataset;
    private boolean dbQueryDone;
    
    public TCReferencedStudy(String stuid)
    {
        this.stuid = stuid;
        this.dbQueryDone = false;
        this.series = new ArrayList<TCReferencedSeries>();
    }
    @Override
    public String getStudyUID()
    {
        return stuid;
    }
    @Override
    public int getSeriesCount()
    {
        return series.size();
    }
    @Override
    public List<TCReferencedSeries> getSeries()
    {
        return Collections.unmodifiableList(series);
    }
    @Override
    public String getStudyValue(int tag)
    {
        if (!dbQueryDone)
        {
            try
            {
                TCQueryLocal dao = (TCQueryLocal) JNDIUtils
                    .lookup(TCQueryLocal.JNDI_NAME);
                Study study = dao.findStudyByUID(stuid);
                if (study!=null)
                {
                    dataset = study.getAttributes(true);
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
    public void addSeries(TCReferencedSeries series)
    {
        if (!this.series.contains(series))
        {
            this.series.add(series);
        }
    }
    public void removeSeries(TCReferencedSeries series) {
    	this.series.remove(series);
    }
}