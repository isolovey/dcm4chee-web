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
package org.dcm4chee.web.war.tc.keywords;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.jfree.util.Log;

/**
 * @author Bernhard Ableitinger <bernhard.ableitinger@agfa.com>
 * @version $Revision$ $Date$
 * @since May 30, 2011
 */
public class TCKeywordNode implements TreeNode, Serializable {

    private static final long serialVersionUID = 1L;

    private TCKeyword keyword;

    private TCKeywordNode parent;

    private List<TCKeywordNode> children;

    /* ************* constructors **************** */

    public TCKeywordNode(TCKeyword keyword) {
        this.keyword = keyword;
    }

    public TCKeywordNode() {
        this(null);
    }

    /* *********** public accessible ************* */

    public TCKeyword getKeyword() {
        return keyword;
    }

    public void setKeyword(TCKeyword keyword) {
        this.keyword = keyword;
    }

    public List<TCKeywordNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public TCKeywordNode getRoot() {
        TCKeywordNode root = this;

        while (!root.isRoot()) {
            root = root.getParent();
        }

        return root;
    }

    public List<TCKeywordNode> getPath() {
        List<TCKeywordNode> path = new ArrayList<TCKeywordNode>(5);

        TCKeywordNode p = parent;

        while (p != null) {
            path.add(0, p);

            p = p.parent;
        }

        return path;
    }

    public TreePath getTreePath() {
        List<TCKeywordNode> path = getPath();
        path.add(this);

        return new TreePath(path.toArray(new TCKeywordNode[0]));
    }

    public void addChildren(TCKeywordNode node) {
        if (children == null) {
            children = new ArrayList<TCKeywordNode>();
        }

        if (!children.contains(node)) {
            node.parent = this;

            children.add(node);
        }
    }

    public void removeChildren(TCKeywordNode node) {
        if (children != null) {
            if (children.remove(node)) {
                node.parent = null;

                if (children.isEmpty()) {
                    children = null;
                }
            }
        }
    }

    public void removeAllChildren() {
        if (children != null) {
            for (TCKeywordNode child : children) {
                child.parent = null;
            }

            children.clear();

            children = null;
        }
    }

    public List<TCKeywordNode> toList() {
        List<TCKeywordNode> list = new ArrayList<TCKeywordNode>();

        addTree(list);

        return list;
    }

    public TCKeywordNode deepCopy() {
        TCKeywordNode copy = new TCKeywordNode(keyword);

        if (children != null) {
            for (TCKeywordNode child : children) {
                copy.addChildren(child.deepCopy());
            }
        }

        return copy;
    }

    @Override
    public String toString() {
        return keyword != null ? keyword.toString() : "Root"; //$NON-NLS-1$
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof TCKeywordNode) {
            TCKeywordNode node = (TCKeywordNode) o;

            return (node.keyword == null && keyword == null)
                    || (node.keyword != null && keyword != null && node.keyword
                            .equals(keyword));
        }

        return super.equals(o);
    }
    @Override
    public int hashCode() {
        return keyword == null ? super.hashCode() : keyword.hashCode();
    }

    /* ************** ITreeNode ******************* */

    public TCKeywordNode getChildAt(int childIndex) {
        return children != null ? children.get(childIndex) : null;
    }

    public int getChildCount() {
        return children != null ? children.size() : 0;
    }

    public TCKeywordNode getParent() {
        return parent;
    }

    public int getIndex(TreeNode node) {
        return children.indexOf(node);
    }

    public boolean getAllowsChildren() {
        return true;
    }

    public boolean isLeaf() {
        return getChildCount() <= 0;
    }

    public boolean isRoot() {
        return parent == null;
    }

    @SuppressWarnings("unchecked")
    public Enumeration<TCKeywordNode> children() {
        if (children != null) {
            return Collections.enumeration(children);
        }

        return Collections.enumeration(Collections.EMPTY_LIST);
    }

    /* ************** non-public accessible *********** */

    private void addTree(List<TCKeywordNode> nodes) {
        nodes.add(this);

        if (children != null) {
            for (TCKeywordNode child : children) {
                child.addTree(nodes);
            }
        }
    }

}
