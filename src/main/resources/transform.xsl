<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:template match="/">
    <register username="[username]" password="[password]">

      <xsl:variable name="provider" select="/root/actor[role='pr']"/>
      <xsl:copy-of select="$provider"/>

      <location>
        <wiews>[lwiews]</wiews>
        <pid>[lpid]</pid>
        <name>[lname]</name>
        <address>[laddress]</address>
        <country>[lcountry]</country>
      </location>

      <sampledoi>[sampledoi]</sampledoi>
      <sampleid>[sampleid]</sampleid>
      <date>[date]</date>
      <method>[method]</method>
      <genus>[genus]</genus>
      <cropnames>
        <name>[cropname]</name>
      </cropnames>

      <targets>
         <target>
           <value>[tvalue]</value>
           <kws>
             <kw>[tkw]</kw>
           </kws>
         </target>
      </targets>

      <progdoi>
        <doi>[progdoi]</doi>
      </progdoi>

      <biostatus>[biostatus]</biostatus>
      <species>[species]</species>
      <spauth>[spauth]</spauth>
      <subtaxa>[subtaxa]</subtaxa>
      <stauth>[stauth]</stauth>

      <names>
        <name>[nvalue]</name>
      </names>

      <ids>
        <id type="[itype]">[ivalue]</id>
      </ids>

      <mlsstatus>[mlsstatus]</mlsstatus>
      <historical>[hist]</historical>

      <acquisition>
        <provider>
          <wiews>[pwiews]</wiews>
          <pid>[ppid]</pid>
          <name>[pname]</name>
          <address>[paddress]</address>
          <country>[pcountry]</country>
        </provider>
        <sampleid>[psampleid]</sampleid>
        <provenance>[provenance]</provenance>
      </acquisition>

      <collection>
        <collectors>
          <collector>
            <wiews>[cwiews]</wiews>
            <pid>[cpid]</pid>
            <name>[cname]</name>
            <address>[caddress]</address>
            <country>[ccountry]</country>
          </collector>
        </collectors>
        <sampleid>[csampleid]</sampleid>
        <missid>[missid]</missid>
        <site>[site]</site>
        <lat>[clat]</lat>
        <lon>[clon]</lon>
        <uncert>[uncert]</uncert>
        <datum>[datum]</datum>
        <georef>[georef]</georef>
        <elevation>[elevation]</elevation>
        <date>[cdate]</date>
        <source>[source]</source>
      </collection>

      <breeding>
        <breeders>
          <breeder>
            <wiews>[wiews]</wiews>
            <pid>[pid]</pid>
            <name>[name]</name>
            <address>[address]</address>
            <country>[country]</country>
          </breeder>
        </breeders>
        <ancestry>[ancestry]</ancestry>
      </breeding>

    </register>
  </xsl:template>

</xsl:stylesheet>
