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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.wicket.RequestCycle;
import org.dcm4chee.web.common.secure.SecureSession;
import org.dcm4chee.web.dao.folder.StudyListFilter;
import org.dcm4chee.web.war.common.AbstractViewPort;
import org.dcm4chee.web.war.config.delegate.WebCfgDelegate;
import org.dcm4chee.web.war.folder.model.PatientModel;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision: 17857 $ $Date: 2013-06-04 15:21:41 +0200 (Di, 04 Jun 2013) $
 * @since Jan 14, 2009
 */
public class ViewPort extends AbstractViewPort {

    private static final long serialVersionUID = 1L;
    
    private StudyListFilter filter;
    private List<PatientModel> patients = new ArrayList<PatientModel>();
	private boolean resetOnSearch;
    
    public StudyListFilter getFilter() {
        if (filter == null) {
            filter = new StudyListFilter(((SecureSession) RequestCycle.get().getSession()).getUsername());
            Date[] studyDatePreset = WebCfgDelegate.getInstance().getPresetStudyDateRange();
            if (studyDatePreset != null) {
                filter.setStudyDateMin(studyDatePreset[0]);
                filter.setStudyDateMax(studyDatePreset[1]);
            }
            filter.setModalityFilter(WebCfgDelegate.getInstance().getModalityFilterList());
        }
        return filter;
    }

    public List<PatientModel> getPatients() {
        return patients;
    }
    
    public void clear() {
        super.clear();
        filter.clear();
        patients.clear();
    }

	public boolean resetOnSearch() {
		return resetOnSearch;
	}
	
	public void setResetOnSearch(boolean resetOnSearch) {
		this.resetOnSearch = resetOnSearch;
	}

}
