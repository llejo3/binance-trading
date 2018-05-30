#!/bin/bash

process_cnt=`ps -ef | grep tradingBinance.jar | grep -v grep | wc -l`
if [ $process_cnt -ne 0 ]
then
	PROCESS=`ps -ef | grep tradingBinance.jar | grep -v grep|awk '{print $2}'`
	echo $PROCESS
	if [ "$PROCESS" != "" ]
	then
		kill -9 $PROCESS
		wait
	fi
fi

nohup /usr/local/java/jdk-10.0.1/bin/java -jar tradingBinance.jar 1> /dev/null 2>&1 &

process_cnt=`ps -ef|grep "tradingMonitor"|grep -v grep|wc -l`
if [ $process_cnt -eq 0 ]
then
	nohup ./tradingMonitor.sh  1> /dev/null 2>&1 &
fi


