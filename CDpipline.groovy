pipeline {
agent any

    stages {
        stage('Docker Deploy To Container') {
            steps {
                script{
                    withDockerRegistry(credentialsId: 'Dockerhub', toolName: 'docker') {
                        sh "docker run -d --name portfolio -p 80:8070 prrague/portfolio:latest"
                    }
                }
            }
        }
    }
}