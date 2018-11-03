#!/usr/bin/env python
# -*- coding: utf-8 -*-
import json
import sys
import requests
import pprint
import os
import argparse
import logging

###################################
# Constants
###################################



###################################
# Global Variable
###################################
server_ip = 'localhost'
server_port = 5050
uid=os.environ.get('HANS_UID', None)
key=os.environ.get('HANS_KEY', None)
sent='這是中文具子'


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Utility to test web service as client')
    parser.add_argument('--UID', help="UID of web service", default=None, type=str)
    parser.add_argument('--KEY', help="KEY of web service", default=None, type=str)
    parser.add_argument('-i', help="Input sentence", default=None, type=str)
    parser.add_argument('--SERVER_PORT', help="Server port", default=None, type=int)
    parser.add_argument('--SERVER_ADDR', help="Server IP address", default=None, type=str)

    args = parser.parse_args()

    if args.UID:
        uid = args.UID

    if args.KEY:
        key = args.KEY     

    if args.i:
        sent = args.i



    print('Send {}'.format(sent))
    data = {'uid':uid, 'key':key, 'Data':sent}
    print('Send {}: {}'.format(sent, data)) 
    resp = requests.post('http://{}:{}/api/v1/query'.format(server_ip, server_port), json=data)
    print("Sending data:\n{}\n\n".format(pprint.pformat(data, indent=4)))
    print("="*50)

    print("Resp status={}".format(resp.status_code))
    if resp.status_code == 200:
        resp_json = json.loads(resp.text)
        print("Receiving data:\n{}\n\n".format(pprint.pformat(resp_json, indent=4)))
        print("="*50)
        task_id = resp_json['task_id']

        print('Retrieve task_id={}...'.format(task_id))
        resp = requests.get('http://{}:{}/api/v1/task/{}'.format(server_ip, server_port, task_id))
        print("Resp status={}".format(resp.status_code))
        if resp.status_code == 200:
            resp_json = json.loads(resp.text)
            print("Receiving data:\n{}\n\n".format(pprint.pformat(resp_json, indent=4)))
            print("="*50)
            print("Parsing Result:")
            r'''
[   {   u'ErrorType': u'Spell',
        u'Notes': u'',
        u'Position': 5,
        u'Suggestion': [u'\u53e5', u'\u64da', u'\u5287']}]
            '''
            if len(resp_json) > 0:
                print("Suggested Correction ({}):".format(len(resp_json)))
                for s in resp_json:
                    cpos = s['Position'] - 1 
                    print("\t{}({:,d}) => {}".format(sent[cpos * 3: (cpos+1) * 3], cpos, ','.join(map(lambda e:e.encode('utf-8'), s['Suggestion']))))

    print("")
