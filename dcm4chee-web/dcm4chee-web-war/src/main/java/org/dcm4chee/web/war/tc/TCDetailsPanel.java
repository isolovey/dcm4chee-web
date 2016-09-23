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

import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.resource.loader.PackageStringResourceLoader;
import org.dcm4chee.web.war.config.delegate.WebCfgDelegate;

/**
 * @author Bernhard Ableitinger <bernhard.ableitinger@agfa.com>
 * @version $Revision$ $Date$
 * @since May 27, 2011
 */
@SuppressWarnings("serial")
public class TCDetailsPanel extends Panel {

	private AbstractDefaultAjaxBehavior tabActivationBehavior;
	
    private Map<TCDetailsTab, Integer> tabsToIndices =
            new HashMap<TCDetailsTab, Integer>();
    
    private int activeTabIndex = 0;

    private WebMarkupContainer errordetailsContainer;

    private WebMarkupContainer nodetailsContainer;

    private WebMarkupContainer detailsContainer;

	public TCDetailsPanel(final String id, final IModel<Boolean> trainingModeModel) {
        super(id, new Model<TCObject>());

        setOutputMarkupId(true);

        final Model<TCObject> tabModel = new Model<TCObject>() {
            @Override
            public TCObject getObject() {
                return (TCObject) TCDetailsPanel.this.getDefaultModelObject();
            }
        };        
        
        WebMarkupContainer tabsContainer = new WebMarkupContainer("details-tabs");
        tabsContainer.add(new Label("tc.details.tab.info.title", 
        		new ResourceModel("tc.details.tab.info.title.text")).setOutputMarkupId(true));
        tabsContainer.add(new Label("tc.details.tab.diagnosis.title", 
        		new ResourceModel("tc.details.tab.diagnosis.title.text")).setOutputMarkupId(true));
        tabsContainer.add(new Label("tc.details.tab.diffDiagnosis.title", 
        		new ResourceModel("tc.details.tab.differential-diagnosis.title.text")).setOutputMarkupId(true));
        tabsContainer.add(new Label("tc.details.tab.finding.title", 
        		new ResourceModel("tc.details.tab.finding.title.text")).setOutputMarkupId(true));
        tabsContainer.add(new Label("tc.details.tab.history.title", 
        		new ResourceModel("tc.details.tab.history.title.text")).setOutputMarkupId(true));
        tabsContainer.add(new Label("tc.details.tab.discussion.title", 
        		new ResourceModel("tc.details.tab.discussion.title.text")).setOutputMarkupId(true));
        tabsContainer.add(new Label("tc.details.tab.organsystem.title", 
        		new ResourceModel("tc.details.tab.organsystem.title.text")).setOutputMarkupId(true));
        tabsContainer.add(new Label("tc.details.tab.author.title", 
        		new ResourceModel("tc.details.tab.author.title.text")).setOutputMarkupId(true));
        tabsContainer.add(new Label("tc.details.tab.bibliography.title", 
        		new ResourceModel("tc.details.tab.bibliography.title.text")).setOutputMarkupId(true));
        tabsContainer.add(new Label("tc.details.tab.documents.title", 
        		new ResourceModel("tc.details.tab.documents.title.text")).setOutputMarkupId(true));
        tabsContainer.add(new Label("tc.details.tab.links.title", 
        		new ResourceModel("tc.details.tab.links.title.text")).setOutputMarkupId(true));
        tabsContainer.add(new Label("tc.details.tab.images.title", 
        		new Model<String>() {
            String title = new PackageStringResourceLoader()
                    .loadStringResource(TCDetailsPanel.class,
                            "tc.details.tab.images.title.text", null, null);
            @Override
            public String getObject() {
                TCObject tc = getTCObject();
                int nImages = tc != null ? tc.getReferencedImages().size()
                        : 0;
                StringBuffer sbuf = new StringBuffer(title);
                sbuf.append(" (");
                sbuf.append(nImages);
                sbuf.append(")");
                return sbuf.toString();
            }
        }).setOutputMarkupId(true));
        
        final TCAttributeVisibilityStrategy attrVisibilityStrategy =
        		new TCAttributeVisibilityStrategy(trainingModeModel);
        
        tabsContainer.add(addTab(new TCDetailsInfoTab("details-overview", attrVisibilityStrategy)).setDefaultModel(tabModel));
        tabsContainer.add(addTab(new TCDetailsDiagnosisTab("details-diagnosis", attrVisibilityStrategy)).setDefaultModel(tabModel));
        tabsContainer.add(addTab(new TCDetailsDefaultTab("details-diffDiagnosis", attrVisibilityStrategy) {
	            @Override
	            public TCAttribute getAttribute() {
	                return TCAttribute.DifferentialDiagnosis;
	            }
	        }).setDefaultModel(tabModel)
        );
        tabsContainer.add(addTab(new TCDetailsDefaultTab("details-finding", attrVisibilityStrategy) {
	            @Override
	            public TCAttribute getAttribute() {
	                return TCAttribute.Finding;
	            }
	        }).setDefaultModel(tabModel)
        );    
        tabsContainer.add(addTab(new TCDetailsDefaultTab("details-history", attrVisibilityStrategy) {
		            @Override
		            public TCAttribute getAttribute() {
		                return TCAttribute.History;
		            }
		        }).setDefaultModel(tabModel)
		);
		tabsContainer.add(addTab(new TCDetailsDefaultTab("details-discussion", attrVisibilityStrategy) {
		            @Override
		            public TCAttribute getAttribute() {
		                return TCAttribute.Discussion;
		            }
		            @Override
		            public boolean visible() {
		            	if (TCForumIntegration.get(WebCfgDelegate
		            			.getInstance().getTCForumIntegrationType())==null) {
		            		return false;
		            	}
		            	return super.visible();
		            }
		        }).setDefaultModel(tabModel)
		);
        tabsContainer.add(addTab(new TCDetailsDefaultTab("details-organSystem", attrVisibilityStrategy) {
	            @Override
	            public TCAttribute getAttribute() {
	                return TCAttribute.OrganSystem;
	            }
	        }).setDefaultModel(tabModel)
        );
        tabsContainer.add(addTab(new TCDetailsAuthorTab("details-author", attrVisibilityStrategy)).setDefaultModel(tabModel));
        tabsContainer.add(addTab(new TCDetailsBibliographyTab("details-bibliography", attrVisibilityStrategy)).setDefaultModel(tabModel));
        tabsContainer.add(addTab(new TCDetailsDocumentsTab("details-documents", attrVisibilityStrategy)).setDefaultModel(tabModel));
        tabsContainer.add(addTab(new TCDetailsLinksTab("details-links", attrVisibilityStrategy)).setDefaultModel(tabModel));
        tabsContainer.add(addTab(new TCDetailsImagesTab("details-images", attrVisibilityStrategy)).setDefaultModel(tabModel));

        nodetailsContainer = new WebMarkupContainer("no-details-panel");
        nodetailsContainer.setOutputMarkupId(true);

        errordetailsContainer = new WebMarkupContainer("error-details-panel");
        errordetailsContainer.setOutputMarkupId(true);

        detailsContainer = new WebMarkupContainer("details-info-panel");
        detailsContainer.setOutputMarkupId(true);
        detailsContainer.add(tabsContainer);

        nodetailsContainer.setVisible(true);
        errordetailsContainer.setVisible(false);
        detailsContainer.setVisible(false);

        add(nodetailsContainer);
        add(errordetailsContainer);
        add(detailsContainer);
        
        add(tabActivationBehavior = new AbstractDefaultAjaxBehavior() {
        	public void respond(AjaxRequestTarget target) {
        		try
        		{
        			String tabIndex = RequestCycle.get().getRequest().getParameter("tabIndex");
        			if (tabIndex!=null && !tabIndex.isEmpty())
        			{
        				activeTabIndex = Integer.valueOf(tabIndex);
        			}
        		}
        		catch (Exception e)
        		{
        			e.printStackTrace();
        		}
        	}
        });
        
        add(new HeaderContributor(new IHeaderContributor()
		{
			public void renderHead(IHeaderResponse response)
			{
				StringBuilder js = new StringBuilder();
		        js.append(getInitUIJavascript());
		        js.append(getDisableTabsJavascript());
		        js.append(getHideTabsJavascript());
		        
		        response.renderOnDomReadyJavascript(js.toString());
			}
		}));
    }

    public void setTCObject(TCObject tc, AjaxRequestTarget target) {
        nodetailsContainer.setVisible(tc == null);
        errordetailsContainer.setVisible(false);
        detailsContainer.setVisible(tc != null);
        activeTabIndex = 0;

        setDefaultModel(new Model<TCObject>(tc));
    }

    public TCObject getTCObject() {
        return (TCObject) getDefaultModelObject();
    }

    public void clearTCObject(boolean error) {
        nodetailsContainer.setVisible(!error);
        errordetailsContainer.setVisible(error);
        detailsContainer.setVisible(false);

        setDefaultModelObject(null);
    }
    
    private TCDetailsTab addTab(TCDetailsTab tab)
    {
    	tabsToIndices.put(tab, tabsToIndices.size());
    	return tab;
    }
        
    private String getInitUIJavascript() {
    	StringBuilder js = new StringBuilder();
    	
    	// make tabs
    	js.append("$('.details-tabs').tabs({" +
    			"active:" + activeTabIndex +"," +
    			"heightStyle:'fill'," +
    			"activate: function(event, ui) {" +
    			"   var url = '" + tabActivationBehavior.getCallbackUrl() + "';" +
    			"   url += (url.indexOf('?')==-1) ? '?tabIndex' : '&tabIndex=';" +
    			"   url += ui.newTab.index();" +
    			"   wicketAjaxGet(url, function(){}, function(){});" +
    			"}" +
    			"});");
    	
    	// move the nav to the bottom
    	js.append("$('.details-tabs .ui-tabs-nav, .details-tabs .ui-tabs-nav > *')");
    	js.append(".removeClass('ui-corner-all ui-corner-top')");
    	js.append(".addClass('ui-corner-bottom');");
    	js.append("$('.details-tabs .ui-tabs-nav').appendTo('.details-tabs');");
    	
    	// show tabs
    	js.append("$('.details-tabs').show();");
    	
    	return js.toString();
    }    
    
    private String getDisableTabsJavascript() {
    	boolean appendDelimiter=false;
    	StringBuffer sbuf = new StringBuffer();
    	sbuf.append("setDisabledTCDetailsTabs([");
        for (Map.Entry<TCDetailsTab, Integer> me : tabsToIndices.entrySet())
        {
            if (!me.getKey().enabled())
            {
            	if (appendDelimiter) {
            		sbuf.append(",");
            	}
            	appendDelimiter = true;
            	sbuf.append(me.getValue());
            }
        }
        sbuf.append("]);");
        return sbuf.toString();
    }
    
    private String getHideTabsJavascript() {
    	boolean appendDelimiter=false;
    	StringBuffer sbuf = new StringBuffer();
    	sbuf.append("setHiddenTCDetailsTabs([");
        for (Map.Entry<TCDetailsTab, Integer> me : tabsToIndices.entrySet())
        {
            if (!me.getKey().visible())
            {
            	if (appendDelimiter) {
            		sbuf.append(",");
            	}
            	appendDelimiter = true;
            	sbuf.append(me.getValue());
            }
        }
        sbuf.append("]);");
        return sbuf.toString();
    }
    
}
