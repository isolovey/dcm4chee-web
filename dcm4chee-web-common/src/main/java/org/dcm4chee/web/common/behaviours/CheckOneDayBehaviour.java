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

package org.dcm4chee.web.common.behaviours;

import java.util.Calendar;
import java.util.Date;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.CancelEventIfNoAjaxDecorator;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.model.IModel;
import org.dcm4chee.web.common.markup.SimpleDateTimeField;
import org.dcm4chee.web.common.util.DateUtils;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 * @version $Revision$ $Date$
 * @since Jun 30, 2010
 */
public class CheckOneDayBehaviour extends AjaxFormSubmitBehavior {

    private static final long serialVersionUID = 5109347770809331652L;

    private SimpleDateTimeField dtfStart;
    private SimpleDateTimeField dtfEnd;
    private Date oldStart, newEnd;
    
    public void setNewEnd(long t) {
		this.newEnd = new Date(t);
	}

	public CheckOneDayBehaviour(SimpleDateTimeField dtfStart, SimpleDateTimeField dtfEnd, String event) {
        super(event);
        this.dtfStart = dtfStart;
        this.dtfEnd = dtfEnd;
    }

    @Override
    protected void onEvent(AjaxRequestTarget target) {
        oldStart = dtfStart.getModelObject();
        if (oldStart != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(oldStart);
            DateUtils.setTimeToMinOrMax(cal, !dtfEnd.isWithoutTime());
            oldStart = cal.getTime();
        }
        super.onEvent(target);
    }

    protected void onSubmit(AjaxRequestTarget target) {
        setEndDate(target);
    }

    private void setEndDate(AjaxRequestTarget target) {
        Date startDate = newEnd == null ? dtfStart.getModelObject() : newEnd;
        Date endDate = newEnd == null ? dtfEnd.getModelObject() : null;
        IModel<Date> mEnd = dtfEnd.getModel();
        if (startDate != null && (endDate == null || endDate.equals(oldStart))) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(startDate);
            DateUtils.setTimeToMinOrMax(cal, !dtfEnd.isWithoutTime());
            mEnd.setObject(cal.getTime());
            target.addComponent(dtfStart);
            target.addComponent(dtfEnd);
            newEnd = null;
        }
    }

    protected IAjaxCallDecorator getAjaxCallDecorator() {
            return new CancelEventIfNoAjaxDecorator(null);
    }

    @Override
    protected void onError(AjaxRequestTarget target) {
        setEndDate(target);
        
    }
}
