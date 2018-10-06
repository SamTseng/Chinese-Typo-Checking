#!/usr/bin/env python
# -*- coding: utf-8 -*- 
from pyspark import SparkConf, SparkContext 
import sys
import json
import re
#print("The Python version is %s.%s.%s" % sys.version_info[:3])
reload(sys) 
sys.setdefaultencoding('utf-8') 
  

########################
# Global Variables
########################
conf = SparkConf().setMaster("local").setAppName("My App")  
sc = SparkContext(conf = conf)
#testRDD = sc.textFile('datas/test.dne')
testRDD = sc.textFile('datas/test.dne')
wordRDD = testRDD.flatMap(lambda line: line.split()).distinct().persist()

topN = 1  # Show at most top 10 candidates
task = 0  # Task ID

if len(sys.argv) > 1:
    target_sent = sys.argv[1].encode('utf-8').strip()
    if len(sys.argv) > 2:
        task = int(sys.argv[2])
else:
    target_sent = '今天還是要去公司'.encode('utf-8')


########################
# APIs
########################
def length(sent):
    try:
        sent = sent.decode('utf-8')
        return len(sent)
    except:
        print("Fail to get length of sent={}".format(sent))
        return len(sent)

def look4Coll(ptoken, etoken):
    r'''
    Look for collocaton from previous token as <ptoken> and 
    current error token as <etoken>
    '''
    if ptoken:
        if ptoken.startswith('+') or ptoken.startswith('-') or ptoken.startswith('~'):
            ptoken = ptoken[1:]

    
    def filterPtoken(token_line):
        token_list = token_line.split()
        encode_token_list = map(lambda t: t.encode('utf-8'), token_list)
        if ptoken:
            return ptoken in encode_token_list
        else:
            return len(set(etoken) & set(encode_token_list[0])) > 0
    
    def collCheck(token_line):
        token_list = token_line.split()
        token_list_len = len(token_list)

        if ptoken is None:
            return ((None, token_list[0].encode('utf-8')), 1)
        else:
            for i in xrange(token_list_len):
                token = token_list[i].encode('utf-8')
                if token == ptoken and i+1 < token_list_len:
                    #print("{} {}".format(token,  token_list[i+1].encode('utf-8')))
                    return ((token, token_list[i+1].encode('utf-8')), 1)
        return ((ptoken, None), 0) 

    def b2s(sent):
        bs_list = []
        for i in range(len(sent)/3):
            bi = i*3
            bs_list.append(sent[bi:bi+3])

        return bs_list

    def sim_of_token(etoken, otoken):
        if len(etoken) != len(otoken):
            return False
 
        etoken_c_list = b2s(etoken)
        otoken_c_list = b2s(otoken)
        hc = 0
        for bs in etoken_c_list:
            if bs in otoken_c_list:
                hc += 1

        try:
            return float(hc) / len(otoken_c_list)
        except:
            print("otoken={}; etoken={}!".format(otoken, etoken))
            return 0.0

    def filterSimUp(e):
        t = e[0]
        s = e[1]
        if t[1] is not None and len(etoken) == len(t[1]):
            return sim_of_token(etoken, t[1]) >= 0.5
        else:
            return False

    if ptoken:
        tmpRDD = testRDD.filter(filterPtoken)
        if tmpRDD.count() > 0:
            rstRDD = tmpRDD.map(collCheck).reduceByKey(lambda a, b: a + b).filter(filterSimUp)
        else:
            rstRDD = None

        return rstRDD
    else:
        return testRDD.map(collCheck).reduceByKey(lambda a, b: a + b).filter(filterSimUp)


def isTokenSeen(token):
    return wordRDD.filter(lambda e: e.encode('utf-8')==token).count() > 0


def tokenCheck(token_line):
    #print("token line={} ({})".format(token_line.encode('utf-8'), len(token_line)))
    global target_sent
    sent = target_sent
    token_list = token_line.split()
    result_token_list = []
    mc = 0  # Missing count
    hc = 0  # Hit count
    for token in token_list:
        token = token.encode('utf-8')
        token_len = len(token)
        #print("Sent={}; token={}".format(sent, token))
        try:
            if token.startswith('r:'):
                mth = re.search(token[6:], sent)
                if mth:
                    token = mth.group(0)
                    token_len = len(token)
                    pi = sent.index(token)
                    #print('Sent={}; token={}; pi={}'.format(sent, token, pi))
                else:
                    raise ValueError('Not match')
            else:
                pi = sent.index(token)

            #print('\tToken={}({}) is found...(pi={})'.format(token, token_len, pi))
            if pi > 0:                
                #print('Add missing {}'.format(sent[0:pi]))
                hc += 1	
                result_token_list.append("*{}".format(sent[0:pi]))
                result_token_list.append("+{}".format(token))
                sent = sent[pi+token_len:]
            else:
                hc += 2
                result_token_list.append("+{}".format(token))
                sent = sent[pi+token_len:]

        except ValueError:
            #print('\tToken={} is not found!'.format(token))
            mc += 1            
        except:
            raise

        if len(sent) == 0:
            break
            

    if len(sent) > 0:
        result_token_list.append("*{}".format(sent))


    #print("{}...mc={}/hc={}: {}".format(token_line.encode('utf-8'), mc, hc, ' '.join(result_token_list)))
    #return "{} {} {}".format(hc, mc, ' '.join(result_token_list))
    return ((' '.join(result_token_list), token_line), hc-0.1*mc)
    #return mc <= 2 and hc > 0

def tokenize(sub_line):
    def _map2Tokenize(token_line):
        
        token_line = token_line.encode('utf-8')
        sent = sub_line
        #print('sub_line={}'.format(sent))        
        token_list = token_line.split()
        result_token_list = []
        mc = 0  # Missing count
        hc = 0  # Hit count
        for token in token_list:
            #print("Sent={}; token={}".format(sent, token))
            token_len = len(token)
            try:
                if token.startswith('r:'):
                    mth = re.search(token[2:], sent.encode('utf-8'))
                    if mth:
                        #print('Match ptn={}: {}'.format(token[6:], mth.group(0)))
                        token = mth.group(0)
                        token_len = len(token)
                        pi = sent.index(token)
                        #print('Sent={}; token={}; pi={}'.format(sent, token, pi))
                    else:
                        raise ValueError('Not match')
                else:
                    pi = sent.index(token)

                hc += 1
                if pi > 0:
                    ctoken = sent[0:pi]
                    #if isTokenSeen(ctoken):
                    #    hc += 0.5
                    #    result_token_list.append("~{}".format(ctoken))
                    #else:
                    result_token_list.append("*{}".format(ctoken))
                    result_token_list.append(token)
                    sent = sent[pi+token_len:]
                else:
                    hc += 1
                    result_token_list.append(token)
                    sent = sent[pi+token_len:]

            except ValueError:
                mc += 1
            except:
                raise

            if len(sent) == 0:
                break


        if len(sent) > 0:
            result_token_list.append("*{}".format(sent))

        return ((' '.join(result_token_list), token_line), hc-0.1*mc)

    rstRDD = testRDD.map(_map2Tokenize).reduceByKey(lambda a, b: a + b).filter(lambda e: e[1] > 0)
    final_token_list = []
    for t, v in sorted(rstRDD.collect(), key=lambda e: e[1], reverse=True):
        k = t[0]
        if ' ' not in k:
            break
        #print('\tCheck {}'.format(k))
        for token in k.split():            
            if token.startswith('*'):
                ctoken = token[1:]
                if wordRDD.filter(lambda e: e.encode('utf-8')==ctoken).count() == 0:
                    final_token_list = []
                    break
                else:                   
                    final_token_list.append("-{}".format(ctoken))
            else:
                final_token_list.append("-{}".format(token))
        break

    return final_token_list


########################
# MAIN
########################
trtRDD = testRDD.map(tokenCheck).reduceByKey(lambda a, b: a + b).filter(lambda e: e[1] > 0)
print("Candidate number={:,d}...".format(trtRDD.count()))
candi_list = []
coll_cache_dict = {}
for t, score in sorted(trtRDD.collect(), key=lambda e: e[1], reverse=True):
    k = t[0].encode('utf-8')    
    final_token_list = []
    suggest_token_list = {}
    ptoken = None
    #print("Precheck {}...v={}".format(k, score)) 
    for token in k.split():
        dtoken = token.encode('utf-8')
        if dtoken.startswith('*'):
            ctoken = dtoken[1:]
            if isTokenSeen(ctoken):
                score += 0.5
                final_token_list.append("~{}".format(ctoken))
                ptoken = ctoken
            else:
                if (len(ctoken)/3) > 4:
                    sub_token_list = tokenize(dtoken[1:])
                    #print('\tRetokenize {} ({}): \'{}\''.format(ctoken, len(ctoken), ' '.join(sub_token_list)))
                    if sub_token_list:
                        #print('\t{}->{}'.format(token, sub_token_list))
                        score += len(sub_token_list) * 0.5
                        final_token_list.extend(sub_token_list)
                        ptoken = ctoken
                        continue

                final_token_list.append(token)
        else:           
            final_token_list.append(token)
            ptoken = token       

    #print("Final {}...v={}".format(k, score))
    candi_list.append(((final_token_list, t[1].encode('utf-8')), score))

#proc_json = {"sentence":target_sent, "rst_list":[]}
proc_json = []
candi_list = sorted(candi_list, key=lambda e: e[1], reverse=True)
ci = 0
for t, v in candi_list:
    tokenized_rst = " ".join(t[0])
    source_sent = t[1]
    rst = {"tokenized": tokenized_rst, "score":v, "source_sent":source_sent, "collsug":[]}
    ci += 1
    if ci <= topN:
        print("Candidate Tokenized Result={} (score={} by '{}')".format(tokenized_rst, v, source_sent))
        ptoken = None
        si = 0
        for token in t[0]:
            ctoken = token[1:]
            #print("Look for {}/{}...({})".format(ctoken, length(ctoken), length(token)))
            if token.startswith('*'):
                collRDD = look4Coll(ptoken, ctoken)
                if collRDD:
                    csl = {"sp":si, "ep":si+length(ctoken), "org":ctoken, "sug":[]}
                    hasSug = False
                    for st, v in sorted(collRDD.collect(), key=lambda e: e[1], reverse=True):
                        #print("\t{}->{}?".format(ctoken, st[1])) 
                        csl["sug"].append(st[1])
                        for i in range(length(ctoken)):
                            sc = st[1][i*3:i*3+3]
                            cc = ctoken[i*3:i*3+3]
                            #print("sc={}; cc={}".format(sc, cc))
                            if sc != cc:
                                proc_json.append({"ErrorType":"Spell", 
                                                  "Notes":"", 
                                                  "Suggestion":[sc],
                                                  "Position": si + i + 1})
 
                    #if hasSug:
                    #    rst["collsug"].append(csl)

            ptoken = token
            si += length(ctoken) 
 
        #proc_json["rst_list"].append(rst)
    else:
        break


with open('/tmp/{}.json'.format(task), 'w') as fw:
    pos_dict = {}
    for ed in proc_json:
        ped = pos_dict.get(ed["Position"], None)
        if ped:
            ped["Suggestion"].append(ed["Suggestion"])
        else:
            pos_dict[ed["Position"]] = ed

    proc_json = map(lambda t:t[1], sorted(pos_dict.items(), key=lambda t:t[0]))
    json.dump(proc_json, fw)
