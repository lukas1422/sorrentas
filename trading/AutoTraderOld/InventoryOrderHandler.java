package AutoTraderOld;

import Trader.Allstatic;
import client.Decimal;
import client.OrderState;
import client.OrderStatus;
import controller.ApiController;
import utility.TradingUtility;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

import static Trader.Allstatic.globalIdOrderMap;
import static utility.Utility.str;

public class InventoryOrderHandler implements ApiController.IOrderHandler {
    private int defaultID;
    private CountDownLatch latch;
    private CyclicBarrier barrier;
    //private static Map<Integer, AtomicBoolean> filledOrderSingleOutput = new HashMap<>();
    private static Set<Integer> filledOrdersSet = new HashSet<>();

    public InventoryOrderHandler(int i, CountDownLatch l, CyclicBarrier cb) {
        defaultID = i;
        latch = l;
        barrier = cb;
    }

    public InventoryOrderHandler(int i, CyclicBarrier cb) {
        defaultID = i;
        latch = new CountDownLatch(1);
        barrier = cb;
    }

    @Override
    public void orderState(OrderState orderState) {
        globalIdOrderMap.get(defaultID).setAugmentedOrderStatus(orderState.status());
//        globalIdOrderMap.get(defaultID).setFinalActionTime(LocalDateTime.now());

        if (orderState.status() == OrderStatus.Filled) {
//            filledOrderSingleOutput.put(defaultID, new AtomicBoolean(true));

            if (!filledOrdersSet.contains(defaultID)) {
//                globalIdOrderMap.get(defaultID).setFinalActionTime(LocalDateTime.now());
                String msg = str(LocalTime.now().truncatedTo(ChronoUnit.SECONDS),
                        "||Order||", defaultID, globalIdOrderMap.get(defaultID), orderState.status());
//                TradingUtility.outputDetailedXU(globalIdOrderMap.get(defaultID).getSymbol(), msg);
                filledOrdersSet.add(defaultID);
            }

            if (latch.getCount() == 1) {
                System.out.println(" counting down latch Inv handler ");
                latch.countDown();

                CompletableFuture.runAsync(() -> {
                    try {
                        System.out.println(" barrier waiting BEFORE #" + barrier.getNumberWaiting());
                        barrier.await();
                        System.out.println(" barrier waiting AFTER #" + barrier.getNumberWaiting());
                    } catch (InterruptedException | BrokenBarrierException e) {
                        barrier.reset();
                        e.printStackTrace();
                    }
                });
            }
            System.out.println(" order state filled ends");
        } else if (orderState.status() == OrderStatus.Cancelled || orderState.status() == OrderStatus.ApiCancelled) {
//            globalIdOrderMap.get(defaultID).setFinalActionTime(LocalDateTime.now());
            String msg = str(" order cancelled ", defaultID,
                    Allstatic.globalIdOrderMap.get(defaultID).getOrder().orderId(),
                    Allstatic.globalIdOrderMap.get(defaultID).getOrder());
//            TradingUtility.outputDetailedXU(globalIdOrderMap.get(defaultID).getSymbol(), msg);
        }
    }

    @Override
    public void orderStatus(OrderStatus status, Decimal filled, Decimal remaining,
                            double avgFillPrice, int permId, int parentId, double lastFillPrice, int clientId, String whyHeld, double mktCapPrice) {
        System.out.println(" in orderStatus Inventory Order handler  ");;
    }


    @Override
    public void handle(int errorCode, String errorMsg) {
        //System.out.println(str(" handling error in inventoryOrderhandle ", errorCode, errorMsg));
    }

    @Override
    public String toString() {
        return str(" inventory handler for ", defaultID, Allstatic.globalIdOrderMap.get(defaultID));
    }
}
