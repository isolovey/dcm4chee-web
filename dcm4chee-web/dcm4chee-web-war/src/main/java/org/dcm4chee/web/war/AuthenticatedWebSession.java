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

import org.apache.wicket.Request;
import org.dcm4chee.web.common.base.BaseWicketApplication;
import org.dcm4chee.web.common.secure.SecureSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision: 16725 $ $Date: 2012-03-22 16:03:00 +0100 (Do, 22 Mär 2012) $
 * @since Jan 13, 2009
 */
public class AuthenticatedWebSession extends SecureSession {

    private static final long serialVersionUID = 1L;
    
    protected static Logger log = LoggerFactory.getLogger(AuthenticatedWebSession.class);

    private org.dcm4chee.web.war.folder.ViewPort folderViewport = new org.dcm4chee.web.war.folder.ViewPort();
    private org.dcm4chee.web.war.trash.ViewPort trashViewport = new org.dcm4chee.web.war.trash.ViewPort();
    private org.dcm4chee.web.war.worklist.modality.ViewPort mwViewport = new org.dcm4chee.web.war.worklist.modality.ViewPort();
    private StudyPermissionHelper studyPermissionHelper;
    
    public AuthenticatedWebSession(BaseWicketApplication wicketApplication, Request request) {
        super(wicketApplication, request);
    }

    @Override
    public void extendedLogin(String username, String passwd, org.apache.wicket.security.hive.authentication.Subject webSubject) {
        try {
            studyPermissionHelper = new StudyPermissionHelper(username, passwd, webSubject);
        } catch (Exception x) {
            throw new RuntimeException("Extended Login Failed!", x);
        }
    }

    @Override
    public void extendedLogin(org.apache.wicket.security.hive.authentication.Subject webSubject) {
        try {
            studyPermissionHelper = new StudyPermissionHelper(webSubject);
        } catch (Exception x) {
            throw new RuntimeException("Extended Login Failed!", x);
        }
    }

    public StudyPermissionHelper getStudyPermissionHelper() {
        return studyPermissionHelper;
    }

    public org.dcm4chee.web.war.folder.ViewPort getFolderViewPort() {
        return folderViewport;
    }

    public org.dcm4chee.web.war.trash.ViewPort getTrashViewPort() {
        return trashViewport;
    }

    public org.dcm4chee.web.war.worklist.modality.ViewPort getMwViewPort() {
        return mwViewport;
    }
}
