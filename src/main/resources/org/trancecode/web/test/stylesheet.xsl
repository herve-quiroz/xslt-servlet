<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.w3.org/1999/XSL/Transform http://www.w3.org/2007/schema-for-xslt20.xsd">

  <xsl:param name="request.path" />
  <xsl:param name="parameter1" />
  <xsl:param name="user.home" select="'UNDEFINED'" />

  <xsl:template match="/">
    <html>
      <body>
        <p>
          <xsl:value-of select="concat('parameter1 = ', $parameter1)" />
        </p>
        <p>
          <xsl:value-of select="concat('user.home = ', $user.home)" />
        </p>
        <p>
          <xsl:value-of select="concat('request.path = ', $request.path)" />
        </p>
      </body>
    </html>
  </xsl:template>

</xsl:stylesheet>
