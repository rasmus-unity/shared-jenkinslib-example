def call(body) {

  service = sh([returnStdout: true, script: 'echo $JOB_NAME | cut -d \"/\" -f 2']).trim()
  // revision = sh([returnStdout: true, script: 'git log --format=\"%H\" -n 1']).trim()
  // image = "gcr.io/unity-ads-workshop-test/${service}:${revision}"

  echo "service: ${service}"
  // echo "revision: ${revision}"

  stage("build") {
    withEnv([
      "image=${image}",
    ]) {
      "sh/build.sh".execute()
    }
  }
  stage("test") {
    echo "test..."
  }
  stage("deploy") {
    echo "deploy..."
  }
}