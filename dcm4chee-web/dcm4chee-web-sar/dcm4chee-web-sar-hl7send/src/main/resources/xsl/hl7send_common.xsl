<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

    <xsl:template name="pn2xpn">
        <xsl:param name="pn"/>
        <xsl:variable name="fn" select="substring-before($pn,'^')"/>
        <xsl:choose>
            <xsl:when test="$fn">
                <xsl:value-of select="$fn"/>
                <component>
                    <xsl:variable name="wofn" select="substring-after($pn,'^')"/>
                    <xsl:variable name="vn" select="substring-before($wofn,'^')"/>
                    <xsl:choose>
                        <xsl:when test="$vn">
                            <xsl:value-of select="$vn"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="$wofn"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </component>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$pn"/>
            </xsl:otherwise>
        </xsl:choose>             
    </xsl:template>
    
</xsl:stylesheet>
