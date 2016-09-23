/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 * Java(TM), hosted at http://sourceforge.net/projects/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * Agfa-Gevaert AG.
 * Portions created by the Initial Developer are Copyright (C) 2008
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See listed authors below.
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.dcm4chee.web.dao.common;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.VR;
import org.dcm4chee.archive.common.Availability;
import org.dcm4chee.archive.common.PrivateTag;
import org.dcm4chee.archive.common.PublishedStudyStatus;
import org.dcm4chee.archive.common.StorageStatus;
import org.dcm4chee.archive.entity.BaseEntity;
import org.dcm4chee.archive.entity.File;
import org.dcm4chee.archive.entity.Instance;
import org.dcm4chee.archive.entity.MPPS;
import org.dcm4chee.archive.entity.MWLItem;
import org.dcm4chee.archive.entity.OtherPatientID;
import org.dcm4chee.archive.entity.Patient;
import org.dcm4chee.archive.entity.PrivateFile;
import org.dcm4chee.archive.entity.PrivateInstance;
import org.dcm4chee.archive.entity.PrivatePatient;
import org.dcm4chee.archive.entity.PrivateSeries;
import org.dcm4chee.archive.entity.PrivateStudy;
import org.dcm4chee.archive.entity.PublishedStudy;
import org.dcm4chee.archive.entity.Series;
import org.dcm4chee.archive.entity.Study;
import org.dcm4chee.archive.entity.StudyOnFileSystem;
import org.dcm4chee.web.dao.util.IOCMUtil;
import org.dcm4chee.web.dao.util.QueryUtil;
import org.dcm4chee.web.dao.util.UpdateDerivedFieldsUtil;
import org.dcm4chee.web.dao.vo.EntityTree;
import org.jboss.annotation.ejb.LocalBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Franz Willer <fwiller@gmail.com>
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision$ $Date$
 * @since Feb 01, 2010
 */

@Stateless
@LocalBinding (jndiBinding=DicomEditLocal.JNDI_NAME)
public class DicomEditBean implements DicomEditLocal {

    private static final int DELETED = 1;
    
    private static Logger log = LoggerFactory.getLogger(DicomEditBean.class);   
            
    private UpdateDerivedFieldsUtil updateUtil;
    
    @PersistenceContext(unitName="dcm4chee-arc")
    private EntityManager em;

    private UpdateDerivedFieldsUtil getUpdateDerivedFieldsUtil() {
        if (updateUtil == null) {
            updateUtil = new UpdateDerivedFieldsUtil(em);
        }
        return updateUtil;
    }
    
    @SuppressWarnings("unchecked")
    public EntityTree moveInstancesToTrash(long[] pks, boolean trustPatientIdWithoutIssuer) {
        Query q = QueryUtil.getQueryForPks(em, "SELECT OBJECT(i) FROM Instance i WHERE i.pk ", pks);
        return moveInstancesToTrash(q.getResultList(), true, trustPatientIdWithoutIssuer);
    }

    @SuppressWarnings("unchecked")
    public EntityTree moveInstanceToTrash(String iuid, boolean trustPatientIdWithoutIssuer) {
        Query q = em.createNamedQuery("Instance.findByIUID");
        q.setParameter("iuid", iuid.trim());
        return moveInstancesToTrash(q.getResultList(), true, trustPatientIdWithoutIssuer);
    }
    public EntityTree moveInstancesToTrash(Collection<Instance> instances, boolean deleteInstance, boolean trustPatientIdWithoutIssuer) {
        return moveInstancesToTrash(instances, deleteInstance, null, trustPatientIdWithoutIssuer);
    }    
    public EntityTree moveInstancesToTrash(Collection<Instance> instances, boolean deleteInstance, EntityTree entityTree, boolean trustPatientIdWithoutIssuer) {
        log.debug("Move {} instances to trash!",instances.size());
        Set<Study> studies = new HashSet<Study>();
        for ( Instance instance : instances) {
            moveInstanceToTrash(instance, trustPatientIdWithoutIssuer);
            if (deleteInstance) {
                if (studies.add(instance.getSeries().getStudy()))
                    markPublishedStudyChanged(instance.getSeries().getStudy());
                log.debug("Delete Instance:{}",instance.getAttributes(false));
                em.remove(instance);
            }

        }
        if (deleteInstance) {
            removeInstancesFromMpps(instances);
            for (Study st : studies) {
                getUpdateDerivedFieldsUtil().updateDerivedFieldsOfStudy(st);
            }
        }
        return entityTree == null ? new EntityTree(instances) : entityTree.addInstances(instances);
    }

    private void markPublishedStudyChanged(Study study) {
        Query qry = em.createNamedQuery("PublishedStudy.findByStudyPkAndStatus");
        qry.setParameter("studyPk", study.getPk());
        qry.setParameter("status", PublishedStudyStatus.STUDY_COMPLETE);
        @SuppressWarnings("unchecked")
        List<PublishedStudy> pStudies = qry.getResultList();
        for (PublishedStudy pStudy : pStudies) {
            pStudy.setStatus(PublishedStudyStatus.STUDY_CHANGED);
            em.merge(pStudy);
        }
    }

    private void markPublishedStudyDeleted(Study st) {
        Query q = em.createQuery("select object(s) from PublishedStudy s where s.study.pk = :studyPk");
        q.setParameter("studyPk", st.getPk());
        for ( PublishedStudy p : (List<PublishedStudy>)q.getResultList()) {
            if (p.getStatus() == PublishedStudyStatus.DEPRECATED) {
                em.remove(p);
            } else {
                p.setStudy(null);
                em.merge(p);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    public EntityTree moveSeriesToTrash(long[] pks, boolean trustPatientIdWithoutIssuer) {
        Query q;
        q = QueryUtil.getQueryForPks(em, "SELECT OBJECT(s) FROM Series s WHERE pk ", pks);
        return moveSeriesToTrash(q.getResultList(), true, null, trustPatientIdWithoutIssuer);
    }

    @SuppressWarnings("unchecked")
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public EntityTree moveSeriesToTrash(String iuid, boolean trustPatientIdWithoutIssuer) {
        Query q = em.createQuery("SELECT OBJECT(s) FROM Series s WHERE seriesInstanceUID = :iuid")
            .setParameter("iuid", iuid.trim());
        return this.moveSeriesToTrash(q.getResultList(), true, null, trustPatientIdWithoutIssuer);
    }

    private EntityTree moveSeriesToTrash(Collection<Series> series, boolean deleteSeries, EntityTree entityTree, boolean trustPatientIdWithoutIssuer) {
        Set<Instance> instances;
        Study study;
        Set<Study> studies = new HashSet<Study>();
        for (Series s : series) {
            instances = s.getInstances();
            if (instances.isEmpty()) {
                log.info("move empty series to trash:{}",s.getSeriesInstanceUID());
                this.moveSeriesToTrash(s, trustPatientIdWithoutIssuer);
            } else {
                entityTree = moveInstancesToTrash(instances, false, entityTree, trustPatientIdWithoutIssuer);
            }
            MPPS mpps = s.getModalityPerformedProcedureStep();
            if (mpps!=null) mpps.getAccessionNumber();//initialize MPPS
            if (deleteSeries) {
                removeSeriesFromMPPS(mpps, s.getSeriesInstanceUID());
                if (studies.add(study = s.getStudy()))
                    markPublishedStudyChanged(s.getStudy());
                em.remove(s);
                study.getSeries().remove(s);
            }
        }
        if (deleteSeries) {
            for (Study st : studies) {
                getUpdateDerivedFieldsUtil().updateDerivedFieldsOfStudy(st);
            }
        }
        return entityTree == null ? new EntityTree() : entityTree;
    }

    @SuppressWarnings("unchecked")
    public EntityTree moveSeriesOfPpsToTrash(long[] pks, boolean trustPatientIdWithoutIssuer)
    {
        Query q = QueryUtil.getQueryForPks(em, "SELECT OBJECT(p) FROM MPPS p WHERE pk ", pks);
        Query qs = QueryUtil.getQueryForPks(em, "SELECT OBJECT(s) FROM Series s WHERE s.modalityPerformedProcedureStep.pk ", pks);
        List<Series> seriess = qs.getResultList();
        EntityTree tree = moveSeriesToTrash(seriess, true, null, trustPatientIdWithoutIssuer);
        List<MPPS> mppss = q.getResultList();
        for(MPPS mpps : mppss) {
            em.remove(mpps);
        }
        return tree;
    }
    
    @SuppressWarnings("unchecked")
    public EntityTree moveStudiesToTrash(long[] pks, boolean trustPatientIdWithoutIssuer) {
        Query q = QueryUtil.getQueryForPks(em, "SELECT OBJECT(s) FROM Study s WHERE pk ", pks);
        return moveStudiesToTrash(q.getResultList(), null, trustPatientIdWithoutIssuer);
    }
    
    @SuppressWarnings("unchecked")
    public EntityTree moveStudyToTrash(String iuid, boolean trustPatientIdWithoutIssuer) {
        Query q = em.createQuery("SELECT OBJECT(s) FROM Study s WHERE studyInstanceUID = :iuid")
            .setParameter("iuid", iuid.trim());
        return this.moveStudiesToTrash(q.getResultList(), null, trustPatientIdWithoutIssuer);
    }

    @SuppressWarnings("unchecked")
    private EntityTree moveStudiesToTrash(Collection<Study> studies, EntityTree entityTree, boolean trustPatientIdWithoutIssuer) {
        Set<Series> series;
        for (Study st : studies) {
            series = st.getSeries();
            if (series.isEmpty()) {
                log.info("move empty study to trash:{}",st.getStudyInstanceUID());
                this.moveStudyToTrash(st, trustPatientIdWithoutIssuer);
            } else {
                entityTree = moveSeriesToTrash(series, false, entityTree, trustPatientIdWithoutIssuer);
            }
            log.debug("Delete Study:{}",st.getAttributes(false));
            Query q = em.createQuery("SELECT OBJECT(sof) FROM StudyOnFileSystem sof WHERE study_fk = :pk");
            q.setParameter("pk", st.getPk());
            for ( StudyOnFileSystem sof : (List<StudyOnFileSystem>)q.getResultList()) {
                em.remove(sof);
            }
            markPublishedStudyDeleted(st);
            
            em.remove(st);
        }
        return entityTree == null ? new EntityTree() : entityTree;
    }

    @SuppressWarnings("unchecked")
    public EntityTree movePatientsToTrash(long[] pks, boolean trustPatientIdWithoutIssuer) {
        Query q = QueryUtil.getQueryForPks(em, "SELECT OBJECT(p) FROM Patient p WHERE pk ", pks);
        return movePatientsToTrash(q.getResultList(), null, trustPatientIdWithoutIssuer);
    }

    @SuppressWarnings("unchecked")
    public EntityTree movePatientToTrash(String patId, String issuer, boolean trustPatientIdWithoutIssuer) {
        return this.movePatientsToTrash(QueryUtil.getPatientQuery(em, patId, issuer).getResultList(), null, trustPatientIdWithoutIssuer);
    }
    
    public void deleteEmptyPatients(Set<Patient> pats) {
    	for (Patient p : pats) {
    		Patient p1 =em.find(Patient.class, p.getPk());
    		if (p1.getStudies().size() == 0) {
    			deletePatient(p1);
    		}
    	}
    }
    
    
    @SuppressWarnings("unchecked")
    public EntityTree getEntitiesOfInstance(String iuid) {
        Query q = em.createQuery("SELECT OBJECT(i) FROM Instance i LEFT JOIN FETCH i.files WHERE sopInstanceUID = :iuid");
        q.setParameter("iuid", iuid.trim());
        return getEntitiesOfInstances(q.getResultList(), null);
    }
    @SuppressWarnings("unchecked")
    public EntityTree getEntitiesOfInstances(long[] pks) {
        Query q = QueryUtil.getQueryForPks(em, "SELECT OBJECT(i) FROM Instance i LEFT JOIN FETCH i.files WHERE i.pk ", pks);
        return getEntitiesOfInstances(q.getResultList(), null);
    }    
    public EntityTree getEntitiesOfInstances(Collection<Instance> instances, EntityTree entityTree) {
        for (Instance i : instances) {
            for (File f : i.getFiles()) {
                f.getFileSystem().getPk();
            }
        }
        return entityTree == null ? new EntityTree(instances) : entityTree.addInstances(instances);
    }
    
    @SuppressWarnings("unchecked")
    public EntityTree getEntitiesOfSeries(long[] pks) {
        Query q;
        q = QueryUtil.getQueryForPks(em, "SELECT OBJECT(s) FROM Series s WHERE pk ", pks);
        return getEntitiesOfSeries(q.getResultList(), null);
    }

    @SuppressWarnings("unchecked")
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public EntityTree getEntitiesOfSeries(String iuid) {
        Query q = em.createQuery("SELECT OBJECT(s) FROM Series s WHERE seriesInstanceUID = :iuid")
            .setParameter("iuid", iuid.trim());
        return this.getEntitiesOfSeries(q.getResultList(), null);
    }

    private EntityTree getEntitiesOfSeries(Collection<Series> series, EntityTree entityTree) {
        Set<Instance> instances;
        for (Series s : series) {
            instances = s.getInstances();
            if (!instances.isEmpty()) {
                entityTree = getEntitiesOfInstances(instances, entityTree);
            }
            MPPS mpps = s.getModalityPerformedProcedureStep();
            if (mpps!=null) mpps.getAccessionNumber();//initialize MPPS
        }
        return entityTree == null ? new EntityTree() : entityTree;
    }

    public void deleteSeries(Collection<Series> series) {
        Study study;
        Series s;
        Set<Study> studies = new HashSet<Study>();
        for (Series s1 : series) {
            s = em.getReference(Series.class, s1.getPk());
            MPPS mpps = s.getModalityPerformedProcedureStep();
            removeSeriesFromMPPS(mpps, s.getSeriesInstanceUID());
            studies.add(study = s.getStudy());
            em.remove(s);
            study.getSeries().remove(s);
        }
        for (Study st : studies) {
            getUpdateDerivedFieldsUtil().updateDerivedFieldsOfStudy(st);
            markPublishedStudyChanged(st);
        }
    }
    
    public void deleteInstances(Collection<Instance> instances) {
        log.debug("Delete {} instances!",instances.size());
        Set<Study> studies = new HashSet<Study>();
        Instance instance;
        for ( Instance i : instances) {
            instance = em.getReference(Instance.class, i.getPk());
            studies.add(instance.getSeries().getStudy());
            log.debug("Delete Instance:{}",instance.getAttributes(false));
            em.remove(instance);
        }
        removeInstancesFromMpps(instances);
        for (Study st : studies) {
            getUpdateDerivedFieldsUtil().updateDerivedFieldsOfStudy(st);
            markPublishedStudyChanged(st);
        }
    }
    
    public void markFilePath(long filePk, String ext, boolean deleteMark) {
        File f = em.getReference(File.class, filePk);
        String p = f.getFilePath();
        if (p.endsWith(ext)) {
            if (deleteMark) {
                f.setFilePath(p.substring(0, p.length()-ext.length()));
                em.merge(f);
            }
        } else {
            if (!deleteMark) {
                f.setFilePath(p+ext);
                em.merge(f);
            }
        }
    }
    
    private EntityTree movePatientsToTrash(Collection<Patient> patients, EntityTree entityTree, boolean trustPatientIdWithoutIssuer) {
        if (entityTree == null) {
            entityTree = new EntityTree();
        }
        Set<Study> studies;
        for (Patient p : patients) {
            studies = p.getStudies();
            if (studies == null || studies.isEmpty()) {
                log.debug("move empty patient to trash:"+p.getPatientID()+"^^^"+p.getIssuerOfPatientID()+":"+p.getPatientName());
                this.movePatientToTrash(p);
                entityTree.addPatient(p);
            } else {
                entityTree = moveStudiesToTrash(studies, entityTree, trustPatientIdWithoutIssuer);
                studies.clear();
            }
            if (p.getModalityWorklistItems() != null) {
                for (MWLItem mwl : p.getModalityWorklistItems()) {
                    entityTree.addMWLItem(mwl);
                }
            }
            deletePatient(p);
        }
        return entityTree;
    }

    private void moveInstanceToTrash(Instance instance, boolean trustPatientIdWithoutIssuer) {
        DicomObject attrs = instance.getAttributes(false);
        PrivateInstance pInst = new PrivateInstance();
        pInst.setAttributes(attrs);
        pInst.setPrivateType(DELETED);
        Series series = instance.getSeries();
        PrivateSeries ps = moveSeriesToTrash(series, trustPatientIdWithoutIssuer);
        pInst.setSeries(ps);
        for ( File f : instance.getFiles() ) {
            PrivateFile pf = new PrivateFile();
            pf.setFileSystem(f.getFileSystem());
            f.getFileSystem().getAvailability();//initialize FileSystem
            pf.setFilePath(f.getFilePath());
            pf.setTransferSyntaxUID(f.getTransferSyntaxUID());
            pf.setFileStatus(f.getFileStatus());
            pf.setFileSize(f.getFileSize());
            pf.setFileMD5(f.getMD5Sum());
            pf.setInstance(pInst);
            em.persist(pf);
        }
        em.persist(pInst);
    }

    private PrivateSeries moveSeriesToTrash(Series series, boolean trustPatientIdWithoutIssuer) {
        PrivateSeries pSeries;
        try {
            Query q = em.createNamedQuery("PrivateSeries.findByIUID");
            q.setParameter("iuid", series.getSeriesInstanceUID());
            pSeries = (PrivateSeries) q.getSingleResult();
            if (!series.getStudy().getPatient().getPatientID()
            		.equals(pSeries.getStudy().getPatient().getPatientID()))
            	throw new EJBException("Series already exists in trash with different patient ID: SeriesInstanceUID: " 
            		+ series.getSeriesInstanceUID());
            else
            	if (trustPatientIdWithoutIssuer) {
	               	if (series.getStudy().getPatient().getIssuerOfPatientID() != null
	               			&& !series.getStudy().getPatient().getIssuerOfPatientID()
	                   		.equals(pSeries.getStudy().getPatient().getIssuerOfPatientID()))
	               		throw new EJBException("Series already exists in trash with different issuer of patient ID: SeriesInstanceUID: "
	               				+ series.getSeriesInstanceUID());
            	} else {
            		if (series.getStudy().getPatient().getIssuerOfPatientID() == null
    	            	|| !series.getStudy().getPatient().getIssuerOfPatientID()
    	                		.equals(pSeries.getStudy().getPatient().getIssuerOfPatientID()))
            			throw new EJBException("Series already exists in trash with different issuer of patient ID: SeriesInstanceUID: "
            					+ series.getSeriesInstanceUID());
            	}
        } catch (NoResultException nre) {
            pSeries = new PrivateSeries();//we need parents initialized.
        }
        DicomObject attrs = series.getAttributes(false);
        attrs.putString(attrs.resolveTag(PrivateTag.CallingAET, PrivateTag.CreatorID), 
                VR.AE, series.getSourceAET());
        pSeries.setAttributes(attrs);
        pSeries.setPrivateType(DELETED);
        Study study = series.getStudy();
        PrivateStudy pStudy = moveStudyToTrash(study, trustPatientIdWithoutIssuer);
        pSeries.setStudy(pStudy);
        em.persist(pSeries);
        return pSeries;
    }

    private PrivateStudy moveStudyToTrash(Study study, boolean trustPatientIdWithoutIssuer) {
        PrivateStudy pStudy;
        try {
            Query q = em.createNamedQuery("PrivateStudy.findByIUID");
            q.setParameter("iuid", study.getStudyInstanceUID());
            pStudy = (PrivateStudy) q.getSingleResult();
            if (!study.getPatient().getPatientID()
            		.equals(pStudy.getPatient().getPatientID()))
            	throw new EJBException("Study already exists in trash with different patient ID, StudyInstanceUID: " 
            		+ study.getStudyInstanceUID());
            else
	        	if (trustPatientIdWithoutIssuer) {
	               	if (study.getPatient().getIssuerOfPatientID() != null
	               			&& !study.getPatient().getIssuerOfPatientID()
	                   		.equals(pStudy.getPatient().getIssuerOfPatientID()))
	               		throw new EJBException("Study already exists in trash with different issuer of patient ID: StudyInstanceUID: "
	               				+ study.getStudyInstanceUID());
	        	} else {
	        		if (study.getPatient().getIssuerOfPatientID() == null
		            	|| !study.getPatient().getIssuerOfPatientID()
		                		.equals(pStudy.getPatient().getIssuerOfPatientID()))
	        			throw new EJBException("Study already exists in trash with different issuer of patient ID: StudyInstanceUID: "
	        					+ study.getStudyInstanceUID());
	        	}
        } catch (NoResultException nre) {
            pStudy = new PrivateStudy();
        }
        pStudy.setAttributes(study.getAttributes(false));
        pStudy.setPrivateType(DELETED);
        Patient pat = study.getPatient();
        PrivatePatient pPat = movePatientToTrash(pat);
        pStudy.setPatient(pPat);
        em.persist(pStudy);
        return pStudy;
    }

    @SuppressWarnings("unchecked")
    private PrivatePatient movePatientToTrash(Patient patient) {
        PrivatePatient pPat = null;
        try {
            if ( patient.getIssuerOfPatientID() != null) {
                Query q = em.createNamedQuery("PrivatePatient.findByIdAndIssuer");
                q.setParameter("patId", patient.getPatientID());
                q.setParameter("issuer", patient.getIssuerOfPatientID());
                pPat = (PrivatePatient) q.getSingleResult();
            } else {
                Query q = em.createQuery("select object(p) from PrivatePatient p where patientID = :patId and patientName = :name");
                q.setParameter("patId", patient.getPatientID());
                q.setParameter("name", patient.getPatientName());
                List<PrivatePatient> pList = (List<PrivatePatient>) q.getResultList();
                PrivatePatient p;
                String birthdate = patient.getAttributes().getString(Tag.PatientBirthDate, "X");
                for (int i = 0, len = pList.size() ;  i  < len ; i++) {
                    p = pList.get(i);
                    if (p.getAttributes().getString(Tag.PatientBirthDate, "X").equals(birthdate)) {
                        pPat = p;
                        break;
                    }
                }
            }
        } catch (NoResultException nre) {            
        }
        if (pPat == null) {
            pPat = new PrivatePatient();
            pPat.setAttributes(getAttrsWithUpdatedOtherPatientIDs(patient));
            pPat.setPrivateType(DELETED);
            em.persist(pPat);
        }
        return pPat;
    }

    private DicomObject getAttrsWithUpdatedOtherPatientIDs(Patient patient) {
        DicomObject attributes = patient.getAttributes();
        attributes.remove(Tag.OtherPatientIDsSequence);
        Set<OtherPatientID> otherPIDs = patient.getOtherPatientIDs();
        if (otherPIDs != null && otherPIDs.size() > 0) {
            DicomElement oPidSeq = attributes.putSequence(Tag.OtherPatientIDsSequence);
            DicomObject item;
            for (OtherPatientID opid : otherPIDs) {
                item = new BasicDicomObject();
                item.putString(Tag.PatientID, VR.LO, opid.getPatientID());
                item.putString(Tag.IssuerOfPatientID, VR.LO, opid.getIssuerOfPatientID());
                oPidSeq.addDicomObject(item);
            }
        }
        return attributes;
    }
    private void deletePatient(Patient patient) {
        log.info("Delete Patient:{}",patient);
        Set<MPPS> mppss = patient.getModalityPerformedProcedureSteps();
        if (mppss != null) {
            for (MPPS mpps : mppss) {
                Set<Series> seriess = mpps.getSeries();
                if (seriess == null || seriess.isEmpty()) {
                    em.remove(mpps);
                } else {
                    Patient pat = seriess.iterator().next().getStudy().getPatient();
                    log.warn("Wrong patient in MPPS found!\n corrected from:"+mpps.getPatient()+"\nto:"+pat);
                    mpps.setPatient(pat);
                }
            }
        }
        delete(patient.getModalityWorklistItems());
        delete(patient.getGeneralPurposeScheduledProcedureSteps());
        delete(patient.getGeneralPurposePerformedProcedureSteps());
        em.flush();
        em.refresh(patient);
        em.remove(patient);
    }
    
    private void delete(Set<? extends BaseEntity> entities) {
        if ( entities != null) {
            for (BaseEntity entity : entities) {
                em.remove(entity);
                log.info("Deleted: {}",entity);
            }
        }        
    }

    @SuppressWarnings("unchecked")
    public List<MPPS> deletePps(long[] pks) {
        Query q = QueryUtil.getQueryForPks(em, "SELECT OBJECT(p) FROM MPPS p WHERE pk ", pks);
        List<MPPS> mppss = q.getResultList();
        DicomObject seriesAttrs;
        for(MPPS mpps : mppss) {
           for (Series series : mpps.getSeries()) {
                seriesAttrs = series.getAttributes(true);
                seriesAttrs.remove(Tag.ReferencedPerformedProcedureStepSequence);
                seriesAttrs.remove(Tag.PerformedProcedureStepStartDate);
                seriesAttrs.remove(Tag.PerformedProcedureStepStartTime);
                series.setAttributes(seriesAttrs);
                series.setModalityPerformedProcedureStep(null);
                em.merge(series);
            }
            mpps.getPatient().getPatientID();
            em.remove(mpps);
        }
        return mppss;
    }
    
    @SuppressWarnings("unchecked")
    public EntityTree moveStudiesToPatient(long pks[], long pk, boolean useIOCM) {
        Query qP = em.createQuery("SELECT OBJECT(p) FROM Patient p WHERE pk = :pk").setParameter("pk", Long.valueOf(pk));
        Query qS = QueryUtil.getQueryForPks(em, "SELECT OBJECT(s) FROM Study s WHERE pk ", pks);
        return moveStudiesToPatient(qS.getResultList(), (Patient)qP.getSingleResult(), useIOCM);
    }

    @SuppressWarnings("unchecked")
    public EntityTree moveStudyToPatient(String iuid, String patId, String issuer, boolean useIOCM) {
        Query qS = em.createQuery("SELECT OBJECT(s) FROM Study s WHERE studyInstanceUID = :iuid").setParameter("iuid", iuid.trim());
        Query qP = QueryUtil.getPatientQuery(em, patId, issuer);
        return moveStudiesToPatient(qS.getResultList(), (Patient)qP.getSingleResult(), useIOCM);
    }

    private EntityTree moveStudiesToPatient(List<Study> studies, Patient patient, boolean useIOCM) {
        EntityTree tree = new EntityTree();
        tree.setContainsChangedEntities(useIOCM);
        for(Study s : studies) {
            tree.addStudy(s);
            s.setPatient(patient);
            markPublishedStudyDeleted(s);
            HashSet<MPPS> set = new HashSet<MPPS>();
            for (Series series : s.getSeries()) {
                MPPS mpps = series.getModalityPerformedProcedureStep();
                if(mpps != null && mpps.getPatient().getPk() != patient.getPk()) {
                    if (set.add(mpps)) {
                        mpps.setPatient(patient);
                        em.merge(mpps);
                    }
                }
                if (useIOCM) {
                    series.setAttributes(IOCMUtil.changeUID(tree, series.getAttributes(false), 
                            Tag.SeriesInstanceUID));
                    em.merge(series);
                    for (Instance instance : series.getInstances()) {
                        instance.setAttributes(IOCMUtil.addReplacementAttrs(
                           IOCMUtil.changeUID(tree, instance.getAttributes(false), Tag.SOPInstanceUID)));
                        em.merge(instance);
                    }
                }
            }
        }
        if (useIOCM) {
            IOCMUtil.updateUIDrefs(tree, em);
        }
        return tree;
    }

    public DicomObject getCompositeObjectforSeries(String iuid) {
        Query q = em.createQuery("SELECT OBJECT(s) FROM Series s WHERE seriesInstanceUID = :iuid")
            .setParameter("iuid", iuid.trim());
        Series s = (Series) q.getSingleResult();
        DicomObject attrs = s.getAttributes(false);
        attrs.putString(attrs.resolveTag(PrivateTag.CallingAET, PrivateTag.CreatorID), VR.AE, s.getSourceAET());
        s.getStudy().getAttributes(false).copyTo(attrs);
        s.getStudy().getPatient().getAttributes().copyTo(attrs);
        return attrs;

    }
    public DicomObject getCompositeObjectforSeries(long pk) {
        Query q = em.createQuery("SELECT OBJECT(s) FROM Series s WHERE pk = :pk").setParameter("pk", pk);
        Series s = (Series) q.getSingleResult();
        DicomObject attrs = s.getAttributes(false);
        s.getStudy().getAttributes(false).copyTo(attrs);
        s.getStudy().getPatient().getAttributes().copyTo(attrs);
        return attrs;
    }
    public DicomObject getCompositeObjectforStudy(String studyIuid) {
        Query q = em.createQuery("SELECT OBJECT(s) FROM Study s WHERE studyInstanceUID = :iuid").setParameter("iuid", studyIuid.trim());
        Study s = (Study) q.getSingleResult();
        DicomObject attrs = s.getAttributes(false);
        s.getPatient().getAttributes().copyTo(attrs);
        return attrs;
    }
    public DicomObject getCompositeObjectforStudy(long pk) {
        Query q = em.createQuery("SELECT OBJECT(s) FROM Study s WHERE pk = :pk").setParameter("pk", pk);
        Study s = (Study) q.getSingleResult();
        DicomObject attrs = s.getAttributes(false);
        s.getPatient().getAttributes().copyTo(attrs);
        return attrs;
    }
    public DicomObject getPatientAttributes(String patId, String issuer) {
        return ((Patient) QueryUtil.getPatientQuery(em, patId, issuer).getSingleResult()).getAttributes();
    }
    public DicomObject getPatientAttributes(long pk) {
        Query q = em.createQuery("SELECT OBJECT(p) FROM Patient p WHERE pk = :pk").setParameter("pk", pk);
        return ((Patient)q.getSingleResult()).getAttributes();
    }
    
    public Series updateSeries(Series series) {
        if (series.getPk() == -1) {
            if (series.getStudy().getPk() == -1) {
                updateStudy(series.getStudy());
            }
            em.persist(series);
        } else {
            em.merge(series);
        }
        return series;
    }
    public Series createSeries(DicomObject seriesAttrs, long studyPk) {
        Study study = em.find(Study.class, studyPk);
        Series series = new Series();
        series.setAvailability(Availability.ONLINE);
        series.setNumberOfSeriesRelatedInstances(0);
        series.setStorageStatus(StorageStatus.STORED);
        series.setAttributes(seriesAttrs);
        series.setStudy(study);
        em.persist(series);
        return series;
    }
    
    public void removeSeries(long seriesPk) {
        Series series = em.find(Series.class, seriesPk);
        em.remove(series);
    }
    
    public int removeForeignPpsInfo(long studyPk) {
        int count = 0;
        Study study = em.find(Study.class, studyPk);
        for (Series s : study.getSeries()) {
            if (s.getPerformedProcedureStepInstanceUID() != null && s.getModalityPerformedProcedureStep() == null) {
                DicomObject attrs = s.getAttributes(true);
                attrs.remove(Tag.ReferencedPerformedProcedureStepSequence);
                s.setAttributes(attrs);
                em.merge(s);
                count++;
            }
        }
        return count;
    }
    
    public Study updateStudy(Study study) {
        if (study.getPk() == -1) {
            if (study.getPatient().getPk() == -1) {
                em.persist(study.getPatient());
            }
            em.persist(study);
        } else {
            em.merge(study);
        }
        return study;
    }
    
    public Study createStudy(DicomObject studyAttrs, long patPk) {
        Patient patient = em.find(Patient.class, patPk);
        Study study = new Study();
        study.setAttributes(studyAttrs);
        study.setAvailability(Availability.ONLINE);
        study.setNumberOfStudyRelatedInstances(0);
        study.setNumberOfStudyRelatedSeries(0);
        study.setPatient(patient);
        em.persist(study);
        return study;
    }
    
    public void removeStudy(long studyPk) {
        Study study = em.find(Study.class, studyPk);
        em.remove(study);
    }
    
    public DicomObject getIanForForwardModifiedObject(DicomObject obj, String level) {
        Instance instance = null;
        if ("IMAGE".equals(level)) {
            instance = findFirstOnlineInstance("SELECT OBJECT(i) FROM Instance i WHERE sopInstanceUID = :iuid", 
                    obj.getString(Tag.SOPInstanceUID));
        } else if ("SERIES".equals(level)) {
            instance = findFirstOnlineInstance("SELECT OBJECT(i) FROM Instance i WHERE i.series.seriesInstanceUID = :iuid", 
                    obj.getString(Tag.SeriesInstanceUID));
        } else if ("STUDY".equals(level)) {
            instance = findFirstOnlineInstance("SELECT OBJECT(i) FROM Instance i WHERE i.series.study.studyInstanceUID = :iuid", 
                    obj.getString(Tag.StudyInstanceUID));
        } else {
            throw new IllegalArgumentException("Illegal QR Level! (must be STUDY, SERIES or IMAGE):"+level);
        }
        if (instance == null)
            return null;
        Series series = instance.getSeries();
        Study study = series.getStudy();
        Patient pat = study.getPatient();
        DicomObject ian = new BasicDicomObject();
        ian.putString(Tag.StudyInstanceUID, VR.UI, study.getStudyInstanceUID());
        ian.putString(Tag.AccessionNumber, VR.SH, study.getAccessionNumber());
        ian.putString(Tag.PatientID, VR.LO, pat.getPatientID());
        ian.putString(Tag.IssuerOfPatientID, VR.LO, pat.getIssuerOfPatientID());
        ian.putString(Tag.PatientName, VR.PN, pat.getPatientName());
        ian.putSequence(Tag.ReferencedPerformedProcedureStepSequence);
        DicomElement refSeriesSeq = ian.putSequence(Tag.ReferencedSeriesSequence);
        DicomObject refSerItem = new BasicDicomObject();
        refSerItem.putString(Tag.SeriesInstanceUID, VR.UI, series.getSeriesInstanceUID());
        refSeriesSeq.addDicomObject(refSerItem);
        DicomElement refSopSeq = refSerItem.putSequence(Tag.ReferencedSOPSequence);
        DicomObject refSopItem = new BasicDicomObject();
        refSopSeq.addDicomObject(refSopItem);
        refSopItem.putString(Tag.RetrieveAETitle, VR.AE, instance.getRetrieveAETs());
        refSopItem.putString(Tag.InstanceAvailability, VR.CS, Availability.ONLINE.name());
        refSopItem.putString(Tag.ReferencedSOPClassUID, VR.UI, instance.getSOPClassUID());
        refSopItem.putString(Tag.ReferencedSOPInstanceUID, VR.UI, instance.getSOPInstanceUID());
        return ian;
    }
    @SuppressWarnings("unchecked")
    private Instance findFirstOnlineInstance(String query, String uid) {
        Query q = em.createQuery(query);
        q.setParameter("iuid", uid);
        List<Instance> l = (List<Instance>) q.getResultList();
        log.debug("findFirstOnlineInstance with query:{} with iuid:{}", query, uid);
        log.debug("Found instances: {}", l.size());
        for (int i = 0, len=l.size() ; i < len ; i++) {
            if (l.get(i).getAvailability().ordinal() <= Availability.NEARLINE.ordinal()) {
                return l.get(i);
            }
        }
        log.debug("No instance (ONLINE) found!");
        return null;
    }
 
    private void removeSeriesFromMPPS(MPPS mpps, String seriesIUID) {
        if(mpps != null && mpps.getAttributes() != null) {
            DicomObject mppsAttrs = mpps.getAttributes();
            removeFromMPPS(mppsAttrs, seriesIUID, null);
            mpps.setAttributes(mppsAttrs);
            em.merge(mpps);
        }
    }

    private void removeFromMPPS(DicomObject mppsAttrs, String seriesIUID, Collection<String> sopIUIDs) {
        DicomElement psSq = mppsAttrs.get(Tag.PerformedSeriesSequence);
        if (psSq == null) {
            log.warn("Missing Performed Series Sequence in MPPS! mpps\n:"+mppsAttrs);
            return;
        }
        for(int i = psSq.countItems() - 1; i >= 0; i--) {
            DicomObject psItem = psSq.getDicomObject(i);
            if(!seriesIUID.equals(psItem.getString(Tag.SeriesInstanceUID)))
                continue;
            if(sopIUIDs == null) {
                psSq.removeDicomObject(i);
                break;
            }
            DicomElement refImgSq = psItem.get(Tag.ReferencedImageSequence);
            for(int j = refImgSq.countItems() - 1 ; j >= 0 ; j--) {
                DicomObject refImgItem = refImgSq.getDicomObject(j);
                if(sopIUIDs.contains(refImgItem.getString(Tag.ReferencedSOPInstanceUID)))
                    refImgSq.removeDicomObject(j);
            }
        }
    }

    private void removeInstancesFromMpps(Collection<Instance> instances) {
        Map<Series, Set<String>> map = new HashMap<Series, Set<String>>();
        Set<String> iuidsPerSeries;
        for(Instance i : instances)
        {
            Series s = i.getSeries();
            iuidsPerSeries = (Set<String>)map.get(s);
            if(iuidsPerSeries == null)
            {
                iuidsPerSeries = new HashSet<String>();
                map.put(s, iuidsPerSeries);
            }
            iuidsPerSeries.add(i.getSOPInstanceUID());
        }

        for (Map.Entry<Series, Set<String>> entry : map.entrySet()) {
            Series s = (Series)entry.getKey();
            MPPS mpps = s.getModalityPerformedProcedureStep();
            if(mpps != null && mpps.getAttributes() != null) {
                DicomObject mppsAttrs = mpps.getAttributes();
                removeFromMPPS(mppsAttrs, s.getSeriesInstanceUID(), entry.getValue());
                mpps.setAttributes(mppsAttrs);
                try {
                    em.merge(mpps);
                } catch (Throwable x) {
                    log.warn("MPPS update failed! mpps:"+mpps);
                }
            }
        }
    }
}
