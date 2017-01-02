@Library('ocutil') _

node() {
  stage("Checkout") {
    deleteDir()
    git(url: "https://github.com/omallo/ruby-ex.git", credentialsId: "github-omallo")
  }

  def gitVersionCmd = "mono /usr/local/GitVersion_3.6.5/GitVersion.exe"
  def buildVersion = sh(script: "${gitVersionCmd} /showvariable FullSemVer", returnStdout: true).trim()
  def tagVersion = sh(script: "${gitVersionCmd} /showvariable MajorMinorPatch", returnStdout: true).trim()
  echo "versions: buildVersion=${buildVersion}, tagVersion=${tagVersion}"

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
      sh "git tag ${releaseVersion}"
      sh "git push --tags"

      ocutil.ocTag("rubex-dev", "frontend", "dev", releaseVersion)
      ocutil.ocTag("rubex-dev", "frontend", releaseVersion, "test")
      ocutil.ocDeploy("rubex-test", "frontend", config.test.deployment.frontend)
    }
  }
}
