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
        sh "./ci/build.sh"
      }
    }

    // run tests for service
    stage("test") {
      echo "test..."
    }

    // push image to docker registry, so can be pulled by Kubernetes manifests
    stage("push image") {
      // need to authenticate to be able to push to docker registry
      withCredentials([
        string(credentialsId: 'GKE_SERVICE_ACCOUNT_USER', variable: 'GKE_SERVICE_ACCOUNT_USER'),
        file(credentialsId: 'GKE_SERVICE_ACCOUNT', variable: 'GKE_SERVICE_ACCOUNT')
      ]) {
        sh """
          set -x
          gcloud auth activate-service-account \${GKE_SERVICE_ACCOUNT_USER} --key-file=\${GKE_SERVICE_ACCOUNT}
          docker login -u _json_key -p "\$(cat ${GKE_SERVICE_ACCOUNT})" https://gcr.io
          docker push ${image}
        """
      }
    }

    // deploy service by applying kubernetes manifests in service repo
    stage("deploy") {
      if (fileExists("manifests")) {
        sh "docker run --rm --volume `pwd`:/service gcr.io/unity-ads-workshop-test/workshop-deployer bash -c 'if kubectl get service ${service}-lb > /dev/null; then kubectl delete service ${service}-lb; fi'"
        sh "docker run --rm --volume `pwd`:/service gcr.io/unity-ads-workshop-test/workshop-deployer bash -c 'kubectl apply -f /service/manifests/service.yaml -f /service/manifests/deployment.yaml'"
        sh "docker run --rm --volume `pwd`:/service gcr.io/unity-ads-workshop-test/workshop-deployer bash -c 'kubectl expose deployment -n workshop ${service} --type=LoadBalancer --name=${service}-lb --port=8080'"
      } else {
        echo "No manifests/ folder found, skipping deployment"
      }
    }
  }
}
