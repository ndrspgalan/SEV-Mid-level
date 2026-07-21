package transfer;

import java.util.Objects;
import java.util.Optional;

public record TransferExecution(
        boolean accepted,
        Optional<TransferRejectionReason> rejectionReason,
        Optional<Integer> sourceBalanceBefore,
        Optional<Integer> sourceBalanceAfter,
        Optional<Integer> destinationBalanceBefore,
        Optional<Integer> destinationBalanceAfter
) {
    public TransferExecution {
        rejectionReason = Objects.requireNonNull(rejectionReason);
        sourceBalanceBefore = Objects.requireNonNull(sourceBalanceBefore);
        sourceBalanceAfter = Objects.requireNonNull(sourceBalanceAfter);
        destinationBalanceBefore = Objects.requireNonNull(destinationBalanceBefore);
        destinationBalanceAfter = Objects.requireNonNull(destinationBalanceAfter);

        if (accepted) {
            if (rejectionReason.isPresent()
                    || sourceBalanceBefore.isEmpty()
                    || sourceBalanceAfter.isEmpty()
                    || destinationBalanceBefore.isEmpty()
                    || destinationBalanceAfter.isEmpty()) {
                throw new IllegalArgumentException("accepted execution requires complete balances and no rejection");
            }
        } else if (rejectionReason.isEmpty()) {
            throw new IllegalArgumentException("rejected execution requires a rejection reason");
        }
    }

    public static TransferExecution rejected(
            TransferRejectionReason reason,
            Optional<Integer> sourceBefore,
            Optional<Integer> destinationBefore
    ) {
        return new TransferExecution(
                false,
                Optional.of(Objects.requireNonNull(reason)),
                sourceBefore,
                Optional.empty(),
                destinationBefore,
                Optional.empty()
        );
    }

    public static TransferExecution completed(
            int sourceBefore,
            int sourceAfter,
            int destinationBefore,
            int destinationAfter
    ) {
        return new TransferExecution(
                true,
                Optional.empty(),
                Optional.of(sourceBefore),
                Optional.of(sourceAfter),
                Optional.of(destinationBefore),
                Optional.of(destinationAfter)
        );
    }
}
