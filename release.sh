#! /bin/sh

# mvn release:prepare release:perform -P jenkins -Dusername=nathanagood -Dpassword=$1 -e
mvn release:prepare release:perform -Dusername=nathanagood -Dpassword=$1 -e

