# Sample script to package the artifacts in a DBB output folder and tars them with a yaml manifest
# Note: package.groovy was added to the root of zAppBuild(not a normal distro)

. $HOME/.profile
artifacts=$(ls -d $1/build*)
package=$HOME/Azure-pipeline-scripts/utilities/package.groovy

echo "**************************************************************"
echo "**     Started: Azure Packaging on HOST/USER: $(uname -Ia) $USER"
echo "**                             WorkDir:" $artifacts
echo "**                       DBB Artifacts:" $1
echo "**                                 App:" $2
echo "**                             Version:" $3
echo "**                             Project:" $4
echo "**                      Package Script:" $package
 
# Other args are for future use  
groovyz $package\
  -workDir      $artifacts\
  -workSpace    $1\
  -application  $2\
  -version      $3\
  -project      $4\
  -s gitSourceUrl-TBD\
  -g git@github.ibm.com:brice/a-dummy-repo.git\
  -x gitSourceBranch-TBD\
  -y gitBuildBranch-TBD\
  -b 12345678\
  -n P092259\
  -u none.com
  