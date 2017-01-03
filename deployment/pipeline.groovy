@Library('ocutil') _

node() {
  def gitVersionCmd = "mono /usr/local/GitVersion/GitVersion.exe"

  stage("Checkout") {
    deleteDir()
    git(url: "https://github.com/omallo/ruby-ex.git", credentialsId: "github-omallo")
  }

  def config = ocutil.parseConfig(readFile("deployment/config.yaml"))

  def fullSemVer = sh(script: "${gitVersionCmd} /showvariable FullSemVer", returnStdout: true).trim()
  def buildVersion = "${fullSemVer}+${currentBuild.number}"
  def tagVersion = sh(script: "${gitVersionCmd} /showvariable MajorMinorPatch", returnStdout: true).trim()
  echo "versions: buildVersion=${buildVersion}, tagVersion=${tagVersion}"

  stage("Build") {
    sh "sed -e 's/{{BUILD_VERSION}}/${buildVersion}/g' -i config.ru"
    ocutil.build("rubex-dev", "frontend", config.dev.build.frontend)
  }

  stage("Deploy to DEV") {
    ocutil.tag("rubex-dev", "frontend", "latest", "dev")
    ocutil.rollout("rubex-dev", "frontend", config.dev.deployment.frontend)
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

      ocutil.tag("rubex-dev", "frontend", "dev", tagVersion)
      ocutil.tag("rubex-dev", "frontend", tagVersion, "test")
      ocutil.rollout("rubex-test", "frontend", config.test.deployment.frontend)
    }
  }
}
