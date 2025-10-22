create table if not exists device_master (
    device_id VARCHAR2(200) primary key,
    cid VARCHAR2(20) not null,
    device_sec_key VARCHAR2(500),
    is_touch_enabled VARCHAR2(1) not null,
    is_migrated VARCHAR2(1) default 'N',
    status VARCHAR2(1) not null,
    audit_id VARCHAR2(20),
    device_os VARCHAR2(20) not null,
    device_os_ver VARCHAR2(20) not null,
    device_model VARCHAR2(100) not null,
    pre_dshboard_enabled VARCHAR2(1),
    biometric_type_flg VARCHAR2(1) not null,
    pre_lgn_dshboard_key VARCHAR2(500),
    session_id VARCHAR2(200),
    ime_no VARCHAR2(100),
    device_nick_name VARCHAR2(100),
    reg_date TIMESTAMP not null,
    last_update_datetime TIMESTAMP not null,
    app_version VARCHAR2(15) not null,
    lang VARCHAR2(5),
    device_os_id VARCHAR2(100)
);

create index if not exists idx_device_master_cid on device_master (cid);
create index if not exists idx_device_master_cid_ime on device_master (cid, ime_no);
create index if not exists idx_device_master_cid_osid on device_master (cid, device_os_id);
create index if not exists idx_device_master_cid_model on device_master (cid, device_model, device_os_ver);

create table if not exists device_master_history (
    history_id IDENTITY primary key,
    device_id VARCHAR2(200) not null,
    cid VARCHAR2(20) not null,
    device_sec_key VARCHAR2(500),
    is_touch_enabled VARCHAR2(1) not null,
    status VARCHAR2(1) not null,
    device_os VARCHAR2(20) not null,
    device_os_ver VARCHAR2(20) not null,
    device_model VARCHAR2(100) not null,
    biometric_type_flg VARCHAR2(1) not null,
    ime_no VARCHAR2(100),
    device_nick_name VARCHAR2(100),
    reg_date TIMESTAMP not null,
    last_update_datetime TIMESTAMP not null,
    app_version VARCHAR2(15) not null,
    lang VARCHAR2(5),
    device_os_id VARCHAR2(100),
    detag_reason VARCHAR2(255),
    detag_datetime TIMESTAMP not null
);

create table if not exists idempotency_record (
    id IDENTITY primary key,
    idempotency_key VARCHAR2(100) not null,
    request_hash VARCHAR2(64) not null,
    response_payload CLOB not null,
    created_at TIMESTAMP not null,
    unique (idempotency_key, request_hash)
);
