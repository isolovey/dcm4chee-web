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

import java.util.Iterator;
import java.util.LinkedHashSet;
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
import org.dcm4chee.usr.model.AETGroup;
import org.dcm4chee.usr.ui.util.CSSUtils;
import org.dcm4chee.usr.util.JNDIUtils;
import org.dcm4chee.web.common.base.BaseWicketPage;
import org.dcm4chee.web.common.secure.SecureSessionCheckPage;
import org.dcm4chee.web.common.secure.SecurityBehavior;
import org.dcm4chee.web.common.util.Auditlog;
/**
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision$ $Date$
 * @since Jan. 10, 2011
 */
public class AETAssignmentPage extends SecureSessionCheckPage {

    private static final long serialVersionUID = 1L;
    
    UserAccess userAccess;
    private AETGroup aetGroup;
    private Set<String> aets = new LinkedHashSet<String>();
    
    public AETAssignmentPage(final ModalWindow modalWindow, AETGroup aetGroup) {
        super();
        userAccess = (UserAccess) JNDIUtils.lookup(UserAccess.JNDI_NAME);        
        this.aetGroup = aetGroup;
        setOutputMarkupId(true);
        
        aets.addAll(((UserAccess) JNDIUtils.lookup(UserAccess.JNDI_NAME)).listAETitles());
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        
        addOrReplace(new Label("groupname", aetGroup.getGroupname())
            .add(new AttributeModifier("title", true, new Model<String>(aetGroup.getDescription()))
            )
        );

        RepeatingView aetRows = new RepeatingView("aet-rows");
        addOrReplace(aetRows);
        
        Iterator<String> aetList = aets.iterator();
        int i = 0;
        while(aetList.hasNext()) {
            String aet = aetList.next();
            WebMarkupContainer rowParent;            
            aetRows.add((rowParent = new WebMarkupContainer(aetRows.newChildId()))
                    .add(new Label("aetname", aet)
                    )
            );
            rowParent.add(new AttributeModifier("class", true, new Model<String>(CSSUtils.getRowClass(i++))));

            AjaxCheckBox aetCheckbox = new AjaxCheckBox("aet-checkbox", new HasAETModel(aetGroup, aet)) {

                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    target.addComponent(this);
                }
                  
                @Override
                protected void onComponentTag(ComponentTag tag) {
                    super.onComponentTag(tag);
                    tag.put("title", new ResourceModel(((HasAETModel) this.getModel()).getObject().booleanValue() ? "aetAssignment.has-aet-checkbox.remove.tooltip" : "aetAssignment.has-aet-checkbox.add.tooltip").wrapOnAssignment(this).getObject());
                }
            };
            aetCheckbox.add(new SecurityBehavior(getModuleName() + ":changeAETAssignmentCheckbox"));
            aetRows.add(rowParent.add(aetCheckbox));
        }
    }
    
    private final class HasAETModel implements IModel<Boolean> {
        
        private static final long serialVersionUID = 1L;
        
        private AETGroup aetGroup;
        private String aet;
        
        public HasAETModel(AETGroup aetGroup, String aet) {
            this.aetGroup = aetGroup;
            this.aet = aet;
        }
        
        @Override
        public Boolean getObject() {
            return aetGroup.getAets().contains(aet);
        }
        
        @Override
        public void setObject(Boolean hasAET) {
            Set<String> aets = aetGroup.getAets();
            if (hasAET) 
                aets.add(aet);
            else 
                aets.remove(aet);
            aetGroup.setAets(aets);
            userAccess.updateAETGroup(aetGroup);
            Auditlog.logSoftwareConfiguration(true, "AEGroup "+aetGroup+": AET "+aet+(hasAET ? " added" : " removed"));
        }

        @Override
        public void detach() {
        }
    }
    
    public static String getModuleName() {
        return "aetassignment";
    }
}
