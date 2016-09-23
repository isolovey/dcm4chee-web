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
 * Java(TM), available at http://sourceforge.net/projects/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * TIANI Medgraph AG.
 * Portions created by the Initial Developer are Copyright (C) 2003-2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * Gunter Zeilinger <gunter.zeilinger@tiani.com>
 * Franz Willer <franz.willer@gwi-ag.com>
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
package org.dcm4chee.web.service.contentedit.iocm;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4che2.data.VR;
import org.dcm4che2.util.UIDUtils;
import org.dcm4chee.archive.common.PrivateTag;
import org.dcm4chee.archive.entity.Code;
import org.dcm4chee.archive.entity.Instance;
import org.dcm4chee.archive.entity.Patient;
import org.dcm4chee.archive.entity.Series;
import org.dcm4chee.archive.entity.Study;
import org.dcm4chee.web.dao.util.IOCMUtil;
import org.dcm4chee.web.dao.vo.EntityTree;
import org.dcm4chee.web.service.contentedit.iocm.ChangeRequestOrder.ChangedInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author franz.willer@gmail.com
 * @version $Revision$ $Date$
 * @since Feb 11, 2010
 */
public class IOCMSupport {

    private Code rejectNoteCode = new Code();
    
    private static Logger log = LoggerFactory.getLogger(IOCMSupport.class);
    
    
    public Code getRejectNoteCode() {
        return rejectNoteCode;
    }

    public void setRejectNoteCode(Code rejectNoteCode) {
        this.rejectNoteCode = rejectNoteCode;
    }

    public void updateUIDs(ChangeRequestOrder order) {
        Collection<ChangedInstance> instances = order.getInstances().values();
        for (ChangedInstance i : instances) {
            DicomObject attrs = i.getChangedHeader();
            if (IOCMUtil.updateUIDs(attrs, order.getUidMap()))
                i.setChangedHeader(attrs);
        }
    }
    
    public DicomObject[] getRejectionNotes(EntityTree entityTree) {
        Map<Patient, Map<Study, Map<Series, Set<Instance>>>> entityTreeMap = entityTree.getEntityTreeMap();
        DicomObject[] rejNotes = new DicomObject[entityTreeMap.size()];
        Map<String, String> uidMap = entityTree.getUIDMap();
        int i = 0;
        for ( Map.Entry<Patient, Map<Study, Map<Series, Set<Instance>>>> studies : entityTreeMap.entrySet()) {
            rejNotes[i] = studies.getValue().isEmpty() ? null : toRejectionNote(studies.getKey(), studies.getValue(), uidMap);
            log.debug("Rejection Note! KOS:{}", rejNotes[i++]);
        }
        return rejNotes;
    }
    
    private DicomObject toRejectionNote(Patient pat, Map<Study, Map<Series, Set<Instance>>> entityTree, Map<String, String> uidMap) {
    	Study study = entityTree.keySet().iterator().next();
        DicomObject kos = newKeyObject( );
        DicomElement crpeSeq = kos.putSequence(Tag.CurrentRequestedProcedureEvidenceSequence);
        DicomElement contentSeq = kos.putSequence(Tag.ContentSequence);
        pat.getAttributes().copyTo(kos);
        study.getAttributes(false).copyTo(kos);
        kos.putString(Tag.StudyInstanceUID, VR.UI, toUID(study.getStudyInstanceUID(), uidMap));
        for (Map.Entry<Study, Map<Series, Set<Instance>>> entry : entityTree.entrySet() ) {
            addProcedureEvidenceSequenceItem(crpeSeq, entry.getKey(), entry.getValue(), uidMap);
            addContentSequenceItem(contentSeq, entry.getKey(), entry.getValue(), uidMap);
        }
        return kos;
    }
    
    private String toUID(String uid, Map<String, String> uidMap) {
        String uid2 = uidMap.get(uid);
        if (uid2 == null)
            return uid;
        String uid3 = uidMap.get(uid2);
        return uid3 == null ? uid2 : uid3;
    }
    
    private DicomObject newKeyObject() {
        DicomObject kos = new BasicDicomObject();
        kos.putString(Tag.SeriesInstanceUID,VR.UI, UIDUtils.createUID());
        kos.putString(Tag.SOPInstanceUID,VR.UI, UIDUtils.createUID());
        kos.putString(Tag.SOPClassUID,VR.UI, UID.KeyObjectSelectionDocumentStorage);
        kos.putString(Tag.Modality, VR.CS, "KO");
        kos.putInt(Tag.InstanceNumber, VR.IS, 1);
        kos.putDate(Tag.ContentDate, VR.DA, new Date());
        kos.putDate(Tag.ContentTime, VR.TM, new Date());
        DicomElement cncSeq = kos.putSequence(Tag.ConceptNameCodeSequence);
        cncSeq.addDicomObject(rejectNoteCode.toCodeItem());
        kos.putString(Tag.ValueType, VR.CS, "CONTAINER");
        DicomElement tmplSeq = kos.putSequence(Tag.ContentTemplateSequence);
        DicomObject tmplItem = new BasicDicomObject();
        tmplItem.putString(Tag.TemplateIdentifier, VR.CS, "2010");
        tmplItem.putString(Tag.MappingResource, VR.CS, "DCMR");
        tmplSeq.addDicomObject(tmplItem);
        kos.putSequence(Tag.ReferencedPerformedProcedureStepSequence);
        kos.putString(Tag.SeriesDescription, VR.LO, "Rejection Note");
        return kos;
    }
        
    private void addProcedureEvidenceSequenceItem(DicomElement crpeSeq, Study study, Map<Series, Set<Instance>> series, Map<String, String> uidMap) {
        DicomObject item = new BasicDicomObject();
        crpeSeq.addDicomObject(item);
        item.putString(Tag.StudyInstanceUID, VR.UI, toUID(study.getStudyInstanceUID(), uidMap));
        DicomElement refSeriesSeq = item.putSequence(Tag.ReferencedSeriesSequence);
        DicomElement refSopSeq;
        DicomObject refSeriesSeqItem, refSopSeqItem;
        for ( Map.Entry<Series, Set<Instance>> instances : series.entrySet()) {
            refSeriesSeqItem = new BasicDicomObject();
            refSeriesSeq.addDicomObject(refSeriesSeqItem);
            refSeriesSeqItem.putString(Tag.SeriesInstanceUID, VR.UI, toUID(instances.getKey().getSeriesInstanceUID(), uidMap));
            refSeriesSeqItem.putString(refSeriesSeqItem.resolveTag(PrivateTag.CallingAET, PrivateTag.CreatorID, true), 
                    VR.AE, instances.getKey().getSourceAET());
            refSopSeq = refSeriesSeqItem.putSequence(Tag.ReferencedSOPSequence);
            for ( Instance inst : instances.getValue()) {
                refSopSeqItem = new BasicDicomObject();
                refSopSeq.addDicomObject(refSopSeqItem);
                refSopSeqItem.putString(Tag.ReferencedSOPInstanceUID, VR.UI, toUID(inst.getSOPInstanceUID(), uidMap));
                refSopSeqItem.putString(Tag.ReferencedSOPClassUID, VR.UI, inst.getSOPClassUID());
            }
        }
    }
    private void addContentSequenceItem(DicomElement contentSeq, Study study, Map<Series, Set<Instance>> series, Map<String, String> uidMap) {
        for ( Map.Entry<Series, Set<Instance>> instances : series.entrySet()) {
            for ( Instance inst : instances.getValue()) {
                DicomObject item = new BasicDicomObject();
                item.putString(Tag.ValueType, VR.CS, "COMPOSITE");
                item.putString(Tag.RelationshipType, VR.CS, "CONTAINS");
                contentSeq.addDicomObject(item);
                DicomElement refSopSq = item.putSequence(Tag.ReferencedSOPSequence);
                DicomObject sopItem = new BasicDicomObject();
                sopItem.putString(Tag.ReferencedSOPInstanceUID, VR.UI, toUID(inst.getSOPInstanceUID(), uidMap));
                sopItem.putString(Tag.ReferencedSOPClassUID, VR.UI, inst.getSOPClassUID());
                refSopSq.addDicomObject(sopItem);
            }
        }
    }

}

