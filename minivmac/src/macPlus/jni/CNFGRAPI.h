/*
	Configuration options used by platform specific code.

	This file is automatically generated by the build system,
	which tries to know what options are valid in what
	combinations. Avoid changing this file manually unless
	you know what you're doing.
*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <sys/time.h>
#include <sys/times.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <errno.h>
#include <unistd.h>
#include <dlfcn.h>

#define CanGetAppPath 1
#define HaveAppPathLink 1
#define TheAppPathLink "/proc/self/exe"
#define HaveSysctlPath 0

#define RaspbianWorkAround 1

#define RomFileName "vMac.ROM"
#define EnableDragDrop 0
#define EnableAltKeysMode 0
#define SwapCommandControl 0
#define VarFullScreen 0
#define WantInitFullScreen 0
#define MayFullScreen 1
#define MayNotFullScreen 1
#define WantInitMagnify 0
#define EnableMagnify 1
#define MyWindowScale 2
#define WantInitRunInBackground 0
#define WantInitNotAutoSlow 0
#define WantInitSpeedValue 3
#define NeedRequestInsertDisk 0
#define NeedDoMoreCommandsMsg 0
#define NeedDoAboutMsg 0
#define UseControlKeys 1
#define UseActvCode 0

/* version and other info to display to user */

#define NeedIntlChars 1
#define kStrAppName "Mini vMac"
#define kAppVariationStr "minivmac-3.3.3-android"
#define kStrCopyrightYear "2013"
#define kMaintainerName "Gil Osher"
#define kStrHomePage "http://dolfin.github.com/minivmac4android"