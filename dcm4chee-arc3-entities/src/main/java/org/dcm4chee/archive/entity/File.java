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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * @author Damien Evans <damien.daddy@gmail.com>
 * @author Justin Falk <jfalkmu@gmail.com>
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision$ $Date$
 * @since Feb 25, 2008
 */
@Entity
@Table(name = "files")
public class File extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 4043304968513421877L;

    @Column(name = "created_time")
    private Date createdTime;

    @Column(name = "filepath")
    private String filePath;

    @Column(name = "file_tsuid")
    private String transferSyntaxUID;

    @Column(name = "file_size")
    private long fileSize;

    @Column(name = "file_md5")
    private String md5Sum;

    @Column(name = "md5_check_time")
    private Date timeOfLastMD5SumCheck;

    @Column(name = "file_status")
    private int fileStatus;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "instance_fk")
    private Instance instance;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "filesystem_fk")    
    private FileSystem fileSystem;

    public Date getCreatedTime() {
        return createdTime;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getTransferSyntaxUID() {
        return transferSyntaxUID;
    }

    public void setTransferSyntaxUID(String transferSyntaxUID) {
        this.transferSyntaxUID = transferSyntaxUID;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getMD5Sum() {
        return md5Sum;
    }

    public void setMD5Sum(String md5Sum) {
        this.md5Sum = md5Sum;
    }

    public Date getTimeOfLastMD5SumCheck() {
        return timeOfLastMD5SumCheck;
    }

    public void setTimeOfLastMD5SumCheck(Date timeOfLastMD5SumCheck) {
        this.timeOfLastMD5SumCheck = timeOfLastMD5SumCheck;
    }

    public int getFileStatus() {
        return fileStatus;
    }

    public void setFileStatus(int fileStatus) {
        this.fileStatus = fileStatus;
    }

    public Instance getInstance() {
        return instance;
    }

    public void setInstance(Instance instance) {
        this.instance = instance;
    }

    public FileSystem getFileSystem() {
        return fileSystem;
    }

    public void setFileSystem(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    @Override
    public String toString() {
        return "File[pk=" + pk
                + ", path=" + filePath
                + ", tsuid=" + transferSyntaxUID
                + ", size=" + fileSize
                + ", status=" + fileStatus
                + "]";
    }

    public void onPrePersist() {
        createdTime = new Date();
    }
}
