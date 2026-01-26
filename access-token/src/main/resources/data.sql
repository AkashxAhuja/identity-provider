INSERT INTO app_client (client_id, client_secret, client_desc, active, allowed_scope, auth_mode, created_at, updated_at)
VALUES
  ('mib-service', 'WKkHAgiCKuAdfCneaMPjnd/g7I4VyRDAZH2wyr3CEmw=', 'MIB Registry Client', TRUE, 'devices.read devices.write', 'BASIC', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('web-portal', '203AphFNqVTsJ6xrKINAVNLBHuGsMEyJkwIGI2MDxwQ=', 'Web Portal Registry Client', TRUE, 'tokens.read tokens.write', 'BASIC', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('partner-001', 'QBaqFsRNXhVnSLv6QxY1i8X+VDtf7LMtxkO/QWnjYAM=', 'Partner Static Token', TRUE, 'partners.read', 'STATIC_TOKEN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('device-service', 'rsgISEW0GmlS1Gy6ocm3mGWUh//RM3ltldBbpF2QlsI=', 'Device Service OAuth Client', TRUE, 'devices.read devices.write devices.manage', 'BASIC', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('identity-admin', '4hhtvbG7QZNghgXoTzMgh2W1aTtV7dT3MKcZoQDu6m8=', 'Identity Admin OAuth Client', TRUE, 'tokens.read tokens.write', 'BASIC', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);