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

package org.dcm4chee.dashboard.ui.report.display;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.IRequestTarget;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.protocol.http.WebResponse;
import org.dcm4chee.dashboard.model.ReportModel;
import org.dcm4chee.dashboard.ui.common.JFreeChartImage;
import org.dcm4chee.dashboard.ui.config.delegate.DashboardCfgDelegate;
import org.dcm4chee.dashboard.ui.util.DatabaseUtils;
import org.dcm4chee.icons.ImageManager;
import org.dcm4chee.icons.behaviours.ImageSizeBehaviour;
import org.dcm4chee.web.common.behaviours.TooltipBehaviour;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisState;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryTick;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot3D;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.CategoryStepRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.text.TextBlock;
import org.jfree.ui.RectangleEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision$ $Date$
 * @since 18.11.2009
 */
public class DisplayReportDiagramPanel extends Panel {
    
    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(DisplayReportDiagramPanel.class);

    private ReportModel report;
    private Map<String, String> parameters;

    public DisplayReportDiagramPanel(String id, ReportModel report, Map<String, String> parameters) {
        super(id);
        this.report = report;
        this.parameters = parameters;
    }
    
    @Override
    public void onBeforeRender() {
        super.onBeforeRender();

        Connection jdbcConnection = null;
        try {
            if (report == null) throw new Exception("No report given to render diagram");

            jdbcConnection = DatabaseUtils.getDatabaseConnection(report.getDataSource());
            ResultSet resultSet = DatabaseUtils.getResultSet(jdbcConnection, report.getStatement(), parameters);
            
            ResultSetMetaData metaData = resultSet.getMetaData();
            JFreeChart chart = null;
            resultSet.beforeFirst();
            
            // Line chart - 1 numeric value
            if (report.getDiagram() == 0) {
                if (metaData.getColumnCount() != 1) throw new Exception(new ResourceModel("dashboard.report.reportdiagram.image.render.error.1numvalues").wrapOnAssignment(this).getObject());

                DefaultCategoryDataset dataset = new DefaultCategoryDataset();
                while (resultSet.next()) dataset.addValue(resultSet.getDouble(1), metaData.getColumnName(1), String.valueOf(resultSet.getRow()));

                chart = ChartFactory.createLineChart(new ResourceModel("dashboard.report.reportdiagram.image.label").wrapOnAssignment(this).getObject(),
                        new ResourceModel("dashboard.report.reportdiagram.image.row-label").wrapOnAssignment(this).getObject(),
                        metaData.getColumnName(1),
                        dataset,
                        PlotOrientation.VERTICAL,
                        true,
                        true,
                        true);

            // XY Series chart - 2 numeric values
            } else if (report.getDiagram() == 1) {
                if (metaData.getColumnCount() != 2) throw new Exception(new ResourceModel("dashboard.report.reportdiagram.image.render.error.2numvalues").wrapOnAssignment(this).getObject());             

                XYSeries series = new XYSeries(metaData.getColumnName(1) + " / " + metaData.getColumnName(2));
                while (resultSet.next()) series.add(resultSet.getDouble(1), resultSet.getDouble(2));

                chart = ChartFactory.createXYLineChart(new ResourceModel("dashboard.report.reportdiagram.image.label").wrapOnAssignment(this).getObject(), 
                        metaData.getColumnName(1),
                        metaData.getColumnName(2),
                        new XYSeriesCollection(series), 
                        PlotOrientation.VERTICAL,
                        true,
                        true,
                        true);

            // Category chart - 1 numeric value, 1 comparable value
            } else if (report.getDiagram() == 2) {
                if (metaData.getColumnCount() != 2) throw new Exception(new ResourceModel("dashboard.report.reportdiagram.image.render.error.2values").wrapOnAssignment(this).getObject());                

                DefaultCategoryDataset dataset = new DefaultCategoryDataset();
                while (resultSet.next()) dataset.setValue(resultSet.getDouble(1), metaData.getColumnName(1) + " / " + metaData.getColumnName(2), resultSet.getString(2));

                chart = new JFreeChart(new ResourceModel("dashboard.report.reportdiagram.image.label").wrapOnAssignment(this).getObject(), 
                                       new CategoryPlot(dataset,
                                       new LabelAdaptingCategoryAxis(14, metaData.getColumnName(2)),
                                       new NumberAxis(metaData.getColumnName(1)), 
                                       new CategoryStepRenderer(true)));

            // Pie chart - 1 numeric value, 1 comparable value (used as category)
            } else if ((report.getDiagram() == 3) || (report.getDiagram() == 4)) {
                if (metaData.getColumnCount() != 2) throw new Exception(new ResourceModel("dashboard.report.reportdiagram.image.render.error.2values").wrapOnAssignment(this).getObject());

                DefaultPieDataset dataset = new DefaultPieDataset();
                while (resultSet.next()) dataset.setValue(resultSet.getString(2), resultSet.getDouble(1));

                if (report.getDiagram() == 3)
                    // Pie chart 2D
                    chart = ChartFactory.createPieChart(new ResourceModel("dashboard.report.reportdiagram.image.label").wrapOnAssignment(this).getObject(), 
                            dataset,
                            true,
                            true,
                            true);
                else if (report.getDiagram() == 4) {
                    // Pie chart 3D
                    chart = ChartFactory.createPieChart3D(new ResourceModel("dashboard.report.reportdiagram.image.label").wrapOnAssignment(this).getObject(), 
                            dataset,
                            true,
                            true,
                            true);
                    ((PiePlot3D) chart.getPlot()).setForegroundAlpha(Float.valueOf(new ResourceModel("dashboard.report.reportdiagram.image.alpha").wrapOnAssignment(this).getObject()));
                }
                
            // Bar chart - 1 numeric value, 2 comparable values (used as category, series)
            } else if (report.getDiagram() == 5) {
                if ((metaData.getColumnCount() != 2) && (metaData.getColumnCount() != 3)) throw new Exception(new ResourceModel("dashboard.report.reportdiagram.image.render.error.3values").wrapOnAssignment(this).getObject());

                DefaultCategoryDataset dataset = new DefaultCategoryDataset();
                while (resultSet.next()) dataset.setValue(resultSet.getDouble(1), resultSet.getString(2), resultSet.getString(metaData.getColumnCount()));

                chart = ChartFactory.createBarChart(new ResourceModel("dashboard.report.reportdiagram.image.label").wrapOnAssignment(this).getObject(), 
                        metaData.getColumnName(2),
                        metaData.getColumnName(1),
                        dataset,
                        PlotOrientation.VERTICAL,
                        true,
                        true,
                        true);
            }

            int[] winSize = DashboardCfgDelegate.getInstance().getWindowSize("reportDiagramImage");
            addOrReplace(new JFreeChartImage("diagram", 
                                    chart, 
                                    winSize[0], winSize[1])
            );

            final JFreeChart downloadableChart = chart;
            addOrReplace(new Link<Object>("diagram-download") {
                
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick() {        

                    RequestCycle.get().setRequestTarget(new IRequestTarget() {

                        public void respond(RequestCycle requestCycle) {

                            WebResponse wr = (WebResponse) requestCycle.getResponse();
                            wr.setContentType("image/png");
                            wr.setHeader( "content-disposition", "attachment;filename=diagram.png");
                    
                                OutputStream os = wr.getOutputStream();
                                try {
                                    ImageIO.write(downloadableChart.createBufferedImage(800, 600), "png", os);
                                    os.close();
                                } catch (IOException e) {
                                    log.error(this.getClass().toString() + ": " + "respond: " + e.getMessage());
                                    log.debug("Exception: ", e);
                                }
                                wr.close();
                        }

                        @Override
                        public void detach(RequestCycle arg0) {
                        }
                    });
                }
            }
            .add(new Image("diagram-download-image", ImageManager.IMAGE_DASHBOARD_REPORT_DOWNLOAD)
            .add(new ImageSizeBehaviour())
            .add(new TooltipBehaviour("dashboard.report.reportdiagram.", "image.downloadlink"))
                )
            );

            addOrReplace(new Image("diagram-print-image", ImageManager.IMAGE_DASHBOARD_REPORT_PRINT)
            .add(new ImageSizeBehaviour())
            .add(new TooltipBehaviour("dashboard.report.reportdiagram.", "image.printbutton"))
            );

            addOrReplace(new Label("error-message", "").setVisible(false));
            addOrReplace(new Label("error-reason", "").setVisible(false));
        } catch (Exception e) {
            log.error("Exception: " + e.getMessage());
            
            addOrReplace(((DynamicDisplayPage) this.getPage()).new PlaceholderLink("diagram-download").setVisible(false));
            addOrReplace(new Image("diagram-print-image").setVisible(false));
            addOrReplace(new Image("diagram").setVisible(false));
            addOrReplace(new Label("error-message", new ResourceModel("dashboard.report.reportdiagram.statement.error").wrapOnAssignment(this).getObject()).add(new AttributeModifier("class", true, new Model<String>("message-error"))));
            addOrReplace(new Label("error-reason", e.getMessage()).add(new AttributeModifier("class", true, new Model<String>("message-error"))));
            log.debug(getClass() + ": ", e);
        } finally {
            try {
                jdbcConnection.close();
            } catch (Exception ignore) {
            }
        }
    }

    public class LabelAdaptingCategoryAxis extends CategoryAxis {
        
        private static final long serialVersionUID = 1L;

        private final float labeledTicks;

        public LabelAdaptingCategoryAxis(float labeledTicks, String label) {
            super(label);
            this.labeledTicks = labeledTicks;
        }

        @SuppressWarnings("unchecked")
        @Override
        public List<CategoryTick> refreshTicks(Graphics2D g2, AxisState state, Rectangle2D dataArea, RectangleEdge edge) {

            List<CategoryTick> standardTicks = super.refreshTicks(g2, state, dataArea, edge);
            int interval;
            if (standardTicks.isEmpty() || ((interval = Math.round((float) standardTicks.size() / labeledTicks)) < 1)) return standardTicks;
            List<CategoryTick> newTicks = new ArrayList<CategoryTick>(standardTicks.size());
            for (int i = 0; i < standardTicks.size(); i+=interval) {
                if (i % interval == 0) {
                    CategoryTick tick = standardTicks.get(i);
                    TextBlock textBlock = new TextBlock();
                    textBlock.addLine(tick.getCategory().toString().substring(0, Math.min(tick.getCategory().toString().length(), 8)), 
                                      tick.getLabel().getLastLine().getFirstTextFragment().getFont(), 
                                      tick.getLabel().getLastLine().getFirstTextFragment().getPaint());
                    newTicks.add(new CategoryTick(
                                      tick.getCategory(), 
                                      textBlock, 
                                      tick.getLabelAnchor(), 
                                      tick.getRotationAnchor(), 
                                      tick.getAngle()
                                )
                    );
                }
            }
            return newTicks;
        }
    }
}
