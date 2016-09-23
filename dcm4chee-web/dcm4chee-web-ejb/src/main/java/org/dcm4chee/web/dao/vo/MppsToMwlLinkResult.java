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
 * Java(TM), available at http://sourceforge.net/projects/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * TIANI Medgraph AG.
 * Portions created by the Initial Developer are Copyright (C) 2003-2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * Gunter Zeilinger <gunter.zeilinger@tiani.com>
 * Franz Willer <franz.willer@gwi-ag.com>
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
package org.dcm4chee.web.dao.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.dcm4chee.archive.entity.MPPS;
import org.dcm4chee.archive.entity.MWLItem;
import org.dcm4chee.archive.entity.Study;

/**
 * @author franz.willer@gmail.com
 * @version $Revision$ $Date$
 * @since Apr 25, 2010
 */
public class MppsToMwlLinkResult implements Serializable {

    private static final long serialVersionUID = 3468299157101603139L;

    private MWLItem mwl;
    private List<MPPS> mppss = new ArrayList<MPPS>();
    private Set<Study> studiesToMove = new HashSet<Study>();
    private EntityTree entityTree;
    
    public void setMwl(MWLItem mwl) {
        this.mwl = mwl;
    }
    
    public void addMppsAttributes(MPPS mpps) {
        this.mppss.add(mpps);
    }
    
    public void addStudyToMove(Study study) {
        this.studiesToMove.add(study);
    }

    public MWLItem getMwl() {
        return mwl;
    }

    public List<MPPS> getMppss() {
        return mppss;
    }
    
    public Set<Study> getStudiesToMove() {
        return this.studiesToMove;
    }
    
    public EntityTree getEntityTree() {
        return entityTree;
    }

    public void setEntityTree(EntityTree entityTree) {
        this.entityTree = entityTree;
    }

    public boolean isUnlinkResult() {
        return mwl == null;
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MppsToMwlLinkResult:");
        if (mwl==null) {
            sb.append("UNLINK\nMPPS Attrs(previous):").append(mppss.get(0).getAttributes());
        } else {
            sb.append("LINK\nMWL Attrs:\n").append(mwl.getAttributes());
            sb.append("\n\nlinked MPPS: ").append(mppss.size());
            for (MPPS mpps : mppss) {
                sb.append("\nMPPS Attributes:").append(mpps).append("\n");
            }
            if (studiesToMove.size() > 0) {
                sb.append("\n Patient(s) of MPPSs was different to MWL patient! need move of studies:");
                for ( Study s : studiesToMove) {
                    sb.append("\nStudyIuid:").append(s.getStudyInstanceUID());
                }
            }
        }
        return sb.toString();
    }
}

