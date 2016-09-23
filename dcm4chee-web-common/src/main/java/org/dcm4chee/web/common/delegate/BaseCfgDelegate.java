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

package org.dcm4chee.web.common.delegate;

import java.util.ArrayList;
import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.wicket.Application;
import org.apache.wicket.protocol.http.WebApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision$ $Date$
 * @since Aug 5, 2010
 */
public class BaseCfgDelegate {

    protected static BaseCfgDelegate singleton;
    protected ObjectName serviceObjectName;
    protected MBeanServerConnection server;
      
    protected static Logger log = LoggerFactory.getLogger(BaseCfgDelegate.class);
    
    protected BaseCfgDelegate() {
        init();
    }

    public static BaseCfgDelegate getInstance() {
        if (singleton == null)
            singleton = new BaseCfgDelegate();
        return singleton;
    }

    public boolean getManageUsers() {
        return getBoolean("manageUsers", true);
    }

    public String getWebConfigPath() {
        return getString("WebConfigPath");
    }
    
    public int[] getWindowSize(String name) {
        if (server == null) return new int[]{800,600};
        try {
            return (int[]) server.invoke(serviceObjectName, "getWindowSize", 
                    new Object[]{name}, new String[]{String.class.getName()});
        } catch (Exception x) {
            log.warn("Cant invoke getWindowSize, using 800,600 as default!", x);
            return new int[]{800,600};
        }
    }

    public ObjectName getObjectName(String attrName, String defaultName) throws MalformedObjectNameException, NullPointerException {
        if (server == null) return defaultName == null ? null : new ObjectName(defaultName);
        try {
            return (ObjectName) server.getAttribute(serviceObjectName, attrName);
        } catch (Throwable t) {
            log.error("Can't get ObjectName for "+attrName+" ! use default:"+defaultName, t);
            return defaultName == null ? null : new ObjectName(defaultName);
        }
    }

    protected String getString(String attrName) {
        if (server == null) return null;
        try {
            return (String) server.getAttribute(serviceObjectName, attrName);
        } catch (Exception x) {
            log.warn("Cant get "+attrName+"! Ignored by return null!", x);
            return null;
        }
    }
    
    protected boolean getBoolean(String attrName, boolean defVal) {
        if (server == null) return defVal;
        try {
            return (Boolean) server.getAttribute(serviceObjectName, attrName);
        } catch (Exception x) {
            log.warn("Cant get "+attrName+" attribute! return "+defVal+" as default!", x);
            return defVal;
        }
    }

    protected Integer getInteger(String attrName, int defVal) {
        if (server == null) return null;
        try {
            return (Integer) server.getAttribute(serviceObjectName, attrName);
        } catch (Exception x) {
            log.warn("Cant get "+attrName+" attribute! return "+defVal+" as default!", x);
            return null;
        }
    }

    public Object invoke(String opName, Object[] args, Object defVal) {
        try {
            if (args == null) {
                args = new Object[]{};
            }
            String[] argTypes = new String[args.length];
            for (int i = 0 ; i < args.length ; i++) {
                argTypes[i] = args.getClass().getName();
            }
            return server.invoke(serviceObjectName, opName, args, argTypes);
        } catch (Exception x) {
            log.warn("Cant invoke "+opName+"! Return defVal:"+defVal, x);
            return defVal;
        }
    }

    protected void init() {
        log.info("Init " + getClass().getName());
        List<?> servers = MBeanServerFactory.findMBeanServer(null);
        if (servers != null && !servers.isEmpty()) {
            server = (MBeanServerConnection) servers.get(0);
            log.debug("Found MBeanServer:"+server);
        } else {
            log.error("Failed to get MBeanServerConnection! MbeanDelegate class:"+getClass().getName());
            return;
        }
        String s = ((WebApplication)Application.get()).getInitParameter("WebCfgServiceName");
        if (s == null)
            s = "dcm4chee.web:service=WebConfig";
        try {
            serviceObjectName = new ObjectName(s);
            log.info(getClass().getName() + " initialized! WebConfig serviceName: "+serviceObjectName);
        } catch (Exception e) {
            log.error( "Failed to set ObjectName for " + getClass().getName() + "! name:"+s, e);
        }
    }

    public MBeanServerConnection getMBeanServer() {
        return server;
    }
    
    @SuppressWarnings("unchecked")
    protected List<String> getStringList(String name) {
        if (server == null) return new ArrayList<String>();
        try {
            return (List<String>) server.invoke(serviceObjectName, name, new Object[] {}, new String[] {});
        } catch (Exception e) {
            log.warn("Cant invoke '" + name + "', returning empty list.", e);
            return new ArrayList<String>();
        }
    }
    
    @SuppressWarnings("unchecked")
    protected List<Integer> getIntegerList(String name, List<Integer> defValue) {
        if (server != null) {
            try {
                return (List<Integer>) server.invoke(serviceObjectName, name, new Object[] {}, new String[] {});
            } catch (Exception e) {
                log.warn("Cant invoke '" + name + "', returning defValue:"+defValue, e);
            }
        }
        return defValue == null ? new ArrayList<Integer>() : defValue;
    }
}
