# Certificates

This directory is reserved for backend-only PostgreSQL trust material when the current server requires custom SSL verification.

Do not treat the previous provider's `ca.pem` flow as the default trust path after switching databases. Only place a certificate here once operations confirms the replacement PostgreSQL deployment needs one, and never copy it into the Android app module or any mobile asset bundle.
