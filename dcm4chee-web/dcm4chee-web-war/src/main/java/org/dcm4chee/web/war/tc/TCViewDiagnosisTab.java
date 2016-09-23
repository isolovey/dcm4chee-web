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
package org.dcm4chee.web.war.tc;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.dcm4chee.web.dao.tc.TCQueryFilterKey;
import org.dcm4chee.web.dao.tc.TCQueryFilterValue.YesNo;
import org.dcm4chee.web.war.tc.TCObject.TextOrCode;
import org.dcm4chee.web.war.tc.TCUtilities.SelfUpdatingCheckBox;
import org.dcm4chee.web.war.tc.TCUtilities.SelfUpdatingTextArea;
import org.dcm4chee.web.war.tc.TCViewPanel.AbstractEditableTCViewTab;

/**
 * @author Bernhard Ableitinger <bernhard.ableitinger@agfa.com>
 * @version $Revision$ $Date$
 * @since Nov 25, 2011
 */
public class TCViewDiagnosisTab extends AbstractEditableTCViewTab 
{
    private static final long serialVersionUID = 1L;

    private SelfUpdatingCheckBox chkBox;
    private SelfUpdatingTextArea area;
        
    public TCViewDiagnosisTab(final String id, IModel<TCEditableObject> model, 
    		TCAttributeVisibilityStrategy attrVisibilityStrategy) {
        super(id, model, attrVisibilityStrategy);

        YesNo yesno = getTC().getDiagnosisConfirmed();
        this.chkBox = new SelfUpdatingCheckBox("tc-view-diagnosis-confirmed-chkbox", 
                yesno!=null && yesno.equals(YesNo.Yes)) {
            @Override
            protected void selectionChanged(boolean selected)
            {
                getTC().setDiagnosisConfirmed(selected?YesNo.Yes:YesNo.No);
            }
        };
        this.area=new SelfUpdatingTextArea("tc-view-diagnosis-text", getStringValue(TCQueryFilterKey.Diagnosis)) {
            @Override
            protected void textUpdated(String text)
            {
                if (isEditing())
                {
                    getTC().setDiagnosis(TextOrCode.text(text));
                }
            }
        };
        this.area.setMarkupId("tc-view-diagnosis-text");
        this.area.add(createTextInputCssClassModifier());
        
        if (!isEditing()) {
            this.area.add(new AttributeAppender("readonly",true,new Model<String>("readonly"), " "));
        }
        
        chkBox.setEnabled(isEditing());
        area.setEnabled(isEditing());
        
        add(this.chkBox);
        add(this.area);
    }

    @Override
    public String getTabTitle()
    {
        return getString("tc.view.diagnosis.tab.title");
    }
    
    @Override
    public boolean hasContent()
    {
        return !getStringValue(TCQueryFilterKey.Diagnosis).isEmpty() ||
            getTC().getDiagnosisConfirmed()!=null;
    }
    
    @Override
    public boolean isTabVisible() {
    	if (super.isTabVisible()) {
	    	return getAttributeVisibilityStrategy()
	    			.isAttributeVisible(TCAttribute.Diagnosis);
    	}
    	
    	return false;
    }
    
    @Override
    protected void saveImpl()
    {
        getTC().setDiagnosisConfirmed(chkBox.getModelObject()?YesNo.Yes:YesNo.No);
        getTC().setDiagnosis(TextOrCode.text(area.getModel().getObject()));
    }
}
