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

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.AbstractTextComponent.ITextFormatProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 * @version $Revision$ $Date$
 * @since Jun 07, 2010
 */
public class TimeField extends TextField<Date> implements ITextFormatProvider {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(TimeField.class);
    
    public TimeField(String id, IModel<Date> model) {
        super(id, model);
        this.setType(Date.class);
    }
    
    @Override
    public void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);
        if ( tag.getAttribute("class") == null ) {
            tag.put("class", "timeField");
        }
    }
    
    @Override
    protected void convertInput() {
        super.convertInput();
    }
   
    @Override
    public IConverter getConverter(Class<?> type) {
        return new IConverter() {

            private static final long serialVersionUID = 1L;
            private Calendar cal = Calendar.getInstance();

            public Object convertToObject(String value, Locale locale) {
                return convert(value);
            }

            public String convertToString(Object value, Locale locale) {
                cal.setTime((Date) value);
                int m = cal.get(Calendar.MINUTE);
                return cal.get(Calendar.HOUR_OF_DAY)+":"+(m < 10 ? "0"+m : String.valueOf(m));
            }

        };
    }
    public String getTextFormat() {
        return "HH:mm";
    }
    
    public Date convert(String value) {
        if ( value == null || value.length()==0) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        int pos = value.indexOf(':');
        int posMinStart = -1;
        try {
            if ( pos == -1) {
                int len = value.length();
                if (len < 3) {
                    pos = len;
                } else if ( len < 5) {
                    pos = len - 2;
                    posMinStart = pos;
                } else {
                    log.info("Convert time String to date failed! Missing ':' value:"+value);
                    throw new ConversionException("Missing ':'");
                }
            } else {
                posMinStart = pos+1;
            }
            int h = Integer.parseInt(value.substring(0,pos));
            int m = posMinStart == -1 ? 0 : Integer.parseInt(value.substring(posMinStart));
            if ( h < 0 || h > 23 || m < 0 || m > 59) {
                log.info("Convert time String to date failed! Hour or minutes not in range! value:"+value);
                throw new ConversionException("Hour or minutes invalid!");
            }
            cal.set(Calendar.HOUR_OF_DAY, h);
            cal.set(Calendar.MINUTE, m);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal.getTime();
        } catch (Exception x) {
            log.info("Convert time String to date failed! Exception thrown! value:"+value, x);
            throw new ConversionException("Hour or minutes invalid!");
        }
    }
}
