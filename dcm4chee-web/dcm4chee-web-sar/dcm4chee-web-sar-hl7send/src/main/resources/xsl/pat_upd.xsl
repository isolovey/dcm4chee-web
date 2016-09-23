<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:output indent="yes" method="xml"/>
    <xsl:include href="hl7send_common.xsl"/>
    
    <xsl:param name="MessageControlID">ReceivingFacility</xsl:param>
    <xsl:param name="MessageDateTime">ReceivingFacility</xsl:param>
    <xsl:param name="SendingApplication">SendingApplication</xsl:param>
    <xsl:param name="SendingFacility">SendingFacility</xsl:param>
    <xsl:param name="ReceivingApplication">ReceivingApplication</xsl:param>
    <xsl:param name="ReceivingFacility">ReceivingFacility</xsl:param>
    
    <xsl:template match="/dicom">
        <hl7>
            <MSH fieldDelimiter="|" componentDelimiter="^" repeatDelimiter="~" escapeDelimiter="\" subcomponentDelimiter="&amp;">
                <field><xsl:value-of select="$SendingApplication"/></field>
                <field><xsl:value-of select="$SendingFacility"/></field>
                <field><xsl:value-of select="$ReceivingApplication"/></field>
                <field><xsl:value-of select="$ReceivingFacility"/></field>
                <field><xsl:value-of select="$MessageDateTime"/></field> <!-- Date/time of Message -->
                <field/> <!-- Security -->
                <field>ADT<component>A08</component></field>
                <field><xsl:value-of select="$MessageControlID"/></field> <!-- Message Control ID -->
                <field>P</field>
                <field>2.3</field>
                <field/> <!-- Sequence Number -->
                <field/> <!-- Continuation Pointer -->
                <field/> <!-- Accept Acknowledgment Type -->
                <field/> <!-- Application Acknowledgment Type -->
                <field/> <!-- Country Code -->
                <field>8859/1</field>
            </MSH>
            <EVN>
                <field><xsl:text>A08</xsl:text></field>
                <field><xsl:value-of select="$MessageDateTime"/></field>
                <field/> <!-- DATE/TIME PLANNED EVENT -->
                <field/> <!-- EVENT REASON CODE -->
                <field/> <!-- OPERATOR ID -->
            </EVN>
            <PID>
                <field/>
                <field/>
                <field><xsl:value-of select="normalize-space(attr[@tag='00100020'])"/>
                    <component/>
                    <component/>
                    <component><xsl:value-of select="normalize-space(attr[@tag='00100021'])"/></component>
                </field>
                <field/>
                <field>
                    <xsl:call-template name="pn2xpn">
                        <xsl:with-param name="pn" select="normalize-space(attr[@tag='00100010'])"/>
                    </xsl:call-template>
                 </field>
                <field/>
                <field><xsl:value-of select="normalize-space(attr[@tag='00100030'])"/></field>
                <field><xsl:value-of select="normalize-space(attr[@tag='00100040'])"/></field>
            </PID>
        </hl7>
    </xsl:template>
        
</xsl:stylesheet>
