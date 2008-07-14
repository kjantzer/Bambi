<?xml version="1.0" encoding="UTF-8" standalone="no" ?>
<xsl:stylesheet version="1.0" xmlns="http://www.w3.org/1999/xhtml" xmlns:bambi="www.cip4.org/Bambi" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:strip-space elements="*"/>
  <xsl:output method="html"/>
  
<xsl:template match="/DeviceList">  
<html>
   <xsl:variable name="context" select="@Context"/>
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1"/>
	<link rel="stylesheet" type="text/css">
		<xsl:attribute name="href"><xsl:value-of select="$context"/>/css/styles_pc.css</xsl:attribute>  
		</link>
		<link rel="icon" type="image/x-icon">
		<xsl:attribute name="href"><xsl:value-of select="$context"/>/favicon.ico</xsl:attribute>  
		</link>
		<title><xsl:value-of select="@DeviceType"/> - Overview</title>
	</head>
	<body>
		<img src="logo.gif" height="70" alt="logo"/>
		<h1><xsl:value-of select="@DeviceType"/> - Overview</h1>
		
		<h3>Root Controller:</h3>
		<table cellspacing="2" border="1">
			<tr>
				<th align="left"> Controller ID </th>
				<th align="left"> Controller Type </th>
				<th align="left"> Controller Status </th>
				<th align="left"> Controller URL </th>
			</tr>
		
    <xsl:apply-templates select="XMLDevice[@Root='true']"/>
		</table>
		
		<br/>
		<h3>Known Devices:</h3>
		<table cellspacing="2" border="1">
			<tr>
				<th align="left"> Device ID </th>
				<th align="left"> Device Type </th>
				<th align="left"> Device Status </th>
				<th align="left"> Device URL </th>
			</tr>
		
    <xsl:apply-templates select="XMLDevice[@Root='false']"/>
		</table>
		
		<p>Build @build.number@, @build.timestamp@</p>
	</body>
</html>
</xsl:template>  

<xsl:template match="XMLDevice[@Root='true']">  
			<tr>
				<td align="left"><xsl:value-of select="@DeviceID"/></td>
				<td align="left"><xsl:value-of select="@DeviceType"/></td>
				<td align="left"><xsl:value-of select="@DeviceStatus"/></td>
				<td align="left"><xsl:value-of select="@DeviceURL"/></td>
			</tr>

</xsl:template>  

<xsl:template match="XMLDevice[@Root='false']">  
			<tr>
				<td align="left">
				<a><xsl:attribute name="href">./showDevice/<xsl:value-of select="@DeviceID"/></xsl:attribute>
				<xsl:value-of select="@DeviceID"/></a>
				</td>
				<td align="left"><xsl:value-of select="@DeviceType"/></td>
				<td align="left"><xsl:value-of select="@DeviceStatus"/></td>
				<td align="left"><xsl:value-of select="@DeviceURL"/></td>
			</tr>
</xsl:template>  

</xsl:stylesheet>  
