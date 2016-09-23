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

import org.apache.wicket.Component;
import org.apache.wicket.PageParameters;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.RequestUtils;
import org.apache.wicket.util.time.Duration;
import org.dcm4chee.archive.entity.Instance;
import org.dcm4chee.archive.util.JNDIUtils;
import org.dcm4chee.web.common.base.ModuleSelectorPanel;
import org.dcm4chee.web.common.secure.SecureSessionCheckPage;
import org.dcm4chee.web.dao.tc.TCQueryLocal;
import org.dcm4chee.web.war.StudyPermissionHelper;
import org.dcm4chee.web.war.tc.TCPopupManager.ITCPopupManagerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Bernhard Ableitinger <bernhard.ableitinger@agfa.com>
 * @since Jul 02, 2013
 */
public class TCCaseViewPage extends SecureSessionCheckPage 
	implements ITCPopupManagerProvider
{    
    protected static Logger log = LoggerFactory.getLogger(TCCaseViewPage.class);

    private TCPopupManager popupManager;
    
    @SuppressWarnings("serial")
	public TCCaseViewPage(PageParameters params) {
        super(params);
        
        if (StudyPermissionHelper.get().isSSO())
        {
            add(new AbstractAjaxTimerBehavior(Duration.milliseconds(1)) {
                    private static final long serialVersionUID = 1L;
        
                    @Override
                    protected void onTimer(AjaxRequestTarget arg0) {
                        try {
                            StudyPermissionHelper.get().doDicomAuthentication();
                        } catch (Exception e) {
                            log.error(getClass().getName() + ": error doing dicom authentication: ", e);
                        } finally {
                            this.stop();
                        }
                    }
                }
            );
        }
        
        add(TCEnvironment.getCSSHeaderContributor()); 
        
        add((popupManager=new TCPopupManager()).getGlobalHideOnOutsideClickHandler());
        
        add(new Label("tc-casepage-title", TCUtilities.getLocalizedString("tc.casepage.title")));
        
        String uid = params.getString("uid");
        if (uid!=null) {
            TCQueryLocal dao = (TCQueryLocal) JNDIUtils
                    .lookup(TCQueryLocal.JNDI_NAME);
            
            Instance instance = dao.findInstanceByUID(uid);
            
	        final Image logo = new Image("img_logo", new ResourceReference(ModuleSelectorPanel.class, 
	                "images/logo.gif"));
	        logo.setOutputMarkupId(true);
	        logo.setVisible(false);
	        add(logo);
	        
            if (instance!=null) {
		        try {
		        	final TCViewPanel panel = new TCViewPanel("tc-case", new TCModel(instance),
			        		new TCAttributeVisibilityStrategy(new Model<Boolean>(false)) {
		        		@Override
		        		public boolean isAttributeVisible(TCAttribute attr) {
		        			if (TCEnvironment.isPrincipalAuthorized("TCViewRestricted")) {
		        				return true;
		        			}

		        			return !attr.isRestricted();
		        		}
		        	}, null);
		        	
			        add( panel );
			        
			        add(getErrorComponent(null));
			        
			        logo.setVisible(true);
			        
			        add(new HeaderContributor(new IHeaderContributor()
					{
						public void renderHead(IHeaderResponse response)
						{		        
							StringBuilder js = new StringBuilder();
							js.append("updateTCViewDialog(true);");
							js.append(panel.getDisableTabsJavascript());
			        		js.append(panel.getHideTabsJavascript());
			        		
					        response.renderOnDomReadyJavascript(js.toString());
						}
					}));
		        }
		        catch (Exception e) {
		        	add(new WebMarkupContainer("tc-case").setVisible(false));
		        	add(getErrorComponent(TCUtilities.getLocalizedString(
		        			"tc.casepage.error.text")));
		        	log.error(null, e);
		        }
            }
            else {
            	add(new WebMarkupContainer("tc-case").setVisible(false));
            	add(getErrorComponent(MessageFormat.format(
            			TCUtilities.getLocalizedString("tc.casepage.casenotfound.text"),uid)));
            }
        }
        else {
        	add(new WebMarkupContainer("tc-case").setVisible(false));
        	add(getErrorComponent(TCUtilities.getLocalizedString(
        			"tc.casepage.casenotspecified.text")));
        }
    }
    
    @Override
    public TCPopupManager getPopupManager() {
    	return popupManager;
    }
    
    public static String urlForCase(String uid) {
    	PageParameters params = new PageParameters();
    	params.put("uid", uid);
    	return RequestUtils.toAbsolutePath(
    			RequestCycle.get().urlFor(
    					TCCaseViewPage.class, params).toString());
    }
    
    @SuppressWarnings("serial")
	private Component getErrorComponent(final String errorMsg) {
    	return new WebMarkupContainer("tc-casepage-error") {
    		@Override
    		public boolean isVisible() {
    			return errorMsg!=null;
    		}
    	}
    	.add(new Label("tc-casepage-error-text", new Model<String>(errorMsg)));
    }
}
