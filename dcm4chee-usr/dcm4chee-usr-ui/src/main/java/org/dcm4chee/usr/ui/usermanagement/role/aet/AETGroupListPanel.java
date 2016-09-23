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

package org.dcm4chee.usr.ui.usermanagement.role.aet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.json.JSONNull;
import net.sf.json.JSONObject;

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
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.Link;
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
import org.dcm4chee.usr.entity.UserRoleAssignment;
import org.dcm4chee.usr.model.AETGroup;
import org.dcm4chee.usr.model.Role;
import org.dcm4chee.usr.model.Group;
import org.dcm4chee.usr.ui.config.delegate.UsrCfgDelegate;
import org.dcm4chee.usr.ui.usermanagement.user.UserListPanel;
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
 * @since Apr. 19, 2011
 */
public class AETGroupListPanel extends Panel {

    private static final long serialVersionUID = 1L;
    
    UserAccess userAccess;
    
    private ListModel<AETGroup> allAETGroups = new ListModel<AETGroup>();
    
    private ConfirmationWindow<AETGroup> confirmationWindow;
    private ModalWindow aetGroupWindow;
    private ModalWindow aetAssignmentWindow;
    private Map<String,int[]> windowsizeMap = new LinkedHashMap<String, int[]>();
    
    public AETGroupListPanel(String id) {
        super(id);
        windowsizeMap.put("editAETGroup", UsrCfgDelegate.getInstance().getWindowSize("editAETGroup"));
        windowsizeMap.put("aetAssignment", UsrCfgDelegate.getInstance().getWindowSize("aetAssignment"));
        
        userAccess = (UserAccess) JNDIUtils.lookup(UserAccess.JNDI_NAME);        
        setOutputMarkupId(true);

        allAETGroups.setObject(userAccess.getAllAETGroups());

        add(aetGroupWindow = new ModalWindow("aet-group-window"));
        add(aetAssignmentWindow = new ModalWindow("aet-assignment-window"));
        
        add(this.confirmationWindow = new ConfirmationWindow<AETGroup>("confirmation-window") {

            private static final long serialVersionUID = 1L;

            @Override
            public void onConfirmation(AjaxRequestTarget target, AETGroup aetGroup) {
                userAccess.removeAETGroup(aetGroup);
                Auditlog.logSoftwareConfiguration(true, "AEGroup "+aetGroup+" removed.");
                target.addComponent(AETGroupListPanel.this);
                allAETGroups.setObject(userAccess.getAllAETGroups());
            }
        });

        int[] winSize = windowsizeMap.get("editAETGroup");
        add(new ModalWindowLink("toggle-aet-group-form-link", aetGroupWindow, winSize[0], winSize[1]) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                aetGroupWindow
                .setPageCreator(new ModalWindow.PageCreator() {
                    
                    private static final long serialVersionUID = 1L;
                      
                    @Override
                    public Page createPage() {
                        return new CreateOrEditAETGroupPage(aetGroupWindow, allAETGroups, null);
                    }
                });
                super.onClick(target);
            }
        }
      .add(new Image("toggle-aet-group-form-image", ImageManager.IMAGE_USER_AET_GROUP_ADD)
      .add(new ImageSizeBehaviour("vertical-align: middle;")))
      .add(new Label("aetgrouplist.add-aet-group-form.title", new ResourceModel("aetgrouplist.add-aet-group-form.title")))
      .add(new TooltipBehaviour("aetgrouplist."))
      .add(new SecurityBehavior(getModuleName() + ":newAETGroupLink"))
      );
        
        add((new Label("groupname", new ResourceModel("aetgrouplist.universalmatch.label"))
            .add(new AttributeModifier("title", true, new ResourceModel("aetgrouplist.universalmatch.description"))))
        );
        add(new Label("assigned-aets", new ResourceModel("aetgrouplist.universalmatch.text")));
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();

        this.allAETGroups.setObject(userAccess.getAllAETGroups());
        
        RepeatingView aetGroupRows = new RepeatingView("aet-group-rows");
        addOrReplace(aetGroupRows);

        WebMarkupContainer rowParent;
        for (int i = 0; i < this.allAETGroups.getObject().size(); i++) {
            final AETGroup aetGroup = this.allAETGroups.getObject().get(i);

            aetGroupRows.add((rowParent = new WebMarkupContainer(aetGroupRows.newChildId()))
                    .add(new Label("groupname", aetGroup.getGroupname())
                    .add(new AttributeModifier("title", true, new Model<String>(aetGroup.getDescription()))))
            );
            
            int[] winSize = windowsizeMap.get("editAETGroup");
            rowParent.add((new ModalWindowLink("edit-aet-group-link", aetGroupWindow, winSize[0], winSize[1]) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    aetGroupWindow
                    .setPageCreator(new ModalWindow.PageCreator() {
                        
                        private static final long serialVersionUID = 1L;
                          
                        @Override
                        public Page createPage() {
                            return new CreateOrEditAETGroupPage(aetGroupWindow, allAETGroups, aetGroup);
                        }
                    });
                    super.onClick(target);
                }
            })
            .add(new Image("aetgrouplist.edit.image", ImageManager.IMAGE_COMMON_DICOM_EDIT)
            .add(new TooltipBehaviour("aetgrouplist.", "edit-aet-group-link", new Model<String>(aetGroup.getGroupname())))
            .add(new ImageSizeBehaviour("vertical-align: middle;")))
            .add(new SecurityBehavior(getModuleName() + ":editAETGroupLink")));

            rowParent.add((new AjaxFallbackLink<Object>("remove-aet-group-link") {

                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    confirmationWindow.confirm(target, 
                    		new StringResourceModel("aetgrouplist.remove-aet-group-link.confirmation", AETGroupListPanel.this, null, new Object[] {aetGroup.getGroupname()}), aetGroup);
                }
            }
            .add(new Image("aetgrouplist.delete.image", ImageManager.IMAGE_COMMON_REMOVE)
            .add(new TooltipBehaviour("aetgrouplist.", "remove-aet-group-link", new Model<String>(aetGroup.getGroupname()))))
            .add(new ImageSizeBehaviour()))
            .setVisible(!userAccess.getUserRoleName().equals(aetGroup.getGroupname())
                    && !userAccess.getAdminRoleName().equals(aetGroup.getGroupname()))
            .add(new AttributeModifier("class", true, new Model<String>(CSSUtils.getRowClass(i))))
            .add(new SecurityBehavior(getModuleName() + ":removeAETGroupLink"))
            );

            winSize = windowsizeMap.get("aetAssignment");
            rowParent.add((new ModalWindowLink("aetAssignment-link", aetAssignmentWindow, winSize[0], winSize[1]) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    aetAssignmentWindow
                        .setPageCreator(new ModalWindow.PageCreator() {
                      
                            private static final long serialVersionUID = 1L;
                        
                                @Override
                                public Page createPage() {
                                    return new AETAssignmentPage(
                                            aetAssignmentWindow, 
                                            aetGroup);                                    
                                }
                        });
                    super.onClick(target);
                }
            }).add(new Image("aetgrouplist.aetAssignment.image", ImageManager.IMAGE_USER_AET_GROUP_AETS)
                .add(new TooltipBehaviour("aetgrouplist.", "aet-assignment-link")))
                .add(new SecurityBehavior(getModuleName() + ":aetAssignmentLink")));

            StringBuffer assignedAETs = new StringBuffer();
            for (String aet : aetGroup.getAets())
                assignedAETs.append(aet).append(", ");
            if (assignedAETs.length() > 0) 
                assignedAETs.delete(assignedAETs.length() - 2, assignedAETs.length() - 1);
            rowParent.add(new Label("assigned-aets", assignedAETs.toString()));
        }
    }
    
    public static String getModuleName() {
        return "aetgrouplist";
    }
}
