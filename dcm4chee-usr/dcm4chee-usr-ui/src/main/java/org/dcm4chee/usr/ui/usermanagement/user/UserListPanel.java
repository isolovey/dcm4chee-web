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

package org.dcm4chee.usr.ui.usermanagement.user;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.resources.CompressedResourceReference;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.dcm4chee.icons.ImageManager;
import org.dcm4chee.icons.behaviours.ImageSizeBehaviour;
import org.dcm4chee.usr.dao.UserAccess;
import org.dcm4chee.usr.entity.User;
import org.dcm4chee.usr.entity.UserRoleAssignment;
import org.dcm4chee.usr.model.Role;
import org.dcm4chee.usr.ui.config.delegate.UsrCfgDelegate;
import org.dcm4chee.usr.ui.usermanagement.ChangePasswordLink;
import org.dcm4chee.usr.ui.util.CSSUtils;
import org.dcm4chee.usr.util.JNDIUtils;
import org.dcm4chee.web.common.base.BaseWicketPage;
import org.dcm4chee.web.common.behaviours.TooltipBehaviour;
import org.dcm4chee.web.common.markup.ModalWindowLink;
import org.dcm4chee.web.common.markup.modal.ConfirmationWindow;
import org.dcm4chee.web.common.secure.SecureSession;
import org.dcm4chee.web.common.secure.SecurityBehavior;
import org.dcm4chee.web.common.util.Auditlog;

/**
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision$ $Date$
 * @since 28.09.2009
 */
public class UserListPanel extends Panel {

    private static final long serialVersionUID = 1L;
    
    private UserAccess userAccess;
    
    private ListModel<User> allUsers;
    private ListModel<Role> allRoles;
    List<String> roleTypes;

    private String userId;

    private ModalWindow addUserWindow;
    private ModalWindow changePasswordWindow;
    private ModalWindow manageRolesWindow;
    private ConfirmationWindow<User> confirmationWindow;

    public UserListPanel(String id) {
        super(id);
        userAccess = (UserAccess) JNDIUtils.lookup(UserAccess.JNDI_NAME);

        setOutputMarkupId(true);
        
        this.userId = ((SecureSession) getSession()).getUsername();
        add(this.changePasswordWindow = new ModalWindow("change-password-window"));
        int[] winSize = UsrCfgDelegate.getInstance().getWindowSize("changePassword");
        if (winSize != null) {
            changePasswordWindow.setInitialWidth(winSize[0]);
            changePasswordWindow.setInitialHeight(winSize[1]);
        }
               
        this.allUsers = new ListModel<User>(userAccess.getAllUsers());
        this.allRoles = new ListModel<Role>(userAccess.getAllRoles());
        
        add(this.confirmationWindow = new ConfirmationWindow<User>("confirmation-window") {

            private static final long serialVersionUID = 1L;

            @Override
            public void onConfirmation(AjaxRequestTarget target, User userObject) {
                ((UserAccess) JNDIUtils.lookup(UserAccess.JNDI_NAME)).deleteUser(userObject.getUserID());
                Auditlog.logSoftwareConfiguration(true, "User "+userObject.getUserID()+" deleted.");
                target.addComponent(UserListPanel.this);
                allUsers.setObject(userAccess.getAllUsers());
            }
        });
        
        add(addUserWindow = new ModalWindow("add-user-window")
            .setPageCreator(new ModalWindow.PageCreator() {
                
                private static final long serialVersionUID = 1L;
                  
                @Override
                public Page createPage() {
                    return new AddUserPage(addUserWindow, allUsers);
                }
            })
        );
        
        winSize = UsrCfgDelegate.getInstance().getWindowSize("addUser");
        add(new ModalWindowLink("toggle-user-form-link", addUserWindow, winSize[0], winSize[1])
        .add(new Image("toggle-user-form-image", ImageManager.IMAGE_USER_ADD)
        .add(new ImageSizeBehaviour("vertical-align: middle;")))
        .add(new Label("userlist.add-user-form.title", new ResourceModel("userlist.add-user-form.title")))
        .add(new TooltipBehaviour("userlist."))
        .add(new SecurityBehavior(getModuleName() + ":newUserLink"))        
        );
        
        add(manageRolesWindow = new ModalWindow("manage-roles-window"));
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();

        this.allUsers.setObject(userAccess.getAllUsers());
        this.allRoles.setObject(userAccess.getAllRoles());

        RepeatingView roleRows = new RepeatingView("role-rows");
        addOrReplace(roleRows);

        for (int i = 0; i < this.allUsers.getObject().size(); i++) {
            
            final User user = this.allUsers.getObject().get(i);
           	
            WebMarkupContainer rowParent;
            roleRows.add((rowParent = new WebMarkupContainer(roleRows.newChildId())).add(new Label("userID", user.getUserID())));

            for (Role role : allRoles.getObject())
            	if (role.isSuperuser())
            		for (UserRoleAssignment ura : user.getRoles())
            			if (ura.getRole().equals(role.getRolename()))
            				rowParent.add(new SecurityBehavior(getModuleName() + ":superuserUserRow"));

            StringBuffer assignedRoles = new StringBuffer();
            for (UserRoleAssignment ura : user.getRoles()) 
                assignedRoles.append(ura.getRole()).append(", ");
           	
            ChangePasswordLink changePasswordLink
                = new ChangePasswordLink("change-password-link", this.changePasswordWindow, this.userId, user);
            changePasswordLink.add(new Image("img-change-password", ImageManager.IMAGE_USER_CHANGE_PASSWORD)
                    .add(new ImageSizeBehaviour()))
                    .add(new AttributeModifier("title", true, new Model<String>(new ResourceModel("userlist.change_password.tooltip").wrapOnAssignment(this).getObject()))
             );           
            rowParent.add(changePasswordLink)
                .add(new AttributeModifier("class", true, new Model<String>(CSSUtils.getRowClass(i))));
            changePasswordLink.add(new SecurityBehavior(getModuleName() + ":changePasswordLink"));

            AjaxFallbackLink<Object> removeUserLink 
                = new AjaxFallbackLink<Object>("remove-user-link") {

                    private static final long serialVersionUID = 1L;
    
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        confirmationWindow.confirm(target, 
                        		new StringResourceModel("userlist.remove-user-link.confirmation", UserListPanel.this, null, new Object[] {user.getUserID()}), user);
                    }
            };
            removeUserLink.add(new Image("img-delete", ImageManager.IMAGE_COMMON_REMOVE)
                .add(new TooltipBehaviour("userlist.", "remove-user-link", new PropertyModel<String>(user,"userID")))
                .add(new ImageSizeBehaviour()))
                .setVisible(!this.userId.equals(user.getUserID())
            );
            rowParent.add(removeUserLink);
            removeUserLink.add(new SecurityBehavior(getModuleName() + ":removeUserLink"));

            int[] winSize = UsrCfgDelegate.getInstance().getWindowSize("manageRoles");
            rowParent.add((new ModalWindowLink("manage-roles-link", manageRolesWindow, winSize[0], winSize[1]) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    manageRolesWindow
                        .setPageCreator(new ModalWindow.PageCreator() {
                      
                            private static final long serialVersionUID = 1L;
                        
                                @Override
                                public Page createPage() {
                                    return new RoleAssignmentPage(
                                            manageRolesWindow, 
                                            user
                                    );
                                }
                        });
                    super.onClick(target);
                }
            }).add(new Image("img-roles", ImageManager.IMAGE_USER_ROLE_ADD)
                .add(new TooltipBehaviour("userlist.", "manage-roles-link")))
                .add(new SecurityBehavior(getModuleName() + ":manageRolesLink"))
            );

            if (assignedRoles.length() > 0) 
                assignedRoles.delete(assignedRoles.length() - 2, assignedRoles.length() - 1);
                
            rowParent.add(new Label("assigned-roles", assignedRoles.toString()));            
        }
    }
    
    public static String getModuleName() {
        return "userlist";
    }    
}
