package org.dcm4chee.web.war.tc;

import org.dcm4chee.web.dao.tc.TCQueryFilterKey;
import org.dcm4chee.web.war.config.delegate.WebCfgDelegate;

public enum TCAttribute {
	Discussion("discussion"),
	Documents("documents"),
	Links("links"),
	Images("images"),
    Abstract(TCQueryFilterKey.Abstract, "abstract"),
    AcquisitionModality(TCQueryFilterKey.AcquisitionModality, "modalities"),
    Anatomy(TCQueryFilterKey.Anatomy, "anatomy"),
    AuthorAffiliation(TCQueryFilterKey.AuthorAffiliation, "author.affiliation"),
    AuthorContact(TCQueryFilterKey.AuthorContact, "author.contact"),
    AuthorName(TCQueryFilterKey.AuthorName, "author.name"),
    Category(TCQueryFilterKey.Category, "category"),
    Diagnosis(TCQueryFilterKey.Diagnosis, "diagnosis"),
    DiagnosisConfirmed(TCQueryFilterKey.DiagnosisConfirmed, "diagnosis.confirmed"),
    DifferentialDiagnosis(TCQueryFilterKey.DifferentialDiagnosis, "diffdiagnosis"),
    Remarks(TCQueryFilterKey.Discussion, "remarks"),
    Finding(TCQueryFilterKey.Finding, "finding"),
    History(TCQueryFilterKey.History, "history"),
    Keyword(TCQueryFilterKey.Keyword, "keyword"),
    Level(TCQueryFilterKey.Level, "level"),
    OrganSystem(TCQueryFilterKey.OrganSystem, "organsystem"),
    Pathology(TCQueryFilterKey.Pathology, "pathology"),
    PatientAge(TCQueryFilterKey.PatientAge, "patient.age"),
    PatientSex(TCQueryFilterKey.PatientSex, "patient.sex"),
    PatientSpecies(TCQueryFilterKey.PatientSpecies, "patient.species"),
    BibliographicReference(TCQueryFilterKey.BibliographicReference, "bibliography"),
    Title(TCQueryFilterKey.Title, "title"),
    CreationDate(TCQueryFilterKey.CreationDate, "content.date");
    
	private String namePropertyKey;
	private TCQueryFilterKey queryKey;
	
	private TCAttribute(String namePropertyKey) {
		this(null, namePropertyKey);
	}
	
	private TCAttribute(TCQueryFilterKey queryKey, String namePropertyKey) {
		this.queryKey = queryKey;
		this.namePropertyKey = namePropertyKey;
	}
	
	public TCQueryFilterKey getQueryKey() {
		return queryKey;
	}
	
	public String toLocalizedString() {
		return toLocalizedString(false);
	}
	
	public String toLocalizedString(boolean appendColon) {
		String value = TCUtilities.getLocalizedString(new StringBuilder("tc.")
			.append(namePropertyKey).append(".text").toString());
		if (appendColon) {
			value += ":";
		}
		return value;
	}
	
	public boolean isRestricted() {
		return WebCfgDelegate.getInstance()
				.isTCRestrictedAttribute(this);
	}
}
