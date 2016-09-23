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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.dcm4chee.web.war.common.model.AbstractDicomModel;

/**
 * @author Robert David <robert.david@agfa.com>
 */
public class SelectAllCheckBox extends AjaxCheckBox {
	
    private static final long serialVersionUID = 1L;
    
    private ArrayList<Component> updateComponents = new ArrayList<Component>();
    private List<? extends AbstractDicomModel> models;
    private int selectLevel;
    private String selectString;
    private boolean selectChilds;
    private HashMap<AbstractDicomModel, Boolean> modifiedModels;
    
    public SelectAllCheckBox(String id, List<? extends AbstractDicomModel> models, int selectLevel, 
    		Component updateComponent, boolean selectChilds) {

        super(id, new Model<Boolean>());
        this.models = models;
        this.selectLevel = selectLevel;

        this.selectString = selectLevel == 0 ? "Patients" : 
        	selectLevel == 1 ? "Studies" : 
        		selectLevel == 3 ? "Series" : null;
        
        add(new AttributeModifier("title", true, new ResourceModel("folder.studyview.selectAll" + selectString + ".tooltip")));

        this.addUpdateComponent(updateComponent);
        this.selectChilds = selectChilds;
    }
    
    public SelectAllCheckBox setModifiedModels(
            HashMap<AbstractDicomModel, Boolean> modifiedModels) {
        this.modifiedModels = modifiedModels;
        return this;
    }

    public SelectAllCheckBox addUpdateComponent(Component c) {
        c.setOutputMarkupId(true);
        updateComponents.add(c);
        return this;
    }
    
    public SelectAllCheckBox setSelectChilds(boolean b) {
        this.selectChilds = b;
        return this;
    }
    
    @Override
    public void onUpdate(AjaxRequestTarget target) {
        for (AbstractDicomModel m : models)
            setSelected(m);
        addToTarget(target);
        add(new AttributeModifier("title", true, new ResourceModel(getModelObject() ? 
        		"folder.studyview.deselectAll" + selectString + ".tooltip" : "folder.studyview.selectAll" + selectString + ".tooltip")));
    }

    private void setSelected(AbstractDicomModel m) {
        if (selectChilds) {
            if (getModelObject()) {
                if (m.levelOfModel() == selectLevel) {
                    internalSetSelected(m);
                    return;
                }
            } else {
                if (m.levelOfModel() >= selectLevel)
                    internalSetSelected(m);
            }
            List<? extends AbstractDicomModel> childs = m.getDicomModelsOfNextLevel();
            if (childs != null) {
                for (AbstractDicomModel m1 : m.getDicomModelsOfNextLevel()) {
                    setSelected(m1);
                }
            }
        } else if (m.levelOfModel() == selectLevel) {
            internalSetSelected(m);
        } else if (m.levelOfModel() < selectLevel) {
            if (m.isCollapsed())
                m.expand();
            for (AbstractDicomModel m1 : m.getDicomModelsOfNextLevel()) {
                setSelected(m1);
            }
        }
    }

    private void internalSetSelected(AbstractDicomModel m) {
        if (modifiedModels == null) {
            m.setSelected(getModelObject());
        } else {
            if (m.isSelected() != getModelObject()) {
                m.setSelected(getModelObject());
                if (modifiedModels.remove(m) == null)
                    modifiedModels.put(m, getModelObject());
            }
        }
    }

    private void addToTarget(AjaxRequestTarget target) {
        for (int i = 0, len = updateComponents.size() ; i < len ; i++) {
            target.addComponent(updateComponents.get(i));
        }
    }    
}
