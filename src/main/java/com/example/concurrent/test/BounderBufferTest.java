package com.example.concurrent.test;

import java.util.concurrent.ExecutionException;

public class BounderBufferTest extends TesCase {
    void testIsEmptyWhenConstructed() {
        BounderBuffer<Object> bb = new BounderBuffer<>(10);
        assertTrue(bb.isEmpty());    // 斷言：應該要是空的
        assertFalse(bb.isFull());   // 斷言：不應該是滿的
    }

    // 測試 2：驗證「塞滿後」的狀態
    void testIsFullAfterPuts() throws InterruptedException {
        BounderBuffer bb = new BounderBuffer(10);
        // 容量為 10，連續放進 10 個數字
        for (int i = 0; i < 10; i++)
            bb.put(i);

        assertTrue(bb.isFull());     // 斷言：現在應該是滿的
        assertFalse(bb.isEmpty());  // 斷言：現在不應該是空的
    }
    
    void testTakeBlocksWhenEmpty() {
        final BounderBuffer<Integer> bb = new BounderBuffer<>(10);

        // 1. 建立一個專門負責「拿東西」的子執行緒 (taker)
        Thread taker = new Thread() {
            public void run() {
                try {
                    Integer unused = bb.take(); // 空的 Buffer，這裡「必須卡住」！
                    fail();                     // 如果走到這行，代表沒卡住直接拿到東西，測試失敗！
                } catch (InterruptedException success) {
                    // 預期行為：被主執行緒 interrupt 後會拋出此例外，當作成功並結束執行緒
                }
            }
        };

        try {
            // 2. 啟動 taker 執行緒
            taker.start();

            // 3. 讓主執行緒睡一下，給 taker 足夠的時間去呼叫 bb.take() 並卡住
            Thread.sleep(LOCKUP_DETECT_TIMEOUT);

            // 4. 發送中斷信號叫醒 taker
            taker.interrupt();

            // 5. 等待 taker 結束（設定超時時間，防止 taker 徹底當死）
            taker.join(LOCKUP_DETECT_TIMEOUT);

            // 6. 驗證 taker 是否已經順利結束生命週期
            assertFasle(taker.isAlive());
        } catch (ExecutionException unexpected) {
            fail();
        }

    }
}
