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

package org.dcm4chee.web.common.markup;

import org.apache.wicket.IPageMap;
import org.apache.wicket.Page;
import org.apache.wicket.PageMap;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.link.PopupSettings;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 * @version $Revision$ $Date$
 * @since Jan 13, 2010
 */
public abstract class PopupLink extends Link<Object> {

    private static final long serialVersionUID = 1L;

    private String popupPageName = "popupPage";
    private int popupHeight = 300;
    private int popupWidth = 500;
    private int popupDisplayFlag = PopupSettings.RESIZABLE | PopupSettings.SCROLLBARS;
    
    private static Logger log = LoggerFactory.getLogger(PopupLink.class);
    
    public PopupLink(String id, String popupPageName) {
        super(id);
        init(popupPageName);
    }

    public PopupLink(String id, IModel<Object> model, String pageMapName) {
        super(id, model);
        init(pageMapName);
    }
    
    public PopupLink(String id, Page targetPageInstance, String targetPageName) {
        this(id, targetPageName);
    }

    public void setPopupHeight(int popupHeight) {
        this.popupHeight = popupHeight;
    }

    public void setPopupWidth(int popupWidth) {
        this.popupWidth = popupWidth;
    }

    public void setPopupDisplayFlags(int popupDisplayFlag) {
        this.popupDisplayFlag = popupDisplayFlag;
    }
    
    public void setPopupDisplayFlag(int flag, boolean b) {
        this.popupDisplayFlag = b ?
                popupDisplayFlag | flag : popupDisplayFlag & ~flag;
    }

    private void init(String pageMapName) {
        if ( popupPageName != null ) 
            popupPageName = pageMapName;

        PopupSettings popupSettings = new MultiPopupSettings(PageMap.forName(popupPageName));
        super.setPopupSettings(popupSettings);
        add(new MultiPopupBehaviour());
    }

    private class MultiPopupSettings extends PopupSettings {
        
        private static final long serialVersionUID = 1L;

        public MultiPopupSettings(IPageMap pageMap) {
            super(pageMap);
        }

        @Override
        public String getPopupJavaScript() {
            String windowTitle = popupPageName;
            // Fix for IE bug.
            windowTitle = windowTitle.replaceAll("\\W", "_");

            StringBuffer script = new StringBuffer("var w = window.open('" 
                    + getURL()
                    + "', '").
              append(windowTitle).append("_'+popup_count, '");
            script.append("scrollbars=").append(flagToString(SCROLLBARS));
            script.append(",location=").append(flagToString(LOCATION_BAR));
            script.append(",menuBar=").append(flagToString(MENU_BAR));
            script.append(",resizable=").append(flagToString(RESIZABLE));
            script.append(",status=").append(flagToString(STATUS_BAR));
            script.append(",toolbar=").append(flagToString(TOOL_BAR));
            if (popupWidth != -1)
                    script.append(",width=").append(popupWidth);
            if (popupHeight != -1)
                    script.append(",height=").append(popupHeight);
            script.append("'); if(w.blur) w.focus(); popup_count++; return false;");
            return script.toString();            
        }
        
        private String flagToString(final int flag) {
                return (popupDisplayFlag & flag) != 0 ? "yes" : "no";
        }
    }
    
    private class MultiPopupBehaviour extends AbstractBehavior implements IHeaderContributor {

        private static final long serialVersionUID = 1L;

        @Override
        public void renderHead(IHeaderResponse response) {
            try {
                response.renderJavascript("var popup_count=1;", "PopupLink");
            } catch (Exception e) {
                log.error("Error render Header with 'PopupLink Javascript'");
            }
         }
    }
}
