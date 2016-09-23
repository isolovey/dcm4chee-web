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

package org.dcm4chee.web.dao.util;

import java.util.Iterator;

import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.VR;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Franz Willer <fwiller@gmail.com>
 * @version $Revision$ $Date$
 * @since July 26, 2011
 */

public class CoercionUtil {

    private static Logger log = LoggerFactory.getLogger(CoercionUtil.class);   
            
    public static void coerceAttributes(DicomObject attrs, DicomObject coerce) {
        coerceAttributes(attrs, coerce, null);
    }
    
    public static void coerceAttributes(DicomObject attrs, DicomObject coerce,
            DicomElement parent) {
        boolean coerced = false;
        DicomElement el;
        DicomElement oldEl;
        for (Iterator<DicomElement> it = coerce.iterator(); it.hasNext();) {
            el = it.next();
            oldEl = attrs.get(el.tag());
            if (el.isEmpty()) {
                coerced = oldEl != null && !oldEl.isEmpty();
                if (oldEl == null || coerced) {
                    if ( el.vr()==VR.SQ ) {
                        attrs.putSequence(el.tag());
                    } else {
                        attrs.putBytes(el.tag(), el.vr(), el.getBytes());
                    }
                }
            } else {
                DicomObject item;
                DicomElement sq = oldEl; 
                if (el.vr() == VR.SQ) {
                    coerced = oldEl != null && sq.vr() != VR.SQ;
                    if (oldEl == null || coerced) {
                        sq = attrs.putSequence(el.tag());
                    }
                    for (int i = 0, n = el.countItems(), sqLen = sq.countItems(); i < n; i++) {
                        if (i < sqLen) {
                            item  = sq.getDicomObject(i);
                        } else {
                            item = new BasicDicomObject();
                            sq.addDicomObject(item);
                        }
                        DicomObject coerceItem = el.getDicomObject(i);
                        coerceAttributes(item, coerceItem, el);
                        if (!coerceItem.isEmpty()) {
                            coerced = true;
                        }
                    }
                } else {
                    coerced = oldEl != null && !oldEl.equals(el);
                    if (oldEl == null || coerced) {
                        attrs.putBytes(el.tag(), el.vr(), el.getBytes());
                    }
                }
            }
            if (coerced) {
                log.info(parent == null ? ("Coerce " + oldEl + " to " + el)
                                : ("Coerce " + oldEl + " to " + el
                                        + " in item of " + parent));
            } else {
                if (oldEl == null && log.isDebugEnabled()) {
                    log.debug(parent == null ? ("Add " + el) : ("Add " + el
                            + " in item of " + parent));
                }
            }
        }
    }

}
