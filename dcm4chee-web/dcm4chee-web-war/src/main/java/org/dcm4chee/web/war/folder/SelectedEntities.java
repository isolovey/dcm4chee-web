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

package org.dcm4chee.web.war.folder;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.dcm4chee.web.war.common.model.AbstractDicomModel;
import org.dcm4chee.web.war.common.model.AbstractEditableDicomModel;
import org.dcm4chee.web.war.config.delegate.WebCfgDelegate;
import org.dcm4chee.web.war.folder.model.FileModel;
import org.dcm4chee.web.war.folder.model.InstanceModel;
import org.dcm4chee.web.war.folder.model.PPSModel;
import org.dcm4chee.web.war.folder.model.PatientModel;
import org.dcm4chee.web.war.folder.model.SeriesModel;
import org.dcm4chee.web.war.folder.model.StudyModel;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 * @version $Revision: 16415 $ $Date: 2011-12-09 10:43:37 +0100 (Fr, 09 Dez 2011) $
 * @since 19 Apr, 2010
 */
public class SelectedEntities implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private Set<PatientModel> patients = new HashSet<PatientModel>();
    private Set<StudyModel> studies = new HashSet<StudyModel>();
    private Set<PPSModel> ppss = new HashSet<PPSModel>();
    private Set<SeriesModel> seriess = new HashSet<SeriesModel>();
    private Set<InstanceModel> instances = new HashSet<InstanceModel>();
    private Set<FileModel> files = new HashSet<FileModel>();

    private boolean hasTooOld = false;

    public boolean update(boolean useStudyPermissions, List<PatientModel> allPatients, String action) {
        return update(useStudyPermissions, allPatients, action, false);
    }
    
    /**
     * 
     * @param useStudyPermissions check StudyPermission
     * @param allPatients List of all Patient models in view.
     * @param action StudyPermission action that must be allowed.
     * @param all Select also childs of selected entities
     * @param patIsTarget Patient is target: keep selection even when the StudyPermissions are not fulfilled
     * @return true if one ore more entities are not allowed 
     */
    public boolean update(boolean useStudyPermissions, List<PatientModel> allPatients, String action, 
            boolean allowSrcInTarget) {
        boolean ignoredNotAllowedEntities = false;
        clear();
        for ( PatientModel patient : allPatients ) {
            if (patient.isSelected()) {
                if (!useStudyPermissions || patient.isActionForAllStudiesAllowed(action)) {
                    patients.add(patient);
                } else {
                    ignoredNotAllowedEntities = true;
                    if (patient.isCollapsed()) {
                        patient.expand();
                    }
                    for (StudyModel study : patient.getStudies()) {
                        if  (useStudyPermissions && !study.getStudyPermissionActions().contains(action)) {
                            continue;
                        }
                        studies.add(study);
                    }
                }
            }
            if (allowSrcInTarget || !patient.isSelected()) {
                boolean actionDenied;
                studyLoop: for (StudyModel study : patient.getStudies()) {
                    actionDenied = useStudyPermissions && !study.getStudyPermissionActions().contains(action);
                    if (study.isSelected()) {
                        if (actionDenied) {
                            ignoredNotAllowedEntities = true;
                            continue studyLoop;
                        }
                        studies.add(study);
                    }
                    if (allowSrcInTarget || !study.isSelected()) {
                        for ( PPSModel pps : study.getPPSs()) {
                            if (pps.isSelected()) {
                                if (actionDenied) {
                                    ignoredNotAllowedEntities = true;
                                    continue studyLoop;
                                }
                                ppss.add(pps);
                            }
                            if (allowSrcInTarget || !pps.isSelected()) {
                                for ( SeriesModel series : pps.getSeries()) {
                                    if (series.isSelected()) {
                                        if (actionDenied) {
                                            ignoredNotAllowedEntities = true;
                                            continue studyLoop;
                                        }
                                        seriess.add(series);
                                    }
                                    if (allowSrcInTarget || !series.isSelected()) {
                                        for (InstanceModel inst : series.getInstances()) {
                                            if (inst.isSelected()) {
                                                if (actionDenied) {
                                                    ignoredNotAllowedEntities = true;
                                                    continue studyLoop;
                                                }
                                               instances.add(inst);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return ignoredNotAllowedEntities;
    }

    public boolean hasDicomSelection() {
        return patients.size() > 0 || studies.size() > 0 || ppss.size() > 0 || seriess.size() > 0 || instances.size() > 0;
    }

    public boolean hasPatients() {
        return !patients.isEmpty();
    }
    public Set<PatientModel> getPatients() {
        return patients;
    }

    public boolean hasStudies() {
        return !studies.isEmpty();
    }
    public Set<StudyModel> getStudies() {
        return studies;
    }

    public boolean hasPPS() {
        return !ppss.isEmpty();
    }
    public Set<PPSModel> getPpss() {
        return ppss;
    }

    public boolean hasSeries() {
        return !seriess.isEmpty();
    }
    public Set<SeriesModel> getSeries() {
        return seriess;
    }

    public boolean hasInstances() {
        return !instances.isEmpty();
    }
    public Set<InstanceModel> getInstances() {
        return instances;
    }
    
    public Set<FileModel> getFiles() {
        return files;
    }

    public void clear() {
        patients.clear();
        studies.clear();
        ppss.clear();
        seriess.clear();
        instances.clear();
    }

    public void deselectAll() {
        deselectAll(patients);
        deselectAll(studies);
        deselectAll(ppss);
        deselectAll(seriess);
        deselectAll(instances);
    }
    public static void deselectAll(Collection<? extends AbstractDicomModel> models) {
        for (AbstractDicomModel m : models) {
            deselectChilds(m.getDicomModelsOfNextLevel());
            m.setSelected(false);
        }
    }
    public void deselectChildsOfSelectedEntities() {
        deselectChilds(patients);
        deselectChilds(studies);
        deselectChilds(ppss);
        deselectChilds(seriess);
        deselectChilds(instances);
    }
    
    private static void deselectChilds(Collection<? extends AbstractDicomModel> models) {
        if (models != null) { 
            for (AbstractDicomModel p : models) {
                deselectChildsOf(p);
            }
        }
    }
    private static void deselectChildsOf(AbstractDicomModel p) {
        if (p != null) { 
            List<? extends AbstractDicomModel> childs = p.getDicomModelsOfNextLevel();
            if ( childs == null || childs.isEmpty())
                return;
            for (AbstractDicomModel c : childs) {
                if ( c.isSelected() ) {
                    c.setSelected(false);
                }
                deselectChildsOf(c);
            }
        }
    }
    
    public static void setSelectionAtLevel(Collection<? extends AbstractDicomModel> all, int level, boolean select, boolean expand) {
        for ( AbstractDicomModel m : all ) {
            if ( m.levelOfModel() == level ) {
                m.setSelected(select);
            } else if (m.levelOfModel() < level) {
                Collection<? extends AbstractDicomModel> modelsOfNextLevel = m.getDicomModelsOfNextLevel();
                if (modelsOfNextLevel.size() == 0 && expand) {
                    m.expand();
                    modelsOfNextLevel = m.getDicomModelsOfNextLevel();
                }
                setSelectionAtLevel(modelsOfNextLevel, level, select, expand);
            }
        }
    }
    
    public void refreshView(boolean deselect) {
        for (InstanceModel m : instances) {
            refreshChilds(m.getSeries());
            m.getSeries().getPPS().refresh();
            m.getSeries().getPPS().getStudy().refresh();//refresh study
            if (deselect) 
                m.setSelected(false);
        }
        for (SeriesModel m : seriess) {
            refreshChilds(m.getPPS().getStudy());
            if (deselect) 
                m.setSelected(false);
        }
        for (PPSModel m : ppss) {
            refreshChilds(m.getStudy());
            if (deselect) 
                m.setSelected(false);
        }
        for (StudyModel m : studies) {
            refreshChilds(m.getPatient());
            if (deselect) 
                m.setSelected(false);
        }
        if (deselect) {
            for (PatientModel m : patients) {
                m.setSelected(false);
            }
        }
    }
    
    private void refreshChilds(AbstractEditableDicomModel m) {
        m.expand();
        m.refresh();
    }
    
    public void computeTooOld() {
        
        for (InstanceModel m : instances) {
            hasTooOld = tooOld(m);
            if (hasTooOld) 
                return;
        }
        for (SeriesModel m : seriess) {
            hasTooOld = tooOld(m);
            if (hasTooOld) 
                return;
        }
        for (PPSModel m : ppss) {
            hasTooOld = tooOld(m);
            if (hasTooOld) 
                return;
        }
        for (StudyModel m : studies) {
            hasTooOld = tooOld(m);
            if (hasTooOld) 
                return;
        }
        for (PatientModel m : patients) {
            hasTooOld = tooOld(m);
            if (hasTooOld) 
                return;
        }
    }
    public Set<StudyModel> getStudiesTooOld() {
        HashSet<StudyModel> toOld = new HashSet<StudyModel>();
        for (InstanceModel m : instances) {
            if (tooOld(m))
                toOld.add(m.getSeries().getPPS().getStudy());
        }
        for (SeriesModel m : seriess) {
            if (tooOld(m))
                toOld.add(m.getPPS().getStudy());
        }
        for (PPSModel m : ppss) {
            if (tooOld(m))
                toOld.add(m.getStudy());
        }
        for (StudyModel m : studies) {
            if (tooOld(m))
                toOld.add(m);
        }
        for (PatientModel m : patients) {
            if (tooOld(m))
                toOld.addAll(m.getStudies());
        }
        return toOld;
    }

    public boolean hasTooOld() {
        return hasTooOld;
    }
    
    public boolean tooOld(final AbstractEditableDicomModel model) {
        if (model.getCreatedTime() != null) {
            Calendar then = Calendar.getInstance();
            then.setTime(model.getCreatedTime());
            long tooOldLimit = WebCfgDelegate.getInstance().getTooOldLimit();
            return tooOldLimit == 0 ? 
                    false 
                    : (Calendar.getInstance().getTimeInMillis() - then.getTimeInMillis()) 
                        > tooOldLimit;
        }
        return true;
    } 

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (patients.size()>0) sb.append(" Patients:").append(patients.size());
        if (studies.size()>0) sb.append(" Studies:").append(studies.size());
        if (ppss.size()>0) sb.append(" PPS:").append(ppss.size());
        if (seriess.size()>0) sb.append(" Series:").append(seriess.size());
        if (instances.size()>0) sb.append(" Instances:").append(instances.size());
        if (files.size()>0) sb.append(" Files:").append(files.size());
        return sb.toString();
    }
}
