@Library('occd') _

node() {
  def config = occd.parseConfig(readFile("deployment/config.yaml"))

  stage("Deploy to PREV") {
    occd.tag("rubex-dev", "frontend", "b-${BRANCH}", "prev")
    occd.rollout("rubex-prev", "frontend", config.prev.deployment.frontend)
  }
}
