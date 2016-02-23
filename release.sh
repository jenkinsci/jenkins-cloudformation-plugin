#! /bin/sh

mvn release:prepare release:perform -P jenkins -Dusername=edovale -Dpassword=$1 -e

