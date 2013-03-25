#!/bin/bash
mysql  mifosplatform_tenants < mifosng-db/multi-tenant-demo-backups/0001-mifos-platform-shared-tenants.sql
mysql  mifostenant_default < mifosng-db/multi-tenant-demo-backups/bk_mifostenant-default.sql
cd mifosng-provider
gradle clean test
gradle clean tomcatRunWar
gradle integrationTest
