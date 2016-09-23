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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.VR;
import org.dcm4chee.archive.entity.Code;

/**
 * @author Bernhard Ableitinger <bernhard.ableitinger@agfa.com>
 * @version $Revision$ $Date$
 * @since May 06, 2011
 */
public abstract class TCQueryFilterValue<T> implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private T[] value;
    
    public TCQueryFilterValue(T...values) {
        this.value = values!=null ? Arrays.copyOf(values, values.length) : null;
    }

    public T getValue() {
        return value[0];
    }
    
    public T[] getValues() {
    	return value;
    }

    @Override
    public String toString() {
    	if (value!=null && value.length>0)
    	{
    		StringBuilder sbuilder = new StringBuilder();
    		sbuilder.append(value[0].toString());
    		for (int i=1; i<value.length; i++)
    		{
    			sbuilder.append(";").append(value[i].toString());
    		}
    		return sbuilder.toString();
    	}
    	
        return "";
    }

    public abstract QueryParam[] appendSQLWhereConstraint(TCQueryFilterKey key,
            StringBuilder sb, boolean multipleValueORConcat);

    public static TCQueryFilterValue<String> create(String value) {
        
        return new TCQueryFilterValue<String>(value) {

            private static final long serialVersionUID = 1L;

            @Override
            public QueryParam[] appendSQLWhereConstraint(TCQueryFilterKey key,
                    StringBuilder sb, boolean multipleValueORConcat) {
                QueryParam searchStringParam = new QueryParam("searchString",
                        "%" + getValue().replaceAll("\\*","%").toUpperCase() + "%");
                QueryParam conceptNameValueParam = new QueryParam(
                        "conceptNameValue", key.getCode().getCodeValue());
                QueryParam conceptNameDesignatorParam = new QueryParam(
                        "conceptNameDesignator", key.getCode()
                                .getCodingSchemeDesignator());

                sb.append("EXISTS (");
                sb.append("FROM ContentItem content_item");
                sb.append(" INNER JOIN content_item.conceptName concept_name");
                sb.append(" WHERE (instance.sopInstanceUID = content_item.instance.sopInstanceUID)");
                sb.append(" AND (content_item.relationshipType = 'CONTAINS')");
                sb.append(" AND (upper(content_item.textValue) LIKE :"
                        + searchStringParam.getKey() + ")");
                sb.append(" AND (concept_name.codeValue = :"
                        + conceptNameValueParam.getKey() + ")");
                sb.append(" AND (concept_name.codingSchemeDesignator = :"
                        + conceptNameDesignatorParam.getKey() + ")");
                sb.append(")");

                return new QueryParam[] { searchStringParam,
                        conceptNameValueParam, conceptNameDesignatorParam };
            }
        };
    }

    
    public static TCQueryFilterValue<ITextOrCode> create(ITextOrCode...toc) {
        
        return new TCQueryFilterValue<ITextOrCode>(toc) {

            private static final long serialVersionUID = 1L;

            @Override
            public QueryParam[] appendSQLWhereConstraint(TCQueryFilterKey key,
                    StringBuilder sb, boolean multipleValueORConcat) {
            	List<QueryParam> params = new ArrayList<QueryParam>();
                QueryParam conceptNameValueParam = new QueryParam(
                        "conceptNameValue", key.getCode().getCodeValue());
                QueryParam conceptNameDesignatorParam = new QueryParam(
                        "conceptNameDesignator", key.getCode()
                                .getCodingSchemeDesignator());
                params.add(conceptNameValueParam);
                params.add(conceptNameDesignatorParam);
                
            	ITextOrCode[] toc = getValues();
                boolean joinCodes = toc!=null && key.supportsCodeValue();
                
                int nExists = 1;
                if (!multipleValueORConcat && toc.length>1)
                {
                	nExists = toc.length;
                }
                
                for (int n=0; n<nExists; n++)
                {
                	if (n>0)
                	{
                		sb.append(" AND ");
                	}
                	
                	sb.append("EXISTS (");
                	sb.append("FROM ContentItem content_item");
                	sb.append(" INNER JOIN content_item.conceptName concept_name");

                	if (joinCodes)
                	{
                		sb.append(" LEFT JOIN content_item.conceptCode concept_code");
                	}

                	sb.append(" WHERE (instance.sopInstanceUID = content_item.instance.sopInstanceUID)");
                	sb.append(" AND (content_item.relationshipType = 'CONTAINS')");
                	sb.append(" AND (concept_name.codeValue = :"
                			+ conceptNameValueParam.getKey() + ")");
                	sb.append(" AND (concept_name.codingSchemeDesignator = :"
                			+ conceptNameDesignatorParam.getKey() + ")");

                	if (toc!=null && toc.length>0)
                	{
                		sb.append(" AND (");
                		
                		int start = multipleValueORConcat ? 0 : n;
                		int end = multipleValueORConcat ? toc.length-1 : n;
                		
                		for (int i=start; i<=end; i++)
                		{
                			if (i>start) {
                				sb.append(" OR ");
                			}

                			ITextOrCode item = toc[i];
                			TCDicomCode code = item.getCode();
                			String text = item.getText();

                			sb.append("(");

                			if (code!=null)
                			{
                				QueryParam valueParam = new QueryParam("conceptCodeValue", code.getValue());
                				QueryParam designatorParam = new QueryParam("conceptCodeDesignator",code.getDesignator());
                				QueryParam valueTextParam = new QueryParam("valueText", code.getValue().toUpperCase());

                				sb.append("((concept_code.codeValue = :"
                						+ valueParam.getKey() + ")");
                				sb.append(" AND (concept_code.codingSchemeDesignator = :"
                						+ designatorParam.getKey() + "))");
                				sb.append(" OR (upper(content_item.textValue) LIKE :"
                						+ valueTextParam.getKey() + ")");

                				params.add(valueParam);
                				params.add(designatorParam);
                				params.add(valueTextParam);
                			}
                			else if (text!=null)
                			{
                				QueryParam param = new QueryParam("searchString",
                						"%" + text.replaceAll("\\*","%").toUpperCase() + "%");

                				sb.append("(upper(content_item.textValue) LIKE :"
                						+ param.getKey() + ")");

                				if (joinCodes)
                				{
                					sb.append(" OR (upper(concept_code.codeMeaning) LIKE :"
                							+ param.getKey() + ")");
                					sb.append(" OR (upper(concept_code.codeValue) LIKE :"
                							+ param.getKey() + ")");
                				}

                				params.add(param);
                			}

                			sb.append(")");
                		}
                		sb.append(")");
                	}
                	sb.append(")");
                }
                
                return params.toArray(new QueryParam[0]);
            }
        };
    }

    public static TCQueryFilterValue<DicomCodeEnum> create(DicomCodeEnum value) {
        
        return new TCQueryFilterValue<DicomCodeEnum>(value) {
            
            private static final long serialVersionUID = 1L;

            @Override
            public QueryParam[] appendSQLWhereConstraint(TCQueryFilterKey key,
                    StringBuilder sb, boolean multipleValueORConcat) {
                QueryParam conceptNameValueParam = new QueryParam(
                        "conceptNameValue", key.getCode().getCodeValue());
                QueryParam conceptNameDesignatorParam = new QueryParam(
                        "conceptNameDesignator", key.getCode()
                                .getCodingSchemeDesignator());
                QueryParam conceptCodeValueParam = new QueryParam(
                        "conceptCodeValue", getValue().getCode().getCodeValue());
                QueryParam conceptCodeDesignatorParam = new QueryParam(
                        "conceptCodeDesignator", getValue().getCode()
                                .getCodingSchemeDesignator());

                sb.append("EXISTS (");
                sb.append("FROM ContentItem content_item");
                sb.append(" INNER JOIN content_item.conceptName concept_name");
                sb.append(" INNER JOIN content_item.conceptCode concept_code");
                sb.append(" WHERE (instance.sopInstanceUID = content_item.instance.sopInstanceUID)");
                sb.append(" AND (content_item.relationshipType = 'CONTAINS')");
                sb.append(" AND (concept_name.codeValue = :"
                        + conceptNameValueParam.getKey() + ")");
                sb.append(" AND (concept_name.codingSchemeDesignator = :"
                        + conceptNameDesignatorParam.getKey() + ")");
                sb.append(" AND (concept_code.codeValue = :"
                        + conceptCodeValueParam.getKey() + ")");
                sb.append(" AND (concept_code.codingSchemeDesignator = :"
                        + conceptCodeDesignatorParam.getKey() + ")");
                sb.append(")");

                return new QueryParam[] { conceptNameValueParam,
                        conceptNameDesignatorParam, conceptCodeValueParam,
                        conceptCodeDesignatorParam };
            }
        };
    }

    public static TCQueryFilterValue<DicomStringEnum> create(
            DicomStringEnum value) {
        
        return new TCQueryFilterValue<DicomStringEnum>(value) {
            
            private static final long serialVersionUID = 1L;

            @Override
            public QueryParam[] appendSQLWhereConstraint(TCQueryFilterKey key,
                    StringBuilder sb, boolean multipleValueORConcat) {
                QueryParam searchStringParam = new QueryParam("searchString",
                        getValue().getString().toUpperCase());
                QueryParam conceptNameValueParam = new QueryParam(
                        "conceptNameValue", key.getCode().getCodeValue());
                QueryParam conceptNameDesignatorParam = new QueryParam(
                        "conceptNameDesignator", key.getCode()
                                .getCodingSchemeDesignator());

                sb.append("EXISTS (");
                sb.append("FROM ContentItem content_item");
                sb.append(" INNER JOIN content_item.conceptName concept_name");
                sb.append(" WHERE (instance.sopInstanceUID = content_item.instance.sopInstanceUID)");
                sb.append(" AND (content_item.relationshipType = 'CONTAINS')");
                sb.append(" AND (upper(content_item.textValue) = :"
                        + searchStringParam.getKey() + ")");
                sb.append(" AND (concept_name.codeValue = :"
                        + conceptNameValueParam.getKey() + ")");
                sb.append(" AND (concept_name.codingSchemeDesignator = :"
                        + conceptNameDesignatorParam.getKey() + ")");
                sb.append(")");

                return new QueryParam[] { searchStringParam,
                        conceptNameValueParam, conceptNameDesignatorParam };
            }
        };
    }
    
    public static TCQueryFilterValue<Date> create(Date from) {
    	return create(from, null);
    }
    
    @SuppressWarnings("serial")
	public static TCQueryFilterValue<Date> create(Date from, Date until) {
    	if (from==null) {
    		from = new Date(0);
    	}
    	if (until==null) {
    		until = new Date();
    	}
    	
        return new TCQueryFilterValue<Date>(from, until) {
            @Override
            public QueryParam[] appendSQLWhereConstraint(TCQueryFilterKey key,
                    StringBuilder sb, boolean multipleValueORConcat) {

            	Date from = getValues()[0];
            	Date until = getValues()[1];
            	
            	if (from.after(until)) {
            		Date tmp = from;
            		from = until;
            		until = tmp;
            	}
            	
            	Calendar calFrom = Calendar.getInstance();
            	calFrom.setTime(from);
                calFrom.set(Calendar.HOUR_OF_DAY, 0);
                calFrom.set(Calendar.MINUTE, 0);
                calFrom.set(Calendar.SECOND, 0);
                calFrom.set(Calendar.MILLISECOND, 0);
                
            	Calendar calUntil = Calendar.getInstance();
            	calUntil.setTime(until);
                calUntil.set(Calendar.HOUR_OF_DAY, 23);
                calUntil.set(Calendar.MINUTE, 59);
                calUntil.set(Calendar.SECOND, 59);
                calUntil.set(Calendar.MILLISECOND, 999);

                QueryParam fromParam = new QueryParam(
                		"fromDate", calFrom.getTime());
                QueryParam untilParam = new QueryParam(
                		"untilDate", calUntil.getTime());
                
                if (key.getDicomTag()==Tag.ContentDate) {
                	sb.append("(instance.contentDateTime BETWEEN :").append(fromParam.getKey())
                	.append(" AND :").append(untilParam.getKey()).append(")");
                	return new QueryParam[] { fromParam, untilParam };
                }
            	
            	return new QueryParam[0];
            }
        };
    }

    private static Code createCode(String designator, String value,
            String meaning) {
        DicomObject dataset = new BasicDicomObject();
        dataset.putString(Tag.CodingSchemeDesignator, VR.SH, designator);
        dataset.putString(Tag.CodeValue, VR.SH, value);
        dataset.putString(Tag.CodeMeaning, VR.LO, meaning == null ? ""
                : meaning);

        return new Code(dataset);
    }

    private static boolean equals(Code code1, Code code2) {
        return code1.getCodingSchemeDesignator().equals(
                code2.getCodingSchemeDesignator())
                && code1.getCodeValue().equals(code2.getCodeValue());
    }

    public static class QueryParam {
        private static final Random random = new Random();

        private String key;

        private Object value;

        public QueryParam(String key_prefix, Object value) {
            this.key = key_prefix + "_" + random.nextInt(Integer.MAX_VALUE);
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public Object getValue() {
            return value;
        }
    }

    private static interface DicomCodeEnum {
        public Code getCode();
    }

    private static interface DicomStringEnum {
        public String getString();
    }

    public static enum Level implements DicomCodeEnum {
        Primary("TCE201", "IHERADTF"), Intermediate("TCE202", "IHERADTF"), Advanced(
                "TCE203", "IHERADTF");

        private Code code;

        private Level(String v, String d) {
            code = createCode(d, v, null);
        }

        public Code getCode() {
            return code;
        }

        public static Level get(Code code) {
            if (code != null) {
                for (Level level : values()) {
                    if (TCQueryFilterValue.equals(code, level.getCode())) {
                        return level;
                    }
                }
            }

            return null;
        }
    }

    public static enum YesNo implements DicomCodeEnum {
        Yes("R-0038D", "SRT"), No("R-00339D", "SRT");

        private Code code;

        private YesNo(String v, String d) {
            code = createCode(d, v, null);
        }

        public Code getCode() {
            return code;
        }

        public static YesNo get(Code code) {
            if (code != null) {
                for (YesNo yesNo : values()) {
                    if (TCQueryFilterValue.equals(code, yesNo.getCode())) {
                        return yesNo;
                    }
                }
            }

            return null;
        }
    }

    public static enum PatientSex implements DicomStringEnum {
        Male("M"), Female("F"), Other("O"), Unknown("U");

        private String value;

        private PatientSex(String v) {
            this.value = v;
        }

        public String getString() {
            return value;
        }

        public static PatientSex get(String value) {
            if (value != null) {
                for (PatientSex sex : values()) {
                    if (value.equals(sex.getString())) {
                        return sex;
                    }
                }
            }

            return null;
        }
    }

    public static final class AcquisitionModality implements Serializable, DicomCodeEnum, DicomStringEnum 
    {
		private static final long serialVersionUID = 818651921486055373L;
		
		public static final AcquisitionModality CR = new AcquisitionModality("DCM", "CR");
        public static final AcquisitionModality CT = new AcquisitionModality("DCM", "CT");
        public static final AcquisitionModality DG = new AcquisitionModality("DCM", "DG");
        public static final AcquisitionModality DX = new AcquisitionModality("DCM", "DX");
        public static final AcquisitionModality ECG = new AcquisitionModality("DCM", "ECG");
        public static final AcquisitionModality ES = new AcquisitionModality("DCM", "ES");
        public static final AcquisitionModality GM = new AcquisitionModality("DCM", "GM");
        public static final AcquisitionModality IVUS = new AcquisitionModality("DCM", "IVUS");
        public static final AcquisitionModality MG = new AcquisitionModality("DCM", "MG");
        public static final AcquisitionModality MR = new AcquisitionModality("DCM", "MR");
        public static final AcquisitionModality NM = new AcquisitionModality("DCM", "NM");
        public static final AcquisitionModality PT = new AcquisitionModality("DCM", "PT");
        public static final AcquisitionModality RF = new AcquisitionModality("DCM", "RF");
        public static final AcquisitionModality RG = new AcquisitionModality("DCM", "RG");
        public static final AcquisitionModality SM = new AcquisitionModality("DCM", "SM");
        public static final AcquisitionModality US = new AcquisitionModality("DCM", "US");
        public static final AcquisitionModality XA = new AcquisitionModality("DCM", "XA");
        public static final AcquisitionModality XC = new AcquisitionModality("DCM", "XC");
        public static final AcquisitionModality OT = new AcquisitionModality("DCM", "OT");
        public static final AcquisitionModality SC = new AcquisitionModality("DCM", "SC");
        private static AcquisitionModality[] modalities = new AcquisitionModality[] {
        	CR,CT,DG,DX,ECG,ES,GM,IVUS,MG,MR,NM,OT,PT,RF,RG,SC,SM,US,XA,XC
        };


        private String value;
        private Code code;

        private AcquisitionModality(String value) {
            this.value = value;
        }
        
        private AcquisitionModality(String d, String v) {
            this.code = createCode(d, v, null);
        }
        
        public Code getCode() {
            return code;
        }
        
        public String getString()
        {
        	return value;
        }
        
        @Override
        public int hashCode() {
        	if (value!=null) {
        		return value.hashCode();
        	}
        	else if (code!=null) {
        		return code.hashCode();
        	}
        	return super.hashCode();
        }
        
        @Override
        public boolean equals(Object o)
        {
        	if (o instanceof AcquisitionModality)
        	{
        		AcquisitionModality m = (AcquisitionModality) o;
        		if (value!=null) {
        			return value.equals(m.value);
        		}
        		else if (code!=null && m.code!=null) {
        			return TCQueryFilterValue.equals(code, m.code);
        		}
        	}
        	
        	return super.equals(o);
        }
        
        @Override
        public String toString() {
        	if (value!=null) {
        		return value;
        	}
        	else if (code!=null) {
        		return code.getCodeValue();
        	}
        	return super.toString();
        }
        
        public TCQueryFilterValue<?> createFilterValue()
        {
        	if (value!=null) {
        		return TCQueryFilterValue.create((DicomStringEnum)this);
        	}
        	else if (code!=null) {
        		return TCQueryFilterValue.create((DicomCodeEnum)this);
        	}
        	return null;
        }
        
        public static AcquisitionModality[] values()
        {
        	return modalities;
        }
                
        public static AcquisitionModality get(String value) {
        	if (value!=null && !value.isEmpty())
        	{
                for (AcquisitionModality mod : modalities) {
                    if (mod.getCode()!=null && 
                    	value.equalsIgnoreCase(mod.getCode().getCodeValue())) 
                    {
                        return mod;
                    }
                }
        		return new AcquisitionModality(value);
        	}
        	return null;
        }

        public static AcquisitionModality get(Code code) {
            if (code != null) {
                for (AcquisitionModality mod : modalities) {
                    if (TCQueryFilterValue.equals(code, mod.getCode())) {
                        return mod;
                    }
                }
                return new AcquisitionModality( code.getCodeValue() );
            }

            return null;
        }
    }

    public static enum Purpose implements DicomCodeEnum {
        ForTeaching("TCE001", "IHERADTF"), ForClinicalTrial("TCE002",
                "IHERADTF"), ForResearch("TCE007", "IHERADTF"), ForPublication(
                "TCE008", "IHERADTF");

        private Code code;

        private Purpose(String v, String d) {
            code = createCode(d, v, null);
        }

        public Code getCode() {
            return code;
        }

        public static Purpose get(Code code) {
            if (code != null) {
                for (Purpose purpose : values()) {
                    if (TCQueryFilterValue.equals(code, purpose.getCode())) {
                        return purpose;
                    }
                }
            }

            return null;
        }
    }

    public static enum Category implements DicomCodeEnum {
        Musculoskeletal("TCE301", "IHERADTF"), Pulmonary("TCE302", "IHERADTF"), Cardiovascular(
                "TCE303", "IHERADTF"), Gastrointestinal("TCE304", "IHERADTF"), Genitourinary(
                "TCE305", "IHERADTF"), Neuro("TCE306", "IHERADTF"), Nuclear(
                "TCE308", "IHERADTF"), Ultrasound("TCE309", "IHERADTF"), VascularAndInterventional(
                "TCE307", "IHERADTF"), Pediatric("TCE310", "IHERADTF"), Breast(
                "TCE311", "IHERADTF");

        private Code code;

        private Category(String v, String d) {
            code = createCode(d, v, null);
        }

        public Code getCode() {
            return code;
        }

        public static Category get(Code code) {
            if (code != null) {
                for (Category category : values()) {
                    if (TCQueryFilterValue.equals(code, category.getCode())) {
                        return category;
                    }
                }
            }

            return null;
        }
    }


}