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

package org.dcm4chee.usr.war;

import javax.security.auth.Subject;
import javax.security.jacc.PolicyContext;

import org.apache.wicket.Page;
import org.apache.wicket.Request;
import org.apache.wicket.Response;
import org.apache.wicket.security.authentication.LoginException;
import org.dcm4chee.usr.war.pages.UserManagementMainPage;
import org.dcm4chee.web.common.base.BaseWicketApplication;
import org.dcm4chee.web.common.login.SSOLoginContext;
import org.dcm4chee.web.common.secure.SecureSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision$ $Date$
 * @since 31.08.2010
 */
public class WicketApplication extends BaseWicketApplication {
    
    protected static Logger log = LoggerFactory.getLogger(WicketApplication.class);
    
    @Override
    public Class<? extends Page> getHomePage() {
        return UserManagementMainPage.class;
    }
    
    @Override
    public SecureSession newSession(Request request, Response response) {
        Subject jaasSubject = null;
        try {
            jaasSubject = (Subject) PolicyContext.getContext("javax.security.auth.Subject.container");
        } catch (Exception x) {
            log.error(getClass().getName() + ": Failed to get subject from javax.security.auth.Subject.container", x);
        }
        SecureSession session = new SecureSession(this, request);
        if (jaasSubject != null) {
            try {
                session.login(new SSOLoginContext(session, jaasSubject));
            } catch (LoginException x) {
                log.error(getClass().getName() + ": Failed login", x);
            }
        }
        return session;
    }
}
