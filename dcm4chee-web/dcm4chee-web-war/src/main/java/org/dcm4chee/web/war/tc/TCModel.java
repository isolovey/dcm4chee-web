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
package org.dcm4chee.web.war.tc;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.dcm4che2.data.Tag;
import org.dcm4chee.archive.entity.File;
import org.dcm4chee.archive.entity.Instance;
import org.dcm4chee.archive.entity.Patient;
import org.dcm4chee.archive.entity.Series;
import org.dcm4chee.archive.entity.Study;
import org.dcm4chee.web.war.common.model.AbstractDicomModel;
import org.dcm4chee.web.war.folder.model.InstanceModel;
import org.dcm4chee.web.war.folder.model.PatientModel;
import org.dcm4chee.web.war.folder.model.SeriesModel;
import org.dcm4chee.web.war.folder.model.StudyModel;

/**
 * @author Bernhard Ableitinger <bernhard.ableitinger@agfa.com>
 * @version $Revision$ $Date$
 * @since May 04, 2011
 */
public class TCModel extends InstanceModel {

    private static final long serialVersionUID = 1L;
    
    private String fsId;

    private String fileId;

    public TCModel(Instance instance) {
        super(instance, null, null);

        dataset = instance.getAttributes(false);

        List<File> files = instance.getFiles();
        if (files != null && !files.isEmpty()) {
            fsId = files.get(0).getFileSystem().getDirectoryPath();
            fileId = files.get(0).getFilePath();
        }

        Series series = instance.getSeries();
        series.getAttributes(false).copyTo(dataset);
        Study study = series.getStudy();
        study.getAttributes(false).copyTo(dataset);
        Patient patient = study.getPatient();
        patient.getAttributes().copyTo(dataset);
        patient.setModalityPerformedProcedureSteps(null);
        
        SeriesModel seriesModel = new SeriesModel(series, null, null);
        seriesModel.setParent(new StudyModel(series.getStudy(),
                new PatientModel(patient, null), null));
        setParent(seriesModel);
    }

    public String getId() {
    	return Long.toString(getPk());
    }
    
    public String getURL() {
    	return TCCaseViewPage.urlForCase(getSOPInstanceUID());
    }
    
    public String getTitle() {
        return dataset.getString(Tag.ContentLabel);
    }

    public String getAuthor() {
        return dataset.getString(Tag.ContentCreatorName);
    }

    public String getAbstract() {
        return dataset.getString(Tag.ContentDescription);
    }

    public Date getCreationDate() {
        return dataset.getDate(Tag.ContentDate);
    }

    public String getStudyInstanceUID() {
        return dataset.getString(Tag.StudyInstanceUID);
    }

    public String getSeriesInstanceUID() {
        return dataset.getString(Tag.SeriesInstanceUID);
    }
    
    public String getPatientId() {
        return dataset.getString(Tag.PatientID);
    }
    
    public String getIssuerOfPatientId() {
        return dataset.getString(Tag.IssuerOfPatientID);
    }

    public String getFileSystemId() {
        return fsId;
    }

    public String getFileId() {
        return fileId;
    }

    @Override
    public int getRowspan() {
        return 0;
    }

    @Override
    public void expand() {
    }

    @Override
    public void collapse() {
    }

    @Override
    public boolean isCollapsed() {
        return false;
    }

    @Override
    public List<? extends AbstractDicomModel> getDicomModelsOfNextLevel() {
        return Collections.emptyList();
    }

    @Override
    public int levelOfModel() {
        return INSTANCE_LEVEL;
    }

    public static enum Sorter {
        Title, Author, Abstract, Date;

        private Comparator<TCModel> asc;

        private Comparator<TCModel> desc;

        public Comparator<TCModel> getComparator(boolean ascending) {
            if (ascending) {
                if (asc == null) {
                    asc = createComparator(true);
                }

                return asc;
            } else {
                if (desc == null) {
                    desc = createComparator(false);
                }

                return desc;
            }
        }

        private int compare(TCModel item1, TCModel item2, boolean ascending) {
            if (Title.equals(this)) {
                return compare(item1.getTitle(), item2.getTitle(), ascending);
            } else if (Author.equals(this)) {
                return compare(item1.getAuthor(), item2.getAuthor(), ascending);
            } else if (Abstract.equals(this)) {
                return compare(item1.getAbstract(), item2.getAbstract(),
                        ascending);
            } else if (Date.equals(this)) {
                return compare(item1.getCreationDate(),
                        item2.getCreationDate(), ascending);
            }

            return -1;
        }

        private int compare(String s1, String s2, boolean ascending) {
            if (ascending) {
                return s2 == null ? -1 : s1 == null ? 1 : s1.compareTo(s2);
            } else {
                return s1 == null ? -1 : s2 == null ? 1 : s2.compareTo(s1);
            }
        }

        private int compare(Date d1, Date d2, boolean ascending) {
            if (ascending) {
                return d2 == null ? -1 : d1 == null ? 1 : d1.compareTo(d2);
            } else {
                return d1 == null ? -1 : d2 == null ? 1 : d2.compareTo(d1);
            }
        }

        private Comparator<TCModel> createComparator(final boolean ascending) {
            return new Comparator<TCModel>() {
                @Override
                public int compare(TCModel item1, TCModel item2) {
                    return Sorter.this.compare(item1, item2, ascending);
                }
            };
        }
    }

}
