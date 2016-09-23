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

package org.dcm4chee.web.dao.trash;

import java.util.Date;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.jboss.annotation.ejb.LocalBinding;

/**
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision$ $Date$
 * @since May 31, 2011
 */
@Stateless
@LocalBinding(jndiBinding=TrashCleanerLocal.JNDI_NAME)
public class TrashCleanerBean implements TrashCleanerLocal {

    @PersistenceContext(unitName="dcm4chee-arc")
    private EntityManager em;

    public void clearTrash(Date limit) {
        em.createQuery("UPDATE PrivateFile p SET p.instance = NULL " + 
                        "WHERE p.instance.id IN " + 
                        "(SELECT pi FROM PrivateInstance pi WHERE pi.createdTime < :limit)")
            .setParameter("limit", limit)
            .executeUpdate();
        em.createQuery("DELETE FROM PrivateInstance pi WHERE pi.createdTime < :limit")
            .setParameter("limit", limit)
            .executeUpdate();
        em.createQuery("DELETE FROM PrivateSeries ps WHERE SIZE(ps.instances) = 0")
        .executeUpdate();
        em.createQuery("DELETE FROM PrivateStudy ps WHERE SIZE(ps.series) = 0")
        .executeUpdate();
        em.createQuery("DELETE FROM PrivatePatient pp WHERE SIZE(pp.studies) = 0")
        .executeUpdate();
    }
}
