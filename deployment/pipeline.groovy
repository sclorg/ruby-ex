@Library('ocutil') _

node() {
  def gitVersionCmd = "mono /usr/local/GitVersion/GitVersion.exe"

  stage("Checkout") {
    deleteDir()
    git(url: "https://github.com/omallo/ruby-ex.git", credentialsId: "github-omallo")
  }

  def config = ocutil.parseConfig(readFile("deployment/config.yaml"))

  def buildVersion = sh(script: "${gitVersionCmd} /showvariable FullSemVer", returnStdout: true).trim()
  def tagVersion = sh(script: "${gitVersionCmd} /showvariable MajorMinorPatch", returnStdout: true).trim()
  echo "versions: buildVersion=${buildVersion}, tagVersion=${tagVersion}"

  stage("Build") {
    sh "sed -e 's/{{BUILD_VERSION}}/${buildVersion}/g' -i config.ru"
    ocutil.ocBuild("rubex-dev", "frontend", config.dev.build.frontend)
  }

  stage("Deploy to DEV") {
    ocutil.ocTag("rubex-dev", "frontend", "latest", "dev")
    ocutil.ocRollout("rubex-dev", "frontend", config.dev.deployment.frontend)
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
      ocutil.ocRollout("rubex-test", "frontend", config.test.deployment.frontend)
    }
  }
}
