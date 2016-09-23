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

package org.dcm4chee.dashboard.ui.report;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.tree.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.tree.table.AbstractRenderableColumn;
import org.apache.wicket.extensions.markup.html.tree.table.ColumnLocation;
import org.apache.wicket.extensions.markup.html.tree.table.IColumn;
import org.apache.wicket.extensions.markup.html.tree.table.IRenderable;
import org.apache.wicket.extensions.markup.html.tree.table.PropertyTreeColumn;
import org.apache.wicket.extensions.markup.html.tree.table.ColumnLocation.Alignment;
import org.apache.wicket.extensions.markup.html.tree.table.ColumnLocation.Unit;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.resources.CompressedResourceReference;
import org.apache.wicket.model.ResourceModel;
import org.dcm4chee.dashboard.mbean.DashboardDelegator;
import org.dcm4chee.dashboard.model.ReportModel;
import org.dcm4chee.dashboard.ui.DashboardPanel;
import org.dcm4chee.dashboard.ui.common.DashboardTreeTable;
import org.dcm4chee.dashboard.ui.config.delegate.DashboardCfgDelegate;
import org.dcm4chee.icons.ImageManager;
import org.dcm4chee.icons.behaviours.ImageSizeBehaviour;
import org.dcm4chee.web.common.base.BaseWicketApplication;
import org.dcm4chee.web.common.behaviours.TooltipBehaviour;
import org.dcm4chee.web.common.markup.ModalWindowLink;
import org.dcm4chee.web.common.secure.SecurityBehavior;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision$ $Date$
 * @since 24.11.2009
 */
public class ReportPanel extends Panel {

    private static final long serialVersionUID = 1L;
    
    private static Logger log = LoggerFactory.getLogger(ReportPanel.class);

    private ModalWindow createGroupWindow;
    private ModalWindow modalWindow;
    
    public ReportPanel(String id) {
        super(id);

        add(createGroupWindow = new ModalWindow("create-group-window")
            .setPageCreator(new ModalWindow.PageCreator() {
                
                private static final long serialVersionUID = 1L;
                  
                @Override
                public Page createPage() {
                    return new CreateGroupPage(createGroupWindow);
                }
            })
        );       

        int[] winSize = DashboardCfgDelegate.getInstance().getWindowSize("createGroup");
        add(new ModalWindowLink("toggle-group-form-link", createGroupWindow, winSize[0],winSize[1])
        .add(new Image("toggle-group-form-image", ImageManager.IMAGE_COMMON_ADD)
        .add(new ImageSizeBehaviour("vertical-align: middle;")))
        .add(new Label("dashboard.report.add-group-form.title", new ResourceModel("dashboard.report.add-group-form.title")))
        .add(new TooltipBehaviour("dashboard.report."))
        .add(new SecurityBehavior(getModuleName() + ":newGroupLink"))
        );
        
        add(modalWindow = new ModalWindow("modal-window"));
    }
    
    @Override
    public void onBeforeRender() {
        super.onBeforeRender();
        
        try {
            DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(new ReportModel());
            
            for (ReportModel group : DashboardDelegator.getInstance((((BaseWicketApplication) getApplication()).getInitParameter("DashboardServiceName"))).listAllReports(true)) {
                DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode();
                groupNode.setUserObject(group);
                rootNode.add(groupNode);
    
                for (ReportModel report : DashboardDelegator.getInstance((((BaseWicketApplication) getApplication()).getInitParameter("DashboardServiceName"))).listAllReports(false)) {
                    if (report.getGroupUuid() != null && report.getGroupUuid().equals(group.getUuid())) {
                        DefaultMutableTreeNode reportNode = new DefaultMutableTreeNode();
                        reportNode.setUserObject(report);
                        groupNode.add(reportNode);
                    }
                }
            }

            ReportTreeTable reportTreeTable = new ReportTreeTable("report-tree-table", 
                    new DefaultTreeModel(rootNode), new IColumn[] {
                        new PropertyTreeColumn(new ColumnLocation(Alignment.LEFT, 44, Unit.PERCENT), 
                                new ResourceModel("dashboard.report.table.title_title").wrapOnAssignment(this).getObject(),  
                                "userObject.title")
                        ,
                        new DynamicRenderableColumn(new ColumnLocation(Alignment.RIGHT, 10, Unit.PERCENT), 
                                new ResourceModel("dashboard.report.table.link.edit").wrapOnAssignment(this).getObject(),  
                                "CreateOrEditReportLink",
                                this.modalWindow)
                        ,
                        new DynamicRenderableColumn(new ColumnLocation(Alignment.RIGHT, 10, Unit.PERCENT), 
                                new ResourceModel("dashboard.report.table.link.remove").wrapOnAssignment(this).getObject(),  
                                "RemoveLink", 
                                this.modalWindow)
                        ,
                        new DynamicRenderableColumn(new ColumnLocation(Alignment.RIGHT, 10, Unit.PERCENT), 
                                new ResourceModel("dashboard.report.table.link.diagram").wrapOnAssignment(this).getObject(),  
                                "DisplayDiagramLink", 
                                this.modalWindow)
                        ,
                        new DynamicRenderableColumn(new ColumnLocation(Alignment.RIGHT, 10, Unit.PERCENT), 
                                new ResourceModel("dashboard.report.table.link.table").wrapOnAssignment(this).getObject(),  
                                "DisplayTableLink", 
                                this.modalWindow)
                        ,
                        new DynamicRenderableColumn(new ColumnLocation(Alignment.RIGHT, 15, Unit.PERCENT), 
                                new ResourceModel("dashboard.report.table.link.diagram+table").wrapOnAssignment(this).getObject(),  
                                "DisplayDiagramAndTableLink", 
                                this.modalWindow)
                    }
            );
            reportTreeTable.setRootLess(true);
            reportTreeTable.getTreeState().expandAll();
            reportTreeTable.getTreeState().setAllowSelectMultiple(false);
            addOrReplace(reportTreeTable);            
        } catch (Exception e) {
            log.error(this.getClass().toString() + ": " + "onBeforeRender: " + e.getMessage());
            log.debug("Exception: ", e);
            throw new WicketRuntimeException(e.getLocalizedMessage(), e);
        }
    }

    private class ReportTreeTable extends DashboardTreeTable {

        private static final long serialVersionUID = 1L;

        public ReportTreeTable(String id, TreeModel model, IColumn[] columns) {
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
                            ((((ReportModel) ((DefaultMutableTreeNode) node).getUserObject()).getGroupUuid() == null) ? 
                                    getRequestCycle().urlFor(ImageManager.IMAGE_DASHBOARD_REPORT_FOLDER) 
                                    : getRequestCycle().urlFor(ImageManager.IMAGE_DASHBOARD_REPORT))
                                    +"')"
                    );                    
                    tag.put("title", ((ReportModel) ((DefaultMutableTreeNode) node).getUserObject()).getStatement());
                }
            };
        }        
    };

    private class DynamicRenderableColumn extends AbstractColumn {

        private static final long serialVersionUID = 1L;

        private String className;
        private ModalWindow modalWindow;

        public DynamicRenderableColumn(ColumnLocation location, String header, String className, ModalWindow modalWindow) {
            super(location, header);
            
            this.className = className;
            this.modalWindow = modalWindow;
        }

        @Override
        public IRenderable newCell(TreeNode node, int level) {
            return null;
        }

        @Override
        public Component newCell(MarkupContainer parent, String id, final TreeNode node, int level) {
            return new DynamicLinkPanel(id, this.className, (ReportModel) ((DefaultMutableTreeNode) node).getUserObject(), this.modalWindow);
        }
    }
    
    public static String getModuleName() {
        return "dashboard.report";
    }
}