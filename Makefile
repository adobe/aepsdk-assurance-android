EXTENSION-LIBRARY-FOLDER-NAME = assurance
TEST-APP-FOLDER-NAME = assurance-testapp
TEST-APP-DEBUG-APK = assurance-testapp-debug.apk

ROOT_DIR=$(shell git rev-parse --show-toplevel)

AAR_FILE_DIR =  $(ROOT_DIR)/code/$(EXTENSION-LIBRARY-FOLDER-NAME)/build/outputs/aar
TEST_APP_APK_OUTPUT_DIR = $(ROOT_DIR)/code/$(TEST-APP-FOLDER-NAME)/build/outputs/apk
ARTIFACTS_DIR = $(ROOT_DIR)/artifacts

clean:
	(rm -rf $(AAR_FILE_DIR))
	(./code/gradlew -p code clean)

checkformat:
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) spotlessCheck)

ci-lint: checkformat

format:
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) spotlessApply)

## This is a custom app build command to generate and copy the test app apk
## Use assemble-app for general usecases
build-app:
	(./code/gradlew -p code clean)
	# Delete TEST-APP-DEBUG-APK-NAME from the contents of ARTIFACTS_DIR
	(rm -rf $(ARTIFACTS_DIR)/$(TEST-APP-DEBUG-APK))
	# Create the artifacts directory if it did not exist
	(mkdir -p $(ARTIFACTS_DIR))
	# Build the Test App APK
	(./code/gradlew -p code/$(TEST-APP-FOLDER-NAME) assemble)
	# Copy the debug test apk generated at TEST_APP_APK_OUTPUT_DIR to ARTIFACTS_DIR
	(cp -r $(TEST_APP_APK_OUTPUT_DIR)/debug/$(TEST-APP-DEBUG-APK) $(ARTIFACTS_DIR))

unit-test:
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) testPhoneDebugUnitTest)

unit-test-coverage:
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) createPhoneDebugUnitTestCoverageReport)

functional-test:
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) uninstallPhoneDebugAndroidTest)
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) connectedPhoneDebugAndroidTest)

functional-test-coverage:
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) uninstallPhoneDebugAndroidTest)
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) createPhoneDebugAndroidTestCoverageReport)

javadoc:
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) javadocJar)

assemble-phone:
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) assemblePhone)

assemble-phone-debug:
	(./code/gradlew -p code/${EXTENSION-LIBRARY-FOLDER-NAME}  assemblePhoneDebug)

assemble-phone-release:
	(./code/gradlew -p code/${EXTENSION-LIBRARY-FOLDER-NAME}  assemblePhoneRelease)

assemble-app:
	(./code/gradlew -p code/$(TEST-APP-FOLDER-NAME) assemble)

ci-publish-staging: clean assemble-phone
	(./code/gradlew -p code/${EXTENSION-LIBRARY-FOLDER-NAME} publishReleasePublicationToSonatypeRepository --stacktrace)

ci-publish-main: clean assemble-phone
	(./code/gradlew -p code/${EXTENSION-LIBRARY-FOLDER-NAME} publishReleasePublicationToSonatypeRepository -Prelease)

ci-publish-maven-local: clean assemble-phone
	(./code/gradlew -p code/${EXTENSION-LIBRARY-FOLDER-NAME} publishReleasePublicationToMavenLocal -x signReleasePublication)

ci-publish-maven-local-jitpack: clean assemble-phone
	(./code/gradlew -p code/${EXTENSION-LIBRARY-FOLDER-NAME} publishReleasePublicationToMavenLocal -Pjitpack -x signReleasePublication)
