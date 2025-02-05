# Assignment3

server: 

mvn clean compile OR mvn compile

mvn exec:java '-Dexec.mainClass=bgu.spl.net.impl.tftp.TftpServer' '-Dexec.args=7777'

client:

mvn clean compile OR mvn compile

mvn exec:java '-Dexec.mainClass=bgu.spl.net.impl.tftp.TftpClient' '-Dexec.args=localhost 7777'