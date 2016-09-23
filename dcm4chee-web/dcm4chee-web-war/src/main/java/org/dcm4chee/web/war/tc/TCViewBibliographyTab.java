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

import java.text.MessageFormat;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.dcm4chee.icons.ImageManager;
import org.dcm4chee.icons.behaviours.ImageSizeBehaviour;
import org.dcm4chee.web.common.behaviours.TooltipBehaviour;
import org.dcm4chee.web.war.tc.TCUtilities.SelfUpdatingTextArea;
import org.dcm4chee.web.war.tc.TCViewPanel.AbstractEditableTCViewTab;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Bernhard Ableitinger <bernhard.ableitinger@agfa.com>
 * @version $Revision$ $Date$
 * @since Nov 25, 2011
 */
public class TCViewBibliographyTab extends AbstractEditableTCViewTab 
{
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(TCViewBibliographyTab.class);
    
    private Label title;
    private AbstractLink addBtn;
    private WebMarkupContainer listContainer;
    private ListView<String> list;
    private Component tabTitleComponent;

    public TCViewBibliographyTab(final String id, IModel<TCEditableObject> model, 
    		TCAttributeVisibilityStrategy attrVisibilityStrategy) {
        super(id, model, attrVisibilityStrategy);

        list = new ListView<String>("tc-view-bibliography-list",new ListModelWrapper()) 
        {
            private static final long serialVersionUID = 1L;
            @Override
            protected void populateItem(final ListItem<String> item) {  
                //textarea
                final TextArea<String> area = new SelfUpdatingTextArea("tc-view-bibliography-text", item.getModelObject()) {
                    @Override
                    protected void textUpdated(String text)
                    {
                        ((ListModelWrapper)list.getModel()).setReference(item.getIndex(), text);
                    }
                };
                area.setOutputMarkupId(true);
                area.setEnabled(isEditing());
                area.add(createTextInputCssClassModifier());
                
                //remove button
                final AbstractLink removeBtn = new AjaxLink<Void>("tc-view-bibliography-remove-btn") {
                    @Override
                    public void onClick(AjaxRequestTarget target)
                    {
                        try
                        {
                            ((ListModelWrapper)list.getModel()).removeReference(item.getIndex());
                            
                            target.addComponent(title);
                            target.addComponent(listContainer);                            
                            target.appendJavascript("updateTCViewDialog();");
                        
                            tabTitleChanged(target);
                            
                            if (tabTitleComponent!=null)
                            {
                                target.addComponent(tabTitleComponent);
                            }
                        }
                        catch (Exception e)
                        {
                            log.error("Removing bibliographic reference from teaching-file failed!", e);
                        }
                    }
                };
                removeBtn.add(new Image("tc-view-bibliography-remove-img", ImageManager.IMAGE_TC_CANCEL_MONO)
                    .add(new ImageSizeBehaviour("vertical-align: middle;")));
                removeBtn.add(new TooltipBehaviour("tc.view.bibliography","remove"));
                removeBtn.setOutputMarkupId(true);
                removeBtn.setVisible(isEditing());
                removeBtn.add(new AttributeModifier("onmouseover",true,new Model<String>(
                        "$(this).children('img').attr('src','" + RequestCycle.get().urlFor(
                                ImageManager.IMAGE_TC_CANCEL)+ "');" 
                )));
                removeBtn.add(new AttributeModifier("onmouseout",true,new Model<String>(
                        "$(this).children('img').attr('src','" + RequestCycle.get().urlFor(
                                ImageManager.IMAGE_TC_CANCEL_MONO)+ "');" 
                )));
                
                item.setOutputMarkupId(true);
                item.add(area);
                item.add(removeBtn);
            }
        };

        listContainer = new WebMarkupContainer("tc-view-bibliography-container");
        listContainer.setOutputMarkupId(true);
        listContainer.setMarkupId("tc-view-bibliography-container");
        listContainer.add(list);
        
        title = new Label("tc-view-bibliography-title-text", new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject()
            {
                String s = title.getString("tc.view.bibliography.number.text");
                List<String> refs = list.getModelObject();
                return MessageFormat.format(s, refs!=null ? refs.size() : 0);
            }
        });
        title.setOutputMarkupId(true);
        
        addBtn = new AjaxLink<Void>("tc-view-bibliography-add-btn") {
            @Override
            public void onClick(AjaxRequestTarget target)
            {
                try
                {
                    ((ListModelWrapper)list.getModel()).addReference(addBtn.getString("tc.view.bibliography.reference.defaulttext"));
    
                    target.addComponent(title);
                    target.addComponent(listContainer);                    
                    target.appendJavascript("updateTCViewDialog();");
                    
                    tabTitleChanged(target);
                    
                    if (tabTitleComponent!=null)
                    {
                        target.addComponent(tabTitleComponent);
                    }
                }
                catch (Exception e)
                {
                    log.error("Adding new bibliographic reference to teachign-file failed!", e);
                }
            }
        };
        addBtn.add(new Image("tc-view-bibliography-add-img", ImageManager.IMAGE_COMMON_ADD)
            .add(new ImageSizeBehaviour("vertical-align: middle;")));
        addBtn.add(new Label("tc-view-bibliography-add-text", new ResourceModel("tc.view.bibliography.add.text")));
        addBtn.add(new TooltipBehaviour("tc.view.bibliography","add"));
        addBtn.setMarkupId("tc-view-bibliography-add-btn");

        title.setEnabled(isEditing());
        addBtn.setVisible(isEditing());
        listContainer.setEnabled(isEditing());
        
        add(title);
        add(addBtn);
        add(listContainer);
    }
    
    
    public void setTabTitleComponent(Component c)
    {
        tabTitleComponent = c;
    }
    
    @Override
    public String getTabTitle()
    {
        return MessageFormat.format(getString("tc.view.bibliography.tab.title"),
                getTC().getBibliographicReferences().size());
    }
    
    @Override
    public boolean isTabVisible() {
    	if (super.isTabVisible()) {
	    	return getAttributeVisibilityStrategy()
	    			.isAttributeVisible(TCAttribute.BibliographicReference);
    	}
    	
    	return false;
    }
    
    @Override
    public boolean hasContent()
    {
        List<String> refs = getTC()!=null ? getTC().getBibliographicReferences() : null;
        return refs!=null && !refs.isEmpty();
    }
    
    @Override
    protected void saveImpl()
    {
        getTC().setBibliographicReferences(list.getModelObject());
    }
    
    private class ListModelWrapper extends ListModel<String>
    {        
        @Override
        public List<String> getObject()
        {
            return getTC().getBibliographicReferences();
        }
        
        @Override
        public void setObject(List<String> list)
        {
            getTC().setBibliographicReferences(list);
        }
        
        public String getReference(int index)
        {
            return getTC().getBibliographicReferences().get(index);
        }
        
        public void setReference(int index, String s)
        {
            try
            {
                getTC().setBibliographicReference(index, s);
            }
            catch (Exception e)
            {
                log.error("Updating bibliographic reference in teaching-file failed!", e);
            }
        }
        
        public void addReference(String s)
        {
            getTC().addBibliographicReference(s);
        }
        
        public void removeReference(int index) throws IndexOutOfBoundsException
        {
            getTC().removeBibliographicReference(index);
        }
    }
}
