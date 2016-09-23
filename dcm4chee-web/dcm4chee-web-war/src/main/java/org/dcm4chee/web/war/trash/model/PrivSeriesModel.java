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

package org.dcm4chee.web.war.trash.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.dcm4che2.data.Tag;
import org.dcm4chee.archive.entity.PrivateInstance;
import org.dcm4chee.archive.entity.PrivateSeries;
import org.dcm4chee.archive.util.JNDIUtils;
import org.dcm4chee.web.dao.trash.TrashListLocal;
import org.dcm4chee.web.war.common.model.AbstractDicomModel;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 * @version $Revision: 15552 $ $Date: 2011-06-07 17:05:40 +0200 (Di, 07 Jun 2011) $
 * @since May 10, 2010
 */
public class PrivSeriesModel extends AbstractDicomModel implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private String sourceAET;
    private List<PrivInstanceModel> instances = new ArrayList<PrivInstanceModel>();

    public PrivSeriesModel(PrivateSeries series, PrivStudyModel studyModel) {
        setPk(series.getPk());
        this.sourceAET = series.getSourceAET();
        this.dataset = series.getAttributes();
        setStudy(studyModel);
    }

    public void setStudy(PrivStudyModel m) {
        setParent(m);
    }

    public PrivStudyModel getStudy() {
        return (PrivStudyModel) getParent();
    }

    public String getSeriesInstanceUID() {
        return dataset.getString(Tag.SeriesInstanceUID);
    }
    
    public Date getDatetime() {
        return toDate(Tag.SeriesDate, Tag.SeriesTime);
    }

    public String getSeriesDate() {
        return dataset.getString(Tag.SeriesDate);
    }

    public String getSeriesNumber() {
        return dataset.getString(Tag.SeriesNumber);
    }

    public String getModality() {
        return dataset.getString(Tag.Modality);
    }

    public String getStationName() {
        return dataset.getString(Tag.StationName);
    }

    public String getManufacturerModelName() {
        return dataset.getString(Tag.ManufacturerModelName);
    }

    public String getManufacturer() {
        return dataset.getString(Tag.Manufacturer);
    }

    public String getInstitutionalDepartmentName() {
        return dataset.getString(Tag.InstitutionalDepartmentName);
    }

    public String getInstitutionName() {
        return dataset.getString(Tag.InstitutionName);
    }

    public String getSourceAET() {
        return sourceAET;
    }

   public String getDescription() {
        return dataset.getString(Tag.SeriesDescription);
    }

    public Long getNumberOfInstances() {
        return ((TrashListLocal) JNDIUtils.lookup(TrashListLocal.JNDI_NAME)).getNumberOfInstancesOfSeries(this.getPk());
    }

    public Date getPPSStartDatetime() {
        return toDate(Tag.PerformedProcedureStepStartDate, Tag.PerformedProcedureStepStartTime);
    }

    public String getPPSId() {
        return dataset.getString(Tag.PerformedProcedureStepID);
    }

    public String getPPSUid() {
        return dataset.getString(new int[] { 
                Tag.ReferencedPerformedProcedureStepSequence, 0,
                Tag.ReferencedSOPInstanceUID });
    }

    public String getPPSDescription() {
        return dataset.getString(Tag.PerformedProcedureStepDescription);
    }

    public String getAvailability() {
        return dataset.getString(Tag.InstanceAvailability);
    }

    public List<PrivInstanceModel> getInstances() {
        return instances;
    }

    @Override
    public int getRowspan() {
        int rowspan = isDetails() ? 2 : 1;
        for (PrivInstanceModel inst : instances) {
            rowspan += inst.getRowspan();
        }
        return rowspan;
    }

    @Override
    public void collapse() {
        instances.clear();
    }

    @Override
    public boolean isCollapsed() {
        return instances.isEmpty();
    }

    public void retainSelectedInstances() {
        for (Iterator<PrivInstanceModel> it = instances.iterator(); it.hasNext();) {
            PrivInstanceModel inst = it.next();
            if (inst.isCollapsed() && !inst.isSelected()) {
                it.remove();
            }
        }
    }

    @Override
    public void expand() {
        this.instances.clear();
        TrashListLocal dao = (TrashListLocal)
                JNDIUtils.lookup(TrashListLocal.JNDI_NAME);
        for (PrivateInstance inst : dao.findInstancesOfSeries(getPk())) {
            this.instances.add(new PrivInstanceModel(inst, this));
        }
    }

    @Override
    public int levelOfModel() {
        return SERIES_LEVEL;
    }
   
    @Override
    public List<? extends AbstractDicomModel> getDicomModelsOfNextLevel() {
        return instances;
    }
}
