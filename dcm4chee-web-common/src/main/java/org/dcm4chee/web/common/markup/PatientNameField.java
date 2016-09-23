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

package org.dcm4chee.web.common.markup;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 * @version $Revision$ $Date$
 * @since Sept 07, 2010
 */
public class PatientNameField extends FormComponentPanel<String> {

    private static final long serialVersionUID = 1L;
    
    private IModel<Boolean> useFnGn;
    private IModel<Boolean> autoWildcard;

    private Label pnLabel;
    private Label fnLabel;
    private Label gnLabel;
    private TextField<String> pnField;
    private TextField<String> fnField;
    private TextField<String> gnField;
    
    private boolean hidePatientnameLabel;

    private static Logger log = LoggerFactory.getLogger(PatientNameField.class);
    
    public PatientNameField(String id, IModel<String> model) {
        this(id, model, null, null);
    }
    public PatientNameField(String id, IModel<String> model, IModel<Boolean> useFnGn, IModel<Boolean> autoWildcard) {
        super(id, model);
        this.useFnGn = useFnGn;
        this.autoWildcard = autoWildcard;
        setType(String.class);
        pnLabel = new Label("pnLabel", new ResourceModel("patientname.label"));
        add(pnLabel);
        pnField = new TextField<String>("pnField", model);
        add(pnField);
        fnLabel = new Label("fnLabel", new ResourceModel("familyname.label"));
        add(fnLabel);
        fnField = new TextField<String>("fnField", new FamilyNameModel(this));
        add(fnField);
        gnLabel = new Label("gnLabel", new ResourceModel("givenname.label"));
        add(gnLabel);
        gnField = new TextField<String>("gnField", new GivenNameModel(this));
        add(gnField);
    }
    
    @Override
    public void onBeforeRender() {
        super.onBeforeRender();
        boolean b = isUseFnGn();
        pnLabel.setVisible(!hidePatientnameLabel && !b);
        pnField.setVisible(!b);
        fnLabel.setVisible(b);
        fnField.setVisible(b);
        gnLabel.setVisible(b);
        gnField.setVisible(b);
    }
    
	public FormComponent<String> setSubfieldsToRequired(boolean required) {
		if (isUseFnGn()) {
			fnField.setRequired(required);
		} else
			pnField.setRequired(required);
		return this;
    }
    
    public IModel<Boolean> getUseFnGn() {
        return useFnGn;
    }

    public void setUseFnGn(IModel<Boolean> useFnGn) {
        this.useFnGn = useFnGn;
    }
    
    public PatientNameField setHidePatientnameLabel(boolean hidePatientnameLabel) {
        this.hidePatientnameLabel = hidePatientnameLabel;
        return this;
    }
    public boolean isUseFnGn() {
        return useFnGn == null ? false : useFnGn.getObject().booleanValue();
    }

    public boolean isAutoWildcard() {
        return autoWildcard == null ? false : autoWildcard.getObject().booleanValue();
    }
    @Override
    public void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);
        if ( tag.getAttribute("class") == null )
            tag.put("class", "patientField");
    }
    
    @Override
    public String getInput() {
        return isUseFnGn() ? fnField.getInput() + "^" + gnField.getInput() : pnField.getInput();
    }
    
    @Override
    protected void convertInput() {
        String fn = fnField.getConvertedInput();
        String converted;
        int pos;
        if (!isUseFnGn()) {
            converted = pnField.getConvertedInput();
        } else if (fn != null && (pos = fn.indexOf('^')) != -1) { //if fn contains '^' ignore gn and set full name!
            if (isAutoWildcard() && pos == fn.lastIndexOf('^')) { //if fn contains only fn and gn -> add * to each.
                converted = fn.substring(0, pos)+"*"+fn.substring(pos)+"*";
            } else {
                converted = fn;
            }
        } else {
            String gn = gnField.getConvertedInput();
            converted = fn == null ? "" : fn;
            if (gn != null && gn.length() != 0) {
                if (isAutoWildcard() && !converted.endsWith("*"))
                    converted += "*";
                converted += "^"+gn;
                if (isAutoWildcard() && gn.indexOf('^') == -1 && !converted.endsWith("*"))
                    converted += "*";
            }
        }
        setConvertedInput(converted);
        log.debug("Converted Input:{}", getConvertedInput());
    }
    
    public Component addToDateField(final IBehavior... behaviors) {
        this.fnField.add(behaviors);
        return this;
    }
    public Component addToTimeField(final IBehavior... behaviors) {
        this.gnField.add(behaviors);
        return this;
    }

    private class FamilyNameModel implements IModel<String> {
        
        private static final long serialVersionUID = 1L;
        
        protected PatientNameField tf;

        public FamilyNameModel(PatientNameField tf) {
            this.tf = tf;
        }
        
        public String getObject() {
            String n = tf.getModelObject();
            if (n == null)
                return null;
            int pos = n.indexOf('^');
            if (pos == -1)
                return n;
            String fn = n.substring(0, pos);
            return n.substring(++pos).indexOf('^') != -1 ? fn : toViewString(fn);
        }
        
        protected String toViewString(String s) {
            return isAutoWildcard() && s != null && s.endsWith("*") ? s.substring(0,s.length()-1) : s;
        }

        public void setObject(String object) {
        }
        public void detach() {}
    }
    
    private class GivenNameModel extends FamilyNameModel {
        private static final long serialVersionUID = 1L;

        public GivenNameModel(PatientNameField tf) {
            super(tf);
        }
        
        public String getObject() {
            String n = tf.getModelObject();
            if (n == null)
                return null;
            int pos = n.indexOf('^');
            if (pos == -1)
                return null;
            String gn = n.substring(++pos);
            return gn.indexOf('^')!=-1 ? gn : toViewString(gn);
        }

    }
}
