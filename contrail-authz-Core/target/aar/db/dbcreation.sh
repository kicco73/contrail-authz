#!/bin/bash 
pass="contrail"
mysql -u root -p"$pass" << EOF
create database UconDB;
source UCONDBcreation.sql;
EOF
