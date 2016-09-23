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

package org.dcm4chee.web.war.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxFallbackButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow.CloseButtonCallback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.ElementDictionary;
import org.dcm4che2.data.SpecificCharacterSet;
import org.dcm4che2.util.TagUtils;
import org.dcm4chee.icons.ImageManager;
import org.dcm4chee.icons.behaviours.ImageSizeBehaviour;
import org.dcm4chee.web.common.behaviours.TooltipBehaviour;
import org.dcm4chee.web.common.exceptions.WicketExceptionWithMsgKey;
import org.dcm4chee.web.common.markup.BaseForm;
import org.dcm4chee.web.common.markup.PatientNameField;
import org.dcm4chee.web.common.model.DicomElementModel;
import org.dcm4chee.web.war.common.model.AbstractDicomModel;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 * @version $Revision: 17371 $ $Date: 2012-11-06 11:40:34 +0100 (Di, 06 Nov 2012) $
 * @since Jan 15, 2009
 */
public class SimpleEditDicomObjectPanel extends Panel {

    private static final long serialVersionUID = 1L;
    
    private static ElementDictionary dict = ElementDictionary.getDictionary();
    private final DicomObject dcmObj;
    private Map<Integer,Map<String,Object>> presetChoices;
    private Map<Integer,String[]> choices = new HashMap<Integer, String[]>();
    private List<Integer> notEditable;
    private List<Integer> requiredTags;
    
    private final BaseForm form;
    private TooltipBehaviour tooltipBehaviour = new TooltipBehaviour("dicom.");
    private Model<String> resultMessage;
    private IModel<Boolean> useFnGn;
    private int[][] tagPaths;
    
    public SimpleEditDicomObjectPanel(String id, final ModalWindow window, AbstractDicomModel dcmModel, 
            String title, final int[][] tagPaths, final boolean close, IModel<Boolean> useFnGn) {
        super(id);
        this.setOutputMarkupId(true);
        this.useFnGn = useFnGn;
        window.setCloseButtonCallback(new CloseButtonCallback() {
            private static final long serialVersionUID = 1L;
            public boolean onCloseButtonClicked(AjaxRequestTarget target) {
                onClose();
                return true;
            }
        });
        
        this.dcmObj = new BasicDicomObject();
        this.tagPaths = tagPaths;
        dcmModel.getDataset().copyTo(this.dcmObj);
        add(new Label("title", new Model<String>(title)));
        add(form = new BaseForm("form"));
        form.setOutputMarkupId(true);
        addHdrLabels(form);
        RepeatingView rv = new RepeatingView("elements") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onPopulate() {
                removeAll();
                addDicomObject(this, tagPaths);
            }
        };
        form.add(rv);
        form.add(new AjaxFallbackButton("submit", new ResourceModel("saveBtn"), form) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                try {
                    SimpleEditDicomObjectPanel.this.onSubmit();
                    getPage().setOutputMarkupId(true);
                    target.addComponent(getPage());
                    if (close)
                        window.close(target);
                } catch (WicketExceptionWithMsgKey e) {
                    resultMessage.setObject(this.getString(e.getMsgKey()));
                    target.addComponent(SimpleEditDicomObjectPanel.this);
                } catch (Exception e) {
                    resultMessage.setObject(e.getLocalizedMessage());
                    target.addComponent(SimpleEditDicomObjectPanel.this);
                }
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                BaseForm.addFormComponentsToAjaxRequestTarget(target, form);
            }
        });
        form.add(new AjaxFallbackButton("cancel", new ResourceModel("cancelBtn"), form) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                SimpleEditDicomObjectPanel.this.onCancel();
                window.close(target);
            }
        }
        .setDefaultFormProcessing(false));
        form.add(new Label("result-message", (resultMessage = new Model<String>(""))));
    }

    public SimpleEditDicomObjectPanel(String id, final ModalWindow window, AbstractDicomModel dcmModel, 
            String title, final int[][] tagPaths, final boolean close) {
        this(id, window, dcmModel, title, tagPaths, close, null);
    }

    public void addChoices(int tag, String[] values) {
        choices.put(tag, values);
    }
    
    public void setPresetChoices(Map<Integer, Map<String, Object>> presetChoices) {
        this.presetChoices = presetChoices;
    }
    public boolean addPresetChoice(int tag, String descr, Object value) {
        if (value == null)
            return false;
        Map<String,Object> m = null;
        if (presetChoices == null) {
            presetChoices = new HashMap<Integer,Map<String, Object>>();
        } else {
            m = presetChoices.get(tag);
        }
        if (m == null) {
            m = new HashMap<String, Object>();
            presetChoices.put(tag, m);
        }
        m.put(descr,value);
        return true;
    }

    public List<Integer> getNotEditable() {
        return notEditable;
    }

    public void setNotEditable(List<Integer> notEditable) {
        this.notEditable = notEditable;
    }

    public List<Integer> getRequiredTags() {
        return requiredTags;
    }

    public void setRequiredTags(List<Integer> tags) {
        this.requiredTags = tags;
    }
    
    private void addHdrLabels(WebMarkupContainer table) {
        table.add(new Label("nameHdr", new ResourceModel("dicom.nameHdr")).add(tooltipBehaviour));
        table.add(new Label("valueHdr", new ResourceModel("dicom.valueHdr")).add(tooltipBehaviour));
        table.add(new Label("presetHdr", presetChoices == null || presetChoices.isEmpty() ? 
                new IModel<String>() {
                    private static final long serialVersionUID = 1L;
                    public void detach() {}
                    public void setObject(String arg0) {}      

                    public String getObject() {
                        return null;
                    }
                } : 
                new ResourceModel("dicom.presetHdr")).add(tooltipBehaviour));
    }

    public DicomObject getDicomObject() {
        int[] tagPath;
    	for (int i = 0; i < tagPaths.length; i++) {
    	    tagPath = tagPaths[i];
    	    if ((tagPath.length | 0x01) == 1) {//normal tagPath?
    		putNullIfMissing(tagPath);
    	    } else {//handle compound (DA TM)
    	        int[] ia = new int[tagPath.length-1];
    	        System.arraycopy(tagPath, 0, ia, 0, ia.length);
                putNullIfMissing(ia);//Date
                ia[ia.length-1] = tagPath[ia.length];
                putNullIfMissing(ia);//Time
    	    }
    	}
        return dcmObj;
    }

    private void putNullIfMissing(int[] tagPath) {
        if (dcmObj.get(tagPath) == null)
        	dcmObj.putNull(tagPath, DicomElementModel.getVRof(dcmObj, tagPath));
    }

    protected void onSubmit() {}

    protected void onCancel() {}

    protected void onClose() {}
    
    private void addDicomObject(RepeatingView rv, int[][] tagPaths) {
        final SpecificCharacterSet cs = dcmObj.getSpecificCharacterSet();
        int[] tagPath;
        for (int i=0 ; i < tagPaths.length ; i++) {
            tagPath = tagPaths[i];
            if ((tagPath.length & 0x1) == 1) {
                DicomElement el = dcmObj.get(tagPath);
                if (el != null && el.hasDicomObjects())
                    continue;
            }
            WebMarkupContainer elrow = new WebMarkupContainer(rv.newChildId());
            rv.add(elrow);
            elrow.add(new ElementFragment("fragment", cs, tagPath));
        }
    }

    public class ElementFragment extends Fragment {

        private static final long serialVersionUID = 1L;

        @SuppressWarnings("unchecked")
        public ElementFragment(String id, SpecificCharacterSet cs, final int[] tagPath) {
            super(id, choices.get(tagPath[tagPath.length-1]) == null ? "element" : "choice_element", SimpleEditDicomObjectPanel.this);
            final int tag = tagPath[tagPath.length-1];

            String s = getString(TagUtils.toString(tag),null,"");
            if (s == "") {
                s = dict.nameOf(tag);
                if (s == null || s.equals(ElementDictionary.getUnkown()) 
                        || ElementDictionary.PRIVATE_CREATOR.equals(s)) {
                    s = TagUtils.toString(tag);
                }
            }
            final String tagName = s;

            add(new Label("name", new AbstractReadOnlyModel<String>(){
                
                private static final long serialVersionUID = 1L;

                @Override
                public String getObject() {
                    return tagName;
                }
            }));
            @SuppressWarnings("rawtypes")
            final FormComponent c = form.getDicomObjectField("value", dcmObj, tagPath, 
                    choices.get(tagPath[tagPath.length-1]));
            if (c instanceof PatientNameField) {
                ((PatientNameField) c).setUseFnGn(useFnGn);
            }
            if (notEditable != null && notEditable.contains(tagPath[tagPath.length-1])) {
                c.setEnabled(false);
            }
            if (requiredTags != null && requiredTags.contains(tagPath[tagPath.length-1])) {
            	if (c instanceof PatientNameField)
            		((PatientNameField) c).setSubfieldsToRequired(true);
            	else
            		c.setRequired(true);
            }
            add(c.setOutputMarkupId(true));
            if (c instanceof FormComponentPanel) {
                setMarkupTagReferenceId("date_element"); 
            }
            c.setLabel(new Model<String>(tagName));
            final IModel<List<String>> presetModel = new AbstractReadOnlyModel<List<String>>() {
                private static final long serialVersionUID = 1L;

                @Override
                public List<String> getObject() {
                    Map<String,Object> presets = presetChoices == null ? null : presetChoices.get(tag);
                    final List<String> values = new ArrayList<String>();
                    if (presets != null && presets.size()>0)
                        values.addAll(presets.keySet());
                    return values;
                }
                
            };
            final DropDownChoice<String> choice = new DropDownChoice<String>("presets", 
                    new Model<String>(), presetModel, new IChoiceRenderer<String>() {
                        private static final long serialVersionUID = 1L;

                        public Object getDisplayValue(String object) {
                            return object+" ("+presetChoices.get(tag).get(object)+")";
                        }

                        public String getIdValue(String object, int index) {
                            return object;
                        }
                
            }) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean isVisible() {
                    return presetModel.getObject().size() > 0;
                }
                @Override
                public void onBeforeRender() {
                    Object value = c.getModelObject();
                    for (Map.Entry<String, Object> e : presetChoices.get(tag).entrySet()) {
                        if (e.getValue().equals(value) ) {
                            setModelObject(e.getKey());
                            super.onBeforeRender();
                            return;
                        }
                    }
                    setModelObject(presetChoices.get(tag).keySet().iterator().next());
                    super.onBeforeRender();
                }
            };

            OnChangeAjaxBehavior onChangeAjaxBehavior = new OnChangeAjaxBehavior() {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    c.setModelObject(presetChoices.get(tag).get(choice.getModelValue()));
                    target.addComponent(c);
                    target.addComponent(choice);
                }
            };
            choice.add(onChangeAjaxBehavior);
            add(choice);
            AjaxLink<?> presetLink = new AjaxLink<Object>("presetLink"){
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    c.setModelObject(presetChoices.get(tag).get(choice.getModelValue()));
                    target.addComponent(c);
                }
                @Override
                public boolean isVisible() {
                    return presetModel.getObject().size() > 0;
                }
            };
            presetLink.add(new Image("presetImg",ImageManager.IMAGE_COMMON_ADD)
            .add(new ImageSizeBehaviour()));
            add(presetLink);
        }
    }
}
