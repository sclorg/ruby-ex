node() {
  def ocCmd = "oc --token=`cat /var/run/secrets/kubernetes.io/serviceaccount/token` --server=https://openshift.default.svc.cluster.local --certificate-authority=/run/secrets/kubernetes.io/serviceaccount/ca.crt"

  def buildConfigUrl = "https://raw.githubusercontent.com/omallo/ruby-ex/master/deployment/config/build.yaml"
  def appConfigUrl = "https://raw.githubusercontent.com/omallo/ruby-ex/master/deployment/config/app.yaml"

  stage('Build') {
    sh "${ocCmd} process -f ${buildConfigUrl} -n rubex-dev | ${ocCmd} apply -f - -n rubex-dev"
    sh "${ocCmd} start-build frontend -w -n rubex-dev"
  }

  stage('Deploy to DEV') {
    sh "${ocCmd} process -f ${appConfigUrl} -v ENV=dev -n rubex-dev | ${ocCmd} apply -f - -n rubex-dev"
    sh "${ocCmd} tag rubex-dev/frontend:latest rubex-dev/frontend:dev"
    sh "${ocCmd} rollout latest dc/frontend -n rubex-dev"
    sh "${ocCmd} rollout status dc/frontend -n rubex-dev"
  }

  def isPromoteToTest = false
  stage('Promote to TEST?') {
    isPromoteToTest = input(message: 'Promotion', parameters: [booleanParam(defaultValue: false, name: 'Promote to TEST?')])
  }

  if (isPromoteToTest) {
    stage('Deploy to TEST') {
      sh "${ocCmd} process -f ${appConfigUrl} -v ENV=test -n rubex-test | ${ocCmd} apply -f - -n rubex-test"
      sh "${ocCmd} tag rubex-dev/frontend:dev rubex-dev/frontend:test"
      sh "${ocCmd} rollout latest dc/frontend -n rubex-test"
      sh "${ocCmd} rollout status dc/frontend -n rubex-test"
    }
  }
}
