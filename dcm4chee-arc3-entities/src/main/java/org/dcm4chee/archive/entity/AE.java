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
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.NamedQuery;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;


/**
 * @author Damien Evans <damien.daddy@gmail.com>
 * @author Justin Falk <jfalkmu@gmail.com>
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * 
 * @version $Revision$ $Date$
 * @since Mar 3, 2008
 */
@Entity
@Table(name = "ae")
@NamedQuery(name="AE.findByTitle",
  query="select ae from AE ae where title = :title"
)
public class AE extends BaseEntity implements Serializable {

    private static final long serialVersionUID = -3132017392325052134L;

    @Column(name = "aet", nullable = false)
    private String title;

    @Column(name = "hostname", nullable = false)
    private String hostName;

    @Column(name = "port", nullable = false)
    private int port;

    @Column(name = "cipher_suites")
    private String cipherSuites;

    @Column(name = "pat_id_issuer")
    private String issuerOfPatientID;

    @Column(name = "acc_no_issuer")
    private String issuerOfAccessionNumber;

    @Column(name = "user_id")
    private String userID;

    @Column(name = "passwd")
    private String password;

    @Column(name = "fs_group_id")
    private String fileSystemGroupID;

    @Column(name = "ae_desc")
    private String description;

    @Column(name = "station_name")
    private String stationName;

    @Column(name = "institution")
    private String institution;

    @Column(name = "department")
    private String department;

    @Column(name = "installed")
    private boolean installed;

    @Column(name = "wado_url")
    private String wadoURL;

    @Column(name = "ae_group")
    private String aeGroup;
    
    @PrePersist
    @PreUpdate
    public void filterCiphers() {
        if (this.cipherSuites == null) return;
        this.cipherSuites = this.cipherSuites.replaceAll("-,", "").replaceAll(",-", "");
        if (this.cipherSuites.length() == 1) 
            this.cipherSuites = this.cipherSuites.replace("-", "");
    }
   
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public List<String> getCipherSuites() {
        List<String> l = new ArrayList<String>();
        if ( cipherSuites != null && cipherSuites.trim().length()>0) {
            int pos0=0, pos1;
            for ( int i = 0 ; i < 3 ; i++ ) {
                pos1 = cipherSuites.indexOf(',', pos0);
                if ( pos1 == -1) {
                    l.add(cipherSuites.substring(pos0));
                    break;
                } else {
                    l.add(cipherSuites.substring(pos0, pos1));
                    pos0 = ++pos1;
                }
            }
        }
        return l;
    }

    public void setCipherSuites(List<String> suites) {
        if (suites == null) 
            cipherSuites = "";
        else {
            StringBuilder sb = new StringBuilder();
            for (String s : suites) {
                if (s != null) {
                    sb.append(s).append(',');
                }
            }
            if (sb.length()>0) {
                sb.setLength(sb.length()-1);
                cipherSuites = sb.toString();
            } else {
                cipherSuites = null;
            }
        }
    }

    public String getIssuerOfPatientID() {
        return issuerOfPatientID;
    }

    public void setIssuerOfPatientID(String issuerOfPatientID) {
        this.issuerOfPatientID = issuerOfPatientID;
    }

    public String getIssuerOfAccessionNumber() {
        return this.issuerOfAccessionNumber;
    }

    public void setIssuerOfAccessionNumber(String issuer) {
        this.issuerOfAccessionNumber = issuer;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFileSystemGroupID() {
        return fileSystemGroupID;
    }

    public void setFileSystemGroupID(String fileSystemGroupID) {
        this.fileSystemGroupID = fileSystemGroupID;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setWadoURL(String wadoURL) {
        this.wadoURL = wadoURL;
    }

    public String getWadoURL() {
        return wadoURL;
    }

    public void setAeGroup(String aeGroup) {
        this.aeGroup = aeGroup;
    }

    public String getAeGroup() {
        return aeGroup;
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public boolean isInstalled() {
        return installed;
    }

    public void setInstalled(boolean installed) {
        this.installed = installed;
    }

    @Override
    public String toString() {
        return getProtocol() + "://" + title + '@' + hostName + ':' + port;
    }

    private String getProtocol() {
        if (cipherSuites == null || cipherSuites.length() == 0) {
            return "dicom";
        }
        if ("SSL_RSA_WITH_NULL_SHA".equals(cipherSuites)) {
            return "dicom-tls.nodes";
        }
        if ("SSL_RSA_WITH_3DES_EDE_CBC_SHA".equals(cipherSuites)) {
            return "dicom-tls.3des";
        }
        if ("TLS_RSA_WITH_AES_128_CBC_SHA,SSL_RSA_WITH_3DES_EDE_CBC_SHA"
                .equals(cipherSuites)) {
            return "dicom-tls.aes";
        }
        return "dicom-tls";
    }
}
