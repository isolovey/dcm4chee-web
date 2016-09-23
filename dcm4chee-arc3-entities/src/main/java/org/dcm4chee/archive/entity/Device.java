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
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

/**
 * @author Damien Evans <damien.daddy@gmail.com>
 * @author Justin Falk <jfalkmu@gmail.com>
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision$ $Date$
 * @since Mar 3, 2008
 */
@Entity
@Table(name = "device")
public class Device extends BaseEntity implements Serializable {

    private static final long serialVersionUID = -4843360661541297998L;

    @Column(name = "station_name", nullable = false)
    private String stationName;

    @Column(name = "station_aet", nullable = false)
    private String stationAET;

    @Column(name = "modality", nullable = false)
    private String modality;

    @ManyToMany(fetch=FetchType.LAZY)
    @JoinTable(
            name="rel_dev_proto",
            joinColumns=
                @JoinColumn(name="prcode_fk", referencedColumnName="pk"),
            inverseJoinColumns=
                @JoinColumn(name="device_fk", referencedColumnName="pk")
        )
    private Set<Code> protocolCodes;

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public String getStationAET() {
        return stationAET;
    }

    public void setStationAET(String stationAET) {
        this.stationAET = stationAET;
    }

    public String getModality() {
        return modality;
    }

    public void setModality(String modality) {
        this.modality = modality;
    }

    public Set<Code> getProtocolCodes() {
        return protocolCodes;
    }

    public void setProtocolCodes(Set<Code> protocolCodes) {
        this.protocolCodes = protocolCodes;
    }

    @Override
    public String toString() {
        return "Device[pk=" + pk
                + ", name=" + stationName
                + ", aet=" + stationAET
                + ", mod=" + modality + "]";
    }
}
