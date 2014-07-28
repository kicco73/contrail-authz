#!/bin/bash 
clear
pass="contrail"
mysql -u root -p"$pass" << EOF
use PipDB;
select * from Attribute;
select * from Owner;
select * from Subscriber;
select * from Owner_Subscriber;
EOF
