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
 * Java(TM), available at http://sourceforge.net/projects/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * Agfa HealthCare.
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

package org.dcm4chee.dashboard.model;

import java.io.Serializable;
import java.util.Calendar;

/**
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision$ $Date$
 * @since 25.11.2009
 */
public class ReportModel implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private String uuid;
    private String title;
    private String dataSource;
    private String statement;
    private Integer diagram;
    private boolean table;
    private String groupUuid;
    private Calendar created = Calendar.getInstance();
    
    public ReportModel() {
    }

    public ReportModel(String uuid, String title, String dataSource, String statement, Integer diagram, boolean table, String groupUuid, Long created) {
        this.uuid = uuid;
        this.title = title;
        this.dataSource = dataSource;
        this.statement = statement;
        this.diagram = diagram;
        this.table = table;
        this.groupUuid = groupUuid;
        if (created != null) this.created.setTimeInMillis(created);
    }
    
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setStatement(String statement) {
        this.statement = statement;
    }
    
    public String getStatement() {
        return statement;
    }

    public void setDiagram(Integer diagram) {
        this.diagram = diagram;
    }
    
    public Integer getDiagram() {
        return diagram;
    }
    
    public void setTable(boolean table) {
        this.table = table;
    }
    
    public boolean getTable() {
        return table;
    }

    public void setGroupUuid(String groupUuid) {
        this.groupUuid = groupUuid;
    }

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setCreated(Long created) {
        if (created != null) this.created.setTimeInMillis(created);
    }

    public Long getCreated() {
        return this.created != null ? created.getTimeInMillis() : null;
    }
}
