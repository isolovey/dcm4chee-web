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

package org.dcm4chee.web.war.folder.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.VR;
import org.dcm4che2.util.UIDUtils;
import org.dcm4chee.archive.entity.MPPS;
import org.dcm4chee.archive.entity.Series;
import org.dcm4chee.archive.entity.Study;
import org.dcm4chee.archive.util.JNDIUtils;
import org.dcm4chee.web.dao.folder.StudyListLocal;
import org.dcm4chee.web.war.common.model.AbstractDicomModel;
import org.dcm4chee.web.war.common.model.AbstractEditableDicomModel;
import org.dcm4chee.web.war.config.delegate.WebCfgDelegate;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision: 18373 $ $Date: 2014-10-16 14:06:07 +0200 (Do, 16 Okt 2014) $
 * @since Dec 12, 2008
 */
public class StudyModel extends AbstractEditableDicomModel implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private List<PPSModel> ppss = new ArrayList<PPSModel>();

    private String availability;
    private String modalities;
    private int numberOfStudyRelatedSeries;
    private int numberOfStudyRelatedInstances;
    private List<String> studyPermissionActions = new ArrayList<String>();
    private String seriesIuid;
    private Boolean unlinkedSeries;
    private boolean hasLinkedSeries;
    private List<String> modalityFilter = WebCfgDelegate.getInstance().getModalityFilterList();
    
    public StudyModel(Study study, PatientModel patModel, Date createdTime) {
        if (study == null) {
            setPk(-1);
            dataset = new BasicDicomObject();
            dataset.putString(Tag.StudyInstanceUID, VR.UI, UIDUtils.createUID());
        } else {
            setPk(study.getPk());
            updateModel(study);
        }
        this.createdTime = createdTime;
        setPatient(patModel);
    }
    
    public StudyModel(Study study, PatientModel patModel, Date createdTime, List<String> studyPermissionActions) {
        this(study, patModel, createdTime);
        setStudyPermissionActions(studyPermissionActions);
    }
    
    public void setRestrictChildsBySeriesIuid(String seriesIuid) {
        this.seriesIuid = seriesIuid;
    }

    private void setPatient(PatientModel patModel) {
        setParent(patModel);
    }

    public PatientModel getPatient() {
        return (PatientModel) getParent();
    }

    public String getStudyInstanceUID() {
        return dataset.getString(Tag.StudyInstanceUID, "");
    }

    public List<String> getStudyPermissionActions() {
        return studyPermissionActions;
    }

    public void setStudyPermissionActions(List<String> studyPermissionActions) {
        if (studyPermissionActions == null) {
            this.studyPermissionActions.clear();
        } else {
            this.studyPermissionActions = studyPermissionActions;
        }
    }
    
    public Date getDatetime() {
        return toDate(Tag.StudyDate, Tag.StudyTime);
    }

    public void setDatetime(Date datetime) {
        
        dataset.putDate(Tag.StudyDate, VR.DA, datetime);
        dataset.putDate(Tag.StudyTime, VR.TM, datetime);
    }
    
    public String getId() {
        return dataset.getString(Tag.StudyID);
    }

    public String getAccessionNumber() {
        return dataset.getString(Tag.AccessionNumber, "");
    }

    public String getModalities() {
        return modalities;
    }

    public String getDescription() {
        return dataset.getString(Tag.StudyDescription);
    }

    public void setDescription(String description) {
        dataset.putString(Tag.StudyDescription, VR.LO, description);
    }

    public int getNumberOfSeries() {
        return numberOfStudyRelatedSeries;
    }

    public int getNumberOfInstances() {
        return numberOfStudyRelatedInstances;
    }

    public String getAvailability() {
        return availability;
    }

    public List<PPSModel> getPPSs() {
        return ppss;
    }

    public boolean hasSeries() {
        if (getPk() != -1) {
            StudyListLocal dao = (StudyListLocal)
            JNDIUtils.lookup(StudyListLocal.JNDI_NAME);
            return dao.countSeriesOfStudy(getPk()) > 0;
        } else { 
            return false;
        }
    }
    public boolean hasUnlinkedSeries() {
        if (unlinkedSeries == null) {
            if (getPk() != -1) {
                StudyListLocal dao = (StudyListLocal)
                JNDIUtils.lookup(StudyListLocal.JNDI_NAME);
                unlinkedSeries = dao.hasUnlinkedSeries(getPk(), modalityFilter);
            } else { 
                unlinkedSeries = true;
            }
        }
        return unlinkedSeries;
    }
    
    public boolean hasLinkedSeries() {
    	return hasLinkedSeries;
    }

    public void setUnlinkedSeries(boolean unlinkedSeries) {
        this.unlinkedSeries = unlinkedSeries;
    }

    @Override
    public int getRowspan() {
        int rowspan = isDetails() ? 2 : 1;
        for (PPSModel pps : ppss) {
            rowspan += pps.getRowspan();
        }
        return rowspan;
    }

    @Override
    public void collapse() {
        ppss.clear();
        seriesIuid = null;
    }

    @Override
    public boolean isCollapsed() {
        return ppss.isEmpty();
    }

    public void retainSelectedPPSs() {
        for (int i = 0; i < ppss.size(); i++) {
            PPSModel pps = ppss.get(i);
            pps.retainSelectedSeries();
            if (pps.isCollapsed() && !pps.isSelected()) {
                ppss.remove(i);
                i--;
            }
        }
    }

    @Override
    public void expand() {
        if (getPk() != -1) {
            ppss.clear();
            unlinkedSeries = false;
            hasLinkedSeries = false;
            StudyListLocal dao = (StudyListLocal)
            JNDIUtils.lookup(StudyListLocal.JNDI_NAME);
            if (seriesIuid == null) {
                for (Series series : dao.findSeriesOfStudy(getPk())) 
                    add(series);
            } else {
                add(dao.findSeriesByIuid(seriesIuid));
            }
        }
    }

    @Override
    public int levelOfModel() {
        return STUDY_LEVEL;
    }
   
    @Override
    public List<? extends AbstractDicomModel> getDicomModelsOfNextLevel() {
        return ppss;
    }
    
    private void add(Series series) {
        MPPS mpps = series.getModalityPerformedProcedureStep();
        SeriesModel seriesModel = new SeriesModel(series, null, series.getCreatedTime());
        for (PPSModel pps : ppss) {
            if (mpps != null ? mpps.getPk() == pps.getPk()
                    : pps.getDataset() == null 
                            && seriesModel.containedBySamePPS(pps.getSeries1())) {
                seriesModel.setParent(pps);
                pps.getSeries().add(seriesModel);
                return;
            }
        }
        if (modalityFilter == null || !modalityFilter.contains(series.getModality())) {
        	unlinkedSeries |= mpps == null || mpps.getAccessionNumber() == null;
        }
        PPSModel pps = new PPSModel(mpps, seriesModel, this, mpps != null ? mpps.getCreatedTime() : null);
        hasLinkedSeries |= pps.getAccessionNumber() != null;
        ppss.add(pps);
    }
    
    void add(PPSModel pps) {
        ppss.add(pps);
    }

    @Override
    public void update(DicomObject dicomObject) {
        StudyListLocal dao = (StudyListLocal)
                JNDIUtils.lookup(StudyListLocal.JNDI_NAME);
        Study s = null;
        if (getPk() == -1) {
            s = dao.addStudy(getPatient().getPk(), dicomObject);
            setPk(s.getPk());
            this.getPatient().getStudies().add(this);
        } else {
            s = dao.updateStudy(getPk(), dicomObject);
        }
        updateModel(s);
    }
    
    @Override
    public AbstractEditableDicomModel refresh() {
        StudyListLocal dao = (StudyListLocal)
        JNDIUtils.lookup(StudyListLocal.JNDI_NAME);
        if (getPk() != -1) {
            updateModel(dao.getStudy(getPk()));
        } else {
            ppss.clear();
            this.getPatient().initMpps(this, dao.findUnconnectedMPPSofPatient(getParent().getPk()));
        }
        return this;
    }    
    
    private void updateModel(Study s) {
        if (getPk() != -1) {
            dataset = s.getAttributes(false);
            availability = s.getAvailability().name();
            modalities = s.getModalitiesInStudy();
            numberOfStudyRelatedSeries = s.getNumberOfStudyRelatedSeries();
            numberOfStudyRelatedInstances = s.getNumberOfStudyRelatedInstances();
            createdTime = s.getCreatedTime();
        }
    }
    
    @Override
    public String toString() {
        return "Study: "+getStudyInstanceUID();
    }
}
