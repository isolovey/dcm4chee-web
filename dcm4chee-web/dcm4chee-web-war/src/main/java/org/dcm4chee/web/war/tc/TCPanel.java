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

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.dcm4chee.icons.ImageManager;
import org.dcm4chee.icons.behaviours.ImageSizeBehaviour;
import org.dcm4chee.web.common.ajax.MaskingAjaxCallBehavior;
import org.dcm4chee.web.common.behaviours.TooltipBehaviour;
import org.dcm4chee.web.dao.tc.TCQueryFilter;
import org.dcm4chee.web.war.tc.TCEditableObject.SaveResult;
import org.dcm4chee.web.war.tc.TCPopupManager.ITCPopupManagerProvider;
import org.dcm4chee.web.war.tc.TCResultPanel.ITCCaseProvider;
import org.dcm4chee.web.war.tc.TCResultPanel.TCListModel;
import org.dcm4chee.web.war.tc.TCViewDialog.ITCViewDialogCloseCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Bernhard Ableitinger <bernhard.ableitinger@agfa.com>
 * @version $Revision$ $Date$
 * @since April 28, 2011
 */
public class TCPanel extends Panel implements ITCPopupManagerProvider {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(TCPanel.class);

    
    public static final String ModuleName = "tc";

    private static final MaskingAjaxCallBehavior macb = new MaskingAjaxCallBehavior();

    private IModel<Boolean> trainingModeModel;
    private TCPopupManager popupManager;
    private TCSearchPanel searchPanel;
    private TCResultPanel resultPanel;
    private TCViewDialog viewDialog;
    
    @SuppressWarnings("serial")
	public TCPanel(final String id) {
        super(id);
        
        setOutputMarkupId(true);
        
        add(new AttributeAppender("class",true,new Model<String>("ui-page")," "));
        
        add(TCEnvironment.getCSSHeaderContributor());
        add(new HeaderContributor(new IHeaderContributor()
		{
        	@Override
			public void renderHead(IHeaderResponse response)
			{
				response.renderOnDomReadyJavascript("initUI($('#" + getMarkupId(true) + "'));");
			}
		}));

        trainingModeModel = new Model<Boolean>(false);
        
        viewDialog = new TCViewDialog("tc-view-dialog");
        
        final TCDetailsPanel detailsPanel = new TCDetailsPanel("details-panel",
        		trainingModeModel);
        final TCListModel listModel = new TCListModel();
        resultPanel = new TCResultPanel("result-panel", listModel, trainingModeModel) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void selectionChanged(TCModel tc, AjaxRequestTarget target) {
                try {
                    if (tc == null) {
                        detailsPanel.clearTCObject(false);
                    } else {
                        detailsPanel.setTCObject(TCObject.create(tc), target);
                    }
                } catch (Exception e) {
                    log.error("Parsing TC object failed!", e);
                    detailsPanel.clearTCObject(true);
                }

                if (target!=null)
                {
                	target.addComponent(detailsPanel);
                }
            }
            
            @Override
            protected void openTC(final TCModel tcModel, final boolean edit, AjaxRequestTarget target)
            {
            	viewDialog.open(null, target, tcModel, trainingModeModel, resultPanel.getCaseProvider(), edit, 
            		new ITCViewDialogCloseCallback() {
            			@Override
            			public void dialogClosed(AjaxRequestTarget target, TCEditableObject tc, SaveResult result) {
            				if (result!=null && result.saved()) {
					            // trigger new search and select new SR
            					// need this in order to immediately 'see' the changes
            					TCModel newTC = listModel.updateByIUID(result.getCaseUID());

            					// in fact, we also need to update linked cases as well
            					List<String> otherUIDs = result.getOtherCaseUIDs();
            					if (otherUIDs!=null) {
            						for (String uid : otherUIDs)
            						{
            							listModel.updateByIUID(uid);
            						}
            					}
            					
            	                resultPanel.clearSelected();
            					resultPanel.selectTC(newTC, null);
            					target.addComponent(resultPanel);
            					target.addComponent(detailsPanel);
            				}
            			}
            		}
            	);

            }
        };

        add(macb);
        add((searchPanel=new TCSearchPanel("search-panel") {

            private static final long serialVersionUID = 1L;

            @Override
            public Component[] doSearch(TCQueryFilter filter) {
                detailsPanel.clearTCObject(false);
                resultPanel.clearSelected();
                listModel.update(filter);

                return new Component[] { resultPanel, detailsPanel };
            }
            
            @Override
            public void redoSearch(AjaxRequestTarget target, String iuid)
            {
                Component[] toUpdate = doSearch((TCQueryFilter)getDefaultModel().getObject());

                TCModel tc = iuid!=null ? listModel.findByIUID(iuid):null;
                if (tc!=null)
                {
                    resultPanel.selectTC(tc, null);
                }
                
                if (toUpdate != null && target != null) {
                    for (Component c : toUpdate) {
                        target.addComponent(c);
                    }
                }
            }
        }).setOutputMarkupId(true));
        
        add(new AjaxLink<Object>("trainingmode-link") {
	        	@Override
	        	public void onClick(AjaxRequestTarget target) {
	        		trainingModeModel.setObject(
	        				!trainingModeModel.getObject());
	        		target.addComponent(this);
	        		target.addComponent(detailsPanel);
	        		
	        		if (TCAttribute.Title.isRestricted() ||
	        			TCAttribute.Abstract.isRestricted() ||
	        			TCAttribute.AuthorName.isRestricted()) {
	        			target.addComponent(resultPanel);
	        		}
	        	}
        	}
        	.add(new Image("trainingmode-link-img",
                        new AbstractReadOnlyModel<ResourceReference>() {
                            @Override
                            public ResourceReference getObject() {
                                return trainingModeModel.getObject()==Boolean.TRUE ? 
                                		ImageManager.IMAGE_TC_BUTTON_ON
                                        : ImageManager.IMAGE_TC_BUTTON_OFF;
                            }
            }).add(new ImageSizeBehaviour(32,32,"vertical-align:middle")))
            .add(new Label("trainingmode-link-text", new AbstractReadOnlyModel<String>() {
	        	public String getObject() {
	        		if (trainingModeModel.getObject()==Boolean.TRUE) {
	        			return TCPanel.this.getString("tc.trainingmode.enabled.text");
	        		}
	        		else {
	        			return TCPanel.this.getString("tc.trainingmode.disabled.text");
	        		}
	        	}
            }).add(new AttributeAppender("style",true,new Model<String>("vertical-align:middle")," ")))
            .add(new TooltipBehaviour("tc.","trainingmode"))
            .setOutputMarkupId(true).setMarkupId("trainingmode-link")
        );

        add(resultPanel);
        add(detailsPanel);
        add(new Form<Void>("tc-view-dialog-outer-form").add(viewDialog));
        
        add((popupManager=new TCPopupManager()).getGlobalHideOnOutsideClickHandler());
    }
    
    public TCViewDialog getViewDialog() {
    	return viewDialog;
    }
    
    public ITCCaseProvider getResultCaseProvider() {
    	return resultPanel.getCaseProvider();
    }
    
    @Override
    public TCPopupManager getPopupManager()
    {
        return popupManager;
    }

    public static String getModuleName() {
        return ModuleName;
    }

    public static MaskingAjaxCallBehavior getMaskingBehaviour() {
        return macb;
    }
}
