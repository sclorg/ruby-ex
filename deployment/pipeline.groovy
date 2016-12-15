import org.yaml.snakeyaml.Yaml

def getOcCmd() {
  return "oc --token=`cat /var/run/secrets/kubernetes.io/serviceaccount/token` --server=https://openshift.default.svc.cluster.local --certificate-authority=/run/secrets/kubernetes.io/serviceaccount/ca.crt"
}

def getReplicas(namespace, name) {
  def ocCmd = getOcCmd()
  return sh(script: "${ocCmd} get dc ${name} --template='{{ .spec.replicas }}' -n ${namespace} || true", returnStdout: true).trim()
}

@NonCPS
def parseYaml(content) {
  return new Yaml().load(content)
}

def ocTemplateParametersAsCommandLineOpt(parameters) {
  return parameters.collect { parameter -> "-v ${parameter}" }.join(" ")​​​​
}

def ocApplyTemplate(namespace, config) {
  ocApplyTemplate(namespace, config.manifest, config.parameters)
}

def ocApplyTemplate(namespace, manifest, parameters) {
  def ocCmd = getOcCmd()
  def parametersOpt = ocTemplateParametersAsCommandLineOpt(parameters)
  sh "${ocCmd} process -f ${manifest} ${parametersOpt} -n ${namespace} | ${ocCmd} apply -f - -n ${namespace}"
}

def ocDelete(namespace, target) {
  def ocCmd = getOcCmd()
  sh "${ocCmd} delete ${target.type}/${target.name} -n ${namespace}"
}

def ocBuild(namespace, name, config) {
  def ocCmd = getOcCmd()

  config.delete.each { target -> ocDelete(namespace, target) }

  config.templates.each { template -> ocApplyTemplate(namespace, template) }

  sh "${ocCmd} start-build ${name} -w -n ${namespace}"
}

def ocTag(isNamespace, isName, sourceTag, targetTag) {
  sh "${ocCmd} tag ${isNamespace}/${isName}:${sourceTag} ${isNamespace}/${isName}:${targetTag}"
}

def ocDeploy(namespace, name, config) {
  def replicas = getReplicas(namespace, name)

  config.delete.each { target -> ocDelete(namespace, target) }

  config.templates.each { template -> 
    def manifest = template.manifest
    def parameters = template.parameters.clone()
    if (replicas) {
      parameters["REPLICAS"] = replicas
    }
    ocApplyTemplate(namespace, manifest, parameters)
  }

  sh "${ocCmd} rollout latest dc/${name} -n ${namespace}"
  sh "${ocCmd} rollout status dc/${name} -n ${namespace}"
}

node() {
  stage("Checkout") {
    git "https://github.com/omallo/ruby-ex.git"
  }

  def config = parseYaml(readFile("deployment/config.yaml"))

  stage("Build") {
    ocBuild(namespace: "rubex-dev", name: "frontend", config: config.dev.build.frontend)
  }

  stage("Deploy to DEV") {
    ocTag(isNamespace: "rubex-dev", isName: "frontend", sourceTag: "latest", targetTag: "dev")
    ocDeploy(namespace: "rubex-dev", name: "frontend", config: config.dev.deployment.frontend)
  }

  def isPromoteToTest = false
  stage("Promote to TEST?") {
    isPromoteToTest = input(message: "Promotion", parameters: [booleanParam(defaultValue: false, name: "Promote to TEST?")])
  }

  if (isPromoteToTest) {
    stage("Deploy to TEST") {
      ocTag(isNamespace: "rubex-dev", isName: "frontend", sourceTag: "dev", targetTag: "test")
      ocDeploy(namespace: "rubex-test", name: "frontend", config: config.test.deployment.frontend)
    }
  }
}
