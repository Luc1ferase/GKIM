# Certificates

`postgres-ca.pem` is the backend-side placement for the existing root `ca.pem` certificate.

Use this certificate only from server processes that connect to PostgreSQL over SSL. Do not copy this file into the Android app module or any mobile asset bundle.
