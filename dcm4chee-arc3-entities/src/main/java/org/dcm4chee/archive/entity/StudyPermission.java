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
import javax.persistence.Table;

/**
 * @author Damien Evans <damien.daddy@gmail.com>
 * @author Justin Falk <jfalkmu@gmail.com>
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision$ $Date$
 * @since Mar 3, 2008
 */
@Entity
@Table(name = "study_permission")
public class StudyPermission extends BaseEntity implements Serializable {

    private static final long serialVersionUID = -4389532918446366208L;

    /**
     * Action value for permission to query attributes of the Study and included
     * Series and Instance entities.
     */
    public static final String QUERY_ACTION = "Q";

    /**
     * Action value for permission to active retrieve or passive receive
     * Instances of the Study.
     */
    public static final String READ_ACTION = "R";

    /**
     * Action value for permission to retrieve Instances of the Study to another
     * application entity.
     */
    public static final String EXPORT_ACTION = "E";

    /**
     * Action value for permission to store Instances to an already existing
     * Study.
     */
    public static final String APPEND_ACTION = "A";

    /**
     * Action value to modify attributes of the Study and included Series.
     */
    public static final String UPDATE_ACTION = "U";

    /**
     * Action value to delete the whole Study or individual Series or Instances.
     */
    public static final String DELETE_ACTION = "D";

    @Column(name = "study_iuid", nullable = false)
    private String studyInstanceUID;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "roles", nullable = false)
    private String role;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStudyInstanceUID() {
        return studyInstanceUID;
    }

    public void setStudyInstanceUID(String studyInstanceUID) {
        this.studyInstanceUID = studyInstanceUID;
    }

    @Override
    public String toString() {
        return "StudyPermission[pk=" + pk
                + ", suid=" + studyInstanceUID
                + ", action=" + action + 
                ", role=" + role + "]";
    }
}
