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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.ejb.EJBException;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.RuntimeMBeanException;
import javax.security.auth.Subject;
import javax.security.jacc.PolicyContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.PageMap;
import org.apache.wicket.PageParameters;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.Component.IVisitor;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.ajax.markup.html.form.AjaxFallbackButton;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxFallbackLink;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow.CloseButtonCallback;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow.WindowClosedCallback;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.IHeaderResponse; 
import org.apache.wicket.markup.html.JavascriptPackageResource;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.IFormSubmittingComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.link.PopupSettings;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.resources.CompressedResourceReference;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.apache.wicket.protocol.http.WebResponse;
import org.apache.wicket.security.components.SecureWebPage;
import org.apache.wicket.security.hive.authentication.DefaultSubject;
import org.apache.wicket.security.hive.authorization.Principal;
import org.apache.wicket.security.hive.authorization.SimplePrincipal;
import org.apache.wicket.security.swarm.strategies.SwarmStrategy;
import org.apache.wicket.util.lang.Classes;
import org.apache.wicket.util.string.AppendingStringBuffer;
import org.apache.wicket.util.time.Duration;
import org.dcm4che2.audit.message.AuditEvent;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.AbstractValidator;
import org.apache.wicket.validation.validator.PatternValidator;
import org.dcm4che2.data.DateRange;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.PersonName;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4che2.data.VR;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4chee.archive.common.PrivateTag;
import org.dcm4chee.archive.conf.AttributeFilter;
import org.dcm4chee.archive.entity.MPPS;
import org.dcm4chee.archive.entity.Patient;
import org.dcm4chee.archive.entity.Study;
import org.dcm4chee.archive.entity.StudyPermission;
import org.dcm4chee.archive.util.JNDIUtils;
import org.dcm4chee.icons.ImageManager;
import org.dcm4chee.icons.behaviours.ImageSizeBehaviour;
import org.dcm4chee.web.common.behaviours.CheckOneDayBehaviour;
import org.dcm4chee.web.common.behaviours.MarkInvalidBehaviour;
import org.dcm4chee.web.common.behaviours.SelectableTableRowBehaviour;
import org.dcm4chee.web.common.behaviours.TooltipBehaviour;
import org.dcm4chee.web.common.delegate.BaseCfgDelegate;
import org.dcm4chee.web.common.exceptions.WicketExceptionWithMsgKey;
import org.dcm4chee.web.common.markup.BaseForm;
import org.dcm4chee.web.common.markup.DateTimeLabel;
import org.dcm4chee.web.common.markup.IFramePage;
import org.dcm4chee.web.common.markup.IFramePanel;
import org.dcm4chee.web.common.markup.ModalWindowLink;
import org.dcm4chee.web.common.markup.PopupLink;
import org.dcm4chee.web.common.markup.SimpleDateTimeField;
import org.dcm4chee.web.common.markup.ModalWindowLink.DisableDefaultConfirmBehavior;
import org.dcm4chee.web.common.markup.modal.ConfirmationWindow;
import org.dcm4chee.web.common.markup.modal.MessageWindow;
import org.dcm4chee.web.common.model.MultiResourceModel;
import org.dcm4chee.web.common.secure.SecureSession;
import org.dcm4chee.web.common.secure.SecurityBehavior;
import org.dcm4chee.web.common.util.Auditlog;
import org.dcm4chee.web.common.util.DateUtils;
import org.dcm4chee.web.common.util.FileUtils;
import org.dcm4chee.web.common.validators.UIDValidator;
import org.dcm4chee.web.common.webview.link.WebviewerLinkProvider;
import org.dcm4chee.web.dao.folder.StudyListFilter;
import org.dcm4chee.web.dao.folder.StudyListLocal;
import org.dcm4chee.web.dao.util.QueryUtil;
import org.dcm4chee.web.war.AuthenticatedWebSession;
import org.dcm4chee.web.war.StudyPermissionHelper;
import org.dcm4chee.web.war.StudyPermissionHelper.StudyPermissionRight;
import org.dcm4chee.web.common.ajax.MaskingAjaxCallBehavior;
import org.dcm4chee.web.common.base.BaseWicketApplication;
import org.dcm4chee.web.war.common.EditDicomObjectPanel;
import org.dcm4chee.web.war.common.IndicatingAjaxFormComponentUpdatingBehavior;
import org.dcm4chee.web.war.common.IndicatingAjaxFormSubmitBehavior;
import org.dcm4chee.web.war.common.SimpleEditDicomObjectPanel;
import org.dcm4chee.web.war.common.UIDFieldBehavior;
import org.dcm4chee.web.war.common.model.AbstractDicomModel;
import org.dcm4chee.web.war.common.model.AbstractEditableDicomModel;
import org.dcm4chee.web.war.config.delegate.WebCfgDelegate;
import org.dcm4chee.web.war.folder.arr.AuditRecordRepositoryFacade;
import org.dcm4chee.web.war.folder.delegate.ContentEditDelegate;
import org.dcm4chee.web.war.folder.delegate.MppsEmulateDelegate;
import org.dcm4chee.web.war.folder.delegate.TarRetrieveDelegate;
import org.dcm4chee.web.war.folder.model.FileModel;
import org.dcm4chee.web.war.folder.model.InstanceModel;
import org.dcm4chee.web.war.folder.model.PPSModel;
import org.dcm4chee.web.war.folder.model.PatientModel;
import org.dcm4chee.web.war.folder.model.SeriesModel;
import org.dcm4chee.web.war.folder.model.StudyModel;
import org.dcm4chee.web.war.folder.studypermissions.StudyPermissionsPage;
import org.dcm4chee.web.war.folder.webviewer.ExportDicomModel;
import org.dcm4chee.web.war.folder.webviewer.ViewerPage;
import org.dcm4chee.web.war.folder.webviewer.Webviewer;
import org.dcm4chee.web.war.folder.webviewer.WebviewerSelectionPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StudyListPage extends Panel {

    private static final ResourceReference CSS = new CompressedResourceReference(StudyListPage.class, "folder-style.css");
    
    private ModalWindow modalWindow;
    
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

    private static final String MODULE_NAME = "folder";
    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(StudyListPage.class);

    private static final int SEARCH_PATIENT = 0;
    private static final int SEARCH_STUDY = 1;
    private static final int SEARCH_PPS_WITHOUT_MWL = 2;
    private static final int SEARCH_WITHOUT_PPS = 3;
    private static final int SEARCH_WITHOUT_MWL = 4;
    private static final int SEARCH_UNCONNECTED_MPPS = 5;

    private static String tooOldAuditMessageText = 
        "Requested editing of an object that should not be edited anymore because of editing time limit. " +
        "The restriction was overriden because the user has assigned the web right 'swarm.principal.IgnoreEditTimeLimit'.";

    private ViewPort viewport;
    private StudyListHeader header;
    private SelectedEntities selected = new SelectedEntities();
    private SimpleEditDicomObjectPanel newPatientPanel;

    private IModel<Boolean> latestStudyFirst = new AbstractReadOnlyModel<Boolean>() {
        private static final long serialVersionUID = 1L;
        @Override
        public Boolean getObject() {
            return viewport.getFilter().isLatestStudiesFirst();
        }
    };
    private IModel<Boolean> hidePPSModel = new Model<Boolean>();
    
    private boolean showSearch = true;
    private boolean disableSearch = false;
    private boolean notSearched = true;
    private BaseForm form;
    private MessageWindow msgWin = new MessageWindow("msgWin");
    private Mpps2MwlLinkPage mpps2MwlLinkWindow = new Mpps2MwlLinkPage("linkPage");
    private ConfirmationWindow<PPSModel> confirmLinkMpps;
    private ConfirmationWindow<StudyModel> confirmLinkMppsStudy;
    private ConfirmationWindow<PPSModel> confirmUnlinkMpps;
    private ConfirmationWindow<StudyModel> confirmUnlinkMppsStudy;
    private ConfirmationWindow<PPSModel> confirmEmulateMpps;
    private ConfirmationWindow<AbstractEditableDicomModel> confirmEdit;
    private ImageSelectionWindow imageSelectionWindow = new ImageSelectionWindow("imgSelection");
    private ModalWindow wadoImageWindow = new ModalWindow("wadoImageWindow");
    private ConfirmationWindow<Form<?>> confirmSearch;
    private ModalWindow arrWindow = new ModalWindow("arrWindow");

    private ExternalLink egg;
    private Model<String> eggModel;
    
    public static final ResourceReference SOKOBAN_SVG = 
        new ResourceReference(StudyListPage.class, "sokoban.svg");
    public static final ResourceReference TETRIS_SVG = 
        new ResourceReference(StudyListPage.class, "tetris.svg");
    public static final ResourceReference EGG_PNG = 
        new ResourceReference(StudyListPage.class, "egg.png");
	
    private WebviewerLinkProvider[] webviewerLinkProviders;
    
    private List<WebMarkupContainer> searchTableComponents = new ArrayList<WebMarkupContainer>();
     
    StudyListLocal dao = (StudyListLocal) JNDIUtils.lookup(StudyListLocal.JNDI_NAME);
    
    StudyPermissionHelper studyPermissionHelper;
    
    final MaskingAjaxCallBehavior macb = new MaskingAjaxCallBehavior();

	private WebMarkupContainer searchFieldContainer;

    public StudyListPage(final String id) {
        super(id);
        
        hidePPSModel.setObject(StudyPermissionHelper.get().isHidePPSAllowed() ? 
                WebCfgDelegate.getInstance().getDefaultHidePPS() : false);
        
        if (StudyListPage.CSS != null)
            add(CSSPackageResource.getHeaderContribution(StudyListPage.CSS));

        viewport = getViewPort();
        viewport.getFilter().setAutoWildcard(WebCfgDelegate.getInstance().getAutoWildcard());
        viewport.getFilter().setLatestStudiesFirst(WebCfgDelegate.getInstance().getDefaultLatestStudiesFirst());
        
        studyPermissionHelper = StudyPermissionHelper.get();

        add(macb);

        add(modalWindow = new ModalWindow("modal-window"));
        modalWindow.setWindowClosedCallback(new WindowClosedCallback() {
            private static final long serialVersionUID = 1L;

            public void onClose(AjaxRequestTarget target) {
                getPage().setOutputMarkupId(true);
                target.addComponent(getPage());
            }            
        });
        initWebviewerLinkProvider();
        
        confirmSearch = new ConfirmationWindow<Form<?>>("confirmSearch") {
            
            private static final long serialVersionUID = 1L;
            
            @Override
            public void onConfirmation(AjaxRequestTarget target, final Form<?> form) {
            	doSearch(target, form, viewport.resetOnSearch());
            }
        };
        add(confirmSearch
            .setInitialHeight(170)
            .setInitialWidth(410));


        final StudyListFilter filter = viewport.getFilter();
        add(form = new BaseForm("form", new CompoundPropertyModel<Object>(filter)));
        form.setResourceIdPrefix("folder.");
        form.add(new IndicatingAjaxButton("bigSearchBtn") {
            private static final long serialVersionUID = 1L;
            
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                if (showSearch) {
                    if (!viewport.getFilter().isFiltered()) {
                        viewport.setResetOnSearch(true);
                        confirmSearch.confirm(target, new StringResourceModel("folder.message.confirmSearch", this, null), form);
                    } else {
                            if (countForWarning(target))
                                    confirmSearch.confirm(target, new StringResourceModel("folder.message.warnSearch", this, null), form);
                            else
                                    doSearch(target, form, true);
                    }
                }
            }
            
            @Override
            public void onError(AjaxRequestTarget target, Form<?> form) {
                BaseForm.addInvalidComponentsToAjaxRequestTarget(target, form);
            }
        }.add(new Label("bigSearchText", new ResourceModel("folder.searchFooter.searchBtn.text"))
            ).setOutputMarkupId(true)
        );
        form.add(new AjaxFallbackLink<Object>("searchToggle") {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                showSearch = !showSearch;
                for (WebMarkupContainer wmc : searchTableComponents)
                    wmc.setVisible(showSearch);               
                target.addComponent(form);
            }
            
            @Override
            public boolean isVisible() {
                return !disableSearch;
            }
        }
        .add((new Image("searchToggleImg", new AbstractReadOnlyModel<ResourceReference>() {

                private static final long serialVersionUID = 1L;

                @Override
                public ResourceReference getObject() {
                    return showSearch ? ImageManager.IMAGE_COMMON_COLLAPSE : 
                        ImageManager.IMAGE_COMMON_EXPAND;
                }
        })
        .add(new TooltipBehaviour("folder.", "searchToggleImg", new AbstractReadOnlyModel<Boolean>() {

            private static final long serialVersionUID = 1L;

            @Override
            public Boolean getObject() {
                return showSearch;
            }
        })))
        .add(new ImageSizeBehaviour())));

        addQueryFields(filter, form);
        addQueryOptions(form);
        addNavigation(form);
        addActions(form);
        
        int[] winSize = WebCfgDelegate.getInstance().getWindowSize("arr");
        arrWindow
        	.setInitialWidth(winSize[0])
        	.setInitialHeight(winSize[1])
        	.setTitle("");

        header = new StudyListHeader("thead", form, viewport, hidePPSModel);
        form.add(header);
        form.add(new PatientListView("patients", viewport.getPatients()));
        msgWin.setTitle("");
        add(msgWin);
        Form<Object> form1 = new Form<Object>("modalForm");
        add(form1);
        form1.add(mpps2MwlLinkWindow);
        add(imageSelectionWindow);
        imageSelectionWindow.setWindowClosedCallback(new WindowClosedCallback(){
            private static final long serialVersionUID = 1L;

            public void onClose(AjaxRequestTarget target) {
                if (!imageSelectionWindow.changeSelection()) 
                    imageSelectionWindow.undoSelectionChanges();
                else
                    if (imageSelectionWindow.isSelectionChanged()) 
                        target.addComponent(form);
            }
        });
        imageSelectionWindow.add(new SecurityBehavior(getModuleName() + ":imageSelectionWindow"));
        add(wadoImageWindow);
        wadoImageWindow.add(new SecurityBehavior(getModuleName() + ":wadoImageWindow"));
        
        add(arrWindow);
        arrWindow.setWindowClosedCallback(new WindowClosedCallback() {
            private static final long serialVersionUID = 1L;

            public void onClose(AjaxRequestTarget target) {
                getPage().setOutputMarkupId(true);
                target.addComponent(getPage());
            }            
        });
        
        add(JavascriptPackageResource.getHeaderContribution(StudyListPage.class, "scrollstate.js"));

		add(new AjaxEventBehavior("onclick") {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onEvent(AjaxRequestTarget target) {
				target.appendJavascript("storeScrollPosition();");
			}
		});
    }

    private void initWebviewerLinkProvider() {
        List<String> names = WebCfgDelegate.getInstance().getWebviewerNameList();
        if (names == null) {
            names = WebCfgDelegate.getInstance().getInstalledWebViewerNameList();
        } 
        if (names == null || names.isEmpty()) {
            webviewerLinkProviders = null;
        } else {
            webviewerLinkProviders = new WebviewerLinkProvider[names.size()];
            Map<String,String> baseUrls = WebCfgDelegate.getInstance().getWebviewerBaseUrlMap();
            for (int i = 0 ; i < webviewerLinkProviders.length ; i++) {
                webviewerLinkProviders[i] = new WebviewerLinkProvider(names.get(i));
                webviewerLinkProviders[i].setBaseUrl(baseUrls.get(names.get(i)));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void addQueryFields(final StudyListFilter filter, final BaseForm form) {
        final IModel<Boolean> enabledModelPat = new AbstractReadOnlyModel<Boolean>(){
            private static final long serialVersionUID = 1L;

            @Override
            public Boolean getObject() {
                return (!filter.isExtendedQuery() || 
                        (QueryUtil.isUniversalMatch(filter.getStudyInstanceUID()) && 
                         QueryUtil.isUniversalMatch(filter.getSeriesInstanceUID())));
            }
        };
        final IModel<Boolean> enabledModel = new AbstractReadOnlyModel<Boolean>(){
            private static final long serialVersionUID = 1L;

            @Override
            public Boolean getObject() {
                return !filter.isPatientQuery() && !filter.isUnconnectedMPPS() &&
                        (!filter.isExtendedQuery() || 
                        (QueryUtil.isUniversalMatch(filter.getStudyInstanceUID()) && 
                         QueryUtil.isUniversalMatch(filter.getSeriesInstanceUID())));
            }
        };
        
        searchTableComponents.add(form.createAjaxParent("searchLabels"));
        
        form.addInternalLabel("patientName");
        form.addInternalLabel("patientIDDescr");
        form.addInternalLabel("studyDate");
        form.addInternalLabel("accessionNumber");
        searchFieldContainer = form.createAjaxParent("searchFields");
        searchTableComponents.add(searchFieldContainer);
        
        form.addPatientNameField("patientName", new PropertyModel<String>(filter, "patientName"),
                new IModel<Boolean>() {
                    private static final long serialVersionUID = 1L;
                    public void detach() {}
                    public void setObject(Boolean arg0) {}
        
                    public Boolean getObject() {
                        return WebCfgDelegate.getInstance().useFamilyAndGivenNameQueryFields();
                    }
                }, 
                new PropertyModel<Boolean>(filter, "PNAutoWildcard"), enabledModelPat, false);
        form.addTextField("patientID", enabledModelPat, true);
        form.addTextField("issuerOfPatientID", enabledModelPat, true);
       
        SimpleDateTimeField dtf = form.addDateTimeField("studyDateMin", new PropertyModel<Date>(filter, "studyDateMin"), 
                enabledModel, false, true);
        SimpleDateTimeField dtfEnd = form.addDateTimeField("studyDateMax", new PropertyModel<Date>(filter, "studyDateMax"), enabledModel, true, true);
        dtf.addToDateField(new CheckOneDayBehaviour(dtf, dtfEnd, "onchange"));
        
        form.addTextField("accessionNumber", enabledModel, false);

        form.addComponent(new CheckBox("fuzzyPN").setVisible(filter.isFuzzyPNEnabled()));
        form.addInternalLabel("fuzzyPN").setVisible(filter.isFuzzyPNEnabled());
        
        searchTableComponents.add(form.createAjaxParent("searchDropdowns"));
        
        form.addInternalLabel("modality");
        form.addInternalLabel("sourceAET");
        form.addDropDownChoice("modality", null, new Model<ArrayList<String>>(new ArrayList<String>(WebCfgDelegate.getInstance().getModalityList())), 
                enabledModel, false).setModelObject("*");
        List<String> aetChoices = viewport.getAetChoices();
        if (aetChoices.size() > 0)
            form.addDropDownChoice("sourceAET", null, new Model<ArrayList<String>>(new ArrayList<String>(aetChoices)), enabledModel, false)
            .setModelObject(aetChoices.get(0));
        else
            form.addDropDownChoice("sourceAET", null, new Model<ArrayList<String>>(new ArrayList<String>(aetChoices)), new Model<Boolean>(false), false)
            .setNullValid(true);

        form.addLabeledCheckBox("exactModalitiesInStudy", null);        
        
        AjaxFallbackLink<?> link = new AjaxFallbackLink<Object>("showExtendedFilter") {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                filter.setExtendedQuery(!filter.isExtendedQuery());
                target.addComponent(form);
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
        .add(new TooltipBehaviour("folder.search.", "showExtendedFilterImg", new AbstractReadOnlyModel<Boolean>() {

            private static final long serialVersionUID = 1L;

            @Override
            public Boolean getObject() {
                return filter.isExtendedQuery();
            }
        })))
        .add(new ImageSizeBehaviour()));
        form.addComponent(link);      
        form.addComponent( new Label("showExtendedFilter.label", new ResourceModel("folder.search.showExtendedFilter.label")));

        final WebMarkupContainer extendedFilter = new WebMarkupContainer("extendedFilter") {

            private static final long serialVersionUID = 1L;
            @Override
            public boolean isVisible() {
                return showSearch && filter.isExtendedQuery();
            }
        };
        extendedFilter.add( new Label("birthDate.label", new ResourceModel("folder.extendedFilter.birthDate.label")));
        extendedFilter.add( new Label("birthDateMin.label", new ResourceModel("folder.extendedFilter.birthDateMin.label")));
        extendedFilter.add( new Label("birthDateMax.label", new ResourceModel("folder.extendedFilter.birthDateMax.label")));
        SimpleDateTimeField dtfB = form.getDateTextField("birthDateMin", null, "extendedFilter.", enabledModelPat);
        SimpleDateTimeField dtfBEnd = form.getDateTextField("birthDateMax", null, "extendedFilter.", enabledModelPat);
        dtfB.addToDateField(new CheckOneDayBehaviour(dtfB, dtfBEnd, "onchange"));
        extendedFilter.add(dtfB);
        extendedFilter.add(dtfBEnd);
        extendedFilter.add( new Label("studyInstanceUID.label", new ResourceModel("folder.extendedFilter.studyInstanceUID.label")));
        extendedFilter.add( new TextField<String>("studyInstanceUID"){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
                return !filter.isPatientQuery() && !filter.isUnconnectedMPPS();
            }            
        }
        .add(new UIDFieldBehavior(form)));
        extendedFilter.add(new CheckBox("requestStudyIUID"));
        extendedFilter.add(new Label("requestStudyIUID.label", new ResourceModel("folder.extendedFilter.requestStudyIUID.label"))
        	.add(new AttributeModifier("title", true, new ResourceModel("folder.extendedFilter.requestStudyIUID.tooltip"))));

        extendedFilter.add( new Label("seriesInstanceUID.label", new ResourceModel("folder.extendedFilter.seriesInstanceUID.label")));
        extendedFilter.add( new TextField<String>("seriesInstanceUID") {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
                return !filter.isPatientQuery() && !filter.isUnconnectedMPPS() &&
                    QueryUtil.isUniversalMatch(filter.getStudyInstanceUID());
            }
        }
        .add(new UIDFieldBehavior(form)));
        
        extendedFilter.add(new CheckBox("exactSeriesIuid"));
        extendedFilter.add(new Label("exactSeriesIuid.label", new ResourceModel("folder.extendedFilter.exactSeriesIuid.label")));
        form.add(extendedFilter);
        
        searchTableComponents.add(form.createAjaxParent("searchFooter"));
        
    }

    private void addQueryOptions(final BaseForm form) {

        final CheckBox chkLatestStudyFirst = form.addLabeledCheckBox("latestStudiesFirst", null);
        AjaxCheckBox chkHidePPSLevel = new AjaxCheckBox("hidePPSLevel", hidePPSModel) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                for (PatientModel patientModel : viewport.getPatients()) {
                    study: for (StudyModel studyModel : patientModel.getStudies()) {
                        if (hidePPSModel.getObject()) {
                            for (PPSModel ppsModel : studyModel.getPPSs()) {
                                if (!ppsModel.isCollapsed()) {
                                    continue study;
                                }
                            }
                            studyModel.collapse();
                        } else {
                            if (studyModel.isCollapsed()) {
                                studyModel.expand();
                                for (PPSModel ppsModel : studyModel.getPPSs()) {
                                    ppsModel.collapse();
                                }
                            }
                        }
                    }
                }
                target.addComponent(form);
            }
            @Override
            public boolean isEnabled() {
                return !viewport.getFilter().isUnconnectedMPPS();
            }
        };
        chkHidePPSLevel.add(new SecurityBehavior(getModuleName() + ":hidePPSLevel"));
        form.addComponent(chkHidePPSLevel);
        form.addInternalLabel(chkHidePPSLevel.getId()).add(new SecurityBehavior(getModuleName() + ":hidePPSLevel"));
        
        List<Integer> expandChoices = WebCfgDelegate.getInstance().getAutoExpandLevelChoiceList();
        final DropDownChoice<Integer> autoExpand = new DropDownChoice<Integer>("autoExpandLevel", expandChoices) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
                return !viewport.getFilter().isPatientQuery() && !viewport.getFilter().isUnconnectedMPPS();
            }
        };
        form.addComponent(autoExpand);
        Label autoExpandLabel = form.addInternalLabel("autoExpandLevel");
        autoExpand.setChoiceRenderer(new IChoiceRenderer<Integer>() {
                private static final long serialVersionUID = 1L;

                public Object getDisplayValue(Integer object) {
                    int level = (Integer) object;
                    return level < 0 ? "auto" : form.getString("folder.searchOptions."+AbstractDicomModel.LEVEL_STRINGS[level].toLowerCase());
                }

                public String getIdValue(Integer object, int index) {
                    return String.valueOf(index);
                }
                
            });
        viewport.getFilter().setAutoExpandLevel(expandChoices.get(0));
        if (expandChoices.size() < 2) {
            autoExpand.setVisible(false);
            autoExpandLabel.setVisible(false);
        }

        final IModel<Integer> searchOptionSelected = new IModel<Integer>(){
            private static final long serialVersionUID = 1L;

            public void detach() {}

            public Integer getObject() {
                StudyListFilter filter = viewport.getFilter();
                if (filter.isPatientQuery())
                    return SEARCH_PATIENT;
                if (filter.isUnconnectedMPPS())
                    return SEARCH_UNCONNECTED_MPPS;
                if (filter.isWithoutPps() && filter.isPpsWithoutMwl())
                    return SEARCH_WITHOUT_MWL;
                if (filter.isPpsWithoutMwl())
                    return SEARCH_PPS_WITHOUT_MWL;
                if (filter.isWithoutPps())
                    return SEARCH_WITHOUT_PPS;
                 return SEARCH_STUDY;
            }

            public void setObject(Integer object) {
                StudyListFilter filter = viewport.getFilter();
                filter.setPatientQuery(object == SEARCH_PATIENT);
                filter.setPpsWithoutMwl(object == SEARCH_WITHOUT_MWL || object == SEARCH_PPS_WITHOUT_MWL);
                filter.setWithoutPps(object == SEARCH_WITHOUT_MWL || object == SEARCH_WITHOUT_PPS);
                filter.setUnconnectedMPPS(object == SEARCH_UNCONNECTED_MPPS);
                if (filter.isUnconnectedMPPS()) {
                    hidePPSModel.setObject(false);
                }
            }
            
        };
        final List<Integer> queryTypeChoices = getQueryTypeChoices();
        final DropDownChoice<Integer> queryType = new DropDownChoice<Integer>("queryType", searchOptionSelected, queryTypeChoices);
        form.addComponent(queryType);
        form.addInternalLabel("queryType");
        queryType.setChoiceRenderer(new IChoiceRenderer<Integer>() {
                private static final long serialVersionUID = 1L;

                public Object getDisplayValue(Integer object) {
                        switch(object) {
                        case SEARCH_PATIENT:
                            return form.getString("folder.searchOptions.patientOption");
                        case SEARCH_STUDY:
                            return form.getString("folder.searchOptions.study");
                        case SEARCH_PPS_WITHOUT_MWL:
                            return form.getString("folder.searchOptions.ppsWithoutMwl");
                        case SEARCH_WITHOUT_PPS:
                            return form.getString("folder.searchOptions.withoutPps");
                        case SEARCH_WITHOUT_MWL:
                            return form.getString("folder.searchOptions.withoutMwl");
                        case SEARCH_UNCONNECTED_MPPS:
                            return form.getString("folder.searchOptions.unconnectedMpps");
                        };
                        return "unknown";
                }

                public String getIdValue(Integer object, int index) {
                    return String.valueOf(queryTypeChoices.get(index));
                }
                
            });
        queryType.add(new IndicatingAjaxFormComponentUpdatingBehavior("onchange", null) {
            private static final long serialVersionUID = 1L;

                @SuppressWarnings("unchecked")
                protected void onUpdate(AjaxRequestTarget target) {
                    chkLatestStudyFirst.setEnabled(!viewport.getFilter().isPatientQuery());
                    BaseForm.addFormComponentsToAjaxRequestTarget(target, searchFieldContainer);
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
        });
    }

    private List<Integer> getQueryTypeChoices() {
        final List<Integer> queryTypeChoices = new ArrayList<Integer>(5);
        StudyPermissionHelper sph = StudyPermissionHelper.get();
        if (sph.hasWebRole("FolderActions"))
            queryTypeChoices.add(SEARCH_PATIENT);
        queryTypeChoices.add(SEARCH_STUDY);
        if (sph.hasWebRole("Mpps2MwlLink")) {
            queryTypeChoices.add(SEARCH_PPS_WITHOUT_MWL);
            queryTypeChoices.add(SEARCH_WITHOUT_PPS);
        }
        if (sph.hasWebRole("Mpps2MwlLinkEasy"))
            queryTypeChoices.add(SEARCH_WITHOUT_MWL);
        if (sph.hasWebRole("UnreferencedMPPS"))
            queryTypeChoices.add(SEARCH_UNCONNECTED_MPPS);
        return queryTypeChoices;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void addNavigation(final BaseForm form) {

        Button resetBtn = new AjaxButton("resetBtn") {
            
            private static final long serialVersionUID = 1L;

            @SuppressWarnings("rawtypes")
			@Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {

                form.clearInput();
                retainSelectedPatients();
                viewport.getFilter().clear();
                ((DropDownChoice) ((WebMarkupContainer) form.get("searchDropdowns")).get("modality")).setModelObject("*");
                DropDownChoice sourceAETDropDownChoice = ((DropDownChoice) ((WebMarkupContainer) form.get("searchDropdowns")).get("sourceAET"));
                if (sourceAETDropDownChoice.getChoices().size() > 0)
                    sourceAETDropDownChoice.setModelObject(sourceAETDropDownChoice.getChoices().get(0));
                else
                    sourceAETDropDownChoice.setNullValid(true);
                pagesize.setObject(WebCfgDelegate.getInstance().getDefaultFolderPagesize());
                notSearched = true;
                hidePPSModel.setObject(false);
                ((WebMarkupContainer) form.get("searchFooter")).get("latestStudiesFirst").setEnabled(true);
                form.setOutputMarkupId(true);
                egg.setVisible(false);
                target.addComponent(form);
            }
        };
        resetBtn.setDefaultFormProcessing(false);
        resetBtn.add(new Image("resetImg",ImageManager.IMAGE_COMMON_RESET)
        .add(new ImageSizeBehaviour("vertical-align: middle;"))
        );
        resetBtn.add(new Label("resetText", new ResourceModel("folder.searchFooter.resetBtn.text"))
            .add(new AttributeModifier("style", true, new Model<String>("vertical-align: middle")))
        );
        form.addComponent(resetBtn);
        eggModel = new Model<String>();
        egg = new ExternalLink("egg",eggModel);
        egg.add(new Image("eggImg",EGG_PNG));
        form.addComponent(egg.setOutputMarkupPlaceholderTag(true)
                .setOutputMarkupId(true).setVisible(false));
        IndicatingAjaxButton searchBtn = new IndicatingAjaxButton("searchBtn") {

            private static final long serialVersionUID = 1L;
            
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                if (!viewport.getFilter().isFiltered()) {
                    viewport.setResetOnSearch(true);
                    confirmSearch.confirm(target, new StringResourceModel("folder.message.confirmSearch", this, null), form);
                } else {
                	if (countForWarning(target))
                		confirmSearch.confirm(target, new StringResourceModel("folder.message.warnSearch", this, null), form);
                	else
                		doSearch(target, form, true);
                }
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

        };
        searchBtn.setOutputMarkupId(true);
        searchBtn.add(new Image("searchImg",ImageManager.IMAGE_COMMON_SEARCH)
            .add(new ImageSizeBehaviour("vertical-align: middle;"))
        );
        searchBtn.add(new Label("searchText", new ResourceModel("folder.searchFooter.searchBtn.text"))
            .add(new AttributeModifier("style", true, new Model<String>("vertical-align: middle;")))
            .setOutputMarkupId(true)
        );
        
        form.addComponent(searchBtn);
        form.setDefaultButton(searchBtn);
        form.clearParent();
        
        form.addDropDownChoice("pagesize", pagesize, 
                new Model<ArrayList<String>>(new ArrayList(WebCfgDelegate.getInstance().getPagesizeList())), 
                new Model<Boolean>(true), 
                true)
         .setNullValid(false)
        .add(new IndicatingAjaxFormSubmitBehavior(form, "onchange", searchBtn) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                if (!WebCfgDelegate.getInstance().isQueryAfterPagesizeChange())
                    return;
                if (!viewport.getFilter().isFiltered()) {
                	viewport.setResetOnSearch(false);
            		confirmSearch.confirm(target, new StringResourceModel("folder.message.confirmSearch", this.getComponent(), null), form); 
                } else doSearch(target, form, false);
                target.addComponent(header);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
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
        });

        addViewPort(form);
        
        confirmEdit = new ConfirmationWindow<AbstractEditableDicomModel>("confirmEdit") {

            private static final long serialVersionUID = 1L;

            @Override
            public void onConfirmation(AjaxRequestTarget target, final AbstractEditableDicomModel model) {
                logSecurityAlert(model, true, StudyListPage.tooOldAuditMessageText);
            }
        };
        confirmEdit.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {
            private static final long serialVersionUID = 1L;
            public void onClose(AjaxRequestTarget target) {
                if (confirmEdit.getState() == ConfirmationWindow.CONFIRMED) {
                    modalWindow.setContent(getEditDicomObjectPanel(confirmEdit.getUserObject()));
                    modalWindow.show(target);
                }
            }
        });
        confirmEdit.setInitialHeight(150);
        form.add(confirmEdit);
        
        addViewPort(form.createAjaxParent("viewport-bottom"));
    }

    protected void doSearch(AjaxRequestTarget target, Form<?> form, boolean reset) {
            String p = viewport.getFilter().getPatientID();
            if ("SOKOBAN".equals(p) || "TETRIS".equals(p)) {
                eggModel.setObject(getRequestCycle().urlFor(p.charAt(0) == 'S' ? SOKOBAN_SVG : TETRIS_SVG).toString());
                egg.setVisible(true);
                viewport.getFilter().setPatientID(null);
            } else {
                if (reset) {
                    viewport.setOffset(0);
                    viewport.getFilter().setAutoWildcard(WebCfgDelegate.getInstance().getAutoWildcard());
                }
                query(target);
                Auditlog.logQuery(true, UID.StudyRootQueryRetrieveInformationModelFIND, viewport.getFilter().getQueryDicomObject());
            }
            target.addComponent(form);
	}

	private void addViewPort(final WebMarkupContainer parent) {
    	
    	parent.add(new AjaxLink<Object>("prev") {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                viewport.setOffset(Math.max(0, viewport.getOffset() - pagesize.getObject()));
                query(target);
                target.addComponent(form);
            }
            
            @Override
            public boolean isVisible() {
                return (!disableSearch && !notSearched && !(viewport.getOffset() == 0));
            }
        }
        .add(new Image("prevImg", ImageManager.IMAGE_COMMON_BACK)
        .add(new ImageSizeBehaviour("vertical-align: middle;"))
        .add(new TooltipBehaviour("folder.search.")))
        );

    	parent.add(new AjaxLink<Object>("next") {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                viewport.setOffset(viewport.getOffset() + pagesize.getObject());
                query(target);
                target.addComponent(form);
            }

            @Override
            public boolean isVisible() {
                return (!disableSearch && !notSearched && !(viewport.getTotal() - viewport.getOffset() <= pagesize.getObject()));
            }
        }
        .add(new Image("nextImg", ImageManager.IMAGE_COMMON_FORWARD)
        .add(new ImageSizeBehaviour("vertical-align: middle;"))
        .add(new TooltipBehaviour("folder.search.")))
        .setVisible(!notSearched)
        );

        //viewport label: use StringResourceModel with key substitution to select 
        //property key according notSearched and getTotal.
        Model<?> keySelectModel = new Model<Serializable>() {

            private static final long serialVersionUID = 1L;

            @Override
            public Serializable getObject() {
                return notSearched ? "folder.search.notSearched" : 
                    viewport.getFilter().isPatientQuery() ? 
                            (viewport.getTotal() == 0 ? "folder.search.noMatchingPatientsFound" : 
                                "folder.search.patientsFound") :
                    viewport.getFilter().isUnconnectedMPPS() ?
                            (viewport.getTotal() == 0 ? "folder.search.noMatchingMPPSFound" : 
                                "folder.search.mppsFound")
                    : (viewport.getTotal() == 0 ? "folder.search.noMatchingStudiesFound" : 
                                "folder.search.studiesFound");
            }
        };
        parent.add(new Label("viewport", new StringResourceModel("${}", StudyListPage.this, keySelectModel,new Object[]{"dummy"}){

            private static final long serialVersionUID = 1L;

            @Override
            protected Object[] getParameters() {
                return new Object[]{viewport.getOffset()+1,
                        Math.min(viewport.getOffset() + pagesize.getObject(), viewport.getTotal()),
                        viewport.getTotal()};
            }
        }).setEscapeModelStrings(false));
    }

    private void addActions(final BaseForm form) {
        
        final ConfirmationWindow<SelectedEntities> confirmDelete = new ConfirmationWindow<SelectedEntities>("confirmDelete") {

            private static final long serialVersionUID = 1L;

            private transient ContentEditDelegate delegate;
            
            private ContentEditDelegate getDelegate() {
                if (delegate == null) {
                    delegate = ContentEditDelegate.getInstance();
                }
                return delegate;
            }

            @Override
            public void onOk(AjaxRequestTarget target) {
                target.addComponent(form);
            }

            @Override
            public void close(AjaxRequestTarget target) {
                target.addComponent(form);
                super.close(target);
            }

            @Override
            public void onConfirmation(AjaxRequestTarget target, final SelectedEntities selected) {

                if (selected.hasTooOld()) {
                    for (StudyModel st : selected.getStudiesTooOld())
                        logSecurityAlert(st, true, StudyListPage.tooOldAuditMessageText);
                }
                this.setStatus(new StringResourceModel("folder.message.delete.running", StudyListPage.this, null));
                
                try {
                	getDelegate().moveToTrash(selected);
                	if(WebCfgDelegate.getInstance().showDoneDialogAfterAction())
                		setStatus(new StringResourceModel("folder.message.deleteDone", StudyListPage.this,null));
                    if (selected.hasPatients()) {
                        viewport.getPatients().clear();
                        query(target);
                    } else
                        selected.refreshView(true);
                    if(!WebCfgDelegate.getInstance().showDoneDialogAfterAction())
                    	close(target);
                } catch (RuntimeMBeanException e) {
                	log.error("moveToTrash failed: ", e);
                	if (e.getCause() instanceof EJBException)
                		if (WebCfgDelegate.getInstance().getTrustPatientIdWithoutIssuer())
                			this.setStatus(new StringResourceModel("folder.message.deleteFailed.alreadyExists", StudyListPage.this, null));
                		else 
                			this.setStatus(new StringResourceModel("folder.message.deleteFailed.notAllowed", StudyListPage.this, null));
                } catch (Throwable t) {
                    log.error("moveToTrash failed: ", t);
                    setStatus(new StringResourceModel("folder.message.deleteFailed", StudyListPage.this,null));
                }
                target.addComponent(getMessageWindowPanel().getMsgLabel());
                target.addComponent(getMessageWindowPanel().getOkBtn());
            }
            
            @Override
            public void onDecline(AjaxRequestTarget target, SelectedEntities selected) {
                if (selected.getPpss().size() != 0) {
                    if (ContentEditDelegate.getInstance().deletePps(selected)) {
                        if(WebCfgDelegate.getInstance().showDoneDialogAfterAction())
                        	this.setStatus(new StringResourceModel("folder.message.deleteDone", StudyListPage.this,null));
                        selected.refreshView(true);
                    } else 
                        this.setStatus(new StringResourceModel("folder.message.deleteFailed", StudyListPage.this,null));
                }
            }
        };
        form.add(confirmDelete);

        AjaxFallbackButton newPatBtn = new AjaxFallbackButton("newPatBtn", form) {
            
            private static final long serialVersionUID = 1L;
            
            @Override
            protected void onSubmit(final AjaxRequestTarget target, Form<?> form) {
                final PatientModel newPatModel = getNewPatientModel();
                newPatientPanel = new SimpleEditDicomObjectPanel("content", modalWindow, newPatModel,
                        new ResourceModel("folder.newPatient.text").wrapOnAssignment(this).getObject(),
                        new int[][]{{Tag.PatientName},{Tag.PatientID},{Tag.IssuerOfPatientID},
                            {Tag.PatientBirthDate},{Tag.PatientSex}}, true, 
                            new IModel<Boolean>() {
                                private static final long serialVersionUID = 1L;
                                public void detach() {}
                                public void setObject(Boolean arg0) {}

                                public Boolean getObject() {
                                    return WebCfgDelegate.getInstance().useFamilyAndGivenNameQueryFields();
                                }
                            }) {
                    private static final long serialVersionUID = 1L;
                    
                    @Override
                    protected void onSubmit() {
                        newPatModel.update(getDicomObject());
                        viewport.getPatients().add(0, newPatModel);
                    }
                };
                newPatientPanel.addChoices(Tag.PatientSex, new String[]{"M","F","O","U"});
                if (!StudyPermissionHelper.get().isEditNewPatientID()) {
                    newPatientPanel.setNotEditable(Arrays.asList(Tag.PatientID, Tag.IssuerOfPatientID));
                }
                newPatientPanel.setRequiredTags(Arrays.asList(Tag.PatientName, Tag.PatientID, Tag.IssuerOfPatientID));
                modalWindow.setContent(newPatientPanel);
                modalWindow.setTitle("");
                modalWindow.show(target);
            }
        };
        newPatBtn.add(new Image("newPatImg",ImageManager.IMAGE_USER_ADD)
            .add(new ImageSizeBehaviour("vertical-align: middle;"))
        );
        newPatBtn.add(new Label("newPatText", new ResourceModel("folder.newPatBtn.text"))
            .add(new AttributeModifier("style", true, new Model<String>("vertical-align: middle")))
        );
        form.add(newPatBtn);
        newPatBtn.add(new SecurityBehavior(getModuleName() + ":newPatientButton"));
        
        AjaxButton deleteBtn = new AjaxButton("deleteBtn") {

        	private static final long serialVersionUID = 1L;
            
        	private void checkWarnings(SelectedEntities selected, MultiResourceModel remarkModel) {
                    if (selected.hasPatients()) {
                    	int studiesCount = 0;
                    	String patientListing = "";
                    	Iterator<PatientModel> i = selected.getPatients().iterator();
                    	while (i.hasNext()) {
                    		PatientModel patientModel = i.next();
                    		studiesCount += dao.countStudiesOfPatient(patientModel.getPk(), null);
                    		patientListing += (
                    				(patientModel.getId() != null ? patientModel.getId() : " ") + 
                    				" / " + 
                    				(patientModel.getIssuer() != null ? patientModel.getIssuer() : " ") + 
                    				" / " + 
                    				(patientModel.getName() != null ? patientModel.getName() : " "));
                    		if (i.hasNext())
                    			patientListing += ", <br /> ";
                    	}               	
                    	remarkModel.addModel(new StringResourceModel("folder.message.warnPatientDelete", 
                    				this, null, new Object[] {studiesCount, patientListing}));
                    	confirmDelete
                    		.setInitialWidth(500)
                    		.setInitialHeight(280 + (20 * selected.getPatients().size()));
                    }                
                    if (ContentEditDelegate.getInstance().sendsRejectionNotes()) 
                        remarkModel.addModel(new StringResourceModel("folder.message.warnDelete",this, null));
        	}
        	
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
            	confirmDelete.initContent(500, 280);
            	
                MultiResourceModel remarkModel = new MultiResourceModel();
            	confirmDelete.setRemark(remarkModel);
            	
                boolean hasIgnored = selected.update(studyPermissionHelper.isUseStudyPermissions(), 
                        viewport.getPatients(), StudyPermission.DELETE_ACTION);   
                selected.deselectChildsOfSelectedEntities();
                checkNotRestoreable(selected, remarkModel);
                selected.computeTooOld();
                if ((hasIgnored ||selected.hasDicomSelection() || selected.hasPPS()) 
                        && selected.hasTooOld()) {
                    if (StudyPermissionHelper.get().ignoreEditTimeLimit()) {
                    	checkWarnings(selected, remarkModel);
                        if (hasIgnored) {
	                    confirmDelete.initContent(confirmDelete.getInitialWidth(), confirmDelete.getInitialHeight() + 50);
                            remarkModel.addModel(new StringResourceModel("folder.message.deleteNotAllowed",this, null));
                        }
                        if (selected.hasPPS()) {
                            confirmDelete.confirmWithCancel(target, new StringResourceModel("folder.message.tooOld.confirmPpsDelete",this, null,new Object[]{selected}), selected);
                        } else if (selected.hasDicomSelection()) {
                        	confirmDelete.initContent(confirmDelete.getInitialWidth(), confirmDelete.getInitialHeight() + 50);
                        	confirmDelete.confirm(target, new StringResourceModel("folder.message.tooOld.delete", this, null, new Object[]{selected}), selected);
                        } else {
                            msgWin.setInfoMessage(getString("folder.message.deleteNotAllowed"));
                            msgWin.setColor("#FF0000");
                            msgWin.show(target);
                        }
                    } else {
                        msgWin.setInfoMessage(getString("folder.message.tooOld.delete.denied"));
                        msgWin.setColor("#FF0000");
                        msgWin.show(target);
                    }
                    return;
                }

                checkWarnings(selected, remarkModel);
                
                if (selected.hasPatients())
                	confirmDelete.setImage(ImageManager.IMAGE_COMMON_WARN);
                
                if (hasIgnored) {
                	confirmDelete.initContent(confirmDelete.getInitialWidth(), confirmDelete.getInitialHeight() + 50);
                    remarkModel.addModel(new StringResourceModel("folder.message.deleteNotAllowed",this, null));
                }
                if (selected.hasPPS()) {
                    confirmDelete.confirmWithCancel(target, new StringResourceModel("folder.message.confirmPpsDelete",this, null,new Object[]{selected}), selected);
                } else if (selected.hasDicomSelection()) {
                    confirmDelete.confirm(target, new StringResourceModel("folder.message.confirmDelete",this, null,new Object[]{selected}), selected);
                } else { 
                    if (hasIgnored) {
                        msgWin.setInfoMessage(getString("folder.message.deleteNotAllowed"));
                        msgWin.setColor("#FF0000");
                    } else {
                        msgWin.setInfoMessage(getString("folder.message.noSelection"));
                        msgWin.setColor("");
                    }
                    msgWin.show(target);
                }
            }
        };
        deleteBtn.add(new Image("deleteImg", ImageManager.IMAGE_FOLDER_DELETE)
            .add(new ImageSizeBehaviour("vertical-align: middle;"))
        );
        deleteBtn.add(new Label("deleteText", new ResourceModel("folder.deleteBtn.text"))
            .add(new AttributeModifier("style", true, new Model<String>("vertical-align: middle")))
        );
        form.add(deleteBtn);
        deleteBtn.add(new SecurityBehavior(getModuleName() + ":deleteButton"));
        
        AjaxFallbackButton moveBtn = new AjaxFallbackButton("moveBtn", form) {
            
            private static final long serialVersionUID = 1L;
            
            @Override
            protected void onSubmit(final AjaxRequestTarget target, Form<?> form) {
                selected.update(false, viewport.getPatients(), StudyPermission.UPDATE_ACTION, true);
                log.debug("Selected Entities:{}",selected);

                if (selected.hasDicomSelection()) {
                    modalWindow
                    .setPageCreator(new ModalWindow.PageCreator() {
                        
                        private static final long serialVersionUID = 1L;
                          
                        @Override
                        public Page createPage() {
                            return new MoveEntitiesPage(
                                  modalWindow, 
                                  selected, 
                                  viewport.getPatients());
                        }
                    });
                    
                    int[] winSize = WebCfgDelegate.getInstance().getWindowSize("move");
                    modalWindow.setInitialWidth(winSize[0]).setInitialHeight(winSize[1]);
                    modalWindow.setTitle("");
                    modalWindow.show(target);
                } else
                    msgWin.show(target, getString("folder.message.noSelection"));
            }
        };
        moveBtn.add(new Image("moveImg",ImageManager.IMAGE_FOLDER_MOVE)
            .add(new ImageSizeBehaviour("vertical-align: middle;"))
        );
        moveBtn.add(new Label("moveText", new ResourceModel("folder.moveBtn.text"))
            .add(new AttributeModifier("style", true, new Model<String>("vertical-align: middle")))
        );
        form.add(moveBtn);
        moveBtn.add(new SecurityBehavior(getModuleName() + ":moveButton"));
        
        PopupLink exportBtn = new PopupLink("exportBtn", "exportPage") {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick() {
                this.setResponsePage(new ExportPage(viewport.getPatients()));
            }
        };
        int[] winSize = WebCfgDelegate.getInstance().getWindowSize("export");
        exportBtn.setPopupWidth(winSize[0]);
        exportBtn.setPopupHeight(winSize[1]);
        exportBtn.add(new Image("exportImg",ImageManager.IMAGE_FOLDER_EXPORT)
            .add(new ImageSizeBehaviour("vertical-align: middle;"))
        );
        exportBtn.add(new Label("exportText", new ResourceModel("folder.exportBtn.text"))
            .add(new AttributeModifier("style", true, new Model<String>("vertical-align: middle")))
        );
        form.add(exportBtn);
        exportBtn.add(new SecurityBehavior(getModuleName() + ":exportButton"));

        int index = -1;
        if (webviewerLinkProviders != null) {
            for (int i = 0; i < webviewerLinkProviders.length; i++) {
                if (webviewerLinkProviders[i].supportViewingAllSelection()) {
                    index = i;
                    break;
                }
            }
        }
        final WebviewerLinkProvider viewer = index >= 0 ? webviewerLinkProviders[index] : null;

        Link<Object> viewBtn = new Link<Object>("viewBtn") {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick() {
                if (viewer != null) {
                    try {
                        selected.update(false, viewport.getPatients(), StudyPermission.UPDATE_ACTION, true);
                        selected.deselectChildsOfSelectedEntities();
                        selected.update(false, viewport.getPatients(), StudyPermission.UPDATE_ACTION, true);

                        ExportDicomModel model = new ExportDicomModel(selected);

                        HttpServletRequest request =
                            ((WebRequestCycle) RequestCycle.get()).getWebRequest().getHttpServletRequest();
                        HttpServletResponse response =
                            ((WebResponse) getRequestCycle().getResponse()).getHttpServletResponse();
                        String result = viewer.viewAllSelection(model.getPatients(), request, response);
                        if (!viewer.notWebPageLinkTarget()) {
                            ViewerPage page = new ViewerPage();
                            page.add(new Label("viewer", new Model<String>(result)).setEscapeModelStrings(false));
                            this.setResponsePage(page);
                        }
                    } catch (Exception e) {
                        log.error("Cannot view all the selection!", e);
                        if (viewer.notWebPageLinkTarget()) {
                            setResponsePage(getPage());
                        }
                    }
                }
            }
            
            @Override
            public boolean isVisible() {
                return viewer != null;
            }
        };
        if (viewer != null && !viewer.notWebPageLinkTarget()) {
            ((Link) viewBtn).setPopupSettings(new PopupSettings(PageMap.forName("viewBtnPage"), PopupSettings.RESIZABLE
                | PopupSettings.SCROLLBARS));
        }

        viewBtn.add(new Image("viewImg", ImageManager.IMAGE_FOLDER_VIEWER).add(new ImageSizeBehaviour(
            "vertical-align: middle;")));
        viewBtn.add(new Label("viewText", new ResourceModel("folder.viewBtn.text")).add(new AttributeModifier("style",
            true, new Model<String>("vertical-align: middle"))));
        form.add(viewBtn);
        viewBtn.add(new SecurityBehavior(getModuleName() + ":viewButton"));
        
        confirmLinkMpps = new ConfirmationWindow<PPSModel>("confirmLink") {
            
            private static final long serialVersionUID = 1L;
            
            @Override
            public void onConfirmation(AjaxRequestTarget target, final PPSModel ppsModel) {
                logSecurityAlert(ppsModel, true, StudyListPage.tooOldAuditMessageText);
            }
        };
        confirmLinkMpps.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {
        	
        	private static final long serialVersionUID = 1L;
        	
        	public void onClose(AjaxRequestTarget target) {
                if (confirmLinkMpps.getState() == ConfirmationWindow.CONFIRMED) {
                	setMppsLinkWindow().show(target, confirmLinkMpps.getUserObject(), form);
                }
        	}
        });
        form.add(confirmLinkMpps
            .setInitialHeight(150)
            .setInitialWidth(410));

        confirmLinkMppsStudy = new ConfirmationWindow<StudyModel>("confirmLinkStudy") {
            
            private static final long serialVersionUID = 1L;
            
            @Override
            public void onConfirmation(AjaxRequestTarget target, final StudyModel studyModel) {
                logSecurityAlert(studyModel, true, StudyListPage.tooOldAuditMessageText);
            }
        };
        confirmLinkMppsStudy.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {
        	
        	private static final long serialVersionUID = 1L;
        	
        	public void onClose(AjaxRequestTarget target) {
        		if (confirmLinkMppsStudy.getState() == ConfirmationWindow.CONFIRMED) {
        			setMppsLinkWindow().show(target, confirmLinkMppsStudy.getUserObject(), form);
        		}
        	}
        });

        form.add(confirmLinkMppsStudy
            .setInitialHeight(150)
            .setInitialWidth(410));
        
        confirmUnlinkMpps = new ConfirmationWindow<PPSModel>("confirmUnlink") {
 
            private static final long serialVersionUID = 1L;

            @Override
            public void onOk(AjaxRequestTarget target) {
                target.addComponent(form);
            }

            @Override
            public void close(AjaxRequestTarget target) {
                target.addComponent(form);
                super.close(target);
            }

            @Override
            public void onConfirmation(AjaxRequestTarget target, final PPSModel ppsModel) {
                logSecurityAlert(ppsModel, true, StudyListPage.tooOldAuditMessageText);
                
                this.setStatus(new StringResourceModel("folder.message.unlink.running", StudyListPage.this, null));
                getMessageWindowPanel().getOkBtn().setVisible(false);

                try {
                    if (ContentEditDelegate.getInstance().unlink(ppsModel)) {
                        setStatus(new StringResourceModel("folder.message.unlinkDone", StudyListPage.this,null));
                        ppsModel.getStudy().expand();
                        ppsModel.getStudy().refresh();
                    } else 
                        setStatus(new StringResourceModel("folder.message.unlinkFailed", StudyListPage.this,null));
                } catch (Throwable t) {
                    log.error("Unlink of MPPS failed:"+ppsModel, t);
                }
                target.addComponent(getMessageWindowPanel().getMsgLabel());
                target.addComponent(getMessageWindowPanel().getOkBtn());
            }
        };
        form.add(confirmUnlinkMpps.setInitialHeight(150));
        
        confirmUnlinkMppsStudy = new ConfirmationWindow<StudyModel>("confirmUnlinkStudy") {
            
            private static final long serialVersionUID = 1L;

            @Override
            public void onOk(AjaxRequestTarget target) {
                target.addComponent(form);
            }

            @Override
            public void close(AjaxRequestTarget target) {
                target.addComponent(form);
                super.close(target);
            }

            @Override
            public void onConfirmation(AjaxRequestTarget target, final StudyModel studyModel) {
                logSecurityAlert(studyModel, true, StudyListPage.tooOldAuditMessageText);
                
                this.setStatus(new StringResourceModel("folder.message.unlink.running", StudyListPage.this, null));
                getMessageWindowPanel().getOkBtn().setVisible(false);

                try {
                    int failed = ContentEditDelegate.getInstance().unlink(studyModel);
                    if (failed == 0) {
                        setStatus(new StringResourceModel("folder.message.unlinkDone", StudyListPage.this,null));
                    } else 
                        setStatus(new StringResourceModel("folder.message.unlinkFailed", StudyListPage.this,null));
                } catch (Throwable t) {
                    log.error("Unlink of MPPS failed:"+studyModel, t);
                }
                target.addComponent(getMessageWindowPanel().getMsgLabel());
                target.addComponent(getMessageWindowPanel().getOkBtn());
            }
        };
        form.add(confirmUnlinkMppsStudy.setInitialHeight(150));
        
        confirmEmulateMpps = new ConfirmationWindow<PPSModel>("confirmEmulate") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onOk(AjaxRequestTarget target) {
                target.addComponent(form);
            }

            @Override
            public void close(AjaxRequestTarget target) {
                target.addComponent(form);
                super.close(target);
            }
            
            @Override
            public void onConfirmation(AjaxRequestTarget target, final PPSModel ppsModel) {
                log.info("Emulate MPPS for Study:"+ppsModel.getStudy().getStudyInstanceUID());
                int success = -1;
                try {
                    if (ppsModel.hasForeignPpsInfo()) {
                        ContentEditDelegate.getInstance().removeForeignPpsInfo(ppsModel.getStudy().getPk());
                    }
                    success = MppsEmulateDelegate.getInstance().emulateMpps(ppsModel.getStudy().getPk());
                } catch (Throwable t) {
                    log.error("Emulate MPPS failed!", t);
                }
                setStatus(new StringResourceModel(success < 0 ? "folder.message.emulateFailed" : "folder.message.emulateDone", 
                        StudyListPage.this, null, new Object[]{new Integer(success)}));
                if (success > 0) {
                    StudyModel st = ppsModel.getStudy();
                    st.collapse();
                    st.expand();
                }
            }
        };
        confirmEmulateMpps.setInitialHeight(150);
        form.add(confirmEmulateMpps);
    }

    protected boolean checkNotRestoreable(SelectedEntities selected, MultiResourceModel remarkModel) {
    	boolean flag = false;
    	long nrLocalInstances, nrInstances;
		if (selected.hasStudies()) {
			String[] studyIuids = new String[selected.getStudies().size()];
			int i = 0;
			for (StudyModel study : selected.getStudies()) {
				studyIuids[i++] = study.getStudyInstanceUID();
			}
			nrInstances = dao.countDownloadableInstances(studyIuids, null, null);
			nrLocalInstances = dao.countDownloadableInstancesLocal(studyIuids, null, null);
			if (nrInstances != nrLocalInstances) {
				remarkModel.addModel(new StringResourceModel("folder.message.warnDelete.not_restoreable",this, null, new Object[] {"Study", (nrInstances-nrLocalInstances)}));
				flag = true;
			}
		}
		if (selected.hasSeries()) {
			String[] seriesIuids = new String[selected.getSeries().size()];
			int i = 0;
			for (SeriesModel study : selected.getSeries()) {
				seriesIuids[i++] = study.getSeriesInstanceUID();
			}
			nrInstances = dao.countDownloadableInstances(null, seriesIuids, null);
			nrLocalInstances = dao.countDownloadableInstancesLocal(null, seriesIuids, null);
			if (nrInstances != nrLocalInstances) {
				remarkModel.addModel(new StringResourceModel("folder.message.warnDelete.not_restoreable",this, null, new Object[] {"Series", (nrInstances-nrLocalInstances)}));
				flag = true;
			}
		}
		if (selected.hasInstances()) {
			String[] sopIuids = new String[selected.getInstances().size()];
			int i = 0;
			for (InstanceModel study : selected.getInstances()) {
				sopIuids[i++] = study.getSOPInstanceUID();
			}
			nrInstances = dao.countDownloadableInstances(null, null, sopIuids);
			nrLocalInstances = dao.countDownloadableInstancesLocal(null, null, sopIuids);
			if (nrInstances != nrLocalInstances) {
				remarkModel.addModel(new StringResourceModel("folder.message.warnDelete.not_restoreable",this, null, new Object[] {"Instance", (nrInstances-nrLocalInstances)}));
				flag = true;
			}
		}
		return flag;
	}

	private void query(AjaxRequestTarget target) {
        StudyListFilter filter = viewport.getFilter();
        notSearched = false;
        if (filter.isUnconnectedMPPS()) {
            queryUnconnectedMPPS(filter);
        } else {
            queryStudies(target, filter);
        }
        filter.markSearchedOptions();
    }

    private void queryUnconnectedMPPS(StudyListFilter filter) {
        viewport.setTotal(dao.countUnconnectedMPPS(filter));
        List<Patient> patients = dao.findUnconnectedMPPS(filter, pagesize.getObject(), viewport.getOffset());
        retainSelectedPatients();
        for (Patient patient : patients) {
            addPatient(patient); 
        }
        header.expandToLevel(AbstractDicomModel.PPS_LEVEL);
    }
    
    private void queryStudies(AjaxRequestTarget target, StudyListFilter filter) {
        try {
            List<String> dicomSecurityRoles = (studyPermissionHelper.applyStudyPermissions() ? 
                        studyPermissionHelper.getDicomRoles() : null);
            viewport.setTotal(dao.count(filter, dicomSecurityRoles));
            updatePatients(dao.findPatients(filter, pagesize.getObject(), viewport.getOffset(), dicomSecurityRoles));
            header.expandToLevel(filter.isPatientQuery() ? 
                    AbstractDicomModel.PATIENT_LEVEL : AbstractDicomModel.STUDY_LEVEL);
            updateAutoExpandLevel();
            if (filter.isExtendedQuery() && filter.getSeriesInstanceUID() != null) {
                filter.setPpsWithoutMwl(false);
                filter.setWithoutPps(false);
            }
        } catch (Throwable x) {
            if ((x instanceof EJBException) && x.getCause() != null) 
                x = x.getCause();
            if ((x instanceof IllegalArgumentException) && x.getMessage() != null && x.getMessage().indexOf("fuzzy") != -1) 
                x = new WicketExceptionWithMsgKey("fuzzyError", x);
            log.error("Error on queryStudies: ", x);
            msgWin.show(target, new WicketExceptionWithMsgKey("folder.message.searcherror", x), true);
        }
    }

    private void updateStudyPermissions() {
        for (PatientModel patient : viewport.getPatients()) {
            for (StudyModel study : patient.getStudies()) 
                study.setStudyPermissionActions(dao.findStudyPermissionActions((study).getStudyInstanceUID(), 
                        studyPermissionHelper.getDicomRoles()
                ));
        }
    }
    
    private void updatePatients(List<Patient> patients) {
        retainSelectedPatients();
        for (Patient patient : patients) {
            PatientModel patientModel = addPatient(patient);   
            if (!viewport.getFilter().isPatientQuery() && 
                    viewport.getFilter().getAutoExpandLevel() != PatientModel.PATIENT_LEVEL) {
                for (Study study : patient.getStudies()) {
                    List<String> actions = dao.findStudyPermissionActions((study).getStudyInstanceUID(), studyPermissionHelper.getDicomRoles());
                    if (!studyPermissionHelper.applyStudyPermissions()
                        || actions.contains("Q")) {  
                        addStudy(study, patientModel, actions);
                        patientModel.setExpandable(true);
                    }
                }
            } else if (WebCfgDelegate.getInstance().forcePatientExpandableForPatientQuery()) {
                patientModel.setExpandable(true);
            }
        }
    }

    private void retainSelectedPatients() {
        for (int i = 0; i < viewport.getPatients().size(); i++) {
            PatientModel patient = viewport.getPatients().get(i);
            patient.retainSelectedStudies();
            if (patient.isCollapsed() && !patient.isSelected()) { 
                viewport.getPatients().remove(i);
                i--;
            }
        }
    }
    
    private void updateAutoExpandLevel() {
        int level = AbstractDicomModel.PATIENT_LEVEL;
        pat: for (PatientModel patient : viewport.getPatients()) {
            if (!patient.isCollapsed()) {
                for (StudyModel s : patient.getStudies()) {
                   if (level < AbstractDicomModel.STUDY_LEVEL)
                       level = AbstractDicomModel.STUDY_LEVEL;
                    for (PPSModel p : s.getPPSs()) {
                        if (level < AbstractDicomModel.PPS_LEVEL)
                            level = AbstractDicomModel.PPS_LEVEL;
                        for (SeriesModel se : p.getSeries()) {
                            if (se.isCollapsed()) {
                                level = AbstractDicomModel.SERIES_LEVEL;
                            } else {
                                level = AbstractDicomModel.INSTANCE_LEVEL;
                                break pat;
                            }
                        }
                    }
                }
            }
        }
        header.setExpandAllLevel(level);
    }

    private PatientModel addPatient(Patient patient) {
        long pk = patient.getPk();
        for (PatientModel patientModel : viewport.getPatients()) {
            if (patientModel.getPk() == pk) {
                return patientModel;
            }
        }
        PatientModel patientModel = new PatientModel(patient, latestStudyFirst);
        viewport.getPatients().add(patientModel);
        return patientModel;
    }
    
    private boolean addStudy(Study study, PatientModel patient, List<String> studyPermissionActions) {
        List<StudyModel> studies = patient.getStudies();
        for (StudyModel studyModel : studies) {
            if (studyModel.getPk() == study.getPk()) {
                return false;
            }
        }
        StudyModel m = new StudyModel(study, patient, study.getCreatedTime(), studyPermissionActions);
        StudyListFilter filter = viewport.getFilter();
        if (filter.isExactSeriesIuid())
            m.setRestrictChildsBySeriesIuid(viewport.getFilter().getSeriesInstanceUID());
        int expandLevel = filter.getAutoExpandLevel();
        if (expandLevel == -1) {
            if (filter.isExactSeriesIuid()) {
                m.expand();
            } else {
                boolean woMwl = filter.isPpsWithoutMwl();
                boolean woPps = filter.isWithoutPps();
                if (woMwl || woPps) {
                    m.expand();
                    if (!hidePPSModel.getObject()) {
                        for (PPSModel pps : m.getPPSs()) {
                            pps.collapse();
                        }
                    } else {
                        m.collapse();
                    }
                } else if (StudyPermissionHelper.get().isEasyLink()) {
                    checkHasUnlinkedSeries(study, m);
                }
            }
        } else if (expandLevel > StudyModel.STUDY_LEVEL) {
            expandToLevel(m, expandLevel);
        } else if (StudyPermissionHelper.get().isEasyLink()) {
            checkHasUnlinkedSeries(study, m);
        }
        studies.add(m);
        return true;
    }
    
    private void checkHasUnlinkedSeries(Study study, StudyModel m) {
        m.expand();
        m.collapse();
    }

    private void expandToLevel(AbstractDicomModel m, int level) {
        int modelLevel = m.levelOfModel();
        if ( modelLevel < level) {
            m.expand();
            if (modelLevel == AbstractDicomModel.STUDY_LEVEL) {//study expands to series
                if (level == AbstractDicomModel.PPS_LEVEL) {
                    for (AbstractDicomModel m1 : m.getDicomModelsOfNextLevel()) {
                        m1.collapse();
                    }
                } else if (level == AbstractDicomModel.INSTANCE_LEVEL) {
                    for (AbstractDicomModel m1 : m.getDicomModelsOfNextLevel()) {
                        for (AbstractDicomModel m2 : m1.getDicomModelsOfNextLevel()) {
                            expandToLevel(m2, level);
                        }
                    }
                }
            } else if (++modelLevel < level) {
                for (AbstractDicomModel m1 : m.getDicomModelsOfNextLevel()) {
                    expandToLevel(m1, level);
                }
            }
        }
    }

    private boolean expandLevelChanged(AbstractDicomModel model) {
        int currLevel = header.getExpandAllLevel();
        int level = model.levelOfModel();
        if (model.isCollapsed() || currLevel > level) {
            level = getExpandedLevel( 0, viewport.getPatients());
        } else {
            level = getExpandedLevel( ++level, model.getDicomModelsOfNextLevel());
        }
        header.setExpandAllLevel(level);
        return level != currLevel;
    }
    
    private int getExpandedLevel(int startLevel, List<? extends AbstractDicomModel> list) {
        int level = startLevel; 
        if (list != null) {
            startLevel++;
            int l;
            for ( AbstractDicomModel m1 : list ) {
                if (!m1.isCollapsed()) {
                    l = getExpandedLevel( startLevel, m1.getDicomModelsOfNextLevel());
                    if ( l > level) 
                        level = l;
                }
            }
        }
        return level;
    }
    
    public static String getModuleName() {
        return MODULE_NAME;
    }

    private final class PatientListView extends PropertyListView<Object> {

        private static final long serialVersionUID = 1L;

        private PatientListView(String id, List<?> list) {
            super(id, list);
        }

        @Override
        protected void populateItem(final ListItem<Object> item) {
            item.setOutputMarkupId(true);
            final PatientModel patModel = (PatientModel) item.getModelObject();
            WebMarkupContainer row = new WebMarkupContainer("row");
            AjaxCheckBox selChkBox = new AjaxCheckBox("selected") {

                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) { 
                    target.addComponent(this.getParent());
                }
            };
            row.add(new SelectableTableRowBehaviour(selChkBox, "patient", "patient_selected"));
            item.add(row);
            WebMarkupContainer cell = new WebMarkupContainer("cell") {

                private static final long serialVersionUID = 1L;

                @Override
                protected void onComponentTag(ComponentTag tag) {
                   super.onComponentTag(tag);
                   tag.put("rowspan", patModel.getRowspan());
                }
            };
            cell.add(new ExpandCollapseLink("expand", patModel, item)
                .setVisible(patModel.isExpandable()));
            row.add(cell);
            
            TooltipBehaviour tooltip = new TooltipBehaviour("folder.content.data.patient.");
            row.add(new Label("name").add(tooltip));        
            row.add(new Label("idAndIssuer").add(tooltip));
            DateTimeLabel dtl = new DateTimeLabel("birthdate").setWithoutTime(true);
            dtl.add(tooltip.newWithSubstitution(new PropertyModel<String>(dtl, "textFormat")));
            row.add(dtl);
            row.add(new Label("sex").add(tooltip));
            row.add(new Label("comments").add(tooltip));
            row.add(new AjaxFallbackLink<Object>("toggledetails") {

                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    if (checkExists(patModel, target)) {
                        patModel.setDetails(!patModel.isDetails());
                        if (target != null) {
                            target.addComponent(item);
                        }
                    }
                }
            }.add(new Image("detailImg",ImageManager.IMAGE_COMMON_DICOM_DETAILS)
            .add(new ImageSizeBehaviour())
            .add(tooltip)));
            row.add(getEditLink(modalWindow, patModel, tooltip)
                    .add(new SecurityBehavior(getModuleName() + ":editPatientLink"))
                    .add(tooltip));           
            row.add(getStudyPermissionLink(modalWindow, patModel, tooltip)
                    .add(new SecurityBehavior(getModuleName() + ":studyPermissionsPatientLink"))
                    .add(tooltip));
            row.add(Webviewer.getLink(patModel, webviewerLinkProviders, studyPermissionHelper, tooltip, modalWindow)
                    .add(new SecurityBehavior(getModuleName() + ":webviewerPatientLink")));
            row.add(new AjaxLink<Object>("arr") {
                
                private static final long serialVersionUID = 1L;
                
                @Override
		public void onClick(final AjaxRequestTarget target) {
                	showAuditQueryResult(target, 
                			new AuditRecordRepositoryFacade()
                				.doSearch(AuditRecordRepositoryFacade.Level.PATIENT, 
                						patModel.getId()));
                }
            }.add(new Image("arrImg",ImageManager.IMAGE_COMMON_ARR)
            .add(new ImageSizeBehaviour())
            .add(tooltip)
            .add(new SecurityBehavior(getModuleName() + ":arrLink"))));
            row.add(selChkBox.add(tooltip));
            WebMarkupContainer details = new WebMarkupContainer("details") {

                private static final long serialVersionUID = 1L;

                @Override
                public boolean isVisible() {
                    return patModel.isDetails();
                }
            };
            item.add(details);
            details.add(new DicomObjectPanel("dicomobject", patModel, false));
            item.add(new StudyListView("studies", patModel.getStudies(), item));
        }
    }

    private final class StudyListView extends PropertyListView<Object> {

        private static final long serialVersionUID = 1L;
        
        private final ListItem<?> patientListItem;

        private StudyListView(String id, List<StudyModel> list,
                ListItem<?> patientListItem) {
            super(id, list);
            this.patientListItem = patientListItem;
        }

        @Override
        protected void populateItem(final ListItem<Object> item) {
            item.setOutputMarkupId(true);

            final StudyModel studyModel = (StudyModel) item.getModelObject();
            WebMarkupContainer row = new WebMarkupContainer("row"){
                private static final long serialVersionUID = 1L;
                @Override
                public boolean isVisible() {
                    return studyModel.getPk() != -1;
                }
            };
            AjaxCheckBox selChkBox = new AjaxCheckBox("selected") {

                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    target.addComponent(this.getParent());
                }
            };
            row.add(new SelectableTableRowBehaviour(selChkBox, "study", "study_selected"));
            item.add(row);
            WebMarkupContainer cell = new WebMarkupContainer("cell") {

                private static final long serialVersionUID = 1L;

                @Override
                protected void onComponentTag(ComponentTag tag) {
                   super.onComponentTag(tag);
                   int rows = studyModel.getRowspan();
                   if (hidePPSModel.getObject()) {
                       rows -= studyModel.getPPSs().size();
                   }
                   tag.put("rowspan", rows);
                }
            };
            cell.add(new ExpandCollapseLink("expand", studyModel, patientListItem));
            row.add(cell);
            
            TooltipBehaviour tooltip = new TooltipBehaviour("folder.content.data.study.");
            
            row.add(new DateTimeLabel("datetime").add(tooltip));
            row.add(new Label("id").add(tooltip));            
            row.add(new Label("accessionNumber").add(tooltip));
            row.add(new Label("modalities").add(tooltip));
            row.add(new Label("description", 
            		new Model<String>(studyModel.getDescription() != null &&
            				studyModel.getDescription().length() > 30 ? 
            						studyModel.getDescription().substring(0,30) : 
            							studyModel.getDescription()))
            	.add(new AttributeModifier("title", true, new Model<String>(studyModel.getDescription()))));
            row.add(new Label("numberOfSeries").add(tooltip));
            row.add(new Label("numberOfInstances").add(tooltip));
            row.add(new Label("availability").add(tooltip));
            row.add(new AjaxFallbackLink<Object>("toggledetails") {

                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    if (checkExists(studyModel, target)) {
                        studyModel.setDetails(!studyModel.isDetails());
                        if (target != null) {
                            target.addComponent(patientListItem);
                        }
                    }
                }
            }
                .add(new Image("detailImg",ImageManager.IMAGE_COMMON_DICOM_DETAILS)
                .add(new ImageSizeBehaviour())
                .add(tooltip))
            );
            row.add(getEditLink(modalWindow, studyModel, tooltip)
                    .add(new SecurityBehavior(getModuleName() + ":editStudyLink"))
            );
            row.add(getStudyPermissionLink(modalWindow, studyModel, tooltip)
                    .add(new SecurityBehavior(getModuleName() + ":studyPermissionsStudyLink"))
                    .add(tooltip));
            row.add(new IndicatingAjaxLink<Object>("imgSelect") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    if (checkExists(studyModel, target)) {
                        int[] winSize = WebCfgDelegate.getInstance().getWindowSize("imgSelect");
                        imageSelectionWindow.setInitialWidth(winSize[0]).setInitialHeight(winSize[1]);
                        imageSelectionWindow.show(target, studyModel);
                    }
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
            }
                .add(new Image("selectImg",ImageManager.IMAGE_COMMON_SEARCH)
                .add(new ImageSizeBehaviour())
                .add(tooltip))
                .setVisible(studyPermissionHelper.checkPermission(studyModel, StudyPermission.READ_ACTION))
                .add(new SecurityBehavior(getModuleName() + ":imageSelectionStudyLink"))
            );
            row.add(new AjaxLink<Object>("arr") {
                
                private static final long serialVersionUID = 1L;
                
                @Override
                public void onClick(final AjaxRequestTarget target) {
                	showAuditQueryResult(target, 
                			new AuditRecordRepositoryFacade()
                				.doSearch(AuditRecordRepositoryFacade.Level.STUDY, 
                						studyModel.getStudyInstanceUID()));
                }
            }.add(new Image("arrImg",ImageManager.IMAGE_COMMON_ARR)
            .add(new ImageSizeBehaviour())
            .add(tooltip)
            .add(new SecurityBehavior(getModuleName() + ":arrLink"))));
            row.add(selChkBox.add(tooltip));
            WebMarkupContainer details = new WebMarkupContainer("details") {
                
                private static final long serialVersionUID = 1L;

                @Override
                public boolean isVisible() {
                    return studyModel.isDetails();
                }
            };
            row.add( Webviewer.getLink(studyModel, webviewerLinkProviders, studyPermissionHelper, tooltip, modalWindow)
                    .add(new SecurityBehavior(getModuleName() + ":webviewerStudyLink")));
            item.add(details);
            details.add(new DicomObjectPanel("dicomobject", studyModel, false));
            details.setVisible(studyPermissionHelper.checkPermission(studyModel, StudyPermission.QUERY_ACTION));
            final boolean tooOld = selected.tooOld(studyModel);
            IndicatingAjaxFallbackLink<?> linkStudyBtn = new IndicatingAjaxFallbackLink<Object>("linkStudyBtn") {
                
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    if (checkExists(studyModel, target)) {
                        if (tooOld) {
                            int[] winSize = WebCfgDelegate.getInstance().getWindowSize("linkToOld");
                            confirmLinkMppsStudy.setInitialWidth(winSize[0]).setInitialHeight(winSize[1]);
                            confirmLinkMppsStudy.confirm(target, 
                                    new StringResourceModel("folder.message.tooOld.link",this, null), 
                                    studyModel);
                        } else {
                            setMppsLinkWindow().show(target, studyModel, form);
                        }
                    }
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
                public boolean isVisible() {
                    return studyModel.hasUnlinkedSeries() && studyPermissionHelper.checkPermission(studyModel, StudyPermission.UPDATE_ACTION);
                }
                @Override
                public boolean isEnabled() {
                    return StudyPermissionHelper.get().ignoreEditTimeLimit() || !tooOld;
                }
            };            
            Image image = tooOld ? new Image("linkStudyImg", ImageManager.IMAGE_FOLDER_TIMELIMIT_LINK) : 
                new Image("linkStudyImg", ImageManager.IMAGE_COMMON_LINK);
            image.add(new ImageSizeBehaviour());
            if (tooOld && !StudyPermissionHelper.get().ignoreEditTimeLimit())
                image.add(new AttributeModifier("title", true, new ResourceModel("folder.message.tooOld.link.tooltip")));
            else
                if (tooltip != null) image.add(tooltip);            
            linkStudyBtn.add(image);
            linkStudyBtn.add(new SecurityBehavior(getModuleName() + ":linkPPSLinkStudy"));
            row.add(linkStudyBtn);
            
            IndicatingAjaxFallbackLink<?> unlinkStudyBtn = new IndicatingAjaxFallbackLink<Object>("unlinkStudyBtn") {

                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {     
                    if (checkExists(studyModel, target)) {
                        confirmUnlinkMppsStudy.confirm(target, new StringResourceModel((tooOld ? "folder.message.tooOld.unlink" : "folder.message.confirmUnlink"),this, null,new Object[]{studyModel}), studyModel);
                    }
                }

                @Override
                public boolean isVisible() {
                    return studyModel.hasLinkedSeries() && !studyModel.hasUnlinkedSeries() && studyPermissionHelper.checkPermission(studyModel, StudyPermission.UPDATE_ACTION);
                }
                
                @Override
                public boolean isEnabled() {
                    return StudyPermissionHelper.get().ignoreEditTimeLimit() || !tooOld;
                }
            };
            unlinkStudyBtn.add(new SecurityBehavior(getModuleName() + ":unlinkPPSLinkStudy"));
            image = tooOld ? new Image("unlinkStudyImg", ImageManager.IMAGE_FOLDER_TIMELIMIT_UNLINK) : 
                new Image("unlinkStudyImg", ImageManager.IMAGE_FOLDER_UNLINK);
            image.add(new ImageSizeBehaviour());
            if (tooOld && !StudyPermissionHelper.get().ignoreEditTimeLimit())
                image.add(new AttributeModifier("title", true, new ResourceModel("folder.message.tooOld.unlink.tooltip")));
            else
                if (tooltip != null) image.add(tooltip);            
            unlinkStudyBtn.add(image);
            row.add(unlinkStudyBtn);
            
            item.add(new PPSListView("ppss",
                    studyModel.getPPSs(), patientListItem));
        }
    }

    private final class PPSListView extends PropertyListView<Object> {

        private static final long serialVersionUID = 1L;
        
        private final ListItem<?> ppsListItem;

        private PPSListView(String id, List<PPSModel> list,
                ListItem<?> patientListItem) {
            super(id, list);
            this.ppsListItem = patientListItem;
        }

        @Override
        protected void populateItem(final ListItem<Object> item) {
            item.setOutputMarkupId(true);
 
            final PPSModel ppsModel = (PPSModel) item.getModelObject();
            final boolean tooOld = selected.tooOld(ppsModel);
            
            WebMarkupContainer row = new WebMarkupContainer("row"){
                private static final long serialVersionUID = 1L;
                @Override
                public boolean isVisible() {
                    return !hidePPSModel.getObject();
                }
            };
            AjaxCheckBox selChkBox = new AjaxCheckBox("selected") {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean isVisible() {
                    return ppsModel.getDataset() != null;
                }
                
                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    target.addComponent(this.getParent());
                }
            }; 
            if (ppsModel.getDataset() != null) 
                row.add(new SelectableTableRowBehaviour(selChkBox, "pps", "pps_selected"));
            item.add(row);
            WebMarkupContainer cell = new WebMarkupContainer("cell"){

                private static final long serialVersionUID = 1L;

                @Override
                protected void onComponentTag(ComponentTag tag) {
                   super.onComponentTag(tag);
                   tag.put("rowspan", ppsModel.getRowspan());
                   if (ppsModel.getParent().getPk() == -1) {
                       tag.put("colspan", "2");
                   }
                }
            };
            cell.add(new ExpandCollapseLink("expand", ppsModel, ppsListItem){
                private static final long serialVersionUID = 1L;

                @Override
                public boolean isVisible() {
                    return !viewport.getFilter().getUnconnectedMPPSSearched();
                }
            });
            row.add(cell);
            
            TooltipBehaviour tooltip = new TooltipBehaviour("folder.content.data.pps.");
            
            row.add(new DateTimeLabel("datetime").add(tooltip));
            row.add(new Label("id").add(tooltip));
            row.add(new Label("spsid").add(tooltip));
            row.add(new Label("modality").add(tooltip));
            row.add(new Label("description", 
            		new Model<String>(ppsModel.getDescription() != null &&
            				ppsModel.getDescription().length() > 30 ? 
            						ppsModel.getDescription().substring(0,30) : 
            							ppsModel.getDescription()))
        		.add(new AttributeModifier("title", true, new Model<String>(ppsModel.getDescription()))));
            row.add(new Label("numberOfSeries").add(tooltip));
            row.add(new Label("numberOfInstances").add(tooltip));
            row.add(new Label("status").add(tooltip));
            row.add(new AjaxFallbackLink<Object>("toggledetails") {

                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    if (checkExists(ppsModel, target)) {
                        ppsModel.setDetails(!ppsModel.isDetails());
                        if (target != null) {
                            target.addComponent(StudyListPage.this.get("form"));
                        }
                    }
                }

                @Override
                public boolean isVisible() {
                    return ppsModel.getDataset() != null;
                }
            }.add(new Image("detailImg",ImageManager.IMAGE_COMMON_DICOM_DETAILS)
            .add(new ImageSizeBehaviour())
                .add(tooltip))
            );
            row.add(getEditLink(modalWindow, ppsModel, tooltip)
                    .add(new SecurityBehavior(getModuleName() + ":editPPSLink"))
            );

            IndicatingAjaxFallbackLink<?> linkBtn = new IndicatingAjaxFallbackLink<Object>("linkBtn") {
                
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    if (checkExists(ppsModel, target)) {
                        if (tooOld) {
                            int[] winSize = WebCfgDelegate.getInstance().getWindowSize("linkToOld");
                            confirmLinkMpps.setInitialWidth(winSize[0]).setInitialHeight(winSize[1]);
                            confirmLinkMpps.confirm(target, 
                                    new StringResourceModel("folder.message.tooOld.link",this, null), 
                                    ppsModel);
                        } else {
                            setMppsLinkWindow().show(target, ppsModel, form);
                        }
                    }
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
                public boolean isVisible() {
                    return ppsModel.getDataset() != null && ppsModel.getAccessionNumber()==null;
                }
                
                @Override
                public boolean isEnabled() {
                    return StudyPermissionHelper.get().ignoreEditTimeLimit() || !tooOld;
                }
            };            
            Image image = tooOld ? new Image("linkImg", ImageManager.IMAGE_FOLDER_TIMELIMIT_LINK) : 
                new Image("linkImg", ImageManager.IMAGE_COMMON_LINK);
            image.add(new ImageSizeBehaviour());
            if (tooOld && !StudyPermissionHelper.get().ignoreEditTimeLimit())
                image.add(new AttributeModifier("title", true, new ResourceModel("folder.message.tooOld.link.tooltip")));
            else
                if (tooltip != null) image.add(tooltip);            
            linkBtn.add(image);
            linkBtn.setVisible(studyPermissionHelper.checkPermission(ppsModel, StudyPermission.UPDATE_ACTION));
            linkBtn.add(new SecurityBehavior(getModuleName() + ":linkPPSLink"));
            row.add(linkBtn);
            
            IndicatingAjaxFallbackLink<?> unlinkBtn = new IndicatingAjaxFallbackLink<Object>("unlinkBtn") {

                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {     
                    if (checkExists(ppsModel, target)) {
                        confirmUnlinkMpps.confirm(target, new StringResourceModel((tooOld ? "folder.message.tooOld.unlink" : "folder.message.confirmUnlink"),this, null,new Object[]{ppsModel}), ppsModel);
                    }
                }

                @Override
                public boolean isVisible() {
                    return ppsModel.getDataset() != null && ppsModel.getAccessionNumber()!=null;
                }
                
                @Override
                public boolean isEnabled() {
                    return StudyPermissionHelper.get().ignoreEditTimeLimit() || !tooOld;
                }
            };
            image = tooOld ? new Image("unlinkImg", ImageManager.IMAGE_FOLDER_TIMELIMIT_UNLINK) : 
                new Image("unlinkImg", ImageManager.IMAGE_FOLDER_UNLINK);
            image.add(new ImageSizeBehaviour());
            if (tooOld && !StudyPermissionHelper.get().ignoreEditTimeLimit())
                image.add(new AttributeModifier("title", true, new ResourceModel("folder.message.tooOld.unlink.tooltip")));
            else
                if (tooltip != null) image.add(tooltip);            
            unlinkBtn.add(image);
            unlinkBtn.setVisible(studyPermissionHelper.checkPermission(ppsModel, StudyPermission.UPDATE_ACTION));
            unlinkBtn.add(new SecurityBehavior(getModuleName() + ":unlinkPPSLink"));
            row.add(unlinkBtn);
            
            row.add(new AjaxFallbackLink<Object>("emulateBtn") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    confirmEmulateMpps.confirm(target, 
                            new StringResourceModel(ppsModel.hasForeignPpsInfo() ? 
                                    "folder.message.confirmForcedEmulate" : "folder.message.confirmEmulate"
                                    ,this, null), ppsModel);
                    
                }

                @Override
                public boolean isVisible() {
                    return ppsModel.getDataset() == null;
                }
            }
                .add(new Image("emulateImg",ImageManager.IMAGE_FOLDER_MPPS)
                .add(new ImageSizeBehaviour()).add(tooltip))
                .setVisible(studyPermissionHelper.checkPermission(ppsModel, StudyPermission.APPEND_ACTION))
                .add(new SecurityBehavior(getModuleName() + ":emulatePPSLink"))
            );

            row.add(new AjaxLink<Object>("forward") {
            	
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    if (checkExists(ppsModel, target)) {
                	modalWindow.setContent(new MppsForwardPanel("content", modalWindow, ppsModel));
                        modalWindow.setTitle("");
                        modalWindow.show(target);
                    }
                }
            }.add(new Image("forwardImg", ImageManager.IMAGE_FOLDER_MPPS_FORWARD)
            .add(new ImageSizeBehaviour())
            .add(tooltip))
            .setVisible(ppsModel.getDataset() != null));
            row.add(new AjaxLink<Object>("arr") {

                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(final AjaxRequestTarget target) {
                	showAuditQueryResult(target, 
                			new AuditRecordRepositoryFacade()
                				.doSearch(AuditRecordRepositoryFacade.Level.PPS, 
                						ppsModel.getStudy().getStudyInstanceUID()));
                }
            }.add(new Image("arrImg",ImageManager.IMAGE_COMMON_ARR)
            .add(new ImageSizeBehaviour())
            .add(tooltip)
            .add(new SecurityBehavior(getModuleName() + ":arrLink"))));
            row.add(selChkBox.add(tooltip));
            WebMarkupContainer details = new WebMarkupContainer("details") {
                
                private static final long serialVersionUID = 1L;

                @Override
                public boolean isVisible() {
                    return ppsModel.isDetails();
                }
            };
            item.add(details);
            details.add(new DicomObjectPanel("dicomobject", ppsModel, false));
            details.setVisible(studyPermissionHelper.checkPermission(ppsModel, StudyPermission.QUERY_ACTION));
            item.add(new SeriesListView("series",
                    ppsModel.getSeries(), ppsListItem));
        }
    }

    private final class SeriesListView extends PropertyListView<Object> {

        private static final long serialVersionUID = 1L;
        
        private final ListItem<?> patientListItem;

        private SeriesListView(String id, List<SeriesModel> list,
                ListItem<?> patientListItem) {
            super(id, list);
            this.patientListItem = patientListItem;
        }

        @Override
        protected void populateItem(final ListItem<Object> item) {
            item.setOutputMarkupId(true);
            
            final SeriesModel seriesModel = (SeriesModel) item.getModelObject();
            WebMarkupContainer row = new WebMarkupContainer("row");
            AjaxCheckBox selChkBox = new AjaxCheckBox("selected") {

                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    target.addComponent(this.getParent());
                }
            };
            row.add(new SelectableTableRowBehaviour(selChkBox, "series", "series_selected"));
            item.add(row);
            WebMarkupContainer cell = new WebMarkupContainer("cell"){

                private static final long serialVersionUID = 1L;

                @Override
                protected void onComponentTag(ComponentTag tag) {
                   super.onComponentTag(tag);
                   tag.put("rowspan", seriesModel.getRowspan());
                   if (hidePPSModel.getObject()) {
                       tag.put("colspan", "2");
                   }
                }
            };
            cell.add(new ExpandCollapseLink("expand", seriesModel, patientListItem));
            row.add(cell);
            
            TooltipBehaviour tooltip = new TooltipBehaviour("folder.content.data.series.");
            
            row.add(new DateTimeLabel("datetime").add(tooltip));
            row.add(new Label("seriesNumber").add(tooltip));
            row.add(new Label("sourceAET").add(tooltip));
            row.add(new Label("modality").add(tooltip));
            row.add(new Label("description", 
            		new Model<String>(seriesModel.getDescription() != null &&
            				seriesModel.getDescription().length() > 30 ? 
            						seriesModel.getDescription().substring(0,30) : 
            							seriesModel.getDescription()))
        		.add(new AttributeModifier("title", true, new Model<String>(seriesModel.getDescription()))));
            row.add(new Label("numberOfInstances").add(tooltip));
            row.add(new Label("availability").add(tooltip));
            row.add(new AjaxFallbackLink<Object>("toggledetails") {

                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    if (checkExists(seriesModel, target)) {
                        seriesModel.setDetails(!seriesModel.isDetails());
                        if (target != null) {
                            target.addComponent(patientListItem);
                        }
                    }
                }

            }.add(new Image("detailImg",ImageManager.IMAGE_COMMON_DICOM_DETAILS)
            .add(new ImageSizeBehaviour())
                .add(tooltip))
            );
            row.add(getEditLink(modalWindow, seriesModel, tooltip)
                    .add(new SecurityBehavior(getModuleName() + ":editSeriesLink"))
            );
            row.add(selChkBox.setOutputMarkupId(true).add(tooltip));
            row.add(new IndicatingAjaxLink<Object>("imgSelect") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    if (checkExists(seriesModel, target)) {
                        int[] winSize = WebCfgDelegate.getInstance().getWindowSize("imgSelect");
                        imageSelectionWindow.setInitialWidth(winSize[0]).setInitialHeight(winSize[1]);
                        imageSelectionWindow.show(target, seriesModel);
                    }
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
            }
                .add(new Image("selectImg",ImageManager.IMAGE_COMMON_SEARCH)
                .add(new ImageSizeBehaviour())
                .add(tooltip))
                .setVisible(studyPermissionHelper.checkPermission(seriesModel, StudyPermission.READ_ACTION))
                .add(new SecurityBehavior(getModuleName() + ":imageSelectionSeriesLink"))
            );
            row.add(new AjaxLink<Object>("arr") {

                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(final AjaxRequestTarget target) {
                	showAuditQueryResult(target, 
                			new AuditRecordRepositoryFacade()
                				.doSearch(AuditRecordRepositoryFacade.Level.SERIES, 
                						((StudyModel) seriesModel.getParent().getParent())
                                        .getStudyInstanceUID()));                	
                }
            }.add(new Image("arrImg",ImageManager.IMAGE_COMMON_ARR)
            .add(new ImageSizeBehaviour())
            .add(tooltip)
            .add(new SecurityBehavior(getModuleName() + ":arrLink"))));
            final WebMarkupContainer details = new WebMarkupContainer("details") {
                
                private static final long serialVersionUID = 1L;

                @Override
                public boolean isVisible() {
                    return seriesModel.isDetails();
                }
            };
            row.add(Webviewer.getLink(seriesModel, webviewerLinkProviders, studyPermissionHelper, tooltip, modalWindow)
                .add(new SecurityBehavior(getModuleName() + ":webviewerSeriesLink")));
            item.add(details);
            details.add(new DicomObjectPanel("dicomobject", seriesModel, false));
            details.setVisible(studyPermissionHelper.checkPermission(seriesModel, StudyPermission.QUERY_ACTION));
            item.add(new InstanceListView("instances",
                    seriesModel.getInstances(), patientListItem));
        }
    }

    private final class InstanceListView extends PropertyListView<Object> {

        private static final long serialVersionUID = 1L;
        
        private final ListItem<?> patientListItem;

        private InstanceListView(String id, List<InstanceModel> list,
                ListItem<?> patientListItem) {
            super(id, list);
            this.patientListItem = patientListItem;
        }

        @Override
        protected void populateItem(final ListItem<Object> item) {
            item.setOutputMarkupId(true);
            
            final InstanceModel instModel = (InstanceModel) item.getModelObject();
            WebMarkupContainer row = new WebMarkupContainer("row");
            AjaxCheckBox selChkBox = new AjaxCheckBox("selected") {

                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    target.addComponent(this.getParent());
                }
            };
            row.add(new SelectableTableRowBehaviour(selChkBox, "instance", "instance_selected"));
            item.add(row);
            WebMarkupContainer cell = new WebMarkupContainer("cell"){

                private static final long serialVersionUID = 1L;

                @Override
                protected void onComponentTag(ComponentTag tag) {
                   super.onComponentTag(tag);
                   tag.put("rowspan", instModel.getRowspan());
                }
            };
            cell.add(new ExpandCollapseLink("expand", instModel, patientListItem));
            row.add(cell);
            
            TooltipBehaviour tooltip = new TooltipBehaviour("folder.content.data.instance.");
            
            row.add(new DateTimeLabel("datetime").add(tooltip));
            row.add(new Label("instanceNumber").add(tooltip));
            row.add(new Label("sopClassUID").add(tooltip));
            row.add(new Label("description", 
            		new Model<String>(instModel.getDescription() != null &&
            				instModel.getDescription().length() > 30 ? 
            						instModel.getDescription().substring(0,30) : 
            							instModel.getDescription()))
        		.add(new AttributeModifier("title", true, new Model<String>(instModel.getDescription()))));
            row.add(new Label("availability").add(tooltip));
            row.add(new AjaxFallbackLink<Object>("toggledetails") {

                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    if (checkExists(instModel, target)) {
                        instModel.setDetails(!instModel.isDetails());
                        if (target != null) {
                            target.addComponent(patientListItem);
                        }
                    }
                }
            }.add(new Image("detailImg",ImageManager.IMAGE_COMMON_DICOM_DETAILS)
            .add(new ImageSizeBehaviour())
            .add(tooltip)));
            row.add(getEditLink(modalWindow, instModel, tooltip)
                    .setVisible(studyPermissionHelper.checkPermission(instModel, StudyPermission.UPDATE_ACTION))
                    .add(new SecurityBehavior(getModuleName() + ":editInstanceLink"))
            );
            row.add(Webviewer.getLink(instModel, webviewerLinkProviders, studyPermissionHelper, tooltip, modalWindow)
                .add(new SecurityBehavior(getModuleName() + ":webviewerInstanceLink")));

            row.add(new AjaxLink<Object>("wado") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    if (checkExists(instModel, target)) {
                        int[] winSize = WebCfgDelegate.getInstance().getWindowSize("wado");
                        wadoImageWindow.setInitialWidth(winSize[0]).setInitialHeight(winSize[1]);
                        wadoImageWindow.setPageCreator(new ModalWindow.PageCreator() {
                              
                            private static final long serialVersionUID = 1L;
                              
                            @Override
                            public Page createPage() {
                                return new InstanceViewPage(wadoImageWindow, instModel);                        
                            }
                        });
                        wadoImageWindow.show(target);
                    }
                }
            }
                .add(new Image("wadoImg",ImageManager.IMAGE_FOLDER_WADO)
                .add(new ImageSizeBehaviour())
                .add(tooltip))
                .setVisible(studyPermissionHelper.checkPermission(instModel, StudyPermission.READ_ACTION))
                .add(new SecurityBehavior(getModuleName() + ":wadoImageInstanceLink"))
            );
            row.add(new AjaxLink<Object>("arr") {

                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(final AjaxRequestTarget target) {
                	showAuditQueryResult(target, 
                			new AuditRecordRepositoryFacade()
                				.doSearch(AuditRecordRepositoryFacade.Level.INSTANCE, 
                						((StudyModel) instModel.getParent().getParent().getParent())
                                        .getStudyInstanceUID()));
                }
            }.add(new Image("arrImg",ImageManager.IMAGE_COMMON_ARR)
            .add(new ImageSizeBehaviour())
            .add(tooltip)
            .add(new SecurityBehavior(getModuleName() + ":arrLink"))));
            row.add(selChkBox.add(tooltip));
            WebMarkupContainer details = new WebMarkupContainer("details") {
                
                private static final long serialVersionUID = 1L;

                @Override
                public boolean isVisible() {
                    return instModel.isDetails();
                }
            };
            item.add(details);
            details.add(new DicomObjectPanel("dicomobject", instModel, false));
            details.setVisible(studyPermissionHelper.checkPermission(instModel, StudyPermission.QUERY_ACTION));
            item.add(new FileListView("files", instModel.getFiles(), patientListItem));
        }
    }

    private final class FileListView extends PropertyListView<Object> {

        private static final long serialVersionUID = 1L;
        
        private final ListItem<?> patientListItem;

        private FileListView(String id, List<FileModel> list,
                ListItem<?> patientListItem) {
            super(id, list);
            this.patientListItem = patientListItem;
        }

        @Override
        protected void populateItem(final ListItem<Object> item) {
            item.setOutputMarkupId(true);
            
            TooltipBehaviour tooltip = new TooltipBehaviour("folder.content.data.file.");
            
            final FileModel fileModel = (FileModel) item.getModelObject();
            item.add(new DateTimeLabel("fileObject.createdTime").add(tooltip));
            item.add(new Label("fileObject.fileSize").add(tooltip));
            item.add(new Label("fileObject.transferSyntaxUID").add(tooltip));
            item.add(new Label("fileObject.fileSystem.directoryPath").add(tooltip));
            item.add(new Label("fileObject.filePath").add(tooltip));
            item.add(new Label("fileObject.fileSystem.availability").add(tooltip));
            item.add(new AjaxFallbackLink<Object>("toggledetails") {

                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    fileModel.setDetails(!fileModel.isDetails());
                    if (target != null) {
                        target.addComponent(patientListItem);
                    }
                }

            }.add(new Image("detailImg",ImageManager.IMAGE_COMMON_DICOM_DETAILS)
            .add(new ImageSizeBehaviour())
            .add(tooltip)));
            item.add(getFileDisplayLink(modalWindow, fileModel, tooltip));
            item.add(new AjaxCheckBox("selected") {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean isVisible() {
                    return false;//no action on file level at the moment
                }
                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    target.addComponent(this);
                }}.setOutputMarkupId(true)
                .add(tooltip));
            WebMarkupContainer details = new WebMarkupContainer("details") {
                
                private static final long serialVersionUID = 1L;

                @Override
                public boolean isVisible() {
                    return fileModel.isDetails();
                }
            };
            item.add(details);
            details.add(new FilePanel("file", fileModel));
        }
    }
    
    private Link<Object> getEditLink(final ModalWindow modalWindow, final AbstractEditableDicomModel model, TooltipBehaviour tooltip) {

        final boolean tooOld = selected.tooOld(model);

        int[] winSize = WebCfgDelegate.getInstance().getWindowSize("dcmEdit");
        ModalWindowLink editLink = new ModalWindowLink("edit", modalWindow, winSize[0], winSize[1]) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                if (checkExists(model, target)) {
                    if (tooOld) {
                        confirmEdit.confirm(target, 
                                new StringResourceModel("folder.message.tooOld.edit", this, null), 
                                model);
                        confirmEdit.show(target);
                    } else {
                        modalWindow.setContent(getEditDicomObjectPanel(model));
                        modalWindow.setTitle("");
                        modalWindow.show(target);
                        super.onClick(target);
                    }
                }
            }
            
            @Override
            public boolean isVisible() {
                return model.getDataset()!= null && (!studyPermissionHelper.isUseStudyPermissions() 
                    || checkEditStudyPermission(model));
            }
            
            @Override
            public boolean isEnabled() {
                return StudyPermissionHelper.get().ignoreEditTimeLimit() || !tooOld;
            }
        };
        Image image = tooOld ? new Image("editImg", ImageManager.IMAGE_FOLDER_TIMELIMIT_EDIT) : 
                new Image("editImg", ImageManager.IMAGE_COMMON_DICOM_EDIT);
        image.add(new ImageSizeBehaviour("vertical-align: middle;"));
        if (tooOld && !StudyPermissionHelper.get().ignoreEditTimeLimit())
            image.add(new AttributeModifier("title", true, new ResourceModel("folder.message.tooOld.edit.tooltip")));
        else
            if (tooltip != null) image.add(tooltip);            
        editLink.add(image);
        return editLink;
    }

    private Panel getEditDicomObjectPanel(final AbstractEditableDicomModel model) {
        return
        new EditDicomObjectPanel(
                "content", 
                modalWindow, 
                (DicomObject) model.getDataset(), 
                model.getClass().getSimpleName()
        ) {
           private static final long serialVersionUID = 1L;

           @Override
           protected void onSubmit() {
               model.update(getDicomObject());
               if (model.levelOfModel() > AbstractDicomModel.PATIENT_LEVEL) {
                   AbstractEditableDicomModel m = (AbstractEditableDicomModel)model.getParent();
                   while (m.levelOfModel() > AbstractDicomModel.PATIENT_LEVEL) {
                       m.refresh();
                       m = (AbstractEditableDicomModel)m.getParent();
                   }
               }
               try {
                   ContentEditDelegate.getInstance().doAfterDicomEdit(model);
               } catch (Exception x) {
                   log.warn("doAfterDicomEdit failed!", x);
               }
               super.onCancel();
           }                       
        };
    }
    
    private Mpps2MwlLinkPage setMppsLinkWindow() {
        mpps2MwlLinkWindow.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {              
            
            private static final long serialVersionUID = 1L;

            @Override
            public void onClose(AjaxRequestTarget target) {
                getPage().setOutputMarkupId(true);
                target.addComponent(getPage());
            }
        });
        
        int[] winSize = WebCfgDelegate.getInstance().getWindowSize("mpps2mwl");
        ((Mpps2MwlLinkPage) mpps2MwlLinkWindow)
                .setInitialWidth(winSize[0]).setInitialHeight(winSize[1]);
        return mpps2MwlLinkWindow;
    }
    
    private Link<Object> getFileDisplayLink(final ModalWindow modalWindow, final FileModel fileModel, TooltipBehaviour tooltip) {

        int[] winSize = WebCfgDelegate.getInstance().getWindowSize("dcmFileDisplay");
        final String fsID = fileModel.getFileObject().getFileSystem().getDirectoryPath();
        final String fileID = fileModel.getFileObject().getFilePath();
        ModalWindowLink displayLink = new ModalWindowLink("displayFile", modalWindow, winSize[0], winSize[1]) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                DicomInputStream dis = null;
                try {
                    final File file = fsID.startsWith("tar:") ? TarRetrieveDelegate.getInstance().retrieveFileFromTar(fsID, fileID) :
                        FileUtils.resolve(new File(fsID, fileID));
                    dis =new DicomInputStream(file);
                    final DicomObject obj = dis.readDicomObject();
                    //modalWindow.setContent(new DicomObjectPanel("content", dis.readDicomObject(), true));
                    modalWindow.setPageCreator(new ModalWindow.PageCreator() {
                        @Override
                        public Page createPage() {
                            return new DicomObjectPage(new DicomObjectPanel("content", obj, true));
                        }
                   	
                    });
                    modalWindow.setTitle(new ResourceModel("folder.dcmfileview.title"));
                    modalWindow.show(target);
                    super.onClick(target);
                } catch (Exception e) {
                    log.error("Error requesting dicom object from file: ", e);
                    msgWin.show(target, getString("folder.message.dcmFileError"));
                } finally {
                    if (dis != null)
                        try {
                            dis.close();
                        } catch (IOException ignore) {}
                }
            }            
        };
        Image image = new Image("displayFileImg",ImageManager.IMAGE_FOLDER_DICOM_FILE);
        image.add(new ImageSizeBehaviour("vertical-align: middle;"));
        if (tooltip != null) image.add(tooltip);
        displayLink.add(image);
        displayLink.setVisible(fsID.startsWith("tar:") || FileUtils.resolve(new File(fsID, fileID)).exists());
        return displayLink;
    }

    private Link<Object> getStudyPermissionLink(final ModalWindow modalWindow, final AbstractEditableDicomModel model, TooltipBehaviour tooltip) {
        
        int[] winSize = WebCfgDelegate.getInstance().getWindowSize("studyPerm");
        ModalWindowLink studyPermissionLink
         = new ModalWindowLink("studyPermissions", modalWindow, winSize[0], winSize[1]) {
            
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                if (checkExists(model, target)) {
                    modalWindow.setPageCreator(new ModalWindow.PageCreator() {
                        
                        private static final long serialVersionUID = 1L;
                          
                        @Override
                        public Page createPage() {
                            return new StudyPermissionsPage(model);
                        }
                    });
    
                    modalWindow.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {              
                        
                        private static final long serialVersionUID = 1L;
    
                        @Override
                        public void onClose(AjaxRequestTarget target) {
                            updateStudyPermissions();
                            query(target); 
                            modalWindow.getPage().setOutputMarkupId(true);
                            target.addComponent(modalWindow.getPage());
                            target.addComponent(header);
                        }
                    });
                    modalWindow.add(new ModalWindowLink.DisableDefaultConfirmBehavior());
                    modalWindow.setTitle("");
                    modalWindow.setCloseButtonCallback(null);
                    modalWindow.show(target);
                }
            }
            
            @Override
            public boolean isVisible() {
                return studyPermissionHelper.isManageStudyPermissions() 
                    && model.getDataset() != null
                    && !(model instanceof PatientModel && !((PatientModel) model).isExpandable());
            }
        };
        Image image = new Image("studyPermissionsImg",ImageManager.IMAGE_FOLDER_STUDY_PERMISSIONS);
        image.add(new ImageSizeBehaviour("vertical-align: middle;"));
        if (tooltip != null) image.add(tooltip);
        studyPermissionLink.add(image);
        return studyPermissionLink;
    }

    private boolean checkEditStudyPermission(AbstractDicomModel model) {
        if (!studyPermissionHelper.isUseStudyPermissions()
                || (model instanceof PatientModel))
            return true;
        return studyPermissionHelper.checkPermission(model, StudyPermission.UPDATE_ACTION);
    }

    private class ExpandCollapseLink extends AjaxFallbackLink<Object> {

        private static final long serialVersionUID = 1L;
        
        private AbstractDicomModel model;
        private ListItem<?> patientListItem;
        
        private ExpandCollapseLink(String id, AbstractDicomModel m, ListItem<?> patientListItem) {
            super(id);
            this.model = m;
            this.patientListItem = patientListItem;
            add( new Image(id+"Img", new AbstractReadOnlyModel<ResourceReference>() {

                private static final long serialVersionUID = 1L;

                @Override
                public ResourceReference getObject() {
                    return model.isCollapsed() ? ImageManager.IMAGE_COMMON_EXPAND : 
                        ImageManager.IMAGE_COMMON_COLLAPSE;
                }
            })
            .add(new ImageSizeBehaviour()));
        }
        
        @Override
        public void onClick(AjaxRequestTarget target) {
            if (checkExists(model, target)) {
                if (model.isCollapsed()) model.expand();
                else model.collapse();
                if (target != null) {
                    target.addComponent(patientListItem);
                    if (expandLevelChanged(model))
                        target.addComponent(header);
                }
            }
        }
    }
    
    @Override
    protected void onBeforeRender() {
        applyPageParameters(getPageParameters());
		add(new AbstractBehavior() {
			private static final long serialVersionUID = 1L;

	        public void renderHead(IHeaderResponse response) {
		        super.renderHead(response);
				response.renderOnLoadJavascript(
						"window.scrollTo(retrieveScrollX(), retrieveScrollY());");
			}
		});
        super.onBeforeRender();
    }
    
    protected PageParameters getPageParameters()
    {
        return getRequestCycle().getPageParameters();
    }
    
    protected ViewPort getViewPort()
    {
        return ((AuthenticatedWebSession) AuthenticatedWebSession.get()).getFolderViewPort();
    }

    private void applyPageParameters(PageParameters paras) {
        if (paras != null && !paras.isEmpty()) {
            log.info("applyPageParameters:"+paras);
            StudyListFilter filter = viewport.getFilter();
            filter.setPatientName(paras.getString("patName"));
            filter.setPatientID(paras.getString("patID"));
            filter.setIssuerOfPatientID(paras.getString("issuer"));
            filter.setAccessionNumber(paras.getString("accNr"));
            Date[] studyDate = toDateRange(paras.getString("studyDate"));
            filter.setStudyDateMin(studyDate[0]);
            filter.setStudyDateMax(studyDate[1]);
            filter.setModality(paras.getString("modality"));
            filter.setSourceAET(paras.getString("sourceAET"));
            String studyIUID = paras.getString("studyIUID");
            filter.setStudyInstanceUID(studyIUID);
            String requestStudyIUID = paras.getString("requestStudyIUID");
            filter.setRequestStudyIUID (requestStudyIUID == null ? 
            		Boolean.getBoolean("org.dcm4chee.web3.pageparam.requestStudyIUID") : Boolean.valueOf(requestStudyIUID));
            String seriesIUID = paras.getString("seriesIUID");
            filter.setSeriesInstanceUID(seriesIUID);
            Date[] birthdate = toDateRange(paras.getString("birthdate"));
            filter.setBirthDateMin(birthdate[0]);
            filter.setBirthDateMax(birthdate[1]);
            filter.setExtendedQuery(studyIUID != null || seriesIUID != null || paras.getString("birthdate") != null);
            
            filter.setLatestStudiesFirst(Boolean.valueOf(paras.getString("latestStudiesFirst")));
            filter.setPatientQuery(Boolean.valueOf(paras.getString("patQuery")));
            filter.setPpsWithoutMwl(Boolean.valueOf(paras.getString("ppsWithoutMwl")));
            filter.setWithoutPps(Boolean.valueOf(paras.getString("withoutPps")));
            filter.setExactModalitiesInStudy(Boolean.valueOf(paras.getString("exactModalitiesInStudy")));
            filter.setAutoWildcard(WebCfgDelegate.getInstance().getAutoWildcard());
            if (paras.containsKey("disableSearch")) {
                disableSearch = Boolean.valueOf(paras.getString("disableSearch"));
                form.get("pagesize").setVisible(!disableSearch);
                form.get("pagesize.label").setVisible(!disableSearch);
                form.get("viewport").setVisible(!disableSearch);
                form.get("viewport-bottom").setVisible(!disableSearch);
            }
            if (disableSearch || paras.containsKey("showSearch")){
                showSearch = disableSearch ? false : Boolean.valueOf(paras.getString("showSearch"));
                for (WebMarkupContainer wmc : searchTableComponents)
                    wmc.setVisible(showSearch); 
            }
            if (Boolean.valueOf(paras.getString("query"))) {
                query(null);
            }
        }
    }

    private Date[] toDateRange(String s) {
        Date[] d = new Date[2];
        if (s != null) {
            if (s.length() > 3) {
                try {
                    DateRange dr = VR.DT.toDateRange(s.getBytes());
                    d[0] = dr.getStart();
                    d[1] = dr.getEnd();
                } catch (Exception x) {
                    log.warn("Wrong date range format:"+s+" Must be [yyyy[MM[dd[hh[mm[ss]]]]]][-yyyy[MM[dd[hh[mm[ss]]]]]]");
                }
            } else {
                d = DateUtils.fromOffsetToCurrentDateRange(s);
            }
        }
        return d;
    }

    private void logSecurityAlert(AbstractDicomModel model, boolean success, String desc) {
        try {
            PatientModel patInfoModel;
            AbstractDicomModel studyInfoModel = null;
            if (model.levelOfModel() > AbstractDicomModel.PATIENT_LEVEL) {
                studyInfoModel = model;
                while (studyInfoModel.levelOfModel() > AbstractDicomModel.STUDY_LEVEL)
                    studyInfoModel = studyInfoModel.getParent();
                patInfoModel = (PatientModel) studyInfoModel.getParent();
            } else {
                patInfoModel = (PatientModel) model;
            }
            String studyIUID = studyInfoModel == null ? null : studyInfoModel.getAttributeValueAsString(Tag.StudyInstanceUID);
            Auditlog.logSecurityAlert(AuditEvent.TypeCode.OBJECT_SECURITY_ATTRIBUTES_CHANGED, success, desc, 
                    patInfoModel.getId(), patInfoModel.getName(), studyIUID);
        } catch (Exception ignore) {
            log.warn("Audit log of SecurityAlert for overriding editing time limit failed!", ignore);
        }
    }

    private PatientModel getNewPatientModel() {
        PatientModel newPatModel = new PatientModel();
        DicomObject attrs = newPatModel.getDataset();
        attrs.putString(Tag.PatientName, VR.PN, "");
        attrs.putString(Tag.PatientID, VR.LO, createPatID());
        attrs.putString(Tag.IssuerOfPatientID, VR.LO, WebCfgDelegate.getInstance().getIssuerOfPatientID());
        attrs.putDate(Tag.PatientBirthDate, VR.DA, null);
        attrs.putString(Tag.PatientSex, VR.CS, "M");
        return newPatModel;
    }

    private String createPatID() {
        String pattern = WebCfgDelegate.getInstance().getPatientIDPattern();
        StringBuilder sb = new StringBuilder();
        int pos1 = pattern.indexOf('{');
        int pos2 = pattern.indexOf('}');
        sb.append(pattern.substring(0, pos1));
        String p = pattern.substring(++pos1, pos2);
        if (p.charAt(0) == '#') {
            String v = String.valueOf(System.currentTimeMillis());
            int diff = v.length()-p.length();
            if (diff > 0) {
                v = v.substring(diff);
            } else {
                for (int j = diff; j < 0; j++) {
                    sb.append('0');
                }
            }
            sb.append(v);
        } else {
            SimpleDateFormat df = new SimpleDateFormat(p);
            sb.append(df.format(new Date()));
        }
        if (++pos2 < pattern.length())
            sb.append(pattern.substring(pos2));
        return sb.toString();
    }
    
    private boolean checkExists(AbstractDicomModel m, AjaxRequestTarget target) {
        if (m.getPk() == -1 || dao.exists(m.getPk(), m.levelOfModel()))
            return true;
        msgWin.show(target, new ResourceModel("folder.message.doesntExist").wrapOnAssignment(StudyListPage.this));
        AbstractDicomModel p = m.getParent();
        while (p != null && !dao.exists(p.getPk(), p.levelOfModel())) {
            m = p;
            p = m.getParent();
        }
        if (p == null) {
            viewport.getPatients().remove((PatientModel) m);
        } else {
            p.getDicomModelsOfNextLevel().remove(m);
        }
        target.addComponent(form);
        return false;
    }
    
    private void showAuditQueryResult(AjaxRequestTarget target, String result) {
		if (result == null) {
			msgWin
				.setInfoMessage(getString("folder.arr.query.failed"));
			msgWin.setColor("#FF0000");
			msgWin.show(target);
		} else
			arrWindow
        		.setContent(new Label("content", new Model<String>(result))
                .setEscapeModelStrings(false)
                .add(new AttributeModifier("class", true, new Model<String>("arr"))))
                .show(target);
    }
    
    private boolean countForWarning(AjaxRequestTarget target) {
    	int threshold = WebCfgDelegate.getInstance().getSearchWarningThreshold();
    	try {
	        StudyListFilter filter = viewport.getFilter();
	        return filter.isUnconnectedMPPS() ? 
	        		dao.countUnconnectedMPPS(filter) > threshold : 
	        			dao.count(filter, (studyPermissionHelper.applyStudyPermissions() ? 
	                            studyPermissionHelper.getDicomRoles() : null)) > threshold;
        } catch (Throwable x) {
            if ((x instanceof EJBException) && x.getCause() != null) 
                x = x.getCause();
            log.error("Error on queryStudies: ", x);
            msgWin.show(target, new WicketExceptionWithMsgKey("folder.message.searcherror", x), true);
            return false;
        }
    }
}
