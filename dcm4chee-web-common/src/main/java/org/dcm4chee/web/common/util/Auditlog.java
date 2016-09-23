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

package org.dcm4chee.web.common.util;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.dcm4che2.audit.message.ActiveParticipant;
import org.dcm4che2.audit.message.AuditEvent;
import org.dcm4che2.audit.message.AuditEvent.ActionCode;
import org.dcm4che2.audit.message.AuditEvent.TypeCode;
import org.dcm4che2.audit.message.AuditMessage;
import org.dcm4che2.audit.message.DataExportMessage;
import org.dcm4che2.audit.message.InstancesAccessedMessage;
import org.dcm4che2.audit.message.ParticipantObject;
import org.dcm4che2.audit.message.ParticipantObjectDescription;
import org.dcm4che2.audit.message.ParticipantObjectDescription.SOPClass;
import org.dcm4che2.audit.message.PatientRecordMessage;
import org.dcm4che2.audit.message.ProcedureRecordMessage;
import org.dcm4che2.audit.message.QueryMessage;
import org.dcm4che2.audit.message.SecurityAlertMessage;
import org.dcm4che2.audit.message.StudyDeletedMessage;
import org.dcm4che2.audit.util.InstanceSorter;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4che2.io.DicomOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Franz Willer <franz.willer@gmail.com>
 * @version $Revision$ $Date$
 * @since Sep 15, 2011
 */
public class Auditlog {
    private static Logger auditLog = LoggerFactory.getLogger("auditlog");
    private static Logger log = LoggerFactory.getLogger(Auditlog.class);

    public static void logQuery(boolean success, String cuid, DicomObject query) {
        HttpUserInfo userInfo = new HttpUserInfo(AuditMessage.isEnableDNSLookups());
        try {
            QueryMessage msg = new QueryMessage();
            msg.setOutcomeIndicator(success ? AuditEvent.OutcomeIndicator.SUCCESS : AuditEvent.OutcomeIndicator.MINOR_FAILURE);
            msg.addDestinationProcess(AuditMessage.getProcessID(), null,
                    AuditMessage.getProcessName(), userInfo.getHostName(), false);

            msg.addSourceProcess(AuditMessage.getProcessID(), null, AuditMessage.getProcessName(), AuditMessage.getLocalHostName(), true);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DicomOutputStream dos = new DicomOutputStream(bos);
            dos.writeDataset(query, UID.ExplicitVRLittleEndian);
            msg.addQuerySOPClass(cuid, UID.ExplicitVRLittleEndian, bos.toByteArray());
            msg.validate();
            auditLog.info(msg.toString());
        } catch (Exception x) {
            log.warn("Audit Log 'Query' failed:", x);
        }
    }

    public static void logInstancesAccessed(AuditEvent.ActionCode actionCode, boolean success, DicomObject kos, boolean addIUID, String detailMessage) {
        HttpUserInfo userInfo = new HttpUserInfo(AuditMessage.isEnableDNSLookups());
        try {
            InstancesAccessedMessage msg = new InstancesAccessedMessage(actionCode);
            msg.setOutcomeIndicator(success ? AuditEvent.OutcomeIndicator.SUCCESS : AuditEvent.OutcomeIndicator.MINOR_FAILURE);
            msg.addUserPerson(userInfo.getUserId(), null, null, userInfo
                    .getHostName(), true);
            msg.addPatient(kos.getString(Tag.PatientID), kos.getString(Tag.PatientName));
            ParticipantObject study;
            DicomElement crpeSeq = kos.get(Tag.CurrentRequestedProcedureEvidenceSequence);
            DicomObject item;
            for ( int i = 0, len = crpeSeq.countItems() ; i < len ; i++ ) {
                item = crpeSeq.getDicomObject(i);
                study = msg.addStudy(item.getString(Tag.StudyInstanceUID),
                        getStudyDescription(item, addIUID));
                if ( detailMessage != null )
                    study.addParticipantObjectDetail("Description", getStudySeriesDetail(detailMessage, item));
            }
            msg.validate();
            auditLog.info(msg.toString());
        } catch (Exception x) {
            log.warn("Audit Log 'Instances Accessed' (actionCode:" + actionCode
                    + ") failed:", x);
        }
    }

    public static void logStudyDeleted(DicomObject kos, boolean success) {
        HttpUserInfo userInfo = new HttpUserInfo(AuditMessage.isEnableDNSLookups());
        try {
            String patId = kos.getString(Tag.PatientID);
            String patName = kos.getString(Tag.PatientName);
            StudyDeletedMessage msg; 
            DicomElement crpeSeq = kos.get(Tag.CurrentRequestedProcedureEvidenceSequence);
            for ( int i = 0, len = crpeSeq.countItems() ; i < len ; i++ ) {
                msg = new StudyDeletedMessage();
                msg.addUserPerson(userInfo.getUserId(), null, null, userInfo
                        .getHostName(), true);
                msg.addPatient(patId, patName);
                DicomObject crpeSeqItem = kos.get(Tag.CurrentRequestedProcedureEvidenceSequence).getDicomObject(i);
                msg.addStudy(crpeSeqItem.getString(Tag.StudyInstanceUID),
                        getStudyDescription(crpeSeqItem, true));
                msg.setOutcomeIndicator(success ? AuditEvent.OutcomeIndicator.SUCCESS : AuditEvent.OutcomeIndicator.MINOR_FAILURE);
                msg.validate();
                auditLog.info(msg.toString());
            }
        } catch (Exception x) {
            log.warn("Audit Log 'Study Deleted' failed:", x);
        }
    }

    public static void logDicomObjectUpdated(boolean success, String patId, String patName, String[] studyIUIDs, DicomObject obj, String detailMessage) {
        HttpUserInfo userInfo = new HttpUserInfo(AuditMessage.isEnableDNSLookups());
        try {
            InstancesAccessedMessage msg = new InstancesAccessedMessage(InstancesAccessedMessage.UPDATE);
            msg.setOutcomeIndicator(success ? AuditEvent.OutcomeIndicator.SUCCESS : AuditEvent.OutcomeIndicator.MINOR_FAILURE);
            msg.addUserPerson(userInfo.getUserId(), null, null, userInfo.getHostName(), true);
            msg.addPatient(patId, patName);
            ParticipantObject study;
            for ( int i = 0; i < studyIUIDs.length ; i++ ) {
                study = msg.addStudy(studyIUIDs[i], getStudyDescription(obj));
                if ( detailMessage != null )
                    study.addParticipantObjectDetail("Description", detailMessage);
            }
            msg.validate();
            auditLog.info(msg.toString());
        } catch (Exception x) {
            log.warn("Audit Log 'Instances Accessed' (actionCode:U) failed:", x);
        }
    }
    
    public static void logPatientRecord(AuditEvent.ActionCode actionCode, boolean success, String patId, String patName) {
        HttpUserInfo userInfo = new HttpUserInfo(AuditMessage.isEnableDNSLookups());
        try {
            PatientRecordMessage msg = new PatientRecordMessage(actionCode);
            msg.setOutcomeIndicator(success ? AuditEvent.OutcomeIndicator.SUCCESS : AuditEvent.OutcomeIndicator.MINOR_FAILURE);
            msg.addUserPerson(userInfo.getUserId(), null, null, userInfo
                    .getHostName(), true);
            msg.addPatient(patId, patName);
            msg.validate();
            auditLog.info(msg.toString());
        } catch (Exception x) {
            log.warn("Audit Log 'Patient Record' (actionCode:" + actionCode + ") failed:", x);
        }
    }
    
    public static void logExport(String mediaType, List<DicomObject> objs, boolean success) {
        try {
            HttpUserInfo userInfo = new HttpUserInfo(AuditMessage.isEnableDNSLookups());
            String user = userInfo.getUserId();
            String host = userInfo.getHostName();
            DataExportMessage msg = new DataExportMessage();
            msg.setOutcomeIndicator( AuditEvent.OutcomeIndicator.SUCCESS );
            msg.addExporterProcess(AuditMessage.getProcessID(), 
                    AuditMessage.getLocalAETitles(),
                    AuditMessage.getProcessName(), false,
                    AuditMessage.getLocalHostName());
            msg.addDataRepository(userInfo.getURL());
            msg.addDestinationMedia(host, null, mediaType, user == null, host );
            if (user != null) {
                ActiveParticipant ap = ActiveParticipant.createActivePerson(user, null, user, null, true);
                msg.addActiveParticipant(ap);

            }
            HashSet<String> pats = new HashSet<String>();
            HashMap<String, HashMap<String, int[]>> studies = new HashMap<String, HashMap<String, int[]>>(); //studyIuid, sopClassUID, nrOfInstances
            String patID, patName;
            HashMap<String, int[]> sopClasses;
            int[] noi;
            for (DicomObject obj : objs) {
                patID = obj.getString(Tag.PatientID);
                patName = obj.getString(Tag.PatientName);
                if (pats.add(patID+"_"+patName)) {
                    msg.addPatient(patID, patName);
                }
                sopClasses = studies.get(obj.getString(Tag.StudyInstanceUID));
                if (sopClasses == null) {
                    sopClasses = new HashMap<String, int[]>();
                    studies.put(obj.getString(Tag.StudyInstanceUID), sopClasses);
                }
                noi = sopClasses.get(obj.getString(Tag.SOPClassUID));
                if (noi == null) {
                    sopClasses.put(obj.getString(Tag.SOPClassUID), new int[]{1});
                } else {
                    noi[0]++;
                }
            }
            for (Map.Entry<String, HashMap<String, int[]>> study : studies.entrySet()) {
                ParticipantObjectDescription desc = new ParticipantObjectDescription();
                for (Map.Entry<String, int[]> sopCl : study.getValue().entrySet()) {
                    SOPClass sopClass = new SOPClass(sopCl.getKey());
                    sopClass.setNumberOfInstances(sopCl.getValue()[0]);
                    desc.addSOPClass(sopClass);
                }
                msg.addStudy(study.getKey(), desc);
            }
            msg.validate();
            auditLog.info(msg.toString());
        } catch (Exception x) {
            log.warn("Audit Log 'Data Export' failed:", x);
        }
    }

    public static void logProcedureRecord(ActionCode actionCode, boolean success, DicomObject patAttrs, String studyIuid, String accNr, String desc) {
        HttpUserInfo userInfo = new HttpUserInfo(AuditMessage.isEnableDNSLookups());
        try {
            ProcedureRecordMessage msg = new ProcedureRecordMessage(actionCode);
            msg.setOutcomeIndicator(success ? AuditEvent.OutcomeIndicator.SUCCESS : AuditEvent.OutcomeIndicator.MINOR_FAILURE);
            msg.addUserPerson(userInfo.getUserId(), null, null, userInfo
                    .getHostName(), true);
            msg.addPatient(patAttrs.getString(Tag.PatientID), patAttrs.getString(Tag.PatientName));
            if (studyIuid != null) {
                ParticipantObjectDescription poDesc = new ParticipantObjectDescription();
                if (accNr != null)
                    poDesc.addAccession(accNr);
                ParticipantObject study = msg.addStudy(studyIuid, poDesc);
                if (desc != null)
                    study.addParticipantObjectDetail("Description", desc);
            }
            msg.validate();
            auditLog.info(msg.toString());
        } catch (Exception x) {
            log.warn("Audit Log 'Procedure Record' failed:", x);
        }
    }

    public static void logSecurityAlert(TypeCode code, boolean success, String desc) {
        try {
            SecurityAlertMessage msg = getSecurityAlertMessage(code, success, desc);
            msg.validate();
            auditLog.info(msg.toString());
        } catch (Exception e) {
            log.warn("Failed to log ActorConfig:", e);
        }
    }

    public static void logSecurityAlert(TypeCode code, boolean success, String desc, String patID, String patName, String studyIUID ) {
        try {
            SecurityAlertMessage msg = getSecurityAlertMessage(code, success, desc);
            msg.addParticipantObject(ParticipantObject.createPatient(patID, patName));
            if (studyIUID != null) {
                msg.addParticipantObject(ParticipantObject.createStudy(studyIUID, null));
            }
            msg.validate();
            auditLog.info(msg.toString());
        } catch (Exception e) {
            log.warn("Failed to log ActorConfig:", e);
        }
    }

    public static SecurityAlertMessage getSecurityAlertMessage(TypeCode code, boolean success, String desc) {
        HttpUserInfo userInfo = new HttpUserInfo(AuditMessage.isEnableDNSLookups());
        SecurityAlertMessage msg = new SecurityAlertMessage(code);
        msg.setOutcomeIndicator(success ? AuditEvent.OutcomeIndicator.SUCCESS : AuditEvent.OutcomeIndicator.MINOR_FAILURE);
        msg.addReportingProcess(AuditMessage.getProcessID(),
                AuditMessage.getLocalAETitles(),
                AuditMessage.getProcessName(),
                AuditMessage.getLocalHostName());
        if ( userInfo.getHostName() != null ) {
                msg.addPerformingPerson(userInfo.getUserId(), null, null, userInfo.getHostName());
        } else {
            msg.addPerformingNode(AuditMessage.getLocalHostName());
        }
        msg.addAlertSubjectWithNodeID(AuditMessage.getLocalNodeID(), desc);
        return msg;
    }
    
    public static void logSoftwareConfiguration(boolean success, String desc) {
        logSecurityAlert(SecurityAlertMessage.SOFTWARE_CONFIGURATION, success, desc);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static boolean addChangeOfCollection(StringBuilder sb, boolean changed, String valueName, 
            Collection oldValue, Collection newValue) {
        if (oldValue == null && newValue == null) {
            return false;
        }
        Collection addedValues = null;
        Collection removedValues = null;
        if (oldValue == null) {
            addedValues = newValue;
        } else if (newValue == null) {
            removedValues = oldValue;
        } else {
            if (newValue.size() >= oldValue.size() && newValue.containsAll(oldValue)) {
                if (newValue.size() == oldValue.size()) {
                    return false;
                } else {
                    addedValues = new ArrayList(newValue.size());
                    addedValues.addAll(newValue);
                    addedValues.removeAll(oldValue);
                }
            } else {
                addedValues = new ArrayList(newValue.size());
                addedValues.addAll(newValue);
                addedValues.removeAll(oldValue);
                removedValues = new ArrayList(oldValue.size());
                addedValues.addAll(oldValue);
                addedValues.removeAll(newValue);
            }
        }
        sb.append(changed ? " ; " : "Change ").append(valueName).append(":");
        if (removedValues != null && removedValues.size() > 0) {
            sb.append(" removed:[");
            for (Object o : removedValues) {
                sb.append(o).append(',');
            }
            sb.setLength(sb.length()-1);
            sb.append("]");
        }
        if (addedValues != null && addedValues.size() > 0) {
            sb.append(" added:[");
            for (Object o : addedValues) {
                sb.append(o).append(',');
            }
            sb.setLength(sb.length()-1);
            sb.append("]");
        }
        return true;
    }

    public static boolean addChange(StringBuilder sb, boolean changed, String valueName, Object oldValue, Object newValue) {
        if (oldValue == newValue || oldValue!= null && oldValue.equals(newValue)) {
            return false;
        }
        sb.append(changed ? " ; " : "Change ").append(valueName).append(" from ").append(oldValue).append(" to ").append(newValue);
        return true;
    }
    
    private static ParticipantObjectDescription getStudyDescription(DicomObject obj) {
        ParticipantObjectDescription desc = new ParticipantObjectDescription();
        if (obj.containsValue(Tag.AccessionNumber)) {
            desc.addAccession(obj.getString(Tag.AccessionNumber));
        }
        if (obj.containsValue(Tag.SOPClassUID)) {
            ParticipantObjectDescription.SOPClass sopClass = 
                new ParticipantObjectDescription.SOPClass(obj.getString(Tag.SOPClassUID));
            sopClass.addInstance(obj.getString(Tag.SOPInstanceUID));
            desc.addSOPClass(sopClass);
        }
        return desc;
    }

    private static ParticipantObjectDescription getStudyDescription(DicomObject crpeSeqItem, boolean addIUID) {
        ParticipantObjectDescription desc = new ParticipantObjectDescription();
        String accNr = crpeSeqItem.getString(Tag.AccessionNumber);
        if (accNr != null)
            desc.addAccession(accNr);
        addSOPClassInfo(desc, crpeSeqItem, addIUID);
        return desc;
    }

    private static String getStudySeriesDetail(String detailMessage, DicomObject crpeSeqItem) {
        DicomElement refSeries = crpeSeqItem.get(Tag.ReferencedSeriesSequence);
        StringBuffer sb = new StringBuffer();
        sb.append(detailMessage);
        int len = refSeries.countItems();
        if ( len > 0 ) {
            sb.append(refSeries.getDicomObject(0).getString(Tag.SeriesInstanceUID));
            for (int i = 1; i < len; i++) {
                sb.append(", ").append(refSeries.getDicomObject(i).getString(Tag.SeriesInstanceUID));
            }
        }
        return sb.toString();
    }

    private static void addSOPClassInfo(ParticipantObjectDescription desc,
            DicomObject studyMgtDs, boolean addIUID) {
        DicomElement refSeries = studyMgtDs.get(Tag.ReferencedSeriesSequence);
        if (refSeries == null)
            return;
        String suid = studyMgtDs.getString(Tag.StudyInstanceUID);
        InstanceSorter sorter = new InstanceSorter();
        DicomObject ds;
        DicomElement refSopSeq;
        for (int i = 0, len = refSeries.countItems(); i < len; i++) {
            refSopSeq = refSeries.getDicomObject(i).get(Tag.ReferencedSOPSequence);
            if (refSopSeq != null) {
                for (int j = 0, jlen = refSopSeq.countItems(); j < jlen; j++) {
                    ds = refSopSeq.getDicomObject(j);
                    sorter.addInstance(suid,
                            ds.getString(Tag.ReferencedSOPClassUID),
                            ds.getString(Tag.ReferencedSOPInstanceUID), null);
                }
            }
        }
        
        for (String cuid : sorter.getCUIDs(suid)) {
            ParticipantObjectDescription.SOPClass sopClass = new ParticipantObjectDescription.SOPClass(cuid);
            sopClass.setNumberOfInstances(sorter.countInstances(suid, cuid));
            if ( addIUID ) {
                for ( String iuid : sorter.getIUIDs(suid, cuid) ) {
                    sopClass.addInstance(iuid);
                }
            }
            desc.addSOPClass(sopClass);
        }
    }

}
