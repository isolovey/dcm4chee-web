package org.dcm4chee.web.war.folder.arr;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

import javax.servlet.http.Cookie;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.wicket.RequestCycle;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WebRequest;
import org.dcm4chee.web.war.config.delegate.WebCfgDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

public class AuditRecordRepositoryFacade {

	private static Logger log = LoggerFactory.getLogger(AuditRecordRepositoryFacade.class);
	
	public enum Level { PATIENT, STUDY, PPS, SERIES, INSTANCE };
	
	public String doSearch(Level queryType, String queryParameter) { 
	  
		HttpURLConnection urlConnection = null;
		InputStream in = null;
		try {
			urlConnection = (HttpURLConnection) 
					new URL(WebCfgDelegate.getInstance().getArrUrl() + "?" 
							+ processParameters(queryType, queryParameter))
					.openConnection();
			WebRequest webRequest = ((WebRequest) RequestCycle.get().getRequest());
			Cookie[] cookies = webRequest.getCookies();
			if (cookies == null) 
				return null;           
			StringBuffer cookieValue = new StringBuffer();
			for (int i = cookies.length ; i > 0 ; ) {
				cookieValue.append(cookies[--i].getName()).append("=").append(cookies[i].getValue());
				if (i != 0)
				    cookieValue.append("; ");
			}
			urlConnection.setRequestProperty("Cookie", cookieValue.toString());
			urlConnection.connect();
			in = urlConnection.getInputStream();
			return transform(in);
		} catch (Exception e) {
			log.error(getClass().getName() + ": ", e);
			return null;
		} finally {
			if (in != null)
				try {
					in.close();
				} catch (IOException e) {
					log.error(getClass().getName() + ": Could not close input stream of url connection: ", e);
				}
			if (urlConnection != null)
				urlConnection.disconnect();
		}
	}

	private String processParameters(Level queryType, String queryParameter) throws Exception {	
		if (Level.PATIENT.equals(queryType)) 
			return 
					"participantObjectIDTypeCode=2&" + 
					"participantObjectID=" + 
					queryParameter;
		else if (Level.STUDY.equals(queryType)
				|| Level.PPS.equals(queryType)
				|| Level.SERIES.equals(queryType)
				|| Level.INSTANCE.equals(queryType))
			return 
					"participantObjectIDTypeCode=110180&" + 
					"participantObjectID=" + 
					queryParameter;
		else
			throw new Exception("Error creating web request for " + queryType
					+ " with parameter " + queryParameter);
	}

	private String transform(InputStream in) {
		try {
			StreamResult result = new StreamResult(new StringWriter());
			TransformerFactory.newInstance()
		       	.newTransformer(new StreamSource(
		       			((WebApplication) RequestCycle.get().getApplication())
		       			.getServletContext().getResource(
		       					RequestCycle.get().getSession().getLocale().getLanguage()
		       						.equals("de") ? 
		       							"/WEB-INF/Auditing_de.xsl" : 
		       								"/WEB-INF/Auditing_en.xsl")
		       			.openStream()))
		       		.transform(
		       				new SAXSource(new InputSource(in)), 
		       				result);
		       String entries = result.getWriter().toString();
		       result.getWriter().close();
		       return entries
		    		   .replaceAll("href=\"*\"", "")
		    		   .replaceAll("cellspacing=\"3\"", "cellspacing=\"0\"");
		} catch (Exception e) {
			log.error("Error transforming xml", e);
			return e.getLocalizedMessage();
		}
	}
}
