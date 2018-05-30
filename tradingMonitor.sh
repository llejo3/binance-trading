#!/bin/bash

log=./logs/monitor.log
SLEEP_SECONDS=300     # 대기 시간
BASIC_GAP_SECONDS=300 # 비교를 위한 기준 간격 시간

while [ 1 ] 
do
  last_date=`tail -200 ./logs/trading.log | grep INFO | tail -1 | cut -c 2-20`
  diff_seconds=$(($(date -d now +%s) - $(date -d "$last_date" +%s)))    	
  if [ $diff_seconds -gt $BASIC_GAP_SECONDS ]  # 로그가 없으면 다시 실행              
  then
    PROCESS=`ps -ef|grep tradingBinance|grep -v grep | awk '{print $2}'`
	kill -9 $PROCESS
	wait
	now_date=`date '+%Y-%m-%d %H:%M:%S'`
	echo "$now_date : Process restarted." >> $log
	./tradingBinance.sh
	wait
  fi
  sleep $SLEEP_SECONDS
done