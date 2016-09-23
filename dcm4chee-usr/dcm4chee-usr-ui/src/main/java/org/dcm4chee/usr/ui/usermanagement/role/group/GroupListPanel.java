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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Page;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.resources.CompressedResourceReference;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.dcm4chee.icons.ImageManager;
import org.dcm4chee.icons.behaviours.ImageSizeBehaviour;
import org.dcm4chee.usr.dao.UserAccess;
import org.dcm4chee.usr.model.Group;
import org.dcm4chee.usr.ui.config.delegate.UsrCfgDelegate;
import org.dcm4chee.usr.ui.usermanagement.role.RoleListPanel;
import org.dcm4chee.usr.ui.util.CSSUtils;
import org.dcm4chee.usr.util.JNDIUtils;
import org.dcm4chee.web.common.base.BaseWicketApplication;
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
 * @since 01.07.2010
 */
public class GroupListPanel extends Panel {

    private static final long serialVersionUID = 1L;
    
    UserAccess userAccess;
    
    private ListModel<Group> allGroups;
    private ConfirmationWindow<Group> confirmationWindow;
    private ModalWindow modalWindow;
    
    int[] winSize;
    
    public GroupListPanel(String id) {
        super(id);

        winSize = UsrCfgDelegate.getInstance().getWindowSize("editGroup");
        
        userAccess = (UserAccess) JNDIUtils.lookup(UserAccess.JNDI_NAME);
        setOutputMarkupId(true);

        allGroups = new ListModel<Group>(getAllGroups());

        add(this.confirmationWindow = new ConfirmationWindow<Group>("confirmation-window") {

            private static final long serialVersionUID = 1L;

            @Override
            public void onConfirmation(AjaxRequestTarget target, Group group) {
                userAccess.removeGroup(group);
                Auditlog.logSoftwareConfiguration(true, "Role Group "+group.getGroupname()+" removed.");
                target.addComponent(GroupListPanel.this);
                allGroups.setObject(getAllGroups());
            }
        });

        add(modalWindow = new ModalWindow("modal-window"));
        add(new ModalWindowLink("toggle-group-form-link", modalWindow, winSize[0], winSize[1]) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                modalWindow
                .setPageCreator(new ModalWindow.PageCreator() {
                    
                    private static final long serialVersionUID = 1L;
                      
                    @Override
                    public Page createPage() {
                        return new CreateOrEditGroupPage(modalWindow, allGroups, null);
                    }
                });
                super.onClick(target);
            }
        }
      .add(new Image("toggle-group-form-image", ImageManager.IMAGE_USER_ROLE_GROUP_ADD)
      .add(new ImageSizeBehaviour("vertical-align: middle;")))
      .add(new Label("grouplist.add-group-form.title", new ResourceModel("grouplist.add-group-form.title")))
      .add(new TooltipBehaviour("grouplist."))
      .add(new SecurityBehavior(getModuleName() + ":newGroupLink"))
      );
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        
        RepeatingView groupRows = new RepeatingView("group-rows");
        addOrReplace(groupRows);
        
        for (int i = 0; i < this.allGroups.getObject().size(); i++) {
            final Group group = this.allGroups.getObject().get(i);
            
            WebMarkupContainer rowParent;
            groupRows.add((rowParent = new WebMarkupContainer(groupRows.newChildId()))
                    .add(new Label("groupname", group.getGroupname())
                    .add(new AttributeModifier("title", true, new Model<String>(group.getDescription()))))
            );
            rowParent.add(new AttributeModifier("style", true, new Model<String>("background-color: " + group.getColor())));
            rowParent.add(new Label("color", group.getColor()));
            rowParent.add((new ModalWindowLink("edit-group-link", modalWindow, winSize[0], winSize[1]) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    modalWindow
                    .setPageCreator(new ModalWindow.PageCreator() {
                        
                        private static final long serialVersionUID = 1L;
                          
                        @Override
                        public Page createPage() {
                            return new CreateOrEditGroupPage(modalWindow, allGroups, group);
                        }
                    });
                    super.onClick(target);
                }
            })
            .add(new Image("grouplist.edit.image", ImageManager.IMAGE_COMMON_DICOM_EDIT)
            .add(new TooltipBehaviour("grouplist.", "edit-group-link", new Model<String>(group.getGroupname())))
            .add(new ImageSizeBehaviour("vertical-align: middle;")))
            .add(new SecurityBehavior(getModuleName() + ":editGroupLink"))
            );

            rowParent.add((new AjaxFallbackLink<Object>("remove-group-link") {

                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    confirmationWindow.confirm(target, 
                    		new StringResourceModel("grouplist.remove-group-link.confirmation", GroupListPanel.this, null, new Object[] {group.getGroupname()}), group);
                }
            }
            .add(new Image("grouplist.delete.image", ImageManager.IMAGE_COMMON_REMOVE)
            .add(new TooltipBehaviour("grouplist.", "remove-group-link", new Model<String>(group.getGroupname()))))
            .add(new ImageSizeBehaviour()))
            .add(new AttributeModifier("class", true, new Model<String>(CSSUtils.getRowClass(i))))
            .add(new SecurityBehavior(getModuleName() + ":removeGroupLink"))
            .setVisible(!group.getGroupname().equals("Web") && !group.getGroupname().equals("Dicom")));
        }
    }

    private ArrayList<Group> getAllGroups() {
        ArrayList<Group> allGroups = new ArrayList<Group>();
        allGroups.addAll(userAccess.getAllGroups());
        return allGroups;
    }

    public static String getModuleName() {
        return "grouplist";
    }
}
