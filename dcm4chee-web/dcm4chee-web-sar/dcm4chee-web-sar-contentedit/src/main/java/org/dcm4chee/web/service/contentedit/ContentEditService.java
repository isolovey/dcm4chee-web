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
 * Java(TM), available at http://sourceforge.net/projects/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * TIANI Medgraph AG.
 * Portions created by the Initial Developer are Copyright (C) 2003-2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * Gunter Zeilinger <gunter.zeilinger@tiani.com>
 * Franz Willer <franz.willer@gwi-ag.com>
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
package org.dcm4chee.web.service.contentedit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.Notification;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.naming.InitialContext;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmDecodeParam;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Tags;
import org.dcm4che2.audit.message.AuditEvent;
import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.ElementDictionary;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.TransferSyntax;
import org.dcm4che2.data.UID;
import org.dcm4che2.data.VR;
import org.dcm4che2.io.DicomOutputStream;
import org.dcm4che2.util.StringUtils;
import org.dcm4che2.util.UIDUtils;
import org.dcm4chee.archive.common.Availability;
import org.dcm4chee.archive.common.PrivateTag;
import org.dcm4chee.archive.conf.AttributeFilter;
import org.dcm4chee.archive.entity.Code;
import org.dcm4chee.archive.entity.File;
import org.dcm4chee.archive.entity.Instance;
import org.dcm4chee.archive.entity.MPPS;
import org.dcm4chee.archive.entity.MWLItem;
import org.dcm4chee.archive.entity.Patient;
import org.dcm4chee.archive.entity.Series;
import org.dcm4chee.archive.entity.Study;
import org.dcm4chee.archive.util.JNDIUtils;
import org.dcm4chee.web.common.util.Auditlog;
import org.dcm4chee.web.common.util.FileUtils;
import org.dcm4chee.web.dao.common.DicomEditLocal;
import org.dcm4chee.web.dao.folder.MppsToMwlLinkLocal;
import org.dcm4chee.web.dao.trash.TrashListLocal;
import org.dcm4chee.web.dao.util.CoercionUtil;
import org.dcm4chee.web.dao.util.IOCMUtil;
import org.dcm4chee.web.dao.vo.EntityTree;
import org.dcm4chee.web.dao.vo.MppsToMwlLinkResult;
import org.dcm4chee.web.service.common.DicomActionNotification;
import org.dcm4chee.web.service.common.FileImportOrder;
import org.dcm4chee.web.service.common.XSLTUtils;
import org.dcm4chee.web.service.common.delegate.TemplatesDelegate;
import org.dcm4chee.web.service.contentedit.iocm.ChangeRequestOrder;
import org.dcm4chee.web.service.contentedit.iocm.ChangeRequestOrder.ChangedInstance;
import org.dcm4chee.web.service.contentedit.iocm.IOCMSupport;
import org.jboss.system.ServiceMBeanSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author franz.willer@gmail.com
 * @version $Revision$ $Date$
 * @since Jan 29, 2009
 */
public class ContentEditService extends ServiceMBeanSupport {

    private static final String STRING = String.class.getName();

    private static Logger log = LoggerFactory.getLogger(ContentEditService.class);
    
    private static final String NONE ="NONE";
    private static final String MWL2STORE_XSL = "mwl-cfindrsp2cstorerq.xsl";

    private static final int[] EXCLUDE_PPS_ATTRS = new int[]{
        Tag.ReferencedPerformedProcedureStepSequence, 
        Tag.PerformedProcedureStepStartDate, Tag.PerformedProcedureStepStartTime};

    private static final String ARCHIVE_EVENT_TYPE = "org.dcm4chex.archive.dcm.ianscu";

    private DicomEditLocal dicomEdit;
    private MppsToMwlLinkLocal mpps2mwl;

    private ObjectName rejNoteServiceName;
    private ObjectName ianScuServiceName;
    private ObjectName moveScuServiceName;
    private ObjectName storeScpServiceName;
    private ObjectName attrModScuServiceName;
    private ObjectName qrServiceName;

    private boolean processIAN;
    private boolean sendIANonMppsLinked;
    private boolean processRejNote;
    private boolean dcm14Stylesheet;

    private boolean useIOCM;
    private IOCMSupport iocm = new IOCMSupport();
    private int[] iocmRequiredInstanceAttributes = new int[]{
            Tag.ContentSequence, Tag.ReferencedInstanceSequence, Tag.ReferencedImageSequence, 
            Tag.ReferencedStudySequence, Tag.ReferencedSeriesSequence, Tag.ContributingEquipmentSequence
    };

    private String addMwlAttrsToMppsXsl;
    
    private boolean notifyArchiveServicesAfterLinking;
    
    private HashSet<String> studyUIDsInMoveOperation = new HashSet<String>();
    
    public boolean isUpdateMwlStatus() {
        return updateMwlStatus;
    }

    public void setUpdateMwlStatus(boolean updateMwlStatus) {
        this.updateMwlStatus = updateMwlStatus;
    }
    private String[] forwardModifiedToAETs;
    
    private String modifyingSystem;
    private String modifyReason;
    private boolean updateMwlStatus;
        
    private static final TransformerFactory tf = TransformerFactory.newInstance();
    protected TemplatesDelegate templates = new TemplatesDelegate(this);
    private String dcm2To14TplName, dcm14To2TplName;
    private Templates dcm2To14Tpl, dcm14To2Tpl;

    private boolean enableForwardOnPatientUpdate;
	private ObjectName archiveIANScuServicename;

    private long iocmForwardDelay;
    
    public String getUIDRoot() {
        return UIDUtils.getRoot();
    }
    
    public void setUIDRoot(String root) {
        UIDUtils.setRoot(root);
    }
    
    public String getRejectionNoteCode() {
        return iocm.getRejectNoteCode().toString()+"\r\n";
    }

    public void setRejectionNoteCode(String code) {
        iocm.setRejectNoteCode(new Code(code));
    }
    
    public String getContributingEquipment() {
        return StringUtils.join(IOCMUtil.getContributingEquipment(), '|');
    }

    public void setContributingEquipment(String s) {
        IOCMUtil.setContributingEquipment(StringUtils.split(s, '|'));
    }

    public boolean isUseIOCM() {
        return useIOCM;
    }

    public void setUseIOCM(boolean useIOCM) {
        if (useIOCM)
            checkAttributeFilterForIOCM();
        this.useIOCM = useIOCM;
    }

    public long getIocmForwardDelay() {
        return iocmForwardDelay;
    }

    public void setIocmForwardDelay(long iocmForwardDelay) {
        this.iocmForwardDelay = iocmForwardDelay;
    }

    private void checkAttributeFilterForIOCM() {
        AttributeFilter filter = AttributeFilter.getInstanceAttributeFilter(null);
        for (int i = 0 ; i < iocmRequiredInstanceAttributes.length ; i++) {
            if (!filter.hasTag(iocmRequiredInstanceAttributes[i]))
                throw new IllegalStateException("InstanceAttributeFilter does not contain "+
                        ElementDictionary.getDictionary().nameOf(iocmRequiredInstanceAttributes[i]));
        }
    }

    public boolean isProcessIAN() {
        return processIAN;
    }

    public void setProcessIAN(boolean processIAN) {
        this.processIAN = processIAN;
    }

    public boolean isProcessRejNote() {
        return processRejNote;
    }

    public void setProcessRejNote(boolean processRejNote) {
        this.processRejNote = processRejNote;
    }
    
    public String getForwardModifiedToAETs() {
        return forwardModifiedToAETs == null ? NONE : StringUtils.join(forwardModifiedToAETs, '\\');
    }

    public void setForwardModifiedToAETs(String aets) {
        this.forwardModifiedToAETs = NONE.equals(aets) ? null : StringUtils.split(aets, '\\');
    }

    public boolean isEnableForwardOnPatientUpdate() {
        return enableForwardOnPatientUpdate;
    }

    public void setEnableForwardOnPatientUpdate(
            boolean enableForwardOnPatientUpdate) {
        this.enableForwardOnPatientUpdate = enableForwardOnPatientUpdate;
    }

    public boolean isSendIANonMppsLinked() {
        return sendIANonMppsLinked;
    }

    public void setSendIANonMppsLinked(boolean sendIANonMppsLinked) {
        this.sendIANonMppsLinked = sendIANonMppsLinked;
    }

    public boolean isNotifyArchiveServicesAfterLinking() {
		return notifyArchiveServicesAfterLinking;
	}

	public void setNotifyArchiveServicesAfterLinking(
			boolean notifyArchiveServicesAfterLinking) {
		this.notifyArchiveServicesAfterLinking = notifyArchiveServicesAfterLinking;
	}

	public String getModifyingSystem() {
        return modifyingSystem;
    }

    public void setModifyingSystem(String modifyingSystem) {
        this.modifyingSystem = modifyingSystem;
    }

    public String getModifyReason() {
        return modifyReason;
    }

    public void setModifyReason(String modifyReason) {
        this.modifyReason = modifyReason;
    }

    public final String getCoerceConfigDir() {
        return templates.getConfigDir();
    }

    public final void setCoerceConfigDir(String path) {
        templates.setConfigDir(path);
    }

    public ObjectName getRejectionNoteServiceName() {
        return rejNoteServiceName;
    }
    public void setRejectionNoteServiceName(ObjectName name) {
        this.rejNoteServiceName = name;
    }

    public ObjectName getIANScuServiceName() {
        return ianScuServiceName;
    }
    public void setIANScuServiceName(ObjectName name) {
        this.ianScuServiceName = name;
    }

    public void setMoveScuServiceName(ObjectName name) {
        this.moveScuServiceName = name;
    }
    public ObjectName getMoveScuServiceName() {
        return moveScuServiceName;
    }

    public ObjectName getStoreScpServiceName() {
        return storeScpServiceName;
    }

    public void setStoreScpServiceName(ObjectName name) {
        storeScpServiceName = name;
    }
    
    public ObjectName getAttrModificationScuServiceName() {
        return this.attrModScuServiceName;
    }

    public void setAttrModificationScuServiceName(ObjectName name) {
        attrModScuServiceName = name;
    }
    
    public ObjectName getQRServiceName() {
        return this.qrServiceName;
    }

    public void setQRServiceName(ObjectName name) {
        qrServiceName = name;
    }
    
    public final ObjectName getTemplatesServiceName() {
        return templates.getTemplatesServiceName();
    }

    public final void setTemplatesServiceName(ObjectName serviceName) {
        templates.setTemplatesServiceName(serviceName);
    }

    public ObjectName getArchiveIANScuServicename() {
		return archiveIANScuServicename;
	}

	public void setArchiveIANScuServicename(ObjectName archiveIANScuServicename) {
		this.archiveIANScuServicename = archiveIANScuServicename;
	}

	public String getAddMwlAttrsToMppsXsl() {
        return addMwlAttrsToMppsXsl == null ? NONE : addMwlAttrsToMppsXsl;
    }

    public void setAddMwlAttrsToMppsXsl(String addMwlAttrToMppsXsl) {
        this.addMwlAttrsToMppsXsl = NONE.equals(addMwlAttrToMppsXsl) ? null : addMwlAttrToMppsXsl;
    }

    public boolean isDcm14Stylesheet() {
        return dcm14Stylesheet;
    }

    public void setDcm14Stylesheet(boolean dcm14Stylesheet) {
        this.dcm14Stylesheet = dcm14Stylesheet;
    }

    public String getDcm2To14Tpl() {
        return dcm2To14TplName;
    }

    public void setDcm2To14Tpl(String name) throws TransformerConfigurationException, MalformedURLException {
        new URL(name);
        dcm2To14Tpl = tf.newTemplates(new StreamSource(name));
        dcm2To14TplName = name;
    }

    public String getDcm14To2Tpl() {
        return dcm14To2TplName;
    }

    public void setDcm14To2Tpl(String name) throws MalformedURLException, TransformerConfigurationException {
        new URL(name);
        dcm14To2Tpl = tf.newTemplates(new StreamSource(name));
        dcm14To2TplName = name;
    }

    public DicomObject moveInstanceToTrash(String iuid, boolean trustPatientIdWithoutIssuer) throws InstanceNotFoundException, MBeanException, ReflectionException {
        DicomObject[] rejNotes = processInstancesDeleted(lookupDicomEditLocal().moveInstanceToTrash(iuid, trustPatientIdWithoutIssuer), "Referenced Series of deleted Instances:");
        return rejNotes == null ? null : rejNotes[0];
    }

    public DicomObject[] moveInstancesToTrash(long[] pks, boolean trustPatientIdWithoutIssuer) throws InstanceNotFoundException, MBeanException, ReflectionException {
        return processInstancesDeleted(lookupDicomEditLocal().moveInstancesToTrash(pks, trustPatientIdWithoutIssuer), "Referenced Series of deleted Instances:");
    }

    public DicomObject moveSeriesToTrash(String iuid, boolean trustPatientIdWithoutIssuer) throws InstanceNotFoundException, MBeanException, ReflectionException {
        DicomObject[] rejNotes = processInstancesDeleted(lookupDicomEditLocal().moveSeriesToTrash(iuid, trustPatientIdWithoutIssuer), "Deleted Series:");
        return rejNotes == null ? null : rejNotes[0];
    }
    
    public DicomObject[] moveSeriessToTrash(long[] pks, boolean trustPatientIdWithoutIssuer) throws InstanceNotFoundException, MBeanException, ReflectionException {
        return processInstancesDeleted(lookupDicomEditLocal().moveSeriesToTrash(pks, trustPatientIdWithoutIssuer), "Deleted Series:");
    }

    public DicomObject[] moveSeriesOfPpsToTrash(long[] pks, boolean trustPatientIdWithoutIssuer) throws InstanceNotFoundException, MBeanException, ReflectionException {
        return processInstancesDeleted(lookupDicomEditLocal().moveSeriesOfPpsToTrash(pks, trustPatientIdWithoutIssuer), "Deleted Series:");
    }
    
    public List<MPPS> deletePps(long[] ppsPks) {
        List<MPPS> mppss = lookupDicomEditLocal().deletePps(ppsPks);
        for (MPPS mpps : mppss) {
            Auditlog.logProcedureRecord(AuditEvent.ActionCode.UPDATE, true, mpps.getPatient().getAttributes(), 
                mpps.getAttributes().getString(new int[]{Tag.ScheduledStepAttributesSequence,0,Tag.StudyInstanceUID}),
                mpps.getAccessionNumber(), "MPPS deleted");
        }
        return mppss;
    }
    
    public DicomObject moveStudyToTrash(String iuid, boolean trustPatientIdWithoutIssuer) throws InstanceNotFoundException, MBeanException, ReflectionException {
        DicomObject[] rejNotes = processStudyDeleted(lookupDicomEditLocal().moveStudyToTrash(iuid, trustPatientIdWithoutIssuer));
        return rejNotes == null ? null : rejNotes[0];
    }
    
    public DicomObject[] moveStudiesToTrash(long[] pks, boolean trustPatientIdWithoutIssuer) throws InstanceNotFoundException, MBeanException, ReflectionException {
        return processStudyDeleted(lookupDicomEditLocal().moveStudiesToTrash(pks, trustPatientIdWithoutIssuer));
    }
    
    public DicomObject[] moveStudyPkToTrash(long pk, boolean trustPatientIdWithoutIssuer) throws InstanceNotFoundException, MBeanException, ReflectionException {
        return processStudyDeleted(lookupDicomEditLocal().moveStudiesToTrash(new long[]{pk}, trustPatientIdWithoutIssuer));
    }

    public DicomObject[] movePatientToTrash(String pid, String issuer, boolean trustPatientIdWithoutIssuer) throws InstanceNotFoundException, MBeanException, ReflectionException {
        EntityTree entityTree = lookupDicomEditLocal().movePatientToTrash(pid, issuer, trustPatientIdWithoutIssuer);
        DicomObject[] rejNotes = processStudyDeleted(entityTree);
        logPatientDeleted(entityTree);
        return rejNotes;
    }
    
    public DicomObject[] movePatientToTrash(long pk, boolean trustPatientIdWithoutIssuer) throws InstanceNotFoundException, MBeanException, ReflectionException {
        return movePatientsToTrash(new long[]{pk}, trustPatientIdWithoutIssuer);
    }
    public DicomObject[] movePatientsToTrash(long[] pks, boolean trustPatientIdWithoutIssuer) throws InstanceNotFoundException, MBeanException, ReflectionException {
        EntityTree entityTree = lookupDicomEditLocal().movePatientsToTrash(pks, trustPatientIdWithoutIssuer);
        DicomObject[] rejNotes = processStudyDeleted(entityTree);
        logPatientDeleted(entityTree);
        return rejNotes;
    }
    public void emptyTrash() throws Exception {
        TrashListLocal dao = (TrashListLocal) JNDIUtils.lookup(TrashListLocal.JNDI_NAME);
        dao.removeTrashAll();
    }

    public int moveInstancesToSeries(long[] instPks, long seriesPk) throws InstanceNotFoundException, MBeanException, ReflectionException {
        DicomObject targetAttrs = lookupDicomEditLocal().getCompositeObjectforSeries(seriesPk);
        EntityTree entityTree = lookupDicomEditLocal().getEntitiesOfInstances(instPks);
        return processMoveEntities(entityTree, targetAttrs, null);
    }

    public int moveInstanceToSeries(String sopIUID, String seriesIUID) throws InstanceNotFoundException, MBeanException, ReflectionException {
        DicomObject targetAttrs = lookupDicomEditLocal().getCompositeObjectforSeries(seriesIUID);
        EntityTree entityTree = lookupDicomEditLocal().getEntitiesOfInstance(sopIUID);
        return processMoveEntities(entityTree, targetAttrs, null);
    }
    
    public int moveSeriesToStudy(long[] seriesPks, long studyPk) throws InstanceNotFoundException, MBeanException, ReflectionException {
        DicomObject targetAttrs = lookupDicomEditLocal().getCompositeObjectforStudy(studyPk);
        EntityTree entityTree = lookupDicomEditLocal().getEntitiesOfSeries(seriesPks);
        return processMoveEntities(entityTree, targetAttrs, EXCLUDE_PPS_ATTRS);
    }

    public int moveSeriesToStudy(String seriesIUID, String studyIUID) throws InstanceNotFoundException, MBeanException, ReflectionException {
        DicomObject targetAttrs = lookupDicomEditLocal().getCompositeObjectforStudy(studyIUID);
        EntityTree entityTree = lookupDicomEditLocal().getEntitiesOfSeries(seriesIUID);
        return processMoveEntities(entityTree, targetAttrs, EXCLUDE_PPS_ATTRS);
    }

    public int moveStudiesToPatient(long[] studyPks, long patPk) throws InstanceNotFoundException, MBeanException, ReflectionException {
        EntityTree tree = moveStudiesToPatient(studyPks, patPk, null);
        return tree == null ? 0 : tree.getAllInstances().size();
    }
    public EntityTree moveStudiesToPatient(long[] studyPks, long patPk, Map<String, String> new2oldUIDmap) throws InstanceNotFoundException, MBeanException, ReflectionException {
        if ( studyPks == null || studyPks.length < 1) {
            return null;
        }
        EntityTree entityTree = lookupDicomEditLocal().moveStudiesToPatient(studyPks, patPk, useIOCM);
        if (!entityTree.isEmpty()) {
            if (new2oldUIDmap != null)
                entityTree.getUIDMap().putAll(new2oldUIDmap);
            propagateMoveStudiesToPatient(entityTree);
        }
        return entityTree;
    }
    
    public void reloadAttributeFilter() {
        AttributeFilter.reload();
    }

    private void propagateMoveStudiesToPatient(EntityTree entityTree) throws InstanceNotFoundException, MBeanException, ReflectionException {
        if (useIOCM) {
            processRejectionNotes(entityTree, true, "Study moved to patient");
        } else {
            DicomObject kos = iocm.getRejectionNotes(entityTree)[0];
            Auditlog.logInstancesAccessed(AuditEvent.ActionCode.UPDATE, true, kos, true, "Study moved to patient. Series of Study:");
        }
        scheduleMoveStudiesToPatient(entityTree);
    }
    private void scheduleMoveStudiesToPatient(EntityTree entityTree) {
        ArrayList<Study> studies = new ArrayList<Study>();
        for (Map<Study, Map<Series, Set<Instance>>> s : entityTree.getEntityTreeMap().values()) {
            studies.addAll(s.keySet());
        }
        try {
            DicomObject obj = new BasicDicomObject();
            obj.putString(Tag.QueryRetrieveLevel, VR.CS, "PATIENT");
            String[] suids = new String[studies.size()];
            int i = 0;
            for (Study study : studies) {
                suids[i++] = study.getStudyInstanceUID();
            }
            studies.iterator().next().getPatient().getAttributes().copyTo(obj);
            obj.putStrings(Tag.StudyInstanceUID, VR.UI, suids);
            server.invoke(qrServiceName, "clearCachedSeriesAttrs", 
                    new Object[]{}, new String[]{});
            if (!useIOCM) {
                log.info("Schedule PATIENT level Attributes Modification Notification (Move Study To Patient)");
                server.invoke(attrModScuServiceName, "scheduleModification", 
                    new Object[]{obj}, new String[]{DicomObject.class.getName()});
            } else {
                String calledAETs = (String) server.getAttribute(rejNoteServiceName, "CalledAETitles");
                if (NONE.equals(calledAETs)) {
                    log.info("IOCM: No CalledAETitles configured in RecectionNote SCU service! Ignore forward.");
                } else {
                    Study study = entityTree.getEntityTreeMap().values().iterator().next().keySet().iterator().next();
                    Map<Series, Set<Instance>> mapSeries = entityTree.getEntityTreeMap().values().iterator().next().values().iterator().next();
                    DicomObject fwdIan = this.makeIAN(study, mapSeries, Availability.ONLINE);
                    forwardIocmModifiedObject(calledAETs, fwdIan);
                }

            }
            if (forwardModifiedToAETs != null) {
                for (int j = 0 ; j < suids.length ; j++ ) {
                    obj.putString(Tag.StudyInstanceUID, VR.UI, suids[j]);
                    DicomObject fwdIan = lookupDicomEditLocal().getIanForForwardModifiedObject(obj, "STUDY");
                    scheduleForward(fwdIan, forwardModifiedToAETs, 0);
                }
            }
        } catch (Exception e) {
            log.error("Scheduling Attributes Modification Notification (Move Study To Patient) failed!", e);
        }
    }

    public int moveStudyToPatient(String studyIUID, String patId, String issuer) throws InstanceNotFoundException, MBeanException, ReflectionException {
        EntityTree entityTree = lookupDicomEditLocal().moveStudyToPatient(studyIUID, patId, issuer, useIOCM);
        if (!entityTree.isEmpty()) {
            propagateMoveStudiesToPatient(entityTree);
        }
        return entityTree.getAllInstances().size();
    }
    
    public Study createStudy(DicomObject studyAttrs, long patPk) {
        return lookupDicomEditLocal().createStudy(studyAttrs, patPk);
    }
    public Study updateStudy(Study study) {
        return lookupDicomEditLocal().updateStudy(study);
    }
    public Series createSeries(DicomObject seriesAttrs, long studyPk) {
        return lookupDicomEditLocal().createSeries(seriesAttrs, studyPk);
    }
    public Series updateSeries(Series series) {
        return lookupDicomEditLocal().updateSeries(series);
    }

    private int processMoveEntities(EntityTree entityTree, DicomObject targetAttrs, int[] excludeTagsForImport)
        throws InstanceNotFoundException, MBeanException,ReflectionException {
        if (entityTree.isEmpty()) {
            log.info("Nothing to move!");
            return 0;
        } else {
        	int result = 0;
        	try {
	            log.info(entityTree.getAllInstances().size()+" instances will be moved!");
	            EntityTree movedEntities = new EntityTree();
	            result = doMove(entityTree, targetAttrs, excludeTagsForImport, movedEntities);
	            if (result < 0) {
	            	return result;
	            }
	            processRejectionNotes(movedEntities, false, "Deleted Instances for move entities:");
	            processIANs(movedEntities, Availability.UNAVAILABLE);
	            return movedEntities.getAllInstances().size();
        	} finally {
        		if (result != -5) {
        			log.debug("########## Remove studyUIDs of current move operation from studyUIDsInMoveOperation!");
	        		synchronized (studyUIDsInMoveOperation) {
	        			studyUIDsInMoveOperation.remove(targetAttrs.getString(Tags.StudyInstanceUID));
		                for ( Map<Study, Map<Series, Set<Instance>>> studies : entityTree.getEntityTreeMap().values() ) {
		                    for (Map.Entry<Study, Map<Series, Set<Instance>>> entry : studies.entrySet() ) {
		                    	studyUIDsInMoveOperation.remove(entry.getKey().getStudyInstanceUID());
		                    }
		                }
	        		}
        		}
        	}
        }
    }
    private synchronized ChangeRequestOrder prepareMoveChangeRequest(EntityTree entityTree, DicomObject targetAttrs, boolean studyIsTarget) {
        DicomObject headerAttrs, seriesAttrs = null;
        String studyIUID, seriesIUID;
        String targetStudyIUID = targetAttrs.getString(Tags.StudyInstanceUID);
   		if (!studyUIDsInMoveOperation.add(targetStudyIUID))
			return null;
   		HashSet<String> studyUIDs = new HashSet<String>();
   		studyUIDs.add(targetStudyIUID);
        ChangeRequestOrder crOrder = new ChangeRequestOrder();
        try {
            for ( Map<Study, Map<Series, Set<Instance>>> studies : entityTree.getEntityTreeMap().values() ) {
                for (Map.Entry<Study, Map<Series, Set<Instance>>> entry : studies.entrySet() ) {
                    studyIUID = entry.getKey().getStudyInstanceUID();
               		if (!studyIUID.equals(targetStudyIUID) && !studyUIDsInMoveOperation.add(studyIUID)) {
               			studyUIDsInMoveOperation.removeAll(studyUIDs);
            			return null;
               		}
               		studyUIDs.add(studyIUID);
                    for (Map.Entry<Series, Set<Instance>> seriesEntry : entry.getValue().entrySet()) {
                        seriesIUID = seriesEntry.getKey().getSeriesInstanceUID();
                        if (studyIsTarget) {
                            seriesAttrs = seriesEntry.getKey().getAttributes(false);
                            seriesAttrs.putString(Tag.SeriesInstanceUID, VR.UI, UIDUtils.createUID());
                            seriesAttrs.putString(seriesAttrs.resolveTag(PrivateTag.CallingAET, PrivateTag.CreatorID, true), 
                                    VR.AE, seriesEntry.getKey().getSourceAET());
                        }
                        for ( Instance i : seriesEntry.getValue()) {
                            if ( i.getFiles().size() < 1)
                                continue;
                            headerAttrs = new BasicDicomObject();
                            targetAttrs.copyTo(headerAttrs);
                            if ( seriesAttrs != null) {
                                seriesAttrs.copyTo(headerAttrs);
                            }
                            i.getAttributes(false).copyTo(headerAttrs);
                            headerAttrs.putString(Tag.SOPInstanceUID, VR.UI, UIDUtils.createUID());
                            crOrder.addChangedInstance(studyIUID, seriesIUID, i, headerAttrs);
                        }
                    }
                }
            }
        } catch (Exception x) {
            log.error("prepareMoveChangeRequest failed!", x);
        }
        return crOrder;
    }

    private int doMove(EntityTree entityTree, DicomObject targetAttrs, int[] excludeTags, EntityTree movedEntities) {
        boolean studyIsTarget = !targetAttrs.containsValue(Tag.SeriesInstanceUID);
        ChangeRequestOrder crOrder = prepareMoveChangeRequest(entityTree, targetAttrs, studyIsTarget);
        if (crOrder == null)
        	return -5;
        DicomEditLocal dicomEdit = lookupDicomEditLocal();
        if (useIOCM)
            iocm.updateUIDs(crOrder);
        int result = 0;
        try {
            for ( ChangedInstance i : crOrder.getInstances().values()) {
                FileImportOrder order = new FileImportOrder();
                DicomObject attrs = i.getChangedHeader();
                if (useIOCM)
                    IOCMUtil.addReplacementAttrs(attrs);
                for ( File f :  i.getFiles()) {
                    //ensure invalid filename in case source entities can't be removed and must be deleted manually
                    dicomEdit.markFilePath(f.getPk(), ".moved", false);
                    DicomObject importHeader = attrs.exclude(excludeTags);
                    order.addFile(f, importHeader);
                }
                try {
                    importFiles(order);
                    movedEntities.addInstance(i.getInstance());
                } catch (Exception x) {
                    log.error("Failed to import instance:"+i);
                    for ( File f :  i.getFiles()) {
                        dicomEdit.markFilePath(f.getPk(), ".moved", true);
                    }
                    throw x;
                }
                
            }
        } catch (Exception x) {
            result = movedEntities.isEmpty() ? -4 : -1;
            log.error("Move failed!"+movedEntities.getAllInstances().size()+
                    " of "+entityTree.getAllInstances().size()+"already moved", x);
        } finally {
            if (movedEntities.isEmpty()) {
                log.info("Nothing moved!");
            } else {
            	try {
            		if (studyIsTarget) {
            			Collection<Map<Study, Map<Series, Set<Instance>>>> patientsMap = movedEntities.getEntityTreeMap().values();
            			for (Map<Study, Map<Series, Set<Instance>>> studiesMap : patientsMap) {
            				for (Map<Series, Set<Instance>> seriesMap : studiesMap.values()) {
            					Set<Series> series = seriesMap.keySet();
            					dicomEdit.deleteSeries(series);
            					log.info(series.size()+" series moved!");
            				}
            			}
            		} else {
            			dicomEdit.deleteInstances(movedEntities.getAllInstances());
            			log.info(movedEntities.getAllInstances().size()+" instances moved!");
            		}
            	} catch (Exception x) {
            		log.error("Failed to delete moved entities!", x);
            		result -= 2;
            	}
            }
        }
        return result;
    }
    
    private void importFiles(FileImportOrder order) throws Exception {
        log.info("import Files:"+order);
        server.invoke(storeScpServiceName, "importFile", 
                new Object[]{order}, new String[]{FileImportOrder.class.getName()});
    }

    public MppsToMwlLinkResult linkMppsToMwl(long[] mppsPks, long mwlPk, String system, String reason) throws InstanceNotFoundException, MBeanException, ReflectionException {
        MppsToMwlLinkResult result = lookupMppsToMwlLinkLocal().linkMppsToMwl(mppsPks, mwlPk, updateMwlStatus,
                emptyAsDefault(system, modifyingSystem), emptyAsDefault(reason, modifyReason));
        doAfterLinkMppsToMwl(result);
        return result;
    }
    public MppsToMwlLinkResult linkMppsToMwl(long[] mppsPks, DicomObject mwlAttrs, Patient pat, String system, String reason) throws InstanceNotFoundException, MBeanException, ReflectionException {
        MppsToMwlLinkResult result = lookupMppsToMwlLinkLocal().linkMppsToMwl(mppsPks, mwlAttrs, pat,
                emptyAsDefault(system, modifyingSystem), emptyAsDefault(reason, modifyReason));
        doAfterLinkMppsToMwl(result);
        return result;
    }

    public void linkMppsToMwl(String mppsIUID, String rpId, String spsId, String system, String reason) throws InstanceNotFoundException, MBeanException, ReflectionException {
        MppsToMwlLinkResult result = lookupMppsToMwlLinkLocal().linkMppsToMwl(mppsIUID, rpId, spsId, updateMwlStatus,
                emptyAsDefault(system, modifyingSystem), emptyAsDefault(reason, modifyReason));
        doAfterLinkMppsToMwl(result);
    }
    
    public List<Patient> selectPatient(DicomObject patAttrs) {
        return lookupMppsToMwlLinkLocal().selectOrCreatePatient(patAttrs);
    }

    
    private String emptyAsDefault(String value, String def) {
        return value == null || value.trim().length() < 1 ? def : value;
    }
    
    private void doAfterLinkMppsToMwl(MppsToMwlLinkResult result) throws InstanceNotFoundException, MBeanException, ReflectionException {
        log.debug("MppsToMwlLinkResult:{}",result);
        logMppsLinkRecord(result);
        EntityTree tree = updateSeriesAttributes(result);
        EntityTree movedStudies = null;
        if (this.addMwlAttrsToMppsXsl != null) {
            addMwlAttrs2Mpps(result);
        }
        this.sendJMXNotification(result);
        log.info("MppsToMwlLinkResult: studiesToMove:"+result.getStudiesToMove().size());
        if (result.getStudiesToMove().size() > 0) {
            Patient pat = result.getMwl().getPatient();
            log.info("Patient of some MPPS are not identical to patient of MWL! Move studies to Patient of MWL:"+
                    pat.getPatientID());
            long[] studyPks = new long[result.getStudiesToMove().size()];
            int i = 0;
            for ( Study s : result.getStudiesToMove()) {
                studyPks[i++] = s.getPk();
                tree.removeStudy(s);
            }
            movedStudies = moveStudiesToPatient(studyPks, pat.getPk(), tree.getUIDMap());
        }
        log.debug("forwardModifiedToAETs:{}", forwardModifiedToAETs);
        ArrayList<DicomObject> fwdIANs = this.getIANs(tree, null);
        log.debug("fwdIANs:{}", fwdIANs);
        if (useIOCM) {
            try {
                processRejectionNotes(tree, true, "MPPS linked to worklist");
                String calledAETs = (String) server.getAttribute(rejNoteServiceName, "CalledAETitles");
                if (NONE.equals(calledAETs)) {
                    log.info("IOCM: No CalledAETitles configured in RecectionNote SCU service! Ignore forward.");
                } else if (!tree.getEntityTreeMap().isEmpty() && !tree.getEntityTreeMap().values().iterator().next().isEmpty()){
                    Study study = tree.getEntityTreeMap().values().iterator().next().keySet().iterator().next();
                    Map<Series, Set<Instance>> mapSeries = tree.getEntityTreeMap().values().iterator().next().values().iterator().next();
                    DicomObject fwdIan = this.makeIAN(study, mapSeries, Availability.ONLINE);
                    forwardIocmModifiedObject(calledAETs, fwdIan);
                }
            } catch (Exception x) {
                log.error("Propagate changes of linking MPPS to MWL via IOCM failed!", x);
            }
        }
        if (this.forwardModifiedToAETs != null && fwdIANs != null) {
            for (DicomObject ian : fwdIANs) {
                this.scheduleForward(ian, forwardModifiedToAETs, 0);
            }
        }
        if (sendIANonMppsLinked) {
            if (movedStudies != null) {
                log.debug("Add IAN's for moved studies! # of IAN's before:"+fwdIANs.size());
                fwdIANs.addAll(getIANs(movedStudies, null));
            }
            log.info("Send IAN after linking MPPS to MWL! IANs:"+fwdIANs.size());
            try {
                this.sendIANs(fwdIANs);
            } catch (Exception x) {
                log.warn("Send IAN after linking MPPS to MWL failed!", x);
            }
        }
    }
    
    private void addMwlAttrs2Mpps(MppsToMwlLinkResult result) {
        try {
            boolean dcm14xsl = addMwlAttrsToMppsXsl.startsWith("14|");
            java.io.File f = FileUtils.toFile(dcm14xsl ? addMwlAttrsToMppsXsl.substring(3) : addMwlAttrsToMppsXsl);
            if (f.isFile()) {
                Templates tpl = templates.getTemplates(f);
                DicomObject coerce = new BasicDicomObject();
                Templates[] tpls = dcm14xsl ? new Templates[]{dcm2To14Tpl,tpl,dcm14To2Tpl} : new Templates[]{tpl};
                XSLTUtils.xslt(result.getMwl().getAttributes(), tpls, coerce, null);
                if (coerce.isEmpty()) {
                    log.warn("No coercion dataset from MWL via addMwlAttrsToMppsXsl:"+addMwlAttrsToMppsXsl);
                    log.warn("Please check stylesheet! (dcm4che14 vs. dcm4che2 xml format)");
                } else {
                    List<MPPS> mppss = result.getMppss();
                    DicomObject mppsAttrs;
                    for (MPPS mpps : mppss) {
                        mppsAttrs = mpps.getAttributes();
                        log.debug("MPPS attributes before addMwlAttrs2Mpps:{}", mppsAttrs);
                        log.debug("Coercion Attrs:{}", coerce);
                        CoercionUtil.coerceAttributes(mppsAttrs, coerce);
                        log.debug("MPPS attributes with MWL attributes:{}", mppsAttrs);
                        lookupMppsToMwlLinkLocal().updateMPPSAttributes(mpps, mppsAttrs);
                    }
                }
            } else {
                log.info("Can not add MWL attributes to MPPS Linked notification! addMwlAttrsToMppsXsl stylesheet file not found! file:"+f);
            }
        } catch (Exception e) {
            log.error("Attribute coercion failed! Can not add MWL attributes to MPPS Linked notification!", e);
        }
    }
    public boolean unlinkMpps(long pk) {
        return unlinkMppsByPks(new long[]{pk});
    }
    
    public boolean unlinkMppsByPks(long[] pks) {
        MppsToMwlLinkResult result = lookupMppsToMwlLinkLocal().unlinkMpps(pks, updateMwlStatus, modifyingSystem, modifyReason, useIOCM);
        for (MPPS mpps : result.getMppss()) {
            DicomObject mppsAttrs = mpps.getAttributes();
            DicomObject patAttrs = mpps.getPatient().getAttributes();
            StringBuilder sb = new StringBuilder();
            sb.append("Unlink MPPS iuid:").append(mppsAttrs.getString(Tag.SOPInstanceUID)).append(" from SPS ID(s): ");
            DicomElement ssaSQ = mppsAttrs.get(Tag.ScheduledStepAttributesSequence);
            for ( int i = 0, len = ssaSQ.countItems() ; i < len ; i++) {
                sb.append(ssaSQ.getDicomObject(i).getString(Tag.ScheduledProcedureStepID)).append(", ");
            }
            Auditlog.logProcedureRecord(AuditEvent.ActionCode.UPDATE, true, patAttrs, ssaSQ.getDicomObject().getString(Tag.StudyInstanceUID),
                    mpps.getAccessionNumber(), sb.substring(0,sb.length()-2));
            if (this.forwardModifiedToAETs != null) {
                this.scheduleForwardByMpps(mpps.getAttributes());
            }
        }
        if (result.getMppss().size() > 0) {
            if (useIOCM) {
                try {
                    EntityTree tree = result.getEntityTree();
                    processRejectionNotes(tree, true, "MPPS linked to worklist");
                    String calledAETs = (String) server.getAttribute(rejNoteServiceName, "CalledAETitles");
                    if (NONE.equals(calledAETs)) {
                        log.info("IOCM: No CalledAETitles configured in RecectionNote SCU service! Ignore forward.");
                    } else {
                        Study study = tree.getEntityTreeMap().values().iterator().next().keySet().iterator().next();
                        Map<Series, Set<Instance>> mapSeries = tree.getEntityTreeMap().values().iterator().next().values().iterator().next();
                        DicomObject fwdIan = this.makeIAN(study, mapSeries, Availability.ONLINE);
                        forwardIocmModifiedObject(calledAETs, fwdIan);
                    }
                } catch (Exception x) {
                    log.error("Propagate changes of linking MPPS to MWL via IOCM failed!", x);
                }
            }
            this.sendJMXNotification(result);
            return true;
        } else {
            return false;
        }
    }
    
    private void scheduleForwardByMpps(DicomObject mpps) {
        String patId = mpps.getString(Tag.PatientID);
        String studyIuid = mpps.getString(Tag.StudyInstanceUID);
        String seriesIuid, iuid;
        DicomElement mppsSeriesSq = mpps.get(Tag.PerformedSeriesSequence);
        if (mppsSeriesSq != null) {
            for (int i=0, len=mppsSeriesSq.countItems() ; i < len ; i++) {
                DicomObject mppsSeriesItem = mppsSeriesSq.getDicomObject(i);
                DicomElement mppsInstanceSq = mppsSeriesItem.get(Tag.ReferencedImageSequence);
                if (mppsInstanceSq.isEmpty()) 
                    mppsInstanceSq = mppsSeriesItem.get(Tag.ReferencedNonImageCompositeSOPInstanceSequence);
                if (mppsInstanceSq.isEmpty()) {
                    log.warn("Referenced series ("+mppsSeriesItem.getString(Tag.SeriesInstanceUID)+") in MPPS "
                            +mpps.getString(Tag.SOPInstanceUID)+" has no instance reference!");
                    continue;
                }
                seriesIuid = mppsSeriesItem.getString(Tag.SeriesInstanceUID);
                iuid = mppsInstanceSq.getDicomObject(0).getString(Tag.ReferencedSOPInstanceUID);
                scheduleForward(patId, studyIuid, seriesIuid, new String[]{iuid});
            }
        } else {
            log.warn("Forward of modified Object ignored! Reason: Missing PerformedSeriesSequence in MPPS "+mpps);
        }
    }

    public int removeForeignPpsInfo(long studyPk) {
        return this.lookupDicomEditLocal().removeForeignPpsInfo(studyPk);
    }
    
    private EntityTree updateSeriesAttributes(MppsToMwlLinkResult result) throws InstanceNotFoundException, MBeanException, ReflectionException {
        DicomObject coerce = getCoercionAttrs(result.getMwl().getAttributes());
        if ( coerce != null && !coerce.isEmpty()) {
            return this.lookupMppsToMwlLinkLocal().updateSeriesAndStudyAttributes(result, coerce, useIOCM);
        } else {
            log.warn("No Coercion attributes to update Study and Series Attributes after linking MPPS to MWL! coerce:"+coerce);
            return null;
        }
    }
    private DicomObject getCoercionAttrs(DicomObject ds) throws InstanceNotFoundException, MBeanException, ReflectionException {
        if ( ds == null ) return null;
        log.debug("Dataset to get coercion ds:{}",ds);
        DicomObject sps = ds.get(Tag.ScheduledProcedureStepSequence).getDicomObject();
        String aet = sps == null ? null : sps.getString(Tag.ScheduledStationAETitle);
        Templates tpl = templates.getTemplatesForAET(aet, MWL2STORE_XSL);
        log.info("found template for aet("+aet+"):"+tpl);
        if (tpl == null) {
            log.warn("Coercion template "+MWL2STORE_XSL+" not found! Can not store MWL attributes to series!");
            return null;
        }
        DicomObject out = new BasicDicomObject();
        try {
            log.info("dcm14Stylesheet:"+dcm14Stylesheet);
            if (dcm14Stylesheet) {
                Templates[] tpls = new Templates[]{dcm2To14Tpl,tpl,dcm14To2Tpl};
                XSLTUtils.xslt(ds, tpls, out, null);
            } else {
                XSLTUtils.xslt(ds, tpl, out, null);
            }
        } catch (Exception e) {
            log.error("Attribute coercion failed:", e);
            return null;
        }
        log.debug("return coerced attributes:{}",out);
        return out;
    }
    
    
    
    private DicomObject makeIAN(Study study, Map<Series, Set<Instance>> mapSeries, Availability availability) {
        log.debug("makeIAN: studyIUID:{}", study.getStudyInstanceUID());
        Patient pat = study.getPatient();
        DicomObject ian = new BasicDicomObject();
        ian.putString(Tag.StudyInstanceUID, VR.UI, study.getStudyInstanceUID());
        ian.putString(Tag.AccessionNumber, VR.SH, study.getAccessionNumber());
        ian.putString(Tag.PatientID, VR.LO, pat.getPatientID());
        ian.putString(Tag.IssuerOfPatientID, VR.LO, pat.getIssuerOfPatientID());
        ian.putString(Tag.PatientName, VR.PN, pat.getPatientName());
        DicomElement refPPSSeq = ian.putSequence(Tag.ReferencedPerformedProcedureStepSequence);
        HashSet<String> mppsuids = new HashSet<String>();
        DicomElement refSeriesSeq = ian.putSequence(Tag.ReferencedSeriesSequence);

        for (Map.Entry<Series, Set<Instance>> entry : mapSeries.entrySet() ) {
            Series sl = entry.getKey();
            MPPS mpps = sl.getModalityPerformedProcedureStep();
            if (mpps != null) {
                String mppsuid = mpps.getSopInstanceUID();
                if (mppsuids.add(mppsuid)) {
                    DicomObject refmpps = new BasicDicomObject();
                    refPPSSeq.addDicomObject(refmpps);
                    refmpps.putString(Tag.ReferencedSOPClassUID, VR.UI, UID.ModalityPerformedProcedureStepSOPClass);
                    refmpps.putString(Tag.ReferencedSOPInstanceUID, VR.UI, mppsuid);
                    refmpps.putSequence(Tag.PerformedWorkitemCodeSequence);
                }
            }
            DicomObject refSerItem = new BasicDicomObject();
            refSeriesSeq.addDicomObject(refSerItem);
            refSerItem.putString(Tag.SeriesInstanceUID, VR.UI, sl.getSeriesInstanceUID());
            DicomElement refSopSeq = refSerItem.putSequence(Tag.ReferencedSOPSequence);
            for (Instance instance : entry.getValue()) {
                DicomObject refSopItem = new BasicDicomObject();
                refSopSeq.addDicomObject(refSopItem);
                refSopItem.putString(Tag.RetrieveAETitle, VR.AE, instance.getRetrieveAETs());
                refSopItem.putString(Tag.InstanceAvailability, VR.CS, 
                        availability == null ? instance.getAvailability().name() : availability.name());
                refSopItem.putString(Tag.ReferencedSOPClassUID, VR.UI, instance.getSOPClassUID());
                refSopItem.putString(Tag.ReferencedSOPInstanceUID, VR.UI, instance.getSOPInstanceUID());
            }
        }
        log.debug("IAN:{}", ian);
        return ian;
    }
    
    private DicomEditLocal lookupDicomEditLocal() {
        if ( dicomEdit == null ) {
            try {
                InitialContext jndiCtx = new InitialContext();
                dicomEdit = (DicomEditLocal) jndiCtx.lookup(DicomEditLocal.JNDI_NAME);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return dicomEdit;
    }
    
    private MppsToMwlLinkLocal lookupMppsToMwlLinkLocal() {
        if ( mpps2mwl == null ) {
            try {
                InitialContext jndiCtx = new InitialContext();
                mpps2mwl = (MppsToMwlLinkLocal) jndiCtx.lookup(MppsToMwlLinkLocal.JNDI_NAME);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return mpps2mwl;
    }

    private void logPatientDeleted(EntityTree entityTree) {
        Set<Patient> pats = entityTree.getEntityTreeMap().keySet();
        for (Patient pat : pats) {
            Auditlog.logPatientRecord(AuditEvent.ActionCode.DELETE, true, pat.getPatientID(), pat.getPatientName());
        }
        for (MWLItem mwl : entityTree.getMwlItems()) {
            Auditlog.logProcedureRecord(AuditEvent.ActionCode.DELETE, true, mwl.getPatient().getAttributes(), 
                    mwl.getStudyInstanceUID(), mwl.getAccessionNumber(), null);
        }
    }

    public void logMppsLinkRecord(MppsToMwlLinkResult result ) {
        MWLItem mwl = result.getMwl();
        String accNr = mwl.getAccessionNumber();
        String spsId = mwl.getScheduledProcedureStepID();
        String studyIuid = mwl.getStudyInstanceUID();
        DicomObject patAttrs = mwl.getPatient().getAttributes();
        for ( MPPS mpps : result.getMppss()) {
            String desc = "MPPS "+mpps.getSopInstanceUID()+" linked with MWL entry "+spsId;
            Auditlog.logProcedureRecord(AuditEvent.ActionCode.UPDATE, true, patAttrs, studyIuid, accNr, desc);
        }
    }
    
    private DicomObject[] processStudyDeleted(EntityTree entityTree) throws InstanceNotFoundException, MBeanException, ReflectionException {
        if (entityTree.isEmpty())
            return null;
        DicomObject[] rejNotes = processRejectionNotes(entityTree, true, null);
        processIANs(entityTree, Availability.UNAVAILABLE);
        return rejNotes;
    }

    private DicomObject[] processInstancesDeleted(EntityTree entityTree, String auditDetail) throws InstanceNotFoundException, MBeanException, ReflectionException {
        if (entityTree.isEmpty())
            return null;
        DicomObject[] rejNotes = this.processRejectionNotes(entityTree, false, auditDetail);
        processIANs(entityTree, Availability.UNAVAILABLE);
        return rejNotes;
    }
    
    private DicomObject[] processRejectionNotes(EntityTree entityTree, boolean study, String auditDetails) 
                throws InstanceNotFoundException, MBeanException, ReflectionException {
        DicomObject[] rejNotes = iocm.getRejectionNotes(entityTree);
        for (DicomObject kos : rejNotes) {
            if (kos == null)
                continue;
            if (study) {
                Auditlog.logStudyDeleted(kos, true);
            } else {
                Auditlog.logInstancesAccessed(AuditEvent.ActionCode.DELETE, true, kos, true, auditDetails);
            }
            processRejectionNote(kos);
        }
        return rejNotes;
    }
    
    private void processRejectionNote(DicomObject rejNote) throws InstanceNotFoundException, MBeanException, ReflectionException {
        if (processRejNote) {
            log.debug("RejectionNote KOS:{}", rejNote);
            server.invoke(rejNoteServiceName, "scheduleRejectionNote", 
                    new Object[]{rejNote}, new String[]{DicomObject.class.getName()});
        }
    }

    private ArrayList<DicomObject> getIANs(EntityTree entityTree, Availability availability) throws InstanceNotFoundException, MBeanException, ReflectionException {
        ArrayList<DicomObject> ians = new ArrayList<DicomObject>();
        Map<Patient, Map<Study, Map<Series, Set<Instance>>>> entityTreeMap = entityTree.getEntityTreeMap();
        for (Map<Study, Map<Series, Set<Instance>>> studyMap : entityTreeMap.values()) {
            for ( Map.Entry<Study, Map<Series, Set<Instance>>> studyEntry: studyMap.entrySet()) {
                ians.add(makeIAN(studyEntry.getKey(), studyEntry.getValue(), availability));
            }
        }
        return ians;
    }
    
    private ArrayList<DicomObject> processIANs(EntityTree entityTree, Availability availability) throws InstanceNotFoundException, MBeanException, ReflectionException {
        if (processIAN) {
            ArrayList<DicomObject> ians = getIANs(entityTree, availability);
            sendIANs(ians);
            return ians;
        }
        return new ArrayList<DicomObject>();
    }

    private void sendIANs(Collection<DicomObject> ians) throws InstanceNotFoundException, MBeanException, ReflectionException{
        for (DicomObject ian : ians) {
            server.invoke(ianScuServiceName, "scheduleIAN", 
                    new Object[]{ian}, new String[]{DicomObject.class.getName()});
        }
    }

    public void sendJMXNotification(MppsToMwlLinkResult result) {
        log.debug("Send JMX Notification: {}", result);
        long eventID = super.getNextNotificationSequenceNumber();
        Notification notif = new Notification(MppsToMwlLinkResult.class.getName(), this,
                eventID);
        notif.setUserData(result);
        super.sendNotification(notif);
        if (notifyArchiveServicesAfterLinking) {
            eventID = super.getNextNotificationSequenceNumber();
            notif = new Notification(ARCHIVE_EVENT_TYPE, this,
                    eventID);
            notif.setUserData(result);
            try {
            	ByteArrayOutputStream baos = new ByteArrayOutputStream(65535);
            	for (MPPS mpps : result.getMppss()) {
            		server.invoke(archiveIANScuServicename, "notifyOtherServices", new Object[]{toDataset(mpps.getAttributes(), baos)}, 
            				new String[]{Dataset.class.getName()});
            	}
            } catch (Exception x) {
                log.error("Failed to notify archive services! archiveIANScuServicename:"+archiveIANScuServicename, x);
			}
        }
    }
    
    public static Dataset toDataset(DicomObject attrs, ByteArrayOutputStream baos) throws IOException {
    	baos.reset();
    	DicomOutputStream dos = null;
    	ByteArrayInputStream bais = null;
    	try {
	        dos = new DicomOutputStream(baos);
	        dos.writeDataset(attrs, TransferSyntax.ExplicitVRLittleEndian);
	        Dataset ds = DcmObjectFactory.getInstance().newDataset();
	        bais = new ByteArrayInputStream(baos.toByteArray());
	        ds.readDataset(bais, DcmDecodeParam.EVR_LE, -1);
	        return ds;
    	} finally {
	        if (dos != null) dos.close();
    		if (bais != null) bais.close();
    	}
    }

    
    private void sendDicomActionNotification(DicomObject obj, String action, String level) {
        DicomActionNotification notif = new DicomActionNotification(this, obj, action, level, getNextNotificationSequenceNumber());
        log.debug("Send JMX Notification:"+notif);
        super.sendNotification(notif);
    }
    
    public void doAfterDicomEdit(String patId, String patName, String[] studyIUIDs, DicomObject obj, String qrLevel) {
        sendDicomActionNotification(obj, DicomActionNotification.UPDATE, qrLevel);
        if (!"IMAGE".equals(qrLevel)) {
            try {
                server.invoke(qrServiceName, "clearCachedSeriesAttrs", 
                        new Object[]{}, new String[]{});
            } catch (Exception ignore) {
                log.info("Clear cached Series Attributes in QR service failed!");
            }
        }
        if ("PATIENT".equals(qrLevel)) {
            Auditlog.logPatientRecord(AuditEvent.ActionCode.UPDATE, true, patId, patName);
            if (enableForwardOnPatientUpdate && forwardModifiedToAETs != null) {
                if (studyIUIDs.length > 0) {
                    obj.putString(Tag.StudyInstanceUID, VR.UI, studyIUIDs[0]);
                    DicomObject fwdIan = lookupDicomEditLocal().getIanForForwardModifiedObject(obj, "STUDY");
                    scheduleForward(fwdIan, forwardModifiedToAETs, 0);
                } else {
                    log.info("Patient has no Study! Forward of modified patient ignored!");
                }
            }
       } else {
            Auditlog.logDicomObjectUpdated(true, patId, patName, studyIUIDs, obj, "Dicom Attributes updated on "+qrLevel+" level!");
            if ("STUDY".equals(qrLevel) || "SERIES".equals(qrLevel) || "IMAGE".equals(qrLevel)) {
                obj.putString(Tag.QueryRetrieveLevel, VR.CS, qrLevel);
                try {
                    server.invoke(attrModScuServiceName, "scheduleModification", 
                            new Object[]{obj}, new String[]{DicomObject.class.getName()});
                } catch (Exception e) {
                    log.error("Scheduling Attributes Modification Notification failed!", e);
                }
                if (forwardModifiedToAETs != null) {
                    DicomObject fwdIan = lookupDicomEditLocal().getIanForForwardModifiedObject(obj, qrLevel);
                    scheduleForward(fwdIan, forwardModifiedToAETs,0);
                }
            } else {
                log.debug("No further action after Dicom Edit defined for level "+qrLevel+"!");
            }
        }        
    }

    private void forwardIocmModifiedObject(String calledAETs, DicomObject fwdIan) {
        scheduleForward(fwdIan, StringUtils.split(calledAETs, '\\'), System.currentTimeMillis()+iocmForwardDelay);
    }

    private void scheduleForward(DicomObject fwdIan, String[] aets, long scheduleTime) {
        log.debug("fwdIan:{}", fwdIan);
        if (fwdIan == null) {
            log.warn("Forward of modified Object ignored! Reason: No ONLINE or NEARLINE instance found!");
        } else {
            for (int i = 0 ; i < aets.length ; i++) {
                try {
                    log.info("Scheduling forward of modified object to {}", aets[i]);
                    server.invoke(moveScuServiceName, "scheduleMoveInstances", 
                            new Object[]{fwdIan, aets[i], null, scheduleTime}, 
                            new String[]{DicomObject.class.getName(), STRING, Integer.class.getName(), 
                                long.class.getName()});
                } catch (Exception e) {
                    log.error("Scheduling forward of modified object to "+aets[i]+" failed!", e);
                }
            }
        }
    }
    private void scheduleForward(String patId, String studyIuid, String seriesIuid, String[] iuids) {
        for (int i = 0 ; i < forwardModifiedToAETs.length ; i++) {
            try {
                server.invoke(moveScuServiceName, "scheduleMoveInstances", 
                        new Object[]{patId, studyIuid, seriesIuid, iuids, null, forwardModifiedToAETs[i], null}, 
                        new String[]{STRING, STRING, STRING, String[].class.getName(), 
                            STRING, STRING, Integer.class.getName()});
            } catch (Exception e) {
                log.error("Scheduling forward of modified object to "+forwardModifiedToAETs[i]+" failed!", e);
            }
        }
    }

}

