package org.dcm4chee.web.war;


import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import org.apache.wicket.Page;
import org.apache.wicket.Session;
import org.apache.wicket.authorization.strategies.role.Roles;
import org.apache.wicket.protocol.http.MockServletContext;
import org.apache.wicket.security.hive.authorization.Principal;
import org.apache.wicket.security.swarm.strategies.SwarmStrategy;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.dcm4chee.archive.entity.AE;
import org.dcm4chee.archive.entity.Code;
import org.dcm4chee.archive.entity.ContentItem;
import org.dcm4chee.archive.entity.File;
import org.dcm4chee.archive.entity.FileSystem;
import org.dcm4chee.archive.entity.GPPPS;
import org.dcm4chee.archive.entity.GPSPS;
import org.dcm4chee.archive.entity.GPSPSPerformer;
import org.dcm4chee.archive.entity.GPSPSRequest;
import org.dcm4chee.archive.entity.Instance;
import org.dcm4chee.archive.entity.Issuer;
import org.dcm4chee.archive.entity.MPPS;
import org.dcm4chee.archive.entity.MWLItem;
import org.dcm4chee.archive.entity.Media;
import org.dcm4chee.archive.entity.OtherPatientID;
import org.dcm4chee.archive.entity.Patient;
import org.dcm4chee.archive.entity.RequestAttributes;
import org.dcm4chee.archive.entity.Series;
import org.dcm4chee.archive.entity.Study;
import org.dcm4chee.archive.entity.StudyOnFileSystem;
import org.dcm4chee.archive.entity.UPS;
import org.dcm4chee.archive.entity.UPSRelatedPS;
import org.dcm4chee.archive.entity.UPSReplacedPS;
import org.dcm4chee.archive.entity.UPSRequest;
import org.dcm4chee.archive.entity.UPSSubscription;
import org.dcm4chee.archive.entity.VerifyingObserver;
import org.dcm4chee.usr.entity.User;
import org.dcm4chee.usr.entity.UserRoleAssignment;
import org.dcm4chee.web.common.login.LoginPage;
import org.dcm4chee.web.dao.folder.StudyListBean;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bm.testsuite.BaseSessionBeanFixture;

public class WicketApplicationTest extends BaseSessionBeanFixture<StudyListBean>
{
    private static final String USER = "user";
    private static final String ADMIN = "admin";
    private WicketApplication testApplicaton;
    private WicketTester wicketTester;
    
    private static Logger log = LoggerFactory.getLogger(WicketApplicationTest.class);

    private static final Class<?>[] usedBeans = {Patient.class, Study.class, Series.class, Instance.class,
        File.class, FileSystem.class, StudyOnFileSystem.class, VerifyingObserver.class,
        Media.class, MPPS.class, GPSPS.class, GPPPS.class, GPSPSRequest.class, GPSPSPerformer.class,
        MWLItem.class,  
        OtherPatientID.class, AE.class, RequestAttributes.class, Code.class, User.class, UserRoleAssignment.class,
        Issuer.class, ContentItem.class, UPS.class, UPSRequest.class, UPSSubscription.class, UPSRelatedPS.class,
        UPSReplacedPS.class};
    public WicketApplicationTest() throws Exception {
        super(StudyListBean.class, usedBeans);
    }
    @Override
    public void setUp() throws Exception {
        this.initDummyMBean();
        WASPTestUtil.initRolesMappingFile();
        super.setUp();
        testApplicaton = new WicketApplication();
        wicketTester = WASPTestUtil.getWicketTester(testApplicaton);
        MockServletContext ctx =(MockServletContext)wicketTester.getApplication().getServletContext();
        ctx.addInitParameter("WebCfgServiceName", "dcm4chee.web:service=WebConfig");
        ctx.addInitParameter("PagemapTimeout", "5");
        ctx.addInitParameter("LoginAllowedRolename", "LoginAllowed");
    }
    
    private void initDummyMBean() {
        MBeanServer mbServer = MBeanServerFactory.createMBeanServer("jboss");
        try {
            mbServer.createMBean("org.dcm4chee.web.war.DummyWebCfgMBean", 
                    new ObjectName("dcm4chee.web:service=WebConfig"));
        } catch (Exception ignore) {log.error("Can't create DummyWebCfgMBean!",ignore);}        
        try {
            mbServer.createMBean("org.dcm4chee.web.war.DummyServerConfigMBean", 
                    new ObjectName("jboss.system:type=ServerConfig"));
        } catch (Exception ignore) {log.error("Can't create ServerConfigBean!",ignore);}        
    }

    
    @Test
    public void testShouldAuthChallenge() {
        wicketTester.startPage(MainPage.class);
        wicketTester.assertRenderedPage(LoginPage.class);
    }
    @Test
    public void testAdminLoginShouldAllow() {
        checkLogin(ADMIN, ADMIN, MainPage.class);
    }
    @Test
    public void testAdminLoginShouldFail() {
        checkLogin(ADMIN, "admon", LoginPage.class);
    }
    @Test
    public void testUserLoginShouldAllow() {
        checkLogin(USER, USER, MainPage.class);
    }
    @Test
    public void testUserLoginShouldFail() {
        checkLogin(USER, "wrong", LoginPage.class);
    }
    @Test
    public void testUnknownLoginShouldFail() {
        checkLogin("unknown", "unknown", LoginPage.class);
    }

    @Test
    public void testAdminRoles() {
         checkRoles(ADMIN, new String[]{"FolderActions","Dashboard","FileSystem","AEWrite","MWLRead","UserRead",
                 "SystemInfo","UserManagement","FolderRead","MWLWrite","FolderWrite","RoleWrite","AERead",
                 "RoleRead","StudyPermissionsWrite","LoginAllowed","ReportWrite","Queue","ReportRead",
                 "UserWrite","TrashActions","TrashRead","TCRead","TCEdit"});
    }
    @Test
    public void testUserRoles() {
        checkRoles( USER, new String[]{"FolderActions","Dashboard","AEWrite","FileSystem","MWLRead","SystemInfo",
                "FolderRead","MWLWrite","FolderWrite","AERead","LoginAllowed","StudyPermissionsWrite","Queue",
                "ReportRead","TrashActions","TrashRead","TCRead"});
    }
    
    @Test
    public void testDocRoles() {
        checkRoles( "doc", new String[]{"FolderActions","Dashboard","MWLRead","FolderRead","MWLWrite","FolderWrite",
                "LoginAllowed","StudyPermissionsWrite","TrashActions","TrashRead","TCRead","TCEdit"});
    }
    @Test
    public void testGuestRoles() {
        checkRoles( "guest", new String[]{"FolderRead","LoginAllowed"});
    }
/*_*/
    private void checkLogin(String user, String passwd, Class<? extends Page> pageClass) {
        wicketTester.startPage(MainPage.class);
        FormTester formTester = wicketTester.newFormTester("signInPanel:signInForm");
        formTester.setValue("username", user);
        formTester.setValue("password", passwd);
        formTester.submit();
        wicketTester.assertRenderedPage(pageClass);
    }

    private void checkRoles(String user, String[] roles) {
        Roles r = new Roles();
        try {
            checkLogin(user, user, MainPage.class);
            Session session = Session.get();
            assertNotNull("Wicket Session is null", session);
            assertEquals("Wrong Class of Wicket Session!", AuthenticatedWebSession.class, session.getClass());
            for ( Principal p : ((SwarmStrategy) SwarmStrategy.get()).getSubject().getPrincipals() ) {
                log.info("### Principal:"+p.getName());
                r.add(p.getName());
            }
            assertEquals("Wrong number of roles!",roles.length, r.size());
            for ( String role : roles) {
                assertTrue("Missing role:"+role, r.hasRole(role));
            }
        } catch (Throwable t) {
            fail(user+"("+r+"): "+t.getMessage());
            t.printStackTrace();
        }
    }
}
