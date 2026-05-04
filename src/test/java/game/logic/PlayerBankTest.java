package game.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerBankTest {

    @Test
    void depositIncreasesBalance() {
        PlayerBank bank = new PlayerBank(5);
        bank.deposit(10);
        assertEquals(15, bank.getBalance(), 1e-9);
    }

    @Test
    void trySpendSuccessDecreasesBalance() {
        PlayerBank bank = new PlayerBank(20);
        assertTrue(bank.trySpend(7));
        assertEquals(13, bank.getBalance(), 1e-9);
    }

    @Test
    void trySpendFailureLeavesBalanceUnchanged() {
        PlayerBank bank = new PlayerBank(3);
        assertFalse(bank.trySpend(5));
        assertEquals(3, bank.getBalance(), 1e-9);
    }

    @Test
    void depositRejectsNegativeAmount() {
        PlayerBank bank = new PlayerBank();
        assertThrows(IllegalArgumentException.class, () -> bank.deposit(-1));
    }

    @Test
    void trySpendRejectsNegativeAmount() {
        PlayerBank bank = new PlayerBank(10);
        assertThrows(IllegalArgumentException.class, () -> bank.trySpend(-1));
    }
}
