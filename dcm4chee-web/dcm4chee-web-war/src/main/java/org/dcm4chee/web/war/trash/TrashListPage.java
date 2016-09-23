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

package org.dcm4chee.web.war.trash;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow.WindowClosedCallback;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.Link;
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
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4chee.archive.common.Availability;
import org.dcm4chee.archive.entity.File;
import org.dcm4chee.archive.entity.Instance;
import org.dcm4chee.archive.entity.Patient;
import org.dcm4chee.archive.entity.PrivateFile;
import org.dcm4chee.archive.entity.PrivateInstance;
import org.dcm4chee.archive.entity.PrivatePatient;
import org.dcm4chee.archive.entity.PrivateSeries;
import org.dcm4chee.archive.entity.PrivateStudy;
import org.dcm4chee.archive.entity.Study;
import org.dcm4chee.archive.util.JNDIUtils;
import org.dcm4chee.icons.ImageManager;
import org.dcm4chee.icons.behaviours.ImageSizeBehaviour;
import org.dcm4chee.web.common.ajax.MaskingAjaxCallBehavior;
import org.dcm4chee.web.common.behaviours.CheckOneDayBehaviour;
import org.dcm4chee.web.common.behaviours.SelectableTableRowBehaviour;
import org.dcm4chee.web.common.behaviours.TooltipBehaviour;
import org.dcm4chee.web.common.markup.BaseForm;
import org.dcm4chee.web.common.markup.DateTimeLabel;
import org.dcm4chee.web.common.markup.SimpleDateTimeField;
import org.dcm4chee.web.common.markup.modal.ConfirmationWindow;
import org.dcm4chee.web.common.markup.modal.MessageWindow;
import org.dcm4chee.web.common.secure.SecurityBehavior;
import org.dcm4chee.web.common.util.FileUtils;
import org.dcm4chee.web.dao.folder.StudyListLocal;
import org.dcm4chee.web.dao.trash.TrashListFilter;
import org.dcm4chee.web.dao.trash.TrashListLocal;
import org.dcm4chee.web.dao.util.QueryUtil;
import org.dcm4chee.web.service.common.FileImportOrder;
import org.dcm4chee.web.war.AuthenticatedWebSession;
import org.dcm4chee.web.war.StudyPermissionHelper;
import org.dcm4chee.web.war.common.IndicatingAjaxFormSubmitBehavior;
import org.dcm4chee.web.war.common.model.AbstractDicomModel;
import org.dcm4chee.web.war.config.delegate.WebCfgDelegate;
import org.dcm4chee.web.war.folder.DicomObjectPanel;
import org.dcm4chee.web.war.folder.arr.AuditRecordRepositoryFacade;
import org.dcm4chee.web.war.trash.delegate.StoreBridgeDelegate;
import org.dcm4chee.web.war.trash.model.PrivInstanceModel;
import org.dcm4chee.web.war.trash.model.PrivPatientModel;
import org.dcm4chee.web.war.trash.model.PrivSeriesModel;
import org.dcm4chee.web.war.trash.model.PrivStudyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision: 18405 $ $Date: 2015-01-16 15:18:35 +0100 (Fr, 16 Jän 2015) $
 * @since May 10, 2010
 */
public class TrashListPage extends Panel {

    private static Logger log = LoggerFactory.getLogger(TrashListPage.class);

    private static final ResourceReference CSS = new CompressedResourceReference(
            TrashListPage.class, "trash-style.css");

    private Model<Integer> pagesize = new Model<Integer>();

    private static final String MODULE_NAME = "trash";
    private static final long serialVersionUID = 1L;
    private ViewPort viewport = ((AuthenticatedWebSession) AuthenticatedWebSession
            .get()).getTrashViewPort();
    private TrashListHeader header = new TrashListHeader("thead");
    private PrivSelectedEntities selected = new PrivSelectedEntities();

    private boolean showSearch = true;
    private boolean notSearched = true;
    private MessageWindow msgWin = new MessageWindow("msgWin");
    private ModalWindow arrWindow = new ModalWindow("arrWindow");
    
    private List<WebMarkupContainer> searchTableComponents = new ArrayList<WebMarkupContainer>();

    TrashListLocal dao = (TrashListLocal) JNDIUtils
            .lookup(TrashListLocal.JNDI_NAME);

    final MaskingAjaxCallBehavior macb = new MaskingAjaxCallBehavior();

    private TextField<String> accessionNumber;
    private DropDownChoice<String> sourceAET;
    private SimpleDateTimeField dtf;
    private SimpleDateTimeField dtfEnd;

    public TrashListPage(final String id) {
        super(id);

        if (TrashListPage.CSS != null)
            add(CSSPackageResource.getHeaderContribution(TrashListPage.CSS));

        add(macb);

        int[] winSize = WebCfgDelegate.getInstance().getWindowSize("arr");
        arrWindow
        	.setInitialWidth(winSize[0])
        	.setInitialHeight(winSize[1])
        	.setTitle("");

        final TrashListFilter filter = viewport.getFilter();
        final BaseForm form = new BaseForm("form",
                new CompoundPropertyModel<Object>(filter));
        form.setResourceIdPrefix("trash.");
        form.setOutputMarkupId(true);
        add(form);

        form.add(new AjaxFallbackLink<Object>("searchToggle") {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                showSearch = !showSearch;
                for (WebMarkupContainer wmc : searchTableComponents)
                    wmc.setVisible(showSearch);
                target.addComponent(form);
            }
        }.add((new Image("searchToggleImg",
                new AbstractReadOnlyModel<ResourceReference>() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public ResourceReference getObject() {
                        return showSearch ? ImageManager.IMAGE_COMMON_COLLAPSE
                                : ImageManager.IMAGE_COMMON_EXPAND;
                    }
                }).add(new TooltipBehaviour("trash", "searchToggleImg",
                new AbstractReadOnlyModel<Boolean>() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public Boolean getObject() {
                        return showSearch;
                    }
                }))).add(new ImageSizeBehaviour())));

        addQueryFields(filter, form);
        addQueryOptions(form);
        addNavigation(form);
        addActions(form);

        form.add(header);
        form.add(new PatientListView("patients", viewport.getPatients()));
        msgWin.setTitle(MessageWindow.TITLE_WARNING);
        add(msgWin);
        
        add(arrWindow);
        arrWindow.setWindowClosedCallback(new WindowClosedCallback() {
            private static final long serialVersionUID = 1L;

            public void onClose(AjaxRequestTarget target) {
                getPage().setOutputMarkupId(true);
                target.addComponent(getPage());
            }            
        });
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void addQueryFields(final TrashListFilter filter, BaseForm form) {
        IModel<Boolean> enabledModelPat = new AbstractReadOnlyModel<Boolean>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Boolean getObject() {
                return QueryUtil.isUniversalMatch(filter.getStudyInstanceUID());
            }
        };
        IModel<Boolean> enabledModel = new AbstractReadOnlyModel<Boolean>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Boolean getObject() {
                return !filter.isPatientQuery()
                        && QueryUtil.isUniversalMatch(filter
                                .getStudyInstanceUID());
            }
        };

        searchTableComponents.add(form.createAjaxParent("searchLabels"));

        form.addInternalLabel("patientName");
        form.addInternalLabel("patientIDDescr");
        form.addInternalLabel("accessionNumber");
        form.addInternalLabel("sourceAET");
        form.addInternalLabel("deletedDate");

        searchTableComponents.add(form.createAjaxParent("searchFields"));

        form.addPatientNameField("patientName", new PropertyModel<String>(
                filter, "patientName"), new IModel<Boolean>() {
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
        accessionNumber = form.addTextField("accessionNumber", enabledModel,
                false);

        List<String> aetChoices = viewport.getAetChoices();
        if (aetChoices.size() > 0)
            (sourceAET = form.addDropDownChoice("sourceAET", null,
                    new Model<ArrayList<String>>(new ArrayList(aetChoices)),
                    enabledModel, false)).setModelObject(aetChoices.get(0));
        else
            (sourceAET = form.addDropDownChoice("sourceAET", null,
                    new Model<ArrayList<String>>(new ArrayList(aetChoices)),
                    new Model<Boolean>(false), false)).setNullValid(true);
        dtf = form.addDateTimeField("deletedDateMin", new PropertyModel<Date>(filter, "deletedDateMin"), 
                enabledModel, false, true);
        dtfEnd = form.addDateTimeField("deletedDateMax", new PropertyModel<Date>(filter, "deletedDateMax"), enabledModel, true, true);
        dtf.addToDateField(new CheckOneDayBehaviour(dtf, dtfEnd, "onchange"));

        searchTableComponents.add(form.createAjaxParent("searchFooter"));
    }

    private void addQueryOptions(final BaseForm form) {

        final Model<String> searchOptionSelected = new Model<String>();

        form.addDropDownChoice("queryType", searchOptionSelected,
                new Model<ArrayList<String>>() {

                    private static final long serialVersionUID = 1L;

                    public ArrayList<String> getObject() {

                        final ArrayList<String> searchOptionsStrings = new ArrayList<String>(
                                2);
                        searchOptionsStrings.add(new StringResourceModel(
                                "trash.searchOptions.patient",
                                TrashListPage.this, null).getObject());
                        searchOptionsStrings.add(new StringResourceModel(
                                "trash.searchOptions.study",
                                TrashListPage.this, null).getObject());
                        if (!searchOptionsStrings.contains(searchOptionSelected
                                .getObject()))
                            searchOptionSelected.setObject(searchOptionsStrings
                                    .get(1));
                        return searchOptionsStrings;
                    }
                }, new Model<Boolean>(true), true).add(
                new AjaxFormComponentUpdatingBehavior("onchange") {

                    private static final long serialVersionUID = 1L;

                    @SuppressWarnings("unchecked")
                    protected void onUpdate(AjaxRequestTarget target) {
                        boolean b = ((DropDownChoice<String>) getComponent())
                                .getChoices().get(0)
                                .equals(searchOptionSelected.getObject());
                        viewport.getFilter().setPatientQuery(b);
                        target.addComponent(accessionNumber.setEnabled(!b));
                        target.addComponent(sourceAET.setEnabled(!b));
                        target.addComponent(dtf);
                        target.addComponent(dtfEnd);
                    }
                });
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void addNavigation(final BaseForm form) {

        Button resetBtn = new AjaxButton("resetBtn") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {

                form.clearInput();
                retainSelectedPatients();
                viewport.getFilter().clear();
                DropDownChoice sourceAETDropDownChoice = ((DropDownChoice) ((WebMarkupContainer) form
                        .get("searchFields")).get("sourceAET"));
                if (sourceAETDropDownChoice.getChoices().size() > 0)
                    sourceAETDropDownChoice
                            .setModelObject(sourceAETDropDownChoice
                                    .getChoices().get(0));
                else
                    sourceAETDropDownChoice.setNullValid(true);
                DropDownChoice queryTypeDropDownChoice = ((DropDownChoice) ((WebMarkupContainer) form
                        .get("searchFooter"))
                        .get("queryType"));
                queryTypeDropDownChoice
                            .setModelObject(queryTypeDropDownChoice
                                    .getChoices().get(1));
                pagesize.setObject(WebCfgDelegate.getInstance()
                        .getDefaultFolderPagesize());
                notSearched = true;
                form.setOutputMarkupId(true);
                target.addComponent(form);
            }
        };
        resetBtn.setDefaultFormProcessing(false);
        resetBtn.add(new Image("resetImg", ImageManager.IMAGE_COMMON_RESET)
                .add(new ImageSizeBehaviour("vertical-align: middle;")));
        resetBtn.add(new Label("resetText", new ResourceModel(
                "trash.searchFooter.resetBtn.text")).add(new AttributeModifier(
                "style", true, new Model<String>("vertical-align: middle"))));
        form.addComponent(resetBtn);

        IndicatingAjaxButton searchBtn = new IndicatingAjaxButton("searchBtn") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                try {
                    viewport.setOffset(0);
                    viewport.getFilter().setAutoWildcard(
                            WebCfgDelegate.getInstance().getAutoWildcard());
                    queryStudies();
                } catch (Throwable t) {
                    log.error("search failed: ", t);
                }
                target.addComponent(form);
                target.addComponent(header);
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
        searchBtn.add(new Image("searchImg", ImageManager.IMAGE_COMMON_SEARCH)
                .add(new ImageSizeBehaviour("vertical-align: middle;")));
        searchBtn.add(new Label("searchText", new ResourceModel(
                "trash.searchFooter.searchBtn.text"))
                .add(new AttributeModifier("style", true, new Model<String>(
                        "vertical-align: middle;"))));
        form.addComponent(searchBtn);
        form.setDefaultButton(searchBtn);

        form.clearParent();

        pagesize.setObject(WebCfgDelegate.getInstance()
                .getDefaultFolderPagesize());
        form.addDropDownChoice(
                "pagesize",
                pagesize,
                new Model<ArrayList<String>>(new ArrayList(WebCfgDelegate
                        .getInstance().getPagesizeList())),
                new Model<Boolean>(true), true)
                .setNullValid(false)
                .add(new IndicatingAjaxFormSubmitBehavior(form, "onchange",
                        searchBtn) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onSubmit(AjaxRequestTarget target) {
                        if (!WebCfgDelegate.getInstance()
                                .isQueryAfterPagesizeChange())
                            return;
                        try {
                            queryStudies();
                        } catch (Throwable t) {
                            log.error("search failed: ", t);
                        }
                        target.addComponent(form);
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

        form.add(new Link<Object>("prev") {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick() {
                viewport.setOffset(Math.max(0,
                        viewport.getOffset() - pagesize.getObject()));
                queryStudies();
            }

            @Override
            public boolean isVisible() {
                return (!notSearched && !(viewport.getOffset() == 0));
            }
        }.add(new Image("prevImg", ImageManager.IMAGE_COMMON_BACK).add(
                new ImageSizeBehaviour("vertical-align: middle;")).add(
                new TooltipBehaviour("trash.search."))));

        form.add(new Link<Object>("next") {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick() {
                viewport.setOffset(viewport.getOffset() + pagesize.getObject());
                queryStudies();
            }

            @Override
            public boolean isVisible() {
                return (!notSearched && !(viewport.getTotal()
                        - viewport.getOffset() <= pagesize.getObject()));
            }
        }.add(new Image("nextImg", ImageManager.IMAGE_COMMON_FORWARD).add(
                new ImageSizeBehaviour("vertical-align: middle;")).add(
                new TooltipBehaviour("trash.search.")))
                .setVisible(!notSearched));

        // viewport label: use StringResourceModel with key substitution to
        // select
        // property key according notSearched and getTotal.
        Model<?> keySelectModel = new Model<Serializable>() {

            private static final long serialVersionUID = 1L;

            @Override
            public Serializable getObject() {
                return notSearched ? "trash.search.notSearched"
                        : viewport.getFilter().isPatientQuery() ? (viewport
                                .getTotal() == 0 ? "trash.search.noMatchingPatientsFound"
                                : "trash.search.patientsFound")
                                : (viewport.getTotal() == 0 ? "trash.search.noMatchingStudiesFound"
                                        : "trash.search.studiesFound");
            }
        };
        form.add(new Label("viewport", new StringResourceModel("${}",
                TrashListPage.this, keySelectModel, new Object[] { "dummy" }) {

            private static final long serialVersionUID = 1L;

            @Override
            protected Object[] getParameters() {
                return new Object[] {
                        viewport.getOffset() + 1,
                        Math.min(viewport.getOffset() + pagesize.getObject(),
                                viewport.getTotal()), viewport.getTotal() };
            }
        }).setEscapeModelStrings(false));
    }

    private void addActions(final BaseForm form) {

        final ConfirmationWindow<List<PrivateFile>> confirmRestore = new ConfirmationWindow<List<PrivateFile>>(
                "confirmRestore") {

            private static final long serialVersionUID = 1L;

            @Override
            public void onOk(AjaxRequestTarget target) {
                setRemark(null);
                target.addComponent(form);
            }

            @Override
            public void close(AjaxRequestTarget target) {
                target.addComponent(form);
                super.close(target);
            }

            @Override
            public void onConfirmation(AjaxRequestTarget target,
                    List<PrivateFile> files) {

                this.setStatus(new StringResourceModel(
                        "trash.message.restore.running", TrashListPage.this,
                        null));
                getMessageWindowPanel().getOkBtn().setVisible(false);

                try {
                    FileImportOrder fio = new FileImportOrder();
                    if (files.size() > 0) {
                        Collections.sort(files, new Comparator<PrivateFile>() {
                            public int compare(PrivateFile f1, PrivateFile f2) {
                                return f2
                                        .getFileSystem()
                                        .getAvailability()
                                        .compareTo(
                                                f1.getFileSystem()
                                                        .getAvailability());
                            }
                        });

                        boolean allOnline = true;
                        for (PrivateFile privateFile : files) {
                            DicomObject dio = dao
                                    .getDicomAttributes(privateFile.getPk());
                            File file = new File();
                            file.setFilePath(privateFile.getFilePath());
                            file.setFileSize(privateFile.getFileSize());
                            file.setFileStatus(privateFile.getFileStatus());
                            file.setFileSystem(privateFile.getFileSystem());
                            file.setMD5Sum(privateFile.getFileMD5());
                            file.setTransferSyntaxUID(privateFile
                                    .getTransferSyntaxUID());
                            Instance instance = new Instance();
                            file.setInstance(instance);
                            fio.addFile(file, dio);

                            if (allOnline
                                    && privateFile.getFileSystem()
                                            .getAvailability()
                                            .equals(Availability.ONLINE))
                                if (!FileUtils.resolve(
                                        new java.io.File(privateFile
                                                .getFileSystem()
                                                .getDirectoryPath(),
                                                privateFile.getFilePath()))
                                        .exists())
                                    allOnline = false;
                        }

                        if (!allOnline)
                            setRemark(new StringResourceModel(
                                    "trash.message.notAllOnline",
                                    TrashListPage.this, null));
                        StoreBridgeDelegate.getInstance().importFile(fio);
                        removeRestoredEntries();
                        if (WebCfgDelegate.getInstance().showDoneDialogAfterAction()) {
                        	setStatus(new StringResourceModel(
                                "trash.message.restoreDone",
                                TrashListPage.this, null));
                        }
                        if (selected.hasPatients()) {
                            viewport.getPatients().clear();
                            queryStudies();
                        } else
                            selected.refreshView(true);
                        if (!WebCfgDelegate.getInstance().showDoneDialogAfterAction())
                            close(target);
                    } else {
                        setStatus(new StringResourceModel(
                                "trash.message.restoreNotPossible",
                                TrashListPage.this, null));
                    }
                } catch (Throwable t) {
                    setStatus(new StringResourceModel(
                            "trash.message.restoreFailed", TrashListPage.this,
                            null));
                    while (t instanceof javax.management.MBeanException)
                        t = ((javax.management.MBeanException) t).getCause();
                    if (t != null)
                        setRemark(new Model<String>(t.getLocalizedMessage()));
                    log.error("Exception restoring entry:" + t.getMessage(), t);
                }
                target.addComponent(getMessageWindowPanel().getMsgLabel());
                target.addComponent(getMessageWindowPanel().getOkBtn());
            }
        };
        form.add(confirmRestore);

        AjaxButton restoreBtn = new AjaxButton("restoreBtn") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                selected.update(viewport.getPatients());
                selected.deselectChildsOfSelectedEntities();
                if (selected.hasDicomSelection()) {
                    try {
                        HashMap<PrivPatientModel,List<Study>> studiesInFolder = new HashMap<PrivPatientModel,List<Study>>();
                        List<PrivateFile> files = getFilesToRestore(studiesInFolder);
                        log.debug("Files to restore: {}", files);
                        log.debug("Trash restore studiesInFolder: {}", studiesInFolder);
                        if (files.size() > 0) {
                            if (studiesInFolder.isEmpty()) {
                                confirmRestore.setInitialWidth(400);
                                confirmRestore.setInitialHeight(200);
                                confirmRestore.confirm(target,
                                        new StringResourceModel(
                                                "trash.message.confirmRestore",
                                                this, null,
                                                new Object[] { selected }),
                                        files);
                            } else {
                            	int nrOfStudiesInFolder = 0;
                                StringBuilder info = new StringBuilder("<br/>");
                                PrivPatientModel pm;
                                List<Study> studies;
                                for (Entry<PrivPatientModel, List<Study>> e : studiesInFolder.entrySet()) {
                                    pm = e.getKey();
                                    studies = e.getValue();
                                    if (studies.isEmpty())
                                        continue;
                                    info.append(pm.getName()).append(" (").append(pm.getId());
                                    if (pm.getIssuer() != null) {
                                        info.append(" / ").append(pm.getIssuer());
                                    }
                                    info.append("):<br/>");
                                    for (Study st : studies) {
                                    	nrOfStudiesInFolder++;
                                        info.append(st.getStudyInstanceUID());
                                        if (isDifferentPatient(pm, st.getPatient())) {
                                            info.append(" -&gt; ");
                                            appendPatientInfo(info, st.getPatient());
                                        }
                                        info.append("<br/>");
                                    }
                                }
                                confirmRestore.setInitialWidth(830);
                                confirmRestore.setInitialHeight(350);
                                confirmRestore
                                        .confirm(
                                                target,
                                                new StringResourceModel(
                                                        "trash.message.confirmRestoreStudyExists",
                                                        this,
                                                        null,
                                                        new Object[] {
                                                                selected,
                                                                nrOfStudiesInFolder,
                                                                info }), files);
                            }
                        } else {
                            msgWin.show(
                                    target,
                                    getString("trash.message.restoreNotPossible"));
                        }
                    } catch (Throwable t) {
                        while (t instanceof javax.management.MBeanException)
                            t = ((javax.management.MBeanException) t)
                                    .getCause();
                        if (t != null)
                            msgWin.show(
                                    target,
                                    new StringResourceModel(
                                            "trash.message.restoreFailedWithReason",
                                            TrashListPage.this, null,
                                            new Object[] { t
                                                    .getLocalizedMessage() }));
                        log.error(
                                "Exception restoring entry:" + t.getMessage(),
                                t);
                    }
                } else {
                    msgWin.setInfoMessage(getString("trash.message.noSelection"));
                    msgWin.show(target);
                }
            }
        };
        restoreBtn
                .add(new Image("restoreImg", ImageManager.IMAGE_TRASH_RESTORE)
                        .add(new ImageSizeBehaviour("vertical-align: middle;")));
        restoreBtn.add(new Label("restoreText", new ResourceModel(
                "trash.restoreBtn.text")).add(new AttributeModifier("style",
                true, new Model<String>("vertical-align: middle"))));
        form.add(restoreBtn);
        restoreBtn
                .add(new SecurityBehavior(getModuleName() + ":restoreButton"));

        final ConfirmationWindow<PrivSelectedEntities> confirmDelete = new ConfirmationWindow<PrivSelectedEntities>(
                "confirmDelete") {

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
            public void onConfirmation(AjaxRequestTarget target,
                    final PrivSelectedEntities selected) {

                this.setStatus(new StringResourceModel(
                        "trash.message.delete.running", TrashListPage.this,
                        null));
                getMessageWindowPanel().getOkBtn().setVisible(false);

                try {
                    if (selected == null ? removeTrashAll()
                            : removeTrashItems(selected)) {
                        setStatus(new StringResourceModel(
                                "trash.message.deleteDone", TrashListPage.this,
                                null));
                        if (selected == null || selected.hasPatients()) 
                            viewport.getPatients().clear();
                        else 
                            selected.refreshView(true);
                        queryStudies();
                        if (!WebCfgDelegate.getInstance().showDoneDialogAfterAction())
                            close(target);
                    } else
                        setStatus(new StringResourceModel(
                                "trash.message.deleteFailed",
                                TrashListPage.this, null));
                } catch (Throwable t) {
                    log.error((selected == null ? "removeTrashAll"
                            : "removeTrashItems") + " failed: ", t);
                }
                target.addComponent(getMessageWindowPanel().getMsgLabel());
                target.addComponent(getMessageWindowPanel().getOkBtn());
            }
        };
        confirmDelete.setInitialHeight(150);
        form.add(confirmDelete);

        AjaxButton deleteAllBtn = new AjaxButton("deleteAllBtn") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
            	confirmDelete.confirm(target, new StringResourceModel(
                        "trash.message.confirmDeleteAll", this, null), null);
            }
        };
        deleteAllBtn.add(new Image("deleteAllImg",
                ImageManager.IMAGE_TRASH_EMPTY).add(new ImageSizeBehaviour(
                "vertical-align: middle;")));
        deleteAllBtn.add(new Label("deleteAllText", new ResourceModel(
                "trash.deleteAllBtn.text")).add(new AttributeModifier("style",
                true, new Model<String>("vertical-align: middle"))));
        form.add(deleteAllBtn);
        deleteAllBtn.add(new SecurityBehavior(getModuleName()
                + ":deleteAllButton"));

        AjaxButton deleteBtn = new AjaxButton("deleteBtn") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                selected.update(viewport.getPatients());
                selected.deselectChildsOfSelectedEntities();
                log.info("Selected Entities: :" + selected);
                if (selected.hasDicomSelection()) {
                    confirmDelete.confirm(target, new StringResourceModel(
                            "trash.message.confirmDelete", this, null,
                            new Object[] { selected }), selected);
                } else {
                    msgWin.setInfoMessage(getString("trash.message.noSelection"));
                    msgWin.show(target);
                }
            }
        };
        deleteBtn.add(new Image("deleteImg",
                ImageManager.IMAGE_TRASH_DELETE_SELECTED)
                .add(new ImageSizeBehaviour("vertical-align: middle;")));
        deleteBtn.add(new Label("deleteText", new ResourceModel(
                "trash.deleteBtn.text")).add(new AttributeModifier("style",
                true, new Model<String>("vertical-align: middle"))));
        form.add(deleteBtn);
        deleteBtn.add(new SecurityBehavior(getModuleName() + ":deleteButton"));
    }

    private boolean isDifferentPatient(PrivPatientModel pm, Patient p) {
        return notEqual(pm.getDataset().getString(Tag.PatientName), p.getAttributes().getString(Tag.PatientName)) ||
            notEqual(pm.getId(), p.getPatientID()) || 
            notEqual(pm.getIssuer(), p.getIssuerOfPatientID());
    }
    private boolean notEqual(String s1, String s2) {
        return s1 == null ? s2 != null : !s1.equals(s2); 
    }
    private void appendPatientInfo(StringBuilder sb, Patient p) {
        sb.append(p.getAttributes().getString(Tag.PatientName)).append(" (")
        .append(p.getPatientID());
        if (p.getIssuerOfPatientID() != null) {
            sb.append(" / ").append(p.getIssuerOfPatientID());
        }
        sb.append(")");
    }
        
    private void queryStudies() {
        List<String> dicomSecurityRoles = (StudyPermissionHelper.get()
                .applyStudyPermissions() ? StudyPermissionHelper.get()
                .getDicomRoles() : null);
        viewport.setTotal(dao.count(viewport.getFilter(), dicomSecurityRoles));
        updatePatients(dao.findPatients(viewport.getFilter(),
                pagesize.getObject(), viewport.getOffset(), dicomSecurityRoles));
        header.expandToLevel(viewport.getFilter().isPatientQuery() ? AbstractDicomModel.PATIENT_LEVEL
                : AbstractDicomModel.STUDY_LEVEL);
        updateAutoExpandLevel();
        notSearched = false;
    }

    private void updatePatients(List<PrivatePatient> patients) {
        retainSelectedPatients();
        for (PrivatePatient patient : patients) {
            PrivPatientModel patientModel = addPatient(patient);
            if (viewport.getFilter().isPatientQuery()) {
                patientModel.setExpandable(dao.countStudiesOfPatient(patient
                        .getPk(), (StudyPermissionHelper.get()
                        .applyStudyPermissions() ? StudyPermissionHelper.get()
                        .getDicomRoles() : null)) > 0);
            } else {
                StudyListLocal folderDao = (StudyListLocal) JNDIUtils
                        .lookup(StudyListLocal.JNDI_NAME);
                for (PrivateStudy study : patient.getStudies()) {
                    List<String> actions = folderDao
                            .findStudyPermissionActions(
                                    (study).getStudyInstanceUID(),
                                    StudyPermissionHelper.get().getDicomRoles());
                    if (!StudyPermissionHelper.get().applyStudyPermissions()
                            || actions.contains("Q")) {
                        addStudy(study, patientModel);
                        patientModel.setExpandable(true);
                    }

                }
                patientModel.sortStudies();
            }
        }
    }

    private boolean addStudy(PrivateStudy study, PrivPatientModel patient) {
        List<PrivStudyModel> studies = patient.getStudies();
        for (PrivStudyModel studyModel : studies) {
            if (studyModel.getPk() == study.getPk()) {
                return false;
            }
        }
        studies.add(new PrivStudyModel(study, patient));
        return true;
    }

    private void retainSelectedPatients() {
        for (Iterator<PrivPatientModel> it = viewport.getPatients().iterator(); it
                .hasNext();) {
            PrivPatientModel patient = it.next();
            patient.retainSelectedStudies();
            if (patient.isCollapsed() && !patient.isSelected()) {
                it.remove();
            }
        }
    }

    private PrivPatientModel addPatient(PrivatePatient patient) {
        long pk = patient.getPk();
        for (PrivPatientModel patientModel : viewport.getPatients()) {
            if (patientModel.getPk() == pk) {
                return patientModel;
            }
        }
        PrivPatientModel patientModel = new PrivPatientModel(patient);
        viewport.getPatients().add(patientModel);
        return patientModel;
    }

    private boolean expandLevelChanged(AbstractDicomModel model) {
        int currLevel = header.getExpandAllLevel();
        int level = model.levelOfModel();
        if (model.isCollapsed() || currLevel > level) {
            level = getExpandedLevel(0, viewport.getPatients());
        } else {
            level = getExpandedLevel(++level, model.getDicomModelsOfNextLevel());
        }
        header.setExpandAllLevel(level);
        return level != currLevel;
    }

    private int getExpandedLevel(int startLevel,
            List<? extends AbstractDicomModel> list) {
        int level = startLevel;
        if (list != null) {
            startLevel++;
            int l;
            for (AbstractDicomModel m1 : list) {
                if (!m1.isCollapsed()) {
                    l = getExpandedLevel(startLevel,
                            m1.getDicomModelsOfNextLevel());
                    if (l > level)
                        level = l;
                }
            }
        }
        return level;
    }

    private void updateAutoExpandLevel() {
        int level = AbstractDicomModel.PATIENT_LEVEL;
        pat: for (PrivPatientModel patient : viewport.getPatients()) {
            if (!patient.isCollapsed()) {
                for (PrivStudyModel s : patient.getStudies()) {
                    if (level < s.levelOfModel())
                        level = s.levelOfModel();
                    for (PrivSeriesModel se : s.getSeries()) {
                        if (se.isCollapsed()) {
                            level = se.levelOfModel();
                        } else {
                            level = se.getInstances().get(0).levelOfModel();
                            break pat;
                        }

                    }
                }
            }
        }
        header.setExpandAllLevel(level);
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

            final PrivPatientModel patModel = (PrivPatientModel) item
                    .getModelObject();
            WebMarkupContainer row = new WebMarkupContainer("row");
            AjaxCheckBox selChkBox = new AjaxCheckBox("selected") {

                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    target.addComponent(this.getParent());
                }
            };
            row.add(new SelectableTableRowBehaviour(selChkBox, "patient",
                    "patient_selected"));
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

            TooltipBehaviour tooltip = new TooltipBehaviour(
                    "trash.content.data.patient.");

            row.add(new Label("name").add(tooltip));
            row.add(new Label("id").add(tooltip));
            row.add(new Label("issuer").add(tooltip));
            row.add(new DateTimeLabel("birthdate").setWithoutTime(true).add(
                    tooltip));
            row.add(new Label("sex").add(tooltip));
            row.add(new Label("comments").add(tooltip));
            row.add(new AjaxFallbackLink<Object>("toggledetails") {

                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    patModel.setDetails(!patModel.isDetails());
                    if (target != null) {
                        target.addComponent(item);
                    }
                }
            }.add(new Image("detailImg",
                    ImageManager.IMAGE_COMMON_DICOM_DETAILS).add(
                    new ImageSizeBehaviour()).add(tooltip)));
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

        private StudyListView(String id, List<PrivStudyModel> list,
                ListItem<?> patientListItem) {
            super(id, list);
            this.patientListItem = patientListItem;
        }

        @Override
        protected void populateItem(final ListItem<Object> item) {
            item.setOutputMarkupId(true);

            final PrivStudyModel studyModel = (PrivStudyModel) item
                    .getModelObject();
            WebMarkupContainer row = new WebMarkupContainer("row");
            AjaxCheckBox selChkBox = new AjaxCheckBox("selected") {

                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    target.addComponent(this.getParent());
                }
            };
            row.add(new SelectableTableRowBehaviour(selChkBox, "study",
                    "study_selected"));
            item.add(row);
            WebMarkupContainer cell = new WebMarkupContainer("cell") {

                private static final long serialVersionUID = 1L;

                @Override
                protected void onComponentTag(ComponentTag tag) {
                    super.onComponentTag(tag);
                    tag.put("rowspan", studyModel.getRowspan());
                }
            };
            cell.add(new ExpandCollapseLink("expand", studyModel,
                    patientListItem));
            row.add(cell);

            TooltipBehaviour tooltip = new TooltipBehaviour(
                    "trash.content.data.study.");

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
                    studyModel.setDetails(!studyModel.isDetails());
                    if (target != null) {
                        target.addComponent(patientListItem);
                    }
                }
            }.add(new Image("detailImg",
                    ImageManager.IMAGE_COMMON_DICOM_DETAILS).add(
                    new ImageSizeBehaviour()).add(tooltip)));
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
            item.add(details);
            details.add(new DicomObjectPanel("dicomobject", studyModel, false));
            item.add(new SeriesListView("series", studyModel.getSeries(),
                    patientListItem));
        }
    }

    private final class SeriesListView extends PropertyListView<Object> {

        private static final long serialVersionUID = 1L;

        private final ListItem<?> patientListItem;

        private SeriesListView(String id, List<PrivSeriesModel> list,
                ListItem<?> patientListItem) {
            super(id, list);
            this.patientListItem = patientListItem;
        }

        @Override
        protected void populateItem(final ListItem<Object> item) {
            item.setOutputMarkupId(true);

            final PrivSeriesModel seriesModel = (PrivSeriesModel) item
                    .getModelObject();
            WebMarkupContainer row = new WebMarkupContainer("row");
            AjaxCheckBox selChkBox = new AjaxCheckBox("selected") {

                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    target.addComponent(this.getParent());
                }
            };
            row.add(new SelectableTableRowBehaviour(selChkBox, "series",
                    "series_selected"));
            item.add(row);
            WebMarkupContainer cell = new WebMarkupContainer("cell") {

                private static final long serialVersionUID = 1L;

                @Override
                protected void onComponentTag(ComponentTag tag) {
                    super.onComponentTag(tag);
                    tag.put("rowspan", seriesModel.getRowspan());
                }
            };
            cell.add(new ExpandCollapseLink("expand", seriesModel,
                    patientListItem));
            row.add(cell);

            TooltipBehaviour tooltip = new TooltipBehaviour(
                    "trash.content.data.series.");

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
                    seriesModel.setDetails(!seriesModel.isDetails());
                    if (target != null) {
                        target.addComponent(patientListItem);
                    }
                }

            }.add(new Image("detailImg",
                    ImageManager.IMAGE_COMMON_DICOM_DETAILS).add(
                    new ImageSizeBehaviour()).add(tooltip)));
            row.add(new AjaxLink<Object>("arr") {
                
                private static final long serialVersionUID = 1L;
                
                @Override
				public void onClick(final AjaxRequestTarget target) {
                	showAuditQueryResult(target, 
                			new AuditRecordRepositoryFacade()
                				.doSearch(AuditRecordRepositoryFacade.Level.SERIES, 
                						((PrivStudyModel) seriesModel.getParent())
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
                    return seriesModel.isDetails();
                }
            };
            item.add(details);
            details.add(new DicomObjectPanel("dicomobject", seriesModel, false));
            item.add(new InstanceListView("instances", seriesModel
                    .getInstances(), patientListItem));
        }
    }

    private final class InstanceListView extends PropertyListView<Object> {

        private static final long serialVersionUID = 1L;

        private final ListItem<?> patientListItem;

        private InstanceListView(String id, List<PrivInstanceModel> list,
                ListItem<?> patientListItem) {
            super(id, list);
            this.patientListItem = patientListItem;
        }

        @Override
        protected void populateItem(final ListItem<Object> item) {
            item.setOutputMarkupId(true);

            final PrivInstanceModel instModel = (PrivInstanceModel) item
                    .getModelObject();
            WebMarkupContainer row = new WebMarkupContainer("row");
            AjaxCheckBox selChkBox = new AjaxCheckBox("selected") {

                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    target.addComponent(this.getParent());
                }
            };
            row.add(new SelectableTableRowBehaviour(selChkBox, "instance",
                    "instance_selected"));
            item.add(row);

            TooltipBehaviour tooltip = new TooltipBehaviour(
                    "trash.content.data.instance.");

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
                    instModel.setDetails(!instModel.isDetails());
                    if (target != null) {
                        target.addComponent(patientListItem);
                    }
                }

            }.add(new Image("detailImg",
                    ImageManager.IMAGE_COMMON_DICOM_DETAILS).add(
                    new ImageSizeBehaviour()).add(tooltip)));
            row.add(new AjaxLink<Object>("arr") {
                
                private static final long serialVersionUID = 1L;
                
                @Override
				public void onClick(final AjaxRequestTarget target) {
                	showAuditQueryResult(target, 
                			new AuditRecordRepositoryFacade()
                				.doSearch(AuditRecordRepositoryFacade.Level.INSTANCE, 
                						((PrivStudyModel) instModel.getParent().getParent())
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
        }
    }

    private class ExpandCollapseLink extends AjaxFallbackLink<Object> {

        private static final long serialVersionUID = 1L;

        private AbstractDicomModel model;
        private ListItem<?> patientListItem;

        private ExpandCollapseLink(String id, AbstractDicomModel m,
                ListItem<?> patientListItem) {
            super(id);
            this.model = m;
            this.patientListItem = patientListItem;
            add(new Image(id + "Img",
                    new AbstractReadOnlyModel<ResourceReference>() {

                        private static final long serialVersionUID = 1L;

                        @Override
                        public ResourceReference getObject() {
                            return model.isCollapsed() ? ImageManager.IMAGE_COMMON_EXPAND
                                    : ImageManager.IMAGE_COMMON_COLLAPSE;
                        }
                    }).add(new ImageSizeBehaviour()));
        }

        @Override
        public void onClick(AjaxRequestTarget target) {
            if (model.isCollapsed())
                model.expand();
            else
                model.collapse();
            if (target != null) {
                target.addComponent(patientListItem);
                if (expandLevelChanged(model))
                    target.addComponent(header);
            }
        }
    }

    private boolean removeTrashItems(PrivSelectedEntities selected) {
        try {
            List<Long> pks = new ArrayList<Long>();
            for (PrivInstanceModel instanceModel : selected.getInstances())
                pks.add(instanceModel.getPk());
            dao.removeTrashEntities(pks, PrivateInstance.class, false);

            pks = new ArrayList<Long>();
            for (PrivSeriesModel seriesModel : selected.getSeries())
                pks.add(seriesModel.getPk());
            dao.removeTrashEntities(pks, PrivateSeries.class, false);

            pks = new ArrayList<Long>();
            for (PrivStudyModel studyModel : selected.getStudies())
                pks.add(studyModel.getPk());
            dao.removeTrashEntities(pks, PrivateStudy.class, false);

            pks = new ArrayList<Long>();
            for (PrivPatientModel patientModel : selected.getPatients())
                pks.add(patientModel.getPk());
            dao.removeTrashEntities(pks, PrivatePatient.class, false);
        } catch (Exception x) {
            log.error("Delete failed! Reason:" + x.getMessage(), x);
            return false;
        }
        return true;
    }

    private boolean removeTrashAll() {
        try {
            dao.removeTrashAll();
        } catch (Exception x) {
            log.error("Delete failed! Reason:" + x.getMessage(), x);
            return false;
        }
        return true;
    }

    private List<PrivateFile> getFilesToRestore(HashMap<PrivPatientModel, List<Study>> studiesInFolder) {

        List<PrivateFile> files = new ArrayList<PrivateFile>();
        if (selected.hasPatients()) {
            for (PrivPatientModel pp : selected.getPatients()) {
                files.addAll(dao.getFilesForEntity(pp.getPk(),
                        PrivatePatient.class));
                List<Study> studies = dao.getStudiesInFolder(pp.getPk());
                if (!studies.isEmpty()) {
                    studiesInFolder.put(pp, studies);
                }
            }
        }
        if (selected.hasStudies()) {
            for (PrivStudyModel pst : selected.getStudies()) {
                files.addAll(dao.getFilesForEntity(pst.getPk(),
                        PrivateStudy.class));
                List<Study> studies = studiesInFolder.get(pst.getPatient());
                if (studies == null) {
                    studies = dao.getStudiesInFolder(new String[]{pst.getStudyInstanceUID()});
                    if (studies.size() > 0)
                        studiesInFolder.put(pst.getPatient(), studies);
                } else {
                    studies.addAll(dao.getStudiesInFolder(new String[] { pst.getStudyInstanceUID() }));
                }
            }
        }
        if (selected.hasSeries()) {
            for (PrivSeriesModel pse : selected.getSeries()) {
                files.addAll(dao.getFilesForEntity(pse.getPk(),
                        PrivateSeries.class));
                List<Study> studies = studiesInFolder.get(pse.getStudy().getPatient());
                if (studies == null) {
                    studies = dao.getStudiesInFolder(new String[]{pse.getStudy().getStudyInstanceUID()});
                    if (studies.size() > 0)
                        studiesInFolder.put(pse.getStudy().getPatient(), studies);
                } else {
                    studies.addAll(dao.getStudiesInFolder(new String[] { pse.getStudy().getStudyInstanceUID() }));
                }
            }
        }
        if (selected.hasInstances()) {
            for (PrivInstanceModel pi : selected.getInstances()) {
                files.addAll(dao.getFilesForEntity(pi.getPk(),
                        PrivateInstance.class));
                List<Study> studies = studiesInFolder.get(pi.getSeries().getStudy().getPatient());
                if (studies == null) {
                    studies = dao.getStudiesInFolder(new String[]{pi.getSeries().getStudy().getStudyInstanceUID()});
                    if (studies.size() > 0)
                        studiesInFolder.put(pi.getSeries().getStudy().getPatient(), studies);
                } else {
                    studies.addAll(dao.getStudiesInFolder(new String[] { pi.getSeries().getStudy().getStudyInstanceUID() }));
                }
            }
        }
        return files;
    }

    private void removeRestoredEntries() {

        if (selected.hasInstances()) {
            List<Long> pks = new ArrayList<Long>();
            for (PrivInstanceModel pi : selected.getInstances())
                pks.add(pi.getPk());
            dao.removeTrashEntities(pks, PrivateInstance.class, true);
        }
        if (selected.hasSeries()) {
            List<Long> pks = new ArrayList<Long>();
            for (PrivSeriesModel pse : selected.getSeries())
                pks.add(pse.getPk());
            dao.removeTrashEntities(pks, PrivateSeries.class, true);
        }
        if (selected.hasStudies()) {
            List<Long> pks = new ArrayList<Long>();
            for (PrivStudyModel pst : selected.getStudies())
                pks.add(pst.getPk());
            dao.removeTrashEntities(pks, PrivateStudy.class, true);
        }
        if (selected.hasPatients()) {
            List<Long> pks = new ArrayList<Long>();
            for (PrivPatientModel pp : selected.getPatients())
                pks.add(pp.getPk());
            dao.removeTrashEntities(pks, PrivatePatient.class, true);
        }
    }
    
    private void showAuditQueryResult(AjaxRequestTarget target, String result) {
		if (result == null) {
			msgWin
				.setInfoMessage(getString("trash.arr.query.failed"));
			msgWin.setColor("#FF0000");
			msgWin.show(target);
		} else
			arrWindow
        		.setContent(new Label("content", new Model<String>(result))
                .setEscapeModelStrings(false)
                .add(new AttributeModifier("class", true, new Model<String>("arr"))))
                .show(target);
    }
}
