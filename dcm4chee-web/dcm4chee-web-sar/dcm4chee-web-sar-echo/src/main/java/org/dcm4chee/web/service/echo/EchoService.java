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
package org.dcm4chee.web.service.echo;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4che2.net.Association;
import org.dcm4che2.net.DimseRSP;
import org.dcm4che2.net.ExtRetrieveTransferCapability;
import org.dcm4che2.net.TransferCapability;
import org.dcm4chee.archive.entity.AE;
import org.dcm4chee.web.service.common.AbstractScuService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author franz.willer@gmail.com
 * @version $Revision$ $Date$
 * @since Jul 29, 2009
 */
public class EchoService extends AbstractScuService {
    
    private static Logger log = LoggerFactory.getLogger(EchoService.class);
    
    private int pingTimeout;
    
    public EchoService() {
        super();
        configureTransferCapability();
    }

    public void configureTransferCapability() {
        TransferCapability[] tcs = new TransferCapability[]{
                new ExtRetrieveTransferCapability(UID.VerificationSOPClass, NATIVE_LE_TS, TransferCapability.SCU)};
        setTransferCapability(tcs);
    }
    
    public int getPingTimeout() {
        return pingTimeout;
    }

    public void setPingTimeout(int pingTimeout) {
        this.pingTimeout = pingTimeout;
    }

    /**
     * Perform a DICOM Echo to given Application Entity Title.
     */
    public boolean echo(String title) {
        Association assoc = null;
        int status = 0;
        try {
            assoc = open(title);
        } catch (Throwable t) {
            log.error("Failed to establish Association aet:"+title, t);
            return false;
        }
        try {
            DimseRSP rsp = assoc.cecho();
            rsp.next();
            DicomObject cmd = rsp.getCommand();
            status = cmd.getInt(Tag.Status);
        } catch (Throwable t) {
            log.error("Echo failed! aet:"+title, t);
            return false;
        }
        try {
            assoc.release(true);
        } catch (InterruptedException t) {
            log.error("Association release failed! aet:"+title, t);
        }
        return  status == 0;
    }

    public String echo(AE ae, int nrOfTests) {
        Association assoc = null;
        StringBuilder echoResult = new StringBuilder();
        try {
            echoResult.append("DICOM Echo to ").append(ae).append(":\n");
            long tStart = System.currentTimeMillis();
            InetAddress.getByName(ae.getHostName());
            assoc = open(ae);
            try {
                long t1 = System.currentTimeMillis();
                echoResult.append("Open Association in ").append(t1-tStart).append(" ms.\n");
                long t0, diff;
                int nrOfLT1ms = 0;
                for (int i = 0; i < nrOfTests; i++) {
                    t0 = System.currentTimeMillis();
                    DimseRSP rsp = assoc.cecho();
                    t1 = System.currentTimeMillis();
                    diff = t1 - t0;
                    rsp.next();
                    DicomObject cmd = rsp.getCommand();
                    int status = cmd.getInt(Tag.Status);
                    if (status != 0) {
                        echoResult.append("Echo failed with status ").append(Integer.toHexString(status))
                        .append("H! Error Comment:").append(cmd.getString(Tag.ErrorComment))
                        .append(" (").append(diff).append("ms)");
                        break;
                    }
                    if (diff < 1) {
                        nrOfLT1ms++;
                    } else {
                        if (nrOfLT1ms > 0) {
                            echoResult.append(nrOfLT1ms).append(" Echoes, each done in less than 1 ms!\n");
                            nrOfLT1ms = 0;
                        }
                        echoResult.append("Echo done in ").append(diff).append(" ms!\n");
                    }
                }
                if (nrOfLT1ms > 0)
                    echoResult.append(nrOfLT1ms).append(" Echoes, each done in less than 1 ms!\n");
                echoResult.append("Total time for successfully echo ").append(ae.getTitle());
                if ( nrOfTests > 1 ) {
                    echoResult.append(' ').append(nrOfTests).append(" times");
                }
                echoResult.append(": ").append(System.currentTimeMillis()-tStart).append(" ms!");
            } finally {
                try {
                    assoc.release(true);
                } catch (Exception e) {
                    log.warn("Failed to release Association AE:" + assoc.getCalledAET());
                }
            }
        } catch (UnknownHostException x) {
            echoResult.append("Echo failed! Reason: unknown host:").append(x.getMessage());
        } catch (Throwable t) {
            log.error("Echo " + ae + " failed", t);
            echoResult.append("Echo failed! Reason: ").append(t.getMessage());
        }
        return echoResult.toString();
    }
    
    public boolean ping(String host) throws UnknownHostException, IOException{
         return InetAddress.getByName(host).isReachable(pingTimeout);
    }
    
 }

