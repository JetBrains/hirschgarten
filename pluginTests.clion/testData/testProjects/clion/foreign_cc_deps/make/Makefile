CC ?= gcc
AR ?= ar

PREFIX ?= /usr/local

LIBDIR = $(PREFIX)/lib
INCLUDEDIR = $(PREFIX)/include

CFLAGS ?= -Wall -Wextra -O2
CPPFLAGS += -Iinclude

SRCS = src/format_utils.c
OBJS = $(SRCS:.c=.o)
LIB = libformat_utils.a

all: $(LIB)

$(LIB): $(OBJS)
	$(AR) rcs $@ $^

$(OBJS): $(wildcard include/*.h)
%.o: %.c
	$(CC) $(CFLAGS) -Iinclude $(CPPFLAGS) -c $< -o $@

install: $(LIB) $(wildcard include/*.h)
	mkdir -p $(DESTDIR)$(LIBDIR) $(DESTDIR)$(INCLUDEDIR)
	cp $(LIB) $(DESTDIR)$(LIBDIR)/
	cp include/format_utils.h $(DESTDIR)$(INCLUDEDIR)/

clean:
	rm -f $(OBJS) $(LIB)

.PHONY: all install clean
