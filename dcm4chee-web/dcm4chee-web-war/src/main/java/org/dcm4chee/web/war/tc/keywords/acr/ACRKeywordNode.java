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
package org.dcm4chee.web.war.tc.keywords.acr;

import org.dcm4chee.web.dao.tc.TCDicomCode;
import org.dcm4chee.web.war.tc.keywords.TCKeyword;
import org.dcm4chee.web.war.tc.keywords.TCKeywordNode;

/**
 * @author Bernhard Ableitinger <bernhard.ableitinger@agfa.com>
 * @version $Revision$ $Date$
 * @since May 30, 2011
 */
public class ACRKeywordNode extends TCKeywordNode {

    private static final long serialVersionUID = 1L;

    private String id;

    private ACRKeywordNode(String id, String name, TCDicomCode code) {
        super(new TCKeyword(name, code, false));

        this.id = id;
    }

    public static ACRKeywordNode create(String id, TCDicomCode code) {
        return new ACRKeywordNode(id, null, code);
    }

    public static ACRKeywordNode createRoot(String id) {
        return new ACRKeywordNode(id, id, null);
    }

    public String getId() {
        return id;
    }

    public boolean addToSubtree(ACRKeywordNode node) {
        String nodeId = node.getId();
        String parentId = nodeId.substring(0, nodeId.length() - 1);

        if (parentId.length() == 0 && id.startsWith("root-")) {
            parentId = id;
        }

        if (id.equalsIgnoreCase(parentId)) {
            addChildren(node);

            return true;
        } else if (getChildCount() > 0) {
            for (TCKeywordNode child : getChildren()) {
                if (child instanceof ACRKeywordNode) {
                    if (((ACRKeywordNode) child).addToSubtree(node)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public ACRKeywordNode findNode(TCKeyword keyword) {
        return keyword != null && keyword.getCode() != null ? findNodeByCode(keyword
                .getCode()) : null;
    }

    public ACRKeywordNode findNodeByCode(TCDicomCode code) {
        if (code != null) {
            TCDicomCode c = getKeyword().getCode();

            if (c != null && c.equals(code)
                    && c.getMeaning().equals(code.getMeaning())) {
                return this;
            }

            if (getChildCount() > 0) {
                for (TCKeywordNode child : getChildren()) {
                    ACRKeywordNode node = ((ACRKeywordNode) child)
                            .findNodeByCode(code);
                    if (node != null) {
                        return node;
                    }
                }
            }
        }

        return null;
    }

    public ACRKeywordNode findNodeByCodeValue(String value) {
        if (value != null) {
            TCDicomCode code = getKeyword().getCode();

            if (code != null) {
                if (value.equals(code.getValue())) {
                    return this;
                }
            }

            if (getChildCount() > 0) {
                for (TCKeywordNode child : getChildren()) {
                    ACRKeywordNode n = ((ACRKeywordNode) child)
                            .findNodeByCodeValue(value);
                    if (n != null) {
                        return n;
                    }
                }
            }
        }

        return null;
    }

}
