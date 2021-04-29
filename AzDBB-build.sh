#! /bin/sh
## Az/DBB Demo- DBB Build v1.2 (njl)

. $HOME/.profile
WorkDir=$1 ; cd $WorkDir
WorkSpace=$2
App=$3
BuildMode="$4 $5" #DBB Build modes:  --impactBuild,  --reset, --fullBuild, '--fullbuild --scanOnly'
zAppBuild=$HOME/dbb-zappbuild-for-azure/build.groovy

echo "**************************************************************"
echo "**     Started:  DBB Build on HOST/USER: $(uname -Ia)/$USER"
echo "**                          WorkDir:" $PWD
echo "**                        Workspace:" $WorkSpace
echo "**                              App:" $App
echo "**    	           DBB Build Mode:" $BuildMode
echo "**               DBB zAppBuild Path:" $zAppBuild
echo "**                         DBB_HOME:" $DBB_HOME
echo "** "
echo " ** Git Status  for buildWorkSpace:"

git -C $WorkSpace status  
groovyz $zAppBuild  --workspace $WorkSpace --application $App  -outDir . --hlq $USER.AZURE  --logEncoding UTF-8  $BuildMode
if [ "$?" -ne "0" ]; then
  echo "DBB Build Error. Check the build log for details"
  exit 12
fi

## Except for the reset mode, check for "nothing to build" condition and throw an error to stop pipeline
if [ "$BuildMode" != "--reset" ]; then 
	buildlistsize=$(wc -c < $(find . -name buildList.txt)) 
	if [ $buildlistsize = 0 ]; then 
    	echo "*** Build Error:  No source changes detected.   RC=12"
    	exit 12    
	fi
else
	echo "*** DBB Reset completed"
fi
exit 0 