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

    #if len(sys.argv) > 1:
    #    sent = sys.argv[1]


    print('Send {}'.format(sent))
    data = {'uid':uid, 'key':key, 'sent':sent}
    print('Send {}: {}'.format(sent, data)) 
    resp = requests.post('http://{}:{}/query'.format(server_ip, server_port), json=data)
    print("Sending data:\n{}\n\n".format(pprint.pformat(data, indent=4)))
    print("="*50)

    print("Resp status={}".format(resp.status_code))
    if resp.status_code == 200:
        resp_json = json.loads(resp.text)
        print("Receiving data:\n{}\n\n".format(pprint.pformat(resp_json, indent=4)))
        print("="*50)
        r'''
        {
            "rst_list": [
                {
                    "tokenized": "+\u4ee5 +\u76ee\u524d *\u9f13\u672c +\u4f30\u7b97", 
                    "collsug": [
                        {
                            "sug": ["\u80a1\u672c"], 
                            "org": "\u9f13\u672c", 
                            "sp": 3, 
                            "ep": 5
                        }], 
                    "score": 4.9, 
                    "source_sent": "\u4ee5 \u76ee\u524d \u80a1\u672c \u4f30\u7b97"
                }], 
            "sentence": "\u4ee5\u76ee\u524d\u9f13\u672c\u4f30\u7b97"
        }
        '''
        print("Parsing Result:")
        rst = resp_json['rst_list'][0]
        print("\tTokenized result: {} ({})".format(rst['tokenized'].encode('utf-8'), rst['source_sent'].encode('utf-8')))
        if len(rst['collsug']) > 0:
            print("\tSuggested Correction ({}):".format(len(rst['collsug'])))
            for s in rst['collsug']:
                print("\t\t{} ({},{}) -> {}".format(s['org'].encode('utf-8'), s['sp'], s['ep'], s['sug'][0].encode('utf-8')))

    print("")
