@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.build.*
import com.ibm.dbb.build.DBBConstants.CopyMode
import groovy.transform.*

// define script properties
@Field def yamlUtils = loadScript(new File("YamlUtilities.groovy"))
def props = parseInput(args)
bindError = false
//deployApplicationPackage (props.workDir, props.tarFile, new File(props.pdsMapping).text)
deployApplicationPackage (props.workDir, props.tarFile, props.system, props.env, props.bindLocation, props.bindCollection, props.bindOwner, props.bindQualifier )

// if bind error occurred signal process error
if (bindError) {
	println "*** Exiting due to bind errors"
	System.exit(1)
}
	
// Script End	


//def deployApplicationPackage (String workDir, String tarFile, String pdsMapping) {
def deployApplicationPackage (String workDir, String tarFile, String appSystem, String deployEnv, String bindLocation, String bindCollection, String bindOwner, String bindQualifier) {
	// Local variables
	//def tempFolderName = "${workDir}/tempDeploy"
	def tempFolderName = "tempDeploy"
	new File(tempFolderName).mkdirs()

	println "** Expand  application archive file ${tarFile}"
	out = new StringBuffer()
	err = new StringBuffer()
	def tempFolder = new File(tempFolderName)
	process = "tar -xvf ${tarFile}".execute(null, tempFolder)
	process.waitForProcessOutput(out, err)
	rc = process.exitValue();
	if(rc!=0){
		print "ERR:\n" + err
		print "OUT:\n" + out
		assert rc == 0, "Failed to expand application archive file"
	}

	println "** Parse the application definition file"
	def scriptDir = getClass().protectionDomain.codeSource.location.path
	def dir = new File('.').absolutePath
	
	def pdsMapping = new File("$tempFolderName/pdsMapping.yaml").text 
	pdsMap = [:]
	pdsMapping.split("\n").each { line ->
		pdsKey = line.replaceAll(/\*\.(.*),.*/, "\$1").trim()
		pdsValue = line.replaceAll(/.*,(.*)/, "\$1").trim()
		pdsMap.put(pdsKey, pdsValue)
	}

	def srcOptions = "cyl space(1,15) lrecl(80) dsorg(PO) recfm(F,B) dsntype(library) msg(1)"
	def loadOptions = "cyl space(1,15) dsorg(PO) recfm(U) blksize(32760) dsntype(library) msg(1)"

	def copy = new CopyToPDS()
	def copyModeMap = ["NCAL": CopyMode.LOAD, "LOAD": CopyMode.LOAD, "CICS_LOAD": CopyMode.LOAD, "IMS_LOAD": CopyMode.LOAD, "DBRM": CopyMode.BINARY]
	def targetDataset = null;

	def deploymentUnitsBlock = { unit ->
		println "** Processing deploy unit: ${unit.originPDS}, type ${unit.type}, deploy type ${unit.deployType}"
		def suffix = unit.originPDS.replaceAll(/.*\.([^.]*)/, "\$1")
		def type = unit.deployType

		targetDataset = pdsMap[suffix]
		targetDataset = targetDataset.replace('@{system}',appSystem).replace('@{environment}',deployEnv)

                //NJL:  Above logic could be expanded to us the deployType as the pdsMap lookup
                // Future enhancement.  Leaving as-is
                // Adding exception for newCo support of NCAL loadLib _ subMods
                if (unit.deployType.endsWith("NCAL")) {
                         targetDataset = pdsMap["NCAL"]
                }

		if ( targetDataset == null )
			return false
			
		if ( unit.deployType.endsWith("LOAD") || unit.deployType.endsWith("NCAL")) {
			new CreatePDS().dataset(targetDataset).options(loadOptions).create()
		}
		else {
			new CreatePDS().dataset(targetDataset).options(srcOptions).create()
		}
		
		copy.setCopyMode(copyModeMap[suffix])
		copy.setDataset(targetDataset)
		return true;
	}
	
	def resourcesBlock = { unit, member  ->
		println "*** Copying member $member.name to dataset $targetDataset"
		copy.file(new File("$tempFolderName/${unit.folder}/${member.name}")).member("${member.name}").copy()
		
		if ( unit.deployType == "DBRM")
			bindApplicationPackage(member.name, targetDataset, bindLocation, bindCollection, bindOwner, bindQualifier)
	}

	yamlUtils.parseDeploymentUnitsAndResources("$tempFolderName/app.yaml", deploymentUnitsBlock, resourcesBlock)
	
	return "${tempFolderName}/app.yaml"
}

// Create and process the Bind command
def bindApplicationPackage(String memberName, String dbrmLib, String bindLocation, String bindCollection, String bindOwner, String bindQualifier) {
	// Create JCLExec String
	println "*** Beginning Bind process for ${memberName}"
	
	jcl = """\
\n//JOBNAME JOB (ACCOUNT),'PROJDEF',CLASS=A,MSGCLASS=X
//*
//BIND EXEC  PGM=IKJEFT01
//SYSTSPRT DD SYSOUT=*
//SYSPRINT DD SYSOUT=*
//SYSUDUMP DD SYSOUT=*
//SYSTSIN  DD *
   DSN SYSTEM(${bindLocation}) RETRY(5)
	   BIND PACKAGE(${bindCollection}) -
	   OWNER(${bindOwner})             -
	   QUALIFIER(${bindQualifier})     -
	   MEMBER(${memberName})           -
	   LIBRARY('${dbrmLib}')           -
	   ACTION(REPLACE)                 -
	   EXPLAIN(YES)                    -
	   VALIDATE(BIND)                  -
	   ISOLATION(CS)                   -
	   FLAG(I)                         -
	   CURRENTDATA(NO)                 -
	   DEGREE(1)                       -
	   RELEASE(COMMIT)                 -
	   ENABLE(*)                       -
	   SQLERROR(NOPACKAGE)
//*
"""

	def dbbConf = System.getenv("DBB_CONF")

	// Create jclExec
	def bindJCL = new JCLExec().text(jcl)
	bindJCL.confDir(dbbConf)

	// Execute jclExec
	bindJCL.execute()

	/**
	 * Store results
	 */
	File logFile = new File("${memberName}.bind.log")
	
	if (logFile.exists())
		logFile.delete()
		
	// Save Job Spool to logFile
	bindJCL.saveOutput(logFile, 'IBM-1047')

	// Splitting the String into a StringArray using CC as the separator
	def jobRcStringArray = bindJCL.maxRC.split("CC")

	// This evals the number of items in the ARRAY! Dont get confused with the returnCode itself
	if ( jobRcStringArray.length > 1 ){
		// Ok, the string can be split because it contains the keyword CC : Splitting by CC the second record contains the actual RC
		rc = bindJCL.maxRC.split("CC")[1].toInteger()

		// manage processing the RC, up to your logic. You might want to flag the build as failed.
		if (rc <= 0){
			println   "***  Bind Job ${bindJCL.submittedJobId} completed with $rc "
			// Store Report in Workspace
		} else { 
			bindError = true
			String errorMsg = "*! The Bind Job ${bindJCL.submittedJobId} failed with RC=($rc) for ${memberName} "
			println(errorMsg)
		}
	}
	else {
		// We don't see the CC, assume an exception
		bindError = true
		String errorMsg = "*!  Bind Job ${bindJCL.submittedJobId} failed with ${bindJCL.maxRC}"
		println(errorMsg)
	}
}


//Parsing the command line
def parseInput(String[] cliArgs)
{
	def cli = new CliBuilder(usage: "DeployUtilities.groovy [options]", header: '', stopAtNonOption: false)
	cli.h(longOpt:'help', 'Prints this message')
	cli.w(longOpt:'workDir', args:1, required:true, 'Absolute path to the working directory')
	cli.t(longOpt:'tarFile', args:1, required:true, 'Absolute path to zar file')
	cli.s(longOpt:'system', args:1, required:true, 'Application system three letter acronym')
	cli.e(longOpt:'environment', args:1, required:true, 'Deployment environment')
	cli.l(longOpt:'bindLocation', args:1, required:true, 'DB2 Subsystem')
	cli.c(longOpt:'bindCollection', args:1, required:true, 'DB2 Collection')
	cli.o(longOpt:'bindOwner', args:1, required:true, 'Bind Owner ID')
	cli.q(longOpt:'bindQualifier', args:1, required:true, 'Bind Qualifier')
	//cli.m(longOpt:'pdsMapping', args:1, required:true, 'Absolute path to pds mapping file')
	def opts = cli.parse(cliArgs)
	
	// if opt parse fail exit.
	if (! opts) {
		System.exit(1)
	}
	
	if (opts.h)
	{
		cli.usage()
		System.exit(0)
	}
	
	def props = new Properties()
	props.workDir        = opts.w
	props.tarFile        = opts.t
	props.system         = opts.s
	props.env            = opts.e
	props.bindLocation   = opts.l
	props.bindCollection = opts.c
	props.bindOwner      = opts.o
	props.bindQualifier  = opts.q
	//props.pdsMapping = opts.m
	return props
}

