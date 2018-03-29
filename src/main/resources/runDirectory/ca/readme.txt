This directory contains the Certificate Authority Certificates used to 
generate test certificates.

The current valid CA certificates have the name MIR2014-16 and represent
certificates valid until about 10/31/2016.

The previous valid CA certificates have the name MIR2012-14-2 and represent
certificates valid until about 10/31/2014.  The -2 represents our second try
at these certificates, as some Windows systems would not accept certificates
with shorter key lengths, so we had to up the key length to 2048. The
MIR2012-14 certificates had shorter key lengths.