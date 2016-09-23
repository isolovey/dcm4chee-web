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

package org.dcm4chee.web.dao.folder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import net.sf.json.JSONObject;

import org.dcm4chee.archive.entity.StudyPermission;
import org.dcm4chee.usr.model.Role;
import org.jboss.annotation.ejb.LocalBinding;
import org.jboss.system.server.ServerConfigLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision$ $Date$
 * @since 05.10.2010
 */
@Stateless
@LocalBinding (jndiBinding=StudyPermissionsLocal.JNDI_NAME)
public class StudyPermissionsBean implements StudyPermissionsLocal {
    
    private static Logger log = LoggerFactory.getLogger(StudyPermissionsBean.class);
    
    @PersistenceContext(unitName="dcm4chee-arc")
    private EntityManager em;

    private File dicomRolesFile;

    @PostConstruct
    private void config() {
        if (this.dicomRolesFile == null) {
            dicomRolesFile = new File(System.getProperty("dcm4chee-web3.cfg.path", "conf/dcm4chee-web3") + "roles.json");
            if (!dicomRolesFile.isAbsolute())
                dicomRolesFile = new File(ServerConfigLocator.locate().getServerHomeDir(), dicomRolesFile.getPath());
            if (log.isDebugEnabled()) {
                log.debug("mappingFile:"+dicomRolesFile);
            }
            if (!dicomRolesFile.exists()) {
                try {
                    if (dicomRolesFile.getParentFile().mkdirs())
                        log.info("M-WRITE dir:" +dicomRolesFile.getParent());
                    dicomRolesFile.createNewFile();
                } catch (IOException e) {
                    log.error("Roles file doesn't exist and can't be created!", e);
                }
            }
        }  
    }
    
    @SuppressWarnings("unchecked")
    public List<StudyPermission> getStudyPermissions(String studyInstanceUID) {
        return (List<StudyPermission>) em.createQuery("SELECT sp FROM StudyPermission sp WHERE sp.studyInstanceUID = :studyInstanceUID")
        .setParameter("studyInstanceUID", studyInstanceUID)
        .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<StudyPermission> getStudyPermissionsForPatient(long pk) {
        return (List<StudyPermission>) em.createQuery("SELECT sp FROM StudyPermission sp, Study s WHERE sp.studyInstanceUID = s.studyInstanceUID AND s.patientFk = :pk ORDER BY sp.role, sp.action")
        .setParameter("pk", pk)
        .getResultList();
    }

    public void grant(StudyPermission studyPermission) {
        em.persist(studyPermission);        
    }

    public void revoke(long pk) {
        this.em.createQuery("DELETE FROM StudyPermission sp WHERE sp.pk = :pk")
        .setParameter("pk", pk)
        .executeUpdate();
    }
    
    // TODO: change this to generic version using JPA 2.0 implementation
    @SuppressWarnings("unchecked")
    public List<String> grantForPatient(long pk, String action, String role) {
        List<String> suids = (List<String>) em.createQuery("SELECT s.studyInstanceUID FROM Study s WHERE s.patientFk = :pk AND s.studyInstanceUID NOT IN(SELECT sp.studyInstanceUID FROM StudyPermission sp WHERE sp.action = :action AND sp.role = :role)")
        .setParameter("pk", pk)
        .setParameter("action", action)
        .setParameter("role", role)
        .getResultList();
        for (String studyInstanceUID : suids) {
            StudyPermission sp = new StudyPermission();
            sp.setAction(action);
            sp.setRole(role);
            sp.setStudyInstanceUID(studyInstanceUID);
            em.persist(sp);
        }
        return suids;
    }
    
    @SuppressWarnings("unchecked")
    public List<String> revokeForPatient(long pk, String action, String role) {
        List<String> suids = (List<String>) em.createQuery("SELECT s.studyInstanceUID FROM Study s WHERE s.patientFk = :pk AND s.studyInstanceUID NOT IN(SELECT sp.studyInstanceUID FROM StudyPermission sp WHERE sp.action = :action AND sp.role = :role)")
        .setParameter("pk", pk)
        .setParameter("action", action)
        .setParameter("role", role)
        .getResultList();
        this.em.createQuery("DELETE FROM StudyPermission sp WHERE sp.studyInstanceUID IN(SELECT s.studyInstanceUID FROM Study s WHERE s.patientFk = :pk) AND sp.action = :action AND sp.role = :role")
        .setParameter("pk", pk)
        .setParameter("action", action)
        .setParameter("role", role)
        .executeUpdate();
        return suids;
    }
    
    public long countStudiesOfPatient(long pk) {
        return (Long) 
        em.createQuery("SELECT COUNT(s) FROM Patient p, IN(p.studies) s WHERE p.pk = :pk")
        .setParameter("pk", pk)
        .getSingleResult();
    }

    public List<String> getAllDicomRolenames() {
        List<String> dicomRolenames = new ArrayList<String>();
        for (Role dicomRole : getAllDicomRoles())
            dicomRolenames.add(dicomRole.getRolename());
        return dicomRolenames;
    }
    
    public List<Role> getAllDicomRoles() {
        BufferedReader reader = null;
        try {
            List<Role> roleList = new ArrayList<Role>();
            String line;
            reader = new BufferedReader(new FileReader(dicomRolesFile));
            while ((line = reader.readLine()) != null) {
                Role role = (Role) JSONObject.toBean(JSONObject.fromObject(line), Role.class);
                if (role.isDicomRole()) 
                    roleList.add(role);
            }
            Collections.sort(roleList);
            return roleList;
        } catch (Exception e) {
            log.error("Can't get dicom roles from roles file!", e);
            return null;
        } finally {
            close(reader, "dicom roles file reader");
        }
    }
    
    // TODO: change this to generic version using JPA 2.0 implementation
    @SuppressWarnings("unchecked")
    public void updateDicomRoles() {
        List<String> newRoles = 
        		em.createQuery("SELECT DISTINCT sp.role FROM StudyPermission sp")
        		.getResultList();
        log.info("newRoles:"+newRoles);
        List<Role> roles = new ArrayList<Role>();
        BufferedReader reader = null;
        try {
            String line;
            reader = new BufferedReader(new FileReader(dicomRolesFile));
            while ((line = reader.readLine()) != null) {
                Role role = (Role) JSONObject.toBean(JSONObject.fromObject(line), Role.class);
                if (newRoles.contains(role.getRolename())) {
                    role.setDicomRole(true);
                    newRoles.remove(role.getRolename());
                }
                roles.add(role);
            }
            if (close(reader, "roles file reader"))
                reader = null;
            log.info("newRoles to add:"+newRoles);
            for (String rolename : newRoles) {
                Role role = new Role(rolename);
                role.setDicomRole(true);
                roles.add(role);
            }
            Collections.sort(roles);
            log.info("save Roles:"+roles);
            save(roles);
        } catch (Exception e) {
            log.error("Can't get roles from roles file!", e);
            return;
        } finally {
            close(reader, "roles file reader in finally");
        }
    }

    public void addDicomRole(String rolename) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(dicomRolesFile, true));
            Role role = new Role(rolename);
            role.setDicomRole(true);
            JSONObject jsonObject = JSONObject.fromObject(role);
            writer.write(jsonObject.toString());
            writer.newLine();
        } catch (IOException e) {
            log.error("Can't add dicom role to roles file!", e);
        } finally {
            close(writer, "roles file reader");
        }
    }

    public void removeDicomRole(Role role) {
                
        List<Role> roles = new ArrayList<Role>();
        BufferedReader reader = null;
        try {
            String line;
            reader = new BufferedReader(new FileReader(dicomRolesFile));
            while ((line = reader.readLine()) != null) { 
                Role currentRole = (Role) JSONObject.toBean(JSONObject.fromObject(line), Role.class);
                if (currentRole.equals(role))
                    currentRole.setDicomRole(false);
                roles.add(currentRole);
            }
        } catch (Exception e) {
            log.error("Can't get roles from roles file!", e);
            return;
        } finally {
            close(reader, "roles file reader");
        }
        save(roles);
    }
    
    private void save(List<Role> roles) {
        BufferedWriter writer = null;
        try {
            File tmpFile = File.createTempFile(dicomRolesFile.getName(), null, dicomRolesFile.getParentFile());
            log.info("tmpFile:"+tmpFile);
            writer = new BufferedWriter(new FileWriter(tmpFile, true));
            JSONObject jsonObject;
            for (int i=0,len=roles.size() ; i < len ; i++) {
                jsonObject = JSONObject.fromObject(roles.get(i));
                writer.write(jsonObject.toString());
                writer.newLine();
            }
            if (close(writer, "Temporary roles file"))
                writer = null;
            dicomRolesFile.delete();
            tmpFile.renameTo(dicomRolesFile);
            log.info("dicomRolesFile:"+dicomRolesFile);
        } catch (IOException e) {
            log.error("Can't save roles in roles file!", e);
        } finally {
            close(writer, "Temporary roles file (in finally block)");
        }        
    }
    
    private boolean close(Closeable toClose, String desc) {
        log.debug("Closing ",desc);
        if (toClose != null) {
            try {
                toClose.close();
                return true;
            } catch (IOException ignore) {
                log.warn("Error closing : "+desc, ignore);
            }
        }
        return false;
    }
}
