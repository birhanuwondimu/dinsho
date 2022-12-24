#!/bin/bash
Applications/Google\ Chrome.app/Contents/MacOS/Google\ Chrome --remote-debugging-port=9222 --user-data-dir="/Users/hawisifan/solo-runner"
echo "waiting 2 seconds for the browser"
sleep 2s
echo "running the funder"
Java -jar /Users/hawisifan/projects/solo/solo-0.0.1-SNAPSHOT.jar