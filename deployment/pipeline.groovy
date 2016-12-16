@Library('ocutil') _

node() {
  stage("Checkout") {
    git "https://github.com/omallo/ruby-ex.git"
  }

  def config = ocutil.parseYaml(readFile("deployment/config.yaml"))

  stage("Build") {
    ocBuild("rubex-dev", "frontend", config.dev.build.frontend)
  }

  stage("Deploy to DEV") {
    ocTag("rubex-dev", "frontend", "latest", "dev")
    ocDeploy("rubex-dev", "frontend", config.dev.deployment.frontend)
  }

  def isPromoteToTest = false
  stage("Promote to TEST?") {
    isPromoteToTest = input(message: "Promotion", parameters: [booleanParam(defaultValue: false, name: "Promote to TEST?")])
  }

  if (isPromoteToTest) {
    stage("Deploy to TEST") {
      ocTag("rubex-dev", "frontend", "dev", "test")
      ocDeploy("rubex-test", "frontend", config.test.deployment.frontend)
    }
  }
}
