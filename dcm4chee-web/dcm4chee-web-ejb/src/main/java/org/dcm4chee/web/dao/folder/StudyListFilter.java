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

package org.dcm4chee.web.dao.folder;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DateRange;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.VR;
import org.dcm4chee.archive.conf.AttributeFilter;
import org.dcm4chee.usr.dao.UserAccess;
import org.dcm4chee.usr.model.AETGroup;
import org.dcm4chee.usr.util.JNDIUtils;
import org.dcm4chee.web.dao.util.QueryUtil;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision$ $Date$
 * @since Dec 30, 2008
 */
public class StudyListFilter implements Serializable {

    private static final long serialVersionUID = 1L;

    private String patientName;
    private String patientID;
    private String issuerOfPatientID;
    private boolean extendedQuery;
    private Date birthDateMin;
    private Date birthDateMax;
    private String accessionNumber;
    private Date studyDateMin;
    private Date studyDateMax;
    private String studyInstanceUID;
    private String modality;
    private String sourceAET;
    private String seriesInstanceUID;
    private boolean patientQuery;
    private boolean latestStudiesFirst;
    private boolean unconnectedMPPS, unconnectedMPPSSearched;
    private boolean ppsWithoutMwl, ppsWithoutMwlSearched;
    private boolean withoutPps, withoutPpsSearched;
    private boolean exactModalitiesInStudy;
    private boolean exactSeriesIuid;
    private int autoExpandLevel = -1;
    private boolean fuzzyPN;
    private int autoWildcard = 0;
    private boolean isStudyIuidQuery;
    private boolean isSeriesIuidQuery;
	private List<String> modalityFilter;

	private boolean requestStudyIUID;
    
    public StudyListFilter(String forUsername) {
        clear();
    }

    public void clear() {
        patientName = patientID = issuerOfPatientID = accessionNumber = 
            studyInstanceUID = modality = sourceAET = seriesInstanceUID = null;
        studyDateMin = studyDateMax = null; 
        birthDateMin = birthDateMax = null;
        patientQuery = false;
        latestStudiesFirst = false;
        ppsWithoutMwl = false;
        withoutPps = false;
        unconnectedMPPS = false;
        exactModalitiesInStudy = false;
        exactSeriesIuid = false;
        fuzzyPN = false;
        isStudyIuidQuery = isSeriesIuidQuery = false;
    }

    public boolean isFiltered() {
    	return  isPatientQuery()
    	        || isPpsWithoutMwl()
    	        || isWithoutPps()
    	        || isUnconnectedMPPS()
    	        || !QueryUtil.isUniversalPNMatch(getPatientName())
    		|| !QueryUtil.isUniversalMatch(getPatientID()) 
    		|| !QueryUtil.isUniversalMatch(getIssuerOfPatientID()) 
    		|| !QueryUtil.isUniversalMatch(getAccessionNumber()) 
                || !QueryUtil.isUniversalMatch(getModality())
                || !QueryUtil.isUniversalMatch(getSourceAET())
                || studyDateMin != null
                || studyDateMax != null 
                || 
                (isExtendedQuery() && (birthDateMin != null || birthDateMax != null
                || !QueryUtil.isUniversalMatch(getSeriesInstanceUID())
                || !QueryUtil.isUniversalMatch(getStudyInstanceUID())
                ));
    }
    
    public String getPatientName() {
        if (patientName != null && isFuzzyPN() && !AttributeFilter.isSoundexWithTrailingWildCardEnabled()) {
            return patientName.replace("*", "");
        }
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public boolean isFuzzyPNEnabled() {
        return AttributeFilter.isSoundexEnabled();
    }
    public boolean isFuzzyPN() {
        return fuzzyPN;
    }
    public void setFuzzyPN(boolean fuzzyPN) {
        this.fuzzyPN = fuzzyPN;
    }

    public String getPatientID() {
        return patientID;
    }

    public void setPatientID(String patientID) {
        this.patientID = patientID;
    }

    public String getIssuerOfPatientID() {
        return issuerOfPatientID;
    }

    public void setIssuerOfPatientID(String issuerOfPatientID) {
        this.issuerOfPatientID = issuerOfPatientID;
    }

    public boolean isExtendedQuery() {
        return extendedQuery;
    }

    public void setExtendedQuery(boolean extendedQuery) {
        this.extendedQuery = extendedQuery;
    }

    public Date getBirthDateMin() {
        return birthDateMin;
    }

    public void setBirthDateMin(Date birthdateMin) {
        this.birthDateMin = birthdateMin;
    }

    public Date getBirthDateMax() {
        return birthDateMax;
    }

    public void setBirthDateMax(Date birthdateMax) {
        this.birthDateMax = birthdateMax;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public boolean isWithoutPps() {
        return withoutPps;
    }

    public void setWithoutPps(boolean withoutPps) {
        this.withoutPps = withoutPps;
    }
    
    public boolean isPpsWithoutMwl() {
        return ppsWithoutMwl;
    }

    public void setPpsWithoutMwl(boolean ppsWithoutMwl) {
        this.ppsWithoutMwl = ppsWithoutMwl;
    }

    public Date getStudyDateMin() {
        return studyDateMin;
    }

    public void setStudyDateMin(Date studyDate) {
        this.studyDateMin = studyDate;
    }

    public Date getStudyDateMax() {
        return studyDateMax;
    }

    public void setStudyDateMax(Date studyDate) {
        this.studyDateMax = studyDate;
    }

    public String getStudyInstanceUID() {
        return studyInstanceUID;
    }

    public void setStudyInstanceUID(String studyInstanceUID) {
        this.studyInstanceUID = studyInstanceUID;
    }

    public boolean isRequestStudyIUID() {
        return requestStudyIUID;
    }
    public void setRequestStudyIUID(boolean b) {
        requestStudyIUID = b;
    }

    public String getModality() {
        return modality;
    }

    public void setModality(String modality) {
        this.modality = modality;
    }

    public boolean isExactModalitiesInStudy() {
        return exactModalitiesInStudy;
    }

    public void setExactModalitiesInStudy(boolean exactModalitiesInStudy) {
        this.exactModalitiesInStudy = exactModalitiesInStudy;
    }

    public boolean isExactSeriesIuid() {
        return exactSeriesIuid;
    }

    public void setExactSeriesIuid(boolean exactSeriesIuid) {
        this.exactSeriesIuid = exactSeriesIuid;
    }

    public String getSourceAET() {
        return sourceAET;
    }

    public void setSourceAET(String sourceAET) {
        this.sourceAET = sourceAET;
    }

    public String[] getSourceAETs() {
        Set<String> aetStringSet = new HashSet<String>();
        if (sourceAET != null) {
            if (sourceAET.startsWith("(") && sourceAET.endsWith(")")) {
                String groupName = sourceAET.substring(1, sourceAET.length() - 1);
                for (AETGroup aetGroup : ((UserAccess) JNDIUtils.lookup(UserAccess.JNDI_NAME)).getAllAETGroups())
                    if (aetGroup.getGroupname().equals(groupName))
                            aetStringSet.addAll(aetGroup.getAets());
            } else                
                return new String[] { sourceAET };
        }
        return aetStringSet.toArray(new String[aetStringSet.size()]);
    }
    
    public String getSeriesInstanceUID() {
        return seriesInstanceUID;
    }

    public void setSeriesInstanceUID(String seriesInstanceUID) {
        this.seriesInstanceUID = seriesInstanceUID;
    }
    
    public boolean isPatientQuery() {
        return patientQuery;
    }

    public void setPatientQuery(boolean patQuery) {
        this.patientQuery = patQuery;
    }

    public boolean isUnconnectedMPPS() {
        return unconnectedMPPS;
    }

    public void setUnconnectedMPPS(boolean unconnectedMPPS) {
        this.unconnectedMPPS = unconnectedMPPS;
    }

    public boolean isLatestStudiesFirst() {
        return latestStudiesFirst;
    }

    public void setLatestStudiesFirst(boolean latestStudiesFirst) {
        this.latestStudiesFirst = latestStudiesFirst;
    }

    public void setAutoExpandLevel(int level) {
        this.autoExpandLevel = level;
    }

    public int getAutoExpandLevel() {
        return autoExpandLevel;
    }

    public boolean isAutoWildcard() {
        return autoWildcard > 1;
    }

    public boolean isPNAutoWildcard() {
        return autoWildcard > 0;
    }
    /**
     * 0..Off
     * 1..Only Patient name
     * 2..All 'wildcard' text fields.
     * @param autoWildcard
     */
    public void setAutoWildcard(int autoWildcard) {
        this.autoWildcard = autoWildcard;
    }

    public boolean isStudyIuidQuery() {
        return isStudyIuidQuery;
    }

    public void setStudyIuidQuery(boolean b) {
        this.isStudyIuidQuery = b;
    }

    public boolean isSeriesIuidQuery() {
        return isSeriesIuidQuery;
    }

    public void setSeriesIuidQuery(boolean b) {
        this.isSeriesIuidQuery = b;
    }
    
    public boolean getUnconnectedMPPSSearched() {
        return unconnectedMPPSSearched;
    }

    public boolean getPpsWithoutMwlSearched() {
        return ppsWithoutMwlSearched;
    }

    public boolean getWithoutPpsSearched() {
        return withoutPpsSearched;
    }
    
    public void markSearchedOptions() {
        unconnectedMPPSSearched = unconnectedMPPS;
        ppsWithoutMwlSearched = ppsWithoutMwl;
        withoutPpsSearched = withoutPps;
    }

    public DicomObject getQueryDicomObject() {
        DicomObject obj = new BasicDicomObject();
        if (patientQuery) {
            obj.putString(Tag.QueryRetrieveLevel, VR.CS, "PATIENT");
            addPatientAttrs(obj);
        } else if (extendedQuery && isSeriesIuidQuery && !isStudyIuidQuery) {
            obj.putString(Tag.QueryRetrieveLevel, VR.CS, "SERIES");
            obj.putString(Tag.SeriesInstanceUID, VR.UI, seriesInstanceUID);
        } else if (extendedQuery && isStudyIuidQuery) {
            obj.putString(Tag.QueryRetrieveLevel, VR.CS, "STUDY");
            obj.putString(Tag.StudyInstanceUID, VR.UI, studyInstanceUID);
        } else {
            obj.putString(Tag.QueryRetrieveLevel, VR.CS, "STUDY");
            addPatientAttrs(obj);
            obj.putString(Tag.AccessionNumber, VR.SH, QueryUtil.checkAutoWildcard(accessionNumber, isAutoWildcard()));
            obj.putDateRange(Tag.StudyDate, VR.DA, new DateRange(studyDateMin, studyDateMax));
            obj.putDateRange(Tag.StudyTime, VR.TM, new DateRange(studyDateMin, studyDateMax));
            obj.putString(Tag.Modality, VR.CS, modality);
        }
        return obj;
    }

    private void addPatientAttrs(DicomObject obj) {
        obj.putString(Tag.PatientName, VR.PN, QueryUtil.checkAutoWildcard(patientName, isPNAutoWildcard()));
        obj.putString(Tag.PatientID, VR.LO, QueryUtil.checkAutoWildcard(patientID, isAutoWildcard()));
        obj.putString(Tag.IssuerOfPatientID, VR.LO, QueryUtil.checkAutoWildcard(issuerOfPatientID, isAutoWildcard()));
        if (extendedQuery) {
            obj.putDateRange(Tag.PatientBirthDate, VR.DA, new DateRange(birthDateMin, birthDateMax));
        }
    }

	public List<String> getModalityFilter() {
		return modalityFilter;
	}
	
	public void setModalityFilter(List<String> modalityFilter) {
		this.modalityFilter = modalityFilter;
	}
}
