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

package org.dcm4chee.web.common.secure;

import org.apache.wicket.IPageMap;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.Session;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.JavascriptPackageResource;
import org.apache.wicket.markup.html.resources.CompressedResourceReference;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.security.components.SecureWebPage;
import org.dcm4chee.web.common.base.BaseWicketPage;

/**
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision$ $Date$
 * @since 01.09.2010
 */
public class SecureSessionCheckPage extends SecureWebPage {

    public static final String SYSTEM_PROPERTY_READINGROOM = "org.dcm4chee.web.readingroom";
    public static final ResourceReference BASE_CSS = new CompressedResourceReference(BaseWicketPage.class, "base-style.css");
    public static final ResourceReference BASE_CSS_R = new CompressedResourceReference(BaseWicketPage.class, "base-style-r.css");
    
    public static final MetaDataKey<Model<ResourceReference>> BASE_CSS_MODEL_MKEY = 
        new MetaDataKey<Model<ResourceReference>>() {
            private static final long serialVersionUID = 1L;
    };

    public SecureSessionCheckPage() {
        super();
        init();
    }
    
    public SecureSessionCheckPage(PageParameters params) {
    	super(params);
    	init();
    }

    public SecureSessionCheckPage(IPageMap pageMap) {
        super(pageMap);
        init();
    }

    public static IModel<ResourceReference> getBaseCSSModel() {
        IModel<ResourceReference> cssModel = Session.get().getMetaData(BASE_CSS_MODEL_MKEY);
        if (cssModel == null) {
            cssModel = new Model<ResourceReference>(Boolean.parseBoolean(System.getProperty(SYSTEM_PROPERTY_READINGROOM)) ? 
                    BASE_CSS_R : BASE_CSS);
            Session.get().setMetaData(BASE_CSS_MODEL_MKEY, cssModel);
        }
        return cssModel;
    }
    
    public static HeaderContributor getBaseCSSHeaderContributor() {
        return new HeaderContributor(new IHeaderContributor() {
                private static final long serialVersionUID = 1L;

                public void renderHead(IHeaderResponse response) {
                    response.renderCSSReference(getBaseCSSModel().getObject());
                }
        });
    }
    
    private void init() {
        add(getBaseCSSHeaderContributor());
        add(JavascriptPackageResource.getHeaderContribution(BaseWicketPage.class, "web3-utils.js"));
    }
}
