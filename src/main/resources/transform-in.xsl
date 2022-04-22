<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:output method="xml" indent="yes"/>
	<xsl:strip-space elements="*"/>

	<xsl:template match="/root">
		<xsl:apply-templates select="pgrfa"/>
	</xsl:template>
	
	<xsl:template match="pgrfa">
		<xsl:element name="{operation}">
			<xsl:attribute name="username">
				<xsl:value-of select="/root/conf/glis_username"/>
			</xsl:attribute>
			<xsl:attribute name="password">
				<xsl:value-of select="/root/conf/glis_password"/>
			</xsl:attribute>
			<manager>
				<wiews><xsl:value-of select="hold_wiews"/></wiews>
				<pid><xsl:value-of select="hold_pid"/></pid>
				<name><xsl:value-of select="hold_name"/></name>
				<address><xsl:value-of select="hold_address"/></address>
				<country><xsl:value-of select="hold_country"/></country>
			</manager>
			<sampledoi><xsl:value-of select="sample_doi"/></sampledoi>
			<popid><xsl:value-of select="sample_id"/></popid>
			<date><xsl:value-of select="date"/></date>
			<genus><xsl:value-of select="genus"/></genus>
			<biostatus><xsl:value-of select="bio_status"/></biostatus>
			<species><xsl:value-of select="species"/></species>
			<spauth><xsl:value-of select="sp_auth"/></spauth>
			<subtaxa><xsl:value-of select="subtaxa"/></subtaxa>
			<stauth><xsl:value-of select="st_auth"/></stauth>
			<mlsstatus><xsl:value-of select="mls_status"/></mlsstatus>
			<occctry><xsl:value-of select="provenance"/></occctry>
			<historical><xsl:value-of select="historical"/></historical>
		    <targets>
				<xsl:for-each select="/root/target">
					<target>
						<value><xsl:value-of select="value"/></value>
						<xsl:variable name="tid" select="id"/>
						<kws>
							<xsl:for-each select="/root/tkw[target_id = $tid]">
								<kw><xsl:value-of select="value"/></kw>
							</xsl:for-each>
						</kws>
					</target>
				</xsl:for-each>
			</targets>

			<site>
				<loc><xsl:value-of select="coll_site"/></loc>
				<lat><xsl:value-of select="coll_lat"/></lat>
				<lon><xsl:value-of select="coll_lon"/></lon>
				<datum><xsl:value-of select="coll_datum"/></datum>
				<elevation><xsl:value-of select="coll_elevation"/></elevation>
				<siteprot><xsl:value-of select="siteprot"/></siteprot>
				<consaction><xsl:value-of select="consactions"/></consaction>
			</site>

			<xsl:if test="/root/actor[role = 'eh']">
				<exsitu>
					<xsl:for-each select="/root/actor[role = 'eh']">
						<holder>
							<wiews><xsl:value-of select="wiews"/></wiews>
							<pid><xsl:value-of select="pid"/></pid>
							<indherb><xsl:value-of select="indherb"/></indherb>
							<name><xsl:value-of select="name"/></name>
							<address><xsl:value-of select="address"/></address>
							<country><xsl:value-of select="country"/></country>
							<id><xsl:value-of select="identifier"/></id>
						</holder>
					</xsl:for-each>
				</exsitu>
			</xsl:if>
		</xsl:element>
	</xsl:template>
</xsl:stylesheet>