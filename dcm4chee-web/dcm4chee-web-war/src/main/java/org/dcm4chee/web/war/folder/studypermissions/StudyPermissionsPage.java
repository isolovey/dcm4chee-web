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

package org.dcm4chee.web.war.folder.studypermissions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.resources.CompressedResourceReference;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.dcm4che2.audit.message.SecurityAlertMessage;
import org.dcm4chee.archive.entity.StudyPermission;
import org.dcm4chee.usr.model.Role;
import org.dcm4chee.usr.ui.util.CSSUtils;
import org.dcm4chee.usr.util.JNDIUtils;
import org.dcm4chee.web.common.base.BaseWicketPage;
import org.dcm4chee.web.common.markup.modal.ConfirmationWindow;
import org.dcm4chee.web.common.secure.SecureSessionCheckPage;
import org.dcm4chee.web.common.util.Auditlog;
import org.dcm4chee.web.dao.folder.StudyPermissionsLocal;
import org.dcm4chee.web.war.StudyPermissionHelper;
import org.dcm4chee.web.war.StudyPermissionHelper.StudyPermissionRight;
import org.dcm4chee.web.war.common.model.AbstractEditableDicomModel;
import org.dcm4chee.web.war.folder.model.PatientModel;
import org.dcm4chee.web.war.folder.model.StudyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision: 17295 $ $Date: 2012-10-18 14:16:35 +0200 (Do, 18 Okt 2012) $
 * @since 21.07.2010
 */
public class StudyPermissionsPage extends SecureSessionCheckPage {
    
    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(StudyPermissionsPage.class);

    private PatientModel patModel;
    private long studyCountForPatient = -1;
   
    private List<StudyPermission> currentStudyPermissions;
    private HashSet<String> allowedActions = new HashSet<String>(7);
    
    private String studyInstanceUID;
    
    private Set<String> studyPermissionActions = new LinkedHashSet<String>();
    
    public StudyPermissionsPage(AbstractEditableDicomModel model) {
        super();
        setOutputMarkupId(true);
        try {
            List<?> servers = MBeanServerFactory.findMBeanServer(null);
            MBeanServerConnection server = null;
            if (servers != null && !servers.isEmpty()) {
                server = (MBeanServerConnection) servers.get(0);
                log.debug("Found MBeanServer:"+server);
            }
        } catch (Exception e) {
            log.error("Failed to get WebConfig Service Attributes: " + e.getMessage());
            return;
        }
        if (model instanceof org.dcm4chee.web.war.folder.model.PatientModel) {
            patModel = (PatientModel) model;
        } else if (model instanceof org.dcm4chee.web.war.folder.model.StudyModel) {
            studyInstanceUID = ((StudyModel) model).getStudyInstanceUID();
        } else
            log.error(this.getClass() + ": No valid model for StudyPermission assignment");
        
        add(new WebMarkupContainer("studyPermissions-patient").setVisible(patModel != null));
        add(new WebMarkupContainer("studyPermissions-study").setVisible(patModel == null));
        
        add(new Label("for-description", (patModel != null ? 
                new StringResourceModel("folder.studyPermissions.description.patient", this, null,new Object[]{((PatientModel) model).getName()}) : 
                    new StringResourceModel("folder.studyPermissions.description.study", this, null,new Object[]{((StudyModel) model).getStudyInstanceUID()})
                )
            )
        );

        StudyPermissionsLocal dao = (StudyPermissionsLocal) JNDIUtils.lookup(StudyPermissionsLocal.JNDI_NAME);
        if (patModel != null) {
            currentStudyPermissions = dao.getStudyPermissionsForPatient(patModel.getPk());
            studyCountForPatient = dao.countStudiesOfPatient(patModel.getPk());
        } else 
            currentStudyPermissions = dao.getStudyPermissions(studyInstanceUID);            
        for (StudyPermission sp : currentStudyPermissions) {
            allowedActions.add(sp.getAction());
        }
        studyPermissionActions.add(StudyPermission.APPEND_ACTION);
        studyPermissionActions.add(StudyPermission.DELETE_ACTION);
        studyPermissionActions.add(StudyPermission.EXPORT_ACTION);
        studyPermissionActions.add(StudyPermission.QUERY_ACTION);
        studyPermissionActions.add(StudyPermission.READ_ACTION);
        studyPermissionActions.add(StudyPermission.UPDATE_ACTION);
        
        RepeatingView actionHeaders = new RepeatingView("action-headers");
        Iterator<String> iterator = studyPermissionActions.iterator();
        while (iterator.hasNext()) {
            String studyPermission = iterator.next();
            actionHeaders.add(new Label(actionHeaders.newChildId(), new ResourceModel("studypermission.action." + studyPermission))
            .add(new AttributeModifier("title", true, new ResourceModel("studypermission.action." + studyPermission))));
        }
        add(actionHeaders);
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        refreshDicomRoles();
    }
    
    public void refreshDicomRoles() {
        RepeatingView roleRows = new RepeatingView("role-rows");
        addOrReplace(roleRows);

        int i = 0;
        for (final Role role : getAllDicomRoles()) {
            WebMarkupContainer rowParent;
            roleRows.add((rowParent = new WebMarkupContainer(roleRows.newChildId()))
                    .add(new Label("rolename", role.getRolename()))
            );

            rowParent.add(new AttributeModifier("class", true, new Model<String>(CSSUtils.getRowClass(i++))));
            RepeatingView actionDividers = new RepeatingView("action-dividers");
            rowParent.add(actionDividers);
           
            Iterator<String> iterator = studyPermissionActions.iterator();
            while (iterator.hasNext()) {
                final String action = iterator.next();
                final Label countLabel = new Label("number-of-studies-label", 
                        new AbstractReadOnlyModel<String>() {
                            private static final long serialVersionUID = 1L;
                            @Override
                            public String getObject() {
                                return patModel == null ? "" : 
                                    (countStudies(role, action) + "/" + studyCountForPatient);
                            }});
                AjaxCheckBox roleCheckbox = new AjaxCheckBox("study-permission-checkbox", new HasStudyPermissionModel(role, action)) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public boolean isEnabled() {
                        return checkStudyPermissionEditable(role, action);
                    }
                    
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        target.addComponent(this);
                        target.addComponent(countLabel);
                    }
                      
                    @Override
                    protected void onComponentTag(ComponentTag tag) {
                        super.onComponentTag(tag);
                        tag.put("title", new ResourceModel(((HasStudyPermissionModel) this.getModel()).getObject().booleanValue() ? "studypermission.has-study-permission-checkbox.add.tooltip" : "studypermission.has-study-permission-checkbox.add.tooltip").wrapOnAssignment(this).getObject());
                    }
                };
                
                actionDividers.add(
                        new WebMarkupContainer(roleRows.newChildId())
                        .add(roleCheckbox)
                        .add(countLabel
                            .setOutputMarkupId(true)
                            .setVisible(patModel != null)
                        )
                );
            }
        }
    }

    private int countStudies(Role role, String action) {
        int count = 0;
        for (StudyPermission sp : currentStudyPermissions) 
            if (sp.getRole().equals(role.getRolename()) && sp.getAction().equals(action)) 
                count++;
        return count;
    }

    private final class HasStudyPermissionModel implements IModel<Boolean> {
        
        private static final long serialVersionUID = 1L;

        private Role role;
        private String action;
        
        public HasStudyPermissionModel(Role role, String action) {
            this.role = role;
            this.action = action;
        }
        
        @Override
        public Boolean getObject() {
            return countStudies(role, action) > 0;
        }
        
        @Override
        public void setObject(Boolean hasStudyPermission) {
            String desc = hasStudyPermission ? "Grant" : "Revoke";
            try {
                if (patModel != null) {
                    desc += " StudyPermissions for patient patId:"+patModel.getId()+"/"+patModel.getIssuer()+
                    " role="+role.getRolename()+" action:"+action+".";
                    List<String> suids;
                    StudyPermissionsLocal dao = (StudyPermissionsLocal) JNDIUtils.lookup(StudyPermissionsLocal.JNDI_NAME);
                    if (hasStudyPermission) {
                        suids = dao.grantForPatient(patModel.getPk(), action, role.getRolename());
                    } else {
                        suids = dao.revokeForPatient(patModel.getPk(), action, role.getRolename());
                    }
                    desc += " Effected StudyIuids: " + (suids.isEmpty() ? "NONE" : toString(suids));
                    currentStudyPermissions = dao.getStudyPermissionsForPatient(patModel.getPk());
                } else {
                    desc += " StudyPermission: StudyIuid="+studyInstanceUID+" role="+role.getRolename()+" action:"+action+".";
                    if (hasStudyPermission) {
                        StudyPermission sp = new StudyPermission();
                        sp.setStudyInstanceUID(studyInstanceUID);
                        sp.setRole(role.getRolename());
                        sp.setAction(action);
                        ((StudyPermissionsLocal) JNDIUtils.lookup(StudyPermissionsLocal.JNDI_NAME)).grant(sp);
                        currentStudyPermissions.add(sp);
                    } else {
                        for (StudyPermission sp : currentStudyPermissions) {
                            if (sp.getRole().equals(role.getRolename()) && sp.getAction().equals(action)) {
                                ((StudyPermissionsLocal) JNDIUtils.lookup(StudyPermissionsLocal.JNDI_NAME)).revoke(sp.getPk());
                                currentStudyPermissions.remove(sp);
                                break;
                            }
                        }
                    }
                }
                Auditlog.logSecurityAlert(SecurityAlertMessage.OBJECT_SECURITY_ATTRIBUTES_CHANGED, true, desc);
            } catch (Exception x) {
                log.error(desc+" failed!", x);
                Auditlog.logSecurityAlert(SecurityAlertMessage.OBJECT_SECURITY_ATTRIBUTES_CHANGED, false, desc);
            }
        }
        
        @Override
        public void detach() {}
        
        protected String toString(List<String> l) {
            if (l == null || l.isEmpty())
                return "";
            StringBuffer sb = new StringBuffer();
            for ( int i=0, len=l.size() ; i < len ; i++ ) {
                sb.append(l.get(i)).append(',');
            }
            sb.setLength(sb.length()-1);
            return sb.toString();
        }
    }

    private boolean checkStudyPermissionRights() {
        StudyPermissionHelper sph = StudyPermissionHelper.get();
        return (!sph.isUseStudyPermissions() || 
                sph.getStudyPermissionRight().equals(StudyPermissionRight.ALL));
    }
    private boolean checkStudyPermissionEditable(Role role, String action) {
        if (checkStudyPermissionRights()) 
            return true;
        StudyPermissionHelper sph = StudyPermissionHelper.get();
        if (sph.getStudyPermissionRight().equals(StudyPermissionRight.OWN))
            return (sph.isPropagationAllowed() || sph.getDicomRoles().contains(role.getRolename())) 
                && allowedActions.contains(action);
        return false;
    }
    
    private ArrayList<Role> getAllDicomRoles() {
        ArrayList<Role> allDicomRoles = new ArrayList<Role>();
        allDicomRoles.addAll(((StudyPermissionsLocal) JNDIUtils.lookup(StudyPermissionsLocal.JNDI_NAME)).getAllDicomRoles());
        return allDicomRoles;
    }
    
    public class ConfirmationWrapperPage extends WebPage {
        
        public ConfirmationWrapperPage(ConfirmationWindow<?> confirmationWindow) {
            add(StudyPermissionsPage.this.getBaseCSSHeaderContributor());
            add(confirmationWindow.getMessageWindowPanel());
        }
    }
    
    public static String getModuleName() {
        return "studypermissions";
    }
}