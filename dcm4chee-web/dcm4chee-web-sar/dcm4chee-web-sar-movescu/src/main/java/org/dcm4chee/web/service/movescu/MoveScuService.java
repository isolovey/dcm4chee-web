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
package org.dcm4chee.web.service.movescu;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.management.ObjectName;

import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4che2.data.UIDDictionary;
import org.dcm4che2.data.VR;
import org.dcm4che2.net.Association;
import org.dcm4che2.net.CommandUtils;
import org.dcm4che2.net.DimseRSPHandler;
import org.dcm4che2.net.ExtRetrieveTransferCapability;
import org.dcm4che2.net.NoPresentationContextException;
import org.dcm4che2.net.TransferCapability;
import org.dcm4chee.web.service.common.AbstractScuService;
import org.dcm4chee.web.service.common.delegate.JMSDelegate;
import org.dcm4chee.web.service.common.RetryIntervalls;

/**
 * @author franz.willer@gmail.com
 * @version $Revision$ $Date$
 * @since Jul 29, 2009
 */
public class MoveScuService extends AbstractScuService implements MessageListener{

    private JMSDelegate jmsDelegate = new JMSDelegate(this);
    private HashMap<String, RetryIntervalls> retryIntervalls = new HashMap<String, RetryIntervalls>();
    private static int MESSAGE_PRIORITY_MIN = 0;
    private int concurrency;
    private String queueName;
    
    private String calledAET;
    private boolean relationQR = true;

    private static final String[] ASSOC_CUIDS = {
        UID.StudyRootQueryRetrieveInformationModelMOVE,
        UID.PatientRootQueryRetrieveInformationModelMOVE,
        UID.PatientStudyOnlyQueryRetrieveInformationModelMOVERetired,
        UID.VerificationSOPClass };
    
    private static final String[] PATIENT_LEVEL_MOVE_CUID = {
        UID.PatientRootQueryRetrieveInformationModelMOVE,
        UID.PatientStudyOnlyQueryRetrieveInformationModelMOVERetired };

    private static final String[] STUDY_LEVEL_MOVE_CUID = {
        UID.StudyRootQueryRetrieveInformationModelMOVE,
        UID.PatientRootQueryRetrieveInformationModelMOVE,
        UID.PatientStudyOnlyQueryRetrieveInformationModelMOVERetired };

    private static final String[] SERIES_LEVEL_MOVE_CUID = {
        UID.StudyRootQueryRetrieveInformationModelMOVE,
        UID.PatientRootQueryRetrieveInformationModelMOVE };
    
    private String[] QR_LEVELS = {"IMAGE", "SERIES", "STUDY", "PATIENT"};
    private String[][] QR_MOVE_CUIDS = {SERIES_LEVEL_MOVE_CUID, SERIES_LEVEL_MOVE_CUID,
            STUDY_LEVEL_MOVE_CUID, PATIENT_LEVEL_MOVE_CUID};
    
    public MoveScuService() {
        super();
        configureTransferCapability(ASSOC_CUIDS, NATIVE_LE_TS);
    }

    public String getCalledAET() {
        return calledAET;
    }

    public void setCalledAET(String calledAET) {
        this.calledAET = calledAET;
    }

    public boolean isRelationQR() {
        return relationQR;
    }

    public void setRelationQR(boolean relationQR) {
        this.relationQR = relationQR;
    }

    public String getRetryIntervalls() {
        StringBuffer sb = new StringBuffer();
        Map.Entry<String, RetryIntervalls> entry;
        String key;
        String defaultIntervalls = null;
        for ( Iterator<Map.Entry<String, RetryIntervalls>> iter = retryIntervalls.entrySet().iterator() ; iter.hasNext() ; ) {
            entry = iter.next();
            key = entry.getKey();
            if ( key == null ) {
                defaultIntervalls = entry.getValue().toString();
            } else {
                sb.append('[').append(key).append(']').append(entry.getValue());
                sb.append(AbstractScuService.LINE_SEPARATOR);
            }
        }
        if ( defaultIntervalls != null ) {
            sb.append(defaultIntervalls);
            sb.append(AbstractScuService.LINE_SEPARATOR);
        }
        
        return sb.length() > 1 ? sb.toString() : "NEVER";
    }

    public void setRetryIntervalls(String text) {
        retryIntervalls.clear();
        if ( "NEVER".equals(text)) return;
        StringTokenizer st = new StringTokenizer(text,";\r\n");
        String token, key;
        int pos;
        while ( st.hasMoreTokens()) {
            token = st.nextToken();
            pos = token.indexOf(']');
            if ( pos == -1 ) {
                retryIntervalls.put(null, new RetryIntervalls(token));
            } else {
                key = token.substring(1,pos);
                retryIntervalls.put(key, new RetryIntervalls(token.substring(pos+1)));
            }
        }
    }

    public final String getQueueName() {
        return queueName;
    }
    public final void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public final ObjectName getJmsServiceName() {
        return jmsDelegate.getJmsServiceName();
    }
    public final void setJmsServiceName(ObjectName jmsServiceName) {
        jmsDelegate.setJmsServiceName(jmsServiceName);
    }

    public final int getConcurrency() {
        return concurrency;
    }
    public final void setConcurrency(int concurrency) throws Exception {
        if (concurrency <= 0)
            throw new IllegalArgumentException("Concurrency: " + concurrency);
        if (this.concurrency != concurrency) {
            final boolean restart = getState() == STARTED;
            if (restart)
                stop();
            this.concurrency = concurrency;
            if (restart)
                start();
        }
    }
    
    protected void startService() throws Exception {
        jmsDelegate.startListening(queueName, this, concurrency);
    }

    protected void stopService() throws Exception {
        jmsDelegate.stopListening(queueName);
    }
    
    public boolean move(String retrieveAET, String moveDest, String patId, String studyIUID, String seriesIUID) throws IOException, InterruptedException, GeneralSecurityException {
        MoveRspHandler rspHandler = new MoveRspHandler();
        this.move(retrieveAET, moveDest, patId, 
                "".equals(studyIUID) ? null : new String[]{studyIUID}, 
                "".equals(seriesIUID) ? null : new String[]{seriesIUID}, null, rspHandler, true); 
        return rspHandler.getStatus() == 0;
    }
    public void move(String retrieveAET, String moveDest, String patId, String[] studyIUIDs,
            String[] seriesIUIDs, String[] sopIUIDs, 
            DimseRSPHandler rspHandler, boolean waitAndCloseAssoc ) throws IOException, InterruptedException, GeneralSecurityException {
        move(retrieveAET, moveDest, priority, patId, studyIUIDs, seriesIUIDs, sopIUIDs, 
                rspHandler, waitAndCloseAssoc);
    }
    /**
     * Perform a DICOM C-MOVE request to given Application Entity Title.
     * @throws IOException 
     * @throws InterruptedException 
     * @throws GeneralSecurityException 
     */
    private void move(String retrieveAET, String moveDest, int priority, String patId, String[] studyIUIDs, 
            String[] seriesIUIDs, String[] sopIUIDs, 
            DimseRSPHandler rspHandler, boolean waitAndCloseAssoc ) throws IOException, InterruptedException, GeneralSecurityException {
        if ( retrieveAET == null || "".equals(retrieveAET) )
            retrieveAET = calledAET;
        int qrLevelIdx = sopIUIDs != null ? 0 : seriesIUIDs != null ? 1
                : studyIUIDs != null ? 2 : 3;
        String qrLevel = QR_LEVELS[qrLevelIdx];
        String[] moveCUIDs = QR_MOVE_CUIDS[qrLevelIdx];
        Association assoc = open(retrieveAET);
        TransferCapability tc = selectTransferCapability(assoc, moveCUIDs);
        if ( tc == null ) {
            throw new NoPresentationContextException(UIDDictionary.getDictionary().prompt(moveCUIDs[0]));
        }
        String cuid = tc.getSopClass();
        String tsuid = tc.getTransferSyntax()[0];
        DicomObject keys = new BasicDicomObject();
        keys.putString(Tag.QueryRetrieveLevel, VR.CS, qrLevel);
        if (patId != null ) keys.putString(Tag.PatientID, VR.LO, patId);
        if (studyIUIDs != null ) keys.putStrings(Tag.StudyInstanceUID, VR.UI, studyIUIDs);
        if (seriesIUIDs != null ) keys.putStrings(Tag.SeriesInstanceUID, VR.UI, seriesIUIDs);
        if (sopIUIDs != null ) keys.putStrings(Tag.SOPInstanceUID, VR.UI, sopIUIDs);
        LOG.info("Send C-MOVE request using {}:\n{}",cuid, keys);
        if (rspHandler == null)
            rspHandler = new MoveRspHandler();
        assoc.cmove(cuid, priority, keys, tsuid, moveDest, rspHandler);
        if (waitAndCloseAssoc) {
            assoc.waitForDimseRSP();
            try {
                assoc.release(true);
            } catch (InterruptedException t) {
                LOG.error("Association release failed! aet:"+retrieveAET, t);
            }
        }
    }

    public void scheduleMoveInstances(String patId, String studyIuid, String seriesIuid, String[] iuids,
            String retrAet, String moveDestination, Integer prio) throws Exception {
        schedule(new MoveOrder(retrAet == null ? this.calledAET : retrAet, moveDestination, 
                prio != null ? prio.intValue() : priority,
                patId, studyIuid, seriesIuid, iuids), 0l);
    }
    
    public void scheduleMoveInstances(DicomObject ian, String moveDestination, Integer prio) throws Exception {
        scheduleMoveInstances(ian, moveDestination, prio, 0);
    }
    
    public void scheduleMoveInstances(DicomObject ian, String moveDestination, Integer prio, long scheduledTime) throws Exception {
        String patId = ian.getString(Tag.PatientID);
        String studyIuid = ian.getString(Tag.StudyInstanceUID);
        HashMap<String, HashMap<String, HashSet<String>>> iuidsMap = getIuidsByRetrAet(ian);
        String retrAet, seriesIuid;
        HashSet<String> iuids;
        for (Map.Entry<String, HashMap<String, HashSet<String>>> seriesByRetr : iuidsMap.entrySet()) {
            retrAet = seriesByRetr.getKey();
            for (Map.Entry<String, HashSet<String>> instBySeries : seriesByRetr.getValue().entrySet() ) {
                seriesIuid = instBySeries.getKey();
                iuids = instBySeries.getValue();
                schedule(new MoveOrder(retrAet,moveDestination, prio != null ? prio.intValue() : priority,
                        patId, studyIuid, seriesIuid, iuids.toArray(new String[iuids.size()])), scheduledTime);
            }
            
        }
    }

    private HashMap<String, HashMap<String, HashSet<String>>> getIuidsByRetrAet(DicomObject ian) {
        HashMap<String, HashMap<String, HashSet<String>>> iuidsByRetrAet = 
            new HashMap<String, HashMap<String, HashSet<String>>>();
        DicomElement refSeriesSeq = ian.get(Tag.ReferencedSeriesSequence);
        DicomObject refSerItem, refSopItem;
        String seriesIuid, sopIuid, retrAet;
        DicomElement refSopSeq;
        HashMap<String, HashSet<String>> seriesMap;
        HashSet<String> instances;
        for ( int i=0, len= refSeriesSeq.countItems() ; i < len ; i++) {
            refSerItem = refSeriesSeq.getDicomObject(i);
            seriesIuid = refSerItem.getString(Tag.SeriesInstanceUID);
            refSopSeq = refSerItem.get(Tag.ReferencedSOPSequence);
            for ( int j=0, len1= refSopSeq.countItems() ; j < len1 ; j++) {
                refSopItem = refSopSeq.getDicomObject(j);
                sopIuid = refSopItem.getString(Tag.ReferencedSOPInstanceUID);
                retrAet = refSopItem.getString(Tag.RetrieveAETitle);
                seriesMap = iuidsByRetrAet.get(retrAet);
                if (seriesMap == null) {
                    seriesMap = new HashMap<String, HashSet<String>>();
                    iuidsByRetrAet.put(retrAet,seriesMap);
                }
                instances = seriesMap.get(seriesIuid);
                if (instances == null) {
                    instances = new HashSet<String>();
                    seriesMap.put(seriesIuid, instances);
                }
                instances.add(sopIuid);
            }
        }
        return iuidsByRetrAet;
    }

    public void schedule(MoveOrder order, long scheduledTime) throws Exception {
        log.info("Schedule order: " + order);            
        jmsDelegate.queue(queueName, order, Message.DEFAULT_PRIORITY, scheduledTime);
    }

    public void onMessage(Message message) {
        ObjectMessage om = (ObjectMessage) message;
        try {
            MoveOrder order = (MoveOrder) om.getObject();
            log.info("Start processing " + order);
            try {
                if (process(order)) {
                    log.info("Finished processing " + order);
                } else {
                    reschedule(order, null);
                }
            } catch (Exception e) {
                reschedule(order, e);
            }
        } catch (JMSException e) {
            log.error("jms error during processing message: " + message, e);
        } catch (Throwable e) {
            log.error("unexpected error during processing message: " + message,
                    e);
        }
    }

    private void reschedule(MoveOrder order, Exception e) throws Exception {
        order.setThrowable(e);
        final int failureCount = order.getFailureCount() + 1;
        order.setFailureCount(failureCount);
        final long delay = getRetryIntervalls(order.getMoveDestination()).getIntervall(failureCount);
        if (delay == -1L) {
            log.error("Give up to process " + order, e);
            jmsDelegate.fail(queueName, order);
        } else {
            log.warn("Failed to process " + order + ". Scheduling retry.", e);
            jmsDelegate.queue(queueName, order, MESSAGE_PRIORITY_MIN, 
                    System.currentTimeMillis() + delay);
        }
    }
    
    private RetryIntervalls getRetryIntervalls(String aet) {
        RetryIntervalls r = retryIntervalls.get(aet);
        if (r == null) r = retryIntervalls.get(null);
        return r != null ? r : new RetryIntervalls();
    }
        
    private boolean process(MoveOrder order) throws IOException, InterruptedException, GeneralSecurityException {
        MoveRspHandler rspHandler = new MoveRspHandler();
        this.move(order.getRetrieveAET(), order.getMoveDestination(), order.getPriority(), 
                order.getPatientId(), order.getStudyIuids(), order.getSeriesIuids(), 
                order.getSopIuids(), rspHandler, true);
        if (rspHandler.status != 0) {
            String[] failedUIDs = rspHandler.getCmd().getStrings(Tag.FailedSOPInstanceUIDList);
            if (failedUIDs != null && failedUIDs.length > 0) {
                order.setSopIuids(failedUIDs);
            }
            return false;
        }
        return true;
    }

    public void configureTransferCapability(String[] cuids, String[] ts) {
        TransferCapability[] tcs = new TransferCapability[cuids.length];
        ExtRetrieveTransferCapability tc;
        for (int i = 0 ; i < cuids.length ; i++) {
            tc = new ExtRetrieveTransferCapability(
                    cuids[i], ts, TransferCapability.SCU);
            tc.setExtInfoBoolean(
                    ExtRetrieveTransferCapability.RELATIONAL_RETRIEVAL, relationQR);
            tcs[i] = tc;
        }    
        setTransferCapability(tcs);
    }
        
    private class MoveRspHandler extends DimseRSPHandler {
        private int status;
        DicomObject cmd;

        public int getStatus() {
            return status;
        }

        public DicomObject getCmd() {
            return cmd;
        }

        @Override
        public void onDimseRSP(Association as, DicomObject cmd,
                DicomObject data) {
            log.info("received C-MOVE-RSP:"+cmd);
            if (!CommandUtils.isPending(cmd)) {
                status = cmd.getInt(Tag.Status);
                this.cmd = cmd;
            }
        }
    }
}

