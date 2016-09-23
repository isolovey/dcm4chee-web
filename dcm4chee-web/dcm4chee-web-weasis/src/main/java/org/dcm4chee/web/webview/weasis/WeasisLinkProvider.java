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

package org.dcm4chee.web.webview.weasis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dcm4chee.web.common.webview.link.spi.WebviewerLinkProviderSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.dicom.data.Patient;
import org.weasis.dicom.data.SOPInstance;
import org.weasis.dicom.data.Series;
import org.weasis.dicom.data.Study;
import org.weasis.dicom.data.TagW;
import org.weasis.dicom.util.StringUtil;
import org.weasis.dicom.wado.WadoParameters;
import org.weasis.dicom.wado.WadoQuery;
import org.weasis.dicom.wado.WadoQueryException;
import org.weasis.util.EncryptUtils;

/**
 * @author Nicolas Roduit
 * @version $Revision$ $Date$
 * @since May 15, 2010
 */
public class WeasisLinkProvider extends WebviewerLinkProviderSPI {
    private static final long serialVersionUID = 4548297230882756086L;

    private static final Logger LOGGER = LoggerFactory.getLogger(WeasisLinkProvider.class);

    private static String ctxUrl = "/weasis-pacs-connector/viewer?";

    private String encryptKey;
    private String wadoUrl;
    private boolean canonicalHostName = false;

    @Override
    public String getName() {
        return "weasis";
    }

    @Override
    public void setBaseURL(String baseUrl) {
        if (baseUrl != null) {
            WeasisLinkProvider.ctxUrl = baseUrl;
            if (WeasisLinkProvider.ctxUrl.indexOf("?") == -1) {
                WeasisLinkProvider.ctxUrl += "?";
            }
        }
        try {
            URL config = this.getClass().getResource("/weasis-pacs-connector.properties");
            if (config != null) {
                Properties pacsProperties = new Properties();
                pacsProperties.load(config.openStream());
                encryptKey = pacsProperties.getProperty("encrypt.key", null);
                wadoUrl = pacsProperties.getProperty("pacs.wado.url", null);
                canonicalHostName =
                    StringUtil.getNULLtoFalse(pacsProperties.getProperty("server.canonical.hostname.mode"));
            }
        } catch (Exception e) {
            StringUtil.logError(LOGGER, e, "Cannot read weasis-pacs-connector.properties");
        }
    }

    @Override
    public boolean supportPatientLevel() {
        return true;
    }

    @Override
    public boolean supportStudyLevel() {
        return true;
    }

    @Override
    public boolean supportSeriesLevel() {
        return true;
    }

    @Override
    public boolean supportInstanceLevel() {
        return true;
    }

    @Override
    public boolean supportPresentationState() {
        return false;
    }

    @Override
    public boolean supportKeySelectionObject() {
        return false;
    }

    @Override
    public boolean supportStructuredReport() {
        return false;
    }

    @Override
    public boolean supportViewingAllSelection() {
        return true;
    }

    @Override
    public boolean notWebPageLinkTarget() {
        return ctxUrl.endsWith("applet?") == false;
    }

    @Override
    public String viewAllSelection(Map<String, Map<Integer, Object>> patients, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        if (!patients.isEmpty()) {
            String baseURL = getBaseURL(request, canonicalHostName);
            URL url = new URL(baseURL + ctxUrl + "upload=manifest");
            URLConnection urlc = url.openConnection();
            urlc.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");
            urlc.setDoOutput(true);
            urlc.setDoInput(true);
            PrintWriter pw = new PrintWriter(urlc.getOutputStream());
            // http send
            pw.write(convertModelToXml(patients, baseURL));
            pw.close();

            if (notWebPageLinkTarget()) {
                if (response != null) {
                    response.setDateHeader("Date", System.currentTimeMillis());
                    response.setDateHeader("Expires", 0);
                    response.setHeader("Pragma", "no-cache");
                    response.setHeader("Cache-Control", "no-cache, no-store");
                    response.setContentType("application/x-java-jnlp-file");
                    response.setHeader("Content-Disposition", String.format("inline; filename=\"%s\"", "viewer.jnlp"));
                    write(urlc.getInputStream(), response.getOutputStream());
                }
            } else {
                StringBuilder buf = new StringBuilder();
                BufferedReader in = new BufferedReader(new InputStreamReader(urlc.getInputStream()));
                try {
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        buf.append(inputLine);
                    }
                } finally {
                    in.close();
                }
                return buf.toString();
            }
        }
        return null;
    }

    public static void write(InputStream in, OutputStream out) throws IOException {
        try {
            copy(in, out, 2048);
        } catch (Exception e) {
            handleException(e);
        } finally {
            try {
                in.close();
                out.flush();
            } catch (IOException e) {
                // jetty 6 throws broken pipe exception here too
                handleException(e);
            }
        }
    }

    public static int copy(final InputStream in, final OutputStream out, final int bufSize) throws IOException {
        final byte[] buffer = new byte[bufSize];
        int bytesCopied = 0;
        while (true) {
            int byteCount = in.read(buffer, 0, buffer.length);
            if (byteCount <= 0) {
                break;
            }
            out.write(buffer, 0, byteCount);
            bytesCopied += byteCount;
        }
        return bytesCopied;
    }

    private static void handleException(Exception e) {
        Throwable throwable = e;
        boolean ignoreException = false;
        while (throwable != null) {
            if (throwable instanceof SQLException) {
                break; // leave false and quit loop
            } else if (throwable instanceof SocketException) {
                String message = throwable.getMessage();
                ignoreException =
                    message != null
                        && (message.indexOf("Connection reset") != -1 || message.indexOf("Broken pipe") != -1
                            || message.indexOf("Socket closed") != -1 || message.indexOf("connection abort") != -1);
            } else {
                ignoreException =
                    throwable.getClass().getName().indexOf("ClientAbortException") >= 0
                        || throwable.getClass().getName().indexOf("EofException") >= 0;
            }
            if (ignoreException) {
                break;
            }
            throwable = throwable.getCause();
        }
        if (!ignoreException) {
            throw new RuntimeException("Unable to write the response", e);
        }
    }

    private String convertModelToXml(Map<String, Map<Integer, Object>> patients, String baseURL)
        throws WadoQueryException {
        List<Patient> pts = new ArrayList<Patient>();

        for (Map<Integer, Object> patient : patients.values()) {
            Patient p =
                new Patient((String) patient.get(TagW.PatientID.getId()), (String) patient.get(TagW.IssuerOfPatientID
                    .getId()));
            p.setPatientName((String) patient.get(TagW.PatientName.getId()));
            p.setPatientBirthDate((String) patient.get(TagW.PatientBirthDate.getId()));
            p.setPatientSex((String) patient.get(TagW.PatientSex.getId()));
            pts.add(p);

            Map<String, Map<Integer, Object>> studies = (Map<String, Map<Integer, Object>>) patient.get(0);
            if (studies != null && !studies.isEmpty()) {
                for (Map<Integer, Object> study : studies.values()) {
                    Study st = new Study((String) study.get(TagW.StudyInstanceUID.getId()));
                    st.setStudyDescription((String) study.get(TagW.StudyDescription.getId()));
                    st.setAccessionNumber((String) study.get(TagW.AccessionNumber.getId()));
                    st.setStudyID((String) study.get(TagW.StudyID.getId()));
                    st.setStudyDate((String) study.get(TagW.StudyDate.getId()));
                    st.setStudyTime((String) study.get(TagW.StudyTime.getId()));
                    st.setReferringPhysicianName((String) study.get(TagW.ReferringPhysicianName.getId()));
                    p.addStudy(st);

                    Map<String, Map<Integer, Object>> series = (Map<String, Map<Integer, Object>>) study.get(0);
                    if (series != null && !series.isEmpty()) {
                        for (Map<Integer, Object> ser : series.values()) {
                            Series s = new Series((String) ser.get(TagW.SeriesInstanceUID.getId()));
                            s.setSeriesDescription((String) ser.get(TagW.SeriesDescription.getId()));
                            s.setModality((String) ser.get(TagW.Modality.getId()));
                            s.setSeriesNumber((String) ser.get(TagW.SeriesNumber.getId()));
                            st.addSeries(s);

                            Map<String, Map<Integer, Object>> instances =
                                (Map<String, Map<Integer, Object>>) ser.get(0);
                            if (instances != null && !instances.isEmpty()) {
                                for (Map<Integer, Object> instance : instances.values()) {
                                    SOPInstance i = new SOPInstance((String) instance.get(TagW.SOPInstanceUID.getId()));
                                    i.setInstanceNumber((String) instance.get(TagW.InstanceNumber.getId()));
                                    s.addSOPInstance(i);
                                }
                            }
                        }
                    }
                }
            }
        }

        if (StringUtil.hasText(wadoUrl)) {
            if (wadoUrl.startsWith("$")) {
                wadoUrl = wadoUrl.replace("${server.base.url}", baseURL);
            }
        }

        WadoParameters wadoParams =
            new WadoParameters(StringUtil.hasText(wadoUrl) ? wadoUrl : baseURL + "/wado", false, null, null, null);
        WadoQuery wadoQuery = new WadoQuery(pts, wadoParams, "UTF-8", false);

        return wadoQuery.toXmlManifest();
    }

    @Override
    public String getUrlForPatient(String patientId, String issuer) {
        StringBuilder buffer = new StringBuilder(ctxUrl);
        addPatient(buffer, patientId, issuer);
        return buffer.toString();
    }

    @Override
    public String getUrlForStudy(String studyIuid) {
        StringBuilder buffer = new StringBuilder(ctxUrl);
        addStudy(buffer, studyIuid);
        return buffer.toString();
    }

    @Override
    public String getUrlForSeries(String seriesIuid) {
        StringBuilder buffer = new StringBuilder(ctxUrl);
        addSeries(buffer, seriesIuid);
        return buffer.toString();
    }

    @Override
    public String getUrlForInstance(String sopIuid) {
        StringBuilder buffer = new StringBuilder(ctxUrl);
        addInstance(buffer, sopIuid);
        return buffer.toString();
    }

    @Override
    public String getUrlForPresentationState(String iuid) {
        return null;
    }

    @Override
    public String getUrlForKeyObjectSelection(String iuid) {
        return null;
    }

    @Override
    public String getUrlForStructuredReport(String arg0) {
        return null;
    }

    public void addPatient(StringBuilder buffer, String patientId, String issuer) {
        if (encryptKey == null) {
            buffer.append("patientID=");
            buffer.append(patientId);
            if (issuer != null) {
                buffer.append("%5E%5E%5E");
                buffer.append(issuer);
            }
        } else {
            buffer.append("patientID=");
            String message = issuer == null ? patientId : patientId + "%5E%5E%5E" + issuer;
            buffer.append(EncryptUtils.encrypt(message, encryptKey));
        }
    }

    public void addStudy(StringBuilder buffer, String studyIuid) {
        buffer.append("studyUID=");
        buffer.append(encryptKey == null ? studyIuid : EncryptUtils.encrypt(studyIuid, encryptKey));
    }

    public void addSeries(StringBuilder buffer, String seriesIuid) {
        buffer.append("seriesUID=");
        buffer.append(encryptKey == null ? seriesIuid : EncryptUtils.encrypt(seriesIuid, encryptKey));
    }

    public void addInstance(StringBuilder buffer, String sopIuid) {
        buffer.append("objectUID=");
        buffer.append(encryptKey == null ? sopIuid : EncryptUtils.encrypt(sopIuid, encryptKey));
    }

    public static String getBaseURL(HttpServletRequest request, boolean canonicalHostName) {
        if (canonicalHostName) {
            try {
                /**
                 * To get Fully Qualified Domain Name behind bigIP it's better using
                 * InetAddress.getLocalHost().getCanonicalHostName() instead of req.getLocalAddr()<br>
                 * If not resolved from the DNS server FQDM is taken from the /etc/hosts on Unix server
                 */
                return request.getScheme() + "://" + InetAddress.getLocalHost().getCanonicalHostName() + ":"
                    + request.getServerPort();
            } catch (UnknownHostException e) {
                StringUtil.logError(LOGGER, e, "Cannot get hostname");
            }
        }
        return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
    }

}
