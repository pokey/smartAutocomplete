#!/usr/bin/python
import smartAutocompleteDaemon

daemon = smartAutocompleteDaemon.Daemon('/tmp/daemon-example.pid')
daemon.stop()
