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
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.dcm4chee.archive.common.PublishedStudyStatus;

/**
 * @author Damien Evans <damien.daddy@gmail.com>
 * @author Justin Falk <jfalkmu@gmail.com>
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @author Franz Willer <franz.willer@gmail.com>
 * @version $Revision$ $Date$
 * @since Mar 3, 2008
 */
@Entity
@Table(name = "published_study")
@NamedQuery(name="PublishedStudy.findByStudyPkAndStatus",
        query="select object(s) from PublishedStudy s where s.study.pk = :studyPk and s.status = :status")
public class PublishedStudy extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 4084609433998181615L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_fk")
    private Study study;
    
    @Column(name = "created_time")
    private Date createdTime;

    @Column(name = "updated_time")
    private Date updatedTime;

    @Column(name = "doc_uid")
    private String documentUID;

    @Column(name = "docentry_uid")
    private String documentEntryUID;
    
    @Column(name = "repository_uid")
    private String repositoryUID;

    @Column(name = "status", nullable = false)
    private PublishedStudyStatus status;

    public PublishedStudy() {
    }
    
    public PublishedStudy(Study study, String documentUID, String documentEntryUID, String repositoryUID, PublishedStudyStatus status) {
        this.study = study;
        this.documentUID = documentUID;
        this.documentEntryUID = documentEntryUID;
        this.repositoryUID = repositoryUID;
        this.status = status;
    }

    public void onPrePersist() {
        createdTime = new Date();
    }

    public void onPreUpdate() {
        updatedTime = new Date();
    }

    public Study getStudy() {
        return study;
    }

    public void setStudy(Study study) {
        this.study = study;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public Date getUpdatedTime() {
        return updatedTime;
    }

    public String getDocumentUID() {
        return documentUID;
    }

    public void setDocumentUID(String documentUID) {
        this.documentUID = documentUID;
    }

    public String getDocumentEntryUID() {
        return documentEntryUID;
    }

    public void setDocumentEntryUID(String documentEntryUID) {
        this.documentEntryUID = documentEntryUID;
    }

    public String getRepositoryUID() {
        return repositoryUID;
    }

    public void setRepositoryUID(String repositoryUID) {
        this.repositoryUID = repositoryUID;
    }

    public PublishedStudyStatus getStatus() {
        return status;
    }

    public void setStatus(PublishedStudyStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "PublishedStudy[pk=" + pk
                + ", study=" + study.toString()
                + " as " + getDocumentUID()
                + "(DocumentEntry.UID:" + getDocumentEntryUID()
                + "), status=" + getStatus()
                +"]";
    }
}
