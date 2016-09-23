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

package org.dcm4chee.cleanup.dao;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.dcm4chee.archive.entity.File;
import org.dcm4chee.archive.entity.PrivateFile;
import org.jboss.annotation.ejb.LocalBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Franz Willer <fwiller@gmail.com>
 * @version $Revision$ $Date$
 * @since Dec 03, 2012
 */

@Stateless
@LocalBinding (jndiBinding=FileCleanupLocal.JNDI_NAME)
public class FileCleanupBean implements FileCleanupLocal {

    private static Logger log = LoggerFactory.getLogger(FileCleanupBean.class);   
            
    @PersistenceContext(unitName="dcm4chee-arc")
    private EntityManager em;

    public List<File> getDuplicatedFiles(String groupID) {
        Query q = em.createQuery("SELECT DISTINCT OBJECT(f1) FROM File f1, File f2 JOIN FETCH f1.fileSystem"+
                " WHERE f1 != f2 AND f1.fileSystem.groupID = :groupID"+
                " AND f2.fileSystem = f1.fileSystem AND f1.filePath = f2.filePath"+
                " ORDER BY f1.filePath");
        q.setParameter("groupID", groupID);
        @SuppressWarnings("unchecked")
        List<File> files = (List<File>) q.getResultList();
        for (File f : files) {
            log.info("Found file pk:"+f.getPk());
            f.getInstance().getSeries().getStudy().getPatient().getPatientID();
        }
        return files;
    }
    
    public int countDuplicatedFiles(String groupID) {
        Query q = em.createQuery("SELECT COUNT(DISTINCT f1.pk) FROM File f1, File f2"+
                " WHERE f1 != f2 AND f1.fileSystem.groupID = :groupID"+
                " AND f2.fileSystem = f1.fileSystem AND f1.filePath = f2.filePath");
        q.setParameter("groupID", groupID);
        return ((Number) q.getSingleResult()).intValue();
    }

    public int countDuplicatedTrashFiles() {
        Query q = em.createQuery("SELECT COUNT(DISTINCT pf.pk) FROM PrivateFile pf, File f"+
                " WHERE pf.fileSystem = f.fileSystem AND pf.filePath = f.filePath");
        return ((Number) q.getSingleResult()).intValue();
    }

    @SuppressWarnings("unchecked")
    public List<PrivateFile> getDuplicatedTrashFiles() {
        Query q = em.createQuery("SELECT DISTINCT OBJECT(pf) FROM PrivateFile pf, File f"+
            " WHERE pf.fileSystem = f.fileSystem AND pf.filePath = f.filePath");
        List<PrivateFile> files = (List<PrivateFile>) q.getResultList();
        log.info("Found "+files.size()+" PrivateFiles duplicated in folder!");
        for (PrivateFile f : files) {
            log.info("Found PrivateFile pk:"+f.getPk());
            if (f.getInstance() == null) {
                log.info("Deleted from trash!");
            } else {
                log.info("PatID:"+f.getInstance().getSeries().getStudy().getPatient().getPatientID());
            }
        }
        return files;
    }
    
    public int deleteDuplicatedTrashFiles() {
        Query q = em.createQuery("SELECT DISTINCT OBJECT(pf) FROM PrivateFile pf, File f"+
        " WHERE pf.fileSystem = f.fileSystem AND pf.filePath = f.filePath");
        @SuppressWarnings("unchecked")
        List<PrivateFile> files = (List<PrivateFile>) q.getResultList();
        for (PrivateFile f : files) {
            em.remove(f);
        }
        em.createQuery("DELETE FROM PrivateInstance ps WHERE SIZE(ps.files) = 0")
        .executeUpdate();
        em.createQuery("DELETE FROM PrivateSeries ps WHERE SIZE(ps.instances) = 0")
        .executeUpdate();
        em.createQuery("DELETE FROM PrivateStudy ps WHERE SIZE(ps.series) = 0")
        .executeUpdate();
        em.createQuery("DELETE FROM PrivatePatient pp WHERE SIZE(pp.studies) = 0")
        .executeUpdate();
        return files.size();
    }
    
    public void updateFilePath(File f, String filePath) {
        File file = em.getReference(File.class, f.getPk());
        file.setFilePath(filePath);
        em.merge(file);
    }
 
}
