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
 * Java(TM), available at http://sourceforge.net/projects/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * Agfa HealthCare.
 * Portions created by the Initial Developer are Copyright (C) 2006-2008
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

package org.dcm4chee.dashboard.model;

import java.io.Serializable;
import java.util.List;

/**
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision$ $Date$
 * @since 14.09.2011
 */
public class MBeanValueModel extends PropertyDisplayModel {

    private static final long serialVersionUID = 1L;
    
    String domain;
	String type;
    String function;
    
    public MBeanValueModel() {
    }

    public MBeanValueModel(String group, String label, String domain, String name, String type, String function, String value) {
    	this.group = group;
    	this.label = label;
        this.domain = domain;
        this.name = name;
        this.type = type;
        this.function = function;
        this.value = value;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }
    
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFunction() {
        return function;
    }

    public void setFunction(String function) {
        this.function = function;
    }
    
    public void setValue(Object value) {
        this.value = value != null ? value.toString() : null;
    }

    @Override
    public String getDescription() {
    	return domain + ":" + name + "," + function;
    }
    
    public int compareTo(MBeanValueModel o) {
        int d = this.domain.compareTo(o.getDomain());
        if (d == 0) {
            int n = this.name.compareTo(o.getName());
            if (n == 0) {
                int t = this.type.compareTo(o.getType());
                if (t == 0)
                    return this.function.compareTo(o.getFunction());
                else return t;
            } else return n;
        } else return d;
    }
}
