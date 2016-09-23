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
import java.util.Date;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4chee.archive.common.GPSPSPriority;
import org.dcm4chee.archive.common.GPSPSStatus;
import org.dcm4chee.archive.common.InputAvailabilityFlag;
import org.dcm4chee.archive.conf.AttributeFilter;
import org.dcm4chee.archive.util.DicomObjectUtils;

/**
 * @author Damien Evans <damien.daddy@gmail.com>
 * @author Justin Falk <jfalkmu@gmail.com>
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision$ $Date$
 * @since Mar 1, 2008
 */
@Entity
@Table(name = "gpsps")
public class GPSPS extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 4800913651614346013L;

    @Column(name = "gpsps_iuid", nullable = false)
    private String sopInstanceUID;

    @Column(name = "gpsps_tuid")
    private String transactionUID;

    @Column(name = "start_datetime", nullable = false)
    private Date startDateTime;

    @Column(name = "end_datetime")
    private Date expectedCompletionDateTime;

    @Column(name = "gpsps_status")
    private GPSPSStatus status;

    @Column(name = "gpsps_prior")
    private GPSPSPriority priority;

    @Column(name = "in_availability")
    private InputAvailabilityFlag inputAvailability;

    // JPA definition in orm.xml
    private byte[] encodedAttributes;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "patient_fk")
    private Patient patient;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "code_fk")
    private Code scheduledWorkItemCode;

    @ManyToMany(fetch=FetchType.LAZY)
    @JoinTable(
            name="rel_gpsps_appcode",
            joinColumns=
                @JoinColumn(name="appcode_fk", referencedColumnName="pk"),
            inverseJoinColumns=
                @JoinColumn(name="gpsps_fk", referencedColumnName="pk")
        )
    private Set<Code> scheduledProcessingApplicationsCodes;

    @ManyToMany(fetch=FetchType.LAZY)
    @JoinTable(
            name="rel_gpsps_devname",
            joinColumns=
                @JoinColumn(name="devname_fk", referencedColumnName="pk"),
            inverseJoinColumns=
                @JoinColumn(name="gpsps_fk", referencedColumnName="pk")
        )
    private Set<Code> scheduledStationNameCodes;

    @ManyToMany(fetch=FetchType.LAZY)
    @JoinTable(
            name="rel_gpsps_devclass",
            joinColumns=
                @JoinColumn(name="devclass_fk", referencedColumnName="pk"),
            inverseJoinColumns=
                @JoinColumn(name="gpsps_fk", referencedColumnName="pk")
        )
    private Set<Code> scheduledStationClassCodes;

    @ManyToMany(fetch=FetchType.LAZY)
    @JoinTable(
            name="rel_gpsps_devloc",
            joinColumns=
                @JoinColumn(name="devloc_fk", referencedColumnName="pk"),
            inverseJoinColumns=
                @JoinColumn(name="gpsps_fk", referencedColumnName="pk")
        )
    private Set<Code> scheduledStationGeographicLocationCodes;

    @OneToMany(mappedBy = "gpsps", fetch=FetchType.LAZY, cascade=CascadeType.REMOVE)
    private Set<GPSPSRequest> referencedRequests;

    @OneToMany(mappedBy = "gpsps", fetch=FetchType.LAZY, cascade=CascadeType.REMOVE)
    private Set<GPSPSPerformer> scheduledHumanPerformers;

    @ManyToMany(mappedBy = "scheduledProcedureSteps", fetch=FetchType.LAZY, cascade=CascadeType.REMOVE)
    private Set<GPPPS> performedProcedureSteps;

    public String getSOPInstanceUID() {
        return sopInstanceUID;
    }

    public String getTransactionUID() {
        return transactionUID;
    }

    public Date getStartDateTime() {
        return startDateTime;
    }

    public Date getExpectedCompletionDateTime() {
        return expectedCompletionDateTime;
    }

    public GPSPSStatus getStatus() {
        return status;
    }

    public GPSPSPriority getPriority() {
        return priority;
    }

    public InputAvailabilityFlag getInputAvailability() {
        return inputAvailability;
    }

    public byte[] getEncodedAttributes() {
        return encodedAttributes;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Code getScheduledWorkItemCode() {
        return scheduledWorkItemCode;
    }

    public void setScheduledWorkItemCode(Code scheduledWorkItemCode) {
        this.scheduledWorkItemCode = scheduledWorkItemCode;
    }

    public Set<Code> getScheduledProcessingApplicationsCodes() {
        return scheduledProcessingApplicationsCodes;
    }

    public void setScheduledProcessingApplicationsCodes(
            Set<Code> scheduledProcessingApplicationsCodes) {
        this.scheduledProcessingApplicationsCodes = scheduledProcessingApplicationsCodes;
    }

    public Set<Code> getScheduledStationNameCodes() {
        return scheduledStationNameCodes;
    }

    public void setScheduledStationNameCodes(Set<Code> scheduledStationNameCodes) {
        this.scheduledStationNameCodes = scheduledStationNameCodes;
    }

    public Set<Code> getScheduledStationClassCodes() {
        return scheduledStationClassCodes;
    }

    public void setScheduledStationClassCodes(Set<Code> codes) {
        this.scheduledStationClassCodes = codes;
    }

    public Set<Code> getScheduledStationGeographicLocationCodes() {
        return scheduledStationGeographicLocationCodes;
    }

    public void setScheduledStationGeographicLocationCodes(Set<Code> codes) {
        this.scheduledStationGeographicLocationCodes = codes;
    }

    public Set<GPSPSRequest> getReferencedRequests() {
        return referencedRequests;
    }

    public void setReferencedRequests(Set<GPSPSRequest> referencedRequests) {
        this.referencedRequests = referencedRequests;
    }

    public Set<GPSPSPerformer> getScheduledHumanPerformers() {
        return scheduledHumanPerformers;
    }

    public void setScheduledHumanPerformers(
            Set<GPSPSPerformer> scheduledHumanPerformers) {
        this.scheduledHumanPerformers = scheduledHumanPerformers;
    }

    public Set<GPPPS> getPerformedProcedureSteps() {
        return performedProcedureSteps;
    }

    @Override
    public String toString() {
        return "GPSPS[pk=" + pk
                + ", iuid=" + sopInstanceUID
                + ", transuid=" + transactionUID
                + ", status=" + status
                + ", start=" + startDateTime
                + ", complete=" + expectedCompletionDateTime
                + ", priority=" + priority
                + ", input=" + inputAvailability
                + "]";
    }

    public DicomObject getAttributes() {
        return DicomObjectUtils.decode(encodedAttributes);
    }

    public void setAttributes(DicomObject attrs) {
        this.sopInstanceUID = attrs.getString(Tag.SOPInstanceUID);
        this.startDateTime = attrs
                .getDate(Tag.ScheduledProcedureStepStartDateTime);
        this.expectedCompletionDateTime = attrs
                .getDate(Tag.ExpectedCompletionDateTime);
        this.status = GPSPSStatus.valueOf(attrs.getString(
                Tag.GeneralPurposeScheduledProcedureStepStatus).replace(' ',
                '_'));
        this.priority = GPSPSPriority.valueOf(attrs
                .getString(Tag.GeneralPurposeScheduledProcedureStepPriority));
        this.inputAvailability = InputAvailabilityFlag.valueOf(attrs
                .getString(Tag.InputAvailabilityFlag));
        this.encodedAttributes = DicomObjectUtils.encode(AttributeFilter.getExcludePatientAttributeFilter().filter(attrs),
                UID.DeflatedExplicitVRLittleEndian);
    }
}
