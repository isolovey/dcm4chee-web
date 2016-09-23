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
package org.dcm4chee.web.dao.tc;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.dcm4che2.data.Tag;
import org.dcm4chee.archive.entity.File;
import org.dcm4chee.archive.entity.Instance;
import org.dcm4chee.archive.entity.Series;
import org.dcm4chee.archive.entity.Study;
import org.dcm4chee.web.dao.tc.TCQueryFilterValue.QueryParam;
import org.dcm4chee.web.dao.util.QueryUtil;
import org.jboss.annotation.ejb.LocalBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Bernhard Ableitinger <bernhard.ableitinger@agfa.com>
 * @version $Revision$ $Date$
 * @since May 05, 2011
 */
@Stateless
@LocalBinding(jndiBinding = TCQueryLocal.JNDI_NAME)
public class TCQueryBean implements TCQueryLocal {

    private static final Logger log = LoggerFactory
            .getLogger(TCQueryBean.class);

    @PersistenceContext(unitName = "dcm4chee-arc")
    private EntityManager em;

    public int countMatchingInstances(TCQueryFilter filter, List<String> roles,
            List<String> restrictedSourceAETs, boolean multipleKeywordORConcat) {
        if (roles != null && roles.isEmpty()) {
            return 0;
        }

        boolean doStudyPermissionCheck = roles != null;
        boolean doSourceAETCheck = restrictedSourceAETs != null
                && !restrictedSourceAETs.isEmpty();

        StringBuilder sb = new StringBuilder(64);
        sb.append(" Select COUNT(*) FROM Instance instance");

        if (doStudyPermissionCheck || doSourceAETCheck) {
            sb.append(" LEFT JOIN FETCH instance.series series");

            if (doStudyPermissionCheck) {
                sb.append(" LEFT JOIN FETCH series.study s");
            }
        }

        sb.append(" LEFT JOIN FETCH instance.conceptNameCode sr_code");
        sb.append(" WHERE (instance.sopClassUID = '1.2.840.10008.5.1.4.1.1.88.11')");
        sb.append(" AND (sr_code.codeValue = 'TCE006')");
        sb.append(" AND (sr_code.codingSchemeDesignator = 'IHERADTF')");

        if (doSourceAETCheck) {
            sb.append(" AND (series.sourceAET IN (");
            for (int i = 0; i < restrictedSourceAETs.size(); i++) {
                if (i > 0) {
                    sb.append(",");
                }

                sb.append("'").append(restrictedSourceAETs.get(i)).append("'");
            }
            sb.append("))");
        }

        Set<Entry<TCQueryFilterKey, TCQueryFilterValue<?>>> entries = filter
                .getEntries();
        Set<QueryParam[]> paramSets = null;

        if (entries != null) {
            for (Entry<TCQueryFilterKey, TCQueryFilterValue<?>> e : entries) {
                sb.append(" AND (");

                QueryParam[] params = e.getValue().appendSQLWhereConstraint(
                        e.getKey(), sb, multipleKeywordORConcat);
                if (params != null) {
                    if (paramSets == null) {
                        paramSets = new HashSet<QueryParam[]>();
                    }

                    paramSets.add(params);
                }

                sb.append(")");
            }
        }

        if (doStudyPermissionCheck) {
            QueryUtil.appendDicomSecurityFilter(sb);
        }

        Query query = em.createQuery(sb.toString());

        if (doStudyPermissionCheck) {
            query.setParameter("roles", roles);
        }

        if (paramSets != null) {
            for (QueryParam[] paramSet : paramSets) {
                if (paramSet != null) {
                    for (QueryParam param : paramSet) {
                        query.setParameter(param.getKey(), param.getValue());
                    }
                }
            }
        }

        return ((Number) query.getSingleResult()).intValue();
    }
    
    
    @SuppressWarnings("unchecked")
    public List<Instance> findMatchingInstances(String searchString,
            List<String> roles, List<String> restrictedSourceAETs) 
    {
        if (roles != null && roles.isEmpty()) {
            return Collections.emptyList();
        }

        boolean doStudyPermissionCheck = roles != null;
        boolean doSourceAETCheck = restrictedSourceAETs != null
                && !restrictedSourceAETs.isEmpty();

        StringBuilder sb = new StringBuilder(64);
        sb.append(" FROM Instance instance");

        if (doStudyPermissionCheck || doSourceAETCheck) {
            sb.append(" LEFT JOIN FETCH instance.series series");

            if (doStudyPermissionCheck) {
                sb.append(" LEFT JOIN FETCH series.study s");
            }
        }

        sb.append(" LEFT JOIN FETCH instance.conceptNameCode sr_code");
        sb.append(" LEFT JOIN FETCH instance.media");
        sb.append(" LEFT JOIN FETCH instance.contentItems content");
        sb.append(" LEFT JOIN FETCH content.conceptCode contentCode");
        
        sb.append(" WHERE (instance.sopClassUID = '1.2.840.10008.5.1.4.1.1.88.11')");
        sb.append(" AND (sr_code.codeValue = 'TCE006')");
        sb.append(" AND (sr_code.codingSchemeDesignator = 'IHERADTF')");

        if (doSourceAETCheck) {
            sb.append(" AND (series.sourceAET IN (");
            for (int i = 0; i < restrictedSourceAETs.size(); i++) {
                if (i > 0) {
                    sb.append(",");
                }

                sb.append("'").append(restrictedSourceAETs.get(i)).append("'");
            }
            sb.append("))");
        }
        
        QueryParam textParam = new QueryParam("searchString",
                "%" + searchString.replaceAll("\\*","%").toUpperCase() + "%");
        QueryParam meaningParam = new QueryParam("searchString",
                "%" + searchString.replaceAll("\\*","%").toUpperCase() + "%");
        
        sb.append(" AND (");
        sb.append("upper(content.textValue) LIKE :" + textParam.getKey());
        sb.append(" OR ");
        sb.append("upper(contentCode.codeMeaning) LIKE:" + meaningParam.getKey());
        sb.append(")");

        if (doStudyPermissionCheck) {
            QueryUtil.appendDicomSecurityFilter(sb);
        }

        Query query = em.createQuery(sb.toString());

        if (doStudyPermissionCheck) {
            query.setParameter("roles", roles);
        }

        query.setParameter(textParam.getKey(), textParam.getValue());
        query.setParameter(meaningParam.getKey(), meaningParam.getValue());

        log.info("Executing teaching-file query: " + query.toString());
        log.info("Restricted to aets: " + restrictedSourceAETs);

        List<Instance> instances = query.getResultList();

        if (instances != null) {
            for (Instance instance : instances) {
                join(instance);
            }

            log.info(instances.size() + " matching teaching-files found!");
        }

        return instances;
    }
    

    @SuppressWarnings("unchecked")
    public List<Instance> findMatchingInstances(TCQueryFilter filter,
            List<String> roles, List<String> restrictedSourceAETs, boolean multipleKeywordORConcat) {
        if (roles != null && roles.isEmpty()) {
            return Collections.emptyList();
        }

        boolean doStudyPermissionCheck = roles != null;
        boolean doSourceAETCheck = restrictedSourceAETs != null
                && !restrictedSourceAETs.isEmpty();

        StringBuilder sb = new StringBuilder(64);
        sb.append(" FROM Instance instance");

        if (doStudyPermissionCheck || doSourceAETCheck) {
            sb.append(" LEFT JOIN FETCH instance.series series");

            if (doStudyPermissionCheck) {
                sb.append(" LEFT JOIN FETCH series.study s");
            }
        }

        sb.append(" LEFT JOIN FETCH instance.conceptNameCode sr_code");
        sb.append(" LEFT JOIN FETCH instance.media");
        sb.append(" WHERE (instance.sopClassUID = '1.2.840.10008.5.1.4.1.1.88.11')");
        sb.append(" AND (sr_code.codeValue = 'TCE006')");
        sb.append(" AND (sr_code.codingSchemeDesignator = 'IHERADTF')");

        if (doSourceAETCheck) {
            sb.append(" AND (series.sourceAET IN (");
            for (int i = 0; i < restrictedSourceAETs.size(); i++) {
                if (i > 0) {
                    sb.append(",");
                }

                sb.append("'").append(restrictedSourceAETs.get(i)).append("'");
            }
            sb.append("))");
        }

        Set<Entry<TCQueryFilterKey, TCQueryFilterValue<?>>> entries = filter
                .getEntries();
        Set<QueryParam[]> paramSets = null;

        if (entries != null) {
            for (Entry<TCQueryFilterKey, TCQueryFilterValue<?>> e : entries) {
                sb.append(" AND (");

                QueryParam[] params = e.getValue().appendSQLWhereConstraint(
                        e.getKey(), sb, multipleKeywordORConcat);
                if (params != null) {
                    if (paramSets == null) {
                        paramSets = new HashSet<QueryParam[]>();
                    }

                    paramSets.add(params);
                }

                sb.append(")");
            }
        }

        if (doStudyPermissionCheck) {
            QueryUtil.appendDicomSecurityFilter(sb);
        }

        Query query = em.createQuery(sb.toString());

        if (doStudyPermissionCheck) {
            query.setParameter("roles", roles);
        }

        if (paramSets != null) {
            for (QueryParam[] paramSet : paramSets) {
                if (paramSet != null) {
                    for (QueryParam param : paramSet) {
                        query.setParameter(param.getKey(), param.getValue());
                    }
                }
            }
        }

        log.info("Executing teaching-file query: " + query.toString());
        log.info("Restricted to aets: " + restrictedSourceAETs);

        List<Instance> instances = query.getResultList();

        if (instances != null) {
            for (Instance instance : instances) {
                join(instance);
            }

            log.info(instances.size() + " matching teaching-files found!");
        }

        return instances;
    }
    
    public List<Instance> findInstancesOfPatient(
    		String patientId, String issuerOfPatientId, 
    		List<String> roles, List<String> restrictedSourceAETs)
    {
        if (roles != null && roles.isEmpty()) {
            return Collections.emptyList();
        }

        boolean doStudyPermissionCheck = roles != null;
        boolean doSourceAETCheck = restrictedSourceAETs != null
                && !restrictedSourceAETs.isEmpty();

        StringBuilder sb = new StringBuilder(64);
        sb.append(" FROM Instance instance");
        sb.append(" LEFT JOIN FETCH instance.series series");
        sb.append(" LEFT JOIN FETCH series.study s");
        sb.append(" LEFT JOIN FETCH s.patient p");
        sb.append(" LEFT JOIN FETCH instance.conceptNameCode sr_code");
        sb.append(" WHERE (instance.sopClassUID = '1.2.840.10008.5.1.4.1.1.88.11')");
        sb.append(" AND (sr_code.codeValue = 'TCE006')");
        sb.append(" AND (sr_code.codingSchemeDesignator = 'IHERADTF')");
        sb.append(" AND (p.patientID = :patID)");
        
        if (issuerOfPatientId!=null) {
        	sb.append(" AND (p.issuerOfPatientID = :issuerOfPatID)");
        }

        if (doSourceAETCheck) {
            sb.append(" AND (series.sourceAET IN (");
            for (int i = 0; i < restrictedSourceAETs.size(); i++) {
                if (i > 0) {
                    sb.append(",");
                }

                sb.append("'").append(restrictedSourceAETs.get(i)).append("'");
            }
            sb.append("))");
        }

        if (doStudyPermissionCheck) {
            QueryUtil.appendDicomSecurityFilter(sb);
        }

        Query query = em.createQuery(sb.toString());
        
        query.setParameter("patID", patientId);
        
        if (issuerOfPatientId!=null) {
        	query.setParameter("issuerOfPatID", issuerOfPatientId);
        }
        
        if (doStudyPermissionCheck) {
            query.setParameter("roles", roles);
        }

        log.info("Executing teaching-file query: " + query.toString());
        log.info("Restricted to aets: " + restrictedSourceAETs);

        List<Instance> instances = query.getResultList();

        if (instances != null) {
            for (Instance instance : instances) {
                join(instance);
            }

            log.info(instances.size() + " matching teaching-files found!");
        }

        return instances;
    }    		

    public Study findStudyByUID(String stuid) {
        Query q = em.createQuery("SELECT DISTINCT study FROM Study AS study LEFT JOIN FETCH study.series s LEFT JOIN FETCH s.instances WHERE study.studyInstanceUID = :stuid");
        q.setParameter("stuid", stuid);
        return (Study) q.getSingleResult();
    }
    
    public Series findSeriesByUID(String suid) {
        Query q = em.createQuery("SELECT DISTINCT s FROM Series s LEFT JOIN FETCH s.study LEFT JOIN FETCH s.instances WHERE s.seriesInstanceUID = :suid");
        q.setParameter("suid", suid);
        return (Series) q.getSingleResult();
    }
    
    @SuppressWarnings("unchecked")
	public Instance findInstanceByUID(String iuid) {
        Query q = em.createQuery("FROM Instance i LEFT JOIN FETCH i.series s LEFT JOIN FETCH s.study WHERE i.sopInstanceUID = :iuid");
        q.setParameter("iuid", iuid);
        
        List<Instance> result = q.getResultList();
        if (result!=null && !result.isEmpty()) {
        	return join(result.get(0));
        }
        return null;
    }
    
    public Instance findInstanceByUID(String iuid, List<String> roles) {
        StringBuilder sb = new StringBuilder("FROM Instance i LEFT JOIN FETCH i.series series LEFT JOIN FETCH series.study s WHERE i.sopInstanceUID = :iuid");
    	
        boolean doStudyPermissionCheck = roles!=null && !roles.isEmpty();
    	if (doStudyPermissionCheck) {
            QueryUtil.appendDicomSecurityFilter(sb);
        }

        Query query = em.createQuery(sb.toString());
        query.setParameter("iuid", iuid);
        
        if (doStudyPermissionCheck) {
            query.setParameter("roles", roles);
        }

        Instance instance = (Instance) query.getSingleResult();
        if (instance!=null) {
        	join(instance);
        }
        
        return instance;
    }
    
    @SuppressWarnings({ "unchecked" })
	public Map<String, Integer> getInstanceNumbers(String suid)
    {
    	if (suid!=null)
    	{
	    	Query q = em.createQuery("FROM Instance i WHERE i.series.seriesInstanceUID=:suid");
	    	q.setParameter("suid", suid);
	    	List<Instance> instances = q.getResultList();
	    	if (instances!=null && !instances.isEmpty())
	    	{
	    		Map<String, Integer> map = new HashMap<String, Integer>();
	    		for (Instance i : instances)
	    		{
	    			try
	    			{
		    			String instanceNumber = i.getInstanceNumber();
		    			map.put(i.getSOPInstanceUID(), Integer.valueOf(instanceNumber));
	    			}
	    			catch (Exception e)
	    			{
	    				map.put(i.getSOPInstanceUID(), null);
	    			}
	    		}
	    		return map;
	    	}
    	}
    	
    	return Collections.emptyMap();
    }
        
    @SuppressWarnings("unchecked")
	public Map<String, Integer> findMultiframeInstances(String stuid)
    {
    	if (stuid!=null)
    	{
    		// compile HQL query
	    	Query query = em.createQuery("FROM Instance i WHERE i.series.study.studyInstanceUID=:stuid");
	    	query.setParameter("stuid",stuid);
	    	
	    	// actually execute query
	    	List<Instance> result = (List<Instance>)query.getResultList();
	    	
	    	// compile result
	    	return findMultiframeInstances(result);
    	}
    	
    	return Collections.emptyMap();
    }
    
    @SuppressWarnings("unchecked")
	public Map<String, Integer> findMultiframeInstances(String stuid, String suid)
    {
    	if (stuid!=null && suid!=null)
    	{
    		// compile HQL query
	    	Query query = em.createQuery("FROM Instance i WHERE i.series.study.studyInstanceUID=:stuid AND i.series.seriesInstanceUID=:suid");
	    	query.setParameter("stuid",stuid);
	    	query.setParameter("suid",suid);
	    	
	    	// actually execute query
	    	List<Instance> result = (List<Instance>)query.getResultList();
	    	
	    	// compile result
	    	return findMultiframeInstances(result);
    	}
    	
    	return Collections.emptyMap();
    }
    
    @SuppressWarnings("unchecked")
	public Map<String, Integer> findMultiframeInstances(String stuid, String suid, String...iuids)
    {
    	if (iuids!=null && iuids.length>0)
    	{
    		// compile HQL query string
	    	StringBuilder queryString = new StringBuilder("FROM Instance i WHERE i.series.study.studyInstanceUID=:stuid AND i.series.seriesInstanceUID=:suid AND i.sopInstanceUID IN (");
	    	queryString.append("'").append(iuids[0]).append("'");
	    	for (int i=1; i<iuids.length; i++)
	    	{
	    		queryString.append(",'").append(iuids[i]).append("'");
	    	}
	    	queryString.append(")");

	    	Query query = em.createQuery(queryString.toString());
	    	query.setParameter("stuid",stuid);
	    	query.setParameter("suid",suid);
	    	
	    	// actually execute query
	    	List<Instance> result = (List<Instance>)query.getResultList();
	    	
	    	// compile result
	    	return findMultiframeInstances(result);
    	}
    	
    	return Collections.emptyMap();
    }
    
    
    private Map<String, Integer> findMultiframeInstances(List<Instance> instances)
    {
    	if (instances!=null && !instances.isEmpty())
    	{
    		Map<String, Integer> map = new HashMap<String, Integer>();
    		for (Instance i : instances)
    		{
    			int frames = i.getAttributes(false).getInt(Tag.NumberOfFrames);
    			if (frames>0)
    			{
    				map.put(i.getSOPInstanceUID(), frames);
    			}
    		}
    		return map;
    	}
    	
    	return Collections.emptyMap();
    }
    
    private Instance join(Instance instance) {
        // touch dicom tree
        instance.getSeries().getSeriesInstanceUID();
        instance.getSeries().getStudy().getStudyInstanceUID();
        instance.getSeries().getStudy().getPatient().getPatientID();

        List<File> files = instance.getFiles();
        if (files != null) {
            for (File file : files) {
                file.getFilePath();
                file.getFileSystem().getDirectoryPath();
            }
        }
        
        return instance;
    }
}
