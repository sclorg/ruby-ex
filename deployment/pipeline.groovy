@Library('ocutil') _

node() {
  stage("Checkout") {
    git "https://github.com/omallo/ruby-ex.git"
  }

  def config = ocutil.parseConfig(readFile("deployment/config.yaml"))

  stage("Build") {
    ocutil.ocBuild namespace: "rubex-dev", name: "frontend", config: config.dev.build.frontend
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
      ocutil.ocTag("rubex-dev", "frontend", "dev", "test")
      ocutil.ocDeploy("rubex-test", "frontend", config.test.deployment.frontend)
    }
  }
}
