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
 ***** END LICENSE BLOCK ***** */

package org.dcm4chee.web.common.model;

import java.util.Date;

import org.apache.wicket.model.IModel;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.ElementDictionary;
import org.dcm4che2.data.VR;
import org.dcm4che2.data.VRMap;


/**
 * @author Franz Willer <franz.willer@gmail.com>
 * @version $Revision$ $Date$
 * @param <E>
 * @since Jul 06, 2010
 */

public abstract class DicomElementModel<E> implements IModel<E>{

    protected static ElementDictionary dict = ElementDictionary.getDictionary();;
    protected static VRMap vrMap = VRMap.getVRMap();
    
    private static final long serialVersionUID = 0L;
    protected DicomObject dcmObj;
    protected int vrCode;
    
    protected DicomElementModel(DicomObject dcmObj, int tag) {
        this.dcmObj = dcmObj;
        DicomElement el = dcmObj.get(tag);
        vrCode = el != null ? el.vr().code(): vrMap.vrOf(tag).code();
    }
    protected DicomElementModel(DicomObject dcmObj, int[] tagPath) {
        this.dcmObj = dcmObj;
        vrCode = getVRof(dcmObj, tagPath).code();
    }
    
    public static VR getVRof(DicomObject dcmObj, int[] tagPath) {
        if ((tagPath.length & 0x1) == 0) { // even length -> Compound DA/DT
            return VR.DT;
        } else {
            DicomElement el = dcmObj.get(tagPath);
            return el != null ? el.vr() : vrMap.vrOf(tagPath[tagPath.length-1]);
        }
    }
    
    public VR getVR() {
        return VR.valueOf(vrCode);
    }
    
    public static DicomElementModel<?> get(DicomObject dcmObj, int[] tagPath) {
        VR vr = getVRof(dcmObj, tagPath);
        if (vr == VR.DA || vr == VR.DT) {
            return newDateModel(dcmObj, tagPath);
        }
        return newStringModel(dcmObj, tagPath);
    }
    
    public static DicomElementModel<String> newStringModel(DicomObject dcmObj, int[] tagPath) {
        return new StringModel(dcmObj, tagPath);
    }
    public static DicomElementModel<Date> newDateModel(DicomObject dcmObj, int[] tagPath) {
        if (tagPath.length == 1) {
            return new SimpleDateModel(dcmObj, tagPath[0]);
        } else if (tagPath.length == 2) {
            return new CompoundDateTimeModel(dcmObj, tagPath[0], tagPath[1]);
        }
        return new DateModel(dcmObj, tagPath);
    }

    public void detach() {
    }

    
}
class StringModel extends DicomElementModel<String> {

    private static final long serialVersionUID = 1L;
    private final int[] tagPath;

    public StringModel(DicomObject dcmObj, int[] tagPath) {
        super(dcmObj, tagPath);
        this.tagPath = tagPath.clone();
    }
    
    public String getObject() {
        return dcmObj.getString(tagPath);
    }

    public void setObject(String object) {
        String prev = getObject();
        if (object == null) {
            dcmObj.putNull(tagPath, VR.valueOf(vrCode));
        } else if (!object.equals(prev)) {
            dcmObj.putString(tagPath, VR.valueOf(vrCode), object);
        }
    }
}
class SimpleDateModel extends DicomElementModel<Date> {

    private static final long serialVersionUID = 1L;
    private final int tag;

    public SimpleDateModel(DicomObject dcmObj, int tag) {
        super(dcmObj, tag);
        this.tag = tag;
    }

    public Date getObject() {
        return dcmObj.getDate(tag);
    }

    public void setObject(Date object) {
        Date prev = getObject();
        if (object == null) {
            dcmObj.putNull(tag, VR.valueOf(vrCode));
        } else if (!object.equals(prev)) {
            dcmObj.putDate(tag, VR.valueOf(vrCode), object);
        }
    }
}

class DateModel extends DicomElementModel<Date> {

    private static final long serialVersionUID = 1L;
    private final int[] tagPath;

    public DateModel(DicomObject dcmObj, int[] tagPath) {
        super(dcmObj, tagPath);
        this.tagPath = tagPath.clone();
    }

    public Date getObject() {
        return dcmObj.getDate(tagPath);
    }

    public void setObject(Date object) {
        Date prev = getObject();
        if (object == null) {
            dcmObj.putNull(tagPath, VR.valueOf(vrCode));
        } else if (!object.equals(prev)) {
            dcmObj.putDate(tagPath, VR.valueOf(vrCode), object);
        }
    }
}

class CompoundDateTimeModel extends DicomElementModel<Date> {

    private static final long serialVersionUID = 1L;
    private int daTag, tmTag;

    public CompoundDateTimeModel(DicomObject dcmObj, int daTag, int tmTag) {
        super(dcmObj, new int[]{daTag, tmTag});
        this.daTag = daTag;
        this.tmTag = tmTag;
    }

    public Date getObject() {
        return dcmObj.getDate(daTag, tmTag);
    }

    public void setObject(Date object) {
        Date prev = getObject();
        if (object == null) {
            dcmObj.putNull(daTag, VR.DA);
            dcmObj.putNull(tmTag, VR.TM);
        } else if (!object.equals(prev)) {
            dcmObj.putDate(daTag, VR.DA, object);
            dcmObj.putDate(tmTag, VR.TM, object);
        }
    }
}

