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

import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.model.Model;
import org.dcm4chee.web.common.behaviours.TooltipBehaviour;
import org.dcm4chee.web.common.markup.DateTimeLabel;
import org.dcm4chee.web.war.worklist.modality.model.MWLItemModel;

/**
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision: 17039 $ $Date: 2012-08-31 15:53:11 +0200 (Fr, 31 Aug 2012) $
 * @since 20.04.2010
 */
public class MWLItemListView extends PropertyListView<MWLItemModel> {

    private static final long serialVersionUID = 1L;
    
    private MwlActionProvider mwlActionProvider;
    private WebMarkupContainer mwlitem;
    
    public MWLItemListView(String id, List<MWLItemModel> list) {
        super(id, list);
    }

    public MWLItemListView(String id, List<MWLItemModel> list, MwlActionProvider mwlActionProvider) {
        this(id, list);
        this.mwlActionProvider = mwlActionProvider;
    }
    protected String getOddEvenClass(ListItem<?> item) {
        return item.getIndex() % 2 == 0 ? "even" : "odd";
    }
    
    @Override
    protected void populateItem(final ListItem<MWLItemModel> item) {

        item.add(this.mwlitem = new WebMarkupContainer("mwlitem"));
        this.mwlitem.add(new AttributeModifier("class", true, new Model<String>(getOddEvenClass(item))));     

        TooltipBehaviour tooltip = new TooltipBehaviour("mw.content.data.");

        this.mwlitem.add(new Label("patientName").add(tooltip))
        .add(new DateTimeLabel("birthDate").setWithoutTime(true).add(tooltip))
        .add(new Label("SPSDescription").add(tooltip))
        .add(new Label("SPSModality").add(tooltip))
        .add(new DateTimeLabel("startDate").add(tooltip))
        .add(new Label("accessionNumber").add(tooltip))
        .add(new Label("stationAET").add(tooltip))
        .add(new Label("stationName").add(tooltip))
        .add(new Label("SPSStatus").add(tooltip));
        
        if (mwlActionProvider != null)
            mwlActionProvider.addMwlActions(item, mwlitem, MWLItemListView.this);
    }

    public interface MwlActionProvider {
        void addMwlActions(ListItem<MWLItemModel> item, WebMarkupContainer valueContainer, MWLItemListView mwlListView);
    }
}
