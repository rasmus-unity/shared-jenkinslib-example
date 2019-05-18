def call(body) {
  node {
    checkout scm

    service = sh([returnStdout: true, script: 'echo $JOB_NAME | cut -d \"/\" -f 2']).trim()
    revision = sh([returnStdout: true, script: 'git log --format=\"%H\" -n 1']).trim()
    image = "gcr.io/unity-ads-workshop-test/${service}:${revision}"

    stage("build") {
      withEnv([
        "image=${image}",
      ]) {
        "./ci/build.sh".execute()
      }
    }
    stage("test") {
      echo "test..."
    }
    stage("deploy") {
      if (fileExists("manifests")) {
        "docker run --rm --volume `pwd`:/service gcr.io/unity-ads-workshop-test/workshop-deployer bash -c 'kubectl apply -f /service/manifests/service.yaml' -f /service/manifests/deployment.yaml'".execute()
        "docker run --rm --volume `pwd`:/service gcr.io/unity-ads-workshop-test/workshop-deployer bash -c 'kubectl expose deployment -n workshop ${service} --type=LoadBalancer --name=${service}-lb --port=8080'".execute()
      } else {
        echo "No manifests/ folder found, skipping deployment"
      }
    }
  }
}