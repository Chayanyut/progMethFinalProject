package game.logic;

/**
 * Player currency store.
 */
public class PlayerBank {
    private double balance;

    public PlayerBank() {
        this(0.0);
    }

    public PlayerBank(double initialBalance) {
        this.balance = initialBalance;
    }

    public double getBalance() {
        return balance;
    }

    public void deposit(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must be non-negative");
        }
        this.balance += amount;
    }

    /**
     * @return true if the spend succeeded, false if insufficient funds
     */
    public boolean trySpend(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must be non-negative");
        }
        if (this.balance < amount) {
            return false;
        }
        this.balance -= amount;
        return true;
    }
}
