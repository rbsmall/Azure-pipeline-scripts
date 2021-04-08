@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import java.io.File
import java.io.UnsupportedEncodingException
import java.security.MessageDigest
import org.apache.http.entity.FileEntity
import com.ibm.dbb.build.*
import com.ibm.dbb.build.DBBConstants.CopyMode
import com.ibm.dbb.build.report.BuildReport
import com.ibm.dbb.build.report.records.DefaultRecordFactory


/************************************************************************************
 * This script publishes the outputs generated from a Package.groovy to artifactory.
 *
 * NJL: Customized  for Garanti
 ************************************************************************************/

// Load the Tools.groovy utility script
def tools = loadScript(new File("$scriptDir/Tools.groovy"))

//hardcoded for now
def String url     ="https://eu.artifactory.swg-devops.com/artifactory"

//TaaS sys-dat-team   works

def String repo    ="sys-dat-team-generic-local"
def tarFile        =new File("/u/nlopez/Garanti-logs/build.20191110.100327.003/ortqq-1.0.0.tar")
def String remotePath ="garanti/ortqq-1.0.0.tar"

//Call the ArtifactoryHelpers to publish the tar file
def artifactoryHelpers = loadScript(new File("$scriptDir/ArtifactoryCurlHelpers.groovy"))
def artifactoryPullableURL = artifactoryHelpers.publish(url, repo, apiKey, remotePath, tarFile)


println " $tarFile published to Artifactory Repo $repo"