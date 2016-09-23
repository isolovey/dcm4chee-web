package org.dcm4chee.web.war.tc;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.apache.wicket.protocol.http.WebApplication;
import org.dcm4che2.audit.message.ActiveParticipant;
import org.dcm4che2.audit.message.AuditEvent.ActionCode;
import org.dcm4che2.audit.message.AuditEvent.OutcomeIndicator;
import org.dcm4che2.audit.message.AuditMessage;
import org.dcm4che2.audit.message.AuditSource;
import org.dcm4che2.audit.message.InstancesAccessedMessage;
import org.dcm4che2.audit.message.ParticipantObject;
import org.dcm4che2.audit.message.ParticipantObject.DataLifeCycle;
import org.dcm4chee.web.common.util.Auditlog;
import org.dcm4chee.web.common.util.HttpUserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Two TF-related events are handled by this audit logger now:
 * 
 * 1) TF EDITED:
 * Whenever a TF is edited/modified, a TF-specific <code>InstancesAccessedMessage<code> is
 * going to be send to the local audit repository (if configured).
 *    
 * The used InstancesAccessedMessage is characterized as follows:
 *    a) Event ID = 'Instances Accessed' (code='110103', 'DCM', 'DICOM Instances Accessed')
 *    b) Event Action = 'Update' (id='U')
 *    c) Event Outcome = 'Success' (id=0)
 *    d) Audit Source
 *    	 d.1) Source-Id = Local host name
 *       d.2) Source Type = 'Application server' (id=4)
 *    e) Active Participant (User) 
 *    	 e.1) User-Id = user-id of the user who edited/modified the TF/images
 *    	 e.2) User-Name = user-id of the user who edited/modified the TF/images
 *       e.3) Requestor = true
 *    	 e.4) NAP-Id = the user's host name
 *    f) Active Participant 2 (Process, i.e Application Server)
 *       f.1) User-ID = Application server name and version
 *       f.2) User-Name = Application server name
 *       f.3) Requestor = false
 *       f.4) NAP-ID = Local host name
 *    f) Participant Object (one for each referenced study)
 *       f.1) Object-Type = 'System' (id=2)
 *       f.2) Object-Role = 'Report' (id=3)
 *       f.3) Object-ID Type = 'Study Instance UID' (id=12)
 *       f.4) Object-ID = study instance UID of the studies referenced in the TF,
 *    		  where one of these contains the TF SR itself
 *    
 * 2) TF VIEWED:
 * Actually, we need to distinguish 2 cases here:
 * 	  a) TF and it's textual content is viewed
 *    b) Images, which are referenced in the TF, are viewed
 * 
 * For each of both cases, a TF-specific <code>InstancesAccessedMessage<code> is sent.
 * The InstancesAccessedMessage is characterized as follows:
 *    a) Event ID = 'Instances Accessed' (code='110103', 'DCM', 'DICOM Instances Accessed')
 *    b) Event Action = 'Read' (id='R')
 *    c) Event Outcome = 'Success' (id=0)
 *    d) Audit Source
 *    	 d.1) Source-Id = Local host name
 *       d.2) Source Type = 'Application server' (id=4)
 *    e) Active Participant (User) 
 *    	 e.1) User-Id = user-id of the user who viewed the TF/images
 *    	 e.2) User-Name = user-id of the user who viewed the TF/images
 *       e.3) Requestor = true
 *    	 e.4) NAP-Id = user's host name
 *    f) Active Participant 2 (Process, i.e Application Server)
 *       f.1) User-ID = Application server name and version
 *       f.2) User-Name = Application server name
 *       f.3) Requestor = false
 *       f.4) NAP-ID = Local host name
 *    f) Participant Object (one for each referenced study)
 *       f.1) Object-Type = 'System' (id=2)
 *       f.2) Object-Role = 'Report' (id=3)
 *       f.3) Object-ID Type = 'Study Instance UID' (id=12)
 *       f.4) Object-ID = study instance UID of the studies referenced in the TF,
 *    		  where one of these contains the TF SR itself
 *    	 f.5) Data Life Cycle = 'ACCESSED' (id=6) (if just the TF and its textual information 
 *    	      is viewed without images)
 * 
 */
public class TCAuditLog {

    private static final Logger auditLog = LoggerFactory.getLogger("auditlog");
    private static final Logger log = LoggerFactory.getLogger(Auditlog.class);

    // the audit source
    private static final AuditSource auditSource = createAuditSource();
    
    // the 'user' active participant
    private static final ActiveParticipant userParticipant = createUserParticipant();
    
    // the 'process' active participant
    private static final ActiveParticipant processParticipant = createProcessParticipant();
    
	public static void logTFViewed(TCObject tc) {
        try {
            AuditMessage msg = createTFViewedMessage(tc, false);
            msg.validate();
            auditLog.info(msg.toString());
        } catch (Exception x) {
            log.warn("Audit Log 'Instances Accessed' (actionCode:R) failed:", x);
        }
	}
	
	public static void logTFImagesViewed(TCModel tc) {
        try {
            logTFImagesViewed(TCObject.create(tc));
        } catch (Exception x) {
            log.warn("Audit Log 'Instances Accessed' (actionCode:R) failed:", x);
        }
	}
		
	public static void logTFImagesViewed(TCObject tc) {
        try {
            AuditMessage msg = createTFViewedMessage(tc, true);
            msg.validate();
            auditLog.info(msg.toString());
        } catch (Exception x) {
            log.warn("Audit Log 'Instances Accessed' (actionCode:R) failed:", x);
        }
	}
		
	public static void logTFEdited(TCObject tc) {
        try {
            AuditMessage msg = createTFEditedMessage(tc);
            msg.validate();
            auditLog.info(msg.toString());
        } catch (Exception x) {
            log.warn("Audit Log 'Instances Accessed' (actionCode:U) failed:", x);
        }
	}	
	
	private static AuditSource createAuditSource()
	{
	    return AuditSource.getDefaultAuditSource();
	}
	
	private static ActiveParticipant createUserParticipant()
	{
		HttpUserInfo userInfo = new HttpUserInfo(AuditMessage.isEnableDNSLookups());
	    return ActiveParticipant.createActivePerson(
                userInfo.getUserId(),null,userInfo.getUserId(),userInfo.getHostName(),true);
	}
		
	private static ActiveParticipant createProcessParticipant()
	{
		String defaultName = AuditMessage.getProcessName();
		String defaultId = AuditMessage.getProcessID();
		
		String name = null;
		String id = null;
		
		try
		{
			WebApplication app = WebApplication.get();
			if (app!=null)
			{
				InputStream in = app.getServletContext().getResourceAsStream("/META-INF/MANIFEST.MF");
				if (in!=null)
				{
					Properties properties = new Properties();
					properties.load(in);
					name = properties.getProperty("Implementation-Title", defaultName);
					String version = properties.getProperty("Implementation-Build", defaultId);
					if (version!=null)
					{
						if (name!=null) {
							id = name + " (" + version + ")";
						}
						else {
							id = version;
						}
					}
				}
			}
		}
		catch (Exception e) {
		}
		
		if (name==null) {
			name = defaultName;
		}
		if (id==null) {
			id = defaultId;
		}

	    return ActiveParticipant.createActiveProcess(id, null, name,
                AuditMessage.getLocalHostName(), false);
	}
	
	private static AuditMessage createTFViewedMessage(TCObject tc, boolean withImages)
	{
		InstancesAccessedMessage msg = new InstancesAccessedMessage(ActionCode.READ);
		msg.addAuditSource(auditSource);
        msg.addActiveParticipant(userParticipant);
        msg.addActiveParticipant(processParticipant);
        msg.setOutcomeIndicator(OutcomeIndicator.SUCCESS);
        msg.addPatient(tc.getPatientId(), tc.getPatientName());
        List<TCReferencedStudy> studies = tc.getReferencedStudies();
		if (studies!=null) {
			for (TCReferencedStudy study : studies) {
				ParticipantObject o = msg.addStudy(study.getStudyUID(), null);
				if (!withImages) {
					o.setParticipantObjectDataLifeCycle(DataLifeCycle.ACCESS);
				}
			}
		}
		return msg;
	}
		
	private static AuditMessage createTFEditedMessage(TCObject tc) {
        InstancesAccessedMessage msg = new InstancesAccessedMessage(InstancesAccessedMessage.UPDATE);
		msg.addAuditSource(auditSource);
        msg.addActiveParticipant(userParticipant);
        msg.addActiveParticipant(processParticipant);
        msg.setOutcomeIndicator(OutcomeIndicator.SUCCESS);
        msg.addPatient(tc.getPatientId(), tc.getPatientName());
        List<TCReferencedStudy> studies = tc.getReferencedStudies();
        if (studies!=null) {
            for (TCReferencedStudy study : studies) {
                msg.addStudy(study.getStudyUID(), null);
            }
        }
        return msg;
	}
}
