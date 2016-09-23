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

package org.dcm4chee.web.common.markup;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.IConverter;
import org.dcm4chee.web.common.util.DateUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @author Franz Willer <franz.willer@gmail.com>
 * @version $Revision$ $Date$
 * @since Jan 22, 2009
 */
public class DateTimeLabel extends Label {

    private static final long serialVersionUID = 1L;
    private SimpleDateFormat df = new SimpleDateFormat("HH:mm");
    private boolean withoutTime;

    public DateTimeLabel(String id) {
        super(id);
    }

    public DateTimeLabel(String id, String label) {
        super(id, label);
    }

    public DateTimeLabel(String id, IModel<Date> model) {
        super(id, model);
    }
    
    public DateTimeLabel setWithoutTime(boolean b) {
        withoutTime = b;
        return this;
    }

    @Override
    public IConverter getConverter(Class<?> type) {
        return new IConverter() {

            private static final long serialVersionUID = 1L;

            public Object convertToObject(String value, Locale locale) {
                throw new UnsupportedOperationException();
            }

            public String convertToString(Object value, Locale locale) {
                
                if (value == null) return null;
                Date d = (Date) value;
                String pattern = getTextFormat();
                DateTimeFormatter dtf = DateTimeFormat.forPattern(pattern)
                .withLocale(getLocale()).withPivotYear(2000);
                return withoutTime ? dtf.print(d.getTime()) : dtf.print(d.getTime())+" "+df.format(d);
            }

            };
    }
    public String getTextFormat() {
        return DateUtils.getDatePattern(this);
    }
}
