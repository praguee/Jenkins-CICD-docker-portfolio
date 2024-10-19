Jenkins CI/CD Pipeline for Portfolio Website

This repository contains a Jenkins CI/CD pipeline for building, analyzing, and deploying a static portfolio website. The pipeline includes SonarQube for code quality checks, OWASP Dependency Check for security analysis, and Docker for containerization. Additionally, the setup involves running a separate Continuous Deployment (CD) pipeline to automatically deploy the website to a Docker container.

Prerequisites Before you begin, make sure the following components are installed and configured on your server:

Jenkins Docker SonarQube PostgreSQL (for SonarQube database) Sonar Scanner OpenJDK 17 Jenkins Installation and Configuration

Install Jenkins Run the following commands to install Jenkins on an Ubuntu system:
sudo apt update sudo apt install openjdk-11-jdk -y curl -fsSL https://pkg.jenkins.io/debian-stable/jenkins.io.key | sudo tee
/usr/share/keyrings/jenkins-keyring.asc > /dev/null sudo sh -c 'echo deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc]
https://pkg.jenkins.io/debian-stable binary/ >
/etc/apt/sources.list.d/jenkins.list' sudo apt update sudo apt install jenkins -y 2. Access Jenkins Once installed, access Jenkins on port 8080:

http://:8080 Get the initial admin password from /var/lib/jenkins/secrets/initialAdminPassword and log in.

Install Required Plugins Go to Manage Jenkins > Manage Plugins. Under the available plugins, search for and install:
SonarQube Scanner OWASP Dependency-Check Docker Pipeline Plugin

Install JDK 17 in Jenkins Navigate to Manage Jenkins > Global Tool Configuration and add JDK 17 by specifying its installation path or setting it up to be installed automatically.

Configure SonarQube in Jenkins Go to Manage Jenkins > Configure System. Scroll to the SonarQube servers section and add your SonarQube server details. Add your Sonar token from SonarQube for authentication.

SonarQube Installation and Setup

Install OpenJDK 17 sudo apt-get install openjdk-17-jdk -y

Install PostgreSQL Add PostgreSQL to your repositories and install:

sudo sh -c 'echo "deb http://apt.postgresql.org/pub/repos/apt/ $(lsb_release -cs)-pgdg main" > /etc/apt/sources.list.d/pgdg.list' sudo apt install postgresql postgresql-contrib -y sudo systemctl enable postgresql sudo systemctl start postgresql

Configure PostgreSQL for SonarQube sudo -u postgres createuser sonar sudo -u postgres psql -c "ALTER USER sonar WITH ENCRYPTED PASSWORD 'yourPassword';" sudo -u postgres createdb -O sonar sonarqube

Download and Install SonarQube sudo wget https://binaries.sonarsource.com/Distribution/sonarqube/sonarqube-9.9.3.79811.zip sudo unzip sonarqube-9.9.3.79811.zip -d /opt sudo mv /opt/sonarqube-9.9.3.79811 /opt/sonarqube

Configure SonarQube Edit the /opt/sonarqube/conf/sonar.properties file: sonar.jdbc.username=sonar sonar.jdbc.password=yourPassword sonar.jdbc.url=jdbc:postgresql://localhost:5432/sonarqube

Set Up SonarQube as a Service Create a systemd service file /etc/systemd/system/sonar.service:

[Unit] Description=SonarQube service After=syslog.target network.target

[Service] Type=forking User=sonar Group=sonar ExecStart=/opt/sonarqube/bin/linux-x86-64/sonar.sh start ExecStop=/opt/sonarqube/bin/linux-x86-64/sonar.sh stop StandardOutput=journal LimitNOFILE=131072 LimitNPROC=8192

[Install] WantedBy=multi-user.target Enable and start the service:

sudo systemctl enable sonar sudo systemctl start sonar sudo systemctl status sonar

Access SonarQube After starting SonarQube, access it in your browser at: http://:9000 Log in with the default credentials: Username: admin Password: admin You will be prompted to change the password upon first login.
Jenkins CI/CD Pipeline

CI Pipeline

The CI pipeline automates the process of checking out the source code, running a SonarQube analysis, performing a security scan using OWASP Dependency Check, building the Docker image, and pushing it to Docker Hub.

Here’s the Jenkinsfile for the CI pipeline:

groovy Copy code pipeline { agent any

tools {
    jdk 'jdk17'
}

environment {
    SCANNER_HOME = tool 'sonar-scanner'
}

stages {
    stage('Git Checkout') {
        steps {
            git credentialsId: 'github', url: 'https://github.com/praguee/containerizing-portfolio.git'
        }
    }

    stage('SonarQube Analysis') {
        steps {
            sh '''$SCANNER_HOME/bin/sonar-scanner \
                  -Dsonar.projectKey=portfolio-website \
                  -Dsonar.sources=. \
                  -Dsonar.host.url=http://localhost:9000 \
                  -Dsonar.login=<your-sonar-token>'''
        }
    }

    stage('OWASP Dependency Check') {
        steps {
            dependencyCheck additionalArguments: "--project 'Portfolio' --scan . --nvdApiKey <your-nvd-api-key>",
                            odcInstallation: 'DependencyCheck'
        }
    }

    stage('Build & Push Docker Image') {
        steps {
            script {
                withDockerRegistry(credentialsId: 'Dockerhub') {
                    sh "docker build -t portfolio:latest ."
                    sh "docker tag portfolio:latest prrague/portfolio:latest"
                    sh "docker push prrague/portfolio:latest"
                }
            }
        }
    }

    stage('Trigger CD Pipeline') {
        steps {
            build job: 'CD pipeline', wait: true
        }
    }
}
}

CD Pipeline

The CD pipeline pulls the latest Docker image from Docker Hub and runs it in a container on the server.

Here’s the Jenkinsfile for the CD pipeline:

pipeline { agent any

stages {
    stage('Docker Deploy To Container') {
        steps {
            script {
                withDockerRegistry(credentialsId: 'Dockerhub') {
                    sh "docker run -d --name portfolio -p 80:8070 prrague/portfolio:latest"
                }
            }
        }
    }
}
}

Troubleshooting Tips Jenkins Startup Issues: If Jenkins doesn’t start, check the logs in /var/log/jenkins/jenkins.log. SonarQube Service Failing: Ensure that the vm.max_map_count and other kernel parameters are set correctly in /etc/sysctl.conf. Docker Permissions: If Docker commands fail within Jenkins, ensure Jenkins has access to the Docker daemon. Add the Jenkins user to the Docker group: sudo usermod -aG docker jenkins SonarQube Authentication: Ensure that the correct SonarQube token is used in the pipeline for authentication.

Conclusion This CI/CD pipeline automates the complete process of code quality analysis, security scanning, Docker image creation, and deployment of a static website. The pipeline can be extended to suit more complex application builds and deployments, making it a robust foundation for continuous integration and delivery.

About
This repository demonstrates the implementation of a complete CI/CD pipeline for deploying a static portfolio website using Jenkins, SonarQube for code quality analysis, OWASP Dependency Check, and Docker for containerization.

Resources
 Readme
 Activity
Stars
 0 stars
Watchers
 1 watching
Forks
 0 forks
Releases
No releases published
Create a new release
Packages
No packages published
Publish your first package
Footer
