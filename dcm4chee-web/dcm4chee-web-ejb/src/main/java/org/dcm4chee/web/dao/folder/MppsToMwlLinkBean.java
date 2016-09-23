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

package org.dcm4chee.web.dao.folder;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.VR;
import org.dcm4chee.archive.common.Availability;
import org.dcm4chee.archive.entity.Instance;
import org.dcm4chee.archive.entity.MPPS;
import org.dcm4chee.archive.entity.MWLItem;
import org.dcm4chee.archive.entity.Patient;
import org.dcm4chee.archive.entity.RequestAttributes;
import org.dcm4chee.archive.entity.Series;
import org.dcm4chee.archive.entity.Study;
import org.dcm4chee.web.dao.util.CoercionUtil;
import org.dcm4chee.web.dao.util.IOCMUtil;
import org.dcm4chee.web.dao.util.QueryUtil;
import org.dcm4chee.web.dao.vo.EntityTree;
import org.dcm4chee.web.dao.vo.MppsToMwlLinkResult;
import org.jboss.annotation.ejb.LocalBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Franz Willer <fwiller@gmail.com>
 * @version $Revision$ $Date$
 * @since Feb 01, 2010
 */

@Stateless
@LocalBinding (jndiBinding=MppsToMwlLinkLocal.JNDI_NAME)
public class MppsToMwlLinkBean implements MppsToMwlLinkLocal {

    private static Logger log = LoggerFactory.getLogger(MppsToMwlLinkBean.class);

    @PersistenceContext(unitName="dcm4chee-arc")
    private EntityManager em;

    @SuppressWarnings("unchecked")
    public MppsToMwlLinkResult linkMppsToMwl(String mppsIUID, String rpId, String spsId, boolean updateMwlStatus,
            String modifyingSystem, String reason) {
        Query qMwl = em.createQuery("select object(m) from MWLItem m where requestedProcedureID = :rpId and scheduledProcedureStepID = :spsId");
        qMwl.setParameter("rpId", rpId).setParameter("spsId", spsId);
        MWLItem mwl = (MWLItem) qMwl.getSingleResult();
        Query qMpps = em.createQuery("select object(m) from MPPS m where sopInstanceUID = :mppsIUID");
        qMpps.setParameter("mppsIUID", mppsIUID);
        List<MPPS> mppss = (List<MPPS>) qMpps.getResultList();
        return link(mppss, mwl, updateMwlStatus, modifyingSystem, reason);
    }   
            
    @SuppressWarnings("unchecked")
    public MppsToMwlLinkResult linkMppsToMwl(long[] mppsPks, long mwlPk, boolean updateMwlStatus, 
            String modifyingSystem, String reason) {
        Query qMwl = em.createQuery("select object(m) from MWLItem m where pk = :pk");
        qMwl.setParameter("pk", mwlPk);
        MWLItem mwl = (MWLItem) qMwl.getSingleResult();
        Query qMpps = QueryUtil.getQueryForPks(em,"select object(m) from MPPS m where pk ", mppsPks);
        List<MPPS> mppss = (List<MPPS>) qMpps.getResultList(); 
        return link(mppss, mwl, updateMwlStatus, modifyingSystem, reason);
    }

    @SuppressWarnings("unchecked")
    public MppsToMwlLinkResult linkMppsToMwl(long[] mppsPks, DicomObject mwlAttrs, Patient mwlPat, String modifyingSystem, String reason) {
        Query qMpps = QueryUtil.getQueryForPks(em,"select object(m) from MPPS m where pk ", mppsPks);
        List<MPPS> mppss = (List<MPPS>) qMpps.getResultList();
        MppsToMwlLinkResult result = new MppsToMwlLinkResult();
        MWLItem mwl = new MWLItem();
        mwl.setAttributes(mwlAttrs);
        mwl.setPatient(mwlPat);
        result.setMwl(mwl);
        for (MPPS mpps : mppss) {
            log.debug("link MPPS {} with MWL!", mpps);
            checkPatients(mpps, mwlPat, result);
            link(mpps, mwlAttrs, modifyingSystem, reason);
            result.addMppsAttributes(mpps);
        }
        return result;
    }

    private void checkPatients(MPPS mpps, Patient mwlPat,
            MppsToMwlLinkResult result) {
        Patient mppsPat;
        mppsPat = mpps.getPatient();
        Study study;
        if ( mppsPat.getPk() != mwlPat.getPk()) {
            log.warn("Patient of MPPS("+mppsPat.getPatientID()+") and MWL("+mwlPat.getPatientID()+") are different!");
            if (mpps.getSeries().size() > 0) {
                study = mpps.getSeries().iterator().next().getStudy();
                study.getPatient().getModalityPerformedProcedureSteps().size();
                result.addStudyToMove(study);
            } else {
                mpps.setPatient(mwlPat);
                em.merge(mpps);
            }
        }
    }

    private MppsToMwlLinkResult link(List<MPPS> mppss, MWLItem mwl, boolean updateMwlStatus, String modifyingSystem, String reason) {
        Patient mwlPat = mwl.getPatient();
        MppsToMwlLinkResult result = new MppsToMwlLinkResult();
        result.setMwl(mwl);
        DicomObject mwlAttrs = mwl.getAttributes();
        for (MPPS mpps : mppss) {
            checkPatients(mpps, mwlPat, result);
            link(mpps, mwlAttrs, modifyingSystem, reason);
            result.addMppsAttributes(mpps);
        }
        if (updateMwlStatus) {
            mwlAttrs.get(Tag.ScheduledProcedureStepSequence).getDicomObject()
                .putString(Tag.ScheduledProcedureStepStatus, VR.CS, "COMPLETED");
            mwl.setAttributes(mwlAttrs);
            em.merge(mwl);
        }
        return result;
    }
    
    private void link(MPPS mpps, DicomObject mwlAttrs, String modifyingSystem, String reason) {
        DicomObject ssa;
        DicomObject mppsAttrs = mpps.getAttributes();
        log.debug("MPPS attrs:{}", mpps);
        log.debug("MWL attrs:{}",mwlAttrs);
        String rpid = mwlAttrs.getString(Tag.RequestedProcedureID);
        DicomElement spsSq = mwlAttrs.get(Tag.ScheduledProcedureStepSequence);
        String spsid = spsSq.getDicomObject().getString(Tag.ScheduledProcedureStepID);
        String accNo = mwlAttrs.getString(Tag.AccessionNumber);
        DicomElement ssaSQ = mppsAttrs.get(Tag.ScheduledStepAttributesSequence);
        DicomObject origAttrs = new BasicDicomObject();
        mppsAttrs.subSet(new int[]{Tag.ScheduledStepAttributesSequence}).copyTo(origAttrs);
        updateOriginalAttributeSequence(mppsAttrs, origAttrs, modifyingSystem,
                reason);
        String ssaSpsID, studyIUID = null;
        boolean spsNotInList = true;
        boolean invalidSSAItem0 = false;
        for (int i = 0, len = ssaSQ.countItems(); i < len; i++) {
            ssa = ssaSQ.getDicomObject(i);
            if (ssa != null) {
                if (studyIUID == null) {
                    studyIUID = ssa.getString(Tag.StudyInstanceUID);
                }
                ssaSpsID = ssa.getString(Tag.ScheduledProcedureStepID);
                if (ssaSpsID == null || spsid.equals(ssaSpsID)) {
                    ssa.putString(Tag.AccessionNumber, VR.SH, accNo);
                    ssa.putString(Tag.ScheduledProcedureStepID, VR.SH, spsid);
                    ssa.putString(Tag.RequestedProcedureID, VR.SH, rpid);
                    ssa.putString(Tag.StudyInstanceUID, VR.UI, studyIUID);
                    spsNotInList = false;
                } else if (ssaSpsID != null && ssa.getString(Tag.AccessionNumber) == null) {
                    log.warn("MPPS contains an invalid ScheduledStepAttributes item! (with SPS Id but no Accession Number):"+ssa);
                    if (i==0)
                        invalidSSAItem0 = true;
                }
            }
        }
        if (spsNotInList) {
            ssa = new BasicDicomObject();
            if (invalidSSAItem0) {
                ssaSQ.addDicomObject(0, ssa);
            } else {
                ssaSQ.addDicomObject(ssa);
            }
            DicomObject spsDS = spsSq.getDicomObject();
            ssa.putString(Tag.StudyInstanceUID, VR.UI, studyIUID);
            ssa.putString(Tag.ScheduledProcedureStepID, VR.SH, spsid);
            ssa.putString(Tag.RequestedProcedureID, VR.SH, rpid);
            ssa.putString(Tag.AccessionNumber, VR.SH, accNo);
            ssa.putSequence(Tag.ReferencedStudySequence);
            ssa.putString(Tag.RequestedProcedureID, VR.SH, rpid);
            ssa.putString(Tag.ScheduledProcedureStepDescription, VR.LO, 
                    spsDS.getString(Tag.ScheduledProcedureStepDescription));
            DicomElement mppsSPCSQ = ssa.putSequence(Tag.ScheduledProtocolCodeSequence);
            DicomElement mwlSPCSQ = spsDS.get(Tag.ScheduledProtocolCodeSequence);
            if (mwlSPCSQ != null) {
                DicomObject codeItem;
                for (int i = 0, len = mwlSPCSQ.countItems(); i < len; i++) {
                    codeItem = new BasicDicomObject();
                    mwlSPCSQ.getDicomObject(i).copyTo(codeItem);
                    mppsSPCSQ.addDicomObject(codeItem);
                }
            }
            log.debug("Add new ScheduledStepAttribute item: {}", ssa);
            log.debug("New mppsAttrs:{}", mppsAttrs);
        } else if (invalidSSAItem0) {
            ssa = ssaSQ.removeDicomObject(0);
            ssaSQ.addDicomObject(ssa);
        }
        mpps.setAttributes(mppsAttrs);
        em.merge(mpps);
        Set<Series> series = mpps.getSeries();
        if ( series.size() > 0) {
            Study s = series.iterator().next().getStudy();
            DicomObject sAttrs = s.getAttributes(true);
            sAttrs.putString(Tag.AccessionNumber, VR.SH, accNo);
            s.setAttributes(sAttrs);
            em.merge(s);
        }
    }

    public MppsToMwlLinkResult unlinkMpps(long[] mppsPks, boolean updateMwlStatus, String modifyingSystem, String modifyReason, boolean useIOCM) {
        MppsToMwlLinkResult result = new MppsToMwlLinkResult();
        EntityTree tree = new EntityTree();
        tree.setContainsChangedEntities(true);
        result.setEntityTree(tree);
        List<MPPS> mppsList = new ArrayList<MPPS>(mppsPks.length);
        for (Long pk : mppsPks) {
            MPPS mpps = em.find(MPPS.class, pk);
            mppsList.add(mpps);
            MPPS mppsSav = new MPPS();
            mppsSav.setAttributes(mpps.getAttributes());
            result.addMppsAttributes(mppsSav);
            mpps.getPatient().getPatientID();
            mppsSav.setPatient(mpps.getPatient());
            mppsSav.setSeries(mpps.getSeries());
            DicomObject mppsAttrs = mpps.getAttributes();
            DicomElement ssaSQ = mppsAttrs.get(Tag.ScheduledStepAttributesSequence);
            String rpId, spsId;
            DicomObject item = null;
            DicomObject mwlAttrs;
            MWLItem mwlItem = null;
            HashSet<String> rpspsIDs = new HashSet<String>(ssaSQ.countItems());
            Query qMwl = em.createQuery("select object(m) from MWLItem m where requestedProcedureID = :rpId and scheduledProcedureStepID = :spsId");
            for ( int i = 0, len = ssaSQ.countItems() ; i < len ; i++) {
                item = ssaSQ.getDicomObject(i);
                rpId = item.getString(Tag.RequestedProcedureID);
                spsId = item.getString(Tag.ScheduledProcedureStepID);
                if (spsId != null) {
                    rpspsIDs.add(rpId+"_"+spsId);
                    if (updateMwlStatus) {
                        try {
                            qMwl.setParameter("rpId", rpId).setParameter("spsId", spsId);
                            mwlItem = (MWLItem)qMwl.getSingleResult();
                            mwlAttrs = mwlItem.getAttributes();
                            mwlAttrs.get(Tag.ScheduledProcedureStepSequence).getDicomObject()
                                .putString(Tag.ScheduledProcedureStepStatus, VR.CS, "SCHEDULED");
                            mwlItem.setAttributes(mwlAttrs);
                            em.merge(mwlItem);
                        } catch (Exception ignore) {
                            log.warn("Can't update MWLItem status to SCHEDULED! MWL:"+mwlItem, ignore);
                        }
                    }
                }
            }
            String studyIUID = item.getString(Tag.StudyInstanceUID);
            item.clear();
            item.putString(Tag.StudyInstanceUID, VR.UI, studyIUID);
            item.putString(Tag.ScheduledProcedureStepID, VR.SH, null);
            item.putString(Tag.AccessionNumber, VR.SH, null);
            item.putSequence(Tag.ReferencedStudySequence);
            item.putString(Tag.RequestedProcedureID, VR.SH, null);
            item.putString(Tag.ScheduledProcedureStepDescription, VR.LO, null);
            item.putSequence(Tag.ScheduledProtocolCodeSequence);
            mppsAttrs.putSequence(Tag.ScheduledStepAttributesSequence).addDicomObject(item);
            mpps.setAttributes(mppsAttrs);
            if (rpspsIDs.size() > 0) {
                DicomObject seriesAttrs, rqAttrSqItem;
                DicomElement reqAttrSQ;
                DicomElement newReqAttrSQ;
                Series s = null;
                for ( Iterator<Series> iter = mpps.getSeries().iterator() ; iter.hasNext() ; ) {
                    s = iter.next();
                    seriesAttrs = s.getAttributes(true);
                    reqAttrSQ = seriesAttrs.get(Tag.RequestAttributesSequence);
                    if (reqAttrSQ != null) {
                        newReqAttrSQ = seriesAttrs.putSequence(Tag.RequestAttributesSequence);
                        for (int i = 0, len = reqAttrSQ.countItems() ; i < len ; i++) {
                            rqAttrSqItem = reqAttrSQ.getDicomObject(i);
                            rpId = rqAttrSqItem.getString(Tag.RequestedProcedureID); 
                            spsId = rqAttrSqItem.getString(Tag.ScheduledProcedureStepID);
                            if (!rpspsIDs.contains(rpId+"_"+spsId)) {
                                newReqAttrSQ.addDicomObject(rqAttrSqItem);
                            }
                        }
                        if (newReqAttrSQ.isEmpty())
                            seriesAttrs.remove(Tag.RequestAttributesSequence);
                    }
                    seriesAttrs.putString(Tag.AccessionNumber, VR.SH, null);
                    if (useIOCM) {
                        IOCMUtil.changeUID(tree, seriesAttrs, Tag.SeriesInstanceUID);
                        DicomObject iAttrs;
                        tree.addSeries(s);
                        for (Instance i : s.getInstances()) {
                            iAttrs = i.getAttributes(false);
                            IOCMUtil.changeUID(tree, iAttrs, Tag.SOPInstanceUID);
                            i.setAttributes(iAttrs);
                            em.merge(i);
                        }
                    }
                    s.setAttributes(seriesAttrs);
                    updateRequestAttributes(s, seriesAttrs.get(Tag.RequestAttributesSequence), null);
                    em.merge(s);
                }
                if (useIOCM)
                    IOCMUtil.updateUIDrefs(tree, em);
                if (s != null) {
                    Study study = s.getStudy();
                    DicomObject studyAttrs = study.getAttributes(false);
                    studyAttrs.putString(Tag.AccessionNumber, VR.SH, null);
                    study.setAttributes(studyAttrs);
                    em.merge(study);
                }
            }
        }
        if (useIOCM) {
            IOCMUtil.updateUIDrefs(tree, em);
            IOCMUtil.updateUIDrefs(mppsList, tree, em);
        }
        return result;
    }
    
    private void updateRequestAttributes(Series s, DicomElement newReqAttrSq, String studyAccNo) {
        Set<RequestAttributes> reqAttrs = s.getRequestAttributes();
        if (newReqAttrSq == null) {
            if (reqAttrs.size() > 0) {
                for (RequestAttributes ra : reqAttrs) {
                    em.remove(ra);
                }
                s.setRequestAttributes(reqAttrs);
            }
        } else {
            RequestAttributes ra;
            DicomObject rqAttrSqItem;
            String rpId, spsId;
            reqAttr: for (Iterator<RequestAttributes> it = reqAttrs.iterator() ; it.hasNext() ; ) {
                ra = it.next();
                if (ra.getRequestedProcedureID() != null && ra.getScheduledProcedureStepID() != null) {
                    for (int i = 0, len = newReqAttrSq.countItems() ; i < len ; i++) {
                        rqAttrSqItem = newReqAttrSq.getDicomObject(i);
                        rpId = rqAttrSqItem.getString(Tag.RequestedProcedureID); 
                        spsId = rqAttrSqItem.getString(Tag.ScheduledProcedureStepID);
                        if (ra.getRequestedProcedureID().equals(rpId) &&
                            ra.getScheduledProcedureStepID().equals(spsId)) {
                            ra.setAttributes(rqAttrSqItem);
                            if (ra.getAccessionNumber() == null && studyAccNo != null)
                                ra.setAccessionNumber(studyAccNo);
                            em.merge(ra);
                            newReqAttrSq.removeDicomObject(i);
                            continue reqAttr;
                        }
                    }
                }
                em.remove(ra);
                it.remove();
            }
            if (newReqAttrSq.countItems() > 0) {
                for (int i = 0, len = newReqAttrSq.countItems() ; i < len ; i++) {
                    ra = new RequestAttributes();
                    ra.setAttributes(newReqAttrSq.getDicomObject(i));
                    if (ra.getAccessionNumber() == null && studyAccNo != null)
                        ra.setAccessionNumber(studyAccNo);
                    ra.setSeries(s);
                    em.persist(ra);
                    reqAttrs.add(ra);
                }
            }
            if (log.isDebugEnabled())
                log.debug("updateRequestAttributes! series.setRequestAttributes:"+reqAttrs);
            s.setRequestAttributes(reqAttrs);
        }
    }
    @SuppressWarnings("unchecked")
    public EntityTree updateSeriesAndStudyAttributes(MppsToMwlLinkResult linkResult, DicomObject coerce, boolean useIOCM) {
        StringBuilder sb = new StringBuilder("SELECT object(s) FROM Series s WHERE performedProcedureStepInstanceUID");
        String[] mppsIuids = new String[linkResult.getMppss().size()];
        int i = 0;
        for (MPPS m : linkResult.getMppss()) {
            mppsIuids[i++] = m.getSopInstanceUID();
        }
        QueryUtil.appendIN(sb, mppsIuids.length);
        Query qS = em.createQuery(sb.toString());
        QueryUtil.setParametersForIN(qS, mppsIuids);
        List<Series> seriess = (List<Series>) qS.getResultList();
        log.info("Coerce Series and Study attributes after linking mpps to mwl: nr of series:"+seriess.size());
        EntityTree tree = new EntityTree();
        tree.setContainsChangedEntities(useIOCM);
        if (seriess.size() > 0) {
            DicomObject seriesAndStudyAttrs = null;
            Study study = null;
            for (Series s : seriess) {
                seriesAndStudyAttrs = s.getAttributes(true);
                study = s.getStudy();
                tree.addStudy(study);
                study.getAttributes(true).copyTo(seriesAndStudyAttrs);
                seriesAndStudyAttrs.remove(Tag.RequestAttributesSequence);
                log.debug("Coerce SeriesAndStudy: orig:"+seriesAndStudyAttrs);
                log.debug("Coerce SeriesAndStudy: coerce:"+coerce);
                CoercionUtil.coerceAttributes(seriesAndStudyAttrs, coerce, null);
                log.debug("Set coerced SeriesAndStudy: "+seriesAndStudyAttrs);
                if (useIOCM) {
                    for (Instance instance : s.getInstances()) {
                        DicomObject iAttrs = instance.getAttributes(false);
                        IOCMUtil.changeUID(tree, iAttrs, Tag.SOPInstanceUID);
                        instance.setAttributes(IOCMUtil.addReplacementAttrs(iAttrs));
                        em.merge(instance);
                    }
                    IOCMUtil.changeUID(tree, seriesAndStudyAttrs, Tag.SeriesInstanceUID);
                }
                s.setAttributes(seriesAndStudyAttrs);
                updateRequestAttributes(s, seriesAndStudyAttrs.get(Tag.RequestAttributesSequence),
                        seriesAndStudyAttrs.getString(Tag.AccessionNumber));
                em.merge(s);
                log.debug("new Series Attrs: "+s.getAttributes(true));
            }
            study.setAttributes(seriesAndStudyAttrs);
            em.merge(study);
            log.debug("new Study Attrs: "+study.getAttributes(true));
        }
        if (useIOCM) {
            IOCMUtil.updateUIDrefs(tree, em);
            IOCMUtil.updateUIDrefs(linkResult.getMppss(), tree, em);
        }
        return tree;
    }

    public void updateMPPSAttributes(MPPS mpps, DicomObject attrs) {
        mpps.setAttributes(attrs);
        em.merge(mpps);
    }

    private void addToResult(Map<String, DicomObject> result, Series series,
            Study study) {
        Instance instance = null;
        for (Iterator<Instance> it = series.getInstances().iterator() ; it.hasNext(); instance = null) {
            instance = it.next();
            if (instance.getAvailability().ordinal() <= Availability.NEARLINE.ordinal())
                break;
        }
        if (instance == null) {
            log.warn("No ONLINE or NEARLINE instance found for series:"+series.getSeriesInstanceUID()+"!");
            return;
        }
        DicomObject ian = result.get(study.getStudyInstanceUID());
        if (ian == null) {
            ian = new BasicDicomObject();
            ian.putString(Tag.StudyInstanceUID, VR.UI, study.getStudyInstanceUID());
            ian.putString(Tag.AccessionNumber, VR.SH, study.getAccessionNumber());
            Patient pat = study.getPatient();
            ian.putString(Tag.PatientID, VR.LO, pat.getPatientID());
            ian.putString(Tag.IssuerOfPatientID, VR.LO, pat.getIssuerOfPatientID());
            ian.putString(Tag.PatientName, VR.PN, pat.getPatientName());
            result.put(study.getStudyInstanceUID(), ian);
        }
        DicomElement refSeriesSeq = ian.get(Tag.ReferencedSeriesSequence);
        if (refSeriesSeq == null)
            refSeriesSeq = ian.putSequence(Tag.ReferencedSeriesSequence);
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
    }

    private void updateOriginalAttributeSequence(DicomObject attrs,
            DicomObject origAttrs, String modifyingSystem, String reason) {
        DicomElement origAttrsSq = attrs.get(Tag.OriginalAttributesSequence);
        if (origAttrsSq == null)
            origAttrsSq = attrs.putSequence(Tag.OriginalAttributesSequence);
        DicomObject origAttrsItem = new BasicDicomObject();
        origAttrsItem.putString(Tag.SourceOfPreviousValues, VR.LO, null);
        origAttrsItem.putDate(Tag.AttributeModificationDateTime, VR.DT, new Date());
        origAttrsItem.putString(Tag.ModifyingSystem, VR.LO, modifyingSystem);
        origAttrsItem.putString(Tag.ReasonForTheAttributeModification, VR.CS, reason);
        DicomElement modSq = origAttrsItem.putSequence(Tag.ModifiedAttributesSequence);
        modSq.addDicomObject(origAttrs);
        origAttrsSq.addDicomObject(origAttrsItem);
    }

    @SuppressWarnings("unchecked")
    public List<Patient> selectOrCreatePatient(DicomObject patAttrs) {
        String patID = patAttrs.getString(Tag.PatientID);
        String issuer = patAttrs.getString(Tag.IssuerOfPatientID);
        Query qPat;
        if (issuer == null) {
            qPat = em.createQuery("select object(p) from Patient p where patientID = :patID");
            qPat.setParameter("patID", patID);
        } else {
            qPat = em.createQuery("select object(p) from Patient p where patientID = :patID and issuerOfPatientID = :issuer");
            qPat.setParameter("patID", patID).setParameter("issuer", issuer);
        }
        List<Patient> pats = (List<Patient>) qPat.getResultList();
        Patient pat;
        if (pats.size() == 0) {
            log.info("create new Patient for linking MPPS to external worklist entry! patID:"+patID);
            pat = new Patient();
            pat.setAttributes(patAttrs);
            em.persist(pat);
            log.debug("Patient created:{}",pat.getAttributes());
            pats.add(pat);
        }
        log.info("return pat:"+pats);
        return pats;
    }
}
