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
import org.dcm4chee.usr.model.AETGroup;
import org.dcm4chee.usr.ui.validator.AETGroupValidator;
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
 * @since Apr. 19, 2011
 */
public class CreateOrEditAETGroupPage extends SecureSessionCheckPage {
    
    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(CreateOrEditAETGroupPage.class);

    protected ModalWindow window;
   
    public CreateOrEditAETGroupPage(final ModalWindow window, ListModel<AETGroup> allAETGroupnames, AETGroup aetGroup) {
        super();
        this.window = window;
        add(new CreateOrEditAETGroupForm("add-aet-group-form", allAETGroupnames, aetGroup));
        
        add(new WebMarkupContainer("create-aet-group-title").setVisible(aetGroup == null));
        add(new WebMarkupContainer("edit-aet-group-title").setVisible(aetGroup != null));
    }

    private final class CreateOrEditAETGroupForm extends BaseForm {
        
        private static final long serialVersionUID = 1L;

        private Model<String> groupname = new Model<String>();
        private Model<String> description= new Model<String>();
        private TextField<String> groupnameTextField= new TextField<String>("aetgrouplist.add-aet-group-form.groupname.input", groupname);
        private TextField<String> descriptionTextField= new TextField<String>("aetgrouplist.add-aet-group-form.description.input", description);
        
        public CreateOrEditAETGroupForm(String id, final ListModel<AETGroup> allAETGroupnames, final AETGroup aetGroup) {
            super(id);

            ((BaseWicketApplication) getApplication()).getInitParameter("UserAccessServiceName");
            
            add(groupnameTextField
                    .setRequired(true)
                    .add(new AETGroupValidator(allAETGroupnames, (aetGroup == null ? null : aetGroup.getGroupname())))
            );
            add(descriptionTextField);

            if (aetGroup != null) {
                groupnameTextField.setModelObject(aetGroup.getGroupname());
                descriptionTextField.setModelObject(aetGroup.getDescription());
            }
            
            add(new AjaxFallbackButton("add-aet-group-submit", CreateOrEditAETGroupForm.this) {
                
                private static final long serialVersionUID = 1L;

                @Override
                protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                    try {
                        UserAccess userAccess = (UserAccess) JNDIUtils.lookup(UserAccess.JNDI_NAME);
                        if (aetGroup == null) {
                            AETGroup newAETGroup = new AETGroup(groupname.getObject());
                            newAETGroup.setDescription(description.getObject());
                            userAccess.addAETGroup(newAETGroup);
                            Auditlog.logSoftwareConfiguration(true, "AEGroup "+newAETGroup+" created.");
                        } else {
                            StringBuilder sb = new StringBuilder("AEGroup ").append(aetGroup.getGroupname())
                            .append(" updated. ");
                            boolean changed = Auditlog.addChange(sb, false, "group name", aetGroup.getGroupname(), groupname.getObject());
                            Auditlog.addChange(sb, changed, "description", aetGroup.getDescription(), aetGroup.getDescription());
                            aetGroup.setGroupname(groupname.getObject());
                            aetGroup.setDescription(description.getObject());
                            userAccess.updateAETGroup(aetGroup);
                            Auditlog.logSoftwareConfiguration(true, sb.toString());
                        }
                        allAETGroupnames.setObject(userAccess.getAllAETGroups());
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
