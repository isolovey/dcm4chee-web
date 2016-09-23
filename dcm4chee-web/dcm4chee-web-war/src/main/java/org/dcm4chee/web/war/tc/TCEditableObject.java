package org.dcm4chee.web.war.tc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4che2.data.VR;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4chee.archive.entity.Code;
import org.dcm4chee.web.common.util.FileUtils;
import org.dcm4chee.web.dao.tc.ITextOrCode;
import org.dcm4chee.web.dao.tc.TCDicomCode;
import org.dcm4chee.web.dao.tc.TCQueryFilterKey;
import org.dcm4chee.web.dao.tc.TCQueryFilterValue.AcquisitionModality;
import org.dcm4chee.web.dao.tc.TCQueryFilterValue.Category;
import org.dcm4chee.web.dao.tc.TCQueryFilterValue.Level;
import org.dcm4chee.web.dao.tc.TCQueryFilterValue.PatientSex;
import org.dcm4chee.web.dao.tc.TCQueryFilterValue.YesNo;
import org.dcm4chee.web.war.folder.delegate.TarRetrieveDelegate;
import org.dcm4chee.web.war.tc.TCDocumentObject.MimeType;
import org.dcm4chee.web.war.tc.keywords.TCKeywordCatalogueProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TCEditableObject extends TCObject {
	
	private static final long serialVersionUID = 1L;

	private final static Logger log = LoggerFactory.getLogger(TCEditableObject.class);

    private DicomObject ds;
    private boolean modified;
    
    private Map<TCReferencedInstance, TCDocumentObject> docsAdded;
    private List<TCReferencedInstance> docsRemoved;
    private List<TCLink> linksRemoved;
    
    private TCEditableObject(String id, DicomObject ds)
    {
        super(id, ds);
        this.ds = ds;
    }
    
    public static TCEditableObject create(TCModel model) throws IOException {
    	DicomObject dataset = model.getDataset();
    	if (dataset.contains(Tag.ContentSequence)) {
    		return new TCEditableObject(model.getId(), dataset);
    	}
    	else {
	        String fsID = model.getFileSystemId();
	        String fileID = model.getFileId();
	
	        DicomInputStream dis = null;
	        try {
	        	dis = new DicomInputStream(fsID.startsWith("tar:") ? 
	        			TarRetrieveDelegate.getInstance().retrieveFileFromTar(fsID, fileID) :
	        				FileUtils.resolve(new File(fsID, fileID)));
	            return new TCEditableObject(model.getId(), dis.readDicomObject());
	        } finally {
	            if (dis != null) {
	                dis.close();
	            }
	        }
    	}
    }
    
    public boolean isModified()
    {
        return modified;
    }
    
    public void setAbstract(String abstr) {
        if (!TCUtilities.equals(this.abstr, abstr))
        {
            this.abstr = abstr;
            this.modified = true;
        }
    }

    public void setAcquisitionModalities(
            List<AcquisitionModality> acquisitionModalities) {
        if (!TCUtilities.equals(this.acquisitionModalities, acquisitionModalities))
        {
            this.acquisitionModalities = acquisitionModalities;
            this.modified = true;
        }
    }

    public void setAnatomy(ITextOrCode anatomy) {
        if (!TCUtilities.equals(this.anatomy, anatomy))
        {
            this.anatomy = anatomy;
            this.modified = true;
        }
    }

    public void setAuthorAffiliation(String authorAffiliation) {
        if (!TCUtilities.equals(this.authorAffiliation, authorAffiliation))
        {
            this.authorAffiliation = authorAffiliation;
            this.modified = true;
        }
    }

    public void setAuthorContact(String authorContact) {
        if (!TCUtilities.equals(this.authorContact, authorContact))
        {
            this.authorContact = authorContact;
            this.modified = true;
        }
    }

    public void setAuthorName(String authorName) {
        if (!TCUtilities.equals(this.authorName, authorName))
        {
            this.authorName = authorName;
            this.modified = true;
        }
    }

    public void setCategory(Category category) {
        if (!TCUtilities.equals(this.category, category))
        {
            this.category = category;
            this.modified = true;
        }
    }

    public void setDiagnosis(ITextOrCode diagnosis) {
        if (!TCUtilities.equals(this.diagnosis, diagnosis))
        {
            this.diagnosis = diagnosis;
            this.modified = true;
        }
    }

    public void setDiagnosisConfirmed(YesNo diagnosisConfirmed) {
        if (!TCUtilities.equals(this.diagnosisConfirmed, diagnosisConfirmed))
        {
            this.diagnosisConfirmed = diagnosisConfirmed;
            this.modified = true;
        }
    }

    public void setDiffDiagnosis(ITextOrCode diffDiagnosis) {
        if (!TCUtilities.equals(this.diffDiagnosis, diffDiagnosis))
        {
            this.diffDiagnosis = diffDiagnosis;
            this.modified = true;
        }
    }

    public void setDiscussion(String discussion) {
        if (!TCUtilities.equals(this.discussion, discussion))
        {
            this.discussion = discussion;
            this.modified = true;
        }
    }

    public void setFinding(ITextOrCode finding) {
        if (!TCUtilities.equals(this.finding, finding)) {
            this.finding = finding;
            this.modified = true;
        }
    }

    public void setHistory(String history) {
        if (!TCUtilities.equals(this.history, history)) {
            this.history = history;
            this.modified = true;
        }
    }
    
    public void setKeywords(List<ITextOrCode> keywords) {
        if (!TCUtilities.equals(this.keywords, keywords)) {
            this.keywords.clear();
            this.keywords.addAll(keywords);
            this.modified = true;
        }
    }
    
    public void setKeywordAt(int index, ITextOrCode keyword) {
        if (keywords!=null) {
            ITextOrCode old = this.keywords.get(index);
            if (old!=null) {
                if (!TCUtilities.equals(old, keyword)) {
                    this.keywords.set(index, keyword);
                    this.modified = true;
                }
            }
        }
    }
    
    public void addKeyword(ITextOrCode keyword) {
        if (addKeywordImpl(keyword))
        {
        	modified = true;
        }
    }

    public void removeKeyword(ITextOrCode keyword) {
        if (removeKeywordImpl(keyword))
        {
        	modified = true;
        }
    }

    public void setLevel(Level level) {
        if (!TCUtilities.equals(this.level, level))
        {
            this.level = level;
            this.modified = true;
        }
    }

    public void setOrganSystem(String organSystem) {
        if (!TCUtilities.equals(this.organSystem, organSystem))
        {
            this.organSystem = organSystem;
            this.modified = true;
        }
    }

    public void setPathology(ITextOrCode pathology) {
        if (!TCUtilities.equals(this.pathology, pathology))
        {
            this.pathology = pathology;
            this.modified = true;
        }
    }
    
    public void setPatientAge(Integer ageInDays) {
    	if (!TCUtilities.equals(this.patientAge, ageInDays)) {
    		this.patientAge = ageInDays;
    		this.modified = true;
    	}
    }

    public void setPatientSex(PatientSex patientSex) {
        if (!TCUtilities.equals(this.patientSex, patientSex))
        {
            this.patientSex = patientSex;
            this.modified = true;
        }
    }

    public void setPatientSpecies(String patientSpecies) {
        if (!TCUtilities.equals(this.patientSpecies, patientSpecies))
        {
            this.patientSpecies = patientSpecies;
            this.modified = true;
        }
    }

    public void setBibliographicReferences(List<String> bibliographicReferences) {
        if (!TCUtilities.equals(this.bibliographicReferences, bibliographicReferences))
        {
            
            this.bibliographicReferences.clear();
            this.bibliographicReferences.addAll(bibliographicReferences);
            this.modified = true;
        }
    }
        
    public void setBibliographicReference(int index, String ref) 
        throws IndexOutOfBoundsException
    {
        if (this.bibliographicReferences==null && index==0)
        {
            this.bibliographicReferences = new ArrayList<String>();
        }
        
        this.bibliographicReferences.set(index, ref);
        this.modified = true;
    }
    
    public void addBibliographicReference(String ref)
    {
        if (this.bibliographicReferences==null)
        {
            this.bibliographicReferences = new ArrayList<String>();
        }
        
        this.bibliographicReferences.add(ref);
        this.modified = true;
    }
    
    public void removeBibliographicReference(int index)
        throws IndexOutOfBoundsException
    {
        if (this.bibliographicReferences!=null)
        {
            this.bibliographicReferences.remove(index);
            this.modified = true;
        }
    }

    public void setTitle(String title) {
        if (!TCUtilities.equals(this.title, title))
        {
            this.title = title;
            this.modified = true;
        }
    }
    
    public boolean addLink(TCLink link) {
    	if (links==null || !links.contains(link)) {
    		TCEditableObject o = link.getLinkedCase();
    		if (o!=null) {
    			// first add crosslink
				o.addLinkImpl(TCLink.createCrossLink(link));
				
				//now add link
				if (addLinkImpl(link)) {
					return true;
				}
    		}
    	}
    	return false;
    }
        
    public boolean removeLink(TCLink link) {
    	if (links!=null && links.contains(link)) {
    		TCEditableObject o = link.getLinkedCase();
    		if (o!=null) {
    			// first remove the crosslink
    			TCLink crosslink = link.findCrossLink();
    			if (crosslink!=null) {
    				o.removeLinkImpl(crosslink);
    			}
    			
    			//now remove the link
    			if (removeLinkImpl(link)) {
    				return true;
    			}
    		}
    	}
    	return false;
    }
        
    @Override
    public TCDocumentObject getReferencedDocumentObject(TCReferencedInstance ref) {
		if (docsAdded!=null && docsAdded.containsKey(ref)) {
			return docsAdded.get(ref);
		}
    	return super.getReferencedDocumentObject(ref);
    }

    public TCDocumentObject addReferencedDocument(MimeType mimeType, String filename, InputStream in, String description) throws Exception {
    	TCDocumentObject doc = TCDocumentObject.create(this, mimeType, filename, in, description);
    	TCReferencedInstance instance = doc.getAsReferencedInstance();
    	
    	if (docsAdded==null) {
    		docsAdded = new HashMap<TCReferencedInstance, TCDocumentObject>(3);
    	}
    	
    	docsAdded.put(instance, doc);

    	addReferencedInstance(instance);
    	
    	modified = true;
    	
    	return doc;
    }
    
    public void removeReferencedDocument(TCDocumentObject doc) {
    	removeReferencedDocument(doc.getAsReferencedInstance());
    }
    
    public void removeReferencedDocument(TCReferencedInstance ref) {
   		removeReferencedInstance(ref);
   		
    	if (docsAdded!=null && docsAdded.containsKey(ref)) {
    		docsAdded.remove(ref);
			if (docsAdded.isEmpty()) {
				docsAdded = null;
			}
    	}
    	else {
    		if (docsRemoved==null) {
    			docsRemoved = new ArrayList<TCReferencedInstance>(3);
    		}
    		docsRemoved.add(ref);
    		modified = true;
    	}
    }
    
    public void setValue(TCQueryFilterKey key, Object value)
    {
        try
        {
            if (TCQueryFilterKey.Abstract.equals(key)) {
                setAbstract(convertValue(value, String.class));
            } else if (TCQueryFilterKey.AcquisitionModality.equals(key)) {
                setAcquisitionModalities(convertValues(value, AcquisitionModality.class));
            } else if (TCQueryFilterKey.Anatomy.equals(key)) {
                setAnatomy(convertValue(value, ITextOrCode.class));
            } else if (TCQueryFilterKey.AuthorAffiliation.equals(key)) {
                setAuthorAffiliation(convertValue(value, String.class));
            } else if (TCQueryFilterKey.AuthorContact.equals(key)) {
                setAuthorContact(convertValue(value, String.class));
            } else if (TCQueryFilterKey.AuthorName.equals(key)) {
                setAuthorName(convertValue(value, String.class));
            } else if (TCQueryFilterKey.BibliographicReference.equals(key)) {
                setBibliographicReferences(convertValues(value, String.class));
            } else if (TCQueryFilterKey.Category.equals(key)) {
                setCategory(convertValue(value, Category.class));
            } else if (TCQueryFilterKey.DiagnosisConfirmed.equals(key)) {
                setDiagnosisConfirmed(convertValue(value, YesNo.class));
            } else if (TCQueryFilterKey.Diagnosis.equals(key)) {
                setDiagnosis(convertValue(value, ITextOrCode.class));
            } else if (TCQueryFilterKey.DifferentialDiagnosis.equals(key)) {
                setDiffDiagnosis(convertValue(value, ITextOrCode.class));
            } else if (TCQueryFilterKey.Discussion.equals(key)) {
                setDiscussion(convertValue(value, String.class));
            } else if (TCQueryFilterKey.Finding.equals(key)) {
                setFinding(convertValue(value, ITextOrCode.class));
            } else if (TCQueryFilterKey.History.equals(key)) {
                setHistory(convertValue(value, String.class));
            } else if (TCQueryFilterKey.Keyword.equals(key)) {
                setKeywords(convertValues(value, ITextOrCode.class));
            } else if (TCQueryFilterKey.Level.equals(key)) {
                setLevel(convertValue(value, Level.class));
            } else if (TCQueryFilterKey.OrganSystem.equals(key)) {
                setOrganSystem(convertValue(value, String.class));
            } else if (TCQueryFilterKey.Pathology.equals(key)) {
                setPathology(convertValue(value, ITextOrCode.class));
            } else if (TCQueryFilterKey.PatientSex.equals(key)) {
                setPatientSex(convertValue(value, PatientSex.class));
            } else if (TCQueryFilterKey.PatientSpecies.equals(key)) {
          		setPatientSpecies(convertValue(value, String.class));
            } else if (TCQueryFilterKey.Title.equals(key)) {
                setTitle(convertValue(value, String.class));
            }
        }
        catch (IllegalArgumentException e)
        {
            log.error("Assigning value to teaching-file key '" + key + "' failed!", e);
        }
    }
    
    public SaveResult save() throws Exception {
    	return save(true);
    }
    
    private SaveResult save(boolean saveLinkedCases) throws Exception {
    	if (modified) {
    		List<String> otherCaseUIDs = null;
    		
	        //we may need to update cross linked cases as well
    		if (saveLinkedCases) {
    			List<TCLink> linksToSave = new ArrayList<TCLink>(3);
    			
    			// save added crosslinks
	    		if (links!=null) {
	    			linksToSave.addAll( links );
	    		}
	    		
	    		// save removed crosslinks
	    		if (linksRemoved!=null) {
	    			linksToSave.addAll( linksRemoved );
	    		}
	    		
    			for (TCLink link : linksToSave) {
    				TCEditableObject linkedCase = link.getLinkedCase();
    				if (linkedCase!=null) {
    					if (linkedCase.isModified()) {
    						if (linkedCase.save(false).saved()) {
    							if (otherCaseUIDs==null) {
    								otherCaseUIDs = new ArrayList<String>(3);
    							}
    							otherCaseUIDs.add(linkedCase.getInstanceUID());
    						}
    					}
    				}
    			}
    		}

	        //save new added (yet unsaved) referenced documents
	        if (docsAdded!=null) {
	        	for (TCDocumentObject doc : docsAdded.values()) {
	        		DicomObject o = doc.toDataset();
	        		if (!TCStoreDelegate.getInstance().storeImmediately(o)) {
	        			throw new Exception("Saving teaching-file case failed: New added referenced document can't be saved!");
	        		}
	        	}
	        }
	        
	        //delete removed referenced documents
	        if (docsRemoved!=null) {
	        	for (TCReferencedInstance doc : docsRemoved) {
	        		if (!TCStoreDelegate.getInstance().deleteImmediately(
		            		doc.getStudyUID(),
		            		doc.getSeriesUID(),
		            		doc.getInstanceUID(),
		            		doc.getClassUID())) {
	        			throw new Exception("Saving teaching-file case failed: Removed referenced document can't be deleted!");
	        		}
	        	}
	        }
	        
	        //now save this case
	    	if (TCStoreDelegate.getInstance().modifyImmediately(
	    			toModificationDataset())) {
    			modified = false;
    			
    			return new SaveResult(true, getInstanceUID(), otherCaseUIDs);
    		}
    	}
    	return new SaveResult(false, getInstanceUID(), null);
    }
    
    @Override
    protected void init(String id, DicomObject o) {
    	super.init(id, o);
    	
    	this.ds = o;
    }
    
    @Override
    protected void clear() {
    	super.clear();

    	docsRemoved = null;
    	docsAdded = null;
    	ds = null;
    	modified = false;
    }
    
    private boolean addLinkImpl(TCLink link) {
    	if (links==null) {
    		links = new ArrayList<TCLink>(5);
    	}
    	if (!links.contains(link)) {
    		if (links.add(link)) {
    			if (linksRemoved!=null) {
    				for (Iterator<TCLink> it = linksRemoved.iterator(); it.hasNext();) {
    					if (it.next().equals(link)) {
    						it.remove();
    					}
    				}
    				if (linksRemoved.isEmpty()) {
    					linksRemoved = null;
    				}
    			}
    			modified = true;
    			return true;
    		}
    	}
    	return false;
    }
    
    private boolean removeLinkImpl(TCLink link) {
    	if (links!=null) {
    		if (links.remove(link)) {
    			if (links.isEmpty()) {
    				links = null;
    			}
    			if (linksRemoved==null) {
    				linksRemoved = new ArrayList<TCLink>(3);
    			}
    			linksRemoved.add(link);
    			modified = true;
				return true;
    		}
    	}
    	return false;
    }
    
    private DicomObject toModificationDataset()
    {
        BasicDicomObject ds = new BasicDicomObject();
                
        ds.putString(Tag.SOPInstanceUID, VR.UI, getInstanceUID());
        ds.putString(Tag.SOPClassUID, VR.UI, getClassUID());
        
        //proprietary; used to show some useful info in the GUI when doing c-find
        ds.putString(Tag.ContentLabel, null, getTitle());
        ds.putString(Tag.ContentDescription, null, getAbstr());
        ds.putString(Tag.ContentCreatorName, null, getAuthorName());
        
        // Documents have been added or removed.
        // Thus, we just can't copy the original dataset.
        // Instead we need to update the sequence
        // of image/document references accordingly.
        if ( (docsRemoved!=null && docsRemoved.size()>0) ||
        	  (docsAdded!=null && docsAdded.size()>0) ) {
        	// get/set sequence
        	DicomElement refs = this.ds.get(Tag.CurrentRequestedProcedureEvidenceSequence);
        	if (refs!=null) {
        		ds.add(refs);
        		refs = ds.get(Tag.CurrentRequestedProcedureEvidenceSequence);
        	}
        	
        	DicomObject seriesItem = null;
        	
        	// add new document references
        	if (docsAdded!=null && !docsAdded.isEmpty()) {
	        	if (refs==null) {
	        		refs = ds.putSequence(Tag.CurrentRequestedProcedureEvidenceSequence);
	        	}
	        	
	        	for (TCReferencedInstance doc : docsAdded.keySet()) {
	        		String suid = doc.getSeriesUID();
	        		
	        		if (seriesItem==null || !suid.equals(
	        				seriesItem.getString(Tag.SeriesInstanceUID))) {
	        			seriesItem = findSeriesItem(refs, doc.getStudyUID(), suid, true);
	        		}
	        		
	        		DicomObject ref = new BasicDicomObject();
	        		ref.putString(Tag.ReferencedSOPInstanceUID, VR.UI, doc.getInstanceUID());
	        		ref.putString(Tag.ReferencedSOPClassUID, VR.UI, doc.getClassUID());
	        		
	        		DicomElement seq = seriesItem.get(Tag.ReferencedSOPSequence);
	        		seq.addDicomObject(ref);
	        	}
        	}
        	
        	// remove document references
        	if (refs!=null && refs.countItems()>0 &&
        			docsRemoved!=null && !docsRemoved.isEmpty()) {
        		for (TCReferencedInstance doc : docsRemoved) {
	        		String suid = doc.getSeriesUID();
	        		String stuid = doc.getStudyUID();
	        		
	        		if (seriesItem==null || !suid.equals(
	        				seriesItem.getString(Tag.SeriesInstanceUID))) {
	        			seriesItem = findSeriesItem(refs, stuid, suid, false);
	        		}
	        		
	        		if (seriesItem!=null) {
	        			// remove referenced SOP instance from SOP sequence
	        			DicomElement seq = seriesItem.get(Tag.ReferencedSOPSequence);
	        			if (seq!=null) {
	        				int r = -1;
	        				for (int i=0; i<seq.countItems(); i++) {
	        					DicomObject o = seq.getDicomObject(i);
	        					if (o.getString(Tag.ReferencedSOPInstanceUID).equals(
	        							doc.getInstanceUID())) {
	        						r = i;
	        						break;
	        					}
	        				}
	        				if (r>=0) {
	        					seq.removeDicomObject(r);
	        				}
	        			}
	        			
	        			if (seq==null || seq.countItems()<=0) {
	        				DicomObject studyItem = findStudyItem(refs, stuid);
	        				if (studyItem!=null) {
	        					// remove referenced series from series sequence 
		        				DicomElement refSeries = studyItem.get(Tag.ReferencedSeriesSequence);
		        				if (refSeries!=null) {
		        					refSeries.removeDicomObject(seriesItem);
		        					seriesItem = null;
		        				}
		        				
		        				// remove study from study sequence
		        				if (refSeries==null || refSeries.countItems()<=0) {
		        					refs.removeDicomObject(studyItem);
		        				}
	        				}
	        			}
	        		}
        		}
        	}
        }

        // recompile content
        DicomElement content = ds.putSequence(Tag.ContentSequence); //add new content
        
        //author name
        String authorName = getAuthorName();
        String authorAffiliation = getAuthorAffiliation();
        String authorContact = getAuthorContact();
        
        if (authorName!=null && !authorName.isEmpty())
        {
            DicomObject author = createTextContent(TCQueryFilterKey.AuthorName, authorName);
            
            //conforming to IHE TCE, author affiliation and contact are
            //nested elements of the author element
            if ((authorAffiliation!=null && !authorAffiliation.isEmpty()) ||
                (authorContact!=null && !authorContact.isEmpty()))
            {
                DicomElement authorContent = author.putSequence(Tag.ContentSequence);
                
                if (authorAffiliation!=null && !authorAffiliation.isEmpty())
                {
                    authorContent.addDicomObject(createTextContent(
                            TCQueryFilterKey.AuthorAffiliation, authorAffiliation, "HAS PROPERTIES"));
                }
                if (authorContact!=null && !authorContact.isEmpty())
                {
                    authorContent.addDicomObject(createTextContent(
                            TCQueryFilterKey.AuthorContact, authorContact, "HAS PROPERTIES"));
                }
            }
            
            content.addDicomObject(author);
        }
        
        //author affiliation
        if (authorAffiliation!=null && !authorAffiliation.isEmpty())
        {
            content.addDicomObject(
                    createTextContent(TCQueryFilterKey.AuthorAffiliation, authorAffiliation));
        }
        
        //author contact
        if (authorContact!=null && !authorContact.isEmpty())
        {
            content.addDicomObject(
                    createTextContent(TCQueryFilterKey.AuthorContact, authorContact));
        }

        //abstract
        String abstr = getAbstr();
        if (abstr!=null && !abstr.isEmpty())
        {
            content.addDicomObject(
                    createTextContent(TCQueryFilterKey.Abstract, abstr));
        }
                
        //anatomy
        ITextOrCode anatomy = getAnatomy();
        if (anatomy!=null)
        {
            DicomObject o = createTextOrCodeContent(TCQueryFilterKey.Anatomy, anatomy);
            if (o!=null) {
                content.addDicomObject(o);
            }
        }
        
        //pathology
        ITextOrCode pathology = getPathology();
        if (pathology!=null)
        {
            DicomObject o = createTextOrCodeContent(TCQueryFilterKey.Pathology, pathology);
            if (o!=null) {
                content.addDicomObject(o);
            }
        }
        
        //keywords
        List<ITextOrCode> keywords = getKeywords();
        if (keywords!=null)
        {
            for (ITextOrCode keyword : keywords)
            {
                DicomObject o = createTextOrCodeContent(TCQueryFilterKey.Keyword, keyword);
                if (o!=null) {
                    content.addDicomObject(o);
                }
            }
        }
        
        //history
        String history = getHistory();
        if (history!=null && !history.isEmpty())
        {
            content.addDicomObject(
                    createTextContent(TCQueryFilterKey.History, history));
        }
        
        //finding
        ITextOrCode finding = getFinding();
        if (finding!=null)
        {
            DicomObject o = createTextOrCodeContent(TCQueryFilterKey.Finding, finding);
            if (o!=null) {
                content.addDicomObject(o);
            }
        }
        
        //discussion
        String discussion = getDiscussion();
        if (discussion!=null && !discussion.isEmpty())
        {
            content.addDicomObject(
                    createTextContent(TCQueryFilterKey.Discussion, discussion));
        }
        
        //diff.-diagnosis
        ITextOrCode diffDiagnosis = getDiffDiagnosis();
        if (diffDiagnosis!=null)
        {
            DicomObject o = createTextOrCodeContent(TCQueryFilterKey.DifferentialDiagnosis, diffDiagnosis);
            if (o!=null) {
                content.addDicomObject(o);
            }
        }
        
        //diagnosis
        ITextOrCode diagnosis = getDiagnosis();
        if (diagnosis!=null)
        {
            DicomObject o = createTextOrCodeContent(TCQueryFilterKey.Diagnosis, diagnosis);
            if (o!=null) {
                content.addDicomObject(o);
            }
        }
        
        //diagnosis confirmed
        YesNo diagConfirmed = getDiagnosisConfirmed();
        if (diagConfirmed!=null)
        {
            content.addDicomObject(
                    createCodeContent(TCQueryFilterKey.DiagnosisConfirmed, diagConfirmed.getCode()));
        }
        
        //organ-system
        String organSystem = getOrganSystem();
        if (organSystem!=null && !organSystem.isEmpty())
        {
            content.addDicomObject(
                    createTextContent(TCQueryFilterKey.OrganSystem, organSystem));
        }
        
        //modalities
        List<AcquisitionModality> modalities = getAcquisitionModalities();
        if (modalities!=null && !modalities.isEmpty())
        {
            for (AcquisitionModality modality : modalities)
            {
                if (modality.getCode()!=null)
                {
                    content.addDicomObject(
                            createCodeContent(TCQueryFilterKey.AcquisitionModality, modality.getCode()));
                }
                else if (modality.getString()!=null)
                {
                	content.addDicomObject(createTextContent(
                			TCQueryFilterKey.AcquisitionModality, modality.getString()));
                }
            }
        }
        
        //category
        Category category = getCategory();
        if (category!=null)
        {
            content.addDicomObject(
                    createCodeContent(TCQueryFilterKey.Category, category.getCode()));
        }
        
        //level
        Level level = getLevel();
        if (level!=null)
        {
            content.addDicomObject(
                    createCodeContent(TCQueryFilterKey.Level, level.getCode()));
        }
        
        //patient age
        Integer patientAge = getPatientAge();
        if (patientAge!=null)
        {
            content.addDicomObject(
                    createTextContent(TCQueryFilterKey.PatientAge, Integer.toString(patientAge)));
        }
        
        //patient sex
        PatientSex patientSex = getPatientSex();
        if (patientSex!=null)
        {
            content.addDicomObject(
                    createTextContent(TCQueryFilterKey.PatientSex, patientSex.getString()));
        }
        
        //patient race
        String patientSpecies = getPatientSpecies();
        if (patientSpecies!=null && !patientSpecies.isEmpty())
        {
            content.addDicomObject(
                    createTextContent(TCQueryFilterKey.PatientSpecies, patientSpecies));
        }
        
        //title
        String title = getTitle();
        if (title!=null && !title.isEmpty())
        {
            content.addDicomObject(
                    createTextContent(TCQueryFilterKey.Title, title));
        }
        
        //bibliographic references
        List<String> bibliographicReferences = getBibliographicReferences();
        if (bibliographicReferences!=null && !bibliographicReferences.isEmpty())
        {
            for (String bibliographicReference : bibliographicReferences)
            {
                content.addDicomObject(
                        createTextContent(TCQueryFilterKey.BibliographicReference, bibliographicReference));
            }
        }
        
        //case links
        List<TCLink> links = getLinks();
        if (links!=null) {
        	for (TCLink link : links) {
	        	DicomObject container = new BasicDicomObject();
	        	container.putString(Tag.RelationshipType, null, "CONTAINS");
	        	container.putString(Tag.ValueType, null, "CONTAINER");
	        	container.putNestedDicomObject(Tag.ConceptNameCodeSequence, 
	        			TCDicomCode.REF_COMPOSITE_CONTAINER.toCode().toCodeItem());
	        	DicomElement content2 = container.putSequence(Tag.ContentSequence);
	        	
        		DicomObject refsop = new BasicDicomObject();
        		refsop.putString(Tag.ReferencedSOPClassUID, null, UID.BasicTextSRStorage);
        		refsop.putString(Tag.ReferencedSOPInstanceUID, null, link.getLinkedCaseUID());

	            DicomObject composite = new BasicDicomObject();
	            composite.putString(Tag.RelationshipType, null, "CONTAINS");
	            composite.putString(Tag.ValueType, null, "COMPOSITE");
	            composite.putNestedDicomObject(Tag.ReferencedSOPSequence, refsop);
	            composite.putNestedDicomObject(Tag.ConceptNameCodeSequence,
	            		TCDicomCode.REF_COMPOSITE_OBJECT.toCode().toCodeItem());
	            content2.addDicomObject(composite);
	            
	            DicomObject reltype = new BasicDicomObject();
	            reltype.putString(Tag.RelationshipType, null, "HAS CONCEPT MOD");
	            reltype.putString(Tag.ValueType, null, "TEXT");
	            reltype.putString(Tag.TextValue, null, link.getLinkRelationship().name());
	            reltype.putNestedDicomObject(Tag.ConceptNameCodeSequence, 
	            		TCDicomCode.REF_COMPOSITE_RELATIONSHIP_TYPE.toCode().toCodeItem());
	            content2.addDicomObject(reltype);
	            
	            String commentText = link.getLinkComment();
	            if (commentText!=null && !commentText.isEmpty()) {
	            	DicomObject comment = new BasicDicomObject();
	            	comment.putString(Tag.RelationshipType, null, "CONTAINS");
	            	comment.putString(Tag.ValueType, null, "TEXT");
	            	comment.putString(Tag.TextValue, null, commentText);
	            	comment.putNestedDicomObject(Tag.ConceptNameCodeSequence,
	            			TCDicomCode.REF_COMPOSITE_COMMENT.toCode().toCodeItem());
	            	content2.addDicomObject(comment);
	            }
	            
	            content.addDicomObject(container);
        	}
        }
        
        return ds;
    }

    private DicomObject createTextContent(TCQueryFilterKey key, String text)
    {
        return createTextContent(key, text, "CONTAINS");
    }
    
    private DicomObject createTextContent(TCQueryFilterKey key, String text, String relationshipType)
    {
        DicomObject ds = new BasicDicomObject();
        ds.putString(Tag.RelationshipType, null, relationshipType);
        ds.putString(Tag.ValueType, null, "TEXT");
        ds.putNestedDicomObject(Tag.ConceptNameCodeSequence, key.getCode().toCodeItem());
        ds.putString(Tag.TextValue, null, text);
        return ds;
    }
    
    private DicomObject createCodeContent(TCQueryFilterKey key, TCDicomCode code)
    {
        return createCodeContent(key, code.toCode());
    }
    
    private DicomObject createCodeContent(TCQueryFilterKey key, Code code)
    {
        DicomObject ds = new BasicDicomObject();
        ds.putString(Tag.RelationshipType, null, "CONTAINS");
        ds.putString(Tag.ValueType, null, "CODE");
        ds.putNestedDicomObject(Tag.ConceptNameCodeSequence, key.getCode().toCodeItem());
        ds.putNestedDicomObject(Tag.ConceptCodeSequence, code.toCodeItem());
        return ds;
    }
    
    private DicomObject createTextOrCodeContent(TCQueryFilterKey key, ITextOrCode value) {
        if (value!=null) {
            String text = value.getText();
            TCDicomCode code = value.getCode();
            
            if (code!=null) {
                TCKeywordCatalogueProvider prov = TCKeywordCatalogueProvider.getInstance();   
                if (prov!=null && prov.hasCatalogue(key)) {
                    return createCodeContent(key, code);
                }
                else {
                    return createTextContent(key, code.toString());
                }
            }
            else if (text!=null && !text.trim().isEmpty()) {
                return createTextContent(key, text.trim());
            }
        }
        return null;
    }
    
    private DicomObject findStudyItem(DicomElement evidenceSeq, String stuid) {
    	int refCount = evidenceSeq != null ? evidenceSeq.countItems() : -1;
        if (refCount > 0) {
            for (int i = 0; i < refCount; i++) {
                DicomObject studyRef = evidenceSeq.getDicomObject(i);
                String stuid_ = studyRef != null ? studyRef
                        .getString(Tag.StudyInstanceUID) : null;

                if (stuid_!=null && stuid_.equals(stuid)) {
                	return studyRef;
                }
            }
        }
        return null;
    }
    
    private DicomObject findSeriesItem(DicomElement evidenceSeq, String stuid, String suid, boolean create) {
        DicomObject studyItem = findStudyItem(evidenceSeq, stuid);
        if (studyItem!=null) {
        	DicomElement seriesSeq = studyItem.get(Tag.ReferencedSeriesSequence);
        	int seriesCount = seriesSeq != null ? seriesSeq
        			.countItems() : -1;
        	if (seriesCount > 0) {
        		for (int j = 0; j < seriesCount; j++) {
        			DicomObject seriesRef = seriesSeq.getDicomObject(j);
        			String suid_ = seriesRef != null ? seriesRef.getString(
        							Tag.SeriesInstanceUID) : null;
        			if (suid_!=null && suid_.equals(suid)) {
        				return seriesRef;
        			}
        		}
        	}
        }
        
        if (create) {
        	if (studyItem==null) {
				studyItem = new BasicDicomObject();
				studyItem.putString(Tag.StudyInstanceUID, VR.UI, stuid);
				studyItem.putSequence(Tag.ReferencedSeriesSequence);
				evidenceSeq.addDicomObject(studyItem);
        	}
        	
			DicomObject seriesItem = new BasicDicomObject();
			seriesItem.putString(Tag.SeriesInstanceUID, VR.UI, suid);
			seriesItem.putSequence(Tag.ReferencedSOPSequence);
			
			DicomElement refSeries = studyItem.get(Tag.ReferencedSeriesSequence);
			refSeries.addDicomObject(seriesItem);
			
			return seriesItem;
        }

        return null;
    }
    
    
    public static class SaveResult {
    	private boolean saved;
    	private String caseUID;
    	private List<String> otherCaseUIDs;
    	
    	public SaveResult(boolean saved, String caseUID, List<String> otherCaseUIDs) {
    		this.saved = saved;
    		this.caseUID = caseUID;
    		this.otherCaseUIDs = otherCaseUIDs;
    	}
    	
    	public boolean saved() {
    		return saved;
    	}
    	
    	public String getCaseUID() {
    		return caseUID;
    	}
    	
    	public List<String> getOtherCaseUIDs() {
    		if (otherCaseUIDs!=null) {
    			return Collections.unmodifiableList(otherCaseUIDs);
    		}
    		return Collections.emptyList();
    	}
    }
    
}
