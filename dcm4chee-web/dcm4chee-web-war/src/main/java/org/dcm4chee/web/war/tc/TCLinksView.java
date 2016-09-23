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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.image.resource.LocalizedImageResource;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.util.time.Duration;
import org.dcm4che2.data.Tag;
import org.dcm4chee.archive.entity.Instance;
import org.dcm4chee.archive.util.JNDIUtils;
import org.dcm4chee.icons.ImageManager;
import org.dcm4chee.icons.behaviours.ImageSizeBehaviour;
import org.dcm4chee.web.common.behaviours.TooltipBehaviour;
import org.dcm4chee.web.dao.tc.TCQueryFilter;
import org.dcm4chee.web.dao.tc.TCQueryLocal;
import org.dcm4chee.web.war.StudyPermissionHelper;
import org.dcm4chee.web.war.config.delegate.WebCfgDelegate;
import org.dcm4chee.web.war.tc.TCLink.TCLinkRelationship;
import org.dcm4chee.web.war.tc.TCModalDialogPanel.ModalDialogCallbackAdapter;
import org.dcm4chee.web.war.tc.TCModalDialogPanel.TCModalDialog;
import org.dcm4chee.web.war.tc.TCUtilities.SelfUpdatingTextArea;
import org.dcm4chee.web.war.tc.TCUtilities.SelfUpdatingTextField;
import org.dcm4chee.web.war.tc.TCUtilities.TCToolTipAppender;
import org.dcm4chee.web.war.tc.imageview.TCImageViewImage;
import org.dcm4chee.web.war.tc.imageview.TCWadoImage;
import org.dcm4chee.web.war.tc.imageview.TCWadoImageSize;
import org.dcm4chee.web.war.tc.widgets.TCAjaxComboBox;
import org.dcm4chee.web.war.tc.widgets.TCHoverImage;
import org.dcm4chee.web.war.tc.widgets.TCMaskingAjaxDecorator;
import org.dcm4chee.web.war.tc.widgets.TCMultiLineLabel;
import org.dcm4chee.web.war.tc.widgets.TCMultiLineLabel.AutoClampSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Bernhard Ableitinger <bernhard.ableitinger@agfa.com>
 * @version $Revision$ $Date$
 * @since Mar 31, 2013
 */
@SuppressWarnings("serial")
public class TCLinksView extends Panel
{
    private static final Logger log = LoggerFactory.getLogger(TCLinksView.class);

    private boolean editing;
    
    private String selectedUID;

    private Component linkBtn;
    private Component searchField;

    public TCLinksView(final String id, 
    		IModel<? extends TCObject> model, 
    		final TCAttributeVisibilityStrategy attrVisibility,
    		final boolean editing) {
        super(id, model);

        this.editing = editing;
        
        final TCModalDialog removeDlg = TCModalDialog.getOkCancel("remove-link-dialog",
        		TCUtilities.getLocalizedString("tc.links.removedialog.msg"), null);
        removeDlg.setInitialHeight(100);
        removeDlg.setInitialWidth(420);
        removeDlg.setUseInitialHeight(true);
        add(removeDlg);
        
        final WebMarkupContainer links = new WebMarkupContainer("links");
        links.setOutputMarkupId(true); 
        links.add(new ListView<TCLink>("link", new ListModel<TCLink>() {
	                @Override
	                public List<TCLink> getObject()
	                {
	                    List<TCLink> list = new ArrayList<TCLink>(getTC().getLinks());
	                    for (Iterator<TCLink> it=list.iterator(); it.hasNext(); ) {
	                    	TCLink link = it.next();
	                    	if (!link.isPermitted() || link.getLinkedCase()==null) {
	                    		it.remove();
	                    	}
	                    }
	                    return list;
	                }
        	}) {
                @Override
                protected void populateItem(final ListItem<TCLink> item) {  
                	final TCLink link = item.getModelObject();
                	final TCEditableObject tc = link.getLinkedCase();
                	
                	//relationship
                	item.add(new Label("link-relationship", link.getLinkRelationship().toString())
                		.add(new AttributeAppender("style",true,new Model<String>(!isShownInDialog()?"float:right":""),";"))
                	);
                	
                	//title link
                	item.add(new AjaxLink<Void>("link-title-link") {
	                		@Override
	                		public boolean isEnabled() {
	                			return !editing;
	                		}
	                		@Override
	                		public void onClick(AjaxRequestTarget target) {
	                			try {
	                				if (!openLinkInDialog(target, link)) {
	                					PageParameters params = new PageParameters();
	                					params.put("uid", link.getLinkedCaseUID());
	                					setResponsePage(new TCCaseViewPage(params));
	                				}
	                	    	}
	                	    	catch (Exception e) {
	                	    		log.error("Unable to open teaching-file link!", e);
	                	    	}
	                		}
	                	}
	                	.add(new Label("link-title", new AbstractReadOnlyModel<String>() {
		                		@Override
		                		public String getObject() {
		                        	if (!attrVisibility.isAttributeVisible(TCAttribute.Title)) {
		                        		return TCUtilities.getLocalizedString("tc.case.text")+
		                        				" " + link.getLinkedCase().getId();
		                        	}
		                        	return link.getLinkedCase().getTitle();
		                		}
	                		})
	                	{
	                		@Override
	                		protected void onComponentTag(ComponentTag tag) {
	                			super.onComponentTag(tag);
	                			if (!editing) {
	                				tag.put("onmouseout", "$(this).siblings('img').hide();");
	                				tag.put("onmouseover", "$(this).siblings('img').show();");
	                			}
	                		}
	                	})
	                	.add(new Image("link-follow-image", ImageManager.IMAGE_TC_EYE_MONO)
	                		.add(new AttributeAppender("style",true,
	                				new Model<String>("display:none"),";"))
	                	)
	                	.add(new TCToolTipAppender("tc.case.open.text") {
	                		@Override
	                		public boolean isEnabled(Component c) {
	                			return !editing;
	                		}
	                	})
	                );
                	                	
                	//image link
                	item.add(new TCWadoImage("link-image", new Model<TCImageViewImage>() {
	                		@Override
	                		public TCImageViewImage getObject() {
	                			List<TCReferencedImage> refImages = tc.getReferencedImages();
	                			return refImages!=null && !refImages.isEmpty() ? 
	                					refImages.get(0) : null;
	                		}
	                	}, TCWadoImageSize.createWidthInstance(64)) {
                			@Override
                	        protected LocalizedImageResource createEmptyImage() {
                	        	LocalizedImageResource emptyImage = new LocalizedImageResource(this);
                	        	emptyImage.setResourceReference(ImageManager.IMAGE_TC_IMAGE_SQUARE);
                	        	return emptyImage;
                	        }
                		}
                	);
                	
                	//abstract
                	item.add(new TCMultiLineLabel("link-abstract", new AbstractReadOnlyModel<String>() {
	                		@Override
	                		public String getObject() {
	                			if (!attrVisibility.isAttributeVisible(TCAttribute.Abstract)) {
	                				return TCUtilities.getLocalizedString("tc.obfuscation.text");
	                			}
	                			return link.getLinkedCase().getAbstr();
	                		}
                		}, new AutoClampSettings(40))
                		.setEscapeModelStrings(false)
                	);
                	
                	//comment
                	item.add(new TCMultiLineLabel("link-comment", new AbstractReadOnlyModel<String>() {
                			@Override
                			public String getObject() {
                            	StringBuilder comment = new StringBuilder();
                            	comment.append(TCUtilities.getLocalizedString("tc.links.comment.text"));
                            	comment.append("&nbsp;");
                            	comment.append(link.getLinkComment());
                            	return comment.toString();
                			}
                		}) {
                			@Override
                			public boolean isVisible() {
                				return link.getLinkComment()!=null && !link.getLinkComment().isEmpty();
                			}

                		}
                		.setEscapeModelStrings(false)
                		.setOutputMarkupPlaceholderTag(true)
                	);

                	//actions
                	final WebMarkupContainer actions = new WebMarkupContainer("link-actions");
                	actions.setOutputMarkupId(true);
                	actions.setOutputMarkupPlaceholderTag(true);
                	actions.add(new AjaxLink<Void>("link-remove-btn") {
	                        @Override
	                        public void onClick(AjaxRequestTarget target)
	                        {
	                            try
	                            {	                            	
	                            	if (editing) {
		                            	removeDlg.setCallback(new ModalDialogCallbackAdapter() {
		                            		@Override
		                            		public void dialogAcknowledged(AjaxRequestTarget target) {
			                            		getEditableTC().removeLink(link);
			                            		target.addComponent(links);
		                            		}
		                            	});
		                            	
		                            	removeDlg.show(target);
	                            	}
	                            }
	                            catch (Exception e)
	                            {
	                                log.error("Removing link from teaching-file failed!", e);
	                            }
	                        }
	                        @Override
	                        public boolean isVisible() {
	                        	return editing;
	                        }
	                    }
	                	.add(new TCHoverImage("link-remove-image", 
	                			ImageManager.IMAGE_TC_CANCEL_MONO, ImageManager.IMAGE_TC_CANCEL)
	                    	.add(new ImageSizeBehaviour(20, 20, "vertical-align: middle;"))
	                    	.add(new TooltipBehaviour("tc.links.","remove"))
	                    )
                	);
                	
                    item.add(new AttributeModifier("onmouseover",true,new Model<String>(
                            "$('#" + actions.getMarkupId(true) + "').show();" 
                    )));
                    item.add(new AttributeModifier("onmouseout",true,new Model<String>(
                            "$('#" + actions.getMarkupId(true) + "').hide();" 
                    )));

                	item.add(actions);
                }
        });
             
        add(links);
        
        final WebMarkupContainer search = new WebMarkupContainer("link-search") {
        	@Override
        	public boolean isVisible() {
        		return editing;
        	}
        };
        
        search.add(new Image("link-search-info-img", ImageManager.IMAGE_TC_INFO) {
	    		@Override	
	    		protected void onComponentTag(ComponentTag tag) {
					super.onComponentTag(tag);
	
					tag.put("title", TCUtilities.getLocalizedString("tc.links.info.text"));
				}
			}
			.add(new ImageSizeBehaviour(16,16,"vertical-align:middle;margin:5px;"))
        );
        
        final ListModel<Instance> searchModel = new ListModel<Instance>(new ArrayList<Instance>());
        final WebMarkupContainer searchResults = new WebMarkupContainer("link-search-case-results");
        
        // link case search result
        search.add(new Label("link-search-case-label",
        		TCUtilities.getLocalizedString("tc.links.search.case.text")));
        
        // link relationship
        final TCAjaxComboBox<TCLinkRelationship> relationshipCBox = 
        		new TCAjaxComboBox<TCLinkRelationship>(
        		"link-search-relationship-select", 
        		Arrays.asList(TCLinkRelationship.values()),
        		TCLinkRelationship.RELATES_TO) {
        	private final int MAX_RESULTS = 20;
            final EnumSet<TCLinkRelationship> patientCases = EnumSet.of(
            		TCLinkRelationship.ANTERIOR, TCLinkRelationship.POSTERIOR);
            final EnumSet<TCLinkRelationship> searchCases = 
            		EnumSet.complementOf(patientCases);
        	@Override
        	protected TCLinkRelationship convertValue(String value) throws Exception {
        		return TCLinkRelationship.valueOfLocalized(value);
        	}
        	@Override
        	protected void valueChanged(TCLinkRelationship value, TCLinkRelationship oldValue, AjaxRequestTarget target) {
	        	boolean changedToSearchCases = patientCases.contains(oldValue) && searchCases.contains(value);
	        	boolean changedToPatientCases = patientCases.contains(value) && searchCases.contains(oldValue);
	        	
	        	if (changedToSearchCases || changedToPatientCases) {
	        		if (target!=null) {
		        		target.addComponent(searchField);
		        		target.appendJavascript("$('#"+searchField.getMarkupId(true)+"').textfield();");
		        	}
		        	
	        		if (changedToPatientCases) {
		        		try {
							TCEditableObject tc = getEditableTC();
							if (tc==null) {
								log.warn("Unable to create/add teaching-file link: Teaching-File not editable!");
							}
							else {
								List<Instance> result = Collections.emptyList();
			        	        TCQueryLocal dao = (TCQueryLocal) JNDIUtils
			        	                .lookup(TCQueryLocal.JNDI_NAME);
			        	        
			                    List<String> roles = StudyPermissionHelper.get()
			                            .applyStudyPermissions() ? StudyPermissionHelper.get()
			                            .getDicomRoles() : null;
			
			        	        result = dao.findInstancesOfPatient(tc.getPatientId(), tc.getPatientIdIssuer(), 
			        	        		roles, WebCfgDelegate.getInstance().getTCRestrictedSourceAETList());
			        	        
		        	        	if (result.size()>MAX_RESULTS) {
		        	        		result = result.subList(0, MAX_RESULTS);
		        	        	}
		        	        	
		        	        	String iuid = tc.getInstanceUID();
		        	        	for (Iterator<Instance> it=result.iterator(); it.hasNext();) {
		        	        		if (iuid.equals(it.next().getSOPInstanceUID())) {
		        	        			it.remove();
		        	        			break;
		        	        		}
		        	        	}
		        	        	
		        	        	searchModel.setObject(result);
		        	        	
			        	        if (selectedUID!=null) {
			        	        	boolean containsSelectedUID = false;
			        	        	for (Instance i : result) {
			        	        		if (selectedUID.equals(i.getSOPInstanceUID())) {
			        	        			containsSelectedUID = true;
			        	        			break;
			        	        		}
			        	        	}
			        	        	if (!containsSelectedUID) {
			        	        		selectedUID = null;
			        	        	}
			        	        }

		        	        	if (target!=null) {
		        	        		target.addComponent(searchResults);
		        	        		target.addComponent(linkBtn);
		        	        		target.appendJavascript("$('#"+linkBtn.getMarkupId(true)+"').button();");
		        	        		target.appendJavascript("$('#"+searchResults.getMarkupId(true)+"').menu();");
		        	        	}
							}
		        		}
		        		catch (Exception e) {
		        			log.error(null, e);
		        		}
	        		}
	        	}
        	}
        };
        search.add(new Label("link-search-relationship-label",
        		TCUtilities.getLocalizedString("tc.links.search.relationship.text")));
        search.add(relationshipCBox);
                
        final ListView<Instance> searchResultsList = new ListView<Instance>("link-search-case-results-list", searchModel) {
        	@Override
        	protected void populateItem(final ListItem<Instance> item) {
        		final Instance i = item.getModelObject();
        		String title = i.getAttributes(false).getString(Tag.ContentLabel);
        		item.setOutputMarkupId(true);
        		item.add(new AjaxLink<Void>("link-search-case-results-list-item") {
	    				@Override
	    				public void onClick(AjaxRequestTarget target) {
	    					selectedUID = i.getSOPInstanceUID();
	
	    					String markupId = item.getMarkupId(true);
	                		StringBuffer sbuf = new StringBuffer();
	                		sbuf.append("$('#").append(markupId).append("').siblings().removeClass('ui-state-active');");
	                		sbuf.append("$('#").append(markupId).append("').removeClass('ui-state-default');");
	                		sbuf.append("$('#").append(markupId).append("').addClass('ui-state-active');");
	                		
	    					target.addComponent(linkBtn);
	    					target.appendJavascript(sbuf.toString());
        	        		target.appendJavascript("$('#"+linkBtn.getMarkupId(true)+"').button();");
	    				}
	    			}.add(new Label("link-search-case-results-item-text", title))
        		);

                if (selectedUID!=null && selectedUID.equals(i.getSOPInstanceUID())) {
                	item.add(new AttributeAppender("class", true, new Model<String>("ui-state-active"), " "));
                }
        	}
        };
        searchResultsList.setOutputMarkupId(true);
        searchResults.setOutputMarkupId(true);
        searchResults.add(searchResultsList);
        
        search.add(searchResults);
        
        // link case search/input
        search.add((searchField=new SelfUpdatingTextField("link-search-case-input", "") {
        	private final int MAX_RESULTS = 50;
        	private volatile String currentSearchString = null;
        	private TCQueryFilter filter = new TCQueryFilter();
        	private IAjaxCallDecorator cursorDecorator = new TCMaskingAjaxDecorator(false, true);
        	@Override
        	protected Duration getThrottleDelay() {
        		return Duration.milliseconds(300);
        	}
        	@Override
        	protected String getUpdateEvent() {
        		return "onkeyup";
        	}
        	@Override
        	protected IAjaxCallDecorator getUpdateDecorator() {
        		return cursorDecorator;
        	}
        	@Override
        	public boolean isVisible() {
        		TCLinkRelationship relationship = relationshipCBox.getModelObject();
        		return !TCLinkRelationship.ANTERIOR.equals(relationship) &&
        				!TCLinkRelationship.POSTERIOR.equals(relationship);
        	}
        	@Override
        	protected void onComponentTag(ComponentTag tag) {
        		super.onComponentTag(tag);
        		
        		tag.put("autocomplete","off");
        		tag.put("placeholder", TCUtilities.getLocalizedString(
        				"tc.links.search.hint.text"));
        	}
        	@Override
        	protected void textUpdated(String text, AjaxRequestTarget target) {
        		boolean search = false;
        		synchronized (this)
        		{
	        		if (!stringEqualsIgnoreCase(currentSearchString, text))
	        		{
	        			currentSearchString = text;
	        			search = true;
	        		}
        		}
        		if (search)
        		{
    	        	selectedUID = null;
    	        	
        			searchImpl(currentSearchString, target);
        		}
        	}
        	private void searchImpl(final String searchString, AjaxRequestTarget target) {
        		try {
        			List<Instance> result = Collections.emptyList();
        			if (searchString!=null && !searchString.isEmpty()) {
	        			filter.setTitle(searchString);
	        			
	        	        TCQueryLocal dao = (TCQueryLocal) JNDIUtils
	        	                .lookup(TCQueryLocal.JNDI_NAME);
	        	        
	                    List<String> roles = StudyPermissionHelper.get()
	                            .applyStudyPermissions() ? StudyPermissionHelper.get()
	                            .getDicomRoles() : null;
	
	        	        result = dao.findMatchingInstances(filter, roles, 
	        	        		WebCfgDelegate.getInstance().getTCRestrictedSourceAETList(), false);
        			}
        			
        	        if (stringEqualsIgnoreCase(currentSearchString, searchString)) {
        	        	if (result.size()>MAX_RESULTS) {
        	        		result = result.subList(0, MAX_RESULTS);
        	        	}
        	        	
        	        	String iuid = getEditableTC().getInstanceUID();
        	        	for (Iterator<Instance> it=result.iterator(); it.hasNext();) {
        	        		if (iuid.equals(it.next().getSOPInstanceUID())) {
        	        			it.remove();
        	        			break;
        	        		}
        	        	}
        	        	
        	        	searchModel.setObject(result);
        	        	
        	        	if (target!=null) {
        	        		target.addComponent(searchResults);
        	        		target.addComponent(linkBtn);
        	        		target.appendJavascript("$('#"+linkBtn.getMarkupId(true)+"').button();");
        	        		target.appendJavascript("$('#"+searchResults.getMarkupId(true)+"').menu();");
        	        	}
        	        }
        		}
        		catch (Exception e) {
        			log.error(null, e);
        		}
        	}
        	private boolean stringEqualsIgnoreCase(String s1, String s2) {
        		return s1==s2 || (s1!=null && s2!=null && s1.equalsIgnoreCase(s2));
        	}
        }).setOutputMarkupId(true).setOutputMarkupPlaceholderTag(true));
        
        // link comment
        final TextArea<String> commentArea = new SelfUpdatingTextArea("link-search-comment-area", new Model<String>());
        search.add(new Label("link-search-comment-label",
        		TCUtilities.getLocalizedString("tc.links.comment.text")));
        search.add(commentArea);

        // link button
		search.add(linkBtn = new AjaxLink<Void>("link-link-btn") {
				@Override
				public boolean isEnabled() {
					return selectedUID!=null && relationshipCBox.getModelObject()!=null;
				}
				@Override
				public void onClick(AjaxRequestTarget target) {
					try {
						if (selectedUID==null) {
							log.warn("Unable to create/add teaching-file link: No case selected!");
						}
						else if (relationshipCBox.getModelObject()==null) {
							log.warn("Unable to create/add teaching-file link: No link relationship selected!");
						}
						else {
							TCEditableObject tc = getEditableTC();
							if (tc==null) {
								log.warn("Unable to create/add teaching-file link: Teaching-File not editable!");
							}
							else {
								tc.addLink(new TCLink(tc.getInstanceUID(), selectedUID, 
										relationshipCBox.getModelObject(), commentArea.getModelObject()));
							}
						}
					}
					catch (Exception e) {              	
						log.error("Unable to create/add teaching-file link!", e);
					}
					finally {
						target.addComponent(links);
					}
				}
				@Override
				protected IAjaxCallDecorator getAjaxCallDecorator()
				{
					return new TCMaskingAjaxDecorator(false, true);
				}
			}
			.add(new Label("link-link-btn-text", 
				TCUtilities.getLocalizedString(
						"tc.links.link.text")))
			.setOutputMarkupId(true)
		);

		add(search);
		
        add(new HeaderContributor(new IHeaderContributor()
		{
			public void renderHead(IHeaderResponse response)
			{		        
		        response.renderOnDomReadyJavascript("$('#"+searchResults.getMarkupId(true)+"').menu();");
			}
		}));
    }
    
    private TCObject getTC() {
    	return (TCObject) getDefaultModelObject();
    }
    
    private TCEditableObject getEditableTC() {
    	TCObject tc = getTC();
    	
    	if (editing && tc instanceof TCEditableObject) {
    		return (TCEditableObject) tc;
    	}
    	
    	return null;
    }
    
    private boolean isShownInDialog() {
    	return getParent() instanceof TCViewLinksTab;
    }
    
    private boolean openLinkInDialog(AjaxRequestTarget target, TCLink link) throws Exception {
    	TCModel tc = link.getLinkedCaseModel();
    	TCPanel mainPanel = TCUtilities.findMainPanel(this);
    	TCViewDialog viewDialog = mainPanel!=null ? mainPanel.getViewDialog() : null;
    	if (viewDialog!=null) {
	    	if (viewDialog.isShown()) {
	    		viewDialog.getView().setCase(target, tc);
	    	}
	    	else {
	    		viewDialog.open(null, target, tc, 
	    				new Model<Boolean>(false), 
	    				mainPanel.getResultCaseProvider(), 
	    				false, null);
	    	}
	    	return true;
    	}
    	return false;
    }
}
