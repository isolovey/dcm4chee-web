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

package org.dcm4chee.web.war.common;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.model.IModel;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4chee.web.war.folder.model.InstanceModel;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 * @version $Revision: 15679 $ $Date: 2011-07-12 15:37:33 +0200 (Di, 12 Jul 2011) $
 * @since Jul 05, 2010
 */
public class WadoImage extends WebComponent {
        private static final long serialVersionUID = 1L;
        
        private static String defaultWadoBaseUrl = "/wado?requestType=WADO&amp;";
        
        private String studyIuid, seriesIuid, sopIuid;
        private IModel<String> baseURLModel;
        private IModel<Integer> imgWidthModel;

        public WadoImage(final String id, InstanceModel instance) {
            super(id);
            sopIuid = instance.getSOPInstanceUID();
            seriesIuid = instance.getSeries().getSeriesInstanceUID();
            studyIuid = instance.getSeries().getPPS().getStudy().getStudyInstanceUID();
        }
        public WadoImage(final String id, InstanceModel instance, IModel<Integer> imgWidthModel) {
            this(id, instance);
            this.imgWidthModel = imgWidthModel;
        }
        
        public WadoImage(final String id, DicomObject attrs) {
            super(id);
            sopIuid = attrs.getString(Tag.SOPInstanceUID);
            seriesIuid = attrs.getString(Tag.SeriesInstanceUID);
            studyIuid = attrs.getString(Tag.StudyInstanceUID);
        }
        
        protected void checkUids() {
            if (sopIuid == null || seriesIuid == null || studyIuid == null) {
                throw new IllegalArgumentException("WADO Image must have Study-, Series- and Instance UID!");
            }
        }
        
        public static String getDefaultWadoBaseUrl() {
            return defaultWadoBaseUrl;
        }

        public static void setDefaultWadoBaseUrl(String defaultWadoBaseUrl) {
            WadoImage.defaultWadoBaseUrl = defaultWadoBaseUrl;
        }

        /**
         * @see org.apache.wicket.Component#initModel()
         */
        @Override
        protected IModel<?> initModel() {
                return null;
        }

        /**
         * @see org.apache.wicket.Component#onComponentTag(ComponentTag)
         */
        @Override
        protected void onComponentTag(final ComponentTag tag) {
                checkComponentTag(tag, "img");
                super.onComponentTag(tag);
                StringBuilder sb = new StringBuilder();
                sb.append(baseURLModel == null ? defaultWadoBaseUrl : baseURLModel.getObject());
                if (sb.indexOf("?") == -1)
                    sb.append("?requestType=WADO");
                sb.append("&amp;studyUID=").append(studyIuid);
                sb.append("&amp;seriesUID=").append(seriesIuid);
                sb.append("&amp;objectUID=").append(sopIuid);
                if (imgWidthModel != null) {
                    sb.append("&amp;columns=").append(imgWidthModel.getObject());
                }
                tag.put("src", sb.toString());
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
        public WadoImage(final String id, String iuid, String suid, String stuid, IModel<Integer> imgWidthModel)
        {
            super(id);
            this.sopIuid = iuid;
            this.seriesIuid = suid;
            this.studyIuid = stuid;
            this.imgWidthModel = imgWidthModel;
        }
}
