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
import org.dcm4chee.archive.util.DicomObjectUtils;

/**
 * @author Damien Evans <damien.daddy@gmail.com>
 * @author Justin Falk <jfalkmu@gmail.com>
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision$ $Date$
 * @since Apr 14, 2011
 */
@Entity
@Table(name = "ups")
public class UPS extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 5358842743055077420L;

    @Column(name = "created_time")
    private Date createdTime;

    @Column(name = "updated_time")
    private Date updatedTime;

    @Column(name = "ups_iuid", nullable = false)
    private String sopInstanceUID;

    @Column(name = "ups_tuid")
    private String transactionUID;

    @Column(name = "adm_id")
    private String admissionID;

    @Column(name = "adm_id_issuer_id")
    private String issuerOfAdmissionIDLocalNamespaceEntityID;

    @Column(name = "adm_id_issuer_uid")
    private String issuerOfAdmissionIDUniversialEntityID;

    @Column(name = "ups_label", nullable = false)
    private String procedureStepLabel;

    @Column(name = "uwl_label", nullable = false)
    private String worklistLabel;

    @Column(name = "ups_start_time", nullable = false)
    private Date scheduledStartDateTime;

    @Column(name = "ups_compl_time")
    private Date expectedCompletionDateTime;

    @Column(name = "ups_state")
    private int stateAsInt;

    @Column(name = "ups_prior")
    private int priorityAsInt;

    @ManyToMany(fetch=FetchType.LAZY)
    @JoinTable(
            name="rel_ups_devclass",
            joinColumns=
                @JoinColumn(name="ups_fk", referencedColumnName="pk"),
            inverseJoinColumns=
                @JoinColumn(name="devclass_fk", referencedColumnName="pk")
        )
    private Set<Code> scheduledStationClassCodes;
    
    @ManyToMany(fetch=FetchType.LAZY)
    @JoinTable(
            name="rel_ups_devname",
            joinColumns=
                @JoinColumn(name="ups_fk", referencedColumnName="pk"),
            inverseJoinColumns=
                @JoinColumn(name="devname_fk", referencedColumnName="pk")
        )
    private Set<Code> scheduledStationNameCodes;
    
    @ManyToMany(fetch=FetchType.LAZY)
    @JoinTable(
            name="rel_ups_appcode",
            joinColumns=
                @JoinColumn(name="ups_fk", referencedColumnName="pk"),
            inverseJoinColumns=
                @JoinColumn(name="appcode_fk", referencedColumnName="pk")
        )
    private Set<Code> scheduledProcessingApplicationsCodes;
    
    @ManyToMany(fetch=FetchType.LAZY)
    @JoinTable(
            name="rel_ups_devloc",
            joinColumns=
                @JoinColumn(name="ups_fk", referencedColumnName="pk"),
            inverseJoinColumns=
                @JoinColumn(name="devloc_fk", referencedColumnName="pk")
        )
    private Set<Code> scheduledStationGeographicLocationCodes;
    
    @ManyToMany(fetch=FetchType.LAZY)
    @JoinTable(
            name="rel_ups_performer",
            joinColumns=
                @JoinColumn(name="ups_fk", referencedColumnName="pk"),
            inverseJoinColumns=
                @JoinColumn(name="performer_fk", referencedColumnName="pk")
        )
    private Set<Code> scheduledHumanPerformerCodes;
    
    @OneToMany(mappedBy = "ups", fetch=FetchType.LAZY, cascade=CascadeType.REMOVE)
    private Set<UPSRequest> refRequests;
    
    @OneToMany(mappedBy = "ups", fetch=FetchType.LAZY, cascade=CascadeType.REMOVE)
    private Set<UPSRelatedPS> relatedProcedureSteps;

    @OneToMany(mappedBy = "ups", fetch=FetchType.LAZY, cascade=CascadeType.REMOVE)
    private Set<UPSReplacedPS> replacedProcedureSteps;

    @OneToMany(mappedBy = "ups", fetch=FetchType.LAZY, cascade=CascadeType.REMOVE)
    private Set<UPSSubscription> subscriptions;
    
    // JPA definition in orm.xml
    private byte[] encodedAttributes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_fk")
    private Patient patient;

    public Date getCreatedTime() {
        return createdTime;
    }

    public Date getUpdatedTime() {
        return updatedTime;
    }

    public String getSopInstanceUID() {
        return sopInstanceUID;
    }

    public Date getScheduledStartDateTime() {
        return scheduledStartDateTime;
    }

    public String getTransactionUID() {
        return transactionUID;
    }

    public String getAdmissionID() {
        return admissionID;
    }

    public String getIssuerOfAdmissionIDLocalNamespaceEntityID() {
        return issuerOfAdmissionIDLocalNamespaceEntityID;
    }

    public String getIssuerOfAdmissionIDUniversialEntityID() {
        return issuerOfAdmissionIDUniversialEntityID;
    }

    public String getProcedureStepLabel() {
        return procedureStepLabel;
    }

    public String getWorklistLabel() {
        return worklistLabel;
    }

    public Date getExpectedCompletionDateTime() {
        return expectedCompletionDateTime;
    }

    public int getStateAsInt() {
        return stateAsInt;
    }

    public int getPriorityAsInt() {
        return priorityAsInt;
    }

    public Set<Code> getScheduledStationClassCodes() {
        return scheduledStationClassCodes;
    }

    public Set<Code> getScheduledStationNameCodes() {
        return scheduledStationNameCodes;
    }

    public Set<Code> getScheduledProcessingApplicationsCodes() {
        return scheduledProcessingApplicationsCodes;
    }

    public Set<Code> getScheduledStationGeographicLocationCodes() {
        return scheduledStationGeographicLocationCodes;
    }

    public Set<Code> getScheduledHumanPerformerCodes() {
        return scheduledHumanPerformerCodes;
    }

    public Set<UPSRequest> getRefRequests() {
        return refRequests;
    }

    public Set<UPSRelatedPS> getRelatedProcedureSteps() {
        return relatedProcedureSteps;
    }

    public Set<UPSReplacedPS> getReplacedProcedureSteps() {
        return replacedProcedureSteps;
    }

    public Set<UPSSubscription> getSubscriptions() {
        return subscriptions;
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

    @Override
    public String toString() {
        return "UPS[pk=" + pk + ", uid=" + sopInstanceUID + ", admissionID:" + admissionID 
                + ", procedureStepLabel:" + procedureStepLabel + ", worklistLabel:" + worklistLabel
                + ", start=" + scheduledStartDateTime + ", state=" + stateAsInt + "]";
    }

    public void onPrePersist() {
        createdTime = new Date();
    }

    public void onPreUpdate() {
        updatedTime = new Date();
    }

    public DicomObject getAttributes() {
        return DicomObjectUtils.decode(encodedAttributes);
    }

    public void setAttributes(DicomObject attrs) {
        throw new RuntimeException("UPS.setAttributes() is not implemented!");
    }
}
