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

import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.IFormSubmittingComponent;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 * @version $Revision: 15859 $ $Date: 2011-08-29 16:03:54 +0200 (Mo, 29 Aug 2011) $
 * @since May 22, 2015
 */
public abstract class IndicatingAjaxFormComponentUpdatingBehavior extends AjaxFormComponentUpdatingBehavior implements IAjaxIndicatorAware {

    private static final long serialVersionUID = 1L;
    
    public IAjaxIndicatorAware indicatorComponent;

    public IndicatingAjaxFormComponentUpdatingBehavior(String event, IAjaxIndicatorAware indicatorComponent) {
        super(event);
        this.indicatorComponent = indicatorComponent;
    }

    public String getAjaxIndicatorMarkupId() {
    	if (indicatorComponent == null) {
    		IFormSubmittingComponent btn = this.getFormComponent().getForm().getDefaultButton();
    		if (btn != null && (btn instanceof IAjaxIndicatorAware)) {
    			indicatorComponent = (IAjaxIndicatorAware) btn;
    		} else {
    			return null;
    		}
    	}
        return indicatorComponent.getAjaxIndicatorMarkupId();
    }
}
