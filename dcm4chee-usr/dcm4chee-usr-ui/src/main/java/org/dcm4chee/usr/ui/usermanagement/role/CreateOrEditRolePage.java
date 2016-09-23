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

package org.dcm4chee.usr.ui.usermanagement.role;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxFallbackButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.resources.CompressedResourceReference;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.dcm4chee.usr.dao.UserAccess;
import org.dcm4chee.usr.model.Group;
import org.dcm4chee.usr.model.Role;
import org.dcm4chee.usr.ui.validator.RoleValidator;
import org.dcm4chee.usr.util.JNDIUtils;
import org.dcm4chee.web.common.base.BaseWicketApplication;
import org.dcm4chee.web.common.base.BaseWicketPage;
import org.dcm4chee.web.common.markup.BaseForm;
import org.dcm4chee.web.common.secure.SecureSessionCheckPage;
import org.dcm4chee.web.common.secure.SecurityBehavior;
import org.dcm4chee.web.common.util.Auditlog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision$ $Date$
 * @since Jul. 21, 2010
 */
public class CreateOrEditRolePage extends SecureSessionCheckPage {
    
    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(CreateOrEditRolePage.class);

    protected ModalWindow window;

    public CreateOrEditRolePage(final ModalWindow window, ListModel<Role> allRolenames, Role role, Map<String,Group> types) {
        super();
        this.window = window;
        add(new CreateOrEditRoleForm("add-role-form", allRolenames, role, types));
        
        add(new WebMarkupContainer("create-role-title").setVisible(role == null));
        add(new WebMarkupContainer("edit-role-title").setVisible(role != null));
    }

    private final class CreateOrEditRoleForm extends BaseForm {
        
        private static final long serialVersionUID = 1L;

        private Model<String> rolename = new Model<String>();
        private Model<String> description = new Model<String>();
        private Model<Boolean> hiddenRole = new Model<Boolean>();
        
        private TextField<String> rolenameTextField= new TextField<String>("rolelist.add-role-form.rolename.input", rolename);
        private TextField<String> descriptionTextField= new TextField<String>("rolelist.add-role-form.description.input", description);

        private List<CheckBox> groupCheckboxList;

        public CreateOrEditRoleForm(String id, final ListModel<Role> allRolenames, final Role role, final Map<String,Group> types) {
            super(id);

            setOutputMarkupId(true);
            
            ((BaseWicketApplication) getApplication()).getInitParameter("UserAccessServiceName");
            
            final UserAccess userAccess = ((UserAccess) JNDIUtils.lookup(UserAccess.JNDI_NAME));
            final List<Group> groups = userAccess.getAllGroups();
            
            add(rolenameTextField
                    .setRequired(true)
                    .add(new RoleValidator(allRolenames, (role == null ? null : role.getRolename())))
            );
            add(descriptionTextField);

            WebMarkupContainer hiddenRoleContainer = new WebMarkupContainer("hiddenRoleContainer");
            add(hiddenRoleContainer);
            final CheckBox hiddenRoleCheckbox = new CheckBox("hiddenRole-checkbox", hiddenRole);
            hiddenRole.setObject(role != null && role.isSuperuser());
            if (role != null) {
                rolenameTextField.setModelObject(role.getRolename());
                descriptionTextField.setModelObject(role.getDescription());
                hiddenRoleCheckbox.setModelObject(role.isSuperuser());
            }
            hiddenRoleContainer
            	.add(hiddenRoleCheckbox)
            	.add(new SecurityBehavior(getModuleName() + ":superuserCheckbox"));
            
            final StringBuffer webRoleUuid = new StringBuffer();
            final StringBuffer dicomRoleUuid = new StringBuffer();
            final StringBuffer aetRoleUuid = new StringBuffer();

            groupCheckboxList = new ArrayList<CheckBox>(groups.size());
            RepeatingView groupRows = new RepeatingView("group-rows");
            add(groupRows);           
            for (final Group group : groups) {
                WebMarkupContainer rowParent;            
                groupRows.add((rowParent = new WebMarkupContainer(groupRows.newChildId())));
                CheckBox groupCheckbox;
                rowParent.add((groupCheckbox = new CheckBox("group-checkbox", 
                        new Model<Boolean>(role != null ? 
                                group.getGroupname().equals("Web") ? role.isWebRole() : 
                                    group.getGroupname().equals("Dicom") ? role.isDicomRole() : 
                                        group.getGroupname().equals("AET") ? role.isAETRole() :
                                            role.getRoleGroups().contains(group.getUuid()) : false)))                          
                        .setLabel(new Model<String>(group.getUuid()))
                        .add(new SecurityBehavior(getModuleName() + ":changeGroupAssignmentCheckbox")));
                rowParent.add(new Label("groupname", new Model<String>(group.getGroupname())));
                groupCheckboxList.add(groupCheckbox);
                groupRows.add(rowParent);

                if (group.getGroupname().equals("Web"))
                    webRoleUuid.append(group.getUuid());
                else if (group.getGroupname().equals("Dicom"))
                    dicomRoleUuid.append(group.getUuid());
                else if (group.getGroupname().equals("AET"))
                    aetRoleUuid.append(group.getUuid());
            }

            add(new AjaxFallbackButton("add-role-submit", CreateOrEditRoleForm.this) {
                
                private static final long serialVersionUID = 1L;

                @Override
                protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                    try {
                        List<String> groupList = new ArrayList<String>();
                        for (CheckBox checkbox : groupCheckboxList)
                            if (checkbox.getModelObject())
                                groupList.add(checkbox.getLabel().getObject());
                        
                        Set<String> aetGroups = new HashSet<String>();
                        aetGroups.add("*");
                        if (role == null) {
                            Role newRole = new Role(rolename.getObject());
                            newRole.setDescription(description.getObject());
                            newRole.setSuperuser(hiddenRoleCheckbox.getModelObject());
                            newRole.setRoleGroups(groupList);
                            newRole.setWebRole(groupList.contains(webRoleUuid.toString()));
                            newRole.setDicomRole(groupList.contains(dicomRoleUuid.toString()));
                            newRole.setAETRole(groupList.contains(aetRoleUuid.toString()));
                            if (newRole.isAETRole())
                                newRole.setAETGroups(aetGroups);
                            userAccess.addRole(newRole);
                            Auditlog.logSoftwareConfiguration(true, "Role "+newRole+" created.");
                        } else {
                            getUpdateInfo(role, groupList, aetGroups);
                            role.setRolename(rolename.getObject());
                            role.setDescription(description.getObject());
                            role.setSuperuser(hiddenRoleCheckbox.getModelObject());
                            role.setRoleGroups(groupList);
                            role.setWebRole(groupList.contains(webRoleUuid.toString()));
                            role.setDicomRole(groupList.contains(dicomRoleUuid.toString()));
                            role.setAETRole(groupList.contains(aetRoleUuid.toString()));
                            if (role.isAETRole() && (role.getAETGroups() == null || role.getAETGroups().size() == 0))
                                role.setAETGroups(aetGroups);
                            userAccess.updateRole(role);
                            Auditlog.logSoftwareConfiguration(true, "Role "+role+" updated.");
                        }
                        allRolenames.setObject(userAccess.getAllRoles());
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
                
                private String getUpdateInfo(Role role, List<String> groupList, Set<String> aetGroups) {
                    StringBuilder sb = new StringBuilder("Role ").append(role).append(" updated.");
                    boolean changed = Auditlog.addChange(sb, false, "rolename", role.getRolename(), rolename.getObject());
                    Auditlog.addChange(sb, changed, "description", role.getDescription(), description.getObject());
                    Auditlog.addChange(sb, changed, "superuser", role.isSuperuser(), hiddenRoleCheckbox.getModelObject());
                    Auditlog.addChange(sb, changed, "WEB role", role.isWebRole(), groupList.contains(webRoleUuid.toString()));
                    Auditlog.addChange(sb, changed, "DICOM role", role.isDicomRole(), groupList.contains(dicomRoleUuid.toString()));
                    Auditlog.addChange(sb, changed, "AET role", role.isAETRole(), groupList.contains(aetRoleUuid.toString()));
                    return sb.toString();
                }

            });
        }
    };

    public static String getModuleName() {
        return "roletypes";
    }
}
