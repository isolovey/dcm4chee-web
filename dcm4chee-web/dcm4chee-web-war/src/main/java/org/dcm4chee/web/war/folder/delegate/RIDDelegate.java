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

package org.dcm4chee.web.war.folder.delegate;

import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;

import org.apache.wicket.RequestCycle;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.dcm4chee.web.war.config.delegate.WebCfgDelegate;
import org.dcm4chee.web.war.folder.model.InstanceModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 * @version $Revision: 15552 $ $Date: 2011-06-07 17:05:40 +0200 (Di, 07 Jun 2011) $
 * @since May 31, 2011
 */
public class RIDDelegate  {

    private static RIDDelegate delegate;

    private static Logger log = LoggerFactory.getLogger(RIDDelegate.class);

    public String getRIDBaseUrl() {
        String ridBaseURL = WebCfgDelegate.getInstance().getRIDBaseURL();
        if (ridBaseURL == null) {
            HttpServletRequest request = ((WebRequestCycle)RequestCycle.get()).getWebRequest()
            .getHttpServletRequest();
            try {
                URL ridURL = new URL( request.isSecure() ? "https" : "http", request.getServerName(),
                        request.getServerPort(), "/rid/IHERetrieveDocument?requestType=DOCUMENT");
                ridBaseURL = ridURL.toString();
            } catch (MalformedURLException e) {
                log.warn("Cant build RID Base URL for request! use http://localhost:8080/rid/IHERetrieveDocument?requestType=DOCUMENT");
                ridBaseURL = "http://localhost:8080/rid/IHERetrieveDocument?requestType=DOCUMENT";
            }
        }
        return ridBaseURL;
    }

    
    public String getURL(InstanceModel instModel, String contentType) {
        return getRIDBaseUrl()+"&documentUID="+instModel.getSOPInstanceUID()+"&preferredContentType="+contentType;
    }
    
    public static RIDDelegate getInstance() {
        if (delegate==null)
            delegate = new RIDDelegate();
        return delegate;
    }

}
