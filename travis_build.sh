#!/bin/bash
cd mifosng-provider
gradle clean test
gradle clean tomcatRunWar
gradle integrationTest
