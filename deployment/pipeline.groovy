node() {
  def ocCmd = "oc --token=`cat /var/run/secrets/kubernetes.io/serviceaccount/token` --server=https://openshift.default.svc.cluster.local --certificate-authority=/run/secrets/kubernetes.io/serviceaccount/ca.crt"

  stage('Build') {
    sh "${ocCmd} start-build frontend -w -n rubex-dev"
  }

  stage('Deploy to DEV') {
    sh "${ocCmd} tag rubex-dev/frontend:latest rubex-dev/frontend:dev"
    sh "${ocCmd} deploy frontend --latest --follow -n rubex-dev"
  }

  def isPromoteToTest = false
  stage('Promote to TEST?') {
    isPromoteToTest = input(message: 'Promotion', parameters: [booleanParam(defaultValue: false, name: 'Promote to TEST?')])
  }

  if (isPromoteToTest) {
    stage('Deploy to TEST') {
      sh "${ocCmd} tag rubex-dev/frontend:dev rubex-dev/frontend:test"
      sh "${ocCmd} deploy frontend --latest --follow -n rubex-test"
    }
  }
}
