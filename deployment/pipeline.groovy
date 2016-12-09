import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor

def getOcCmd() {
  return "oc --token=`cat /var/run/secrets/kubernetes.io/serviceaccount/token` --server=https://openshift.default.svc.cluster.local --certificate-authority=/run/secrets/kubernetes.io/serviceaccount/ca.crt"
}

def getReplicasOrDefault(deploymentConfig, project, defaultReplicas) {
  def ocCmd = getOcCmd()
  def replicas = sh(script: "${ocCmd} get dc ${deploymentConfig} --template='{{ .spec.replicas }}' -n ${project} || true", returnStdout: true).trim()
  return replicas ?: defaultReplicas
}

@NonCPS
def getConfig() {
  def yamlAsString = "{JYaml: Original Java Implementation, "
            + "JvYaml: Java port of RbYaml, SnakeYAML: Java 5 / YAML 1.1, "
            + "YamlBeans: To/from JavaBeans}";
  return new Yaml().load(yamlAsString)
}

node() {
  def ocCmd = getOcCmd()

  def buildManifest = "deployment/manifests/build.yaml"
  def appManifest = "deployment/manifests/app.yaml"

  stage("Build") {
    git "https://github.com/omallo/ruby-ex.git"
    
    println "teeeeeest start"
    def config = getConfig()
    println config.getClass()
    println "teeeeeest end"

    sh "${ocCmd} process -f ${buildManifest} -n rubex-dev | ${ocCmd} apply -f - -n rubex-dev"
    sh "${ocCmd} start-build frontend -w -n rubex-dev"
  }

  stage("Deploy to DEV") {
    def replicas = getReplicasOrDefault("frontend", "rubex-dev", 1)
    sh "${ocCmd} process -f ${appManifest} -v ENV=dev -v REPLICAS=${replicas} -n rubex-dev | ${ocCmd} apply -f - -n rubex-dev"
    sh "${ocCmd} tag rubex-dev/frontend:latest rubex-dev/frontend:dev"
    sh "${ocCmd} rollout latest dc/frontend -n rubex-dev"
    sh "${ocCmd} rollout status dc/frontend -n rubex-dev"
  }

  def isPromoteToTest = false
  stage("Promote to TEST?") {
    isPromoteToTest = input(message: "Promotion", parameters: [booleanParam(defaultValue: false, name: "Promote to TEST?")])
  }

  if (isPromoteToTest) {
    stage("Deploy to TEST") {
      def replicas = getReplicasOrDefault("frontend", "rubex-test", 2)
      sh "${ocCmd} process -f ${appManifest} -v ENV=test -v REPLICAS=${replicas} -n rubex-test | ${ocCmd} apply -f - -n rubex-test"
      sh "${ocCmd} tag rubex-dev/frontend:dev rubex-dev/frontend:test"
      sh "${ocCmd} rollout latest dc/frontend -n rubex-test"
      sh "${ocCmd} rollout status dc/frontend -n rubex-test"
    }
  }
}
