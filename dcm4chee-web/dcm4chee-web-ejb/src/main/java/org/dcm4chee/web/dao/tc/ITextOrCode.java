package org.dcm4chee.web.dao.tc;

import java.io.Serializable;


public interface ITextOrCode extends Serializable {
    public String getText();
    public TCDicomCode getCode();
    public String toShortString();
    public String toLongString();
}