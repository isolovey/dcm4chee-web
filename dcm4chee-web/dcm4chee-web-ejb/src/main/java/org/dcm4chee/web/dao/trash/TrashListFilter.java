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

package org.dcm4chee.web.dao.trash;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.dcm4chee.usr.dao.UserAccess;
import org.dcm4chee.usr.model.AETGroup;
import org.dcm4chee.usr.util.JNDIUtils;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision$ $Date$
 * @since Dec 30, 2008
 */
public class TrashListFilter implements Serializable {

    private static final long serialVersionUID = 1L;

    private String patientName;
    private String patientID;
    private String issuerOfPatientID;
    private String accessionNumber;
    private String studyInstanceUID;
    private String sourceAET;
    private boolean patientQuery;
    private int autoWildcard = 0;
    private Date deletedDateMin;
    private Date deletedDateMax;

    public TrashListFilter(String forUsername) {
        clear();
    }

    public void clear() {
        patientName = patientID = issuerOfPatientID = accessionNumber = 
            studyInstanceUID = sourceAET = null;
        patientQuery = false;
        deletedDateMin = deletedDateMax = null;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
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

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public String getStudyInstanceUID() {
        return studyInstanceUID;
    }

    public void setStudyInstanceUID(String studyInstanceUID) {
        this.studyInstanceUID = studyInstanceUID;
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

    public boolean isPatientQuery() {
        return patientQuery;
    }

    public void setPatientQuery(boolean patQuery) {
        this.patientQuery = patQuery;
    }
    
    public boolean isAutoWildcard() {
        return autoWildcard  > 1;
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

    public Date getDeletedDateMin() {
        return deletedDateMin;
    }

    public void setDeletedDateMin(Date deletedDateMin) {
        this.deletedDateMin = deletedDateMin;
    }

    public Date getDeletedDateMax() {
        return deletedDateMax;
    }

    public void setDeletedDateMax(Date deletedDateMax) {
        this.deletedDateMax = deletedDateMax;
    }
}
