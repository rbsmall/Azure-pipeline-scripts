#! /bin/sh
## Az/DBB Demo- Deploy DBB Package to target LPAR v1.3 (njl)

. $HOME/.profile

WorkDir=$1 ; cd $WorkDir
#tar=~/$WorkDir/$2.tar
tar=$(ls -p ~/$WorkDir/*.tar | grep -v /$)
applicationSystem=$3
deployEnv=$4
location=$5
collection=$6
owner=$7
qualifier=$8
#pdsMapping=$3

echo "**************************************************************"
echo "**     Started:  DBB Build on HOST/USER: $(uname -Ia)/$USER"
echo "**                           WorkDir:" $PWD
echo "**                       Tar Package:" $tar 
echo "**                       Application:" $applicationSystem
echo "**                       Environment:" $deployEnv 
echo "**                      DB2 Location:" $location 
echo "**                    DB2 Collection:" $collection
echo "**                      DB2 Owner ID:" $owner 
echo "**                     DB2 Qualifier:" $qualifier  
echo "** "

deploy=$HOME/Azure-pipeline-scripts/utilities/DeployUtilities.groovy
groovyz $deploy  -w $WorkDir -t $tar -s $applicationSystem -e $deployEnv -l $location -c $collection -o $owner -q $qualifier