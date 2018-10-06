#!/bin/sh
#java -cp STokenizer.jar l2.spark.tokenizer.BuildTokens datas/test.dne /home/datas/tokens/
#java -cp STokenizer.jar l2.spark.tokenizer.BuildTokens datas/ED.dne /home/datas/tokens/
#java -cp STokenizer.jar l2.spark.tokenizer.BuildTokens /tmp/test.dne /home/datas/tokens/
java -cp STokenizer.jar l2.spark.tokenizer.BuildTokens $1 /home/datas/tokens/
