@Library('occd') _

node() {
  stage("Deploy to PREV") {
    occd.tag("rubex-dev", "frontend", "${SOURCE_IMAGE_STREAM_TAG}", "prev")
    occd.rollout("rubex-prev", "frontend", config.prev.deployment.frontend)
  }
}
