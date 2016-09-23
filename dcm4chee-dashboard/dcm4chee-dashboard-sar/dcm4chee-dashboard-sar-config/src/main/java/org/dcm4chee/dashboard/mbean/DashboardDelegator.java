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

package org.dcm4chee.dashboard.mbean;

import java.io.File;
import java.util.List;
import java.util.Map;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.dcm4chee.dashboard.model.MBeanValueModel;
import org.dcm4chee.dashboard.model.ReportModel;
import org.dcm4chee.dashboard.model.PropertyDisplayModel;
import org.dcm4chee.dashboard.model.PropertyDisplayModel;
import org.jboss.mx.util.MBeanServerLocator;

/**
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision$ $Date$
 * @since 25.11.2009
 */
public final class DashboardDelegator {

    private static final long serialVersionUID = 1L;
    
    private static DashboardDelegator dashboardDelegator;
    
    private final MBeanServer server = MBeanServerLocator.locate();
    private ObjectName objectName;
    
    private String newline = System.getProperty("line.separator");
    
    private DashboardDelegator(String objectName) throws MalformedObjectNameException, NullPointerException {
        this.objectName = new ObjectName(objectName);
    }

    public static synchronized DashboardDelegator getInstance(String objectName) throws MalformedObjectNameException, NullPointerException { 
        if (dashboardDelegator == null) 
            dashboardDelegator = new DashboardDelegator(objectName); 
        return dashboardDelegator; 
    }
    
    public String[] listAllFileSystemGroups() throws MalformedObjectNameException, NullPointerException, InstanceNotFoundException, ReflectionException, MBeanException {
        return (String[]) this.server.invoke(
                        this.objectName, 
                        "listAllFileSystemGroups", null, null);
    }
    
    public File[] listFileSystemsOfGroup(String groupname) throws InstanceNotFoundException, MalformedObjectNameException, ReflectionException, MBeanException, NullPointerException {
        return (File[]) server.invoke(
                        this.objectName,
                        "listFileSystemsOfGroup",
                        new Object[] { groupname },
                        new String[] { String.class.getName() });
    }
    
    public long getMinimumFreeDiskSpaceOfGroup(String groupname) throws InstanceNotFoundException, MalformedObjectNameException, ReflectionException, MBeanException, NullPointerException, AttributeNotFoundException {
        return ((Long) server.invoke(
                        this.objectName,
                        "getMinimumFreeDiskSpaceOfGroup",
                        new Object[] { groupname },
                        new String[] { String.class.getName() }))
                        .longValue();
    }
    
    public long getExpectedDataVolumePerDay(String groupname) throws InstanceNotFoundException, MalformedObjectNameException, ReflectionException, MBeanException, NullPointerException, AttributeNotFoundException {
        return ((Long) server.invoke(
                        this.objectName,
                        "getExpectedDataVolumePerDay",
                        new Object[] { groupname },
                        new String[] { String.class.getName() }))
                        .longValue();
    }

    public String getDefaultRetrieveAETitle(String groupname) throws AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException {
        return ((String) server.invoke(
                        this.objectName,
                        "getDefaultRetrieveAETitle",
                        new Object[] { groupname },
                        new String[] { String.class.getName() }));
    }
    
    public String[] listOtherFileSystems() throws InstanceNotFoundException, ReflectionException, MBeanException {
        return (String[]) this.server.invoke(
                        this.objectName, 
                        "listOtherFileSystems", null, null);
    }
    
    @SuppressWarnings("unchecked")
    public List<PropertyDisplayModel> getSystemProperties() throws InstanceNotFoundException, MalformedObjectNameException, ReflectionException, MBeanException, NullPointerException {
        return (List<PropertyDisplayModel>) server.invoke(
                        this.objectName,
                        "getSystemProperties", null, null);
    }
    
    public List<MBeanValueModel> getMBeanValues() throws InstanceNotFoundException, ReflectionException, MBeanException {
        return (List<MBeanValueModel>) this.server.invoke(
                        this.objectName, 
                        "getMBeanValues", null, null);
    }

    public ReportModel[] listAllReports(boolean groups) throws InstanceNotFoundException, MalformedObjectNameException, ReflectionException, MBeanException, NullPointerException {
        return (ReportModel[]) server.invoke(
                        this.objectName,
                        "listAllReports", 
                        new Object[] { groups },
                        new String[] { boolean.class.getName() });
    }
    
    public void createReport(ReportModel report, boolean isGroup) throws InstanceNotFoundException, MalformedObjectNameException, ReflectionException, MBeanException, NullPointerException {
                        server.invoke(
                        this.objectName,
                        "createReport", 
                        new Object[] { report, isGroup }, 
                        new String[] { ReportModel.class.getName(), boolean.class.getName()});
    }
    
    public void updateReport(ReportModel report) throws InstanceNotFoundException, MalformedObjectNameException, ReflectionException, MBeanException, NullPointerException {
                        server.invoke(
                        this.objectName,
                        "updateReport", 
                        new Object[] { report }, 
                        new String[] { ReportModel.class.getName() });       
    }
    
    public void deleteReport(ReportModel report, boolean isGroup) throws InstanceNotFoundException, MalformedObjectNameException, ReflectionException, MBeanException, NullPointerException {
                        server.invoke(
                        this.objectName,
                        "deleteReport", 
                        new Object[] { report, isGroup }, 
                        new String[] { ReportModel.class.getName(), boolean.class.getName() });       
    }
    
    public String[] getDataSources() throws InstanceNotFoundException, MalformedObjectNameException, ReflectionException, MBeanException, NullPointerException, AttributeNotFoundException {
        return (String[]) server.getAttribute(
                        this.objectName,
                        "dataSourceList")
                        .toString()
                        .split(this.newline);
    }
    
    public String[][] listQueueNames() throws InstanceNotFoundException, MalformedObjectNameException, ReflectionException, MBeanException, NullPointerException, AttributeNotFoundException {
        return (String[][]) server.invoke(
                        this.objectName,
                        "listQueueNames", null, null);
    }
    
    public int[] listQueueAttributes(String domainName, String queueName) throws InstanceNotFoundException, MalformedObjectNameException, ReflectionException, MBeanException, NullPointerException, AttributeNotFoundException {
        return (int[]) server.invoke(
                        this.objectName,
                        "listQueueAttributes", 
                        new Object[] { domainName, queueName }, 
                        new String[] { java.lang.String.class.getName(), java.lang.String.class.getName() });
    }
    
    public String[] listQueueDepthConfig() throws InstanceNotFoundException, ReflectionException, MBeanException {
        return (String[]) this.server.invoke(
                        this.objectName, 
                        "listQueueDepthConfig", null, null);
    }

    public int getReportTablePagesize() throws InstanceNotFoundException, MalformedObjectNameException, ReflectionException, MBeanException, NullPointerException, AttributeNotFoundException {
        return (Integer) server.getAttribute(
                        this.objectName,
                        "reportTablePagesize");
    }
}
