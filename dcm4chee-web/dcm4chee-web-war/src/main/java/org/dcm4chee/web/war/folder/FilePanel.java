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

package org.dcm4chee.web.war.folder;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.dcm4chee.web.common.markup.DateTimeLabel;
import org.dcm4chee.web.war.folder.model.FileModel;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision: 15715 $ $Date: 2011-07-26 17:19:49 +0200 (Di, 26 Jul 2011) $
 * @since Jan 22, 2009
 */
public class FilePanel extends Panel {

    private static final long serialVersionUID = 1L;

    public FilePanel(String id, final FileModel fileModel) {
        super(id, new CompoundPropertyModel<Object>(fileModel));
  
        add(new Label("pkInfo", new AbstractReadOnlyModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return getString("pkInfo")+fileModel.getFileObject().getPk();
            }
            
        }){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return fileModel.getFileObject().getPk() != -1;
            }
        });
       
        add(new DateTimeLabel("fileObject.createdTime"));
        add(new Label("fileObject.fileSize"));
        add(new Label("fileObject.transferSyntaxUID"));
        add(new Label("fileObject.md5Sum"));
        add(new DateTimeLabel("fileObject.timeOfLastMD5SumCheck"));
        add(new Label("fileObject.fileStatus"));
        add(new Label("fileObject.filePath"));
        add(new Label("fileObject.fileSystem.directoryPath"));
        add(new Label("absoluteDirectoryPath"));
        add(new Label("fileObject.fileSystem.groupID"));
        add(new Label("fileObject.fileSystem.retrieveAET"));
        add(new Label("fileObject.fileSystem.availability"));
        add(new Label("fileObject.fileSystem.status"));
        add(new Label("fileObject.fileSystem.userInfo"));
    }
}
