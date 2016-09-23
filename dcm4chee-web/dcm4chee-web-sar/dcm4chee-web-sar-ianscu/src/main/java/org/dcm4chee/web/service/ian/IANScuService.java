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
package org.dcm4chee.web.service.ian;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.jms.MessageListener;

import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomElement;
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

/**
 * @author franz.willer@gmail.com
 * @version $Revision$ $Date$
 * @since Feb 11, 2010
 */
public class IANScuService extends AbstractScheduledScuService implements MessageListener {

    private String[] calledAETs;
    private TransferCapability tcIAN = new TransferCapability(
            UID.InstanceAvailabilityNotificationSOPClass,
            NATIVE_LE_TS, TransferCapability.SCU);
    private TransferCapability tcSCN = new TransferCapability(
            UID.BasicStudyContentNotificationSOPClassRetired,
            NATIVE_LE_TS, TransferCapability.SCU);

    private static final int[] INSTANCE_AVAILABILITY = {Tag.InstanceAvailability};
    
    public IANScuService() {
        super();
    }

    public String getCalledAETs() {
        return calledAETs == null ? NONE : StringUtils.join(calledAETs, '\\');
    }

    public void setCalledAETs(String calledAET) {
        this.calledAETs = NONE.equals(calledAET) ? null : StringUtils.split(calledAET, '\\');
    }

    public final boolean isOfferStudyContentNotification() {
        return localNAE.getTransferCapability().length == 2;
    }

    public final void setOfferStudyContentNotification(boolean offerSCN) {
        if ( getTransferCapability().length != (offerSCN ? 2 : 1) ) {
            TransferCapability[] tc = offerSCN ? new TransferCapability[]{tcIAN, tcSCN} : 
                new TransferCapability[]{tcIAN};
            setTransferCapability(tc);
        }
    }

    public void scheduleIAN(DicomObject ian) throws Exception {
        if (calledAETs != null) {
            for ( String aet : calledAETs) {
                schedule( new DicomActionOrder(aet,ian, "Instance Availability Notification"));
            }
        }
    }
    
    public void process(DicomActionOrder order) throws Exception {
        this.sendIAN(order.getDestAET(), order.getDicomObject());
    }
        
    private int sendIAN(String aet, DicomObject ian) throws IOException, InterruptedException, GeneralSecurityException {
        String iuid = UIDUtils.createUID();
        Association assoc = open(aet);
        TransferCapability tc = assoc.getTransferCapabilityAsSCU(UID.InstanceAvailabilityNotificationSOPClass);
        RspHandler rspHandler = new RspHandler();
        if (tc == null) {
            tc = assoc.getTransferCapabilityAsSCU(UID.BasicStudyContentNotificationSOPClassRetired);
            if (tc == null) {
                throw new NoPresentationContextException(UIDDictionary.getDictionary().prompt(
                        isOfferStudyContentNotification() ? UID.BasicStudyContentNotificationSOPClassRetired : 
                            UID.InstanceAvailabilityNotificationSOPClass));
            }
            String tsuid = tc.getTransferSyntax()[0];
            DicomObject scn = toSCN(ian);
            scn.putString(Tag.SOPInstanceUID, VR.UI, iuid);
            scn.putString(Tag.SOPClassUID, VR.UI, UID.BasicStudyContentNotificationSOPClassRetired);
            LOG.debug("Study Content Notification to {}:\n{}", aet, scn);
            assoc.cstore(UID.BasicStudyContentNotificationSOPClassRetired, iuid, priority, 
                    new DataWriterAdapter(scn), tsuid, rspHandler);
        } else {
            String tsuid = tc.getTransferSyntax()[0];
            LOG.debug("Instance Availability Notification to {}:\n{}", aet, ian);
            assoc.ncreate(UID.InstanceAvailabilityNotificationSOPClass, iuid, ian, tsuid, rspHandler);
        }
        assoc.waitForDimseRSP();
        try {
            assoc.release(true);
        } catch (InterruptedException t) {
            LOG.error("Association release failed! aet:"+aet, t);
        }
        return rspHandler.getStatus();
    }

    private DicomObject toSCN(DicomObject ian) {
        DicomObject scn = new BasicDicomObject();
        scn.putString(Tag.PatientID, VR.LO, ian.getString(Tag.PatientID));
        scn.putString(Tag.PatientName, VR.PN, ian.getString(Tag.PatientName));
        scn.putString(Tag.StudyID, VR.SH, ian.getString(Tag.StudyID));
        scn.putString(Tag.StudyInstanceUID, VR.UI, ian.getString(Tag.StudyInstanceUID));
        DicomElement ianSeriesSeq = ian.get(Tag.ReferencedSeriesSequence);
        DicomElement scnSeriesSeq = scn.putSequence(Tag.ReferencedSeriesSequence);
        DicomObject ianSeriesItem, scnSeriesItem, scnSOPItem;
        DicomElement ianSOPSeq, scnSOPSeq;
        for (int i = 0, n = ianSeriesSeq.countItems(); i < n; ++i) {
            ianSeriesItem = ianSeriesSeq.getDicomObject(i);
            scnSeriesItem = new BasicDicomObject();
            scnSeriesItem.putString(Tag.SeriesInstanceUID, VR.UI, ianSeriesItem
                    .getString(Tag.SeriesInstanceUID));
            scnSeriesSeq.addDicomObject(scnSeriesItem);
            ianSOPSeq = ianSeriesItem.get(Tag.ReferencedSOPSequence);
            scnSOPSeq = scnSeriesItem.putSequence(Tag.ReferencedImageSequence);
            for (int j = 0, m = ianSOPSeq.countItems(); j < m; ++j) {
                scnSOPItem = new BasicDicomObject();
                ianSOPSeq.getDicomObject(j).exclude(INSTANCE_AVAILABILITY).copyTo(scnSOPItem);
                scnSOPSeq.addDicomObject(scnSOPItem);
            }
        }
        return scn;
    }
    
    private class RspHandler extends DimseRSPHandler {
        private int status;

        public int getStatus() {
            return status;
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
                break;
            default:
                LOG.error("Sending IAN(SCN) failed with status {}H at calledAET:{}", StringUtils.shortToHex(status), as.getCalledAET());
            }
        }
    }

}

