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

import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.VR;
import org.dcm4chee.archive.entity.Code;

/**
 * @author Bernhard Ableitinger <bernhard.ableitinger@agfa.com>
 * @version $Revision$ $Date$
 * @since May 05, 2011
 */
public enum TCQueryFilterKey {
    Abstract("IHERADTF", "TCE104", "Abstract"), AcquisitionModality("DCM",
            "121139", "Modality"), Anatomy("DCM", "112005",
            "Radiographic Anatomy", true), AuthorAffiliation("IHERADTF",
            "TCE102", "Affiliation"), AuthorContact("IHERADTF", "TCE103",
            "Contact"), AuthorName("IHERADTF", "TCE101", "Author"), Category(
            "IHERADTF", "TCE109", "Category"), Diagnosis("IHERADTF", "TCE107",
            "Diagnosis", true), DiagnosisConfirmed("IHERADTF", "TCE111",
            "Diagnosis confirmed"), DifferentialDiagnosis("DCM", "111023",
            "Differential Diagnosis/Impression", true), Discussion("IHERADTF",
            "TCE106", "Discussion"), Finding("DCM", "121071", "Finding", true), History(
            "DCM", "121060", "History"), Keyword("IHERADTF", "TCE105",
            "Keywords", true), Level("IHERADTF", "TCE110", "Level"), OrganSystem(
            "IHERADTF", "TCE108", "Organ system"), Pathology("DCM", "111042",
            "Pathology", true), PatientAge("AGFAIMPAXEE", "TF101",
            "Patient Age"), PatientSex("AGFAIMPAXEE", "TF102",
            "Patient Sex"), PatientSpecies("AGFAIMPAXEE", "TF103",
            "Patient Species"), BibliographicReference("AGFAIMPAXEE", "TF005",
            "Bibliographic Reference"), Title("AGFAIMPAXEE", "TF001", "Title"),
            CreationDate(Tag.ContentDate);

    private Code code;
    private int dicomTag;
    private boolean supportsCodeValue;

    private TCQueryFilterKey(int dicomTag) {
        this.dicomTag = dicomTag;
        this.supportsCodeValue = false;
    }
    
    private TCQueryFilterKey(String designator, String value, String meaning) {
        this(designator, value, meaning, false);
    }

    private TCQueryFilterKey(String designator, String value, String meaning,
            boolean supportsCodeValue) {
        this.code = createCode(designator, value, meaning);
        this.supportsCodeValue = supportsCodeValue;
    }
    
    public int getDicomTag() {
    	return dicomTag;
    }

    public Code getCode() {
        return code;
    }

    public boolean supportsCodeValue() {
        return supportsCodeValue;
    }

    private static Code createCode(String designator, String value,
            String meaning) {
        DicomObject dataset = new BasicDicomObject();
        dataset.putString(Tag.CodingSchemeDesignator, VR.SH, designator);
        dataset.putString(Tag.CodeValue, VR.SH, value);
        dataset.putString(Tag.CodeMeaning, VR.LO, meaning);

        return new Code(dataset);
    }

}
