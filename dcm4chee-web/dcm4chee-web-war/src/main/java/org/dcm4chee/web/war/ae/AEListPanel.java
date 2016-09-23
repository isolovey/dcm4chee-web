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

package org.dcm4chee.web.war.ae;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Page;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow.WindowClosedCallback;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.OddEvenListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.resources.CompressedResourceReference;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.dcm4chee.archive.entity.AE;
import org.dcm4chee.archive.util.JNDIUtils;
import org.dcm4chee.icons.ImageManager;
import org.dcm4chee.icons.behaviours.ImageSizeBehaviour;
import org.dcm4chee.usr.dao.UserAccess;
import org.dcm4chee.usr.model.AETGroup;
import org.dcm4chee.web.common.behaviours.TooltipBehaviour;
import org.dcm4chee.web.common.license.ae.AELicenseProviderManager;
import org.dcm4chee.web.common.license.ae.spi.AELicenseProviderSPI;
import org.dcm4chee.web.common.markup.BaseForm;
import org.dcm4chee.web.common.markup.ModalWindowLink;
import org.dcm4chee.web.common.markup.modal.ConfirmationWindow;
import org.dcm4chee.web.common.secure.SecurityBehavior;
import org.dcm4chee.web.dao.ae.AEHomeLocal;
import org.dcm4chee.web.war.ae.delegate.AEDelegate;
import org.dcm4chee.web.war.config.delegate.WebCfgDelegate;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision: 17062 $ $Date: 2012-09-05 12:59:33 +0200 (Mi, 05 Sep 2012) $
 * @since Jan 5, 2009
 */
public class AEListPanel extends Panel {

    private static final long serialVersionUID = 1L;

    private static final ResourceReference CSS = new CompressedResourceReference(AEListPanel.class, "ae-style.css");

    private ModalWindow modalWindow;
    private DicomEchoWindow dicomEchoWindow;
    private ConfirmationWindow<AE> confirm; 
    
    PropertyListView<AE> list;
    private List<String> mppsEmulatedAETs;
    
    private final IModel<String> typeSelectionModel = new Model<String>();
    
    private IModel<Integer> pagesize = new IModel<Integer>() {
        private static final long serialVersionUID = 1L;

        private int pagesize = WebCfgDelegate.getInstance().getDefaultFolderPagesize();

        public Integer getObject() {
            return pagesize;
        }
        
        public void setObject(Integer object) {
            if (object != null)
                pagesize = object;
        }
        
        public void detach() {}
    };
    private Model<String> title = new Model<String>();
    
    private int offset = 0, total = -1;

    @SuppressWarnings("unchecked")
    public AEListPanel(String id) {
        super(id);
        
        if (AEListPanel.CSS != null)
            add(CSSPackageResource.getHeaderContribution(AEListPanel.CSS));
        
        add(modalWindow = new ModalWindow("modal-window"));
        
        setOutputMarkupId(true);
        dicomEchoWindow = new DicomEchoWindow("echoPanel", true);
        dicomEchoWindow.setWindowClosedCallback(new WindowClosedCallback() {

            private static final long serialVersionUID = 1L;

            public void onClose(AjaxRequestTarget target) {
                target.addComponent(AEListPanel.this);
            }});
        add(dicomEchoWindow);
        dicomEchoWindow.add(new SecurityBehavior(getModuleName() + ":dicomEchoWindow"));
        
        confirm = new ConfirmationWindow<AE>("confirm") {

            private static final long serialVersionUID = 1L;
            
            @Override
            public void onConfirmation(AjaxRequestTarget target, AE ae) {
                removeAE(ae);
                AEListPanel.this.setOutputMarkupId(true);
                target.addComponent(AEListPanel.this);
            }
        };
        confirm.setInitialHeight(150);
        add(confirm);

        int[] winSize = WebCfgDelegate.getInstance().getWindowSize("aeEdit");
        ModalWindowLink newAET = 
            new ModalWindowLink("newAET", modalWindow, winSize[0], winSize[1]) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                modalWindow
                .setPageCreator(new ModalWindow.PageCreator() {
                    
                    private static final long serialVersionUID = 1L;
                      
                    @Override
                    public Page createPage() {
                        return new CreateOrEditAETPage(modalWindow, new AE(), AEListPanel.this);
                    }
                });
                super.onClick(target);
            }
        };
        newAET.add(new Image("newAETImg",ImageManager.IMAGE_COMMON_ADD)
            .add(new ImageSizeBehaviour("vertical-align: middle;"))
        );
        newAET.add(new TooltipBehaviour("ae."));
        newAET.add(new Label("newAETText", new ResourceModel("ae.newAET.text"))
            .add(new AttributeModifier("style", true, new Model<String>("vertical-align: middle")))
        );
        add(newAET);
        newAET.add(new SecurityBehavior(getModuleName() + ":newAETLink"));

        BaseForm form = new BaseForm("form");
        add(form);
        String aeManagementDefault = WebCfgDelegate.getInstance().getAEManagementDefault();
        
        Set<String> aetTypeSet = new LinkedHashSet<String>();
        AEHomeLocal aeHome = (AEHomeLocal) JNDIUtils.lookup(AEHomeLocal.JNDI_NAME);
        aetTypeSet.add("<NONE>");
        aetTypeSet.addAll(aeHome.listAeTypes());
        aetTypeSet.addAll(WebCfgDelegate.getInstance().getAETTypes());
        aetTypeSet.addAll(AELicenseProviderManager.get(null).getProvider().getAETypes(WebCfgDelegate.getInstance().getAETTypes()));
        aetTypeSet.remove(null);
        List<String> aetTypes = new ArrayList<String>(aetTypeSet);

        if (!"ANY".equals(aeManagementDefault) && !"NONE".equals(aeManagementDefault))
            typeSelectionModel.setObject(aeManagementDefault);
        
        form.addComponent(new Label("type.filter.label", new StringResourceModel("ae.type.filter.label", AEListPanel.this, null, new Object[]{1} ) ) );
        DropDownChoice<String> typeSelection = form.addDropDownChoice("type-selection", 
                typeSelectionModel, new Model<ArrayList<String>>(new ArrayList<String>(aetTypes)), null, false);
        typeSelection.setNullValid(true)
        .add(new AjaxFormComponentUpdatingBehavior("onchange") {
            
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                offset = 0;
                updateAETList();
                target.addComponent(AEListPanel.this);
            }
        });
        
        form.addComponent(new Label("titleSearch.label", new ResourceModel("ae.search.title.label")));
        form.addComponent(new TextField<String>("titleSearch", title));
        
        AjaxButton searchBtn = new AjaxButton("searchBtn") {
            
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> arg1) {
                offset = 0;
                updateAETList();
                target.addComponent(AEListPanel.this);
            }
        };
        searchBtn.add(new Image("searchImg",ImageManager.IMAGE_COMMON_SEARCH)
        .add(new ImageSizeBehaviour("vertical-align: middle;")));
        searchBtn.add(new Label("searchText", new ResourceModel("ae.searchBtn.text"))
        .add(new AttributeModifier("style", true, new Model<String>("vertical-align: middle;")))
        .setOutputMarkupId(true));
        form.addComponent(searchBtn);
        form.setDefaultButton(searchBtn);
       
        form.addComponent(new Label("pagesize.label", new ResourceModel("ae.pagesize.label")));
        form.addDropDownChoice("pagesize", pagesize, 
                new Model<ArrayList<Integer>>(new ArrayList<Integer>(WebCfgDelegate.getInstance().getPagesizeList())),
                null, false)
                .setNullValid(false)
                .add(new AjaxFormComponentUpdatingBehavior("onchange") {
                    
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        offset = 0;
                        updateAETList();
                        target.addComponent(AEListPanel.this);
                    }
                });
        form.addComponent(new Link<Object>("prev") {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick() {
                offset = Math.max(0, offset - pagesize.getObject());
                updateAETList();               
            }
            
            @Override
            public boolean isVisible() {
                return offset > 0;
            }
        }
        .add(new Image("prevImg", ImageManager.IMAGE_COMMON_BACK)
        .add(new ImageSizeBehaviour("vertical-align: middle;"))
        .add(new TooltipBehaviour("ae.search.")))
        );

        form.addComponent(new Link<Object>("next") {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick() {
                offset = offset + pagesize.getObject();
                updateAETList();
            }

            @Override
            public boolean isVisible() {
                return total > -1 && total - offset > pagesize.getObject();
            }
        }
        .add(new Image("nextImg", ImageManager.IMAGE_COMMON_FORWARD)
        .add(new ImageSizeBehaviour("vertical-align: middle;"))
        .add(new TooltipBehaviour("ae.search.")))
        );

        //viewport label: use StringResourceModel with key substitution to select 
        //property key according notSearched and getTotal.
        Model<?> keySelectModel = new Model<Serializable>() {

            private static final long serialVersionUID = 1L;

            @Override
            public Serializable getObject() {
                return total < 0 ? "ae.search.notSearched" : 
                    total == 0 ? "ae.search.noMatch" : "ae.search.found";
            }
        };
        form.addComponent(new Label("viewport", new StringResourceModel("${}", AEListPanel.this, keySelectModel,new Object[]{"dummy"}){

            private static final long serialVersionUID = 1L;

            @Override
            protected Object[] getParameters() {
                return new Object[]{offset+1, Math.min(offset + pagesize.getObject(), total), total};
            }
        }).setEscapeModelStrings(false));
        
        add( new Label("titleHdr.label", new ResourceModel("ae.titleHdr.label")));
        add( new Label("typeHdr.label", new ResourceModel("ae.typeHdr.label")));
        add( new Label("hostHdr.label", new ResourceModel("ae.hostHdr.label")));
        add( new Label("portHdr.label", new ResourceModel("ae.portHdr.label")));
        add( new Label("descriptionHdr.label", new ResourceModel("ae.descriptionHdr.label")));        
        add( new Label("cipherHdr.label", new ResourceModel("ae.cipherHdr.label")));
        add( new Label("emulatedHdr.label", new ResourceModel("ae.emulatedHdr.label")));
        add( new Label("stationHdr.label", new ResourceModel("ae.stationHdr.label")));
        add( new Label("institutionHdr.label", new ResourceModel("ae.institutionHdr.label")));
        add( new Label("departmentHdr.label", new ResourceModel("ae.departmentHdr.label")));

        final List<AETGroup> aetGroups = ((UserAccess) JNDIUtils.lookup(UserAccess.JNDI_NAME)).getAllAETGroups();
        add((list = new PropertyListView<AE>("list") {

            private static final long serialVersionUID = 1L;

            @Override
            protected ListItem<AE> newItem(final int index) {
                return new OddEvenListItem<AE>(index, getListItemModel(getModel(), index));
            }

            @Override
            protected void populateItem(final ListItem<AE> item) {
                StringBuffer tooltip = new StringBuffer();
                String name = item.getModelObject().getTitle();
                for (AETGroup aetGroup : aetGroups)
                    if (aetGroup.getAets().contains(name))
                        tooltip.append(aetGroup.getGroupname()).append(" ");
                item.add(new Label("title").add(new AttributeModifier("title", true, new Model<String>(tooltip.toString()))));
                item.add(new Label("aeGroup"));
                item.add(new Label("hostName"));
                item.add(new Label("port"));
                item.add(new Label("description", new Model<String>(item.getModelObject().getDescription())));
                CheckBox cipherSuites = new CheckBox("cipherSuites");
                cipherSuites.setModel(new Model<Boolean>() {
                        
                    private static final long serialVersionUID = 1L;

                        @Override
                        public Boolean getObject() {
                            for (String cipher: item.getModelObject().getCipherSuites())
                                if (!cipher.equals("-"))
                                    return true;
                            return false;
                        }
                });
                cipherSuites.setEnabled(false);
                item.add(cipherSuites);
                item.add(new CheckBox("emulated", new AbstractReadOnlyModel<Boolean>(){
                    private static final long serialVersionUID = 1L;
                    @Override
                    public Boolean getObject() {
                        return getMppsEmulatedAETs().contains(item.getModelObject().getTitle());
                    }
                }).setEnabled(false));
                item.add(new Label("stationName"));
                item.add(new Label("institution"));
                item.add(new Label("department"));

                int[] winSize = WebCfgDelegate.getInstance().getWindowSize("aeEdit");
                item.add(new ModalWindowLink("editAET", modalWindow, winSize[0], winSize[1]) {
                    private static final long serialVersionUID = 1L;
    
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        modalWindow
                        .setPageCreator(new ModalWindow.PageCreator() {
                            
                            private static final long serialVersionUID = 1L;
                              
                            @Override
                            public Page createPage() {
                                return new CreateOrEditAETPage(modalWindow, item.getModelObject(), AEListPanel.this);
                            }
                        });
                        super.onClick(target);
                    }
                }
                    .add(new Image("ae.editAET.image", ImageManager.IMAGE_AE_EDIT)
                    .add(new ImageSizeBehaviour("vertical-align: middle;")))
                    .add(new TooltipBehaviour("ae."))
                    .add(new SecurityBehavior(getModuleName() + ":editAETLink"))
                );
                
                AjaxLink<?> removeAET = new AjaxLink<Object>("removeAET") {
    
                    private static final long serialVersionUID = 1L;
    
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        confirm.confirm(target, new StringResourceModel("ae.confirmDelete", AEListPanel.this, null, new Object[]{item.getModelObject()}), item.getModelObject());
                    }
                };
                removeAET.add(new Image("ae.removeAET.image", ImageManager.IMAGE_COMMON_REMOVE)
                .add(new ImageSizeBehaviour()));
                removeAET.add(new TooltipBehaviour("ae."));
                item.add(removeAET);
                removeAET.add(new SecurityBehavior(getModuleName() + ":removeAETLink"));
                
                item.add(new AjaxLink<Object>("echo") {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        dicomEchoWindow.show(target, item.getModelObject());
                    }
                }
                    .add(new Image("ae.echoAET.image", ImageManager.IMAGE_AE_ECHO)
                    .add(new ImageSizeBehaviour()))
                    .add(new TooltipBehaviour("ae."))
                    .add(new SecurityBehavior(getModuleName() + ":dicomEchoLink"))
                );
            }
        }));
        if (!"NONE".equals(aeManagementDefault)) {
            updateAETList();
        }
    }

    protected void updateAETList() {
        list.setModel(new ListModel<AE>());
        AEHomeLocal aeHome = (AEHomeLocal) JNDIUtils.lookup(AEHomeLocal.JNDI_NAME);
        List<AE> updatedList = new ArrayList<AE>();
        total = aeHome.count(typeSelectionModel.getObject(), title.getObject());
        updatedList.addAll(aeHome.find(typeSelectionModel.getObject(), title.getObject(), offset, pagesize.getObject()));
        list.setModelObject(updatedList);
        mppsEmulatedAETs = AEDelegate.getInstance().getEmulatedAETs();
    }
    
    private void removeAE(AE ae) {
        AEDelegate.getInstance().delete(ae);
        List<AE> updatedList = list.getModelObject();
        long pk = ae.getPk();
        for (int i = 0; i < updatedList.size(); i++) {
            if (updatedList.get(i).getPk() == pk) {
                updatedList.remove(i);
                break;
            }
        }
        list.setModelObject(updatedList);
    }

    public List<String> getMppsEmulatedAETs() {
        if (this.mppsEmulatedAETs == null) {
            mppsEmulatedAETs = AEDelegate.getInstance().getEmulatedAETs();
        }
        return mppsEmulatedAETs;
    }
    
    public static String getModuleName() {
        return "aelist";
    }
}
