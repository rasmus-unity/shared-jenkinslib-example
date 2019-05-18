def call(body) {

  service = 'echo $JOB_NAME | cut -d \"/\" -f 2'.execute().text().trim()
  revision = 'git log --format=\"%H\" -n 1'.execute().text().trim()
  image = "gcr.io/unity-ads-workshop-test/${service}:${revision}"

  echo "service: ${service}"
  echo "revision: ${revision}"

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