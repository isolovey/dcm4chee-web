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

package org.dcm4chee.web.war.folder.webviewer;

import java.io.Serializable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.wicket.Page;
import org.apache.wicket.PageMap;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.link.PopupSettings;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.apache.wicket.protocol.http.WebResponse;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.protocol.http.request.WebClientInfo;
import org.apache.wicket.request.target.basic.EmptyRequestTarget;
import org.dcm4che2.data.UID;
import org.dcm4chee.archive.entity.StudyPermission;
import org.dcm4chee.icons.ImageManager;
import org.dcm4chee.icons.behaviours.ImageSizeBehaviour;
import org.dcm4chee.web.common.behaviours.TooltipBehaviour;
import org.dcm4chee.web.common.markup.ModalWindowLink;
import org.dcm4chee.web.common.webview.link.WebviewerLinkProvider;
import org.dcm4chee.web.war.StudyPermissionHelper;
import org.dcm4chee.web.war.common.model.AbstractDicomModel;
import org.dcm4chee.web.war.config.delegate.WebCfgDelegate;
import org.dcm4chee.web.war.folder.model.InstanceModel;
import org.dcm4chee.web.war.folder.model.PatientModel;
import org.dcm4chee.web.war.folder.model.SeriesModel;
import org.dcm4chee.web.war.folder.model.StudyModel;
import org.dcm4chee.web.war.tc.TCModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 * @version $Revision: 18453 $ $Date: 2015-05-23 18:41:04 +0200 (Sa, 23 Mai 2015) $
 * @since May 24, 2011
 */
public class Webviewer  {

    private static final String WEBVIEW_ID = "webview";
    private static Logger log = LoggerFactory.getLogger(Webviewer.class);

    public static AbstractLink getLink(final AbstractDicomModel model, final WebviewerLinkProvider[] providers,
            final StudyPermissionHelper studyPermissionHelper, TooltipBehaviour tooltip, ModalWindow modalWindow) {
    	return getLink(model, providers, studyPermissionHelper, tooltip, modalWindow, null);
    }
    	
    @SuppressWarnings("serial")
	public static AbstractLink getLink(final AbstractDicomModel model, final WebviewerLinkProvider[] providers,
            final StudyPermissionHelper studyPermissionHelper, TooltipBehaviour tooltip, ModalWindow modalWindow,
            final WebviewerLinkClickedCallback callback) {
        final WebviewerLinkProvider[] p = {null};
        AbstractLink link = null;
        if (providers != null)
            for (int i = 0 ; i < providers.length ; i++) {
                if (isLevelSupported(model, providers[i])) {
                    if (p[0] == null) {
                        p[0] = providers[i];
                    } else {
                        link = getWebviewerSelectionPageLink(model, providers, modalWindow, callback);
                        break;
                    }
                }
            }
        if (p[0] == null) {
            link = new ExternalLink(WEBVIEW_ID, "http://dummy");
            link.setVisible(false);
        } else {
            if (link == null) {
                if (p[0].hasOwnWindow()) {
                    String url = getUrlForModel(model, p[0]);
                    link = new ExternalLink(WEBVIEW_ID, url);
                	((ExternalLink)link).setPopupSettings(getPopupSettingsForDirectGET(url));
                } else if (p[0].supportViewingAllSelection()) {
                    link = new Link<Object>(WEBVIEW_ID) {

                        private static final long serialVersionUID = 1L;
                        
                        @Override
                        public void onClick() {
                            try {
                                ExportDicomModel dicomModel = new ExportDicomModel(model);

                                RequestCycle.get().setRequestTarget(EmptyRequestTarget.getInstance());

                                HttpServletRequest request =
                                    ((WebRequestCycle) RequestCycle.get()).getWebRequest().getHttpServletRequest();
                                HttpServletResponse response =
                                    ((WebResponse) getRequestCycle().getResponse()).getHttpServletResponse();
                                String result =p[0].viewAllSelection(dicomModel.getPatients(), request, response);
                                if (!p[0].notWebPageLinkTarget()){
                                    ViewerPage page = new ViewerPage();
                                    page.add(new Label("viewer", new Model<String>(result)).setEscapeModelStrings(false));
                                    this.setResponsePage(page);
                                }                     
                            } catch (Exception e) {
                                log.error("Cannot view the selection!", e);
                                if (p[0].notWebPageLinkTarget()) {
                                    setResponsePage(getPage());
                                }
                            }
                        }
                    };
                    
                    WebClientInfo clientInfo = (WebClientInfo)WebRequestCycle.get().getClientInfo();
                    String browser = clientInfo.getUserAgent();
                    
                    if (!p[0].notWebPageLinkTarget() || (browser != null && browser.contains("Firefox"))){
                        ((Link) link).setPopupSettings(new PopupSettings(PageMap.forName("webviewPage"), 
                            PopupSettings.RESIZABLE|PopupSettings.SCROLLBARS));
                    }
                } else {
                    link =  new Link<Object>(WEBVIEW_ID) {
                        private static final long serialVersionUID = 1L;
            
                        @Override
                        public void onClick() {
                            setResponsePage(new WebviewerRedirectPage(model, p[0]));
                        }
                    };
                    ((Link) link).setPopupSettings(new PopupSettings(PageMap.forName("webviewPage"), 
                        PopupSettings.RESIZABLE|PopupSettings.SCROLLBARS));
                }
                if (callback!=null) {
                	link.add(new AjaxEventBehavior("onclick") {
						@Override
						public void onEvent(AjaxRequestTarget target) {
                			callback.linkClicked(target);
                		}
						// just for the case, if the link is rendered as a <button> element
						@Override
						protected void onComponentTag(ComponentTag tag) {
							String current = tag.getAttribute("onclick");
							
							super.onComponentTag(tag);
							
							if (current!=null) {
								tag.put("onclick", new StringBuilder(tag.getAttribute("onclick"))
									.append(";").append(current));
							}
						}
                	});
                }
            }
            if (model instanceof PatientModel) {
                link.setVisible(studyPermissionHelper.checkPermission(model.getDicomModelsOfNextLevel(), 
                        StudyPermission.READ_ACTION, false));
            } else {
                link.setVisible(studyPermissionHelper.checkPermission(model, StudyPermission.READ_ACTION));
            }
        }
        Image image = new Image("webviewImg",ImageManager.IMAGE_FOLDER_VIEWER);
        image.add(new ImageSizeBehaviour("vertical-align: middle;"));
        if (tooltip != null) image.add(tooltip);
        link.add(image);
        return link;
    }

    public static PopupSettings getPopupSettingsForDirectGET(final String url) {
		PopupSettings ps = new PopupSettings(){
			@Override
			public String getPopupJavaScript(){
				return getSendHttpRequestJavascript(url)+"return false;";
			}
		};
		return ps;
	}
    public static String getSendHttpRequestJavascript(final String url) {
			StringBuilder sb = new StringBuilder();
			sb.append("xmlHttp = new XMLHttpRequest();")
			.append("xmlHttp.open( 'GET', '").append(url).append("&zz=").append("'+new Date().getTime(), true );")
			.append("xmlHttp.send( null );");
			return sb.toString();
	}


	private static AbstractLink getWebviewerSelectionPageLink(final AbstractDicomModel model, final WebviewerLinkProvider[] providers, final ModalWindow modalWindow,
    		final WebviewerLinkClickedCallback callback) {
        log.debug("Use SelectionLINK for model:{}", model);
        if (modalWindow == null) {
            Link<Object> link =  new Link<Object>(WEBVIEW_ID) {
                private static final long serialVersionUID = 1L;
    
                @Override
                public void onClick() {
                    setResponsePage(new WebviewerSelectionPage(model, providers, null, callback));
                }
            };
            link.setPopupSettings(new PopupSettings(PageMap.forName("webviewPage"), 
                    PopupSettings.RESIZABLE|PopupSettings.SCROLLBARS));
            return link;
        } else {
            int[] winSize = WebCfgDelegate.getInstance().getWindowSize("webviewer");
            return new ModalWindowLink(WEBVIEW_ID, modalWindow, winSize[0], winSize[1]) {
                
                private static final long serialVersionUID = 1L;
        
                @Override
                public void onClick(AjaxRequestTarget target) {
        
                    modalWindow.setPageCreator(new ModalWindow.PageCreator() {
                        
                        private static final long serialVersionUID = 1L;
                          
                        @Override
                        public Page createPage() {
                            return new WebviewerSelectionPage(model, providers, modalWindow, callback);
                        }
                    });
                    modalWindow.setTitle("");
                    modalWindow.show(target);
                }
                
            };
        }
    }
	
	public static boolean isLevelSupported(AbstractDicomModel model, WebviewerLinkProvider provider) {
        switch (model.levelOfModel()) {
        case AbstractDicomModel.PATIENT_LEVEL:
            return provider.supportPatientLevel();
        case AbstractDicomModel.STUDY_LEVEL:
        	return provider.supportStudyLevel();
        case AbstractDicomModel.SERIES_LEVEL:
        	return provider.supportSeriesLevel();
        case AbstractDicomModel.INSTANCE_LEVEL:
        	if ( model instanceof TCModel && !provider.supportStructuredReport() ) {
        		return provider.supportStudyLevel();
        	} else {
        		return provider.supportInstanceLevel();
            }
        default:
            log.warn("Level of Model not supported by this Webviewer Selection Page! model:"+
                    model.getClass().getName()+" level:"+model.levelOfModel());
            return false;
        }
	}

    public static String getUrlForModel(AbstractDicomModel model, WebviewerLinkProvider provider) {
        switch (model.levelOfModel()) {
            case AbstractDicomModel.PATIENT_LEVEL:
                if (provider.supportPatientLevel()) {
                    PatientModel pat = (PatientModel) model;
                    return provider.getUrlForPatient(pat.getId(), pat.getIssuer());
                }
                break;
            case AbstractDicomModel.STUDY_LEVEL:
                if (provider.supportStudyLevel()) {
                    return provider.getUrlForStudy(((StudyModel) model).getStudyInstanceUID());
                }
                break;
            case AbstractDicomModel.SERIES_LEVEL:
                if (provider.supportSeriesLevel()) {
                    return provider.getUrlForSeries(((SeriesModel) model).getSeriesInstanceUID());
                }
                break;
            case AbstractDicomModel.INSTANCE_LEVEL:
            	// WEB-1109: if we want to open a teaching-file (i.e referenced images) within
            	// an external viewer but the viewer doesn't support SR parsing/loading, just
            	// open whole study
            	if ( model instanceof TCModel && 
            			!provider.supportStructuredReport() )
            	{
            		if ( provider.supportStudyLevel() )
            		{
            			return provider.getUrlForStudy( ((TCModel)model).getStudyInstanceUID() );
            		}
            	}
                if (provider.supportInstanceLevel()) {
                    String iuid = ((InstanceModel) model).getSOPInstanceUID();
                    String cuid = ((InstanceModel) model).getSopClassUID();
                    if (UID.KeyObjectSelectionDocumentStorage.equals(cuid) && provider.supportKeySelectionObject()) {
                        return provider.getUrlForKeyObjectSelection(iuid);
                    }
                    int type = WebCfgDelegate.getInstance().checkCUID(cuid);
                    if (type == 1 && provider.supportStructuredReport()) {
                        return provider.getUrlForStructuredReport(iuid);
                    } else if (type == 5 && provider.supportPresentationState()) {
                        return provider.getUrlForPresentationState(iuid);
                    }
                    return provider.getUrlForInstance(iuid);
                }
                break;
            default:
                log.warn("Level of Model not supported by this Webviewer Selection Page! model:"+
                        model.getClass().getName()+" level:"+model.levelOfModel());
                return null;
        }
        log.debug("WebviewerProvider {} doesn't support DICOM model with level:{}", provider.getName(), model.levelOfModel());
        return null;
    }
    
    public static interface WebviewerLinkClickedCallback extends Serializable {
    	public void linkClicked(AjaxRequestTarget target);
    }
}
