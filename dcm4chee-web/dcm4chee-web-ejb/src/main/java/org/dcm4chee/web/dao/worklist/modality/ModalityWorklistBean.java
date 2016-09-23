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

package org.dcm4chee.web.dao.worklist.modality;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TemporalType;

import org.dcm4che2.data.DicomObject;
import org.dcm4chee.archive.common.SPSStatus;
import org.dcm4chee.archive.entity.MWLItem;
import org.dcm4chee.archive.entity.Study;
import org.dcm4chee.web.dao.util.QueryUtil;
import org.jboss.annotation.ejb.LocalBinding;

/**
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision$ $Date$
 * @since 20.04.2010
 */
@Stateless
@LocalBinding (jndiBinding=ModalityWorklistLocal.JNDI_NAME)
public class ModalityWorklistBean implements ModalityWorklistLocal {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

    @PersistenceContext(unitName="dcm4chee-arc")
    private EntityManager em;

    @SuppressWarnings("unchecked")
    public List<MWLItem> findAll() {
        List<MWLItem> l = em.createQuery("FROM MWLItem mwlItem ORDER BY mwlItem.studyInstanceUID")
                .getResultList();
        em.clear();
        return l;
    }
    
    public int countMWLItems(ModalityWorklistFilter filter) {
        StringBuilder ql = new StringBuilder(64);
        ql.append("SELECT COUNT(*)");
        appendFromClause(ql, filter);
        appendWhereClause(ql, filter);
        Query query = em.createQuery(ql.toString());
        setQueryParameters(query, filter);
        return ((Number) query.getSingleResult()).intValue();
    }

    @SuppressWarnings("unchecked")
    public List<MWLItem> findMWLItems(ModalityWorklistFilter filter, int max, int index) {
        StringBuilder ql = new StringBuilder(64);
        ql.append("SELECT m");
        appendFromClause(ql, filter);
        appendWhereClause(ql, filter);
        ql.append(" ORDER BY p.patientName, m.startDateTime");
        if (filter.isLatestItemsFirst()) {
            ql.append(" DESC");
        }
        Query query = em.createQuery(ql.toString());
        setQueryParameters(query, filter);
        return query.setMaxResults(max).setFirstResult(index).getResultList();
    }

    private static void appendFromClause(StringBuilder ql, ModalityWorklistFilter filter) {
        ql.append(ql.toString().startsWith("SELECT COUNT(*)") ?
            " FROM MWLItem m LEFT JOIN m.patient p " : 
            " FROM MWLItem m LEFT JOIN FETCH m.patient p "
        );
    }

    private static void appendWhereClause(StringBuilder ql, ModalityWorklistFilter filter) {
        ql.append(" WHERE p.mergedWith IS NULL");

        if (filter.isExtendedQuery() && !QueryUtil.isUniversalMatch(filter.getStudyInstanceUID())) {
            ql.append(" AND m.studyInstanceUID = :studyInstanceUID");
        } else {
            if (filter.isFuzzyPN()) {
                QueryUtil.appendPatientNameFuzzyFilter(ql, "p", filter.getPatientName());
            } else {
                QueryUtil.appendPatientNameFilter(ql, "p", QueryUtil.checkAutoWildcard(filter.getPatientName(), filter.isPNAutoWildcard()));
            }
            appendPatientIDFilter(ql, QueryUtil.checkAutoWildcard(filter.getPatientID(), filter.isAutoWildcard()));
            appendIssuerOfPatientIDFilter(ql, QueryUtil.checkAutoWildcard(filter.getIssuerOfPatientID(), filter.isAutoWildcard()));
            if (filter.isExtendedQuery()) {
                appendPatientBirthDateFilter(ql, filter.getBirthDateMin(), filter.getBirthDateMax());
            }
            appendAccessionNumberFilter(ql, QueryUtil.checkAutoWildcard(filter.getAccessionNumber(), filter.isAutoWildcard()));
            appendStartDateMinFilter(ql, filter.getStartDateMin());
            appendStartDateMaxFilter(ql, filter.getStartDateMax());
            appendModalityFilter(ql, filter.getModality());
            appendScheduledStationAETFilter(ql, filter.getScheduledStationAETs());
            appendScheduledStationNameFilter(ql, filter.getScheduledStationName());
            appendScheduledProcedureStepStatus(ql, filter.getSPSStatus());
        }        
    }
    
    private static void setQueryParameters(Query query, ModalityWorklistFilter filter) {

        if (filter.isExtendedQuery() && !QueryUtil.isUniversalMatch(filter.getStudyInstanceUID())) {
            setStudyInstanceUIDQueryParameter(query, filter.getStudyInstanceUID());
        } else {        
            if (filter.isFuzzyPN()) {
                QueryUtil.setPatientNameFuzzyQueryParameter(query, filter.getPatientName());
            } else {
                QueryUtil.setPatientNameQueryParameter(query, QueryUtil.checkAutoWildcard(filter.getPatientName(), filter.isPNAutoWildcard()));
            }
            setPatientIDQueryParameter(query, QueryUtil.checkAutoWildcard(filter.getPatientID(), filter.isAutoWildcard()));
            setIssuerOfPatientIDQueryParameter(query, QueryUtil.checkAutoWildcard(filter.getIssuerOfPatientID(), filter.isAutoWildcard()));
            if (filter.isExtendedQuery()) {
                setPatientBirthDateQueryParameter(query, filter.getBirthDateMin(), filter.getBirthDateMax());
            }
            setAccessionNumberQueryParameter(query, QueryUtil.checkAutoWildcard(filter.getAccessionNumber(), filter.isAutoWildcard()));
            setStartDateMinQueryParameter(query, filter.getStartDateMin());
            setStartDateMaxQueryParameter(query, filter.getStartDateMax());
            setModalityQueryParameter(query, filter.getModality());
            setScheduledStationAETQueryParameter(query, filter.getScheduledStationAETs());
            setScheduledStationNameQueryParameter(query, filter.getScheduledStationName());
            setScheduledProcedureStepStatusQueryParameter(query, filter.getSPSStatus());
        }
    }

    private static void appendPatientIDFilter(StringBuilder ql,
            String patientID) {
        QueryUtil.appendANDwithTextValue(ql, "p.patientID", "patientID", patientID);
    }

    private static void setPatientIDQueryParameter(Query query,
            String patientID) {
        QueryUtil.setTextQueryParameter(query, "patientID", patientID);
    }

    private static void appendIssuerOfPatientIDFilter(StringBuilder ql,
            String issuerOfPatientID) {
        QueryUtil.appendANDwithTextValue(ql, "p.issuerOfPatientID", "issuerOfPatientID", issuerOfPatientID);
    }

    private static void setIssuerOfPatientIDQueryParameter(Query query,
            String issuerOfPatientID) {
        QueryUtil.setTextQueryParameter(query, "issuerOfPatientID", issuerOfPatientID);
    }

    private static void appendPatientBirthDateFilter(StringBuilder ql, Date minDate, Date maxDate) {
        if (minDate!=null) {
            if (maxDate==null) {
                ql.append(" AND p.patientBirthDate >= :birthdateMin");
            } else {
                ql.append(" AND p.patientBirthDate BETWEEN :birthdateMin AND :birthdateMax");
                
            }
        } else if (maxDate!=null) {
            ql.append(" AND p.patientBirthDate <= :birthdateMax");
        }
    }
    private static void setPatientBirthDateQueryParameter(Query query, Date minDate, Date maxDate) {
        if ( minDate!=null)
            query.setParameter("birthdateMin", sdf.format(minDate));
        if ( maxDate!=null)
            query.setParameter("birthdateMax", sdf.format(maxDate));
    }

    private static void appendStartDateMinFilter(StringBuilder ql, Date date) {
        if (date != null) {
            ql.append(" AND m.startDateTime >= :startDateTimeMin");
        }
    }

    private static void appendStartDateMaxFilter(StringBuilder ql, Date date) {
        if (date != null) {
            ql.append(" AND m.startDateTime <= :startDateTimeMax");
        }
    }

    private static void setStartDateMinQueryParameter(Query query, Date date) {
        setStartDateQueryParameter(query, date, "startDateTimeMin");
    }

    private static void setStartDateMaxQueryParameter(Query query, Date date) {
        setStartDateQueryParameter(query, date, "startDateTimeMax");
    }

    private static void setStartDateQueryParameter(Query query,
            Date startDate, String param) {
        if (startDate != null) {
            query.setParameter(param, startDate, TemporalType.TIMESTAMP);
        }
    }

    private static void appendAccessionNumberFilter(StringBuilder ql,
            String accessionNumber) {
        QueryUtil.appendANDwithTextValue(ql, "m.accessionNumber", "accessionNumber", accessionNumber);
    }

    private static void setAccessionNumberQueryParameter(Query query,
            String accessionNumber) {
        QueryUtil.setTextQueryParameter(query, "accessionNumber", accessionNumber);
    }

    private static void setStudyInstanceUIDQueryParameter(Query query,
            String studyInstanceUID) {
        if (!QueryUtil.isUniversalMatch(studyInstanceUID)) {
            query.setParameter("studyInstanceUID", studyInstanceUID);
        }
    }

    private static void appendModalityFilter(StringBuilder ql,
            String modality) {
        if (!QueryUtil.isUniversalMatch(modality)) {
            ql.append(" AND m.modality = :modality");
        }
    }

    private static void setModalityQueryParameter(Query query,
            String modality) {
        if (!QueryUtil.isUniversalMatch(modality)) {
            query.setParameter("modality", modality);
        }
    }

    private static void appendScheduledStationAETFilter(StringBuilder ql,
            String[] scheduledStationAETs) {
        if (!QueryUtil.isUniversalMatch(scheduledStationAETs)) {
            if (scheduledStationAETs.length == 1) {
                ql.append(" AND m.scheduledStationAET = :scheduledStationAET");
            } else {
                ql.append(" AND m.scheduledStationAET");
                QueryUtil.appendIN(ql, scheduledStationAETs.length);
            }
        }
    }

    private static void setScheduledStationAETQueryParameter(Query query,
            String[] scheduledStationAETs) {
        if (!QueryUtil.isUniversalMatch(scheduledStationAETs)) {
            if (scheduledStationAETs.length == 1) {
                query.setParameter("scheduledStationAET", scheduledStationAETs[0]);
            } else {
                QueryUtil.setParametersForIN(query, scheduledStationAETs);
            }
        }
    }

    private static void appendScheduledStationNameFilter(StringBuilder ql,
            String scheduledStationName) {
        if (!QueryUtil.isUniversalMatch(scheduledStationName)) {
            ql.append(" AND m.scheduledStationName = :scheduledStationName");
        }
    }

    private static void setScheduledStationNameQueryParameter(Query query,
            String scheduledStationName) {
        if (!QueryUtil.isUniversalMatch(scheduledStationName)) {
            query.setParameter("scheduledStationName", scheduledStationName);
        }
    }

    private static void appendScheduledProcedureStepStatus(StringBuilder ql,
            String scheduledProcedureStepStatus) {
        if (!QueryUtil.isUniversalMatch(scheduledProcedureStepStatus)) {
            ql.append(" AND m.status = :scheduledProcedureStepStatus");
        }
    }

    private static void setScheduledProcedureStepStatusQueryParameter(Query query,
            String scheduledProcedureStepStatus) {        
        if (!QueryUtil.isUniversalMatch(scheduledProcedureStepStatus)) {
            query.setParameter("scheduledProcedureStepStatus", SPSStatus.valueOf(scheduledProcedureStepStatus));
        }
    }

    @SuppressWarnings("unchecked")
    public List<Study> findMWLItemsOfPatient(long pk, boolean latestStudyFirst) {
        return em.createQuery(latestStudyFirst
                    ? "FROM Study s WHERE s.patient.pk=?1 ORDER BY s.studyDateTime DESC"
                    : "FROM Study s WHERE s.patient.pk=?1 ORDER BY s.studyDateTime")
                .setParameter(1, pk)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<String> selectDistinctModalities() {
        return em.createQuery("SELECT DISTINCT m.modality FROM MWLItem m WHERE m.modality IS NOT NULL ORDER BY m.modality")
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<String> selectDistinctStationAETs() {
        return em.createQuery("SELECT DISTINCT m.scheduledStationAET FROM MWLItem m WHERE m.scheduledStationAET IS NOT NULL ORDER BY m.scheduledStationAET")
        .getResultList();
    }
    
    @SuppressWarnings("unchecked")
    public List<String> selectDistinctStationNames() {
        return em.createQuery("SELECT DISTINCT m.scheduledStationName FROM MWLItem m WHERE m.scheduledStationName IS NOT NULL ORDER BY m.scheduledStationName")
        .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<String> selectDistinctStatus() {
        return em.createQuery("SELECT DISTINCT m.status FROM MWLItem m WHERE m.status IS NOT NULL ORDER BY m.status")
                .getResultList();
    }

    public MWLItem getMWLItem(long pk) {
        return em.find(MWLItem.class, pk);
    }

    public MWLItem updateMWLItem(long pk, DicomObject attrs) {
        MWLItem mwlItem = em.find(MWLItem.class, pk);
        mwlItem.setAttributes(attrs);
        return mwlItem;
    }
    
    public boolean hasMPPS(String accessionNumber) {
        return ((Number)em.createQuery("SELECT COUNT(*) FROM MPPS mpps WHERE mpps.accessionNumber IS NOT NULL AND mpps.accessionNumber = :accessionNumber")
        .setParameter("accessionNumber", accessionNumber)
        .getSingleResult()).intValue() > 0;
    }
    
    public void removeMWLItem(long pk) {
        MWLItem mwlItem = em.find(MWLItem.class, pk);
        em.remove(mwlItem);
    }
}
