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
      withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'github-omallo', usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD']]) {
        sh "git tag ${tagVersion}"
        sh "git push --tags https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/omallo/ruby-ex.git"
      }

      ocutil.ocTag("rubex-dev", "frontend", "dev", tagVersion)
      ocutil.ocTag("rubex-dev", "frontend", tagVersion, "test")
      ocutil.ocDeploy("rubex-test", "frontend", config.test.deployment.frontend)
    }
  }
}
