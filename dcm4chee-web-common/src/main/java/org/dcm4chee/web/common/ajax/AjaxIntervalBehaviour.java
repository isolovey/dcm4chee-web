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
package org.dcm4chee.web.common.ajax;

import org.apache.wicket.Page;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.IHeaderResponse;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 * @version $Revision: $ $Date: $
 * @since Dec 07, 2011
 */
public abstract class AjaxIntervalBehaviour extends AbstractDefaultAjaxBehavior {

    private static final long serialVersionUID = 1L;

    private int updateInterval;

    public AjaxIntervalBehaviour(final int updateInterval) {
        if (updateInterval <= 0) {
            throw new IllegalArgumentException("Invalid update interval! Must be >0 but is:"+updateInterval);
        }
        this.updateInterval = updateInterval;
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.renderOnLoadJavascript(getJsTimeoutCall());
    }

    protected final String getJsTimeoutCall() {
        return "setInterval(\"" + getCallbackScript() + "\", " + updateInterval + ");";
    }

    @Override
    protected CharSequence getCallbackScript() {
        return generateCallbackScript("wicketAjaxGet('" + getCallbackUrl() + "'");
    }

    @Override
    protected CharSequence getPreconditionScript() {
        String precondition = null;
        if (!(getComponent() instanceof Page)) {
            String componentId = getComponent().getMarkupId();
            precondition = "var c = Wicket.$('" + componentId +
                "'); return typeof(c) != 'undefined' && c != null";
        }
        return precondition;
    }

    @Override
    protected final void respond(final AjaxRequestTarget target) {
        onTimer(target);
    }

    /**
     * Listener method for the AJAX timer event.
     * 
     * @param target
     *            The request target
     */
    protected abstract void onTimer(final AjaxRequestTarget target);

}
