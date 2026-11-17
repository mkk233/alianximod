我先通读这份补充审查说明，确认只读范围后再核对它点名的顺序事实。第二节那句「预留发生在 `hurtConfirmed` 返回真之后（99–102 行）」是笔误。单价大于 0 时，`apply` 先预留，再调用 `confirmDamage`，确认后才 `retainShare`。

`DomainContactBilling.apply` 里，费用大于 0 的实际顺序是：

1. 第 92 行 `ManaPaymentReceipt.charge` 预留。
2. 第 93–94 行收据为 null 时直接返回，不会进入后面的确认。
3. 第 99 行在 `try` 里调用 `confirmDamage`。
4. 第 101 行只有确认成功才 `retainShare`，并在第 102 行把 `committed` 设为真。
5. 第 104–107 行的 `finally` 在尚未提交时 `receipt.refund`。

第 99–102 行是确认后的 `retainShare`，预留已经在第 92 行完成。第三节写的「先 `charge`，再 `hurtConfirmed`，成功再 `retainShare`」与这段大于 0 的路径一致。

```86:108:src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/DomainContactBilling.java
        if (price <= 0) {
            if (confirmDamage(event)) {
                deliverPayload(event);
            }
            return;
        }
        ManaPaymentReceipt receipt = ManaPaymentReceipt.charge(event.owner, price, false);
        if (receipt == null) {
            return;
        }
        boolean committed = false;
        boolean confirmed = false;
        try {
            confirmed = eligible(event) && confirmDamage(event);
            if (confirmed) {
                receipt.retainShare(price);
                committed = true;
            }
        } finally {
            if (!committed) {
                receipt.refund(event.owner);
            }
        }
```

同一方法里，第 86–90 行在报价小于等于 0 时直接 `confirmDamage`，没有 `charge`、`retainShare` 或 `refund`。当前单价常量是 1，种类数大于 0 时不会走进这个分支。
