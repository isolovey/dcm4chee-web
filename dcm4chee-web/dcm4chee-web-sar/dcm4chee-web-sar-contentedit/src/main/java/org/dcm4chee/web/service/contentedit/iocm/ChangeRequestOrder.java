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
 * TIANI Medgraph AG.
 * Portions created by the Initial Developer are Copyright (C) 2003-2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * Gunter Zeilinger <gunter.zeilinger@tiani.com>
 * Franz Willer <franz.willer@gwi-ag.com>
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
package org.dcm4chee.web.service.contentedit.iocm;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4chee.archive.entity.File;
import org.dcm4chee.archive.entity.Instance;

/**
 * @author franz.willer@gmail.com
 * @version $Revision$ $Date$
 * @since Feb 11, 2010
 */
public class ChangeRequestOrder implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * Map contains changed header attributes. key=original SopIUID
     */
    private Map<String, ChangedInstance> instances = new HashMap<String, ChangedInstance>();
    private Map<String, String> uidMap = new HashMap<String, String>();
    
    private DicomObject rejNote = new BasicDicomObject();

    public Map<String, ChangedInstance> getInstances() {
        return instances;
    }

    public Map<String, String> getUidMap() {
        return uidMap;
    }

    public DicomObject getRejNote() {
        return rejNote;
    }
    
    public ChangeRequestOrder() {
    }
    
    public void addChangedInstance(String studyIUID, String seriesIUID, Instance instance, DicomObject changed) {
        String sopIUID = instance.getSOPInstanceUID();
        if (instances.containsKey(sopIUID))
            return;
        if (changed != null) {
            if (sopIUID.equals(changed.getString(Tag.SOPInstanceUID))) 
                throw new IllegalArgumentException("Changed header must have different SOP Instance UID!");
            uidMap.put(sopIUID, changed.getString(Tag.SOPInstanceUID));
            uidMap.put(seriesIUID, changed.getString(Tag.SeriesInstanceUID));
        }
        instances.put(sopIUID, new ChangedInstance(instance, changed));
    }
    
    public class ChangedInstance implements Serializable {
        private static final long serialVersionUID = 1L;
        private Instance instance;
        private DicomObject changedHeader;

        public ChangedInstance(Instance instance, DicomObject header) {
            this.instance = instance;
            this.setChangedHeader(header);
        }
        
        public Instance getInstance() {
            return instance;
        }
        
        public String getSopIUID() {
            return instance.getSOPInstanceUID();
        }
        public String getSopClassUID() {
            return instance.getSOPClassUID();
        }
        public DicomObject getChangedHeader() {
            return changedHeader;
        }
        public void setChangedHeader(DicomObject changedHeader) {
            this.changedHeader = changedHeader;
        }
        
        public List<File> getFiles() {
            return instance.getFiles();
        }
    }

}


