<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0" 
	xmlns:jdf="http://www.CIP4.org/JDFSchema_1_1" xmlns:bambi="www.cip4.org/Bambi" >

<!--  device processor -->
<xsl:template match="bambi:Processor">
<hr/>
<h2>Processor Status</h2> 

Processor Status:   <xsl:value-of select="@DeviceStatus"/> - Details: <xsl:value-of select="@DeviceStatusDetails"/> 
<xsl:if test="@NodeStatus">
<h2>Node Status</h2> 
Node Status:   <xsl:value-of select="@NodeStatus"/> - Details: <xsl:value-of select="@NodeStatusDetails"/> <br/>
QueueEntryID: <xsl:value-of select="@QueueEntryID"/><br/>
Node type: <xsl:value-of select="@Type"/><br/>
Description: <xsl:value-of select="@DescriptiveName"/><br/>
Start Time: <xsl:value-of select="@StartTime"/><br/>
Show JDF: <a><xsl:attribute name="href">../showJDF/<xsl:value-of select="../@DeviceID"/>?qeID=<xsl:value-of select="@QueueEntryID"/></xsl:attribute>
<xsl:value-of select="@QueueEntryID"/></a>



<h2>Resources:</h2>
<table>
<tr>
<th>Type</th><th>Phase Amount</th><th>Phase Waste</th><th>Total Amount</th><th>Total Waste</th>
</tr>

<xsl:apply-templates select="bambi:PhaseAmount"/>

</table>
</xsl:if>
</xsl:template>

<!--   ///////////////////////////////////////////////// -->

<xsl:template match="bambi:PhaseAmount">
<tr>
<td><xsl:value-of select="@ResourceName"/></td>
<td><xsl:value-of select="@PhaseAmount"/></td>
<td><xsl:value-of select="@PhaseWaste"/></td>
<td><xsl:value-of select="@TotalAmount"/></td>
<td><xsl:value-of select="@TotalWaste"/></td>
</tr>


</xsl:template>

</xsl:stylesheet>