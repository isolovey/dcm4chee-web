<?xml version="1.0" encoding="UTF-8"?>
<html xsl:version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns="http://www.w3.org/1999/xhtml">
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
	</head>
	<body style="font-family:Arial;font-size:12pt;background-color:#444444">
		<xsl:variable name="thColor" select="'333333'"/>
		<xsl:variable name="tableColorA" select="'444444'"/>
		<xsl:variable name="tableColorB" select="'4C4C4C'"/>
		
		<xsl:variable name="instanceAccessed" select="'BBBBBB'"/>
		<xsl:variable name="instanceTransferred" select="'88AADD'"/>
		<xsl:variable name="studyDeleted" select="'FF8888'"/>
		<xsl:variable name="dataExport" select="'99DD99'"/>
		<xsl:variable name="dataImport" select="'88EE88'"/>
		<xsl:variable name="otherOperation" select="'FFFFFF'"/>
		
		<xsl:variable name="colorFailureMinor" select="'FF8888'"/>
		<xsl:variable name="colorFailureSerious" select="'FF9999'"/>
		<xsl:variable name="colorFailureMajor" select="'FF9F9F'"/>
		<xsl:variable name="empty_string"/>
		
		<table border="0" cellpadding="5" cellspacing="3" width="100%">
		  	<tr bgcolor="AAAAAA" align="left">
		  		<th><font color="{$thColor}">Date</font></th>
				<th><font color="{$thColor}">Action</font></th>
		  		<th><font color="{$thColor}">Operation</font></th>
		  		<th><font color="{$thColor}">User</font></th>
		  		<th><font color="{$thColor}">Participants</font></th>
		  		<th><font color="{$thColor}">Patient name (Patient ID)</font></th>
		  		<th><font color="{$thColor}">Studies</font></th>
		  		<th><font color="{$thColor}">Status</font></th>
		  	</tr>
		  	
			<xsl:for-each select="AuditMessages/AuditMessage">
				<xsl:sort select="EventIdentification/@EventDateTime" order="descending" />
			
				<xsl:variable name="fontcolor">
					<xsl:choose>
						<xsl:when test="EventIdentification/EventID/@code='110103'">
							<!--DICOM Instance Accessed -->
							<xsl:value-of select="$instanceAccessed"/>
						</xsl:when>	
						<xsl:when test="EventIdentification/EventID/@code='110104'">
							<!--DICOM Instance Transferred -->
							<xsl:value-of select="$instanceTransferred"/>
						</xsl:when>
						<xsl:when test="EventIdentification/EventID/@code='110105'">
							<!--DICOM Study Deleted -->
							<xsl:value-of select="$studyDeleted"/>
						</xsl:when>
						<xsl:when test="EventIdentification/EventID/@code='110106'">
							<!--Data Export -->
							<xsl:value-of select="$dataExport"/>
						</xsl:when>
						<xsl:when test="EventIdentification/EventID/@code='110107'">
							<!--Data Import -->
							<xsl:value-of select="$dataImport"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="$otherOperation"/>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:variable>
			    
				<xsl:variable name="bgColor">
					<xsl:choose>
						<xsl:when test="position() mod 2 = 1">
							<xsl:value-of select="$tableColorA"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="$tableColorB"/>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:variable>
			
				
				
				<tr bgcolor="{$bgColor}">
					<td>
						<!-- D A T E "2010-04-01T13:21:20.96+02:00" -->
						<xsl:variable name="dateComplete" select="EventIdentification/@EventDateTime"/>
						
						<xsl:variable name="date" select="substring-before($dateComplete, 'T')" />
						
						<xsl:variable name="year" select="substring-before($date, '-')" />
						<xsl:variable name="yearRemain" select="substring-after($date, '-')" />
						<xsl:variable name="month" select="substring-before($yearRemain, '-')" />
						<xsl:variable name="day" select="substring-after($yearRemain, '-')" />
						
					    <xsl:variable name="timeComplete" select="substring-after($dateComplete, 'T')" />
						<xsl:variable name="time" select="substring($timeComplete, 1, 8)" />
					    
						<xsl:variable name="monthLong">
							<xsl:choose>
								<xsl:when test="$month='01'">
									<xsl:value-of select="'January'"/>
								</xsl:when>
								<xsl:when test="$month='02'">
									<xsl:value-of select="'February'"/>
								</xsl:when>
								<xsl:when test="$month='03'">
									<xsl:value-of select="'March'"/>
								</xsl:when>
								<xsl:when test="$month='04'">
									<xsl:value-of select="'April'"/>
								</xsl:when>
								<xsl:when test="$month='05'">
									<xsl:value-of select="'May'"/>
								</xsl:when>
								<xsl:when test="$month='06'">
									<xsl:value-of select="'June'"/>
								</xsl:when>
								<xsl:when test="$month='07'">
									<xsl:value-of select="'July'"/>
								</xsl:when>
								<xsl:when test="$month='08'">
									<xsl:value-of select="'August'"/>
								</xsl:when>
								<xsl:when test="$month='09'">
									<xsl:value-of select="'September'"/>
								</xsl:when>
								<xsl:when test="$month='10'">
									<xsl:value-of select="'October'"/>
								</xsl:when>
								<xsl:when test="$month='11'">
									<xsl:value-of select="'November'"/>
								</xsl:when>
								<xsl:when test="$month='12'">
									<xsl:value-of select="'December'"/>
								</xsl:when>
							</xsl:choose>
						</xsl:variable>
						
						<nobr>
							<xsl:value-of select="$monthLong" /> <font>&#8201;</font>	
							<xsl:value-of select="$day" /> <font>,&#8201;</font> 
							<xsl:value-of select="$year" />
						</nobr>
						<br />
						<xsl:value-of select="$time" />
					</td>
					<td>
						<!-- E V E N T   A C T I O N -->
						<xsl:choose>
							<xsl:when test="EventIdentification/@EventActionCode='C'">
					        	<font color="{$fontcolor}">Create</font>
					        </xsl:when>
							<xsl:when test="EventIdentification/@EventActionCode='R'">
					        	<font color="{$fontcolor}">Read</font>
					        </xsl:when>
							<xsl:when test="EventIdentification/@EventActionCode='U'">
					        	<font color="{$fontcolor}">Update</font>
					        </xsl:when>
							<xsl:when test="EventIdentification/@EventActionCode='D'">
					        	<font color="{$fontcolor}">Delete</font>
					        </xsl:when>
							<xsl:when test="EventIdentification/@EventActionCode='E'">
					        	<font color="{$fontcolor}">Execute</font>
					        </xsl:when>
						</xsl:choose>
					</td>
					<td>
						<!-- O P E R A T I O N -->
						<xsl:choose>
							<xsl:when test="EventIdentification/EventID/@code='110103'">
					        	<font color="{$fontcolor}">DICOM Instances Accessed</font>
					        </xsl:when>
					        <xsl:when test="EventIdentification/EventID/@code='110104'">
					        	<font color="{$fontcolor}">DICOM Instances Transferred</font>
					        </xsl:when>
							<xsl:when test="EventIdentification/EventID/@code='110105'">
					        	<font color="{$fontcolor}">DICOM Study Deleted</font>
					        </xsl:when>
					        <xsl:when test="EventIdentification/EventID/@code='110106'">
					        	<font color="{$fontcolor}">Data Export</font>
					        </xsl:when>
					        <xsl:when test="EventIdentification/EventID/@code='110107'">
					        	<font color="{$fontcolor}">Data Import</font>
					        </xsl:when>
					        <xsl:otherwise>
								<xsl:choose>
									<xsl:when test="EventIdentification/EventID/@displayName">
						        		<xsl:value-of select="EventIdentification/EventID/@displayName"/>
						        	</xsl:when>
						        	<xsl:otherwise>
						          		<xsl:value-of select="EventIdentification/EventID/@code"/>
						        	</xsl:otherwise>
								</xsl:choose>
					        </xsl:otherwise>
						</xsl:choose>
						<!-- EventTypeCode  -->
						<xsl:if test="EventIdentification/EventTypeCode/@code">
							<br/>
						</xsl:if>
						<xsl:choose>
							<xsl:when test="EventIdentification/EventTypeCode/@code='110120'">
					        	<font color="{$fontcolor}">(Application Start)</font>
					        </xsl:when>
							<xsl:when test="EventIdentification/EventTypeCode/@code='110121'">
					        	<font color="{$fontcolor}">(Application Stop)</font>
					        </xsl:when>
					        <xsl:when test="EventIdentification/EventTypeCode/@code='110122'">
					        	<font color="{$fontcolor}">(Login)</font>
					        </xsl:when>
					        <xsl:when test="EventIdentification/EventTypeCode/@code='110123'">
					        	<font color="{$fontcolor}">(Logout)</font>
					        </xsl:when>
					        <xsl:when test="EventIdentification/EventTypeCode/@code='110124'">
					        	<font color="{$fontcolor}">(Attach)</font>
					        </xsl:when>
					        <xsl:when test="EventIdentification/EventTypeCode/@code='110125'">
					        	<font color="{$fontcolor}">(Detach)</font>
					        </xsl:when>
					        <xsl:when test="EventIdentification/EventTypeCode/@code='110126'">
					        	<font color="{$fontcolor}">(Node Authentication)</font>
					        </xsl:when>
					        <xsl:when test="EventIdentification/EventTypeCode/@code='110127'">
					        	<font color="{$fontcolor}">(Emergency Override)</font>
					        </xsl:when>
					        <xsl:when test="EventIdentification/EventTypeCode/@code='110128'">
					        	<font color="{$fontcolor}">(Network Configuration)</font>
					        </xsl:when>
					        <xsl:when test="EventIdentification/EventTypeCode/@code='110129'">
					        	<font color="{$fontcolor}">(Security Configuration)</font>
					        </xsl:when>
					        <xsl:when test="EventIdentification/EventTypeCode/@code='110130'">
					        	<font color="{$fontcolor}">(Hardware Configuration)</font>
					        </xsl:when>
					        <xsl:when test="EventIdentification/EventTypeCode/@code='110131'">
					        	<font color="{$fontcolor}">(Software Configuration)</font>
					        </xsl:when>
					        <xsl:when test="EventIdentification/EventTypeCode/@code='110132'">
					        	<font color="{$fontcolor}">(Use of Restricted Function)</font>
					        </xsl:when>
					        <xsl:when test="EventIdentification/EventTypeCode/@code='110133'">
					        	<font color="{$fontcolor}">(Audit Recording Stopped)</font>
					        </xsl:when>
					        <xsl:when test="EventIdentification/EventTypeCode/@code='110134'">
					        	<font color="{$fontcolor}">(Audit Recording Started)</font>
					        </xsl:when>
					        <xsl:when test="EventIdentification/EventTypeCode/@code='110135'">
					        	<font color="{$fontcolor}">(Object Security Attributes Changed)</font>
					        </xsl:when>
					        <xsl:when test="EventIdentification/EventTypeCode/@code='110136'">
					        	<font color="{$fontcolor}">(Security Roles Changed)</font>
					        </xsl:when>
					        <xsl:when test="EventIdentification/EventTypeCode/@code='110137'">
					        	<font color="{$fontcolor}">(User security Attributes Changed)</font>
					        </xsl:when>
						</xsl:choose>
					</td>
					<td>
						<!-- U S E R -->
						<xsl:for-each select="ActiveParticipant">
							<xsl:if test="not(@UserIsRequestor='false')">
	                            <xsl:value-of select="@UserID"/>
	                            <xsl:if test="@AlternativeUserID">
		                            <i><xsl:text> (</xsl:text>
		                            <xsl:value-of select="@AlternativeUserID"/>
		                            <xsl:text>)</xsl:text></i>
	                            </xsl:if><br/>
							</xsl:if>
						</xsl:for-each>	
					</td>
					<td width="20%">
						<!-- H O S T -->
						<xsl:for-each select="ActiveParticipant">
							<xsl:if test="@UserIsRequestor='false'">
	                            <xsl:value-of select="@UserID"/>
	                            <xsl:if test="@AlternativeUserID">
		                            <i><xsl:text> (</xsl:text>
		                            <xsl:value-of select="@AlternativeUserID"/>
		                            <xsl:text>)</xsl:text></i>
	                            </xsl:if><br/>
							</xsl:if>
						</xsl:for-each>	
					</td>
					<td>
						<!-- P A T I E N T   N A M E -->
						<!-- LastName^FirstName^MiddleName^NamePrefix^NameSuffix -->
						<xsl:variable name="numPatients" select="count(ParticipantObjectIdentification[@ParticipantObjectTypeCodeRole='1'])"/>
						<xsl:variable name="numStudies" select="count(ParticipantObjectIdentification[@ParticipantObjectTypeCodeRole='3'])"/>
						<xsl:variable name="studyUIDs">
                            <xsl:for-each select="ParticipantObjectIdentification[@ParticipantObjectTypeCode='2']/@ParticipantObjectID">                            
                                     <xsl:value-of select="."/> 
                                      <xsl:if test="position() != last()">
                                        <xsl:text>,</xsl:text>
                                      </xsl:if>
                            </xsl:for-each>      
                        </xsl:variable>
                        
						<xsl:for-each select="ParticipantObjectIdentification">
							<xsl:if test="@ParticipantObjectTypeCodeRole='1'">
							<nobr>
								<xsl:if test="$numPatients &gt; 1">
									<xsl:if test="position() &gt; 1">
										<br/>
									</xsl:if>
									<xsl:value-of select="position()"/><font>.&#8201;</font>
								</xsl:if>

								<!-- Extract Patient Name -->
							    <xsl:variable name="list" select="ParticipantObjectName"/> 
							    <xsl:variable name="newlist" select="concat(normalize-space($list), ' ')" />
							    
							    <xsl:variable name="lastName" select="substring-before($newlist, '^')" />
							    <xsl:variable name="remaining" select="substring-after($newlist, '^')" />
								
							    <xsl:variable name="firstName">
									<xsl:choose>
										<xsl:when test="contains($remaining, '^')">
											<xsl:value-of select="substring-before($remaining, '^')"/>
										</xsl:when>
										<xsl:otherwise>
											<xsl:value-of select="$remaining"/>
										</xsl:otherwise>
									</xsl:choose>
								</xsl:variable>
							    <xsl:variable name="remaining2" select="substring-after($remaining, '^')" />
								
								<xsl:variable name="middleName">
									<xsl:choose>
										<xsl:when test="contains($remaining2, '^')">
											<xsl:value-of select="substring-before($remaining2, '^')"/>
										</xsl:when>
										<xsl:otherwise>
											<xsl:value-of select="$remaining2"/>
										</xsl:otherwise>
									</xsl:choose>
								</xsl:variable>
								<xsl:variable name="remaining3" select="substring-after($remaining2, '^')" />
								
								<xsl:variable name="namePrefix">
									<xsl:choose>
										<xsl:when test="contains($remaining3, '^')">
											<xsl:value-of select="substring-before($remaining3, '^')"/>
										</xsl:when>
										<xsl:otherwise>
											<xsl:value-of select="$remaining3"/>
										</xsl:otherwise>
									</xsl:choose>
								</xsl:variable>
								<xsl:variable name="remaining4" select="substring-after($remaining3, '^')" />
								
								<xsl:variable name="nameSuffix">
									<xsl:choose>
										<xsl:when test="contains($remaining4, '^')">
											<xsl:value-of select="substring-before($remaining4, '^')"/>
										</xsl:when>
										<xsl:otherwise>
											<xsl:value-of select="$remaining4"/>
										</xsl:otherwise>
									</xsl:choose>
								</xsl:variable>
								
								<!-- Display Patient Name -->
								<a color="EEEEEE"  href="open://{$studyUIDs}">
								<xsl:value-of select="$lastName" />
								<xsl:if test="string($firstName)">
										<font>, </font>
										<xsl:value-of select="$firstName" />
								</xsl:if>
								<xsl:if test="string($middleName)">
										<font>, </font>
										<xsl:value-of select="$middleName" />
								</xsl:if>
								<xsl:if test="string($namePrefix)">
										<font>, </font>
										<xsl:value-of select="$namePrefix" />
								</xsl:if>
							
								<!-- P A T I E N T   I D -->
								<xsl:variable name="fullName" select="@ParticipantObjectID"/>
								<xsl:variable name="firstPart" select="substring-before($fullName, '^')"/>
								<font>&#9;(</font>
								<xsl:choose>
									<xsl:when test="normalize-space($firstPart) != $empty_string">	
										<xsl:value-of select="$firstPart"/>
									</xsl:when>
									<xsl:otherwise>
										<xsl:value-of select="$fullName"/>
									</xsl:otherwise>
								</xsl:choose>
								<font>)</font></a>
								</nobr>
							</xsl:if>
						</xsl:for-each>
					</td>
					<td align="center">
						<!-- N U M B E R   S T U D I E S -->
						<xsl:variable name="numberStudies" select="count(ParticipantObjectIdentification[@ParticipantObjectTypeCodeRole='3'])"/>
						<font color="{$instanceAccessed}">
								<xsl:value-of select="$numberStudies"/>
						</font>
					</td>
					<td>
						<!-- S T A T U S -->
						<xsl:choose>
							<xsl:when test="EventIdentification/@EventOutcomeIndicator='0'">
								Success
							</xsl:when>
							<xsl:when test="EventIdentification/@EventOutcomeIndicator='4'">
								<font color="{$colorFailureMinor}"><b>Failure</b>&#8201;</font> <font>(minor)</font> 
								<!-- br></br> <font>(action restarted)</font -->
							</xsl:when>
							<xsl:when test="EventIdentification/@EventOutcomeIndicator='8'">
								<font color="{$colorFailureSerious}"><b>Failure</b>&#8201;</font> <font>(serious)</font> 
								<!-- br></br> <font>(action terminated)</font -->
							</xsl:when>
							<xsl:when test="EventIdentification/@EventOutcomeIndicator='12'">
								<font color="{$colorFailureMajor}"><b>Failure</b>&#8201;</font> <font>(major)</font> 
								<!-- br></br> <font>(action made unavailable)</font -->
							</xsl:when>
						</xsl:choose>
					</td>
				</tr>
			</xsl:for-each>  	
		</table>
	</body>
</html>