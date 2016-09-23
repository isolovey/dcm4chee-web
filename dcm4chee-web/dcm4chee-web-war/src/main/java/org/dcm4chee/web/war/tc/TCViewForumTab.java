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

import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.dcm4chee.web.war.config.delegate.WebCfgDelegate;
import org.dcm4chee.web.war.tc.TCViewPanel.AbstractEditableTCViewTab;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Bernhard Ableitinger <bernhard.ableitinger@agfa.com>
 * @version $Revision$ $Date$
 * @since Nov 25, 2011
 */
@SuppressWarnings("serial")
public class TCViewForumTab extends AbstractEditableTCViewTab 
{
	private static final Logger log = LoggerFactory.getLogger(TCViewForumTab.class);
        
    public TCViewForumTab(final String id, final IModel<TCEditableObject> model, 
    		TCAttributeVisibilityStrategy attrVisibilityStrategy) {
        super(id, model, attrVisibilityStrategy);

        add(new TCForumPostsPanel("forum-container", new AbstractReadOnlyModel<String>() {
        	String lastUID=null;
        	String lastURL=null;
        	@Override
        	public String getObject() {
        		TCEditableObject o = model.getObject();
        		String uid = o!=null ? o.getInstanceUID() : null;
        		if (!TCUtilities.equals(uid, lastUID)) {
        			lastURL = null;
        			if (uid!=null) {
        				try {
        					TCForumIntegration impl = TCForumIntegration.get(
        							WebCfgDelegate.getInstance().getTCForumIntegrationType());
        					if (impl!=null) {
		         				lastURL = TCForumIntegration.JForum.getPostsPageURL(o);
		           				lastUID = uid;
        					}
        				}
        				catch (Exception e) {
        					log.error(null, e);
        				}
        			}
        		}
        		return lastURL;
        	}
        }));
    }

    @Override
    public String getTabTitle()
    {
        return getString("tc.view.forum.tab.title");
    }
    
    @Override
    public boolean hasContent()
    {
        return true;
    }
    
    @Override
    public boolean isTabVisible() {
    	if (super.isTabVisible()) {
    		if (TCForumIntegration.get(WebCfgDelegate.getInstance().
    				getTCForumIntegrationType())!=null) {
    			return getAttributeVisibilityStrategy()
    				.isAttributeVisible(TCAttribute.Discussion);
    		}
    	}
    	
    	return false;
    }
    
    @Override
    protected void saveImpl()
    {
    }
}
