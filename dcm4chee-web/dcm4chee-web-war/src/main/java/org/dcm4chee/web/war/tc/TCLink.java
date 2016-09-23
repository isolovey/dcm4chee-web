package org.dcm4chee.web.war.tc;

import java.io.Serializable;
import java.util.List;

import org.apache.log4j.Logger;
import org.dcm4chee.archive.entity.Instance;
import org.dcm4chee.archive.util.JNDIUtils;
import org.dcm4chee.web.dao.tc.TCQueryLocal;
import org.dcm4chee.web.war.StudyPermissionHelper;

@SuppressWarnings("serial")
public class TCLink implements Serializable {

	private static final Logger log = Logger.getLogger(TCLink.class);

	private String caseUID;
	private String linkedCaseUID;
	private TCLinkRelationship linkRelationship;
	private String linkComment;
	
	private transient Boolean permitted;
	private transient TCModel linkedCaseModel;
	private transient TCEditableObject linkedCase;
	private transient boolean linkedCaseModelFetched;

	
	public TCLink(String caseUID, String linkedCaseUID, TCLinkRelationship linkRelationship, String linkComment)	{
		this.caseUID = caseUID;
		this.linkedCaseUID = linkedCaseUID;
		this.linkedCaseModelFetched = linkedCase!=null ? true : false;
		this.linkRelationship = linkRelationship;
		this.linkComment = linkComment;
	}
	
	public static TCLink createCrossLink(TCLink link) {
		return new TCLink(link.getLinkedCaseUID(), link.getCaseUID(), 
				link.getLinkRelationship().twin(), link.getLinkComment());
	}
	
	public String getCaseUID() {
		return caseUID;
	}

	public String getLinkedCaseUID() {
		return linkedCaseUID;
	}
	
	public TCLinkRelationship getLinkRelationship() {
		return linkRelationship;
	}
	
	public String getLinkComment() {
		return linkComment;
	}
	
	public synchronized boolean isPermitted() {
		if (permitted==null) {
			StudyPermissionHelper permissionHelper = StudyPermissionHelper.get();
			boolean permissionsEnabled = permissionHelper.applyStudyPermissions();
			
			try
			{
				if (permissionsEnabled) {
			        TCQueryLocal dao = (TCQueryLocal) JNDIUtils.lookup(TCQueryLocal.JNDI_NAME);
			        Instance i = dao.findInstanceByUID(linkedCaseUID, permissionHelper.getDicomRoles());
			        if (i==null) {
			        	permitted = false;
			        }
				}
				
				if (permitted==null) {
					permitted = true;
				}
			}
			catch (Exception e) {
				log.error("Unable to create linked case", e);
				permitted = !permissionsEnabled;
			}
		}
		return permitted;
	}
	
	public synchronized TCModel getLinkedCaseModel() {
		if (!linkedCaseModelFetched && linkedCaseModel==null) {
			try
			{
		        TCQueryLocal dao = (TCQueryLocal) JNDIUtils
		                .lookup(TCQueryLocal.JNDI_NAME);
	                    
		        Instance i = dao.findInstanceByUID(linkedCaseUID, null);
		        if (i==null) {
		        	log.warn("Teaching-File crosslink is unresolved: Perhaps the linked case has been deleted meanwhile!");
		        }
		        else {
		        	linkedCaseModel = new TCModel(i);
		        }
			}
			catch (Exception e) {
				log.error("Unable to create linked case", e);
			}
			finally {
				linkedCaseModelFetched = true;
			}
		}
		return linkedCaseModel;
	}
	
	public synchronized TCEditableObject getLinkedCase() {
		if (linkedCase==null) {
			try {
				TCModel tcModel = getLinkedCaseModel();
				if (tcModel!=null) {
					linkedCase = TCEditableObject.create(tcModel);
				}
			}
			catch (Exception e) {
				log.error(null, e);
			}
		}
		return linkedCase;
	}
	
	public synchronized TCLink findCrossLink() {
		TCEditableObject linkedCase = getLinkedCase();
		if (linkedCase!=null) {
			List<TCLink> crosslinks = linkedCase.getLinks();
			if (crosslinks!=null) {
				TCLink template = createCrossLink(this);
				for (TCLink link : crosslinks) {
					if (link.equals(template)) {
						return link;
					}
				}
			}
		}
		return null;
	}
			
	@Override
	public int hashCode() {
		return (caseUID+linkedCaseUID+linkRelationship.name()).hashCode();
	}
		
	@Override
	public boolean equals(Object o) {
		if (o instanceof TCLink) {
			return caseUID.equals(((TCLink)o).caseUID) &&
					linkedCaseUID.equals(((TCLink)o).linkedCaseUID) &&
					linkRelationship.equals(((TCLink)o).linkRelationship);
		}
		return super.equals(o);
	}
	
	public static enum TCLinkRelationship implements Serializable {
		RELATES_TO, SIMILAR, POSTERIOR, ANTERIOR;
		
		public TCLinkRelationship twin() {
			if (this.equals(RELATES_TO)) {
				return RELATES_TO;
			}
			else if (this.equals(SIMILAR)) {
				return SIMILAR;
			}
			else if (this.equals(ANTERIOR)) {
				return POSTERIOR;
			}
			else if (this.equals(POSTERIOR)) {
				return ANTERIOR;
			}
			return RELATES_TO;
		}
		
		@Override
		public String toString() {
			return TCUtilities.getLocalizedString(
					"tc.links.relationship."+name().toLowerCase()+".text");
		}
		
		public static TCLinkRelationship valueOfLocalized(String s) {
			for (TCLinkRelationship r : values()) {
				if (s.equals(r.toString())) {
					return r;
				}
			}
			return null;
		}
	}
	
}
