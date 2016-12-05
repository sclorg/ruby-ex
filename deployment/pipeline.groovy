def oc(args) {
  sh getOcCmd(args)
}

def ocPipe(args1, args2) {
  def ocCmd1 = getOcCmd(args1)
  def ocCmd2 = getOcCmd(args2)
  sh "${ocCmd1} | ${ocCmd2}"
}

def getOcCmd(args) {
  def ocBin = "oc --token=`cat /var/run/secrets/kubernetes.io/serviceaccount/token` --server=https://openshift.default.svc.cluster.local --certificate-authority=/run/secrets/kubernetes.io/serviceaccount/ca.crt"
  return "${ocBin} ${args}" 
}

node() {
  stage('Build') {
    oc "start-build frontend -w -n rubex-dev"
  }

  stage('Deploy to DEV') {
    oc "tag rubex-dev/frontend:latest rubex-dev/frontend:dev"
    oc "deploy frontend --latest --follow -n rubex-dev"
  }

  def isPromoteToTest = false
  stage('Promote to TEST?') {
    isPromoteToTest = input(message: 'Promotion', parameters: [booleanParam(defaultValue: false, name: 'Promote to TEST?')])
  }

  if (isPromoteToTest) {
    stage('Deploy to TEST') {
      oc "tag rubex-dev/frontend:dev rubex-dev/frontend:test"
      oc "deploy frontend --latest --follow -n rubex-test"
    }
  }
}
