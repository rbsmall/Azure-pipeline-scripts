#! /bin/sh
## Az/DBB Demo- Deploy DBB Package to target LPAR v1.3 (njl)

. $HOME/.profile

WorkDir=$1 ; cd $WorkDir
tar=~/$WorkDir/$2.tar
applicationSystem=$3
deployEnv=$4

echo "**************************************************************"
echo "**     Started:  DBB Build on HOST/USER: $(uname -Ia)/$USER"
echo "**                           WorkDir:" $PWD
echo "**                       Tar Package:" $tar 
echo "**                    System Acronym:" $applicationSystem
echo "**            Deployment Environment:" $deployEnv
echo "** "

deploy=$HOME/Azure-pipeline-scripts/utilities/DeployUtilities.groovy
groovyz $deploy  -w $WorkDir -t $tar -s $applicationSystem -e $deployEnv 