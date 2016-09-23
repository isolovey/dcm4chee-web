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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.navigation.paging.PagingNavigator;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.GridView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.dcm4chee.icons.ImageManager;
import org.dcm4chee.web.war.common.WadoImage;
import org.dcm4chee.web.war.folder.delegate.WADODelegate;

/**
 * @author Bernhard Ableitinger <bernhard.ableitinger@agfa.com>
 * @version $Revision$ $Date$
 * @since May 27, 2011
 */
@SuppressWarnings("serial")
public class TCDetailsImagesTab extends TCDetailsTab {

    private WebMarkupContainer navWmc;

    public TCDetailsImagesTab(final String id, 
    		TCAttributeVisibilityStrategy attrVisibilityStrategy) {
        super(id, attrVisibilityStrategy);

        WadoImage.setDefaultWadoBaseUrl(WADODelegate.getInstance()
                .getWadoBaseUrl());

        final IModel<Integer> width = new Model<Integer>(64);
        final int cols = 5;
        final int rows = 5;

        final ReferencedImageProvider imageProvider = new ReferencedImageProvider();
        GridView<TCReferencedImage> view = new GridView<TCReferencedImage>(
                "details-image-row", imageProvider) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final Item<TCReferencedImage> item) {
                TCReferencedInstance ref = item.getModelObject();

                if (WADODelegate.getInstance().getRenderType(ref.getClassUID()) == WADODelegate.IMAGE) {
                    item.add(
                            new WadoImage("image", ref.getInstanceUID(), ref
                                    .getSeriesUID(), ref.getStudyUID(), width))
                            .setOutputMarkupId(true);
                } else {
                    item.add(new Image("image",
                            ImageManager.IMAGE_TC_IMAGE_PLACEHOLDER)
                            .setOutputMarkupId(true));
                }
            }

            @Override
            protected void populateEmptyItem(final Item<TCReferencedImage> item) {
                item.add(new Image("image",
                        ImageManager.IMAGE_TC_IMAGE_PLACEHOLDER)
                        .setOutputMarkupId(true));
            }
        };

        view.setColumns(cols);
        view.setRows(rows);

        navWmc = new WebMarkupContainer("details-images-nav-container") {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                int nImages = imageProvider.size();
                return nImages > cols * rows;
            }
        };
        navWmc.add(new PagingNavigator("details-images-nav", view));
        navWmc.setOutputMarkupPlaceholderTag(true);
        navWmc.setOutputMarkupId(true);
        navWmc.setMarkupId("details-images-nav-container");
        
        add(navWmc);
        add(view);
    }
    
    @Override
    public boolean enabled() {
        TCObject tc = getTCObject();
        return tc != null && tc.getReferencedImages() != null
                && !tc.getReferencedImages().isEmpty();
    }
    
    @Override
    public boolean visible() {
    	return getAttributeVisibilityStrategy().isAttributeVisible(
    			TCAttribute.Images);
    }

    private TCObject getTCObject() {
        return (TCObject) getDefaultModelObject();
    }

    public class ReferencedImageProvider implements IDataProvider<TCReferencedImage> {

        private static final long serialVersionUID = 1L;

        @SuppressWarnings({ "unchecked" })
        @Override
        public Iterator<TCReferencedImage> iterator(int first, int count) {
            List<TCReferencedImage> images = getTCObject().getReferencedImages();
            if (images != null) {
                return images.subList(first, first + count).iterator();
            } else {
                return (Iterator) Collections.emptyList().iterator();
            }
        }

        @Override
        public int size() {
            List<TCReferencedImage> images = getTCObject().getReferencedImages();
            return images != null ? images.size() : 0;
        }

        @Override
        public IModel<TCReferencedImage> model(TCReferencedImage object) {
            return new Model<TCReferencedImage>(object);
        }

        @Override
        public void detach() {
        }
    }
}
