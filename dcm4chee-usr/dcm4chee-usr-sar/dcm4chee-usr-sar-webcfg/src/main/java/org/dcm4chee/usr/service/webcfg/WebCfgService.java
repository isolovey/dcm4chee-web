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

package org.dcm4chee.usr.service.webcfg;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.jboss.system.ServiceMBeanSupport;
import org.jboss.system.server.ServerConfigLocator;

/**
 * @author franz.willer@gmail.com
 * @author robert.david@agfa.com
 * @version $Revision$ $Date$
 * @since July 26, 2010
 */
public class WebCfgService extends ServiceMBeanSupport {

    public WebCfgService() {
    }

    public String getUserMgtUserRole() {
        return System.getProperty("dcm4chee-usr.cfg.userrole", NONE);
    }

    public void setUserMgtUserRole(String name) {
        if (NONE.equals(name)) {
            System.getProperties().remove("dcm4chee-usr.cfg.userrole");
        } else {
            System.setProperty("dcm4chee-usr.cfg.userrole", name);
        }
    }
    
    public String getUserMgtAdminRole() {
        return System.getProperty("dcm4chee-usr.cfg.adminrole", NONE);
    }

    public void setUserMgtAdminRole(String name) {
        if (NONE.equals(name)) {
            System.getProperties().remove("dcm4chee-usr.cfg.adminrole");
        } else {
            System.setProperty("dcm4chee-usr.cfg.adminrole", name);
        }
    }

    public String getGroupsFilename() {
        return System.getProperty("dcm4chee-usr.cfg.groups-filename", NONE);
    }

    public void setGroupsFilename(String name) {
        if (NONE.equals(name)) 
            System.getProperties().remove("dcm4chee-usr.cfg.groups-filename");
        else 
            System.setProperty("dcm4chee-usr.cfg.groups-filename", name);
    }

    protected static final long serialVersionUID = 1L;

    protected boolean manageUsers;
    protected String webConfigPath;
    
    protected Map<String,int[]> windowsizeMap = new LinkedHashMap<String, int[]>();
    
    protected static final String NONE = "NONE";
    protected final String NEWLINE = System.getProperty("line.separator", "\n");
    
    public void setManageUsers(boolean manageUsers) {
        this.manageUsers = manageUsers;
    }

    public boolean isManageUsers() {
        return manageUsers;
    }

    public String getWebConfigPath() {
        return System.getProperty("dcm4chee-web3.cfg.path", NONE);
    }

    public void setWebConfigPath(String webConfigPath) {
        if (NONE.equals(webConfigPath)) {
            System.getProperties().remove("dcm4chee-web3.cfg.path");
        } else {
            String old = System.getProperty("dcm4chee-web3.cfg.path");
            if (!webConfigPath.endsWith("/")) webConfigPath += "/";
            System.setProperty("dcm4chee-web3.cfg.path", webConfigPath);
            if (old == null) {
                initDefaultRolesFile();
            }
        }
    }
    
    protected void initDefaultRolesFile() {
        String webConfigPath = System.getProperty("dcm4chee-web3.cfg.path", "conf/dcm4chee-web3");
        File mappingFile = new File(webConfigPath + "roles.json");
        if (!mappingFile.isAbsolute())
            mappingFile = new File(ServerConfigLocator.locate().getServerHomeDir(), mappingFile.getPath());
        if (mappingFile.exists()) return;
        log.info("Init default Role Mapping file! mappingFile:"+mappingFile);
        if (mappingFile.getParentFile().mkdirs())
            log.info("M-WRITE dir:" +mappingFile.getParent());
        FileChannel fos = null;
        InputStream is = null;
        try {
            URL url = getClass().getResource("/META-INF/roles-default.json");
            log.info("Use default Mapping File content of url:"+url);
            is = url.openStream();
            ReadableByteChannel inCh = Channels.newChannel(is);
            fos = new FileOutputStream(mappingFile).getChannel();
            int pos = 0;
            while (is.available() > 0)
                pos += fos.transferFrom(inCh, pos, is.available());
        } catch (Exception e) {
            log.error("Roles file doesn't exist and the default can't be created!", e);
        } finally {
            close(is);
            close(fos);
        }
    }
    
    protected void close(Closeable toClose) {
        if (toClose != null) {
            try {
                toClose.close();
            } catch (IOException ignore) {
                log.debug("Error closing : "+toClose.getClass().getName(), ignore);
            }
        }
    }

    public String getWindowSizeConfig() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, int[]> e : windowsizeMap.entrySet()) {
            sb.append(e.getKey()).append(':').
            append(e.getValue()[0]).append('x').append(e.getValue()[1]).
            append(NEWLINE);
        }
        return sb.toString();
    }

    public void setWindowSizeConfig(String s) {
        windowsizeMap.clear();
        StringTokenizer st = new StringTokenizer(s, " \t\r\n;");
        String t;
        int pos;
        while (st.hasMoreTokens()) {
            t = st.nextToken();
            if ((pos = t.indexOf(':')) == -1) {
                throw new IllegalArgumentException("Format must be:<name>:<width>x<height>! "+t);
            } else {
                windowsizeMap.put(t.substring(0, pos), parseSize(t.substring(++pos)));
            }
        }
    }
    
    public int[] getWindowSize(String name) {
        int[] size = windowsizeMap.get(name);
        if (size==null) 
            size = windowsizeMap.get("default");
        if (size==null) {
            log.warn("No default window size is configured! use 800x600 as default!");
            return new int[]{800,600};
        }
        return size;
    }

    protected int[] parseSize(String s) {
        int pos = s.indexOf('x');
        if (pos == -1)
            throw new IllegalArgumentException("Windowsize must be <width>x<height>! "+s);
        return new int[]{Integer.parseInt(s.substring(0,pos).trim()), 
                Integer.parseInt(s.substring(++pos).trim())};
    }
}
