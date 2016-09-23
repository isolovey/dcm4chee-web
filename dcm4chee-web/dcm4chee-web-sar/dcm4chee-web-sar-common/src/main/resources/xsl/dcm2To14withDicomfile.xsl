<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
    <xsl:output method="xml" indent="yes"/>
    <xsl:template match="/dicom">
        <dicomfile>
            <dataset>
                <xsl:copy-of select="./*"/>
            </dataset>
       </dicomfile>
    </xsl:template>
</xsl:stylesheet>
