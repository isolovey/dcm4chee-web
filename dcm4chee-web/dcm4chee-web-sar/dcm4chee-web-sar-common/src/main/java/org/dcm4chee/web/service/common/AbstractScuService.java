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
package org.dcm4chee.web.service.common;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.concurrent.Executor;

import javax.management.ObjectName;
import javax.naming.InitialContext;

import org.dcm4che2.data.UID;
import org.dcm4che2.net.Association;
import org.dcm4che2.net.Device;
import org.dcm4che2.net.NetworkApplicationEntity;
import org.dcm4che2.net.NetworkConnection;
import org.dcm4che2.net.NewThreadExecutor;
import org.dcm4che2.net.TransferCapability;
import org.dcm4che2.net.pdu.AAssociateRJ;
import org.dcm4chee.archive.entity.AE;
import org.dcm4chee.web.dao.ae.AEHomeLocal;
import org.jboss.system.ServiceMBeanSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author franz.willer@gmail.com
 * @version $Revision$ $Date$
 * @since Feb 11, 2010
 */
public abstract class AbstractScuService extends ServiceMBeanSupport {

    private Device device;
    protected NetworkConnection localConn;
    protected NetworkApplicationEntity localNAE;
    
    private boolean bindToCallingAET;
    
    protected int priority;

    private AEHomeLocal aeHome;
    
    private Executor executor;
    
    private ObjectName tlsCfgServiceName;    
  
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractScuService.class);
    
    protected static final String NONE ="NONE";
    
    public static final String[] NATIVE_LE_TS = {
        UID.ExplicitVRLittleEndian,
        UID.ImplicitVRLittleEndian};
    
    public static final String[] PRIORITIES = {"MEDIUM", "HIGH", "LOW"};
    public static final String LINE_SEPARATOR = System.getProperty("line.separator", "\n");
        
    public AbstractScuService() {
        String scuName = this.getClass().getSimpleName();
        log.info("SCU Devicename:"+scuName);
        executor  = new NewThreadExecutor(scuName);
        device = new Device(scuName);
        localConn = new NetworkConnection();
        localNAE = new NetworkApplicationEntity();
        localNAE.setNetworkConnection(localConn);
        localNAE.setAssociationInitiator(true);
        device.setNetworkApplicationEntity(localNAE);
        device.setNetworkConnection(localConn);
    }

    public boolean isBindToCallingAET() {
        return bindToCallingAET;
    }

    public void setBindToCallingAET(boolean bindToCallingAET) {
        this.bindToCallingAET = bindToCallingAET;
    }
    
    public int getMaxPDULengthReceive() {
        return localNAE.getMaxPDULengthReceive();
    }

    public void setMaxPDULengthReceive(int maxPDULength) {
        localNAE.setMaxPDULengthReceive(maxPDULength);
    }

    public int getMaxOpsInvoked() {
        return localNAE.getMaxOpsInvoked();
    }

    public void setMaxOpsInvoked(int maxOpsInvoked) {
        localNAE.setMaxOpsInvoked(maxOpsInvoked);
    }

    public int getDimseRspTimeout() {
        return localNAE.getDimseRspTimeout();
    }

    public void setDimseRspTimeout(int retrieveRspTimeout) {
        localNAE.setDimseRspTimeout(retrieveRspTimeout);
    }
    public int getRetrieveRspTimeout() {
        return localNAE.getRetrieveRspTimeout();
    }

    public void setRetrieveRspTimeout(int retrieveRspTimeout) {
        localNAE.setRetrieveRspTimeout(retrieveRspTimeout);
    }

    public boolean isPackPDV() {
        return localNAE.isPackPDV();
    }

    public void setPackPDV(boolean packPDV) {
        localNAE.setPackPDV(packPDV);
    }

    public int getAcceptTimeout() {
        return localConn.getAcceptTimeout();
    }

    public void setAcceptTimeout(int timeout) {
        localConn.setAcceptTimeout(timeout);
    }

    public int getConnectTimeout() {
        return localConn.getConnectTimeout();
    }

    public void setConnectTimeout(int timeout) {
        localConn.setConnectTimeout(timeout);
    }

    public int getReleaseTimeout() {
        return localConn.getReleaseTimeout();
    }

    public void setReleaseTimeout(int timeout) {
        localConn.setReleaseTimeout(timeout);
    }

    public int getRequestTimeout() {
        return localConn.getRequestTimeout();
    }

    public void setRequestTimeout(int timeout) {
        localConn.setRequestTimeout(timeout);
    }
    
    public int getSocketCloseDelay() {
        return localConn.getSocketCloseDelay();
    }

    public void setSocketCloseDelay(int timeout) {
        localConn.setSocketCloseDelay(timeout);
    }

    public boolean isTcpNoDelay() {
        return localConn.isTcpNoDelay();
    }

    public void setTcpNoDelay(boolean tcpNoDelay) {
        localConn.setTcpNoDelay(tcpNoDelay);
    }

    public String getCallingAET() {
        return localNAE.getAETitle();
    }

    public void setCallingAET(String callingAET) {
        localNAE.setAETitle(callingAET);
        device.setDeviceName(callingAET);
    }

    public String getPriority() {
        return PRIORITIES[priority];
    }

    public void setPriority(String priorityName) {
        this.priority = PRIORITIES[1].equals(priorityName) ? 1 : PRIORITIES[2].equals(priorityName) ? 2 : 0;
    }
        
    public ObjectName getTlsCfgServiceName() {
        return tlsCfgServiceName;
    }

    public void setTlsCfgServiceName(ObjectName name) {
        this.tlsCfgServiceName = name;
    }
    
    public Association open(String aet) throws IOException, GeneralSecurityException {
        AE ae;
        try {
            ae = lookupAEHome().findByTitle(aet);
        } catch (Exception x) {
            throw new RuntimeException("AET not found:"+aet);
        }
        return open(ae);
    }
    
    public Association open(AE ae) throws IOException, GeneralSecurityException {
        NetworkApplicationEntity remoteAE = new NetworkApplicationEntity();
        NetworkConnection remoteConn = new NetworkConnection();
   
        remoteConn.setHostname(ae.getHostName());
        remoteConn.setPort(ae.getPort());
        remoteAE.setAETitle(ae.getTitle());
        remoteAE.setInstalled(true);
        remoteAE.setAssociationAcceptor(true);
        remoteAE.setNetworkConnection(remoteConn);
   
        List<String> ciphers = ae.getCipherSuites();
        LOG.info("Open association to {} url:{} Ciphers:{}", new Object[]{ae.getTitle(), ae, ciphers});
        if (ciphers.size() > 0) {
            String[] ciphers1 = (String[]) ciphers.toArray(new String[ciphers.size()]);
            try {
                server.invoke(tlsCfgServiceName, "initTLS", 
                        new Object[]{remoteConn, device, ciphers1}, 
                        new String[]{NetworkConnection.class.getName(), 
                        Device.class.getName(), ciphers1.getClass().getName()});
            } catch (Exception e) {
                log.error("Failed to initialize TLS! AE:"+ae+" ciphers:"+ciphers, e);
                throw new IOException("Failed to initialize TLS aet:"+ae.getTitle());
            }
        } else {
            remoteConn.setTlsCipherSuite(new String[0]);
            localConn.setTlsCipherSuite(new String[0]);
        }
        if (bindToCallingAET) {
            try {
                AE callingAE = this.lookupAEHome().findByTitle(localNAE.getAETitle());
                log.info("Try to bind socket to callingAE:"+callingAE+" hostname:"+callingAE.getHostName());
                localConn.setHostname(callingAE.getHostName());
                log.info("Socket bound to "+callingAE.getHostName());
            } catch (Exception x) {
                log.warn("Socket can not be bound to IP of calling AET!", x);
            }
        }
        try {
            return localNAE.connect(remoteAE, executor, true);
        } catch (AAssociateRJ t) {
            throw t;
        } catch (Throwable t) {
            log.error("localNAE.connect failed!",t);
            throw new IOException("Failed to establish Association aet:"+ae.getTitle());
        }
    }
    
    public TransferCapability[] getTransferCapability() {
        return localNAE.getTransferCapability();        
    }
    public void setTransferCapability(TransferCapability[] tc) {
        localNAE.setTransferCapability(tc);        
    }
    
    public TransferCapability selectTransferCapability(Association assoc, String[] cuid) {
        TransferCapability tc;
        for (int i = 0; i < cuid.length; i++) {
            tc = assoc.getTransferCapabilityAsSCU(cuid[i]);
            if (tc != null)
                return tc;
        }
        return null;
    }

    public AEHomeLocal lookupAEHome() {
        if ( aeHome == null ) {
            try {
                InitialContext jndiCtx = new InitialContext();
                aeHome = (AEHomeLocal) jndiCtx.lookup(AEHomeLocal.JNDI_NAME);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return aeHome;
    }
}

