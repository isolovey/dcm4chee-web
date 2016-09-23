package org.dcm4chee.web.war.tc;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.wicket.Component;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4chee.archive.entity.Code;
import org.dcm4chee.archive.util.JNDIUtils;
import org.dcm4chee.web.common.util.FileUtils;
import org.dcm4chee.web.dao.tc.ITextOrCode;
import org.dcm4chee.web.dao.tc.TCDicomCode;
import org.dcm4chee.web.dao.tc.TCQueryFilterKey;
import org.dcm4chee.web.dao.tc.TCQueryFilterValue.AcquisitionModality;
import org.dcm4chee.web.dao.tc.TCQueryFilterValue.Category;
import org.dcm4chee.web.dao.tc.TCQueryFilterValue.Level;
import org.dcm4chee.web.dao.tc.TCQueryFilterValue.PatientSex;
import org.dcm4chee.web.dao.tc.TCQueryFilterValue.YesNo;
import org.dcm4chee.web.dao.tc.TCQueryLocal;
import org.dcm4chee.web.war.folder.delegate.TarRetrieveDelegate;
import org.dcm4chee.web.war.tc.TCLink.TCLinkRelationship;
import org.dcm4chee.web.war.tc.keywords.TCKeyword;
import org.dcm4chee.web.war.tc.keywords.TCKeywordCatalogue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Bernhard Ableitinger <bernhard.ableitinger@agfa.com>
 * @version $Revision$ $Date$
 * @since May 04, 2011
 */
public class TCObject implements Serializable {

    private static final long serialVersionUID = 1L;

    private final static Logger log = LoggerFactory.getLogger(TCObject.class);
    
    private String id;
    
    private String cuid;
    
    private String iuid;
    
    private String suid;
    
    private String stuid;
    
    private String patId;
    
    private String patIdIssuer;
    
    private String patName;

    protected String abstr;

    protected List<AcquisitionModality> acquisitionModalities;

    protected ITextOrCode anatomy;

    protected String authorAffiliation;

    protected String authorContact;

    protected String authorName;

    protected Category category;

    protected ITextOrCode diagnosis;

    protected YesNo diagnosisConfirmed;

    protected ITextOrCode diffDiagnosis;

    protected String discussion;

    protected ITextOrCode finding;

    protected String history;

    protected List<ITextOrCode> keywords;

    protected Level level;

    protected String organSystem;

    protected ITextOrCode pathology;
    
    protected Integer patientAge;

    protected PatientSex patientSex;

    protected String patientSpecies;

    protected List<String> bibliographicReferences;

    protected String title;
    
    protected Date creationDate;
    
    protected List<TCLink> links;

    private List<TCReferencedStudy> studyRefs;
    
    private List<TCReferencedInstance> instanceRefs;
    
    private List<TCReferencedInstance> docRefs;
    
    private List<TCReferencedImage> imageRefs;
        
    protected TCObject(String id, DicomObject object) {
    	this.id = id;
        parse(object);
    }

    public static TCObject create(TCModel model) throws IOException {
    	DicomObject dataset = model.getDataset();
    	if (dataset.contains(Tag.ContentSequence)) {
    		return new TCObject(model.getId(), dataset);
    	}
    	else {
	        String fsID = model.getFileSystemId();
	        String fileID = model.getFileId();
	
	        DicomInputStream dis = null;
	        try {
	        	dis = new DicomInputStream(fsID.startsWith("tar:") ? 
	        			TarRetrieveDelegate.getInstance().retrieveFileFromTar(fsID, fileID) :
	        				FileUtils.resolve(new File(fsID, fileID)));
	            return new TCObject(model.getId(), dis.readDicomObject());
	        } finally {
	            if (dis != null) {
	                dis.close();
	            }
	        }
    	}
    }
        
    public String getId() 
    {
    	return id;
    }
    
    public String getURL() {
    	return TCCaseViewPage.urlForCase(iuid);
    }
    
    public String getClassUID()
    {
    	return cuid;
    }
    
    public String getInstanceUID()
    {
        return iuid;
    }
    
    public String getSeriesInstanceUID()
    {
    	return suid;
    }
    
    public String getStudyInstanceUID()
    {
    	return stuid;
    }
    
    public String getPatientId()
    {
    	return patId;
    }
    
    public String getPatientIdIssuer()
    {
    	return patIdIssuer;
    }
    
    public String getPatientName()
    {
    	return patName;
    }
    
    public Date getCreationDate() {
    	return creationDate;
    }
    
    public String getAbstr() {
        return abstr;
    }

    public List<AcquisitionModality> getAcquisitionModalities() {
        return acquisitionModalities;
    }

    public ITextOrCode getAnatomy() {
        return anatomy;
    }

    public String getAuthorAffiliation() {
        return authorAffiliation;
    }

    public String getAuthorContact() {
        return authorContact;
    }

    public String getAuthorName() {
        return authorName;
    }

    public Category getCategory() {
        return category;
    }

    public ITextOrCode getDiagnosis() {
        return diagnosis;
    }

    public YesNo getDiagnosisConfirmed() {
        return diagnosisConfirmed;
    }

    public ITextOrCode getDiffDiagnosis() {
        return diffDiagnosis;
    }

    public String getDiscussion() {
        return discussion;
    }

    public ITextOrCode getFinding() {
        return finding;
    }

    public String getHistory() {
        return history;
    }
    
    public int getKeywordCount() {
        return keywords!=null ? keywords.size() : 0;
    }
 
    public List<ITextOrCode> getKeywords() {
        return keywords;
    }

    public Level getLevel() {
        return level;
    }

    public String getOrganSystem() {
        return organSystem;
    }

    public ITextOrCode getPathology() {
        return pathology;
    }
    
    public Integer getPatientAge() {
        return patientAge;
    }
    
    public PatientSex getPatientSex() {
        return patientSex;
    }

    public String getPatientSpecies() {
        return patientSpecies;
    }

    public List<String> getBibliographicReferences() {
        if (bibliographicReferences != null) {
            return Collections.unmodifiableList(bibliographicReferences);
        } else {
            return Collections.emptyList();
        }
    }

    public String getTitle() {
        return title;
    }
    
    public List<TCLink> getLinks() {
    	if (links != null) {
    		return Collections.unmodifiableList(links);
    	}
    	return Collections.emptyList();
    }
    
    public List<TCReferencedStudy> getReferencedStudies() {
        if (studyRefs != null) {
            return Collections.unmodifiableList(studyRefs);
        }

        return Collections.emptyList();
    }
    
    public List<TCReferencedInstance> getReferencedInstances()
    {
        if (instanceRefs==null)
        {
            instanceRefs = new ArrayList<TCReferencedInstance>();
            
            List<TCReferencedStudy> studies = getReferencedStudies();
            for (TCReferencedStudy study : studies)
            {
                for (TCReferencedSeries series : study.getSeries())
                {
                    for (TCReferencedInstance instance : series.getInstances())
                    {
                        if (!instanceRefs.contains(instance))
                        {
                            instanceRefs.add(instance);
                        }
                    }
                }
            }
        }
        
        return instanceRefs;
    }
    
    public List<TCReferencedImage> getReferencedImages()
    {
        if (imageRefs==null)
        {
            imageRefs = new ArrayList<TCReferencedImage>();
            
            List<TCReferencedStudy> studies = getReferencedStudies();

            for (TCReferencedStudy study : studies)
            {
                for (TCReferencedSeries series : study.getSeries())
                {
                    for (TCReferencedImage image : series.getImages())
                    {
                    	if (image.isImage() && !imageRefs.contains(image))
                        {
                            imageRefs.add(image);
                        }
                    }
                }
            }
        }
        
        return imageRefs;
    }
    
    public List<TCReferencedInstance> getReferencedDocuments()
    {
        if (docRefs==null)
        {
            docRefs = new ArrayList<TCReferencedInstance>();
            
            List<TCReferencedStudy> studies = getReferencedStudies();

            for (TCReferencedStudy study : studies)
            {
                for (TCReferencedSeries series : study.getSeries())
                {
                    for (TCReferencedInstance doc : series.getDocuments())
                    {
                    	if (!docRefs.contains(doc))
                        {
                            docRefs.add(doc);
                        }
                    }
                }
            }
        }
        
        return docRefs;
    }
    
    public TCDocumentObject getReferencedDocumentObject(TCReferencedInstance ref) {
    	if (ref.isDocument()) {
    		try {
    			TCDocumentObject doc = TCDocumentObject.create(ref);
    			if (doc!=null) {
    				return doc;
    			}
    			
    			log.warn("Unable to find a referenced document object (SOP instance UID: " + 
						ref.getInstanceUID() + ")!");
    		}
    		catch (Exception e) {
    			log.error("Unable to create/parse a referenced document object (SOP instance UID: " +
    					ref.getInstanceUID() + ")!");
    		}
    	}
    	return null;
    }
    
    public List<TCDocumentObject> getReferencedDocumentObjects() {
    	List<TCReferencedInstance> refs = getReferencedDocuments();
    	if (refs==null || refs.isEmpty()) {
    		return Collections.emptyList();
    	}
    	else {
    		List<TCDocumentObject> docs = new ArrayList<TCDocumentObject>(refs.size());
    		for (TCReferencedInstance ref : refs) {
    			TCDocumentObject doc = getReferencedDocumentObject(ref);
    			if (doc!=null) {
    				docs.add(doc);
    			}
    		}
    		if (docs.size()>1) {
            	Collections.sort(docs, new Comparator<TCDocumentObject>() {
            		public int compare(TCDocumentObject doc1, TCDocumentObject doc2) {
            			Date date1 = doc1.getDocumentAddedDate();
            			Date date2 = doc2.getDocumentAddedDate();
            			if (date1!=null && date2!=null) {
            				return date1.compareTo(date2);
            			}
            			else if (date1!=null) {
            				return -1;
            			}
            			else if (date2!=null) {
            				return 1;
            			}
            			else {
            				return -1;
            			}
            		}
            	});
    		}
    		return Collections.unmodifiableList(docs);
    	}
    }
    
    public Object getValue(TCQueryFilterKey key) {
        Object value = null;

        if (TCQueryFilterKey.Abstract.equals(key)) {
            value = getAbstr();
        } else if (TCQueryFilterKey.AcquisitionModality.equals(key)) {
            value = concatStringValues(getAcquisitionModalities(), false);
        } else if (TCQueryFilterKey.Anatomy.equals(key)) {
            value = getAnatomy();
        } else if (TCQueryFilterKey.AuthorAffiliation.equals(key)) {
            value = getAuthorAffiliation();
        } else if (TCQueryFilterKey.AuthorContact.equals(key)) {
            value = getAuthorContact();
        } else if (TCQueryFilterKey.AuthorName.equals(key)) {
            value = getAuthorName();
        } else if (TCQueryFilterKey.BibliographicReference.equals(key)) {
            value = getBibliographicReferences();
        } else if (TCQueryFilterKey.Category.equals(key)) {
            value = getCategory() != null ? getCategory() : null;
        } else if (TCQueryFilterKey.DiagnosisConfirmed.equals(key)) {
            value = getDiagnosisConfirmed() != null ? getDiagnosisConfirmed() : null;
        } else if (TCQueryFilterKey.Diagnosis.equals(key)) {
            value = getDiagnosis();
        } else if (TCQueryFilterKey.DifferentialDiagnosis.equals(key)) {
            value = getDiffDiagnosis();
        } else if (TCQueryFilterKey.Discussion.equals(key)) {
            value = getDiscussion();
        } else if (TCQueryFilterKey.Finding.equals(key)) {
            value = getFinding();
        } else if (TCQueryFilterKey.History.equals(key)) {
            value = getHistory();
        } else if (TCQueryFilterKey.Keyword.equals(key)) {
            value = getKeywords();
        } else if (TCQueryFilterKey.Level.equals(key)) {
            value = getLevel() != null ? getLevel() : null;
        } else if (TCQueryFilterKey.OrganSystem.equals(key)) {
            value = getOrganSystem();
        } else if (TCQueryFilterKey.Pathology.equals(key)) {
            value = getPathology();
        } else if (TCQueryFilterKey.PatientAge.equals(key)) {
            value = getPatientAge() != null ? getPatientAge() : null;
        } else if (TCQueryFilterKey.PatientSex.equals(key)) {
            value = getPatientSex() != null ? getPatientSex() : null;
        } else if (TCQueryFilterKey.PatientSpecies.equals(key)) {
            value = getPatientSpecies();
        } else if (TCQueryFilterKey.Title.equals(key)) {
            value = getTitle();
        }

        return value;
    }

    public String getValueAsLocalizedString(TCQueryFilterKey key, Component c) {
    	return getValueAsLocalizedString(key, c, false);
    }

    public String getValueAsLocalizedString(TCQueryFilterKey key, Component c, boolean shortString) {
        if (TCQueryFilterKey.Category.equals(key)) {
            return getCategory() != null ? c.getString("tc.category."
                    + getCategory().name().toLowerCase()) : null;
        } else if (TCQueryFilterKey.DiagnosisConfirmed.equals(key)) {
            return getDiagnosisConfirmed() != null ? c.getString("tc.yesno."
                    + getDiagnosisConfirmed().name().toLowerCase()) : null;
        } else if (TCQueryFilterKey.Level.equals(key)) {
            return getLevel() != null ? c.getString("tc.level."
                    + getLevel().name().toLowerCase()) : null;
        } else if (TCQueryFilterKey.PatientSex.equals(key)) {
            return getPatientSex() != null ? c.getString("tc.patientsex."
                    + getPatientSex().name().toLowerCase()) : null;
        } else {
            return getValueAsString(key, shortString);
        }
    }

    public String getValueAsString(TCQueryFilterKey key) {
    	return getValueAsString(key, false);
    }
    
    public String getValueAsString(TCQueryFilterKey key, boolean shortString) {
        return toStringValue(getValue(key), shortString);
    }

    private String concatStringValues(List<?> list, boolean shortString) {
        if (list != null) {
            Iterator<?> it = list.iterator();
            StringBuilder sbuilder = new StringBuilder();
            if (it.hasNext())
                sbuilder.append(toStringValue(it.next(), shortString));
            while (it.hasNext()) {
                sbuilder.append("; ");
                sbuilder.append(toStringValue(it.next(), shortString));
            }
            return sbuilder.toString();
        }

        return null;
    }
    
    protected final void addReferencedInstance(TCReferencedInstance instance) {
    	TCReferencedSeries series = instance.getSeries();
    	TCReferencedStudy study = series.getStudy();

		series.addInstance(instance);
		study.addSeries(series);
		
    	if (studyRefs==null) {
    		studyRefs = new ArrayList<TCReferencedStudy>();
    	}
    	if (!studyRefs.contains(study)) {
    		studyRefs.add(study);
    	}

    	List<TCReferencedInstance> instanceRefs = getReferencedInstances();
    	if (instanceRefs==null) {
    		this.instanceRefs = instanceRefs = new ArrayList<TCReferencedInstance>();
    		this.instanceRefs.add(instance);
    	}
    	else if (!instanceRefs.contains(instance))
    	{
    		instanceRefs.add(instance);
    	}
    	
    	if (instance.isImage() && 
    			instance instanceof TCReferencedImage) 
    	{
    		List<TCReferencedImage> imageRefs = getReferencedImages();
    		if (imageRefs==null) {
    			this.imageRefs = imageRefs = new ArrayList<TCReferencedImage>();
    			this.imageRefs.add((TCReferencedImage)instance);
    		}
    		else if (!imageRefs.contains(instance)) {
    			imageRefs.add((TCReferencedImage) instance);
    		}
    	}
    	
    	if (instance.isDocument()) {
	    	List<TCReferencedInstance> docRefs = getReferencedDocuments();
	    	if (docRefs==null) {
	    		this.docRefs = docRefs = new ArrayList<TCReferencedInstance>();
	    		this.docRefs.add(instance);
	    	}
	    	else if (!docRefs.contains(instance))
	    	{
	    		docRefs.add(instance);
	    	}
    	}
    }
    
    protected final void removeReferencedInstance(TCReferencedInstance instance) {
    	TCReferencedSeries series = instance.getSeries();
    	TCReferencedStudy study = series.getStudy();
    	
    	series.removeInstance(instance);
    	if (series.getInstanceCount()<=0) {
    		study.removeSeries(series);
    	}
    	if (study.getSeriesCount()<=0) {
    		studyRefs.remove(study);
    		if (studyRefs.isEmpty()) {
    			studyRefs = null;
    		}
    	}

    	List<TCReferencedInstance> refs = getReferencedInstances();
    	if (refs.contains(instance))
    	{
    		refs.remove(instance);
    	}
    	
    	List<TCReferencedImage> imageRefs = getReferencedImages();
    	if (imageRefs.contains(instance))
    	{
    		imageRefs.remove(instance);
    	}
    	
    	List<TCReferencedInstance> docRefs = getReferencedDocuments();
    	if (docRefs.contains(instance))
    	{
    		docRefs.remove(instance);
    	}
    }
    
    protected void init(String id, DicomObject o) {
    	clear();
    	parse(o);
    	
    	this.id = id;
    }
    
    protected void clear() {
    	id=null;
    	cuid=null;
        iuid=null;
        suid=null;
        stuid=null;
        patId=null;
        patIdIssuer=null;
        patName=null;
        abstr=null;
        acquisitionModalities=null;
        anatomy=null;
        authorAffiliation=null;
        authorContact=null;
        authorName=null;
        category=null;
        diagnosis=null;
        diagnosisConfirmed=null;
        diffDiagnosis=null;
        discussion=null;
        finding=null;
        history=null;
        keywords=null;
        level=null;
        organSystem=null;
        pathology=null;
        patientAge=null;
        patientSex=null;
        patientSpecies=null;
        bibliographicReferences=null;
        title=null;
        studyRefs=null;
        instanceRefs=null;
        docRefs=null;
        imageRefs=null;
        links=null;
        creationDate=null;
    }

    private void parse(DicomObject o) {
    	cuid = o.getString(Tag.SOPClassUID);
        iuid = o.getString(Tag.SOPInstanceUID);
        suid = o.getString(Tag.SeriesInstanceUID);
        stuid = o.getString(Tag.StudyInstanceUID);
        patId= o.getString(Tag.PatientID);
        patIdIssuer = o.getString(Tag.IssuerOfPatientID);
        patName = o.getString(Tag.PatientName);
        creationDate = o.getDate(Tag.ContentDate);
        
        // parse content
        DicomElement content = o != null ? o.get(Tag.ContentSequence) : null;
        if (content != null) {
            int count = content.countItems();
            for (int i = 0; i < count; i++) {
                try {
                    DicomObject item = content.getDicomObject(i);

                    if (item != null) {
                        String valueType = item.getString(Tag.ValueType);
                        TCDicomCode conceptName = new TCDicomCode(
                                item.getNestedDicomObject(Tag.ConceptNameCodeSequence));

                        if ("CONTAINER".equalsIgnoreCase(valueType) &&
                        		TCDicomCode.REF_COMPOSITE_CONTAINER.equals(conceptName))
                        {
                        	String cuid=null, iuid=null, reltype=null, comment=null;
                            DicomElement content2 = item.get(Tag.ContentSequence);
                            if (content2 != null) {
                                int count2 = content2.countItems();
                                for (int j = 0; j < count2; j++) {
                                	DicomObject item2 = content2.getDicomObject(j);
                                	if (item2!=null) {
                                		String valueType2 = item2.getString(Tag.ValueType);
                                        TCDicomCode conceptName2 = new TCDicomCode(
                                                item2.getNestedDicomObject(Tag.ConceptNameCodeSequence));
                                        if ("COMPOSITE".equalsIgnoreCase(valueType2) &&
                                        		TCDicomCode.REF_COMPOSITE_OBJECT.equals(conceptName2)) {
                                        	DicomObject refsop = item2.getNestedDicomObject(Tag.ReferencedSOPSequence);
                                        	if (refsop!=null) {
                                        		iuid = refsop.getString(Tag.ReferencedSOPInstanceUID);
                                        		cuid = refsop.getString(Tag.ReferencedSOPClassUID);
                                        	}
                                        }
                                        else if ("TEXT".equalsIgnoreCase(valueType2)) {
                                        	if (TCDicomCode.REF_COMPOSITE_RELATIONSHIP_TYPE.equals(conceptName2)) {
                                        		reltype = item2.getString(Tag.TextValue);
                                        	}
                                        	else if (TCDicomCode.REF_COMPOSITE_COMMENT.equals(conceptName2)) {
                                        		comment = item2.getString(Tag.TextValue);
                                        	}
                                        }
                                	}
                                }
                            }
                            if (cuid!=null && iuid!=null) {
                            	// crosslink
                            	if (UID.BasicTextSRStorage.equals(cuid)) {
                            		if (links==null) {
                            			links = new ArrayList<TCLink>(5);
                            		}
                            		links.add( new TCLink(this.iuid, iuid, reltype!=null ?
                            				TCLinkRelationship.valueOf(reltype) : 
                            					TCLinkRelationship.RELATES_TO, comment));
                            	}
                            }
                        }
                        else if ("TEXT".equalsIgnoreCase(valueType)) {
                            if (conceptName.equals(TCQueryFilterKey.Abstract
                                    .getCode())) {
                                abstr = getTextValue(item);
                            } else if (conceptName
                                    .equals(TCQueryFilterKey.AcquisitionModality
                                            .getCode())) {
                                AcquisitionModality m = AcquisitionModality
                                        .get(getTextValue(item));

                                if (m != null) {
                                    if (acquisitionModalities == null) {
                                        acquisitionModalities = new ArrayList<AcquisitionModality>();
                                    }

                                    if (!acquisitionModalities.contains(m)) {
                                        acquisitionModalities.add(m);
                                    }
                                }
                            } else if (conceptName
                                    .equals(TCQueryFilterKey.Anatomy.getCode())) {
                                anatomy = TextOrCode.text(getTextValue(item));
                            } else if (conceptName
                                    .equals(TCQueryFilterKey.AuthorName
                                            .getCode())) {
                                authorName = getTextValue(item);
                            } else if (conceptName
                                    .equals(TCQueryFilterKey.AuthorContact
                                            .getCode())) {
                                authorContact = getTextValue(item);
                            } else if (conceptName
                                    .equals(TCQueryFilterKey.AuthorAffiliation
                                            .getCode())) {
                                authorAffiliation = getTextValue(item);
                            } else if (conceptName
                                    .equals(TCQueryFilterKey.BibliographicReference
                                            .getCode())) {
                                String s = getTextValue(item);
                                if (s != null) {
                                    if (bibliographicReferences == null) {
                                        bibliographicReferences = new ArrayList<String>();
                                    }

                                    bibliographicReferences.add(s);
                                }
                            } else if (conceptName
                                    .equals(TCQueryFilterKey.Diagnosis
                                            .getCode())) {
                                diagnosis = TextOrCode.text(getTextValue(item));
                            } else if (conceptName
                                    .equals(TCQueryFilterKey.DifferentialDiagnosis
                                            .getCode())) {
                                diffDiagnosis = TextOrCode.text(getTextValue(item));
                            } else if (conceptName
                                    .equals(TCQueryFilterKey.Discussion
                                            .getCode())) {
                                discussion = getTextValue(item);
                            } else if (conceptName
                                    .equals(TCQueryFilterKey.Finding.getCode())) {
                                finding = TextOrCode.text(getTextValue(item));
                            } else if (conceptName
                                    .equals(TCQueryFilterKey.History.getCode())) {
                                history = getTextValue(item);
                            } else if (conceptName
                                    .equals(TCQueryFilterKey.Keyword.getCode())) {
                                addKeywordImpl(TextOrCode.text(getTextValue(item)));
                            } else if (conceptName
                                    .equals(TCQueryFilterKey.OrganSystem
                                            .getCode())) {
                                organSystem = getTextValue(item);
                            } else if (conceptName
                                    .equals(TCQueryFilterKey.Pathology
                                            .getCode())) {
                                pathology = TextOrCode.text(getTextValue(item));
                            } else if (conceptName
                                    .equals(TCQueryFilterKey.PatientAge
                                            .getCode())) {
                                try
                                {
                                    patientAge = Integer.valueOf(getTextValue(item));
                                }
                                catch (Exception e)
                                {
                                    log.warn("Parsing patient age failed! Skipped..");
                                }
                            } else if (conceptName
                                    .equals(TCQueryFilterKey.PatientSex
                                            .getCode())) {
                                patientSex = PatientSex.get(getTextValue(item));
                            } else if (conceptName
                                    .equals(TCQueryFilterKey.PatientSpecies
                                            .getCode())) {
                                patientSpecies = getTextValue(item);
                            } else if (conceptName
                                    .equals(TCQueryFilterKey.Title.getCode())) {
                                title = getTextValue(item);
                            }
                        } else if ("CODE".equalsIgnoreCase(valueType)) {
                            if (conceptName.equals(TCQueryFilterKey.Category
                                    .getCode())) {
                                category = Category.get(toCode(getCodeValue(item)));
                            } else if (conceptName
                                    .equals(TCQueryFilterKey.AcquisitionModality
                                            .getCode())) {
                                AcquisitionModality m = AcquisitionModality
                                        .get(toCode(getCodeValue(item)));

                                if (m != null) {
                                    if (acquisitionModalities == null) {
                                        acquisitionModalities = new ArrayList<AcquisitionModality>();
                                    }

                                    if (!acquisitionModalities.contains(m)) {
                                        acquisitionModalities.add(m);
                                    }
                                }
                            } else if (conceptName
                                    .equals(TCQueryFilterKey.DiagnosisConfirmed
                                            .getCode())) {
                                diagnosisConfirmed = YesNo.get(toCode(getCodeValue(item)));
                            } else if (conceptName
                                    .equals(TCQueryFilterKey.Level.getCode())) {
                                level = Level.get(toCode(getCodeValue(item)));
                            } else if (conceptName
                                    .equals(TCQueryFilterKey.Anatomy.getCode())) {
                                anatomy = TextOrCode.code(getCodeValue(item));
                            } else if (conceptName
                                    .equals(TCQueryFilterKey.Diagnosis
                                            .getCode())) {
                                diagnosis = TextOrCode.code(getCodeValue(item));
                            } else if (conceptName
                                    .equals(TCQueryFilterKey.DifferentialDiagnosis
                                            .getCode())) {
                                diffDiagnosis = TextOrCode.code(getCodeValue(item));
                            } else if (conceptName
                                    .equals(TCQueryFilterKey.Finding.getCode())) {
                                finding = TextOrCode.code(getCodeValue(item));
                            } else if (conceptName
                                    .equals(TCQueryFilterKey.Keyword.getCode())) {
                                addKeywordImpl(TextOrCode.code(getCodeValue(item)));
                            } else if (conceptName
                                    .equals(TCQueryFilterKey.Pathology
                                            .getCode())) {
                                pathology = TextOrCode.code(getCodeValue(item));
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Parsing TC attribute failed! Skipped...", e);
                }
            }
        }
        
        // parse references
        DicomElement ref = o != null ? o
                .get(Tag.CurrentRequestedProcedureEvidenceSequence) : null;
        int refCount = ref != null ? ref.countItems() : -1;
        if (refCount > 0) {
            for (int i = 0; i < refCount; i++) {
                DicomObject studyRef = ref.getDicomObject(i);
                String stuid = studyRef != null ? studyRef
                        .getString(Tag.StudyInstanceUID) : null;

                if (stuid != null) {
                    TCReferencedStudy study = new TCReferencedStudy(stuid);
                    
                    DicomElement seriesSeq = studyRef
                            .get(Tag.ReferencedSeriesSequence);
                    int seriesCount = seriesSeq != null ? seriesSeq
                            .countItems() : -1;
                    int instanceCount = 0;
                    
                    if (seriesCount > 0) {
                        for (int j = 0; j < seriesCount; j++) {
                            DicomObject seriesRef = seriesSeq.getDicomObject(j);
                            String suid = seriesRef != null ? seriesRef
                                    .getString(Tag.SeriesInstanceUID) : null;

                            if (suid != null) {
                                TCReferencedSeries series = new TCReferencedSeries(suid, study);
                                
                                DicomElement instanceSeq = seriesRef
                                        .get(Tag.ReferencedSOPSequence);
                                instanceCount = instanceSeq != null ? instanceSeq
                                        .countItems() : -1;
                                
                                if (instanceCount > 0) {
                                    TCQueryLocal ejb = (TCQueryLocal) JNDIUtils
                                            .lookup(TCQueryLocal.JNDI_NAME);
                                    
                                    study.addSeries(series);

                                    Map<String, TCReferencedImage> images = null;
                                    Map<String, Integer> instanceNumbers = ejb.getInstanceNumbers(suid);
                                    
                                    for (int k = 0; k < instanceCount; k++) {
                                        DicomObject instanceRef = instanceSeq
                                                .getDicomObject(k);
                                        String iuid = instanceRef
                                                .getString(Tag.ReferencedSOPInstanceUID);
                                        String cuid = instanceRef
                                                .getString(Tag.ReferencedSOPClassUID);
                                        Integer instanceNumber = instanceNumbers.get(iuid);
                                        
                                        if (TCReferencedInstance.isImage(cuid))
                                        {
                                        	TCReferencedImage image = new TCReferencedImage(series, iuid, cuid,
                                        			instanceNumber!=null?instanceNumber:-1);
                                        	series.addInstance(image);
                                        	
                                        	if (images==null)
                                        	{
                                        		images = new HashMap<String, TCReferencedImage>();
                                        	}
                                        	
                                        	images.put(iuid, image);
                                        }
                                        else
                                        {
                                        	series.addInstance(
                                                new TCReferencedInstance(series, iuid, cuid, 
                                                		instanceNumber!=null?instanceNumber:-1));
                                        }
                                    }
                                    
                                    if (images!=null && !images.isEmpty())
                                    {
                                        Map<String, Integer> frames = ejb.findMultiframeInstances(
                                        		stuid, suid, images.keySet().toArray(new String[0]));
                                        
                                        if (frames!=null && !frames.isEmpty())
                                        {
                                        	for (Map.Entry<String, Integer> me : frames.entrySet())
                                        	{
                                        		TCReferencedImage image = images.get(me.getKey());
                                        		series.removeInstance(image);
                                        		
                                        		for (int n=1; n<=me.getValue(); n++)
                                        		{
                                        			series.addInstance(new TCReferencedImage(
                                        					series, image.getInstanceUID(), image.getClassUID(), 
                                        					image.getInstanceNumber(), n));
                                        		}
                                        	}
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    if (instanceCount>0)
                    {
                        if (studyRefs==null)
                        {
                            studyRefs = new ArrayList<TCReferencedStudy>();
                        }
                        studyRefs.add(study);
                    }
                }
            }
        }
    }
    
    private String getTextValue(DicomObject object) {
        return object != null ? object.getString(Tag.TextValue) : null;
    }

    private TCDicomCode getCodeValue(DicomObject object) {
        return object == null ? null : object.contains(Tag.ConceptCodeSequence) ?
            new TCDicomCode(object.getNestedDicomObject(Tag.ConceptCodeSequence)) : null;
    }
    
    private Code toCode(TCDicomCode c) {
        return c == null ? null : c.toCode();
    }

    private String toStringValue(Object value, boolean shortString) {
        if (value instanceof List) {
            return concatStringValues((List<?>)value, shortString);
        }
        else if (value!=null) {
        	if (value instanceof ITextOrCode)
        	{
        		return shortString ? ((ITextOrCode)value).toShortString() :
        			((ITextOrCode)value).toLongString();
        	}
            return value.toString();
        }
        else {
            return null;
        }
    }
    
    protected boolean addKeywordImpl(ITextOrCode keyword) {
        if (keywords==null) {
            keywords = new ArrayList<ITextOrCode>(2);
        }
        if (!keywords.contains(keyword)) {
            return keywords.add(keyword);
        }
        return false;
    }
    
    protected boolean removeKeywordImpl(ITextOrCode keyword) {
        if (keywords!=null) {
            return keywords.remove(keyword);
        }
        return false;
    }
    
    protected static <T extends Object> T convertValue(Object v, Class<T> valueClass) throws IllegalArgumentException
    {
        return convertValue(v, valueClass, null);
    }
        
    @SuppressWarnings("unchecked")
    protected static <T extends Object> T convertValue(Object v, Class<T> valueClass, TCKeywordCatalogue cat) throws IllegalArgumentException
    {
        if (v==null)
        {
            return null;
        }
        else if (String.class.isAssignableFrom(valueClass))
        {
            if (TCDicomCode.class.isAssignableFrom(v.getClass()))
            {
                return (T) ((TCDicomCode)v).toString();
            }
            else if (ITextOrCode.class.isAssignableFrom(v.getClass())) {
                return (T) ((ITextOrCode)v).getText();
            }
            else
            {
                return (T) v.toString();
            }
        }
        else if (TCDicomCode.class.isAssignableFrom(valueClass))
        {
            if (TCDicomCode.class.isAssignableFrom(v.getClass()))
            {
                return (T) v;
            }
            else if (TCKeyword.class.isAssignableFrom(v.getClass()))
            {
                return (T) ((TCKeyword)v).getCode();
            }
            else if (ITextOrCode.class.isAssignableFrom(v.getClass())) {
                return (T) ((ITextOrCode)v).getCode();
            }
            else if (String.class.isAssignableFrom(v.getClass()) && cat!=null)
            {
                TCKeyword keyword = cat.findKeyword((String)v);
                
                TCDicomCode code = keyword!=null?keyword.getCode():null;
                
                if (code!=null)
                {
                    return (T) code;
                }
            }
        }
        else if (ITextOrCode.class.isAssignableFrom(valueClass)) {
            if (ITextOrCode.class.isAssignableFrom(v.getClass())) {
                return (T) v;
            }
            else if (TCDicomCode.class.isAssignableFrom(v.getClass())) {
                return (T) TextOrCode.code((TCDicomCode)v);
            }
            else if (String.class.isAssignableFrom(v.getClass())) {
                return (T) TextOrCode.text((String)v);
            }
        }
        else if (Enum.class.isAssignableFrom(valueClass))
        {
            if (valueClass.isAssignableFrom(v.getClass()))
            {
                return (T) v;
            }
            else
            {
                for (Object o : valueClass.getEnumConstants())
                {
                    if (((Enum<?>)o).name().equals(v.toString().trim()))
                    {
                        return (T) o;
                    }
                }
            }

            throw new IllegalArgumentException("Unable to convert enum (" + valueClass + "): No enum constant found for name '" + v.toString() + "'!"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
        else if (AcquisitionModality.class.isAssignableFrom(valueClass))
        {
        	if (valueClass.isAssignableFrom(v.getClass()))
        	{
        		return (T) v;
        	}
        	else
        	{
        		AcquisitionModality modality = AcquisitionModality.get(v.toString().trim());
        		if (modality!=null)
        		{
        			return (T) modality;
        		}
        	}
        }
        
        throw new IllegalArgumentException("Unable to convert from " + v.getClass() + " to " + valueClass); //$NON-NLS-1$ //$NON-NLS-2$
    }

    protected <T extends Object> List<T> convertValues(Object v, Class<T> valueClass) throws IllegalArgumentException
    {
        return convertValues(v, valueClass, null);
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected <T extends Object> List<T> convertValues(Object v, Class<T> valueClass, TCKeywordCatalogue cat) throws IllegalArgumentException
    {
        if (v==null)
        {
            return null;
        }
        else
        {
            Object[] values = null;
            
            if (v instanceof List)
            {
                values = new Object[((List<?>)v).size()];
                for (int i=0; i<values.length; i++)
                {
                    values[i] = convertValue(((List<?>)v).get(i), valueClass, cat);
                }
            }
            else
            {
                values = new Object[] {convertValue(v, valueClass, cat)};
            }
            
            return (List) Arrays.asList(values);
        }
    }
    
    
    public static class TextOrCode implements ITextOrCode
    {
        private static final long serialVersionUID = 1L;
        
        private String text;
        private TCDicomCode code;
        
        private TextOrCode(String text) {
            this.text = text;
        }
        private TextOrCode(TCDicomCode code) {
            this.code = code;
        }
        public static TextOrCode text(String text) {
            return new TextOrCode(text);
        }
        public static TextOrCode code(TCDicomCode code) {
            return new TextOrCode(code);
        }
        public String getText() {
            return text;
        }
        public TCDicomCode getCode() {
            return code;
        }
        public String toString() {
            return toShortString();
        }
        public String toShortString() {
        	return toString(true);
        }
        public String toLongString() {
        	return toString(false);
        }
        private String toString(boolean shortString) {
            if (code!=null) {
           		return shortString ? code.toShortString() :
           			code.toString();
            }
            else if (text!=null) {
                return text;
            }
            return "";
        }
    }
}
