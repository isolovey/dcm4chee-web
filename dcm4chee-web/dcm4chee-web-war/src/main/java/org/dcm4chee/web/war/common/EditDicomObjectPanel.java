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

import java.util.Iterator;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.ajax.markup.html.form.AjaxFallbackButton;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.resources.CompressedResourceReference;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.validation.validator.PatternValidator;
import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.ElementDictionary;
import org.dcm4che2.data.SpecificCharacterSet;
import org.dcm4che2.data.VR;
import org.dcm4che2.util.TagUtils;
import org.dcm4chee.icons.ImageManager;
import org.dcm4chee.icons.behaviours.ImageSizeBehaviour;
import org.dcm4chee.web.common.base.BaseWicketPage;
import org.dcm4chee.web.common.behaviours.TooltipBehaviour;
import org.dcm4chee.web.common.exceptions.WicketExceptionWithMsgKey;
import org.dcm4chee.web.common.markup.BaseForm;
import org.dcm4chee.web.common.markup.modal.MessageWindow;
import org.dcm4chee.web.common.secure.SecurityBehavior;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision: 17816 $ $Date: 2013-05-29 10:04:10 +0200 (Mi, 29 Mai 2013) $
 * @since Jan 15, 2009
 */
public class EditDicomObjectPanel extends Panel {

    private static final long serialVersionUID = 1L;
    
    private static ElementDictionary dict = ElementDictionary.getDictionary();
    private final DicomObject dcmObj;
    private final WebMarkupContainer table;
    private MessageWindow mw = new MessageWindow("mw");
    private TooltipBehaviour tooltipBehaviour = new TooltipBehaviour("dicom.");
    private Model<String> resultMessage;
    private EditableDicomAttributes editable;
    
    public EditDicomObjectPanel(String id, final ModalWindow window, DicomObject dcmObj, String attrModelName) {
        super(id);
        editable = new EditableDicomAttributes(attrModelName);
        add(mw);
        try {
            add(new Label("title", new ResourceModel("dicom.edit.title."+attrModelName)));    
        } catch (Exception x) {
            add(new Label("title", new Model<String>("DICOM Edit")));
        }
        add(new AjaxCheckBox("allowAll", new PropertyModel<Boolean>(editable, "allowAll")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                System.out.println("allowAll changed: allowAll"+editable.isAllowAll());
                target.addComponent(table);
            }
            
        }.add(new SecurityBehavior("edit:allowAll")));
        add(new Label("allowAllLabel", new ResourceModel("edit.allowAll")).add(new SecurityBehavior("edit:allowAll")));
        this.dcmObj = new BasicDicomObject();
        dcmObj.copyTo(this.dcmObj);
        Form<?> form = new Form<Object>("form");
        add(form);
        table = new WebMarkupContainer("table");
        addHdrLabels(table);
        table.setOutputMarkupId(true);
        form.add(table);
        RepeatingView rv = new RepeatingView("elements") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onPopulate() {
                removeAll();
                addDicomObject(this, EditDicomObjectPanel.this.dcmObj, "",
                        new int[0]);
            }
        };
        table.add(rv);
        form.add(new AjaxFallbackButton("submit", new ResourceModel("saveBtn"), form) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                try {
                    EditDicomObjectPanel.this.onSubmit();
                    window.close(target);
                } catch (WicketExceptionWithMsgKey e) {
                    resultMessage.setObject(this.getString(e.getMsgKey()));
                    target.addComponent(form);
                } catch (Exception e) {
                    resultMessage.setObject(e.getLocalizedMessage());
                    target.addComponent(form);
                }
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.addComponent(form);
            }
        });
        form.add(new AjaxFallbackButton("cancel", new ResourceModel("cancelBtn"), form) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                EditDicomObjectPanel.this.onCancel();
                window.close(target);
            }
        }
        .setDefaultFormProcessing(false));
        form.add(new Label("result-message", (resultMessage = new Model<String>(""))));
    }

    private void addHdrLabels(WebMarkupContainer table) {
        table.add(new Label("nameHdr", new ResourceModel("dicom.nameHdr")).add(tooltipBehaviour));
        table.add(new Label("tagHdr", new ResourceModel("dicom.tagHdr")).add(tooltipBehaviour));
        table.add(new Label("vrHdr", new ResourceModel("dicom.vrHdr")).add(tooltipBehaviour));
        table.add(new Label("lenHdr", new ResourceModel("dicom.lenHdr")).add(tooltipBehaviour));
        table.add(new Label("valueHdr", new ResourceModel("dicom.valueHdr")).add(tooltipBehaviour));
    }

    protected DicomObject getDicomObject() {
        return dcmObj;
    }

    protected void onSubmit() {}

    protected void onCancel() {}

    private void addDicomObject(RepeatingView rv, DicomObject dcmObj,
            String nesting, int[] itemPath) {
        final SpecificCharacterSet cs = dcmObj.getSpecificCharacterSet();
        final String nesting1 = nesting + '>';
        for (Iterator<DicomElement> it = dcmObj.iterator(); it.hasNext();) {
            DicomElement el = it.next();
            final int[] tagPath = new int[itemPath.length+1];
            tagPath[itemPath.length] = el.tag();
            System.arraycopy(itemPath, 0, tagPath, 0, itemPath.length);
            WebMarkupContainer elrow = new WebMarkupContainer(rv.newChildId());
            rv.add(elrow);
            elrow.add(new ElementFragment("fragment", el, cs, tagPath, nesting));
            if (!el.hasItems()) {
                elrow.add(new AttributeModifier("title", true, 
                        new Model<String>(el.getValueAsString(cs, 256))));
            }
            if (el.hasDicomObjects()) {
                int numitems = el.countItems();
                for (int i = 0; i < numitems; i++) {
                    final int[] itemPath1 = new int[tagPath.length+1];
                    System.arraycopy(tagPath, 0, itemPath1, 0, tagPath.length);
                    itemPath1[tagPath.length] = i;
                    WebMarkupContainer itemrow =
                        new WebMarkupContainer(rv.newChildId());
                    rv.add(itemrow);
                    DicomObject item = el.getDicomObject(i);
                    itemrow.add(new ItemFragment("fragment", tagPath, i,
                            nesting1));
                    addDicomObject(rv, item, nesting1, itemPath1);
                }
                WebMarkupContainer additemrow =
                    new WebMarkupContainer(rv.newChildId());
                rv.add(additemrow);
                additemrow.add(new AddItemFragment("fragment", tagPath, numitems,
                        nesting1));
            }
        }
        WebMarkupContainer addelrow = new WebMarkupContainer(rv.newChildId());
        rv.add(addelrow);
        addelrow.add(
                new AddElementFragment("fragment", itemPath, nesting));
        
    }

    public class ElementFragment extends Fragment {

        private static final long serialVersionUID = 1L;

        public ElementFragment(String id, DicomElement el,
                SpecificCharacterSet cs, final int[] tagPath, String nesting) {
            super(id, "element", EditDicomObjectPanel.this);
            final int tag = el.tag();
            add(new Label("name", nesting + dict.nameOf(tag)));
            add(new Label("tag", TagUtils.toString(tag)));
            add(new Label("vr", el.vr().toString()));
            add(new Label("length", Integer.toString(el.length())));
            final Model<String> model = 
                el.hasItems() ? 
                        new Model<String>("") 
                      : new DicomElementModel(el, cs, tagPath);
            add(new TextField<String>("value", model)
                    .setVisible(!el.hasItems())
                    .setEnabled(el.length() < 65 && editable.isEditable(tagPath))
                    .add(new AjaxFormComponentUpdatingBehavior("onChange") {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected void onUpdate(AjaxRequestTarget arg0) {}
                        
                    }));
            
            AjaxFallbackLink<?> removeLink = new AjaxFallbackLink<Object>("remove"){
                
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    if (editable.isUseNullForRemoveTag()) {
                        model.setObject(null);
                    } else {
                        EditDicomObjectPanel.this.dcmObj.remove(tagPath);
                    }
                    if (target != null) {
                        target.addComponent(table);
                    }
                }

            };
            removeLink.add(new Image("img-remove", ImageManager.IMAGE_COMMON_REMOVE)
            .add(new ImageSizeBehaviour()));
            removeLink.setVisible(editable.isEditable(tagPath));
            add(removeLink);
        }
     }

    public class AddElementFragment extends Fragment {

        private static final long serialVersionUID = 1L;

        public AddElementFragment(String id, final int[] itemPath, 
                String nesting) {
            super(id, "addelement", EditDicomObjectPanel.this);
            add(new Label("name", nesting + "New Attribute"));
            Form<?> form = new BaseForm("form");
            add(form);
            final Model<String> tagModel = new Model<String>("(0008,0000)");
            form.add(new TextField<String>("tag", tagModel).add(new PatternValidator(
                    "\\([0-9a-fA-F]{4},[0-9a-fA-F]{4}\\)")).setOutputMarkupId(true));
            add(new AjaxSubmitLink("add", form){

                private static final long serialVersionUID = 1L;
                
                @Override
                public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                    String s = tagModel.getObject();
                    int tag = parseTag(s);
                    if (TagUtils.isGroupLengthElement(tag)) {
                        mw.setTitle(new ResourceModel(MessageWindow.TITLE_WARNING));
                        mw.show(target, this.getString("msgwindow.msg.GroupLengthElementNotAllowed"));
                    } else {
                        int[] tagPath = new int[itemPath.length+1];
                        System.arraycopy(itemPath, 0, tagPath, 0, itemPath.length);
                        tagPath[itemPath.length] = tag;
                        if (editable.isEditable(tagPath)) {
                            EditDicomObjectPanel.this.dcmObj.getNestedDicomObject(itemPath).putNull(tag, null);
                        } else {
                            mw.setTitle(new ResourceModel(MessageWindow.TITLE_WARNING));
                            mw.show(target, this.getString("msgwindow.msg.EditNotAllowed"));
                        }
                    }
                    if (target != null) {
                        target.addComponent(table);
                    }
                }
                @Override
                protected void onError(AjaxRequestTarget target, Form<?> form) {
                    target.addComponent(form.get("tag"));
                }
            }.add(new Image("img-add",ImageManager.IMAGE_COMMON_ADD)
            .add(new ImageSizeBehaviour())));
        }
     }

    private static int parseTag(String s) {
        return (Integer.parseInt(s.substring(1,5), 16) << 16)
                 | Integer.parseInt(s.substring(6,10), 16);
    }

    public class ItemFragment extends Fragment {

        private static final long serialVersionUID = 1L;

        public ItemFragment(String id, final int[] tagPath,
                final int itemIndex, String nesting) {
            super(id, "item", EditDicomObjectPanel.this);
            add(new Label("name", nesting + "Item #" + (itemIndex+1)));
            add(new AjaxFallbackLink<Object>("remove"){
                
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    EditDicomObjectPanel.this.dcmObj.get(tagPath)
                            .removeDicomObject(itemIndex);
                    if (target != null) {
                        target.addComponent(table);
                    }
                }
                
            }.add(new Image("img-remove", ImageManager.IMAGE_COMMON_REMOVE)
            .add(new ImageSizeBehaviour())));
        }
     }

    public class AddItemFragment extends Fragment {

        private static final long serialVersionUID = 1L;

        public AddItemFragment(String id, final int[] tagPath,
                final int itemIndex, String nesting) {
            super(id, "additem", EditDicomObjectPanel.this);
            add(new Label("name", nesting + "New Item #" + (itemIndex+1)));
            add(new AjaxFallbackLink<Object>("add"){
                
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    EditDicomObjectPanel.this.dcmObj.get(tagPath)
                            .addDicomObject(new BasicDicomObject());
                    if (target != null) {
                        target.addComponent(table);
                    }
                }
                
            }.add(new Image("img-add", ImageManager.IMAGE_COMMON_ADD)
            .add(new ImageSizeBehaviour())));
        }

     }

    private class DicomElementModel extends Model<String> {

        private static final long serialVersionUID = 1L;
        private final int[] tagPath;
        private final int vr;
 
        public DicomElementModel(DicomElement el, SpecificCharacterSet cs,
                int[] tagPath) {
            super(el.getValueAsString(cs, 64));
            this.vr = el.vr().code();
            this.tagPath = tagPath.clone();
        }

        @Override
        public void setObject(String object) {
            Object prev = super.getObject();
            if (vr != 0) {
                if (object == null) {
                    EditDicomObjectPanel.this.dcmObj.putNull(tagPath, 
                            VR.valueOf(vr));
                } else if (!object.equals(prev)) {
                    EditDicomObjectPanel.this.dcmObj.putString(tagPath,
                            VR.valueOf(vr), object);
                }
            }
            super.setObject(object);
        }
    }
}
