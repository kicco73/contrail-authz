#!/bin/bash 
clear
pass="contrail"
mysql -u root -p"$pass" << EOF
use UconDB;
truncate table attr_per_session;
truncate table sessions;
truncate table retrieval_policy;
truncate table attribute;
EOF