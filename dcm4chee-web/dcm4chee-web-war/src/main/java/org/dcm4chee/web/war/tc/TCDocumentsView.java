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

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.image.NonCachingImage;
import org.apache.wicket.markup.html.link.ResourceLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.util.string.Strings;
import org.dcm4chee.icons.ImageManager;
import org.dcm4chee.icons.behaviours.ImageSizeBehaviour;
import org.dcm4chee.web.common.behaviours.TooltipBehaviour;
import org.dcm4chee.web.war.tc.TCDocumentObject.MimeType;
import org.dcm4chee.web.war.tc.TCModalDialogPanel.ModalDialogCallbackAdapter;
import org.dcm4chee.web.war.tc.TCModalDialogPanel.TCModalDialog;
import org.dcm4chee.web.war.tc.widgets.TCHoverImage;
import org.dcm4chee.web.war.tc.widgets.TCMaskingAjaxDecorator;
import org.dcm4chee.web.war.tc.widgets.TCMaxImageSizeBehavior;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Bernhard Ableitinger <bernhard.ableitinger@agfa.com>
 * @version $Revision$ $Date$
 * @since Jan 04, 2013
 */
@SuppressWarnings("serial")
public class TCDocumentsView extends Panel
{
    private static final Logger log = LoggerFactory.getLogger(TCDocumentsView.class);

    private boolean editing;
    
    public TCDocumentsView(final String id, 
    		IModel<? extends TCObject> model, final boolean editing) {
        super(id, model);

        this.editing = editing;
        
        final TCModalDialog removeDocDlg = TCModalDialog.getOkCancel("remove-doc-dialog",
        		TCUtilities.getLocalizedString("tc.documents.removedialog.msg"), null);
        removeDocDlg.setInitialHeight(100);
        removeDocDlg.setInitialWidth(420);
        removeDocDlg.setUseInitialHeight(true);
        
        final WebMarkupContainer items = new WebMarkupContainer("document-items");
        items.setOutputMarkupId(true);
        items.add(new ListView<TCDocumentObject>("document-item", new ListModel<TCDocumentObject>() {
	                @Override
	                public List<TCDocumentObject> getObject()
	                {
	                    return getTC().getReferencedDocumentObjects();
	                }
        	}) {
                @Override
                protected void populateItem(final ListItem<TCDocumentObject> item) {  
                	final TCDocumentObject doc = item.getModelObject();
                	
                	// compile text
                	MimeType mimeType = doc.getMimeType();
                	Date addedDate = doc.getDocumentAddedDate();
                	String docDescription = doc.getDocumentDescription();
                	if (docDescription==null) {
                		docDescription = doc.getDocumentName();
                	}
                	
                	StringBuilder sbuilder = new StringBuilder();
                	if (docDescription!=null) {
                		sbuilder.append(docDescription).append("\n");
                	}
                	sbuilder.append("<i>");
                	sbuilder.append(mimeType.getDocumentType().getHumanReadableName());
                	sbuilder.append("; ");
                	sbuilder.append(TCUtilities.getLocalizedString("tc.documents.addedon.text")).append(" ");
                	sbuilder.append(DateFormat.getDateInstance(DateFormat.MEDIUM).format(addedDate));
                	sbuilder.append("</i>");
                	
                	// add components
                	final WebMarkupContainer actions = new WebMarkupContainer("document-item-actions");
                	actions.setOutputMarkupId(true);
                	actions.setOutputMarkupPlaceholderTag(true);
                	actions.add(new ResourceLink<Void>("document-item-download-btn", doc.getDocumentContent(true))
    	                .add(new TCHoverImage("document-item-download-image", 
    	                		ImageManager.IMAGE_TC_DISK_MONO, ImageManager.IMAGE_TC_DISK)
    	                    .add(new ImageSizeBehaviour(20, 20, "vertical-align: middle;"))
    	                    .add(new TooltipBehaviour("tc.documents.","download"))
    	                 )
    	            );
                	actions.add(new AjaxLink<Void>("document-item-remove-btn") {
	                        @Override
	                        public void onClick(AjaxRequestTarget target)
	                        {
	                            try
	                            {	                            	
	                            	if (editing) {
		                            	removeDocDlg.setCallback(new ModalDialogCallbackAdapter() {
		                            		@Override
		                            		public void dialogAcknowledged(AjaxRequestTarget target) {
			                            		getEditableTC().removeReferencedDocument(doc);
			                            		target.addComponent(items);
		                            		}
		                            	});
		                            	
		                            	removeDocDlg.show(target);
	                            	}
	                            }
	                            catch (Exception e)
	                            {
	                                log.error("Removing referenced document from teaching-file failed!", e);
	                            }
	                        }
	                        @Override
	                        public boolean isVisible() {
	                        	return editing;
	                        }
	                    }
	                	.add(new TCHoverImage("document-item-remove-image", 
	                			ImageManager.IMAGE_TC_CANCEL_MONO, ImageManager.IMAGE_TC_CANCEL)
	                    	.add(new ImageSizeBehaviour(20, 20, "vertical-align: middle;"))
	                    	.add(new TooltipBehaviour("tc.documents.","remove"))
	                    )
                	);
                	
                    item.add(new AttributeModifier("onmouseover",true,new Model<String>(
                            "$('#" + actions.getMarkupId(true) + "').show();" 
                    )));
                    item.add(new AttributeModifier("onmouseout",true,new Model<String>(
                            "$('#" + actions.getMarkupId(true) + "').hide();" 
                    )));
                   
                    
                	item.add(new ResourceLink<Void>("document-item-view-link", doc.getDocumentContent(false))
                		.add(new NonCachingImage("document-item-image", doc.getDocumentThumbnail())
                			.add(new TCMaxImageSizeBehavior(32,32).setAdditionalCSS("vertical-align:middle"))
                		)
                		.add(new TooltipBehaviour("tc.documents.","view"))
                		.add(new AttributeAppender("target",true,new Model<String>("_blank")," "))
                	);
                	item.add(new MultiLineLabel("document-item-text", sbuilder.toString()) {
	                	    @Override
	                	    protected void onComponentTagBody(final MarkupStream markupStream,
	                	            final ComponentTag openTag) {
	                			final CharSequence body = Strings.toMultilineMarkup(
	                					getDefaultModelObjectAsString());
	                			
	                			replaceComponentTagBody(markupStream, openTag, body.toString().replaceFirst(
	                					"<p>", "<p style=\"margin:0px\">"));
	                	    }
                		}
                		.setEscapeModelStrings(false)
                	);
                	item.add(actions);
                }
        });
        
        final FileUploadField uploadField = new FileUploadField("file-upload-field") {
        	@Override
        	protected void onComponentTag(ComponentTag tag) {
        		super.onComponentTag(tag);
        		
        		MimeType[] types = MimeType.values();
        		if (types!=null && types.length>0) {
        			StringBuilder accept = new StringBuilder();
        			accept.append(types[0].getMimeTypeString());
        			for (int i=1; i<types.length; i++) {
        				accept.append(",").append(
        						types[i].getMimeTypeString());
        			}
        			tag.put("accept", accept.toString());
        		}
        	}
        };
        final long maxUploadSize = Bytes.megabytes(25).bytes();
        final TextArea<String> uploadDescription = new TextArea<String>("file-upload-description-text", new Model<String>());
        final Form<Void> uploadForm = new Form<Void>("file-upload-form");
		uploadForm.setMultiPart(true);
		uploadForm.setMaxSize(Bytes.megabytes(25)); // seems that this doesn't work because of a bug in WICKET 1.4
		uploadForm.add(new Label("file-upload-label", 
				TCUtilities.getLocalizedString(
						"tc.documents.upload.text")));
		uploadForm.add(new Label("file-upload-description-label", 
				TCUtilities.getLocalizedString(
						"tc.documents.upload.description.text")));
		uploadForm.add(uploadField);
		uploadForm.add(uploadDescription);
		uploadForm.add(new AjaxButton("file-upload-btn") {			
				@Override
				public void onSubmit(AjaxRequestTarget target, Form<?> form) {
		            final FileUpload upload = uploadField.getFileUpload();
		            if (upload != null)
		            {
		            	String contentType = null, fileName = null;
		            	final long totalBytes = upload.getSize();		            	
		                try {
		                	if (totalBytes>0) {
		                		if (totalBytes<=maxUploadSize) {
				                	getEditableTC().addReferencedDocument(
				                			MimeType.get(contentType = upload.getContentType()), 
				                			fileName = upload.getClientFileName(), 
				                			upload.getInputStream(), 
				                			uploadDescription.getModelObject());
		                		}
		                		else {
		                			log.warn("File upload denied: Max upload size is " + maxUploadSize + " bytes!");
		                		}
		                	}
		                	
		                	target.addComponent(items);
		                }
		                catch (Exception e) {              	
		                	log.error("Unable to upload teaching-file referenced document (content-type='"+
		                			contentType+"', file-name='" + fileName + "')!", e);
		                }
		                finally {
		                	upload.closeStreams();

		                	uploadField.clearInput();
		                	uploadDescription.clearInput();
		                	uploadDescription.setModelObject(null);
		                	
		                	target.addComponent(uploadForm);
		                }
		            }
				}
				@Override
				public void onError(AjaxRequestTarget target, Form<?> form) {
				}
				@Override
				protected IAjaxCallDecorator getAjaxCallDecorator()
				{
					return new TCMaskingAjaxDecorator(true, true);
				}
			}
			.add(new Label("file-upload-btn-text", 
				TCUtilities.getLocalizedString(
						"tc.documents.upload.start.text")))
		);

		add(items);
		add(removeDocDlg);
        add(new WebMarkupContainer("file-upload") {
	        	@Override
	        	public boolean isVisible() {
	        		return editing;
	        	}
	        }
        	.add(new Image("file-upload-info-img", ImageManager.IMAGE_TC_INFO) {
	        		@Override	
	        		protected void onComponentTag(ComponentTag tag) {
        				super.onComponentTag(tag);
        				
        				StringBuilder sbuilder = new StringBuilder();
        				sbuilder.append(MessageFormat.format(
        						TCUtilities.getLocalizedString(
        								"tc.documents.upload.maxsize.text"),
        						Bytes.bytes(maxUploadSize).megabytes()));
        				sbuilder.append("\n");
        				
        				MimeType[] mimeTypes = MimeType.values();
        				if (mimeTypes!=null) {
        					boolean firstExt = true;
        					int nExtPerLine = 8;
        					int nExtInLine = 0;
        					for (MimeType type : mimeTypes) {
        						String[] exts = type.getSupportedFileExtensions();
        						if (exts!=null) {
        							for (String ext : exts) {
        								String s = "*."+ext;
        								if (firstExt) {
        									sbuilder.append(TCUtilities.getLocalizedString(
        											"tc.documents.upload.files.text"));
        									sbuilder.append("\n");
        									sbuilder.append(s);
        									firstExt = false;
        									nExtInLine++;
        								}
        								else {
	        								if (!sbuilder.toString().contains(s)) {
	        									if (nExtInLine==nExtPerLine) {
	        										sbuilder.append("\n");
	        										nExtInLine=0;
	        									}
	        									else {
	        										sbuilder.append(", ");
	        									}
	        									sbuilder.append(s);
	        									nExtInLine++;
	        								}
        								}
        							}
        						}
        					}
        				}
        				tag.put("title", sbuilder.toString());
        			}
        		}
        		.add(new ImageSizeBehaviour(16,16,"vertical-align:middle;margin:5px;"))
        	)
        	.add(uploadForm)
        	.setMarkupId("documents-file-upload")
        );
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
}
