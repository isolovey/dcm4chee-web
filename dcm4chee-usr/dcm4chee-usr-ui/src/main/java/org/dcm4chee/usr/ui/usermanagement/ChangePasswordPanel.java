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

package org.dcm4chee.usr.ui.usermanagement;

import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxFallbackButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.validation.EqualPasswordInputValidator;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.resources.CompressedResourceReference;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.dcm4chee.usr.dao.UserAccess;
import org.dcm4chee.usr.entity.User;
import org.dcm4chee.usr.ui.validator.PasswordValidator;
import org.dcm4chee.usr.util.JNDIUtils;
import org.dcm4chee.web.common.base.BaseWicketPage;
import org.dcm4chee.web.common.markup.BaseForm;
import org.dcm4chee.web.common.secure.SecureSession;
import org.dcm4chee.web.common.util.Auditlog;
import org.dcm4chee.web.common.util.SecurityUtils;

/**
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision$ $Date$
 * @since Sept. 28, 2009
 */
public class ChangePasswordPanel extends Panel {
    
    private static final long serialVersionUID = 1L;
    
    public ChangePasswordPanel(String id) {
        this(id, ((SecureSession) RequestCycle.get().getSession()).getUsername(), null, null);
    }

    public ChangePasswordPanel(String id, String userId, final User forUser, final ModalWindow window) {
        super(id);
        add(new ChangePasswordForm("change-password-form", userId, forUser, new Model<String>(), new Model<String>(), window));
    }

    private final class ChangePasswordForm extends BaseForm {

        private static final long serialVersionUID = 1L;

        private User userToChange;
        private Label resultLabel; 
            
        public ChangePasswordForm(String id, String userId, final User forUser, Model<String> oldPassword, final Model<String> newPassword, final ModalWindow window) {
            super(id);
            if (forUser == null) {
                try {
                    userToChange = ((UserAccess) JNDIUtils.lookup(UserAccess.JNDI_NAME)).getUser(userId);
                } catch (Exception ignore) {
                    // cope with case-insensitive usernames
                    userToChange = ((UserAccess) JNDIUtils.lookup(UserAccess.JNDI_NAME)).getUserIgnoreCase(userId);
                }
            } else {
                userToChange = forUser;
            }

            WebMarkupContainer oldPasswordLabel = new WebMarkupContainer("old-password-label");
            this.add(oldPasswordLabel);
                    
            final PasswordTextField oldPasswordTf = new PasswordTextField("change_password.old_password.input", oldPassword);
            this.add(oldPasswordTf);

            Label forUserLabel = new Label("for-user-label", userToChange.getUserID());
            this.add(forUserLabel);
            
            if (userToChange.getUserID().equals(userId)) {
                forUserLabel.setVisible(false);
                this.add(new PasswordValidator(userToChange, oldPasswordTf));
            } else {
                oldPasswordLabel.setVisible(false);
                oldPasswordTf.setVisible(false);
            }
            
            final PasswordTextField newPasswordTf1 = new PasswordTextField("change_password.new_password_1.input", newPassword);
            this.add(newPasswordTf1);

            final PasswordTextField newPasswordTf2 = new PasswordTextField("change_password.new_password_2.input", new Model<String>(""));
            this.add(newPasswordTf2);
       
            this.add(new EqualPasswordInputValidator(newPasswordTf1, newPasswordTf2));

            add(new AjaxFallbackButton("change-password-submit", ChangePasswordForm.this) {
                private static final long serialVersionUID = 1L;
    
                @Override
                protected void onSubmit(AjaxRequestTarget target, Form<?> form) {

                    UserAccess dao = (UserAccess) JNDIUtils.lookup(UserAccess.JNDI_NAME);
                    String encodedPassword = SecurityUtils.encodePassword(newPassword.getObject());
                    dao.updateUser(userToChange.getUserID(), encodedPassword);
                    Auditlog.logSoftwareConfiguration(true, "User "+userToChange.getUserID()+": password changed.");
                    userToChange.setPassword(encodedPassword);
                    if (window != null)
                        window.close(target);
                    else {
                        resultLabel.setVisible(true);
                        target.addComponent(resultLabel);
                        
                        oldPasswordTf.removeAll();
                        newPasswordTf1.removeAll();
                        newPasswordTf2.removeAll();
                        target.addComponent(oldPasswordTf);
                        target.addComponent(newPasswordTf1);
                        target.addComponent(newPasswordTf2);
                    }
                }

                @Override
                protected void onError(AjaxRequestTarget target, Form<?> form) {
                    target.addComponent(form);
                }
            });
            
            add((resultLabel = new Label("resultLabel", new ResourceModel("change_password.result.success")))
                .setVisible(false)
                .setOutputMarkupId(true)
                .setOutputMarkupPlaceholderTag(true)
            );
        }
    }
    
    public static String getModuleName() {
        return "change_password";
    }
}
