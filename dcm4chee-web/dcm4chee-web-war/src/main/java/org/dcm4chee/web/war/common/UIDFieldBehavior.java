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
 * Portions created by the Initial Developer are Copyright (C) 2008
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

package org.dcm4chee.web.war.common;

import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.Component.IVisitor;
import org.apache.wicket.Response;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.AbstractTextComponent;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.util.string.AppendingStringBuffer;
import org.apache.wicket.util.string.JavascriptUtils;
import org.dcm4chee.web.dao.util.QueryUtil;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 * @version $Revision: 15859 $ $Date: 2011-08-29 16:03:54 +0200 (Mo, 29 Aug 2011) $
 * @since Sep 05, 2011
 */
public class UIDFieldBehavior extends AjaxEventBehavior {

    private static final long serialVersionUID = 1L;
    
    private Form<?> form;
    
    public UIDFieldBehavior(Form<?> form) {
        super("onchange");
        this.form = form;
    }

    @Override
    protected final CharSequence getEventHandler() {
        AppendingStringBuffer asb = (new AppendingStringBuffer("wicketAjaxPost('").append(
                getCallbackUrl(false)).append(
                        "', wicketSerializeForm(Wicket.$('" + getComponent().getMarkupId() + "'))"));
        return generateCallbackScript(asb);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected final void onEvent(final AjaxRequestTarget target) {
        final FormComponent<String> formComponent = (FormComponent<String>)getComponent();
        boolean b = QueryUtil.isUniversalMatch(formComponent.getModelObject());
        try {
            formComponent.inputChanged();
            formComponent.validate();
            if (formComponent.hasErrorMessage()) {
                formComponent.invalid();
            } else {
                formComponent.valid();
                formComponent.updateModel();
                if (b != QueryUtil.isUniversalMatch(formComponent.getModelObject())) {
                    updateOtherFields(target.getPage().getRequest().getParameterMap(), formComponent.getInputName());
                    addToTarget(target);
                }
            }
        } catch (RuntimeException e) {
            onError(target, e);
        }
    }
    private void updateOtherFields(Map<String, String[]> parameterMap, String inputName) {
        FormComponent<?> c;
        for (String name : parameterMap.keySet()) {
            if (inputName.equals(name))
                continue;
            c = (FormComponent<?>) form.get(name);
            if (c != null) {
                c.inputChanged();
                c.validate();
                if (c.hasErrorMessage()) {
                    c.invalid();
                } else {
                    c.valid();
                    c.updateModel();
                }
            }
        }
    }

    @Override
    protected void onComponentRendered() {
        if (getComponent() instanceof AbstractTextComponent)  {
            Response response = getComponent().getResponse();
            final String id = getComponent().getMarkupId();
            response.write(JavascriptUtils.SCRIPT_OPEN_TAG);
            response.write("new Wicket.ChangeHandler('" + id + "');");
            response.write(JavascriptUtils.SCRIPT_CLOSE_TAG);
        }
    }

    protected void onError(AjaxRequestTarget target, RuntimeException e) {
        if (e != null) {
            throw e;
        }
    }
    
    private void addToTarget(final AjaxRequestTarget target) {
        final String excludeId = getComponent().getId();
        IVisitor<Component> visitor = new IVisitor<Component>() {
            public Object component(Component c) {
                if ( c instanceof FormComponent<?> && c.getOutputMarkupId() && !c.getId().equals(excludeId) ) {
                    target.addComponent(c);
                }
                return IVisitor.CONTINUE_TRAVERSAL;
            }
        };
        form.visitChildren(visitor);
    }
    
}
