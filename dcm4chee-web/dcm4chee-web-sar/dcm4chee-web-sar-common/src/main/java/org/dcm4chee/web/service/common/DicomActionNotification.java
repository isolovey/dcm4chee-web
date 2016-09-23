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
package org.dcm4chee.web.service.common;

import javax.management.Notification;
import javax.management.NotificationFilter;

import org.dcm4che2.data.DicomObject;

/**
 * @author franz.willer@gmail.com
 * @version $Revision$ $Date$
 * @since Feb 14, 2010
 */
public class DicomActionNotification extends Notification {
    public static final String NOTIFICATION_TYPE = "DicomActionNotification";

    private static final long serialVersionUID = 0L;

    private String action, level;
 
    public static final NotificationFilter NOTIF_FILTER = new NotificationFilter() {          
        private static final long serialVersionUID = 7625954422409724162L;

        public boolean isNotificationEnabled(Notification notif) {
            return NOTIFICATION_TYPE.equals(notif.getType());
        }
    };
    
    public static final String UPDATE = "UPDATE";
    
    public DicomActionNotification(Object src, DicomObject obj, String action, String level, long sqNr) {
        super(NOTIFICATION_TYPE, src, sqNr);
        setUserData(obj);
        this.action = action;
        this.level = level;
    }

    public String getAction() {
        return action;
    }
    
    public String getLevel() {
        return level;
    }

    public DicomObject getDicomObject() {
        return (DicomObject) super.getUserData();
    }
    
    public String toString() {
        return super.toString()+"[Action:"+action+"][Level:"+level+"] Dicom object:\n"+getUserData();
    }
}
