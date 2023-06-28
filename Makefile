EXTENSION-LIBRARY-FOLDER-NAME = assurance
TEST-APP-FOLDER-NAME = assurance-testapp
TEST-APP-DEBUG-APK = assurance-testapp-debug.apk

BUILD-ASSEMBLE-LOCATION = ./ci/assemble
ROOT_DIR=$(shell git rev-parse --show-toplevel)

PROJECT_NAME = $(shell cat $(ROOT_DIR)/code/gradle.properties | grep "moduleProjectName" | cut -d'=' -f2)
SOURCE_FILE_DIR =  $(ROOT_DIR)/code/$(PROJECT_NAME)
AAR_FILE_DIR =  $(ROOT_DIR)/code/$(PROJECT_NAME)/build/outputs/aar
TEST_APP_APK_OUTPUT_DIR = $(ROOT_DIR)/code/$(TEST-APP-FOLDER-NAME)/build/outputs/apk
ARTIFACTS_DIR = $(ROOT_DIR)/artifacts

clean:
	(rm -rf $(AAR_FILE_DIR))
	(./code/gradlew -p code clean)

checkformat:
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) spotlessCheck)

format:
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) spotlessApply)

build:
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) lint)
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) assemblePhone)

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
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) platformUnitTestJacocoReport)

functional-test:
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) uninstallPhoneDebugAndroidTest)
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) connectedPhoneDebugAndroidTest platformFunctionalTestJacocoReport)

javadoc:
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) javadocPublic)

generate-library-debug:
	(./code/gradlew -p code/${EXTENSION-LIBRARY-FOLDER-NAME}  assemblePhoneDebug)

generate-library-release:
	(./code/gradlew -p code/${EXTENSION-LIBRARY-FOLDER-NAME}  assemblePhoneRelease)

build-release:
	(./code/gradlew -p code/${EXTENSION-LIBRARY-FOLDER-NAME} clean lint assemblePhoneRelease)

ci-publish-staging: clean build-release
	(./code/gradlew -p code/${EXTENSION-LIBRARY-FOLDER-NAME} publishReleasePublicationToSonatypeRepository --stacktrace)

ci-publish-main: clean build-release
	(./code/gradlew -p code/${EXTENSION-LIBRARY-FOLDER-NAME} publishReleasePublicationToSonatypeRepository -Prelease)

ci-publish-maven-local: clean build-release
	(./code/gradlew -p code/${EXTENSION-LIBRARY-FOLDER-NAME} publishReleasePublicationToMavenLocal -x signReleasePublication)

ci-publish-jitpack: clean build-release
	(./code/gradlew -p code/${EXTENSION-LIBRARY-FOLDER-NAME} publishReleasePublicationToMavenLocal -Pjitpack -x signReleasePublication)
