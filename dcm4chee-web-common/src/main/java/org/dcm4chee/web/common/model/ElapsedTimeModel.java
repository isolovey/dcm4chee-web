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

import org.apache.wicket.model.AbstractReadOnlyModel;


/**
 * @author Franz Willer <franz.willer@gmail.com>
 * @version $Revision$ $Date$
 * @param <E>
 * @since Jun 15, 2010
 */

public class ElapsedTimeModel extends AbstractReadOnlyModel<Long>{

    public static final int MILLIS = 1;
    public static final int SECONDS = 1000;
    public static final int MINUTES = 60000;
    public static final int HOURS = 3600000;
    public static final int DAYS = 86400000;
    
    private long startTime;
    private int unitDivider;
    private boolean reset = false;
    
    private static final long serialVersionUID = 0L;
    
    public ElapsedTimeModel() {
        this(System.currentTimeMillis(), MILLIS);
    }
    public ElapsedTimeModel(int divider) {
        this(System.currentTimeMillis(), divider);
    }
    public ElapsedTimeModel(long start, int divider) {
        if (divider == 0)
            throw new IllegalArgumentException("UNIT divider must not be 0!");
        startTime = start;
        unitDivider = divider;
    }
    
    public ElapsedTimeModel setResetAfterGet(boolean b) {
        reset = b;
        return this;
    }
    
    public Long getObject() {
        return unitDivider == 1 ? getMillis() : getMillis() / unitDivider;
    }
    public Long getMillis() {
        long t1 = System.currentTimeMillis();
        long diff = t1 - startTime;
        if (reset) {
            startTime = t1;
        }
        return diff;
        
    }
    public Long getSeconds() {
        return getMillis() / SECONDS;
    }
    public Long getMinutes() {
        return getMillis() / MINUTES;
    }
    public Long getHours() {
        return getMillis() / HOURS;
    }
    public Long getDays() {
        return getMillis() / DAYS;
    }

}

