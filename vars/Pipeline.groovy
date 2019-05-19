def call(body) {
  node {
    checkout scm

    service = sh([returnStdout: true, script: 'echo $JOB_NAME | cut -d \"/\" -f 2']).trim()
    revision = sh([returnStdout: true, script: 'git log --format=\"%H\" -n 1']).trim()
    image = "gcr.io/unity-ads-workshop-test/${service}:${revision}"

    // build docker image
    stage("build") {
      withEnv([
        "image=${image}",
      ]) {
        sh """
          find .
          ./ci/build.sh
        """
      }
    }

    // run tests for service
    stage("test") {
      echo "test..."
    }

    // push image to docker registry, so can be pulled by Kubernetes manifests
    stage("push image") {
      sh "docker push ${image}"
    }

    // deploy service by applying kubernetes manifests in service repo
    stage("deploy") {
      if (fileExists("manifests")) {
        sh "docker run --rm --volume `pwd`:/service gcr.io/unity-ads-workshop-test/workshop-deployer bash -c 'kubectl apply -f /service/manifests/service.yaml' -f /service/manifests/deployment.yaml'"
        sh "docker run --rm --volume `pwd`:/service gcr.io/unity-ads-workshop-test/workshop-deployer bash -c 'kubectl expose deployment -n workshop ${service} --type=LoadBalancer --name=${service}-lb --port=8080'"
      } else {
        echo "No manifests/ folder found, skipping deployment"
      }
    }
  }
}