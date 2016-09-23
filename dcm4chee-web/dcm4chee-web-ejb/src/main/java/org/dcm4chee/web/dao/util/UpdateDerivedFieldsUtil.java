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

package org.dcm4chee.web.dao.util;

import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.dcm4chee.archive.common.Availability;
import org.dcm4chee.archive.entity.Series;
import org.dcm4chee.archive.entity.Study;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Franz Willer <fwiller@gmail.com>
 * @version $Revision$ $Date$
 * @since Apr 24, 2010
 */

public class UpdateDerivedFieldsUtil {

    private static Logger log = LoggerFactory.getLogger(UpdateDerivedFieldsUtil.class);   
            
    @PersistenceContext(unitName="dcm4chee-arc")
    private EntityManager em;

    public UpdateDerivedFieldsUtil(EntityManager em) {
        this.em = em;
    }
    
    public void updateDerivedFieldsOfSeries(Series series) {
        updateNumberOfSeriesRelatedInstances(series);
        updateAvailability(series);
        em.merge(series);
    }

    public void updateDerivedFieldsOfStudy(Study study) {
        for (Series s : study.getSeries()) {
            updateDerivedFieldsOfSeries(s);
        }
        updateNumberOfStudyRelatedSeriesAndInstances(study);
        updateModalitiesInStudy(study);
        updateCUIDsInStudy(study);
        updateAvailability(study);
        em.merge(study);
    }
    
    private void updateNumberOfStudyRelatedSeriesAndInstances(Study study) {
        Query qS = em.createQuery("SELECT count(s) FROM Study st INNER JOIN st.series s WHERE st.pk = :pk");
        qS.setParameter("pk", study.getPk());
        Long nos = (Long) qS.getSingleResult();
        Query qI = em.createQuery("SELECT count(i) FROM Study st INNER JOIN st.series s INNER JOIN s.instances i WHERE st.pk = :pk");
        qI.setParameter("pk", study.getPk());
        Long noi = (Long) qI.getSingleResult();
        study.setNumberOfStudyRelatedSeries(nos.intValue());
        study.setNumberOfStudyRelatedInstances(noi.intValue());
    }

    private void updateNumberOfSeriesRelatedInstances(Series series) {
        Query qI = em.createQuery("SELECT count(i) FROM Series s INNER JOIN s.instances i WHERE s.pk = :pk");
        qI.setParameter("pk", series.getPk());
        Long noi = (Long) qI.getSingleResult();
        series.setNumberOfSeriesRelatedInstances(noi.intValue());
    }
    
    public boolean updateModalitiesInStudy(Study study) {
        String mds = "";
        if (study.getNumberOfStudyRelatedInstances() > 0) {
            Query qM = em.createQuery("SELECT DISTINCT s.modality FROM Study st, IN(st.series) s WHERE st.pk = :pk");
            qM.setParameter("pk", study.getPk());
            List<?> modalities = qM.getResultList();
            if (modalities.remove(null))
                log.warn("Study[iuid=" + study.getStudyInstanceUID()
                        + "] contains Series with unspecified Modality");
            if (!modalities.isEmpty()) {
                Iterator<?> it = modalities.iterator();
                StringBuffer sb = new StringBuffer((String) it.next());
                while (it.hasNext())
                    sb.append('\\').append(it.next());
                mds = sb.toString();
            }
        }
        if (mds.equals(study.getModalitiesInStudy())) {
            return false;
        }
        study.setModalitiesInStudy(mds);
        return true;
    }

    public boolean updateCUIDsInStudy(Study study) {
        String cuids = "";
        if (study.getNumberOfStudyRelatedInstances() > 0) {
            Query qM = em.createQuery("SELECT DISTINCT i.sopClassUID FROM Instance i WHERE i.series.study.pk = :pk");
            qM.setParameter("pk", study.getPk());
            List<?> uids = qM.getResultList();
            StringBuffer sb = new StringBuffer();
            for (Iterator<?> it = uids.iterator() ; it.hasNext() ; ) {
                sb.append(it.next()).append('\\');
            }
            if (sb.length() > 0)
                cuids = sb.substring(0, sb.length()-1);
        }
        if (cuids.equals(study.getSopClassesInStudy())) {
            return false;
        }
        study.setSopClassesInStudy(cuids);
        return true;
    }
    
    private boolean updateAvailability(Study study) {
        Availability availability = null;
        if (study.getNumberOfStudyRelatedInstances() > 0) {
            Query qA = em.createQuery("SELECT MAX(s.availability) FROM Series s WHERE s.study.pk = :pk");
            qA.setParameter("pk", study.getPk());
            availability = (Availability) qA.getSingleResult();
        } 
        if (availability == null) {
            availability = Availability.ONLINE;
        }
        Availability prevAvailability = study.getAvailability();
        if (availability.equals(prevAvailability)) {
            return false;
        }
        study.setAvailability(availability);
        if (log.isDebugEnabled()) {
            log.debug("update Availability of Study[pk=" + study.getPk()
                    + ", uid=" + study.getStudyInstanceUID() + "] from " 
                    + prevAvailability.name() + " to "
                    + availability.name());
        }
        return true;
    }
    private boolean updateAvailability(Series series) {
        log.info("Update availability of series pk:"+series.getPk());
        Availability availability = null;
        if (series.getNumberOfSeriesRelatedInstances() > 0) {
            Query qA = em.createQuery("SELECT MAX(i.availability) FROM Instance i WHERE i.series.pk = :pk");
            qA.setParameter("pk", series.getPk());
            availability = (Availability) qA.getSingleResult();
            log.info("found availabilty for series from instances"+availability);
        }
        if (availability == null) {
            availability = Availability.ONLINE;
        }
        Availability prevAvailability = series.getAvailability();
        log.info("availabilty:"+availability.name()+" prevAvailability:"+prevAvailability.name()+" prevAvailability:"+prevAvailability);
        if (availability.equals(prevAvailability)) {
            return false;
        }
        series.setAvailability(availability);
        if (log.isDebugEnabled()) {
            log.debug("update Availability of Study[pk=" + series.getPk()
                    + ", uid=" + series.getSeriesInstanceUID() + "] from " 
                    + prevAvailability.name() + " to "
                    + availability.name());
        }
        return true;
    }

}
