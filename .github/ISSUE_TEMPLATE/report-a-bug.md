---
name: Report a bug
about: Create a report to help us improve
title: ''
labels: bug
assignees: rusmonster

---

** Please provide the application logs when reporting bugs **

1. Enable debug log

    ```
    // Before creating a new Conversations Client with create() add this line:
    ConversationsClient.setLogLevel(android.util.Log.VERBOSE);
    ...

    ConversationsClient.create(.... your usual init code
    ```

2. Run your application and reproduce the problem

3. Capture the entire device log by running

    ```
    adb logcat -d > my_error_log
    ```

4. Send my_error_log together with this bug report.

5. YES, we need the entire log to reconstruct order of events on the client side.

6. NO, only sending the lines with
    ```
    Native thread exiting without having called DetachCurrentThread (maybe it's going to use a pthread_key_create destructor?): Thread[28,tid=22833,Native,Thread*=0xeec28600,peer=0x132c4a40,"EventThread - 2 - 22833"]
    ```
will not help - those are harmless and accounted for.

*Remove this text and the text above from the issue you're entering and fill in the sections below:*

**Describe the bug**
A clear and concise description of what the bug is.

**To Reproduce**
Steps to reproduce the behavior:
1. Go to '...'
2. Click on '....'
3. Scroll down to '....'
4. See error

**Expected behavior**
A clear and concise description of what you expected to happen.

**Screenshots**
If applicable, add screenshots to help explain your problem.

**Logs**
Attach log file(s) here.

**Device (please complete the following information):**
 - OS: Android
 - Version [e.g. 22]
 - Device manufacturer: [e.g. Samsung]
 - Device model: [e.g. Galaxy Fold 2]

**Twilio SDK**
 - Conversations SDK version: [e.g. 2.0.1]
 - Demo App Version: [e.g. 1.0]
