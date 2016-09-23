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
 * Portions created by the Initial Developer are Copyright (C) 2002-2005
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

package org.dcm4chee.dashboard.ui.common;

import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.extensions.markup.html.tree.table.IColumn;
import org.apache.wicket.extensions.markup.html.tree.table.TreeTable;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.Model;
import org.dcm4chee.icons.ImageManager;

/**
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision$ $Date$
 * @since 15.04.2010
 */
public abstract class DashboardTreeTable extends TreeTable {

    private static final long serialVersionUID = 1L;

    public DashboardTreeTable(String id, TreeModel model, IColumn[] columns) {
        super(id, model, columns);
        add(new AttributeModifier("class", true, new Model<String>("table")));
    }

    private class TreeFragment extends Fragment {

        private static final long serialVersionUID = 1L;

        public TreeFragment(String id, final TreeNode node, int level,
                final IRenderNodeCallback renderNodeCallback) {
            super(id, "fragment", DashboardTreeTable.this);

            add(newIndentation(this, "indent", node, level));
            add(newJunctionLink(this, "link", "image", node));
            add(newNodeLink(this, "nodeLink", node)
            .add(newNodeIcon(this, "icon", node))
            .add(new Label("label", new AbstractReadOnlyModel<Object>() {

                private static final long serialVersionUID = 1L;

                @Override
                public Object getObject() {
                    return renderNodeCallback.renderNode(node);
                }
            }))
            .setEnabled(false));
        }
    }

    @Override
    protected Component newTreePanel(MarkupContainer parent, String id, final TreeNode node, 
                                     int level, IRenderNodeCallback renderNodeCallback) {
        return new TreeFragment(id, node, level, renderNodeCallback);
    }

    @Override
    protected MarkupContainer newJunctionImage(MarkupContainer parent, final String id, final TreeNode node) {

        return (MarkupContainer)new WebMarkupContainer(id) {
            
            private static final long serialVersionUID = 1L;

            @Override
            protected void onComponentTag(ComponentTag tag) {
                
                super.onComponentTag(tag);

                RequestCycle.get().getResponse().write("<span class=\"" 
                        + ((node.getParent() == null ? 
                                true
                                : node.getParent().getChildAt(node.getParent().getChildCount() - 1).equals(node)) ? 
                                        "junction-last" 
                                        : "junction")
                        + "\"><span class=\"" +
                            (!node.isLeaf() ? "plus" : "corner") 
                                + "\""
                                +  (!node.isLeaf() ? 
                                " style=\"background-image: url('"
                                        + (isNodeExpanded(node) ? 
                                                getRequestCycle().urlFor(ImageManager.IMAGE_COMMON_COLLAPSE)
                                                : getRequestCycle().urlFor(ImageManager.IMAGE_COMMON_EXPAND)) 
                                                + "')\""
                                : "")
                        + "></span></span>");
            }
        }.setRenderBodyOnly(true);
    }
}
