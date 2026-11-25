-- T-096: persiste o e-mail opcional informado no cadastro do atleta (T-078),
-- que até aqui só era usado para dedupe. Permite expor `email` em
-- AthleteResponse e pré-preencher o convite no front. Coluna anulável para
-- preservar atletas cadastrados sem e-mail.
ALTER TABLE athletes
    ADD COLUMN email VARCHAR(255);
