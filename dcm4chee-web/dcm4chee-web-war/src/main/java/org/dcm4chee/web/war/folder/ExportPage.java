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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

import org.apache.catalina.connector.ClientAbortException;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.IRequestTarget;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.Response;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.JavascriptPackageResource;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.resources.CompressedResourceReference;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.protocol.http.WebResponse;
import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.VR;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.io.DicomOutputStream;
import org.dcm4che2.io.StopTagInputHandler;
import org.dcm4che2.net.Association;
import org.dcm4che2.net.CommandUtils;
import org.dcm4che2.net.DimseRSPHandler;
import org.dcm4che2.util.StringUtils;
import org.dcm4chee.archive.common.Availability;
import org.dcm4chee.archive.entity.AE;
import org.dcm4chee.archive.entity.File;
import org.dcm4chee.archive.entity.Instance;
import org.dcm4chee.archive.entity.Study;
import org.dcm4chee.archive.entity.StudyPermission;
import org.dcm4chee.archive.util.JNDIUtils;
import org.dcm4chee.web.common.ajax.AjaxIntervalBehaviour;
import org.dcm4chee.web.common.markup.BaseForm;
import org.dcm4chee.web.common.model.ProgressProvider;
import org.dcm4chee.web.common.secure.SecureSession;
import org.dcm4chee.web.common.secure.SecureSessionCheckPage;
import org.dcm4chee.web.common.util.Auditlog;
import org.dcm4chee.web.common.util.CloseRequestSupport;
import org.dcm4chee.web.common.util.FileUtils;
import org.dcm4chee.web.dao.ae.AEHomeLocal;
import org.dcm4chee.web.dao.folder.StudyListLocal;
import org.dcm4chee.web.war.AuthenticatedWebSession;
import org.dcm4chee.web.war.StudyPermissionHelper;
import org.dcm4chee.web.war.config.delegate.WebCfgDelegate;
import org.dcm4chee.web.war.folder.delegate.ExportDelegate;
import org.dcm4chee.web.war.folder.model.InstanceModel;
import org.dcm4chee.web.war.folder.model.PPSModel;
import org.dcm4chee.web.war.folder.model.PatientModel;
import org.dcm4chee.web.war.folder.model.SeriesModel;
import org.dcm4chee.web.war.folder.model.StudyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision: 18442 $ $Date: 2015-04-29 08:48:56 +0200 (Mi, 29 Apr 2015) $
 * @since Jan 11, 2010
 */
public class ExportPage extends SecureSessionCheckPage implements CloseRequestSupport {
    
    private static final ResourceReference CSS = new CompressedResourceReference(ExportPage.class, "export-style.css");
    
    private static final MetaDataKey<AE> LAST_DESTINATION_AET_ATTRIBUTE = new MetaDataKey<AE>(){

        private static final long serialVersionUID = 1L;
    };
    
    private static final MetaDataKey<HashMap<Integer,ExportResult>> EXPORT_RESULTS = new MetaDataKey<HashMap<Integer,ExportResult>>(){

        private static final long serialVersionUID = 1L;
    };
    
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss,SSS");
    private AE destinationAET;
    private boolean closeOnFinished;
    private boolean closeRequest;
    private boolean isClosed;
    private static int id_count = 0;
    private static int id_req_count = 0;

    private List<AE> destinationAETs = new ArrayList<AE>();
    private int resultId;
    private ExportInfo exportInfo;
    
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
    		
    protected ExportResult r;
    private boolean exportPerformed = false;
    
    private static Logger log = LoggerFactory.getLogger(ExportPage.class);
    
    public static java.io.File temp;
    
    public ExportPage(List<PatientModel> list) {
        super();        
        
        StudyPermissionHelper studyPermissionHelper = StudyPermissionHelper.get(); 
        if (ExportPage.CSS != null)
            add(CSSPackageResource.getHeaderContribution(ExportPage.CSS));

        initDestinationAETs();

        HashMap<Integer,ExportResult> results = getSession().getMetaData(EXPORT_RESULTS);
        exportInfo = new ExportInfo(list);
        if ( results == null ) {
            results = new HashMap<Integer,ExportResult>();
            getSession().setMetaData(EXPORT_RESULTS, results);
        }
        resultId = id_count++;
        ExportResult result = new ExportResult(resultId, getPage().getNumericId());
        results.put(resultId, result);
        destinationAET = getSession().getMetaData(LAST_DESTINATION_AET_ATTRIBUTE);
        add(CSSPackageResource.getHeaderContribution(ExportPage.class, "folder-style.css"));

        final BaseForm form = new BaseForm("form");
        form.setResourceIdPrefix("export.");
        add(form);
        
        form.add( new Label("label","DICOM Export"));
        form.addLabel("selectedItems");
        form.addLabel("selectedPats");
        form.add( new Label("selectedPatsValue", new PropertyModel<Integer>(exportInfo, "nrOfPatients")));
        form.add( new Label("deniedPatsValue", new PropertyModel<Integer>(exportInfo, "deniedNrOfPatients"))
            .setVisible(studyPermissionHelper.isUseStudyPermissions()));
        form.addLabel("selectedStudies");
        form.add( new Label("selectedStudiesValue", new PropertyModel<Integer>(exportInfo, "nrOfStudies")));
        form.add( new Label("deniedStudiesValue", new PropertyModel<Integer>(exportInfo, "deniedNrOfStudies"))
            .setVisible(studyPermissionHelper.isUseStudyPermissions()));
        form.addLabel("selectedSeries");
        form.add( new Label("selectedSeriesValue", new PropertyModel<Integer>(exportInfo, "nrOfSeries")));
        form.add( new Label("deniedSeriesValue", new PropertyModel<Integer>(exportInfo, "deniedNrOfSeries"))
            .setVisible(studyPermissionHelper.isUseStudyPermissions()));
        form.addLabel("selectedInstances");
        form.add( new Label("selectedInstancesValue", new PropertyModel<Integer>(exportInfo, "nrOfInstances")));
        form.add( new Label("deniedInstancesValue", new PropertyModel<Integer>(exportInfo, "deniedNrOfInstances"))
            .setVisible(studyPermissionHelper.isUseStudyPermissions()));
        form.addLabel("totInstances");
        form.add( new Label("totInstancesValue", new PropertyModel<Integer>(exportInfo, "totNrOfInstances")));

        form.add(new DropDownChoice<AE>("destinationAETs", destinationModel, destinationAETs, new IChoiceRenderer<AE>(){

            private static final long serialVersionUID = 1L;

            public Object getDisplayValue(AE ae) {
                if (ae.getDescription() == null || ae.getDescription().length() == 0) {
                    return ae.getTitle();
                } else {
                    return ae.getTitle()+"("+ae.getDescription()+")";
                }
            }

            public String getIdValue(AE ae, int idx) {
                return String.valueOf(idx);
            }
        }){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
                return exportInfo.hasSelection() && isExportInactive() && destinationAETs.size() > 0;
            }
        }.setNullValid(false).setOutputMarkupId(true));        	
        
        if (destinationModel.getObject() == null && destinationAETs.size() > 0)
        	destinationModel.setObject(destinationAETs.get(0));
        
        form.addLabel("destinationAETsLabel");
        form.addLabel("exportResultLabel");
        form.add(new Label("exportResult", new AbstractReadOnlyModel<String>() {

            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                if (exportInfo.hasSelection()) {
                    r = getExportResults().get(resultId);
                    exportPerformed = true;
                    return (r == null ? getString("export.message.exportDone") : r.getResultString());
                } else {
                    return getString("export.message.noSelectionForExport");
                }
            }
        }) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onComponentTag(ComponentTag tag) {
                String cssClass = exportPerformed ? 
                        r == null ? "export_succeed" : 
                                r.failedRequests.size() == 0 ? 
                                        "export_running" : "export_failed"
                        :
                        "export_nop";
                log.debug("Export Result CSS class: {}",cssClass);
                tag.getAttributes().put("class", cssClass);
                super.onComponentTag(tag);
            }
        }.setOutputMarkupId(true));
        
        form.add(new AjaxButton("export"){

            private static final long serialVersionUID = 1L;
            
            @Override
            public boolean isEnabled() {
                return exportInfo.hasSelection() && isExportInactive() && destinationAETs.size() > 0;
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                getSession().setMetaData(LAST_DESTINATION_AET_ATTRIBUTE, destinationAET);
                exportSelected();
                target.addComponent(form);
            }
        }.add(new Label("exportText", new ResourceModel("export.exportBtn.text"))
         .add(new AttributeModifier("style", true, new Model<String>("vertical-align: middle"))))
         .setOutputMarkupId(true));
        form.add(new AjaxButton("close"){

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                removeProgressProvider(getExportResults().remove(resultId), true);
                getPage().getPageMap().remove(ExportPage.this);
                target.appendJavascript("javascript:top.window.close()");
                target.addComponent(form);
            }
        }.add(new Label("closeText", new ResourceModel("export.closeBtn.text"))
        .add(new AttributeModifier("style", true, new Model<String>("vertical-align: middle"))))
        .setOutputMarkupId(true));
        form.add(new AjaxCheckBox("closeOnFinished", new IModel<Boolean>(){

            private static final long serialVersionUID = 1L;
            
            public Boolean getObject() {
                return closeOnFinished;
            }
            public void setObject(Boolean object) {
                closeOnFinished = object;
            }
            public void detach() {}
        }){

            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.addComponent(this);
            }
            
        }.setEnabled(exportInfo.hasSelection()));
        form.addLabel("closeOnFinishedLabel");
        
        form.add(new AjaxIntervalBehaviour(700) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onTimer(AjaxRequestTarget target) {
                log.debug("########### Export timer for resultId:", resultId);
                if (closeRequest) {
                    removeProgressProvider(getExportResults().remove(resultId), true);
                    getPage().getPageMap().remove(ExportPage.this);
                    target.appendJavascript("javascript:window.close()");
                    isClosed = true;
                } else {
                    ExportResult result = getExportResults().get(resultId);
                    result.updateRefreshed();
                    if (result != null && !result.isRendered) {
                        target.addComponent(form.get("exportResult"));
                        if (result.nrOfMoverequests == 0) {
                            target.addComponent(form.get("export"));
                            target.addComponent(form.get("destinationAETs"));
                            target.addComponent(form.get("downloadLink"));
                            if (closeOnFinished && result.failedRequests.isEmpty()) {
                                removeProgressProvider(getExportResults().remove(resultId), false);
                                getPage().getPageMap().remove(ExportPage.this);
                                target.appendJavascript("javascript:window.close()");
                            }
                        }
                        result.isRendered = true;
                    }
                }
            }
        });
        add(JavascriptPackageResource.getHeaderContribution(ExportPage.class, "popupcloser.js"));
        final Label downloadError = new Label("downloadError", new Model<String>(""));
        if (!exportInfo.isDownloadable()) {
            downloadError.setDefaultModel(new ResourceModel("export.download.disabled"));
        }
        form.add(downloadError);
        form.add(new Link<Object>("downloadLink") {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
                return exportInfo.isDownloadable() && isExportInactive();
            }

            @Override
            public void onClick() {
                final List<FileToExport> files = getFilesToExport();
                if (files == null || files.isEmpty()) {
                    downloadError.setDefaultModel(new ResourceModel("export.download.missingFile"));
                    return;
                }
                RequestCycle.get().setRequestTarget(new IRequestTarget() {
                       
                    public void detach(RequestCycle requestCycle) {
                    }

                    public void respond(RequestCycle requestCycle) {
                        boolean success = false;
                        OutputStream out = null;
                        try {
                            Response response = requestCycle.getResponse();
                            byte[] buf = new byte[WebCfgDelegate.getInstance().getDefaultFolderPagesize()];
                            HashSet<Integer> sopHash = new HashSet<Integer>();
                            if (files.size() > 1) {
                                response.setContentType("application/zip");
                                ((WebResponse) response).setAttachmentHeader("dicom.zip");
                                ZipOutputStream zos = new ZipOutputStream(response.getOutputStream());
                                out = zos;
                                for (FileToExport fto : files) {
                                    log.debug("Write file to zip:{}", fto.file);
                                    ZipEntry entry = new ZipEntry(getZipEntryName(fto.blobAttrs, sopHash));
                                    zos.putNextEntry(entry);                                
                                    writeDicomFile(fto.file, fto.blobAttrs, zos, buf);
                                    zos.closeEntry();
                                }
                            } else {
                                response.setContentType("application/dicom");
                                ((WebResponse) response).setAttachmentHeader(
                                        getTemplateParam(files.get(0).blobAttrs, "#sopIuid", sopHash)+".dcm");
                                out = response.getOutputStream();
                                writeDicomFile(files.get(0).file, files.get(0).blobAttrs, out, buf);
                            }
                            success = true;
                        } catch (ZipException ze) {
                            log.warn("Problem creating zip file: " + ze);
                        } catch (ClientAbortException cae) {
                            log.warn("Client aborted zip file download: " + cae);
                        } catch (Exception e) {
                            log.error("An error occurred while attempting to stream zip file for download: ", e);
                        } finally {
                            logExport(files, success);
                            try {
                                if (out != null)
                                    out.close();
                            } catch (Exception ignore) {}
                        }
                    }
                });
            }
        }
        .add(new Label("downloadLabel", new ResourceModel("export.downloadBtn.text")))
        .setOutputMarkupId(true)
        );
    }

    private List<FileToExport> getFilesToExport() {
        StudyListLocal dao = (StudyListLocal) JNDIUtils.lookup(StudyListLocal.JNDI_NAME);
        List<Instance> instances = new ArrayList<Instance>();
        for (MoveRequest rq : exportInfo.getMoveRequests()) {
            List<Instance> downloadableInstances = dao.getDownloadableInstances(rq.studyIUIDs, rq.seriesIUIDs, rq.sopIUIDs);
            if (downloadableInstances != null)
                instances.addAll(downloadableInstances);
        }
        log.debug("found instances for file export:{}",instances.size());
        ArrayList<FileToExport> files = new ArrayList<FileToExport>(instances.size());
        Instance instance;
        java.io.File dcmFile;
        for (int i=0,len=instances.size() ; i < len ; i++) {
            instance = instances.get(i); 
            dcmFile = getDicomFile(instance.getFiles());
            if (dcmFile == null) {
                log.warn("Instance {} has no existing file for export!", instance);
                return null;
            }
            files.add(new FileToExport(instance, dcmFile));
        }
        return files;
    }
    
    private java.io.File getDicomFile(List<File> files) {
        java.io.File dcmFile = null;
        int bestAvail = Availability.OFFLINE.ordinal();
        long lastPk = -1;
        int fsAvail;
        File bestFile = null;
        for ( File file : files) {
            fsAvail = file.getFileSystem().getAvailability().ordinal();
            if (fsAvail < bestAvail || (fsAvail == bestAvail && file.getPk() > lastPk)) {
                java.io.File f = new java.io.File(file.getFileSystem().getDirectoryPath(), file.getFilePath());
                if (file.getFileSystem().getDirectoryPath().startsWith("tar:")) {
                    dcmFile = f;
                } else {
                     f = FileUtils.resolve(f);
                    if (!f.exists()) { 
                        log.debug("Dicom file does not exist: {}", f);
                        continue;
                    }
                    dcmFile = f;
                }
                bestAvail = fsAvail;
                bestFile = file;
                lastPk = file.getPk();
            }
        }
        if (dcmFile != null && dcmFile.getPath().startsWith("tar:")) {
            try {
                dcmFile = ExportDelegate.getInstance().retrieveFileFromTar(bestFile, 
                        WebCfgDelegate.getInstance().getObjectName("tarRetrieveServiceName", null));
            } catch (Exception x) {
                log.error("Retrieve file from tar failed!", x);
                dcmFile = null;
            }
        }
        return dcmFile;
    }

    private String getZipEntryName(DicomObject blobAttrs, HashSet<Integer> sopHash) {
        String tmpl = WebCfgDelegate.getInstance().getZipEntryTemplate();
        int pos0 = 0;
        int pos1, pos2;
        StringBuilder sb = new StringBuilder();
        while ((pos1 = tmpl.indexOf('{', pos0)) != -1) {
            pos2 = tmpl.indexOf("}", pos1);
            if (pos2 == -1)
                throw new IllegalArgumentException("Missing '}' in zip entry name template");
            sb.append(tmpl.substring(pos0, pos1++));
            sb.append(getTemplateParam(blobAttrs, tmpl.substring(pos1, pos2++), sopHash));
            pos0 = pos2;
        }
        return sb.toString();
    }
    
    private String getTemplateParam(DicomObject blobAttrs, String param, HashSet<Integer> sopHash) {
        boolean useHash = param.charAt(0) == '#';
        String value;
        if (param.endsWith("patID")) {
            value = blobAttrs.getString(Tag.PatientID);
        } else if (param.endsWith("patName")) {
            value = blobAttrs.getString(Tag.PatientName);
        } else if (param.endsWith("studyIuid")) {
            value = blobAttrs.getString(Tag.StudyInstanceUID);
        } else if (param.endsWith("seriesIuid")) {
            value = blobAttrs.getString(Tag.SeriesInstanceUID);
        } else if (param.endsWith("sopIuid")) {
            value = blobAttrs.getString(Tag.SOPInstanceUID);
            if (useHash) {
                int hash;
                for(hash = value.hashCode() ; !sopHash.add(hash) ; hash++);
                return FileUtils.toHex(hash);
            }
        } else {
            throw new IllegalArgumentException("Unknown zip entry template parameter:"+param);
        }
        return useHash ? FileUtils.toHex(value == null ? -1 : value.hashCode()) : value;
    }

    private void writeDicomFile(java.io.File dcmFile, DicomObject blobAttrs, OutputStream out, byte[] buf) throws FileNotFoundException, IOException {
		DicomInputStream dis = new DicomInputStream(new FileInputStream(FileUtils.resolve(dcmFile)));
        dis.setHandler(new StopTagInputHandler(Tag.PixelData));
        DicomObject attrs = dis.readDicomObject();
        if (!blobAttrs.getString(Tag.SOPInstanceUID).equals(attrs.getString(Tag.MediaStorageSOPInstanceUID))) {
            log.info("SOPInstanceUID has been changed! correct MediaStorageSOPInstanceUID from "
                    +attrs.getString(Tag.MediaStorageSOPInstanceUID)+" to "+blobAttrs.getString(Tag.SOPInstanceUID));
            attrs.putString(Tag.MediaStorageSOPInstanceUID, VR.UI, blobAttrs.getString(Tag.SOPInstanceUID));
        }
        if (!blobAttrs.getString(Tag.SOPClassUID).equals(attrs.getString(Tag.MediaStorageSOPClassUID))) {
            log.info("SOPClassUID has been changed! correct MediaStorageSOPClassUID from "
                    +attrs.getString(Tag.MediaStorageSOPClassUID)+" to "+blobAttrs.getString(Tag.SOPClassUID));
            attrs.putString(Tag.MediaStorageSOPClassUID, VR.UI, blobAttrs.getString(Tag.SOPClassUID));
        }
        blobAttrs.copyTo(attrs);

        @SuppressWarnings("resource")
		DicomOutputStream dos = new DicomOutputStream(out);
        
        dos.setAutoFinish(false);//we have an DeflaterOutputStream
        dos.writeDicomFile(attrs);
        if (dis.tag() >= Tag.PixelData) {
            dos.writeHeader(dis.tag(), dis.vr(), dis.valueLength());
        }
        int len;
        while (dis.available() > 0) {
            len = dis.read(buf);
            out.write(buf, 0, len);
        }
        dis.close();
    }
    
    private void initDestinationAETs() {
        List<String> aetChoices = ((AuthenticatedWebSession) AuthenticatedWebSession.get()).getFolderViewPort().getAetChoices();
        for (int i = aetChoices.size()-1; i >= 0; i--)
            if (aetChoices.get(i).startsWith("(") && aetChoices.get(i).endsWith(")"))
            	aetChoices.remove(i);

        destinationAETs.clear();
        AEHomeLocal dao = (AEHomeLocal) JNDIUtils.lookup(AEHomeLocal.JNDI_NAME);
        destinationAETs.addAll(dao.findAll(null));
        for (int i = destinationAETs.size()-1; i >= 0; i--)
        	if (!aetChoices.contains(destinationAETs.get(i).getTitle()))
        			destinationAETs.remove(i);
        
        if (destinationAETs.size() > 0)
        	if (destinationAET == null)
        		destinationAET = destinationAETs.get(0);
    }

    private HashMap<Integer,ExportResult> getExportResults() {
        return getSession().getMetaData(EXPORT_RESULTS);
    }

    private void exportSelected() {
        ExportResult result = getExportResults().get(resultId);
        if ( result == null ) {
            result = new ExportResult(resultId, getPage().getNumericId());
            getExportResults().put(resultId, result);
        } else {
            getExportResults().get(resultId).clear();
        }
        for (MoveRequest rq : exportInfo.getMoveRequests()) {
            export(destinationAET.getTitle(), rq.patId, rq.studyIUIDs, rq.seriesIUIDs, rq.sopIUIDs, rq.toString(), result);
        }
    }

    private void export(String destAET, String patID, String[] studyIUIDs, String[] seriesIUIDs, String[] sopIUIDs, String descr, ExportResult result) {
        ExportResponseHandler rq = result.newRequest(destAET, descr);
        try {
            ExportDelegate.getInstance().export(destAET, patID, studyIUIDs, seriesIUIDs, sopIUIDs, rq );
        } catch (Exception e) {
            log.error("Export failed!", e);
            rq.reqDescr += " failed. Reason: ";
            Throwable cause = e;
            for ( ; cause.getCause() != null ; cause = cause.getCause()) {}
            rq.reqDescr += cause.getMessage() == null ? cause.getClass().getCanonicalName() :
                    cause.getLocalizedMessage();
            result.requestDone(rq, false);
        }
    }
    
    private String[] toArray(List<String> l) {
        if (l == null) return null;
        return l.toArray(new String[l.size()]);
    }
    
    private List<Study> getStudiesOfPatient(PatientModel pat) {
        return ((StudyListLocal)
                JNDIUtils.lookup(StudyListLocal.JNDI_NAME)).findStudiesOfPatient(pat.getPk(), true, null);
    }

    private boolean isExportInactive() {
        ExportResult r = getExportResults().get(resultId);
        return r == null || r.nrOfMoverequests==0;
    }
    
    private boolean removeProgressProvider(ProgressProvider p, boolean onlyInactive) {
        if (p == null || (onlyInactive && p.inProgress()))
            return false;
        Session s = getSession();
        if ( s instanceof SecureSession) {
            return ((SecureSession) s).removeProgressProvider(p);
        }
        return false;
    }

    public void setCloseRequest() {
        closeRequest = true;
    }
    public boolean isCloseRequested() {
        return closeRequest;
    }
    public boolean isClosed() {
        return isClosed;
    }

    private void logExport(List<FileToExport> files, boolean success) {
        try {
            ArrayList<DicomObject> objs = new ArrayList<DicomObject>(files.size());
            for (FileToExport fte : files) {
                objs.add(fte.blobAttrs);
            }
            Auditlog.logExport("WEB export", objs, success);
        } catch (Exception ignore) {
            log.warn("Audit log of DataExport failed!", ignore);
        }
    }
    
    private class ExportInfo implements Serializable {
        
        private static final long serialVersionUID = 1L;
        
        List<MoveRequest> requests;
        int nrPat, nrStudy, nrSeries, nrInstances;
        long totNrInstances, totNrInstancesLocal;
        int NOTnrPat, NOTnrStudy, NOTnrSeries, NOTnrInstances;
        
        StudyListLocal dao;
        
        private ExportInfo(List<PatientModel> patients) {
            dao = (StudyListLocal) JNDIUtils.lookup(StudyListLocal.JNDI_NAME);
            this.requests = new ArrayList<MoveRequest>(patients.size());
            for (PatientModel pat : patients) {
                if (pat.isSelected()) {
                    prepareStudiesOfPatientRequests(pat);
                } else {
                    prepareStudyRequests(pat.getStudies());
                }
            }
            for (MoveRequest rq : requests) {
                totNrInstances += dao.countDownloadableInstances(rq.studyIUIDs, rq.seriesIUIDs, rq.sopIUIDs);
                totNrInstancesLocal += dao.countDownloadableInstancesLocal(rq.studyIUIDs, rq.seriesIUIDs, rq.sopIUIDs);
            }
        }
        
        public List<MoveRequest> getMoveRequests() {
            return requests;
        }

        public boolean hasSelection() {
            return totNrInstances > 0;
        }
        
        public boolean isDownloadable() {
            return totNrInstances > 0 && totNrInstances == totNrInstancesLocal;
        }

        @SuppressWarnings("unused")
        public int getNrOfPatients() {
            return nrPat;
        }

        @SuppressWarnings("unused")
        public int getNrOfStudies() {
            return nrStudy;
        }

        @SuppressWarnings("unused")
        public int getNrOfSeries() {
            return nrSeries;
        }

        @SuppressWarnings("unused")
        public int getNrOfInstances() {
            return nrInstances;
        }

        @SuppressWarnings("unused")
        public long getTotNrOfInstances() {
            return totNrInstances;
        }

        @SuppressWarnings("unused")
        public int getDeniedNrOfPatients() {
            return NOTnrPat;
        }

        @SuppressWarnings("unused")
        public int getDeniedNrOfStudies() {
            return NOTnrStudy;
        }

        @SuppressWarnings("unused")
        public int getDeniedNrOfSeries() {
            return NOTnrSeries;
        }

        @SuppressWarnings("unused")
        public int getDeniedNrOfInstances() {
            return NOTnrInstances;
        }

        private void prepareStudiesOfPatientRequests(PatientModel pat) {
            ArrayList<String> uids = new ArrayList<String>();
            List<Study> studies = getStudiesOfPatient(pat);
            int allowed = 0;
            for (Study study : studies) {
                boolean denied = StudyPermissionHelper.get().isUseStudyPermissions()
                &&  !(dao.findStudyPermissionActions(study.getStudyInstanceUID(), StudyPermissionHelper.get().getDicomRoles())
                                        .contains(StudyPermission.EXPORT_ACTION));
                if (!denied) {
                    uids.add(study.getStudyInstanceUID());
                    allowed++;
                } else 
                    NOTnrStudy++;
            }
            if (pat.isSelected()) {
                if (allowed == studies.size()) 
                    nrPat++;
                else {
                    NOTnrPat++;
                    nrStudy += allowed;
                }
            }

            log.debug("Selected for export: Studies of Patient:{} StudyUIDs:{}", pat.getId(), uids);
            requests.add(new MoveRequest().setStudyMoveRequest(pat.getId(), uids.isEmpty() ? null : toArray(uids)));
        }

        private void prepareStudyRequests(List<StudyModel> studies) {
            ArrayList<String> uids = new ArrayList<String>();
            for (StudyModel study : studies) {
                boolean denied = StudyPermissionHelper.get().isUseStudyPermissions()
                &&  !(dao.findStudyPermissionActions(study.getStudyInstanceUID(), StudyPermissionHelper.get().getDicomRoles())
                                        .contains(StudyPermission.EXPORT_ACTION));
                if (study.isSelected()) {
                    if (denied) {
                        NOTnrStudy++;
                    } else {
                        uids.add(study.getStudyInstanceUID());
                        nrStudy++;
                    }
                } else {
                    prepareSeriesRequests(study.getStudyInstanceUID(), study.getPPSs(), denied);
                }
            }
            if ( !uids.isEmpty()) {
                log.debug("Selected for export: Studies:{}",uids);
                requests.add( new MoveRequest().setStudyMoveRequest(null, toArray(uids)));
            }
        }

        private void prepareSeriesRequests(String studyIUID, List<PPSModel> ppss, boolean denied) {
            ArrayList<String> uids = new ArrayList<String>();
            for (PPSModel pps : ppss ) {
                if (pps.isSelected()) {
                    log.debug("Selected for export: Series of selected PPS! AccNr:{}",pps.getAccessionNumber());
                    for (SeriesModel series : pps.getSeries()) {
                        if (denied)
                            NOTnrSeries++;
                        else {
                            uids.add(series.getSeriesInstanceUID());
                            nrSeries++;
                        }
                    }
                } else {
                    for (SeriesModel series : pps.getSeries()) {
                        if (series.isSelected()) {
                            if (denied)
                                NOTnrSeries++;
                            else {
                                uids.add(series.getSeriesInstanceUID());
                                nrSeries++;
                            }
                        } else {
                            prepareInstanceRequest(studyIUID, series.getSeriesInstanceUID(), series.getInstances(), denied);
                        }
                    }
                } 
            }
            if ( !uids.isEmpty()) {
                log.debug("Selected for export: Series (selected PPS and selcted Series):{}",uids);
                requests.add( new MoveRequest().setSeriesMoveRequest(null, studyIUID, toArray(uids)));
            }
        }

        private void prepareInstanceRequest(String studyIUID, String seriesIUID,
                List<InstanceModel> instances, boolean denied) {
            ArrayList<String> uids = new ArrayList<String>();
            for (InstanceModel instance : instances) {
                if (instance.isSelected()) {
                    if (denied)
                        NOTnrInstances++;
                    else {
                        uids.add(instance.getSOPInstanceUID());
                        nrInstances++;
                    }
                }
            }
            if ( !uids.isEmpty()) {
                log.debug("Selected for export: Instances:{}",uids);
                requests.add( new MoveRequest().setInstanceMoveRequest(null, studyIUID, seriesIUID, toArray(uids)));
            }
        }
    }
    
    private class ExportResult implements ProgressProvider, Serializable {
        private static final long serialVersionUID = 1L;
        private long start, end, lastRefreshed;
        private List<ExportResponseHandler> moveRequests = new ArrayList<ExportResponseHandler>(); 
        private List<ExportResponseHandler> failedRequests = new ArrayList<ExportResponseHandler>();
        private int nrOfMoverequests;
        private boolean isRendered = true;
        private int pageID;
        
        public ExportResult(int id, int pageID) {
            this.pageID = pageID;
            Session s = getSession();
            if ( s instanceof SecureSession) {
                ((SecureSession) s).addProgressProvider(this);
            }
            lastRefreshed = System.currentTimeMillis();
        }
        
        public String getName() {
            return "DICOM Export";
        }

        public int[] calcTotal() {
            int[] total = new int[4];
            for ( ExportResponseHandler h : moveRequests) {
                total[0] += h.completed;
                total[1] += h.warning;
                total[2] += h.failed;
                total[3] += h.remaining;
            }
            return total;
        }
        
        public void clear() {
            moveRequests.clear();
            failedRequests.clear();
            nrOfMoverequests = 0;
        }

        public ExportResponseHandler newRequest(String destAET, String descr) {
            if (start==0)
                start = System.currentTimeMillis();
            nrOfMoverequests++;
            ExportResponseHandler handler = new ExportResponseHandler(this, descr);
            moveRequests.add(handler);
            isRendered = false;
            return handler;
        }
        
        public void requestDone(ExportResponseHandler h, boolean success) {
            if ( --nrOfMoverequests == 0)
                end = System.currentTimeMillis();
            if (!success)
                failedRequests.add(h);
            this.isRendered = false;
        }

        public String getResultString() {
            int totalRequests = moveRequests.size();
            if (totalRequests == 0) {
                return ExportPage.this.getString("export.message.exportNotStarted");
            } else {
                int[] total = calcTotal();
                StringBuilder sb = new StringBuilder();
                if ( this.nrOfMoverequests == 0) {
                    sb.append(totalRequests).append(" C-MOVE requests done in ")
                    .append(end-start).append(" ms!\n");
                    start = 0;
                } else {
                    sb.append(moveRequests.size()).append(" of ").append(totalRequests)
                    .append(" C-MOVE requests pending!\n");
                }
                sb.append("Instances completed:").append(total[0])
                .append(" warning:").append(total[1]).append(" failed:").append(total[2])
                .append(" remaining:").append(total[3]);
                if (!failedRequests.isEmpty()) {
                    sb.append("\nFailed C-MOVE requests:\n");
                    for ( ExportResponseHandler h : failedRequests) {
                        sb.append(h.id).append(": ").append(h.reqDescr).append("\n");
                    }
                }
                return sb.toString();
            }
        }

        public boolean inProgress() {
            return moveRequests.size() > 0 && nrOfMoverequests > 0;
        }

        public int getStatus() {
            return moveRequests.size() == 0 ? ProgressProvider.NOT_STARTED :
                nrOfMoverequests == 0 ? ProgressProvider.FINISHED : ProgressProvider.BUSY;
        }

        public long getTotal() {
            return moveRequests.size();
        }

        public long getSuccessful() {
            return calcTotal()[0];
        }

        public long getWarnings() {
            return calcTotal()[1];
        }

        public long getFailures() {
            return calcTotal()[2];
        }

        public long getRemaining() {
            return calcTotal()[3];
        }

        public long getStartTimeInMillis() {
            return start;
        }

        public long getEndTimeInMillis() {
            return end;
        }
        public Integer getPopupPageId() {
            return pageID;
        }
        public String getPageClassName() {
            return ExportPage.class.getName();
        }

        public void updateRefreshed() {
            lastRefreshed = System.currentTimeMillis();
        }
        public long getLastRefreshedTimeInMillis() {
            return lastRefreshed;

        }
    }

    private class ExportResponseHandler extends DimseRSPHandler implements Serializable {
        private static final long serialVersionUID = 1L;
        private String reqDescr;
        
        private int id;
        private ExportResult exportResult;
        private long started;

        private int remaining;
        private int completed;
        private int warning;
        private int failed;
        private int status;

        
        public ExportResponseHandler(ExportResult result, String descr) {
            started = System.currentTimeMillis();
            id = id_req_count++;
            this.exportResult = result;
            reqDescr = descr;
        }
        
        @Override
        public int hashCode() {
            return id;
        }
        
        @Override
        public boolean equals(Object o) {
            return ((ExportResponseHandler) o).id == id;
        }

        @Override
        public void onDimseRSP(Association as, DicomObject cmd,
                DicomObject data) {
            log.info("ExportResponseHandler (msgId{}) received C-MOVE-RSP:{}", getMessageID(), cmd);
            exportResult.isRendered = false;
            remaining = cmd.getInt(Tag.NumberOfRemainingSuboperations);
            completed = cmd.getInt(Tag.NumberOfCompletedSuboperations);
            warning = cmd.getInt(Tag.NumberOfWarningSuboperations);
            failed = cmd.getInt(Tag.NumberOfFailedSuboperations);
            if (!CommandUtils.isPending(cmd)) {
                status = cmd.getInt(Tag.Status);
                synchronized (exportResult) {
                    if (status == 0) {
                        exportResult.requestDone(this, true);
                    } else {
                        reqDescr += "\n  failed with status "+StringUtils.shortToHex(status)+
                            " Error Comment:"+cmd.getString(Tag.ErrorComment)+
                            "\n  Started at "+sdf.format(new Date(started))+" failed at "+sdf.format(new Date());
                        exportResult.requestDone(this, false);
                    }
                }
                log.info("Move Request Done. close assoc! calledAET:{}",as.getCalledAET());
                try {
                    as.release(false);
                } catch (InterruptedException e) {
                    log.error("Association release failed! AET:{}", as.getCalledAET());
                }
            }
        }
    }
    
    private class MoveRequest implements Serializable {

        private static final long serialVersionUID = 1L;
        
        String patId;
        String[] studyIUIDs, seriesIUIDs, sopIUIDs;
        
        public MoveRequest setStudyMoveRequest(String patId, String[] studyIUIDs) {
            this.patId = patId;
            this.studyIUIDs = studyIUIDs;
            return this;
        }
        public MoveRequest setSeriesMoveRequest(String patId, String studyIUID, String[] seriesIUIDs) {
            this.patId = patId;
            this.studyIUIDs = new String[]{studyIUID};
            this.seriesIUIDs = seriesIUIDs;
            return this;
        }
        public MoveRequest setInstanceMoveRequest(String patId, String studyIUID, String seriesIUID, String[] sopIUIDs) {
            this.patId = patId;
            this.studyIUIDs = new String[]{studyIUID};
            this.seriesIUIDs = new String[]{seriesIUID};
            this.sopIUIDs = sopIUIDs;
            return this;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("PatId:").append(patId == null ? "[unknown]" : patId);
            if (studyIUIDs != null) 
                sb.append(",studyIUIDs:").append(StringUtils.join(studyIUIDs, ','));
            if (seriesIUIDs != null) 
                sb.append(", seriesIUIDs:").append(StringUtils.join(seriesIUIDs, ','));
            if (sopIUIDs != null) 
                sb.append(", sopIUIDs:").append(StringUtils.join(sopIUIDs, ','));
            return sb.toString();
        }
    }    
    
    private class FileToExport {
        private DicomObject blobAttrs;
        private java.io.File file;
        
        private FileToExport(Instance instance, java.io.File file) {
            blobAttrs = new BasicDicomObject();
            instance.getAttributes(false).copyTo(blobAttrs);
            instance.getSeries().getAttributes(false).copyTo(blobAttrs);
            instance.getSeries().getStudy().getAttributes(false).copyTo(blobAttrs);
            instance.getSeries().getStudy().getPatient().getAttributes().copyTo(blobAttrs);
            this.file = file;
        }
        
        public String toString() {
            return "FileToExport sopIuid:"+blobAttrs.getString(Tag.SOPInstanceUID)+" file:"+file.getPath();
        }
    }
}
