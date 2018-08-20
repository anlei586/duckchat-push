#!/bin/bash
#!/bin/sh
# ----------------------------------------------------------------------------
# Copyright 2018-2028 Akaxin Group
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------


mvn install:install-file -Dfile=zaly-push-stater/lib/MiPush-SDK-Server-2.2.18.jar -DgroupId=com.xiaomi -DartifactId=MiPush-SDK-Server -Dversion=2.2.18 -Dpackaging=jar
mvn install:install-file -Dfile=zaly-push-stater/lib/json-simple-1.1.1.jar -DgroupId=org.json.simple -DartifactId=json-simple -Dversion=1.1.1 -Dpackaging=jar

cd `dirname $0`
mvn -T 2C clean package

cp zaly-push-starter/target/zaly-push-starter-1.0.1-SNAPSHOT.jar zaly-push-starter-1.0.1-SNAPSHOT.jar