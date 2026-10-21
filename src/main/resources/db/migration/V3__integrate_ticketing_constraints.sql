-- V1 et V2 sont conservees : cette evolution s'applique aussi aux bases existantes.
-- Six billets au prix catalogue maximal doivent tenir dans le total de l'achat.
ALTER TABLE ticket_purchases MODIFY total_amount DECIMAL(12,2) NOT NULL;
ALTER TABLE ticket_purchases ADD CONSTRAINT chk_purchase_total_positive CHECK (total_amount > 0);
ALTER TABLE loyalty_cards ADD CONSTRAINT chk_loyalty_period CHECK (expires_at > purchased_at);
