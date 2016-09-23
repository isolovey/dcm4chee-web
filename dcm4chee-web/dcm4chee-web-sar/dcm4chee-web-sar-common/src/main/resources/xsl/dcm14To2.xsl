<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
    <xsl:output method="xml" indent="yes"/>
    <xsl:template match="/">
        <xsl:apply-templates select="/dataset"/>
    </xsl:template>
    <xsl:template match="dataset"> 
        <dicom>
                <xsl:copy-of select="./*"/>
       </dicom>
    </xsl:template>
</xsl:stylesheet>