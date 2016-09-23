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

package org.dcm4chee.usr.ui.usermanagement.role.group;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxFallbackButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.resources.CompressedResourceReference;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.dcm4chee.usr.dao.UserAccess;
import org.dcm4chee.usr.model.Group;
import org.dcm4chee.usr.ui.usermanagement.markup.ColorPicker;
import org.dcm4chee.usr.ui.validator.GroupValidator;
import org.dcm4chee.usr.util.JNDIUtils;
import org.dcm4chee.web.common.base.BaseWicketApplication;
import org.dcm4chee.web.common.base.BaseWicketPage;
import org.dcm4chee.web.common.markup.BaseForm;
import org.dcm4chee.web.common.secure.SecureSessionCheckPage;
import org.dcm4chee.web.common.util.Auditlog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision$ $Date$
 * @since 21.07.2010
 */
public class CreateOrEditGroupPage extends SecureSessionCheckPage {
    
    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(CreateOrEditGroupPage.class);

    protected ModalWindow window;
   
    public CreateOrEditGroupPage(final ModalWindow window, ListModel<Group> allGroupnames, Group group) {
        super();
        this.window = window;
        add(new CreateOrEditGroupForm("add-group-form", allGroupnames, group));
        
        add(new WebMarkupContainer("create-group-title").setVisible(group == null));
        add(new WebMarkupContainer("edit-group-title").setVisible(group != null));
    }

    private final class CreateOrEditGroupForm extends BaseForm {
        
        private static final long serialVersionUID = 1L;

        private Model<String> groupname = new Model<String>();
        private Model<String> description= new Model<String>();
        private Model<String> colorPickerModel = new Model<String>();
        
        private TextField<String> groupnameTextField= new TextField<String>("grouplist.add-group-form.groupname.input", groupname);
        private TextField<String> descriptionTextField= new TextField<String>("grouplist.add-group-form.description.input", description);
        
        public CreateOrEditGroupForm(String id, final ListModel<Group> allGroupnames, final Group group) {
            super(id);

            ((BaseWicketApplication) getApplication()).getInitParameter("UserAccessServiceName");
            
            add(groupnameTextField
                    .setRequired(true)
                    .add(new GroupValidator(allGroupnames, (group == null ? null : group.getGroupname())))
                    .setEnabled(group == null || (!group.getGroupname().equals("Web") && !group.getGroupname().equals("Dicom")))
            );
            add(descriptionTextField);
            final ColorPicker colorPicker;
            add(colorPicker = new ColorPicker("color-picker", colorPickerModel));

            if (group != null) {
                groupnameTextField.setModelObject(group.getGroupname());
                descriptionTextField.setModelObject(group.getDescription());
                colorPicker.setColorValue(group.getColor());
            }
            
            add(new AjaxFallbackButton("add-group-submit", CreateOrEditGroupForm.this) {
                
                private static final long serialVersionUID = 1L;

                @Override
                protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                    try {
                        UserAccess userAccess = (UserAccess) JNDIUtils.lookup(UserAccess.JNDI_NAME);
                        if (group == null) {
                            Group newGroup = new Group(groupname.getObject());
                            newGroup.setDescription(description.getObject());
                            newGroup.setColor(colorPicker.getColorValue());
                            userAccess.addGroup(newGroup);
                            Auditlog.logSoftwareConfiguration(true, "Role Group "+newGroup+" created.");
                        } else {
                            StringBuilder sb = new StringBuilder("Role Group ").append(groupname).append(" updated.");
                            boolean changed = Auditlog.addChange(sb, false, "rolename", group.getGroupname(), groupname.getObject());
                            Auditlog.addChange(sb, changed, "description", group.getDescription(), description.getObject());
                            Auditlog.addChange(sb, changed, "color", group.getColor(), colorPicker.getColorValue());
                            group.setGroupname(groupname.getObject());
                            group.setDescription(description.getObject());
                            group.setColor(colorPicker.getColorValue());
                            userAccess.updateGroup(group);
                            Auditlog.logSoftwareConfiguration(true, "Role Group "+groupname+" updated.");
                        }
                        allGroupnames.setObject(userAccess.getAllGroups());
                        window.close(target);
                    } catch (final Exception e) {
                        log.error(this.getClass().toString() + ": " + "onSubmit: " + e.getMessage());
                        log.debug("Exception: ", e);
                    }
                }
                
                @Override
                protected void onError(AjaxRequestTarget target, Form<?> form) {
                    target.addComponent(form);
                }
            });
        }
    };
}
