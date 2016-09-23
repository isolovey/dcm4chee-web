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

import java.util.Arrays;

import org.dcm4chee.web.dao.tc.TCDicomCode;
import org.dcm4chee.web.dao.tc.TCQueryFilterKey;
import org.dcm4chee.web.war.tc.keywords.TCKeyword;
import org.dcm4chee.web.war.tc.keywords.TCKeywordCatalogue;

/**
 * @author Bernhard Ableitinger <bernhard.ableitinger@agfa.com>
 * @version $Revision$ $Date$
 * @since May 30, 2011
 */
public class ACRCatalogue extends TCKeywordCatalogue {

    private static ACRCatalogue instance;

    private ACRKeywordNode anatomyRoot;

    private ACRKeywordNode[] pathologyRoots;

    private boolean anatomyRootCreated;

    private boolean pathologyRootsCreated;

    private ACRCatalogue() {
        super("ACR", "ACR","American College of Radiology", null);
    }

    public static synchronized ACRCatalogue getInstance() {
        if (instance == null) {
            instance = new ACRCatalogue();
        }

        return instance;
    }

    public ACRKeywordNode getAnatomyRoot() {
        if (!anatomyRootCreated) {
            anatomyRoot = createAnatomyRoot();
        }

        return anatomyRoot;
    }

    public ACRKeywordNode[] getPathologyRoots() {
        if (!pathologyRootsCreated) {
            pathologyRoots = createPathologyRoots();
        }

        return pathologyRoots;
    }

    public ACRKeywordNode getPathologyRoot(ACRKeywordNode anatomyNode) {
        if (!pathologyRootsCreated) {
            pathologyRoots = createPathologyRoots();
        }

        if (anatomyNode != null) {
            int index = Integer.parseInt(anatomyNode.getId().substring(0, 1));

            if (index >= 0 && pathologyRoots != null
                    && index < pathologyRoots.length) {
                return pathologyRoots[index];
            }
        }

        return null;
    }

    public int getIndexOfPathologyRoot(ACRKeywordNode pathologyRoot) {
        if (!pathologyRootsCreated) {
            pathologyRoots = createPathologyRoots();
        }

        if (pathologyRoots != null) {
            for (int i = 0; i < pathologyRoots.length; i++) {
                if (pathologyRoot == pathologyRoots[i]) {
                    return i;
                }
            }
        }

        return -1;
    }

    @Override
    public TCKeywordACRInput createInput(final String id, TCQueryFilterKey filterKey,
            boolean usedForSearch, boolean exclusive, 
            TCKeyword...selectedKeywords) 
    {
        return new TCKeywordACRInput(id, filterKey, usedForSearch, exclusive,
        		selectedKeywords!=null?Arrays.asList(selectedKeywords):null);
    }

    public TCKeyword findAnatomyKeywordByValue(String anatomyValue) {
        ACRKeywordNode node = getAnatomyRoot().findNodeByCodeValue(anatomyValue);
        return node != null ? node.getKeyword() : null;
    }

    public TCKeyword findPathologyKeywordByValue(String anatomyValue, String pathologyValue) {
    	if (anatomyValue!=null) {
    		ACRKeywordNode node = getAnatomyRoot().findNodeByCodeValue(anatomyValue);
    		if (node!=null) {
    			ACRKeywordNode pathologyRoot = getPathologyRoot( node );
    			if (pathologyRoot!=null) {
    				ACRKeywordNode pathologyNode = pathologyRoot.findNodeByCodeValue( pathologyValue );
    				if (pathologyNode!=null) {
    					return pathologyNode.getKeyword();
    				}
    			}
    		}
    	}
    	
        for (ACRKeywordNode pathologyRoot : getPathologyRoots()) {
            ACRKeywordNode node = pathologyRoot.findNodeByCodeValue(pathologyValue);
            if (node != null) {
                return node.getKeyword();
            }
        }
        
        return null;
    }

    @Override
    public TCKeyword findKeyword(String codeValue) {
        if (codeValue != null) {
        	String anatomyValue = ACRKeyword.getAnatomyCodeValue(codeValue);
            TCKeyword anatomyKeyword = findAnatomyKeywordByValue(anatomyValue);

            TCKeyword pathologyKeyword = findPathologyKeywordByValue(anatomyValue,
            		ACRKeyword.getPathologyCodeValue(codeValue));

            if (anatomyKeyword != null && pathologyKeyword != null) {
                return new ACRKeyword(anatomyKeyword, pathologyKeyword);
            } else if (anatomyKeyword != null) {
                return anatomyKeyword;
            } else if (pathologyKeyword != null) {
                return pathologyKeyword;
            }

            return null;
        }

        return null;
    }

    public boolean isCompositeKeyword(TCKeyword keyword) {
        return keyword instanceof ACRKeyword;
    }

    public boolean isAnatomyKeyword(TCKeyword keyword) {
        return getAnatomyRoot().findNode(keyword) != null;
    }

    public boolean isPathologyKeyword(TCKeyword keyword) {
        for (ACRKeywordNode pathologyRoot : getPathologyRoots()) {
            if (pathologyRoot.findNode(keyword) != null) {
                return true;
            }
        }
        return false;
    }

    private ACRKeywordNode createAnatomyRoot() {
        anatomyRootCreated = true;

        String[] data = ACRData.rawdataAnatomy;

        ACRKeywordNode root = ACRKeywordNode.createRoot("root-0");

        for (String s : data) {
            root.addToSubtree(createAnatomyNode(s));
        }

        return root;
    }

    private ACRKeywordNode[] createPathologyRoots() {
        pathologyRootsCreated = true;

        String[][] datas = ACRData.rawdataPathology;

        ACRKeywordNode[] roots = new ACRKeywordNode[datas.length];

        for (int i = 0; i < datas.length; i++) {
            roots[i] = ACRKeywordNode.createRoot("root-"+i);

            String[] data = datas[i];

            for (String s : data) {
                roots[i].addToSubtree(createPathologyNode(s));
            }
        }

        return roots;
    }

    private ACRKeywordNode createAnatomyNode(String s) {
        String[] parts = s.trim().split(";");
        String value = parts[1].trim();

        return ACRKeywordNode.create(value, new TCDicomCode(getDesignatorId(),
                value, parts[0]));
    }

    private ACRKeywordNode createPathologyNode(String s) {
        String[] parts = s.trim().split(";");
        String value = parts[0].trim();

        return ACRKeywordNode.create(value, new TCDicomCode(getDesignatorId(),
                value, parts[1]));
    }

}
