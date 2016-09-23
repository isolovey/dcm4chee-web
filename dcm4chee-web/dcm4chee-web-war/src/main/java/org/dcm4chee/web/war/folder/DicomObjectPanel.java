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

import java.util.Iterator;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.ElementDictionary;
import org.dcm4che2.data.SpecificCharacterSet;
import org.dcm4che2.util.TagUtils;
import org.dcm4chee.web.war.common.model.AbstractDicomModel;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision: 15552 $ $Date: 2011-06-07 17:05:40 +0200 (Di, 07 Jun 2011) $
 * @since Jan 12, 2009
 */
public class DicomObjectPanel extends Panel {

    private static final long serialVersionUID = 1L;
    
    private static ElementDictionary dict = ElementDictionary.getDictionary();
    private boolean populated;
    private long modelPk = -1;
    
    public DicomObjectPanel(String id, AbstractDicomModel dcmModel, boolean header) {
        this(id, dcmModel.getDataset(), header);
        modelPk = dcmModel.getPk();
    }

    public DicomObjectPanel(String id, final DicomObject dcmObj, boolean header) {
        super(id);
        add(new Label("pkInfo", new AbstractReadOnlyModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return getString("pkInfo")+modelPk;
            }
            
        }){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return modelPk != -1;
            }
        });
        add(new WebMarkupContainer("header").setVisible(header));
        RepeatingView rv = new RepeatingView("elements") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onPopulate() {
                if (!populated) {
                    addDicomObject(this, dcmObj, "");
                    populated = true;
                }
            }
            
        };
        add(rv);
    }

    private void addDicomObject(RepeatingView rv, DicomObject dcmObj, String nesting) {
        final SpecificCharacterSet cs = dcmObj.getSpecificCharacterSet();
        final String nesting1 = nesting + '>';
        for (Iterator<DicomElement> it = dcmObj.iterator(); it.hasNext();) {
            WebMarkupContainer elrow = new WebMarkupContainer(rv.newChildId());
            rv.add(elrow);
            DicomElement el = it.next();
            elrow.add(new ElementFragment("fragment", el, cs, nesting));
            int numitems = el.countItems();
            for (int i = 0; i < numitems; i++) {
                WebMarkupContainer itemrow = new WebMarkupContainer(rv.newChildId());
                rv.add(itemrow);
                if (el.hasDicomObjects()) {
                    DicomObject item = el.getDicomObject(i);
                    itemrow.add(new ItemFragment("fragment", i, nesting1, null));
                    addDicomObject(rv, item, nesting1);
                } else {
                    byte[] ba = el.getFragment(i);
                    String data = null;
                    if (ba != null && ba.length > 0) {
                        if (ba.length > 16) {
                            byte[] ba1 = new byte[16];
                            System.arraycopy(ba, 0, ba1, 0, 16);
                            data = el.vr().toString(ba1, el.bigEndian(), null)+" ...";
                        } else {
                            data = el.vr().toString(ba, el.bigEndian(), null);
                        }
                    }
                    itemrow.add(new ItemFragment("fragment", i, nesting1, data));
                }
            }
        }
    }

    public class ElementFragment extends Fragment {

        private static final long serialVersionUID = 1L;

        public ElementFragment(String id, DicomElement el,
                SpecificCharacterSet cs, String nesting) {
            super(id, "element", DicomObjectPanel.this);
            int tag = el.tag();
            add(new Label("name", nesting + dict.nameOf(tag)));
            add(new Label("tag", TagUtils.toString(tag)));
            add(new Label("vr", el.vr().toString()));
            add(new Label("length", Integer.toString(el.length())));
            add(new Label("value",
                    el.hasItems() ? "" : el.getValueAsString(cs, 64)));
        }
     }

    public class ItemFragment extends Fragment {

        private static final long serialVersionUID = 1L;

        public ItemFragment(String id, int itemIndex, String nestingLevel, String data) {
            super(id, "item", DicomObjectPanel.this);
            if (data == null) {
                add(new Label("name", nestingLevel + "Item #" + (itemIndex + 1)));
            } else {
                add(new Label("name", nestingLevel + "Fragment #" + (itemIndex + 1)+": "+data));
            }
        }
     }
}
