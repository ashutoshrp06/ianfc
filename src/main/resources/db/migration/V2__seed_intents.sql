INSERT INTO intents (intent_id, name, intent_type, target_entity, target_region, threshold_value, threshold_unit, severity, active)
VALUES
    ('a0000000-0000-0000-0000-000000000001',
     'BGP Adjacency SLA - Edge Router',
     'BGP_ADJACENCY',
     'router-edge-01',
     'EU-WEST-1',
     0,
     'count',
     'CRITICAL',
     true),

    ('a0000000-0000-0000-0000-000000000002',
     'Max Latency SLA - Core Router',
     'MAX_LATENCY',
     'router-core-01',
     'EU-WEST-1',
     100,
     'ms',
     'MAJOR',
     true),

    ('a0000000-0000-0000-0000-000000000003',
     'Packet Loss SLA - Access Switch',
     'MAX_PACKET_LOSS',
     'switch-access-01',
     'EU-WEST-1',
     5,
     'percent',
     'MINOR',
     true)
    ON CONFLICT (intent_id) DO NOTHING;