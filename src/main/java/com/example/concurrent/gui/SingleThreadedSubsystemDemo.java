package com.example.concurrent.gui;


import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.*;

public class SingleThreadedSubsystemDemo {

    @SuppressWarnings("unchecked")
    public static<T> T createThreadConfinedProxy(Class<T> iface, T target) {
        // 1. 建立這個子系統專屬的單一執行緒（相當於它的專屬 EDT）
        ExecutorService dedicatedExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "Dedicated-Native-Thread");
            t.setDaemon(true);
            return t;
        });

        // 2. 利用 Dynamic Proxy 攔截所有外界呼叫
        return (T) Proxy.newProxyInstance(
                iface.getClassLoader(),
                new Class<?>[]{iface},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        // 3. 把呼叫包裝成 Callable 丟進專屬執行緒排隊
                        Callable<Object> task = () -> method.invoke(target, args);

                        // 4. 外界呼叫者在這裡卡住等待專屬執行緒執行完畢並取回結果
                        Future<Object> future = dedicatedExecutor.submit(task);
                        try {
                            return future.get();
                        } catch (ExecutionException e) {
                            throw e.getCause();
                        }
                    }
                }
        );
    }

    public static void main(String[] args) throws InterruptedException {
        // 外部取得的是一個「線程安全」的代理物件
        LegacyNativeDevice safeDevice = createThreadConfinedProxy(
                LegacyNativeDevice.class,
                new UnsafeNativeDevice()
        );

        // 即使有 10 個外部執行緒同時呼叫它...
        ExecutorService clientPool = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 5; i++) {
            final int id = i;
            clientPool.execute(() -> {
                String resp = safeDevice.sendCommand("Command-" + id);
                System.out.println("收到回應: " + resp);
            });
        }

        clientPool.shutdown();
        clientPool.awaitTermination(2, TimeUnit.SECONDS);
    }
 }
