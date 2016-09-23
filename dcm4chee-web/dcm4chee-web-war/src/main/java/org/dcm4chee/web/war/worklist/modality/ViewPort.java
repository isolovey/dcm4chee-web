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

package org.dcm4chee.web.war.worklist.modality;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.RequestCycle;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.dcm4chee.web.common.secure.SecureSession;
import org.dcm4chee.web.dao.worklist.modality.ModalityWorklistFilter;
import org.dcm4chee.web.war.common.AbstractViewPort;
import org.dcm4chee.web.war.folder.delegate.MwlScuDelegate;
import org.dcm4chee.web.war.worklist.modality.model.MWLItemModel;

/**
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision: 15662 $ $Date: 2011-07-08 09:55:11 +0200 (Fr, 08 Jul 2011) $
 * @since Apr. 20, 2010
 */
public class ViewPort extends AbstractViewPort {
    public static final String INTERNAL_WORKLISTPROVIDER = "<intern>";
    private static final long serialVersionUID = 1L;

    private ModalityWorklistFilter filter;
    private final List<MWLItemModel> mwlItemModels = new ArrayList<MWLItemModel>();
    
    final List<String> mwlProviders = new ArrayList<String>();

    private IModel<String> worklistProvider = new Model<String>(
            MwlScuDelegate.getInstance().getDefaultWorklistProvider());

    public ViewPort() {
        mwlProviders.add(ViewPort.INTERNAL_WORKLISTPROVIDER);
        mwlProviders.addAll(MwlScuDelegate.getInstance().getWorklistProviders());
    }
    
    public ModalityWorklistFilter getFilter() {
        if (filter == null) 
            filter = new ModalityWorklistFilter(((SecureSession) RequestCycle.get().getSession()).getUsername()); 
        return filter;
    }

    public List<MWLItemModel> getMWLItemModels() {
        return mwlItemModels;
    }
    
    public IModel<List<String>> getWorklistProviderListModel() {
        return new AbstractReadOnlyModel<List<String>>() {
            private static final long serialVersionUID = 1L;

            @Override
            public List<String> getObject() {
                return mwlProviders;
            }
            
        };
    }

    public IModel<String> getWorklistProviderModel() {
        return worklistProvider;
    }
    
    public String getWorklistProvider() {
        return worklistProvider.getObject();
    }
    
    public boolean isInternalWorklistProvider() {
        return INTERNAL_WORKLISTPROVIDER.equals(getWorklistProvider());
    }
    
    public void clear() {
        super.clear();
        filter.clear();
        mwlItemModels.clear();
        mwlProviders.clear();
        mwlProviders.add(ViewPort.INTERNAL_WORKLISTPROVIDER);
        mwlProviders.addAll(MwlScuDelegate.getInstance().getWorklistProviders());
        worklistProvider.setObject(MwlScuDelegate.getInstance().getDefaultWorklistProvider());
    }

}
