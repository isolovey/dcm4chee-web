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
package org.dcm4chee.web.service.mwl;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.ElementDictionary;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4che2.data.UIDDictionary;
import org.dcm4che2.data.VR;
import org.dcm4che2.net.Association;
import org.dcm4che2.net.CommandUtils;
import org.dcm4che2.net.DimseRSP;
import org.dcm4che2.net.NoPresentationContextException;
import org.dcm4che2.net.TransferCapability;
import org.dcm4che2.util.TagUtils;
import org.dcm4chee.web.service.common.AbstractScuService;

/**
 * @author franz.willer@gmail.com
 * @version $Revision$ $Date$
 * @since Jun 07, 2011
 */
public class MwlScuService extends AbstractScuService {

    private List<String> worklistProviders = new ArrayList<String>();
    private String defaultWorklistProvider;
    
    private TransferCapability tcMWL = new TransferCapability(
            UID.ModalityWorklistInformationModelFIND,
            NATIVE_LE_TS, TransferCapability.SCU);

    ElementDictionary tagDict = ElementDictionary.getDictionary();
    
    public MwlScuService() {
        super();
        setTransferCapability(new TransferCapability[]{tcMWL});
    }

    public String getWorklistProviders() {
        return list2string(worklistProviders, "\\");
    }

    public void setWorklistProviders(String calledAET) {
        this.worklistProviders = string2list(calledAET, "\\");
    }
    
    public List<String> getWorklistProviderList() {
        ArrayList<String> l = new ArrayList<String>();
        l.addAll(worklistProviders);
        return l;
    }

    public String getDefaultWorklistProvider() {
        return defaultWorklistProvider;
    }

    public void setDefaultWorklistProvider(String defaultWorklistProvider) {
        this.defaultWorklistProvider = defaultWorklistProvider;
    }

    public List<DicomObject> queryMWL(String aet, DicomObject searchDS, int maxResults, boolean complementAll) throws IOException, InterruptedException, GeneralSecurityException {
        if (maxResults < 1)
            maxResults = Integer.MAX_VALUE;
        if (complementAll)
            searchDS = complementAllResultAttributes(searchDS);
        List<DicomObject> mwlItems = new ArrayList<DicomObject>();
        Association assoc = open(aet);
        try {
            TransferCapability tc = assoc.getTransferCapabilityAsSCU(UID.ModalityWorklistInformationModelFIND);
            if (tc == null) {
                throw new NoPresentationContextException(UIDDictionary.getDictionary().prompt(UID.ModalityWorklistInformationModelFIND));
            } else {
                String tsuid = tc.getTransferSyntax()[0];
                LOG.debug("send MWL C-FIND request to {}:\n{}", aet, searchDS);
                DimseRSP rsp = assoc.cfind(UID.ModalityWorklistInformationModelFIND, priority, searchDS, 
                        tsuid, maxResults);
                while (rsp.next()) {
                    DicomObject cmd = rsp.getCommand();
                    if (CommandUtils.isPending(cmd)) {
                        if (mwlItems.size() < maxResults) {
                            DicomObject data = rsp.getDataset();
                            mwlItems.add(data);
                            LOG.info("MWL C-FIND Response #{}:\n{}", mwlItems.size(), data);
                        } else {
                            LOG.info("MWL C-FIND Response ignored after receiving {} responses.", maxResults);
                        }
                    }
                }
            }
        } finally {
            try {
                assoc.release(true);
                LOG.info("Released connection to {}", aet);
            } catch (InterruptedException x) {
                LOG.error("Failed to release association! reason:"+x.getMessage(), x);
            }
        }
        return mwlItems;
    }

    private DicomObject complementAllResultAttributes(DicomObject searchDS) {
        DicomElement spsSq = searchDS.get(Tag.ScheduledProcedureStepSequence);
        DicomObject item;
        if (spsSq == null || spsSq.isEmpty()) {
            item = new BasicDicomObject();
            searchDS.putSequence(Tag.ScheduledProcedureStepSequence).addDicomObject(item);
        } else {
            while (spsSq.countItems() > 1) {
                item = spsSq.removeDicomObject(1);
                LOG.warn("ScheduledProcedureStepSequence should contain only one item! remove item:\n{}", item);
            }
            item = spsSq.getDicomObject(0);
        }
        complement(item, Tag.ScheduledStationAETitle, VR.AE);
        complement(item, Tag.ScheduledProcedureStepStartDate, VR.DA);
        complement(item, Tag.ScheduledProcedureStepStartTime, VR.TM);
        complement(item, Tag.Modality, VR.CS);
        complement(item, Tag.ScheduledPerformingPhysicianName, VR.PN);
        complement(item, Tag.ScheduledProcedureStepDescription, VR.LO);
        complement(item, Tag.ScheduledProcedureStepLocation, VR.SH);
        complement(item, Tag.ScheduledProtocolCodeSequence, VR.SQ);
        complement(item, Tag.PreMedication, VR.LO);
        complement(item, Tag.ScheduledProcedureStepID, VR.SH);
        complement(item, Tag.RequestedContrastAgent, VR.LO);
        complement(item, Tag.ScheduledProcedureStepStatus, VR.CS);
        // Requested Procedure
        complement(searchDS, Tag.RequestedProcedureID, VR.SH);
        complement(searchDS, Tag.RequestedProcedureDescription, VR.LO);
        complement(searchDS, Tag.RequestedProcedureCodeSequence, VR.SQ);
        complement(searchDS, Tag.StudyInstanceUID, VR.UI);
        complement(searchDS, Tag.StudyDate, VR.DA);
        complement(searchDS, Tag.StudyTime, VR.TM);
        complement(searchDS, Tag.ReferencedStudySequence, VR.SQ);
        complement(searchDS, Tag.RequestedProcedurePriority, VR.SH);
        complement(searchDS, Tag.PatientTransportArrangements, VR.LO);
        //other Attributess from requested procedure Module
        complement(searchDS, Tag.ReasonForTheRequestedProcedure, VR.LO);
        complement(searchDS, Tag.ReasonForRequestedProcedureCodeSequence, VR.SQ);
        complement(searchDS, Tag.RequestedProcedureComments, VR.LT);
        complement(searchDS, Tag.RequestedProcedureLocation, VR.LO);
        complement(searchDS, Tag.ConfidentialityCode, VR.LO);
        complement(searchDS, Tag.ReportingPriority, VR.SH);
        complement(searchDS, Tag.NamesOfIntendedRecipientsOfResults, VR.PN);
        complement(searchDS, Tag.IntendedRecipientsOfResultsIdentificationSequence, VR.SQ);
        //imaging service request
        complement(searchDS, Tag.AccessionNumber, VR.SH);
        complement(searchDS, Tag.RequestingPhysician, VR.PN);
        complement(searchDS, Tag.ReferringPhysicianName, VR.PN);
        complement(searchDS, Tag.ImagingServiceRequestComments, VR.LT);
        complement(searchDS, Tag.RequestingPhysicianIdentificationSequence, VR.SQ);
        complement(searchDS, Tag.ReferringPhysicianIdentificationSequence, VR.SQ);
        complement(searchDS, Tag.RequestingService, VR.LO);
        complement(searchDS, Tag.IssueDateOfImagingServiceRequest, VR.DA);
        complement(searchDS, Tag.IssueTimeOfImagingServiceRequest, VR.TM);
        complement(searchDS, Tag.PlacerOrderNumberImagingServiceRequest, VR.LO);
        complement(searchDS, Tag.FillerOrderNumberImagingServiceRequest, VR.LO);
        complement(searchDS, Tag.OrderEnteredBy, VR.PN);
        complement(searchDS, Tag.OrderEntererLocation, VR.SH);
        complement(searchDS, Tag.OrderCallbackPhoneNumber, VR.SH);
        //Patient/Visit Identification
        complement(searchDS, Tag.PatientName, VR.PN);
        complement(searchDS, Tag.PatientID, VR.LO);
        complement(searchDS, Tag.AdmissionID, VR.LO);
        complement(searchDS, Tag.CurrentPatientLocation, VR.LO);
        complement(searchDS, Tag.ReferencedPatientSequence, VR.SQ);
        complement(searchDS, Tag.PatientBirthDate, VR.DA);
        complement(searchDS, Tag.PatientSex, VR.CS);
        complement(searchDS, Tag.PatientWeight, VR.DS);
        complement(searchDS, Tag.ConfidentialityConstraintOnPatientDataDescription, VR.LO);
        complement(searchDS, Tag.PatientState, VR.LO);
        complement(searchDS, Tag.PregnancyStatus, VR.US);
        complement(searchDS, Tag.MedicalAlerts, VR.LO);
        complement(searchDS, Tag.Allergies, VR.LO);
        complement(searchDS, Tag.SpecialNeeds, VR.LO);
        return searchDS;
    }
    
    private void complement(DicomObject obj, int tag, VR vr) {
        if (!obj.contains(tag)) {
            obj.putNull(tag, vr);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Complement MWL searchDS with tag {} : {}", 
                        TagUtils.toString(tag), tagDict.nameOf(tag));
            }
        }
    }
    
    private String list2string(List<String> list, String sep) {
        if (list.isEmpty()) 
            return NONE;
        StringBuilder sb = new StringBuilder();
        for (String m : list) {
            sb.append(m).append(sep);
        }
        sb.setLength(sb.length()-1);
        return sb.toString();
    }
    private List<String> string2list(String s, String sep) {
        ArrayList<String> l = new ArrayList<String>();
        if (NONE.equals(s))
            return l;
        StringTokenizer st = new StringTokenizer(s, sep);
        while (st.hasMoreTokens()) {
            l.add(st.nextToken());
        }
        return l;
    }

}

