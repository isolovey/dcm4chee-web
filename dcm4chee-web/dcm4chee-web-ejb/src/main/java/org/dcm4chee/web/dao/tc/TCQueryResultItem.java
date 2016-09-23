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
package org.dcm4chee.web.dao.tc;

import java.io.Serializable;

import org.dcm4chee.archive.entity.Instance;

/**
 * @author Bernhard Ableitinger <bernhard.ableitinger@agfa.com>
 * @version $Revision$ $Date$
 * @since May 04, 2011
 */
public class TCQueryResultItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private Instance instance;

    private String fileId;

    private String fsId;

    private long instancePk;

    private long seriesPk;

    private long studyPk;

    private long patientPk;

    public TCQueryResultItem(Instance instance) {
        this.instance = instance;
        this.instancePk = instance.getPk();
        this.seriesPk = instance.getSeries().getPk();
        this.studyPk = instance.getSeries().getStudy().getPk();
        this.patientPk = instance.getSeries().getStudy().getPatient().getPk();
        this.fileId = instance.getFiles().iterator().next().getFilePath();
        this.fsId = instance.getFiles().iterator().next().getFileSystem()
                .getDirectoryPath();

        instance.getSeries().getSeriesInstanceUID();
        instance.getSeries().getStudy().getStudyInstanceUID();
        instance.getSeries().getStudy().getPatient().getPatientID();
    }

    public Instance getInstance() {
        return instance;
    }

    public long getInstancePk() {
        return instancePk;
    }

    public long getSeriesPk() {
        return seriesPk;
    }

    public long getStudyPk() {
        return studyPk;
    }

    public long getPatientPk() {
        return patientPk;
    }

    public String getFileId() {
        return fileId;
    }

    public String getFileSystemId() {
        return fsId;
    }
}
