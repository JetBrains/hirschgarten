@echo off
setlocal EnableDelayedExpansion

rem A fake GCC that only answers the probes of the IDE, any compilation fails.

if "%~1"=="--version" (
  echo gcc ^(cc_false_toolchain^) 1.0
  exit /b 0
)

set preprocess=0
set language=c++
set quote=
set angled=

:next
set "arg=%~1"
set "value=%~2"
if not defined arg goto done
if "!arg!"=="-E" (
  set preprocess=1
) else if "!arg!"=="-x" (
  set "language=!value!"
  shift
) else if "!arg:~0,2!"=="-x" (
  set "language=!arg:~2!"
) else if "!arg!"=="-iquote" (
  set "quote=!quote! "!value!""
  shift
) else if "!arg:~0,7!"=="-iquote" (
  set "quote=!quote! "!arg:~7!""
) else if "!arg!"=="-I" (
  set "angled=!angled! "!value!""
  shift
) else if "!arg!"=="-isystem" (
  set "angled=!angled! "!value!""
  shift
) else if "!arg:~0,8!"=="-isystem" (
  set "angled=!angled! "!arg:~8!""
) else if "!arg:~0,2!"=="-I" (
  set "angled=!angled! "!arg:~2!""
)
shift
goto next

:done
if not "%preprocess%"=="1" exit /b 1

rem the markers the IDE expects around the predefined macros
echo ___CIDR_DEFINITIONS_END
echo ___CIDR_FEATURES_START

rem without a language standard the IDE does not send the source to its C/C++ engine
if "!language!"=="c" (
  echo { int ____CIDR_command_SET_LANGUAGE_STANDARD, ____CIDR_command_STANDARD_C = ^(201710L, 0^); }
) else (
  echo { int ____CIDR_command_SET_LANGUAGE_STANDARD, ____CIDR_command_STANDARD_CPP = ^(201703L, 0^); }
)

>&2 echo #include "..." search starts here:
for %%d in (!quote!) do >&2 echo  %%~d
>&2 echo #include ^<...^> search starts here:
for %%d in (!angled!) do >&2 echo  %%~d
>&2 echo End of search list.
