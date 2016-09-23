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
 * Portions created by the Initial Developer are Copyright (C) 2002-2005
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

package org.dcm4chee.dashboard.ui.util;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.apache.wicket.RequestCycle;
import org.dcm4chee.dashboard.ui.report.CreateOrEditReportPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision$ $Date$
 * @since 11.03.2010
 */
public class DatabaseUtils {

    private static Logger log = LoggerFactory.getLogger(CreateOrEditReportPage.class);

    public static ResultSet getResultSet(Connection jdbcConnection, String statement, Map<String, String> parameters) throws SQLException, Exception {

        ResultSet resultSet = null;
        if (parameters == null)
            resultSet = 
                jdbcConnection
                .createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)
                .executeQuery(statement);
        else
            resultSet = 
                DatabaseUtils.createPreparedStatement(jdbcConnection, statement, parameters)
                .executeQuery();
        return resultSet;
    }
    
    public static Connection getDatabaseConnection(String dataSourceName) {

        Context jndiContext = null;
        try {
            jndiContext = new InitialContext();
            return ((DataSource) 
                    (jndiContext).lookup(dataSourceName.trim()))
                    .getConnection();
        } catch (Exception e) {
            log.error("Failed to get database connection:", e);
        } finally {
            try {
                jndiContext.close();
            } catch (Exception ignore) {
            }
        }
        return null;
    }
    
    private static PreparedStatement createPreparedStatement(Connection connection, String sqlStatement, Map<String, String> parameters) throws SQLException, Exception {

        PreparedStatement preparedStatement = connection.prepareStatement(createSQLStatement(sqlStatement), ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);        
        int i = 1;

        for (String parameterName : getParameterOccurences(sqlStatement)) {
            if (parameterName.startsWith("text"))
                preparedStatement.setString(i, parameters.get(parameterName));
            else if (parameterName.startsWith("int"))
                preparedStatement.setInt(i, new Integer(parameters.get(parameterName)));
            else if (parameterName.startsWith("float"))
                preparedStatement.setFloat(i, new Float(parameters.get(parameterName)));
            else if (parameterName.startsWith("boolean"))
                preparedStatement.setBoolean(i, new Boolean(parameters.get(parameterName)));
            else if (parameterName.startsWith("date")) 
                preparedStatement.setDate(i, Date.valueOf(
                        new SimpleDateFormat("yyyy-MM-dd").format(
                                SimpleDateFormat.getDateInstance(
                                        SimpleDateFormat.SHORT, 
                                        RequestCycle.get().getSession().getLocale()
                                ).parse(parameters.get(parameterName)
                            )
                        )
                    )
                );
            i++;
        }
        return preparedStatement;
    }
    
    public static String createSQLStatement(String statement) {
        return Pattern.compile("\\[\\:(text|int|float|boolean|date)\\:[A-Za-z0-9\\.]*\\:\\]")
                .matcher(statement)
                .replaceAll("?");
    }
    
    public static List<String> getParameterOccurences(String statement) {
        List<String> parameters = new ArrayList<String>();
        Matcher m = Pattern.compile("\\[\\:(text|int|float|boolean|date)\\:[A-Za-z0-9\\.]*\\:\\]").matcher(statement);
        while(m.find()) parameters.add(m.group().replaceAll("(\\[\\:|\\:\\])", "").replaceAll("\\:", " "));
        return parameters;
    }
    
    public static boolean isConfigurableStatement(String statement) {
        return Pattern.compile("\\[\\:(text|int|float|boolean|date)\\:[A-Za-z0-9\\.]*\\:\\]").matcher(statement).find();
    }
    
    public static Set<String> getParameterSet(String statement) {
        Set<String> parameters = new TreeSet<String>();
        Matcher m = Pattern.compile("\\[\\:(text|int|float|boolean|date)\\:[A-Za-z0-9\\.]*\\:\\]").matcher(statement);
        while(m.find()) parameters.add(m.group().replaceAll("(\\[\\:|\\:\\])", "").replaceAll("\\:", " "));
        return parameters;
    }
}
