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
 * Agfa-Gevaert AG.
 * Portions created by the Initial Developer are Copyright (C) 2008
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

package org.dcm4chee.web.dao.util;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.DicomObject.Visitor;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.VR;
import org.dcm4chee.archive.entity.Instance;
import org.dcm4chee.archive.entity.MPPS;
import org.dcm4chee.web.dao.vo.EntityTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Franz Willer <fwiller@gmail.com>
 * @version $Revision$ $Date$
 * @since Aug 25, 2013
 */

public class IOCMUtil {

    private static String[] contributingEquipment = new String[3];

    private static Logger log = LoggerFactory.getLogger(IOCMUtil.class);
            
    public static String[] getContributingEquipment() {
        return contributingEquipment;
    }

    public static void setContributingEquipment(String[] equipment) {
        contributingEquipment = equipment;
    }
    
    public static DicomObject changeUID(EntityTree tree, DicomObject attrs, int tag) {
        String uid = attrs.getString(tag);
        attrs.putString(tag, VR.UI, tree.getChangedUID(uid));
        return attrs;
    }

    public static DicomObject addReplacementAttrs(DicomObject obj) {
        if (obj != null) {
            DicomObject item = new BasicDicomObject();
            item.putString(Tag.InstitutionName, VR.LO, contributingEquipment[0]);
            item.putString(Tag.StationName, VR.SH, contributingEquipment[1]);
            item.putString(Tag.Manufacturer, VR.LO, contributingEquipment[2]);
            item.putDate(Tag.ContributionDateTime, VR.DT, new Date());
            DicomElement sq = obj.get(Tag.ContributingEquipmentSequence);
            if (sq == null)
                sq = obj.putSequence(Tag.ContributingEquipmentSequence);
            sq.addDicomObject(item);
        }
        return obj;
    }

    public static void updateUIDrefs(EntityTree tree, EntityManager em) {
        Map<String, String> uidMap = tree.getOld2NewUIDMap();
        if (uidMap.isEmpty()) {
           log.debug("UID mapping empty! nothing to do");
           return;
        }
        for (Instance i : tree.getAllInstances()) {
            DicomObject attrs = i.getAttributes(false);
            if (updateUIDs(attrs, uidMap)) {
                i.setAttributes(attrs);
                em.merge(i);
            }
        }
        
    }
    public static void updateUIDrefs(List<MPPS> mppsList, EntityTree tree, EntityManager em) {
        Map<String, String> uidMap = tree.getOld2NewUIDMap();
        if (uidMap.isEmpty()) {
           log.debug("UID mapping empty! nothing to do");
           return;
        }
        for(MPPS mpps : mppsList) {
            DicomObject attrs = mpps.getAttributes();
            if (updateUIDs(attrs, uidMap)) {
                mpps.setAttributes(attrs);
                em.merge(mpps);
            }
        }
    }

    public static boolean updateUIDs(DicomObject attrs, final Map<String, String> uidMap) {
        return updateUIDsInSequence(attrs, uidMap);
    }

    private static boolean updateUIDsInSequence(DicomObject attrs, final Map<String, String> uidMap) {
        final boolean[] changed = new boolean[]{false};
        attrs.accept(new Visitor() {
            public boolean visit(DicomElement attr) {
                if (attr.vr() == VR.SQ) {
                    for (int i = 0, len = attr.countItems(); i < len ; i++) {
                        changed[0] |= updateUIDsInSequence(attr.getDicomObject(i), uidMap);
                    }
                }
                return true;
            }
        });
        if (!changed[0]) {
            changed[0] |= updateUID(attrs, Tag.StudyInstanceUID, uidMap);
            changed[0] |= updateUID(attrs, Tag.SeriesInstanceUID, uidMap);
        }
        changed[0] |= updateUID(attrs, Tag.SOPInstanceUID, uidMap);
        changed[0] |= updateUID(attrs, Tag.ReferencedSOPInstanceUID, uidMap);
        changed[0] |= updateUID(attrs, Tag.UID, uidMap);
        return changed[0];
    }
    
    public static boolean updateUID(DicomObject attrs, int tag, Map<String, String> uidMap) {
        String uid = uidMap.get(attrs.getString(tag));
        if (uid != null) {
            attrs.putString(tag, VR.UI, uid);
            return true;
        } else {
            return false;
        }
    }
}
