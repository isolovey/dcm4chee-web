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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4chee.archive.common.Availability;
import org.dcm4chee.archive.common.StorageStatus;
import org.dcm4chee.archive.entity.File;
import org.dcm4chee.archive.entity.Instance;
import org.dcm4chee.archive.entity.MPPS;
import org.dcm4chee.archive.entity.Patient;
import org.dcm4chee.archive.entity.Series;
import org.dcm4chee.archive.entity.Study;
import org.dcm4chee.archive.entity.StudyPermission;
import org.dcm4chee.web.dao.util.QueryUtil;
import org.dcm4chee.web.dao.util.UpdateDerivedFieldsUtil;
import org.jboss.annotation.ejb.LocalBinding;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision$ $Date$
 * @since Dec 17, 2008
 */
@Stateless
@LocalBinding(jndiBinding=StudyListLocal.JNDI_NAME)
public class StudyListBean implements StudyListLocal {

    private static Comparator<Instance> instanceComparator = new Comparator<Instance>() {
        public int compare(Instance o1, Instance o2) {
            String in1 = o1.getInstanceNumber();
            String in2 = o2.getInstanceNumber();
            return QueryUtil.compareIntegerStringAndPk(o1.getPk(), o2.getPk(), in1, in2);
        }
    };
    private static Comparator<Series> seriesComparator = new Comparator<Series>() {
        public int compare(Series o1, Series o2) {
            String in1 = o1.getSeriesNumber();
            String in2 = o2.getSeriesNumber();
            return QueryUtil.compareIntegerStringAndPk(o1.getPk(), o2.getPk(), in1, in2);
        }

    };

    @PersistenceContext(unitName="dcm4chee-arc")
    private EntityManager em;
    
    public int count(StudyListFilter filter, List<String> roles) {
        if ((roles != null) && (roles.size() == 0)) return 0;
        StringBuilder ql = new StringBuilder(64);
        ql.append("SELECT COUNT(DISTINCT ")
        .append(filter.isPatientQuery() ? "p)" : "s)");
        appendFromClause(ql, filter);
        appendWhereClause(ql, filter, roles);
        Query query = em.createQuery(ql.toString());
        setQueryParameters(query, filter, roles);
        return ((Number) query.getSingleResult()).intValue();
    }

    @SuppressWarnings("unchecked")
    public List<Patient> findPatients(StudyListFilter filter, int max, int index, List<String> roles) {
        if ((roles != null) && (roles.size() == 0)) return new ArrayList<Patient>();
        StringBuilder ql = new StringBuilder(64);
        ql.append("SELECT p");
        if (!filter.isPatientQuery())
            ql.append(", s");
        appendFromClause(ql, filter);
        appendWhereClause(ql, filter, roles);
        String studyDT = filter.isPatientQuery() ? null : 
            filter.isLatestStudiesFirst() ? "s.studyDateTime DESC" : "s.studyDateTime";
        QueryUtil.appendOrderBy(ql, new String[]{"p.patientName, p.patientID, p.issuerOfPatientID", studyDT});
        Query query = em.createQuery(ql.toString());
        setQueryParameters(query, filter, roles);
        List<Patient> patientList;
        if (filter.isPatientQuery()) {
           patientList = (List<Patient>)query.setMaxResults(max).setFirstResult(index).getResultList();
           for (Patient p : patientList) {
               p.setModalityPerformedProcedureSteps(new HashSet<MPPS>());
           }
        } else {
            List<Object[]> result = query.setMaxResults(max).setFirstResult(index).getResultList();
            patientList = new ArrayList<Patient>();
            Patient patient = null;
            for (Object[] element: result) {
                patient = (Patient) element[0];
                if (!patientList.contains(patient)) {
                    patient.setStudies(new LinkedHashSet<Study>());
                    patient.setModalityPerformedProcedureSteps(null);
                    patientList.add(patient);
                }
                patient.getStudies().add((Study) element[1]);
            }
        }
        return patientList;
    }
    
    public MPPS findMPPS(String mppsUID) {
        Query query = em.createQuery("SELECT m FROM MPPS m LEFT JOIN FETCH m.patient WHERE m.sopInstanceUID = :uid");
        query.setParameter("uid", mppsUID);
        try {
            return (MPPS)query.getSingleResult();
        } catch (NoResultException x) {
            return null;
        }
    }

    public boolean hasUnlinkedSeries(long studyPk, List<String> modalityFilter) {
    	StringBuilder sb = new StringBuilder("SELECT COUNT(s) FROM Series s WHERE s.study.pk = :pk AND ");
    	if (modalityFilter != null & modalityFilter.size() > 0) {
    		sb.append("s.modality NOT IN (:modalityFilter) AND ");
    	}
    	int mark = sb.length();
        Query query = em.createQuery(sb.append("s.modalityPerformedProcedureStep IS NULL").toString());
    	if (modalityFilter != null & modalityFilter.size() > 0) {
            query.setParameter("modalityFilter", modalityFilter);
    	}
        query.setParameter("pk", studyPk);
        int count = ((Number) query.getSingleResult()).intValue();
        if (count == 0) {
        	sb.setLength(mark);
            query = em.createQuery(sb.append("s.modalityPerformedProcedureStep.accessionNumber IS NULL").toString());
        	if (modalityFilter != null & modalityFilter.size() > 0) {
                query.setParameter("modalityFilter", modalityFilter);
        	}
            query.setParameter("pk", studyPk);
            count = ((Number) query.getSingleResult()).intValue();
        }
        return count > 0;
    }

    public int countUnconnectedMPPS(StudyListFilter filter) {
        StringBuilder ql = new StringBuilder(64);
        ql.append("SELECT COUNT(*) FROM MPPS m WHERE NOT EXISTS (SELECT s.pk FROM Series s WHERE s.modalityPerformedProcedureStep = m)");
        appendPatFilter(ql, "m.patient", filter);
        Query query = em.createQuery(ql.toString());
        setPatQueryParameters(query, filter);
        return ((Number) query.getSingleResult()).intValue();
    }
    public List<Patient> findUnconnectedMPPS(StudyListFilter filter, int max, int index) {
        ArrayList<Patient> patList = new ArrayList<Patient>();
        StringBuilder ql = new StringBuilder(64);
        ql.append("SELECT m, m.patient FROM MPPS m WHERE NOT EXISTS ")
        .append("(SELECT s.pk FROM Series s WHERE s.modalityPerformedProcedureStep = m)");
        appendPatFilter(ql, "m.patient", filter);
        ql.append(" ORDER BY m.patient.patientName");
        Query query = em.createQuery(ql.toString());
        setPatQueryParameters(query, filter);
        @SuppressWarnings("unchecked")
        List<Object[]> result = query.setMaxResults(max).setFirstResult(index).getResultList();
        MPPS mpps;
        Patient patient;
        for (Object[] entry: result) {
            mpps = (MPPS)entry[0];
            patient = (Patient)entry[1];
            if (!patList.contains(patient)) {
                patient.setModalityPerformedProcedureSteps(new LinkedHashSet<MPPS>());
                patList.add(patient);
            }
            patient.getModalityPerformedProcedureSteps().add(mpps);
        }
        return patList;
    }    
    @SuppressWarnings("unchecked")
    public List<MPPS> findUnconnectedMPPSofPatient(long patPk) {
        StringBuilder ql = new StringBuilder(64);
        ql.append("SELECT m FROM MPPS m WHERE m.patient.pk = :pk AND ")
        .append("NOT EXISTS (SELECT s.pk FROM Series s WHERE s.modalityPerformedProcedureStep = m)");
        Query query = em.createQuery(ql.toString());
        query.setParameter("pk", patPk);
        return (List<MPPS>) query.getResultList();
    }    

    private static void appendFromClause(StringBuilder ql, StudyListFilter filter) {
        ql.append(" FROM Patient p");
        if (!filter.isPatientQuery() ) {
            ql.append(" INNER JOIN p.studies s");
            if ( filter.isExtendedQuery() && !QueryUtil.isUniversalMatch(filter.getStudyInstanceUID()) && filter.isRequestStudyIUID()) {
                ql.append(", IN(s.series) series");
            }
        }
    }
    
    private void appendWhereClause(StringBuilder ql, StudyListFilter filter, List<String> roles) {
        ql.append(" WHERE p.mergedWith IS NULL");
        if ( filter.isPatientQuery()) {
            appendPatFilter(ql, "p", filter);
        } else {
            if ( filter.isExtendedQuery() && !QueryUtil.isUniversalMatch(filter.getStudyInstanceUID()) ) {
                ql.append(" AND s.studyInstanceUID = :studyInstanceUID");
                if (filter.isRequestStudyIUID()) {
                	ql.append(" OR EXISTS (SELECT rq FROM RequestAttributes rq WHERE rq.series = series AND rq.studyInstanceUID = :studyInstanceUID)");
                }
            } else if (filter.isExtendedQuery() && !QueryUtil.isUniversalMatch(filter.getSeriesInstanceUID())) {
                QueryUtil.appendSeriesInstanceUIDFilter(ql, filter.getSeriesInstanceUID());
            } else {
                appendPatFilter(ql, "p", filter);
                QueryUtil.appendAccessionNumberFilter(ql, QueryUtil.checkAutoWildcard(filter.getAccessionNumber(), filter.isAutoWildcard()));
                QueryUtil.appendPpsWithoutMwlFilter(ql, filter.isWithoutPps(), filter.isPpsWithoutMwl(), !filter.getModalityFilter().isEmpty());
                QueryUtil.appendStudyDateMinFilter(ql, filter.getStudyDateMin());
                QueryUtil.appendStudyDateMaxFilter(ql, filter.getStudyDateMax());
                if (filter.isExactModalitiesInStudy()) {
                    QueryUtil.appendModalitiesInStudyExactFilter(ql, filter.getModality());
                } else {
                    QueryUtil.appendModalityFilter(ql, filter.getModality());
                }
                QueryUtil.appendSourceAETFilter(ql, filter.getSourceAETs());
            }
            if ((roles != null) && !filter.isPatientQuery())
                QueryUtil.appendDicomSecurityFilter(ql);
        }
    }

    private static void appendPatFilter(StringBuilder ql, String alias, StudyListFilter filter) {
        if (filter.isFuzzyPN()) {
            QueryUtil.appendPatientNameFuzzyFilter(ql, alias, filter.getPatientName());
        } else {
            QueryUtil.appendPatientNameFilter(ql, alias, QueryUtil.checkAutoWildcard(filter.getPatientName(), filter.isPNAutoWildcard()));
        }
        QueryUtil.appendPatientIDFilter(ql, alias, QueryUtil.checkAutoWildcard(filter.getPatientID(), filter.isAutoWildcard()));
        QueryUtil.appendIssuerOfPatientIDFilter(ql, alias, QueryUtil.checkAutoWildcard(filter.getIssuerOfPatientID(), filter.isAutoWildcard()));
        if ( filter.isExtendedQuery()) {
            QueryUtil.appendPatientBirthDateFilter(ql, alias, filter.getBirthDateMin(), filter.getBirthDateMax());
        }
    }

    private static void setQueryParameters(Query query, StudyListFilter filter, List<String> roles) {
        if ( filter.isPatientQuery()) {
            setPatQueryParameters(query, filter);
        } else {
            if ( filter.isExtendedQuery() && !QueryUtil.isUniversalMatch(filter.getStudyInstanceUID())) {
                QueryUtil.setStudyInstanceUIDQueryParameter(query, filter.getStudyInstanceUID());
            } else if (filter.isExtendedQuery() && !QueryUtil.isUniversalMatch(filter.getSeriesInstanceUID())) {
                QueryUtil.setSeriesInstanceUIDQueryParameter(query, filter.getSeriesInstanceUID());
            } else {
                setPatQueryParameters(query, filter);
                QueryUtil.setAccessionNumberQueryParameter(query, QueryUtil.checkAutoWildcard(filter.getAccessionNumber(), filter.isAutoWildcard()));
                QueryUtil.setStudyDateMinQueryParameter(query, filter.getStudyDateMin());
                QueryUtil.setStudyDateMaxQueryParameter(query, filter.getStudyDateMax());
                QueryUtil.setModalityQueryParameter(query, filter.getModality());
                QueryUtil.setSourceAETQueryParameter(query, filter.getSourceAETs());
            }
            if ((roles != null) && !filter.isPatientQuery())
                query.setParameter("roles", roles);
            if (!QueryUtil.isUniversalMatch(filter.getModality()) && filter.isExactModalitiesInStudy()) 
                query.setParameter("modality", filter.getModality());
            if (!filter.getModalityFilter().isEmpty() && (filter.isWithoutPps() || filter.isPpsWithoutMwl())) {
            	query.setParameter("modalityFilter", filter.getModalityFilter());
            }
        }
    }

    private static void setPatQueryParameters(Query query, StudyListFilter filter) {
        if (filter.isFuzzyPN()) {
            QueryUtil.setPatientNameFuzzyQueryParameter(query, filter.getPatientName());
        } else {
            QueryUtil.setPatientNameQueryParameter(query, QueryUtil.checkAutoWildcard(filter.getPatientName(), filter.isPNAutoWildcard()));
        }
        QueryUtil.setPatientIDQueryParameter(query, QueryUtil.checkAutoWildcard(filter.getPatientID(), filter.isAutoWildcard()));
        QueryUtil.setIssuerOfPatientIDQueryParameter(query, QueryUtil.checkAutoWildcard(filter.getIssuerOfPatientID(), filter.isAutoWildcard()));
        if ( filter.isExtendedQuery()) {
            QueryUtil.setPatientBirthDateQueryParameter(query, filter.getBirthDateMin(), filter.getBirthDateMax());
        }
    }

    public int countStudiesOfPatient(long pk, List<String> roles) {
        if ((roles != null) && (roles.size() == 0)) return 0;
        return ((Number) getStudiesOfPatientQuery(true, pk, false, roles).getSingleResult()).intValue();
    }
    
    @SuppressWarnings("unchecked")
    public List<Study> findStudiesOfPatient(long pk, boolean latestStudyFirst, List<String> roles) {
        if ((roles != null) && (roles.size() == 0)) return new ArrayList<Study>();
        return getStudiesOfPatientQuery(false, pk, latestStudyFirst, roles).getResultList();
    }
        
    private Query getStudiesOfPatientQuery(boolean isCount, long pk, boolean latestStudyFirst, List<String> roles) {
        StringBuilder ql = new StringBuilder(64);
        ql.append("SELECT " + (isCount ? "COUNT(s)" : "s") + " FROM Study s WHERE s.patient.pk=?1");
        if (roles != null)
            QueryUtil.appendDicomSecurityFilter(ql);
        if (!isCount)
            ql.append(latestStudyFirst
                  ? " ORDER BY s.studyDateTime DESC"
                  : " ORDER BY s.studyDateTime");
        Query query = em.createQuery(ql.toString());
        query.setParameter(1, pk);
        if (roles != null)
            query.setParameter("roles", roles);
        return query;
    }

    public boolean isActionForAllStudiesOfPatientAllowed(long patPk, String action, List<String> roles) {
        if (roles == null)
            return true;
        if (roles.isEmpty())
            return false;
        StringBuilder ql = new StringBuilder(64);
        ql.append("SELECT COUNT(s) FROM Study s WHERE s.patient.pk = ?1")
        .append(" AND (s.studyInstanceUID NOT IN (SELECT sp.studyInstanceUID FROM StudyPermission sp WHERE sp.action = ?2 AND sp.role IN (:roles)))");
        Query query = em.createQuery(ql.toString());
        query.setParameter(1, patPk);
        query.setParameter(2, action);
        query.setParameter("roles", roles);
        return (((Number) query.getSingleResult()).intValue() == 0);
    }
    
    @SuppressWarnings("unchecked")
    public List<String> findStudyPermissionActions(String studyInstanceUID, List<String> roles) {
        if(roles == null) return null;
        return ((roles != null) && (roles.size()) > 0) ? 
                em.createQuery("SELECT DISTINCT sp.action FROM StudyPermission sp WHERE sp.studyInstanceUID = :studyInstanceUID AND role IN (:roles)")
                    .setParameter("studyInstanceUID", studyInstanceUID)
                    .setParameter("roles", roles)
                    .getResultList()
                : new ArrayList<String>();
    }
    
    @SuppressWarnings("unchecked")
    public List<Series> findSeriesOfStudy(long pk) {
        return sortSeries(em.createQuery("FROM Series s LEFT JOIN FETCH s.modalityPerformedProcedureStep WHERE s.study.pk=?1 ORDER BY s.seriesNumber, s.pk")
                .setParameter(1, pk)
                .getResultList());
    }
    
    @SuppressWarnings("unchecked")
    public List<Series> findSeriesOfStudyWithoutPPS(long pk) {
        return sortSeries(em.createQuery("FROM Series s WHERE s.study.pk=?1 AND s.modalityPerformedProcedureStep IS NULL ORDER BY s.seriesNumber, s.pk")
                .setParameter(1, pk)
                .getResultList());
    }
    public int countSeriesOfStudy(long pk) {
        return ((Number) em.createQuery("SELECT COUNT(s) FROM Series s WHERE s.study.pk=?1")
                .setParameter(1, pk)
                .getSingleResult()).intValue();
    }
    
    public Series findSeriesByIuid(String iuid) {
        Query q = em.createQuery("FROM Series s LEFT JOIN FETCH s.modalityPerformedProcedureStep WHERE s.seriesInstanceUID = :iuid");
        q.setParameter("iuid", iuid);
        return (Series) q.getSingleResult();
    }

    @SuppressWarnings("unchecked")
    public List<Series> findSeriesOfMpps(String uid) {
        return sortSeries(em.createQuery("FROM Series s WHERE s.performedProcedureStepInstanceUID=?1 ORDER BY s.pk")
                .setParameter(1, uid)
                .getResultList());
    }

    private List<Series> sortSeries(List<Series> l) {
        Collections.sort(l, seriesComparator);
        return l;
    }

    @SuppressWarnings("unchecked")
    public List<Instance> findInstancesOfSeries(long pk) {
        List<Instance> l = em.createQuery("FROM Instance i LEFT JOIN FETCH i.media WHERE i.series.pk=?1 ORDER BY i.pk")
                .setParameter(1, pk)
                .getResultList();
        Collections.sort(l, instanceComparator);
        return l;
    }

    public int countInstancesOfSeries(long pk) {
        return ((Number) em.createQuery("SELECT COUNT(i) FROM Instance i WHERE i.series.pk=?1")
                .setParameter(1, pk)
                .getSingleResult()).intValue();
    }
    
    @SuppressWarnings("unchecked")
    public List<File> findFilesOfInstance(long pk) {
        return em.createQuery("FROM File f JOIN FETCH f.fileSystem WHERE f.instance.pk=?1 ORDER BY f.pk")
                .setParameter(1, pk)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<String> selectDistinctSourceAETs() {
        return em.createQuery("SELECT DISTINCT s.sourceAET FROM Series s WHERE s.sourceAET IS NOT NULL ORDER BY s.sourceAET")
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<String> selectDistinctModalities() {
        return em.createQuery("SELECT DISTINCT s.modality FROM Series s WHERE s.modality IS NOT NULL ORDER BY s.modality")
                .getResultList();
    }

    public Patient getPatient(long pk) {
        return em.find(Patient.class, pk);
    }

    public Patient updatePatient(long pk, DicomObject attrs) {
        Patient patient;
        String issuer = attrs.getString(Tag.IssuerOfPatientID);
        StringBuilder sb = new StringBuilder("SELECT p FROM Patient p WHERE p.patientID=?1");
        if (issuer == null || issuer.length() == 0) {
            sb.append(" AND p.issuerOfPatientID IS NULL");
        } else {
            sb.append(" AND p.issuerOfPatientID=?2");
        }
        Query q = em.createQuery(sb.toString()).setParameter(1, attrs.getString(Tag.PatientID));
        if (issuer != null)
            q.setParameter(2, issuer);

        if (pk == -1) {
            if (q.getResultList().size() > 0)
                return null;
            patient = new Patient();
            patient.setAttributes(attrs);
            em.persist(patient);
        } else {
            @SuppressWarnings("unchecked")
            List<Patient> pats = q.getResultList();
            if (pats.size() > 1 || (pats.size() > 0 && pats.get(0).getPk() != pk)) {
                return null;
            }
            patient = em.find(Patient.class, pk);
            patient.setAttributes(attrs);
        }
        return patient;
    }

    public Study getStudy(long pk) {
        return em.find(Study.class, pk);
    }

    public Study updateStudy(long pk, DicomObject attrs) {
        Study study = em.find(Study.class, pk);
        study.setAttributes(attrs);
        return study;
    }

    public Study addStudy(long patPk, DicomObject attrs) {
        Patient pat = em.find(Patient.class, patPk);
        Study study = new Study();
        study.setAttributes(attrs);
        study.setPatient(pat);
        study.setAvailability(Availability.ONLINE);
        em.persist(study);
        return study;
    }

    public void copyStudyPermissions(String srcStudyIuid, String destStudyIuid) {
        Query query = em.createQuery("SELECT sp FROM StudyPermission sp WHERE studyInstanceUID=?1");
        query.setParameter(1, srcStudyIuid);
        @SuppressWarnings("unchecked")
        List<StudyPermission> l = (List<StudyPermission>)query.getResultList();
        StudyPermission studyPermission;
        for (StudyPermission sp : l) {
            studyPermission = new StudyPermission();
            studyPermission.setAction(sp.getAction());
            studyPermission.setRole(sp.getRole());
            studyPermission.setStudyInstanceUID(destStudyIuid);
            em.persist(studyPermission);
        }
    }

    public Series getSeries(long pk) {
        return em.find(Series.class, pk);
    }

    public Series updateSeries(long pk, DicomObject attrs) {
        Series series = em.find(Series.class, pk);
        series.setAttributes(attrs);
        new UpdateDerivedFieldsUtil(em).updateDerivedFieldsOfStudy(series.getStudy());
        return series;
    }
    public Series addSeries(long studyPk, DicomObject attrs) {
        Study study = em.find(Study.class, studyPk);
        Series series = new Series();
        series.setAttributes(attrs);
        series.setStudy(study);
        series.setAvailability(Availability.ONLINE);
        series.setNumberOfSeriesRelatedInstances(0);
        series.setStorageStatus(StorageStatus.STORED);
        study.setNumberOfStudyRelatedSeries(study.getNumberOfStudyRelatedSeries()+1);
        em.persist(series);
        em.persist(study);
        return series;
    }

    public Instance getInstance(long pk) {
        return em.find(Instance.class, pk);
    }
    
    public Instance updateInstance(long pk, DicomObject attrs) {
        Instance inst = em.find(Instance.class, pk);
        inst.setAttributes(attrs);
        UpdateDerivedFieldsUtil util = new UpdateDerivedFieldsUtil(em);
        util.updateDerivedFieldsOfSeries(inst.getSeries());
        util.updateDerivedFieldsOfStudy(inst.getSeries().getStudy());
        return inst;
    }

    public MPPS getMPPS(long pk) {
        return em.find(MPPS.class, pk);
    }

    public MPPS updateMPPS(long pk, DicomObject attrs) {
        MPPS mpps = em.find(MPPS.class, pk);
        mpps.setAttributes(attrs);
        return mpps;
    }

    public long countDownloadableInstances(String[] studyIuids, String[] seriesIuids, String[] sopIuids) {
        if (studyIuids == null && seriesIuids == null && sopIuids == null)
            return 0;
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT count(i) FROM Instance i WHERE ");
        String[] uids = appendUIDs(studyIuids, seriesIuids, sopIuids, sb);
        Query q = em.createQuery(sb.toString());
        QueryUtil.setParametersForIN(q, uids);
        return (Long) q.getSingleResult();
    }

    public long countDownloadableInstancesLocal(String[] studyIuids, String[] seriesIuids, String[] sopIuids) {
        if (studyIuids == null && seriesIuids == null && sopIuids == null)
            return 0;
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT count(i) FROM Instance i WHERE i.files IS NOT EMPTY AND ");
        String[] uids = appendUIDs(studyIuids, seriesIuids, sopIuids, sb);
        Query q = em.createQuery(sb.toString());
        QueryUtil.setParametersForIN(q, uids);
        return (Long) q.getSingleResult();
    }


    @SuppressWarnings("unchecked")
    public List<Instance> getDownloadableInstances(String[] studyIuids, String[] seriesIuids, String[] sopIuids) {
        List<Instance> instances;
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT i FROM Instance i JOIN FETCH i.series s JOIN FETCH s.study st JOIN FETCH st.patient WHERE ");
        String[] uids = appendUIDs(studyIuids, seriesIuids, sopIuids, sb);
        if (uids == null)
            return null;
        Query q = em.createQuery(sb.toString());
        QueryUtil.setParametersForIN(q, uids);
        instances = (List<Instance>) q.getResultList();
        for (Instance instance : instances) {
            for (File file : instance.getFiles()) 
                file.getFileSystem().getDirectoryPath();
        }       
        return instances;
    }
    
    public boolean hasStudyForeignPpsInfo(long studyPk) {
        return ((Long)em.createQuery("SELECT count(s) FROM Series s WHERE s.study.pk=?1 AND s.performedProcedureStepInstanceUID IS NOT NULL AND s.modalityPerformedProcedureStep IS NULL")
                .setParameter(1, studyPk).getSingleResult()) > 0l;
    }

    public boolean exists(long pk, int levelOfModel) {
        try {
            switch (levelOfModel) {
                case 0: return em.find(Patient.class, pk) != null;
                case 1: return em.find(Study.class, pk) != null;
                case 2: return em.find(MPPS.class, pk) != null;
                case 3: return em.find(Series.class, pk) != null;
                case 4: return em.find(Instance.class, pk) != null;
                default: return false;
            }
        } catch (Exception x) {
            return false;
        }
    }
    private String[] appendUIDs(String[] studyIuids, String[] seriesIuids,
            String[] sopIuids, StringBuilder sb) {
        String[] uids;
        if (sopIuids != null) {
            uids = sopIuids;
            sb.append("i.sopInstanceUID");
        } else if (seriesIuids != null) {
            uids = seriesIuids;
            sb.append("i.series.seriesInstanceUID");
        } else if (studyIuids != null) {
            uids = studyIuids;
            sb.append("i.series.study.studyInstanceUID");
        } else {
            return null;
        }
        QueryUtil.appendIN(sb, uids.length);
        return uids;
    }
}
