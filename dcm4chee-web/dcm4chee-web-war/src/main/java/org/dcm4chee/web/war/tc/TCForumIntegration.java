package org.dcm4chee.web.war.tc;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.Cookie;

import org.apache.wicket.RequestCycle;
import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.WebResponse;
import org.dcm4chee.web.common.secure.SecureSession;

public enum TCForumIntegration {
	JForum;

	private ForumAPI api;
	
	public static TCForumIntegration get(String type) {
		if (type!=null) {
			for (TCForumIntegration i : values()) {
				if (type.equalsIgnoreCase(i.name())) {
					return i;
				}
			}
		}
		return null;
	}
	
	public void setUserCookie() throws Exception {
		getAPI().setUserCookie();
	}
	
	public void setAdminUserCookie() throws Exception {
		getAPI().setAdminUserCookie();
	}
	
	public String getPostsPageURL(TCModel tccase) throws Exception {
		return getAPI().getPostsPageURL(tccase);
	}
	
	public String getPostsPageURL(TCObject tccase) throws Exception {
		return getAPI().getPostsPageURL(tccase);
	}
	
	public String getAdminPageURL() throws Exception {
		return getAPI().getAdminPageURL();
	}
	
	private synchronized ForumAPI getAPI() throws Exception {
		if (api==null) {
			if (JForum.equals(this)) {
				api = new JForumAPI();
			}
		}
		return api;
	}
	
	public abstract class ForumAPI {	
		public abstract void setUserCookie() throws Exception;
		public abstract void setAdminUserCookie() throws Exception;
		public abstract String getPostsPageURL(TCModel tccase) throws Exception;
		public abstract String getPostsPageURL(TCObject tccase) throws Exception;
		public abstract String getAdminPageURL() throws Exception;
		protected String getUserName() {
			return ((SecureSession)Session.get()).getUsername();
		}
	}
		
	public class JForumAPI extends ForumAPI {
		private final String BASE_URL = getBaseURL();
		private final String API_KEY = "dcm4chee-web";
		private final String SSO_COOKIE_NAME = "dcm4chee-web.userId";
		@Override
		public String getPostsPageURL(TCObject tccase) throws Exception {
			return getPostsPageURL(tccase.getInstanceUID(), tccase.getTitle(), 
					tccase.getCreationDate(), tccase.getAuthorName());
		}
		@Override
		public String getPostsPageURL(TCModel tccase) throws Exception {
			return getPostsPageURL(tccase.getSOPInstanceUID(), tccase.getTitle(), 
					tccase.getCreationDate(), tccase.getAuthor());
		}
		@Override
		public String getAdminPageURL() throws Exception {
			StringBuilder url = new StringBuilder();
			url.append(BASE_URL);
			url.append("admBase/main.page");
			return url.toString();
		}
		private String getPostsPageURL(String caseUID, String caseTitle, Date caseDate, String caseAuthor) throws Exception {
			// make sure, that the current user is available in jforum
			registerCurrentUser();

			//if there's no topic for that case yet
			int topicId = fetchTopicId(caseUID);

			if (topicId<0) {
				//...create a new topic/initial post on the fly
				topicId = createInitialPost(caseTitle, caseDate, caseAuthor);

				//...and add a persistent case-id -> topic-id mapping
				if (topicId>=0) {
			      	storeTopicId(caseUID, topicId);
				}
			}
			
			//...now we are ready to open the case' posts page
			if (topicId>=0) {
				//needed to autom. login current user into jforum
				setUserCookie();
				
		      	StringBuilder targetUrl = new StringBuilder();
		      	targetUrl.append("http://");
				targetUrl.append(BASE_URL);
		      	targetUrl.append("/posts/list/");
		      	targetUrl.append(topicId); //topic-id
		      	targetUrl.append(".page");

		      	return targetUrl.toString();
			}
			
			return null;
		}
		
		@Override
		public void setUserCookie() throws Exception {
			Cookie cookie = new Cookie(SSO_COOKIE_NAME, getUserName());
			cookie.setPath("/");
			((WebResponse)RequestCycle.get().getResponse()).addCookie(cookie);
		}
		
		@Override
		public void setAdminUserCookie() throws Exception {
			Cookie cookie = new Cookie(SSO_COOKIE_NAME, "admin");
			cookie.setPath("/");
			((WebResponse)RequestCycle.get().getResponse()).addCookie(cookie);
		}
		
		private void registerCurrentUser() throws Exception {
			String userName = getUserName();
			
			HashMap<String, String> params = new HashMap<String, String>(4);
			params.put("api_key", API_KEY);
			params.put("username", userName);
			params.put("email", userName+"@jforum.net");
			params.put("password", userName);
			
			TCUtilities.doHTTPPost(
					BASE_URL+"/userApi/insert.page", params, null);
		}		
		
		private int createInitialPost(String caseTitle, Date caseDate, String caseAuthor) throws Exception {
			StringBuilder title = new StringBuilder(caseTitle!=null && !caseTitle.isEmpty() ?
					caseTitle : "No Title");
			if (caseDate!=null || (caseAuthor!=null && !caseAuthor.isEmpty())) {
				title.append(" (");
				if (caseDate!=null) {
					Locale locale = Session.get()!=null ? Session.get().getLocale() : null;
					if (locale!=null) {
						locale = Locale.getDefault();
					}
					title.append(DateFormat.getDateInstance(
							DateFormat.SHORT, locale).format(caseDate)); 
				}
				if (caseAuthor!=null && !caseAuthor.isEmpty()) {
					if (caseDate!=null) {
						title.append(", ");
					}
					title.append(caseAuthor);
				}
				title.append(")");
			}			
			
			HashMap<String, String> params = new HashMap<String, String>(5);
			params.put("api_key", API_KEY);
			params.put("email", "admin@jforum.net");
			params.put("forum_id", "1");
			params.put("subject", title.toString());
			params.put("message", TCUtilities.getLocalizedString(
					"tc.forum.initialpost.text"));

			String response = TCUtilities.doHTTPPost(
					BASE_URL+"/postApi/insert.page", params, null);
			
			Pattern p = Pattern.compile("\\d+\\.page");
			Matcher m = p.matcher(response);
			if(m.find()) {
			    String result = m.group();
			    return Integer.valueOf(result.substring(0, result.lastIndexOf(".page")));
			}
			return -1;
		}
		
		private int fetchTopicId(String caseUID) throws Exception {
			HashMap<String, String> params = new HashMap<String, String>(2);
			params.put("api_key", API_KEY);
			params.put("case_id", caseUID);

			String xmlResponse = TCUtilities.doHTTPPost(
					BASE_URL+"/tccaseApi/listMapping.page", params, null);

			Pattern p = Pattern.compile("topic-id=\"\\d+\"");
			Matcher m = p.matcher(xmlResponse);
			if(m.find()) {
				String result = m.group();
				return Integer.valueOf(result.substring(
						result.indexOf("\"")+1, result.lastIndexOf("\"")));
			}

			return -1;
		}
		
		private void storeTopicId(String caseUID, int topicId) throws Exception {
			HashMap<String, String> params = new HashMap<String, String>(3);
			params.put("api_key", API_KEY);
			params.put("case_id", caseUID);
			params.put("topic_id", Integer.toString(topicId));

	      	TCUtilities.doHTTPPost(
	      			BASE_URL+"/tccaseApi/insertMapping.page", params, null);
		}
		
		private String getBaseURL() {
			StringBuilder url = new StringBuilder();
			String host = System.getProperty("jboss.bind.address");
			if (host==null || "0.0.0.0".equals(host)) {
				host = "localhost";
			}
			url.append(host);
			url.append(":8080");
			url.append("/jforum");
			return url.toString();
		}
	}
}
