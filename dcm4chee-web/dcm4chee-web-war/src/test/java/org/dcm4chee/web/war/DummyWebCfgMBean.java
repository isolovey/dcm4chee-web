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
 * TIANI Medgraph AG.
 * Portions created by the Initial Developer are Copyright (C) 2003-2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * Gunter Zeilinger <gunter.zeilinger@tiani.com>
 * Franz Willer <franz.willer@gwi-ag.com>
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
package org.dcm4chee.web.war;

import java.util.ArrayList;
import java.util.Arrays;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.ReflectionException;

import org.jboss.system.ServiceMBeanSupport;

public class DummyWebCfgMBean extends ServiceMBeanSupport implements DynamicMBean {

    public Object getAttribute(String attribute)
            throws AttributeNotFoundException, MBeanException,
            ReflectionException {
        if (attribute.endsWith("Pagesize")) return 10;
        if ("QueryAfterPagesizeChange".equals(attribute)) return true;
        if ("useFamilyAndGivenNameQueryFields".equals(attribute)) return false;
        if ("WebConfigPath".equals(attribute)) return "conf/dcm4chee-web3";
        if ("sourceAetsPropertiesFilename".equals(attribute)) return "source_aets.properties";
        if ("stationAetsPropertiesFilename".equals(attribute)) return "station_aets.properties";
        if ("mppsEmulatorServiceName".equals(attribute)) {
            return this.getServiceName();
        }
        if ("ModalityAETitles".equals(attribute)) return "TEST_AET";
        
        return null;
    }

    public AttributeList getAttributes(String[] attributes) {
        return null;
    }

    public MBeanInfo getMBeanInfo() {
        MBeanOperationInfo[] ops = new MBeanOperationInfo[]{};
        MBeanInfo info = new MBeanInfo(getClass().getName(), "Dummy WebCfg and UserAccess Service implementation for unit testing", null, null, ops, null);
        return info;
    }

    public Object invoke(String actionName, Object[] p, String[] signature)
            throws MBeanException, ReflectionException {
        if ("getPagesizeList".equals(actionName)) return new ArrayList<Integer>(Arrays.asList(10,25,50));
        if ("getModalityList".equals(actionName)) return new ArrayList<String>(Arrays.asList("CT","MR"));
        if ("getStationNameList".equals(actionName)) return new ArrayList<String>(Arrays.asList("STATION1","STATION2"));
        if ("getWindowSize".equals(actionName)) return new int[]{800,600};
        if ("getAutoExpandLevelChoiceList".equals(actionName)) return Arrays.asList(-1);
        if ("getAETTypesList".equals(actionName)) return Arrays.asList(null,"TEST");
        return null;
    }

    public void setAttribute(Attribute attribute)
            throws AttributeNotFoundException, InvalidAttributeValueException,
            MBeanException, ReflectionException {
    }

    public AttributeList setAttributes(AttributeList attributes) {
        return null;
    }
}
