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

package org.dcm4chee.web.dao.common;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.ejb.Local;

import org.dcm4che2.data.DicomObject;
import org.dcm4chee.archive.entity.Instance;
import org.dcm4chee.archive.entity.MPPS;
import org.dcm4chee.archive.entity.Series;
import org.dcm4chee.archive.entity.Study;
import org.dcm4chee.archive.entity.Patient;
import org.dcm4chee.web.dao.vo.EntityTree;

/**
 * @author Franz Willer <fwiller@gmail.com>
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision$ $Date$
 * @since Feb 01, 2010
 */
@Local
public interface DicomEditLocal {

    String JNDI_NAME = "dcm4chee-web-ear/DicomEditBean/local";

    EntityTree moveInstancesToTrash(long[] pk, boolean trustPatientIdWithoutIssuer);
    EntityTree moveInstanceToTrash(String iuid, boolean trustPatientIdWithoutIssuer);
    EntityTree moveSeriesToTrash(long[] pk, boolean trustPatientIdWithoutIssuer);
    EntityTree moveSeriesToTrash(String iuid, boolean trustPatientIdWithoutIssuer);
    EntityTree moveSeriesOfPpsToTrash(long[] pks, boolean trustPatientIdWithoutIssuer);
    EntityTree moveStudiesToTrash(long[] pk, boolean trustPatientIdWithoutIssuer);
    EntityTree moveStudyToTrash(String iuid, boolean trustPatientIdWithoutIssuer);
    EntityTree movePatientsToTrash(long[] pk, boolean trustPatientIdWithoutIssuer);
    EntityTree movePatientToTrash(String patId, String issuer, boolean trustPatientIdWithoutIssuer);
    
    public void deleteEmptyPatients(Set<Patient> pats);
    
    List<MPPS> deletePps(long[] pks);
    
    EntityTree moveStudiesToPatient(long al[], long l, boolean useIOCM);
    EntityTree moveStudyToPatient(String s, String s1, String s2, boolean useIOCM);

    DicomObject getCompositeObjectforSeries(String seriesIuid);
    DicomObject getCompositeObjectforSeries(long pk);
    DicomObject getCompositeObjectforStudy(String studyIuid);
    DicomObject getCompositeObjectforStudy(long pk);
    DicomObject getPatientAttributes(String patId, String issuer);
    DicomObject getPatientAttributes(long pk);
    
    Series updateSeries(Series series);
    Series createSeries(DicomObject seriesAttrs, long studyPk);
    void removeSeries(long seriesPk);
    Study updateStudy(Study study);
    Study createStudy(DicomObject studyAttrs, long patPk);
    void removeStudy(long studyPk);
    int removeForeignPpsInfo(long studyPk);
    
    DicomObject getIanForForwardModifiedObject(DicomObject obj, String level);
    
    EntityTree getEntitiesOfInstance(String iuid);
    EntityTree getEntitiesOfInstances(long[] pks);
    EntityTree getEntitiesOfSeries(long[] pks);
    EntityTree getEntitiesOfSeries(String iuid);
    void deleteSeries(Collection<Series> series);
    void deleteInstances(Collection<Instance> instances);
    void markFilePath(long filePk, String ext, boolean deleteMark);

}
