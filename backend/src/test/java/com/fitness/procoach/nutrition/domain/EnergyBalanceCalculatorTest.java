package com.fitness.procoach.nutrition.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitness.procoach.nutrition.domain.EnergyBalance.Classification;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class EnergyBalanceCalculatorTest {

	private static final BigDecimal GET = new BigDecimal("1602");

	@Test
	void dietaAbaixoDoGetEhDeficit() {
		EnergyBalance balance = EnergyBalanceCalculator.calculate(1400, GET);

		assertThat(balance.balanceKcal()).isEqualByComparingTo("-202");
		assertThat(balance.balancePercent()).isEqualByComparingTo("-12.61");
		assertThat(balance.classification()).isEqualTo(Classification.DEFICIT);
		assertThat(balance.getKcal()).isEqualByComparingTo("1602");
	}

	@Test
	void dietaAcimaDoGetEhSuperavit() {
		EnergyBalance balance = EnergyBalanceCalculator.calculate(2000, GET);

		assertThat(balance.balanceKcal()).isEqualByComparingTo("398");
		assertThat(balance.balancePercent()).isEqualByComparingTo("24.84");
		assertThat(balance.classification()).isEqualTo(Classification.SURPLUS);
	}

	@Test
	void dietaProximaDoGetEhManutencao() {
		// 1650 = +48 kcal = +3,00% do GET, dentro de ±5%.
		EnergyBalance balance = EnergyBalanceCalculator.calculate(1650, GET);

		assertThat(balance.balanceKcal()).isEqualByComparingTo("48");
		assertThat(balance.balancePercent()).isEqualByComparingTo("3.00");
		assertThat(balance.classification()).isEqualTo(Classification.MAINTENANCE);
	}

	@Test
	void limiteDeCincoPorCentoAindaEhManutencao() {
		// GET 2000: 1900 = −100 kcal = exatamente −5,00%.
		EnergyBalance balance = EnergyBalanceCalculator.calculate(1900, new BigDecimal("2000"));

		assertThat(balance.balancePercent()).isEqualByComparingTo("-5.00");
		assertThat(balance.classification()).isEqualTo(Classification.MAINTENANCE);
	}

	@Test
	void logoAlemDoLimiteDeixaDeSerManutencao() {
		EnergyBalance balance = EnergyBalanceCalculator.calculate(1899, new BigDecimal("2000"));

		assertThat(balance.classification()).isEqualTo(Classification.DEFICIT);
	}

	@Test
	void getInvalidoEhRecusado() {
		assertThatThrownBy(() -> EnergyBalanceCalculator.calculate(1500, BigDecimal.ZERO))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> EnergyBalanceCalculator.calculate(1500, null))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
