project('libhello', 'cpp')

cxx = meson.get_compiler('cpp')

libhello = static_library(
  'hello',
  'hello.cpp',
  cpp_args: cxx.get_supported_arguments(),
  include_directories: include_directories('.'),
  install: true,
  install_dir: join_paths(get_option('prefix'), 'lib'),
)

install_headers('hello.h', subdir: 'include')
