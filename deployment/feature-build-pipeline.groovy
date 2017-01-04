@Library('occd') _

node() {
  stage("Checkout") {
    deleteDir()
    git(url: "https://github.com/omallo/ruby-ex.git", branch: "${BRANCH}", credentialsId: "github-omallo")
  }

  stage("Build") {
    def buildVersion = occd.getFeatureBuildVersion("${BRANCH}")
    echo "versions: build=${buildVersion}"

    sh "sed -e 's/{{BUILD_VERSION}}/${buildVersion}/g' -i config.ru"

    occd.build("rubex-dev", "frontend-${BRANCH}", "deployment/manifests/build.yaml", "${BRANCH}")
  }
}
