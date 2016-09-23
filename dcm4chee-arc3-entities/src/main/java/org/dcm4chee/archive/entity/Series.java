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
 * Accurate Software Design, LLC.
 * Portions created by the Initial Developer are Copyright (C) 2006-2008
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
package org.dcm4chee.archive.entity;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.PersonName;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.VR;
import org.dcm4chee.archive.common.Availability;
import org.dcm4chee.archive.common.PrivateTag;
import org.dcm4chee.archive.common.StorageStatus;
import org.dcm4chee.archive.conf.AttributeFilter;
import org.dcm4chee.archive.exceptions.ConfigurationException;
import org.dcm4chee.archive.util.DicomObjectUtils;

/**
 * @author Damien Evans <damien.daddy@gmail.com>
 * @author Justin Falk <jfalkmu@gmail.com>
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision$ $Date$
 * @since Feb 25, 2008
 */
@Entity
@Table(name = "series")
@NamedQuery(name="Series.findByIUID",
        query="select object(s) from Series s where seriesInstanceUID = :iuid")

public class Series extends BaseEntity implements Serializable {

    private static final long serialVersionUID = -5882522097745649285L;

    @Column(name = "created_time")
    private Date createdTime;

    @Column(name = "updated_time")
    private Date updatedTime;

    @Column(name = "series_iuid", nullable = false)
    private String seriesInstanceUID;

    @Column(name = "series_no")
    private String seriesNumber;

    @Column(name = "series_desc")
    private String seriesDescription;

    @Column(name = "modality")
    private String modality;

    // JPA definition in orm.xml
    private String institutionalDepartmentName;

    // JPA definition in orm.xml
    private String institutionName;

    @Column(name = "station_name")
    private String stationName;

    @Column(name = "body_part")
    private String bodyPartExamined;

    @Column(name = "laterality")
    private String laterality;

    // JPA definition in orm.xml
    private String performingPhysicianName;
    
    public String getPerformingPhysicianFamilyNameSoundex() {
        return performingPhysicianFamilyNameSoundex;
    }

    public void setPerformingPhysicianFamilyNameSoundex(
            String performingPhysicianFamilyNameSoundex) {
        this.performingPhysicianFamilyNameSoundex = performingPhysicianFamilyNameSoundex;
    }

    @Column(name = "perf_phys_fn_sx")
    private String performingPhysicianFamilyNameSoundex;
    
    @Column(name = "perf_phys_gn_sx")
    private String performingPhysicianGivenNameSoundex;

    // JPA definition in orm.xml
    private String performingPhysicianIdeographicName;

    // JPA definition in orm.xml
    private String performingPhysicianPhoneticName;

    @Column(name = "pps_start")
    private Date performedProcedureStepStartDateTime;

    @Column(name = "pps_iuid")
    private String performedProcedureStepInstanceUID;

    @Column(name = "series_custom1")
    private String seriesCustomAttribute1;

    @Column(name = "series_custom2")
    private String seriesCustomAttribute2;

    @Column(name = "series_custom3")
    private String seriesCustomAttribute3;

    // JPA definition in orm.xml
    private byte[] encodedAttributes;
    
    @Column(name = "num_instances", nullable = false)
    private int numberOfSeriesRelatedInstances;

    @Column(name = "src_aet")
    private String sourceAET;

    @Column(name = "retrieve_aets")
    private String retrieveAETs;

    @Column(name = "ext_retr_aet")
    private String externalRetrieveAET;

    @Column(name = "fileset_iuid")
    private String fileSetUID;

    @Column(name = "fileset_id")
    private String fileSetID;

    @Column(name = "availability", nullable = false)
    private Availability availability;

    @Column(name = "series_status", nullable = false)
    private StorageStatus storageStatus;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inst_code_fk")
    private Code institutionCode;

    @OneToMany(mappedBy = "series", fetch = FetchType.LAZY, cascade=CascadeType.REMOVE)
    private Set<RequestAttributes> requestAttributes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mpps_fk")
    private MPPS modalityPerformedProcedureStep;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_fk")
    private Study study;

    @OneToMany(mappedBy = "series", fetch = FetchType.LAZY, cascade=CascadeType.REMOVE)
    private Set<Instance> instances;

    public Date getCreatedTime() {
        return createdTime;
    }

    public Date getUpdatedTime() {
        return updatedTime;
    }

    public String getSeriesInstanceUID() {
        return seriesInstanceUID;
    }

    public String getSeriesNumber() {
        return seriesNumber;
    }

    public String getSeriesDescription() {
        return seriesDescription;
    }

    public String getModality() {
        return modality;
    }

    public String getInstitutionalDepartmentName() {
        return institutionalDepartmentName;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public String getStationName() {
        return stationName;
    }

    public String getBodyPartExamined() {
        return bodyPartExamined;
    }

    public String getLaterality() {
        return laterality;
    }

    public String getPerformingPhysicianName() {
        return performingPhysicianName;
    }

    public String getPerformingPhysicianGivenNameSoundex() {
        return performingPhysicianGivenNameSoundex;
    }

    public void setPerformingPhysicianGivenNameSoundex(
            String performingPhysicianGivenNameSoundex) {
        this.performingPhysicianGivenNameSoundex = performingPhysicianGivenNameSoundex;
    }

    public String getPerformingPhysicianIdeographicName() {
        return performingPhysicianIdeographicName;
    }

    public String getPerformingPhysicianPhoneticName() {
        return performingPhysicianPhoneticName;
    }

    public Date getPerformedProcedureStepStartDateTime() {
        return performedProcedureStepStartDateTime;
    }

    public String getPerformedProcedureStepInstanceUID() {
        return performedProcedureStepInstanceUID;
    }

    public String getSeriesCustomAttribute1() {
        return seriesCustomAttribute1;
    }
    public void setSeriesCustomAttribute1(String value) {
        seriesCustomAttribute1 = value;
    }

    public String getSeriesCustomAttribute2() {
        return seriesCustomAttribute2;
    }
    public void setSeriesCustomAttribute2(String value) {
        seriesCustomAttribute2 = value;
    }

    public String getSeriesCustomAttribute3() {
        return seriesCustomAttribute3;
    }
    public void setSeriesCustomAttribute3(String value) {
        seriesCustomAttribute3 = value;
    }

    public int getNumberOfSeriesRelatedInstances() {
        return numberOfSeriesRelatedInstances;
    }

    public void setNumberOfSeriesRelatedInstances(
            int numberOfSeriesRelatedInstances) {
        this.numberOfSeriesRelatedInstances = numberOfSeriesRelatedInstances;
    }

    public String getSourceAET() {
        return sourceAET;
    }

    public String getRetrieveAETs() {
        return retrieveAETs;
    }

    public void setRetrieveAETs(String retrieveAETs) {
        this.retrieveAETs = retrieveAETs;
    }

    public String getExternalRetrieveAET() {
        return externalRetrieveAET;
    }

    public void setExternalRetrieveAET(String externalRetrieveAET) {
        this.externalRetrieveAET = externalRetrieveAET;
    }

    public String getFileSetUID() {
        return fileSetUID;
    }

    public void setFileSetUID(String fileSetUID) {
        this.fileSetUID = fileSetUID;
    }

    public String getFileSetID() {
        return fileSetID;
    }

    public void setFileSetID(String fileSetID) {
        this.fileSetID = fileSetID;
    }

    public Availability getAvailability() {
        return availability;
    }

    public void setAvailability(Availability availability) {
        this.availability = availability;
    }

    public StorageStatus getStorageStatus() {
        return storageStatus;
    }

    public void setStorageStatus(StorageStatus storageStatus) {
        this.storageStatus = storageStatus;
    }

    public Code getInstitutionCode() {
        return institutionCode;
    }

    public void setInstitutionCode(Code institutionCode) {
        this.institutionCode = institutionCode;
    }

    public byte[] getEncodedAttributes() {
        return encodedAttributes;
    }

    public Set<RequestAttributes> getRequestAttributes() {
        return requestAttributes;
    }

    public void setRequestAttributes(Set<RequestAttributes> requestAttributes) {
        this.requestAttributes = requestAttributes;
    }

    public MPPS getModalityPerformedProcedureStep() {
        return modalityPerformedProcedureStep;
    }

    public void setModalityPerformedProcedureStep(
            MPPS modalityPerformedProcedureStep) {
        this.modalityPerformedProcedureStep = modalityPerformedProcedureStep;
    }

    public Study getStudy() {
        return study;
    }

    public void setStudy(Study study) {
        this.study = study;
    }

    public Set<Instance> getInstances() {
        return instances;
    }

    @Override
    public String toString() {
        return "Series[pk=" + pk
                + ", uid=" + seriesInstanceUID
                + ", serno=" + seriesNumber
                + ", desc=" + seriesDescription
                + ", mod=" + modality
                + ", station=" + stationName
                + ", department=" + institutionalDepartmentName
                + ", institution=" + institutionName
                + ", srcaet=" + sourceAET
                + ", bodypart=" + bodyPartExamined
                + ", laterality=" + laterality
                + ", performer=" + performingPhysicianName
                + ", start=" + performedProcedureStepStartDateTime
                + ", ppsuid=" + performedProcedureStepInstanceUID
                + ", numinsts=" + numberOfSeriesRelatedInstances
                + ", status=" + storageStatus
                + ", avail=" + availability
                + ", aets=" + retrieveAETs
                + ", extaet=" + externalRetrieveAET
                + ", fsid=" + fileSetID
                + ", fsuid=" + fileSetUID
                + "]";
    }

    public void onPrePersist() {
        createdTime = new Date();
    }

    public void onPreUpdate() {
        updatedTime = new Date();
    }

    public DicomObject getAttributes(boolean cfindrsp) {
        DicomObject dataset = DicomObjectUtils.decode(encodedAttributes);
        if (cfindrsp) {
            dataset.putInt(Tag.NumberOfSeriesRelatedInstances, VR.IS,
                    numberOfSeriesRelatedInstances);
            if (fileSetUID != null && fileSetID != null) {
                dataset.putString(Tag.StorageMediaFileSetUID, VR.UI, fileSetUID);
                dataset.putString(Tag.StorageMediaFileSetID, VR.SH, fileSetID);
            }
            if (retrieveAETs != null || externalRetrieveAET != null) {
                dataset.putString(Tag.RetrieveAETitle, VR.AE, 
                        externalRetrieveAET == null ? retrieveAETs
                                : retrieveAETs == null ? externalRetrieveAET
                                : retrieveAETs + '\\' + externalRetrieveAET);
            }
            dataset.putString(Tag.InstanceAvailability, VR.CS,
                    availability.name());
        }
        return dataset;
    }

    public void setAttributes(DicomObject attrs) {
        AttributeFilter filter = AttributeFilter.getSeriesAttributeFilter();
        this.seriesInstanceUID = attrs.getString(Tag.SeriesInstanceUID);
        this.seriesNumber = attrs.getString(Tag.SeriesNumber, "");
        this.seriesDescription = filter.toUpperCase(attrs.getString(Tag.SeriesDescription, ""), Tag.SeriesDescription);
        this.modality = filter.toUpperCase(attrs.getString(Tag.Modality, ""), Tag.Modality);
        this.institutionalDepartmentName = filter.toUpperCase(attrs.getString(
                Tag.InstitutionalDepartmentName, ""), Tag.InstitutionalDepartmentName);
        this.institutionName = filter.toUpperCase(attrs.getString(Tag.InstitutionName, ""), Tag.InstitutionName);
        this.stationName = filter.toUpperCase(attrs.getString(Tag.StationName, ""), Tag.StationName);
        String srcAET = attrs.getString(attrs.resolveTag(
                PrivateTag.CallingAET, PrivateTag.CreatorID));
        if (srcAET != null && srcAET.trim().length() > 1)
            this.sourceAET = srcAET;
        this.bodyPartExamined = filter.toUpperCase(attrs.getString(Tag.BodyPartExamined, ""), Tag.BodyPartExamined);
        this.laterality = filter.toUpperCase(attrs.getString(Tag.Laterality, ""), Tag.Laterality);
        PersonName pn = new PersonName(attrs
                .getString(Tag.PerformingPhysicianName));
        this.performingPhysicianName = pn.componentGroupString(
                PersonName.SINGLE_BYTE, false).toUpperCase();
        this.performingPhysicianIdeographicName = pn.componentGroupString(
                PersonName.IDEOGRAPHIC, false);
        this.performingPhysicianPhoneticName = pn.componentGroupString(
                PersonName.PHONETIC, false);
        if (AttributeFilter.isSoundexEnabled()) {
            this.performingPhysicianFamilyNameSoundex = AttributeFilter.toSoundex(pn, PersonName.FAMILY, "*");
            this.performingPhysicianGivenNameSoundex = AttributeFilter.toSoundex(pn, PersonName.GIVEN, "*");
        }
        this.performedProcedureStepStartDateTime = attrs.getDate(
                Tag.PerformedProcedureStepStartDate,
                Tag.PerformedProcedureStepStartTime);
        this.performedProcedureStepInstanceUID = attrs.getString(new int[] {
                Tag.ReferencedPerformedProcedureStepSequence, 0,
                Tag.ReferencedSOPInstanceUID });
        int[] fieldTags = filter.getFieldTags();
        for (int i = 0; i < fieldTags.length; i++) {
            try {
                setField(filter.getField(fieldTags[i]), attrs.getString(fieldTags[i], ""));
            } catch (Exception e) {
                throw new ConfigurationException(e);
            }
        }
        this.encodedAttributes = DicomObjectUtils.encode(filter.filter(attrs),
                filter.getTransferSyntaxUID());
    }
    
    private void setField(String field, String value ) {
        try {
            Method m = Series.class.getMethod("set" 
                    + Character.toUpperCase(field.charAt(0))
                    + field.substring(1), new Class[]{String.class});
            m.invoke(this, new Object[] { value });
        } catch (Exception e) {
            throw new ConfigurationException(e);
        }       
    }

}
