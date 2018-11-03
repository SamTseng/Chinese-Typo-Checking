#!/usr/bin/env python
import json
import os
import time
import threading
import logging
from datetime import timedelta, datetime
from flask import Flask, redirect, url_for, request, make_response, current_app, Response
from subprocess import call
from functools import update_wrapper
from flask_cors import CORS

''' Updated @ 2018/10/06 07:03 am '''

RUN_SIG = 'Processing'
TMO_SIG = 'Timeout'
CLN_SIG = 'CleanCache'
SHW_SIG = 'ShowCache'

app = Flask(__name__)
CORS(app, resources={r"/api/*": {"origins": "*"}})

task_id = 0
''' task id '''
task_list = {}
'''task list '''
cache_dict = {}
''' Cache dict '''
cf_dict = {}
''' Confusion set '''

TIMEOUT_PER_SENT = 20
''' timeout in second per sentence '''

print("Reading confusion set...")
with open('../ConfusionSet/ConfusionSet.txt', 'r') as fh:
    for line in fh:
        key, words = line.split(',')
        cf_dict[key.strip()] = words.strip()

print("Reading confusion set...{:,d}".format(len(cf_dict)))

class WatchDog(threading.Thread):
    def __init__(self, cache_dict):
        threading.Thread.__init__(self)
        self.cache_dict = cache_dict
        self.setDaemon(True)        

    def run(self):
        print("\t[Info] Start watch dog at {}...\n".format(datetime.now()))
        while True:
            time.sleep(5)
            if os.path.isfile(CLN_SIG):
                with open(CLN_SIG, 'r') as fh:
                    print("\t[Info] Clean cache:")
                    for line in fh:
                        line = line.strip()
                        print("\t\tRead line='{}'...".format(line))
                        if line in self.cache_dict:
                            print("\t\tPurge '{}'!".format(line))
                            del self.cache_dict[line]

                print("\t[Info] Remove clean cache signal file...")                
                os.remove(CLN_SIG)
            elif os.path.isfile(SHW_SIG):
                print("\t[Info] Show Cache ({:,d}):".format(len(self.cache_dict)))
                for k, v in self.cache_dict.items():
                    print("\t\t{}\t{}".format(k, v))

                print("")
                os.remove(SHW_SIG)

# Starting watch dog
wd = WatchDog(cache_dict)
wd.start()


def task_fun(sent, task_id):
    try:
        global cache_dict
        global cf_dict
        global task_list
        global RUN_SIG
        global TMO_SIG
        global TIMEOUT_PER_SENT
        print("Source sent='{}'".format(sent))
        while len(sent) > 0 and sent[-1].isdigit():
            sent = sent[:-1]

        task_list[task_id] = RUN_SIG

        cache_rst = cache_dict.get(sent, None)
        if cache_rst:
            print('Return from cache for sent=\'{}\''.format(sent))
            task_list[task_id] = cache_rst
            return

        #print("Handle sent={}...(task_id={})".format(sent, task_id))
        #call(['spark-submit', 'demo.py', sent, str(task_id)])
        spark_master = 'local[*]'
        data_sources = '/home/john/Github/SparkLineTokenizer/datas/ED.dne'
        # java -cp STokenizer.jar l2.spark.tokenizer.TPSearch <sent> 1
        #call(['spark-submit', '--class', 'l2.spark.tokenizer.demo', 'STokenizer.jar', sent, data_sources, str(task_id)])
        call(['java', '-cp', 'STokenizer.jar', 'l2.spark.tokenizer.TPSearch', sent, str(task_id)])
        task_id_rst = "/tmp/{}.json".format(task_id)
        wc = 0

        while True:
            if os.path.isfile(task_id_rst):
                with open(task_id_rst, 'r') as fh:
                    rst = fh.read()
                    json_obj = json.loads(rst)
                    for sug_dict in json_obj:
                        if "Suggestion" in sug_dict:
                            sw_list = sug_dict["Suggestion"]
                            wp = sug_dict["Position"]
                            w = sw_list[0].encode('utf-8')
                            ow = sent[3*(wp-1):3*wp]
                            cf_line = cf_dict.get(w, None)
                            select_ch_cnt = 0
                            if cf_line:
                                select_ch_cnt = 0
                                select_pos = 0
                                while select_ch_cnt < 2 and select_pos < len(cf_line)/3:
                                    select_w = cf_line[select_pos*3:select_pos*3+3]
                                    select_pos += 1
                                    if select_w == ow:
                                        continue

                                    sw_list.append(select_w)
                                    select_ch_cnt += 1

                    rst = json.dumps(json_obj)
                    cache_dict[sent] = rst
                    task_list[task_id] = rst

                return

            wc += 1 
            time.sleep(1)
            if wc > TIMEOUT_PER_SENT:
                #task_list[task_id] = TMO_SIG
                task_list[task_id] = []
                return
    except:
        logging.exception('Thread with exception!')
        task_list[task_id] = "[]"

@app.route("/error/<reason>")
def error(reason):
    return "Please contact lunghaolee@gmail.com for registering the usage of this service! ({})".format(reason)

@app.route("/")
def hello():
    return redirect(url_for('error',reason = 'Hello'))

@app.route("/done")
def done(msg):
    return msg

@app.route("/api/v1/task/<task_id>", methods = ['POST', 'GET', 'OPTIONS'])
def task(task_id):
    global task_list
    try:
        task_id = int(task_id)
        #print("task_id={}; task_list: {}".format(task_id, task_list))
        if task_id not in task_list:
            return json.dumps({"error":"no such task"})
        else:
            result = task_list[task_id]
            if result == TMO_SIG:
                #task_list.pop(task_id, None)
                return json.dumps({"error":"timeout"})
            elif result == RUN_SIG:
                return json.dumps({"info":result})
            else:
                #task_list.pop(task_id, None)
                return Response(result, mimetype='application/json')
    except:
        return json.dumps({"error":"illegal taskid"})

@app.route("/api/v1/query", methods = ['POST', 'GET', 'OPTIONS'])
def query():
    global task_id
    if request.method == 'OPTIONS':
        rj = request.json  # Request json
        print("Receive: {}".format(request.data))
        return "{}"
    elif request.method == 'POST':
        try:
            rj = request.json  # Request json
            print("Receive: {}".format(request.data))
            if rj['uid'] == 'l2' and rj['key'] == 'ntnu':                
                #print("Handle sent={}...".format(rj['Data'].encode('utf-8')))
                task_id = (task_id + 1) % 100000
                thd = threading.Thread(target = task_fun, args = (rj['Data'].encode('utf-8'), task_id), name='thd-{}'.format(task_id))
                thd.start()

                return Response(json.dumps({"task_id": task_id}), mimetype='application/json')
            else:
                return json.dumps({"error":"authorization failure"})

            return json.dumps({"Num":0})
        except:
            raise
            return redirect(url_for('error',reason = 'UnknownData'))        
    else:
        return redirect(url_for('error',reason = 'UnsupportMethodInQuery'))

if __name__ == "__main__":
    app.run('0.0.0.0', 5050, True)
