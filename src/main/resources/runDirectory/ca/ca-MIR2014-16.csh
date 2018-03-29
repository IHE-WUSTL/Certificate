#!/bin/csh
#*******************************************************************************
# Generates new CA certificate for IHE testing. 
# Parameters:
#    req     PKCS#10 X.509 Certificate Signing Request (CSR) Management
#   -nodes   do not encrypt created private key
#   -new     new certificate request. Will ask for fields values (in ca.txt)
#   -x509    self signed certificate
#   -outform output format PEM
#   -newkey  also creates new private key (RSA key 2048 bits)
#   -keyout  key output file name ca_priv.pem
#   -out     certificate output file name ca_cert.pem
#   -sha1    message digest to sign request with. Secure Hash Algorithm 1
#   -days    number of days until certificate expires 
#*******************************************************************************
set NAME = MIR2014-16
set EDATE = `date -d "2016/11/30" "+%s"`
set NOW = `date "+%s"`
@ DIFF = `expr $EDATE - $NOW`
@ DEN = `expr 60 \* 60 \* 24`
@ DAYS = `expr $DIFF / $DEN`
openssl req -nodes -new -x509 -outform PEM -newkey rsa:2048 -keyout cakey.$NAME.pem -out cacert.$NAME.pem -sha1 -days $DAYS <  ca$NAME.txt
