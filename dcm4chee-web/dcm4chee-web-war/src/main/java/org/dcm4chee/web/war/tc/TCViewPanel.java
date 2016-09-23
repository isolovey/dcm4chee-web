package org.dcm4chee.web.war.tc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.dcm4chee.icons.ImageManager;
import org.dcm4chee.icons.behaviours.ImageSizeBehaviour;
import org.dcm4chee.web.common.behaviours.TooltipBehaviour;
import org.dcm4chee.web.common.secure.SecurityBehavior;
import org.dcm4chee.web.common.webview.link.WebviewerLinkProvider;
import org.dcm4chee.web.dao.tc.TCQueryFilterKey;
import org.dcm4chee.web.war.StudyPermissionHelper;
import org.dcm4chee.web.war.config.delegate.WebCfgDelegate;
import org.dcm4chee.web.war.folder.webviewer.Webviewer;
import org.dcm4chee.web.war.folder.webviewer.Webviewer.WebviewerLinkClickedCallback;
import org.dcm4chee.web.war.tc.TCResultPanel.ITCCaseProvider;
import org.dcm4chee.web.war.tc.keywords.TCKeywordCatalogueProvider;
import org.dcm4chee.web.war.tc.widgets.TCMaskingAjaxDecorator;

/**
 * @author Bernhard Ableitinger <bernhard.ableitinger@agfa.com>
 * @version $Revision$ $Date$
 * @since Nov 25, 2011
 */
@SuppressWarnings("serial")
public class TCViewPanel extends Panel
{
	private static final Logger log = Logger.getLogger(TCViewPanel.class);
	
	private ModalWindow webviewerSelectionWindow;
    private WebviewerLinkProvider[] webviewerLinkProviders;
    
    private Map<AbstractTCViewTab, Integer> tabsToIndices =
        new HashMap<AbstractTCViewTab, Integer>();
    
    private AbstractAjaxBehavior tabActivationBehavior;

    private TCModel tcModel;
    
    private TCAttributeVisibilityStrategy attrVisibilityStrategy;
    
    private boolean sendImagesViewedLog = true;

    
	public TCViewPanel(final String id, TCModel tc,
			final TCAttributeVisibilityStrategy attrVisibilityStrategy,
			final ITCCaseProvider caseProvider)
    {
        super(id, new Model<TCEditableObject>());

        this.attrVisibilityStrategy = attrVisibilityStrategy;
        
        if (tc!=null) {
        	try {
	        	tcModel = tc;
	        	setDefaultModelObject(TCEditableObject.create(tc));
        	}
        	catch (Exception e) {
        		log.error("Unable to create/parse teaching-file!", e);
        	}
        }
        
        initWebviewerLinkProvider();

        add(webviewerSelectionWindow = new ModalWindow("tc-view-webviewer-selection-window"));
        add(createWebviewerLink(tcModel));
                
        add(new AjaxLink<Void>("tc-print-btn") {
                @Override
                public void onClick(AjaxRequestTarget target) {
                    
                }
            }
            .add(new Image("tc-print-img", ImageManager.IMAGE_TC_PRINT).add(
                (new ImageSizeBehaviour("vertical-align: middle;"))))
            .setOutputMarkupId(true)
            .setVisible(false) //for now
        );
        
        WebMarkupContainer caseNavigator = new WebMarkupContainer("tc-view-case-navigator") {
        	@Override
        	public boolean isVisible() {
        		return !isEditable() && caseProvider!=null;
        	}
        };
        caseNavigator.setMarkupId("tc-view-case-navigator");
        caseNavigator.setOutputMarkupId(true);
        caseNavigator.add(new Label("tc-view-casenumber", new StringResourceModel(
                "tc.view.casenumber.text", this, null,
                new Object[] { new Model<String>() {
                    @Override
                    public String getObject() {
                    	int index = caseProvider.getIndexOfCase(tcModel);
                    	return index>=0 ? Integer.toString(index+1) : "-";
                    }
                },
                new Model<Integer>() {
                    @Override
                    public Integer getObject() {
                        return caseProvider.getCaseCount();
                    }
                }})
        	){
        		@Override
        		public boolean isEnabled() {
        			return caseProvider!=null && 
        					caseProvider.getIndexOfCase(tcModel)>=0;
        		}
        	}.setOutputMarkupId(true)
        );
        caseNavigator.add(new AjaxLink<Void>("tc-view-prev") {
	            @Override
	            public void onClick(AjaxRequestTarget target) {
	            	TCModel prev = caseProvider.getPrevCase(tcModel);
	            	if (prev!=null) {
	            		tcModel = prev;
	            		setCase(target, prev);
	            	}
	            }
	            @Override
	            public boolean isEnabled() {
	            	return caseProvider.getPrevCase(tcModel)!=null;
	            }
	            @Override
	        	protected IAjaxCallDecorator getAjaxCallDecorator()
	        	{
	        		return new TCMaskingAjaxDecorator(false, true);
	        	}
	        }
	        .add(new Image("tc-view-prev-img", ImageManager.IMAGE_TC_ARROW_PREV).add(
	            (new ImageSizeBehaviour("vertical-align: middle;"))))
	        .add(new TooltipBehaviour("tc.view.case.","prev"))
	        .setOutputMarkupId(true)
        );
        caseNavigator.add(new AjaxLink<Void>("tc-view-next") {
	            @Override
	            public void onClick(AjaxRequestTarget target) {
	            	TCModel next = caseProvider.getNextCase(tcModel);
	            	if (next!=null) {
	            		tcModel = next;
	            		setCase(target, next);
	            	}
	            }
	            @Override
	            public boolean isEnabled() {
	            	return caseProvider.getNextCase(tcModel)!=null;
	            }
	            @Override
	        	protected IAjaxCallDecorator getAjaxCallDecorator()
	        	{
	        		return new TCMaskingAjaxDecorator(false, true);
	        	}
	        }
	        .add(new Image("tc-view-next-img", ImageManager.IMAGE_TC_ARROW_NEXT).add(
	            (new ImageSizeBehaviour("vertical-align: middle;"))))
	        .add(new TooltipBehaviour("tc.view.case.","next"))
	        .setOutputMarkupId(true)
	    );
        caseNavigator.add(new AjaxLink<Void>("tc-view-next-random") {
	            @Override
	            public void onClick(AjaxRequestTarget target) {
	            	TCModel next = caseProvider.getNextRandomCase(tcModel);
	            	if (next!=null) {
	            		tcModel = next;
	            		setCase(target, next);
	            	}
	            }
	            @Override
	            public boolean isEnabled() {
	            	return caseProvider.getCaseCount()>1;
	            }
	            @Override
	        	protected IAjaxCallDecorator getAjaxCallDecorator()
	        	{
	        		return new TCMaskingAjaxDecorator(false, true);
	        	}
	        }
	        .add(new Image("tc-view-next-random-img", ImageManager.IMAGE_TC_ARROW_NEXT_RANDOM).add(
	            (new ImageSizeBehaviour(24,16,"vertical-align: middle"))))
	        .add(new TooltipBehaviour("tc.view.case.","nextrandom"))
	        .setOutputMarkupId(true)
	        .setVisible(!isEditable() && attrVisibilityStrategy.isTrainingModeOn())
	    );
            
        add(caseNavigator);
        final Component titleText = new Label("tc-view-title-text", new AbstractReadOnlyModel<String>() {
	            @Override
	            public String getObject()
	            {
	        		if (!attrVisibilityStrategy.isAttributeVisible(TCAttribute.Title)) {
	        			return TCUtilities.getLocalizedString("tc.case.text")+" " + getTC().getId();
	        		}
	        		return getTC().getTitle();
	            }
	        })
	        .add(new AttributeAppender("style",new AbstractReadOnlyModel<String>() {
	        	@Override
	        	public String getObject() {
	        		if (isEditable()) {
	        			return "display:inline-block";
	        		}
	        		return "";
	        	}
	        }, ";"))
	        .setMarkupId("tc-view-title-text")
	        .setOutputMarkupId(true);
        add(titleText);
                
        final Label biblioTitleLabel = new Label("tc.view.bibliography.tab.title");
        biblioTitleLabel.setOutputMarkupId(true);
        
        final Label documentsTitleLabel = new Label("tc.view.documents.tab.title");
        documentsTitleLabel.setOutputMarkupId(true);
        
        final Label linksTitleLabel = new Label("tc.view.links.tab.title");
        linksTitleLabel.setOutputMarkupId(true);
        
        final TCViewForumTab forumTab = new TCViewForumTab("tc-view-forum", getModel(), attrVisibilityStrategy);
        final TCViewOverviewTab overviewTab = new TCViewOverviewTab("tc-view-overview", getModel(), attrVisibilityStrategy);
        final TCViewDiagnosisTab diagnosisTab = new TCViewDiagnosisTab("tc-view-diagnosis", getModel(), attrVisibilityStrategy);
        final WebMarkupContainer imagesTab =  new TCViewImagesTab("tc-view-images", getModel(), attrVisibilityStrategy);
        
        final TCViewGenericTextTab diffDiagnosisTab = new TCViewGenericTextTab("tc-view-diffDiagnosis", getModel(), 
        		attrVisibilityStrategy) {
            @Override
            public String getTabTitle()
            {
                return getString("tc.view.diffDiagnosis.tab.title");
            }
            @Override
            protected TCAttribute getAttribute() {
                return TCAttribute.DifferentialDiagnosis;
            }
        };
        final TCViewGenericTextTab findingTab = new TCViewGenericTextTab("tc-view-finding", getModel(), 
        		attrVisibilityStrategy) {
            @Override
            public String getTabTitle()
            {
                return getString("tc.view.finding.tab.title");
            }
            @Override
            public boolean isTabVisible()
            {
            	TCQueryFilterKey queryKey = getAttribute().getQueryKey();
            	if (queryKey!=null && !TCKeywordCatalogueProvider.getInstance()
            			.hasCatalogue(queryKey))
            	{
            		return super.isTabVisible();
            	}
            	return false;
            }
            @Override
            protected TCAttribute getAttribute() {
                return TCAttribute.Finding;
            }
        };
        final TCViewGenericTextTab historyTab = new TCViewGenericTextTab("tc-view-history", getModel(), 
        		attrVisibilityStrategy) {
            @Override
            public String getTabTitle()
            {
                return getString("tc.view.history.tab.title");
            }
            @Override
            protected TCAttribute getAttribute() {
                return TCAttribute.History;
            }
        };
        final TCViewGenericTextTab discussionTab = new TCViewGenericTextTab("tc-view-discussion", getModel(), 
        		attrVisibilityStrategy) {
            @Override
            public String getTabTitle()
            {
                return getString("tc.view.discussion.tab.title");
            }
            @Override
            protected TCAttribute getAttribute() {
                return TCAttribute.Remarks;
            }
        };
        final TCViewGenericTextTab organSystemTab = new TCViewGenericTextTab("tc-view-organSystem", getModel(), 
        		attrVisibilityStrategy) {
            @Override
            public String getTabTitle()
            {
                return getString("tc.view.organSystem.tab.title");
            }
            @Override
            protected TCAttribute getAttribute() {
                return TCAttribute.OrganSystem;
            }
        };
               
        final TCViewBibliographyTab biblioTab = new TCViewBibliographyTab("tc-view-bibliography", getModel(), 
        		attrVisibilityStrategy) {
        	@Override
            protected void tabTitleChanged(AjaxRequestTarget target)
            {
                if (target!=null)
                {
                    target.addComponent(biblioTitleLabel);
                }
            }
        };
        
        biblioTitleLabel.setDefaultModel(new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject()
            {
                return biblioTab.getTabTitle();
            }
        });
        
        final TCViewDocumentsTab documentsTab = new TCViewDocumentsTab("tc-view-documents", getModel(), 
        		attrVisibilityStrategy) {
        	@Override
            protected void tabTitleChanged(AjaxRequestTarget target)
            {
                if (target!=null)
                {
                    target.addComponent(documentsTitleLabel);
                }
            }
        };
        
        documentsTitleLabel.setDefaultModel(new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject()
            {
                return documentsTab.getTabTitle();
            }
        });
        
        final TCViewLinksTab linksTab = new TCViewLinksTab("tc-view-links", getModel(), 
        		attrVisibilityStrategy) {
        	@Override
            protected void tabTitleChanged(AjaxRequestTarget target)
            {
                if (target!=null)
                {
                    target.addComponent(linksTitleLabel);
                }
            }
        };
        
        linksTitleLabel.setDefaultModel(new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject()
            {
                return linksTab.getTabTitle();
            }
        });
        
        tabActivationBehavior = new AbstractDefaultAjaxBehavior() {
        	public void respond(AjaxRequestTarget target) {
        		String newTabId = RequestCycle.get().getRequest().getParameter("newTabId");
        		String oldTabId = RequestCycle.get().getRequest().getParameter("oldTabId");
        		
        		AbstractTCViewTab newTab = newTabId!=null ? getTabByMarkupId(newTabId) : null;
        		AbstractTCViewTab oldTab = oldTabId!=null ? getTabByMarkupId(oldTabId) : null;
        		
        		tabSelectionChanged(target, newTab, oldTab);
        	}
        };
        
        final WebMarkupContainer content = new WebMarkupContainer("tc-view-content") {
            @Override
            protected void onComponentTag(ComponentTag tag)
            {
                super.onComponentTag(tag);
                tag.put("activation-callback-url", tabActivationBehavior.getCallbackUrl());
            }
        };

        content.setOutputMarkupId(true);
        content.setMarkupId(isEditable() ? 
                "tc-view-editable-content" : "tc-view-content");
        content.add(new TCUtilities.TCClassAppender( isEditable() ?
       			"tc-view-editable-content" : "tc-view-content"));
        
        content.add(new Label("tc.view.overview.tab.title", new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject()
            {
                return overviewTab.getTabTitle();
            }
        }));
        
        content.add(new Label("tc.view.diagnosis.tab.title", new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject()
            {
                return diagnosisTab.getTabTitle();
            }
        }));
        
        content.add(new Label("tc.view.diffDiagnosis.tab.title", new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject()
            {
                return diffDiagnosisTab.getTabTitle();
            }
        }));
        
        content.add(new Label("tc.view.finding.tab.title", new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject()
            {
                return findingTab.getTabTitle();
            }
        }));
        
        content.add(new Label("tc.view.history.tab.title", new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject()
            {
                return historyTab.getTabTitle();
            }
        }));
        
        content.add(new Label("tc.view.discussion.tab.title", new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject()
            {
                return discussionTab.getTabTitle();
            }
        }));
        
        content.add(new Label("tc.view.forum.tab.title", new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject()
            {
                return forumTab.getTabTitle();
            }
        }));
        
        content.add(new Label("tc.view.organSystem.tab.title", new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject()
            {
                return organSystemTab.getTabTitle();
            }
        }));
        
        content.add(biblioTitleLabel);
        content.add(documentsTitleLabel);
        content.add(linksTitleLabel);
        
        content.add(new Label("tc.view.images.tab.title", new AbstractReadOnlyModel<String>() {
			@Override
        	public String getObject()
        	{
        		return imagesTab instanceof TCViewImagesTab ?
        				((TCViewImagesTab)imagesTab).getTabTitle() : null;
        	}
        }));
        
        
        tabsToIndices.put(overviewTab, 0);
        tabsToIndices.put(diagnosisTab, 1);
        tabsToIndices.put(diffDiagnosisTab, 2);
        tabsToIndices.put(findingTab, 3);
        tabsToIndices.put(historyTab, 4);
        tabsToIndices.put(discussionTab, 5);
        tabsToIndices.put(forumTab, 6);
        tabsToIndices.put(organSystemTab, 7);
        tabsToIndices.put(biblioTab, 8);
        tabsToIndices.put(documentsTab, 9);
        tabsToIndices.put(linksTab, 10);
        
        if (imagesTab instanceof TCViewImagesTab)
        {
        	tabsToIndices.put((TCViewImagesTab)imagesTab, 11);
        }
                
        content.add(overviewTab);
        content.add(diagnosisTab);
        content.add(diffDiagnosisTab);
        content.add(findingTab);
        content.add(historyTab);
        content.add(discussionTab);
        content.add(forumTab);
        content.add(organSystemTab);
        content.add(biblioTab);
        content.add(documentsTab);
        content.add(linksTab);
        content.add(imagesTab);
        
        content.add(tabActivationBehavior);
        
        add(content);
        
        add(new AjaxLink<Void>("tc-solve-btn") {
			@Override
        	public void onClick(AjaxRequestTarget target) {
        		attrVisibilityStrategy.setShowAllIfTrainingModeIsOn(true);
        		
        		target.addComponent(this);
        		target.addComponent(titleText);
        		target.addComponent(content);
        		
        		
				StringBuilder js = new StringBuilder();
				js.append("updateTCViewDialog();");
				js.append(getDisableTabsJavascript());
        		js.append(getHideTabsJavascript());
        		
        		target.appendJavascript(js.toString());
        	}
			@Override
			public boolean isEnabled() {
				return !attrVisibilityStrategy.getShowAllIfTrainingModeIsOn();
			}
			@Override
			public boolean isVisible() {
				return !isEditable() && attrVisibilityStrategy.isTrainingModeOn();
			}
			@Override
			protected void onComponentTag(ComponentTag tag) {
				super.onComponentTag(tag);
				tag.put("title", TCUtilities.getLocalizedString("tc.view.solve.tooltip"));
			}
        }
        .add(new Label("tc-solve-label", new Model<String>(TCUtilities.getLocalizedString("tc.view.solve.text"))))
        .add(new Image("tc-solve-img", ImageManager.IMAGE_TC_LIGHT_BULB).add(
        		(new ImageSizeBehaviour("vertical-align: middle;"))))
        .setOutputMarkupId(true));
        /*
        add(new HeaderContributor(new IHeaderContributor()
		{
			public void renderHead(IHeaderResponse response)
			{		        
				String markupId = getMarkupId(true);
				
				StringBuilder js = new StringBuilder();
				js.append("updateTCView('"+markupId+"');");
				js.append(getDisableTabsJavascript(markupId));
        		js.append(getHideTabsJavascript(markupId));
        		
		        response.renderOnDomReadyJavascript(js.toString());
			}
		}));
		*/
    }
    
    public boolean isEditable()
    {
        return false;
    }
    
    
    public TCEditableObject getTC()
    {
        return (TCEditableObject) getDefaultModelObject();
    }
    
    public String getDisableTabsJavascript() {
    	boolean appendDelimiter=false;
    	StringBuffer sbuf = new StringBuffer();
    	sbuf.append("setDisabledTCViewTabs([");
        for (Map.Entry<AbstractTCViewTab, Integer> me : tabsToIndices.entrySet())
        {
            if (!me.getKey().isTabEnabled())
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
    
    public String getHideTabsJavascript() {
    	boolean appendDelimiter=false;
    	StringBuffer sbuf = new StringBuffer();
    	sbuf.append("setHiddenTCViewTabs([");
        for (Map.Entry<AbstractTCViewTab, Integer> me : tabsToIndices.entrySet())
        {
            if (!me.getKey().isTabVisible())
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
    
    private Component createWebviewerLink(TCModel tcModel) {
    	return Webviewer.getLink(tcModel, webviewerLinkProviders,
	                StudyPermissionHelper.get(),
	                new TooltipBehaviour("tc.view.", "webview"), webviewerSelectionWindow,
	                new WebviewerLinkClickedCallback() {
	                	public void linkClicked(AjaxRequestTarget target) {
	                		TCAuditLog.logTFImagesViewed(getTC());
	                	}
	                })
	        .add(new Label("webview-label", new Model<String>(TCUtilities.getLocalizedString("tc.view.webview.text"))))
	        .add(new SecurityBehavior(TCPanel.getModuleName()
	                        + ":webviewerInstanceLink"))
	        .setOutputMarkupId(true);
    }
    
    public void setCase(AjaxRequestTarget target, TCModel tc) {
    	try
    	{
    		if (!isEditable()) {
    			TCAuditLog.logTFViewed(getTC());
    		}
    		
    		this.attrVisibilityStrategy.setShowAllIfTrainingModeIsOn(false);
    		
    		this.tcModel = tc;
	    	setDefaultModelObject(TCEditableObject.create(tc));

	    	addOrReplace(createWebviewerLink(tc));
	    	
	        setVisible(true);
	    	target.addComponent(this);
	    	
	    	
	        target.appendJavascript("updateTCViewDialog();");
	        
	        //disable tabs
	        if (!isEditable())
	        {
	        	target.appendJavascript(getDisableTabsJavascript());
	        }
	        
	        //hide tabs
	        target.appendJavascript(getHideTabsJavascript());
    	}
    	catch (Exception e)
    	{
    		log.error("Unable to set teaching-file case!", e);
    	}
    }
    
    @Override
    protected void onComponentTag(ComponentTag tag)
    {
        super.onComponentTag(tag);
        tag.put("style", "height:100%;width:100%");
    }
    
    protected void tabSelectionChanged(AjaxRequestTarget target, AbstractTCViewTab newTab, AbstractTCViewTab oldTab)
    {
    	if (sendImagesViewedLog && newTab instanceof TCViewImagesTab) {
    		try {
    			TCAuditLog.logTFImagesViewed(getTC());
    		}
    		finally {
    			sendImagesViewedLog = false;
    		}
    	}
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private IModel<TCEditableObject> getModel()
    {
        return (IModel) getDefaultModel();
    }
    
    private void initWebviewerLinkProvider() {
        List<String> names = WebCfgDelegate.getInstance()
                .getWebviewerNameList();
        if (names == null) {
            names = WebCfgDelegate.getInstance()
                    .getInstalledWebViewerNameList();
        }
        if (names == null || names.isEmpty()) {
            webviewerLinkProviders = null;
        } else {
            webviewerLinkProviders = new WebviewerLinkProvider[names.size()];
            Map<String, String> baseUrls = WebCfgDelegate.getInstance()
                    .getWebviewerBaseUrlMap();
            for (int i = 0; i < webviewerLinkProviders.length; i++) {
                webviewerLinkProviders[i] = new WebviewerLinkProvider(
                        names.get(i));
                webviewerLinkProviders[i]
                        .setBaseUrl(baseUrls.get(names.get(i)));
            }
        }
    }
    
    private AbstractTCViewTab getTabByMarkupId(String id)
    {
    	for (AbstractTCViewTab tab : tabsToIndices.keySet())
    	{
    		if (tab.getMarkupId().equals(id))
    		{
    			return tab;
    		}
    	}
    	return null;
    }

    public abstract static class AbstractTCViewTab extends Panel
    {
    	private TCAttributeVisibilityStrategy attrVisibilityStrategy;
    	
        public AbstractTCViewTab(final String id, IModel<? extends TCObject> model,
        		TCAttributeVisibilityStrategy attrVisibilityStrategy)
        {
            super(id, model);
            this.attrVisibilityStrategy = attrVisibilityStrategy;
        }
        
        public TCObject getTC()
        {
            return (TCObject) super.getDefaultModelObject();
        }
        
        public TCAttributeVisibilityStrategy getAttributeVisibilityStrategy() {
        	return this.attrVisibilityStrategy;
        }
        
        public boolean isEditable()
        {
            return false;
        }
        
        public boolean isEditing()
        {
            return false;
        }
        
        public void setEditing(boolean editing, AjaxRequestTarget target)
        {
            /* do nothing by default */
        }
        
        public boolean isTabEnabled()
        {
            return hasContent();
        }
        
        public boolean isTabVisible()
        {
            return true;
        }
        
        public abstract String getTabTitle();
        
        public abstract boolean hasContent();
        
        protected String getStringValue(TCQueryFilterKey key) {
            TCObject tc = getTC();

            String s = tc != null ? tc.getValueAsLocalizedString(key, this) : null;

            return s != null ? s : "";
        }
        
        protected String getShortStringValue(TCQueryFilterKey key) {
            TCObject tc = getTC();

            String s = tc != null ? tc.getValueAsLocalizedString(key, this, true) : null;

            return s != null ? s : "";
        }
    }
        
    public abstract static class AbstractEditableTCViewTab extends
        AbstractTCViewTab
    {
        public AbstractEditableTCViewTab(final String id, 
        		IModel<TCEditableObject> model,
        		TCAttributeVisibilityStrategy attrVisibilityStrategy)
        {
            super(id, model, attrVisibilityStrategy);
        }
        
        @Override
        public TCEditableObject getTC()
        {
            return (TCEditableObject) super.getTC();
        }
        
        @Override
        public final boolean isEditable()
        {
            return true;
        }
        
        @Override
        public final boolean isEditing()
        {
            return getAttributeVisibilityStrategy().isEditModeOn();
        }
        
        public final void save()
        {
            if (isEditing())
            {
                saveImpl();
            }
        }

        protected abstract void saveImpl();
        
        protected void tabTitleChanged(AjaxRequestTarget target)
        {
            /* do nothing by default */
        }

        protected AttributeModifier createTextInputCssClassModifier()
        {
            return new AttributeAppender("class",true,new Model<String>(
                    isEditing() ? "tc-view-input-editable" : "tc-view-input-non-editable"), " ");
        }
    }
}
