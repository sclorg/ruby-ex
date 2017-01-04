@Library('occd') _

node() {
  stage("Checkout") {
    deleteDir()
    git(url: "https://github.com/omallo/ruby-ex.git", branch: "${FEATURE_BRANCH}", credentialsId: "github-omallo")
  }

  stage("Build") {
    def buildVersion = occd.getFeatureBuildVersion("${FEATURE_BRANCH}")
    echo "versions: build=${buildVersion}"

    sh "sed -e 's/{{BUILD_VERSION}}/${buildVersion}/g' -i config.ru"

    occd.build("rubex-dev", "frontend-${FEATURE_BRANCH}", "deployment/manifests/build.yaml", "${FEATURE_BRANCH}")
  }
}
