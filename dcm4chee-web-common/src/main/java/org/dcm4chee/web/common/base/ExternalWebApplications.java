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

package org.dcm4chee.web.common.base;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.apache.wicket.model.Model;
import org.dcm4che2.util.StringUtils;
import org.dcm4chee.web.common.delegate.BaseCfgDelegate;
import org.dcm4chee.web.common.login.LoginContextSecurityHelper;
import org.dcm4chee.web.common.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 * @version $Revision$ $Date$
 * @since May 26, 2011
 */

public class ExternalWebApplications implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final String CFG_FILE_NAME = "external_webapps.cfg";
    
    private List<String> jaasRoles;
    private List<ExternalWebApp> applications = new ArrayList<ExternalWebApp>();
    
    private static Logger log = LoggerFactory.getLogger(ExternalWebApplications.class);
   
    public ExternalWebApplications() {
        jaasRoles = LoginContextSecurityHelper.getJaasRoles();
    }
    
    public List<ExternalWebApp> getExternalWebAppPanels() {
        File cfgPath = FileUtils.resolve(new File(BaseCfgDelegate.getInstance().getWebConfigPath()));
        File cfgFile = new File(cfgPath, CFG_FILE_NAME);
        if (cfgFile.isFile()) {
            BufferedReader br = null;
            try {
                br =new BufferedReader(new FileReader(cfgFile)); 
                String line;
                String appTitle, grpTitle,url;
                int pos1, pos2, height;
                HashMap<String, ExternalWebAppGroupPanel> grpPanels = new HashMap<String, ExternalWebAppGroupPanel>();
                Model<String> titleModel;
                while ((line = br.readLine()) != null) {
                    if (line.length() == 0)
                        continue;
                    if (line.charAt(0) != '#') {
                        pos1 = line.indexOf('=');
                        if (pos1 == -1) {
                            log.warn(CFG_FILE_NAME+": Wrong formatted line ignored! Reason: '=' missing! line:"+line);
                            continue;
                        }
                        appTitle = line.substring(0, pos1++);
                        pos2 = line.indexOf('|', pos1);
                        if (pos2 == -1) {
                            log.warn(CFG_FILE_NAME+": Wrong formatted line ignored! Reason: '|' missing! line:"+line);
                            continue;
                        }
                        grpTitle = line.substring(pos1,pos2++);
                        pos1 = pos2;
                        pos2 = line.indexOf('|', pos1);
                        if (pos2 == -1) {
                            log.warn(CFG_FILE_NAME+": Wrong formatted line ignored! Reason: second '|' missing! line:"+line);
                            continue;
                        }
                        if (!hasRole(line.substring(pos1, pos2++)))
                                continue;
                        pos1 = pos2;
                        pos2 = line.indexOf('|', pos1);
                        if (pos2 == -1) {
                            log.warn(CFG_FILE_NAME+": Wrong formatted line ignored! Reason: third '|' missing! line:"+line);
                            continue;
                        }
                        try {
                            height = Integer.parseInt(line.substring(pos1, pos2++));
                        } catch (Exception x) {
                            log.warn(CFG_FILE_NAME+": Wrong formatted line ignored! Reason: height not an integer! line:"+line);
                            continue;
                        }
                        url = line.substring(pos2);
                        titleModel = new Model<String>(appTitle);
                        if (grpTitle.length() < 1) {
                            this.applications.add(new ExternalWebAppPanel(TabbedPanel.TAB_PANEL_ID, url, 
                                    titleModel, height));
                        } else {
                            ExternalWebAppGroupPanel grpPanel = grpPanels.get(grpTitle);
                            if (grpPanel == null) {
                                grpPanel = new ExternalWebAppGroupPanel(TabbedPanel.TAB_PANEL_ID,
                                                new Model<String>(grpTitle));
                                grpPanels.put(grpTitle, grpPanel);
                                this.applications.add(grpPanel);
                            }
                            grpPanel.addModule(new ExternalWebAppPanel(TabbedPanel.TAB_PANEL_ID, url, 
                                    titleModel, height), titleModel);
                        }
                    }
                }
            } catch (Exception x) {
                log.error("Failed to read config file:"+cfgFile, x);
            }
        }
        log.debug("ExternalWebAppPanels:{}",applications);
        return applications;
    }
    
    private boolean hasRole(String rolesStr) {
        String[] roles = StringUtils.split(rolesStr, ',');
        for (int i = 0 ; i < roles.length ; i++) {
            if (jaasRoles.contains(roles[i]))
                return true;
        }
        return false;
    }
}
