#!/bin/bash 
pass="contrail"
mysql -u contrail -p"$pass" << EOF
create database UconDB;
source UCONDBcreation.sql;
EOF
