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
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4chee.archive.common.HangingProtocolLevel;
import org.dcm4chee.archive.util.DicomObjectUtils;

/**
 * @author Damien Evans <damien.daddy@gmail.com>
 * @author Justin Falk <jfalkmu@gmail.com>
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision$ $Date$
 * @since Mar 2, 2008
 */
@Entity
@Table(name = "hp")
public class HangingProtocol extends BaseEntity implements Serializable {

    private static final long serialVersionUID = -450985503561915871L;

    @Column(name = "hp_iuid", nullable = false)
    private String sopInstanceUID;

    @Column(name = "hp_cuid")
    private String sopClassUID;

    // JPA definition in orm.xml
    private String name;

    @Column(name = "hp_level")
    private HangingProtocolLevel level;

    @Column(name = "num_priors")
    private int numberOfPriorsReferenced;

    // JPA definition in orm.xml
    private String userGroupName;

    @Column(name = "num_screens")
    private int numberOfScreens;

    // JPA definition in orm.xml
    private byte[] encodedAttributes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_fk")
    private Code userIDCode;

    @OneToMany(mappedBy = "hangingProtocol", fetch=FetchType.LAZY)
    private Set<HPDefinition> definitions;

    public String getSOPInstanceUID() {
        return sopInstanceUID;
    }

    public String getSOPClassUID() {
        return sopClassUID;
    }

    public String getName() {
        return name;
    }

    public HangingProtocolLevel getLevel() {
        return level;
    }

    public int getNumberOfPriorsReferenced() {
        return numberOfPriorsReferenced;
    }

    public String getUserGroupName() {
        return userGroupName;
    }

    public int getNumberOfScreens() {
        return numberOfScreens;
    }

    public byte[] getEncodedAttributes() {
        return encodedAttributes;
    }

    public Code getUserIDCode() {
        return userIDCode;
    }

    public void setUserIDCode(Code userIDCode) {
        this.userIDCode = userIDCode;
    }

    public Set<HPDefinition> getDefinitions() {
        return definitions;
    }

    public void setDefinitions(Set<HPDefinition> definitions) {
        this.definitions = definitions;
    }

    @Override
    public String toString() {
        return "HangingProtocol[pk=" + pk
                + ", iuid=" + sopInstanceUID
                + ", cuid=" + sopClassUID
                + ", name=" + name
                + ", level=" + level
                + ", userGroupName=" + userGroupName
                + ", priors=" + numberOfPriorsReferenced
                + ", screens=" + numberOfScreens
                + "]";
    }

    public DicomObject getAttributes() {
        return DicomObjectUtils.decode(encodedAttributes);
    }

    public void setAttributes(DicomObject attrs) {
        this.sopInstanceUID = attrs.getString(Tag.SOPInstanceUID);
        this.sopClassUID = attrs.getString(Tag.SOPClassUID);
        this.name = attrs.getString(Tag.HangingProtocolName);
        this.level = HangingProtocolLevel.valueOf(attrs
                .getString(Tag.HangingProtocolLevel));
        this.numberOfPriorsReferenced = attrs
                .getInt(Tag.NumberOfPriorsReferenced);
        this.userGroupName = attrs.getString(Tag.HangingProtocolUserGroupName);
        this.numberOfScreens = attrs.getInt(Tag.NumberOfScreens);
        this.encodedAttributes = DicomObjectUtils.encode(attrs,
                UID.DeflatedExplicitVRLittleEndian);
    }
}
