@Library('occd') _

node() {
  stage("Deploy to PREV") {
    occd.tag("rubex-dev", "frontend", "b-${BRANCH}", "prev")
    occd.rollout("rubex-prev", "frontend", config.prev.deployment.frontend)
  }
}
