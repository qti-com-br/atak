
include ../../GDALmake.opt

SUBDIRS-yes	:= \
	generic avc bna csv dgn geojson gml gmt mem kml \
	mitab ntf gpx rec s57 sdts shape tiger vrt \
	geoconcept xplane georss gtm dxf pgdump gpsbabel \
	sua openair pds htf aeronavfaa edigeo svg idrisi \
	arcgen segukooa segy sxf openfilegdb wasp selafin jml vdv mvt

SUBDIRS-$(HAVE_DODS)	+= dods
SUBDIRS-$(HAVE_TEIGHA)  += dwg
SUBDIRS-$(HAVE_CAD)	+= cad
SUBDIRS-$(HAVE_FME)	+= fme
SUBDIRS-$(HAVE_GRASS)	+= grass
SUBDIRS-$(HAVE_IDB)	+= idb
SUBDIRS-$(HAVE_XERCES)	+= ili
SUBDIRS-$(HAVE_XERCES)	+= gmlas
SUBDIRS-$(HAVE_NAS)	+= nas
SUBDIRS-$(HAVE_MYSQL)	+= mysql
SUBDIRS-$(ODBC_SETTING)	+= odbc pgeo mssqlspatial geomedia walk
SUBDIRS-$(HAVE_OGDI)	+= ogdi
SUBDIRS-$(HAVE_OCI)	+= oci
SUBDIRS-$(HAVE_OGR_PG)	+= pg
SUBDIRS-$(HAVE_SQLITE)	+= sqlite
SUBDIRS-$(HAVE_SDE)     += sde
SUBDIRS-$(HAVE_FGDB)    += filegdb
SUBDIRS-$(HAVE_ARCOBJECTS)  += arcobjects
SUBDIRS-$(HAVE_INGRES)  += ingres
SUBDIRS-$(HAVE_SQLITE)  += vfk
SUBDIRS-$(HAVE_LIBKML)  += libkml
SUBDIRS-$(CURL_SETTING) += wfs
SUBDIRS-$(MDB_ENABLED)  += mdb
SUBDIRS-$(CURL_SETTING) += gft
SUBDIRS-$(CURL_SETTING) += couchdb
SUBDIRS-$(CURL_SETTING) += cloudant
SUBDIRS-$(HAVE_FREEXL)  += xls
SUBDIRS-$(HAVE_EXPAT)   += ods
SUBDIRS-$(HAVE_EXPAT)   += xlsx
SUBDIRS-$(CURL_SETTING) += elastic
SUBDIRS-$(HAVE_SQLITE)	+= gpkg
SUBDIRS-$(HAVE_SQLITE)	+= osm
SUBDIRS-$(HAVE_SOSI)	+= sosi
SUBDIRS-$(CURL_SETTING) += carto
SUBDIRS-$(CURL_SETTING) += amigocloud
SUBDIRS-$(CURL_SETTING) += plscenes
SUBDIRS-$(CURL_SETTING) += csw
SUBDIRS-$(HAVE_MONGODB) += mongodb
# Disabled due to portability issues with android ndk
#SUBDIRS-$(CURL_SETTING) += ngw

default:	$(foreach d,$(SUBDIRS-yes),$(d)-target)

clean:	$(foreach d,$(SUBDIRS-yes),$(d)-clean)
	rm -f o/*.o
	$(RM) o/*.lo
