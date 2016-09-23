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

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Page;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.dcm4chee.dashboard.mbean.DashboardDelegator;
import org.dcm4chee.dashboard.model.ReportModel;
import org.dcm4chee.dashboard.ui.config.delegate.DashboardCfgDelegate;
import org.dcm4chee.icons.ImageManager;
import org.dcm4chee.icons.behaviours.ImageSizeBehaviour;
import org.dcm4chee.web.common.base.BaseWicketApplication;
import org.dcm4chee.web.common.secure.SecurityBehavior;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision$ $Date$
 * @since 18.01.2010
 */
public class DynamicLinkPanel extends Panel {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(DynamicLinkPanel.class);

    private Link<Object> link;
    
    private ReportModel report;
    private Object window;

    @SuppressWarnings("unchecked")
    public DynamicLinkPanel(String id, String className, final ReportModel report, ModalWindow window) {
        super(id);

        this.report = report;
        this.window = window;

        try {
            add((this.link = (Link<Object>) ((Class<? extends Link<Object>>) Class.forName(this.getClass().getName() + "$" + className)).getConstructors()[0].newInstance(new Object[] {
                 this, 
                 "report-table-link", 
                 report, 
                 this.window
            }))
            .add(new Image("image")
            .add(new ImageSizeBehaviour())
            .add(new AttributeModifier("src", true, new AbstractReadOnlyModel<CharSequence>() {
    
                private static final long serialVersionUID = 1L;
    
                @Override
                public CharSequence getObject() {
                    return (link instanceof org.dcm4chee.dashboard.ui.report.DynamicLinkPanel.CreateOrEditReportLink) ? 
                        (report == null || report.getGroupUuid() == null) ? 
                                getRequestCycle().urlFor(ImageManager.IMAGE_DASHBOARD_REPORT_ADD) :
                                    getRequestCycle().urlFor(ImageManager.IMAGE_DASHBOARD_REPORT_EDIT) : 
                    (link instanceof org.dcm4chee.dashboard.ui.report.DynamicLinkPanel.RemoveLink) ? 
                            getRequestCycle().urlFor(ImageManager.IMAGE_COMMON_REMOVE) : 
                    (link instanceof org.dcm4chee.dashboard.ui.report.DynamicLinkPanel.DisplayDiagramLink) ?
                            getRequestCycle().urlFor(ImageManager.IMAGE_DASHBOARD_REPORT_CHART) : 
                    (link instanceof org.dcm4chee.dashboard.ui.report.DynamicLinkPanel.DisplayTableLink) ?
                            getRequestCycle().urlFor(ImageManager.IMAGE_DASHBOARD_REPORT_TABLE) : 
                    (link instanceof org.dcm4chee.dashboard.ui.report.DynamicLinkPanel.DisplayDiagramAndTableLink) ? 
                            getRequestCycle().urlFor(ImageManager.IMAGE_DASHBOARD_REPORT_CHART_AND_TABLE) : 
                    "";
                }
            }))));
            
            this.link.setVisible(
                    (report != null) && (
                        this.link instanceof org.dcm4chee.dashboard.ui.report.DynamicLinkPanel.CreateOrEditReportLink
                        || this.link instanceof org.dcm4chee.dashboard.ui.report.DynamicLinkPanel.RemoveLink
                        || (
                            ((report.getDataSource() != null && !report.getDataSource().equals("")) && (
                                (this.link instanceof org.dcm4chee.dashboard.ui.report.DynamicLinkPanel.DisplayDiagramLink
                                        && report.getDiagram() != null)
                                || (this.link instanceof org.dcm4chee.dashboard.ui.report.DynamicLinkPanel.DisplayTableLink
                                        && report.getTable())
                                || (this.link instanceof org.dcm4chee.dashboard.ui.report.DynamicLinkPanel.DisplayDiagramAndTableLink
                                        && (report.getDiagram() != null || report.getTable()))
                            ))
                        )
                    )
            );
            this.link.add(new SecurityBehavior(ReportPanel.getModuleName() + ":" + className));
        } catch (Exception e) {
            log.error(this.getClass().toString() + ": " + "init: " + e.getMessage());
            log.debug("Exception: ", e);
            throw new WicketRuntimeException(e.getLocalizedMessage(), e);
        }
    }

    @Override
    public void onBeforeRender() {
        super.onBeforeRender();
        
        if (this.link == null) return;
        
        this.link.get("image").add(new AttributeModifier("title", true, 
                (this.report.getGroupUuid() == null ? 
                        this.link instanceof org.dcm4chee.dashboard.ui.report.DynamicLinkPanel.CreateOrEditReportLink ?
                                new ResourceModel("dashboard.dynamiclink.report.create" ).wrapOnAssignment(this) :
                                this.link instanceof org.dcm4chee.dashboard.ui.report.DynamicLinkPanel.RemoveLink ? 
                                        new ResourceModel("dashboard.dynamiclink.report.group.remove").wrapOnAssignment(this) : 
                                        new Model<String>("")
                        : 
                        this.link instanceof org.dcm4chee.dashboard.ui.report.DynamicLinkPanel.CreateOrEditReportLink ? 
                                new ResourceModel("dashboard.dynamiclink.report.edit").wrapOnAssignment(this) : 
                                this.link instanceof org.dcm4chee.dashboard.ui.report.DynamicLinkPanel.RemoveLink ?
                                        new ResourceModel("dashboard.dynamiclink.report.remove").wrapOnAssignment(this) : 
                                        new Model<String>("")
        )));
        
        if (this.link instanceof RemoveLink)
                this.link.add(new AttributeModifier("onclick", true, new Model<String>("return confirm('" + new ResourceModel(
                        this.report.getGroupUuid() == null ? 
                                "dashboard.dynamiclink.report.group.remove_confirmation" : 
                                "dashboard.dynamiclink.report.remove_confirmation"
                ).wrapOnAssignment(this).getObject() + "');")));
    }

    abstract private class AjaxDisplayLink extends AjaxFallbackLink<Object> {
        
        private static final long serialVersionUID = 1L;
        
        ReportModel report;
        ModalWindow modalWindow;
        
        public AjaxDisplayLink(String id, ReportModel report, ModalWindow modalWindow) {
            super(id);

            this.report = report;
            this.modalWindow = modalWindow;
        }
        
        void setAjaxDisplayProperties() {
            
            this.modalWindow.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {              
                
                private static final long serialVersionUID = 1L;

                @Override
                public void onClose(AjaxRequestTarget target) {
                    modalWindow.getPage().setOutputMarkupId(true);
                    target.addComponent(modalWindow.getPage());
                }
            });
        }
        
        class DisableDefaultConfirmBehavior extends AbstractBehavior implements IHeaderContributor {

            private static final long serialVersionUID = 1L;

            @Override
            public void renderHead(IHeaderResponse response) {
                response.renderOnDomReadyJavascript ("Wicket.Window.unloadConfirmation = false");
            }
        }
    }    

    abstract private class DisplayLink extends Link<Object> {
            
        private static final long serialVersionUID = 1L;
      
        ReportModel report;
        public DisplayLink(String id, ReportModel report, ModalWindow modalWindow) {
            super(id);

            this.report = report;
        }
    }

    private class CreateOrEditReportLink extends AjaxDisplayLink {
        
        private static final long serialVersionUID = 1L;
        
        public CreateOrEditReportLink(String id, ReportModel report, ModalWindow modalWindow) {
            super(id, report, modalWindow);       
        }
        
        @Override
        public void onClick(AjaxRequestTarget target) {

            setAjaxDisplayProperties();
            
            this.modalWindow.setPageCreator(new ModalWindow.PageCreator() {
                  
                private static final long serialVersionUID = 1L;
                  
                @Override
                public Page createPage() {
                    return new CreateOrEditReportPage(modalWindow, report);                        
                }
            });
            
            int[] winSize = DashboardCfgDelegate.getInstance().getWindowSize("editReport");
            ((ModalWindow) this.modalWindow.add(new DisableDefaultConfirmBehavior()))
            .setInitialWidth(winSize[0])
            .setInitialHeight(winSize[1])
            .show(target);
        }
    }    

    private class RemoveLink extends DisplayLink {
        
        private static final long serialVersionUID = 1L;
        
        public RemoveLink(String id, ReportModel report, ModalWindow modalWindow) {
            super(id, report, modalWindow);
        }

        @Override
        public void onClick() {
            try {
                DashboardDelegator.getInstance((((BaseWicketApplication) getApplication()).getInitParameter("DashboardServiceName"))).deleteReport(this.report, this.report.getGroupUuid() == null);
            } catch (Exception e) {
                log.error(this.getClass().toString() + ": " + "onBeforeRender: " + e.getMessage());
                log.debug("Exception: ", e);
                throw new WicketRuntimeException(e.getLocalizedMessage(), e); 
            }
        }
    }

    private class DisplayDiagramLink extends AjaxDisplayLink {

        private static final long serialVersionUID = 1L;

        public DisplayDiagramLink(String id, ReportModel report, ModalWindow modalWindow) {
            super(id, report, modalWindow);
        }
        
        @Override
        public void onClick(AjaxRequestTarget target) {

            setAjaxDisplayProperties();
            
            this.modalWindow.setPageCreator(new ModalWindow.PageCreator() {
                  
                private static final long serialVersionUID = 1L;
                  
                @Override
                public Page createPage() {
                    return new ConfigureReportPage(modalWindow, report, true, false);
                }                
            });

            int[] winSize = DashboardCfgDelegate.getInstance().getWindowSize("reportDiagram");
            ((ModalWindow) this.modalWindow.add(new DisableDefaultConfirmBehavior()))
            .setInitialWidth(winSize[0])
            .setInitialHeight(winSize[1])
            .show(target);
        }        
    }

    private class DisplayTableLink extends AjaxDisplayLink {

        private static final long serialVersionUID = 1L;

        public DisplayTableLink(String id, ReportModel report, ModalWindow modalWindow) {
            super(id, report, modalWindow);
        }
        
        @Override
        public void onClick(AjaxRequestTarget target) {

            setAjaxDisplayProperties();
            
            this.modalWindow.setPageCreator(new ModalWindow.PageCreator() {
                  
                private static final long serialVersionUID = 1L;
                  
                @Override
                public Page createPage() {
                    return new ConfigureReportPage(modalWindow, report, false, true);
                }                
            });

            int[] winSize = DashboardCfgDelegate.getInstance().getWindowSize("reportTable");
            ((ModalWindow) this.modalWindow.add(new DisableDefaultConfirmBehavior()))
            .setInitialWidth(winSize[0])
            .setInitialHeight(winSize[1])
            .show(target);
        }        
    }

    private class DisplayDiagramAndTableLink extends AjaxDisplayLink {

        private static final long serialVersionUID = 1L;

        public DisplayDiagramAndTableLink(String id, ReportModel report, ModalWindow modalWindow) {
            super(id, report, modalWindow);
        }
        
        @Override
        public void onClick(AjaxRequestTarget target) {

            setAjaxDisplayProperties();
            
            this.modalWindow.setPageCreator(new ModalWindow.PageCreator() {
                  
                private static final long serialVersionUID = 1L;
                  
                @Override
                public Page createPage() {
                    return new ConfigureReportPage(modalWindow, report, true, true);
                }                
            });
            
            int[] winSize = DashboardCfgDelegate.getInstance().getWindowSize("reportDiagramandtable");
            ((ModalWindow) this.modalWindow.add(new DisableDefaultConfirmBehavior()))
            .setInitialWidth(winSize[0])
            .setInitialHeight(winSize[1])
            .show(target);
        }
    }
}
