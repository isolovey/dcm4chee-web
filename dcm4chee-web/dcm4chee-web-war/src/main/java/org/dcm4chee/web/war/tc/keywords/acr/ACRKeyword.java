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
package org.dcm4chee.web.war.tc.keywords.acr;

import org.dcm4chee.web.dao.tc.TCDicomCode;
import org.dcm4chee.web.war.tc.keywords.TCKeyword;

/**
 * @author Bernhard Ableitinger <bernhard.ableitinger@agfa.com>
 * @version $Revision$ $Date$
 * @since May 30, 2011
 */
public class ACRKeyword extends TCKeyword {

    private static final long serialVersionUID = 1L;

    private static final String NAME_DELIMITER = " - ";

    private static final String VALUE_DELIMITER = ".";

    private static final String MEANING_DELIMITER = " - ";

    private TCKeyword anatomyKeyword;

    private TCKeyword pathologyKeyword;

    public ACRKeyword(TCKeyword anatomyKeyword, TCKeyword pathologyKeyword) {
        super(compositeNames(anatomyKeyword.getName(),
                pathologyKeyword.getName()), compositeCodes(
                anatomyKeyword.getCode(), pathologyKeyword.getCode()));
        this.anatomyKeyword = anatomyKeyword;
        this.pathologyKeyword = pathologyKeyword;
    }

    public TCKeyword getAnatomyKeyword() {
        return anatomyKeyword;
    }

    public TCKeyword getPathologyKeyword() {
        return pathologyKeyword;
    }

    public static String getAnatomyCodeValue(String value) {
        if (isValidCodeValue(value)) {
            TCDicomCode code = TCDicomCode.fromString(ACRCatalogue.getInstance()
                    .getDesignatorId(), value);
            String codeValue = code != null ? code.getValue() : null;

            if (codeValue != null) {
                if (codeValue.contains(VALUE_DELIMITER)) {
                    return codeValue.split("\\" + VALUE_DELIMITER)[0].trim();
                }

                return codeValue;
            }
        }

        return null;
    }

    public static String getPathologyCodeValue(String value) {
        if (isValidCodeValue(value)) {
            TCDicomCode code = TCDicomCode.fromString(ACRCatalogue.getInstance()
                    .getDesignatorId(), value);
            String codeValue = code != null ? code.getValue() : null;

            if (codeValue != null) {
                if (codeValue.contains(VALUE_DELIMITER)) {
                    return codeValue.split("\\" + VALUE_DELIMITER)[1].trim();
                }
            }
        }

        return null;
    }

    public static String getAnatomyKeywordName(String name) {
        if (name != null && name.contains(MEANING_DELIMITER)) {
            return name.split(MEANING_DELIMITER)[0].trim();
        }
        return name;
    }

    public static String getPathologyKeywordName(String name) {
        if (name != null && name.contains(MEANING_DELIMITER)) {
            return name.split(MEANING_DELIMITER)[1].trim();
        }
        return name;
    }

    private static String compositeNames(String name1, String name2) {
        StringBuffer sbuf = new StringBuffer();
        if (name1 != null) {
            sbuf.append(name1);
        }

        if (name2 != null) {
            if (sbuf.length() > 0) {
                sbuf.append(NAME_DELIMITER);
            }

            sbuf.append(name2);
        }

        return sbuf.toString();
    }

    private static TCDicomCode compositeCodes(TCDicomCode code1, TCDicomCode code2) {
        StringBuffer valueBuf = new StringBuffer();
        StringBuffer meaningBuf = new StringBuffer();
        String designatorId = null;

        if (code1 != null) {
            designatorId = code1.getDesignator();
            valueBuf.append(code1.getValue());

            if (code1.getMeaning() != null) {
                meaningBuf.append(code1.getMeaning());
            }
        }

        if (code2 != null) {
            if (designatorId == null) {
                designatorId = code2.getDesignator();
            }

            if (valueBuf.length() > 0) {
                valueBuf.append(VALUE_DELIMITER);
            }

            if (meaningBuf.length() > 0 && code2.getMeaning() != null) {
                meaningBuf.append(MEANING_DELIMITER);
            }

            valueBuf.append(code2.getValue());

            if (code2.getMeaning() != null) {
                meaningBuf.append(code2.getMeaning());
            }
        }

        if (designatorId != null && valueBuf.length() > 0) {
            return new TCDicomCode(designatorId, valueBuf.toString(),
                    meaningBuf.toString());
        }

        return null;
    }

    private static boolean isValidCodeValue(String value) {
        return value != null
                && value.matches("[0-9]+(\\" + VALUE_DELIMITER + "[0-9]+)?");
    }
}
