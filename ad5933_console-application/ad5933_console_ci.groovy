// Project directory name
projectName = "ad5933_console-application"

// This variable holds the build status
buildStatusInfo = "Success"

def ad5933ConsoleBuild() {
	Map buildMap =[:]
	if (env.CHANGE_TARGET == "main" || env.CHANGE_TARGET == "develop") {
		// This map is for building all targets when merging to main or develop branches
		buildMap = [
			PLATFORM_NAME: ['SDP_K1', 'NUCLEO_L552ZE_Q', 'DISCO_F769NI'],
			EVB_INTERFACE: ['SDP_120', 'ARDUINO'],
			ACTIVE_DEVICE: ['DEV_AD5933']
		]
	} else {
		// This map is for building targets that is always build
		buildMap = [
			PLATFORM_NAME: ['SDP_K1'],
			EVB_INTERFACE: ['SDP_120', 'ARDUINO'],
			ACTIVE_DEVICE: ['DEV_AD5933']
		]
	}

	// Get the matrix combination and filter them to exclude unrequired matrixes
	List buildMatrix = general.getMatrixCombination(buildMap).findAll { axis ->
		// Skip 'all platforms except SDP-K1 + SDP_120 EVB interface'
		!(axis['PLATFORM_NAME'] == 'NUCLEO_L552ZE_Q' && axis['EVB_INTERFACE'] == 'SDP_120') &&
    	!(axis['PLATFORM_NAME'] == 'DISCO_F769NI' && axis['EVB_INTERFACE'] == 'SDP_120')
	}

	def buildType = buildInfo.getBuildType()
	if (buildType == "sequential") {
		node(label : "${buildInfo.getBuilderLabel(projectName: projectName)}") {
			ws('workspace/pcg-fw') {
				checkout scm
				try {
					for (int i = 0; i < buildMatrix.size(); i++) {
						Map axis = buildMatrix[i]
						runSeqBuild(axis)
					}
				}
				catch (Exception ex) {
					echo "Failed in ${projectName}-Build"
					echo "Caught:${ex}"
					buildStatusInfo = "Failed"
					currentBuild.result = 'FAILURE'
				}
				deleteDir()
			}
		}
	}
	else {
		// Build all included matrix combinations
		buildMap = [:]
		for (int i = 0; i < buildMatrix.size(); i++) {
			// Convert the Axis into valid values for withEnv step
			Map axis = buildMatrix[i]
			List axisEnv = axis.collect { key, val ->
				"${key}=${val}"
			}

			buildMap[axisEnv.join(', ')] = { ->
				ws('workspace/pcg-fw') {
					checkout scm
					withEnv(axisEnv) {
						try {
							stage("Build-${PLATFORM_NAME}-${ACTIVE_DEVICE}-${EVB_INTERFACE}") {

								echo "^^**^^ ^^**^^ ^^**^^ ^^**^^ ^^**^^ ^^**^^ ^^**^^ ^^**^^ ^^**^^ ^^**^^"     
								echo "Running on node: '${env.NODE_NAME}'"
							
								echo "TOOLCHAIN: ${TOOLCHAIN}"
								echo "TOOLCHAIN_PATH: ${TOOLCHAIN_PATH}"
							
								echo "Building for ${PLATFORM_NAME} and ${ACTIVE_DEVICE} with ${EVB_INTERFACE} interface"
								echo "^^**^^ ^^**^^ ^^**^^ ^^**^^ ^^**^^ ^^**^^ ^^**^^ ^^**^^ ^^**^^ ^^**^^"
							
								echo "Starting mbed build..."
								//NOTE: if adding in --profile, need to change the path where the .bin is found by mbedflsh in Test stage
								bat "cd ${projectName} & make test TARGET_BOARD=${PLATFORM_NAME} ARTIFACT-NAME=${PLATFORM_NAME}-${ACTIVE_DEVICE}-${EVB_INTERFACE} TEST_FLAGS=-DPLATFORM_NAME=\\\\\\\"${PLATFORM_NAME}\\\\\\\" TEST_FLAGS+=-DDEVICE_NAME=\\\\\\\"${ACTIVE_DEVICE}\\\\\\\" TEST_FLAGS+=-D${ACTIVE_DEVICE} TEST_FLAGS+=-D${EVB_INTERFACE}"
								artifactory.uploadArtifacts("${projectName}/build/${PLATFORM_NAME}/${TOOLCHAIN}","precision-converters-firmware/${projectName}/${env.BRANCH_NAME}")
								archiveArtifacts allowEmptyArchive: true, artifacts: "${projectName}/build/**/*.bin, ${projectName}/build/**/*.elf"
								stash includes: "${projectName}/build/**/*.bin, ${projectName}/build/**/*.elf", name: "${PLATFORM_NAME}-${ACTIVE_DEVICE}-${EVB_INTERFACE}"
								deleteDir()
							}
						}
						catch (Exception ex) {
								echo "Failed in Build-${PLATFORM_NAME}-${ACTIVE_DEVICE}-${EVB_INTERFACE} stage"
								echo "Caught:${ex}"
								buildStatusInfo = "Failed"
								currentBuild.result = 'FAILURE'
								deleteDir()
						}
					}
				}
			}
		}

		stage("Matrix Builds") {
			parallel(buildMap)
		}
	}

	return buildStatusInfo
}

def runSeqBuild(Map config =[:]) {
	stage("Build-${config.PLATFORM_NAME}-${config.ACTIVE_DEVICE}-${config.EVB_INTERFACE}") {

		echo "^^**^^ ^^**^^ ^^**^^ ^^**^^ ^^**^^ ^^**^^ ^^**^^ ^^**^^ ^^**^^ ^^**^^"     
		echo "Running on node: '${env.NODE_NAME}'"
	
		echo "TOOLCHAIN: ${TOOLCHAIN}"
		echo "TOOLCHAIN_PATH: ${TOOLCHAIN_PATH}"
	
		echo "Building for ${config.PLATFORM_NAME} and ${config.ACTIVE_DEVICE} with ${config.EVB_INTERFACE} interface"
		echo "^^**^^ ^^**^^ ^^**^^ ^^**^^ ^^**^^ ^^**^^ ^^**^^ ^^**^^ ^^**^^ ^^**^^"
	
		echo "Starting mbed build..."
		//NOTE: if adding in --profile, need to change the path where the .bin is found by mbedflsh in Test stage
		bat "cd ${projectName} & make test TARGET_BOARD=${config.PLATFORM_NAME} ARTIFACT-NAME=${config.PLATFORM_NAME}-${config.ACTIVE_DEVICE}-${config.EVB_INTERFACE} TEST_FLAGS=-DPLATFORM_NAME=\\\\\\\"${config.PLATFORM_NAME}\\\\\\\" TEST_FLAGS+=-DDEVICE_NAME=\\\\\\\"${config.ACTIVE_DEVICE}\\\\\\\" TEST_FLAGS+=-D${config.ACTIVE_DEVICE} TEST_FLAGS+=-D${config.EVB_INTERFACE}"
		artifactory.uploadArtifacts("${projectName}/build/${config.PLATFORM_NAME}/${TOOLCHAIN}","precision-converters-firmware/${projectName}/${env.BRANCH_NAME}")
		archiveArtifacts allowEmptyArchive: true, artifacts: "${projectName}/build/**/*.bin, ${projectName}/build/**/*.elf"
		stash includes: "${projectName}/build/**/*.bin, ${projectName}/build/**/*.elf", name: "${config.PLATFORM_NAME}-${config.ACTIVE_DEVICE}-${config.EVB_INTERFACE}"
		bat "cd ${projectName} & make clean TARGET_BOARD=${config.PLATFORM_NAME}"
	}
}

def ad5933ConsoleTest() {
	Map testMap =[:]
	if (env.CHANGE_TARGET == "main" || env.CHANGE_TARGET == "develop") {
		// This map is for testing all targets when merging to main or develop branches
		testMap = [
			PLATFORM_NAME: ['SDP_K1'],
			EVB_INTERFACE: ['ARDUINO'],
			ACTIVE_DEVICE: ['DEV_AD5933']
		]
	} else {
		// This map is for testing target that is always tested
		testMap = [
			PLATFORM_NAME: ['SDP_K1'],
			EVB_INTERFACE: ['ARDUINO'],
			ACTIVE_DEVICE: ['DEV_AD5933']
		]
	}

	List testMatrix = general.getMatrixCombination(testMap)
	for (int i = 0; i < testMatrix.size(); i++) {
		Map axis = testMatrix[i]
		runTest(axis)
	}
}

def runTest(Map config =[:]) {
	node(label : "${hw.getAgentLabel(platform_name: config.PLATFORM_NAME, evb_ref_id: "PMOD_IA_AD5933")}") {
		ws('workspace/pcg-fw') {
			checkout scm
			try {
				stage("Test-${config.PLATFORM_NAME}-${config.ACTIVE_DEVICE}-${config.EVB_INTERFACE}") {
					// Fetch the stashed files from build stage
					unstash "${config.PLATFORM_NAME}-${config.ACTIVE_DEVICE}-${config.EVB_INTERFACE}"

					echo "^^**^^ ^^**^^ ^^**^^ ^^**^^ ^^**^^ ^^**^^ ^^**^^ ^^**^^ ^^**^^ ^^**^^"                       
					echo "Running on node: '${env.NODE_NAME}'"
					echo "Testing for ${config.PLATFORM_NAME} and ${config.ACTIVE_DEVICE} with ${config.EVB_INTERFACE} interface"
					echo "^^**^^ ^^**^^ ^^**^^ ^^**^^ ^^**^^ ^^**^^ ^^**^^ ^^**^^ ^^**^^ ^^**^^"
					
					platformInfo = hw.getPlatformInfo(platform_name: config.PLATFORM_NAME, evb_ref_id: "PMOD_IA_AD5933")
					echo "Programming MCU platform..."               
					bat "mbedflsh --version"
					// need to ignore return code from mbedflsh as it returns 1 when programming successful
					bat returnStatus: true , script: "mbedflsh --disk ${platformInfo["mountpoint"]} --file ${WORKSPACE}\\${projectName}\\build\\${config.PLATFORM_NAME}\\${TOOLCHAIN}\\${config.PLATFORM_NAME}-${config.ACTIVE_DEVICE}-${config.EVB_INTERFACE}.bin"               
					bat "cd ${projectName}/tests & pytest --rootdir=${WORKSPACE}\\${projectName}\\tests -c pytest.ini --serialnumber ${platformInfo["serialnumber"]}  --serialport ${platformInfo["serialport"]} --mountpoint ${platformInfo["mountpoint"]}"			
					archiveArtifacts allowEmptyArchive: true, artifacts: "${projectName}/tests/output/*.csv"
					junit allowEmptyResults:true, testResults: "**/${projectName}/tests/output/*.xml"
					deleteDir()
				}
			}
			catch (Exception ex) {
				echo "Failed in Test-${config.PLATFORM_NAME}-${config.ACTIVE_DEVICE}-${config.EVB_INTERFACE} stage"
				echo "Caught:${ex}"
				currentBuild.result = 'FAILURE'
				deleteDir()
			}
		}
	}
}

return this;