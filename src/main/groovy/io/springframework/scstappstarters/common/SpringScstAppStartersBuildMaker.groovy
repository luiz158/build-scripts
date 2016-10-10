package io.springframework.scstappstarters.common

import io.springframework.common.Cron
import io.springframework.common.JdkConfig
import io.springframework.common.Maven
import io.springframework.common.TestPublisher
import javaposse.jobdsl.dsl.DslFactory

import static io.springframework.common.Artifactory.artifactoryMaven3Configurator
import static io.springframework.common.Artifactory.artifactoryMavenBuild
/**
 * @author Marcin Grzejszczak
 */
class SpringScstAppStartersBuildMaker implements JdkConfig, TestPublisher,
        Cron, SpringScstAppStarterJobs, Maven {

    private final DslFactory dsl
    final String organization
    final String project

    final String branchToBuild = "master"

    SpringScstAppStartersBuildMaker(DslFactory dsl, String organization, String project) {
        this.dsl = dsl
        this.organization = organization
        this.project = project
    }

    /**
     * Dirty hack cause Jenkins is not inserting Maven to path...
     * Requires using Maven3 installation before calling
     *
     * TODO: This doesn't belong here
     */
    String mavenBin() {
        return "/opt/jenkins/data/tools/hudson.tasks.Maven_MavenInstallation/maven33/apache-maven-3.3.9/bin/"
    }

    /**
     * TODO: This doesn't belong here
     */
    String setupGitCredentials() {
        return """
					set +x
					git config user.name "${githubUserName()}"
					git config user.email "${githubEmail()}"
					git config credential.helper "store --file=/tmp/gitcredentials"
					echo "https://\$${githubRepoUserNameEnvVar()}:\$${githubRepoPasswordEnvVar()}@github.com" > /tmp/gitcredentials
					set -x
				"""
    }

    /**
     * TODO: This doesn't belong here
     */
    String cleanGitCredentials() {
        return "rm -rf /tmp/gitcredentials"
    }

    void deploy() {
        dsl.job("${prefixJob(project)}-${branchToBuild}-ci") {
            triggers {
                githubPush()
            }
            jdk jdk8()
            scm {
                git {
                    remote {
                        url "https://github.com/${organization}/${project}"
                        branch branchToBuild
                    }
                }
            }
            steps {
                maven {
                    mavenInstallation(maven32())
                    goals('install -U -DskipTests')
                }
                shell("""#!/bin/bash -x
					export MAVEN_PATH=${mavenBin()}
					${setupGitCredentials()}
					echo "Generating and build apps"
					./generateApps.sh
                    cd apps
                    mvn clean install
					${cleanGitCredentials()}
					""")
            }
            configure {
                artifactoryMavenBuild(it as Node) {
                    mavenVersion(maven33())
                    goals('clean install')
                }
                artifactoryMaven3Configurator(it as Node)
            }
            //ENABLE ONCE WE HAVE TESTS
//				publishers {
//					archiveJunit mavenJUnitResults()
//					archiveJunit mavenJUnitFailsafeResults()
//				}
        }

    }
}