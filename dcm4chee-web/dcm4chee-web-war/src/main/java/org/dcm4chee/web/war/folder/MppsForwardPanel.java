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

package org.dcm4chee.web.war.folder;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.resources.CompressedResourceReference;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.dcm4chee.archive.entity.AE;
import org.dcm4chee.archive.util.JNDIUtils;
import org.dcm4chee.web.common.behaviours.FocusOnLoadBehaviour;
import org.dcm4chee.web.common.markup.BaseForm;
import org.dcm4chee.web.dao.ae.AEHomeLocal;
import org.dcm4chee.web.war.folder.delegate.MppsForwardDelegate;
import org.dcm4chee.web.war.folder.model.PPSModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Robert David <robert.david@agfa.com>
 */
public class MppsForwardPanel extends Panel {

    private static final long serialVersionUID = 1L;
    
    private static Logger log = LoggerFactory.getLogger(MppsForwardPanel.class);
	
    private static final ResourceReference CSS = new CompressedResourceReference(ExportPage.class, "export-style.css");

    private AE destinationAET;
    
    private String result;
    private boolean done = false;
    private boolean success = false;
    
    private List<AE> destinationAETs = new ArrayList<AE>();
    private IModel<AE> destinationModel = new IModel<AE>() {

        private static final long serialVersionUID = 1L;
        
        public AE getObject() {
            return destinationAET;
        }
        public void setObject(AE dest) {
            destinationAET = dest;
        }
        public void detach() {}
    };

    private Label resultLabel = new Label("forwardResult", 
            new AbstractReadOnlyModel<Object>() {
        
                private static final long serialVersionUID = 1L;

                @Override
                public Object getObject() {
                    return result;
                }
            })
        {
            private static final long serialVersionUID = 1L;
    
            @Override
            public void onComponentTag(ComponentTag tag) {
                tag.getAttributes().put("class", 
                		done ? success ? "export_succeed" : "export_failed" : "export_nop");
                super.onComponentTag(tag);
            }
        };

    public MppsForwardPanel(String id, final ModalWindow modalWindow, final PPSModel ppsModel) {
    	super(id);
        if (MppsForwardPanel.CSS != null)
            add(CSSPackageResource.getHeaderContribution(MppsForwardPanel.CSS));

        initDestinationAETs();
        
        add(new Label("title", new ResourceModel("forward.title")));    
        
        final BaseForm form = new BaseForm("form");
        form.setResourceIdPrefix("forward.");
        add(form);

        add(new Label("id.label", new ResourceModel("forward.pps.id.label")));
        add(new Label("id", ppsModel.getId()));
        add(new Label("description.label", new ResourceModel("forward.pps.description.label")));
        add(new Label("description", ppsModel.getDescription()));
        add(new Label("accessionNumber.label", new ResourceModel("forward.pps.accessionNumber.label")));
        add(new Label("accessionNumber", ppsModel.getAccessionNumber()));
        add(new Label("spsId.label", new ResourceModel("forward.pps.spsId.label")));
        add(new Label("spsId", ppsModel.getSpsid()));

        add(new Label("modality.label", new ResourceModel("forward.pps.modality.label")));
        add(new Label("modality", ppsModel.getModality()));
        add(new Label("stationAET.label", new ResourceModel("forward.pps.stationAET.label")));
        add(new Label("stationAET", ppsModel.getStationAET()));
        add(new Label("stationName.label", new ResourceModel("forward.pps.stationName.label")));
        add(new Label("stationName", ppsModel.getStationName()));

        add(new Label("status.label", new ResourceModel("forward.pps.status.label")));
        add(new Label("status", ppsModel.getStatus()));
        
       
        form.add(new DropDownChoice<AE>("destinationAETs", destinationModel, destinationAETs, new IChoiceRenderer<AE>(){
            private static final long serialVersionUID = 1L;

            public Object getDisplayValue(AE ae) {
                if (ae.getDescription() == null) {
                    return ae.getTitle();
                } else {
                    return ae.getTitle()+"("+ae.getDescription()+")";
                }
            }

            public String getIdValue(AE ae, int idx) {
                return String.valueOf(idx);
            }
        }).setNullValid(false).setOutputMarkupId(true));

        form.addLabel("destinationAETsLabel");
        form.addLabel("forwardResultLabel");

        form.add(resultLabel.setOutputMarkupId(true));

        AjaxButton forwardBtn = 
        		new IndicatingAjaxButton("forward") {
            
            private static final long serialVersionUID = 1L;
            
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
            	done = true;
            	success = false;
            	
                result = getString("forward.forwardResult.default");
                try {
                	result = MppsForwardDelegate.getInstance()
                		.forwardMPPS(ppsModel, destinationAET.getTitle());
                	success = result.endsWith("COMPLETED SUCCESSFULLY");
            	} catch (Exception e) {
            		log.error("Error forwarding mpps", e);
            		result = "Error: " + e.getCause().getMessage();
            		success = false;
            	}
                target.addComponent(resultLabel);
            }
            
            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.addComponent(resultLabel);
                BaseForm.addInvalidComponentsToAjaxRequestTarget(target, form);
            }
        };
        forwardBtn.setModel(new ResourceModel("forward.forwardBtn.text"));       
        form.add(forwardBtn);
        		
        form.add(new AjaxButton("close", new ResourceModel("closeBtn")) {
            
            private static final long serialVersionUID = 1L;
            
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                modalWindow.close(target);
            }
            
            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                modalWindow.close(target);
            }
        }).setOutputMarkupId(true).add(FocusOnLoadBehaviour.newSimpleFocusBehaviour());

        setOutputMarkupId(true);
    }
    
    private void initDestinationAETs() {
        destinationAETs.clear();
        AEHomeLocal dao = (AEHomeLocal) JNDIUtils.lookup(AEHomeLocal.JNDI_NAME);
        destinationAETs.addAll(dao.findAll(null));
        if ( destinationAET == null && destinationAETs.size() > 0) {
            destinationAET = destinationAETs.get(0);
        }
    }
}
