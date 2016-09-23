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
import org.dcm4chee.web.war.folder.model.SeriesModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 * @version $Revision: 15582 $ $Date: 2011-06-15 17:34:22 +0200 (Mi, 15 Jun 2011) $
 * @since Aug 18, 2009
 */
public class WADODelegate  {

    public static final int NOT_RENDERABLE = -1;
    public static final int IMAGE = 1;
    public static final int TEXT = 2;
    public static final int VIDEO = 3;
    public static final int ENCAPSULATED = 4;
    public static final int PRESENTATION_STATE = 5;
    
    private static WADODelegate delegate;

    private static Logger log = LoggerFactory.getLogger(WADODelegate.class);

    private WADODelegate() {
        super();
    }

    public String getWadoBaseUrl() {
        String wadoBaseURL = WebCfgDelegate.getInstance().getWadoBaseURL();
        if (wadoBaseURL==null) {
            HttpServletRequest request = ((WebRequestCycle)RequestCycle.get()).getWebRequest()
            .getHttpServletRequest();
            try {
                URL wadoURL = new URL( request.isSecure() ? "https" : "http", request.getServerName(),
                        request.getServerPort(), "/wado?requestType=WADO");
                wadoBaseURL = wadoURL.toString();
            } catch (MalformedURLException e) {
                log.warn("Cant build WADO Base URL for request! use http://localhost:8080/wado?requestType=WADO");
                wadoBaseURL = "http://localhost:8080/wado?requestType=WADO";
            }
        }
        return wadoBaseURL;
    }

    public int getRenderType(String cuid) {
        int type = WebCfgDelegate.getInstance().checkCUID(cuid);
        switch (type) {
            case 0:
                return IMAGE;
            case 1:
                return TEXT;
            case 2:
                return VIDEO;
            case 3:
                return ENCAPSULATED;
        }
        return NOT_RENDERABLE;
    }
    
    public String getURL(InstanceModel instModel) {
        SeriesModel seriesModel = instModel.getSeries();
        return getWadoBaseUrl()+"&studyUID="+seriesModel.getPPS().getStudy().getStudyInstanceUID()+"&seriesUID="+
            seriesModel.getSeriesInstanceUID()+"&objectUID="+instModel.getSOPInstanceUID();
    }
    
    public static WADODelegate getInstance() {
        if (delegate==null)
            delegate = new WADODelegate();
        return delegate;
    }

}
