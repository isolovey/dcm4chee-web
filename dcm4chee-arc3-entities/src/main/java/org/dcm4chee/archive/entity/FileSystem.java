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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.dcm4chee.archive.common.Availability;
import org.dcm4chee.archive.common.FileSystemStatus;

/**
 * @author Damien Evans <damien.daddy@gmail.com>
 * @author Justin Falk <jfalkmu@gmail.com>
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision$ $Date$
 * @since Feb 25, 2008
 */
@Entity
@Table(name = "filesystem")
public class FileSystem extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 8949412622901631772L;

    @Column(name = "dirpath", unique=true, nullable = false)
    private String directoryPath;

    @Column(name = "fs_group_id", nullable = false)
    private String groupID;

    @Column(name = "retrieve_aet", nullable = false)
    private String retrieveAET;

    @Column(name = "availability", nullable = false)
    private Availability availability;

    @Column(name = "fs_status", nullable = false)
    private FileSystemStatus status;

    @Column(name = "user_info")
    private String userInfo;

    @OneToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "next_fk")
    private FileSystem nextFileSystem;

    public String getDirectoryPath() {
        return directoryPath;
    }

    public void setDirectoryPath(String directoryPath) {
        this.directoryPath = directoryPath;
    }

    public String getGroupID() {
        return groupID;
    }

    public void setGroupID(String groupID) {
        this.groupID = groupID;
    }

    public String getRetrieveAET() {
        return retrieveAET;
    }

    public void setRetrieveAET(String retrieveAET) {
        this.retrieveAET = retrieveAET;
    }

    public Availability getAvailability() {
        return availability;
    }

    public void setAvailability(Availability availability) {
        this.availability = availability;
    }

    public FileSystemStatus getStatus() {
        return status;
    }

    public void setStatus(FileSystemStatus status) {
        this.status = status;
    }

    public String getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(String userInfo) {
        this.userInfo = userInfo;
    }

    public FileSystem getNextFileSystem() {
        return nextFileSystem;
    }

    public void setNextFileSystem(FileSystem nextFileSystem) {
        this.nextFileSystem = nextFileSystem;
    }

    @Override
    public String toString() {
        return "FileSystem[pk=" + pk
                + ", dir=" + directoryPath
                + ", avail=" + availability
                + ", status=" + status
                + ", aet=" + retrieveAET
                + "]";
    }

}
