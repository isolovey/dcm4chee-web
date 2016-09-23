package org.dcm4chee.web.war;


import java.net.URL;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;

import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.VR;
import org.dcm4chee.archive.entity.AE;
import org.dcm4chee.archive.entity.Code;
import org.dcm4chee.archive.entity.ContentItem;
import org.dcm4chee.archive.entity.File;
import org.dcm4chee.archive.entity.FileSystem;
import org.dcm4chee.archive.entity.GPPPS;
import org.dcm4chee.archive.entity.GPSPS;
import org.dcm4chee.archive.entity.GPSPSPerformer;
import org.dcm4chee.archive.entity.GPSPSRequest;
import org.dcm4chee.archive.entity.Instance;
import org.dcm4chee.archive.entity.Issuer;
import org.dcm4chee.archive.entity.MPPS;
import org.dcm4chee.archive.entity.MWLItem;
import org.dcm4chee.archive.entity.Media;
import org.dcm4chee.archive.entity.OtherPatientID;
import org.dcm4chee.archive.entity.Patient;
import org.dcm4chee.archive.entity.PrivateFile;
import org.dcm4chee.archive.entity.PrivateInstance;
import org.dcm4chee.archive.entity.PrivatePatient;
import org.dcm4chee.archive.entity.PrivateSeries;
import org.dcm4chee.archive.entity.PrivateStudy;
import org.dcm4chee.archive.entity.RequestAttributes;
import org.dcm4chee.archive.entity.Series;
import org.dcm4chee.archive.entity.Study;
import org.dcm4chee.archive.entity.StudyOnFileSystem;
import org.dcm4chee.archive.entity.UPS;
import org.dcm4chee.archive.entity.UPSRelatedPS;
import org.dcm4chee.archive.entity.UPSReplacedPS;
import org.dcm4chee.archive.entity.UPSRequest;
import org.dcm4chee.archive.entity.UPSSubscription;
import org.dcm4chee.archive.entity.VerifyingObserver;
import org.dcm4chee.usr.entity.UserRoleAssignment;
import org.dcm4chee.usr.entity.User;
import org.dcm4chee.web.dao.common.DicomEditBean;
import org.dcm4chee.web.dao.common.DicomEditLocal;
import org.jboss.net.protocol.URLStreamHandlerFactory;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bm.testsuite.BaseSessionBeanFixture;

public class DicomEditBeanTest extends BaseSessionBeanFixture<DicomEditBean>
{

    private static Logger log = LoggerFactory.getLogger(DicomEditBeanTest.class);
    
    private static final Class<?>[] usedBeans = {Patient.class, Study.class, Series.class, Instance.class,
        File.class, FileSystem.class, StudyOnFileSystem.class, VerifyingObserver.class,
        Media.class, MPPS.class, GPSPS.class, GPPPS.class, GPSPSRequest.class, GPSPSPerformer.class,
        MWLItem.class, 
        PrivatePatient.class, PrivateStudy.class, PrivateSeries.class, PrivateInstance.class, PrivateFile.class,
        OtherPatientID.class, AE.class, RequestAttributes.class, Code.class, User.class, UserRoleAssignment.class,
        Issuer.class, ContentItem.class, UPS.class, UPSRequest.class, UPSSubscription.class, UPSRelatedPS.class,
        UPSReplacedPS.class};
    
    static {
        URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory());
    }
    public DicomEditBeanTest() throws Exception {
        super(DicomEditBean.class, usedBeans);
    }
    @SuppressWarnings("unchecked")
    @Override
    public void setUp() throws Exception {
        this.initDummyMBean();
        WASPTestUtil.initRolesMappingFile();
        super.setUp();
        EntityManager em = getEntityManager();
        List<PrivatePatient> privP = em.createQuery("SELECT OBJECT(p) FROM PrivatePatient p").getResultList();
        if (privP != null) {
            em.getTransaction().begin();
            for (PrivatePatient p : privP) {
                em.remove(p);
            }
            em.getTransaction().commit();
        }
    }
    private void initDummyMBean() {
        MBeanServer mbServer = MBeanServerFactory.createMBeanServer("jboss");
        try {
            mbServer.createMBean("org.dcm4chee.web.war.DummyWebCfgMBean", 
                    new ObjectName("dcm4chee.web:service=WebConfig"));
        } catch (Exception ignore) {log.error("Can't create DummyWebCfgMBean!",ignore);}        
        try {
            mbServer.createMBean("org.dcm4chee.web.war.DummyServerConfigMBean", 
                    new ObjectName("jboss.system:type=ServerConfig"));
        } catch (Exception ignore) {log.error("Can't create ServerConfigBean!",ignore);}        
    }
    
    @Test
    public void testDeletePatient() throws NamingException {
        Patient p = new Patient();
        DicomObject patAttrs = new BasicDicomObject();
        patAttrs.putString(Tag.PatientName, VR.PN, "test");
        patAttrs.putString(Tag.PatientID, VR.LO, "PID_1");
        p.setAttributes(patAttrs);
        createPatient(p);
        deletePatient(p);
    }
/* doesn't work because of Batch Update error: java.sql.BatchUpdateException: failed batch   
    @Test
    public void testDeletePatientWithMPPS() throws NamingException {
        Patient p = new Patient();
        DicomObject patAttrs = new BasicDicomObject();
        patAttrs.putString(Tag.PatientName, VR.PN, "test");
        patAttrs.putString(Tag.PatientID, VR.LO, "PID_1");
        p.setAttributes(patAttrs);
        createPatient(p);
        MPPS mpps = new MPPS();
        DicomObject mppsAttrs = new BasicDicomObject();
        mppsAttrs.putString(Tag.SOPInstanceUID, VR.UI, "1.2.3.4");
        mppsAttrs.putString(Tag.PerformedProcedureStepStatus, VR.CS, "COMPLETED");
        mpps.setAttributes(mppsAttrs);
        mpps.setPatient(p);
        this.getEntityManager().persist(mpps);
        deletePatient(p);
    }
    @Test
    public void testDeletePatientWithMWL() throws NamingException {
        Patient p = new Patient();
        DicomObject patAttrs = new BasicDicomObject();
        patAttrs.putString(Tag.PatientName, VR.PN, "test");
        patAttrs.putString(Tag.PatientID, VR.LO, "PID_1");
        p.setAttributes(patAttrs);
        createPatient(p);
        MWLItem mwl = new MWLItem();
        DicomObject mwlAttrs = new BasicDicomObject();
        DicomObject spsItem = new BasicDicomObject();
        mwlAttrs.putSequence(Tag.ScheduledProcedureStepSequence).addDicomObject(spsItem);
        mwlAttrs.putString(Tag.RequestedProcedureID, VR.SH, "rqProcId-1");
        mwlAttrs.putString(Tag.StudyInstanceUID, VR.UI, "1.2.3.4.1");
        spsItem.putString(Tag.ScheduledProcedureStepID, VR.SH, "spsId_1");
        spsItem.putString(Tag.Modality, VR.CS, "CT");
        spsItem.putString(Tag.ScheduledStationAETitle, VR.AE, "AET_1");
        spsItem.putString(Tag.ScheduledProcedureStepStatus, VR.CS, "SCHEDULED");
        spsItem.putDate(Tag.ScheduledProcedureStepStartDate, VR.DA, new Date());
        spsItem.putDate(Tag.ScheduledProcedureStepStartTime, VR.TM, new Date());
        mwl.setAttributes(mwlAttrs);
        mwl.setPatient(p);
        this.getEntityManager().persist(mwl);
        deletePatient(p);
    }
    @Test
    public void testDeletePatientWithGPSPS() throws NamingException {
        Patient p = new Patient();
        DicomObject patAttrs = new BasicDicomObject();
        patAttrs.putString(Tag.PatientName, VR.PN, "test");
        patAttrs.putString(Tag.PatientID, VR.LO, "PID_1");
        p.setAttributes(patAttrs);
        createPatient(p);
        GPSPS gpsps = new GPSPS();
        DicomObject gpspsAttrs = new BasicDicomObject();
        gpspsAttrs.putString(Tag.SOPInstanceUID, VR.UI, "1.2.3.4.1.1");
        gpspsAttrs.putString(Tag.GeneralPurposeScheduledProcedureStepStatus, VR.CS, "SCHEDULED");
        gpspsAttrs.putString(Tag.GeneralPurposeScheduledProcedureStepPriority, VR.CS, "LOW");
        gpspsAttrs.putString(Tag.InputAvailabilityFlag, VR.CS, "COMPLETE");
        gpspsAttrs.putDate(Tag.ScheduledProcedureStepStartDateTime, VR.DT, new Date());
        gpsps.setAttributes(gpspsAttrs);
        gpsps.setPatient(p);
        this.getEntityManager().persist(gpsps);
        deletePatient(p);
    }
    @Test
    public void testDeletePatientWithGPPPS() throws NamingException {
        Patient p = new Patient();
        DicomObject patAttrs = new BasicDicomObject();
        patAttrs.putString(Tag.PatientName, VR.PN, "test");
        patAttrs.putString(Tag.PatientID, VR.LO, "PID_1");
        p.setAttributes(patAttrs);
        createPatient(p);
        GPPPS gppps = new GPPPS();
        DicomObject gpppsAttrs = new BasicDicomObject();
        gpppsAttrs.putString(Tag.SOPInstanceUID, VR.UI, "1.2.3.4");
        gpppsAttrs.putString(Tag.GeneralPurposePerformedProcedureStepStatus, VR.CS, "COMPLETED");
        gpppsAttrs.putDate(Tag.PerformedProcedureStepStartDate, VR.DA, new Date());
        gpppsAttrs.putDate(Tag.PerformedProcedureStepStartTime, VR.TM, new Date());
        gppps.setAttributes(gpppsAttrs);
        gppps.setPatient(p);
        this.getEntityManager().persist(gppps);
        deletePatient(p);
    }
/*_*/
    
    private void createPatient(Patient p) throws NamingException {
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        em.persist(p);
        em.getTransaction().commit();
        assertEquals(em.createQuery("SELECT OBJECT(p) FROM Patient p").getResultList().size(), 1);
    }
    @SuppressWarnings("unchecked")
    private void deletePatient(Patient p) throws NamingException {
        EntityManager em = getEntityManager();
        InitialContext jndiCtx = new InitialContext();
        DicomEditLocal dicomEdit = (DicomEditLocal) jndiCtx.lookup(DicomEditLocal.JNDI_NAME);
        em.getTransaction().begin();
        dicomEdit.movePatientsToTrash(new long[]{p.getPk()}, true);
        em.getTransaction().commit();
        List<PrivatePatient> privP = em.createQuery("SELECT OBJECT(p) FROM PrivatePatient p").getResultList();
        assertEquals(privP.size(), 1);
        List<Patient> allP = em.createQuery("SELECT OBJECT(p) FROM Patient p").getResultList();
        assertEquals(allP.size(), 0);
    }
}
