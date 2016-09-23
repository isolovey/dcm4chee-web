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
package org.dcm4chee.web.common.login;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.wicket.Application;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.authentication.panel.SignInPanel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.resource.loader.ClassStringResourceLoader;
import org.apache.wicket.resource.loader.PackageStringResourceLoader;
import org.apache.wicket.security.WaspSession;
import org.apache.wicket.security.authentication.LoginException;
import org.dcm4chee.web.common.base.BaseWicketPage;
import org.dcm4chee.web.common.behaviours.FocusOnLoadBehaviour;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision$ $Date$
 * @since July 20, 2009
 */
public class LoginPage extends BaseWicketPage {

    public LoginPage() {
        super();

        selectorPanel.setShowLogoutLink(false);
       
        String nodeInfo;
        try {
            nodeInfo = System.getProperty("dcm4che.archive.nodename", InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            nodeInfo = "DCM4CHEE";
        }       
        add(new Label("loginLabel", new StringResourceModel("loginLabel", LoginPage.this, 
                null, new Object[]{nodeInfo})));

        add(new SignInPanel("signInPanel") {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean signIn(String username, String password) {
                try {
                  ((WaspSession) getSession()).login(new WebLoginContext(username, password));                 
                } catch (LoginException e) {
                    return false;
                }                
                return true;
            }

            protected void onSignInFailed() {
                Component user = LoginPage.this.get("signInPanel:signInForm:username");
                user.add(FocusOnLoadBehaviour.newFocusAndSelectBehaviour());
            }
        }.add(new AttributeModifier("align", true, new Model<String>("left"))));
        this.get("signInPanel:signInForm").add(new FocusOnLoadBehaviour());        
    }
    
    protected String getBrowserTitle() {

            Class<?> clazz = Application.get().getHomePage();
            String s = new ClassStringResourceLoader(clazz).loadStringResource(null, "application.browser_title");
            if (s == null) 
                s = new PackageStringResourceLoader().loadStringResource(clazz, "application.browser_title", 
                        getSession().getLocale(), null);
            return (s == null ? "DCM4CHEE" : s) + ": " + 
                this.getString("application.login", null, "Login");
    }
}
