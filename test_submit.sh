#!/bin/sh

# # Working
# spark-submit --master yarn --deploy-mode cluster --class l2.spark.tokenizer.demo STokenizer.jar yarn $1 /datas/test 0 /root/SparkLineTokenizer/STokenizer.jar
# spark-submit --jars STokenizer.jar --master yarn --deploy-mode client --class l2.spark.tokenizer.demo STokenizer.jar $1 /datas/test 0

spark-submit --class l2.spark.tokenizer.demo STokenizer.jar $1 /home/john/Github/SparkLineTokenizer/datas/ED.dne 0
