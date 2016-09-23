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

package org.dcm4chee.web.common.behaviours;

import java.io.Serializable;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 * @version $Revision$ $Date$
 * @since Nov 14, 2009
 */

public class FocusOnLoadBehaviour extends AbstractBehavior {

    private static final long serialVersionUID = 1L;
    
    private Component component;
    private FocusStrategy focusStrategy;
    
    public FocusOnLoadBehaviour() {}

    public FocusOnLoadBehaviour(FocusStrategy strategy) {
        focusStrategy = strategy;
    }
    
    public static FocusOnLoadBehaviour newSimpleFocusBehaviour() {
        FocusOnLoadBehaviour fb = new FocusOnLoadBehaviour();
        fb.focusStrategy = fb.new SimpleFocusStrategy();
        return fb;
    }
    public static FocusOnLoadBehaviour newFocusAndSelectBehaviour() {
        FocusOnLoadBehaviour fb = new FocusOnLoadBehaviour();
        fb.focusStrategy = fb.new FocusAndSelectTextStrategy();
        return fb;
    }
    public static FocusOnLoadBehaviour newComponentModelFocusBehaviour(IModel<Component> m) {
        FocusOnLoadBehaviour fb = new FocusOnLoadBehaviour();
        fb.focusStrategy = fb.new ComponentModelFocusStrategy(m);
        return fb;
    }
    
    public void bind( Component component ) {
        this.component = component;
        if ( focusStrategy == null ) {
            if (component instanceof FormComponent<?>) {
                focusStrategy = new EmptyFocusStrategy();
            } else if (component instanceof Form<?>) {
                focusStrategy = new FirstEmptyTextfieldFocusStrategy();
            }
        }
        component.setOutputMarkupId(true);
    }

    public void renderHead( IHeaderResponse headerResponse ) {
        super.renderHead(headerResponse);
        focusStrategy.focus(headerResponse, component);
    }

    public boolean isTemporary() {
        return false;
    }
    
    private boolean setFocusOnEmpty(IHeaderResponse headerResponse, Component c) {
        Object o = c.getDefaultModelObject();
        if ( o == null || o.toString().length() < 1) {
            headerResponse.renderOnLoadJavascript(getJavaScriptString(c));
            return true;
        }
        return false;
    }
    private String getJavaScriptString(Component c) {
        return "self.focus();var elem=document.getElementById('"+
                c.getMarkupId() + "');elem.focus()";
    }

    public interface FocusStrategy extends Serializable {
        void focus( IHeaderResponse headerResponse, Component c );
    }
    
    public class SimpleFocusStrategy implements FocusStrategy {

        private static final long serialVersionUID = 1L;

        public void focus(IHeaderResponse headerResponse, Component c) {
            headerResponse.renderOnLoadJavascript(getJavaScriptString(c));
        }
    }

    public class ComponentModelFocusStrategy implements FocusStrategy {

        private static final long serialVersionUID = 1L;
        
        private IModel<Component> focusComponentModel;
        public ComponentModelFocusStrategy(IModel<Component> c) {
            focusComponentModel = c;
        }
        public void focus(IHeaderResponse headerResponse, Component c) {
            if (focusComponentModel.getObject() != null) {
                headerResponse.renderOnLoadJavascript(getJavaScriptString(focusComponentModel.getObject()));
            }
        }
    }

    public class EmptyFocusStrategy implements FocusStrategy {

        private static final long serialVersionUID = 1L;

        public void focus(IHeaderResponse headerResponse, Component c) {
            setFocusOnEmpty(headerResponse, c);
        }
    }

    public class FocusAndSelectTextStrategy implements FocusStrategy {

        private static final long serialVersionUID = 1L;

        public void focus(IHeaderResponse headerResponse, Component c) {
            if ( c instanceof TextField<?>) {
                headerResponse.renderOnLoadJavascript(getJavaScriptString(c)+";elem.select()");
            }
        }
    }
    
    public class FirstEmptyTextfieldFocusStrategy implements FocusStrategy {

        private static final long serialVersionUID = 1L;

        public void focus(IHeaderResponse headerResponse, Component fc) {
            Form<?> form = (Form<?>)fc;
            Component c;
            for ( int i=0 ; i<form.size() ; i++) {
                c = form.get(i);
                if (c instanceof TextField<?>) {
                    if ( setFocusOnEmpty(headerResponse, c) ) {
                        c.setOutputMarkupId(true);
                        break;
                    }
                }
            }
        }
    }
}
