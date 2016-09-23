package org.dcm4chee.web.common.login;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.security.Principal;
import java.security.acl.Group;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.jacc.PolicyContext;

import net.sf.json.JSONObject;

import org.apache.wicket.RequestCycle;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.security.hive.authentication.DefaultSubject;
import org.apache.wicket.security.hive.authorization.SimplePrincipal;
import org.dcm4chee.web.common.base.BaseWicketApplication;
import org.dcm4chee.web.common.secure.SecureSession;
import org.jboss.system.server.ServerConfigLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginContextSecurityHelper {
    
    private static String rolesGroupName;
    
    protected static Logger log = LoggerFactory.getLogger(LoginContextSecurityHelper.class);
    
    static Map<String, String> readSwarmPrincipals() throws MalformedURLException, IOException {
        InputStream in = ((WebApplication) RequestCycle.get().getApplication()).getServletContext().getResource("/WEB-INF/dcm4chee.hive").openStream();
        BufferedReader dis = new BufferedReader (new InputStreamReader (in));
        HashMap<String, String> principals = new LinkedHashMap<String, String>();
        String line;
        String principal = null;
        while ((line = dis.readLine()) != null) 
            if (line.startsWith("grant principal ")) {
                principal = line.substring(line.indexOf("\"") + 1, line.lastIndexOf("\""));
                principals.put(principal, null);
            } else if ((principal != null) && (line.trim().startsWith("// KEY:"))) { 
                principals.put(principal, line.substring(line.indexOf("// KEY:") + 7).trim());
                principal = null;
            }
        in.close();
        return principals;
    }
    
    static DefaultSubject mapSwarmSubject(Subject jaasSubject, SecureSession session) throws IOException {
        getJaasRolesGroupName();
        DefaultSubject subject = new DefaultSubject();
        Map<String, Set<String>> mappings = null;
        Set<String> swarmPrincipals = new HashSet<String>();
        for (Principal principal : jaasSubject.getPrincipals()) {
            if (!(principal instanceof Group) && (session != null)) 
                session.setUsername(principal.getName());           
            if ((principal instanceof Group) && (rolesGroupName.equalsIgnoreCase(principal.getName()))) {
                Enumeration<? extends Principal> members = ((Group) principal).members();                    
                if (mappings == null) {
                    mappings = readRolesFile();
                }
                Set<String> set;
                while (members.hasMoreElements()) {
                    Principal member = members.nextElement();
                    if ((set = mappings.get(member.getName()) ) != null) {
                        for (Iterator<String> i = set.iterator() ; i.hasNext() ;) {
                            String appRole = i.next();
                            if (swarmPrincipals.add(appRole)) 
                                subject.addPrincipal(new SimplePrincipal(appRole));
                        }
                    }
                }
            }
        }
        return subject;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Set<String>> readRolesFile() throws IOException {
        String fn = System.getProperty("dcm4chee-web3.cfg.path");
        if (fn == null) 
            throw new FileNotFoundException("Web config path not found! Not specified with System property 'dcm4chee-web3.cfg.path'");
        File mappingFile = new File(fn + "roles.json");
        if (!mappingFile.isAbsolute())
            mappingFile = new File(ServerConfigLocator.locate().getServerHomeDir(), mappingFile.getPath());
        Map<String, Set<String>> mappings = new HashMap<String, Set<String>>();
        String line;
        BufferedReader reader = null;
        try { 
            reader = new BufferedReader(new FileReader(mappingFile));
            while ((line = reader.readLine()) != null) {
                JSONObject jsonObject = JSONObject.fromObject(line);
                Set<String> set = new HashSet<String>();
                Iterator<String> i = jsonObject.getJSONArray("swarmPrincipals").iterator();
                while (i.hasNext())
                    set.add(i.next());
                mappings.put(jsonObject.getString("rolename"), set);
            }
            return mappings;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignore) {}
            }
        }
    }

    static boolean checkLoginAllowed(DefaultSubject subject) {
        String loginAllowedRolename = 
                BaseWicketApplication.get()
                .getInitParameter("LoginAllowedRolename");
        // TODO: this is a workaround because of wicket tests in dcm4chee-web
        if (loginAllowedRolename == null)
            loginAllowedRolename =
                BaseWicketApplication.get()
                .getServletContext()
                .getInitParameter("LoginAllowedRolename");
        return loginAllowedRolename == null ? false : 
            subject.getPrincipals().contains(new SimplePrincipal(loginAllowedRolename)); 
    }

    public static Subject getJaasSubject() {
        try {
            return (Subject) PolicyContext.getContext("javax.security.auth.Subject.container");
        } catch (Exception x) {
            log.error("Failed to get subject from javax.security.auth.Subject.container", x);
            return null;
        }
    }
    
    public static String getJaasRolesGroupName() {
        if (rolesGroupName == null) {
            try {
                rolesGroupName = ((WebApplication) RequestCycle.get().getApplication()).getInitParameter("rolesGroupName");
                if (rolesGroupName == null) rolesGroupName = "Roles";
            } catch (Exception x) {
                log.error("Can't get InitParameter 'rolesGroupName' from Wicket Application!", x);
            }
        }
        return rolesGroupName;
    }
    public static final List<String> getJaasRoles() {
        getJaasRolesGroupName();
        List<String> roles = new ArrayList<String>();
        String rolesGroupName = ((WebApplication) RequestCycle.get().getApplication()).getInitParameter("rolesGroupName");
        if (rolesGroupName == null) rolesGroupName = "Roles";
        try {
            for (Principal principal : ((Subject) PolicyContext.getContext("javax.security.auth.Subject.container")).getPrincipals()) {
                if ((principal instanceof Group) && rolesGroupName.equalsIgnoreCase(principal.getName())) {
                    Enumeration<? extends Principal> members = ((Group) principal).members();
                    while (members.hasMoreElements()) 
                        roles.add(members.nextElement().getName());
                }
            }
        } catch (Exception e) {
            log.error("Failed to get jaas subject from javax.security.auth.Subject.container", e);
        }
        return roles;
    }

}
