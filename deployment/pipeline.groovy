def oc(args) {
  def ocCmd = "/tmp/oc2 --token=`cat /var/run/secrets/kubernetes.io/serviceaccount/token` --server=https://openshift.default.svc.cluster.local --certificate-authority=/run/secrets/kubernetes.io/serviceaccount/ca.crt"
  sh "${ocCmd} " + args
}

node() {
  stage('Build') {
    oc "start-build frontend -w -n rubex-dev"
  }

  stage('Deploy to DEV') {
    // noop
  }

  def isPromoteToTest = false
  stage('Promote to TEST?') {
    isPromoteToTest = input(message: 'Promotion', parameters: [booleanParam(defaultValue: false, name: 'Promote to TEST?')])
  }

  if (isPromoteToTest) {
    stage('Deploy to TEST') {
      oc "tag rubex-dev/frontend:latest rubex-dev/frontend:test"
    }
  }
}
