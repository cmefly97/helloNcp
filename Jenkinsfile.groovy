node {
    // Jenkins 파일에서 취급하는 파라미터들을 미리 정의한다.
    // 아래와 같이 미리 정의하면 Jenkins Job 이 Parametrized Job 이 되며 기본 변수들이 들어가게 된다
    properties(
            [
                    [$class: 'ParametersDefinitionProperty', parameterDefinitions:
                            [
                                    [$class: 'BooleanParameterDefinition', defaultValue: true, description: '테스트를 Skip 할 수 있습니다. 선택 시 테스트를 건너뛰고 체크아웃 - 빌드 - 아카이빙만 진행합니다', name: 'skipTests']
                                    , [$class: 'StringParameterDefinition', defaultValue: 'development', description: 'Maven에서 Active 할 Profile 을 입력하세요. 예) production', name: 'activeProfile']
                            ]
                    ]])

    def mvnHome

    stage('Preparation') { // for display purposes
        echo "Current workspace : ${workspace}"
        // Get the Maven tool.
        // ** NOTE: This 'M3' Maven tool must be configured
        // **       in the global configuration.
        mvnHome = tool 'Maven_3_5'
        env.JAVA_HOME = tool 'jdk8'
    }
    stage('Checkout') {
        // Get some code from a Git repository
        println "Get code from a SourceCommit repository"
        checkout scm
    }
    if (skipTests == true) {
        
        println "skipTests : " + skipTests
        
        stage('Test') {
            sh "'${mvnHome}/bin/mvn'  -Dmaven.test.failure.ignore -B verify"
            //sh "'${mvnHome}/bin/mvn' -P ${activeProfile} -Dmaven.test.failure.ignore -B verify"
        }
        stage('Store Test Results') {
            junit '**/target/surefire-reports/TEST-*.xml'jenkinpipeline
        }
    }
    stage('Build') {
    
    	println "Build Start ::::::::::::: "
    	
        
        sh "'${mvnHome}/bin/mvn'  -Dmaven.test.skip=true  clean install package"
        //sh "'${mvnHome}/bin/mvn' -P ${activeProfile} -Dmaven.test.skip=true clean install"
    }
    stage('Archive') {
        archive '**/target/*.war'
    }
    stage('Deploy') {
        echo "Deploy start Now !!"
        
        
        // SSH Agent Plugin
        
        echo "agent start ~ "
        sh "pwd"
        
        echo "Upload ObjectStorage"
    	sh "python /usr/objectstorage/objUpload.py"
    	
    	echo "Deploy start Now !!"
   		sh "python /usr/objectstorage/objDown.py"
        
        echo "stop springboot ~"
        sh "ssh  -o StrictHostKeyChecking=no root@49.236.137.211 -p3333  sh /var/www/script/runNcp.sh stop"
        
        echo "copy artipact  to remote server  over ssh !!"
        sh "scp -P 3333 -p -r  ./target/*.war root@49.236.137.211:/var/www/html"
        
        echo "start springboot ~"
        sh "ssh  -o StrictHostKeyChecking=no root@49.236.137.211 -p3333  sh /var/www/script/runNcp.sh start"
    }
   
}