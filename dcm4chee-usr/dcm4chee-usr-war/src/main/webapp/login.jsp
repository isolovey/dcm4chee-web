<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:wicket="http://wicket.apache.org">
    <head>
	    <title>${login.browser_title}</title>
	    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
         <link rel="stylesheet" type="text/css" href="resources/org.dcm4chee.web.common.base.BaseWicketPage/base-style.css" />
    </head>
    <body>
        <jsp:useBean id="login" scope="request" class="org.dcm4chee.web.common.login.LoginResources" />
        <% 
	        Cookie[] cookies = request.getCookies();
	        for (int i = 0; i < cookies.length; i++) {
	            if (cookies[i].getName().equals("WEB3LOCALE")) {
	                login.setLocale(cookies[i].getValue());
	                break;
	            }
	        }
        %>
        <div class="tabpanel">
            <div class="module-selector">
                <div class="tab-row">
			        <ul>
		            </ul>
                </div>
		        <div class="tab-logo" style="float: right; margin-top: 12px; height: 43px; padding-right: 10px; padding-left: 10px;">
		           <img alt="dcm4che.org" src="resources/org.dcm4chee.web.common.base.BaseWicketPage/images/logo.gif" /><br/>
		        </div>
	       </div>
	       <div class="module-panel"></div>
        </div>
        <div class="signin" style="padding-top: 160px;">
            <span class="login-desc">${login.loginLabel}</span>
            <div>
		        <form action="j_security_check">
		            <table style="padding-top: 60px; padding-right: 90px; padding-bottom: 10px;">
                        <tbody>
			                <tr style="text-align: left;">
			                    <td align="right">${login.username}</td>
			                    <td>
			                        <input type="text" name="j_username" size="30" />
			                    </td>
			                </tr>
			                <tr style="text-align: left;">
			                    <td align="right">${login.password}</td>
			                    <td>
			                        <input type="password" name="j_password" size="30" />
			                    </td>
			                </tr>
			                <tr style="text-align: left;">
			                    <td></td>
			                    <td>
			                        <input type="submit" name="submit" value="${login.submit}" />
			                        <input type="reset" value="${login.reset}" />
			                    </td>
			                </tr>
                        </tbody>
                    </table>
		        </form>
            </div>
        </div>
  </body>
</html>
