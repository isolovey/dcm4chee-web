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

package org.dcm4chee.usr.model;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

/**
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision$ $Date$
 * @since Apr. 19, 2011
 */
public class AETGroup implements Serializable, Comparable<AETGroup> {

    private static final long serialVersionUID = 1L;

    private String uuid;
    private String groupname;
    private String description;
    private Set<String> aets;

    public AETGroup() {
        this.uuid = UUID.randomUUID().toString();
    }
    
    public AETGroup(String groupname) {
        this();
        this.setGroupname(groupname);
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setGroupname(String groupname) {
        this.groupname = groupname;
    }

    public String getGroupname() {
        return groupname;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setAets(Set<String> aets) {
        this.aets = aets;
    }

    public Set<String> getAets() {
        return aets;
    }

    @Override
    public String toString() {
        return getGroupname();   
    }

    public int compareTo(AETGroup aetGroup) {
        int i = groupname.toUpperCase().compareTo(aetGroup.getGroupname().toUpperCase());
        return i == 0 ? groupname.compareTo(aetGroup.getGroupname()) : i;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof AETGroup) {
            String rn = ((AETGroup)o).groupname;
            return groupname == null ? rn == null : groupname.equals(rn);
        } else {
            return false;
        }
    }
    
    @Override
    public int hashCode() {
        return groupname.hashCode();
    }
}