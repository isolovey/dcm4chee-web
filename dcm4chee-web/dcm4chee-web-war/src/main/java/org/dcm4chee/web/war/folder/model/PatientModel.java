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
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.wicket.model.IModel;
import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4chee.archive.entity.MPPS;
import org.dcm4chee.archive.entity.Patient;
import org.dcm4chee.archive.entity.Study;
import org.dcm4chee.archive.util.JNDIUtils;
import org.dcm4chee.web.common.exceptions.WicketExceptionWithMsgKey;
import org.dcm4chee.web.dao.folder.StudyListLocal;
import org.dcm4chee.web.war.StudyPermissionHelper;
import org.dcm4chee.web.war.common.model.AbstractDicomModel;
import org.dcm4chee.web.war.common.model.AbstractEditableDicomModel;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision: 18392 $ $Date: 2014-11-25 15:21:42 +0100 (Di, 25 Nov 2014) $
 * @since Dec 12, 2008
 */
public class PatientModel extends AbstractEditableDicomModel implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private List<StudyModel> studies = new ArrayList<StudyModel>();
    private Set<MPPS> mppsList;
    private IModel<Boolean> latestStudyFirst;
    private Boolean expandable;

    StudyListLocal dao = (StudyListLocal) JNDIUtils.lookup(StudyListLocal.JNDI_NAME);

    public PatientModel() {
        setPk(-1);
        dataset = new BasicDicomObject();
    }
    public PatientModel(Patient patient, IModel<Boolean> latestStudyFirst) {
        setPk(patient.getPk());
        this.dataset = patient.getAttributes();
        this.latestStudyFirst = latestStudyFirst;
        this.createdTime = patient.getCreatedTime();
        expandable = null;
        try {
            mppsList = patient.getModalityPerformedProcedureSteps();
            if (mppsList != null && !mppsList.isEmpty()) {
                StudyModel study = new StudyModel(null, this, createdTime);
                studies.add(study);
                initMpps(study, mppsList);
            } else {
                mppsList = null;
            }
        } catch (Throwable ignore) {
            log.warn("Cannot get ModalityPerformedProcedureSteps of patient{}! Ignored", toString());
        }
    }

    protected void initMpps(StudyModel study, Collection<MPPS> mppsList) {
        for (MPPS pps : mppsList) {
            study.add(new PPSModel(pps, null, study, pps.getCreatedTime()));
        }
        expandable = study.getPPSs().size() > 0;
    }

    @Override
    public void setParent(AbstractDicomModel o) {
        throw new UnsupportedOperationException("Patient has no parent model!");
    }

    public String getName() {
        String pn = dataset.getString(Tag.PatientName);
        return pn == null ? null : pn.trim();
    }

    public String getId() {
        return dataset.getString(Tag.PatientID);
    }

    public String getIssuer() {
        return dataset.getString(Tag.IssuerOfPatientID);
    }
    
    public String getIdAndIssuer() {
        String id = getId();
        String issuer = getIssuer();
        if (issuer == null) {
            return id == null ? "" : id; 
        }
        return id == null ? " / "+issuer : id+" / "+issuer;
        
    }

    public String getSex() {
        return dataset.getString(Tag.PatientSex);
    }

    public Date getBirthdate() {
        return toDate(Tag.PatientBirthDate);
    }

    public String getComments() {
        return dataset.getString(Tag.PatientComments);
    }

    public List<StudyModel> getStudies() {
        return studies;
    }
    
    @Override
    public int getRowspan() {
        int rowspan = isDetails() ? 2 : 1;
        for (StudyModel study : studies) {
            rowspan += study.getRowspan();
        }
        return rowspan;
    }

    @Override
    public void collapse() {
        studies.clear();
    }

    @Override
    public boolean isCollapsed() {
        return studies.isEmpty();
    }

    public void retainSelectedStudies() {
        for (int i = 0; i < studies.size(); i++) {
            StudyModel study = studies.get(i);
            study.retainSelectedPPSs();
            if (study.isCollapsed() && !study.isSelected()) {
                studies.remove(i);
                i--;
            }
        }
    }

    @Override
    public void expand() {
        studies.clear();
        if (mppsList == null) {
            List<String> dicomSecurityRoles = StudyPermissionHelper.get().applyStudyPermissions() ? 
                    StudyPermissionHelper.get().getDicomRoles() : null;
            for (Study study : dao.findStudiesOfPatient(getPk(), 
                    latestStudyFirst == null ? false : latestStudyFirst.getObject(), dicomSecurityRoles))     
                this.studies.add(new StudyModel(study, this, study.getCreatedTime(), 
                        dao.findStudyPermissionActions(study.getStudyInstanceUID(), 
                                StudyPermissionHelper.get().getDicomRoles())));
        } else {
            StudyModel study = new StudyModel(null, this, createdTime);
            studies.add(study);
            initMpps(study, mppsList);
        }
        for (StudyModel m : studies) {
        	m.expand();
        	m.collapse();
        }
    }

    @Override
    public int levelOfModel() {
        return PATIENT_LEVEL;
    }
   
    @Override
    public List<? extends AbstractDicomModel> getDicomModelsOfNextLevel() {
        return studies;
    }

    @Override
    public void update(DicomObject dicomObject) {
        Patient pat = dao.updatePatient(getPk(), dicomObject);
        if (pat == null) {
            throw new WicketExceptionWithMsgKey("PatientAlreadyExists");
        }
        dataset = pat.getAttributes();
        if (getPk() == -1) {
            setPk(pat.getPk());
            createdTime = pat.getCreatedTime();
        }
    }
    
    @Override
    public AbstractEditableDicomModel refresh() {
        dataset = dao.getPatient(getPk()).getAttributes();
        return this;
    }
    
    public void setExpandable(boolean expandable) {
        this.expandable = expandable;
    }
    
    public boolean isExpandable() {
        if (expandable == null) {
            List<String> dicomSecurityRoles = StudyPermissionHelper.get().applyStudyPermissions() ?
                    StudyPermissionHelper.get().getDicomRoles() : null;
            expandable = dao.countStudiesOfPatient(getPk(), dicomSecurityRoles) > 0;
        }
        return expandable;
    }
    
    public boolean isActionForAllStudiesAllowed(String action) {
        return dao.isActionForAllStudiesOfPatientAllowed(getPk(), action, 
            StudyPermissionHelper.get().isUseStudyPermissions() ? StudyPermissionHelper.get().getDicomRoles() : null);
    }
    
    @Override
    public String toString() {
        return "Patient: "+getName()+" (ID:"+getIdAndIssuer()+")";
    }
}
