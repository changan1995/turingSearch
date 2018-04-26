sudo add-apt-repository ppa:webupd8team/java
sudo apt-get update
sudo apt-get install oracle-java8-installer
sudo apt-get install oracle-java8-set-default
sudo apt-get install maven
git clone https://github.com/changan1995/cis455.git pj4
t2.2xlarge




ssh -i "~/.ec2/edu_sshkey.pem" ubuntu@ec2-184-72-100-180.compute-1.amazonaws.com
ssh -i "~/.ec2/edu_sshkey.pem" ubuntu@ec2-75-101-216-74.compute-1.amazonaws.com

sftp -i "~/.ec2/edu_sshkey.pem" ubuntu@ec2-184-72-100-180.compute-1.amazonaws.com
rm -r /store
mvn clean install
mvn exec:java@WebCrawler -Dexec.args="./config.txt 0"
mvn exec:java@WebCrawler -Dexec.args="./config.txt 1 ./seedPage"


nohup mvn exec:java@WebCrawler -Dexec.args="./config.txt 0" > log &
nohup mvn exec:java@WebCrawler -Dexec.args="./config.txt 1" > log &

nohup mvn exec:java@WebCrawler -Dexec.args="./config.txt 1 200 1000" > log &
nohup mvn exec:java@WebCrawler -Dexec.args="./config.txt 0 15 200" > log2 &
nohup mvn exec:java@WebCrawler -Dexec.args="./config.txt 1" > log2 &


ssh -i "~/cis555project.pem" ubuntu@ec2-34-235-125-121.compute-1.amazonaws.com
git clone https://github.com/changan1995/cis455.git
nohup mvn exec:java@WebCrawler -Dexec.args="./moj.txt 0" > log &
nohup mvn exec:java@WebCrawler -Dexec.args="./moj.txt 1" > log &
nohup mvn exec:java@WebCrawler -Dexec.args="./moj.txt 4 ./seedPage" > log &

54.85.18.147:5001/status
54.165.46.64:5001/status
34.227.49.62:5001/status
35.172.194.220:5001/status
34.229.20.19:5001/status