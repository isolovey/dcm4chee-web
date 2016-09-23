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

package org.dcm4chee.web.dao.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TemporalType;

import org.dcm4che2.data.PersonName;
import org.dcm4che2.data.Tag;
import org.dcm4chee.archive.conf.AttributeFilter;

/**
 * @author Franz Willer <fwiller@gmail.com>
 * @version $Revision$ $Date$
 * @since Apr 25, 2010
 */

public class QueryUtil {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
    
    private static final String ESCAPE_CHAR = "|";
    
    public static Query getQueryForPks(EntityManager em, String base, long[] pks) {
        Query q;
        int len=pks.length;
        if (len == 1) {
            q = em.createQuery(base+"= :pk").setParameter("pk", pks[0]);
        } else {
            StringBuilder sb = new StringBuilder(base);
            appendIN(sb, len);
            q = em.createQuery(sb.toString());
            setParametersForIN(q, pks);
        }
        return q;
    }
    
    public static void appendIN(StringBuilder sb, int len) {
        sb.append(" IN ( ?");
        for (int i = 1 ; i < len ; i++ ) {
            sb.append(i).append(", ?");
        }
        sb.append(len).append(" )");
    }
        
    public static void setParametersForIN(Query q, long[] pks) {
        int i = 1;
        for ( long pk : pks ) {
            q.setParameter(i++, pk);
        }
    }    
    
    public static void setParametersForIN(Query q, Object[] values) {
        int i = 1;
        for ( Object v : values ) {
            q.setParameter(i++, v);
        }
    }
        
    public static Query getPatientQuery(EntityManager em, String patId, String issuer) {
        StringBuilder sb = new StringBuilder();
        boolean useIssuer = issuer != null && issuer.trim().length() > 0;
        sb.append("SELECT OBJECT(p) FROM Patient p WHERE patientID = :patId");
        if (useIssuer) {
            sb.append(" AND issuerOfPatientID = :issuer");
        }
        Query qP = em.createQuery(sb.toString()).setParameter("patId", patId);
        if (useIssuer)
            qP.setParameter("issuer", issuer);
        return qP;
        
    }
        
    public static String checkAutoWildcard(String s, boolean allowWildcard) {
        if (isUniversalMatch(s)) {
            return null;
        } else if (!allowWildcard || s.indexOf('*')!=-1 || s.indexOf('?')!=-1 || s.indexOf('^')!=-1) {
            return s;
        } else {
            return s+'*';
        } 
    }
    public static boolean isUniversalMatch(String s) {
        return s == null || s.length() == 0  || s.equals("*");
    }
    public static boolean isUniversalPNMatch(String s) {
        return s == null || s.length() == 0 || s.equals("*") || s.equals("*^*");
    }
    public static boolean isUniversalMatch(String[] sa) {
        if (sa == null || sa.length == 0)
            return true;
        for (int i = 0 ; i < sa.length ; i++) {
            if (sa[i] == null || sa[i].equals("*"))
                return true;
        }
        return false;
    }
    public static boolean containsWildcard(String s) {
        return s.indexOf('*') != -1 || s.indexOf('?') != -1;
    }
    public static boolean needEscape(String s) {
        return s.indexOf('%') != -1 || s.indexOf('_') != -1;
    }

    public static boolean isMustNotNull(String s) {
        return "?*".equals(s) || "*?".equals(s);
    }

    public static String toLike(String s) {
        StringBuilder param = new StringBuilder();
        StringTokenizer tokens = new StringTokenizer(s, "*?_%", true);
        while (tokens.hasMoreTokens()) {
            String token = tokens.nextToken();
            switch (token.charAt(0)) {
            case '%':
                param.append(ESCAPE_CHAR+"%");
                break;
            case '*':
                param.append('%');
                break;
            case '?':
                param.append('_');
                break;
            case '_':
                param.append(ESCAPE_CHAR+"_");
                break;
            default:
                param.append(token);
            }
        }
        return param.toString();
    }

    public static void appendANDwithTextValue(StringBuilder ql, String fieldName, String varName, String value) {
        if (value!=null) {
            ql.append(" AND ").append(fieldName);
            if ("-".equals(value)) {
                ql.append(" IS NULL");
            } else if (isMustNotNull(value)) {
                ql.append(" IS NOT NULL");
            } else if (containsWildcard(value)) {
                ql.append(" LIKE ");
                if (needEscape(value)) {
                    appendESCAPE(ql, toLike(value));
                } else {
                    ql.append(toVarName(fieldName,varName));
                }
            } else {
                ql.append(" = ").append(toVarName(fieldName, varName));
            }
        }
    }

    public static void setTextQueryParameter(Query query, String varName, String value) {
        if (value!=null
                && !"-".equals(value)
                && !isMustNotNull(value)
                && !(containsWildcard(value) && needEscape(value))) {
            query.setParameter(varName,
                    containsWildcard(value)
                            ? toLike(value)
                            : value);
        }
    }

    public static void appendPatientNameFilter(StringBuilder ql, String alias, String patientName) {
        appendPatientName(ql, alias+".patientName", ":patientName", patientName);
    }

    public static void appendPatientName(StringBuilder ql, String fieldName, String varName, String patientName) {
        if (patientName!=null) {
            ql.append(" AND ").append(fieldName).append(" LIKE ");
            if (needEscape(patientName)) {
                appendESCAPE(ql, toPatientNameQueryString(patientName));
            } else {
                ql.append(toVarName(fieldName, varName));
            }
        }
    }
    
    public static void appendESCAPE(StringBuilder sb, String value) {
    	sb.append("'").append(value).append("' ESCAPE '").append(ESCAPE_CHAR).append("' ");
    }
    
    public static void appendPatientNameFuzzyFilter(StringBuilder ql, String alias, String patientName) {
        appendPersonNameFuzzyFilter(ql, alias+".patientFamilyNameSoundex", alias+".patientGivenNameSoundex", patientName);
    }
    public static void appendPersonNameFuzzyFilter(StringBuilder ql, String fnField, String gnField, String name) {
        PersonName pn = new PersonName(name);
        String fnSoundex = AttributeFilter.toSoundexWithLike(pn, PersonName.FAMILY);
        String gnSoundex = AttributeFilter.toSoundexWithLike(pn, PersonName.GIVEN);
        if (fnSoundex != null && gnSoundex != null) {
            ql.append(" AND ((").append(fnField).append(like(fnSoundex)).append(" :fnsx OR ").append(fnField).append(" ='*')")
            .append(" AND (").append(gnField).append(like(gnSoundex)).append(" :gnsx OR ").append(gnField).append(" ='*')")
            .append(" OR (").append(gnField).append(like(fnSoundex)).append(" :fnsx OR ").append(gnField).append(" ='*')")
            .append(" AND (").append(fnField).append(like(gnSoundex)).append(" :gnsx OR ").append(fnField).append(" ='*'))");
        } else if (fnSoundex!=null || gnSoundex!=null) {
            String varName = fnSoundex!=null ? " :fnsx" : " :gnsx"; 
            ql.append(" AND (").append(fnField).append(like(fnSoundex)).append(varName)
            .append(" OR ").append(gnField).append(like(fnSoundex)).append(varName)
            .append(" OR (").append(fnField).append(" ='*' AND ").append(gnField).append(" ='*'))");
        }
    }
    
    private static String like(String soundex) {
        return soundex != null && soundex.endsWith("%") ? " LIKE " : " = ";
    }
    
    public static void setPatientNameQueryParameter(Query query, String patientName) {
        setPatientNameQueryParameter(query, "patientName", patientName);
    }

    public static void setPatientNameFuzzyQueryParameter(Query query, String patientName) {
        PersonName pn = new PersonName(patientName);
        String fnSoundex = AttributeFilter.toSoundexWithLike(pn, PersonName.FAMILY);
        String gnSoundex = AttributeFilter.toSoundexWithLike(pn, PersonName.GIVEN);
        if (fnSoundex != null)
            query.setParameter("fnsx", fnSoundex);
        if (gnSoundex != null)
            query.setParameter("gnsx", gnSoundex);
    }

    public static void setPatientNameQueryParameter(Query query, String varName, String patientName) {
        if (patientName!=null && !needEscape(patientName)) {
            query.setParameter(varName, toPatientNameQueryString(patientName));
        }
    }

    public static void appendPatientIDFilter(StringBuilder ql, String alias,
            String patientID) {
        appendANDwithTextValue(ql, alias+".patientID", "patientID", patientID);
    }

    public static void setPatientIDQueryParameter(Query query,
            String patientID) {
        setTextQueryParameter(query, "patientID", patientID);
    }

    public static void appendIssuerOfPatientIDFilter(StringBuilder ql, String alias,
            String issuerOfPatientID) {
        appendANDwithTextValue(ql, alias+".issuerOfPatientID", "issuerOfPatientID", issuerOfPatientID);
    }

    public static void setIssuerOfPatientIDQueryParameter(Query query,
            String issuerOfPatientID) {
        setTextQueryParameter(query, "issuerOfPatientID", issuerOfPatientID);
    }

    public static void appendPatientBirthDateFilter(StringBuilder ql, String alias, Date minDate, Date maxDate) {
        if (minDate!=null) {
            if (maxDate==null) {
                ql.append(" AND "+alias+".patientBirthDate >= :birthdateMin");
            } else {
                ql.append(" AND "+alias+".patientBirthDate BETWEEN :birthdateMin AND :birthdateMax");
                
            }
        } else if (maxDate!=null) {
            ql.append(" AND "+alias+".patientBirthDate <= :birthdateMax");
        }
    }
    public static void setPatientBirthDateQueryParameter(Query query, Date minDate, Date maxDate) {
        if ( minDate!=null)
            query.setParameter("birthdateMin", sdf.format(minDate));
        if ( maxDate!=null)
            query.setParameter("birthdateMax", sdf.format(maxDate));
    }

    public static void appendStudyDateMinFilter(StringBuilder ql, Date date) {
        if (date != null) {
            ql.append(" AND s.studyDateTime >= :studyDateTimeMin");
        }
    }

    public static void appendStudyDateMaxFilter(StringBuilder ql, Date date) {
        if (date != null) {
            ql.append(" AND s.studyDateTime <= :studyDateTimeMax");
        }
    }
    
    public static void setStudyDateMinQueryParameter(Query query, Date date) {
        setStudyDateQueryParameter(query, date, "studyDateTimeMin");
    }

    public static void setStudyDateMaxQueryParameter(Query query, Date date) {
        setStudyDateQueryParameter(query, date, "studyDateTimeMax");
    }

    public static void setStudyDateQueryParameter(Query query, Date studyDate, String param) {
        if (studyDate != null) {
            query.setParameter(param, studyDate, TemporalType.TIMESTAMP);
        }
    }

    public static void appendAccessionNumberFilter(StringBuilder ql, String accessionNumber) {
        appendANDwithTextValue(ql, "s.accessionNumber", "accessionNumber", accessionNumber);
    }

    public static void setAccessionNumberQueryParameter(Query query, String accessionNumber) {
        setTextQueryParameter(query, "accessionNumber", accessionNumber);
    }

    public static void appendPpsWithoutMwlFilter(StringBuilder ql, boolean withoutPps, boolean ppsWithoutMwl, boolean filterModalities) {
    	if (withoutPps || ppsWithoutMwl) {
            ql.append(" AND (");
            if (withoutPps) {
                ql.append("EXISTS (SELECT ser FROM s.series ser WHERE ser.modalityPerformedProcedureStep IS NULL")
                .append(filterModalities ? " AND ser.modality NOT IN (:modalityFilter))" : ")")
                .append(ppsWithoutMwl ? " OR " : ")");
            }
            if (ppsWithoutMwl) {
                ql.append("EXISTS (SELECT ser FROM s.series ser WHERE ser.modalityPerformedProcedureStep")
                .append(" IS NOT NULL AND ser.modalityPerformedProcedureStep.accessionNumber IS NULL")
                .append(filterModalities ? " AND ser.modality NOT IN (:modalityFilter)))" : "))");
            }
        }
    }
    
    public static void setStudyInstanceUIDQueryParameter(Query query, String studyInstanceUID) {
        if (!isUniversalMatch(studyInstanceUID)) {
            query.setParameter("studyInstanceUID", studyInstanceUID);
        }
    }

    public static void appendDicomSecurityFilter(StringBuilder ql) {
        ql.append(" AND (s.studyInstanceUID IN (SELECT sp.studyInstanceUID FROM StudyPermission sp WHERE sp.action = 'Q' AND sp.role IN (:roles)))");
    }

    
    public static void appendModalityFilter(StringBuilder ql, String modality) {
        if (!isUniversalMatch(modality)) {
            ql.append(" AND EXISTS (SELECT ser FROM s.series ser WHERE ser.modality = :modality)");
        }
    }

    public static void appendModalitiesInStudyExactFilter(StringBuilder ql, String modality) {
        if (!isUniversalMatch(modality)) {
            ql.append(" AND s.modalitiesInStudy = :modality");
        }
    }

    public static void setModalityQueryParameter(Query query, String modality) {
        if (!isUniversalMatch(modality)) {
            query.setParameter("modality", modality);
        }
    }

    public static void appendSourceAETFilter(StringBuilder ql, String[] sourceAETs) {
        if (!isUniversalMatch(sourceAETs)) {
            if (sourceAETs.length == 1) {
                ql.append(" AND EXISTS (SELECT ser FROM s.series ser WHERE ser.sourceAET = :sourceAET)");
            } else {
                ql.append(" AND EXISTS (SELECT ser FROM s.series ser WHERE ser.sourceAET");
                appendIN(ql, sourceAETs.length);
                ql.append(")");
            }
        }
    }

    public static void setSourceAETQueryParameter(Query query, String[] sourceAETs) {
        if (!isUniversalMatch(sourceAETs)) {
            if (sourceAETs.length == 1) {
                query.setParameter("sourceAET", sourceAETs[0]);
            } else {
                setParametersForIN(query, sourceAETs);
            }
        }
    }

    public static void appendSeriesInstanceUIDFilter(StringBuilder ql, String seriesInstanceUID) {
        if (!isUniversalMatch(seriesInstanceUID)) {
            ql.append(" AND EXISTS (SELECT ser FROM s.series ser WHERE ser.seriesInstanceUID = :seriesInstanceUID)");
        }
    }
    
    public static void setSeriesInstanceUIDQueryParameter(Query query, String seriesInstanceUID) {
        if (!isUniversalMatch(seriesInstanceUID)) {
            query.setParameter("seriesInstanceUID", seriesInstanceUID);
        }
    }
    
    public static void appendOrderBy(StringBuilder ql, String[] order) {
        if (order != null && order.length > 0) {
            ql.append(" ORDER BY ").append(order[0]);
            for (int i = 1 ; i < order.length ; i++) {
                if (order[i] != null)
                    ql.append(", ").append(order[i]);
            }
        }
    }


    private static String toPatientNameQueryString(String patientName) {
        int padcarets = 4;
        StringBuilder param = new StringBuilder();
        if (AttributeFilter.getPatientAttributeFilter().isICase(Tag.PatientName))
            patientName = patientName.toUpperCase();
        StringTokenizer tokens = new StringTokenizer(patientName,
                "^*?_%", true);
        while (tokens.hasMoreTokens()) {
            String token = tokens.nextToken();
            switch (token.charAt(0)) {
            case '%':
                param.append(ESCAPE_CHAR+"%");
                break;
            case '*':
                param.append('%');
                break;
            case '?':
                param.append('_');
                break;
            case '^':
                padcarets--;
                param.append('^');
                break;
            case '_':
                param.append(ESCAPE_CHAR+"_");
                break;
            default:
                param.append(token);
            }
        }
        if (param.charAt(param.length()-1) != '%' && padcarets > 0)
            param.append("^%");
        return param.toString();
    }

    private static Object toVarName(String fieldName, String varName) {
        if (varName == null) {
            if (fieldName == null)
                throw new IllegalArgumentException("toVarName: filedName must not be null if varName is null");
            int pos = fieldName.lastIndexOf('.');
            varName = ":"+fieldName.substring(++pos);
        } else if (varName.charAt(0) != ':') {
            varName = ":"+varName;
        }
        return varName;
    }
    
    /**
     * Compare String values numeric.
     * Rules:
     * 1) null values are greater (sort to end). (both null - > compare pk's)
     * 2) both values numeric -> compare numeric (if equal compare pk's)
     * 3) none numeric values are always greater than numeric values
     * 4) both values not numeric -> compare textual (if equal compare pk's)
     * @param o1 BaseEntity 1 to compare pk's
     * @param o2 BaseEntity 2 to compare pk's
     * @param is1 String value 1
     * @param is2 String value 2
     * @return <0 if o1 < o2, 0 if o1 = o2 and >0 if o1 > o2
     */
    public static int compareIntegerStringAndPk(long pk1, long pk2, String is1, String is2) {
        if (is1 != null) {
            if (is2 != null) {
                try {
                    Integer i1 = new Integer(is1);
                    try {
                        int i = i1.compareTo(new Integer(is2));
                        if (i != 0)  
                            return i;
                    } catch (NumberFormatException x) {
                        return -1; 
                    }
                } catch (NumberFormatException x) {
                    try {
                        Integer.parseInt(is2);
                        return 1;
                    } catch (NumberFormatException x1) {
                        int i = is1.compareTo(is2);
                        if (i != 0)
                            return i;
                    }
                }
            } else {
                return -1;
            }
        } else if ( is2 != null) {
            return 1;
        }
        return new Long(pk1).compareTo(new Long(pk2));
    }
}
