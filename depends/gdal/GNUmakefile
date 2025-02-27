include GDALmake.opt

GDAL_OBJ	=	frmts/o/*.o \
			gcore/*.o \
			port/*.o \
			alg/*.o \
			apps/commonutils.o \
			apps/gdalinfo_lib.o \
			apps/gdal_translate_lib.o \
			apps/gdalwarp_lib.o \
			apps/ogr2ogr_lib.o \
			apps/gdaldem_lib.o \
			apps/nearblack_lib.o \
			apps/gdal_grid_lib.o \
			apps/gdal_rasterize_lib.o \
			apps/gdalbuildvrt_lib.o

GDAL_OBJ += ogr/ogrsf_frmts/o/*.o

ifeq ($(GNM_ENABLED),yes)
   GDAL_OBJ += $(GDAL_ROOT)/gnm/*.o $(GDAL_ROOT)/gnm/gnm_frmts/o/*.o
endif

ifeq ($(HAVE_LERC),yes)
   GDAL_OBJ += $(GDAL_ROOT)/third_party/o/*.o
endif

include ./ogr/file.lst
GDAL_OBJ += $(addprefix ogr/,$(OBJ))

LIBGDAL-yes	:=	$(GDAL_LIB)
LIBGDAL-$(HAVE_LD_SHARED)	+=	$(GDAL_SLIB)
# override if we are using libtool
LIBGDAL-$(HAVE_LIBTOOL)	:= $(LIBGDAL)

default:	lib-target apps-target swig-target gdal.pc
ifeq ($(PDF_PLUGIN),yes)
	(cd frmts/pdf; $(MAKE) plugin)
endif

lib-target:	check-lib;

force-lib:
	$(AR) r $(GDAL_LIB) $(GDAL_OBJ)
	$(RANLIB) $(GDAL_LIB)
	$(LD_SHARED) $(GDAL_SLIB_SONAME) $(GDAL_OBJ) $(GDAL_LIBS) $(LDFLAGS) $(LIBS) \
		-o $(GDAL_SLIB)

static-lib: lib-dependencies GDALmake.opt
	$(MAKE) static-lib-stage2

static-lib-stage2: $(GDAL_OBJ)
	rm -f libgdal.a
	$(AR) r $(GDAL_LIB) $(GDAL_OBJ)

$(GDAL_LIB):	$(GDAL_OBJ) GDALmake.opt
	rm -f libgdal.a
	$(AR) r $(GDAL_LIB) $(GDAL_OBJ)
	$(RANLIB) $(GDAL_LIB)

$(GDAL_SLIB):	$(GDAL_OBJ) $(GDAL_LIB)
	$(LD_SHARED) $(GDAL_SLIB_SONAME) $(GDAL_OBJ) $(GDAL_LIBS) $(LDFLAGS) $(LIBS) \
		-o $(GDAL_SLIB)

$(LIBGDAL):	$(GDAL_OBJ:.o=.lo)
	$(LD) $(LDFLAGS) $(LIBS) -o $@ $(sort $(wildcard $(GDAL_OBJ:.o=.lo))) \
	    -rpath $(INST_LIB) \
	    -no-undefined \
	    -version-info $(LIBGDAL_CURRENT):$(LIBGDAL_REVISION):$(LIBGDAL_AGE)
	case $(LIBGDAL) in *.la) dos2unix $(LIBGDAL) ;; esac
ifeq ($(MACOSX_FRAMEWORK),yes)
	install_name_tool -id ${OSX_VERSION_FRAMEWORK_PREFIX}/GDAL .libs/libgdal.dylib
endif

lib-dependencies:	port-target core-target frmts-target third-party-target ogr-target gnm-target appslib-target

check-lib:	lib-dependencies
	$(MAKE) $(LIBGDAL-yes)

generate_gdal_version_h:
	(cd gcore; $(MAKE) generate_gdal_version_h)

appslib-target: generate_gdal_version_h
	(cd apps; $(MAKE) appslib)

port-target:
	(cd port; $(MAKE))

ogr-target: generate_gdal_version_h
	(cd ogr; $(MAKE) lib )

ifeq ($(GNM_ENABLED),yes)
gnm-target: generate_gdal_version_h
	(cd gnm; $(MAKE) lib )
else
gnm-target:	;
endif

core-target: generate_gdal_version_h
	(cd gcore; $(MAKE))
	(cd alg; $(MAKE))

frmts-target: generate_gdal_version_h
	(cd frmts; $(MAKE))

third-party-target: generate_gdal_version_h
	(cd third_party; $(MAKE))

ogr-all: generate_gdal_version_h
	(cd ogr; $(MAKE) all)

apps-target:	lib-target
	(cd apps; $(MAKE))

ifeq ($(BINDINGS),)
swig-target: ;
else
swig-target:    swig-modules;
endif

# Python bindings needs gdal-config, hence apps-target
swig-modules:	apps-target
	(cd swig; $(MAKE) build)

clean:	lclean
	(cd port; $(MAKE) clean)
	(cd ogr; $(MAKE) clean)
	(cd gnm; $(MAKE) clean)
	(cd gcore; $(MAKE) clean)
	(cd frmts; $(MAKE) clean)
	(cd third_party; $(MAKE) clean)
	(cd alg; $(MAKE) clean)
	(cd apps; $(MAKE) clean)
ifneq ($(BINDINGS),)
	(cd swig; $(MAKE) clean)
endif
ifeq ($(PDF_PLUGIN),yes)
	(cd frmts/pdf; $(MAKE) clean)
endif


lclean:
	rm -f *.a *.so config.log config.cache html/*.*
	$(RM) *.la

distclean:	dist-clean

dist-clean:	clean
	rm -f GDALmake.opt port/cpl_config.h config.cache config.status
	rm -f libtool
	rm -rf autom4te.cache

config:	configure
	./configure

GDALmake.opt:	GDALmake.opt.in config.status
	./config.status

config.status:  configure
	./config.status --recheck

docs:
	(cd html; rm -f *.*)
# Generate translated docs. Should go first, because index.html page should
# be overwritten with the main one later
	doxygen -w html html/header.html html/footer.html html/stylesheet.css
	sed -e 's,iso-8859-1,utf-8,g' html/header.html > html/header_ru.html
	cp html/header.html html/header_br.html
	(cd doc/ru ; doxygen ; cp html/*.* ../../html/. )
	(cd doc/br ; doxygen ; cp html/*.* ../../html/. )
# Generate HTML docs
# Current DoxygenLayout.xml works only with Doxygen >= 1.8
	@if [ `doxygen --version | awk -F . '{print $$1}'` -eq "1" ] && [ `doxygen --version | awk -F . '{print $$2}'` -lt "8" ]; then \
	  echo "Using deprecated doxygen version. Removing custom layout"; \
	  (cat Doxyfile; echo "LAYOUT_FILE=/dev/null") | doxygen -; \
	else \
	  doxygen Doxyfile; \
	fi
	cp data/gdalicon.png html
	cp doc/images/*.* html
	cp doc/grid/*.png html
	cp frmts/*.html frmts/*/frmt_*.html frmts/*/drv_*.html html
	cp frmts/openjpeg/*.xml html
	cp frmts/wms/frmt_*.xml html
	cp ogr/ogrsf_frmts/*/frmt_*.html html
	cp ogr/ogrsf_frmts/*/drv_*.html html
	cp ogr/ogrsf_frmts/ogr_formats.html html
	cp ogr/*.html html
	cp ogr/ogrsf_frmts/gpkg/geopackage_aspatial.html html
	cp ogr/*.gif html
	cp ogr/*.png html

.PHONY: man

man:
# Generate man pages
	(cat Doxyfile ; echo "ENABLED_SECTIONS=man"; echo "INPUT=apps swig/python/scripts"; echo "FILE_PATTERNS=*.cpp *.dox"; echo "GENERATE_HTML=NO"; echo "GENERATE_MAN=YES" ; echo "LAYOUT_FILE=/dev/null") | doxygen -
# Remove "Directory reference" file. Not sure if there's a better way of doing it.
	 @find man -name '_home_*_gdal_apps_.1' -exec rm {} \;

all:	default ogr-all

install-docs:
	$(INSTALL_DIR) $(DESTDIR)$(INST_DOCS)/gdal
	cp html/*.* $(DESTDIR)$(INST_DOCS)/gdal

install-man:
	$(INSTALL_DIR) $(DESTDIR)$(INST_MAN)/man1
	for f in $(wildcard man/man1/*.1) ; do $(INSTALL_DATA) $$f $(DESTDIR)$(INST_MAN)/man1 ; done

web-update:	docs
	$(INSTALL_DIR) $(INST_HTML)
	cp html/*.* $(INST_HTML)

install:	install-actions

install-static-lib: static-lib gdal.pc
	$(INSTALL_LIB) $(GDAL_LIB) $(DESTDIR)$(INST_LIB)
	$(INSTALL_DIR) $(DESTDIR)$(INST_DATA)
	$(INSTALL_DIR) $(DESTDIR)$(INST_INCLUDE)
	(cd port; $(MAKE) install)
	(cd gcore; $(MAKE) install)
	(cd frmts; $(MAKE) install)
	(cd alg; $(MAKE) install)
	(cd ogr; $(MAKE) install)
	(cd gnm; $(MAKE) install)
	for f in LICENSE.TXT data/*.* ; do $(INSTALL_DATA) $$f $(DESTDIR)$(INST_DATA) ; done
	$(LIBTOOL_FINISH) $(DESTDIR)$(INST_LIB)
	$(INSTALL_DIR) $(DESTDIR)$(INST_LIB)/pkgconfig
	$(INSTALL_DATA) gdal.pc $(DESTDIR)$(INST_LIB)/pkgconfig/gdal.pc

install-actions: install-lib
	$(INSTALL_DIR) $(DESTDIR)$(INST_BIN)
	$(INSTALL_DIR) $(DESTDIR)$(INST_DATA)
	$(INSTALL_DIR) $(DESTDIR)$(INST_INCLUDE)
ifeq ($(MACOSX_FRAMEWORK),yes)
	$(INSTALL_DIR) $(DESTDIR)${OSX_VERSION_FRAMEWORK_PREFIX}/PlugIns
	$(INSTALL_DIR) $(DESTDIR)${OSX_VERSION_FRAMEWORK_PREFIX}/unix/lib
	ln -sfh Versions/Current/unix $(DESTDIR)${OSX_FRAMEWORK_PREFIX}/unix
	ln -sfh ../Programs $(DESTDIR)${OSX_VERSION_FRAMEWORK_PREFIX}/unix/bin
	ln -sfh ../Headers $(DESTDIR)${OSX_VERSION_FRAMEWORK_PREFIX}/unix/include
	ln -sf ../../GDAL $(DESTDIR)${OSX_VERSION_FRAMEWORK_PREFIX}/unix/lib/libgdal.dylib
	ln -sfh $(GDAL_VERSION_MAJOR).$(GDAL_VERSION_MINOR) $(DESTDIR)${OSX_FRAMEWORK_PREFIX}/Versions/Current
	ln -sfh Versions/Current/Headers $(DESTDIR)${OSX_FRAMEWORK_PREFIX}/Headers
	ln -sfh Versions/Current/Programs $(DESTDIR)${OSX_FRAMEWORK_PREFIX}/Programs
	ln -sfh Versions/Current/Resources $(DESTDIR)${OSX_FRAMEWORK_PREFIX}/Resources
	ln -sfh Versions/Current/GDAL $(DESTDIR)${OSX_FRAMEWORK_PREFIX}/GDAL
endif
	(cd port; $(MAKE) install)
	(cd gcore; $(MAKE) install)
	(cd frmts; $(MAKE) install)
	(cd alg; $(MAKE) install)
	(cd ogr; $(MAKE) install)
	(cd gnm; $(MAKE) install)
	(cd apps; $(MAKE) install)
ifneq ($(BINDINGS),)
	(cd swig; $(MAKE) install)
endif
	(cd scripts; $(MAKE) install)
	for f in LICENSE.TXT data/*.* ; do $(INSTALL_DATA) $$f $(DESTDIR)$(INST_DATA) ; done
	$(LIBTOOL_FINISH) $(DESTDIR)$(INST_LIB)
	$(INSTALL_DIR) $(DESTDIR)$(INST_LIB)/pkgconfig
	$(INSTALL_DATA) gdal.pc $(DESTDIR)$(INST_LIB)/pkgconfig/gdal.pc

ifeq ($(HAVE_LIBTOOL),yes)

install-lib: default
	$(INSTALL_DIR) $(DESTDIR)$(INST_LIB)
	for f in $(LIBGDAL-yes) ; do $(INSTALL_LIB) $$f $(DESTDIR)$(INST_LIB) ; done
ifeq ($(MACOSX_FRAMEWORK),yes)
	if [ -f "$(DESTDIR)$(INST_LIB)/libgdal.$(GDAL_VERSION_MAJOR).$(LIBGDAL_AGE).$(LIBGDAL_REVISION).dylib" ] ; then mv -f $(DESTDIR)$(INST_LIB)/libgdal.$(GDAL_VERSION_MAJOR).$(LIBGDAL_AGE).$(LIBGDAL_REVISION).dylib $(DESTDIR)${OSX_VERSION_FRAMEWORK_PREFIX}/GDAL ; fi
	if [ -f "$(DESTDIR)$(INST_LIB)/libgdal.$(GDAL_VERSION_MAJOR).$(LIBGDAL_AGE).dylib" ] ; then mv -f $(DESTDIR)$(INST_LIB)/libgdal.$(GDAL_VERSION_MAJOR).$(LIBGDAL_AGE).dylib $(DESTDIR)${OSX_VERSION_FRAMEWORK_PREFIX}/GDAL ; fi
	if [ -f "$(DESTDIR)$(INST_LIB)/libgdal.$(GDAL_VERSION_MAJOR).dylib" ] ; then mv -f $(DESTDIR)$(INST_LIB)/libgdal.$(GDAL_VERSION_MAJOR).dylib $(DESTDIR)${OSX_VERSION_FRAMEWORK_PREFIX}/GDAL ; fi
	if [ -f "$(DESTDIR)$(INST_LIB)/libgdal.dylib" ] ; then mv -f $(DESTDIR)$(INST_LIB)/libgdal.dylib $(DESTDIR)${OSX_VERSION_FRAMEWORK_PREFIX}/GDAL ; fi
	rm -f $(DESTDIR)$(INST_LIB)/libgdal.$(GDAL_VERSION_MAJOR).$(LIBGDAL_AGE).$(LIBGDAL_REVISION).dylib
	rm -f $(DESTDIR)$(INST_LIB)/libgdal.$(GDAL_VERSION_MAJOR).$(LIBGDAL_AGE).dylib
	rm -f $(DESTDIR)$(INST_LIB)/libgdal.$(GDAL_VERSION_MAJOR).dylib
	rm -f $(DESTDIR)$(INST_LIB)/libgdal.dylib
	rm -f $(DESTDIR)$(INST_LIB)/libgdal.la
else
	$(INSTALL_DIR) $(DESTDIR)$(INST_LIB)/gdalplugins
endif

else

ifeq ($(HAVE_LD_SHARED),yes)

GDAL_SLIB_B	=	$(notdir $(GDAL_SLIB))

install-lib: default

	$(INSTALL_DIR) $(DESTDIR)$(INST_LIB)
ifeq ($(MACOSX_FRAMEWORK),yes)
	$(INSTALL_LIB) $(GDAL_SLIB) $(DESTDIR)$(INST_LIB)/GDAL
else
	rm -f $(DESTDIR)$(INST_LIB)/$(GDAL_SLIB_B)
	rm -f $(DESTDIR)$(INST_LIB)/$(GDAL_SLIB_B).$(GDAL_VERSION_MAJOR)
	rm -f $(DESTDIR)$(INST_LIB)/$(GDAL_SLIB_B).$(GDAL_VER)
	$(INSTALL_LIB) $(GDAL_SLIB) $(DESTDIR)$(INST_LIB)/$(GDAL_SLIB_B).$(GDAL_VER)
	(cd $(DESTDIR)$(INST_LIB) ; \
	 cp $(GDAL_SLIB_B).$(GDAL_VER) $(GDAL_SLIB_B).$(GDAL_VERSION_MAJOR))
	(cd $(DESTDIR)$(INST_LIB) ; \
	 cp $(GDAL_SLIB_B).$(GDAL_VERSION_MAJOR) $(GDAL_SLIB_B))
	$(INSTALL_DIR) $(DESTDIR)$(INST_LIB)/gdalplugins
endif

else

install-lib: default
	$(INSTALL_DIR) $(DESTDIR)$(INST_LIB)
	$(INSTALL_LIB) $(GDAL_LIB) $(DESTDIR)$(INST_LIB)

endif # HAVE_LD_SHARED=no

endif # HAVE_LIBTOOL=no

gdal.pc:	gdal.pc.in GDALmake.opt ./GNUmakefile VERSION
	rm -f gdal.pc
	echo 'CONFIG_VERSION='`cat ./VERSION`'' >> gdal.pc
	echo 'CONFIG_INST_PREFIX=$(INST_PREFIX)' >> gdal.pc
	echo 'CONFIG_INST_LIBS=$(CONFIG_LIBS_INS)' >> gdal.pc
	echo 'CONFIG_INST_CFLAGS=-I$(INST_INCLUDE)' >> gdal.pc
	echo 'CONFIG_INST_DATA=$(INST_DATA)' >> gdal.pc
	cat gdal.pc.in >> gdal.pc
