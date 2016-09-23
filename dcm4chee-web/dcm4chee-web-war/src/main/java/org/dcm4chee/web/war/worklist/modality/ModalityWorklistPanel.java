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

package org.dcm4chee.web.war.worklist.modality;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.ejb.EJBException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow.WindowClosedCallback;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.resources.CompressedResourceReference;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.dcm4che2.audit.message.AuditEvent;
import org.dcm4che2.audit.message.AuditEvent.ActionCode;
import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DateRange;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4che2.data.VR;
import org.dcm4chee.archive.common.SPSStatus;
import org.dcm4chee.archive.entity.MWLItem;
import org.dcm4chee.archive.util.JNDIUtils;
import org.dcm4chee.icons.ImageManager;
import org.dcm4chee.icons.behaviours.ImageSizeBehaviour;
import org.dcm4chee.web.common.ajax.MaskingAjaxCallBehavior;
import org.dcm4chee.web.common.behaviours.CheckOneDayBehaviour;
import org.dcm4chee.web.common.behaviours.TooltipBehaviour;
import org.dcm4chee.web.common.exceptions.WicketExceptionWithMsgKey;
import org.dcm4chee.web.common.markup.BaseForm;
import org.dcm4chee.web.common.markup.ModalWindowLink;
import org.dcm4chee.web.common.markup.PatientNameField;
import org.dcm4chee.web.common.markup.SimpleDateTimeField;
import org.dcm4chee.web.common.markup.modal.ConfirmationWindow;
import org.dcm4chee.web.common.markup.modal.MessageWindow;
import org.dcm4chee.web.common.secure.SecurityBehavior;
import org.dcm4chee.web.common.util.Auditlog;
import org.dcm4chee.web.common.validators.UIDValidator;
import org.dcm4chee.web.dao.util.QueryUtil;
import org.dcm4chee.web.dao.worklist.modality.ModalityWorklistFilter;
import org.dcm4chee.web.dao.worklist.modality.ModalityWorklistLocal;
import org.dcm4chee.web.war.AuthenticatedWebSession;
import org.dcm4chee.web.war.common.EditDicomObjectPanel;
import org.dcm4chee.web.war.common.IndicatingAjaxFormSubmitBehavior;
import org.dcm4chee.web.war.common.UIDFieldBehavior;
import org.dcm4chee.web.war.config.delegate.WebCfgDelegate;
import org.dcm4chee.web.war.folder.DicomObjectPanel;
import org.dcm4chee.web.war.folder.StudyListPage;
import org.dcm4chee.web.war.folder.delegate.MwlScuDelegate;
import org.dcm4chee.web.war.worklist.modality.MWLItemListView.MwlActionProvider;
import org.dcm4chee.web.war.worklist.modality.model.MWLItemModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision: 18498 $ $Date: 2015-10-13 15:29:30 +0200 (Tue, 13 Oct 2015) $
 * @since Apr 20, 2010
 */
public class ModalityWorklistPanel extends Panel implements MwlActionProvider {

    private static final long serialVersionUID = 1L;
    private static final ResourceReference CSS = new CompressedResourceReference(StudyListPage.class, "folder-style.css");
    
    private static Logger log = LoggerFactory.getLogger(ModalityWorklistPanel.class);
    
    private ModalWindow modalWindow;
    private MessageWindow msgWin = new MessageWindow("msgWin");
    
    public Model<Integer> pagesize = new Model<Integer>();

    private static final String MODULE_NAME = "mw";
    private Model<Boolean> showSearchModel = new Model<Boolean>(true);
    private boolean notSearched = true;
    private ViewPort viewport;
    protected final BaseForm form;
    
    private List<WebMarkupContainer> searchTableComponents = new ArrayList<WebMarkupContainer>();
    
    private transient ModalityWorklistLocal dao;

    private WebMarkupContainer listPanel;
    private WebMarkupContainer navPanel;
    private PatientNameField pnField;
    protected IndicatingAjaxButton searchBtn;
    
    protected boolean ajaxRunning = false;
    protected boolean ajaxDone = false;
    protected Image hourglassImage;

    final MaskingAjaxCallBehavior macb = new MaskingAjaxCallBehavior();

    private ConfirmationWindow<MWLItemModel> confirm;
    
    public ModalityWorklistPanel(final String id) {
        super(id);

        if (ModalityWorklistPanel.CSS != null)
            add(CSSPackageResource.getHeaderContribution(ModalityWorklistPanel.CSS));
            
        add(macb);
        
        msgWin.setTitle(new ResourceModel("mw.search.msg.title").wrapOnAssignment(this));
        add(msgWin);
        add(modalWindow = new ModalWindow("modal-window"));
        modalWindow.setWindowClosedCallback(new WindowClosedCallback() {
            private static final long serialVersionUID = 1L;

            public void onClose(AjaxRequestTarget target) {
                getPage().setOutputMarkupId(true);
                target.addComponent(getPage());
            }            
        });
        viewport = initViewPort();

        final ModalityWorklistFilter filter = viewport.getFilter();
        filter.setLatestItemsFirst(WebCfgDelegate.getInstance().getDefaultLatestMwlItemsFirst());
        add(form = new BaseForm("form", new CompoundPropertyModel<Object>(filter)));
        form.setResourceIdPrefix("mw.");
        AjaxFallbackLink<Object> toggleLink;
        toggleLink = new AjaxFallbackLink<Object>("searchToggle") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                boolean b = !showSearchModel.getObject();
                showSearchModel.setObject(b);
                for (WebMarkupContainer wmc : searchTableComponents) {
                    wmc.setVisible(b);               
                    target.addComponent(wmc);
                }
                target.addComponent(this);
            }
        };
        form.add(toggleLink);
        toggleLink.add((new Image("searchToggleImg", new AbstractReadOnlyModel<ResourceReference>() {

                private static final long serialVersionUID = 1L;

                @Override
                public ResourceReference getObject() {
                    return showSearchModel.getObject() ? ImageManager.IMAGE_COMMON_COLLAPSE : 
                        ImageManager.IMAGE_COMMON_EXPAND;
                }
        })
        .add(new TooltipBehaviour("mw.", "searchToggleImg", showSearchModel)))
        .add(new ImageSizeBehaviour()));

        addQueryFields(filter, form);
        addQueryOptions(form);
        addNavigation(form);
        
        form.setResourceIdPrefix("mw.");

        listPanel = new WebMarkupContainer("listPanel");
        add(listPanel);
        listPanel.setOutputMarkupId(true);
        listPanel.add(getMWLItemListView());
        
        confirm = new ConfirmationWindow<MWLItemModel>("confirm") {

            private static final long serialVersionUID = 1L;
            
            @Override
            public void onConfirmation(AjaxRequestTarget target, MWLItemModel mwlItemModel) {
                ((ModalityWorklistLocal) JNDIUtils.lookup(ModalityWorklistLocal.JNDI_NAME))
                    .removeMWLItem(mwlItemModel.getPk());
                logOrderRecord(mwlItemModel, AuditEvent.ActionCode.DELETE, true);
                ModalityWorklistPanel.this.setOutputMarkupId(true);
                queryMWLItems(target);
                target.addComponent(ModalityWorklistPanel.this);
            }
        };
        confirm.setInitialHeight(150);
        add(confirm);

    }

    protected ViewPort initViewPort() {
        return ((AuthenticatedWebSession) getSession()).getMwViewPort();
    }
    
    public ViewPort getViewPort() {
        return viewport;
    }

    @SuppressWarnings("unchecked")
    protected void addQueryFields(final ModalityWorklistFilter filter, final BaseForm form) {
        final IModel<Boolean> enabledModel = new AbstractReadOnlyModel<Boolean>(){

            private static final long serialVersionUID = 1L;

            @Override
            public Boolean getObject() {
                return (!filter.isExtendedQuery() || QueryUtil.isUniversalMatch(filter.getStudyInstanceUID()));
            }
        };
        
        searchTableComponents.add(form.createAjaxParent("searchLabels"));
        
        form.addInternalLabel("patientName");
        form.addInternalLabel("patientIDDescr");
        form.addInternalLabel("startDate");
        form.addInternalLabel("accessionNumber");
        
        searchTableComponents.add(form.createAjaxParent("searchFields"));
        
        pnField = form.addPatientNameField("patientName", new PropertyModel<String>(filter, "patientName"),
                new IModel<Boolean>() {
                    private static final long serialVersionUID = 1L;
                    public void detach() {}
                    public void setObject(Boolean arg0) {}
        
                    public Boolean getObject() {
                        return WebCfgDelegate.getInstance().useFamilyAndGivenNameQueryFields();
                    }
                }, 
                new PropertyModel<Boolean>(filter, "PNAutoWildcard"), enabledModel, false);
        pnField.setOutputMarkupId(true);
       
        form.addTextField("patientID", enabledModel, true);
        form.addTextField("issuerOfPatientID", enabledModel, true);
        SimpleDateTimeField dtf = form.addDateTimeField("startDateMin", null, enabledModel, false, true);
        SimpleDateTimeField dtfMax = form.addDateTimeField("startDateMax", null, enabledModel, true, true);
        dtf.addToDateField(new CheckOneDayBehaviour(dtf, dtfMax, "onchange"));
        
        form.addTextField("accessionNumber", enabledModel, false);
        
        searchTableComponents.add(form.createAjaxParent("searchFuzzy"));        
        form.addComponent(new CheckBox("fuzzyPN").setVisible(filter.isFuzzyPNEnabled() && viewport.isInternalWorklistProvider()));
        form.addInternalLabel("fuzzyPN").setVisible(filter.isFuzzyPNEnabled() && viewport.isInternalWorklistProvider());

        searchTableComponents.add(form.createAjaxParent("searchDropdowns"));

        form.addInternalLabel("modality");
        form.addInternalLabel("scheduledStationAET");
        form.addInternalLabel("scheduledStationName");
        form.addInternalLabel("SPSStatus");

        form.addDropDownChoice("modality", null, new Model<ArrayList<String>>(new ArrayList<String>(WebCfgDelegate.getInstance().getModalityList())), 
                enabledModel, false).setModelObject("*");
        
        List<String> aetChoices = viewport.getAetChoices();
        if (aetChoices.size() > 0)
            form.addDropDownChoice("scheduledStationAET", null, new Model<ArrayList<String>>(new ArrayList<String>(aetChoices)), enabledModel, false)
            .setModelObject(aetChoices.get(0));
        else
            form.addDropDownChoice("scheduledStationAET", null, new Model<ArrayList<String>>(new ArrayList<String>(aetChoices)), new Model<Boolean>(false), false)
            .setNullValid(true);

        form.addDropDownChoice("scheduledStationName", null, new Model<ArrayList<String>>(new ArrayList<String>(WebCfgDelegate.getInstance().getStationNameList())), enabledModel, false).setModelObject("*");
        form.addDropDownChoice("SPSStatus", null, new Model<ArrayList<String>>(new ArrayList<String>(getSpsStatusChoices())), enabledModel, false).setModelObject("*");

        final WebMarkupContainer extendedFilter = new WebMarkupContainer("extendedFilter") {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return showSearchModel.getObject() && filter.isExtendedQuery();
            }
        };

        extendedFilter.add( new Label("birthDate.label", new ResourceModel("mw.extendedFilter.birthDate.label")));
        extendedFilter.add( new Label("birthDateMin.label", new ResourceModel("mw.extendedFilter.birthDateMin.label")));
        extendedFilter.add( new Label("birthDateMax.label", new ResourceModel("mw.extendedFilter.birthDateMax.label")));
        SimpleDateTimeField dtfB = form.getDateTextField("birthDateMin", null, "extendedFilter.", enabledModel);
        SimpleDateTimeField dtfBEnd = form.getDateTextField("birthDateMax", null, "extendedFilter.", enabledModel);
        dtfB.addToDateField(new CheckOneDayBehaviour(dtfB, dtfBEnd, "onchange"));
        extendedFilter.add(dtfB);
        extendedFilter.add(dtfBEnd);
        extendedFilter.add( new Label("studyInstanceUID.label", new ResourceModel("mw.extendedFilter.studyInstanceUID.label")));
        extendedFilter.add( new TextField<String>("studyInstanceUID")
         .add(new UIDFieldBehavior(form)));
        extendedFilter.setOutputMarkupId(true);
        extendedFilter.setOutputMarkupPlaceholderTag(true);
        form.add(extendedFilter);
        
        searchTableComponents.add(extendedFilter);
        searchTableComponents.add(form.createAjaxParent("searchFooter"));
        
        AjaxFallbackLink<?> link = new AjaxFallbackLink<Object>("showExtendedFilter") {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                filter.setExtendedQuery(!filter.isExtendedQuery());
                target.addComponent(form);
                target.addComponent(this);
            }
        };
        link.add((new Image("showExtendedFilterImg", new AbstractReadOnlyModel<ResourceReference>() {

                private static final long serialVersionUID = 1L;

                @Override
                public ResourceReference getObject() {
                    return filter.isExtendedQuery() ? ImageManager.IMAGE_COMMON_COLLAPSE : 
                        ImageManager.IMAGE_COMMON_EXPAND;
                }
        })
        .add(new TooltipBehaviour("mw.search.", "showExtendedFilterImg", new AbstractReadOnlyModel<Boolean>() {

            private static final long serialVersionUID = 1L;

            @Override
            public Boolean getObject() {
                return filter.isExtendedQuery();
            }
        }))
        .add(new ImageSizeBehaviour())));
        link.setOutputMarkupId(true).setOutputMarkupPlaceholderTag(true);
        form.addComponent(link);
        form.addComponent( new Label("showExtendedFilter.label", new ResourceModel("mw.search.showExtendedFilter.label")));
    }

    protected void addQueryOptions(BaseForm form) {
        form.addLabeledCheckBox("latestItemsFirst", null);
    }

    protected void addNavigation(final BaseForm form) {
        addMwlScpAetSelection(form);
        Button resetBtn = new AjaxButton("resetBtn") {
            
            private static final long serialVersionUID = 1L;

            @SuppressWarnings("unchecked")
            @Override
            protected void onSubmit(final AjaxRequestTarget target, Form<?> form) {
                form.clearInput();
                viewport.clear();
                ((DropDownChoice<String>) ((WebMarkupContainer) form.get("searchDropdowns")).get("modality")).setModelObject("*");
                DropDownChoice<String> scheduledStationAETDropDownChoice = ((DropDownChoice<String>) ((WebMarkupContainer) form.get("searchDropdowns")).get("scheduledStationAET"));
                if (scheduledStationAETDropDownChoice.getChoices().size() > 0)
                    scheduledStationAETDropDownChoice.setModelObject(scheduledStationAETDropDownChoice.getChoices().get(0));
                else
                    scheduledStationAETDropDownChoice.setNullValid(true);
                ((DropDownChoice<String>) ((WebMarkupContainer) form.get("searchDropdowns")).get("scheduledStationName")).setModelObject("*");
                ((DropDownChoice<String>) ((WebMarkupContainer) form.get("searchDropdowns")).get("SPSStatus")).setModelObject("*");
                pagesize.setObject(WebCfgDelegate.getInstance().getDefaultMWLPagesize());
                notSearched = true;
                BaseForm.addFormComponentsToAjaxRequestTarget(target, form);
                target.addComponent(navPanel);
                target.addComponent(listPanel);
            }
        };
        resetBtn.setDefaultFormProcessing(false);
        resetBtn.add(new Image("resetImg",ImageManager.IMAGE_COMMON_RESET)
            .add(new ImageSizeBehaviour("vertical-align: middle;"))
        );
        resetBtn.add(new Label("resetText", new ResourceModel("mw.searchFooter.resetBtn.text"))
            .add(new AttributeModifier("style", true, new Model<String>("vertical-align: middle")))
        );
        form.addComponent(resetBtn);
        form.addComponent(new AjaxButton("defaultSubmit") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                doSearch(target);
            }
            @Override
            public void onComponentTag(ComponentTag tag) {
                super.onComponentTag(tag);
                tag.put("class", "IE_DEFAULT_SUBMIT");
            }
  
        });
        searchBtn = new IndicatingAjaxButton("searchBtn") {
            
            private static final long serialVersionUID = 1L;
            
            @Override
            public void onSubmit(AjaxRequestTarget target, final Form<?> form) {
                doSearch(target);
            }
            
            @Override
            public void onError(AjaxRequestTarget target, Form<?> form) {
                BaseForm.addInvalidComponentsToAjaxRequestTarget(target, form);
            }
            
            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                try {
                    return macb.getAjaxCallDecorator();
                } catch (Exception e) {
                    log.error("Failed to get IAjaxCallDecorator: ", e);
                }
                return null;
            }

            @Override
            public boolean isEnabled() {
                return !ajaxRunning;
            }
        };
        searchBtn.setOutputMarkupId(true);
        searchBtn.add(new Image("searchImg",ImageManager.IMAGE_COMMON_SEARCH)
            .add(new ImageSizeBehaviour("vertical-align: middle;"))
        );
        searchBtn.add(new Label("searchText", new ResourceModel("mw.searchFooter.searchBtn.text"))
            .add(new AttributeModifier("style", true, new Model<String>("vertical-align: middle;")))
        );
        form.addComponent(searchBtn);
        form.setDefaultButton(searchBtn);
        
        navPanel = form.createAjaxParent("navPanel");
        
        pagesize.setObject(WebCfgDelegate.getInstance().getDefaultMWLPagesize());
        form.addDropDownChoice("pagesize", pagesize, new Model<ArrayList<Integer>>(new ArrayList<Integer>(WebCfgDelegate.getInstance().getPagesizeList())), new Model<Boolean>() {
                    
            private static final long serialVersionUID = 1L;

            @Override
            public Boolean getObject() {
                return !ajaxRunning;
            }
        }, true).setNullValid(false)
        .add(new IndicatingAjaxFormSubmitBehavior(form, "onchange", searchBtn) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                if (!WebCfgDelegate.getInstance().isQueryAfterPagesizeChange())
                    return;
                queryMWLItems(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
            }
        });

        form.addComponent(new AjaxFallbackLink<Object>("prev") {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                viewport.setOffset(Math.max(0, viewport.getOffset() - pagesize.getObject()));
                queryMWLItems(target);
            }
            
            @Override
            public boolean isVisible() {
                return (!notSearched && !(viewport.getOffset() == 0));
            }
        }
        .add(new Image("prevImg", ImageManager.IMAGE_COMMON_BACK)
        .add(new ImageSizeBehaviour("vertical-align: middle;"))
        .add(new TooltipBehaviour("mw.search.")))
        );
 
        form.addComponent(new AjaxFallbackLink<Object>("next") {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                viewport.setOffset(viewport.getOffset() + pagesize.getObject());
                queryMWLItems(target);
            }

            @Override
            public boolean isVisible() {
                return (!notSearched && !(viewport.getTotal() - viewport.getOffset() <= pagesize.getObject()));
            }
        }
        .add(new Image("nextImg", ImageManager.IMAGE_COMMON_FORWARD)
        .add(new ImageSizeBehaviour("vertical-align: middle;"))
        .add(new TooltipBehaviour("mw.search.")))
        .setVisible(!notSearched)
        );

        //viewport label: use StringResourceModel with key substitution to select 
        //property key according notSearched and getTotal.
        Model<?> keySelectModel = new Model<Serializable>() {

            private static final long serialVersionUID = 1L;

            @Override
            public Serializable getObject() {
                return notSearched ? "mw.search.notSearched" :
                        viewport.getTotal() == 0 ? "mw.search.noMatchingMwlFound" : 
                            "mw.search.mwlFound";
            }
        };
        form.addComponent(new Label("viewport", new StringResourceModel("${}", ModalityWorklistPanel.this, keySelectModel,new Object[]{"dummy"}){

            private static final long serialVersionUID = 1L;

            @Override
            protected Object[] getParameters() {
                return new Object[]{viewport.getOffset()+1,
                        Math.min(viewport.getOffset() + pagesize.getObject(), viewport.getTotal()),
                        viewport.isInternalWorklistProvider() ? String.valueOf(viewport.getTotal()) : "?" };
            }
        }).setEscapeModelStrings(false));
        form.clearParent();
    }

    private void addMwlScpAetSelection(final BaseForm form) {
        DropDownChoice<String> ch = new DropDownChoice<String>("aetSelect", 
                viewport.getWorklistProviderModel(), viewport.getWorklistProviderListModel()){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return viewport.getWorklistProviderListModel().getObject().size() > 1;
            }
        };
        ch.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            private static final long serialVersionUID = 1L;

            protected void onUpdate(AjaxRequestTarget target) {
                BaseForm.addFormComponentsToAjaxRequestTarget(target, form);
            }
        });
        form.addComponent(ch);
        form.addComponent(new Label("aetSelect.label", new ResourceModel("mw.searchFooter.aetSelect.label")) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return viewport.getWorklistProviderListModel().getObject().size() > 1;
            }
        });
    }

    protected void queryMWLItems(AjaxRequestTarget target) {
        try {
            long  t1 = System.currentTimeMillis();
            log.debug("#### start queryMWLItems");
            ModalityWorklistLocal dao = lookupMwlDAO();
            List<MWLItemModel> current = viewport.getMWLItemModels();
            current.clear();
            if (ViewPort.INTERNAL_WORKLISTPROVIDER.equals(viewport.getWorklistProvider())) {
                viewport.setTotal(dao.countMWLItems(viewport.getFilter()));
                for (MWLItem mwlItem : dao.findMWLItems(viewport.getFilter(), pagesize.getObject(), viewport.getOffset())) 
                    current.add(new MWLItemModel(mwlItem));
            } else {
                List<DicomObject> result = queryAET(viewport.getWorklistProvider(), viewport.getFilter());
                if (result != null) {
                    viewport.setTotal(result.size());
                    int failures = 0;
                    Exception failure = null;
                    for (DicomObject obj : result) {
                        try {
                            current.add(new MWLItemModel(obj));
                        } catch (Exception x) {
                            if (failure == null)
                                failure = x;
                            failures++;
                        }
                    }
                    Collections.sort(current, new Comparator<MWLItemModel>() {
                        public int compare(MWLItemModel o1, MWLItemModel o2) {
                            int c = o1.getPatientName().compareTo(o2.getPatientName());
                            if (c == 0) {
                                if (viewport.getFilter().isLatestItemsFirst()) {
                                    c = o2.getStartDate().compareTo(o1.getStartDate());
                                } else {
                                    c = o1.getStartDate().compareTo(o2.getStartDate());
                                }
                            }
                            return c;
                        }
                    });
                    if (failure != null) {
                        msgWin.show(target, new WicketExceptionWithMsgKey("mw.search.msg.resultErrors", failure)
                        .setMsgParams(new Integer[]{failures}), true);
                    }
                } else {
                    viewport.setTotal(-1);
                }
            }
            notSearched = false;
            log.debug("#### queryMWLItems (found "+current.size()+" items) done in "+(System.currentTimeMillis()-t1)+" ms!");
        } catch (Throwable x) {
            log.error("Query MWL failed!", x);
            if ((x instanceof EJBException) && x.getCause() != null) {
                x = x.getCause();
            }
            if ((x instanceof IllegalArgumentException) && x.getMessage() != null && x.getMessage().indexOf("fuzzy") != -1) {
                x = new WicketExceptionWithMsgKey("fuzzyError", x);
            }
            msgWin.show(target, new WicketExceptionWithMsgKey("mw.search.msg.queryFailed", x), true);
        }
        addAfterQueryComponents(target);
    }
    
    private List<DicomObject> queryAET(String aet, ModalityWorklistFilter filter) throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {
        DicomObject searchDS = new BasicDicomObject();
        searchDS.putString(Tag.PatientName, VR.PN, checkAutoWildcard(filter.getPatientName()));
        searchDS.putString(Tag.PatientID, VR.LO, filter.getPatientID());
        searchDS.putString(Tag.IssuerOfPatientID, VR.LO, filter.getIssuerOfPatientID());
        searchDS.putString(Tag.AccessionNumber, VR.SH, filter.getAccessionNumber());
        DicomElement spsSq = searchDS.putSequence(Tag.ScheduledProcedureStepSequence);
        DicomObject spsSqItem = new BasicDicomObject();
        spsSq.addDicomObject(spsSqItem);
        DateRange drStart = new DateRange(filter.getStartDateMin(), filter.getStartDateMax());
        spsSqItem.putDateRange(Tag.ScheduledProcedureStepStartDate, VR.DA, drStart);
        spsSqItem.putDateRange(Tag.ScheduledProcedureStepStartTime, VR.TM, drStart);
        spsSqItem.putString(Tag.Modality, VR.CS, filter.getModality());
        spsSqItem.putStrings(Tag.ScheduledStationAETitle, VR.AE, filter.getScheduledStationAETs());
        spsSqItem.putString(Tag.ScheduledStationName, VR.SH, filter.getScheduledStationName());
        String status = filter.getSPSStatus();
        spsSqItem.putString(Tag.ScheduledProcedureStepStatus, VR.CS, "*".equals(status) ? null : status);
        searchDS.putString(Tag.StudyInstanceUID, VR.UI, filter.getStudyInstanceUID());
        DateRange drBirth = new DateRange(filter.getBirthDateMin(), filter.getBirthDateMax());
        searchDS.putDateRange(Tag.PatientBirthDate, VR.DA, drBirth);
        return MwlScuDelegate.getInstance().queryMWL(aet, searchDS, pagesize.getObject());
    }
    
    private String checkAutoWildcard(String s) {
        if (s == null || s.length() == 0  || s.equals("*")) {
            return null;
        } else if (s.indexOf('*')!=-1 || s.indexOf('?')!=-1 || s.indexOf('^')!=-1) {
            return s;
        } else {
            return s+'*';
        }
    }

    private ModalityWorklistLocal lookupMwlDAO() {
        if (dao == null)
            dao = (ModalityWorklistLocal) JNDIUtils.lookup(ModalityWorklistLocal.JNDI_NAME);
        return dao;
    }

    public static String getModuleName() {
        return MODULE_NAME;
    }

    protected List<String> getSpsStatusChoices() {
        List<String> status = new ArrayList<String>();
        status.add("*");
        for (SPSStatus spsStatus : SPSStatus.values())
            status.add(spsStatus.toString());
        return status;
    }

    protected MWLItemListView getMWLItemListView() {
        return new MWLItemListView("mwlitems", viewport.getMWLItemModels(), this);
    }
    
    //MwlActionProvider (details and edit)
    public void addMwlActions(final ListItem<MWLItemModel> item, WebMarkupContainer valueContainer, final MWLItemListView mwlListView) {

        final MWLItemModel mwlItemModel = item.getModelObject();
        valueContainer.add(new WebMarkupContainer("cell") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onComponentTag(ComponentTag tag) {
               super.onComponentTag(tag);
               if (item.getModelObject().isDetails()) 
                   tag.put("rowspan", "2");
            }
        });

        WebMarkupContainer details = new WebMarkupContainer("details") {
            
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return mwlItemModel.isDetails();
            }
        };
        DicomObject detailAttrs = new BasicDicomObject();
        mwlItemModel.getDataset().copyTo(detailAttrs);
        mwlItemModel.getPatientAttributes().copyTo(detailAttrs);
        item.add(details.add(new DicomObjectPanel("dicomobject", detailAttrs, false)));

        int[] winSize = WebCfgDelegate.getInstance().getWindowSize("mwEdit");
        valueContainer.add(new AjaxFallbackLink<Object>("toggledetails") {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                mwlItemModel.setDetails(!mwlItemModel.isDetails());
                form.setOutputMarkupId(true);
                if (target != null) {
                    addAfterQueryComponents(target);
                }
            }

        }.add(new Image("detailImg", ImageManager.IMAGE_COMMON_DICOM_DETAILS)
        .add(new ImageSizeBehaviour())
        .add(new TooltipBehaviour("mw.", "detailImg"))))
        .add(new ModalWindowLink("edit", modalWindow, winSize[0], winSize[1]) {
            
                private static final long serialVersionUID = 1L;
    
                @Override
                public void onClick(AjaxRequestTarget target) {
                    modalWindow.setContent(new EditDicomObjectPanel(
                            "content", 
                            modalWindow, 
                            (DicomObject) mwlItemModel.getDataset(), 
                            mwlItemModel.getClass().getSimpleName()
                    ) {
                       private static final long serialVersionUID = 1L;
    
                       @Override
                       protected void onSubmit() {
                           try {
                               mwlItemModel.update(getDicomObject());
                               logOrderRecord(mwlItemModel, AuditEvent.ActionCode.UPDATE, true);
                           } catch (RuntimeException x) {
                               logOrderRecord(mwlItemModel, AuditEvent.ActionCode.UPDATE, false);
                               throw x;
                           }
                           super.onCancel();
                       }                       
                    });
                    modalWindow.show(target);
                    super.onClick(target);
                }
                
                @Override
                public boolean isVisible() {
                    return mwlItemModel.getPk() != -1;
                }
        }
            .add(new Image("editImg",ImageManager.IMAGE_COMMON_DICOM_EDIT)
            .add(new ImageSizeBehaviour())
            .add(new TooltipBehaviour("mw.", "editImg")))
            .add(new SecurityBehavior(getModuleName() + ":editMwlItem"))
        );

        final boolean isLinked = 
            ((ModalityWorklistLocal) JNDIUtils.lookup(ModalityWorklistLocal.JNDI_NAME))
                .hasMPPS(item.getModelObject().getAccessionNumber());

        AjaxLink<?> removeMWLItem = new AjaxLink<Object>("remove") {
            
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
                return !isLinked;
            }
            
            @Override
            public void onClick(AjaxRequestTarget target) {
                confirm.confirm(target, new ResourceModel("mw.confirmRemove").wrapOnAssignment(this), mwlItemModel);
            }
            
            @Override
            public boolean isVisible() {
                return mwlItemModel.getPk() != -1;
            }
        };
        removeMWLItem.add(new Image("removeImg", ImageManager.IMAGE_COMMON_REMOVE)
        .add(!isLinked ? new TooltipBehaviour("mw.", "removeImg") : 
            new AttributeModifier("title", true, new ResourceModel("mw.removeImg.isLinked.tooltip")))
        .add(new ImageSizeBehaviour()));
        removeMWLItem.add(new SecurityBehavior(getModuleName() + ":removeMWLItem"));
        valueContainer.add(removeMWLItem);
    }

    public void doSearch(AjaxRequestTarget target) {
        viewport.setOffset(0);
        viewport.getFilter().setAutoWildcard(WebCfgDelegate.getInstance().getAutoWildcard());
        queryMWLItems(target);
        Auditlog.logQuery(true, UID.ModalityWorklistInformationModelFIND, viewport.getFilter().getQueryDicomObject());
        BaseForm.addFormComponentsToAjaxRequestTarget(target, form);
    }

    private void addAfterQueryComponents(final AjaxRequestTarget target) {
        target.addComponent(pnField);
        target.addComponent(navPanel);
        target.addComponent(listPanel);
    }

    protected void logOrderRecord(MWLItemModel mwlItemModel, ActionCode action, boolean success) {
        Auditlog.logProcedureRecord(action, success, mwlItemModel.getPatientAttributes(), 
           mwlItemModel.getAttributeValueAsString(Tag.StudyInstanceUID), mwlItemModel.getAccessionNumber(), null);
    }
}
