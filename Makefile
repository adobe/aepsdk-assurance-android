EXTENSION-LIBRARY-FOLDER-NAME = assurance
TEST-APP-FOLDER-NAME = assurance-testapp

BUILD-ASSEMBLE-LOCATION = ./ci/assemble
ROOT_DIR=$(shell git rev-parse --show-toplevel)

PROJECT_NAME = $(shell cat $(ROOT_DIR)/code/gradle.properties | grep "moduleProjectName" | cut -d'=' -f2)
AAR_NAME = $(shell cat $(ROOT_DIR)/code/gradle.properties | grep "moduleAARName" | cut -d'=' -f2)
MODULE_NAME = $(shell cat $(ROOT_DIR)/code/gradle.properties | grep "moduleName" | cut -d'=' -f2)
LIB_VERSION = $(shell cat $(ROOT_DIR)/code/gradle.properties | grep "moduleVersion" | cut -d'=' -f2)
SOURCE_FILE_DIR =  $(ROOT_DIR)/code/$(PROJECT_NAME)
AAR_FILE_DIR =  $(ROOT_DIR)/code/$(PROJECT_NAME)/build/outputs/aar

clean:
	(rm -rf ci)
	(rm -rf $(AAR_FILE_DIR))
	(./code/gradlew -p code clean)

create-ci: clean
	(mkdir -p ci)

ci-checkformat:
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) spotlessCheck)

ci-format:
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) spotlessApply)

ci-build: create-ci
	(mkdir -p ci/assemble)

	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) lint)
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) assemblePhone)

ci-build-app:
	(./code/gradlew -p code/$(TEST-APP-FOLDER-NAME) assemble)

ci-unit-test: create-ci
	(mkdir -p ci/unit-test)
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) platformUnitTestJacocoReport)
	(cp -r ./code/$(EXTENSION-LIBRARY-FOLDER-NAME)/build ./ci/unit-test/)

ci-functional-test: create-ci
	(mkdir -p ci/functional-test)
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) uninstallPhoneDebugAndroidTest)
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) connectedPhoneDebugAndroidTest platformFunctionalTestJacocoReport)
	(cp -r ./code/$(EXTENSION-LIBRARY-FOLDER-NAME)/build ./ci/functional-test)

ci-javadoc: create-ci
	(mkdir -p ci/javadoc)
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) javadocPublic > ci/javadocPublic.log 2>&1)
	(cp -r ./code/$(EXTENSION-LIBRARY-FOLDER-NAME)/build ./ci/javadoc)

ci-generate-library-debug:
	(./code/gradlew -p code/${EXTENSION-LIBRARY-FOLDER-NAME}  assemblePhoneDebug)

ci-generate-library-release:
	(./code/gradlew -p code/${EXTENSION-LIBRARY-FOLDER-NAME}  assemblePhoneRelease)

build-release:
	(./code/gradlew -p code/${EXTENSION-LIBRARY-FOLDER-NAME} clean lint assemblePhoneRelease)

ci-publish-staging: clean build-release
	(./code/gradlew -p code/${EXTENSION-LIBRARY-FOLDER-NAME} publishReleasePublicationToSonatypeRepository --stacktrace)

ci-publish-main: clean build-release
	(./code/gradlew -p code/${EXTENSION-LIBRARY-FOLDER-NAME} publishReleasePublicationToSonatypeRepository -Prelease)
