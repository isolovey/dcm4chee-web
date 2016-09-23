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
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.PersonName;
import org.dcm4che2.data.Tag;
import org.dcm4chee.archive.conf.AttributeFilter;
import org.dcm4chee.archive.exceptions.ConfigurationException;
import org.dcm4chee.archive.util.DicomObjectUtils;
import org.hibernate.annotations.Cascade;

/**
 * @author Damien Evans <damien.daddy@gmail.com>
 * @author Justin Falk <jfalkmu@gmail.com>
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision$ $Date$
 * @since Feb 23, 2008
 */
@Entity
@Table(name = "patient")
public class Patient extends BaseEntity implements Serializable {

    private static final long serialVersionUID = -1348274766865261645L;

    @Column(name = "created_time")
    private Date createdTime;

    @Column(name = "updated_time")
    private Date updatedTime;

    @Column(name = "pat_id")
    private String patientID;

    @Column(name = "pat_id_issuer")
    private String issuerOfPatientID;

    // JPA definition in orm.xml
    private String patientName;
    
    @Column(name = "pat_fn_sx")
    private String patientFamilyNameSoundex;
    
    @Column(name = "pat_gn_sx")
    private String patientGivenNameSoundex;

    // JPA definition in orm.xml
    private String patientIdeographicName;

    // JPA definition in orm.xml
    private String patientPhoneticName;

    @Column(name = "pat_birthdate")
    private String patientBirthDate;

    @Column(name = "pat_sex")
    private String patientSex;

    @Column(name = "pat_custom1")
    private String patientCustomAttribute1;

    @Column(name = "pat_custom2")
    private String patientCustomAttribute2;

    @Column(name = "pat_custom3")
    private String patientCustomAttribute3;

    // JPA definition in orm.xml
    private byte[] encodedAttributes;

    @ManyToMany(cascade = CascadeType.ALL, fetch=FetchType.LAZY)
    @JoinTable(
            name = "rel_pat_other_pid", 
            joinColumns = @JoinColumn(name = "patient_fk", referencedColumnName = "pk"), 
            inverseJoinColumns = @JoinColumn(name = "other_pid_fk", referencedColumnName = "pk"))
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    private Set<OtherPatientID> otherPatientIDs;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "merge_fk")
    private Patient mergedWith;

    @OneToMany(mappedBy = "mergedWith", fetch=FetchType.LAZY, cascade=CascadeType.REMOVE)
    private Set<Patient> previous;

    @OneToMany(mappedBy = "patient", fetch=FetchType.LAZY, cascade=CascadeType.REMOVE)
    private Set<Study> studies;

    @OneToMany(mappedBy = "patient", fetch=FetchType.LAZY, cascade=CascadeType.REMOVE)
    private Set<MWLItem> modalityWorklistItems;

    @OneToMany(mappedBy = "patient", fetch=FetchType.LAZY, cascade=CascadeType.REMOVE)
    private Set<MPPS> modalityPerformedProcedureSteps;

    @OneToMany(mappedBy = "patient", fetch=FetchType.LAZY, cascade=CascadeType.REMOVE)
    private Set<GPSPS> generalPurposeScheduledProcedureSteps;

    @OneToMany(mappedBy = "patient", fetch=FetchType.LAZY, cascade=CascadeType.REMOVE)
    private Set<GPPPS> generalPurposePerformedProcedureSteps;

    @OneToMany(mappedBy = "patient", fetch=FetchType.LAZY, cascade=CascadeType.REMOVE)
    private Set<UPS> unifiedProcedureSteps;
    
    public Date getCreatedTime() {
        return createdTime;
    }

    public Date getUpdatedTime() {
        return updatedTime;
    }

    public String getPatientID() {
        return patientID;
    }
    
    public void setPatientID(String patientID) {
        this.patientID = patientID;
    }

    public String getIssuerOfPatientID() {
        return issuerOfPatientID;
    }
    
    public void setIssuerOfPatientID(String issuer) {
        this.issuerOfPatientID = issuer;
    }

    public String getPatientName() {
        return patientName;
    }
    
    public void setPatientName(String name) {
        this.patientName = name;
    }

    public String getPatientFamilyNameSoundex() {
        return patientFamilyNameSoundex;
    }

    public void setPatientFamilyNameSoundex(String patientFamilyNameSoundex) {
        this.patientFamilyNameSoundex = patientFamilyNameSoundex;
    }

    public String getPatientGivenNameSoundex() {
        return patientGivenNameSoundex;
    }

    public void setPatientGivenNameSoundex(String patientGivenNameSoundex) {
        this.patientGivenNameSoundex = patientGivenNameSoundex;
    }

    public String getPatientIdeographicName() {
        return patientIdeographicName;
    }
    
    public void setPatientIdeographicName(String name) {
        this.patientIdeographicName = name;
    }

    public String getPatientPhoneticName() {
        return patientPhoneticName;
    }
    
    public void setPatientPhoneticName(String name) {
        this.patientPhoneticName = name;
    }

    public String getPatientBirthDate() {
        return patientBirthDate;
    }
    
    public void setPatientBirthDate(String dob) {
        this.patientBirthDate = dob;
    }

    public String getPatientSex() {
        return patientSex;
    }
    
    public void setPatientSex(String patientSex) {
        this.patientSex = patientSex;
    }

    public String getPatientCustomAttribute1() {
        return patientCustomAttribute1;
    }
    
    public void setPatientCustomAttribute1(String attr) {
        this.patientCustomAttribute1 = attr;
    }

    public String getPatientCustomAttribute2() {
        return patientCustomAttribute2;
    }
    
    public void setPatientCustomAttribute2(String attr) {
        this.patientCustomAttribute2 = attr;
    }

    public String getPatientCustomAttribute3() {
        return patientCustomAttribute3;
    }
    
    public void setPatientCustomAttribute3(String attr) {
        this.patientCustomAttribute3 = attr;
    }

    public byte[] getEncodedAttributes() {
        return encodedAttributes;
    }

    public Set<OtherPatientID> getOtherPatientIDs() {
        return otherPatientIDs;
    }

    public void setOtherPatientIDs(Set<OtherPatientID> otherPatientIDs) {
        this.otherPatientIDs = otherPatientIDs;
    }

    public Patient getMergedWith() {
        return mergedWith;
    }

    public void setMergedWith(Patient mergedWith) {
        this.mergedWith = mergedWith;
    }

    public Set<Patient> getPrevious() {
        return previous;
    }

    public void setPrevious(Set<Patient> previous) {
        this.previous = previous;
    }

    public Set<Study> getStudies() {
        return studies;
    }
    
    public void setStudies(Set<Study> studies) {
        this.studies = studies;
    }

    public Set<MWLItem> getModalityWorklistItems() {
        return modalityWorklistItems;
    }
    
    public void setModalityWorklistItems(Set<MWLItem> modalityWorklistItems) {
        this.modalityWorklistItems = modalityWorklistItems;
    }

    public Set<MPPS> getModalityPerformedProcedureSteps() {
        return modalityPerformedProcedureSteps;
    }
    
    public void setModalityPerformedProcedureSteps(Set<MPPS> mpps) {
        this.modalityPerformedProcedureSteps = mpps;
    }

    public Set<GPSPS> getGeneralPurposeScheduledProcedureSteps() {
        return generalPurposeScheduledProcedureSteps;
    }
    
    public void setGeneralPurposeScheduledProcedureSteps(Set<GPSPS> gpsps) {
        this.generalPurposeScheduledProcedureSteps = gpsps;
    }

    public Set<GPPPS> getGeneralPurposePerformedProcedureSteps() {
        return generalPurposePerformedProcedureSteps;
    }
    
    public void setGeneralPurposePerformedProcedureSteps(Set<GPPPS> gppps) {
        this.generalPurposePerformedProcedureSteps = gppps;
    }

    public Set<UPS> getUnifiedProcedureSteps() {
        return unifiedProcedureSteps;
    }
    
    @Override
    public String toString() {
        return "Patient[pk=" + pk
                + ", pid=" + (issuerOfPatientID != null
                        ? patientID + "^^^" + issuerOfPatientID
                        : patientID)
                + ", name=" + patientName
                + ", birthdate=" + patientBirthDate
                + ", sex=" + patientSex
                + "]";
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
        this.patientID = attrs.getString(Tag.PatientID, "");
        this.issuerOfPatientID = attrs.getString(Tag.IssuerOfPatientID);
        PersonName pn = new PersonName(attrs.getString(Tag.PatientName));
        this.patientName = pn.componentGroupString(PersonName.SINGLE_BYTE,
                false).toUpperCase();
        this.patientIdeographicName = pn.componentGroupString(
                PersonName.IDEOGRAPHIC, false);
        this.patientPhoneticName = pn.componentGroupString(PersonName.PHONETIC, false);
        if (AttributeFilter.isSoundexEnabled()) {
            this.patientFamilyNameSoundex = AttributeFilter.toSoundex(pn, PersonName.FAMILY, "*");
            this.patientGivenNameSoundex = AttributeFilter.toSoundex(pn, PersonName.GIVEN, "*");
        }
        this.patientBirthDate = normalizeDA(attrs.getString(Tag.PatientBirthDate));
        this.patientSex = attrs.getString(Tag.PatientSex, "");
        AttributeFilter filter = AttributeFilter.getPatientAttributeFilter();
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

    private static String normalizeDA(String s) {
        if (s == null) {
            return null;
        }
        String trim = s.trim();
        int l = trim.length();
        if (l == 0) {
            return null;
        }
        if (l == 10 && trim.charAt(4) == '-' && trim.charAt(7) == '-') {
            StringBuilder sb = new StringBuilder(8);
            sb.append(trim.substring(0, 4));
            sb.append(trim.substring(5, 7));
            sb.append(trim.substring(8));
            return sb.toString();
        }
        return trim;
    }
    
    /**
     * Given an issuer, find the corresponding patient id.
     * 
     * @param issuer
     *                A String containing the Issuer of Patient ID
     * @return {@link OtherPatientID} The matching patient id, or null if no
     *         matches were found.
     */
    public OtherPatientID getOtherPatientIDForIssuer(String issuer) {
        if (otherPatientIDs == null || otherPatientIDs.size() == 0) {
            return null;
        }

        for (OtherPatientID opid : otherPatientIDs) {
            if (issuer.equals(opid.getIssuerOfPatientID())) {
                return opid;
            }
        }

        return null;
    }

    /**
     * Given an issuer, find the corresponding patient id. This method
     * encompasses both the primary patient id of this entity, plus other
     * patient ids.
     * 
     * @param issuer
     *                A String containing the Issuer of Patient ID
     * @return String The matching patient id, or null if no matches were found.
     */
    public String getPatientIDForIssuer(String issuer) {
        if (issuer == null || issuer.length() == 0)
            return patientID;

        if (issuerOfPatientID != null) {
            if (issuerOfPatientID.equals(issuer)) {
                return patientID;
            }
        }

        if (otherPatientIDs == null || otherPatientIDs.size() == 0) {
            return null;
        }

        for (OtherPatientID opid : otherPatientIDs) {
            if (issuer.equals(opid.getIssuerOfPatientID())) {
                return opid.getPatientID();
            }
        }

        return null;
    }
    
    private void setField(String field, String value ) {
        try {
            Method m = Patient.class.getMethod("set" 
                    + Character.toUpperCase(field.charAt(0))
                    + field.substring(1), new Class[]{String.class});
            m.invoke(this, new Object[] { value });
        } catch (Exception e) {
            throw new ConfigurationException(e);
        }       
    }

}
