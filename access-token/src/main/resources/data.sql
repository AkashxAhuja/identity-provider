INSERT INTO application_client (client_id, client_secret_hash, static_token_hash, source_type, auth_mode, active, expires_at, created_at, updated_at, allowed_scopes)
VALUES
  ('mib-service', 'WKkHAgiCKuAdfCneaMPjnd/g7I4VyRDAZH2wyr3CEmw=', NULL, 'MIB', 'BASIC', TRUE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'devices.read devices.write'),
  ('web-portal', '203AphFNqVTsJ6xrKINAVNLBHuGsMEyJkwIGI2MDxwQ=', NULL, 'WEB_PORTAL', 'BASIC', TRUE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'tokens.read tokens.write'),
  (NULL, NULL, 'QBaqFsRNXhVnSLv6QxY1i8X+VDtf7LMtxkO/QWnjYAM=', 'PARTNER', 'STATIC_TOKEN', TRUE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'partners.read');
