#!/bin/bash
mysql  mifosplatform-tenants < mifosng-db/multi-tenant-demo-backups/0001-mifos-platform-shared-tenants.sql
mysql  mifostenant-default < mifosng-db/multi-tenant-demo-backups/bk_mifostenant-default.sql
cd mifosng-provider
gradle clean test
gradle clean tomcatRunWar
gradle integrationTest
