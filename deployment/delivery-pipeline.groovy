@Library('occd') _

node() {
  stage("Checkout") {
    deleteDir()
    git(url: "https://github.com/omallo/ruby-ex.git", branch: "master", credentialsId: "github-omallo")
  }

  def config = occd.parseConfig(readFile("deployment/config.yaml"))

  def buildVersion = occd.getBuildVersion()
  def releaseVersion = occd.getReleaseVersion()

  stage("Build") {
    echo "versions: build=${buildVersion}, next-release=${releaseVersion}"
    sh "sed -e 's/{{BUILD_VERSION}}/${buildVersion}/g' -i config.ru"
    occd.build("rubex-dev", "frontend-master", config.dev.build.frontend)
  }

  stage("Deploy to DEV") {
    occd.tag("rubex-dev", "frontend", "b-master", "dev")
    occd.rollout("rubex-dev", "frontend", config.dev.deployment.frontend)
  }

  def isPromoteToTest = false
  stage("Promote to TEST?") {
    isPromoteToTest = input(message: "Promotion", parameters: [booleanParam(defaultValue: false, name: "Promote to TEST?")])
  }

  if (isPromoteToTest) {
    stage("Deploy to TEST") {
      withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'github-omallo', usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD']]) {
        sh "git tag ${releaseVersion}"
        sh "git push --tags https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/omallo/ruby-ex.git"
      }

      occd.tag("rubex-dev", "frontend", "dev", releaseVersion)
      occd.tag("rubex-dev", "frontend", releaseVersion, "test")
      occd.rollout("rubex-test", "frontend", config.test.deployment.frontend)
    }
  }
}
