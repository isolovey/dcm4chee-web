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

import java.util.List;

import javax.ejb.Local;

import org.dcm4che2.data.DicomObject;
import org.dcm4chee.archive.entity.File;
import org.dcm4chee.archive.entity.Instance;
import org.dcm4chee.archive.entity.MPPS;
import org.dcm4chee.archive.entity.Patient;
import org.dcm4chee.archive.entity.Series;
import org.dcm4chee.archive.entity.Study;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision$ $Date$
 * @since Dec 17, 2008
 */
@Local
public interface StudyListLocal {

    String JNDI_NAME = "dcm4chee-web-ear/StudyListBean/local";

    int count(StudyListFilter filter, List<String> roles);
    boolean hasUnlinkedSeries(long studyPk, List<String> modalityFilter);
    int countUnconnectedMPPS(StudyListFilter filter);
    
    List<Patient> findPatients(StudyListFilter filter, int max, int index, List<String> roles);
    List<Patient> findUnconnectedMPPS(StudyListFilter filter, int max, int index);
    List<MPPS> findUnconnectedMPPSofPatient(long patPk);
    MPPS findMPPS(String mppsUID);
    
    int countStudiesOfPatient(long pk, List<String> roles);
    
    List<Study> findStudiesOfPatient(long pk, boolean latestStudyFirst, List<String> roles);

    boolean isActionForAllStudiesOfPatientAllowed(long patPk, String action, List<String> roles);
    
    List<String> findStudyPermissionActions(String studyInstanceUID, List<String> roles);
    
    List<Series> findSeriesOfStudy(long pk);
    List<Series> findSeriesOfStudyWithoutPPS(long pk);
    
    int countSeriesOfStudy(long pk);

    List<Series> findSeriesOfMpps(String uid);
    
    Series findSeriesByIuid(String seriesIuid);

    List<Instance> findInstancesOfSeries(long pk);
    int countInstancesOfSeries(long pk);
    
    List<File> findFilesOfInstance(long pk);

    List<String> selectDistinctSourceAETs();

    List<String> selectDistinctModalities();

    Patient getPatient(long pk);

    Patient updatePatient(long pk, DicomObject dataset);

    Study getStudy(long pk);

    Study updateStudy(long pk, DicomObject dataset);
    Study addStudy(long patPk, DicomObject dataset);
    void copyStudyPermissions(String srcStudyIuid, String destStudyIuid);
    
    Series getSeries(long pk);

    Series updateSeries(long pk, DicomObject dataset);
    Series addSeries(long studyPk, DicomObject dataset);

    Instance getInstance(long pk);

    Instance updateInstance(long pk, DicomObject dataset);

    MPPS getMPPS(long pk);

    MPPS updateMPPS(long pk, DicomObject dataset);
    
    long countDownloadableInstances(String[] studyIuids, String[] seriesIuids, String[] sopIuids);
    long countDownloadableInstancesLocal(String[] studyIuids, String[] seriesIuids, String[] sopIuids);
    List<Instance> getDownloadableInstances(String[] studyIuids, String[] seriesIuids, String[] sopIuids);
    
    boolean hasStudyForeignPpsInfo(long studyPk);
    
    boolean exists(long pk, int levelOfModel);
}
