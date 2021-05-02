# $Id$
#
# Makefile building json-c library (http://oss.metaparadigm.com/json-c/)
# 
include ../../../../GDALmake.opt

OBJ = \
	arraylist.o \
	debug.o \
	json_object.o \
	json_tokener.o \
	json_util.o \
	linkhash.o \
	printbuf.o \
	json_object_iterator.o \
	json_c_version.o

O_OBJ = $(foreach file,$(OBJ),../../o/$(file))

CPPFLAGS := $(CPPFLAGS)

default:	$(O_OBJ:.o=.$(OBJ_EXT))

# -D_XOPEN_SOURCE=700 to enable strdup() definition in C11 mode
# Mac/ios require 600+ for strdup; the original 500 is not sufficient
CPPFLAGS := -D_XOPEN_SOURCE=700 $(CPPFLAGS)

../../o/%.$(OBJ_EXT):	%.c
	$(CC) $(CFLAGS) $(CPPFLAGS) -c -o $@ $<

clean:
	rm -f *.o $(O_OBJ)
	rm -f *~
