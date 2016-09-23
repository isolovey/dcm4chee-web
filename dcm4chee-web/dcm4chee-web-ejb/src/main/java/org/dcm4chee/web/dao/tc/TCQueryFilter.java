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
package org.dcm4chee.web.dao.tc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.dcm4chee.web.dao.tc.TCQueryFilterValue.AcquisitionModality;
import org.dcm4chee.web.dao.tc.TCQueryFilterValue.Category;
import org.dcm4chee.web.dao.tc.TCQueryFilterValue.Level;
import org.dcm4chee.web.dao.tc.TCQueryFilterValue.PatientSex;
import org.dcm4chee.web.dao.tc.TCQueryFilterValue.YesNo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Bernhard Ableitinger <bernhard.ableitinger@agfa.com>
 * @version $Revision$ $Date$
 * @since May 05, 2011
 */
public class TCQueryFilter implements Serializable {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(TCQueryFilter.class);

    private Map<TCQueryFilterKey, TCQueryFilterValue<?>> values;

    public void clear() {
        if (values != null) {
            values.clear();
            values = null;
        }
    }

    public Set<Entry<TCQueryFilterKey, TCQueryFilterValue<?>>> getEntries() {
        return values != null ? values.entrySet() : null;
    }

    public String getAbstract() {
        return (String) getValue(TCQueryFilterKey.Abstract);
    }

    public void setAbstract(String abstr) {
        putValue(TCQueryFilterKey.Abstract, abstr);
    }

    public AcquisitionModality getAcquisitionModality() {
        return (AcquisitionModality) getValue(TCQueryFilterKey.AcquisitionModality);
    }

    public void setAcquisitionModality(AcquisitionModality acquisitionModality) {
        putValue(
                TCQueryFilterKey.AcquisitionModality,
                acquisitionModality != null ? 
                		acquisitionModality.createFilterValue() : null);
    }

    public String getAuthorAffiliation() {
        return (String) getValue(TCQueryFilterKey.AuthorAffiliation);
    }

    public void setAuthorAffiliation(String authorAffiliation) {
        putValue(TCQueryFilterKey.AuthorAffiliation, authorAffiliation);
    }

    public String getAuthorContact() {
        return (String) getValue(TCQueryFilterKey.AuthorContact);
    }

    public void setAuthorContact(String authorContact) {
        putValue(TCQueryFilterKey.AuthorContact, authorContact);
    }

    public String getAuthorName() {
        return (String) getValue(TCQueryFilterKey.AuthorName);
    }

    public void setAuthorName(String authorName) {
        putValue(TCQueryFilterKey.AuthorName, authorName);
    }

    public Category getCategory() {
        return (Category) getValue(TCQueryFilterKey.Category);
    }

    public void setCategory(Category category) {
        putValue(TCQueryFilterKey.Category,
                category != null ? TCQueryFilterValue.create(category) : null);
    }

    public YesNo getDiagnosisConfirmed() {
        return (YesNo) getValue(TCQueryFilterKey.DiagnosisConfirmed);
    }

    public void setDiagnosisConfirmed(YesNo diagnosisConfirmed) {
        putValue(
                TCQueryFilterKey.DiagnosisConfirmed,
                diagnosisConfirmed != null ? TCQueryFilterValue
                        .create(diagnosisConfirmed) : null);
    }
    
    public Date getCreationDateFrom() {
    	Object[] dates = getValues(TCQueryFilterKey.CreationDate);
    	if (dates!=null && dates.length>0) {
    		return (Date) dates[0];
    	}
    	return null;
    }
    
    public Date getCreationDateUntil() {
    	Object[] dates = getValues(TCQueryFilterKey.CreationDate);
    	if (dates!=null && dates.length>1) {
    		return (Date) dates[1];
    	}
    	return null;
    }
    
    public void setCreationDate(Date fromDate, Date untilDate) {
    	TCQueryFilterValue<Date> value = null;
    	if (fromDate!=null || untilDate!=null) {
    		value = TCQueryFilterValue.create(fromDate, untilDate);
    	}
    	putValue(TCQueryFilterKey.CreationDate, value);
    }

    public String getDiscussion() {
        return (String) getValue(TCQueryFilterKey.Discussion);
    }

    public void setDiscussion(String discussion) {
        putValue(TCQueryFilterKey.Discussion, discussion);
    }

    public String getHistory() {
        return (String) getValue(TCQueryFilterKey.History);
    }

    public void setHistory(String history) {
        putValue(TCQueryFilterKey.History, history);
    }

    public Level getLevel() {
        return (Level) getValue(TCQueryFilterKey.Level);
    }

    public void setLevel(Level level) {
        putValue(TCQueryFilterKey.Level,
                level != null ? TCQueryFilterValue.create(level) : null);
    }

    public PatientSex getPatientSex() {
        return (PatientSex) getValue(TCQueryFilterKey.PatientSex);
    }

    public void setPatientSex(PatientSex patientSex) {
        putValue(TCQueryFilterKey.PatientSex,
                patientSex != null ? TCQueryFilterValue.create(patientSex)
                        : null);
    }

    public String getPatientSpecies() {
        return (String) getValue(TCQueryFilterKey.PatientSpecies);
    }

    public void setPatientSpecies(String patientSpecies) {
        putValue(TCQueryFilterKey.PatientSpecies, patientSpecies);
    }

    public String getBibliographicReference() {
        return (String) getValue(TCQueryFilterKey.BibliographicReference);
    }

    public void setBibliographicReference(String bibliographicReference) {
        putValue(TCQueryFilterKey.BibliographicReference,
                bibliographicReference);
    }

    public String getTitle() {
        return (String) getValue(TCQueryFilterKey.Title);
    }

    public void setTitle(String title) {
        putValue(TCQueryFilterKey.Title, title);
    }

    public ITextOrCode getAnatomy() {
        try {
            return (ITextOrCode) getValue(TCQueryFilterKey.Anatomy);
        } catch (ClassCastException e) {
            log.warn(
                    "TC property 'anatomy' not of type Code. Returning null...",
                    e);

            return null;
        }
    }

    public void setAnatomy(ITextOrCode anatomy) {
        putValue(TCQueryFilterKey.Anatomy, anatomy);
    }

    public ITextOrCode getDiagnosis() {
        try {
            return (ITextOrCode) getValue(TCQueryFilterKey.Diagnosis);
        } catch (ClassCastException e) {
            log.warn(
                    "TC property 'diagnosis' not of type Code. Returning null...",
                    e);

            return null;
        }
    }

    public void setDiagnosis(ITextOrCode diagnosis) {
        putValue(TCQueryFilterKey.Diagnosis, diagnosis);
    }

    public ITextOrCode getDiffDiagnosis() {
        try {
            return (ITextOrCode) getValue(TCQueryFilterKey.DifferentialDiagnosis);
        } catch (ClassCastException e) {
            log.warn(
                    "TC property 'diff-diagnosis' not of type Code. Returning null...",
                    e);

            return null;
        }
    }

    public void setDiffDiagnosis(ITextOrCode diffDiagnosis) {
        putValue(TCQueryFilterKey.DifferentialDiagnosis, diffDiagnosis);
    }

    public ITextOrCode getFinding() {
        try {
            return (ITextOrCode) getValue(TCQueryFilterKey.Finding);
        } catch (ClassCastException e) {
            log.warn(
                    "TC property 'finding' not of type Code. Returning null...",
                    e);

            return null;
        }
    }

    public void setFinding(ITextOrCode finding) {
        putValue(TCQueryFilterKey.Finding, finding);
    }

    public ITextOrCode[] getKeywords() {
        try {
            return (ITextOrCode[]) getValues(TCQueryFilterKey.Keyword);
        } catch (ClassCastException e) {
            log.warn(
                    "TC property 'keyword' not of type Code. Returning null...",
                    e);

            return null;
        }
    }

    public void setKeywords(ITextOrCode...keywords) {
        putValue(TCQueryFilterKey.Keyword, keywords);
    }

    public ITextOrCode getOrganSystem() {
        try {
            return (ITextOrCode) getValue(TCQueryFilterKey.OrganSystem);
        } catch (ClassCastException e) {
            log.warn(
                    "TC property 'organ-system' not of type Code. Returning null...",
                    e);

            return null;
        }
    }

    public void setOrganSystem(ITextOrCode organSystem) {
        putValue(TCQueryFilterKey.OrganSystem, organSystem);
    }

    public ITextOrCode getPathology() {
        try {
            return (ITextOrCode) getValue(TCQueryFilterKey.Pathology);
        } catch (ClassCastException e) {
            log.warn(
                    "TC property 'pathology' not of type Code. Returning null...",
                    e);

            return null;
        }
    }

    public void setPathology(ITextOrCode pathology) {
        putValue(TCQueryFilterKey.Pathology, pathology);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TC search for ");

        if (values != null && !values.isEmpty()) {
            boolean first = true;
            sb.append(values.size() + " attributes (");
            for (Map.Entry<TCQueryFilterKey, TCQueryFilterValue<?>> me : values
                    .entrySet()) {
                if (!first) {
                    sb.append(", ");
                } else {
                    first = false;
                }

                sb.append(me.getKey().name());
                sb.append("=");
                sb.append("'" + me.getValue() + "'");
            }
            sb.append(")");
        } else {
            sb.append("0 attributes");
        }

        return sb.toString();
    }

    public Object getValue(TCQueryFilterKey key) {
        TCQueryFilterValue<?> value = values != null ? values.get(key) : null;

        return value != null ? value.getValue() : null;
    }
    
    public Object[] getValues(TCQueryFilterKey key) {
        TCQueryFilterValue<?> value = values != null ? values.get(key) : null;

        return value != null ? value.getValues() : null;
    }

    private void putValue(TCQueryFilterKey key, String value) {
        putValue(
                key,
                value != null && value.length() > 0 ? TCQueryFilterValue
                        .create(value) : null);
    }
    
    private void putValue(TCQueryFilterKey key, ITextOrCode...values)
    {
    	List<ITextOrCode> searchables = null;
    	
    	if (values!=null && values.length>0)
    	{
    		searchables = new ArrayList<ITextOrCode>();
    		for (ITextOrCode value : values)
    		{
    			if (value!=null)
    			{
    				String text = value.getText();
    				TCDicomCode code = value.getCode();
    				if ( (code!=null) || (text!=null && !text.isEmpty()))
    				{
    					searchables.add(value);
    				}
    			}
    		}
    	}
    	if (searchables==null || searchables.isEmpty())
    	{
    		putValue(key, (TCQueryFilterValue<?>) null);
    	}
    	else
    	{
    		putValue(key, TCQueryFilterValue.create(
    				searchables.toArray(new ITextOrCode[0])));
    	}
    }

    private void putValue(TCQueryFilterKey key, TCQueryFilterValue<?> value) {
        if (value == null) {
            if (values != null) {
                values.remove(key);

                if (values.isEmpty()) {
                    values = null;
                }
            }
        } else {
            if (values == null) {
                values = new HashMap<TCQueryFilterKey, TCQueryFilterValue<?>>(3);
            }

            values.put(key, value);
        }
    }

}
