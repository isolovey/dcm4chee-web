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

package org.dcm4chee.web.war.tc.imageview;


import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.image.resource.LocalizedImageResource;
import org.apache.wicket.model.IModel;
import org.dcm4chee.icons.ImageManager;
import org.dcm4chee.web.war.common.WadoImage;
import org.dcm4chee.web.war.folder.delegate.WADODelegate;

/**
 * @author Bernhard Ableitinger <bernhard.ableitinger@agfa.com>
 * @version $Revision$ $Date$
 * @since Nov 30, 2011
 */
public class TCWadoImage extends WebComponent 
{
        private static final long serialVersionUID = 1L;

        private TCWadoImageSize requestedSize;
        private TCWadoImageSize renderedSize;
        
        private LocalizedImageResource emptyImage;
        
        public TCWadoImage(final String id, IModel<TCImageViewImage> model, TCWadoImageSize requestedSize, TCWadoImageSize renderedSize) {
            super(id, model);
            setOutputMarkupId(true);
            this.requestedSize = requestedSize;
            this.renderedSize = renderedSize;
        }
        
        public TCWadoImage(final String id, IModel<TCImageViewImage> model, TCWadoImageSize size) {
            this(id, model, size, size);
        }
        
        public TCWadoImage(final String id, IModel<TCImageViewImage> model) {
            this(id, model, null, null);
        }
        
        @SuppressWarnings("unchecked")
        public IModel<TCImageViewImage> getModel()
        {
            return (IModel<TCImageViewImage>) getDefaultModel();
        }
        
        public TCWadoImageSize getRequestedSize()
        {
            return requestedSize;
        }
        
        public TCWadoImageSize getRenderedSize()
        {
            return renderedSize;
        }
        
        protected LocalizedImageResource createEmptyImage() {
        	LocalizedImageResource emptyImage = new LocalizedImageResource(this);
        	emptyImage.setResourceReference(ImageManager.IMAGE_TC_IMAGE_PLACEHOLDER);
        	return emptyImage;
        }
        
        private LocalizedImageResource getEmptyImage() {
        	if (this.emptyImage==null) {
        		this.emptyImage = createEmptyImage();
        	}
        	return emptyImage;
        }

        /**
         * @see org.apache.wicket.Component#onComponentTag(ComponentTag)
         */
        @Override
        protected void onComponentTag(final ComponentTag tag) {
                checkComponentTag(tag, "img");
                
                super.onComponentTag(tag);

                //set src-attribute
                if (isValid(getModel()))
                {
                    tag.put("src", getWadoUrl(requestedSize));
                }
                else
                {
                	LocalizedImageResource emptyImage = getEmptyImage();
                	if (emptyImage!=null) {
                		emptyImage.setSrcAttribute(tag);
                	}
                }

                //set width attribute, if available
                if (renderedSize!=null)
                {
                    int width = renderedSize.getWidth();
                    int height = renderedSize.getHeight();
                    if (width>0)
                    {
                        tag.put("width", width);
                    }
                    if (height>0)
                    {
                        tag.put("height", height);
                    }
                }
        }

        /**
         * @see org.apache.wicket.Component#getStatelessHint()
         */
        @Override
        protected boolean getStatelessHint() {
                return false;
        }

        /**
         * @see org.apache.wicket.Component#onComponentTagBody(MarkupStream, ComponentTag)
         */
        @Override
        protected void onComponentTagBody(final MarkupStream markupStream, final ComponentTag openTag) {
        }
        
        protected final String getWadoUrl(TCWadoImageSize requestedSize)
        {
            TCImageViewImage image = getModel()!=null?getModel().getObject():null;
            
            if (image!=null)
            {
                return getWadoUrl(image.getSeries().getStudy().getStudyUID(),
                        image.getSeries().getSeriesUID(), 
                        image.getInstanceUID(),
                        image.getFrameNumber(),
                        requestedSize);
            }
            
            return null;
        }
        
        public boolean isValid(IModel<TCImageViewImage> model)
        {
            if (model!=null)
            {
                TCImageViewImage image = model.getObject();
                
                if (image!=null)
                {
                    if (image.getClassUID()!=null && image.getInstanceUID()!=null && 
                        image.getSeries()!=null && image.getSeries().getStudy()!=null)
                    {
                        return WADODelegate.getInstance().getRenderType(
                            image.getClassUID()) == WADODelegate.IMAGE;
                    }
                }
            }
            return false;
        }
        
        public static String getWadoUrl(String stuid, String suid, String iuid)
        {
        	return getWadoUrl(stuid, suid, iuid, -1);
        }
        
        public static String getWadoUrl(String stuid, String suid, String iuid, int frameNumber)
        {
            return getWadoUrl(stuid, suid, iuid, frameNumber, null);
        }
        
        public static String getWadoUrl(String stuid, String suid, String iuid, int frameNumber, TCWadoImageSize requestedSize)
        {
            StringBuilder sb = new StringBuilder();
            sb.append(WadoImage.getDefaultWadoBaseUrl());
            if (sb.indexOf("?") == -1)
                sb.append("?requestType=WADO");
            sb.append("&studyUID=").append(stuid);
            sb.append("&seriesUID=").append(suid);
            sb.append("&objectUID=").append(iuid);
            
            if (frameNumber>0)
            {
            	sb.append("&frameNumber=").append(frameNumber);
            }
            
            if (requestedSize != null) {
                int width = requestedSize.getWidth();
                int height = requestedSize.getHeight();
                if (width>0)
                {
                    sb.append("&columns=").append(width);
                }
                if (height>0)
                {
                    sb.append("&rows=").append(height);                    
                }
            }
            
            return sb.toString();
        }
}
