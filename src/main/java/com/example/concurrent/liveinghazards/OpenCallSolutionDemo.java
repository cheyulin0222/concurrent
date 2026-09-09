package com.example.concurrent.liveinghazards;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

public class OpenCallSolutionDemo {

    public static void main(String[] args) {
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

        public void setLocation(Point location) {
            boolean reachedDestination;

            // 關鍵：只鎖住修改自己內部狀態的這幾行
            synchronized (this) {
                this.location = location;
                reachedDestination = location.equals(destination);
            } // 走出這個大括號，Taxi 鎖已經徹底釋放！

            // 開放呼叫（Open Call）：此時手上沒有任何鎖！
            if (reachedDestination) {
                dispatcher.notifyAvailable(this);
            }
        }
    }

    static class Dispatcher {
        private final Set<Taxi> taxis = new HashSet<>();
        private final Set<Taxi> availableTaxis = new HashSet<>();

        public synchronized void notifyAvailable(Taxi taxi) {
            availableTaxis.add(taxi);
        }

        public void getImages() {
            Set<Taxi> copy;

            synchronized (this) {
                copy = new HashSet<>(taxis);
            } // 走出這個大括號，Dispatcher 鎖已經徹底釋放！

            // 開放呼叫（Open Call）：在沒有持有 Dispatcher 鎖的狀態下，逐一去問每輛車
            for (Taxi t : copy) {
                Point p = t.getLocation(); // 拿 Taxi 鎖時，身上根本沒有 Dispatcher 鎖
                // 繪製地圖標記
            }
        }
    }
}
