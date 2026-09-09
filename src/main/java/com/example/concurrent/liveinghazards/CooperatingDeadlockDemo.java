package com.example.concurrent.liveinghazards;


import java.awt.*;
import java.util.HashSet;
import java.util.Set;

public class CooperatingDeadlockDemo {
    public static void main(String[] args) {
        Dispatcher dispatcher = new Dispatcher();
        Taxi taxi = new Taxi(dispatcher, new Point(10, 10));
        dispatcher.addTaxi(taxi);
        
        // 執行緒 1：模擬 GPS 回傳抵達目的的的訓號 (先拿 Taxi 鎖，再拿 Dispatcher 鎖）
        Thread gpsThread = new Thread(() -> {
            while (true) {
                taxi.setLocation(new Point(10, 10)); // 抵達目的地
            }
        }, "GPS-Thread");

        // 執行緒 2：後台定時渲染地圖（先拿 Dispatcher 鎖，再拿 Taxi 鎖）
        Thread renderThread = new Thread(() -> {
            while (true) {
                dispatcher.getImage();
            }
        }, "Render-Thread");

        gpsThread.start();
        renderThread.start();
    }

    static class Taxi {
        private Point location;
        private final Point destination;
        private final Dispatcher dispatcher;

        public Taxi(Dispatcher dispatcher, Point destination) {
            this.dispatcher = dispatcher;
            this.destination = destination;
        }

        public synchronized Point getLocation() {
            return location;
        }

        public synchronized void setLocation(Point location) {
            this.location = location;

            // 模擬抵達目的地
            if (location.equals(destination)) {
                // 加入 10ms 延遲讓另一條執行緒有時間走 Dispatcher 鎖
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {}

                // 持有 Taxi(this) 鎖的同時，呼叫外部方法，嘗試取得 Dispatcher 鎖
                dispatcher.notifyAvailable(this);
            }
        }

    }

    static class Dispatcher {
        private final Set<Taxi> taxis = new HashSet<>();
        private final Set<Taxi> availableTaxis = new HashSet<>();

        public synchronized void addTaxi(Taxi taxi) {
            taxis.add(taxi);
        }

        public synchronized void notifyAvailable(Taxi taxi) {
            availableTaxis.add(taxi);
            System.out.println("Taxi reported available");
        }

        public synchronized void getImage() {
            // 加入 10ms 延遲讓另一條執行緒有時間拿走 Taxi 鎖
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {}

            for (Taxi t : taxis) {
                Point p = t.getLocation();
                System.out.println("Drawing taxi at: " + p);
            }
        }
    }
}
