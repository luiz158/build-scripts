package io.springframework.cloud.ci

import io.springframework.cloud.common.SpringCloudNotification
import io.springframework.common.job.Cron
import io.springframework.common.job.JdkConfig
import io.springframework.common.job.SlackPlugin
import javaposse.jobdsl.dsl.DslFactory
/**
 * @author Marcin Grzejszczak
 */
class SleuthMemoryBenchmarksBuildMaker implements SpringCloudNotification, JdkConfig, Cron {
	private final DslFactory dsl

	SleuthMemoryBenchmarksBuildMaker(DslFactory dsl) {
		this.dsl = dsl
	}

	void buildSleuth() {
		doBuildSleuth(everySunday())
	}

	private void doBuildSleuth(String cronExpr) {
		dsl.job('spring-cloud-sleuth-memory-benchmark-ci') {
			triggers {
				cron cronExpr
			}
			scm {
				git {
					remote {
						url "https://github.com/marcingrzejszczak/sleuth-memory-benchmarks"
						branch 'master'
					}
				}
			}
			wrappers {
				timestamps()
				colorizeOutput()
				timeout {
					noActivity(300)
					failBuild()
					writeDescription('Build failed due to timeout after {0} minutes of inactivity')
				}
			}
			jdk jdk8()
			def numbers = [100, 500, 1000]
			steps {
				numbers.each { int no ->
					shell("""#!/bin/bash
					echo -e "Running example benchmarks with actuator for [${no}] requests"
					WITH_ACTUATOR=yes NO_OF_REQUESTS=${no} ./scripts/runAcceptanceTests.sh | tee log_${no}_actuator.log
					./scripts/kill.sh || echo "Continuing..."
					""")
					shell("""#!/bin/bash
					echo -e "Running example benchmarks without actuator for [${no}] requests"
					WITH_ACTUATOR=no NO_OF_REQUESTS=${no} ./scripts/runAcceptanceTests.sh | tee log_${no}_no_actuator.log
					./scripts/kill.sh || echo "Continuing..."
					""")
				}
			}
			configure {
				SlackPlugin.slackNotification(it as Node) {
					room(cloudRoom())
				}
			}
			publishers {
				numbers.each { int no ->
					archiveArtifacts("log_${no}_actuator.log")
					archiveArtifacts("log_${no}_no_actuator.log")
				}
			}
		}
	}
}
