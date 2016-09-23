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
package org.dcm4chee.web.service.rejnote;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import javax.jms.MessageListener;

import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4che2.data.UIDDictionary;
import org.dcm4che2.net.Association;
import org.dcm4che2.net.DataWriterAdapter;
import org.dcm4che2.net.DimseRSPHandler;
import org.dcm4che2.net.NoPresentationContextException;
import org.dcm4che2.net.TransferCapability;
import org.dcm4che2.util.StringUtils;
import org.dcm4chee.archive.common.PrivateTag;
import org.dcm4chee.web.service.common.AbstractScheduledScuService;
import org.dcm4chee.web.service.common.DicomActionOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author franz.willer@gmail.com
 * @version $Revision$ $Date$
 * @since Feb 11, 2010
 */
public class RejectionNoteScuService extends AbstractScheduledScuService implements MessageListener {

    private String[] calledAETs;
    private HashMap<String, List<String>> ignoreSourceAETs = new HashMap<String, List<String>>();
    
    private static Logger log = LoggerFactory.getLogger(RejectionNoteScuService.class);
    
    public RejectionNoteScuService() {
        super();
        TransferCapability tc = new TransferCapability(
                UID.KeyObjectSelectionDocumentStorage,
                NATIVE_LE_TS, TransferCapability.SCU);
        setTransferCapability(new TransferCapability[]{tc});
    }

    public String getCalledAETs() {
        return calledAETs == null ? NONE : StringUtils.join(calledAETs, '\\');
    }

    public void setCalledAETs(String calledAET) {
        this.calledAETs = NONE.equals(calledAET) ? null : StringUtils.split(calledAET, '\\');
    }

    public String getIgnoreSourceAETs() {
        if (ignoreSourceAETs.isEmpty())
            return NONE;
        StringBuilder sb = new StringBuilder();
        for (Entry<String, List<String>> e : ignoreSourceAETs.entrySet()) {
            sb.append(e.getKey()).append(":");
            for (String s : e.getValue()) {
                sb.append(s).append(',');
            }
            sb.setLength(sb.length()-1);
            sb.append(LINE_SEPARATOR);
        }
        return sb.toString();
    }

    public void setIgnoreSourceAETs(String aets) {
        ignoreSourceAETs.clear();
        if (!NONE.equals(aets)) {
            StringTokenizer st = new StringTokenizer(aets, "\n\t\r,;");
            String tk, aet;
            ArrayList<String> ignoreAets;
            int pos;
            while (st.hasMoreElements()) {
                tk = st.nextToken();
                pos = tk.indexOf(':');
                if (pos != -1) {
                    aet = tk.substring(0, pos);
                    ignoreAets = new ArrayList<String>();
                    for (String s : StringUtils.split(tk.substring(++pos), ',')) {
                        ignoreAets.add(s.trim());
                    }
                    ignoreSourceAETs.put(aet, ignoreAets);
                }
            }            
        }
    }

    
    public void scheduleRejectionNote(DicomObject kos) throws Exception {
        if (calledAETs != null) {
            DicomObject scheduledKos;
            for ( String aet : calledAETs) {
                scheduledKos = prepareKOS(kos, aet);
                if (scheduledKos != null) {
                    schedule(new DicomActionOrder(aet, scheduledKos, "Rejection Note"));
                } else {
                    log.debug("Schedule Rejection Note to {} ignored!", aet);
                }
            }
        }
    }
    
    private DicomObject prepareKOS(DicomObject kos, String calledAet) {
        List<String> ignoreAets = ignoreSourceAETs.get(calledAet);
        log.debug("ignoreSourceAETs:{}",ignoreSourceAETs);
        if (ignoreAets == null)
            return kos;
        DicomObject newKos = new BasicDicomObject();
        kos.copyTo(newKos);
        DicomElement seq = newKos.get(Tag.CurrentRequestedProcedureEvidenceSequence);
        DicomObject item, refSeriesSeqItem;
        DicomElement refSeriesSeq;
        String srcAet;
        int countSeriesToReject = 0;
        log.debug("Number of CurrentRequestedProcedureEvidence items:"+seq.countItems());
        for (int i = 0, len =seq.countItems() ; i < len ; i++) {
            item = seq.getDicomObject(i);
            refSeriesSeq = item.get(Tag.ReferencedSeriesSequence);
            log.debug("Number of series in item {}:",refSeriesSeq.countItems());
            for (int j = refSeriesSeq.countItems() ; j > 0 ; ) {
                refSeriesSeqItem = refSeriesSeq.getDicomObject(--j);
                srcAet = refSeriesSeqItem.getString(
                        refSeriesSeqItem.resolveTag(PrivateTag.CallingAET, PrivateTag.CreatorID));
                if (ignoreAets.contains(srcAet)) {
                    log.info("Series "+refSeriesSeqItem.getString(Tag.SeriesInstanceUID)+
                            " removed from RejectionNote! Source AET "+srcAet+
                            " is ignored for calledAET "+calledAet);
                    refSeriesSeq.removeDicomObject(j);
                }
            }
            log.debug("Remaining number of series in item {}:",refSeriesSeq.countItems());
            countSeriesToReject += refSeriesSeq.countItems();
        }
        log.debug("Remaining series to reject:{}",countSeriesToReject);
        return countSeriesToReject == 0 ? null : newKos;
    }

    public void process(DicomActionOrder order) throws Exception {
        this.sendRejectionNote(order.getDestAET(), order.getDicomObject());
    }
    private int sendRejectionNote(String aet, DicomObject kos) throws IOException, InterruptedException, GeneralSecurityException {
        Association assoc = open(aet);
        TransferCapability tc = assoc.getTransferCapabilityAsSCU(UID.KeyObjectSelectionDocumentStorage);
        if ( tc == null ) {
            throw new NoPresentationContextException(UIDDictionary.getDictionary().prompt(UID.KeyObjectSelectionDocumentStorage));
        }
        
        String cuid = tc.getSopClass();
        String tsuid = tc.getTransferSyntax()[0];
        LOG.debug("Send C-STORE request for Rejection Note to {}:\n{}", aet, kos);
        RspHandler rspHandler = new RspHandler();
        assoc.cstore(cuid, kos.getString(Tag.SOPInstanceUID), priority, 
                new DataWriterAdapter(kos), tsuid, rspHandler);
        assoc.waitForDimseRSP();
        try {
            assoc.release(true);
        } catch (InterruptedException t) {
            LOG.error("Association release failed! aet:"+aet, t);
        }
        return rspHandler.getStatus();
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
            case 0:
                LOG.debug("Rejection Note KOS stored at {}",as.getCalledAET());
                break;
            case 0xB000:
            case 0xB006:
            case 0xB007:
                LOG.warn("Rejection Note KOS stored at {} with Status {}H",as.getCalledAET(), StringUtils.shortToHex(status));
                break;
            default:
                LOG.error("Sending Rejection Note KOS failed with Status {}H at calledAET:{}", StringUtils.shortToHex(status), as.getCalledAET());
            }
        }
    }

}

