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

package org.dcm4chee.web.war;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.jar.Manifest;

import javax.servlet.http.HttpSession;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.util.time.Duration;
import org.dcm4chee.dashboard.ui.DashboardPanel;
import org.dcm4chee.usr.ui.usermanagement.ChangePasswordPanel;
import org.dcm4chee.usr.ui.usermanagement.role.RolePanel;
import org.dcm4chee.usr.ui.usermanagement.user.UserListPanel;
import org.dcm4chee.web.common.base.BaseWicketApplication;
import org.dcm4chee.web.common.base.ExternalWebApp;
import org.dcm4chee.web.common.base.ExternalWebApplications;
import org.dcm4chee.web.common.base.ModuleSelectorPanel;
import org.dcm4chee.web.common.license.ae.AELicenseProviderManager;
import org.dcm4chee.web.common.license.ae.spi.AELicenseProviderSPI;
import org.dcm4chee.web.common.secure.SecureSession;
import org.dcm4chee.web.common.secure.SecureSessionCheckPage;
import org.dcm4chee.web.common.secure.SecureWicketPage;
import org.dcm4chee.web.common.util.FileUtils;
import org.dcm4chee.web.war.ae.AEPanel;
import org.dcm4chee.web.war.folder.StudyListPage;
import org.dcm4chee.web.war.tc.TCEnvironment;
import org.dcm4chee.web.war.tc.TCPanel;
import org.dcm4chee.web.war.trash.TrashListPage;
import org.dcm4chee.web.war.worklist.modality.ModalityWorklistPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision: 18466 $ $Date: 2015-06-22 12:46:49 +0200 (Mo, 22 Jun 2015) $
 * @since July 7, 2009
 */
public class MainPage extends SecureWicketPage {
    
    protected static Logger log = LoggerFactory.getLogger(MainPage.class);

    private static String tceSystemPropertyKey = null;

    public MainPage() {
        super();
        if (StudyPermissionHelper.get().isSSO())
            add (new AbstractAjaxTimerBehavior(Duration.milliseconds(1)) {
    
                private static final long serialVersionUID = 1L;
    
                @Override
                protected void onTimer(AjaxRequestTarget arg0) {
                    try {
                        StudyPermissionHelper.get().doDicomAuthentication();
                    } catch (Exception e) {
                        log.error(getClass().getName() + ": error doing dicom authentication: ", e);
                    } finally {
                        this.stop();
                    }
                }
            });
        HttpSession session = ((WebRequest)getRequest()).getHttpServletRequest().getSession();
        add (new AbstractAjaxTimerBehavior(Duration.milliseconds((session.getMaxInactiveInterval()+1)*1000)) {
            
            private static final long serialVersionUID = 1L;
            private long lastTime = 0l;
            @Override
            protected void onTimer(AjaxRequestTarget arg0) {
                HttpSession session = ((WebRequest)getRequest()).getHttpServletRequest().getSession();
                long now = System.currentTimeMillis();
                if (log.isDebugEnabled()) {
                    log.debug("############### Session Timeout checker!");
                    log.debug("####### session getMaxInactiveInterval:"+session.getMaxInactiveInterval());
                    log.debug("####### session getLastAccessedTime:"+session.getLastAccessedTime());
                    log.debug("####### session currentTime:"+now);
                    log.debug("####### session currentTime-lastAccessedTime:"+(now-session.getLastAccessedTime()));
                    log.debug("####### session lastTime:"+lastTime);
                    log.debug("####### session LastAccessedTime-lastTime:"+(session.getLastAccessedTime()-lastTime));
                }
                if ( session.getLastAccessedTime() < lastTime) {
                    session.invalidate();
                    this.setUpdateInterval(Duration.milliseconds(1000));
                } else {
                    long wait = (session.getMaxInactiveInterval()+1) * 1000 - 
                    now + session.getLastAccessedTime();
                    lastTime = now+2000;
                    this.setUpdateInterval(Duration.milliseconds(wait));
                }
            }
        });
        addModules(getModuleSelectorPanel());
       
    }

    private void addModules(ModuleSelectorPanel selectorPanel) {
        
        AELicenseProviderSPI provider = AELicenseProviderManager.get(null).getProvider();
        List<ExternalWebApp> extApps = new ExternalWebApplications().getExternalWebAppPanels();
        
        selectorPanel.addModule(StudyListPage.class);
        selectorPanel.addModule(TrashListPage.class);
        selectorPanel.addModule(AEPanel.class);
        selectorPanel.addModule(ModalityWorklistPanel.class);
        if (provider.allowFeature("Teachingfiles")) {
        	TCEnvironment.init(extApps);
        	selectorPanel.addModule(TCPanel.class);
        }
        if (provider.allowFeature("Dashboard"))
            selectorPanel.addModule(DashboardPanel.class);
        selectorPanel.addModule(RolePanel.class, null);

        if (((SecureSession) RequestCycle.get().getSession()).getManageUsers()) {
            selectorPanel.addModule(UserListPanel.class, null);
            selectorPanel.addModule(ChangePasswordPanel.class, null);
        }
        for (ExternalWebApp p : extApps) {
            selectorPanel.addModule(p.getPanel(), p.getTitle());
        }
        selectorPanel.getAboutWindow().setPageCreator(new ModalWindow.PageCreator() {

            private static final long serialVersionUID = 1L;

            public Page createPage() {
                return new AboutPage();
            }
        });

        try {
            Properties properties = new Properties();
            properties.load(((BaseWicketApplication) getApplication()).getServletContext().getResourceAsStream("/META-INF/MANIFEST.MF"));
            selectorPanel.get("aboutLink:img_logo").add(new AttributeModifier("title", true, 
                    new Model<String>(
                            properties.getProperty("Implementation-Title", "")
                            + " : " + properties.getProperty("Implementation-Build", "")
                            + " (" + properties.getProperty("SCM-Revision", "?")+")"+
                            getPatchVersion()
                            )));            
        } catch (Exception ignore) {}
    }   
    
    private String getPatchVersion() {
    	Properties p = getPatchInfoProperties();
    	return p == null ? "" : " Patch version: "+p.getProperty("patch_version", "???");
    }
	private Properties getPatchInfoProperties() {
        File patchInfoFile = FileUtils.resolve(new File("patch-info/version"));
        if (patchInfoFile.isFile()) {
        	FileInputStream in = null;
        	Properties p = new Properties();
        	try {
        		in = new FileInputStream(patchInfoFile);
				p.load(new FileInputStream(patchInfoFile));
			} catch (IOException e) {
				p.setProperty("version", "PATCH INFO UNKNOWN! (can not read "+patchInfoFile.getAbsolutePath()+")");
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException ignore) {}
				}
			}
        	return p;
        }
		return null;
	}

    private class AboutPage extends SecureSessionCheckPage {

        private static final long serialVersionUID = 1L;

        public AboutPage() {

            Properties webProperties = new Properties();
            Manifest pacsManifest = null;
            Manifest dcmManifest = null;
            try {
                webProperties.load(((BaseWicketApplication) getApplication()).getServletContext().getResourceAsStream("/META-INF/MANIFEST.MF"));
            } catch (IOException e) {
                log.warn("Could not retrieve properties from /META-INF/MANIFEST.MF for web application");
                log.debug("StackTrace:", e);
            }
            try {
                 dcmManifest = new java.util.jar.JarFile(new java.io.File(
                		Class.forName("org.dcm4che.data.Dataset").getProtectionDomain().getCodeSource().getLocation().toExternalForm().substring(6))).getManifest();
            } catch (Exception e) {
                log.warn("Could not retrieve properties from /META-INF/MANIFEST.MF for DICOM library");
                log.debug("StackTrace:", e);
            }
            try {
                pacsManifest = new java.util.jar.JarFile(new java.io.File(
                		Class.forName("org.dcm4chex.archive.dcm.DcmServerService").getProtectionDomain()
                		.getCodeSource().getLocation().toExternalForm().substring(6))).getManifest();
            } catch (Exception e) {
                log.warn("Could not retrieve properties from /META-INF/MANIFEST.MF for archive application");
                log.debug("StackTrace:", e);
            }
           
            add(new Label("content", new StringResourceModel(isTCE() ? "template_tce" : "template", 
            		this, null, new Object[] {
            		webProperties.getProperty("Implementation-Title",""),
            		webProperties.getProperty("Implementation-Build", ""),
            		webProperties.getProperty("SCM-Revision", ""),
            		getManifestValue(pacsManifest, "Implementation-Title", "-"),
            		getManifestValue(pacsManifest, "Implementation-Version", "-"),
            		getManifestValue(pacsManifest, "Implementation-Vendor", "-"),
            		getManifestValue(dcmManifest, "Implementation-Title", "-"),
            		getManifestValue(dcmManifest, "Implementation-Version", "-"),
            		getManifestValue(dcmManifest, "Implementation-Vendor", "-"),
            		getImplementationBuild(webProperties),
            		getPatchInfo()
            })).setEscapeModelStrings(false));
        }

		private String getImplementationBuild(Properties webProperties) {
			String s = webProperties.getProperty("Implementation-Build", "2.18.x");
            char c;
            for (int pos = 4, len = s.length() ; pos < len ; pos++) {
            	c = s.charAt(pos);
            	if (!Character.isLetterOrDigit(c) && c != '.') {
            		return s.substring(0, pos);
            	}
            }
            return s;
		}
        
    	private String getPatchInfo() {
    		Properties p = getPatchInfoProperties();
            return p == null ? "" : new StringResourceModel("patch_template", this, null, new Object[] {
    							p.getProperty("patch_version", "???"),
    							p.getProperty("patch_scm-revision", "???"),
    							p.getProperty("patch_components", "???"),	
    							p.getProperty("patch_artifacts", "???")	
    						}).getString();
    	}

    	private String getManifestValue(Manifest m, String attrName, String defValue) {
        	return m == null ? defValue : m.getMainAttributes().getValue(attrName);
        }
    	
    	private boolean isTCE() {
    		if (tceSystemPropertyKey == null) {
	    		for (String key : System.getProperties().stringPropertyNames()) {
	    			if (key.endsWith("web.login.tce")) {
	    				tceSystemPropertyKey = key;
	    				return Boolean.getBoolean(tceSystemPropertyKey);
	    			}
	    		}
	    		return false;
    		}
    		return Boolean.getBoolean(tceSystemPropertyKey);
    	}
    }

}
