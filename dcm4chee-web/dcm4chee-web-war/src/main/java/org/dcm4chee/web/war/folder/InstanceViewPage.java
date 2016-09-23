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

package org.dcm4chee.web.war.folder;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.resources.CompressedResourceReference;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4chee.icons.ImageManager;
import org.dcm4chee.icons.behaviours.ImageSizeBehaviour;
import org.dcm4chee.web.common.base.BaseWicketPage;
import org.dcm4chee.web.common.behaviours.TooltipBehaviour;
import org.dcm4chee.web.common.secure.SecureSessionCheckPage;
import org.dcm4chee.web.war.config.delegate.WebCfgDelegate;
import org.dcm4chee.web.war.folder.delegate.RIDDelegate;
import org.dcm4chee.web.war.folder.delegate.WADODelegate;
import org.dcm4chee.web.war.folder.model.InstanceModel;
import org.dcm4chee.web.war.folder.model.PatientModel;
import org.dcm4chee.web.war.folder.model.SeriesModel;
import org.dcm4chee.web.war.folder.model.StudyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision: 17295 $ $Date: 2012-10-18 14:16:35 +0200 (Do, 18 Okt 2012) $
 * @since 08.08.2010
 */
public class InstanceViewPage extends SecureSessionCheckPage {
    
    private static final long serialVersionUID = 1L;

    private InstanceModel instanceModel;
    private int wadoRenderType;
    
    private int height = 500;
    private WadoImage wadoImg;
    private boolean largeImg;
    
    private static Logger log = LoggerFactory.getLogger(InstanceViewPage.class);
    
    public InstanceViewPage(ModalWindow modalWindow, InstanceModel instanceModel) {
        this.instanceModel = instanceModel;
        height = modalWindow.getInitialHeight()-100;
        add(new Label("info", new StringResourceModel("folder.instanceview.info", this, null, getInfoParams(), null )));
        wadoRenderType = WADODelegate.getInstance().getRenderType(instanceModel.getSopClassUID());
        if (wadoRenderType == WADODelegate.IMAGE) {
            add(new ImageFragment("fragment", instanceModel, modalWindow.getInitialWidth()-100, height));
        } else {
            add(new MimeFragment("fragment", instanceModel));
        }
    }
            
    private Object[] getInfoParams() {
        Object[] params = new Object[10];
        SeriesModel series = instanceModel.getSeries();
        StudyModel study = series.getPPS().getStudy();
        PatientModel pat = study.getPatient();
        params[0] = pat.getName();
        params[1] = pat.getId();
        params[2] = pat.getIssuer();
        params[3] = pat.getBirthdate();
        params[4] = study.getStudyInstanceUID();
        params[5] = study.getDescription();
        params[6] = series.getSeriesInstanceUID();
        params[7] = study.getDescription();
        params[8] = instanceModel.getSOPInstanceUID();
        params[9] = instanceModel.getDescription();
        for (int i = 0 ; i < params.length ; i++) {
            if (params[i] == null && (params[i] instanceof String))
                params[i] = "";
        }
        return params;
    }

    public class ImageFragment extends Fragment {
        private static final long serialVersionUID = 1L;
        private ImageController imgCtr;
        
        public ImageFragment(String id, InstanceModel instanceModel, int width, int height) {
            super(id, "image", InstanceViewPage.this);
            imgCtr = new ImageController("imgCtr", width, height);
            add(imgCtr);
            wadoImg = new WadoImage("wadoImg", imgCtr);
            wadoImg.setOutputMarkupId(true);
            add(wadoImg);
        }
     }

    public class MimeFragment extends Fragment {
        private static final long serialVersionUID = 1L;

        private MimeController mimeCtr;

        public MimeFragment(String id, final InstanceModel instanceModel) {
            super(id, "url", InstanceViewPage.this);
            WebMarkupContainer iframe = new WebMarkupContainer("iframe") {
                private static final long serialVersionUID = 1L;
                protected void onComponentTag(ComponentTag tag) {
                    super.onComponentTag(tag);
                    log.debug("Set URL for iframe:{}",mimeCtr.getURL());
                    tag.put("src", mimeCtr.getURL());
                    tag.put("width", "100%");
                    tag.put("height", "100%");
                }
            };
            mimeCtr = new MimeController("mimeCtr", iframe);
            add(mimeCtr);
            add(iframe.setOutputMarkupId(true));
        }
     }
    
    private class WadoImage extends WebComponent {

        private static final long serialVersionUID = 1L;
        private ImageController imgCtr;

        public WadoImage(String id, ImageController ctrl) {
            super(id);
            this.imgCtr = ctrl;
        }

        protected void onComponentTag(ComponentTag tag) {
            super.onComponentTag(tag);
            checkComponentTag(tag, "img");
            String wadoUrl = imgCtr.getWadoURL();
            tag.put("src", wadoUrl);
            tag.put("title", wadoUrl);
            tag.put("alt", new ResourceModel("folder.wadoImage.alt.text").wrapOnAssignment(this).getObject());
        }
    }
    
    private class ImageController extends WebMarkupContainer {
        private static final long serialVersionUID = 1L;
        private StringBuilder baseWadoURL;
        private int baseUrlLength;
        private Model<Integer> columnsModel;
        private Model<Integer> rowsModel;
        private FrameModel frameModel;
        private Model<Boolean> origSizeModel = new Model<Boolean>(false);
        private Model<Boolean> usePngModel = new Model<Boolean>(false);
        
        public ImageController(String id, int width, int height) {
            super(id);
            initModels(width, height);
            setOutputMarkupId(true);
            add(new Link<Object>("prev") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick() {
                    frameModel.prev();
                }
                @Override
                public boolean isVisible() {
                    return frameModel.getObject() > 1;
                }
            }
            .add(new Image("prevImg", ImageManager.IMAGE_COMMON_BACK)
            .add(new ImageSizeBehaviour("vertical-align: middle;"))
            .add(new TooltipBehaviour("folder.instanceview.")))
            );
            add(new Label("frame", frameModel));
            add(new Label("nrOfFrames", String.valueOf(frameModel.nrOfFrames)));
            add(new Link<Object>("next") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick() {
                    frameModel.next();
                }
                @Override
                public boolean isVisible() {
                    return frameModel.getObject() < frameModel.nrOfFrames;
                }
            }
            .add(new Image("nextImg", ImageManager.IMAGE_COMMON_FORWARD)
            .add(new ImageSizeBehaviour("vertical-align: middle;"))
            .add(new TooltipBehaviour("folder.instanceview.")))
            );
            add(new Label("origLabel", new ResourceModel("folder.instanceview.origLabel"))
                    .setVisible(largeImg));
            add(new AjaxCheckBox("orig", origSizeModel){
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    target.addComponent(wadoImg);
                }
                
            }.add(new TooltipBehaviour("folder.instanceview.")).setVisible(largeImg));
            add(new Label("pngLabel", new ResourceModel("folder.instanceview.pngLabel")));
            add(new AjaxCheckBox("png", usePngModel){
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    target.addComponent(wadoImg);
                }
                
            }.add(new TooltipBehaviour("folder.instanceview.")));
        }

        private void initModels(int width, int height) {
            baseWadoURL = new StringBuilder(WADODelegate.getInstance().getURL(instanceModel));
            baseUrlLength = baseWadoURL.length();
            DicomObject obj = instanceModel.getDataset();
            Integer columns = obj.getInt(Tag.Columns);
            Integer rows = obj.getInt(Tag.Rows);
            if (columns > width) {
                largeImg = true;
                columns = width;
                rows = (int) ((float)rows * (float)width /(float) columns);
            }
            if (rows > height) {
                rows = height;
                columns = null;
                largeImg = true;
            } else {
                rows = null;
            }
            columnsModel = new Model<Integer>(columns);
            rowsModel = new Model<Integer>(rows);
            frameModel = new FrameModel(1, obj.getInt(Tag.NumberOfFrames));
        }

        public String getWadoURL() {
            baseWadoURL.setLength(baseUrlLength);
            baseWadoURL.append("&frameNumber=").append(frameModel.getObject());
            if (!origSizeModel.getObject() && rowsModel.getObject() != null)
                baseWadoURL.append("&rows=").append(rowsModel.getObject());
            if (!origSizeModel.getObject() && columnsModel.getObject() != null)
                baseWadoURL.append("&columns=").append(columnsModel.getObject());
            if (usePngModel.getObject())
                baseWadoURL.append("&contentType=image/png");
            return baseWadoURL.toString();
        }
        
    }

    private class FrameModel implements IModel<Integer> {
        private static final long serialVersionUID = 1L;
        private int frameNr = 1;
        private int nrOfFrames;
        
        private FrameModel(int frameNr, int nrOfFrames) {
            this.nrOfFrames = nrOfFrames < 1 ? 1 : nrOfFrames;
            setObject(frameNr);
        }
        
        public void prev() {
            if (frameNr > 1)
                frameNr--;
        }
        public void next() {
            if (frameNr < nrOfFrames)
                frameNr++;
        }

        public void setObject(Integer i) {
            frameNr = (i > nrOfFrames) ? nrOfFrames : (i < 1) ? 1 : i;
        }

        public void detach() {}

        public Integer getObject() {
            return frameNr;
        }
    }

    private class MimeController extends WebMarkupContainer {
        private static final String MIME_DEFAULT = "<default>";

        private static final long serialVersionUID = 1L;
        
        private Model<String> mimeModel = new Model<String>(null);
        
        public MimeController(String id, final Component iframe) {
            super(id);
            List<String> mimeTypes = new ArrayList<String>();
            mimeTypes.add(MIME_DEFAULT);
            List<String> ridMimes = WebCfgDelegate.getInstance().getRIDMimeTypes(instanceModel.getSopClassUID());
            if (ridMimes != null)
                mimeTypes.addAll(ridMimes);
            DropDownChoice<String> mimeSelector = new DropDownChoice<String>("mimeTypes", mimeModel, mimeTypes);
            mimeSelector.setDefaultModelObject(wadoRenderType == WADODelegate.NOT_RENDERABLE && mimeTypes.size() > 1 ? 
                    mimeTypes.get(1) : MIME_DEFAULT);
            mimeSelector.add(new AjaxFormComponentUpdatingBehavior("onchange") {
                private static final long serialVersionUID = 1L;

                protected void onUpdate(AjaxRequestTarget target) {
                     target.addComponent(iframe);
                }
            });
            add(new Label("contentTypeLabel", new ResourceModel("folder.instanceview.contentTypeLabel")));
            add(mimeSelector);
            setOutputMarkupId(true);
        }

        public String getURL() {
            String mime = mimeModel.getObject();
            if (MIME_DEFAULT.equals(mime)) {
                return WADODelegate.getInstance().getURL(instanceModel);
            } else {
                try {
                    mime = URLEncoder.encode(mime, "UTF-8");
                } catch (UnsupportedEncodingException ignore) {}
                return RIDDelegate.getInstance().getURL(instanceModel, mime);
            }
        }
        
    }
}
