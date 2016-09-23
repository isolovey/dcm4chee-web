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
package org.dcm4chee.web.service.modify;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.jms.MessageListener;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4che2.data.UIDDictionary;
import org.dcm4che2.data.VR;
import org.dcm4che2.net.Association;
import org.dcm4che2.net.DataWriterAdapter;
import org.dcm4che2.net.DimseRSPHandler;
import org.dcm4che2.net.NoPresentationContextException;
import org.dcm4che2.net.TransferCapability;
import org.dcm4che2.util.StringUtils;
import org.dcm4che2.util.UIDUtils;
import org.dcm4chee.web.service.common.AbstractScheduledScuService;
import org.dcm4chee.web.service.common.DicomActionOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author franz.willer@gmail.com
 * @version $Revision$ $Date$
 * @since Feb 11, 2010
 */
public class AttributesModificationScuService extends AbstractScheduledScuService implements MessageListener {

    private String[] calledAETs;
    private String modifyingSystem;
    private String reasonForTheAttributeModification;
    
    private int noRetryErrorCode;

    private TransferCapability tcMod = new TransferCapability(
            UID.Dcm4cheAttributesModificationNotificationSOPClass,
            NATIVE_LE_TS, TransferCapability.SCU);

    private static Logger log = LoggerFactory.getLogger(AttributesModificationScuService.class);
    
    public AttributesModificationScuService() {
        super();
        setTransferCapability(new TransferCapability[]{tcMod});
    }

    public String getCalledAETs() {
        return calledAETs == null ? NONE : StringUtils.join(calledAETs, '\\');
    }

    public void setCalledAETs(String calledAET) {
        this.calledAETs = NONE.equals(calledAET) ? null : StringUtils.split(calledAET, '\\');
    }

    public String getModifyingSystem() {
        return modifyingSystem;
    }

    public void setModifyingSystem(String modifyingSystem) {
        this.modifyingSystem = modifyingSystem;
    }

    public String getReasonForTheAttributeModification() {
        return reasonForTheAttributeModification;
    }

    public void setReasonForTheAttributeModification(
            String reasonForTheAttributeModification) {
        this.reasonForTheAttributeModification = reasonForTheAttributeModification;
    }

    public final void setNoRetryErrorCode(String noRetryErrorCode) {
        int endPos = noRetryErrorCode.length()-1;
        this.noRetryErrorCode = noRetryErrorCode.charAt(endPos) == 'H' ? 
                Integer.parseInt(noRetryErrorCode.substring(0, endPos), 16) : 
                Integer.parseInt(noRetryErrorCode);
    }

    public final String getNoRetryErrorCode() {
        return String.format("%04XH", noRetryErrorCode);
    }
    
    public void scheduleModification(DicomObject obj, String level, String system, String reason) throws Exception {
        obj.putString(Tag.QueryRetrieveLevel, VR.CS, level);
        obj.putString(Tag.ModifyingSystem, VR.LO, system == null ? this.modifyingSystem : system);
        obj.putString(Tag.ReasonForTheAttributeModification, VR.CS, reason == null ? this.reasonForTheAttributeModification : reason);
        doScheduleModification(obj);
    }
    
    public void scheduleModification(DicomObject obj) throws Exception {
        if (!obj.contains(Tag.ModifyingSystem)) 
            obj.putString(Tag.ModifyingSystem, VR.LO, this.modifyingSystem);
        if (!obj.contains(Tag.ReasonForTheAttributeModification)) 
            obj.putString(Tag.ReasonForTheAttributeModification, VR.CS, this.reasonForTheAttributeModification);
        doScheduleModification(obj);
    }

    private void doScheduleModification(DicomObject obj) throws Exception {
        log.info("Schedule AttributesModification order! obj:\n"+obj);
        String level = obj.getString(Tag.QueryRetrieveLevel);
        if (level == null)
            throw new IllegalArgumentException("Missing Query/Retrieve Level");
        if ("IMAGE".equals(level)) {
            if (!obj.containsValue(Tag.SOPInstanceUID))
                throw new IllegalArgumentException("Missing SOP Instance UID on IMAGE level!");
        } else if ("SERIES".equals(level)) {
            if (!obj.containsValue(Tag.SeriesInstanceUID))
                throw new IllegalArgumentException("Missing Series Instance UID on SERIES level!");
        } else if ("STUDY".equals(level)) {
            if (!obj.containsValue(Tag.StudyInstanceUID))
                throw new IllegalArgumentException("Missing Study Instance UID on STUDY level!");
        } else if ("PATIENT".equals(level)) {
            if (!obj.containsValue(Tag.PatientID))
                throw new IllegalArgumentException("Missing Patient ID on PATIENT level!");
            if (!obj.containsValue(Tag.StudyInstanceUID))
                throw new IllegalArgumentException("Missing Study Instance UID on PATIENT level!");
        } else {
            throw new IllegalArgumentException("Illegal Query/Retrieve Level: " + level);
        }
        if (calledAETs != null) {
            for ( String aet : calledAETs) {
                schedule( new DicomActionOrder(aet,obj, "DCM4CHE Attributes Modification"));
            }
        }
    }
    
    public void process(DicomActionOrder order) throws Exception {
        this.sendModification(order.getDestAET(), order.getDicomObject());
    }
        
    private int sendModification(String aet, DicomObject obj) throws IOException, InterruptedException, GeneralSecurityException {
        String iuid = UIDUtils.createUID();
        Association assoc = open(aet);
        TransferCapability tc = assoc.getTransferCapabilityAsSCU(UID.Dcm4cheAttributesModificationNotificationSOPClass);
        if (tc == null) {
            throw new NoPresentationContextException(UIDDictionary.getDictionary().prompt(UID.Dcm4cheAttributesModificationNotificationSOPClass));
        }
        RspHandler rspHandler = new RspHandler();
        String tsuid = tc.getTransferSyntax()[0];
        LOG.debug("Send Attributes Modification to {}:\n{}", aet, obj);
        assoc.cstore(UID.Dcm4cheAttributesModificationNotificationSOPClass, iuid, priority, new DataWriterAdapter(obj), tsuid, rspHandler);
        assoc.waitForDimseRSP();
        try {
            assoc.release(true);
        } catch (InterruptedException t) {
            LOG.error("Association release failed! aet:"+aet, t);
        }
        int status = rspHandler.getStatus();
        if (status != 0) {
            String msg = "Received Error Status " + Integer.toHexString(status) + "H, Error Comment: "
                    + rspHandler.getErrorComment();
            LOG.warn(msg);
            if (status != noRetryErrorCode)
                throw new RuntimeException("Send Attributes Modification failed! reason:"+msg);
        }
        return status;
    }

    private class RspHandler extends DimseRSPHandler {
        private int status;
        private String errorComment;

        public int getStatus() {
            return status;
        }
        
        public String getErrorComment() {
            return errorComment;
        }

        @Override
        public void onDimseRSP(Association as, DicomObject cmd,
                DicomObject data) {
            int status = cmd.getInt(Tag.Status);

            switch (status) {
            case 0x0000:
            case 0x0001:
            case 0x0002:
            case 0x0003:
                break;
            case 0x0116:
                LOG.warn("Received Warning Status 116H (=Attribute Value Out of Range) from remote AE {}", as.getCalledAET());
                errorComment = cmd.getString(Tag.ErrorComment);
                break;
            default:
                LOG.error("Sending C-STORE request for AttributesModificationNotification failed with status {}H at calledAET:{}", StringUtils.shortToHex(status), as.getCalledAET());
                errorComment = cmd.getString(Tag.ErrorComment);
            }
        }
    }

}

