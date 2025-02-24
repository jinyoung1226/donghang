pipeline {
	agent any
	// 배포 테스트 3
	environment {
        IMAGE_NAME = 'jinyoung1226/donghang-back'
        DEPLOY_DIR = '/home/jinyoung/donghang'
    }

	stages {
		stage('01. git으로 소스코드 불러오기') {
			steps {
				script {
					echo "Git에서 코드 가져오기"
					checkout scm
				}
			}
		}

		stage('02. Gradlew 권한 허용') {
			steps {
				script {
					echo "Gradlew 실행 권한 설정"
					sh "chmod +x ./gradlew"
				}
			}
		}

		stage('03. Gradle로 앱 빌드 후 jar 파일 생성') {
			steps {
				script {
					echo "Gradle 빌드 수행"
                    sh "./gradlew clean build"
				}
			}
		}

		stage('04. 도커 로그인') {
			steps {
				withCredentials([usernamePassword(credentialsId: 'docker-hub-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASSWORD')]) {
					script {
						echo "Docker Hub 로그인"
						sh "docker login -u '${DOCKER_USER}' -p '${DOCKER_PASSWORD}'"
					}
				}
			}
		}

		stage('05. Dockerfile 기반 이미지 빌드') {
			steps {
				script {
					echo "Docker 이미지 빌드"
                    sh "docker build -t ${IMAGE_NAME} ."
				}
			}
		}

		stage('06. 도커 이미지 push') {
			steps {
				script {
					echo "Docker hub에 이미지 푸시"
                    sh "docker push ${IMAGE_NAME}"
				}
			}
		}

		stage('07. 기존 docker-compose stop(SSH)') {
			steps {
				withCredentials([
					sshUserPrivateKey(credentialsId: 'deploy-server-ssh', keyFileVariable: 'SSH_KEY'),
					string(credentialsId: 'deploy-server-ip', variable: 'DEPLOY_IP'),
					string(credentialsId: 'deploy-server-username', variable: 'DEPLOY_USERNAME'),
					string(credentialsId: 'deploy-server-port', variable: 'DEPLOY_PORT')
				]) {
					script {
						echo "원격 서버에서 기존 컨테이너 종료"
                        sh """
                        	ssh -p ${DEPLOY_PORT} -i ${SSH_KEY} -o StrictHostKeyChecking=no ${DEPLOY_USERNAME}@${DEPLOY_IP} '
                        	cd ${DEPLOY_DIR}
                        	docker-compose down || true
                        	'
                        """
                    }
				}
			}
		}

		stage('08. 푸시한 이미지 불러오기') {
			steps {
				withCredentials([
					sshUserPrivateKey(credentialsId: 'deploy-server-ssh', keyFileVariable: 'SSH_KEY'),
					string(credentialsId: 'deploy-server-ip', variable: 'DEPLOY_IP'),
					string(credentialsId: 'deploy-server-username', variable: 'DEPLOY_USERNAME'),
					string(credentialsId: 'deploy-server-port', variable: 'DEPLOY_PORT')
				]) {
					script {
						echo "푸시한 이미지 불러오기"
                        sh """
                        	ssh -p ${DEPLOY_PORT} -i ${SSH_KEY} -o StrictHostKeyChecking=no ${DEPLOY_USERNAME}@${DEPLOY_IP} '
                        	cd ${DEPLOY_DIR}
                        	docker-compose pull || exit 1
                        	'
                        """
                    }
				}
			}
		}

		stage('09. 새로운 docker-compose.yml로 컨테이너 띄우기') {
			steps {
				withCredentials([
					sshUserPrivateKey(credentialsId: 'deploy-server-ssh', keyFileVariable: 'SSH_KEY'),
					string(credentialsId: 'deploy-server-ip', variable: 'DEPLOY_IP'),
					string(credentialsId: 'deploy-server-username', variable: 'DEPLOY_USERNAME'),
					string(credentialsId: 'deploy-server-port', variable: 'DEPLOY_PORT')
				]) {
					script {
						echo "새로운 docker-compose.yml로 컨테이너 띄우기"
                        sh """
                        	ssh -p ${DEPLOY_PORT} -i ${SSH_KEY} -o StrictHostKeyChecking=no ${DEPLOY_USERNAME}@${DEPLOY_IP} '
                        	cd ${DEPLOY_DIR}
                        	docker-compose up --build -d || exit 1
                        	'
                        """
                    }
				}
			}
		}
	}
}