def getOcCmd() {
  return "oc --token=`cat /var/run/secrets/kubernetes.io/serviceaccount/token` --server=https://openshift.default.svc.cluster.local --certificate-authority=/run/secrets/kubernetes.io/serviceaccount/ca.crt"
}

def getReplicasOpt(deploymentConfig, project) {
  def ocCmd = getOcCmd()
  def replicas = sh(script: "${ocCmd} get dc ${deploymentConfig} --template='{{ .spec.replicas }}' -n ${project}", returnStdout: true).trim()
  return replicas ? "-v REPLICAS=${replicas}" : ""
}

node() {
  def ocCmd = getOcCmd()

  def buildConfigFile = "deployment/config/build.yaml"
  def appConfigFile = "deployment/config/app.yaml"

  stage('Build') {
    git "https://github.com/omallo/ruby-ex.git"
    sh "${ocCmd} process -f ${buildConfigFile} -n rubex-dev | ${ocCmd} apply -f - -n rubex-dev"
    sh "${ocCmd} start-build frontend -w -n rubex-dev"
  }

  stage('Deploy to DEV') {
    def replicasOpt = getReplicasOpt("frontend", "rubex-dev")
    sh "${ocCmd} process -f ${appConfigFile} -v ENV=dev ${replicasOpt} -n rubex-dev | ${ocCmd} apply -f - -n rubex-dev"
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
      def replicasOpt = getReplicasOpt("frontend", "rubex-test")
      sh "${ocCmd} process -f ${appConfigFile} -v ENV=test ${replicasOpt} -n rubex-test | ${ocCmd} apply -f - -n rubex-test"
      sh "${ocCmd} tag rubex-dev/frontend:dev rubex-dev/frontend:test"
      sh "${ocCmd} rollout latest dc/frontend -n rubex-test"
      sh "${ocCmd} rollout status dc/frontend -n rubex-test"
    }
  }
}
