package org.dcm4chee.web.war.tc;

import java.util.Date;

import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4che2.data.VR;
import org.dcm4che2.util.DateUtils;
import org.dcm4che2.util.UIDUtils;
import org.dcm4chee.web.common.delegate.BaseMBeanDelegate;
import org.dcm4chee.web.war.folder.delegate.ContentEditDelegate;
import org.slf4j.LoggerFactory;

/**
 * @author bernhard.ableitinger@gmail.com
 * @version $Revision$ $Date$
 * @since Jan 10, 2012
 */
public class TCStoreDelegate extends BaseMBeanDelegate 
{
    private static TCStoreDelegate instance;

    private TCStoreDelegate()
    {
        super();
    }
    
    public static synchronized TCStoreDelegate getInstance()
    {
        if (instance==null)
        {
            instance = new TCStoreDelegate();
        }
        return instance;
    }
    
    @Override
    public String getServiceNameCfgAttribute() 
    {
        return "tcStoreScuServiceName";
    }
            
    public void store(DicomObject dataset) throws Exception
    {
    	store(null, dataset);
    }
    
    public void store(String cuid, DicomObject dataset) throws Exception
    {
        try
        {
            LoggerFactory.getLogger(TCStoreDelegate.class).info("Storing dataset " + dataset.getString(Tag.SOPInstanceUID));
            server.invoke(serviceObjectName, "schedule", new Object[]{dataset}, new String[]{DicomObject.class.getName()});
        }
        catch (Exception e)
        {
            log.error("Failed to store dataset!", e);
        }
    }
    
    public boolean storeImmediately(DicomObject dataset) throws Exception
    {
    	return storeImmediately(null, dataset);
    }
    
    public boolean modifyImmediately(DicomObject dataset) throws Exception {
    	return storeImmediately(UID.Dcm4cheAttributesModificationNotificationSOPClass, 
    			createModificationDataset(dataset));
    }
    
    public boolean deleteImmediately(
    		String stuid, String suid, String iuid, String cuid) throws Exception {
    	return storeImmediately(createRejectionNoteDataset(
    			stuid,suid,iuid,cuid));
    }
    
    private boolean storeImmediately(String cuid, DicomObject dataset) throws Exception
    {
        try
        {
            return (Boolean) server.invoke(serviceObjectName, "store", 
            		new Object[]{cuid,dataset}, 
            		new String[]{String.class.getName(),DicomObject.class.getName()});
        }
        catch (Exception e)
        {
            log.error("Failed to store dataset!", e);
            
            throw e;
        }
    }
    
    private DicomObject createModificationDataset(DicomObject dataset) {
    	BasicDicomObject ds = new BasicDicomObject();
    	dataset.copyTo(ds);
    	
        //specific AttributeModificationService attributes
        ds.putString(Tag.QueryRetrieveLevel, VR.CS, "IMAGE");
        ds.putString(Tag.ReasonForTheAttributeModification, VR.CS, "CORRECT");
        ds.putString(Tag.ModifyingSystem, VR.LO, 
        		ModifyingSystemHelper.getInstance().getModifyingSystem());
        
        return ds;
    }

    private DicomObject createRejectionNoteDataset(
    		String stuid, String suid, String iuid, String cuid)
    {   
        DicomObject title = new BasicDicomObject();
        title.putString(Tag.CodingSchemeDesignator, null, "DCM");
        title.putString(Tag.CodeValue, null, "113001");
        title.putString(Tag.CodeMeaning, null, "Rejected For Quality Reasons");
        
        DicomObject refSOP = new BasicDicomObject();
        refSOP.putString(Tag.ReferencedSOPClassUID, null, cuid);
        refSOP.putString(Tag.ReferencedSOPInstanceUID, null, iuid);
        
        DicomObject refSeries = new BasicDicomObject();
        refSeries.putString(Tag.SeriesInstanceUID, null, suid);
        refSeries.putSequence(Tag.ReferencedSOPSequence).addDicomObject(refSOP);
        
        DicomObject refStudy = new BasicDicomObject();
        refStudy.putString(Tag.StudyInstanceUID, null, stuid);
        refStudy.putSequence(Tag.ReferencedSeriesSequence).addDicomObject(refSeries);
        
        BasicDicomObject ko = new BasicDicomObject();
        /*
        
        if (getPatientId()!=null) {
        	ko.putString(Tag.PatientID, VR.LO, getPatientId());
        }
        if (getPatientName()!=null) {
        	ko.putString(Tag.PatientName, VR.PN, getPatientName());
        }
        if (getPatientIdIssuer()!=null) {
        	ko.putString(Tag.IssuerOfPatientID, VR.LO, getPatientIdIssuer());
        }
        */
        
        ko.putString(Tag.SOPClassUID, null, UID.KeyObjectSelectionDocumentStorage);
        ko.putString(Tag.SOPInstanceUID, null, UIDUtils.createUID());
        ko.putString(Tag.SeriesInstanceUID, null, UIDUtils.createUID());
        ko.putString(Tag.StudyInstanceUID, null, stuid);
        ko.putString(Tag.Modality, null, "KO");
        ko.putString(Tag.ContentDate, null, DateUtils.formatDA(new Date()));
        ko.putString(Tag.ContentTime, null, DateUtils.formatTM(new Date()));
        ko.putSequence(Tag.ConceptNameCodeSequence).addDicomObject(title);
        ko.putSequence(Tag.CurrentRequestedProcedureEvidenceSequence).addDicomObject(refStudy);
        
        return ko;
    }

    private static class ModifyingSystemHelper extends BaseMBeanDelegate {
    	private static ModifyingSystemHelper instance;
    	private ModifyingSystemHelper() {
    	}
    	public static synchronized ModifyingSystemHelper getInstance() {
    		if (instance==null) {
    			instance = new ModifyingSystemHelper();
    		}
    		return instance;
    	}
    	@Override
    	public String getServiceNameCfgAttribute() {
    		return ContentEditDelegate.getInstance().getServiceNameCfgAttribute();
    	}
    	public String getModifyingSystem() {
    		try {
    			return (String) server.getAttribute(serviceObjectName, 
    					"ModifyingSystem");
    		}
    		catch (Exception e) {
    			log.warn("Unable to determine modifying system! Using default...", e);
    			return "DCM4CHEE-WEB";
    		}
    	}
    }
}
