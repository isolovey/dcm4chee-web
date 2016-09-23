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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.io.ContentHandlerAdapter;
import org.dcm4chee.archive.conf.AttributeFilter;
import org.dcm4chee.web.common.util.FileUtils;
import org.dcm4chee.web.war.config.delegate.WebCfgDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;


/**
 * @author Franz Willer <franz.willer@gmail.com>
 * @version $Revision: 16034 $ $Date: 2011-10-03 13:18:27 +0200 (Mo, 03 Okt 2011) $
 * @since March 30, 2011
 */
public class EditableDicomAttributes implements Serializable {
    
    private static final long serialVersionUID = 1L;
    private static final String BASE_ATTR_MODEL_NAME = "BASE";
    private static Logger log = LoggerFactory.getLogger(EditableDicomAttributes.class);
    
    private String attrModelName;
    private static Map<String,DicomObject> allEditableAttrs = new HashMap<String,DicomObject>();
    private static Map<String,Long> fileLastModified = new HashMap<String, Long>();
    private DicomObject editableAttrs;
    private boolean useNullForRemoveTag;
    private boolean allowAll;
    
    public EditableDicomAttributes(String attrModelName) {
        prepare(attrModelName == null ? BASE_ATTR_MODEL_NAME : attrModelName);
        useNullForRemoveTag = "PatientModel".equals(attrModelName) || "StudyModel".equals(attrModelName) || 
        "SeriesModel".equals(attrModelName) || "InstanceModel".equals(attrModelName);
    }

    private void prepare(String attrModelName) {
        this.attrModelName = attrModelName;
        load(attrModelName);
        editableAttrs = allEditableAttrs.get(attrModelName);
        if (editableAttrs == null && !BASE_ATTR_MODEL_NAME.equals(attrModelName)) {
            editableAttrs = load(BASE_ATTR_MODEL_NAME);
        }
        log.info("Select Attribute filter for modeName:"+attrModelName);
        AttributeFilter filter = getAttributeFilter(attrModelName);
            log.info("selected Attribute Filter:"+filter);
        if (editableAttrs != null && filter != null) {
            editableAttrs = filter.filter(editableAttrs);
        }
        log.info("(filtered) Editable DICOM attributes for modelName "+attrModelName+":"+editableAttrs);
    }

    private AttributeFilter getAttributeFilter(String attrModelName) {
        AttributeFilter filter = "PatientModel".equals(attrModelName) ? AttributeFilter.getPatientAttributeFilter() :
            "StudyModel".equals(attrModelName) ? AttributeFilter.getStudyAttributeFilter() :
            "SeriesModel".equals(attrModelName) ? AttributeFilter.getSeriesAttributeFilter() :
            "InstanceModel".equals(attrModelName) ? AttributeFilter.getInstanceAttributeFilter(null) :
            "PPSModel".equals(attrModelName) ? AttributeFilter.getExcludePatientAttributeFilter() :
            "MWLItemModel".equals(attrModelName) ? AttributeFilter.getExcludePatientAttributeFilter() :
            null;
        return filter;
    }
    
    public static DicomObject load(String attrModelName) {
        String fn = "dicomedit"+File.separatorChar+attrModelName.toLowerCase()+".xml";
        File f = FileUtils.resolve(new File(WebCfgDelegate.getInstance().getWebConfigPath(), fn));
        InputStream is = null;
        try {
            if (f.isFile()) {
                Long lastModified = fileLastModified.get(f.getPath());
                if ( lastModified != null && f.lastModified() == lastModified) {
                    return allEditableAttrs.get(attrModelName);
                }
                fileLastModified.put(f.getPath(), f.lastModified());
                is = new FileInputStream(f);
            } else {
                if (fileLastModified.containsKey(f.getPath()) ) {
                    fileLastModified.remove(f.getPath());
                } else if (allEditableAttrs.containsKey(attrModelName)) {
                    return allEditableAttrs.get(attrModelName);
                }
                is = EditableDicomAttributes.class.getResourceAsStream(fn);
            }
        } catch (FileNotFoundException ignore) {}
        if (is != null) {
            DicomObject attrs = new BasicDicomObject();
            try {
                loadDicomObject(is, attrs);
                allEditableAttrs.put(attrModelName, attrs);
                log.info("'Editable DICOM attributes' config file loaded:"+
                        (f.isFile() ? f.getAbsolutePath() : EditableDicomAttributes.class.getResource(fn).toString()));
                log.debug("Editable DICOM attributes for modelName "+attrModelName+":"+attrs.toString());
                return attrs;
            } catch (Exception x) {
                log.error("Failed: read config file for editable dicom Attributes! :"+f, x);
            }
        } else {
            log.debug("No configuration of Editable DICOM attributes for modelName "+attrModelName+" found!");
        }
        return null;
    }
    
    public static void clear() {
        allEditableAttrs.clear();
        fileLastModified.clear();
        load(BASE_ATTR_MODEL_NAME);
    }

    public boolean isEditable(int[] tagPath) {
        if (allowAll || editableAttrs == null) {
            AttributeFilter filter = getAttributeFilter(attrModelName);
            return filter == null ? true : filter.hasTag(tagPath[0]) ^ filter.isExclude();
        } else {
            return isEditable(tagPath, null);
        }
    }

    private boolean isEditable(int[] tagPath, Integer childTag) {
        int itemIdx = tagPath.length-2;
        if (itemIdx > 0) {
            tagPath[itemIdx] = 0;
        }
        if (editableAttrs.get(tagPath) != null) {
            return isTagAllowedInSeq(tagPath, childTag);
        } else if (itemIdx < 0) {
            return false;
        }
        int[] newTagPath = new int[itemIdx];
        System.arraycopy(tagPath, 0, newTagPath, 0, itemIdx);
        return isEditable(newTagPath, tagPath[tagPath.length-1]);
    }
    
    private boolean isTagAllowedInSeq(int[] tagPath, Integer childTag) {
        if (childTag != null) {
            DicomElement el = editableAttrs.get(tagPath);
            if (!el.isEmpty()) {
                DicomObject obj = el.getDicomObject(0);
                if (!obj.isEmpty() && obj.get(childTag)==null) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isUseNullForRemoveTag() {
        return useNullForRemoveTag;
    }

    public boolean isAllowAll() {
        return allowAll;
    }

    public void setAllowAll(boolean allowAll) {
        this.allowAll = allowAll;
    }

    private static void loadDicomObject(InputStream is, DicomObject dcmobj) throws FactoryConfigurationError, ParserConfigurationException, 
            SAXException, IOException {
        SAXParserFactory f = SAXParserFactory.newInstance();
        SAXParser p = f.newSAXParser();
        ContentHandlerAdapter ch = new ContentHandlerAdapter(dcmobj);
        p.parse(is, ch);
    }

}
