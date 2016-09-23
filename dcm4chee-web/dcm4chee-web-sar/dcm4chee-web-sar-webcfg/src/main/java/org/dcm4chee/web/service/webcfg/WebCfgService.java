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
 * Java(TM), available at http://sourceforge.net/projects/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * TIANI Medgraph AG.
 * Portions created by the Initial Developer are Copyright (C) 2003-2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * Gunter Zeilinger <gunter.zeilinger@tiani.com>
 * Franz Willer <franz.willer@gwi-ag.com>
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
package org.dcm4chee.web.service.webcfg;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import javax.imageio.spi.ServiceRegistry;
import javax.management.Attribute;
import javax.management.InstanceNotFoundException;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.timer.TimerNotification;

import org.dcm4che2.data.UID;
import org.dcm4che2.util.UIDUtils;
import org.dcm4chee.archive.util.JNDIUtils;
import org.dcm4chee.web.common.webview.link.spi.WebviewerLinkProviderSPI;
import org.dcm4chee.web.dao.folder.StudyListLocal;
import org.dcm4chee.web.dao.folder.StudyPermissionsLocal;
import org.dcm4chee.web.dao.trash.TrashCleanerLocal;
import org.dcm4chee.web.dao.worklist.modality.ModalityWorklistLocal;
import org.dcm4chee.web.service.common.RetryIntervalls;
import org.jboss.system.ServiceMBeanSupport;
import org.jboss.system.server.ServerConfigLocator;

/**
 * @author franz.willer@gmail.com
 * @author robert.david@agfa.com
 * @version $Revision$ $Date$
 * @since July 26, 2010
 */
public class WebCfgService extends ServiceMBeanSupport implements
        NotificationListener, NotificationFilter {

    private static final String PACS_WEB_DEFAULT_LOCALE = "pacs.web.default.locale";

	protected static final long serialVersionUID = 1L;

    protected boolean manageUsers;

    protected String webConfigPath;

    protected Map<String, int[]> windowsizeMap = new LinkedHashMap<String, int[]>();

    protected static final String NONE = "NONE";

    protected final String NEWLINE = System.getProperty("line.separator", "\n");

    private static final String ANY = "ANY";
    private static final String EMPTY = "EMPTY";

    private static final String DEFAULT_TIMER_SERVICE = "jboss:service=Timer";

    private static final long ONE_DAY_IN_MILLIS = 60000 * 60 * 24;

    public static final String[] LEVEL_STRINGS = { "Patient", "Study", "PPS",
            "Series", "Instance" };
    private static final String SYSTEM_PROPERTY_READINGROOM = "org.dcm4chee.web.readingroom";
    
    private String dicomSecurityServletUrl;
    private String wadoBaseURL;
    private String ridBaseURL;
    private String arrURL;
    
    private List<String> webviewerNames;
    private Map<String, String> webviewerBaseUrls = new HashMap<String, String>();

    private ObjectName aeServiceName;
    private ObjectName echoServiceName;
    private ObjectName moveScuServiceName;
    private ObjectName tcStoreScuServiceName;
    private ObjectName contentEditServiceName;
    private ObjectName storeBridgeServiceName;
    private ObjectName mppsEmulatorServiceName;
    private ObjectName mwlscuServiceName;
    private ObjectName tarRetrieveServiceName;
    private ObjectName mppsForwardServiceName;
    private ObjectName timerServiceName;

    private Map<String, String> imageCUIDS = new LinkedHashMap<String, String>();
    private Map<String, String> srCUIDS = new LinkedHashMap<String, String>();
    private Map<String, String> psCUIDS = new LinkedHashMap<String, String>();
    private Map<String, String> waveformCUIDS = new LinkedHashMap<String, String>();
    private Map<String, String> videoCUIDS = new LinkedHashMap<String, String>();
    private Map<String, String> encapsulatedCUIDS = new LinkedHashMap<String, String>();

    private Map<String, List<String>> ridMimeTypes = new LinkedHashMap<String, List<String>>();

    private String tcForumIntegrationType;
    private String tcSeriesDisplayFormat;
    private String tcKeywordCataloguesPath;
    private Map<String, String> tcKeywordCatalogues = new LinkedHashMap<String, String>();
    private Map<String, String> tcKeywordCataloguesExclusive = new LinkedHashMap<String, String>();
    private List<String> tcRestrictedSrcAETs = new ArrayList<String>();
    private List<String> tcRestrictedAttributes = new ArrayList<String>();
    private List<String> tcEthnicGroups = new ArrayList<String>();
    private boolean tcEditOnDoubleClick;
    private boolean tcMultipleKeywordSearch;
    private boolean tcMultipleKeywordORConcat;
    private boolean tcShowImagesInDialog;
    
    private String patientIDPattern;
    private String issuerOfPatientID;

    private List<String> modalities = new ArrayList<String>();
    private List<String> aetTypes = new ArrayList<String>();
    
    private String aeManagementDefault = new String();
    
    private List<String> stationNames = new ArrayList<String>();

    private boolean autoUpdateModalities;
    private boolean autoUpdateStationNames;
    private boolean autoUpdateDicomRoles;
    private Integer autoUpdateTimerId;

    private List<Integer> pagesizes = new ArrayList<Integer>();
    private int defaultFolderPagesize;
    private int defaultMWLPagesize;
    private boolean queryAfterPagesizeChange;

    private String mpps2mwlPresetPatientname;
    private String mpps2mwlPresetModality;
    private String mpps2mwlPresetStartDate;
    private boolean mpps2mwlAutoQuery;

    private boolean useFamilyAndGivenNameQueryFields;

    private boolean manageStudyPermissions;
    private boolean useStudyPermissions;

    private boolean forcePatientExpandableForPatientQuery;

    private String tooOldLimit;
    private String ignoreEditTimeLimitRolename;

    private String retentionTime;

    private String emptyTrashInterval;
    private Integer trashCleanerTimerId;

    private List<Integer> autoExpandLevelChoices = new ArrayList<Integer>(6);

    private int autoWildcard;
    
    private boolean defaultHidePPS;
    private boolean defaultLatestMwlItemsFirst;
    private boolean defaultLatestStudiesFirst;

    private int hasNotificationListener;
    
    private String zipEntryTemplate;
    private int bufferSize;
    
    private String presetStudyDateOffset;

	private boolean trustPatientIdWithoutIssuer;

    private List<String> modalityFilter = new ArrayList<String>();
    
    private int searchWarningThreshold;
    
    private boolean showDoneDialogAfterAction;
    
    public WebCfgService() throws MalformedObjectNameException {
        timerServiceName = new ObjectName(DEFAULT_TIMER_SERVICE);
    }

    @Override
    public void startService() {
        this.updateAutoUpdateTimer();
        this.configureTrashCleaner();
    }

    public void setDicomSecurityServletUrl(String dicomSecurityServletUrl) {
        this.dicomSecurityServletUrl = dicomSecurityServletUrl;
    }

    public String getDicomSecurityServletUrl() {
        return dicomSecurityServletUrl;
    }

    public String getWadoBaseURL() {
        return wadoBaseURL;
    }

    public void setWadoBaseURL(String wadoBaseURL) {
        this.wadoBaseURL = wadoBaseURL;
    }

    public String getRIDBaseURL() {
        return ridBaseURL;
    }

    public void setRIDBaseURL(String url) {
        this.ridBaseURL = url;
    }

    public String getArrURL() {
        return arrURL;
    }

    public void setArrURL(String url) {
        this.arrURL = url;
    }

    public String getInstalledWebViewer() {
        List<String> l = getInstalledWebViewerNameList();
        if (l == null)
            return "ERROR";
        StringBuilder sb = new StringBuilder();
        for (int i = 0, len = l.size(); i < len; i++) {
            sb.append(l.get(i)).append(NEWLINE);
        }
        return sb.toString();
    }

    public List<String> getInstalledWebViewerNameList() {
        try {
            ArrayList<String> l = new ArrayList<String>();
            Iterator<WebviewerLinkProviderSPI> iter = ServiceRegistry
                    .lookupProviders(WebviewerLinkProviderSPI.class);
            while (iter.hasNext()) {
                l.add(iter.next().getName());
            }
            return l;
        } catch (Throwable t) {
            log.error("getInstalledWebViewer failed! reason:" + t, t);
            return null;
        }
    }

    public List<String> getWebviewerNameList() {
        return webviewerNames;
    }

    public String getWebviewerNames() {
        return listAsString(webviewerNames, NEWLINE);
    }

    public void setWebviewerNames(String names) {
        this.webviewerNames = stringAsList(names, NEWLINE);
    }

    public Map<String, String> getWebviewerBaseUrlMap() {
        return webviewerBaseUrls;
    }

    public String getWebviewerBaseUrls() {
        return mapAsString(webviewerBaseUrls);
    }

    public void setWebviewerBaseUrls(String urls) {
        this.webviewerBaseUrls = stringAsMap(urls);
    }

    public String getPatientIDPattern() {
        return patientIDPattern;
    }

    public void setPatientIDPattern(String p) {
        if (p.indexOf('{') == -1 || p.indexOf('}') == -1)
            throw new IllegalArgumentException("Wrong pattern format! Must contain '{' and '}'");
        this.patientIDPattern = p;
    }

    public String getIssuerOfPatientID() {
        return issuerOfPatientID;
    }

    public void setIssuerOfPatientID(String issuerOfPatientID) {
        if (issuerOfPatientID.trim().length() == 0) {
            throw new IllegalArgumentException("Issuer of patient ID must not be empty!");
        }
        this.issuerOfPatientID = issuerOfPatientID;
    }

    public String getImageCUIDS() {
        return uidsToString(imageCUIDS);
    }

    public void setImageCUIDS(String imageCUIDS) {
        this.imageCUIDS = parseUIDs(imageCUIDS);
    }

    public String getSrCUIDS() {
        return uidsToString(srCUIDS);
    }

    public void setSrCUIDS(String srCUIDS) {
        this.srCUIDS = parseUIDs(srCUIDS);
    }

    public String getPsCUIDS() {
        return uidsToString(psCUIDS);
    }

    public void setPsCUIDS(String psCUIDS) {
        this.psCUIDS = parseUIDs(psCUIDS);
    }

    public String getWaveformCUIDS() {
        return uidsToString(waveformCUIDS);
    }

    public void setWaveformCUIDS(String waveformCUIDS) {
        this.waveformCUIDS = parseUIDs(waveformCUIDS);
    }

    public String getVideoCUIDS() {
        return uidsToString(videoCUIDS);
    }

    public void setVideoCUIDS(String videoCUIDS) {
        this.videoCUIDS = parseUIDs(videoCUIDS);
    }

    public String getEncapsulatedCUIDS() {
        return uidsToString(encapsulatedCUIDS);
    }

    public void setEncapsulatedCUIDS(String encapsulatedCUIDS) {
        this.encapsulatedCUIDS = parseUIDs(encapsulatedCUIDS);
    }

    public String getTCKeywordCataloguesPath() {
        return tcKeywordCataloguesPath;
    }

    public void setTCKeywordCataloguesPath(String path) {
        tcKeywordCataloguesPath = path;
    }

    public Map<String, String> getTCKeywordCataloguesMap() {
        Map<String, String> map = new LinkedHashMap<String, String>();
        if (tcKeywordCatalogues!=null) {
            map.putAll(tcKeywordCatalogues);
        }
        if (tcKeywordCataloguesExclusive!=null) {
            map.putAll(tcKeywordCataloguesExclusive);
        }
        return Collections.unmodifiableMap(map);
    }
    
    public Map<String, String> getTCKeywordCataloguesMapExclusive() {
        if (tcKeywordCataloguesExclusive!=null) {
            return Collections.unmodifiableMap(tcKeywordCataloguesExclusive);
        }
        else {
            return Collections.emptyMap();
        }
    }
    
    public Map<String, String> getTCKeywordCataloguesMapNonExclusive() {
        if (tcKeywordCatalogues!=null) {
            return Collections.unmodifiableMap(tcKeywordCatalogues);
        }
        else {
            return Collections.emptyMap();
        }
    }
    
    public String getTCForumIntegrationType() {
    	return tcForumIntegrationType;
    }
    
    public void setTCForumIntegrationType(String type) {
    	this.tcForumIntegrationType = type!=null ?
    			type.trim() : type;
    }

    public String getTCKeywordCatalogues() {
        return keywordCataloguesToString(tcKeywordCatalogues, tcKeywordCataloguesExclusive);
    }

    public void setTCKeywordCatalogues(String s) {
        this.tcKeywordCatalogues = parseKeywordCatalogues(s, false);
        this.tcKeywordCataloguesExclusive = parseKeywordCatalogues(s, true);
    }
    
    public List<String> getTCRestrictedAttributesList() {
    	if (tcRestrictedAttributes!=null) {
    		return tcRestrictedAttributes;
    	}
    	return Collections.emptyList();
    }
    
    public String getTCRestrictedAttributes() {
    	if (tcRestrictedAttributes==null || 
    			tcRestrictedAttributes.isEmpty()) {
    		return NONE;
    	}
    	else {
    		StringBuilder sbuilder = new StringBuilder();
    		sbuilder.append(tcRestrictedAttributes.get(0));
    		for (int i=1; i<tcRestrictedAttributes.size(); i++) {
    			sbuilder.append(NEWLINE).append(
    					tcRestrictedAttributes.get(i));
    		}
    		return sbuilder.toString();
    	}
    }
    
    public void setTCRestrictedAttributes(String s) {
    	if (tcRestrictedAttributes!=null) {
    		tcRestrictedAttributes.clear();
    	}
        if (s!= null && !NONE.equals(s.trim()))
        {
	        for (StringTokenizer st = new StringTokenizer(s, "\t\r\n;") ; st.hasMoreTokens() ;) {
	            tcRestrictedAttributes.add(
	            		st.nextToken().trim());
	        }
        }
    }

    public String getTCRestrictedSourceAETs() {
        return listAsString(tcRestrictedSrcAETs, "|");
    }

    public List<String> getTCRestrictedSourceAETList() {
        return new ArrayList<String>(tcRestrictedSrcAETs);
    }

    public void setTCRestrictedSourceAETs(String s) {
        updateList(tcRestrictedSrcAETs, s, "|");
    }
    
    public List<String> getTCEthnicGroupsAsList()
    {
    	return Collections.unmodifiableList( tcEthnicGroups );
    }
    
    public String getTCEthnicGroups()
    {
    	String s = listAsString(tcEthnicGroups, NEWLINE);
    	if (s==null || s.equals(NONE)) {
    		return NONE+NEWLINE;
    	}
    	return s;
    }
    
    public void setTCEthnicGroups( String ethnicGroups )
    {
    	updateList( tcEthnicGroups, ethnicGroups!=null ?
    			ethnicGroups.trim() : ethnicGroups, NEWLINE );
    }
        
    public boolean isTCEditOnDoubleClick() {
        return tcEditOnDoubleClick;
    }

    public void setTCEditOnDoubleClick(boolean b) {
        tcEditOnDoubleClick = b;
    }
    
    public boolean isTCShowImagesInDialogEnabled() {
        return tcShowImagesInDialog;
    }

    public void setTCShowImagesInDialogEnabled(boolean b) {
        tcShowImagesInDialog = b;
    }
    
    public boolean isTCMultipleKeywordSearchEnabled() {
        return tcMultipleKeywordSearch;
    }

    public void setTCMultipleKeywordSearchEnabled(boolean b) {
        tcMultipleKeywordSearch = b;
    }
    
    public boolean isTCMultipleKeywordORConcatEnabled() {
    	return tcMultipleKeywordORConcat;
    }
        
    public void setTCMultipleKeywordORConcatEnabled(boolean b) {
    	tcMultipleKeywordORConcat = b;
    }
    
    public String getTCSeriesDisplayFormat()
    {
        return tcSeriesDisplayFormat;
    }
    
    public void setTCSeriesDisplayFormat(String s)
    {
        tcSeriesDisplayFormat = s;
    }

    public String getRIDMimeTypes() {
        if (ridMimeTypes.isEmpty())
            return NONE;
        StringBuilder sb = new StringBuilder();
        Entry<String, List<String>> entry;
        for (Iterator<Entry<String, List<String>>> it = ridMimeTypes.entrySet()
                .iterator(); it.hasNext();) {
            entry = it.next();
            sb.append(entry.getKey()).append(':')
                    .append(listAsString(entry.getValue(), "|"))
                    .append(NEWLINE);
        }
        return sb.toString();
    }

    public void setRIDMimeTypes(String mimes) {
        Map<String, List<String>> map = new LinkedHashMap<String, List<String>>();
        if (!NONE.equals(mimes)) {
            StringTokenizer st = new StringTokenizer(mimes, " \t\r\n;");
            String line;
            int pos;
            while (st.hasMoreTokens()) {
                line = st.nextToken().trim();
                pos = line.indexOf(':');
                if (pos == -1)
                    throw new IllegalArgumentException(
                            "Wrong RidCUIDSwithMime format! Missing ':'!");
                map.put(line.substring(0, pos++),
                        stringAsList(line.substring(pos), "|"));
            }
        }
        this.ridMimeTypes = map;
    }

    public List<String> getRIDMimeTypesForCuid(String cuid) {
        if (srCUIDS.containsValue(cuid)) {
            return ridMimeTypes.get("SR");
        } else if (encapsulatedCUIDS.containsValue(cuid)) {
            return ridMimeTypes.get("DOC");
        } else if (waveformCUIDS.containsValue(cuid)) {
            return ridMimeTypes.get("ECG");
        }
        return null;
    }

    public String getModalities() {
        return listAsString(modalities, "|");
    }

    public List<String> getModalityList() {
        if (modalities.isEmpty()) {
            this.updateModalities();
        }
        return new ArrayList<String>(modalities);
    }

    public void setModalities(String s) {
        updateList(modalities, s, "|");
    }

    public boolean isAutoUpdateModalities() {
        return autoUpdateModalities;
    }

    public void setAutoUpdateModalities(boolean b) {
        autoUpdateModalities = b;
        updateAutoUpdateTimer();
    }

    public String getAETTypes() {
        return listAsString(aetTypes, "|");
    }

    public List<String> getAETTypesList() {
        return new ArrayList<String>(aetTypes);
    }

    public void setAETTypes(String s) {
        updateList(aetTypes, s, "|");
    }

    public String getAEManagementDefault() {
        return aeManagementDefault;
    }

    public void setAEManagementDefault(String aeManagementDefault) {
        this.aeManagementDefault = aeManagementDefault;
    }

    public String getStationNames() {
        return listAsString(stationNames, "|");
    }

    public List<String> getStationNameList() {
        if (stationNames.isEmpty()) {
            this.updateStationNames();
        }
        return new ArrayList<String>(stationNames);
    }

    public void setStationNames(String s) {
        updateList(stationNames, s, "|");
    }

    public boolean isAutoUpdateStationNames() {
        return autoUpdateStationNames;
    }

    public void setAutoUpdateStationNames(boolean b) {
        autoUpdateStationNames = b;
        updateAutoUpdateTimer();
    }

    public boolean isAutoUpdateDicomRoles() {
        return autoUpdateDicomRoles;
    }

    public void setAutoUpdateDicomRoles(boolean b) {
        autoUpdateDicomRoles = b;
        updateAutoUpdateTimer();
    }

    public int getAutoWildcard() {
        return autoWildcard;
    }

    public void setAutoWildcard(int autoWildcard) {
        if (autoWildcard < 0 || autoWildcard > 2) {
            throw new IllegalArgumentException(
                    "AutoWildcard must be 0, 1 or 2!");
        }
        this.autoWildcard = autoWildcard;
    }

    public List<Integer> getPagesizeList() {
        return pagesizes;
    }

    public String getPagesizes() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0, len = pagesizes.size(); i < len; i++) {
            sb.append(pagesizes.get(i)).append(NEWLINE);
        }
        return sb.toString();
    }

    public void setPagesizes(String s) {
        pagesizes.clear();
        StringTokenizer st = new StringTokenizer(s.trim(), " \t\r\n;");
        String t;
        while (st.hasMoreTokens()) {
            t = st.nextToken().trim();
            if (t.length() > 0)
                pagesizes.add(new Integer(t));
        }
    }

    public int getDefaultFolderPagesize() {
        return defaultFolderPagesize;
    }

    public void setDefaultFolderPagesize(int size) {
        this.defaultFolderPagesize = size;
        if (!pagesizes.contains(new Integer(size))) {
            pagesizes.add(0, size);
        }
    }

    public int getDefaultMWLPagesize() {
        return defaultMWLPagesize;
    }

    public void setDefaultMWLPagesize(int size) {
        this.defaultMWLPagesize = size;
        if (!pagesizes.contains(new Integer(size))) {
            pagesizes.add(0, size);
        }
    }

    public boolean isQueryAfterPagesizeChange() {
        return queryAfterPagesizeChange;
    }

    public void setQueryAfterPagesizeChange(boolean queryAfterPagesizeChange) {
        this.queryAfterPagesizeChange = queryAfterPagesizeChange;
    }

    public boolean isUseFamilyAndGivenNameQueryFields() {
        return useFamilyAndGivenNameQueryFields;
    }

    public void setUseFamilyAndGivenNameQueryFields(boolean b) {
        this.useFamilyAndGivenNameQueryFields = b;
    }

    public boolean isDefaultHidePPS() {
        return defaultHidePPS;
    }

    public void setDefaultHidePPS(boolean defaultHidePPS) {
        this.defaultHidePPS = defaultHidePPS;
    }

    public boolean isDefaultLatestMwlItemsFirst() {
		return defaultLatestMwlItemsFirst;
	}

	public void setDefaultLatestMwlItemsFirst(boolean defaultLatestMwlItemsFirst) {
		this.defaultLatestMwlItemsFirst = defaultLatestMwlItemsFirst;
	}

	public boolean isDefaultLatestStudiesFirst() {
		return defaultLatestStudiesFirst;
	}

	public void setDefaultLatestStudiesFirst(boolean defaultLatestStudiesFirst) {
		this.defaultLatestStudiesFirst = defaultLatestStudiesFirst;
	}

	public boolean isForcePatientExpandableForPatientQuery() {
        return forcePatientExpandableForPatientQuery;
    }

    public void setForcePatientExpandableForPatientQuery(boolean b) {
        this.forcePatientExpandableForPatientQuery = b;
    }

    public String getAutoExpandLevelChoices() {
        StringBuilder sb = new StringBuilder();
        int l;
        for (int i = 0, len = autoExpandLevelChoices.size(); i < len; i++) {
            l = autoExpandLevelChoices.get(i);
            sb.append(l == -1 ? "auto" : LEVEL_STRINGS[l]).append(NEWLINE);
        }
        return sb.toString();
    }

    public List<Integer> getAutoExpandLevelChoiceList() {
        return autoExpandLevelChoices;
    }

    public void setAutoExpandLevelChoices(String choices) {
        autoExpandLevelChoices.clear();
        String tk;
        StringTokenizer st = new StringTokenizer(choices, "\n\r\t;");
        token: while (st.hasMoreTokens()) {
            tk = st.nextToken().trim();
            if ("auto".equals(tk)) {
                autoExpandLevelChoices.add(-1);
            } else {
                for (int i = 0; i < LEVEL_STRINGS.length; i++) {
                    if (LEVEL_STRINGS[i].equals(tk)) {
                        autoExpandLevelChoices.add(i);
                        continue token;
                    }
                }
                throw new IllegalArgumentException("Unknown AutoExpandLevel:"
                        + tk);
            }
        }
        if (autoExpandLevelChoices.isEmpty())
            autoExpandLevelChoices.add(-1);
    }
   
    public String getPresetStudyDate() {
        return this.presetStudyDateOffset == null ? NONE : presetStudyDateOffset;
    }
    
    public void setPresetStudyDate(String s) {
        if (NONE.equals(s)) {
            presetStudyDateOffset = null;
        } else {
            if (new Integer(s) < 0)
                throw new IllegalArgumentException("Offset must not be negative!");
            presetStudyDateOffset = s;
        }
    }
    
    public String getDefaultLanguage() {
    	return System.getProperty(PACS_WEB_DEFAULT_LOCALE, NONE);
    }
    
    public void setDefaultLanguage(String s) {
    	if (NONE.equals(s)) {
        	System.getProperties().remove(PACS_WEB_DEFAULT_LOCALE);
    	} else {
    		System.setProperty(PACS_WEB_DEFAULT_LOCALE, s);
    	}
    }

    private String listAsString(List<String> list, String sep) {
        if (list == null)
            return ANY;
        if (list.isEmpty())
            return NONE;
        StringBuilder sb = new StringBuilder();
        sb.append(list.get(0)==null ? EMPTY : list.get(0));
        for (int i=1; i<list.size(); i++) {
            String s = list.get(i);
            sb.append(sep).append(s==null?EMPTY:s);
        }
        return sb.append(sep).toString();
    }

    private List<String> stringAsList(String s, String sep) {
        s = s.trim();
        if (ANY.equals(s))
            return null;
        ArrayList<String> l = new ArrayList<String>();
        updateList(l, s, sep);
        return l;
    }

    private void updateList(List<String> list, String s, String sep) {
        list.clear();
        if (!NONE.equals(s)) {
            StringTokenizer st = new StringTokenizer(s, sep);
            String tk;
            while (st.hasMoreTokens()) {
                tk = st.nextToken().trim();
                list.add(EMPTY.equals(tk) ? null : tk);
            }
        }
    }

    private String mapAsString(Map<String, String> map) {
        if (map.isEmpty())
            return NONE;
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : map.entrySet()) {
            sb.append(e.getKey()).append(':').append(e.getValue())
                    .append(NEWLINE);
        }
        return sb.toString();
    }

    private Map<String, String> stringAsMap(String s) {
        Map<String, String> m = new HashMap<String, String>();
        if (!NONE.equals(s)) {
            StringTokenizer st = new StringTokenizer(s, "\n\r\t");
            String tk;
            int pos;
            while (st.hasMoreTokens()) {
                tk = st.nextToken();
                pos = tk.indexOf(':');
                if (pos == -1)
                    throw new IllegalArgumentException("Missing ':'");
                m.put(tk.substring(0, pos++), tk.substring(pos));
            }
        }
        return m;
    }

    public String getMpps2mwlPresetPatientname() {
        return mpps2mwlPresetPatientname;
    }

    public void setMpps2mwlPresetPatientname(String mpps2mwlPresetPatientname) {
        this.mpps2mwlPresetPatientname = mpps2mwlPresetPatientname;
    }

    public String getMpps2mwlPresetModality() {
        return mpps2mwlPresetModality;
    }

    public void setMpps2mwlPresetModality(String mpps2mwlPresetModality) {
        this.mpps2mwlPresetModality = mpps2mwlPresetModality;
    }

    public String getMpps2mwlPresetStartDate() {
        return mpps2mwlPresetStartDate;
    }

    public void setMpps2mwlPresetStartDate(String s) {
        if (!"delete".equals(s) && !s.startsWith("today")
                && !s.startsWith("mpps")) {
            throw new IllegalArgumentException(
                    "Preset Start Date must be delete, mpps[(startOffset,endOffset)] or today[((startOffset,endOffset)]! "
                            + s);
        }
        int pos = s.indexOf('(');
        if (pos != -1) {
            try {
                int pos1 = s.indexOf(',', ++pos);
                Integer.parseInt(s.substring(pos, pos1));
                Integer.parseInt(s.substring(++pos1, s.indexOf(')', pos1)));
            } catch (Exception x) {
                log.info("Illegal Date range extension!", x);
                throw new IllegalArgumentException(
                        "Date range extension must be (startOffset,endOffset):"
                                + s);
            }
        }
        mpps2mwlPresetStartDate = s;
    }

    public boolean isMpps2mwlAutoQuery() {
        return mpps2mwlAutoQuery;
    }

    public void setMpps2mwlAutoQuery(boolean mpps2mwlAutoQuery) {
        this.mpps2mwlAutoQuery = mpps2mwlAutoQuery;
    }

    public boolean isShowDoneDialogAfterAction() {
		return showDoneDialogAfterAction;
	}

	public void setShowDoneDialogAfterAction(boolean showDoneDialogAfterAction) {
		this.showDoneDialogAfterAction = showDoneDialogAfterAction;
	}

	public ObjectName getAEServiceName() {
        return aeServiceName;
    }

    public void setAEServiceName(ObjectName name) {
        this.aeServiceName = name;
    }

    public ObjectName getEchoServiceName() {
        return echoServiceName;
    }

    public void setEchoServiceName(ObjectName echoServiceName) {
        this.echoServiceName = echoServiceName;
    }

    public ObjectName getMoveScuServiceName() {
        return moveScuServiceName;
    }

    public void setMoveScuServiceName(ObjectName moveScuServiceName) {
        this.moveScuServiceName = moveScuServiceName;
    }
    
    public ObjectName getTCStoreScuServiceName() {
        return tcStoreScuServiceName;
    }

    public void setTCStoreScuServiceName(ObjectName tcStoreScuServiceName) {
        this.tcStoreScuServiceName = tcStoreScuServiceName;
    }

    public ObjectName getContentEditServiceName() {
        return contentEditServiceName;
    }

    public void setContentEditServiceName(ObjectName contentEditServiceName) {
        this.contentEditServiceName = contentEditServiceName;
    }

    public ObjectName getStoreBridgeServiceName() {
        return storeBridgeServiceName;
    }

    public void setStoreBridgeServiceName(ObjectName storeBridgeServiceName) {
        this.storeBridgeServiceName = storeBridgeServiceName;
    }

    public ObjectName getMppsEmulatorServiceName() {
        return mppsEmulatorServiceName;
    }

    public void setMppsEmulatorServiceName(ObjectName mppsEmulatorServiceName) {
        this.mppsEmulatorServiceName = mppsEmulatorServiceName;
    }

    public ObjectName getMwlScuServiceName() {
        return mwlscuServiceName;
    }

    public void setMwlScuServiceName(ObjectName mwlscuServiceName) {
        this.mwlscuServiceName = mwlscuServiceName;
    }

    public ObjectName getTarRetrieveServiceName() {
        return tarRetrieveServiceName;
    }

    public void setTarRetrieveServiceName(ObjectName name) {
        this.tarRetrieveServiceName = name;
    }

    public ObjectName getMppsforwardServiceName() {
        return mppsForwardServiceName;
    }

    public void setMppsforwardServiceName(ObjectName name) {
        this.mppsForwardServiceName = name;
    }

    public int checkCUID(String cuid) {
        if (isInCuids(cuid, imageCUIDS)) {
            return 0;
        } else if (isInCuids(cuid, srCUIDS)) {
            return 1;
        } else if (isInCuids(cuid, videoCUIDS)) {
            return 2;
        } else if (isInCuids(cuid, encapsulatedCUIDS)) {
            return 3;
        } else if (isInCuids(cuid, waveformCUIDS)) {
            return 4;
        } else if (isInCuids(cuid, psCUIDS)) {
            return 5;
        }
        return -1;
    }

    private boolean isInCuids(String cuid, Map<String, String> cuids) {
        if (cuid == null)
            return false;
        if (!isDigit(cuid.charAt(0))) {
            try {
                cuid = UID.forName(cuid);
            } catch (Throwable t) {
                log.warn("Unknown UID:" + cuid, t);
                return false;
            }
        }
        return cuids.values().contains(cuid);
    }

    private String uidsToString(Map<String, String> uids) {
        if (uids.isEmpty())
            return NONE;
        StringBuilder sb = new StringBuilder();
        Iterator<String> iter = uids.keySet().iterator();
        while (iter.hasNext()) {
            sb.append(iter.next()).append(NEWLINE);
        }
        return sb.toString();
    }

    private static Map<String, String> parseUIDs(String uids) {
        Map<String, String> map = new LinkedHashMap<String, String>();
    	if ("NONE".equals(uids))
    		return map;
        StringTokenizer st = new StringTokenizer(uids, " \t\r\n;");
        String uid, name;
        while (st.hasMoreTokens()) {
            uid = st.nextToken().trim();
            name = uid;
            if (isDigit(uid.charAt(0))) {
                UIDUtils.verifyUID(uid);
            } else {
                uid = UID.forName(name);
            }
            map.put(name, uid);
        }
        return map;
    }

    private String keywordCataloguesToString(Map<String, String> nonExclusiveCatalogues, Map<String, String> exclusiveCatalogues) {
        if ((nonExclusiveCatalogues == null || nonExclusiveCatalogues.isEmpty()) &&
            (exclusiveCatalogues==null || exclusiveCatalogues.isEmpty())) 
        {
            return NONE;
        } else {
            StringBuilder sbuilder = new StringBuilder();
            if (nonExclusiveCatalogues!=null)
            {
                for (Map.Entry<String, String> me : nonExclusiveCatalogues.entrySet()) {
                    sbuilder.append(me.getKey()).append(":").append(me.getValue())
                            .append(",false").append(NEWLINE);
                }
            }
            if (exclusiveCatalogues!=null)
            {
                for (Map.Entry<String, String> me : exclusiveCatalogues.entrySet()) {
                    sbuilder.append(me.getKey()).append(":").append(me.getValue())
                            .append(",true").append(NEWLINE);
                }
            }
            
            return sbuilder.toString();
        }
    }

    private LinkedHashMap<String, String> parseKeywordCatalogues(String s, boolean exclusive) {
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
        if (s == null || NONE.equals(s.trim()))
            return map;
        for (StringTokenizer st = new StringTokenizer(s, "\t\r\n;") ; st.hasMoreTokens() ;) {
            String entry = st.nextToken().trim();
            String[] parts = entry.split(":");
            String[] catparts = parts[1].split(",");
            String cat = parts[1];
            boolean excl = true;
            
            if (catparts.length>=3) {
                excl = Boolean.valueOf(catparts[2]);
                cat = catparts[0].trim()+","+catparts[1].trim();
            }
                    
            if (excl==exclusive)
            {
                map.put(parts[0], cat);
            }
        }

        return map;
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    public void updateModalities() {
        log.info("Update Modality List");
        StudyListLocal dao = (StudyListLocal) JNDIUtils
                .lookup(StudyListLocal.JNDI_NAME);
        String mods = listAsString(dao.selectDistinctModalities(), "|");
        Attribute attribute = new Attribute("Modalities", mods);
        try {
            server.setAttribute(getServiceName(), attribute);
        } catch (Exception x) {
            log.error(
                    "Update Modalities failed! Modality list is only valid for service lifetime: "
                            + mods, x);
            this.setModalities(mods);
        }
    }

    public void updateStationNames() {
        log.info("Update Station Name List");
        ModalityWorklistLocal dao = (ModalityWorklistLocal) JNDIUtils
                .lookup(ModalityWorklistLocal.JNDI_NAME);
        String names = listAsString(dao.selectDistinctStationNames(), "|");
        Attribute attribute = new Attribute("StationNames", names);
        try {
            server.setAttribute(getServiceName(), attribute);
        } catch (Exception x) {
            log.error(
                    "Update StationNames failed! List of Station Names is only valid for service lifetime: "
                            + names, x);
            this.setStationNames(names);
        }
    }

    public void updateDicomRoles() {
    	try {
    		log.info("Update Dicom Roles");
    		((StudyPermissionsLocal) JNDIUtils.lookup(StudyPermissionsLocal.JNDI_NAME))
    			.updateDicomRoles();   		
        } catch (Exception x) {
            log.error("Update Dicom Roles failed", x);
        }
    }

    private Date nextMidnight() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        return cal.getTime();
    }

    public void handleNotification(Notification notification, Object handback) {
        Integer id = ((TimerNotification) notification).getNotificationID();
        if (id.equals(this.autoUpdateTimerId)) {
            new Thread(new Runnable() {
                public void run() {
                    if (isAutoUpdateModalities())
                        updateModalities();
                    if (isAutoUpdateStationNames())
                        updateStationNames();
                    if (isAutoUpdateDicomRoles())
                        updateDicomRoles();
                }
            }).start();
        } else if (id.equals(this.trashCleanerTimerId))
            ((TrashCleanerLocal) JNDIUtils.lookup(TrashCleanerLocal.JNDI_NAME))
                    .clearTrash(new Date(new Date().getTime()
                            - RetryIntervalls
                                    .parseIntervalOrNever(retentionTime)));
    }

    public boolean isNotificationEnabled(Notification notification) {
        if (notification instanceof TimerNotification)
            return true;
        return false;
    }

    private void updateAutoUpdateTimer() {
		try {
            if (server == null)
                return;
            if (autoUpdateModalities || autoUpdateStationNames || autoUpdateDicomRoles) {
                if (autoUpdateTimerId == null) {
                    log.info("Start AutoUpdate Scheduler with period of 24h at 23:59:59");
                    autoUpdateTimerId = (Integer) server
                            .invoke(timerServiceName,
                                    "addNotification",
                                    new Object[] { "Schedule",
                                            "Scheduler Notification", null,
                                            nextMidnight(),
                                            new Long(ONE_DAY_IN_MILLIS) },
                                    new String[] { String.class.getName(),
                                            String.class.getName(),
                                            Object.class.getName(),
                                            Date.class.getName(),
                                            Long.TYPE.getName() });
                    addNotificationListener();
                }
            } else if (autoUpdateTimerId != null) {
                removeNotification(autoUpdateTimerId);
                autoUpdateTimerId = null;
            }
        } catch (Exception x) {
            log.error("Start AutoUpdate Scheduler failed!", x);
        }
    }

    private void configureTrashCleaner() {
        try {
            if (server == null)
                return;
            long timespan = RetryIntervalls.parseIntervalOrNever(retentionTime);
            long interval = RetryIntervalls
                    .parseIntervalOrNever(emptyTrashInterval);
            if (timespan > 0L && interval > 0L) {
                if (trashCleanerTimerId == null) {
                    trashCleanerTimerId = (Integer) server
                            .invoke(timerServiceName,
                                    "addNotification",
                                    new Object[] { "TrashCleaner",
                                            "TrashCleaner Notification", null,
                                            new Date(), interval },
                                    new String[] { String.class.getName(),
                                            String.class.getName(),
                                            Object.class.getName(),
                                            Date.class.getName(),
                                            Long.TYPE.getName() });
                    addNotificationListener();
                }
            } else if (trashCleanerTimerId != null) {
                removeNotification(trashCleanerTimerId);
                trashCleanerTimerId = null;
            }
        } catch (Exception x) {
            log.error("Start TrashCleaner Scheduler failed!", x);
        }
    }

    private void addNotificationListener() throws InstanceNotFoundException {
        if (hasNotificationListener == 0)
            server.addNotificationListener(timerServiceName, this, this, null);
        hasNotificationListener++;
    }

    private void removeNotification(Integer id) {
        hasNotificationListener--;
        try {
            log.info("Stop scheduled notification with id " + id);
            if (hasNotificationListener == 0)
                server.removeNotificationListener(timerServiceName, this);
            server.invoke(timerServiceName, "removeNotification",
                    new Object[] { id },
                    new String[] { Integer.class.getName() });
            autoUpdateTimerId = null;
        } catch (Exception e) {
            log.error("operation failed", e);
        }
    }

    @Override
    public void stopService() {
        if (autoUpdateTimerId != null)
            removeNotification(autoUpdateTimerId);
        if (trashCleanerTimerId != null)
            removeNotification(trashCleanerTimerId);
    }

    public String getUserMgtUserRole() {
        return System.getProperty("dcm4chee-usr.cfg.userrole", NONE);
    }

    public void setUserMgtUserRole(String name) {
        if (NONE.equals(name)) {
            System.getProperties().remove("dcm4chee-usr.cfg.userrole");
        } else {
            System.setProperty("dcm4chee-usr.cfg.userrole", name);
        }
    }

    public String getUserMgtAdminRole() {
        return System.getProperty("dcm4chee-usr.cfg.adminrole", NONE);
    }

    public void setUserMgtAdminRole(String name) {
        if (NONE.equals(name)) {
            System.getProperties().remove("dcm4chee-usr.cfg.adminrole");
        } else {
            System.setProperty("dcm4chee-usr.cfg.adminrole", name);
        }
    }

    public void setManageStudyPermissions(boolean manageStudyPermissions) {
        this.manageStudyPermissions = manageStudyPermissions;
    }

    public boolean isManageStudyPermissions() {
        return manageStudyPermissions;
    }

    public void setUseStudyPermissions(boolean useStudyPermissions) {
        this.useStudyPermissions = useStudyPermissions;
    }

    public boolean isUseStudyPermissions() {
        return useStudyPermissions;
    }

    public void setManageUsers(boolean manageUsers) {
        this.manageUsers = manageUsers;
    }

    public boolean isManageUsers() {
        return manageUsers;
    }

    public void setTooOldLimit(String tooOldLimit) {
        this.tooOldLimit = tooOldLimit;
    }

    public String getTooOldLimit() {
        return tooOldLimit;
    }

    public String getIgnoreEditTimeLimitRolename() {
        return ignoreEditTimeLimitRolename;
    }

    public void setIgnoreEditTimeLimitRolename(
            String ignoreEditTimeLimitRolename) {
        this.ignoreEditTimeLimitRolename = ignoreEditTimeLimitRolename;
    }

    public void setRetentionTime(String retentionTime) {
        this.retentionTime = retentionTime;
        if (emptyTrashInterval != null)
            configureTrashCleaner();
    }

    public String getRetentionTime() {
        return retentionTime;
    }

    public void setEmptyTrashInterval(String emptyTrashInterval) {
        this.emptyTrashInterval = emptyTrashInterval;
        if (retentionTime != null)
            configureTrashCleaner();
    }

    public String getEmptyTrashInterval() {
        return emptyTrashInterval;
    }

    public String getWebConfigPath() {
        return System.getProperty("dcm4chee-web3.cfg.path", NONE);
    }

    public void setWebConfigPath(String webConfigPath) {
        if (NONE.equals(webConfigPath)) {
            System.getProperties().remove("dcm4chee-web3.cfg.path");
        } else {
            String old = System.getProperty("dcm4chee-web3.cfg.path");
            if (!webConfigPath.endsWith("/"))
                webConfigPath += "/";
            System.setProperty("dcm4chee-web3.cfg.path", webConfigPath);
            if (old == null) {
                initDefaultRolesFile();
            }
        }
    }

    public void setTrustPatientIdWithoutIssuer(boolean trustPatientIdWithoutIssuer) {
        this.trustPatientIdWithoutIssuer = trustPatientIdWithoutIssuer;
    }

    public boolean isTrustPatientIdWithoutIssuer() {
        return trustPatientIdWithoutIssuer;
    }

    protected void initDefaultRolesFile() {
        String webConfigPath = System.getProperty("dcm4chee-web3.cfg.path",
                "conf/dcm4chee-web3");
        File mappingFile = new File(webConfigPath + "roles.json");
        if (!mappingFile.isAbsolute())
            mappingFile = new File(ServerConfigLocator.locate()
                    .getServerHomeDir(), mappingFile.getPath());
        if (mappingFile.exists())
            return;
        log.info("Init default Role Mapping file! mappingFile:" + mappingFile);
        if (mappingFile.getParentFile().mkdirs())
            log.info("M-WRITE dir:" + mappingFile.getParent());
        FileChannel fos = null;
        InputStream is = null;
        try {
            URL url = getClass().getResource("/META-INF/roles-default.json");
            log.info("Use default Mapping File content of url:" + url);
            is = url.openStream();
            ReadableByteChannel inCh = Channels.newChannel(is);
            fos = new FileOutputStream(mappingFile).getChannel();
            int pos = 0;
            while (is.available() > 0)
                pos += fos.transferFrom(inCh, pos, is.available());
        } catch (Exception e) {
            log.error(
                    "Roles file doesn't exist and the default can't be created!",
                    e);
        } finally {
            close(is);
            close(fos);
        }
    }

    protected void close(Closeable toClose) {
        if (toClose != null) {
            try {
                toClose.close();
            } catch (IOException ignore) {
                log.debug("Error closing : " + toClose.getClass().getName(),
                        ignore);
            }
        }
    }

    public String getWindowSizeConfig() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, int[]> e : windowsizeMap.entrySet()) {
            sb.append(e.getKey()).append(':').append(e.getValue()[0])
                    .append('x').append(e.getValue()[1]).append(NEWLINE);
        }
        return sb.toString();
    }

    public void setWindowSizeConfig(String s) {
        windowsizeMap.clear();
        StringTokenizer st = new StringTokenizer(s, " \t\r\n;");
        String t;
        int pos;
        while (st.hasMoreTokens()) {
            t = st.nextToken();
            if ((pos = t.indexOf(':')) == -1) {
                throw new IllegalArgumentException(
                        "Format must be:<name>:<width>x<height>! " + t);
            } else {
                windowsizeMap.put(t.substring(0, pos),
                        parseSize(t.substring(++pos)));
            }
        }
    }

    public int[] getWindowSize(String name) {
        int[] size = windowsizeMap.get(name);
        if (size == null)
            size = windowsizeMap.get("default");
        if (size == null) {
            log.warn("No default window size is configured! use 800x600 as default!");
            return new int[] { 800, 600 };
        }
        return size;
    }

    public String getZipEntryTemplate() {
        return zipEntryTemplate;
    }

    public void setZipEntryTemplate(String zipPathTemplate) {
        if (zipPathTemplate.indexOf("sopIuid") == -1) 
            throw new IllegalArgumentException("Missing template parameter: sopIuid");
        this.zipEntryTemplate = zipPathTemplate;
    }
    
    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    protected int[] parseSize(String s) {
        int pos = s.indexOf('x');
        if (pos == -1)
            throw new IllegalArgumentException(
                    "Windowsize must be <width>x<height>! " + s);
        return new int[] { Integer.parseInt(s.substring(0, pos).trim()),
                Integer.parseInt(s.substring(++pos).trim()) };
    }

    public String getModalityFilter() {
    	return listAsString(modalityFilter, "|");
    }

    public void setModalityFilter(String s) {
        updateList(modalityFilter, s, "|");
    }
    
    public List<String> getModalityFilterList() {
    	return modalityFilter;
    }
    
    public int getSearchWarningThreshold() {
        return searchWarningThreshold;
    }

    public void setSearchWarningThreshold(int size) {
        this.searchWarningThreshold = size;
    }
    
    public boolean isReadingRoom() {
        return Boolean.parseBoolean(System.getProperty(SYSTEM_PROPERTY_READINGROOM));
    }
    public void setReadingRoom(boolean b) {
        System.setProperty(SYSTEM_PROPERTY_READINGROOM, Boolean.toString(b));
    }
}
