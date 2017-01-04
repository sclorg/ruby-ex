@Library('occd') _

node() {
  stage("Checkout") {
    deleteDir()
    git(url: "https://github.com/omallo/ruby-ex.git", branch: "${FEATURE_BRANCH}", credentialsId: "github-omallo")
  }
}
