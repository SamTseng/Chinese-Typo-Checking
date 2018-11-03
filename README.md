## Backend Service Introduction
Here we will introduce how to compile/run/maintain backend service. For the whole project introduction, please refer to [this slide](docs/2018_NTNU_NCU_CTC_1006.pptx) for more details.

* [__Compile Backend Main Program__](#backend_compile) 
* [__Launch Backend web service__](#backend_web_service)
* [__Backend Maintenance__](#backend_maintain) 

## Compile Backend Main Program <a name='backend_compile'></a>
The Backend Main Program `STokenizer.jar` is used to carry out below two things while get called by web service [`index.py`](Backend/index.py):
* Apply TPS (Token Path Search) algorithm to tokenalize the given sentence
* Use collocation/correlation to look for suggestion of character(s) while encountering unknown token
![Image of Yaktocat](docs/images/b_1.PNG)

The source code location of backend main program is [`Backend/src`](Backend/src). To compile it, please enter path [`Backend`](Backend) and use below command to compile the source code and build the backend main program `STokenizer.jar`:
```bash
// Check all available gradle task(s)
# gradle tasks --all
...

// Compile the source code > Wrap the jar file > Copy jar file into current working folder
# gradle copyJarToRoot
:compileJava UP-TO-DATE
:processResources NO-SOURCE
:classes UP-TO-DATE
:jar UP-TO-DATE
:copyJarToRoot

BUILD SUCCESSFUL

Total time: 0.656 secs

// Check the content of our toolkit tc.sh
# cat tc.sh
...
java -cp STokenizer.jar l2.spark.tokenizer.TPSearch $1 0
echo ""

// Testing our new built jar 
# ./tc.sh 華語文教學應用軟體競賽
        [Info] 2 solution(s) found:
        華語文|教學|應|用|軟體|競賽
        華語文|教學|應用|軟體|競賽 
```
## Launch Backend web service <a name='backend_web_service'></a>
To start our backend web service, we have to enter folder [`Backend`](Backend) which contains the file [`index.py`](Backend/index.py) which will be used to launch our backend web service. Please follow below steps to start web service:
```bash
// Create a screen to start the backend web service.
// So when we logout the server, the service will still be running
// If the screen with name 'http' is already exist, use below command to enter target screen:
// # screen -r http
# screen -S http

// Now we are inside screen with name as 'http'
# ./index.py
Reading confusion set...
Reading confusion set...4,743
        [Info] Start watch dog at 2018-11-03 13:08:20.712634...

 * Serving Flask app "index" (lazy loading)
 * Environment: production
   WARNING: Do not use the development server in a production environment.
   Use a production WSGI server instead.
 * Debug mode: on
 * Running on http://0.0.0.0:5050/ (Press CTRL+C to quit)
 * Restarting with stat
Reading confusion set...
Reading confusion set...4,743

// Exit screen 'http' by Ctrl+A+D
// The web service is listening to port 5050
// Use below command to confirm the web service is ready
# netstat -tunlp | grep 5050
tcp        0      0 0.0.0.0:5050            0.0.0.0:*               LISTEN      18709/python
```
Then you can use toolkit [`testClient.py`](Backend/testClient.py) to check the availability of our web service:
```bash
# ./testClient.py
Send 這是中文具子
Send 這是中文具子: {'Data': '\xe9\x80\x99\xe6\x98\xaf\xe4\xb8\xad\xe6\x96\x87\xe5\x85\xb7\xe5\xad\x90', 'uid': 'l2', 'key': 'ntnu'}
Sending data:
{   'Data': '\xe9\x80\x99\xe6\x98\xaf\xe4\xb8\xad\xe6\x96\x87\xe5\x85\xb7\xe5\xad\x90',
    'key': 'ntnu',
    'uid': 'l2'}


==================================================
Resp status=200
Receiving data:
{   u'task_id': 16}


==================================================
Retrieve task_id=16...
Resp status=200
Receiving data:
[   {   u'ErrorType': u'Spell',
        u'Notes': u'',
        u'Position': 5,
        u'Suggestion': [u'\u53e5', u'\u64da', u'\u5287']}]


==================================================
Parsing Result:
Suggested Correction (1):
        具(4) => 句,據,劇
```
## Backend Maintenance <a name='backend_maintain'></a>
Sometimes you will receive feedback on the correctness of the processing result from front-end. Then you can follow below steps to fix the unwanted result observed from the front-end. First of all, all our toolkit are under path [`Backend`](Backend). So before working on below sections, please enter folder [`Backend`](Backend) firstly.

### Unseen sentence/token
It is very common for our backend to receive un-seen sentece. To solve this issue, you can folow below steps:
```bash
// Sentence '青年北漂議題發酵的背後' is never seen before.
# ./tc.sh 青年北漂議題發酵的背後
        [Info] No TP found!
        
// Save the tokenalized result of unseen sentence into file        
# echo '青年 北漂 議題 發酵 的 背後' > /tmp/test.txt

// Use toolkit buildTokenPool.sh to add the tokenalized result into backend corpus
# ./buildTokenPool.sh /tmp/test.txt
        [Info] Start building token pool...
        [Info] Done (1)!

// Try again and now our backend can recognize the unseen sentence now
# ./tc.sh 青年北漂議題發酵的背後
        [Info] 1 solution(s) found:
        青年|北漂|議題|發酵|的|背後        
```

### Remove Wrong Tokenalized Result from corpus
Sometimes we may find some wrong token in our corpus. For example:
```bash
# ./tc.sh 陷井重重的道路
        [Info] 3 solution(s) found:
        陷井|重重|的|道路
```
Here the token '陷井' is the unwanted token. We can use toolkit [`delToken.sh`](Backend/delToken.sh) to remove unwanted token:
```bash
// Remove the token '陷井'
# ./delToken.sh 陷井 -
Remove token=陷井...Done!

// Double confirm the deletion 
# ./tc.sh 陷井重重的道路
        [Info] No TP found!
```
