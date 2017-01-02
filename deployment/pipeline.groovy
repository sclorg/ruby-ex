@Library('ocutil') _

node() {
  stage("Checkout") {
    deleteDir()
    git "https://github.com/omallo/ruby-ex.git"
  }

  def semver = sh(script: "mono /usr/local/GitVersion_3.6.5/GitVersion.exe /showvariable FullSemVer", returnStdout: true).trim()
  echo "semver: ${semver}"

  def config = ocutil.parseConfig(readFile("deployment/config.yaml"))

  stage("Build") {
    ocutil.ocBuild("rubex-dev", "frontend", config.dev.build.frontend)
  }

  stage("Deploy to DEV") {
    ocutil.ocTag("rubex-dev", "frontend", "latest", "dev")
    ocutil.ocDeploy("rubex-dev", "frontend", config.dev.deployment.frontend)
  }

  def isPromoteToTest = false
  stage("Promote to TEST?") {
    isPromoteToTest = input(message: "Promotion", parameters: [booleanParam(defaultValue: false, name: "Promote to TEST?")])
  }

  if (isPromoteToTest) {
    stage("Deploy to TEST") {
      sh "git tag ${semver}"
      sh "git push --tags"

      ocutil.ocTag("rubex-dev", "frontend", "dev", semver)
      ocutil.ocTag("rubex-dev", "frontend", semver, "test")
      ocutil.ocDeploy("rubex-test", "frontend", config.test.deployment.frontend)
    }
  }
}
