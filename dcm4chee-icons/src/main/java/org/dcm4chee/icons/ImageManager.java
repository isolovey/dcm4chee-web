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
 * Portions created by the Initial Developer are Copyright (C) 2002-2005
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

package org.dcm4chee.icons;

import org.apache.wicket.ResourceReference;

/**
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision$ $Date$
 * @since 13.04.2010
 */
public class ImageManager {

    private static final String schemeDependentPrefix = "GREEN_";
    
    public static int defaultWidth = 16;
    public static int defaultHeight = 16;
   
    public static final ResourceReference IMAGE_COMMON_ADD = 
        new ResourceReference(ImageManager.class, "common/add.png");
    public static final ResourceReference IMAGE_COMMON_BACK = 
        new ResourceReference(ImageManager.class, "common/" + schemeDependentPrefix + "back.png");
    public static final ResourceReference IMAGE_COMMON_COLLAPSE = 
        new ResourceReference(ImageManager.class, "common/" + schemeDependentPrefix + "collapse.png");
    public static final ResourceReference IMAGE_COMMON_EXPAND = 
        new ResourceReference(ImageManager.class, "common/" + schemeDependentPrefix + "expand.png");
    public static final ResourceReference IMAGE_COMMON_FORWARD = 
        new ResourceReference(ImageManager.class, "common/" + schemeDependentPrefix + "forward.png");
    public static final ResourceReference IMAGE_COMMON_LOGOUT = 
        new ResourceReference(ImageManager.class, "common/logout.png");
    public static final ResourceReference IMAGE_COMMON_REMOVE = 
        new ResourceReference(ImageManager.class, "common/remove.png");
    public static final ResourceReference IMAGE_COMMON_TRASH = 
        new ResourceReference(ImageManager.class, "common/trash.png");
    public static final ResourceReference IMAGE_COMMON_DICOM_DETAILS = 
        new ResourceReference(ImageManager.class, "common/dicom_details.png");
    public static final ResourceReference IMAGE_COMMON_DICOM_EDIT = 
        new ResourceReference(ImageManager.class, "common/dicom_edit.png");
    public static final ResourceReference IMAGE_COMMON_LINK = 
        new ResourceReference(ImageManager.class, "common/link.png");
    public static final ResourceReference IMAGE_COMMON_SEARCH = 
        new ResourceReference(ImageManager.class, "common/search.png");
    public static final ResourceReference IMAGE_COMMON_RESET = 
        new ResourceReference(ImageManager.class, "common/reset.png");
    public static final ResourceReference IMAGE_COMMON_CANCEL = 
        new ResourceReference(ImageManager.class, "common/cancel.png");
    public static final ResourceReference IMAGE_COMMON_ARR =
            new ResourceReference(ImageManager.class, "common/arr.png");
    public static final ResourceReference IMAGE_COMMON_WARN =
            new ResourceReference(ImageManager.class, "common/warn.png");

    public static final ResourceReference IMAGE_FOLDER_DELETE = 
        new ResourceReference(ImageManager.class, "folder/delete.png");
    public static final ResourceReference IMAGE_FOLDER_EXPORT = 
        new ResourceReference(ImageManager.class, "folder/export.png");
    public static final ResourceReference IMAGE_FOLDER_DICOM_FILE = 
        new ResourceReference(ImageManager.class, "folder/dicom_file.png");
    public static final ResourceReference IMAGE_FOLDER_MOVE = 
        new ResourceReference(ImageManager.class, "folder/move.png");
    public static final ResourceReference IMAGE_FOLDER_UNLINK = 
        new ResourceReference(ImageManager.class, "folder/unlink.png");
    public static final ResourceReference IMAGE_FOLDER_VIEWER = 
        new ResourceReference(ImageManager.class, "folder/viewer.png");
    public static final ResourceReference IMAGE_FOLDER_WADO = 
        new ResourceReference(ImageManager.class, "folder/wado.png");
    public static ResourceReference IMAGE_FOLDER_STUDY_PERMISSIONS = 
        new ResourceReference(ImageManager.class, "folder/study_permissions.png");
    public static ResourceReference IMAGE_FOLDER_MPPS = 
        new ResourceReference(ImageManager.class, "folder/mpps.png");
    public static ResourceReference IMAGE_FOLDER_SELECT_ALL= 
        new ResourceReference(ImageManager.class, "folder/select_all.png");
    public static ResourceReference IMAGE_FOLDER_DESELECT_ALL= 
        new ResourceReference(ImageManager.class, "folder/deselect_all.png");
    public static ResourceReference IMAGE_FOLDER_NO_IMAGE = 
        new ResourceReference(ImageManager.class, "folder/no_image.png");
    public static final ResourceReference IMAGE_FOLDER_TIMELIMIT_EDIT = 
        new ResourceReference(ImageManager.class, "folder/time_limit_edit.png");
    public static final ResourceReference IMAGE_FOLDER_TIMELIMIT_LINK = 
        new ResourceReference(ImageManager.class, "folder/time_limit_link.png");
    public static final ResourceReference IMAGE_FOLDER_TIMELIMIT_UNLINK = 
        new ResourceReference(ImageManager.class, "folder/time_limit_unlink.png");
    public static final ResourceReference IMAGE_FOLDER_MPPS_FORWARD = 
            new ResourceReference(ImageManager.class, "folder/mpps_forward.png");
    
    public static final ResourceReference IMAGE_AE_ECHO = 
        new ResourceReference(ImageManager.class, "ae/echo.png");
    public static final ResourceReference IMAGE_AE_EDIT = 
        new ResourceReference(ImageManager.class, "ae/edit.png");
    
    public static final ResourceReference IMAGE_USER_CHANGE_PASSWORD = 
        new ResourceReference(ImageManager.class, "user/change_password.png");
    public static final ResourceReference IMAGE_USER_ROLE_ADD = 
        new ResourceReference(ImageManager.class, "user/role_add.png");
    public static final ResourceReference IMAGE_USER_ROLE_GROUP_ADD = 
        new ResourceReference(ImageManager.class, "user/group_add.png");
    public static final ResourceReference IMAGE_USER_ADD = 
        new ResourceReference(ImageManager.class, "user/user_add.png");
    public static ResourceReference IMAGE_USER_WEB_PERMISSIONS = 
        new ResourceReference(ImageManager.class, "user/web_permissions.png");
    public static final ResourceReference IMAGE_USER_AET_PERMISSIONS = 
        new ResourceReference(ImageManager.class, "user/aet_group_assignment.png");
    public static final ResourceReference IMAGE_USER_AET_GROUP_ADD = 
        new ResourceReference(ImageManager.class, "user/aet_group_add.png");
    public static final ResourceReference IMAGE_USER_AET_GROUP_AETS = 
        new ResourceReference(ImageManager.class, "user/aet_group_aets.png");
    
    public static final ResourceReference IMAGE_DASHBOARD_FILESYSTEM_GROUP = 
        new ResourceReference(ImageManager.class, "dashboard/filesystem/filesystem_group.png");
    public static final ResourceReference IMAGE_DASHBOARD_FILESYSTEM = 
        new ResourceReference(ImageManager.class, "dashboard/filesystem/filesystem.png");
    public static final ResourceReference IMAGE_DASHBOARD_FILESYSTEM_TAR = 
        new ResourceReference(ImageManager.class, "dashboard/filesystem/tar.png");

    public static final ResourceReference IMAGE_DASHBOARD_PROPERTY = 
        new ResourceReference(ImageManager.class, "dashboard/property/" + schemeDependentPrefix + "property.png");
    public static final ResourceReference IMAGE_DASHBOARD_PROPERTY_FOLDER = 
        new ResourceReference(ImageManager.class, "dashboard/property/folder_property.png");

    public static final ResourceReference IMAGE_DASHBOARD_QUEUE_MESSAGE = 
        new ResourceReference(ImageManager.class, "dashboard/queue/message.png");
    public static final ResourceReference IMAGE_DASHBOARD_QUEUE = 
        new ResourceReference(ImageManager.class, "dashboard/queue/server_queue.png");
    
    public static final ResourceReference IMAGE_DASHBOARD_REPORT_CHART_AND_TABLE = 
        new ResourceReference(ImageManager.class, "dashboard/report/both_chart_table.png");
    public static final ResourceReference IMAGE_DASHBOARD_REPORT_CHART = 
        new ResourceReference(ImageManager.class, "dashboard/report/chart_bar.png");
    public static final ResourceReference IMAGE_DASHBOARD_REPORT_FOLDER_ADD = 
        new ResourceReference(ImageManager.class, "dashboard/report/folder_add.png");
    public static final ResourceReference IMAGE_DASHBOARD_REPORT_FOLDER_DELETE = 
        new ResourceReference(ImageManager.class, "dashboard/report/folder_delete.png");
    public static final ResourceReference IMAGE_DASHBOARD_REPORT_FOLDER = 
        new ResourceReference(ImageManager.class, "dashboard/report/folder_report.png");
    public static final ResourceReference IMAGE_DASHBOARD_REPORT_ADD = 
        new ResourceReference(ImageManager.class, "dashboard/report/report_add.png");
    public static final ResourceReference IMAGE_DASHBOARD_REPORT_DELETE = 
        new ResourceReference(ImageManager.class, "dashboard/report/report_delete.png");
    public static final ResourceReference IMAGE_DASHBOARD_REPORT_EDIT = 
        new ResourceReference(ImageManager.class, "dashboard/report/report_edit.png");
    public static final ResourceReference IMAGE_DASHBOARD_REPORT = 
        new ResourceReference(ImageManager.class, "dashboard/report/report.png");
    public static final ResourceReference IMAGE_DASHBOARD_REPORT_TABLE = 
        new ResourceReference(ImageManager.class, "dashboard/report/table.png");
    public static final ResourceReference IMAGE_DASHBOARD_REPORT_DOWNLOAD = 
        new ResourceReference(ImageManager.class, "dashboard/report/report_download.png");
    public static final ResourceReference IMAGE_DASHBOARD_REPORT_PRINT = 
        new ResourceReference(ImageManager.class, "dashboard/report/print.png");

    public static final ResourceReference IMAGE_TRASH_DELETE_SELECTED = 
        new ResourceReference(ImageManager.class, "trash/delete_permanent.png");
    public static final ResourceReference IMAGE_TRASH_EMPTY = 
        new ResourceReference(ImageManager.class, "trash/empty_trash.png");
    public static final ResourceReference IMAGE_TRASH_RESTORE = 
        new ResourceReference(ImageManager.class, "trash/restore.png");
    
    public static final ResourceReference IMAGE_TC_IMAGE_PLACEHOLDER =
        new ResourceReference(ImageManager.class, "tc/empty-image.png");
    public static final ResourceReference IMAGE_TC_PRINT =
        new ResourceReference(ImageManager.class, "tc/print.png");
    public static final ResourceReference IMAGE_TC_ACKNOWLEDGE =
        new ResourceReference(ImageManager.class, "tc/acknowledge.png");
    public static final ResourceReference IMAGE_TC_CANCEL =
        new ResourceReference(ImageManager.class, "tc/cancel.png");
    public static final ResourceReference IMAGE_TC_CANCEL_MONO =
        new ResourceReference(ImageManager.class, "tc/cancel-mono.png");
    public static final ResourceReference IMAGE_TC_EYE =
            new ResourceReference(ImageManager.class, "tc/eye.png");
    public static final ResourceReference IMAGE_TC_EYE_MONO =
            new ResourceReference(ImageManager.class, "tc/eye-mono.png");
    public static final ResourceReference IMAGE_TC_ARROW_DOWN =
        new ResourceReference(ImageManager.class, "tc/arrow-down.png");
    public static final ResourceReference IMAGE_TC_LIGHT_BULB =
    	new ResourceReference(ImageManager.class, "tc/light-bulb.png");
    public static final ResourceReference IMAGE_TC_BUTTON_ON =
        	new ResourceReference(ImageManager.class, "tc/button-on.png");
    public static final ResourceReference IMAGE_TC_BUTTON_OFF =
        	new ResourceReference(ImageManager.class, "tc/button-off.png");
    public static final ResourceReference IMAGE_TC_ARROW_PREV =
        	new ResourceReference(ImageManager.class, "tc/arrow-prev.png");
    public static final ResourceReference IMAGE_TC_ARROW_NEXT =
        	new ResourceReference(ImageManager.class, "tc/arrow-next.png");
    public static final ResourceReference IMAGE_TC_ARROW_NEXT_RANDOM =
        	new ResourceReference(ImageManager.class, "tc/arrow-next-random.png");
    public static final ResourceReference IMAGE_TC_EXCEL =
        	new ResourceReference(ImageManager.class, "tc/excel.png");
    public static final ResourceReference IMAGE_TC_IMAGE =
    		new ResourceReference(ImageManager.class, "tc/image.png");
    public static final ResourceReference IMAGE_TC_PDF =
    		new ResourceReference(ImageManager.class, "tc/pdf.png");
    public static final ResourceReference IMAGE_TC_ZIP =
    		new ResourceReference(ImageManager.class, "tc/zip.png");
    public static final ResourceReference IMAGE_TC_POWERPOINT =
    		new ResourceReference(ImageManager.class, "tc/powerpoint.png");
    public static final ResourceReference IMAGE_TC_TEXT =
    		new ResourceReference(ImageManager.class, "tc/text.png");
    public static final ResourceReference IMAGE_TC_WORD =
    		new ResourceReference(ImageManager.class, "tc/word.png");
    public static final ResourceReference IMAGE_TC_DISK =
            new ResourceReference(ImageManager.class, "tc/disk.png");
    public static final ResourceReference IMAGE_TC_DISK_MONO =
            new ResourceReference(ImageManager.class, "tc/disk-mono.png");
    public static final ResourceReference IMAGE_TC_INFO =
            new ResourceReference(ImageManager.class, "tc/info.png");
    public static final ResourceReference IMAGE_TC_IMAGE_SQUARE =
            new ResourceReference(ImageManager.class, "tc/image-square.png");
    public static final ResourceReference IMAGE_TC_FORUM =
            new ResourceReference(ImageManager.class, "tc/forum.png");
    public static final ResourceReference IMAGE_TC_EXTERNAL =
    		new ResourceReference(ImageManager.class, "tc/external.png");
}
