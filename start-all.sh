TOP_DIR=$(cd $(dirname "$0") && pwd)
cd visallo-mvn-tutorial-repo
mvn integration-test -Pamp-to-war &
sleep 120
cd ../visallo-mvn-tutorial-share
mvn integration-test -Pamp-to-war -Dmaven.tomcat.port=8081

