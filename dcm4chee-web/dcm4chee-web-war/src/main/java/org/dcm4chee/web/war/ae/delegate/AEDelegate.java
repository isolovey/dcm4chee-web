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

package org.dcm4chee.web.war.ae.delegate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.Attribute;
import javax.management.ObjectName;

import org.dcm4che2.audit.message.AuditEvent;
import org.dcm4chee.archive.entity.AE;
import org.dcm4chee.archive.util.JNDIUtils;
import org.dcm4chee.web.common.delegate.BaseCfgDelegate;
import org.dcm4chee.web.common.delegate.BaseMBeanDelegate;
import org.dcm4chee.web.common.util.Auditlog;
import org.dcm4chee.web.dao.ae.AEHomeLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 * @version $Revision: 16912 $ $Date: 2012-07-31 09:43:31 +0200 (Di, 31 Jul 2012) $
 * @since Aug 18, 2009
 */
public class AEDelegate extends BaseMBeanDelegate {

    private static Logger log = LoggerFactory.getLogger(AEDelegate.class);
    private static AEDelegate singleton;
    
    private static String newline = System.getProperty("line.separator", "\n");
    
    private ObjectName mppsEmulatorServiceObjectName;
    private List<String> mppsEmulatedAETs;
    
    private AEDelegate() {
        super();
        try {
            mppsEmulatorServiceObjectName = BaseCfgDelegate.getInstance().getObjectName("mppsEmulatorServiceName", null);
            log.info("MBeanDelegate initialized! serviceName:"+mppsEmulatorServiceObjectName);
        } catch (Exception e) {
            log.error( "Failed to set ObjectName for MBeanService! serviceNameCfgAttribute:mppsEmulatorServiceName",e );
        }
    }
    
    public static AEDelegate getInstance() {
        if (singleton == null)
            singleton = new AEDelegate();
        return singleton;
    }
    
    public void delete(AE ae) {
        ((AEHomeLocal) JNDIUtils.lookup(AEHomeLocal.JNDI_NAME)).removeAET(ae.getPk());
        Auditlog.logSecurityAlert(AuditEvent.TypeCode.NETWORK_CONFIGURATION, true, "Delete AE:" + ae);
        clearCache();
        notifyAEChange(ae.getTitle(), null);
    }

    public void updateOrCreate(AE ae) {
        if (ae.getPk() != -1) {
            clearCache();
        }
        String updOrCreate = ae.getPk() == -1 ? "Create new AE:" : "Update AE:";
        AE oldAE = ((AEHomeLocal) JNDIUtils.lookup(AEHomeLocal.JNDI_NAME)).updateOrCreateAET(ae);
        updateMPPSEmulateAets(ae.getTitle());
        Auditlog.logSecurityAlert(AuditEvent.TypeCode.NETWORK_CONFIGURATION, true, updOrCreate + ae);
    }

    public void notifyAEChange(String oldAET, String aet) {
        log.debug("Notify AE title change!");
        try {
            server.invoke(serviceObjectName, "notifyAETchange", 
                new Object[]{oldAET, aet, "WEB3 AE change"}, 
                new String[]{String.class.getName(), String.class.getName(), String.class.getName()});
        } catch (Exception x) {
            log.error("Notify AE title change failed!", x);
        }
    }
    
    public void clearCache() {
        log.debug("Clear AE Cache!");
        try {
            server.invoke(serviceObjectName, "clearCache", 
                new Object[]{}, 
                new String[]{});
        } catch (Exception x) {
            log.error("Clear AE Cache failed!", x);
        }
    }

    @Override
    public String getServiceNameCfgAttribute() {
        return "aeServiceName";
    }

    public List<String> getEmulatedAETs() {
        this.mppsEmulatedAETs = new ArrayList<String>();
        try {
            String[] aets =((String) server.getAttribute(mppsEmulatorServiceObjectName, "ModalityAETitles"))
                .split(newline);
            for (String aet : aets) {
                mppsEmulatedAETs.add(aet);
            }
        } catch (Exception e) {
            log.error("Getting mppsEmulatedAETs failed!", e);
        }
        return mppsEmulatedAETs;
    }
    
    @Override
    public String getDefaultServiceObjectName() {
        return "dcm4chee.archive:service=AE";
    }
    
    private void updateMPPSEmulateAets(String aet) {
        try {
            String aetStr = newline+aet+newline;
            String emulateAETs = newline+server.getAttribute(mppsEmulatorServiceObjectName, "ModalityAETitles");
            int pos = emulateAETs.indexOf(aetStr);
            if (pos == -1) {
                if(mppsEmulatedAETs.contains(aet)) {
                    emulateAETs += aet;
                }
            } else {
                if (!mppsEmulatedAETs.contains(aet)) {
                    emulateAETs = emulateAETs.replace(aetStr, newline);
                }
            }
            server.setAttribute(mppsEmulatorServiceObjectName, new Attribute("ModalityAETitles", emulateAETs));
        } catch (Exception e) {
            log.error("Update MPPSEmulate AETs failed! aet:"+aet, e);
        }       
    }
}
