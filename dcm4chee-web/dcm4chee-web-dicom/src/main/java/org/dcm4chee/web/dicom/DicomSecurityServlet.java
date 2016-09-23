package org.dcm4chee.web.dicom;

import java.io.PrintWriter;
import java.security.Principal;
import java.security.acl.Group;
import java.util.Enumeration;
import java.util.Iterator;

import javax.security.auth.Subject;
import javax.security.jacc.PolicyContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision: 15551 $ $Date: 2011-06-07 16:49:41 +0200 (Di, 07 Jun 2011) $
 * @since Feb. 18, 2011
 */
public class DicomSecurityServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    
    protected static Logger log = LoggerFactory.getLogger(DicomSecurityServlet.class);

    private String newline = System.getProperty("line.separator");

    public DicomSecurityServlet() {
        super();

        if (log.isInfoEnabled()) {
            try {
                log.info(getClass().getName() + ": " + (Subject) PolicyContext.getContext("javax.security.auth.Subject.container"));
            } catch (Exception e) {
                log.error(getClass().getName() + ": " + e.getMessage()); 
            }
        }
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) {
        try {
            StringBuffer dicomRoles = new StringBuffer();
            Principal rolesPrincipal;
            for (Iterator<Principal> i = ((Subject) PolicyContext.getContext("javax.security.auth.Subject.container")).getPrincipals().iterator() ; i.hasNext() ; ) {
                rolesPrincipal = i.next();
                if (rolesPrincipal instanceof Group) {
                    Enumeration<? extends Principal> e = ((Group) rolesPrincipal).members();
                    while (e.hasMoreElements()) { 
                        dicomRoles.append(e.nextElement().getName());
                        if (e.hasMoreElements())
                            dicomRoles.append(newline);
                    }
                }
            }
            response.setContentType("text/html");
            PrintWriter out = response.getWriter();
            out.write(dicomRoles.toString());
            out.flush();
            out.close();
        } catch (Exception e) {
            log.error(getClass().getName() + ": Error fetching dicom roles: ", e);
        }
    }
}
