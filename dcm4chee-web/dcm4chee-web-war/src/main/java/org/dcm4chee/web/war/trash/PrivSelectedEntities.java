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

package org.dcm4chee.web.war.trash;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.dcm4chee.web.war.common.model.AbstractDicomModel;
import org.dcm4chee.web.war.trash.model.PrivInstanceModel;
import org.dcm4chee.web.war.trash.model.PrivPatientModel;
import org.dcm4chee.web.war.trash.model.PrivSeriesModel;
import org.dcm4chee.web.war.trash.model.PrivStudyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision: 15552 $ $Date: 2011-06-07 17:05:40 +0200 (Di, 07 Jun 2011) $
 * @since 12.05.2010
 */
public class PrivSelectedEntities implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private Set<PrivPatientModel> patients = new HashSet<PrivPatientModel>();
    private Set<PrivStudyModel> studies = new HashSet<PrivStudyModel>();
    private Set<PrivSeriesModel> seriess = new HashSet<PrivSeriesModel>();
    private Set<PrivInstanceModel> instances = new HashSet<PrivInstanceModel>();
    
    private static Logger log = LoggerFactory.getLogger(PrivSelectedEntities.class);
            
    public PrivSelectedEntities() {
    }
    
    public void update(List<PrivPatientModel> list) {
        clear();
        for ( PrivPatientModel p : list ) {
            if ( p.isSelected() ) {
                patients.add(p);
            } else {
                for (PrivStudyModel study : p.getStudies()) {
                    if (study.isSelected()) {
                        studies.add(study);
                    } else {
                        for ( PrivSeriesModel series : study.getSeries()) {
                            if ( series.isSelected() ) {
                                seriess.add(series);
                            } else {
                                for (PrivInstanceModel inst : series.getInstances()) {
                                    if (inst.isSelected()) {
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
    
    public boolean hasDicomSelection() {
        return patients.size() > 0 || studies.size() > 0 || seriess.size() > 0 || instances.size() > 0;
    }

    public boolean hasPatients() {
        return !patients.isEmpty();
    }
    public Set<PrivPatientModel> getPatients() {
        return patients;
    }

    public boolean hasStudies() {
        return !studies.isEmpty();
    }
    public Set<PrivStudyModel> getStudies() {
        return studies;
    }

    public boolean hasSeries() {
        return !seriess.isEmpty();
    }
    public Set<PrivSeriesModel> getSeries() {
        return seriess;
    }

    public boolean hasInstances() {
        return !instances.isEmpty();
    }
    public Set<PrivInstanceModel> getInstances() {
        return instances;
    }

    public void clear() {
        patients.clear();
        studies.clear();
        seriess.clear();
        instances.clear();
    }

    public void deselectChildsOfSelectedEntities() {
        deselectChilds(patients);
    }
    
    private void deselectChilds(Collection<? extends AbstractDicomModel> models) {
        for (AbstractDicomModel p : models) {
            deselectChildsOf(p, p.levelOfModel());
        }
    }
    private void deselectChildsOf(AbstractDicomModel p, int parentLevel) {
        List<? extends AbstractDicomModel> childs = p.getDicomModelsOfNextLevel();
        if ( childs == null || childs.isEmpty())
            return;
        for (AbstractDicomModel c : childs) {
            if ( c.isSelected() ) {
                c.setSelected(false);
                log.debug("Deselect entity {} because parent is already selected! selected parent level: {}", c, parentLevel);
            }
            deselectChildsOf(c, parentLevel);
        }
    }
    
    public void refreshView(boolean deselect) {
        for (PrivInstanceModel m : instances) 
            m.getSeries().expand();
        for (PrivSeriesModel m : seriess) 
            m.getStudy().expand();
        for (PrivStudyModel m : studies) 
            m.getPatient().expand();
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (patients.size()>0) sb.append(" Patients:").append(patients.size());
        if (studies.size()>0) sb.append(" Studies:").append(studies.size());
        if (seriess.size()>0) sb.append(" Series:").append(seriess.size());
        if (instances.size()>0) sb.append(" Instances:").append(instances.size());
        return sb.toString();
    }
}
