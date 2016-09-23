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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.VR;
import org.dcm4chee.archive.common.PrivateTag;
import org.dcm4chee.archive.entity.BaseEntity;
import org.dcm4chee.archive.entity.PrivateFile;
import org.dcm4chee.archive.entity.PrivateInstance;
import org.dcm4chee.archive.entity.PrivatePatient;
import org.dcm4chee.archive.entity.PrivateSeries;
import org.dcm4chee.archive.entity.PrivateStudy;
import org.dcm4chee.archive.entity.Study;
import org.dcm4chee.web.dao.util.QueryUtil;
import org.jboss.annotation.ejb.LocalBinding;

/**
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision$ $Date$
 * @since June 07, 2010
 */
@Stateless
@LocalBinding (jndiBinding=TrashListLocal.JNDI_NAME)
public class TrashListBean implements TrashListLocal {

    @PersistenceContext(unitName="dcm4chee-arc")
    private EntityManager em;

    private static Comparator<PrivateInstance> instanceComparator = new Comparator<PrivateInstance>() {
        public int compare(PrivateInstance o1, PrivateInstance o2) {
            String in1 = o1.getAttributes().getString(Tag.InstanceNumber);
            String in2 = o2.getAttributes().getString(Tag.InstanceNumber);
            return QueryUtil.compareIntegerStringAndPk(o1.getPk(), o2.getPk(), in1, in2);
        }

    };

    private static Comparator<PrivateSeries> seriesComparator = new Comparator<PrivateSeries>() {
        public int compare(PrivateSeries o1, PrivateSeries o2) {
            String in1 = o1.getAttributes().getString(Tag.SeriesNumber);
            String in2 = o2.getAttributes().getString(Tag.SeriesNumber);
            return QueryUtil.compareIntegerStringAndPk(o1.getPk(), o2.getPk(), in1, in2);
        }

    };

    public int count(TrashListFilter filter, List<String> roles) {
        if ((roles != null) && (roles.size() == 0)) return 0;
        StringBuilder ql = new StringBuilder(64);
        ql.append("SELECT COUNT(*)");
        appendFromClause(ql, filter);
        appendWhereClause(ql, filter, roles);
        Query query = em.createQuery(ql.toString());
        setQueryParameters(query, filter, roles);
        return ((Number) query.getSingleResult()).intValue();
    }

    @SuppressWarnings("unchecked")
    public List<PrivatePatient> findPatients(TrashListFilter filter, int pagesize, int offset, List<String> roles) {
        if ((roles != null) && (roles.size() == 0)) return new ArrayList<PrivatePatient>();
        StringBuilder ql = new StringBuilder(64);
        ql.append("SELECT p");
        if (!filter.isPatientQuery())
            ql.append(", s");
        appendFromClause(ql, filter);
        appendWhereClause(ql, filter, roles);
        QueryUtil.appendOrderBy(ql, new String[]{"p.patientName, p.patientID, p.issuerOfPatientID"});
        Query query = em.createQuery(ql.toString());
        setQueryParameters(query, filter, roles);
        if (filter.isPatientQuery())
            return query.setMaxResults(pagesize).setFirstResult(offset).getResultList();
        else {
            List<Object[]> result = query.setMaxResults(pagesize).setFirstResult(offset).getResultList();
            List<PrivatePatient> patientList = new ArrayList<PrivatePatient>();
            PrivatePatient patient = null;
            for (Object[] element: result) {
                patient = (PrivatePatient) element[0];
                if (!patientList.contains(patient)) {
                    patient.setStudies(new HashSet<PrivateStudy>());
                    patientList.add(patient);
                }
                patient.getStudies().add((PrivateStudy) element[1]);
            }
            return patientList;
        }
    }

    private static void appendFromClause(StringBuilder ql, TrashListFilter filter) {
        ql.append(" FROM PrivatePatient p");
        if (!filter.isPatientQuery()) 
            ql.append(" INNER JOIN p.studies s");
    }

    private static void appendWhereClause(StringBuilder ql, TrashListFilter filter, List<String> roles) {
        ql.append(" WHERE p.privateType = 1");
        if ( filter.isPatientQuery()) {
            appendPatFilter(ql, filter);
        } else {
            if ( QueryUtil.isUniversalMatch(filter.getStudyInstanceUID()) ) {
                appendPatFilter(ql, filter);
                QueryUtil.appendAccessionNumberFilter(ql, QueryUtil.checkAutoWildcard(filter.getAccessionNumber(), filter.isAutoWildcard()));
                QueryUtil.appendSourceAETFilter(ql, new String[]{filter.getSourceAET()});
                if (filter.getDeletedDateMin() != null || filter.getDeletedDateMax() != null) {
                    ql.append(" AND EXISTS (SELECT st FROM PrivateStudy st JOIN st.series se JOIN se.instances i WHERE st = s AND ");
                    if (filter.getDeletedDateMin() != null) {
                        ql.append("i.createdTime >= :deletedDateMin");
                        if (filter.getDeletedDateMax() != null)
                            ql.append(" AND ");
                    }
                    if (filter.getDeletedDateMax() != null) {
                        ql.append("i.createdTime <= :deletedDateMax");
                    }
                    ql.append(")");
                }
            } else {
                ql.append(" AND s.studyInstanceUID = :studyInstanceUID");
            }
            if (roles != null)
                QueryUtil.appendDicomSecurityFilter(ql);
        }
    }

    private static void appendPatFilter(StringBuilder ql, TrashListFilter filter) {
        QueryUtil.appendPatientNameFilter(ql, "p", QueryUtil.checkAutoWildcard(filter.getPatientName(), filter.isPNAutoWildcard()));
        QueryUtil.appendPatientIDFilter(ql, "p", QueryUtil.checkAutoWildcard(filter.getPatientID(), filter.isAutoWildcard()));
        QueryUtil.appendIssuerOfPatientIDFilter(ql, "p", QueryUtil.checkAutoWildcard(filter.getIssuerOfPatientID(), filter.isAutoWildcard()));
    }

    private static void setQueryParameters(Query query, TrashListFilter filter, List<String> roles) {
        if ( filter.isPatientQuery()) {
            setPatQueryParameters(query, filter);
        } else {
            if ( QueryUtil.isUniversalMatch(filter.getStudyInstanceUID()) ) {
                setPatQueryParameters(query, filter);
                QueryUtil.setAccessionNumberQueryParameter(query, QueryUtil.checkAutoWildcard(filter.getAccessionNumber(), filter.isAutoWildcard()));
                QueryUtil.setSourceAETQueryParameter(query, new String[]{filter.getSourceAET()});
                if (filter.getDeletedDateMin() != null) {
                    query.setParameter("deletedDateMin", filter.getDeletedDateMin());
                }
                if (filter.getDeletedDateMax() != null) {
                    query.setParameter("deletedDateMax", filter.getDeletedDateMax());
                }
            } else {
                QueryUtil.setStudyInstanceUIDQueryParameter(query, filter.getStudyInstanceUID());
            }
            if (roles != null)
                query.setParameter("roles", roles);
        }
    }

    private static void setPatQueryParameters(Query query, TrashListFilter filter) {
        QueryUtil.setPatientNameQueryParameter(query, QueryUtil.checkAutoWildcard(filter.getPatientName(), filter.isPNAutoWildcard()));
        QueryUtil.setPatientIDQueryParameter(query, QueryUtil.checkAutoWildcard(filter.getPatientID(), filter.isAutoWildcard()));
        QueryUtil.setIssuerOfPatientIDQueryParameter(query, QueryUtil.checkAutoWildcard(filter.getIssuerOfPatientID(), filter.isAutoWildcard()));
    }
    
    public int countStudiesOfPatient(long pk, List<String> roles) {
        if ((roles != null) && (roles.size() == 0)) return 0;
        return ((Number) getStudiesOfPatientQuery(true, pk, roles).getSingleResult()).intValue();
    }
    
    @SuppressWarnings("unchecked")
    public List<PrivateStudy> findStudiesOfPatient(long pk, List<String> roles) {
        if ((roles != null) && (roles.size() == 0)) return new ArrayList<PrivateStudy>();
        return getStudiesOfPatientQuery(false, pk, roles).getResultList();
    }
        
    private Query getStudiesOfPatientQuery(boolean isCount, long pk, List<String> roles) {
        StringBuilder ql = new StringBuilder(64);
        ql.append("SELECT " + (isCount ? "COUNT(s)" : "s") + " FROM PrivateStudy s WHERE s.patient.pk=?1");
        if (roles != null)
            QueryUtil.appendDicomSecurityFilter(ql);
        Query query = em.createQuery(ql.toString());
        query.setParameter(1, pk);
        if (roles != null)
            query.setParameter("roles", roles);
        return query;
    }

    @SuppressWarnings("unchecked")
    public List<PrivateSeries> findSeriesOfStudy(long pk) {
        List<PrivateSeries> l = em.createQuery("FROM PrivateSeries s WHERE s.study.pk=?1 ORDER BY s.pk")
                .setParameter(1, pk).getResultList();
        Collections.sort(l, seriesComparator);
        return l;
    }

    @SuppressWarnings("unchecked")
    public List<PrivateInstance> findInstancesOfSeries(long pk) {
        List<PrivateInstance> l = em.createQuery("FROM PrivateInstance i WHERE i.series.pk=?1 ORDER BY i.pk")
                .setParameter(1, pk)
                .getResultList();
        Collections.sort(l, instanceComparator);
        return l;
   }

    @SuppressWarnings("unchecked")
    public List<String> selectDistinctSourceAETs() {
        return em.createQuery("SELECT DISTINCT s.sourceAET FROM Series s WHERE s.sourceAET IS NOT NULL ORDER BY s.sourceAET")
                .getResultList();
    }

    public PrivatePatient getPatient(long pk) {
        return em.find(PrivatePatient.class, pk);
    }

    public PrivateStudy getStudy(long pk) {
        return em.find(PrivateStudy.class, pk);
    }

    public PrivateSeries getSeries(long pk) {
        return em.find(PrivateSeries.class, pk);
    }

    public PrivateInstance getInstance(long pk) {
        return em.find(PrivateInstance.class, pk);
    }
    
    public void removeTrashEntities(List<Long> pks, Class<? extends BaseEntity> clazz, boolean removeFile) {
        
        if (clazz.equals(PrivatePatient.class)) {
            for (Long pk : pks)
                removeTrashEntity(getPatient(pk), removeFile);
        } else if (clazz.equals(PrivateStudy.class)) {
            for (Long pk : pks)
                removeTrashEntity(getStudy(pk), removeFile);
        } else if (clazz.equals(PrivateSeries.class)) {
            for (Long pk : pks)
                removeTrashEntity(getSeries(pk), removeFile);
        } else if (clazz.equals(PrivateInstance.class)) {
            for (Long pk : pks)
                removeTrashEntity(getInstance(pk), removeFile);
        }
    }
    
    private void removeTrashEntity(BaseEntity entity, boolean removeFile) {
        if (entity == null) return;
        else {
            if (entity instanceof PrivatePatient) {
                PrivatePatient pp = (PrivatePatient) entity;
                for (PrivateStudy pst : pp.getStudies())
                    removeTrashEntity(pst, removeFile);
                em.remove(pp);
            } else if (entity instanceof PrivateStudy) {
                PrivateStudy pst = (PrivateStudy) entity;
                for (PrivateSeries pse : pst.getSeries())
                    removeTrashEntity(pse, removeFile);
                PrivatePatient p = pst.getPatient();
                em.remove(pst);
                if (p.getStudies().size() <= 1)
                    em.remove(p);
            } else if (entity instanceof PrivateSeries) {
                PrivateSeries pse = (PrivateSeries) entity;
                for (PrivateInstance pi : pse.getInstances())
                    removeTrashEntity(pi, removeFile);
                PrivateStudy pst = pse.getStudy();
                em.remove(pse);
                if (pst.getSeries().size() <= 1)
                    em.remove(pst);
            } else if (entity instanceof PrivateInstance) {
                PrivateInstance pi = (PrivateInstance) entity;
                for (PrivateFile pf : pi.getFiles()) {
                    if (removeFile) {
                        em.remove(pf);
                    } else {
                        pf.setInstance(null);
                        em.merge(pf);
                    }
                }
                PrivateSeries pse = pi.getSeries();
                em.remove(pi);
                if (pse.getInstances().size() <= 1)
                    em.remove(pse);
            } else return;
        }
    }
    
    public void removeTrashAll() {
        em.createQuery("UPDATE PrivateFile p SET p.instance = Null").executeUpdate();
        em.createQuery("DELETE FROM PrivateInstance pi").executeUpdate();
        em.createQuery("DELETE FROM PrivateSeries pse").executeUpdate();
        em.createQuery("DELETE FROM PrivateStudy pst").executeUpdate();
        em.createQuery("DELETE FROM PrivatePatient pp").executeUpdate();
    }

    @SuppressWarnings("unchecked")
    public List<PrivateFile> getFilesForEntity(long pk, Class<? extends BaseEntity> clazz) {
        
        String query = "SELECT DISTINCT f FROM PrivateFile f LEFT JOIN FETCH f.fileSystem fs ";

        if (clazz.equals(PrivateInstance.class))
            query += "WHERE f.instance.pk = :pk";
        else {
            query += "LEFT JOIN f.instance.series se ";
            
            if (clazz.equals(PrivateSeries.class))
                query += "WHERE se.pk = :pk";
            else {
                query += "LEFT JOIN se.study st ";
                if (clazz.equals(PrivateStudy.class))
                    query += "WHERE st.pk = :pk";
                else if (clazz.equals(PrivatePatient.class))
                    query += "LEFT JOIN st.patient p WHERE p.pk = :pk";
                else return null;
            }
        }
        
        return em.createQuery(query)
            .setParameter("pk", pk)
            .getResultList();
    }
    
    @SuppressWarnings("unchecked")
    public List<Study> getStudiesInFolder(String[] suids) {
        StringBuilder sb = new StringBuilder("SELECT st FROM Study st LEFT JOIN FETCH st.patient WHERE st.studyInstanceUID");
        QueryUtil.appendIN(sb, suids.length);
        Query q = em.createQuery(sb.toString());
        QueryUtil.setParametersForIN(q, suids);
        return q.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Study> getStudiesInFolder(long privPatPk) {
        StringBuilder sb = new StringBuilder("SELECT st FROM Study st LEFT JOIN FETCH st.patient, PrivateStudy pst WHERE pst.patient.pk = :patPk AND st.studyInstanceUID = pst.studyInstanceUID");
        Query q = em.createQuery(sb.toString());
        q.setParameter("patPk", new Long(privPatPk));
        return q.getResultList();
    }
    
    public DicomObject getDicomAttributes(long filePk) {
        PrivateFile pf = em.find(PrivateFile.class, filePk);
        DicomObject dio = pf.getInstance().getAttributes();
        pf.getInstance().getSeries().getAttributes().copyTo(dio);
        pf.getInstance().getSeries().getStudy().getAttributes().copyTo(dio);
        pf.getInstance().getSeries().getStudy().getPatient().getAttributes().copyTo(dio);
        dio.putString(dio.resolveTag(PrivateTag.CallingAET, PrivateTag.CreatorID), 
                VR.AE, pf.getInstance().getSeries().getSourceAET());
        return dio;
    }

    public Long getNumberOfSeriesOfStudy(long studyPk) {
        return (Long) em.createQuery("SELECT COUNT(s) from PrivateSeries s WHERE s.study.pk = :studyPk")
        .setParameter("studyPk", studyPk)
        .getSingleResult();
    }
    
    public Long getNumberOfInstancesOfStudy(long studyPk) {
        return (Long) em.createQuery("SELECT DISTINCT COUNT(i) FROM PrivateInstance i, PrivateSeries se , PrivateStudy st WHERE i.series.pk = se.pk AND se.study.pk = st.pk AND st.pk = :studyPk")
        .setParameter("studyPk", studyPk)
        .getSingleResult();
    }

    public Long getNumberOfInstancesOfSeries(long seriesPk) {
        return (Long) em.createQuery("SELECT COUNT(i) from PrivateInstance i WHERE i.series.pk = :seriesPk")
        .setParameter("seriesPk", seriesPk)
        .getSingleResult();
    }
    
    
}
