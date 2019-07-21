#!/bin/bash
# 平滑关闭和启动 Spring Boot 程序
#设置端口
SERVER_PORT="8081"
#设置应用名称
JAR_NAME="springboot-shutdown-0.0.1-SNAPSHOT"
#设置 JAVA 启动参数
JAVA_OPTIONS="-server -Xms1024M -Xmx1024M -Dserver.port=$SERVER_PORT"

#Actuator 方式远程关闭应用
curl -X POST "http://localhost:$SERVER_PORT/actuator/shutdown"
echo ""
#循环遍历应用端口是否被使用，作为应用运作状态的标志
echo "关闭旧应用开始"
UP_STATUS=1
while(( $UP_STATUS>0 ))
do
    UP_STATUS=$(lsof -i:"$SERVER_PORT" | wc -l)
done
echo "\n关闭旧应用结束"
echo "启动应用开始"
#非挂起方式启动应用，并且跟踪启动日志文件
nohup>"$SERVER_PORT".log java -jar "$JAVA_OPTIONS" "$JAR_NAME".jar 2>&1 &
echo "启动应用中" && tail -20f "$SERVER_PORT".log