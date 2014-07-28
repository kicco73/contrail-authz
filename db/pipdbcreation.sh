#!/bin/bash 
pass="contrail"
mysql -u root -p "$pass" << EOF
create database PipDB;
EOF
