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

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ReflectionException;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxFallbackButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.resources.CompressedResourceReference;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.dcm4chee.dashboard.model.ReportModel;
import org.dcm4chee.dashboard.ui.DashboardPanel;
import org.dcm4chee.dashboard.ui.report.display.DynamicDisplayPage;
import org.dcm4chee.dashboard.ui.util.DatabaseUtils;
import org.dcm4chee.web.common.base.BaseWicketPage;
import org.dcm4chee.web.common.behaviours.TooltipBehaviour;
import org.dcm4chee.web.common.markup.BaseForm;
import org.dcm4chee.web.common.markup.SimpleDateTimeField;
import org.dcm4chee.web.common.secure.SecureSessionCheckPage;
import org.dcm4chee.web.common.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision$ $Date$
 * @since 28.09.2009
 */
public class ConfigureReportPage extends SecureSessionCheckPage {
    
    private static final long serialVersionUID = 1L;
    
    private static Logger log = LoggerFactory.getLogger(ConfigureReportPage.class);

    private ReportModel report;
    protected ModalWindow window;
    
    private boolean diagram;
    private boolean table;
    
    private Label resultMessage;
    
    public ConfigureReportPage(final ModalWindow window, final ReportModel report, boolean diagram, boolean table) {
        super();

        try {
            this.report = report;
            this.window = window;
            this.diagram = diagram;
            this.table = table;

            add(resultMessage = new Label("result-message"));
            resultMessage.setOutputMarkupId(true);
            resultMessage.setDefaultModel(new Model<String>(""));

            add(new ConfigureReportForm("configure-report-form", this.report, resultMessage, this.window));
        } catch (Exception e) {
            log.error(this.getClass().toString() + ": " + "init: " + e.getMessage());
            log.debug("Exception: ", e);
            throw new WicketRuntimeException(e.getLocalizedMessage(), e);
        }
    }

    @Override
    public void onBeforeRender() {
        super.onBeforeRender();

        if (!DatabaseUtils.isConfigurableStatement(this.report.getStatement()))
            redirectToInterceptPage(new DynamicDisplayPage(this.report, null, this.diagram, this.table));
    }
    
    private final class ConfigureReportForm extends BaseForm {

        private static final long serialVersionUID = 1L;

        private Map<String, String> parameters = new HashMap<String, String>();
        
        public ConfigureReportForm(final String id, final ReportModel report, final Label resultMessage, final ModalWindow window) throws InstanceNotFoundException, MalformedObjectNameException, AttributeNotFoundException, ReflectionException, MBeanException, NullPointerException {
            super(id);

            this.add(new Label("dashboard.report.configure.form.title.name", new PropertyModel<String>(report, "title"))
            .add(new AttributeModifier("size", true, new ResourceModel("dashboard.report.configure.form.title.columns"))));

            RepeatingView variableRows = new RepeatingView("variable-rows");
            add(variableRows);

            for (final String parameterName : DatabaseUtils.getParameterSet(report.getStatement())) {
                
                WebMarkupContainer parameterRow = new WebMarkupContainer(parameterName);
                variableRows.add(parameterRow);                        

                if (parameterName.startsWith("date")) {
                	parameterRow.add(new Label("variable-name", parameterName.substring(4)));
                    SimpleDateTimeField dtf;
                    parameterRow
                            .add(dtf = new SimpleDateTimeField("date-variable-value", new IModel<Date>() {
        
                                private static final long serialVersionUID = 1L;
        
                                @Override
                                public void setObject(Date value) {
                                    parameters.put(parameterName, value != null ? 
                                            DateFormat.getDateTimeInstance(
                                            DateFormat.SHORT,
                                            DateFormat.SHORT, 
                                            getSession().getLocale())
                                            .format(value) 
                                            : "");
                                }
        
                                @Override
                                public Date getObject() {
                                    return null;
                                }
        
                                @Override
                                public void detach() {
                                }
                            }));
                            dtf.addToDateField(
                                    new TooltipBehaviour("dashboard.report.configure.", 
                                        "date-variable-value",
                                    new AbstractReadOnlyModel<String>(){
                                        private static final long serialVersionUID = 1L;
                    
                                        @Override
                                        public String getObject() {
                                            return DateUtils.getDatePattern(getParent());
                                        }
                                    }
                            ));
                            dtf.addToTimeField(
                                    new TooltipBehaviour("dashboard.report.configure.", 
                                        "date-variable-value.timeField"));
                    parameterRow.add(new TextField<String>("variable-value").setVisible(false));
                } else {
                	parameterRow.add(new Label("variable-name", 
                			parameterName
                				.replace("text","")
                				.replace("int","")
                				.replace("float","")
                				.replace("boolean","")
                			));
                    parameterRow
                            .add((new TextField<String>("variable-value", new Model<String>() {
    
                                private static final long serialVersionUID = 1L;
    
                                @Override
                                public void setObject(String value) {
                                    parameters.put(parameterName, value != null ? value : "");
                                }                            
                            }))
                            .setVisible(!parameterName.startsWith("date"))
                    );
                    parameterRow.add(new Label("date-variable-value").setVisible(false));
                }
            }

            add(new AjaxFallbackButton("form-submit", ConfigureReportForm.this) {
                private static final long serialVersionUID = 1L;
    
                @Override
                protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                    String message = null;
                    try {
                        if (report.getDataSource() == null) {
                            message = new ResourceModel("dashboard.report.configure.form.statement-test-submit.no-datasource-message").wrapOnAssignment(this.getParent()).getObject();
                            return;
                        }
                        window.setResponsePage(new DynamicDisplayPage(report, parameters, diagram, table));
                    } catch (Exception e) {
                      message = e.getLocalizedMessage();
                      log.debug("Exception: ", e);

                      resultMessage.setDefaultModel(new Model<String>(new ResourceModel("dashboard.report.configure.form.form-submit.failure-message")
                          .wrapOnAssignment(this.getParent()).getObject().toString()
                          + (message == null ? "" : message)))
                          .add(new AttributeModifier("class", true, new Model<String>("message-error")))
                          .setVisible(true);
                      target.addComponent(resultMessage);
                    }
                }

                @Override
                protected void onError(AjaxRequestTarget target, Form<?> form) {
                    target.addComponent(form);
                }
            });
        }
    }
}
