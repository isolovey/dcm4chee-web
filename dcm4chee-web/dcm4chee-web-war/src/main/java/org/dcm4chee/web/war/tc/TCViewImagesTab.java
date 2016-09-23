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
import java.util.List;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.ListModel;
import org.dcm4chee.web.war.config.delegate.WebCfgDelegate;
import org.dcm4chee.web.war.tc.TCViewPanel.AbstractTCViewTab;
import org.dcm4chee.web.war.tc.imageview.TCImageViewPanel;

/**
 * @author Bernhard Ableitinger <bernhard.ableitinger@agfa.com>
 * @version $Revision$ $Date$
 * @since Nov 30, 2011
 */
@SuppressWarnings("serial")
public class TCViewImagesTab extends AbstractTCViewTab 
{
    public TCViewImagesTab(final String id, IModel<? extends TCObject> model, 
    		TCAttributeVisibilityStrategy attrVisibilityStrategy) {
        super(id, model, attrVisibilityStrategy);
        add(new TCImageViewPanel("tc-view-images-panel", 
                new ImagesModel()).setOutputMarkupId(true));
    }
    
    @Override
    public String getTabTitle()
    {
        List<TCReferencedImage> images = getTC().getReferencedImages();
        return MessageFormat.format(
                getString("tc.view.images.tab.title"),
                images!=null?images.size():0);
    }
    
    @Override
    public boolean isTabVisible() {
    	if (super.isTabVisible()) {
    		if (WebCfgDelegate.getInstance().isTCShowImagesInDialogEnabled()) {
    			return getAttributeVisibilityStrategy()
    					.isAttributeVisible(TCAttribute.Images);
    		}
    	}
    	
    	return false;
    }
    
    @Override
    public boolean hasContent()
    {
        List<TCReferencedImage> images = getTC().getReferencedImages();
        return images!=null && !images.isEmpty();
    }

    @SuppressWarnings("serial")
	private class ImagesModel extends ListModel<TCReferencedStudy> {
    	@Override
    	public List<TCReferencedStudy> getObject() {
    		return getTC().getReferencedStudies();
    	}
    }

}
