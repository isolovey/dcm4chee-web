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

package org.dcm4chee.dashboard.ui.filesystem;

import java.awt.Color;
import java.awt.Paint;
import java.io.File;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.management.MBeanException;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.extensions.markup.html.tree.table.ColumnLocation;
import org.apache.wicket.extensions.markup.html.tree.table.IColumn;
import org.apache.wicket.extensions.markup.html.tree.table.IRenderable;
import org.apache.wicket.extensions.markup.html.tree.table.PropertyRenderableColumn;
import org.apache.wicket.extensions.markup.html.tree.table.PropertyTreeColumn;
import org.apache.wicket.extensions.markup.html.tree.table.ColumnLocation.Alignment;
import org.apache.wicket.extensions.markup.html.tree.table.ColumnLocation.Unit;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.resources.CompressedResourceReference;
import org.apache.wicket.model.ResourceModel;
import org.dcm4chee.dashboard.mbean.DashboardDelegator;
import org.dcm4chee.dashboard.ui.DashboardPanel;
import org.dcm4chee.dashboard.ui.common.DashboardTreeTable;
import org.dcm4chee.dashboard.ui.common.JFreeChartImage;
import org.dcm4chee.icons.ImageManager;
import org.dcm4chee.web.common.base.BaseWicketApplication;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis3D;
import org.jfree.chart.axis.NumberAxis3D;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.StackedBarRenderer3D;
import org.jfree.data.Range;
import org.jfree.data.category.DefaultCategoryDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision$ $Date$
 * @since 15.11.2009
 */
public class FileSystemPanel extends Panel {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(FileSystemPanel.class);

    private static final ResourceReference CSS = new CompressedResourceReference(FileSystemPanel.class, "filesystem-style.css");
    
    public FileSystemPanel(String id) {
        super(id);
        
        if (FileSystemPanel.CSS != null)
            add(CSSPackageResource.getHeaderContribution(FileSystemPanel.CSS));
    }
    
    @Override
    public void onBeforeRender() {
        super.onBeforeRender();

        try {
            DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(new FileSystemModel());
            for (String groupname : DashboardDelegator.getInstance((((BaseWicketApplication) getApplication()).getInitParameter("DashboardServiceName"))).listAllFileSystemGroups()) {
                FileSystemModel group = new FileSystemModel();
                
                int index = groupname.indexOf("group=");
                if (index < 0) continue;
                group.setDirectoryPath(groupname.substring(index + 6));
                group.setDescription(groupname + ",AET=" + DashboardDelegator.getInstance((((BaseWicketApplication) getApplication()).getInitParameter("DashboardServiceName"))).getDefaultRetrieveAETitle(groupname));
                group.setGroup(true);
                DefaultMutableTreeNode groupNode;
                rootNode.add(groupNode = new DefaultMutableTreeNode(group));

                File[] fileSystems = null;
                try {
                    fileSystems = DashboardDelegator.getInstance((((BaseWicketApplication) getApplication()).getInitParameter("DashboardServiceName"))).listFileSystemsOfGroup(groupname);
                } catch (MBeanException mbe) {
                }
                
                if (!((fileSystems == null) || (fileSystems.length == 0))) {
                    long minBytesFree = DashboardDelegator.getInstance((((BaseWicketApplication) getApplication()).getInitParameter("DashboardServiceName"))).getMinimumFreeDiskSpaceOfGroup(groupname);
                    
                    for (File file : fileSystems) {
                        FileSystemModel fsm = new FileSystemModel();
                        fsm.setDirectoryPath(file.getName());                            
                        fsm.setDescription(file.getName().startsWith("tar:") ? file.getName() : file.getAbsolutePath());
                        fsm.setOverallDiskSpace(file.getTotalSpace() / FileSystemModel.MEGA);
                        fsm.setUsedDiskSpace(Math.max((file.getTotalSpace() - file.getUsableSpace()) / FileSystemModel.MEGA, 0));
                        fsm.setFreeDiskSpace(Math.max(file.getUsableSpace() / FileSystemModel.MEGA, 0));
                        fsm.setMinimumFreeDiskSpace(fsm.getOverallDiskSpaceLong() == 0 ? 0 : minBytesFree / FileSystemModel.MEGA);
                        fsm.setUsableDiskSpace(Math.max((file.getUsableSpace() - minBytesFree) / FileSystemModel.MEGA, 0));
                        fsm.setRemainingTime(Math.max((file.getUsableSpace() - minBytesFree) / 
                                DashboardDelegator.getInstance((((BaseWicketApplication) getApplication()).getInitParameter("DashboardServiceName"))).getExpectedDataVolumePerDay(groupname), 0));
                        
                        group.setOverallDiskSpace(group.getOverallDiskSpaceLong() + fsm.getOverallDiskSpaceLong());
                        group.setUsedDiskSpace(group.getUsedDiskSpaceLong() + fsm.getUsedDiskSpaceLong());
                        group.setFreeDiskSpace(group.getFreeDiskSpaceLong() + fsm.getFreeDiskSpaceLong());
                        group.setMinimumFreeDiskSpace(group.getMinimumFreeDiskSpaceLong() + fsm.getMinimumFreeDiskSpaceLong());
                        group.setUsableDiskSpace(group.getUsableDiskSpaceLong() + fsm.getUsableDiskSpaceLong());
                        group.setRemainingTime(group.getRemainingTime() + fsm.getRemainingTime());
                        groupNode.add(new DefaultMutableTreeNode(fsm));
                    }
                }
            }

            String[] otherFileSystems = DashboardDelegator.getInstance((((BaseWicketApplication) getApplication()).getInitParameter("DashboardServiceName"))).listOtherFileSystems();
            if (otherFileSystems != null && otherFileSystems.length > 0) {

                FileSystemModel group = new FileSystemModel();
                group.setDirectoryPath(new ResourceModel("dashboard.filesystem.group.other").wrapOnAssignment(this).getObject());
                group.setGroup(true);
                group.setRemainingTime(-1);
                DefaultMutableTreeNode groupNode;
                rootNode.add(groupNode = new DefaultMutableTreeNode(group));

                for (String otherFileSystem : otherFileSystems) {
                    File file = new File(otherFileSystem);
                    FileSystemModel fsm = new FileSystemModel();
                    fsm.setDirectoryPath(file.getAbsolutePath());                            
                    fsm.setDescription(file.getName().startsWith("tar:") ? file.getName() : file.getAbsolutePath());
                    fsm.setOverallDiskSpace(file.getTotalSpace() / FileSystemModel.MEGA);
                    fsm.setUsedDiskSpace(Math.max((file.getTotalSpace() - file.getUsableSpace()) / FileSystemModel.MEGA, 0));
                    fsm.setFreeDiskSpace(Math.max(file.getUsableSpace() / FileSystemModel.MEGA, 0));
                    fsm.setMinimumFreeDiskSpace(fsm.getOverallDiskSpaceLong() / FileSystemModel.MEGA);
                    fsm.setUsableDiskSpace(Math.max(file.getUsableSpace() / FileSystemModel.MEGA, 0));
                    fsm.setRemainingTime(-1);
                    
                    group.setOverallDiskSpace(group.getOverallDiskSpaceLong() + fsm.getOverallDiskSpaceLong());
                    group.setUsedDiskSpace(group.getUsedDiskSpaceLong() + fsm.getUsedDiskSpaceLong());
                    group.setFreeDiskSpace(group.getFreeDiskSpaceLong() + fsm.getFreeDiskSpaceLong());
                    group.setMinimumFreeDiskSpace(group.getMinimumFreeDiskSpaceLong() + fsm.getMinimumFreeDiskSpaceLong());
                    group.setUsableDiskSpace(group.getUsableDiskSpaceLong() + fsm.getUsableDiskSpaceLong());
                    group.setVisible(false);
                    groupNode.add(new DefaultMutableTreeNode(fsm));
                }
            }
            
            FileSystemTreeTable fileSystemTreeTable = new FileSystemTreeTable("filesystem-tree-table", 
                    new DefaultTreeModel(rootNode), new IColumn[] {
                new PropertyTreeColumn(new ColumnLocation(
                        Alignment.LEFT, 25, Unit.PERCENT), 
                        new ResourceModel(
                                "dashboard.filesystem.table.column.name").wrapOnAssignment(this).getObject(), 
                                "userObject.directoryPath"),
                new ImageRenderableColumn(new ColumnLocation(
                        Alignment.MIDDLE, 30, Unit.PROPORTIONAL), 
                        new ResourceModel(
                                "dashboard.filesystem.table.column.image").wrapOnAssignment(this).getObject(),
                                "userObject.directoryPath"),
                new PropertyRenderableColumn(new ColumnLocation(
                        Alignment.RIGHT, 7, Unit.PERCENT),
                        new ResourceModel(
                                "dashboard.filesystem.table.column.overall").wrapOnAssignment(this).getObject(),
                                "userObject.overallDiskSpaceString"), 
                new PropertyRenderableColumn(new ColumnLocation(
                        Alignment.RIGHT, 7, Unit.PERCENT),
                        new ResourceModel(
                                "dashboard.filesystem.table.column.used").wrapOnAssignment(this).getObject(),
                                "userObject.usedDiskSpaceString"),
                new PropertyRenderableColumn(new ColumnLocation(
                        Alignment.RIGHT, 7, Unit.PERCENT),
                        new ResourceModel(
                                "dashboard.filesystem.table.column.free").wrapOnAssignment(this).getObject(),
                                "userObject.freeDiskSpaceString"),
                new PropertyRenderableColumn(new ColumnLocation(
                        Alignment.RIGHT, 7, Unit.PERCENT),
                        new ResourceModel(
                                "dashboard.filesystem.table.column.minimumfree").wrapOnAssignment(this).getObject(),
                                "userObject.minimumFreeDiskSpaceString"),
                new PropertyRenderableColumn(new ColumnLocation(
                        Alignment.RIGHT, 7, Unit.PERCENT),
                        new ResourceModel(
                                "dashboard.filesystem.table.column.usable").wrapOnAssignment(this).getObject(),
                                "userObject.usableDiskSpaceString"), 
                new PropertyRenderableColumn(new ColumnLocation(
                        Alignment.RIGHT, 10, Unit.PERCENT),
                        new ResourceModel(
                                "dashboard.filesystem.table.column.remainingtime").wrapOnAssignment(this).getObject(),
                                "userObject.remainingTimeString")
            });
            fileSystemTreeTable.getTreeState().setAllowSelectMultiple(true);
            fileSystemTreeTable.getTreeState().collapseAll();
            fileSystemTreeTable.setRootLess(true);
            addOrReplace(fileSystemTreeTable);
        } catch (Exception e) {
            log.error(this.getClass().toString() + ": " + "onBeforeRender: " + e.getMessage());
            log.debug("Exception: ", e);
            throw new WicketRuntimeException(e.getLocalizedMessage(), e);
        }
    }

    private class ImageRenderableColumn extends PropertyTreeColumn {

        private static final long serialVersionUID = 1L;

        public ImageRenderableColumn(ColumnLocation location, String header, String propertyExpression) {
            super(location, header, propertyExpression);
        }

        @Override
        public IRenderable newCell(javax.swing.tree.TreeNode node, int level) {
            return null;
        }
        
        @Override
        public Component newCell(MarkupContainer parent, String id, final TreeNode node, int level) {
            if (!((node instanceof DefaultMutableTreeNode) && (((DefaultMutableTreeNode) node)
                    .getUserObject() instanceof FileSystemModel)))
                return null;

            if (!((FileSystemModel) ((DefaultMutableTreeNode) node).getUserObject()).isVisible())
                return new Label("image") {
                
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onComponentTag(ComponentTag tag) {
                        super.onComponentTag(tag);
                    }
                };

            final FileSystemModel fsm = (FileSystemModel) ((DefaultMutableTreeNode) node).getUserObject();

            boolean noDiskSpace = fsm.getOverallDiskSpaceLong() == 0f;
            float used = noDiskSpace ? 0f : (100f * fsm.getUsedDiskSpaceLong()) / fsm.getOverallDiskSpaceLong();
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            dataset.addValue(noDiskSpace ? 0f : used, new Integer(1), "");
            dataset.addValue(noDiskSpace ? 0f : 
                fsm.getMinimumFreeDiskSpaceLong() == 0f ? (100f - used) : 
                    (100f * fsm.getUsableDiskSpaceLong() / fsm.getOverallDiskSpaceLong()), new Integer(2), "");
            dataset.addValue(noDiskSpace ? 0f : (100f * 
                    Math.min(fsm.getMinimumFreeDiskSpaceLong(), fsm.getFreeDiskSpaceLong())
                        / fsm.getOverallDiskSpaceLong()), new Integer(3), "");

            final JFreeChart chart = ChartFactory.createStackedBarChart3D(
                    null, null, null, dataset, PlotOrientation.HORIZONTAL, false,
                    false, false);
            chart.setBackgroundPaint(new Color(0, 0, 0, 0));
            
            final CategoryPlot plot = chart.getCategoryPlot();
            plot.setBackgroundPaint(Color.white);
            plot.setForegroundAlpha(0.7f);

            NumberAxis3D numberaxis = new NumberAxis3D();
            numberaxis.setRange(new Range(0, 100));            
            plot.setRangeAxis(numberaxis);

            CategoryAxis3D categoryaxis = new CategoryAxis3D();
            categoryaxis.setLabel("%");
            plot.setDomainAxis(categoryaxis);
            StackedBarRenderer3D renderer = new StackedBarRenderer3D(10, 10) {

                private static final long serialVersionUID = 1L;

                @Override
                public Paint getItemPaint(final int row, final int column) {
                    if (fsm.getOverallDiskSpaceLong() <= 0) return Color.white;
                    return (row == 0) ? Color.red : (row == 1) ? Color.green
                            : (row == 2) ? Color.yellow : Color.white;
                }
            };
            renderer.setBaseItemLabelsVisible(false);
            plot.setRenderer(renderer);

            return 
            new JFreeChartImage("image", chart, 350, 50) {

                private static final long serialVersionUID = 1L;

                @Override
                protected void onComponentTag(ComponentTag tag) {
                    tag.setName("img");
                    super.onComponentTag(tag);
                }
            };
        }
    }

    private class FileSystemTreeTable extends DashboardTreeTable {

        private static final long serialVersionUID = 1L;
        
        public FileSystemTreeTable(String id, TreeModel model, IColumn[] columns) {
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
                            (((FileSystemModel) ((DefaultMutableTreeNode) node).getUserObject()).isGroup() ?  
                                    getRequestCycle().urlFor(ImageManager.IMAGE_DASHBOARD_FILESYSTEM_GROUP) :
                                    ((FileSystemModel) ((DefaultMutableTreeNode) node).getUserObject()).getDirectoryPath().contains("tar:") ?
                                            getRequestCycle().urlFor(ImageManager.IMAGE_DASHBOARD_FILESYSTEM_TAR) : 
                                            getRequestCycle().urlFor(ImageManager.IMAGE_DASHBOARD_FILESYSTEM))
                                            + "')"
                    );
                    tag.put("title", ((FileSystemModel) ((DefaultMutableTreeNode) node).getUserObject()).getDescription());
                }
            };
        }        
    };

    protected class FileSystemModel implements Serializable {

        private static final long serialVersionUID = -1L;

        public static final int KILO = 1000;
        public static final int MEGA = 1000000;
        
        private final int diskSpaceDisplayLength = 8;
        
        private NumberFormat memoryFormatter;
        private NumberFormat daysFormatter;
        
        private String directoryPath;
        private String description;
        
        private long overallDiskSpace = 0;
        private long usedDiskSpace = 0;
        private long usableDiskSpace = 0;
        private long freeDiskSpace = 0;
        private long minimumFreeDiskSpace = 0;
        
        private boolean isGroup = false;
        private boolean visible = true;
        
        private long remainingTime = 0;
        
        public FileSystemModel() {
            this.memoryFormatter = DecimalFormat.getInstance();
            this.memoryFormatter.setMaximumFractionDigits(3);
            this.memoryFormatter.setMinimumIntegerDigits(1);

            this.daysFormatter = DecimalFormat.getInstance();
            this.daysFormatter.setMaximumFractionDigits(0);
            this.daysFormatter.setMinimumIntegerDigits(1);
            this.daysFormatter.setGroupingUsed(false);
        }

        public void setDirectoryPath(String directoryPath) {
            this.directoryPath = directoryPath;
        }

        public String getDirectoryPath() {
            return directoryPath;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        public void setOverallDiskSpace(long overallDiskSpace) {
            this.overallDiskSpace = overallDiskSpace;
        }

        public long getOverallDiskSpaceLong() {
            return this.overallDiskSpace;
        }

        public String getOverallDiskSpaceString() {
            String overallDiskSpaceString = this.memoryFormatter.format(new Float(this.overallDiskSpace)/FileSystemModel.KILO);
            return overallDiskSpaceString.substring(0, Math.min(overallDiskSpaceString.length(), this.diskSpaceDisplayLength)) + " GB";
        }

        public void setUsedDiskSpace(long usedDiskSpace) {
            this.usedDiskSpace = usedDiskSpace;
        }

        public long getUsedDiskSpaceLong() {
            return this.usedDiskSpace;
        }

        public String getUsedDiskSpaceString() {
            String usedDiskSpaceString = this.memoryFormatter.format(new Float(this.usedDiskSpace)/FileSystemModel.KILO);
            return usedDiskSpaceString.substring(0, Math.min(usedDiskSpaceString.length(), this.diskSpaceDisplayLength)) + " GB";
        }

        public void setUsableDiskSpace(long usableDiskSpace) {
            this.usableDiskSpace = usableDiskSpace;
        }

        public long getUsableDiskSpaceLong() {
            return this.usableDiskSpace;
        }

        public String getUsableDiskSpaceString() {
            String usableDiskSpaceString = this.memoryFormatter.format(new Float(this.usableDiskSpace)/FileSystemModel.KILO);
            return usableDiskSpaceString.substring(0, Math.min(usableDiskSpaceString.length(), this.diskSpaceDisplayLength)) + " GB";
        }

        public void setFreeDiskSpace(long freeDiskSpace) {
            this.freeDiskSpace = freeDiskSpace;
        }

        public long getFreeDiskSpaceLong() {
            return this.freeDiskSpace;
        }

        public String getFreeDiskSpaceString() {
            String freeDiskSpaceString = this.memoryFormatter.format(new Float(this.freeDiskSpace)/FileSystemModel.KILO);
            return freeDiskSpaceString.substring(0, Math.min(freeDiskSpaceString.length(), this.diskSpaceDisplayLength)) + " GB";
        }

        public void setMinimumFreeDiskSpace(long minimumFreeDiskSpace) {
            this.minimumFreeDiskSpace = minimumFreeDiskSpace;
        }

        public long getMinimumFreeDiskSpaceLong() {
            return this.minimumFreeDiskSpace;
        }

        public String getMinimumFreeDiskSpaceString() {
            String minimumFreeDiskSpaceString = this.memoryFormatter.format(new Float(this.minimumFreeDiskSpace)/FileSystemModel.KILO);
            return minimumFreeDiskSpaceString.substring(0, Math.min(minimumFreeDiskSpaceString.length(), this.diskSpaceDisplayLength)) + " GB";
        }

        public void setGroup(boolean isGroup) {
            this.isGroup = isGroup;
        }

        public boolean isGroup() {
            return isGroup;
        }

        public void setVisible(boolean visible) {
            this.visible = visible;
        }

        public boolean isVisible() {
            return visible;
        }

        public void setRemainingTime(long l) {
            this.remainingTime = l;
        }

        public long getRemainingTime() {
            return remainingTime;
        }
        
        public String getRemainingTimeString() {
            return this.remainingTime >= 0 ? "~ " + this.daysFormatter.format(new Float(this.remainingTime)) : ""; 
        }
    }
    
    public static String getModuleName() {
        return "dashboard.filesystem";
    }    
}