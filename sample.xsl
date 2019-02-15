<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:output method="xml" indent="yes"/>

	<xsl:template match="/root">
		<xsl:apply-templates select="pgrfas"/>
	</xsl:template>

	<xsl:template match="pgrfas">
		<register username="toolkit" password="Passw0rd">
			<location>
				<wiews><xsl:value-of select="hold_wiews"/></wiews>
				<pid><xsl:value-of select="hold_pid"/></pid>
				<name><xsl:value-of select="hold_name"/></name>
				<address><xsl:value-of select="hold_address"/></address>
				<country><xsl:value-of select="hold_country"/></country>
			</location>
			<xsl:if test="operation = 'update'">
				<sampledoi><xsl:value-of select="sample_doi"/></sampledoi>
			</xsl:if>
			<sampleid><xsl:value-of select="sample_id"/></sampleid>
			<date><xsl:value-of select="date"/></date>
			<method><xsl:value-of select="method"/></method>
			<genus><xsl:value-of select="genus"/></genus>
			<biostatus><xsl:value-of select="biostatus"/></biostatus>
			<species><xsl:value-of select="species"/></species>
			<spauth><xsl:value-of select="spauth"/></spauth>
			<subtaxa><xsl:value-of select="subtaxa"/></subtaxa>
			<stauth><xsl:value-of select="stauth"/></stauth>
			<mlsstatus><xsl:value-of select="mlsstatus"/></mlsstatus>
			<historical><xsl:value-of select="historical"/></historical>
			<cropnames>
				<xsl:for-each select="/root/names[name_type = 'cn']">
					<name><xsl:value-of select="name"/></name>
				</xsl:for-each>
			</cropnames>
			<names>
				<xsl:for-each select="/root/names[name_type = 'on']">
					<name><xsl:value-of select="name"/></name>
				</xsl:for-each>
		   </names>
		   <targets>
				<xsl:for-each select="/root/targets">
					<target>
						<value><xsl:value-of select="value"/></value>
						<xsl:variable name="tid" select="id"/>
						<kws>
							<xsl:for-each select="/root/tkws[target_id = $tid]">
								<kw><xsl:value-of select="value"/></kw>
							</xsl:for-each>
						</kws>
					</target>
				</xsl:for-each>
			</targets>
			<progdoi>
				<xsl:for-each select="/root/progdois">
				   <doi><xsl:value-of select="doi"/></doi>
				</xsl:for-each>
			</progdoi>
			<ids>
				<xsl:for-each select="/root/identifiers">
					<id type="{type}"><xsl:value-of select="value"/></id>
				</xsl:for-each>
			</ids>

			<xsl:if test="/root/actors[role = 'pr']">
				<acquisition>
					<provider>
						<wiews><xsl:value-of select="wiews"/></wiews>
						<pid><xsl:value-of select="pid"/></pid>
						<name><xsl:value-of select="name"/></name>
						<address><xsl:value-of select="address"/></address>
						<country><xsl:value-of select="country"/></country>
					</provider>
					<sampleid><xsl:value-of select="prov_sid"/></sampleid>
					<provenance><xsl:value-of select="provenance"/></provenance>
				</acquisition>
			</xsl:if>

			<collection>
				<xsl:if test="/root/actors[role = 'co']">
					<collectors>
						<xsl:for-each select="/actors[role = 'co']">
							<collector>
								<wiews><xsl:value-of select="wiews"/></wiews>
								<pid><xsl:value-of select="pid"/></pid>
								<name><xsl:value-of select="name"/></name>
								<address><xsl:value-of select="address"/></address>
								<country><xsl:value-of select="country"/></country>
							</collector>
						 </xsl:for-each>
					</collectors>
				</xsl:if>
				<sampleid><xsl:value-of select="coll_sid"/></sampleid>
				<missid><xsl:value-of select="coll_miss_id"/></missid>
				<site><xsl:value-of select="coll_site"/></site>
				<lat><xsl:value-of select="coll_lat"/></lat>
				<lon><xsl:value-of select="coll_lon"/></lon>
				<uncert><xsl:value-of select="coll_uncert"/></uncert>
				<datum><xsl:value-of select="coll_datum"/></datum>
				<georef><xsl:value-of select="coll_georef"/></georef>
				<elevation><xsl:value-of select="coll_elevation"/></elevation>
				<date><xsl:value-of select="coll_date"/></date>
				<source><xsl:value-of select="coll_source"/></source>
			</collection>

			<breeding>
				<xsl:if test="/root/actors[role = 'br']">
					<breeders>
						<xsl:for-each select="/actors[role = 'co']">
							<breeder>
								<wiews><xsl:value-of select="wiews"/></wiews>
								<pid><xsl:value-of select="pid"/></pid>
								<name><xsl:value-of select="name"/></name>
								<address><xsl:value-of select="address"/></address>
								<country><xsl:value-of select="country"/></country>
							</breeder>
						</xsl:for-each>
					</breeders>
				</xsl:if>
				<ancestry><xsl:value-of select="ancestry"/></ancestry>
			</breeding>
		</register>
	</xsl:template>
</xsl:stylesheet>