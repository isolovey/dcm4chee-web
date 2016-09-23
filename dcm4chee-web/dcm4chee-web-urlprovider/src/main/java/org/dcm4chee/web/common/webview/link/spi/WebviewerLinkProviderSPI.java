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

package org.dcm4chee.web.common.webview.link.spi;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 * @version $Revision$ $Date$
 * @since May 15, 2010
 */
public abstract class WebviewerLinkProviderSPI implements Serializable {

    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(WebviewerLinkProviderSPI.class);
    
    public abstract String getName();
    public abstract boolean supportPatientLevel();
    public abstract boolean supportStudyLevel();
    public abstract boolean supportSeriesLevel();
    public abstract boolean supportInstanceLevel();
    public abstract boolean supportPresentationState();
    public abstract boolean supportKeySelectionObject();
    public abstract boolean supportStructuredReport();
    public abstract String getUrlForPatient(String patientId, String issuer);
    public abstract String getUrlForStudy(String studyIuid);
    public abstract String getUrlForSeries(String seriesIuid);
    public abstract String getUrlForInstance(String sopIuid);
    public abstract String getUrlForPresentationState(String iuid);
    public abstract String getUrlForKeyObjectSelection(String iuid);
    public abstract String getUrlForStructuredReport(String iuid);
    public void setBaseURL(String baseUrl) {
        log.warn("This WebviewerLinkProvider ignores setting of Base Webviewer URL! base URL:"+baseUrl);
    }
    public boolean hasOwnWindow() {
    	return false;
    }
    public boolean notWebPageLinkTarget() {
        return false;
    }
    public boolean supportViewingAllSelection() {
        return false;
    }
    public String viewAllSelection(Map<String, Map<Integer, Object>> patients, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        return null;
    }
}
