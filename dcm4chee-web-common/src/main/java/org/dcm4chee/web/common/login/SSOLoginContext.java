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

import javax.security.auth.Subject;

import org.apache.wicket.RequestCycle;
import org.apache.wicket.security.authentication.LoginException;
import org.apache.wicket.security.hive.authentication.DefaultSubject;
import org.apache.wicket.security.hive.authentication.LoginContext;
import org.dcm4chee.web.common.delegate.BaseCfgDelegate;
import org.dcm4chee.web.common.delegate.BaseMBeanDelegate;
import org.dcm4chee.web.common.secure.SecureSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Robert David <robert.david@agfa.com>
 * @author Franz Willer <franz.willer@gmail.com>
 * @version $Revision$ $Date$
 * @since Aug. 31, 2010
 */
public class SSOLoginContext extends LoginContext {

    protected static Logger log = LoggerFactory.getLogger(BaseMBeanDelegate.class);

    SecureSession session;
    private Subject jaasSubject;
    
    public SSOLoginContext() {
        
    }
    public SSOLoginContext(SecureSession secureSession, Subject jaasSubject) {
        this.session = secureSession;
        this.jaasSubject = jaasSubject;
    }

    @Override
    public final org.apache.wicket.security.hive.authentication.Subject login() throws LoginException
    {
            if (jaasSubject == null)
                    throw new LoginException("Insufficient information to login");
            return getSubject(jaasSubject);
    }
    
    protected org.apache.wicket.security.hive.authentication.Subject getSubject(Subject jaasSubject) throws LoginException {

        if (session == null) {
            try {
                session = ((SecureSession) RequestCycle.get().getSession());
            } catch (Exception e) {
                log.warn("SSO Login failed. Reason: " + e.getMessage());
                throw new LoginException();
            }
        }
        session.setManageUsers(BaseCfgDelegate.getInstance().getManageUsers());
        if (!readHiveFile())
            return null;

        DefaultSubject subject;
        try {
            subject = LoginContextSecurityHelper.mapSwarmSubject(jaasSubject, session);
            if (!LoginContextSecurityHelper.checkLoginAllowed(subject)) {
                session.invalidate();
                RequestCycle.get().getResponse().redirect("");
                log.warn("Failed to authorize subject for login, denied. See 'LoginAllowedRolename' parameter in web.xml.");
            } else
                session.extendedLogin(subject);
        } catch (Exception e) {
            log.error("Login failed for JAAS subject: "+jaasSubject, e);
            session.invalidate();
            return new DefaultSubject();
        }
        return subject;
    }

    private boolean readHiveFile() {
        try {
            session.setAllSwarmPrincipals(LoginContextSecurityHelper.readSwarmPrincipals());
            return true;
        } catch (Exception e) {
            log.error("Exception (error processing hive file): " + e.getMessage());
            session.invalidate();
            return false ;
        }
    }
}
