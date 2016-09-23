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
 * Portions created by the Initial Developer are Copyright (C) 2002-2005
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

package org.dcm4chee.dashboard.ui.systeminfo;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.extensions.markup.html.tree.table.ColumnLocation;
import org.apache.wicket.extensions.markup.html.tree.table.IColumn;
import org.apache.wicket.extensions.markup.html.tree.table.PropertyRenderableColumn;
import org.apache.wicket.extensions.markup.html.tree.table.PropertyTreeColumn;
import org.apache.wicket.extensions.markup.html.tree.table.ColumnLocation.Alignment;
import org.apache.wicket.extensions.markup.html.tree.table.ColumnLocation.Unit;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.list.OddEvenListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.resources.CompressedResourceReference;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.dcm4chee.dashboard.mbean.DashboardDelegator;
import org.dcm4chee.dashboard.model.MBeanValueModel;
import org.dcm4chee.dashboard.model.PropertyDisplayModel;
import org.dcm4chee.dashboard.ui.DashboardPanel;
import org.dcm4chee.dashboard.ui.common.DashboardTreeTable;
import org.dcm4chee.dashboard.ui.util.CSSUtils;
import org.dcm4chee.icons.ImageManager;
import org.dcm4chee.web.common.base.BaseWicketApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision$ $Date$
 * @since 18.11.2009
 */
public class SystemInfoPanel extends Panel {
    
    private static final long serialVersionUID = 1L;
    
    private static Logger log = LoggerFactory.getLogger(SystemInfoPanel.class);

    public SystemInfoPanel(String id) {
        super(id);
    }
    
    @Override
    public void onBeforeRender() {
        super.onBeforeRender();

        try {
            Class<?> memoryPoolMXBean = Thread.currentThread().getContextClassLoader()
                .loadClass("java.lang.management.MemoryPoolMXBean");
            Class<?> memoryUsage = Thread.currentThread().getContextClassLoader()
                .loadClass("java.lang.management.MemoryUsage");
            
            List<MemoryInstanceModel> memoryInstanceList = new ArrayList<MemoryInstanceModel>();

            for (Object pool : (List<?>) Thread.currentThread().getContextClassLoader()
                                    .loadClass("java.lang.management.ManagementFactory")
                                    .getMethod("getMemoryPoolMXBeans", new Class[0])
                                    .invoke(null, new Object[0])) {
                Object usage = memoryPoolMXBean
                                .getMethod("getUsage", new Class[0])
                                .invoke(pool, new Object[0]);
                if (usage != null) {
                    memoryInstanceList.add(
                            new MemoryInstanceModel(
                                    memoryPoolMXBean
                                        .getMethod("getName", new Class[0])
                                        .invoke(pool, new Object[0]).toString(), 
                                    memoryPoolMXBean
                                        .getMethod("getType", new Class[0])
                                        .invoke(pool, new Object[0]).toString(), 
                                    (Long) memoryUsage.getMethod("getInit", new Class[0]).invoke(usage, new Object[0]), 
                                    (Long) memoryUsage.getMethod("getUsed", new Class[0]).invoke(usage, new Object[0]), 
                                    (Long) memoryUsage.getMethod("getCommitted", new Class[0]).invoke(usage, new Object[0]), 
                                    (Long) memoryUsage.getMethod("getMax", new Class[0]).invoke(usage, new Object[0])));
                }
            }

            addOrReplace(new ListView<MemoryInstanceModel>("memory-instance-rows", memoryInstanceList) {

                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(ListItem<MemoryInstanceModel> item) {
                    MemoryInstanceModel memoryInstanceModel = (MemoryInstanceModel) item.getModelObject();
                    item.add(new Label("name", String.valueOf(memoryInstanceModel.getName())));
                    item.add(new Label("type", String.valueOf(memoryInstanceModel.getType())));
                    item.add(new Label("init", String.valueOf(memoryInstanceModel.getInit())));
                    item.add(new Label("used", String.valueOf(memoryInstanceModel.getUsed())));
                    item.add(new Label("committed", String.valueOf(memoryInstanceModel.getCommitted())));
                    item.add(new Label("max", String.valueOf(memoryInstanceModel.getMax())));
                    item.add(new AttributeModifier("class", true, new Model<String>(CSSUtils.getRowClass(item.getIndex()))));
                }
            });
            
            DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(new PropertyDisplayModel());
            Map<String, List<PropertyDisplayModel>> propertyDisplayMap = new HashMap<String, List<PropertyDisplayModel>>();            		

            try {
                for (PropertyDisplayModel model : DashboardDelegator.getInstance((((BaseWicketApplication) getApplication()).getInitParameter("DashboardServiceName"))).getSystemProperties()) {
                    if (!propertyDisplayMap.containsKey(model.getGroup()))
                        propertyDisplayMap.put(model.getGroup(), new ArrayList<PropertyDisplayModel>());
                    propertyDisplayMap.get(model.getGroup()).add((PropertyDisplayModel) model);
                }
            } catch (Exception e) {
                log.error("Can't create list for system properties: ", e);
            }

            try {
                for (MBeanValueModel model : DashboardDelegator.getInstance((((BaseWicketApplication) getApplication()).getInitParameter("DashboardServiceName"))).getMBeanValues()) {
                    if (!propertyDisplayMap.containsKey(model.getGroup()))
                        propertyDisplayMap.put(model.getGroup(), new ArrayList<PropertyDisplayModel>());
                    propertyDisplayMap.get(model.getGroup()).add((PropertyDisplayModel) model);
                }
            } catch (Exception e) {
                log.error("Can't create list for mbean values: ", e);
            }
            
            for (String key : propertyDisplayMap.keySet()) {
                PropertyDisplayModel group = new PropertyDisplayModel();
                group.setLabel(key);
                DefaultMutableTreeNode groupNode;
                rootNode.add(groupNode = new DefaultMutableTreeNode(group));

                List<PropertyDisplayModel> propertyDisplayModelList = propertyDisplayMap.get(key);
                Collections.sort(propertyDisplayModelList);
                for (PropertyDisplayModel propertyDisplayModel : propertyDisplayModelList) {
                    groupNode.add(new DefaultMutableTreeNode(propertyDisplayModel));
                }
            }

            PropertyRenderableColumn valueColumn = 
            new PropertyRenderableColumn(new ColumnLocation(
                    Alignment.RIGHT, 70, Unit.PERCENT), 
                    new ResourceModel(
                            "dashboard.systemproperty.table.column.value").wrapOnAssignment(this).getObject(),
                            "userObject.value");
            valueColumn.setContentAsTooltip(true);
            SystemPropertyTreeTable systemPropertyTreeTable = new SystemPropertyTreeTable("systemproperty-tree-table", 
                    new DefaultTreeModel(rootNode), new IColumn[] {
                new PropertyTreeColumn(new ColumnLocation(
                        Alignment.LEFT, 30, Unit.PERCENT), 
                        new ResourceModel(
                                "dashboard.systemproperty.table.column.label").wrapOnAssignment(this).getObject(), 
                                "userObject.label"), 
                valueColumn
            });
            systemPropertyTreeTable.getTreeState().setAllowSelectMultiple(true);
            systemPropertyTreeTable.getTreeState().expandAll();
            systemPropertyTreeTable.setRootLess(true);
            addOrReplace(systemPropertyTreeTable);
        } catch (Exception e) {
            log.error(this.getClass().toString() + ": " + "onBeforeRender: " + e.getMessage());
            log.debug("Exception: ", e);
            throw new WicketRuntimeException(e.getLocalizedMessage(), e);
        }        
    }
    
    private class SystemPropertyTreeTable extends DashboardTreeTable {

        private static final long serialVersionUID = 1L;

        public SystemPropertyTreeTable(String id, TreeModel model, IColumn[] columns) {
            super(id, model, columns);
        }

        @Override
        protected Component newNodeIcon(MarkupContainer parent, String id, final TreeNode node) {

            return new WebMarkupContainer(id) {

                private static final long serialVersionUID = 1L;

                @Override
                protected void onComponentTag(ComponentTag tag) {
                    super.onComponentTag(tag);

                    tag.put("style", "background-image: url('" + 
                    ((((PropertyDisplayModel) ((DefaultMutableTreeNode) node).getUserObject()).getGroup() == null) ? 
                        getRequestCycle().urlFor(ImageManager.IMAGE_DASHBOARD_PROPERTY_FOLDER) 
                        : getRequestCycle().urlFor(ImageManager.IMAGE_DASHBOARD_PROPERTY)) 
                        +"')"
                    );
                    tag.put("title", ((PropertyDisplayModel) ((DefaultMutableTreeNode) node).getUserObject()).getDescription());
                }
            };
        }        
    };

    private class MemoryInstanceModel implements Serializable {
        
        private static final long serialVersionUID = 1L;
        
        public static final int MEGA = 1048576;
        
        private final int memoryDisplayLength = 8;
        
        private NumberFormat formatter;

        private String name;
        private String type;
        
        private long init;
        private long used;
        private long committed;
        private long max;
        
        public MemoryInstanceModel(String name, String type, long init, long used, long committed, long max) {
            this.name = name;
            this.type = type;
            this.init = init;
            this.used = used;
            this.committed = committed;
            this.max = max;
            
            this.formatter = DecimalFormat.getInstance();
            this.formatter.setMaximumFractionDigits(3);
            this.formatter.setMinimumIntegerDigits(1);
        }
        
        public String getName() {
            return name;
        }
        
        public String getType() {
            return type;
        }
        
        public String getInit() {
            return format(this.init);
        }
        
        public String getUsed() {
            return format(this.used);
        }
        
        public String getCommitted() {
            return format(this.committed);
        }
        
        public String getMax() {
            return format(this.max);
        }
        
        private String format(long memory) {
            String memoryString = this.formatter.format(new Float(memory)/MemoryInstanceModel.MEGA);
            return memoryString.substring(0, Math.min(memoryString.length(), this.memoryDisplayLength)) + " MB";
        }
    }
    
    public static String getModuleName() {
        return "dashboard.systeminfo";
    }
}