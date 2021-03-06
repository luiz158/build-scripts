package io.springframework.cloud.compatibility

import io.springframework.cloud.common.AllCloudJobs
import io.springframework.cloud.common.SpringCloudJobs
import javaposse.jobdsl.dsl.DslFactory

import static io.springframework.cloud.common.AllCloudConstants.LATEST_SPRING_VERSION
import static io.springframework.cloud.compatibility.CompatibilityTasks.SPRING_VERSION_VAR
import static io.springframework.cloud.compatibility.SpringCompatibilityBuildMaker.COMPATIBILITY_BUILD_SPRING_SUFFIX

/**
 * Creates the jobs for the Boot Compatibility verifier
 *
 * @author Marcin Grzejszczak
 */
class ManualSpringCompatibilityBuildMaker implements SpringCloudJobs {
	private final DslFactory dsl

	ManualSpringCompatibilityBuildMaker(DslFactory dsl) {
		this.dsl = dsl
	}

	void build() {
		buildAllRelatedJobs()
		dsl.multiJob("spring-cloud-${COMPATIBILITY_BUILD_SPRING_SUFFIX}") {
			parameters {
				stringParam(SPRING_VERSION_VAR, LATEST_SPRING_VERSION, 'Which version of Spring should be used for the build')
			}
			steps {
				phase('spring-compatibility-phase') {
					(AllCloudJobs.BOOT_COMPATIBILITY_BUILD_JOBS).each { String projectName ->
						String prefixedProjectName = prefixJob(projectName)
						phaseJob("${prefixedProjectName}-${COMPATIBILITY_BUILD_SPRING_SUFFIX}".toString()) {
							currentJobParameters()
						}
					}
				}
			}
		}
	}

	void buildAllRelatedJobs() {
		AllCloudJobs.ALL_DEFAULT_JOBS.each { String projectName->
			new SpringCompatibilityBuildMaker(dsl, COMPATIBILITY_BUILD_SPRING_SUFFIX).buildWithoutTests(projectName)
		}
		AllCloudJobs.JOBS_WITHOUT_TESTS.each {
			new SpringCompatibilityBuildMaker(dsl, COMPATIBILITY_BUILD_SPRING_SUFFIX).buildWithoutTests(it)
		}
		new SpringCompatibilityBuildMaker(dsl, COMPATIBILITY_BUILD_SPRING_SUFFIX).buildWithoutTests("spring-cloud-contract")
		new ConsulCompatibilityBuildMaker(dsl, COMPATIBILITY_BUILD_SPRING_SUFFIX).buildWithoutTestsForSpring()
		new SpringCompatibilityBuildMaker(dsl, COMPATIBILITY_BUILD_SPRING_SUFFIX, 'spring-cloud-samples').buildWithoutTests('tests')
	}
}
