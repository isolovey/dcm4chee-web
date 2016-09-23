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

import org.apache.wicket.PageMap;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.PopupSettings;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.ResourceModel;
import org.dcm4chee.web.common.secure.SecureSessionCheckPage;
import org.dcm4chee.web.common.webview.link.WebviewerLinkProvider;
import org.dcm4chee.web.war.common.model.AbstractDicomModel;
import org.dcm4chee.web.war.folder.webviewer.Webviewer.WebviewerLinkClickedCallback;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 * @version $Revision: 18214 $ $Date: 2014-01-28 12:11:10 +0100 (Di, 28 Jän 2014) $
 * @since May 24, 2011
 */
public class WebviewerSelectionPage extends SecureSessionCheckPage {

    public WebviewerSelectionPage(AbstractDicomModel model, WebviewerLinkProvider[] providers, final ModalWindow modalWindow,
    		final WebviewerLinkClickedCallback callback) {
        super();        
        add(new Label("header", new ResourceModel("webviewer.selection.header")));
        add(new Label("info", model.toString()));
        RepeatingView rv = new RepeatingView("repeater");
        add(rv);
        WebMarkupContainer mc;
        for (int i = 0 ; i < providers.length ; i++) {
            final WebviewerLinkProvider provider = providers[i];
            final String url = Webviewer.getUrlForModel(model, provider);
            if (url != null) {
                mc = new WebMarkupContainer(rv.newChildId());
                ExternalLink link = new ExternalLink("link", url, provider.getName());
                if (!provider.hasOwnWindow()) {
                    link.setPopupSettings(new PopupSettings(PageMap.forName(provider.getName()), 
                        PopupSettings.RESIZABLE|PopupSettings.SCROLLBARS));
                }
                if (modalWindow != null || callback!=null) {
                    link.add(new AjaxEventBehavior("onclick") {
                        private static final long serialVersionUID = 1L;
                        @Override
                        protected void onEvent(AjaxRequestTarget target)
                        {
                        	if (modalWindow!=null) {
                        		modalWindow.close(target);
                        	}
                            if (callback!=null) {
                            	callback.linkClicked(target);
                            }
                        }
                    	@Override
                    	protected CharSequence generateCallbackScript(CharSequence partialCall) {
                    		CharSequence script = super.generateCallbackScript(partialCall);
                    		if (provider.hasOwnWindow()) {
                    			script = Webviewer.getSendHttpRequestJavascript(url)+script+"return false;";
                    		}
                    		return script;
                    	}

                    });
                }

                mc.add(link);
                rv.add(mc);
            }
        }
    }

}
