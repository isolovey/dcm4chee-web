package org.dcm4chee.web.dao.tc;

import java.io.Serializable;

import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.VR;
import org.dcm4chee.archive.entity.Code;

@SuppressWarnings("serial")
public class TCDicomCode implements Serializable {
	
	public static final TCDicomCode REF_COMPOSITE_CONTAINER =
			new TCDicomCode("DCM4CHEE-TC","TF1100","Referenced Composite Container");
	
	public static final TCDicomCode REF_COMPOSITE_OBJECT =
			new TCDicomCode("DCM4CHEE-TC","TF1101","Referenced Composite Object");

	public static final TCDicomCode REF_COMPOSITE_RELATIONSHIP_TYPE =
			new TCDicomCode("DCM4CHEE-TC","TF1102","Referenced Composite Relationship Type");

	public static final TCDicomCode REF_COMPOSITE_COMMENT =
			new TCDicomCode("DCM4CHEE-TC","TF1103","Referenced Composite Comment");

    private String value;

    private String designator;

    private String meaning;

    private String version;

    public TCDicomCode(String designator, String value, String meaning,
            String version) {
        this.value = value;
        this.designator = designator;
        this.meaning = meaning;
        this.version = version;
    }

    public TCDicomCode(String designator, String value, String meaning) {
        this(designator, value, meaning, null);
    }

    public TCDicomCode(DicomObject dataset) {
        this.value = dataset.getString(Tag.CodeValue);
        this.designator = dataset.getString(Tag.CodingSchemeDesignator);
        this.meaning = dataset.getString(Tag.CodeMeaning);
        this.version = dataset.getString(Tag.CodingSchemeVersion);
    }

    public static TCDicomCode fromString(String designator, String s) {
        if (designator != null && s != null) {
            if (s.startsWith("(")) {
                String value = s.substring(1, s.indexOf(")"));
                String meaning = s.substring(s.indexOf(")") + 2);

                return new TCDicomCode(designator, value, meaning);
            } else {
                return new TCDicomCode(designator, s, null);
            }
        }

        return null;
    }

    public String getValue() {
        return value;
    }

    public String getDesignator() {
        return designator;
    }

    public String getMeaning() {
        return meaning;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        StringBuffer sbuf = new StringBuffer();

        if (meaning != null) {
            sbuf.append("(");
        }

        sbuf.append(value);

        if (meaning != null) {
            sbuf.append(") ");
            sbuf.append(meaning);
        }

        return sbuf.toString();
    }
    
    public String toShortString() {
        if (meaning!=null)
        {
        	return meaning;
        }
        return value;
    }

    public Code toCode() {
        DicomObject dataset = new BasicDicomObject();
        dataset.putString(Tag.CodingSchemeDesignator, VR.SH, designator);
        dataset.putString(Tag.CodeValue, VR.SH, value);
        dataset.putString(Tag.CodeMeaning, VR.LO, meaning == null ? ""
                : meaning);

        if (version != null) {
            dataset.putString(Tag.CodingSchemeVersion, null, version);
        }

        return new Code(dataset);
    }

    public boolean equals(Code code) {
        if (code != null) {
            return value.equals(code.getCodeValue())
                    && designator.equals(code.getCodingSchemeDesignator())
                    && (version == null || version.equals(code
                            .getCodingSchemeVersion()));
        }

        return false;
    }
    @Override
    public boolean equals(Object o) {
        if (o != null && (o instanceof TCDicomCode)) {
            TCDicomCode code = (TCDicomCode) o;
            return value.equals(code.value)
                    && designator.equals(code.designator)
                    && (version == null || version.equals(code.version));
        }

        return false;
    }
    @Override
    public int hashCode() {
        return (value+"_"+designator+"_"+version).hashCode();
    }
}