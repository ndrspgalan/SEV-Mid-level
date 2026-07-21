package console;

import application.ValerianEconomicSystem;
import application.account.AccountHolderService;
import application.account.ProfessionChangeService;
import application.history.AccountHistoryQueryService;
import application.history.AccountHistoryStatisticsService;
import application.lifecycle.AccountLifecycleService;
import application.operation.*;

import java.io.InputStream;
import java.io.PrintStream;
import java.time.Clock;
import java.util.Objects;

public final class JuniorConsoleFactory {

    private JuniorConsoleFactory() {
    }

    public static JuniorConsole create(
            ValerianEconomicSystem system,
            InputStream input,
            PrintStream output
    ) {
        Objects.requireNonNull(system);
        Objects.requireNonNull(input);
        Objects.requireNonNull(output);

        return create(system, new ConsoleInput(input, output), output);
    }

    static JuniorConsole create(
            ValerianEconomicSystem system,
            ConsoleInput consoleInput,
            PrintStream output
    ) {
        Objects.requireNonNull(system);
        Objects.requireNonNull(consoleInput);
        Objects.requireNonNull(output);

        Clock clock = Clock.systemUTC();
        TransactionQueryService transactionQueryService =
                new TransactionQueryService(system.getTransactionLedger());
        AccountHistoryQueryService accountHistoryQueryService =
                new AccountHistoryQueryService(system.getConsumerRegistry().getAccountHistoryJournal());

        return new JuniorConsole(
                consoleInput,
                output,
                new MintOperationService(
                        system.getMintPolicy(),
                        system.getTransactionLedger(),
                        clock,
                        system.getConsumerRegistry(),
                        system.getOperationalControlService(),
                        system.getOperationalDecisionJournal()
                ),
                new ExchangeOperationService(
                        system.getConsumerRegistry(),
                        system.getExchangePolicy(),
                        system.getTransactionLedger(),
                        clock,
                        system.getOperationalControlService(),
                        system.getOperationalDecisionJournal()
                ),
                new PurchaseOperationService(
                        system.getConsumerRegistry(),
                        system.getConsumableRegistry(),
                        system.getCommercialTransactionPolicy(),
                        system.getTransactionLedger(),
                        clock,
                        system.getOperationalControlService(),
                        system.getOperationalDecisionJournal()
                ),
                new TransferOperationService(
                        system.getConsumerRegistry(),
                        system.getTransferPolicy(),
                        system.getTransferRequestRegistry(),
                        system.getTransactionLedger(),
                        clock,
                        system.getOperationalControlService(),
                        system.getOperationalDecisionJournal()
                ),
                new AccountQueryService(system.getConsumerRegistry()),
                transactionQueryService,
                new TransactionStatisticsService(transactionQueryService),
                new ProfessionChangeService(system.getConsumerRegistry()),
                new AccountHolderService(system.getConsumerRegistry()),
                accountHistoryQueryService,
                new AccountHistoryStatisticsService(accountHistoryQueryService),
                new AccountLifecycleService(system.getConsumerRegistry())
        );
    }
}
