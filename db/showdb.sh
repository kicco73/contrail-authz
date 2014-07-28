#!/bin/bash 
clear
pass="contrail"
mysql -u root -p"$pass" << EOF
use UconDB;
select * from attr_per_session;
select session_key, session_status, lastReevaluation, sessionId from sessions;
select * from retrieval_policy;
select * from attribute;
EOF
