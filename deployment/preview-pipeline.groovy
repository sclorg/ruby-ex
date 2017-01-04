@Library('occd') _

node() {
  stage("Checkout") {
    deleteDir()
    git(url: "https://github.com/omallo/ruby-ex.git", branch: "${BRANCH}", credentialsId: "github-omallo")
  }

  def config = occd.parseConfig(readFile("deployment/config.yaml"))

  stage("Deploy to PREV") {
    occd.tag("rubex-dev", "frontend", "b-${BRANCH}", "prev")
    occd.rollout("rubex-prev", "frontend", config.prev.deployment.frontend)
  }
}
