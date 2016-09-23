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

package org.dcm4chee.web.war.worklist.modality.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4chee.archive.conf.AttributeFilter;
import org.dcm4chee.archive.entity.MWLItem;
import org.dcm4chee.archive.util.JNDIUtils;
import org.dcm4chee.web.dao.worklist.modality.ModalityWorklistLocal;
import org.dcm4chee.web.war.common.model.AbstractDicomModel;
import org.dcm4chee.web.war.common.model.AbstractEditableDicomModel;

/**
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision: 18097 $ $Date: 2013-10-16 14:04:24 +0200 (Mi, 16 Okt 2013) $
 * @since 20.04.2010
 */
public class MWLItemModel extends AbstractEditableDicomModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private DicomObject patAttrs, spsItem;
    
    private boolean collapsed;

    public MWLItemModel(MWLItem mwlItem) {
        this.collapsed = true;
        setPk(mwlItem.getPk());
        this.dataset = mwlItem.getAttributes();
        patAttrs = mwlItem.getPatient().getAttributes();
        spsItem = dataset.getNestedDicomObject(Tag.ScheduledProcedureStepSequence);
    }
    
    public MWLItemModel(DicomObject mwl) {
        this.collapsed = true;
        setPk(-1);
        AttributeFilter filter = AttributeFilter.getExcludePatientAttributeFilter();
        this.dataset = filter.filter(mwl);
        patAttrs = new BasicDicomObject();
        DicomElement e;
        for (Iterator<DicomElement> it = mwl.iterator() ; it.hasNext() ;) {
            e = it.next();
            if (filter.hasTag(e.tag())) {
                patAttrs.add(e);
            }
        }
        spsItem = dataset.getNestedDicomObject(Tag.ScheduledProcedureStepSequence);
        if (spsItem == null) {
            String msg = "Missing Scheduled Procedure Step Sequence (0040,0100) in MWL Item!";
            log.error(msg+" Dataset:{}", dataset);
            throw new IllegalArgumentException(msg);
        }
    }

    public String getSPSDescription() {
        String desc = getCodeString(spsItem.get(Tag.ScheduledProtocolCodeSequence));
        if (desc == null)
            desc = spsItem.getString(Tag.ScheduledProcedureStepDescription);
        return desc;
    }

    public String getPatientName() {
        String pn = patAttrs.getString(Tag.PatientName);
        return pn == null ? null : pn.trim();
    }
    public String getPatientID() {
        return patAttrs.getString(Tag.PatientID);
    }
    public String getIssuerOfPatientID() {
        return patAttrs.getString(Tag.IssuerOfPatientID);
    }
    public String getPatientIDAndIssuer() {
        String id = getPatientID();
        String issuer = getIssuerOfPatientID();
        if (issuer == null) {
            return id == null ? "" : id; 
        }
        return id == null ? " / "+issuer : id+" / "+issuer;
        
    }
    public Date getBirthDate() {
        return patAttrs.getDate(Tag.PatientBirthDate);
    }
    public DicomObject getPatientAttributes() {
        return patAttrs;
    }
    
    public String getSPSModality() {
        return spsItem.getString(Tag.Modality);
    }

    public Date getStartDate() {
        try {
            Date d = spsItem.getDate(Tag.ScheduledProcedureStepStartDateTime);
            if (d == null) {
                d = spsItem.getDate(Tag.ScheduledProcedureStepStartDate, Tag.ScheduledProcedureStepStartTime);
            }
            return d;
        } catch (Exception x) {
            log.warn("DicomObject contains wrong value in date attribute!:"+dataset);
            return null;
        }
    }

    public String getSPSID() {
        return spsItem.getString(Tag.ScheduledProcedureStepID);
    }

    public String getRequestedProcedureID() {
        return dataset.getString(Tag.RequestedProcedureID);
    }

    public String getAccessionNumber() {
        return dataset.getString(Tag.AccessionNumber);
    }
 
    public String getReqProcedureDescription() {
        String desc = getCodeString(dataset.get(Tag.RequestedProcedureCodeSequence));
        if (desc == null) {
            desc = dataset.getString( Tag.RequestedProcedureDescription );
        }
        return desc;
    }

    public String getStationAET() {
        return spsItem.getString( Tag.ScheduledStationAETitle );
    }

    public String getStationName() {
        return spsItem.getString( Tag.ScheduledStationName );
    }

    public String getSPSStatus() {
        return spsItem.getString(Tag.ScheduledProcedureStepStatus);
    }

    @Override
    public void collapse() {
        this.collapsed = true;
    }

    @Override
    public void expand() {
        this.collapsed = false;
    }

    @Override
    public List<? extends AbstractDicomModel> getDicomModelsOfNextLevel() {
        return null;
    }

    @Override
    public int getRowspan() {
        return 0;
    }

    @Override
    public boolean isCollapsed() {
        return this.collapsed;
    }

    @Override
    public int levelOfModel() {
        return NO_LEVEL;
    }

    @Override
    public void update(DicomObject dicomObject) {
        ModalityWorklistLocal dao = (ModalityWorklistLocal)
                JNDIUtils.lookup(ModalityWorklistLocal.JNDI_NAME);
        dataset = dao.updateMWLItem(getPk(), dicomObject).getAttributes();
    }

    @Override
    public AbstractEditableDicomModel refresh() {
        ModalityWorklistLocal dao = (ModalityWorklistLocal)
        JNDIUtils.lookup(ModalityWorklistLocal.JNDI_NAME);
        dataset = dao.getMWLItem(getPk()).getAttributes();     
        return this;
    }
}
