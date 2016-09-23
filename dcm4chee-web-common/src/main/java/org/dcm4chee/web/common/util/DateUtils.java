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

package org.dcm4chee.web.common.util;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.wicket.Component;
import org.joda.time.format.DateTimeFormat;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision$ $Date$
 * @since Dec 19, 2008
 */
public class DateUtils {

    public static String da2str(String da) {
        if (da == null) {
            return null;
        }
        if (da.length() != 8) {
            return da;
        }
        StringBuilder sb = new StringBuilder(10);
        sb.append(da.substring(0, 4))
                .append('-')
                .append(da.substring(4, 6))
                .append('-')
                .append(da.substring(6));
        return sb.toString();
    }

    public static String str2da(String str) {
        String s = str.trim();
        if (s.length() != 10) {
            return s;
        }
        StringBuilder sb = new StringBuilder(8);
        sb.append(s.substring(0, 4))
                .append(s.substring(5, 7))
                .append(s.substring(8));
        return sb.toString();
    }

    public static String tm2str(String tm) {
        if (tm == null) {
            return null;
        }
        if (tm.length() < 4 || tm.charAt(2) == ':') {
            return tm;
        }
        StringBuilder sb = new StringBuilder(10);
        sb.append(tm.substring(0, 2))
                .append(':')
                .append(tm.substring(2, 4));
        if (tm.length() >= 6) {
            sb.append(':').append(tm.substring(4,6));
        }
        return sb.toString();
    }

    public static String str2tm(String str) {
        String s = str.trim();
        int colon = s.indexOf(':');
        if (colon == -1) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s.length());
        int pos = 0;
        do {
            sb.append(s.substring(pos, colon));
            pos = colon + 1;
            colon = s.indexOf(':', pos);
        } while (colon != -1);
        sb.append(s.substring(pos));
        return sb.toString();
    }

    public static String datm2str(String da, String tm) {
        return da2str(da) + " " + tm2str(tm);
    }

    public static String[] str2datm(String str) {
        String s = str.trim();
        int space = s.indexOf(' ');
        String[] datm = new String[2];
        if (space == -1) {
            datm[0] = str2da(s);
        } else {
            datm[0] = str2da(s.substring(0, space));
            datm[1] = str2tm(s.substring(space+1).trim());
        }
        return datm;
    }
    
    public static String getDatePattern(Component c) {
        String pattern = DateTimeFormat.patternForStyle("S-", c.getLocale());
        int pos1 = pattern.indexOf('y');
        if (pos1 != -1) {
            if (pattern.length() <= pos1+2) {
                pattern = pattern + "yy";
            } else if ( pattern.charAt(pos1+2)!='y') {
                pattern = pattern.substring(0,pos1)+"yyyy"+pattern.substring(pos1+2);
            }
        }
        return pattern;
    }
    
    public static void setTimeToMinOrMax(Calendar cal, boolean max) {
        if (max) {
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 999);
        } else {
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
        }
    }

    public static Date[] fromOffsetToCurrentDateRange(String offset) {
        if (offset == null || "NONE".equals(offset))
            return null;
        int offs = Integer.parseInt(offset);
        GregorianCalendar cal = new GregorianCalendar();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis() - (long)offs * 86400000l;
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return new Date[]{new Date(start), cal.getTime()};
    }

}
