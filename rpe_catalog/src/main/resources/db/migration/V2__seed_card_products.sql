-- The four card products the business works with, with fixed ids so other services can reference them.
-- rpe_card_processor uses GOLD (3f2b6c1e-8d4a-4f7b-9c2e-1a5d6e7f8a9b) as its default product.
-- Names are stored upper-cased, as Product/ProductName do. Existing names are left untouched.
INSERT INTO card_product (id, name, description, status, created_at, updated_at) VALUES
    ('0b9d3e7a-2c4f-4a1b-8e6d-5f7a9b1c3d2e', 'BLACK CARD', 'Top-tier card with premium benefits and highest limits', 'ATIVO', now(), now()),
    ('6a1c8e4b-3d5f-4b2a-9c7e-8d0f2a4b6c1d', 'PREMIUM',    'Premium card with travel and lounge benefits',           'ATIVO', now(), now()),
    ('3f2b6c1e-8d4a-4f7b-9c2e-1a5d6e7f8a9b', 'GOLD',       'Standard card for everyday purchases',                   'ATIVO', now(), now()),
    ('9e5a2d7c-1b3f-4c8a-a6d4-7b9e1f3a5c2d', 'PLATINUM',   'Card with enhanced limits and cashback',                 'ATIVO', now(), now())
ON CONFLICT ON CONSTRAINT uk_card_product_name DO NOTHING;
