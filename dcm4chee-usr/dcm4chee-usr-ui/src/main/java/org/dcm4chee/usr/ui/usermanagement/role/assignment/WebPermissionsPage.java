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

package org.dcm4chee.usr.ui.usermanagement.role.assignment;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.resources.CompressedResourceReference;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.dcm4chee.usr.dao.UserAccess;
import org.dcm4chee.usr.model.Role;
import org.dcm4chee.usr.ui.config.delegate.UsrCfgDelegate;
import org.dcm4chee.usr.ui.util.CSSUtils;
import org.dcm4chee.usr.util.JNDIUtils;
import org.dcm4chee.web.common.base.BaseWicketApplication;
import org.dcm4chee.web.common.base.BaseWicketPage;
import org.dcm4chee.web.common.secure.SecureSession;
import org.dcm4chee.web.common.secure.SecureSessionCheckPage;
import org.dcm4chee.web.common.secure.SecurityBehavior;
import org.dcm4chee.web.common.util.Auditlog;
/**
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision$ $Date$
 * @since Jan. 10, 2011
 */
public class WebPermissionsPage extends SecureSessionCheckPage {

    private static final long serialVersionUID = 1L;
    
    UserAccess userAccess;
    private Role role;
    
    public WebPermissionsPage(final ModalWindow modalWindow, Role role) {
        super();
        userAccess = (UserAccess) JNDIUtils.lookup(UserAccess.JNDI_NAME);        
        this.role = role;
        setOutputMarkupId(true);
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        
        addOrReplace(new Label("rolename", role.getRolename())
            .add(new AttributeModifier("title", true, new Model<String>(role.getDescription()))
            )
        );

        RepeatingView principalRows = new RepeatingView("principal-rows");
        addOrReplace(principalRows);
        
        Map<String, String> principalsAndKeys = ((SecureSession) getSession()).getAllSwarmPrincipals();
        Iterator<String> principals = principalsAndKeys.keySet().iterator();

        boolean showStudyPermissionRights = UsrCfgDelegate.getInstance().getShowStudyPermissionRights();
        String studyPermissionsAll = BaseWicketApplication.get().getInitParameter("StudyPermissionsAllRolename");
        String studyPermissionsOwn = BaseWicketApplication.get().getInitParameter("StudyPermissionsOwnRolename");
        String studyPermissionsPropagation = BaseWicketApplication.get().getInitParameter("StudyPermissionsPropagationRolename");
        
        int i = 0;
        while(principals.hasNext()) {
            String principal = principals.next();

            if (!showStudyPermissionRights && (principal.equals(studyPermissionsAll)
                    || principal.equals(studyPermissionsOwn)
                    || principal.equals(studyPermissionsPropagation)))
                continue;
            
            WebMarkupContainer rowParent;
            String key = principalsAndKeys.get(principal);
            
            principalRows.add((rowParent = new WebMarkupContainer(principalRows.newChildId()))
                    .add(new Label("principalname", key == null ? new Model<String>(principal) : 
                        new ResourceModel(key + ".name", principal))
                    .add(new AttributeModifier("title", true, key == null ? new Model<String>(principal) : 
                        new ResourceModel(key + ".tooltip", key)))
                    )
            );
            rowParent.add(new AttributeModifier("class", true, new Model<String>(CSSUtils.getRowClass(i++))));

            AjaxCheckBox principalCheckbox = new AjaxCheckBox("principal-checkbox", new HasPrincipalModel(role, principal)) {

                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    target.addComponent(this);
                }
                  
                @Override
                protected void onComponentTag(ComponentTag tag) {
                    super.onComponentTag(tag);
                    tag.put("title", new ResourceModel(((HasPrincipalModel) this.getModel()).getObject().booleanValue() ? "webPermissions.has-principal-checkbox.remove.tooltip" : "webPermissions.has-principal-checkbox.add.tooltip").wrapOnAssignment(this).getObject());
                }
            };
            principalCheckbox.add(new SecurityBehavior(getModuleName() + ":changePrincipalAssignmentCheckbox"));
            principalRows.add(rowParent.add(principalCheckbox));
        }
    }
    
    private final class HasPrincipalModel implements IModel<Boolean> {
        
        private static final long serialVersionUID = 1L;
        
        private Role role;
        private String principalname;
        
        public HasPrincipalModel(Role role, String principal) {
            this.role = role;
            this.principalname = principal;
        }
        
        @Override
        public Boolean getObject() {
            return role.getSwarmPrincipals().contains(principalname);
        }
        
        @Override
        public void setObject(Boolean hasPrincipal) {
            Set<String> swarmPrincipals = role.getSwarmPrincipals();
            if (hasPrincipal) 
                swarmPrincipals.add(principalname);
            else 
                swarmPrincipals.remove(principalname);
            role.setSwarmPrincipals(swarmPrincipals);
            userAccess.updateRole(role);
            Auditlog.logSoftwareConfiguration(true, "Role "+role+": WEB right "+principalname+" "+ (hasPrincipal ? " assigned" : "unassigned"));
        }

        @Override
        public void detach() {
        }
    }
    
    public static String getModuleName() {
        return "webpermissions";
    }
}
