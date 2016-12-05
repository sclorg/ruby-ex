#!/bin/bash

sudo htpasswd -b /etc/origin/master/htpasswd dev1 dev1
sudo htpasswd -b /etc/origin/master/htpasswd test1 test1

oc login -u admin -p admin

oc new-project rubex-dev
oc policy add-role-to-user edit dev1 -n rubex-dev
oc policy add-role-to-user view test1 -n rubex-dev

oc new-project rubex-test
oc policy add-role-to-user edit test1 -n rubex-test
oc policy add-role-to-group system:image-puller system:serviceaccount:rubex-test:default -n rubex-dev

oc login -u dev1 -p dev1
oc project rubex-dev

oc new-app --name frontend ruby:2.3~https://github.com/omallo/ruby-ex # then: configure GitHub webhook
oc expose service frontend

oc tag rubex-dev/frontend:latest rubex-dev/frontend:test

oc login -u test1 -p test1
oc project rubex-test

oc new-app rubex-dev/frontend:test
oc expose service frontend

oc login -u dev1 -p dev1
oc project rubex-dev

oc tag rubex-dev/frontend:latest rubex-dev/frontend:test

oc login -u admin -p admin

oc new-project cicd
oc policy add-role-to-user edit system:serviceaccount:cicd:default -n rubex-dev
oc policy add-role-to-user edit system:serviceaccount:cicd:default -n rubex-test

oc new-app library/jenkins:2.19.4
oc expose svc jenkins --hostname ci.oc.habitz-app.com
