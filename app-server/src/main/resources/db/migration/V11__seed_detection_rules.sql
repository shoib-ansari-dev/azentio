INSERT INTO detection_rule (rule_code, name, description, risk_weight, parameters) VALUES
    ('CTR_THRESHOLD',          'Cash Transaction Threshold', 'Single transaction at or above the reporting threshold.',            80, '{"threshold_inr": 1000000}'),
    ('STRUCTURING',            'Structuring',                'Multiple transactions just below the threshold within a window.',    90, '{"window_hours": 24, "min_count": 3, "lower_inr": 900000, "upper_inr": 999999}'),
    ('RAPID_MOVEMENT',         'Rapid Movement of Funds',    'Large outflow shortly after inflow within a window.',                70, '{"window_hours": 48, "outflow_pct": 80}'),
    ('HIGH_RISK_JURISDICTION', 'High-Risk Jurisdiction',     'Transaction counterparty in a high-risk jurisdiction.',              85, '{"jurisdictions": ["IR", "KP", "SY"]}'),
    ('BEHAVIORAL_DEVIATION',   'Behavioral Deviation',       'Transaction far above the account historical average.',              60, '{"lookback_days": 90, "multiplier": 3.0}'),
    ('ROUND_NUMBER',           'Round-Number Transactions',  'Repeated round-number transactions within a window.',                40, '{"window_hours": 24, "min_count": 2}');
