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
package org.dcm4chee.web.war;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

import org.apache.wicket.protocol.http.MockServletContext;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.security.actions.Actions;
import org.apache.wicket.security.hive.HiveMind;
import org.apache.wicket.util.tester.WicketTester;
import org.jboss.system.server.ServerConfigLocator;

public class WASPTestUtil {
    
    private static String actionKey;
    private static String hiveKey;
    
    private static final String ROLE_MAPPING_FILENAME = "roles.json";

    public static WicketTester getWicketTester(WebApplication testApplicaton) throws URISyntaxException {
        if (actionKey != null) {
            HiveMind.unregisterHive(hiveKey);
            Actions.unregisterActionFactory(actionKey);
            actionKey = hiveKey = null;
        }
        String contextPath = new java.io.File(WASPTestUtil.class.getResource("WASPTestUtil.class").toURI()).getParent();
        WicketTester wicketTester = new WicketTester(testApplicaton, contextPath);
        if (actionKey == null) {
            hiveKey = "hive_"+testApplicaton.getName();
            actionKey = testApplicaton.getClass().getName() + ":" + hiveKey;
        }
        MockServletContext ctx =(MockServletContext)wicketTester.getApplication().getServletContext();

        ctx.addInitParameter("securityDomainName", "dcm4chee");
        ctx.addInitParameter("rolesGroupName", "Roles");
        URL url = WASPTestUtil.class.getResource("/wicket.login.file");
        //Set login configuration file! '=' means: overwrite other login configuration given in Java security properties file.
        System.setProperty("java.security.auth.login.config", "="+url.getPath());
        return wicketTester;
    }
    
    public static void initRolesMappingFile() throws IOException {
        String webConfigPath = ServerConfigLocator.locate().getServerHomeDir().getAbsolutePath() + "/conf/";
        System.setProperty("dcm4chee-web3.cfg.path", webConfigPath);
        File f = new File(ROLE_MAPPING_FILENAME);
        if (!f.isAbsolute())
            f = new File(webConfigPath, f.getPath());
        if (f.exists()) return;
        f.getParentFile().mkdirs();
        FileChannel fos = null;
        InputStream is = null;
        try {
            URL url = WASPTestUtil.class.getResource("/roles-test.json");
            is = url.openStream();
            ReadableByteChannel inCh = Channels.newChannel(is);
            fos = new FileOutputStream(f).getChannel();
            int pos = 0;
            while (is.available() > 0)
                pos += fos.transferFrom(inCh, pos, is.available());
        } finally {
            try {
                if (is != null) is.close();
            } catch (Exception ignore){}
            try {
                if (fos != null) fos.close();
            } catch (Exception ignore){}
        }
    }
}
